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
import java.util.HashMap;
import java.util.Properties;

import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.exceptions.InvalidArgumentsException;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.exceptions.UndefinedFunctionException;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.exceptions.UndefinedOperatorException;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.exceptions.XPathException;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathBoolean;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathNodeSet;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathNumber;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathObject;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathString;

/**
 * Engine for executing XPath expressions on trees of TreeNode objects. Supports
 * the complete specification. An instance of this class can be added new,
 * custom functions via the addFunction() method. Those functions will be
 * available only with this specific instance, however. For generally adding
 * functions, use the XPath.addFunction() method.
 * 
 * @author sautter
 */
public class XPathEngine {
	
	private final boolean isDefaultEngine;
	
	/**	Constructor
	 */
	public XPathEngine() {
		this(false);
	}
	
	/**	Constructor
	 */
	XPathEngine(boolean isDefaultEngine) {
		this.isDefaultEngine = isDefaultEngine;
	}
	
	/**
	 * evaluate an XPath query
	 * @param path the XPath to evaluate
	 * @param startNode the TreeNode to start at
	 * @param variableBindings the variable bindings which are currently valid
	 * @return the NodeSet resulting form the evaluation
	 * @throws XPathException
	 */
	public XPathNodeSet evaluatePath(XPath path, TreeNode startNode, Properties variableBindings) throws XPathException {
		if (startNode.getDocumentOrderPosition() == -1)
			startNode.computeDocumentOrderPosition(0);
		XPathNodeSet result = new XPathNodeSet();
		result.add(startNode);
		return evaluatePath(path, result, variableBindings);
	}
	
	/**
	 * evaluate an XPath query
	 * @param path the XPath to evaluate
	 * @param startNodes the NodeSet with the TreeNodes to start at
	 * @param variableBindings the variable bindings which are currently valid
	 * @return the NodeSet resulting form the evaluation
	 * @throws XPathException
	 */
	public XPathNodeSet evaluatePath(XPath path, XPathNodeSet startNodes, Properties variableBindings) throws XPathException {
		XPathNodeSet result = startNodes;
		for (int s = 0; s < path.steps.length; s++)
			result = evaluateStep(path.steps[s], result, variableBindings);
		return result;
	}
	
	/**
	 * evaluate an XPathStep
	 * @param
	 * @param startNodes the NodeSet with the TreeNodes to start at
	 * @param variableBindings the variable bindings which are currently valid
	 * @return the NodeSet resulting form the evaluation
	 * @throws XPathException
	 */
	private XPathNodeSet evaluateStep(XPathStep step, XPathNodeSet startNodes, Properties variableBindings) throws XPathException {
		if (step.axis == null) step.axis = "child";
		XPathNodeSet result = new XPathNodeSet(step.axis.startsWith("preceding") || step.axis.startsWith("ancestor"));
		String filterType;
		if ("node()".equals(step.nodeTest) || "*".equals(step.nodeTest))
			filterType = null;
		else if ("comment()".equals(step.nodeTest))
			filterType = TreeNode.COMMENT_NODE_TYPE;
		else if ("text()".equals(step.nodeTest))
			filterType = TreeNode.DATA_NODE_TYPE;
		else filterType = step.nodeTest;
		
		for (int n = 0; n < startNodes.size(); n++) {
			XPathNodeSet nodeResult = new XPathNodeSet(result.isReverseDocOrder);
			TreeNode node = startNodes.get(n);
			
			//	evaluate axis and node test
			if (step.axis.startsWith("ancestor"))
				nodeResult = getAncestor(node, filterType);
				
			else if (step.axis.startsWith("descendant"))
				nodeResult = getDescendant(node, filterType);
				
			else if (step.axis.startsWith("preceeding")) {
				nodeResult = getPreceedingSibling(node, (step.axis.endsWith("sibling") ? filterType : null));
				if (!step.axis.endsWith("sibling")) {
					XPathNodeSet temp = getAncestor(node, null);
					for (int a = 0; a < temp.size(); a++)
						nodeResult.addAll(getPreceedingSibling(temp.get(a), null));
					temp = new XPathNodeSet(nodeResult.isReverseDocOrder);
					for (int p = 0; p < nodeResult.size(); p++)
						temp.addAll(getDescendant(nodeResult.get(p), filterType));
					nodeResult.addAll(temp);
					nodeResult = nodeResult.filterByType(filterType);
				}
			}
			else if (step.axis.startsWith("following")) {
				nodeResult = getFollowingSibling(node, (step.axis.endsWith("sibling") ? filterType : null));
				if (!step.axis.endsWith("sibling")) {
					XPathNodeSet temp = getAncestor(node, null);
					for (int a = 0; a < temp.size(); a++)
						nodeResult.addAll(getPreceedingSibling(temp.get(a), null));
					temp = new XPathNodeSet(nodeResult.isReverseDocOrder);
					for (int f = 0; f < nodeResult.size(); f++)
						temp.addAll(getDescendant(nodeResult.get(f), filterType));
					nodeResult.addAll(temp);
					nodeResult = nodeResult.filterByType(filterType);
				}
			}
			else if ("parent".equals(step.axis)) {
				TreeNode parent = node.getParent();
				if ((parent != null) && ((filterType == null) || filterType.equals(parent.getNodeType()))) 
					nodeResult.add(parent);
			}
			else if ("child".equals(step.axis)) {
				for (int c = 0; c < node.getChildNodeCount(); c++) {
					TreeNode child = node.getChildNode(c);
					if ((filterType == null) || filterType.equals(child.getNodeType()))
						nodeResult.add(child);
				}
			}
			else if ("attribute".equals(step.axis)) {
				if (filterType == null) {
					String[] attributeNames = node.getAttributeNames();
					for (int a = 0; a < attributeNames.length; a++)
						nodeResult.add(node.getAttributeNode(attributeNames[a]));
				}
				else nodeResult.add(node.getAttributeNode(filterType));
			}
			
			if (step.axis.endsWith("self"))
				nodeResult.add(node);
			
			nodeResult = nodeResult.filterByType(filterType);
			
			//	apply predicates
			if (step.predicates != null) {
				for (int p = 0; p < step.predicates.length; p++)
					nodeResult = applyPredicate(step.predicates[p], nodeResult, variableBindings);
			}
			
			//	copy nodes to result
			result.addAll(nodeResult);
		}
		
		return result;
	}
	
	/**	compute the ancestor axis of a given TreeNode
	 * @param	startNode	the TreeNode to compute the ancestore axis for
	 * @param	filterType	the type of the nodes to include in the resuling NodeSet
	 * @return all TreeNodes that are on the ancestor axis of the specified TreeNode
	 */
	private XPathNodeSet getAncestor(TreeNode startNode, String filterType) {
		XPathNodeSet result = new XPathNodeSet(true);
		TreeNode parent = startNode.getParent();
		while (parent != null) {
			if ((filterType == null) || filterType.equals(parent.getNodeType()))
				result.add(parent);
			parent = parent.getParent();
		}
		return result;
	}
	
	/**	compute the preceeding-sibling axis of a given TreeNode
	 * @param	startNode	the TreeNode to compute the preceeding-sibling axis for
	 * @param	filterType	the type of the nodes to include in the resuling NodeSet
	 * @return all TreeNodes that are on the preceeding-sibling axis of the specified TreeNode
	 */
	private static XPathNodeSet getPreceedingSibling(TreeNode startNode, String filterType) {
		XPathNodeSet result = new XPathNodeSet(true);
		TreeNode parent = startNode.getParent();
		if (parent != null) {
			int index = parent.getChildNodeIndex(startNode);
			if (index != -1) {
				index --;
				while (index != -1) {
					TreeNode node = parent.getChildNode(index);
					if ((filterType == null) || filterType.equals(node.getNodeType()))
						result.add(node);
					index --;
				}
			}
		}
		return result;
	}
	
	/**	compute the following-sibling axis of a given TreeNode
	 * @param	startNode	the TreeNode to compute the following-sibling axis for
	 * @param	filterType	the type of the nodes to include in the resuling NodeSet
	 * @return all TreeNodes that are on the following-sibling axis of the specified TreeNode
	 */
	private XPathNodeSet getFollowingSibling(TreeNode startNode, String filterType) {
		XPathNodeSet result = new XPathNodeSet(false);
		TreeNode parent = startNode.getParent();
		if (parent != null) {
			int index = parent.getChildNodeIndex(startNode);
			if (index != -1) {
				index ++;
				while (index < parent.getChildNodeCount()) {
					TreeNode node = parent.getChildNode(index);
					if ((filterType == null) || filterType.equals(node.getNodeType()))
						result.add(node);
					index ++;
				}
			}
		}
		return result;
	}
	
	/**	compute the descendant axis of a given TreeNode
	 * @param	startNode	the TreeNode to compute the descendant axis for
	 * @param	filterType	the type of the nodes to include in the resuling NodeSet
	 * @return all TreeNodes that are on the descendant axis of the specified TreeNode
	 */
	private XPathNodeSet getDescendant(TreeNode startNode, String filterType) {
		XPathNodeSet result = new XPathNodeSet(false);
		NodeCollector nc = new NodeCollector(result, filterType);
		nc.searchTree(startNode);
		result.remove(startNode);
		return result;
	}
	
	/** class for collecting nodes
	 */
	private class NodeCollector {
		
		private XPathNodeSet nodeSet;
		private String filterType;
		
		private NodeCollector(XPathNodeSet nodeSet, String filterType) {
			this.nodeSet = nodeSet;
			this.filterType = filterType;
		}
		
		private void searchTree(TreeNode node) {
			if (node != null) {
				if ((filterType == null) || filterType.equals(node.getNodeType()))
					nodeSet.add(node);
				TreeNode[] children = node.getChildNodes();
				for (int c = 0; c < children.length; c++)
					this.searchTree(children[c]);
			}
		}
	}
	
	/**	filter a NodeSet according to a predicate expression
	 * @param	nodeSet				the XPathNodeSet to be filtered
	 * @param	axisName			the name of the axis
	 * @param	variableBindings	the current variable bindings
	 * @return
	 */
	private XPathNodeSet applyPredicate(XPathPredicate predicate, XPathNodeSet nodeSet, Properties variableBindings) throws XPathException {
		if (predicate.expression == null) return nodeSet;
		XPathNodeSet resultNodeSet = new XPathNodeSet(nodeSet.isReverseDocOrder);
		int size = nodeSet.size();
		for (int n = 0; n < size; n++) {
			TreeNode node = nodeSet.get(n);
			XPathObject xpo = evaluateExpression(predicate.expression, node, (n + 1), size, variableBindings);
			if (xpo instanceof XPathNumber) {
				if (xpo.asNumber().value == (n + 1))
					resultNodeSet.add(node);
			}
			else if (xpo.asBoolean().value)
				resultNodeSet.add(node);
		}
		return resultNodeSet;
	}
	
	/**	evaluate an XPath expression
	 * @param	expression			the XPath expression to evaluate
	 * @param	contextNode			the context node
	 * @param	contextPosition		the context position
	 * @param	contextSize			the context size
	 * @param	variableBindings	the variable bindings
	 * @return the resulting XPathObject
	 */
	private XPathObject evaluateExpression(XPathExpression expression, TreeNode contextNode, int contextPosition, int contextSize, Properties variableBindings) throws XPathException {
		if (expression instanceof XPathUnaryExpression)
			return evaluateUnaryExpression(((XPathUnaryExpression) expression), contextNode, contextPosition, contextSize, variableBindings);
		else if (expression instanceof XPathBinaryExpression)
			return evaluateBinaryExpression(((XPathBinaryExpression) expression), contextNode, contextPosition, contextSize, variableBindings);
		else return new XPathBoolean(false);
	}
	
	private XPathObject evaluateUnaryExpression(XPathUnaryExpression expression, TreeNode contextNode, int contextPosition, int contextSize, Properties variableBindings) throws XPathException {
		if (expression.literal != null) return expression.literal;
		if (expression.number != null) return (expression.isNegative ? new XPathNumber(-expression.number.value) : expression.number);
		if (expression.variableName != null) return new XPathString(variableBindings.getProperty(expression.variableName));
		if (expression.enclosedExpression != null) {
			XPathObject result = evaluateExpression(expression.enclosedExpression, contextNode, contextPosition, contextSize, variableBindings);
			if (result instanceof XPathNumber)
				return (expression.isNegative ? new XPathNumber(-result.asNumber().value) : result);
			return evaluateExpression(expression.enclosedExpression, contextNode, contextPosition, contextSize, variableBindings);
		}
		if (expression.functionName != null) {
			
			XPathObject[] args = new XPathObject[expression.functionArgs.length];
			for (int a = 0; a < args.length; a++)
				args[a] = evaluateExpression(expression.functionArgs[a], contextNode, contextPosition, contextSize, variableBindings);
			XPathObject xpo = executeFunction(expression.functionName, contextNode, contextPosition, contextSize, args);
			
			if (xpo instanceof XPathNumber)
				return (expression.isNegative ? new XPathNumber(-xpo.asNumber().value) : xpo);
			
			if ((expression.predicates != null) && (expression.predicates.length != 0)) {
				if (!(xpo instanceof XPathNodeSet)) throw new InvalidArgumentsException("Predicates are applicable only for NodeSets.");
				XPathNodeSet nodeSet = ((XPathNodeSet) xpo);
				for (int p = 0; p < expression.predicates.length; p++)
					nodeSet = applyPredicate(expression.predicates[p], nodeSet, variableBindings);
				xpo = nodeSet;
			}
			
			if ((expression.pathExpression != null) && (expression.pathExpression.steps != null) && (expression.pathExpression.steps.length != 0)) {
				if (!(xpo instanceof XPathNodeSet)) throw new InvalidArgumentsException("Path expressions are applicable only for NodeSets.");
				XPathNodeSet nodeSet = ((XPathNodeSet) xpo);
				return evaluatePath(expression.pathExpression, nodeSet, variableBindings);
			}
			
			return xpo;
		}
		if (expression.pathExpression != null)
			return evaluatePath(expression.pathExpression, contextNode, variableBindings);
		if ((expression.partExpressions != null) && (expression.partExpressions.length != 0)) {
			XPathNodeSet result = new XPathNodeSet();
			for (int p = 1; p < expression.partExpressions.length; p++) {
				XPathObject xpo = evaluateExpression(expression.partExpressions[p], contextNode, contextPosition, contextSize, variableBindings);
				if (!(xpo instanceof XPathNodeSet))
					throw new InvalidArgumentsException("Union expressions are applicable only for NodeSets.");
				XPathNodeSet nodeSet = ((XPathNodeSet) xpo);
				for (int n = 0; n < expression.predicates.length; n++)
					result.add(nodeSet.get(n));
			}
			return result;
		}
		return new XPathBoolean(false);
	}
	
	private XPathObject evaluateBinaryExpression(XPathBinaryExpression expression, TreeNode contextNode, int contextPosition, int contextSize, Properties variableBindings) throws XPathException {
		if (expression.leftExpression == null)
			return new XPathBoolean(true);
		
		//	get left result
		XPathObject left = evaluateExpression(expression.leftExpression, contextNode, contextPosition, contextSize, variableBindings);
		if (expression.rightExpression == null)
			return left;
		
		//	evaluate OR and AND operator
		if ("or".equals(expression.operator)) {
			boolean res = left.asBoolean().value;
			if (res)
				return new XPathBoolean(true);
			XPathObject right = evaluateExpression(expression.rightExpression, contextNode, contextPosition, contextSize, variableBindings);
			return new XPathBoolean(right.asBoolean().value);
		}
		else if ("and".equals(expression.operator)) {
			boolean res = left.asBoolean().value;
			if (!res)
				return new XPathBoolean(false);
			XPathObject right = evaluateExpression(expression.rightExpression, contextNode, contextPosition, contextSize, variableBindings);
			return new XPathBoolean(right.asBoolean().value);
		}
		
		//	get right result
		XPathObject right = evaluateExpression(expression.rightExpression, contextNode, contextPosition, contextSize, variableBindings);
		
		//	check if NodeSets involved
		boolean leftIsSet = (left instanceof XPathNodeSet);
		boolean rightIsSet = (right instanceof XPathNodeSet);
		
		//	evaluate = and != operators
		if ("!=".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				XPathString[] leftStrings = XPath.stringValues((XPathNodeSet) left);
				XPathString[] rightStrings = XPath.stringValues((XPathNodeSet) right);
				for (int l = 0; l < leftStrings.length; l++)
					for (int r = 0; r < rightStrings.length; r++)
						if (!leftStrings[l].value.equals(rightStrings[r].value))
							return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (leftIsSet) {
				if (right instanceof XPathBoolean) return new XPathBoolean(right.asBoolean().value != left.asBoolean().value);
				XPathString[] strings = XPath.stringValues((XPathNodeSet) left);
				if (right instanceof XPathNumber) {
					for (int s = 0; s < strings.length; s++)
						if (right.asNumber().value != strings[s].asNumber().value)
							return new XPathBoolean(true);
					return new XPathBoolean(false);
				}
				else {
					for (int s = 0; s < strings.length; s++)
						if (!right.asString().value.equals(strings[s].value))
							return new XPathBoolean(true);
					return new XPathBoolean(false);
				}
			}
			else if (rightIsSet) {
				if (left instanceof XPathBoolean) return new XPathBoolean(left.asBoolean().value != right.asBoolean().value);
				XPathString[] strings = XPath.stringValues((XPathNodeSet) right);
				if (left instanceof XPathNumber) {
					for (int s = 0; s < strings.length; s++)
						if (left.asNumber().value != strings[s].asNumber().value)
							return new XPathBoolean(true);
					return new XPathBoolean(false);
				}
				else {
					for (int s = 0; s < strings.length; s++)
						if (!left.asString().value.equals(strings[s].value))
							return new XPathBoolean(true);
					return new XPathBoolean(false);
				}
			}
			else {
				if ((left instanceof XPathBoolean) || (right instanceof XPathBoolean))
					return new XPathBoolean(left.asBoolean().value != right.asBoolean().value);
				else if ((left instanceof XPathNumber) || (right instanceof XPathNumber))
					return new XPathBoolean(left.asNumber().value != right.asNumber().value);
				else return new XPathBoolean(!left.asString().value.equals(right.asString().value));
			}
		}
		else if ("=".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				XPathString[] leftStrings = XPath.stringValues((XPathNodeSet) left);
				XPathString[] rightStrings = XPath.stringValues((XPathNodeSet) right);
				for (int l = 0; l < leftStrings.length; l++)
					for (int r = 0; r < rightStrings.length; r++)
						if (leftStrings[l].value.equals(rightStrings[r].value))
							return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (leftIsSet) {
				if (right instanceof XPathBoolean) return new XPathBoolean(right.asBoolean().value == left.asBoolean().value);
				XPathString[] strings = XPath.stringValues((XPathNodeSet) left);
				if (right instanceof XPathNumber) {
					for (int s = 0; s < strings.length; s++)
						if (right.asNumber().value == strings[s].asNumber().value)
							return new XPathBoolean(true);
					return new XPathBoolean(false);
				}
				else {
					for (int s = 0; s < strings.length; s++)
						if (right.asString().value.equals(strings[s].value))
							return new XPathBoolean(true);
					return new XPathBoolean(false);
				}
			}
			else if (rightIsSet) {
				if (left instanceof XPathBoolean) return new XPathBoolean(left.asBoolean().value == right.asBoolean().value);
				XPathString[] strings = XPath.stringValues((XPathNodeSet) right);
				if (left instanceof XPathNumber) {
					for (int s = 0; s < strings.length; s++)
						if (left.asNumber().value == strings[s].asNumber().value)
							return new XPathBoolean(true);
					return new XPathBoolean(false);
				}
				else {
					for (int s = 0; s < strings.length; s++)
						if (left.asString().value.equals(strings[s].value))
							return new XPathBoolean(true);
					return new XPathBoolean(false);
				}
			}
			else {
				if ((left instanceof XPathBoolean) || (right instanceof XPathBoolean))
					return new XPathBoolean(left.asBoolean().value == right.asBoolean().value);
				else if ((left instanceof XPathNumber) || (right instanceof XPathNumber))
					return new XPathBoolean(left.asNumber().value == right.asNumber().value);
				else return new XPathBoolean(left.asString().value.equals(right.asString().value));
			}
		}
		
		//	evaluate <, <=, >= and > operators
		if ("<".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				XPathString[] leftStrings = XPath.stringValues((XPathNodeSet) left);
				XPathString[] rightStrings = XPath.stringValues((XPathNodeSet) right);
				for (int l = 0; l < leftStrings.length; l++)
					for (int r = 0; r < rightStrings.length; r++)
						if (leftStrings[l].asNumber().value < rightStrings[r].asNumber().value)
							return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (leftIsSet) {
				XPathString[] strings = XPath.stringValues((XPathNodeSet) left);
				for (int s = 0; s < strings.length; s++)
					if (strings[s].asNumber().value < right.asNumber().value)
						return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (rightIsSet) {
				XPathString[] strings = XPath.stringValues((XPathNodeSet) right);
				for (int s = 0; s < strings.length; s++)
					if (left.asNumber().value < strings[s].asNumber().value)
						return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else return new XPathBoolean(left.asNumber().value < right.asNumber().value);
		}
		else if ("<=".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				XPathString[] leftStrings = XPath.stringValues((XPathNodeSet) left);
				XPathString[] rightStrings = XPath.stringValues((XPathNodeSet) right);
				for (int l = 0; l < leftStrings.length; l++)
					for (int r = 0; r < rightStrings.length; r++)
						if (leftStrings[l].asNumber().value <= rightStrings[r].asNumber().value)
							return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (leftIsSet) {
				XPathString[] strings = XPath.stringValues((XPathNodeSet) left);
				for (int s = 0; s < strings.length; s++)
					if (strings[s].asNumber().value <= right.asNumber().value)
						return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (rightIsSet) {
				XPathString[] strings = XPath.stringValues((XPathNodeSet) right);
				for (int s = 0; s < strings.length; s++)
					if (left.asNumber().value <= strings[s].asNumber().value)
						return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else return new XPathBoolean(left.asNumber().value <= right.asNumber().value);
		}
		else if (">=".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				XPathString[] leftStrings = XPath.stringValues((XPathNodeSet) left);
				XPathString[] rightStrings = XPath.stringValues((XPathNodeSet) right);
				for (int l = 0; l < leftStrings.length; l++)
					for (int r = 0; r < rightStrings.length; r++)
						if (leftStrings[l].asNumber().value >= rightStrings[r].asNumber().value)
							return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (leftIsSet) {
				XPathString[] strings = XPath.stringValues((XPathNodeSet) left);
				for (int s = 0; s < strings.length; s++)
					if (strings[s].asNumber().value >= right.asNumber().value)
						return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (rightIsSet) {
				XPathString[] strings = XPath.stringValues((XPathNodeSet) right);
				for (int s = 0; s < strings.length; s++)
					if (left.asNumber().value >= strings[s].asNumber().value)
						return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else return new XPathBoolean(left.asNumber().value >= right.asNumber().value);
		}
		else if (">".equals(expression.operator)) {
			if (leftIsSet && rightIsSet) {
				XPathString[] leftStrings = XPath.stringValues((XPathNodeSet) left);
				XPathString[] rightStrings = XPath.stringValues((XPathNodeSet) right);
				for (int l = 0; l < leftStrings.length; l++)
					for (int r = 0; r < rightStrings.length; r++)
						if (leftStrings[l].asNumber().value > rightStrings[r].asNumber().value)
							return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (leftIsSet) {
				XPathString[] strings = XPath.stringValues((XPathNodeSet) left);
				for (int s = 0; s < strings.length; s++)
					if (strings[s].asNumber().value > right.asNumber().value)
						return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else if (rightIsSet) {
				XPathString[] strings = XPath.stringValues((XPathNodeSet) right);
				for (int s = 0; s < strings.length; s++)
					if (left.asNumber().value > strings[s].asNumber().value)
						return new XPathBoolean(true);
				return new XPathBoolean(false);
			}
			else return new XPathBoolean(left.asNumber().value > right.asNumber().value);
		}
		
		//	evaluate + and - operators
		if ("+".equals(expression.operator))
			return new XPathNumber(left.asNumber().value + right.asNumber().value);
			
		else if ("-".equals(expression.operator))
			return new XPathNumber(left.asNumber().value - right.asNumber().value);
		
		//	evaluate *, MOD and DIV operators
		if ("*".equals(expression.operator))
			return new XPathNumber(left.asNumber().value * right.asNumber().value);
			
		else if ("mod".equals(expression.operator))
			return new XPathNumber(left.asNumber().value % right.asNumber().value);
			
		else if ("div".equals(expression.operator))
			return new XPathNumber(left.asNumber().value / right.asNumber().value);
		
		throw new UndefinedOperatorException("The operator '" + expression.operator + "' is not defined.");
	}
	
	/**	execute an XPath core function
	 * @param	functionName		the name of the function to be executed
	 * @param	contextNode			the context node
	 * @param	contextPosition		the context position
	 * @param	contextSize			the context size
	 * @param	args				the arguments for the function call
	 * @return the result of the function execution
	 * @throws XPathException
	 */
	private XPathObject executeFunction(String functionName, TreeNode contextNode, int contextPosition, int contextSize, XPathObject[] args) throws XPathException {
		
		//	execute custom function if there is one for the specified name
		if (customFunctions.containsKey(functionName)) try {
			XPathFunction function = ((XPathFunction) customFunctions.get(functionName));
			return function.execute(contextNode, contextPosition, contextSize, args);
		} catch (Exception e) {}
		
		if ("boolean".equalsIgnoreCase(functionName)) {
			if (args.length != 1) throw new InvalidArgumentsException("The function 'boolean' requires 1 argument(s) of type(s) XPathObject.");
			return args[0].asBoolean();
		}
		else if ("contains".equalsIgnoreCase(functionName)) {
			if (args.length != 2) throw new InvalidArgumentsException("The function 'contains' requires 2 argument(s) of type(s) XPathString.");
			return new XPathBoolean(args[0].asString().value.indexOf(args[1].asString().value) != -1);
		}
		else if ("false".equalsIgnoreCase(functionName)) {
			if (args.length != 0) throw new InvalidArgumentsException("The function 'false' requires 0 argument(s).");
			return new XPathBoolean(false);
		}
		else if ("lang".equalsIgnoreCase(functionName)) {
			if ((args.length != 1) || !(args[0] instanceof XPathString)) throw new InvalidArgumentsException("The function 'boolean' requires 1 argument(s) of type(s) XPathString.");
			return new XPathBoolean(true);
		}
		else if ("not".equalsIgnoreCase(functionName))
			return new XPathBoolean(!args[0].asBoolean().value);
			
		else if ("starts-with".equalsIgnoreCase(functionName)) {
			if (args.length != 2) throw new InvalidArgumentsException("The function 'starts-with' requires 2 argument(s) of type(s) XPathString.");
			return new XPathBoolean(args[0].asString().value.startsWith(args[1].asString().value));
		}
		else if ("true".equalsIgnoreCase(functionName)) {
			if (args.length != 0) throw new InvalidArgumentsException("The function 'true' requires 0 argument(s).");
			return new XPathBoolean(true);
		}
		else if ("id".equalsIgnoreCase(functionName)) {
			if (args.length != 1) throw new InvalidArgumentsException("The function 'id' requires 1 argument(s) of type(s) XPathObject.");
			
			TreeNode root = contextNode;
			while (root.getParent() != null) root = root.getParent();
			
			XPathNodeSet nodeSet = new XPathNodeSet();
			ArrayList ids = new ArrayList();
			if (args[0] instanceof XPathNodeSet) {
				XPathNodeSet idNodes = ((XPathNodeSet) args[0]);
				for (int n = 0; n < idNodes.size(); n++) {
					TreeNode[] textNodes = TreeTools.getAllNodesOfType(idNodes.get(n), TreeNode.DATA_NODE_TYPE);
					StringBuffer assembler = new StringBuffer();
					for (int t = 0; t < textNodes.length; t++)
						assembler.append(((t == 0) ? "" : " ") + textNodes[t].getNodeValue());
					ids.add(assembler.toString());
				}
			}
			else ids.add(args[0].asString().value);
			
			for (int i = 0; i < ids.size(); i++) {
				IdBasedNodeCollector nc = new IdBasedNodeCollector(nodeSet, ids.get(i).toString());
				nc.searchTree(root);
			}
			
			return nodeSet;
		}
		else if ("ceiling".equalsIgnoreCase(functionName)) {
			if (args.length != 1) throw new InvalidArgumentsException("The function 'ceiling' requires 1 argument(s) of type(s) XPathNumber.");
			return new XPathNumber(Math.ceil(args[0].asNumber().value));
		}
		else if ("count".equalsIgnoreCase(functionName)) {
			if ((args.length == 1) && (args[0] instanceof XPathNodeSet)) return new XPathNumber(((XPathNodeSet) args[0]).size());
			throw new InvalidArgumentsException("The function 'count' requires 1 argument(s) of type(s) XPathNodeSet.");
		}
		else if ("floor".equalsIgnoreCase(functionName)) {
			if (args.length != 1) throw new InvalidArgumentsException("The function 'floor' requires 1 argument(s) of type(s) XPathNumber.");
			return new XPathNumber(Math.floor(args[0].asNumber().value));
		}
		else if ("last".equalsIgnoreCase(functionName)) {
			if (args.length != 0) throw new InvalidArgumentsException("The function 'last' requires 0 argument(s).");
			return new XPathNumber(contextSize);
		}
		else if ("number".equalsIgnoreCase(functionName)) {
			if (args.length == 0) {
				XPathNodeSet nodeSet = new XPathNodeSet();
				nodeSet.add(contextNode);
				return nodeSet.asNumber();
			}
			else if (args.length == 1) return args[0].asNumber();
			throw new InvalidArgumentsException("The function 'number' requires 0 argument(s) or 1 argument(s) of type(s) XPathObject.");
		}
		else if ("position".equalsIgnoreCase(functionName)) {
			if (args.length != 0) throw new InvalidArgumentsException("The function 'position' requires 0 argument(s).");
			return new XPathNumber(contextPosition);
		}
		else if ("round".equalsIgnoreCase(functionName)) {
			if (args.length != 1) throw new InvalidArgumentsException("The function 'round' requires 1 argument(s) of type(s) XPathNumber.");
			return new XPathNumber(Math.round(args[0].asNumber().value));
		}
		else if ("string-length".equalsIgnoreCase(functionName)) {
			if (args.length == 0) {
				XPathNodeSet nodeSet = new XPathNodeSet();
				nodeSet.add(contextNode);
				return new XPathNumber(nodeSet.asString().value.length());
			}
			else if (args.length == 1) return new XPathNumber(args[0].asString().value.length());
			throw new InvalidArgumentsException("The function 'string-length' requires 0 argument(s) or 1 argument(s) of type(s) XPathString.");
		}
		else if ("sum".equalsIgnoreCase(functionName)) {
			if ((args.length != 1) || !(args[0] instanceof XPathNodeSet)) throw new InvalidArgumentsException("The function 'sum' requires 1 argument(s) of type(s) XPathNodeSet.");
			
			XPathNodeSet nodeSet = ((XPathNodeSet) args[0]);
			double sum = 0;
			for (int s = 0; s < nodeSet.size(); s++) {
				TreeNode[] nodes = TreeTools.getAllNodesOfType(nodeSet.get(s), TreeNode.DATA_NODE_TYPE);
				StringBuffer assembler = new StringBuffer();
				for (int n = 0; n < nodes.length; n++)
					assembler.append(((n == 0) ? "" : " ") + nodes[n].getNodeValue());
				sum += new XPathString(assembler.toString()).asNumber().value;
			}
			return new XPathNumber(sum);
		}
		else if ("concat".equalsIgnoreCase(functionName)) {
			if (args.length < 2) throw new InvalidArgumentsException("The function 'concat' requires 2 or more argument(s) of type(s) XPathString.");
			
			StringBuffer assembler = new StringBuffer("");
			for (int a = 0; a < args.length; a++) {
				if (!(args[a] instanceof XPathString) && false) throw new InvalidArgumentsException("The function 'concat' requires 2 or more argument(s) of type(s) XPathString.");
				assembler.append(((a == 0) ? "" : " ") + args[a].asString().value);
			}
			return new XPathString(assembler.toString());
		}
		else if ("local-name".equalsIgnoreCase(functionName)) {
			if (args.length == 0) return new XPathString(contextNode.getNodeType());
			else if ((args.length == 1) && (args[0] instanceof XPathNodeSet)) {
				XPathNodeSet gpas = ((XPathNodeSet) args[0]);
				return new XPathString(gpas.isEmpty() ? "" : gpas.getFirst().getNodeType());
			}
			throw new InvalidArgumentsException("The function 'local-name' requires 0 argument(s) or 1 argument(s) of type(s) XPathNodeSet.");
		}
		else if ("name".equalsIgnoreCase(functionName)) {
			if (args.length == 0) return new XPathString(contextNode.getNodeType());
			else if ((args.length == 1) && (args[0] instanceof XPathNodeSet)) {
				XPathNodeSet gpas = ((XPathNodeSet) args[0]);
				return new XPathString(gpas.isEmpty() ? "" : gpas.getFirst().getNodeType());
			}
			throw new InvalidArgumentsException("The function 'name' requires 0 argument(s) or 1 argument(s) of type(s) XPathNodeSet.");
		}
		else if ("namespace-uri".equalsIgnoreCase(functionName)) {
			if (args.length == 0) return new XPathString("This implementation uses generic namespaces.");
			else if (args[0] instanceof XPathNodeSet) return new XPathString("This implementation uses generic namespaces.");
			throw new InvalidArgumentsException("The function 'namespace-uri' requires 0 argument(s) or 1 argument(s) of type(s) XPathNodeSet.");
		}
		else if ("normalize-space".equalsIgnoreCase(functionName)) {
			String toNormalize;
			if (args.length == 0) {
				XPathNodeSet nodeSet = new XPathNodeSet();
				nodeSet.add(contextNode);
				toNormalize = nodeSet.asString().value;
			} 
			else if (args.length == 1)
				toNormalize = args[0].asString().value;
			else throw new InvalidArgumentsException("The function 'normalize-space' requires 0 argument(s) or 1 argument(s) of type(s) XPathString.");
			
			boolean lastWasWhitespace = false;
			StringBuffer assembler = new StringBuffer();
			for (int s = 0; s < toNormalize.length(); s++) {
				char c = toNormalize.charAt(s);
				if (c > 32) {
					assembler.append(c);
					lastWasWhitespace = false;
				}
				else if (!lastWasWhitespace) {
					assembler.append(c);
					lastWasWhitespace = true;
				}
			}
			
			return new XPathString(assembler.toString().trim());
		}
		else if ("string".equalsIgnoreCase(functionName)) {
			if (args.length == 0) {
				XPathNodeSet nodeSet = new XPathNodeSet();
				nodeSet.add(contextNode);
				return nodeSet.asString();
			}
			else if (args.length == 1) return args[0].asString();
			throw new InvalidArgumentsException("The function 'string' requires 0 argument(s) or 1 argument(s) of type(s) XPathObject.");
		}
		else if ("substring".equalsIgnoreCase(functionName)) {
			if ((args.length != 2) && (args.length != 3)) throw new InvalidArgumentsException("The function 'substring' requires 2 or 3 argument(s) of type(s) XPathString, XPathNumber" + ((args.length > 3) ? ", XPathNumber" : "") + ".");
			if (!(args[1] instanceof XPathNumber)) throw new InvalidArgumentsException("The function 'substring' requires 2 or 3 argument(s) of type(s) XPathString, XPathNumber" + ((args.length == 3) ? ", XPathNumber" : "") + ".");
			if ((args.length == 3) && !(args[2] instanceof XPathNumber)) throw new InvalidArgumentsException("The function 'substring' requires 2 or 3 argument(s) of type(s) XPathString, XPathNumber" + ((args.length == 3) ? ", XPathNumber" : "") + ".");
			
			int s = 0;
			int l = args[0].asString().value.length();
			try {
				s = ((int) args[1].asNumber().value);
				if (s < 0) s = 0;
				if (s > l) return new XPathString("");
			} catch (Exception e) {}
			if (args.length == 3) try {
				l = ((int) args[2].asNumber().value);
				if (l < 1) return new XPathString("");
			} catch (Exception e) {}
			if ((s + l) > args[0].asString().value.length()) l = args[0].asString().value.length() - s;
			return new XPathString(args[0].asString().value.substring(s, (s + l)));
		}
		else if ("substring-after".equalsIgnoreCase(functionName)) {
			if (args.length != 2) throw new InvalidArgumentsException("The function 'substring-after' requires 2 argument(s) of type(s) XPathString.");
			
			int s = args[0].asString().value.indexOf(args[1].asString().value);
			if (s == -1) return new XPathString("");
			s += args[1].asString().value.length();
			if (s >= args[0].asString().value.length()) return new XPathString("");
			return new XPathString(args[0].asString().value.substring(s));
		}
		else if ("substring-before".equalsIgnoreCase(functionName)) {
			if (args.length != 2) throw new InvalidArgumentsException("The function 'substring-before' requires 2 argument(s) of type(s) XPathString.");
			
			int l = args[0].asString().value.indexOf(args[1].asString().value);
			if (l == -1) return new XPathString("");
			return new XPathString(args[0].asString().value.substring(0, l));
		}
		else if ("translate".equalsIgnoreCase(functionName)) {
			if (args.length != 3) throw new InvalidArgumentsException("The function 'translate' requires 3 argument(s) of type(s) XPathString.");
			
			String originals = args[1].asString().value;
			String translations = args[2].asString().value;
			Properties dictionary = new Properties();
			for (int i = 0; i < originals.length(); i++) {
				String o = originals.substring(i, (i + 1));
				String t = ((i < translations.length()) ? translations.substring(i, (i + 1)) : "");
				dictionary.setProperty(o, t);
			}
			
			String string = args[0].asString().value;
			StringBuffer assembler = new StringBuffer();
			for (int s = 0; s < string.length(); s++) {
				String o = string.substring(s, (s + 1));
				assembler.append(dictionary.getProperty(o, o));
			}
			
			return new XPathString(assembler.toString());
		}
		
		if (this.isDefaultEngine) throw new UndefinedFunctionException("The function '" + functionName + "' is not defined.");
		
		return XPath.DEFAULT_ENGINE.executeFunction(functionName, contextNode, contextPosition, contextSize, args);
	}
	
	//	register for custom functions
	private final HashMap customFunctions = new HashMap();
	
	/**	add a custom function to the function library of this XPathEngine (will overwrite the original function if it has the same name, the same is true for equally named custom functions in the default function library)
	 * @param	functionName	the name the function will be invocable with in XPath queries
	 * @param	function		the XPathFunction to be added
	 */
	public void addFunction(String functionName, XPathFunction function) {
		if ((functionName != null) && (function != null))
			customFunctions.put(functionName, function);
	}
	
	/**	remove a custom function from the function library of this XPathEngine
	 * @param	functionName	the name of the function to be removed
	 * @return the function that was just removed, or null if there was no function with the specified name
	 */
	public XPathFunction removeFunction(String functionName) {
		return ((XPathFunction) customFunctions.remove(functionName));
	}
	
	/**	representation of IDref-based node selection
	 */
	private class IdBasedNodeCollector {
		
		private XPathNodeSet nodeSet;
		private String searchID;
		
		private IdBasedNodeCollector(XPathNodeSet nodeSet, String searchID) {
			this.nodeSet = nodeSet;
			this.searchID = searchID;
		}
		
		private void searchTree(TreeNode node) {
			if (node != null) {
				if (this.storeNode(node)) this.nodeSet.add(node);
				TreeNode[] children = node.getChildNodes();
				for (int c = 0; c < children.length; c++) this.searchTree(children[c]);
			}
		}
		
		private boolean storeNode(TreeNode node) {
			return node.getAttribute("id", "").equals(this.searchID);
		}
	}
}
