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
import java.util.Stack;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.AbstractAnalyzer;

public class SentenceTagger extends AbstractAnalyzer {
	
	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, Properties)
	 */
	public void process(MutableAnnotation data, Properties parameters) {
		MutableAnnotation[] paragraphs = data.getMutableAnnotations(MutableAnnotation.PARAGRAPH_TYPE);
		
		if (paragraphs.length != 0) 
			for (int p = 0; p < paragraphs.length; p++)
				this.process(paragraphs[p]);
		else this.process(data);
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.Analyzer#process(de.uka.ipd.idaho.gamta.MutableAnnotation, boolean)
	 */
	private void process(MutableAnnotation data) {
		
		//	catch empty data
		if (data.size() == 0) return;
		
		Token lastToken = null;
		Token token = null;
		Token nextToken = data.firstToken();
		
		int sentenceStart = 0;
		int sentenceSize = 0;
		
		Token lastOpenBracket = null;
		Stack openBrackets = new Stack();
		String quoter = null;
		
		for (int t = 1; t <= data.size(); t++) {
			lastToken = token;
			token = nextToken;
			nextToken = ((t < data.size()) ? data.tokenAt(t) : null);
			sentenceSize++;
			
			//	trace brackets
			if (Gamta.isOpeningBracket(token.toString())) {
				openBrackets.push(token);
				lastOpenBracket = token;
			}
			else if ((lastOpenBracket != null) && Gamta.closes(token.toString(), lastOpenBracket.toString())) {
				openBrackets.pop();
				lastOpenBracket = ((openBrackets.size() == 0) ? null : (Token) openBrackets.peek());
			}
			
			//	end of quotations
			if (token.equals(quoter)) {
//				if ((lastToken != null) && Gamta.isSentenceEnd(lastToken.toString())) {
				if ((lastToken != null) && Gamta.isSentenceEnd(lastToken.toString()) && !this.isSentenceContinued(nextToken)) {
					data.addAnnotation(MutableAnnotation.SENTENCE_TYPE, sentenceStart, sentenceSize);
					sentenceStart = t;
					sentenceSize = 0;
				}
				quoter = null;
			}
			
			//	start of quotations
			else if (token.equals("\"") || token.equals("'")) {
				quoter = token.getValue();
			}
			
			//	end of sentence
			else if (
					Gamta.isSentenceEnd(token.toString())
					&&
					(lastOpenBracket == null)
					&&
					(quoter == null)
					&&
					(sentenceSize != 0)
					&&
//					((nextToken == null) || !Gamta.isLowerCaseWord(nextToken.toString()))
					!this.isSentenceContinued(nextToken)
					) {
				data.addAnnotation(MutableAnnotation.SENTENCE_TYPE, sentenceStart, sentenceSize);
				sentenceStart = t;
				sentenceSize = 0;
			}
		}
		
		//	mark remaining part of data
		if (sentenceSize != 0) data.addAnnotation(MutableAnnotation.SENTENCE_TYPE, sentenceStart, sentenceSize);
	}
	
	private boolean isSentenceContinued(Token token) {
		if (token == null)
			return false;
		else if (token.length() == 0)
			return false;
		
		char c = token.charAt(0);
		
		//	lower case word, sentence continues
		if (('a' <= c) && (c <= 'z'))
			return true;
		
		//	upper case word, might start new sentence
		else if (('A' <= c) && (c <= 'Z'))
			return false;
		
		//	number, might start new sentence
		else if (('0' <= c) && (c <= '9'))
			return false;
		
		//	quoter, might start new sentence
		else if ((c == '\'') || (c == '"'))
			return false;
		
		//	opening bracket, might start new sentence
		else if (Gamta.isOpeningBracket(token))
			return false;
		
		//	other punctuation, usually continues sentence
		else return true;
	}
}
