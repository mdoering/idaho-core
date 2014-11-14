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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;

/**
 * Implementation of a basic (immutable) gamta token sequence.
 * 
 * @author sautter
 */
public class PlainTokenSequence implements TokenSequence {
	
	private CharSequence charData;
	private Tokenizer tokenizer;
	private ArrayList tokenOverlay = new ArrayList();
	
	/** Constructor
	 * @param	charData	the gamta char sequence this token sequence is an overlay for
	 * @param	tokenizer	the Tokenizer used to decompose the char sequence into tokens
	 */
	public PlainTokenSequence(CharSequence charData, Tokenizer tokenizer) {
		this.charData = charData;
		this.tokenizer = tokenizer;
		
		//	generate token overlay
		Tokenizer.TokenIterator ti = this.tokenizer.getTokenIterator(this.charData);
		while (ti.hasMoreTokens()) {
			Tokenizer.CharSequenceToken cst = ti.getNextToken();
			this.tokenOverlay.add(new GamtaToken(cst.startOffset, cst.endOffset));
		}
	}
	
	private class GamtaToken extends AbstractAttributed implements Token {
		private int startOffset;
		private int endOffset;
		//	private String valueCache = null;	TODO: cache value
		
		/**
		 * @param startOffset
		 * @param endOffset
		 */
		protected GamtaToken(int startOffset, int endOffset) {
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}
		
		/** @see de.gamta.Token#getTokenizer()
		 */
		public Tokenizer getTokenizer() {
			return tokenizer;
		}
		
		/** @see de.gamta.Token#getValue()
		 */
		public String getValue() {
			return charData.subSequence(this.startOffset, this.endOffset).toString();
		}
		
		/** @see java.lang.CharSequence#charAt(int)
		 */
		public char charAt(int index) {
			if ((index > -1) && (index < this.length())) return charData.charAt(this.startOffset + index);
			throw new ArrayIndexOutOfBoundsException("" + index);
		}
		
		/** @see java.lang.CharSequence#length()
		 */
		public int length() {
			return (this.endOffset - this.startOffset);
		}
		
		/** @see java.lang.CharSequence#subSequence(int, int)
		 */
		public CharSequence subSequence(int start, int end) {
			return this.getValue().subSequence(start, end);
		}
		
		/** @see de.gamta.CharSpan#getEndOffset()
		 */
		public int getEndOffset() {
			return this.endOffset;
		}
		
		/** @see de.gamta.CharSpan#getStartOffset()
		 */
		public int getStartOffset() {
			return this.startOffset;
		}
		
		/** @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			return this.getValue().equals(obj);
		}
		
		/** @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.getValue().hashCode();
		}

		/** @see java.lang.Object#toString()
		 */
		public String toString() {
			return this.getValue();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#tokenAt(int)
	 */
	public Token tokenAt(int index) {
		return ((Token) this.tokenOverlay.get(index));
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#firstToken()
	 */
	public Token firstToken() {
		return this.tokenAt(0);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#lastToken()
	 */
	public Token lastToken() {
		return this.tokenAt(this.size() - 1);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#valueAt(int)
	 */
	public String valueAt(int index) {
		return ((Token) this.tokenOverlay.get(index)).getValue();
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#firstValue()
	 */
	public String firstValue() {
		return this.valueAt(0);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#lastValue()
	 */
	public String lastValue() {
		return this.valueAt(this.size() - 1);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getLeadingWhitespace()
	 */
	public String getLeadingWhitespace() {
		if (this.size() == 0) return this.charData.toString();
		return this.charData.subSequence(0, this.firstToken().getStartOffset()).toString();
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getWhitespaceAfter(int)
	 */
	public String getWhitespaceAfter(int index) {
		if (this.size() == 0) return this.charData.toString();
		else if (index == (this.size() - 1)) this.charData.subSequence(this.lastToken().getEndOffset(), this.charData.length()).toString();
		return this.charData.subSequence(this.tokenAt(index).getEndOffset(), this.tokenAt(index + 1).getStartOffset()).toString();
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#size()
	 */
	public int size() {
		return this.tokenOverlay.size();
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getTokenizer()
	 */
	public Tokenizer getTokenizer() {
		return this.tokenizer;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getSubsequence(int, int)
	 */
	public TokenSequence getSubsequence(int start, int size) {
		
		//	empty ourselves
		if ((this.size() == 0) && (start == 0) && (size == 0))
			return new PlainTokenSequence(this.charData.subSequence(0, this.charData.length()), this.getTokenizer());
		
		//	empty suffix
		if ((start == this.size()) && (size <= 0)) 
			return new PlainTokenSequence(this.charData.subSequence(this.lastToken().getEndOffset(), this.charData.length()), this.getTokenizer());
		
		//	empty token sequence
		if (size == 0) return new PlainTokenSequence("", this.getTokenizer());
		
		//	suffix with content
		return new PlainTokenSequence(this.charData.subSequence(this.tokenAt(start).getStartOffset(), this.tokenAt(start + size - 1).getEndOffset()), this.getTokenizer());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return this.charData.length();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.charData.toString();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		return this.charData.charAt(index);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return this.charData.subSequence(start, end);
	}
}
