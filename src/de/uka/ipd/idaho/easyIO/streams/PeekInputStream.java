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

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Input stream that allows for inspecting the next few bytes of data without
 * consuming them. Instances of this class uses a buffered input stream
 * internally, using its mark() and reset() methods to implement their peek()
 * methods. Instances of this class do not support marking and resetting
 * themselves.
 * 
 * @author sautter
 */
public class PeekInputStream extends FilterInputStream {
	private int lookahead;
	
	/** Constructor
	 * @param in the input stream providing the data to read
	 * @param maxLookahead the number of bytes to buffer for lookahead
	 */
	public PeekInputStream(InputStream in, int maxLookahead) throws IOException {
		super((in instanceof BufferedInputStream) ? ((BufferedInputStream) in) : new BufferedInputStream(in));
		this.lookahead = maxLookahead;
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#mark(int)
	 */
	public synchronized void mark(int readlimit) {}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#reset()
	 */
	public synchronized void reset() throws IOException {
		throw new IOException("mark/reset not supported, use peek() instead");
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#markSupported()
	 */
	public boolean markSupported() {
		return false;
	}
	
	/**
	 * Retrieve the next byte of data from this input stream, but without
	 * consuming it.
	 * @return the next byte of data
	 * @see java.io.InputStream#read()
	 */
	public int peek() throws IOException {
		this.in.mark(1);
		int r = this.read();
		this.in.reset();
		return r;
	}
	
	/**
	 * Retrieve up to <code>byte.length</code> bytes of data from this input
	 * stream into an array of bytes, but without consuming them. If the length
	 * of the argument array is greater than the maximum lookahead handed to the
	 * constructor, the latter is the limit.
	 * @see java.io.InputStream#read(byte[])
	 */
	public int peek(byte[] b) throws IOException {
		return this.peek(b, 0, b.length);
	}
	
	/**
	 * Retrieve up to <code>len</code> bytes of data from this input stream into
	 * an array of bytes, but without consuming them. If <code>len</code> is
	 * greater than the maximum lookahead handed to the constructor, the latter
	 * is the limit.
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int peek(byte[] b, int off, int len) throws IOException {
		len = Math.min(this.lookahead, len);
		this.in.mark(len);
		int r = this.read(b, off, len);
		this.in.reset();
		return r;
	}
}
