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


import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;

/**
 * A Reader implementation similar to StringReader, but using a
 * QueriableAnnotation as its source instead of a String. Text and attribute
 * values are escaped to comply with XML syntax rules.
 * 
 * @author sautter
 */
public class AnnotationReader extends Reader {
	
	private QueriableAnnotation source;
	private String indent = null;
	private boolean outputIDs = false;
	
	private Annotation[] nestedAnnotations;
	private Stack stack = new Stack();
	private int annotationPointer = 0;
	
	private Token token = null;
	private Token lastToken;
	
	private int tokenIndex = 0;
	
	private boolean lastWasLineBreak = false;
	private boolean lastWasTag = true;
	
	private StringBuffer lineAssembler = new StringBuffer();
	private LinkedList lineBuffer = new LinkedList();
	private int bufferLevel = 0;
	private String string = "";
	private int stringOffset = 0;
	
	private HashSet lineBroken = new HashSet();
	
	private Set attributeFilter;
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 */
	public AnnotationReader(QueriableAnnotation source) {
		this(source, false, null, null, null);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 */
	public AnnotationReader(QueriableAnnotation source, boolean outputIDs) {
		this(source, outputIDs, null, null, null);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 */
	public AnnotationReader(QueriableAnnotation source, String indent) {
		this(source, false, indent, null, null);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 */
	public AnnotationReader(QueriableAnnotation source, boolean outputIDs, String indent) {
		this(source, outputIDs, indent, null, null);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationReader(QueriableAnnotation source, Set typeFilter, Set attributeFilter) {
		this(source, false, null, typeFilter, attributeFilter);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationReader(QueriableAnnotation source, boolean outputIDs, Set typeFilter, Set attributeFilter) {
		this(source, outputIDs, null, typeFilter, attributeFilter);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationReader(QueriableAnnotation source, String indent, Set typeFilter, Set attributeFilter) {
		this(source, false, indent, typeFilter, attributeFilter);
	}
	
	/**
	 * Constructor
	 * @param source the DocumentPart to read from
	 * @param outputIDs include annotation IDs in the output?
	 * @param indent the String to insert for each level of indentation
	 *            (specifying null will result in no indentation)
	 * @param typeFilter a set containing the types of the annotations to
	 *            include in the output (specifying null will include all
	 *            attributes)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 */
	public AnnotationReader(QueriableAnnotation source, boolean outputIDs, String indent, Set typeFilter, Set attributeFilter) {
		this.source = source;
		this.outputIDs = outputIDs;
		this.indent = (((indent == null) || (indent.length() == 0)) ? null : indent);
		this.attributeFilter = attributeFilter;
		
		//	get nested annotations
		this.nestedAnnotations = this.source.getAnnotations();
		
		//	apply filter if given
		if (typeFilter != null) {
			ArrayList typeFilteredAnnotations = new ArrayList();
			for (int a = 0; a < this.nestedAnnotations.length; a++)
				if (typeFilter.contains(this.nestedAnnotations[a].getType()))
					typeFilteredAnnotations.add(this.nestedAnnotations[a]);
			this.nestedAnnotations = ((Annotation[]) typeFilteredAnnotations.toArray(new Annotation[typeFilteredAnnotations.size()]));
		}
		
		//	make sure there is a root element
		if ((this.nestedAnnotations.length == 0) || (this.nestedAnnotations[0].size() < this.source.size())) {
			Annotation[] nestedAnnotations = new Annotation[this.nestedAnnotations.length + 1];
			nestedAnnotations[0] = this.source;
			System.arraycopy(this.nestedAnnotations, 0, nestedAnnotations, 1, this.nestedAnnotations.length);
			this.nestedAnnotations = nestedAnnotations;
		}
	}
	
	/** @see java.io.Reader#close()
	 */
	public void close() throws IOException {
		this.source = null;
		
		this.lineBuffer = null;
		this.lineAssembler = null;
		this.string = null;
		
		this.nestedAnnotations = null;
		this.stack = null;
		this.token = null;
		this.lastToken = null;
	}
	
	/*
	 * @see java.io.Reader#read(char[], int, int)
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		if (this.source == null)
			throw new IOException("Stream closed");
		
		if (this.bufferLevel < len) {
			int added = this.fillBuffer((len - this.bufferLevel));
			
			if ((this.bufferLevel == 0) && (added == 0)) return -1;
			else this.bufferLevel += added;
		}
		
		int w = 0;
		while (w < len) {
			if (this.stringOffset == this.string.length()) {
				if (this.lineBuffer.isEmpty()) return w;
				this.string = this.lineBuffer.removeFirst().toString();
				this.stringOffset = 0;
			}
			else {
				cbuf[off] = this.string.charAt(this.stringOffset);
				this.stringOffset++;
				this.bufferLevel--;
				off++;
				w++;
			}
		}
		
		return len;
	}
	
	/*	produce some Strings for the buffer
	 * @param	minChars	the minimum number of chars to produce
	 * @return the number of chars actually produced (if less than minChars, the end is near)
	 */
	private int fillBuffer(int minChars) {
		int newChars = 0;
		
		while (this.tokenIndex < this.source.size()) {
			
			//	switch to next Token
			this.lastToken = this.token;
			this.token = this.source.tokenAt(this.tokenIndex);
			
			//	write end tags for Annotations ending before current Token
			while ((this.stack.size() > 0) && ((((Annotation) this.stack.peek()).getStartIndex() + ((Annotation) this.stack.peek()).size()) <= this.tokenIndex)) {
				Annotation annotation = ((Annotation) this.stack.pop());
				
				//	line break only if nested Annotations
				if (!this.lastWasLineBreak && this.lineBroken.contains(annotation.getAnnotationID())) {
					this.lineAssembler.append("\n");
					this.lineBuffer.addLast(this.lineAssembler.toString());
					newChars += this.lineAssembler.length();
					this.lineAssembler = new StringBuffer();
				}
				
				//	add indent
				if (this.lastWasLineBreak && (this.indent != null))
					for (int i = 0; i < this.stack.size(); i++)
						this.lineAssembler.append(this.indent);
				
				//	store line
				this.lineAssembler.append("</" + annotation.getType() + ">");
				this.lineAssembler.append("\n");
				this.lineBuffer.addLast(this.lineAssembler.toString());
				newChars += this.lineAssembler.length();
				this.lineAssembler = new StringBuffer();
				
				this.lastWasLineBreak = true;
				this.lastWasTag = true;
			}
			
			//	insert line break if required
			if (!this.lastWasLineBreak && (this.lastToken != null) && this.lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
				this.lineAssembler.append("\n");
				this.lineBuffer.addLast(this.lineAssembler.toString());
				newChars += this.lineAssembler.length();
				this.lineAssembler = new StringBuffer();
				this.lastWasLineBreak = true;
			}
			
			//	skip space character before unspaced punctuation (e.g. ','), after line breaks and tags, and if there is no whitespace in the token sequence
			if (!this.lastWasTag && !this.lastWasLineBreak && 
				(this.lastToken != null) && !this.lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) && 
				Gamta.insertSpace(this.lastToken, this.token) && 
				(this.tokenIndex != 0) && (this.source.getWhitespaceAfter(this.tokenIndex-1).length() != 0)
				) this.lineAssembler.append(" ");
			
			//	write start tags for Annotations beginning at current Token
			while ((this.annotationPointer < this.nestedAnnotations.length) && (this.nestedAnnotations[this.annotationPointer].getStartIndex() == this.tokenIndex)) {
				Annotation annotation = this.nestedAnnotations[this.annotationPointer];
				
				//	line break
				if (!this.lastWasTag) {
					this.lineAssembler.append("\n");
					this.lineBuffer.addLast(this.lineAssembler.toString());
					newChars += this.lineAssembler.length();
					this.lineAssembler = new StringBuffer();
				}
				
				//	add indent
				if (this.lastWasLineBreak && (this.indent != null))
					for (int i = 0; i < this.stack.size(); i++)
						this.lineAssembler.append(this.indent);
				
				this.lineAssembler.append(AnnotationUtils.produceStartTag(annotation, this.outputIDs, this.attributeFilter, true));
				
				this.stack.push(annotation);
				this.annotationPointer++;
				
				//	line break only if nested Annotations
				if ((this.annotationPointer < this.nestedAnnotations.length) && AnnotationUtils.contains(annotation, this.nestedAnnotations[this.annotationPointer])) {
					this.lineAssembler.append("\n");
					this.lineBuffer.addLast(this.lineAssembler.toString());
					newChars += this.lineAssembler.length();
					this.lineAssembler = new StringBuffer();
					this.lineBroken.add(annotation.getAnnotationID());
				}
				
				this.lastWasLineBreak = true;
				this.lastWasTag = true;
			}
			
			//	append current Token
			String tokenValue = this.token.getValue();
			this.lineAssembler.append(AnnotationUtils.escapeForXml(tokenValue));
			this.lastWasLineBreak = false;
			this.lastWasTag = false;
			
			//	switch to next token
			this.tokenIndex++;
			
			//	some token is left for triggering the stack flush, and we've written enough characters, return
			if ((this.tokenIndex < this.source.size()) && (newChars >= minChars))
				return newChars;
		}
		
		//	write end tags for Annotations not closed so far
		while (this.stack.size() > 0) {
			Annotation annotation = ((Annotation) this.stack.pop());
			
			//	line break only if nested Annotations
			if (!this.lastWasLineBreak && this.lineBroken.contains(annotation.getAnnotationID())) {
				this.lineAssembler.append("\n");
				this.lineBuffer.addLast(this.lineAssembler.toString());
				newChars += this.lineAssembler.length();
				this.lineAssembler = new StringBuffer();
				this.lastWasLineBreak = true;
			}
			
			//	add indent
			if (this.lastWasLineBreak && (this.indent != null))
				for (int i = 0; i < this.stack.size(); i++)
					this.lineAssembler.append(this.indent);
			
			//	add end tag
			this.lineAssembler.append("</" + annotation.getType() + ">");
			this.lastWasLineBreak = false;
		}
		
		this.lineBuffer.addLast(this.lineAssembler.toString());
		newChars += this.lineAssembler.length();
		this.lineAssembler = new StringBuffer();
		
		return newChars;
	}
}