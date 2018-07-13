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
import java.util.Collections;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringVector;

public class AnnotationReasoner {
	
	/**
	 * clone Annotations of a particular type
	 * @param data the DocumentPart to process
	 * @param type the type of Annotation to clone
	 * @return an array of Annotations cloned from the original Annotations
	 */
	public static Annotation[] cloneAnnotations(QueriableAnnotation data, String type) {

		// check parameters
		if ((data == null) || (type == null)) return new Annotation[0];

		// clone Annotations
		Annotation[] basicAnnotations = data.getAnnotations(type);
		Annotation[] annotations = new Annotation[basicAnnotations.length];
		for (int a = 0; a < basicAnnotations.length; a++)
			annotations[a] = Gamta.newAnnotation(data, null, basicAnnotations[a].getStartIndex(), basicAnnotations[a].size());

		return annotations;
	}

	/**
	 * do value-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param type the type of Annotation to use
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking all occurences of a value String
	 *         that was initially annotated with the specified type
	 */
	public static Annotation[] doValueReasoning(QueriableAnnotation data, String type, boolean caseSensitive) {
		
		// check parameters
		if ((data == null) || (type == null)) return new Annotation[0];
		
		//	extract the values or Tokens already annotated
		int maxTokens = 1;
		Annotation[] basicAnnotations = data.getAnnotations(type);
		StringVector list = new StringVector(caseSensitive);
		for (int a = 0; a < basicAnnotations.length; a++) {
			list.addElementIgnoreDuplicates(basicAnnotations[a].getValue(), caseSensitive);
			if (basicAnnotations[a].size() > maxTokens) maxTokens = basicAnnotations[a].size();
		}
		
		Annotation[] annotations = Gamta.extractAllContained(data, list, maxTokens, caseSensitive);
		
//		//	remove Annotations that where in the basicAnnotation set
//		ArrayList annotationList = new ArrayList();
//		int basicAnnotationIndex = 0;
//		for (int a = 0; a < annotations.length; a++) {
//			
//			//	find possible duplicates
//			while ((basicAnnotationIndex < basicAnnotations.length) && basicAnnotations[basicAnnotationIndex].getStartIndex() < annotations[a].getStartIndex())
//				basicAnnotationIndex ++;
//			
//			//	no more basic Annotations left that could be equal to the new Annotation
//			if (basicAnnotationIndex == basicAnnotations.length) {
//				annotations[a].changeTypeTo(type);
//				annotationList.add(annotations[a]);
//			} else {
//				int i = 0;
//				while ((basicAnnotationIndex + i) < basicAnnotations.length) {
//					if (basicAnnotations[basicAnnotationIndex + i].getStartIndex() > annotations[a].getStartIndex()) {
//						annotations[a].changeTypeTo(type);
//						annotationList.add(annotations[a]);
//						i = basicAnnotations.length;
//					} else if (basicAnnotations[basicAnnotationIndex + i].size() < annotations[a].size()) {
//						annotations[a].changeTypeTo(type);
//						annotationList.add(annotations[a]);
//						i = basicAnnotations.length;
//					} else if (basicAnnotations[basicAnnotationIndex + i].size() == annotations[a].size()) {
//						i = basicAnnotations.length;
//					} else i++;
//				}
//			}
//		}
//		
//		return ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
		
		return annotations;
	}
	
	/**
	 * do Token-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param sourceType the type of Annotation to use
	 * @param targetType the type of Annotation to reason about
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking all sequences of Tokens contained
	 *         in an Annotation of the source type
	 */
	public static Annotation[] doTokenReasoning(QueriableAnnotation data, String sourceType, String targetType, boolean caseSensitive) {
		return doTokenReasoning(data, sourceType, targetType, 1, caseSensitive);
	}

	/**
	 * do Token-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param sourceType the type of Annotation to use
	 * @param targetType the type of Annotation to reason about
	 * @param minMatchFactor the minimum fraction of Tokens that must be in an
	 *            Annotation of the source type
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking all sequences of Tokens contained
	 *         in an Annotation of the source type
	 */
	public static Annotation[] doTokenReasoning(QueriableAnnotation data, String sourceType, String targetType, float minMatchFactor, boolean caseSensitive) {
		
		//	check parameters
		if ((data == null) || (sourceType == null) || (targetType == null) || (minMatchFactor < 0) || (minMatchFactor > 1)) return new Annotation[0];
		
		//	extract the values or Tokens already annotated
		Annotation[] basicAnnotations = data.getAnnotations(sourceType);
		StringVector list = new StringVector(caseSensitive);
		for (int a = 0; a < basicAnnotations.length; a++)
			list.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(basicAnnotations[a]), caseSensitive);
		
		Annotation[] annotations = data.getAnnotations(targetType);
		ArrayList annotationList = new ArrayList();
		
		for (int a = 0; a < annotations.length; a++) {
			int knownValueCount = 0;
			for (int v = 0; v < annotations[a].size(); v++)
				if (list.contains(annotations[a].valueAt(v))) knownValueCount ++;
				else if (!caseSensitive && list.containsIgnoreCase(annotations[a].valueAt(v))) knownValueCount ++;
			if (knownValueCount >= (minMatchFactor * annotations[a].size()))
				annotationList.add(Gamta.newAnnotation(data, null, annotations[a].getStartIndex(), annotations[a].size()));
		}
		
		return ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
	}
	
	/**
	 * do Token-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param type the type of Annotation to use
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking all sequences of Tokens contained
	 *         in an Annotation of the source type
	 */
	public static Annotation[] doTokenReasoning(QueriableAnnotation data, String type, boolean caseSensitive) {
		return doTokenReasoning(data, type, 0, null, false, caseSensitive);
	}

	/**
	 * do Token-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param type the type of Annotation to use
	 * @param maxSpanTokens the maximum number of unknown Tokens to span between
	 *            known ones
	 * @param list a Dictionary providing Token values not to span
	 * @param include true: span only Tokens contained in list, false: span only
	 *            Tokens not contained in list
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking all sequences of Tokens contained
	 *         in an Annotation of the source type
	 */
	public static Annotation[] doTokenReasoning(QueriableAnnotation data, String type, int maxSpanTokens, Dictionary list, boolean include, boolean caseSensitive) {
		
		//	check parameters
		if ((data == null) || (type == null)) return new Annotation[0];
		int maxSpan = ((maxSpanTokens > 0) ? maxSpanTokens : 0);
		StringVector inExcludeList = new StringVector();
		if (list != null) {
			StringIterator si = list.getEntryIterator();
			while (si.hasMoreStrings())
				inExcludeList.addElementIgnoreDuplicates(si.nextString(), caseSensitive);
		}
		
		//	extract the values or Tokens already annotated
		Annotation[] basicAnnotations = data.getAnnotations(type);
		StringVector tokenList = new StringVector(caseSensitive);
		for (int a = 0; a < basicAnnotations.length; a++)
			tokenList.addContentIgnoreDuplicates(TokenSequenceUtils.getTextTokens(basicAnnotations[a]), caseSensitive);
		
		Annotation[] annotations = Gamta.extractAllContained(data, tokenList, 1, caseSensitive);
		
		//	join neighbored Annotations
		if (annotations.length != 0) {
			ArrayList annotationList = new ArrayList();
			Annotation currentAnnotation = annotations[0];
			for (int a = 1; a < annotations.length; a++) {
				if (currentAnnotation.getEndIndex() == annotations[a].getStartIndex())
					currentAnnotation = Gamta.newAnnotation(data, currentAnnotation.getType(), currentAnnotation.getStartIndex(), (currentAnnotation.size() + annotations[a].size()));
				
				else if ((currentAnnotation.getEndIndex() + maxSpan) >= annotations[a].getStartIndex()) {
					
					//	check if span possible
					StringVector spanTokens = new StringVector();
					for (int v = currentAnnotation.getEndIndex(); v < annotations[a].getStartIndex(); v++)
						spanTokens.addElementIgnoreDuplicates(data.valueAt(v));
					
					if (include) spanTokens = spanTokens.without(inExcludeList, caseSensitive);
					else spanTokens = spanTokens.intersect(inExcludeList, caseSensitive);
					
					//	span possible
					if (spanTokens.size() == 0)
						currentAnnotation = Gamta.newAnnotation(data, currentAnnotation.getType(), currentAnnotation.getStartIndex(), (annotations[a].getEndIndex() - currentAnnotation.getStartIndex()));
						
					//	span not possible
					else {
						annotationList.add(currentAnnotation);
						currentAnnotation = annotations[a];
					}
				}
				else if (currentAnnotation.getEndIndex() < annotations[a].getStartIndex()) {
					annotationList.add(currentAnnotation);
					currentAnnotation = annotations[a];
				}
			}
			annotationList.add(currentAnnotation);
			annotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
		}
		
//		//	remove Annotations that where in the basicAnnotation set
//		ArrayList annotationList = new ArrayList();
//		int basicAnnotationIndex = 0;
//		for (int a = 0; a < annotations.length; a++) {
//			
//			//	find possible duplicates
//			while ((basicAnnotationIndex < basicAnnotations.length) && basicAnnotations[basicAnnotationIndex].getStartIndex() < annotations[a].getStartIndex())
//				basicAnnotationIndex ++;
//			
//			//	no more basic Annotations left that could be equal to the new Annotation
//			if (basicAnnotationIndex == basicAnnotations.length) {
//				annotations[a].changeTypeTo(type);
//				annotationList.add(annotations[a]);
//				
//			//	remove duplicates
//			} else {
//				int i = 0;
//				while ((basicAnnotationIndex + i) < basicAnnotations.length) {
//					if (basicAnnotations[basicAnnotationIndex + i].getStartIndex() > annotations[a].getStartIndex()) {
//						annotations[a].changeTypeTo(type);
//						annotationList.add(annotations[a]);
//						i = basicAnnotations.length;
//					} else if (basicAnnotations[basicAnnotationIndex + i].size() < annotations[a].size()) {
//						annotations[a].changeTypeTo(type);
//						annotationList.add(annotations[a]);
//						i = basicAnnotations.length;
//					} else if (basicAnnotations[basicAnnotationIndex + i].size() == annotations[a].size()) {
//						i = basicAnnotations.length;
//					} else i++;
//				}
//			}
//		}
//		
//		return ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
		
		return annotations;
	}
	
	/**
	 * join neighbouring Annotations of a given type
	 * @param data the DocumentPart to process
	 * @param type the type of Annotation to use
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking all sequences of Tokens contained
	 *         in an Annotation of the source type
	 */
	public static Annotation[] joinAnnotations(QueriableAnnotation data, String type, boolean caseSensitive) {
		return joinAnnotations(data, type, 0, null, false, caseSensitive);
	}

	/**
	 * join neighbouring Annotations of a given type
	 * @param data the DocumentPart to process
	 * @param type the type of Annotation to use
	 * @param maxSpanTokens the maximum number of unknown Tokens to span between
	 *            known ones
	 * @param list a Dictionary providing Token values not to span
	 * @param include true: span only Tokens contained in list, false: span only
	 *            Tokens not contained in list
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking all sequences of Tokens contained
	 *         in an Annotation of the source type
	 */
	public static Annotation[] joinAnnotations(QueriableAnnotation data, String type, int maxSpanTokens, Dictionary list, boolean include, boolean caseSensitive) {
		
		//	check parameters
		if ((data == null) || (type == null)) return new Annotation[0];
		int maxSpan = ((maxSpanTokens > 0) ? maxSpanTokens : 0);
		StringVector inExcludeList = new StringVector();
		if (list != null) {
			StringIterator si = list.getEntryIterator();
			while (si.hasMoreStrings()) inExcludeList.addElementIgnoreDuplicates(si.nextString(), caseSensitive);
		}
		
		//	extract the values or Tokens already annotated
//		Annotation[] basicAnnotations = data.getAnnotations(type);
		Annotation[] annotations = data.getAnnotations(type);
		
		//	join neighbored Annotations
		if (annotations.length != 0) {
			ArrayList annotationList = new ArrayList();
			Annotation currentAnnotation = annotations[0];
			for (int a = 1; a < annotations.length; a++) {
				if (currentAnnotation.getEndIndex() == annotations[a].getStartIndex())
					currentAnnotation = Gamta.newAnnotation(data, currentAnnotation.getType(), currentAnnotation.getStartIndex(), (currentAnnotation.size() + annotations[a].size()));
				
				else if ((currentAnnotation.getEndIndex() + maxSpan) >= annotations[a].getStartIndex()) {
					
					//	check if span possible
					StringVector spanTokens = new StringVector();
					for (int v = currentAnnotation.getEndIndex(); v < annotations[a].getStartIndex(); v++)
						spanTokens.addElementIgnoreDuplicates(data.valueAt(v));
					
					if (include) spanTokens = spanTokens.without(inExcludeList, caseSensitive);
					else spanTokens = spanTokens.intersect(inExcludeList, caseSensitive);
					
					//	span possible
					if (spanTokens.size() == 0)
						currentAnnotation = Gamta.newAnnotation(data, currentAnnotation.getType(), currentAnnotation.getStartIndex(), (annotations[a].getEndIndex() - currentAnnotation.getStartIndex()));
						
					//	span not possible
					else {
						annotationList.add(currentAnnotation);
						currentAnnotation = annotations[a];
					}
				}
				else if (currentAnnotation.getEndIndex() < annotations[a].getStartIndex()) {
					annotationList.add(currentAnnotation);
					currentAnnotation = annotations[a];
				}
			}
			annotationList.add(currentAnnotation);
			annotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
		}
		
//		//	remove Annotations that where in the basicAnnotation set
//		ArrayList annotationList = new ArrayList();
//		int basicAnnotationIndex = 0;
//		for (int a = 0; a < annotations.length; a++) {
//			
//			//	find possible duplicates
//			while ((basicAnnotationIndex < basicAnnotations.length) && basicAnnotations[basicAnnotationIndex].getStartIndex() < annotations[a].getStartIndex())
//				basicAnnotationIndex ++;
//			
//			//	no more basic Annotations left that could be equal to the new Annotation
//			if (basicAnnotationIndex == basicAnnotations.length) {
//				annotations[a].changeTypeTo(type);
//				annotationList.add(annotations[a]);
//				
//			//	remove duplicates
//			} else {
//				int i = 0;
//				while ((basicAnnotationIndex + i) < basicAnnotations.length) {
//					if (basicAnnotations[basicAnnotationIndex + i].getStartIndex() > annotations[a].getStartIndex()) {
//						annotations[a].changeTypeTo(type);
//						annotationList.add(annotations[a]);
//						i = basicAnnotations.length;
//					} else if (basicAnnotations[basicAnnotationIndex + i].size() < annotations[a].size()) {
//						annotations[a].changeTypeTo(type);
//						annotationList.add(annotations[a]);
//						i = basicAnnotations.length;
//					} else if (basicAnnotations[basicAnnotationIndex + i].size() == annotations[a].size()) {
//						i = basicAnnotations.length;
//					} else i++;
//				}
//			}
//		}
//		
//		return ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
		
		return annotations;
	}
	
	/**
	 * do environment-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param sourceType the type of Annotation to use
	 * @param targetType the type of Annotation to reason about
	 * @param envWidth the width of the environtent to use (in Tokens to the
	 *            left and right of the target Annotations)
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking target type Annnotations that
	 *         have the same environment as one of the initial source type
	 *         Annotations
	 */
	public static Annotation[] doEnvironmentReasoning(QueriableAnnotation data, String sourceType, String targetType, int envWidth, boolean caseSensitive) {
		return doEnvironmentReasoning(data, sourceType, targetType, envWidth, envWidth, caseSensitive);
	}

	/**
	 * do environment-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param sourceType the type of Annotation to use
	 * @param targetType the type of Annotation to reason about
	 * @param leftEnvWidth the number of environtent Tokens to use to the left
	 *            of the target Annotations
	 * @param rightEnvWidth the number of environtent Tokens to use to the right
	 *            of the target Annotations
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking target type Annnotations that
	 *         have the same environment as one of the initial source type
	 *         Annotations
	 */
	public static Annotation[] doEnvironmentReasoning(QueriableAnnotation data, String sourceType, String targetType, int leftEnvWidth, int rightEnvWidth, boolean caseSensitive) {
		return doEnvironmentReasoning(data, sourceType, targetType, (leftEnvWidth + rightEnvWidth), leftEnvWidth, rightEnvWidth, caseSensitive);
	}

	/**
	 * do environment-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param sourceType the type of Annotation to use
	 * @param targetType the type of Annotation to reason about
	 * @param envWidth the maximum overall number of environment tokens (is this
	 *            number is larger than the sum of leftEnvWidth and
	 *            rightEnvWidth, the environment will shift from left to right,
	 *            having always at least leftEnvWidth tokens on the left side
	 *            and rightEnvWidth tokens on the right side)
	 * @param leftEnvWidth the number of environtent Tokens to use to the left
	 *            of the target Annotations
	 * @param rightEnvWidth the number of environtent Tokens to use to the right
	 *            of the target Annotations
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @return an array of Annotations marking target type Annnotations that
	 *         have the same environment as one of the initial source type
	 *         Annotations
	 */
	public static Annotation[] doEnvironmentReasoning(QueriableAnnotation data, String sourceType, String targetType, int envWidth, int leftEnvWidth, int rightEnvWidth, boolean caseSensitive) {
		
		//	check parameters
		if ((data == null) || (sourceType == null) || (targetType == null) || ((leftEnvWidth + rightEnvWidth) < 1)) return new Annotation[0];
		
		//	adjust overall width
		envWidth = Math.max(envWidth, (leftEnvWidth + rightEnvWidth));
		
		//	collect environments
		Annotation[] basicAnnotations = data.getAnnotations(sourceType);
		StringVector environments = new StringVector(caseSensitive);
		StringVector environment = new StringVector();
		int index;
		for (int a = 0; a < basicAnnotations.length; a++) {
			
			//	switch from outmost left to outmost right environment
			for (int effectiveLeftEnv, effectiveRightEnv = rightEnvWidth;
				(effectiveLeftEnv = (envWidth - effectiveRightEnv)) >= leftEnvWidth;
				effectiveRightEnv++) {
				
				environment.clear();
				
				//	extract environment
				int left = basicAnnotations[a].getStartIndex() - effectiveLeftEnv;
				if (left < 0) left = 0;
				index = left;
				while (index < basicAnnotations[a].getStartIndex()) {
					environment.addElement(data.valueAt(index));
					index++;
				}
				
				int right = basicAnnotations[a].getEndIndex() + effectiveRightEnv;
				if (right > data.size()) right = data.size();
				index = basicAnnotations[a].getEndIndex();
				while (index < right) {
					environment.addElement(data.valueAt(index));
					index++;
				}
				
				//	store environment
				environments.addElementIgnoreDuplicates(environment.concatStrings(" "), caseSensitive);
			}
			
//			environment.clear();
//			
//			//	extract environment
//			int left = basicAnnotations[a].getStartIndex() - leftEnvWidth;
//			if (left < 0) left = 0;
//			index = left;
//			while (index < basicAnnotations[a].getStartIndex()) {
//				environment.addElement(data.valueAt(index));
//				index++;
//			}
//			
//			int right = basicAnnotations[a].getEndIndex() + rightEnvWidth;
//			if (right > data.size()) right = data.size();
//			index = basicAnnotations[a].getEndIndex();
//			while (index < right) {
//				environment.addElement(data.valueAt(index));
//				index++;
//			}
//			
//			//	store environment
//			environments.addElementIgnoreDuplicates(environment.concatStrings(" "), caseSensitive);
		}
		
		Annotation[] targetAnnotations = data.getAnnotations(targetType);
		ArrayList annotationList = new ArrayList();
		
		for (int a = 0; a < targetAnnotations.length; a++) {
			
			//	switch from outmost left to outmost right environment
			for (int effectiveLeftEnv, effectiveRightEnv = rightEnvWidth;
				(effectiveLeftEnv = (envWidth - effectiveRightEnv)) >= leftEnvWidth;
				effectiveRightEnv++) {
				
				environment.clear();
				
				//	extract environment
				int left = targetAnnotations[a].getStartIndex() - effectiveLeftEnv;
				if (left < 0) left = 0;
				index = left;
				while (index < targetAnnotations[a].getStartIndex()) {
					environment.addElement(data.valueAt(index));
					index++;
				}
				
				int right = targetAnnotations[a].getEndIndex() + effectiveRightEnv;
				if (right > data.size()) right = data.size();
				index = targetAnnotations[a].getEndIndex();
				while (index < right) {
					environment.addElement(data.valueAt(index));
					index++;
				}
				
				//	lookup environment
				if (environments.contains(environment.concatStrings(" "))
						|| (!caseSensitive && environments.containsIgnoreCase(environment.concatStrings(" ")))
						) {
					annotationList.add(Gamta.newAnnotation(data, null, targetAnnotations[a].getStartIndex(), targetAnnotations[a].size()));
					
					//	terminate environment shift loop
					effectiveRightEnv = (envWidth + leftEnvWidth + 1);
				}
			}
			
//			environment.clear();
//			
//			//	extract environment
//			int left = annotations[a].getStartIndex() - leftEnvWidth;
//			if (left < 0) left = 0;
//			index = left;
//			while (index < annotations[a].getStartIndex()) {
//				environment.addElement(data.valueAt(index));
//				index++;
//			}
//			
//			int right = annotations[a].getEndIndex() + rightEnvWidth;
//			if (right > data.size()) right = data.size();
//			index = annotations[a].getEndIndex();
//			while (index < right) {
//				environment.addElement(data.valueAt(index));
//				index++;
//			}
//			
//			//	lookup environment
//			if (environments.contains(environment.concatStrings(" "))
//					|| (!caseSensitive && environments.containsIgnoreCase(environment.concatStrings(" ")))
//					) {
//				annotationList.add(Gamta.newAnnotation(data, null, targetAnnotations[a].getStartIndex(), targetAnnotations[a].size()));
//			}
		}
		
//		//	remove duplicates
//		annotations = ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
//		annotationList.clear();
//		int basicAnnotationIndex = 0;
//		for (int a = 0; a < annotations.length; a++) {
//			
//			//	find possible duplicates
//			while ((basicAnnotationIndex < basicAnnotations.length) && basicAnnotations[basicAnnotationIndex].getStartIndex() < annotations[a].getStartIndex())
//				basicAnnotationIndex ++;
//			
//			//	no more basic Annotations left that could be equal to the new Annotation
//			if (basicAnnotationIndex == basicAnnotations.length) {
//				annotationList.add(annotations[a]);
//			} else {
//				int i = 0;
//				while ((basicAnnotationIndex + i) < basicAnnotations.length) {
//					if (basicAnnotations[basicAnnotationIndex + i].getStartIndex() > annotations[a].getStartIndex()) {
//						annotationList.add(annotations[a]);
//						i = basicAnnotations.length;
//					} else if (basicAnnotations[basicAnnotationIndex + i].size() < annotations[a].size()) {
//						annotationList.add(annotations[a]);
//						i = basicAnnotations.length;
//					} else if (basicAnnotations[basicAnnotationIndex + i].size() == annotations[a].size()) {
//						i = basicAnnotations.length;
//					} else i++;
//				}
//			}
//		}
		
		return ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
	}
	
	/**
	 * do iterative value- and environment-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param sourceType the type of Annotation to use
	 * @param targetType the type of Annotation to reason about
	 * @param envWidth the width of the environtent to use (in Tokens to the
	 *            left and right of the target Annotations)
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @param maxRounds the maximum number of value reasoning environment
	 *            reasoning rounds (0 means no limit, will run until no new
	 *            Annotations found)
	 * @return an array of Annotations marking target type Annnotations that
	 *         have the same environment as one of the initial source type
	 *         Annotations
	 */
	public static Annotation[] doValueEnvironmentReasoning(QueriableAnnotation data, String sourceType, String targetType, int envWidth, boolean caseSensitive, int maxRounds) {
		return doEnvironmentReasoning(data, sourceType, targetType, envWidth, envWidth, caseSensitive);
	}

	/**
	 * do iterative value- and environment-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param sourceType the type of Annotation to use
	 * @param targetType the type of Annotation to reason about
	 * @param leftEnvWidth the number of environtent Tokens to use to the left
	 *            of the target Annotations
	 * @param rightEnvWidth the number of environtent Tokens to use to the right
	 *            of the target Annotations
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @param maxRounds the maximum number of value reasoning environment
	 *            reasoning rounds (0 means no limit, will run until no new
	 *            Annotations found)
	 * @return an array of Annotations marking target type Annnotations that
	 *         have the same environment as one of the initial source type
	 *         Annotations
	 */
	public static Annotation[] doValueEnvironmentReasoning(QueriableAnnotation data, String sourceType, String targetType, int leftEnvWidth, int rightEnvWidth, boolean caseSensitive, int maxRounds) {
		return doValueEnvironmentReasoning(data, sourceType, targetType, (leftEnvWidth + rightEnvWidth), leftEnvWidth, rightEnvWidth, caseSensitive, maxRounds);
	}
	
	/**
	 * do iterative value- and environment-based reasoning over Annotations
	 * @param data the DocumentPart to process
	 * @param sourceType the type of Annotation to use
	 * @param targetType the type of Annotation to reason about
	 * @param envWidth the maximum overall number of environment tokens (is this
	 *            number is larger than the sum of leftEnvWidth and
	 *            rightEnvWidth, the environment will shift from left to right,
	 *            having always at least leftEnvWidth tokens on the left side
	 *            and rightEnvWidth tokens on the right side)
	 * @param leftEnvWidth the number of environtent Tokens to use to the left
	 *            of the target Annotations
	 * @param rightEnvWidth the number of environtent Tokens to use to the right
	 *            of the target Annotations
	 * @param caseSensitive use case sensitive or case insensitive value
	 *            matching
	 * @param maxRounds the maximum number of value reasoning environment
	 *            reasoning rounds (0 means no limit, will run until no new
	 *            Annotations found)
	 * @return an array of Annotations marking target type Annnotations that
	 *         have the same environment as one of the initial source type
	 *         Annotations
	 */
	public static Annotation[] doValueEnvironmentReasoning(QueriableAnnotation data, String sourceType, String targetType, int envWidth, int leftEnvWidth, int rightEnvWidth, boolean caseSensitive, int maxRounds) {
		
		//	check parameters
		if ((data == null) || (sourceType == null) || (targetType == null) || ((leftEnvWidth + rightEnvWidth) < 1)) return new Annotation[0];
		int maximumRounds = ((maxRounds < 1) ? Integer.MAX_VALUE : maxRounds);
		
		//	adjust overall width
		envWidth = Math.max(envWidth, (leftEnvWidth + rightEnvWidth));
		
		//	collect basic values and environments
		Annotation[] sourceAnnotations = data.getAnnotations(sourceType);
		
		StringVector values = new StringVector();
		
		StringVector environments = new StringVector(caseSensitive);
		StringVector environment = new StringVector();
		int index;
		
		for (int a = 0; a < sourceAnnotations.length; a++) {
			
			//	store value
			values.addElementIgnoreDuplicates(sourceAnnotations[a].getValue(), caseSensitive);
			
			
			//	switch from outmost left to outmost right environment
			for (int effectiveLeftEnv, effectiveRightEnv = rightEnvWidth;
				(effectiveLeftEnv = (envWidth - effectiveRightEnv)) >= leftEnvWidth;
				effectiveRightEnv++) {
				
				environment.clear();
				
				//	extract environment
				int left = sourceAnnotations[a].getStartIndex() - effectiveLeftEnv;
				if (left < 0) left = 0;
				index = left;
				while (index < sourceAnnotations[a].getStartIndex()) {
					environment.addElement(data.valueAt(index));
					index++;
				}
				
				int right = sourceAnnotations[a].getEndIndex() + effectiveRightEnv;
				if (right > data.size()) right = data.size();
				index = sourceAnnotations[a].getEndIndex();
				while (index < right) {
					environment.addElement(data.valueAt(index));
					index++;
				}
				
				//	store environment
				environments.addElementIgnoreDuplicates(environment.concatStrings(" "), caseSensitive);
			}
			
//			//	extract environment
//			environment.clear();
//			int left = sourceAnnotations[a].getStartIndex() - leftEnvWidth;
//			if (left < 0) left = 0;
//			index = left;
//			while (index < sourceAnnotations[a].getStartIndex()) {
//				environment.addElement(data.valueAt(index));
//				index++;
//			}
//			
//			int right = sourceAnnotations[a].getEndIndex() + rightEnvWidth;
//			if (right > data.size()) right = data.size();
//			index = sourceAnnotations[a].getEndIndex();
//			while (index < right) {
//				environment.addElement(data.valueAt(index));
//				index++;
//			}
//			
//			//	store environment
//			environments.addElementIgnoreDuplicates(environment.concatStrings(" "), caseSensitive);
		}
		
		//	get subjects of reasoning
		Annotation[] targetAnnotations = data.getAnnotations(targetType);
		ArrayList annotationList = new ArrayList();
		
		//	iterate
		int round = 0;
		boolean newAnnotation;
		do {
			newAnnotation = false;
			
			//	do value reasoning, collecting environments on the fly
			for (int a = 0; a < targetAnnotations.length; a++) {
				if ((targetAnnotations[a] != null) && (caseSensitive ? values.contains(targetAnnotations[a].getValue()) : values.containsIgnoreCase(targetAnnotations[a].getValue()))) {
					annotationList.add(Gamta.newAnnotation(data, null, targetAnnotations[a].getStartIndex(), targetAnnotations[a].size()));
					newAnnotation = true;
					
					//	switch from outmost left to outmost right environment
					for (int effectiveLeftEnv, effectiveRightEnv = rightEnvWidth;
						(effectiveLeftEnv = (envWidth - effectiveRightEnv)) >= leftEnvWidth;
						effectiveRightEnv++) {
						
						environment.clear();
						
						//	extract environment
						int left = targetAnnotations[a].getStartIndex() - effectiveLeftEnv;
						if (left < 0) left = 0;
						index = left;
						while (index < targetAnnotations[a].getStartIndex()) {
							environment.addElement(data.valueAt(index));
							index++;
						}
						
						int right = targetAnnotations[a].getEndIndex() + effectiveRightEnv;
						if (right > data.size()) right = data.size();
						index = targetAnnotations[a].getEndIndex();
						while (index < right) {
							environment.addElement(data.valueAt(index));
							index++;
						}
						
						//	store environment
						environments.addElementIgnoreDuplicates(environment.concatStrings(" "), caseSensitive);
					}
					
//					//	extract environment
//					environment.clear();
//					int left = targetAnnotations[a].getStartIndex() - leftEnvWidth;
//					if (left < 0) left = 0;
//					index = left;
//					while (index < targetAnnotations[a].getStartIndex()) {
//						environment.addElement(data.valueAt(index));
//						index++;
//					}
//					
//					int right = targetAnnotations[a].getEndIndex() + rightEnvWidth;
//					if (right > data.size()) right = data.size();
//					index = targetAnnotations[a].getEndIndex();
//					while (index < right) {
//						environment.addElement(data.valueAt(index));
//						index++;
//					}
//					
//					//	store environment
//					environments.addElementIgnoreDuplicates(environment.concatStrings(" "), caseSensitive);
					
					//	remove approved Annotation from targets (saves duplicate elimination)
					targetAnnotations[a] = null;
				}
			}
			
			//	do environment reasoning, collecting values on the fly
			for (int a = 0; a < targetAnnotations.length; a++) {
				if (targetAnnotations[a] != null) {
					
					//	switch from outmost left to outmost right environment
					for (int effectiveLeftEnv, effectiveRightEnv = rightEnvWidth;
						(effectiveLeftEnv = (envWidth - effectiveRightEnv)) >= leftEnvWidth;
						effectiveRightEnv++) {
						
						environment.clear();
						
						//	extract environment
						int left = targetAnnotations[a].getStartIndex() - effectiveLeftEnv;
						if (left < 0) left = 0;
						index = left;
						while (index < targetAnnotations[a].getStartIndex()) {
							environment.addElement(data.valueAt(index));
							index++;
						}
						
						int right = targetAnnotations[a].getEndIndex() + effectiveRightEnv;
						if (right > data.size()) right = data.size();
						index = targetAnnotations[a].getEndIndex();
						while (index < right) {
							environment.addElement(data.valueAt(index));
							index++;
						}
						
						//	lookup environment
						if (caseSensitive ? environments.contains(environment.concatStrings(" ")) : environments.containsIgnoreCase(environment.concatStrings(" "))) {
							
							//	store value for next round
							values.addElementIgnoreDuplicates(targetAnnotations[a].getValue(), caseSensitive);
							
							/*
							 * do not add annotation immediately (except in last
							 * round or if environment is not shifting), for
							 * this would prevent all environments except not
							 * yet collected (possibly all but the matched one)
							 * from being entered in the list. Just remember
							 * value if there's a next round to do the rest
							 */
							if ((round + 1) == maximumRounds) {
								annotationList.add(Gamta.newAnnotation(data, null, targetAnnotations[a].getStartIndex(), targetAnnotations[a].size()));
								
								//	remove approved Annotation from targets (saves duplicate elimination)
								targetAnnotations[a] = null;
							}
							
							else if ((leftEnvWidth + rightEnvWidth) == envWidth) {
								annotationList.add(Gamta.newAnnotation(data, null, targetAnnotations[a].getStartIndex(), targetAnnotations[a].size()));
								
								//	remove approved Annotation from targets (saves duplicate elimination)
								targetAnnotations[a] = null;
							}
							newAnnotation = true;
							
							//	terminate environment shift loop
							effectiveRightEnv = (envWidth + leftEnvWidth + 1);
						}
					}
					
//					//	extract environment
//					environment.clear();
//					int left = targetAnnotations[a].getStartIndex() - leftEnvWidth;
//					if (left < 0) left = 0;
//					index = left;
//					while (index < targetAnnotations[a].getStartIndex()) {
//						environment.addElement(data.valueAt(index));
//						index++;
//					}
//					
//					int right = targetAnnotations[a].getEndIndex() + rightEnvWidth;
//					if (right > data.size()) right = data.size();
//					index = targetAnnotations[a].getEndIndex();
//					while (index < right) {
//						environment.addElement(data.valueAt(index));
//						index++;
//					}
//					
//					//	lookup environment
//					if (caseSensitive ? environments.contains(environment.concatStrings(" ")) : environments.containsIgnoreCase(environment.concatStrings(" "))) {
//						annotationList.add(Gamta.newAnnotation(data, null, targetAnnotations[a].getStartIndex(), targetAnnotations[a].size()));
//						newAnnotation = true;
//						
//						//	store value for next round
//						values.addElementIgnoreDuplicates(targetAnnotations[a].getValue(), caseSensitive);
//						
//						//	remove approved Annotation from targets (saves duplicate elimination)
//						targetAnnotations[a] = null;
//					}
				}
			}
			
			//	switch to next round
			round ++;
		}
		
		//	check whether to continue
		while (newAnnotation && (round < maximumRounds));
		
		//	sort and return result Annotations
		Collections.sort(annotationList);
		return ((Annotation[]) annotationList.toArray(new Annotation[annotationList.size()]));
	}
}
