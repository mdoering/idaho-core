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

import javax.servlet.ServletException;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;

/**
 * A generic servlet for producing HTML pages. Actual production is done by
 * HtmlPageBuilder objects, which generate web pages in a template-based
 * fashion. The page generation process basically consists of successively
 * replacing so-called <b>signal tags</b> with respective content, either
 * generated HTML code, or HTML code loaded from other files. In particular, the
 * generation process works in the following steps:
 * <ul>
 * <li>Generation starts with a so-called base page, the main or popup HTML page
 * template (see respective parameter below)</li>
 * <li>In the base page, the signal tags (eg <i>&lt;includeFile
 * file=&quot;mySpecialContentFile&quot;/&gt;</i>) are successively replaced by
 * the content generated for them</li>
 * <li>If this generated content contains further signal tags, replacement works
 * them off recursively</li>
 * </ul>
 * Beside generating the HTML code that contains the page information, it is
 * possible to include references to CSS stylesheets and JavaScript files in the
 * page head, and to trigger JavaScript based initialization. In particular,
 * before closing the head of the generated page, the page builder will obtain
 * the following information from its host servlet:
 * <ul>
 * <li>The paths and names of CSS stylesheets to include; paths are interpreted
 * relative to the surrounding web-app's context path, unless they specify a
 * protocol (eg 'http://'), in which case they are interpreted as absolute</li>
 * <li>The paths and names of JavaScript files to include; paths are interpreted
 * relative to the surrounding web-app's context path, unless they specify a
 * protocol (eg 'http://'), in which case they are interpreted as absolute</li>
 * <li>JavaScript functions to call when loading the web page in the browser;
 * this is a generic way of having arbitrary JavaScipt executed on page loading</li>
 * <li>JavaScript functions to call when un-loading the web page in the browser;
 * this is a generic way of having arbitrary JavaScipt executed on page
 * un-loading</li>
 * <li>Finally, a page builder is offered to add arbitrary custom code to the
 * generated page's head, eg generated JavaScript or CSS code</li>
 * </ul>
 * In order to work properly, this servlet reads four parameters from its
 * configuration file:
 * <ul>
 * <li><b>basePage</b>: the name and path of the main page template file, to
 * start generation with; if a respective file exists in the servlets data path,
 * this file will be used, otherwise, a respective file from the surrounding
 * web-app's context path will be used (if this parameter is not specified, it
 * defaults to 'portal.html')</li>
 * <li><b>popupBasePage</b>: the name and path of the template file for popup
 * pages, to start generation with; if a respective file exists in the servlets
 * data path, this file will be used, otherwise, a respective file from the
 * surrounding web-app's context path will be used (if this parameter is not
 * specified, it defaults to 'popup.html'); this template is intended for
 * smaller sub windows, e.g. ones opened using the JavaScript window.open()
 * command</li>
 * <li><b>cssNames</b>: the names of the CSS stylesheets to link in generated
 * pages, separated by spaces; the files do not have to reside in the servlet's
 * data path, but can also come from the WEB-INF or context path</li>
 * <li><b>javaScriptNames</b>: the names of the JavaScript files to link in
 * generated pages, separated by spaces; the files do not have to reside in the
 * servlet's data path, but can also come from the WEB-INF or context path</li>
 * <li><b>iconFileName</b>: the name of the icon to display in the tab bar and
 * in bookmarks for the generated pages; the files do not have to reside in the
 * servlet's data path, but can also come from the WEB-INF or context path</li>
 * </ul>
 * 
 * @author sautter
 */
public abstract class HtmlServlet extends WebServlet implements HtmlPageBuilderHost {
	
	/** HTML grammar for extracting type information from tokens, etc */
	protected static Html html = new Html();
	
	/** HTML-configured parser for handling web page templates */
	protected static Parser htmlParser = new Parser(html);
	
	/**
	 * the name and path of the main page template file, to start generation
	 * with; if a respective file exists in the servlets data path, this file
	 * will be used, otherwise, a respective file from the surrounding web-app's
	 * context path will be used
	 */
	protected String basePage = "portal.html";
	
	/**
	 * the name and path of the template file for popup pages, to start
	 * generation with; if a respective file exists in the servlets data path,
	 * this file will be used, otherwise, a respective file from the surrounding
	 * web-app's context path will be used; this template is intended for
	 * smaller sub windows, e.g. ones opened using the JavaScript window.open()
	 * command
	 */
	protected String popupBasePage = "popup.html";
	
	
	private String[] cssNames = {};
	private String[] javaScriptNames = {};
	private String iconFileName = null;
	
	/**
	 * Initialize the HTML servlet. This implementation reads the basePage,
	 * cssNames, javaStriptNames, and iconFileName parameters from the settings.
	 * Sub classes overwriting this method thus have to make the super call.
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		this.basePage = this.getSetting("basePage", this.basePage);
		this.popupBasePage = this.getSetting("popupBasePage", this.popupBasePage);
		
		String cssNameString = this.getSetting("cssNames");
		if (cssNameString != null) {
			this.cssNames = cssNameString.trim().split("\\s+");
			for (int n = 0; n < this.cssNames.length; n++)
				ResourceServlet.registerAccessibleResource(this.getServletContext(), this.cssNames[n]);
		}
		String jsNameString = this.getSetting("javaScriptNames");
		if (jsNameString != null) {
			this.javaScriptNames = jsNameString.trim().split("\\s+");
			for (int n = 0; n < this.javaScriptNames.length; n++)
				ResourceServlet.registerAccessibleResource(this.getServletContext(), this.javaScriptNames[n]);
		}
		this.iconFileName = this.getSetting("iconFileName");
		if (this.iconFileName != null)
			ResourceServlet.registerAccessibleResource(this.getServletContext(), this.iconFileName);
	}
	
	/**
	 * Send an HTML page. This method locates the configured base page template
	 * file using the findFile() method and streams it through the argument page
	 * builder. The argument page builder is closed afterward. The base page is
	 * assumed to be encoded in the platform default encoding.
	 * @param pageBuilder the page builder to use
	 * @throws IOException
	 */
	protected void sendHtmlPage(HtmlPageBuilder pageBuilder) throws IOException {
		this.sendHtmlPage(this.basePage, pageBuilder);
	}
	
	/**
	 * Send a popup HTML page. This method locates the configured popup page
	 * template file using the findFile() method and streams it through the
	 * argument page builder. The argument page builder is closed afterward. The
	 * base page is assumed to be encoded in the platform default encoding.
	 * @param pageBuilder the page builder to use
	 * @throws IOException
	 */
	protected void sendPopupHtmlPage(HtmlPageBuilder pageBuilder) throws IOException {
		this.sendHtmlPage(this.popupBasePage, pageBuilder);
	}
	
	/**
	 * Send an HTML page. This method locates the base page file with the
	 * specified name using the findFile() method and streams it through the
	 * argument page builder. The argument page builder is closed afterward. The
	 * base page is assumed to be encoded in the platform default encoding.
	 * @param basePageName the name of the base page file to use
	 * @param pageBuilder the page builder to use
	 * @throws IOException
	 */
	protected void sendHtmlPage(String basePageName, HtmlPageBuilder pageBuilder) throws IOException {
		File pageFile = this.findFile(basePageName);
		if (pageFile == null)
			throw new IOException("Page base file not found: '" + basePageName + "'.");
		Reader basePageReader = new BufferedReader(new InputStreamReader(new FileInputStream(pageFile)));
		this.sendHtmlPage(basePageReader, pageBuilder);
		basePageReader.close();
	}
	
	/**
	 * Send an HTML page. This method streams the data from the specified reader
	 * through the argument page builder. The argument page builder is closed
	 * afterward.
	 * @param basePageReader the reader to read from
	 * @param pageBuilder the page builder to use
	 * @throws IOException
	 */
	protected void sendHtmlPage(Reader basePageReader, HtmlPageBuilder pageBuilder) throws IOException {
		htmlParser.stream(basePageReader, pageBuilder);
		pageBuilder.close();
	}
	
	/**
	 * This implementation returns the servlet's configured data path
	 * (physically located in the WEB-INF folder) prefixed with '/resources', so
	 * a ResourceServlet can serve the CSS and JavaScript files.
	 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost#getRelativeDataPath()
	 */
	public String getRelativeDataPath() {
		return ("/" + RESOURCE_PATH + this.dataPath);
	}
	
	/**
	 * This implementation returns the CSS file names specified in the
	 * 'cssNames' setting of the servlet's configuration file. The CSS files
	 * have to be addressed relative to the servlet's data path.
	 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost#getCssStylesheets()
	 */
	public String[] getCssStylesheets() {
		return this.cssNames;
	}
	
	/**
	 * This implementation returns the JavaScript file names specified in the
	 * 'javaScriptNames' setting of the servlet's configuration file. The
	 * JavaScript files have to be addressed relative to the servlet's data
	 * path.
	 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost#getJavaScriptFiles()
	 */
	public String[] getJavaScriptFiles() {
		return this.javaScriptNames;
	}
	
	/**
	 * This implementation returns the image file name specified in the
	 * 'iconFileName' setting of the servlet's configuration file.
	 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost#getIcon()
	 */
	public String getIcon() {
		return this.iconFileName;
	}
	
	/**
	 * This implementation does nothing, sub classes are welcome to overwrite it
	 * as needed.
	 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost#writePageHeadExtensions(de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder)
	 */
	public void writePageHeadExtensions(HtmlPageBuilder out) throws IOException {}
	
	/**
	 * This implementation returns null so no JavaScript calls are included, sub
	 * classes are welcome to overwrite it as needed.
	 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost#getOnloadCalls()
	 */
	public String[] getOnloadCalls() {
		return null;
	}
	
	/**
	 * This implementation returns null so no JavaScript calls are included, sub
	 * classes are welcome to overwrite it as needed.
	 * @see de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost#getOnunloadCalls()
	 */
	public String[] getOnunloadCalls() {
		return null;
	}
}
