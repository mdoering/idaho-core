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

import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Static class providing comparison and other functionality for token
 * sequences.
 * 
 * @author sautter
 */
public class TokenSequenceUtils {

	/**
	 * get a Token value's index in a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @return the index of the first Token t in ts such that t.equals(token) is
	 *         true, or -1 if there is no such Token
	 */
	public static int indexOf(TokenSequence ts, CharSequence token) {
		return indexOf(ts, token, 0, true);
	}

	/**
	 * get a Token value's index in a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @param caseSensitive do case sensitive comparison?
	 * @return the index of the first Token t in ts such that t.equals(token) is
	 *         true, or -1 if there is no such Token
	 */
	public static int indexOf(TokenSequence ts, CharSequence token, boolean caseSensitive) {
		return indexOf(ts, token, 0, caseSensitive);
	}

	/**
	 * get a Token value's index in a TokenSequence, starting from a given
	 * position
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @param from the index to start at
	 * @return the index of the first Token t in ts such that t.equals(token) is
	 *         true, or -1 if there is no such Token
	 */
	public static int indexOf(TokenSequence ts, CharSequence token, int from) {
		return indexOf(ts, token, from, true);
	}

	/**
	 * get a Token value's index in a TokenSequence, starting from a given
	 * position
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @param from the index to start at
	 * @param caseSensitive do case sensitive comparison?
	 * @return the index of the first Token t in ts such that t.equals(token) is
	 *         true, or -1 if there is no such Token
	 */
	public static int indexOf(TokenSequence ts, CharSequence token, int from, boolean caseSensitive) {
		for (int t = from; t < ts.size(); t++) {
			if (CharSequenceUtils.equals(ts.tokenAt(t), token))
				return t;
			else if (!caseSensitive && CharSequenceUtils.equalsIgnoreCase(ts.tokenAt(t), token))
				return t;
		}
		return -1;
	}

	/**
	 * get a Token value's index in a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @return the index of the last Token t in ts such that t.equals(token) is
	 *         true, or -1 if there is no such Token
	 */
	public static int lastIndexOf(TokenSequence ts, CharSequence token) {
		return lastIndexOf(ts, token, ts.size(), true);
	}

	/**
	 * get a Token value's index in a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @param caseSensitive do case sensitive comparison?
	 * @return the index of the last Token t in ts such that t.equals(token) is
	 *         true, or -1 if there is no such Token
	 */
	public static int lastIndexOf(TokenSequence ts, CharSequence token, boolean caseSensitive) {
		return lastIndexOf(ts, token, ts.size(), caseSensitive);
	}

	/**
	 * get a Token value's index in a TokenSequence, starting from a given
	 * position
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @param to the index to start at
	 * @return the index of the last Token t in ts such that t.equals(token) is
	 *         true, or -1 if there is no such Token
	 */
	public static int lastIndexOf(TokenSequence ts, CharSequence token, int to) {
		return lastIndexOf(ts, token, to, true);
	}

	/**
	 * get a Token value's index in a TokenSequence, starting from a given
	 * position
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @param to the index to start at
	 * @param caseSensitive do case sensitive comparison?
	 * @return the index of the last Token t in ts such that t.equals(token) is
	 *         true, or -1 if there is no such Token
	 */
	public static int lastIndexOf(TokenSequence ts, CharSequence token, int to, boolean caseSensitive) {
		for (int t = Math.min(to, (ts.size() - 1)); t > -1; t--)
			if (CharSequenceUtils.equals(ts.tokenAt(t), token)) return t;
			else if (!caseSensitive && CharSequenceUtils.equalsIgnoreCase(ts.tokenAt(t), token)) return t;
		return -1;
	}

	/**
	 * Test if a token sequence starts with a specific sub sequence.
	 * @param ts the TokenSequence to test
	 * @param tokens the TokenSequence to search for
	 * @return true if ts starts with tokens, false otherwise
	 */
	public static boolean startsWith(TokenSequence ts, TokenSequence tokens) {
		return startsWith(ts, tokens, 0, true);
	}
	
	/**
	 * Test if a token sequence starts with a specific sub sequence.
	 * @param ts the TokenSequence to test
	 * @param tokens the TokenSequence to search for
	 * @param caseSensitive do case sensitive comparison?
	 * @return true if ts starts with tokens, false otherwise
	 */
	public static boolean startsWith(TokenSequence ts, TokenSequence tokens, boolean caseSensitive) {
		return startsWith(ts, tokens, 0, caseSensitive);
	}
	
	/**
	 * Test if a token sequence contains a specific sub sequence at a specific
	 * index.
	 * @param ts the TokenSequence to test
	 * @param tokens the TokenSequence to search for
	 * @param from the index to test from
	 * @return true if ts starts with tokens, false otherwise
	 */
	public static boolean startsWith(TokenSequence ts, TokenSequence tokens, int from) {
		return startsWith(ts, tokens, from, true);
	}
	
	/**
	 * Test if a token sequence contains a specific sub sequence at a specific
	 * index.
	 * @param ts the TokenSequence to test
	 * @param tokens the TokenSequence to search for
	 * @param from the index to test from
	 * @param caseSensitive do case sensitive comparison?
	 * @return true if ts starts with tokens, false otherwise
	 */
	public static boolean startsWith(TokenSequence ts, TokenSequence tokens, int from, boolean caseSensitive) {
		for (int t = 0; t < tokens.size(); t++) {
			if ((from + t) >= ts.size())
				return false;
			if (!(caseSensitive ? ts.valueAt(from + t).equals(tokens.valueAt(t)) : ts.valueAt(from + t).equalsIgnoreCase(tokens.valueAt(t))))
				return false;
		}
		return true;
	}
	
	/**
	 * get the start index of a subsequence of a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @return the index of the first Token of the TokenSequence in ts, or -1 if
	 *         there is no such TokenSequence
	 */
	public static int indexOf(TokenSequence ts, TokenSequence tokens) {
		return indexOf(ts, tokens, 0, true);
	}

	/**
	 * get the start index of a subsequence of a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @param caseSensitive do case sensitive comparison?
	 * @return the index of the first Token of the TokenSequence in ts, or -1 if
	 *         there is no such TokenSequence
	 */
	public static int indexOf(TokenSequence ts, TokenSequence tokens, boolean caseSensitive) {
		return indexOf(ts, tokens, 0, caseSensitive);
	}

	/**
	 * get the start index of a subsequence of a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @param from the index to start at
	 * @return the index of the first Token of the TokenSequence in ts, or -1 if
	 *         there is no such TokenSequence
	 */
	public static int indexOf(TokenSequence ts, TokenSequence tokens, int from) {
		return indexOf(ts, tokens, from, true);
	}

	/**
	 * get the start index of a subsequence of a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @param from the index to start at
	 * @param caseSensitive do case sensitive comparison?
	 * @return the index of the first Token of the TokenSequence in ts, or -1 if
	 *         there is no such TokenSequence
	 */
	public static int indexOf(TokenSequence ts, TokenSequence tokens, int from, boolean caseSensitive) {
		if (tokens.size() == 0)
			return -1;
		Token anchor = tokens.firstToken();
		int s = indexOf(ts, anchor, from, caseSensitive);
		while (s != -1) {
			int t = 1;
			while (t != 0) {
				if (t == tokens.size())
					return s;
				else if ((s + t) == ts.size())
					return -1;
				else if (
					CharSequenceUtils.equals(ts.tokenAt(s + t), tokens.tokenAt(t))
					||
					(
						!caseSensitive 
						&&
						CharSequenceUtils.equalsIgnoreCase(ts.tokenAt(s + t), tokens.tokenAt(t)))
					)
					t++;
				else t = 0;
			}
			s = indexOf(ts, anchor, (s + 1));
		}
		return -1;
	}

	/**
	 * get the start index of a subsequence of a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @return the index of the last Token of the TokenSequence in ts, or -1 if
	 *         there is no such TokenSequence
	 */
	public static int lastIndexOf(TokenSequence ts, TokenSequence tokens) {
		return lastIndexOf(ts, tokens, ts.size(), true);
	}

	/**
	 * get the start index of a subsequence of a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @param caseSensitive do case sensitive comparison?
	 * @return the index of the last Token of the TokenSequence in ts, or -1 if
	 *         there is no such TokenSequence
	 */
	public static int lastIndexOf(TokenSequence ts, TokenSequence tokens, boolean caseSensitive) {
		return lastIndexOf(ts, tokens, ts.size(), caseSensitive);
	}

	/**
	 * get the start index of a subsequence of a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @param to the index to start at
	 * @return the index of the last Token of the TokenSequence in ts, or -1 if
	 *         there is no such TokenSequence
	 */
	public static int lastIndexOf(TokenSequence ts, TokenSequence tokens, int to) {
		return lastIndexOf(ts, tokens, to, true);
	}

	/**
	 * get the start index of a subsequence of a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @param to the index to start at
	 * @param caseSensitive do case sensitive comparison?
	 * @return the index of the last Token of the TokenSequence in ts, or -1 if
	 *         there is no such TokenSequence
	 */
	public static int lastIndexOf(TokenSequence ts, TokenSequence tokens, int to, boolean caseSensitive) {
		if (tokens.size() == 1)
			return -1;
		Token anchor = tokens.firstToken();
		int s = lastIndexOf(ts, anchor, to, caseSensitive);
		while (s != -1) {
			int t = 1;
			while (t != 0) {
				if (t == tokens.size())
					return s;
				else if ((s + t) == ts.size())
					t = 0;
				else if (
					CharSequenceUtils.equals(ts.tokenAt(s + t), tokens.tokenAt(t))
					||
					(
						!caseSensitive 
						&&
						CharSequenceUtils.equalsIgnoreCase(ts.tokenAt(s + t), tokens.tokenAt(t)))
					)
					t++;
				else t = 0;
			}
			s = lastIndexOf(ts, anchor, (s - 1));
		}
		return -1;
	}

	/**
	 * check if a Token is contained in a TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @return true if and only if ts contains a Token t such that
	 *         t.equals(token) is true
	 */
	public static boolean contains(TokenSequence ts, Token token) {
		return (indexOf(ts, token) != -1);
	}

	/**
	 * check if a Token is contained in a TokenSequence, using case insensitive
	 * comparison
	 * @param ts the TokenSequence to search through
	 * @param token the Token to search for
	 * @return true if and only if ts contains a Token t such that
	 *         t.equals(token) is true
	 */
	public static boolean containsIgnoreCase(TokenSequence ts, Token token) {
		return (indexOf(ts, token, false) != -1);
	}

	/**
	 * check if a TokenSequence is contained in (i.e. is a subsequence of) a
	 * TokenSequence
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @return true if and only if ts contains a Token t such that
	 *         t.equals(token) is true
	 */
	public static boolean contains(TokenSequence ts, TokenSequence tokens) {
		return (indexOf(ts, tokens) != -1);
	}

	/**
	 * check if a TokenSequence is contained in (i.e. is a subsequence of) a
	 * TokenSequence, using case insensitive comparison
	 * @param ts the TokenSequence to search through
	 * @param tokens the TokenSequence to search for
	 * @return true if and only if ts contains a Token t such that
	 *         t.equals(token) is true
	 */
	public static boolean containsIgnoreCase(TokenSequence ts, TokenSequence tokens) {
		return (indexOf(ts, tokens, false) != -1);
	}

	/**
	 * get the values of a sequence of Tokens, concatenated to a String Note:
	 * this method does not include the leading and tailing whitespaces of the
	 * specified token sequence
	 * @param tokens the TokenSequence to concatenate
	 * @return the values of all Tokens in a TokenSequence, concatenated to a
	 *         String
	 */
	public static String concatTokens(TokenSequence tokens) {
		return concatTokens(tokens, 0, tokens.size());
	}

	/**
	 * get the values of a sequence of Tokens, concatenated to a String Note:
	 * this method does not include the leading and tailing whitespaces of the
	 * specified token sequence
	 * @param tokens the TokenSequence to concatenate
	 * @param normalizeWhitespace completely normalize whitespace between
	 *            tokens? (see five argument version of this method for
	 *            explanation)
	 * @param ignoreLineBreaks suppress including line breaks in result string?
	 *            (see five argument version of this method for explanation)
	 * @return the values of a sequence of Tokens, concatenated to a String
	 */
	public static String concatTokens(TokenSequence tokens, boolean normalizeWhitespace, boolean ignoreLineBreaks) {
		return concatTokens(tokens, 0, tokens.size(), normalizeWhitespace, ignoreLineBreaks);
	}

	/**
	 * get the values of a sequence of Tokens, concatenated to a String Note:
	 * this method does not include the leading and tailing whitespaces of the
	 * specified token sequence
	 * @param tokens the TokenSequence to concatenate
	 * @param start the index of the Token to start at
	 * @param size the number of Tokens to concatenate
	 * @return the values of a sequence of Tokens, concatenated to a String
	 */
	public static String concatTokens(TokenSequence tokens, int start, int size) {
		return concatTokens(tokens, start, size, false, false);
	}

	/**
	 * Concatenate the individual token values of a token sequence to a String.
	 * Depending on the parameters, the whitespace between the token values can
	 * be normalized to different levels:<br>
	 * If <code>normalizeWhitespace</code> is false, a single whitespace
	 * character is inserted in the result String if there is one or more
	 * whitespace characters in the specified token sequence, no whitespace if
	 * there is none. If <code>normalizeWhitespace</code> is true, in turn,
	 * whitespace is inserted between two tokens t and u if
	 * <code>Gamta.insertSpace(t, u)</code> returns true, or if it is a line
	 * break.<br>
	 * If <code>ignoreLineBreaks</code> is true, every whitespace character
	 * inserted in the result string is the plain space character. If
	 * <code>ignoreLineBreaks</code> is false, in turn, the newline character
	 * is inserted after tokens having the
	 * <code>Token.PARAGRAPH_END_ATTRIBUTE</code> attribute set. <br>
	 * Note: this method does not include the leading and tailing whitespaces of
	 * the specified token sequence
	 * @param tokens the TokenSequence to concatenate
	 * @param start the index of the Token to start at
	 * @param size the number of Tokens to concatenate
	 * @param normalizeWhitespace completely normalize whitespace between
	 *            tokens? (default is false, see above for explanation)
	 * @param ignoreLineBreaks suppress including line breaks in result string?
	 *            (default is false, see above for explanation)
	 * @return the values of a sequence of Tokens, concatenated to a String
	 */
	public static String concatTokens(TokenSequence tokens, int start, int size, boolean normalizeWhitespace, boolean ignoreLineBreaks) {
		if ((size == 0) || (tokens.size() == 0)) return "";
		
		String lastValue = tokens.valueAt(start);
		boolean insertLineBreak = false;
		StringBuffer result = new StringBuffer(lastValue);
		
		for (int t = (start + 1); t < (start + size); t++) {
			Token token = tokens.tokenAt(t);
			String value = token.getValue();

			if (insertLineBreak) result.append("\n");
			else if (normalizeWhitespace ? Gamta.insertSpace(lastValue, value) : (tokens.getWhitespaceAfter(t - 1).length() != 0)) result.append(" ");

			result.append(value);

			lastValue = value;
			insertLineBreak = (!ignoreLineBreaks && token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE));
		}

		return result.toString();
	}

	/**
	 * extract all tokens from a token sequence that are words or numbers
	 * @param tokens the token sequence to process
	 * @return all token values from the specified token sequence that are words
	 *         or numbers, packed in a StringVector in their original order
	 */
	public static StringVector getTextTokens(TokenSequence tokens) {
		StringVector textTokens = new StringVector();
		Token token;
		for (int t = 0; t < tokens.size(); t++) {
			token = tokens.tokenAt(t);
			if (Gamta.isWord(token) || Gamta.isNumber(token)) textTokens.addElement(token.getValue());
		}
		return textTokens;
	}

	/**
	 * find the index of the token at some character offset
	 * @param tokens the token sequence to process
	 * @param offset the offset to find the token for
	 * @return the index of the token at the specified offset, or -1, if there
	 *         is no such token
	 */
	public static int getTokenIndexAtOffset(TokenSequence tokens, int offset) {

		// check parameter
		if (tokens.size() == 0) return -1;

		// use binary search to narrow search interval
		int left = 0;
		int right = tokens.size();
		int tIndex = 0;
		while ((right - left) > 2) {
			tIndex = ((left + right) / 2);
			if (tokens.tokenAt(tIndex).getEndOffset() <= offset) left = tIndex;
			else if (tokens.tokenAt(tIndex).getStartOffset() <= offset) return tIndex;
			else right = tIndex;
		}

		// scan remaining interval
		tIndex = left;
		while (tIndex < tokens.size()) {
			if (tokens.tokenAt(tIndex).getEndOffset() <= offset) tIndex++;
			else if (tokens.tokenAt(tIndex).getStartOffset() <= offset) return tIndex;
			else tIndex++;
		}
		return -1;
	}
}
