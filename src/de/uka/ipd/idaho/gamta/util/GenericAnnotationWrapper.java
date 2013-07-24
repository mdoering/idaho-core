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


import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;

/**
 * This class implements a generic wrapper for arbitrary annotations. It loops
 * all method calls through to the wrapped annotations. The purpose of this
 * class is to provide a standard wrapper implementation of all the methods in
 * Annotation in situations where some few of the methods need to be added
 * functionality through a wrapper class. Wrappers can simply extend this class
 * and overwrite methods as needed while avoiding having to implement all the
 * other methods as well. If Tokens retrieved from this wrapper need to be
 * wrapped as well, the respective sub classes of this class should overwrite
 * the wrapToken() method to provide the respective wrappers.
 * 
 * @author sautter
 */
public class GenericAnnotationWrapper implements Annotation {

	/**	the wrapped Annotation */
	protected Annotation annotationData;
	
	/** Constructor
	 * @param	data	the Annotation to wrap
	 */
	public GenericAnnotationWrapper(Annotation data) {
		this.annotationData = data;
	}
	
	public void clearAttributes() {
		this.annotationData.clearAttributes();
	}
	
	public Object removeAttribute(String name) {
		return this.annotationData.removeAttribute(name);
	}
	
	public Object setAttribute(String name, Object value) {
		return this.annotationData.setAttribute(name, value);
	}

	public void copyAttributes(Attributed source) {
		this.annotationData.copyAttributes(source);
	}

	public boolean hasAttribute(String name) {
		return this.annotationData.hasAttribute(name);
	}

	public Object getAttribute(String name, Object def) {
		return this.annotationData.getAttribute(name, def);
	}

	public Object getAttribute(String name) {
		return this.annotationData.getAttribute(name);
	}

	public String[] getAttributeNames() {
		return this.annotationData.getAttributeNames();
	}
	
	public String getDocumentProperty(String propertyName, String defaultValue) {
		return this.annotationData.getDocumentProperty(propertyName, defaultValue);
	}

	public String getDocumentProperty(String propertyName) {
		return this.annotationData.getDocumentProperty(propertyName);
	}

	public String[] getDocumentPropertyNames() {
		return this.annotationData.getDocumentPropertyNames();
	}

	public int getStartIndex() {
		return this.annotationData.getStartIndex();
	}
	
	public int getEndIndex() {
		return this.annotationData.getEndIndex();
	}
	
	public int size() {
		return this.annotationData.size();
	}
	
	public String getType() {
		return this.annotationData.getType();
	}
	
	public String getAnnotationID() {
		return this.annotationData.getAnnotationID();
	}
	
	public String getValue() {
		return this.annotationData.getValue();
	}
	
	public String toString() {
		return this.annotationData.toString();
	}
	
	public int compareTo(Object o) {
		return this.annotationData.compareTo(o);
	}
	
	public Token firstToken() {
		return this.wrapToken(this.annotationData.firstToken());
	}
	
	public String firstValue() {
		return this.annotationData.firstValue();
	}
	
	public String getLeadingWhitespace() {
		return this.annotationData.getLeadingWhitespace();
	}
	
	public TokenSequence getSubsequence(int start, int size) {
		return this.annotationData.getSubsequence(start, size);
	}
	
	public Tokenizer getTokenizer() {
		return this.annotationData.getTokenizer();
	}
	
	public Token lastToken() {
		return this.wrapToken(this.annotationData.lastToken());
	}
	
	public String lastValue() {
		return this.annotationData.lastValue();
	}
	
	public int getStartOffset() {
		return this.annotationData.getStartOffset();
	}

	public int length() {
		return this.annotationData.length();
	}
	
	public int getEndOffset() {
		return this.annotationData.getEndOffset();
	}

	public char charAt(int index) {
		return this.annotationData.charAt(index);
	}

	public CharSequence subSequence(int start, int end) {
		return this.annotationData.subSequence(start, end);
	}

	public Token tokenAt(int index) {
		return this.wrapToken(this.annotationData.tokenAt(index));
	}
	
	public String valueAt(int index) {
		return this.annotationData.valueAt(index);
	}
	
	public String getWhitespaceAfter(int index) {
		return this.annotationData.getWhitespaceAfter(index);
	}

	public String toXML() {
		return this.annotationData.toXML();
	}
	
	public String changeTypeTo(String newType) {
		return this.annotationData.changeTypeTo(newType);
	}
	
	/** wrap a Token before returning it in order to provide additional functionality through the wrapper class
	 * Note: This default implementation simply returns the argument Token, sub classes are welcome to overwrite this method as needed.
	 * @param	token	the Token to wrap
	 * @return the wrapped Token
	 */
	protected Token wrapToken(Token token) {
		return token;
	}
	
	/**
	 * Generic wrapper implementation for the Token interface, with the same purpose as the surrounding class
	 * 
	 * @author sautter
	 */
	public static class GenericTokenWrapper implements Token {
		
		/**	the wrapped Token */
		protected Token tokenData;
		
		/** Constructor
		 * @param	token	the Token to wrap
		 */
		public GenericTokenWrapper(Token token) {
			this.tokenData = token;
		}
		
		public boolean equals(Object obj) {
			return this.getValue().equals(obj);
		}
		
		public int hashCode() {
			return this.getValue().hashCode();
		}
		
		public String toString() {
			return this.getValue();
		}

		public void clearAttributes() {
			this.tokenData.clearAttributes();
		}
		
		public Object removeAttribute(String name) {
			return this.tokenData.removeAttribute(name);
		}
		
		public Object setAttribute(String name, Object value) {
			return this.tokenData.setAttribute(name, value);
		}

		public void copyAttributes(Attributed source) {
			this.tokenData.copyAttributes(source);
		}

		public boolean hasAttribute(String name) {
			return this.tokenData.hasAttribute(name);
		}

		public Object getAttribute(String name, Object def) {
			return this.tokenData.getAttribute(name, def);
		}

		public Object getAttribute(String name) {
			return this.tokenData.getAttribute(name);
		}

		public String[] getAttributeNames() {
			return this.tokenData.getAttributeNames();
		}

		public Tokenizer getTokenizer() {
			return this.tokenData.getTokenizer();
		}

		public String getValue() {
			return this.tokenData.getValue();
		}

		public int getStartOffset() {
			return this.tokenData.getStartOffset();
		}

		public int getEndOffset() {
			return this.tokenData.getEndOffset();
		}

		public int length() {
			return this.tokenData.length();
		}

		public char charAt(int index) {
			return this.tokenData.charAt(index);
		}

		public CharSequence subSequence(int start, int end) {
			return this.tokenData.subSequence(start, end);
		}
	}
}
