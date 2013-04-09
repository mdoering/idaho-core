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
 * the root element of an Annotation hierarchy, forming a standalone document.
 * In general, any implementation of MutableAnnotation that is capable of
 * standalone existence base on a mutable char sequence can be such a root. But
 * a DocumentRoot can also define a document-wide nesting order for Annotations
 * based on their type, and can have properties that are inherited to all
 * Annotations belonging to this DocumentRoot
 */
public interface DocumentRoot extends MutableAnnotation {
	
	/**	the type of a DocumentRoot, to enable distinction from other Annotations */
	public static final String DOCUMENT_TYPE = "document";
	
	/**	the ID attribute of a DocumentRoot */
	public static final String DOCUMENT_ID_ATTRIBUTE = "docId";
	
	/**	the attribute holding a String representation of the annotation nesting order for this DocumentRoot */
	public static final String ANNOTATION_NESTING_ORDER_ATTRIBUTE = "annotationNestingOrder";
	
	/** default annotation nesting order, as a space-separated string concatenation of annotation types, outmost type first */
	public static final String DEFAULT_ANNOTATION_NESTING_ORDER = DOCUMENT_TYPE + " " + SECTION_TYPE + " " + SUB_SECTION_TYPE + " " + SUB_SUB_SECTION_TYPE + " " + PARAGRAPH_TYPE + " " + SENTENCE_TYPE;
	
	/** set the type-based Annotation nesting order for this document, replacing the current one
	 * @param	ano		the new type-based Annotation nesting order for this document, as a space-separated string concatenation of annotation types, outmost type first
	 * @return the old annotation nesting order
	 */
	public abstract String setAnnotationNestingOrder(String ano);
	
	/** set a property for this document. The value will then be retrievable from all Annotations that belong to the hierarch built on this DocumentRoot
	 * @param	propertyName	the name for the property
	 * @param	value			the value for the property
	 * @return the old value of the property, or null, if there was no such value
	 */
	public abstract String setDocumentProperty(String propertyName, String value);
	
	/** remove a property from this document 
	 * @param	propertyName	the name of the property to remove
	 * @return the value of the property that was just removed, or null, if there was no value
	 */
	public abstract String removeDocumentProperty(String propertyName);
	
	/** remove all properties from this document 
	 */
	public abstract void clearDocumentProperties();
}
