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
package de.uka.ipd.idaho.gamta.util.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Progress monitoring dialog.
 * 
 * @author sautter
 */
public class ProgressMonitorDialog extends ProgressMonitorPanel implements ProgressMonitorWindow {
	private JDialog dialog;
	
	/**
	 * Constructor
	 * @param topWindow the window to set the dialog modal to (null uses top
	 *            window)
	 * @param title the title for the dialog
	 */
	public ProgressMonitorDialog(Window topWindow, String title) {
		this(false, false, topWindow, title);
	}
	
	/**
	 * Constructor
	 * @param supportPauseResume add pause/resume button?
	 * @param supportAbort add abort button?
	 * @param topWindow the window to set the dialog modal to (null uses top
	 *            window)
	 * @param title the title for the dialog
	 */
	public ProgressMonitorDialog(boolean supportPauseResume, boolean supportAbort, Window topWindow, String title) {
		super(supportPauseResume, supportAbort);
		
		if (topWindow == null)
			topWindow = DialogFactory.getTopWindow();
		
		if (topWindow instanceof Dialog)
			this.dialog = new JDialog(((Dialog) topWindow), title, true);
		else if (topWindow instanceof Frame)
			this.dialog = new JDialog(((Frame) topWindow), title, true);
		else this.dialog = new JDialog(((Frame) null), title, true);
		
		this.dialog.getContentPane().setLayout(new BorderLayout());
		this.dialog.add(this, BorderLayout.CENTER);
		
		this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.Component#setSize(java.awt.Dimension)
	 */
	public void setSize(Dimension d) {
		this.dialog.setSize(d);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.Component#setSize(int, int)
	 */
	public void setSize(int width, int height) {
		this.dialog.setSize(width, height);
	}
	
	/* (non-Javadoc)
	 * @see java.awt.Window#setLocationRelativeTo(Component)
	 */
	public void setLocationRelativeTo(Component c) {
		this.dialog.setLocationRelativeTo(c);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorWindow#getSubWindow(java.awt.Window, java.lang.String, boolean, boolean)
	 */
	public ProgressMonitorWindow getSubWindow(Window topWindow, String title, boolean supportPauseResume, boolean supportAbort) {
		if (topWindow == this.dialog)
			return this;
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(supportPauseResume, supportAbort, topWindow, title);
		pmd.setButtonSize(this.getButtonSize());
		pmd.setStepLabelAlignment(this.getStepLabelAlignment());
		pmd.setInfoLabelAlignment(this.getInfoLabelAlignment());
		pmd.setInfoLineLimit(this.getInfoLineLimit());
		pmd.dialog.setSize(this.dialog.getSize());
		return pmd;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorWindow#getWindow()
	 */
	public Window getWindow() {
		return this.dialog;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorWindow#popUp(boolean)
	 */
	public void popUp(boolean block) {
		if (this.dialog.isVisible())
			return;
		if (block)
			this.dialog.setVisible(true);
		else {
			Thread put = new Thread() {
				public void run() {
					dialog.setVisible(true);
				}
			};
			put.start();
			if (SwingUtilities.isEventDispatchThread())
				return;
			while (!this.dialog.isVisible()) try {
				Thread.sleep(100);
			} catch (InterruptedException ie) {}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.swing.ProgressMonitorWindow#close()
	 */
	public void close() {
		this.dialog.dispose();
	}
}
