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


import java.util.Vector;

import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;

/**
 * This class implements a char sequence and a token sequence in combination.
 * While this is not a clean as a token sequence overlay to a char sequence, it
 * is in great favor of higher performance with larger amounts of text.
 * 
 * @author sautter
 */
public class TokenizedCharSequence implements TokenSequence {

	Vector tokens = new Vector();
	int length = 0;
	
	StringBuffer leadingWhitespace = new StringBuffer();
	Tokenizer tokenizer;
	
	/**	Constructor
	 * @param	tokenizer	the Tokenizer the content of this StringVector was Tokenized with (will be used for, e.g., indexOf() methods)
	 * @param	charData	the character data
	 */
	public TokenizedCharSequence(Tokenizer tokenizer, CharSequence charData) {
		this.tokenizer = tokenizer;
		
		//	check for whitespace
		boolean whitespaceOnly = true;
		for (int c = 0; c < charData.length(); c++)
			if (charData.charAt(c) > 32) {
				whitespaceOnly = false;
				c = charData.length();
			}
		
		//	no non-whitespace data
		if (whitespaceOnly) {
			this.leadingWhitespace.append(charData);
			this.length = this.leadingWhitespace.length();
		}
		
		//	generate tokens
		else {
			Tokenizer.TokenIterator ti = this.tokenizer.getTokenIterator(charData);
			GamtaToken lastToken = null;
			while (ti.hasMoreTokens()) {
				Tokenizer.CharSequenceToken cst = ti.getNextToken();
				
				//	check for whitespace
				if (this.length < cst.startOffset) {
					if (lastToken == null)
						this.leadingWhitespace.append(charData.subSequence(this.length, cst.startOffset));
					else lastToken.whitespace.append(charData.subSequence(this.length, cst.startOffset));
					this.length = cst.startOffset;
				}
				
				//	produce token
				GamtaToken token = new GamtaToken(charData.subSequence(cst.startOffset, cst.endOffset));
				
				//	adjust offsets
				token.startOffset = cst.startOffset;
				this.length = cst.endOffset;
				
				//	store & remember token
				this.tokens.add(token);
				lastToken = token;
			}
			
			//	add last whitespace
			if (this.length < charData.length()) {
				if (lastToken == null)
					this.leadingWhitespace.append(charData.subSequence(this.length, charData.length()));
				else lastToken.whitespace.append(charData.subSequence(this.length, charData.length()));
				this.length = charData.length();
			}
		}
//		
//		//	generate token overlay
//		Tokenizer.TokenIterator ti = this.tokenizer.getTokenIterator(charData);
//		int lastEnd = -1;
//		while (ti.hasMoreTokens()) {
//			Tokenizer.CharSequenceToken cst = ti.getNextToken();
//			if (lastEnd == -1) this.leadingWhitespace.append(charData.subSequence(0, cst.startOffset).toString());
//			else this.getLastToken().whitespace.append(charData.subSequence(lastEnd, cst.startOffset).toString());
//			GamtaToken gt = new GamtaToken(charData.subSequence(cst.startOffset, cst.endOffset));
//			gt.startOffset = cst.startOffset;
//			this.tokens.addElement(gt);
//			lastEnd = cst.endOffset;
//		}
//		
//		//	add last whitespace
//		if (lastEnd < charData.length()) {
//			if (lastEnd == -1) this.leadingWhitespace.append(charData.toString());
//			else this.getLastToken().whitespace.append(charData.subSequence(lastEnd, charData.length()).toString());
//		}
	}
	
	/** package visible constructor used by TokenizedMutableCharSequence
	 * @param tokenizer
	 */
	TokenizedCharSequence(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#tokenAt(int)
	 */
	public Token tokenAt(int index) {
		return this.tcsTokenAt(index);
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#firstToken()
	 */
	public Token firstToken() {
		return this.tcsFirstToken();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#lastToken()
	 */
	public Token lastToken() {
		return this.tcsLastToken();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#valueAt(int)
	 */
	public String valueAt(int index) {
		return this.tcsTokenAt(index).getValue();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#firstValue()
	 */
	public String firstValue() {
		return this.tcsFirstToken().getValue();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#lastValue()
	 */
	public String lastValue() {
		return this.tcsLastToken().getValue();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getLeadingWhitespace()
	 */
	public String getLeadingWhitespace() {
		return this.leadingWhitespace.toString();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getWhitespaceAfter(int)
	 */
	public String getWhitespaceAfter(int index) {
		return this.tcsTokenAt(index).whitespace.toString();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#size()
	 */
	public int size() {
		return this.tokens.size();
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
		TokenizedCharSequence tcs = new TokenizedCharSequence(this.tokenizer, "");
		
		//	copy tokens and compute length
		for (int t = start; t < (start + size); t++) {
			GamtaToken gt = this.tcsTokenAt(t);
			GamtaToken cgt = new GamtaToken(gt.value, (((t+1) == (start + size)) ? ((CharSequence) "") : ((CharSequence) gt.whitespace)));
			cgt.startOffset = tcs.length;
			tcs.tokens.addElement(cgt);
			tcs.length += cgt.gtLength();
		}
		
		//	return copy
		return tcs;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return this.length;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		if (index < this.leadingWhitespace.length())
			return this.leadingWhitespace.charAt(index);
		
		int to = this.tcsIndexAtOffset(index);
		if (to >= this.size())
			return ' ';
		GamtaToken gt = this.tcsTokenAt(to);
		return gt.gtCharAt(index - gt.startOffset);
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		if (this.tokens.isEmpty())
			return this.leadingWhitespace.subSequence(start, end);
		
		StringBuffer subSequence = new StringBuffer();
		
		int to = this.tcsIndexAtOffset(start);
		if (to == -1) {
			subSequence.append(this.leadingWhitespace.subSequence(start, Math.min(end, this.leadingWhitespace.length())).toString());
			to = 0;
		}
		if (to == this.tokens.size()) return "";
		
		GamtaToken gt = this.tcsTokenAt(to);
		while ((subSequence.length() < (end - start)) && (gt != null)) {
			subSequence.append(gt.gtSubSequence(Math.max(0, (start - gt.startOffset)), Math.min(gt.gtLength(), (end - gt.startOffset))).toString());
			to++;
			gt = ((to == this.tokens.size()) ? null : this.tcsTokenAt(to));
		}
		return subSequence.toString();
	}
	
	GamtaToken tcsTokenAt(int index) {
		return ((GamtaToken) this.tokens.get(index));
	}
	
	GamtaToken tcsFirstToken() {
		return ((GamtaToken) this.tokens.firstElement());
	}
	
	GamtaToken tcsLastToken() {
		return ((GamtaToken) this.tokens.lastElement());
	}
	
	//	cache fields for character wise iteration
	private int lastOffsetTokenIndex = -1;
	
	/*
	 * find the index of the token holding the char at some given offset
	 */
	static final boolean DEBUG_OFFSET_INDEX_CACHE = false;
	synchronized int tcsIndexAtOffset(int offset) {
		if (DEBUG_OFFSET_INDEX_CACHE) System.out.println("TokenizedCharSequence(" + this.hashCode() + "): getting token index at offset " + offset);
		
		//	check for empty token sequence
		if (this.tokens.isEmpty()) {
			if (DEBUG_OFFSET_INDEX_CACHE) System.out.println(" - before start of first token");
			this.lastOffsetTokenIndex = -1;
			return -1;
		}
		
		//	check for leading and tailing tokens
		if (offset < this.tcsFirstToken().startOffset) {
			if (DEBUG_OFFSET_INDEX_CACHE) System.out.println(" - before start of first token");
			this.lastOffsetTokenIndex = -1;
			return -1;
		}
		if (offset >= this.tcsLastToken().gtEndOffset()) {
			if (DEBUG_OFFSET_INDEX_CACHE) System.out.println(" - after end of last token");
			this.lastOffsetTokenIndex = -1;
			return this.size();
		}
		
		//	do cache lookup if cached index in range
		if ((this.lastOffsetTokenIndex != -1) && (this.lastOffsetTokenIndex < this.tokens.size())) {
			if (DEBUG_OFFSET_INDEX_CACHE) System.out.println(" - using cache");
			GamtaToken gt = this.tcsTokenAt(this.lastOffsetTokenIndex);
			
			//	request for same token
			if ((gt.startOffset <= offset) && (offset < gt.gtEndOffset())) {
				if (DEBUG_OFFSET_INDEX_CACHE) System.out.println("   - cache hit");
				return this.lastOffsetTokenIndex;
			}
			
			//	request for subsequent token
			else if (offset == gt.gtEndOffset()) {
				this.lastOffsetTokenIndex++;
				if (this.lastOffsetTokenIndex == this.tokens.size()) {
					if (DEBUG_OFFSET_INDEX_CACHE) System.out.println("   - cache at end offset miss");
					this.lastOffsetTokenIndex = -1;
					return this.tokens.size();
				}
				else {
					if (DEBUG_OFFSET_INDEX_CACHE) System.out.println("   - cache increment hit");
					return this.lastOffsetTokenIndex;
				}
			}
			
			//	request for previous token
			else if (gt.startOffset == (offset + 1)) {
				if (DEBUG_OFFSET_INDEX_CACHE) System.out.println("   - cache decrement hit");
				this.lastOffsetTokenIndex--;
				return this.lastOffsetTokenIndex;
			}
			else if (DEBUG_OFFSET_INDEX_CACHE) System.out.println("   - cache miss");
		}
		
		//	use binary search to narrow search interval
		int left = 0;
		int right = this.tokens.size();
		int tIndex = 0;
		GamtaToken gt;
		while ((right - left) > 2) {
			tIndex = ((left + right) / 2);
			gt = this.tcsTokenAt(tIndex);
			if (DEBUG_OFFSET_INDEX_CACHE) System.out.println("   - tIndex is " + tIndex + ", startOffset is " + gt.startOffset + ", endOffset is " + gt.gtEndOffset());
			if (gt.gtEndOffset() <= offset) left = tIndex;
			else if (gt.startOffset <= offset) {
				this.lastOffsetTokenIndex = tIndex;
				return tIndex;
			}
			else right = tIndex;
		}
		if (DEBUG_OFFSET_INDEX_CACHE) System.out.println(" - tIndex is at least " + left + ", start offset there is " + this.tcsTokenAt(left).startOffset);
		
		//	scan remaining interval
		tIndex = left;
		while (tIndex < this.tokens.size()) {
			gt = this.tcsTokenAt(tIndex);
			if (DEBUG_OFFSET_INDEX_CACHE) System.out.println("   - tIndex is " + tIndex + ", startOffset is " + gt.startOffset + ", endOffset is " + gt.gtEndOffset());
			if (gt.gtEndOffset() <= offset) tIndex++;
			else if (gt.startOffset <= offset) {
				if (DEBUG_OFFSET_INDEX_CACHE) System.out.println(" - found offset at " + tIndex);
				this.lastOffsetTokenIndex = tIndex;
				return tIndex;
			}
			else tIndex++;
		}
		
		if (DEBUG_OFFSET_INDEX_CACHE) System.out.println(" - end offset exceeded");
		this.lastOffsetTokenIndex = -1;
		return this.tokens.size();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.subSequence(0, this.length()).toString();
	}
	
	/*
	 * internal token representation
	 */
	class GamtaToken extends AbstractAttributed implements Token {
		
		int startOffset = 0;
		
		StringBuffer value = new StringBuffer();
		StringBuffer whitespace = new StringBuffer();
		
		GamtaToken(CharSequence value) {
			this.value.append(value.toString());
		}
		
		GamtaToken(CharSequence value, CharSequence whitespace) {
			this.value.append(value.toString());
			this.whitespace.append(whitespace.toString());
		}
		
		char gtCharAt(int index) {
			if (index < this.value.length())
				return this.value.charAt(index);
			else return this.whitespace.charAt(index - this.value.length());
		}
		
		CharSequence gtSubSequence (int start, int end) {
			StringBuffer subSequence = new StringBuffer();
			if (start < this.value.length())
				subSequence.append(this.value.subSequence(start, Math.min(end, this.value.length())).toString());
			if (this.value.length() < end)
				subSequence.append(this.whitespace.subSequence((Math.max(0, (start - this.value.length()))), (end - this.value.length())).toString());
			return subSequence.toString();
		}
		
		int gtEndOffset() {
			return (this.startOffset + this.gtLength());
		}
		
		int gtLength() {
			return (this.value.length() + this.whitespace.length());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String)
		 */
		public Object getAttribute(String name) {
			return this.getAttribute(name, null);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String, java.lang.Object)
		 */
		public Object getAttribute(String name, Object def) {
			if (TOKEN_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
			else return super.getAttribute(name, def);
		}
		
		/** @see java.lang.Object#toString()
		 */
		public String toString() {
			return this.getValue();
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

		/** @see java.lang.CharSequence#charAt(int)
		 */
		public char charAt(int index) {
			return this.value.charAt(index);
		}

		/** @see java.lang.CharSequence#length()
		 */
		public int length() {
			return this.value.length();
		}

		/** @see java.lang.CharSequence#subSequence(int, int)
		 */
		public CharSequence subSequence(int start, int end) {
			return this.value.subSequence(start, end);
		}

		/** @see de.gamta.Token#getTokenizer()
		 */
		public Tokenizer getTokenizer() {
			return TokenizedCharSequence.this.getTokenizer();
		}

		/** @see de.gamta.CharSpan#getStartOffset()
		 */
		public int getStartOffset() {
			return this.startOffset;
		}

		/** @see de.gamta.CharSpan#getEndOffset()
		 */
		public int getEndOffset() {
			return (this.startOffset + this.value.length());
		}

		/** @return the Token's value
		 */
		public String getValue() {
			return this.value.toString();
		}
	}
}
