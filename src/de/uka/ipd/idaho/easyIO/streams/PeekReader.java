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
	private int lookahead;
	private int lookaheadChar = -1;
	private String lookaheadCache;
	
	/** Constructor
	 * @param in the input stream providing the data to read
	 * @param maxLookahead the number of bytes to buffer for lookahead
	 */
	public PeekReader(Reader in, int maxLookahead) throws IOException {
		super((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
		this.lookahead = maxLookahead;
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
		this.lookaheadCache = null;
		this.lookaheadChar = -1;
		return super.read();
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#read(char[], int, int)
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		this.lookaheadCache = null;
		this.lookaheadChar = -1;
		return super.read(cbuf, off, len);
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterReader#skip(long)
	 */
	public long skip(long n) throws IOException {
		this.lookaheadCache = null;
		this.lookaheadChar = -1;
		return super.skip(n);
	}
	
	/**
	 * Retrieve the next character from this input stream, but without
	 * consuming it.
	 * @return the next character
	 * @see java.io.Reader#read()
	 */
	public int peek() throws IOException {
		if (this.lookaheadCache != null)
			return ((int) this.lookaheadCache.charAt(0));
		else if (this.lookaheadChar != -1)
			return this.lookaheadChar;
		this.in.mark(1);
		this.lookaheadChar = this.in.read();
		this.in.reset();
		return this.lookaheadChar;
	}
	
	/**
	 * Retrieve up to <code>byte.length</code> characters from this input stream
	 * into a char array, but without consuming them. If the length of the
	 * argument array is greater than the maximum lookahead handed to the
	 * constructor, the latter is the limit.
	 * @see java.io.Reader#read(char[])
	 */
	public int peek(char[] cbuf) throws IOException {
		return this.peek(cbuf, 0, cbuf.length);
	}
	
	/**
	 * Retrieve up to <code>len</code> characters from this input stream into a
	 * char array, but without consuming them. If <code>len</code> is greater
	 * than the maximum lookahead handed to the constructor, the latter is the
	 * limit.
	 * @see java.io.Reader#read(char[], int, int)
	 */
	public int peek(char[] cbuf, int off, int len) throws IOException {
		len = Math.min((this.lookahead - off), len);
		int r = this.fillLookaheadCache();
		if (r == -1)
			return -1;
		int end = Math.min(r, (off+len));
		this.lookaheadCache.getChars(off, end, cbuf, off);
		return (end-off);
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
		if (this.lookahead < prefix.length())
			return false;
		this.fillLookaheadCache();
		if (caseSensitive)
			return this.lookaheadCache.startsWith(prefix);
		else return this.lookaheadCache.toLowerCase().startsWith(prefix.toLowerCase());
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
		int r = this.fillLookaheadCache();
		return ((r == -1) ? -1 : this.lookaheadCache.indexOf(ch));
	}
	
	/**
	 * Find the offset of the next character equaling the argument one. This
	 * method checks at most the lookahead length of characters, and thus the
	 * offset returned is always less than the lookahead. If the character is
	 * not found within this range, this method returns -1.
	 * @param infix the infix string to find
	 * @return the offset the argument character first occurs at, or -1 if it is
	 *         not found
	 * @throws IOException
	 */
	public int indexOf(String infix) throws IOException {
		if (infix.length() == 0)
			return 0;
		int r = this.fillLookaheadCache();
		return ((r == -1) ? -1 : this.lookaheadCache.indexOf(infix));
	}
	
	private int fillLookaheadCache() throws IOException {
		if ((this.lookaheadCache != null) && (this.lookaheadCache.length() == this.lookahead))
			return this.lookahead;
		this.in.mark(this.lookahead);
		char[] lBuffer = new char[this.lookahead];
		int r = this.in.read(lBuffer, 0, lBuffer.length);
		if (r != -1) {
			this.lookaheadCache = new String(lBuffer, 0, r);
			this.in.reset();
		}
		return r;
	}
}