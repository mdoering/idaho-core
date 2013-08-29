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
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathAnnotationSet;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 *
 */
public abstract class GScriptAnnotationSetFunction implements GScriptFunction {
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterNames()
	 */
	public String[] getParameterNames() {
		String[] subParameterNames = this.getSubParameterNames();
		String[] parameterNames = new String[1 + subParameterNames.length];
		parameterNames[0] = "annotationSet";
		System.arraycopy(subParameterNames, 0, parameterNames, 1, subParameterNames.length);
		return parameterNames;
	}
	
	/**
	 * @return the names of the parameters this function takes beside the
	 *         annotation set to process
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterNames()
	 */
	public abstract String[] getSubParameterNames();
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterTypes()
	 */
	public String[] getParameterTypes() {
		String[] subParameterNames = this.getSubParameterTypes();
		String[] parameterNames = new String[1 + subParameterNames.length];
		parameterNames[0] = GScript.GPATH_ANNOTATION_SET_TYPE;
		System.arraycopy(subParameterNames, 0, parameterNames, 1, subParameterNames.length);
		return parameterNames;
	}
	
	/**
	 * Retrieve the types of the parameters this function takes beside the
	 * annotatuion set to process. The types must be from the GPath (Gamta
	 * adaptation of XPath) type hirarchy (GPathObject - GPathBoolean,
	 * GPathNumber, GPathString, GPathAnnotationSet).
	 * @return the types of the parameters this function takes
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterTypes()
	 */
	public abstract String[] getSubParameterTypes();
	
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterDescriptions()
	 */
	public String[] getParameterDescriptions() {
		String[] subParameterDescriptions = this.getSubParameterDescriptions();
		String[] parameterDescriptions = new String[1 + subParameterDescriptions.length];
		parameterDescriptions[0] = "a GPathAnnotationSet holding the Annotations to process";
		System.arraycopy(subParameterDescriptions, 0, parameterDescriptions, 1, subParameterDescriptions.length);
		return parameterDescriptions;
	}
	
	/**
	 * Retrieve descriptions of the parameters this function takes beside the
	 * annotation set to process, each field of the array containing one
	 * descriptive String. The order of the descriptions should be the same as
	 * that of the parameters they belong to in the array returned by
	 * getSubParameterNames().
	 * @return descriptions of the parameters this function takes
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getParameterDescriptions()
	 */
	public abstract String[] getSubParameterDescriptions();
	
	
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, GPathObject[] parameters, GPathVariableResolver variables) {
		if (parameters.length == 0) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects the following parameters:\n  - " + parameterDescriptions.concatStrings("\n  - "));
		}
		
		QueriableAnnotation[] annotations;
		if (parameters[0] instanceof GPathAnnotationSet) {
			GPathAnnotationSet gpas = ((GPathAnnotationSet) parameters[0]);
			annotations = new QueriableAnnotation[gpas.size()];
			for (int a = 0; a < gpas.size(); a++) annotations[a] = gpas.get(a);
		}
		else annotations = GPath.evaluatePath(data, parameters[0].asString().value, variables);
		
		GPathObject[] subParameters = new GPathObject[parameters.length - 1];
		System.arraycopy(parameters, 1, subParameters, 0, subParameters.length);
		
		return this.process(data, annotations, subParameters, variables);
	}
	
	/**
	 * Execute the function on a set of annotations.
	 * @param data the document the annotations to process belong to
	 * @param annotations the annotations to process
	 * @param parameters the additional parameters, packed in an array
	 * @param variables the variables currently set
	 * @return some status report, may be null
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, QueriableAnnotation[] annotations, GPathObject[] parameters, GPathVariableResolver variables) {
		int affectedAnnotationCount = 0;
		for (int a = 0; a < annotations.length; a++) {
			GPathAnnotationSet gpas = new GPathAnnotationSet();
			gpas.add(annotations[a]);
			variables.setVariable("$1", gpas);
			if (this.process(data, annotations[a], parameters, variables) != null)
				affectedAnnotationCount ++;
		}
		return (affectedAnnotationCount + " Annotations affected.");
	}
	
	/**
	 * Process an individual Annotation. The annotation being processed is
	 * contained in a one element annotation set bound to the inline variable
	 * '$1'.
	 * @param data the document in whose context to process the specified
	 *            annotation
	 * @param annotation the Annotation to process
	 * @param parameters the parameters
	 * @param variables the variables currently set
	 * @return some status report, may be null
	 */
	public abstract String process(MutableAnnotation data, QueriableAnnotation annotation, GPathObject[] parameters, GPathVariableResolver variables);
}
