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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.htmlXmlUtil;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.htmlXmlUtil.exceptions.ParseException;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;

/**
 * Buffered reader that allows lookahead operations on the buffer, with several
 * static methods used both in tokenizer an attribute parser.
 * 
 * @author sautter
 */
class LookaheadReader extends FilterReader {
	private char[] charBuffer;
	private int bufferStart = 0;
	private int bufferEnd;
	private int lookahead;
	LookaheadReader(Reader in, int lookahead) throws IOException {
		super((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
		this.lookahead = Math.max(lookahead, 256);
		this.charBuffer = new char[Math.max((this.lookahead * 2), 2048)];
	}
	private void fillBuffer(int min) throws IOException {
		if (this.bufferEnd == -1)
			return;
		if (min < (this.bufferEnd - this.bufferStart))
			return;
		if (this.bufferStart != 0) {
			for (int i = 0; i < (this.bufferEnd - this.bufferStart); i++)
				this.charBuffer[i] = this.charBuffer[this.bufferStart + i];
			this.bufferEnd -= this.bufferStart;
			this.bufferStart = 0;
		}
		while (this.bufferEnd < this.charBuffer.length) {
			int r = super.read();
			if (r == -1)
				break;
			this.charBuffer[this.bufferEnd++] = ((char) r);
		}
		if (this.bufferStart == this.bufferEnd)
			this.bufferEnd = -1;
		//	TODO_not consider using small char buffer instead of char-by-char reading
		//	==> looking inside BufferedReader, the single char read() method looks like the best choice
	}
	public boolean markSupported() {
		return false;
	}
	public void mark(int readAheadLimit) throws IOException {}
	public void reset() throws IOException {
		throw new IOException("mark/reset not supported, use peek() instead");
	}
	public int read() throws IOException {
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd == -1)
			return -1;
		return this.charBuffer[this.bufferStart++];
	}
	public int read(char[] cbuf, int off, int len) throws IOException {
		this.fillBuffer(this.lookahead + len);
		if (this.bufferEnd == -1)
			return -1;
		int read = 0;
		while (read < len) {
			if (this.bufferStart == this.bufferEnd)
				break;
			cbuf[off + read] = this.charBuffer[this.bufferStart++];
			read++;
		}
		return read;
	}
	public long skip(long n) throws IOException {
		this.fillBuffer((int) n + this.lookahead);
		if (this.bufferEnd == -1)
			return 0;
		int skip = Math.min((this.bufferEnd - this.bufferStart), ((int) n));
		this.bufferStart += skip;
		return skip;
	}
	void skipSpace() throws IOException {
		while ((this.peek() < 33) && (this.peek() != -1))
			this.read();
	}
	int peek() throws IOException {
		return this.peek(0);
	}
	int peek(int index) throws IOException {
		if (this.lookahead < index)
			return -1;
		this.fillBuffer(this.lookahead + index);
		if (this.bufferEnd <= (this.bufferStart + index))
			return -1;
		return this.charBuffer[this.bufferStart + index];
	}
	int peek(char[] cbuf) throws IOException {
		return this.peek(cbuf, 0, cbuf.length);
	}
	int peek(char[] cbuf, int off, int len) throws IOException {
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd == -1)
			return -1;
		int peek = 0;
		while (peek < len) {
			if ((this.bufferStart + peek) == this.bufferEnd)
				break;
			cbuf[off + peek] = this.charBuffer[this.bufferStart + peek];
			peek++;
		}
		return peek;
	}
	boolean startsWith(String prefix, boolean caseSensitive) throws IOException {
		return this.startsWith(prefix, caseSensitive, 0);
	}
	boolean startsWith(String prefix, boolean caseSensitive, int from) throws IOException {
		if (this.lookahead < (from + prefix.length()))
			return false;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd < (this.bufferStart + from + prefix.length()))
			return false;
		for (int c = 0; c < prefix.length(); c++) {
			if ((this.bufferStart + from + c) == this.bufferEnd)
				return false;
			if (caseSensitive ? (this.charBuffer[this.bufferStart + from + c] != prefix.charAt(c)) : (Character.toLowerCase(this.charBuffer[this.bufferStart + from + c]) != Character.toLowerCase(prefix.charAt(c))))
				return false;
		}
		return true;
	}
	int indexOf(char ch) throws IOException {
		return this.indexOf(ch, 0);
	}
	int indexOf(char ch, int from) throws IOException {
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd == -1)
			return -1;
		for (int i = from; i < (this.bufferEnd - this.bufferStart); i++) {
			if (this.charBuffer[this.bufferStart + i] == ch)
				return i;
		}
		return -1;
	}
	int indexOf(String infix) throws IOException {
		return this.indexOf(infix, 0);
	}
	int indexOf(String infix, int from) throws IOException {
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd == -1)
			return -1;
		if (infix.length() == 0)
			return from;
		if (this.lookahead < (from + infix.length()))
			return -1;
		char infixStartChar = infix.charAt(0);
		for (int i = from; i < (this.bufferEnd - this.bufferStart); i++) {
			if (this.charBuffer[this.bufferStart + i] != infixStartChar)
				continue;
			boolean match = true;
			for (int c = 1; c < infix.length(); c++) {
				if ((this.bufferStart + i + c) == this.bufferEnd)
					return -1;
				if (this.charBuffer[this.bufferStart + i + c] != infix.charAt(c)) {
					match = false;
					break;
				}
			}
			if (match)
				return i;
		}
		return -1;
	}
	int find(String pattern) throws IOException {
		return this.find(pattern, 0);
	}
	int find(String pattern, final int from) throws IOException {
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd == -1)
			return -1;
		if (this.lookahead < from)
			return -1;
		Matcher m = Pattern.compile(pattern).matcher(new CharSequence() {
			public int length() {
				return (bufferEnd - (bufferStart + from));
			}
			public char charAt(int index) {
				return charBuffer[bufferStart + from + index];
			}
			public CharSequence subSequence(int start, int end) {
				return new String(charBuffer, (bufferStart + from + start), (end - start));
			}
		});
		if (m.find())
			return m.start();
		else return -1;
	}
	
	private static final char NULLCHAR = '\u0000';
	
	static final String cropName(LookaheadReader charSource) throws IOException {
		StringBuffer name = new StringBuffer();
		while (charSource.peek() != -1) {
			if (Character.isLetter((char) charSource.peek()))
				name.append((char) charSource.read());
			else if ("_:".indexOf((char) charSource.peek()) != -1)
				name.append((char) charSource.read());
			else if (name.length() == 0)
				break;
			else if (Character.isDigit((char) charSource.peek()))
				name.append((char) charSource.read());
			else if ("-.".indexOf((char) charSource.peek()) != -1)
				name.append((char) charSource.read());
			else break;
		}
		return name.toString();
	}
	
	static final String cropAttributeValue(LookaheadReader charSource, Grammar grammar, String tagType, String attribName, char tagEnd, char endTagMarker) throws IOException {
		StringBuffer value = new StringBuffer();
		char quoter = NULLCHAR;
		
		//	read quoter, if any
		if (grammar.isTagAttributeValueQuoter((char) charSource.peek()))
			quoter = ((char) charSource.read());
		
		//	read value
		while (charSource.peek() != -1) {
			
			//	un-quoted value
			if (quoter == NULLCHAR) {
				
				//	tag end
				if (((charSource.peek() == tagEnd) || ((charSource.peek() == endTagMarker) && (charSource.peek(1) == tagEnd))))
					break;
				
				//	whitespace
				if (grammar.isWhitespace((char) charSource.peek()))
					break;
				
				//	un-opened quoter, consume it
				if (grammar.isTagAttributeValueQuoter((char) charSource.peek())) {
					if (grammar.correctErrors()) {
						charSource.read();
						break;
					}
					else throw new ParseException("Invalid character '" + ((char) charSource.peek()) + "'");
				}
			}
			
			//	end quoter, consume it and we're done
			if (charSource.peek() == quoter) {
				charSource.read();
				break;
			}
			
			//	whitespace in attribute value that may not contain it
			if (grammar.isWhitespace((char) charSource.peek()) && grammar.correctErrors() && !grammar.valueMayContainWhitespace(tagType, attribName))
				break;
			
			//	asymmetric quoter, consume it
			if (grammar.correctErrors() && grammar.isTagAttributeValueQuoter((char) charSource.peek()) && !grammar.valueMayContainWhitespace(tagType, attribName)) {
				charSource.read();
				break;
			}
			
			//	check encoding if activated
			if (grammar.correctCharEncoding()) {
				
				//	possible start of encoded character
				if (charSource.peek() == '&')
					value.append(cropCharCode(charSource, grammar));
				
				//	encode current character
				else value.append(grammar.getCharCode((char) charSource.read()));
			}
			
			//	other character
			else value.append((char) charSource.read());
		}
		return value.toString();
	}
	
	static final String cropCharCode(LookaheadReader charSource, Grammar grammar) throws IOException {
		StringBuffer charCode = new StringBuffer("&");
		
		//	check if we have a semicolon to terminate the character code
		int nextSemicolonIndex = charSource.indexOf(';');
		if ((nextSemicolonIndex != -1) && (nextSemicolonIndex < grammar.getCharLookahead())) {
			
			//	read remaining potential character code
			for (int i = 1; i <= nextSemicolonIndex; i++)
				charCode.append((char) charSource.peek(i));
			
			//	we do have a character code
			if (grammar.isCharCode(charCode.toString())) {
				charSource.skip(charCode.length());
				return charCode.toString();
			}
		}
		
		//	check if character code with missing semicolon
		else for (int i = 1; i < grammar.getCharLookahead(); i++) {
			if (charSource.peek(i) < 33)
				break;
			charCode.append((char) charSource.peek(i));
			if (charCode.length() < 3)
				continue; // we have at least the ampersand and two letters, or a hash tag or x and at least one digit
			String codePrefix = charCode.substring(0, 3);
			if (codePrefix.startsWith("&#x") || codePrefix.startsWith("&x")) {
				if ("0123456789abcdefABCDEF".indexOf((char) charSource.peek(i+1)) != -1)
					continue; // next char is valid hex digit
			}
			else if (codePrefix.startsWith("&#")) {
				 if ("0123456789".indexOf((char) charSource.peek(i+1)) != -1)
					 continue; // next char is valid decimal digit
			}
			else if (Character.isLetter((char) charSource.peek(i+1)))
				continue; // next char continues alphanumeric character code
			
			//	we do have a character code
			if (grammar.isCharCode(charCode.toString() + ';')) {
				charSource.skip(charCode.length());
				charCode.append(';');
				return charCode.toString();
			}
		}
		
		//	no character code found, escape ampersand
		return grammar.getCharCode((char) charSource.read());
	}
}