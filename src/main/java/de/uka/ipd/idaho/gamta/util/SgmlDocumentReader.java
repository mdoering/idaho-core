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


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Stack;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Utility for reading GAMTA documents from HTML/XML data provided by some File,
 * InputStream, or Reader.
 * 
 * @author sautter
 */
public class SgmlDocumentReader extends TokenReceiver {
	
	private static final String DOCUMENT_PROPERTY_PREFIX = "DP_";
	
	private static final boolean DEBUG = false;
	
	private MutableAnnotation document;
	
	private Grammar grammar;
	private Properties tagMapping;
	private StringVector ignoreTags;
	private StringVector paragraphTags;
	
	private int maxTokens = 0;
	private char lastChar = StringUtils.NULLCHAR;
	private boolean inProlog = true;
	
	private Stack stack = new Stack();
	private ArrayList annotations = new ArrayList();
	
	/**	Constructor
	 * @param	document		the Document to store the content in 
	 * @param	grammar			the Grammar for determining the tags
	 * @param	tagMapping		a Properties mapping tag types to Annotation types
	 * @param	ignoreTags		a list of tag types to ignore (lookup BEFORE tag mapping is applied)
	 * @param	paragraphTags	a list of tag types the last Token before which to mark as paragraph ending (lookup AFTER tag mapping is applied)
	 * @throws IOException
	 */
	public SgmlDocumentReader(MutableAnnotation document, Grammar grammar, Properties tagMapping, StringVector ignoreTags, StringVector paragraphTags) throws IOException {
		this(document, grammar, tagMapping, ignoreTags, paragraphTags, 0);
	}
	
	/**	Constructor
	 * @param	document		the Document to store the content in 
	 * @param	grammar			the Grammar for determining the tags
	 * @param	tagMapping		a Properties mapping tag types to Annotation types
	 * @param	ignoreTags		a list of tag types to ignore (lookup BEFORE tag mapping is applied)
	 * @param	paragraphTags	a list of tag types the last Token before which to mark as paragraph ending (lookup AFTER tag mapping is applied)
	 * @param	maxTokens		an upper bound for the number of Tokens in the Document (0 means no limit)
	 * @throws IOException
	 */
	public SgmlDocumentReader(MutableAnnotation document, Grammar grammar, Properties tagMapping, StringVector ignoreTags, StringVector paragraphTags, int maxTokens) throws IOException {
		this.document = ((document == null) ? Gamta.newDocument(Gamta.INNER_PUNCTUATION_TOKENIZER) : document);
		this.grammar = ((grammar == null) ? new StandardGrammar() : grammar);
		this.tagMapping = ((tagMapping == null) ? new Properties() : tagMapping);
		this.ignoreTags = ((ignoreTags == null) ? new StringVector() : ignoreTags);
		this.paragraphTags = ((paragraphTags == null) ? new StringVector() : paragraphTags);
		this.maxTokens = maxTokens;
		
		this.paragraphTags.addElementIgnoreDuplicates(MutableAnnotation.SECTION_TYPE);
		this.paragraphTags.addElementIgnoreDuplicates(MutableAnnotation.SUB_SECTION_TYPE);
		this.paragraphTags.addElementIgnoreDuplicates(MutableAnnotation.SUB_SUB_SECTION_TYPE);
		this.paragraphTags.addElementIgnoreDuplicates(MutableAnnotation.PARAGRAPH_TYPE);
		
		this.ignoreTags.addElementIgnoreDuplicates(DocumentRoot.DOCUMENT_TYPE);
	}
	
	/**	@return	the document
	 */
	public MutableAnnotation getDocument() {
		return this.document;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver#close()
	 */
	public void close() throws IOException {
		
		//	close all remaining Annotations (if any)
		while (!this.stack.isEmpty()) {
			AnnotationContainer ac = ((AnnotationContainer) this.stack.pop());
			ac.size = (this.document.size() - ac.start);
		}
		
		//	write Annotations
		for (int a = 0; a < this.annotations.size(); a++) {
			AnnotationContainer ac = ((AnnotationContainer) this.annotations.get(a));
			if (ac.size == 0)
				continue;
			
			//	add annotation
			String annotationType = this.tagMapping.getProperty(ac.type, ac.type);
			MutableAnnotation annotation = this.document.addAnnotation(annotationType, ac.start, ac.size);
			
			//	mark paragraph end if necessary
			if (this.paragraphTags.containsIgnoreCase(annotationType))
				annotation.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			
			//	transfer attributes
			String[] attributeNames = ac.attributes.getAttributeNames();
			for (int n = 0; n < attributeNames.length; n++) {
				if ("id".equalsIgnoreCase(attributeNames[n])) {
					String id = ac.attributes.getAttribute(attributeNames[n]);
					if (id.matches("[0-9A-Fa-f]{32}")) {
						annotation.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, id.toUpperCase());
						continue;
					}
				}
				annotation.setAttribute(attributeNames[n], ac.attributes.getAttribute(attributeNames[n]));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver#storeToken(java.lang.String, int)
	 */
	public void storeToken(String token, int treeDepth) throws IOException {
		if (DEBUG) System.out.println("SgmlDocumentReader: got token - " + token);
		
		//	tagging information, ignore processing instructions and !DOCTYPE, etc.
		if (this.grammar.isTag(token) && !token.startsWith("<?") && !token.startsWith("<!")) {
			this.inProlog = false;
			String type = this.grammar.getType(token);
			if (DEBUG) System.out.println("   it's a tag - type " + type);
			
			//	read document root tag for attributes
			if ((this.document.size() == 0) && DocumentRoot.DOCUMENT_TYPE.equals(type) && !this.grammar.isEndTag(token)) {
				TreeNodeAttributeSet docAttributes = TreeNodeAttributeSet.getTagAttributes(token, this.grammar);
				String[] attributeNames = docAttributes.getAttributeNames();
				for (int n = 0; n < attributeNames.length; n++) {
					
					//	set document property
					if ((this.document instanceof DocumentRoot) && (attributeNames[n].startsWith(DOCUMENT_PROPERTY_PREFIX)))
						((DocumentRoot) this.document).setDocumentProperty(attributeNames[n].substring(DOCUMENT_PROPERTY_PREFIX.length()), docAttributes.getAttribute(attributeNames[n]));
					
					//	annotation nesting order
					else if (DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE.equals(attributeNames[n])) {
						if (this.document instanceof DocumentRoot)
							((DocumentRoot) this.document).setAnnotationNestingOrder(docAttributes.getAttribute(attributeNames[n]));
					}
					
					//	set attribute regularly
					this.document.setAttribute(attributeNames[n], docAttributes.getAttribute(attributeNames[n]));
				}
			}
			
			//	relevant tag
			else if (!this.ignoreTags.containsIgnoreCase(type)) {
				if (DEBUG) System.out.println("   processing tag ...");
				
				//	append line of dashes for HR tag in HTML 
				if ("<hr>".equalsIgnoreCase(token)) {
					if (this.document.size() != 0) {
						if (DEBUG) System.out.println("   it's a horizontal row");
						this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
						
						if (this.lastChar != '\n')
							this.document.addChar('\n');
						
						AnnotationContainer ac = new AnnotationContainer(MutableAnnotation.PARAGRAPH_TYPE, this.document.size(), TreeNodeAttributeSet.getTagAttributes(token, this.grammar));
						
						int oldSize = this.document.size();
						this.document.addTokens("-------------------------------- page break --------------------------------");
						this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
						
						this.document.addChar('\n');
						this.lastChar = '\n';
						
						ac.size = (this.document.size() - oldSize);
						this.annotations.add(ac);
					}
				}
				
				//	end of Annotation
				else if (this.grammar.isEndTag(token)) {
					if (DEBUG) System.out.println("   it's an end tag");
					
					//	any Annotation open?
					if (!this.stack.isEmpty()) {
						AnnotationContainer ac = ((AnnotationContainer) this.stack.pop());
						ac.size = (this.document.size() - ac.start);
						if (DEBUG) System.out.println("   closed Annotation - type " + ac.type);
						
						//	paragraph end
						if (this.paragraphTags.containsIgnoreCase(type) && (this.document.size() != 0)) {
							this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
							if (this.lastChar > 32)
								this.document.addChar('\n');
							this.lastChar = '\n';
						}
					}
				}
				
				//	start of Annotation
				else if (!this.grammar.isSingularTag(token)) {
					if (DEBUG) System.out.println("   it's a start tag");
					AnnotationContainer ac = new AnnotationContainer(type, this.document.size(), TreeNodeAttributeSet.getTagAttributes(token, this.grammar));
					this.stack.push(ac);
					this.annotations.add(ac);
					if (DEBUG) System.out.println("   opened Annotation - type " + ac.type);
				}
				
				//	singular tag, probable line break in HTML
				else if (this.paragraphTags.containsIgnoreCase(type) && (this.document.size() != 0)) {
					if (DEBUG) System.out.println("   it's a singular tag");
					this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
					
					if (this.lastChar > 32)
						this.document.addChar('\n');
					
					this.lastChar = '\n';
				}
			}
			else if (DEBUG) System.out.println("   ignored");
		}
		
		//	textual content before first tag
		else if (this.inProlog) {
			if (DEBUG) System.out.println("   prolog ignored");
		}
		
		//	textual content
		else if ((this.maxTokens < 1) || (this.document.size() < this.maxTokens)) {
			int oldSize = this.document.size();
			
			String plain;
			if (this.grammar.isStrictXML())
				plain = this.grammar.unescape(token).trim();
			else plain = IoTools.prepareForPlainText(token).trim();
			
			if (plain.length() != 0) {// ignore whitespace between tags and text
				TokenSequence ts = this.document.getTokenizer().tokenize(plain);
				
				//	make sure tokens before and after tags do not cling together
				if ((this.document.size() != 0) && (ts.size() != 0) && (this.lastChar > 32) && Gamta.insertSpace(this.document.lastToken(), ts.firstToken()))
					this.document.addChar(' ');
				
				this.document.addTokens(ts);
				this.lastChar = plain.charAt(plain.length() - 1);
			}
			
			for (int t = oldSize; t < this.document.size(); t++) 
				this.document.tokenAt(t).removeAttribute(Token.PARAGRAPH_END_ATTRIBUTE);
		}
	}
	
	/**	representation for Annotations that are not yet complete
	 */
	private class AnnotationContainer {
		private String type;
		private int start;
		private int size = 1;
		private TreeNodeAttributeSet attributes;
		AnnotationContainer(String type, int start, TreeNodeAttributeSet attributes) {
			this.type = type;
			this.start = start;
			this.attributes = attributes;
		}
	}
	
	private static final Parser PARSER = new Parser();
	
	/**	read a Document from an XML file using default settings
	 * @param	fileName	the path and name of the file to read from
	 * @return a document created from the data in the specified file
	 */
	public static DocumentRoot readDocument(String fileName) throws IOException {
		return readDocument(new File(fileName));
	}
	
	/**	read a Document from an XML file using default settings
	 * @param	file	the file to read from
	 * @return a document created from the data in the specified file
	 */
	public static DocumentRoot readDocument(File file) throws IOException {
		Reader reader = new FileReader(file);
		DocumentRoot doc = readDocument(reader);
		reader.close();
		return doc;
	}
	
	/**	read a Document from an input stream using default settings
	 * @param	input	the InputStream to read from
	 * @return a document created from the data form the specified InputStream
	 */
	public static DocumentRoot readDocument(InputStream input) throws IOException {
		return readDocument(new InputStreamReader(input));
	}
	
	/**	read a Document from an XML file using default settings
	 * @param	reader	the file to read from
	 * @return a document created from the data form the specified Reader
	 */
	public static DocumentRoot readDocument(Reader reader) throws IOException {
		SgmlDocumentReader dc = new SgmlDocumentReader(null, null, null, null, null);
		PARSER.stream(reader, dc);
		dc.close();
		return ((DocumentRoot) dc.getDocument());
	}
	
	/**	read a Document from an XML file using default settings
	 * @param	fileName	the path and name of the file to read from
	 * @param	document	the document to append the file content to
	 * @return the number of tokens added to the specified document
	 */
	public static int readDocument(String fileName, MutableAnnotation document) throws IOException {
		return readDocument(new File(fileName), document);
	}
	
	/**	read a Document from an XML file using default settings
	 * @param	file		the file to read from
	 * @param	document	the document to append the file content to
	 * @return the number of tokens added to the specified document
	 */
	public static int readDocument(File file, MutableAnnotation document) throws IOException {
		Reader reader = new FileReader(file);
		int read = readDocument(reader, document);
		reader.close();
		return read;
	}
	
	/**	read a Document from an input stream using default settings
	 * @param	input		the InputStream to read from
	 * @param	document	the document to append the input stream's content to
	 * @return the number of tokens added to the specified document
	 */
	public static int readDocument(InputStream input, MutableAnnotation document) throws IOException {
		return readDocument(new InputStreamReader(input), document);
	}
	
	/**	read a Document from an XML file using default settings
	 * @param	reader	the file to read from
	 * @param	document	the document to append the reader's content to
	 * @return the number of tokens added to the specified document
	 */
	public static int readDocument(Reader reader, MutableAnnotation document) throws IOException {
		int oldSize = document.size();
		SgmlDocumentReader dc = new SgmlDocumentReader(document, null, null, null, null);
		PARSER.stream(reader, dc);
		dc.close();
		return (document.size() - oldSize);
	}
}