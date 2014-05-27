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


import java.util.Arrays;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;
import de.uka.ipd.idaho.gamta.util.gScript.GScript;
import de.uka.ipd.idaho.gamta.util.gScript.GScriptAnnotationSetFunction;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * function parquetting the gaps between the annotations in a set with new annotations
 * 
 * @author sautter
 */
public class ParquettFunction extends GScriptAnnotationSetFunction {
	
	private static final String NO_INCLUSION_PARQUETTING_MODE = "-n";
	private static final String START_INCLUSION_PARQUETTING_MODE = "-s";
	private static final String END_INCLUSION_PARQUETTING_MODE = "-e";
	
	private static final String CONTEXT_START_INCLUSION_MODE = "-is";
	private static final String CONTEXT_END_INCLUSION_MODE = "-ie";
	
	private static final String[] parameterNames = {
		"type",
		"mode (optional)"
	};
	
	private static final String[] parameterTypes = {
		GScript.GPATH_STRING_TYPE,
		GScript.GPATH_STRING_TYPE
	};
	
	private static final String[] parameterDescriptions = {
		"the type for the parquetting annotations to create",
		"the parquetting mode (any one of " + NO_INCLUSION_PARQUETTING_MODE + ", " + START_INCLUSION_PARQUETTING_MODE + ", or " + END_INCLUSION_PARQUETTING_MODE + ", plus any of " + CONTEXT_START_INCLUSION_MODE + " or " + CONTEXT_END_INCLUSION_MODE + ", concatenate without space, e.g. " + NO_INCLUSION_PARQUETTING_MODE + CONTEXT_START_INCLUSION_MODE + ") - " +
			"'" + NO_INCLUSION_PARQUETTING_MODE + "' parquetts only the gap (default), " +
			"'" + START_INCLUSION_PARQUETTING_MODE + "' includes the specified Annotations at the start of the newly created ones, " +
			"'" + END_INCLUSION_PARQUETTING_MODE + "' includes them at the end." +
			"'" + CONTEXT_START_INCLUSION_MODE + "' also creates an annotation from the start of the context up to the first separator annotation." +
			"'" + CONTEXT_END_INCLUSION_MODE + "' also creates an annotation from the end of the last separator annotation to the end of the context."
	};
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptFunction#getName()
	 */
	public String getName() {
		return "parquett";
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
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptAnnotationSetFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.Annotation[], de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[])
	 */
	public String process(MutableAnnotation data, Annotation[] annotations, GPathObject[] parameters) {
		if ((parameters.length < 1) || (parameters.length > 2)) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects the following parameters:\n  - " + parameterDescriptions.concatStrings("\n  - "));
		}
		
		Annotation[] sortedAnnotations = new Annotation[annotations.length];
		System.arraycopy(annotations, 0, sortedAnnotations, 0, annotations.length);
		Arrays.sort(sortedAnnotations);
		
		String type = parameters[0].asString().value;
		String mode = NO_INCLUSION_PARQUETTING_MODE;
		boolean includeStart = false;
		boolean includeEnd = false;
		
		if (parameters.length == 2) {
			StringVector parser = new StringVector();
			parser.parseAndAddElements(parameters[1].asString().value, "-");
			parser.removeAll("");
			
			for (int p = 0; p < parser.size(); p++)
				parser.setElementAt(("-" + parser.get(p)), p);
			
			includeStart = parser.contains(CONTEXT_START_INCLUSION_MODE);
			parser.removeAll(CONTEXT_START_INCLUSION_MODE);
			
			includeEnd = parser.contains(CONTEXT_END_INCLUSION_MODE);
			parser.removeAll(CONTEXT_END_INCLUSION_MODE);
			
			if (!parser.isEmpty()) mode = parser.get(0);
		}
		
		int affectedAnnotationCount = 0;
		
		//	create annotation from start up to first separator (if required)
		if (includeStart && (sortedAnnotations.length != 0) && (sortedAnnotations[0].getStartIndex() != 0)) {
			if (END_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode)) {
				data.addAnnotation(type, 0, sortedAnnotations[0].getEndIndex());
				affectedAnnotationCount++;
			}
			else if (NO_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode) || START_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode)) {
				data.addAnnotation(type, 0, sortedAnnotations[0].getStartIndex());
				affectedAnnotationCount++;
			}
			else throw new IllegalArgumentException("Unknown parquetting mode: " + mode);
		}
		
		//	create annotations between separators
		for (int a = 1; a < sortedAnnotations.length; a++) {
			if (this.spanGap(data, sortedAnnotations[a - 1], sortedAnnotations[a], type, mode))
				affectedAnnotationCount++;
		}
		
		//	create annotation from last separator to end (if required)
		if (includeEnd && (sortedAnnotations.length != 0) && (sortedAnnotations[sortedAnnotations.length - 1].getEndIndex() < data.size())) {
			if (START_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode)) {
				data.addAnnotation(type, sortedAnnotations[sortedAnnotations.length - 1].getStartIndex(), (data.size() - sortedAnnotations[sortedAnnotations.length - 1].getStartIndex()));
				affectedAnnotationCount++;
			}
			else if (NO_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode) || END_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode)) {
				data.addAnnotation(type, sortedAnnotations[sortedAnnotations.length - 1].getEndIndex(), (data.size() - sortedAnnotations[sortedAnnotations.length - 1].getEndIndex()));
				affectedAnnotationCount++;
			}
			else throw new IllegalArgumentException("Unknown parquetting mode: " + mode);
		}
		
		//	create annotation from start to end if no separators given
		if ((annotations.length == 0) && (includeStart || includeEnd)) {
			data.addAnnotation(type, 0, data.size());
			affectedAnnotationCount++;
		}
		
		return (affectedAnnotationCount + " Annotations created.");
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.gScript.GScriptAnnotationSetFunction#process(de.uka.ipd.idaho.gamta.MutableAnnotation, de.uka.ipd.idaho.gamta.QueriableAnnotation, de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject[], de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver)
	 */
	public String process(MutableAnnotation data, QueriableAnnotation annotation, GPathObject[] parameters, GPathVariableResolver variables) {
		if ((parameters.length < 1) || (parameters.length > 2)) {
			StringVector parameterDescriptions = new StringVector();
			parameterDescriptions.addContent(this.getParameterDescriptions());
			throw new IllegalArgumentException("'" + this.getName() + "' expects the following parameters:\n  - " + parameterDescriptions.concatStrings("\n  - "));
		}
		return null;
	}
	
	private boolean spanGap(MutableAnnotation data, Annotation a1, Annotation a2, String type, String mode) {
		if (a1.getEndIndex() < a2.getStartIndex()) {
			int startIndex = -1;
			int endIndex = 0;
			if (START_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode)) {
				startIndex = a1.getStartIndex();
				endIndex = a2.getStartIndex();
			}
			else if (END_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode)) {
				startIndex = a1.getEndIndex();
				endIndex = a2.getEndIndex();
			}
			else if (NO_INCLUSION_PARQUETTING_MODE.equalsIgnoreCase(mode)) {
				startIndex = a1.getEndIndex();
				endIndex = a2.getStartIndex();
			}
			else throw new IllegalArgumentException("Unknown parquetting mode: " + mode);
			
			data.addAnnotation(type, startIndex, (endIndex - startIndex));
			return true;
		}
		else return false;
	}
}
