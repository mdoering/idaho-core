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
package de.uka.ipd.idaho.htmlXmlUtil.accessories;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 * Utility for sending multipart form data via HTTP POST, especially file
 * uploads. TODO document this further
 * 
 * @author sautter
 */
public class MultipartFormData {
	
	private class Parameter {
		final String name;
		final String fileName;
		final int valueSize;
		final InputStream valueIn;
		Parameter(String name, String fileName, int valueSize, InputStream valueIn) {
			this.name = name;
			this.fileName = fileName;
			this.valueSize = valueSize;
			this.valueIn = valueIn;
		}
	}
	
	private class FileValueInputStream extends InputStream {
		private File file;
		private InputStream fileIn = null;
		FileValueInputStream(File file) {
			this.file = file;
		}
		public int read() throws IOException {
			this.ensureOpen();
			return this.fileIn.read();
		}
		public int read(byte[] b) throws IOException {
			return this.read(b, 0, b.length);
		}
		public int read(byte[] b, int off, int len) throws IOException {
			this.ensureOpen();
			return this.fileIn.read(b, off, len);
		}
		public long skip(long n) throws IOException {
			this.ensureOpen();
			return this.fileIn.skip(n);
		}
		public int available() throws IOException {
			this.ensureOpen();
			return this.fileIn.available();
		}
		public void close() throws IOException {
			if (this.fileIn != null)
				this.fileIn.close();
		}
		private void ensureOpen() throws IOException {
			if (this.fileIn == null)
				this.fileIn = new FileInputStream(this.file);
		}
	}
	
	private Properties headers = new Properties();
	private ArrayList parameters = new ArrayList(2);
	
	/**
	 * Add a request parameter whose value lies in a file. Each request can have
	 * multiple values per parameter name. the postTo() method will read the
	 * argument input stream to its end and close it afterward. If this method
	 * is called after the postTo() method, an IllegalStateException will be
	 * thrown.
	 * @param name the parameter name
	 * @param value the file holding the parameter value
	 * @param fileName the parameter file name, null for non-file values
	 * @param valueSize the number of bytes in the parameter value
	 */
	public void addParameter(String name, InputStream valueIn, String fileName, int valueSize) {
		if (this.parameters == null)
			throw new IllegalStateException("Request already sent");
		this.parameters.add(new Parameter(name, fileName, valueSize, valueIn));
	}
	
	/**
	 * Add a request parameter whose value lies in a file. Each request can have
	 * multiple values per parameter name. If this method is called after the
	 * postTo() method, an IllegalStateException will be thrown.
	 * @param name the parameter name
	 * @param value the file holding the parameter value
	 */
	public void addParameter(String name, File value) {
		this.addParameter(name, new FileValueInputStream(value), value.getName(), ((int) value.length()));
	}
	
	/**
	 * Add a request parameter whose value lies in memory. Each request can have
	 * multiple values per parameter name. If this method is called after the
	 * postTo() method, an IllegalStateException will be thrown.
	 * @param name the parameter name
	 * @param value the parameter value in a byte array
	 */
	public void addParameter(String name, byte[] value) {
		this.addParameter(name, new ByteArrayInputStream(value), null, value.length);
	}
	
	/**
	 * Set a request header. If this method is called after the postTo() method,
	 * an IllegalStateException will be thrown.
	 * @param name the header name
	 * @param value the header value
	 */
	public void setHeader(String name, String value) {
		if (this.headers == null)
			throw new IllegalStateException("Request already sent");
		this.headers.setProperty(name, value);
	}
	
	/**
	 * Send the request to some URL using HTTP POST. This method expends all
	 * input streams added as parameters, so it can be used only once per
	 * instance.
	 * @param url the URL to send the data to
	 * @param encoding the encoding to use for textual data
	 * @return an input stream for reading the server response
	 * @throws IOException
	 */
	public InputStream postTo(URL url, String encoding) throws IOException {
		if (this.parameters == null)
			throw new IllegalStateException("Request already sent");
		
		//	create connection
		final HttpURLConnection con = ((HttpURLConnection) url.openConnection());
		con.setDoOutput(true);
		con.setDoInput(true);
		con.setRequestMethod("POST");
		
		//	generate boundary
		String boundary = ("---------------------------" + System.currentTimeMillis());
		String endBoundary = (boundary + "--");
		
		//	compute content length
		int length = ("--".length() + endBoundary.length());
		for (int p = 0; p < this.parameters.size(); p++) {
			Parameter param = ((Parameter) this.parameters.get(p));
			length += this.getParameterSize(boundary, param.name, param.fileName, param.valueSize, encoding);
		}
		
		//	set request headers
		con.setRequestProperty("Content-Length", ("" + length));
		con.setRequestProperty("Content-Type", ("multipart/form-data; boundary=" + boundary));
		for (Iterator hit = this.headers.keySet().iterator(); hit.hasNext();) {
			String hn = ((String) hit.next());
			if ("Content-Length".equalsIgnoreCase(hn))
				continue;
			if ("Content-Type".equalsIgnoreCase(hn))
				continue;
			con.setRequestProperty(hn, this.headers.getProperty(hn));
		}
		
		//	send request
		BufferedOutputStream out = new BufferedOutputStream(con.getOutputStream());
		for (int p = 0; p < this.parameters.size(); p++) {
			Parameter param = ((Parameter) this.parameters.get(p));
			this.writeParameter(out, boundary, param.name, param.fileName, param.valueIn, param.valueSize, encoding);
		}
		out.write("--".getBytes(encoding));
		out.write(endBoundary.getBytes(encoding));
		out.flush();
		out.close();
		
		//	remember being sent
		this.headers = null;
		this.parameters = null;
		
		//	return input stream for reading server response
		return new BufferedInputStream(con.getInputStream()) {
			public void close() throws IOException {
				super.close();
				con.disconnect();
			}
		};
	}
	
	private static final byte[] lineBreakBytes = {((byte) '\r'), ((byte) '\n')};
	
	private int getParameterSize(String boundary, String name, String fileName, int dataLength, String encoding) throws IOException {
		int length = 0;
		length += ("--".length() + boundary.length()); // boundary
		length += lineBreakBytes.length; // line break after boundary
		length += ("Content-Disposition: form-data; name=\"\"").length(); // field name context
		length += name.getBytes(encoding).length; // actual field name
		if (fileName != null) {
			length += ("; filename=\"\"").length(); // file name context (if any)
			length += fileName.getBytes(encoding).length;
		}
		length += lineBreakBytes.length; // line break after content disposition
		length += lineBreakBytes.length; // line break for blank line
		length += dataLength; // length of actual data
		length += lineBreakBytes.length; // terminal line break
		return length;
	}
	
	private void writeParameter(OutputStream out, String boundary, String name, String fileName, InputStream dataIn, int dataInLength, String encoding) throws IOException {
		out.write("--".getBytes(encoding));
		out.write(boundary.getBytes(encoding));
		out.write(lineBreakBytes);
		out.write(("Content-Disposition: form-data; name=\"" + name + "\"").getBytes(encoding));
		if (fileName != null)
			out.write(("; filename=\"" + fileName + "\"").getBytes(encoding));
		out.write(lineBreakBytes);
		out.write(lineBreakBytes);
		byte[] dataBuf = new byte[((dataInLength < 1) ? 1024 : Math.min(1024, dataInLength))];
		int r;
		while ((r = dataIn.read(dataBuf, 0, dataBuf.length)) != -1)
			out.write(dataBuf, 0, r);
		dataIn.close();
		out.write(lineBreakBytes);
	}
}
