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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Centralized servlet responsible for delivering resource files (e.g. images or
 * CSS or JavaScript files) that are stored in servlet's data paths below the
 * surrounding wep-app's WEB-INF folder and are thus invisible via HTTP. In
 * general, servlets using such resources have to register them with this
 * servlet first using the registerResource(ServletContext, String) method.
 * However, if the file extension is configured to allow general access in this
 * servlet's configuration file, requests are looped through as well; the
 * respective setting is named 'accessibleFileExtensions', its value has to
 * contain the file extensions separated by spaces.
 * 
 * This servlet has to be mapped to '/resources/*' in the web-app's web.xml
 * file.
 * 
 * @author sautter
 */
public class ResourceServlet extends WebServlet implements WebConstants {
	
	private static final SimpleDateFormat lastModifiedDateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss z");
	
	private static HashMap accessibleResourceSets = new HashMap();
	
	private static Properties fileExtensionsToMimeTypesDefault = new Properties();
	static { // default MIME types, according to Wikipedia (http://en.wikipedia.org/wiki/Internet_media_type)
		fileExtensionsToMimeTypesDefault.setProperty("json", "application/json");// JavaScript Object Notation JSON; Defined in RFC 4627
		fileExtensionsToMimeTypesDefault.setProperty("js", "application/javascript");// ECMAScript/JavaScript; Defined in RFC 4329 (equivalent to application/ecmascript but with looser processing rules) It is not accepted in IE 8 or earlier - text/javascript is accepted but it is defined as obsolete in RFC 4329. The "type" attribute of the <script> tag in HTML5 is optional. In practice, omitting the media type of JavaScript programs is the most interoperable solution, since all browsers have always assumed the correct default even before HTML5.
		fileExtensionsToMimeTypesDefault.setProperty("pdf", "application/pdf");// Portable Document Format, PDF has been in use for document exchange on the Internet since 1993; Defined in RFC 3778
		fileExtensionsToMimeTypesDefault.setProperty("ps", "application/postscript");// PostScript; Defined in RFC 2046
		fileExtensionsToMimeTypesDefault.setProperty("rdf", "application/rdf+xml");// Resource Description Framework; Defined by RFC 3870
		fileExtensionsToMimeTypesDefault.setProperty("rss", "application/rss+xml");// RSS feeds
		fileExtensionsToMimeTypesDefault.setProperty("woff", "application/font-woff");// Web Open Font Format; (candidate recommendation; use application/x-font-woff until standard is official)
		fileExtensionsToMimeTypesDefault.setProperty("xhtml", "application/xhtml+xml");// XHTML; Defined by RFC 3236
		fileExtensionsToMimeTypesDefault.setProperty("xml", "application/xml");// XML files; Defined by RFC 3023
		fileExtensionsToMimeTypesDefault.setProperty("dtd", "application/xml-dtd");// DTD files; Defined by RFC 3023
		fileExtensionsToMimeTypesDefault.setProperty("zip", "application/zip");// ZIP archive files; Registered[8]
		fileExtensionsToMimeTypesDefault.setProperty("gz", "application/gzip");// Gzip, Defined in RFC 6713
		
		fileExtensionsToMimeTypesDefault.setProperty("ttf", "application/x-font-ttf");// TrueType Font No registered MIME type, but this is the most commonly used
		fileExtensionsToMimeTypesDefault.setProperty("js", "application/x-javascript");// 
		fileExtensionsToMimeTypesDefault.setProperty("tex", "application/x-latex");// LaTeX files
		fileExtensionsToMimeTypesDefault.setProperty("rar", "application/x-rar-compressed");// RAR archive files
		fileExtensionsToMimeTypesDefault.setProperty("swf", "application/x-shockwave-flash");// Adobe Flash files for example with the extension .swf
		fileExtensionsToMimeTypesDefault.setProperty("tar", "application/x-tar");// Tarball files
		fileExtensionsToMimeTypesDefault.setProperty("gimp", "image/x-xcf");// GIMP image file
		fileExtensionsToMimeTypesDefault.setProperty("jq", "text/x-jquery-tmpl");// jQuery template data
		
		fileExtensionsToMimeTypesDefault.setProperty("mp4", "audio/mp4");// MP4 audio
		fileExtensionsToMimeTypesDefault.setProperty("mpeg", "audio/mpeg");// MP3 or other MPEG audio; Defined in RFC 3003
		fileExtensionsToMimeTypesDefault.setProperty("mpg", "audio/mpeg");// MP3 or other MPEG audio; Defined in RFC 3003
		fileExtensionsToMimeTypesDefault.setProperty("mp3", "audio/mpeg");// MP3 or other MPEG audio; Defined in RFC 3003
		
		fileExtensionsToMimeTypesDefault.setProperty("gif", "image/gif");// GIF image; Defined in RFC 2045 and RFC 2046
		fileExtensionsToMimeTypesDefault.setProperty("jpg", "image/jpeg");// JPEG JFIF image; Defined in RFC 2045 and RFC 2046
		fileExtensionsToMimeTypesDefault.setProperty("jpeg", "image/jpeg");// JPEG JFIF image; Defined in RFC 2045 and RFC 2046
		fileExtensionsToMimeTypesDefault.setProperty("pjp", "image/pjpeg");// JPEG JFIF image; Associated with Internet Explorer; Listed in ms775147(v=vs.85) - Progressive JPEG, initiated before global browser support for progressive JPEGs (Microsoft and Firefox).
		fileExtensionsToMimeTypesDefault.setProperty("png", "image/png");// Portable Network Graphics; Registered,[10] Defined in RFC 2083
		fileExtensionsToMimeTypesDefault.setProperty("svg", "image/svg+xml");// SVG vector image; Defined in SVG Tiny 1.2 Specification Appendix M
		fileExtensionsToMimeTypesDefault.setProperty("tif", "image/tiff");// Tag Image File Format (only for Baseline TIFF); Defined in RFC 3302
		fileExtensionsToMimeTypesDefault.setProperty("tiff", "image/tiff");// Tag Image File Format (only for Baseline TIFF); Defined in RFC 3302
		
		fileExtensionsToMimeTypesDefault.setProperty("css", "text/css");// Cascading Style Sheets; Defined in RFC 2318
		fileExtensionsToMimeTypesDefault.setProperty("csv", "text/csv");// Comma-separated values; Defined in RFC 4180
		fileExtensionsToMimeTypesDefault.setProperty("htm", "text/html");// HTML; Defined in RFC 2854
		fileExtensionsToMimeTypesDefault.setProperty("html", "text/html");// HTML; Defined in RFC 2854
		fileExtensionsToMimeTypesDefault.setProperty("js", "text/javascript");// JavaScript; Defined in and made obsolete in RFC 4329 in order to discourage its usage in favor of application/javascript. However, text/javascript is allowed in HTML 4 and 5 and, unlike application/javascript, has cross-browser support. The "type" attribute of the <script> tag in HTML5 is optional and there is no need to use it at all since all browsers have always assumed the correct default (even in HTML 4 where it was required by the specification).
		fileExtensionsToMimeTypesDefault.setProperty("txt", "text/plain");// Textual data; Defined in RFC 2046 and RFC 3676
		fileExtensionsToMimeTypesDefault.setProperty("log", "text/plain");// Textual data; Defined in RFC 2046 and RFC 3676
		fileExtensionsToMimeTypesDefault.setProperty("vc", "text/vcard");// vCard (contact information); Defined in RFC 6350
		fileExtensionsToMimeTypesDefault.setProperty("xml", "text/xml");// Extensible Markup Language; Defined in RFC 3023
		
		fileExtensionsToMimeTypesDefault.setProperty("qt", "video/quicktime");// QuickTime video; Registered[11]
		fileExtensionsToMimeTypesDefault.setProperty("wmf", "video/x-ms-wmv");// Windows Media Video; Documented in Microsoft KB 288102
		fileExtensionsToMimeTypesDefault.setProperty("flv", "video/x-flv");// Flash video (FLV files)
	}
	
	/**
	 * Register a resource to be accessible via HTTP for a given web-app. The
	 * argument resource name is made accessible for the whole web-app, ignoring
	 * any path prefixes. This facilitates physically storing resource files
	 * centrally for an entire web-app, while logically each servlet behaves as
	 * if it had its own copy, making servlets independent of one another.
	 * @param sc the servlet context identifying the web-app
	 * @param resName the name of the resource
	 */
	public static synchronized void registerAccessibleResource(ServletContext sc, String resName) {
		String scPath = sc.getRealPath("./");
		scPath = (new File(scPath)).getAbsolutePath();
		
		while (resName.indexOf('/') != -1)
			resName = resName.substring(resName.indexOf('/') + 1);
		
		HashSet accessibleResources = ((HashSet) accessibleResourceSets.get(scPath));
		if (accessibleResources == null) {
			accessibleResources = new HashSet();
			accessibleResourceSets.put(scPath, accessibleResources);
		}
		accessibleResources.add(resName.toLowerCase());
	}
	
	private HashSet accessibleFileExtensions = new HashSet();
	private HashSet accessibleFileNames = new HashSet();
	
	private Properties fileExtensionsToMimeTypes = new Properties(fileExtensionsToMimeTypesDefault);
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#reInit()
	 */
	protected void reInit() throws ServletException {
		
		//	read accessible file extensions
		String afeString = this.getSetting("accessibleFileExtensions");
		if (afeString == null)
			return;
		String afes[] = afeString.split("\\s+");
		for (int e = 0; e < afes.length; e++) {
			String afe = afes[e].toLowerCase();
			if (afe.startsWith("."))
				afe = afe.substring(1);
			this.accessibleFileExtensions.add(afe);
		}
		
		//	load MIME types from config (overwriting built-in defaults)
		for (Iterator feit = this.accessibleFileExtensions.iterator(); feit.hasNext();) {
			String fe = ((String) feit.next());
			String feMimeType = this.getSetting(fe + "-mime");
			if (feMimeType != null)
				this.fileExtensionsToMimeTypes.setProperty(fe.toLowerCase(), feMimeType);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//	check if we have the resource filter yet (we need this to be independent of servlet loading order)
		if (this.accessibleFileNames == null)
			this.accessibleFileNames = ((HashSet) accessibleResourceSets.get(this.rootFolder.getAbsolutePath()));
		
		//	get requested resource name
		String resName = request.getPathInfo();
		if (resName == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		//	isolate plain file name without path for filtering
		String resFileName = resName;
		while (resFileName.indexOf('/') != -1)
			resFileName = resFileName.substring(resFileName.indexOf('/') + 1).trim();
		if (resFileName.length() == 0) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		//	isolate file extension
		String fileExtension = ((resFileName.lastIndexOf('.') == -1) ? null : resFileName.substring(resFileName.lastIndexOf('.') + 1).toLowerCase());
		
		//	check file name filter
		if ((this.accessibleFileNames != null) && this.accessibleFileNames.contains(resFileName.toLowerCase())) {}
		
		//	no file extension to filter
		else if (fileExtension == null)
			resName = null;
		
		//	check file extension filter
		else if (!this.accessibleFileExtensions.contains(fileExtension))
			resName = null;
		
		//	try and find file if not filtered out
		File resFile = ((resName == null) ? null : this.findFile(resName));
		if (resFile == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		//	get MIME type from lookup table
		String mimeType = ((fileExtension == null) ? null : this.fileExtensionsToMimeTypes.getProperty(fileExtension));
		
		//	deliver resource
		InputStream resIn = new BufferedInputStream(new FileInputStream(resFile));
		if (mimeType != null)
			response.setContentType(mimeType);
		response.setHeader("Last-Modified", lastModifiedDateFormat.format(new Date(resFile.lastModified())));
		OutputStream resOut = response.getOutputStream();
		byte[] resBuf = new byte[1024];
		int read;
		while ((read = resIn.read(resBuf, 0, resBuf.length)) != -1)
			resOut.write(resBuf, 0, read);
		resOut.flush();
		resIn.close();
	}
}
