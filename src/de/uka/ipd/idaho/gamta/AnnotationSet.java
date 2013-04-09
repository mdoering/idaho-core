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
package de.uka.ipd.idaho.gamta;


/**
 * wrapper for a set of Annotations, acting as an Annotation itself. The virtual
 * Annotation spans all Tokens from the underlying TokenSequence starting from
 * the smalles start index of all of the contained Annotations up to the
 * greatest end index of all of the contained Annotations.
 * 
 * The Annotations in the set are always kept sorted in document order.
 * 
 * @author sautter
 */
public interface AnnotationSet extends StandaloneAnnotation {
	
	/**	add an Annotation to this AnnotationSet
	 * @param	annotation	the Annotation to add
	 */
	public abstract void addAnnotation(Annotation annotation);
	
	/**	add all Annotations from an AnnotationSert to this AnnotationSet
	 * @param	annotationSet	the AnnotationSet whose content to add
	 */
	public abstract void addAll(AnnotationSet annotationSet);
	
	/** get the index-th Annotation in this Annotation set, in document order
	 * @param	index	the index of the desired Annotation
	 * @return index-th Annotation in this Annotation set, in document order
	 */
	public abstract Annotation get(int index);
	
	/** get a subset of this AnnotationSet, containg only Annotations of a specific type
	 * @param	type	the desired type
	 * @return an AnnotationSet containing the Annotations of the specified type from this AnnotationSet
	 */
	public abstract AnnotationSet getSubset(String type);
	
	/** remove the index-th Annotation from this Annotation set, in document order
	 * @param	index	the index of the Annotation to be removed
	 * @return index-th Annotation in this Annotation set, in document order
	 */
	public abstract Annotation remove(int index);
	
	/** remove an Annotation from this Annotation set
	 * @param	annotation	the Annotation to be removed
	 * @return true if the specified Annotation was part of this AnnotationSet, false otherwise
	 */
	public abstract boolean remove(Annotation annotation);
	
	/** remove all Annotation in an AnnotastionSet from this Annotation set
	 * @param	annotationSet	the AnnotationSet holding the Annotation to be removed
	 * @return true if this AnnotationSet was modified, false otherwise
	 */
	public abstract boolean removeAll(AnnotationSet annotationSet);
	
	/**	clear this AnnotationSet, dropping all Annotations
	 */
	public abstract void clear();
	
	/**	@return	the number of Annotations in this AnnotationSet
	 * Note: size() returns the number of Tokens spanned 
	 */
	public abstract int annotationCount();
	
	/**	@return	true if and only if there are no Annotations contained in this AnnotationSet
	 */
	public abstract boolean isEmpty();
	
	/**	@return	return the contained Annotations in an Array
	 */
	public abstract Annotation[] toArray();
	
	/**	split this AnnotationSet into a set of AnnotationSets, each containing a set of overlapping or neighboring Annotations
	 * @return an array of AnnotationSets, each containing a set of overlapping Annotations from this AnnotationSet
	 */
	public abstract AnnotationSet[] getSpannedParts();
}
