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
 * A marker for some part of a token sequence. As such, an annotation is itself
 * a token sequence that other annotations can mark parts of.
 * 
 * @author sautter
 */
public interface Annotation extends Attributed, TokenSequence, Comparable, CharSpan {
	
	/**
	 * the default annotation type, 'genericAnnotation', which is used if an
	 * Annotation is created with a null or empty string type
	 */
	public static final String DEFAULT_ANNOTATION_TYPE = "genericAnnotation";
	
	
	/**
	 * the generic attribute name for retrieving the start index of an
	 * Annotation as an Integer object via the getAttribute method, namely
	 * 'START_INDEX'. Getting this attribute should never return null, though the
	 * listing of attribute names should never contain this attribute name.
	 */
	public static final String START_INDEX_ATTRIBUTE = "START_INDEX";
	
	/**
	 * the generic attribute name for retrieving the size of an Annotation as an
	 * Integer object via the getAttribute method, namely 'SIZE'. Getting this
	 * attribute should never return null, though the listing of attribute names
	 * should never contain this attribute name.
	 */
	public static final String SIZE_ATTRIBUTE = "SIZE";
	
	/**
	 * the generic attribute name for retrieving the end index of an Annotation
	 * as an Integer object via the getAttribute method, namely 'END_INDEX'.
	 * Getting this attribute should never return null, though the listing of
	 * attribute names should never contain this attribute name.
	 */
	public static final String END_INDEX_ATTRIBUTE = "END_INDEX";
	
	/**
	 * the generic attribute name for retrieving the value of an Annotation via
	 * the getAttribute method, namely 'VALUE'. Getting this attribute should
	 * never return null, though the listing of attribute names should never
	 * contain this attribute name.
	 */
	public static final String ANNOTATION_VALUE_ATTRIBUTE = "VALUE";
	
	/**
	 * the generic attribute name for retrieving the ID of an Annotation via the
	 * getAttribute method, same as the getAnnotationID() method, namely
	 * 'ANNOTATION_ID'. Getting this attribute should never return null, though
	 * the listing of attribute names should never contain this attribute name.
	 * If setting this attribute is implemented to modify the Annotation's ID,
	 * this change should also reflect in the return value of the
	 * getAnnotationID() method.
	 */
	public static final String ANNOTATION_ID_ATTRIBUTE = "ANNOTATION_ID";
	
	
	/**
	 * Add an attribute to this Annotation. If the name is one of
	 * START_INDEX_ATTRIBUTE, SIZE_ATTRIBUTE, END_INDEX_ATTRIBUTE and VALUE_ATTRIBUTE,
	 * this method should ignore the invocation. If the name is
	 * ANNOTATION_ID_ATTRIBUTE and the invocation is not ignored, the change
	 * should also reflect in the return value of the getAnnotationID() method.
	 * @param name the name for the attribute
	 * @param value the value of the attribute
	 * @return the old value of the attribute, if there was one, null otherwise
	 */
	public abstract Object setAttribute(String name, Object value);
	
	/**
	 * Retrieve the value of an attribute. If the name is one of
	 * START_INDEX_ATTRIBUTE, SIZE_ATTRIBUTE, END_INDEX_ATTRIBUTE, VALUE_ATTRIBUTE or
	 * ANNOTATION_ID_ATTRIBUTE, this method should return the respective value as
	 * described for these constants.
	 * @param name the name of the attribute
	 * @return the value of the attribute with the specified name, or null, if
	 *         there is no such attribute
	 */
	public abstract Object getAttribute(String name);
	
	/**
	 * Retrieve the value of an attribute. If the name is one of
	 * START_INDEX_ATTRIBUTE, SIZE_ATTRIBUTE, END_INDEX_ATTRIBUTE, VALUE_ATTRIBUTE or
	 * ANNOTATION_ID_ATTRIBUTE, this method should return the respective value as
	 * described for these constants.
	 * @param name the name of the attribute
	 * @param def the value to return if this object does not have an attribute
	 *            with the specified name
	 * @return the value of the attribute with the specified name, or def, if
	 *         there is no such attribute
	 */
	public abstract Object getAttribute(String name, Object def);
	
	/**
	 * Check if this object has a particular attribute. If the name is one of
	 * START_INDEX_ATTRIBUTE, SIZE_ATTRIBUTE, END_INDEX_ATTRIBUTE, VALUE_ATTRIBUTE or
	 * ANNOTATION_ID_ATTRIBUTE, this method should always return true.
	 * @param name the name of the attribute
	 * @return true if and only if this object has an attribute with the
	 *         specified name
	 */
	public abstract boolean hasAttribute(String name);
	
	/**
	 * @return the index of the first Token in this Annotation, relative to the
	 *         TokenSequence it belongs to
	 */
	public abstract int getStartIndex();
	
	/**
	 * @return the index after the last Token in this Annotation, relative to the
	 *         TokenSequence it belongs to
	 */
	public abstract int getEndIndex();
	
	/**
	 * @return the type of the Annotation
	 */
	public abstract String getType();
	
	/**
	 * Change the type of the Annotation, Note: This method is not intended to
	 * be a classic setter method (that's why it is not named setType()) This
	 * method is rather provided for the rare cases when Annotation's types have
	 * to be unified, e.g. in order to match an XML name space. The intention is
	 * NOT that it is used frequently.
	 * @param newType the new type for the Annotation (specifying null or an
	 *            empty String will not change anything)
	 * @return the old type of the Annotation
	 */
	public abstract String changeTypeTo(String newType);
	
	/**
	 * @return a 128 bit hex String uniquely identifying the Annotation
	 */
	public abstract String getAnnotationID();
	
	/**
	 * Retrieve the value of the annotation. Note: As opposed to the
	 * TokenSequenceUtils.concatTokens() methods, this method returns the
	 * underlaying char sequence as is, i.e. without normalizing whitespaces
	 * Note: This method does not include leading and tailing whitespaces of the
	 * token sequence it represents
	 * @return the Annotation's value, i.e. the part of the underlying character
	 *         sequence spanned by the Annotation.
	 */
	public abstract String getValue();
	
	/**
	 * @return this Annotaion's value surrounded by start and end tag of this
	 *         annotation's type, start tag containing the Annotation's
	 *         attributes whose values are Strings
	 */
	public abstract String toXML();
	
	/**
	 * Get a property of the document this Annotation belongs to.
	 * @param propertyName the name of the property
	 * @return the value of the requested property, or null, if there is no
	 *         property with the specified name, or if this Annotation does not
	 *         belong to a document
	 */
	public abstract String getDocumentProperty(String propertyName);
	
	/**
	 * Get a property of the document this Annotation belongs to.
	 * @param propertyName the name of the property
	 * @param defaultValue the value to return if there is no property with the
	 *            specified name, or if this Annotation does not belong to a
	 *            document
	 * @return the value of the requested property, or the specified def value,
	 *         if there is no property with the specified name, or if this
	 *         Annotation does not belong to a document
	 */
	public abstract String getDocumentProperty(String propertyName, String defaultValue);
	
	/**
	 * Get the names of all properties of the document.
	 * @return an array holding the names of all properties of the document this
	 *         Annotation refers to
	 */
	public abstract String[] getDocumentPropertyNames();
}
