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


import java.util.Properties;

import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.exceptions.XPathException;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathNodeSet;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathString;

/**
 * An XPath expression, as defined in the XPath specification. The static
 * execute() methods use an internal default instance of XPathEngine. Adding a
 * function via the addFunction() method will result in this function being
 * available in all XPathEngines throughout the JVM.
 * 
 * @author sautter
 */
public class XPath {
	
	XPathStep[] steps = new XPathStep[0];
	
	/**	Constructor
	 */
	XPath() {}
	
	/**	Constructor
	 * @param	path	the string representation of the XPath expression to create (will be paresed automatically)
	 */
	public XPath(String path) {
		XPath xPath = XPathParser.parsePath(path);
		this.steps = xPath.steps;
		xPath.steps = null;
	}
	
	/**	evaluate the XPath query
	 * @param	startNode			the TreeNode to start at
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return the NodeSet resulting form the evaluation
	 * @throws XPathException
	 */
	public XPathNodeSet evaluate(TreeNode startNode, Properties variableBindings) throws XPathException {
		return evaluatePath(this, startNode, variableBindings);
	}
	
	/**	evaluate the XPath query
	 * @param	startNodes			the NodeSet with the TreeNodes to start at
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return the NodeSet resulting form the evaluation
	 * @throws XPathException
	 */
	public XPathNodeSet evaluate(XPathNodeSet startNodes, Properties variableBindings) throws XPathException {
		return evaluatePath(this, startNodes, variableBindings);
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
	
	public static final XPathEngine DEFAULT_ENGINE = new XPathEngine(true);
	
	/**	add a custom function to the default function library (will overwrite the original function if it has the same name)
	 * @param	functionName	the name the function will be invocable with in XPath queries
	 * @param	function		the XPathFunction to be added
	 */
	public static void addFunction(String functionName, XPathFunction function) {
		if ((functionName != null) && (function != null))
			DEFAULT_ENGINE.addFunction(functionName, function);
	}

	/**	remove a custom function from the default function library
	 * @param	functionName	the name of the function to be removed
	 * @return the function that was just removed, or null if there was no function with the specified name
	 */
	public static XPathFunction removeFunction(String functionName) {
		return DEFAULT_ENGINE.removeFunction(functionName);
	}

	/**	compute the String value of a given TreeNode
	 * @param	node	the node the String value of which to compute
	 * @return the String value of the specified node as an XPathString
	 */
	public static XPathString stringValue(TreeNode node) {
		if (node == null) return new XPathString("");
		if (node.isAttributeNode()) return new XPathString(node.getNodeValue());
		TreeNode[] nodes = TreeTools.getAllNodesOfType(node, TreeNode.DATA_NODE_TYPE);
		StringBuffer assembler = new StringBuffer();
		for (int n = 0; n < nodes.length; n++)
			assembler.append(((n == 0) ? "" : " ") + nodes[n].getNodeValue());
		return new XPathString(assembler.toString());
	}

	/**	compute the String values of the TreeNodes in a given NodeSet
	 * @param	nodeSet		the NodeSet containing the TreeNodes the String values of which are to compute
	 * @return the String values of the specified nodes as an array of XPathStrings
	 */
	public static XPathString[] stringValues(XPathNodeSet nodeSet) {
		if (nodeSet == null) return new XPathString[0];
		XPathString[] strings = new XPathString[nodeSet.size()];
		for (int n = 0; n < nodeSet.size(); n++)
			strings[n] = stringValue(nodeSet.get(n));
		return strings;
	}

	/**	evaluate an XPath query
	 * @param	path				the XPath to evaluate
	 * @param	startNode			the TreeNode to start at
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return the NodeSet resulting form the evaluation
	 * @throws XPathException
	 */
	public static XPathNodeSet evaluatePath(XPath path, TreeNode startNode, Properties variableBindings) throws XPathException {
		startNode.computeDocumentOrderPosition(0);
		return DEFAULT_ENGINE.evaluatePath(path, startNode, variableBindings);
	}

	/**	evaluate an XPath query
	 * @param	path				the XPath to evaluate
	 * @param	startNodes			the NodeSet with the TreeNodes to start at
	 * @param	variableBindings	the variable bindings which are currently valid
	 * @return the NodeSet resulting form the evaluation
	 * @throws XPathException
	 */
	public static XPathNodeSet evaluatePath(XPath path, XPathNodeSet startNodes, Properties variableBindings) throws XPathException {
		return DEFAULT_ENGINE.evaluatePath(path, startNodes, variableBindings);
	}
}
