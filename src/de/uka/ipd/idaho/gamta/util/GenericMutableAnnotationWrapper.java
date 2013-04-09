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


import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationListener;
import de.uka.ipd.idaho.gamta.CharSequenceListener;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableCharSequence;
import de.uka.ipd.idaho.gamta.MutableTokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceListener;

/**
 * This class implments a generic wrapper for arbitrary mutable annotations. It
 * loops all method calls through to the wrapped annotations. The purpose of
 * this class is to provide a standard wrapper implementation of all the methods
 * in MutableAnnotation in situations where some few of the methods need to be
 * added functionality through a wrapper class. Wrappers can simply extend this
 * class and overwrite methods as needed while avoiding having to implement all
 * the other methods as well. If Tokens and Annotations retrieved from this
 * wrapper need to be wrapped as well, the respective sub classes of this class
 * should overwrite the wrapToken(), wrapAnnotation() and
 * wrapMutableAnnotation() methods to provide the respective wrappers.
 * 
 * @author sautter
 */
public class GenericMutableAnnotationWrapper extends GenericQueriableAnnotationWrapper implements MutableAnnotation {
	
	/**
	 * the wrapped annotation (equal to the 'annotationData' and
	 * 'queriableAnnotationData' fields of super classes, existing to spare
	 * class casts)
	 */
	protected MutableAnnotation mutableAnnotationData;
	
	/** Constructor
	 * @param	data	the MutableAnnotation to wrap
	 */
	public GenericMutableAnnotationWrapper(MutableAnnotation data) {
		super(data);
		this.mutableAnnotationData = data;
	}
	
	public MutableAnnotation addAnnotation(Annotation annotation) {
		return this.wrapMutableAnnotation(this.mutableAnnotationData.addAnnotation(annotation));
	}
	
	public MutableAnnotation addAnnotation(String type, int startIndex, int size) {
		return this.wrapMutableAnnotation(this.mutableAnnotationData.addAnnotation(type, startIndex, size));
	}
	
	public MutableAnnotation[] getMutableAnnotations() {
		MutableAnnotation[] annotations = this.mutableAnnotationData.getMutableAnnotations();
		for (int a = 0; a < annotations.length; a++)
			annotations[a] = this.wrapMutableAnnotation(annotations[a]);
		return annotations;
	}
	
	public MutableAnnotation[] getMutableAnnotations(String type) {
		MutableAnnotation[] annotations = this.mutableAnnotationData.getMutableAnnotations(type);
		for (int a = 0; a < annotations.length; a++)
			annotations[a] = this.wrapMutableAnnotation(annotations[a]);
		return annotations;
	}
	
	public Annotation removeAnnotation(Annotation annotation) {
		return this.mutableAnnotationData.removeAnnotation(annotation);
	}
	
	public void addAnnotationListener(AnnotationListener al) {
		this.mutableAnnotationData.addAnnotationListener(al);
	}
	
	public void removeAnnotationListener(AnnotationListener al) {
		this.mutableAnnotationData.removeAnnotationListener(al);
	}
	
	/** wrap a MutableAnnotation before returning it in order to provide additional functionality through the wrapper class
	 * Note: This default implementation simply returns the argument MutableAnnotation, sub classes are welcome to overwrite this method as needed.
	 * @param	annotation	the MutableAnnotation to wrap
	 * @return the wrapped MutableAnnotation
	 */
	protected MutableAnnotation wrapMutableAnnotation(MutableAnnotation annotation) {
		return annotation;
	}
	
	public CharSequence addTokens(CharSequence tokens) {
		return this.mutableAnnotationData.addTokens(tokens);
	}
	
	public CharSequence insertTokensAt(CharSequence tokens, int index) {
		return this.mutableAnnotationData.insertTokensAt(tokens, index);
	}
	
	public TokenSequence removeTokensAt(int index, int size) {
		return this.mutableAnnotationData.removeTokensAt(index, size);
	}
	
	public TokenSequence removeTokens(Annotation annotation) {
		return this.mutableAnnotationData.removeTokens(annotation);
	}
	
	public void clear() {
		this.mutableAnnotationData.clear();
	}
	
	public MutableTokenSequence getMutableSubsequence(int start, int size) {
		return this.mutableAnnotationData.getMutableSubsequence(start, size);
	}
	
	public CharSequence setValueAt(CharSequence value, int index) throws IllegalArgumentException {
		return this.mutableAnnotationData.setValueAt(value, index);
	}
	
	public CharSequence setWhitespaceAfter(CharSequence whitespace, int index) throws IllegalArgumentException {
		return this.mutableAnnotationData.setWhitespaceAfter(whitespace, index);
	}
	
	public CharSequence setLeadingWhitespace(CharSequence whitespace) throws IllegalArgumentException {
		return this.mutableAnnotationData.setLeadingWhitespace(whitespace);
	}
	
	public void addTokenSequenceListener(TokenSequenceListener tsl) {
		this.mutableAnnotationData.addTokenSequenceListener(tsl);
	}
	
	public void removeTokenSequenceListener(TokenSequenceListener tsl) {
		this.mutableAnnotationData.removeTokenSequenceListener(tsl);
	}
	
	
	public void addChar(char ch) {
		this.mutableAnnotationData.addChar(ch);
	}
	
	public void addChars(CharSequence chars) {
		this.mutableAnnotationData.addChars(chars);
	}
	
	public void insertChar(char ch, int offset) {
		this.mutableAnnotationData.insertChar(ch, offset);
	}
	
	public void insertChars(CharSequence chars, int offset) {
		this.mutableAnnotationData.insertChars(chars, offset);
	}
	
	public char setChar(char ch, int offset) {
		return this.mutableAnnotationData.setChar(ch, offset);
	}
	
	public CharSequence setChars(CharSequence chars, int offset, int length) {
		return this.mutableAnnotationData.setChars(chars, offset, length);
	}
	
	public char removeChar(int offset) {
		return this.mutableAnnotationData.removeChar(offset);
	}
	
	public CharSequence removeChars(int offset, int length) {
		return this.mutableAnnotationData.removeChars(offset, length);
	}
	
	public MutableCharSequence mutableSubSequence(int start, int end) {
		return this.mutableAnnotationData.mutableSubSequence(start, end);
	}
	
	public void addCharSequenceListener(CharSequenceListener csl) {
		this.mutableAnnotationData.addCharSequenceListener(csl);
	}
	
	public void removeCharSequenceListener(CharSequenceListener csl) {
		this.mutableAnnotationData.removeCharSequenceListener(csl);
	}
}
