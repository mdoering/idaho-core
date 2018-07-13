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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Stack;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**	Simple converter writing GAMTA documents in ARFF format and reading ARFF files in GAMTA documents 
 */
public class ArffConverter {
	
	private static final boolean DEBUG = false;
	
	public static final String RELATION = "@RELATION";
	public static final String ATTRIBUTE = "@ATTRIBUTE";
	public static final String DATA = "@DATA";
	
	public static final String ANNOTATION_START = "START";
	public static final String ANNOTATION_END = "END";
	public static final String LINE_END = "LINE_END";
	
	public static final String REAL_TYPE = "real";
	public static final String INTEGER_TYPE = "integer";
	public static final String NUMERIC_TYPE = "NUMERIC";
	public static final String STRING_TYPE = "string";
	
	private static final char NULLCHAR = '\u0000';
	
	/**	write a DocumentPart to a file in ARFF format
	 * @param	doc		the DocumentPart to write
	 * @param	file	the file to write to
	 * @throws IOException, if any occurs while writing to the specified file
	 */
	public static void writeArff(QueriableAnnotation doc, File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		writeArff(doc, fw);
		fw.flush();
		fw.close();
	}
	
	/**	write a DocumentPart to an OutputStream in ARFF format
	 * @param	doc		the DocumentPart to write
	 * @param	out		the OutputStream to write to
	 * @throws IOException, if any occurs while writing to the specified OutputStream
	 */
	public static void writeArff(QueriableAnnotation doc, OutputStream out) throws IOException {
		OutputStreamWriter osw = new OutputStreamWriter(out);
		writeArff(doc, osw);
		osw.flush();
	}
	
	/**	write a DocumentPart to a Writer in ARFF format
	 * @param	doc		the DocumentPart to write
	 * @param	out		the Writer to write to
	 * @throws IOException, if any occurs while writing to the specified Writer
	 */
	public static void writeArff(QueriableAnnotation doc, Writer out) throws IOException {
		BufferedWriter buf = new BufferedWriter(out);
		
		//	collect words
		StringVector wordAttributes = new StringVector();
		for (int v = 0; v < doc.size(); v++)
			wordAttributes.addElementIgnoreDuplicates(doc.valueAt(v));
		wordAttributes.sortLexicographically(false, false);
		
		//	get Annotations
		Annotation[] annotations = doc.getAnnotations();
		
		//	collect types and attribute names
		StringVector annotationTypes = new StringVector();
		StringVector annotationAttributes = new StringVector();
		for (int a = 0; a < annotations.length; a++) {
			String type = annotations[a].getType();
			annotationTypes.addElementIgnoreDuplicates(type);
			String[] attributeNames = annotations[a].getAttributeNames();
			for (int an = 0; an < attributeNames.length; an++)
				annotationAttributes.addElementIgnoreDuplicates(type + "." + attributeNames[an]);
		}
		annotationTypes.sortLexicographically(false, false);
		annotationAttributes.sortLexicographically(false, false);
		
		//	write relation name
		buf.write(RELATION + " GamtaDoc");
		buf.newLine();
		buf.newLine();
		
		//	write ARFF attributes
		for (int w = 0; w < wordAttributes.size(); w++) {
			buf.write(ATTRIBUTE + " \"" + wordAttributes.get(w) + "\" " + INTEGER_TYPE);
			buf.newLine();
		}
		buf.write(ATTRIBUTE + " \"" + LINE_END + "\" " + INTEGER_TYPE);
		buf.newLine();
		for (int t = 0; t < annotationTypes.size(); t++) {
			buf.write(ATTRIBUTE + " \"" + ANNOTATION_START + "." + annotationTypes.get(t) + "\" " + INTEGER_TYPE);
			buf.newLine();
			buf.write(ATTRIBUTE + " \"" + ANNOTATION_END + "." + annotationTypes.get(t) + "\" " + INTEGER_TYPE);
			buf.newLine();
		}
		for (int a = 0; a < annotationAttributes.size(); a++) {
			buf.write(ATTRIBUTE + " \"" + annotationAttributes.get(a) + "\" " + STRING_TYPE);
			buf.newLine();
		}
		
		//	start data section
		buf.newLine();
		buf.write(DATA);
		buf.newLine();
		buf.newLine();
		
		Stack stack = new Stack();
		int annotationPointer = 0;
		
		Token token = null;
		
		StringVector openAnnotations = new StringVector();
		StringVector closeAnnotations = new StringVector();
		Properties attributes = new Properties();
		
		for (int i = 0; i < doc.size(); i++) {
			
			//	switch to next Token
			token = doc.tokenAt(i);
			
			//	write end tags for Annotations ending before actual Token
			while ((stack.size() > 0) && ((((Annotation) stack.peek()).getStartIndex() + ((Annotation) stack.peek()).size()) <= i))
				closeAnnotations.addElement(((Annotation) stack.pop()).getType());
			
			//	complete last line (not for first token)
			if (i != 0) {
				
				//	write annotation start and end features
				for (int t = 0; t < annotationTypes.size(); t++) {
					String type = annotationTypes.get(t);
					buf.write("," + ((openAnnotations.contains(type) ? "1" : "0")));
					buf.write("," + ((closeAnnotations.contains(type) ? "1" : "0")));
				}
				
				//	write annotation attribute features
				for (int a = 0; a < annotationAttributes.size(); a++) {
					String value = attributes.getProperty(annotationAttributes.get(a), "");
					buf.write("," + ((value.length() == 0) ? "" : ("\"" + value + "\"")));
				}
				
				//	finish line and clear registers
				buf.newLine();
				openAnnotations.clear();
				closeAnnotations.clear();
				attributes.clear();
			}
			
			//	write start tags for Annotations beginning at actual Token
			while ((annotationPointer < annotations.length) && (annotations[annotationPointer].getStartIndex() == i)) {
				Annotation annotation = annotations[annotationPointer];
				String type = annotation.getType();
				openAnnotations.addElement(type);
				String[] attributeNames = annotation.getAttributeNames();
				for (int an = 0; an < attributeNames.length; an++)
					attributes.setProperty((type + "." + attributeNames[an]), annotation.getAttribute(attributeNames[an], "").toString());
				stack.push(annotations[annotationPointer]);
				annotationPointer++;
			}
			
			//	write word and line end features
			String value = token.getValue();
			for (int w = 0; w < wordAttributes.size(); w++)
				buf.write(((w == 0) ? "" : ",") + (value.equals(wordAttributes.get(w)) ? "1" : "0"));
			buf.write("," + (token.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE) ? "1" : "0"));
		}
		
		//	write end tags for Annotations not closed so far
		while (stack.size() > 0)
			closeAnnotations.addElement(((Annotation) stack.pop()).getType());
		
		//	write annotation start and end features
		for (int t = 0; t < annotationTypes.size(); t++) {
			String type = annotationTypes.get(t);
			buf.write("," + ((openAnnotations.contains(type) ? "1" : "0")));
			buf.write("," + ((closeAnnotations.contains(type) ? "1" : "0")));
		}
		
		//	write annotation attribute features
		for (int a = 0; a < annotationAttributes.size(); a++) {
			String value = attributes.getProperty(annotationAttributes.get(a), "");
			buf.write("," + ((value.length() == 0) ? "" : ("\"" + value + "\"")));
		}
		
		//	finish line and clear registers
		buf.newLine();
		openAnnotations.clear();
		closeAnnotations.clear();
		attributes.clear();
		
		//	flush writer
		buf.flush();
	}
	
	/** read a document from an ARFF file 
	 * @param	file	the file to read from
	 * @return	a GAMTA DocumentPart created from the specified file
	 * @throws IOException, if any occurs while reading from the specified file
	 */
	public static MutableAnnotation readArff(File file) throws IOException {
		FileReader fr = new FileReader(file);
		MutableAnnotation doc = readArff(fr);
		fr.close();
		return doc;
	}
	
	/** read a document from an ARFF file and append it to an existing document
	 * @param	file		the file to read from
	 * @param	document	the document to append the reader's data to
	 * @return the number of tokens added to the specified document
	 * @throws IOException, if any occurs while reading from the specified file
	 */
	public static int readArff(File file, MutableAnnotation document) throws IOException {
		FileReader fr = new FileReader(file);
		int read = readArff(fr, document);
		fr.close();
		return read;
	}
	
	/** read a document from an InputStream providing ARFF data 
	 * @param	input	the InputStream to read from
	 * @return	a GAMTA DocumentPart created from the data provided by the specified InputStream
	 * @throws IOException, if any occurs while reading from the specified InputStream
	 */
	public static MutableAnnotation readArff(InputStream input) throws IOException {
		return readArff(new InputStreamReader(input));
	}
	
	/** read a document from an InputStream providing ARFF data and append it to an existing document
	 * @param	input		the InputStream to read from
	 * @param	document	the document to append the reader's data to
	 * @return the number of tokens added to the specified document
	 * @throws IOException, if any occurs while reading from the specified InputStream
	 */
	public static int readArff(InputStream input, MutableAnnotation document) throws IOException {
		return readArff(new InputStreamReader(input), document);
	}
	
	/** read a document from a Reader providing ARFF data 
	 * @param	input	the Reader to read from
	 * @return	a GAMTA DocumentPart created from the data provided by the specified Reader
	 * @throws IOException, if any occurs while reading from the specified Reader
	 */
	public static MutableAnnotation readArff(Reader input) throws IOException {
		MutableAnnotation doc = Gamta.newDocument(Gamta.INNER_PUNCTUATION_TOKENIZER);
		readArff(input, doc);
		return doc;
	}
		
	/** read a document from a Reader providing ARFF data and append it to an existing document
	 * @param	input	the Reader to read from
	 * @param	doc		the document to append the reader's data to
	 * @return the number of tokens added to the specified document
	 * @throws IOException, if any occurs while reading from the specified Reader
	 */
	public static int readArff(Reader input, MutableAnnotation doc) throws IOException {
		int oldSize = doc.size();
		
		BufferedReader buf = new BufferedReader(input);
		String line = buf.readLine();
		
		//	skip lines up to first attribute
		ArrayList features = new ArrayList();
		while ((line != null) && !line.toUpperCase().startsWith(ATTRIBUTE))
			line = buf.readLine();
		
		//	read attributes
		while ((line != null) && line.startsWith(ATTRIBUTE.toUpperCase())) {
			
			//	parse line
			String lineData = line.substring(ATTRIBUTE.length()).trim();
			int split = lineData.lastIndexOf(' ');
			String name = lineData.substring(0, split).trim();
			boolean wasQuoted = false;
			if (name.startsWith("\""))  {
				name = name.substring(1);
				wasQuoted = true;
			}
			if (name.endsWith("\"")) {
				name = name.substring(0, (name.length() - 1));
				wasQuoted = true;
			}
			String type = lineData.substring(split).trim();
			
			//	token value, line end, or start or end of annotation
			if (INTEGER_TYPE.equalsIgnoreCase(type) || NUMERIC_TYPE.equalsIgnoreCase(type)) {
				
				//	annotation start
				if (name.startsWith(ANNOTATION_START + "."))
					features.add(new Feature(name.substring(ANNOTATION_START.length() + 1), Feature.ANNOTATION_START));
				
				//	annotation end
				else if (name.startsWith(ANNOTATION_END + "."))
					features.add(new Feature(name.substring(ANNOTATION_END.length() + 1), Feature.ANNOTATION_END));
				
				//	line end feature
				else if (LINE_END.equals(name))
					features.add(new Feature("", Feature.LINE_END));
					
				//	token value
				else if (wasQuoted)
					features.add(new Feature(name, Feature.TOKEN_VALUE));
					
				//	other feature
				else features.add(new Feature(name, Feature.OTHER));
			}
			
			//	attribute value (type is STRING_TYPE)
			else if (wasQuoted && (name.indexOf('.') != -1))
				features.add(new Feature(name, Feature.ANNOTATION_ATTRIBUTE));
				
			//	other feature
			else features.add(new Feature(name, Feature.OTHER));
			
			//	read next line
			line = buf.readLine();
		}
		
//		//	TEST OUTPUT
//		System.out.println(annotationTypes.concatStrings("\n"));
//		System.out.println();
//		System.out.println(annotationAttributes.concatStrings("\n"));
//		System.out.println();
//		System.out.println(wordAttributes.concatStrings("\n"));
		
		//	skip lines up to data marker
		while ((line != null) && !line.toUpperCase().startsWith(DATA))
			line = buf.readLine();
		
		//	skip lines up to data
		while ((line != null) && (line.toUpperCase().startsWith(DATA) || (line.trim().length() == 0)))
			line = buf.readLine();
		
		//	read data lines
		String tokenValue = null;
		boolean lineBreak = false;
		StringVector annotationStarts = new StringVector();
		StringVector annotationEnds = new StringVector();
		StringVector attributeNames = new StringVector();
		Properties attributes = new Properties();
		StringVector tokenAnnotations = new StringVector();
		
		ArrayList annotations = new ArrayList();
		while (line != null) {
			String lineData = line.trim();
			
			//	parse line
			if (lineData.length() != 0) {
				
				char quoter = NULLCHAR;
				int featureIndex = 0;
				StringBuffer assembler = new StringBuffer("");
				
				for (int c = 0; c < lineData.length(); c++) {
					char ch = lineData.charAt(c);
					
					if (quoter != NULLCHAR) {
						if (ch == quoter) quoter = NULLCHAR;
						else assembler.append(ch);
					}
					else if ((ch == '"') || (ch == '\''))
						quoter = ch;
					else if (ch == ',') {
						String featureValue = assembler.toString();
						Feature feature = ((Feature) features.get(featureIndex));
						if ("0".equals(featureValue)) {
							//	ignore 0's for now
						}
						else if ("1".equals(featureValue)) {
							if (feature.type == Feature.TOKEN_VALUE) tokenValue = feature.name;
							else if (feature.type == Feature.LINE_END) lineBreak = true;
							else if (feature.type == Feature.ANNOTATION_START) annotationStarts.addElement(feature.name);
							else if (feature.type == Feature.ANNOTATION_END) annotationEnds.addElement(feature.name);
							else tokenAnnotations.addElement(featureValue);
						}
						else if (featureValue.length() != 0) {
							if (feature.type == Feature.ANNOTATION_ATTRIBUTE) {
								attributeNames.addElement(feature.name);
								attributes.setProperty(feature.name, featureValue);
							}
							else tokenAnnotations.addElement(featureValue);
						}
						featureIndex++;
						assembler = new StringBuffer("");
					}
					else assembler.append(ch);
				}
				
				//	check buffer
				if ((assembler.length() != 0) && (featureIndex < features.size())) {
					String featureValue = assembler.toString();
					Feature feature = ((Feature) features.get(featureIndex));
					if ("0".equals(featureValue)) {
						//	ignore 0's for now
					}
					else if ("1".equals(featureValue)) {
						if (feature.type == Feature.TOKEN_VALUE) tokenValue = feature.name;
						else if (feature.type == Feature.LINE_END) lineBreak = true;
						else if (feature.type == Feature.ANNOTATION_START) annotationStarts.addElement(feature.name);
						else if (feature.type == Feature.ANNOTATION_END) annotationEnds.addElement(feature.name);
						else tokenAnnotations.addElement(featureValue);
					}
					else if (featureValue.length() != 0) {
						if (feature.type == Feature.ANNOTATION_ATTRIBUTE) {
							attributeNames.addElement(feature.name);
							attributes.setProperty(feature.name, featureValue);
						}
						else tokenAnnotations.addElement(featureValue);
					}
				}
			}
			
			//	TEST OUTPUT
			if (DEBUG) {
				System.out.println("token: " + tokenValue + (lineBreak ? ", lineBreak" : ""));
				System.out.println("starts: " + annotationStarts.concatStrings(", "));
				System.out.println("ends: " + annotationEnds.concatStrings(", "));
				System.out.println("attributes:");
				for (int a = 0; a < attributeNames.size(); a++)
					System.out.println(" - " + attributeNames.get(a) + ": " + attributes.getProperty(attributeNames.get(a)));
				System.out.println();
			}
			
			//	open Annotations
			while (!annotationStarts.isEmpty()) {
				String type = annotationStarts.remove(0);
				AnnotationContainer ac = new AnnotationContainer(type, doc.size());
				annotations.add(ac);
				for (int a = 0; a < attributeNames.size(); a++) {
					String attributeName = attributeNames.get(a);
					if (attributeName.startsWith(type + ".")) {
						String aName = attributeName.substring(type.length() + 1);
						ac.attributeNames.addElement(aName);
						ac.attributes.setProperty(aName, attributes.getProperty(attributeName));
					}
				}
			}
			
			//	create and store Token
			doc.addTokens(tokenValue);
			doc.lastToken().setAttribute(Token.PARAGRAPH_END_ATTRIBUTE, Token.PARAGRAPH_END_ATTRIBUTE);
			
			//	close Annotations
			while (!annotationEnds.isEmpty()) {
				String type = annotationEnds.remove(0);
				int annotationIndex = 0;
				while (annotationIndex < annotations.size()) {
					AnnotationContainer ac = ((AnnotationContainer) annotations.get(annotationIndex));
					if (ac.type.equals(type)) {
						annotations.remove(annotationIndex);
						ac.size = (doc.size() - ac.start);
						Annotation annotation = doc.addAnnotation(ac.type, ac.start, ac.size);
						for (int a = 0; a < ac.attributeNames.size(); a++) {
							String attributeName = ac.attributeNames.get(a);
							annotation.setAttribute(attributeName, ac.attributes.getProperty(attributeName));
						}
					} else annotationIndex++;
				}
			}
			
			//	add token annotations
			while (!tokenAnnotations.isEmpty())
				doc.addAnnotation(tokenAnnotations.remove(0), (doc.size() - 2), 1);
			
			//	clean up
			tokenValue = null;
			lineBreak = false;
			annotationStarts.clear();
			annotationEnds.clear();
			attributeNames.clear();
			attributes.clear();
			
			//	read next line
			line = buf.readLine();
		}
		
		//	write remaining Annotations (if any)
		while (!annotations.isEmpty()) {
			AnnotationContainer ac = ((AnnotationContainer) annotations.remove(0));
			ac.size = (doc.size() - ac.start);
			Annotation annotation = doc.addAnnotation(ac.type, ac.start, ac.size);
			for (int a = 0; a < ac.attributeNames.size(); a++) {
				String attributeName = ac.attributeNames.get(a);
				annotation.setAttribute(attributeName, ac.attributes.getProperty(attributeName));
			}
		}
		
		return (doc.size() - oldSize);
	}
	
	/**	representation for Annotations that are not yet complete
	 */
	private static class AnnotationContainer {
		private String type;
		private int start;
		private int size = 1;
		
		private StringVector attributeNames = new StringVector();
		private Properties attributes = new Properties();
		
		private AnnotationContainer(String type, int start) {
			this.type = type;
			this.start = start;
		}
	}
	
	/** representation for features
	 */
	private static class Feature {
		private static final int TOKEN_VALUE = 0;
		private static final int LINE_END = 1;
		private static final int ANNOTATION_START = 2;
		private static final int ANNOTATION_END = 3;
		private static final int ANNOTATION_ATTRIBUTE = 4;
		private static final int OTHER = 5;
		
		private String name;
		private int type;
		
		public Feature(String name, int type) {
			this.name = name;
			this.type = type;
		}
	}
}
