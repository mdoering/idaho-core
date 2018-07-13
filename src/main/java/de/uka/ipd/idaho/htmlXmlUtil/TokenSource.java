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


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Vector;

import de.uka.ipd.idaho.htmlXmlUtil.exceptions.ParseException;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * A TokenSource decomposes a stream of HTML/XML data into individual tokens.
 * If the grammar specified to the constructor allows for error correction, the
 * token source makes a best effort to fix low level syntax errors like missing
 * or erroneous quotation of attributes.
 * 
 * @author sautter
 */
public class TokenSource {
	
	private static final boolean DEBUG = false;
	
	private final char tagStart;
	private final char tagEnd;
	private final char endTagMarker;
	private final char tagAttributeSeparator;
	private final char tagAttributeValueSeparator;
	private final String commentStartMarker;
	private final String commentEndMarker;
	private final String processingInstructionStartMarker;
	private final String processingInstructionEndMarker;
	private final String dtdStartMarker;
	private final String dtdEndMarker;
	
	private final boolean correctErrors;
	private final boolean correctCharEncoding;
	
	private int tokenLookahead;
	private Vector tokenBuffer = new Vector();
	
	private LookaheadReader charSource;
	private Grammar grammar;
	
	private String skippedWhitespace = "";
	
	private TokenSource(LookaheadReader charSource, Grammar grammar) throws IOException {
		this.charSource = charSource;
		this.grammar = grammar;
		
		//	get characters for tag recognition
		this.tagStart = this.grammar.getTagStart();
		this.tagEnd = this.grammar.getTagEnd();
		this.endTagMarker = this.grammar.getEndTagMarker();
		this.tagAttributeSeparator = this.grammar.getTagAttributeSeparator();
		this.tagAttributeValueSeparator = this.grammar.getTagAttributeValueSeparator();
		this.correctErrors = this.grammar.correctErrors();
		this.correctCharEncoding = this.grammar.correctCharEncoding();
		
		//	get comment markers
		String csm = this.grammar.getCommentStartMarker();
		this.commentStartMarker = (this.correctErrors ? csm.substring(0, (csm.length() - (csm.length() / 3))) : csm);
		String cem = this.grammar.getCommentEndMarker();
		this.commentEndMarker = (this.correctErrors ? cem.substring(cem.length() / 3) : cem);
		
		//	get processing instruction markers
		this.processingInstructionStartMarker = this.grammar.getProcessingInstructionStartMarker();
		this.processingInstructionEndMarker = this.grammar.getProcessingInstructionEndMarker();
		
		//	get DTD markers
		this.dtdStartMarker = this.grammar.getDtdStartMarker();
		this.dtdEndMarker = this.grammar.getDtdEndMarker();
		
		//	get token lookahead required for error correction
		this.tokenLookahead = this.grammar.getTokenLookahead() + 1;
		
		this.produceTokens();
	}
	
	/**	@return	true if and only if there are more tokens to be retrieved from this source
	 */
	public boolean hasMoreTokens() {
		if (this.tokenBuffer.size() != 0)
			return true;
		try {
			return (this.charSource.peek() != -1);
		}
		catch (IOException ioe) {
			return false;
		}
	}
	
	/**	@return	the first token in the queue
	 */
	public String retrieveToken() throws IOException {
		this.produceTokens();
		return ((this.tokenBuffer.size() == 0) ? null : ((String) this.tokenBuffer.remove(0)));
	}
	
	private void produceTokens() throws IOException {
		String awaitedEndTag = null;
		
		//	refill buffer
		while ((this.charSource.peek() != -1) && ((this.tokenBuffer.size() < this.tokenLookahead) || (awaitedEndTag != null))) {
			String token = this.produceToken((awaitedEndTag == null) ? null : ("" + this.tagStart + "" + this.endTagMarker + "" + awaitedEndTag + "" + this.tagEnd));
			if (DEBUG) System.out.println("TokenSource got token: " + token);
			if (token.length() == 0)
				continue;
			
			//	tag token, might have to wait for end tag
			if (this.grammar.isTag(token)) {
				if ((awaitedEndTag != null) && this.grammar.isEndTag(token) && awaitedEndTag.equalsIgnoreCase(this.grammar.getType(token)))
					awaitedEndTag = null;
				else if (this.grammar.waitForEndTag(token))
					awaitedEndTag = this.grammar.getType(token);
			}
			
			//	store token
			this.tokenBuffer.addElement(token);
		}
		
		//	check buffer
		this.grammar.ckeckTokenSequence(this.tokenBuffer);
	}
	
	private String produceToken(String stopTag) throws IOException {
		if (DEBUG) System.out.println("TokenSource: producing token" + ((stopTag == null) ? "" : (" up to '" + stopTag + "'")));
		
		//	end of input
		if (this.charSource.peek() == -1)
			return null;
		
		//	waiting for some end tag
		else if (stopTag != null)
			return this.cropUpTo(stopTag, false);
		
		//	comment
		else if (this.charSource.startsWith(this.commentStartMarker, false))
			return this.cropUpTo(this.commentEndMarker, true);
		
		//	DTD
		else if (this.charSource.startsWith(this.dtdStartMarker, false))
			return this.cropUpTo(this.dtdEndMarker, true);
		
		//	processing instruction
		else if (this.charSource.startsWith(this.processingInstructionStartMarker, false))
			return this.cropUpTo(this.processingInstructionEndMarker, true);
		
		//	tag, or data starting with tag start
		else if (this.charSource.peek() == this.tagStart) {
//			if (!this.correctErrors || !this.correctCharEncoding)
//				return this.cropTag();
			if (Character.isLetter((char) this.charSource.peek(1)))
				return this.cropTag();
			if ("_:".indexOf((char) this.charSource.peek(1)) != -1)
				return this.cropTag();
			if (this.charSource.peek(1) == this.endTagMarker)
				return this.cropTag();
			return this.cropData();
		}
		
		//	character data
		else return this.cropData();
	}
	
	private void skipWhitespace(boolean buffer) throws IOException {
		StringBuffer whitespace = new StringBuffer();
		
		//	crop space
		while ((this.charSource.peek() != -1) && this.grammar.isWhitespace((char) this.charSource.peek()))
			whitespace.append((char) this.charSource.read());
		
		//	buffer if asked to
		this.skippedWhitespace = (buffer ? whitespace.toString() : "");
	}
	
	private String cropUpTo(String stopSequence, boolean includeStopSequence) throws IOException {
		StringBuffer token = new StringBuffer();
		char stopChar = stopSequence.charAt(0);
		while (this.charSource.peek() != -1) {
			
			//	stop sequence reached
			if ((this.charSource.peek() == stopChar) && this.charSource.startsWith(stopSequence, false)) {
				
				//	handle stop sequence and buffer subsequent whitespace
				if (includeStopSequence || (token.length() == 0)) {
					for (int c = 0; c < stopSequence.length(); c++)
						token.append((char) this.charSource.read());
					this.skipWhitespace(true);
				}
				
				//	we're done here
				break;
			}
			
			//	other character
			else token.append((char) this.charSource.read());
		}
		
		//	finally ...
		return token.toString();
	}
	
	private String cropTag() throws IOException {
		StringBuffer tag = new StringBuffer();
		boolean gotEndTagMarker = false;
		tag.append((char) this.charSource.read());
		
		//	crop end tag marker
		if (this.charSource.peek() == this.endTagMarker) {
			tag.append((char) this.charSource.read());
			gotEndTagMarker = true;
		}
		
		//	crop qName
		String tagType = LookaheadReader.cropName(this.charSource);
		tag.append(tagType);
		this.skipWhitespace(false);
		
		//	crop attribute-value pairs
		while (this.charSource.peek() != -1) {
			this.skipWhitespace(false);
			
			//	end of attributes
			if ((this.charSource.peek() == this.tagEnd) || (this.charSource.peek() == this.endTagMarker))
				break;
			if (DEBUG) {
				System.out.println("Tag part: " + tag.toString());
				System.out.println("Next char: " + ((char) this.charSource.peek()) + " " + this.charSource.peek());
			}
			
			//	read attribute name
			String attribName = LookaheadReader.cropName(this.charSource);
			if (DEBUG) System.out.println("Attrib name: " + attribName);
			this.skipWhitespace(false);
			
			/* if attribute name comes up empty, skip to next valid attribute
			 * name or tag end, whichever is closer ... better to lose some
			 * attribute than to lose the entire document */
			if (attribName.length() == 0) {
				if (DEBUG) System.out.println("Empty attrib name");
				if (this.correctErrors) {
					
					//	seek tag end
					int tagEndIndex = this.charSource.indexOf(this.tagEnd);
					if (tagEndIndex == -1)
						tagEndIndex = Integer.MAX_VALUE;
					else {
						int sTagEndIndex = this.charSource.indexOf(this.endTagMarker + "" + this.tagEnd);
						if ((sTagEndIndex != -1) && (sTagEndIndex < tagEndIndex))
							tagEndIndex = sTagEndIndex;
						if (DEBUG) System.out.println(" - tag end is " + tagEndIndex + " out");
					}
					
					//	seek attribute name
					int attribNameIndex = this.charSource.find("[a-zA-Z\\_\\:][a-zA-Z0-9\\_\\:\\.\\-]*\\s*\\" + this.tagAttributeValueSeparator);
					if (attribNameIndex == -1)
						attribNameIndex = Integer.MAX_VALUE;
					else if (DEBUG) System.out.println(" - next attrib name is " + attribNameIndex + " out");
					
					//	tag end is closer
					if (tagEndIndex < attribNameIndex) {
						if (DEBUG) System.out.println(" ==> skipping up to tag end");
						this.charSource.skip(tagEndIndex);
						continue;
					}
					
					//	next attribute is closer
					else if (attribNameIndex < tagEndIndex) {
						if (DEBUG) System.out.println(" ==> skipping up next attrib name");
						this.charSource.skip(attribNameIndex);
						continue;
					}
					
					//	found neither, quit
					else break;
				}
				
				//	we're not corecting errors ...
				else throw new ParseException("Invalid character '" + ((char) this.charSource.peek()) + "', expected name");
			}
			
			//	we have a value (tolerate missing separator if configured that way)
			String attribValue;
			if ((this.charSource.peek() == this.tagAttributeValueSeparator) || this.grammar.isTagAttributeValueQuoter((char) this.charSource.peek())) {
				if (this.charSource.peek() == this.tagAttributeValueSeparator)
					this.charSource.read();
				else if (!this.correctErrors)
					throw new ParseException("Invalid character '" + ((char) this.charSource.peek()) + "', expected '" + this.tagAttributeValueSeparator + "'");
				this.skipWhitespace(false);
				attribValue = LookaheadReader.cropAttributeValue(this.charSource, this.grammar, tagType, attribName, this.tagEnd, this.endTagMarker);
				if (DEBUG) System.out.println("Attrib value: " + attribValue);
			}
			
			//	we have a standalone attribute, substitute name for value
			else attribValue = attribName;
			
			//	append normalized attribute
			tag.append(this.tagAttributeSeparator + attribName + this.tagAttributeValueSeparator + this.grammar.getTagAttributeValueQuoter() + attribValue + this.grammar.getTagAttributeValueQuoter());
		}
		
		//	end tag marker at end of singular tag
		if (this.charSource.peek() == this.endTagMarker) {
			if (gotEndTagMarker)
				this.charSource.read();
			else tag.append((char) this.charSource.read());
		}
		
		//	tag end
		if (this.charSource.peek() == this.tagEnd)
			tag.append((char) this.charSource.read());
		
		//	buffer space after tag
		this.skipWhitespace(true);
		
		//	finally ...
		if (DEBUG) System.out.println("Tag full: " + tag.toString());
		return tag.toString();
	}
	
	private String cropData() throws IOException {
		StringBuffer data = new StringBuffer(this.skippedWhitespace);
		while (this.charSource.peek() != -1) {
			
			//	start of comment / end of data
			if (this.charSource.startsWith(this.commentStartMarker, false))
				break;
			
			//	start of DTD / end of data
			else if (this.charSource.startsWith(this.dtdStartMarker, false))
				break;
			
			//	start of processing instruction / end of data
			else if (this.charSource.startsWith(this.processingInstructionStartMarker, false))
				break;
			
			//	start of tag, or data starting with tag start / end of data
			else if (this.charSource.peek() == this.tagStart) {
//				if (!this.correctErrors)
//					break;
				if (Character.isLetter((char) this.charSource.peek(1)))
					break;
				if ("_:".indexOf((char) this.charSource.peek(1)) != -1)
					break;
				if (this.charSource.peek(1) == this.endTagMarker)
					break;
			}
			
			//	check encoding if activated
			if (this.correctCharEncoding) {
				
				//	possible start of encoded character
				if (this.charSource.peek() == '&')
					data.append(LookaheadReader.cropCharCode(this.charSource, this.grammar));
				
				//	encode current character
				else data.append(this.grammar.getCharCode((char) this.charSource.read()));
			}
			
			//	other character
			else data.append((char) this.charSource.read());
		}
		
		//	finally ...
		return data.toString();
	}
	
	/**	create a TokenSource providing tokens parsed from a String in the context of the StandardGrammar 
	 * @param 	string	the String to parse the tokens from
	 * @return	a TokenSource providing tokens parsed from the specified String in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(String string) throws IOException {
		return getTokenSource(string, null);
	}
	/**	create a TokenSource providing tokens parsed from a String in the context of the specified Grammar 
	 * @param 	string	the String to parse the tokens from
	 * @param 	grammar	the Grammar that's context to parse the tokens in
	 * @return	a TokenSource providing tokens parsed from the specified String in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(String string, Grammar grammar) throws IOException {
		if (grammar == null)
			grammar = new StandardGrammar(); 
		return new TokenSource(new LookaheadReader(new StringReader(string), (grammar.getCharLookahead()+1)), grammar);
	}
	
	/**	create a TokenSource providing tokens parsed from an InputStream in the context of the StandradGrammar 
	 * @param 	stream	the InputStream to parse the tokens from
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(InputStream stream) throws IOException {
		return getTokenSource(stream, null);
	}
	/**	create a TokenSource providing tokens parsed from an InputStream in the context of the specified Grammar 
	 * @param 	stream	the InputStream to parse the tokens from
	 * @param 	grammar	the Grammar that's context to parse the tokens in
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(InputStream stream, Grammar grammar) throws IOException {
		if (grammar == null)
			grammar = new StandardGrammar(); 
		return new TokenSource(new LookaheadReader(new InputStreamReader(stream/*, TODO guessEncoding()*/), (grammar.getCharLookahead()+1)), grammar);
	}
	
	/**	create a TokenSource providing tokens parsed from a Reader in the context of the StandradGrammar 
	 * @param 	stream	the Reader to parse the tokens from
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(Reader stream) throws IOException {
		return getTokenSource(stream, null);
	}
	/**	create a TokenSource providing tokens parsed from a Reader in the context of the specified Grammar 
	 * @param 	stream	the Reader to parse the tokens from
	 * @param 	grammar	the Grammar that's context to parse the tokens in
	 * @return	a TokenSource providing tokens parsed from the specified Reader in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(Reader stream, Grammar grammar) throws IOException {
		if (grammar == null)
			grammar = new StandardGrammar(); 
		return new TokenSource(new LookaheadReader(stream, (grammar.getCharLookahead()+1)), grammar);
	}
	
	/**	create a TokenSource providing tokens parsed from an InputStream in the context of the StandardGrammar 
	 * @param 	file	the File from that's content the tokens are to be parsed
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(File file) throws IOException {
		return getTokenSource(file, null);
	}
	/**	create a TokenSource providing tokens parsed from an InputStream in the context of the specified Grammar 
	 * @param 	file	the File from that's content the tokens are to be parsed
	 * @param 	grammar	the Grammar that's context to parse the tokens in
	 * @return	a TokenSource providing tokens parsed from the specified InputStream in the context of the specified Grammar 
	 * @throws IOException
	 */
	public static TokenSource getTokenSource(File file, Grammar grammar) throws IOException {
		return getTokenSource(new FileInputStream(file), grammar);
	}
//	
//	public static void main(String[] args) throws Exception {
////		String html = "<?xml screw=it?><!element is><!element it><!element now><html>\r\n  <head><script>some & crap < with --> bad <!-- char > sequences</script></head>\r\n  <body test=test with space test2=with' width='100% hight=100%\">  Test  </body>\r\n<!-- comment -></html>";
////		TokenSource ts = getTokenSource(html, new Html());
////		while (ts.hasMoreTokens())
////			System.out.println("Token: '" + ts.retrieveToken() + "'");
////		TokenSource ts = getTokenSource((new URL("http://www.plantsystematics.org/taxpage/0/genus/Agave.html").openStream()), html);
//		
//		File f = new File("E:/Projektdaten/Antfiles/Species  Camponotus (Camponotus) vagus - AntWeb.raw.htm");
//		InputStream in = new FileInputStream(f);
//		TokenSource ts = getTokenSource(in, new Html());
//		
//		//	Tokenizes OK
////		TokenSource ts = getTokenSource(new StringReader("<t a=\"value with \"invalid\" chars\">text</t>"), new StandardGrammar());
//		//	Tokenizes OK
////		TokenSource ts = getTokenSource(new StringReader("<t a=\"Støp­Bowitz, C. (1992) Polychètes pèlagiques des Campagnes de \" L’ombango \" dans les eaux équatoriales et tropicales ouest­africaines. Editions de l’ORSTOM. Coll. Études et Thèses. Paris, 115 pp.\">text</t>"), new StandardGrammar());
//		//	Tokenizes BULLSHIT ==> TODO improve it !!!
////		TokenSource ts = getTokenSource(new StringReader("<t a=\"Støp­Bowitz, C. (1992) Polychètes pèlagiques des Campagnes de \" L'ombango \" dans les eaux équatoriales et tropicales ouest­africaines. Editions de l'ORSTOM. Coll. Études et Thèses. Paris, 115 pp.\">text</t>"), new StandardGrammar());
//		while (ts.hasMoreTokens()) {
//			String token = ts.retrieveToken();
//			if (!DEBUG) System.out.println("Token: '" + token + "'");
////			if (html.isTag(token) && !html.isEndTag(token)) {
////				TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, html);
////				String[] ans = tnas.getAttributeNames();
////				for (int a = 0; a < ans.length; a++)
////					System.out.println(ans[a] + " = " + tnas.getAttribute(ans[a]));
////			}
//		}
//	}
}