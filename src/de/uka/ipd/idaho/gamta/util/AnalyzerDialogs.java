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
package de.uka.ipd.idaho.gamta.util;


import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;

/**
 * Utility methods for dialogs of interactive Analyzers, in particular for
 * controlling what frame or dialog the analyzer's dialog is modal to.
 * 
 * @author sautter
 */
public class AnalyzerDialogs {
	
	private static Vector parentWindows = new Vector();
	
	/**	set the owner for dialogs returned by the produceDialog() factory method
	 * @param	parent	the JDialog to own dialogs from now on
	 */
	public static void setDialogParent(JDialog parent) {
		cleanup(parent);
		if (parent != null)
			parentWindows.add(parent);
	}
	
	/**	set the owner for dialogs returned by the produceDialog() factory method
	 * @param	parent	the JFrame to own dialogs from now on
	 */
	public static void setDialogParent(JFrame parent) {
		cleanup(parent);
		if (parent != null)
			parentWindows.add(parent);
	}
	
	//	remove windows not visible any more, and to-be-added window, so it is moved to the end
	private static void cleanup(Object toAdd) {
		for (int p = 0; p < parentWindows.size();) {
			Object po = parentWindows.get(p);
			
			if (po == toAdd)
				parentWindows.remove(p);
			
			else if (po instanceof JDialog) {
				if (((JDialog) po).isVisible()) p++;
				else parentWindows.remove(p);
			}
			else if (po instanceof JFrame) {
				if (((JFrame) po).isVisible()) p++;
				else parentWindows.remove(p);
			}
			else parentWindows.remove(p);
		}
	}
	
	/**	produce a JDialog owned by the last JDialog or JFrame submitted through one of the setDialogParent() methods
	 * @param	title	the title for the dialog
	 * @param	modal	make the dialog modal or not?
	 * @return a JDialog owned by the current dialog parent
	 */
	public static JDialog produceDialog(String title, boolean modal) {
		Object po = (parentWindows.isEmpty() ? null : parentWindows.lastElement());
		
		if (po instanceof JDialog)
			return new JDialog(((JDialog) po), title, modal);
		
		else if (po instanceof JFrame)
			return new JDialog(((JFrame) po), title, modal);

		else return new JDialog(((JFrame) null), title, modal);
	}
}
