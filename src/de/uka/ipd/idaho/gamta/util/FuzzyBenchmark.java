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
import java.util.HashMap;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.stringUtils.StringVector;


/**
 * A Benchmark implementation transferring the AnnotationDiffer algorithm to
 * documents that may have different words / tokens. Since the word sequences
 * may differ, their difference is evaluated as well. Before evaluating the
 * Annotations, the document words are transformed to match those of the
 * benchmark document so that the AnnotationDiffer algorithm becomes applicable.
 * Since both transformation and evaluation are done on copies, no input
 * document will be changed by the transformation.
 */
public class FuzzyBenchmark {
	private static final boolean DEBUG = false;
	
	private int tokenDistance = 0;
	
	private ArrayList benchmarks = new ArrayList();
	private StringVector benchmarkedAnnotationTypes = new StringVector();
	private HashMap benchmarksByAnnotationType = new HashMap();
	
	private StringVector summary = new StringVector();
	
	private FuzzyBenchmark() {}
	
	/**
	 * @return the distance between the token sequences in document and the
	 *         benchmark document
	 */
	public int getTokenDistance() {
		return this.tokenDistance;
	}
	
	/**
	 * get the Benchmark for a specific annotation type
	 * @param annotationType the annotation type to obtain the Benchmark for
	 * @return the Benchmark for the specified annotation type, or null, if
	 *         there is no such Benchmark
	 */
	public Benchmark getBenchmark(String annotationType) {
		return ((Benchmark) this.benchmarksByAnnotationType.get(annotationType));
	}
	
	/**
	 * @return the annotation types that have been evaluated for this
	 *         FuzzyBenchmark
	 */
	public String[] getBenchmarkedAnnotationTypes() {
		return this.benchmarkedAnnotationTypes.toStringArray();
	}

	/**
	 * @return all the markup-related Benchmarks belonging to this
	 *         FuzzyBenchmark
	 */
	public Benchmark[] getBenchmarks() {
		return ((Benchmark[]) this.benchmarks.toArray(new Benchmark[this.benchmarks.size()]));
	}

	private void addBenchmark(Benchmark bm, String type) {
		this.benchmarks.add(bm);
		this.benchmarkedAnnotationTypes.addElement(type);
		this.benchmarksByAnnotationType.put(type, bm);
	}
	
	/**
	 * @return the evaluation summary
	 */
	public String[] getSummary() {
		return this.summary.toStringArray();
	}

	private void addSummaryEntry(String entry) {
		this.summary.addElement(entry);
	}
	
	/**
	 * evaluate the markup of a document against the markup of a benchmark
	 * document
	 * @param doc the DocumentPart to evaluate
	 * @param benchmark the DocumentPart to use as the benchmark for evaluation
	 * @param annotationType the type of the Annotations to evaluate
	 * @return an array holding the result messages
	 */
	public static FuzzyBenchmark computeBenchmark(QueriableAnnotation doc, QueriableAnnotation benchmark, String annotationType) {
		String[] type = {annotationType};
		return computeBenchmark(doc, benchmark, type, new StringVector());
	}
	
	/**
	 * evaluate the markup of a document against the markup of a benchmark
	 * document
	 * @param doc the DocumentPart to evaluate
	 * @param benchmark the DocumentPart to use as the benchmark for evaluation
	 * @param annotationTypes an array holding the types of the Annotations to
	 *            evaluate
	 * @return an array holding the result messages
	 */
	public static FuzzyBenchmark computeBenchmark(QueriableAnnotation doc, QueriableAnnotation benchmark, String[] annotationTypes) {
		return computeBenchmark(doc, benchmark, annotationTypes, new StringVector());
	}

	/**
	 * evaluate the markup of a document against the markup of a benchmark
	 * document
	 * @param doc the DocumentPart to evaluate
	 * @param benchmark the DocumentPart to use as the benchmark for evaluation
	 * @param annotationType the type of the Annotations to evaluate
	 * @param relevantAttributes a Set containing the names of the attributes to
	 *            consider when comparing Annotations (specifying null will
	 *            consider all attributes)
	 * @return an array holding the result messages
	 */
	public static FuzzyBenchmark computeBenchmark(QueriableAnnotation doc, QueriableAnnotation benchmark, String annotationType, StringVector relevantAttributes) {
		String[] type = { annotationType };
		return computeBenchmark(doc, benchmark, type, relevantAttributes);
	}

	/**
	 * evaluate the markup of a document against the markup of a benchmark
	 * document
	 * @param doc the DocumentPart to evaluate
	 * @param benchmark the DocumentPart to use as the benchmark for evaluation
	 * @param annotationTypes an array holding the types of the Annotations to
	 *            evaluate
	 * @param relevantAttributes a Set containing the names of the attributes to
	 *            consider when comparing Annotations (specifying null will
	 *            consider all attributes)
	 * @return an array holding the result messages
	 */
	public static FuzzyBenchmark computeBenchmark(QueriableAnnotation doc, QueriableAnnotation benchmark, String[] annotationTypes, StringVector relevantAttributes) {
		FuzzyBenchmark result = new FuzzyBenchmark();
		
		//	check parameters
		if (doc == null)
			result.addSummaryEntry("No document specified for evaluation.");
		if (benchmark == null)
			result.addSummaryEntry("No benchmark document specified.");
		if (annotationTypes == null)
			result.addSummaryEntry("No annotationTypes specified for evaluation.");
		if (!result.summary.isEmpty())
			return result;
		
		//	copy documents
		MutableAnnotation dDoc = Gamta.copyDocument(doc);
		
		//	collect token values
		StringVector dTokens = new StringVector();
		for (int t = 0; t < dDoc.size(); t++) dTokens.addElement(dDoc.valueAt(t));
		StringVector bTokens = new StringVector();
		for (int t = 0; t < benchmark.size(); t++) bTokens.addElement(benchmark.valueAt(t));
		
		//	get unique values (eliminate shared ones)
		StringVector sharedTokens = dTokens.intersect(bTokens);
		for (int s = 0; s < sharedTokens.size(); s++) {
			String shared = sharedTokens.get(s);
			while (dTokens.contains(shared) && bTokens.contains(shared)) {
				bTokens.remove(shared);
				dTokens.remove(shared);
			}
		}
		
		//	check hyphenated words
		int tokenIndex = 0;
		int hyphenationCount = 0;
		while ((tokenIndex + 1) < dDoc.size()) {
			Token t1 = dDoc.tokenAt(tokenIndex);
			Token t2 = dDoc.tokenAt(tokenIndex + 1);
			
			//	concatenate word divided Tokens
			if (isWordDevided(t1, t2) && !bTokens.contains(t1.getValue()) && !bTokens.contains(t2.getValue()) && bTokens.contains(t1.getValue().substring(0, (t1.length() - 1)) + t2.getValue())) {
				if (DEBUG) System.out.println("Hyphenated: " + t1.getValue() + ", " + t2.getValue());
				dTokens.remove(t1.getValue());
				dTokens.remove(t2.getValue());
				String word = t1.getValue().substring(0, (t1.length() - 1)) + t2.getValue();
				dDoc.setValueAt(word, tokenIndex);
				dDoc.removeTokensAt(tokenIndex+1, 1);
				bTokens.remove(word);
				hyphenationCount++;
			}
			else tokenIndex++;
		}
		if (DEBUG) System.out.println("Hypenated words in doc: " + hyphenationCount);
		result.addSummaryEntry("Hyphenated words: " + hyphenationCount);
		result.tokenDistance += hyphenationCount;
		
		//	show remaining tokens
		if (DEBUG) System.out.println("Unique in doc:\n - " + dTokens.concatStrings("\n - "));
		result.tokenDistance += dTokens.size();
		if (DEBUG) System.out.println("Unique in benchmark:\n - " + bTokens.concatStrings("\n - "));
		result.tokenDistance += bTokens.size();
		
		//	evaluate textual difference
		if (dTokens.size() > bTokens.size()) {
			if (DEBUG) System.out.println("Misspelled words in doc: " + bTokens.size());
			result.addSummaryEntry("Misspelled words: " + bTokens.size());
			if (DEBUG) System.out.println("Artifact tokens in doc: " + (dTokens.size() - bTokens.size()) + " (about)");
			result.addSummaryEntry("Artifact tokens: " + (dTokens.size() - bTokens.size()) + " (about)");
		}
		else {
			if (DEBUG) System.out.println("Misspelled words in doc: " + dTokens.size());
			result.addSummaryEntry("Misspelled words: " + dTokens.size());
			if (DEBUG) System.out.println("Missing tokens in doc: " + (bTokens.size() - bTokens.size()) + " (about)");
			result.addSummaryEntry("Missing tokens: " + (bTokens.size() - dTokens.size()) + " (about)");
		}
		
		//	transform document
		tokenIndex = 0;
		while ((tokenIndex < dDoc.size()) || (tokenIndex < benchmark.size())) {
			if (tokenIndex == dDoc.size()) {
				if (DEBUG) System.out.println(tokenIndex + " - Appending '" + benchmark.valueAt(tokenIndex) + "'");
				dDoc.addTokens(benchmark.valueAt(tokenIndex));
				tokenIndex++;
			}
			else if (tokenIndex == benchmark.size()) {
				if (DEBUG) System.out.println(tokenIndex + " - Cutting '" + dDoc.valueAt(tokenIndex) + "'");
				dTokens.remove(dDoc.valueAt(tokenIndex));
				dDoc.removeTokensAt(tokenIndex, 1);
			}
			else if (dDoc.valueAt(tokenIndex).equals(benchmark.valueAt(tokenIndex))) {
				if (DEBUG) System.out.println(tokenIndex + " - Keeping '" + dDoc.valueAt(tokenIndex) + "'");
				tokenIndex++;
			}
			else if (dTokens.contains(dDoc.valueAt(tokenIndex)) && bTokens.contains(benchmark.valueAt(tokenIndex))) {
				String dv = dDoc.valueAt(tokenIndex);
				String bv = benchmark.valueAt(tokenIndex);
				
				if (dv.toLowerCase().startsWith(bv.toLowerCase()) && (bv.length() < dv.length())) {
					if (DEBUG) System.out.println(tokenIndex + " - Transforming (cut/insert) '" + dDoc.valueAt(tokenIndex) + "' to '" + bv + "'");
					String rest = dv.substring(bv.length());
					dTokens.remove(dDoc.valueAt(tokenIndex));
					
					if (bTokens.contains(rest)) bTokens.remove(rest);
					else dTokens.addElement(rest);
					
					bTokens.remove(benchmark.valueAt(tokenIndex));
					dDoc.insertTokensAt(benchmark.valueAt(tokenIndex), tokenIndex);
					tokenIndex++;
					dDoc.setValueAt(rest, tokenIndex);
				}
				else if (((tokenIndex + 1) < dDoc.size()) && bv.toLowerCase().startsWith(dv.toLowerCase() + dDoc.valueAt(tokenIndex + 1).toLowerCase())) {
					if (DEBUG) System.out.println(tokenIndex + " - Transforming (append/remove) '" + dDoc.valueAt(tokenIndex) + "' to '" + bv + "'");
					String newDv = dv + dDoc.valueAt(tokenIndex + 1);
					dTokens.remove(dv);
					dTokens.remove(dDoc.valueAt(tokenIndex + 1));
					
					if (bTokens.contains(newDv)) bTokens.remove(newDv);
					else dTokens.addElement(newDv);
					
					dDoc.setValueAt(newDv, tokenIndex);
					dDoc.removeTokensAt(tokenIndex + 1, 1);
				}
				else {
					if (DEBUG) System.out.println(tokenIndex + " - Transforming '" + dDoc.valueAt(tokenIndex) + "' to '" + benchmark.valueAt(tokenIndex) + "'");
					dTokens.remove(dDoc.valueAt(tokenIndex));
					bTokens.remove(bv);
					dDoc.setValueAt(benchmark.valueAt(tokenIndex), tokenIndex);
					tokenIndex++;
				}
			}
			else if (((tokenIndex + 1) < dDoc.size()) && ((tokenIndex + 1) < benchmark.size()) && dDoc.valueAt(tokenIndex + 1).equals(benchmark.valueAt(tokenIndex + 1))) {
				if (DEBUG) System.out.println(tokenIndex + " - Transforming (for lookahead)'" + dDoc.valueAt(tokenIndex) + "' to '" + benchmark.valueAt(tokenIndex) + "'");
				if (dTokens.contains(dDoc.valueAt(tokenIndex))) dTokens.remove(dDoc.valueAt(tokenIndex));
				else bTokens.addElement(dDoc.valueAt(tokenIndex));
				if (bTokens.contains(benchmark.valueAt(tokenIndex))) bTokens.remove(benchmark.valueAt(tokenIndex));
				else dTokens.addElement(benchmark.valueAt(tokenIndex));
				
				dDoc.setValueAt(benchmark.valueAt(tokenIndex), tokenIndex);
				tokenIndex++;
			}
			else if (bTokens.contains(benchmark.valueAt(tokenIndex))) {
				if (DEBUG) System.out.println(tokenIndex + " - Inserting '" + benchmark.valueAt(tokenIndex) + "'");
				dDoc.insertTokensAt(benchmark.valueAt(tokenIndex), tokenIndex);
				bTokens.remove(benchmark.valueAt(tokenIndex));
				tokenIndex++;
			}
			else if (dTokens.contains(dDoc.valueAt(tokenIndex))) {
				if (DEBUG) System.out.println(tokenIndex + " - Removing '" + dDoc.valueAt(tokenIndex) + "'");
				dTokens.remove(dDoc.valueAt(tokenIndex));
				dDoc.removeTokensAt(tokenIndex, 1);
			}
			else {
				if (DEBUG) System.out.println(tokenIndex + " - Funny this ... FUCK! dDoc is at '" + dDoc.valueAt(tokenIndex) + "', bDoc is at '" + benchmark.valueAt(tokenIndex) + "'");
				
				//	re-synchronize docs
				int offset = 0;
				while ((offset != -1) && (((tokenIndex + offset) < benchmark.size()) || ((tokenIndex + offset) < dDoc.size()))) {
					
					int dLookahead = 1;
					while (((tokenIndex + offset + dLookahead) < dDoc.size()) && !dDoc.valueAt(tokenIndex + offset + dLookahead).equals(benchmark.valueAt(tokenIndex + offset))) {
						dLookahead ++;
					}
					if (DEBUG) System.out.println("    - dLookahead is " + dLookahead);
					int bLookahead = 1;
					while (((tokenIndex + offset + bLookahead) < benchmark.size()) && !benchmark.valueAt(tokenIndex + offset + bLookahead).equals(dDoc.valueAt(tokenIndex + offset))) {
						bLookahead ++;
					}
					if (DEBUG) System.out.println("    - bLookahead is " + bLookahead);
					
					if (dLookahead < bLookahead) {
						while (dLookahead > 0) {
							if (DEBUG) System.out.println("    - Removing '" + dDoc.valueAt(tokenIndex) + "'");
							if (dTokens.contains(dDoc.valueAt(tokenIndex))) dTokens.remove(dDoc.valueAt(tokenIndex));
							else bTokens.addElement(dDoc.valueAt(tokenIndex));
							dDoc.removeTokensAt(tokenIndex, 1);
							dLookahead --;
						}
						offset = -1;
					}
					else if (dLookahead > bLookahead) {
						while (bLookahead > 0) {
							if (DEBUG) System.out.println("    - Inserting '" + benchmark.valueAt(tokenIndex) + "'");
							if (bTokens.contains(benchmark.valueAt(tokenIndex))) bTokens.remove(benchmark.valueAt(tokenIndex));
							else dTokens.addElement(benchmark.valueAt(tokenIndex));
							dDoc.insertTokensAt(benchmark.valueAt(tokenIndex), (tokenIndex));
							bLookahead --;
							tokenIndex++;
						}
						offset = -1;
					}
					else {
						offset ++;
						if (DEBUG) System.out.println("    - HELL, equal lookaheads, setting search offset to " + offset);
					}
				}
			}
		}
		
		if (DEBUG) System.out.println("Remaining unique in doc:\n - " + dTokens.concatStrings("\n - "));
		if (DEBUG) System.out.println("Remaining unique in benchmark:\n - " + bTokens.concatStrings("\n - "));
		
		for (int t = 0; t < dDoc.size(); t++)
			dTokens.addElement(dDoc.valueAt(t));
		for (int t = 0; t < benchmark.size(); t++)
			bTokens.addElement(benchmark.valueAt(t));
		
		//	get unique values (eliminate matching ones)
		sharedTokens = dTokens.intersect(bTokens);
		for (int s = 0; s < sharedTokens.size(); s++) {
			String shared = sharedTokens.get(s);
			while (dTokens.contains(shared) && bTokens.contains(shared)) {
				bTokens.remove(shared);
				dTokens.remove(shared);
			}
		}
		
		if (DEBUG) System.out.println("Control: Remaining unique in doc:\n - " + dTokens.concatStrings("\n - "));
		if (DEBUG) System.out.println("Control: Remaining unique in benchmark:\n - " + bTokens.concatStrings("\n - "));
		
		for (int a = 0; a < annotationTypes.length; a++) {
			Benchmark b = Benchmark.computeBenchmark(dDoc.getAnnotations(annotationTypes[a]), benchmark.getAnnotations(annotationTypes[a]), new StringVector());
			result.addSummaryEntry("The annotations of type '" + annotationTypes[a] + "' are "
					+ ((int) (b.getFMeasureStrict() * 100)) + "% (P:" + ((int) (b.getPrecisionStrict() * 100)) + "%, R:" + ((int) (b.getRecallStrict() * 100)) + "%) fully correct, "
					+ ((int) (b.getFMeasureLenient() * 100)) + "% (P:" + ((int) (b.getPrecisionLenient() * 100)) + "%, R:" + ((int) (b.getRecallLenient() * 100)) + "%) partially correct.");
			result.addBenchmark(b, annotationTypes[a]);
		}
		return result;
	}
	
//	private static MutableAnnotation copyDocument(MutableAnnotation data) {
//		
//		//	catch bad parameters
//		if (data == null) return null;
//		
//		//	copy Tokens
//		TokenizedMutableCharSequence copyTokens = new TokenizedMutableCharSequence(data.getTokenizer());
//		for (int t = 0; t < data.size(); t++) {
//			Token token = data.tokenAt(t);
//			Token copyToken = new Token(token.getValue());
//			copyToken.setWhitespace(token.getWhitespace());
//			copyToken.setParagraphEnd(token.isParagraphEnd());
//			copyTokens.addToken(copyToken);
//		}
//		
//		//	copy markup
//		MutableAnnotation copy = new Document(copyTokens);
//		Annotation[] annotations = data.getAnnotations();
//		for (int a = 0; a < annotations.length; a++) {
//			if (!DocumentRoot.DOCUMENT_TYPE.equals(annotations[a].getType()))
//				copy.addAnnotation(annotations[a]);
//		}
//		
//		//	return copied document
//		return copy;
//	}
//	
	private static boolean isWordDevided(Token firstToken, Token secondToken) {
		if (firstToken == null) return false;
		if (firstToken.length() < 2) return false;
		
		String firstValue = firstToken.getValue();
		boolean is = (firstValue.endsWith("-") && (Gamta.LOWER_CASE_LETTERS.indexOf(firstValue.charAt(firstValue.length() - 2)) != -1) && isWordContinued(secondToken));
		return is;
	}
	
	private static boolean isWordContinued(Token token) {
		if (token == null) return false;
		return (Gamta.isWord(token.toString()) && !token.equals("and") && !token.equals("or"));
	}
}
