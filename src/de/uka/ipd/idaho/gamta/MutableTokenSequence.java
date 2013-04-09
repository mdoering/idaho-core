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
package de.uka.ipd.idaho.gamta;



/**
 * a mutable sequence of tokens, offering some higher level modification functions to a MutableCharSequence, in particular "smart" modification functions that maintain existing Tokens in the face of modifications.
 * 
 * @author sautter
 */
public interface MutableTokenSequence extends TokenSequence, MutableCharSequence {
	
	/**
	 * an object encapsulating the essential information on changes to a MutableTokenSequence
	 * 
	 * @author sautter
	 */
	public static class TokenSequenceEvent {
		
		/** the MutableTokenSequence that was changed
		 */
		public final MutableTokenSequence tokenSequence;
		
		/** the start index of the change
		 */
		public final int index;
		
		/**	the sequence of chars that was inserted (will be empty on pure removals, never null)
		 */
		public final TokenSequence inserted;
		
		/**	the sequence of chars that was removed (will be empty on pure insertions, never null)
		 */
		public final TokenSequence removed;

		/** the change to the underlaying char sequence that caused the change to the token overlay
		 */
		public final CharSequenceEvent cause;
		
		/** Constructor
		 * @param	tokenSequence	the MutableCharSequence that was changed
		 * @param	index			the start index of the change
		 * @param	inserted		the sequence of chars that was inserted (will be empty on pure removals)
		 * @param	removed			the sequence of chars that was removed (will be empty on pure insertions)
		 * @param	cause			the change to the underlaying char sequence that caused the change to the token overlay
		 */
		public TokenSequenceEvent(MutableTokenSequence tokenSequence, int index, TokenSequence inserted, TokenSequence removed, CharSequenceEvent cause) {
			this.tokenSequence = tokenSequence;
			this.index = index;
			this.inserted = ((inserted == null) ? new EmptyTokenSequence(tokenSequence.getTokenizer()) : inserted);
			this.removed = ((removed == null) ? new EmptyTokenSequence(tokenSequence.getTokenizer()) : removed);
			this.cause = cause;
		}
		
		/** helper class for representing a generic empty token sequence in events
		 */
		private class EmptyTokenSequence implements TokenSequence {
			private Tokenizer tokenizer;
			public EmptyTokenSequence(Tokenizer tokenizer) {
				this.tokenizer = tokenizer;
			}
			public Token tokenAt(int index) {
				throw new ArrayIndexOutOfBoundsException("" + index);
			}
			public Token firstToken() {
				throw new ArrayIndexOutOfBoundsException("" + 0);
			}
			public Token lastToken() {
				throw new ArrayIndexOutOfBoundsException("" + 0);
			}
			public String valueAt(int index) {
				throw new ArrayIndexOutOfBoundsException("" + 0);
			}
			public String firstValue() {
				return this.valueAt(0);
			}
			public String lastValue() {
				throw new ArrayIndexOutOfBoundsException("" + 0);
			}
			public String getLeadingWhitespace() {
				return "";
			}
			public String getWhitespaceAfter(int index) {
				return "";
			}
			public int size() {
				return 0;
			}
			public Tokenizer getTokenizer() {
				return this.tokenizer;
			}
			public TokenSequence getSubsequence(int start, int size) {
				return new EmptyTokenSequence(this.getTokenizer());
			}
			public int length() {
				return 0;
			}
			public String toString() {
				return "";
			}
			public char charAt(int offset) {
				throw new ArrayIndexOutOfBoundsException("" + offset);
			}
			public java.lang.CharSequence subSequence(int start, int end) {
				throw new ArrayIndexOutOfBoundsException("" + start);
			}
		}
	}
	
	/**	change the leading whitespace of a MutableTokenSequence
	 * @param	whitespace	the new leading whitespace of this MutableTokenSequence
	 * @return the old leading whitespace of this MutableTokenSequence
	 * @throws IllegalArgumentException if the specified whitespace char sequence contains non-whitespace characters
	 */
	public abstract java.lang.CharSequence setLeadingWhitespace(java.lang.CharSequence whitespace) throws IllegalArgumentException; 
	
	/**	change a Token's value. If the specified value String tokenizes to no or more than one Token under this TokenSequence's Tokenizer, implementations may either throw an IllegalArgumentException, or provide some "smart insert" behavior using the first Token as the new value at the specified index and inserting the remaining tokens after the specified index, or deleting the Token at the specified index, respectively. 
	 * @param	value	the new value for the Token
	 * @param	index	the index of the Token that's value is to be set 
	 * @return the old value of the Token
	 * @throws IllegalArgumentException if the specified value String tokenizes to no or more than one Token
	 */
	public abstract java.lang.CharSequence setValueAt(java.lang.CharSequence value, int index) throws IllegalArgumentException;
	
	/**	change the whitespace after a Token's value
	 * @param	whitespace	the new whitespace after the Token
	 * @param	index		the index of the Token the whitespace after which is to be set 
	 * @return the old whitespace after the Token
	 * @throws IllegalArgumentException if the specified whitespace char sequence contains non-whitespace characters
	 */
	public abstract java.lang.CharSequence setWhitespaceAfter(java.lang.CharSequence whitespace, int index) throws IllegalArgumentException;
	
	/**	remove a sequence of Tokens from this TokenSequence. Implementations of this method should not leave duplicate whitespace characters as a result of removing the Tokens in between. If there are whitespace chars to be removed, simple space chars (ASCII 32) should be removed prior to other whitespace chars. If the removal hampers the integrity of surrounding tokens (e.g. no whitespace remains between two token, which would be conflated as a result), implementations should insert a single space character for padding.  
	 * @param	index	the index of the first Token to be removed
	 * @param	size	the number of Tokens to be removed
	 * @return a TokenSequence containing the Tokens that were removed from the TokenSequence
	 */
	public abstract TokenSequence removeTokensAt(int index, int size);
	
	/**	tokenize a char sequence using this TokenSequence's Tokenizer and insert the resulting Tokens at the specified position. As opposed to the insertCharsAt() method, implementations of this method should not hamper surrounding tokens. For this purpose, they might extend the inserted char sequence with whitespace characters at the beginning and end in order to prevent inserted tokens from sticking to ones existing prior to the invokation of this method.
	 * @param	tokens	the char sequence to be inserted
	 * @param	index	the index to insert the Token at
	 * @return the sequence of cars that was actually inserted, i.e. the specified one plus potential padding chars
	 */
	public abstract java.lang.CharSequence insertTokensAt(java.lang.CharSequence tokens, int index);
	
	/**	tokenize a char sequence using this TokenSequence's Tokenizer and append the resulting Tokens at the end of this TokenSequence. As opposed to the appendChars() method, implementations of this method should not hamper surrounding tokens. For this purpose, they might extend the appended char sequence with whitespace characters at the beginning in order to prevent inserted tokens from sticking to ones existing prior to the invokation of this method.
	 * @param	tokens	the char sequence to be added
	 * @return the sequence of cars that was actually appended, i.e. the specified one plus potential padding chars
	 */
	public abstract java.lang.CharSequence addTokens(java.lang.CharSequence tokens);
	
	/**	clear the TokenSequence (i.e. remove all Tokens)
	 */
	public abstract void clear();
	
	/**	obtain a mutable copy of a part of this mutable token sequence
	 * @param	start	the index of the Token to start at
	 * @param	size	the number of Tokens
	 * @return a subsequence of this MutableTokenSequence, starting with the Token at the specified index and containing the specified number of Tokens
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#getSubsequence(int, int)
	 */
	public abstract MutableTokenSequence getMutableSubsequence(int start, int size);
	
	/**	add a listener listening for changes to the TokenSequence
	 * @param	tsl		the listener to be added
	 */
	public abstract void addTokenSequenceListener(TokenSequenceListener tsl);
	
	/**	remove a listener from the TokenSequence
	 * @param	tsl		the listener to be removed
	 */
	public abstract void removeTokenSequenceListener(TokenSequenceListener tsl);
}
