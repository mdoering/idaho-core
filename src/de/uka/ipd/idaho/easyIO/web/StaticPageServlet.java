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
package de.uka.ipd.idaho.easyIO.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;

/**
 * Centralized servlet responsible for delivering static content HTML pages that
 * are stored in servlet's data paths below the surrounding web-app's WEB-INF
 * folder and are thus invisible via HTTP. This allows for static content, e.g.
 * description pages, to be delivered with a web application's custom layout.
 * 
 * This servlet has to be mapped to '/static/*' and '/staticPopup/*' in the
 * web-app's web.xml file.
 * 
 * @author sautter
 */
public class StaticPageServlet extends HtmlServlet {
	
	private HashSet includeTags = new HashSet();
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.HtmlServlet#reInit()
	 */
	public void reInit() throws ServletException {
		super.reInit();
		
		//	reload include tags to react to
		this.includeTags.clear();
		this.includeTags.add("includeBody");
		String includeTags = this.getSetting("includeTags");
		if (includeTags != null)
			this.includeTags.addAll(Arrays.asList(includeTags.trim().split("\\s+")));
		this.includeTags.remove("");
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//	get static page name
		String pathInfo = request.getPathInfo();
		if (pathInfo == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		pathInfo = pathInfo.trim();
		while (pathInfo.startsWith("/"))
			pathInfo = pathInfo.substring(1).trim();
		if ((pathInfo.length() == 0) || pathInfo.equalsIgnoreCase(this.basePage) || pathInfo.equalsIgnoreCase(this.popupBasePage)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		File contentFile = this.findFile(pathInfo);
		if ((contentFile == null) || !contentFile.exists() || !contentFile.isFile()) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, pathInfo);
			return;
		}
		
		//	get static page content
		Reader contentIn = new BufferedReader(new InputStreamReader(new FileInputStream(contentFile), "UTF-8"));
		HtmlPageBuffer hpb = new HtmlPageBuffer();
		htmlParser.stream(contentIn, hpb);
		contentIn.close();
		
		//	create page builder
		StaticHtmlPageBuilder shpb = new StaticHtmlPageBuilder(this, request, response, hpb);
		
		//	send page or popup
		if (request.getServletPath().endsWith("Popup"))
			this.sendPopupHtmlPage(shpb);
		else this.sendHtmlPage(shpb);
	}
	
	private class HtmlPageBuffer extends TokenReceiver {
		boolean inHeader = false;
		LinkedList headerContent = new LinkedList();
		boolean inTitle = false;
		StringBuffer pageTitle = new StringBuffer();
		String[] onLoadCalls = null;
		String[] onUnloadCalls = null;
		boolean inBody = false;
		LinkedList bodyContent = new LinkedList();
		HtmlPageBuffer() throws IOException {}
		public void storeToken(String token, int treeDepth) throws IOException {
			if (html.isTag(token)) {
				String type = html.getType(token);
				if ("head".equalsIgnoreCase(type))
					this.inHeader = !html.isEndTag(token);
				else if ("body".equalsIgnoreCase(type)) {
					if (html.isEndTag(token)) {
						this.bodyContent.addLast("</div>");
						this.inBody = false;
					}
					else {
						this.inBody = true;
						TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
						String onload = tnas.getAttribute("onload");
						if (onload != null) {
							this.onLoadCalls = new String[1];
							this.onLoadCalls[0] = onload;
						}
						tnas.removeAttribute("onload");
						String onunload = tnas.getAttribute("onunload");
						if (onunload != null) {
							this.onUnloadCalls = new String[1];
							this.onUnloadCalls[0] = onunload;
						}
						tnas.removeAttribute("onunload");
						this.bodyContent.addLast("<div " + tnas.getAttributeValueString(' ', '=', '"') + ">");
					}
				}
				else if (this.inHeader) {
					if ("title".equalsIgnoreCase(type))
						this.inTitle = !html.isEndTag(token);
					else if (this.inTitle)
						this.pageTitle.append(token);
					else this.headerContent.addLast(token.trim());
				}
				else if (this.inBody)
					this.bodyContent.addLast(token);
			}
			else if (this.inTitle)
				this.pageTitle.append(token);
			else if (this.inHeader)
				this.headerContent.addLast(token);
			else if (this.inBody)
				this.bodyContent.addLast(token);
		}
		public void close() throws IOException {}
	}
	
	private class StaticHtmlPageBuilder extends HtmlPageBuilder {
		private HtmlPageBuffer hpb;
		StaticHtmlPageBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response, HtmlPageBuffer hpb) throws IOException {
			super(host, request, response);
			this.hpb = hpb;
		}
		protected void include(String type, String tag) throws IOException {
			if (includeTags.contains(type)) {
				if (this.hpb == null)
					return;
				for (Iterator phit = this.hpb.bodyContent.iterator(); phit.hasNext();)
					this.storeToken((String) phit.next());
				this.hpb = null;
			}
			else super.include(type, tag);
		}
		protected String getPageTitle(String title) {
			return (((this.hpb == null) || (this.hpb.pageTitle.length() == 0)) ? title : this.hpb.pageTitle.toString());
		}
		protected String[] getOnloadCalls() {
			return ((this.hpb == null) ? super.getOnloadCalls() : this.hpb.onLoadCalls);
		}
		protected String[] getOnunloadCalls() {
			return ((this.hpb == null) ? super.getOnunloadCalls() : this.hpb.onUnloadCalls);
		}
		protected void writePageHeadExtensions() throws IOException {
			if (this.hpb == null)
				return;
			for (Iterator phit = this.hpb.headerContent.iterator(); phit.hasNext();)
				this.write((String) phit.next());
		}
	}
}
