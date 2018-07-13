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
package de.uka.ipd.idaho.htmlXmlUtil;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

/**
 * A TokenReceiver receives HTML/XML tokens from a Parser as the latter
 * processes an input stream of data. There are default implementations to
 * receive through factory methods, which collect the tokens in a StringBuffer
 * or a java.util.List or write them to a File, an OutputStream, or a Writer.
 * These TokenReceivers are available through the respective factory methods.
 * For processing tokens in a custom way, e.g. for restoring a data structure
 * from a respective XML representation, extend this class and handle tokens in
 * the way required by the surrounding application.
 * 
 * @author sautter
 */
public abstract class TokenReceiver {
	
	protected TokenReceiver() throws IOException {}
	
	/**	take over a token for storage
	 * @param	token	the token to be stored
	 * @throws 	IOException
	 */
	public abstract void storeToken(String token, int treeDepth) throws IOException;
	
	/**	close the TokenReceiver, flush an OutputSteam, etc
	 * @throws IOException
	 */
	public abstract void close() throws IOException;
	
	/**	@return	a TokenReceiver collecting tokens in the specified StringBuffer
	 */
	public static TokenReceiver getTokenReceiver(StringBuffer buffer) throws IOException {
		return new StringTokenReceiver(buffer);
	}
	
	/**	@return	a TokenReceiver collecting tokens in the specified java.util.List
	 */
	public static TokenReceiver getTokenReceiver(List list) throws IOException {
		return new ListTokenReceiver(list);
	}
	
	/**	@return	a TokenReceiver writing tokens to the specified OutputStream
	 */
	public static TokenReceiver getTokenReceiver(OutputStream output) throws IOException {
		return new StreamTokenReceiver(output);
	}
	
	/**	@return	a TokenReceiver writing tokens to the specified Writer
	 */
	public static TokenReceiver getTokenReceiver(Writer output) throws IOException {
		return new WriterTokenReceiver(output);
	}
	
	/**	@return	a TokenReceiver writing tokens to the specified File
	 */
	public static TokenReceiver getTokenReceiver(File file) throws IOException {
		return getTokenReceiver(new FileOutputStream(file));
	}
	
//	private static class PrintTokenReceiver extends TokenReceiver {
//		
//		public PrintTokenReceiver() throws IOException {}
//		
//		/**	@see	TokenReceiver#storeToken(String)
//		 */
//		public void storeToken(String token, int treeDepth) throws IOException {
//			System.out.println(treeDepth + " " + token);
//		}
//		
//		/**	@see	TokenReceiver#close()
//		 */
//		public void close() throws IOException {}
//	}

	private static class StreamTokenReceiver extends TokenReceiver {
		
		private OutputStream output;
		private BufferedWriter writer;
		
		/**	Constructor
		 * @param 	output	the OutputStream to write the tokens to
		 * @throws 	IOException
		 */
		public StreamTokenReceiver(OutputStream output) throws IOException {
			this.output = output;
			this.writer = new BufferedWriter(new OutputStreamWriter(this.output));
		}
		
		/**	@see	TokenReceiver#storeToken(String)
		 */
		public void storeToken(String token, int treeDepth) throws IOException {
			this.writer.write(token);
			this.writer.newLine();
		}
		
		/**	@see	TokenReceiver#close()
		 */
		public void close() throws IOException {
			this.writer.flush();
			this.writer.close();
		}
	}

	private static class WriterTokenReceiver extends TokenReceiver {
		
//		private Writer output;
		private BufferedWriter writer;
		
		/**	Constructor
		 * @param 	output	the Writer to write the tokens to
		 * @throws 	IOException
		 */
		public WriterTokenReceiver(Writer output) throws IOException {
//			this.output = output;
//			this.writer = new BufferedWriter(this.output);
			this.writer = ((output instanceof BufferedWriter) ? ((BufferedWriter) output) : new BufferedWriter(output));
		}
		
		/**	@see	TokenReceiver#storeToken(String)
		 */
		public void storeToken(String token, int treeDepth) throws IOException {
			this.writer.write(token);
			this.writer.newLine();
		}
		
		/**	@see	TokenReceiver#close()
		 */
		public void close() throws IOException {
			this.writer.flush();
			this.writer.close();
		}
	}

	private static class StringTokenReceiver extends TokenReceiver {
		
		private StringBuffer output; 
		
		/**	Constructor
		 * @param 	output	the StringBuffer to collect the tokens in
		 * @throws 	IOException
		 */
		public StringTokenReceiver(StringBuffer output) throws IOException {
			this.output = output;
		}
		
		/**	@see	TokenReceiver#storeToken(String)
		 */
		public void storeToken(String token, int treeDepth) throws IOException {
			this.output.append(token + "\n");
		}
		
		/**	@see	TokenReceiver#close()
		 */
		public void close() throws IOException {}
	}

	private static class ListTokenReceiver extends TokenReceiver {
		
		private List output; 
		
		/**	Constructor
		 * @param 	output	the java.util.List to collect the tokens in
		 * @throws 	IOException
		 */
		public ListTokenReceiver(List output) throws IOException {
			this.output = output;
		}
		
		/**	@see	TokenReceiver#storeToken(String)
		 */
		public void storeToken(String token, int treeDepth) throws IOException {
			this.output.add(token);
		}
		
		/**	@see	TokenReceiver#close()
		 */
		public void close() throws IOException {}
	}
}
