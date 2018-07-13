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
package de.uka.ipd.idaho.gamta.util.gPath.types;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;



/**
 * @author sautter
 *
 * TODO document this class
 */
public class GPathAnnotationSet extends GPathObject {
	
	private ArrayList annotationList = null;
	private TreeSet annotationSet = null;
	
	public final boolean isReverseDocOrder;
	
	public GPathAnnotationSet() {
		this(false);
	}
	
	public GPathAnnotationSet(boolean reverse) {
		this.isReverseDocOrder = reverse;
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject#asBoolean()
	 */
	public GPathBoolean asBoolean() {
		return new GPathBoolean(this.size() != 0);
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject#asNumber()
	 */
	public GPathNumber asNumber() {
		return this.asString().asNumber();
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject#asString()
	 */
	public GPathString asString() {
		if (this.isEmpty()) return new GPathString("");
		return new GPathString(this.getFirst().getValue());
	}
	
	/**
	 * Filter the Annotations in this AnnotationSet by their type
	 * @param filterType the type to use as the filter
	 * @return a new AnnotationSet containing the annotations from this
	 *         AnnotationSet that have the specified type
	 */
	public GPathAnnotationSet filterByType(String filterType) {
		if (filterType == null) return this;
		GPathAnnotationSet result = new GPathAnnotationSet();
		for (int n = 0; n < this.size(); n++) {
			QueriableAnnotation annotation = this.get(n);
			if (filterType.equals(annotation.getType())) result.add(annotation);
		}
		return result;
	}
	
	public synchronized boolean add(QueriableAnnotation annotation) {
		if (annotation == null) return false;
		if (this.annotationSet == null)
			this.annotationSet = new TreeSet(this.buildSortOrder(annotation));
		
		if (this.annotationSet.add(annotation)) {
			this.annotationList = null;
			return true;
		}
		else return false;
	}
	
	public boolean addAll(GPathAnnotationSet annotationSet) {
		if ((annotationSet == null) || annotationSet.isEmpty()) return false;
		
		if (this.annotationSet == null)
			this.annotationSet = new TreeSet(this.buildSortOrder(annotationSet.getFirst()));
		
		if (this.annotationSet.addAll(annotationSet.annotationSet)) {
			this.annotationList = null;
			return true;
		}
		else return false;
	}
	
	private Comparator buildSortOrder(QueriableAnnotation annotation) {
		final Comparator typeComparator = AnnotationUtils.getTypeComparator(annotation.getAnnotationNestingOrder());
		
		final Comparator annotationComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
				QueriableAnnotation a1 = ((QueriableAnnotation) o1);
				QueriableAnnotation a2 = ((QueriableAnnotation) o2);
				int c = (a1.getAbsoluteStartIndex() - a2.getAbsoluteStartIndex());
				if (c != 0) return c;
				c = (a2.size() - a1.size());
				if (c != 0) return c;
				c = typeComparator.compare(a1.getType(), a2.getType());
				if (c != 0) return c;
				return a1.hashCode() - a2.hashCode();
			}
		};
		
		if (this.isReverseDocOrder) return new Comparator() {
			public int compare(Object o1, Object o2) {
				return -annotationComparator.compare(o1, o2);
			}
		};
		else return annotationComparator;
	}
	
	public boolean remove(QueriableAnnotation annotation) {
		if (this.isEmpty()) return false;
		if (this.annotationSet.remove(annotation)) {
			if (this.annotationList != null)
				this.annotationList.remove(annotation);
			return true;
		}
		else return false;
	}
	
	public boolean isEmpty() {
		return (this.annotationSet == null);
	}
	
	public int size() {
		return ((this.annotationSet == null) ? 0 : this.annotationSet.size());
	}
	
	public QueriableAnnotation getFirst() {
		if (this.isEmpty()) return null;
		if (this.annotationList == null)
			this.annotationList = new ArrayList(this.annotationSet);
		return ((QueriableAnnotation) this.annotationList.get(0));
	}
	
	public QueriableAnnotation getLast() {
		if (this.isEmpty()) return null;
		if (this.annotationList == null)
			this.annotationList = new ArrayList(this.annotationSet);
		return ((QueriableAnnotation) this.annotationList.get(this.size() - 1));
	}
	
	/**
	 * Get the Annotation at index
	 * @param index the index of the desired Annotation by the rules of a JAVA
	 *            List, i.e. 0 ... (size() - 1)
	 * @return the Annotation at the specified index
	 */
	public QueriableAnnotation get(int index) {
		if ((index >= 0) && (index < this.size())) {
			if (this.annotationList == null)
				this.annotationList = new ArrayList(this.annotationSet);
			return ((QueriableAnnotation) this.annotationList.get(index));
		}
		else return null;
	}
	
	/**
	 * Get the Annotation at position
	 * @param position the index of the desired annotation by the rules of
	 *            XPath, i.e. 1 ... size()
	 * @return the Annotation at the specified index
	 */
	public QueriableAnnotation annotationAt(int position) {
		return this.get(position - 1);
	}
	
	/**
	 * Get the index of an Annotation in this AnnotationSet
	 * @param annotation the Annotation to find
	 * @return the index of the specified Annotation in this AnnotationSet by
	 *         the rules of a JAVA List, i.e. 0 ... (size() - 1)
	 */
	public int indexOf(QueriableAnnotation annotation) {
		if (this.isEmpty()) return -1;
		if (this.annotationList == null)
			this.annotationList = new ArrayList(this.annotationSet);
		return this.annotationList.indexOf(annotation);
	}
	
	/**
	 * Get the position of an Annotation in this AnnotationSet
	 * @param annotation the Annotation to find
	 * @return the position of the specified Annotation in this AnnotationSet by
	 *         the rules of XPath, i.e. 1 ... size()
	 */
	public int positionOf(QueriableAnnotation annotation) {
		return this.indexOf(annotation) + 1;
	}
//	
//	private ArrayList annotationList = new ArrayList();
//	private HashSet annotationSet = new HashSet();
//	
//	public final boolean isReverseDocOrder;
//	
//	public GPathAnnotationSet() {
//		this(false);
//	}
//	
//	public GPathAnnotationSet(boolean reverse) {
//		this.isReverseDocOrder = reverse;
//	}
//	
//	/** @see de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject#asBoolean()
//	 */
//	public GPathBoolean asBoolean() {
//		return new GPathBoolean(this.size() != 0);
//	}
//	
//	/** @see de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject#asNumber()
//	 */
//	public GPathNumber asNumber() {
//		return this.asString().asNumber();
//	}
//	
//	/** @see de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject#asString()
//	 */
//	public GPathString asString() {
//		if (this.isEmpty()) return new GPathString("");
//		return new GPathString(this.getFirst().getValue());
//	}
//	
//	/**	filter the Annotations in this AnnotationSet by their type
//	 * @param	filterType	the type to use as the filter
//	 * @return a new AnnotationSet containing the annotations from this AnnotationSet that have the specified type 
//	 */
//	public GPathAnnotationSet filterByType(String filterType) {
//		if (filterType == null) return this;
//		GPathAnnotationSet result = new GPathAnnotationSet();
//		for (int n = 0; n < this.size(); n++) {
//			QueriableAnnotation annotation = this.get(n);
//			if (filterType.equals(annotation.getType())) result.add(annotation);
//		}
//		return result;
//	}
//	
//	public synchronized boolean add(QueriableAnnotation annotation) {
//		if ((annotation == null) || this.annotationSet.contains(annotation)) return false;
//		this.annotationList.add(annotation);
//		this.annotationSet.add(annotation);
//		this.isSorted = false;
//		return true;
//	}
//	
//	public boolean addAll(GPathAnnotationSet annotationSet) {
//		if ((annotationSet == null) || annotationSet.isEmpty()) return false;
//		boolean changed = false;
//		for (int n = 0; n < annotationSet.size(); n++) {
//			QueriableAnnotation annotation = annotationSet.get(n);
//			if ((annotation != null) && !this.annotationSet.contains(annotation)) {
//				this.annotationList.add(annotation);
//				this.annotationSet.add(annotation);
//				changed = true;
//			}
//		}
//		if (changed)
//			this.isSorted = false;
//		return changed;
//	}
//	
//	public boolean remove(QueriableAnnotation annotation) {
//		this.annotationList.remove(annotation);
//		return this.annotationSet.remove(annotation);
//	}
//	
//	public boolean isEmpty() {
//		return this.annotationSet.isEmpty();
//	}
//	
//	public int size() {
//		return this.annotationSet.size();
//	}
//	
//	public QueriableAnnotation getFirst() {
//		this.ensureSorted();
//		return ((this.size() == 0) ? null : ((QueriableAnnotation) this.annotationList.get(0)));
//	}
//	
//	public QueriableAnnotation getLast() {
//		this.ensureSorted();
//		return ((this.size() == 0) ? null : ((QueriableAnnotation) this.annotationList.get(this.size() - 1)));
//	}
//	
//	/**	get the Annotation at index
//	 * @param	index	the index of the desired Annotation by the rules of a JAVA List, i.e. 0 ... (size() - 1)
//	 * @return the Annotation at the specified index
//	 */
//	public QueriableAnnotation get(int index) {
//		if ((index >= 0) && (index < this.size())) {
//			this.ensureSorted();
//			return ((QueriableAnnotation) this.annotationList.get(index));
//		}
//		return null;
//	}
//	
//	/**	get the Annotation at position
//	 * @param	position	the index of the desired annotation by the rules of XPath, i.e. 1 ... size()
//	 * @return the Annotation at the specified index
//	 */
//	public QueriableAnnotation annotationAt(int position) {
//		return this.get(position - 1);
//	}
//	
//	/**	get the index of an Annotation in this AnnotationSet
//	 * @param	annotation	the Annotation to find
//	 * @return the index of the specified Annotation in this AnnotationSet by the rules of a JAVA List, i.e. 0 ... (size() - 1)
//	 */
//	public int indexOf(QueriableAnnotation annotation) {
//		this.ensureSorted();
//		return this.annotationList.indexOf(annotation);
//	}
//	
//	/**	get the position of an Annotation in this AnnotationSet
//	 * @param	annotation	the Annotation to find
//	 * @return the position of the specified Annotation in this AnnotationSet by the rules of XPath, i.e. 1 ... size()
//	 */
//	public int positionOf(QueriableAnnotation annotation) {
//		return this.indexOf(annotation) + 1;
//	}
//	
//	//	sort the AnnotationSet in document order, and remove duplicates
//	private void sort() {
//		if (this.annotationList.size() < 2) return;
//		this.ensureSortOrder();
//		Collections.sort(this.annotationList, this.sortOrder);
//	}
//	
//	private boolean isSorted = true;
//	private Comparator sortOrder = null;
//	
//	private void ensureSorted() {
//		if (this.isSorted) return;
//		
//		this.sort();
//		this.isSorted = true;
//	}
//	
//	private void ensureSortOrder() {
//		if (this.sortOrder != null) return;
//		if (this.isEmpty()) return;
//		
//		
//		final Comparator typeComparator = AnnotationUtils.getTypeComparator(((QueriableAnnotation) this.annotationList.get(0)).getAnnotationNestingOrder());
//		
//		final Comparator annotationComparator = new Comparator() {
//			public int compare(Object o1, Object o2) {
//				QueriableAnnotation a1 = ((QueriableAnnotation) o1);
//				QueriableAnnotation a2 = ((QueriableAnnotation) o2);
//				int c = (a1.getAbsoluteStartIndex() - a2.getAbsoluteStartIndex());
//				if (c != 0) return c;
//				c = (a2.size() - a1.size());
//				if (c != 0) return c;
//				return typeComparator.compare(a1.getType(), a2.getType());
//			}
//		};
//		
//		this.sortOrder = (this.isReverseDocOrder ?
//				new Comparator() {
//					public int compare(Object o1, Object o2) {
//						return -annotationComparator.compare(o1, o2);
//					}
//				}
//				:
//				annotationComparator
//			);
//	}
}
