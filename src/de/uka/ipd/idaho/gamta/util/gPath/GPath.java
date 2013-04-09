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
package de.uka.ipd.idaho.gamta.util.gPath;


import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Port of the XPath XML query language to the GAMTA data model. Due to the
 * nature of the GAMTA data model to treat annotations as somewhat independen
 * from each other, the ancestore axis behaves differently from the XPath
 * specification, and the preceeding and following axis' always refer to an
 * annotation of the same type as the context annotation.<br>
 * <br>
 * In order to allow for querying the actual document text without annotating
 * every single token, the GamtaPath query language adds a 'token' ('#' for
 * short) axis to what is used from XPath. This latter axis accesses the
 * document text the context annotation contains. Similar to attributes, tokens
 * have children of their own.
 * 
 * @author sautter
 */
public class GPath implements GPathConstants {
	
	/**	@return	the dummy variable resolver to use in (the rather frequent) circumstances that no variables are used
	 */
	public static GPathVariableResolver getDummyVariableResolver() {
		return GPathVariableResolver.getDummyResolver();
	}
	
	GPathStep[] steps = new GPathStep[0];
	
	/**	Constructor
	 */
	GPath() {}
	
	/**	Constructor
	 * @param	path	the String representation of the GPath to create (will be parsed automatically)
	 */
	public GPath(String path) {
		GPath gPath = GPathParser.parsePath(path);
		this.steps = gPath.steps;
//		gPath.steps = null;
	}
	
	/**	evaluate the GPath query
	 * @param	context				the DocumentPart to evaluate the query on
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return an array containing the Annotations resulting form the evaluation
	 * @throws GPathException
	 */
	public QueriableAnnotation[] evaluate(QueriableAnnotation context, GPathVariableResolver variableBindings) throws GPathException {
		return evaluatePath(context, this, variableBindings);
	}
	
	/**	@see java.lang.Object#toString()
	 */
	public String toString() {
		if (this.steps == null) return "";
		if (this.steps.length == 0) return "";
		String string = this.steps[0].toString();
		for (int s = 1; s < this.steps.length; s++)
			string = string + "/" + this.steps[s].toString();
		return string;
	}
	
	public static final GPathEngine DEFAULT_ENGINE = new GPathEngine(true);
	
	/**	add a custom function to the default function library (will overwrite the original function if it has the same name)
	 * @param	functionName	the name the function will be invocable with in GPath queries
	 * @param	function		the GPathFunction to be added
	 */
	public static void addFunction(String functionName, GPathFunction function) {
		if ((functionName != null) && (function != null))
			DEFAULT_ENGINE.addFunction(functionName, function);
	}
	
	/**	remove a custom function from the default function library
	 * @param	functionName	the name of the function to be removed
	 * @return the function that was just removed, or null if there was no function with the specified name
	 */
	public static GPathFunction removeFunction(String functionName) {
		return DEFAULT_ENGINE.removeFunction(functionName);
	}
	
//	/**	compute the String value of a given Annotation
//	 * @param	annotation	the annotation the String value of which to compute
//	 * @return the String value of the specified annotation as an GPathString
//	 */
//	public static GPathString stringValue(Annotation annotation) {
//		if (annotation == null) return new GPathString("");
//		return new GPathString(annotation.getValue());
//	}
//	
//	/**	compute the String values of the Annotations in a given AnnotationSet
//	 * @param	annotationSet		the annotationSet containing the Annotations the String values of which are to compute
//	 * @return the String values of the specified annotations as an array of GPathStrings
//	 */
//	public static GPathString[] stringValues(GPathAnnotationSet annotationSet) {
//		if (annotationSet == null) return new GPathString[0];
//		GPathString[] strings = new GPathString[annotationSet.size()];
//		for (int n = 0; n < annotationSet.size(); n++)
//			strings[n] = stringValue(annotationSet.get(n));
//		return strings;
//	}
//	
	/**	evaluate a GPath query on a DocumentPart
	 * @param	context				the DocumentPart to evaluate the query on
	 * @param	path				a String representing the GPath to evaluate
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return an array containing the Annotations resulting form the evaluation
	 * @throws GPathException
	 */
	public static QueriableAnnotation[] evaluatePath(QueriableAnnotation context, String path, GPathVariableResolver variableBindings) throws GPathException {
		return DEFAULT_ENGINE.evaluatePath(context, path, variableBindings);
	}
	
	/**	evaluate a GPath query on a DocumentPart
	 * @param	context				the DocumentPart to evaluate the query on
	 * @param	path				the GPath to evaluate
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return an array containing the Annotations resulting form the evaluation
	 * @throws GPathException
	 */
	public static QueriableAnnotation[] evaluatePath(QueriableAnnotation context, GPath path, GPathVariableResolver variableBindings) throws GPathException {
		return DEFAULT_ENGINE.evaluatePath(context, path, variableBindings);
	}
	
	/**	evaluate a GPath expression on a DocumentPart
	 * @param	context				the DocumentPart to evaluate the expression on
	 * @param	expression			a String representing the GPath expression to evaluate
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return the resulting GPathObject
	 */
	public static GPathObject evaluateExpression(String expression, QueriableAnnotation context, GPathVariableResolver variableBindings) throws GPathException {
		GPathExpression parsedExpression = GPathParser.parseExpression(expression);
		return DEFAULT_ENGINE.evaluateExpression(parsedExpression, context, variableBindings);
	}
	
	/**	evaluate a GPath expression on a DocumentPart
	 * @param	context				the DocumentPart to evaluate the expression on
	 * @param	expression			the GPath expression to evaluate
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return the resulting GPathObject
	 */
	public static GPathObject evaluateExpression(GPathExpression expression, QueriableAnnotation context, GPathVariableResolver variableBindings) throws GPathException {
		return DEFAULT_ENGINE.evaluateExpression(expression, context, variableBindings);
	}
	
	private static final String TOKEN_AXIS_EXTENSIONS = " first last text word number punctuation sentenceEnd bracket openingBracket closingBracket ";
	
	/**	explode a GPath expression
	 * @param	gPath	the GPath expression to be exploded
	 * @return the specified GPath expression in a form readable for users, in particular broken to multiple lines and indented
	 */
	public static String explodePath(String gPath) {
		return explodePath(gPath, "\t");
	}
	
	/**	explode a GPath expression
	 * @param	gPath	the GPath expression to be exploded
	 * @param	indent	the String to use for indentation
	 * @return the specified GPath expression in a form readable for users, in particular broken to multiple lines and indented
	 */
	public static String explodePath(String gPath, String indent) {
		if ((gPath.indexOf("\n") == -1) && (gPath.indexOf("\r") == -1) && (gPath.indexOf("\f") == -1)) {
			
			StringVector lines = new StringVector();
			
			//	generate path lines
			String[] tokens = GPathParser.tokenize(gPath);
			StringBuffer assembler = new StringBuffer();
			boolean canBeOp = false;
			String lastToken = "";
			for (int t = 0; t < tokens.length; t++) {
				String token = tokens[t];
				if ("[(".indexOf(token) != -1) {
					if (assembler.length() != 0) {
						lines.addElement(assembler.toString());
						assembler = new StringBuffer();
					}
					lines.addElement(token);
					canBeOp = false;
				}
				else if ("])".indexOf(token) != -1) {
					if (assembler.length() != 0) {
						lines.addElement(assembler.toString());
						assembler = new StringBuffer();
					}
					lines.addElement(token);
					canBeOp = false;
				}
				else if ("/".equals(token)) {
					if (assembler.length() != 0) {
						lines.addElement(assembler.toString());
						assembler = new StringBuffer();
					}
					lines.addElement(token);
					canBeOp = false;
				}
				else if (canBeOp && (" and or ".indexOf(" " + token + " ") != -1)) {//  + - * div mod < > = <= != >=
					if (assembler.length() != 0) {
						lines.addElement(assembler.toString());
						assembler = new StringBuffer();
					}
					lines.addElement(token);
					canBeOp = false;
				}
				else {
					if ((assembler.length() != 0) && !"/".equals(token) && !"/".equals(lastToken) && !"::".equals(token) && !"::".equals(lastToken) && !"@".equals(lastToken) && (!"#".equals(lastToken) || (TOKEN_AXIS_EXTENSIONS.indexOf(" " + token + " ") == -1))) assembler.append(" ");
					assembler.append(token);
					canBeOp = true;
				}
				
				//	remember last token
				lastToken = token;
			}
			if (assembler.length() != 0) lines.addElement(assembler.toString());
			
			//	concat short lines
			int index = 0;
			String line;
			String n1Line;
			String n2Line;
			while ((index + 1) < lines.size()) {
				line = lines.get(index);
				n1Line = lines.get(index + 1);
				n2Line = (((index + 2) < lines.size()) ? lines.get(index + 2) : "");
				if (("[(".indexOf(line) != -1) && ("[(".indexOf(line) == "])".indexOf(n2Line))) {
					lines.setElementAt((line + n1Line + n2Line), index);
					lines.removeElementAt(index + 1);
					lines.removeElementAt(index + 1);
				}
				else if (n1Line.equals("(") && n2Line.equals(")")) {
					lines.setElementAt((line + n1Line + n2Line), index);
					lines.removeElementAt(index + 1);
					lines.removeElementAt(index + 1);
				}
				else if (line.equals(".") && n1Line.equals("/")) {
					lines.setElementAt((line + n1Line + n2Line), index);
					lines.removeElementAt(index + 1);
					lines.removeElementAt(index + 1);
				}
				else index ++;
			}
			index = 0;
			while ((index + 1) < lines.size()) {
				line = lines.get(index);
				n1Line = lines.get(index + 1);
				if ((line.indexOf(" ") == -1) && ("[(".indexOf(line) == -1) && (" and or div mod ".indexOf(" " + line + " ") == -1) && n1Line.startsWith("(")) {
					lines.setElementAt((line + n1Line), index);
					lines.removeElementAt(index + 1);
				}
				else if (("<=>!+-*".indexOf(n1Line.charAt(0)) != -1) && (n1Line.indexOf(" ") != -1)) {
					lines.setElementAt((line + " " + n1Line), index);
					lines.removeElementAt(index + 1);
				}
				else index ++;
			}
			
			//	indent lines
			int indentDepth = 0;
			String indentString = "";
			for (int l = 0; l < lines.size(); l++) {
				line = lines.get(l);
				if (line.endsWith("(") || line.endsWith("[")) {
					lines.setElementAt((indentString + line), l);
					indentDepth++;
					indentString = "";
					for (int i = 0; i < indentDepth; i++)
						indentString += indent;
				}
				else if (line.startsWith(")") || line.startsWith("]")) {
					indentDepth--;
					indentString = "";
					for (int i = 0; i < indentDepth; i++)
						indentString += indent;
					lines.setElementAt((indentString + line), l);
				}
				else lines.setElementAt((indentString + line), l);
			}
			
			//	concatenate and return result
			return lines.concatStrings("\n");
		}
		
		//	path already exploded
		else return gPath;
	}
	
	/**	normalize a GPath expression (inverse to the explodePath() methods)
	 * @param	gPath	the GPath expression to be normalized
	 * @return the specified GPath expression in a single line
	 */
	public static String normalizePath(String gPath) {
		StringVector lines = new StringVector();
		lines.parseAndAddElements(gPath, "\n");
		if (lines.isEmpty()) return "";
		String normalizedPath = lines.get(0).trim();
		for (int l = 1; l < lines.size(); l++) {
			String line = lines.get(l).trim();
			if (!line.startsWith(")")
					&& !line.startsWith("]")
					&& !line.startsWith("[")
					&& !line.startsWith("/")
					&& !line.startsWith("::")
					&& !normalizedPath.endsWith("(")
					&& !normalizedPath.endsWith("[")
					&& !normalizedPath.endsWith("]")
					&& !normalizedPath.endsWith("/")
					&& !normalizedPath.endsWith("::")
					&& !normalizedPath.endsWith("@")
					&& (!normalizedPath.endsWith("#") || (TOKEN_AXIS_EXTENSIONS.indexOf(" " + line + " ") == -1))
				) normalizedPath += " ";
			normalizedPath += line;
		}
		return normalizedPath;
	}
}
