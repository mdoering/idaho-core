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

import de.uka.ipd.idaho.gamta.CharSequenceListener;
import de.uka.ipd.idaho.gamta.MutableCharSequence;
import de.uka.ipd.idaho.gamta.MutableTokenSequence;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * Default implementation of the MutableTokenSequence overlay over an arbitrary
 * MutableCharSequence implementation. This implementation is not thread-safe.
 * 
 * @author sautter
 */
public class GamtaTokenSequence extends AbstractMutableTokenSequence {
	
	private static final boolean DEBUG = false;
	
	private ArrayList tokenOverlay = new ArrayList();
	private TokenAdjuster adjuster = new TokenAdjuster();
	
	/** Constructor
	 * @param	charData	the char sequence this token sequence is an overlay for
	 * @param	tokenizer	the Tokenizer used to decompose the char sequence into tokens
	 */
	public GamtaTokenSequence(MutableCharSequence charData, Tokenizer tokenizer) {
		super(charData, tokenizer);
		
		//	generate token overlay
		Tokenizer.TokenIterator ti = this.tokenizer.getTokenIterator(this.charData);
		while (ti.hasMoreTokens()) {
			Tokenizer.CharSequenceToken cst = ti.getNextToken();
			this.tokenOverlay.add(new GamtaToken(cst.startOffset, cst.endOffset));
		}
		
		//	listen to changes
		this.charData.addCharSequenceListener(this.adjuster);
	}
	
	private class GamtaToken extends AbstractAttributed implements Token {
		private int startOffset;
		private int endOffset;
		
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

		/** @see java.lang.Object#toString()
		 */
		public String toString() {
			return this.getValue();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		super.finalize();
		this.charData.removeCharSequenceListener(this.adjuster);
	}
	
	/**
	 * Char sequence listener for adopting the token overlay to changes in the
	 * underlaying char sequence.
	 * 
	 * We use a private class to prevent making a GamtaTokenSequence listen to a
	 * char sequence other than the underlaying one.
	 * 
	 * @author sautter
	 */
	private class TokenAdjuster implements CharSequenceListener {
		
		/* (non-Javadoc)
		 * @see de.gamta.CharSequenceListener#charSequenceChanged(de.gamta.MutableCharSequence.CharSequenceEvent)
		 */
		public void charSequenceChanged(CharSequenceEvent change) {
			
			if (DEBUG) System.out.println("TokenSequence: Underlaying char sequence changed at " + change.offset + ": '" + change.removed + "' changed to '" + change.inserted + "'");
			
			//	no tokens to adjust, handle insertion if any
			if (tokenOverlay.isEmpty()) {
				
				//	insert new tokens
				TokenSequence its = new PlainTokenSequence(change.inserted, getTokenizer());
				for (int t = 0; t < its.size(); t++) {
					Token tok = its.tokenAt(t);
					GamtaToken gt = new GamtaToken((change.offset + tok.getStartOffset()), (change.offset + tok.getEndOffset()));
					tokenOverlay.add(gt);
				}
				
				//	notify listeners & return (we're done)
				notifyTokenSequenceChanged(0, its, new PlainTokenSequence(change.removed, getTokenizer()), change);
				return;
			}
			
			
			//	modification after last token, nothing to adjust, handle insertion
			if (change.offset > lastToken().getEndOffset()) {
				
				//	insert new tokens
				int size = size();
				TokenSequence its = new PlainTokenSequence(change.inserted, getTokenizer());
				for (int t = 0; t < its.size(); t++) {
					Token tok = its.tokenAt(t);
					GamtaToken gt = new GamtaToken((change.offset + tok.getStartOffset()), (change.offset + tok.getEndOffset()));
					tokenOverlay.add(gt);
				}
				
				//	notify listeners & return (we're done)
				notifyTokenSequenceChanged(size, its, new PlainTokenSequence(change.removed, getTokenizer()), change);
				return;
			}
			
			
			//	cache lengths
			int insertLength = change.inserted.length();
			int removeLength = change.removed.length();
			int lengthDelta = (insertLength - removeLength);
			
			
			//	modification before first token, adjust existing tokens and insert new ones
			if ((change.offset + removeLength) < firstToken().getStartOffset()) {
				
				//	adjust existing tokens
				for (int t = 0; t < tokenOverlay.size(); t++) {
					GamtaToken gt = ((GamtaToken) tokenOverlay.get(t));
					gt.startOffset += lengthDelta;
					gt.endOffset += lengthDelta;
				}
				
				//	insert new tokens
				TokenSequence its = new PlainTokenSequence(change.inserted, getTokenizer());
				for (int t = 0; t < its.size(); t++) {
					Token tok = its.tokenAt(t);
					GamtaToken gt = new GamtaToken((change.offset + tok.getStartOffset()), (change.offset + tok.getEndOffset()));
					tokenOverlay.add(t, gt);
				}
				
				//	notify listeners & return (we're done)
				notifyTokenSequenceChanged(0, its, new PlainTokenSequence(change.removed, getTokenizer()), change);
				return;
			}
			
			
			//	find next whitespace before modification
			int lwi = change.offset-1;
			while ((lwi > -1) && (charData.charAt(lwi) > 32)) lwi--;
			String lb = charData.subSequence(lwi+1, change.offset).toString();
			if (DEBUG) System.out.println(" - left whitespace offset: " + lwi);
			
			//	find next whitespace after modification
			int rwi = (change.offset + insertLength);
			while ((rwi < charData.length()) && (charData.charAt(rwi) > 32)) rwi++;
			String rb = charData.subSequence((change.offset + insertLength), rwi).toString();
			if (DEBUG) System.out.println(" - right whitespace offset: " + rwi);
			
			
			//	produce char blocks
			String unModified = lb + change.removed + rb;
			String modified = lb + change.inserted + rb;
			if (DEBUG) System.out.println(" - char blocks modified: '" + unModified + "' changed to '" + modified + "'");
			
			//	tokenize char blocks
			TokenSequence uts = new PlainTokenSequence(unModified, getTokenizer());
			TokenSequence mts = new PlainTokenSequence(modified, getTokenizer());
			
			
			//	search token in which the change occurred (approximate jump, then linear - maybe change the latter later on)
			int tokenIndexAtOffset = ((tokenOverlay.size() * change.offset) / lastToken().getEndOffset());
			
			//	find first affected token
			while ((tokenIndexAtOffset != -1) && ((tokenIndexAtOffset >= size()) || (tokenAt(tokenIndexAtOffset).getStartOffset() > change.offset)))
				tokenIndexAtOffset--;
			while (((tokenIndexAtOffset + 1) < size()) && (tokenAt(tokenIndexAtOffset + 1).getStartOffset() <= change.offset))
				tokenIndexAtOffset++;
			if (DEBUG) System.out.println(" - index at offset: " + tokenIndexAtOffset);
			
			
			//	find first (possibly) affected token
			int firstTokenIndex = tokenIndexAtOffset;
			while ((firstTokenIndex != -1) && (tokenAt(firstTokenIndex).getStartOffset() > lwi+1))
				firstTokenIndex--;
			if (DEBUG) System.out.println(" - first affected token index: " + firstTokenIndex);
			
			//	find last (possibly) affected token
			int lastTokenIndex = (((rwi - lengthDelta) < firstToken().getStartOffset()) ? -1 : Math.max(0, tokenIndexAtOffset));
			while ((lastTokenIndex != -1) && (lastTokenIndex < size()) && (tokenAt(lastTokenIndex).getEndOffset() < (rwi - lengthDelta)))
				lastTokenIndex++;
			if (DEBUG) System.out.println(" - last affected token index: " + lastTokenIndex);
			
			//	adjust tokens after last one affected
			for (int t = (lastTokenIndex + 1); t < tokenOverlay.size(); t++) {
				GamtaToken gt = ((GamtaToken) tokenOverlay.get(t));
				gt.startOffset += lengthDelta;
				gt.endOffset += lengthDelta;
			}
			
			int minSize = Math.min(uts.size(), mts.size());
			
			//	find first token that was actually modified
			int lti = 0;
			int rti = 0;
			while (((lti + rti) < minSize) && (mts.tokenAt(lti).getStartOffset() < (change.offset - lwi - 1)) && uts.valueAt(lti).equals(mts.valueAt(lti))) {
				GamtaToken gt = ((GamtaToken) tokenOverlay.get(firstTokenIndex + lti));
				Token tok = mts.tokenAt(lti);
				gt.startOffset = (lwi + 1 + tok.getStartOffset());
				gt.endOffset = (lwi + 1 + tok.getEndOffset());
				if (DEBUG) System.out.println(" - keeping left unmodified token '" + tok.getValue() + "' at " + (firstTokenIndex + lti));
				lti++;
			}
			
			//	find last token that was actually modified
			while (((lti + rti) < minSize) && uts.valueAt(uts.size() - rti - 1).equals(mts.valueAt(mts.size() - rti - 1))) {
				GamtaToken gt = ((GamtaToken) tokenOverlay.get(lastTokenIndex - rti));
				Token tok = mts.tokenAt(mts.size() - rti - 1);
				gt.startOffset = (lwi + 1 + tok.getStartOffset());
				gt.endOffset = (lwi + 1 + tok.getEndOffset());
				if (DEBUG) System.out.println(" - keeping right unmodified token '" + tok.getValue() + "' at " + (lastTokenIndex - rti));
				rti++;
			}
			
			//	avoid inserting and removing tokens
			int mod = 0;
			for (int t = lti; t < (minSize - rti); t++) {
				GamtaToken gt = ((GamtaToken) tokenOverlay.get(firstTokenIndex + t));
				Token tok = mts.tokenAt(t);
				gt.startOffset = (lwi + 1 + tok.getStartOffset());
				gt.endOffset = (lwi + 1 + tok.getEndOffset());
				if (DEBUG) System.out.println(" - modifying token at " + (firstTokenIndex + t));
				mod++;
			}
			
			//	handle modified tokens
			for (int t = (lti + mod); t < (uts.size() - rti); t++) {
				tokenOverlay.remove(firstTokenIndex + (lti + mod));
				if (DEBUG) System.out.println(" - removing token at " + (firstTokenIndex + (lti + mod)));
			}
			
			for (int t = (lti + mod); t < (mts.size() - rti); t++) {
				Token tok = mts.tokenAt(t);
				GamtaToken gt = new GamtaToken((lwi + 1 + tok.getStartOffset()), (lwi + 1 + tok.getEndOffset()));
				tokenOverlay.add((firstTokenIndex + t), gt);
				if (DEBUG) System.out.println(" - inserting token at " + (firstTokenIndex + t));
			}
			
			if ((mts.size() + uts.size()) > 0)
				notifyTokenSequenceChanged(
						Math.max((firstTokenIndex + lti), 0), 
						mts.getSubsequence(lti, (mts.size() - lti - rti)), 
						uts.getSubsequence(lti, (uts.size() - lti - rti)), 
						change);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#setLeadingWhitespace(java.lang.CharSequence)
	 */
	public CharSequence setLeadingWhitespace(CharSequence whitespace) throws IllegalArgumentException {
		if (whitespace.toString().trim().length() != 0)
			throw new IllegalArgumentException("Whitespace must not contain non-whitespace characters.");
		return this.charData.setChars(whitespace, 0, this.getLeadingWhitespace().length());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#setValueAt(java.lang.CharSequence, int)
	 */
	public CharSequence setValueAt(CharSequence value, int index) throws IllegalArgumentException {
		Token tok = this.tokenAt(index);
		if (this.needsPadding(this.charData, tok.getStartOffset(), value, 0)) value = (" " + value);
		if (this.needsPadding(value, value.length(), this.charData, tok.getEndOffset())) value = (value + " ");
		this.charData.setChars(value, tok.getStartOffset(), tok.length());
		return value;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#setWhitespaceAfter(java.lang.CharSequence, int)
	 */
	public CharSequence setWhitespaceAfter(CharSequence whitespace, int index) throws IllegalArgumentException {
		if (whitespace.toString().trim().length() != 0)
			throw new IllegalArgumentException("Whitespace must not contain non-whitespace characters.");
		return this.charData.setChars(whitespace, this.tokenAt(index).getEndOffset(), this.getWhitespaceAfter(index).length());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#removeTokensAt(int, int)
	 */
	public TokenSequence removeTokensAt(int index, int size) {
		int startOffset = this.tokenAt(index).getStartOffset();
		char bc = ((startOffset == 0) ? StringUtils.NULLCHAR : this.charData.charAt(startOffset - 1));
		int endOffset = (((index + size) == this.size()) ? this.charData.length() : this.tokenAt(index + size - 1).getEndOffset());
		char ac = ((endOffset == this.charData.length()) ? StringUtils.NULLCHAR : this.charData.charAt(endOffset));
		
		if (DEBUG) System.out.println("BC: '" + bc + "' AC: '" + ac + "'");
		boolean insertPad = false;
		if (bc == StringUtils.NULLCHAR) {
			if ((ac != StringUtils.NULLCHAR) && (ac < 33)) endOffset++;
		} else if (ac == StringUtils.NULLCHAR) {
			if ((bc != StringUtils.NULLCHAR) && (bc < 33)) startOffset--;
		} else if (bc == 32) {
			if ((ac < 33) || !this.needsPadding(this.charData, (startOffset - 1), this.charData, endOffset))
				startOffset--;
			if ((ac < 33) && !this.needsPadding(this.charData, startOffset, this.charData, (endOffset + 1)))
				endOffset++;
		} else if (ac == 32) {
			if ((bc < 33) || !this.needsPadding(this.charData, startOffset, this.charData, (endOffset + 1)))
				endOffset++;
			if ((bc < 33) && !this.needsPadding(this.charData, (startOffset - 1), this.charData, endOffset))
				startOffset--;
		} else if ((bc < 33) && (ac < 33)) {
			endOffset++;
		} else if (this.needsPadding(this.charData, startOffset, this.charData, endOffset)) {
			insertPad = true;
		}
		
		//	TODO: decide on char sequence implementation based on argument char sequence length later on
		return new PlainTokenSequence(this.charData.setChars((insertPad ? " " : ""), startOffset, (endOffset - startOffset)), this.getTokenizer());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#insertTokensAt(java.lang.CharSequence, int)
	 */
	public CharSequence insertTokensAt(CharSequence tokens, int index) {
		
		//	check parameter
		if ((tokens == null) || (tokens.length() == 0)) return tokens;
		
		//	insert padding if necessary
		int insertOffset = ((index == this.size()) ? this.charData.length() : this.tokenAt(index).getStartOffset());
		if (this.needsPadding(this.charData, insertOffset, tokens, 0))
			tokens = (" " + tokens);
		if (this.needsPadding(tokens, tokens.length(), this.charData, insertOffset))
			tokens = (tokens + " ");
		
		//	append and return prepared sequence (token overlay will be adjusted in charsInserted() method)
		this.charData.insertChars(tokens, insertOffset);
		return tokens;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#addTokens(java.lang.CharSequence)
	 */
	public CharSequence addTokens(CharSequence tokens) {
		
		//	check parameter
		if ((tokens == null) || (tokens.length() == 0)) return tokens;
		
		//	insert padding if necessary
		if (this.needsPadding(this.charData, this.charData.length(), tokens, 0))
			tokens = (" " + tokens);
		
		//	append and return prepared sequence (token overlay will be adjusted in charsInserted() method)
		this.charData.addChars(tokens);
		return tokens;
	}
	
	/** determine if two char sequences need padding in order not to hamper tokenization
	 * @param	cs1		the first char sequence
	 * @param	o1		the offset before which to check the first char sequence backward (exclusive, thus 1 to length())
	 * @param	cs2		the second char sequence
	 * @param	o2		the offset from which to check the second char sequence forward (inclusive, thus 0 to length()-1)
	 * @return true if a padding whitespace needs to be inserted between the two char sequences so tokenization is not hampered
	 */
	private boolean needsPadding(CharSequence cs1, int o1, CharSequence cs2, int o2) {
		
		//	insertion at start of first sequence, or first sequence has whitespace in ending position
		if ((o1 == 0) || (cs1.charAt(o1 - 1) < 33)) return false;
		
		//	insertion at end of second sequence, or second sequence has whitespace in starting position
		if ((o2 == cs2.length()) || (cs2.charAt(o2) < 33)) return false;
		
		//	check if padding necessary
		Tokenizer.TokenIterator ti;
		
		//	find start of last block in cs1
		int lbs = o1 - 1;
		while ((lbs > -1) && (cs1.charAt(lbs) > 32)) lbs--;
		String lb = cs1.subSequence((lbs + 1), o1).toString();
		
		//	count tokens in own last block
		int lbTokens = 0;
		ti = this.getTokenizer().getTokenIterator(lb);
		while (ti.hasMoreTokens()) {
			lbTokens++;
			ti.getNextToken();
		}
		
		//	count tokens in parameter's first block
		int fbe = o2;
		while ((fbe < cs2.length()) && (cs2.charAt(fbe) > 32)) fbe++;
		String fb = cs2.subSequence(o2, fbe).toString();
		
		//	count tokens in parameter's first block
		int fbTokens = 0;
		ti = this.getTokenizer().getTokenIterator(fb);
		while (ti.hasMoreTokens()) {
			fbTokens++;
			ti.getNextToken();
		}
		
		//	count tokens in concatenation of both blocks
		int concatTokens = 0;
		ti = this.getTokenizer().getTokenIterator(lb + fb);
		while (ti.hasMoreTokens()) {
			concatTokens++;
			ti.getNextToken();
		}
		
		//	check if tokens of concatenation are as many as sum of tokens of argument sequences
		return ((lbTokens + fbTokens) != concatTokens);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#clear()
	 */
	public void clear() {
		this.tokenOverlay.clear();
		this.charData.removeChars(0, this.charData.length());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#getMutableSubsequence(int, int)
	 */
	public MutableTokenSequence getMutableSubsequence(int start, int size) {
		MutableCharSequence chars;
		try {
			chars = ((MutableCharSequence) this.charData.getClass().newInstance());
		} catch (InstantiationException e) {
			chars = new StringBufferCharSequence();
		} catch (IllegalAccessException e) {
			chars = new StringBufferCharSequence();
		}
		
		//	empty sub sequence
		if ((start == this.size()) && (size == 0))
			chars.addChars(this.charData.subSequence(this.lastToken().getEndOffset(), this.charData.length()));
		
		//	sub sequence with content
		else chars.addChars(this.charData.subSequence(this.tokenAt(start).getStartOffset(), this.tokenAt(start + size - 1).getEndOffset()));
		
		//	wrap chars in token overlay and return them
		return new GamtaTokenSequence(chars, this.getTokenizer());
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
		
		//	empty suffix
		if ((start == this.size()) && (size == 0)) 
			return new PlainTokenSequence(this.charData.subSequence(this.lastToken().getEndOffset(), this.charData.length()), this.getTokenizer());
		
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
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		return this.charData.charAt(index);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.charData.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return this.charData.subSequence(start, end);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#mutableSubSequence(int, int)
	 */
	public MutableCharSequence mutableSubSequence(int start, int end) {
		return this.charData.mutableSubSequence(start, end);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChar(char)
	 */
	public void addChar(char ch) {
		this.charData.addChar(ch);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChars(java.lang.CharSequence)
	 */
	public void addChars(CharSequence chars) {
		this.charData.addChars(chars);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChar(char, int)
	 */
	public void insertChar(char ch, int offset) {
		this.charData.insertChar(ch, offset);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChars(java.lang.CharSequence, int)
	 */
	public void insertChars(CharSequence chars, int offset) {
		this.charData.insertChars(chars, offset);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChar(int)
	 */
	public char removeChar(int offset) {
		return this.charData.removeChar(offset);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChars(int, int)
	 */
	public CharSequence removeChars(int offset, int length) {
		return this.charData.removeChars(offset, length);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChar(char, int)
	 */
	public char setChar(char ch, int offset) {
		return this.charData.setChar(ch, offset);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChars(java.lang.CharSequence, int, int)
	 */
	public CharSequence setChars(CharSequence chars, int offset, int length) {
		return this.charData.setChars(chars, offset, length);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addCharSequenceListener(de.gamta.CharSequenceListener)
	 */
	public void addCharSequenceListener(CharSequenceListener csl) {
		this.charData.addCharSequenceListener(csl);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeCharSequenceListener(de.gamta.CharSequenceListener)
	 */
	public void removeCharSequenceListener(CharSequenceListener csl) {
		this.charData.removeCharSequenceListener(csl);
	}
}
