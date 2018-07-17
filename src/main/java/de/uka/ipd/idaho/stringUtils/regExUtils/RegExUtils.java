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
package de.uka.ipd.idaho.stringUtils.regExUtils;


import java.util.HashSet;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Some utilities for regular expressions. <BR>
 * The explodeRegEx() and normalizeRegEx() methods are intended to make complex
 * regular expressions easier to handle. <BR>
 * The produceOrGroup() and produceEnumerationGroup() methods are intended to
 * create capturing groups on the fly without having to deal with problems like
 * escaping.
 * 
 * @author sautter
 */
public class RegExUtils {
	
	/**	constant regular expression matching any sequence of two or more letters */
	public static final String WORD = "([a-zA-Z][a-z]{2,})";
	
	/**	constant regular expression matching any sequence of an upper case letter followed by two or more lower case letters */
	public static final String CAPITALIZED_WORD = "([A-Z][a-z]{2,})";
	
	/**	constant regular expression matching any sequence of two or more upper case letters */
	public static final String UPPER_CASE_WORD = "([A-Z]{2,})";
	
	/**	constant regular expression matching any sequence of two or more lower case letters */
	public static final String LOWER_CASE_WORD = "([a-z]{2,})";
	
	/**	constant regular expression matching any sequence of a letter followed by an optional lower case letter and a dot */
	public static final String ABBREVIATION = "([a-zA-Z][a-z]?\\.)";
	
	/**	constant regular expression matching any sequence of a letter followed by up to three lower case letters and a dot */
	public static final String LONG_ABBREVIATION = "([a-zA-Z][a-z]{0,3}\\.)";
	
	/**	constant regular expression matching any sequence of an upper case letter followed by an optional lower case letter and a dot */
	public static final String CAPITALIZED_ABBREVIATION = "([A-Z][a-z]?\\.)";
	
	/**	constant regular expression matching any sequence of an upper case letter followed by up to three lower case letters and a dot */
	public static final String CAPITALIZED_LONG_ABBREVIATION = "([A-Z][a-z]{0,3}\\.)";
	
	/**	constant regular expression matching any sequence of one or two lower case letters followed by a dot */
	public static final String LOWER_CASE_ABBREVIATION = "([a-z][a-z]?\\.)";
	
	/**	constant regular expression matching any sequence of one up to four lower case letters followed by a dot */
	public static final String LOWER_CASE_LONG_ABBREVIATION = "([a-z]{1,4}\\.)";
	
	/**	constant regular expression matching a FIRST_LETTER_UP_WORD or an UPPER_CASE_ABBREVIATION */
	public static final String FIRST_NAME = 
			"(" +
				CAPITALIZED_WORD + 
				"|" +
				CAPITALIZED_ABBREVIATION + 
			")";
	
	/**
	 * constant regular expression matching any sequence of an optional prefix
	 * out of {Mac, Mc, O', D'} followed by a CAPITALIZED_WORD
	 */
	public static final String LAST_NAME = 
			"(" +
				"(Mc|Mac|O'|D')?" +
				CAPITALIZED_WORD + 
			")";
	
	/**
	 * constant regular expression matching any sequence of an optional prefix
	 * or title out of {Sir, Prof, Dr, Mr, Mrs, Ms} followed by one or more
	 * FIRST_NAMEs, a LAST_NAME and an optional suffix out of {Jun, Sen}, all
	 * parts separated by singular whitespace characters
	 */
	public static final String PERSON_NAME = 
			"((" +
				"((Prof\\.|Dr\\.|Mr\\.|Mrs\\.|Ms\\.|Sir|Prof|Dr|Mr|Mrs|Ms)\\s)*" + 
				"(" + FIRST_NAME + "\\s)+" + 
				LAST_NAME +
				"((\\,)?\\s(Jun\\.|Sen\\.|Jun|Sen))?" +
			")|(" +
				"((Prof\\.|Dr\\.|Mr\\.|Mrs\\.|Ms\\.|Sir|Prof|Dr|Mr|Mrs|Ms)\\s)+" + 
				"(" + FIRST_NAME + "\\s)*" + 
				LAST_NAME +
				"((\\,)?\\s(Jun\\.|Sen\\.|Jun|Sen))?" +
			"))";
	
	/**
	 * escape all characters in a String that have to be escaped in a regular
	 * expression
	 * @param string the String the characters of which are to be escaped
	 * @return the specified String, with all characters escaped that have to be
	 *         escaped in a regular expression
	 */
	public static String escapeForRegEx(String string) {
		if ((string == null) || (string.length() == 0)) return string;
		StringBuffer assembler = new StringBuffer();
		for (int s = 0; s < string.length(); s++) {
			char c = string.charAt(s);
			if (StringUtils.LETTERS.indexOf(c) != -1) assembler.append(c);
			else if (StringUtils.DIGITS.indexOf(c) != -1) assembler.append(c);
			else if (c < 33) assembler.append("\\s");
			else assembler.append("\\" + c);
		}
		return assembler.toString();
	}

	/**
	 * produce a capturing group matching exacly one of the specified words at a
	 * time
	 * @param words the words to combine disjunctively
	 * @return a capturing group matching exacly one of the specified words at a
	 *         time Note: If a specified word contains characters that have to
	 *         be escaped in a regular expression, this is done automatically
	 */
	public static String produceOrGroup(StringVector words) {
		return produceOrGroup(words, true);
	}
	
	/**
	 * produce a capturing group matching exacly one of the specified words at a
	 * time
	 * @param parts the parts to combine disjunctively
	 * @param escape escape the specified Strings or not?
	 * @return a capturing group matching exacly one of the specified words at a
	 *         time
	 */
	public static String produceOrGroup(StringVector parts, boolean escape) {
		if ((parts == null) || (parts.size() == 0)) return "";
		StringBuffer assembler = new StringBuffer("(");
		for (int p = 0; p < parts.size(); p++)
			assembler.append(((p == 0) ? "" : "|") + (escape ? escapeForRegEx(parts.get(p)) : parts.get(p)));
		assembler.append(")");
		return assembler.toString();
	}
	
	/**
	 * produce a capturing group matching exacly one of the specified words at a
	 * time
	 * @param words the words to combine disjunctively
	 * @return a capturing group matching exacly one of the specified words at a
	 *         time Note: If a specified word contains characters that have to
	 *         be escaped in a regular expression, this is done automatically
	 */
	public static String produceOrGroup(String[] words) {
		return produceOrGroup(words, true);
	}
	
	/**
	 * produce a capturing group matching exacly one of the specified words at a
	 * time
	 * @param parts the parts to combine disjunctively
	 * @param escape escape the specified Strings or not?
	 * @return a capturing group matching exacly one of the specified words at a
	 *         time
	 */
	public static String produceOrGroup(String[] parts, boolean escape) {
		if ((parts == null) || (parts.length == 0)) return "";
		StringBuffer assembler = new StringBuffer("(");
		for (int p = 0; p < parts.length; p++)
			assembler.append(((p == 0) ? "" : "|") + (escape ? escapeForRegEx(parts[p]) : parts[p]));
		assembler.append(")");
		return assembler.toString();
	}
	
	/**	produce a pattern matching a textual enumeration of words or phrases matching partEx
	 * @param	partEx	the regular expression matching one part of the enumeration
	 * @return a pattern matching a textual enumeration of matches of partEx
	 * Note: This method assumes that 
	 * a textual enumeration is always terminated with one of the following phrases (-partEx- represents a match of the specified part expression):
	 * -partEx- and -partEx-
	 * -partEx-, ect.
	 * -partEx- and other
	 * -partEx- and others
	 * the other parts are separated by a comma and a whitespace
	 */
	public static String produceEnumerationGroup(String partEx) {
		return produceEnumerationGroup(partEx, false);
	}
	
	/**	produce a pattern matching a textual enumeration of words or phrases matching partEx
	 * @param	partEx			the regular expression matching one part of the enumeration
	 * @param	allowCommaOnly	allow that all parts of an enumeration are separated with commas
	 * @return a pattern matching a textual enumeration of matches of partEx
	 * Note: This method assumes that 
	 * a textual enumeration is always terminated with one of the following phrases (-partEx- represents a match of the specified part expression):
	 * -partEx- and -partEx-
	 * -partEx-, -partEx- (only if allowCommaOnly is set to true)
	 * -partEx-, ect.
	 * -partEx- and other
	 * -partEx- and others
	 * the other parts are separated by a comma and a whitespace
	 */
	public static String produceEnumerationGroup(String partEx, boolean allowCommaOnly) {
		String part = partEx;
		if (!part.startsWith("(")) part = "(" + part;
		if (!part.endsWith(")")) part = part + ")";
		return ("(" +
				"(" + part + "\\,\\s)*" + 
				part + 
				"(" + (allowCommaOnly ? ("(\\,\\s" + part + ")|") : "") + "(\\,?\\sand\\s" + part + ")|(\\,\\setc\\.)|(\\,?\\sand\\sother(s)?))" + 
				")");
	}
	
	/**
	 * normalize a regular expression
	 * @param regEx the regular expression to be normalized
	 * @return the specified regular expression in a form processable by
	 *         java.util.regex.Pattern
	 */
	public static String normalizeRegEx(String regEx) {
		if ((regEx.indexOf("\n") != -1) || (regEx.indexOf("\r") != -1) || (regEx.indexOf("\f") != -1)) {
			int index = 0;
			StringVector sv = new StringVector(false);
			String temp = regEx;
			
			//	normalize linebreaks
			sv.parseAndAddElements(temp, "\r");
			temp = sv.concatStrings("\n");
			sv.clear();
			sv.parseAndAddElements(temp, "\f");
			temp = sv.concatStrings("\n");
			sv.clear();
			sv.parseAndAddElements(temp, "\n\n");
			temp = sv.concatStrings("\n");
			sv.clear();
			
			//	remove indents (tabs and spaces after linebreaks)
			while ((temp.indexOf("\n\t") != -1) || (temp.indexOf("\n ") != -1)) {
				
				//	remove tabs after linebreaks
				sv.parseAndAddElements(temp, "\n\t");
				temp = sv.concatStrings("\n");
				sv.clear();
				
				//	remove tabs after linebreaks
				sv.parseAndAddElements(temp, "\n ");
				temp = sv.concatStrings("\n");
				sv.clear();
			}
			
			//	remove comments and linebreaks
			sv.parseAndAddElements(temp, "\n");
			while (index < sv.size()) {
				if (sv.get(index).startsWith("//"))
					sv.removeElementAt(index);
				else index++;
			}
			return sv.concatStrings("");
		}
		
		//	no line breaks or indents in regex
		else return regEx;
	}
	
	/**
	 * explode a regular expression
	 * @param regEx the regular expression to be exploded
	 * @return the specified regular expression in a form readable for users
	 */
	public static String explodeRegEx(String regEx) {
		return explodeRegEx(regEx, "\t");
	}

	/**
	 * explode a regular expression
	 * @param regEx the regular expression to be exploded
	 * @param indent the String to use for indentation
	 * @return the specified regular expression in a form readable for users
	 */
	public static String explodeRegEx(String regEx, String indent) {
		if ((regEx.indexOf("\n") == -1) && (regEx.indexOf("\r") == -1) && (regEx.indexOf("\f") == -1)) {
			
			StringVector lines = new StringVector();
			
			//	parse regEx
			StringBuffer assembler = new StringBuffer();
			char currentChar = StringUtils.NULLCHAR;
			int escapeIndex = -1;
			for (int charIndex = 0; charIndex < regEx.length(); charIndex++) {
				currentChar = regEx.charAt(charIndex);
				
				//	actual character is escaped
				if (charIndex == escapeIndex)
					assembler.append(currentChar);
					
				//	actual character is escaper
				else if (currentChar == '\\') {
					escapeIndex = (charIndex + 1);
					assembler.append(currentChar);
				}
				
				//	opening bracket
				else if (currentChar == '(') {
					if (assembler.length() != 0) {
						lines.addElement(assembler.toString());
						assembler = new StringBuffer();
					}
					lines.addElement("(");
				}
				
				//	closing bracket
				else if (currentChar == ')') {
					if (assembler.length() != 0) {
						lines.addElement(assembler.toString());
						assembler = new StringBuffer();
					}
					lines.addElement(")");
				}
				
				//	other cheracter
				else assembler.append(currentChar);
			}
			if (assembler.length() != 0)
				lines.addElement(assembler.toString());
			
			//	concat short lines
			int index = 0;
			String line;
			String n1Line;
			String n2Line;
			while ((index + 1) < lines.size()) {
				line = lines.get(index);
				n1Line = lines.get(index + 1);
				n2Line = (((index + 2) < lines.size()) ? lines.get(index + 2) : "");
				if (line.equals("(") && n2Line.equals(")")) {
					lines.setElementAt((line + n1Line + n2Line), index);
					lines.removeElementAt(index + 1);
					lines.removeElementAt(index + 1);
				}
				else if (n1Line.equals("?") || n1Line.equals("*") || n1Line.equals("+")) {
					lines.setElementAt((line + n1Line), index);
					lines.removeElementAt(index + 1);
				}
				else if (n1Line.startsWith("?") || n1Line.startsWith("*") || n1Line.startsWith("+")) {
					lines.setElementAt((line + n1Line.charAt(0)), index);
					lines.setElementAt(n1Line.substring(1), (index + 1));
				}
				else index ++;
			}
			
			//	indent lines
			int indentDepth = 0;
			String indentString = "";
			for (int l = 0; l < lines.size(); l++) {
				line = lines.get(l);
				if (line.equals("(")) {
					lines.setElementAt((indentString + line), l);
					indentDepth++;
					indentString = "";
					for (int i = 0; i < indentDepth; i++) indentString += indent;
				}
				else if (line.startsWith(")")) {
					indentDepth--;
					indentString = "";
					for (int i = 0; i < indentDepth; i++) indentString += indent;
					lines.setElementAt((indentString + line), l);
				}
				else lines.setElementAt((indentString + line), l);
			}
			
			//	concatenate and return result
			return lines.concatStrings("\n");
		}
		
		//	regex already exploded
		else return regEx;
	}
	
	/**	check if a String is a valid JAVA regular expression
	 * @param	string	the String to test
	 * @return true if and only if the specified String represents a valid JAVA regular expression
	 */
	public static boolean isValidRegEx(String string) {
		try {
			"".matches(string);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	private static final String SIMPLE_PATTERN_REGEX = "([a-zA-Z0-9\\-\\_\\.\\,\\:\\;]++)";
	private static final String ALPHANUMERIC_CHAR_REGEX = "[a-zA-Z0-9\\\\\\_\\\\\\.\\\\\\-]"; 
	
	/**
	 * Check if a String represents a simple pattern. A simple pattern consists
	 * of alphanumeric characters and one of four wildcards: '?' matches no or
	 * one arbitrary character, '+' matches exactly one arbitrary character, '*'
	 * matches none or more arbitrary characters, and '#' matches one or more
	 * arbitrary characters.
	 * @param string the String to test
	 * @return true if and only if the specified String is a simple pattern
	 */
	public static boolean isSimplePattern(String string) {
		return ((string != null) && string.replaceAll("[\\*\\+\\?\\#]", "_").matches(SIMPLE_PATTERN_REGEX));
	}
	
	private static Properties PATTERN_CACHE = new Properties();
	
	/**
	 * Convert a simple pattern into a full JAVA regular expression. A simple
	 * pattern consists of alphanumeric characters and one of four wildcards:
	 * '?' matches no or one arbitrary character, '+' matches exactly one
	 * arbitrary character, '*' matches none or more arbitrary characters, and
	 * '#' matches one or more arbitrary characters. This method creates a full
	 * JAVA regular expression from a simple pattern.
	 * @param simplePattern the simple pattern to convert
	 * @return a JAVA regular expression generated from the specified simple
	 *         pattern, or the specified String, if it is not a simple pattern
	 */
	public static String getFullPattern(String simplePattern) {
		if (isSimplePattern(simplePattern)) {
			
			//	do cache lookup 
			if (PATTERN_CACHE.containsKey(simplePattern)) return PATTERN_CACHE.getProperty(simplePattern);
			
			//	convert pattern
			String pattern = simplePattern;
			System.out.println("Simple pattern expression: " + pattern);
			
			//	optimize
			int l = pattern.length() + 1;
			while (l > pattern.length()) {
				l = pattern.length();
				
				//	remove ? next to *
				pattern = pattern.replaceAll("\\?\\*", "*");
				pattern = pattern.replaceAll("\\*\\?", "*");
				
				//	convert + next to * into #
				pattern = pattern.replaceAll("\\+\\*", "#");
				pattern = pattern.replaceAll("\\*\\+", "#");
				
				//	remove * next to #
				pattern = pattern.replaceAll("\\*\\#", "#");
				pattern = pattern.replaceAll("\\#\\*", "#");
				
				//	remove * next to *
				pattern = pattern.replaceAll("\\*\\*", "*");
				
				//	remove # next to #
				pattern = pattern.replaceAll("\\#\\#", "#");
			}
			System.out.println("Optimized simple pattern expression: " + pattern);
			
			//	convert to JAVA pattern
			//	convert ? (none or one)
			pattern = pattern.replaceAll("\\?", (ALPHANUMERIC_CHAR_REGEX + "?"));
			
			//	convert * (none or more)
			pattern = pattern.replaceAll("\\*", (ALPHANUMERIC_CHAR_REGEX + "*"));
			
			//	convert + (exactly one)
			pattern = pattern.replaceAll("\\+", ALPHANUMERIC_CHAR_REGEX);
			
			//	convert # (one or more)
			pattern = pattern.replaceAll("\\#", (ALPHANUMERIC_CHAR_REGEX + "+"));
			
			System.out.println("Converted simple pattern expression: " + pattern);
			PATTERN_CACHE.setProperty(simplePattern, pattern);
			return pattern;
		}
		else return simplePattern;
	}
	
	/**	test if a string matches a simple pattern
	 * @param	string	the String to test
	 * @param	simplePattern	the simple pattern to match the String with
	 * @return true if and only if the specified String matches the specified simple pattern
	 */
	public static boolean matches(String string, String simplePattern) {
		return ((string != null) && string.matches(getFullPattern(simplePattern)));
	}
	
	private static final String SUB_PATTERN_NAME_REGEX = "(\\<\\<[A-Za-z0-9\\.]++\\>\\>)";
	private static final Pattern SUB_PATTERN_NAME_PATTERN = Pattern.compile(SUB_PATTERN_NAME_REGEX);
	
	static final Properties DEFAULT_SUB_PATTERN_RESOLVER = new Properties();
	static {
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("word".toLowerCase(), WORD);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("capitalizedWord".toLowerCase(), CAPITALIZED_WORD);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("upperCaseWord".toLowerCase(), UPPER_CASE_WORD);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("lowerCaseWord".toLowerCase(), LOWER_CASE_WORD);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("abbreviation".toLowerCase(), ABBREVIATION);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("longAbbreviation".toLowerCase(), LONG_ABBREVIATION);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("capitalizedAbbreviation".toLowerCase(), CAPITALIZED_ABBREVIATION);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("capitalizedLongAbbreviation".toLowerCase(), CAPITALIZED_LONG_ABBREVIATION);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("lowerCaseAbbreviation".toLowerCase(), LOWER_CASE_ABBREVIATION);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("lowerCaseLongAbbreviation".toLowerCase(), LOWER_CASE_LONG_ABBREVIATION);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("firstName".toLowerCase(), FIRST_NAME);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("lastName".toLowerCase(), LAST_NAME);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("name".toLowerCase(), PERSON_NAME);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("personName".toLowerCase(), PERSON_NAME);
		DEFAULT_SUB_PATTERN_RESOLVER.setProperty("person".toLowerCase(), PERSON_NAME);
	}
	
	/**
	 * convert a meta pattern to into a full JAVA regular expression.<br>
	 * 
	 * If the specified pattern is a full JAVA regular expression (i.e. it does
	 * not contain placeholders), this method does nothing<br>
	 * 
	 * Otherwise, this method will replace any part of the form
	 * &lt;&lt;subPatternName&gt;&gt; with whatever the specified resolver maps
	 * subPatternName to<br>
	 * 
	 * The names of the sub patterns must not contain characters other than
	 * letters, numbers, and '.'<br>
	 * 
	 * @param metaPattern the meta pattern to convert
	 * @param resolver a mapping of sub pattern names to JAVA regular
	 *            expressions
	 * @return a full JAVA regular expression
	 */
	public static String preCompile(String metaPattern, Properties resolver) {
		return preCompile(metaPattern, resolver, new HashSet());
	}
	private static String preCompile(String metaPattern, Properties resolver, HashSet beingResolved) {
		Properties spnResolver = ((resolver == null) ? new Properties() : resolver);
		
		//	split meta pattern
		String[] parts = metaPattern.split(SUB_PATTERN_NAME_REGEX, -1);
		
		//	nothing to replace
		if (parts.length == 1) return metaPattern;
		
		//	get sub pattern names to resolve
		StringVector subPatternNames = new StringVector();
		Matcher matcher = SUB_PATTERN_NAME_PATTERN.matcher(metaPattern);
		while (matcher.find()) subPatternNames.addElement(matcher.group(0));
		
		//	cut '<<' and '>>'
		for (int s = 0; s < subPatternNames.size(); s++) {
			String spn = subPatternNames.get(s);
			subPatternNames.set(s, spn.substring(2, (spn.length() - 2)));
		}
		
		//	assemble pattern
		String pattern = parts[0];
		int errorIndex = parts[0].length();
		for (int p = 1; p < parts.length; p++) {
			
			//	fetch pattern for name
			String spn = subPatternNames.get(p - 1);
			String sp = spnResolver.getProperty(spn, DEFAULT_SUB_PATTERN_RESOLVER.getProperty(spn.toLowerCase()));
			
			//	pattern not found
			if (sp == null) throw new PatternSyntaxException(("The sub pattern placeholder <<" + spn + ">> cannot be resolved"), metaPattern, errorIndex);
			
			//	catch cyclic replacements
			if (beingResolved.contains(spn)) throw new PatternSyntaxException(("Cyclic reference in sub pattern placeholder <<" + spn + ">>"), metaPattern, errorIndex);
			
			//	compile sub pattern recursively
			beingResolved.add(spn);
			pattern += preCompile(sp, spnResolver, beingResolved);
			beingResolved.remove(spn);
			errorIndex += spn.length() + 4;
			
			//	add next fix part
			pattern += parts[p];
			errorIndex += parts[p].length();
		}
		
		//	return result
		return pattern;
	}
	
	/** validate a regular expression pattern
	 * @param	regEx	the regular expression pattern to validate
	 * @return the error message if the validation fails, null otherwise
	 */
	public static String validateRegEx(String regEx) {
		return validateRegEx(regEx, null);
	}
	
	/** validate a regular expression pattern
	 * @param	regEx		the regular expression pattern to validate
	 * @param	resolver	a Properties for resolving sub pattern placeholders
	 * @return the error message if the validation fails, null otherwise
	 */
	public static String validateRegEx(String regEx, Properties resolver) {
		try {
			Pattern.compile(preCompile(normalizeRegEx(regEx), resolver));
			return null;
		}
		catch (PatternSyntaxException pse) {
			return pse.getDescription();
		}
	}
}
