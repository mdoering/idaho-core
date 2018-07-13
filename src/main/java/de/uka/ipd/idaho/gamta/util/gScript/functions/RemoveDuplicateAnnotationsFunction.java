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
package de.uka.ipd.idaho.gamta.util.gScript.functions;


import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnnotationFilter;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gScript.GScript;
import de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * function removing duplicate annotations from the context document
 * 
 * @author sautter
 */
public class RemoveDuplicateAnnotationsFunction implements GScriptFunction {
	
	private static final String[] parameterNames = {
		"type (optional)"
	};
	
	private static final String[] parameterTypes = {
		GScript.GPATH_STRING_TYPE
	};
	
	private static final String[] parameterDescriptions = {
		"the type of annotations whose duplicates to remove (optional, if not specified, duplicate elimination will be applied to all annotations)"
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if (parameters.length > 1) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects the following parameters:\n  - " + parameterDescriptions.concatStrings("\n  - "));
		}
		
		String type = ((parameters.length == 0) ? null : parameters[0].asString().value);
		int before = data.getAnnotations(type).length;
		AnnotationFilter.removeDuplicates(data, type);
		int after = data.getAnnotations(type).length;
		
		return ((before - after) + " Annotations removed.");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getName()
	 */
	public String getName() {
		return "removeDuplicates";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterNames()
	 */
	public String[] getParameterNames() {
		return parameterNames;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterTypes()
	 */
	public String[] getParameterTypes() {
		return parameterTypes;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterDescriptions()
	 */
	public String[] getParameterDescriptions() {
		return parameterDescriptions;
	}
}
