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
package de.uka.ipd.idaho.easyIO.help;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import de.uka.ipd.idaho.easyIO.streams.CharSequenceReader;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;

/**
 * HelpChapter loading its content on demand from a File or URL. This avoids
 * loading all content of a halp into memory on startup, thus preventing pages
 * being loaded that may never be displayed. The first access to the page
 * represented by an instance of this class, however, might take longer, since
 * the page's content has to be loaded dynamically.
 * 
 * @author sautter
 */
public class DynamicHelpChapter extends HelpChapter {
	private static final Parser HTML_PARSER = new Parser(new Html());
	
	private Reader contentReader = null;
	private File contentFile = null;
	private URL contentUrl = null;
	
	private StringBuffer contentBuffer = null;
	
	/**	Constructor
	 * @param	title		the title for this help chapter
	 * @param	contentUrl	the String representation of the URL to load the content form
	 */
	public DynamicHelpChapter(String title, String contentUrl) {
		super(title, ("<HTML>The content of this chapter will be loaded from<BR><TT>" + contentUrl + "</TT><BR>on demand.</HTML>"));
		try {
			this.contentUrl = new URL(contentUrl);
		} catch (MalformedURLException mue) {}
	}
	
	/**	Constructor
	 * @param	title			the title for this help chapter
	 * @param	contentFile		the file to load the content form
	 */
	public DynamicHelpChapter(String title, File contentFile) {
		super(title, ("<HTML>The content of this chapter will be loaded from<BR><TT>" + contentFile.getAbsolutePath() + "</TT><BR>on demand.</HTML>"));
		this.contentFile = contentFile;
	}
	
	/**	Constructor
	 * @param	title		the title for this help chapter
	 * @param	contentUrl	the URL to load the content form
	 */
	public DynamicHelpChapter(String title, URL contentUrl) {
		super(title, ("<HTML>The content of this chapter will be loaded from<BR><TT>" + contentUrl.toString() + "</TT><BR>on demand.</HTML>"));
		this.contentUrl = contentUrl;
	}
	
	/**	Constructor
	 * @param	title		the title for this help chapter
	 * @param	contentReader	the reader to load the content form
	 */
	public DynamicHelpChapter(String title, Reader contentReader) {
		super(title, ("<HTML>The content of this chapter will be loaded on demand.</HTML>"));
		this.contentReader = contentReader;
	}
	
	/** @see de.uka.ipd.idaho.easyIO.help.HelpChapter#getTextReader()
	 */
	public Reader getTextReader() {
		
		//	we've loaded this one before
		if (this.contentBuffer != null)
			return new CharSequenceReader(this.contentBuffer);
		
		//	load and buffer content
		try {
			
			//	create content reader
			Reader contentReader;
			if (this.contentReader != null)
				contentReader = this.contentReader;
			else if (this.contentFile != null)
				contentReader = new InputStreamReader(new FileInputStream(this.contentFile));
			else if (this.contentUrl != null)
				contentReader = new InputStreamReader(this.contentUrl.openStream());
			else contentReader = super.getTextReader();
			
			//	create content buffer
			this.contentBuffer = new StringBuffer();
			
			//	stream content into buffer
			HTML_PARSER.stream(new BufferedReader(contentReader), new TokenReceiver() {
				public void storeToken(String token, int treeDepth) throws IOException {
					if (!token.toLowerCase().startsWith("<meta "))
						contentBuffer.append(token);
					else if (token.toLowerCase().indexOf("content-type") == -1)
						contentBuffer.append(token);
				}
				public void close() throws IOException {}
			});
			
			//	finally ...
			return new CharSequenceReader(this.contentBuffer);
		}
		catch (Exception e) {
			System.out.println("... ERROR");
			return new StringReader("<HTML><TT>" + e.getClass().getName() + "</TT><BR>(" + e.getMessage() + ")<BR>while creating content reader, sorry.</HTML>");
		}
	}
}