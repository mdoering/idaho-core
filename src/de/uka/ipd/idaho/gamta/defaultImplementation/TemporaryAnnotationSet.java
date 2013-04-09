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
package de.uka.ipd.idaho.gamta.defaultImplementation;


import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationSet;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.StandaloneAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;

/**
 * Implementation of the AnnotationSet interface. Using a null TokenSequence
 * will not cause trouble unless the Annotation fraction of the methods is
 * touched.
 * 
 * @author sautter
 */
public class TemporaryAnnotationSet extends AbstractAttributed implements AnnotationSet {
	
	private String id = Gamta.getAnnotationID();
	
	private TokenSequence tokens;
	
	private String type = DEFAULT_ANNOTATION_TYPE;
	private int setStartIndex = -1;
	private int setEndIndex = 0;
	boolean modified = false;
	
	private TreeMap annotationMap = new TreeMap(new Comparator() {
		public int compare(Object o1, Object o2) {
			Annotation a1 = ((o1 instanceof Annotation) ? ((Annotation) o1) : null);
			Annotation a2 = ((o2 instanceof Annotation) ? ((Annotation) o2) : null);
			if ((a1 == null) && (a2 == null)) return 0;
			else if (a1 == null) return 1;
			else if (a2 == null) return -1;
			else {
				
				//	first, order by start index ascending
				int c = (a1.getStartIndex() - a2.getStartIndex());
				if (c != 0) return c;
				
				//	second, order by length descending in order to preserve depth first order
				c = (a2.size() - a1.size());
				if (c != 0) return c;
				
				//	last, compare type in order to comply with the general contract of equals() and compareTo()
				return a1.getType().compareTo(a2.getType());
			}
		}
	});
	private Annotation[] annotationArray = null;
	
	private synchronized void buildAnnotArray() {
		if (this.annotationArray != null) return;
		else {
			this.annotationArray = new Annotation[this.annotationMap.size()];
			int index = 0;
			for (Iterator iter = this.annotationMap.keySet().iterator(); iter.hasNext(); index++)
				this.annotationArray[index] = ((Annotation) iter.next());
		}
	}
	
	/**
	 * Constructor creating an empty set
	 * @param tokens the token sequence the annotation set refers to
	 */
	public TemporaryAnnotationSet(TokenSequence tokens) {
		this.tokens = tokens;
	}
	
	/**
	 * Constructor creating a set with one annotation in it
	 * @param tokens the token sequence the annotation set refers to
	 * @param type the type of the annotation
	 * @param startIndex the start index of the annotation
	 * @param size the size of the annotation
	 */
	public TemporaryAnnotationSet(TokenSequence tokens, String type, int startIndex, int size) {
		this(new TemporaryAnnotation(tokens, type, startIndex, size));
	}
	
	/**
	 * Constructor creating a set with one annotation in it
	 * @param tokens the token sequence the annotation set refers to
	 * @param model the annotation to be initially contained in the set
	 */
	public TemporaryAnnotationSet(TokenSequence tokens, Annotation model) {
		this.tokens = tokens;
		this.type = model.getType();
		this.copyAttributes(model);
		this.addAnnotation(model);
	}
	
	/**
	 * Constructor creating a set with one annotation in it
	 * @param model the annotation to be initially contained in the set, and the
	 *            token sequence of which reference
	 */
	public TemporaryAnnotationSet(StandaloneAnnotation model) {
		this.tokens = model.getData();
		this.type = model.getType();
		this.copyAttributes(model);
		this.addAnnotation(model);
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String)
	 */
	public String getDocumentProperty(String propertyName) {
		if (this.tokens instanceof Annotation) {
			return ((Annotation) this.tokens).getDocumentProperty(propertyName);
		} else {
			String value = null;
			for (int a = 0; a < this.size(); a++) {
				value = this.get(a).getDocumentProperty(propertyName);
				if (value != null) return value;
			}
			return value;
		}
	}

	/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String, java.lang.String)
	 */
	public String getDocumentProperty(String propertyName, String defaultValue) {
		String value = null;
		
		if (this.tokens instanceof Annotation)
			value = ((Annotation) this.tokens).getDocumentProperty(propertyName);
		if (value != null) return value;
		
		for (int a = 0; a < this.size(); a++) {
			value = this.get(a).getDocumentProperty(propertyName);
			if (value != null) return value;
		}
		
		return defaultValue;
	}

	/** @see de.uka.ipd.idaho.gamta.Annotation#getDocumentPropertyNames()
	 */
	public String[] getDocumentPropertyNames() {
		String[] propertyNames = null;
		
		if (this.tokens instanceof Annotation)
			propertyNames = ((Annotation) this.tokens).getDocumentPropertyNames();
		if ((propertyNames != null) && (propertyNames.length != 0)) return propertyNames;
		
		for (int a = 0; a < this.size(); a++) {
			propertyNames = this.get(a).getDocumentPropertyNames();
			if ((propertyNames != null) && (propertyNames.length != 0)) return propertyNames;
		}
		
		return new String[0];
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		if (START_INDEX_ATTRIBUTE.equals(name)) return new Integer(this.getStartIndex());
		else if (SIZE_ATTRIBUTE.equals(name)) return new Integer(this.size());
		else if (END_INDEX_ATTRIBUTE.equals(name)) return new Integer(this.getEndIndex());
		else if (ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
		else if (ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.id;
		else return super.getAttribute(name);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#getAttribute(java.lang.String, java.lang.Object)
	 */
	public Object getAttribute(String name, Object def) {
		if (START_INDEX_ATTRIBUTE.equals(name)) return new Integer(this.getStartIndex());
		else if (SIZE_ATTRIBUTE.equals(name)) return new Integer(this.size());
		else if (END_INDEX_ATTRIBUTE.equals(name)) return new Integer(this.getEndIndex());
		else if (ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return this.getValue();
		else if (ANNOTATION_ID_ATTRIBUTE.equals(name)) return this.id;
		else return super.getAttribute(name, def);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#hasAttribute(java.lang.String)
	 */
	public boolean hasAttribute(String name) {
		return (START_INDEX_ATTRIBUTE.equals(name) || SIZE_ATTRIBUTE.equals(name) || END_INDEX_ATTRIBUTE.equals(name) || ANNOTATION_VALUE_ATTRIBUTE.equals(name) || ANNOTATION_ID_ATTRIBUTE.equals(name) || super.hasAttribute(name));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed#setAttribute(java.lang.String, java.lang.Object)
	 */
	public Object setAttribute(String name, Object value) {
		if (START_INDEX_ATTRIBUTE.equals(name) || SIZE_ATTRIBUTE.equals(name) || END_INDEX_ATTRIBUTE.equals(name) || ANNOTATION_VALUE_ATTRIBUTE.equals(name)) return value;
		else if (ANNOTATION_ID_ATTRIBUTE.equals(name)) {
			if ((value != null) && (value instanceof String) && (value.toString().trim().length() == this.id.length())) {
				String oldId = this.id;
				this.id = value.toString();
				return oldId;
			}
			else return value;
		}
		else return super.setAttribute(name, value);
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#addAnnotation(de.uka.ipd.idaho.gamta.Annotation)
	 */
	public void addAnnotation(Annotation annotation) {
//		System.out.println(id + " - AnnotationSet.add(" + annotation.toXML() + ") ...");
		
		//	check if new Annotation contained in set
		if (this.annotationMap.containsKey(annotation)) return;
		
		if (DEFAULT_ANNOTATION_TYPE.equals(this.type))
			this.type = annotation.getType();
		
		this.annotationArray = null;
		this.annotationMap.put(annotation, annotation);
		
		//	empty set, take start and end index from first Annotation added
		if (this.setStartIndex == -1) {
			this.setStartIndex = annotation.getStartIndex();
			this.setEndIndex = annotation.getEndIndex();
			
		//	all subsequent Annotations
		} else if (this.setStartIndex > annotation.getStartIndex())
			this.setStartIndex = annotation.getStartIndex();
		
		//	adjust end index
		if (this.setEndIndex < annotation.getEndIndex())
			this.setEndIndex = annotation.getEndIndex();
//		Collections.sort(this.annotations);
//		System.out.println(id + " - AnnotationSet.add() ==> " + this.setStartIndex + ", " + this.setEndIndex);
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#addAnnotation(de.uka.ipd.idaho.gamta.Annotation)
	 */
	public void addAll(AnnotationSet annotationSet) {
//		System.out.println(id + " - AnnotationSet.addAll() ...");
		
		//	adding AnnotationSet to previously empty set
		if (this.setStartIndex == -1) {
			this.setStartIndex = annotationSet.getStartIndex();
			this.setEndIndex = annotationSet.getEndIndex();
			
		//	already some Annotation present
		} else if (this.setStartIndex > annotationSet.getStartIndex())
			this.setStartIndex = annotationSet.getStartIndex();
		
		//	adust end index
		if (this.setEndIndex < annotationSet.getEndIndex())
			this.setEndIndex = annotationSet.getEndIndex();
		
		Annotation[] annotations = annotationSet.toArray();
		for (int a = 0; a < annotations.length; a++)
			if (!this.annotationMap.containsKey(annotations[a])) {
				this.annotationArray = null;
				this.annotationMap.put(annotations[a], annotations[a]);
			}
		
//		Collections.sort(this.annotations);
		
//		System.out.println(id + " - AnnotationSet.addAll() ==> " + this.setStartIndex + ", " + this.setEndIndex);
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#get(int)
	 */
	public Annotation get(int index) {
		if (this.annotationArray == null) this.buildAnnotArray();
		return this.annotationArray[index];
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#getSubset(java.lang.String)
	 */
	public AnnotationSet getSubset(String type) {
//		System.out.println(id + " - AnnotationSet.subSet(" + type + ") ==> " + this.setStartIndex + ", " + this.setEndIndex);
		
		if (type == null) return this;
		
		if (this.annotationArray == null) this.buildAnnotArray();
		
		AnnotationSet subSet = new TemporaryAnnotationSet(this.tokens);
		for (int a = 0; a < this.annotationArray.length; a++)
			if (type.equals(this.annotationArray[a].getType()))
				subSet.addAnnotation(this.annotationArray[a]);
		
		return subSet;
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#remove(int)
	 */
	public Annotation remove(int index) {
//		System.out.println(id + " - AnnotationSet.remove(" + index + ") ...");
		
		if (this.annotationArray == null) this.buildAnnotArray();
		Annotation removed = this.annotationArray[index];
		
		this.annotationArray = null;
		this.annotationMap.remove(removed);
		
		//	adjust start and end index if necessary
		if ((this.setStartIndex == removed.getStartIndex()) || (this.setEndIndex == removed.getEndIndex())) {
			
			//	set is empty now
			if (this.annotationMap.isEmpty()) {
				this.setStartIndex = -1;
				this.setEndIndex = 0;
				
			//	some Annotations left
			} else {
				this.setStartIndex = this.setEndIndex;
				this.setEndIndex = 0;
				for (Iterator iter = this.annotationMap.keySet().iterator(); iter.hasNext();) {
					Annotation annotation = ((Annotation) iter.next());
					if (this.setStartIndex > annotation.getStartIndex())
						this.setStartIndex = annotation.getStartIndex();
					if (this.setEndIndex < annotation.getEndIndex())
						this.setEndIndex = annotation.getEndIndex();
				}
			}
		}
		
//		System.out.println(id + " - AnnotationSet.remove() ==> " + this.setStartIndex + ", " + this.setEndIndex);
		return removed;
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#remove(de.uka.ipd.idaho.gamta.Annotation)
	 */
	public boolean remove(Annotation remove) {
//		System.out.println(id + " - AnnotationSet.remove(" + remove.toXML() + ") ...");
		
		if (this.annotationMap.remove(remove) != null) {
			this.annotationArray = null;
			
			//	adjust start and end index if necessary
			if ((this.setStartIndex == remove.getStartIndex()) || (this.setEndIndex == remove.getEndIndex())) {
				
				//	set is empty now
				if (this.annotationMap.isEmpty()) {
					this.setStartIndex = -1;
					this.setEndIndex = 0;
					
				//	some Annotations left
				} else {
					this.setStartIndex = this.setEndIndex;
					this.setEndIndex = 0;
					for (Iterator iter = this.annotationMap.keySet().iterator(); iter.hasNext();) {
						Annotation annotation = ((Annotation) iter.next());
						if (this.setStartIndex > annotation.getStartIndex())
							this.setStartIndex = annotation.getStartIndex();
						if (this.setEndIndex < annotation.getEndIndex())
							this.setEndIndex = annotation.getEndIndex();
					}
				}
			}
			
//			System.out.println(id + " - AnnotationSet.remove() ==> " + this.setStartIndex + ", " + this.setEndIndex);
			return true;
		} else return false;
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#removeAll(de.uka.ipd.idaho.gamta.AnnotationSet)
	 */
	public boolean removeAll(AnnotationSet removeSet) {
//		System.out.println(id + " - AnnotationSet.removeAll() ...");
		
		Annotation[] annotations = removeSet.toArray();
		boolean removed = false;
		int removeStart = this.getEndIndex();
		int removeEnd = 0;
		for (int a = 0; a < annotations.length; a++) {
			if (this.annotationMap.remove(annotations[a]) != null) {
				this.annotationArray = null;
				
				//	remember what was removed
				if (removeStart > annotations[a].getStartIndex())
					removeStart = annotations[a].getStartIndex();
				if (removeEnd < annotations[a].getEndIndex())
					removeEnd = annotations[a].getEndIndex();
				removed = true;
			}
		}
		
		//	nothing removed --> no re-computation necessary
		if (!removed) return false;
		
		//	adjust start and end index if necessary
		if ((this.setStartIndex == removeStart) || (this.setEndIndex == removeEnd)) {
			
			//	set is empty now
			if (this.annotationMap.isEmpty()) {
				this.setStartIndex = -1;
				this.setEndIndex = 0;
				
			//	some Annotations left
			} else {
				this.setStartIndex = this.setEndIndex;
				this.setEndIndex = 0;
				for (Iterator iter = this.annotationMap.keySet().iterator(); iter.hasNext();) {
					Annotation annotation = ((Annotation) iter.next());
					if (this.setStartIndex > annotation.getStartIndex())
						this.setStartIndex = annotation.getStartIndex();
					if (this.setEndIndex < annotation.getEndIndex())
						this.setEndIndex = annotation.getEndIndex();
				}
			}
		}
		
//		System.out.println(id + " - AnnotationSet.removeAll() ==> " + this.setStartIndex + ", " + this.setEndIndex);
		return removed;
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#annotationCount()
	 */
	public int annotationCount() {
		return this.annotationMap.size();
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#clear()
	 */
	public void clear() {
		this.annotationArray = null;
		this.annotationMap.clear();
		this.setStartIndex = -1;
		this.setEndIndex = 0;
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#isEmpty()
	 */
	public boolean isEmpty() {
		return this.annotationMap.isEmpty();
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#toArray()
	 */
	public Annotation[] toArray() {
		if (this.annotationArray == null) this.buildAnnotArray();
		Annotation[] annotations = new Annotation[this.annotationArray.length];
		System.arraycopy(this.annotationArray, 0, annotations, 0, this.annotationArray.length);
		return annotations;
	}
	
	/** @see de.uka.ipd.idaho.gamta.AnnotationSet#getSpannedParts()
	 */
	public AnnotationSet[] getSpannedParts() {
		//	TODO: divide Annotations in set into smaller sets covering continuous spans
		TemporaryAnnotationSet[] spanned = {this};
		return spanned;
	}
	
	/** @see de.uka.ipd.idaho.gamta.StandaloneAnnotation#getData()
	 */
	public TokenSequence getData() {
		return this.tokens;
	}

	/** @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
	 */
	public int getStartIndex() {
		return this.setStartIndex;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
	 */
	public int getEndIndex() {
		return this.setEndIndex;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getType()
	 */
	public String getType() {
		return this.type;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#changeTypeTo(java.lang.String)
	 */
	public String changeTypeTo(String newType) {
		if ((newType != null) && (newType.trim().length() != 0)) {
			String oldType = this.type;
			this.type = newType;
			return oldType;
		} else {
			return newType;
		}
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getAnnotationID()
	 */
	public String getAnnotationID() {
		return this.id;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getValue()
	 */
	public String getValue() {
		return this.tokens.subSequence(this.getStartOffset(), this.getEndOffset()).toString();
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#toXML()
	 */
	public String toXML() {
		return (AnnotationUtils.produceStartTag(this) + AnnotationUtils.escapeForXml(this.getValue()) + AnnotationUtils.produceEndTag(this));
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#tokenAt(int)
	 */
	public Token tokenAt(int index) {
		return this.tokens.tokenAt(this.getStartIndex() + index);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstToken()
	 */
	public Token firstToken() {
		return this.tokens.tokenAt(this.getStartIndex());
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastToken()
	 */
	public Token lastToken() {
		return this.tokens.tokenAt(this.getStartIndex() + this.size() - 1);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#valueAt(int)
	 */
	public String valueAt(int index) {
		return this.tokens.valueAt(this.getStartIndex() + index);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#firstValue()
	 */
	public String firstValue() {
		return this.tokens.valueAt(this.getStartIndex());
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#lastValue()
	 */
	public String lastValue() {
		return this.tokens.valueAt(this.getStartIndex() + this.size() - 1);
	}
	
	/** @see de.uka.ipd.idaho.gamta.Annotation#getLeadingWhitespace()
	 */
	public String getLeadingWhitespace() {
		return ((this.getStartIndex() == 0) ? this.tokens.getLeadingWhitespace() : "");
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#size()
	 */
	public int size() {
		return (this.setEndIndex - this.setStartIndex);
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#length()
	 */
	public int length() {
		return (this.getEndOffset() - this.getStartOffset());
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#getTokenizer()
	 */
	public Tokenizer getTokenizer() {
		return this.tokens.getTokenizer();
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequence#getSubsequence(int, int)
	 */
	public TokenSequence getSubsequence(int start, int size) {
		return this.tokens.getSubsequence((this.getStartIndex() + start), size);
	}
	
	/** @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o instanceof Annotation) {
			int c = AnnotationUtils.compare(this, ((Annotation) o));
			if (c == 0) return this.type.compareTo(((Annotation) o).getType());
			else return c;
		}
		return -1;
	}

	/** @see de.uka.ipd.idaho.gamta.TokenSequence#getWhitespaceAfter(int)
	 */
	public String getWhitespaceAfter(int index) {
		return this.tokens.getWhitespaceAfter(this.getStartIndex() + index);
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.CharSpan#getEndOffset()
	 */
	public int getEndOffset() {
		return this.lastToken().getEndOffset();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.CharSpan#getStartOffset()
	 */
	public int getStartOffset() {
		return this.firstToken().getStartOffset();
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		return this.tokens.charAt(this.getStartOffset() + index);
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return this.tokens.subSequence((this.getStartOffset() + start), (this.getStartOffset() + end));
	}
}
