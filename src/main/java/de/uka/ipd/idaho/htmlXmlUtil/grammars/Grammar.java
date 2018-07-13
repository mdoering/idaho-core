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
 * A Grammar defines the actual behavior of a Parser instance. In particular, a
 * grammar defines
 * <ul>
 * <li>which characters start and end a tag (these are the angle brackets in HTML/XML, but custom subclasses may change this on demand)</li>
 * <li>which character marks an end tag</li>
 * <li>which element nestings are valid</li>
 * <li>...</li>
 * </ul>
 * 
 * @author sautter
 */
public abstract class Grammar {
	
	//	default values, may be overwritten by subclasses
	/** the character starting a tag, default is '<' */
	protected final char tagStart = '<';
	
	/** the character ending a tag, default is '>' */
	protected final char tagEnd = '>';
	
	/** the character marking an end tag, default is '/' */
	protected final char endTagMarker = '/';
	
	/** a String holding the characters an element name may start with, case insensitive, default are the Latin letters */
	protected final String suitableTagStarts = "abcdefghijklmnopqrstuvwxyz";
	
	/** the character separating an element name from the start of the first attribute name, if any, default is ' ' */
	protected final char tagAttributeSeparator = ' ';
	
	/** the character separating an attribute name from the attribute's value, default is '=' */
	protected final char tagAttributeValueSeparator = '=';
	
	/** the character sequence starting a comment, default is '<!--' */
	protected final String commentStartMarker = "<!--";
	
	/** the character sequence ending a comment, default is '-->' */
	protected final String commentEndMarker = "-->";
	
	/** the character sequence starting a processing instruction, default is '<?' */
	protected final String processingInstructionStartMarker = "<?";
	
	/** the character sequence ending a processing instruction, default is '?>' */
	protected final String processingInstructionEndMarker = "?>";
	
	/** the character sequence starting an embedded DTD, default is '<!' */
	protected final String dtdStartMarker = "<!";
	
	/** the character sequence ending an embedded DTD, default is '!>' */
	protected final String dtdEndMarker = ">";
	
	/** the indentation character sequence for output, default is '  ' (two spaces) */
	protected final String standardIndent = "  ";
	
	
	/** correct errors on parsing? */
	protected final boolean correctErrors = true;
	
	/** encode characters not encoded in entities on tokenization? */
	protected final boolean correctCharEncoding = false;
	
	/** end empty tags with '/' on output? In HTML, for example, the 'br' element is empty by definition */
	protected final boolean isStrictXML = true;
	
	/** @return true if the parser should correct errors while parsing a String, false otherwise
	 */
	public boolean correctErrors() {
		return this.correctErrors;
	}
	
	/**
	 * @return true if the parser should encode special characters while parsing
	 *         a String, false otherwise (e.g. " will be converted to &quot; in
	 *         HTML)
	 */
	public boolean correctCharEncoding() {
		return this.correctCharEncoding;
	}
	
	/**
	 * @return true if a tree should be converted to code strictly following XML
	 *         syntax. In particular, this means the following:<BR>
	 *         <B>true:</B> childless nodes are converted to &lt;'type +
	 *         parameters'/&gt;<BR>
	 *         <B>false:</B> childless nodes are converted to &lt;'type +
	 *         parameters'&gt;&lt;/'type'&gt;, inheritently singular nodes are
	 *         converted to &lt;'type + parameters'&gt; (e.g. type 'BR' nodes in
	 *         HTML)
	 */
	public boolean isStrictXML() {
		return this.isStrictXML;
	}

	/**
	 * @return the whitespace to be inserted for indention once per tree level
	 *         when converting a tree to code
	 */
	public String getStandardIndent() {
		return this.standardIndent;
	}

	/**
	 * @return the character sequence a comment is opened with
	 */
	public String getCommentStartMarker() {
		return this.commentStartMarker;
	}

	/**
	 * @return the character sequence a comment is closed with
	 */
	public String getCommentEndMarker() {
		return this.commentEndMarker;
	}
	
	/**
	 * @return the character sequence a processing instruction is opened with
	 */
	public String getProcessingInstructionStartMarker() {
		return this.processingInstructionStartMarker;
	}

	/**
	 * @return the character sequence a processing instruction is closed with
	 */
	public String getProcessingInstructionEndMarker() {
		return this.processingInstructionEndMarker;
	}

	/**
	 * @return the character sequence a DTD is opened with
	 */
	public String getDtdStartMarker() {
		return this.dtdStartMarker;
	}

	/**
	 * @return the character sequence a DTD is closed with
	 */
	public String getDtdEndMarker() {
		return this.dtdEndMarker;
	}
	
	/**
	 * @return the character a tag starts with
	 */
	public char getTagStart() {
		return this.tagStart;
	}

	/**
	 * @return a list containing all characters a tag type may start with
	 */
	public String getSuitableTagStarts() {
		return this.suitableTagStarts;
	}

	/**
	 * @return the character used to end a tag
	 */
	public char getTagEnd() {
		return this.tagEnd;
	}

	/**
	 * @return the character end tags are marked with as such
	 */
	public char getEndTagMarker() {
		return this.endTagMarker;
	}

	/**
	 * @return the separator character between attribute / value pairs within a
	 *         tag
	 */
	public char getTagAttributeSeparator() {
		return this.tagAttributeSeparator;
	}

	/**
	 * @return the separator character between a tag attribute and it's value
	 */
	public char getTagAttributeValueSeparator() {
		return this.tagAttributeValueSeparator;
	}

	/**
	 * @return the standard quoting character for the values of tag attributes
	 */
	public char getTagAttributeValueQuoter() {
		return '"';
	}

	/**
	 * @return the number of chars that have to be inspected at the same time on
	 *         order to rcognize the longest marker sequence known to this
	 *         Grammar
	 */
	public int getCharLookahead() {
		return 4;
	}

	/**
	 * check weather the specified String is a comment
	 * @param tag the String to be checked
	 * @return true if and only if the specified String is a comment
	 */
	public abstract boolean isComment(String tag);
	
	/**
	 * check weather the specified String is a processing instruction
	 * @param tag the String to be checked
	 * @return true if and only if the specified String is a processing
	 *         instruction
	 */
	public abstract boolean isProcessingInstruction(String tag);

	/**
	 * check weather the specified String is a DTD
	 * @param tag the String to be checked
	 * @return true if and only if the specified String is a DTD
	 */
	public abstract boolean isDTD(String tag);

	/**
	 * check weather the specified String is a tag
	 * @param tag the String to be checked
	 * @return true if and only if the specified String is a tag
	 */
	public abstract boolean isTag(String tag);

	/**
	 * check weather the specified String is an end tag
	 * @param tag the String to be checked
	 * @return true if and only if the specified String is an end tag
	 */
	public abstract boolean isEndTag(String tag);

	/**
	 * check if the specified tag is singular
	 * @param tag the tag to be checked for singularity
	 * @return true if and only if the tag is singular in the context of this
	 *         grammar, ie no end tag to be expected
	 */
	public abstract boolean isSingularTag(String tag);

	/**
	 * check if the specified tag type is inherently singular
	 * @param type the tag type to be checked for singularity
	 * @return true if and only if the tag type is inherently singular in the
	 *         context of this grammar, ie no end tag to be expected
	 */
	public abstract boolean isSingularTagType(String type);

	/**
	 * get the type of the specified tag
	 * @param tag the tag that's type is to be determined
	 * @return the type of the specified tag, in particular the tag without
	 *         markers and parameters
	 */
	public abstract String getType(String tag);

	/**
	 * check if the specified char should be treated as whitespace
	 * @param c the char to be checked
	 * @return true if and only if the specified char should be treated as
	 *         whitespace in the context of this Grammar
	 */
	public abstract boolean isWhitespace(char c);

	/**
	 * check if the specified char should be treated as whitespace if appearing
	 * within a tag
	 * @param c the char to be checked
	 * @return true if and only if the specified char should be treated as
	 *         whitespace in the context of this Grammar if it appears within a
	 *         tag
	 */
	public abstract boolean isTagWhitespace(char c);

	/**
	 * check if the value of the specified attribute may contain whitespaces if
	 * used within the specified tag
	 * @param tag the tag the attribute appears within
	 * @param attribute the attribute to be checked
	 * @return true if and only if the value of the specified attribute may
	 *         contain whitespaces if used within the specified tag in the
	 *         context of this Grammar
	 */
	public abstract boolean valueMayContainWhitespace(String tag, String attribute);
	
	/**
	 * check if the specified char can be used to quote attribute values within
	 * tags
	 * @param c the char to be checked
	 * @return true if and only if the specified char can be used to quote
	 *         attribute values within tags in the context of this Grammar
	 */
	public abstract boolean isTagAttributeValueQuoter(char c);

	/**
	 * translate the specified tag
	 * @param tag the tag to be translated
	 * @return the translated tag name
	 */
	public abstract String translateTag(String tag);

	/**
	 * get all possible parent tags to the specified tag in the context of this
	 * grammar
	 * @param tag the child tag
	 * @return all possible parent tags to the specified tag in the context of
	 *         this grammar
	 */
	public abstract HashSet getParentTags(String tag);

	/**
	 * check if a tag can be child of another tag in the context of this grammar
	 * @param child the child tag type
	 * @param parent the parent tag type
	 * @return true if and only if the specified child tag can be a child of the
	 *         specified parent tag in the context of this grammar
	 */
	public abstract boolean canBeChildOf(String child, String parent);

	/**
	 * check if a tag can appear within another tag in the context of this
	 * grammar
	 * @param child the child tag type
	 * @param parent the direct or indirect parent tag type
	 * @return true if and only if the specified child tag can appear within the
	 *         specified parent tag in the context of this grammar
	 */
	public abstract boolean canBeEmbeddedIn(String child, String parent);
	
	/**
	 * get the encoding sequence for the specified character
	 * @param c the character to be encoded
	 * @return the encoding sequence for the specified character (e.g.
	 *         &amp;quot; for &quot; in HTML), or null if there is no special
	 *         code for the specified character
	 */
	public abstract String getCharCode(char c);
	
	/**
	 * get the character for a given encoding sequence. This implementation
	 * decodes codes in decimal and hexadecimal notation. Other encodings are to
	 * be handled by sub classes.
	 * @param code the encoding sequence to be decoded
	 * @return the character encoded by the argument encoding sequence (e.g.
	 *         &quot; for &amp;quot; in HTML), or 0 if the specified character
	 *         code is invalid
	 */
	public char getPlainChar(String code) {
		if ((code == null) || (code.trim().length() == 0))
			return 0;
		code = code.trim();
		
		//	decode hex code
		if (code.startsWith("&#x")) {
			if (code.endsWith(";"))
				code = code.substring(0, (code.length() - 1));
			try {
				return ((char) Integer.parseInt(code.substring("&#x".length()), 16));
			} catch (NumberFormatException nfe) {}
		}
		
		//	decode hex code
		else if (code.startsWith("&x")) {
			if (code.endsWith(";"))
				code = code.substring(0, (code.length() - 1));
			try {
				return ((char) Integer.parseInt(code.substring("&x".length()), 16));
			} catch (NumberFormatException nfe) {}
		}
		
		//	handle decimal encoding
		else if (code.startsWith("&#")) {
			if (code.endsWith(";"))
				code = code.substring(0, (code.length() - 1));
			try {
				return ((char) Integer.parseInt(code.substring("&#".length())));
			} catch (NumberFormatException nfe) {}
		}
		
		//	we could not handle this one ...
		return 0;
	}
	
	/**
	 * check if a String encodes a special character
	 * @param code the String to be checked
	 * @return true if and only if the specified String encodes a special
	 *         character (e.g. &amp;quot; in HTML)
	 */
	public boolean isCharCode(String code) {
		return (this.getPlainChar(code) != 0);
	}

	
	/**
	 * Escape a string according to the grammar's rules, e.g. escape &amp;,
	 * &lt;, &gt;, and &quot; in XML context.
	 * @param string the string to escape
	 * @return the escaped string
	 */
	public abstract String escape(String string);
	
	/**
	 * Un-escape a string according to the grammar's rules, e.g. un-escape
	 * &amp;, &lt;, &gt;, and &quot; in XML context.
	 * @param escapedString the string to un-escape
	 * @return the un-escaped string
	 */
	public abstract String unescape(String escapedString);
	
	/**
	 * @return true if and only if the TokenSource should first read, tokenize
	 *         and verify the input up to the end tag of the specified tag
	 *         before passing more tokens to the Parser if the specified tag is
	 *         read. The longest end tag waited for must be at most as long as
	 *         the character lookahead returned by getCharLookahead()
	 * 
	 */
	public boolean waitForEndTag(String tag) {
		return false;
	}

	/**
	 * @return the number of tokens the verifyTokenSequence method of this
	 *         Grammar needs to look ahead on verification
	 */
	public int getTokenLookahead() {
		return 0;
	}

	/**
	 * apply some specific error correction to the parsed string before the tree
	 * is built up
	 * @param ts the the token sequence in a Vector
	 */
	public abstract void ckeckTokenSequence(Vector ts);
}
