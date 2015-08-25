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
package de.uka.ipd.idaho.easyIO.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.easyIO.streams.PeekInputStream;

/**
 * Memory based cache for form data uploads, both regular and multipart,
 * especially file uploads. TODO document this
 * 
 * @author sautter
 */
public class FormDataReceiver {
	/**
	 * Input stream for field values, providing some meta information.
	 * 
	 * @author sautter
	 */
	public static class FieldValueInputStream extends InputStream {
		private InputStream in;
		
		/** the field the stream belongs to */
		public final String fieldName;
		
		/** the the name of the uploaded file stored in this field (null for all other fields) */
		public final String fileName;
		
		/** the total number of bytes in the stream */
		public final int fieldLength;
		
		/** true if value cached in file, false if in memory */
		public final boolean isFileBased;
		
		private int bytesRead = 0;
		private int bytesReadMark = 0;
		
		private String encoding = null;
		private boolean encodingGuessed = false;
		
		private Field parent;
		
		FieldValueInputStream(String fieldName, byte[] bytes, String fileName, String encoding) {
			this.fieldName = fieldName;
			this.fieldLength = bytes.length;
			this.in = new ByteArrayInputStream(bytes);
			this.fileName = fileName;
			this.isFileBased = false;
			if (encoding == null) {
				this.encoding = guessEncoding(bytes, 0);
				this.encodingGuessed = (getBomEnd(bytes) == 0);
			}
			else {
				this.encoding = encoding;
				this.encodingGuessed = false;
			}
		}
		
		FieldValueInputStream(String fieldName, final File fieldValueFile, String fileName, String encoding, Field parent) throws IOException {
			this.fieldName = fieldName;
			this.fieldLength = ((int) fieldValueFile.length());
			this.in = new BufferedInputStream(new InputStream() {
				private InputStream fin = null;
				public int read() throws IOException {
					this.ensureOpen();
					return this.fin.read();
				}
				public int read(byte[] b) throws IOException {
					return this.read(b, 0, b.length);
				}
				public int read(byte[] b, int off, int len) throws IOException {
					this.ensureOpen();
					return this.fin.read(b, off, len);
				}
				public long skip(long n) throws IOException {
					this.ensureOpen();
					return this.fin.skip(n);
				}
				public int available() throws IOException {
					this.ensureOpen();
					return this.fin.available();
				}
				public void close() throws IOException {
					if (this.fin != null)
						this.fin.close();
				}
				private void ensureOpen() throws IOException {
					if (this.fin == null) {
						this.fin = new FileInputStream(fieldValueFile);
						FieldValueInputStream.this.parent.fieldInputStreams.put(FieldValueInputStream.this, FieldValueInputStream.this);
					}
				}
			});
			this.fileName = fileName;
			this.isFileBased = true;
			this.parent = parent;
			if (encoding != null) {
				this.encoding = encoding;
				this.encodingGuessed = false;
			}
		}
		
		/**
		 * Get the number of bytes read from this stream so far.
		 * @return the bytesRead
		 */
		public int getBytesRead() {
			return this.bytesRead;
		}
		
		/**
		 * Get the character encoding of the field. This method only returns
		 * meaningful values if the field actually contains character data. For
		 * fields holding the content of an uploaded file, the encoding
		 * generally cannot be determined from the HTTP request header. In such
		 * cases, the encoding is, in this order, (a) determined from a byte
		 * order mark, if given, or (b) inferred from bytes that are
		 * characteristic for specific encodings.
		 * @return the encoding
		 */
		public String getEncoding() {
			if (this.encoding == null) try {
				byte[] bytes = new byte[1024];
				this.in.mark(1024);
				int l = this.in.read(bytes);
				this.in.reset();
				if (l != -1) {
					this.encoding = guessEncoding(bytes, l);
					this.encodingGuessed = (getBomEnd(bytes) == 0);
				}
			}
			catch (IOException ioe) {
				System.out.println("COULD NOT GUESS ENCODING: " + ioe.getMessage());
				ioe.printStackTrace(System.out);
			}
			return this.encoding;
		}
		
		/**
		 * Check if the character encoding returned by the getEncoding() method
		 * is explicitly given in the HTTP request or the data itself, or if it
		 * is inferred from characteristic bytes.
		 * @return the encodingGuessed
		 */
		public boolean isEncodingInferred() {
			return this.encodingGuessed;
		}

		public int read() throws IOException {
			int r = this.in.read();
			if (r != -1)
				this.bytesRead++;
			return r;
		}
		public int read(byte[] b) throws IOException {
			int r = this.in.read(b);
			if (r != -1)
				this.bytesRead += r;
			return r;
		}
		public int read(byte[] b, int off, int len) throws IOException {
			int r = this.in.read(b, off, len);
			if (r != -1)
				this.bytesRead += r;
			return r;
		}
		public long skip(long n) throws IOException {
			long s = this.in.skip(n);
			this.bytesRead += s;
			return s;
		}
		public int available() throws IOException {
			return this.in.available();
		}
		public void close() throws IOException {
			this.in.close();
			if (this.parent != null)
				this.parent.fieldInputStreams.remove(this);
		}
		public synchronized void mark(int readlimit) {
			this.in.mark(readlimit);
			this.bytesReadMark = this.bytesRead;
		}
		public synchronized void reset() throws IOException {
			this.in.reset();
			this.bytesRead = this.bytesReadMark;
		}
		public boolean markSupported() {
			return this.in.markSupported();
		}
	}
	
	/**
	 * A field represents a request parameter, with one or more values.
	 * 
	 * @author sautter
	 */
	public static class Field {
		
		/** the field name */
		public final String name;
		
		private ArrayList values = new ArrayList(1);
		private HashMap fieldInputStreams = new HashMap(2);
		
		Field(String name) {
			this.name = name;
		}
		void addValue(byte[] bytes, String fileName, String encoding) {
			this.values.add(new FieldValue(bytes, fileName, encoding));
		}
		void addValue(File cacheFile, String fileName, String encoding) {
			this.values.add(new FieldValue(cacheFile, fileName, encoding));
		}
		String getFileName() {
			return ((FieldValue) this.values.get(0)).fileName;
		}
		String[] getFileNames() {
			String[] sValues = new String[this.values.size()];
			for (int v = 0; v < this.values.size(); v++)
				sValues[v] = ((FieldValue) this.values.get(v)).fileName;
			return sValues;
		}
		String getValue() throws IOException {
			FieldValue value = ((FieldValue) this.values.get(0));
			if (value.bytes == null)
				throw new IOException("Cached on disc, stream access only.");
			return getString(value.bytes, false, value.encoding);
		}
		String[] getValues() throws IOException {
			String[] sValues = new String[this.values.size()];
			for (int v = 0; v < this.values.size(); v++) {
				FieldValue value = ((FieldValue) this.values.get(v));
				if (value.bytes == null)
					throw new IOException("Cached on disc, stream access only.");
				sValues[v] = getString(value.bytes, false, value.encoding);
			}
			return sValues;
		}
		byte[] getByteValue() throws IOException {
			FieldValue value = ((FieldValue) this.values.get(0));
			if (value.bytes == null)
				throw new IOException("Cached on disc, stream access only.");
			return value.bytes;
		}
		byte[][] getByteValues() throws IOException {
			byte[][] sValues = new byte[this.values.size()][];
			for (int v = 0; v < this.values.size(); v++) {
				FieldValue value = ((FieldValue) this.values.get(v));
				if (value.bytes == null)
					throw new IOException("Cached on disc, stream access only.");
				sValues[v] = value.bytes;
			}
			return sValues;
		}
		FieldValueInputStream getByteStream() throws IOException {
			FieldValue value = ((FieldValue) this.values.get(0));
			if (value.bytes == null)
				return new FieldValueInputStream(this.name, value.cacheFile, value.fileName, value.encoding, this);
			else return new FieldValueInputStream(this.name, value.bytes, value.fileName, value.encoding);
		}
		FieldValueInputStream[] getByteStreams() throws IOException {
			FieldValueInputStream[] sValues = new FieldValueInputStream[this.values.size()];
			for (int v = 0; v < this.values.size(); v++) {
				FieldValue value = ((FieldValue) this.values.get(v));
				if (value.bytes == null)
					sValues[v] = new FieldValueInputStream(this.name, value.cacheFile, value.fileName, value.encoding, this);
				else sValues[v] = new FieldValueInputStream(this.name, value.bytes, value.fileName, value.encoding);
			}
			return sValues;
		}
		void closeStreams() {
			FieldValueInputStream[] fvis = ((FieldValueInputStream[]) this.fieldInputStreams.keySet().toArray(new FieldValueInputStream[this.fieldInputStreams.size()]));
			for (int i = 0; i < fvis.length; i++) try {
				fvis[i].close();
			} catch (IOException ioe) {}
		}
		void dispose() {
			this.closeStreams();
			for (int v = 0; v < this.values.size(); v++) {
				FieldValue value = ((FieldValue) this.values.get(v));
				if (value.cacheFile != null)
					value.cacheFile.delete();
			}
		}
	}
	private static class FieldValue {
		byte[] bytes;
		String fileName;
		String encoding;
		File cacheFile = null;
		FieldValue(byte[] bytes, String fileName, String encoding) {
			this.bytes = bytes;
			this.fileName = fileName;
			this.encoding = ((fileName == null) ? encoding : null); // encoding for file content is not in HTTP request
		}
		FieldValue(File cacheFile, String fileName, String encoding) {
			this.cacheFile = cacheFile;
			this.fileName = fileName;
			this.encoding = ((fileName == null) ? encoding : null); // encoding for file content is not in HTTP request
		}
	}
	
	private String method = null;
	
	private String serverName = null;
	private int serverPort = 0;
	private String contextPath = null;
	private String servletPath = null;
	private String pathInfo = null;
	private String queryString = null;
	
	private String remoteAddress = null;
	private String remoteHost = null;
	private String remoteUser = null;
	
	private HashMap headers = new HashMap(8);
	private HashMap fields = new HashMap(8);
	
	private String contentType;
	
	private File cacheFolder;
	private int maxFieldBytes;
	
	/** the identifier of the request */
	public final String id;
	
	private FormDataReceiver(String id) {
		this.id = id;
	}
	
	/**
	 * Persist the form data set to disc. The form data set can be
	 * reinstantiated via the restore() method. If the form data set does not
	 * have a cache folder, this method has no effect. Otherwise, this method
	 * clears all data after persisting it, this object should not be used after
	 * this method returns.
	 * @return the folder the form data set was persisted to
	 */
	public File persist() throws IOException {
		return this.persist(this.cacheFolder);
	}
	
	/**
	 * Persist the form data set to disc, with a specific ID. The form data set
	 * can be reinstantiated via the restore() method. If the form data set does
	 * not have a cache folder, this method has no effect. Otherwise, this
	 * method clears all data after persisting it, this object should not be
	 * used after this method returns. The data is stored in a folder named as
	 * the argument ID, located in the same parent directory as the original
	 * cache folder.
	 * @param id the ID to persist the data under
	 * @return the folder the form data set was persisted to
	 */
	public File persist(String id) throws IOException {
		return ((this.cacheFolder == null) ? null : this.persist((id == null) ? this.cacheFolder : new File(this.cacheFolder.getParentFile(), id)));
	}
	
	/**
	 * Persist the form data set to disc, to a specific folder. The form data
	 * set can be reinstantiated via the restore() method. If the form data set
	 * does not have a cache folder and the argument folder is null, this method
	 * has no effect and returns null. Otherwise, this method clears all data
	 * after persisting it, so this object should not be used after this method
	 * returns. If the form data set does have a cache folder and the argument
	 * folder is not null and points to a different storage location, the cache
	 * folder is renamed to the argument folder. If the argument folder is a
	 * file and not a directory, this method throws an IllegalArgumentException.
	 * @param cacheFolder the folder to persist the data to
	 * @return the folder the form data set was persisted to
	 */
	public File persist(File cacheFolder) throws IOException {
		
		//	make sure we have a directory to cache to
		if (cacheFolder == null)
			cacheFolder = this.cacheFolder;
		else if (this.cacheFolder == null)
			this.cacheFolder = cacheFolder;
		if (cacheFolder == null)
			return null;
		if (!cacheFolder.exists() && !cacheFolder.mkdirs())
			return null;
		if (!cacheFolder.isDirectory())
			throw new IllegalArgumentException("Form data can only be persisted to directories.");
		
		//	store general request data
		Settings data = new Settings();
		data.setSetting("maxFieldBytes", ("" + this.maxFieldBytes));
		
		if (this.method != null)
			data.setSetting("method", this.method);
		if (this.contentType != null)
			data.setSetting("contentType", this.contentType);
		
		if (this.serverName != null)
			data.setSetting("serverName", this.serverName);
		if (this.serverPort != 0)
			data.setSetting("serverPort", ("" + this.serverPort));
		if (this.contextPath != null)
			data.setSetting("contextPath", this.contextPath);
		if (this.servletPath != null)
			data.setSetting("servletPath", this.servletPath);
		if (this.pathInfo != null)
			data.setSetting("pathInfo", this.pathInfo);
		if (this.queryString != null)
			data.setSetting("queryString", this.queryString);
		
		if (this.remoteAddress != null)
			data.setSetting("remoteAddress", this.remoteAddress);
		if (this.remoteHost != null)
			data.setSetting("remoteHost", this.remoteHost);
		if (this.remoteUser != null)
			data.setSetting("remoteUser", this.remoteUser);
		
		//	store request headers
		String[] hns = this.getHeaderNames();
		if (hns.length != 0) {
			Settings headerData = data.getSubset("header");
			for (int h = 0; h < hns.length; h++) {
				String[] hvs = this.getHeaderValues(hns[h]);
				for (int v = 0; v < hvs.length; v++)
					headerData.setSetting((hns[h] + ".value" + v), hvs[v]);
			}
		}
		
		//	close streams & persist data
		String[] fns = this.getFieldNames();
		if (fns.length != 0) {
			Settings fieldData = data.getSubset("field");
			for (int f = 0; f < fns.length; f++) {
				Field field = this.getField(fns[f], false);
				if (field == null)
					continue;
				field.closeStreams();
				
				String fieldName = field.name.replaceAll("[^a-zA-Z0-9\\_\\-]", "_");
				fieldData.setSetting((fieldName + ".name"), field.name);
				
				for (int v = 0; v < field.values.size(); v++) {
					FieldValue fv = ((FieldValue) field.values.get(v));
					
					if (fv.fileName != null)
						fieldData.setSetting((fieldName + ".value" + v + ".fileName"), URLEncoder.encode(fv.fileName, "UTF-8"));
					if (fv.encoding != null)
						fieldData.setSetting((fieldName + ".value" + v + ".encoding"), fv.encoding);
					
					if (fv.cacheFile == null) {
						fieldData.setSetting((fieldName + ".value" + v + ".isFile"), "f");
						fieldData.setSetting((fieldName + ".value" + v + ".dataString"), URLEncoder.encode(getString(fv.bytes, false, fv.encoding), "UTF-8"));
					}
					else {
						fieldData.setSetting((fieldName + ".value" + v + ".isFile"), "t");
						fieldData.setSetting((fieldName + ".value" + v + ".dataFile"), URLEncoder.encode(fv.cacheFile.getName(), "UTF-8"));
					}
				}
			}
		}
		
		//	store data
		data.storeAsText(new File(cacheFolder, "Request.cache"));
		
		//	rename folder if necessary
		if ((this.cacheFolder != null) && !this.cacheFolder.equals(cacheFolder))
			this.cacheFolder.renameTo(cacheFolder);
		
		//	free up memory (prevents any deletions in dispose() along the way)
		this.fields.clear();
		this.headers.clear();
		this.cacheFolder = null;
		
		//	... finally
		return cacheFolder;
	}
	
	/**
	 * Dispose of the request data. This method will close all open input
	 * streams for any field values and delete any files caching request data on
	 * disc.
	 */
	public void dispose() {
		String[] fns = this.getFieldNames();
		for (int f = 0; f < fns.length; f++) {
			Field field = this.getField(fns[f], false);
			if (field != null)
				field.dispose();
		}
		if (this.cacheFolder != null)
			this.cacheFolder.delete();
	}
	
	/**
	 * Finalizing a request calls the dispose() method, so to free up any disc
	 * space used for caching.
	 */
	protected void finalize() throws Throwable {
		this.dispose();
	}
	
	private void addHeaderValue(String name, String value) {
		name = name.toLowerCase();
		Object nValue = this.headers.get(name);
		if (nValue == null)
			this.headers.put(name, value);
		else if (nValue instanceof ArrayList)
			((ArrayList) nValue).add(value);
		else {
			ArrayList values = new ArrayList(3);
			values.add(nValue);
			values.add(value);
			this.headers.put(name, values);
		}
	}
	
	/**
	 * Retrieve a request header. If the header with the argument name has
	 * multiple values, this method returns only the first one.
	 * @param name the header name
	 * @return the value of header with the specified name
	 */
	public String getHeaderValue(String name) {
		name = name.toLowerCase();
		Object value = this.headers.get(name);
		if (value == null)
			return null;
		else if (value instanceof String)
			return ((String) value);
		else return ((String) ((ArrayList) value).get(0));
	}
	
	/**
	 * Retrieve all values of a request header.
	 * @param name the header name
	 * @return the values of header with the specified name
	 */
	public String[] getHeaderValues(String name) {
		name = name.toLowerCase();
		Object value = this.headers.get(name);
		if (value == null)
			return null;
		else if (value instanceof String) {
			String[] values = {((String) value)};
			return values;
		}
		else return ((String[]) ((ArrayList) value).toArray(new String[((ArrayList) value).size()]));
	}
	
	/**
	 * Retrieve the content type of the request. This usually is either of
	 * 'multipart/form-data', 'application/x-www-form-urlencoded', or
	 * 'text/plain'.
	 * @return the content type
	 */
	public String getContentType() {
		return this.contentType;
	}
	
	/**
	 * Retrieve the HTTP method the wrapped request was sent with.
	 * @return the HTTP method
	 */
	public String getMethod() {
		return this.method;
	}
	
	/**
	 * Retrieve the server name the wrapped request was sent to.
	 * @return the server name
	 */
	public String getServerName() {
		return this.serverName;
	}
	
	/**
	 * Retrieve the server port the wrapped request was sent to.
	 * @return the server port
	 */
	public int getServerPort() {
		return this.serverPort;
	}
	
	/**
	 * Retrieve the context path of the wrapped request.
	 * @return the context path
	 */
	public String getContextPath() {
		return this.contextPath;
	}
	
	/**
	 * Retrieve the servlet path of the wrapped request.
	 * @return the servlet path
	 */
	public String getServletPath() {
		return this.servletPath;
	}
	
	/**
	 * Retrieve the path info of the wrapped request.
	 * @return the path info
	 */
	public String getPathInfo() {
		return this.pathInfo;
	}
	
	/**
	 * Retrieve the query string of the wrapped request.
	 * @return the query string
	 */
	public String getQueryString() {
		return this.queryString;
	}
	
	/**
	 * Retrieve the remote IP address of the wrapped request.
	 * @return the remote address
	 */
	public String getRemoteAddress() {
		return this.remoteAddress;
	}
	
	/**
	 * Retrieve the remote host name of the wrapped request.
	 * @return the remote host name
	 */
	public String getRemoteHost() {
		return this.remoteHost;
	}
	
	/**
	 * Retrieve the remote user of the wrapped request.
	 * @return the remote user
	 */
	public String getRemoteUser() {
		return this.remoteUser;
	}
	
	/**
	 * Retrieve the names of all headers of the request.
	 * @return an array holding the header names
	 */
	public String[] getHeaderNames() {
		return ((String[]) this.headers.keySet().toArray(new String[this.headers.size()]));
	}
	
	/**
	 * Add an in-memory value to a field. If the field does not yet exist in the
	 * form data set, it is created. The field name must be a valid HTTP field
	 * name.
	 * @param name the field name
	 * @param value the field value as a byte array
	 * @param encoding the character encoding of the value
	 */
	public void addFieldValue(String name, byte[] value, String encoding) {
		this.addFieldValue(name, value, null, encoding);
	}
	
	/**
	 * Add the data provided by an input stream as a value to a field. If the
	 * field does not yet exist in the form data set, it is created. The field
	 * name must be a valid HTTP field name. If the cacheImmediately flag is set
	 * to true, the data provided by the argument stream is cached on disc right
	 * away. If the field data set was constructed without a cache folder,
	 * however, that flag has no effect, and the data will reside in memory.
	 * @param name the field name
	 * @param fieldValueIn the input stream to read the the field value from
	 * @param encoding the character encoding of the value
	 * @param cacheImmediately cache directly to disc?
	 */
	public void addFieldValue(String name, InputStream fieldValueIn, String encoding, boolean cacheImmediately) throws IOException {
		OutputStream fieldValue;
		File fieldValueFile = null;
		
		if (cacheImmediately && (this.cacheFolder != null)) {
			String fieldValueFileName = ("Field." + name.replaceAll("[^a-zA-Z0-9\\_\\-]", "_") + "." + this.getValueCount(name) + ".cache");
			fieldValueFile = new File(this.cacheFolder, fieldValueFileName);
			fieldValueFile.getParentFile().mkdirs();
			fieldValueFile.createNewFile();
			fieldValue = new BufferedOutputStream(new FileOutputStream(fieldValueFile));
		}
		else fieldValue = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[1024];
		int read;
		int fieldValueSize = 0;
		while ((read = fieldValueIn.read(buffer, 0, buffer.length)) != -1) {
			fieldValue.write(buffer, 0, read);
			fieldValueSize += read;
			
			//	switch to file if field too big or explicitly required
			if ((fieldValue instanceof ByteArrayOutputStream) && (fieldValueSize > this.maxFieldBytes)) {
				String fieldValueFileName = ("Field." + name.replaceAll("[^a-zA-Z0-9\\_\\-]", "_") + "." + this.getValueCount(name) + ".cache");
				fieldValueFile = new File(this.cacheFolder, fieldValueFileName);
				fieldValueFile.getParentFile().mkdirs();
				fieldValueFile.createNewFile();
				byte[] fv = ((ByteArrayOutputStream) fieldValue).toByteArray();
				fieldValue = new BufferedOutputStream(new FileOutputStream(fieldValueFile));
				fieldValue.write(fv);
			}
		}
		
		if (fieldValue instanceof ByteArrayOutputStream)
			this.addFieldValue(name, ((ByteArrayOutputStream) fieldValue).toByteArray(), null, encoding);
		else {
			fieldValue.flush();
			fieldValue.close();
			this.addFieldValue(name, fieldValueFile, null, encoding);
		}
	}
	
	private void addFieldValue(String name, byte[] value, String fileName, String encoding) {
		this.getField(name, true).addValue(value, fileName, encoding);
	}
	private void addFieldValue(String name, File fieldValueFile, String fileName, String encoding) {
		this.getField(name, true).addValue(fieldValueFile, fileName, encoding);
	}
	
	/**
	 * Remove a field from the form data set. If any of the field's values are
	 * cached on disc, the respective files are deleted. Before that, this
	 * method closes any input streams that might be open for any of the values.
	 * @param name the name of the field to remove
	 */
	public void removeField(String name) {
		Field field = this.getField(name);
		if (field != null)
			field.dispose();
		this.fields.remove(name);
	}
	
	private Field getField(String name) {
		return this.getField(name, false);
	}
	private Field getField(String name, boolean create) {
		Field field = ((Field) this.fields.get(name));
		if ((field == null) && create) {
			field = new Field(name);
			this.fields.put(name, field);
		}
		return field;
	}
	
	/**
	 * Test if the request contains a value for a given field.
	 * @param name the field name
	 * @return true if the request contains at least one value for the argument
	 *         field
	 */
	public boolean hasField(String name) {
		return (this.getField(name) != null);
	}
	
	/**
	 * Test if the request contains multiple values for a given field.
	 * @param name the field name
	 * @return true if the request contains two r more values for the arguent
	 *         field
	 */
	public boolean isMultiValued(String name) {
		Field field = this.getField(name);
		return ((field != null) && (field.values.size() > 1));
	}
	
	/**
	 * Retrieve the number of values this request contains for a given field.
	 * @param name the field name
	 * @return the number of values the request contains for the argument field
	 */
	public int getValueCount(String name) {
		Field field = this.getField(name);
		return ((field == null) ? 0 : field.values.size());
	}
	
	/**
	 * Retrieve the name of the file a field value originated from on the client
	 * side. If the argument field does not originate from a file, this method
	 * returns null. If the field with the argument name has multiple values,
	 * this method returns only the source file name for the first one.
	 * @param name the field name
	 * @return the source file name of the specified field
	 */
	public String getSourceFileName(String name) {
		Field field = this.getField(name);
		return ((field == null) ? null : field.getFileName());
	}
	
	/**
	 * Retrieve the names of the files the values of a field originated from on
	 * the client side. If some value of the argument field does not originate
	 * from a file, the corresponding entry in the returned array is null.
	 * @param name the field name
	 * @return the source file names of the values of the specified field
	 */
	public String[] getSourceFileNames(String name) {
		Field field = this.getField(name);
		return ((field == null) ? null : field.getFileNames());
	}
	
	/**
	 * Retrieve the value of a field. If the field with the argument name has
	 * multiple values, this method returns only the first one. The field value
	 * is decoded using the encoding specified in the request header. If the
	 * field value is cached on disc because it is larger than the limit for
	 * memory based caching, this method throws an IOException. In that case,
	 * the field value is only accessible via the getFieldByteStream() method.
	 * @param name the field name
	 * @return the value of the specified field
	 * @throws IOException
	 */
	public String getFieldValue(String name) throws IOException {
		Field field = this.getField(name);
		return ((field == null) ? null : field.getValue());
	}
	
	/**
	 * Retrieve all values of a field. The field values are decoded using the
	 * encoding specified in the request header. If any of the field values is
	 * cached on disc because it is larger than the limit for memory based
	 * caching, this method throws an IOException. In that case, the field
	 * values are only accessible via the getFieldByteStreams() method.
	 * @param name the field name
	 * @return an array holding the values of the specified field
	 * @throws IOException
	 */
	public String[] getFieldValues(String name) throws IOException {
		Field field = this.getField(name);
		return ((field == null) ? null : field.getValues());
	}
	
	/**
	 * Retrieve the raw bytes of the value of a field. If the field with the
	 * argument name has multiple values, this method returns only the first
	 * one. If the field value is cached on disc because it is larger than the
	 * limit for memory based caching, this method throws an IOException. In
	 * that case, the field value is only accessible via the
	 * getFieldByteStream() method.
	 * @param name the field name
	 * @return the value of the specified field
	 * @throws IOException
	 */
	public byte[] getFieldByteValue(String name) throws IOException {
		Field field = this.getField(name);
		return ((field == null) ? null : field.getByteValue());
	}
	
	/**
	 * Retrieve the raw bytes of all values of a field. If any of the field
	 * values is cached on disc because it is larger than the limit for memory
	 * based caching, this method throws an IOException. In that case, the field
	 * values are only accessible via the getFieldByteStreams() method.
	 * @param name the field name
	 * @return an array holding the values of the specified field
	 * @throws IOException
	 */
	public byte[][] getFieldByteValues(String name) throws IOException {
		Field field = this.getField(name);
		return ((field == null) ? null : field.getByteValues());
	}
	
	/**
	 * Retrieve an input stream providing the raw bytes of the value of a field.
	 * If the field with the argument name has multiple values, this method
	 * returns an input stream only for the first one.
	 * @param name the field name
	 * @return an input stream for the value of the specified field
	 * @throws IOException
	 */
	public FieldValueInputStream getFieldByteStream(String name) throws IOException {
		Field field = this.getField(name);
		return ((field == null) ? null : field.getByteStream());
	}
	
	/**
	 * Retrieve input streams providing the raw bytes of all values of a field.
	 * @param name the field name
	 * @return an array holding input streams for the values of the specified
	 *         field
	 * @throws IOException
	 */
	public FieldValueInputStream[] getFieldByteStreams(String name) throws IOException {
		Field field = this.getField(name);
		return ((field == null) ? null : field.getByteStreams());
	}
	
	/**
	 * Retrieve the names of all field in the request.
	 * @return an array holding the field names
	 */
	public String[] getFieldNames() {
		return ((String[]) this.fields.keySet().toArray(new String[this.fields.size()]));
	}
	
	/**
	 * Receive a multipart upload from an HTTP request. This method reads the
	 * request body completely.
	 * @param request the request to receive
	 * @param maxBytes the maximum number of bytes for the request as a whole
	 * @param cacheRoot the folder to use for file based data caching
	 * @param maxFieldBytes the maximum number of bytes in a single field value
	 *            kept in memory
	 * @param cacheImmediately a set of field names to write to disc immediately
	 * @return a form data handler providing access to the data
	 * @throws IOException
	 */
	public static FormDataReceiver receive(HttpServletRequest request, int maxBytes, File cacheRoot, int maxFieldBytes, Set cacheImmediately) throws IOException {
		
		//	check data
		String contentLengthString = request.getHeader("content-length");
		if (contentLengthString == null)
			throw new IOException("Invalid content length");
		int contentLength;
		try {
			contentLength = Integer.parseInt(contentLengthString.trim());
		}
		catch (NumberFormatException nfe) {
			throw new IOException("Invalid content length " + contentLengthString);
		}
//		System.out.println("Content length is " + contentLength);
		if (contentLength > maxBytes)
			throw new IOException("Content too large, " + contentLengthString + " > " + maxBytes);
		
		String encoding = request.getCharacterEncoding();
		String contentType = request.getHeader("content-type");
		if (contentType == null)
			contentType = "application/x-www-form-urlencoded";
		String boundary;
		String endBoundary;
		String id;
		
		//	multipart request
		if (contentType.toLowerCase().startsWith("multipart/form-data")) {
			if (contentType.indexOf("boundary=") == -1)
				throw new IOException("Invalid content type " + contentType);
			boundary = ("--" + contentType.substring(contentType.indexOf("boundary=") + "boundary=".length()));
			endBoundary = (boundary + "--");
			contentType = "multipart/form-data";
			id = boundary.replaceAll("\\-", "");
		}
		
		//	regular request, no file uploads
		else {
			boundary = null;
			endBoundary = null;
			id = (System.currentTimeMillis() + "-" + ((int) (Math.random() * 256)));
		}
		
		//	create instance
		FormDataReceiver fields = new FormDataReceiver(id);
		fields.contentType = contentType;
		
		//	initialize file caching
		HashSet immediateCacheFields;
		if ((cacheRoot == null) || (maxFieldBytes < 1)) {
			fields.maxFieldBytes = Integer.MAX_VALUE;
			fields.cacheFolder = null;
			immediateCacheFields = new HashSet(1);
		}
		else {
			fields.maxFieldBytes = Math.min(maxFieldBytes, 1024);
			fields.cacheFolder = new File(cacheRoot, ("C" + id));
			immediateCacheFields = ((cacheImmediately == null) ? new HashSet(1) : new HashSet(cacheImmediately));
		}
		
		//	store request properties
		fields.method = request.getMethod();
		
		//	store local routing information
		fields.serverName = request.getServerName();
		fields.serverPort = request.getServerPort();
		fields.contextPath = request.getContextPath();
		fields.servletPath = request.getServletPath();
		fields.pathInfo = request.getPathInfo();
		fields.queryString = request.getQueryString();
		
		//	store remote information
		fields.remoteAddress = request.getRemoteAddr();
		fields.remoteHost = request.getRemoteHost();
		fields.remoteUser = request.getRemoteUser();
		
		//	store headers
		for (Enumeration hne = request.getHeaderNames(); hne.hasMoreElements();) {
			String hn = ((String) hne.nextElement());
			for (Enumeration hve = request.getHeaders(hn); hve.hasMoreElements();) {
				String hv = ((String) hve.nextElement());
				fields.addHeaderValue(hn, hv);
			}
		}
		
		//	receive non-multipart request
		if (boundary == null) {
			boolean isUrlEncoded = "application/x-www-form-urlencoded".equalsIgnoreCase(contentType);
			String fieldName = null;
			OutputStream fieldValue = null;
			int fieldValueSize = 0;
			File fieldValueFile = null;
			PeekInputStream pis = new PeekInputStream(request.getInputStream(), 32);
			while (true) {
				
				//	read field name
				StringBuffer fn = new StringBuffer();
				while ((pis.peek() != -1) && (pis.peek() != '='))
					fn.append((char) pis.read());
				
				//	got field name, store it and prepare reading value
				if ((fn.length() != 0) && (pis.peek() == '=')) {
					fieldName = fn.toString();
					fieldValueSize = 0;
					
					//	set up file if explicitly required
					if (immediateCacheFields.contains(fieldName)) {
						String fieldValueFileName = ("Field." + fieldName.replaceAll("[^a-zA-Z0-9\\_\\-]", "_") + "." + fields.getValueCount(fieldName) + ".cache");
						fieldValueFile = new File(fields.cacheFolder, fieldValueFileName);
						fieldValueFile.getParentFile().mkdirs();
						fieldValueFile.createNewFile();
						fieldValue = new BufferedOutputStream(new FileOutputStream(fieldValueFile));
					}
					
					//	start in-memory otherwise
					else fieldValue = new ByteArrayOutputStream();
					
					//	consume '='
					pis.read();
				}
				
				//	reached end of data
				else break;
				
				//	read field value
				while ((pis.peek() != -1) && (pis.peek() != '&')) {
					
					//	resolve URL encoding ...
					if (isUrlEncoded && (pis.peek() == '%')) {
						pis.read();
						char hb = ((char) pis.read());
						char lb = ((char) pis.read());
						fieldValue.write(translateHex("" + hb + "" + lb));
					}
					
					//	... and encoded space, ...
					else if (pis.peek() == '+') {
						pis.read();
						fieldValue.write((int) ' ');
					}
					
					//	... or store normal byte
					else fieldValue.write(pis.read());
					fieldValueSize++;
					
					//	switch to file if field too big or explicitly required
					if ((fieldValue instanceof ByteArrayOutputStream) && (fieldValueSize > fields.maxFieldBytes)) {
						String fieldValueFileName = ("Field." + fieldName.replaceAll("[^a-zA-Z0-9\\_\\-]", "_") + "." + fields.getValueCount(fieldName) + ".cache");
						fieldValueFile = new File(fields.cacheFolder, fieldValueFileName);
						fieldValueFile.getParentFile().mkdirs();
						fieldValueFile.createNewFile();
						byte[] fv = ((ByteArrayOutputStream) fieldValue).toByteArray();
						fieldValue = new BufferedOutputStream(new FileOutputStream(fieldValueFile));
						fieldValue.write(fv);
					}
				}
				
				//	store value
				if (fieldValue instanceof ByteArrayOutputStream)
					fields.addFieldValue(fieldName, ((ByteArrayOutputStream) fieldValue).toByteArray(), null, encoding);
				else {
					fieldValue.flush();
					fieldValue.close();
					fields.addFieldValue(fieldName, fieldValueFile, null, encoding);
				}
				
				//	clean up
				fieldName = null;
				fieldValueSize = 0;
				fieldValue = null;
				fieldValueFile = null;
				
				//	end of request body
				if (pis.peek() == -1)
					break;
				
				//	consume separator & clean up
				pis.read();
			}
			pis.close();
			
			//	return what we have
			return fields;
		}
		
		//	receive multipart request
		else {
			LineInputStream lis = new LineInputStream(new BufferedInputStream(request.getInputStream()));
			byte[] lineBytes;
			byte[] lineFeed = NONE;
			String fieldName = null;
			String fileName = null;
			int fieldValueSize = 0;
			OutputStream fieldValue = null;
			File fieldValueFile = null;
			while ((lineBytes = lis.readLine()) != null) {
				
				//	end of input
				if (equals(lineBytes, endBoundary)) {
					
					//	close last field
					if (fieldName != null) {
						if (fieldValue instanceof ByteArrayOutputStream)
							fields.addFieldValue(fieldName, ((ByteArrayOutputStream) fieldValue).toByteArray(), fileName, encoding);
						else {
							fieldValue.flush();
							fieldValue.close();
							fields.addFieldValue(fieldName, fieldValueFile, fileName, encoding);
						}
						fieldName = null;
						fileName = null;
						fieldValueSize = 0;
						fieldValue = null;
						fieldValueFile = null;
						lineFeed = NONE;
					}
					
					//	we're done here
					break;
				}
				
				//	start of field
				if (equals(lineBytes, boundary)) {
					
					//	close last field
					if (fieldName != null) {
						if (fieldValue instanceof ByteArrayOutputStream)
							fields.addFieldValue(fieldName, ((ByteArrayOutputStream) fieldValue).toByteArray(), fileName, encoding);
						else {
							fieldValue.flush();
							fieldValue.close();
							fields.addFieldValue(fieldName, fieldValueFile, fileName, encoding);
						}
						fieldName = null;
						fileName = null;
						fieldValueSize = 0;
						fieldValue = null;
						fieldValueFile = null;
						lineFeed = NONE;
					}
					
					//	read meta data of next field
					while ((lineBytes = lis.readLine()) != null) {
						
						//	blank line separating field header from content
						if (isBlankLine(lineBytes))
							break;
						
						//	this one contains the field name
						if (startsWith(lineBytes, "Content-Disposition", 0)) {
							int fieldNameStart = "Content-Disposition".length();
							while ((fieldNameStart < lineBytes.length) && !startsWith(lineBytes, " name=\"", fieldNameStart))
								fieldNameStart++;
							if (fieldNameStart == lineBytes.length)
								continue;
							fieldNameStart += " name=\"".length();
							int fieldNameEnd = fieldNameStart+1;
							while ((fieldNameEnd < lineBytes.length) && !startsWith(lineBytes, "\"", fieldNameEnd))
								fieldNameEnd++;
							if (fieldNameEnd == lineBytes.length)
								continue;
							StringBuffer fieldNameBuffer = new StringBuffer(fieldNameEnd - fieldNameStart);
							for (int b = fieldNameStart; b < fieldNameEnd; b++) {
								if (32 < lineBytes[b])
									fieldNameBuffer.append((char) lineBytes[b]);
							}
							fieldName = fieldNameBuffer.toString();
							if (fieldName.length() == 0) {
								fieldName = null;
								fileName = null;
							}
							else {
								fieldValue = new ByteArrayOutputStream();
								int fileNameStart = fieldNameEnd;
								while ((fileNameStart < lineBytes.length) && !startsWith(lineBytes, " filename=\"", fileNameStart))
									fileNameStart++;
								if (fileNameStart == lineBytes.length)
									fileName = null;
								else {
									fileNameStart += " filename=\"".length();
									int fileNameEnd = fileNameStart+1;
									while ((fileNameEnd < lineBytes.length) && !startsWith(lineBytes, "\"", fileNameEnd))
										fileNameEnd++;
									if (fileNameEnd == lineBytes.length)
										fileName = null;
									else {
										byte[] fileNameBytes = new byte[fileNameEnd - fileNameStart];
										System.arraycopy(lineBytes, fileNameStart, fileNameBytes, 0, fileNameBytes.length);
										fileName = getString(fileNameBytes, false, ((encoding == null) ? guessEncoding(fileNameBytes, 0) : encoding)).trim();
									}
								}
							}
						}
					}
					
					//	something's wrong ...
					if (lineBytes == null)
						break;
					
					//	read first line of field value (doing this here to simplify line break handling ...)
					lineBytes = lis.readLine();
					
					//	something's wrong ...
					if (lineBytes == null)
						break;
					
					//	store part of field value, preserve line feed
					if (fieldValue != null) {
						lineFeed = getLineFeed(lineBytes);
						fieldValueSize += (lineBytes.length - lineFeed.length);
						if ((fieldValue instanceof ByteArrayOutputStream) && ((fieldValueSize > fields.maxFieldBytes) || (immediateCacheFields.contains(fieldName)))) {
							String fieldValueFileName = ("Field." + fieldName.replaceAll("[^a-zA-Z0-9\\_\\-]", "_") + "." + fields.getValueCount(fieldName) + ".cache");
							fieldValueFile = new File(fields.cacheFolder, fieldValueFileName);
							fieldValueFile.getParentFile().mkdirs();
							fieldValueFile.createNewFile();
							byte[] fv = ((ByteArrayOutputStream) fieldValue).toByteArray();
							fieldValue = new BufferedOutputStream(new FileOutputStream(fieldValueFile));
							fieldValue.write(fv);
						}
						fieldValue.write(lineBytes, 0, (lineBytes.length - lineFeed.length));
					}
				}
				
				//	non-first line of field value
				else if (fieldValue != null) {
					
					//	add preceding line feed
					fieldValue.write(lineFeed);
					fieldValueSize += lineFeed.length;
					
					//	store data, preserve line feed
					lineFeed = getLineFeed(lineBytes);
					fieldValueSize += (lineBytes.length - lineFeed.length);
					if ((fieldValue instanceof ByteArrayOutputStream) && ((fieldValueSize > fields.maxFieldBytes) || (immediateCacheFields.contains(fieldName)))) {
						String fieldValueFileName = ("Field." + fieldName.replaceAll("[^a-zA-Z0-9\\_\\-]", "_") + "." + fields.getValueCount(fieldName) + ".cache");
						fieldValueFile = new File(fields.cacheFolder, fieldValueFileName);
						fieldValueFile.getParentFile().mkdirs();
						fieldValueFile.createNewFile();
						byte[] fv = ((ByteArrayOutputStream) fieldValue).toByteArray();
						fieldValue = new BufferedOutputStream(new FileOutputStream(fieldValueFile));
						fieldValue.write(fv);
					}
					fieldValue.write(lineBytes, 0, (lineBytes.length - lineFeed.length));
				}
			}
			lis.close();
		}
		
		//	return what we have
		return fields;
	}
	
	/**
	 * Restore a form data set from disc cache. If there is no 'Request.cache'
	 * file in the argument folder, this method returns null. The ID of the
	 * returned form data set is equal to the name of the argument folder.
	 * @param cacheFolder the folder to load the form data set from
	 * @return the form data set
	 * @throws IOException
	 */
	public static FormDataReceiver restore(File cacheFolder) throws IOException {
		File dataFile = new File(cacheFolder, "Request.cache");
		if (!dataFile.exists())
			return null;
		
		//	read data file
		Settings data = Settings.loadSettings(dataFile);
		
		//	create form data set
		FormDataReceiver fields = new FormDataReceiver(cacheFolder.getName());
		
		//	store cache folder
		fields.cacheFolder = cacheFolder;
		fields.maxFieldBytes = Integer.parseInt(data.getSetting("maxFieldBytes"));
		
		//	read back basic data
		fields.method = data.getSetting("method");
		fields.contentType = data.getSetting("contentType");
		
		fields.serverName = data.getSetting("serverName");
		fields.serverPort = Integer.parseInt(data.getSetting("serverPort", "0"));
		fields.contextPath = data.getSetting("contextPath");
		fields.servletPath = data.getSetting("servletPath");
		fields.pathInfo = data.getSetting("pathInfo");
		fields.queryString = data.getSetting("queryString");
		
		fields.remoteAddress = data.getSetting("remoteAddress");
		fields.remoteHost = data.getSetting("remoteHost");
		fields.remoteUser = data.getSetting("remoteUser");
		
		//	read back headers
		Settings headers = data.getSubset("header");
		String[] hns = headers.getSubsetPrefixes();
		for (int h = 0; h < hns.length; h++) {
			for (int v = 0; true; v++) {
				String hv = headers.getSetting((hns[h] + ".value" + v));
				if (hv == null)
					break;
				fields.addHeaderValue(hns[h], hv);
			}
		}
		
		//	read back fields
		Settings fieldData = data.getSubset("field");
		String[] fns = fieldData.getSubsetPrefixes();
		for (int f = 0; f < fns.length; f++) {
			String name = fieldData.getSetting(fns[f] + ".name");
			if (name == null)
				continue;
			
			for (int v = 0; true; v++) {
				String isFile = fieldData.getSetting(fns[f] + ".value" + v + ".isFile");
				if (isFile == null)
					break;
				
				String fileName = fieldData.getSetting(fns[f] + ".value" + v + ".fileName");
				if (fileName != null)
					fileName = URLDecoder.decode(fileName, "UTF-8");
				String encoding = fieldData.getSetting(fns[f] + ".value" + v + ".encoding");
				
				if ("t".equals(isFile)) {
					String dataFileName = fieldData.getSetting((fns[f] + ".value" + v + ".dataFile"));
					if (dataFileName != null)
						fields.addFieldValue(name, new File(cacheFolder, URLDecoder.decode(dataFileName, "UTF-8")), fileName, encoding);
				}
				else {
					String dataString = fieldData.getSetting((fns[f] + ".value" + v + ".dataString"));
					if (dataString != null)
						fields.addFieldValue(name, URLDecoder.decode(dataString, "UTF-8").getBytes(encoding), fileName, encoding);
				}
			}
		}
		
		//	finally ...
		return fields;
	}
	
	private static final int translateHex(String s) {
		if (s.length() == 0)
			return 0;
		int v = 0;
		for (int i = 0; i < s.length(); i++) {
			v <<= 4;
			v += translateHex(s.charAt(i));
		}
		return v;
	}
	private static final int translateHex(char c) {
		if (('0' <= c) && (c <= '9')) return (((int) c) - '0');
		else if (('a' <= c) && (c <= 'f')) return (((int) c) - 'a' + 10);
		else if (('A' <= c) && (c <= 'F')) return (((int) c) - 'A' + 10);
		else return 0;
	}
	
	private static final byte[] NONE = {};
	private static final byte[] R = {((byte) '\r')};
	private static final byte[] N = {((byte) '\n')};
	private static final byte[] RN = {((byte) '\r'), ((byte) '\n')};
	private static byte[] getLineFeed(byte[] lineBytes) {
		if ((lineBytes.length >= 2) && (lineBytes[lineBytes.length-2] == '\r') && (lineBytes[lineBytes.length-1] == '\n'))
			return RN;
		if (lineBytes.length >= 1) {
			if (lineBytes[lineBytes.length-1] == '\r')
				return R;
			if (lineBytes[lineBytes.length-1] == '\n')
				return N;
		}
		return NONE;
	}
	private static boolean isBlankLine(byte[] bytes) {
		if (bytes.length == 0)
			return true;
		if (bytes.length == 1)
			return ((bytes[0] == '\r') || (bytes[0] == '\n'));
		if (bytes.length == 2)
			return ((bytes[0] == '\r') && (bytes[1] == '\n'));
		return false;
	}
	private static class LineInputStream extends FilterInputStream {
		int buf = -1;
		LineInputStream(InputStream in) {
			super((in instanceof BufferedInputStream) ? in : new BufferedInputStream(in));
		}
		//	returns a line of bytes, INCLUDING its terminal line break bytes
		byte[] readLine() throws IOException {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int bl = 0;
			if (this.buf != -1) {
				baos.write(this.buf);
				bl++;
				this.buf = -1;
			}
			int b;
			boolean lastWasCR = false;
			while ((b = this.read()) != -1) {
				if (lastWasCR) {
					if (b == '\n') {
						baos.write(b);
						bl++;
						this.buf = -1;
					}
					else this.buf = b;
					break;
				}
				else if (b == '\n') {
					baos.write(b);
					bl++;
					break;
				}
				lastWasCR = (b == '\r');
				baos.write(b);
				bl++;
			}
			return ((bl == 0) ? null : baos.toByteArray());
		}
	}
//	private static String getString(byte[] bytes, boolean stopAtLineEnd) {
//		StringBuffer sb = new StringBuffer();
//		for (int c = 0; c < bytes.length; c++) {
//			if (stopAtLineEnd && ((bytes[c] == '\n') || (bytes[c] == '\r')))
//				break;
//			sb.append((char) (bytes[c] + ((bytes[c] < 0) ? 256 : 0)));
//		}
//		return sb.toString();
//	}
	private static String guessEncoding(byte[] bytes, int sampleLimit) {
		if (bytes.length >= 2) {
			if ((bytes[0] == -2) && (bytes[1] == -1)) {
				System.out.println("RECOGNIZED ENCODING FROM BOM: UTF-16BE");
				return "UTF-16BE";
			}
			if ((bytes[0] == -1) && (bytes[1] == -2)) {
				System.out.println("RECOGNIZED ENCODING FROM BOM: UTF-16LE");
				return "UTF-16LE";
			}
		}
		if ((bytes.length >= 3) && (bytes[0] == -17) && (bytes[1] == -69) && (bytes[2] == -65)) {
			System.out.println("RECOGNIZED ENCODING FROM BOM: UTF-8");
			return "UTF-8";
		}
		
		int sampleSize = bytes.length;
		if (sampleLimit > 1)
			sampleSize = Math.min(sampleSize, sampleLimit);
		
		int openingAngleBracket = 0;
		while ((openingAngleBracket < sampleSize) && (bytes[openingAngleBracket] != '<'))
			openingAngleBracket++;
		if ((openingAngleBracket < sampleSize) && startsWith(bytes, "<?xml", openingAngleBracket)) {
			int encodingStart = (openingAngleBracket + "<?xml".length());
			while ((encodingStart < sampleSize) && !startsWith(bytes, "encoding=\"", encodingStart)) {
				if (bytes[encodingStart] == '>')
					encodingStart = sampleSize;
				else encodingStart++;
			}
			if (encodingStart < sampleSize) {
				encodingStart += "encoding=\"".length();
				int encodingEnd = encodingStart+1;
				while ((encodingEnd < sampleSize) && (bytes[encodingEnd] != '"'))
					encodingEnd++;
				if (encodingEnd < sampleSize) {
					String encoding = new String(bytes, encodingStart, (encodingEnd - encodingStart));
					System.out.println("RECOGNIZED ENCODING FROM XML: " + encoding);
					return encoding;
				}
			}
		}
		
		int oddZeros = 0;
		int evenZeros = 0;
		int negativePairs = 0;
		int negativeBytes = 0;
		for (int b = 0; b < sampleSize; b++) {
			if (bytes[b] == 0) {
				if ((b & 1) == 0)
					evenZeros++;
				else oddZeros++;
			}
			else if (((bytes[b] == -61) || (bytes[b] == -62)) && ((b+1) < bytes.length) && (bytes[b+1] < 0)) {
				negativePairs++;
				b++;
			}
			else if (bytes[b] < 0)
				negativeBytes++;
		}
		if ((oddZeros * 3) > sampleSize) {
			System.out.println("GUESSED ENCODING FROM " + oddZeros + " ODD ZEROS: UTF-16LE");
			return "UTF-16LE";
		}
		if ((evenZeros * 3) > sampleSize) {
			System.out.println("GUESSED ENCODING FROM " + evenZeros + " EVEN ZEROS: UTF-16BE");
			return "UTF-16BE";
		}
		if (negativePairs > negativeBytes) {
			System.out.println("GUESSED ENCODING FROM " + negativePairs + " NEG-PAIRS OUT OF " + negativeBytes + " NEG-BYTES: UTF-8");
			return "UTF-8";
		}
		System.out.println("FALLING BACK TO DEFAULT: ISO-8859-1");
		return "ISO-8859-1";
	}
	private static int getBomEnd(byte[] bytes) {
		if (bytes.length >= 2) {
			if ((bytes[0] == -2) && (bytes[1] == -1))
				return 2;
			if ((bytes[0] == -1) && (bytes[1] == -2))
				return 2;
		}
		if ((bytes.length >= 3) && (bytes[0] == -17) && (bytes[1] == -69) && (bytes[2] == -65))
			return 3;
		return 0;
	}
	private static String getString(byte[] bytes, boolean stopAtLineEnd, String encoding) {
		int start = getBomEnd(bytes);
		int end = bytes.length;
		for (int c = 0; stopAtLineEnd && (c < bytes.length); c++)
			if ((bytes[c] == '\n') || (bytes[c] == '\r')) {
				end = c;
				break;
			}
		if (encoding == null)
			encoding = guessEncoding(bytes, 0);
		if (encoding == null) {
			System.out.println("INVALID ENCODING: " + encoding);
			return new String(bytes, start, (end-start));
		}
		try {
			return new String(bytes, start, (end-start), encoding);
		}
		catch (UnsupportedEncodingException uee) {
			System.out.println("UNSUPPORTED ENCODING: " + encoding);
			return new String(bytes, start, (end-start));
		}
	}
	private static boolean equals(byte[] bytes, String str) {
		if (bytes.length < str.length())
			return false;
		
		int actualByteCount = bytes.length;
		while ((actualByteCount > 0) && ((bytes[actualByteCount-1] == '\n') || (bytes[actualByteCount-1] == '\r')))
			actualByteCount--;
		if (actualByteCount != str.length())
			return false;
		
		for (int c = 0; c < str.length(); c++) {
			if (bytes[c] != str.charAt(c))
				return false;
		}
		
		return true;
	}
	private static boolean startsWith(byte[] bytes, String str, int offset) {
		if (bytes.length < (offset + str.length()))
			return false;
		
		for (int c = 0; c < str.length(); c++) {
			if (bytes[offset + c] != str.charAt(c))
				return false;
		}
		
		return true;
	}
}
