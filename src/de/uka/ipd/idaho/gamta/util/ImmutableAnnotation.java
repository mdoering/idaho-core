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


import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;

/**
 * An immutable wrapper implementation of the QueriableAnnotation interface. Any
 * attempt of changing the type, the value, or an attribute of the wrapped
 * Annotation or an Annotation retrieved from this Annotation will result in an
 * exception being thrown.
 * 
 * @author sautter
 */
public class ImmutableAnnotation implements QueriableAnnotation {
	private QueriableAnnotation data; // the annotation to wrap
	
	/** Constructor
	 * @param	data	the annotation to make immutable
	 */
	public ImmutableAnnotation(QueriableAnnotation data) {
		this.data = data;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#changeTypeTo(java.lang.String)
	 */
	public String changeTypeTo(String newType) {
		throw new RuntimeException("Illegal modification of annotation type.");
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#charAt(int)
	 */
	public char charAt(int index) {
		return this.data.charAt(index);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#compareTo(java.lang.Object)
	 */
	public int compareTo(Object obj) {
		return this.data.compareTo(obj);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#firstToken()
	 */
	public Token firstToken() {
		return new ImmutableToken(this.data.firstToken());
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#firstValue()
	 */
	public String firstValue() {
		return this.data.firstValue();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getAbsoluteStartIndex()
	 */
	public int getAbsoluteStartIndex() {
		return this.data.getAbsoluteStartIndex();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getAbsoluteStartOffset()
	 */
	public int getAbsoluteStartOffset() {
		return this.data.getAbsoluteStartOffset();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getAnnotationID()
	 */
	public String getAnnotationID() {
		return this.data.getAnnotationID();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getAnnotations()
	 */
	public QueriableAnnotation[] getAnnotations() {
		QueriableAnnotation[] annotations = this.data.getAnnotations();
		for (int a = 0; a < annotations.length; a++)
			annotations[a] = new ImmutableAnnotation(annotations[a]);
		return annotations;
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getAnnotations(java.lang.String)
	 */
	public QueriableAnnotation[] getAnnotations(String type) {
		QueriableAnnotation[] annotations = this.data.getAnnotations(type);
		for (int a = 0; a < annotations.length; a++)
			annotations[a] = new ImmutableAnnotation(annotations[a]);
		return annotations;
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getAnnotationTypes()
	 */
	public String[] getAnnotationTypes() {
		return this.data.getAnnotationTypes();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getDocumentProperty(java.lang.String, java.lang.String)
	 */
	public String getDocumentProperty(String propertyName, String defaultValue) {
		return this.data.getDocumentProperty(propertyName, defaultValue);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getDocumentProperty(java.lang.String)
	 */
	public String getDocumentProperty(String propertyName) {
		return this.data.getDocumentProperty(propertyName);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getDocumentPropertyNames()
	 */
	public String[] getDocumentPropertyNames() {
		return this.data.getDocumentPropertyNames();
	}

	/* (non-Javadoc)
	 * @see de.gamta.QueriableAnnotation#getAnnotationNestingOrder()
	 */
	public String getAnnotationNestingOrder() {
		return this.data.getAnnotationNestingOrder();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getEndIndex()
	 */
	public int getEndIndex() {
		return this.data.getEndIndex();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getEndOffset()
	 */
	public int getEndOffset() {
		return this.data.getEndOffset();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getLeadingWhitespace()
	 */
	public String getLeadingWhitespace() {
		return this.data.getLeadingWhitespace();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getStartIndex()
	 */
	public int getStartIndex() {
		return this.data.getStartIndex();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getStartOffset()
	 */
	public int getStartOffset() {
		return this.data.getStartOffset();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getSubsequence(int, int)
	 */
	public TokenSequence getSubsequence(int start, int size) {
		return this.data.getSubsequence(start, size);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getTokenizer()
	 */
	public Tokenizer getTokenizer() {
		return this.data.getTokenizer();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getType()
	 */
	public String getType() {
		return this.data.getType();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getValue()
	 */
	public String getValue() {
		return this.data.getValue();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#getWhitespaceAfter(int)
	 */
	public String getWhitespaceAfter(int index) {
		return this.data.getWhitespaceAfter(index);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#lastToken()
	 */
	public Token lastToken() {
		return new ImmutableToken(this.data.lastToken());
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#lastValue()
	 */
	public String lastValue() {
		return this.data.lastValue();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#length()
	 */
	public int length() {
		return this.data.length();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#size()
	 */
	public int size() {
		return this.data.size();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return this.data.subSequence(start, end);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#tokenAt(int)
	 */
	public Token tokenAt(int index) {
		return new ImmutableToken(this.data.tokenAt(index));
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#toString()
	 */
	public String toString() {
		return this.data.toString();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#toXML()
	 */
	public String toXML() {
		return this.data.toXML();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.GamtaDocument#valueAt(int)
	 */
	public String valueAt(int index) {
		return this.data.valueAt(index);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.AbstractAttributed#clearAttributes()
	 */
	public void clearAttributes() {
		throw new RuntimeException("Illegal modification of annotation attributes.");
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.AbstractAttributed#copyAttributes(de.gamta.Attributed)
	 */
	public void copyAttributes(Attributed source) {
		throw new RuntimeException("Illegal modification of annotation attributes.");
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String, java.lang.Object)
	 */
	public Object getAttribute(String name, Object def) {
		return this.data.getAttribute(name, def);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		return this.data.getAttribute(name);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.AbstractAttributed#getAttributeNames()
	 */
	public String[] getAttributeNames() {
		return this.data.getAttributeNames();
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.AbstractAttributed#hasAttribute(java.lang.String)
	 */
	public boolean hasAttribute(String name) {
		return this.data.hasAttribute(name);
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.AbstractAttributed#removeAttribute(java.lang.String)
	 */
	public Object removeAttribute(String name) {
		throw new RuntimeException("Illegal modification of annotation attributes.");
	}

	/* (non-Javadoc)
	 * @see de.gamta.defaultImplementation.AbstractAttributed#setAttribute(java.lang.String, java.lang.Object)
	 */
	public Object setAttribute(String name, Object value) {
		throw new RuntimeException("Illegal modification of annotation attributes.");
	}
	
	/**	wrapper for a token, blocking any modification
	 */
	private class ImmutableToken implements Token {
		private Token data; // the token to wrap
		private ImmutableToken(Token data) {
			this.data = data;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return this.getValue().equals(obj);
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.getValue().hashCode();
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return this.getValue();
		}
		/* (non-Javadoc)
		 * @see de.gamta.Token#charAt(int)
		 */
		public char charAt(int index) {
			return this.data.charAt(index);
		}
		/* (non-Javadoc)
		 * @see de.gamta.Token#getEndOffset()
		 */
		public int getEndOffset() {
			return (this.data.getEndOffset());
		}
		/* (non-Javadoc)
		 * @see de.gamta.Token#getStartOffset()
		 */
		public int getStartOffset() {
			return (this.data.getStartOffset());
		}
		/* (non-Javadoc)
		 * @see de.gamta.Token#getTokenizer()
		 */
		public Tokenizer getTokenizer() {
			return this.data.getTokenizer();
		}
		/* (non-Javadoc)
		 * @see de.gamta.Token#getValue()
		 */
		public String getValue() {
			return this.data.getValue();
		}
		/* (non-Javadoc)
		 * @see de.gamta.Token#length()
		 */
		public int length() {
			return this.data.length();
		}
		/* (non-Javadoc)
		 * @see de.gamta.Token#subSequence(int, int)
		 */
		public CharSequence subSequence(int start, int end) {
			return this.data.subSequence(start, end);
		}
		/* (non-Javadoc)
		 * @see de.gamta.Attributed#clearAttributes()
		 */
		public void clearAttributes() {
			throw new RuntimeException("Illegal modification of token attributes.");
		}
		/* (non-Javadoc)
		 * @see de.gamta.Attributed#copyAttributes(de.gamta.Attributed)
		 */
		public void copyAttributes(Attributed source) {
			throw new RuntimeException("Illegal modification of token attributes.");
		}
		/* (non-Javadoc)
		 * @see de.gamta.Attributed#getAttribute(java.lang.String, java.lang.Object)
		 */
		public Object getAttribute(String name, Object def) {
			return this.data.getAttribute(name, def);
		}
		/* (non-Javadoc)
		 * @see de.gamta.Attributed#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.data.getAttribute(name);
		}
		/* (non-Javadoc)
		 * @see de.gamta.Attributed#getAttributeNames()
		 */
		public String[] getAttributeNames() {
			return this.data.getAttributeNames();
		}
		/* (non-Javadoc)
		 * @see de.gamta.Attributed#hasAttribute(java.lang.String)
		 */
		public boolean hasAttribute(String name) {
			return this.data.hasAttribute(name);
		}
		/* (non-Javadoc)
		 * @see de.gamta.Attributed#removeAttribute(java.lang.String)
		 */
		public Object removeAttribute(String name) {
			throw new RuntimeException("Illegal modification of token attributes.");
		}
		/* (non-Javadoc)
		 * @see de.gamta.Attributed#setAttribute(java.lang.String, java.lang.Object)
		 */
		public Object setAttribute(String name, Object value) {
			throw new RuntimeException("Illegal modification of token attributes.");
		}
	}
}
