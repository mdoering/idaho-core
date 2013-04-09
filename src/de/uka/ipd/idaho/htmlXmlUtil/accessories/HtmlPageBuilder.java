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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;

/**
 * Builder object for HTML pages. This class wraps the output stream of an
 * HttpServletResponse. If a servlet wants to set response properties, it
 * has to do so before wrapping the response object in an HtmlPageBuidler.
 * 
 * @author sautter
 */
public class HtmlPageBuilder extends TokenReceiver {

	/**
	 * A host object for HTML page builders, to provide file IO etc.
	 * 
	 * @author sautter
	 */
	public static interface HtmlPageBuilderHost {

		/**
		 * Find a file with a given name somewhere in the data accessible to the
		 * host. The file name can also contain relative paths.
		 * @param fileName the name of the file to find
		 * @return a file object pointing to the sought file, or null, if the
		 *         file does not exist
		 */
		public abstract File findFile(String fileName);

		/**
		 * Obtain the relative path leading from an HTTP request's context path
		 * (the part of a URL identifying the application to send a requst to,
		 * usually the first step after the server name and port) to specific
		 * data items, like stylesheets or JavaScript files.
		 * @return the relative data path
		 */
		public abstract String getRelativeDataPath();
		
		/**
		 * Retrieve an array holding the addresses of CSS stylesheets to reference
		 * in a page head. If an address includes a protocol, eg 'http://', it is
		 * interpreted as absolute. Otherwise, it is interpreted relative to the
		 * servlet's data path.
		 * @return an array holding the addresses of CSS stylesheets to reference in
		 *         a page head
		 */
		public abstract String[] getCssStylesheets();
		
		/**
		 * Retrieve an array holding the addresses of JavaScript files to reference
		 * in a page head. If an address includes a protocol, eg 'http://', it is
		 * interpreted as absolute. Otherwise, it is interpreted relative to the
		 * servlet's data path.
		 * @return an array holding the addresses of JavaScript files to reference
		 *         in a page head
		 */
		public abstract String[] getJavaScriptFiles();
		
		/**
		 * Retrieve the address of an image file to reference as the page icon
		 * in a page head. If an address includes a protocol, eg 'http://', it
		 * is interpreted as absolute. Otherwise, it is interpreted relative to
		 * the servlet's data path. If this method returns null, the pages will
		 * not have an icon. The file has to be a .ico file to work in IE; most
		 * other browsers also support .bmp, .gif, .png, and .jpg.
		 * @return an array holding the address of an image file to reference as
		 *         the page icon in a page head
		 */
		public abstract String getIcon();
		
		/**
		 * Write servlet specific extensions to a page head. This may be, for
		 * instance, meta tags, JavaScrips, or CSS style information.
		 * @param out the page builder to write to.
		 * @throws IOException
		 */
		public abstract void writePageHeadExtensions(HtmlPageBuilder out) throws IOException;
		
		/**
		 * Retrieve an array holding the JavaScript commands to execute when a page
		 * is loaded in a browser.
		 * @return an array holding the JavaScript commands to execute when a page
		 *         is loaded in a browser
		 */
		public abstract String[] getOnloadCalls();
		
		/**
		 * Retrieve an array holding the JavaScript commands to execute when a page
		 * is un-loaded in a browser.
		 * @return an array holding the JavaScript commands to execute when a page
		 *         is un-loaded in a browser
		 */
		public abstract String[] getOnunloadCalls();
	}
	
	/**
	 * This wrapper jumps over a leading byte order mark in XML files, always
	 * returning '&lt;' the first byte. This helps preventing errors in components
	 * that take input streams as a data source, but cannot handle byte order marks.
	 * For instance, this wrapper prevents the "content not allowed in prolog"
	 * exception thrown by Java's XML components. Using this wrapper with data
	 * other than XML or HTML is likely to cause undesired behavior.
	 */
	public static class ByteOrderMarkFilterInputStream extends FilterInputStream {
		private boolean inContent = false;
		
		/**
		 * Constructor
		 * @param in the input stream to wrap
		 */
		public ByteOrderMarkFilterInputStream(InputStream in) {
			super(in);
		}
		
		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#read()
		 */
		public int read() throws IOException {
			int i = super.read();
			while (!this.inContent) {
				if (i == '<') this.inContent = true;
				else i = super.read();
			}
			return i;
		}

		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#read(byte[], int, int)
		 */
		public int read(byte[] b, int off, int len) throws IOException {
			if (this.inContent)	return super.read(b, off, len);
			else {
				int i = super.read();
				while (!this.inContent) {
					if (i == '<') this.inContent = true;
					else i = super.read();
				}
				b[off] = ((byte) i);
				return (1 + super.read(b, (off + 1), (len - 1)));
			}
		}
	}
	
	/** HTML grammar for extracting type information from tokens, etc */
	protected static Html html = new Html();
	
	/** HTML-configured parser for handling web page templates */
	protected static Parser htmlParser = new Parser(html);
	
	/** the servlet that operates the page builder */
	protected HtmlPageBuilderHost host;
	
	/** the request in response to which the page is generated */
	public final HttpServletRequest request;
	
	//	local status information
	private boolean inHyperLink = false;
	private boolean inTitle = false;
	private String title;
	
	private String[] onloadCalls = null;
	private String[] onunloadCalls = null;
	
	//	output writer
	private BufferedWriter out;
	
	/**
	 * Constructor
	 * @param host the host object granting access to files, etc.
	 * @param request the HttpServletRequest to answer
	 * @param response the HttpServletResponse to write the answer to
	 * @throws IOException
	 */
	protected HtmlPageBuilder(HtmlPageBuilderHost host, HttpServletRequest request, HttpServletResponse response) throws IOException {
		this.host = host;
		
//		cannot do this here, as sub classes may overwrite the getters with code that depends on the sub class constructor having been executed
//		this.onloadCalls = combineArrays(this.host.getOnloadCalls(), this.getOnloadCalls());
//		if (this.onloadCalls == null)
//			this.onloadCalls = new String[0];
//		this.onunloadCalls = combineArrays(this.host.getOnunloadCalls(), this.getOnunloadCalls());
//		if (this.onunloadCalls == null)
//			this.onunloadCalls = new String[0];
		
		this.request = request;
		this.out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "utf-8"));
//		this.out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "utf-8")) {
//			public void close() throws IOException {
//				System.out.println("HtmlPageBuilder Writer: being closed ...");
//				StackTraceElement[] stes = Thread.currentThread().getStackTrace();
//				for (int e = 0; e < stes.length; e++)
//					System.out.println(stes[e].toString());
//				super.close();
//			}
//			public void flush() throws IOException {
//				System.out.println("HtmlPageBuilder Writer: being fushed ");
//				StackTraceElement[] stes = Thread.currentThread().getStackTrace();
//				for (int e = 0; e < stes.length; e++)
//					System.out.println(stes[e].toString());
//				super.flush();
//			}
//		};
	}
	
	private static String[] combineArrays(String[] strs1, String[] strs2) {
		if ((strs1 == null) || (strs1.length == 0))
			return strs2;
		else if ((strs2 == null) || (strs2.length == 0))
			return strs1;
		String[] strs = new String[strs1.length + strs2.length];
		System.arraycopy(strs1, 0, strs, 0, strs1.length);
		System.arraycopy(strs2, 0, strs, strs1.length, strs2.length);
		return strs;
	}
	
	private void prepareJavaScriptCalls() {
		if (this.onloadCalls == null) {
			this.onloadCalls = combineArrays(this.host.getOnloadCalls(), this.getOnloadCalls());
			if (this.onloadCalls == null)
				this.onloadCalls = new String[0];
		}
		if (this.onunloadCalls == null) {
			this.onunloadCalls = combineArrays(this.host.getOnunloadCalls(), this.getOnunloadCalls());
			if (this.onunloadCalls == null)
				this.onunloadCalls = new String[0];
		}
	}
	
	/**
	 * Close the page this page builder builds. This will flush the enclosed
	 * writer, triggering the page being sent to the requesting browser.
	 * @see de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver#close()
	 */
	public void close() throws IOException {
		this.out.flush();
	}
	
	/**
	 * Write an HTML token (tag or textual content) to the web page built by
	 * this page builder
	 * @param token the token to write
	 * @throws IOException
	 */
	public void storeToken(String token) throws IOException {
		this.storeToken(token, 0);
	}
	
	/**
	 * Write a string to the page being built.
	 * @param s the string to write
	 * @throws IOException
	 */
	public void write(String s) throws IOException {
		this.out.write(s);
	}
	
	/**
	 * Retrieve a Writer that writes directly to the Writer embedded in the HTML
	 * page builder, without any further checks. Flushing or closing the writer
	 * returned by this method does not have any effect.
	 * @return the Writer
	 */
	public Writer asWriter() {
		if (this.loopThroughWriter == null)
			this.loopThroughWriter = new Writer() {
				public void write(char[] cbuf, int off, int len) throws IOException {
					HtmlPageBuilder.this.out.write(cbuf, off, len);
				}
				public void flush() throws IOException {System.out.println("HtmlPageBuilder.asWriter: ignoring flush()");}
				public void close() throws IOException {System.out.println("HtmlPageBuilder.asWriter: ignoring close()");}
			};
		return this.loopThroughWriter;
	}
	private Writer loopThroughWriter = null;
	
	/**
	 * Write a line break to the page being built.
	 * @throws IOException
	 */
	public void newLine() throws IOException {
		this.out.newLine();
	}
	
	/**
	 * Write a string and a subsequent lin break to the page being built.
	 * This method is equivalent to first invoking write() and then
	 * newLine().
	 * @param s the string to write
	 * @throws IOException
	 */
	public void writeLine(String s) throws IOException {
		this.out.write(s);
		this.out.newLine();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver#storeToken(java.lang.String, int)
	 */
	public void storeToken(String token, int treeDepth) throws IOException {
		if (html.isTag(token)) {
			String type = html.getType(token);
			
			//	file inclusion
			if ("includeFile".equals(type)) {
				TreeNodeAttributeSet as = TreeNodeAttributeSet.getTagAttributes(token, html);
				String includeFile = as.getAttribute("file");
				if (includeFile != null) 
					this.includeFile(includeFile);
			}
			
			//	other inclusion marker
			else if (type.startsWith("include")) {
				if (!html.isEndTag(token))
					this.include(type, token);
			}
			
			//	page title
			else if ("title".equalsIgnoreCase(html.getType(token))) {
				if (html.isEndTag(token)) {
					this.write("<title>");
					this.write(this.getPageTitle(this.title));
					this.write("</title>");
					this.newLine();
					this.inTitle = false;
				}
				else this.inTitle = true;
			}
			
			//	page head
			else if ("head".equalsIgnoreCase(type) && html.isEndTag(token)) {
				
				//	write extensions to page head
				this.extendPageHead();
				
				//	close page head
				this.writeLine(token);
			}
			
			//	start of page body
			else if ("body".equalsIgnoreCase(type) && !html.isEndTag(token)) {
				
				//	make sure JavaScript calls are initialized
				this.prepareJavaScriptCalls();
				
				//	include calls to doOnloadCalls() and doOnunloadCalls() functions
				this.writeLine("<body" + ((this.onloadCalls.length == 0) ? "" : " onload=\"doOnloadCalls();\"") + ((this.onunloadCalls.length == 0) ? "" : " onunload=\"doOnunloadCalls();\"") + ">");
			}
			
			//	image, make link absolute
			else if ("img".equalsIgnoreCase(type)) {
				
				//	check for link
				TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
				String src = tnas.getAttribute("src");
				if ((src != null) && (src.indexOf("://") == -1)) {
					src = this.adjustLink(src);
					if (!src.equals(tnas.getAttribute("src"))) {
						tnas.setAttribute("src", src);
						token = ("<img " + tnas.getAttributeValueString(html) + ">");
					}
				}
				this.write(token);
			}
			
			//	link, make CSS link absolute
			else if ("link".equalsIgnoreCase(type)) {
				
				//	check for link
				TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
				String href = tnas.getAttribute("href");
				if ((href != null) && (href.indexOf("://") == -1) && tnas.getAttribute("rel", "").toLowerCase().endsWith("stylesheet")) {
					href = this.adjustLink(href);
					if (!href.equals(tnas.getAttribute("href"))) {
						tnas.setAttribute("href", href);
						token = ("<link " + tnas.getAttributeValueString(html) + ">");
					}
				}
				this.write(token);
			}
			
			//	script, make source link absolute
			else if ("script".equalsIgnoreCase(type) && !html.isEndTag(token)) {
				
				//	check for link
				TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
				String src = tnas.getAttribute("src");
				if ((src != null) && (src.indexOf("://") == -1)) {
					src = this.adjustLink(src);
					if (!src.equals(tnas.getAttribute("src"))) {
						tnas.setAttribute("src", src);
						token = ("<script " + tnas.getAttributeValueString(html) + ">");
					}
				}
				this.write(token);
			}
			
			//	other token
			else {
				
				//	make href absolute
				if ("a".equalsIgnoreCase(type) && !html.isEndTag(token)) {
					TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
					String href = tnas.getAttribute("href");
					if ((href != null) && (href.indexOf("://") == -1)) {
						href = this.adjustLink(href);
						if (!href.equals(tnas.getAttribute("href"))) {
							tnas.setAttribute("href", href);
							token = ("<a " + tnas.getAttributeValueString(html) + ">");
						}
					}
				}
				
				//	write token
				this.write(token);
				
				// do not insert line break after hyperlink tags, bold tags, and span tags
				if (!"a".equalsIgnoreCase(type) && !"b".equalsIgnoreCase(type) && !"span".equalsIgnoreCase(type))
					this.newLine();
				
				//	remember being in hyperlink (for auto-activation)
				if ("a".equals(type))
					this.inHyperLink = !html.isEndTag(token);
			}
		}
		
		//	textual content
		else {
			
			//	remove spaces from links, and activate them
			if ((token.startsWith("http:") || token.startsWith("ftp:")) && (token.indexOf("tp: //") != -1)) {
				String link = token.replaceAll("\\s", "");
				if (!this.inHyperLink)
					this.write("<a mark=\"autoGenerated\" href=\"" + link + "\">");
				this.write(link);
				if (!this.inHyperLink)
					this.write("</a>");
			}
			
			//	store title to facilitate modification by sub classes
			else if (this.inTitle)
				this.title = token;
			
			//	other token, just write it
			else this.write(token);
		}
	}
	
	private String adjustLink(String link) {
		if (link.startsWith("."))
			link = link.substring(1);
		while (link.startsWith("//"))
			link = link.substring(1);
		if (!link.startsWith("/"))
			link = ("/" + link);
		if (!link.startsWith(this.request.getContextPath()))
			link = (this.request.getContextPath() + link);
		return link;
	}
	
	/**
	 * Retrieve a custom title for the web page under construction in this
	 * page builder. This default implementation simply returns the original
	 * (the argument) title, sub classes are welcome to overwrite it as
	 * needed.
	 * @param title the original title, as specified in the base page
	 * @return the title for the web page
	 */
	protected String getPageTitle(String title) {
		return title;
	}
	
	/**
	 * Retrieve an array holding the JavaScript commands to execute when the
	 * page generated by this page builder is loaded in a browser. This method
	 * exists to return commands more fine-granular than possible in the page
	 * builder host. This default implementation returns an empty array, sub
	 * classes are welcome to overwrite it as needed.
	 * @return an array holding the JavaScript commands to execute when the page
	 *         is loaded in a browser
	 */
	protected String[] getOnloadCalls() {
		return new String[0];
	}
	
	/**
	 * Retrieve an array holding the JavaScript commands to execute when when
	 * the page generated by this page builder is un-loaded in a browser. This
	 * method exists to return commands more fine-granular than possible in the
	 * page builder host. This default implementation returns an empty array,
	 * sub classes are welcome to overwrite it as needed.
	 * @return an array holding the JavaScript commands to execute when the page
	 *         is un-loaded in a browser
	 */
	protected String[] getOnunloadCalls() {
		return new String[0];
	}
	
	private void extendPageHead() throws IOException {
		String dataPath = this.host.getRelativeDataPath();
		if (!dataPath.startsWith("/"))
			dataPath = ("/" + dataPath);
		
		//	include CSS
		String[] cssStylesheets = this.host.getCssStylesheets();
		if (cssStylesheets != null)
			for (int c = 0; c < cssStylesheets.length; c++) {
				
				//	make stylesheet URL absolute
				String cssStylesheetUrl = cssStylesheets[c];
				if (cssStylesheetUrl.indexOf("://") == -1)
					cssStylesheetUrl = this.request.getContextPath() + dataPath + "/" + cssStylesheetUrl;
				
				//	write link
				this.writeLine("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssStylesheetUrl + "\" />");
			}
		
		//	include JavaScript
		String[] javaScriptFiles = this.host.getJavaScriptFiles();
		if (javaScriptFiles != null)
			for (int j = 0; j < javaScriptFiles.length; j++) {
				
				//	make JavaScript file URL absolute
				String javaScriptUrl = javaScriptFiles[j];
				if (javaScriptUrl.indexOf("://") == -1)
					javaScriptUrl = this.request.getContextPath() + dataPath + "/" + javaScriptUrl;
				
				//	write link
				this.writeLine("<script type=\"text/javascript\" src=\"" + javaScriptUrl + "\"></script>");
			}
		
		//	add icon
		String iconFileName = this.host.getIcon();
		if (iconFileName != null) {
			
			//	get image type
			String imageType = ((iconFileName.indexOf('.') == -1) ? null : iconFileName.substring(iconFileName.lastIndexOf('.') + 1));
			if (!imageType.matches("[a-zA-Z]{2,4}"))
				imageType = null;
			
			//	we do have a valid icon
			if (imageType != null) {
				
				//	make image file URL absolute
				if (iconFileName.indexOf("://") == -1)
					iconFileName = this.request.getContextPath() + dataPath + "/" + iconFileName;
				
				//	write IE link
				if ("ico".equalsIgnoreCase(imageType))
					this.writeLine("<link rel=\"shortcut icon\" href=\"" + iconFileName + "\">");
				
				//	write generic link
				this.writeLine("<link rel=\"icon\" type=\"image/" + imageType.toLowerCase() + "\" href=\"" + iconFileName + "\">");
			}
		}
		
		//	make sure JavaScript calls are initialized
		this.prepareJavaScriptCalls();
		
		//	add JavaScript calls for page loading
		if (this.onloadCalls.length != 0) {
			this.writeLine("<script type=\"text/javascript\">");
			this.writeLine("function doOnloadCalls() {");
			if (this.onloadCalls != null)
				for (int f = 0; f < this.onloadCalls.length; f++) {
					String call = this.onloadCalls[f];
					if (call != null)
						this.writeLine(call + (call.endsWith(";") ? "" : ";"));
				}
			this.writeLine("}");
			this.writeLine("</script>");
		}
		
		//	add JavaScript calls for page un-loading
		if (this.onunloadCalls.length != 0) {
			this.writeLine("<script type=\"text/javascript\">");
			this.writeLine("function doOnunloadCalls() {");
			if (this.onunloadCalls != null)
				for (int f = 0; f < this.onunloadCalls.length; f++) {
					String call = this.onunloadCalls[f];
					if (call != null)
						this.writeLine(call + (call.endsWith(";") ? "" : ";"));
				}
			this.writeLine("}");
			this.writeLine("</script>");
		}
		
		//	write servlet specific header
		this.host.writePageHeadExtensions(this);
		
		//	write page builder specific header
		this.writePageHeadExtensions();
	}
	
	/**
	 * Write the content of a file to a page. This method expects the
	 * specified file to contain HTML code, and it writes out the content of
	 * the body tag, thus ignoring all information in the file's head. The
	 * specified file is first searched for in the servlet's data path; if
	 * it is not found there, it is searched in the surrounding web-app's
	 * context path. If it is not found there, either, a respective error
	 * message is written as an XML comment. The two-step hierarchical
	 * search for the file is in order to facilitate providing files used by
	 * multiple servlets in a central location (the web-app's context path),
	 * while still enabling individual servlets to replace specific files
	 * with a servlet specific version.
	 * @param fileName the name of the file to include
	 * @throws IOException
	 */
	protected void includeFile(String fileName) throws IOException {
		this.newLine();
		try {
			File file = this.host.findFile(fileName);
			if ((file != null) && file.exists())
				this.includeFile(file);
			else this.storeToken("<!-- file '" + fileName + "' not found -->", 0);
		}
		catch (IOException ioe) {
			this.writeExceptionAsXmlComment(("exception including file '" + fileName + "'"), ioe);
		}
		this.newLine();
	}
	private void includeFile(File file) throws IOException {
		InputStream is = null;
		try {
			TokenReceiver fr = new TokenReceiver() {
				private boolean inBody = false;
				public void close() throws IOException {}
				public void storeToken(String token, int treeDepth) throws IOException {
					if (html.isTag(token) && "body".equalsIgnoreCase(html.getType(token))) {
						if (html.isEndTag(token))
							this.inBody = false;
						else this.inBody = true;
					}
					else if (this.inBody) HtmlPageBuilder.this.storeToken(token, 0);
				}
			};
			is = new ByteOrderMarkFilterInputStream(new FileInputStream(file));
			htmlParser.stream(is, fr);
		}
		catch (Exception e) {
			throw new IOException(e.getMessage());
		}
		finally {
			if (is != null)
				is.close();
		}
	}
	
	/**
	 * Write an exception to an HTML page as an XML comment
	 * @param label a custom message label to give implementation specific
	 *            context information
	 * @param e the exception to write
	 * @throws IOException
	 */
	protected void writeExceptionAsXmlComment(String label, Exception e) throws IOException {
		this.writeLine("<!-- " + label + ": " + e.getMessage());
		StackTraceElement[] ste = e.getStackTrace();
		for (int s = 0; s < ste.length; s++)
			this.writeLine("  " + ste[s].toString());
		this.writeLine("  " + label + " -->");
	}
	
	/**
	 * Handle a sub class specific signal tag. This method is the main
	 * extension point for servlet specific implementations of
	 * HtmlPageBuilder, as all signal tags (ie tags whose type starts with
	 * 'include') delegate here, except for 'includeFile'. This default
	 * implementation simply adds an XML comment indicating that the
	 * specified tag has not been understood, thus effectively ignoring the
	 * signal tag alltogether. Sub classes should overwrite this method to
	 * filter out their specific signal tags and delegate to this
	 * implementation for ones that they do not understand.
	 * @param type the type of the signal tag
	 * @param tag the signal tag as a whole, eg to parse attributes from
	 * @throws IOException
	 */
	protected void include(String type, String tag) throws IOException {
		this.writeLine("<!-- include tag '" + type + "' not understood -->");
	}
	
	/**
	 * Write servlet specific extensions to a page head. This may be, for
	 * instance, meta tags, JavaScrips, or CSS style information. This default
	 * implementation does nothing, sub classes are welcome to overwrite it as
	 * needed.
	 * @throws IOException
	 */
	protected void writePageHeadExtensions() throws IOException {}
}
