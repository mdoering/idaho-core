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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

/**
 * Factory for dialogs. This factory returns dialogs that have the currently
 * focussed frame of dialog as their owner, thus, for instance, rendering
 * integration of anlyzers in arbitrary GUIs more natural in terms of that the
 * dialogs produced will inherit their icon and style from the owner. In
 * addition, if the owner window is closed, a non-modal dialog produced by this
 * method will be closed automatically along the way.
 * 
 * @author sautter
 */
public class DialogFactory {
	
	/**
	 * Retrieve the window on top of the hierarchy of (modal or non-modal)
	 * dialogs and frames. This is useful, for instance, for setting the
	 * location of the JOptionPane dialogs relative to the current window,
	 * instead of centering it on the screen, and at the same time saves
	 * tracking which window is currently on top.
	 * @return the window on top of the hierarchy, or null, if there is no such
	 *         window
	 */
	public static Window getTopWindow() {
		Window topWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		if (topWindow != null) return topWindow;
		
//		Disabled due to Java VM bug (InternalError in sun.awt.windows.WToolkit.eventLoop(Native Method) because of JNI flaw)
//		TODO: re-anable this once VM bug is fixed
//		Frame[] frames = Frame.getFrames();
//		LinkedList windows = new LinkedList();
//		for (int f = 0; f < frames.length; f++)
//			windows.addLast(frames[f]);
//		while (windows.size() != 0) {
//			topWindow = ((Window) windows.removeFirst());
//			Window[] subWindows = topWindow.getOwnedWindows();
//			for (int w = 0; w < subWindows.length; w++)
//				windows.add(subWindows[w]);
//		}
		
		return topWindow;
	}
	
	/**
	 * Produce a dialog with the currently focussed frame or dialog as its
	 * owner. This method first obtains the top window, and that produces a
	 * JDialog modal to that window.
	 * @param title the title for the dialog
	 * @param modal should the dialog be modal?
	 * @return a dialog with the currently focussed frame or dialog as its owner
	 */
	public static JDialog produceDialog(String title, boolean modal) {
		Window activeWindow = getTopWindow();
		final JDialog dialog;
		
		if (activeWindow instanceof Frame) {
			dialog = new JDialog(((Frame) activeWindow), title, modal);
			if (!modal) activeWindow.addWindowListener(new WindowAdapter() {
				public void windowClosed(WindowEvent we) {
					if (dialog.isVisible()) dialog.dispose();
				}
			});
		}
		
		else if (activeWindow instanceof Dialog) {
			dialog = new JDialog(((Dialog) activeWindow), title, modal);
			if (!modal) activeWindow.addWindowListener(new WindowAdapter() {
				public void windowClosed(WindowEvent we) {
					if (dialog.isVisible()) dialog.dispose();
				}
			});
		}
		
		else dialog = new JDialog(((Frame) null), title, modal);
		
		return dialog;
	}
}
