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
package de.uka.ipd.idaho.gamta.util.analyzers;


import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;

/**
 * @author sautter
 *
 */
public class SectionTagger extends AbstractAnalyzer {
	
	private static final int MAX_TITLE_TOKENS = 15;
	private static final int ENVIRONMENT_LENGHT_FACTOR = 3;
	
	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		if (paragraphs.length == 0) return;
		
		int sectionStart = 0;
		int sectionSize = 0;
		
		for (int p = 0; p < paragraphs.length; p++) {
			MutableAnnotation paragraph = paragraphs[p];
			if (this.isSectionTitle(paragraph) 
					&& ((p + 1) < paragraphs.length) 
					&& ((p == 0) || ((paragraph.size() * ENVIRONMENT_LENGHT_FACTOR) < paragraphs[p - 1].size()))
					&& ((paragraph.size() * ENVIRONMENT_LENGHT_FACTOR) < paragraphs[p + 1].size())
				) {
				data.addAnnotation(MutableAnnotation.SECTION_TYPE, sectionStart, sectionSize);
				sectionStart = paragraph.getStartIndex();
				sectionSize = paragraph.size();
			} else sectionSize += paragraph.size();
		}
		
		if (sectionSize != 0) data.addAnnotation(MutableAnnotation.SECTION_TYPE, sectionStart, sectionSize);
	}
	
	private boolean isSectionTitle(MutableAnnotation paragraph) {
		if (paragraph.size() > MAX_TITLE_TOKENS) return false;
		return (Gamta.hasWord(paragraph) && (Gamta.isTitleCase(paragraph) || Gamta.isAllUpperCase(paragraph)));
	}
}
