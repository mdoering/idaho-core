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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * JFrame for displaying help content. This class does little more than wrap a
 * JFrame around a HlepPanel.
 * 
 * @author sautter
 */
public class Help extends JFrame implements HelpPanelListener {
	
	private HelpPanel content;
	
	/**	Constructor
	 * @param	title			the title for the help window
	 * @param	contentRoot		the root node of the help content tree
	 */
	public Help(String title, HelpChapter contentRoot) {
		this(title, contentRoot, ((File) null));
	}
	
	/**	Constructor
	 * @param	title			the title for the help window
	 * @param	contentRoot		the root node of the help content tree
	 * @param	rootIcon		the root icon
	 */
	public Help(String title, HelpChapter contentRoot, Image rootIcon) {
		super((title == null) ? "Help" : title);
		this.content = new HelpPanel(title, contentRoot, rootIcon);
		this.content.addHelpPanelListener(this);
		
		//	adjust tree icon
		if (rootIcon != null)
			this.setIconImage(rootIcon);
		
		//	initialize
		this.init();
	}
	
	/**	Constructor
	 * @param	title			the title for the help window
	 * @param	contentRoot		the root node of the help content tree
	 * @param	dataBaseUrl		the path where icon files etc. are located
	 */
	public Help(String title, HelpChapter contentRoot, String dataBaseUrl) {
		super((title == null) ? "Help" : title);
		this.content = new HelpPanel(title, contentRoot, dataBaseUrl);
		this.content.addHelpPanelListener(this);
		
		//	adjust tree icon
		if (dataBaseUrl != null) try {
			Image icon = Toolkit.getDefaultToolkit().getImage(new URL(dataBaseUrl + HelpPanel.OPEN_ICON_FILE_NAME));
			if (icon == null) icon = Toolkit.getDefaultToolkit().getImage(new URL(dataBaseUrl + HelpPanel.CLOSED_ICON_FILE_NAME));
			if (icon != null) this.setIconImage(icon);
		} catch (MalformedURLException mue) {}
		
		//	initialize
		this.init();
	}
	
	/**	Constructor
	 * @param	title			the title for the help window
	 * @param	contentRoot		the root node of the help content tree
	 * @param	dataPath		the path where icon files etc. are located
	 */
	public Help(String title, HelpChapter contentRoot, File dataPath) {
		super((title == null) ? "Help" : title);
		this.content = new HelpPanel(title, contentRoot, dataPath);
		this.content.addHelpPanelListener(this);
		
		//	adjust tree icon
		if (dataPath != null) {
			Image icon = Toolkit.getDefaultToolkit().getImage((new File(dataPath, HelpPanel.OPEN_ICON_FILE_NAME)).toString());
			if (icon == null) icon = Toolkit.getDefaultToolkit().getImage((new File(dataPath, HelpPanel.CLOSED_ICON_FILE_NAME)).toString());
			if (icon != null) this.setIconImage(icon);
		}
		
		//	initialize
		this.init();
	}
	
	private void init() {
		
		//	create close button
		JButton closeButton = new JButton("Close");
		closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		closeButton.setPreferredSize(new Dimension(100, 23));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				dispose();
			}
		});
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(closeButton);
		
		//	put it all together
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(this.content, BorderLayout.CENTER);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		this.setSize(new Dimension(800, 600));
		this.setResizable(true);
	}
	
	/**
	 * show the Help window. If it is already on the screen, it comes to front.
	 */
	public void showHelp() {
		if (this.isShowing()) this.toFront();
		else this.setVisible(true);
	}

	/**
	 * show the Help window, switching to the chapter with the specified title.
	 * If the window is already on the screen, it comes to front.
	 * @param title the title of the chapter to switch to (null will switch to
	 *            root chapter)
	 */
	public void showHelp(String title) {
		this.showHelp();
		this.content.displayChapter(title);
	}

	/**
	 * @see de.uka.ipd.idaho.easyIO.help.HelpPanelListener#chapterSelected(de.uka.ipd.idaho.easyIO.help.HelpChapter)
	 */
	public void chapterSelected(HelpChapter chapter) {}

	/**
	 * @see de.uka.ipd.idaho.easyIO.help.HelpPanelListener#titleChanged(java.lang.String)
	 */
	public void titleChanged(String title) {
		if (title != null) this.setTitle(title);
	}
}
