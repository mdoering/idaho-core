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
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		this.accessibleFileNames = ((HashSet) accessibleResourceSets.get(this.rootFolder.getAbsolutePath()));
		
		String afeString = this.getSetting("accessibleFileExtensions");
		if (afeString == null)
			return;
		String afes[] = afeString.split("\\s+");
		for (int e = 0; e < afes.length; e++) {
			String afe = afes[e].toLowerCase();
			if (!afe.startsWith("."))
				afe = ("." + afe);
			this.accessibleFileExtensions.add(afe);
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		//	check if we have the resource filte yet (we need this to be independent of servlet loading order)
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
		
		//	check file name filter
		if ((this.accessibleFileNames != null) && this.accessibleFileNames.contains(resFileName.toLowerCase())) {}
		
		//	no file extension to filter
		else if (resFileName.indexOf('.') == -1)
			resName = null;
		
		//	check file extension filter
		else if (!this.accessibleFileExtensions.contains(resFileName.substring(resFileName.lastIndexOf('.')).toLowerCase()))
			resName = null;
		
		//	try and find file if not filtered out
		File resFile = ((resName == null) ? null : this.findFile(resName));
		if (resFile == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		
		//	deliver resource
		InputStream resIn = new BufferedInputStream(new FileInputStream(resFile));
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
