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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import de.uka.ipd.idaho.htmlXmlUtil.accessories.XsltUtils.TransformerPool.PooledTransformer;

/**
 * Utility class for creating and caching XSLT transformers, and for chaining
 * transformers.
 * 
 * @author sautter
 */
public class XsltUtils {
	
	/**
	 * This wrapper jumps over a leading byte order mark in XML files, always
	 * returning '&lt;' the first byte. This helps preventing errors in components
	 * that take input streams as a data source, but cannot handle byte order marks.
	 * For instance, this wrapper prevents the "content not allowed in prolog"
	 * exception thrown by Java's XML components. Using this wrapper with data
	 * other than XML or HTML is likely to cause undesired behavior.
	 */
	public static class ByteOrderMarkFilterInputStream extends FilterInputStream {
		private boolean inContent = false;
		
		/**
		 * Constructor
		 * @param in the input stream to wrap
		 */
		public ByteOrderMarkFilterInputStream(InputStream in) {
			super(in);
		}
		
		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#read()
		 */
		public int read() throws IOException {
			int i = super.read();
			while (!this.inContent) {
				if (i == '<') this.inContent = true;
				else i = super.read();
			}
			return i;
		}

		/* (non-Javadoc)
		 * @see java.io.FilterInputStream#read(byte[], int, int)
		 */
		public int read(byte[] b, int off, int len) throws IOException {
			if (this.inContent)	return super.read(b, off, len);
			else {
				int i = super.read();
				while (!this.inContent) {
					if (i == '<')
						this.inContent = true;
					else if (i == -1)
						return -1;
					else i = super.read();
				}
				b[off] = ((byte) i);
				return (1 + super.read(b, (off + 1), (len - 1)));
			}
		}
	}
	
	//	!!! for test purposes only !!!
	public static void main(String[] args) throws Exception {
		File cXsltFile1 = new File("./Components/GgServerPlaziWCSData/gg2wiki.xslt");
		getTransformer(cXsltFile1);
		getTransformer(cXsltFile1, false);
		if (true)
			return;
		
//		File xsltFile1 = new File("E:/Projektdaten/XSLT Round Trip/gg2taxonx.xsl");
//		File xsltFile2 = new File("E:/Projektdaten/XSLT Round Trip/taxonx2gg.xsl");
//		Transformer trf1 = getTransformer(xsltFile1);
//		Transformer trf2 = getTransformer(xsltFile2);
//		
////		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/21211_gg1.xml"));
////		Reader reader = new AnnotationReader(doc);
////		reader = chain(reader, trf1);
////		reader = chain(reader, trf2);
////		reader = chain(reader, trf1);
////		
////		char[] buffer = new char[1024];
////		int read;
////		while ((read = reader.read(buffer, 0, buffer.length)) != -1)
////			System.out.print(new String(buffer, 0, read));
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new File("E:/Projektdaten/TaxonxTest/21211_gg1.xml"));
//		Writer writer = new OutputStreamWriter(System.out);
//		writer = wrap(writer, trf2);
//		writer = wrap(writer, trf1);
//		AnnotationUtils.writeXML(doc, writer, null, null, true);
//		writer.close();
	}
	
	/**
	 * Produce an InputStream that provides the output of a given XSLT
	 * transformer whose input is provided by a given InputStream. This method
	 * will use an extra Thread for sending the data from the source InputStream
	 * into the transformer. This thread is started soon as the read() method of
	 * the returned InputStream is invoked for the first time. If the argument
	 * transformer is a pooled transformer obtained from a transformer pool, it
	 * is handed back to the pool after the transformation is finished.
	 * @param source the InputStream to read data from
	 * @param transformer the transformer to run the data through
	 * @return an InputStream providing the output of the specified Transformer,
	 *         or the argument InputStream, if the specified Transformer is null
	 * @throws IOException
	 */
	public static InputStream chain(final InputStream source, final Transformer transformer) throws IOException {
		if (transformer == null) return source;
		
		final Object handshake = new Object();
		final IOException[] exception = {null};
		
		final PipedOutputStream pos = new PipedOutputStream() {
			private boolean firstWrite = true;
			public void write(int c) throws IOException {
				this.wakeReader();
				super.write(c);
			}
			public void write(byte[] b, int off, int len) throws IOException {
				this.wakeReader();
				super.write(b, off, len);
			}
			public void write(byte[] b) throws IOException {
				this.wakeReader();
				super.write(b);
			}
			public void close() throws IOException {
				this.wakeReader();
				super.close();
			}
			public synchronized void flush() throws IOException {
				this.wakeReader();
				super.flush();
			}
			private void wakeReader() {
				if (this.firstWrite) 
					synchronized (handshake) {
						this.firstWrite = false;
//						System.out.println("First data arrived, waking up reader");
						handshake.notify();
					}
			}
			/*
			 * no need for overwriting the other methods for writing, they all
			 * refer to the one-argument and three-argument write() methods
			 */
		};
		
		final Thread t = new Thread() {
			public void run() {
				try {
					try {
//						System.out.println("Start processing input");
						transformer.transform(new StreamSource(source), new StreamResult(pos));
//						System.out.println("Done processing input");
						pos.flush();
						pos.close();
					}
					catch (TransformerException te) {
						throw new IOException(te.getMessageAndLocation());
					}
				}
				catch (IOException e) {
					exception[0] = e;
				}
				finally {
					if (transformer instanceof PooledTransformer)
						((PooledTransformer) transformer).handBack();
				}
			}
		};
		
		return new PipedInputStream(pos) {
			private boolean firstRead = true;
			public synchronized int read() throws IOException {
				if (exception[0] != null)
					throw exception[0];
				
				if (this.firstRead) {
					this.firstRead = false;
					synchronized (handshake) {
						t.start();
//						System.out.println("Transformer started, waiting for data to come ...");
						try {
							handshake.wait();
						} catch (InterruptedException ie) {}
//						System.out.println("First data arrived, resuming");
					}
				}
				
				int r = super.read();
				
				if (r < 0) try {
					t.join();
				} catch (InterruptedException ie) {}
				
				return r;
			}
			/*
			 * no need for overwriting the other methods for reading, they all
			 * refer to the no-argument read() method
			 */
		};
	}
	
	/**
	 * Produce a Reader that provides the output of a given XSLT transformer
	 * whose input is provided by a given Reader. This method will use an extra
	 * Thread for sending the data from the source Reader into the transformer.
	 * This thread is started soon as the read() method of the returned Reader
	 * is invoked for the first time. If the argument transformer is a pooled
	 * transformer obtained from a transformer pool, it is handed back to the
	 * pool after the transformation is finished.
	 * @param source the Reader to read data from
	 * @param transformer the transformer to run the data through
	 * @return a Reader providing the output of the specified Transformer, or
	 *         the argument reader, if the specified Transformer is null
	 * @throws IOException
	 */
	public static Reader chain(final Reader source, final Transformer transformer) throws IOException {
		if (transformer == null) return source;
		
		final Object handshake = new Object();
		final IOException[] exception = {null};
		
		final PipedWriter pw = new PipedWriter() {
			private boolean firstWrite = true;
			public void write(char[] cbuf, int off, int len) throws IOException {
				this.wakeReader();
				super.write(cbuf, off, len);
			}
			public void write(int c) throws IOException {
				this.wakeReader();
				super.write(c);
			}
			public void close() throws IOException {
				this.wakeReader();
				super.close();
			}
			public synchronized void flush() throws IOException {
				this.wakeReader();
				super.flush();
			}
			private void wakeReader() {
				if (this.firstWrite) 
					synchronized (handshake) {
						this.firstWrite = false;
//						System.out.println("First data arrived, waking up reader");
						handshake.notify();
					}
			}
			
			/*
			 * no need for overwriting the other methods for writing, they all
			 * refer to the one-argument and three-argument write() methods
			 */
		};
		
		final Thread t = new Thread() {
			public void run() {
				try {
					try {
//						System.out.println("Start processing input");
						transformer.transform(new StreamSource(source), new StreamResult(pw));
//						System.out.println("Done processing input");
						pw.flush();
						pw.close();
					}
					catch (TransformerException te) {
						throw new IOException(te.getMessageAndLocation());
					}
				}
				catch (IOException e) {
					exception[0] = e;
				}
				finally {
					if (transformer instanceof PooledTransformer)
						((PooledTransformer) transformer).handBack();
				}
			}
		};
		
		return new PipedReader(pw) {
			private boolean firstRead = true;
			public synchronized int read() throws IOException {
				if (exception[0] != null)
					throw exception[0];
				
				if (this.firstRead) {
					this.firstRead = false;
					synchronized (handshake) {
						t.start();
//						System.out.println("Transformer started, waiting for data to come ...");
						try {
							handshake.wait();
						} catch (InterruptedException ie) {}
//						System.out.println("First data arrived, resuming");
					}
				}
				
				int r = super.read();
				
				if (r < 0) try {
					t.join();
				} catch (InterruptedException ie) {}
				
				return r;
			}
			/*
			 * no need for overwriting the other methods for reading, they all
			 * refer to the no-argument read() method
			 */
		};
	}
	
	/**
	 * An IsolatorOutputStream prevents its contained OutputStream from being
	 * flushed and closed, by overwriting the respective methods to do nothing.
	 * This provides a means of preventing the data flow to a given output
	 * stream from being terminated after an XSLT transformer has written its
	 * output to it.
	 * 
	 * @author sautter
	 */
	public static class IsolatorOutputStream extends FilterOutputStream {
		private boolean isolateFlush;
		
		/**
		 * Constructor
		 * @param out the output stream to isolate
		 */
		public IsolatorOutputStream(OutputStream out) {
			this(out, true);
		}
		
		/**
		 * Constructor
		 * @param out the output stream to isolate
		 * @param isolateFlush isolate the wrapped stream's flush method?
		 */
		public IsolatorOutputStream(OutputStream out, boolean isolateFlush) {
			super(out);
			this.isolateFlush = isolateFlush;
		}
		
		/**
		 * Acording to the purpose of this class, this method does nothing.
		 * @see java.io.FilterOutputStream#flush()
		 */
		public void flush() throws IOException {
			if (!this.isolateFlush)
				super.flush();
		}
		
		/**
		 * Acording to the purpose of this class, this method does nothing.
		 * @see java.io.FilterOutputStream#close()
		 */
		public void close() throws IOException {}
	}

	/**
	 * Wrap a given OutputStream in another OutputStream that sends data through
	 * a given XSLT transformer before writing to the argument stream. This
	 * method will use an extra Thread for sending the data from the returned
	 * OutputStream through the transformer. This thread is started soon as the
	 * write() method of the returned OutputStream is invoked for the first
	 * time. The argument OutputStream will be flushed and closed after
	 * transformation is finished, i.e., when the returned OutputStream is
	 * closed. The returned OutputStream must be closed (flushes automatically)
	 * after all to-transform data has been written to the returned OutputStream
	 * in order for the transformation thread to finish. If a given OutputStream
	 * should not be flushed and closed after the transformation, e.g. if
	 * additional data is to be written after the transformation output, wrap an
	 * IsolatorOutputStream around that OutputStream before handing it to this
	 * method. If the argument transformer is a pooled transformer obtained from
	 * a transformer pool, it is handed back to the pool after the argument
	 * writer is closed.
	 * @param target the OutputStream to write data to
	 * @param transformer the transformer to run the data through
	 * @return an OutputStream writing to the specified transformer, or the
	 *         argument OutputStream, if the specified Transformer is null
	 * @throws IOException
	 */
	public static OutputStream wrap(final OutputStream target, final Transformer transformer) throws IOException {
		if (transformer == null) return target;
		
		final IOException[] exception = {null};
		
		final PipedInputStream pis = new PipedInputStream();
		
		final Thread t = new Thread() {
			public void run() {
				try {
					try {
//						System.out.println("Start processing input");
						transformer.transform(new StreamSource(pis), new StreamResult(target));
//						System.out.println("Done processing input");
						target.flush();
						target.close();
					}
					catch (TransformerException te) {
						throw new IOException(te.getMessageAndLocation());
					}
				}
				catch (IOException e) {
					exception[0] = e;
				}
				finally {
					if (transformer instanceof PooledTransformer)
						((PooledTransformer) transformer).handBack();
				}
			}
		};
		
		return new PipedOutputStream(pis) {
			private boolean firstWrite = true;
			public void write(int c) throws IOException {
				this.wakeWriter();
				super.write(c);
			}
			public void write(byte[] b, int off, int len) throws IOException {
				this.wakeWriter();
				super.write(b, off, len);
			}
			public void write(byte[] b) throws IOException {
				this.wakeWriter();
				super.write(b);
			}
			public void close() throws IOException {
				this.wakeWriter();
				try {
					super.flush();
				} catch (Exception e) {}
				super.close();
				try {
					t.join();
//					System.out.println("Transformer finished");
				} catch (InterruptedException ie) {}
			}
			public synchronized void flush() throws IOException {
				this.wakeWriter();
				super.flush();
			}
			private void wakeWriter() throws IOException {
				if (exception[0] != null)
					throw exception[0];
				
				if (this.firstWrite) {
					this.firstWrite = false;
					t.start();
//					System.out.println("Transformer started");
				}
			}
			/*
			 * no need for overwriting the other methods for writing, they all
			 * refer to the one-argument and three-argument write() methods
			 */
		};
	}
	
	/**
	 * An IsolatorWriter prevents its contained Writer from being flushed and
	 * closed, by overwriting the respective methods to do nothing. This
	 * provides a means of preventing the data flow to a given writer from being
	 * terminated after an XSLT transformer has written its output to it.
	 * 
	 * @author sautter
	 */
	public static class IsolatorWriter extends FilterWriter {
		private boolean isolateFlush;
		
		/**
		 * Constructor
		 * @param out the writer to isolate
		 */
		public IsolatorWriter(Writer out) {
			this(out, true);
		}
		
		/**
		 * Constructor
		 * @param out the writer to isolate
		 * @param isolateFlush isolate the wrapped writer's flush method?
		 */
		public IsolatorWriter(Writer out, boolean isolateFlush) {
			super(out);
			this.isolateFlush = isolateFlush;
		}
		
		/**
		 * Acording to the purpose of this class, this method does nothing.
		 * @see java.io.FilterWriter#flush()
		 */
		public void flush() throws IOException {
			if (!this.isolateFlush)
				super.flush();
		}
		
		/**
		 * Acording to the purpose of this class, this method does nothing.
		 * @see java.io.FilterWriter#close()
		 */
		public void close() throws IOException {}
	}
	
	/**
	 * Wrap a given Writer in another Writer that sends data through a given
	 * XSLT transformer before writing to the argument writer. This method will
	 * use an extra Thread for sending the data from the returned Writer through
	 * the transformer. This thread is started soon as the write() method of the
	 * returned Writer is invoked for the first time. The argument Writer will
	 * be flushed and closed after transformation is finished, i.e., when the
	 * returned Writer is closed. The returned Writer must be closed (flushes
	 * automatically) after all to-transform data has been written to the
	 * returned Writer in order for the transformation thread to finish. If a
	 * given Writer should not be flushed and closed after the transformation,
	 * e.g. if additional data is to be written after the transformation output,
	 * wrap an IsolatorWriter around that Writer before handing it to this
	 * method. If the argument transformer is a pooled transformer obtained from
	 * a transformer pool, it is handed back to the pool after the argument
	 * writer is closed.
	 * @param target the Writer to write data to
	 * @param transformer the transformer to run the data through
	 * @return a Writer writing to the specified transformer, or the argument
	 *         Writer, if the specified Transformer is null
	 * @throws IOException
	 */
	public static Writer wrap(final Writer target, final Transformer transformer) throws IOException {
		if (transformer == null) return target;
		
		final IOException[] exception = {null};
		
		final PipedReader pr = new PipedReader();
		
		final Thread t = new Thread() {
			public void run() {
				try {
					try {
//						System.out.println("Start processing input");
						transformer.transform(new StreamSource(pr), new StreamResult(target));
//						System.out.println("Done processing input");
						target.flush();
						target.close();
					}
					catch (TransformerException te) {
						te.printStackTrace(System.out);
						throw new IOException(te.getMessageAndLocation());
					}
				}
				catch (IOException e) {
					exception[0] = e;
				}
				finally {
					if (transformer instanceof PooledTransformer)
						((PooledTransformer) transformer).handBack();
				}
			}
		};
		
		return new PipedWriter(pr) {
			private boolean firstWrite = true;
			public void write(int c) throws IOException {
				this.wakeWriter();
				super.write(c);
			}
			public void write(char[] cbuf, int off, int len) throws IOException {
				this.wakeWriter();
				super.write(cbuf, off, len);
			}
			public void close() throws IOException {
				this.wakeWriter();
				try {
					super.flush();
				} catch (Exception e) {}
				super.close();
				try {
					t.join();
//					System.out.println("Transformer finished");
				} catch (InterruptedException ie) {}
			}
			public synchronized void flush() throws IOException {
				this.wakeWriter();
				super.flush();
			}
			private void wakeWriter() throws IOException {
				if (exception[0] != null)
					throw exception[0];
				
				if (this.firstWrite) {
					this.firstWrite = false;
					t.start();
//					System.out.println("Transformer started");
				}
			}
			/*
			 * no need for overwriting the other methods for writing, they all
			 * refer to the one-argument and three-argument write() methods
			 */
		};
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet located at a URL.
	 * @param xsltUrl the URL of the stylesheet to load
	 * @return an XSLT transformer produced from the stylesheet located at the
	 *         specified URL
	 * @throws IOException
	 */
	public static TransformerPool getTransformer(URL xsltUrl) throws IOException {
		return getTransformer(xsltUrl, true);
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet located at a URL.
	 * @param xsltUrl the URL of the stylesheet to load
	 * @param allowCache allow using cached transformers? Specifying false
	 *            forces loading the transformer from the specified URL.
	 * @return an XSLT transformer produced from the stylesheet located at the
	 *         specified URL
	 * @throws IOException
	 */
	public static TransformerPool getTransformer(URL xsltUrl, boolean allowCache) throws IOException {
		if (xsltUrl == null)
			return null;
		InputStream xsltIn = new OnDemandInputStream(xsltUrl);
		try {
			return getTransformer(xsltUrl.toString(), xsltIn, allowCache);
		}
		finally {
			xsltIn.close();
		}
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet located in a file.
	 * @param xsltFile the file containing the stylesheet to load
	 * @return an XSLT transformer produced from the stylesheet located in the
	 *         specified file
	 * @throws IOException
	 */
	public static TransformerPool getTransformer(File xsltFile) throws IOException {
		return getTransformer(xsltFile, true);
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet located in a file.
	 * @param xsltFile the file containing the stylesheet to load
	 * @param allowCache allow using cached transformers? Specifying false
	 *            forces loading the transformer from the specified file.
	 * @return an XSLT transformer produced from the stylesheet located in the
	 *         specified file
	 * @throws IOException
	 */
	public static TransformerPool getTransformer(File xsltFile, boolean allowCache) throws IOException {
		if (xsltFile == null)
			return null;
		InputStream xsltIn = new OnDemandInputStream(xsltFile);
		try {
			return getTransformer(xsltFile.getAbsolutePath(), xsltIn, allowCache);
		}
		finally {
			xsltIn.close();
		}
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet located in a file or
	 * at a URL. If the specified address starts with 'http://', it is
	 * interpreted as a URL, otherwise as a file name.
	 * @param xsltAddress the address containing the stylesheet to load
	 * @return an XSLT transformer produced from the stylesheet located at the
	 *         specified address
	 * @throws IOException
	 */
	public static TransformerPool getTransformer(String xsltAddress) throws IOException {
		return getTransformer(xsltAddress, true);
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet located in a file or
	 * at a URL. If the specified address starts with 'http://', it is
	 * interpreted as a URL, otherwise as a file name.
	 * @param xsltAddress the address containing the stylesheet to load
	 * @param allowCache allow using cached transformers? Specifying false
	 *            forces loading the transformer from the specified address.
	 * @return an XSLT transformer produced from the stylesheet located at the
	 *         specified address
	 * @throws IOException
	 */
	public static synchronized TransformerPool getTransformer(String xsltAddress, boolean allowCache) throws IOException {
		InputStream xsltIn;
		if (xsltAddress.startsWith("http://"))
			xsltIn = new OnDemandInputStream(new URL(xsltAddress));
		else xsltIn = new OnDemandInputStream(new File(xsltAddress));
		try {
			return getTransformer(xsltAddress, xsltIn, allowCache);
		}
		finally {
			xsltIn.close();
		}
	}
	
	/* this class allows us to open an actual input stream only after a cache miss */
	private static class OnDemandInputStream extends InputStream {
		private final File file;
		private final URL url;
		private InputStream in;
		OnDemandInputStream(File file) {
			this.file = file;
			this.url = null;
		}
		OnDemandInputStream(URL url) {
			this.file = null;
			this.url = url;
		}
		private InputStream getInputStream() throws IOException {
			if (this.in != null)
				return this.in;
			if (this.file != null)
				this.in = new FileInputStream(this.file);
			if (this.url != null)
				this.in = this.url.openStream();
			return this.in;
		}
		public int read() throws IOException {
			return this.getInputStream().read();
		}
		public int read(byte[] b) throws IOException {
			return this.getInputStream().read(b);
		}
		public int read(byte[] b, int off, int len) throws IOException {
			return this.getInputStream().read(b, off, len);
		}
		public long skip(long n) throws IOException {
			return this.getInputStream().skip(n);
		}
		public int available() throws IOException {
			return this.getInputStream().available();
		}
		public void close() throws IOException {
			if (this.in != null)
				this.in.close();
		}
		public synchronized void mark(int readlimit) {
			try {
				this.getInputStream().mark(readlimit);
			} catch (IOException ioe) {}
		}
		public synchronized void reset() throws IOException {
			this.getInputStream().reset();
		}
		public boolean markSupported() {
			try {
				return this.getInputStream().markSupported();
			}
			catch (IOException ioe) {
				return false;
			}
		}
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet loaded from some input
	 * stream. If the agrument name is null, there are no cache lookups, and the
	 * transformer pool will not be cached.
	 * @param xsltIn an input stream to load the stylesheet from
	 * @param name the name of the input stream, for caching
	 * @return an XSLT transformer produced from the specified stylesheet
	 * @throws IOException
	 */
	public static synchronized TransformerPool getTransformer(String name, InputStream xsltIn) throws IOException {
		return getTransformer(name, xsltIn, true);
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet loaded from some input
	 * stream. If the agrument name is null, there are no cache lookups, and the
	 * transformer pool will not be cached.
	 * @param xsltIn an input stream to load the stylesheet from
	 * @param name the name of the input stream, for caching
	 * @param allowCache allow using cached transformers? Specifying false
	 *            forces loading the transformer from the specified address.
	 * @return an XSLT transformer produced from the specified stylesheet
	 * @throws IOException
	 */
	public static synchronized TransformerPool getTransformer(String name, InputStream xsltIn, boolean allowCache) throws IOException {
		if (allowCache && (name != null) && transformerCache.containsKey(name)) {
//			System.out.println("XsltUtils: XSL Transformer Pool cache hit for '" + name + "'");
			return ((TransformerPool) transformerCache.get(name));
		}
		InputStream tis = new ByteOrderMarkFilterInputStream(xsltIn);
		ByteArrayOutputStream xsltBytes = new ByteArrayOutputStream();
		int bytesRead;
		byte[] byteBuffer = new byte[1024];
		while ((bytesRead = tis.read(byteBuffer, 0, byteBuffer.length)) != -1)
			xsltBytes.write(byteBuffer, 0, bytesRead);
		return doGetTransformer(name, xsltBytes.toByteArray(), allowCache);
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet loaded from some
	 * reader. If the agrument name is null, there are no cache lookups, and the
	 * transformer pool will not be cached.
	 * @param xsltIn a reader to load the stylesheet from
	 * @param name the name of the input stream, for caching
	 * @return an XSLT transformer produced from the specified stylesheet
	 * @throws IOException
	 */
	public static synchronized TransformerPool getTransformer(String name, Reader xsltIn) throws IOException {
		return getTransformer(name, xsltIn, true);
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet loaded from some
	 * reader. If the agrument name is null, there are no cache lookups, and the
	 * transformer pool will not be cached.
	 * @param xsltIn a reader to load the stylesheet from
	 * @param name the name of the input stream, for caching
	 * @param allowCache allow using cached transformers? Specifying false
	 *            forces loading the transformer from the specified address.
	 * @return an XSLT transformer produced from the specified stylesheet
	 * @throws IOException
	 */
	public static synchronized TransformerPool getTransformer(String name, Reader xsltIn, boolean allowCache) throws IOException {
		if (allowCache && (name != null) && transformerCache.containsKey(name)) {
//			System.out.println("XsltUtils: XSL Transformer Pool cache hit for '" + name + "'");
			return ((TransformerPool) transformerCache.get(name));
		}
		BufferedReader xsltReader = ((xsltIn instanceof BufferedReader) ? ((BufferedReader) xsltIn) : new BufferedReader(xsltIn));
		ByteArrayOutputStream xsltBytes = new ByteArrayOutputStream();
		String xsltLine;
		while ((xsltLine = xsltReader.readLine()) != null) {
			if (xsltBytes.size() == 0) {
				if (xsltLine.trim().length() == 0)
					continue;
				if (xsltLine.indexOf('<') > 0)
					xsltLine = xsltLine.substring(xsltLine.indexOf('<'));
			}
			xsltBytes.write(xsltLine.getBytes("UTF-8"));
			xsltBytes.write('\r');
			xsltBytes.write('\n');
		}
		return doGetTransformer(name, xsltBytes.toByteArray(), allowCache);
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet. If the agrument name
	 * is null, there are no cache lookups, and the transformer pool will not be
	 * cached.
	 * @param name the name of the input stream, for caching
	 * @param xsltBytes an array holding the bytes of the stylesheet to load
	 * @return an XSLT transformer produced from the specified stylesheet
	 * @throws IOException
	 */
	public static synchronized TransformerPool getTransformer(String name, byte[] xsltBytes) throws IOException {
		return getTransformer(name, xsltBytes, true);
	}
	
	/**
	 * Produce an XSLT transformer pool from a stylesheet. If the agrument name
	 * is null, there are no cache lookups, and the transformer pool will not be
	 * cached.
	 * @param name the name of the input stream, for caching
	 * @param xsltBytes an array holding the bytes of the stylesheet to load
	 * @param allowCache allow using cached transformers? Specifying false
	 *            forces loading the transformer from the specified address.
	 * @return an XSLT transformer produced from the specified stylesheet
	 * @throws IOException
	 */
	public static synchronized TransformerPool getTransformer(String name, byte[] xsltBytes, boolean allowCache) throws IOException {
		byte[] cXsltBytes = new byte[xsltBytes.length];
		System.arraycopy(xsltBytes, 0, cXsltBytes, 0, xsltBytes.length);
		return doGetTransformer(name, cXsltBytes, allowCache);
	}
	
	/*
	 * this extra method is required so we can copy byte arrays that external
	 * code might have a reference to, but do not need to copy byte arrays we've
	 * read from some stream to achieve this.
	 */
	private static synchronized TransformerPool doGetTransformer(String name, byte[] xsltBytes, boolean allowCache) throws IOException {
		if (allowCache && (name != null) && transformerCache.containsKey(name)) {
//			System.out.println("XsltUtils: XSL Transformer Pool cache hit for '" + name + "'");
			return ((TransformerPool) transformerCache.get(name));
		}
		try {
			TransformerPool tp = new TransformerPool(xsltBytes);
			tp.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//			System.out.println("XsltUtils: loaded XSL Transformer Pool from '" + name + "'");
			if (name != null)
				transformerCache.put(name, tp);
			return tp;
		}
		catch (Exception e) {
			throw new IOException(e.getClass().getName() + " (" + e.getMessage() + ") while creating XSL Transformer Pool from '" + name + "'.");
		}
	}
//	public static synchronized Transformer getTransformer(String xsltAddress, boolean allowCache) throws IOException {
//		
//		if (allowCache && transformerCache.containsKey(xsltAddress)) {
//			System.out.println("XsltUtils: transformer cache hit for '" + xsltAddress + "'");
//			return ((Transformer) transformerCache.get(xsltAddress));
//		}
//		
//		if (transformerFactory == null) {
//			transformerFactory = TransformerFactory.newInstance();
//			System.out.println("XsltUtils: transformer factory instantiated");
//		}
//		
//		try {
//			InputStream tis = new ByteOrderMarkFilterInputStream(
//					xsltAddress.startsWith("http://") ? 
//						new URL(xsltAddress).openStream()
//						:
//						new FileInputStream(new File(xsltAddress))
//					);
//			Transformer transformer = transformerFactory.newTransformer(new StreamSource(new InputStreamReader(tis, "UTF-8")));
//			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//			tis.close();
//			System.out.println("XsltUtils: loaded transformer from '" + xsltAddress + "'");
//			
//			if ((transformer == transformerCache.put(xsltAddress, transformer)) && !allowCache) {
//				transformerFactory = null;
//				System.out.println("XsltUtils: transformer factory caches transformer instances, setting factory to null and recursing.");
//				return getTransformer(xsltAddress, allowCache);
//			}
//			else return transformer;
//		}
//		
//		catch (Exception e) {
//			throw new IOException(e.getClass().getName() + " (" + e.getMessage() + ") while creating XSL Transformer from '" + xsltAddress + "'.");
//		}
//	}
	
	private static HashMap transformerCache = new HashMap();
	
	private static boolean transformerFactoryTestedForCaching = false;
	private static TransformerFactory transformerFactory = null;
	
	private static synchronized Transformer produceTransformer(byte[] stylesheet) throws TransformerConfigurationException {
		
		//	we have tested the installed factory for caching behavior and not made it available, so it is caching 
		if (transformerFactoryTestedForCaching)
			return null;
		
		//	chaching behavior yet to be tested, do it right now
		try {
			System.out.println("XsltUtils: testing XSL Transformer Factory for internal caching behavior ...");
			TransformerFactory tf = TransformerFactory.newInstance();
			System.out.println(" - got XSL Transformer Factory instance");
			Transformer t1 = tf.newTransformer(new StreamSource(new InputStreamReader(new ByteArrayInputStream(stylesheet), "UTF-8")));
			System.out.println(" - got XSL Transformer instance 1");
			Transformer t2 = tf.newTransformer(new StreamSource(new InputStreamReader(new ByteArrayInputStream(stylesheet), "UTF-8")));
			System.out.println(" - got XSL Transformer instance 2");
			if (t1 != t2) {
				System.out.println(" - found factory to not be caching internally, can use centralized instance");
				transformerFactory = tf;
			}
			else System.out.println(" - found factory to be caching internally, cannot use centralized instance");
			transformerFactoryTestedForCaching = true;
			return t1;
		}
		catch (UnsupportedEncodingException uee) {
			return null; // not going to happen with UTF-8, but Java don't know ...
		}
	}
	
	/**
	 * This class mimics the interface of javax.xml.transform.Transformer. As
	 * opposed to the latter transformers, however, instances of this class
	 * <b>are</b> safe to use by multiple threads concurrently. They hold a pool
	 * of actual transformers internally, making sure each is used by only one
	 * thread at a time. The size of the internal transformer pool can be
	 * modified via the setTransformerPoolSize() method. Larger pool sizes
	 * result in better performance in high-load situations because no extra
	 * transformers have to be created to meet demand. However, this comes at
	 * the cost of some memory for keeping the transformers. The default pool
	 * size is 3. However, new transformers are only created if all exesting
	 * ones are in use when another transformer is needed.<br>
	 * Parameters and properties set with this class are set with all
	 * transformers in the pool. Thus, implementations with highly
	 * purpose-specific parameters and properties should not use this class
	 * directly for their transformations, but instead retrieve an actual
	 * transformer from the getTransformer() method.<br>
	 * Error handlers are not accessible from this class directly. Thus,
	 * implementations requiring access to error handlers should not use this
	 * class directly for their transformations, but instead retrieve an actual
	 * transformer from the getTransformer() method.
	 * 
	 * @author sautter
	 */
	public static class TransformerPool extends Transformer {
		
		private byte[] stylesheet;
		private Transformer model;
		
		private HashMap parameters = new HashMap(3);
		private Properties properties;
		private URIResolver uriResolver;
		
		private LinkedList transformerPool = new LinkedList();
		private int transformerPoolSize = 3;
		
		TransformerPool(byte[] stylesheet) throws TransformerConfigurationException {
			this.stylesheet = stylesheet;
			this.model = this.produceTransformer();
			this.properties = new Properties(this.model.getOutputProperties());
		}

		private Transformer produceTransformer() throws TransformerConfigurationException {
			try {
				//	we have a centralized transformer factory, so we know it does not cache
				if (transformerFactory != null)
					return transformerFactory.newTransformer(new StreamSource(new InputStreamReader(new ByteArrayInputStream(stylesheet), "UTF-8")));
				
				//	chaching behavior yet to be tested, do it right now
				if (!transformerFactoryTestedForCaching)
					return XsltUtils.produceTransformer(this.stylesheet);
				
				/*
				 * we have to create a new transformer factory for every
				 * transformer because installed factory implementation caches
				 * transformer instances internally, which effectively prevents
				 * the parallel existence of multiple transformers in the pool
				 */
				return TransformerFactory.newInstance().newTransformer(new StreamSource(new InputStreamReader(new ByteArrayInputStream(this.stylesheet), "UTF-8")));
			}
			catch (UnsupportedEncodingException uee) {
				return null; // not going to happen with UTF-8, but Java don't know ...
			}
		}
		
		/**
		 * Obtain a transformer from the pool for explicit configuration an use.
		 * If no transformer is available in the pool due to heavy use, a new
		 * transformer is created.
		 * @return a transfromer from the pool
		 */
		public synchronized PooledTransformer getTransformer() {
//			System.out.println("TransformerPool: retrieving transformer ...");
			PooledTransformer pt;
			try {
				if (this.transformerPool.isEmpty()) {
					pt = new PooledTransformer(this, this.produceTransformer());
//					System.out.println(" - transformer created, pool was empty");
				}
				else {
					pt = ((PooledTransformer) this.transformerPool.removeFirst());
//					System.out.println(" - transformer found in pool");
				}
			}
			catch (TransformerConfigurationException tce) {
				return null; // not going to happen after constructor goes OK, but Java don't know ...
			}
			pt.prepare(this.parameters, this.properties, this.uriResolver);
//			System.out.println(" - transformer prepared");
			return pt;
		}
		
		synchronized void handBack(PooledTransformer pt) {
//			System.out.println("TransformerPool: getting back transformer ...");
			if (this.transformerPool.size() < this.transformerPoolSize) {
				this.transformerPool.addLast(pt);
//				System.out.println(" - transformer stored in pool");
			}
//			else System.out.println(" - transformer discarded, pool was full");
		}
		
		/**
		 * @return the number of transformers currently in the pool
		 */
		public int getTransformerPoolLevel() {
			return this.transformerPool.size();
		}
		
		/**
		 * @return the size of the transformer pool, i.e., the maximum number of
		 *         transformers the pool may contain
		 */
		public int getTransformerPoolSize() {
			return this.transformerPoolSize;
		}
		
		/**
		 * Set the maximum size the maximum number of transformers the pool may
		 * contain. Larger pool sizes result in better performance in high-load
		 * situations because no extra transformers have to be created to meet
		 * demand. However, this comes at the cost of some memory for keeping
		 * the transformers. The default pool size is 3.
		 * @param tps the new transformer pool size (must be 1 or greater)
		 */
		public void setTransformerPoolSize(int tps) {
			if (tps < 1)
				throw new IllegalArgumentException("Transformer pool size cannot be less than 1.");
			this.transformerPoolSize = tps;
			while (this.transformerPool.size() > this.transformerPoolSize)
				this.transformerPool.removeFirst();
		}
		
		/**
		 * This implementation retrieves a transformer from the pool, uses it,
		 * and returns it to the pool by calling its handBack() method.
		 * @see javax.xml.transform.Transformer#transform(javax.xml.transform.Source, javax.xml.transform.Result)
		 */
		public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
			PooledTransformer pt = this.getTransformer();
			try {
				pt.transform(xmlSource, outputTarget);
			}
			finally {
				pt.handBack();
			}
		}
		
		/**
		 * This implementation works on the centralized parameter store of the
		 * transformer pool. The parameters are put into individual tranformers
		 * right before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#getParameter(java.lang.String)
		 */
		public synchronized Object getParameter(String name) {
			return this.parameters.get(name);
		}
		
		/**
		 * This implementation works on the centralized parameter store of the
		 * transformer pool. The parameters are put into individual tranformers
		 * right before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#setParameter(java.lang.String, java.lang.Object)
		 */
		public synchronized void setParameter(String name, Object value) {
			this.parameters.put(name, value);
		}
		
		/**
		 * This implementation works on the centralized parameter store of the
		 * transformer pool. The parameters are put into individual tranformers
		 * right before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#clearParameters()
		 */
		public synchronized void clearParameters() {
			this.parameters.clear();
		}
		
		/**
		 * This implementation set the centralized URI resolver of the
		 * transformer pool, which is put into individual tranformers right
		 * before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#setURIResolver(javax.xml.transform.URIResolver)
		 */
		public synchronized void setURIResolver(URIResolver resolver) {
			this.uriResolver = resolver;
		}
		
		/**
		 * This implementation returns the centralized URI resolver of the
		 * transformer pool, which is put into individual tranformers right
		 * before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#getURIResolver()
		 */
		public synchronized URIResolver getURIResolver() {
			return this.uriResolver;
		}
		
		/**
		 * This implementation writes to the centralized property store of the
		 * transformer pool, which also contains the properties set in the
		 * stylesheet. The properties are put into individual tranformers right
		 * before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#setOutputProperties(java.util.Properties)
		 */
		public synchronized void setOutputProperties(Properties oformat) {
			if (oformat == null)
				this.properties.clear();
			else this.properties.putAll(oformat);
		}
		
		/**
		 * This implementation returns a view of the centralized property store
		 * of the transformer pool, which also contains the properties set in
		 * the stylesheet. The properties are put into individual tranformers
		 * right before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#getOutputProperties()
		 */
		public synchronized Properties getOutputProperties() {
			return new Properties(this.properties);
		}
		
		/**
		 * This implementation works on the centralized property store of the
		 * transformer pool, which also contains the properties set in the
		 * stylesheet. The properties are put into individual tranformers right
		 * before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#setOutputProperty(java.lang.String, java.lang.String)
		 */
		public synchronized void setOutputProperty(String name, String value) throws IllegalArgumentException {
			this.properties.setProperty(name, value);
		}
		
		/**
		 * This implementation works on the centralized property store of the
		 * transformer pool. The properties are put into individual tranformers
		 * right before they are returned from the getTransformer() method, and
		 * cleared again when a pooled transformer is returned to the pool by
		 * calling its handBack() method.
		 * @see javax.xml.transform.Transformer#getOutputProperty(java.lang.String)
		 */
		public synchronized String getOutputProperty(String name) throws IllegalArgumentException {
			return this.properties.getProperty(name);
		}
		
		/**
		 * This implementation does nothing, as instances of this class do not
		 * have any transformation specific internal state. It is simply
		 * provided for interface compliance.
		 * @see javax.xml.transform.Transformer#reset()
		 */
		public void reset() {}
		
		/**
		 * This imeplementation does not have any effect because instances of
		 * this class do not perform transformations themselves but internally
		 * delegate to actual transformers held in an internal pool for
		 * multi-thread use. Implementations requiring access to error handlers
		 * should not use this class directly for their transformations, but
		 * instead retrieve an actual transformer from the getTransformer()
		 * method.
		 * @see javax.xml.transform.Transformer#setErrorListener(javax.xml.transform.ErrorListener)
		 */
		public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {}
		
		/**
		 * This imeplementation always returns null because instances of this
		 * class do not perform transformations themselves but internally
		 * delegate to actual transformers held in an internal pool for
		 * multi-thread use. Implementations requiring access to error handlers
		 * should not use this class directly for their transformations, but
		 * instead retrieve an actual transformer from the getTransformer()
		 * method.
		 * @see javax.xml.transform.Transformer#getErrorListener()
		 */
		public ErrorListener getErrorListener() {
			return null;
		}
		
		/**
		 * This class acts as a wrapper for transformers produced by the
		 * transformer factory installed in the local JVM. All methods loop
		 * through to the wrapped transformer. Instances of this class are not
		 * safe to use by multiple threads concurrently. After a piece of code
		 * is done with a pooled transformer object, it should invoke its
		 * handBack() method, which adds it back to its parent transformer pool
		 * and resets the parameters and properties to the pool defaults.
		 * 
		 * @author sautter
		 */
		public class PooledTransformer extends Transformer {
			
			private TransformerPool parent;
			private Transformer transformer;
			
			PooledTransformer(TransformerPool parent, Transformer transformer) {
				this.parent = parent;
				this.transformer = transformer;
			}
			void prepare(HashMap parameters, Properties oformat, URIResolver uriResolver) {
				for (Iterator pit = parameters.keySet().iterator(); pit.hasNext();) {
					String name = ((String) pit.next());
					this.transformer.setParameter(name, parameters.get(name));
				}
				
				for (Iterator pit = oformat.keySet().iterator(); pit.hasNext();) {
					String name = ((String) pit.next());
					this.transformer.setOutputProperty(name, oformat.getProperty(name));
				}
				
				this.transformer.setURIResolver(uriResolver);
			}
			
			/**
			 * Put the pooled transformer back into the pool. This method will
			 * also clear all parameters and properties set after retrieving the
			 * transformer from the pool, so it might behave differently
			 * afterward. Code using this class should not use a transformer
			 * after invoking this method. In fact, invoking this method should
			 * be the last thing done before letting go of the reference to the
			 * pooled trasnformer.
			 */
			public void handBack() {
				this.reset();
				this.parent.handBack(this);
			}
			public Object getParameter(String name) {
				return this.transformer.getParameter(name);
			}
			public void setParameter(String name, Object value) {
				this.transformer.setParameter(name, value);
			}
			public void clearParameters() {
				this.transformer.clearParameters();
			}
			public String getOutputProperty(String name) throws IllegalArgumentException {
				return this.transformer.getOutputProperty(name);
			}
			public void setOutputProperty(String name, String value) throws IllegalArgumentException {
				this.transformer.setOutputProperty(name, value);
			}
			public Properties getOutputProperties() {
				return this.transformer.getOutputProperties();
			}
			public void setOutputProperties(Properties oformat) {
				this.transformer.setOutputProperties(oformat);
			}
			public ErrorListener getErrorListener() {
				return this.transformer.getErrorListener();
			}
			public void setErrorListener(ErrorListener listener) throws IllegalArgumentException {
				this.transformer.setErrorListener(listener);
			}
			public URIResolver getURIResolver() {
				return this.transformer.getURIResolver();
			}
			public void setURIResolver(URIResolver resolver) {
				this.transformer.setURIResolver(resolver);
			}
			public void reset() {
				try {
					this.transformer.reset();
				} catch (UnsupportedOperationException uoe) {}
			}
			public void transform(Source xmlSource, Result outputTarget) throws TransformerException {
				this.transformer.transform(xmlSource, outputTarget);
			}
		}
	}
}
