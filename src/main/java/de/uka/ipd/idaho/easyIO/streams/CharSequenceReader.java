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

import java.io.IOException;
import java.io.Reader;

/**
 * Reader working on a char sequence. This class is a generalized version of a
 * StringReader that can work on arbitrary char sequences, preventing, for
 * instance, a StringBuffer from having to be converted into a String before
 * becoming suitable for stream style access. After embedding a char sequence in
 * an instance of this class, client code should not modify the char sequence
 * externally, except for appending to it.
 * 
 * @author sautter
 */
public class CharSequenceReader extends Reader {
	private CharSequence chars;
	private int length;
	private int offset = 0;
	private int mark = 0;
	
	/**
	 * Constructor
	 * @param chars the char sequence to wrap
	 */
	public CharSequenceReader(CharSequence chars) {
		this.chars = chars;
		this.length = this.chars.length();
	}
	/* (non-Javadoc)
	 * @see java.io.Reader#read()
	 */
	public int read() throws IOException {
		synchronized (this.lock) {
			ensureOpen();
			if (this.offset >= this.length)
				return -1;
			else return this.chars.charAt(this.offset++);
		}
	}
	/* (non-Javadoc)
	 * @see java.io.Reader#read(char[], int, int)
	 */
	public int read(char[] cbuf, int off, int len) throws IOException {
		synchronized (this.lock) {
			ensureOpen();
			if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0))
				throw new IndexOutOfBoundsException();
			else if (len == 0)
				return 0;
			else if (this.offset >= this.length)
				return -1;
			int readable = Math.min(this.length - this.offset, len);
			for (int r = 0; r < readable; r++)
				cbuf[off + r] = this.chars.charAt(this.offset++);
			return readable;
		}
	}
	/* (non-Javadoc)
	 * @see java.io.Reader#skip(long)
	 */
	public long skip(long ns) throws IOException {
		synchronized (this.lock) {
			ensureOpen();
			if (this.offset >= this.length)
				return 0;
			long skippable = Math.min(this.length - this.offset, ns);
			skippable = Math.max(-this.offset, skippable);
			this.offset += skippable;
			return skippable;
		}
	}
	/* (non-Javadoc)
	 * @see java.io.Reader#ready()
	 */
	public boolean ready() throws IOException {
		synchronized (this.lock) {
			ensureOpen();
			return true;
		}
	}
	/* (non-Javadoc)
	 * @see java.io.Reader#markSupported()
	 */
	public boolean markSupported() {
		return true;
	}
	/* (non-Javadoc)
	 * @see java.io.Reader#mark(int)
	 */
	public void mark(int readAheadLimit) throws IOException {
		if (readAheadLimit < 0)
			throw new IllegalArgumentException("Read-ahead limit < 0");
		synchronized (this.lock) {
			ensureOpen();
			this.mark = this.offset;
		}
	}
	/* (non-Javadoc)
	 * @see java.io.Reader#reset()
	 */
	public void reset() throws IOException {
		synchronized (this.lock) {
			ensureOpen();
			this.offset = this.mark;
		}
	}
	/* (non-Javadoc)
	 * @see java.io.Reader#close()
	 */
	public void close() throws IOException {
		this.chars = null;
	}
	
	private void ensureOpen() throws IOException {
		if (this.chars == null) throw new IOException("Stream closed");
	}
}
