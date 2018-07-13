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



/**
 * a sequence of tokens
 * 
 * @author sautter
 */
public interface TokenSequence extends CharSequence {
	
	/**	retrieve a Token
	 * @param	index	the index of the required Token 
	 * @return the Token at the desired index
	 */
	public abstract Token tokenAt(int index);
	
	/**	retrieve the first Token
	 * @return the first Token of this TokenSequence
	 */
	public abstract Token firstToken();
	
	/**	retrieve the last Token
	 * @return the last Token of this TokenSequence
	 */
	public abstract Token lastToken();
	
	/**	retrieve a Token's value
	 * @param	index	the index of the Token that's value is required 
	 * @return the value of the Token at the desired index
	 */
	public abstract String valueAt(int index);
	
	/**	retrieve the first Token's value
	 * @return the value of the first Token of this TokenSequence
	 */
	public abstract String firstValue();
	
	/**	retrieve the last Token's value
	 * @return the value of the last Token of this TokenSequence
	 */
	public abstract String lastValue();
	
	/**	@return	the leading whitespace of the character data this TokenSequence was tokenized from
	 */
	public abstract String getLeadingWhitespace();
	
	/**	@return	the whitespace following the Token at the specified index
	 */
	public abstract String getWhitespaceAfter(int index);
	
	/**	@return the number of Tokens contained in this TokenSequence
	 */
	public abstract int size();
	
	/**	@return	the Tokenizer this TokenSequence was tokenized with
	 */
	public abstract Tokenizer getTokenizer();
	
	/**	get a subsequence of this TokenSequence
	 * @param	start	the index of the Token to start at
	 * @param	size	the number of Tokens
	 * @return a subsequence of this TokenSequence, starting with the Token at the specified index and containing the specified number of Tokens
	 */
	public abstract TokenSequence getSubsequence(int start, int size);
}
