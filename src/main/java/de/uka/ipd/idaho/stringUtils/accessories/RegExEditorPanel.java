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
package de.uka.ipd.idaho.stringUtils.accessories;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;

/**
 * An editor panel for regular expressions, includng syntax check functions.
 * Through the buttons that can be passed to the constructor, the functionality
 * can be extended in arbitrary directions. This includes the meta regular
 * expressions RegExUtils is capable of handling.
 * 
 * @see de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils
 * @see de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils#preCompile(String,
 *      Properties).
 * 
 * @author sautter
 */
public class RegExEditorPanel extends JPanel implements DocumentListener {
	
	private static final int MAX_SCROLLBAR_WAIT = 200;
	
	private JTextArea editor;
	private JScrollPane editorBox;
	
	private String content = "";
	private String indent = "  ";
	
	private Properties resolver;
	
	private String fontName = "Courier New";
	private int fontSize = 12;
	private Color fontColor = Color.BLACK;
	
	private boolean dirty = false;
	
	/**	Constructor
	 */
	public RegExEditorPanel() {
		this("", new JButton[0]);
	}
	
	/**	Constructor
	 * @param	regEx	the regular expression to display in the editor
	 */
	public RegExEditorPanel(String regEx) {
		this(regEx, new JButton[0]);
	}
	
	/**	Constructor
	 * @param	customButtons	an array of custom buttons to include in the button panel
	 */
	public RegExEditorPanel(JButton[] customButtons) {
		this("", customButtons);
	}
	
	/**	Constructor
	 * @param	regEx			the regular expression to display in the editor
	 * @param	customButtons	an array of custom buttons to include in the button panel
	 */
	public RegExEditorPanel(String regEx, JButton[] customButtons) {
		super(new BorderLayout(), true);
		
		//	initialize editor
		this.editor = new JTextArea();
		this.editor.setEditable(true);
		
		//	wrap editor in scroll pane
		this.editorBox = new JScrollPane(this.editor);
		
		//	initialize buttons
		JButton refreshButton = new JButton("Format RegEx");
		refreshButton.setBorder(BorderFactory.createRaisedBevelBorder());
		refreshButton.setPreferredSize(new Dimension(115, 21));
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				formatRegEx();
			}
		});
		
		JButton validateButton = new JButton("Validate");
		validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
		validateButton.setPreferredSize(new Dimension(115, 21));
		validateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				validateRegEx();
			}
		});
		
		JButton enumerationButton = new JButton("Enumeration");
		enumerationButton.setBorder(BorderFactory.createRaisedBevelBorder());
		enumerationButton.setPreferredSize(new Dimension(115, 21));
		enumerationButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				produceEnumeration();
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
		buttonPanel.add(refreshButton, gbc.clone());
		gbc.gridx ++;
		buttonPanel.add(validateButton, gbc.clone());
		gbc.gridx ++;
		buttonPanel.add(enumerationButton, gbc.clone());
		
		//	add custom buttons
		for (int b = 0; b < customButtons.length; b++) {
			gbc.gridx ++;
			buttonPanel.add(customButtons[b], gbc.clone());
		}
		
		//	put the whole stuff together
		this.add(this.editorBox, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);
		this.setContent(regEx);
	}
	
	private void formatRegEx() {
		String regEx = this.editor.getText();
		if ((regEx == null) || (regEx.length() == 0))
			return;
		
		//	remember scrolling position
		final Point viewPosition = this.editorBox.getViewport().getViewPosition();
		
		//	normalize content
		String normalizedRegEx = RegExUtils.normalizeRegEx(regEx);
		this.editor.getDocument().removeDocumentListener(this);
		this.editor.setText(RegExUtils.explodeRegEx(normalizedRegEx, this.indent));
		this.editor.getDocument().addDocumentListener(this);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					editorBox.getViewport().setViewPosition(viewPosition);
					validate();
				} catch (RuntimeException re) {}
			}
		});
	}
	
	private void validateRegEx() {
		boolean selected = true;
		String regEx = this.editor.getSelectedText();
		if ((regEx == null) || (regEx.length() == 0)) {
			regEx = this.editor.getText();
			selected = false;
		}
		String validationError = RegExUtils.validateRegEx(regEx, this.resolver);
		if (validationError == null) JOptionPane.showMessageDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is a valid pattern.", "RegEx Validation", JOptionPane.INFORMATION_MESSAGE);
		else JOptionPane.showMessageDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is not a valid pattern:\n" + validationError, "RegEx Validation", JOptionPane.ERROR_MESSAGE);
	}
	
	private void produceEnumeration() {
		boolean selected = true;
		String regEx = this.editor.getSelectedText();
		if ((regEx == null) || (regEx.length() == 0)) {
			regEx = this.editor.getText();
			selected = false;
		}
		String validationError = RegExUtils.validateRegEx(regEx, this.resolver);
		if ((validationError == null) || (JOptionPane.showConfirmDialog(this, "The " + (selected ? "selected pattern part" : "pattern") + " is not a valid pattern.\nProduce enumeration expression anyway?", "RegEx Validation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) {
			boolean allowCommaOnly = (JOptionPane.showConfirmDialog(this, "Should the produced expression match enumerations whose elements are separated by commas only?\n(Selecting NO will produce an expression requiring the last two elements separated by a preposition)", "Allow Comma Only", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
			String enumEx = RegExUtils.produceEnumerationGroup(RegExUtils.normalizeRegEx(regEx), allowCommaOnly);
			if (selected) {
				this.editor.replaceSelection(RegExUtils.explodeRegEx(enumEx));
				this.content = RegExUtils.normalizeRegEx(this.editor.getText());
			}
			else this.content = enumEx;
			this.refreshDisplay();
		}
	}
	
	private void refreshDisplay() {
		
		//	remember scrolling position
		final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
		final int scrollPosition = scroller.getValue();
//		final Point viewPosition = this.editorBox.getViewport().getViewPosition();
		
		//	create content
		this.editor.getDocument().removeDocumentListener(this);
		this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
		this.editor.setText(RegExUtils.explodeRegEx(this.content, this.indent));
		this.editor.getDocument().addDocumentListener(this);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int scrollbarWaitCounter = 0;
				while (scroller.getValueIsAdjusting() && (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT)) try {
					Thread.sleep(10);
					scrollbarWaitCounter ++;
				} catch (Exception e) {}
				
				if (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT) {
					scroller.setValueIsAdjusting(true);
					scroller.setValue(scrollPosition);
					scroller.setValueIsAdjusting(false);
				}
				validate();
			}
//			public void run() {
//				try {
//					editorBox.getViewport().setViewPosition(viewPosition);
//					validate();
//				} catch (RuntimeException re) {}
//			}
		});
	}
	
	/**	set the Properties to use for resolving sub pattern placeholders
	 * @param	resolver	the Properties to use for resolving sub pattern placeholders
	 */
	public void setSubPatternResolver(Properties resolver) {
		this.resolver = resolver;
	}
	
	/**	@return	the regular expression displayed in the editor (will be normalized before)
	 */
	public String getContent() {
		if (this.isDirty())
			this.content = RegExUtils.normalizeRegEx(this.editor.getText());
		return this.content;
	}
	
	/**	set the content of the editor
	 * @param	regEx	the regular expression to display in the editor
	 */
	public void setContent(String regEx) {
		this.content = RegExUtils.normalizeRegEx(regEx);
		this.refreshDisplay();
		this.dirty = false;
	}
	
	/**	@return	true if and only if the content of the editor has been modified since the last call to setContent()
	 */
	public boolean isDirty() {
		return this.dirty;
	}
	
	/** @return the String currently used for indenting the regular expression in the editor
	 */
	public String getIndent() {
		return this.indent;
	}
	
	/** @param	indent	the String to use for indenting the regular expression in the editor (use space and tab characters only)
	 * @throws IllegalArgumentException if the specified String contains other characters than the space or tab character
	 */
	public void setIndent(String indent) {
		for (int i = 0; i < indent.length(); i++)
			if ((indent.charAt(i) != ' ') && (indent.charAt(i) !='\t'))
				throw new IllegalArgumentException("The indent must contain space and tab characters only.");
		this.indent = indent;
		this.formatRegEx();
	}
	
	/** @return the color currently used for the font in the editor
	 */
	public Color getFontColor() {
		return this.fontColor;
	}
	
	/** @param	fontColor	the color to use for the font in the editor
	 */
	public void setFontColor(Color fontColor) {
		this.fontColor = fontColor;
		this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
		this.editor.validate();
	}
	
	/** @return the name of the font currently used in the editor
	 */
	public String getFontName() {
		return this.fontName;
	}
	
	/** @param	fontName	the name of the font to use in the editor
	 */
	public void setFontName(String fontName) {
		this.fontName = fontName;
		this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
		this.editor.validate();
	}
	
	/** @return the size of the font currently used in the editor
	 */
	public int getFontSize() {
		return this.fontSize;
	}
	
	/** @param	fontSize	the font size to use in the editor
	 */
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
		this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
		this.editor.validate();
	}
	
	/* @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
	 */
	public void changedUpdate(DocumentEvent de) {
		//	attribute changes are not of interest for now
	}
	
	/* @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
	 */
	public void insertUpdate(DocumentEvent de) {
		this.dirty = true;
	}
	
	/* @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
	 */
	public void removeUpdate(DocumentEvent de) {
		this.dirty = true;
	}
}
