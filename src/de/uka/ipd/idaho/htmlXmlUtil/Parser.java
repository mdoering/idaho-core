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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Stack;
import java.util.Vector;

import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.htmlXmlUtil.exceptions.InvalidNestingException;
import de.uka.ipd.idaho.htmlXmlUtil.exceptions.MissingEndTagException;
import de.uka.ipd.idaho.htmlXmlUtil.exceptions.UnexpectedEndTagException;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * An error tolerant parser for HTML and XML files. It uses a Grammar object to
 * control the exact behavior, including if input wellformedness errors should
 * be corrected by the parser instance as far as possible, or if the parser
 * should throw an exception if encountering such an error.<br>
 * A parser can work both in a SAX-like streaming mode (the stream() methods),
 * sending out tokens to a TokenReceiver as it processed the input data stream,
 * and in a DOM-like tree building mode (the parse() methods), which return the
 * root TreeNode of the parsed input data. Internally, the Parser always builds
 * a tree, but in streaming mode, the tree is destroyed as soon as the end tag
 * of an element has been sent to the TokenReceiver.
 * 
 * @author sautter
 */
public class Parser {
	
	private final boolean correctErrors;
	private final Grammar grammar;
	
	/**	 Constructor using StandardGrammar
	 */
	public Parser() {
		this(null);
	}
	
	/**	 Constructor
	 * @param	grammar		the Grammar to use for parsing
	 */
	public Parser(Grammar grammar) {
		this.grammar = ((grammar == null) ? new StandardGrammar() : grammar);
		this.correctErrors = this.grammar.correctErrors();
	}
	
	/**	build an XML tree out of the chars provided by the specified InputStream
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(InputStream input) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), null, true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), null, true);
	}
	
	/**	build an XML tree out of the chars provided by the specified Reader
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(Reader input) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), null, true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), null, true);
	}
	
	/**	build an XML tree out of the specified String
	 * @param	input		the String containing the char sequence to be parsed
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(String input) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), null, true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), null, true);
	}
	
	/**	build an XML tree out of the chars provided by the specified InputStream, stream it to the specified OutputStream simultaneously
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(InputStream input, OutputStream output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	build an XML tree out of the chars provided by the specified Reader, stream it to the specified OutputStream simultaneously
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(Reader input, OutputStream output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	build an XML tree out of the specified String, stream it to the specified OutputStream simultaneously
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(String input, OutputStream output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	build an XML tree out of the chars provided by the specified InputStream, stream it to the specified Writer simultaneously
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the Writer to write the tokens to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(InputStream input, Writer output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	build an XML tree out of the chars provided by the specified Reader, stream it to the specified Writer simultanously
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the Writer to write the tokens to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(Reader input, Writer output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	build an XML tree out of the specified String, stream it to the specified Writer simultaneously
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the Writer to write the tokens to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(String input, Writer output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	build an XML tree out of the chars provided by the specified InputStream, stream it to the specified TokenReceiver simultaneously
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(InputStream input, TokenReceiver output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), output, true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), output, true);
	}
	
	/**	build an XML tree out of the chars provided by the specified Reader, stream it to the specified TokenReceiver simultaneously
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(Reader input, TokenReceiver output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), output, true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), output, true);
	}
	
	/**	build an XML tree out of the specified String, stream it to the specified TokenReceiver simultaneously
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 * @return	the root node of the XML tree
	 */
	public TreeNode parse(String input, TokenReceiver output) throws IOException {
//		return this.processTree(TokenSource.getTokenSource(input, this.grammar), output, true);
		return this.parse(TokenSource.getTokenSource(input, this.grammar), output, true);
	}
	
	private TreeNode parse(TokenSource input, TokenReceiver output, boolean t) throws IOException {
		ParserInstance pi = new ParserInstance(input, output, false);
		pi.consumeTokens();
		return pi.getRootNode();
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified InputStream, and write the tokens to the specified OutputStream
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 */
	public void stream(InputStream input, OutputStream output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified Reader, and write the tokens to the specified OutputStream
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 */
	public void stream(Reader input, OutputStream output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
	}
	
	/**	virtually build an XML tree out of the specified String, and write the tokens to the specified OutputStream
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 */
	public void stream(String input, OutputStream output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified InputStream, and write the tokens to the specified Writer
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the Writer to write the tree to
	 */
	public void stream(InputStream input, Writer output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified Reader, and write the tokens to the specified Writer
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the Writer to write the tree to
	 */
	public void stream(Reader input, Writer output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
	}
	
	/**	virtually build an XML tree out of the specified String, and write the tokens to the specified Writer
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the Writer to write the tree to
	 */
	public void stream(String input, Writer output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), false);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified InputStream, and write the tokens to the specified TokenReceiver
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 */
	public void stream(InputStream input, TokenReceiver output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), output, false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), output, false);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified Reader, and write the tokens to the specified TokenReceiver
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 */
	public void stream(Reader input, TokenReceiver output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), output, false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), output, false);
	}
	
	/**	virtually build an XML tree out of the specified String, and write the tokens to the specified TokenReceiver
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 */
	public void stream(String input, TokenReceiver output) throws IOException {
//		this.processTree(TokenSource.getTokenSource(input, this.grammar), output, false);
		this.stream(TokenSource.getTokenSource(input, this.grammar), output, false);
	}
	
	private void stream(TokenSource input, TokenReceiver output, boolean t) throws IOException {
		(new ParserInstance(input, output, true)).consumeTokens();
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified InputStream, and write the tokens to the specified OutputStream
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 */
	public ParserInstance getInstance(InputStream input, OutputStream output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified Reader, and write the tokens to the specified OutputStream
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 */
	public ParserInstance getInstance(Reader input, OutputStream output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	virtually build an XML tree out of the specified String, and write the tokens to the specified OutputStream
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the OutputStream to write the tree to
	 */
	public ParserInstance getInstance(String input, OutputStream output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified InputStream, and write the tokens to the specified Writer
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the Writer to write the tree to
	 */
	public ParserInstance getInstance(InputStream input, Writer output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified Reader, and write the tokens to the specified Writer
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the Writer to write the tree to
	 */
	public ParserInstance getInstance(Reader input, Writer output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	virtually build an XML tree out of the specified String, and write the tokens to the specified Writer
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the Writer to write the tree to
	 */
	public ParserInstance getInstance(String input, Writer output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), TokenReceiver.getTokenReceiver(output), true);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified InputStream, and write the tokens to the specified TokenReceiver
	 * @param	input		the InputStream providing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 */
	public ParserInstance getInstance(InputStream input, TokenReceiver output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), output, true);
	}
	
	/**	virtually build an XML tree out of the chars provided by the specified Reader, and write the tokens to the specified TokenReceiver
	 * @param	input		the Reader providing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 */
	public ParserInstance getInstance(Reader input, TokenReceiver output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), output, true);
	}
	
	/**	virtually build an XML tree out of the specified String, and write the tokens to the specified TokenReceiver
	 * @param	input		the String containing the char sequence to be parsed
	 * @param	output		the TokenReceiver to write the tokens to
	 */
	public ParserInstance getInstance(String input, TokenReceiver output) throws IOException {
		return new ParserInstance(TokenSource.getTokenSource(input, this.grammar), output, true);
	}
//	
//	//	method used for parsing
//	private TreeNode processTree(TokenSource input, TokenReceiver output, boolean build) throws IOException {
//		
//		TreeNode rootNode = new TreeNode(null, TreeNode.ROOT_NODE_TYPE);
//		TreeNode lastNode;
//		TreeNode node = rootNode;
//		TreeNode newNode;
//		
//		StringStack parserStack = new StringStack();
//		Vector missingEndTags = new Vector();
//		
//		while (input.hasMoreTokens()) {
//			
//			String token = input.retrieveToken();
//			
//			//	handle tag
//			if ((token != null) && this.grammar.isTag(token)) {
//				
//				String tagType = this.grammar.getType(token);
//				
//				//	handle end tag
//				if (this.grammar.isEndTag(token)) {
//					
//					//	tag closed implicitly
//					if (missingEndTags.contains(tagType.toLowerCase())) {
//						
//						//	remove current tag from list of implicitly closed tags
//						missingEndTags.remove(tagType.toLowerCase());
//					}
//					
//					//	end tag matches an open tag
//					else if (parserStack.search(tagType) > -1) {
//						
//						//	rise to matching start tag, close lower tags implicitly and write end tags
//						while (!parserStack.empty() && !parserStack.peek().equalsIgnoreCase(tagType)) {
//							missingEndTags.add(parserStack.pop().toLowerCase());
//							lastNode = node;
//							node = node.getParent();
//							
//							//	if streaming, write end tag directly and clean up closed tag and it's content
//							if (output != null)
//								output.storeToken(lastNode.getEndTag(this.grammar), parserStack.size() + 1);
//							if (!build)
//								lastNode.deleteSubtree();
//						}
//						
//						//	if end tag(s) missing and not correcting errors, throw Exception
//						if (!this.correctErrors && (missingEndTags.size() > 0))
//							throw new MissingEndTagException("The following tag(s) have not been closed properly: <" + TreeTools.concatVector(missingEndTags, ">, <") + ">");
//						
//						//	rise one tree level and write end tag
//						if (!parserStack.empty())
//							parserStack.pop();
//						lastNode = node;
//						node = node.getParent();
//						
//						//	if streaming, write end tag directly and clean up closed tag and it's content
//						if (output != null)
//							output.storeToken(lastNode.getEndTag(this.grammar), parserStack.size() + 1);
//						if (!build)
//							lastNode.deleteSubtree();
//					}
//					
//					//	end tag not matching any open tag
//					else {
//						
//						//	if tag not open and not correcting errors, throw Exception
//						if (!this.correctErrors) throw new UnexpectedEndTagException("The following tag has never been opened: <" + tagType + ">");
//					}
//				}
//				
//				//	handle start tag
//				else {
//					
//					//	clean list of implicitly closed tags
//					missingEndTags.clear();
//					
//					//	rise to next appropriate parent tag, close lower tags implicitly
//					while (!parserStack.empty() && ((this.searchParent(tagType, parserStack) > 0) || !this.embeddingValid(tagType, parserStack))) {
//						
//						//	if not correcting errors, throw Exception
//						if (!this.correctErrors)
//							throw new InvalidNestingException("<" + parserStack.peek() + "> is not a valid parent for <" + tagType + "> in the context of the Grammar in use (" + this.grammar.getClass().getName() + ")");
//						
//						//	rise to appropriate parent tag, close lower tags implicitly and write end tags
//						parserStack.pop();
//						lastNode = node;
//						node = node.getParent();
//						
//						//	if streaming, write end tag directly
//						if (output != null)
//							output.storeToken(lastNode.getEndTag(this.grammar), parserStack.size());
//						if (!build)
//							lastNode.deleteSubtree();
//					}
//					
//					//	create new node and link it to the tree
//					newNode = new TreeNode(node, this.grammar.translateTag(tagType), "", TreeNodeAttributeSet.getTagAttributes(token, this.grammar));
//					node.addChildNode(newNode);
//					 
//					//	descend to new node if tag not singular, and write start tag
//					if (!this.grammar.isSingularTag(token)) {
//						parserStack.push(tagType);
//						node = newNode;
//						
//						//	if streaming, write start tag directly
//						if (output != null)
//							output.storeToken(node.getStartTag(this.grammar), parserStack.size());
//					}
//					else {
//						
//						//	if streaming, write singular tag directly
//						if (output != null) {
//							
//							//	get singular tag
//							String singularTag = newNode.getSingularTag(this.grammar);
//							
//							//	write singular tag if allowed in context of grammar in use
//							if (singularTag != null)
//								output.storeToken(singularTag, parserStack.size());
//							
//							//	otherwise, write start tag and end tag
//							else {
//								output.storeToken(newNode.getStartTag(this.grammar), parserStack.size());
//								output.storeToken(newNode.getEndTag(this.grammar), parserStack.size());
//							}
//						}
//					}
//				}
//			}
//			
//			//	handle data, comment, DTD, or processing instruction
//			else if (token != null) {
//				
//				//	create new data / comment node and link it to the tree
//				if (this.grammar.isComment(token)) {
//					
//					//	if streaming, write comment directly
//					if (output != null)
//						output.storeToken(token, parserStack.size() + 1);
//					node.addChildNode(new TreeNode(node, TreeNode.COMMENT_NODE_TYPE, token));
//				}
//				else if (this.grammar.isDTD(token)) {
//					
//					//	if streaming, write data directly
//					if (output != null)
//						output.storeToken(token, parserStack.size() + 1);
//					node.addChildNode(new TreeNode(node, TreeNode.DTD_NODE_TYPE, token));
//				}
//				else if (this.grammar.isProcessingInstruction(token)) {
//					
//					//	if streaming, write data directly
//					if (output != null)
//						output.storeToken(token, parserStack.size() + 1);
//					node.addChildNode(new TreeNode(node, TreeNode.PROCESSING_INSTRUCTION_NODE_TYPE, token));
//				}
//				else {
//					
//					//	if streaming, write data directly
//					if (output != null)
//						output.storeToken(token, parserStack.size() + 1);
//					node.addChildNode(new TreeNode(node, TreeNode.DATA_NODE_TYPE, this.grammar.unescape(token)));
//				}
//			}
//		}
//		
//		//	return root of tree if not streaming
//		return rootNode;
//	}
	
	/**
	 * Objects of this class parse a single input stream using the grammar of
	 * their parent parser. Using this class instead of one of the various
	 * <code>parse()</code> or <code>stream()</code> methods gives client code
	 * more control over the parsing progress. In particular, client code can
	 * trigger the consumption of the encapsulated input stream token by token.
	 * This is especially useful in cases where parsing is a filter interposed
	 * in another data stream.
	 * 
	 * @author sautter
	 */
	public class ParserInstance {
		private TokenSource input;
		private TokenReceiver output;
		private boolean stream;
		ParserInstance(TokenSource input, TokenReceiver output, boolean stream) {
			this.input = input;
			this.output = output;
			this.stream = stream;
		}
		
		private TreeNode rootNode = new TreeNode(null, TreeNode.ROOT_NODE_TYPE);
		private TreeNode node = rootNode;
		
		private StringStack stack = new StringStack();
		private Vector missingEndTags = new Vector();
		
		/**
		 * Consume all remaining tokens, i.e., parse the encapsulated input
		 * stream to its end.
		 */
		public void consumeTokens() throws IOException {
			while (this.hasMoreTokens())
				this.consumeToken();
		}
		
		/**
		 * Consume the next token. The returned boolean indicates if a token was
		 * actually consumed. If not, the end of the encapsulated stream has
		 * been reached.
		 * @return true if a token was consumed, false otherwise
		 */
		public boolean consumeToken() throws IOException {
			if (this.input.hasMoreTokens()) {
				this.consumToken(this.input.retrieveToken());
				return true;
			}
			else return false;
		}
		
		/**
		 * Test if the encapsulated input stream has more tokens.
		 * @return true if there are more tokens, false otherwise
		 */
		public boolean hasMoreTokens() {
			return this.input.hasMoreTokens();
		}
		
		TreeNode getRootNode() {
			return ((this.stream || this.hasMoreTokens()) ? null : this.rootNode);
		}
		
		private void consumToken(String token) throws IOException {
			
			//	catch empty tokens
			if (token == null)
				return;
			
			//	handle tag
			if (grammar.isTag(token)) {
				
				String tagType = grammar.getType(token);
				
				//	handle end tag
				if (grammar.isEndTag(token)) {
					
					//	tag closed implicitly
					if (this.missingEndTags.contains(tagType.toLowerCase()))
						
						//	remove current tag from list of implicitly closed tags
						this.missingEndTags.remove(tagType.toLowerCase());
					
					//	end tag matches an open tag
					else if (this.stack.search(tagType) > -1) {
						
						//	rise to matching start tag, close lower tags implicitly and write end tags
						while (!this.stack.empty() && !this.stack.peek().equalsIgnoreCase(tagType)) {
							this.missingEndTags.add(stack.pop().toLowerCase());
							TreeNode lastNode = this.node;
							this.node = this.node.getParent();
							
							//	if streaming, write end tag directly and clean up closed tag and it's content
							if (this.output != null)
								this.output.storeToken(lastNode.getEndTag(grammar), this.stack.size() + 1);
							if (this.stream)
								lastNode.deleteSubtree();
						}
						
						//	if end tag(s) missing and not correcting errors, throw Exception
						if (!correctErrors && (this.missingEndTags.size() > 0))
							throw new MissingEndTagException("The following tag(s) have not been closed properly: <" + TreeTools.concatVector(missingEndTags, ">, <") + ">");
						
						//	rise one tree level and write end tag
						if (!this.stack.empty())
							this.stack.pop();
						TreeNode lastNode = this.node;
						this.node = this.node.getParent();
						
						//	if streaming, write end tag directly and clean up closed tag and it's content
						if (this.output != null)
							this.output.storeToken(lastNode.getEndTag(grammar), this.stack.size() + 1);
						if (this.stream)
							lastNode.deleteSubtree();
					}
					
					//	end tag not matching any open tag
					else {
						
						//	if tag not open and not correcting errors, throw Exception
						if (!correctErrors)
							throw new UnexpectedEndTagException("The following tag has never been opened: <" + tagType + ">");
					}
				}
				
				//	handle start tag
				else {
					TreeNode newNode;
					
					//	clean list of implicitly closed tags
					this.missingEndTags.clear();
					
					//	ascend to next appropriate parent tag, close lower tags implicitly
					while (!this.stack.empty() && ((searchParent(tagType, this.stack) > 0) || !embeddingValid(tagType, this.stack))) {
						
						//	if not correcting errors, throw Exception
						if (!correctErrors)
							throw new InvalidNestingException("<" + this.stack.peek() + "> is not a valid parent for <" + tagType + "> in the context of the Grammar in use (" + grammar.getClass().getName() + ")");
						
						//	ascend to appropriate parent tag, close lower tags implicitly and write end tags
						this.stack.pop();
						TreeNode lastNode = this.node;
						this.node = this.node.getParent();
						
						//	if streaming, write end tag directly
						if (this.output != null)
							this.output.storeToken(lastNode.getEndTag(grammar), this.stack.size());
						if (this.stream)
							lastNode.deleteSubtree();
					}
					
					//	create new node and link it to the tree
					newNode = new TreeNode(this.node, grammar.translateTag(tagType), "", TreeNodeAttributeSet.getTagAttributes(token, grammar));
					this.node.addChildNode(newNode);
					
					//	descend to new node if tag not singular, and write start tag
					if (!grammar.isSingularTag(token)) {
						this.stack.push(tagType);
						this.node = newNode;
						
						//	if streaming, write start tag directly
						if (this.output != null)
							this.output.storeToken(this.node.getStartTag(grammar), this.stack.size());
					}
					
					//	if streaming, write singular tag directly
					else if (this.output != null) {
						
						//	get singular tag
						String singularTag = newNode.getSingularTag(grammar);
						
						//	write singular tag if allowed in context of grammar in use
						if (singularTag != null)
							this.output.storeToken(singularTag, this.stack.size());
						
						//	otherwise, write start tag and end tag
						else {
							this.output.storeToken(newNode.getStartTag(grammar), this.stack.size());
							this.output.storeToken(newNode.getEndTag(grammar), this.stack.size());
						}
					}
				}
			}
			
			//	handle comment node
			else if (grammar.isComment(token)) {
				if (this.output != null)
					this.output.storeToken(token, this.stack.size() + 1);
				this.node.addChildNode(new TreeNode(this.node, TreeNode.COMMENT_NODE_TYPE, token));
			}
			
			//	handle DTD node
			else if (grammar.isDTD(token)) {
				if (this.output != null)
					this.output.storeToken(token, this.stack.size() + 1);
				this.node.addChildNode(new TreeNode(this.node, TreeNode.DTD_NODE_TYPE, token));
			}
			
			//	handle processing instruction node
			else if (grammar.isProcessingInstruction(token)) {
				if (this.output != null)
					this.output.storeToken(token, this.stack.size() + 1);
				this.node.addChildNode(new TreeNode(this.node, TreeNode.PROCESSING_INSTRUCTION_NODE_TYPE, token));
			}
			
			//	handle content node
			else {
				if (this.output != null)
					this.output.storeToken(token, this.stack.size() + 1);
				this.node.addChildNode(new TreeNode(this.node, TreeNode.DATA_NODE_TYPE, grammar.unescape(token)));
			}
		}
	}
	
	/* find the first possible parent for the specified tag
	 * @param	tag		the tag to find a parent for
	 * @param	stack	the stack to search the parent in
	 * @return	the stack depth of the first possible parent for the specified tag
	 */
	private int searchParent(String tag, StringStack stack) {
		for (int i = 0; i < stack.size(); i++) {
			if (this.grammar.canBeChildOf(tag, stack.get(i)))
				return i;
		}
		return -1;
	}
	
	/* check weather a tag can appear within any of the tags in the stack
	 * @param	tag		the tag to be checked for embedding problems
	 * @param	stack	the stack to be searched for tags that might cause nesting problems
	 * @return	true if and only if there is no embedding problem with the specified tag and any of the tags in the specified spack
	 */
	private boolean embeddingValid(String tag, StringStack stack) {
		for (int i = 0; i < stack.size(); i++) {
			if (!this.grammar.canBeEmbeddedIn(tag, stack.get(i)))
				return false;
		}
		return true;
	}
	
	/**
	 * String specific wrapper for a Stack
	 * 
	 * @author sautter
	 */
	private class StringStack {
		
		private Stack content = new Stack();
		
		/**	Constructor
		 */
		public StringStack(){}
		
		/**	@return true if and only if this stack contains no elements 
		 */
		public boolean empty() {
			return (this.content.isEmpty());
		}
		
		/**	push an element onto the stack
		 * @param	s	the element to be pushed
		 */
		public void push(String s) {
			this.content.push(s);
		}
		
		/**	@return	the top element of this stack (without removing it, see also pop())
		 */
		public String peek() {
			return ((String) this.content.peek());
		}
		
		/**	@return	the top element of this stack (removing it, see also peek())
		 */
		public String pop() {
			return ((String) this.content.pop());
		}
		
		/**	search this stack for an element
		 * @param	s	the element to be searched
		 * @return the stack depth of the specified element, or -1, if this stack doesn't contain that element
		 */
		public int search(String s) {
			int r = this.content.size()-1;
			for (int i = 0; i < this.content.size(); i++) {
				if (((String) this.content.get(r-i)).equalsIgnoreCase(s)) return i;
			}
			return -1;
		}
		
		/**	@return the number of element in this stack
		 */
		public int size() {
			return this.content.size();
		}
		
		/**	inspect an element from the middle of the stack
		 * @param	index	the stack depth
		 * @return the element at the specified stack depth (get(0) has the same effect as peek())
		 */
		public String get(int index) {
			if ((index >= 0) && (index < this.content.size())) {
				return ((String) this.content.get(this.content.size() - 1 - index));
			} else {
				return "";
			}
		}
//		
//		/**	@return a String concatenated from all of this stack's elements, separated by space characters
//		 */
//		public String trace() {
//			StringBuffer assembler = new StringBuffer();
//			for (int i = 0; i < this.content.size(); i++)
//				assembler.append(((i > 0) ? " " : "") + ((String) this.content.get(i)));
//			return assembler.toString();
//		}
	}
//	
//	//	!!! TEST ONLY !!!
//	public static void main(String[] args) throws Exception {
//		Parser parser = new Parser(new Html());
//		URL url = new URL("http://plazi.cs.umb.edu/GgServer/xslt/FC21C429805C81809F75E8BBA095EBF8");
//		BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
//		
//		//	the old way
//		parser.stream(br, new TokenReceiver() {
//			public void storeToken(String token, int treeDepth) throws IOException {
//				token = token.trim();
//				if (token.length() != 0)
//					System.out.println(token);
//			}
//			public void close() throws IOException {}
//		});
//		
//		//	the new way
//		final LinkedList tokens = new LinkedList();
//		ParserInstance pi = parser.getInstance(br, new TokenReceiver() {
//			public void storeToken(String token, int treeDepth) throws IOException {
//				token = token.trim();
//				if (token.length() != 0)
//					tokens.addLast(token);
//			}
//			public void close() throws IOException {}
//		});
//		int b = 0;
//		while ((tokens.size() != 0) || pi.hasMoreTokens()) {
//			while ((tokens.size() < 10) && pi.hasMoreTokens())
//				pi.consumeToken();
//			System.out.println("Batch " + ++b);
//			while (tokens.size() != 0)
//				System.out.println(tokens.removeFirst());
//		}
//	}
}
