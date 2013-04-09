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


import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * A standard find/replace dialog for use in text editing. Can work on an
 * arbitrary JTextComponent.
 * 
 * @author sautter
 */
public class FindReplaceDialog extends JDialog {
	
	private JTextComponent display;
	
	private JTextField searchString = new JTextField();
	private JTextField replaceString = new JTextField();
	
	private JCheckBox caseSensitive = new JCheckBox("Case Sensitive", true);
	private JRadioButton backward = new JRadioButton("Back", false);
	private JRadioButton forward = new JRadioButton("Forward", true);
	
	private JButton replaceFindButton;
	
	private JLabel statusLabel = new JLabel("", JLabel.LEFT);
	
	/**
	 * Constructor
	 * @param owner the dialog this FindReplaceDialog belongs to
	 * @param display the JTextComponent to do Find/Replace on
	 */
	public FindReplaceDialog(Dialog owner, JTextComponent display) {
		super(owner, "Find / Replace", false);
		this.init(display);
	}
	
	/**
	 * Constructor
	 * @param owner the frame this FindReplaceDialog belongs to
	 * @param display the JTextComponent to do Find/Replace on
	 */
	public FindReplaceDialog(Frame owner, JTextComponent display) {
		super(owner, "Find / Replace", false);
		this.init(display);
	}
	
	private void init(JTextComponent display) {
		this.display = display;
		
		//	make sure selection is put into search String each time dialog is focussed
		this.addWindowFocusListener(new WindowFocusListener() {
			public void windowGainedFocus(WindowEvent we) {
				if (getSelectionForSearch()) replaceFindButton.setEnabled(true);
			}
			public void windowLostFocus(WindowEvent we) {}
		});
		
		//	initialize fields
		this.searchString.setBorder(BorderFactory.createLoweredBevelBorder());
		this.searchString.setPreferredSize(new Dimension(200, 23));
		this.replaceString.setBorder(BorderFactory.createLoweredBevelBorder());
		this.replaceString.setPreferredSize(new Dimension(200, 23));
		
		//	initialize buttons
		JButton findNextButton = new JButton("Find Next");
		findNextButton.setBorder(BorderFactory.createRaisedBevelBorder());
		findNextButton.setPreferredSize(new Dimension(100, 23));
		findNextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!markNext()) statusLabel.setText("String not found.");
				else replaceFindButton.setEnabled(true);
			}
		});
		this.replaceFindButton = new JButton("Replace / Find");
		this.replaceFindButton.setBorder(BorderFactory.createRaisedBevelBorder());
		this.replaceFindButton.setPreferredSize(new Dimension(100, 23));
		this.replaceFindButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				replace();
				if (!markNext()) {
					statusLabel.setText("String not found.");
					replaceFindButton.setEnabled(false);
				}
			}
		});
		JButton replaceAllButton = new JButton("Replace All");
		replaceAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
		replaceAllButton.setPreferredSize(new Dimension(100, 23));
		replaceAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				replaceAll();
				replaceFindButton.setEnabled(false);
			}
		});
		JButton closeButton = new JButton("Close");
		closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		closeButton.setPreferredSize(new Dimension(100, 23));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		//	initialize parameter fields
		ButtonGroup backwardForward = new ButtonGroup();
		backwardForward.add(this.backward);
		backwardForward.add(this.forward);
		JPanel backwardForwardPanel = new JPanel(new GridLayout(1, 2), true);
		backwardForwardPanel.add(this.backward);
		backwardForwardPanel.add(this.forward);
		
		//	initialize status label
		this.statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
		this.statusLabel.setPreferredSize(new Dimension(200, 23));
		
		//	preset serach String with selection (if any)
		this.replaceFindButton.setEnabled(this.getSelectionForSearch());
		
		//	put the whole stuff together
		this.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.top = 2;
		gbc.insets.bottom = 2;
		gbc.insets.left = 3;
		gbc.insets.right = 3;
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		
		gbc.gridwidth = 2;
		this.getContentPane().add(this.searchString, gbc.clone());
		
		gbc.gridy ++;
		this.getContentPane().add(this.replaceString, gbc.clone());
		
		gbc.gridy ++;
		gbc.gridwidth = 1;
		gbc.gridx = 0;
		this.getContentPane().add(this.caseSensitive, gbc.clone());
		gbc.gridx = 1;
		this.getContentPane().add(backwardForwardPanel, gbc.clone());
		
		gbc.gridy ++;
		gbc.gridx = 0;
		this.getContentPane().add(findNextButton, gbc.clone());
		gbc.gridx = 1;
		this.getContentPane().add(this.replaceFindButton, gbc.clone());
		
		gbc.gridy ++;
		gbc.gridx = 0;
		this.getContentPane().add(replaceAllButton, gbc.clone());
		gbc.gridx = 1;
		this.getContentPane().add(closeButton, gbc.clone());
		
		gbc.gridy ++;
		gbc.gridwidth = 2;
		gbc.gridx = 0;
		this.getContentPane().add(this.statusLabel, gbc.clone());
		
		this.setSize(250, 190);
		this.setResizable(false);
	}
	
	private boolean getSelectionForSearch() {
		Caret caret = this.display.getCaret();
		if (caret.getMark() != caret.getDot()) {
			this.searchString.setText(this.display.getSelectedText());
			return true;
		}
		else return false;
	}
	
	private boolean markNext() {
		Caret caret = this.display.getCaret();
		Document document = this.display.getDocument();
		try {
			
			//	search forward
			if (this.forward.isSelected()) {
				String search = this.searchString.getText();
				if (search.length() == 0)
					return false;
				
				int startPos = Math.max(caret.getDot(), caret.getMark());
				String text = document.getText(startPos, (document.getLength() - startPos));
				
				int offset = -1;
				if (this.caseSensitive.isSelected())
					offset = text.indexOf(search);
				else offset = text.toLowerCase().indexOf(search.toLowerCase());
				if (offset == -1)
					return false;
				
				caret = this.display.getCaret();
				caret.setDot(startPos + offset);
				caret.moveDot(startPos + offset + search.length());
				
				return true;
			}
			
			//	search backward
			else if (this.backward.isSelected()) {
				String search = this.searchString.getText();
				if (search.length() == 0)
					return false;
				
				int startPos = Math.min(caret.getDot(), caret.getMark());
				String text = document.getText(0, startPos);
				
				int offset = -1;
				if (this.caseSensitive.isSelected())
					offset = text.lastIndexOf(search);
				else offset = text.toLowerCase().lastIndexOf(search.toLowerCase());
				if (offset == -1)
					return false;
				
				caret = this.display.getCaret();
				caret.setDot(offset);
				caret.moveDot(offset + search.length());
				
				return true;
			}
			else return false;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	private boolean replace() {
		Caret caret = this.display.getCaret();
		if (caret.getMark() != caret.getDot()) {
			String replace = this.replaceString.getText();
			this.display.replaceSelection(replace);
			return true;
		}
		else return false;
	}
	
	private void replaceAll() {
		int count = 0;
		while (markNext() && replace())
			count++;
		this.statusLabel.setText(count + " occurrences replaced.");
	}
}
