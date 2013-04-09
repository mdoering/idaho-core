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
 * A token, the atomic unit of a text.
 * 
 * @author sautter
 */
public interface Token extends CharSpan, Attributed {
	
	/** the annotation type for Tokens, namely 'token' (though Tokens are not real Annotations in the default case), used e.g. in GPath */
	public static final String TOKEN_ANNOTATION_TYPE = "token";
	
	/** the attribute indicating that a paragraph ends after a Token, namely 'paragraphEnd', i.e. there should be a line break. */
	public static final String PARAGRAPH_END_ATTRIBUTE = "paragraphEnd"; 
	
	/** the generic attribute name for retrieving the value of a Token via the getAttribute() method, namely 'VALUE'. Getting this attribute should never return null, though the listing of attribute names should never contain this attribute name. */
	public static final String TOKEN_VALUE_ATTRIBUTE = "VALUE";
	
	/**	@return	this Token's value, which must not contain whitespace characters
	 */
	public abstract String getValue();
	
	/**	@return	the Tokenizer this Token was created with
	 */
	public abstract Tokenizer getTokenizer();
}
