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


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Utility class for storing and reading GAMTA documents in the generic GAMTA
 * format, from and to arbitrary sources. The generic GAMTA format is different
 * from XML in a sense that it can represent Annotations that are not properly
 * nested, i.e. would not result in a wellformed XML document. This is done by
 * representing the start and end of an annotation with empty XML elememnts -
 * the start marker element holding the attributes of the Annotation.
 * 
 * @author sautter
 */
public class GenericGamtaXML extends TokenReceiver {
	
	private static final String DOCUMENT_PROPERTY_PREFIX = "DP_";
	
	private MutableAnnotation document;
	private ArrayList annotations = new ArrayList();
	private HashMap annotationsByNumber = new HashMap();
	
	private TreeNodeAttributeSet tokenAttributes = null;
	
	private GenericGamtaXML(MutableAnnotation document) throws IOException {
		this.document = document;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver#close()
	 */
	public void close() throws IOException {
		
		//	write Annotations
		for (int a = 0; a < this.annotations.size(); a++) {
			GamtaAnnotationContainer ac = ((GamtaAnnotationContainer) this.annotations.get(a));
			Annotation annotation;
			
			//	Annotation
			annotation = this.document.addAnnotation(ac.type, ac.start, ac.size);
			
			//	transfer attributes
			if (annotation != null) {
				String[] attributeNames = ac.attributes.getAttributeNames();
				for (int n = 0; n < attributeNames.length; n++)
					annotation.setAttribute(attributeNames[n], unescapeAttributeValue(ac.attributes.getAttribute(attributeNames[n])));
			}
		}
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver#storeToken(java.lang.String, int)
	 */
	public void storeToken(String token, int treeDepth) throws IOException {
		
		//	Annotation marker
		if (GRAMMAR.isTag(token)) {
			String partType = GRAMMAR.getType(token);
			
			TreeNodeAttributeSet attributes = TreeNodeAttributeSet.getTagAttributes(token, GRAMMAR);
			
			String type = attributes.getAttribute(TYPE_ATTRIBUTE, partType);
			attributes.removeAttribute(TYPE_ATTRIBUTE);
			
			//	document root
			if (DocumentRoot.DOCUMENT_TYPE.equals(type)) {
				String[] attributeNames = attributes.getAttributeNames();
				for (int n = 0; n < attributeNames.length; n++) {
					
					//	document property
					if ((this.document instanceof DocumentRoot) && attributeNames[n].startsWith(DOCUMENT_PROPERTY_PREFIX))
						((DocumentRoot) this.document).setDocumentProperty(attributeNames[n].substring(DOCUMENT_PROPERTY_PREFIX.length()), unescapeAttributeValue(attributes.getAttribute(attributeNames[n])));
					
					//	annotation nesting order
					else if (DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE.equals(attributeNames[n])) {
						if (this.document instanceof DocumentRoot)
							((DocumentRoot) this.document).setAnnotationNestingOrder(attributes.getAttribute(attributeNames[n]));
					}
					
					//	reguilar attribute
					else this.document.setAttribute(attributeNames[n], unescapeAttributeValue(attributes.getAttribute(attributeNames[n])));
				}
			}
			
			//	end marker of document, ignore it
			else if (("<" + ANNOTATION_END_PREFIX + "-1/>").equals(token)) {}
			
			//	line break
			else if (LINE_BREAK_TAG.equals(token)) {
				if (this.document.size() != 0) {
					this.document.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
					this.document.addChar('\n');
				}
			}
			
			//	attribute carrier for token
			else if (TOKEN_TAG.equals(type))
				this.tokenAttributes = attributes;
				
			//	start or end marker for Annotation
			else {
				if (partType.startsWith(ANNOTATION_START_PREFIX)) {
					String annotNumber = partType.substring(ANNOTATION_START_PREFIX.length());
					GamtaAnnotationContainer gac = new GamtaAnnotationContainer(type, this.document.size(), attributes);
					this.annotationsByNumber.put(annotNumber, gac);
					this.annotations.add(gac);
				}
				else if (partType.startsWith(ANNOTATION_END_PREFIX)) {
					String annotNumber = partType.substring(ANNOTATION_END_PREFIX.length());
					GamtaAnnotationContainer gac = ((GamtaAnnotationContainer) this.annotationsByNumber.get(annotNumber));
					gac.size = (this.document.size() - gac.start);
				}
			}
		}
		
		//	textual content
		else {
			int aTokenIndex = this.document.size();
			this.document.addTokens(AnnotationUtils.unescapeFromXml(token));
			if ((this.document.size() > aTokenIndex) && (this.tokenAttributes != null)) {
				Token aToken = this.document.tokenAt(aTokenIndex);
				String[] attributeNames = this.tokenAttributes.getAttributeNames();
				for (int n = 0; n < attributeNames.length; n++)
					aToken.setAttribute(attributeNames[n], unescapeAttributeValue(this.tokenAttributes.getAttribute(attributeNames[n])));
				this.tokenAttributes = null;
			}
//			this.document.addTokens(AnnotationUtils.unescapeFromXml(token));
//			if (this.tokenAttributes != null) {
//				Token last = this.document.lastToken();
//				String[] attributeNames = this.tokenAttributes.getAttributeNames();
//				for (int n = 0; n < attributeNames.length; n++)
//					last.setAttribute(attributeNames[n], unescapeAttributeValue(this.tokenAttributes.getAttribute(attributeNames[n])));
//				this.tokenAttributes = null;
//			}
		}
	}
	
	private static final Grammar GRAMMAR = new GenericGamtaGrammar();
	private static final Parser PARSER = new Parser(GRAMMAR);
	
	private static final String ANNOTATION_START_PREFIX = "sAnnot_";
	private static final String ANNOTATION_END_PREFIX = "eAnnot_";
	
	private static final String TYPE_ATTRIBUTE = "t_y_p_e";
	
	private static final String N_ATTRIBUTE_LINE_BREAK_MARKER = "#\\n#";
	private static final String R_ATTRIBUTE_LINE_BREAK_MARKER = "#\\r#";
	
	private static final String TOKEN_TAG = "token";
	private static final String LINE_BREAK_TAG = "<BR/>";
	
	/** read a document from a file and append it to an existing document
	 * @param	fileName	the path and name of the file to read
	 * @param	document	the document to append the file content to
	 * @return the number of tokens added to the specified document
	 * @throws IOException
	 */
	public static int readDocument(String fileName, MutableAnnotation document) throws IOException {
		return readDocument(new File(fileName), document);
	}
	
	/** read a document from a file and append it to an existing document
	 * @param	file		the file to read from
	 * @param	document	the document to append the file content to
	 * @return the number of tokens added to the specified document
	 * @throws IOException
	 */
	public static int readDocument(File file, MutableAnnotation document) throws IOException {
		FileReader fr = new FileReader(file);
		int tokenCount = readDocument(fr, document);
		fr.close();
		return tokenCount;
	}
	
	/** read a document from an input stream and append it to an existing document
	 * @param	source		the input straem to read from
	 * @param	document	the document to append the input stream's content to
	 * @return the number of tokens added to the specified document
	 * @throws IOException
	 */
	public static int readDocument(InputStream source, MutableAnnotation document) throws IOException {
		return readDocument(new InputStreamReader(source), document);
	}
	
	/** read a document from a reader and append it to an existing document
	 * @param	source		the reader to read from
	 * @param	document	the document to append the reader's data to
	 * @return the number of tokens added to the specified document
	 * @throws IOException
	 */
	public static int readDocument(Reader source, MutableAnnotation document) throws IOException {
		int sizeBefore = document.size();
		GenericGamtaXML reader = new GenericGamtaXML(document);
		PARSER.stream(source, reader);
		reader.close();
		return (document.size() - sizeBefore);
	}
	
	/** read a document from a file
	 * @param	fileName	the path and name of the file to read
	 * @return the document read from the file
	 * @throws IOException
	 */
	public static DocumentRoot readDocument(String fileName) throws IOException {
		return readDocument(new File(fileName));
	}
	
	/** read a document from a file
	 * @param	file	the file to read from
	 * @return the document read from the specified file
	 * @throws IOException
	 */
	public static DocumentRoot readDocument(File file) throws IOException {
//		FileReader fr = new FileReader(file);
//		DocumentRoot doc = readDocument(fr);
//		fr.close();
		FileInputStream fis = new FileInputStream(file);
		DocumentRoot doc = readDocument(fis);
		fis.close();
		return doc;
	}
	
	/**
	 * Read a document from an input stream. The data on the stream is decoded
	 * as UTF-8.
	 * @param source the input straem to read from
	 * @return the document read from the input stream
	 * @throws IOException
	 */
	public static DocumentRoot readDocument(InputStream source) throws IOException {
		return readDocument(new InputStreamReader(source, "UTF-8"));
	}
	
	/** read a document from a reader
	 * @param	source	the reader to read from
	 * @return the document read from the reader
	 * @throws IOException
	 */
	public static DocumentRoot readDocument(Reader source) throws IOException {
		DocumentRoot document = Gamta.newDocument(Gamta.INNER_PUNCTUATION_TOKENIZER);
		GenericGamtaXML reader = new GenericGamtaXML(document);
		PARSER.stream(source, reader);
		reader.close();
		return document;
	}
	
	/**
	 * write a document part to a file in generic GAMTA format. The document is
	 * written without any intermediate line breaks. Thus, a newline can be used
	 * to mark the end of the document data.
	 * @param data the DocumentPart to write
	 * @param fileName the path and name of the file to write to
	 * @return true if and only if the specified DocumentPart is written
	 *         successfully
	 */
	public static boolean storeDocument(QueriableAnnotation data, String fileName) {
		return storeDocument(data, new File(fileName));
	}
	
	/**
	 * write a document part to a File in generic GAMTA format. The document is
	 * written without any intermediate line breaks. Thus, a newline can be used
	 * to mark the end of the document data.
	 * @param data the DocumentPart to write
	 * @param file the File to write to
	 * @return true if and only if the specified DocumentPart is written
	 *         successfully
	 */
	public static boolean storeDocument(QueriableAnnotation data, File file) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			storeDocument(data, fos);
			fos.close();
			return true;
		}
		catch (IOException ioe) {
			return false;
		}
	}
	
	/**
	 * Write a document to an arbitrary OutpurStream in generic GAMTA XML
	 * format. The characters will be UTF-8 encoded. The document is written
	 * without any intermediate line breaks. Thus, a newline can be used to mark
	 * the end of the document data.
	 * @param data the DocumentPart to write
	 * @param output the OutpurStream to write to
	 * @return true
	 * @throws IOException
	 */
	public static boolean storeDocument(QueriableAnnotation data, OutputStream output) throws IOException {
		return storeDocument(data, new OutputStreamWriter(output, "UTF-8"));
	}
	
	/**
	 * write a document part to an arbitrary writer in generic GAMTA format. The
	 * document is written without any intermediate line breaks. Thus, a newline
	 * can be used to mark the end of the document data.
	 * @param data the DocumentPart to write
	 * @param output the Writer to write to
	 * @return true
	 * @throws IOException
	 */
	public static boolean storeDocument(QueriableAnnotation data, Writer output) throws IOException {
		BufferedWriter buf = ((output instanceof BufferedWriter) ? ((BufferedWriter) output) : new BufferedWriter(output));
		
		int annotationPointer = 0;
		QueriableAnnotation[] nestedAnnotations = data.getAnnotations();
		HashMap annotationsByEndIndex = new HashMap();
		HashMap annotationNumbersByID = new HashMap();
		
		//	add generic document tag if not given
		if ((nestedAnnotations.length == 0) || !DocumentRoot.DOCUMENT_TYPE.equals(nestedAnnotations[0].getType())) {
			StringBuffer attributes = new StringBuffer(" " + TYPE_ATTRIBUTE + "=\"" + DocumentRoot.DOCUMENT_TYPE + "\"");
			
			//	add ID attribute
			attributes.append(" " + Annotation.ANNOTATION_ID_ATTRIBUTE + "=\"" + data.getAnnotationID() + "\"");
			
			//	add attributes
			String attributeNames[] = data.getAttributeNames();
			for (int n = 0; n < attributeNames.length; n++) {
				Object o = data.getAttribute(attributeNames[n]);
				if (o instanceof String) attributes.append(" " + attributeNames[n] + "=\"" + escapeAttributeValue(o.toString()) + "\"");
			}
			
			//	add annotation nesting order if different from natural one
			if (!DocumentRoot.DEFAULT_ANNOTATION_NESTING_ORDER.equals(data.getAnnotationNestingOrder()))
				attributes.append(" " + DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE + "=\"" + data.getAnnotationNestingOrder() + "\"");
			
			//	add document properties for first Annotation
			String documentPropertyNames[] = data.getDocumentPropertyNames();
			for (int n = 0; n < documentPropertyNames.length; n++)
				attributes.append(" " + DOCUMENT_PROPERTY_PREFIX + documentPropertyNames[n] + "=\"" + escapeAttributeValue(data.getDocumentProperty(documentPropertyNames[n])) + "\"");
			
			buf.write("<" + ANNOTATION_START_PREFIX + "-1" + attributes.toString() + "/>");
		}
		
		Token token = null;
		Token lastToken;
		
		for (int t = 0; t < data.size(); t++) {
			
			//	switch to next Token
			lastToken = token;
			token = data.tokenAt(t);
			
			//	write end markers of annotations ending before current token
			LinkedList endingAnnotationList = ((LinkedList) annotationsByEndIndex.remove(new Integer(t)));
			if (endingAnnotationList != null) {
				while (endingAnnotationList.size() != 0) {
					Annotation annotation = ((Annotation) endingAnnotationList.removeLast());
					Integer annotationNumber = ((Integer) annotationNumbersByID.remove(annotation.getAnnotationID()));
					buf.write(produceEndTag(annotation, annotationNumber.intValue()));
				}
//				for (int a = 0; a < endingAnnotationList.size(); a++) {
//					Annotation annotation = ((Annotation) endingAnnotationList.get(a));
//					Integer annotationNumber = ((Integer) annotationNumbersByID.remove(annotation.getAnnotationID()));
//					buf.write(produceEndTag(annotation, annotationNumber.intValue()));
//				}
//				endingAnnotationList.clear();
			}
			
			//	write a single space character if previous token has whitespace after it and is not a line break
			if (((lastToken != null) && !lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) && (t != 0) && (data.getWhitespaceAfter(t-1).length() != 0))
				buf.write(" ");
			
			//	write start markers for Annotations beginning at current Token
			while ((annotationPointer < nestedAnnotations.length) && (nestedAnnotations[annotationPointer].getStartIndex() == t)) {
				QueriableAnnotation annotation = nestedAnnotations[annotationPointer];
				buf.write(produceStartTag(annotation, annotationPointer));
				
				//	store annotation in index so end marker can be written at appropriate position
				Integer endingListKey = new Integer(annotation.getEndIndex());
				LinkedList endingList = ((LinkedList) annotationsByEndIndex.get(endingListKey));
				if (endingList == null) {
					endingList = new LinkedList();
					annotationsByEndIndex.put(endingListKey, endingList);
				}
				endingList.add(annotation);
				annotationNumbersByID.put(annotation.getAnnotationID(), new Integer(annotationPointer));
				
				annotationPointer++;
			}
			
			//	write Token attributes if given
			String tokenTag = produceTag(token);
			if (tokenTag != null) buf.write(tokenTag);
			
			//	append current Token
			buf.write(AnnotationUtils.escapeForXml(token.getValue()));
			
			//	add line break at end of paragraph
			if (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) buf.write(LINE_BREAK_TAG);
		}
		
		//	write end markers of annotations ending at end of document
		LinkedList endingAnnotationList = ((LinkedList) annotationsByEndIndex.remove(new Integer(data.size())));
		if (endingAnnotationList != null) {
//			for (int a = 0; a < endingAnnotationList.size(); a++) {
//				Annotation annotation = ((Annotation) endingAnnotationList.get(a));
//				Integer annotationNumber = ((Integer) annotationNumbersByID.remove(annotation.getAnnotationID()));
//				buf.write(produceEndTag(annotation, annotationNumber.intValue()));
//			}
			while (endingAnnotationList.size() != 0) {
				Annotation annotation = ((Annotation) endingAnnotationList.removeLast());
				Integer annotationNumber = ((Integer) annotationNumbersByID.remove(annotation.getAnnotationID()));
				buf.write(produceEndTag(annotation, annotationNumber.intValue()));
			}
//			endingAnnotationList.clear();
		}
		
		//	close generic document tag if given
		if ((nestedAnnotations.length == 0) || !DocumentRoot.DOCUMENT_TYPE.equals(nestedAnnotations[0].getType()))
			buf.write("<" + ANNOTATION_END_PREFIX + "-1" + "/>");
		
		//	we're done
		buf.flush();
		return true;
	}
	
	private static String produceStartTag(QueriableAnnotation annotation, int annotationNumber) {
		String type = annotation.getType();
		if (type == null) type = "generic";
		StringBuffer attributes = new StringBuffer(" " + TYPE_ATTRIBUTE + "=\"" + type + "\"");
		
		//	add ID attribute
		attributes.append(" " + Annotation.ANNOTATION_ID_ATTRIBUTE + "=\"" + annotation.getAnnotationID() + "\"");
		
		//	add attributes
		String attributeNames[] = annotation.getAttributeNames();
		for (int n = 0; n < attributeNames.length; n++) {
			Object o = annotation.getAttribute(attributeNames[n]);
			if (o instanceof String) attributes.append(" " + attributeNames[n] + "=\"" + escapeAttributeValue(o.toString()) + "\"");
		}
		
		//	add document properties for first Annotation
		if ((annotationNumber == 0) && DocumentRoot.DOCUMENT_TYPE.equals(type)) {
			String documentPropertyNames[] = annotation.getDocumentPropertyNames();
			for (int n = 0; n < documentPropertyNames.length; n++)
				attributes.append(" " + DOCUMENT_PROPERTY_PREFIX + documentPropertyNames[n] + "=\"" + escapeAttributeValue(annotation.getDocumentProperty(documentPropertyNames[n])) + "\"");
			
			//	add annotation nesting order if different from natural one
			if (!DocumentRoot.DEFAULT_ANNOTATION_NESTING_ORDER.equals(annotation.getAnnotationNestingOrder()))
				attributes.append(" " + DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE + "=\"" + annotation.getAnnotationNestingOrder() + "\"");
		}
		
		//	return tag
		return ("<" + ANNOTATION_START_PREFIX + annotationNumber + attributes.toString() + "/>");
	}
	
	private static String escapeAttributeValue(String value) {
		if (value == null) return null;
		value = AnnotationUtils.escapeForXml(value, true);
		StringBuffer escapedValue = new StringBuffer(value.length());
		for (int c = 0; c < value.length(); c++) {
			char ch = value.charAt(c);
			if (ch == '\n')
				escapedValue.append(N_ATTRIBUTE_LINE_BREAK_MARKER);
			else if (ch == '\r')
				escapedValue.append(R_ATTRIBUTE_LINE_BREAK_MARKER);
			else escapedValue.append(ch);
		}
		return escapedValue.toString();
	}
	
	private static String unescapeAttributeValue(String escapedValue) {
		if (escapedValue == null) return null;
		StringBuffer value = new StringBuffer(escapedValue.length());
		for (int c = 0; c < escapedValue.length(); c++) {
			char ch = escapedValue.charAt(c);
			if (ch == '#') {
				if (escapedValue.startsWith(N_ATTRIBUTE_LINE_BREAK_MARKER, c)) {
					value.append('\n');
					c += (N_ATTRIBUTE_LINE_BREAK_MARKER.length() - 1);
				}
				else if (escapedValue.startsWith(R_ATTRIBUTE_LINE_BREAK_MARKER, c)) {
					value.append('\r');
					c += (R_ATTRIBUTE_LINE_BREAK_MARKER.length() - 1);
				}
			}
			else value.append(ch);
		}
		return value.toString(); // XML un-escaping is in TreeNodeAttributeSet now
	}
	
	private static String produceEndTag(Annotation annotation, int annotationNumber) {
		return ("<" + ANNOTATION_END_PREFIX + annotationNumber + "/>");
	}
	
	private static String produceTag(Token token) {
		String attributeNames[] = token.getAttributeNames();
		StringBuffer attributes = new StringBuffer();
		for (int n = 0; n < attributeNames.length; n++) {
			Object o = token.getAttribute(attributeNames[n]);
			if (o instanceof String)
				attributes.append(" " + attributeNames[n] + "=\"" + escapeAttributeValue(o.toString()) + "\"");
		}
		return ((attributes.length() == 0) ? null : ("<" + TOKEN_TAG + attributes.toString() + "/>"));
	}
	

	private static class GenericGamtaGrammar extends StandardGrammar {
		
		/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isTag(java.lang.String)
		 */
		public boolean isTag(String tag) {
			if (!super.isTag(tag) || (tag == null)) return false;
			return (tag.startsWith("<" + ANNOTATION_START_PREFIX) ||
					tag.startsWith("<" + ANNOTATION_END_PREFIX) ||
					tag.startsWith("<" + TOKEN_TAG) || 
					tag.equals(LINE_BREAK_TAG));
		}
	}

	private static class GamtaAnnotationContainer {
		String type;
		int start;
		int size;
		TreeNodeAttributeSet attributes;
		
		private GamtaAnnotationContainer(String type, int start, TreeNodeAttributeSet attributes) {
			this.type = type;
			this.start = start;
			this.attributes = attributes;
		}
	}
	
	/**
	 * Specialized input stream for transfering the generic GAMTA XML
	 * representation of a document. This class allows for manipulating the
	 * document's attributes without actually instantiating the whole document.
	 * This is helpful in cases where a document is transferred through a pipe
	 * or a network connection, being used as a fully instantiated document only
	 * on the receiving end of the connection. In particular, it allows avoiding
	 * to instatiate the whole document on the sending side of the connection.
	 * The data provided by streams of this class is intended only for two
	 * purposes: to be decoded by GenericGamtaXML.readDocument(), or to be
	 * written to an output stream and the decoded on this streams receiving
	 * end. It is the instantiating code's responsibility to make sure the
	 * underlying input stream in fact provides a document in generic GAMTA XML
	 * serialized format.<br>
	 * Manipulation of the attributes and document properties is allowed only
	 * before the first invocation of any of the read() methods. All the
	 * attribute and document property modification methods will throw an
	 * IllegalStateException after the first invocation of any of the read()
	 * methods, though getAttribute(), getAttributeNames(), hasAttribute(),
	 * getDocumentProperty(), and getDocumentPropertyNames() will still work.
	 * 
	 * @author sautter
	 */
	public static class DocumentInputStream extends InputStream implements Attributed {
		
		private BufferedInputStream in;
		private int ostLength;
		private int docLength;
		private boolean closed = false;
		
		private ByteArrayInputStream startTagBuffer = null;
		
		/**
		 * Constructor
		 * @param in
		 * @param docLength
		 */
		public DocumentInputStream(InputStream in, int docLength) throws IOException {
			this.in = ((in instanceof BufferedInputStream) ? ((BufferedInputStream) in) : new BufferedInputStream(in));
			this.docLength = docLength;
			
			//	read start tag and parse attributes
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int lastByte = 0;
			int currentByte = 0;
			while (currentByte != -1) {
				lastByte = currentByte;
				currentByte = this.in.read();
				baos.write(currentByte);
				if ((currentByte == '>') && (lastByte == '/'))
					currentByte = -1;
			}
			
			//	check success
			if (lastByte != '/')
				throw new IOException("Document tag broken");
			
			//	get naked bytes
			byte[] stBytes = baos.toByteArray();
			this.ostLength = stBytes.length;
			
			//	instantiate start tag and get annotation number
			String startTag = new String(stBytes, "UTF-8");
			String type = GRAMMAR.getType(startTag);
			this.annotationNumber = (type.endsWith("0") ? 0 : -1);
			
			//	parse and store attributes and document properties
			TreeNodeAttributeSet attributes = TreeNodeAttributeSet.getTagAttributes(startTag, GRAMMAR);
			attributes.removeAttribute(TYPE_ATTRIBUTE);
			String[] attributeNames = attributes.getAttributeNames();
			for (int n = 0; n < attributeNames.length; n++) {
				
				//	annotation ID
				if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(attributeNames[n]))
					this.annotationId = unescapeAttributeValue(attributes.getAttribute(attributeNames[n]));
				
				//	document property
				else if (attributeNames[n].startsWith(DOCUMENT_PROPERTY_PREFIX))
					this.setDocumentProperty(attributeNames[n].substring(DOCUMENT_PROPERTY_PREFIX.length()), unescapeAttributeValue(attributes.getAttribute(attributeNames[n])));
				
				//	annotation nesting order
				else if (DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE.equals(attributeNames[n]))
					this.annotationNestingOrder = attributes.getAttribute(attributeNames[n]);
				
				//	regular attribute
				else this.setAttribute(attributeNames[n], unescapeAttributeValue(attributes.getAttribute(attributeNames[n])));
			}
		}
		
		/* (non-Javadoc)
		 * @see java.io.InputStream#read()
		 */
		public int read() throws IOException {
			if (this.closed) return -1;
			if (this.startTagBuffer == null)
				this.produceStartTagBuffer();
			if (this.startTagBuffer.available() != 0)
				return this.startTagBuffer.read();
			else return this.in.read();
		}
		
		/**
		 * @return in bytes, the length of the generic GAMTA XML representation
		 *         of the document to receive through this stream.
		 */
		public int docLength() throws IOException {
			return this.docLength;
		}
		
		/* (non-Javadoc)
		 * @see java.io.InputStream#read(byte[], int, int)
		 */
		public int read(byte[] b, int off, int len) throws IOException {
			if (this.closed) return -1;
			if ((this.startTagBuffer == null) || (this.startTagBuffer.available() != 0))
				return super.read(b, off, len);
			else return this.in.read(b, off, len);
		}
		
		/* (non-Javadoc)
		 * @see java.io.InputStream#close()
		 */
		public void close() throws IOException {
			this.closed = true;
			this.in.close();
		}
		
		private int annotationNumber;
		private String annotationId;
		private String annotationNestingOrder;
		private HashMap attributes = new HashMap();
		private Properties documentProperties = new Properties();
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#clearAttributes()
		 */
		public void clearAttributes() {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Attributes can be modified only before reading from this stream.");
			this.attributes.clear();
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#copyAttributes(de.uka.ipd.idaho.gamta.Attributed)
		 */
		public void copyAttributes(Attributed source) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Attributes can be modified only before reading from this stream.");
			if (source != null) {
				String[] attributeNames = source.getAttributeNames();
				if (attributeNames.length == 0) return; // no need for initializing map if no attributes to copy
				for (int a = 0; a < attributeNames.length; a++) 
					this.attributes.put(attributeNames[a], source.getAttribute(attributeNames[a]));
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#getAttribute(java.lang.String, java.lang.Object)
		 */
		public Object getAttribute(String name, Object def) {
			return (this.attributes.containsKey(name) ? this.attributes.get(name) : def);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.attributes.get(name);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#getAttributeNames()
		 */
		public String[] getAttributeNames() {
			ArrayList names = new ArrayList(this.attributes.keySet());
			Collections.sort(names);
			return ((String[]) names.toArray(new String[names.size()]));
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return this.attributes.containsKey(name);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#removeAttribute(java.lang.String)
		 */
		public Object removeAttribute(String name) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Attributes can be modified only before reading from this stream.");
			return this.attributes.remove(name);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#setAttribute(java.lang.String, java.lang.Object)
		 */
		public Object setAttribute(String name, Object value) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Attributes can be modified only before reading from this stream.");
			if (value == null)
				return this.removeAttribute(name);
			else return this.attributes.put(name, value);
		}
		
		/* (non-Javadoc)
		 * @see de.gamta.Annotation#getDocumentProperty(java.lang.String)
		 */
		public String getDocumentProperty(String propertyName) {
			return this.getDocumentProperty(propertyName, null);
		}
		
		/* (non-Javadoc)
		 * @see de.gamta.Annotation#getDocumentProperty(java.lang.String, java.lang.String)
		 */
		public String getDocumentProperty(String propertyName, String defaultValue) {
			return this.documentProperties.getProperty(propertyName, defaultValue);
		}

		/* (non-Javadoc)
		 * @see de.gamta.Annotation#getDocumentPropertyNames()
		 */
		public String[] getDocumentPropertyNames() {
			ArrayList names = new ArrayList(this.documentProperties.keySet());
			Collections.sort(names);
			return ((String[]) names.toArray(new String[names.size()]));
		}
		
		/* (non-Javadoc)
		 * @see de.gamta.DocumentRoot#setDocumentProperty(java.lang.String, java.lang.String)
		 */
		public String setDocumentProperty(String propertyName, String value) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Document properties can be modified only before reading from this stream.");
			return ((String) this.documentProperties.setProperty(propertyName, value));
		}

		/* (non-Javadoc)
		 * @see de.gamta.DocumentRoot#removeDocumentProperty(java.lang.String)
		 */
		public String removeDocumentProperty(String propertyName) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Document properties can be modified only before reading from this stream.");
			return ((String) this.documentProperties.remove(propertyName));
		}

		/* (non-Javadoc)
		 * @see de.gamta.DocumentRoot#clearDocumentProperties()
		 */
		public void clearDocumentProperties() {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Document properties can be modified only before reading from this stream.");
			this.documentProperties.clear();
		}
		
		private final void produceStartTagBuffer() throws IOException {
			StringBuffer attributes = new StringBuffer(" " + TYPE_ATTRIBUTE + "=\"" + DocumentRoot.DOCUMENT_TYPE + "\"");
			
			//	add ID attribute
			attributes.append(" " + Annotation.ANNOTATION_ID_ATTRIBUTE + "=\"" + this.annotationId + "\"");
			
			//	add attributes
			String attributeNames[] = this.getAttributeNames();
			for (int n = 0; n < attributeNames.length; n++) {
				Object o = this.getAttribute(attributeNames[n]);
				if (o instanceof String) attributes.append(" " + attributeNames[n] + "=\"" + escapeAttributeValue(o.toString()) + "\"");
			}
			
			//	add document properties
			String documentPropertyNames[] = this.getDocumentPropertyNames();
			for (int n = 0; n < documentPropertyNames.length; n++)
				attributes.append(" " + DOCUMENT_PROPERTY_PREFIX + documentPropertyNames[n] + "=\"" + escapeAttributeValue(this.getDocumentProperty(documentPropertyNames[n])) + "\"");
			
			//	add annotation nesting order if different from natural one
			if (!DocumentRoot.DEFAULT_ANNOTATION_NESTING_ORDER.equals(this.annotationNestingOrder) && (this.annotationNestingOrder != null))
				attributes.append(" " + DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE + "=\"" + this.annotationNestingOrder + "\"");
			
			//	create tag buffer
			byte[] stBytes = ("<" + ANNOTATION_START_PREFIX + this.annotationNumber + attributes.toString() + "/>").getBytes("UTF-8");
			this.startTagBuffer = new ByteArrayInputStream(stBytes);
			this.docLength += (stBytes.length - this.ostLength);
		}
	}
	
	
	/**
	 * Specialized reader for transfering the generic GAMTA XML representation
	 * of a document. This class allows for manipulating the document's
	 * attributes without actually instantiating the whole document. This is
	 * helpful in cases where a document is transferred through a pipe or a
	 * network connection, being used as a fully instantiated document only on
	 * the receiving end of the connection. In particular, it allows avoiding to
	 * instatiate the whole document on the sending side of the connection. The
	 * data provided by readers of this class is intended only for two purposes:
	 * to be decoded by GenericGamtaXML.readDocument(), or to be written to a
	 * writer and the decoded on this writer's receiving end. It is the
	 * instantiating code's responsibility to make sure the underlying reader in
	 * fact provides a document in generic GAMTA XML serialized format.<br>
	 * Manipulation of the attributes and document properties is allowed only
	 * before the first invocation of any of the read() methods. All the
	 * attribute and document property modification methods will throw an
	 * IllegalStateException after the first invocation of any of the read()
	 * methods, though getAttribute(), getAttributeNames(), hasAttribute(),
	 * getDocumentProperty(), and getDocumentPropertyNames() will still work.
	 * 
	 * @author sautter
	 */
	public static class DocumentReader extends Reader implements Attributed {
		
		private BufferedReader in;
		private int ostLength = 0;
		private int docLength;
		private boolean closed = false;
		
		private StringReader startTagBuffer = null;
		
		/**
		 * Constructor
		 * @param in
		 * @param docLength
		 */
		public DocumentReader(Reader in, int docLength) throws IOException {
			this.in = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			this.docLength = docLength;
			
			//	read start tag and parse attributes
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int lastByte = 0;
			int currentByte = 0;
			while (currentByte != -1) {
				lastByte = currentByte;
				currentByte = this.in.read();
				baos.write(currentByte);
				if ((currentByte == '>') && (lastByte == '/'))
					currentByte = -1;
			}
			
			//	check success
			if (lastByte != '/')
				throw new IOException("Document tag broken");
			
			//	instantiate start tag and get annotation number
			String startTag = new String(baos.toByteArray(), "UTF-8");
			this.ostLength = startTag.length();
			String type = GRAMMAR.getType(startTag);
			this.annotationNumber = (type.endsWith("0") ? 0 : -1);
			
			//	parse and store attributes and document properties
			TreeNodeAttributeSet attributes = TreeNodeAttributeSet.getTagAttributes(startTag, GRAMMAR);
			attributes.removeAttribute(TYPE_ATTRIBUTE);
			String[] attributeNames = attributes.getAttributeNames();
			for (int n = 0; n < attributeNames.length; n++) {
				
				//	annotation ID
				if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(attributeNames[n]))
					this.annotationId = unescapeAttributeValue(attributes.getAttribute(attributeNames[n]));
				
				//	document property
				else if (attributeNames[n].startsWith(DOCUMENT_PROPERTY_PREFIX))
					this.setDocumentProperty(attributeNames[n].substring(DOCUMENT_PROPERTY_PREFIX.length()), unescapeAttributeValue(attributes.getAttribute(attributeNames[n])));
				
				//	annotation nesting order
				else if (DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE.equals(attributeNames[n]))
					this.annotationNestingOrder = attributes.getAttribute(attributeNames[n]);
				
				//	regular attribute
				else this.setAttribute(attributeNames[n], unescapeAttributeValue(attributes.getAttribute(attributeNames[n])));
			}
		}
		
		/**
		 * @return in bytes, the length of the generic GAMTA XML representation
		 *         of the document to receive through this reader.
		 */
		public int docLength() throws IOException {
			return this.docLength;
		}
		
		/* (non-Javadoc)
		 * @see java.io.Reader#close()
		 */
		public void close() throws IOException {
			this.closed = true;
			this.in.close();
		}
		
		/* (non-Javadoc)
		 * @see java.io.Reader#read(char[], int, int)
		 */
		public int read(char[] cbuf, int off, int len) throws IOException {
			if (this.closed) return -1;
			
			if (this.startTagBuffer == null)
				this.produceStartTagBuffer();
			
			int read = this.startTagBuffer.read(cbuf, off, len);
			if (read == -1)
				return this.in.read(cbuf, off, len);
			else if (len == read)
				return read;
			else return (read + this.in.read(cbuf, (off + read), (len - read)));
		}
		
		private int annotationNumber;
		private String annotationId;
		private String annotationNestingOrder;
		private HashMap attributes = new HashMap();
		private Properties documentProperties = new Properties();
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#clearAttributes()
		 */
		public void clearAttributes() {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Attributes can be modified only before reading from this stream.");
			this.attributes.clear();
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#copyAttributes(de.uka.ipd.idaho.gamta.Attributed)
		 */
		public void copyAttributes(Attributed source) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Attributes can be modified only before reading from this stream.");
			if (source != null) {
				String[] attributeNames = source.getAttributeNames();
				if (attributeNames.length == 0) return; // no need for initializing map if no attributes to copy
				for (int a = 0; a < attributeNames.length; a++) 
					this.attributes.put(attributeNames[a], source.getAttribute(attributeNames[a]));
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#getAttribute(java.lang.String, java.lang.Object)
		 */
		public Object getAttribute(String name, Object def) {
			return (this.attributes.containsKey(name) ? this.attributes.get(name) : def);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.attributes.get(name);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#getAttributeNames()
		 */
		public String[] getAttributeNames() {
			ArrayList names = new ArrayList(this.attributes.keySet());
			Collections.sort(names);
			return ((String[]) names.toArray(new String[names.size()]));
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return this.attributes.containsKey(name);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#removeAttribute(java.lang.String)
		 */
		public Object removeAttribute(String name) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Attributes can be modified only before reading from this stream.");
			return this.attributes.remove(name);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.Attributed#setAttribute(java.lang.String, java.lang.Object)
		 */
		public Object setAttribute(String name, Object value) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Attributes can be modified only before reading from this stream.");
			if (value == null)
				return this.removeAttribute(name);
			else return this.attributes.put(name, value);
		}
		
		/* (non-Javadoc)
		 * @see de.gamta.Annotation#getDocumentProperty(java.lang.String)
		 */
		public String getDocumentProperty(String propertyName) {
			return this.getDocumentProperty(propertyName, null);
		}
		
		/* (non-Javadoc)
		 * @see de.gamta.Annotation#getDocumentProperty(java.lang.String, java.lang.String)
		 */
		public String getDocumentProperty(String propertyName, String defaultValue) {
			return this.documentProperties.getProperty(propertyName, defaultValue);
		}

		/* (non-Javadoc)
		 * @see de.gamta.Annotation#getDocumentPropertyNames()
		 */
		public String[] getDocumentPropertyNames() {
			ArrayList names = new ArrayList(this.documentProperties.keySet());
			Collections.sort(names);
			return ((String[]) names.toArray(new String[names.size()]));
		}
		
		/* (non-Javadoc)
		 * @see de.gamta.DocumentRoot#setDocumentProperty(java.lang.String, java.lang.String)
		 */
		public String setDocumentProperty(String propertyName, String value) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Document properties can be modified only before reading from this stream.");
			return ((String) this.documentProperties.setProperty(propertyName, value));
		}

		/* (non-Javadoc)
		 * @see de.gamta.DocumentRoot#removeDocumentProperty(java.lang.String)
		 */
		public String removeDocumentProperty(String propertyName) {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Document properties can be modified only before reading from this stream.");
			return ((String) this.documentProperties.remove(propertyName));
		}

		/* (non-Javadoc)
		 * @see de.gamta.DocumentRoot#clearDocumentProperties()
		 */
		public void clearDocumentProperties() {
			if (this.startTagBuffer != null)
				throw new IllegalStateException("Document properties can be modified only before reading from this stream.");
			this.documentProperties.clear();
		}
		
		private final void produceStartTagBuffer() throws IOException {
			StringBuffer attributes = new StringBuffer(" " + TYPE_ATTRIBUTE + "=\"" + DocumentRoot.DOCUMENT_TYPE + "\"");
			
			//	add ID attribute
			attributes.append(" " + Annotation.ANNOTATION_ID_ATTRIBUTE + "=\"" + this.annotationId + "\"");
			
			//	add attributes
			String attributeNames[] = this.getAttributeNames();
			for (int n = 0; n < attributeNames.length; n++) {
				Object o = this.getAttribute(attributeNames[n]);
				if (o instanceof String) attributes.append(" " + attributeNames[n] + "=\"" + escapeAttributeValue(o.toString()) + "\"");
			}
			
			//	add document properties
			String documentPropertyNames[] = this.getDocumentPropertyNames();
			for (int n = 0; n < documentPropertyNames.length; n++)
				attributes.append(" " + DOCUMENT_PROPERTY_PREFIX + documentPropertyNames[n] + "=\"" + escapeAttributeValue(this.getDocumentProperty(documentPropertyNames[n])) + "\"");
			
			//	add annotation nesting order if different from natural one
			if (!DocumentRoot.DEFAULT_ANNOTATION_NESTING_ORDER.equals(this.annotationNestingOrder) && (this.annotationNestingOrder != null))
				attributes.append(" " + DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE + "=\"" + this.annotationNestingOrder + "\"");
			
			//	create tag buffer
			String startTag = ("<" + ANNOTATION_START_PREFIX + this.annotationNumber + attributes.toString() + "/>");
			this.startTagBuffer = new StringReader(startTag);
			this.docLength += (startTag.length() - this.ostLength);
		}
	}
	
	/**
	 * Instances of this specialized input stream act rather similar to
	 * DocumentInputStream objects, the only difference being that the data read
	 * from them is not encoded in generic GAMTA XML, but in regular XML. This
	 * class is intended to wrap a document input stream whose content is to be
	 * streamed somewhere where normal XML is expected, like an XSLT
	 * transformer.
	 * 
	 * @author sautter
	 */
	public static class XmlDocumentInputStream extends PipedInputStream {
		private DocumentInputStream in;
		
		private Thread parserThread;
		private IOException parserException;
		
		/**
		 * Constructor
		 * @param in the document input stream to wrap
		 */
		public XmlDocumentInputStream(DocumentInputStream in) throws IOException {
			this(in, null, null);
		}
		/**
		 * Constructor
		 * @param in the document input stream to wrap
		 * @param annotationTypeFilter a set containing the annotation types to
		 *            restrict the output to (specifying null will write all
		 *            annotations)
		 * @param attributeFilter a set containing the names of the attributes
		 *            to include in the tags (specifying null will include all
		 *            attributes)
		 * @throws IOException
		 */
		public XmlDocumentInputStream(DocumentInputStream in, final Set annotationTypeFilter, final Set attributeFilter) throws IOException {
			this.in = in;
			final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new PipedOutputStream(this), "UTF-8"));
			this.parserThread = new Thread() {
				public void run() {
					try {
						TokenReceiver tr = new ConverterTokenReceiver(out, annotationTypeFilter, attributeFilter);
						PARSER.stream(new InputStreamReader(XmlDocumentInputStream.this.in, "UTF-8"), tr);
						tr.close();
					}
					catch (IOException ioe) {
						XmlDocumentInputStream.this.parserException = ioe;
					}
				}
			};
			this.parserThread.start();
		}
		
		/* (non-Javadoc)
		 * @see java.io.PipedInputStream#close()
		 */
		public void close() throws IOException {
			this.in.close();
			try {
				this.parserThread.join();
			} catch (InterruptedException e) {}
			super.close();
		}
		
		/* (non-Javadoc)
		 * @see java.io.PipedInputStream#read()
		 */
		public synchronized int read() throws IOException {
			int read = super.read();
			if (this.parserException != null)
				throw this.parserException;
			else return read;
		}
		
		/* (non-Javadoc)
		 * @see java.io.PipedInputStream#read(byte[], int, int)
		 */
		public synchronized int read(byte[] b, int off, int len) throws IOException {
			int read = super.read(b, off, len);
			if (this.parserException != null)
				throw this.parserException;
			else return read;
		}
	}
	
	/**
	 * Instances of this specialized reader act rather similar to DocumentReader
	 * objects, the only difference being that the data read from them is not
	 * encoded in generic GAMTA XML, but in regular XML. This class is intended
	 * to wrap a document reader whose content is to be streamed somewhere where
	 * normal XML is expected, like an XSLT transformer.
	 * 
	 * @author sautter
	 */
	public static class XmlDocumentReader extends PipedReader {
		private DocumentReader in;
		
		private Thread parserThread;
		private IOException parserException;
		
		/**
		 * Constructor
		 * @param in the document reader to wrap
		 */
		public XmlDocumentReader(DocumentReader in) throws IOException {
			this(in, null, null);
		}
		
		/**
		 * Constructor
		 * @param in the document reader to wrap
		 * @param annotationTypeFilter a set containing the annotation types to
		 *            restrict the output to (specifying null will write all
		 *            annotations)
		 * @param attributeFilter a set containing the names of the attributes
		 *            to include in the tags (specifying null will include all
		 *            attributes)
		 * @throws IOException
		 */
		public XmlDocumentReader(DocumentReader in, final Set annotationTypeFilter, final Set attributeFilter) throws IOException {
			this.in = in;
			final BufferedWriter out = new BufferedWriter(new PipedWriter(this));
			this.parserThread = new Thread() {
				public void run() {
					try {
						TokenReceiver tr = new ConverterTokenReceiver(out, annotationTypeFilter, attributeFilter);
						PARSER.stream(XmlDocumentReader.this.in, tr);
						tr.close();
					}
					catch (IOException ioe) {
						XmlDocumentReader.this.parserException = ioe;
					}
				}
			};
			this.parserThread.start();
		}
		
		/* (non-Javadoc)
		 * @see java.io.PipedReader#close()
		 */
		public void close() throws IOException {
			this.in.close();
			try {
				this.parserThread.join();
			} catch (InterruptedException e) {}
			super.close();
		}
		
		/* (non-Javadoc)
		 * @see java.io.PipedReader#read()
		 */
		public synchronized int read() throws IOException {
			int read = super.read();
			if (this.parserException != null)
				throw this.parserException;
			else return read;
		}
		
		/* (non-Javadoc)
		 * @see java.io.PipedReader#read(char[], int, int)
		 */
		public synchronized int read(char[] cbuf, int off, int len) throws IOException {
			int read = super.read(cbuf, off, len);
			if (this.parserException != null)
				throw this.parserException;
			else return read;
		}
	}
	
	private static final class ConverterTokenReceiver extends TokenReceiver {
		private HashMap typesByAnnotationsNumbers = new HashMap();
		private TreeSet openAnnotationNumbers = new TreeSet();
		private TreeSet toCloseAnnotationNumbers = new TreeSet();
		
		private boolean lastWasLineBreak = true;
		
		private BufferedWriter out;
		private Set annotationTypeFilter;
		private Set attributeFilter;
		
		ConverterTokenReceiver(BufferedWriter out, Set annotationTypeFilter, Set attributeFilter) throws IOException {
			this.out = out;
			this.annotationTypeFilter = annotationTypeFilter;
			this.attributeFilter = attributeFilter;
		}
		
		public void close() throws IOException {
			while (this.openAnnotationNumbers.size() != 0) {
				Integer toClose = ((Integer) this.openAnnotationNumbers.last());
				this.openAnnotationNumbers.remove(toClose);
				this.toCloseAnnotationNumbers.remove(toClose);
				
				if (!this.lastWasLineBreak)
					this.out.newLine();
				
				String annotType = ((String) this.typesByAnnotationsNumbers.get(toClose));
				this.out.write("</" + annotType + ">");
				this.out.newLine();
				this.lastWasLineBreak = true;
			}
			this.out.flush();
			this.out.close();
		}
		public void storeToken(String token, int treeDepth) throws IOException {
			
			//	Annotation marker
			if (GRAMMAR.isTag(token)) {
				String partType = GRAMMAR.getType(token);
				
				TreeNodeAttributeSet attributes = TreeNodeAttributeSet.getTagAttributes(token, GRAMMAR);
				
				String type = attributes.getAttribute(TYPE_ATTRIBUTE, partType);
				attributes.removeAttribute(TYPE_ATTRIBUTE);
				
				//	document root
				if (DocumentRoot.DOCUMENT_TYPE.equals(type)) {
					String[] attributeNames = attributes.getAttributeNames();
					for (int n = 0; n < attributeNames.length; n++) {
						
						//	document property
						if (attributeNames[n].startsWith(DOCUMENT_PROPERTY_PREFIX))
							attributes.removeAttribute(attributeNames[n]);
						
						//	annotation nesting order
						else if (DocumentRoot.ANNOTATION_NESTING_ORDER_ATTRIBUTE.equals(attributeNames[n]))
							attributes.removeAttribute(attributeNames[n]);
					}
				}
				
				//	line break
				if (LINE_BREAK_TAG.equals(token)) {
					if (!this.lastWasLineBreak)
						this.out.newLine();
					this.lastWasLineBreak = true;
				}
				
				//	attribute carrier for token
				else if (TOKEN_TAG.equals(type)) {}
				
				//	start or end marker for Annotation
				else {
					if (partType.startsWith(ANNOTATION_START_PREFIX)) {
						
						//	check filter
						if ((this.annotationTypeFilter != null) && !this.annotationTypeFilter.contains(type))
							return;
						
						//	line break
						if (!this.lastWasLineBreak)
							this.out.newLine();
						
						//	add start tag
						this.out.write("<");
						this.out.write(type);
						String[] ans = attributes.getAttributeNames();
						for (int a = 0; a < ans.length; a++) {
							if (Annotation.ANNOTATION_ID_ATTRIBUTE.equals(ans[a]))
								continue;
							
							if ((this.attributeFilter == null) || this.attributeFilter.contains(ans[a])) {
								Object value = attributes.getAttribute(ans[a]);
								if (value != null)
									this.out.write(" " + ans[a] + "=\"" + GRAMMAR.escape(value.toString()) + "\"");
							}
						}
						this.out.write(">");

						this.out.newLine();
						this.lastWasLineBreak = true;
						
						Integer annotNumber = new Integer(partType.substring(ANNOTATION_START_PREFIX.length()));
						
						this.typesByAnnotationsNumbers.put(annotNumber, type);
						this.openAnnotationNumbers.add(annotNumber);
					}
					
					else if (partType.startsWith(ANNOTATION_END_PREFIX)) {
						Integer annotNumber = new Integer(partType.substring(ANNOTATION_END_PREFIX.length()));
						String annotType = ((String) this.typesByAnnotationsNumbers.get(annotNumber));
						
						//	start tag filtered out
						if (annotType == null)
							return;
						
						//	get last open annotation
						Integer latestOpen = (this.openAnnotationNumbers.isEmpty() ? annotNumber : ((Integer) this.openAnnotationNumbers.last()));
						
						//	we're fine, everything well-formed
						if (annotNumber.intValue() == latestOpen.intValue()) {
							this.openAnnotationNumbers.remove(annotNumber);
							
							//	line break
							if (!this.lastWasLineBreak) 
								this.out.newLine();
							
							//	write tag and line break
							this.out.write("</" + annotType + ">");
							this.out.newLine();
							this.lastWasLineBreak = true;
							
							//	close pending annotations (if any)
							while (this.toCloseAnnotationNumbers.size() != 0) {
								Integer toClose = ((Integer) this.toCloseAnnotationNumbers.last());
								this.openAnnotationNumbers.remove(toClose);
								this.toCloseAnnotationNumbers.remove(toClose);
								
								annotType = ((String) this.typesByAnnotationsNumbers.get(toClose));
								this.out.write("</" + annotType + ">");
								this.out.newLine();
								this.lastWasLineBreak = true;
							}
						}
						
						//	we'll have to wait for some other annotation to close
						else this.toCloseAnnotationNumbers.add(annotNumber);
					}
				}
			}
			
			//	textual content
			else if (token.trim().length() != 0) {
				this.out.write(token);
				this.lastWasLineBreak = false;
			}
			
			this.out.flush();
		}
	}
}
