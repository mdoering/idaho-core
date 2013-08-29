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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathSyntaxException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathNumber;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;

/**
 * @author sautter
 *
 * TODO document this class
 */
public class GPathParser {
	
	private static final char NULLCHAR = '\u0000';
	private static final boolean DEBUG = false;
	
	/**
	 * Parse a GPath query.
	 * @param path the String representation of the GPath to parse
	 * @return a GPath object representing the parsed GPath
	 */
	public static GPath parsePath(String path) {
		
		//	check path
		if ((path == null) || (path.length() == 0)) return new GPath();
		
		//	do cache lookup
		GPath gp = ((GPath) pathCache.get(path));
		
		//	cache miss, parse path string
		if (gp == null) {
			
			//	tokenize & validate path
			String[] pathTokens = tokenize(path);
			String error = validatePath(pathTokens);
			if (error != null)
				throw new GPathSyntaxException("Invalid GPath expression: " + error);
			
			//	parse path
			gp = parsePath(pathTokens);
			
			//	put path in cache
			pathCache.put(path, gp);
		}
		
		//	return path
		return gp;
	}
	private static HashMap pathCache = new HashMap();
	
	private static GPath parsePath(String[] pathTokens) {
		
		ArrayList steps = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		
		String token;
		for (int t = 0; t < pathTokens.length; t++) {
			token = pathTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token))
					openBrackets.pop();
					
				else if ("(".equals(token))
					openBrackets.push(")");
					
				else if ("[".equals(token))
					openBrackets.push("]");
			}
				
			//	end of step
			else if ("/".equals(token)) {
				if (DEBUG) System.out.print("Got step:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				steps.add(collector.toArray(new String[collector.size()]));
				collector.clear();
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				openBrackets.push("]");
			}
			
			//	other char
			else collector.add(token);
		}
		
		if (!collector.isEmpty()) {
			if (DEBUG) System.out.print("Got final step:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			steps.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] stepStrings = ((String[][]) steps.toArray(new String[steps.size()][]));
		steps.clear();
		
		GPath pg = new GPath();
		pg.steps = new GPathStep[stepStrings.length];
		
		for (int s = 0; s < stepStrings.length; s++)
			pg.steps[s] = parseStep(stepStrings[s]);
		
		return pg;
	}
	
	private static GPathStep parseStep(String[] stepTokens) {
		
		//	check parameter
		if (stepTokens == null) return null;
		
		//	abbreviated step
		if (stepTokens.length == 0) {
			GPathStep gps = new GPathStep();
			gps.axis = "descendant-or-self";
			return gps;
		}
		else if (".".equals(stepTokens[0])) {
			GPathStep gps = new GPathStep();
			gps.axis = "self";
			return gps;
		}
		else if ("..".equals(stepTokens[0])) {
			GPathStep gps = new GPathStep();
			gps.axis = "parent";
			return gps;
		}
		
		//	implicit axis
		if (stepTokens.length == 1) {
			if ("#".equals(stepTokens[0])) {
				GPathStep gps = new GPathStep();
				gps.axis = "token";
				return gps;
			}
			else if ("@".equals(stepTokens[0])) {
				GPathStep gps = new GPathStep();
				gps.axis = "attribute";
				return gps;
			}
			else {
				GPathStep gps = new GPathStep();
				gps.annotationTest = stepTokens[0];
				return gps;
			}
		}
		
		GPathStep gps = new GPathStep();
		int index = 0;
		
		//	read axis and annotation test
		if ("::".equals(stepTokens[1])) {
			if ("@".equals(stepTokens[0]))
				gps.axis = "attribute";
			else if ("#".equals(stepTokens[0]))
				gps.axis = "token";
			else gps.axis = stepTokens[0];
			index = 2;
		}
		else if ("@".equals(stepTokens[0])) {
			gps.axis = "attribute";
			index = 1;
		}
		else if ("#".equals(stepTokens[0])) {
			gps.axis = "token";
			index = 1;
		}
		
		//	read annotation test if given
		if (!"[".equals(stepTokens[index]) && !"(".equals(stepTokens[index])) {
			gps.annotationTest = stepTokens[index];
			index++;
		}
		if (index < stepTokens.length) {
			if ("(".equals(stepTokens[index])) {
				gps.annotationTest += "(";
				index ++;
				if (")".equals(stepTokens[index])) {
					gps.annotationTest += ")";
					index ++;
				}
				else {
					gps.annotationTest += stepTokens[index];
					index ++;
					gps.annotationTest += ")";
					index ++;
				}
			}
		}
		
		//	read eventual predicates
		if (index < stepTokens.length) {
			
			ArrayList predicates = new ArrayList();
			Stack openBrackets = new Stack();
			ArrayList collector = new ArrayList();
			
			while (index < stepTokens.length) {
				String token = stepTokens[index];
				
				//	nested part
				if (!openBrackets.isEmpty()) {
					
					//	process inner parts of predicates, etc.
					if (openBrackets.peek().equals(token)) {
						openBrackets.pop();
						
						if (openBrackets.isEmpty() && "]".equals(token)) {
							if (DEBUG) System.out.print("Got predicate:");
							if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
							if (DEBUG) System.out.println();
							predicates.add(collector.toArray(new String[collector.size()]));
							collector.clear();
						}
						else collector.add(token);
					}
					else if ("(".equals(token)) {
						collector.add(token);
						openBrackets.push(")");
					}
					else if ("[".equals(token)) {
						collector.add(token);
						openBrackets.push("]");
					}
					else collector.add(token);
				}
				
				//	start of annotation test brackets
				else if ("(".equals(token)) {
					collector.add(token);
					openBrackets.push(")");
				}
				
				//	start of predicate
				else if ("[".equals(token)) {
					//collector.add(token);
					openBrackets.push("]");
				}
				
				//	other char
				else collector.add(token);
				
				index++;
			}
			
			if (!collector.isEmpty()) {
				if (DEBUG) System.out.print("Got final predicate:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				predicates.add(collector.toArray(new String[collector.size()]));
			}
			
			String[][] predicateStrings = ((String[][]) predicates.toArray(new String[predicates.size()][]));
			
			gps.predicates = new GPathPredicate[predicateStrings.length];
			for (int p = 0; p < predicateStrings.length; p++) {
				gps.predicates[p] = new GPathPredicate();
				gps.predicates[p].expression = parseOrExpression(predicateStrings[p]);
			}
		}
		
		return gps;
	}
	
	/**
	 * Parse a GPath expression.
	 * @param expression the String representation of the GPath expression to
	 *            parse
	 * @return a GPathExpression object representing the parsed GPath
	 */
	public static GPathExpression parseExpression(String expression) {
		
		//	check parameter
		if (expression == null) return null;
		
		//	do cache lookup
		GPathExpression gpe = ((GPathExpression) expressionCache.get(expression));
		
		//	cache miss, parse expression string
		if (gpe == null) {
			
			//	tokenize expression & validate expression
			String[] expressionTokens = tokenize(expression);
			String error = validatePath(expressionTokens);
			if (error != null)
				throw new GPathSyntaxException("Invalid GPath expression: " + error + "\n  " + expression);
			
			//	parse expression
			gpe = parseOrExpression(expressionTokens);
			
			//	cache expression
			expressionCache.put(expression, gpe);
		}
		
		//	parse expression & return result
		return gpe;
	}
	private static HashMap expressionCache = new HashMap();
	
	private static GPathExpression parseOrExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		ArrayList parts = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		boolean canBeOperator = false;
		boolean lastWasOperator = false;
		
		for (int t = 0; t < expressionTokens.length; t++) {
			String token = expressionTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token)) {
					openBrackets.pop();
					canBeOperator = true;
				}
				else if ("(".equals(token)) {
					canBeOperator = false;
					openBrackets.push(")");
				}
				else if ("[".equals(token)) {
					canBeOperator = false;
					openBrackets.push("]");
				}
			}
			
			//	start of annotation test brackets
			else if ("or".equals(token) && canBeOperator && !lastWasOperator) {
				if (DEBUG) System.out.print("Got OR expression part:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				canBeOperator = false;
				lastWasOperator = true;
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				canBeOperator = false;
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				canBeOperator = false;
				openBrackets.push("]");
			}
			
			//	other char
			else {
				collector.add(token);
				lastWasOperator = canBeOperator && canBeOperator(token);
				canBeOperator = canPreceedOperator(token);
			}
		}
		
		if (!collector.isEmpty()) {
			if (DEBUG) System.out.print("Got final OR expression part:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0)
			return null;
		if (partStrings.length == 1)
			return parseAndExpression(partStrings[0]);
		
		GPathBinaryExpression gpe = new GPathBinaryExpression();
		GPathBinaryExpression tempGpe = gpe;
		int i = partStrings.length - 1;
		while (i > 1) {
			tempGpe.rightExpression = parseAndExpression(partStrings[i]);
			tempGpe.operator = "or";
			tempGpe.leftExpression = new GPathBinaryExpression();
			tempGpe = ((GPathBinaryExpression) tempGpe.leftExpression);
			i--;
		}
		
		if (partStrings.length < 2) {
			StringBuffer sb = new StringBuffer();
			for (int t = 0; t < expressionTokens.length; t++)
				sb.append(" " + expressionTokens[t]);
			throw new GPathSyntaxException("Incomplete expression:" + sb.toString());
		}
		
		tempGpe.rightExpression = parseAndExpression(partStrings[1]);
		tempGpe.operator = "or";
		tempGpe.leftExpression = parseAndExpression(partStrings[0]);
		return gpe;
	}
	
	private static GPathExpression parseAndExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		ArrayList parts = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		boolean canBeOperator = false;
		boolean lastWasOperator = false;
		
		for (int t = 0; t < expressionTokens.length; t++) {
			String token = expressionTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token)) {
					openBrackets.pop();
					canBeOperator = true;
				}
				else if ("(".equals(token)) {
					canBeOperator = false;
					openBrackets.push(")");
				}
				else if ("[".equals(token)) {
					canBeOperator = false;
					openBrackets.push("]");
				}
			}
			
			//	start of annotation test brackets
			else if ("and".equals(token) && canBeOperator && !lastWasOperator) {
				if (DEBUG) System.out.print("Got AND expression part:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				canBeOperator = false;
				lastWasOperator = true;
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				canBeOperator = false;
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				canBeOperator = false;
				openBrackets.push("]");
			}
			
			//	other char
			else {
				collector.add(token);
				lastWasOperator = canBeOperator && canBeOperator(token);
				canBeOperator = canPreceedOperator(token);
			}
		}
		
		if (!collector.isEmpty()) {
			if (DEBUG) System.out.print("Got final AND expression part:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0)
			return null;
		if (partStrings.length == 1)
			return parseEqualityExpression(partStrings[0]);
		
		GPathBinaryExpression gpe = new GPathBinaryExpression();
		GPathBinaryExpression tempGpe = gpe;
		int i = partStrings.length - 1;
		while (i > 1) {
			tempGpe.rightExpression = parseEqualityExpression(partStrings[i]);
			tempGpe.operator = "and";
			tempGpe.leftExpression = new GPathBinaryExpression();
			tempGpe = ((GPathBinaryExpression) tempGpe.leftExpression);
			i--;
		}
		
		if (partStrings.length < 2) {
			StringBuffer sb = new StringBuffer();
			for (int t = 0; t < expressionTokens.length; t++)
				sb.append(" " + expressionTokens[t]);
			throw new GPathSyntaxException("Incomplete expression:" + sb.toString());
		}
		
		tempGpe.rightExpression = parseEqualityExpression(partStrings[1]);
		tempGpe.operator = "and";
		tempGpe.leftExpression = parseEqualityExpression(partStrings[0]);
		return gpe;
	}
	
	private static GPathExpression parseEqualityExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		ArrayList parts = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		
		for (int t = 0; t < expressionTokens.length; t++) {
			String token = expressionTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token))
					openBrackets.pop();
					
				else if ("(".equals(token))
					openBrackets.push(")");
					
				else if ("[".equals(token))
					openBrackets.push("]");
			}
			
			//	start of annotation test brackets
			else if ("!=".equals(token) || "=".equals(token)) {
				if (DEBUG) System.out.print("Got equality expression part:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				openBrackets.push("]");
			}
			
			//	other char
			else collector.add(token);
		}
		
		if (!collector.isEmpty()) {
			if (DEBUG) System.out.print("Got final equality expression part:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0)
			return null;
		if (partStrings.length == 1)
			return parseRelationalExpression(partStrings[0]);
		
		GPathBinaryExpression gpe = new GPathBinaryExpression();
		GPathBinaryExpression tempGpe = gpe;
		int i = partStrings.length - 1;
		while (i > 2) {
			tempGpe.rightExpression = parseRelationalExpression(partStrings[i]);
			tempGpe.operator = partStrings[i - 1][0];
			tempGpe.leftExpression = new GPathBinaryExpression();
			tempGpe = ((GPathBinaryExpression) tempGpe.leftExpression);
			i -= 2;
		}
		
		if (partStrings.length < 3) {
			StringBuffer sb = new StringBuffer();
			for (int t = 0; t < expressionTokens.length; t++)
				sb.append(" " + expressionTokens[t]);
			throw new GPathSyntaxException("Incomplete expression:" + sb.toString());
		}
		
		tempGpe.rightExpression = parseRelationalExpression(partStrings[2]);
		tempGpe.operator = partStrings[1][0];
		tempGpe.leftExpression = parseRelationalExpression(partStrings[0]);
		return gpe;
	}
	
	private static GPathExpression parseRelationalExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		ArrayList parts = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		
		for (int t = 0; t < expressionTokens.length; t++) {
			String token = expressionTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token))
					openBrackets.pop();
					
				else if ("(".equals(token))
					openBrackets.push(")");
					
				else if ("[".equals(token))
					openBrackets.push("]");
			}
			
			//	start of annotation test brackets
			else if ("<=".equals(token) || "<".equals(token) || ">".equals(token) || ">=".equals(token)) {
				if (DEBUG) System.out.print("Got relational expression part:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				openBrackets.push("]");
			}
			
			//	other char
			else collector.add(token);
		}
		
		if (!collector.isEmpty()) {
			if (DEBUG) System.out.print("Got final relational expression part:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0)
			return null;
		if (partStrings.length == 1)
			return parseAdditiveExpression(partStrings[0]);
		
		GPathBinaryExpression gpe = new GPathBinaryExpression();
		GPathBinaryExpression tempGpe = gpe;
		int i = partStrings.length - 1;
		while (i > 2) {
			tempGpe.rightExpression = parseAdditiveExpression(partStrings[i]);
			tempGpe.operator = partStrings[i - 1][0];
			tempGpe.leftExpression = new GPathBinaryExpression();
			tempGpe = ((GPathBinaryExpression) tempGpe.leftExpression);
			i -= 2;
		}
		
		if (partStrings.length < 3) {
			StringBuffer sb = new StringBuffer();
			for (int t = 0; t < expressionTokens.length; t++)
				sb.append(" " + expressionTokens[t]);
			throw new GPathSyntaxException("Incomplete expression:" + sb.toString());
		}
		
		tempGpe.rightExpression = parseAdditiveExpression(partStrings[2]);
		tempGpe.operator = partStrings[1][0];
		tempGpe.leftExpression = parseAdditiveExpression(partStrings[0]);
		return gpe;
	}
	
	private static GPathExpression parseAdditiveExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		ArrayList parts = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		boolean canBeOperator = false;
		
		for (int t = 0; t < expressionTokens.length; t++) {
			String token = expressionTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token)) {
					openBrackets.pop();
					canBeOperator = true;
				}
				else if ("(".equals(token)) {
					openBrackets.push(")");
					canBeOperator = false;
				}
				else if ("[".equals(token)) {
					openBrackets.push("]");
					canBeOperator = false;
				}
			}
			
			//	start of annotation test brackets
			else if ("+".equals(token)) {
				if (DEBUG) System.out.print("Got additive expression part:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
				canBeOperator = false;
			}
			
			//	start of annotation test brackets
			else if ("-".equals(token) && canBeOperator) {
				if (DEBUG) System.out.print("Got additive expression part:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
				canBeOperator = false;
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				canBeOperator = false;
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				canBeOperator = false;
				openBrackets.push("]");
			}
			
			//	other char
			else {
				collector.add(token);
				canBeOperator = canPreceedOperator(token);
			}
		}
		
		if (!collector.isEmpty()) {
			if (DEBUG) System.out.print("Got final additive expression part:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0)
			return null;
		if (partStrings.length == 1)
			return parseMultiplicativeExpression(partStrings[0]);
		
		GPathBinaryExpression gpe = new GPathBinaryExpression();
		GPathBinaryExpression tempGpe = gpe;
		int i = partStrings.length - 1;
		while (i > 2) {
			tempGpe.rightExpression = parseMultiplicativeExpression(partStrings[i]);
			tempGpe.operator = partStrings[i - 1][0];
			tempGpe.leftExpression = new GPathBinaryExpression();
			tempGpe = ((GPathBinaryExpression) tempGpe.leftExpression);
			i -= 2;
		}
		
		if (partStrings.length < 3) {
			StringBuffer sb = new StringBuffer();
			for (int t = 0; t < expressionTokens.length; t++)
				sb.append(" " + expressionTokens[t]);
			throw new GPathSyntaxException("Incomplete expression:" + sb.toString());
		}
		
		tempGpe.rightExpression = parseMultiplicativeExpression(partStrings[2]);
		tempGpe.operator = partStrings[1][0];
		tempGpe.leftExpression = parseMultiplicativeExpression(partStrings[0]);
		return gpe;
	}
	
	private static GPathExpression parseMultiplicativeExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		ArrayList parts = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		boolean canBeOperator = false;
		boolean lastWasOperator = false;
		
		for (int t = 0; t < expressionTokens.length; t++) {
			String token = expressionTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token)) {
					openBrackets.pop();
					canBeOperator = true;
				}
				else if ("(".equals(token)) {
					openBrackets.push(")");
					canBeOperator = false;
				}
				else if ("[".equals(token)) {
					openBrackets.push("]");
					canBeOperator = false;
				}
			}
			
			//	start of annotation test brackets
			else if (("*".equals(token) || "div".equals(token) || "mod".equals(token)) && canBeOperator && !lastWasOperator) {
				if (DEBUG) System.out.print("Got multiplicative expression part:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
				canBeOperator = false;
				lastWasOperator = true;
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				canBeOperator = false;
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				canBeOperator = false;
				openBrackets.push("]");
			}
			
			//	other char
			else {
				collector.add(token);
				lastWasOperator = canBeOperator && canBeOperator(token);
				canBeOperator = canPreceedOperator(token);
			}
		}
		
		if (!collector.isEmpty()) {
			if (DEBUG) System.out.print("Got final multiplicative expression part:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0)
			return null;
		if (partStrings.length == 1)
			return parseUnaryExpression(partStrings[0]);
		
		GPathBinaryExpression gpe = new GPathBinaryExpression();
		GPathBinaryExpression tempGpe = gpe;
		int i = partStrings.length - 1;
		while (i > 2) {
			tempGpe.rightExpression = parseUnaryExpression(partStrings[i]);
			tempGpe.operator = partStrings[i - 1][0];
			tempGpe.leftExpression = new GPathBinaryExpression();
			tempGpe = ((GPathBinaryExpression) tempGpe.leftExpression);
			i -= 2;
		}
		
		if (partStrings.length < 3) {
			StringBuffer sb = new StringBuffer();
			for (int t = 0; t < expressionTokens.length; t++)
				sb.append(" " + expressionTokens[t]);
			throw new GPathSyntaxException("Incomplete expression:" + sb.toString());
		}
		
		tempGpe.rightExpression = parseUnaryExpression(partStrings[2]);
		tempGpe.operator = partStrings[1][0];
		tempGpe.leftExpression = parseUnaryExpression(partStrings[0]);
		return gpe;
	}
	
	private static GPathUnaryExpression parseUnaryExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		int start = 0;
		if ("-".equals(expressionTokens[0])) start ++;
		
		//	parse eventual union
		ArrayList parts = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		
		for (int t = start; t < expressionTokens.length; t++) {
			String token = expressionTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token))
					openBrackets.pop();
					
				else if ("(".equals(token))
					openBrackets.push(")");
					
				else if ("[".equals(token))
					openBrackets.push("]");
			}
				
			//	start of annotation test brackets
			else if ("|".equals(token)) {
				if (DEBUG) System.out.print("Got unary expression part:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				openBrackets.push("]");
			}
			
			//	other char
			else collector.add(token);
		}
		
		if (!collector.isEmpty()) {
			if (DEBUG) System.out.print("Got final unary expression part:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0)
			return null;
		
		GPathUnaryExpression gpue;
		if (partStrings.length == 1) {
			gpue = parsePathExpression(partStrings[0]);
			gpue.isNegative = (start == 1);
			return gpue;
		}
		
		gpue = new GPathUnaryExpression();
		gpue.isNegative = (start == 1);
		gpue.partExpressions = new GPathUnaryExpression[partStrings.length];
		for (int p = 0; p < partStrings.length; p++)
			gpue.partExpressions[p] = parsePathExpression(partStrings[p]);
		
		return gpue;
	}
	
	private static GPathUnaryExpression parsePathExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		if (DEBUG) System.out.println("GPathParser.parsePathExpression: started with " + expressionTokens.length + " tokens");
		
		//	recognize leading primary expression
		
		//	single-tokened primary expression (shortcut return)
		if (expressionTokens.length == 1) {
			String token = expressionTokens[0];
			if (DEBUG) System.out.println("GPathParser.parsePathExpression: got single token - " + token);
			if ((token == null) || (token.length() == 0)) return null;
			
			GPathUnaryExpression xpue = new GPathUnaryExpression();
			
			//	literal
			if (token.startsWith("'") || token.startsWith("\"")) {
				if (DEBUG) System.out.println("GPathParser.parsePathExpression: it's a literal");
				xpue.literal = new GPathString(token.substring(1, (token.length() - 1)));
				xpue.quoter = token.charAt(0);
				return xpue;
			}
			
			//	number
			else if (isDigit(token.charAt(0)) || ((token.length() > 1) && (token.startsWith(".") && isDigit(token.charAt(1))))) {
				try {
					xpue.number = new GPathNumber(Double.parseDouble(token));
					return xpue;
				} catch (NumberFormatException nfe) {}
			}
			
			//	variable reference
			else if (token.startsWith("$")) {
				xpue.variableName = token;
				return xpue;
			}
		}
		
		//	expression in brackets
		if ("(".equals(expressionTokens[0])) {
			
			//	find end of leading filter expression
			int filterLength = -1;
			Stack openBrackets = new Stack();
			
			for (int t = 0; t < expressionTokens.length; t++) {
				String token = expressionTokens[t];
				
				//	nested part
				if (!openBrackets.isEmpty()) {
					
					//	process inner parts of predicates, etc.
					if (openBrackets.peek().equals(token))
						openBrackets.pop();
						
					else if ("(".equals(token))
						openBrackets.push(")");
						
					else if ("[".equals(token))
						openBrackets.push("]");
				}
					
				//	start of annotation test brackets
				else if ("/".equals(token)) {
					if (filterLength == -1)
						filterLength = t;
				}
				
				//	start of annotation test brackets
				else if ("(".equals(token))
					openBrackets.push(")");
					
				//	start of predicate
				else if ("[".equals(token))
					openBrackets.push("]");
			}
			
			//	get filter expression
			if (filterLength == -1)
				return parseFilterExpression(expressionTokens);
			
			String[] filterTokens = new String[filterLength];
			for (int f = 0; f < filterLength; f++) 
				filterTokens[f] = expressionTokens[f];
			
			GPathUnaryExpression gpue = parseFilterExpression(filterTokens);
			
			//	get path
			int pathLength = expressionTokens.length - filterLength - 1;
			String[] pathTokens = new String[pathLength];
			for (int p = 0; p < pathLength; p++) 
				pathTokens[p] = expressionTokens[p + filterLength + 1];
			
			gpue.pathExpression = parsePath(pathTokens);
			
			return gpue;
		}
		
		//	function call or annotation test
		else if ((expressionTokens.length > 2) && "(".equals(expressionTokens[1])) {
			
			//	annotation test (beginning of path)
			if (("annotation".equals(expressionTokens[0]) || "text".equals(expressionTokens[0]) || "comment".equals(expressionTokens[0])) && ")".equals(expressionTokens[2])) {
				GPathUnaryExpression gpue = new GPathUnaryExpression();
				gpue.pathExpression = parsePath(expressionTokens);
				return gpue;
			}
			else if ("procession-instruction".equals(expressionTokens[0]) && (")".equals(expressionTokens[2]) || ((expressionTokens.length > 3) && ")".equals(expressionTokens[3])))) {
				GPathUnaryExpression gpue = new GPathUnaryExpression();
				gpue.pathExpression = parsePath(expressionTokens);
				return gpue;
			}
			
			//	function call
			else {
				
				//	find end of leading filter expression
				int filterLength = -1;
				Stack openBrackets = new Stack();
				
				for (int t = 0; t < expressionTokens.length; t++) {
					String token = expressionTokens[t];
					
					//	nested part
					if (!openBrackets.isEmpty()) {
						
						//	process inner parts of predicates, etc.
						if (openBrackets.peek().equals(token))
							openBrackets.pop();
							
						else if ("(".equals(token))
							openBrackets.push(")");
							
						else if ("[".equals(token))
							openBrackets.push("]");
					}
					
					//	start of annotation test brackets
					else if ("/".equals(token)) {
						if (filterLength == -1)
							filterLength = t;
					}
					
					//	start of annotation test brackets
					else if ("(".equals(token))
						openBrackets.push(")");
						
					//	start of predicate
					else if ("[".equals(token))
						openBrackets.push("]");
				}
				
				//	get filter expression
				if (filterLength == -1)
					return parseFilterExpression(expressionTokens);
				
				String[] filterTokens = new String[filterLength];
				for (int f = 0; f < filterLength; f++) 
					filterTokens[f] = expressionTokens[f];
				
				GPathUnaryExpression gpue = parseFilterExpression(filterTokens);
				
				//	get path
				int pathLength = expressionTokens.length - filterLength - 1;
				String[] pathTokens = new String[pathLength];
				for (int p = 0; p < pathLength; p++) 
					pathTokens[p] = expressionTokens[p + filterLength + 1];
				
				gpue.pathExpression = parsePath(pathTokens);
				
				return gpue;
			}
		}
		else {
			GPathUnaryExpression gpue = new GPathUnaryExpression();
			gpue.pathExpression = parsePath(expressionTokens);
			return gpue;
		}
	}
	
	private static GPathUnaryExpression parseFilterExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		//	find end of leading expression and parse eventual predicates
		int primaryExpressionLength = -1;
		ArrayList predicates = new ArrayList();
		Stack openBrackets = new Stack();
		ArrayList collector = new ArrayList();
		
		for (int t = 0; t < expressionTokens.length; t++) {
			String token = expressionTokens[t];
			
			//	nested part
			if (!openBrackets.isEmpty()) {
				collector.add(token);
				
				//	process inner parts of predicates, etc.
				if (openBrackets.peek().equals(token)) {
					openBrackets.pop();
					if (openBrackets.isEmpty()) {
						if (primaryExpressionLength == -1) primaryExpressionLength = t;
						else if ("]".equals(token)) {
							if (DEBUG) System.out.print("Got filter expression predicate:");
							if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
							if (DEBUG) System.out.println();
							predicates.add(collector.toArray(new String[collector.size()]));
						}
						
						collector.clear();
					}
				}
				else if ("(".equals(token))
					openBrackets.push(")");
					
				else if ("[".equals(token))
					openBrackets.push("]");
			}
			
			//	start of annotation test brackets
			else if ("(".equals(token)) {
				collector.add(token);
				openBrackets.push(")");
			}
			
			//	start of predicate
			else if ("[".equals(token)) {
				collector.add(token);
				openBrackets.push("]");
			}
			
			//	other char
			else collector.add(token);
		}
		
		if (!collector.isEmpty() && "[".equals(collector.get(0))) {
			if (DEBUG) System.out.print("Got final filter expression predicate:");
			if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
			if (DEBUG) System.out.println();
			predicates.add(collector.toArray(new String[collector.size()]));
		}
		
		GPathUnaryExpression gpue = new GPathUnaryExpression();
		
		//	get eventual predicates
		if (!predicates.isEmpty()) {
			String[][] predicateStrings = ((String[][]) predicates.toArray(new String[predicates.size()][]));
			
			gpue.predicates = new GPathPredicate[predicateStrings.length];
			for (int p = 0; p < predicateStrings.length; p++) {
				gpue.predicates[p] = new GPathPredicate();
				String[] predicateTokens = new String[predicateStrings[p].length - 2];
				for (int pt = 0; pt < predicateTokens.length; pt++) 
					predicateTokens[pt] = predicateStrings[p][pt + 1];
				gpue.predicates[p].expression = parseOrExpression(predicateTokens);
			}
		}
		
		//	expression in brackets
		if ("(".equals(expressionTokens[0])) {
			
			//	get enclosed expression
			String[] enclosedExpressionTokens = new String[primaryExpressionLength - 1];
			for (int f = 0; f < (primaryExpressionLength - 1); f++)
				enclosedExpressionTokens[f] = expressionTokens[f + 1];
			gpue.enclosedExpression = parseOrExpression(enclosedExpressionTokens);
			
			return gpue;
		}
		
		//	function call
		else if ((expressionTokens.length > 2) && "(".equals(expressionTokens[1])) {
			
			gpue.functionName = expressionTokens[0];
			
			//	parse arguments
			ArrayList arguments = new ArrayList();
			openBrackets = new Stack();
			collector = new ArrayList();
			
			for (int t = 2; t < primaryExpressionLength; t++) {
				String token = expressionTokens[t];
				
				//	nested part
				if (!openBrackets.isEmpty()) {
					collector.add(token);
					
					//	process inner parts of predicates, etc.
					if (openBrackets.peek().equals(token))
						openBrackets.pop();
						
					else if ("(".equals(token))
						openBrackets.push(")");
						
					else if ("[".equals(token))
						openBrackets.push("]");
				}
				
				//	start of annotation test brackets
				else if (",".equals(token)) {
					if (DEBUG) System.out.print("Got function call argument:");
					if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
					if (DEBUG) System.out.println();
					arguments.add(collector.toArray(new String[collector.size()]));
					collector.clear();
				}
				
				//	start of annotation test brackets
				else if ("(".equals(token)) {
					collector.add(token);
					openBrackets.push(")");
				}
				
				//	start of predicate
				else if ("[".equals(token)) {
					collector.add(token);
					openBrackets.push("]");
				}
				
				//	other char
				else collector.add(token);
			}
			
			if (!collector.isEmpty()) {
				if (DEBUG) System.out.print("Got final function call argument:");
				if (DEBUG) for (int c = 0; c < collector.size(); c++) System.out.print(" " + collector.get(c));
				if (DEBUG) System.out.println();
				arguments.add(collector.toArray(new String[collector.size()]));
			}
			
			String[][] argumentStrings = ((String[][]) arguments.toArray(new String[predicates.size()][]));
			gpue.functionArgs = new GPathExpression[argumentStrings.length];
			for (int a = 0; a < argumentStrings.length; a++)
				gpue.functionArgs[a] = parseOrExpression(argumentStrings[a]);
		}
		
		return gpue;
	}
	
	/**	tokenize a gPath expression
	 * @param	string	the String to tokenize
	 * @return an arrray of Strings representing the tokens the specified String was decomposed to
	 */
	public static String[] tokenize(String string) {
		//	check parameter
		if ((string == null) || (string.trim().length() == 0)) return new String[0];
		
		//	normalize whitespace
		StringBuffer assembler = new StringBuffer();
		char c;
		for (int i = 0; i < string.length(); i++) {
			c = string.charAt(i);
			assembler.append((c < 33) ? ' ' : c);
		}
		String normalizedString = assembler.toString().trim();
		
		//	tokenize
		ArrayList tokens = new ArrayList();
		assembler = new StringBuffer();
		char quoter = NULLCHAR;
		boolean inName = false;
		
		char last = NULLCHAR;
		char next = normalizedString.charAt(0);
		int i = 0;
		while (next != NULLCHAR) {
			c = normalizedString.charAt(i);
			next = (((i + 1) < normalizedString.length()) ? normalizedString.charAt(i + 1) : NULLCHAR);
			
			//	end of quoted part
			if (c == quoter) {
				assembler.append(c);
				i++;
				quoter = NULLCHAR;
				
				tokens.add(assembler.toString());
				assembler = new StringBuffer();
			}
			
			//	in quotes
			else if (quoter != NULLCHAR) {
				assembler.append(c);
				i++;
			}
			
			//	start of quotas
			else if ((c == '"') || (c == '\'')) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}

				assembler.append(c);
				i++;
				quoter = c;
			}
				
			//	explicit expression token
			else if (normalizedString.startsWith("::", i)) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add("::");
				i += 2;
			}
			else if (normalizedString.startsWith("..", i)) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add("..");
				i += 2;
			}
			else if ("()[]@#,".indexOf(c) != -1) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add("" + c);
				i ++;
			}
			else if ((c == '.') && !isDigit(last) && !isDigit(next)) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add(".");
				i ++;
			}
			
			//	operators
			else if (normalizedString.startsWith("!=", i)) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add("!=");
				i += 2;
			}
			else if (normalizedString.startsWith("<=", i)) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add("<=");
				i += 2;
			}
			else if (normalizedString.startsWith(">=", i)) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add(">=");
				i += 2;
			}
			else if ((c == '-') && !inName) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add("-");
				i ++;
			}
			else if ("|+=<>*".indexOf(c) != -1) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add("" + c);
				i ++;
			}
			else if (c == '/') {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				if (last != '/') tokens.add("/");
				i ++;
			}
			
			//	whitespace
			else if (c < 33) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				i ++;
			}
			
			//	other char
			else {
				assembler.append(c);
				i ++;
			}
			
			inName = isNameStartChar(c) || (inName && isNameChar(c));
			last = c;
		}
		
		if (assembler.length() != 0) tokens.add(assembler.toString());
		
		//for (int t = 0; t < tokens.size(); t++) System.out.println(tokens.get(t));
		
		return ((String[]) tokens.toArray(new String[tokens.size()]));
	}
	
	/**	check if a GPath expression is sytactically correct
	 * @param	gPath	the GPath expression to check
	 * @return null if the specified GPath expression is syntactically correct, a String describing the error if it is not
	 */
	public static String validatePath(String gPath) {
		return validatePath(tokenize(gPath));
	}
	
	/**	check if a GPath expression is sytactically correct
	 * @param	gPathTokens	an array of String tokens representing the GPath expression to check
	 * @return null if the specified GPath expression is syntactically correct, a String describing the error if it is not
	 */
	public static String validatePath(String[] gPathTokens) {
		
		//	check brackets and operator order
		Stack openBrackets = new Stack();
		String lastToken = "NOT_A_TOKEN";
		for (int t = 0; t < gPathTokens.length; t++) {
			String token = gPathTokens[t];
			
			//	check brackets
			if ("([])".indexOf(token) != -1) {
				if ("(".equals(token)) openBrackets.push(")");
				else if ("[".equals(token)) openBrackets.push("]");
				else if (openBrackets.isEmpty()) return ("Unmatched closing bracket '" + token + "' at token " + t);
				else if (openBrackets.peek().equals(token)) openBrackets.pop();
				else return ("Invalid closing bracket '" + token + "' for '" + (")".equals(token) ? "[" : "(") + "' at token " + t);
				
			}
			
			//	check operators
			if ((OPERATORS.indexOf(token) != -1) && (INVALID_BEFORE_OPERATORS.indexOf(lastToken) != -1)) return ("Invalid operator '" + token + "' after '" + lastToken + "' at token " + t + ".");
			else if ((OPERATORS.indexOf(lastToken) != -1) && (INVALID_AFTER_OPERATORS.indexOf(token) != -1)) return ("Invalid operator '" + lastToken + "' before '" + token + "' at token " + (t - 1) + ".");
			
			lastToken = token;
		}
		
		//	check if any brackets remained open
		if (!openBrackets.isEmpty()) return ("Unmatched opening bracket '" + openBrackets.peek() + "'");
		
		//	checks passed
		return null;
	}
	
	private static final String INVALID_BEFORE_OPERATORS 		= " :: + = != < <= > >= ( [ ";
	private static final String OPERATORS 						= " + = != < <= > >= ";
	private static final String INVALID_AFTER_OPERATORS 		= " ) ] + = != < <= > >= ";
	
	private static final String OPERATOR_KILLERS				= " @ :: ( [ , / // | + - = != < <= > >= ";
	private static final String CONDITIONAL_OPERATOR_KILLERS	= " and or mod div * ";
	
	private static boolean isDigit(char c) {
		return ((c < 127) && Character.isDigit(c));
	}
	
	private static boolean isNameStartChar(char c) {
		return ((c == '_') || Character.isLetter(c));
	}
	
	private static boolean isNameChar(char c) {
		return (isNameStartChar(c) || isDigit(c) || (c == '.') || (c == '-'));
	}
	
	private static boolean canBeOperator(String token) {
		return (CONDITIONAL_OPERATOR_KILLERS.indexOf(" " + token + " ") != -1);
	}
	
	private static boolean canPreceedOperator(String token) {
		return (OPERATOR_KILLERS.indexOf(" " + token + " ") == -1);
	}
}