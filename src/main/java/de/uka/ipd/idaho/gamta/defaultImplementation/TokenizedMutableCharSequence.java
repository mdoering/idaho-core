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
import de.uka.ipd.idaho.gamta.CharSequenceUtils;
import de.uka.ipd.idaho.gamta.MutableCharSequence;
import de.uka.ipd.idaho.gamta.MutableTokenSequence;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceListener;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.stringUtils.StringUtils;

/**
 * This class implements a mutable char sequence and a mutable token sequence in
 * combination. While this is not as clean as a token sequence overlay to a
 * mutable char sequence, it is in great favor of higher performance with larger
 * amounts of text.
 * 
 * @author sautter
 */
public class TokenizedMutableCharSequence extends TokenizedCharSequence implements MutableTokenSequence {
	
	private static final boolean DEBUG = false;
	
	/**	Constructor
	 * @param	tokenizer	the tokenizer to use for creating tokens from the character data
	 */
	public TokenizedMutableCharSequence(Tokenizer tokenizer) {
		super(tokenizer);
	}
	
	/**	Constructor
	 * @param	tokenizer	the tokenizer to use for creating tokens from the character data
	 * @param	data		the character data to be initially contained in this mutable token sequence
	 */
	public TokenizedMutableCharSequence(Tokenizer tokenizer, CharSequence data) {
		super(tokenizer, data);
	}
	
	/**	Constructor cloning a token sequence.
	 * @param	original	the token sequence to copy
	 */
	public TokenizedMutableCharSequence(TokenSequence original) {
		super(original.getTokenizer());
		
		//	copy leading whitespace
		this.leadingWhitespace.append(original.getLeadingWhitespace());
		this.length += this.leadingWhitespace.length();
		
		//	copy tokens
		for (int t = 0; t < original.size(); t++) {
			Token originalToken = original.tokenAt(t);
			
			//	copy token data
			GamtaToken token = new GamtaToken(originalToken.getValue(), original.getWhitespaceAfter(t));
			token.copyAttributes(originalToken);
			
			//	adjust offsets
			token.startOffset = this.length;
			this.length += token.gtLength();
			
			//	store token
			this.tokens.add(token);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#setLeadingWhitespace(java.lang.CharSequence)
	 */
	public CharSequence setLeadingWhitespace(CharSequence whitespace) throws IllegalArgumentException {
		if (whitespace.toString().trim().length() != 0)
			throw new IllegalArgumentException("Whitespace must not contain non-whitespace characters.");
		
		StringBuffer nlw = new StringBuffer(whitespace.toString());
		CharSequence olw = this.leadingWhitespace;
		
		this.leadingWhitespace = nlw;
		this.length += (nlw.length() - olw.length());
		this.adjustTokenOffsets(0, (nlw.length() - olw.length()));
		this.checkTokenOffsets(-1, 1);
		
		this.notifyCharSequenceChanged(0, nlw.toString(), olw.toString());
		return olw.toString();
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#setValueAt(java.lang.CharSequence, int)
	 */
	public CharSequence setValueAt(CharSequence value, int index) throws IllegalArgumentException {
		if (DEBUG) System.out.println("SETTING VALUE AT " + index + " TO '" + value + "'");
		
		Token tok = this.tokenAt(index);
		if (this.tokensMergeAtBoundary(this, tok.getStartOffset(), value, 0))
			value = (" " + value);
		if (this.tokensMergeAtBoundary(value, value.length(), this, tok.getEndOffset()))
			value = (value + " ");
		
		if (DEBUG) System.out.println("- value padded to '" + value + "'");
		this.setChars(value, tok.getStartOffset(), tok.length());
		return value;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#setWhitespaceAfter(java.lang.CharSequence, int)
	 */
	public CharSequence setWhitespaceAfter(CharSequence whitespace, int index) throws IllegalArgumentException {
		if (whitespace.toString().trim().length() != 0)
			throw new IllegalArgumentException("Whitespace must not contain non-whitespace characters.");
		
		GamtaToken gt = this.tcsTokenAt(index);
		
		StringBuffer nw = new StringBuffer(whitespace.toString());
		CharSequence ow = gt.whitespace;
		
		gt.whitespace = nw;
		this.length += (nw.length() - ow.length());
		this.adjustTokenOffsets((index + 1), (nw.length() - ow.length()));
		this.checkTokenOffsets((index - 1), (index + 2));
		
		this.notifyCharSequenceChanged(gt.getEndOffset(), nw.toString(), ow.toString());
		return ow.toString();
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#removeTokensAt(int, int)
	 */
	public TokenSequence removeTokensAt(int index, int size) {
		if (DEBUG) System.out.println("REMOVING " + size + " TOKENS AT " + index);
		
		int startOffset = this.tokenAt(index).getStartOffset();
		if (DEBUG) System.out.println("- start offset is " + startOffset);
		char bc = ((startOffset == 0) ? StringUtils.NULLCHAR : this.charAt(startOffset - 1));
		if (DEBUG) System.out.println("- char before is '" + bc + "'");
		int endOffset = (((index + size) == this.size()) ? this.length() : this.tokenAt(index + size - 1).getEndOffset());
		if (DEBUG) System.out.println("- end offset is " + endOffset);
		char ac = ((endOffset == this.length()) ? StringUtils.NULLCHAR : this.charAt(endOffset));
		if (DEBUG) System.out.println("- char after is '" + ac + "'");
		
		String paddingWhitespace = "";
		char transferWhitespace = ' ';
		if (bc == StringUtils.NULLCHAR) {
			if ((ac != StringUtils.NULLCHAR) && (ac < 33))
				endOffset++;
		}
		else if (ac == StringUtils.NULLCHAR) {}
		else if (ac == 32) {
			if (bc < 33)
				endOffset++;
		}
		else if (bc == 32) {
			if (ac < 33) {
				endOffset++;
				transferWhitespace = ac;
			}
		}
		else if ((bc < 33) && (ac < 33))
			endOffset++;
		else if (this.tokensMergeAtBoundary(this, startOffset, this, endOffset))
			paddingWhitespace = " ";
		
		if (DEBUG) System.out.println("- transfer whitespace is '" + transferWhitespace + "'");
		if (DEBUG) System.out.println("- padding whitespace is '" + paddingWhitespace + "'");
		CharSequence removedChars = this.setChars(paddingWhitespace, startOffset, (endOffset - startOffset));
		if ((transferWhitespace != ' ') && (index != 0)) {
			GamtaToken bt = this.tcsTokenAt(index-1);
			int wsl = bt.whitespace.length();
			if (wsl != 0)
				bt.whitespace.setCharAt((wsl-1), transferWhitespace);
		}
		return new TokenizedCharSequence(this.tokenizer, removedChars);
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#insertTokensAt(java.lang.CharSequence, int)
	 */
	public CharSequence insertTokensAt(CharSequence tokens, int index) {
		
		//	check parameter
		if ((tokens == null) || (tokens.length() == 0))
			return tokens;
		if (DEBUG) System.out.println("INSERTING TOKENS '" + tokens + "' at " + index);
		
		//	insert padding if necessary
		int insertOffset = ((index == this.size()) ? this.length : this.tokenAt(index).getStartOffset());
		if (DEBUG) System.out.println("- insert offset is " + insertOffset);
		if (this.tokensMergeAtBoundary(this, insertOffset, tokens, 0))
			tokens = (" " + tokens);
		if (this.tokensMergeAtBoundary(tokens, tokens.length(), this, insertOffset))
			tokens = (tokens + " ");
		
		//	append and return prepared sequence (token overlay will be adjusted in charsInserted() method)
		if (DEBUG) System.out.println("- tokens padded to '" + tokens + "'");
		this.insertChars(tokens, insertOffset);
		return tokens;
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#addTokens(java.lang.CharSequence)
	 */
	public CharSequence addTokens(CharSequence tokens) {
		
		//	check parameter
		if ((tokens == null) || (tokens.length() == 0))
			return tokens;
		if (DEBUG) System.out.println("ADDING TOKENS '" + tokens + "'");
		
		//	insert padding if necessary
		if (this.tokensMergeAtBoundary(this, this.length, tokens, 0))
			tokens = (" " + tokens);
		
		//	append and return prepared sequence
		if (DEBUG) System.out.println("- tokens padded to '" + tokens + "'");
		this.addChars(tokens);
		return tokens;
	}
	
	/* determine if two char sequences need padding in order not to hamper tokenization
	 * @param	cs1		the first char sequence
	 * @param	o1		the offset before which to check the first char sequence backward (exclusive, thus 1 to length())
	 * @param	cs2		the second char sequence
	 * @param	o2		the offset from which to check the second char sequence forward (inclusive, thus 0 to length()-1)
	 * @return true if a padding whitespace needs to be inserted between the two char sequences so tokenization is not hampered
	 */
	private boolean tokensMergeAtBoundary(CharSequence cs1, int o1, CharSequence cs2, int o2) {
		
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
		ti = this.tokenizer.getTokenIterator(lb);
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
		ti = this.tokenizer.getTokenIterator(fb);
		while (ti.hasMoreTokens()) {
			fbTokens++;
			ti.getNextToken();
		}
		
		//	count tokens in concatenation of both blocks
		int concatTokens = 0;
		ti = this.tokenizer.getTokenIterator(lb + fb);
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
		this.tokens.clear();
		this.leadingWhitespace = new StringBuffer();
		this.length = 0;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#getMutableSubsequence(int, int)
	 */
	public MutableTokenSequence getMutableSubsequence(int start, int size) {
		TokenizedMutableCharSequence tmcs = new TokenizedMutableCharSequence(this.tokenizer);
		
		//	copy tokens and compute length
		for (int t = start; t < (start + size); t++) {
			GamtaToken gt = this.tcsTokenAt(t);
			GamtaToken cgt = new GamtaToken(gt.value, (((t+1) == (start + size)) ? ((CharSequence) "") : ((CharSequence) gt.whitespace)));
			cgt.startOffset = tmcs.length;
			tmcs.tokens.add(cgt);
			tmcs.length += cgt.gtLength();
		}
		
		//	return copy
		return tmcs;
	}

	private ArrayList charListeners = new ArrayList();
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addCharSequenceListener(de.gamta.CharSequenceListener)
	 */
	public void addCharSequenceListener(CharSequenceListener csl) {
		this.charListeners.add(csl);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeCharSequenceListener(de.gamta.CharSequenceListener)
	 */
	public void removeCharSequenceListener(CharSequenceListener csl) {
		this.charListeners.remove(csl);
	}
	
	/**	notify this listener that a portion of this MutableCharSequence has been changed
	 * @param	offset		the start position of the change
	 * @param	inserted	the sequence of chars that was inserted
	 * @param	removed		the sequence of chars that was removed
	 */
	private void notifyCharSequenceChanged(int offset, java.lang.CharSequence inserted, java.lang.CharSequence removed) {
		this.notifyCharSequenceChanged(new CharSequenceEvent(this, offset, inserted, removed));
	}
	
	/**	notify this listener that a portion of this MutableCharSequence has been changed
	 * @param	cse		the CharSequenceEvent holding the details of the cange
	 */
	private void notifyCharSequenceChanged(CharSequenceEvent cse) {
		for (int l = 0; l < this.charListeners.size(); l++)
			((CharSequenceListener) this.charListeners.get(l)).charSequenceChanged(cse);
	}
	
	
	private ArrayList tokenListeners = new ArrayList();
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#addTokenSequenceListener(de.gamta.TokenSequenceListener)
	 */
	public void addTokenSequenceListener(TokenSequenceListener tsl) {
		this.tokenListeners.add(tsl);
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#removeTokenSequenceListener(de.gamta.TokenSequenceListener)
	 */
	public void removeTokenSequenceListener(TokenSequenceListener tsl) {
		this.tokenListeners.remove(tsl);
	}
	
	/**	notify this listener that a portion of this MutableTokenSequence has been changed
	 * @param	tse		the TokenSequenceEvent holding the details of the cange
	 */
	private void notifyTokenSequenceChanged(TokenSequenceEvent tse) {
		for (int l = 0; l < this.tokenListeners.size(); l++)
			((TokenSequenceListener) this.tokenListeners.get(l)).tokenSequenceChanged(tse);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChar(char)
	 */
	public void addChar(char ch) {
		this.setChars(("" + ch), this.length, 0);
		return;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChars(java.lang.CharSequence)
	 */
	public void addChars(CharSequence chars) {
		this.setChars(chars, this.length, 0);
		return;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChar(char, int)
	 */
	public void insertChar(char ch, int offset) {
		this.setChars(("" + ch), offset, 0);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChars(java.lang.CharSequence, int)
	 */
	public void insertChars(CharSequence chars, int offset) {
		this.setChars(chars, offset, 0);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChar(int)
	 */
	public char removeChar(int offset) {
		CharSequence removed = this.setChars("", offset, 1);
		return removed.charAt(0);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChars(int, int)
	 */
	public CharSequence removeChars(int offset, int length) {
		return this.setChars("", offset, length);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChar(char, int)
	 */
	public char setChar(char ch, int offset) {
		CharSequence removed = this.setChars(("" + ch), offset, 1);
		return removed.charAt(0);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChars(java.lang.CharSequence, int, int)
	 */
	public CharSequence setChars(CharSequence chars, int offset, int length) {
		if ((offset + length) > this.length)
			throw new IndexOutOfBoundsException("" + offset + "+" + length + " > " + this.length);
		
		if (DEBUG) System.out.println("REPLACING " + length + " CHARS AT " + offset + " WITH '" + chars + "'");
		try {
			CharSequence removed = this.subSequence(offset, (offset + length));
			if (DEBUG) System.out.println("- replaced is '" + removed + "'");
			
			CharSequenceEvent cse = new CharSequenceEvent(this, offset, chars, removed);
			TokenSequenceEvent tse = null;
			
			//	replacement only inside leading whitespace, or at end of empty token sequence
			if (this.tokens.isEmpty() || ((offset + length) < this.leadingWhitespace.length())) {
				if (DEBUG) System.out.println("- replacement inside leading whitespace");
				
				//	tokenize inserted character sequence embedded in current leading whitespace
				TokenizedCharSequence tcs = new TokenizedCharSequence(this.tokenizer, ("" + this.leadingWhitespace.subSequence(0, offset) + chars + this.leadingWhitespace.subSequence((offset + length), this.leadingWhitespace.length())));
				
				//	transfer new leading whitespace
				this.leadingWhitespace = new StringBuffer(tcs.getLeadingWhitespace());
				
				//	transfer any new tokens
				for (int t = 0; t < tcs.size(); t++) {
					GamtaToken gt = tcs.tcsTokenAt(t);
					GamtaToken cgt = new GamtaToken(gt.value, gt.whitespace);
					cgt.startOffset = gt.startOffset;
					this.tokens.insertElementAt(cgt, t);
				}
				
				//	adjust data structures
				this.length += (chars.length() - length);
				this.adjustTokenOffsets(tcs.size(), (chars.length() - length));
				this.checkTokenOffsets(-1, (tcs.size() + 1));
				
				//	notify listeners
				this.notifyCharSequenceChanged(cse);
				if (tcs.size() != 0) {
					tcs.leadingWhitespace = new StringBuffer();
					tcs.tcsLastToken().whitespace = new StringBuffer();
					tse = new TokenSequenceEvent(this, 0, tcs, null, cse);
					this.notifyTokenSequenceChanged(tse);
				}
				
				//	finally ...
				return removed;
			}
			
			//	get affected token index
			int from = this.tcsIndexAtOffset(offset);
			if (DEBUG) System.out.println("- from index is " + from);
			
			//	handle part of replacement in leading whitespace (if we get here, leading whitespace is removed completely from offset onward
			if (from == -1) {
				if (DEBUG) System.out.println("- start is in leading whitespace");
				
				//	truncate leading whitespace
				int dlw = (this.leadingWhitespace.length() - offset);
				this.leadingWhitespace.delete(offset, this.leadingWhitespace.length());
				
				//	add leading whitespace of inserted character sequence
				int alw = 0;
				while ((alw < chars.length()) && (chars.charAt(alw) < 33))
					this.leadingWhitespace.append(chars.charAt(alw++));
				
				//	adjust data structures
				this.length += (alw - dlw);
				this.adjustTokenOffsets(0, (alw - dlw));
				this.checkTokenOffsets(-1, 1);
				
				//	adjust arguments
				offset += alw;
				length -= dlw;
				chars = chars.subSequence(alw, chars.length());
				from = 0;
				if (DEBUG) System.out.println("- offset increased to " + offset);
				if (DEBUG) System.out.println("- length reduced to " + length);
				if (DEBUG) System.out.println("- chars truncated to '" + chars + "'");
			}
			
			//	replacement somewhere inside the text
			int fti = from;
			boolean replacementAfterFromValue = ((from < this.tokens.size()) && (offset >= this.tcsTokenAt(from).getEndOffset()));
			StringBuffer newValue = new StringBuffer();
			while ((fti != 0) && (this.tcsTokenAt(fti-1).whitespace.length() == 0)) {
				newValue.insert(0, this.tcsTokenAt(fti-1).value.toString());
				fti--;
			}
			int ftOffset = ((fti == this.tokens.size()) ? this.length : this.tcsTokenAt(fti).startOffset);
			if (DEBUG) System.out.println("- first affected token is " + fti + ", offset is " + ftOffset);
			
			int to = from;
			while ((to < this.tokens.size()) && (this.tcsTokenAt(to).gtEndOffset() < (offset + length))) {
				newValue.append(this.tcsTokenAt(to).value);
				newValue.append(this.tcsTokenAt(to).whitespace);
				to++;
			}
			if (DEBUG) System.out.println("- to index is " + to);
			
			int lti = to;
			while ((lti < this.tokens.size()) && ((this.tcsTokenAt(lti).whitespace.length() == 0) || (lti == to))) {
				newValue.append(this.tcsTokenAt(lti).value);
				newValue.append(this.tcsTokenAt(lti).whitespace);
				lti++;
			}
			if (lti < this.tokens.size()) {
				newValue.append(this.tcsTokenAt(lti).value);
				newValue.append(this.tcsTokenAt(lti).whitespace);
			} else lti--;
			if (DEBUG) System.out.println("- last affected token is " + lti);
			
			//	do actual replacement
			newValue.delete((offset - ftOffset), (offset - ftOffset + length));
			newValue.insert((offset - ftOffset), chars.toString());
			
			TokenSequence ots = this.getSubsequence(fti, (lti - fti + 1));
			TokenizedCharSequence nts = new TokenizedCharSequence(this.tokenizer, newValue);
			if (DEBUG) System.out.println("- original token sequence is '" + ots + "'");
			if (DEBUG) System.out.println("- new token sequence is '" + nts + "'");
			
			if (nts.leadingWhitespace.length() != 0) {
				if (fti == 0)
					this.leadingWhitespace.append(nts.leadingWhitespace);
				else this.tcsTokenAt(fti - 1).whitespace.append(nts.leadingWhitespace);
				ftOffset += nts.leadingWhitespace.length();
			}
			
			int lk = 0;
			int rk = 0;
			while (((fti + lk) < (replacementAfterFromValue ? (from + 1) : from)) && ((lk + rk) < ots.size()) && (((lk + rk) < nts.size()) && CharSequenceUtils.equals(ots.tokenAt(lk), nts.tokenAt(lk)))) {
				this.tcsTokenAt(fti + lk).whitespace = nts.tcsTokenAt(lk).whitespace;
				lk++;
			}
			if (DEBUG) System.out.println("- left keep is " + lk);
			while (((lti - rk + 1) > to) && ((lk + rk) < ots.size()) && (((lk + rk) < nts.size()) && CharSequenceUtils.equals(ots.tokenAt(ots.size() - rk - 1), nts.tokenAt(nts.size() - rk - 1)))) {
				this.tcsTokenAt(lti - rk).whitespace = nts.tcsTokenAt(nts.size() - rk - 1).whitespace;
				rk++;
			}
			if (DEBUG) System.out.println("- right keep is " + rk);
			while (((lk + rk) < ots.size()) && (((lk + rk) < nts.size()) && CharSequenceUtils.equals(ots.tokenAt(lk), nts.tokenAt(lk)))) {
				this.tcsTokenAt(fti + lk).whitespace = nts.tcsTokenAt(lk).whitespace;
				lk++;
			}
			if (DEBUG) System.out.println("- left keep increased to " + lk);
			while (((lk + rk) < ots.size()) && (((lk + rk) < nts.size()) && CharSequenceUtils.equals(ots.tokenAt(ots.size() - rk - 1), nts.tokenAt(nts.size() - rk - 1)))) {
				this.tcsTokenAt(lti - rk).whitespace = nts.tcsTokenAt(nts.size() - rk - 1).whitespace;
				rk++;
			}
			if (DEBUG) System.out.println("- right keep increased to " + rk);
			
			for (int r = 0; r < (ots.size() - lk - rk); r++)
				this.tokens.remove(fti + lk);
			for (int t = 0; t < lk; t++)
				this.tcsTokenAt(fti + t).startOffset = ftOffset + nts.tcsTokenAt(t).startOffset;
			for (int t = lk; t < (nts.size() - rk); t++) {
				GamtaToken ngt = nts.tcsTokenAt(t);
				GamtaToken cgt = new GamtaToken(ngt.value, ngt.whitespace);
				cgt.startOffset = ftOffset + ngt.startOffset;
				this.tokens.insertElementAt(cgt, (fti + t));
			}
			
			this.adjustTokenOffsets((fti + nts.size() - rk), (chars.length() - length));
			this.checkTokenOffsets((fti - 1), (fti + nts.size() + 1));
			if (((lk + rk) != nts.size()) || ((lk + rk) != ots.size()))
				tse = new TokenSequenceEvent(this, (fti + lk), nts.getSubsequence(lk, (nts.size() - lk - rk)), ots.getSubsequence(lk, (ots.size() - lk - rk)), cse);
			
			this.length += (chars.length() - length);
			this.notifyCharSequenceChanged(cse);
			if (tse != null)
				this.notifyTokenSequenceChanged(tse);
			
			return removed;
		}
		catch (RuntimeException re) {
			re.printStackTrace(System.out);
			this.printSanityCheck(offset - length, offset + length + chars.length());
			throw re;
		}
	}
	
	public void printSanityCheck(int from, int to) {
		System.out.println("TokenizedMutableCharSequence statistics: size is " + this.tokens.size() + ", length is " + this.length);
		System.out.println("-1, 0, '" + this.leadingWhitespace + "'");
		int actualLength = this.leadingWhitespace.length();
		for (int t = 0; t < this.tokens.size(); t++) {
			GamtaToken gt = this.tcsTokenAt(t);
			if ((gt.startOffset >= from) && (gt.startOffset < to))
				System.out.println(t + ", " + gt.startOffset + " (" + actualLength + "), '" + gt.value + gt.whitespace + "'");
			actualLength += gt.gtLength();
		}
		System.out.println("actual length is " + actualLength);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#mutableSubSequence(int, int)
	 */
	public MutableCharSequence mutableSubSequence(int start, int end) {
		if (this.tokens.isEmpty())
			return new StringBufferCharSequence(this.leadingWhitespace.subSequence(start, end));
		
		StringBuffer subSequence = new StringBuffer();
		if (start < this.tcsFirstToken().startOffset) {
			subSequence.append(this.leadingWhitespace.subSequence(start, Math.min(end, this.leadingWhitespace.length())).toString());
			start = this.leadingWhitespace.length();
		}
		
		int to = this.tcsIndexAtOffset(start);
		GamtaToken gt = this.tcsTokenAt(to);
		while ((subSequence.length() < (end - start)) && (gt != null)) {
			subSequence.append(gt.gtSubSequence(Math.max(0, (start - gt.startOffset)), Math.min(gt.gtLength(), (end - gt.startOffset))).toString());
			to = this.tcsIndexAtOffset(gt.gtEndOffset());
			gt = ((to == this.tokens.size()) ? null : this.tcsTokenAt(to));
		}
		return new StringBufferCharSequence(subSequence);
	}
	
	private void adjustTokenOffsets(int index, int delta) {
		if (delta == 0) return;
		for (int t = index; t < this.tokens.size(); t++)
			this.tcsTokenAt(t).startOffset += delta;
	}
	
	private void checkTokenOffsets(int startIndex, int minEndIndex) {
		int end = Math.min(this.tokens.size(), minEndIndex);
		int index;
		int offset;
		if (startIndex < 0) {
			index = 0;
			offset = this.leadingWhitespace.length();
		}
		else {
			index = startIndex;
			offset = this.tcsTokenAt(startIndex).getStartOffset();
		}
		while ((index < end) || ((index < this.tokens.size()) && (this.tcsTokenAt(index).getStartOffset() != offset))) {
			GamtaToken gt = this.tcsTokenAt(index++);
			gt.startOffset = offset;
			offset += gt.gtLength();
		}
	}
}
