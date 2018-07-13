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
 * An Annotation allowing for retrieving Annotations nested in it
 * 
 * @author sautter
 */
public interface QueriableAnnotation extends Annotation {
	
	/**	@return	the absolute index of this QueriableAnnotation first Token in the backing TokenSequence
	 */
	public abstract int getAbsoluteStartIndex();
	
	/**	@return	the absolute offset of this QueriableAnnotation's first character in the backing char sequence
	 */
	public abstract int getAbsoluteStartOffset();
	
	/**	@return	the Annotations made to this DocumentPart
	 * Note:	If new Annotations are created based on one of this DocumentPart's Annotations, they should be added to this DocumentPart as well, not to one above or below it.
	 * 			This is because the start indices of the Annotations returned by this method are relative to the start index of this DocumentPart 
	 */
	public abstract QueriableAnnotation[] getAnnotations();
	
	/**	get the Annotations of a certain type
	 * @param	type	the type of the desired Annotations (specifying null will return all Annotations, regardless of their types)
	 * @return	the Annotations made to this DocumentPart, restricted to Annotations of the specified type
	 * Note:	If new Annotations are created based on one of this DocumentPart's Annotations, they should be added to this DocumentPart as well, not to one above or below it.
	 * 			This is because the start indices of the Annotations returned by this method are relative to the start index of this DocumentPart 
	 */
	public abstract QueriableAnnotation[] getAnnotations(String type);
	
	/**	@return	the types of all Annotation contained in this QuerieableAnnotation, packed in an array
	 */
	public abstract String[] getAnnotationTypes();
	
	/**	@return	obtain the type-based Annotation nesting order currently valid for this document, as a space-separated string concatenation of annotation types, outmost type first
	 */
	public abstract String getAnnotationNestingOrder();
}
