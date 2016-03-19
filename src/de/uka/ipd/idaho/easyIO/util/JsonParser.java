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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.easyIO.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uka.ipd.idaho.easyIO.streams.PeekReader;

/**
 * Parser for JSON data, exclusively using core Java classes to represent
 * de-serialized objects, namely HashMap, ArrayList, String, and primitive type
 * wrapper classes.
 * 
 * @author sautter
 */
public class JsonParser {
	
	/**
	 * Escape a string for JavaScript and JSON use, expecting a single quote to
	 * go around the escaped string. Use the two-argument version of this
	 * method to escape a string for other quoters, e.g. double quotes.
	 * @param str the string to escape
	 * @return the escaped string
	 */
	public static String escape(String str) {
		return escape(str, '\'');
	}
	
	/**
	 * Escape a string for JavaScript and JSON use.
	 * @param str the string to escape
	 * @param quot the quoter to go around the escaped string
	 * @return the escaped string
	 */
	public static String escape(String str, char quot) {
		if (str == null)
			return null;
		StringBuffer escaped = new StringBuffer();
		char ch;
		for (int c = 0; c < str.length(); c++) {
			ch = str.charAt(c);
			if (ch == '\r')
				escaped.append("\\r");
			else if (ch == '\n')
				escaped.append("\\n");
			else if (ch < 32)
				escaped.append(' ');
			else {
				if ((ch == quot) || (ch == '\\'))
					escaped.append('\\');
				escaped.append(ch);
			}
		}
		return escaped.toString();
	}
	
	/**
	 * Get a Map representing a JSON object from a List representing a JSON
	 * array. If the object at the argument index is not a Map, this method
	 * returns null.
	 * @param array the JSON array to retrieve the object from
	 * @param index the index of the sought object
	 * @return the Map at the argument index
	 */
	public static Map getObject(List array, int index) {
		Object map = array.get(index);
		return ((map instanceof Map) ? ((Map) map) : null);
	}
	
	/**
	 * Get a List representing a JSON array from another List representing a
	 * JSON array. If the object at the argument index is not a List, this
	 * method returns null.
	 * @param array the JSON array to retrieve the array from
	 * @param index the index of the sought array
	 * @return the List at the argument index
	 */
	public static List getArray(List array, int index) {
		Object list = array.get(index);
		return ((list instanceof List) ? ((List) list) : null);
	}
	
	/**
	 * Get a String from a list representing a JSON array. If the object at the
	 * argument index is not a String, but a Number or Boolean, it is converted
	 * to a String. If it belongs to another class, this method returns null.
	 * @param array the JSON array to retrieve the string from
	 * @param index the index of the sought string
	 * @return the String at the argument index
	 */
	public static String getString(List array, int index) {
		Object string = array.get(index);
		if (string instanceof String)
			return ((String) string);
		else if (string instanceof Number)
			return ((Number) string).toString();
		else if (string instanceof Boolean)
			return ((Boolean) string).toString();
		else return null;
	}
	
	/**
	 * Get a Number from a list representing a JSON array. If the object at the
	 * argument index is not a Number, but a String, this method attempts to
	 * convert it into a Number. If the latter fails, or the object belongs to
	 * another class, this method returns null.
	 * @param array the JSON array to retrieve the number from
	 * @param index the index of the sought number
	 * @return the Number at the argument index
	 */
	public static Number getNumber(List array, int index) {
		Object number = array.get(index);
		if (number instanceof Number)
			return ((Number) number);
		else if (number instanceof String) {
			try {
				return new Long((String) number);
			} catch (NumberFormatException nfe) {}
			try {
				return new Double((String) number);
			} catch (NumberFormatException nfe) {}
			return null;
		}
		else return null;
	}
	
	/**
	 * Get a Boolean from a list representing a JSON array. If the object at
	 * the argument index is not a Boolean, but a String, this method attempts
	 * to convert it into a boolean. If the latter fails, or the object belongs
	 * to another class, this method returns null.
	 * @param array the JSON array to retrieve the boolean from
	 * @param index the index of the sought boolean
	 * @return the Boolean at the argument index
	 */
	public static Boolean getBoolean(List array, int index) {
		Object bool = array.get(index);
		if (bool instanceof Boolean)
			return ((Boolean) bool);
		else if (bool instanceof String)
			return new Boolean((String) bool);
		else return null;
	}
	
	/**
	 * Get a Map representing a JSON object from another Map representing a
	 * JSON object. If the argument name is not mapped to a Map, this method
	 * returns null.
	 * @param object the JSON object to retrieve the object from
	 * @param name the name of the sought object
	 * @return the Map the argument name maps to
	 */
	public static Map getObject(Map object, String name) {
		Object map = object.get(name);
		return ((map instanceof Map) ? ((Map) map) : null);
	}
	
	/**
	 * Get a List representing a JSON array from a Map representing a JSON
	 * object. If the argument name is not mapped to a List, this method
	 * returns null.
	 * @param object the JSON object to retrieve the array from
	 * @param name the name of the sought array
	 * @return the List the argument name maps to
	 */
	public static List getArray(Map object, String name) {
		Object list = object.get(name);
		return ((list instanceof List) ? ((List) list) : null);
	}
	
	/**
	 * Get a String from a Map representing a JSON object. If the argument name
	 * is not mapped to a String, but a Number or Boolean, it is converted to a
	 * String. If it belongs to another class, this method returns null.
	 * @param object the JSON object to retrieve the string from
	 * @param name the name of the sought string
	 * @return the String the argument name maps to
	 */
	public static String getString(Map object, String name) {
		Object string = object.get(name);
		if (string instanceof String)
			return ((String) string);
		else if (string instanceof Number)
			return ((Number) string).toString();
		else if (string instanceof Boolean)
			return ((Boolean) string).toString();
		else return null;
	}
	
	/**
	 * Get a Number from a Map representing a JSON object. If the argument name
	 * is not mapped to a Number, but a String, this method attempts to convert
	 * it into a Number. If the latter fails, or the object belongs to another
	 * class, this method returns null.
	 * @param object the JSON object to retrieve the number from
	 * @param name the name of the sought number
	 * @return the Number the argument name maps to
	 */
	public static Number getNumber(Map object, String name) {
		Object number = object.get(name);
		if (number instanceof Number)
			return ((Number) number);
		else if (number instanceof String) {
			try {
				return new Long((String) number);
			} catch (NumberFormatException nfe) {}
			try {
				return new Double((String) number);
			} catch (NumberFormatException nfe) {}
			return null;
		}
		else return null;
	}
	
	/**
	 * Get a Boolean from a Map representing a JSON object. If the argument
	 * name is not mapped to a Boolean, but a String, this method attempts to
	 * convert it into a boolean. If the latter fails, or the object belongs to
	 * another class, this method returns null.
	 * @param object the JSON object to retrieve the boolean from
	 * @param name the name of the sought boolean
	 * @return the Boolean the argument name maps to
	 */
	public static Boolean getBoolean(Map object, String name) {
		Object bool = object.get(name);
		if (bool instanceof Boolean)
			return ((Boolean) bool);
		else if (bool instanceof String)
			return new Boolean((String) bool);
		else return null;
	}
	
	/**
	 * Parse JSON data from a character stream.
	 * @param in the reader to read from
	 * @return the de-serialized object
	 * @throws IOException
	 */
	public static Object parseJson(Reader in) throws IOException {
		PeekReader pr = new PeekReader(in, 5);
		return cropNext(pr, false);
	}
	
	private static Object cropNext(PeekReader pr, boolean inArrayOrObject) throws IOException {
		pr.skipSpace();
		if (pr.peek() == '"')
			return cropString(pr);
		else if (pr.peek() == '{')
			return cropObject(pr);
		else if (pr.peek() == '[')
			return cropArray(pr);
		else if ("-0123456789".indexOf(pr.peek()) != -1)
			return cropNumber(pr);
		else if (pr.startsWith("null", false)) {
			pr.skip(4);
			return null;
		}
		else if (pr.startsWith("true", false)) {
			pr.skip(4);
			return Boolean.TRUE;
		}
		else if (pr.startsWith("false", false)) {
			pr.skip(5);
			return Boolean.FALSE;
		}
		else if (inArrayOrObject && ((pr.peek() == ',') || (pr.peek() == '}')))
			return null;
		else throw new IOException("Unexpected char '" + ((char) pr.peek()) + "'");
	}
	
	private static Map cropObject(PeekReader pr) throws IOException {
		pr.read(); // consume opening curly bracket
		pr.skipSpace();
		Map map = new HashMap();
		while (pr.peek() != '}') {
			String key = cropString(pr);
			pr.skipSpace();
			if (pr.peek() != ':')
				throw new IOException("Unexpected char '" + ((char) pr.peek()) + "'");
			pr.read(); // consume colon
			Object value = cropNext(pr, true);
			map.put(key, value);
			pr.skipSpace();
			if (pr.peek() == ',') {
				pr.read(); // consume comma (also consumes a dangling one)
				pr.skipSpace();
			}
			else if (pr.peek() != '}')
				throw new IOException("Unexpected char '" + ((char) pr.peek()) + "'");
		}
		pr.read(); // consume closing curly bracket
		return map;
	}
	
	private static List cropArray(PeekReader pr) throws IOException {
		pr.read(); // consume opening square bracket
		pr.skipSpace();
		List array = new ArrayList();
		while (pr.peek() != ']') {
			Object value = cropNext(pr, true);
			array.add(value);
			pr.skipSpace();
			if (pr.peek() == ',') {
				pr.read(); // consume comma (also consumes a dangling one)
				pr.skipSpace();
			}
			else if (pr.peek() != ']')
				throw new IOException("Unexpected char '" + ((char) pr.peek()) + "'");
		}
		pr.read(); // consume closing square bracket
		return array;
	}
	
	private static String cropString(PeekReader pr) throws IOException {
		pr.read(); // consume opening quotes
		boolean escaped = false;
		StringBuffer string = new StringBuffer();
		while (pr.peek() != -1) {
			char ch = ((char) pr.read());
			if (escaped) {
				if (ch == 'b')
					string.append("\b");
				else if (ch == 'f')
					string.append("\f");
				else if (ch == 'n')
					string.append("\n");
				else if (ch == 'r')
					string.append("\r");
				else if (ch == 't')
					string.append("\t");
				else if (ch == 'u') {
					StringBuffer hex = new StringBuffer();
					hex.append((char) pr.read());
					hex.append((char) pr.read());
					hex.append((char) pr.read());
					hex.append((char) pr.read());
					string.append((char) Integer.parseInt(hex.toString(), 16));
				}
				else string.append(ch);
				escaped = false;
			}
			else if (ch == '\\')
				escaped = true;
			else if (ch == '"')
				break;
			else string.append(ch);
		}
		return string.toString();
	}
	
	private static Number cropNumber(PeekReader pr) throws IOException {
		StringBuffer numBuf = new StringBuffer();
		if (pr.peek() == '-') {
			numBuf.append((char) pr.peek());
			pr.read();
			pr.skipSpace();
		}
		StringBuffer intBuf = new StringBuffer();
		while ("0123456789".indexOf(pr.peek()) != -1) {
			numBuf.append((char) pr.peek());
			intBuf.append((char) pr.read());
		}
		pr.skipSpace();
		StringBuffer fracBuf = new StringBuffer();
		if (pr.peek() == '.') {
			numBuf.append((char) pr.peek());
			pr.read();
			while ("0123456789".indexOf(pr.peek()) != -1)
				fracBuf.append((char) pr.read());
		}
		pr.skipSpace();
		StringBuffer expBuf = new StringBuffer();
		if ((pr.peek() == 'e') || (pr.peek() == 'E')) {
			numBuf.append('e');
			pr.read();
			if (pr.peek() == '-') {
				numBuf.append((char) pr.peek());
				pr.read();
				pr.skipSpace();
			}
			else if (pr.peek() == '+') {
				pr.read();
				pr.skipSpace();
			}
			while ("0123456789".indexOf(pr.peek()) != -1) {
				numBuf.append((char) pr.peek());
				expBuf.append((char) pr.read());
			}
		}
		if ((fracBuf.length() + expBuf.length()) == 0)
			return new Long(numBuf.toString());
		else return new Double(numBuf.toString());
	}
	
	//	!!! EXCLSIVELY FOR TEST PURPOSES !!!
	public static void main(String[] args) throws Exception {
		String json = "{" +
					"\"id\": \"113503575767148437762\"," +
//					"\"name\": \"Guido Sautter\"," +
//					"\"given_name\": \"Guido\"," +
//					"\"family_name\": \"Sautter\"," +
					"\"name\": {" +
						"\"full\": \"Guido\\n Sautter\"," +
						"\"given_name\": \"Guido\"," +
						"\"family_name\": \"Sautter\"" +
					"}," +
					"\"link\": \"https://plus.google.com/113503575767148437762\"," +
					"\"gender\": \"male\"," +
					"\"locale\": \"en\"," +
					"\"locales\": [-3.8e7, {\"primary\": \"en\"}, \"de\", \"fr\"]" +
				"}";
		Object obj = parseJson(new StringReader(json));
		System.out.println(obj);
	}
}