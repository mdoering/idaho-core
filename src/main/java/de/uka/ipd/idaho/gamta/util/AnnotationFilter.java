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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * Utility library for renaming, removing, and deleting annotations and
 * annotation attributes. For details, please refer to the documentation of the
 * individual methods.
 * 
 * @author sautter
 */
public class AnnotationFilter {
	
	/**
	 * remove duplicate Annotations
	 * @param data the MutableAnnotation to process
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeDuplicates(MutableAnnotation data) {
		return removeDuplicates(data, null);
	}

	/**
	 * remove duplicate Annotations of a specific type
	 * @param data the MutableAnnotation to process
	 * @param type the type of the Annotations to check (specifying null will
	 *            result in all Annotations being checked)
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeDuplicates(MutableAnnotation data, String type) {
		
		//	check parameters
		if (data == null) return false;
		
		//	get and process Annotations
		boolean modified = false;
		Annotation[] annotations = data.getAnnotations(type);
		for (int a = 0; a < annotations.length; a++) {
			if (annotations[a] == null)
				continue;
			
			for (int d = (a+1); d < annotations.length; d++) {
				if (AnnotationUtils.equals(annotations[a], annotations[d])) {
					
					//	copy attributes, give priority to annnotation not removed
					AttributeUtils.copyAttributes(annotations[d], annotations[a], AttributeUtils.ADD_ATTRIBUTE_COPY_MODE);
					
					//	remove annotation
					data.removeAnnotation(annotations[d]);
					annotations[d] = null;
					modified = true;
				}
				else d = annotations.length;
			}
		}
		
		//	report changes
		return modified;
		
//		//	get and process Annotations
//		boolean modified = false;
//		Annotation[] annotations = data.getAnnotations(type);
//		for (int a = 0; a < annotations.length; a++) {
//			if (annotations[a] != null) {
//				int i = 1;
//				while ((a + i) < annotations.length) {
//					if (annotations[a + i] != null) {
//						if ((annotations[a].getStartIndex() == (annotations[a + i].getStartIndex())) 
//								&& (annotations[a].size() == (annotations[a + i].size())) 
//								&& annotations[a].getType().equals(annotations[a + i].getType())) {
//							
//							//	copy attributes, give priority to Annnotation not removed
//							annotations[a + i].copyAttributes(annotations[a]);
//							annotations[a].copyAttributes(annotations[a + i]);
//							
//							//	remove Annotation
//							data.removeAnnotation(annotations[a + i]);
//							annotations[a + i] = null;
//							modified = true;
//							i++;
//						}
//						else if ((annotations[a].getStartIndex() == annotations[a + i].getStartIndex()) && (annotations[a].size() == annotations[a + i].size()))
//							i++;
//						else i = annotations.length;
//					}
//					else i++;
//				}
//			}
//		}
//		
//		return modified;
	}
	
	/**
	 * remove Annotations of a specific type that are contained in an Annotation
	 * of the same type
	 * @param data the MutableAnnotation to process
	 * @param type the type of the Annotations to process
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeInner(MutableAnnotation data, String type) {
		return removeContained(data, type, type);
	}

	/**
	 * remove Annotations of a specific type that are contained in an
	 * Annotations of another specific type
	 * @param data the MutableAnnotation to process
	 * @param outerType the type of the containing Annotations
	 * @param innerType the type of the Annotations to be removed if contained
	 *            in an outer type Annotation
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeContained(MutableAnnotation data, String outerType, String innerType) {
		return removeByContained(data, outerType, innerType, false);
	}

	/**
	 * remove Annotations of a specific type that are not contained in an
	 * Annotations of another specific type
	 * @param data the MutableAnnotation to process
	 * @param outerType the type of the containing Annotations
	 * @param innerType the type of the Annotations to be removed if not
	 *            contained in an outer type Annotation
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeNotContained(MutableAnnotation data, String outerType, String innerType) {
		return removeByContained(data, outerType, innerType, true);
	}

	/**
	 * remove Annotations of a specific type that are or are not contained in an
	 * Annotations of another specific type
	 * @param data the MutableAnnotation to process
	 * @param outerType the type of the containing Annotations
	 * @param innerType the type of the Annotations to be removed if contained
	 *            in an outer type Annotation
	 * @param invert if true, innerType Annotations will be removed if they are
	 *            not contained in an outerType Annotation, if false, they will
	 *            if they are
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeByContained(MutableAnnotation data, String outerType, String innerType, boolean invert) {
		
		//	check parameters
		if ((data == null) || (outerType == null) || (innerType == null)) return false;
		
		//	merge duplicates first if inner any outer type equal
		if (!invert && outerType.equalsIgnoreCase(innerType))
			removeDuplicates(data, outerType);
		
		//	get IDs of affected annotations
		boolean modified = false;
		HashSet containedIDs = new HashSet();
		HashSet duplicateIDs = new HashSet();
		Annotation[] outer = data.getAnnotations(outerType);
		Annotation[] inner = data.getAnnotations(innerType);
		fillIdSets(outer, inner, new HashSet(), containedIDs, duplicateIDs);
		for (int i = 0; i < inner.length; i++) {
			if ((containedIDs.contains(inner[i].getAnnotationID()) != invert) && !duplicateIDs.contains(inner[i].getAnnotationID())) {
				data.removeAnnotation(inner[i]);
				modified = true;
			}
//			if (invert) {
//				if (!nestedIDs.contains(inner[i].getAnnotationID()) && !duplicateIDs.contains(inner[i].getAnnotationID())) {
//					data.removeAnnotation(inner[i]);
//					modified = true;
//				}
//			}
//			else {
//				if (nestedIDs.contains(inner[i].getAnnotationID())) {
//					data.removeAnnotation(inner[i]);
//					modified = true;
//				}
//			}
		}
		
		//	report modifications
		return modified;
		
//		//	merge duplicates first if inner any outer type equal
//		if (!invert && outerType.equalsIgnoreCase(innerType))
//			removeDuplicates(data, outerType);
//		
//		//	get and process Annotations
//		boolean modified = false;
//		String marker = ("MARK" + Math.random());
//		Annotation[] annotations = data.getAnnotations();
//		int lastStartIndex = 0;
//		int lastStartAnnotationIndex = 0;
//		for (int a = 0; a < annotations.length; a++) {
//			
//			//	remember array index of first Annotation in group of Annotations with same start index
//			if (annotations[a].getStartIndex() > lastStartIndex) {
//				lastStartIndex = annotations[a].getStartIndex();
//				lastStartAnnotationIndex = a;
//			}
//			
//			if (outerType.equalsIgnoreCase(annotations[a].getType())) {
//				
//				//	make sure to ckeck Annotations with equal start index & size in both ways
//				int i = (lastStartAnnotationIndex - a);
//				while ((a + i) < annotations.length) {
//					
//					//	don't check an Annotation against itself, however
//					if ((i != 0) && innerType.equalsIgnoreCase(annotations[a + i].getType())) {
//						if (AnnotationUtils.contains(annotations[a], annotations[a + i])) {
//							annotations[a + i].setAttribute(marker, marker);
//							i++;
//						}
//						else if (annotations[a].getEndIndex() > annotations[a + i].getStartIndex())
//							i++;
//						else i = annotations.length;
//					}
//					else i++;
//				}
//			}
//		}
//		
//		//	remove marked (or un-marked) annotations
//		annotations = data.getAnnotations(innerType);
//		for (int a = 0; a < annotations.length; a++) {
//			if (annotations[a].hasAttribute(marker) != invert) {
//				data.removeAnnotation(annotations[a]);
//				modified = true;
//			}
//			else annotations[a].removeAttribute(marker);
//		}
//		
//		//	report modifications
//		return modified;
	}
	
	private static final void fillIdSets(Annotation[] outer, Annotation[] inner, Set outerIDs, Set innerIDs, Set duplicateIDs) {
		if ((outer.length * inner.length) == 0)
			return;
		
		boolean sameType = outer[0].getType().equalsIgnoreCase(inner[0].getType());
		int ii = 0;
		for (int o = 0; o < outer.length; o++) {
			
			//	find next possibly nested annotation
			while ((ii < inner.length) && (inner[ii].getStartIndex() < outer[o].getStartIndex()))
				ii++;
			
			//	find all nested inner annotation for current outer annotation
			for (int i = ii; i < inner.length; i++) {
				if (inner[i].getStartIndex() >= outer[o].getEndIndex())
					i = inner.length;
				else if (AnnotationUtils.equals(outer[o], inner[i], false)) {
					if (outer[o].getAnnotationID().equals(inner[i].getAnnotationID())) {}
					else if (sameType) {
						duplicateIDs.add(outer[o].getAnnotationID());
						duplicateIDs.add(inner[i].getAnnotationID());
					}
					else {
						outerIDs.add(outer[o].getAnnotationID());
						innerIDs.add(inner[i].getAnnotationID());
					}
				}
				else if (AnnotationUtils.contains(outer[o], inner[i])) {
					outerIDs.add(outer[o].getAnnotationID());
					innerIDs.add(inner[i].getAnnotationID());
				}
			}
		}
	}
	
	/**
	 * remove Annotations of a specific type that are containing an Annotation
	 * of the same type
	 * @param data the MutableAnnotation to process
	 * @param type the type of the Annotations to process
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeOuter(MutableAnnotation data, String type) {
		return removeContaining(data, type, type);
	}

	/**
	 * remove Annotations of a specific type that are containing an Annotation
	 * of another specific type
	 * @param data the MutableAnnotation to process
	 * @param outerType the type of the containing Annotations to be removed if
	 *            containing an inner type Annotation
	 * @param innerType the type of the contained Annotations
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeContaining(MutableAnnotation data, String outerType, String innerType) {
		return removeByContaining(data, outerType, innerType, false);
	}

	/**
	 * remove Annotations of a specific type that are not containing an
	 * Annotation of another specific type
	 * @param data the MutableAnnotation to process
	 * @param outerType the type of the containing Annotations to be removed if
	 *            containing an inner type Annotation
	 * @param innerType the type of the contained Annotations
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeNotContaining(MutableAnnotation data, String outerType, String innerType) {
		return removeByContaining(data, outerType, innerType, true);
	}

	/**
	 * remove Annotations of a specific type that are containing an Annotation
	 * of another specific type
	 * @param data the MutableAnnotation to process
	 * @param outerType the type of the containing Annotations to be removed if
	 *            containing an inner type Annotation
	 * @param innerType the type of the contained Annotations
	 * @param invert if true, outerType Annotations will be removed if they are
	 *            not containing an innerType Annotation, if false, they will if
	 *            they are
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeByContaining(MutableAnnotation data, String outerType, String innerType, boolean invert) {
		
		//	check parameters
		if ((data == null) || (outerType == null) || (innerType == null)) return false;
		
		//	merge duplicates first if inner any outer type equal
		if (!invert && outerType.equalsIgnoreCase(innerType))
			removeDuplicates(data, outerType);
		
		//	get IDs of affected annotations
		boolean modified = false;
		HashSet containingIDs = new HashSet();
		HashSet duplicateIDs = new HashSet();
		Annotation[] outer = data.getAnnotations(outerType);
		Annotation[] inner = data.getAnnotations(innerType);
		fillIdSets(outer, inner, containingIDs, new HashSet(), duplicateIDs);
		for (int o = 0; o < outer.length; o++) {
			if ((containingIDs.contains(outer[o].getAnnotationID()) != invert) && !duplicateIDs.contains(outer[o].getAnnotationID())) {
				data.removeAnnotation(outer[o]);
				modified = true;
			}
//			if (invert) {
//				if (!nestedIDs.contains(inner[i].getAnnotationID()) && !duplicateIDs.contains(inner[i].getAnnotationID())) {
//					data.removeAnnotation(inner[i]);
//					modified = true;
//				}
//			}
//			else {
//				if (nestedIDs.contains(inner[i].getAnnotationID())) {
//					data.removeAnnotation(inner[i]);
//					modified = true;
//				}
//			}
		}
		
		//	report modifications
		return modified;
		
//		//	merge duplicates first if inner any outer type equal
//		if (!invert && outerType.equalsIgnoreCase(innerType))
//			removeDuplicates(data, outerType);
//		
//		//	get and process Annotations
//		boolean modified = false;
//		String marker = ("MARK" + Math.random());
//		Annotation[] annotations = data.getAnnotations();
//		int lastStartIndex = 0;
//		int lastStartAnnotationIndex = 0;
//		for (int a = 0; a < annotations.length; a++) {
//			
//			//	remember array index of first Annotation in group of Annotations with same start index
//			if (annotations[a].getStartIndex() > lastStartIndex) {
//				lastStartIndex = annotations[a].getStartIndex();
//				lastStartAnnotationIndex = a;
//			}
//			
//			if (outerType.equalsIgnoreCase(annotations[a].getType())) {
//				
//				//	make sure to ckeck Annotations with equal start index & size in both ways
//				int i = (lastStartAnnotationIndex - a);
//				while ((a + i) < annotations.length) {
//					
//					//	don't check an Annotation against itself, however
//					if ((i != 0) && innerType.equalsIgnoreCase(annotations[a + i].getType())) {
//						if (AnnotationUtils.contains(annotations[a], annotations[a + i])) {
//							annotations[a].setAttribute(marker, marker);
//							i = annotations.length;
//						}
//						else if (annotations[a].getEndIndex() > annotations[a + i].getStartIndex())
//							i++;
//						else i = annotations.length;
//					}
//					else i++;
//				}
//			}
//		}
//		
//		//	remove marked (or un-marked) annotations
//		annotations = data.getAnnotations(outerType);
//		for (int a = 0; a < annotations.length; a++) {
//			if (annotations[a].hasAttribute(marker) != invert) {
//				data.removeAnnotation(annotations[a]);
//				modified = true;
//			} else annotations[a].removeAttribute(marker);
//		}
//		
//		//	report modifications
//		return modified;
	}
	
	/**
	 * remove Annotations of a specific type
	 * @param data the MutableAnnotation to process
	 * @param type the type of Annotations to remove (specifying null will
	 *            result in all Annotations being removed)
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeAnnotations(MutableAnnotation data, String type) {
		return renameAnnotations(data, type, null);
	}

	/**
	 * rename Annotations of a specific type
	 * @param data the MutableAnnotation to process
	 * @param type the type of Annotations to rename (specifying null will
	 *            result in all Annotations being removed)
	 * @param newType the type to rename the Annotations to (specifying null
	 *            will result in the annotations of the specified type being
	 *            removed)
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean renameAnnotations(MutableAnnotation data, String type, String newType) {
		
		//	check parameters
		if ((data == null) || ((type != null) && (type.equals(newType))) || ((newType != null) && (newType.trim().length() == 0))) return false;
		
		//	get and process Annotations
		Annotation[] annotations = data.getAnnotations(type);
		for (int a = 0; a < annotations.length; a++)
			if ((type == null) || (newType == null)) data.removeAnnotation(annotations[a]);
			else annotations[a].changeTypeTo(newType);
		
		return (annotations.length != 0);
	}
	
	/**
	 * remove an attribute of Annotations of a specific type
	 * @param data the MutableAnnotation to process
	 * @param type the type of Annotations to process (specifying null will
	 *            result in all Annotations being processed)
	 * @param attribute the attribute to remove (specifying null will result in
	 *            all attributes being removed)
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean removeAnnotationAttribute(MutableAnnotation data, String type, String attribute) {
		return renameAnnotationAttribute(data, type, attribute, null);
	}

	/**
	 * rename an attribute of Annotations of a specific type
	 * @param data the MutableAnnotation to process
	 * @param type the type of Annotations to process (specifying null will
	 *            result in all Annotations being processed)
	 * @param attribute the attribute to rename (specifying null will result in
	 *            all attributes being removed)
	 * @param newAttribute the new name for the attribute (specifying null will
	 *            result in the specified attribute being removed)
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean renameAnnotationAttribute(MutableAnnotation data, String type, String attribute, String newAttribute) {
		
		//	check parameters
		if ((data == null) || ((attribute != null) && attribute.equals(newAttribute)) || ((newAttribute != null) && (newAttribute.trim().length() == 0))) return false;
		
		//	get and process Annotations
		boolean modified = false;
		Annotation[] annotations = data.getAnnotations(type);
		for (int a = 0; a < annotations.length; a++) {
			if (attribute == null) {
				modified = (modified || (annotations[a].getAttributeNames().length != 0));
				annotations[a].clearAttributes();
			}
			else if (annotations[a].hasAttribute(attribute)) {
				Object attributeValue = annotations[a].removeAttribute(attribute);
				if (newAttribute != null) annotations[a].setAttribute(newAttribute, attributeValue);
				modified = true;
			}
		}
		
		return modified;
	}
	
	/**
	 * delete Annotations of a specific type, including the Tokens contained in
	 * them
	 * @param data the MutableAnnotation to process
	 * @param type the type of the Annotations process
	 * @return true if and only if the MutableAnnotation was modified by this method
	 */
	public static boolean deleteAnnotations(MutableAnnotation data, String type) {
		
		//	check parameters
		if ((data == null) || (type == null)) return false;
		
		//	get and process Annotations
		Annotation[] annotations = data.getAnnotations(type);
		if (annotations.length == 0) return false;
		
		//	determine token ranges to remove, and remove Annotations from document
		int start = annotations[0].getStartIndex();
		int end = annotations[0].getEndIndex();
		data.removeAnnotation(annotations[0]);
		
		ArrayList annotationList = new ArrayList();
		for (int a = 1; a < annotations.length; a++) {
			if (annotations[a].getStartIndex() > end) {
				annotationList.add(Gamta.newAnnotation(data, "delete", start, (end - start)));
				start = annotations[a].getStartIndex();
				end = annotations[a].getEndIndex();
			}
			else end = annotations[a].getEndIndex();
			data.removeAnnotation(annotations[a]);
		}
		annotationList.add(Gamta.newAnnotation(data, "delete", start, (end - start)));
		
		//	delete Annotation Tokens
		boolean modified = false;
		for (int a = (annotationList.size() - 1); a != -1; a--) {
			Annotation annotation = ((Annotation) annotationList.get(a));
			data.removeTokensAt(annotation.getStartIndex(), annotation.size());
			modified = true;
		}
		
		return modified;
	}
}
