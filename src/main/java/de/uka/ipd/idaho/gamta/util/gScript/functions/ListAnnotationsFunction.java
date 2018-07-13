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


import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gScript.GScript;
import de.uka.ipd.idaho.gamta.util.gScript.GScriptAnnotationSetFunction;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * function listing all annotations in a set to the GScript standard output destination
 * 
 * @author sautter
 */
public class ListAnnotationsFunction extends GScriptAnnotationSetFunction {
	
	private static final String[] parameterNames = {};
	
	private static final String[] parameterTypes = {};
	
	private static final String[] parameterDescriptions = {};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptAnnotationSetFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.QueriableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, QueriableAnnotation annotation, GPathObject[] parameters, GPathVariableResolver variables) {
		if (parameters.length != 0) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects the following parameters:\n  - " + parameterDescriptions.concatStrings("\n  - "));
		}
		
		StringBuffer value = new StringBuffer(AnnotationUtils.produceStartTag(annotation));
		if (annotation.size() <= 10) value.append(annotation.getValue());
		else {
			value.append(annotation.getSubsequence(0, 10).toString());
			value.append(" ...");
		}
		value.append("</" + annotation.getType() + ">");
		GScript.output(value.toString());
		
		return "done";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getName()
	 */
	public String getName() {
		return "list";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterNames()
	 */
	public String[] getSubParameterNames() {
		return parameterNames;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptAnnotationSetFunction#getSubParameterTypes()
	 */
	public String[] getSubParameterTypes() {
		return parameterTypes;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterDescriptions()
	 */
	public String[] getSubParameterDescriptions() {
		return parameterDescriptions;
	}
}
