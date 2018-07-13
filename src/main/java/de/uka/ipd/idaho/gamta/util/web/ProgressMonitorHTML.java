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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.gamta.util.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.util.ControllingProgressMonitor;

/**
 * This class helps project a progress monitor into an HTML page. It generates
 * the HTML and JavaSCript code required for showing status and progress
 * updates for longer-running processes to users. Catching and responding to
 * the corresponding HTTP requests is up to client code, however.
 * 
 * @author sautter
 */
public class ProgressMonitorHTML implements ControllingProgressMonitor {
	
	private String step = "";
	
	private ArrayList infoLines = new ArrayList();
	private int infoLineLimit = defaultInfoLineLimit;
	
	private int baseProgress = 0;
	private int maxProgress = 100;
	private int progress = 0;
	
	private final int refreshRateMillis;
	
	private final boolean pauseResumeSupported;
	private boolean pauseResumeEnabled;
	private String pauseResumeText = null;
	private boolean pause = false;
	private final Object pauseLock;
	
	private final boolean abortSupported;
	private boolean abortEnabled;
	private String abortText = null;
	private boolean abort = false;
	
	private String abortExceptionMessage = "Aborted at user's request.";
	
	/**
	 * Constructor
	 */
	public ProgressMonitorHTML() {
		this(defaultRefreshRateMillis, false, false);
	}
	
	/**
	 * Constructor
	 * @param refreshRateMillis the number of milliseconds to wait between two
	 *            status updates
	 */
	public ProgressMonitorHTML(int refreshRateMillis) {
		this(refreshRateMillis, false, false);
	}
	
	/**
	 * Constructor
	 * @param supportPauseResume add button for pausing/resuming the monitored
	 *            process?
	 * @param supportAbort add button for aborting the monitored process?
	 */
	public ProgressMonitorHTML(boolean supportPauseResume, boolean supportAbort) {
		this(defaultRefreshRateMillis, supportPauseResume, supportAbort);
	}
	
	/**
	 * Constructor
	 * @param refreshRateMillis the number of milliseconds to wait between two
	 *            status updates
	 * @param supportPauseResume add button for pausing/resuming the monitored
	 *            process?
	 * @param supportAbort add button for aborting the monitored process?
	 */
	public ProgressMonitorHTML(int refreshRateMillis, boolean supportPauseResume, boolean supportAbort) {
		this.refreshRateMillis = refreshRateMillis;
		this.pauseResumeSupported = supportPauseResume;
		this.pauseResumeEnabled = supportPauseResume;
		this.pauseResumeText = (supportPauseResume ? "Pause" : null);
		this.abortSupported = supportAbort;
		this.abortEnabled = supportAbort;
		this.abortText = (supportAbort ? "Abort" : null);
		this.pauseLock = ((supportPauseResume || supportAbort) ? new Object() : null);
	}
	
	/**
	 * @return the maximum number of info messages to display
	 */
	public int getInfoLineLimit() {
		return this.infoLineLimit;
	}
	
	/**
	 * Set the maximum number of info messages to display. If this number is
	 * greater than 1, the panel displays the last <code>infoLineLimit</code>
	 * strings handed to the setInfo() method, latest one at the bottom. Calling
	 * this method with an argument &lt;1 has no effect.
	 * @param infoLineLimit the maximum number of info messages to display
	 */
	public void setInfoLineLimit(int infoLineLimit) {
		if (infoLineLimit < 1)
			return;
		this.infoLineLimit = infoLineLimit;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#supportsPauseResume()
	 */
	public boolean supportsPauseResume() {
		return this.pauseResumeSupported;
	}
	private void pauseResume() {
		if (this.pause)
			synchronized(this.pauseLock) {
				this.pause = false;
				this.pauseResumeText = "Pause";
				this.pauseLock.notify();
			}
		else {
			this.pause = true;
			this.pauseResumeText = "Pausing ...";
			this.pauseResumeEnabled = false;
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ControllingProgressMonitor#setPauseResumeEnabled(boolean)
	 */
	public void setPauseResumeEnabled(boolean pre) {
		if (this.pauseResumeSupported)
			this.pauseResumeEnabled = pre;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#supportsAbort()
	 */
	public boolean supportsAbort() {
		return this.abortSupported;
	}
	void abort() {
		synchronized(pauseLock) {
			this.abort = true;
			this.abortEnabled = false;
			this.abortText = "Aborting ...";
			if (this.pauseResumeSupported)
				this.pauseResumeEnabled = false;
			this.pauseLock.notify();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ControllingProgressMonitor#setAbortEnabled(boolean)
	 */
	public void setAbortEnabled(boolean ae) {
		if (this.abortSupported)
			this.abortEnabled = ae;
	}
	
	private final void checkPauseAbort() {
		if (this.pauseLock == null)
			return;
		
		if (this.pause)
			synchronized (this.pauseLock) {
				this.pauseResumeEnabled = true;
				this.pauseResumeText = "Resume";
				try {
					this.pauseLock.wait();
				} catch (InterruptedException ie) {}
			}
		
		if (this.abort)
			throw new RuntimeException(this.abortExceptionMessage);
	}
	
	/**
	 * @return the message text of the exception thrown when the abort button is
	 *         clicked
	 */
	public String getAbortExceptionMessage() {
		return this.abortExceptionMessage;
	}
	
	/**
	 * Set the message text of the runtime exception to throw in the monitored
	 * process when the abort button is clicked. Invoking this method with a
	 * null argument has no effect. If this panel has been constructed not to
	 * display an abort button, this method has no effect.
	 * @param exceptionText the message text of the exception to throw on abort
	 * @see de.uka.ipd.idaho.gamta.util.ControllingProgressMonitor#setAbortExceptionMessage(java.lang.String)
	 */
	public void setAbortExceptionMessage(String aem) {
		if (aem != null)
			this.abortExceptionMessage = aem;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setStep(java.lang.String)
	 */
	public void setStep(String step) {
		this.step = step;
		this.checkPauseAbort();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setInfo(java.lang.String)
	 */
	public void setInfo(String info) {
		this.infoLines.add(info);
		while (this.infoLines.size() > this.infoLineLimit)
			this.infoLines.remove(0);
		this.checkPauseAbort();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setBaseProgress(int)
	 */
	public void setBaseProgress(int baseProgress) {
		this.baseProgress = baseProgress;
		this.checkPauseAbort();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setMaxProgress(int)
	 */
	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
		this.checkPauseAbort();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setProgress(int)
	 */
	public void setProgress(int progress) {
		this.progress = (this.baseProgress + (((this.maxProgress - this.baseProgress) * progress) / 100));
		this.checkPauseAbort();
	}
	
	/**
	 * Perform an action that came in with the request for a status script,
	 * namely one of 'ABORT' and 'PAUSE-RESUME'.
	 * @param action the action to handle
	 */
	public void performAction(String action) {
		if (this.abortSupported && this.abortEnabled &&  "ABORT".equalsIgnoreCase(action))
			this.abort();
		else if (this.pauseResumeSupported && this.pauseResumeEnabled &&  "PAUSE-RESUME".equalsIgnoreCase(action))
			this.pauseResume();
	}
	
	/**
	 * Retrieve a series of JavaScript calls that, executed in the HTML page
	 * generated by the <code>writeHtml()</code> method, make said HTML page
	 * reflect the current status of the progress monitor. Client code has to
	 * return these calls as the content of the JavaScript it produces for the
	 * update script URL it hands to the <code>writeJavaScripts()</code>
	 * method.
	 * @return an array of JavaScript calls.
	 */
	public String[] getStatusUpdateCalls() {
		ArrayList sucList = new ArrayList(6);
		sucList.add("pmSetStep('" + escapeForJavaScript(this.step) + "');");
		sucList.add("pmSetInfo(" + this.getInfoArray() + ");");
		sucList.add("pmSetProgress(" + this.progress + ");");
		if (this.pauseResumeSupported)
			sucList.add("pmSetPauseResumeStatus('" + escapeForJavaScript(this.pauseResumeText) + "', " + this.pauseResumeEnabled + ");");
		if (this.abortSupported)
			sucList.add("pmSetAbortStatus('" + escapeForJavaScript(this.abortText) + "', " + this.abortEnabled + ");");
		sucList.add("pmTriggerStatusUpdate();");
		return ((String[]) sucList.toArray(new String[sucList.size()]));
	}
	
	private String getInfoArray() {
		String[] infoLines = ((String[]) this.infoLines.toArray(new String[this.infoLines.size()]));
		StringBuffer infoArray = new StringBuffer("[");
		for (int l = 0; l < infoLines.length; l++) {
			if (l != 0)
				infoArray.append(", ");
			infoArray.append("'" + escapeForJavaScript(infoLines[l]) + "'");
		}
		infoArray.append("]");
		return infoArray.toString();
	}
	
	private static String escapeForJavaScript(String str) {
		if (str == null)
			return null;
		StringBuffer eStr = new StringBuffer();
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (ch == '\r')
				eStr.append("\\r");
			else if (ch == '\n')
				eStr.append("\\n");
			else {
				if ((ch == '\'') || (ch == '\\'))
					eStr.append('\\');
				eStr.append(ch);
			}
		}
		return eStr.toString();
	}
	
	/**
	 * Retrieve a JavaScript call showing an error message in the progress
	 * monitor HTML page. The error message replaces info area, and replaces
	 * the pause/resume and abort buttons (if any) with an OK button.
	 * @param errorMessage the error message proper
	 * @return a JavaScript call showing the error message
	 */
	public String getErrorUpdateCall(String errorMessage) {
		return this.getErrorUpdateCall(null, errorMessage, null, null);
	}
	
	/**
	 * Retrieve a JavaScript call showing an error message in the progress
	 * monitor HTML page. The error message replaces title and info area, and
	 * replaces the pause/resume and abort buttons (if any) with an OK button.
	 * If the <code>title</code> is null, no title is displayed.
	 * @param errorTitle the title for the error message
	 * @param errorMessage the error message proper
	 * @return a JavaScript call showing the error message
	 */
	public String getErrorUpdateCall(String errorTitle, String errorMessage) {
		return this.getErrorUpdateCall(errorTitle, errorMessage, null, null);
	}
	
	/**
	 * Retrieve a JavaScript call showing an error message in the progress
	 * monitor HTML page. The error message replaces title and info area, and
	 * replaces the pause/resume and abort buttons (if any) with an OK button.
	 * If the <code>closeForwardUrl</code> is null, a click on said button
	 * calls window.close(); otherwise, it forwards to the argument URL. If the
	 * <code>title</code> is null, no title is displayed.
	 * @param errorTitle the title for the error message
	 * @param errorMessage the error message proper
	 * @param closeForwardUrl the URL to forward to on closing the error
	 *            message
	 * @return a JavaScript call showing the error message
	 */
	public String getErrorUpdateCall(String errorTitle, String errorMessage, String closeForwardUrl) {
		return this.getErrorUpdateCall(errorTitle, errorMessage, null, null);
	}
	
	/**
	 * Retrieve a JavaScript call showing an error message in the progress
	 * monitor HTML page. The error message replaces title and info area, and
	 * replaces the pause/resume and abort buttons (if any) with an OK button.
	 * If the <code>closeForwardUrl</code> is null, a click on said button
	 * calls window.close(); otherwise, it forwards to the argument URL. If the
	 * <code>iconSrcUrl</code> is null, no icon is displayed. If the
	 * <code>title</code> is null, no title is displayed.
	 * @param errorTitle the title for the error message
	 * @param errorMessage the error message proper
	 * @param closeForwardUrl the URL to forward to on closing the error
	 *            message
	 * @param iconSrcUrl the source URL of the error icon to display
	 * @return a JavaScript call showing the error message
	 */
	public String getErrorUpdateCall(String errorTitle, String errorMessage, String closeForwardUrl, String iconSrcUrl) {
		return ("pmShowError(" + 
				((errorTitle == null) ? "null" : ("'" + escapeForJavaScript(errorTitle) + "'")) + "," +
				" '" + escapeForJavaScript(errorMessage) + "'," +
				" " + ((closeForwardUrl == null) ? "null" : ("'" + escapeForJavaScript(closeForwardUrl) + "'")) + "," +
				" " + ((iconSrcUrl == null) ? "null" : ("'" + escapeForJavaScript(iconSrcUrl) + "'")) +
				");");
	}
	
	/**
	 * Write the HTML code of the progress monitor page.
	 * @param out the writer to write to
	 * @param title the title for the progress monitor
	 * @param statusUpdateScriptUrl the URL of the update script
	 * @throws IOException
	 */
	public void writeHtml(Writer out, String title) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		
		//	open main div
		bw.write("<div class=\"progressMonitor\">");bw.newLine();
		
		//	add title area
		bw.write("<div id=\"pmTitle\" class=\"progressMonitorTitle\">" + AnnotationUtils.escapeForXml(title) + "</div>");bw.newLine();
		
		//	add step area
		bw.write("<div id=\"pmStep\" class=\"progressMonitorStep\">STEP</div>");bw.newLine();
		
		//	add info area
		bw.write("<div id=\"pmInfo\" class=\"progressMonitorInfo\">");bw.newLine();
		for (int l = 0; l < this.infoLineLimit; l++) {
			bw.write("<div id=\"pmInfoLine" + l + "\" class=\"progressMonitorInfoLine\">INFO</div>");bw.newLine();
		}
		bw.write("</div>");bw.newLine();
		
		//	add progress bar
		bw.write("<div id=\"pmProgressBar\" class=\"progressMonitorProgressBar\">");bw.newLine();
		bw.write("<div id=\"pmProgressIndicator\" class=\"progressMonitorProgressIndicator\" style=\"width: " + this.progress + "%; overflow: 'hidden';\">" + this.progress + "%</div>");bw.newLine();
		bw.write("</div>");bw.newLine();
		
		//	add buttons
		bw.write("<div id=\"pmButtons\" class=\"progressMonitorButtons\">");bw.newLine();
		if (this.pauseResumeSupported) {
			bw.write("<button id=\"pmPauseResumeButton\" class=\"progressMonitorButton\" onclick=\"return getStatusUpdateScript('&action=PAUSE-RESUME');\">Pause</button>");bw.newLine();
		}
		if (this.abortSupported) {
			bw.write("<button id=\"pmAbortButton\" class=\"progressMonitorButton\" onclick=\"return getStatusUpdateScript('&action=ABORT');\">Abort</button>");bw.newLine();
		}
		bw.write("</div>");bw.newLine();
		
		//	close main div, add dynamic script node, and flush writer
		bw.write("</div>");bw.newLine();
		bw.write("<script id=\"pmStatusUpdateScript\" type=\"text/javascript\" src=\"toBeSetDynamically\"></script>");bw.newLine();
		bw.write("<script type=\"text/javascript\">window.setTimeout('getStatusUpdateScript(null)', " + this.refreshRateMillis + ");</script>");bw.newLine();
		if (bw != out)
			bw.flush();
	}
	
	/**
	 * Write JavaScript functions required for the progress monitor HTML page
	 * to work to some writer. Client code is recommended to have this code
	 * written to the page header, but it also works in the page body. In the
	 * latter, however, it has to be written before the corresponding HTML
	 * code. The update script URL is used for periodic callbacks fetching
	 * progress information updates to the HTML page.
	 * @param out the writer to write to
	 * @param statusUpdateScriptUrl the URL of the update script
	 * @throws IOException
	 */
	public void writeJavaScripts(Writer out, String statusUpdateScriptUrl) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		
		//	open script
		bw.write("<script type=\"text/javascript\">");bw.newLine();
		
		//	(re-) get status update script
		bw.write("function getStatusUpdateScript(params) {");bw.newLine();
		bw.write("  var sus = getById('pmStatusUpdateScript');");bw.newLine();
		bw.write("  var susp = sus.parentNode;");bw.newLine();
		bw.write("  removeElement(sus);");bw.newLine();
		bw.write("  var susSrc = ('" + statusUpdateScriptUrl + "?time=' + (new Date()).getTime() + ((params == null) ? '' : params));");bw.newLine();
		bw.write("  sus = newElement('script', 'pmStatusUpdateScript');");bw.newLine();
		bw.write("  sus.type = 'text/javascript';");bw.newLine();
		bw.write("  sus.src = susSrc;");bw.newLine();
		bw.write("  susp.appendChild(sus);");bw.newLine();
		bw.write("  return false;");bw.newLine();
		bw.write("}");bw.newLine();
		
		//	trigger re-fetch of status update script
		bw.write("var pmLastStatusUpdate = 0;");bw.newLine();
		bw.write("function pmTriggerStatusUpdate() {");bw.newLine();
		bw.write("  var time = (new Date()).getTime();");bw.newLine();
		bw.write("  var timeToWait = (" + this.refreshRateMillis + " - (time - pmLastStatusUpdate));");bw.newLine();
		bw.write("  pmLastStatusUpdate = time;");bw.newLine();
		bw.write("  if (timeToWait <= 0)");bw.newLine();
		bw.write("    return getStatusUpdateScript(null);");bw.newLine();
		bw.write("  else {");bw.newLine();
		bw.write("    window.setTimeout('getStatusUpdateScript(null)', timeToWait);");bw.newLine();
		bw.write("    return false;");bw.newLine();
		bw.write("  }");bw.newLine();
		bw.write("}");bw.newLine();
		
		//	update processing step
		bw.write("function pmSetStep(step) {");bw.newLine();
		bw.write("  getById('pmStep').innerHTML = step;");bw.newLine();
		bw.write("}");bw.newLine();
		
		//	update info lines
		bw.write("function pmSetInfo(infoArray) {");bw.newLine();
		bw.write("  for (var l = 0; l < " + this.infoLineLimit + "; l++) {");bw.newLine();
		bw.write("    var il = getById('pmInfoLine' + l);");bw.newLine();
		bw.write("    if (il != null)");bw.newLine();
		bw.write("      il.innerHTML = ((l < infoArray.length) ? infoArray[l] : '');");bw.newLine();
		bw.write("  }");bw.newLine();
		bw.write("}");bw.newLine();
		
		//	update progress bar
		bw.write("function pmSetProgress(progress) {");bw.newLine();
		bw.write("  if (progress < 0)");bw.newLine();
		bw.write("    progress = 0;");bw.newLine();
		bw.write("  else if (progress > 100)");bw.newLine();
		bw.write("    progress = 100;");bw.newLine();
		bw.write("  getById('pmProgressIndicator').style.width = (progress + '%');");bw.newLine();
		bw.write("  getById('pmProgressIndicator').innerHTML = (progress + '%');");bw.newLine();
		bw.write("}");bw.newLine();

		//	update state of pause/resume button
		if (this.pauseResumeSupported) {
			bw.write("function pmSetPauseResumeStatus(pauseResumeText, pauseResumeEnabled) {");bw.newLine();
			bw.write("  var prb = getById('pmPauseResumeButton');");bw.newLine();
			bw.write("  if (prb != null) {");bw.newLine();
			bw.write("    prb.innerHTML = pauseResumeText;");bw.newLine();
			bw.write("    prb.disabled = !pauseResumeEnabled;");bw.newLine();
			bw.write("  }");bw.newLine();
			bw.write("}");bw.newLine();
		}
		
		//	update status of abort button
		if (this.abortSupported) {
			bw.write("function pmSetAbortStatus(abortText, abortEnabled) {");bw.newLine();
			bw.write("  var ab = getById('pmAbortButton');");bw.newLine();
			bw.write("  if (ab != null) {");bw.newLine();
			bw.write("    ab.innerHTML = abortText;");bw.newLine();
			bw.write("    ab.disabled = !abortEnabled;");bw.newLine();
			bw.write("  }");bw.newLine();
			bw.write("}");bw.newLine();
		}
		
		//	show error message
		bw.write("function pmShowError(errorTitle, errorMessage, closeForwardUrl, iconSrcUrl) {");bw.newLine();
		//	change or hide title
		bw.write("  var td = getById('pmTitle');");bw.newLine();
		bw.write("  if (errorTitle == null)");bw.newLine();
		bw.write("    td.style.display = 'none';");bw.newLine();
		bw.write("  else td.innerHTML = errorTitle;");bw.newLine();
		//	hide step
		bw.write("  getById('pmStep').style.display = 'none';");bw.newLine();
		//	hide progress bar
		bw.write("  getById('pmProgressBar').style.display = 'none';");bw.newLine();
		//	clean info
		bw.write("  var id = getById('pmInfo');");bw.newLine();
		bw.write("  while (id.firstElementChild)");bw.newLine();
		bw.write("    id.removeChild(id.firstElementChild);");bw.newLine();
		//	build message
		bw.write("  var messageDiv = newElement('div', null, 'progressMonitorErrorMessage', null);");bw.newLine();
		bw.write("  var messageParts = errorMessage.split(/(\\r\\n|\\r|\\n)/);");bw.newLine();
		bw.write("  for (var p = 0; p < messageParts.length; p++) {");bw.newLine();
		bw.write("    if (p != 0)");bw.newLine();
		bw.write("      messageDiv.appendChild(newElement('br', null, null, null));");bw.newLine();
		bw.write("    messageDiv.appendChild(document.createTextNode(messageParts[p]));");bw.newLine();
		bw.write("  }");bw.newLine();
		//	simply add message if we don't have an icon
		bw.write("  if (iconSrcUrl == null)");bw.newLine();
		bw.write("    id.appendChild(messageDiv);");bw.newLine();
		//	add message and icon in table to keep icon to right of message
		bw.write("  else {");bw.newLine();
		bw.write("    var icon = newElement('img', null, 'progressMonitorErrorIcon', null);");bw.newLine();
		bw.write("    icon.src = iconSrcUrl;");bw.newLine();
		bw.write("    var iconDiv = newElement('div', null, 'progressMonitorErrorIcon', null);");bw.newLine();
		bw.write("    iconDiv.appendChild(icon);");bw.newLine();
		bw.write("    var emtd = newElement('td', null, null, null);");bw.newLine();
		bw.write("    emtd.appendChild(messageDiv);");bw.newLine();
		bw.write("    var eitd = newElement('td', null, null, null);");bw.newLine();
		bw.write("    eitd.appendChild(iconDiv);");bw.newLine();
		bw.write("    var etr = newElement('tr', null, null, null);");bw.newLine();
		bw.write("    etr.appendChild(emtd);");bw.newLine();
		bw.write("    etr.appendChild(eitd);");bw.newLine();
		bw.write("    var et = newElement('table', null, null, null);");bw.newLine();
		bw.write("    et.appendChild(etr);");bw.newLine();
		bw.write("    id.appendChild(et);");bw.newLine();
		bw.write("  }");bw.newLine();
		//	hide existing buttons
		bw.write("  var prb = getById('pmPauseResumeButton');");bw.newLine();
		bw.write("  if (prb != null)");bw.newLine();
		bw.write("    prb.style.display = 'none';");bw.newLine();
		bw.write("  var ab = getById('pmAbortButton')");bw.newLine();
		bw.write("  if (ab != null)");bw.newLine();
		bw.write("    ab.style.display = 'none';");bw.newLine();
		//	add OK button
		bw.write("  var eob = newElement('button', 'pmErrorOkButton', 'progressMonitorButton', 'OK');");bw.newLine();
		bw.write("  eob.onclick = function() {");bw.newLine();
		bw.write("    if (closeForwardUrl == null)");bw.newLine();
		bw.write("      window.close();");bw.newLine();
		bw.write("    else window.location.href = closeForwardUrl;");bw.newLine();
		bw.write("  };");bw.newLine();
		bw.write("  getById('pmButtons').appendChild(eob);");bw.newLine();
		bw.write("}");bw.newLine();
		
		//	close script and flush writer
		bw.write("</script>");bw.newLine();
		if (bw != out)
			bw.flush();
	}
	
	/**
	 * Set the default refresh rate (in milliseconds) for new instances. The
	 * respective constructor argument takes precedence if specified. Calls to
	 * this method do not have any effect on instances created before.
	 * @param refreshRateMillis the default refresh rate (in milliseconds) 
	 */
	public static void setDefaultRefreshRate(int refreshRateMillis) {
		defaultRefreshRateMillis = refreshRateMillis;
	}
	private static int defaultRefreshRateMillis = 333;
	
	/**
	 * Retrieve the URL of the default CSS stylesheet for new instances, to
	 * include in the head of an HTML page that uses HTML code generated by the
	 * <code>writeHtml()</code> method.
	 * @return the URL of the default CSS stylesheet
	 */
	public static String getDefaultCssURL() {
		return defaultCssUrl; 
	}
	
	/**
	 * Set the URL of the default CSS stylesheet for new instances.
	 * @param cssUrl the URL of the new default CSS stylesheet
	 */
	public static void setDefaultCssURL(String cssUrl) {
		defaultCssUrl = cssUrl;
	}
	private static String defaultCssUrl = null;
	
	/**
	 * @return the default maximum number of info messages to display
	 */
	public static int getDefaultInfoLineLimit() {
		return defaultInfoLineLimit;
	}
	
	/**
	 * Set the default maximum number of info messages to display in newly
	 * created instances.
	 * @param infoLineLimit the maximum number of info messages to display
	 */
	public static void setDefaultInfoLineLimit(int infoLineLimit) {
		if (infoLineLimit < 1)
			return;
		defaultInfoLineLimit = infoLineLimit;
	}
	private static int defaultInfoLineLimit = 3;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	TODO test this sucker
	}
}