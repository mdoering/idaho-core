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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;

import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;

/**
 * Default Grammar for handling HTML, defining all the elements, etc. This
 * grammar switches on error correction in order to facilitate parsing any
 * given real-world HTML page as far as possible.
 * 
 * @author sautter
 */
public class Html extends StandardGrammar {
	
	//	HTML specific data
	private static final String tagTypeToParentTypesMappingStrings[] = {
			"p:address;applet;blockquote;body;button;center;del;dd;div;span;fieldset;form;iframe;ins;li;map;noframes;noscript;object;header;nav;main;article;section;footer;aside;address;td;th",
			"noframes:applet;blockquote;body;button;center;dd;del;div;span;fieldset;form;frameset;iframe;ins;li;map;noscript;object;header;nav;main;article;section;footer;aside;address;td;th",
			"h6:h5:h4:h3:h2:h1:table:ul:ol:menu:dl:dir:pre:noscript:hr:div:center:header:nav:main:article:section:footer:aside:address:blockquote:address:applet;blockquote;body;button;center;dd;del;div;span;fieldset;form;iframe;ins;li;map;noframes;noscript;object;header;nav;main;article;section;footer;aside;address;td;th",
			"isindex:applet;blockquote;body;center;dd;del;div;span;fieldset;form;head;iframe;ins;li;map;noframes;noscript;object;header;nav;main;article;section;footer;aside;address;td;th",
			"fieldset:applet;blockquote;body;center;dd;del;div;span;fieldset;form;iframe;ins;li;map;noframes;noscript;object;header;nav;main;article;section;footer;aside;address;td;th",
			"form:applet;blockquote;body;center;dd;del;div;span;fieldset;iframe;ins;li;map;noframes;noscript;object;header;nav;main;article;section;footer;aside;address;table;td;th;tr",
			"param:applet;object",
			"col:colgroup;table",
			"dd:dt:dl",
			"legend:fieldset",
			"frame:frameset",
			"title:style:meta:link:base:head",
			"head:html",
			"body:htm;html;noframes",
			"frameset:htm;html;frameset",
			"area:map",
			"li:ol;ul;dir;font;menu",
			"optgroup:select",
			"option:select;optgroup",
			"tbody:tfoot:thead:colgroup:caption:table;form",
			"tr:table;thead;tfoot;tbody;form",
			"th:td:tr;form",
		};
		
	private static final String tagTypeTranslationStrings[] = {
			"htm:html",
		};
	
	private static final String singularTagTypesString = "area;base;br;hr;img;input;isindex;link;meta;param";
	
	private static final String subsequentEndTagsAllowedTypesString = "frameset;ul;ol;dl;div;span";
	
	private static final String whitespaceFreeValueAttributesString = "align;clear;columns;colspan;dataformatas;dir;for;frame;frameborder;headers;http-equiv;id;maxlength;method;name;nohref;rows;rowspan;rules;scheme;scope;scrolling;shape;span;start;tabindex;type;valign;valuetype;width";
	
	private static final String[] characterEncodingsString = {
			"\" &quot;",
			"' &apos;",
			"& &amp;",
			"< &lt;",
			"> &gt;",
			//"  &nbsp;", do not encode whitespaces
			"¡ &iexcl;",
			"¢ &cent;",
			"£ &pound;",
			"¤ &curren;",
			"¥ &yen;",
			"¦ &brvbar;",
			"§ &sect;",
			"¨ &uml;",
			"© &copy;",
			"ª &ordf;",
			"« &laquo;",
			"¬ &not;",
			"­ &shy;",
			"® &reg;",
			"¯ &macr;",
			"° &deg;",
			"± &plusmn;",
			"² &sup2;",
			"³ &sup3;",
			"´ &acute;",
			"µ &micro;",
			"¶ &para;",
			"· &middot;",
			"¸ &cedil;",
			"¹ &sup1;",
			"º &ordm;",
			"» &raquo;",
			"¼ &frac14;",
			"½ &frac12;",
			"¾ &frac34;",
			"¿ &iquest;",
			"À &Agrave;",
			"Á &Aacute;",
			"Â &Acirc;",
			"Ã &Atilde;",
			"Ä &Auml;",
			"Å &Aring;",
			"Æ &AElig;",
			"Ç &Ccedil;",
			"È &Egrave;",
			"É &Eacute;",
			"Ê &Ecirc;",
			"Ë &Euml;",
			"Ì &Igrave;",
			"Í &Iacute;",
			"? &Iuml;",
			"Î &Icirc;",
			"Ï &Iuml;",
			"Ð &ETH;",
			"Ñ &Ntilde;",
			"Ò &Ograve;",
			"Ó &Oacute;",
			"Ô &Ocirc;",
			"Õ &Otilde;",
			"Ö &Ouml;",
			"Œ &OElig;",
			"× &times;",
			"Ø &Oslash;",
			"Ù &Ugrave;",
			"Ú &Uacute;",
			"Û &Ucirc;",
			"Ü &Uuml;",
			"Ý &Yacute;",
			"Þ &THORN;",
			"ß &szlig;",
			"à &agrave;",
			"á &aacute;",
			"â &acirc;",
			"ã &atilde;",
			"ä &auml;",
			"å &aring;",
			"æ &aelig;",
			"ç &ccedil;",
			"è &egrave;",
			"é &eacute;",
			"ê &ecirc;",
			"ë &euml;",
			"ì &igrave;",
			"í &iacute;",
			"î &icirc;",
			"ï &iuml;",
			"ð &eth;",
			"ñ &ntilde;",
			"ò &ograve;",
			"ó &oacute;",
			"ô &ocirc;",
			"õ &otilde;",
			"ö &ouml;",
			"œ &oelig;",
			"÷ &divide;",
			"ø &oslash;",
			"ù &ugrave;",
			"ú &uacute;",
			"û &ucirc;",
			"ü &uuml;",
			"ý &yacute;",
			"þ &thorn;",
			"ÿ &yuml;",
			"? &Alpha;",
			"? &alpha;",
			"? &Beta;",
			"? &beta;",
			"? &Gamma;",
			"? &gamma;",
			"? &Delta;",
			"? &delta;",
			"? &Epsilon;",
			"? &epsilon;",
			"? &Zeta;",
			"? &zeta;",
			"? &Eta;",
			"? &eta;",
			"? &Theta;",
			"? &theta;",
			"? &Iota;",
			"? &iota;",
			"? &Kappa;",
			"? &kappa;",
			"? &Lambda;",
			"? &lambda;",
			"? &Mu;",
			"? &mu;",
			"? &Nu;",
			"? &nu;",
			"? &Xi;",
			"? &xi;",
			"? &Omicron;",
			"? &omicron;",
			"? &Pi;",
			"? &pi;",
			"? &Rho;",
			"? &rho;",
			"? &Sigma;",
			"? &sigmaf;",
			"? &sigma;",
			"? &Tau;",
			"? &tau;",
			"? &Upsilon;",
			"? &upsilon;",
			"? &Phi;",
			"? &phi;",
			"? &Chi;",
			"? &chi;",
			"? &Psi;",
			"? &psi;",
			"? &Omega;",
			"? &omega;",
			"? &thetasym;",
			"? &upsih;",
			"? &piv;",
			"? &forall;",
			"? &part;",
			"? &exist;",
			"? &empty;",
			"? &nabla;",
			"? &isin;",
			"? &notin;",
			"? &ni;",
			"? &prod;",
			"? &sum;",
			"? &minus;",
			"? &lowast;",
			"? &radic;",
			"? &prop;",
			"? &infin;",
			"? &ang;",
			"? &and;",
			"? &or;",
			"? &cap;",
			"? &cup;",
			"? &int;",
			"? &there4;",
			"? &sim;",
			"? &cong;",
			"? &asymp;",
			"? &ne;",
			"? &equiv;",
			"? &le;",
			"? &ge;",
			"? &sub;",
			"? &sup;",
			"? &nsub;",
			"? &sube;",
			"? &supe;",
			"? &oplus;",
			"? &otimes;",
			"? &perp;",
			"? &sdot;",
			"? &loz;",
			"? &lceil;",
			"? &rceil;",
			"? &lfloor;",
			"? &rfloor;",
			"? &lang;",
			"? &rang;",
			"? &larr;",
			"? &uarr;",
			"? &rarr;",
			"? &darr;",
			"? &harr;",
			"? &crarr;",
			"? &lArr;",
			"? &uArr;",
			"? &rArr;",
			"? &dArr;",
			"? &hArr;",
			"• &bull;",
			"? &prime;",
			"? &oline;",
			"? &frasl;",
			"? &weierp;",
			"? &image;",
			"? &real;",
			"™ &trade;",
			"€ &euro;",
			"? &alefsym;",
			"? &spades;",
			"? &clubs;",
			"? &hearts;",
			"? &diams;",
			"? &ensp;",
			"? &emsp;",
			"? &thinsp;",
			"? &zwnj;",
			"? &zwj;",
			"? &lrm;",
			"? &rlm;",
			"– &ndash;",
			"— &mdash;",
			"‘ &lsquo;",
			"’ &rsquo;",
			"‚ &sbquo;",
			"“ &ldquo;",
			"” &rdquo;",
			"„ &bdquo;",
			"† &dagger;",
			"‡ &Dagger;",
			"… &hellip;",
			"‰ &permil;",
			"‹ &lsaquo;",
			//"= &equals;",
			"› &rsaquo;",
			"? ?"
		};
	private static final String characterCodesString = "&nbsp;";
//	private final String characterDecCodeCharsString = "0,1,2,3,4,5,6,7,8,9,;";
//	private final String characterHexCodeCharsString = "0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f,;";
	
	//	data structures for specific data
	protected final HashMap tagTypeToParentTypesMappings = new HashMap();
	protected final Properties tagTypeTranslations = new Properties();
	protected final HashSet singularTagTypes = new HashSet();
	protected final HashSet subsequentEndTagsAllowedTypes = new HashSet();
	protected final HashSet whitespaceFreeValueAttributes = new HashSet();
	
	protected final Properties characterEncodings = new Properties();
	protected final Properties characterDecodings = new Properties();
	protected final HashSet characterCodes = new HashSet();
//	protected final HashSet characterDecCodeChars = new HashSet();
//	protected final HashSet characterHexCodeChars = new HashSet();
	
	protected final boolean isStrictXML = false;
	private int charLookahead = 6; // length of '&nbsp;'
	
	/**	Constructor
	 */
	public Html() {
		
		//	initialize tag type --> parent tag types mappings
		for (int i = 0; i < tagTypeToParentTypesMappingStrings.length; i++) {
			String s = tagTypeToParentTypesMappingStrings[i].toLowerCase();
			int split = s.lastIndexOf(":");
			if ((split > -1) && (split < (s.length() - 2))) {
				
				//	separate children and parents
				String types = s.substring(0, split);
				String parents = s.substring(split + 1);
				
				//	parse and store parents
				String[] t = IoTools.parseString(parents, ";");
				HashSet pSet = new HashSet();
				for (int j = 0; j < t.length; j++) pSet.add(t[j]);
				
				//	parse child types and store mappings
				t = IoTools.parseString(types, ":");
				for (int j = 0; j < t.length; j++) this.tagTypeToParentTypesMappings.put(t[j], pSet);
			}
		}
		
		//	initialize tag type translation
		for (int i = 0; i < tagTypeTranslationStrings.length; i++) {
			String s = tagTypeTranslationStrings[i].toLowerCase();
			int split = s.lastIndexOf(":");
			if ((split > -1) && (split < (s.length() - 2))) {
				
				//	separate children and parents
				String types = s.substring(0, s.lastIndexOf(":"));
				String translation = s.substring(s.lastIndexOf(":") + 1);
				
				//	parse child types and store mappings
				String[] t = IoTools.parseString(types, ":");
				for (int j = 0; j < t.length; j++) this.tagTypeTranslations.put(t[j], translation);
			}
		}
		
		//	initialize singular tag types
		String[] t = IoTools.parseString(singularTagTypesString.toLowerCase(), ";");
		for (int i = 0; i < t.length; i++) this.singularTagTypes.add(t[i]);
		
		//	initialize tag types that's end tags may be subsequent
		t = IoTools.parseString(subsequentEndTagsAllowedTypesString.toLowerCase(), ";");
		for (int i = 0; i < t.length; i++) this.subsequentEndTagsAllowedTypes.add(t[i]);
		
		//	initialize parameters that's values may not contain whitespaces, even if quoted
		t = IoTools.parseString(whitespaceFreeValueAttributesString.toLowerCase(), ";");
		for (int i = 0; i < t.length; i++) this.whitespaceFreeValueAttributes.add(t[i]);
		
		//	initialize character encoding and decoding
		for (int i = 0; i < characterEncodingsString.length; i++) {
			String s = characterEncodingsString[i];
			this.characterCodes.add(s.substring(2));
			this.characterEncodings.setProperty(s.substring(0, 1), s.substring(2));
			this.characterDecodings.setProperty(s.substring(2), s.substring(0, 1));
			this.charLookahead = Math.max(this.charLookahead, (s.length() - 2));
		}
		this.characterDecodings.setProperty("&nbsp;", " ");
		
		//	also observe awaiting end tags
		this.charLookahead = Math.max(this.charLookahead, (2 + "textarea".length() + 1));
		
		//	initialize character code list
		t = IoTools.parseString(characterCodesString.toLowerCase(), ",");
		for (int i = 0; i < t.length; i++) this.characterCodes.add(t[i]);
//		
//		//	initialize character decimal code list
//		t = IoTools.parseString(this.characterDecCodeCharsString.toLowerCase(), ",");
//		for (int i = 0; i < t.length; i++) this.characterDecCodeChars.add(t[i]);
//		
//		//	initialize character hexadecimal code list
//		t = IoTools.parseString(this.characterHexCodeCharsString.toLowerCase(), ",");
//		for (int i = 0; i < t.length; i++) this.characterHexCodeChars.add(t[i]);
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isStrictXML()
	 */
	public boolean isStrictXML() {
		return this.isStrictXML;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#correctCharEncoding()
	 */
	public boolean correctCharEncoding() {
		return true;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#waitForEndTag(java.lang.String)
	 */
	public boolean waitForEndTag(String tag) {
		String type = this.getType(tag);
		return (type.equalsIgnoreCase("script") || type.equalsIgnoreCase("style") || type.equalsIgnoreCase("pre") || type.equalsIgnoreCase("textarea"));
	}
	
	/**	@return	the number of tokens the verifyTokenSequence method of this Grammar needs to look ahead on verification
	 */
	public int getTokenLookahead() {
		return 2;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isSingularTag(java.lang.String)
	 */
	public boolean isSingularTag(String tag) {
		String t = this.getType(tag).toLowerCase();
		return (this.isSingularTagType(t) || super.isSingularTag(tag));
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#isSingularTagType(java.lang.String)
	 */
	public boolean isSingularTagType(String type) {
		String t = this.getType(type).toLowerCase();
		return this.singularTagTypes.contains(t);
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#valueMayContainWhitespace(String, String)
	 */
	public boolean valueMayContainWhitespace(String tag, String attribute) {
		return ((attribute == null) || !this.whitespaceFreeValueAttributes.contains(attribute.toLowerCase()));
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#translateTag(java.lang.String)
	 */
	public String translateTag(String tag) {
		String t = this.tagTypeTranslations.getProperty(this.getType(tag));
		return ((t != null) ? t : tag);
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#getParentTags(java.lang.String)
	 */
	public HashSet getParentTags(String tag) {
		HashSet parents = new HashSet();
		Object o = this.tagTypeToParentTypesMappings.get(tag);
		if ((o != null) && (o instanceof HashSet)) parents.addAll((HashSet) o);
		return parents;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#canBeChildOf(java.lang.String, java.lang.String)
	 */
	public boolean canBeChildOf(String child, String parent) {
		Object o = this.tagTypeToParentTypesMappings.get(child.toLowerCase());
		if ((o != null) && (o instanceof HashSet)) {
			HashSet pSet = ((HashSet) o);
			return pSet.contains(parent.toLowerCase());
		} 
		else return true;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#getCharLookahead()
	 */
	public int getCharLookahead() {
		return this.charLookahead;
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#getCharCode(char)
	 */
	public String getCharCode(char c) {
		//	we need this hack as IE does not know &apos;, but we need to decode it anyways
		return ((c == '\'') ? "'" : this.characterEncodings.getProperty(("" + c), ("" + c)));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#getPlainChar(java.lang.String)
	 */
	public char getPlainChar(String code) {
		String ch = this.characterDecodings.getProperty(code);
		return ((ch == null) ? super.getPlainChar(code) : ch.charAt(0));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar#isCharCode(java.lang.String)
	 */
	public boolean isCharCode(String code) {
		return (this.characterDecodings.containsKey(code) || super.isCharCode(code));
	}
	
	/**
	 * This implementation escapes a string for HTML - in particular, it escapes
	 * &lt;, &gt;, and &quot;, but not &amp; if it occurs in a string without
	 * whitespace and after a question mark, in order not to destroy URLs that
	 * include a query. In addition, &amp; is not escaped if it is the start of
	 * an HTML character code.
	 * @param string the string to escape
	 * @return the escaped string
	 * @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#escape(java.lang.String)
	 * @see de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar#escape(java.lang.String)
	 */
	public String escape(String string) {
		StringBuffer escapedString = new StringBuffer();
		boolean hadWhitespace = false;
		boolean hadQuestionMark = false;
		for (int c = 0; c < string.length(); c++) {
			char ch = string.charAt(c);
			if (ch == '?')
				hadQuestionMark = true;
			else if (this.isWhitespace(ch))
				hadWhitespace = true;
			if (ch == '<')
				escapedString.append("&lt;");
			else if (ch == '>')
				escapedString.append("&gt;");
			else if (ch == '"')
				escapedString.append("&quot;");
//			else if (ch == '\'') // this does a lot more harm than good, as many applications don't understand the entity
//				escapedString.append("&apos;");
			else if (ch == '&') {
				int escapeEnd = string.indexOf(';', c);
				if ((escapeEnd != -1) && ((escapeEnd - c) <= this.charLookahead) && this.isCharCode(string.substring(c, (escapeEnd+1))))
					escapedString.append(ch);
				else if (!hadWhitespace && hadQuestionMark)
					escapedString.append(ch);
				else escapedString.append("&amp;");
			}
			else escapedString.append(ch);
		}
		return escapedString.toString();
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar#ckeckTokenSequence(java.util.Vector)
	 */
	public void ckeckTokenSequence(Vector ts) {
		int index = 0;
		boolean inScript = false;
		boolean inStyle = false;
		String lastPreservedPart = null;
		String currentPart;
		StringBuffer collector = new StringBuffer();
		
		while (index < ts.size()) {
			currentPart = ((String) ts.get(index));
			
			//	reassemble parsed scripts
			if (inScript) {
				
				//	end of script
				if (this.isEndTag(currentPart) && this.getType(currentPart).equalsIgnoreCase("script")) {
					inScript = false;
					
					//	store script content, surround it by comment marks if no already so
					if (collector.length() > 0) {
						String script = collector.toString().trim();
//						if (script.startsWith("<!--"))
//							script = "//" + script;
//						else if (!script.startsWith("//<!--"))
//							script = "//<!--\r\n" + script;
//						if (!script.endsWith("-->"))
//							script = script + "\r\n//-->";
						
						ts.insertElementAt(script, index);
						index++;
					}
					
					//	clear collector and store end tag
					collector.delete(0, collector.length());
					lastPreservedPart = currentPart;
					index++;
				}
				
				//	there can be no script within a script
				else if (this.isTag(currentPart) && this.getType(currentPart).equalsIgnoreCase("script"))
					ts.removeElementAt(index);
				
				//	script continues
				else {
					collector.append(currentPart);
					ts.removeElementAt(index);
				}
			}
			
			//	reassemble parsed styles
			else if (inStyle) {
				
				//	end of style
				if (this.isEndTag(currentPart) && this.getType(currentPart).equalsIgnoreCase("style")) {
					inStyle = false;
					
					//	store style content, surround it by comment marks if no already so
					if (collector.length() > 0) {
						String style = collector.toString().trim();
//						if (!(style.startsWith("//") && style.substring(2).trim().startsWith("<!--")) && !style.startsWith("<!--"))
//							style = "<!--\n" + style;
//						if (!style.endsWith("-->"))
//							style = style + "\n//-->";
						
						ts.insertElementAt(style, index);
						index++;
					}
					
					//	clear collector and store end tag
					collector.delete(0, collector.length());
					lastPreservedPart = currentPart;
					index++;
				}
				
				//	there can be no style within a style
				else if (this.isTag(currentPart) && this.getType(currentPart).equalsIgnoreCase("style"))
					ts.removeElementAt(index);
					
				//	style continues
				else {
					collector.append(currentPart);
					ts.removeElementAt(index);
				}
			}
			
			//	check subsequent equal end tags
			else if (this.isEndTag(currentPart) && currentPart.equalsIgnoreCase(lastPreservedPart) && !this.subsequentEndTagsAllowedTypes.contains(this.getType(currentPart).toLowerCase()))
				ts.removeElementAt(index);
			
			//	repair badly marked comments
			else if (currentPart.startsWith("<!-") && currentPart.endsWith("->")) {
				if (!currentPart.startsWith("<!--"))
					currentPart = "<!--" + currentPart.substring("<!-".length());
				if (!currentPart.endsWith("-->"))
					currentPart = currentPart.substring(0, (currentPart.length() - "->".length())) + "-->";
				ts.setElementAt(currentPart, index);
				lastPreservedPart = currentPart;
				index++;
			}
			
			//	reassemble parsed scripts
			else if (this.isTag(currentPart) && !this.isEndTag(currentPart) && this.getType(currentPart).equalsIgnoreCase("script")) {
				inScript = true;
				lastPreservedPart = currentPart;
				index++;
			}
			
			//	reassemble parsed styles
			else if (this.isTag(currentPart) && !this.isEndTag(currentPart) && this.getType(currentPart).equalsIgnoreCase("style")) {
				inStyle = true;
				lastPreservedPart = currentPart;
				index++;
			}
			
			//	otherwise add the part
			else {
				lastPreservedPart = currentPart;
				index++;
			}
		}
	}
}
