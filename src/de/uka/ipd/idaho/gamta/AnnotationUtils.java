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
package de.uka.ipd.idaho.gamta;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Static class providing comparison functionality for Annotations. Since the
 * methods do not check which token sequence Annotations refer to (that may not
 * even be possible in the general case), comparing Annotations that refer to
 * different token sequences may result in strange behavior of functionality
 * relying on comparison results.
 * 
 * @author sautter
 */
public class AnnotationUtils {
	
	private static final String NCTypeRegEx = "([a-zA-Z\\_][a-zA-Z0-9\\_\\-\\.]*+)"; // regular expression matching XML NCNames
	private static final String ATypeRegEx = "(" + NCTypeRegEx + "(\\:" + NCTypeRegEx + ")?+)"; // regular expression matching XML QNames
	private static final Pattern ATypePattern = Pattern.compile(ATypeRegEx);
	
	/**
	 * Comparator ordering Annotations in terms of their position. This
	 * comparator does neither consider the Annotation type, nor the attributes.
	 * It merely encapsulates the compare() method provided by theis class. This
	 * object may only be used with Arrays or collections containing objects
	 * implementing the Annotation interface. Using it with any other objects
	 * causes class cast exceptions.
	 */
	public static Comparator ANNOTATION_NESTING_ORDER = new Comparator() {
		public int compare(Object o1, Object o2) {
			return AnnotationUtils.compare(((Annotation) o1), ((Annotation) o2));
		}
	};
	
	/**
	 * Check whether an annotation type String is valid.
	 * @param type the annotation type String to test
	 * @return true if and only if the specified annotation type is valid (a
	 *         QName in the sense of XML)
	 */
	public static boolean isValidAnnotationType(String type) {
		return ATypePattern.matcher(type).matches();
	}
	
	/**
	 * Check if an Annotation overlaps with another Annotation.
	 * @param annotation1 the first Annotation to test
	 * @param annotation2 the second Annotation to test
	 * @return true is and only if annotation1 has at least one Token in common
	 *         with annotation2
	 */
	public static boolean overlaps(Annotation annotation1, Annotation annotation2) {
		return ((annotation1.getStartIndex() < annotation2.getEndIndex()) && (annotation2.getStartIndex() < annotation1.getEndIndex()));
	}
	
	/**
	 * Check if an Annotation's range totally covers another Annotation.
	 * @param annotation1 the first Annotation to test
	 * @param annotation2 the second Annotation to test
	 * @return true if and only if annotation1 spans all Tokens contained in
	 *         annotation2
	 */
	public static boolean contains(Annotation annotation1, Annotation annotation2) {
		return ((annotation1.getStartIndex() <= annotation2.getStartIndex()) && (annotation1.getEndIndex() >= annotation2.getEndIndex()));
	}
	
	/**
	 * Check if an Annotation totally lies in another Annotation's range.
	 * @param annotation1 the first Annotation to test
	 * @param annotation2 the second Annotation to test
	 * @return true is and only if all Tokens contained in annotation1 are also
	 *         contained in annotation2
	 */
	public static boolean liesIn(Annotation annotation1, Annotation annotation2) {
		return ((annotation1.getStartIndex() >= annotation2.getStartIndex()) && (annotation1.getEndIndex() <= annotation2.getEndIndex()));
	}
	
	/**
	 * Check if an Annotation starts with the Token directly following the last
	 * Token of another Annotation in the backing TokenSequence.
	 * @param annotation1 the first Annotation to test
	 * @param annotation2 the second Annotation to test
	 * @return true is and only if annotation1 starts with the Token directly
	 *         following the last Token of annotation2 in the backing
	 *         TokenSequence
	 */
	public static boolean follows(Annotation annotation1, Annotation annotation2) {
		return (annotation1.getStartIndex() == annotation2.getEndIndex());
	}
	
	/**
	 * Check if an Annotation ends with the Token directly preceeding the first
	 * Token of another Annotation in the backing TokenSequence.
	 * @param annotation1 the first Annotation to test
	 * @param annotation2 the second Annotation to test
	 * @return true is and only if annotation1 ends with the Token directly
	 *         preceeding the first Token of annotation2 in the backing
	 *         TokenSequence
	 */
	public static boolean preceeds(Annotation annotation1, Annotation annotation2) {
		return (annotation1.getEndIndex() == annotation2.getStartIndex());
	}
	
	/**
	 * Check if an Annotation neighbours another Annotation, i.e. preceeds or
	 * follows it.Note: This method is provided for convenience, the returned
	 * boolean is equal to<br>
	 * <code>(preceeds(annotation1, annotation2) || follows(annotation1, annotation2))</code>
	 * @param annotation1 the first Annotation to test
	 * @param annotation2 the second Annotation to test
	 * @return true is and only if annotation1 neighbours annotation2, i.e.
	 *         preceeds or follows it
	 */
	public static boolean neighbors(Annotation annotation1, Annotation annotation2) {
		return (preceeds(annotation1, annotation2) || follows(annotation1, annotation2));
	}
	
	/**
	 * Compare two Annotations in terms of their position (note that this method
	 * does neither consider the Annotation type, nor the attributes).
	 * @param annotation1 the first Annotation
	 * @param annotation2 the second Annotation
	 * @return a value lesst than, equal to, or gteater than zero, depending on
	 *         whether annotation1 is less than, equal to, or greater than
	 *         annotation2 in terms of position
	 */
	public static int compare(Annotation annotation1, Annotation annotation2) {
		int s1 = annotation1.getStartIndex();
		int s2 = annotation2.getStartIndex();
		if (s1 == s2) return (annotation2.size() - annotation1.size());
		else return (s1 - s2);
	}
	
	/**
	 * Check if two Annotations are equal, including their type being the same.
	 * @param annotation1 the first Annotation
	 * @param annotation2 the second Annotation
	 * @return true if and only if annotation1 and annotation2 have the same
	 *         start index, size, and type
	 */
	public static boolean equals(Annotation annotation1, Annotation annotation2) {
		return equals(annotation1, annotation2, true);
	}
	
	/**
	 * Check if two Annotations are equal.
	 * @param annotation1 the first Annotation
	 * @param annotation2 the second Annotation
	 * @param compareType compare the types of the Annotations?
	 * @return true if and only if annotation1 and annotation2 have the same
	 *         start index and size, and either compareType is false, or
	 *         annotation1 and annotation2 have the same type
	 */
	public static boolean equals(Annotation annotation1, Annotation annotation2, boolean compareType) {
		if (annotation1.getStartIndex() != annotation2.getStartIndex()) return false;
		if (annotation1.size() != annotation2.size()) return false;
		return (!compareType || annotation1.getType().equals(annotation2.getType()));
	}
	
	/**
	 * Sort an array of Annotations according to some nesting order.
	 * @param annotations the array of annotations to sort
	 * @param nestingOrder the nesting order to use, as a space-separated string
	 *            concatenation of annotation types, outmost type first
	 */
	public static void sort(Annotation[] annotations, String nestingOrder) {
		Arrays.sort(annotations, getComparator(nestingOrder));
	}
	
	/**
	 * Produce a comparator from a specific annotation type nesting order.
	 * @param nestingOrder the nesting order to use, as a space-separated string
	 *            concatenation of annotation types, outmost type first
	 * @return a comparator for the specified type nesting order (will assume
	 *         that arguments to compare() method are Strings)
	 */
	public static Comparator getTypeComparator(final String nestingOrder) {
		
		//	do cache lookup
		Comparator comparator = ((Comparator) typeComparatorCache.get(nestingOrder));
		if (comparator != null) return comparator;
		
		//	parse custom nesting order
		StringVector noParser = new StringVector();
		noParser.parseAndAddElements(nestingOrder, " ");
		noParser.removeAll("");
		
		//	parse default nesting order
		StringVector dnoParser = new StringVector();
		dnoParser.parseAndAddElements(DocumentRoot.DEFAULT_ANNOTATION_NESTING_ORDER, " ");
		dnoParser.removeAll("");
		
		//	fold nesting orders
		StringVector noBuilder = new StringVector();
		for (int t = 0; t < dnoParser.size(); t++) {
			String type = dnoParser.get(t);
			if (noParser.contains(type)) {
				while (!noParser.isEmpty() && !noParser.firstElement().equals(type))
					noBuilder.addElement(noParser.remove(0));
				noParser.removeAll(type);
			}
			noBuilder.addElement(type);
		}
		
		//	index nesting order
		final HashMap noRanking = new HashMap();
		for (int t = 0; t < noBuilder.size(); t++) {
			String type = noBuilder.get(t).trim();
			if ((type.length() != 0) && !DocumentRoot.DOCUMENT_TYPE.equals(type))
				noRanking.put(type, new Integer(t));
		}
		
		//	create comparator
		comparator = new Comparator() {
			public int compare(Object type1, Object type2) {
				
				//	keep documents outside every other Annotation, regardless of configuration
				if (DocumentRoot.DOCUMENT_TYPE.equals(type1)) return (DocumentRoot.DOCUMENT_TYPE.equals(type2) ? 0 : -1);
				if (DocumentRoot.DOCUMENT_TYPE.equals(type2)) return 1;
				
				//	get ordering numbers
				Integer i1 = ((Integer) noRanking.get(type1));
				Integer i2 = ((Integer) noRanking.get(type2));
				
				//	both specified, compare
				if ((i1 != null) && (i2 != null)) return i1.compareTo(i2);
				
				//	second type not specified, assume it to be some detail
				else if (i1 != null) return -1;
				
				//	first type not specified, assume it to be some detail
				else if (i2 != null) return 1;
				
				//	none of the types specified
				else return 0;
			}
			
			public String toString() {
				return nestingOrder;
			}
			
			public boolean equals(Object obj) {
				return ((obj != null) && nestingOrder.equals(obj.toString()));
			}
			
			public int hashCode() {
				return nestingOrder.hashCode();
			}
		};
		
		//	cache comparator
		typeComparatorCache.put(nestingOrder, comparator);
		
		//	return comparator
		return comparator;
	}
	private static HashMap typeComparatorCache = new HashMap();
	
	/**
	 * Produce a comparator from a specific annotation nesting order.
	 * @param nestingOrder the nesting order to use, as a space-separated string
	 *            concatenation of annotation types, outmost type first
	 * @return a comparator for the specified type nesting order (will assume
	 *         that arguments to compare() method are Annotations)
	 */
	public static Comparator getComparator(final String nestingOrder) {
		
		//	do cache lookup
		Comparator comparator = ((Comparator) comparatorCache.get(nestingOrder));
		if (comparator != null) return comparator;
		
		//	get type comparator
		final Comparator typeComparator = getTypeComparator(nestingOrder);
		
		//	create comparator
		comparator = new Comparator() {
			public int compare(Object annotation1, Object annotation2) {
				int c = AnnotationUtils.compare(((Annotation) annotation1), ((Annotation) annotation2));
				return ((c == 0) ? typeComparator.compare(((Annotation) annotation1).getType(), ((Annotation) annotation2).getType()) : c);
			}
			public String toString() {
				return nestingOrder;
			}
			public boolean equals(Object obj) {
				return ((obj != null) && nestingOrder.equals(obj.toString()));
			}
			public int hashCode() {
				return nestingOrder.hashCode();
			}
		};
		
		//	cache comparator
		comparatorCache.put(nestingOrder, comparator);
		
		//	return comparator
		return comparator;
	}
	private static HashMap comparatorCache = new HashMap();
	
	/**
	 * Produce an XML start tag for an Annotation. This method includes all
	 * attributes, escaping their values. For more fine-frained control, use the
	 * three argument verison of this method.
	 * @param data the Annotation to produce a start tag for
	 * @return an XML start tag for the specified Annotation
	 */
	public static String produceStartTag(Annotation data) {
		return produceStartTag(data, null, true);
	}
	
	/**
	 * Produce an XML start tag for an Annotation. The specified attribute
	 * filter set (if any) is checked for the individual attribute names solely
	 * by means of its contains() method; this facilitates filtering out
	 * specific attributes by excluding them explicitly, returning true for all
	 * other types.
	 * @param data the Annotation to produce a start tag for
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tag (specifying null will include all
	 *            attributes)
	 * @param escapeValues check and if necessary escape attribute values
	 *            (transform '&amp;' to '&amp;amp;', '&quot;' to '&amp;quot;',
	 *            etc.)
	 * @return an XML start tag for the specified Annotation
	 */
	public static String produceStartTag(Annotation data, Set attributeFilter, boolean escapeValues) {
		StringBuffer tagAssembler = new StringBuffer("<");
		tagAssembler.append(data.getType());
		String[] ans = data.getAttributeNames();
		for (int a = 0; a < ans.length; a++) {
			if ((attributeFilter == null) || attributeFilter.contains(ans[a])) {
				Object value = data.getAttribute(ans[a]);
				if (value != null) {
					String valueString = value.toString();
					if (escapeValues) {
						valueString = escapeForXml(valueString, true);
//						valueString = valueString.replaceAll("\\&", "\\&amp\\;");
//						valueString = valueString.replaceAll("\\\"", "\\&quot\\;");
//						valueString = valueString.replaceAll("\\<", "\\&lt\\;");
//						valueString = valueString.replaceAll("\\>", "\\&gt\\;");
					}
					tagAssembler.append(" " + ans[a] + "=\"" + valueString + "\"");
				}
			}
		}
		tagAssembler.append(">");
		return tagAssembler.toString();
	}
	
	/**
	 * Produce an XML end tag for an Annotation.
	 * @param data the Annotation to produce an end tag for
	 * @return an XML end tag for the specified Annotation
	 */
	public static String produceEndTag(Annotation data) {
		return ("</" + data.getType() + ">");
	}
	
	/**
	 * Obtain an XML representation of an Annotation.
	 * @param data the Annotation to produce an XML representation for
	 * @return an XML representation of the specified Annotation
	 */
	public static String toXML(Annotation data) {
		return (produceStartTag(data) + data.getValue() + produceEndTag(data));
	}
	
	/**
	 * Obtain an XML representation of an Annotation.
	 * @param data the Annotation to produce an XML representation for
	 * @return an XML representation of the specified Annotation
	 */
	public static String toXML(QueriableAnnotation data) {
		return toXML(data, null);
	}
	
	/**
	 * Obtain an XML representation of an Annotation.
	 * @param data the Annotation to produce an XML representation for
	 * @param annotationTypes a set containing the annotation types to restrict
	 *            the output to (specifying null will write all annotations)
	 * @return an XML representation of the specified Annotation
	 */
	public static String toXML(QueriableAnnotation data, Set annotationTypes) {
		try {
			StringWriter sw = new StringWriter();
			writeXML(data, sw, annotationTypes);
			return sw.toString();
		}
		catch (IOException e) {
			return "";
		}
	}
	
	/**
	 * Write the content of a Queriable Annotation marked up with XML to the
	 * specified Writer. This method writes all annotations and includes all
	 * attributes in the start tags, escaping their values. For more
	 * fine-grained control, use the five argument version of this method.
	 * @param data the Annotation to write
	 * @param output the Writer to write to
	 * @return true if and only if the output was written successfully
	 */
	public static boolean writeXML(QueriableAnnotation data, Writer output) throws IOException {
		return writeXML(data, output, null);
	}
	
	/**
	 * Write the content of a Queriable Annotation marked up with XML to the
	 * specified Writer. The specified annotation type set (if any) is checked
	 * for the individual annotation types solely by means of its contains()
	 * method; this facilitates filtering out specific annotation types by
	 * excluding them explicitly, returning true for all other types. This
	 * method includes all attributes in the start tags, escaping their values.
	 * For more fine-grained control, use the five argument version of this
	 * method.
	 * @param data the Annotation to write
	 * @param output the Writer to write to
	 * @param annotationTypes a set containing the annotation types to restrict
	 *            the output to (specifying null will write all annotations)
	 * @return true if and only if the output was written successfully
	 */
	public static boolean writeXML(QueriableAnnotation data, Writer output, Set annotationTypes) throws IOException {
		return writeXML(data, output, annotationTypes, null, true);
	}
	
	/**
	 * Write the content of a Queriable Annotation marked up with XML to the
	 * specified Writer. The specified annotation type set (if any) is checked
	 * for the individual annotation types solely by means of its contains()
	 * method; this facilitates filtering out specific annotation types by
	 * excluding them explicitly, returning true for all other types. The same
	 * applies to the attribute filter set.
	 * @param data the Annotation to write
	 * @param output the Writer to write to
	 * @param annotationTypes a set containing the annotation types to restrict
	 *            the output to (specifying null will write all annotations)
	 * @param attributeFilter a set containing the names of the attributes to
	 *            include in the tags (specifying null will include all
	 *            attributes)
	 * @param escape check and if necessary escape text data and attribute
	 *            values (transform '&amp;' to '&amp;amp;', '&quot;' to
	 *            '&amp;quot;', etc.)
	 * @return true if and only if the output was written successfully
	 */
	public static boolean writeXML(QueriableAnnotation data, Writer output, Set annotationTypes, Set attributeFilter, boolean escape) throws IOException {
		BufferedWriter buf = ((output instanceof BufferedWriter) ? ((BufferedWriter) output) : new BufferedWriter(output));
		
		//	get annotations
		Annotation[] nestedAnnotations = data.getAnnotations();
		
		//	filter annotations
		if (annotationTypes != null) {
			ArrayList annotationList = new ArrayList();
			for (int a = 0; a < nestedAnnotations.length; a++)
				if (annotationTypes.contains(nestedAnnotations[a].getType()))
					annotationList.add(nestedAnnotations[a]);
			nestedAnnotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
		}
		
		//	make sure there is a root element
		if ((nestedAnnotations.length == 0) || (nestedAnnotations[0].size() < data.size())) {
			Annotation[] newNestedAnnotations = new Annotation[nestedAnnotations.length + 1];
			newNestedAnnotations[0] = data;
			System.arraycopy(nestedAnnotations, 0, newNestedAnnotations, 1, nestedAnnotations.length);
			nestedAnnotations = newNestedAnnotations;
		}
		
		Stack stack = new Stack();
		int annotationPointer = 0;
		
		Token token = null;
		Token lastToken;
		
		boolean lastWasTag = false;
		boolean lastWasLineBreak = true;
		
		HashSet lineBroken = new HashSet();
		
		for (int t = 0; t < data.size(); t++) {
			
			//	switch to next Token
			lastToken = token;
			token = data.tokenAt(t);
			
			//	write end tags for Annotations ending before current Token
			while ((stack.size() > 0) && (((Annotation) stack.peek()).getEndIndex() <= t)) {
				Annotation annotation = ((Annotation) stack.pop());
				
				//	line break only if nested Annotations
				if (!lastWasLineBreak && lineBroken.contains(annotation.getAnnotationID())) 
					buf.newLine();
				
				//	write tag and line break
				buf.write(produceEndTag(annotation));
				lastWasTag = true;
				buf.newLine();
				lastWasLineBreak = true;
			}
			
			//	add line break if required
			if (!lastWasLineBreak && (lastToken != null) && lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) {
				buf.newLine();
				lastWasLineBreak = true;
			}
			
			//	skip space character before unspaced punctuation (e.g. ','), after line breaks and tags, and if there is no whitespace in the token sequence
			if (!lastWasTag && !lastWasLineBreak && 
				(lastToken != null) && !lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) && 
				Gamta.insertSpace(lastToken, token) && (t != 0) && (data.getWhitespaceAfter(t-1).length() != 0)
				) buf.write(" ");
			
			//	write start tags for Annotations beginning at current Token
			while ((annotationPointer < nestedAnnotations.length) && (nestedAnnotations[annotationPointer].getStartIndex() == t)) {
				Annotation annotation = nestedAnnotations[annotationPointer];
				stack.push(annotation);
				annotationPointer++;
				
				//	line break
				if (!lastWasLineBreak) buf.newLine();
				
				//	add start tag
				buf.write(produceStartTag(annotation, attributeFilter, escape));
				lastWasTag = true;
				lastWasLineBreak = false;
				
				//	line break only if nested Annotations
				if ((annotationPointer < nestedAnnotations.length) && AnnotationUtils.contains(annotation, nestedAnnotations[annotationPointer])) {
					buf.newLine();
					lastWasLineBreak = true;
					lineBroken.add(annotation.getAnnotationID());
				}
			}
			
			//	append current Token
			if (escape)
				buf.write(escapeForXml(token.getValue()));
			else buf.write(token.getValue());
			
			//	set status
			lastWasTag = false;
			lastWasLineBreak = false;
		}
		
		//	write end tags for Annotations not closed so far
		while (stack.size() > 0) {
			Annotation annotation = ((Annotation) stack.pop());
			
			if (!lastWasLineBreak && lineBroken.contains(annotation.getAnnotationID()))
				buf.newLine();
			
			buf.write(produceEndTag(annotation));
			lastWasLineBreak = false;
		}
		if (buf != output)
			buf.flush();
		return true;
	}
	
	/**
	 * Escape a string to be well-formed XML - in particular, escape &amp;,
	 * &lt;, &gt;, &quot;.
	 * @param string the string to escape
	 * @return the escaped string
	 */
	public static String escapeForXml(String string) {
		return escapeForXml(string, false);
	}
	
	/**
	 * Escape a string to be well-formed XML - in particular, escape &amp;,
	 * &lt;, &gt;, &quot;, and optionally control characters. The latter is
	 * helpful for storing attribute values that have line breaks, for instance.
	 * @param string the string to escape
	 * @param escapeControl escape control characters?
	 * @return the escaped string
	 */
	public static String escapeForXml(String string, boolean escapeControl) {
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
			else if (((ch < 32) || (ch == 127)) && escapeControl)
				escapedString.append("&x" + Integer.toString(((int) ch), 16).toUpperCase() + ";");
			else escapedString.append(ch);
		}
		return escapedString.toString();
	}
	
	/**
	 * Un-escape a string from its XML encoding - in particular, un-escape
	 * &amp;, &lt;, &gt;, &quot;, and hex encoded characters.
	 * @param escapedString the string to un-escape
	 * @return the un-escaped string
	 */
	public static String unescapeFromXml(String escapedString) {
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
				else if (escapedString.startsWith("x", (c+1))) {
					int sci = escapedString.indexOf(';', (c+1));
					if ((sci != -1) && (sci <= (c+4))) try {
						ch = ((char) Integer.parseInt(escapedString.substring((c+2), sci), 16));
						c = sci;
					} catch (Exception e) {}
					string.append(ch);
					c++;
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
	
	/**
	 * Test if the annotations of a document or a part of it represent
	 * wellformed XML, i.e. if the annotations are properly nested.
	 * @param data the document to test
	 * @return true if and only if the annotations of the specified document are
	 *         properly nested
	 */
	public static boolean isWellFormedNesting(QueriableAnnotation data) {
		return isWellFormedNesting(data, null);
	}
	
	/**
	 * Test if the annotations of a document or a part of it represent
	 * wellformed XML, i.e. if the annotations are properly nested. The
	 * specified annotation type set (if any) is checked for the individual
	 * annotation types solely by means of its contains() method; this
	 * facilitates filtering out specific annotation types by excluding them
	 * explicitly, returning true for all other types.
	 * @param data the document to test
	 * @param typeFilter a set containing the annotation types to include in the
	 *            test (specifying null considers all annotations, regardless of
	 *            their type)
	 * @return true if and only if the annotations of the specified document are
	 *         properly nested
	 */
	public static boolean isWellFormedNesting(QueriableAnnotation data, Set typeFilter) {
		
		//	get annotations
		Annotation[] nestedAnnotations = data.getAnnotations();
		
		//	filter annotations
		if (typeFilter != null) {
			ArrayList annotationList = new ArrayList();
			for (int a = 0; a < nestedAnnotations.length; a++)
				if (typeFilter.contains(nestedAnnotations[a].getType()))
					annotationList.add(nestedAnnotations[a]);
			nestedAnnotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
		}
		
		//	make sure there is a root element
		if ((nestedAnnotations.length == 0) || (nestedAnnotations[0].size() < data.size())) {
			Annotation[] newNestedAnnotations = new Annotation[nestedAnnotations.length + 1];
			newNestedAnnotations[0] = data;
			System.arraycopy(nestedAnnotations, 0, newNestedAnnotations, 1, nestedAnnotations.length);
			nestedAnnotations = newNestedAnnotations;
		}
		
		Stack stack = new Stack();
		int annotationPointer = 0;
		
		for (int t = 0; t < data.size(); t++) {
			
			//	pop and test annotations ending before current Token
			while ((stack.size() > 0) && (((Annotation) stack.peek()).getEndIndex() <= t)) {
				Annotation annotation = ((Annotation) stack.pop());
				
				//	test if annotation ends at current index or before
				if (annotation.getEndIndex() < t) {
					stack.clear();
					return false;
				}
			}
			
			//	push annotations beginning at current Token
			while ((annotationPointer < nestedAnnotations.length) && (nestedAnnotations[annotationPointer].getStartIndex() == t)) {
				Annotation annotation = nestedAnnotations[annotationPointer];
				stack.push(annotation);
				annotationPointer++;
			}
		}
		
		//	pop and test remaining annotations
		while (stack.size() > 0) {
			Annotation annotation = ((Annotation) stack.pop());
			
			//	test if annotation ends at current index or before
			if (annotation.getEndIndex() < data.size()) {
				stack.clear();
				return false;
			}
		}
		
		//	no errors found, report success
		return true;
	}
}
