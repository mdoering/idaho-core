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
package de.uka.ipd.idaho.gamta.util;


import de.uka.ipd.idaho.gamta.QueriableAnnotation;

/**
 * This class implments a generic wrapper for arbitrary queriable annotations.
 * It loops all method calls through to the wrapped annotations. The purpose of
 * this class is to provide a standard wrapper implementation of all the methods
 * in QueriableAnnotation in situations where some few of the methods need to be
 * added functionality through a wrapper class. Wrappers can simply extend this
 * class and overwrite methods as needed while avoiding having to implement all
 * the other methods as well. If Tokens and Annotations retrieved from this
 * wrapper need to be wrapped as well, the respective sub classes of this class
 * should overwrite the wrapToken() and wrapAnnotation() methods to provide the
 * respective wrappers.
 * 
 * @author sautter
 */
public class GenericQueriableAnnotationWrapper extends GenericAnnotationWrapper implements QueriableAnnotation {
	
	/**
	 * the wrapped annotation (equal to the 'annotationData' field of super
	 * class, existing to spare class casts)
	 */
	protected QueriableAnnotation queriableAnnotationData;
	
	/** Constructor
	 * @param	data	the QueriableAnnotation to wrap
	 */
	public GenericQueriableAnnotationWrapper(QueriableAnnotation data) {
		super(data);
		this.queriableAnnotationData = data;
	}
	
	public String getAnnotationNestingOrder() {
		return this.queriableAnnotationData.getAnnotationNestingOrder();
	}

	public int getAbsoluteStartOffset() {
		return this.queriableAnnotationData.getAbsoluteStartOffset();
	}
	
	public int getAbsoluteStartIndex() {
		return this.queriableAnnotationData.getAbsoluteStartIndex();
	}
	
	public QueriableAnnotation[] getAnnotations() {
		QueriableAnnotation[] annotations = this.queriableAnnotationData.getAnnotations();
		for (int a = 0; a < annotations.length; a++)
			annotations[a] = this.wrapAnnotation(annotations[a]);
		return annotations;
	}
	
	public QueriableAnnotation[] getAnnotations(String type) {
		QueriableAnnotation[] annotations = this.queriableAnnotationData.getAnnotations(type);
		for (int a = 0; a < annotations.length; a++)
			annotations[a] = this.wrapAnnotation(annotations[a]);
		return annotations;
	}
	
	public String[] getAnnotationTypes() {
		return this.queriableAnnotationData.getAnnotationTypes();
	}
	
	/** wrap a QueriableAnnotation before returning it in order to provide additional functionality through the wrapper class
	 * Note: This default implementation simply returns the argument QueriableAnnotation, sub classes are welcome to overwrite this method as needed.
	 * @param	annotation	the QueriableAnnotation to wrap
	 * @return the wrapped QueriableAnnotation
	 */
	protected QueriableAnnotation wrapAnnotation(QueriableAnnotation annotation) {
		return annotation;
	}
}
