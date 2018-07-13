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
package de.uka.ipd.idaho.easyIO.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class encapsulates another <code>InputStream</code>. However, the
 * latter does not have to be specified as a constructor argument, but is
 * rather provided via the abstract <code>getInputStream()</code> method. This
 * method is only called upon the first read, only opening the encapsulated
 * input stream upon first use.
 * 
 * @author sautter
 */
public abstract class OnDemandInputStream extends InputStream {
	private InputStream in = null;
	
	/** Constructor
	 */
	public OnDemandInputStream() {}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read()
	 */
	public int read() throws IOException {
		if (this.in == null)
			this.in = this.getInputStream();
		return this.in.read();
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		if (this.in == null)
			this.in = this.getInputStream();
		return this.in.read(b, off, len);
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#skip(long)
	 */
	public long skip(long n) throws IOException {
		if (this.in == null)
			this.in = this.getInputStream();
		return this.in.skip(n);
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#available()
	 */
	public int available() throws IOException {
		if (this.in == null)
			this.in = this.getInputStream();
		return this.in.available();
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#close()
	 */
	public void close() throws IOException {
		if (this.in != null)
			this.in.close();
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#markSupported()
	 */
	public boolean markSupported() {
		if (this.in == null) try {
			this.in = this.getInputStream();
		} catch (IOException ioe) {}
		return ((this.in != null) && this.in.markSupported());
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#mark(int)
	 */
	public synchronized void mark(int readlimit) {
		if (this.in == null) try {
			this.in = this.getInputStream();
		} catch (IOException ioe) {}
		if (this.in != null)
			this.in.mark(readlimit);
	}
	
	/* (non-Javadoc)
	 * @see java.io.InputStream#reset()
	 */
	public synchronized void reset() throws IOException {
		if (this.in == null)
			this.in = this.getInputStream();
		this.in.reset();
	}
	
	/* (non-Javadoc)
	 * @see java.io.Reader#close()
	 */
	public void closed() throws IOException {
		if (this.in != null)
			this.in.close();
	}
	
	/**
	 * Produce the encapsulated <code>InputStream</code>. This method is only
	 * called upon the first invocation of the <code>read()</code> method.
	 * Implementations of this method must not return null.
	 * @return the encapsulated input stream
	 */
	protected abstract InputStream getInputStream() throws IOException;
}