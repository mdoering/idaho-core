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
import java.io.Writer;

/**
 * This class encapsulates another <code>Writer</code>. However, the latter
 * does not have to be specified as a constructor argument, but is rather
 * provided via the abstract <code>getWriter()</code> method. This method is
 * only called upon the first write, only opening the encapsulated writer upon
 * first use.
 * 
 * @author sautter
 */
public abstract class OnDemandWriter extends Writer {
	private Writer out = null;
	
	/** Constructor
	 */
	public OnDemandWriter() {}
	
	/* (non-Javadoc)
	 * @see java.io.Writer#write(int)
	 */
	public void write(int c) throws IOException {
		if (this.out == null)
			this.out = this.getWriter();
		this.out.write(c);
	}
	
	/* (non-Javadoc)
	 * @see java.io.Writer#write(char[], int, int)
	 */
	public void write(char[] cbuf, int off, int len) throws IOException {
		if (this.out == null)
			this.out = this.getWriter();
		this.out.write(cbuf, off, len);
	}
	
	/* (non-Javadoc)
	 * @see java.io.Writer#flush()
	 */
	public void flush() throws IOException {
		if (this.out == null)
			this.out = this.getWriter();
		this.out.flush();
	}
	
	/* (non-Javadoc)
	 * @see java.io.Writer#close()
	 */
	public void close() throws IOException {
		if (this.out != null)
			this.out.close();
	}
	
	/**
	 * Produce the encapsulated <code>Writer called upon the first invocation
	 * of the <code>write()</code> method. Implementations of this method must
	 * not return null.
	 * @return the encapsulated writer
	 */
	protected abstract Writer getWriter() throws IOException;
}