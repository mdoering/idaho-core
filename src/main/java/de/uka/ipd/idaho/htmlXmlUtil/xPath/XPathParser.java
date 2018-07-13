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
package de.uka.ipd.idaho.htmlXmlUtil.xPath;


import java.util.ArrayList;
import java.util.Stack;

import de.uka.ipd.idaho.htmlXmlUtil.xPath.exceptions.XPathSyntaxException;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathNumber;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathString;

/**
 * Parser for XPath expressions. Supports the complete specification.
 * 
 * @author sautter
 */
public class XPathParser {
	
	//	TODO make this a linear parser ... now we know how that works ...
	
	private static final char NULLCHAR = '\u0000';
	private static final boolean DEBUG = false;
	
	/**	parse an XPath
	 * @param	path	the String representation of the XPath to parse
	 * @return an XPath object representing the parsed XPath
	 */
	public static XPath parsePath(String path) {
		
		//	check parameter
		if ((path == null) || (path.length() == 0)) return new XPath();
		String[] pathTokens = tokenize(path);
		String error = validatePath(pathTokens);
		if (error != null) throw new XPathSyntaxException("Invalid XPath expression: " + error);
		return parsePath(pathTokens);
	}
	
	private static XPath parsePath(String[] pathTokens) {
		
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
				if (DEBUG) {
					System.out.print("Got step:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				steps.add(collector.toArray(new String[collector.size()]));
				collector.clear();
			}
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final step:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			steps.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] stepStrings = ((String[][]) steps.toArray(new String[steps.size()][]));
		steps.clear();
		
		XPath xp = new XPath();
		xp.steps = new XPathStep[stepStrings.length];
		
		for (int s = 0; s < stepStrings.length; s++)
			xp.steps[s] = parseStep(stepStrings[s]);
		
		return xp;
	}
	
	private static XPathStep parseStep(String[] stepTokens) {
		
		//	check parameter
		if (stepTokens == null) return null;
		
		//	abbreviated step
		if (stepTokens.length == 0) {
			XPathStep xps = new XPathStep();
			xps.axis = "descendant-or-self";
			return xps;
		}
		else if (".".equals(stepTokens[0])) {
			XPathStep xps = new XPathStep();
			xps.axis = "self";
			return xps;
		}
		else if ("..".equals(stepTokens[0])) {
			XPathStep xps = new XPathStep();
			xps.axis = "parent";
			return xps;
		}
		
		//	implicit axis
		if (stepTokens.length == 1) {
			XPathStep xps = new XPathStep();
			xps.nodeTest = stepTokens[0];
			return xps;
		}
		
		XPathStep xps = new XPathStep();
		int index = 0;
		
		//	read axis and node test
		if ("::".equals(stepTokens[1])) {
			xps.axis = stepTokens[0];
			index = 2;
		}
		else if ("@".equals(stepTokens[0])) {
			xps.axis = "attribute";
			index = 1;
		}
		
		//	read node test
		xps.nodeTest = stepTokens[index];
		index++;
		if (index < stepTokens.length) {
			if ("(".equals(stepTokens[index])) {
				xps.nodeTest += "(";
				index ++;
				if (")".equals(stepTokens[index])) {
					xps.nodeTest += ")";
					index ++;
				}
				else {
					xps.nodeTest += stepTokens[index];
					index ++;
					xps.nodeTest += ")";
					index ++;
				}
			}
		}
		
		//	read possible predicates
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
							if (DEBUG) {
								System.out.print("Got predicate:");
								for (int c = 0; c < collector.size(); c++)
									System.out.print(" " + collector.get(c));
								System.out.println();
							}
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
				
				//	start of node test brackets
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
				if (DEBUG) {
					System.out.print("Got final predicate:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				predicates.add(collector.toArray(new String[collector.size()]));
			}
			
			String[][] predicateStrings = ((String[][]) predicates.toArray(new String[predicates.size()][]));
			
			xps.predicates = new XPathPredicate[predicateStrings.length];
			for (int p = 0; p < predicateStrings.length; p++) {
				xps.predicates[p] = new XPathPredicate();
				xps.predicates[p].expression = parseOrExpression(predicateStrings[p]);
			}
		}
		
		return xps;
	}
	
	private static XPathExpression parseOrExpression(String[] expressionTokens) {
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
			
			//	start of node test brackets
			else if ("or".equals(token) && canBeOperator && !lastWasOperator) {
				if (DEBUG) {
					System.out.print("Got OR expression part:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				canBeOperator = false;
				lastWasOperator = true;
			}
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final OR expression part:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0) return null;
		if (partStrings.length == 1) return parseAndExpression(partStrings[0]);
		
		XPathBinaryExpression xpe = new XPathBinaryExpression();
		XPathBinaryExpression tempXpe = xpe;
		int i = partStrings.length - 1;
		while (i > 1) {
			tempXpe.rightExpression = parseAndExpression(partStrings[i]);
			tempXpe.operator = "or";
			tempXpe.leftExpression = new XPathBinaryExpression();
			tempXpe = ((XPathBinaryExpression) tempXpe.leftExpression);
			i--;
		}
		tempXpe.rightExpression = parseAndExpression(partStrings[1]);
		tempXpe.operator = "or";
		tempXpe.leftExpression = parseAndExpression(partStrings[0]);
		return xpe;
	}
	
	private static XPathExpression parseAndExpression(String[] expressionTokens) {
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
			
			//	start of node test brackets
			else if ("and".equals(token) && canBeOperator && !lastWasOperator) {
				if (DEBUG) {
					System.out.print("Got AND expression part:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				canBeOperator = false;
				lastWasOperator = true;
			}
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final AND expression part:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0) return null;
		if (partStrings.length == 1) return parseEqualityExpression(partStrings[0]);
		
		XPathBinaryExpression xpe = new XPathBinaryExpression();
		XPathBinaryExpression tempXpe = xpe;
		int i = partStrings.length - 1;
		while (i > 1) {
			tempXpe.rightExpression = parseEqualityExpression(partStrings[i]);
			tempXpe.operator = "and";
			tempXpe.leftExpression = new XPathBinaryExpression();
			tempXpe = ((XPathBinaryExpression) tempXpe.leftExpression);
			i--;
		}
		tempXpe.rightExpression = parseEqualityExpression(partStrings[1]);
		tempXpe.operator = "and";
		tempXpe.leftExpression = parseEqualityExpression(partStrings[0]);
		return xpe;
	}
	
	private static XPathExpression parseEqualityExpression(String[] expressionTokens) {
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
			
			//	start of node test brackets
			else if ("!=".equals(token) || "=".equals(token)) {
				if (DEBUG) {
					System.out.print("Got equality expression part:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
			}
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final equality expression part:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0) return null;
		if (partStrings.length == 1) return parseRelationalExpression(partStrings[0]);
		
		XPathBinaryExpression xpe = new XPathBinaryExpression();
		XPathBinaryExpression tempXpe = xpe;
		int i = partStrings.length - 1;
		while (i > 2) {
			tempXpe.rightExpression = parseRelationalExpression(partStrings[i]);
			tempXpe.operator = partStrings[i - 1][0];
			tempXpe.leftExpression = new XPathBinaryExpression();
			tempXpe = ((XPathBinaryExpression) tempXpe.leftExpression);
			i -= 2;
		}
		tempXpe.rightExpression = parseRelationalExpression(partStrings[2]);
		tempXpe.operator = partStrings[1][0];
		tempXpe.leftExpression = parseRelationalExpression(partStrings[0]);
		return xpe;
	}
	
	private static XPathExpression parseRelationalExpression(String[] expressionTokens) {
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
			
			//	start of node test brackets
			else if ("<=".equals(token) || "<".equals(token) || ">".equals(token) || ">=".equals(token)) {
				if (DEBUG) {
					System.out.print("Got relational expression part:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
			}
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final relational expression part:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0) return null;
		if (partStrings.length == 1) return parseAdditiveExpression(partStrings[0]);
		
		XPathBinaryExpression xpe = new XPathBinaryExpression();
		XPathBinaryExpression tempXpe = xpe;
		int i = partStrings.length - 1;
		while (i > 2) {
			tempXpe.rightExpression = parseAdditiveExpression(partStrings[i]);
			tempXpe.operator = partStrings[i - 1][0];
			tempXpe.leftExpression = new XPathBinaryExpression();
			tempXpe = ((XPathBinaryExpression) tempXpe.leftExpression);
			i -= 2;
		}
		tempXpe.rightExpression = parseAdditiveExpression(partStrings[2]);
		tempXpe.operator = partStrings[1][0];
		tempXpe.leftExpression = parseAdditiveExpression(partStrings[0]);
		return xpe;
	}
	
	private static XPathExpression parseAdditiveExpression(String[] expressionTokens) {
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
			
			//	start of node test brackets
			else if ("+".equals(token)) {
				if (DEBUG) {
					System.out.print("Got additive expression part:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
				canBeOperator = false;
			}
			
			//	start of node test brackets
			else if ("-".equals(token) && canBeOperator) {
				if (DEBUG) {
					System.out.print("Got additive expression part:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
				canBeOperator = false;
			}
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final additive expression part:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0) return null;
		if (partStrings.length == 1) return parseMultiplicativeExpression(partStrings[0]);
		
		XPathBinaryExpression xpe = new XPathBinaryExpression();
		XPathBinaryExpression tempXpe = xpe;
		int i = partStrings.length - 1;
		while (i > 2) {
			tempXpe.rightExpression = parseMultiplicativeExpression(partStrings[i]);
			tempXpe.operator = partStrings[i - 1][0];
			tempXpe.leftExpression = new XPathBinaryExpression();
			tempXpe = ((XPathBinaryExpression) tempXpe.leftExpression);
			i -= 2;
		}
		tempXpe.rightExpression = parseMultiplicativeExpression(partStrings[2]);
		tempXpe.operator = partStrings[1][0];
		tempXpe.leftExpression = parseMultiplicativeExpression(partStrings[0]);
		return xpe;
	}
	
	private static XPathExpression parseMultiplicativeExpression(String[] expressionTokens) {
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
			
			//	start of node test brackets
			else if (("*".equals(token) || "div".equals(token) || "mod".equals(token)) && canBeOperator && !lastWasOperator) {
				if (DEBUG) {
					System.out.print("Got multiplicative expression part:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
				
				String[] operator = new String[1];
				operator[0] = token;
				parts.add(operator);
				canBeOperator = false;
				lastWasOperator = true;
			}
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final multiplicative expression part:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0) return null;
		if (partStrings.length == 1) return parseUnaryExpression(partStrings[0]);
		
		XPathBinaryExpression xpe = new XPathBinaryExpression();
		XPathBinaryExpression tempXpe = xpe;
		int i = partStrings.length - 1;
		while (i > 2) {
			tempXpe.rightExpression = parseUnaryExpression(partStrings[i]);
			tempXpe.operator = partStrings[i - 1][0];
			tempXpe.leftExpression = new XPathBinaryExpression();
			tempXpe = ((XPathBinaryExpression) tempXpe.leftExpression);
			i -= 2;
		}
		tempXpe.rightExpression = parseUnaryExpression(partStrings[2]);
		tempXpe.operator = partStrings[1][0];
		tempXpe.leftExpression = parseUnaryExpression(partStrings[0]);
		return xpe;
	}
	
	private static XPathUnaryExpression parseUnaryExpression(String[] expressionTokens) {
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
			
			//	start of node test brackets
			else if ("|".equals(token)) {
				if (DEBUG) {
					System.out.print("Got unary expression part:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				parts.add(collector.toArray(new String[collector.size()]));
				collector.clear();
			}
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final unary expression part:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			parts.add(collector.toArray(new String[collector.size()]));
		}
		
		String[][] partStrings = ((String[][]) parts.toArray(new String[parts.size()][]));
		
		if (partStrings.length == 0) return null;
		
		XPathUnaryExpression xpue;
		if (partStrings.length == 1) {
			xpue = parsePathExpression(partStrings[0]);
			xpue.isNegative = (start == 1);
			return xpue;
		}
		
		xpue = new XPathUnaryExpression();
		xpue.isNegative = (start == 1);
		xpue.partExpressions = new XPathUnaryExpression[partStrings.length];
		for (int p = 0; p < partStrings.length; p++)
			xpue.partExpressions[p] = parsePathExpression(partStrings[p]);
		
		return xpue;
	}
	
	private static XPathUnaryExpression parsePathExpression(String[] expressionTokens) {
		if ((expressionTokens == null) || (expressionTokens.length == 0)) return null;
		
		//	recognize leading primary expression
		
		//	single-tokened primary expression (shortcut return)
		if (expressionTokens.length == 1) {
			String token = expressionTokens[0];
			if ((token == null) || (token.length() == 0)) return null;
			
			XPathUnaryExpression xpue = new XPathUnaryExpression();
			
			//	literal
			if (token.startsWith("'") || token.startsWith("\"")) {
				xpue.literal = new XPathString(token.substring(1, (token.length() - 1)));
				xpue.quoter = token.charAt(0);
				return xpue;
			}
			
			//	number
			else if (isDigit(token.charAt(0)) || ((token.length() > 1) && (token.startsWith(".") && isDigit(token.charAt(1))))) {
				try {
					xpue.number = new XPathNumber(Double.parseDouble(token));
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
				
				//	start of node test brackets
				else if ("/".equals(token))
					if (filterLength == -1) filterLength = t;
					
				//	start of node test brackets
				else if ("(".equals(token))
					openBrackets.push(")");
					
				//	start of predicate
				else if ("[".equals(token))
					openBrackets.push("]");
			}
			
			//	get filter expression
			if (filterLength == -1) return parseFilterExpression(expressionTokens);
			
			String[] filterTokens = new String[filterLength];
			for (int f = 0; f < filterLength; f++) 
				filterTokens[f] = expressionTokens[f];
			
			XPathUnaryExpression xpue = parseFilterExpression(filterTokens);
			
			//	get path
			int pathLength = expressionTokens.length - filterLength - 1;
			String[] pathTokens = new String[pathLength];
			for (int p = 0; p < pathLength; p++) 
				pathTokens[p] = expressionTokens[p + filterLength + 1];
			
			xpue.pathExpression = parsePath(pathTokens);
			
			return xpue;
		}
		
		//	function call or node test
		else if ((expressionTokens.length > 2) && "(".equals(expressionTokens[1])) {
			
			//	node test (beginning of path)
			if (("node".equals(expressionTokens[0]) || "text".equals(expressionTokens[0]) || "comment".equals(expressionTokens[0])) && ")".equals(expressionTokens[2])) {
				XPathUnaryExpression xpue = new XPathUnaryExpression();
				xpue.pathExpression = parsePath(expressionTokens);
				return xpue;
			}
			else if ("procession-instruction".equals(expressionTokens[0]) && (")".equals(expressionTokens[2]) || ((expressionTokens.length > 3) && ")".equals(expressionTokens[3])))) {
				XPathUnaryExpression xpue = new XPathUnaryExpression();
				xpue.pathExpression = parsePath(expressionTokens);
				return xpue;
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
					
					//	start of node test brackets
					else if ("/".equals(token))
						if (filterLength == -1) filterLength = t;
						
					//	start of node test brackets
					else if ("(".equals(token))
						openBrackets.push(")");
						
					//	start of predicate
					else if ("[".equals(token))
						openBrackets.push("]");
				}
				
				//	get filter expression
				if (filterLength == -1) return parseFilterExpression(expressionTokens);
				
				String[] filterTokens = new String[filterLength];
				for (int f = 0; f < filterLength; f++) 
					filterTokens[f] = expressionTokens[f];
				
				XPathUnaryExpression xpue = parseFilterExpression(filterTokens);
				
				//	get path
				int pathLength = expressionTokens.length - filterLength - 1;
				String[] pathTokens = new String[pathLength];
				for (int p = 0; p < pathLength; p++) 
					pathTokens[p] = expressionTokens[p + filterLength + 1];
				
				xpue.pathExpression = parsePath(pathTokens);
				
				return xpue;
			}
		} else {
			XPathUnaryExpression xpue = new XPathUnaryExpression();
			xpue.pathExpression = parsePath(expressionTokens);
			return xpue;
		}
	}
	
	private static XPathUnaryExpression parseFilterExpression(String[] expressionTokens) {
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
							if (DEBUG) {
								System.out.print("Got filter expression predicate:");
								for (int c = 0; c < collector.size(); c++)
									System.out.print(" " + collector.get(c));
								System.out.println();
							}
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
			
			//	start of node test brackets
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
			if (DEBUG) {
				System.out.print("Got final filter expression predicate:");
				for (int c = 0; c < collector.size(); c++)
					System.out.print(" " + collector.get(c));
				System.out.println();
			}
			predicates.add(collector.toArray(new String[collector.size()]));
		}
		
		XPathUnaryExpression xpue = new XPathUnaryExpression();
		
		//	get eventual predicates
		if (!predicates.isEmpty()) {
			String[][] predicateStrings = ((String[][]) predicates.toArray(new String[predicates.size()][]));
			
			xpue.predicates = new XPathPredicate[predicateStrings.length];
			for (int p = 0; p < predicateStrings.length; p++) {
				xpue.predicates[p] = new XPathPredicate();
				String[] predicateTokens = new String[predicateStrings[p].length - 2];
				for (int pt = 0; pt < predicateTokens.length; pt++) 
					predicateTokens[pt] = predicateStrings[p][pt + 1];
				xpue.predicates[p].expression = parseOrExpression(predicateTokens);
			}
		}
		
		//	expression in brackets
		if ("(".equals(expressionTokens[0])) {
			
			//	get enclosed expression
			String[] enclosedExpressionTokens = new String[primaryExpressionLength - 1];
			for (int f = 0; f < (primaryExpressionLength - 1); f++)
				enclosedExpressionTokens[f] = expressionTokens[f + 1];
			xpue.enclosedExpression = parseOrExpression(enclosedExpressionTokens);
			
			return xpue;
		}
		
		//	function call
		else if ((expressionTokens.length > 2) && "(".equals(expressionTokens[1])) {
			
			xpue.functionName = expressionTokens[0];
			
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
				
				//	start of node test brackets
				else if (",".equals(token)) {
					if (DEBUG) {
						System.out.print("Got function call argument:");
						for (int c = 0; c < collector.size(); c++)
							System.out.print(" " + collector.get(c));
						System.out.println();
					}
					arguments.add(collector.toArray(new String[collector.size()]));
					collector.clear();
				}
				
				//	start of node test brackets
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
				if (DEBUG) {
					System.out.print("Got final function call argument:");
					for (int c = 0; c < collector.size(); c++)
						System.out.print(" " + collector.get(c));
					System.out.println();
				}
				arguments.add(collector.toArray(new String[collector.size()]));
			}
			
			String[][] argumentStrings = ((String[][]) arguments.toArray(new String[predicates.size()][]));
			xpue.functionArgs = new XPathExpression[argumentStrings.length];
			for (int a = 0; a < argumentStrings.length; a++)
				xpue.functionArgs[a] = parseOrExpression(argumentStrings[a]);
		}
		
		return xpue;
	}
	
	private static String[] tokenize(String string) {
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
			else if ("()[]@,".indexOf(c) != -1) {
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
			else if ("/|+=<>*".indexOf(c) != -1) {
				if (assembler.length() != 0) {
					tokens.add(assembler.toString());
					assembler = new StringBuffer();
				}
				tokens.add("" + c);
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
	
	/**	check if a XPath expression is sytactically correct
	 * @param	path	the XPath expression to check
	 * @return null if the specified GPath expression is syntactically correct, a String describing the error if it is not
	 */
	public static String validatePath(String path) {
		return validatePath(tokenize(path));
	}
	
	/**	check if a XPath expression is sytactically correct
	 * @param	pathTokens	an array of String tokens representing the XPath expression to check
	 * @return null if the specified GPath expression is syntactically correct, a String describing the error if it is not
	 */
	public static String validatePath(String[] pathTokens) {
		
		//	check brackets and operator order
		Stack openBrackets = new Stack();
		String lastToken = "NOT_A_TOKEN";
		for (int t = 0; t < pathTokens.length; t++) {
			String token = pathTokens[t];
			
			//	check brackets
			if ("([])".indexOf(token) != -1) {
				if ("(".equals(token))
					openBrackets.push(")");
				else if ("[".equals(token))
					openBrackets.push("]");
				else if (openBrackets.isEmpty())
					return ("Unmatched closing bracket '" + token + "' at token " + t);
				else if (openBrackets.peek().equals(token))
					openBrackets.pop();
				else return ("Invalid closing bracket '" + token + "' for '" + (")".equals(token) ? "[" : "(") + "' at token " + t);
			}
			
			//	check operators
			if ((OPERATORS.indexOf(token) != -1) && (INVALID_BEFORE_OPERATORS.indexOf(lastToken) != -1))
				return ("Invalid operator '" + token + "' after '" + lastToken + "' at token " + t + ".");
			else if ((OPERATORS.indexOf(lastToken) != -1) && (INVALID_AFTER_OPERATORS.indexOf(token) != -1))
				return ("Invalid operator '" + lastToken + "' before '" + token + "' at token " + (t - 1) + ".");
			
			lastToken = token;
		}
		
		//	check if any brackets remained open
		if (!openBrackets.isEmpty()) return ("Unmatched opening bracket '" + openBrackets.peek() + "'");
		
		//	checks passed
		return null;
	}
	
	private static final String LETTERS 	= "aäáâàbcdeéêèfghiíîìjklmnoöóôòpqrstuüúûùvwxyzßAÄÁÂÀBCDEÉÊÈFGHIÍîÌJKLMNOÖÓÔÒPQRSTUÜÚÛÙVWXYZ";
	private static final String DIGITS		= "0123456789";
	
	private static final String INVALID_BEFORE_OPERATORS 		= " :: + = != < <= > >= ( [ ";
	private static final String OPERATORS 						= " + = != < <= > >= ";
	private static final String INVALID_AFTER_OPERATORS 		= " ) ] + = != < <= > >= ";
	
	private static final String OPERATOR_KILLERS				= " @ :: ( [ , / // | + - = != < <= > >= ";
	private static final String CONDITIONAL_OPERATOR_KILLERS	= " and or mod div * ";
	
	private static boolean isDigit(char c) {
		return (DIGITS.indexOf(c) != -1);
	}
	
	private static boolean isNameStartChar(char c) {
		return ((c == '_') || (LETTERS.indexOf(c) != -1));
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
	
	public static void main(String[] args) throws Exception {
		String xPath = "mods:mods/mods:title[./@mods:type = 'full']";
		String[] tokens = tokenize(xPath);
		for (int t = 0; t < tokens.length; t++)
			System.out.println(tokens[t]);
		XPath path = parsePath(tokens);
		System.out.println(path.toString());
	}
}
