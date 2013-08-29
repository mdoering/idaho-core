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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
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
 * The pattern syntax is XML based:<br>
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
	
	//	TODO facilitate parts in brackets
	
	//	TODO facilitate quantifiers
	
	//	TODO facilitate starting with literals
	
	//	TODO create manager plugin in goldengate-plugins
	
	private static Grammar grammar = new StandardGrammar();
	private static Parser parser = new Parser(grammar);
	
	private static class AnnotationPattern {
		AnnotationPatternElement[] elements;
		AnnotationPattern(AnnotationPatternElement[] elements) {
			this.elements = elements;
		}
	}
	
	private static class AnnotationPatternElement {
		String annotationType = null;
		TreeNodeAttributeSet annotationAttributes = null;
		TokenSequence tokens = null;
		boolean isOptional = false;
		AnnotationPatternElement(TokenSequence tokens) {
			this.tokens = tokens;
		}
		AnnotationPatternElement(String annotationType, TreeNodeAttributeSet annotationAttributes) {
			this.annotationType = annotationType;
			this.annotationAttributes = ((annotationAttributes.size() > 0) ? annotationAttributes : null);
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
	 * Attempt to match an annotation pattern against a token sequence with
	 * existing annotations.
	 * @param tokens the token sequence to match against
	 * @param annotationIndex an index holding the annotations belonging to the
	 *            token sequence
	 * @param pattern the pattern to match
	 * @return an array holding the matches of the pattern
	 */
	public static Annotation[] getMatches(TokenSequence tokens, AnnotationIndex annotationIndex, String pattern) {
		AnnotationPattern ap = getPattern(tokens.getTokenizer(), pattern);
		Annotation[] match = new Annotation[ap.elements.length];
		Arrays.fill(match, null);
		ArrayList matches = new ArrayList();
		for (int s = 0; s < tokens.size(); s++)
			step(tokens, ap, 0, match, s, annotationIndex, matches);
		return ((Annotation[]) matches.toArray(new Annotation[matches.size()]));
	}
	
	private static void step(TokenSequence tokens, AnnotationPattern pattern, int elementIndex, Annotation[] match, int matchFromIndex, AnnotationIndex annotationIndex, ArrayList matches) {
		
		//	end of pattern reached, we have a match
		if (pattern.elements.length == elementIndex) {
			int matchStart = -1;
			for (int m = 0; m < match.length; m++)
				if (match[m] != null) {
					matchStart = match[m].getStartIndex();
					break;
				}
			if ((matchStart != -1) && (matchStart < matchFromIndex)) {
				Annotation m = Gamta.newAnnotation(tokens, null, matchStart, (matchFromIndex-matchStart));
				matches.add(m);
				return;
			}
		}
		
		//	literal
		if (pattern.elements[elementIndex].tokens != null) {
			if (TokenSequenceUtils.indexOf(tokens, pattern.elements[elementIndex].tokens, matchFromIndex) == matchFromIndex) {
				match[elementIndex] = null;
				step(tokens, pattern, (elementIndex+1), match, (matchFromIndex + pattern.elements[elementIndex].tokens.size()), annotationIndex, matches);
			}
			if (pattern.elements[elementIndex].isOptional) {
				match[elementIndex] = null;
				step(tokens, pattern, (elementIndex+1), match, matchFromIndex, annotationIndex, matches);
			}
			return;
		}
		
		//	pattern
		if (pattern.elements[elementIndex].annotationType != null) {
			Annotation[] annots = annotationIndex.getAnnotations(pattern.elements[elementIndex].annotationType, matchFromIndex);
			for (int a = 0; a < annots.length; a++) {
				boolean attribMatch = true;
				if (pattern.elements[elementIndex].annotationAttributes != null) {
					System.out.println("Testing attributes of annotation " + AnnotationUtils.produceStartTag(annots[a]));
					String[] attribNames = pattern.elements[elementIndex].annotationAttributes.getAttributeNames();
					for (int n = 0; n < attribNames.length; n++) {
						System.out.println(" - " + attribNames[n]);
						Object attribValueObj = annots[a].getAttribute(attribNames[n]);
						if (attribValueObj == null) {
							attribMatch = false;
							System.out.println(" --> absent");
							break;
						}
						String attribTest = pattern.elements[elementIndex].annotationAttributes.getAttribute(attribNames[n]);
						System.out.println(" - test is " + attribTest);
						if ("*".equals(attribTest)) {
							System.out.println(" --> wildcard match");
							continue;
						}
						String attribValue = attribValueObj.toString();
						if (attribTest.startsWith("(") && attribTest.endsWith(")")) {
							if (!attribValue.matches(attribTest)) {
								attribMatch = false;
								System.out.println(" --> pattern match failed");
								break;
							}
							System.out.println(" --> pattern match");
						}
						else if (!attribTest.equalsIgnoreCase(attribValue)) {
							attribMatch = false;
							System.out.println(" --> value match failed");
							break;
						}
						else System.out.println(" --> value match");
					}
				}
				if (attribMatch) {
					match[elementIndex] = annots[a];
					step(tokens, pattern, (elementIndex+1), match, (matchFromIndex + annots[a].size()), annotationIndex, matches);
				}
			}
			if (pattern.elements[elementIndex].isOptional) {
				match[elementIndex] = null;
				step(tokens, pattern, (elementIndex+1), match, matchFromIndex, annotationIndex, matches);
			}
			return;
		}
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
		final LinkedList elements = new LinkedList();
		StringReader pr = new StringReader(pattern);
		parser.stream(pr, new TokenReceiver() {
			private int index = 0;
			public void close() throws IOException {}
			public void storeToken(String token, int treeDepth) throws IOException {
				if (grammar.isTag(token)) {
					elements.add(new AnnotationPatternElement(grammar.getType(token), TreeNodeAttributeSet.getTagAttributes(token, grammar)));
					this.index += token.length();
					return;
				}
				while ((token = this.trimStart(token)).length() != 0) {
					
					//	make last part optional
					if (token.startsWith("?")) {
						if (elements.isEmpty())
							throw new AnnotationPatternParseException("Optional element marker before first element.", this.index);
						else ((AnnotationPatternElement) elements.getLast()).isOptional = true;
						token = token.substring(1);
						this.index++;
						continue;
					}
					
					//	get quoted part
					if (token.startsWith("'")) {
						token = token.substring(1).trim();
						this.index++;
						token = this.trimStart(token);
						int quoteEnd = token.indexOf('\'');
						if (quoteEnd < 1)
							throw new AnnotationPatternParseException(((quoteEnd == -1) ? "Missing closing quote." : "Empty literal."), this.index);
						elements.add(new AnnotationPatternElement(Gamta.newTokenSequence(token.substring(0, quoteEnd).trim(), tokenizer)));
						token = token.substring(quoteEnd+1);
						this.index += (quoteEnd+1);
						continue;
					}
					
					//	get (unquoted) rest
					elements.add(new AnnotationPatternElement(Gamta.newTokenSequence(token.trim(), tokenizer)));
					this.index += token.length();
					break;
				}
			}
			private String trimStart(String token) {
				while ((token.length() != 0) && (token.charAt(0) < 33)) {
					token = token.substring(1);
					this.index++;
				}
				return token;
			}
		});
		
//		System.out.println("Got annotation pattern from '" + pattern + "':");
//		for (int e = 0; e < elements.size(); e++) {
//			AnnotationPatternElement ape = ((AnnotationPatternElement) elements.get(e));
//			if (ape.annotationType != null) {
//				System.out.println(" - '" + ape.annotationType + "' annotation" + (ape.isOptional ? " (optional)": ""));
//				if (ape.annotationAttributes != null) {
//					String[] attribNames = ape.annotationAttributes.getAttributeNames();
//					for (int n = 0; n < attribNames.length; n++) {
//						String attribTest = ape.annotationAttributes.getAttribute(attribNames[n]);
//						System.out.println("   - '" + attribNames[n] + "' attribute with value " + attribTest);
//					}
//				}
//			}
//			if (ape.tokens != null)
//				System.out.println(" - '" + TokenSequenceUtils.concatTokens(ape.tokens) + "' tokens" + (ape.isOptional ? " (optional)": ""));
//		}
		
		return new AnnotationPattern(((AnnotationPatternElement[]) elements.toArray(new AnnotationPatternElement[elements.size()])));
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
	
//	AnnotationPattern(String[] elementDefs, Tokenizer tokenizer) {
//		ArrayList elements = new ArrayList();
//		for (int e = 0; e < elementDefs.length; e++) {
//			String elementDef = elementDefs[e];
//			boolean isOptional = false;
//			if (elementDef.endsWith("?")) {
//				isOptional = true;
//				elementDef = elementDef.substring(0, (elementDef.length()-1));
//			}
//			if (grammar.isTag(elementDef))
//				elements.add(new AnnotationPatternElement(grammar.getType(elementDef), TreeNodeAttributeSet.getTagAttributes(elementDef, grammar), isOptional));
//			else elements.add(new AnnotationPatternElement(Gamta.newTokenSequence(elementDef, tokenizer), isOptional));
//		}
//		this.elements = ((AnnotationPatternElement[]) elements.toArray(new AnnotationPatternElement[elements.size()]));
//	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		MutableAnnotation doc = SgmlDocumentReader.readDocument(new StringReader("<d>n. <l a=\"y\"><l>ln-a</l> <l a=\"x\">ln-b</l></l>, <i>i-i</i>, <l><l>ln-a</l> ln-b</l> <i>i-i</i>, Jr. n 2345.</d>"));
		AnnotationUtils.writeXML(doc, new OutputStreamWriter(System.out));
//		Annotation[] ans = getMatches(doc, "<l> ','? <i> ', Jr'?");
		Annotation[] ans = getMatches(doc, "<l a=\"(a|b|x)\"> ','? <i> ', Jr'?");
		for (int a = 0; a < ans.length; a++) {
			System.out.println(ans[a].toXML());
		}
	}
}