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

import java.awt.Window;

import de.uka.ipd.idaho.gamta.util.ProgressMonitor;

/**
 * A window that monitors the progress of some computation and displays it.
 * Typical implementors of this interface are dialogs or frames that contain a
 * progree monitor panel.
 * 
 * @author sautter
 */
public interface ProgressMonitorWindow extends ProgressMonitor {
	
	/**
	 * Retrieve the window (dialog or frame) this object represents, e.g. to
	 * open some other dialog modal to it.
	 * @return the window this object is
	 */
	public abstract Window getWindow();
	
	/**
	 * Obtain a secondary progress monitor window that is modal to the argument
	 * window and loops its updates through to this window in addition to
	 * displaying them. This method is helpful in situations when a monitored
	 * process opens some user dialog modal to the original progress monitor
	 * window and then wants to display the progress of some action a user
	 * triggered in the dialog. The returned progress monitor should be
	 * configured the same way as the one represented by this object. If the
	 * argument window is null or equal to this very window, this method should
	 * return the object implementing it.
	 * @param topWindow the current top window to set the returned progress
	 *            monitor modal to
	 * @param title the title for the returned window
	 * @param supportPauseResume support pausing/resuming the monitored process?
	 * @param supportAbort support aborting the monitored process?
	 * @return a secondary progress monitor window, bound to this one
	 */
	public abstract ProgressMonitorWindow getSubWindow(Window topWindow, String title, boolean supportPauseResume, boolean supportAbort);
	
	/**
	 * Show the progress monitor window. If the argument boolean is true, this
	 * method should only return after the window becomes invisible again;
	 * otherwise, it should return as soon as the window is visible.
	 * @param block should this method block?
	 */
	public abstract void popUp(boolean block);
	
	/**
	 * Close the progress monitor window. An invokation of this method should
	 * make any blocking invocation of popUp() return.
	 */
	public abstract void close();
}
