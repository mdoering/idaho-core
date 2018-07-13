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


import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathAnnotationSet;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gScript.GScript;
import de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * 
 * @author sautter
 *
 */
public class CopyAttributesFunction implements GScriptFunction {
	
	private static final String[] parameterNames = {
		"source",
		"target"
	};
	
	private static final String[] parameterTypes = {
		GScript.GPATH_ANNOTATION_SET_TYPE,
		GScript.GPATH_ANNOTATION_SET_TYPE
	};
	
	private static final String[] parameterDescriptions = {
		"the set containing the Annotations from the first of which to take the attributes. If this set is empty, this function does nothing.",
		"the set containing the Annotations to copy the attributes to."
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if (parameters.length != 2) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects the following parameters:\n  - " + parameterDescriptions.concatStrings("\n  - "));
		}
		
		Annotation source;
		if (parameters[0] instanceof GPathAnnotationSet) {
			GPathAnnotationSet sourceSet = ((GPathAnnotationSet) parameters[0]);
			if (sourceSet.isEmpty())
				return "0 Annotations affected.";
			else source = sourceSet.get(0);
		}
		else {
			Annotation[] annotations = GPath.evaluatePath(data, parameters[0].asString().value, GPath.getDummyVariableResolver());
			if (annotations.length == 0)
				return "0 Annotations affected.";
			else source = annotations[0];
		}
		if (parameters[1] instanceof GPathAnnotationSet) {
			GPathAnnotationSet targetSet = ((GPathAnnotationSet) parameters[1]);
			for (int a = 0; a < targetSet.size(); a++)
				targetSet.get(a).copyAttributes(source);
			return (targetSet.size() + " Annotations affected.");
		}
		else {
			Annotation[] annotations = GPath.evaluatePath(data, parameters[1].asString().value, GPath.getDummyVariableResolver());
			for (int a = 0; a < annotations.length; a++)
				annotations[a].copyAttributes(source);
			return (annotations.length + " Annotations affected.");
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getName()
	 */
	public String getName() {
		return "copyAttributes";
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
