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
package de.uka.ipd.idaho.gamta.util;

import java.io.FilterReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.PatternSyntaxException;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Matcher for patterns over annotations and intermediate literals. Annotations
 * can be filtered based on attributes and attribute values, also using regular
 * expression patterns. Individual parts of patterns can be optional or
 * required, but other quantifiers currently do not work; this can be
 * substituted, however, by repeating pattern elements, or by first using a
 * helper pattern to aggregate sequences of annotations of a specific type.
 * Furthermore, the first part of a match cannot be a literal; this can be
 * substituted, however, by annotating occurrences of the literal before
 * invoking the matcher.<br>
 * The pattern syntax is partially XML based:<br>
 * <ul>
 * <li><code>&lt;<i>type</i>&gt;</code>: matches an annotation of the specified
 * type</li>
 * <li>
 * <code>&lt;<i>type</i> <i>attribute</i>=&quot;*&quot;&gt;</code>: matches an
 * annotation of the specified type if it has the specified attribute</li>
 * <li>
 * <code>&lt;<i>type</i> <i>attribute</i>=&quot;<i>value</i>&quot;&gt;</code>:
 * matches an annotation of the specified type if it has the specified attribute
 * with the specified value</li>
 * <li>
 * <code>&lt;<i>type</i> <i>attribute</i>=&quot;(<i>regex</i>)&quot;&gt;</code>:
 * matches an annotation of the specified type if it has the specified attribute
 * with a value matching the regular expression pattern between the parentheses</li>
 * <li>
 * <code>'<i>literal</i>'</code>: matches the literal between the high commas</li>
 * <li>
 * <code>?</code>: marks the annotation or literal immediately preceding it as
 * optional</li>
 * </ul>
 * <br>
 * Examples:<br>
 * <code>&lt;lastName&gt; ', ' &lt;initials&gt;? &lt;firstName&gt; &lt;initials&gt;?</code>
 * <br>
 * This pattern matches a <code>lastName</code> annotation followed by a comma,
 * an optional leading <code>initials</code> annotation, a
 * <code>firstName</code> annotation, and , an optional middle
 * <code>initials</code> annotation.<br>
 * <code>&lt;lastName lang=&quot;en&quot;&gt; ', ' &lt;initials&gt;? &lt;firstName&gt; &lt;initials&gt;?</code>
 * <br>
 * This pattern matches the same, but only if the <code>lastName</code>
 * annotation has a <code>lang</code> attribute with value <code>en</code>.<br>
 * <code>&lt;lastName lang=&quot;(en|fr|de)&quot;&gt; ', ' &lt;initials&gt;? &lt;firstName&gt; &lt;initials&gt;?</code>
 * <br>
 * This pattern again matches the same, but only if the <code>lastName</code>
 * annotation has a <code>lang</code> attribute whose value matches the regular
 * expression pattern <code>en|fr|de</code>.<br>
 * 
 * @author sautter
 */
public class AnnotationPatternMatcher {
	
	/* TODO update documentation to reflect all this:
'...' -> literal: ends a next non-escaped high comma not part of pattern literal
"..." -> regular expression pattern literal: ends a next non-escaped double quote not part of literal or annotation
<...> -> annotation: ends at next closing angle bracket
(...|...) -> disjunction: ends at next closing bracket, elements separated by pipes
(...) -> sub pattern: ends at next closing bracket not part of annotation or literal
{m,n} -> quantifier: ends at next closing curly bracket
?,*,+ -> short quantifier: single chars
	 */
	
	/**
	 * Normalize an annotation pattern, i.e, remove line breaks and indents.
	 * @param pattern the annotation pattern to normalize
	 * @return the argument annotation pattern in its flattened out form
	 */
	public static String normalizePattern(String pattern) {
		AnnotationPattern ap = getPattern(Gamta.INNER_PUNCTUATION_TOKENIZER, pattern);
		return ap.toString(null);
	}
	
	/**
	 * Explode an annotation pattern, i.e., add line breaks and indents to
	 * improve readability.
	 * @param pattern the annotation pattern to explode
	 * @return the argument annotation pattern in a form readable for users
	 */
	public static String explodePattern(String pattern) {
		AnnotationPattern ap = getPattern(Gamta.INNER_PUNCTUATION_TOKENIZER, pattern);
		return ap.toString("");
	}
	
	private static Grammar grammar = new StandardGrammar();
	
	private static class AnnotationPattern {
		AnnotationPatternElement[] elements;
		AnnotationPattern(AnnotationPatternElement[] elements) {
			this.elements = elements;
		}
		public String toString() {
			return this.toString("");
		}
		public String toString(String indent) {
			StringBuffer sea = new StringBuffer();
			for (int e = 0; e < this.elements.length; e++) {
				if (e != 0)
					sea.append((indent == null) ? " " : "\r\n");
				sea.append(this.elements[e].toString(indent));
			}
			return sea.toString();
		}
	}
	
	private static class AnnotationPatternElement {
		String annotationType = null;
		TreeNodeAttributeSet annotationAttributes = null;
		String patternLiteral = null;
		TokenSequence tokenLiteral = null;
		AnnotationPatternElement[] sequenceElements = null;
		AnnotationPatternElement[] alternativeElements = null;
		int minCount = 1;
		int maxCount = 1;
		AnnotationPatternElement(TokenSequence tokenLiteral) {
			this.tokenLiteral = tokenLiteral;
		}
		AnnotationPatternElement(String patternLiteral) {
			this.patternLiteral = patternLiteral;
		}
		AnnotationPatternElement(String annotationType, TreeNodeAttributeSet annotationAttributes) {
			this.annotationType = annotationType;
			this.annotationAttributes = ((annotationAttributes.size() > 0) ? annotationAttributes : null);
		}
		AnnotationPatternElement(AnnotationPatternElement[] subElements, boolean isSequence) {
			if (isSequence)
				this.sequenceElements = subElements;
			else this.alternativeElements = subElements;
		}
		public String toString(String indent) {
			
			//	assemble quantifier
			String quantifier;
			if (this.minCount == 0) {
				if (this.maxCount == 1)
					quantifier = "?";
				else if (this.maxCount == 0xFFFF)
					quantifier = "*";
				else quantifier = ("{0," + this.maxCount + "}");
			}
			else if (this.minCount == 1) {
				if (this.maxCount == 1)
					quantifier = "";
				else if (this.maxCount == 0xFFFF)
					quantifier = "+";
				else quantifier = ("{1," + this.maxCount + "}");
			}
			else {
				if (this.minCount == this.maxCount)
					quantifier = ("{" + this.minCount + "}");
				else if (this.maxCount == 0xFFFF)
					quantifier = ("{" + this.minCount + ",}");
				else quantifier = ("{" + this.minCount + "," + this.maxCount + "}");
			}
			
			//	literal
			if (this.tokenLiteral != null) {
				String tokens = this.tokenLiteral.toString();
				StringBuffer escapedTokens = new StringBuffer();
				for (int c = 0; c < tokens.length(); c++) {
					char ch = tokens.charAt(c);
					if ((ch == '\\') || (ch == '\''))
						escapedTokens.append('\\');
					escapedTokens.append(ch);
				}
				if (indent == null)
					return ("'" + escapedTokens.toString() + "'" + quantifier);
				else return (indent + "'" + escapedTokens.toString() + "'" + quantifier);
			}
			
			//	pattern literal
			if (this.patternLiteral != null) {
				StringBuffer escapedPattern = new StringBuffer();
				for (int c = 0; c < this.patternLiteral.length(); c++) {
					char ch = this.patternLiteral.charAt(c);
					if ((ch == '\\') || (ch == '\''))
						escapedPattern.append('\\');
					escapedPattern.append(ch);
				}
				if (indent == null)
					return ("\"" + escapedPattern.toString() + "\"" + quantifier);
				else return (indent + "\"" + escapedPattern.toString() + "\"" + quantifier);
			}
			
			//	annotation
			if (this.annotationType != null) {
				if (indent == null)
					return ("<" + this.annotationType + ((this.annotationAttributes == null) ? "" : (" " + this.annotationAttributes.getAttributeValueString(grammar))) + ">" + quantifier);
				else return (indent + "<" + this.annotationType + ((this.annotationAttributes == null) ? "" : (" " + this.annotationAttributes.getAttributeValueString(grammar))) + ">" + quantifier);
			}
			
			//	sequence sub pattern
			if (this.sequenceElements != null) {
				StringBuffer sea = new StringBuffer();
				if (indent == null) {
					sea.append("(");
					for (int e = 0; e < this.sequenceElements.length; e++) {
						if (e != 0)
							sea.append(" ");
						sea.append(this.sequenceElements[e].toString(null));
					}
					sea.append(")");
				}
				else {
					sea.append(indent + "(");
					if ((this.sequenceElements.length == 1) && ((this.sequenceElements[0].tokenLiteral != null) || (this.sequenceElements[0].annotationType != null))) {
						sea.append(this.sequenceElements[0].toString(""));
						sea.append(")");
					}
					else {
						for (int e = 0; e < this.sequenceElements.length; e++) {
							sea.append("\r\n");
							sea.append(this.sequenceElements[e].toString(indent + "  "));
						}
						sea.append("\r\n" + indent + ")");
					}
				}
				sea.append(quantifier);
				return sea.toString();
			}
			
			//	disjunction sub pattern
			if (this.alternativeElements != null) {
				StringBuffer sea = new StringBuffer();
				if (indent == null) {
					sea.append("(");
					for (int e = 0; e < this.alternativeElements.length; e++) {
						if (e != 0)
							sea.append("|");
						sea.append(this.alternativeElements[e].toString(null));
					}
					sea.append(")");
				}
				else {
					sea.append(indent + "(");
					for (int e = 0; e < this.alternativeElements.length; e++) {
						sea.append("\r\n");
						if (e != 0)
							sea.append(indent + "  " + "|\r\n");
						sea.append(this.alternativeElements[e].toString(indent + "  "));
					}
					sea.append("\r\n" + indent + ")");
				}
				sea.append(quantifier);
				return sea.toString();
			}
			
			//	whatever else ...
			return "";
		}
	}
	
	/**
	 * A node of a match tree, detailing on matching result in debug mode.
	 * 
	 * @author sautter
	 */
	public static class MatchTreeNode {
		AnnotationPatternElement matched;
		Annotation match;
		LinkedList children = null;
		MatchTreeNode(AnnotationPatternElement matched, Annotation match) {
			this.matched = matched;
			this.match = match;
		}
		public String getPattern() {
			return this.matched.toString(null);
		}
		public Annotation getMatch() {
			return this.match;
		}
		void addChild(MatchTreeNode child) {
			if (this.children == null)
				this.children = new LinkedList();
			this.children.addLast(child);
		}
		public MatchTreeNode[] getChildren() {
			return ((MatchTreeNode[]) this.children.toArray(new MatchTreeNode[this.children.size()]));
		}
		public String toString() {
			return this.toString("");
		}
		public String toString(String indent) {
			StringBuffer sb = new StringBuffer(indent + this.matched.toString(null) + " ==> " + this.match.toXML());
			for (Iterator cit = this.children.iterator(); cit.hasNext();) {
				sb.append("\r\n");
				sb.append(((MatchTreeNode) cit.next()).toString(indent + "  "));
			}
			return sb.toString();
		}
	}
	
	/**
	 * A leaf of a match tree, detailing on matching result in debug mode.
	 * 
	 * @author sautter
	 */
	public static class MatchTreeLeaf extends MatchTreeNode {
		MatchTreeLeaf(AnnotationPatternElement matched, Annotation match) {
			super(matched, match);
		}
		void addChild(MatchTreeNode child) {}
		public MatchTreeNode[] getChildren() {
			return null;
		}
		public String toString(String indent) {
			return (indent + this.matched.toString(null) + " ==> " + this.match.toXML());
		}
	}
	
	/**
	 * Root node of a match tree, detailing on matching result in debug mode.
	 * 
	 * @author sautter
	 */
	public static class MatchTree extends MatchTreeNode {
		MatchTree(Annotation match) {
			super(null, match);
		}
		public String toString(String indent) {
			StringBuffer sb = new StringBuffer(this.match.toXML());
			for (Iterator cit = this.children.iterator(); cit.hasNext();) {
				sb.append("\r\n");
				sb.append(((MatchTreeNode) cit.next()).toString(indent + "  "));
			}
			return sb.toString();
		}
	}
	
	/**
	 * An annotation index provides quick access to individual annotations by
	 * type and start index to speed up matching.
	 * 
	 * @author sautter
	 */
	public static class AnnotationIndex {
		private HashMap index = new HashMap();
		private AnnotationIndex defIndex;
		private QueriableAnnotation data;
		private HashSet dataRetrievedTypes;
		
		/** Constructor
		 */
		public AnnotationIndex() {}
		
		/**
		 * Constructor
		 * @param data the queriable annotation to index
		 * @param defIndex an additional annotation index that can provide
		 *            further annotations (may be null)
		 */
		public AnnotationIndex(QueriableAnnotation data, AnnotationIndex defIndex) {
			this.defIndex = defIndex;
			this.data = data;
			this.dataRetrievedTypes = new HashSet();
		}
		
		/**
		 * Index an array of annotations.
		 * @param annots the annotations to index
		 */
		public void addAnnotations(Annotation[] annots) {
			for (int a = 0; a < annots.length; a++)
				this.getAnnotationList(annots[a].getType(), annots[a].getStartIndex(), true).add(annots[a]);
		}
		
		/**
		 * Index an array of annotations of a specific type.
		 * @param annots the annotations to index
		 * @param type the type of the annotations to index
		 */
		public void addAnnotations(Annotation[] annots, String type) {
			for (int a = 0; a < annots.length; a++)
				this.getAnnotationList(type, annots[a].getStartIndex(), true).add(annots[a]);
		}
		Annotation[] getAnnotations(String type, int startIndex) {
			ArrayList al = this.getAnnotationList(type, startIndex, false);
			if ((al == null) && (this.data != null) && this.dataRetrievedTypes.add(type)) {
				this.addAnnotations(this.data.getAnnotations(type), type);
				al = this.getAnnotationList(type, startIndex, false);
			}
			if (this.defIndex != null) {
				if (al == null)
					return this.defIndex.getAnnotations(type, startIndex);
				al = new ArrayList(al);
				ArrayList dal = this.defIndex.getAnnotationList(type, startIndex, false);
				if (dal != null)
					al.addAll(dal);
			}
			return ((al == null) ? new Annotation[0] : ((Annotation[]) al.toArray(new Annotation[al.size()])));
		}
		ArrayList getAnnotationList(String type, int startIndex, boolean create) {
			String alk = ("" + startIndex + " " + type);
			ArrayList al = ((ArrayList) this.index.get(alk));
			if ((al == null) && create) {
				al = new ArrayList(2);
				this.index.put(alk, al);
			}
			return al;
		}
	}
	
	/**
	 * Attempt to match an annotation pattern against a queriable annotation.
	 * The children of the argument queriable annotation will be indexed
	 * automatically.
	 * @param data the queriable annotation to match against
	 * @param pattern the pattern to match
	 * @return an array holding the matches of the pattern
	 */
	public static Annotation[] getMatches(QueriableAnnotation data, String pattern) {
		return getMatches(data, null, pattern);
	}
	
	/**
	 * Attempt to match an annotation pattern against a queriable annotation.
	 * The children of the argument queriable annotation will be indexed
	 * automatically.
	 * @param data the queriable annotation to match against
	 * @param pattern the pattern to match
	 * @return an array holding the matches of the pattern
	 */
	public static MatchTree[] getMatchTrees(QueriableAnnotation data, String pattern) {
		return getMatchTrees(data, null, pattern);
	}
	
	/**
	 * Attempt to match an annotation pattern against a queriable annotation.
	 * The children of the argument queriable annotation will be indexed
	 * automatically. The argument annotation index can contain further
	 * annotation.
	 * @param data the queriable annotation to match against
	 * @param annotationIndex an index holding additional annotations belonging
	 *            to the token sequence underneith the argument queriable
	 *            annotation
	 * @param pattern the pattern to match
	 * @return an array holding the matches of the pattern
	 */
	public static Annotation[] getMatches(QueriableAnnotation data, AnnotationIndex annotationIndex, String pattern) {
		return getMatches(((TokenSequence) data), new AnnotationIndex(data, annotationIndex), pattern);
	}
	
	/**
	 * Attempt to match an annotation pattern against a queriable annotation.
	 * The children of the argument queriable annotation will be indexed
	 * automatically. The argument annotation index can contain further
	 * annotation.
	 * @param data the queriable annotation to match against
	 * @param annotationIndex an index holding additional annotations belonging
	 *            to the token sequence underneith the argument queriable
	 *            annotation
	 * @param pattern the pattern to match
	 * @return an array holding the matches of the pattern
	 */
	public static MatchTree[] getMatchTrees(QueriableAnnotation data, AnnotationIndex annotationIndex, String pattern) {
		return getMatchTrees(((TokenSequence) data), new AnnotationIndex(data, annotationIndex), pattern);
	}
	
	/**
	 * Attempt to match an annotation pattern against a token sequence with
	 * existing annotations.
	 * @param tokens the token sequence to match against
	 * @param annotationIndex an index holding the annotations belonging to the
	 *            token sequence
	 * @param pattern the pattern to match
	 * @return an array holding the matches of the pattern
	 */
	public static Annotation[] getMatches(TokenSequence tokens, AnnotationIndex annotationIndex, String pattern) {
		MatchTree[] matchTrees = getMatchTrees(tokens, annotationIndex, pattern);
		Annotation[] matches = new Annotation[matchTrees.length];
		for (int m = 0; m < matchTrees.length; m++)
			matches[m] = matchTrees[m].getMatch();
		return matches;
	}
	
	/**
	 * Attempt to match an annotation pattern against a token sequence with
	 * existing annotations.
	 * @param tokens the token sequence to match against
	 * @param annotationIndex an index holding the annotations belonging to the
	 *            token sequence
	 * @param pattern the pattern to match
	 * @return an array holding the matches of the pattern
	 */
	public static MatchTree[] getMatchTrees(TokenSequence tokens, AnnotationIndex annotationIndex, String pattern) {
		
		//	compile pattern
		AnnotationPattern ap = getPattern(tokens.getTokenizer(), pattern);
		
		//	resolve pattern literals into temporary annotations
		AnnotationIndex patternLiteralMatchIndex = new AnnotationIndex();
		HashSet indexedPatternLiteralMatchTypes = new HashSet();
		for (int e = 0; e < ap.elements.length; e++)
			indexPatternLiteralMatches(tokens, ap.elements[e], patternLiteralMatchIndex, indexedPatternLiteralMatchTypes);
		
		//	do matching
		LinkedList matches = new LinkedList();
		LinkedList matchTree = new LinkedList();
		for (int s = 0; s < tokens.size(); s++) {
			step(tokens, s, s, ap.elements, 0, 0, annotationIndex, patternLiteralMatchIndex, matches, 0, matchTree);
			matchTree.clear();
		}
		
		//	finally ...
		return ((MatchTree[]) matches.toArray(new MatchTree[matches.size()]));
	}
	
	private static void indexPatternLiteralMatches(TokenSequence tokens, AnnotationPatternElement ape, AnnotationIndex patternLiteralMatchIndex, HashSet indexedPatternLiteralMatchTypes) {
		
		//	pattern literal
		if (ape.patternLiteral != null) {
			String patternLiteralMatchType = ("regEx" + ape.patternLiteral.hashCode());
			if (indexedPatternLiteralMatchTypes.add(patternLiteralMatchType)) {
				Annotation[] plms = Gamta.extractAllMatches(tokens, ape.patternLiteral, true, false);
				patternLiteralMatchIndex.addAnnotations(plms, patternLiteralMatchType);
			}
		}
		
		//	sub pattern
		else if (ape.sequenceElements != null) {
			for (int e = 0; e < ape.sequenceElements.length; e++)
				indexPatternLiteralMatches(tokens, ape.sequenceElements[e], patternLiteralMatchIndex, indexedPatternLiteralMatchTypes);
		}
		
		//	disjunction
		else if (ape.alternativeElements != null) {
			for (int e = 0; e < ape.alternativeElements.length; e++)
				indexPatternLiteralMatches(tokens, ape.alternativeElements[e], patternLiteralMatchIndex, indexedPatternLiteralMatchTypes);
		}
	}
	
	private static void step(TokenSequence tokens, int matchStart, int matchFrom, AnnotationPatternElement[] pattern, int elementIndex, int elementMatchCount, AnnotationIndex annotationIndex, AnnotationIndex patternLiteralMatchIndex, LinkedList matches, int matchDepth, LinkedList matchTree) {
		
		//	end of pattern reached, we have a match
		if (pattern.length == elementIndex) {
			if (matchStart < matchFrom) {
				Annotation match = Gamta.newAnnotation(tokens, null, matchStart, (matchFrom-matchStart));
				MatchTree mt = new MatchTree(match);
				for (Iterator mtit = matchTree.iterator(); mtit.hasNext();)
					mt.addChild((MatchTreeNode) mtit.next());
				matches.add(mt);
			}
			return;
		}
		
		//	we can do without (further) matches of current element
		if (pattern[elementIndex].minCount <= elementMatchCount)
			step(tokens, matchStart, matchFrom, pattern, (elementIndex+1), 0, annotationIndex, patternLiteralMatchIndex, matches, matchDepth, matchTree);
		
		//	we cannot do with any further matches of current element
		if (pattern[elementIndex].maxCount <= elementMatchCount)
			return;
		
		//	literal
		if (pattern[elementIndex].tokenLiteral != null) {
			if (TokenSequenceUtils.startsWith(tokens, pattern[elementIndex].tokenLiteral, matchFrom)) {
				matchTree.addLast(new MatchTreeLeaf(pattern[elementIndex], Gamta.newAnnotation(tokens, "literal", matchFrom, pattern[elementIndex].tokenLiteral.size())));
				step(tokens, matchStart, (matchFrom + pattern[elementIndex].tokenLiteral.size()), pattern, elementIndex, (elementMatchCount + 1), annotationIndex, patternLiteralMatchIndex, matches, matchDepth, matchTree);
				matchTree.removeLast();
			}
			return;
		}
		
		//	pattern literal
		if (pattern[elementIndex].patternLiteral != null) {
			Annotation[] annots = patternLiteralMatchIndex.getAnnotations(("regEx" + pattern[elementIndex].patternLiteral.hashCode()), matchFrom);
			for (int a = 0; a < annots.length; a++) {
				matchTree.addLast(new MatchTreeLeaf(pattern[elementIndex], annots[a]));
				step(tokens, matchStart, (matchFrom + annots[a].size()), pattern, elementIndex, (elementMatchCount + 1), annotationIndex, patternLiteralMatchIndex, matches, matchDepth, matchTree);
				matchTree.removeLast();
			}
			return;
		}
		
		//	annotation
		if (pattern[elementIndex].annotationType != null) {
			Annotation[] annots = annotationIndex.getAnnotations(pattern[elementIndex].annotationType, matchFrom);
			for (int a = 0; a < annots.length; a++) {
				boolean attribMatch = true;
				if (pattern[elementIndex].annotationAttributes != null) {
					String[] attribNames = pattern[elementIndex].annotationAttributes.getAttributeNames();
					for (int n = 0; n < attribNames.length; n++) {
						System.out.println(" - " + attribNames[n]);
						Object attribValueObj = annots[a].getAttribute(attribNames[n]);
						if (attribValueObj == null) {
							attribMatch = false;
							break;
						}
						String attribTest = pattern[elementIndex].annotationAttributes.getAttribute(attribNames[n]);
						System.out.println(getIndent(matchDepth) + " - test is " + attribTest);
						if ("*".equals(attribTest))
							continue;
						String attribValue = attribValueObj.toString();
						if (attribTest.startsWith("(") && attribTest.endsWith(")")) {
							if (!attribValue.matches(attribTest)) {
								attribMatch = false;
								System.out.println(getIndent(matchDepth) + " --> pattern match failed");
								break;
							}
						}
						else if (!attribTest.equalsIgnoreCase(attribValue)) {
							attribMatch = false;
							System.out.println(getIndent(matchDepth) + " --> value match failed");
							break;
						}
					}
				}
				if (attribMatch) {
					matchTree.addLast(new MatchTreeLeaf(pattern[elementIndex], annots[a]));
					step(tokens, matchStart, (matchFrom + annots[a].size()), pattern, elementIndex, (elementMatchCount + 1), annotationIndex, patternLiteralMatchIndex, matches, matchDepth, matchTree);
					matchTree.removeLast();
				}
			}
			return;
		}
		
		//	sequence sub pattern
		if (pattern[elementIndex].sequenceElements != null) {
			LinkedList subMatches = new LinkedList();
			LinkedList subMatchTree = new LinkedList();
			step(tokens, matchFrom, matchFrom, pattern[elementIndex].sequenceElements, 0, 0, annotationIndex, patternLiteralMatchIndex, subMatches, (matchDepth + 1), subMatchTree);
			for (Iterator smtit = subMatches.iterator(); smtit.hasNext();) {
				MatchTree smt = ((MatchTree) smtit.next());
				smt.getMatch().changeTypeTo("sequence");
				MatchTreeNode mtn = new MatchTreeNode(pattern[elementIndex], smt.getMatch());
				for (Iterator cit = smt.children.iterator(); cit.hasNext();)
					mtn.addChild((MatchTreeNode) cit.next());
				matchTree.addLast(mtn);
				step(tokens, matchStart, (matchFrom + smt.getMatch().size()), pattern, elementIndex, (elementMatchCount + 1), annotationIndex, patternLiteralMatchIndex, matches, matchDepth, matchTree);
				matchTree.removeLast();
			}
		}
		
		//	disjunction sub pattern
		if (pattern[elementIndex].alternativeElements != null) {
			AnnotationPatternElement[] subPattern = {null};
			LinkedList subMatches = new LinkedList();
			LinkedList subMatchTree = new LinkedList();
			for (int a = 0; a < pattern[elementIndex].alternativeElements.length; a++) {
				subPattern[0] = pattern[elementIndex].alternativeElements[a];
				step(tokens, matchFrom, matchFrom, subPattern, 0, 0, annotationIndex, patternLiteralMatchIndex, subMatches, (matchDepth + 1), subMatchTree);
				for (Iterator smtit = subMatches.iterator(); smtit.hasNext();) {
					MatchTree smt = ((MatchTree) smtit.next());
					smt.getMatch().changeTypeTo("alternative");
					MatchTreeNode mtn = new MatchTreeNode(pattern[elementIndex], smt.getMatch());
					for (Iterator cit = smt.children.iterator(); cit.hasNext();)
						mtn.addChild((MatchTreeNode) cit.next());
					matchTree.addLast(mtn);
					step(tokens, matchStart, (matchFrom + smt.getMatch().size()), pattern, elementIndex, (elementMatchCount + 1), annotationIndex, patternLiteralMatchIndex, matches, matchDepth, matchTree);
					matchTree.removeLast();
				}
				subMatches.clear();
			}
		}
	}
	
	private static ArrayList indents = new ArrayList();
	private static String getIndent(int depth) {
		if (indents.isEmpty())
			indents.add("");
		while (indents.size() <= depth)
			indents.add(((String) indents.get(indents.size()-1)) + "  ");
		return ((String) indents.get(depth));
	}
	
	private static HashMap patternCache = new HashMap();
	private static AnnotationPattern getPattern(Tokenizer tokenizer, String pattern) {
		AnnotationPattern ap = ((AnnotationPattern) patternCache.get(pattern));
		if (ap == null) try {
			ap = parsePattern(tokenizer, pattern);
			patternCache.put(pattern, ap);
		}
		catch (IOException ioe) {
			if (ioe instanceof AnnotationPatternParseException)
				throw new PatternSyntaxException(ioe.getMessage(), pattern, ((AnnotationPatternParseException) ioe).getIndex());
			else return null; // never gonna happen, but Java don't know ...
		}
		return ap;
	}
	
	private static AnnotationPattern parsePattern(final Tokenizer tokenizer, String pattern) throws IOException {
		PatternReader pr = new PatternReader(pattern);
		LinkedList elements = new LinkedList();
		for (AnnotationPatternElement ape; ((ape = cropNext(tokenizer, pr)) != null);)
			elements.add(ape);
		return new AnnotationPattern(((AnnotationPatternElement[]) elements.toArray(new AnnotationPatternElement[elements.size()])));
	}
	
	private static AnnotationPatternElement cropNext(Tokenizer tokenizer, PatternReader pr) throws IOException {
		pr.skipSpace();
		int ch = pr.peek();
		if (ch == -1)
			return null;
		
		//	crop next element on this nesting level
		AnnotationPatternElement ape;
		if (ch == '\'')
			ape = cropLiteral(tokenizer, pr);
		else if (ch == '"')
			ape = cropPattern(pr);
		else if (ch == '<')
			ape = cropAnnotation(pr);
		else if (ch == '(') {
			LinkedList spes = new LinkedList();
			boolean isSequence = cropSubPattern(tokenizer, pr, spes);
			ape = new AnnotationPatternElement(((AnnotationPatternElement[]) spes.toArray(new AnnotationPatternElement[spes.size()])), isSequence);
		}
		else throw new AnnotationPatternParseException(("Unexpected character: " + ((char) ch)), pr.readSoFar());
		
		//	crop associated quantifier, if any
		pr.skipSpace();
		int quantifierStart = pr.readSoFar();
		if ("?*+{".indexOf(pr.peek()) != -1) {
			int quantifier = cropQuantifier(pr);
			ape.minCount = quantifier >>> 16;
			ape.maxCount = quantifier & 0x0000FFFF;
			if (ape.minCount < 0)
				throw new AnnotationPatternParseException("Invalid quantifier (min less than 0)", quantifierStart);
			if (ape.maxCount < 1)
				throw new AnnotationPatternParseException("Invalid quantifier (max less than 1)", quantifierStart);
			if (ape.maxCount < ape.minCount)
				throw new AnnotationPatternParseException("Invalid quantifier (max less than min)", quantifierStart);
		}
		
		//	finally
		return ape;
	}
	
	private static AnnotationPatternElement cropLiteral(Tokenizer tokenizer, PatternReader pr) throws IOException {
		pr.read(); // consume leading high comma
		pr.skipSpace();
		
		//	read string
		StringBuffer literalBuffer = new StringBuffer();
		boolean escaped = false;
		while (true) {
			int ch = pr.read();
			if (ch == -1)
				throw new AnnotationPatternParseException("Expected closing high comma", pr.readSoFar());
			if (escaped) {
				literalBuffer.append((char) ch);
				escaped = false;
			}
			else if (ch == '\\')
				escaped = true;
			else if (ch == '\'')
				break;
			else literalBuffer.append((char) ch);
		}
		
		//	tokenize and wrap literal
		return new AnnotationPatternElement(new CharTokenSequence(literalBuffer.toString(), tokenizer));
	}
	
	private static AnnotationPatternElement cropPattern(PatternReader pr) throws IOException {
		pr.read(); // consume leading high comma
		pr.skipSpace();
		
		//	read string
		StringBuffer patternBuffer = new StringBuffer();
		boolean escaped = false;
		while (true) {
			int ch = pr.read();
			if (ch == -1)
				throw new AnnotationPatternParseException("Expected closing double quote", pr.readSoFar());
			if (escaped) {
				patternBuffer.append((char) ch);
				escaped = false;
			}
			else if (ch == '\\')
				escaped = true;
			else if (ch == '"')
				break;
			else patternBuffer.append((char) ch);
		}
		
		//	tokenize and wrap literal
		return new AnnotationPatternElement(patternBuffer.toString());
	}
	
	private static AnnotationPatternElement cropAnnotation(PatternReader pr) throws IOException {
		StringBuffer annotBuffer = new StringBuffer();
		
		//	read XML tag
		while (pr.peek() != -1) {
			int ch = pr.read();
			annotBuffer.append((char) ch);
			if (ch == '>')
				break;
		}
		
		//	parse and return XML tag
		String annotString = annotBuffer.toString();
		if (!annotString.endsWith(">"))
			throw new AnnotationPatternParseException("Expected closing angle bracket", (pr.readSoFar() - annotString.length()));
		String annotType = grammar.getType(annotString);
		if (!AnnotationUtils.isValidAnnotationType(annotType))
			throw new AnnotationPatternParseException(("Invalid annotation type: " + annotType), (pr.readSoFar() - annotString.length()));
		return new AnnotationPatternElement(annotType, TreeNodeAttributeSet.getTagAttributes(annotString, grammar));
	}
	
	private static boolean cropSubPattern(Tokenizer tokenizer, PatternReader pr, LinkedList subPatternElements) throws IOException {
		pr.read(); // consume leading bracket
		pr.skipSpace();
		
		//	read sub pattern recursively
		boolean isSequence = true;
		while (true) {
			int ch = pr.peek();
			if (ch == -1)
				throw new AnnotationPatternParseException("Expected closing bracket", pr.readSoFar());
			if (ch == ')') {
				pr.read();
				break;
			}
			else if (ch == '|') {
				pr.read();
				isSequence = false;
			}
			else {
				AnnotationPatternElement ape = cropNext(tokenizer, pr);
				if (ape != null)
					subPatternElements.add(ape);
				pr.skipSpace();
			}
		}
		return isSequence;
	}
	
	private static int cropQuantifier(PatternReader pr) throws IOException {
		pr.skipSpace();
		int ch = pr.read();
		if (ch == '?')
			return 0x00000001;
		else if (ch == '*')
			return 0x0000FFFF;
		else if (ch == '+')
			return 0x0001FFFF;
		
		StringBuffer quantifierBuffer = new StringBuffer();
		while (true) {
			ch = pr.read();
			if (ch == -1)
				throw new AnnotationPatternParseException("Expected closing curly bracket", pr.readSoFar());
			if (ch == '}')
				break;
			else quantifierBuffer.append((char) ch);
		}
		
		String quantifier = quantifierBuffer.toString().trim();
		String[] quantifierParts = quantifier.split("\\s*\\,\\s*", 2);
		if ((quantifierParts.length == 1) || (quantifierParts.length == 2)) try {
			int min = Integer.parseInt(quantifierParts[0]);
			int max = ((quantifierParts.length == 1) ? min : ((quantifierParts[1].length() == 0) ? 0xFFFF : Integer.parseInt(quantifierParts[1])));
			return ((min << 16) + max);
		} catch (NumberFormatException nfe) {}
		throw new AnnotationPatternParseException(("Invalid quantifier: " + quantifier), (pr.readSoFar() - quantifierBuffer.length()));
	}
	
	private static class PatternReader extends FilterReader {
		private int lookaheadChar = -1;
		int readSoFar;
		PatternReader(String pattern) throws IOException {
			super(new StringReader(pattern));
		}
		public int read() throws IOException {
			this.lookaheadChar = -1;
			int r = super.read();
			if (r != -1)
				this.readSoFar++;
			return r;
		}
		public int read(char[] cbuf, int off, int len) throws IOException {
			this.lookaheadChar = -1;
			int r = super.read(cbuf, off, len);
			if (r != -1)
				this.readSoFar += r;
			return r;
		}
		int peek() throws IOException {
			if (this.lookaheadChar != -1)
				return this.lookaheadChar;
			this.in.mark(1);
			this.lookaheadChar = this.in.read();
			this.in.reset();
			return this.lookaheadChar;
		}
		void skipSpace() throws IOException {
			while ((this.peek() < 33) && (this.peek() != -1))
				this.read();
		}
		int readSoFar() {
			return this.readSoFar;
		}
	}
	
	private static class AnnotationPatternParseException extends IOException {
		private int index;
		AnnotationPatternParseException(String message, int index) {
			super(message);
			this.index = index;
		}
		int getIndex() {
			return index;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String pattern;
		pattern = "<stopWord>* <lastName> ','? <firstName>";
		pattern = "(<stopWord>* <lastName>{1,2}){2,} ','? <firstName>";
		pattern = "(<a>{1,3}|(<b><c>{2,4})){2,} ','? <d> (','? <e>)?";
		pattern = "'Mr.'? (<fn>|<in>)+ (<i>* <ln>)+ (','? <a>)?";
		pattern = "'Mr.'? (<fn>|\"[A-Z]\\\\.\")+ (<i>* <ln>)+ (','? <a>)?";
//		pattern = "<bibRefCitation>\n')'?\n<number>";
		AnnotationPattern ap = getPattern(Gamta.INNER_PUNCTUATION_TOKENIZER, pattern);
		System.out.println(ap.toString());
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("Mr. <fn>Tommy</fn> <in>F.</in> <ln><fn>Lee</fn></ln> <i>van</i> <i>den</i> <ln>Jones</ln>, <a>Jr.</a>"));
		MatchTree[] mts = getMatchTrees(doc, ap.toString());
		for (int a = 0; a < mts.length; a++)
			System.out.println(mts[a].toString());
		Annotation[] ans = getMatches(doc, ap.toString());
		for (int a = 0; a < ans.length; a++)
			System.out.println(ans[a].toXML());
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		System.out.println();
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<d>n. <l a=\"y\"><l>ln-a</l> <l a=\"x\">ln-b</l></l>, <i>i-i</i>, <l><l>ln-a</l> ln-b</l> <i>i-i</i>, Jr. n 2345.</d>"));
//		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		Annotation[] ans = getMatches(doc, "<l a=\"(a|b|x)\"> ','? <i> ', Jr'?");
//		for (int a = 0; a < ans.length; a++) {
//			System.out.println(ans[a].toXML());
//		}
	}
}