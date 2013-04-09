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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableCharSequence;
import de.uka.ipd.idaho.gamta.MutableTokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;

public class RegExTokenizer implements Tokenizer {
	
	private Pattern pattern;
	private String regEx;
	
	/**	Constructor
	 * @param	regEx	the regular expression used for tokenization (must not contain parts matching any type of whitespace)
	 */
	public RegExTokenizer(String regEx) {
		if (regEx.indexOf("\\s") != -1)
			throw new IllegalArgumentException("The regular expression for tokenization must not contain parts matching any type of whitespace.");
		this.regEx = regEx;
		this.pattern = Pattern.compile(regEx);
	}
	
	/** @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return ((o != null) && o.toString().equals(this.toString()));
	}
	
	/** @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.regEx;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Tokenizer#getTokenIterator(java.lang.CharSequence)
	 */
	public TokenIterator getTokenIterator(CharSequence cs) {
		return new RegExTokenIterator(cs);
	}
	
	private class RegExTokenIterator extends Tokenizer.TokenIterator {
		protected RegExTokenIterator(CharSequence charData) {
			super(charData);
		}
		
		/** @see de.uka.ipd.idaho.gamta.Tokenizer.TokenIterator#tokenize(java.lang.CharSequence)
		 */
		protected int[] tokenize(CharSequence chars) {
			int offset = 0;
			int[] offsets = new int[1];
			int offsetCount = 0;
			Matcher matcher = RegExTokenizer.this.pattern.matcher(chars);
			while (matcher.find()) {
				/*
				 * extens offset array by one on overflow. while this approach
				 * incurs an array extension for every single token from the
				 * second on, more than one token in a whitespace-free block
				 * seldom occurs (~20%, for punctuation), and more than two are
				 * very rare.
				 * 
				 * TODO: For screwed patterns, it might occur more often, however,
				 * so (later on) decide on increment based on a statistics of the
				 * final lengths of the array returned.
				 */
				if (offsetCount == offsets.length) { 
					int[] newOffsets = new int[offsets.length + 1];
					System.arraycopy(offsets, 0, newOffsets, 0, offsets.length);
					offsets = newOffsets;
				}
				offsets[offsetCount] = offset;
				offset += matcher.group(0).length();
				offsetCount++;
			}
			return offsets;
		}
	}
	
	/** @see de.uka.ipd.idaho.gamta.Tokenizer#tokenize(java.lang.CharSequence)
	 */
	public TokenSequence tokenize(CharSequence cs) {
		if (cs instanceof MutableCharSequence)
			return this.tokenize((MutableCharSequence) cs);
		return new PlainTokenSequence(cs, this);
		//	TODO: decide on char sequence implementation based on argument char sequence length later on
	}
	
	/** @see de.uka.ipd.idaho.gamta.Tokenizer#tokenize(de.uka.ipd.idaho.gamta.MutableCharSequence)
	 */
	public MutableTokenSequence tokenize(MutableCharSequence cs) {
		return Gamta.newTokenSequence(cs, this);
	}
}
