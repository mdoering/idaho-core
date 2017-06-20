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
	private byte[] byteBuffer;
	private int bufferStart = 0;
	private int bufferEnd;
	private int lookahead;
	
	/** Constructor
	 * @param in the input stream providing the data to read
	 * @param lookahead the number of bytes to buffer for lookahead
	 */
	public PeekInputStream(InputStream in, int lookahead) throws IOException {
		super((in instanceof BufferedInputStream) ? ((BufferedInputStream) in) : new BufferedInputStream(in));
		this.lookahead = lookahead;
		this.byteBuffer = new byte[Math.max((this.lookahead * 2), 1024)];
	}
	
	private void fillBuffer(int min) throws IOException {
		if (this.bufferEnd == -1)
			return;
		if (min < (this.bufferEnd - this.bufferStart))
			return;
		if (this.bufferStart != 0) {
			for (int i = 0; i < (this.bufferEnd - this.bufferStart); i++)
				this.byteBuffer[i] = this.byteBuffer[this.bufferStart + i];
			this.bufferEnd -= this.bufferStart;
			this.bufferStart = 0;
		}
		while (this.bufferEnd < this.byteBuffer.length) {
			int r = super.read();
			if (r == -1)
				break;
			if (127 < r)
				r -= 256;
			this.byteBuffer[this.bufferEnd++] = ((byte) r);
		}
		if (this.bufferStart == this.bufferEnd)
			this.bufferEnd = -1;
		//	TODO_not consider using small byte buffer instead of byte-by-byte reading
		//	==> looking inside BufferedInputStream, the single byte read() method looks like the best choice
	}
	private int convertByte(int b) {
		return ((b < 0) ? (b + 256) : b);
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#available()
	 */
	public int available() throws IOException {
		return (super.available() + (this.bufferEnd - this.bufferStart));
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#close()
	 */
	public void close() throws IOException {
		super.close();
		this.bufferEnd = -1;
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
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#read()
	 */
	public int read() throws IOException {
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd <= this.bufferStart)
			return -1;
		return this.convertByte(this.byteBuffer[this.bufferStart++]);
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		this.fillBuffer(this.lookahead + len);
		if (this.bufferEnd <= this.bufferStart)
			return -1;
		int read = 0;
		while (read < len) {
			if (this.bufferStart == this.bufferEnd)
				break;
			b[off + read] = this.byteBuffer[this.bufferStart++];
			read++;
		}
		return read;
	}
	
	/* (non-Javadoc)
	 * @see java.io.FilterInputStream#skip(long)
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
	 * Skip all leading whitespace, i.e., all leading bytes whose value is 0x20
	 * or less. This method only stops if either reaches the first byte with a
	 * value greater than 0x20, or the end of the stream.
	 * @return the number of bytes skipped
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
	 * Retrieve the next byte from this input stream, but without
	 * consuming it.
	 * @return the next byte
	 * @see java.io.InputStream#read()
	 */
	public int peek() throws IOException {
		return this.peek(0);
	}
	
	/**
	 * Retrieve the index-th byte from the input stream, but without consuming
	 * it or any bytes before it. If there are fewer than
	 * <code>index</code> bytes left in the stream, this method returns
	 * -1.
	 * @param index the index of the byte to inspect
	 * @return the index-th byte
	 * @see java.io.InputStream#read()
	 */
	public int peek(int index) throws IOException {
		if (index < 0)
			return -1;
		if (this.lookahead < index)
			return -1;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd <= (this.bufferStart + index))
			return -1;
		return this.convertByte(this.byteBuffer[this.bufferStart + index]);
	}
	
	/**
	 * Retrieve up to <code>byte.length</code> bytes from the stream into an
	 * array, but without consuming them. If the length of the argument array
	 * is greater than the maximum lookahead handed to the constructor, the
	 * latter is the limit.
	 * @param b the buffer array to fill
	 * @return the number of bytes written into the argument array
	 * @see java.io.InputStream#read(byte[])
	 */
	public int peek(byte[] b) throws IOException {
		return this.peek(0, b, 0, b.length);
	}
	
	/**
	 * Retrieve up to <code>byte.length</code> bytes from the stream into an
	 * array, starting at the index-th byte, but without consuming them. If the
	 * length of the argument array is greater than the maximum lookahead
	 * handed to the constructor, the latter is the limit.
	 * @param index the index of the first byte to inspect
	 * @param b the buffer array to fill
	 * @return the number of bytes written into the argument array
	 * @see java.io.InputStream#read(byte[])
	 */
	public int peek(int index, byte[] b) throws IOException {
		return this.peek(index, b, 0, b.length);
	}
	
	/**
	 * Retrieve up to <code>len</code> bytes from the stream into an array, but
	 * without consuming them. If <code>len</code> is greater than the maximum
	 * lookahead handed to the constructor, the latter is the limit.
	 * @param b the buffer array to fill
	 * @param off the offset from which to fill the array.
	 * @param len the maximum number of bytes to read.
	 * @return the number of bytes written into the argument array
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int peek(byte[] b, int off, int len) throws IOException {
		return this.peek(0, b, off, len);
	}
	
	/**
	 * Retrieve up to <code>len</code> bytes from the stream into an array,
	 * starting at the index-th byte, but without consuming them. If
	 * <code>len</code> is greater than the maximum lookahead handed to the
	 * constructor, the latter is the limit.
	 * @param index the index of the first byte to inspect
	 * @param b the buffer array to fill
	 * @param off the offset from which to fill the array.
	 * @param len the maximum number of bytes to read.
	 * @return the number of bytes written into the argument array
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int peek(int index, byte[] b, int off, int len) throws IOException {
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
			b[off + peek] = this.byteBuffer[this.bufferStart + index + peek];
			peek++;
		}
		return peek;
	}
	
	/**
	 * Test if the next <code>prefix.length</code> bytes in this stream are the
	 * same as the argument prefix. If the argument prefix is longer than the
	 * maximum lookahead handed to the constructor, this method simply returns
	 * false.
	 * @param prefix the prefix to test
	 * @return true if if the next <code>prefix.length</code> bytes in the
	 *         stream are the same as the argument prefix
	 */
	public boolean startsWith(byte[] prefix) throws IOException {
		return this.startsWith(prefix, 0);
	}
	
	/**
	 * Test if the next <code>prefix.length</code> bytes, starting from
	 * <code>from</code>, in this stream are the same as the argument prefix.
	 * If the argument prefix is longer than the maximum lookahead handed to
	 * the constructor, this method simply returns false.
	 * @param prefix the prefix to test
	 * @param from the index to start the comparison from
	 * @return true if if the next <code>prefix.length</code> bytes in the
	 *         stream are the same as the argument prefix
	 */
	public boolean startsWith(byte[] prefix, int from) throws IOException {
		if (from < 0)
			return false;
		if ((this.lookahead - from) < prefix.length)
			return false;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd < (this.bufferStart + from + prefix.length))
			return false;
		for (int b = 0; b < prefix.length; b++) {
			if (this.byteBuffer[this.bufferStart + from + b] != prefix[b])
				return false;
		}
		return true;
	}
	
	/**
	 * Test if the next <code>prefix.length()</code> bytes in this stream are
	 * the same as the byte representation of the argument prefix in the
	 * argument encoding. If the argument prefix is longer than the maximum
	 * lookahead handed to the constructor, this method simply returns false.
	 * @param prefix the prefix to test
	 * @param encoding the character encoding to use
	 * @return true if if the next <code>prefix.length()</code> bytes in
	 *         this stream are the same as the argument prefix
	 */
	public boolean startsWith(String prefix, String encoding) throws IOException {
		return this.startsWith(prefix, encoding, 0);
	}
	
	/**
	 * Test if the next <code>prefix.length()</code> bytes, starting from
	 * <code>from</code>, in this stream are the same as the byte representation
	 * of the argument prefix in the argument encoding. If the argument prefix
	 * is longer than the maximum lookahead handed to the constructor, this
	 * method simply returns false.
	 * @param prefix the prefix to test
	 * @param encoding the character encoding to use
	 * @param from the index to start the comparison from
	 * @return true if if the next <code>prefix.length()</code> bytes in
	 *         this stream are the same as the argument prefix
	 */
	public boolean startsWith(String prefix, String encoding, int from) throws IOException {
		return this.startsWith(prefix.getBytes(encoding), from);
	}
	
	/**
	 * Find the offset of the next byte equaling the argument one. This method
	 * checks at most the lookahead length of bytes, and thus the offset
	 * returned is always less than the lookahead. If the byte is not found
	 * within this range, this method returns -1.
	 * @param b the byte to find
	 * @return the offset the argument byte first occurs at, or -1 if it is
	 *         not found
	 * @throws IOException
	 */
	public int indexOf(byte b) throws IOException {
		return this.indexOf(b, 0);
	}
	
	/**
	 * Find the offset of the next byte equaling the argument one. This method
	 * checks at most the lookahead length of bytes, and thus the offset
	 * returned is always less than the lookahead. If the byte is not found
	 * within this range, this method returns -1.
	 * @param b the byte to find
	 * @param from the index to start the search from
	 * @return the offset the argument byte first occurs at, or -1 if it is
	 *         not found after the argument index
	 * @throws IOException
	 */
	public int indexOf(byte b, int from) throws IOException {
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
			if (b == this.byteBuffer[this.bufferStart + s])
				return s;
		}
		return -1;
	}
	
	/**
	 * Find the offset of the next byte sequence equaling the argument one.
	 * This method checks at most the lookahead length of bytes, and thus the
	 * offset returned is always less than the lookahead. If the byte sequence
	 * is not found within this range, this method returns -1.
	 * @param infix the byte sequence to find
	 * @return the offset the argument byte sequence first occurs at, or -1 if
	 *         it is not found
	 * @throws IOException
	 */
	public int indexOf(byte[] infix) throws IOException {
		return this.indexOf(infix, 0);
	}
	
	/**
	 * Find the offset of the next byte sequence equaling the argument one.
	 * This method checks at most the lookahead length of bytes, and thus the
	 * offset returned is always less than the lookahead. If the byte sequence
	 * is not found within this range, this method returns -1.
	 * @param infix the byte sequence to find
	 * @param from the index to start the search from
	 * @return the offset the argument byte sequence first occurs at, or -1 if
	 *         it is not found after the argument index
	 * @throws IOException
	 */
	public int indexOf(byte[] infix, int from) throws IOException {
		if (from < 0)
			return -1;
		if ((this.lookahead - from) < infix.length)
			return -1;
		if (infix.length == 0)
			return from;
		this.fillBuffer(this.lookahead + 1);
		if (this.bufferEnd < (this.bufferStart + from + infix.length))
			return -1;
		for (int s = from; s < this.lookahead; s++) {
			if (this.bufferEnd <= (this.bufferStart + s))
				return -1;
			if (infix[0] != this.byteBuffer[this.bufferStart + s])
				continue;
			if (this.startsWith(infix, s))
				return s;
		}
		return -1;
	}
	
	/**
	 * Find the offset of the next bytes equaling the byte representation of
	 * the argument characters in the argument encoding. This method checks at
	 * most the lookahead length of bytes, and thus the offset returned is
	 * always less than the lookahead. If the bytes are not found within this
	 * range, this method returns -1.
	 * @param infix the infix string to find
	 * @param encoding the character encoding to use
	 * @return the offset the argument character first occurs at, or -1 if it
	 *         is not found
	 * @throws IOException
	 */
	public int indexOf(String infix, String encoding) throws IOException {
		return this.indexOf(infix, encoding, 0);
	}
	
	/**
	 * Find the offset of the next bytes equaling the byte representation of
	 * the argument characters in the argument encoding. This method checks at
	 * most the lookahead length of bytes, and thus the offset returned is
	 * always less than the lookahead. If the character is not found within
	 * this range, this method returns -1.
	 * @param infix the infix string to find
	 * @param encoding the character encoding to use
	 * @param from the index to start the search from
	 * @return the offset the argument character first occurs at, or -1 if it is
	 *         not found after the argument index
	 * @throws IOException
	 */
	public int indexOf(String infix, String encoding, int from) throws IOException {
		return this.indexOf(infix.getBytes(encoding), from);
	}
//	
//	//	!!! TEST ONLY !!!
//	public static void main(String[] args) throws Exception {
//		String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
//		PeekInputStream pis = new PeekInputStream(new ByteArrayInputStream(str.getBytes("UTF-8")), 26);
//		int infixLength = 1;
//		while (pis.peek() != -1) {
//			for (int c = 0; c <= (str.length() - infixLength); c++)
//				System.out.println(str.substring(c, (c+infixLength)) + " at " + pis.indexOf(str.substring(c, (c+infixLength)), "UTF-8"));
//			pis.peek(new byte[3]);
//			pis.read(new byte[3]);
//		}
//		pis.peek(new byte[3]);
//		pis.read(new byte[3]);
//		pis.close();
//	}
}