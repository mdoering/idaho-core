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


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * This class provides the same logic as used by the GATE AnnotationDiffer tool,
 * adjusted to the GAMTA data model
 */
public class Benchmark {

	/**
	 * Type for correct pairings (when the benchmark Annotation and Annotation
	 * match completely)
	 */
	public static final int CORRECT_TYPE = 1;

	/**
	 * Type for partially correct pairings (when the benchmark Annotation and
	 * Annotation match in type and significant features but the spans are just
	 * overlapping and not identical.
	 */
	public static final int PARTIALLY_CORRECT_TYPE = 2;

	/**
	 * Type for spurious pairings (where the Annotation is not matching any
	 * benchmark Annotation).
	 */
	public static final int SPURIOUS_TYPE = 3;

	/**
	 * Type for missing pairings (where the benchmark Annotation was not matched
	 * to an Annotation).
	 */
	public static final int MISSING_TYPE = 4;

	/**
	 * Score for a correct pairing.
	 */
	public static final int CORRECT = 2;

	/**
	 * Score for a partially correct pairing.
	 */
	public static final int PARTIALLY_CORRECT = 1;

	/**
	 * Score for a wrong (missing or spurious) pairing.
	 */
	public static final int WRONG = 0;

	/**
	 * The set of significant attributes used for matching.
	 */
	private StringVector relevantAttributes = null;

	private int correctMatches = 0;

	private int partiallyCorrectMatches = 0;

	private int missing = 0;

	private int spurious = 0;

	private Annotation[] benchmarks;

	private Annotation[] annotations;

	// lists representing all possible choices for each benchmark Annotation
	private ArrayList[] benchmarkPairings;

	// lists representing all possible choices for each annotation
	private ArrayList[] annotationPairings;

	// All the posible choices are added to this list for easy iteration.
	private ArrayList possiblePairings = new ArrayList();

	// A list with the choices selected for the best result.
	private ArrayList finalPairings = new ArrayList();

	private HashSet correctAnnotations = new HashSet();

	private HashSet partiallyCorrectAnnotations = new HashSet();

	private HashSet missingAnnotations = new HashSet();

	private HashSet spuriousAnnotations = new HashSet();

	// problem list for sanity check error messages
	private StringVector sanityProblems = null;

	// lock default constructor
	private Benchmark() {
	}

	/**
	 * Constructor
	 * @param annotations the Annotations to evaluate
	 * @param benchmark the Annotations to use as the benchmark for evaluation
	 * @param relevantAttributes a Set containing the names of the attributes to
	 *            consider when comparing Annotations (specifying null will
	 *            consider all attributes)
	 */
	private Benchmark(Annotation[] annotations, Annotation[] benchmark, StringVector relevantAttributes) {
		this.benchmarks = benchmark;
		this.annotations = annotations;
		this.relevantAttributes = relevantAttributes;
		
		this.benchmarkPairings = new ArrayList[this.benchmarks.length];
		for (int k = 0; k < this.benchmarkPairings.length; k++) this.benchmarkPairings[k] = null;
		this.annotationPairings = new ArrayList[this.annotations.length];
		for (int r = 0; r < this.annotationPairings.length; r++) this.annotationPairings[r] = null;
		
		evaluate(this);
	}
	
	/**	check if two Annotations have the same start index and size
	 */
	private static boolean isCoextensive(Annotation benchmark, Annotation annotation) {
		return ((benchmark.getStartIndex() == annotation.getStartIndex()) && (benchmark.size() == annotation.size()));
	}
	
	/**	check if two Annotations have the same start index, size, and attribute values
	 */
	private static boolean isCompatible(Annotation benchmark, Annotation annotation, StringVector attributeNames) {
		if (!isCoextensive(benchmark, annotation)) return false;
		return isAttributesCompatible(benchmark, annotation, attributeNames);
	}
	
	/**	check if two Annotations overlap and have the same attribute values
	 */
	private static boolean isPartiallyCompatible(Annotation benchmark, Annotation annotation, StringVector attributeNames) {
		if (AnnotationUtils.overlaps(benchmark, annotation)) return false;
		return isAttributesCompatible(benchmark, annotation, attributeNames);
	}
	
	/**	check if two Annotations have the same attribute values
	 */
	private static boolean isAttributesCompatible(Annotation benchmark, Annotation annotation, StringVector attributeNames) {
		
		//	collect attribute names
		StringVector benchmarkAttrNames = new StringVector();
		benchmarkAttrNames.addContent(benchmark.getAttributeNames());
		StringVector annotationAttrNames = new StringVector();
		annotationAttrNames.addContent(annotation.getAttributeNames());
		
		//	compare attribute values
		for (int a = 0; a < benchmarkAttrNames.size(); a++) {
			String attrName = benchmarkAttrNames.get(a);
			if ((attributeNames == null) || attributeNames.containsIgnoreCase(attrName)) {
				if (!annotationAttrNames.contains(attrName)) return false;
				else {
					Object benchmarkAttr = benchmark.getAttribute(attrName);
					Object annotationAttr = annotation.getAttribute(attrName);
					if ((benchmarkAttr == null) && (annotationAttr != null)) return false;
					else if ((benchmarkAttr != null) && !benchmarkAttr.equals(annotationAttr)) return false;
				}
			}
		}
		
		//	all comparisons successful
		return true;
	}
	
	/** evaluate a Benchmark
	 * @param	bm	the Benchmark to evaluate
	 */
	private static void evaluate(Benchmark bm) {
		
		//	check if multiple Annotation types present
		boolean oneType = true;
		String lastType = null;
		for (int b = 0; oneType && (b < bm.benchmarks.length); b++) {
			Annotation benchmark = bm.benchmarks[b];
			if (lastType == null) lastType = benchmark.getType();
			else if (!lastType.equalsIgnoreCase(benchmark.getType())) oneType = false;
		}
		lastType = null;
		for (int a = 0; oneType && (a < bm.annotations.length); a++) {
			Annotation annotation = bm.annotations[a];
			if (lastType == null) lastType = annotation.getType();
			else if (!lastType.equalsIgnoreCase(annotation.getType())) oneType = false;
		}
		
		// generate all possible pairings
		for (int b = 0; b < bm.benchmarks.length; b++) {
			for (int a = 0; a < bm.annotations.length; a++) {
				Annotation benchmark = bm.benchmarks[b];
				Annotation annotation = bm.annotations[a];
				
				//	create only pairings of Annotations of equal type if multiple types present
				if  (oneType || benchmark.getType().equalsIgnoreCase(annotation.getType())) {
					Pairing pairing = null;
					
					// full overlap -> CORRECT or WRONG
					if (isCoextensive(benchmark, annotation)) {
						
						// full match
						if (isCompatible(benchmark, annotation, bm.relevantAttributes))
							pairing = new Pairing(bm, b, a, CORRECT);
						
						// the two annotations are coextensive but don't match: mismatch
						else pairing = new Pairing(bm, b, a, WRONG);
					}
					
					// partial overlap -> PARTIALLY_CORRECT or WRONG
					else if (AnnotationUtils.overlaps(benchmark, annotation)) {
						if (isPartiallyCompatible(benchmark, annotation, bm.relevantAttributes)) pairing = new Pairing(bm, b, a, PARTIALLY_CORRECT);
						else pairing = new Pairing(bm, b, a, WRONG);
					}

					// add the new pairing if any
					if (pairing != null) {
						if (bm.benchmarkPairings[b] == null) bm.benchmarkPairings[b] = new ArrayList();
						bm.benchmarkPairings[b].add(pairing);
						if (bm.annotationPairings[a] == null) bm.annotationPairings[a] = new ArrayList();
						bm.annotationPairings[a].add(pairing);
						bm.possiblePairings.add(pairing);
					}
				}
			}
		}
		
		// from all possible pairings, find the maximal set that also maximises the total score
		Collections.sort(bm.possiblePairings, new PairingScoreComparator());
		Collections.reverse(bm.possiblePairings);
		
		//	do greedy evaluation, always keep current top pairing
		while (!bm.possiblePairings.isEmpty()) {
			Pairing bestPairing = (Pairing) bm.possiblePairings.remove(0);
			bm.consumePairing(bestPairing);
			bm.finalPairings.add(bestPairing);
			switch (bestPairing.type) {
				case CORRECT: {
					bm.correctAnnotations.add(bestPairing.getAnnotation());
					bm.correctMatches++;
					break;
				}
				case PARTIALLY_CORRECT: {
					bm.partiallyCorrectAnnotations.add(bestPairing.getAnnotation());
					bm.partiallyCorrectMatches++;
					break;
				}
				case WRONG: {
					if (bestPairing.getBenchmark() != null) {
						// missed benchmark Annotation
						bm.missingAnnotations.add(bestPairing.getBenchmark());
						bm.missing++;
					}
					if (bestPairing.getAnnotation() != null) {
						// spurious annotation
						bm.spuriousAnnotations.add(bestPairing.getAnnotation());
						bm.spurious++;
					}
					break;
				}
				default: throw new RuntimeException("Invalid pairing type: " + bestPairing.type);
			}
		}
		
		// add choices for the incorrect matches (MISSED, SPURIOUS) get the unmatched benchmark Annotations
		for (int b = 0; b < bm.benchmarkPairings.length; b++) {
			ArrayList list = bm.benchmarkPairings[b];
			if (list == null || list.isEmpty()) {
				bm.missingAnnotations.add(bm.benchmarks[b]);
				bm.finalPairings.add(new Pairing(bm, b, -1, WRONG));
				bm.missing++;
			}
		}
		
		// get the unmatched annotations
		for (int a = 0; a < bm.annotationPairings.length; a++) {
			ArrayList list = bm.annotationPairings[a];
			if (list == null || list.isEmpty()) {
				bm.spuriousAnnotations.add(bm.annotations[a]);
				bm.finalPairings.add(new Pairing(bm, -1, a, WRONG));
				bm.spurious++;
			}
		}
	}
	
	/** perform some basic checks over the internal data structures of a Benchmark
	 */
	private static void sanityCheck(Benchmark ad) {
		
		//	check already performed
		if (ad.sanityProblems != null)
			return;
		ad.sanityProblems = new StringVector();
		
		// all benchmark Annotations and annotations should have at most one choice left
		for (int b = 0; b < ad.benchmarkPairings.length; b++) {
			ArrayList pairings = ad.benchmarkPairings[b];
			if (pairings == null)
				continue;
			if (pairings.size() > 1)
				ad.sanityProblems.addElement("Multiple choices found!");
			else if (!pairings.isEmpty()) {
				
				// size must be 1
				Pairing pairing = ((Pairing) pairings.get(0));
				
				// the SAME choice should be found for the associated annotation
				ArrayList otherPairings = ad.annotationPairings[pairing.annotationIndex];
				if (otherPairings == null || otherPairings.size() != 1 || otherPairings.get(0) != pairing)
					ad.sanityProblems.addElement("Reciprocity error!");
			}
		}
		
		for (int a = 0; a < ad.annotationPairings.length; a++) {
			ArrayList pairings = ad.annotationPairings[a];
			if (pairings == null)
				continue;
			if (pairings.size() > 1)
				ad.sanityProblems.addElement("Multiple choices found!");
			else if (!pairings.isEmpty()) {
				
				// size must be 1
				Pairing pairing = ((Pairing) pairings.get(0));
				
				// the SAME choice should be found for the associated annotation
				ArrayList otherPairings = ad.benchmarkPairings[pairing.benchmarkIndex];
				if (otherPairings == null)
					ad.sanityProblems.addElement("Reciprocity error : null!");
				else if (otherPairings.size() != 1)
					ad.sanityProblems.addElement("Reciprocity error: not 1!");
				else if (otherPairings.get(0) != pairing)
					ad.sanityProblems.addElement("Reciprocity error: different!");
			}
		}
	}
	
	/**
	 * remove all mutually exclusive OTHER choices possible from the data
	 * structures. <tt>pairing</tt> gets removed from
	 * {@link #possiblePairings} as well.
	 */
	private void consumePairing(Pairing pairing) {
		this.possiblePairings.remove(pairing);

		ArrayList sameKeyPairings = this.benchmarkPairings[pairing.benchmarkIndex];
		sameKeyPairings.remove(pairing);
		this.possiblePairings.removeAll(sameKeyPairings);

		ArrayList sameResponsePairings = this.annotationPairings[pairing.annotationIndex];
		sameResponsePairings.remove(pairing);
		this.possiblePairings.removeAll(sameResponsePairings);

		Iterator iter = new ArrayList(sameKeyPairings).iterator();
		while (iter.hasNext())
			this.removePairing((Pairing) iter.next());

		iter = new ArrayList(sameResponsePairings).iterator();
		while (iter.hasNext())
			this.removePairing((Pairing) iter.next());

		sameKeyPairings.add(pairing);
		sameResponsePairings.add(pairing);
	}

	/*
	 * remove a Pairing from the two lists it belongs to
	 */
	private void removePairing(Pairing pairing) {
		this.benchmarkPairings[pairing.benchmarkIndex].remove(pairing);
		this.annotationPairings[pairing.annotationIndex].remove(pairing);
	}

	/**
	 * @return an array containing sanity problems (will perform sanity check if
	 *         not done yet)
	 */
	public String[] getSanityProblems() {
		if (this.sanityProblems == null)
			sanityCheck(this);
		return this.sanityProblems.toStringArray();
	}

	/**
	 * @return the strict precision (the ratio of correct annotations out of all
	 *         the provided annotations).
	 */
	public double getPrecisionStrict() {
		if (this.annotations.length == 0)
			return 1.0;
		return (((double) this.correctMatches) / ((double) this.annotations.length));
	}

	/**
	 * @return the strict recall (the ratio of benchmark Annotation matched to a
	 *         annotation out of all the benchmark Annotations).
	 */
	public double getRecallStrict() {
		if (this.benchmarks.length == 0)
			return 1.0;
		return (((double) this.correctMatches) / ((double) this.benchmarks.length));
	}

	/**
	 * @return the lenient precision (where the partial matches are considered
	 *         as correct).
	 */
	public double getPrecisionLenient() {
		if (this.annotations.length == 0)
			return 1.0;
		return (((double) (this.correctMatches + this.partiallyCorrectMatches)) / this.annotations.length);
	}

	/**
	 * @return the average of the strict and lenient precision values.
	 */
	public double getPrecisionAverage() {
		return ((getPrecisionLenient() + getPrecisionStrict()) / 2.0);
	}

	/**
	 * @return the lenient recall (where the partial matches are considered as
	 *         correct).
	 */
	public double getRecallLenient() {
		if (this.benchmarks.length == 0) return 1.0;
		return (((double) (this.correctMatches + this.partiallyCorrectMatches)) / this.benchmarks.length);
	}

	/**
	 * @return the average of the strict and lenient recall values.
	 */
	public double getRecallAverage() {
		return ((getRecallLenient() + getRecallStrict()) / 2.0);
	}

	/**
	 * Gets the strict f-Measure (the harmonic weighted mean of the strict
	 * precision and the strict recall) using the provided parameter as relative
	 * weight.
	 * @return the strict f-Measure
	 */
	public double getFMeasureStrict() {
		return this.getFMeasureStrict(1);
	}

	/**
	 * Gets the strict f-Measure (the harmonic weighted mean of the strict
	 * precision and the strict recall) using the provided parameter as relative
	 * weight.
	 * @param beta The relative weight of precision and recall. A value of 1
	 *            gives equal weights to precision and recall. A value of 0
	 *            takes the recall value completely out of the equation.
	 * @return the strict f-Measure
	 */
	public double getFMeasureStrict(double beta) {
		double precision = getPrecisionStrict();
		double recall = getRecallStrict();
		double betaSq = beta * beta;
		double fScore = (((betaSq + 1) * precision * recall) / (betaSq * precision + recall));
		if (Double.isNaN(fScore)) fScore = 0.0;
		return fScore;
	}

	/**
	 * Gets the lenient f-Measure (f-Measure where the lenient precision and
	 * recall values are used) using the provided parameter as relative weight.
	 * @return the lenient f-Measure
	 */
	public double getFMeasureLenient() {
		return this.getFMeasureLenient(1);
	}

	/**
	 * Gets the lenient f-Measure (f-Measure where the lenient precision and
	 * recall values are used) using the provided parameter as relative weight.
	 * @param beta The relative weight of precision and recall. A value of 1
	 *            gives equal weights to precision and recall. A value of 0
	 *            takes the recall value completely out of the equation.
	 * @return the lenient f-Measure
	 */
	public double getFMeasureLenient(double beta) {
		double precision = getPrecisionLenient();
		double recall = getRecallLenient();
		double betaSq = beta * beta;
		double fScore = (((betaSq + 1) * precision * recall) / (betaSq * precision + recall));
		if (Double.isNaN(fScore)) fScore = 0.0;
		return fScore;
	}
	
	/** Gets the average of strict and lenient f-Measure values.
	 * @return the average of strict and lenient f-Measure
	 */
	public double getFMeasureAverage() {
		return this.getFMeasureAverage(1);
	}
	
	/** Gets the average of strict and lenient f-Measure values.
	 * @param beta The relative weight of precision and recall. A value of 1
	 *            gives equal weights to precision and recall. A value of 0
	 *            takes the recall value completely out of the equation.
	 * @return the average of strict and lenient f-Measure
	 */
	public double getFMeasureAverage(double beta) {
		return ((getFMeasureLenient(beta) + getFMeasureStrict(beta)) / 2.0);
	}
	
	/** @return the number of correct matches.
	 */
	public int getCorrectMatches() {
		return this.correctMatches;
	}
	
	/** @return the number of partially correct matches.
	 */
	public int getPartiallyCorrectMatches() {
		return this.partiallyCorrectMatches;
	}
	
	/** @return the number of pairings of type {@link #MISSING_TYPE}.
	 */
	public int getMissing() {
		return this.missing;
	}
	
	/** @return the number of pairings of type {@link #SPURIOUS_TYPE}.
	 */
	public int getSpurious() {
		return this.spurious;
	}
	
	/** @return the number of pairings of type {@link #SPURIOUS_TYPE}.
	 */
	public int getFalsePositivesStrict() {
		return (this.annotations.length - this.correctMatches);
	}
	
	/** @return the number of annotations that aren't either correct or partially correct.
	 */
	public int getFalsePositivesLenient() {
		return (this.annotations.length - this.correctMatches - this.partiallyCorrectMatches);
	}
	
	/** @return the number of benchmark Annotations provided.
	 */
	public int getBemchmarkCount() {
		return this.benchmarks.length;
	}
	
	/** @return the number of annotations provided.
	 */
	public int getAnnotationCount() {
		return this.annotations.length;
	}
	
	/**	@return	this Benchmark's incorrect pairings as a String
	 */
	public String getMissmatches() {
		StringWriter sw = new StringWriter();
		try {
			this.writeMissmatches(sw);
		} catch (IOException e) {}
		return sw.toString();
	}
	
	/** print to System.out the pairings that are not correct.
	 */
	public void printMissmatches() {
		try {
			this.writeMissmatches(new PrintWriter(System.out));
		} catch (IOException e) {}
	}
	
	/** write the pairings that are not correct.
	 */
	public void writeMissmatches(String fileName) throws IOException {
		this.writeMissmatches(new File(fileName));
	}
	
	/** write the pairings that are not correct.
	 */
	public void writeMissmatches(File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		this.writeMissmatches(fw);
		fw.close();
	}
	
	/** write the pairings that are not correct.
	 */
	public void writeMissmatches(OutputStream out) throws IOException {
		this.writeMissmatches(new OutputStreamWriter(out));
	}
	
	/** write the pairings that are not correct.
	 */
	public void writeMissmatches(Writer writer) throws IOException {
		
		//	create writer
		BufferedWriter out = new BufferedWriter(writer);
		
		HashSet benchmarkType = new HashSet();
		if (this.benchmarks.length != 0) benchmarkType.add(this.benchmarks[0].getType());
		HashSet annotationType = new HashSet();
		if (this.annotations.length != 0) annotationType.add(this.annotations[0].getType());
		
		// get the partial correct matches
		Iterator iter = this.finalPairings.iterator();
		while (iter.hasNext()) {
			Pairing pairing = (Pairing) iter.next();
			if (pairing.type == PARTIALLY_CORRECT) {
				out.newLine();
				out.write("Missmatch (partially correct):");
				out.newLine();
				
				if (this.benchmarks[pairing.benchmarkIndex] instanceof QueriableAnnotation)
					out.write("Benchmark: " + AnnotationUtils.toXML(((QueriableAnnotation) this.benchmarks[pairing.benchmarkIndex]), benchmarkType));
				else out.write("Benchmark: " + this.benchmarks[pairing.benchmarkIndex].toXML());
				out.newLine();
				
				if (this.annotations[pairing.annotationIndex] instanceof QueriableAnnotation)
					out.write("Annotation: " + AnnotationUtils.toXML(((QueriableAnnotation) this.annotations[pairing.annotationIndex]), annotationType));
				else out.write("Annotation: " + this.annotations[pairing.annotationIndex].toXML());
				out.newLine();
			}
		}
		
		// get the unmatched benchmark Annotations
		for (int b = 0; b < this.benchmarkPairings.length; b++) {
			ArrayList list = this.benchmarkPairings[b];
			if (list == null || list.isEmpty()) {
				out.newLine();
				if (this.benchmarks[b] instanceof QueriableAnnotation)
					out.write("Missed Benchmark: " + AnnotationUtils.toXML(((QueriableAnnotation) this.benchmarks[b]), benchmarkType));
				else out.write("Missed Benchmark: " +  this.benchmarks[b].toXML());
				out.newLine();
			}
		}
		
		// get the unmatched annotations
		for (int a = 0; a < this.annotationPairings.length; a++) {
			ArrayList list = this.annotationPairings[a];
			if (list == null || list.isEmpty()) {
				out.newLine();
				if (this.annotations[a] instanceof QueriableAnnotation)
					out.write("Spurious Annotation: " + AnnotationUtils.toXML(((QueriableAnnotation) this.annotations[a]), annotationType));
				else out.write("Spurious Annotation: " + this.annotations[a].toXML());
				out.newLine();
			}
		}
		
		//	flush writer
		out.flush();
	}
	
	/**	@return	this Benchmark's incorrect pairings as HTML
	 */
	public String getMissmatchesHTML() {
		StringWriter sw = new StringWriter();
		try {
			this.writeMissmatchesHTML(sw);
		} catch (IOException e) {}
		return sw.toString();
	}
	
	/** write the pairings that are not correct as HTML
	 */
	public void writeMissmatchesHTML(String fileName) throws IOException {
		this.writeMissmatchesHTML(new File(fileName));
	}
	
	/** write the pairings that are not correct as HTML
	 */
	public void writeMissmatchesHTML(File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		this.writeMissmatchesHTML(fw);
		fw.close();
	}
	
	/** write the pairings that are not correct as HTML
	 */
	public void writeMissmatchesHTML(OutputStream out) throws IOException {
		this.writeMissmatchesHTML(new OutputStreamWriter(out));
	}
	
	/** write the pairings that are not correct as an HTML table
	 */
	public void writeMissmatchesHTML(Writer writer) throws IOException {
		
		//	create writer & open table
		BufferedWriter out = new BufferedWriter(writer);
		out.write("<table>");
		out.newLine();
		
		HashSet benchmarkType = new HashSet();
		if (this.benchmarks.length != 0) benchmarkType.add(this.benchmarks[0].getType());
		HashSet annotationType = new HashSet();
		if (this.annotations.length != 0) annotationType.add(this.annotations[0].getType());
		
		// get the partial correct matches
		Iterator iter = this.finalPairings.iterator();
		while (iter.hasNext()) {
			Pairing pairing = (Pairing) iter.next();
			if (pairing.type == PARTIALLY_CORRECT) {
				out.write("<tr><td><b>Missmatch (partially correct):</b><br>");
				out.newLine();
				
				if (this.benchmarks[pairing.benchmarkIndex] instanceof QueriableAnnotation)
					out.write("<b>Benchmark:</b> " + IoTools.prepareForHtml(AnnotationUtils.toXML(((QueriableAnnotation) this.benchmarks[pairing.benchmarkIndex]), benchmarkType)) + "<br>");
				else out.write("<b>Benchmark:</b> " + IoTools.prepareForHtml(this.benchmarks[pairing.benchmarkIndex].toXML()) + "<br>");
				out.newLine();
				
				if (this.annotations[pairing.annotationIndex] instanceof QueriableAnnotation)
					out.write("<b>Annotation:</b> " + IoTools.prepareForHtml(AnnotationUtils.toXML(((QueriableAnnotation) this.annotations[pairing.annotationIndex]), annotationType)) + "<br>");
				else out.write("<b>Annotation:</b> " + IoTools.prepareForHtml(this.annotations[pairing.annotationIndex].toXML()) + "<br>");
				out.newLine();
				
				out.write("</td></tr>");
				out.newLine();
			}
		}
		
		// get the unmatched benchmark Annotations
		for (int b = 0; b < this.benchmarkPairings.length; b++) {
			ArrayList list = this.benchmarkPairings[b];
			if (list == null || list.isEmpty()) {
				out.write("<tr><td>");
				out.newLine();
				
				if (this.benchmarks[b] instanceof QueriableAnnotation)
					out.write("<b>Missed Benchmark:</b> " + IoTools.prepareForHtml(AnnotationUtils.toXML(((QueriableAnnotation) this.benchmarks[b]), benchmarkType)) + "<br>");
				else out.write("<b>Missed Benchmark:</b> " + IoTools.prepareForHtml(this.benchmarks[b].toXML()) + "<br>");
				out.newLine();
				
				out.write("</td></tr>");
				out.newLine();
			}
		}
		
		// get the unmatched annotations
		for (int a = 0; a < this.annotationPairings.length; a++) {
			ArrayList list = this.annotationPairings[a];
			if (list == null || list.isEmpty()) {
				out.write("<tr><td>");
				out.newLine();
				
				if (this.annotations[a] instanceof QueriableAnnotation)
					out.write("<b>Spurious Annotation:</b> " + IoTools.prepareForHtml(AnnotationUtils.toXML(((QueriableAnnotation) this.annotations[a]), annotationType)) + "<br>");
				else out.write("<b>Spurious Annotation:</b> " + IoTools.prepareForHtml(this.annotations[a].toXML()) + "<br>");
				out.newLine();
				
				out.write("</td></tr>");
				out.newLine();
			}
		}
		
		//	close table & flush writer
		out.write("</table>");
		out.newLine();
		out.flush();
	}
	
	/**	@return	this Benchmark's statistics as a String
	 */
	public String getStatistics() {
		StringWriter sw = new StringWriter();
		try {
			this.writeStatistics(sw);
		} catch (IOException e) {}
		return sw.toString();
	}
	
	/** print to System.out the statistics
	 */
	public void printStatistics() {
		try {
			this.writeStatistics(new PrintWriter(System.out));
		} catch (IOException e) {}
	}
	
	/** write the statistics
	 */
	public void writeStatistics(String fileName) throws IOException {
		this.writeStatistics(new File(fileName));
	}
	
	/** write the statistics
	 */
	public void writeStatistics(File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		this.writeStatistics(fw);
		fw.close();
	}
	
	/** write the statistics
	 */
	public void writeStatistics(OutputStream out) throws IOException {
		this.writeStatistics(new OutputStreamWriter(out));
	}
	
	/** write the statistics
	 */
	public void writeStatistics(Writer writer) throws IOException {
		
		//	create writer
		BufferedWriter out = new BufferedWriter(writer);
		
		//	write statistics
		out.write("Test Result:");
		out.newLine();
		out.write(" - Base data: " + this.getAnnotationCount() + " Annotations, " + this.getBemchmarkCount() + " benchmark Annotations");
		out.newLine();
		out.write(" - Considered attributes: " + ((this.relevantAttributes == null) ? "All" : (this.relevantAttributes.isEmpty() ? "None" : this.relevantAttributes.concatStrings(", "))));
		out.newLine();
		out.write(" - Result numbers: " + this.getCorrectMatches() + " correct, " + this.getPartiallyCorrectMatches() + " partially correct, " + this.getSpurious() + " spurious, " + this.getMissing() + " missing");
		out.newLine();
		out.write(" - Strict fScore: " + this.getFMeasureStrict() + " (P: " + this.getPrecisionStrict() + ", R: " + this.getRecallStrict() + ")");
		out.newLine();
		out.write(" - Lenient fScore: " + this.getFMeasureLenient() + " (P: " + this.getPrecisionLenient() + ", R: " + this.getRecallLenient() + ")");
		out.newLine();
		out.write(" - Average fScore: " + this.getFMeasureAverage() + " (P: " + this.getPrecisionAverage() + ", R: " + this.getRecallAverage() + ")");
		out.newLine();
		out.write(" - Sanity problems: " + ((this.sanityProblems == null) ? "Not computed" : (this.sanityProblems.isEmpty() ? "None" : ("\n   - " + this.sanityProblems.concatStrings("\n   - ")))));
		out.newLine();
		
		//	flush writer
		out.flush();
	}
	
	/**	@return	this Benchmark's statistics as HTML
	 */
	public String getStatisticsHTML() {
		StringWriter sw = new StringWriter();
		try {
			this.writeStatisticsHTML(sw);
		} catch (IOException e) {}
		return sw.toString();
	}
	
	/** write the statistics as HTML
	 */
	public void writeStatisticsHTML(String fileName) throws IOException {
		this.writeStatisticsHTML(new File(fileName));
	}
	
	/** write the statistics as HTML
	 */
	public void writeStatisticsHTML(File file) throws IOException {
		FileWriter fw = new FileWriter(file);
		this.writeStatisticsHTML(fw);
		fw.close();
	}
	
	/** write the statistics as HTML
	 */
	public void writeStatisticsHTML(OutputStream out) throws IOException {
		this.writeStatisticsHTML(new OutputStreamWriter(out));
	}
	
	/** write the statistics as an HTML list
	 */
	public void writeStatisticsHTML(Writer writer) throws IOException {
		
		//	create writer & open table
		BufferedWriter out = new BufferedWriter(writer);
		out.write("<table><tr><td>");
		out.newLine();
		
		//	write statistics
		out.write("<b>Test Result:</b>");
		out.newLine();
		out.write("<ul>");
		out.newLine();
		out.write("<li><b>Base data:</b> " + this.getAnnotationCount() + " Annotations, " + this.getBemchmarkCount() + " benchmark Annotations</li>");
		out.newLine();
		out.write("<li><b>Considered attributes:</b> " + ((this.relevantAttributes == null) ? "All" : (this.relevantAttributes.isEmpty() ? "None" : this.relevantAttributes.concatStrings(", "))) + "</li>");
		out.newLine();
		out.write("<li><b>Result numbers:</b> " + this.getCorrectMatches() + " correct, " + this.getPartiallyCorrectMatches() + " partially correct, " + this.getSpurious() + " spurious, " + this.getMissing() + " missing</li>");
		out.newLine();
		out.write("<li><b>Strict fScore:</b> " + this.getFMeasureStrict() + " (P: " + this.getPrecisionStrict() + ", R: " + this.getRecallStrict() + ")</li>");
		out.newLine();
		out.write("<li><b>Lenient fScore:</b> " + this.getFMeasureLenient() + " (P: " + this.getPrecisionLenient() + ", R: " + this.getRecallLenient() + ")</li>");
		out.newLine();
		out.write("<li><b>Average fScore:</b> " + this.getFMeasureAverage() + " (P: " + this.getPrecisionAverage() + ", R: " + this.getRecallAverage() + ")</li>");
		out.newLine();
		out.write("<li><b>Sanity problems:</b> " + ((this.sanityProblems == null) ? "Not computed" : (this.sanityProblems.isEmpty() ? "None" : ("<ul><li>" + this.sanityProblems.concatStrings("</li><li>") + "</li></ul>"))) + "</li>");
		out.newLine();
		out.write("</ul>");
		out.newLine();
		
		//	close table & flush writer
		out.write("</td></tr></table>");
		out.newLine();
		out.flush();
	}
	
	/** @return the attributes considered significant for the matching algorithm.
	 */
	public StringVector getSignificantAttributesSet() {
		return relevantAttributes;
	}
	
	/** A method that returns specific type of annotations
	 * @param type (anything other than CORRECT_TYPE, PARTIALLY_CORRECT_TYPE, SPURIOUS_TYPE, or MISSING_TYPE will return an empty array)
	 * @return the Annotations of the specified type.
	 */
	public Annotation[] getAnnotationsOfType(int type) {
		ArrayList list;
		switch (type) {
			case CORRECT_TYPE: 
				list = new ArrayList(this.correctAnnotations);
				break;
			case PARTIALLY_CORRECT_TYPE: 
				list = new ArrayList(this.partiallyCorrectAnnotations);
				break;
			case SPURIOUS_TYPE: 
				list = new ArrayList(this.spuriousAnnotations);
				break;
			case MISSING_TYPE: 
				list = new ArrayList(this.missingAnnotations);
				break;
			default: list = new ArrayList();
		}
		Collections.sort(list);
		return ((Annotation[]) list.toArray(new Annotation[list.size()]));
	}
	
	/**	@return	the pairings the Benchmark's scores were computed from
	 */
	public Pairing[] getPairings() {
		return ((Pairing[]) this.finalPairings.toArray(new Pairing[finalPairings.size()]));
	}
	
	/** Represents a pairing of a benchmark annotation with a annotation annotation and the associated score for that pairing.
	 */
	public static class Pairing implements Comparable {
		private final Benchmark base;
		
		private final int benchmarkIndex;
		private final int annotationIndex;
		
		/**	the type of this Pairing (one of CORRECT, PARTIALLY_CORRECT and WRONG)
		 */
		public final int type;
		
		private int score;
		private boolean scoreCalculated = false;
		
		private Pairing(Benchmark base, int benchmarkIndex, int annotationIndex, int type) {
			this.base = base;
			this.benchmarkIndex = benchmarkIndex;
			this.annotationIndex = annotationIndex;
			this.type = type;
		}
		
		/**	@return	the score of this Pairing
		 */
		public int getScore() {
			if (!scoreCalculated) this.calculateScore();
			return score;
		}
		
		/**	@return	the benchmark Annotation of this Pairing
		 */
		public Annotation getBenchmark() {
			return ((benchmarkIndex == -1) ? null : this.base.benchmarks[benchmarkIndex]);
		}
		
		/**	@return	the Annotation of this Pairing
		 */
		public Annotation getAnnotation() {
			return ((annotationIndex == -1) ? null : this.base.annotations[annotationIndex]);
		}
		
		/** Calculates the score for this choice as: type - sum of all the types of all OTHER mutually exclusive choices
		 */
		private void calculateScore() {
			
			// this needs to be a set so we don't count conflicts twice
			HashSet conflictSet = new HashSet();
			
			// add all the choices from the same annotation annotation
			conflictSet.addAll(this.base.annotationPairings[annotationIndex]);
			
			// add all the choices from the same benchmark Annotation
			conflictSet.addAll(this.base.benchmarkPairings[benchmarkIndex]);
			
			// remove this choice from the conflict set
			conflictSet.remove(this);
			this.score = this.type;
			
			// compute score
			Iterator conflictIter = conflictSet.iterator();
			while (conflictIter.hasNext()) this.score -= ((Pairing) conflictIter.next()).type;
			
			//	remember score computed
			this.scoreCalculated = true;
		}
		
		/** @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		public int compareTo(Object o) {
			if ((o == null) || !(o instanceof Pairing)) return -1;
			Pairing p = ((Pairing) o);
			
			Annotation benchmark1 = this.getBenchmark();
			Annotation benchmark2 = p.getBenchmark();
			Annotation annotation1 = this.getAnnotation();
			Annotation annotation2 = p.getAnnotation();
			
			int start1 = (benchmark1 == null) ? -1 : benchmark1.getStartIndex();
			if (start1 == -1) start1 = annotation1.getStartIndex();
			int start2 = (benchmark2 == null) ? -1 : benchmark2.getStartIndex();
			if (start2 == -1) start2 = annotation2.getStartIndex();
			int result = (start1 - start2);
			
			//	if start indices equal, compare by type
			if (result == 0) result = p.type - this.type;

			return result;
		}
	}
	
	/** Compares two pairings: the better score is preferred; for the same score the better type is preferred (exact matches are preferred to partial ones).
	 */
	private static class PairingScoreComparator implements Comparator {
		
		/**	@see java.util.Comparator#compare(T, T)
		 */
		public int compare(Object o1, Object o2) {
			Pairing first = (Pairing) o1;
			Pairing second = (Pairing) o2;
			
			// compare by score
			int result = first.getScore() - second.getScore();
			
			// compare by type
			if (result == 0) result = first.type - second.type;
			
			// compare by completeness (a wrong match with both benchmark Annotation and annotation is "better" than one with only benchmark Annotation or annotation
			if (result == 0) result = ((first.getBenchmark() == null) ? 0 : 1) + 
						((first.getAnnotation() == null) ? 0 : 1) + 
						((second.getBenchmark() == null) ? 0 : -1) + 
						((second.getAnnotation() == null) ? 0 : -1);
			
			//	finally got the result
			return result;
		}
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
	 */
	public static Benchmark computeBenchmark(QueriableAnnotation doc, QueriableAnnotation benchmark, String annotationType, StringVector relevantAttributes) {
		return computeBenchmark(doc, benchmark, annotationType, annotationType, relevantAttributes);
	}

	/**
	 * evaluate the markup of a document against the markup of a benchmark
	 * document
	 * @param doc the DocumentPart to evaluate
	 * @param benchmark the DocumentPart to use as the benchmark for evaluation
	 * @param annotationType the type of the Annotations to evaluate
	 * @param benchmarkType the type of the Annotations to use as the benchmark
	 * @param relevantAttributes a Set containing the names of the attributes to
	 *            consider when comparing Annotations (specifying null will
	 *            consider all attributes)
	 */
	public static Benchmark computeBenchmark(QueriableAnnotation doc, QueriableAnnotation benchmark, String annotationType, String benchmarkType, StringVector relevantAttributes) {
		return computeBenchmark(doc.getAnnotations(annotationType), benchmark.getAnnotations(benchmarkType), relevantAttributes);
	}

	/**
	 * evaluate a set of Annotations against a set of benchmark Annotations
	 * @param annotations the Annotations to evaluate
	 * @param benchmark the Annotations to use as the benchmark for evaluation
	 * @param relevantAttributes a Set containing the names of the attributes to
	 *            consider when comparing Annotations (specifying null will
	 *            consider all attributes)
	 */
	public static Benchmark computeBenchmark(Annotation[] annotations, Annotation[] benchmark, StringVector relevantAttributes) {
		return new Benchmark(annotations, benchmark, relevantAttributes);
	}
}
