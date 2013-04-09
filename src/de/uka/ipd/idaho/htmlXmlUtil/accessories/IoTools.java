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
package de.uka.ipd.idaho.htmlXmlUtil.accessories;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Tool box library for downloading web pages, File IO, and manipulating the
 * tree representation of HTML/XML documents. For details, please refer to the
 * description of the individual methods.
 * 
 * @author Guido Sautter
 */
public class IoTools {
	
	/**
	 * default mapping of HTML encoded special characters to normal UTF-8
	 * characters to be specified with the two argument version of the
	 * <code>prepareForPlainText()</code> method
	 * <ul>
	 * <li>Ligatures are separated to two chars</li>
	 * <li>dash characters are mapped to hyphens</li>
	 * <li> Accents and other language-specific markers are removed</li>
	 * </ul>
	 */
	public static final Properties HTML_CHAR_NORMALIZATION = new Properties();
	
	//	fill the character normalization mapping
	static {
		HTML_CHAR_NORMALIZATION.setProperty("&Agrave;", "A");
		HTML_CHAR_NORMALIZATION.setProperty("&Aacute;", "A");
		HTML_CHAR_NORMALIZATION.setProperty("&Acirc;", "A");
		HTML_CHAR_NORMALIZATION.setProperty("&Atilde;", "A");
		HTML_CHAR_NORMALIZATION.setProperty("&Auml;", "AE");
		HTML_CHAR_NORMALIZATION.setProperty("&Aring;", "A");
		HTML_CHAR_NORMALIZATION.setProperty("&AElig;", "AE");
		HTML_CHAR_NORMALIZATION.setProperty("&Ccedil;", "C");
		HTML_CHAR_NORMALIZATION.setProperty("&Egrave;", "E");
		HTML_CHAR_NORMALIZATION.setProperty("&Eacute;", "E");
		HTML_CHAR_NORMALIZATION.setProperty("&Ecirc;", "E");
		HTML_CHAR_NORMALIZATION.setProperty("&Euml;", "E");
		HTML_CHAR_NORMALIZATION.setProperty("&Igrave;", "I");
		HTML_CHAR_NORMALIZATION.setProperty("&Iacute;", "I");
		HTML_CHAR_NORMALIZATION.setProperty("&Icirc;", "I");
		HTML_CHAR_NORMALIZATION.setProperty("&Iuml;", "I");
		HTML_CHAR_NORMALIZATION.setProperty("&Ntilde;", "N");
		HTML_CHAR_NORMALIZATION.setProperty("&Ograve;", "O");
		HTML_CHAR_NORMALIZATION.setProperty("&Oacute;", "O");
		HTML_CHAR_NORMALIZATION.setProperty("&Ocirc;", "O");
		HTML_CHAR_NORMALIZATION.setProperty("&Otilde;", "O");
		HTML_CHAR_NORMALIZATION.setProperty("&Ouml;", "OE");
		HTML_CHAR_NORMALIZATION.setProperty("&OElig;", "OE");
		HTML_CHAR_NORMALIZATION.setProperty("&Oslash;", "O");
		HTML_CHAR_NORMALIZATION.setProperty("&Ugrave;", "U");
		HTML_CHAR_NORMALIZATION.setProperty("&Uacute;", "U");
		HTML_CHAR_NORMALIZATION.setProperty("&Ucirc;", "U");
		HTML_CHAR_NORMALIZATION.setProperty("&Uuml;", "UE");
		HTML_CHAR_NORMALIZATION.setProperty("&Yacute;", "Y");
		HTML_CHAR_NORMALIZATION.setProperty("&szlig;", "ss");
		HTML_CHAR_NORMALIZATION.setProperty("&agrave;", "a");
		HTML_CHAR_NORMALIZATION.setProperty("&aacute;", "a");
		HTML_CHAR_NORMALIZATION.setProperty("&acirc;", "a");
		HTML_CHAR_NORMALIZATION.setProperty("&atilde;", "a");
		HTML_CHAR_NORMALIZATION.setProperty("&auml;", "ae");
		HTML_CHAR_NORMALIZATION.setProperty("&aring;", "a");
		HTML_CHAR_NORMALIZATION.setProperty("&aelig;", "ae");
		HTML_CHAR_NORMALIZATION.setProperty("&ccedil;", "c");
		HTML_CHAR_NORMALIZATION.setProperty("&egrave;", "e");
		HTML_CHAR_NORMALIZATION.setProperty("&eacute;", "e");
		HTML_CHAR_NORMALIZATION.setProperty("&ecirc;", "e");
		HTML_CHAR_NORMALIZATION.setProperty("&euml;", "e");
		HTML_CHAR_NORMALIZATION.setProperty("&igrave;", "i");
		HTML_CHAR_NORMALIZATION.setProperty("&iacute;", "i");
		HTML_CHAR_NORMALIZATION.setProperty("&icirc;", "i");
		HTML_CHAR_NORMALIZATION.setProperty("&iuml;", "i");
		HTML_CHAR_NORMALIZATION.setProperty("&ntilde;", "n");
		HTML_CHAR_NORMALIZATION.setProperty("&ograve;", "o");
		HTML_CHAR_NORMALIZATION.setProperty("&oacute;", "o");
		HTML_CHAR_NORMALIZATION.setProperty("&ocirc;", "o");
		HTML_CHAR_NORMALIZATION.setProperty("&otilde;", "o");
		HTML_CHAR_NORMALIZATION.setProperty("&ouml;", "oe");
		HTML_CHAR_NORMALIZATION.setProperty("&oelig;", "oe");
		HTML_CHAR_NORMALIZATION.setProperty("&oslash;", "o");
		HTML_CHAR_NORMALIZATION.setProperty("&ugrave;", "u");
		HTML_CHAR_NORMALIZATION.setProperty("&uacute;", "u");
		HTML_CHAR_NORMALIZATION.setProperty("&ucirc;", "u");
		HTML_CHAR_NORMALIZATION.setProperty("&uuml;", "ue");
		HTML_CHAR_NORMALIZATION.setProperty("&yacute;", "y");
		HTML_CHAR_NORMALIZATION.setProperty("&yuml;", "y");
		HTML_CHAR_NORMALIZATION.setProperty("&yacute;", "y");
		HTML_CHAR_NORMALIZATION.setProperty("&ndash;", "-");
		HTML_CHAR_NORMALIZATION.setProperty("&mdash;", "-");
	}
	
	/** encoding for http GET queries
	 */
	public static final String URL_ENCODING = "UTF-8";
	
	//	parser for page handling
	private static Html HTML = new Html();
	private static Parser HTML_PARSER = new Parser(HTML);
	
	//	parser for xml file handling
	private static Parser XML_PARSER = new Parser(new StandardGrammar());
	
	/**
	 * get the page from the specified URL
	 * @param url the URL to get the page from
	 * @return the page as a String
	 * @throws MalformedURLException, if the specified URL is invalid
	 * @throws IOException
	 */
	public static String getPage(String url) throws Exception, IOException {
		return getPage(url, false, false, null);
	}

	/**
	 * get the page from the specified URL
	 * @param url the URL to get the page from
	 * @param sessionID the session ID to be specified in the http header
	 * @return the page as a String
	 * @throws MalformedURLException, if the specified URL is invalid
	 * @throws IOException
	 */
	public static String getPage(String url, String sessionID) throws Exception, IOException {
		return getPage(url, false, false, sessionID);
	}

	/**
	 * get the page from the specified URL
	 * @param url the URL to get the page from
	 * @param encode encode the URL or not (set to true for unencoded get
	 *            requests)
	 * @param correctErrors correct HTLM errors (same as calling
	 *            getAndParsePage(url, encode, sessionID, false).treeToCode(),
	 *            only that tree is built virtually)
	 * @return the page as a String
	 * @throws MalformedURLException, if the specified URL is invalid
	 * @throws IOException
	 */
	public static String getPage(String url, boolean encode, boolean correctErrors) throws Exception, IOException {
		return getPage(url, encode, correctErrors, null);
	}

	/**
	 * get the page from the specified URL
	 * @param url the URL to get the page from
	 * @param encode encode the URL or not (set to true for unencoded http GET
	 *            requests)
	 * @param correctErrors correct HTLM errors (same as calling
	 *            getAndParsePage(url, encode, sessionID, false).treeToCode(),
	 *            only that tree is built virtually)
	 * @param sessionID the session ID to be specified in the http header
	 * @return the page as a String
	 * @throws MalformedURLException, if the specified URL is invalid
	 * @throws IOException
	 */
	public static String getPage(String url, boolean encode, boolean correctErrors, String sessionID) throws IOException {
		//	get URL
		URL address = createUrl(url, encode);
		StringBuffer assembler = new StringBuffer();
		
		//	open connection
		URLConnection connection = address.openConnection();
		if (sessionID != null) connection.setRequestProperty("Cookie", sessionID);
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);
		connection.connect();
		
		//	stream the page through the HTML_PARSER
		if (correctErrors) {
			try {
				//	get and parse page
				HTML_PARSER.stream(connection.getInputStream(), TokenReceiver.getTokenReceiver(assembler));
				return assembler.toString();
			}
			catch (IOException pe) {
				
				//	if session caused problems, try again without specifying it
				if (sessionID != null) return getPage(url, encode, correctErrors, null);
//				throw new IOException(pe.getClass() + " (" + pe.getMessage() + ") while getting page from url \"" + url + "\"");
				else throw pe;
			}
		}
		
		//	simply download the page
		else {
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			char[] buffer = new char[1024];
			int read;
			while ((read = reader.read(buffer, 0, buffer.length)) != -1)
				assembler.append(buffer, 0, read);
			
			reader.close();
			return assembler.toString();
			
//			while (reader.ready())
//				assembler.append((char) reader.read());
//			reader.close();
//			return assembler.toString();
		}
	}
	
	/**
	 * get the page from the specified URL as a tree
	 * @param url the URL to get the page from
	 * @return the root node of the parsed page's tree
	 * @throws MalformedURLException, if the specified URL is invalid
	 * @throws IOException
	 */
	public static TreeNode getAndParsePage(String url) throws Exception, IOException {
		return getAndParsePage(url, false, null, false, false);
	}

	/**
	 * get the page from the specified URL as a tree
	 * @param url the URL to get the page from
	 * @param sessionID the session ID to be specified in the http header
	 * @param qualifyLinks set URL-based attributes to fully qualified URL
	 * @param extendFrames if set to true, get the content pages of <frame>
	 *            nodes and link them to the tree (the html node of the content
	 *            page will be linked to the frame node as the only child)
	 * @return the root node of the parsed page's tree
	 * @throws MalformedURLException, if the specified URL is invalid
	 * @throws IOException
	 */
	public static TreeNode getAndParsePage(String url, String sessionID, boolean qualifyLinks, boolean extendFrames) throws Exception, IOException {
		return getAndParsePage(url, false, sessionID, qualifyLinks, extendFrames);
	}

	/**
	 * get the page from the specified URL as a tree
	 * @param url the URL to get the page from
	 * @param encode encode the URL or not (set to true for unencoded get
	 *            requests)
	 * @return the root node of the parsed page's tree
	 * @throws MalformedURLException, if the specified URL is invalid
	 * @throws IOException
	 */
	public static TreeNode getAndParsePage(String url, boolean encode) throws Exception, IOException {
		return getAndParsePage(url, encode, null, false, false);
	}

	/**
	 * get the page from the specified URL as a tree
	 * @param url the URL to get the page from
	 * @param encode encode the URL or not (set to true for unencoded http GET
	 *            requests)
	 * @param sessionID the session ID to be specified in the http header
	 * @param qualifyLinks set URL-based attributes to fully qualified URL
	 * @param extendFrames if set to true, get the content pages of <frame>
	 *            nodes and link them to the tree (the html node of the content
	 *            page will be linked to the frame node as the only child)
	 * @return the root node of the parsed page's tree
	 * @throws MalformedURLException, if the specified URL is invalid
	 * @throws IOException
	 */
	public static TreeNode getAndParsePage(String url, boolean encode, String sessionID, boolean qualifyLinks, boolean extendFrames) throws Exception, IOException {
		
		//	get URL
		URL address = createUrl(url, encode);
		TreeNode pageRoot;
		
		//	open connection
		URLConnection connection = address.openConnection();
		if (sessionID != null) connection.setRequestProperty("Cookie", sessionID);
		connection.setRequestProperty("User-Agent", "IoTools");
		connection.setDoInput(true);
		connection.setDoOutput(true);
		connection.setUseCaches(false);
		connection.connect();
		
		//	get and parse page
		try {
			pageRoot = HTML_PARSER.parse(connection.getInputStream());
		}
		catch (IOException pe) {
			
			//	if session caused problems, try again without specifying it
			if (sessionID != null) return getAndParsePage(url, encode, null, qualifyLinks, extendFrames);
//			throw new IOException(pe.getClass() + " (" + pe.getMessage() + ") while parsing page from url \"" + url + "\"");
			else throw new IOException(pe.getClass() + " (" + pe.getMessage() + ") while parsing page from url \"" + url + "\"");
		}
		
		//	check final URL (may be different from original one due to header-based forward
		address = connection.getURL();
		String host = address.getAuthority();
		String finalUrl = address.getProtocol() + "://" + address.getAuthority() + address.getFile();
		String session = connection.getHeaderField("Set-Cookie");
		
		//	remember session
		if (session != null) {
			pageRoot.setAttribute("Session", session);
			TreeTools.addAttribute(pageRoot, "a", "Session", session);
			TreeTools.addAttribute(pageRoot, "area", "Session", session);
		} else if (sessionID != null) {
			pageRoot.setAttribute("Session", sessionID);
			TreeTools.addAttribute(pageRoot, "a", "Session", sessionID);
			TreeTools.addAttribute(pageRoot, "area", "Session", sessionID);
		}
		
		//	execute meta-tag based address forward if necessary 
		TreeNode[] metaNodes = TreeTools.getAllNodesOfType(pageRoot, "meta");
		for (int i = 0; i < metaNodes.length; i++) {
			if  (metaNodes[i].getAttribute("http-equiv", "").equalsIgnoreCase("refresh") && metaNodes[i].hasAttribute("content")) {
				
				//	get the forward url
				String forward = metaNodes[i].getAttribute("content", "");
				if (forward.indexOf(";") != -1)	forward = forward.substring(forward.indexOf(";")+1).trim();
				
				//	fully qualified forward url
				if ((forward.indexOf("://") != -1) || forward.startsWith(host)) return getAndParsePage(forward, encode, sessionID, qualifyLinks, extendFrames);
				
				//	check if address is relative
				URL forwardUrl = createUrl(forward, false);
				String paramHost = forwardUrl.getAuthority();
				if (paramHost.equals("") || paramHost.equals(".") || paramHost.equals("..")) forward = ("http://" + host + forwardUrl.getFile());
				
				//	get page
				return getAndParsePage(forward, encode, sessionID, qualifyLinks, extendFrames);
			}
		}
		
		//	store source URL in root node
		pageRoot.setAttribute("src", finalUrl);
		
		if (qualifyLinks) qualifyPage(pageRoot, finalUrl);
		if (extendFrames) extendFrames(pageRoot, finalUrl, encode, sessionID, qualifyLinks);
		return pageRoot;
	}
	
	/**
	 * copy the page at the specified URL and all linked pages on the same host
	 * to the specified folder and supfolders
	 * @param url the URL of the entry page
	 * @param destination the path to store the pages in (folders and subfolders
	 *            will be created automatically)
	 * @param report if set to true, status information will be printet to
	 *            System.out
	 */
	public static void deepCopy(String url, String destination, boolean report) throws Exception {
		deepCopy(url, new File(destination), report);
	}

	/**
	 * copy the page at the specified URL and all linked pages on the same host
	 * to the specified folder and supfolders
	 * @param url the URL of the entry page
	 * @param destinationFolder the folder to store the pages in (subfolders
	 *            will be created automatically)
	 * @param report if set to true, status information will be printet to
	 *            System.out
	 */
	public static void deepCopy(String url, File destinationFolder, boolean report) throws Exception {
		Vector urls = new Vector();
		HashSet additionalUrls = new HashSet();
		int index = 0;
		
		//	check address
		URL address = createUrl(url, false);
		String file = address.getPath();
		if (file.endsWith("/")) {
			urls.add(url + "index.htm");
			urls.add(url + "index.html");
			file = file.substring(0, (file.length() - 1));
		}
		else {
			urls.add(url);
			if (file.indexOf("/") > -1) file = file.substring(0, file.lastIndexOf("/"));
		}
		String domain = address.getAuthority().toLowerCase();
		
		//	where to store the pages
		String destination = destinationFolder.toString();
		if (!destination.endsWith(File.separator)) destination = destination + File.separator;
		
		//	status string for errors
		String errorStatus = "";
		
		while (index < urls.size()) {
			try {
				//	get page
				errorStatus = "getting page";
				String pageUrl = ((String) urls.get(index));
				
				//	make sure to stay within th domain
				if (pageUrl.toLowerCase().startsWith(domain) || (pageUrl.toLowerCase().indexOf("://" + domain) != -1)) {
					if (report) System.out.print("Getting page from " + pageUrl + " ... ");
					TreeNode pageRoot = getAndParsePage(pageUrl, false, null, false, false);
					if (report) System.out.println("done");
					
					//	extract links
					errorStatus = "extracting links";
					if (report) System.out.print("Extracting links ... ");
					TreeNode[] pages = TreeTools.getAllNodesOfType(pageRoot, "a");
					for (int i = 0; i < pages.length; i++) {
						if (pages[i].hasAttribute("href")) {
							String href = pages[i].getAttribute("href");
							if (href.indexOf("#") == -1) {
								href = qualifyUrlAttributeValue(href, pageUrl);
								if (!urls.contains(href)) urls.addElement(href);
							}
						}
					}
					if (report) System.out.print(" hyperlink hrefs done ... ");
					
					pages = TreeTools.getAllNodesOfType(pageRoot, "frame");
					for (int i = 0; i < pages.length; i++) {
						if (pages[i].hasAttribute("src")) {
							String src = pages[i].getAttribute("src");
							src = qualifyUrlAttributeValue(src, pageUrl);
							if (!urls.contains(src)) urls.addElement(src);
						}
					}
					if (report) System.out.print(" frame srcs done ... ");
					
					//	get eventual stylesheets and scripts from extra files
					TreeNode[] links = TreeTools.getAllNodesOfType(pageRoot, "link");
					for (int i = 0; i < links.length; i++) {
						if (links[i].getAttribute("rel").equalsIgnoreCase("stylesheet")) {
							if (pages[i].hasAttribute("href")) {
								String href = links[i].getAttribute("href");
								href = qualifyUrlAttributeValue(href, pageUrl);
								if (!additionalUrls.contains(href.toLowerCase())) {
									try {
										String s = getPage(href, false, false, null);
										String fileName = destination + href.substring(href.indexOf(domain) + domain.length());
										writeFile(getFile(fileName), s);
										additionalUrls.add(href.toLowerCase());
									}
									catch (Exception e) {
										if (report) System.out.println("error while getting stylesheet from " + href + "\n" + e.getClass().getName() + ": " + e.getMessage());
									}
								}
								links[i].setAttribute("href", relativateUrlAttributeValue(href, pageUrl));
							}
						}
					}
					if (report) System.out.print(" style links done ... ");
					
					TreeNode[] styles = TreeTools.getAllNodesOfType(pageRoot, "style");
					for (int i = 0; i < styles.length; i++) {
						TreeNode tn = styles[i].getChildNode(TreeNode.COMMENT_NODE_TYPE, 0);
						if (tn != null) {
							String style = tn.getNodeValue();
							if (style.indexOf("url(") != -1) {
								String[] styleUrls = parseString(style, "url(");
								for (int j = 1; j < styleUrls.length; j++) {
									int split = styleUrls[j].indexOf(')');
									if (split != -1) {
										String src = styleUrls[j].substring(0, split);
										if (src.endsWith("\"") || src.endsWith("'")) src = src.substring(0, (split - 1));
										if (src.startsWith("\"") || src.startsWith("'")) src = src.substring(1);
										src = qualifyUrlAttributeValue(src, pageUrl);
										try {
											String s = getPage(src, false, false, null);
											String fileName = destination + src.substring(src.indexOf(domain) + domain.length());
											writeFile(getFile(fileName), s);
											additionalUrls.add(src.toLowerCase());
										}
										catch (Exception e) {
											if (report) System.out.println("error while getting imported stylesheet from " + src + "\n" + e.getClass().getName() + ": " + e.getMessage());
										}
									}
								}
							}
						}
					}
					if (report) System.out.print(" style imports done ... ");
					
					TreeNode[] scripts = TreeTools.getAllNodesOfType(pageRoot, "script");
					for (int i = 0; i < scripts.length; i++) {
						if (scripts[i].hasAttribute("src")) {
							String src = scripts[i].getAttribute("src");
							src = qualifyUrlAttributeValue(src, pageUrl);
							if (!additionalUrls.contains(src.toLowerCase())) {
								try {
									String s = getPage(src, false, false, null);
									String fileName = destination + src.substring(src.indexOf(domain) + domain.length());
									writeFile(getFile(fileName), s);
									additionalUrls.add(src.toLowerCase());
								}
								catch (Exception e) {
									if (report) System.out.println("error while getting script from " + src + "\n" + e.getClass().getName() + ": " + e.getMessage());
								}
							}
							scripts[i].setAttribute("src", relativateUrlAttributeValue(src, pageUrl));
						}
					}
					if (report) System.out.print(" scripts done ... ");
					
					if (report) System.out.println("done");
					
					//	store page
					errorStatus = "writing file";
					relativatePage(pageRoot, pageUrl);
					String fileName = destination + pageUrl.substring(pageUrl.indexOf(domain) + domain.length());
					if (report) System.out.print("Storing page to " + fileName + " ... ");
					TokenReceiver receiver = TokenReceiver.getTokenReceiver(getFile(fileName));
					try {
						pageRoot.treeToCode(receiver, "  ");
					}
					catch (IOException ioe) {
						writeFile(getFile(fileName), pageRoot.treeToCode("  "));
					}
					finally {
						receiver.close();
					}
					if (report) System.out.println("done");
					
					//	clean up
					pageRoot.deleteSubtree();
					System.gc();
				}
				
				//	switch to next page
				index ++;
			}
			catch (Exception e) {
				if (report) System.out.println("error while " + errorStatus + " for url " + urls.get(index) + "\n" + e.getClass().getName() + ": " + e.getMessage());
				index++;
			}
		}
	}
	
	/**	create a valid URL from the specified String
	 * @param	url		the String to create a URL from
	 * @param	encode	encode the URL or not (set to true for unencoded http GET requests)
	 * @return a URL object pointing to the specified address
	 */
	public static URL createUrl(String url, boolean encode) throws IOException {
		
		String workString = url;
		
		String protocol = "http";
		String host;
		int port = -1;
		String file = "";
		String attributes = "";
		
		//	get protocol
		protocol = ((workString.indexOf("://") == -1) ? protocol : workString.substring(0, workString.indexOf("://")));
		workString = ((workString.indexOf("://") == -1) ? workString : workString.substring(workString.indexOf("://") + 3));
		
		//	get host
		host = ((workString.indexOf("/") == -1) ? workString : workString.substring(0, workString.indexOf("/")));
		workString = ((workString.indexOf("/") == -1) ? "" : workString.substring(workString.indexOf("/")));
		
		//	get file
		file = ((workString.indexOf("?") == -1) ? workString : workString.substring(0, workString.indexOf("?")));
		workString = (((workString.indexOf("?") == -1) && ((workString.indexOf("?") + 1) < workString.length())) ? "" : workString.substring(workString.indexOf("?") + 1));
		
		//	get attributes, encode if desired
		attributes = ((encode) ? URLEncoder.encode(workString, URL_ENCODING) : workString);
		file = ((attributes.length() == 0) ? file : (file + "?" + attributes));
		
		//	read port number
		if (host.indexOf(":") != -1) {
			try {
				port = java.lang.Integer.parseInt(host.substring(host.indexOf(":") + 1));
				host = host.substring(0, host.indexOf(":"));
			}
			catch (NumberFormatException nfe) {}
		}
		
		//	create and return URL
		return ((port == -1) ? new URL(protocol, host, file) : new URL(protocol, host, port, file));
	}
	
	/**	extend the <frame> nodes with the pages they contain
	 * @param	pageRoot	the root node of the page tree to process
	 */
	private static void extendFrames(TreeNode pageRoot, String url, boolean encode, String sessionID, boolean qualifyLinks) throws Exception {
		TreeNode[] frames = TreeTools.getAllNodesOfType(pageRoot, "frame");
		for (int i = 0; i < frames.length; i++) {
			
			//	extend only frame nodes that haven't been extended before
			if (frames[i].getChildNode("html", 0) == null) {
				String src = (qualifyLinks) ? frames[i].getAttribute("src") : qualifyUrlAttributeValue(frames[i].getAttribute("src"), url);
				
				try {
					TreeNode subPageRoot = getAndParsePage(src, encode, sessionID, qualifyLinks, true);
					if (qualifyLinks) qualifyPage(subPageRoot, src);
					TreeNode htmlNode = subPageRoot.getChildNode("html", 0);
					frames[i].addChildNode(htmlNode);
					htmlNode.setParent(frames[i]);
				}
				catch (Exception ex) {
					TreeNode errorRoot = new Parser(null).parse("<HTML><BODY><P>No page to be displayed</P><BR><P><I>" + ex.getClass() + ex.getMessage() + "</I></P><BR><P>while getting page from</P><BR><P><I>" + src + "</I></P></BODY></HTML>");
					TreeNode errorNode = errorRoot.getChildNode("html", 0);
					errorNode.setParent(frames[i]);
					frames[i].addChildNode(errorNode);
				}
			}
		}
	}
	
	/**	replace the values of URL-based attributes by fully qualified URLs
	 * @param	pageRoot	the root node of the page tree to process
	 * @param 	url			the URL to use for qualification
	 */
	public static void qualifyPage(TreeNode pageRoot, String url) throws IOException {
		qualifyUrlAttribute(pageRoot, "a", "href", url);
		qualifyUrlAttribute(pageRoot, "img", "src", url);
		qualifyUrlAttribute(pageRoot, "frame", "src", url);
		qualifyUrlAttribute(pageRoot, "area", "href", url);
		qualifyUrlAttribute(pageRoot, "input", "src", url);
		qualifyUrlAttribute(pageRoot, "form", "action", url);
		qualifyUrlAttribute(pageRoot, null, "background", url);
	}
	
	/**	replace the values of a URL-based attribute by fully qualified URLs
	 * @param	root		the root node of the page tree to process
	 * @param	nodeType	the type of the nodes to process (if null is specified for this attribute, all nodes of all types will be affected)
	 * @param	attribute	the attribute to be qualified
	 * @param 	url			the URL to use for qualification
	 */
	public static void qualifyUrlAttribute(TreeNode root, String nodeType, String attribute, String url) throws IOException {
		
		//	prepare address
		String address = url;
		
		//	cut protocol, file name and eventual query off address
		if (address.indexOf("://") > 0) address = address.substring(address.indexOf("://") + 3);
		if (address.indexOf("?") > 0) address = address.substring(0, address.indexOf("?"));
		if (address.indexOf("/") > 0) address = address.substring(0, address.lastIndexOf("/"));
		
		//	prepare address parts
		String[] addressParts = parseString(address, "/");
		
		//	get nodes and host
		TreeNode[] nodes = ((nodeType != null) ? TreeTools.getAllNodesOfType(root, nodeType) :  TreeTools.treeToBreadthFirstOrder(root));
		String host = createUrl(url, false).getAuthority();
		String protocol = createUrl(url, false).getProtocol();
		if (protocol.equals("")) protocol = "http";
		
		//	check attribute and make it absolute if it's relative
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].hasAttribute(attribute)) {
				String attributeValue = nodes[i].getAttribute(attribute);
				if ((attributeValue.indexOf("://") == -1) && !attributeValue.startsWith(host)){
					String newValue = qualifyUrlAttributeValue(attributeValue, addressParts, protocol);
					if (!attributeValue.equalsIgnoreCase(newValue)) nodes[i].setAttribute(attribute, (newValue));
				}
			}
		}
	}
	
	/**	produce a fully qualified URL out of an (eventually relative) URL and a base URL 
	 * @param	attributeValue	the attribute value to be qualified
	 * @param 	url			the URL to use for qualification
	 * @return a fully qualified URL produced out of specified attribute value and base URL
	 */
	public static String qualifyUrlAttributeValue(String attributeValue, String url) throws IOException {
		
		String address = url;
		String protocol = createUrl(url, false).getProtocol();
		if (protocol.equals("")) protocol = "http";
		
		//	link contains a protocol --> fully qualified address
		if (attributeValue.indexOf("://") != -1) return attributeValue;
		
		//	cut protocol, file name and eventual query off address
		if (address.indexOf("://") > 0) address = address.substring(address.indexOf("://") + 3);
		if (address.indexOf("?") > 0) address = address.substring(0, address.indexOf("?"));
		if (address.indexOf("/") > 0) address = address.substring(0, address.lastIndexOf("/"));
		
		//	prepare address parts
		String[] addressParts = IoTools.parseString(address, "/");
		
		//	delegate to common method part
		return qualifyUrlAttributeValue(attributeValue, addressParts, protocol);
	}
	
	/**	produce a fully qualified URL out of an (eventually relative) URL and a base URL 
	 * @param	attributeValue	the attribute value to be qualified
	 * @param 	urlParts		the parts of the URL to use for qualification
	 * @param 	protocol		the protocol name to insert at the beginning of the address
	 * @return a fully qualified URL produced out of specified attribute value and base URL
	 */
	public static String qualifyUrlAttributeValue(String attributeValue, String[] urlParts, String protocol) throws IOException {
		String paramValue = attributeValue;
		
		//	remember query if given
		String paramQuery = ((paramValue.indexOf("?") == -1) ? "" : paramValue.substring(paramValue.indexOf("?")));
		
		//	cut eventual query
		paramValue = ((paramValue.indexOf("?") == -1) ? paramValue : paramValue.substring(0, paramValue.indexOf("?")));
		
		//	cut leading in-place references
		while (paramValue.startsWith("./")) paramValue = paramValue.substring(2);
		
		//	parse paths
		int addressPartEndIndex = urlParts.length;
		String[] paramParts = IoTools.parseString(paramValue, "/");
		int paramPartIndex = 0;
		
		//	ascend to superfolders if necessary
		while ((addressPartEndIndex > 0) && (paramPartIndex < paramParts.length) && paramParts[paramPartIndex].equals("..")) {
			addressPartEndIndex --;
			paramPartIndex ++;
		}
		
		//	first part of param contains more than one dot or a domain extension after a dot --> fully qualified address
		if ((paramPartIndex < paramParts.length) && (paramParts[paramPartIndex].indexOf(".") != -1) && (domainExtensions.contains(paramParts[paramPartIndex].substring(paramParts[paramPartIndex].lastIndexOf("."))) || (paramParts[paramPartIndex].lastIndexOf(".") > paramParts[paramPartIndex].indexOf("."))))
			return (protocol + "://" + paramValue + paramQuery);
			
		//	first part of param is not last part, but contains at least one dot --> fully qualified address 
		else if (((paramPartIndex + 1) < paramParts.length) && (paramParts[paramPartIndex].indexOf(".") != -1))
			return (protocol + "://" + paramValue + paramQuery);
			
		else {
			StringBuffer assembler = new StringBuffer();
			for (int i = 0; i < addressPartEndIndex; i++) assembler.append(((i > 0) ? "/" : "") + urlParts[i]);
			for (int i = paramPartIndex; i < paramParts.length; i++) assembler.append("/" + paramParts[i]);
			return (protocol + "://" + assembler.toString() + paramQuery);
		}
	}
	
	//	heuristic data for href attribute qualification
	private static String[] domainExtensionsArray = {"de", "com", "gov", "net", "org", "edu", "mil", "ch", "fr", "info", "uk", "at", "nl", "dk"};
	private static HashSet domainExtensions = new ArrayHashSet(domainExtensionsArray);
	
	/**	replace the values of URL-based attributes by URLs relative to the specified URL
	 * @param	pageRoot	the root node of the page tree to process
	 * @param 	url			the URL to relativate against
	 */
	public static void relativatePage(TreeNode pageRoot, String url) throws IOException {
		relativateUrlAttribute(pageRoot, "a", "href", url);
		relativateUrlAttribute(pageRoot, "img", "src", url);
		relativateUrlAttribute(pageRoot, "frame", "src", url);
		relativateUrlAttribute(pageRoot, "area", "href", url);
		relativateUrlAttribute(pageRoot, "input", "src", url);
		relativateUrlAttribute(pageRoot, "form", "action", url);
		relativateUrlAttribute(pageRoot, null, "background", url);
	}
	
	/**	replace the values of a URL-based attribute by URLs relative to the specified one
	 * @param	root		the root node of the page tree to process
	 * @param	nodeType	the type of the nodes to process (if null is specified for this parameter, all nodes of all types will be affected)
	 * @param	attribute	the attribute to be qualified
	 * @param 	url			the URL to use for qualification
	 */
	public static void relativateUrlAttribute(TreeNode root, String nodeType, String attribute, String url) throws IOException {
		
		//	prepare address parts
		String[] addressParts = parseString(url, "/");
		
		//	get nodes and host
		TreeNode[] nodes = ((nodeType != null) ? TreeTools.getAllNodesOfType(root, nodeType) :  TreeTools.treeToBreadthFirstOrder(root));
		
		//	check attribute and make it absolute if it's relative
		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i].hasAttribute(attribute)) {
				String attributeValue = nodes[i].getAttribute(attribute);
				String newValue = relativateUrlAttributeValue(attributeValue, addressParts);
				if (!attributeValue.equalsIgnoreCase(newValue)) nodes[i].setAttribute(attribute, newValue);
			}
		}
	}
	
	/**	produce a URL relative to the specified one out of the param value
	 * @param	attributeValue	the attribute value to be processed
	 * @param 	url				the URL to relativate to
	 * @return a URL relative to the specified one
	 */
	public static String relativateUrlAttributeValue(String attributeValue, String url) throws IOException {
		
		//	prepare address parts
		String[] addressParts = IoTools.parseString(url, "/");
		
		//	delegate to common method part
		return relativateUrlAttributeValue(attributeValue, addressParts);
	}
	
	/**	produce a URL relative to the specified one out of the param value
	 * @param	attributeValue	the attribute value to be processed
	 * @param 	urlParts		the parts of the URL to relativate against
	 * @return a URL relative to the specified one out of the param value
	 */
	public static String relativateUrlAttributeValue(String attributeValue, String[] urlParts) throws IOException {
		String paramValue = attributeValue;
		
		//	remember query if given
		String paramQuery = ((paramValue.indexOf("?") == -1) ? "" : paramValue.substring(paramValue.indexOf("?")));
		
		//	cut eventual query
		paramValue = ((paramValue.indexOf("?") == -1) ? paramValue : paramValue.substring(0, paramValue.indexOf("?")));
		
		//	assemble base url
		String url = "";
		for (int i = 0; i < urlParts.length; i++) url = (url + ((i != 0) ? "/" : "") + urlParts[i]);
		
		try {
			URL u = IoTools.createUrl(paramValue, false);
			URL bu = IoTools.createUrl(url, false);
			if (!u.getAuthority().equalsIgnoreCase(bu.getAuthority()) || !u.getProtocol().equalsIgnoreCase(bu.getProtocol()) || (u.getPort() != bu.getPort())) return attributeValue;
		}
		catch (MalformedURLException mue) {
			return attributeValue;
		}
		
		//	cut protocol if not specified in base url
		if ((paramValue.indexOf("://") != -1) && ((urlParts.length < 2) || !urlParts[1].equals(""))) paramValue = paramValue.substring(paramValue.indexOf("://") + 3);
		
		//	add protocol if specified in base url
		if ((paramValue.indexOf("://") == -1) && (urlParts.length > 1) && urlParts[1].equals("")) paramValue = (urlParts[0] + "://" + paramValue);
		
		//	parse urls
		String[] paramValueParts = parseString(paramValue, "/");
		
		StringBuffer assembler = new StringBuffer();
		int startIndex = 0;
		
		//	cut equal prefix
		while ((startIndex < paramValueParts.length) && (startIndex < urlParts.length) && paramValueParts[startIndex].equalsIgnoreCase(urlParts[startIndex])) startIndex ++;
		
		//	catch equal urls (return filename)
		if ((startIndex == paramValueParts.length) && (startIndex != 0)) return (paramValueParts[startIndex - 1] + paramQuery);
		
		//	add upward part
		for (int i = startIndex; i < (urlParts.length - 1); i++) assembler.append((((i != (urlParts.length - 1)) || url.endsWith("/")) ? "../" : ""));
		
		//	add folder part
		for (int i = startIndex; i < paramValueParts.length; i++) assembler.append(paramValueParts[i] + (((i != (paramValueParts.length - 1)) || paramValue.endsWith("/")) ? "/" : ""));
		
		//	re-attach eventual query and return result
		return (assembler.toString() + paramQuery);
	}
	
	/**	wrap an InputStream so the data it provides will be converted to well formed XML
	 * @param	source	the InputStrem to wrap
	 * @return an InputStream that obtains its data from the specified one, then cleans and propagats it
	 * @throws IOException 
	 */
	public static InputStream wrap(InputStream source) throws IOException {
		return wrap(source, null);
	}
	
	/**	wrap an InputStream so the data it provides will be converted to well formed XML
	 * @param	source		the InputStrem to wrap
	 * @param	grammar		the Grammar to use for cleaning
	 * @return an InputStream that obtains its data from the specified one, then cleans and propagats it
	 * @throws IOException 
	 */
	public static InputStream wrap(final InputStream source, Grammar grammar) throws IOException {
		PipedInputStream pis = new PipedInputStream();
		final PipedOutputStream pos = new PipedOutputStream(pis);
		final Parser parser = new Parser(grammar);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					parser.stream(source, pos);
				} catch (IOException ioe) {}
			}
		});
		t.start();
		return pis;
	}
	
	/**	wrap an Reader so the data it provides will be converted to well formed XML
	 * @param	source	the Reader to wrap
	 * @return an Reader that obtains its data from the specified one, then cleans and propagats it
	 * @throws IOException 
	 */
	public static Reader wrap(Reader source) throws IOException {
		return wrap(source, null);
	}
	
	/**	wrap an Reader so the data it provides will be converted to well formed XML
	 * @param	source		the Reader to wrap
	 * @param	grammar		the Grammar to use for cleaning
	 * @return an Reader that obtains its data from the specified one, then cleans and propagats it
	 * @throws IOException 
	 */
	public static Reader wrap(final Reader source, Grammar grammar) throws IOException {
		PipedReader pr = new PipedReader();
		final PipedWriter pw = new PipedWriter(pr);
		final Parser parser = new Parser(grammar);
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					parser.stream(source, pw);
				} catch (IOException ioe) {}
			}
		});
		t.start();
		return pr;
	}
	
	/**	read the content of a file into a String
	* @param	fileName	the path and name of the file to read
	* @return	the content of the specified file as a String
	* @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
	* @throws	IOException, if any IOException occures
	*/
	public static String readFile(String fileName) throws FileNotFoundException, IOException {
		return readFile(new File(fileName));
	}
	
	/**	read the content of a file into a String
	* @param	file	the file to read
	* @return	the content of the specified file as a String
	* @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
	* @throws	IOException, if any IOException occures
	*/
	public static String readFile(File file) throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		StringBuffer assembler = new StringBuffer();
		
		char[] buffer = new char[1024];
		int read;
		while ((read = reader.read(buffer, 0, buffer.length)) != -1)
			assembler.append(buffer, 0, read);
		
		reader.close();
		return assembler.toString();
		
//		while (reader.ready()) assembler.append((char) reader.read());
//		reader.close();
//		return assembler.toString();
	}
	
	/**	read the content of a file and build an XML tree out of it
	* @param	fileName	the path and name of the file to read
	* @return	an XML tree built off the content of the specified file as a String
	* @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
	* @throws	IOException, if any IOException occures
	*/
	public static TreeNode readAndParseFile(String fileName) throws FileNotFoundException, IOException {
		return readAndParseFile(new File(fileName));
	}

	/**	read the content of a file and build an XML tree out of it
	* @param	file	the file to read
	* @return	an XML tree built off the content of the specified file as a String
	* @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
	* @throws	IOException, if any IOException occures
	*/
	public static TreeNode readAndParseFile(File file) throws FileNotFoundException, IOException {
		FileInputStream fis = new FileInputStream(file);
//		try {
			//	get and parse file
			return XML_PARSER.parse(fis);
//		}
//		catch (ParseException pe) {
//			throw new IOException(pe.getClass() + " (" + pe.getMessage() + ") while reading file \"" + file.toString() + "\"");
//		}
	}

	/**	write the specified String to the end of the specified file
	 * @param	fileName	the path and name of the file to write to
	 * @param	content		the String to be written to the end of the specified file
	 * @return	true, if the writing process was successful, false otherwise
	 * @throws	IOException, if any IOEXception occures
	 * 	If the specified file doesn't exist in the specified path, it will be created
	 */
	public static boolean writeFile(String fileName, String content) throws IOException {
		return writeFile(new File(fileName), content);
	}
	
	/**	write the specified String to the end of the specified file
	 * @param	file		the file to write to
	 * @param	content		the String to be written to the end of the specified file
	 * @return	true, if the writing process was successful, false otherwise
	 * @throws	IOException, if any IOEXception occures
	 * 	If the specified file doesn't exist in the specified path, it will be created
	 */
	public static boolean writeFile(File file, String content) throws IOException {
		String[] cont = {content};
		return writeFile(file, cont);
	}
	
	/**	write the specified String to the end of the specified file
	 * @param	fileName	the path and name of the file to write to
	 * @param	content		the array containing the Strings to be written to the end of the specified file
	 * @return	true, if the writing process was successful, false otherwise
	 * @throws	IOException, if any IOEXception occures
	 * 	If the specified file doesn't exist in the specified path, it will be created
	 */
	public static boolean writeFile(String fileName, String[] content) throws IOException {
		return writeFile(new File(fileName), content);
	}
	
	/**	write the specified String to the end of the specified file
	 * @param	file		the file to write to
	 * @param	content		the array containing the Strings to be written to the end of the specified file
	 * @return	true, if the writing process was successful, false otherwise
	 * @throws	IOException, if any IOEXception occures
	 * 	If the specified file doesn't exist in the specified path, it will be created
	 */
	public static boolean writeFile(File file, String[] content) throws IOException {
		
		//	make sure the destination file exists
		File destination = ensureFile(file);
		
		//	prepare data
		Vector temp;
		Vector lines = new Vector();
		for (int i = 0; i < content.length; i++) {
			temp = parseWriteString(content[i]);
			lines.addAll(temp);
		}
		
		//	write
		try {
			BufferedWriter buf = new BufferedWriter(new FileWriter(destination, true));
			for (int i = 0; i < lines.size(); i++) {
				buf.write((String) lines.get(i));
				buf.newLine();
			}
			buf.flush();
			buf.close();
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}
	
	/**	write the specified String to the end of the specified file
	 * @param	fileName	the path and name of the file to write to
	 * @param	content		the String to be written to the end of the specified file
	 * @return	true, if the writing process was successful, false otherwise
	 * @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
	 * @throws	IOException, if any IOEXception occures
	 * 	If the specified file doesn't exist in the specified path, it won't be created
	 */
	public static boolean writeToFile(String fileName, String content) throws IOException {
		return writeToFile(new File(fileName), content);
	}
	
	/**	write the specified String to the end of the specified file
	 * @param	file		the file to write to
	 * @param	content		the String to be written to the end of the specified file
	 * @return	true, if the writing process was successful, false otherwise
	 * @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
	 * @throws	IOException, if any IOEXception occures
	 * 	If the specified file doesn't exist in the specified path, it won't be created
	 */
	public static boolean writeToFile(File file, String content) throws IOException {
		return ((file.exists()) ? writeFile(file, content) : false);
	}
	
	/**	write the specified String to the end of the specified file
	 * @param	fileName	the path and name of the file to write to
	 * @param	content		the String to be written to the end of the specified file
	 * @return	true, if the writing process was successful, false otherwise
	 * @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
	 * @throws	IOException, if any IOEXception occures
	 * 	If the specified file doesn't exist in the specified path, it won't be created
	 */
	public static boolean writeToFile(String fileName, String[] content) throws IOException {
		return writeToFile(new File(fileName), content);
	}
	
	/**	write the specified String to the end of the specified file
	 * @param	file		the file to write to
	 * @param	content		the String to be written to the end of the specified file
	 * @return	true, if the writing process was successful, false otherwise
	 * @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
	 * @throws	IOException, if any IOEXception occures
	 * 	If the specified file doesn't exist in the specified path, it won't be created
	 */
	public static boolean writeToFile(File file, String[] content) throws IOException {
		return ((file.exists()) ? writeFile(file, content) : false);
	}
	
	/**	parseWriteString parses the String s to singualr lines at \n and \r characters
	 * @param	s	the String to be parsed
	 * @return	a StringVector containing the parts of s
	 */
	private static Vector parseWriteString(String s) {
		Vector lines = new Vector();
		String workString = s;
		while ((workString.indexOf("\n") > - 1) || (workString.indexOf("\r") > - 1)) {
			if ((workString.indexOf("\n") > - 1) && ((workString.indexOf("\n") < workString.indexOf("\r")) || (workString.indexOf("\r") == -1))) {
				lines.addElement(workString.substring(0, workString.indexOf("\n")));
				workString = ((workString.indexOf("\n") < workString.length()) ? workString.substring(workString.indexOf("\n")+1) : "");
			} else if ((workString.indexOf("\r") > - 1) && ((workString.indexOf("\r") < workString.indexOf("\n")) || (workString.indexOf("\n") == -1))) {
				lines.addElement(workString.substring(0, workString.indexOf("\r")));
				workString = ((workString.indexOf("\r") < workString.length()) ? workString.substring(workString.indexOf("\r")+1) : "");
			}
		}
		if (workString.length() > 0) lines.addElement(workString);
		
		return lines;
	}
	
	/**	create a file
	 * @param	path	the path and filename
	 * @return a File object pointing to the desired file 
	 * @throws IOException
	 */
	public static File getFile(String path) throws IOException {
		//	check parameters
		return (((path == null) || (path.length() == 0)) ? null : ensureFile(new File(path)));
	}
	
	/**	ensure a file exists
	 * @param	file	the file
	 * @return a File object pointing to the desired file 
	 * @throws IOException
	 */
	public static File ensureFile(File file) throws IOException {
		
		//	check parameters
		if (file == null) return null;
		if (file.exists()) return file;
		
		//	parse the path
		String[] pathParts = IoTools.parseString(file.toString(), File.separator);
		
		//	check and eventually create folders and file
		if (pathParts.length > 0) {
			
			//	get the drive
			File workFile = new File(pathParts[0]);
			
			//	check and eventually create folders and file recursively
			for (int i = 1; i < pathParts.length; i++) {
				workFile = new File(workFile, pathParts[i]);
				if (!workFile.exists()) {
					if (i < (pathParts.length - 1)) workFile.mkdirs();
					else workFile.createNewFile();
				}
			}
			
			//	return the desired file
			return workFile;
		}
		
		//	if no file was created, return null
		return null;
	}

	/**	parse a String of data
	 * @param	data		the data String
	 * @param	separator	the separator String
	 * @return the parts of the data String in a String array
	 */
	public static String[] parseString(String data, String separator) {
		String workString = data;
		ArrayList list = new ArrayList();
		while (workString.indexOf(separator) > -1) {
			list.add(workString.substring(0, workString.indexOf(separator)));
			workString = workString.substring(workString.indexOf(separator) + separator.length());
		}
		if (workString.length() > 0) list.add(workString);
		return ((String[]) list.toArray(new String[list.size()]));
	}
	
	/**
	 * Prepare a String for being displayed in an HTML page. Details: characters
	 * causing a line feed ('\n', '\r', '\f') are replaced by BR tags special
	 * characters are encoded for HTML display (e.g. '&' is encoded as
	 * &amp;amp;)
	 * @param string the String to be prepared
	 * @return a String looking - in an HTML page - the same as the specified
	 *         one would look in an Editor
	 */
	public static String prepareForHtml(String string) {
		return prepareForHtml(string, null);
	}
	
	/**
	 * Prepare a String for being displayed in an HTML page. Details: characters
	 * causing a line feed ('\n', '\r', '\f') are replaced by BR tags special
	 * characters are encoded for HTML display (e.g. '&' is encoded as
	 * &amp;amp;)
	 * @param string the String to be prepared
	 * @param mapping a custom mapping of characters (one might e.g. want &Auml;
	 *            normalized to A instead of encoded to &amp;Auml;)
	 * @return a String looking - in an HTML page - the same as the specified
	 *         one would look in an Editor
	 */
	public static String prepareForHtml(String string, Properties mapping) {
		
		if ((string == null) || (string.length() == 0)) return "";
		
		Properties charMapping = ((mapping == null) ? new Properties() : mapping);
		
		StringBuffer assembler = new StringBuffer();
		int index = 0;
		char ch;
		char lastChar = '\u0000';
		
		while (index < string.length()) {
			ch = string.charAt(index);
			
			//	eventual start of character code
			if (ch == '&') {
				if (string.indexOf(";", index) != -1) {
					String code = string.substring(index, (string.indexOf(";", index) + 1));
					if (HTML.isCharCode(code)) {
						assembler.append(code);
						index += code.length();
					}
					else {
						assembler.append(HTML.getCharCode(ch));
						index ++;
					}
				}
				else {
					assembler.append(HTML.getCharCode(ch));
					index ++;
				}
			}
			
			//	line feed
			else if ((ch == '\n') || (ch == '\r') || (ch == '\f')) {
				if ((lastChar == ch) || ((lastChar != '\r') && (lastChar != '\n'))) assembler.append("<br>");
				index ++;
			}
			
			//	other character 
			else {
				if (charMapping.containsKey("" + ch)) assembler.append(charMapping.getProperty(("" + ch), ("" + ch)));
				else assembler.append(HTML.getCharCode(ch));
				index ++;
			}
			
			lastChar = ch;
		}
		
		return assembler.toString();
	}
	
	/**
	 * Prepare a String for being displayed as plain text. Details: BR tags are
	 * replaced with line breaks ('\n') encoded special characters are decoded
	 * for plain display (e.g. &amp;amp; is  decoded to '&')
	 * @param string the String to be prepared
	 * @return a String looking - as plain text (e.g. in an editor) - the same
	 *         as the specified one would look in an HTML page
	 */
	public static String prepareForPlainText(String string) {
		return prepareForPlainText(string, null);
	}
	
	/**
	 * Prepare a String for being displayed as plain text. Details: BR tags are
	 * replaced with line breaks ('\n') encoded special characters are decoded
	 * for plain display (e.g. &amp;amp; is decoded to '&')
	 * @param string the String to be prepared
	 * @param customMappings a Properties providing some custom character code
	 *            mappings (providing a plain char here will overrule the HTML
	 *            native decoding)
	 * @return a String looking - as plain text (e.g. in an editor) - the same
	 *         as the specified one would look in an HTML page
	 */
	public static String prepareForPlainText(String string, Properties customMappings) {
		
		//	check parameter
		if ((string == null) || (string.length() == 0))
			return "";
		
		String lowerString = string.toLowerCase();
		StringBuffer assembler = new StringBuffer();
		int index = 0;
		char ch;
		
		while (index < string.length()) {
			ch = string.charAt(index);
			
			//	probable start of character code
			if (ch == '&') {
				if (string.indexOf(";", index) != -1) {
					String code = string.substring(index, (string.indexOf(";", index) + 1));
					if ((customMappings != null) && customMappings.containsKey(code))
						assembler.append(customMappings.getProperty(code));
					else if (HTML.isCharCode(code))
						assembler.append(HTML.getPlainChar(code));
					else assembler.append(code);
					
					index += code.length();
				}
				else {
					assembler.append(ch);
					index ++;
				}
			}
			
			//	line feed
			else if (lowerString.startsWith("<br>", index)) {
				assembler.append("\n");
				index += 4;
			}
			
			//	other character 
			else {
				assembler.append(ch);
				index ++;
			}
		}
		
		return assembler.toString();
	}
	
	/**
	 * Check weather a String is marked up with HTML or not. Note: plain text
	 * Strings might also contain HTML tags or parts looking like such, so this
	 * method might produce false negatives.
	 * @param string the String to be checked
	 * @return true if and only if the specified String is marked up with HTML
	 */
	public static boolean isHtmlEncoded(String string) {
		
		//	check parameter
		if ((string == null) || (string.length() == 0)) return false;
		
		//	check for HTML tags
		String lowerString = string.toLowerCase();
		if (lowerString.startsWith("<html ") || lowerString.startsWith("<html>") || lowerString.endsWith("</html>") || lowerString.startsWith("<htm ") || lowerString.startsWith("<htm>") || lowerString.endsWith("</htm>")) {
			//System.out.println(string + "\nis HTML encoded for it starts or ends with an 'html' tag");
			return true;
		}
		
		//	check for line breaks
		if ((lowerString.indexOf("<br>") != 0) && (lowerString.indexOf('\n') == 0) && (lowerString.indexOf('\r') == 0)) {
			//System.out.println(string + "\nis HTML encoded for it uses '<br>' instead of line break characters");
			return true;
		}
		
		//	check for tags in general
		if (lowerString.startsWith("<") && !lowerString.startsWith("< ")) {
			//System.out.println(string + "\nis HTML encoded for it seems to start with a tag");
			return true;
		}
		
		//	check parsability
		try {
			TreeNode root = HTML_PARSER.parse(string);
			if ((root.getChildNodeCount() == 1) && (root.getChildNode(0).getNodeType().equals(TreeNode.DATA_NODE_TYPE))) {
				//System.out.println(string + "\nis not HTML encoded for it contains no tags");
				return false;
			}
			TreeNode[] nodes = TreeTools.treeToBreadthFirstOrder(root);
			for (int i = (nodes.length - 1); i > -1; i--) if (nodes[i].getTreeDepth() > 2) {
				//System.out.println(string + "\nis HTML encoded for contains tags and has a tree deph greater than 2");
				return true;
			}
		}
		catch (IOException pe) {
			//System.out.println(string + "\nis not HTML encoded for it is not parsable");
			return false;
		}
		
		//	space for further checks
		//System.out.println(string + "\nis not HTML encoded for none of the rules applied");
		return false;
	}
	
	private static class ArrayHashSet extends HashSet {
		
		/**	Constructor
		 * @param	content		an array of Strings containing the initial contant for the HashSet
		 */
		public ArrayHashSet(String[] content) {
			for (int i = 0; i < content.length; i++)
				this.add(content[i]);
		}
	}
}
