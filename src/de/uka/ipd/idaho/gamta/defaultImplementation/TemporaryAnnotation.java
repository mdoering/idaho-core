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
package de.uka.ipd.idaho.gamta.defaultImplementation;


import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.StandaloneAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;

/**
 * @author sautter
 */
public class TemporaryAnnotation extends AbstractAttributed implements StandaloneAnnotation {
	
	private String annotationId = Gamta.getAnnotationID();
	private TokenSequence tokens;
	
	private String type;
	private int startIndex;
	private int size;
	
	/**	Constructor
	 * @param	tokens		the TokenSequence the Annotation refers to
	 * @param	type		the type of the Annotation (specifying null or an empty String will use the default type)
	 * @param	startIndex	the index of the Annotation's first Token in the TokenSequence
	 * @param	size		the number of Tokens the Annotation spans
	 */
	public TemporaryAnnotation(TokenSequence tokens, String type, int startIndex, int size) {
		this.tokens = tokens;
		this.type = (((type == null) || (type.trim().length() == 0)) ? DEFAULT_ANNOTATION_TYPE : type);
		this.startIndex = startIndex;
		this.size = size;
	}
	
	/**	Constructor
	 * @param	tokens		the TokenSequence the Annotation refers to
	 * @param	model		the Annotation to use as a model for this one
	 */
	public TemporaryAnnotation(TokenSequence tokens, Annotation model) {
		this.tokens = tokens;
		this.type = model.getType();
		this.startIndex = model.getStartIndex();
		this.size = model.size();
		this.copyAttributes(model);
	}
	
	/** @see de.uka.ipd.idaho.gamta.StandaloneAnnotation#getData()
	 */
	public TokenSequence getData() {
		return this.tokens;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String)
	 */
	public String getDocumentProperty(String propertyName) {
		return ((this.tokens instanceof Annotation) ? ((Annotation) this.tokens).getDocumentProperty(propertyName) : null);
	}

	/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String, java.lang.String)
	 */
	public String getDocumentProperty(String propertyName, String defaultValue) {
		return ((this.tokens instanceof Annotation) ? ((Annotation) this.tokens).getDocumentProperty(propertyName, defaultValue) : null);
	}

	/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentPropertyNames()
	 */
	public String[] getDocumentPropertyNames() {
		return ((this.tokens instanceof Annotation) ? ((Annotation) this.tokens).getDocumentPropertyNames() : new String[0]);
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		if (START_INDEX_ATTRIBUTE.equals(name)) return new Integer(this.getStartIndex());
		else if (SIZE_ATTRIBUTE.equals(name)) return new Integer(this.size());
		else if (END_INDEX_ATTRIBUTE.equals(name)) return new Integer(this.getEndIndex());
		else if (ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
		else if (ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.annotationId;
		else return super.getAttribute(name);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String, java.lang.Object)
	 */
	public Object getAttribute(String name, Object def) {
		if (START_INDEX_ATTRIBUTE.equals(name)) return new Integer(this.getStartIndex());
		else if (SIZE_ATTRIBUTE.equals(name)) return new Integer(this.size());
		else if (END_INDEX_ATTRIBUTE.equals(name)) return new Integer(this.getEndIndex());
		else if (ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
		else if (ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.annotationId;
		else return super.getAttribute(name, def);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#hasAttribute(java.lang.String)
	 */
	public boolean hasAttribute(String name) {
		return (START_INDEX_ATTRIBUTE.equals(name) || SIZE_ATTRIBUTE.equals(name) || END_INDEX_ATTRIBUTE.equals(name) || ANNOTATION_VALUE_ATTRIBUTE.equals(name) || ANNOTATION_ID_ATTRIBUTE.equals(name) || super.hasAttribute(name));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#setAttribute(java.lang.String, java.lang.Object)
	 */
	public Object setAttribute(String name, Object value) {
		if (START_INDEX_ATTRIBUTE.equals(name) || SIZE_ATTRIBUTE.equals(name) || END_INDEX_ATTRIBUTE.equals(name) || ANNOTATION_VALUE_ATTRIBUTE.equals(name))
			return value;
		else if (ANNOTATION_ID_ATTRIBUTE.equals(name)) {
			if ((value != null) && (value instanceof String) && (value.toString().trim().length() == this.annotationId.length())) {
				String oldId = this.annotationId;
				this.annotationId = value.toString();
				return oldId;
			}
			else return value;
		}
		else return super.setAttribute(name, value);
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
	 */
	public int getStartIndex() {
		return this.startIndex;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
	 */
	public int getEndIndex() {
		return (this.startIndex + this.size);
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getType()
	 */
	public String getType() {
		return this.type;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#changeTypeTo(java.lang.String)
	 */
	public String changeTypeTo(String newType) {
		if ((newType != null) && (newType.trim().length() != 0)) {
			String oldType = this.type;
			this.type = newType;
			return oldType;
		}
		else return newType;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getAnnotationID()
	 */
	public String getAnnotationID() {
		return this.annotationId;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getValue()
	 */
	public String getValue() {
		return this.tokens.subSequence(this.getStartOffset(), this.getEndOffset()).toString();
	}
	
	/** @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.getValue();
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#toXML()
	 */
	public String toXML() {
		return (AnnotationUtils.produceStartTag(this) + AnnotationUtils.escapeForXml(this.getValue()) + AnnotationUtils.produceEndTag(this));
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getDocument()
	 */
	public QueriableAnnotation getDocument() {
		return ((this.tokens instanceof QueriableAnnotation) ? ((QueriableAnnotation) this.tokens) : null);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#tokenAt(int)
	 */
	public Token tokenAt(int index) {
		return this.tokens.tokenAt(this.startIndex + index);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstToken()
	 */
	public Token firstToken() {
		return this.tokens.tokenAt(this.startIndex);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastToken()
	 */
	public Token lastToken() {
		return this.tokens.tokenAt(this.startIndex + this.size - 1);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#valueAt(int)
	 */
	public String valueAt(int index) {
		return this.tokens.valueAt(this.startIndex + index);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstValue()
	 */
	public String firstValue() {
		return this.tokens.valueAt(this.startIndex);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastValue()
	 */
	public String lastValue() {
		return this.tokens.valueAt(this.startIndex + this.size - 1);
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getLeadingWhitespace()
	 */
	public String getLeadingWhitespace() {
		return ((this.startIndex == 0) ? this.tokens.getLeadingWhitespace() : "");
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#size()
	 */
	public int size() {
		return this.size;
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#length()
	 */
	public int length() {
		return (this.getEndOffset() - this.getStartOffset());
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#getTokenizer()
	 */
	public Tokenizer getTokenizer() {
		return this.tokens.getTokenizer();
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#getSubsequence(int, int)
	 */
	public TokenSequence getSubsequence(int start, int size) {
		return this.tokens.getSubsequence((this.startIndex + start), size);
	}
	
	/** @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o instanceof Annotation) {
			int c = AnnotationUtils.compare(this, ((Annotation) o));
			if (c == 0) return this.type.compareTo(((Annotation) o).getType());
			else return c;
		}
		return -1;
	}

	/** @see de.uka.ipd.idaho.gamta.TokenSequence#getWhitespaceAfter(int)
	 */
	public String getWhitespaceAfter(int index) {
		return this.tokens.getWhitespaceAfter(this.getStartIndex() + index);
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.CharSpan#getEndOffset()
	 */
	public int getEndOffset() {
		//	TODO_ne make work if tokens are standalone annotation
		return (this.lastToken().getEndOffset() - this.tokens.firstToken().getStartOffset());
//		return this.lastToken().getEndOffset();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.CharSpan#getStartOffset()
	 */
	public int getStartOffset() {
		//	TODO_ne make work if tokens are standalone annotation
		return (this.firstToken().getStartOffset() - this.tokens.firstToken().getStartOffset());
//		return this.firstToken().getStartOffset();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		return this.tokens.charAt(this.getStartOffset() + index);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return this.tokens.subSequence((this.getStartOffset() + start), (this.getStartOffset() + end));
	}
}
