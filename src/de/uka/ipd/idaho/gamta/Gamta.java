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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.gamta.defaultImplementation.GamtaDocument;
import de.uka.ipd.idaho.gamta.defaultImplementation.GamtaTokenSequence;
import de.uka.ipd.idaho.gamta.defaultImplementation.RegExTokenizer;
import de.uka.ipd.idaho.gamta.defaultImplementation.StringBufferCharSequence;
import de.uka.ipd.idaho.gamta.defaultImplementation.TemporaryAnnotation;
import de.uka.ipd.idaho.gamta.defaultImplementation.TemporaryAnnotationSet;
import de.uka.ipd.idaho.gamta.defaultImplementation.TokenizedMutableCharSequence;
import de.uka.ipd.idaho.gamta.util.TestDocumentProvider;
import de.uka.ipd.idaho.gamta.util.analyzers.LineEndMarker;
import de.uka.ipd.idaho.gamta.util.analyzers.ParagraphStructureNormalizer;
import de.uka.ipd.idaho.gamta.util.analyzers.ParagraphTagger;
import de.uka.ipd.idaho.gamta.util.analyzers.SectionTagger;
import de.uka.ipd.idaho.gamta.util.analyzers.SentenceTagger;
import de.uka.ipd.idaho.gamta.util.analyzers.WhitespaceNormalizer;
import de.uka.ipd.idaho.stringUtils.Dictionary;
import de.uka.ipd.idaho.stringUtils.StringIterator;
import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.regExUtils.RegExUtils;


/**
 * The GAMTA function library includes methods for specifying the types of
 * tokens, for creating annotations using regular expressions and dictionary
 * lists, for doing Levenshtein style comparison and transformation of token
 * sequences, factory methods for the basic markup elements, and a central
 * registry for components who are providing test documents, which is useful in
 * GUI applications editing dictionary lists and regular expressions.
 * 
 * @author sautter
 */
public class Gamta extends StringUtils {
	
	/**
	 * Interface for alterante implementations of the GAMTA data model's data
	 * object interfaces.
	 * 
	 * @author sautter
	 */
	public static interface GamtaDataFactory {
		
		/**
		 * Obtain a new MutableCharSequence object.
		 * @return a new mutable char sequence object, using this factories
		 *         implementation
		 */
		public abstract MutableCharSequence newCharSequence();
		
		/**
		 * Obtain a new MutableTokenSequence from a char sequence object.
		 * @param charData the mutbale char sequence to wrap in the token
		 *            sequence (specifying null results in an empty token
		 *            sequence)
		 * @param tokenizer the Tokenizer to use (specifying null will result in
		 *            the default tokenizer being used)
		 * @return a new mutable token sequence object, using this factories
		 *         implementation
		 */
		public abstract MutableTokenSequence newTokenSequence(CharSequence charData, Tokenizer tokenizer);
		
		/**
		 * Wrap a new MutableTokenSequence overlay object around a
		 * MutableCharSequence object.
		 * @param charData the MutableCharSequence to wrap in the token sequence
		 *            (specifying null results in an empty token sequence)
		 * @param tokenizer the Tokenizer to use (specifying null will result in
		 *            the default tokenizer being used)
		 * @return a new mutable token sequence object, using this factories
		 *         implementation
		 */
		public abstract MutableTokenSequence newTokenSequence(MutableCharSequence charData, Tokenizer tokenizer);
		
		/**
		 * Produce a new MutableTokenSequence object from a token sequence.
		 * @param data the token sequence to copy (specifying null results in an
		 *            empty token sequence)
		 * @return a new mutable token sequence copied from the specified token
		 *         sequence, using this factories implementation
		 */
		public abstract MutableTokenSequence copyTokenSequence(TokenSequence data);
		
		/**
		 * Obtain a new, empty DocumentRoot.
		 * @param tokenizer the Tokenizer to use
		 * @return a new document instance using the specified tokenizer, using
		 *         this factories implementation
		 */
		public abstract DocumentRoot newDocument(Tokenizer tokenizer);
		
		/**
		 * Produce a new DocumentRoot from a MutableTokenSequence.
		 * @param tokens the tokens to wrap (if null, a new MutableTokenSequence
		 *            is created and used)
		 * @return a new document instance wrapped around the specified token
		 *         sequence, using this factories implementation
		 */
		public abstract DocumentRoot newDocument(MutableTokenSequence tokens);
		
		/**
		 * Produce a new document from a queriable annotations.
		 * @param data the QueriableAnnotation to copy
		 * @return a new document instance copied from the specified queriable
		 *         annotation, using this factories implementation
		 */
		public abstract DocumentRoot copyDocument(QueriableAnnotation data);
		
		/**
		 * Obtain a new StandaloneAnnotation to a TokenSequence.
		 * @param tokens the token sequence ot annotate
		 * @param type the type for the new annotation
		 * @param startIndex the start index of the new annotation
		 * @param size the size of the new annotation
		 * @return a new standalone annotation of the specified type, marking
		 *         size tokens of the specified token sequence, starting from
		 *         the specified start index, using this factories
		 *         implementation
		 */
		public abstract StandaloneAnnotation newAnnotation(TokenSequence tokens, String type, int startIndex, int size);
		
		/**
		 * Obtain a new StandaloneAnnotation to a TokenSequence.
		 * @param tokens the token sequence ot annotate
		 * @param model the annotation to copy data from
		 * @return a new standalone annotation of the specified type, marking
		 *         size tokens of the specified token sequence, starting from
		 *         the specified start index, using this factories
		 *         implementation
		 */
		public abstract StandaloneAnnotation newAnnotation(TokenSequence tokens, Annotation model);
		
		/**
		 * Produce an empty AnnotationSet.
		 * @param tokens the token sequence the annotation set refers to
		 * @return a new empty annotation set referring to the specified token
		 *         sequence, using this factories implementation
		 */
		public abstract AnnotationSet newAnnotationSet(TokenSequence tokens);
		
		/**
		 * Produce an new AnnotationSet, containing one Annotations.
		 * @param tokens the token sequence the annotation set refers to
		 * @param type the type of the annotation set
		 * @param startIndex the start index for the first annotation in the set
		 * @param size the size of the first annotation in the set
		 * @return a new annotation set referring to the specified token
		 *         sequence, with an annotation of the specified type in it,
		 *         using this factories implementation
		 */
		public abstract AnnotationSet newAnnotationSet(TokenSequence tokens, String type, int startIndex, int size);
		
		/**
		 * Produce an new AnnotationSet, containing a copy of a model
		 * Annotation.
		 * @param tokens the token sequence the annotation set refers to
		 * @param model the annotation to copy data from
		 * @return a new annotation set referring to the specified token
		 *         sequence, with the specified annotation in it, using this
		 *         factories implementation
		 */
		public abstract AnnotationSet newAnnotationSet(TokenSequence tokens, Annotation model);
		
		/**
		 * Produce an new AnnotationSet, containing a copy of a model
		 * StandaloneAnnotation.
		 * @param model the annotation to copy data from, and to the tokens of
		 *            which to refer
		 * @return a new annotation set referring to the specified token
		 *         sequence, with the specified annotation in it, using this
		 *         factories implementation
		 */
		public abstract AnnotationSet newAnnotationSet(StandaloneAnnotation model);
	}
	private static GamtaDataFactory dataFactory = null;
	
	private static final SentenceTagger SENTENCE_TAGGER = new SentenceTagger();
	private static final ParagraphTagger PARAGRAPH_TAGGER = new ParagraphTagger();
	private static final SectionTagger SECTION_TAGGER = new SectionTagger();
	private static final WhitespaceNormalizer WHITESPACE_NORMALIZER = new WhitespaceNormalizer();
	private static final ParagraphStructureNormalizer PARAGRAPH_STRUCTURE_NORMALIZER = new ParagraphStructureNormalizer();
	private static final LineEndMarker LINE_END_MARKER = new LineEndMarker();
	
	private static final StringVector noiseWords = getNoiseWords();
	
	private static HashMap testDocProviders = new LinkedHashMap();
	private static TestDocumentProvider testDocProvider = null;
	
	/**	add a provider of test documents to this central blackboard
	 * @param	provider	the provider to add
	 */
	public static void addTestDocumentProvider(TestDocumentProvider provider) {
		if (provider != null) {
			testDocProvider = provider;
			testDocProviders.put(provider.getClass().getName(), provider);
		}
	}
	
	/**	remove a provider of test documents from this central blackboard
	 * @param	provider	the provider to remove
	 */
	public static void removeTestDocumentProvider(TestDocumentProvider provider) {
		if (provider != null) {
			testDocProviders.remove(provider.getClass().getName());
			if (testDocProvider == provider) {
				if (testDocProviders.size() == 0) testDocProvider = null;
				else {
					ArrayList list = new ArrayList(testDocProviders.values());
					testDocProvider = ((TestDocumentProvider) list.get(list.size() - 1));
				}
			}
		}
	}
	
	/**
	 * @return a test document from the last provider that was registered with
	 *         Gamta
	 */
	public static QueriableAnnotation getTestDocument() {
		return getTestDocument(null);
	}
	
	/**
	 * @return a test document from the provider with the specified name
	 */
	public static QueriableAnnotation getTestDocument(String providerName) {
		if (testDocProviders.isEmpty()) return null;
		else if (providerName == null) return testDocProvider.getTestDocument();
		else if (testDocProviders.containsKey(providerName)) return ((TestDocumentProvider) testDocProviders.get(providerName)).getTestDocument();
		else return testDocProvider.getTestDocument();
	}
	
	//	factory methods for easier change of default implementations
	
	/**
	 * @return the GMATA data factory currently installed
	 */
	public static GamtaDataFactory getDataFactory() {
		return dataFactory;
	}
	
	/**
	 * Set the factory for the implementations of the GAMTA data model's data
	 * object interfaces. Specifying null sets the factory back to the default.
	 * @param dataFactory the new factory for the implementations of the GAMTA
	 *            data model's data object interfaces
	 */
	public static void setDataFactory(GamtaDataFactory dataFactory) {
		Gamta.dataFactory = dataFactory;
	}
	
	/**
	 * Obtain a new MutableCharSequence object.
	 * @return a new mutable char sequence object, using the
	 *         implementation currently installed
	 */
	public static MutableCharSequence newCharSequence() {
		if (dataFactory == null)
			return new StringBufferCharSequence();
		else return dataFactory.newCharSequence();
	}
	
	/**
	 * Obtain a new MutableTokenSequence from a char sequence object.
	 * @param charData the char sequence to wrap in the token sequence
	 *            (specifying null results in an empty token sequence)
	 * @param tokenizer the Tokenizer to use (specifying null will result in the
	 *            default tokenizer being used)
	 * @return a new mutable token sequence object, using the
	 *         implementation currently installed
	 */
	public static MutableTokenSequence newTokenSequence(CharSequence charData, Tokenizer tokenizer) {
		if (dataFactory == null) {
			if (charData == null)
				return new TokenizedMutableCharSequence((tokenizer == null) ? INNER_PUNCTUATION_TOKENIZER : tokenizer);
			else return new TokenizedMutableCharSequence(((tokenizer == null) ? INNER_PUNCTUATION_TOKENIZER : tokenizer), charData);
		}
		else return dataFactory.newTokenSequence(charData, tokenizer);
	}
	
	/**
	 * Wrap a new MutableTokenSequence overlay object around a
	 * MutableCharSequence object.
	 * @param charData the MutableCharSequence to wrap in the token sequence
	 *            (specifying null results in an empty token sequence)
	 * @param tokenizer the Tokenizer to use (specifying null will result in the
	 *            default tokenizer being used)
	 * @return a new mutable token sequence object, using the implementation
	 *         currently installed
	 */
	public static MutableTokenSequence newTokenSequence(MutableCharSequence charData, Tokenizer tokenizer) {
		if (dataFactory == null) {
			if (charData == null)
				return new TokenizedMutableCharSequence((tokenizer == null) ? INNER_PUNCTUATION_TOKENIZER : tokenizer); 
			else return new GamtaTokenSequence(charData, ((tokenizer == null) ? INNER_PUNCTUATION_TOKENIZER : tokenizer));
		}
		else return dataFactory.newTokenSequence(charData, tokenizer);
	}
	
	/**
	 * Produce a new MutableTokenSequence object from a token sequence.
	 * @param data the token sequence to copy (specifying null results in an
	 *            empty token sequence)
	 * @return a new mutable token sequence copied from the specified token
	 *         sequence, using the implementation currently installed
	 */
	public static MutableTokenSequence copyTokenSequence(TokenSequence data) {
		if (dataFactory == null) {
			if (data == null)
				return new TokenizedMutableCharSequence(INNER_PUNCTUATION_TOKENIZER);
			else return new TokenizedMutableCharSequence(data);
		}
		else return dataFactory.copyTokenSequence(data);
	}
	
	/**
	 * Obtain a new, empty DocumentRoot.
	 * @param tokenizer the Tokenizer to use
	 * @return a new document instance using the specified tokenizer, using the
	 *         implementation currently installed
	 */
	public static DocumentRoot newDocument(Tokenizer tokenizer) {
		if (dataFactory == null) {
			DocumentRoot doc = new GamtaDocument(newTokenSequence(null, tokenizer));
			doc.setAnnotationNestingOrder(annotationNestingOrder);
			return doc;
		}
		else return dataFactory.newDocument(tokenizer);
	}
	
	/**
	 * Produce a new DocumentRoot from a MutableTokenSequence.
	 * @param tokens the tokens to wrap (if null, a new MutableTokenSequence is
	 *            created and used)
	 * @return a new document instance wrapped around the specified token
	 *         sequence, using the implementation currently installed
	 */
	public static DocumentRoot newDocument(MutableTokenSequence tokens) {
		if (dataFactory == null) {
			DocumentRoot doc = new GamtaDocument((tokens == null) ? newTokenSequence(null, null) : tokens);
			doc.setAnnotationNestingOrder(annotationNestingOrder);
			return doc;
		}
		else return dataFactory.newDocument(tokens);
	}
	
	/**
	 * Produce a new document from a queriable annotations.
	 * @param data the QueriableAnnotation to copy
	 * @return a new document instance copied from the specified queriable
	 *         annotation, using the implementation currently installed
	 */
	public static DocumentRoot copyDocument(QueriableAnnotation data) {
		if (dataFactory == null) 
			return new GamtaDocument(data);
		else return dataFactory.copyDocument(data);
	}
	
	/**
	 * Obtain a new StandaloneAnnotation to a TokenSequence.
	 * @param tokens the token sequence ot annotate
	 * @param type the type for the new annotation
	 * @param startIndex the start index of the new annotation
	 * @param size the size of the new annotation
	 * @return a new standalone annotation of the specified type, marking size
	 *         tokens of the specified token sequence, starting from the
	 *         specified start index, using the implementation currently
	 *         installed
	 */
	public static StandaloneAnnotation newAnnotation(TokenSequence tokens, String type, int startIndex, int size) {
		if (startIndex < 0)
			throw new RuntimeException("Annotation start index out of bounds: " + startIndex);
		else if (size < 1)
			throw new RuntimeException("Annotation size out of bounds: " + size);
		else if (tokens.size() < (startIndex + size))
			throw new RuntimeException("Annotation end index out of bounds: " + (startIndex + size));
		
		if (dataFactory == null) 
			return new TemporaryAnnotation(tokens, type, startIndex, size);
		else return dataFactory.newAnnotation(tokens, type, startIndex, size);
	}
	
	/**
	 * Obtain a new StandaloneAnnotation to a TokenSequence.
	 * @param tokens the token sequence ot annotate
	 * @param model the annotation to copy data from
	 * @return a new standalone annotation of the specified type, marking size
	 *         tokens of the specified token sequence, starting from the
	 *         specified start index, using the implementation currently
	 *         installed
	 */
	public static StandaloneAnnotation newAnnotation(TokenSequence tokens, Annotation model) {
		if (model.getStartIndex() < 0)
			throw new RuntimeException("Annotation start index out of bounds: " + model.getStartIndex());
		else if (model.size() < 1)
			throw new RuntimeException("Annotation size out of bounds: " + model.size());
		else if (tokens.size() < model.getEndIndex())
			throw new RuntimeException("Annotation end index out of bounds: " + model.getEndIndex());
		
		if (dataFactory == null) {
			StandaloneAnnotation sa = newAnnotation(tokens, model.getType(), model.getStartIndex(), model.size());
			sa.copyAttributes(model);
			return sa;
		}
		else return dataFactory.newAnnotation(tokens, model);
	}
	
	/**
	 * Produce an empty AnnotationSet.
	 * @param tokens the token sequence the annotation set refers to
	 * @return a new empty annotation set referring to the specified token
	 *         sequence, using the implementation currently installed
	 */
	public static AnnotationSet newAnnotationSet(TokenSequence tokens) {
		if (dataFactory == null) 
			return new TemporaryAnnotationSet(tokens);
		else return dataFactory.newAnnotationSet(tokens);
	}
	
	/**
	 * Produce an new AnnotationSet, containing one Annotations.
	 * @param tokens the token sequence the annotation set refers to
	 * @param type the type of the annotation set
	 * @param startIndex the start index for the first annotation in the set
	 * @param size the size of the first annotation in the set
	 * @return a new annotation set referring to the specified token sequence,
	 *         with an annotation of the specified type in it, using the
	 *         implementation currently installed
	 */
	public static AnnotationSet newAnnotationSet(TokenSequence tokens, String type, int startIndex, int size) {
		if (dataFactory == null) 
			return newAnnotationSet(newAnnotation(tokens, type, startIndex, size));
		else return dataFactory.newAnnotationSet(tokens, type, startIndex, size);
	}
	
	/**
	 * Produce an new AnnotationSet, containing a copy of a model Annotation.
	 * @param tokens the token sequence the annotation set refers to
	 * @param model the annotation to copy data from
	 * @return a new annotation set referring to the specified token sequence,
	 *         with the specified annotation in it, using the implementation
	 *         currently installed
	 */
	public static AnnotationSet newAnnotationSet(TokenSequence tokens, Annotation model) {
		if (dataFactory == null) 
			return new TemporaryAnnotationSet(tokens, model);
		else return dataFactory.newAnnotationSet(tokens, model);
	}
	
	/**
	 * Produce an new AnnotationSet, containing a copy of a model StandaloneAnnotation.
	 * @param model the annotation to copy data from, and to the tokens of which
	 *            to refer
	 * @return a new annotation set referring to the specified token sequence,
	 *         with the specified annotation in it, using the implementation
	 *         currently installed
	 */ 
	public static AnnotationSet newAnnotationSet(StandaloneAnnotation model) {
		if (dataFactory == null) 
			return new TemporaryAnnotationSet(model);
		else return dataFactory.newAnnotationSet(model);
	}
	
	//	factory methods for easier change of default implementations
	
	/**
	 * @return the type-based Annotation nesting order currently valid for all
	 *         documents produced by one of the factory methods, as a
	 *         space-separated string concatenation of annotation types, outmost
	 *         type first
	 */
	public static String getAnnotationNestingOrder() {
		return annotationNestingOrder;
	}
	
	/**
	 * Set the global type-based Annotation nesting order for all documents
	 * produced by one of the factory methods from now on, replacing the current
	 * one.
	 * @param nestingOrder the new global type-based Annotation nesting order
	 *            for all documents produced by one of the factory methods from
	 *            now on, as a space-separated string concatenation of
	 *            annotation types, outmost type first
	 * @return the old annotation nesting order
	 */
	public static String setAnnotationNestingOrder(String nestingOrder) {
		String old = annotationNestingOrder;
		annotationNestingOrder = ((nestingOrder == null) ? DocumentRoot.DEFAULT_ANNOTATION_NESTING_ORDER : nestingOrder);
		return old;
	}
	
	private static String annotationNestingOrder = DocumentRoot.DEFAULT_ANNOTATION_NESTING_ORDER;
	
	/**
	 * Add sentence tags to a mutable annotation (mark parts from Token just after
	 * senetnce ending Token up to next sentence ending Token).
	 * @param data the mutable annotation to tag the paragraphs in
	 */
	public static void tagSentences(MutableAnnotation data) {
		SENTENCE_TAGGER.process(data, new Properties());
	}
	
	/**
	 * Add paragraph tags to a mutable annotation (mark parts from Token just after
	 * paragraph end Token up to next paragraph end Token).
	 * @param data the mutable annotation to tag the paragraphs in
	 */
	public static void tagParagraphs(MutableAnnotation data) {
		PARAGRAPH_TAGGER.process(data, new Properties());
	}
	
	/**
	 * Add section tags to a mutable annotation (mark parts from section starting
	 * paragraph Token right before next section starting paragraph).
	 * @param data the mutable annotation to tag the sections in
	 */
	public static void tagSections(MutableAnnotation data) {
		SECTION_TAGGER.process(data, new Properties());
	}
	
	/**
	 * Normalize the whitespace in a sequence of Tokens to the whitespace rules
	 * of GAMTA.
	 * @param data the mutable annotation to normalize the whitespace in
	 */
	public static void normalizeWhitespace(MutableAnnotation data) {
		WHITESPACE_NORMALIZER.process(data, new Properties());
	}
	
	/**
	 * Normalize the inner structure of a paragraph (remove inner linebreaks,
	 * re-join hyphenated words).
	 * @param data the paragraph or mutable annotation containing the paragraphs to
	 *            normalize
	 */
	public static void normalizeParagraphStructure(MutableAnnotation data) {
		PARAGRAPH_STRUCTURE_NORMALIZER.process(data, new Properties());
	}
	
	/**
	 * Mark line ends (mark each token succeeded by a line breaking whitespace
	 * as a paragraph end).
	 * @param data the paragraph or mutable annotation containing the paragraphs
	 *            to mark the line ends in
	 */
	public static void markLineEnds(MutableAnnotation data) {
		LINE_END_MARKER.process(data, new Properties());
	}
	
	/**
	 * Check if a String is a closing bracket closing another String that is an
	 * opening bracket.
	 * @param string1 the to-be tested closing bracket String
	 * @param string2 the opening bracket String to test string1 against
	 * @return true if and only if the first String is a closing bracket, the
	 *         second String is an opening bracket, and both brackets match
	 */
	public static boolean closes(CharSequence string1, CharSequence string2) {
		return StringUtils.closes(string1, string2);
	}
	
	/**
	 * Check if a String is an opening bracket opening another String that is a
	 * closing bracket.
	 * @param string1 the to-be tested opening bracket String
	 * @param string2 the closing bracket String to test string1 against
	 * @return true if and only if the first String is an opening bracket, the
	 *         second String is a closing bracket, and both brackets match
	 */
	public static boolean opens(CharSequence string1, CharSequence string2) {
		return StringUtils.opens(string1, string2);
	}
	
	/**
	 * Check if a String has a space between the previous String and itself.
	 * @param string the String to be tested
	 * @return true if and only if the specified Stringe has a space between the
	 *         previous String and itself
	 */
	public static boolean spaceBefore(CharSequence string) {
		return StringUtils.spaceBefore(string);
	}
	
	/**
	 * Check if a String has a space between itself and the subsequent String.
	 * @param string the String to be tested
	 * @return true if and only if the specified String has a space between
	 *         itself and the subsequent Sting
	 */
	public static boolean spaceAfter(CharSequence string) {
		return StringUtils.spaceAfter(string);
	}
	
	/**
	 * Check if a space is to be inserted between two Strings in a text.
	 * @param string1 the first String
	 * @param string2 the second String
	 * @return true is and only if a space is to be inserted between the two
	 *         specified Strings on concatenation
	 */
	public static boolean insertSpace(CharSequence string1, CharSequence string2) {
		return StringUtils.insertSpace(string1, string2);
	}
	
	/**
	 * Check if a String is a punctuation mark ending a sentence.
	 * @param string the String to be tested
	 * @return true if and only if the String is a punctuation mark ending a
	 *         sentence
	 */
	public static boolean isSentenceEnd(CharSequence string) {
		return StringUtils.isSentenceEnd(string);
	}

	/**
	 * Check if a String is a punctuation mark used within sentences.
	 * @param string the String to be tested
	 * @return true if and only if the String is a punctuation mark used for
	 *         sentence punctuation
	 */
	public static boolean isSentencePunctuation(CharSequence string) {
		return StringUtils.isSentencePunctuation(string);
	}

	/**
	 * Check if a String is a closing bracket.
	 * @param string the String to be tested
	 * @return true if and only if the String is a closing bracket
	 */
	public static boolean isClosingBracket(CharSequence string) {
		return StringUtils.isClosingBracket(string);
	}

	/**
	 * Check if a String is an opening bracket.
	 * @param string the String to be tested
	 * @return true if and only if the String is an opening bracket
	 */
	public static boolean isOpeningBracket(CharSequence string) {
		return StringUtils.isOpeningBracket(string);
	}

	/**
	 * Check if a String is a bracket.
	 * @param string the String to be tested
	 * @return true if and only if the String is a bracket
	 */
	public static boolean isBracket(CharSequence string) {
		return StringUtils.isBracket(string);
	}

	/**
	 * Check if a String is a punctuation mark.
	 * @param string the String to be tested
	 * @return true if and only if the String is a punctuation mark
	 */
	public static boolean isPunctuation(CharSequence string) {
		return StringUtils.isPunctuation(string);
	}

	/**
	 * Check if a String is a number.
	 * @param string the String to be tested
	 * @return true if and only if the String is a number (consists of digits
	 *         and in-number punctuation marks only)
	 */
	public static boolean isNumber(CharSequence string) {
		return StringUtils.isNumber(string);
	}

	/**
	 * Check if a String is an upper case word.
	 * @param string the String to be tested
	 * @return true if and only if the String is a word and contains capital
	 *         letters only
	 */
	public static boolean isUpperCaseWord(CharSequence string) {
		return StringUtils.isUpperCaseWord(string);
	}

	/**
	 * Check if a String is a capitalized word.
	 * @param string the String to be tested
	 * @return true if and only if the String is a word and starts with a
	 *         capital letter, followed by lower case letters only
	 */
	public static boolean isCapitalizedWord(CharSequence string) {
		return StringUtils.isCapitalizedWord(string);
	}

	/**
	 * Check if a String is a first letter up word.
	 * @param string the String to be tested
	 * @return true if and only if the String is a word and starts with a
	 *         capital letter
	 */
	public static boolean isFirstLetterUpWord(CharSequence string) {
		return StringUtils.isFirstLetterUpWord(string);
	}

	/**
	 * Check if a String is a lower case word.
	 * @param string the String to be tested
	 * @return true if and only if the String is a word and contains lower case
	 *         letters only
	 */
	public static boolean isLowerCaseWord(CharSequence string) {
		return StringUtils.isLowerCaseWord(string);
	}

	/**
	 * Check if a String is a word.
	 * @param string the String to be tested
	 * @return true if and only if the String is a word (consists of letters and
	 *         in-word punctuation marks only)
	 */
	public static boolean isWord(CharSequence string) {
		return StringUtils.isWord(string);
	}
	
	/**
	 * Check if a TokenSequence contains at least one non-noise word.
	 * @param tokens the TokenSequence to check
	 * @return true if and only if the specified TokenSequence contains at least
	 *         one non-noise word
	 */
	public static boolean hasWord(TokenSequence tokens) {
		for (int t = 0; t < tokens.size(); t++) {
			String token = tokens.valueAt(t);
			if (isWord(token) && !noiseWords.containsIgnoreCase(token))
				return true;
		}
		return false;
	}
	
	/**
	 * Check if a TokenSequence is in title case (at least all non-noise words
	 * capitalized).
	 * @param tokens the TokenSequence to check
	 * @return true if and only if the specified TokenSequence is in title case
	 */
	public static boolean isTitleCase(TokenSequence tokens) {
		boolean hasWord = false;
		for (int t = 0; t < tokens.size(); t++) {
			String token = tokens.valueAt(t);
			if (isWord(token)) {
				if (!isCapitalizedWord(token) && !noiseWords.containsIgnoreCase(token))
					return false;
				hasWord = true;
			}
		}
		return hasWord;
	}
	
	/**
	 * Check if a TokenSequence is in all upper case (all words in upper case).
	 * @param tokens the TokenSequence to check
	 * @return true if and only if the specified TokenSequence is in all upper
	 *         case
	 */
	public static boolean isAllUpperCase(TokenSequence tokens) {
		for (int t = 0; t < tokens.size(); t++) {
			String token = tokens.valueAt(t);
			if (isWord(token) && !noiseWords.containsIgnoreCase(token) && !isCapitalizedWord(token))
				return false;
		}
		return true;
	}
	
	/**
	 * Compute the Levenshtein distance for transforming one TokenSequences into
	 * another.
	 * @param start the TokenSequences to start from
	 * @param goal the TokenSequences to transform start into
	 * @return the Levenshtein cost for transforming start into goal
	 */
	public static int getLevenshteinDistance(TokenSequence start, TokenSequence goal) {
		return getLevenshteinDistance(start, goal, 0, false, 1, 1);
	}
	
	/**
	 * Compute the Levenshtein distance for transforming one TokenSequences into
	 * another. Note: a threshold of 0 will compute the entire editing distance,
	 * regardless of its value.
	 * @param start the TokenSequences to start from
	 * @param goal the TokenSequences to transform start into
	 * @param threshold the maximum distance (computation will stop if specified
	 *            value reached)
	 * @return the Levenshtein cost for transforming start into goal, maximum
	 *         the specified threshold
	 */
	public static int getLevenshteinDistance(TokenSequence start, TokenSequence goal, int threshold) {
		return getLevenshteinDistance(start, goal, threshold, false, 1, 1);
	}
	
	/**
	 * Compute the Levenshtein distance for transforming one TokenSequences into
	 * another.
	 * @param start the TokenSequences to start from
	 * @param goal the TokenSequences to transform start into
	 * @param caseSensitive use case sensitive or case insensitive comparison
	 *            for Token values
	 * @return the Levenshtein cost for transforming start into goal
	 */
	public static int getLevenshteinDistance(TokenSequence start, TokenSequence goal, boolean caseSensitive) {
		return getLevenshteinDistance(start, goal, 0, caseSensitive, 1, 1);
	}
	
	/**
	 * Compute the Levenshtein distance for transforming one TokenSequences into
	 * another.Note: a threshold of 0 will compute the entire editing distance,
	 * regardless of its value.
	 * @param start the TokenSequences to start from
	 * @param goal the TokenSequences to transform start into
	 * @param threshold the maximum distance (computation will stop if specified
	 *            value reached)
	 * @param caseSensitive use case sensitive or case insensitive comparison
	 *            for Token values
	 * @return the Levenshtein cost for transforming start into goal, maximum
	 *         the specified threshold
	 */
	public static int getLevenshteinDistance(TokenSequence start, TokenSequence goal, int threshold, boolean caseSensitive) {
		return getLevenshteinDistance(start, goal, threshold, caseSensitive, 1, 1);
	}
	
	/**
	 * Compute the Levenshtein distance for transforming one TokenSequences into
	 * another. Note: a threshold of 0 will compute the entire editing distance,
	 * regardless of its value.
	 * @param start the TokenSequences to start from
	 * @param goal the TokenSequences to transform start into
	 * @param threshold the maximum distance (computation will stop if specified
	 *            value reached)
	 * @param caseSensitive use case sensitive or case insensitive comparison
	 *            for Token values
	 * @param insertCost the cost for inserting a Token
	 * @param deleteCost the cost for deleting a Token
	 * @return the Levenshtein cost for transforming start into goal, maximum
	 *         the specified threshold
	 * 
	 */
	public static int getLevenshteinDistance(TokenSequence start, TokenSequence goal, int threshold, boolean caseSensitive, int insertCost, int deleteCost) {
		
		int[][] distanceMatrix; // matrix
		int startSize; // length of s
		int goalSize; // length of t
		int minSize; // the limit for the quadratic computation
		int substitutionFactor = (insertCost + deleteCost);
		int cost; // cost
		int distance = 0;
	
		//	fill the matrix top-left to bottom-right instead of line-wise
		int limit = 1;
	
		// Step 1
		startSize = ((start == null) ? 0 : start.size());
		goalSize = ((goal == null) ? 0 : goal.size());
		minSize = ((startSize > goalSize) ? goalSize : startSize);
		
		//	Step 1.5
		if ((Math.abs(startSize - goalSize) > threshold) && (threshold > 0)) return threshold;
		
		// Step 2
		distanceMatrix = new int[startSize + 1][goalSize + 1];
		distanceMatrix[0][0] = 0;
		
		while (limit <= minSize) {
			distanceMatrix[limit][0] = (limit * insertCost);
			distanceMatrix[0][limit] = (limit * deleteCost);
			
			//	compute line
			for (int c = 1; c < limit; c++) {
				cost = getCost(start.tokenAt(c - 1), goal.tokenAt(limit - 1), substitutionFactor, caseSensitive);
				distance = min3(distanceMatrix[c - 1][limit] + deleteCost, distanceMatrix[c][limit - 1] + insertCost, distanceMatrix[c - 1][limit - 1] + cost);
				distanceMatrix[c][limit] = distance;
			}
			
			//	compute column
			for (int l = 1; l < limit; l++) {
				cost = getCost(start.tokenAt(limit - 1), goal.tokenAt(l - 1), substitutionFactor, caseSensitive);
				distance = min3(distanceMatrix[limit - 1][l] + deleteCost, distanceMatrix[limit][l - 1] + insertCost, distanceMatrix[limit - 1][l - 1] + cost);
				distanceMatrix[limit][l] = distance;
			}
			
			//	compute new corner
			cost = getCost(start.tokenAt(limit - 1), goal.tokenAt(limit - 1), substitutionFactor, caseSensitive);
			distance = min3(distanceMatrix[limit - 1][limit] + deleteCost, distanceMatrix[limit][limit - 1] + insertCost, distanceMatrix[limit - 1][limit - 1] + cost);
			if ((distance > threshold) && (threshold > 0)) return threshold;
			distanceMatrix[limit][limit] = distance;
			
			//	increment limit
			limit ++;
		}
		
		//	Step 2.5a (compute remaining columns)
		while (limit <= startSize) {
			distanceMatrix[limit][0] = (limit * insertCost);
			
			//	compute column
			for (int l = 1; l <= goalSize; l++) {
				cost = getCost(start.tokenAt(limit - 1), goal.tokenAt(l - 1), substitutionFactor, caseSensitive);
				distance = min3(distanceMatrix[limit - 1][l] + deleteCost, distanceMatrix[limit][l - 1] + insertCost, distanceMatrix[limit - 1][l - 1] + cost);
				distanceMatrix[limit][l] = distance;
			}
			if ((distance > threshold) && (threshold > 0)) return threshold;
			
			//	increment limit
			limit ++;
		}
		
		//	Step 2.5b (compute remaining rows)
		while (limit <= goalSize) {
			distanceMatrix[0][limit] = (limit * deleteCost);
			
			//	compute line
			for (int c = 1; c <= startSize; c++) {
				cost = getCost(start.tokenAt(c - 1), goal.tokenAt(limit - 1), substitutionFactor, caseSensitive);
				distance = min3(distanceMatrix[c - 1][limit] + deleteCost, distanceMatrix[c][limit - 1] + insertCost, distanceMatrix[c - 1][limit - 1] + cost);
				distanceMatrix[c][limit] = distance;
			}
			if ((distance > threshold) && (threshold > 0)) return threshold;
			
			//	increment limit
			limit ++;
		}
		
		// Step 7
		return distanceMatrix[startSize][goalSize];
	}
	
	/**
	 * Compute the Levenshtein sequence of insert/delete/substitute operations
	 * for transforming one TokenSequences into another
	 * @param start the TokenSequences to start from
	 * @param goal the TokenSequences to transform start into
	 * @return the Levenshtein cost for transforming start into goal
	 */
	public static int[] getLevenshteinEditSequence(TokenSequence start, TokenSequence goal) {
		return getLevenshteinEditSequence(start, goal, false, 1, 1);
	}
	
	/**
	 * Compute the Levenshtein sequence of insert/delete/substitute operations
	 * for transforming one TokenSequences into another
	 * @param start the TokenSequences to start from
	 * @param goal the TokenSequences to transform start into
	 * @param caseSensitive use case sensitive or case insensitive comparison
	 *            for Token values
	 * @return the Levenshtein cost for transforming start into goal
	 */
	public static int[] getLevenshteinEditSequence(TokenSequence start, TokenSequence goal, boolean caseSensitive) {
		return getLevenshteinEditSequence(start, goal, caseSensitive, 1, 1);
	}
	
	/**
	 * Compute the Levenshtein sequence of insert/delete/substitute operations
	 * for transforming one TokenSequences into another
	 * @param start the TokenSequences to start from
	 * @param goal the TokenSequences to transform start into
	 * @param caseSensitive use case sensitive or case insensitive comparison
	 *            for Token values
	 * @param insertCost the cost for inserting a Token
	 * @param deleteCost the cost for deleting a Token
	 * @return the Levenshtein cost for transforming start into goal
	 */
	public static int[] getLevenshteinEditSequence(TokenSequence start, TokenSequence goal, boolean caseSensitive, int insertCost, int deleteCost) {
		
		int[][] distanceMatrix; // matrix
		int startSize; // length of s
		int goalSize; // length of t
		int minSize; // the limit for the quadratic computation
		int substitutionFactor = (insertCost + deleteCost);
		int cost; // cost
		int distance = 0;
		
		//	fill the matrix top-left to bottom-right instead of line-wise
		int limit = 1;
	
		// Step 1
		startSize = ((start == null) ? 0 : start.size());
		goalSize = ((goal == null) ? 0 : goal.size());
		minSize = ((startSize > goalSize) ? goalSize : startSize);
		
		// Step 2
		distanceMatrix = new int[startSize + 1][goalSize + 1];
		distanceMatrix[0][0] = 0;
		
		while (limit <= minSize) {
			distanceMatrix[limit][0] = (limit * insertCost);
			distanceMatrix[0][limit] = (limit * deleteCost);
			
			//	compute line
			for (int c = 1; c < limit; c++) {
				cost = getCost(start.tokenAt(c - 1), goal.tokenAt(limit - 1), substitutionFactor, caseSensitive);
				distance = min3(distanceMatrix[c - 1][limit] + deleteCost, distanceMatrix[c][limit - 1] + insertCost, distanceMatrix[c - 1][limit - 1] + cost);
				distanceMatrix[c][limit] = distance;
			}
			
			//	compute column
			for (int l = 1; l < limit; l++) {
				cost = getCost(start.tokenAt(limit - 1), goal.tokenAt(l - 1), substitutionFactor, caseSensitive);
				distance = min3(distanceMatrix[limit - 1][l] + deleteCost, distanceMatrix[limit][l - 1] + insertCost, distanceMatrix[limit - 1][l - 1] + cost);
				distanceMatrix[limit][l] = distance;
			}
			
			//	compute new corner
			cost = getCost(start.tokenAt(limit - 1), goal.tokenAt(limit - 1), substitutionFactor, caseSensitive);
			distance = min3(distanceMatrix[limit - 1][limit] + deleteCost, distanceMatrix[limit][limit - 1] + insertCost, distanceMatrix[limit - 1][limit - 1] + cost);
			distanceMatrix[limit][limit] = distance;
			
			//	increment limit
			limit ++;
		}
		
		//	Step 2.5a (compute remaining columns)
		while (limit <= startSize) {
			distanceMatrix[limit][0] = (limit * insertCost);
			
			//	compute column
			for (int l = 1; l <= goalSize; l++) {
				cost = getCost(start.tokenAt(limit - 1), goal.tokenAt(l - 1), substitutionFactor, caseSensitive);
				distance = min3(distanceMatrix[limit - 1][l] + deleteCost, distanceMatrix[limit][l - 1] + insertCost, distanceMatrix[limit - 1][l - 1] + cost);
				distanceMatrix[limit][l] = distance;
			}
			
			//	increment limit
			limit ++;
		}
		
		//	Step 2.5b (compute remaining rows)
		while (limit <= goalSize) {
			distanceMatrix[0][limit] = (limit * deleteCost);
			
			//	compute line
			for (int c = 1; c <= startSize; c++) {
				cost = getCost(start.tokenAt(c - 1), goal.tokenAt(limit - 1), substitutionFactor, caseSensitive);
				distance = min3(distanceMatrix[c - 1][limit] + deleteCost, distanceMatrix[c][limit - 1] + insertCost, distanceMatrix[c - 1][limit - 1] + cost);
				distanceMatrix[c][limit] = distance;
			}
			
			//	increment limit
			limit ++;
		}
		
		//	compute editing order
		int startIndex = startSize;
		int goalIndex = goalSize;
		distance = distanceMatrix[startSize][goalSize];
		int maxDistance = distance;
		ArrayList steps = new ArrayList();
		
		while ((startIndex != 0) || (goalIndex != 0) || (distance != 0)) {
			
			//	read possible steps
			int subst = (((startIndex != 0) && (goalIndex != 0)) ? distanceMatrix[startIndex - 1][goalIndex - 1] : maxDistance);
			int del = ((startIndex != 0) ? distanceMatrix[startIndex - 1][goalIndex] : maxDistance);
			int ins = ((goalIndex != 0) ? distanceMatrix[startIndex][goalIndex - 1] : maxDistance);
			
			//	substitution
			if ((subst <= del) && (subst <= ins) && (startIndex != 0) && (goalIndex != 0)) {
				if (start.tokenAt(startIndex-1).equals(goal.tokenAt(goalIndex-1)) || (!caseSensitive && start.valueAt(startIndex-1).equalsIgnoreCase(goal.valueAt(goalIndex-1)))) {
					//System.out.println("Keep " + start.tokenAt(startIndex-1));
					steps.add(new Integer(LEVENSHTEIN_KEEP));
				} else {
					//System.out.println("Replace " + start.tokenAt(startIndex-1) + " by " + goal.tokenAt(goalIndex-1));
					steps.add(new Integer(LEVENSHTEIN_REPLACE));
				}
				startIndex--;
				goalIndex--;
				distance = subst;
			}
			
			//	insertion
			else if ((ins <= subst) && (ins <= del) && (goalIndex != 0)) {
				//System.out.println("Insert " + goal.tokenAt(goalIndex-1));
				steps.add(new Integer(LEVENSHTEIN_INSERT));
				goalIndex--;
				distance = ins;
			}
			
			//	deletion
			else if ((del <= subst) && (del <= ins) && (startIndex != 0)) {
				//System.out.println("Delete " + start.tokenAt(startIndex-1));
				steps.add(new Integer(LEVENSHTEIN_DELETE));
				startIndex--;
				distance = del;
			}
			
			//	theoretically impossible state, break in order to avoid endless loop
			else {
				System.out.println("Impossible ... Theoretically ... F**K !!!");
				startIndex = 0;
				goalIndex = 0;
				distance = 0;
			}
		}
		Collections.reverse(steps);
		int[] editSequence = new int[steps.size()];
		for (int i = 0; i < steps.size(); i++)
			editSequence[i] = ((Integer) steps.get(i)).intValue();
		
		// Step 7
		return editSequence;
	}
	
	/**
	 * Compute edit cost for two Tokens
	 * @param token1 the first Token
	 * @param token2 the second Token
	 * @param caseSensitive use case sensitive or case insensitive comparison
	 *            for the Token's values
	 * @return the edit cost for the two Tokens
	 */
	public static int getCost(Token token1, Token token2, int factor, boolean caseSensitive) {
		if (token1.equals(token2)) return 0;
		if (!caseSensitive && token1.getValue().equalsIgnoreCase(token2.getValue())) return 0;
		int length = (token1.length() + token2.length());
		int dist = getLevenshteinDistance(token1.getValue(), token2.getValue(), 0, caseSensitive);
		return ((factor * dist) / length);
	}
	
	
	/**
	 * Execute the Levenshtein sequence of insert/delete/substitute operations,
	 * transforming one TokenSequences into another
	 * @param start the TokenSequences to transform
	 * @param goal the TokenSequences to transform start into
	 */
	public static void doLevenshteinTransformation(MutableTokenSequence start, TokenSequence goal) {
		doLevenshteinTransformation(start, goal, false, 1, 1);
	}
	
	/**
	 * Execute the Levenshtein sequence of insert/delete/substitute operations,
	 * transforming one TokenSequences into another
	 * @param start the TokenSequences to transform
	 * @param goal the TokenSequences to transform start into
	 * @param insertCost the cost for inserting a Token
	 * @param deleteCost the cost for deleting a Token
	 */
	public static void doLevenshteinTransformation(MutableTokenSequence start, TokenSequence goal, int insertCost, int deleteCost) {
		doLevenshteinTransformation(start, goal, false, 1, 1);
	}
	
	/**
	 * Execute the Levenshtein sequence of insert/delete/substitute operations,
	 * transforming one TokenSequences into another
	 * @param start the TokenSequences to transform
	 * @param goal the TokenSequences to transform start into
	 * @param caseSensitive use case sensitive or case insensitive comparison
	 *            for Token values
	 */
	public static void doLevenshteinTransformation(MutableTokenSequence start, TokenSequence goal, boolean caseSensitive) {
		doLevenshteinTransformation(start, goal, caseSensitive, 1, 1);
	}
	
	/**
	 * Execute the Levenshtein sequence of insert/delete/substitute operations,
	 * transforming one TokenSequences into another
	 * @param start the TokenSequences to transform
	 * @param goal the TokenSequences to transform start into
	 * @param caseSensitive use case sensitive or case insensitive comparison
	 *            for Token values
	 * @param insertCost the cost for inserting a Token
	 * @param deleteCost the cost for deleting a Token
	 */
	public static void doLevenshteinTransformation(MutableTokenSequence start, TokenSequence goal, boolean caseSensitive, int insertCost, int deleteCost) {
		int[] steps = getLevenshteinEditSequence(start, goal, caseSensitive, insertCost, deleteCost);
		doLevenshteinTransformation(start, goal, steps);
	}
	
	/**
	 * Execute the Levenshtein sequence of insert/delete/substitute operations,
	 * transforming one TokenSequences into another
	 * @param start the TokenSequences to transform
	 * @param goal the TokenSequences to transform start into
	 * @param editSequence the transformation steps
	 */
	public static void doLevenshteinTransformation(MutableTokenSequence start, TokenSequence goal, int[] editSequence) {
		int startIndex = 0;
		int goalIndex = 0;
		for (int s = 0; s < editSequence.length; s++) {
			int step = editSequence[s];
			if (step == LEVENSHTEIN_KEEP) {
				startIndex++;
				goalIndex++;
			}
			else if (step == LEVENSHTEIN_REPLACE) {
				start.setValueAt(goal.valueAt(goalIndex), startIndex);
				startIndex++;
				goalIndex++;
			}
			else if (step == LEVENSHTEIN_INSERT) {
				start.insertTokensAt(goal.tokenAt(goalIndex), startIndex);
				startIndex++;
				goalIndex++;
			}
			else if (step == LEVENSHTEIN_DELETE)
				start.removeTokensAt(startIndex, 1);
		}
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx) {
		return extractAllMatches(tokens, regEx, 0, null, null, false, false);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param exclude a Dictionary of Strings that matches may not contain
	 * @param startExclude a Dictionary of Strings that matches may not start
	 *            with
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, Dictionary startExclude, Dictionary exclude) {
		return extractAllMatches(tokens, regEx, 0, startExclude, exclude, false, false);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param maxTokens the maximum number of tokens a matching part may contain
	 *            (0 means no limit, Attention: high computation effort)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens) {
		return extractAllMatches(tokens, regEx, maxTokens, null, null, false, false);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param maxTokens the maximum number of tokens a matching part may contain
	 *            (0 means no limit, Attention: high computation effort)
	 * @param exclude a Dictionary of Strings that matches may not contain
	 * @param startExclude a Dictionary of Strings that matches may not start
	 *            with
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, Dictionary startExclude, Dictionary exclude) {
		return extractAllMatches(tokens, regEx, maxTokens, startExclude, exclude, false, false);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, boolean allowOverlap) {
		return extractAllMatches(tokens, regEx, 0, null, null, allowOverlap, false);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param exclude a Dictionary of Strings that matches may not contain
	 * @param startExclude a Dictionary of Strings that matches may not start
	 *            with
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, Dictionary startExclude, Dictionary exclude, boolean allowOverlap) {
		return extractAllMatches(tokens, regEx, 0, startExclude, exclude, allowOverlap, false);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param maxTokens the maximum number of tokens a matching part may contain
	 *            (0 means no limit, Attention: high computation effort)
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, boolean allowOverlap) {
		return extractAllMatches(tokens, regEx, maxTokens, null, null, allowOverlap, false);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param maxTokens the maximum number of tokens a matching part may contain
	 *            (0 means no limit, Attention: high computation effort)
	 * @param exclude a Dictionary of Strings that matches may not contain
	 * @param startExclude a Dictionary of Strings that matches may not start
	 *            with
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, Dictionary startExclude, Dictionary exclude, boolean allowOverlap) {
		return extractAllMatches(tokens, regEx, maxTokens, startExclude, exclude, allowOverlap, false);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted (default is false)
	 * @param ignoreLinebreaks ignore the endings of lines (paragraphs) while
	 *            matching (default is false)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, boolean allowOverlap, boolean ignoreLinebreaks) {
		return extractAllMatches(tokens, regEx, 0, null, null, allowOverlap, ignoreLinebreaks);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted (default is false)
	 * @param ignoreLinebreaks ignore the endings of lines (paragraphs) while
	 *            matching (default is false)
	 * @param normalize normalize whitespaces? (default is true)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, boolean allowOverlap, boolean ignoreLinebreaks, boolean normalize) {
		return extractAllMatches(tokens, regEx, 0, null, null, allowOverlap, ignoreLinebreaks, normalize);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param exclude a Dictionary of Strings that matches may not contain
	 * @param startExclude a Dictionary of Strings that matches may not start
	 *            with
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted (default is false)
	 * @param ignoreLinebreaks ignore the endings of lines (paragraphs) while
	 *            matching (default is false)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, Dictionary startExclude, Dictionary exclude, boolean allowOverlap, boolean ignoreLinebreaks) {
		return extractAllMatches(tokens, regEx, 0, startExclude, exclude, allowOverlap, ignoreLinebreaks);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param exclude a Dictionary of Strings that matches may not contain
	 * @param startExclude a Dictionary of Strings that matches may not start
	 *            with
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted (default is false)
	 * @param ignoreLinebreaks ignore the endings of lines (paragraphs) while
	 *            matching (default is false)
	 * @param normalize normalize whitespaces? (default is true)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, Dictionary startExclude, Dictionary exclude, boolean allowOverlap, boolean ignoreLinebreaks, boolean normalize) {
		return extractAllMatches(tokens, regEx, 0, startExclude, exclude, allowOverlap, ignoreLinebreaks, normalize);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param maxTokens the maximum number of tokens a matching part may contain
	 *            (0 means no limit, Attention: high computation effort)
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted (default is false)
	 * @param ignoreLinebreaks ignore the endings of lines (paragraphs) while
	 *            matching (default is false)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, boolean allowOverlap, boolean ignoreLinebreaks) {
		return extractAllMatches(tokens, regEx, maxTokens, null, null, allowOverlap, ignoreLinebreaks);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param maxTokens the maximum number of tokens a matching part may contain
	 *            (0 means no limit, Attention: high computation effort)
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted (default is false)
	 * @param ignoreLinebreaks ignore the endings of lines (paragraphs) while
	 *            matching (default is false)
	 * @param normalize normalize whitespaces? (default is true)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, boolean allowOverlap, boolean ignoreLinebreaks, boolean normalize) {
		return extractAllMatches(tokens, regEx, maxTokens, null, null, allowOverlap, ignoreLinebreaks, normalize);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param maxTokens the maximum number of tokens a matching part may contain
	 *            (0 means no limit, Attention: high computation effort)
	 * @param exclude a Dictionary of Strings that matches may not contain
	 * @param startExclude a Dictionary of Strings that matches may not start
	 *            with
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted (default is false)
	 * @param ignoreLinebreaks ignore the endings of lines (paragraphs) while
	 *            matching (default is false)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, Dictionary startExclude, final Dictionary exclude, boolean allowOverlap, boolean ignoreLinebreaks) {
		return extractAllMatches(tokens, regEx, maxTokens, startExclude, exclude, allowOverlap, ignoreLinebreaks, true);
	}
	
	/**
	 * Extract all parts from a String that match a regular expression
	 * @param tokens the TokenSequence to be rooted through
	 * @param regEx the regular expression that's matches are to be extracted
	 * @param maxTokens the maximum number of tokens a matching part may contain
	 *            (0 means no limit, Attention: high computation effort)
	 * @param exclude a Dictionary of Strings that matches may not contain
	 * @param startExclude a Dictionary of Strings that matches may not start
	 *            with
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted (default is false)
	 * @param ignoreLinebreaks ignore the endings of lines (paragraphs) while
	 *            matching (default is false)
	 * @param normalize normalize whitespaces? (default is true)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that match the specified regular expression
	 */
	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, Dictionary startExclude, final Dictionary exclude, boolean allowOverlap, boolean ignoreLinebreaks, boolean normalize) {
		
		/*
		 * Inspect regex in order to find excludable punctuation marks (keep
		 * match token sequences short). This is only a heuristic, though,
		 * better would be inspecting the NFA produced by Pattern.compile() in
		 * order to check which characters can appear in matches of the regular
		 * expression.
		 * 
		 * Possible representations of punctuation marks in RegEx:
		 * - . (matches any, if given omit check)
		 * - \W (non-word character, matches any but _, if given omit check)
		 * - \p{Punct}, \p{Graph}, \p{Print}, \p{ASCII} (matches any, if given omit check)
		 * - \<mark> (matches punctuation mark <mark>)
		 * - ^ (negation of something else)
		 */
		Set excludePunct = new HashSet(); // hash set is enough, no case insensitivity required for punctuation marks
		boolean allDotsEscped = true;
		String adeTestRegEx = regEx;
		while (adeTestRegEx.indexOf("\\\\") != -1)
			adeTestRegEx = adeTestRegEx.replaceAll("\\\\\\\\", "");
//		System.out.println("ADE test pattern: " + adeTestRegEx);
//		System.out.println(" - dot split is " + adeTestRegEx.split("\\.").length);
//		System.out.println(" - escaped dot split: " + adeTestRegEx.split("\\\\\\.").length);
		allDotsEscped = (adeTestRegEx.split("\\.").length == adeTestRegEx.split("\\\\\\.").length);
		if (allDotsEscped && (regEx.indexOf("\\W") == -1) && (regEx.indexOf("\\p{Punct}") == -1) && (regEx.indexOf("\\p{Graph}") == -1) && (regEx.indexOf("\\p{Print}") == -1) && (regEx.indexOf("\\p{ASCII}") == -1) && (regEx.indexOf('^') == -1)) {
//		if ((regEx.indexOf(".") == -1) && (regEx.indexOf("\\W") == -1) && (regEx.indexOf("\\p{Punct}") == -1) && (regEx.indexOf("\\p{Graph}") == -1) && (regEx.indexOf("\\p{Print}") == -1) && (regEx.indexOf("\\p{ASCII}") == -1) && (regEx.indexOf('^') == -1)) {
//			String excludedPunct = "";
			for (int p = 0; p < PUNCTUATION.length(); p++) {
				String punct = PUNCTUATION.substring(p, (p+1));
				if (regEx.indexOf("\\" + punct) == -1) {
					excludePunct.add(punct);
//					excludedPunct += punct;
				}
			}
//			System.out.println("Gamta.extractAllMatches(): excluding punctuation marks " + excludedPunct);
		}
//		System.out.println(" - excluding punctuation " + excludePunct);
		
		//	normalize reg ex if necessary, and compile pattern
		if ((regEx.indexOf("\n") != -1) || (regEx.indexOf("\r") != -1) || (regEx.indexOf("\f") != -1))
			regEx = RegExUtils.normalizeRegEx(regEx);
		
		Pattern pattern = ((Pattern) patternCache.get(regEx));
		if (pattern == null) {
			pattern = Pattern.compile(regEx);
			patternCache.put(regEx, pattern);
		}
		
		//	build normalized copy of token sequence and extract pre matches
		ArrayList preMatches = new ArrayList();
		MatchTokenSequence normalizedTokenSequence = null;
		Token last = null;
		Token current;
		String currentValue;
		for (int t = 0; t < tokens.size(); t++) {
			current = tokens.tokenAt(t);
			currentValue = current.getValue();
			
			//	current token cannot belong to a match, stop adding tokens and do extraction
			if (excludePunct.contains(currentValue) || ((exclude != null) && exclude.lookup(currentValue))) {
				if (normalizedTokenSequence != null) {
					addPreMatches(normalizedTokenSequence, preMatches, tokens, pattern, maxTokens, allowOverlap);
					normalizedTokenSequence.tokens.clear();
					normalizedTokenSequence = null;
					last = null;
				}
			}
			
			//	token can start a match, or we are in the middle of a sequence
			else if ((normalizedTokenSequence != null) || (startExclude == null) || !startExclude.lookup(currentValue)) {
				
				//	start new sequence
				if (normalizedTokenSequence == null)
					normalizedTokenSequence = new MatchTokenSequence(t);
				
				//	add whitespace to current sequence
				else if ((last != null) && (normalize ? insertSpace(last, current) : (tokens.getWhitespaceAfter(t-1).length() != 0)))
					normalizedTokenSequence.appendWhitespace((!ignoreLinebreaks && last.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE)) ? "\n" : " ");
				
				//	append current token
				normalizedTokenSequence.appendToken(currentValue);
				
				//	remember Token
				last = current;
			}
		}
		
		//	process last sequence
		if (normalizedTokenSequence != null) {
			addPreMatches(normalizedTokenSequence, preMatches, tokens, pattern, maxTokens, allowOverlap);
			normalizedTokenSequence.tokens.clear();
			normalizedTokenSequence = null;
		}
		
		//	sort pre matches
		Collections.sort(preMatches);
		
		//	overlapping matches allowed, return all pre matches
		if (allowOverlap) return ((Annotation[]) preMatches.toArray(new Annotation[preMatches.size()]));
		
		//	sort out overlapping matches if not allowed
		else {
			ArrayList matches = new ArrayList();
			
			//	sort out overlapping pre matches
			int lastEndIndex = 0;
			for (int m = 0; m < preMatches.size(); m++) {
				Annotation preMatch = ((Annotation) preMatches.get(m));
				if (preMatch.getStartIndex() >= lastEndIndex) {
					matches.add(preMatch);
					lastEndIndex = preMatch.getEndIndex();
				}
			}
			
			//	return the rest
			return ((Annotation[]) matches.toArray(new Annotation[matches.size()]));
		}
	}
	
	private static Map patternCache = Collections.synchronizedMap(new HashMap());
	
	private static void addPreMatches(MatchTokenSequence normalizedTokenSequence, ArrayList preMatches, TokenSequence tokens, Pattern pattern, int maxTokens, boolean allowOverlap) {
		int matchingStartOffset;
		int matchStartOffset, matchEndOffset;
		MatchToken startMt, endMt;
		
		Matcher matcher = pattern.matcher(normalizedTokenSequence);
		matchingStartOffset = 0;
		
//		THIS HELPS INVESTIGATING PATTERNS THAT DRIVE THE MATCHER INTO ENDLESS LOOPS
//		final boolean[] done = {false};
//		final Thread ct = Thread.currentThread();
//		Thread wt = new Thread() {
//			public void run() {
//				do {
//					try {
//						sleep(100);
//					} catch (InterruptedException ie) {}
//					StackTraceElement[] ste = ct.getStackTrace();
//					for (int s = 0; s < ste.length; s++)
//						System.out.println(ste[s].toString());
//					System.out.println();
//				}
//				while (!done[0]);
//			}
//		};
//		wt.start();
		
		//	find matching char sequences
		while ((matchingStartOffset < normalizedTokenSequence.length()) && matcher.find(matchingStartOffset)) {
			matchStartOffset = matcher.start();
			startMt = normalizedTokenSequence.tokenAtOffset(matchStartOffset);
			if (startMt != null) { // this may happen if regex matches the empty string
				
				//	current match starts at token start
				if (startMt.startOffset == matchStartOffset) {
					matchEndOffset = matcher.end();
					endMt = normalizedTokenSequence.tokenAtOffset(matchEndOffset - 1);
					if (endMt != null) { // this may happen if regex matches the empty string
						
						//	current match ends at token end and is below token limit ==> preMatch
						if (((endMt.startOffset + endMt.value.length()) == matchEndOffset) && (startMt.index <= endMt.index) && ((maxTokens == 0) || ((endMt.index - startMt.index) < maxTokens)))
							preMatches.add(newAnnotation(tokens, null, (normalizedTokenSequence.startIndex + startMt.index), (endMt.index - startMt.index + 1)));
						
						//	prefixes of current match might also match (test if overlapping matches allowed, or main match does not end at token border, or main match is longer than token limit)
						if ((startMt.index < endMt.index) && (allowOverlap || ((maxTokens > 0) && ((endMt.index - startMt.index + 1) > maxTokens)) || ((endMt.startOffset + endMt.value.length()) != matchEndOffset))) {
							CharSequence preMatchSequence = matcher.group();
							while (startMt.index < endMt.index) {
								
								//	try one token less
								endMt = ((MatchToken) normalizedTokenSequence.tokens.get(endMt.index - 1));
								
								//	size of match below token limit
								if ((maxTokens == 0) || ((endMt.index - startMt.index) < maxTokens)) {
									preMatchSequence = preMatchSequence.subSequence(0, (endMt.startOffset - startMt.startOffset + endMt.value.length()));
									
									//	found match ending at token border
									if (pattern.matcher(preMatchSequence).matches()) {
										preMatches.add(newAnnotation(tokens, null, (normalizedTokenSequence.startIndex + startMt.index), (endMt.index - startMt.index + 1)));
										
										//	stop here if overlapping matches not allowed
										if (!allowOverlap)
											endMt = startMt;
									}
								}
							}
						}
					}
				}
				
				//	start next 
				matchingStartOffset = startMt.endOffset;
			}
			
			//	avoid endless loop
			else matchingStartOffset++;
		}
//		
//		done[0] = true;
	}
	
	private static class MatchToken {
		final int index;
		final int startOffset;
		int endOffset;
		final String value;
		final StringBuffer token;
		private MatchToken(int index, int startOffset, String value) {
			this.index = index;
			this.startOffset = startOffset;
			this.value = value;
			this.token = new StringBuffer(this.value);
			this.endOffset = (this.startOffset + value.length());
		}
		private void addWhitespace(String whitespace) {
			this.token.append(whitespace);
			this.endOffset += whitespace.length();
		}
		private int length() {
			return this.token.length();
		}
		private char charAt(int offset) {
			return this.token.charAt(offset);
		}
	}
	
	private static class MatchTokenSequence implements CharSequence {
		int startIndex;
		ArrayList tokens = new ArrayList();
		int length = 0;
		private MatchTokenSequence(int startIndex) {
			this.startIndex = startIndex;
		}
		
		private void appendToken(String value) {
			MatchToken mt = new MatchToken(this.tokens.size(), this.length, value);
			this.tokens.add(mt);
			this.length += mt.length();
		}
		
		private void appendWhitespace(String whitespace) {
			if (this.tokens.size() != 0) {
				MatchToken mt = ((MatchToken) this.tokens.get(this.tokens.size() - 1));
				mt.addWhitespace(whitespace);
				this.length += whitespace.length();
			}
		}
		
		private int lastOffsetTokenIndex = -1;
		private MatchToken tokenAtOffset(int offset) {
			
			//	check for leading and tailing tokens
			if (offset < ((MatchToken) this.tokens.get(0)).startOffset) {
				this.lastOffsetTokenIndex = -1;
				return null;
			}
			if (offset >= ((MatchToken) this.tokens.get(this.tokens.size() - 1)).endOffset) {
				this.lastOffsetTokenIndex = -1;
				return null;
			}
			
			//	do cache lookup
			if (this.lastOffsetTokenIndex != -1) {
				MatchToken gt = ((MatchToken) this.tokens.get(this.lastOffsetTokenIndex));
				
				//	request for same token
				if ((gt.startOffset <= offset) && (offset < gt.endOffset))
					return ((MatchToken) this.tokens.get(this.lastOffsetTokenIndex));
				
				//	request for subsequent token
				else if (offset == gt.endOffset) {
					this.lastOffsetTokenIndex++;
					if (this.lastOffsetTokenIndex == this.tokens.size()) {
						this.lastOffsetTokenIndex = -1;
						return null;
					}
					else return ((MatchToken) this.tokens.get(this.lastOffsetTokenIndex));
				}
				
				//	request for previous token
				else if (gt.startOffset == (offset + 1)) {
					this.lastOffsetTokenIndex--;
					return ((MatchToken) this.tokens.get(this.lastOffsetTokenIndex));
				}
			}
			
			//	use binary search to narrow search interval
			int left = 0;
			int right = this.tokens.size();
			int tIndex = 0;
			MatchToken mt;
			while ((right - left) > 2) {
				tIndex = ((left + right) / 2);
				mt = ((MatchToken) this.tokens.get(tIndex));
				if (mt.endOffset <= offset)
					left = tIndex;
				else if (mt.startOffset <= offset) {
					this.lastOffsetTokenIndex = tIndex;
					return ((MatchToken) this.tokens.get(tIndex));
				}
				else right = tIndex;
			}
			
			//	scan remaining interval
			tIndex = left;
			while (tIndex < this.tokens.size()) {
				mt = ((MatchToken) this.tokens.get(tIndex));
				if (mt.endOffset <= offset)
					tIndex++;
				else if (mt.startOffset <= offset) {
					this.lastOffsetTokenIndex = tIndex;
					return ((MatchToken) this.tokens.get(tIndex));
				}
				else tIndex++;
			}
			
			this.lastOffsetTokenIndex = -1;
			return null;
		}
		public char charAt(int offset) {
			MatchToken mt = this.tokenAtOffset(offset);
			return mt.charAt(offset - mt.startOffset);
		}
		public int length() {
			return this.length;
		}
		public CharSequence subSequence(int start, int end) {
			StringBuffer subSequence = new StringBuffer();
			MatchToken mt = this.tokenAtOffset(start);
			subSequence.append(mt.token.subSequence((start - mt.startOffset), Math.min(mt.token.length(), (end - mt.startOffset))));
			while ((mt.endOffset < end) && ((mt.index + 1) < this.tokens.size())) {
				mt = ((MatchToken) this.tokens.get(mt.index + 1));
				subSequence.append(mt.token.subSequence(0, Math.min(mt.token.length(), (end - mt.startOffset))));
			}
			return subSequence;
		}
		public String toString() {
			// not used in this special purpose implementation
			return super.toString();
		}
	}
	
//	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, Dictionary startExclude, Dictionary exclude, boolean allowOverlap, boolean ignoreLinebreaks) {
//		ArrayList annotations = new ArrayList();
//		
//		int lastStartIndex = 0;
//		int lastMatchedIndex = 0;
//		int index = 0;
//		Annotation currentPart;
//		
//		//System.out.println("Extracting all subsequences of length " + maxTokens + " or less ...");
//		
//		//	normalize RegEx if necessary
//		if ((regEx.indexOf("\n") != -1) || (regEx.indexOf("\r") != -1) || (regEx.indexOf("\f") != -1))
//			regEx = RegExUtils.normalizeRegEx(regEx);
//		
//		Pattern pattern = Pattern.compile(regEx);
//		
//		Token lastToken = null;
//		while (index < tokens.size()) {
//			
//			//System.out.println("--> index is " + index);
//			
//			lastStartIndex = index;
//			Annotation match = null;
//			boolean validStart = ((maxTokens == 1) || (startExclude == null) || !startExclude.lookup(tokens.valueAt(index), true));
//			boolean stop = false;
//			
//			while (validStart && !stop && (index < tokens.size()) && (((index - lastStartIndex) < maxTokens) || (maxTokens == 0)) && (ignoreLinebreaks || (lastToken == null) || (lastStartIndex == index) || !lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))) {
//				
//				//	check if current part matches
//				if (((exclude != null) && exclude.lookup(tokens.valueAt(index), true)))
//					stop = true;
//				
//				else {
//					currentPart = Gamta.newAnnotation(tokens, null, lastStartIndex, (index - lastStartIndex));
//					String testPart = TokenSequenceUtils.concatTokens(currentPart, true, true);
//					
//					if (pattern.matcher(testPart).matches()) {
//						lastMatchedIndex = index;
//						
//						//	if ovelap is allowed, immediately create annotation
//						if (allowOverlap)
//							annotations.add(currentPart);
//						else match = currentPart; 
//					}
//					lastToken = tokens.tokenAt(index);
//					index++;
//				}
//			}
//			
//			//	store longest match if overlap is not allowed
//			if (match == null) index = lastStartIndex;
//			
//			else {
//				annotations.add(match);
//				index = lastMatchedIndex;
//			}
//			
//			//	tidy up
//			lastToken = tokens.tokenAt(index);
//			index ++;
//		}
//		
//		Collections.sort(annotations);
//		return ((Annotation[]) annotations.toArray(new Annotation[annotations.size()]));
//	}
//	public static Annotation[] extractAllMatches(TokenSequence tokens, String regEx, int maxTokens, Dictionary startExclude, Dictionary exclude, boolean allowOverlap, boolean ignoreLinebreaks) {
//		ArrayList annotations = new ArrayList();
//		
//		int lastStartIndex = 0;
//		int lastMatchedIndex = 0;
//		int index = 0;
//		MutableTokenSequence currentPart = newTokenSequence(null, tokens.getTokenizer());
//		boolean foundMatch = false;
//		
//		//System.out.println("Extracting all subsequences of length " + maxTokens + " or less ...");
//		
//		//	normalize RegEx if necessary
//		if ((regEx.indexOf("\n") != -1) || (regEx.indexOf("\r") != -1) || (regEx.indexOf("\f") != -1))
//			regEx = RegExUtils.normalizeRegEx(regEx);
//		
//		Pattern pattern = Pattern.compile(regEx);
//		
//		Token lastToken = null;
//		while (index < tokens.size()) {
//			
//			//System.out.println("--> index is " + index);
//			
//			lastStartIndex = index;
//			boolean stop = false;
//			boolean validStart = ((maxTokens == 1) || (startExclude == null) || !startExclude.lookup(tokens.valueAt(index), true));
//			
//			while (validStart && !stop && (index < tokens.size()) && ((currentPart.size() < maxTokens) || (maxTokens == 0)) && (ignoreLinebreaks || (lastToken == null) || (currentPart.size() == 0) || !lastToken.hasAttribute(Token.PARAGRAPH_END_ATTRIBUTE))) {
//				
//				//	check if actual part matches
//				if (((exclude != null) && exclude.lookup(tokens.valueAt(index), true))/* || ((startExclude != null) && startExclude.lookup(tokens.valueAt(index)) && (currentPart.size() == 0))*/) {
//					stop = true;
//				} else {
//					currentPart.addTokens(tokens.tokenAt(index));
//					String testPart = TokenSequenceUtils.concatTokens(currentPart, true, true);
//					
//					if (pattern.matcher(testPart).matches()) {
//						foundMatch = true;
//						lastMatchedIndex = index;
//						
//						//	if ovelap is allowed, immediately create annotation
//						if (allowOverlap) {
//							Annotation a = new TemporaryAnnotation(tokens, null, lastStartIndex, ((lastMatchedIndex - lastStartIndex) + 1));
//							annotations.add(a);
//						}
//					}
//					lastToken = tokens.tokenAt(index);
//					index++;
//				}
//			}
//			
//			//	store longest match if overlap is not allowed
//			if (!allowOverlap && foundMatch) {
//				Annotation a = new TemporaryAnnotation(tokens, null, lastStartIndex, ((lastMatchedIndex - lastStartIndex) + 1));
//				annotations.add(a);
//				index = lastMatchedIndex;
//			} else {
//				index = lastStartIndex;
//			}
//			
//			//	tidy up
//			foundMatch = false;
//			currentPart.clear();
//			lastToken = tokens.tokenAt(index);
//			index ++;
//		}
//		
//		Collections.sort(annotations);
//		return ((Annotation[]) annotations.toArray(new Annotation[annotations.size()]));
//	}
	
	/**
	 * Extract all parts from a String that are contained in a list of Strings
	 * @param tokens the TokenSequence to be rooted through
	 * @param list the Dictionary containing the Strings to be found
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that's String representation is contained in the
	 *         specified StringVector
	 */
	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list) {
		return extractAllContained(tokens, list, 0, list.isDefaultCaseSensitive(), false, true);
	}
	
	/**
	 * Extract all parts from a String that are contained in a list of Strings
	 * @param tokens the TokenSequence to be rooted through
	 * @param list the Dictionary containing the Strings to be found
	 * @param caseSensitive use case sensitive or case insensitive matching
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that's String representation is contained in the
	 *         specified StringVector
	 */
	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, boolean caseSensitive) {
		return extractAllContained(tokens, list, 0, caseSensitive, false, true);
	}
	
	/**
	 * Extract all parts from a String that are contained in a list of Strings
	 * @param tokens the TokenSequence to be rooted through
	 * @param list the Dictionary containing the Strings to be found
	 * @param caseSensitive use case sensitive or case insensitive matching
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that's String representation is contained in the
	 *         specified StringVector
	 */
	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, boolean caseSensitive, boolean allowOverlap) {
		return extractAllContained(tokens, list, 0, caseSensitive, allowOverlap, true);
	}
	
	/**
	 * Extract all parts from a String that are contained in a list of Strings
	 * @param tokens the TokenSequence to be rooted through
	 * @param list the Dictionary containing the Strings to be found
	 * @param caseSensitive use case sensitive or case insensitive matching
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted
	 * @param normalize normalize whitespaces? (default is true)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that's String representation is contained in the
	 *         specified StringVector
	 */
	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, boolean caseSensitive, boolean allowOverlap, boolean normalize) {
		return extractAllContained(tokens, list, 0, caseSensitive, allowOverlap, normalize);
	}
	
	/**
	 * Extract all parts from a String that are contained in a list of Strings
	 * @param tokens the TokenSequence to be rooted through
	 * @param list the Dictionary containing the Strings to be found
	 * @param maxTokens the maximum number of tokens a String contained in the
	 *            list may consist of (0 means no limit, Attention: high
	 *            computation effort)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that's String representation is contained in the
	 *         specified StringVector
	 */
	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, int maxTokens) {
		return extractAllContained(tokens, list, maxTokens, list.isDefaultCaseSensitive(), false, true);
	}
	
	/**
	 * Extract all parts from a String that are contained in a list of Strings
	 * @param tokens the TokenSequence to be rooted through
	 * @param list the Dictionary containing the Strings to be found
	 * @param maxTokens the maximum number of tokens a String contained in the
	 *            list may consist of (0 means no limit, Attention: high
	 *            computation effort)
	 * @param caseSensitive use case sensitive or case insensitive matching
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that's String representation is contained in the
	 *         specified StringVector
	 */
	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, int maxTokens, boolean caseSensitive) {
		return extractAllContained(tokens, list, maxTokens, caseSensitive, false, true);
	}
	
	/**
	 * Extract all parts from a String that are contained in a list of Strings
	 * @param tokens the TokenSequence to be rooted through
	 * @param list the Dictionary containing the Strings to be found
	 * @param maxTokens the maximum number of tokens a String contained in the
	 *            list may consist of (0 means no limit, Attention: high
	 *            computation effort)
	 * @param caseSensitive use case sensitive or case insensitive matching
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that's String representation is contained in the
	 *         specified StringVector
	 */
	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, int maxTokens, boolean caseSensitive, boolean allowOverlap) {
		return extractAllContained(tokens, list, maxTokens, caseSensitive, allowOverlap, true);
	}
	
	/**
	 * Extract all parts from a String that are contained in a list of Strings
	 * @param tokens the TokenSequence to be rooted through
	 * @param list the Dictionary containing the Strings to be found
	 * @param maxTokens the maximum number of tokens a String contained in the
	 *            list may consist of (0 means no limit, Attention: high
	 *            computation effort)
	 * @param caseSensitive use case sensitive or case insensitive matching
	 * @param allowOverlap allow a set of overlapping matches to be all
	 *            extracted
	 * @param normalize normalize whitespaces? (default is true)
	 * @return an array of Annotations marking all subsequences of the specified
	 *         TokenSequence that's String representation is contained in the
	 *         specified StringVector
	 */
	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, int maxTokens, boolean caseSensitive, boolean allowOverlap, boolean normalize) {
		
		//	check parameters
		if ((tokens == null) || (list == null) || list.isEmpty()) return new Annotation[0];
		
		ArrayList annotations = new ArrayList();
		
		int lastStartIndex = 0;
		int lastMatchedIndex = 0;
		int index = 0;
		Annotation currentPart;
		
		//	compute maximum number of Tokens in match, and extract valid start tokens
		int maximumTokens = 1;
		StringVector startTokens = new StringVector();
		Tokenizer tokenizer = tokens.getTokenizer();
		if (maxTokens == 1)
			maximumTokens = 1;
		
		else {
			StringVector normalizedList = new StringVector();
			
			maximumTokens = maxTokens;
			StringIterator si = list.getEntryIterator();
			
			while (si.hasMoreStrings()) {
				TokenSequence ts = tokenizer.tokenize(si.nextString());
				if (ts.size() != 0) {
					if ((maxTokens == 0) && (ts.size() > maximumTokens))
						maximumTokens = ts.size();
					
					startTokens.addElementIgnoreDuplicates(ts.firstValue());
					if (normalize)
						normalizedList.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(ts, true, true));
				}
			}
			
			if (normalize)
				list = normalizedList;
		}
		
		//	find matches
		while (index < tokens.size()) {
			
			Annotation match = null;
			lastStartIndex = index;
			boolean validStart = ((maximumTokens == 1) || (caseSensitive ? startTokens.contains(tokens.valueAt(index)) : startTokens.containsIgnoreCase(tokens.valueAt(index))));
			
			while (validStart && (index < tokens.size()) && ((index - lastStartIndex) <= maximumTokens)) {
				
				//	check if current part matches
				currentPart = Gamta.newAnnotation(tokens, null, lastStartIndex, (index - lastStartIndex + 1));
				String testPart = TokenSequenceUtils.concatTokens(currentPart, true, true);
				if (list.lookup(testPart, caseSensitive)) {
					lastMatchedIndex = index;
					if (allowOverlap)
						annotations.add(currentPart);
					else match = currentPart;
				}
				index++;
			}
			
			//	store match (if any)
			if (match == null)
				index = lastStartIndex;
			else {
				annotations.add(match);
				index = lastMatchedIndex;
			}
			
			//	tidy up
			match = null;
			index ++;
		}
		
		return ((Annotation[]) annotations.toArray(new Annotation[annotations.size()]));
	}
//	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, int maxTokens, boolean caseSensitive, boolean allowOverlap) {
//		
//		//	check parameters
//		if ((tokens == null) || (list == null) || list.isEmpty()) return new Annotation[0];
//		
//		ArrayList annotations = new ArrayList();
//		
//		int lastStartIndex = 0;
//		int lastMatchedIndex = 0;
//		int index = 0;
//		Annotation currentPart;
//		
//		//	compute maximum number of Tokens in match, and extract valid start tokens
//		int maximumTokens = 1;
//		StringVector startTokens = new StringVector();
//		Tokenizer tokenizer = tokens.getTokenizer();
//		if (maxTokens == 1)
//			maximumTokens = 1;
//		
//		else {
//			StringVector normalizedList = new StringVector();
//			
//			maximumTokens = maxTokens;
//			StringIterator si = list.getEntryIterator();
//			
//			while (si.hasMoreStrings()) {
//				TokenSequence ts = tokenizer.tokenize(si.nextString());
//				if (ts.size() != 0) {
//					if ((maxTokens == 0) && (ts.size() > maximumTokens))
//						maximumTokens = ts.size();
//					
//					startTokens.addElementIgnoreDuplicates(ts.firstValue());
//					normalizedList.addElementIgnoreDuplicates(TokenSequenceUtils.concatTokens(ts, true, true));
//				}
//			}
//			
//			list = normalizedList;
//		}
//		
//		System.out.println(startTokens.concatStrings("\n"));
//		
//		//	find matches
//		while (index < tokens.size()) {
//			
//			Annotation match = null;
//			lastStartIndex = index;
//			boolean validStart = ((maximumTokens == 1) || (caseSensitive ? startTokens.contains(tokens.valueAt(index)) : startTokens.containsIgnoreCase(tokens.valueAt(index))));
//			
//			while (validStart && (index < tokens.size()) && ((index - lastStartIndex) <= maximumTokens)) {
//				
//				//	check if current part matches
//				currentPart = Gamta.newAnnotation(tokens, null, lastStartIndex, (index - lastStartIndex));
//				String testPart = TokenSequenceUtils.concatTokens(currentPart, true, true);
//				if (list.lookup(testPart, caseSensitive)) {
//					lastMatchedIndex = index;
//					if (allowOverlap)
//						annotations.add(currentPart);
//					else match = currentPart;
//				}
//				index++;
//			}
//			
//			//	store match (if any)
//			if (match == null)
//				index = lastStartIndex;
//			else {
//				annotations.add(match);
//				index = lastMatchedIndex;
//			}
//			
//			//	tidy up
//			match = null;
//			index ++;
//		}
//		
//		return ((Annotation[]) annotations.toArray(new Annotation[annotations.size()]));
//	}
//	public static Annotation[] extractAllContained(TokenSequence tokens, Dictionary list, int maxTokens, boolean caseSensitive, boolean allowOverlap) {
//		
//		//	check parameters
//		if ((tokens == null) || (list == null) || list.isEmpty()) return new Annotation[0];
//		
//		ArrayList annotations = new ArrayList();
//		
//		int lastStartIndex = 0;
//		int lastMatchedIndex = 0;
//		int index = 0;
//		MutableTokenSequence currentPart = newTokenSequence(null, tokens.getTokenizer());
//		boolean foundMatch = false;
//		
//		//	compute maximum number of Tokens in match, and extract valid start tokens
//		int maximumTokens = 1;
//		StringVector startTokens = new StringVector();
//		Tokenizer tokenizer = tokens.getTokenizer();
//		if (maxTokens == 0) {
//			StringIterator si = list.getEntryIterator();
//			while (si.hasMoreStrings()) {
//				TokenSequence ts = tokenizer.tokenize(si.nextString());
//				if (ts.size() > maximumTokens) maximumTokens = ts.size();
//				startTokens.addElementIgnoreDuplicates(ts.firstValue());
//			}
//		} else if (maxTokens != 1) {
//			maximumTokens = maxTokens;
//			StringIterator si = list.getEntryIterator();
//			while (si.hasMoreStrings()) {
//				TokenSequence ts = tokenizer.tokenize(si.nextString());
//				startTokens.addElementIgnoreDuplicates(ts.firstValue());
//			}
//		} else maximumTokens = 1;
//		
//		//	find matches
//		while (index < tokens.size()) {
//			
//			lastStartIndex = index;
//			boolean validStart = ((maximumTokens == 1) || (caseSensitive ? startTokens.contains(tokens.valueAt(index)) : startTokens.containsIgnoreCase(tokens.valueAt(index))));
//			
//			while (validStart && (index < tokens.size()) && (currentPart.size() < maximumTokens)) {
//				
//				//	check if actual part matches
//				currentPart.addTokens(tokens.tokenAt(index));
//				String testPart = TokenSequenceUtils.concatTokens(currentPart, true, true);
//				if (list.lookup(testPart, caseSensitive)) {
//					foundMatch = true;
//					lastMatchedIndex = index;
//				}
//				index++;
//			}
//			
//			//	store eventual match
//			if (foundMatch) {
//				Annotation a = new TemporaryAnnotation(tokens, null, lastStartIndex, ((lastMatchedIndex - lastStartIndex) + 1));
//				annotations.add(a);
//				index = (allowOverlap ? lastStartIndex : lastMatchedIndex);
//			} else {
//				index = lastStartIndex;
//			}
//			
//			//	tidy up
//			foundMatch = false;
//			currentPart.clear();
//			index ++;
//		}
//		
//		return ((Annotation[]) annotations.toArray(new Annotation[annotations.size()]));
//	}
	
	/**
	 * Normalize a given string to its ASCII 7 base form. In particular, this
	 * method strips accents and other language specific extensions from
	 * characters and replaces them with their ASCII 7 base form. This is useful
	 * for regular expression pattern matching, indexing, etc. since it reduces
	 * the search space and saves typing accents and the like. The accuracy
	 * costs are moderate to non-existant. This method does non-strict
	 * normalization, thus transforms ligatures and German umlauts into the
	 * equivalent pairs of ASCII 7 characters.
	 * @param string the String to normalize
	 * @return the normalized String
	 */
	public static String normalize(String string) {
		return normalize(string, false);
	}
	
	/**
	 * Normalize a given string to its ASCII 7 base form. In particular, this
	 * method strips accents and other language specific extensions from
	 * characters and replaces them with their ASCII 7 base form. This is useful
	 * for regular expression pattern matching, indexing, etc. since it reduces
	 * the search space and saves typing accents and the like. The accuracy
	 * costs are moderate to non-existant. The strict form of normalization
	 * reduces ligatures and German umlauts to the plain base character, whereas
	 * the non-strict form transforms them into the equivalent pairs of ASCII 7
	 * characters.
	 * @param string the String to normalize
	 * @param strict do strict normalization?
	 * @return the normalized String
	 */
	public static String normalize(String string, boolean strict) {
//		return string;
//		Properties mappings = (strict ? strictMappings : nonStrictMappings);
		StringBuffer normalizedString = new StringBuffer();
		for (int c = 0; c < string.length(); c++) {
			if (strict)
				normalizedString.append(getBaseChar(string.charAt(c)));
			else normalizedString.append(getNormalForm(string.charAt(c)));
//			String s = string.substring(c, (c+1));
//			normalizedString.append(mappings.getProperty(s, s));
		}
		return (string.matches("[^a-z]++") ? normalizedString.toString().toUpperCase() : normalizedString.toString());
	}
//	
//	private static final Properties strictMappings = new Properties();
//	private static final Properties nonStrictMappings = new Properties();
//	private static final String[] mappingData = {
//		
//		//	hand coded
//		"À A",
//		"Á A",
//		"Â A",
//		"Ã A",
//		"Ä Ae",
//		"Å A",
//		"Æ Ae",
//
//		"Ç C",
//
//		"È E",
//		"É E",
//		"Ê E",
//		"Ë E",
//
//		"Ì I",
//		"Í I",
//		"Î I",
//		"Ï I",
//
//		"Ñ N",
//
//		"Ò O",
//		"Ó O",
//		"Ô O",
//		"Õ O",
//		"Ö Oe",
//		"Œ Oe",
//		"Ø O",
//
//		"Ù U",
//		"Ú U",
//		"Û U",
//		"Ü Ue",
//
//		"Ý Y",
//
//		"à a",
//		"á a",
//		"â a",
//		"ã a",
//		"ä ae",
//		"å a",
//		"æ ae",
//
//		"ç c",
//
//		"è e",
//		"é e",
//		"ê e",
//		"ë e",
//
//		"ì i",
//		"í i",
//		"î i",
//		"ï i",
//
//		"ñ n",
//
//		"ò o",
//		"ó o",
//		"ô o",
//		"õ o",
//		"ö oe",
//		"œ oe",
//		"ø o",
//
//		"ù u",
//		"ú u",
//		"û u",
//		"ü ue",
//
//		"ý y",
//		"ÿ y",
//
//		"ß ss",
//
//		"– -",
//		"— -",
//		
//		//	extracted from Wikipedia ISO-8859 char sets 
////		"? -",
////		"? -",
//		"× x",
//		"× x",
//		"× x",
//		"× x",
////		"? °",
//		"Á A",
//		"á a",
//		"Á A",
//		"á a",
//		"Á A",
//		"á a",
//		"À A",
//		"à a",
//		"À A",
//		"à a",
//		"À A",
//		"à a",
//		"À A",
//		"à a",
//		"Â A",
//		"â a",
//		"Â A",
//		"â a",
//		"Â A",
//		"â a",
//		"Ä A",
//		"ä a",
//		"Ä A",
//		"ä a",
//		"Ä A",
//		"ä a",
////		"? A",
////		"? a",
////		"? A",
////		"? a",
////		"? A",
////		"? a",
////		"? A",
////		"? A",
////		"? A",
////		"? a",
//		"Ã A",
//		"ã a",
//		"Ã A",
//		"ã a",
//		"Ã A",
//		"ã a",
//		"Ã A",
//		"ã a",
//		"Å A",
//		"å a",
//		"Å A",
//		"å a",
//		"Å A",
//		"å a",
//		"Å A",
//		"å a",
////		"? A",
////		"? a",
////		"? A",
////		"? a",
////		"? A",
////		"? a",
////		"? A",
////		"? a",
////		"? A",
////		"? a",
//		"Æ Ae",
//		"æ ae",
//		"Æ Ae",
//		"æ ae",
//		"Æ Ae",
//		"æ ae",
//		"Æ Ae",
//		"æ ae",
//		"Æ Ae",
//		"æ ae",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
////		"? C",
////		"? c",
//		"Ç C",
//		"ç c",
//		"Ç C",
//		"ç c",
//		"Ç C",
//		"ç e",
////		"? D",
////		"? d",
////		"? D",
////		"? d",
////		"? D",
////		"? d",
////		"? D",
////		"? d",
////		"? d",
//		"Ð D",
//		"ð d",
//		"Ð D",
//		"ð d",
//		"Ð D",
//		"ð d",
//		"É E",
//		"é e",
//		"É E",
//		"é e",
//		"É E",
//		"é e",
//		"È E",
//		"è e",
//		"È E",
//		"è e",
//		"È E",
//		"è e",
//		"È E",
//		"è e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
//		"Ê E",
//		"ê e",
//		"Ê E",
//		"ê e",
//		"Ê E",
//		"ê e",
//		"Ê E",
//		"ê e",
//		"Ë E",
//		"ë e",
//		"Ë E",
//		"ë e",
//		"Ë E",
//		"ë e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? G",
////		"? g",
////		"? G",
////		"? g",
////		"? G",
////		"? g",
////		"? G",
////		"? g",
////		"? G",
////		"? g",
////		"? G",
////		"? g",
////		"? G",
////		"? g",
////		"? G",
////		"? g",
////		"? H",
////		"? h",
////		"? h",
////		"? i",
////		"? i",
//		"Í I",
//		"í i",
//		"Í I",
//		"í i",
//		"Í I",
//		"í i",
//		"Ì I",
//		"ì i",
//		"Ì I",
//		"ì i",
//		"Ì I",
//		"ì i",
//		"Ì I",
//		"ì i",
//		"? I",
//		"? I",
//		"Î I",
//		"î i",
//		"Î I",
//		"î i",
//		"Î I",
//		"î i",
//		"Ï I",
//		"ï i",
//		"Ï I",
//		"ï i",
//		"Ï I",
//		"ï i",
//		"Ï I",
//		"ï i",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? J",
////		"? j",
////		"? k",
////		"? k",
////		"? K",
////		"? k",
////		"? K",
////		"? k",
////		"? K",
////		"? k",
////		"? L",
////		"? l",
////		"? L",
////		"? l",
////		"? L",
////		"? l",
////		"? L",
////		"? l",
////		"? L",
////		"? l",
////		"? L",
////		"? l",
////		"? L",
////		"? l",
////		"? L",
////		"? l",
////		"? N",
////		"? N",
////		"? n",
////		"? N",
////		"? n",
////		"? N",
////		"? n",
////		"? N",
////		"? n",
//		"Ñ N",
//		"ñ n",
//		"Ñ N",
//		"ñ n",
//		"Ñ N",
//		"ñ n",
//		"Ñ N",
//		"ñ n",
////		"? N",
////		"? n",
////		"? n",
////		"? N",
////		"? n",
////		"? N",
////		"? n",
////		"? N",
////		"? n",
////		"? ",
//		"º °",
//		"º °",
//		"Ó O",
//		"ó o",
//		"Ó O",
//		"ó o",
//		"Ó O",
//		"ó o",
//		"Ò O",
//		"ò o",
//		"Ò O",
//		"ò o",
//		"Ò O",
//		"ò o",
//		"Ò O",
//		"ò o",
//		"Ô O",
//		"ô o",
//		"Ô O",
//		"ô o",
//		"Ô O",
//		"ô o",
//		"Ö O",
//		"ö o",
//		"Ö O",
//		"ö o",
//		"Ö O",
//		"ö o",
////		"? O",
////		"? o",
////		"? o",
////		"? O",
////		"? o",
//		"Õ O",
//		"õ o",
//		"Õ O",
//		"õ o",
//		"Õ O",
//		"õ o",
////		"? O",
////		"? o",
////		"? O",
////		"? o",
//		"Ø O",
//		"ø o",
//		"Ø O",
//		"ø o",
//		"Ø O",
//		"ø o",
//		"Ø O",
//		"ø o",
//		"Ø O",
//		"ø o",
//		"Œ Oe",
//		"œ oe",
////		"? R",
////		"? r",
////		"? R",
////		"? r",
////		"? R",
////		"? r",
////		"? R",
////		"? r",
////		"? d",
////		"? S",
////		"? s",
////		"? S",
////		"? s",
////		"? S",
////		"? S",
////		"? s",
//		"Š S",
//		"š s",
//		"Š S",
//		"š s",
//		"Š S",
//		"š s",
//		"Š S",
//		"š s",
//		"Š S",
//		"š s",
////		"? S",
////		"? s",
////		"? S",
////		"? s",
//		"ß ss",
//		"ß ss",
//		"ß ss",
////		"? T",
////		"? t",
////		"? T",
////		"? t",
//		"Þ B",
//		"þ b",
//		"Þ b",
//		"þ b",
//		"Þ b",
//		"þ b",
////		"? F",
////		"? F",
////		"? t",
////		"? t",
//		"Ú U",
//		"ú u",
//		"Ú U",
//		"ú u",
//		"Ú U",
//		"ú u",
//		"Ù U",
//		"ù u",
//		"Ù U",
//		"ù u",
//		"Ù U",
//		"ù u",
//		"Ù U",
//		"ù u",
//		"Û U",
//		"û u",
//		"Û U",
//		"û u",
//		"Û U",
//		"û u",
//		"Û U",
//		"û u",
//		"Ü U",
//		"ü u",
//		"Ü U",
//		"ü u",
//		"Ü U",
//		"ü u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? U",
////		"? u",
////		"? W",
////		"? w",
////		"? W",
////		"? w",
////		"? W",
////		"? w",
////		"? W",
////		"? w",
//		"Ý Y",
//		"ý y",
//		"Ý Y",
//		"ý y",
//		"Ý Y",
//		"ý y",
////		"? Y",
////		"? y",
////		"? Y",
////		"? y",
//		"ÿ y",
//		"ÿ y",
//		"Ÿ Y",
//		"ÿ y",
//		"Ÿ Y",
////		"? Z",
////		"? z",
////		"? Z",
////		"? z",
////		"? Z",
////		"? z",
////		"? Z",
////		"? z",
////		"? Z",
////		"? z",
////		"? Z",
////		"? z",
//		"Ž Z",
//		"ž z",
//		"Ž Z",
//		"ž z",
//		"Ž Z",
//		"ž z",
//		"Ž Z",
//		"ž z",
//		"Ž Z",
//		"ž z",
////		"? A",
////		"? A",
////		"? a",
////		"? B",
////		"? E",
////		"? E",
////		"? Z",
////		"? H",
////		"? H",
////		"? n",
////		"? n",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? i",
////		"? K",
////		"? M",
////		"? N",
////		"? O",
////		"? o",
////		"? O",
////		"? o",
////		"? P",
////		"? T",
////		"? u",
////		"? u",
////		"? u",
////		"? u",
////		"? Y",
////		"? Y",
////		"? Y",
////		"? P",
////		"? X",
////		"? O",
////		"? A",
////		"? a",
////		"? B",
////		"? E",
////		"? e",
////		"? E",
////		"? e",
////		"? S",
////		"? s",
////		"? I",
////		"? i",
////		"? I",
////		"? i",
////		"? J",
////		"? j",
////		"? K",
////		"? k",
////		"? M",
////		"? m",
////		"? H",
////		"? O",
////		"? o",
////		"? P",
////		"? p",
////		"? C",
////		"? c",
////		"? T",
////		"? h",
////		"? Y",
////		"? y",
////		"? Y",
////		"? y",
////		"? x",
////		"? o",
////		"? I",
////		"? i",
//		
//		//	extracted from Wikipedia list of punctuation marks
////		"? -",
//		"— -",
//		"– -",
////		"? -",
////		"? -",
//		
////		"? /",
//	};
//	static {
//		for (int m = 0; m < mappingData.length; m++) try {
//			String mapped = mappingData[m].substring(0, 1);
//			String mapping = mappingData[m].substring(2);
//			strictMappings.setProperty(mapped, mapping.substring(0, 1));
//			nonStrictMappings.setProperty(mapped, mapping);
//		}
//		catch (Exception e) {
//			System.out.println(e.getMessage() + " while parsing '" + mappingData[m] + "'");
//			e.printStackTrace(System.out);
//		}
//	}
	
	//	static Tokenizers for existing methods
	
//	//	TODOne extend this to cover letters from additional writing systems (Cyrillic, etc.), making blocks out of (any groups of) letters from any one writing system
//	
//	/**
//	 * The regular expression pattern used in the Tokenizer allowing in-word
//	 * (''' and dashes) and in-number (',' and '.') punctuation.
//	 */
//	public static final String INNER_PUNCTUATION_TOKENIZER_REGEX = "(" +
//			"([" + Gamta.LETTERS + "]++([" + Gamta.LETTERS + RegExUtils.escapeForRegEx(Gamta.IN_WORD_PUNCTUATION) + "]++)*+)" +
//			"|" +
//			"([" + Gamta.DIGITS + "]++([" + RegExUtils.escapeForRegEx(Gamta.IN_NUMBER_PUNCTUATION) + "][" + Gamta.DIGITS + "]++)*+)" +
//			"|" +
//			"((.)(\\7)*+)" +
//		")";
//	
//	/**
//	 * The Tokenizer allowing in-word (''' and dashes) and in-number (',' and '.')
//	 * punctuation. This Tokenizer is GAMTA's default Tokenizer.
//	 */
//	public static final Tokenizer INNER_PUNCTUATION_TOKENIZER = new RegExTokenizer(INNER_PUNCTUATION_TOKENIZER_REGEX);
//	
//	//	TODOne extend this to cover letters from additional writing systems (Cyrillic, etc.), making blocks out of (any groups of) letters from any one writing system
//	
//	/**
//	 * The regular expression pattern used in the Tokenizer forbidding any
//	 * in-word and in-number punctuation, thus forcing word to consist strictly
//	 * of letters, and numbers to consist strictly of digits
//	 */
//	public static final String NO_INNER_PUNCTUATION_TOKENIZER_REGEX = "(" +
//			"([" + Gamta.LETTERS + "]++)" +
//			"|" +
//			"([" + Gamta.DIGITS + "]++)" +
//			"|" +
//			"((.)(\\5)*+)" +
//		")";
//	
//	/**
//	 * The Tokenizer forbidding any in-word and in-number punctuation, thus
//	 * forcing word to consist strictly of letters, and numbers to consist
//	 * strictly of digits
//	 */
//	public static final Tokenizer NO_INNER_PUNCTUATION_TOKENIZER = new RegExTokenizer(NO_INNER_PUNCTUATION_TOKENIZER_REGEX);
//	
	private static final String[] unicodeBlocks = {
		"0000;007F;Latin, Common", // Basic Latin[g] 
		"0080;00FF;Latin, Common", // Latin-1 Supplement[h] 
		"0100;017F;Latin", // Latin Extended-A 
		"0180;024F;Latin", // Latin Extended-B 
		"0250;02AF;Latin", // IPA Extensions 
		"02B0;02FF;Latin, Common", // Spacing Modifier Letters 
		"0300;036F;Inherited", // Combining Diacritical Marks 
		"0370;03FF;Greek, Coptic, Common", // Greek and Coptic 
		"0400;04FF;Cyrillic, Inherited", // Cyrillic 
		"0500;052F;Cyrillic", // Cyrillic Supplement 
		"0530;058F;Armenian, Common", // Armenian 
		"0590;05FF;Hebrew", // Hebrew 
		"0600;06FF;Arabic, Common, Inherited", // Arabic 
		"0700;074F;Syriac", // Syriac 
		"0750;077F;Arabic", // Arabic Supplement 
		"0780;07BF;Thaana", // Thaana 
		"07C0;07FF;Nko", // NKo 
		"0800;083F;Samaritan", // Samaritan 
		"0840;085F;Mandaic", // Mandaic 
		"08A0;08FF;Arabic", // Arabic Extended-A 
		"0900;097F;Devanagari, Common, Inherited", // Devanagari 
		"0980;09FF;Bengali", // Bengali 
		"0A00;0A7F;Gurmukhi", // Gurmukhi 
		"0A80;0AFF;Gujarati", // Gujarati 
		"0B00;0B7F;Oriya", // Oriya 
		"0B80;0BFF;Tamil", // Tamil 
		"0C00;0C7F;Telugu", // Telugu 
		"0C80;0CFF;Kannada", // Kannada 
		"0D00;0D7F;Malayalam", // Malayalam 
		"0D80;0DFF;Sinhala", // Sinhala 
		"0E00;0E7F;Thai, Common", // Thai 
		"0E80;0EFF;Lao", // Lao 
		"0F00;0FFF;Tibetan, Common", // Tibetan 
		"1000;109F;Myanmar", // Myanmar 
		"10A0;10FF;Georgian, Common", // Georgian 
		"1100;11FF;Hangul", // Hangul Jamo 
		"1200;137F;Ethiopic", // Ethiopic 
		"1380;139F;Ethiopic", // Ethiopic Supplement 
		"13A0;13FF;Cherokee", // Cherokee 
		"1400;167F;Canadian Aboriginal", // Unified Canadian Aboriginal Syllabics 
		"1680;169F;Ogham", // Ogham 
		"16A0;16FF;Runic, Common", // Runic 
		"1700;171F;Tagalog", // Tagalog 
		"1720;173F;Hanunoo, Common", // Hanunoo 
		"1740;175F;Buhid", // Buhid 
		"1760;177F;Tagbanwa", // Tagbanwa 
		"1780;17FF;Khmer", // Khmer 
		"1800;18AF;Mongolian, Common", // Mongolian 
		"18B0;18FF;Canadian Aboriginal", // Unified Canadian Aboriginal Syllabics Extended 
		"1900;194F;Limbu", // Limbu 
		"1950;197F;Tai Le", // Tai Le 
		"1980;19DF;New Tai Lue", // New Tai Lue 
		"19E0;19FF;Khmer", // Khmer Symbols 
		"1A00;1A1F;Buginese", // Buginese 
		"1A20;1AAF;Tai Tham", // Tai Tham 
		"1B00;1B7F;Balinese", // Balinese 
		"1B80;1BBF;Sundanese", // Sundanese 
		"1BC0;1BFF;Batak", // Batak 
		"1C00;1C4F;Lepcha", // Lepcha 
		"1C50;1C7F;Ol Chiki", // Ol Chiki 
		"1CC0;1CCF;Sundanese", // Sundanese Supplement 
		"1CD0;1CFF;Common, Inherited", // Vedic Extensions 
		"1D00;1D7F;Cyrillic, Greek, Latin", // Phonetic Extensions 
		"1D80;1DBF;Latin, Greek", // Phonetic Extensions Supplement 
		"1DC0;1DFF;Inherited", // Combining Diacritical Marks Supplement 
		"1E00;1EFF;Latin", // Latin Extended Additional 
		"1F00;1FFF;Greek", // Greek Extended 
		"2000;206F;Common, Inherited", // General Punctuation 
		"2070;209F;Latin, Common", // Superscripts and Subscripts 
		"20A0;20CF;Common", // Currency Symbols 
		"20D0;20FF;Inherited", // Combining Diacritical Marks for Symbols 
		"2100;214F;Latin, Greek, Common", // Letterlike Symbols 
		"2150;218F;Latin, Common", // Number Forms 
		"2190;21FF;Common", // Arrows 
		"2200;22FF;Common", // Mathematical Operators 
		"2300;23FF;Common", // Miscellaneous Technical 
		"2400;243F;Common", // Control Pictures 
		"2440;245F;Common", // Optical Character Recognition 
		"2460;24FF;Common", // Enclosed Alphanumerics 
		"2500;257F;Common", // Box Drawing 
		"2580;259F;Common", // Block Elements 
		"25A0;25FF;Common", // Geometric Shapes 
		"2600;26FF;Common", // Miscellaneous Symbols 
		"2700;27BF;Common", // Dingbats 
		"27C0;27EF;Common", // Miscellaneous Mathematical Symbols-A 
		"27F0;27FF;Common", // Supplemental Arrows-A 
		"2800;28FF;Braille", // Braille Patterns 
		"2900;297F;Common", // Supplemental Arrows-B 
		"2980;29FF;Common", // Miscellaneous Mathematical Symbols-B 
		"2A00;2AFF;Common", // Supplemental Mathematical Operators 
		"2B00;2BFF;Common", // Miscellaneous Symbols and Arrows 
		"2C00;2C5F;Glagolitic", // Glagolitic 
		"2C60;2C7F;Latin", // Latin Extended-C 
		"2C80;2CFF;Coptic", // Coptic 
		"2D00;2D2F;Georgian", // Georgian Supplement 
		"2D30;2D7F;Tifinagh", // Tifinagh 
		"2D80;2DDF;Ethiopic", // Ethiopic Extended 
		"2DE0;2DFF;Cyrillic", // Cyrillic Extended-A 
		"2E00;2E7F;Common", // Supplemental Punctuation 
		"2E80;2EFF;Han", // CJK Radicals Supplement 
		"2F00;2FDF;Han", // Kangxi Radicals 
		"2FF0;2FFF;Common", // Ideographic Description Characters 
		"3000;303F;Han, Hangul, Common, Inherited", // CJK Symbols and Punctuation 
		"3040;309F;Hiragana, Common, Inherited", // Hiragana 
		"30A0;30FF;Katakana, Common", // Katakana 
		"3100;312F;Bopomofo", // Bopomofo 
		"3130;318F;Hangul", // Hangul Compatibility Jamo 
		"3190;319F;Common", // Kanbun 
		"31A0;31BF;Bopomofo", // Bopomofo Extended 
		"31C0;31EF;Common", // CJK Strokes 
		"31F0;31FF;Katakana", // Katakana Phonetic Extensions 
		"3200;32FF;Katakana, Hangul, Common", // Enclosed CJK Letters and Months 
		"3300;33FF;Katakana, Common", // CJK Compatibility 
		"3400;4DBF;Han", // CJK Unified Ideographs Extension A 
		"4DC0;4DFF;Common", // Yijing Hexagram Symbols 
		"4E00;9FFF;Han", // CJK Unified Ideographs 
		"A000;A48F;Yi", // Yi Syllables 
		"A490;A4CF;Yi", // Yi Radicals 
		"A4D0;A4FF;Lisu", // Lisu 
		"A500;A63F;Vai", // Vai 
		"A640;A69F;Cyrillic", // Cyrillic Extended-B 
		"A6A0;A6FF;Bamum", // Bamum 
		"A700;A71F;Common", // Modifier Tone Letters 
		"A720;A7FF;Latin, Common", // Latin Extended-D 
		"A800;A82F;Syloti Nagri", // Syloti Nagri 
		"A830;A83F;Common", // Common Indic Number Forms 
		"A840;A87F;Phags Pa", // Phags-pa 
		"A880;A8DF;Saurashtra", // Saurashtra 
		"A8E0;A8FF;Devanagari", // Devanagari Extended 
		"A900;A92F;Kayah Li", // Kayah Li 
		"A930;A95F;Rejang", // Rejang 
		"A960;A97F;Hangul", // Hangul Jamo Extended-A 
		"A980;A9DF;Javanese", // Javanese 
		"AA00;AA5F;Cham", // Cham 
		"AA60;AA7F;Myanmar", // Myanmar Extended-A 
		"AA80;AADF;Tai Viet", // Tai Viet 
		"AAE0;AAFF;Meetei Mayek", // Meetei Mayek Extensions 
		"AB00;AB2F;Ethiopic", // Ethiopic Extended-A 
		"ABC0;ABFF;Meetei Mayek", // Meetei Mayek 
		"AC00;D7AF;Hangul", // Hangul Syllables 
		"D7B0;D7FF;Hangul", // Hangul Jamo Extended-B 
//		"D800;DB7F;", // High Surrogates 
//		"DB80;DBFF;", // High Private Use Surrogates 
//		"DC00;DFFF;", // Low Surrogates 
//		"E000;F8FF;", // Private Use Area 
		"F900;FAFF;Han", // CJK Compatibility Ideographs 
		"FB00;FB4F;Latin, Hebrew, Armenian", // Alphabetic Presentation Forms 
		"FB50;FDFF;Arabic, Common", // Arabic Presentation Forms-A 
		"FE00;FE0F;Inherited", // Variation Selectors 
		"FE10;FE1F;Common", // Vertical Forms 
		"FE20;FE2F;Inherited", // Combining Half Marks 
		"FE30;FE4F;Common", // CJK Compatibility Forms 
		"FE50;FE6F;Common", // Small Form Variants 
		"FE70;FEFF;Arabic, Common", // Arabic Presentation Forms-B 
		"FF00;FFEF;Latin, Katakana, Hangul, Common", // Halfwidth and fullwidth forms 
		"FFF0;FFFF;Common", // Specials 
	};
	static {
		
		//	set up cache for character ranges in each script
		Map charBlocks = new LinkedHashMap() {
			public Object get(Object key) {
				Object o = super.get(key);
				if (o == null) {
					o = new TreeSet();
					this.put(key, o);
				}
				return o;
			}
		};
		
		//	go through Unicode blocks
		int charClass = -1;
		int blockStart = -1;
		for (int b = 0; b < unicodeBlocks.length; b++) {
			String[] blockData = unicodeBlocks[b].split("\\;");
			int l = Integer.parseInt(blockData[0], 16);
			int h = Integer.parseInt(blockData[1], 16);
			String[] scripts = blockData[2].split("\\s*\\,\\s*");
			for (int c = l; c <= h; c++) {
				char ch = ((char) c);
				if (Character.isLetter(ch)) {
					if (charClass != 0) {
						if (blockStart != -1) {
							int blockEnd = (c - 1);
							String block = ((blockStart == blockEnd) ? getHex(blockStart) : (getHex(blockStart) + "-" + getHex(blockEnd)));
							storeCharBlock(charClass, scripts, block, charBlocks);
						}
						blockStart = c;
					}
					charClass = 0;
				}
				else if (Character.isDigit(ch)) {
					if (charClass != 1) {
						if (blockStart != -1) {
							int blockEnd = (c - 1);
							String block = ((blockStart == blockEnd) ? getHex(blockStart) : (getHex(blockStart) + "-" + getHex(blockEnd)));
							storeCharBlock(charClass, scripts, block, charBlocks);
						}
						blockStart = c;
					}
					charClass = 1;
				}
				else {
					if (charClass != 2) {
						if (blockStart != -1) {
							int blockEnd = (c - 1);
							String block = ((blockStart == blockEnd) ? getHex(blockStart) : (getHex(blockStart) + "-" + getHex(blockEnd)));
							storeCharBlock(charClass, scripts, block, charBlocks);
						}
						blockStart = c;
					}
					charClass = 2;
				}
			}
		}
		
		//	get/remove special character groups
		charBlocks.remove("Punctuation");
		TreeSet digits = ((TreeSet) charBlocks.remove("Digits"));
		TreeSet common = ((TreeSet) charBlocks.remove("Common"));
		TreeSet inherited = ((TreeSet) charBlocks.remove("Inherited"));
		
		//	generate in-word and in-number punctuation ranges
		String inWordPunctuation = "[";
		for (int p = 0; p < Gamta.IN_WORD_PUNCTUATION.length(); p++)
			inWordPunctuation += getHex(Gamta.IN_WORD_PUNCTUATION.charAt(p));
		inWordPunctuation += "]";
		String inNumberPunctuation = "[";
		for (int p = 0; p < Gamta.IN_NUMBER_PUNCTUATION.length(); p++)
			inNumberPunctuation += getHex(Gamta.IN_NUMBER_PUNCTUATION.charAt(p));
		inNumberPunctuation += "]";
		
		//	start regexes
		StringBuffer noInnerPunctuationTokenizerRegex = new StringBuffer("(");
		StringBuffer innerPunctuationTokenizerRegex = new StringBuffer("(");
		
		//	add parts for letter blocks in individual scripts
		for (Iterator sit = charBlocks.keySet().iterator(); sit.hasNext();) {
			String scriptName = ((String) sit.next());
			TreeSet scriptCharRanges = ((TreeSet) charBlocks.get(scriptName));
			StringBuffer scriptCharRange = new StringBuffer();
			for (Iterator rit = scriptCharRanges.iterator(); rit.hasNext();)
				scriptCharRange.append((String) rit.next());
//			System.out.println(scriptName + ": " + scriptCharRange.toString());
			
			scriptCharRanges.addAll(common);
			scriptCharRanges.addAll(inherited);
			StringBuffer extendedScriptCharRange = new StringBuffer();
			for (Iterator rit = scriptCharRanges.iterator(); rit.hasNext();)
				extendedScriptCharRange.append((String) rit.next());
			
			if (noInnerPunctuationTokenizerRegex.length() > 1)
				noInnerPunctuationTokenizerRegex.append("|");
			noInnerPunctuationTokenizerRegex.append("([" + scriptCharRange.toString() + "]++([" + extendedScriptCharRange.toString() + "]++)*+)");
			if (innerPunctuationTokenizerRegex.length() > 1)
				innerPunctuationTokenizerRegex.append("|");
			innerPunctuationTokenizerRegex.append("([" + scriptCharRange.toString() + "]++(" + inWordPunctuation + "?[" + extendedScriptCharRange.toString() + "]++)*" + inWordPunctuation + "?)");
		}
		
		//	add parts for digit blocks
		StringBuffer digitCharRange = new StringBuffer();
		for (Iterator rit = digits.iterator(); rit.hasNext();)
			digitCharRange.append((String) rit.next());
		
		noInnerPunctuationTokenizerRegex.append("|");
		noInnerPunctuationTokenizerRegex.append("([" + digitCharRange.toString() + "]++)");
		innerPunctuationTokenizerRegex.append("|");
		innerPunctuationTokenizerRegex.append("([" + digitCharRange.toString() + "]++(" + inNumberPunctuation + "?[" + digitCharRange.toString() + "]++)*)");
		
		//	add catch part for all the rest
		noInnerPunctuationTokenizerRegex.append("|");
		noInnerPunctuationTokenizerRegex.append("((.)(\\" + ((2 * charBlocks.size()) + 2 + 2) + ")*+)");
		innerPunctuationTokenizerRegex.append("|");
		innerPunctuationTokenizerRegex.append("((.)(\\" + ((2 * charBlocks.size()) + 2 + 3) + ")*+)");
		
		//	finalize regex
		noInnerPunctuationTokenizerRegex.append(")");
		innerPunctuationTokenizerRegex.append(")");
		
		//	store constants
		NO_INNER_PUNCTUATION_TOKENIZER_REGEX = noInnerPunctuationTokenizerRegex.toString();
		INNER_PUNCTUATION_TOKENIZER_REGEX = innerPunctuationTokenizerRegex.toString();
	}
	private static final void storeCharBlock(int charClass, String[] scripts, String block, Map charBlocks) {
		if (charClass == 0) {
			for (int s = 0; s < scripts.length; s++) {
				((TreeSet) charBlocks.get(scripts[s])).add(block);
//				System.out.println(scripts[s] + " letter block: " + block);
			}
		}
		else if (charClass == 1) {
			((TreeSet) charBlocks.get("Digits")).add(block);
//			System.out.println("digit block: " + block);
		}
		else if (charClass == 2) {
			((TreeSet) charBlocks.get("Punctuation")).add(block);
//			System.out.println("punctuation block: " + block);
		}
	}
	private static final String getHex(int i) {
		String is = Integer.toString(i, 16);
		while (is.length() < 4)
			is = ("0" + is);
		return ("\\u" + is);
	}
	
	/**
	 * The regular expression pattern used in the Tokenizer allowing in-word
	 * (''' and dashes) and in-number (',' and '.') punctuation.
	 */
	public static final String INNER_PUNCTUATION_TOKENIZER_REGEX;
	
	/**
	 * The Tokenizer allowing in-word (''' and dashes) and in-number (',' and '.')
	 * punctuation. This Tokenizer is GAMTA's default Tokenizer.
	 */
	public static final Tokenizer INNER_PUNCTUATION_TOKENIZER = new RegExTokenizer(INNER_PUNCTUATION_TOKENIZER_REGEX);
	
	/**
	 * The regular expression pattern used in the Tokenizer forbidding any
	 * in-word and in-number punctuation, thus forcing word to consist strictly
	 * of letters, and numbers to consist strictly of digits
	 */
	public static final String NO_INNER_PUNCTUATION_TOKENIZER_REGEX;
	
	/**
	 * The Tokenizer forbidding any in-word and in-number punctuation, thus
	 * forcing word to consist strictly of letters, and numbers to consist
	 * strictly of digits
	 */
	public static final Tokenizer NO_INNER_PUNCTUATION_TOKENIZER = new RegExTokenizer(NO_INNER_PUNCTUATION_TOKENIZER_REGEX);
	
	/**
	 * Unify a portion of whitespace (in particular, convert '\n\r' and '\r\n'
	 * to '\n', then convert remaining '\r' to '\n')
	 * @param whitespace the whitespace to unify
	 * @return the specified whitespace with unified line breaks
	 */
	public static String unifyWhitespace(String whitespace) {
		String unifiedWhitespace = whitespace;
		unifiedWhitespace = unifiedWhitespace.replaceAll("\\\r\\\n", "\\\n");
		unifiedWhitespace = unifiedWhitespace.replaceAll("\\\n\\\r", "\\\n");
		unifiedWhitespace = unifiedWhitespace.replaceAll("\\\r", "\\\n");
		return unifiedWhitespace;
	}
	
	/**
	 * Create a new Annotation ID (can be used for other purposes as well).
	 * Basically, this method produces a 128 bit random number and returns its
	 * hex representation.
	 * @return a new 128 bit hex String
	 */
	public static String getAnnotationID() {
		
		//	generate 128 random bits, packed in a char array
		char[] bits = new char[8];
		for (int i = 0; i < 8; i++) {
			double r = Math.random();
			bits[i] = (char) (r * 65536);
		}
		
		//	convert each 16 bit block (char) into it's four-char hex representation
		char[] hex = new char[32];
		for (int i = 0; i < 8; i++) {
			char c = bits[i];
			for (int j = 0; j < 4; j++) {
				byte b = ((byte) (c & 15));
				hex[((4*i) + (3-j))] = ((b < 10) ? ((char) (b + '0')) : ((char) (b + '7'))); //	'7' = 'A' - 10
				c >>= 4;
			}
		}
		
		//	compose ID String and return it
		StringBuffer id = new StringBuffer();
		for (int i = 0; i < hex.length; i++) id.append(hex[i]);
		return (id.toString());
	}
}
