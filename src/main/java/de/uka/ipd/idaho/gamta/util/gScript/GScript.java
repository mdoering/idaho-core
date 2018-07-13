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
package de.uka.ipd.idaho.gamta.util.gScript;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.VariableNotBoundException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathAnnotationSet;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;
import de.uka.ipd.idaho.gamta.util.gScript.exceptions.GScriptException;
import de.uka.ipd.idaho.gamta.util.gScript.functions.AnnotateFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.CopyAttributesFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.DeleteAnnotationsFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.ListAnnotationsFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.ParquettFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.RemoveAnnotationsFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.RemoveAttributeFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.RemoveDuplicateAnnotationsFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.RenameAnnotationsFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.RenameAttributeFunction;
import de.uka.ipd.idaho.gamta.util.gScript.functions.SetAttributeFunction;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * The Gamta Script main class, holding constants as well as utility functions,
 * the tokenizer, and the execution engine
 * 
 * <p>
 * Gamta Script is a scripting extension of the GPath query language for Gamta
 * DocumentParts. It adds simple modification functions for Annotations,
 * extensibility with regard to integrating new Java implemented functions, plus
 * some execution control mechanisms, in particular if-elseIf-else blocks and
 * for loops over GPathAnnotationSets.
 * </p>
 * 
 * <p>
 * Gamta Script uses the type system and expression syntax of GPath, which has
 * it adopted from the W3C XPath XML query language. Note that functions are
 * free with regard to how they interpret their parameters internally. The
 * explanations below refer to how expressions are evaluated before they are
 * actually passed to the function.
 * </p>
 * 
 * <p>
 * <b>Basics:</b>
 * </p>
 * 
 * <p>
 * <b>Expressions</b>: enclosing a function parameter in '(' and ')' causes the content
 * of the brackets to be evaluated as a GPath expression. The result of the
 * evaluation is then passed to the function as a parameter.
 * </p>
 * 
 * <p>
 * <b>Annotation set constructors</b>: enclosing a function parameter in '{' and '}'
 * causes the content of the curly brackets to be treated as a GPath query to
 * the context DocumentPart. The resulting GPathAnnotationSet is passed to the
 * function as a parameter, with one exception: If the function does not expect
 * a GPathAnnotationSet for the particular parameter, the annotation set is not
 * converted according to the conversion rules of GPath, but the function is
 * executed once for each Annotation in the set, each time using the value of
 * the current Annotation as the function parameter. This behavior mimics a "for
 * all" statement, or an implicit in line for loop.
 * </p>
 * 
 * <p>
 * <b>Functions:</b>
 * </p>
 * 
 * <p>
 * The Gamta Script language has a set of built-in functions, explanations
 * below.
 * </p>
 * 
 * <p>
 * Create an Annotation of some type, starting at some index, and either
 * spanning a given number of tokens, or ending at an explicit index.<br>
 * <b>annotate <i>(GPathString type, GPathNumber startIndex, GPathNumber endIndex,
 * GPathString mode (optional))</i></b><br> - GPathString type - the type for the
 * Annotation to create<br> - GPathNumber startIndex - the index in the
 * document the new Annotations is to start at (inclusive)<br> - GPathNumber
 * endIndex - the index in the document the new Annotations is to end at
 * (exclusive), or the number of tokens to include in the Annotation, depending
 * on mode<br> - GPathString mode (optional) - the annotation mode - '-e'
 * annotates using start and end index (default), '-s' annotates using start
 * index and size.
 * </p>
 * 
 * <p>
 * Delete the Annotations contained in a GPathAnnotationSet, removing the Tokens
 * they span from the document.<br>
 * <b>delete <i>(GPathAnnotationSet annotationSet)</i></b><br> - GPathAnnotationSet
 * annotationSet - a GPathAnnotationSet holding the Annotations to process
 * </p>
 * 
 * <p>
 * List the Annotations contained in a GPathAnnotationSet. This function is very
 * useful for testing GPathExpressions in a console.<br>
 * <b>list <i>(GPathAnnotationSet annotationSet)</i></b><br> - GPathAnnotationSet
 * annotationSet - a GPathAnnotationSet holding the Annotations to process
 * </p>
 * 
 * <p>
 * List all the avaliable GScript functions. This will show all the functions
 * listed here, plus the ones added dynamically.<br>
 * <b>listF()</b>
 * </p>
 * 
 * <p>
 * Cover the gaps between the Annotations in a GPathAnnotationSet with new
 * Annotations of a specific type, optionally enclosing the Annotations in the
 * set at the start or end of the new Annotations.<br>
 * <b>parquett <i>(GPathAnnotationSet annotationSet, GPathString type, GPathString
 * mode (optional))</i></b><br> - GPathAnnotationSet annotationSet - a
 * GPathAnnotationSet holding the Annotations to process<br> - GPathString type -
 * the type for the parquetting annotations to create<br> - GPathString mode
 * (optional) - the parquetting mode - '-n' parquetts only the gap (default),
 * '-s' includes the specified Annotations at the start of the newly created
 * ones, '-e' includes them at the end.
 * </p>
 * 
 * <p>
 * Remove the Annotations contained in a GPathAnnotationSet from the document.<br>
 * <b>remove <i>(GPathAnnotationSet annotationSet)</i></b><br> - GPathAnnotationSet
 * annotationSet - a GPathAnnotationSet holding the Annotations to process
 * </p>
 * 
 * <p>
 * Remove a specific attribute from all Annotations in a GPathAnnotationSet.<br>
 * <b>removeAttribute <i>(GPathAnnotationSet annotationSet, GPathString attributeName
 * (optional))</i></b><br> - GPathAnnotationSet annotationSet - a GPathAnnotationSet
 * holding the Annotations to process<br> - GPathString attributeName
 * (optional) - the name of the attribute to remove - if not specified, all
 * attributes are removed from the affected annotations.
 * </p>
 * 
 * <p>
 * Remove duplicate Annotations contained in a GPathAnnotationSet. Attributes
 * will be merged. This function makes particular sense when selectiong the
 * Annotations solely by type.<br>
 * <b>removeDuplicates <i>(GPathString type (optional))</i></b><br> - GPathString type
 * (optional) - the type of annotations whose duplicates to remove (optional, if
 * not specified, duplicate elimination will be applied to all annotations)
 * </p>
 * 
 * <p>
 * Rename the Annotations contained in a GPathAnnotationSet, setting their type
 * to a specific value.<br>
 * <b>rename <i>(GPathAnnotationSet annotationSet, GPathString newType)</i></b><br> -
 * GPathAnnotationSet annotationSet - a GPathAnnotationSet holding the
 * Annotations to process<br> - GPathString newType - the type to rename the
 * specified Annotations to
 * </p>
 * 
 * <p>
 * Rename a specific attribute of all Annotations in a GPathAnnotationSet. If an
 * Annotation does not have the specified attribute, no action is taken for this
 * particular Annotation.<br>
 * <b>renameAttribute <i>(GPathAnnotationSet annotationSet, GPathString attributeName,
 * GPathString newAttributeName)</i></b><br> - GPathAnnotationSet annotationSet - a
 * GPathAnnotationSet holding the Annotations to process<br> - GPathString
 * attributeName - the name of the attribute to rename<br> - GPathString
 * newAttributeName - the name to rename the attribute to
 * </p>
 * 
 * <p>
 * Set a specific attribute to a specific value for all Annotations in a
 * GPathAnnotationSet. It does not make any difference if the Annotation has the
 * specified attribute prior to the incokation of this function. If it is to
 * make a difference, the GPathExpression creating the Annotation set has to
 * reflect this, for instance using a predicate.<br>
 * <b>setAttribute <i>(GPathAnnotationSet annotationSet, GPathString attributeName,
 * GPathString attributeValue)</i></b><br> - GPathAnnotationSet annotationSet - a
 * GPathAnnotationSet holding the Annotations to process<br> - GPathString
 * attributeName - the name of the attribute to set<br> - GPathString
 * attributeValue - the value to set the attribute to
 * </p>
 * 
 * <p>
 * Copy all attributes from the first Annotation in on set to all Annotations in
 * another set.<br>
 * <b>copyAttributes <i>(GPathAnnotationSet sourceSet, GPathAnnotationSet targetSet)</i></b><br> -
 * GPathAnnotationSet sourceSet - the GPathAnnotationSet containing the
 * Annotations from the first of which to take the attributes. If this set is
 * empty, this function does nothing<br> - GPathAnnotationSet targetSet - the
 * GPathAnnotationSet containing the Annotations to copy the attributes to
 * </p>
 * 
 * <p>
 * <b>Control statements: </b>
 * </p>
 * 
 * <p>
 * The Gamta Script language includes if-eleseIf-else bocks and for loops as
 * control statements. They work as follows:<br>
 * <br> - <b>if-elseIf-else blocks</b>:<br>
 * <code><b>if</b> (expresion1)<br>
 * &nbsp;&nbsp;statement1<br>
 * <b>elseIf</b> (expression2)<br>
 * &nbsp;&nbsp;statement2<br>
 * ...<br>
 * <b>elseIf</b> (expressionN)<br>
 * &nbsp;&nbsp;statementN<br>
 * <b>else</b><br>
 * &nbsp;&nbsp;statementN+1<br></code> <br>
 * <p>
 * The conditions are GPath expressions, whose result is converted to a
 * GPathBoolean according to the conventions of GPath. The statement following
 * the first expression to evaluate to true is executed. The can be at most one
 * statement per condition. If there is no statement following the first
 * condition to hold, but another elseIf or else line, no action is taken. This
 * can be exploited to catch circumstances where the entire if-eleseIf-else
 * block should do nothing. The elseIf and else lines are optional. Note,
 * however, that there may be at most one else line, and no elseIf line may
 * follow it.<br>
 * <br> - <b>for loops</b>:<br>
 * <code><b>for</b> $vName {GPathExpression}<br>
 * &nbsp;&nbsp;statement1<br>
 * &nbsp;&nbsp;...<br>
 * &nbsp;&nbsp;statementN<br>
 * <b>end</b> $vName<br></code> <br>
 * The GPathExpression in the for line creates a GPathAnnotationSet, and the
 * loop runs once for each Annotation in it. In each run of the loop, the value
 * of one Annotation is bound to the variable $vName. the last line marks the
 * end of the loop over the Annotation set created in the first line. You have
 * to specify the name of the running variable in the last line because for
 * loops may be nested. If the end line is missing, the end of either the
 * enclosing for loop, or the end the script is assumed to be the end of the
 * loop, whichever occurs first. There may be an arbitrary number of statements
 * in a for loop.<br>
 * <br> - <b>sub scripts</b>:<br>
 * <code><b>sub</b> sName<br>
 * &nbsp;&nbsp;statement1<br>
 * &nbsp;&nbsp;...<br>
 * &nbsp;&nbsp;statementN<br>
 * <b>endSub</b><br></code> <br>
 * This syntax creates a sub script to a script defined in the file. Sub scripts
 * have to be defined above the main scrip. They can then be called from or
 * included in the main script. When resolving a script name to call or include,
 * sub scripts defined in the same file as the main script take precedence over
 * other main scripts.
 * </p>
 * 
 * @author sautter
 */
public class GScript {
	
	public static final String GPATH_OBJECT_TYPE = "GPathObject";
	public static final String GPATH_STRING_TYPE = "GPathString";
	public static final String GPATH_NUMBER_TYPE = "GPathNumber";
	public static final String GPATH_BOOLEAN_TYPE = "GPathBoolean";
	public static final String GPATH_ANNOTATION_SET_TYPE = "GPathAnnotationSet";
	
	public static final char ANNOTATION_SET_CONSTRUCTOR_START = '{';
	public static final char ANNOTATION_SET_CONSTRUCTOR_END = '}';
	
	public static final char EXPRESSION_START = '(';
	public static final char EXPRESSION_END = ')';
	
	public static final char ESCAPER = '\\';
	public static final String COMMENT_LINE_START = "--";
	
	public static final String START_FOR_LOOP_COMMAND = "for";
	public static final String END_FOR_LOOP_COMMAND = "end";
	
	public static final String START_SUB_SCRIPT_COMMAND = "sub";
	public static final String END_SUB_SCRIPT_COMMAND = "endSub";
	
	public static final String IF_COMMAND = "if";
	public static final String ELSE_IF_COMMAND = "elseIf";
	public static final String ELSE_COMMAND = "else";
	
	public static final String DEFINE_VARIABLE_COMMAND = "def";
	public static final String CLEAR_VARIABLE_DEFINITION_COMMAND = "clearDef";
	public static final String CLEAR_VARIABLE_DEFINITIONS_COMMAND = "clearDefs";
	
	public static final String CALL_COMMAND = "call";
	public static final String INCLUDE_COMMAND = "include";
	
	private static HashMap functions = new HashMap();
	
	static {
		addFunction(new AnnotateFunction());
		addFunction(new ParquettFunction());
		
		addFunction(new ListAnnotationsFunction());
		
		addFunction(new RenameAnnotationsFunction());
		addFunction(new RemoveDuplicateAnnotationsFunction());
		addFunction(new RemoveAnnotationsFunction());
		addFunction(new DeleteAnnotationsFunction());
		
		addFunction(new RenameAttributeFunction());
		addFunction(new RemoveAttributeFunction());
		addFunction(new SetAttributeFunction());
		addFunction(new CopyAttributesFunction());
	}
	
	/**
	 * Interface to be implemented by components that can resolve the name of an
	 * included or called GAMTA script to the actual script
	 * 
	 * @author sautter
	 */
	public static interface ScriptNameResolver {
		
		/**
		 * Resolve the name of an included or called GAMTA script to the actual
		 * script
		 * @param scriptName the script name to resolve
		 * @return the script with the specified name, or null if there is no
		 *         such script
		 */
		public abstract String getScriptForName(String scriptName);
	}
	
	/**
	 * Tokenize a GAMTA script command given as a String
	 * @param command the String representation of the command and parameters to
	 *            tokenize
	 * @return an array of Strings holding the tokens of the specified sript
	 */
	public static String[] tokenizeCommand(String command) throws GScriptException {
		try {
			return tokenizeCommand(new StringReader(command));
		}
		catch (IOException ioe) {
			return new String[0];
		}
	}
	
	/**
	 * Tokenize a GAMTA script command provided by a reader
	 * @param source the Reader providing the command and parameters to tokenize
	 * @return an array of Strings holding the tokens of the script provided by
	 *         the specified Reader
	 */
	public static String[] tokenizeCommand(Reader source) throws IOException, GScriptException {
		char currentChar;
		int escape = -1;
		int position = 0;
		char quoter = StringUtils.NULLCHAR;
		StringVector tokenCollector = new StringVector();
		StringBuffer tokenAssembler = new StringBuffer();
		LinkedList stack = new LinkedList();
		while ((currentChar = ((char) source.read())) != ((char) -1)) {
			
			//	char is escaped
			if (position == escape) {
				tokenAssembler.append(currentChar);
			}
			
			//	escaper
			else if (currentChar == ESCAPER) {
				escape = position;
				position--; // compensate increment at end of loop
			}
			
			//	in annotation set constructor (GPath Expression)
			else if (quoter == ANNOTATION_SET_CONSTRUCTOR_START) {
				
				tokenAssembler.append(currentChar);
				
				//	end of path expression
				if (currentChar == ANNOTATION_SET_CONSTRUCTOR_END) {
					
					stack.removeLast();
					
					if (stack.isEmpty()) {
						String token = tokenAssembler.toString().trim();
						if (token.length() > 0) tokenCollector.addElement(token);
						tokenAssembler = new StringBuffer();
						quoter = StringUtils.NULLCHAR;
					}
					
					else quoter = ((Character) stack.getLast()).charValue();
				}
				
				//	start of annotation set constructor (GPath Expression)
				else if (currentChar == ANNOTATION_SET_CONSTRUCTOR_START)
					throw new GScriptException("An annotation set constructor cannot start inside another expression.", tokenCollector.toStringArray());
				
				//	start of expression
				else if (currentChar == EXPRESSION_START) {
					quoter = currentChar;
					stack.addLast(new Character(quoter));
				}
				
				//	start of quoted area
				else if ((currentChar == '\'') || (currentChar == '"')) {
					quoter = currentChar;
					stack.addLast(new Character(quoter));
				}
			}
			
			//	in expression
			else if (quoter == EXPRESSION_START) {
				
				tokenAssembler.append(currentChar);
				
				//	end of path expression
				if (currentChar == EXPRESSION_END) {
					
					stack.removeLast();
					
					if (stack.isEmpty()) {
						String token = tokenAssembler.toString().trim();
						if (token.length() > 0) tokenCollector.addElement(token);
						tokenAssembler = new StringBuffer();
						quoter = StringUtils.NULLCHAR;
					}
					
					else quoter = ((Character) stack.getLast()).charValue();
				}
				
				//	start of annotation set constructor (GPath Expression)
				else if (currentChar == ANNOTATION_SET_CONSTRUCTOR_START)
					throw new GScriptException("An annotation set constructor cannot start inside another expression.", tokenCollector.toStringArray());
				
				//	start of expression
				else if (currentChar == EXPRESSION_START) {
					quoter = currentChar;
					stack.addLast(new Character(quoter));
				}
				
				//	start of quoted area
				else if ((currentChar == '\'') || (currentChar == '"')) {
					quoter = currentChar;
					stack.addLast(new Character(quoter));
				}
			}
			
			//	in quoted area
			else if (quoter != StringUtils.NULLCHAR) {
				
				//	end of quoted area
				if (currentChar == quoter) {
					
					stack.removeLast();
					
					if (stack.isEmpty()) {
						String token = tokenAssembler.toString().trim();
						if (token.length() > 0) tokenCollector.addElement(token);
						tokenAssembler = new StringBuffer();
						quoter = StringUtils.NULLCHAR;
					}
					else {
						tokenAssembler.append(currentChar);
						quoter = ((Character) stack.getLast()).charValue();
					}
				}
				else tokenAssembler.append(currentChar);
			}
			
			//	start of annotation set constructor (GPath Expression)
			else if (currentChar == ANNOTATION_SET_CONSTRUCTOR_START) {
				
				if (stack.isEmpty()) {
					String token = tokenAssembler.toString().trim();
					if (token.length() > 0) tokenCollector.addElement(token);
					tokenAssembler = new StringBuffer();
				}
				
				tokenAssembler.append(currentChar);
				
				quoter = currentChar;
				stack.addLast(new Character(quoter));
			}
			
			//	end of annotation set constructor without constructor open
			else if (currentChar == ANNOTATION_SET_CONSTRUCTOR_END)
				throw new GScriptException("Unmatched annotation set constructor end.", tokenCollector.toStringArray());
			
			//	start of expression
			else if (currentChar == EXPRESSION_START) {
				
				if (stack.isEmpty()) {
					String token = tokenAssembler.toString().trim();
					if (token.length() > 0) tokenCollector.addElement(token);
					tokenAssembler = new StringBuffer();
				}
				
				tokenAssembler.append(currentChar);
				
				quoter = currentChar;
				stack.addLast(new Character(quoter));
			}
			
			//	end of annotation set constructor without constructor open
			else if (currentChar == EXPRESSION_END)
				throw new GScriptException("Unmatched expression end.", tokenCollector.toStringArray());
			
			//	start of quoted area
			else if ((currentChar == '\'') || (currentChar == '"')) {
				
				if (stack.isEmpty()) {
					String token = tokenAssembler.toString().trim();
					if (token.length() > 0) tokenCollector.addElement(token);
					tokenAssembler = new StringBuffer();
				}
				else tokenAssembler.append(currentChar);
				
				quoter = currentChar;
				stack.addLast(new Character(quoter));
			}
			
			//	whitespace character delimiting parameters
			else if ((currentChar == ' ') || (currentChar == '\t')) {
				String token = tokenAssembler.toString().trim();
				if (token.length() > 0) tokenCollector.addElement(token);
				tokenAssembler = new StringBuffer();
			}
			
			//	other character
			else {
				tokenAssembler.append(currentChar);
				
				//	command is comment
				if (tokenCollector.isEmpty() && COMMENT_LINE_START.equals(tokenAssembler.toString())) {
					tokenCollector.addElement(tokenAssembler.toString());
					tokenAssembler = new StringBuffer();
				}
			}
				
			//	switch position
			position ++;
		}
		
		//	store last token
		String token = tokenAssembler.toString().trim();
		if (token.length() > 0) tokenCollector.addElement(token);
		
		if (stack.size() != 0) throw new GScriptException("Missing closing bracket or quoter.", tokenCollector.toStringArray());
		
		return tokenCollector.toStringArray();
	}
	
	private static final PrintWriter DEFAULT_OUTPUT_DESTINATION = new PrintWriter(System.out);
	private static PrintWriter OUTPUT_DESTINATION = DEFAULT_OUTPUT_DESTINATION;
	
	/**
	 * Write a line of output (does not necessarily go to System.out)
	 * @param line the line to write
	 */
	public static void output(String line) {
		OUTPUT_DESTINATION.println(line);
	}
	
	/**
	 * Set the destination output() writes to (null sets the destination to
	 * System.out, the default)
	 * @param pw the PrintWriter to direct output to
	 */
	public static void setOutputDestination(PrintWriter pw) {
		OUTPUT_DESTINATION = ((pw == null) ? DEFAULT_OUTPUT_DESTINATION : pw);
	}
	
	/**
	 * Test if a command is valid
	 * @param command the command to check
	 * @return true if and only if there is a function with the specified name
	 */
	public static boolean isValidCommand(String command) {
		return (
			START_FOR_LOOP_COMMAND.equals(command) ||
			END_FOR_LOOP_COMMAND.equals(command) ||
			
			START_SUB_SCRIPT_COMMAND.equals(command) ||
			END_SUB_SCRIPT_COMMAND.equals(command) ||
			
			IF_COMMAND.equals(command) ||
			ELSE_IF_COMMAND.equals(command) ||
			ELSE_COMMAND.equals(command) ||
			
			DEFINE_VARIABLE_COMMAND.equals(command) ||
			CLEAR_VARIABLE_DEFINITION_COMMAND.equals(command) ||
			CLEAR_VARIABLE_DEFINITIONS_COMMAND.equals(command) ||
			
			CALL_COMMAND.equals(command) ||
			INCLUDE_COMMAND.equals(command) ||
			
			functions.containsKey(command)
		);
	}
	
	/**
	 * Add a function to the function library
	 * @param function the function to add
	 */
	public static void addFunction(GScriptFunction function) {
		String functionName = function.getName();
		ArrayList functionListCs = ((ArrayList) functions.get(functionName));
		if (functionListCs == null) {
			functionListCs = new ArrayList();
			functions.put(functionName, functionListCs);
		}
		functionListCs.add(function);
	}
	
	/**
	 * Obtain a function from the function library
	 * @param functionName the name of the desired function
	 * @param parameters the function invokation parameters (helps
	 *            disambiguating if there are multiple functions with the same
	 *            name)
	 * @return a function with the specified name, best matching the specified
	 *         parameters
	 */
	public static GScriptFunction getFunction(String functionName, GPathObject[] parameters) {
		ArrayList functionList = ((ArrayList) functions.get(functionName));
		if (functionList == null) return null;
		else if (functionList.size() == 1) return ((GScriptFunction) functionList.get(0));
		else for (int f = 0; f < functionList.size(); f++) {
			GScriptFunction function = ((GScriptFunction) functionList.get(f));
			if (function.getParameterNames().length == parameters.length)
				return function;
		}
		return null;
	}
	
	/**
	 * @return an array holding the names of the available Gamta Script
	 *         functions
	 */
	public static String[] getFunctionNames() {
		ArrayList functionNameList = new ArrayList(functions.keySet());
		Collections.sort(functionNameList);
		return ((String[]) functionNameList.toArray(new String[functionNameList.size()]));
	}
	
	/**
	 * @return an array of String arrays, each of the second-order arrays
	 *         holding the name and the parameter descriptions of one of the
	 *         available Gamta Script functions
	 */
	public static String[][] getFunctionDescriptions() {
		ArrayList functionDescriptionList = new ArrayList();
		ArrayList functionNameList = new ArrayList(functions.keySet());
		Collections.sort(functionNameList);
		for (int fn = 0; fn < functionNameList.size(); fn++) {
			String functionName = functionNameList.get(fn).toString();
			ArrayList functionList = ((ArrayList) functions.get(functionName));
			for (int f = 0; f < functionList.size(); f++) {
				GScriptFunction function = ((GScriptFunction) functionList.get(f));
				
				String[] pNames = function.getParameterNames();
				String[] pTypes = function.getParameterTypes();
				String[] pDescriptions = function.getParameterDescriptions();
				
				String[] functionDescription = new String[1 + pNames.length];
				String pString = "";
				for (int p = 0; p < pNames.length; p++) {
					pString += ((p == 0) ? "" : ", ") + pTypes[p] + " " + pNames[p];
					functionDescription[p + 1] = (" - " + pTypes[p] + " " + pNames[p] + " - " + pDescriptions[p]);
				}
				functionDescription[0] = functionName + "(" + pString + ")";
				functionDescriptionList.add(functionDescription);
			}
		}
		return ((String[][]) functionDescriptionList.toArray(new String[functionDescriptionList.size()][]));
	}
	
	/**
	 * @param functionName the name of the function(s) the description to obtain
	 *            for
	 * @return an array of String arrays, each of the second-order arrays
	 *         holding the name and the parameter descriptions of one of the
	 *         available Gamta Script functions with the specified name
	 */
	public static String[][] getFunctionDescriptions(String functionName) {
		ArrayList functionDescriptionList = new ArrayList();
		ArrayList functionList = ((ArrayList) functions.get(functionName));
		if (functionList != null) {
			for (int f = 0; f < functionList.size(); f++) {
				GScriptFunction function = ((GScriptFunction) functionList.get(f));
				
				String[] pNames = function.getParameterNames();
				String[] pTypes = function.getParameterTypes();
				String[] pDescriptions = function.getParameterDescriptions();
				
				String[] functionDescription = new String[1 + pNames.length];
				String pString = "";
				for (int p = 0; p < pNames.length; p++) {
					pString += ((p == 0) ? "" : ", ") + pTypes[p] + " " + pNames[p];
					functionDescription[p + 1] = (" - " + pTypes[p] + " " + pNames[p] + " - " + pDescriptions[p]);
				}
				functionDescription[0] = functionName + "(" + pString + ")";
				functionDescriptionList.add(functionDescription);
			}
		}
		return ((String[][]) functionDescriptionList.toArray(new String[functionDescriptionList.size()][]));
	}
	
	private static Vector scriptNameResolvers = new Vector(); 
	
	/**
	 * Register a script name resolver so it can be used to resolve scrip names
	 * @param resolver the ScriptNameResolver to add
	 */
	public static void addScriptNameResolver(ScriptNameResolver resolver) {
		scriptNameResolvers.add(resolver);
	}
	
	/**
	 * Remove a script name resolver
	 * @param resolver the ScriptNameResolver to remove
	 * @return true if and only if the specified ScriptNameResolver was
	 *         registered before the call to this method
	 */
	public static boolean removeScriptNameResolver(ScriptNameResolver resolver) {
		return scriptNameResolvers.remove(resolver);
	}
	
	private static String resolveScriptName(String scriptName) {
		for (int r = 0; r < scriptNameResolvers.size(); r++) {
			String script = ((ScriptNameResolver) scriptNameResolvers.get(r)).getScriptForName(scriptName);
			if (script != null) return script;
		}
		return null;
	}
	
	/**
	 * Execute a command
	 * @param data the document to apply the script to
	 * @param command the command to execute
	 * @return a status report
	 */
	public static String executeCommand(MutableAnnotation data, String command) throws GScriptException {
		String[] commandTokens = tokenizeCommand(command);
		
		String functionName = commandTokens[0];
		GPathString[] scriptParameters = new GPathString[commandTokens.length - 1];
		for (int p = 0; p < scriptParameters.length; p++)
			scriptParameters[p] = new GPathString(commandTokens[p+1]);
		GScriptFunction function = getFunction(functionName, scriptParameters);
		
		if (function == null) return ("Unknown Function: " + functionName);
		else {
			String[] parameterTypes = function.getParameterTypes();
			GPathObject[] functionParameters = new GPathObject[scriptParameters.length];
			StringVector results = new StringVector();
			results.addContent(executeCommand(data, function, functionParameters, parameterTypes, scriptParameters, 0, new GPathVariableResolver(), null));
			return results.concatStrings("\n");
		}
	}
	
	/**
	 * Execute a script
	 * @param data the document to apply the script to
	 * @param script the script to execute
	 * @return an array holding status reports
	 */
	public static String[] executeScript(MutableAnnotation data, String script) throws GScriptException {
		return executeScript(data, script, GPath.getDummyVariableResolver(), null);
	}
	
	/**
	 * Execute a script
	 * @param data the document to apply the script to
	 * @param script the script to execute
	 * @param bindings existing bindings, e.g. from an enclosing script calling
	 *            the specified one
	 * @param debug a DebugLogger logging how the script is executed
	 * @return an array holding status reports
	 */
	public static String[] executeScript(MutableAnnotation data, String script, GPathVariableResolver bindings, DebugLogger debug) throws GScriptException {
		StringVector lines = new StringVector();
		lines.parseAndAddElements(script, "\n");
		
		//	extract executable lines
		StringVector commands = new StringVector();
		for (int l = 0; l < lines.size(); l++) {
			String line = lines.get(l).trim();
			
			//	it's an executable line
			if ((line.length() != 0) && !line.startsWith(COMMENT_LINE_START)) commands.addElement(line);
		}
		
		//	execute what is executable
		return executeScript(data, commands.toStringArray(), bindings, debug);
	}
	
	/**
	 * Execute a script
	 * @param data the document to apply the script to
	 * @param commands the script commands to execute
	 * @return an array holding status reports
	 */
	public static String[] executeScript(MutableAnnotation data, String[] commands) throws GScriptException {
		return executeScript(data, commands, GPath.getDummyVariableResolver(), null);
	}
	
	/**
	 * Execute a script
	 * @param data the document to apply the script to
	 * @param commands the script commands to execute
	 * @param bindings existing bindings, e.g. from an enclosing script calling
	 *            the specified one
	 * @param debug a DebugLogger logging how the script is executed
	 * @return an array holding status reports
	 */
	public static String[] executeScript(MutableAnnotation data, String[] commands, GPathVariableResolver bindings, DebugLogger debug) throws GScriptException {
		String[][] tokenizedCommands = new String[commands.length][];
		for (int c = 0; c < commands.length; c++)
			tokenizedCommands[c] = tokenizeCommand(commands[c]);
		return executeScript(data, tokenizedCommands, 0, commands.length, bindings, debug);
	}
	
	/**
	 * Execute a portion of a script
	 * @param data the document to apply the function to
	 * @param tokenizedCommands an array holding the tokens of the commands to
	 *            execute
	 * @param cStartIndex the index of the first command to execute in this
	 *            level of recursion
	 * @param cEndIndex the index of the command to stop at in this level of
	 *            recursion
	 * @param globalBindings the current variable bindings
	 * @param debug a DebugLogger logging how the script is executed
	 * @return an array holding status reports
	 */
	public static String[] executeScript(MutableAnnotation data, String[][] tokenizedCommands, int cStartIndex, int cEndIndex, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException {
		
		StringVector results = new StringVector();
		int cIndex = cStartIndex;
		while (cIndex < Math.min(cEndIndex, tokenizedCommands.length)) {
			String[] commandTokens = tokenizedCommands[cIndex];
			
			//	jump blank lines  and comments
			if ((commandTokens.length == 0) || COMMENT_LINE_START.equals(commandTokens[0])) {
				cIndex++;
				continue;
			}
			
			//	get name of what to do
			String functionName = commandTokens[0];
			
			//	clear all variable bindings
			if (CLEAR_VARIABLE_DEFINITIONS_COMMAND.equals(functionName)) {
				if (commandTokens.length == 1) {
					
					//	write debug info
					if (debug != null) {
						String[] vNames = globalBindings.getVariableNames();
						for (int v = 0; v < vNames.length; v++)
							debug.variableAssigned(vNames[v], null);
					}
					
					//	clear bindings & move on to next command
					globalBindings.clear();
					cIndex++;
				}
				else throw new GScriptException((CLEAR_VARIABLE_DEFINITIONS_COMMAND + " requires no parameters."), results.toStringArray());
			}
			
			//	clear one specific variable binding
			else if (CLEAR_VARIABLE_DEFINITION_COMMAND.equals(functionName)) {
				if ((commandTokens.length == 2) && commandTokens[1].startsWith("$")) {
					
					//	write debug info
					if (debug != null) debug.variableAssigned(commandTokens[1], null);
					
					//	clear binding & move on to next command
					globalBindings.removeVariable(commandTokens[1]);
					cIndex++;
				}
				else throw new GScriptException((CLEAR_VARIABLE_DEFINITION_COMMAND + " requires the name of the variable to clear as its only parameter. Variable names have to start with '$'."), results.toStringArray());
			}
			
			//	bind a variable to a (new) value
			else if (DEFINE_VARIABLE_COMMAND.equals(functionName)) {
				
				//	compute value (if necessary) & set binding
				if ((commandTokens.length == 3) && commandTokens[1].startsWith("$")) {
					String value = commandTokens[2];
					
					//	binding to value of annotation set
					if (isAnnotationSetConstructor(value)) {
						
						//	cut annotation set constructor brackets
						value = value.substring(1, (value.length() - 1));
						
						//	create annotation set
						QueriableAnnotation[] annotations = GPath.evaluatePath(data, value, globalBindings);
						GPathAnnotationSet gpas = new GPathAnnotationSet();
						for (int a = 0; a < annotations.length; a++)
							gpas.add(annotations[a]);
						
						//	write debug info
						if (debug != null) debug.expressionEvaluated(commandTokens[2], gpas.asString().value);
						
						//	convert annotation set to literal according to XPath specification
						globalBindings.setVariable(commandTokens[1], gpas);
					}
					
					//	binding to value of expression
					else if (isExpression(value)) {
						GPathObject exRes = GPath.evaluateExpression(value, data, globalBindings);
						globalBindings.setVariable(commandTokens[1], exRes);
						
						//	write debug info
						if (debug != null) debug.expressionEvaluated(commandTokens[2], exRes.asString().value);
					}
					
					//	straight literal
					else globalBindings.setVariable(commandTokens[1], new GPathString(value));
					
					//	write debug info
					if (debug != null) debug.variableAssigned(commandTokens[1], globalBindings.getVariable(commandTokens[1]).asString().value);
					
					//	move on to next command
					cIndex++;
				}
				else throw new GScriptException((DEFINE_VARIABLE_COMMAND + " requires the name and content of the variable to define as its only parameters. Variable names have to start with '$'."), results.toStringArray());
			}
			
			//	start of for loop
			else if (START_FOR_LOOP_COMMAND.equals(functionName)) {
				
				//	compute value (if necessary) & set binding
				if ((commandTokens.length == 3) && commandTokens[1].startsWith("$") && isAnnotationSetConstructor(commandTokens[2])) {
					String pathExpression = commandTokens[2];
					
					//	cut annotation set constructor brackets
					pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
					
					//	create annotation set
					QueriableAnnotation[] annotations = GPath.evaluatePath(data, pathExpression, globalBindings);
					
					//	find end of for loop
					int forEndIndex = (cIndex + 1);
					while (forEndIndex < cEndIndex) {
						String[] forLoopCommandTokens = tokenizedCommands[forEndIndex];
						if ((forLoopCommandTokens.length == 2) && END_FOR_LOOP_COMMAND.equals(forLoopCommandTokens[0]) && commandTokens[1].equals(forLoopCommandTokens[1])) break;
						else forEndIndex++;
					}
					
					//	execute for loop
					for (int a = 0; a < annotations.length; a++) {
						
						//	bind loop variable to current value
						GPathAnnotationSet gpas = new GPathAnnotationSet();
						gpas.add(annotations[a]);
						globalBindings.setVariable(commandTokens[1], gpas);
						
						//	write debug info
						if (debug != null) debug.forLoopVariableAssigned(commandTokens[1], commandTokens[2], annotations[a].getValue());
						
						//	execute commands in loop
						System.out.println("running FOR loop " + (cIndex + 1) + "-" + forEndIndex);
						String[] forLoopResults = executeScript(data, tokenizedCommands, (cIndex + 1), forEndIndex, globalBindings, debug);
						results.addContent(forLoopResults);
						
						//	unbind loop variable
						globalBindings.removeVariable(commandTokens[1]);
						
						//	write debug info
						if (debug != null) debug.forLoopVariableAssigned(commandTokens[1], commandTokens[2], null);
					}
					
					//	jump to first command after for loop
					cIndex = forEndIndex + 1;
				}
				else throw new GScriptException((START_FOR_LOOP_COMMAND + " requires the name of the variable to use and the annotation set to iterate through as its only parameters. Variable names have to start with '$', annotation set constructors are to be enclosed in '{' and '}'."), results.toStringArray());
			}
			
			//	end of for loop, without foor loop open
			else if (END_FOR_LOOP_COMMAND.equals(functionName))
				throw new GScriptException((END_FOR_LOOP_COMMAND + " without " + START_FOR_LOOP_COMMAND + ": '" + concatTokens(commandTokens) + "'"), results.toStringArray());
			
			//	start of if-elseIf-else block
			else if (IF_COMMAND.equals(functionName)) {
				
				if ((commandTokens.length == 2) && isExpression(commandTokens[1])) {
					
					//	collect condition / command pairs
					ArrayList ccPairs = new ArrayList();
					
					//	find end of if-elseIf-else block, and bind commands to conditions
					int ifEndIndex = (cIndex + 1);
					
					//	create pair for if condition
					CcPair currentCcPair = new CcPair(commandTokens[1]);
					ccPairs.add(currentCcPair);
					
					while (ifEndIndex < cEndIndex) {
						String[] ifBlockTokens = tokenizedCommands[ifEndIndex];
						if (ELSE_IF_COMMAND.equals(ifBlockTokens[0])) {
							if (currentCcPair.condition == null) { // pair is from else command, no elseIf may follow
								throw new GScriptException((ELSE_COMMAND + " has to be the last in an if-elseIf-else block, no " + ELSE_IF_COMMAND + " may follow it."), results.toStringArray());
							}
							else if ((ifBlockTokens.length == 2) && isExpression(ifBlockTokens[1])) {
								if (ccPairs.get(ccPairs.size()-1) != currentCcPair)
									ccPairs.add(currentCcPair);
								currentCcPair = new CcPair(ifBlockTokens[1]);
								ifEndIndex++;
							}
							else throw new GScriptException((ELSE_IF_COMMAND + " requires an expression as its only parameter."), results.toStringArray());
						}
						else if (ELSE_COMMAND.equals(ifBlockTokens[0])) {
							if (currentCcPair.condition == null) { // pair is from else command, no other else may follow
								throw new GScriptException(("There can be only one " + ELSE_COMMAND + " in an if-elseIf-else block."), results.toStringArray());
							}
							else if (ifBlockTokens.length == 1) {
								if (ccPairs.get(ccPairs.size()-1) != currentCcPair)
									ccPairs.add(currentCcPair);
								currentCcPair = new CcPair(null);
								ifEndIndex++;
							}
							else throw new GScriptException((ELSE_IF_COMMAND + " requires no parameters."), results.toStringArray());
						}
						else if (currentCcPair.command == null) { // no command since last control statement
							currentCcPair.command = ifBlockTokens;//ifCommand;
							ifEndIndex++;
						}
						else break;
					}
					
					//	add last pair
					if (ccPairs.get(ccPairs.size()-1) != currentCcPair)
						ccPairs.add(currentCcPair);
					
					//	determine the command to execute
					String[] ifCommand = null;
					for (int ic = 0; ic < ccPairs.size(); ic++) {
						CcPair p = ((CcPair) ccPairs.get(ic));
						if (p.condition == null) {
							ifCommand = p.command;
							break;
						}
						else {
							
							//	evaluate expression
							String expression = p.condition.substring(1, (p.condition.length() - 1));
							GPathObject condRes = GPath.evaluateExpression(expression, data, globalBindings);
							
							//	write debug info
							if (debug != null) debug.expressionEvaluated(p.condition, condRes.asString().value);
							
							//	test expression
							if (condRes.asBoolean().value) {
								ifCommand = p.command;
								break; // necessary, for subsequent conditions may also be true
							}
						}
					}
					
					//	execute command
					if (ifCommand != null) {
						
						//	conditional one-line command, recurse
						if (CLEAR_VARIABLE_DEFINITIONS_COMMAND.equals(ifCommand[0]) || CLEAR_VARIABLE_DEFINITION_COMMAND.equals(ifCommand[0]) || DEFINE_VARIABLE_COMMAND.equals(ifCommand[0]) || CALL_COMMAND.equals(ifCommand[0]) || INCLUDE_COMMAND.equals(ifCommand[0])) {
							String[][] ifScript = {ifCommand};
							String[] ifResult = executeScript(data, ifScript, 0, 1, globalBindings, debug);
							results.addContent(ifResult);
						}
						
						//	function invocation
						else {
							
							//	gather parameters
							GPathObject[] scriptParameters = new GPathObject[ifCommand.length - 1];
							for (int p = 0; p < scriptParameters.length; p++)
								scriptParameters[p] = new GPathString(ifCommand[p+1]);
							GScriptFunction function = getFunction(ifCommand[0], scriptParameters);
							
							//	unknown function
							if (function == null) {
								throw new GScriptException(("Unknown Function: '" + ifCommand[0] + "'"), results.toStringArray());
							}
							
							//	write debug info
							if (debug != null) {
								String[] pValues = new String[scriptParameters.length];
								for (int p = 0; p < pValues.length; p++)
									pValues[p] = scriptParameters[p].asString().value;
								debug.functionInvoking(ifCommand[0], pValues);
							}
							
							//	execute function
							String[] parameterTypes = function.getParameterTypes();
							GPathObject[] functionParameters = new GPathObject[scriptParameters.length];
							String[] commandResults = executeCommand(data, function, functionParameters, parameterTypes, scriptParameters, 0, new GPathVariableResolver(globalBindings), debug);
							results.addContent(commandResults);
						}
					}
					
					//	jump to first command after if-elseIf-else block
					cIndex = ifEndIndex;
				}
				else throw new GScriptException((IF_COMMAND + " requires an expression as its only parameter."), results.toStringArray());
			}
			
			//	elseIf command without if-elseIf-else block open
			else if (ELSE_IF_COMMAND.equals(functionName))
				throw new GScriptException((ELSE_IF_COMMAND + " without " + IF_COMMAND + ": '" + concatTokens(commandTokens) + "'"), results.toStringArray());
			
			//	else command without if-elseIf-else block open
			else if (ELSE_COMMAND.equals(functionName))
				throw new GScriptException((ELSE_COMMAND + " without " + IF_COMMAND + ": '" + concatTokens(commandTokens) + "'"), results.toStringArray());
			
			//	call to other script
			else if (CALL_COMMAND.equals(functionName)) {
				if ((commandTokens.length == 2) || ((commandTokens.length == 3) && isAnnotationSetConstructor(commandTokens[2]))) {
					
					//	obtain called script
					String script = resolveScriptName(commandTokens[1]);
					if (script == null)
						throw new GScriptException(("Cannot resolve called script name: '" + commandTokens[1] + "'"), results.toStringArray());
					
					//	call to context
					else if (commandTokens.length == 2) {
						
						//	execute called script & move on to next command
						String[] scriptRes = executeScript(data, script, GPath.getDummyVariableResolver(), debug);
						results.addContent(scriptRes);
						cIndex++;
					}
					
					//	call for annotation set
					else {
						
						//	get annotation set constructor and cut brackets
						String pathExpression = commandTokens[2];
						pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
						
						//	create annotation set & collect IDs
						Annotation[] annotations = GPath.evaluatePath(data, pathExpression, globalBindings);
						HashSet annotationIDs = new HashSet();
						for (int a = 0; a < annotations.length; a++)
							annotationIDs.add(annotations[a].getAnnotationID());
						
						//	execute called script for each selected annotation
						MutableAnnotation[] dataAnnotations = data.getMutableAnnotations();
						for (int a = 0; a < dataAnnotations.length; a++)
							if (annotationIDs.contains(dataAnnotations[a].getAnnotationID())) {
								String[] scriptRes = executeScript(dataAnnotations[a], script, GPath.getDummyVariableResolver(), debug);
								results.addContent(scriptRes);
							}
						
						//	move on to next command
						cIndex++;
					}
					
				}
				else throw new GScriptException((CALL_COMMAND + " requires the name of the script to call as a parameter, plus an optional GPathAnnotationSet to execute the called script on. Annotation set constructors are to be enclosed in '{' and '}'."), results.toStringArray());
			}
			
			//	inclusion of other script
			else if (INCLUDE_COMMAND.equals(functionName)) {
				if ((commandTokens.length == 2) || ((commandTokens.length == 3) && isAnnotationSetConstructor(commandTokens[2]))) {
					
					//	obtain called script
					String script = resolveScriptName(commandTokens[1]);
					if (script == null)
						throw new GScriptException(("Cannot resolve included script name: '" + commandTokens[1] + "'"), results.toStringArray());
					
					//	include to context
					else if (commandTokens.length == 2) {
						
						//	execute called script & move on to next command
						String[] scriptRes = executeScript(data, script, globalBindings, debug);
						results.addContent(scriptRes);
						cIndex++;
					}
					
					//	include for annotation set
					else {
						
						//	get annotation set constructor and cut brackets
						String pathExpression = commandTokens[2];
						pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
						
						//	create annotation set & collect IDs
						Annotation[] annotations = GPath.evaluatePath(data, pathExpression, globalBindings);
						HashSet annotationIDs = new HashSet();
						for (int a = 0; a < annotations.length; a++)
							annotationIDs.add(annotations[a].getAnnotationID());
						
						//	execute called script for each selected annotation
						MutableAnnotation[] dataAnnotations = data.getMutableAnnotations();
						for (int a = 0; a < dataAnnotations.length; a++)
							if (annotationIDs.contains(dataAnnotations[a].getAnnotationID())) {
								String[] scriptRes = executeScript(dataAnnotations[a], script, globalBindings, debug);
								results.addContent(scriptRes);
							}
						
						//	move on to next command
						cIndex++;
					}
					
				}
				else throw new GScriptException((INCLUDE_COMMAND + " requires the name of the script to call as a parameter, plus an optional GPathAnnotationSet to execute the included script on. Annotation set constructors are to be enclosed in '{' and '}'."), results.toStringArray());
			}
			
			//	other command
			else {
				
				//	gather parameters
				GPathObject[] scriptParameters = new GPathObject[commandTokens.length - 1];
				for (int p = 0; p < scriptParameters.length; p++)
					scriptParameters[p] = new GPathString(commandTokens[p+1]);
				GScriptFunction function = getFunction(functionName, scriptParameters);
				
				//	unknown function
				if (function == null)
					throw new GScriptException(("Unknown Function: '" + functionName + "'"), results.toStringArray());
				
				//	write debug info
				if (debug != null) {
					String[] pValues = new String[scriptParameters.length];
					for (int p = 0; p < pValues.length; p++)
						pValues[p] = scriptParameters[p].asString().value;
					debug.functionInvoking(functionName, pValues);
				}
				
				//	execute function
				String[] parameterTypes = function.getParameterTypes();
				GPathObject[] functionParameters = new GPathObject[scriptParameters.length];
				String[] commandResults = executeCommand(data, function, functionParameters, parameterTypes, scriptParameters, 0, new GPathVariableResolver(globalBindings), debug);
				results.addContent(commandResults);
				
				//	move on to next command
				cIndex++;
			}
		}
		
		return results.toStringArray();
	}
	
	/**
	 * Execute a function, iteratively processing annotation sets
	 * @param data the document to apply the function to
	 * @param function the function
	 * @param functionParameters the parameters that will be handed to the
	 *            function
	 * @param parameterTypes the types of the parameters the function
	 *            understands
	 * @param scriptParameters the parameters specified in the command line of
	 *            script
	 * @param pIndex the index of the parameter to process in this recursion
	 *            step
	 * @param inLineBindings the current variable bindings
	 * @param debug a DebugLogger logging how the command is executed
	 * @return an array holding status reports
	 */
	public static String[] executeCommand(MutableAnnotation data, GScriptFunction function, GPathObject[] functionParameters, String[] parameterTypes, GPathObject[] scriptParameters, int pIndex, GPathVariableResolver inLineBindings, DebugLogger debug) throws GScriptException {
		
		//	all parameters inspected
		if (pIndex == functionParameters.length) {
			
			//	write debug info
			if (debug != null) {
				String[] pValues = new String[functionParameters.length];
				for (int p = 0; p < pValues.length; p++)
					pValues[p] = functionParameters[p].asString().value;
				debug.functionInvoking(function.getName(), pValues);
			}
			
			String[] result = new String[1];
			result[0] = function.process(data, functionParameters, inLineBindings);
			
			//	write debug info
			if (debug != null) debug.functionInvoked(function.getName(), result);
			
			//	return result
			return result;
		}
		
		//	inspect parameters, use for loops if annotation set given where value expected 
		else {
			
			StringVector resultCollector = new StringVector();
			
			//	resolve variable
			if (scriptParameters[pIndex].asString().value.startsWith("$")) {
				GPathObject vv = inLineBindings.getVariable(scriptParameters[pIndex].asString().value);
				if (vv == null) throw new VariableNotBoundException("The variable '" + scriptParameters[pIndex].asString().value + "' has been referenced, but is not bound to a value.");
				else scriptParameters[pIndex] = vv;
			}
			
			//	TODO_not hand over expression if not resolvable instead of throwing exception
			//	==> throwing the exception on a plain variable is OK, expressions are handled differently 
			
			//	function wants annotation set for current parameter
			if (GPATH_ANNOTATION_SET_TYPE.equals(parameterTypes[pIndex])) {
				
				//	it's already an annotation set
				if (scriptParameters[pIndex] instanceof GPathAnnotationSet)
					functionParameters[pIndex] = scriptParameters[pIndex];
				
				//	create annotation set, interpret value as GPath expresion
				else {
					GPathAnnotationSet gpas = new GPathAnnotationSet();
					String pathExpression = scriptParameters[pIndex].asString().value;
					
					//	check if value OK
					if (pathExpression.length() != 0) {
						
						//	cut annotation set constructor brackets
						if (isAnnotationSetConstructor(pathExpression))
							pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
						
						//	annotation set constructor coded as expression
						else if (isExpression(pathExpression)) {
							GPathObject exRes = GPath.evaluateExpression(pathExpression, data, inLineBindings);
							pathExpression = exRes.asString().value;
							
							//	write debug info
							if (debug != null) debug.expressionEvaluated(scriptParameters[pIndex].asString().value, pathExpression);
						}
						
						QueriableAnnotation[] annotations = GPath.evaluatePath(data, pathExpression, inLineBindings);
						for (int a = 0; a < annotations.length; a++)
							gpas.add(annotations[a]);
						
					}
					
					//	hand over annotation set as effective parameter
					functionParameters[pIndex] = gpas;
				}
				
				//	proceed to next parameter
				String[] results = executeCommand(data, function, functionParameters, parameterTypes, scriptParameters, (pIndex + 1), inLineBindings, debug);
				resultCollector.addContent(results);
			}
			
			//	function wants value for current parameter
			else {
				GPathAnnotationSet gpas = null;
				String inlineBindingName = ("$" + (pIndex + 1));
				
				//	it's an annotation set
				if (scriptParameters[pIndex] instanceof GPathAnnotationSet)
					gpas = ((GPathAnnotationSet) scriptParameters[pIndex]);
				
				//	check if annotation set constructor
				else {
					String pathExpression = scriptParameters[pIndex].asString().value;
					if (isAnnotationSetConstructor(pathExpression)) {
						
						//	cut annotation set constructor brackets
						pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
						
						//	create annotation set
						QueriableAnnotation[] annotations = GPath.evaluatePath(data, pathExpression, inLineBindings);
						gpas = new GPathAnnotationSet();
						for (int a = 0; a < annotations.length; a++)
							gpas.add(annotations[a]);
					}
				}
				
				//	regular value
				if (gpas == null) {
					
					//	execute expression if given
					GPathObject gpo = null;
					if (isExpression(scriptParameters[pIndex].asString().value)) {
						
						//	cut expression brackets
						String pathExpression = scriptParameters[pIndex].asString().value;
						
						//	evaluate expression, deferring evaluation if it fails here due to non-existing inline binding 
						try {
							gpo = GPath.evaluateExpression(pathExpression, data, inLineBindings);
						}
						catch (VariableNotBoundException vnbe) {
							if (!vnbe.getMessage().matches(".*\\$[1-9].*"))
								throw vnbe;
						}
					}
					
					//	set inline binding
					inLineBindings.setVariable(inlineBindingName, ((gpo == null) ? scriptParameters[pIndex] : gpo));
					
					//	write debug info
					if (debug != null) {
						String value = ((gpo == null) ? scriptParameters[pIndex] : gpo).asString().value;
						debug.variableAssigned(inlineBindingName, value);
					}
					
					//	proceed to next parameter
					functionParameters[pIndex] = ((gpo == null) ? scriptParameters[pIndex] : gpo);
					String[] results = executeCommand(data, function, functionParameters, parameterTypes, scriptParameters, (pIndex + 1), inLineBindings, debug);
					resultCollector.addContent(results);
					
					//	remove inline binding
					inLineBindings.removeVariable(inlineBindingName);
					
					//	write debug info
					if (debug != null) debug.variableAssigned(inlineBindingName, null);
				}
				
				//	annotation set, iterate
				else {
					
					for (int a = 0; a < gpas.size(); a++) {
						
						//	get current value
						Annotation annotation = gpas.get(a);
						GPathString effectiveParameter = new GPathString(annotation.getValue());
						
						//	set inline binding
						inLineBindings.setVariable(inlineBindingName, effectiveParameter);
						
						//	write debug info
						if (debug != null) debug.variableAssigned(inlineBindingName, effectiveParameter.value);
						
						//	proceed to next parameter
						functionParameters[pIndex] = effectiveParameter;
						String[] results = executeCommand(data, function, functionParameters, parameterTypes, scriptParameters, (pIndex + 1), inLineBindings, debug);
						resultCollector.addContent(results);
						
						//	remove inline binding
						inLineBindings.removeVariable(inlineBindingName);
						
						//	write debug info
						if (debug != null) debug.variableAssigned(inlineBindingName, null);
					}
				}
			}
			
			return resultCollector.toStringArray();
		}
	}
	
	private static boolean isAnnotationSetConstructor(String token) {
		return ((token.length() != 0) && (token.charAt(0) == ANNOTATION_SET_CONSTRUCTOR_START) && token.charAt(token.length() - 1) == ANNOTATION_SET_CONSTRUCTOR_END);
	}
	
	/**
	 * Test if a string is an expression, i.e., if it starts with '(' and ends
	 * with ')'.
	 * @param token the string to test
	 * @return true if the argument string is an expression, false otherwise
	 */
	public static boolean isExpression(String token) {
		return ((token.length() != 0) && (token.charAt(0) == EXPRESSION_START) && token.charAt(token.length() - 1) == EXPRESSION_END);
	}
	
	/**
	 * Concatenate the Strings in an array for output purposes
	 * @param tokens the String to concatenate
	 * @return the space-separated concatenation of the Strings in the specified
	 *         array
	 */
	private static String concatTokens(String[] tokens) {
		if (tokens.length == 0) return "";
		StringBuffer sb = new StringBuffer(tokens[0]);
		for (int t = 1; t < tokens.length; t++)
			sb.append(" " + tokens[t]);
		return sb.toString();
	}
	
	/**
	 * Check if a script is valid
	 * @param script the script to check
	 * @param verbose the StringVector to add report messages to (specifying
	 *            null will result in no messages)
	 * @return a String describing the validation error, on null, if the script
	 *         is valid
	 */
	public static String validateScript(String script, StringVector verbose) throws GScriptException {
		
		if (verbose != null)
			verbose.addElement("\n ----- Validating Script ... -----");
		
		String[][] scriptTokens;
		
		try {
			scriptTokens = parseScript(script);
			if (verbose != null) {
				verbose.addElement("==> The script parses successfully");
				for (int st = 0; st < scriptTokens.length; st++) {
					String[] commandTokens = scriptTokens[st];
					for (int ct = 0; ct < commandTokens.length; ct++)
						verbose.addElement(((ct == 0) ? "" : "  ") + (st + 1) + "." + (ct + 1) + ((ct == 0) ? "    " : "  ") + commandTokens[ct]);
				}
			}
		}
		catch (GScriptException gse) {
			if (verbose != null)
				verbose.addElement("The script does not parse successfully: " + gse.getMessage());
			return gse.getMessage();
		}
		
		try {
			compileScript(scriptTokens);
			if (verbose != null)
				verbose.addElement("==> The script is valid");
		}
		catch (GScriptException gse) {
			if (verbose != null)
				verbose.addElement("The script is not valid: " + gse.getMessage());
			return gse.getMessage();
		}
		
		return null;
	}
	
	/**
	 * Compile a GamtaScript. Executing the returned GScript on a given
	 * MutableAnnotation has the same effect as invoking the
	 * executeStript(MutableAnnotation, String) method, but is faster for
	 * complex scripts because the script is compiled.
	 * @param script the script to compile
	 * @return a GScript object created from the specified string representation
	 * @throws GScriptException
	 */
	public static GScript compile(String script) throws GScriptException {
		return compileScript(parseScript(script));
	}
	
	private static String[][] parseScript(String script) throws GScriptException {
		StringVector scriptLines = new StringVector();
		scriptLines.parseAndAddElements(script, "\n");
		
		//	extract executable lines
		StringVector commands = new StringVector();
		for (int l = 0; l < scriptLines.size(); l++) {
			String scriptLine = scriptLines.get(l).trim();
			
			//	it's an executable line
			if ((scriptLine.length() != 0) && !scriptLine.startsWith(COMMENT_LINE_START))
				commands.addElement(scriptLine);
		}
		
		//	parse executable lines
		String[][] tokenizedCommands = new String[commands.size()][];
		for (int c = 0; c < commands.size(); c++)
			tokenizedCommands[c] = tokenizeCommand(commands.get(c));
		
		//	build AST
		return tokenizedCommands;
	}
	
	private static GScript compileScript(String[][] tokenizedCommands) throws GScriptException {
		Map localScriptResolver = new LinkedHashMap();
		Set toResolve = new HashSet();
		
		//	extract sub scripts and compile sub scripts
		int commandIndex = 0;
		int mainStartStart = 0;
		while (commandIndex < tokenizedCommands.length) {
			String[] commandTokens = tokenizedCommands[commandIndex];
			
			//	get name of what to do
			String functionName = ((commandTokens.length == 0) ? null : commandTokens[0]);
			
			//	jump blank lines  and comments
			if ((functionName == null) || COMMENT_LINE_START.equals(functionName))
				commandIndex++;
			
			//	start of sub script
			else if (START_SUB_SCRIPT_COMMAND.equals(functionName)) {
				
				//	compute value (if necessary)
				if ((commandTokens.length == 2)) {
					
					//	check for duplicate definitions
					if (localScriptResolver.containsKey(commandTokens[1]))
						throw new GScriptException("The sub script '" + commandTokens[1] + "' is already defined.");
					
					//	find end of for sub script
					int subEndIndex = (commandIndex + 1);
					while (subEndIndex < tokenizedCommands.length) {
						String[] subScriptCommandTokens = tokenizedCommands[subEndIndex];
						if ((subScriptCommandTokens.length == 1) && END_SUB_SCRIPT_COMMAND.equals(subScriptCommandTokens[0])) break;
						else subEndIndex++;
					}
					
					//	store sub script
					localScriptResolver.put(commandTokens[1], new GScript(compileScriptBody(tokenizedCommands, (commandIndex + 1), subEndIndex, toResolve)));
					
					//	jump to first command after sub script
					commandIndex = subEndIndex + 1;
					mainStartStart = subEndIndex + 1;
				}
				else throw new GScriptException(START_SUB_SCRIPT_COMMAND + " requires the name of the sub script as its only parameter. The name must not include whitespaces.");
			}
			
			//	end of for loop, without foor loop open
			else if (END_SUB_SCRIPT_COMMAND.equals(functionName))
				throw new GScriptException(END_SUB_SCRIPT_COMMAND + " without " + START_SUB_SCRIPT_COMMAND + ": '" + concatTokens(commandTokens) + "'");
			
			//	other command, ignore now
			else commandIndex++;
		}
		
		//	compile main script
		GScript main = new GScript(compileScriptBody(tokenizedCommands, mainStartStart, tokenizedCommands.length, toResolve), localScriptResolver);
		
		//	link script invocations
		for (Iterator rit = toResolve.iterator(); rit.hasNext();) {
			GScriptInvocation gsi = ((GScriptInvocation) rit.next());
			gsi.resolve(localScriptResolver);
		}
		
		//	return compiled script
		return main;
	}
	
	private static GScriptCommand[] compileScriptBody(String[][] tokenizedCommands, int cStartIndex, int cEndIndex, Set toResolve) throws GScriptException {
		ArrayList commands = new ArrayList();
		
		int cIndex = cStartIndex;
		while (cIndex < Math.min(cEndIndex, tokenizedCommands.length)) {
			String[] commandTokens = tokenizedCommands[cIndex];
			
			//	get name of what to do
			String functionName = ((commandTokens.length == 0) ? null : commandTokens[0]);
			
			//	jump blank lines  and comments
			if ((functionName == null) || COMMENT_LINE_START.equals(functionName))
				cIndex++;
			
			//	clear all variable bindings
			else if (CLEAR_VARIABLE_DEFINITIONS_COMMAND.equals(functionName)) {
				if (commandTokens.length == 1) {
					
					//	clear bindings & move on to next command
					commands.add(new VariablesClearing());
					cIndex++;
				}
				else throw new GScriptException(CLEAR_VARIABLE_DEFINITIONS_COMMAND + " requires no parameters.");
			}
			
			//	clear one specific variable binding
			else if (CLEAR_VARIABLE_DEFINITION_COMMAND.equals(functionName)) {
				if ((commandTokens.length == 2) && commandTokens[1].startsWith("$")) {
					
					//	clear binding & move on to next command
					commands.add(new VariableClearing(commandTokens[1]));
					cIndex++;
				}
				else throw new GScriptException(CLEAR_VARIABLE_DEFINITION_COMMAND + " requires the name of the variable to clear as its only parameter. Variable names have to start with '$'.");
			}
			
			//	bind a variable to a (new) value
			else if (DEFINE_VARIABLE_COMMAND.equals(functionName)) {
				
				//	compute value (if necessary) & set binding
				if ((commandTokens.length == 3) && commandTokens[1].startsWith("$")) {
					
					//	add definition & move on to next command
					commands.add(new VariableDefinition(commandTokens[1], commandTokens[2]));
					cIndex++;
				}
				else throw new GScriptException(DEFINE_VARIABLE_COMMAND + " requires the name and content of the variable to define as its only parameters. Variable names have to start with '$'.");
			}
			
			//	start of for loop
			else if (START_FOR_LOOP_COMMAND.equals(functionName)) {
				
				//	compute value (if necessary) & set binding
				if ((commandTokens.length == 3) && commandTokens[1].startsWith("$") && isAnnotationSetConstructor(commandTokens[2])) {
					String pathExpression = commandTokens[2];
					
					//	cut annotation set constructor brackets
					pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
					
					//	find end of for loop
					int forEndIndex = (cIndex + 1);
					while (forEndIndex < cEndIndex) {
						String[] forLoopCommandTokens = tokenizedCommands[forEndIndex];
						if ((forLoopCommandTokens.length == 2) && END_FOR_LOOP_COMMAND.equals(forLoopCommandTokens[0]) && commandTokens[1].equals(forLoopCommandTokens[1]))
							break;
						else forEndIndex++;
					}
					
					//	store for loop
					commands.add(new ForLoop(commandTokens[1], GPathParser.parsePath(pathExpression), compileScriptBody(tokenizedCommands, (cIndex + 1), forEndIndex, toResolve)));
					
					//	jump to first command after for loop
					cIndex = forEndIndex + 1;
				}
				else throw new GScriptException(START_FOR_LOOP_COMMAND + " requires the name of the variable to use and the annotation set to iterate through as its only parameters. Variable names have to start with '$', annotation set constructors are to be enclosed in '{' and '}'.");
			}
			
			//	end of for loop, without foor loop open
			else if (END_FOR_LOOP_COMMAND.equals(functionName))
				throw new GScriptException(END_FOR_LOOP_COMMAND + " without " + START_FOR_LOOP_COMMAND + ": '" + concatTokens(commandTokens) + "'");
			
			//	start of if-elseIf-else block
			else if (IF_COMMAND.equals(functionName)) {
				
				if ((commandTokens.length == 2) && isExpression(commandTokens[1])) {
					
					//	collect condition / command pairs
					ArrayList ifBlockParts = new ArrayList();
					
					//	find end of if-elseIf-else block, and bind commands to conditions
					int ifEndIndex = (cIndex + 1);
					
					//	create pair for if condition
					String condition = commandTokens[1];
					boolean lastWasCondition = true;
					boolean gotElse = false;
					
					while (ifEndIndex < cEndIndex) {
						String[] ifBlockTokens = tokenizedCommands[ifEndIndex];
						if (ELSE_IF_COMMAND.equals(ifBlockTokens[0])) {
							if (gotElse) { // else command was already there, no elseIf may follow
								throw new GScriptException(ELSE_COMMAND + " has to be the last in an if-elseIf-else block, no " + ELSE_IF_COMMAND + " may follow it.");
							}
							else if (lastWasCondition) {
								throw new GScriptException(IF_COMMAND + ", " + ELSE_IF_COMMAND + ", and " + ELSE_IF_COMMAND + " have to be followed by a command.");
							}
							else if ((ifBlockTokens.length == 2) && isExpression(ifBlockTokens[1])) {
								condition = ifBlockTokens[1];
								lastWasCondition = true;
								ifEndIndex++;
							}
							else throw new GScriptException(ELSE_IF_COMMAND + " requires an expression as its only parameter.");
						}
						else if (ELSE_COMMAND.equals(ifBlockTokens[0])) {
							if (gotElse) { // else command was already there, no other else may follow
								throw new GScriptException("There can be only one " + ELSE_COMMAND + " in an if-elseIf-else block.");
							}
							else if (lastWasCondition) {
								throw new GScriptException(IF_COMMAND + ", " + ELSE_IF_COMMAND + ", and " + ELSE_IF_COMMAND + " have to be followed by a command.");
							}
							else if (ifBlockTokens.length == 1) {
								condition = null;
								lastWasCondition = true;
								gotElse = true;
								ifEndIndex++;
							}
							else throw new GScriptException(ELSE_COMMAND + " requires no parameters.");
						}
						else if (lastWasCondition) { // no command since last control statement
							ifBlockParts.add(new IfBlockPart(((condition == null) ? null : GPathParser.parseExpression(condition.substring(1, (condition.length() - 1)))), parseCommand(ifBlockTokens, toResolve)));
							lastWasCondition = false;
							ifEndIndex++;
						}
						else break;
					}
					
					//	store if block
					commands.add(new IfBlock((IfBlockPart[]) ifBlockParts.toArray(new IfBlockPart[ifBlockParts.size()])));
					
					//	jump to first command after if-elseIf-else block
					cIndex = ifEndIndex;
				}
				else throw new GScriptException(IF_COMMAND + " requires an expression as its only parameter.");
			}
			
			//	elseIf command without if-elseIf-else block open
			else if (ELSE_IF_COMMAND.equals(functionName))
				throw new GScriptException(ELSE_IF_COMMAND + " without " + IF_COMMAND + ": '" + concatTokens(commandTokens) + "'");
			
			//	else command without if-elseIf-else block open
			else if (ELSE_COMMAND.equals(functionName))
				throw new GScriptException(ELSE_COMMAND + " without " + IF_COMMAND + ": '" + concatTokens(commandTokens) + "'");
			
			//	other command
			else {
				commands.add(parseCommand(commandTokens, toResolve));
				cIndex++;
			}
		}
		
		return ((GScriptCommand[]) commands.toArray(new GScriptCommand[commands.size()]));
	}
	
	private static GScriptCommand parseCommand(String[] commandTokens, Set toResolve) throws GScriptException {
		
		//	call to other script
		if (CALL_COMMAND.equals(commandTokens[0])) {
			if ((commandTokens.length == 2) || ((commandTokens.length == 3) && (isAnnotationSetConstructor(commandTokens[2]) || ('$' == commandTokens[2].charAt(0))))) {
				
				//	include to context
				if (commandTokens.length == 2)
					
					//	store called script & move on to next command
					return new GScriptCall(commandTokens[1], toResolve);
				
				//	include for variable
				else if ('$' == commandTokens[2].charAt(0))
					
					//	store called script
					return new GScriptCall(commandTokens[1], commandTokens[2], toResolve);
				
				//	include for annotation set
				else {
					
					//	get annotation set constructor and cut brackets
					String pathExpression = commandTokens[2];
					pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
					
					//	store called script
					return new GScriptCall(commandTokens[1], GPathParser.parsePath(pathExpression), toResolve);
				}
			}
			else throw new GScriptException(CALL_COMMAND + " requires the name of the script to call as a parameter, plus an optional GPathAnnotationSet to execute the called script on. Annotation set constructors are to be enclosed in '{' and '}'.");
		}
		
		//	inclusion of other script
		else if (INCLUDE_COMMAND.equals(commandTokens[0])) {
			if ((commandTokens.length == 2) || ((commandTokens.length == 3) && (isAnnotationSetConstructor(commandTokens[2]) || ('$' == commandTokens[2].charAt(0))))) {
				
				//	include to context
				if (commandTokens.length == 2)
					
					//	store called script
					return new GScriptInclusion(commandTokens[1], toResolve);
				
				//	include for variable
				else if ('$' == commandTokens[2].charAt(0))
					
					//	store called script
					return new GScriptInclusion(commandTokens[1], commandTokens[2], toResolve);
				
				//	include for annotation set
				else {
					
					//	get annotation set constructor and cut brackets
					String pathExpression = commandTokens[2];
					pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
					
					//	store called script
					return new GScriptInclusion(commandTokens[1], GPathParser.parsePath(pathExpression), toResolve);
				}
			}
			else throw new GScriptException(INCLUDE_COMMAND + " requires the name of the script to call as a parameter, plus an optional GPathAnnotationSet to execute the included script on. Annotation set constructors are to be enclosed in '{' and '}'.");
		}
		
		//	clear all variable bindings
		else if (CLEAR_VARIABLE_DEFINITIONS_COMMAND.equals(commandTokens[0])) {
			if (commandTokens.length == 1) {
				
				//	clear bindings & move on to next command
				 return new VariablesClearing();
			}
			else throw new GScriptException(CLEAR_VARIABLE_DEFINITIONS_COMMAND + " requires no parameters.");
		}
		
		//	clear one specific variable binding
		else if (CLEAR_VARIABLE_DEFINITION_COMMAND.equals(commandTokens[0])) {
			if ((commandTokens.length == 2) && commandTokens[1].startsWith("$")) {
				
				//	clear binding & move on to next command
				return new VariableClearing(commandTokens[1]);
			}
			else throw new GScriptException(CLEAR_VARIABLE_DEFINITION_COMMAND + " requires the name of the variable to clear as its only parameter. Variable names have to start with '$'.");
		}
		
		//	bind a variable to a (new) value
		else if (DEFINE_VARIABLE_COMMAND.equals(commandTokens[0])) {
			
			//	compute value (if necessary) & set binding
			if ((commandTokens.length == 3) && commandTokens[1].startsWith("$")) {
				
				//	add definition & move on to next command
				return new VariableDefinition(commandTokens[1], commandTokens[2]);
			}
			else throw new GScriptException(DEFINE_VARIABLE_COMMAND + " requires the name and content of the variable to define as its only parameters. Variable names have to start with '$'.");
		}
		
		//	other command
		else {
			
			//	gather parameters
			GPathObject[] parameterDefinitions = new GPathObject[commandTokens.length - 1];
			for (int p = 0; p < parameterDefinitions.length; p++)
				parameterDefinitions[p] = new GPathString(commandTokens[p+1]);
			GScriptFunction function = getFunction(commandTokens[0], parameterDefinitions);
			
			//	unknown function
			if (function == null)
				throw new GScriptException("Unknown Function: '" + commandTokens[0] + "'");
			
			//	execute function
			return new FunctionInvocation(function, parameterDefinitions);
		}
	}
	
	private final GScriptCommand[] body;
	private final Map subScriptsByName = new LinkedHashMap();
	
	/**
	 * Constructor to be used in compiler method
	 * @param body the commands included in the script
	 */
	private GScript(GScriptCommand[] body) {
		this(body, null);
	}
	
	private GScript(GScriptCommand[] body, Map subScriptsByName) {
		this.body = body;
		if (subScriptsByName != null)
			this.subScriptsByName.putAll(subScriptsByName);
	}
	
	public String toString() {
		return this.toString("");
	}
	
	String toString(String indent) {
		StringBuffer string = new StringBuffer();
		
		for (Iterator sit = this.subScriptsByName.keySet().iterator(); sit.hasNext();) {
			String subScriptName = ((String) sit.next());
			GScript subScript = ((GScript) this.subScriptsByName.get(subScriptName));
			string.append(((string.length() == 0) ? "" : "\n") + indent + START_SUB_SCRIPT_COMMAND + " " + subScriptName);
			string.append("\n" + subScript.toString(indent + "  "));
			string.append("\n" + indent + END_SUB_SCRIPT_COMMAND);
			string.append("\n");
		}
		
		for (int b = 0; b < this.body.length; b++)
			string.append((((b + this.subScriptsByName.size()) == 0) ? "" : "\n") + this.body[b].toString(indent));
		
		return string.toString();
	}
	
	/**
	 * Execute the script on a mutable annotation.
	 * @param data the mutable annotation to process
	 * @throws GScriptException
	 */
	public String[] execute(MutableAnnotation data) throws GScriptException {
		return this.execute(data, GPath.getDummyVariableResolver(), null);
	}
	
	/**
	 * Execute the script on a mutable annotation.
	 * @param data the mutable annotation to process
	 * @param debug a DebugLogger logging how the script is executed
	 * @throws GScriptException
	 */
	public String[] execute(MutableAnnotation data, DebugLogger debug) throws GScriptException {
		return this.execute(data, GPath.getDummyVariableResolver(), debug);
	}
	
	private String[] execute(MutableAnnotation data, GPathVariableResolver bindings, DebugLogger debug) throws GScriptException {
		StringVector results = new StringVector();
		
		//	execute commands in loop
		for (int b = 0; b < this.body.length; b++) {
			String[] commandResults = this.body[b].execute(data, bindings, debug);
			results.addContent(commandResults);
		}
		
		//	return log
		return results.toStringArray();
	}
	
	/**
	 * Abstract super class for all GScript AST elements
	 * 
	 * @author sautter
	 */
	private static abstract class GScriptCommand {
		final String command; // the command string
		GScriptCommand(String command) {
			this.command = command;
		}
		abstract String[] execute(MutableAnnotation data, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException;
		public String toString() {
			return this.toString("");
		}
		abstract String toString(String indent);
	}
	
	/**
	 * A definition of a variable
	 * 
	 * @author sautter
	 */
	private static class VariableDefinition extends GScriptCommand {
		final String variable;
		final String definition;
		final GPathString literal;
		final GPath annotationSetDefinition;
		final GPathExpression expression;
		VariableDefinition(String variable, String definition) {
			super(DEFINE_VARIABLE_COMMAND);
			this.variable = variable;
			this.definition = definition;
			
			//	binding to value of annotation set
			if (isAnnotationSetConstructor(definition)) {
				
				//	cut annotation set constructor brackets
				definition = definition.substring(1, (definition.length() - 1));
				
				//	initialize fields
				this.literal = null;
				this.annotationSetDefinition = GPathParser.parsePath(definition);
				this.expression = null;
			}
			
			//	binding to value of expression
			else if (isExpression(definition)) {
				
				//	initialize fields
				this.literal = null;
				this.annotationSetDefinition = null;
				this.expression = GPathParser.parseExpression(definition);
			}
			
			//	straight literal
			else {
				
				//	initialize fields
				this.literal = new GPathString(definition);
				this.annotationSetDefinition = null;
				this.expression = null;
			}
		}
		String[] execute(MutableAnnotation data, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException {
			
			//	binding to value of annotation set
			if (this.annotationSetDefinition != null) {
				
				//	create annotation set
				QueriableAnnotation[] annotations = GPath.evaluatePath(data, this.annotationSetDefinition, globalBindings);
				GPathAnnotationSet gpas = new GPathAnnotationSet();
				for (int a = 0; a < annotations.length; a++)
					gpas.add(annotations[a]);
				
				//	write debug info
				if (debug != null) debug.expressionEvaluated(this.definition, gpas.asString().value);
				
				//	convert annotation set to literal according to XPath specification
				globalBindings.setVariable(this.variable, gpas);
			}
			
			//	binding to value of expression
			else if (this.expression != null) {
				GPathObject exRes = GPath.evaluateExpression(this.expression, data, globalBindings);
				globalBindings.setVariable(this.variable, exRes);
				
				//	write debug info
				if (debug != null) debug.expressionEvaluated(this.definition, exRes.asString().value);
			}
			
			//	straight literal
			else globalBindings.setVariable(this.variable, this.literal);
			
			//	write debug info
			if (debug != null) debug.variableAssigned(this.variable, globalBindings.getVariable(this.variable).asString().value);
			
			//	by default
			return new String[0];
		}
		String toString(String indent) {
			return (indent + this.command + " " + this.variable + " " + this.definition);
		}
	}
	
	/**
	 * A clearing of a variable
	 * 
	 * @author sautter
	 */
	private static class VariableClearing extends GScriptCommand {
		final String variable;
		VariableClearing(String variable) {
			super(CLEAR_VARIABLE_DEFINITION_COMMAND);
			this.variable = variable;
		}
		String[] execute(MutableAnnotation data, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException {
			
			//	write debug info
			if (debug != null) debug.variableAssigned(this.variable, null);
			
			//	clear binding
			globalBindings.removeVariable(this.variable);
			
			//	by default
			return new String[0];
		}
		String toString(String indent) {
			return (indent + this.command + " " + this.variable);
		}
	}
	
	/**
	 * A clearing of all variables
	 * 
	 * @author sautter
	 */
	private static class VariablesClearing extends GScriptCommand {
		VariablesClearing() {
			super(CLEAR_VARIABLE_DEFINITIONS_COMMAND);
		}
		String[] execute(MutableAnnotation data, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException {
			
			//	write debug info
			if (debug != null) {
				String[] vNames = globalBindings.getVariableNames();
				for (int v = 0; v < vNames.length; v++)
					debug.variableAssigned(vNames[v], null);
			}
			
			//	clear bindings
			globalBindings.clear();
			
			//	by default
			return new String[0];
		}
		String toString(String indent) {
			return (indent + this.command);
		}
	}
	
	/**
	 * A for loop and its enclosed statements
	 * 
	 * @author sautter
	 */
	private static class ForLoop extends GScriptCommand {
		final String variable;
		final GPath annotationSetDefinition;
		final GScriptCommand[] body;
		ForLoop(String variable, GPath annotationSetDefinition, GScriptCommand[] body) {
			super(START_FOR_LOOP_COMMAND);
			this.variable = variable;
			this.annotationSetDefinition = annotationSetDefinition;
			this.body = body;
		}
		String[] execute(MutableAnnotation data, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException {
			StringVector results = new StringVector();
			
			//	create annotation set
			QueriableAnnotation[] annotations = GPath.evaluatePath(data, this.annotationSetDefinition, globalBindings);
			
			//	execute for loop
			for (int a = 0; a < annotations.length; a++) {
				
				//	bind loop variable to current value
				GPathAnnotationSet gpas = new GPathAnnotationSet();
				gpas.add(annotations[a]);
				globalBindings.setVariable(this.variable, gpas);
				
				//	write debug info
				if (debug != null) debug.forLoopVariableAssigned(this.variable, this.annotationSetDefinition.toString(), annotations[a].getValue());
				
				//	execute commands in loop
				for (int b = 0; b < this.body.length; b++) {
					String[] commandResults = this.body[b].execute(data, globalBindings, debug);
					results.addContent(commandResults);
				}
				
				//	unbind loop variable
				globalBindings.removeVariable(this.variable);
				
				//	write debug info
				if (debug != null) debug.forLoopVariableAssigned(this.variable, this.annotationSetDefinition.toString(), null);
			}
			
			//	return log
			return results.toStringArray();
		}
		String toString(String indent) {
			StringBuffer string = new StringBuffer(indent + START_FOR_LOOP_COMMAND + " " + this.variable + " " + ANNOTATION_SET_CONSTRUCTOR_START + this.annotationSetDefinition.toString() + ANNOTATION_SET_CONSTRUCTOR_END);
			for (int b = 0; b < this.body.length; b++)
				string.append("\n" + this.body[b].toString(indent + "  "));
			string.append("\n" + indent + END_FOR_LOOP_COMMAND + " " + this.variable);
			return string.toString();
		}
	}
	
	/**
	 * An if-block and its enclosed pairs of conditions and statements
	 * 
	 * @author sautter
	 */
	private static class IfBlock extends GScriptCommand {
		final IfBlockPart[] body;
		IfBlock(IfBlockPart[] body) {
			super(IF_COMMAND);
			this.body = body;
		}
		String[] execute(MutableAnnotation data, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException {
			
			//	test parts one by one
			for (int b = 0; b < this.body.length; b++) {
				
				//	test condition
				boolean conditionFulfilled = true;
				if (this.body[b].condition != null) {
					conditionFulfilled = GPath.evaluateExpression(this.body[b].condition, data, globalBindings).asBoolean().value;
					if (debug != null)
						debug.expressionEvaluated(this.body[b].condition.toString(), ("" + conditionFulfilled));
				}
				
				//	on match, execute command & return its result
				if (conditionFulfilled)
					return this.body[b].command.execute(data, globalBindings, debug);
			}
			
			//	by defaut
			return new String[0];
		}
		String toString(String indent) {
			StringBuffer string = new StringBuffer();
			for (int b = 0; b < this.body.length; b++) {
				string.append(((b == 0) ? "" : "\n") + indent + ((this.body[b].condition == null) ? ELSE_COMMAND : (((b == 0) ? IF_COMMAND : ELSE_IF_COMMAND) + " " + EXPRESSION_START + this.body[b].condition.toString() + EXPRESSION_END)));
				string.append("\n" + this.body[b].command.toString(indent + "  "));
			}
			return string.toString();
		}
	}
	
	/**
	 * A pair of a condition and the assigned statement in if-elseIf-else blocks
	 * 
	 * @author sautter
	 */
	private static class IfBlockPart {
		final GPathExpression condition;
		final GScriptCommand command;
		IfBlockPart(GPathExpression condition, GScriptCommand command) {
			this.condition = condition;
			this.command = command;
		}
	}
	
	/**
	 * A function invocation
	 * 
	 * @author sautter
	 */
	private static class FunctionInvocation extends GScriptCommand {
		final GScriptFunction function;
		final String[] parameterTypes;
		final GPathObject[] parameterDefinitions;
		FunctionInvocation(GScriptFunction function, GPathObject[] parameterDefinitions) {
			super(function.getName());
			this.function = function;
			this.parameterTypes = this.function.getParameterTypes();
			this.parameterDefinitions = parameterDefinitions;
		}
		String[] execute(MutableAnnotation data, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException {
			GPathObject[] resolvedParameterDefinitions = new GPathObject[this.parameterDefinitions.length];
			System.arraycopy(this.parameterDefinitions, 0, resolvedParameterDefinitions, 0, resolvedParameterDefinitions.length);
			GPathObject[] parameterValues = new GPathObject[this.parameterDefinitions.length];
			return this.execute(data, resolvedParameterDefinitions, parameterValues, 0, new GPathVariableResolver(globalBindings), debug);
		}
		
		private String[] execute(MutableAnnotation data, GPathObject[] resolvedParameterDefinitions, GPathObject[] parameterValues, int pIndex, GPathVariableResolver inLineBindings, DebugLogger debug) throws GScriptException {
			
			//	all parameters inspected
			if (pIndex == parameterValues.length) {
				
				//	write debug info
				if (debug != null) {
					String[] pValues = new String[parameterValues.length];
					for (int p = 0; p < pValues.length; p++)
						pValues[p] = parameterValues[p].asString().value;
					debug.functionInvoking(this.function.getName(), pValues);
				}
				
				String[] result = new String[1];
				result[0] = this.function.process(data, parameterValues, inLineBindings);
				
				//	write debug info
				if (debug != null) debug.functionInvoked(this.function.getName(), result);
				
				//	return result
				return result;
			}
			
			//	inspect parameters, use for loops if annotation set given where value expected 
			else {
				StringVector resultCollector = new StringVector();
				
				//	resolve variable
				if (resolvedParameterDefinitions[pIndex].asString().value.startsWith("$")) {
					GPathObject vv = inLineBindings.getVariable(resolvedParameterDefinitions[pIndex].asString().value);
					if (vv == null) throw new VariableNotBoundException("The variable '" + resolvedParameterDefinitions[pIndex].asString().value + "' has been referenced, but is not bound to a value.");
					else resolvedParameterDefinitions[pIndex] = vv;
				}
				
				//	TODO_not hand over expression if not resolvable instead of throwing exception
				//	==> throwing the exception on a plain variable is OK, expressions are handled differently 
				
				//	function wants annotation set for current parameter
				if (GPATH_ANNOTATION_SET_TYPE.equals(this.parameterTypes[pIndex])) {
					
					//	it's already an annotation set
					if (resolvedParameterDefinitions[pIndex] instanceof GPathAnnotationSet)
						parameterValues[pIndex] = resolvedParameterDefinitions[pIndex];
					
					//	create annotation set, interpret value as GPath expression
					else {
						GPathAnnotationSet gpas = new GPathAnnotationSet();
						String pathExpression = resolvedParameterDefinitions[pIndex].asString().value;
						
						//	check if value OK
						if (pathExpression.length() != 0) {
							
							//	cut annotation set constructor brackets
							if (isAnnotationSetConstructor(pathExpression))
								pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
							
							//	annotation set constructor coded as expression
							else if (isExpression(pathExpression)) {
								GPathObject exRes = GPath.evaluateExpression(pathExpression, data, inLineBindings);
								pathExpression = exRes.asString().value;
								
								//	write debug info
								if (debug != null) debug.expressionEvaluated(resolvedParameterDefinitions[pIndex].asString().value, pathExpression);
							}
							
							QueriableAnnotation[] annotations = GPath.evaluatePath(data, pathExpression, inLineBindings);
							for (int a = 0; a < annotations.length; a++)
								gpas.add(annotations[a]);
						}
						
						//	hand over annotation set as effective parameter
						parameterValues[pIndex] = gpas;
					}
					
					//	proceed to next parameter
					String[] results = this.execute(data, resolvedParameterDefinitions, parameterValues, (pIndex + 1), inLineBindings, debug);
					resultCollector.addContent(results);
				}
				
				//	function wants value for current parameter
				else {
					GPathAnnotationSet gpas = null;
					String inlineBindingName = ("$" + (pIndex + 1));
					
					//	it's an annotation set
					if (resolvedParameterDefinitions[pIndex] instanceof GPathAnnotationSet)
						gpas = ((GPathAnnotationSet) resolvedParameterDefinitions[pIndex]);
					
					//	check if annotation set constructor
					else {
						String pathExpression = resolvedParameterDefinitions[pIndex].asString().value;
						if (isAnnotationSetConstructor(pathExpression)) {
							
							//	cut annotation set constructor brackets
							pathExpression = pathExpression.substring(1, (pathExpression.length() - 1));
							
							//	create annotation set
							QueriableAnnotation[] annotations = GPath.evaluatePath(data, pathExpression, inLineBindings);
							gpas = new GPathAnnotationSet();
							for (int a = 0; a < annotations.length; a++)
								gpas.add(annotations[a]);
						}
					}
					
					//	regular value
					if (gpas == null) {
						
						//	execute expression if given
						GPathObject gpo = null;
						if (isExpression(resolvedParameterDefinitions[pIndex].asString().value)) {
							
							//	cut expression brackets
							String pathExpression = resolvedParameterDefinitions[pIndex].asString().value;
							
							//	evaluate expression
							gpo = GPath.evaluateExpression(pathExpression, data, inLineBindings);
						}
						
						//	set inline binding
						inLineBindings.setVariable(inlineBindingName, ((gpo == null) ? resolvedParameterDefinitions[pIndex] : gpo));
						
						//	write debug info
						if (debug != null) {
							String value = ((gpo == null) ? resolvedParameterDefinitions[pIndex] : gpo).asString().value;
							debug.variableAssigned(inlineBindingName, value);
						}
						
						//	proceed to next parameter
						parameterValues[pIndex] = ((gpo == null) ? resolvedParameterDefinitions[pIndex] : gpo);
						String[] results = this.execute(data, resolvedParameterDefinitions, parameterValues, (pIndex + 1), inLineBindings, debug);
						resultCollector.addContent(results);
						
						//	remove inline binding
						inLineBindings.removeVariable(inlineBindingName);
						
						//	write debug info
						if (debug != null) debug.variableAssigned(inlineBindingName, null);
					}
					
					//	annotation set, iterate
					else {
						
						for (int a = 0; a < gpas.size(); a++) {
							
							//	get current value
							Annotation annotation = gpas.get(a);
							GPathString parameterValue = new GPathString(annotation.getValue());
							
							//	set inline binding
							inLineBindings.setVariable(inlineBindingName, parameterValue);
							
							//	write debug info
							if (debug != null) debug.variableAssigned(inlineBindingName, parameterValue.value);
							
							//	proceed to next parameter
							parameterValues[pIndex] = parameterValue;
							String[] results = this.execute(data, resolvedParameterDefinitions, parameterValues, (pIndex + 1), inLineBindings, debug);
							resultCollector.addContent(results);
							
							//	remove inline binding
							inLineBindings.removeVariable(inlineBindingName);
							
							//	write debug info
							if (debug != null) debug.variableAssigned(inlineBindingName, null);
						}
					}
				}
				
				//	return log
				return resultCollector.toStringArray();
			}
		}
		String toString(String indent) {
			StringBuffer string = new StringBuffer(indent + this.command);
			for (int p = 0; p < this.parameterDefinitions.length; p++) {
				final String quoter;
				if (this.parameterDefinitions[p] instanceof GPathString) {
					String value = this.parameterDefinitions[p].asString().value;
					if (value.matches("\\$?+[a-zA-Z0-9]++"))
						quoter = "";
					else if (isAnnotationSetConstructor(value) || isExpression(value))
						quoter = "";
					else if (value.indexOf("'") == -1)
						quoter = "'";
					else quoter = "\"";
				}
				else quoter = "";
				string.append(" " + quoter + this.parameterDefinitions[p].asString().value + quoter);
			}
			return string.toString();
		}
	}
	
	/**
	 * An inclusion of anoter GScript
	 * 
	 * @author sautter
	 */
	private static abstract class GScriptInvocation extends GScriptCommand {
		final String scriptName;
		final String variable;
		final GPath annotationSetDefinition;
		private GScript script;
		GScriptInvocation(String command, String scriptName, String variable, GPath annotationSetDefinition, Set toResolve) {
			super(command);
			this.scriptName = scriptName;
			this.variable = variable;
			this.annotationSetDefinition = annotationSetDefinition;
			toResolve.add(this);
		}
		
		void resolve(Map localScriptNameResolver) throws GScriptException {
			
			//	obtain local sub script
			this.script = ((GScript) localScriptNameResolver.get(this.scriptName));
			
			//	not found, check global resolvers
			if (this.script == null) {
				String scriptString = resolveScriptName(this.scriptName);
				if (scriptString != null)
					this.script = compileScript(parseScript(scriptString));
			}
			
			//	not found
			if (this.script == null)
				throw new GScriptException("Cannot resolve included script name: '" + this.scriptName + "'");
		}
		
		String[] execute(MutableAnnotation data, GPathVariableResolver globalBindings, DebugLogger debug) throws GScriptException {
			
			//	not found
			if (this.script == null)
				throw new GScriptException("Cannot resolve included script name: '" + this.scriptName + "'");
			
			StringVector results = new StringVector();
			
			//	include to variable
			if (this.variable != null) {
				
				//	resolve variable
				GPathObject gpo = globalBindings.getVariable(this.variable);
				if (gpo == null) throw new VariableNotBoundException("The variable '" + this.variable + "' has been referenced, but is not bound to a value.");
				else if (gpo instanceof GPathAnnotationSet) {
					
					//	create annotation set & collect IDs
					GPathAnnotationSet gpas = ((GPathAnnotationSet) gpo);
					HashSet annotationIDs = new HashSet();
					for (int a = 0; a < gpas.size(); a++)
						annotationIDs.add(gpas.get(a).getAnnotationID());
					
					//	execute called script for each selected annotation
					MutableAnnotation[] dataAnnotations = data.getMutableAnnotations();
					for (int a = 0; a < dataAnnotations.length; a++)
						if (annotationIDs.contains(dataAnnotations[a].getAnnotationID())) {
							String[] scriptResults = this.script.execute(dataAnnotations[a], this.getResolver(globalBindings), debug);
							results.addContent(scriptResults);
						}
				}
				else throw new GScriptException("In order to be used in including other GScripts, the variable '" + this.variable + "' must be bound to an annotation set.");
			}
			
			//	include for annotation set
			else if (this.annotationSetDefinition != null) {
				
				//	create annotation set & collect IDs
				Annotation[] annotations = GPath.evaluatePath(data, this.annotationSetDefinition, globalBindings);
				HashSet annotationIDs = new HashSet();
				for (int a = 0; a < annotations.length; a++)
					annotationIDs.add(annotations[a].getAnnotationID());
				
				//	execute called script for each selected annotation
				MutableAnnotation[] dataAnnotations = data.getMutableAnnotations();
				for (int a = 0; a < dataAnnotations.length; a++)
					if (annotationIDs.contains(dataAnnotations[a].getAnnotationID())) {
						String[] scriptResults = this.script.execute(dataAnnotations[a], this.getResolver(globalBindings), debug);
						results.addContent(scriptResults);
					}
			}
			
			//	include to context
			else {
				
				//	execute called script & move on to next command
				String[] scriptResults = this.script.execute(data, this.getResolver(globalBindings), debug);
				results.addContent(scriptResults);
			}
			
			//	return log
			return results.toStringArray();
		}
		abstract GPathVariableResolver getResolver(GPathVariableResolver globalBindings);
		String toString(String indent) {
			StringBuffer string = new StringBuffer(indent + this.command + " " + this.scriptName);
			if (this.variable != null)
				string.append(" " + this.variable);
			else if (this.annotationSetDefinition != null)
				string.append(" " + this.annotationSetDefinition.toString());
			return string.toString();
		}
	}
	
	/**
	 * An inclusion of anoter GScript
	 * 
	 * @author sautter
	 */
	private static class GScriptInclusion extends GScriptInvocation {
		GScriptInclusion(String scriptName, Set toResolve) {
			this(scriptName, null, null, toResolve);
		}
		GScriptInclusion(String scriptName, String variable, Set toResolve) {
			this(scriptName, variable, null, toResolve);
		}
		GScriptInclusion(String scriptName, GPath annotationSetDefinition, Set toResolve) {
			this(scriptName, null, annotationSetDefinition, toResolve);
		}
		private GScriptInclusion(String scriptName, String variable, GPath annotationSetDefinition, Set toResolve) {
			super(INCLUDE_COMMAND, scriptName, variable, annotationSetDefinition, toResolve);
		}
		GPathVariableResolver getResolver(GPathVariableResolver globalBindings) {
			return new GPathVariableResolver(globalBindings);
		}
	}
	
	/**
	 * A call to anoter GScript
	 * 
	 * @author sautter
	 */
	private static class GScriptCall extends GScriptInvocation {
		GScriptCall(String scriptName, Set toResolve) {
			this(scriptName, null, null, toResolve);
		}
		GScriptCall(String scriptName, String variable, Set toResolve) {
			this(scriptName, variable, null, toResolve);
		}
		GScriptCall(String scriptName, GPath annotationSetDefinition, Set toResolve) {
			this(scriptName, null, annotationSetDefinition, toResolve);
		}
		private GScriptCall(String scriptName, String variable, GPath annotationSetDefinition, Set toResolve) {
			super(CALL_COMMAND, scriptName, variable, annotationSetDefinition, toResolve);
		}
		GPathVariableResolver getResolver(GPathVariableResolver globalBindings) {
			return GPath.getDummyVariableResolver();
		}
	}
	
	/**
	 * A pair of a condition and the assigned statement in if-elseIf-else blocks
	 * 
	 * @author sautter
	 */
	private static class CcPair {
		final String condition;
		String[] command;
		CcPair(String condition) {
			this.condition = condition;
		}
	}
	
	/**
	 * Specify an inplementation of this interface as the last argument to the
	 * executeScript(DocumentPart, String[], int, int, Properties, DebugLogger)
	 * method to receive messages on how the specified script is executed.
	 * 
	 * @author sautter
	 */
	public static interface DebugLogger {
		
		/**	receive notification that a variable has been assigned
		 * @param	name	the name of the variable
		 * @param	value	the new value of the variable
		 */
		public abstract void variableAssigned(String name, String value);
		
		/**	receive notification that the running variable of a for loop has been assigned
		 * @param	name				the name of the running variable
		 * @param	forLoopExpression	the expression defining the annotation set the for loop runs through
		 * @param	value				the new value of the running variable
		 */
		public abstract void forLoopVariableAssigned(String name, String forLoopExpression, String value); 
		
		/**	receive notification that a function is about to be invoked
		 * @param	name		the name of the function
		 * @param	parameters	the values of the function parameters
		 */
		public abstract void functionInvoking(String name, String[] parameters);
		
		/**	receive notification that a function invokation has returned
		 * @param	name		the name of the function
		 * @param	results		the output of the function
		 */
		public abstract void functionInvoked(String name, String[] results);
		
		/**	receive notification that an expression has been evaluated
		 * @param	expression	the expression
		 * @param	result		the result of the evaluation
		 */
		public abstract void expressionEvaluated(String expression, String result);
	}
	
	/**
	 * Convenience implementation of the DebugLogger interface, whose methods do
	 * not do anything by default. Users are free to overwrite them as needed.
	 * This is comparable to the ...Adapter implementations of the ...Listener
	 * interfaces in Swing.
	 * 
	 * @author sautter
	 */
	public static class VoidDebugLogger implements DebugLogger {
		
		/** @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#expressionEvaluated(java.lang.String, java.lang.String)
		 */
		public void expressionEvaluated(String expression, String result) {}
		
		/** @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#forLoopVariableAssigned(java.lang.String, java.lang.String, java.lang.String)
		 */
		public void forLoopVariableAssigned(String name, String forLoopExpression, String value) {}
		
		/** @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#functionInvoking(java.lang.String, java.lang.String[])
		 */
		public void functionInvoking(String name, String[] parameters) {}
		
		/** @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#functionInvoked(java.lang.String, java.lang.String[])
		 */
		public void functionInvoked(String name, String[] results) {}
		
		/** @see de.uka.ipd.idaho.gamta.util.gScript.GScript.DebugLogger#variableAssigned(java.lang.String, java.lang.String)
		 */
		public void variableAssigned(String name, String value) {}
	}
//	
//	public static void main(String[] args) throws Exception {
//		String docId = "8AD0DAEF2180649D27DBA7CE08E4FF93"; // Anochetus boltoni
//		Gamta.setAnnotationNestingOrder(DocumentRoot.DOCUMENT_TYPE + " " + DocumentRoot.SECTION_TYPE + " " + DocumentRoot.SUB_SECTION_TYPE + " " + DocumentRoot.SUB_SUB_SECTION_TYPE + " " + LiteratureConstants.FOOTNOTE_TYPE + " " + LiteratureConstants.CAPTION_TYPE + " " + DocumentRoot.PARAGRAPH_TYPE + " " + DocumentRoot.SENTENCE_TYPE);
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new URL("http://plazi.cs.umb.edu/GgServer/xslt/" + docId).openStream(), "UTF-8"));
//		
//		String script = 
//			"def $pnr 0\n" +
//			"def $pid -1\n" +
//			"\n" +
//			"for $pbt {//pageStartToken}\n" +
//			"  def $t {$pbt/@pageNumber}\n" +
//			"  if ($t and not($t = ''))\n" +
//			"    def $pnr $t\n" +
//			"  else\n" +
//			"    def $pnr ($pnr + 1)\n" +
//			"  setAttribute {$pbt} 'pageNumber' $pnr\n" +
//			"end $pbt";
////		GScript gs = compile(script);
////		System.out.println(gs.toString());
//		executeScript(doc, script, GPath.getDummyVariableResolver(), new DebugLogger() {
//			public void expressionEvaluated(String expression, String result) {
//				System.out.println("expression '" + expression + "' evaluated to '" + result + "'");
//			}
//			public void variableAssigned(String name, String value) {
//				System.out.println("variable '" + name + "' set to '" + value + "'");
//			}
//			public void forLoopVariableAssigned(String name, String forLoopExpression, String value) {
//				System.out.println("variable '" + name + "' in loop over '" + forLoopExpression + "' set to '" + value + "'");
//			}
//			public void functionInvoking(String name, String[] parameters) {
//				System.out.println("Invoking function '" + name + "'");
//				for (int p = 0; p < parameters.length; p++)
//					System.out.println(" - " + parameters[p]);
//			}
//			public void functionInvoked(String name, String[] results) {
//				System.out.println("function '" + name + "' returned:");
//				for (int p = 0; p < results.length; p++)
//					System.out.println(" - " + results[p]);
//			}
//		});
////		gs.execute(doc, new DebugLogger() {
////			public void expressionEvaluated(String expression, String result) {
////				System.out.println("expression '" + expression + "' evaluated to '" + result + "'");
////			}
////			public void variableAssigned(String name, String value) {
////				System.out.println("variable '" + name + "' set to '" + value + "'");
////			}
////			public void forLoopVariableAssigned(String name, String forLoopExpression, String value) {
////				System.out.println("variable '" + name + "' in loop over '" + forLoopExpression + "' set to '" + value + "'");
////			}
////			public void functionInvoking(String name, String[] parameters) {
////				System.out.println("Invoking function '" + name + "'");
////				for (int p = 0; p < parameters.length; p++)
////					System.out.println(" - " + parameters[p]);
////			}
////			public void functionInvoked(String name, String[] results) {
////				System.out.println("function '" + name + "' returned:");
////				for (int p = 0; p < results.length; p++)
////					System.out.println(" - " + results[p]);
////			}
////		});
//		/*
//def $pnr 0
//def $pid -1
//
//for $pbt {//pageBreakToken}
//  if ($pbt/@pageNumber and not($pbt/@pageNumber = ''))
//    def $pnr ($pbt/@pageNumber)
//  elseIf (true)
//    def $pnr 'fuck off'
//  setAttribute $pbt 'pageNumber' $pnr
//end $pbt
//		 */
////		Writer out = new OutputStreamWriter(System.out);
////		BufferedWriter bw = new BufferedWriter(out);
////		AnnotationUtils.writeXML(doc, bw);
////		bw.flush();
//	}
}
