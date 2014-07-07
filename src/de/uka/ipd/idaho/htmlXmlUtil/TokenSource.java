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
package de.uka.ipd.idaho.htmlXmlUtil;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Vector;

import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * A TokenSource decomposes a stream of HTML/XML data into individual tokens.
 * If the grammar specified to the constructor allows for error correction, the
 * token source makes a best effort to fix low level syntax errors like missing
 * or erroneous quotation of attributes.
 * 
 * @author sautter
 */
public class TokenSource {
	
	private static final char NULLCHAR = '\u0000';
	
	private final char tagStart;
	private final char tagEnd;
	private final char endTagMarker;
	private final char tagAttributeSeparator;
	private final char tagAttributeValueSeparator;
	private final String commentStartMarker;
	private final String commentEndMarker;
	private final String processingInstructionStartMarker;
	private final String processingInstructionEndMarker;
	private final String dtdStartMarker;
	private final String dtdEndMarker;
	
	private final boolean correctErrors;
	private final boolean correctCharEncoding;
	
	private int tokenLookahead;
	private Vector tokenBuffer = new Vector();
	
	private LookaheadReader charSource;
	private Grammar grammar;
	
	private String skippedWhitespace = "";
	
	private TokenSource(LookaheadReader charSource, Grammar grammar) throws IOException {
		this.charSource = charSource;
		this.grammar = grammar;
		
		//	get characters for tag recognition
		this.tagStart = this.grammar.getTagStart();
		this.tagEnd = this.grammar.getTagEnd();
		this.endTagMarker = this.grammar.getEndTagMarker();
		this.tagAttributeSeparator = this.grammar.getTagAttributeSeparator();
		this.tagAttributeValueSeparator = this.grammar.getTagAttributeValueSeparator();
		this.correctErrors = this.grammar.correctErrors();
		this.correctCharEncoding = this.grammar.correctCharEncoding();
		
		//	get comment markers
		String csm = this.grammar.getCommentStartMarker();
		this.commentStartMarker = (this.correctErrors ? csm.substring(0, (csm.length() - (csm.length() / 3))) : csm);
		String cem = this.grammar.getCommentEndMarker();
		this.commentEndMarker = (this.correctErrors ? cem.substring(cem.length() / 3) : cem);
		
		//	get processing instruction markers
		this.processingInstructionStartMarker = this.grammar.getProcessingInstructionStartMarker();
		this.processingInstructionEndMarker = this.grammar.getProcessingInstructionEndMarker();
		
		//	get DTD markers
		this.dtdStartMarker = this.grammar.getDtdStartMarker();
		this.dtdEndMarker = this.grammar.getDtdEndMarker();
		
		//	get token lookahead required for error correction
		this.tokenLookahead = this.grammar.getTokenLookahead() + 1;
		
		this.produceTokens();
	}
	
	/**	@return	true if and only if there are more tokens to be retrieved from this source
	 */
	public boolean hasMoreTokens() {
		if (this.tokenBuffer.size() != 0)
			return true;
		try {
			return (this.charSource.peek() != -1);
		}
		catch (IOException ioe) {
			return false;
		}
	}
	
	/**	@return	the first token in the queue
	 */
	public String retrieveToken() throws IOException {
		this.produceTokens();
		return ((this.tokenBuffer.size() == 0) ? null : ((String) this.tokenBuffer.remove(0)));
	}
	
	private void produceTokens() throws IOException {
		String awaitedEndTag = null;
		
		//	refill buffer
		while ((this.charSource.peek() != -1) && ((this.tokenBuffer.size() < this.tokenLookahead) || (awaitedEndTag != null))) {
			String token = this.produceToken((awaitedEndTag == null) ? null : ("" + this.tagStart + "" + this.endTagMarker + "" + awaitedEndTag + "" + this.tagEnd));
			if (token.length() == 0)
				continue;
			
			//	tag token, might have to wait for end tag
			if (this.grammar.isTag(token)) {
				if ((awaitedEndTag != null) && this.grammar.isEndTag(token) && awaitedEndTag.equalsIgnoreCase(this.grammar.getType(token)))
					awaitedEndTag = null;
				else if (this.grammar.waitForEndTag(token))
					awaitedEndTag = this.grammar.getType(token);
			}
			
			//	store token
			this.tokenBuffer.addElement(token);
		}
		
		//	check buffer
		this.grammar.ckeckTokenSequence(this.tokenBuffer);
	}
	
	private String produceToken(String stopTag) throws IOException {
		
		//	end of input
		if (this.charSource.peek() == -1)
			return null;
		
		//	waiting for some end tag
		else if (stopTag != null)
			return this.cropUpTo(stopTag, false);
		
		//	comment
		else if (this.charSource.startsWith(this.commentStartMarker, false))
			return this.cropUpTo(this.commentEndMarker, true);
		
		//	DTD
		else if (this.charSource.startsWith(this.dtdStartMarker, false))
			return this.cropUpTo(this.dtdEndMarker, true);
		
		//	processing instruction
		else if (this.charSource.startsWith(this.processingInstructionStartMarker, false))
			return this.cropUpTo(this.processingInstructionEndMarker, true);
		
		//	tag, or data starting with tag start
		else if (this.charSource.peek() == this.tagStart) {
//			if (!this.correctErrors || !this.correctCharEncoding)
//				return this.cropTag();
			if (Character.isLetter((char) this.charSource.peek(1)))
				return this.cropTag();
			if ("_:".indexOf((char) this.charSource.peek(1)) != -1)
				return this.cropTag();
			if (this.charSource.peek(1) == this.endTagMarker)
				return this.cropTag();
			return this.cropData();
		}
		
		//	character data
		else return this.cropData();
	}
	
	private void skipWhitespace(boolean buffer) throws IOException {
		StringBuffer whitespace = new StringBuffer();
		
		//	crop space
		while ((this.charSource.peek() != -1) && this.grammar.isWhitespace((char) this.charSource.peek()))
			whitespace.append((char) this.charSource.read());
		
		//	buffer if asked to
		this.skippedWhitespace = (buffer ? whitespace.toString() : "");
	}
	
	private String cropUpTo(String stopSequence, boolean includeStopSequence) throws IOException {
		StringBuffer token = new StringBuffer();
		char stopChar = stopSequence.charAt(0);
		while (this.charSource.peek() != -1) {
			
			//	stop sequence reached
			if ((this.charSource.peek() == stopChar) && this.charSource.startsWith(stopSequence, false)) {
				
				//	handle stop sequence and buffer subsequent whitespace
				if (includeStopSequence || (token.length() == 0)) {
					for (int c = 0; c < stopSequence.length(); c++)
						token.append((char) this.charSource.read());
					this.skipWhitespace(true);
				}
				
				//	we're done here
				break;
			}
			
			//	other character
			else token.append((char) this.charSource.read());
		}
		
		//	finally ...
		return token.toString();
	}
	
	private String cropTag() throws IOException {
		StringBuffer tag = new StringBuffer();
		boolean gotEndTagMarker = false;
		tag.append((char) this.charSource.read());
		
		//	crop end tag marker
		if (this.charSource.peek() == this.endTagMarker) {
			tag.append((char) this.charSource.read());
			gotEndTagMarker = true;
		}
		
		//	crop qName
		String tagType = this.cropName();
		tag.append(tagType);
		this.skipWhitespace(false);
		
		//	crop attribute-value pairs
		while (this.charSource.peek() != -1) {
			this.skipWhitespace(false);
			
			//	end of attributes
			if ((this.charSource.peek() == this.tagEnd) || (this.charSource.peek() == this.endTagMarker))
				break;
//			System.out.println("Tag char: " + ((char) this.charSource.peek()) + " " + this.charSource.peek());
//			System.out.println("Tag part: " + tag.toString());
			
			//	read attribute name
			String attribName = this.cropName();
//			System.out.println("Attrib name: " + attribName);
			this.skipWhitespace(false);
			
			//	we have a value (tolerate missing separator if configured that way)
			String attribValue;
			if ((this.charSource.peek() == this.tagAttributeValueSeparator) || (this.correctErrors && this.grammar.isTagAttributeValueQuoter((char) this.charSource.peek()))) {
				if (this.charSource.peek() == this.tagAttributeValueSeparator)
					this.charSource.read();
				this.skipWhitespace(false);
				attribValue = this.cropAttributeValue(tagType, attribName);
//				System.out.println("Attrib value: " + attribValue);
			}
			
			//	we have a standalone attribute, substitute name for value
			else attribValue = attribName;
			
			//	append normalized attribute
			tag.append(this.tagAttributeSeparator + attribName + this.tagAttributeValueSeparator + this.grammar.getTagAttributeValueQuoter() + attribValue + this.grammar.getTagAttributeValueQuoter());
		}
		
		//	end tag marker at end of singular tag
		if (this.charSource.peek() == this.endTagMarker) {
			if (gotEndTagMarker)
				this.charSource.read();
			else tag.append((char) this.charSource.read());
		}
		
		//	tag end
		if (this.charSource.peek() == this.tagEnd)
			tag.append((char) this.charSource.read());
		
		//	buffer space after tag
		this.skipWhitespace(true);
		
		//	finally ...
//		System.out.println("Tag full: " + tag.toString());
		return tag.toString();
	}
	
	private String cropName() throws IOException {
		StringBuffer name = new StringBuffer();
		while (this.charSource.peek() != -1) {
			if (Character.isLetter((char) this.charSource.peek()))
				name.append((char) this.charSource.read());
			else if ("_:".indexOf((char) this.charSource.peek()) != -1)
				name.append((char) this.charSource.read());
			else if (name.length() == 0)
				break;
			else if (Character.isDigit((char) this.charSource.peek()))
				name.append((char) this.charSource.read());
			else if ("-.".indexOf((char) this.charSource.peek()) != -1)
				name.append((char) this.charSource.read());
			else break;
		}
		return name.toString();
	}
	
	private String cropAttributeValue(String tagType, String attribName) throws IOException {
		StringBuffer value = new StringBuffer();
		char quoter = NULLCHAR;
		
		//	read quoter, if any
		if (this.grammar.isTagAttributeValueQuoter((char) this.charSource.peek()))
			quoter = ((char) this.charSource.read());
		
		//	read value
		while (this.charSource.peek() != -1) {
			
			//	un-quoted value
			if (quoter == NULLCHAR) {
				
				//	tag end
				if (((this.charSource.peek() == this.tagEnd) || ((this.charSource.peek() == this.endTagMarker) && (this.charSource.peek(1) == this.tagEnd))))
					break;
				
				//	whitespace
				if (this.grammar.isWhitespace((char) this.charSource.peek()))
					break;
				
				//	un-opened quoter, consume it
				if (this.correctErrors && this.grammar.isTagAttributeValueQuoter((char) this.charSource.peek())) {
					this.charSource.read();
					break;
				}
			}
			
			//	end quoter, consume it and we're done
			if (this.charSource.peek() == quoter) {
				this.charSource.read();
				break;
			}
			
			//	whitespace in attribute value that may not contain it
			if (this.grammar.isWhitespace((char) this.charSource.peek()) && this.correctErrors && !this.grammar.valueMayContainWhitespace(tagType, attribName))
				break;
			
			//	asymmetric quoter, consume it
			if (this.correctErrors && this.grammar.isTagAttributeValueQuoter((char) this.charSource.peek()) && !this.grammar.valueMayContainWhitespace(tagType, attribName)) {
				this.charSource.read();
				break;
			}
			
			//	check encoding if activated
			if (this.correctCharEncoding) {
				
				//	possible start of encoded character
				if (this.charSource.peek() == '&')
					value.append(this.cropCharCode());
				
				//	encode current character
				else value.append(this.grammar.getCharCode((char) this.charSource.read()));
			}
			
			//	other character
			else value.append((char) this.charSource.read());
		}
		return value.toString();
	}
	
	private String cropData() throws IOException {
		StringBuffer data = new StringBuffer(this.skippedWhitespace);
		while (this.charSource.peek() != -1) {
			
			//	start of comment / end of data
			if (this.charSource.startsWith(this.commentStartMarker, false))
				break;
			
			//	start of DTD / end of data
			else if (this.charSource.startsWith(this.dtdStartMarker, false))
				break;
			
			//	start of processing instruction / end of data
			else if (this.charSource.startsWith(this.processingInstructionStartMarker, false))
				break;
			
			//	start of tag, or data starting with tag start / end of data
			else if (this.charSource.peek() == this.tagStart) {
//				if (!this.correctErrors)
//					break;
				if (Character.isLetter((char) this.charSource.peek(1)))
					break;
				if ("_:".indexOf((char) this.charSource.peek(1)) != -1)
					break;
				if (this.charSource.peek(1) == this.endTagMarker)
					break;
			}
			
			//	check encoding if activated
			if (this.correctCharEncoding) {
				
				//	possible start of encoded character
				if (this.charSource.peek() == '&')
					data.append(this.cropCharCode());
				
				//	encode current character
				else data.append(this.grammar.getCharCode((char) this.charSource.read()));
			}
			
			//	other character
			else data.append((char) this.charSource.read());
		}
		
		//	finally ...
		return data.toString();
	}
	
	private String cropCharCode() throws IOException {
		StringBuffer charCode = new StringBuffer("&");
		
		//	check if we have a semicolon to terminate the character code
		int nextSemicolonIndex = this.charSource.indexOf(';');
		if ((nextSemicolonIndex != -1) && (nextSemicolonIndex < this.grammar.getCharLookahead())) {
			
			//	read remaining potential character code
			for (int i = 1; i <= nextSemicolonIndex; i++)
				charCode.append((char) this.charSource.peek(i));
			
			//	we do have a character code
			if (this.grammar.isCharCode(charCode.toString())) {
				this.charSource.skip(charCode.length());
				return charCode.toString();
			}
		}
		
		//	check if character code with missing semicolon
		else for (int i = 1; i < this.grammar.getCharLookahead(); i++) {
			if (this.charSource.peek(i) < 33)
				break;
			charCode.append((char) this.charSource.peek(i));
			if (charCode.length() < 3)
				continue; // we have at least the ampersand and two letters, or a hash tag or x and at least one digit
			String codePrefix = charCode.substring(0, 3);
			if (codePrefix.startsWith("&#x") || codePrefix.startsWith("&x")) {
				if ("0123456789abcdefABCDEF".indexOf((char) this.charSource.peek(i+1)) != -1)
					continue; // next char is valid hex digit
			}
			else if (codePrefix.startsWith("&#")) {
				 if ("0123456789".indexOf((char) this.charSource.peek(i+1)) != -1)
					 continue; // next char is valid decimal digit
			}
			else if (Character.isLetter((char) this.charSource.peek(i+1)))
				continue; // next char continues alphanumeric character code
			
			//	we do have a character code
			if (this.grammar.isCharCode(charCode.toString() + ';')) {
				this.charSource.skip(charCode.length());
				charCode.append(';');
				return charCode.toString();
			}
		}
		
		//	no character code found, escape ampersand
		return this.grammar.getCharCode((char) this.charSource.read());
	}
	
	/**	create a TokenSource providing tokens parsed from a String in the context of the StandardGrammar 
	 * @param 	string	the String to parse the tokens from
	 * @return	a TokenSource providing tokens parsed from the specified String in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(String string) throws IOException {
		return getTokenSource(string, null);
	}
	/**	create a TokenSource providing tokens parsed from a String in the context of the specified Grammar 
	 * @param 	string	the String to parse the tokens from
	 * @param 	grammar	the Grammar that's context to parse the tokens in
	 * @return	a TokenSource providing tokens parsed from the specified String in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(String string, Grammar grammar) throws IOException {
		if (grammar == null)
			grammar = new StandardGrammar(); 
		return new TokenSource(new LookaheadReader(new StringReader(string), (grammar.getCharLookahead()+1)), grammar);
	}
	
	/**	create a TokenSource providing tokens parsed from an InputStream in the context of the StandradGrammar 
	 * @param 	stream	the InputStream to parse the tokens from
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(InputStream stream) throws IOException {
		return getTokenSource(stream, null);
	}
	/**	create a TokenSource providing tokens parsed from an InputStream in the context of the specified Grammar 
	 * @param 	stream	the InputStream to parse the tokens from
	 * @param 	grammar	the Grammar that's context to parse the tokens in
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(InputStream stream, Grammar grammar) throws IOException {
		if (grammar == null)
			grammar = new StandardGrammar(); 
		return new TokenSource(new LookaheadReader(new InputStreamReader(stream/*, TODO guessEncoding()*/), (grammar.getCharLookahead()+1)), grammar);
	}
	
	/**	create a TokenSource providing tokens parsed from a Reader in the context of the StandradGrammar 
	 * @param 	stream	the Reader to parse the tokens from
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(Reader stream) throws IOException {
		return getTokenSource(stream, null);
	}
	/**	create a TokenSource providing tokens parsed from a Reader in the context of the specified Grammar 
	 * @param 	stream	the Reader to parse the tokens from
	 * @param 	grammar	the Grammar that's context to parse the tokens in
	 * @return	a TokenSource providing tokens parsed from the specified Reader in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(Reader stream, Grammar grammar) throws IOException {
		if (grammar == null)
			grammar = new StandardGrammar(); 
		return new TokenSource(new LookaheadReader(stream, (grammar.getCharLookahead()+1)), grammar);
	}
	
	/**	create a TokenSource providing tokens parsed from an InputStream in the context of the StandardGrammar 
	 * @param 	file	the File from that's content the tokens are to be parsed
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(File file) throws IOException {
		return getTokenSource(file, null);
	}
	/**	create a TokenSource providing tokens parsed from an InputStream in the context of the specified Grammar 
	 * @param 	file	the File from that's content the tokens are to be parsed
	 * @param 	grammar	the Grammar that's context to parse the tokens in
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(File file, Grammar grammar) throws IOException {
		return getTokenSource(new FileInputStream(file), grammar);
	}
	
	private static class LookaheadReader extends FilterReader {
		private char[] charBuffer;
		private int bufferStart = 0;
		private int bufferEnd;
		private int lookahead;
		LookaheadReader(Reader in, int lookahead) throws IOException {
			super((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			this.lookahead = lookahead;
			this.charBuffer = new char[Math.max((this.lookahead * 2), 1024)];
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
//		void skipSpace() throws IOException {
//			while ((this.peek() < 33) && (this.peek() != -1))
//				this.read();
//		}
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
//		int peek(char[] cbuf) throws IOException {
//			return this.peek(cbuf, 0, cbuf.length);
//		}
//		int peek(char[] cbuf, int off, int len) throws IOException {
//			this.fillBuffer(this.lookahead + 1);
//			if (this.bufferEnd == -1)
//				return -1;
//			int peek = 0;
//			while (peek < len) {
//				if ((this.bufferStart + peek) == this.bufferEnd)
//					break;
//				cbuf[off + peek] = this.charBuffer[this.bufferStart + peek];
//				peek++;
//			}
//			return peek;
//		}
		boolean startsWith(String prefix, boolean caseSensitive) throws IOException {
			if (this.lookahead < prefix.length())
				return false;
			this.fillBuffer(this.lookahead + 1);
			if (this.bufferEnd < (this.bufferStart + prefix.length()))
				return false;
			for (int c = 0; c < prefix.length(); c++) {
				if ((this.bufferStart + c) == this.bufferEnd)
					return false;
				if (caseSensitive ? (this.charBuffer[this.bufferStart + c] != prefix.charAt(c)) : (Character.toLowerCase(this.charBuffer[this.bufferStart + c]) != Character.toLowerCase(prefix.charAt(c))))
					return false;
//				if (caseSensitive) {
//					if (this.charBuffer[this.bufferStart + c] != prefix.charAt(c))
//						return false;
//				}
//				else {
//					if (Character.toLowerCase(this.charBuffer[this.bufferStart + c]) != Character.toLowerCase(prefix.charAt(c)))
//						return false;
//				}
			}
			return true;
		}
		int indexOf(char ch) throws IOException {
			this.fillBuffer(this.lookahead + 1);
			if (this.bufferEnd == -1)
				return -1;
			for (int i = 0; i < (this.bufferEnd - this.bufferStart); i++) {
				if (this.charBuffer[this.bufferStart + i] == ch)
					return i;
			}
			return -1;
		}
//		int indexOf(String infix) throws IOException {
//			this.fillBuffer(this.lookahead + 1);
//			if (this.bufferEnd == -1)
//				return -1;
//			if (infix.length() == 0)
//				return 0;
//			if (this.lookahead < infix.length())
//				return -1;
//			char infixStartChar = infix.charAt(0);
//			for (int i = 0; i < (this.bufferEnd - this.bufferStart); i++) {
//				if (this.charBuffer[this.bufferStart + i] != infixStartChar)
//					continue;
//				boolean match = true;
//				for (int c = 1; c < infix.length(); c++) {
//					if ((this.bufferStart + i + c) == this.bufferEnd)
//						return -1;
//					if (this.charBuffer[this.bufferStart + i + c] != infix.charAt(c)) {
//						match = false;
//						break;
//					}
//				}
//				if (match)
//					return i;
//			}
//			return -1;
//		}
//		private int fillLookaheadCache() throws IOException {
//			if (this.lookahead == -1)
//				return -1;
//			if ((this.lookaheadCache != null) && (this.lookaheadCache.length() == this.lookahead))
//				return this.lookahead;
//			this.in.mark(this.lookahead);
//			char[] lBuffer = new char[this.lookahead];
//			int read = 0;
//			while (read < this.lookahead) {
//				int r = this.in.read(lBuffer, read, (lBuffer.length-read));
//				if (r == -1)
//					break;
//				read += r;
//			}
//			if (read == 0) {
//				this.lookahead = -1;
//				return -1;
//			}
//			this.lookaheadCache = new String(lBuffer, 0, read);
//			this.in.reset();
//			return read;
//		}
	}
	
	public static void main(String[] args) throws Exception {
//		String html = "<?xml screw=it?><!element is><!element it><!element now><html>\r\n  <head><script>some & crap < with --> bad <!-- char > sequences</script></head>\r\n  <body test=test with space test2=with' width='100% hight=100%\">  Test  </body>\r\n<!-- comment -></html>";
//		TokenSource ts = getTokenSource(html, new Html());
//		while (ts.hasMoreTokens())
//			System.out.println("Token: '" + ts.retrieveToken() + "'");
		TokenSource ts = getTokenSource((new URL("http://www.plantsystematics.org/taxpage/0/genus/Agave.html").openStream()), new Html());
		while (ts.hasMoreTokens())
			System.out.println("Token: '" + ts.retrieveToken() + "'");
		
	}
}