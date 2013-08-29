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


import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;

/**
 * The interface to implement for custom GANTA Script functions
 * 
 * @author sautter
 */
public interface GScriptFunction {
	
	/**
	 * @return the name to identify this function with
	 */
	public abstract String getName();
	
	/**
	 * @return the names of the parameters this function takes (beside the
	 *         document to process)
	 */
	public abstract String[] getParameterNames();
	
	/**
	 * Retrieve the types of the parameters this function takes (beside the
	 * document to process). The types must be from the GPath (Gamta adaptation
	 * of XPath) type hirarchy (GPathObject - GPathBoolean, GPathNumber,
	 * GPathString, GPathAnnotationSet).
	 * @return the types of the parameters this function takes
	 */
	public abstract String[] getParameterTypes();
	
	/**
	 * Retrieve descriptions of the parameters this function takes (beside the
	 * document to process), each field of the array containing one descriptive
	 * String. The order of the descriptions should be the same as that of the
	 * parameters they belong to in the array returned by getParameterNames().
	 * @return descriptions of the parameters this function takes
	 */
	public abstract String[] getParameterDescriptions();
	
	/**
	 * Execute the function on a document.
	 * @param data the document to process
	 * @param parameters the additional parameters, packed in an array
	 * @param variables the variables currently set
	 * @return some status report, may be null
	 */
	public abstract String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables);
}
