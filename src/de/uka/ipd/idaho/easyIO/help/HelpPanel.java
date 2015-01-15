/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.easyIO.help;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * JPanel for displaying a help, i.e. a hierarchy of HelpChapters. The panel
 * needs a File or URL from which to load the icons for the tree representation
 * of the help content. This panel cannot only navigate a help and display its
 * content pages, but also browse the help content with Back and Forward
 * buttons. It also includes a full text search, which is backed by inverted
 * list files if the HelpPanel has a folder to store the index files in. In
 * addition, it is possible to extrenalize the help content, saving it as an
 * HTML page with a uList based tree for navigation and an iframe for displaying
 * the content pages. This is useful for exporting the help from an application,
 * e.g. for making it available on a web page.
 * 
 * @author sautter
 */
public class HelpPanel extends JPanel {
	
	static final String ROOT_ICON_FILE_NAME = "root.gif";
	static final String CLOSED_ICON_FILE_NAME = "closed.gif";
	static final String OPEN_ICON_FILE_NAME = "open.gif";
	static final String LEAF_ICON_FILE_NAME = "leaf.gif";
	
	private String mainTitle = "Help";
	private HelpChapter rootChapter;
	
	private JTree navigation;
	private JScrollPane navigationBox;
	private boolean isNavigating = false;
	
	private JEditorPane display = new JEditorPane("text/html", "<HTML>Help is coming</HTML>");
	private JScrollPane displayBox = new JScrollPane(this.display);
	
	private JSplitPane navigationDisplaySplit;
	private boolean dividerLocationSet = false;
	
	private JButton backButton = new JButton("Back");
	private JButton forwardButton = new JButton("Forward");
	
	private HelpChapter currentChapter = null;
	private Stack backHistory = new Stack();
	private Stack forwardHistory = new Stack();
	
	private JComboBox searchField = new JComboBox();
	private JButton searchButton = new JButton("Search");
	
	/**	Constructor
	 * @param	title			the title for the help window
	 * @param	contentRoot		the root node of the help content tree
	 */
	public HelpPanel(String title, HelpChapter contentRoot) {
		this(title, contentRoot, ((File) null));
	}

	/**	Constructor
	 * @param	title			the title for the help window
	 * @param	contentRoot		the root node of the help content tree
	 * @param	rootIcon		the root icon
	 */
	public HelpPanel(String title, HelpChapter contentRoot, Image rootIcon) {
		super(new BorderLayout(), true);
		if (title != null) this.mainTitle = title;
		this.rootChapter = contentRoot;
		
		//	initialize tree
		this.navigation = new JTree(contentRoot);
		this.navigation.setShowsRootHandles(true);
		this.navigation.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		
		//	select root chapter
		this.navigation.setSelectionRow(0);
		
		//	adjust tree icons (open / closed book, book page)
		this.navigation.setCellRenderer(new HelpCellRenderer(rootIcon));
		
		//	do remaining initialization
		this.init();
	}
	
	/**	Constructor
	 * @param	title			the title for the help window
	 * @param	contentRoot		the root node of the help content tree
	 * @param	dataBaseUrl		the path where icon files etc. are located
	 */
	public HelpPanel(String title, HelpChapter contentRoot, String dataBaseUrl) {
		super(new BorderLayout(), true);
		if (title != null) this.mainTitle = title;
		this.rootChapter = contentRoot;
		
		//	initialize tree
		this.navigation = new JTree(contentRoot);
		this.navigation.setShowsRootHandles(true);
		this.navigation.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		
		//	select root chapter
		this.navigation.setSelectionRow(0);
		
		//	adjust tree icons (open / closed book, book page)
		if (dataBaseUrl != null) try {
			this.navigation.setCellRenderer(new HelpCellRenderer(dataBaseUrl));
		} catch (MalformedURLException e) {}
		
		//	do remaining initialization
		this.init();
	}

	/**	Constructor
	 * @param	title			the title for the help window
	 * @param	contentRoot		the root node of the help content tree
	 * @param	dataPath		the path where icon files etc. are located
	 */
	public HelpPanel(String title, HelpChapter contentRoot, File dataPath) {
		super(new BorderLayout(), true);
		if (title != null) this.mainTitle = title;
		this.rootChapter = contentRoot;
		
		//	initialize tree
		this.navigation = new JTree(contentRoot);
		this.navigation.setShowsRootHandles(true);
		this.navigation.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		
		//	select root chapter
		this.navigation.setSelectionRow(0);
		
		//	adjust tree icons (open / closed book, book page)
		if (dataPath != null) {
			this.navigation.setCellRenderer(new HelpCellRenderer(dataPath));
			this.initSearchIndex(dataPath);
		}
		
		//	do remaining initialization
		this.init();
	}
	
	private void init() {
		
		//	listen for changes
		this.navigation.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent tse) {
				if (!isNavigating) displayChapter((HelpChapter) navigation.getLastSelectedPathComponent());
			}
		});
		
		this.navigationBox = new JScrollPane(this.navigation);
		
		//	initialize display
		this.display.setEditable(false);
		
		//	initialize function panel
		JPanel functionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		
		this.backButton.setEnabled(false);
		this.backButton.setBorder(BorderFactory.createRaisedBevelBorder());
		this.backButton.setPreferredSize(new Dimension(70, 21));
		this.backButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				back();
			}
		});
		functionPanel.add(this.backButton);
		
		this.forwardButton.setEnabled(false);
		this.forwardButton.setBorder(BorderFactory.createRaisedBevelBorder());
		this.forwardButton.setPreferredSize(new Dimension(70, 21));
		this.forwardButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				forward();
			}
		});
		functionPanel.add(this.forwardButton);
		
		this.searchField.setBorder(BorderFactory.createLoweredBevelBorder());
		this.searchField.setPreferredSize(new Dimension(100, 23));
		this.searchField.setEditable(true);
		functionPanel.add(this.searchField);
		
		this.searchButton.setBorder(BorderFactory.createRaisedBevelBorder());
		this.searchButton.setPreferredSize(new Dimension(70, 21));
		this.searchButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Object o = searchField.getSelectedItem();
				if (o != null) search(o.toString());
			}
		});
		functionPanel.add(this.searchButton);
		
		JButton externalizeButton = new JButton("Externalize");
		externalizeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		externalizeButton.setPreferredSize(new Dimension(70, 21));
		externalizeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				externalizeHelp();
			}
		});
		functionPanel.add(externalizeButton);
		
		//	initialize main split
		this.navigationDisplaySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		this.navigationDisplaySplit.add(this.navigationBox, JSplitPane.LEFT);
		this.navigationDisplaySplit.add(this.displayBox, JSplitPane.RIGHT);
		
		//	put it all together
		this.add(functionPanel, BorderLayout.NORTH);
		this.add(this.navigationDisplaySplit, BorderLayout.CENTER);
		
		//	display root chapter of content
		this.currentChapter = this.rootChapter;
		this.displayCurrentChapter();
		
		//	make sure divider of main split is not set before the panel knows its size 
		this.addAncestorListener(new AncestorListener() {
			public void ancestorAdded(AncestorEvent ae) {
				if (!dividerLocationSet) navigationDisplaySplit.setDividerLocation(0.25);
				dividerLocationSet = true;
			}
			public void ancestorMoved(AncestorEvent ae) {}
			public void ancestorRemoved(AncestorEvent ae) {}
		});
	}
	
	private void displayChapter(HelpChapter chapter) {
		if (chapter == null)
			return;
		
		//	disable forward
		this.forwardHistory.clear();
		this.forwardButton.setEnabled(false);
		
		//	remember prevoius chapter
		if ((this.currentChapter != null) && (this.currentChapter != chapter)) {
			this.backHistory.push(this.currentChapter);
			while (this.backHistory.size() > 10)
				this.backHistory.removeElementAt(0);
			this.backButton.setEnabled(true);
		}
		
		//	remember and display chapter
		this.currentChapter = chapter;
		this.displayCurrentChapter();
	}
	
	private void displayCurrentChapter() {
		if (this.currentChapter == null) {
			this.notifyTitleChanged(this.mainTitle);
			return;
		}
		
		//	collect path in order to select chapter in tree
		this.isNavigating = true; // keep tree listener from firing another update
		ArrayList pathList = new ArrayList();
		TreeNode chapter = this.currentChapter;
		while (chapter != null) {
			pathList.add(chapter);
			chapter = chapter.getParent();
		}
		Collections.reverse(pathList);
		this.navigation.setSelectionPath(new TreePath(pathList.toArray()));
		this.isNavigating = false; // re-enable tree listener
		
		//	set title
		String title = this.currentChapter.getTitle();
		HelpChapter parentChapter = (HelpChapter) this.currentChapter.getParent();
		while (parentChapter != null) {
			title = parentChapter.getTitle() + " - " + title;
			parentChapter = (HelpChapter) parentChapter.getParent();
		}
		this.notifyTitleChanged(this.mainTitle + " - " + title);
		
		//	set content
		Thread reader = new Thread(new Runnable() {
			public void run() {
				try {
					Reader r = currentChapter.getTextReader();
					display.read(r, new HTMLDocument());
					r.close();
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
					display.setText("<HTML><TT>" + ioe.getClass().getName() + "</TT><BR>(" + ioe.getMessage() + ")<BR>while reading content, sorry.</HTML>");
				}
			}
		});
		reader.start();
		
		//	notify listeners
		this.notifyChapterSelected(this.currentChapter);
	}
	
	private void back() {
		if (this.backHistory.isEmpty())
			this.backButton.setEnabled(false);
		
		else {
			
			//	remember current chapter
			this.forwardHistory.push(this.currentChapter);
			this.forwardButton.setEnabled(true);
			
			//	switch to prevoius chapter
			this.currentChapter = ((HelpChapter) this.backHistory.pop());
			if (this.backHistory.isEmpty())
				this.backButton.setEnabled(false);
			this.displayCurrentChapter();
		}
	}
	
	private void forward() {
		if (this.forwardHistory.isEmpty())
			this.backButton.setEnabled(false);
		
		else {
			
			//	remember current chapter
			this.backHistory.push(this.currentChapter);
			this.backButton.setEnabled(true);
			
			//	switch to next chapter
			this.currentChapter = ((HelpChapter) this.forwardHistory.pop());
			if (this.forwardHistory.isEmpty())
				this.forwardButton.setEnabled(false);
			this.displayCurrentChapter();
		}
	}
	
	private static final String SEARCH_INDEX_FILE_NAME = "SearchIndex.txt";
	private static final String CHAPTER_INDEX_FILE_NAME = "ChapterIndex.txt";
	
	private Properties idToChapterMapping = new Properties();
	private HashMap termToChapterIDsMapping = new HashMap();
	
	private void search(String searchString) {
		StringVector searchTerms = new StringVector();
		searchTerms.parseAndAddElements(searchString.toLowerCase(), " ");
		searchTerms.removeAll("");
		
		StringVector resultIDsCollector = new StringVector();
		for (int t = 0; t < searchTerms.size(); t++) {
			String searchTerm = StringUtils.porterStem(searchTerms.get(t));
			if (this.termToChapterIDsMapping.containsKey(searchTerm)) {
				String[] trIDs = ((String[]) this.termToChapterIDsMapping.get(searchTerm));
				int termWeight = (this.idToChapterMapping.size() / trIDs.length);
				for (int i = 0; i < termWeight; i++) resultIDsCollector.addContent(trIDs);
			}
		}
		
		StringVector resultIDs = resultIDsCollector.union(resultIDsCollector);
		if (resultIDs.size() > 0) {
			HelpSearchResult[] results = new HelpSearchResult[resultIDs.size()]; 
			for (int r = 0; r < resultIDs.size(); r++)
				results[r] = new HelpSearchResult(resultIDs.get(r), resultIDsCollector.getElementCount(resultIDs.get(r)));
			Arrays.sort(results);
			
			String[] resultTitles = new String[results.length];
			int[] resultScores = new int[results.length];
			for (int r = 0; r < results.length; r++) {
				resultTitles[r] = this.idToChapterMapping.getProperty(results[r].chapterID, "unknown");
				resultScores[r] = results[r].score;
			}
			
			HelpSearchResultDialog  hsrd = new HelpSearchResultDialog(this, ("Results for '" + searchString + "'"), resultScores, resultTitles);
			hsrd.setVisible(true);
			String resultTitle = hsrd.getSelectedResultTitle();
			if (resultTitle != null)
				this.displayChapter(resultTitle);
		}
		else JOptionPane.showMessageDialog(this, "Your search returned no results, sorry.", ("Results for '" + searchString + "'"), JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void initSearchIndex(File dataPath) {
		try {
			FileReader fr = new FileReader(new File(dataPath, CHAPTER_INDEX_FILE_NAME));
			StringVector chapterIndexLines = StringVector.loadList(fr);
			fr.close();
			
			for (int c = 0; c < chapterIndexLines.size(); c++) {
				String chapterIndexLine = chapterIndexLines.get(c);
				if (chapterIndexLine.indexOf(':') != -1) {
					String id = chapterIndexLine.substring(0, chapterIndexLine.indexOf(':'));
					String chapterName = chapterIndexLine.substring(chapterIndexLine.indexOf(':') + 1);
					this.idToChapterMapping.setProperty(id, chapterName);
				}
			}
			
			//	check if index files valid (vague chack by number of chapters)
			ArrayList chapters = new ArrayList();
			chapters.add(this.rootChapter);
			int chapterCount = 0;
			
			//	do breadth first search
			while (!chapters.isEmpty()) {
				TreeNode chapter = ((TreeNode) chapters.remove(0));
				chapterCount ++;
				for (int c = 0; c < chapter.getChildCount(); c++) chapters.add(chapter.getChildAt(c));
			}
			
			//	number of existing chapters equals number of indexed chapters
			if (chapterCount == this.idToChapterMapping.size()) {
				fr = new FileReader(new File(dataPath, SEARCH_INDEX_FILE_NAME));
				StringVector indexLines = StringVector.loadList(fr);
				fr.close();
				
				StringVector idParser = new StringVector();
				for (int i = 0; i < indexLines.size(); i++) {
					String indexLine = indexLines.get(i);
					if (indexLine.indexOf(':') != -1) {
						String term = indexLine.substring(0, indexLine.indexOf(':'));
						String idString = indexLine.substring(indexLine.indexOf(':') + 1);
						idParser.parseAndAddElements(idString, ",");
						this.termToChapterIDsMapping.put(term, idParser.toStringArray());
						idParser.clear();
					}
				}
			}
			
			//	something changed since last indexing run, rebuild index
			else {
				this.idToChapterMapping.clear();
				
				//	rename existing files
				File indexFile = new File(dataPath, CHAPTER_INDEX_FILE_NAME);
				File oldFile = new File(dataPath, (CHAPTER_INDEX_FILE_NAME + ".old"));
				if (oldFile.exists() && oldFile.delete())
					oldFile = new File(dataPath, (CHAPTER_INDEX_FILE_NAME + ".old"));
				indexFile.renameTo(oldFile);
				
				indexFile = new File(dataPath, SEARCH_INDEX_FILE_NAME);
				oldFile = new File(dataPath, (SEARCH_INDEX_FILE_NAME + ".old"));
				if (oldFile.exists() && oldFile.delete())
					oldFile = new File(dataPath, (SEARCH_INDEX_FILE_NAME + ".old"));
				indexFile.renameTo(oldFile);
				
				//	rebuild index
				this.buildSearchIndex(dataPath);
			}
			
		}
		catch (FileNotFoundException fnfe) {
			this.buildSearchIndex(dataPath);
		}
		catch (IOException ioe) {}
	}
	
	private void buildSearchIndex(File dataPath) {
		if ((dataPath != null) && dataPath.isDirectory()) try {
			
			//	collect index data
			Properties idToChapterMapping = new Properties();
			StringVector termList = new StringVector();
			HashMap termToIDsMapping = new HashMap();
			int lastID = this.indexHelpChapter(this.rootChapter, 0, StringUtils.getNoiseWords(), idToChapterMapping, termList, termToIDsMapping);
			
			//	write data & fill local structures
			BufferedWriter br = new BufferedWriter(new FileWriter(new File(dataPath, CHAPTER_INDEX_FILE_NAME)));
			for (int i = 0; i < lastID; i++) {
				String idString = ("" + i);
				String chapterTitle = idToChapterMapping.getProperty(idString);
				this.idToChapterMapping.setProperty(idString, chapterTitle);
				try {
					br.write(idString + ":" + chapterTitle);
					br.newLine();
				} catch (IOException ioe) {}
			}
			try {
				br.flush();
				br.close();
			} catch (IOException ioe) {}
			
			br = new BufferedWriter(new FileWriter(new File(dataPath, SEARCH_INDEX_FILE_NAME)));
			for (int t = 0; t < termList.size(); t++) {
				String term = termList.get(t);
				StringVector chapterIDs = ((StringVector) termToIDsMapping.get(term));
				this.termToChapterIDsMapping.put(term, chapterIDs.toStringArray());
				try {
					br.write(term + ":" + chapterIDs.concatStrings(","));
					br.newLine();
				} catch (IOException ioe) {}
			}
			try {
				br.flush();
				br.close();
			} catch (IOException ioe) {}
			
		} catch (Exception e) {}
	}
		
	private int indexHelpChapter(HelpChapter chapter, int id, final StringVector noiseWords, Properties idToChapterMapping, StringVector termList, HashMap termToIDsMapping) throws Exception {
		
		String idString = ("" + id);
		
		//	map ID to chapter title
		idToChapterMapping.setProperty(idString, chapter.getTitle());
		
		//	extract index terms from chapter content
		final BufferedReader content = new BufferedReader(chapter.getTextReader());
		PipedOutputStream pos = new PipedOutputStream();
		PipedInputStream pis = new PipedInputStream(pos);
		final BufferedWriter parserWriter = new BufferedWriter(new OutputStreamWriter(pos));
		
		Thread writer = new Thread(new Runnable() {
			public void run() {
				try {
					String line = content.readLine();
					while (line != null) {
						parserWriter.write(line);
						line = content.readLine();
					}
					parserWriter.flush();
					parserWriter.close();
				} catch (Exception e) {}
			}
		});
		
		final Grammar grammar = new StandardGrammar();
		final StringVector indexTerms = new StringVector();
		TokenReceiver termCollector = new TokenReceiver() {
			private Pattern pattern = Pattern.compile("([A-Za-z]++)");
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (!grammar.isTag(token)) {
					Matcher matcher = pattern.matcher(token);
					while (matcher.find()) {
						String indexTerm = matcher.group(0).toLowerCase();
						if (!noiseWords.containsIgnoreCase(indexTerm))
							indexTerms.addElementIgnoreDuplicates(StringUtils.porterStem(indexTerm));
					}
				}
			}
		};
		
		writer.start();
		try {
			Parser p = new Parser();
			p.stream(pis, termCollector);
		} catch (Exception e) {}
		
		//	map index terms to chapter ID
		for (int t = 0; t < indexTerms.size(); t++) {
			String term = indexTerms.get(t).toLowerCase();
			termList.addElementIgnoreDuplicates(term);
			StringVector idList;
			if (termToIDsMapping.containsKey(term))
				idList = ((StringVector) termToIDsMapping.get(term));
			
			else {
				idList = new StringVector();
				termToIDsMapping.put(term, idList);
			}
			idList.addElement(idString);
		}
		
		//	write children if given
		if (chapter.isLeaf()) return (id + 1);
		else {
			int nextID = id + 1;
			for (int c = 0; c < chapter.getChildCount(); c++)
				nextID = this.indexHelpChapter(((HelpChapter) chapter.getChildAt(c)), nextID, noiseWords, idToChapterMapping, termList, termToIDsMapping);
			return nextID;
		}
	}
	
	/**	switch to the chapter with the specified title
	 * @param	title	the title of the chapter to switch to (null will switch to root chapter)
	 */
	public void displayChapter(String title) {
		this.displayChapter(this.findChapter(title));
	}
	
	//	cache for chapters by their title
	private HashMap chaptersByTitle = new HashMap();
	
	private HelpChapter findChapter(String title) {
		if (title == null) return this.rootChapter;
		
		//	look up title in cache
		if (this.chaptersByTitle.containsKey(title))
			return ((HelpChapter) this.chaptersByTitle.get(title));
		
		//	initialize search
		ArrayList chapters = new ArrayList();
		HelpChapter candidate = null;	//	remember case insensitive matches in case no case sensitive ones found
		chapters.add(this.rootChapter);
		
		//	do breadth first search
		while (!chapters.isEmpty()) {
			HelpChapter chapter = ((HelpChapter) chapters.remove(0));
			if (title.equals(chapter.getTitle())) {
				this.chaptersByTitle.put(title, chapter);
				return chapter;
			} else {
				if (title.equalsIgnoreCase(chapter.getTitle())) candidate = chapter;
				for (int c = 0; c < chapter.getChildCount(); c++) chapters.add(chapter.getChildAt(c));
			}
		}
		
		//	chapter not found
		return candidate;
	}
	
	private static final String FRAME_MODE = "Navigation & Frame";
	private static final String JDOC_MODE = "Navigation & Document";
	private static final String DOC_MODE = "Single Document";
	
	private void externalizeHelp() {
		String[] modes = {FRAME_MODE, JDOC_MODE, DOC_MODE};
		int mode = JOptionPane.showOptionDialog(this, "In which format should the help content be exportet?", "Choose Export Format", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, modes, FRAME_MODE);
		if (mode == 0) this.externalizeHelpAsFrame();
		else if (mode == 1) this.externalizeHelpAsJDoc();
		else this.externalizeHelpAsPage();
	}
	
	private void externalizeHelpAsFrame() {
		JFileChooser fc = new JFileChooser("Save Help Content");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setMultiSelectionEnabled(false);
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File storageDir = fc.getSelectedFile();
			if ((storageDir != null) && storageDir.isDirectory()) try {
				
				//	create writer for navigation page
				BufferedWriter navigationWriter = new BufferedWriter(new FileWriter(new File(storageDir, "HelpMainPage.html")));
				
				//	initialize navigation page
				navigationWriter.write("<html><head>");
				navigationWriter.write("<title>" + this.mainTitle + "</title>");
				navigationWriter.write("</head><body>");
				
				//	open main table
				navigationWriter.write("<table frame=\"void\" height=\"100%\" width=\"100%\"><tr><td valign=\"top\" align=\"left\">");
				
				//	write navigation tree
				this.externalizeHelpChapterAsPage(this.rootChapter, storageDir, "./", navigationWriter);
				
				//	write content iframe
				navigationWriter.write("</td><td width=\"100%\">");
				navigationWriter.write("<iframe name=\"content\" src=\"" + (this.rootChapter.getTitle() + ".html") + "\" height=\"100%\" width=\"100%\">");
				
				//	close table and navigation page
				navigationWriter.write("</td></tr></table>");
				navigationWriter.write("</body></html>");
				
				//	close navigation page writer
				navigationWriter.flush();
				navigationWriter.close();
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(this, (e.getClass().getName() + ":\n" + e.getMessage() + "\nwhile externalizing help content."), "Error Externalizing Help", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void externalizeHelpChapterAsPage(HelpChapter chapter, File storageDir, String linkPrefix, Writer navigationWriter) throws Exception {
		
		//	produce file system and URL compatible file name
		String chapterFileName = chapter.getTitle().replaceAll("\\s", "");
		
		//	write chapter content
		BufferedWriter contentWriter = new BufferedWriter(new FileWriter(new File(storageDir, (chapterFileName + ".html"))));
		BufferedReader content = new BufferedReader(chapter.getTextReader());
		String line = content.readLine();
		while (line != null) {
			contentWriter.write(line);
			contentWriter.newLine();
			line = content.readLine();
		}
		contentWriter.flush();
		contentWriter.close();
		
		//	open navigator entry
		if (chapter != this.rootChapter) navigationWriter.write("<li>");
		navigationWriter.write("<a target=\"content\" href=\"" + (linkPrefix + chapterFileName + ".html") + "\"><font face=\"Verdana\" size=\"2\">" + chapter.getTitle().replaceAll("\\s", "&nbsp;") + "</font></a>");
		
		//	write children if given
		if (!chapter.isLeaf()) {
			
			//	open sub chapter navigator list
			if (chapter != this.rootChapter) navigationWriter.write("<ul>");
			
			//	externalize sub chapters
			File storageSubDir = new File(storageDir, (chapterFileName + "/"));
			if (!storageSubDir.exists()) storageSubDir.mkdir();
			for (int c = 0; c < chapter.getChildCount(); c++)
				this.externalizeHelpChapterAsPage(((HelpChapter) chapter.getChildAt(c)), storageSubDir, (linkPrefix + chapterFileName + "/"), navigationWriter);
			
			//	close sub chapter navigator list
			if (chapter != this.rootChapter) navigationWriter.write("</ul>");
		}
		
		//	close navigator entry
		if (chapter != this.rootChapter) navigationWriter.write("</li>");
	}
	
	private void externalizeHelpAsJDoc() {
		JFileChooser fc = new JFileChooser("Save Help Content");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setMultiSelectionEnabled(false);
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File storageDir = fc.getSelectedFile();
			if ((storageDir != null) && storageDir.isDirectory()) try {
				
				//	create writer for navigation page
				BufferedWriter navigationWriter = new BufferedWriter(new FileWriter(new File(storageDir, "Help.html")));
				
				//	create writer for content page
				BufferedWriter contentWriter = new BufferedWriter(new FileWriter(new File(storageDir, "HelpContent.html")));
				
				//	write navigation tree
				this.externalizeHelpChapterAsPage(this.rootChapter, contentWriter);
				
				//	initialize navigation page
				navigationWriter.write("<html><head>");
				navigationWriter.write("<title>" + this.mainTitle + "</title>");
				navigationWriter.write("</head><body>");
				
				//	initialize navigation page
				contentWriter.write("<html><head>");
				contentWriter.write("<title>" + this.mainTitle + "</title>");
				contentWriter.write("</head><body>");
				
				//	open main table
				navigationWriter.write("<table frame=\"void\" height=\"100%\" width=\"100%\"><tr><td valign=\"top\" align=\"left\">");
				
				//	write navigation tree
				this.externalizeHelpChapterAsJDocSection(this.rootChapter, navigationWriter, contentWriter);
				
				//	write content iframe
				navigationWriter.write("</td><td width=\"100%\">");
				navigationWriter.write("<iframe name=\"content\" src=\"HelpContent.html\" height=\"100%\" width=\"100%\">");
				
				//	close table and navigation page
				navigationWriter.write("</td></tr></table>");
				navigationWriter.write("</body></html>");
				
				//	close content page
				contentWriter.write("</body></html>");
				
				//	close navigation page writer
				navigationWriter.flush();
				navigationWriter.close();
				
				//	close content page writer
				contentWriter.flush();
				contentWriter.close();
				
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(this, (e.getClass().getName() + ":\n" + e.getMessage() + "\nwhile externalizing help content."), "Error Externalizing Help", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void externalizeHelpChapterAsJDocSection(HelpChapter chapter, final Writer navigationWriter, final Writer contentWriter) throws Exception {
		
		//	insert spacer if chapter is not root
		if (chapter != this.rootChapter)
			contentWriter.write("<hr width=\"100%\">");
		
		//	produce file system and URL compatible file name
		String chapterFileName = chapter.getTitle().replaceAll("\\s", "");
		
		//	write section head line
		contentWriter.write("<a name=\"" + chapterFileName + "\"></a>");
		contentWriter.write("<font face=\"Verdana\" size=\"5\"><b>" + chapter.getTitle() + "</b></font>");
		
		//	write sub chapter navigation line
		if (!chapter.isLeaf()) {
			
			//	open sub chapter navigator line
			contentWriter.write("<p><font face=\"Verdana\" size=\"2\">");
			contentWriter.write("<b>Sub Chapters:</b>&nbsp;");
			
			//	write navigation entries
			for (int c = 0; c < chapter.getChildCount(); c++) {
				HelpChapter subChapter = ((HelpChapter) chapter.getChildAt(c));
				String subChapterFileName = subChapter.getTitle().replaceAll("\\s", "");
				contentWriter.write("&nbsp;" + ((c == 0) ? "" : "|&nbsp;") + "<a href=\"#" + subChapterFileName + "\">" + subChapter.getTitle() + "</a>");
			}
			
			//	close sub chapter navigator line
			contentWriter.write("</font></p>");
		}
		
		//	build content parser
		TokenReceiver contentFilter = new TokenReceiver() {
			boolean inHtml = false;
			boolean inHead = false;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				String testToken = token.toLowerCase();
				if (testToken.startsWith("<html ") || testToken.startsWith("<html>")) this.inHtml = true;
				else if (testToken.equals("</html>")) this.inHtml = false;
				else if (testToken.startsWith("<head ") || testToken.startsWith("<head>")) this.inHead = true;
				else if (testToken.equals("</head>")) this.inHead = false;
				else if (this.inHtml && !this.inHead) {
					if (!testToken.startsWith("<body ") && !testToken.startsWith("<body>") && !testToken.startsWith("</body>"))
						contentWriter.write(token);
				}
			}
		};
		
		//	stream content through parser
		Parser parser = new Parser(new Html());
		Reader contentReader = chapter.getTextReader();
		parser.stream(contentReader, contentFilter);
		contentReader.close();
		
		//	write navigator entry
		if (chapter != this.rootChapter) navigationWriter.write("<li>");
		navigationWriter.write("<a target=\"content\" href=\"HelpContent.html#" + chapterFileName + "\"><font face=\"Verdana\" size=\"2\">" + chapter.getTitle().replaceAll("\\s", "&nbsp;") + "</font></a>");
		
		//	write sub chapters if given
		if (!chapter.isLeaf()) {
			
			//	open sub chapter navigator list
			if (chapter != this.rootChapter) navigationWriter.write("<ul>");
			
			//	externalize sub chapters
			for (int c = 0; c < chapter.getChildCount(); c++)
				this.externalizeHelpChapterAsJDocSection(((HelpChapter) chapter.getChildAt(c)), navigationWriter, contentWriter);
			
			//	close sub chapter navigator list
			if (chapter != this.rootChapter) navigationWriter.write("</ul>");
		}
		
		//	close navigator entry
		if (chapter != this.rootChapter) navigationWriter.write("</li>");
	}
	
	private void externalizeHelpAsPage() {
		JFileChooser fc = new JFileChooser("Save Help Content");
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		fc.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File file) {
				return ((file != null) && (file.isDirectory() || file.getName().toLowerCase().endsWith(".html")));
			}
			public String getDescription() {
				return "HTML files only";
			}
		});
		fc.setSelectedFile(new File(this.mainTitle + ".html"));
		fc.setMultiSelectionEnabled(false);
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File storageFile = fc.getSelectedFile();
			if (storageFile != null) try {
				
				//	create writer for page
				BufferedWriter contentWriter = new BufferedWriter(new FileWriter(storageFile));
				
				//	initialize navigation page
				contentWriter.write("<html><head>");
				contentWriter.write("<title>" + this.mainTitle + "</title>");
				contentWriter.write("</head><body>");
				
				//	write navigation tree
				this.externalizeHelpChapterAsPage(this.rootChapter, contentWriter);
				
				//	close page
				contentWriter.write("</body></html>");
				
				//	close page writer
				contentWriter.flush();
				contentWriter.close();
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(this, (e.getClass().getName() + ":\n" + e.getMessage() + "\nwhile externalizing help content."), "Error Externalizing Help", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void externalizeHelpChapterAsPage(HelpChapter chapter, final Writer contentWriter) throws Exception {
		
		//	insert spacer if chapter is not root
		if (chapter != this.rootChapter)
			contentWriter.write("<hr width=\"100%\">");
		
		//	produce file system and URL compatible file name
		String chapterFileName = chapter.getTitle().replaceAll("\\s", "");
		
		//	write section head line
		contentWriter.write("<a name=\"" + chapterFileName + "\"></a>");
		contentWriter.write("<font face=\"Verdana\" size=\"5\"><b>" + chapter.getTitle() + "</b></font>");
		
		//	write sub chapter navigation line
		if (!chapter.isLeaf()) {
			
			//	open sub chapter navigator line
			contentWriter.write("<p><font face=\"Verdana\" size=\"2\">");
			contentWriter.write("<b>Sub Chapters:</b>&nbsp;");
			
			//	write navigation entries
			for (int c = 0; c < chapter.getChildCount(); c++) {
				HelpChapter subChapter = ((HelpChapter) chapter.getChildAt(c));
				String subChapterFileName = subChapter.getTitle().replaceAll("\\s", "");
				contentWriter.write("&nbsp;" + ((c == 0) ? "" : "|&nbsp;") + "<a href=\"#" + subChapterFileName + "\">" + subChapter.getTitle() + "</a>");
			}
			
			//	close sub chapter navigator line
			contentWriter.write("</font></p>");
		}
		
		//	build content parser
		TokenReceiver contentFilter = new TokenReceiver() {
			boolean inHtml = false;
			boolean inHead = false;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				String testToken = token.toLowerCase();
				if (testToken.startsWith("<html ") || testToken.startsWith("<html>")) this.inHtml = true;
				else if (testToken.equals("</html>")) this.inHtml = false;
				else if (testToken.startsWith("<head ") || testToken.startsWith("<head>")) this.inHead = true;
				else if (testToken.equals("</head>")) this.inHead = false;
				else if (this.inHtml && !this.inHead) {
					if (!testToken.startsWith("<body ") && !testToken.startsWith("<body>") && !testToken.startsWith("</body>"))
						contentWriter.write(token);
				}
			}
		};
		
		//	stream content through parser
		Parser parser = new Parser(new Html());
		Reader contentReader = chapter.getTextReader();
		parser.stream(contentReader, contentFilter);
		contentReader.close();
		
		//	write sub chapters if given
		if (!chapter.isLeaf())
			for (int c = 0; c < chapter.getChildCount(); c++)
				this.externalizeHelpChapterAsPage(((HelpChapter) chapter.getChildAt(c)), contentWriter);
	}
	
	private Vector listeners = null;
	
	/**	register a helpPanelListener to be notified everytime the title or selected chapter change
	 * @param	hpl		the HelpPanelListener to be registered
	 */
	public void addHelpPanelListener(HelpPanelListener hpl) {
		if (hpl != null) {
			if (this.listeners == null) this.listeners = new Vector(2);
			this.listeners.add(hpl);
		}
	}
	
	/**	unregister a helpPanelListener
	 * @param	hpl		the HelpPanelListener to be unregistered
	 */
	public void removeHelpPanelListener(HelpPanelListener hpl) {
		if (this.listeners != null) this.listeners.remove(hpl);
	}
	
	private void notifyTitleChanged(String title) {
		if (this.listeners != null)
			for (int l = 0; l < this.listeners.size(); l++)
				((HelpPanelListener) this.listeners.get(l)).titleChanged(title);
	}
	
	private void notifyChapterSelected(HelpChapter chapter) {
		if (this.listeners != null)
			for (int l = 0; l < this.listeners.size(); l++)
				((HelpPanelListener) this.listeners.get(l)).chapterSelected(chapter);
	}
	
	private class HelpCellRenderer extends DefaultTreeCellRenderer {
		
		private Icon rootIcon = null;
		
		HelpCellRenderer(Image rootIcon) {
			String iconPackageName = HelpPanel.class.getName();
			iconPackageName = (iconPackageName.substring(0, (iconPackageName.lastIndexOf('.') + 1)) + "icons.");
			iconPackageName = iconPackageName.replace('.', '/');
			try {
				this.setClosedIcon(new ImageIcon(ImageIO.read(HelpPanel.class.getClassLoader().getResourceAsStream(iconPackageName + CLOSED_ICON_FILE_NAME))));
				this.setOpenIcon(new ImageIcon(ImageIO.read(HelpPanel.class.getClassLoader().getResourceAsStream(iconPackageName + OPEN_ICON_FILE_NAME))));
				this.setLeafIcon(new ImageIcon(ImageIO.read(HelpPanel.class.getClassLoader().getResourceAsStream(iconPackageName + LEAF_ICON_FILE_NAME))));
			} catch (IOException ioe) { /* never gonna happen, but Java don't know */ }
			this.rootIcon = new ImageIcon(rootIcon);
		}
		
		HelpCellRenderer(File dataPath) {
			this.setClosedIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage((new File(dataPath, HelpPanel.CLOSED_ICON_FILE_NAME)).toString())));
			this.setOpenIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage((new File(dataPath, HelpPanel.OPEN_ICON_FILE_NAME)).toString())));
			this.setLeafIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage((new File(dataPath, HelpPanel.LEAF_ICON_FILE_NAME)).toString())));
			this.rootIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage((new File(dataPath, HelpPanel.ROOT_ICON_FILE_NAME)).toString()));
		}
		
		HelpCellRenderer(String dataBaseUrl) throws MalformedURLException {
			if (!dataBaseUrl.endsWith("/")) dataBaseUrl += "/";
			this.setClosedIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(new URL(dataBaseUrl + HelpPanel.CLOSED_ICON_FILE_NAME))));
			this.setOpenIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(new URL(dataBaseUrl + HelpPanel.OPEN_ICON_FILE_NAME))));
			this.setLeafIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(new URL(dataBaseUrl +  HelpPanel.LEAF_ICON_FILE_NAME))));
			this.rootIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(new URL(dataBaseUrl +  HelpPanel.ROOT_ICON_FILE_NAME)));
		}
		
		/** @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
		 */
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			if ((this.rootIcon != null) && (value != null) && (value instanceof TreeNode) && (((TreeNode) value).getParent() == null))
				this.setIcon(this.rootIcon);
			return this;
		}
	}

	private class HelpSearchResult implements Comparable {
		final String chapterID;
		final int score;
		HelpSearchResult(String chapterID, int score) {
			this.chapterID = chapterID;
			this.score = score;
		}
		public int compareTo(Object o) {
			if ((o == null) || !(o instanceof HelpSearchResult)) return -1;
			return (((HelpSearchResult) o).score - this.score);
		}
	}

	private class HelpSearchResultDialog extends JDialog {
		private JList resultList;
		private String selectedResultTitle = null;
		HelpSearchResultDialog(HelpPanel parent, String title, int[] resultScores, final String[] resultTitles) {
			super(((Frame) null), ((title == null) ? "Search Results" : title), true);
			this.getContentPane().setLayout(new BorderLayout());
			
			String[] results = new String[resultTitles.length];
			for (int r = 0; r < resultTitles.length; r++)
				results[r] = ("(" + resultScores[r] + ") " + resultTitles[r]);
			
			this.resultList = new JList(results);
			this.resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			JScrollPane resultListBox = new JScrollPane(this.resultList);
			resultListBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			resultListBox.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			
			JPanel functionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			
			JButton button = new JButton("Show Chapter");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					int index = resultList.getSelectedIndex();
					if (index != -1) selectedResultTitle = resultTitles[index];
					dispose();
				}
			});
			functionPanel.add(button);
			
			button = new JButton("Cancel");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					selectedResultTitle = null;
					dispose();
				}
			});
			functionPanel.add(button);
			
			this.getContentPane().add(resultListBox, BorderLayout.CENTER);
			this.getContentPane().add(functionPanel, BorderLayout.SOUTH);
			this.setSize(new Dimension(300, 500));
			this.setResizable(true);
			this.setLocationRelativeTo(parent);
		}
		
		String getSelectedResultTitle() {
			return this.selectedResultTitle;
		}
	}
}
