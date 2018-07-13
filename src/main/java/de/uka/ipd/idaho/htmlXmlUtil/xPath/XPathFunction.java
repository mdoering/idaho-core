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


import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.exceptions.XPathException;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathObject;

/**
 * Interface to implement for a custom XPath function to add to an XPathEngine.
 * If a function is added via the XPath.addFunction() method, it is available in
 * all instances of XPathEngine in the JVM. If it is added to an individual
 * XPathEngine via the XPathEngine.adddFunction() method, in contrast, it is
 * available only in that engine. This is due to the way XPathEngine instances
 * execute functions:
 * <ul>
 * <li>first, a function is searched for in the engine's own repository of
 * added functions.</li>
 * <li>second, the functions defined in the XPath specification are searched
 * through</li>
 * <li>third, the default engine is asked to execute the function</li>
 * <li>if all this fails, i.e. no definition of the function is found, the
 * XPathEngine instance executing the function throws an
 * UndefinedFunctionException</li>
 * </ul>
 * This mechanism also facilitates overwriting functions: Adding a function
 * named equally to one defined in the specification will replace this
 * function's default definition with the custom one, since th lookup in the
 * custom functions is first.
 * 
 * @author sautter
 */
public interface XPathFunction {
	
	/**	custom XPathFunction (can be added to XPathFunctions via the addFunction() method)
	 * @param	contextNode			the context node
	 * @param	contextPosition		the context position
	 * @param	contextSize			the context size
	 * @param	args				the arguments for the function call
	 * @return the result of the function execution
	 * @throws XPathException
	 */
	public abstract XPathObject execute(TreeNode contextNode, int contextPosition, int contextSize, XPathObject[] args) throws XPathException;
}
