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
 *     * Neither the name of the Universit�t Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSIT�T KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
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
package de.uka.ipd.idaho.easyIO.web;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.uka.ipd.idaho.easyIO.util.RandomByteSource;

/**
 * This servlet provides the means to re-initialize individual servlets and
 * entire web applications via the respective methods of the WebAppHost class.
 * Login works via a single pass key, to store in the <code>passKey</code>
 * setting in the servlet's config file.
 * 
 * @author sautter
 */
public class ReInitializerServlet extends HtmlServlet {
	private String passKey = RandomByteSource.getGUID(); // initialize to random non-null value, locking access if config is faulty
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.HtmlServlet#reInit()
	 */
	protected void reInit() throws ServletException {
		super.reInit();
		
		//	read pass key
		this.passKey = this.getSetting("passKey", this.passKey);
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//	check authentication
		if (!this.authenticate(request)) {
			this.writeLoginForm(request, response);
			return;
		}
		
		//	get servlet name
		String servletName = request.getParameter("servletName");
		
		//	re-initialize entire webapp
		if ("all".equals(servletName))
			this.webAppHost.reInitialize();
		
		//	re-initialize specified servlet
		else if (servletName != null)
			this.webAppHost.reInitialize(servletName);
		
		//	list re-initializable servlets
		this.writeServletListHtml(request, response);
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doGet(request, response);
	}

	private void writeLoginForm(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("text/html; charset=utf-8");
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
		bw.write("<html><head>"); bw.newLine();
		bw.write("<title>Authenticate to Refresh Individual Servlets</title>"); bw.newLine();
		bw.write("</head><body>"); bw.newLine();
		bw.write("<form method=\"POST\" action=\"" + request.getContextPath() + request.getServletPath() + "\">"); bw.newLine();
		
		//	open master table
		bw.write("<table class=\"loginTable\">"); bw.newLine();
		
		//	add login fields
		bw.write("<tr>"); bw.newLine();
		
		bw.write("<td>"); bw.newLine();
		bw.write("Pass Key&nbsp;"); bw.newLine();
		bw.write("<input type=\"password\" name=\"passKey\">"); bw.newLine();
		bw.write("</td>"); bw.newLine();
		
		bw.write("</tr>"); bw.newLine();
		
		//	add login button
		bw.write("<tr>"); bw.newLine();
		bw.write("<td align=\"center\">"); bw.newLine();
		bw.write("<input type=\"submit\" value=\"Log In\">"); bw.newLine();
		bw.write("</td>"); bw.newLine();
		bw.write("</tr>"); bw.newLine();
		
		//	close table
		bw.write("</table>"); bw.newLine();
		
		bw.write("</form>"); bw.newLine();
		bw.write("</body></html>"); bw.newLine();
		
		bw.flush();
	}
	
	private void writeServletListHtml(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setHeader("Cache-Control", "no-cache");
		response.setContentType("text/html; charset=utf-8");
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
		bw.write("<html><head>"); bw.newLine();
		bw.write("<title>Refresh Individual Servlets</title>"); bw.newLine();
		bw.write("</head><body>"); bw.newLine();
		bw.write("<div>"); bw.newLine();
		
		String[] servletNames = this.webAppHost.getReInitializableServletNames();
		for (int s = 0; s < servletNames.length; s++) {
			bw.write("<p>"); bw.newLine();
			bw.write("<a href=\"" + request.getContextPath() + request.getServletPath() + "?servletName=" + servletNames[s] + "\">Refresh <b>" + servletNames[s] + "</b></a></td>"); bw.newLine();
			bw.write("</p>"); bw.newLine();
		}
		
		bw.write("<p>"); bw.newLine();
		bw.write("<a href=\"" + request.getContextPath() + request.getServletPath() + "?servletName=" + "all" + "\">Refresh <b>" + "entire web application" + "</b></a></td>"); bw.newLine();
		bw.write("</p>"); bw.newLine();
		
		bw.write("</div>"); bw.newLine();
		bw.write("</body></html>"); bw.newLine();
		
		bw.flush();
	}
	
	private HashMap sessions = new HashMap();
	private int sessionTimeout = (15 * 60 * 1000);
	
	private boolean authenticate(HttpServletRequest request) throws IOException {
		HttpSession session = request.getSession(true);
		String sessionId = session.getId();
		
		long currentTime = System.currentTimeMillis();
		Long lastActivity = ((Long) this.sessions.get(sessionId));
		if (lastActivity != null) {
			if (currentTime < (lastActivity.longValue() + this.sessionTimeout)) {
				this.sessions.put(sessionId, new Long(currentTime));
				return true;
			}
			else this.sessions.remove(sessionId);
		}
		
		if (this.passKey.equals(request.getParameter("passKey"))) {
			this.sessions.put(sessionId, new Long(currentTime));
			return true;
		}
		else return false;
	}
}