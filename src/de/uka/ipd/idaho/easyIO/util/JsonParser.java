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
	 * Parse JSON data from a character stream.
	 * @param in the reader to read from
	 * @return the de-serialized object
	 * @throws IOException
	 */
	public static Map parseJson(Reader in) throws IOException {
		PeekReader pr = new PeekReader(in, 5);
		pr.skipSpace();
		if (pr.peek() != '{')
			throw new IOException("Unexpected char '" + ((char) pr.peek()) + "'");
		else return cropObject(pr);
	}
	
	private static Object cropNext(PeekReader pr) throws IOException {
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
		else throw new IOException("Unexpected char '" + ((char) pr.peek()) + "'");
	}
	
	private static Map cropObject(PeekReader pr) throws IOException {
		pr.read(); // consume opening curly bracket
		pr.skipSpace();
		Map map = new HashMap();
		while (pr.peek() != '}') {
			pr.skipSpace();
			String key = cropString(pr);
			pr.skipSpace();
			if (pr.peek() != ':')
				throw new IOException("Unexpected char '" + ((char) pr.peek()) + "'");
			pr.read(); // consume colon
			map.put(key, cropNext(pr));
			pr.skipSpace();
			if (pr.peek() == ',')
				pr.read(); // consume comma
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
			array.add(cropNext(pr));
			pr.skipSpace();
			if (pr.peek() == ',')
				pr.read(); // consume comma
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
//		boolean intPos;
		if (pr.peek() == '-') {
			numBuf.append((char) pr.peek());
//			intPos = false;
			pr.read();
			pr.skipSpace();
		}
//		else intPos = true;
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
//		boolean expPos;
		if ((pr.peek() == 'e') || (pr.peek() == 'E')) {
			numBuf.append('e');
			pr.read();
			if (pr.peek() == '-') {
//				expPos = false;
				numBuf.append((char) pr.peek());
				pr.read();
				pr.skipSpace();
			}
			else if (pr.peek() == '+') {
//				expPos = true;
				pr.read();
				pr.skipSpace();
			}
//			else expPos = true;
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