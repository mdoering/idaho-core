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

/**
 * Progress monitor that is able to pause/resume and to abort the process it
 * observers. Implementations should pause and resume the observed process by
 * blocking it in one of the setter methods, or interrupt the process by
 * throwing a runtime exception. If the latter behavior is desired, the observed
 * code should not catch runtime exception originating from any of the progress
 * monitor's methods.
 * 
 * @author sautter
 */
public interface ControllingProgressMonitor extends ProgressMonitor {
	
	/**
	 * Check if this progress monitor supports pausing/resuming a monitored
	 * process in some sort of way.
	 * @return true if the progress monitor can pause/resume the monitored
	 *         process, false otherwise
	 */
	public abstract boolean supportsPauseResume();
	
	/**
	 * Enable or disable the pause/resume functionality of this progress
	 * monitor. This method only has an effect if supportsPauseResume() returns
	 * true.
	 * @param pre the enabled flag for pause/resume
	 */
	public abstract void setPauseResumeEnabled(boolean pre);
	
	/**
	 * Check if this progress monitor supports aborting a monitored process in
	 * some sort of way.
	 * @return true if the progress monitor can abort the monitored process,
	 *         false otherwise
	 */
	public abstract boolean supportsAbort();
	
	/**
	 * Enable or disable the abort functionality of this progress monitor. This
	 * method only has an effect if supportsAbort() returns true.
	 * @param ae the enabled flag for abort
	 */
	public abstract void setAbortEnabled(boolean ae);
	
	/**
	 * Set the message text of the runtime exception to throw in the monitored
	 * process when the progress monitor aborts it. Invoking this method with a
	 * null argument has no effect. If the supportsAbort() message returns
	 * false, this method has no effect.
	 * @param aem the message text of the exception to throw on abort
	 */
	public abstract void setAbortExceptionMessage(String aem);
}
