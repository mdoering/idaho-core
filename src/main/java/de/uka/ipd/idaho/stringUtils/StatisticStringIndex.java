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
package de.uka.ipd.idaho.stringUtils;


/**
 * A counting index keeping all the data needed for Markov-chain based
 * computations
 * 
 * @author sautter
 */
public class StatisticStringIndex extends StringIndex {
	
	private int maxLength;
	private int[] counts;
	
	private StatisticStringIndex markovFactors = null;
	
	/**	Constructor
	 */
	public StatisticStringIndex(boolean caseSensitive, int maxLength) {
		this(caseSensitive, maxLength, (maxLength > 1));
	}
	
	/**	Constructor
	 */
	public StatisticStringIndex(int maxLength) {
		this(false, maxLength, (maxLength > 1));
	}
	
	//	main constructor
	private StatisticStringIndex(boolean caseSensitive, int maxLength, boolean storeMarkov) {
		super(caseSensitive, false);
		this.maxLength = maxLength;
		this.counts = new int[maxLength];
		if (storeMarkov) this.markovFactors = new StatisticStringIndex(caseSensitive, (this.maxLength-1), false);
	}
	
	/**	@return		the relative frequency of string among all strings of the same length
	 */
	public double getFactor(String string) {
		int l = string.length() - 1;
		if ((l >= 0) && (l < this.maxLength)) {
			double f = this.getCount(string);
			double c = this.counts[l];
			return ((c > 0) ? (f / c) : 0);
		} else {
			return 0;
		}
	}
	
	/**	@return		the conditional relative frequency of string among all strings of the same length,
	 * 				given the first string.length()-1 characters
	 */
	public double getMarkovFactor(String string) {
		int l = string.length() - 1;
		if ((l >= 0) && (l < this.maxLength)) {
			double f = this.getFactor(string);
			if (l > 0) {
				double mf = this.markovFactors.getFactor(string.substring(0, l));
				return ((mf > 0) ? (f / mf) : 0);
			} else {
				return f;
			}
		} else {
			return 0;
		}
	}
	
	/**	@return		the number of strings of the given length added to this index so far
	 */
	public int size(int length) {
		int l = length - 1;
		if ((l >= 0) && (l < this.maxLength)) {
			return this.counts[l];
		} else {
			return 0;
		}
	}
	
	/**	add a string to this index, using count 1
	 * @return	true if the specified string was added for the first time, false otherwise
	 */
	public boolean add(String string) {
		return this.add(string, 1);
	}
	
	/**	add a string to this index, using a custom count (same as count times adding string, but faster)
	 * @return	true if the specified string was added for the first time, false otherwise
	 */
	public boolean add(String string, int count) {
		int l = string.length() - 1;
		if ((l >= 0) && (l < this.maxLength)) {
			this.counts[l] += count;
			if ((this.markovFactors != null) && (l > 0)) this.markovFactors.add(string.substring(0, l), count);
		}
		return super.add(string, count);
	}

	/**	multiply the count of string times multiplier
	 * 	The count will be increased at least by 1
	 *  If string wasn't contained in this StringIndex before, it is added with count 1
	 * @return	true if the specified string was added for the first time, false otherwise
	 */
	public boolean multiply(String string, double multiplier) {
		int l = string.length() - 1;
		if ((l >= 0) && (l < this.maxLength)) {
 			int oldCount = super.getCount(string);
 			int newCount = (int) (oldCount * multiplier);
 			int diff = ((oldCount == newCount) ? 1 : (newCount - oldCount));
 			return this.add(string, diff);
		} else {
			return false;
		}
	}
	
	/**	remove a string from this index once, decreasing it's count by 1
	 */
	public boolean remove(String string) {
		return this.remove(string, 1);
	}
	
	/**	remove a string from this index, using a custom count (same as count times removing string, but faster)
	 */
	public boolean remove(String string, int count) {
		int i = super.getCount(string);
		int c = ((i < count) ? i : count);
		int l = string.length() - 1;
		if ((i > 0) && (l >= 0) && (l < this.maxLength)) {
			this.counts[l] -= c;
			if ((this.markovFactors != null) && (l > 0)) this.markovFactors.remove(string.substring(0, l), c);
		}
		return super.remove(string, c);
	}
	
	/**	remove a string from this index totally, setting it's count to 0
	 */
	public void removeAll(String string) {
		int i = super.getCount(string);
		int l = string.length() - 1;
		if ((l >= 0) && (l < this.maxLength)) {
			this.counts[l] -= i;
			if ((this.markovFactors != null) && (l > 0)) this.markovFactors.removeAll(string.substring(0, l));
		}
		super.removeAll(string);
	}
	
	/**	totally clear this index
	 */
	public void clear() {
		super.clear();
		for (int i = 0; i < this.maxLength; i++) {
			this.counts[i] = 0;
		}
		if (this.markovFactors != null) this.markovFactors.clear();
	}	
}
