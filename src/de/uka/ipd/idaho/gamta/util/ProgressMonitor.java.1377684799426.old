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
 * Observer for retrieving information on some process. Implementations can also
 * pause and resume the observed process by blocking it in one of the setter
 * methods, or interrupt the process by throwing a runtime exception. If the
 * latter behavior is desired, the observed code should not catch runtime
 * exception originating from any of the progress monitor's methods.
 * 
 * @author sautter
 */
public interface ProgressMonitor {
	
	//	TODO use this everywhere, wherever there are local implementations
	
	//	TODO use this in GoldenGATE's apply document processor methods
	
	/**
	 * Dummy implementation printing steps and labels to System.out
	 */
	public static final ProgressMonitor dummy = new ProgressMonitor() {
		public void setBaseProgress(int baseProgress) {}
		public void setMaxProgress(int maxProgress) {}
		public void setProgress(int progress) {}
		public void setStep(String importStep) {
			System.out.println(importStep);
		}
		public void setInfo(String text) {
			System.out.println(text);
		}
//		public boolean supportsPauseResume() {
//			return false;
//		}
//		public boolean supportsAbort() {
//			return false;
//		}
//		public void setAbortExceptionMessage(String aem) {}
	};
	
	/**
	 * Set the processing step, indicating a major phase of the monitored
	 * process.
	 * @param step the new processing step
	 */
	public abstract void setStep(String step);
	
	/**
	 * Set the message string for indicating a specific activity or providing
	 * information about a specific state in a processing step.
	 * @param info the new info text
	 */
	public abstract void setInfo(String info);
	
	/**
	 * Set the base progress, i.e. the progress to indicate in a progress bar
	 * for invoking setProgress() with agrument zero.
	 * @param baseProgress the base progress
	 */
	public abstract void setBaseProgress(int baseProgress);
	
	/**
	 * Set the max progress, i.e. the progress to indicate in a progress bar for
	 * invoking setProgress() with agrument 100.
	 * @param maxProgress the maximum progress
	 */
	public abstract void setMaxProgress(int maxProgress);
	
	/**
	 * Set the progress in between the current base and maximum progress.
	 * @param progress the progress to indicate
	 */
	public abstract void setProgress(int progress);
//	
//	/**
//	 * Check if this progress monitor supports pausing/resuming a monitored
//	 * process in some sort of way.
//	 * @return true if the progress monitor can pause/resume the monitored
//	 *         process, false otherwise
//	 */
//	public abstract boolean supportsPauseResume();
//	
//	/**
//	 * Check if this progress monitor supports aborting a monitored process in
//	 * some sort of way.
//	 * @return true if the progress monitor can abort the monitored process,
//	 *         false otherwise
//	 */
//	public abstract boolean supportsAbort();
//	
//	/**
//	 * Set the message text of the runtime exception to throw in the monitored
//	 * process when the progress monitor aborts it. Invoking this method with a
//	 * null argument has no effect. If the supportsAbort() message returns
//	 * false, this method has no effect.
//	 * @param aem the message text of the exception to throw on abort
//	 */
//	public abstract void setAbortExceptionMessage(String aem);
}
