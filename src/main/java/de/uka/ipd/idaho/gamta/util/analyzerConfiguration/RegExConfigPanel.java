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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.accessories.RegExEditorPanel;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;


/**
 * Widget for editing complex regular expression patterns belonging to the
 * configuration of an Analyzer.
 * 
 * @author sautter
 */
public class RegExConfigPanel extends NameListConfigPanel {
	private JFileChooser fileChooser = null;
	private String regExName;
	private RegExEditorPanel editor;
	
	/**
	 * the default file extension for lists, namely '.regEx.txt'
	 */
	public static final String DEFAULT_FILE_EXTENSION = ".regEx.txt";

	/**
	 * Constructor for a regular expression pattern configuration panel with no
	 * attributes, using the default file extension '.regEx.txt', and allowing
	 * for creating or deleting regular expression patterns
	 * @param dataProvider the data provider to read and write regular
	 *            expression patterns from and to
	 * @param title the title for the panel
	 * @param toolTip the tool tip for the panel
	 */
	public RegExConfigPanel(AnalyzerDataProvider dataProvider, String title, String toolTip) {
		this(dataProvider, DEFAULT_FILE_EXTENSION, new NameAttributeSet[0], title, toolTip, true);
	}

	/**
	 * Constructor
	 * @param dataProvider the data provider to read and write regular
	 *            expression patterns from and to
	 * @param fileExtension the file extension to use
	 * @param attributes an array holding attributes for the regular expression
	 *            patterns
	 * @param title the title for the panel
	 * @param toolTip the tool tip for the panel
	 * @param editable allow for creating and deleting regular expression
	 *            patterns?
	 */
	public RegExConfigPanel(AnalyzerDataProvider dataProvider, String fileExtension, NameAttributeSet[] attributes, String title, String toolTip, boolean editable) {
		this(new DefaultConfigPanelDataProvider(dataProvider), DEFAULT_FILE_EXTENSION, attributes, title, toolTip, editable);
	}
	
	/**
	 * Constructor for a regular expression pattern configuration panel with no
	 * attributes, using the default file extension '.regEx.txt', and allowing
	 * for creating or deleting regular expression patterns
	 * @param dataProvider the data provider to read and write regular
	 *            expression patterns from and to
	 * @param title the title for the panel
	 * @param toolTip the tool tip for the panel
	 */
	public RegExConfigPanel(ConfigPanelDataProvider dataProvider, String title, String toolTip) {
		this(dataProvider, DEFAULT_FILE_EXTENSION, new NameAttributeSet[0], title, toolTip, true);
	}

	/**
	 * Constructor
	 * @param dataProvider the data provider to read and write regular
	 *            expression patterns from and to
	 * @param fileExtension the file extension to use
	 * @param attributes an array holding attributes for the regular expression
	 *            patterns
	 * @param title the title for the panel
	 * @param toolTip the tool tip for the panel
	 * @param editable allow for creating and deleting regular expression
	 *            patterns?
	 */
	public RegExConfigPanel(ConfigPanelDataProvider dataProvider, String fileExtension, NameAttributeSet[] attributes, String title, String toolTip, boolean editable) {
		super(dataProvider, fileExtension, attributes, title, toolTip, editable);
		
		JButton[] buttons = new JButton[1];
		buttons[0] = new JButton("Test RegEx");
		buttons[0].setBorder(BorderFactory.createRaisedBevelBorder());
		buttons[0].setPreferredSize(new Dimension(115, 21));
		buttons[0].addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				testRegEx(editor.getContent());
			}
		});
		
		this.editor = new RegExEditorPanel(buttons);
		this.editor.setSubPatternResolver(getSubPatternNameResolver());
		
		this.add(this.editor, BorderLayout.CENTER);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameListConfigPanel#nameSelected(java.lang.String)
	 */
	void nameSelected(String name) {
		if (this.editor.isDirty())
			this.storeRegEx(this.regExName, this.editor.getContent());
		this.regExName = name;
		this.editor.setContent(this.loadRegEx(this.regExName));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameListConfigPanel#getEditButtons()
	 */
	JButton[] getEditButtons() {
		JButton button = new JButton("Load File");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				loadRegEx();
			}
		});
		JButton[] ebs = {button};
		return ebs;
	}
	
	private void loadRegEx() {
		if (this.fileChooser == null)
			this.fileChooser = new JFileChooser();
		
		if (this.fileChooser.showOpenDialog(DialogFactory.getTopWindow()) == JFileChooser.APPROVE_OPTION) {
			File file = this.fileChooser.getSelectedFile();
			if ((file == null) || !file.isFile())
				return;
			try {
				String fileName = file.toString();
				fileName = fileName.substring(fileName.lastIndexOf(File.separatorChar) + 1);
				BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				StringVector list = StringVector.loadList(br);
				br.close();
				if (this.createRegEx(fileName, list.concatStrings("")))
					this.nameList.refresh();
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), (ioe.getMessage() + " while trying to load file\n" + file.toString()), "Could Not Read File", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	//	TODO simplify getting resolver
	
	private Properties getSubPatternNameResolver() {
		return new Resolver();
	}
	
	private class Resolver extends Properties {
		public String getProperty(String name, String def) {
			try {
				if (!name.endsWith(fileExtension)) name += fileExtension;
				InputStream is = dataProvider.getInputStream(name);
				StringVector rawRegEx = StringVector.loadList(is);
				is.close();
				return RegExUtils.preCompile(RegExUtils.normalizeRegEx(rawRegEx.concatStrings("\n")), this);
			}
			catch (IOException ioe) {
				return def;
			}
		}
		public String getProperty(String name) {
			return this.getProperty(name, null);
		}
	}
	
	private String loadRegEx(String regExName) {
		try {
			InputStream is = this.dataProvider.getInputStream(regExName);
			StringVector lines = StringVector.loadList(is);
			is.close();
			return lines.concatStrings("");
		}
		catch (IOException ioe) {
			return null;
		}
	}
	
	private void storeRegEx(String regExName, String regEx) {
		try {
			StringVector lines = new StringVector();
			lines.parseAndAddElements(regEx, "\n");
			OutputStream os = this.dataProvider.getOutputStream(regExName);
			lines.storeContent(os);
			os.flush();
			os.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel#updateFont(java.awt.Font)
	 */
	protected void updateFont(Font font) {
		if (this.editor == null)
			return;
		this.editor.setFontName(font.getFamily());
		this.editor.setFontSize(font.getSize());
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
			String regEx = this.editor.getContent();
			this.storeRegEx(this.regExName, regEx);
			this.editor.setContent(regEx); // necessary to reset the dirty bit
		}
		for (int a = 0; a < this.attributes.length; a++)
			this.attributes[a].commitChanges();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameListConfigPanel#createData()
	 */
	boolean createData() {
		return this.createRegEx(null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.NameListConfigPanel#cloneData()
	 */
	boolean cloneData() {
		return this.createRegEx(this.regExName);
	}
	
	private boolean createRegEx(String name) {
		return this.createRegEx(name, ((name == null) ? null : this.editor.getContent()));
	}
	
	private boolean createRegEx(String name, String regEx) {
		CreateRegExDialog cred;
		Window top = DialogFactory.getTopWindow();
		if (top instanceof Frame)
			cred = new CreateRegExDialog(((Frame) top), name, regEx);
		else if (top instanceof Dialog)
			cred = new CreateRegExDialog(((Dialog) top), name, regEx);
		else cred = new CreateRegExDialog(((Frame) null), name, regEx);
		cred.setVisible(true);
		if (cred.isCommitted()) {
			String regExName = cred.getRegExName();
			if (!regExName.endsWith(this.fileExtension))
				regExName += this.fileExtension;
			this.storeRegEx(regExName, cred.getRegEx());
			return true;
		}
		else return false;
	}
	
	private void testRegEx(String rawRegEx) {
		Properties resolver = getSubPatternNameResolver();
		String validationError = RegExUtils.validateRegEx(rawRegEx, resolver);
		if (validationError == null) {
			QueriableAnnotation data = Gamta.getTestDocument();
			if (data != null) {
				String regEx = RegExUtils.preCompile(rawRegEx, resolver);
				Annotation[] annotations = Gamta.extractAllMatches(data, regEx, 20);
				AnnotationDisplayDialog add;
				Window top = DialogFactory.getTopWindow();
				if (top instanceof Frame)
					add = new AnnotationDisplayDialog(((Frame) top), "Matches of RegEx", annotations, true);
				else if (top instanceof Dialog)
					add = new AnnotationDisplayDialog(((Dialog) top), "Matches of RegEx", annotations, true);
				else add = new AnnotationDisplayDialog(((Frame) null), "Matches of RegEx", annotations, true);
				add.setLocationRelativeTo(this);
				add.setVisible(true);
			}
		} else JOptionPane.showMessageDialog(this, "The pattern is not valid:\n" + validationError, "RegEx Validation", JOptionPane.ERROR_MESSAGE);
	}
	
	private class CreateRegExDialog extends JDialog {
		private JTextField nameField;
		private String regExName = null;
		private RegExEditorPanel editor;
		private CreateRegExDialog(Frame frame, String name, String regEx) {
			super(frame, "Create RegEx", true);
			this.init(frame, name, regEx);
		}
		private CreateRegExDialog(Dialog dialog, String name, String regEx) {
			super(dialog, "Create RegEx", true);
			this.init(dialog, name, regEx);
		}
		private void init(Window window, String name, String regEx) {
			this.nameField = new JTextField((name == null) ? "New RegEx" : name);
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
			JButton[] buttons = new JButton[1];
			buttons[0] = new JButton("Test RegEx");
			buttons[0].setBorder(BorderFactory.createRaisedBevelBorder());
			buttons[0].setPreferredSize(new Dimension(115, 21));
			buttons[0].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testRegEx(editor.getContent());
				}
			});
			
			this.editor = new RegExEditorPanel(buttons);
			this.editor.setSubPatternResolver(getSubPatternNameResolver());
			this.editor.setContent((regEx == null) ? "" : regEx);
			
			//	put the whole stuff together
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(this.nameField, BorderLayout.NORTH);
			this.getContentPane().add(this.editor, BorderLayout.CENTER);
			this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(600, 400));
			this.setLocationRelativeTo(window);
		}
		
		private boolean isCommitted() {
			return (this.regExName != null);
		}
		
		private String getRegEx() {
			return this.editor.getContent();
		}
		
		private String getRegExName() {
			return this.regExName;
		}
		
		private void cancel() {
			this.regExName = null;
			this.dispose();
		}
		
		private void create() {
			this.regExName = this.nameField.getText();
			this.dispose();
		}
	}
}
