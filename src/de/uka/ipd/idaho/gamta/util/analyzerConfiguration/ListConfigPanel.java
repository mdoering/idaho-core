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
package de.uka.ipd.idaho.gamta.util.analyzerConfiguration;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.accessories.FindReplaceDialog;


/**
 * Widget for editing lists belonging to the configuration of an Analyzer.
 * 
 * @author sautter
 */
public class ListConfigPanel extends NameListConfigPanel {
	private JFileChooser fileChooser = null;
	private ListEditorPanel editor;
	private String listName;
	
	/**
	 * the default file extension for lists, namely '.list.txt'
	 */
	public static final String DEFAULT_FILE_EXTENSION = ".list.txt";
	
	/**
	 * Constructor for a list configuration panel with no attributes, using the
	 * default file extension '.list.txt', and not allowing for creating or
	 * deleting lists
	 * @param dataProvider the data provider to read and write lists from and to
	 * @param title the title for the panel
	 * @param toolTip the tool tip for the panel
	 */
	public ListConfigPanel(AnalyzerDataProvider dataProvider, String title, String toolTip) {
		this(dataProvider, DEFAULT_FILE_EXTENSION, new NameAttributeSet[0], title, toolTip, true);
	}
	
	/**
	 * Constructor
	 * @param dataProvider the data provider to read and write lists from and to
	 * @param fileExtension the file extension to use
	 * @param attributes an array holding attributes for the lists
	 * @param title the title for the panel
	 * @param toolTip the tool tip for the panel
	 * @param editable allow for creating and deleting lists?
	 */
	public ListConfigPanel(AnalyzerDataProvider dataProvider, String fileExtension, NameAttributeSet[] attributes, String title, String toolTip, boolean editable) {
		this(new DefaultConfigPanelDataProvider(dataProvider), DEFAULT_FILE_EXTENSION, attributes, title, toolTip, editable);
	}
	
	/**
	 * Constructor for a list configuration panel with no attributes, using the
	 * default file extension '.list.txt', and not allowing for creating or
	 * deleting lists
	 * @param dataProvider the data provider to read and write lists from and to
	 * @param title the title for the panel
	 * @param toolTip the tool tip for the panel
	 */
	public ListConfigPanel(ConfigPanelDataProvider dataProvider, String title, String toolTip) {
		super(dataProvider, DEFAULT_FILE_EXTENSION, new NameAttributeSet[0], title, toolTip, false);
	}
	
	/**
	 * Constructor
	 * @param dataProvider the data provider to read and write lists from and to
	 * @param fileExtension the file extension to use
	 * @param attributes an array holding attributes for the lists
	 * @param title the title for the panel
	 * @param toolTip the tool tip for the panel
	 * @param editable allow for creating and deleting lists?
	 */
	public ListConfigPanel(ConfigPanelDataProvider dataProvider, String fileExtension, NameAttributeSet[] attributes, String title, String toolTip, boolean editable) {
		super(dataProvider, fileExtension, attributes, title, toolTip, editable);
		this.editor = new ListEditorPanel(null);
		this.add(this.editor, BorderLayout.CENTER);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameListConfigPanel#getEditButtons()
	 */
	JButton[] getEditButtons() {
		ArrayList editButtons = new ArrayList();
		JButton button = new JButton("Load URL");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				loadListFromURL();
			}
		});
		editButtons.add(button);
		
		button = new JButton("Load File");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				loadList();
			}
		});
		editButtons.add(button);
		
		return ((JButton[]) editButtons.toArray(new JButton[editButtons.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameListConfigPanel#createData()
	 */
	boolean createData() {
		return this.createList(null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameListConfigPanel#cloneData()
	 */
	boolean cloneData() {
		return this.createList(this.listName);
	}
	
	private boolean createList(String name) {
		return this.createList(name, ((name == null) ? null : this.editor.getContent()));
	}
	
	private boolean createList(String name, StringVector list) {
		CreateListDialog cld;
		Window top = DialogFactory.getTopWindow();
		if (top instanceof Frame)
			cld = new CreateListDialog(((Frame) top), name, list);
		else if (top instanceof Dialog)
			cld = new CreateListDialog(((Dialog) top), name, list);
		else cld = new CreateListDialog(((Frame) null), name, list);
		cld.setVisible(true);
		if (cld.isCommitted()) {
			String listName = cld.getListName();
			if (!listName.endsWith(this.fileExtension))
				listName += this.fileExtension;
			this.storeList(listName, cld.getList());
			return true;
		}
		else return false;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameListConfigPanel#nameSelected(java.lang.String)
	 */
	void nameSelected(String name) {
		if (this.editor.isDirty())
			this.storeList(this.listName, this.editor.getContent());
		this.listName = name;
		this.editor.setContent(this.loadList(this.listName));
	}
	
	private void loadList() {
		if (this.fileChooser == null)
			this.fileChooser = new JFileChooser();
		
		if (this.fileChooser.showOpenDialog(DialogFactory.getTopWindow()) == JFileChooser.APPROVE_OPTION) {
			File file = this.fileChooser.getSelectedFile();
			if ((file == null) || !file.isFile())
				return;
			try {
				String fileName = file.toString();
				fileName = fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
				this.loadList(new FileInputStream(file), fileName);
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), (ioe.getMessage() + " while trying to load list from\n" + file.toString()), "Could Not Read File", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	private void loadListFromURL() {
		Object urlObject = JOptionPane.showInputDialog(DialogFactory.getTopWindow(), "Please enter URL to load", "Enter URL", JOptionPane.QUESTION_MESSAGE, null, null, "http://");
		if (urlObject == null)
			return;
		try {
			URL url = this.dataProvider.getURL((String) urlObject);
			String fileName = StringUtils.replaceAll(url.getHost() + url.getPath(), "/", "_");
			this.loadList(url.openStream(), fileName);
		}
		catch (IOException ioe) {
			JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), (ioe.getMessage() + " while trying to load list from\n" + urlObject.toString()), "Could Not Load URL", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void loadList(InputStream source, String name) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(source));
		StringVector list = StringVector.loadList(br);
		br.close();
		if (this.createList(name, list))
			this.nameList.refresh();
	}
	
	private StringVector loadList(String name) {
		if ((name == null) || !this.dataProvider.isDataAvailable(name))
			return null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(name), "UTF-8"));
			StringVector list = StringVector.loadList(br);
			br.close();
			return list;
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
			return null;
		}
	}
	
	private void storeList(String name, StringVector list) {
		try {
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream(name), "UTF-8"));
			list.storeContent(bw);
			bw.flush();
			bw.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel#updateFont(java.awt.Font)
	 */
	protected void updateFont(Font font) {
		if (this.editor != null)
			this.editor.editor.setFont(font);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel#isDirty()
	 */
	public boolean isDirty() {
		boolean dirty = this.editor.isDirty();
		for (int a = 0; a < this.attributes.length; a++)
			dirty = (dirty || this.attributes[a].isDirty());
		return dirty;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel#commitChanges()
	 */
	public boolean commitChanges() throws IOException {
		if (!this.isDirty())
			return false;
		if (this.editor.isDirty()) {
			this.storeList(this.listName, this.editor.getContent());
			this.editor.dirty = false;
		}
		for (int a = 0; a < this.attributes.length; a++)
			this.attributes[a].commitChanges();
		return true;
	}
	
	private String selectList(String label, String button, boolean[] caseSensitive) {
		String[] listNames = this.nameList.getNames();
		if (listNames.length == 0)
			return null;
		
		Arrays.sort(listNames, String.CASE_INSENSITIVE_ORDER);
		final String[] listName = {null};
		final JDialog dialog = DialogFactory.produceDialog(label, true);
		final JList listNameList = new JList(listNames);
		listNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listNameList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				if (me.getClickCount() > 1) {
					int index = listNameList.getSelectedIndex();
					if (index != -1) {
						listName[0] = listNameList.getSelectedValue().toString();
						dialog.dispose();
					}
				}
			}
		});
		
		JCheckBox cs = new JCheckBox("Case Sensitive?");
		
		JButton commitButton = new JButton(button);
		commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
		commitButton.setPreferredSize(new Dimension(100, 21));
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				listName[0] = listNameList.getSelectedValue().toString();
				dialog.dispose();
			}
		});
		JButton abortButton = new JButton("Cancel");
		abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
		abortButton.setPreferredSize(new Dimension(100, 21));
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				dialog.dispose();
			}
		});
		
		JPanel mainButtonPanel = new JPanel(new FlowLayout());
		mainButtonPanel.add(commitButton);
		mainButtonPanel.add(abortButton);
		JPanel functionPanel = new JPanel(new BorderLayout());
		functionPanel.add(cs, BorderLayout.CENTER);
		functionPanel.add(mainButtonPanel, BorderLayout.SOUTH);
		
		dialog.getContentPane().setLayout(new BorderLayout());
		dialog.getContentPane().add(new JScrollPane(listNameList), BorderLayout.CENTER);
		dialog.getContentPane().add(functionPanel, BorderLayout.SOUTH);
		
		dialog.setResizable(true);
		dialog.setSize(new Dimension(300, Math.min(Math.max(200, (60 + (listNames.length * 25))), 500)));
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
		
		caseSensitive[0] = cs.isSelected();
		return listName[0];
	}
	
	private class ListEditorPanel extends JPanel implements DocumentListener {
		private JTextArea editor;
		private JScrollPane editorBox;
		private boolean dirty = false;
		ListEditorPanel(StringVector list) {
			super(new BorderLayout(), true);
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setFont(new Font(getFontName(), Font.PLAIN, getFontSize()));
			this.editor.setEditable(true);
			
			//	wrap editor in scroll pane
			this.editorBox = new JScrollPane(this.editor);
			this.editorBox.getVerticalScrollBar().setUnitIncrement(50);
			this.editorBox.getVerticalScrollBar().setBlockIncrement(100);
			
			//	initialize buttons
			JButton sortButton = new JButton("Sort List");
			sortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			sortButton.setPreferredSize(new Dimension(70, 21));
			sortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					sortList();
				}
			});
			
			JButton findReplaceButton = new JButton("Find / Replace");
			findReplaceButton.setBorder(BorderFactory.createRaisedBevelBorder());
			findReplaceButton.setPreferredSize(new Dimension(70, 21));
			findReplaceButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					openFindReplace();
				}
			});
			
			JButton unionButton = new JButton("Union");
			unionButton.setBorder(BorderFactory.createRaisedBevelBorder());
			unionButton.setPreferredSize(new Dimension(70, 21));
			unionButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					unionList();
				}
			});
			
			JButton intersectButton = new JButton("Intersect");
			intersectButton.setBorder(BorderFactory.createRaisedBevelBorder());
			intersectButton.setPreferredSize(new Dimension(70, 21));
			intersectButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					intersectList();
				}
			});
			
			JButton subtractButton = new JButton("Subtract");
			subtractButton.setBorder(BorderFactory.createRaisedBevelBorder());
			subtractButton.setPreferredSize(new Dimension(70, 21));
			subtractButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					subtractList();
				}
			});
			
			JPanel buttonPanel = new JPanel(new GridBagLayout(), true);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.weightx = 1;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			gbc.gridx = 0;
			buttonPanel.add(sortButton, gbc.clone());
			gbc.gridx++;
			buttonPanel.add(findReplaceButton, gbc.clone());
			gbc.gridx++;
			buttonPanel.add(unionButton, gbc.clone());
			gbc.gridx++;
			buttonPanel.add(intersectButton, gbc.clone());
			gbc.gridx++;
			buttonPanel.add(subtractButton, gbc.clone());
			
			//	put the whole stuff together
			this.add(this.editorBox, BorderLayout.CENTER);
			this.editor.getDocument().addDocumentListener(this);
			this.add(buttonPanel, BorderLayout.SOUTH);
			if (list != null)
				this.setContent(list);
		}
		
		void setContent(StringVector list) {
			this.writeListToEditor(list);
			this.dirty = false;
		}
		
		private void writeListToEditor(StringVector list) {
			try {
				this.editor.setText("");
				BufferedWriter bw = new BufferedWriter(new DocWriter(this.editor.getDocument()));
				for (int l = 0; l < list.size(); l++) {
					bw.write(list.get(l));
					bw.newLine();
				}
				bw.flush();
				bw.close();
			}
			catch (IOException ioe) {
				//  should never happen, as readers and writers are mock/wrappers, but we never know ...
				ioe.printStackTrace(System.out);
			}
		}
		
		StringVector getContent() {
			try {
				StringVector list = new StringVector();
				BufferedReader br = new BufferedReader(new DocReader(this.editor.getDocument()));
				String line;
				while ((line = br.readLine()) != null)
					list.addElement(line);
				br.close();
				return list;
			}
			catch (IOException ioe) {
				//  should never happen, as readers and writers are mock/wrappers, but we never know ...
				ioe.printStackTrace(System.out);
				return null;
			}
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		private class DocReader extends Reader {
			Document doc;
			int docPos = 0;
			DocReader(Document doc) {
				this.doc = doc;
			}
			public void close() throws IOException {
				this.doc = null;
			}
			public int read(char[] cbuf, int off, int len) throws IOException {
				if (this.doc == null)
					throw new IOException("Closed");
				int read = Math.min((this.doc.getLength() - this.docPos), len);
				if (read == 0)
					return -1;
				String str;
				try {
					str = this.doc.getText(this.docPos, read);
				}
				catch (BadLocationException ble) {
					throw new IOException("Bad Location");
				}
				str.getChars(0, str.length(), cbuf, off);
				this.docPos += read;
				return read;
			}
		}
		
		private class DocWriter extends Writer {
			Document doc;
			DocWriter(Document doc) {
				this.doc = doc;
			}
			public void flush() throws IOException {}
			public void close() throws IOException {
				this.doc = null;
			}
			public void write(char[] cbuf, int off, int len) throws IOException {
				if (this.doc == null)
					throw new IOException("Closed");
				try {
					this.doc.insertString(this.doc.getLength(), new String(cbuf, off, len), null);
				}
				catch (BadLocationException ble) {
					throw new IOException("Bad Location");
				}
			}
		}
		
		void sortList() {
			StringVector list = this.getContent();
			list.sortLexicographically(false, false);
			this.writeListToEditor(list);
		}
		
		private FindReplaceDialog findReplace = null;
		void openFindReplace() {
			if (this.findReplace == null) {
				Window top = DialogFactory.getTopWindow();
				if (top instanceof Frame)
					this.findReplace = new FindReplaceDialog(((Frame) top), this.editor);
				else if (top instanceof Dialog)
					this.findReplace = new FindReplaceDialog(((Dialog) top), this.editor);
				else this.findReplace = new FindReplaceDialog(((Frame) null), this.editor);
			}
			this.findReplace.setLocationRelativeTo(this);
			this.findReplace.setVisible(true);
			this.findReplace.toFront();
		}
		
		void unionList() {
			boolean[] caseSensitive = {true};
			String unionListName = selectList("Union with List", "Union", caseSensitive);
			StringVector unionList = loadList(unionListName);
			if (unionList == null)
				return;
			StringVector list = this.getContent();
			this.writeListToEditor(list.union(unionList, caseSensitive[0]));
		}
		
		void intersectList() {
			boolean[] caseSensitive = {true};
			String intersectListName = selectList("Intersect with List", "Intersect", caseSensitive);
			StringVector intersectList = loadList(intersectListName);
			if (intersectList == null)
				return;
			StringVector list = this.getContent();
			this.writeListToEditor(list.intersect(intersectList, caseSensitive[0]));
		}
		
		void subtractList() {
			boolean[] caseSensitive = {true};
			String subtractListName = selectList("Subtract List", "Subtract", caseSensitive);
			StringVector subtractList = loadList(subtractListName);
			if (subtractList == null)
				return;
			StringVector list = this.getContent();
			this.writeListToEditor(list.without(subtractList, caseSensitive[0]));
		}
		
		public void changedUpdate(DocumentEvent de) {
			//	attribute changes are not of interest for now
		}
		public void insertUpdate(DocumentEvent de) {
			this.dirty = true;
		}
		public void removeUpdate(DocumentEvent de) {
			this.dirty = true;
		}
	}
	
	private class CreateListDialog extends JDialog {
		private JTextField nameField;
		private String listName = null;
		private ListEditorPanel editor;
		private CreateListDialog(Frame frame, String name, StringVector list) {
			super(frame, "Create List", true);
			this.init(frame, name, list);
		}
		private CreateListDialog(Dialog dialog, String name, StringVector list) {
			super(dialog, "Create List", true);
			this.init(dialog, name, list);
		}
		private void init(Window window, String name, StringVector list) {
			this.nameField = new JTextField((name == null) ? "New List" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					create();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					cancel();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new ListEditorPanel(list);
			
			//	put the whole stuff together
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(this.nameField, BorderLayout.NORTH);
			this.getContentPane().add(this.editor, BorderLayout.CENTER);
			this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(window);
		}
		
		boolean isCommitted() {
			return (this.listName != null);
		}
		
		StringVector getList() {
			return this.editor.getContent();
		}
		
		String getListName() {
			return this.listName;
		}
		
		void cancel() {
			this.listName = null;
			this.dispose();
		}
		
		void create() {
			this.listName = this.nameField.getText();
			this.dispose();
		}
	}
}
