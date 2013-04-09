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


import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Static class providing comparison finctinality for character sequences.
 * 
 * @author sautter
 */
public class CharSequenceUtils {
	
	/** a hash code for a char sequence, computed as in a string
	 * @param	cs	the char sequence to hash
	 * @see java.lang.Object#hashCode()
	 * @see java.lang.String#hashCode()
	 */
	public static int hashCode(CharSequence cs) {
		return cs.toString().hashCode();
	}
	
	/** a hash code for a char sequence, computed as in a string, but converted to lower case
	 * @param	cs	the char sequence to hash
	 * @see java.lang.Object#hashCode()
	 * @see java.lang.String#hashCode()
	 */
	public static int caseInsensitiveHashCode(CharSequence cs) {
		return cs.toString().toLowerCase().hashCode();
	}
	
	/** Test if a char sequence matches some other char sequence (in terms of String equality)
	 * @param	cs1		the first char sequence to test
	 * @param	cs2		the second char sequence to test
	 * @return true if and only if cs1 equals cs2 in terms of string equality
	 */
	public static boolean equals(CharSequence cs1, CharSequence cs2) {
		int length = cs1.length();
		if (cs2.length() != length) return false;
		for (int c = 0; c < length; c++)
			if (cs1.charAt(c) != cs2.charAt(c)) return false;
		return true;
	}
	
	/** Test if a char sequence matches some other char sequence (in terms of String equality)
	 * @param	cs1		the first char sequence to test
	 * @param	cs2		the second char sequence to test
	 * @return true if and only if cs1 equals cs2 in terms of string equality
	 */
	public static boolean equalsIgnoreCase(CharSequence cs1, CharSequence cs2) {
		int length = cs1.length();
		if (cs2.length() != length) return false;
		for (int c = 0; c < length; c++)
			if ((cs1.charAt(c) != cs2.charAt(c)) && (Character.toLowerCase(cs1.charAt(c)) != Character.toLowerCase(cs2.charAt(c))))
				return false;
		return true;
	}
	
	/** Compare a char sequence to some other char sequence
	 * @param	cs1		the first char sequence to test
	 * @param	cs2		the second char sequence to test
	 * @return a value less than, equal to, or greater than 0 if cs1 is lexicographically less than, equal to, or greater than the specified one
	 */
	public static int compareTo(CharSequence cs1, CharSequence cs2) {
		int length = Math.min(cs1.length(), cs2.length());
		for (int c = 0; c < length; c++)
			if (cs1.charAt(c) != cs2.charAt(c))
				return (cs1.charAt(c) - cs2.charAt(c));
		return (cs1.length() - cs2.length());
	}
	
	/** Caseinsensitively compare a char sequence to some other char sequence
	 * @param	cs1		the first char sequence to test
	 * @param	cs2		the second char sequence to test
	 * @return a value less than, equal to, or greater than 0 if cs1 is lexicographically less than, equal to, or greater than the specified one
	 */
	public static int compareToIgnoreCase(CharSequence cs1, CharSequence cs2) {
		int length = Math.min(cs1.length(), cs2.length());
		for (int c = 0; c < length; c++)
			if ((cs1.charAt(c) != cs2.charAt(c)) && (Character.toLowerCase(cs1.charAt(c)) != Character.toLowerCase(cs2.charAt(c))))
				return (cs1.charAt(c) - cs2.charAt(c));
		return (cs1.length() - cs2.length());
	}
	
	/** Find the index within a char sequence of the first occurrence of another char sequence 
	 * @param	cs			the first char sequence to test
	 * @param	pattern		the second char sequence to test
	 * @return the index within cs1 of the first occurrence of pattern
	 */
	public static int offsetOf(CharSequence cs, CharSequence pattern) {
		return offsetOf(cs, pattern, 0, true);
	}
	
	/** Find the index within a char sequence of the first occurrence of another char sequence 
	 * @param	cs				the first char sequence to test
	 * @param	pattern			the second char sequence to test
	 * @param	caseSensitive	do case sensitive comparison?
	 * @return the index within cs1 of the first occurrence of pattern
	 */
	public static int offsetOf(CharSequence cs, CharSequence pattern, boolean caseSensitive) {
		return offsetOf(cs, pattern, 0, caseSensitive);
	}
	
	/** Find the index within a char sequence of the first occurrence of another char sequence, starting at the specified index 
	 * @param	cs			the first char sequence to test
	 * @param	pattern		the second char sequence to test
	 * @param	from		the offset to start from
	 * @return the index within cs of the first occurrence of pattern, starting at the specified index
	 */
	public static int offsetOf(CharSequence cs, CharSequence pattern, int from) {
		return offsetOf(cs, pattern, from, true);
	}
	
	/** Find the index within a char sequence of the first occurrence of another char sequence, starting at the specified index 
	 * @param	cs				the first char sequence to test
	 * @param	pattern			the second char sequence to test
	 * @param	from			the offset to start from
	 * @param	caseSensitive	do case sensitive comparison?
	 * @return the index within cs of the first occurrence of pattern, starting at the specified index
	 */
	public static int offsetOf(CharSequence cs, CharSequence pattern, int from, boolean caseSensitive) {
		if (pattern.length() == 0) return from;
		if (cs.length() < (from + pattern.length())) return -1;
		char anchorChar = pattern.charAt(0);
		int csLength = cs.length();
		for (int o = from; o < csLength; o++)
			if ((cs.charAt(o) == anchorChar) && startsWith(cs, pattern, o)) return o;
			else if (!caseSensitive && (Character.toLowerCase(cs.charAt(o)) == Character.toLowerCase(anchorChar)) && startsWith(cs, pattern, o)) return o;
		return -1;
	}
	
	/** Find the index within a char sequence of the last occurrence of another char sequence
	 * @param	cs			the first char sequence to test
	 * @param	pattern		the second char sequence to test
	 * @return the index within cs of the last occurrence of pattern
	 */
	public static int lastOffsetOf(CharSequence cs, CharSequence pattern) {
		return lastOffsetOf(cs, pattern, cs.length(), true);
	}
	
	/** Find the index within a char sequence of the last occurrence of another char sequence
	 * @param	cs				the first char sequence to test
	 * @param	pattern			the second char sequence to test
	 * @param	caseSensitive	do case sensitive comparison?
	 * @return the index within cs of the last occurrence of pattern
	 */
	public static int lastOffsetOf(CharSequence cs, CharSequence pattern, boolean caseSensitive) {
		return lastOffsetOf(cs, pattern, cs.length(), caseSensitive);
	}
	
	/** Find the index within a char sequence of the last occurrence of another char sequence, searching backward starting at the specified index
	 * @param	cs			the first char sequence to test
	 * @param	pattern		the second char sequence to test
	 * @param	to			the offset to start from
	 * @return the index within cs of the last occurrence of pattern
	 */
	public static int lastOffsetOf(CharSequence cs, CharSequence pattern, int to) {
		return lastOffsetOf(cs, pattern, to, true);
	}
	
	/** Find the index within a char sequence of the last occurrence of another char sequence, searching backward starting at the specified index
	 * @param	cs				the first char sequence to test
	 * @param	pattern			the second char sequence to test
	 * @param	to				the offset to start from
	 * @param	caseSensitive	do case sensitive comparison?
	 * @return the index within cs of the last occurrence of pattern
	 */
	public static int lastOffsetOf(CharSequence cs, CharSequence pattern, int to, boolean caseSensitive) {
		if (pattern.length() == 0) return to;
		if (cs.length() < pattern.length()) return -1;
		char anchorChar = pattern.charAt(0);
		for (int o = to; o > -1; o--)
			if ((cs.charAt(o) == anchorChar) && startsWith(cs, pattern, o)) return o;
			else if (!caseSensitive && (Character.toLowerCase(cs.charAt(o)) == Character.toLowerCase(anchorChar)) && startsWith(cs, pattern, o)) return o;
		return -1;
	}
	
	/** Tests if a char sequence starts with some prefix
	 * @param	cs		the char sequence to test
	 * @param	prefix	the prefix to test
	 * @return true if cs starts with prefix
	 */
	public static boolean startsWith(CharSequence cs, CharSequence prefix) {
		return startsWith(cs, prefix, 0, true);
	}
	
	/** Tests if a char sequence starts with some prefix
	 * @param	cs				the char sequence to test
	 * @param	prefix			the prefix to test
	 * @param	caseSensitive	do case sensitive comparison?
	 * @return true if cs starts with prefix
	 */
	public static boolean startsWith(CharSequence cs, CharSequence prefix, boolean caseSensitive) {
		return startsWith(cs, prefix, 0, caseSensitive);
	}
	
	/** Tests if a char sequence starts with some prefix at a specific offset
	 * @param	cs		the char sequence to test
	 * @param	prefix	the prefix to test
	 * @param	offset	the offset to look at
	 * @return true if prefix lies in cs, starting at the specified offset 
	 */
	public static boolean startsWith(CharSequence cs, CharSequence prefix, int offset) {
		return startsWith(cs, prefix, offset, true);
	}
	
	/** Tests if a char sequence starts with some prefix at a specific offset
	 * @param	cs				the char sequence to test
	 * @param	prefix			the prefix to test
	 * @param	offset			the offset to look at
	 * @param	caseSensitive	do case sensitive comparison?
	 * @return true if prefix lies in cs, starting at the specified offset 
	 */
	public static boolean startsWith(CharSequence cs, CharSequence prefix, int offset, boolean caseSensitive) {
		if (offset < 0) return false;
		int prefixLength = prefix.length();
		if (cs.length() < (offset + prefixLength)) return false;
		int c;
		for (c = 0; (c < prefixLength) && ((c + offset) < cs.length()); c++)
			if (caseSensitive && cs.charAt(offset + c) != prefix.charAt(c)) return false;
			else if (!caseSensitive && Character.toLowerCase(cs.charAt(offset + c)) != Character.toLowerCase(prefix.charAt(c))) return false;
		return (c == prefixLength);
	}
	
	/** Tests if a char sequence ends with some suffix
	 * @param	cs		the first char sequence to test
	 * @param	suffix	the suffix to test
	 * @return true if cs ends with suffix
	 */
	public static boolean endsWith(CharSequence cs, CharSequence suffix) {
		return startsWith(cs, suffix, (cs.length() - suffix.length()));
	}
	
	/** Tests if a char sequence ends with some suffix
	 * @param	cs				the first char sequence to test
	 * @param	suffix			the suffix to test
	 * @param	caseSensitive	do case sensitive comparison?
	 * @return true if cs ends with suffix
	 */
	public static boolean endsWith(CharSequence cs, CharSequence suffix, boolean caseSensitive) {
		return startsWith(cs, suffix, (cs.length() - suffix.length()), caseSensitive);
	}
	
	/** check if a char sequence is contained in a char sequence
	 * @param	cs1		the first char sequence to test
	 * @param	cs2		the second char sequence to test
	 * @return true if and only if cs1 contains a cs2
	 */
	public static boolean contains(CharSequence cs1, CharSequence cs2) {
		return (offsetOf(cs1, cs2) != -1);
	}
	
	/** check if a char sequence is contained in a char sequence, usin case insensitive comparison
	 * @param	cs1		the first char sequence to test
	 * @param	cs2		the second char sequence to test
	 * @return true if and only if cs1 contains a cs2
	 */
	public static boolean containsIgnoreCase(CharSequence cs1, CharSequence cs2) {
		return (offsetOf(cs1, cs2, false) != -1);
	}
	
	/** Test if a char sequence matches some regular expression pattern
	 * @param	cs		the char sequence to test
	 * @param	regex	the regular expression pattern to test
	 * @return true if and only if cs1 matches the specified regular expression pattern
	 */
	public static boolean matches(CharSequence cs, String regex) {
		return getPattern(regex).matcher(cs).matches();
	}
	
	//	produce a Pattern from a regular expression, or retrieve one from cache
	private static Pattern getPattern(String regex) {
		Pattern pattern = ((Pattern) patternCache.get(regex));
		if (pattern == null) {
			pattern = Pattern.compile(regex);
			patternCache.put(regex, pattern);
		}
		return pattern;
	}
	
	//	cache patterns to save compilation effort when used frequently
	private static HashMap patternCache = new HashMap();
}
