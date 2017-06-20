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
package de.uka.ipd.idaho.easyIO.streams;

import java.io.BufferedReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Reader that allows for inspecting the next few chars of data without
 * consuming them. Instances of this class uses a buffered reader internally,
 * using its mark() and reset() methods to implement their peek() methods.
 * Instances of this class do not support marking and resetting themselves.
 * 
 * @author sautter
 */
public class PeekReader extends FilterReader {
	private char[] charBuffer;
	private int bufferStart = 0;
	private int bufferEnd;
	private int lookahead;
	
	/** Constructor
	 * @param in the input stream providing the data to read
	 * @param lookahead the number of bytes to buffer for lookahead
	 */
	public PeekReader(Reader in, int lookahead) throws IOException {
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
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#ready()
	 */
	public boolean ready() throws IOException {
		return (this.peek() != -1);
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#close()
	 */
	public void close() throws IOException {
		super.close();
		this.bufferEnd = -1;
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#markSupported()
	 */
	public boolean markSupported() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#mark(int)
	 */
	public void mark(int readAheadLimit) throws IOException {}
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#reset()
	 */
	public void reset() throws IOException {
		throw new IOException("mark/reset not supported, use peek() instead");
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#read()
	 */
	public int read() throws IOException {
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd <= this.bufferStart)
			return -1;
		return this.charBuffer[this.bufferStart++];
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#read(char[], int, int)
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		this.fillBuffer(this.lookahead + len);
		if (this.bufferEnd <= this.bufferStart)
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
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#skip(long)
	 */
	public long skip(long n) throws IOException {
		this.fillBuffer((int) n + this.lookahead);
		if (this.bufferEnd <= this.bufferStart)
			return 0;
		int skip = Math.min((this.bufferEnd - this.bufferStart), ((int) n));
		this.bufferStart += skip;
		return skip;
	}
	
	/**
	 * Skip all leading whitespace, i.e., all leading characters whose byte
	 * value is 0x20 or less. This method only stops if either reaches the
	 * first char with a byte value greater than 0x20, or the end of the
	 * stream.
	 * @return the number of characters skipped
	 * @throws IOException
	 */
	public int skipSpace() throws IOException {
		int skipped = 0;
		while ((this.peek() < 33) && (this.peek() != -1)) {
			this.read();
			skipped++;
		}
		return skipped;
	}
	
	/**
	 * Retrieve the next character from the stream, but without consuming it.
	 * @return the next character
	 * @see java.io.Reader#read()
	 */
	public int peek() throws IOException {
		return this.peek(0);
	}
	
	/**
	 * Retrieve the index-th character from the stream, but without consuming
	 * it or any characters before it. If there are fewer than
	 * <code>index</code> characters left in the stream, this method returns
	 * -1.
	 * @param index the index of the character to inspect
	 * @return the index-th character
	 * @see java.io.Reader#read()
	 */
	public int peek(int index) throws IOException {
		if (index < 0)
			return -1;
		if (this.lookahead < index)
			return -1;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd <= (this.bufferStart + index))
			return -1;
		return this.charBuffer[this.bufferStart + index];
	}
	
	/**
	 * Retrieve up to <code>byte.length</code> characters from the stream into
	 * a char array, but without consuming them. If the length of the argument
	 * array is greater than the maximum lookahead handed to the constructor,
	 * the latter is the limit.
	 * @param cbuf the buffer to fill
	 * @return the number of characters written into the argument array
	 * @see java.io.Reader#read(char[])
	 */
	public int peek(char[] cbuf) throws IOException {
		return this.peek(0, cbuf, 0, cbuf.length);
	}
	
	/**
	 * Retrieve up to <code>byte.length</code> characters from the stream into
	 * a char array, starting at the index-th character, but without consuming
	 * them. If the length of the argument array is greater than the maximum
	 * lookahead handed to the constructor, the latter is the limit.
	 * @param index the index of the first character to inspect
	 * @param cbuf the buffer to fill
	 * @return the number of characters written into the argument array
	 * @see java.io.Reader#read(char[])
	 */
	public int peek(int index, char[] cbuf) throws IOException {
		return this.peek(index, cbuf, 0, cbuf.length);
	}
	
	/**
	 * Retrieve up to <code>len</code> characters from the stream into a char
	 * array, but without consuming them. If <code>len</code> is greater than
	 * the maximum lookahead handed to the constructor, the latter is the
	 * limit.
	 * @param cbuf the buffer to fill
	 * @param off the offset at which to start storing characters
	 * @param len the maximum number of characters to read
	 * @return the number of characters written into the argument array
	 * @see java.io.Reader#read(char[], int, int)
	 */
	public int peek(char[] cbuf, int off, int len) throws IOException {
		return this.peek(0, cbuf, off, len);
	}
	
	/**
	 * Retrieve up to <code>len</code> characters from the stream into a char
	 * array, starting at the index-th character, but without consuming them.
	 * If <code>len</code> is greater than the maximum lookahead handed to the
	 * constructor, the latter is the limit.
	 * @param index the index of the first character to inspect
	 * @param cbuf the buffer to fill
	 * @param off the offset at which to start storing characters
	 * @param len the maximum number of characters to read
	 * @return the number of characters written into the argument array
	 * @see java.io.Reader#read(char[], int, int)
	 */
	public int peek(int index, char[] cbuf, int off, int len) throws IOException {
		if (index < 0)
			return -1;
		len = Math.min((this.lookahead - index), len);
		if (len < 1)
			return -1;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd <= (this.bufferStart + index))
			return -1;
		int peek = 0;
		while (peek < len) {
			if ((this.bufferStart + index + peek) == this.bufferEnd)
				break;
			cbuf[off + peek] = this.charBuffer[this.bufferStart + index + peek];
			peek++;
		}
		return peek;
	}
	
	/**
	 * Test if the next <code>prefix.length()</code> characters in this stream
	 * are the same as the argument prefix. If the argument prefix is longer
	 * than the maximum lookahead handed to the constructor, this method simply
	 * returns false.
	 * @param prefix the prefix to test
	 * @param caseSensitive perform case sensitive comparison?
	 * @return true if if the next <code>prefix.length()</code> characters in
	 *         this stream are the same as the argument prefix
	 */
	public boolean startsWith(String prefix, boolean caseSensitive) throws IOException {
		return this.startsWith(prefix, caseSensitive, 0);
	}
	
	/**
	 * Test if the next <code>prefix.length()</code> characters, starting from
	 * <code>from</code>, in this stream are the same as the argument prefix.
	 * If the argument prefix is longer than the maximum lookahead handed to
	 * the constructor, this method simply returns false.
	 * @param prefix the prefix to test
	 * @param caseSensitive perform case sensitive comparison?
	 * @param from the index to start the comparison from
	 * @return true if if the next <code>prefix.length()</code> characters in
	 *         this stream are the same as the argument prefix
	 */
	public boolean startsWith(String prefix, boolean caseSensitive, int from) throws IOException {
		if (from < 0)
			return false;
		if ((this.lookahead - from) < prefix.length())
			return false;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd < (this.bufferStart + from + prefix.length()))
			return false;
		for (int c = 0; c < prefix.length(); c++) {
			if (caseSensitive ? (this.charBuffer[this.bufferStart + from + c] != prefix.charAt(c)) : (Character.toLowerCase(this.charBuffer[this.bufferStart + from + c]) != Character.toLowerCase(prefix.charAt(c))))
				return false;
		}
		return true;
	}
	
	/**
	 * Find the offset of the next character equaling the argument one. This
	 * method checks at most the lookahead length of characters, and thus the
	 * offset returned is always less than the lookahead. If the character is
	 * not found within this range, this method returns -1.
	 * @param ch the character to find
	 * @return the offset the argument character first occurs at, or -1 if it is
	 *         not found
	 * @throws IOException
	 */
	public int indexOf(char ch) throws IOException {
		return this.indexOf(ch, 0);
	}
	
	/**
	 * Find the offset of the next character equaling the argument one. This
	 * method checks at most the lookahead length of characters, and thus the
	 * offset returned is always less than the lookahead. If the character is
	 * not found within this range, this method returns -1.
	 * @param ch the character to find
	 * @param from the index to start the search from
	 * @return the offset the argument character first occurs at, or -1 if it is
	 *         not found
	 * @throws IOException
	 */
	public int indexOf(char ch, int from) throws IOException {
		if (from < 0)
			return -1;
		if (this.lookahead <= from)
			return -1;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd <= (this.bufferStart + from))
			return -1;
		for (int s = from; s < this.lookahead; s++) {
			if (this.bufferEnd <= (this.bufferStart + s))
				return -1;
			if (ch == this.charBuffer[this.bufferStart + s])
				return s;
		}
		return -1;
	}
	
	/**
	 * Find the offset of the next character sequence equaling the argument
	 * one. This method checks at most the lookahead length of characters, and
	 * thus the offset returned is always less than the lookahead. If the
	 * character is not found within this range, this method returns -1.
	 * @param infix the infix string to find
	 * @param caseSensitive perform case sensitive comparison?
	 * @return the offset the argument character sequence first occurs at, or
	 *         -1 if it is not found after the argument index
	 * @throws IOException
	 */
	public int indexOf(String infix, boolean caseSensitive) throws IOException {
		return this.indexOf(infix, caseSensitive, 0);
	}
	
	/**
	 * Find the offset of the next character sequence equaling the argument
	 * one. This method checks at most the lookahead length of characters, and
	 * thus the offset returned is always less than the lookahead. If the
	 * character is not found within this range, this method returns -1.
	 * @param infix the infix string to find
	 * @param caseSensitive perform case sensitive comparison?
	 * @param from the index to start the search from
	 * @return the offset the argument character sequence first occurs at, or
	 *         -1 if it is not found after the argument index
	 * @throws IOException
	 */
	public int indexOf(String infix, boolean caseSensitive, int from) throws IOException {
		if (from < 0)
			return -1;
		if ((this.lookahead - from) < infix.length())
			return -1;
		if (infix.length() == 0)
			return from;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd < (this.bufferStart + from + infix.length()))
			return -1;
		for (int s = from; s < this.lookahead; s++) {
			if (this.bufferEnd <= (this.bufferStart + s))
				return -1;
			if (caseSensitive ? (infix.charAt(0) != this.charBuffer[this.bufferStart + s]) : (Character.toLowerCase(infix.charAt(0)) != Character.toLowerCase(this.charBuffer[this.bufferStart + s])))
				continue;
			if (this.startsWith(infix, caseSensitive, s))
				return s;
		}
		return -1;
	}
//	
//	//	!!! TEST ONLY !!!
//	public static void main(String[] args) throws Exception {
//		String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
//		PeekReader pr = new PeekReader(new StringReader(str), 26);
//		int infixLength = 4;
//		while (pr.peek() != -1) {
//			for (int c = 0; c <= (str.length() - infixLength); c++) {
//				System.out.println(str.substring(c, (c+infixLength)) + " at " + pr.indexOf(str.substring(c, (c+infixLength)), true) + " cs");
//				System.out.println(str.substring(c, (c+infixLength)) + " at " + pr.indexOf(str.substring(c, (c+infixLength)), false) + " cis");
//			}
//			pr.peek(new char[3]);
//			pr.read(new char[3]);
//		}
//		pr.peek(new char[3]);
//		pr.read(new char[3]);
//		pr.close();
//	}
}