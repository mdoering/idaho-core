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
package de.uka.ipd.idaho.htmlXmlUtil.accessories;


import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;

import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Html;

/**
 * Tool box library for executing basic path expressions on trees of TreeNode
 * objects and for generating these expressions from a given tree. It also
 * provides methods for quantitatively comparing two given trees, and for tree
 * traversal.
 * 
 * @author sautter
 */
public class TreeTools {
	
	private static final String NON_STRUCT_TAGS = ";pcdata;h1;h2;h3;h4;h5;h6;br;blockquote;address;pre;em;strong;code;samp;kbd;var;cite;dfn;acronym;abbr;ins;del;bdo;b;i;tt;u;strike;s;big;small;sup;sub;";

	/**
	 * execute a path query
	 * @param root the root node of the tree to execute the query on
	 * @param query the path query itself
	 * @return the result of the query
	 */
	public static String executeQuery(TreeNode root, String query) {
		return executeQuery(root, query, null, null, null);
	}

	/**
	 * execute a path query
	 * @param root the root node of the tree to execute the query on
	 * @param query the path query itself
	 * @param start the Stzring to be inserted befor the result
	 * @param separator the String to be inserted between the sigular results
	 * @param end the String to be inserted at the end of the result
	 * @return the result of the query
	 */
	public static String executeQuery(TreeNode root, String query, String start, String separator, String end) {
		StringVector queryParts = parseQuery(query);
		if (queryParts.size() > 0) {
			
			//	extract parameter and StructEx part
			String parameter = getParameterPart((String) queryParts.lastElement());
			String structExpr = getStructExPart((String) queryParts.lastElement());
			
			//	fetch nodes
			TreeNode[] nodes = getNodesByPath(root, query);
			
			if (nodes.length > 0) {
				
				String result;
				
				//	singular result
				if ((start == null) || (separator == null) || (end == null)) {
					
					//	get parameter or value
					result = ((parameter.length() > 0) ? nodes[0].getAttribute(parameter) : nodes[0].getNodeValue());
					
					//	execute StructExpr if required and return result
					return ((structExpr.length() > 0) ? executeStructExpr(structExpr, result) : result);
				}
				
				//	multi result
				else {
					
					Vector results = new Vector();
					
					//	assemble result parts
					for (int i = 0; i < nodes.length; i++) {
						
						//	get parameter or value
						result = ((parameter.length() > 0) ? nodes[i].getAttribute(parameter) : nodes[i].getNodeValue());
						
						//	execute StructExpr if required
						results.addElement((structExpr.length() > 0) ? executeStructExpr(structExpr, result) : result);
					}
					
					//	assemble and return result
					return (start + concatVector(results, separator) + end);
				}
			}
			else return "";
		}
		else return "";
	}
	
	/**
	 * get a node by following a path in the tree
	 * @param root the root node of the tree to execute the query on
	 * @param query the path query itself
	 * @return the node addressed by the query, or null, if there is no such
	 *         node in the tree with the specified root
	 */
	public static TreeNode getNodeByPath(TreeNode root, String query) {
		TreeNode[] result = getNodesByPath(root, query);
		return ((result.length > 0) ? result[0] : null);
	}

	/**
	 * get a set of nodes by following a path in the tree
	 * @param root the root node of the tree to execute the query on
	 * @param query the path query itself
	 * @return the node addressed by the query, or an empty array of length 0,
	 *         if there are no such node in the tree with the specified root
	 */
	public static TreeNode[] getNodesByPath(TreeNode root, String query) {
		TreeNode[] result;
		StringVector queryParts = parseQuery(query);
		
		TreeNode[] tempResult;
		TreeNode[] temp;
		
		if (queryParts.size() > 0) {
			
			//	execute first part of query
			result = executeQueryPart(root, queryParts.get(0));
			
			//	execute rest of query
			for (int q = 1; q < queryParts.size(); q++) {
				
				//	remember result of last step
				tempResult = result;
				Vector resultNodes = new Vector();
				
				//	execute next part of query with all nodes of the intermediate result ...
				for (int i = 0; i < tempResult.length; i++) {
					temp = executeQueryPart(tempResult[i], queryParts.get(q));
					
					//	... and move resulting nodes to new intermediate result
					for (int j = 0; j < temp.length; j++)
						resultNodes.addElement(temp[j]);
				}
				
				result = ((TreeNode[]) resultNodes.toArray(new TreeNode[resultNodes.size()]));
			}
			
			//	retrun result
			return result;
		} else {
			return new TreeNode[0];
		}
	}
	
	/**
	 * execute a Structural Expression on a String
	 * @param structExpr the expression to execute
	 * @param dataString the String to execute the expression on
	 * @return the part of dataString addressed by the specified Structural
	 *         Expression
	 */
	public static String executeStructExpr(String structExpr, String dataString) {
		
		StringBuffer result = new StringBuffer(dataString.trim());
		String structuralExpression = structExpr;
		
		/*	- analyze Structural Expression
		 *	 	- fix or variable lenght target (a couple of ? or a single §)
		 *	 	- target at beginning, at the end or in the middle
		 *	 	- target bordered by border directly, or fix length waist in between
		 */
		
		int index = 0;
		char currentChar;
		int escapeIndex = -1;
		StringBuffer structExprPartsAssembler = new StringBuffer();
		StringVector structExprParts = new StringVector();
		IntVector structExprPartMeanings = new IntVector();
		
		//	no target string mark
		if (structExpr.indexOf("§") == -1 && structExpr.indexOf("?") == -1) return "";
		
		//	fix length target at beginning or end (keep attention for escape characters if target at end)
		if (structExpr.startsWith("?")) return ((result.length() > (structExpr.lastIndexOf("?") + 1)) ? result.delete((structExpr.lastIndexOf("?") + 1), result.length()).toString().trim() : result.toString());
		if (structExpr.endsWith("?") && !structExpr.endsWith("'?")) return (((result.length() - (structExpr.length() - structExpr.indexOf("?"))) > 0) ? result.delete(0, (result.length() - (structExpr.length() - structExpr.indexOf("?")))).toString().trim() : result.toString());
		
		//	 - parse StructExpr
		for (int i = 0; i < structuralExpression.length(); i++) {
			currentChar = structuralExpression.charAt(i);
			
			//	escaped character
			if (i == escapeIndex)
				structExprPartsAssembler = structExprPartsAssembler.append(currentChar);
				
			//	escape character
			else if (currentChar == '\'') {
				escapeIndex = i+1;
				structExprPartsAssembler = structExprPartsAssembler.append(currentChar);
			}
			
			//	variable length target or waist
			else if (currentChar == '*' || currentChar == '§') {
				if (structExprPartsAssembler.length() > 0) {
					structExprParts.addElement(structExprPartsAssembler.toString());
					structExprPartsAssembler = structExprPartsAssembler.delete(0, structExprPartsAssembler.length());
				}
				structExprParts.addElement(java.lang.Character.toString(currentChar));
			}
			
			//	part of fix length target or waist
			else if (currentChar == '+' || currentChar == '?') {
				if (structExprPartsAssembler.length() == 0 || structExprPartsAssembler.charAt(0) == currentChar)
					structExprPartsAssembler = structExprPartsAssembler.append(currentChar);
				
				else {
					structExprParts.addElement(structExprPartsAssembler.toString());
					structExprPartsAssembler = structExprPartsAssembler.delete(0, structExprPartsAssembler.length());
					structExprPartsAssembler = structExprPartsAssembler.append(currentChar);
				}
			}
			
			//	part of a border
			else {
				if (structExprPartsAssembler.length() == 0 || !(structExprPartsAssembler.charAt(0) == '?' || structExprPartsAssembler.charAt(0) == '+')) {
					structExprPartsAssembler = structExprPartsAssembler.append(currentChar);
				} else {
					structExprParts.addElement(structExprPartsAssembler.toString());
					structExprPartsAssembler = structExprPartsAssembler.delete(0, structExprPartsAssembler.length());
					structExprPartsAssembler = structExprPartsAssembler.append(currentChar);
				}
			}
		}
		if (structExprPartsAssembler.length() > 0) {
			structExprParts.addElement(structExprPartsAssembler.toString());
			structExprPartsAssembler = structExprPartsAssembler.delete(0, structExprPartsAssembler.length());
		}
		
		/*	- check StructExpr
		 *		- remove duplicate * or §
		 *		- remove + beside * and ? beside §
		 *		- if variable length target and waist neighboured, target is not clear
		 */
		while (index < (structExprParts.size()-1)) {
			if (structExprParts.get(index).equals("*")) {
				if (structExprParts.get(index+1).equals("*") || structExprParts.get(index+1).startsWith("+"))
					structExprParts.removeElementAt(index+1);
				else if (structExprParts.get(index+1).equals("§"))
					return dataString;
				else index++;
			}
			else if (structExprParts.get(index).equals("§")) {
				if (structExprParts.get(index+1).equals("§") || structExprParts.get(index+1).startsWith("?"))
					structExprParts.removeElementAt(index+1);
				else if (structExprParts.get(index+1).equals("*"))
					return dataString;
				else index++;
			}
			else if (structExprParts.get(index).startsWith("+")) {
				if (structExprParts.get(index+1).equals("*"))
					structExprParts.removeElementAt(index);
				else index++;
			}
			else if (structExprParts.get(index).startsWith("?")) {
				if (structExprParts.get(index+1).equals("§"))
					structExprParts.removeElementAt(index);
				else index++;
			}
			else index++;
		}
		
		//	- mark borders
		for (int i = 0; i < structExprParts.size(); i++) {
			if (structExprParts.get(i).equals("*") || structExprParts.get(i).equals("§") || structExprParts.get(i).startsWith("+") || structExprParts.get(i).startsWith("?")) {
				structExprPartMeanings.addElement(1);
			} else {
				structExprPartMeanings.addElement(0);
			}
		}
		
		//	- remove escape characters from borders
		for (int i = 0; i < structExprParts.size(); i++) {
			if (structExprPartMeanings.get(i) == 0) {
				String part = structExprParts.get(i);
				escapeIndex = -1;
				for (int j = 0; j < part.length(); j++) {
					currentChar = part.charAt(j);
					if (j == escapeIndex)
						structExprPartsAssembler = structExprPartsAssembler.append(currentChar);
					else if (currentChar == '\'')
						escapeIndex = j+1;
					else structExprPartsAssembler = structExprPartsAssembler.append(currentChar);
				}
				structExprParts.setElementAt(structExprPartsAssembler.toString(), i);
				structExprPartsAssembler = structExprPartsAssembler.delete(0, structExprPartsAssembler.length());
			}
		}
		
		//	- apply StructExpr to data string
		int length = 0;
		while (structExprParts.size() > 0) {
			
			//	if no change in last loop, return result (endless loop protection)
			if (result.length() == length) return result.toString();
			
			//	remember current length
			length = result.length();
			
			//	variable length waist
			if (structExprParts.get(0).equals("*") && (structExprPartMeanings.get(0) == 1)) {
				
				//	subsequent part is fix length target
				if ((structExprParts.size() > 1) && structExprParts.get(1).startsWith("?") && (structExprPartMeanings.get(1) == 1)) {
					
					//	data string ends with fix length target
					if (structExprParts.size() == 2)
						
						//	return result
						return (((result.length() - structExprParts.get(1).length()) > -1) ? result.substring((result.length() - structExprParts.get(1).length()), result.length()).trim() : result.toString());
						
					//	second subsequent part is fix length waist
					else if (structExprParts.get(2).startsWith("+") && (structExprPartMeanings.get(2) == 1)) {
						
						//	data string ends with fix length waist
						if (structExprParts.size() == 3) {
							
							//	return result
							if ((result.length() - (structExprParts.get(1)).length() - structExprParts.get(2).length()) > -1)
								return result.substring((result.length() - (structExprParts.get(1)).length() - structExprParts.get(2).length()), (result.length() - structExprParts.get(2).length())).trim();
							else if ((result.length() - structExprParts.get(1).length()) > 0)
								return result.substring(0, structExprParts.get(1).length()).trim();
							else return result.toString().trim();
						}
						
						//	third subsequent part is border
						else {
							
							//	cut after border
							if (result.toString().toLowerCase().indexOf(structExprParts.get(3).toLowerCase()) > -1) {
								result = result.delete(result.toString().toLowerCase().indexOf(structExprParts.get(3).toLowerCase()), result.length());
								structExprParts.clearFrom(3);
								structExprPartMeanings.clearFrom(3);
							}
							else {
								structExprParts.clearFrom(3);
								structExprPartMeanings.clearFrom(3);
							}
						}
					}
					
					//	second subsequent part is variable length waist
					//	==> StructExpr ambiguous, return empty result
					else if (structExprParts.get(2).startsWith("*") && (structExprPartMeanings.get(2) == 1))	 
						return "";
						
					//	second subsequent part is border
					else {
						
						//	cut after border
						if (result.toString().toLowerCase().indexOf(structExprParts.get(2).toLowerCase()) > -1) {
							result = result.delete(result.toString().toLowerCase().indexOf(structExprParts.get(2).toLowerCase()), result.length());
							structExprParts.clearFrom(2);
							structExprPartMeanings.clearFrom(2);
						}
						else {
							structExprParts.clearFrom(2);
							structExprPartMeanings.clearFrom(2);
						}
					}
				}
				
				//	subsequent part is border
				else {
					
					//	cut before border
					if (result.toString().toLowerCase().indexOf(structExprParts.get(1).toLowerCase()) > -1) {
						result = result.delete(0, (result.toString().toLowerCase().indexOf(structExprParts.get(1).toLowerCase()) + structExprParts.get(1).length()));
						structExprParts.clearTo(2);
						structExprPartMeanings.clearTo(2);
					}
					else {
						structExprParts.clearTo(2);
						structExprPartMeanings.clearTo(2);
					}
				}
			}
			
			//	variable length target
			else if (structExprParts.get(0).equals("§") && (structExprPartMeanings.get(0) == 1)) {
				
				//	data string ends with variable length target ==> return result
				if (structExprParts.size() == 1)
					return result.toString().trim();
					
				//	subsequent part is fix length waist
				else if (structExprParts.get(1).startsWith("+") && (structExprPartMeanings.get(1) == 1)) {
					
					//	data string ends with fix length waist ==> return result
					if (structExprParts.size() == 2)
						return (((result.length() - structExprParts.get(1).length()) > 0) ? result.substring(0, (result.length() - structExprParts.get(1).length())).trim() : "");
						
					//	second subsequent part is border ==> return result
					else return (((result.toString().toLowerCase().indexOf(structExprParts.get(2).toLowerCase()) - structExprParts.get(1).length()) > 0) ? result.substring(0, (result.toString().toLowerCase().indexOf(structExprParts.get(2).toLowerCase()) - structExprParts.get(1).length())).trim() : "");
				}
				
				//	subsequent part is border ==> return result
				else return ((result.toString().toLowerCase().indexOf(structExprParts.get(1).toLowerCase()) > -1) ? result.substring(0, result.toString().toLowerCase().indexOf(structExprParts.get(1).toLowerCase())).trim() : result.toString().trim());
			}
			
			//	fix length target ==> return result
			else if (structExprParts.get(0).startsWith("?") && (structExprPartMeanings.get(0) == 1))
				return ((structExprParts.get(0).length() < result.length()) ? result.substring(0, structExprParts.get(0).length()).trim() : result.toString().trim());
				
			//	fix length waist
			else if (structExprParts.get(0).startsWith("+") && (structExprPartMeanings.get(0) == 1)) {
				
				//	remove waist from beginnig of data
				if (structExprParts.get(0).length() < result.length()) {
					result = result.delete(0, structExprParts.get(0).length());
					structExprParts.removeElementAt(0);
					structExprPartMeanings.removeElementAt(0);
				}
				else result = result.delete(0, result.length());
			}
			
			//	border
			else {
				
				//	remove border from beginning of data
				if (result.toString().toLowerCase().indexOf(structExprParts.get(0).toLowerCase()) == 0) {
					result = result.delete(0, structExprParts.get(0).length());
					structExprParts.removeElementAt(0);
					structExprPartMeanings.removeElementAt(0);
				}
				else {
					structExprParts.removeElementAt(0);
					structExprPartMeanings.removeElementAt(0);
				}
			}
		}
		
		//	if no result, return complete data string
		return dataString.trim();
	}
	
	/**
	 * execute a part of a path query
	 * @param root the root node of the tree to execute the query on
	 * @param queryPart the path query itself (one step down the tree)
	 * @return an array containing the result nodes of the specified query part
	 */
	private static TreeNode[] executeQueryPart(TreeNode root, String queryPart) {
		
		Vector resultNodes = new Vector();
		
		String tag = getTagPart(queryPart);
		String index = getIndexPart(queryPart);
		String textIndex = getTextIndexPart(index);
		
		//	read indices
		IntVector indices = ((tag.startsWith("\"")) ? parseIndices(root.countNodesInSubtree("PcData"), index) : parseIndices(root.getChildNodeCount(), index));
		
		//	evaluate query part
		
		//	relative index
		if (index.startsWith("-") || index.startsWith("+")) {
			if (root.getParent() != null){
				int baseIndex = root.getParent().getTypeSpecificChildNodeIndex(root);
				TreeNode currentNode = root.getParent().getChildNode(root.getNodeType(), (baseIndex + indices.get(0)));
				if (currentNode != null) resultNodes.addElement(currentNode);
			}
		}
		
		//	text tag
		else if (tag.startsWith("\"")) {
			TreeNode[] temp = getAllNodesOfType(root, "PcData");
			Vector matchingNodes = new Vector();
			String pattern = ((tag.length() > 2) ? tag.substring(1, (tag.length() - 1)) : "");
			
			//	apply pattern
			for (int i = 0; i < temp.length; i++) if (matches(pattern, temp[i].getNodeValue())) matchingNodes.addElement(temp[i]);
			
			//	apply indices
			for (int j = 0; j < indices.size(); j++) if (indices.get(j) < matchingNodes.size()) resultNodes.addElement(matchingNodes.get(indices.get(j)));
		}
		
		//	text index
		else if (textIndex.length() > 0) {
			
			//	get nodes
			TreeNode[] resTemp = ((tag.length() > 0) ? root.getChildNodes(tag) : root.getChildNodes());
			Vector resNodes = new Vector();
			
			int i = 0;
			while (i < resTemp.length) {
				TreeNode[] temp = getAllNodesOfType(resTemp[i], "PcData");
				
				//	apply pattern
				boolean match = false;
				for (int j = 0; (j < temp.length) && !match; j++) match = matches(textIndex, temp[i].getNodeValue());
				
				//	check if node with desired content available
				if (match) resNodes.addElement(resTemp[i]);
			}
			
			//	apply inices
			for (int j = 0; j < indices.size(); j++) if (indices.get(j) < resNodes.size()) resultNodes.addElement(resNodes.get(indices.get(j)));
		}
		
		//	regular tag with regular index
		else {
			
			//	get nodes
			TreeNode[] resTemp = ((tag.length() > 0) ? root.getChildNodes(tag) : root.getChildNodes());
			
			//	apply indices
			for (int i = 0; i < indices.size(); i++)
				if (resTemp.length > indices.get(i))
					resultNodes.addElement(resTemp[indices.get(i)]);
		}
		
		//	return result nodes
		return ((TreeNode[]) resultNodes.toArray(new TreeNode[resultNodes.size()]));
	}
	
	/**
	 * parse a query into it's parts
	 * @param query the path query to be parsed
	 * @return the parts of the specified query, such that each part toes one
	 *         step down the tree
	 */
	private static StringVector parseQuery(String query) {
		StringVector queryParts = new StringVector();
		StringBuffer assembler = new StringBuffer();
		
		boolean inQuotas = false;
		char currentChar;
		int escape = -1;
		
		boolean inRegEx = false;
		
		for (int i = 0; (i < query.length()) && !inRegEx; i++) {
			
			//	read next character
			currentChar = query.charAt(i);
			
			//	in quotas
			if (inQuotas) {
				
				//	current character is escaped
				if (i == escape)
					assembler = assembler.append(currentChar);
					
				//	escape next cahacter
				else if (currentChar == '\'') {
					escape = i+1;
					assembler = assembler.append(currentChar);
				}
				
				//	end of quotas
				else if (currentChar == '\"') {
					inQuotas = false;
					assembler = assembler.append(currentChar);
				}
				
				//	common character
				else assembler = assembler.append(currentChar);
			}
				
			//	not in quotas
			else {
				
				//	end of query part
				if (currentChar == '.') {
					queryParts.addElement(assembler.toString().trim());
					assembler = new StringBuffer();
				}
				
				//	start of quotas
				else if (currentChar == '\"') {
					inQuotas = true;
					assembler = assembler.append(currentChar);
				}
				
				//	start of a Structural Expression
				else if (currentChar == '(') {
					inRegEx = true;
					assembler = assembler.append(query.substring(i));
				}
				
				//	common character
				else assembler = assembler.append(currentChar);
			}
		}
		
		//	store last query part
		queryParts.addElement(assembler.toString().trim());
		
		return queryParts;
	}
	
	/**
	 * get the tag part of a part of a path query
	 * @param queryPart the query part to deal with
	 * @return the tag part of the specified query part
	 */
	private static String getTagPart(String queryPart) {
		
		boolean inQuotas = false;
		char currentChar;
		int escape = -1;
		
		//	parse query part
		for (int i = 0; i < queryPart.length(); i++) {
			
			//	read next character
			currentChar = queryPart.charAt(i);
			
			//	in quotas
			if (inQuotas) {
				
				//	current character is escaped
				if (i == escape) {}
					
				//	escape next character
				else if (currentChar == '\'')
					escape = i+1;
					
				//	end of quotas
				else if (currentChar == '\"')
					inQuotas = false;
					
				//	common character
				else {}
			}
			
			//	not in quotas
			else {
				
				//	start of a Structural Expression
				if (currentChar == '(')
					return queryPart.substring(0, i);
					
				//	start of quotas
				else if (currentChar == '\"')
					inQuotas = true;
					
				//	start of index part
				else if (currentChar == '[')
					return queryPart.substring(0, i);
					
				//	common character
				else {}
			}
		}
		
		return queryPart;
	}
	
	/**
	 * get the index part of a part of a path query
	 * @param queryPart the query part to deal with
	 * @return the index part of the specified query part
	 */
	private static String getIndexPart(String queryPart) {
		
		int start = -1;
		int end = -1;
		boolean done = false;
		
		boolean inQuotas = false;
		char currentChar;
		int escape = -1;
		
		//	parse query part
		for (int i = 0; (i < queryPart.length()) && !done; i++) {
			
			//	read next character
			currentChar = queryPart.charAt(i);
			
			//	in quotas
			if (inQuotas) {
				
				//	character is escaped
				if (i == escape) {}
					
				//	escape next character
				else if (currentChar == '\'')
					escape = i+1;
					
				//	end of quotas
				else if (currentChar == '\"')
					inQuotas = false;
					
				//	common character
				else {}
			}
			
			//	not in quotas
			else {
				
				//	start of index part
				if (currentChar == '[')
					start = i+1;
					
				//	start of quotas
				else if (currentChar == '\"')
					inQuotas = true;
					
				//	end of index part
				else if (currentChar == ']') {
					end = i;
					done = true;
				}
				
				//	start of a Structural Expression
				else if (currentChar == '(')
					done = true;
					
				//	common character
				else {}
			}
		}
		if ((start > 0) && (end > start) && (end < (queryPart.length() + 1)))
			return queryPart.substring(start, end);
		else return "";
	}
	
	/**
	 * get the parameter part of a part of a path query
	 * @param queryPart the query part to deal with
	 * @return the parameter part of the specified query part (has an effect
	 *         only for the last part of a query)
	 */
	private static String getParameterPart(String queryPart) {
		
		int start = -1;
		int end = -1;
		boolean done = false;
		
		boolean inQuotas = false;
		char currentChar;
		int escape = -1;
		
		//	parse query part
		for (int i = 0; (i < queryPart.length()) && !done; i++) {
			
			//	read next character
			currentChar = queryPart.charAt(i);
			
			//	in quotas
			if (inQuotas) {
				
				//	character is escaped
				if (i == escape) {}
					
				//	escape next character
				else if (currentChar == '\'')
					escape = i+1;
					
				//	end of quotas
				else if (currentChar == '\"')
					inQuotas = false;
					
				//	common character
				else {}
			}
			
			//	not in quotas
			else {
				
				//	start of parameter part
				if (currentChar == ']')
					start = i+1;
					
				//	start of quotas
				else if (currentChar == '\"')
					inQuotas = true;
					
				//	start of a Structural Expression
				else if (currentChar == '(') {
					end = i;
					done = true;
				}
				
				//	common character
				else {}
			}
		}
		
		if (end == -1)
			end = queryPart.length();
		
		if ((start > 0) && (end > start) && (end < (queryPart.length() + 1)))
			return queryPart.substring(start, end);
		else return "";
	}
	
	/**
	 * get the Structural Expression part of a part of a path query
	 * @param queryPart the query part to deal with
	 * @return the Structural Expression part of the specified query part (has
	 *         an effect only for the last part of a query)
	 */
	private static String getStructExPart(String queryPart) {
		
		boolean inQuotas = false;
		char currentChar;
		int escape = -1;
		
		//	parse query part
		for (int i = 0; i < queryPart.length(); i++) {
			
			//	read next character
			currentChar = queryPart.charAt(i);
			
			//	in quotas
			if (inQuotas) {
				
				//	current character is escaped
				if (i == escape) {}
					
				//	escape next character
				else if (currentChar == '\'')
					escape = i+1;
					
				//	end of quotas
				else if (currentChar == '\"')
					inQuotas = false;
					
				//	common character
				else {}
			}
			
			//	not in quotas
			else {
				
				//	start of Structural Expression
				if (currentChar == '(') {
					try {
						return queryPart.substring((i + 1), (queryPart.length() - 1));
					}
					catch (Exception e) {
						return "";
					}
				}
				
				//	start of quotas
				else if (currentChar == '\"')
					inQuotas = true;
					
				//	common character
				else {}
			}
		}
		
		return "";
	}
	
	/**
	 * parse the index part of a query part
	 * @param max upper limit for wildcard indices
	 * @param indexString the String to parse the indices from
	 * @return the singular indices
	 */
	private static IntVector parseIndices(int max, String indexString) {
		IntVector indices = new IntVector();
		String iString;
		
		//	catch null
		if (indexString == null)
			iString = "";
		else iString = indexString.trim();
		
		//	remove text part if text index
		if (iString.startsWith("\"")) {
			
			//	remove text part
			if (iString.lastIndexOf("\"") < iString.length())
				iString = iString.substring(iString.lastIndexOf("\"") + 1).trim();
			else iString = "";
			
			//	remove leading comma
			if (iString.startsWith(","))
				iString = iString.substring(1).trim();
		}
		
		//	implicit 0-index
		if ((iString == null) || (iString.length() == 0))
			indices.addElement(0);
			
		//	*-index
		else if (iString.startsWith("*")) {
			for (int i = 0; i < max; i++)
				indices.addElement(i);
		}
		
		//	relative index
		else if (iString.startsWith("-") || iString.startsWith("+")) {
			String index = iString;
			
			//	remove subsequent indices
			if (index.indexOf(",") > -1)
				index = index.substring(0, index.indexOf(","));
			
			//	remove leading +
			if (index.startsWith("+"))
				index = index.substring(1);
			
			//	read index
			if (index.length() > 0) {
				try {
					indices.addElement(java.lang.Integer.parseInt(index));
				} catch (Exception e) {}
			}
		}
		
		//	regular indices and index ranges
		else {
			
			//	parse indizes
			String[] indexTokens = IoTools.parseString(iString, ",");
			
			//	handle singular indices and index ranges
			for (int i = 0; i < indexTokens.length; i++) {
				String token = indexTokens[i].trim();
				int indexRangeSeparatorPosition = token.indexOf("-");
				
				//	index range
				if (indexRangeSeparatorPosition > -1) {
					
					//	parse upper and lower limit
					String rangeStart = token.substring(0, indexRangeSeparatorPosition).trim();
					String rangeEnd = "*";
					int lowerIndex = 0;
					int higherIndex = 0;
					
					//	read lower limit
					if (indexRangeSeparatorPosition < (token.length()-1)) {
						rangeEnd = token.substring(indexRangeSeparatorPosition + 1).trim();
					}
					if (rangeStart.length() > 0) {
						try {
							lowerIndex = java.lang.Integer.parseInt(rangeStart);
						}
						catch (Exception e) {
							lowerIndex = 0;
						}
					}
					
					//	read upper limit
					if (rangeEnd.equals("*"))
						higherIndex = max;
					else {
						try {
							higherIndex = java.lang.Integer.parseInt(rangeEnd);
						}
						catch (Exception e) {
							higherIndex = 0;
						}
					}
					
					//	swap uppper and lower limit if necessary
					if (higherIndex < lowerIndex) {
						int store = higherIndex;
						higherIndex = lowerIndex;
						lowerIndex = store;
					}
					
					//	store indices
					for (int j = lowerIndex; j <= higherIndex; j++) indices.addElement(j);
				}
				
				//	singular index
				else {
					if (token.length() > 0) {
						try {
							indices.addElement(java.lang.Integer.parseInt(token));
						} catch (Exception e) {}
					}
				}
			}
		}

		//	sort indices
		for (int i = 0; i < indices.size(); i++) {
			for (int j = 0; j < (indices.size()-1); j++)
				if (indices.get(j) > indices.get(j+1)) indices.insertElementAt(indices.remove(j), (j+1));
		}
		
		//	remove duplicate indices
		int currentIndex = 0;
		while (currentIndex < (indices.size()-1)) {
			if (indices.get(currentIndex) == indices.get(currentIndex+1))
				indices.removeElementAt(currentIndex);
			else currentIndex++;
		}
		
		return indices;
	}
	
	/**
	 * get the text part of the index part of a part of a path query
	 * @param indexString the String to parse the indices from
	 * @return the text index part, if there is one
	 */
	private static String getTextIndexPart(String indexString) {
		
		boolean inQuotas = false;
		char currentChar;
		int escape = -1;
		
		//	parse query part
		for (int i = 0; i < indexString.length(); i++) {
			
			//	read next character
			currentChar = indexString.charAt(i);
			
			//	in quotas
			if (inQuotas) {
				
				//	actaul character is escaped
				if (i == escape) {}
					
				//	escape next character
				else if (currentChar == '\'')
					escape = i+1;
					
				//	end of quotas
				else if (currentChar == '\"')
					return indexString.substring(1, i);
					
				//	common character
				else {}
			}
			
			//	not in quotas
			else {
				
				//	start of quotas
				if (currentChar == '\"')
					inQuotas = true;
					
				//	common character
				else {}
			}
		}
		return "";
	}
	
	/**
	 * check if a String matches a pattern
	 * @param pattern the pattern
	 * @param string the String to be checked
	 * @return true if and only if the specified String matches the specified
	 *         pattern
	 */
	private static boolean matches(String pattern, String string) {
		
		char currentChar;
		int escape = -1;
		
		StringBuffer assembler = new StringBuffer();
		StringVector parts = new StringVector();
		IntVector partMeanings = new IntVector();
		
		//	parse pattern
		for (int i = 0; i < pattern.length(); i++) {
			
			//	read next character
			currentChar = pattern.charAt(i);
			
			//	current character is escaped
			if (i == escape) {
				assembler = assembler.append(currentChar);
				
			//	escape next character
			} else if (currentChar == '\'') {
				escape = i+1;
				
			//	wildcard
			} else if (currentChar == '%') {
				if (assembler.length() > 0) {
					parts.addElement(assembler.toString().trim());
					assembler = new StringBuffer();
					partMeanings.addElement(0);
				}
				parts.addElement("%");
				partMeanings.addElement(1);
				
			//	common character
			} else {
				assembler = assembler.append(currentChar);
			}
		}
		if (assembler.length() > 0) {
			parts.addElement(assembler.toString().trim());
			partMeanings.addElement(0);
		}
		
		//	check string against pattern
		if (parts.size() > 0) {
			
			int cleanIndex = 0;
			String start = parts.get(0);
			int startMeaning = partMeanings.get(0);
			
			//	correct pattern, remove wildcards
			while (cleanIndex < (parts.size()-1)) {
				if (partMeanings.get(cleanIndex) == 1) {
					parts.removeElementAt(cleanIndex);
					partMeanings.removeElementAt(cleanIndex);
				} else if ((partMeanings.get(cleanIndex) == 0) && (partMeanings.get(cleanIndex+1) == 0)) {
					parts.setElementAt((((String) parts.get(cleanIndex)) + ((String) parts.remove(cleanIndex+1))), cleanIndex);
					partMeanings.removeElementAt(cleanIndex);
				} else {
					cleanIndex++;
				}
			}
			if (partMeanings.get(cleanIndex) == 1) {
				parts.removeElementAt(cleanIndex);
				partMeanings.removeElementAt(cleanIndex);
			}
		
			//	apply pattern to String
			boolean match = true;
			int compareIndex = -1;
			if (!(start.equals("%") && (startMeaning == 1)) && !string.startsWith((String) parts.get(0))) {
				return false;
			} else {
				for (int i = 0; (i < parts.size()) && match; i++) {
					compareIndex = string.indexOf(((String) parts.get(i)), (compareIndex+1));
					match = (compareIndex > -1);
				}
			}
			
			return match;
		} else {
			return true;
		}
	}
	
	/**
	 * get the path of a node containing the specified search String in it's
	 * value
	 * @param root the root node of the tree to search in
	 * @param searchValue the value to search
	 * @return the path of the first data node that's value containes the search
	 *         String (in depth first order)
	 */
	public static String getPathOfValue(TreeNode root, String searchValue) {

		TreeNode currentNode = getNodeOfType(root, TreeNode.DATA_NODE_TYPE);

		while (currentNode != null) {
			if (currentNode.getNodeValue().toLowerCase().indexOf(searchValue.toLowerCase()) > -1)
				return getRootPathOfNode(currentNode);
			currentNode = getNodeOfType(root, TreeNode.DATA_NODE_TYPE);
		}
		return "";
	}

	/**
	 * get the path of a node of the the specified type
	 * @param root the root node of the tree to search in
	 * @param searchTag the node type to search
	 * @return the path of the first node that's type matches the specified one
	 *         (in depth first order)
	 */
	public static String getPathOfTag(TreeNode root, String searchTag) {
		return getRootPathOfNode(getNodeOfType(root, searchTag));
	}

	/**
	 * get a node of the the specified type
	 * @param root the root node of the tree to search in
	 * @param searchTag the node type to search
	 * @return the first node that's type matches the specified one (in depth
	 *         first order)
	 */
	public static TreeNode getNodeOfType(TreeNode root, String searchTag) {

		Stack searchStack = new Stack();
		TreeNode currentNode = root;
		int currentNodeIndex = 0;
		
		if (root == null)
			return null;
		
		while (currentNode != null && (currentNode != root || currentNodeIndex < currentNode.getChildNodeCount())) {

			//	return node of desired type if not marked
			if (currentNodeIndex < currentNode.getChildNodeCount() && !currentNode.getChildNode(currentNodeIndex).isMarked() && currentNode.getChildNode(currentNodeIndex).getNodeType().equalsIgnoreCase(searchTag)) {
				currentNode.getChildNode(currentNodeIndex).markNode();
				return currentNode.getChildNode(currentNodeIndex);
			}
			
			//	otherwise continue search
			else {
				
				//	current node has children
				if (currentNodeIndex < currentNode.getChildNodeCount() && currentNode.getChildNode(currentNodeIndex).getChildNodeCount() > 0) {

					//	push index for that level onto the stack, decend and set index to 0
					searchStack.push(new Integer(currentNodeIndex));
					currentNode = currentNode.getChildNode(currentNodeIndex);
					currentNodeIndex = 0;
				}
				
				//	no children
				else {
					
					//	switch to next child node, if there is one
					if (currentNodeIndex < currentNode.getChildNodeCount())
						currentNodeIndex++;
					
					//	mark current node as searched, ascend, get index of the next higher level and switch to next node
					else {
						currentNode.markNode();
						currentNode = currentNode.getParent();
						currentNodeIndex = ((Integer) searchStack.pop()).intValue()+1;
					}
				}
			}
		}
	
		root.unmarkSubtree();
		return null;
	}
	
	/**
	 * get all nodes of the the specified type
	 * @param root the root node of the tree to search in
	 * @param searchTag the node type to search
	 * @return the nodes that's types matche the specified one (packed in an
	 *         array in depth first order)
	 */
	public static TreeNode[] getAllNodesOfType(TreeNode root, String searchTag) {
		TreeNode[] tree = treeToDepthFirstOrder(root);
		Vector nodes = new Vector();
		for (int i = 0; i < tree.length; i++)
			if (tree[i].getNodeType().equalsIgnoreCase(searchTag)) nodes.add(tree[i]);
		return ((TreeNode[]) nodes.toArray(new TreeNode[nodes.size()]));
	}

	/**
	 * get the path leading from the specified node to the root of the tree
	 * @param node the node
	 * @return the path leading from the specified node to the root of the tree
	 */
	public static String getRootPathOfNode(TreeNode node) {

		TreeNode currentNode = node;
		TreeNode parentNode = currentNode.getParent();
		StringBuffer pathAssembler = new StringBuffer();
		
		//	node is not null and not the root of the tree
		if (currentNode != null && parentNode != null) {
			
			//	ascend the tree step by step up to the root and remember the steps
			while (parentNode != null) {
				pathAssembler = pathAssembler.insert(0, currentNode.getNodeType() + "[" + parentNode.getTypeSpecificChildNodeIndex(currentNode) + "].");
				currentNode = parentNode;
				parentNode = parentNode.getParent();
			}
			
			//	remove dot at the end of the path
			pathAssembler = pathAssembler.deleteCharAt(pathAssembler.length()-1);
			return pathAssembler.toString();
		}
		return "";
	}
	
	/**
	 * get the nodes of a tree in depth first order
	 * @param root the root of the tree
	 * @return an array containing the nodes of the tree in depth first order
	 */
	public static TreeNode[] treeToDepthFirstOrder(TreeNode root) {
		
		//	check parameters
		if (root == null) return new TreeNode[0];
		
		Vector treeInOrder = new Vector();
		Stack searchStack = new Stack();
		TreeNode currentNode = root;
		int currentNodeIndex = 0;
		treeInOrder.addElement(root);

		while (currentNode != null && (currentNode != root || currentNodeIndex < currentNode.getChildNodeCount())) {
			
			//	if the current node has children
			if (currentNodeIndex < currentNode.getChildNodeCount() && currentNode.getChildNode(currentNodeIndex).getChildNodeCount() > 0) {
				
				//	push index for that level onto the stack, decend and set index to 0
				searchStack.push(new Integer(currentNodeIndex));
				currentNode = currentNode.getChildNode(currentNodeIndex);
				treeInOrder.addElement(currentNode);
				currentNodeIndex = 0;
			}
			
			//	no children
			else {
				
				//	switch to next child node, if there is one
				if (currentNodeIndex < currentNode.getChildNodeCount()) {
					treeInOrder.addElement(currentNode.getChildNode(currentNodeIndex));
					currentNodeIndex++;
				}
				
				//	no more child nodes to visit
				else {
					
					// ascend, get index of the next higher level and switch to next node
					currentNode = currentNode.getParent();
					if (!searchStack.empty())
						currentNodeIndex = ((Integer) searchStack.pop()).intValue()+1;
				}
			}
		}
		
		//	return nodes
		return ((TreeNode[]) treeInOrder.toArray(new TreeNode[treeInOrder.size()]));
	}
	
	/**
	 * get the nodes of a tree in breadth first order
	 * @param root the root of the tree
	 * @return an array containing the nodes of the tree in breadth first order
	 */
	public static TreeNode[] treeToBreadthFirstOrder(TreeNode root) {

		// check parameter
		if (root == null) return new TreeNode[0];

		Vector treeInOrder = new Vector();
		int currentNodeIndex = 0;
		treeInOrder.addElement(root);

		while (currentNodeIndex < treeInOrder.size()) {

			// asppend all children of the current node to the set of nodes,
			// then switch to next node
			for (int i = 0; i < ((TreeNode) treeInOrder.get(currentNodeIndex)).getChildNodeCount(); i++)
				treeInOrder.addElement(((TreeNode) treeInOrder.get(currentNodeIndex)).getChildNode(i));
			currentNodeIndex++;
		}

		// return nodes
		return ((TreeNode[]) treeInOrder.toArray(new TreeNode[treeInOrder.size()]));
	}

	/**
	 * check if two trees are equal
	 * @param root1 the root node of the first tree
	 * @param root2 the root node of the second tree
	 * @return true if and only if both trees are equal
	 */
	public static boolean treesEqual(TreeNode root1, TreeNode root2) {
		
		TreeNode[] tree1 = treeToDepthFirstOrder(root1);
		TreeNode[] tree2 = treeToDepthFirstOrder(root2);
		int index = 0;
		
		//	compare the trees node by node
		while ((tree1.length == tree2.length) && (index < tree1.length))
			index = ((tree1[index].getNodeType().equalsIgnoreCase(tree2[index].getNodeType())) ? (index + 1) : (tree1.length + 1));
		
		//	return comparison result
		return ((tree1.length == tree2.length) && (index == tree1.length));
	}
	
	/**
	 * check if two trees are equal, considering only nodes affecting the page
	 * layout structure
	 * @param root1 the root node of the first tree
	 * @param root2 the root node of the second tree
	 * @return true if and only if both trees are equal in structure
	 */
	public static boolean treesEqualStruct(TreeNode root1, TreeNode root2) {
		
		TreeNode[] tree1 = removeNonStructNodes(treeToDepthFirstOrder(root1));
		TreeNode[] tree2 = removeNonStructNodes(treeToDepthFirstOrder(root2));
		int index = 0;
		
		//	compare the trees node by node
		while ((tree1.length == tree2.length) && (index < tree1.length))
			index = ((tree1[index].getNodeType().equalsIgnoreCase(tree2[index].getNodeType())) ? (index + 1) : (tree1.length + 1));
		
		//	return comparison result
		return ((tree1.length == tree2.length) && (index == tree1.length));
	}
	
	/**
	 * compute the equality of two trees
	 * @param root1 the root node of the first tree
	 * @param root2 the root node of the second tree
	 * @return the equality of the trees in percent
	 */
	public static int treesEquality(TreeNode root1, TreeNode root2) {
		TreeNode[] tree1 = treeToDepthFirstOrder(root1);
		TreeNode[] tree2 = treeToDepthFirstOrder(root2);

		return compareTrees(tree1, tree2);
	}

	/**
	 * compute the equality of two trees, considering only nodes affecting the
	 * page layout structure
	 * @param root1 the root node of the first tree
	 * @param root2 the root node of the second tree
	 * @return the equality of the trees in percent
	 */
	public static int treesEqualityStruct(TreeNode root1, TreeNode root2) {
		TreeNode[] tree1 = treeToDepthFirstOrder(root1);
		TreeNode[] tree2 = treeToDepthFirstOrder(root2);
		
		removeNonStructNodes(tree1);
		removeNonStructNodes(tree2);
		
		return compareTrees(tree1, tree2);
	}
	
	/**
	 * remove the non-structural (text level layout) nodes
	 * @param nodes the array containing the nodes
	 * @return an array containing all the structural the nodes contained in the
	 *         argument array
	 */
	private static TreeNode[] removeNonStructNodes(TreeNode[] nodes) {
		Vector structNodes = new Vector();
		for (int i = 0; i < nodes.length; i++)
			if (NON_STRUCT_TAGS.indexOf(";" + nodes[i].getNodeType().toLowerCase() + ";") == -1) structNodes.addElement(nodes[i]);
		return ((TreeNode[]) structNodes.toArray(new TreeNode[structNodes.size()]));
	}

	/**
	 * compare two trees hierarchically (the closer to the root node differences
	 * are, the more they reduce equality)
	 * @param root1 the first tree
	 * @param root2 the second tree
	 * @return the match level of the two trees in percent
	 */
	private static int compareTrees(TreeNode[] tree1, TreeNode[] tree2) {
		
		//	check parameters
		if ((tree1 == null) != (tree2 == null)) return 0;
		if ((tree1.length == 0) != (tree2.length == 0)) return 0;
		
		String tree1String;
		String tree2String;
		TreeNodeVector tree1subtreeRoots = new TreeNodeVector();
		TreeNodeVector tree2subtreeRoots = new TreeNodeVector();
		Vector tree1subtrees = new Vector();
		Vector tree2subtrees = new Vector();
		int maxEquality1;
		int maxEquality2;
		
		//	get tree strings
		tree1[0].markSubtree();
		tree2[0].markSubtree();
		tree1String = getMarkedNodeString(tree1);
		tree2String = getMarkedNodeString(tree2);
		tree1[0].unmarkSubtree();
		tree2[0].unmarkSubtree();
		
		//	check if trees are equal
		if (tree1String.equalsIgnoreCase(tree2String)) {
			System.gc();
			return 100;
		}
		
		//	if trees are not equal
		else {
			
			//	if both trees have subtrees contained in the sets of relevant subtrees
			if ((tree1.length > 1) && (tree2.length > 1)) {
				
				//	if one tree is pre-, in- or suffix of the other one, compute equality value directly
				if (tree1String.startsWith(tree2String.substring(0, tree2[0].getNodeType().length())) && (tree1String.indexOf(tree2String.substring(tree2[0].getNodeType().length())) > -1)) {
					System.gc();
					return (int) ((tree2.length * 100) / tree1.length);
				}
				
				else if (tree2String.startsWith(tree1String.substring(0, tree1[0].getNodeType().length())) && (tree2String.indexOf(tree1String.substring(tree1[0].getNodeType().length())) > -1)) {
					System.gc();
					return (int) ((tree1.length * 100) / tree2.length);
				}
				
				//	otherwise
				else {
					
					//	get subtrees
					//	get root nodes of first level subtrees
					for (int i = 0; i < tree1[0].getChildNodeCount(); i++) tree1subtreeRoots.addElement(tree1[0].getChildNode(i));
					for (int i = 0; i < tree2[0].getChildNodeCount(); i++) tree2subtreeRoots.addElement(tree2[0].getChildNode(i));
					
					//	get first level subtrees
					for (int i = 0; i < tree1subtreeRoots.size(); i++) tree1subtrees.addElement(treeToDepthFirstOrder(tree1subtreeRoots.get(i)));
					for (int i = 0; i < tree2subtreeRoots.size(); i++) tree2subtrees.addElement(treeToDepthFirstOrder(tree2subtreeRoots.get(i)));
					
					//	compare subtrees in all possible combinations
					maxEquality1 = compareTreeSets(tree1subtrees, tree2subtrees);
					maxEquality2 = compareTreeSets(tree2subtrees, tree1subtrees);
					
					//	return maximum achieved equality value
					if (maxEquality1 > maxEquality2) {
						System.gc();
						return maxEquality1;
					}
					else {
						System.gc();
						return maxEquality2;
					}
				}
			}
			else {
				System.gc();
				return 0;
			}
		}
	}
	
	/**
	 * concatenate the types of the marked nodes in the array to a String
	 * @param nodes the array containing the nodes
	 * @return the types of the marked nodes in the array concatenated to a
	 *         String
	 */
	private static String getMarkedNodeString(TreeNode[] nodes) {
		StringBuffer assembler = new StringBuffer();
		for (int i = 0; i < nodes.length; i++)
			if (nodes[i].isMarked()) assembler = assembler.append(nodes[i].getNodeType().toLowerCase());
		return assembler.toString();
	}

	/**
	 * compare all possible combinations of two sets of trees in order to find
	 * the best match
	 * @param set1 the first set of trees
	 * @param set2 the second set of trees
	 * @return the maximum match percentage
	 */
	private static int compareTreeSets(Vector set1, Vector set2) {
		
		Vector rootNodeTypes1 = new Vector();
		Vector rootNodeTypes2 = new Vector();
		Vector subSet1 = new Vector();
		Vector subSet2 = new Vector();
		
		int heuristicMaxValue = 0;
		int currentEquality = 0;
		int restEquality = 0;
		int equality = 0;
		int maxEquality = 0;
		
		//	if both sets are not empty
		if (set1.size() > 0 && set2.size() > 0) {
			
			//	get root nodes of all trees in the sets
			for (int i = 0; i < set1.size(); i++) rootNodeTypes1.addElement(((TreeNode[]) set1.get(i))[0].getNodeType());
			for (int i = 0; i < set2.size(); i++) rootNodeTypes2.addElement(((TreeNode[]) set2.get(i))[0].getNodeType());
			
			//	combine all trees in the first set with all in the second
			for (int i = 0; i < rootNodeTypes1.size(); i++){
				for (int j = 0; j < rootNodeTypes2.size(); j++) {
					
					//	compute maximum achievable equality value with current anchors
					if ((set1.size() - i) < (set2.size() - j))
						heuristicMaxValue = ((set1.size() - (i+1)) * 200);
					else heuristicMaxValue = ((set2.size() - (j+1)) * 200);
					
					//	check if there can be a combination with higher equality
					if ((((String) rootNodeTypes1.get(i)).equalsIgnoreCase((String) rootNodeTypes2.get(j))) && (((int) (heuristicMaxValue + 200) / (set1.size() + set2.size())) > maxEquality)) {
						subSet1.clear();
						subSet2.clear();
						
						//	collect all trees following the current anchors
						for (int k = i+1; k < set1.size(); k++) subSet1.addElement(set1.get(k));
						for (int k = j+1; k < set2.size(); k++) subSet2.addElement(set2.get(k));
						
						//	compare current anchor trees
						currentEquality = compareTrees((TreeNode[]) set1.get(i), (TreeNode[]) set2.get(j));
						
						//	if the equality value might be higher than the highes achieved so far
						//	check all combinations af the tree sets folloging the current anchor
						if (((int) ((heuristicMaxValue + (currentEquality * 2)) / (set1.size() + set2.size()))) > maxEquality) {
							restEquality = compareTreeSets(subSet1, subSet2);
							equality = (int) (((restEquality * (subSet1.size() + subSet2.size())) + (currentEquality * 2)) / (set1.size() + set2.size()));
							
							if (equality > maxEquality)
								maxEquality = equality;
						}
					}
				}
			}
			
			//	return maximum achieved equality value
			System.gc();
			return maxEquality;
		}
		
		//	otherwise
		else {
			System.gc();
			return 0;
		}
	}
	
	/**
	 * encode a String so it is displayed in an HTML page a it's handed over to
	 * this method (e.g. replace '<' with '&lt;')
	 * @param string the String to be encoded
	 * @return a String that is displayed in an HTML page a just as the argument
	 *         String would be in a text area
	 */
	public static String encodeSpecialCharacters(String string) {
		//	check parameter
		if ((string == null) || (string.length() == 0)) return "";
		
		//	check each character
		Html grammar = new Html();
		StringBuffer assembler = new StringBuffer();
		int index = 0;
		while (index < string.length()) {
			char currentChar = string.charAt(index);
			
			//	probable start of encoded character
			if (currentChar == '&') {
				StringBuffer codeAssembler = new StringBuffer();
				String code = null;
				int cIndex = 0;
				
				//	check if encoded character
				while (((index + cIndex) < string.length()) && (cIndex < grammar.getCharLookahead()) && (code == null)) {
					codeAssembler.append(string.charAt(index + cIndex));
					if (grammar.isCharCode(codeAssembler.toString()))
						code = codeAssembler.toString();
					cIndex ++;
				}
				
				//	if encoded character identified, append code
				if (code != null) {
					assembler.append(code);
					index += cIndex;
				}
				
				//	else append current character
				else {
					assembler.append(grammar.getCharCode(currentChar));
					index ++;
				}
			}
			
			//	encode current character and append char
			else {
				assembler.append(grammar.getCharCode(currentChar));
				index ++;
			} 
		}

		return assembler.toString();
	}

	/**
	 * concatenate the Strings contained in the specified vector
	 * @param v the Vector
	 * @param separator the separator String
	 * @return the Strings contained in v concatenated with separator in between
	 */
	public static String concatVector(Vector v, String separator) {
		StringBuffer assembler = new StringBuffer();
		for (int i = 0; i < v.size(); i++)
			if (v.get(i) instanceof String) assembler.append(((i > 0) ? separator : "") + ((String) v.get(i)));
		return assembler.toString();
	}

	/**
	 * remove the specified attribute from any node of the specified type
	 * @param root the root node of the page tree to process
	 * @param nodeType the type of the nodes to process (is null is specified
	 *            for this attribute, all nodes of all types will be affected)
	 * @param attribute the attribute to be removed
	 */
	public static void removeAttribute(TreeNode root, String nodeType, String attribute) {
		TreeNode[] nodes = ((nodeType != null) ? getAllNodesOfType(root, nodeType) : treeToBreadthFirstOrder(root));
		for (int i = 0; i < nodes.length; i++)
			nodes[i].removeAttribute(attribute);
	}

	/**
	 * remove the specified attributes from the nodes with the specified types
	 * @param root the root node of the tree to process
	 * @param nodeTypes the types of the nodes to process (is null is specified
	 *            for this attribute, all nodes of all types will be affected)
	 * @param attributes the attributes to be removed
	 */
	public static void removeAttributes(TreeNode root, String[] nodeTypes, String[] attributes) {
		TreeNode[] nodes = treeToBreadthFirstOrder(root);
		HashSet types = new HashSet();
		for (int i = 0; i < nodeTypes.length; i++) types.add(nodeTypes[i].toLowerCase());
		for (int i = 0; i < nodes.length; i++) {
			if ((nodeTypes == null) || types.contains(nodes[i].getNodeType().toLowerCase()))
				for (int j = 0; j < attributes.length; j++) nodes[i].removeAttribute(attributes[j]);
		}
	}

	/**
	 * add the specified attribute with the specified value to all nodes of the
	 * specified type
	 * @param root the root node of the tree to process
	 * @param nodeType the type of the nodes to process (is null is specified
	 *            for this attribute, all nodes of all types will be affected)
	 * @param attribute the attributes to be added
	 * @param value the value to set the attribute to
	 */
	public static void addAttribute(TreeNode root, String nodeType, String attribute, String value) {
		if (root != null) {
			TreeNode[] nodes = ((nodeType != null) ? getAllNodesOfType(root, nodeType) : treeToBreadthFirstOrder(root));
			for (int i = 0; i < nodes.length; i++)
				if (!nodes[i].hasAttribute(attribute)) nodes[i].setAttribute(attribute, value);
		}
	}
	
	private static class TreeNodeVector {
		private Vector vector;
		TreeNodeVector() {
			this.vector = new Vector();
		}
		public void addElement(TreeNode p) {
			vector.addElement(p);
		}
		public TreeNode get(int index) {
			return (TreeNode) vector.get(index);
		}
		public int size() {
			return vector.size();
		}
	}

	private static class IntVector {
		private Vector vector;
		IntVector() {
			this.vector = new Vector();
		}
		public void addElement(int i) {
			vector.addElement(new Integer(i));
		}
		public int get(int index) {
			return ((Int) vector.get(index)).intValue();
		}
		public void insertElementAt(int i, int index) {
			vector.insertElementAt(new Int(i), index);
		}
		public int remove(int index) {
			return ((Int) vector.remove(index)).intValue();
		}
		public void removeElementAt(int index) {
			vector.removeElementAt(index);
		}
		public int size() {
			return vector.size();
		}
		public void clearTo(int i) {
			int currentIndex = i - 1;
			while (this.vector.size() > 0 && currentIndex > 0) {
				this.vector.removeElementAt(0);
				currentIndex--;
			}
		}
		public void clearFrom(int i) {
			while ((this.vector.size() > i) && (this.vector.size() > 0)) this.vector.removeElementAt(i);
		}
	}

	private static class Int {
		private int value;
		Int(int val) {
			this.value = val;
		}
		public int intValue() {
			return this.value;
		}
	}

	private static class StringVector {
		private Vector vector;
		StringVector() {
			this.vector = new Vector();
		}
		public void addElement(String s) {
			vector.addElement(s);
		}
		public String get(int index) {
			return ((String) vector.get(index));
		}
		public String lastElement() {
			return ((String) vector.lastElement());
		}
		public String remove(int index) {
			return ((String) vector.remove(index));
		}
		public void removeElementAt(int index) {
			vector.removeElementAt(index);
		}
		public void setElementAt(String s, int index) {
			vector.setElementAt(s, index);
		}
		public int size() {
			return vector.size();
		}
		public void clearTo(int i) {
			int currentIndex = i - 1;
			while (this.vector.size() > 0 && currentIndex > 0) {
				this.vector.removeElementAt(0);
				currentIndex--;
			}
		}
		public void clearFrom(int i) {
			while ((this.vector.size() > i) && (this.vector.size() > 0)) this.vector.removeElementAt(i);
		}
	}
}
