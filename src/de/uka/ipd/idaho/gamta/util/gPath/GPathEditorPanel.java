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
package de.uka.ipd.idaho.gamta.util.gPath;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
 * An editor panel for GPath expressions, includng syntax check functions.
 * Through the buttons that can be passed to the constructor, the functionality
 * can be extended in arbitrary directions.
 * 
 * @see de.uka.ipd.idaho.gamta.util.gPath.GPath
 * 
 * @author sautter
 */
public class GPathEditorPanel extends JPanel implements DocumentListener {
	
	private static final int MAX_SCROLLBAR_WAIT = 200;
	
	private JTextArea editor;
	private JScrollPane editorBox;
	
	private String content = "";
	private String indent = "  ";
	
	private String fontName = "Verdana";
	private int fontSize = 12;
	private Color fontColor = Color.BLACK;
	
	private boolean dirty = false;
	
	/**	Constructor
	 */
	public GPathEditorPanel() {
		this("", new JButton[0]);
	}
	
	/**	Constructor
	 * @param	gPath	the GPath expression to display in the editor
	 */
	public GPathEditorPanel(String gPath) {
		this(gPath, new JButton[0]);
	}
	
	/**	Constructor
	 * @param	customButtons	an array of custom buttons to include in the button panel
	 */
	public GPathEditorPanel(JButton[] customButtons) {
		this("", customButtons);
	}
	
	/**	Constructor
	 * @param	gPath			the GPath expression to display in the editor
	 * @param	customButtons	an array of custom buttons to include in the button panel
	 */
	public GPathEditorPanel(String gPath, JButton[] customButtons) {
		super(new BorderLayout(), true);
		
		//	initialize editor
		this.editor = new JTextArea();
		this.editor.setEditable(true);
		
		//	wrap editor in scroll pane
		this.editorBox = new JScrollPane(this.editor);
		
		//	initialize buttons
		JButton refreshButton = new JButton("Format GPath");
		refreshButton.setBorder(BorderFactory.createRaisedBevelBorder());
		refreshButton.setPreferredSize(new Dimension(115, 21));
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				formatGPath();
			}
		});
		
		JButton validateButton = new JButton("Validate");
		validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
		validateButton.setPreferredSize(new Dimension(115, 21));
		validateButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				validateGPath();
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
		
		//	add custom buttons
		for (int b = 0; b < customButtons.length; b++) {
			gbc.gridx ++;
			buttonPanel.add(customButtons[b], gbc.clone());
		}
		
		//	put the whole stuff together
		this.add(this.editorBox, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);
		this.setContent(gPath);
	}
	
	/**	@return	the regular expression displayed in the editor (will be normalized before)
	 */
	public String getContent() {
		if (this.isDirty()) this.content = RegExUtils.normalizeRegEx(this.editor.getText());
		return this.content;
	}
	
	/**	set the content of the editor
	 * @param	gPath	the regular expression to display in the editor
	 */
	public void setContent(String gPath) {
		this.content = RegExUtils.normalizeRegEx(gPath);
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
		this.formatGPath();
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
	
	private void formatGPath() {
		String gPath = this.editor.getText();
		if ((gPath == null) || (gPath.length() == 0))
			return;
		
		final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
		final int scrollPosition = scroller.getValue();
		
		String normalizedGPath = GPath.normalizePath(gPath);
		this.editor.getDocument().removeDocumentListener(this);
		this.editor.setText(GPath.explodePath(normalizedGPath, this.indent));
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
		});
	}
	
	private void validateGPath() {
		boolean selected = true;
		String gPath = this.editor.getSelectedText();
		if ((gPath == null) || (gPath.length() == 0)) {
			gPath = this.editor.getText();
			selected = false;
		}
		String error = GPathParser.validatePath(gPath);
		if (error == null)
			JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is valid."), "GPath Validation", JOptionPane.INFORMATION_MESSAGE);
		else JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
	}
	
	private void refreshDisplay() {
		final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
		final int scrollPosition = scroller.getValue();
		
		this.editor.getDocument().removeDocumentListener(this);
		this.editor.setFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
		this.editor.setText(GPath.explodePath(this.content, "  "));
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
		});
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
