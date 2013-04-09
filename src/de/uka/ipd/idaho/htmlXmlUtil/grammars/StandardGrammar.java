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
package de.uka.ipd.idaho.htmlXmlUtil.grammars;


import java.util.HashSet;
import java.util.Vector;


/**
 * Default grammar class for handling XML data in a generic, schema un-aware
 * fashion. Grammars intended for parsing specific data objects from XML data
 * streams can extend this class, instead of extending Grammar directly.
 * 
 * @author sautter
 */
public class StandardGrammar extends Grammar {
	
	private final String tagEndSeq = "" + tagEnd;
	private final String singularTagEndSeq = endTagMarker + "" + tagEnd;
	private final HashSet startTagStartSeqs = new HashSet();
	private final HashSet endTagStartSeqs = new HashSet();
	
	/**	Constructor
	 */
	public StandardGrammar() {
		String s;
		for (int i = 0; i < suitableTagStarts.length(); i++) {
			s = suitableTagStarts.substring(i, (i+1));
			this.startTagStartSeqs.add(tagStart + s.toLowerCase());
			this.startTagStartSeqs.add(tagStart + s.toUpperCase());
			this.endTagStartSeqs.add(tagStart + "" + endTagMarker + s.toLowerCase());
			this.endTagStartSeqs.add(tagStart + "" + endTagMarker + s.toUpperCase());
		}
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isComment(java.lang.String)
	 */
	public boolean isComment(String tag) {
		return (tag.startsWith(this.commentStartMarker) && tag.endsWith(this.commentEndMarker));
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isDTD(java.lang.String)
	 */
	public boolean isDTD(String tag) {
		return (tag.startsWith(this.dtdStartMarker) && tag.endsWith(this.dtdEndMarker));
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isProcessingInstruction(java.lang.String)
	 */
	public boolean isProcessingInstruction(String tag) {
		return (tag.startsWith(this.processingInstructionStartMarker) && tag.endsWith(this.processingInstructionEndMarker));
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isTag(java.lang.String)
	 */
	public boolean isTag(String tag) {
		return (!this.isComment(tag) && !this.isDTD(tag) && !this.isProcessingInstruction(tag) && (this.isEndTag(tag) || ((tag.length() > 1) && (this.startTagStartSeqs.contains(tag.substring(0, 2))) && (tag.endsWith(this.tagEndSeq)))));
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isEndTag(java.lang.String)
	 */
	public boolean isEndTag(String tag) {
		return ((tag.length() > 2) && this.endTagStartSeqs.contains(tag.substring(0, 3)) && tag.endsWith(this.tagEndSeq));
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isSingularTag(java.lang.String)
	 */
	public boolean isSingularTag(String tag) {
		return (!this.isEndTag(tag) && this.isTag(tag) && ((tag.endsWith(this.singularTagEndSeq)) || this.isSingularTagType(this.getType(tag))));
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isSingularTagType(java.lang.String)
	 */
	public boolean isSingularTagType(String type) {
		return (type.startsWith("!") || type.startsWith("?"));
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#getType(java.lang.String)
	 */
	public String getType(String tag) {
		String type = tag;
		
		//	cut leading markers of end tags
		if (this.isEndTag(type))
			type = type.substring(2, (type.length()-1));
		
		//	cut leading marker of start tags
		if (this.isTag(type))
			type = type.substring(1);
		
		//	cut before first parameter
		if (type.indexOf(this.tagAttributeSeparator) != -1)
			type = type.substring(0, type.indexOf(this.tagAttributeSeparator));
		
		//	cut end markers of singular tag
		if (type.endsWith(this.singularTagEndSeq))
			type = type.substring(0, (type.length() - this.singularTagEndSeq.length()));
		
		//	cut end marker of tag
		if (type.endsWith(this.tagEndSeq))
			type = type.substring(0, (type.length() - this.tagEndSeq.length()));
		
		return type;
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isWhitespace(char)
	 */
	public boolean isWhitespace(char c) {
		return ((c == ' ') || (c == '\t') || (c == '\n') || (c == '\r') || (c == '\f'));
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isTagWhitespace(char)
	 */
	public boolean isTagWhitespace(char c) {
		return ((c == this.tagAttributeSeparator) || (c == '\t') || (c == '\n') || (c == '\r') || (c == '\f'));
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#valueMayContainWhitespace(String, String)
	 */
	public boolean valueMayContainWhitespace(String tag, String attribute) {
		return true;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isTagAttributeValueQuoter(char)
	 */
	public boolean isTagAttributeValueQuoter(char c) {
		return ((c == '\'') || (c == '"'));
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#translateTag(java.lang.String)
	 */
	public String translateTag(String tag) {
		return tag;
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#getParentTags(java.lang.String)
	 */
	public HashSet getParentTags(String tag) {
		return new HashSet();
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#canBeChildOf(java.lang.String, java.lang.String)
	 */
	public boolean canBeChildOf(String child, String parent) {
		return true;
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#canBeEmbeddedIn(java.lang.String, java.lang.String)
	 */
	public boolean canBeEmbeddedIn(String child, String parent) {
		return true;
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#getCharCode(char)
	 */
	public String getCharCode(char c) {
		return ("" + c);
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isCharCode(String)
	 */
	public boolean isCharCode(String code) {
		return ("&amp;".equals(code) || "&lt;".equals(code) || "&gt;".equals(code) || "&quot;".equals(code) || super.isCharCode(code));
	}
	
	/**
	 * This implementation escapes a string to be well-formed XML - in
	 * particular, it escapes &amp;, &lt;, &gt;, and &quot;
	 * @param string the string to escape
	 * @return the escaped string
	 * @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#escape(java.lang.String)
	 */
	public String escape(String string) {
		StringBuffer escapedString = new StringBuffer();
		for (int c = 0; c < string.length(); c++) {
			char ch = string.charAt(c);
			if (ch == '<')
				escapedString.append("&lt;");
			else if (ch == '>')
				escapedString.append("&gt;");
			else if (ch == '"')
				escapedString.append("&quot;");
			else if (ch == '&')
				escapedString.append("&amp;");
			else escapedString.append(ch);
		}
		return escapedString.toString();
	}

	/**
	 * This implementation un-escapes a string from its XML encoding - in
	 * particular, it un-escapes &amp;, &lt;, &gt;, and &quot;
	 * @param escapedString the string to un-escape
	 * @return the un-escaped string
	 * @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#unescape(java.lang.String)
	 */
	public String unescape(String escapedString) {
		StringBuffer string = new StringBuffer();
		for (int c = 0; c < escapedString.length();) {
			char ch = escapedString.charAt(c);
			if (ch == '&') {
				if (escapedString.startsWith("amp;", (c+1))) {
					string.append('&');
					c+=5;
				}
				else if (escapedString.startsWith("lt;", (c+1))) {
					string.append('<');
					c+=4;
				}
				else if (escapedString.startsWith("gt;", (c+1))) {
					string.append('>');
					c+=4;
				}
				else if (escapedString.startsWith("quot;", (c+1))) {
					string.append('"');
					c+=6;
				}
				else if ((escapedString.startsWith("#", (c+1)) || escapedString.startsWith("x", (c+1))) && (escapedString.indexOf(';', (c+1)) != -1)) {
					String cc = escapedString.substring(c, (escapedString.indexOf(';', (c+1))+1));
					char dch = this.getPlainChar(cc);
					if (dch == 0) {
						string.append(ch);
						c++;
					}
					else {
						string.append(dch);
						c+=cc.length();
					}
				}
				else {
					string.append(ch);
					c++;
				}
			}
			else {
				string.append(ch);
				c++;
			}
		}
		return string.toString();
	}

	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#ckeckTokenSequence(java.util.Vector)
	 */
	public void ckeckTokenSequence(Vector ts) {}
}
