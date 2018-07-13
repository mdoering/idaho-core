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
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;

/**
 * This wrapper class makes a mutable token sequence a basic (immutable) token
 * sequence. It cannot guaranty, though, that objects retain references to the
 * underlying (possibly mutable) token sequence and apply modifications to it.
 * Such modifications will inevitably be visible through this immutable view.
 * This wrapper ensures, though, that subordinary token sequences retrieved from
 * the getSubsequence() method are immutable, even if the underlying token
 * sequence is mutable and returns a mutable sub sequence.
 * 
 * @author sautter
 */
public class ImmutableTokenSequence implements TokenSequence {
	
	private TokenSequence tokens;
	
	/** Constructor
	 * @param	tokens	the token sequence to wrap
	 */
	public ImmutableTokenSequence(TokenSequence tokens) {
		this.tokens = tokens;
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#tokenAt(int)
	 */
	public Token tokenAt(int index) {
		return new ImmutableToken(this.tokens.tokenAt(index));
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#firstToken()
	 */
	public Token firstToken() {
		return new ImmutableToken(this.tokens.firstToken());
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#lastToken()
	 */
	public Token lastToken() {
		return new ImmutableToken(this.tokens.lastToken());
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#valueAt(int)
	 */
	public String valueAt(int index) {
		return this.tokens.valueAt(index);
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#firstValue()
	 */
	public String firstValue() {
		return this.tokens.firstValue();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#lastValue()
	 */
	public String lastValue() {
		return this.tokens.lastValue();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getLeadingWhitespace()
	 */
	public String getLeadingWhitespace() {
		return this.tokens.getLeadingWhitespace();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getWhitespaceAfter(int)
	 */
	public String getWhitespaceAfter(int index) {
		return this.tokens.getWhitespaceAfter(index);
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
		return this.tokens.getTokenizer();
	}

	/* (non-Javadoc)
	 * @see de.gamta.TokenSequence#getSubsequence(int, int)
	 */
	public TokenSequence getSubsequence(int start, int size) {
		TokenSequence ts = this.tokens.getSubsequence(start, size);
		return new ImmutableTokenSequence(ts);
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return this.tokens.length();
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		return this.tokens.charAt(index);
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return this.tokens.subSequence(start, end);
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
		 * @see de.uka.ipd.idaho.gamta.Attributed#setAttribute(java.lang.String)
		 */
		public void setAttribute(String name) {
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
