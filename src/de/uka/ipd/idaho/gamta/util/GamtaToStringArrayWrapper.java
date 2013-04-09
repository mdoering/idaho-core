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


import java.util.HashMap;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.TokenSequenceListener;
import de.uka.ipd.idaho.gamta.MutableTokenSequence.TokenSequenceEvent;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This class is intended to facilitate the application of String array based NLP to GAMTA documents.
 * 
 * @author sautter
 *
 */
public class GamtaToStringArrayWrapper implements TokenSequenceListener {
	
	/**	the marker String indicating that a new Annotation starts at the current index
	 */
	public static final String START = "START";
	
	/**	the marker String indicating that the Annotation last started continues with the current index 
	 */
	public static final String CONTINUE = "CONTINUE";
	
	/**	the marker String indicating that the current index does not belong to an Annotation
	 */
	public static final String OTHER = "OTHER";
	
	//	the document wrapped
	private MutableAnnotation document;
	
	//	token array cache
	private String[] tokens = null;
	
	//	marker array cache
	private StringVector markerArrayCacheKeys = new StringVector();
	private HashMap markerArrayCache = new HashMap();
	
	//	mapping of default marker Strings to custom ones
	private Properties markerMappings = new Properties();
	
	/**	Constructor
	 * @param	document	the DocumentPart to wrap
	 */
	public GamtaToStringArrayWrapper(MutableAnnotation document) {
		this(document, START, CONTINUE, OTHER);
	}
	
	/**	Constructor
	 * @param	document		the DocumentPart to wrap
	 * @param	markerMappings	a Properties object specifying a mapping of the default marker Strings to custom marker Strings
	 */
	public GamtaToStringArrayWrapper(MutableAnnotation document, Properties markerMappings) {
		this(document, 
			((markerMappings == null) ? START : markerMappings.getProperty(START, START)),
			((markerMappings == null) ? CONTINUE : markerMappings.getProperty(CONTINUE, CONTINUE)),
			((markerMappings == null) ? OTHER : markerMappings.getProperty(OTHER, OTHER))
			);
	}
	
	/**	Constructor
	 * @param	document		the DocumentPart to wrap
	 * @param	startMarker		the marker String indicating an Annotation starting at a token
	 * @param	continueMarker	the marker String indicating a token continuing an Annotation
	 * @param	otherMarker		the marker String indicating a token not being part of an Annotation
	 */
	public GamtaToStringArrayWrapper(MutableAnnotation document, String startMarker, String continueMarker, String otherMarker) {
		this.document = document;
		this.document.addTokenSequenceListener(this);
		if (startMarker != null) this.markerMappings.setProperty(START, startMarker);
		if (continueMarker != null) this.markerMappings.setProperty(CONTINUE, continueMarker);
		if (otherMarker != null) this.markerMappings.setProperty(OTHER, otherMarker);
	}
	
	/**	@return	a String array representation of the document wrapped in this wrapper.
	 */
	public String[] getTokens() {
		if (this.tokens == null) this.createTokenArray();
		return this.tokens;
	}
	
	/**	obtain an array of marker Strings for Annotations of a given type
	 * @param	annotationType	the type of Annotations to obtain a marker array for
	 * @return an array of marker Strings for Annotations of the specified type
	 */
	public String[] getMarkerArray(String annotationType) {
		return this.getMarkerArray(annotationType, this.markerMappings);
	}
	
	/**	obtain an array of marker Strings for Annotations of a given type
	 * @param	annotationType	the type of Annotations to obtain a marker array for
	 * @param	markerMappings	a Properties object specifying a mapping of the default marker Strings to custom marker Strings
	 * @return an array of marker Strings for Annotations of the specified type
	 */
	public synchronized String[] getMarkerArray(String annotationType, Properties markerMappings) {
		
		//	create cache key
		Properties mapping = ((markerMappings == null) ? this.markerMappings : markerMappings);
		String cacheKey = "";
		if ((annotationType != null) && (annotationType.length() != 0)) {
			cacheKey += annotationType.toLowerCase();
			cacheKey += ("|" + mapping.getProperty(START, START));
			cacheKey += ("|" + mapping.getProperty(CONTINUE, CONTINUE));
			cacheKey += ("|" + mapping.getProperty(OTHER, OTHER));
		}
		
		//	do cache lookup
		if (this.markerArrayCache.containsKey(cacheKey))
			return ((String[]) this.markerArrayCache.get(cacheKey));
		
		//	get Annotations
		Annotation[] annotations = (((annotationType == null) || (annotationType.length() == 0)) ? new Annotation[0] : this.document.getAnnotations(annotationType));
		
		//	create marker array
		int annotationIndex = 0;
		Annotation currentAnnotation = ((annotationIndex < annotations.length) ? annotations[annotationIndex] : null);
		
		int markerIndex = 0;
		String[] markers = new String[this.document.size()];
		
		String start = mapping.getProperty(START, START);
		String cont = mapping.getProperty(CONTINUE, CONTINUE);
		String other = mapping.getProperty(OTHER, OTHER);
		
		while (markerIndex < this.document.size()) {
			
			//	after last Annotation
			if (currentAnnotation == null) {
				markers[markerIndex] = other;
				markerIndex++;
				
			//	some Annotation left to handle
			} else {
				
				//	fill to start of current Annotation
				while (markerIndex < currentAnnotation.getStartIndex()) {
					markers[markerIndex] = other;
					markerIndex++;
				}
				
				//	mark start of current Annotation
				if (markerIndex == currentAnnotation.getStartIndex()) {
					markers[markerIndex] = start;
					markerIndex++;
				}
				
				//	mark rest of current Annotation
				while (markerIndex < currentAnnotation.getEndIndex()) {
					markers[markerIndex] = cont;
					markerIndex++;
				}
				
				//	switch to next Annotation
				annotationIndex++;
				currentAnnotation = ((annotationIndex < annotations.length) ? annotations[annotationIndex] : null);
			}
		}
		
		//	store marker array in cache and return it
		this.markerArrayCache.put(cacheKey, markers);
		this.markerArrayCacheKeys.addElementIgnoreDuplicates(cacheKey);
		return markers;
	}
	
	/**	add Annotations given as an array of markers to the document wrapped
	 * @param	markers			the array of marker String representing the Annotations to be added
	 * @param	annotationType	the type to add the Annotations with
	 */
	public void annotate(String[] markers, String annotationType) {
		this.annotate(markers, annotationType, this.markerMappings);
	}
	
	/**	add Annotations given as an array of markers to the document wrapped
	 * @param	markers			the array of marker String representing the Annotations to be added
	 * @param	annotationType	the type to add the Annotations with
	 * @param	markerMappings	a Properties object specifying a mapping of the default marker Strings to custom marker Strings
	 */
	public synchronized void annotate(String[] markers, String annotationType, Properties markerMappings) {
		
		//	check parameters
		if ((markers != null) && (markers.length == this.document.size()) && (annotationType != null) && (annotationType.length() != 0)) {
			
			//	check marker String mapping
			Properties mapping = ((markerMappings == null) ? this.markerMappings : markerMappings);
			String start = mapping.getProperty(START, START);
			String cont = mapping.getProperty(CONTINUE, CONTINUE);
			
			//	write Annotations represented by markers
			int aStart = -1;
			int aSize = 0;
			boolean annotated = false;
			for (int m = 0; m < markers.length; m++) {
				
				//	start of Annotation
				if (start.equals(markers[m])) {
					
					//	write last Annotation if given
					if ((aSize != 0) && (aStart != -1)) {
						this.document.addAnnotation(annotationType, aStart, aSize);
						annotated = true;
						aSize = 0;
					}
					
					//	remember start index
					aStart = m;
					aSize ++;
					
				//	Annotation continues
				} else if (cont.equals(markers[m])) {
					aSize ++;
					
				//	not in an Annotation
				} else {
					
					//	write last Annotation if given
					if ((aSize != 0) && (aStart != -1)) {
						this.document.addAnnotation(annotationType, aStart, aSize);
						annotated = true;
						aStart = -1;
						aSize = 0;
					}
				}
			}
			
			//	add last Annotation if given
			if ((aSize != 0) && (aStart != -1)) {
				this.document.addAnnotation(annotationType, aStart, aSize);
				annotated = true;
				aSize = 0;
			}
			
			//	invalidate cache if new Annotation added
			if (annotated) this.invalidateMarkers(annotationType);
		}
	}
	
	/**	create a String array representation of the document wrapped
	 */
	private synchronized void createTokenArray() {
		this.tokens = new String[this.document.size()];
		for (int v = 0; v < this.document.size(); v++)
			this.tokens[v] = this.document.valueAt(v);
	}
	
	/**	invalidate token array cache
	 */
	private synchronized void invalidateTokens() {
		this.tokens = null;
		this.invalidateMarkers(null);
	}
	
	/**	invalidate marker array cache for a given Annotation type
	 */
	private synchronized void invalidateMarkers(String annotationType) {
		
		//	complete invalidation
		if ((annotationType == null) || (annotationType.length() == 0)) {
			this.markerArrayCacheKeys.clear();
			this.markerArrayCache.clear();
			System.gc();
			
		//	invalidate particular Annotation type
		} else {
			String type = annotationType.toLowerCase() + "|";
			int t = 0;
			boolean removed = false;
			while (t < this.markerArrayCacheKeys.size()) {
				String key = this.markerArrayCacheKeys.get(t);
				if (key.startsWith(type)) {
					this.markerArrayCacheKeys.remove(t);
					this.markerArrayCache.remove(key);
					removed = true;
				} else t++;
			}
			if (removed) System.gc();
		}
	}
	
	/** @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		
		//	clear all caches for easier garbage collection
		this.invalidateTokens();
		this.document.removeTokenSequenceListener(this);
		
		//	finalize super calss
		super.finalize();
	}
	
	/** @see de.uka.ipd.idaho.gamta.TokenSequenceListener#tokenSequenceChanged(de.uka.ipd.idaho.gamta.MutableTokenSequence.TokenSequenceEvent)
	 */
	public void tokenSequenceChanged(TokenSequenceEvent change) {
		if (change.inserted.size() == change.removed.size())
			for (int t = change.index; t < change.inserted.size(); t++)
				this.tokens[t] = change.inserted.valueAt(t);
		else this.invalidateTokens();
	}
}
