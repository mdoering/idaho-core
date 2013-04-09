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


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;

import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * A TokenSource decomposes a stream of HTML/XML data into individual tokens. If
 * the Grammar specified to the constructor allows for error correction, the
 * token source can fix low level syntax errors like missing or erroneous
 * quotation of attributes.
 * 
 * @author sautter
 */
public class TokenSource {
	
	private static final char NULLCHAR = '\u0000';
	
	private final char tagStart;
	private final char tagEnd;
	private final char endTagMarker;
	private final String suitableTagStarts;
	private final char tagParameterSeparator;
	private final char tagParameterValueSeparator;
	private final String commentStartMarker;
	private final String commentEndMarker;
	private final String processingInstructionStartMarker;
	private final String processingInstructionEndMarker;
	private final String dtdStartMarker;
	private final String dtdEndMarker;
	
	private final boolean correctErrors;
	private final boolean correctCharEncoding;
	
	private int lookahead;
	private Vector tokens = new Vector();
	
	private CharSource source;
	private Grammar grammar;
	
	private TokenSource(CharSource source, Grammar grammar) throws IOException {
		this.source = source;
		this.grammar = grammar;
		
		//	get characters for tag recognition
		this.tagStart = this.grammar.getTagStart();
		this.tagEnd = this.grammar.getTagEnd();
		this.endTagMarker = this.grammar.getEndTagMarker();
		this.suitableTagStarts = this.grammar.getSuitableTagStarts().toLowerCase();
		this.tagParameterSeparator = this.grammar.getTagAttributeSeparator();
		this.tagParameterValueSeparator = this.grammar.getTagAttributeValueSeparator();
		this.correctErrors = this.grammar.correctErrors();
		this.correctCharEncoding = this.grammar.correctCharEncoding();
		
		//	get comment markers
		String marker;
		
		//	get comment start marker
		marker = this.grammar.getCommentStartMarker();
		this.commentStartMarker = (this.correctErrors ? marker.substring(0, (marker.length() - (marker.length() / 3))) : marker);
		
		//	get comment end marker
		marker = this.grammar.getCommentEndMarker();
		this.commentEndMarker = (this.correctErrors ? marker.substring(marker.length() / 3) : marker);
		
		//	get processing instruction start marker
		this.processingInstructionStartMarker = this.grammar.getProcessingInstructionStartMarker();
		
		//	get processing instruction end marker
		this.processingInstructionEndMarker = this.grammar.getProcessingInstructionEndMarker();
		
		//	get DTD start marker
		this.dtdStartMarker = this.grammar.getDtdStartMarker();
		
		//	get DTD end marker
		this.dtdEndMarker = this.grammar.getDtdEndMarker();
		
		//	get token lookahead required for error correction
		this.lookahead = this.grammar.getTokenLookahead() + 1;
		
		this.produceTokens();
	}
	
	/**	@return	true if and only if there are more tokens to be retrieved from this source
	 */
	public boolean hasMoreTokens() {
		return (this.source.hasMoreChars() || (this.tokens.size() != 0));
	}
	
	/**	@return	the first token in the queue
	 */
	public String retrieveToken() throws IOException {
		this.produceTokens();
		String token = ((this.tokens.size() > 0) ? ((String) this.tokens.remove(0)) : null);
		return token;
	}
	
	/**	refill the token queue
	 */
	private void produceTokens() throws IOException {
		String awaitedEndTag = null;
		
		//	refill buffer
		while (this.source.hasMoreChars() && ((this.tokens.size() < this.lookahead) || (awaitedEndTag != null))) {
			String token = this.produceToken((awaitedEndTag == null) ? null : ("" + this.tagStart + "" + this.endTagMarker + "" + awaitedEndTag + "" + this.tagEnd));//.trim();
			if (token.length() > 0) {
				
				//	tag token, might have to wait for end tag
				if (this.grammar.isTag(token)) {
					if ((awaitedEndTag != null) && this.grammar.isEndTag(token) && awaitedEndTag.equalsIgnoreCase(this.grammar.getType(token)))
						awaitedEndTag = null;
					
					else if (this.grammar.waitForEndTag(token))
						awaitedEndTag = this.grammar.getType(token);
				}
				
				//	store token
				this.tokens.addElement(token);
			}
		}
		
		//	check buffer
		this.grammar.ckeckTokenSequence(this.tokens);
	}
		
	/**	produce a single token
	 * @param	stop	if not null, the input is treated as a comment until the appearance of this String
	 * @return the next token
	 */
	private String produceToken(String stop) throws IOException {
			
		char lastAppendedChar = NULLCHAR;
		char currentChar;
		char nextChar;
		
		boolean inTag = false;
		
		boolean inQuotas = false;
		char quoter = NULLCHAR;
		
		boolean inComment = false;
		boolean inProcessingInstruction = false;
		boolean inDtd = false;
		
		StringBuffer assembler = new StringBuffer();
		
		String tagType = null;
		String currentParameterName = null;
		StringBuffer parameterNameAssembler = new StringBuffer();
		
		while (this.source.hasMoreChars()) {
			
			currentChar = this.source.getCharAt(0);
			nextChar = ((this.source.size() > 1) ? this.source.getCharAt(1) : NULLCHAR);
			
			//	waiting for some stop sequence
			if (stop != null) {
				
				//	stop sequence, return comment token
				if (this.source.startsWith(stop)) {
					if (assembler.length() != 0) return assembler.toString();
					stop = null;
				}
				
				//	unchecked sequence goes on
				else {
					assembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
				}
			}
			
			//	in comment
			else if (inComment) {
				
				//	end of comment, return comment token
				if (this.source.startsWith(this.commentEndMarker)) {
					assembler.append(this.commentEndMarker);
					this.source.consumeChars(this.commentEndMarker.length());
					return assembler.toString();
				}
				
				//	comment goes on
				else {
					assembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
				}
			}
			
			//	in quotas
			else if (inQuotas) {
				
				//	end of quotas
				if (currentChar == quoter) {
					quoter = NULLCHAR;
					inQuotas = false;
					assembler.append(currentChar);
					parameterNameAssembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
				}
				
				//	repair missing end quoter
				else if (this.correctErrors && this.grammar.isTagWhitespace(currentChar) && !this.grammar.valueMayContainWhitespace(tagType, currentParameterName)) {
					assembler.append(quoter);
					parameterNameAssembler.append(quoter);
					lastAppendedChar = quoter;
					quoter = NULLCHAR;
					inQuotas = false;
				}
				
				//	repair missing quoter at end of tag
				else if (this.correctErrors && (currentChar == this.tagEnd) && !this.grammar.valueMayContainWhitespace(tagType, currentParameterName)) {
					assembler.append(quoter);
					parameterNameAssembler.append(quoter);
					lastAppendedChar = quoter;
					quoter = NULLCHAR;
					inQuotas = false;
				}
				
				//	quoted part continues
				else {
					assembler.append(currentChar);
					parameterNameAssembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
				}
				
				//	if quotas ended, store parameter token
				if (!inQuotas && (parameterNameAssembler.length() != 0)) {
					parameterNameAssembler.delete(0, parameterNameAssembler.length());
				}
			}
			
			//	in processing instruction
			else if (inProcessingInstruction) {
				
				//	end of processing instruction, return comment token
				if (this.source.startsWith(this.processingInstructionEndMarker)) {
					assembler.append(this.processingInstructionEndMarker);
					this.source.consumeChars(this.processingInstructionEndMarker.length());
					return assembler.toString();
				}
				
				//	processing instruction goes on
				else {
					assembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
				}
			}
			
			//	in DTD
			else if (inDtd) {
				
				//	end of DTD, return comment token
				if (this.source.startsWith(this.dtdEndMarker)) {
					assembler.append(this.dtdEndMarker);
					this.source.consumeChars(this.dtdEndMarker.length());
					return assembler.toString();
				}
				
				//	DTD goes on
				else {
					assembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
				}
			}
			
			//	in tag
			else if (inTag) {
				
				//	probable start of quotas
				if (this.grammar.isTagAttributeValueQuoter(currentChar)) {
					
					//	quotas can begin only directly after a parameter name
					if (lastAppendedChar == this.tagParameterValueSeparator) {
						assembler.append(currentChar);
						lastAppendedChar = currentChar;
						inQuotas = true;
						quoter = currentChar;
					}
					this.source.consumeChar();
					
					//	store parameter token
					if (parameterNameAssembler.length() != 0) {
						currentParameterName = parameterNameAssembler.toString();
						parameterNameAssembler.delete(0, parameterNameAssembler.length());
					}
					if (inQuotas)
						parameterNameAssembler.append(currentChar);
				}
				
				//	end of tag, return tag token
				else if (currentChar == this.tagEnd) {
					assembler.append(currentChar);
					this.source.consumeChar();
					return (assembler.toString());
				}
				
				//	indent or line break within tag
				else if (this.grammar.isTagWhitespace(currentChar)) {
					if (!this.grammar.isTagWhitespace(lastAppendedChar) && (lastAppendedChar != this.tagParameterValueSeparator) && !this.grammar.isTagWhitespace(nextChar) && (nextChar != this.tagParameterValueSeparator)) {
						assembler.append(this.tagParameterSeparator);
						lastAppendedChar = this.tagParameterSeparator;
						
						//	store tag type or parameter token
						if (tagType == null) {
							tagType = this.grammar.getType(parameterNameAssembler.toString() + this.tagEnd);
							parameterNameAssembler.delete(0, parameterNameAssembler.length());
						}
						else if (parameterNameAssembler.length() != 0) {
							currentParameterName = parameterNameAssembler.toString();
							parameterNameAssembler.delete(0, parameterNameAssembler.length());
						}
					}
					this.source.consumeChar();
				}
				
				//	normal character within tag
				else {
					assembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
					
					//	store parameter if value to be expected
					if (currentChar == this.tagParameterValueSeparator) {
						if (parameterNameAssembler.length() != 0) {
							currentParameterName = parameterNameAssembler.toString();
							parameterNameAssembler.delete(0, parameterNameAssembler.length());
						}
					}
					else parameterNameAssembler.append(currentChar);
				}
			}
			
			//	base state
			else {
				
				//	start of comment, thus end of previous token, return previous token
				if (this.source.startsWith(this.commentStartMarker)) {
					if (assembler.length() > 0)
						return (assembler.toString());
					assembler.append(this.commentStartMarker);
					this.source.consumeChars(this.commentStartMarker.length());
					inComment = true;
				}
				
				//	start of DTD
				else if (this.source.startsWith(this.dtdStartMarker)) {
					if (assembler.length() != 0)
						return (assembler.toString());
					assembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
					inDtd = true;
				}
				
				//	start of processing instruction
				else if (this.source.startsWith(this.processingInstructionStartMarker)) {
					if (assembler.length() != 0)
						return (assembler.toString());
					assembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
					inProcessingInstruction = true;
				}
				
				//	start of tag, thus end of previous token, return previous token
				else if ((currentChar == this.tagStart) && (((this.source.size() > 0) && (this.suitableTagStarts.indexOf(("" + this.source.getCharAt(1)).toLowerCase()) != -1)) || ((this.source.size() > 1) && (this.source.getCharAt(1) == this.endTagMarker) && (this.suitableTagStarts.indexOf(("" + this.source.getCharAt(2)).toLowerCase()) != -1)))) {
					if (assembler.length() != 0)
						return (assembler.toString());
					assembler.append(currentChar);
					this.source.consumeChar();
					lastAppendedChar = currentChar;
					inTag = true;
				}
				
				//	ignore whitespace after line breaks (indents)
				else if (this.grammar.isWhitespace(currentChar)) {
					if ((lastAppendedChar != '\f') && (lastAppendedChar != '\n') && (lastAppendedChar != '\r') && (lastAppendedChar != '\t') && (lastAppendedChar != ' ')/* && (lastAppendedChar != NULLCHAR)*/) {
						if ((currentChar == '\r') || (currentChar == '\f'))
							currentChar = '\n';
						else if (currentChar == '\t')
							currentChar = ' ';
						assembler.append(currentChar);
						lastAppendedChar = currentChar;
					}
					this.source.consumeChar();
				}
				
				//	common character
				else {
					
					//	check encoding
					if (this.correctCharEncoding) {
						
						//	probable start of encoded character
						if (currentChar == '&') {
							StringBuffer codeAssembler = new StringBuffer();
							String code = null;
							int index = 0;
							
							//	check if encoded character
							while ((index < this.grammar.getCharLookahead()) && (code == null)) {
								codeAssembler.append(this.source.getCharAt(index));
								if (this.grammar.isCharCode(codeAssembler.toString()))
									code = codeAssembler.toString();
								index ++;
							}
							
							//	if encoded character identified, append code
							if (code != null) {
								assembler.append(code);
								this.source.consumeChars(code.length());
								lastAppendedChar = ';';
							}
							
							//	else append actual character
							else {
								assembler.append(this.grammar.getCharCode(currentChar));
								this.source.consumeChar();
								lastAppendedChar = currentChar;
							}
						}
						
						//	encode actual character and append char
						else {
							assembler.append(this.grammar.getCharCode(currentChar));
							this.source.consumeChar();
							lastAppendedChar = currentChar;
						} 
					}
					
					//	simply append char if no encoding correction
					else {
						assembler.append(currentChar);
						this.source.consumeChar();
						lastAppendedChar = currentChar;
					}
				}
			}
		}
		
		//	if source ends with broken tag, complete it
		if (quoter != NULLCHAR)
			assembler.append(quoter);
		if (inTag)
			assembler.append(this.tagEnd);
		
		//	if there is anything left in the assembler, return it
		return assembler.toString();
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
		return new TokenSource(new ReaderCharSource(grammar.getCharLookahead(), new StringReader(string)), grammar);
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
		return new TokenSource(new StreamCharSource(grammar.getCharLookahead(), stream), grammar);
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
		return new TokenSource(new ReaderCharSource(grammar.getCharLookahead(), stream), grammar);
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
	
	private abstract static class CharSource {
		
		protected int lookahead;
		protected char[] buffer;
		protected int bufferLevel = 0;
		protected int bufferStart = 0;
		protected int bufferEnd = 0;
		
		protected boolean inputFinished = false;
		
		/**	Constructor
		 * @param	lookahead	the minimum number of chars to be provided for lookahead while there are enough chars left in the source
		 * @throws	Exception	if any Exception occurs while setting up access to the source
		 */
		CharSource(int lookahead) throws IOException {
			this.lookahead = lookahead;
			this.buffer = new char[((this.lookahead > 0) ? this.lookahead : 1) + 1];
		}
		
		/**	@return	true if and only if there are chars left in the buffer to be retrieved
		 */
		boolean hasMoreChars() {
			return (!this.inputFinished || (this.bufferLevel != 0));
		}
		
		/**	@return	the first char in the buffer
		 */
		char consumeChar() throws IOException {
			char c = this.buffer[this.bufferStart];
			this.bufferStart ++;
			if (this.bufferStart == this.buffer.length) this.bufferStart = 0;
			this.bufferLevel --;
			this.fillBuffer();
			return c;
		}
		
		/**	@return	the first index chars in the buffer
		 */
		String consumeChars(int index) throws IOException {
			StringBuffer assembler = new StringBuffer(index);
			for (int i = 0; i < index; i++) assembler.append(this.consumeChar());
			return assembler.toString();
		}
		
		/**	add the specified char to the end of the buffer
		 * @param 	c	the char to be stored
		 */
		void storeChar(char c) {
			this.buffer[this.bufferEnd] = c;
			this.bufferEnd ++;
			if (this.bufferEnd == this.buffer.length) this.bufferEnd = 0;
			this.bufferLevel ++;
		}
		
		/**	check weather the buffer content starts with the specified prefix
		 * @param	prefix
		 * @return	true if and only if the first prefix.length() chars in the buffer match the specified prefix
		 */
		boolean startsWith(String prefix) {
			if ((prefix != null) && (prefix.length() <= this.bufferLevel)) {
				for (int i = 0; i < prefix.length(); i++) if (this.getCharAt(i) != prefix.charAt(i)) return false;
				return true;
			}
			else return false;
		}
		
		/**	inspect the char at the specified index without retrieving it
		 * @return	the char at the specified index modulo the buffer's size
		 */
		char getCharAt(int index) {
			int i = this.bufferStart + index;
			while (i >= this.buffer.length) i -= this.buffer.length;
			while (i < 0) i += this.buffer.length;
			return this.buffer[i];
		}
		
		/**	@return	the number of chars actually contained in the buffer
		 */
		int size() {
			return this.bufferLevel;
		}
		
		/**	fill the buffer at least up to the minimum lookahead level
		 * @throws Exception
		 */
		abstract void fillBuffer() throws IOException;
	}

	private static class StreamCharSource extends CharSource {
		
		private InputStream input;
		
		/**	Constructor
		 * @param	lookahead	the minimum number of chars to be provided for lookahead while there are enough chars left in the source
		 * @param	input		the InputStream to read the chars from
		 * @throws	Exception	if any Exception occurs while setting up access to the source
		 */
		StreamCharSource(int lookahead, InputStream input) throws IOException {
			super(lookahead);
			this.input = input;
			this.fillBuffer();
		}
		
		/**	@see	CharSource#fillBuffer()
		 */
		void fillBuffer() throws IOException {
			while ((this.bufferLevel < this.lookahead) && !this.inputFinished) {
				int i = this.input.read();
				if (i == -1)
					this.inputFinished = true;
				 else this.storeChar((char) i);
			}
		}
	}

	private static class ReaderCharSource extends CharSource {
		
		private Reader input;
		
		/**	Constructor
		 * @param	lookahead	the minimum number of chars to be provided for lookahead while there are enough chars left in the source
		 * @param	input		the Reader to read the chars from
		 * @throws	Exception	if any Exception occurs while setting up access to the source
		 */
		ReaderCharSource(int lookahead, Reader input) throws IOException {
			super(lookahead);
			this.input = input;
			this.fillBuffer();
		}
		
		/**	@see	CharSource#fillBuffer()
		 */
		void fillBuffer() throws IOException {
			while ((this.bufferLevel < this.lookahead) && !this.inputFinished) {
				int i = this.input.read();
				if (i == -1)
					this.inputFinished = true;
				else this.storeChar((char) i);
			}
		}
	}
//
//	private static class StringCharSource extends CharSource {
//		
//		private String input;
//		private int index = 0;
//		
//		/**	Constructor
//		 * @param	lookahead	the minimum number of chars to be provided for lookahead while there are enough chars left in the source
//		 * @param	input		the String to read the chars from
//		 * @throws	Exception	if any Exception occurs while setting up access to the source
//		 */
//		StringCharSource(int lookahead, String input) throws IOException {
//			super(lookahead);
//			this.input = input;
//			this.fillBuffer();
//		}
//		
//		/**	@see	CharSource#fillBuffer()
//		 */
//		void fillBuffer() throws IOException {
//			while ((this.bufferLevel < this.lookahead) && !this.inputFinished) {
//				char c = this.input.charAt(index);
//				this.storeChar(c);
//				this.index ++;
//				if (this.index == this.input.length()) this.inputFinished = true;
//			}
//		}
//	}
}
