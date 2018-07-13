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


import de.uka.ipd.idaho.gamta.MutableCharSequence;

/**
 * Implementation of a mutable char sequence using a storage structure similar
 * to a b*-tree for fast random access and modification
 * 
 * @author sautter
 */
public class BTreeCharSequence extends AbstractMutableCharSequence {
	
	/**	the default number of chars / char blocks per dimension of the overlay
	 */
	public static final int DEFAULT_DIMENSION_SIZE = 1024;
	
	/**
	 * 
	 */
	public BTreeCharSequence() {}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChar(char)
	 */
	public void addChar(char ch) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChars(java.lang.CharSequence)
	 */
	public void addChars(CharSequence chars) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChar(char, int)
	 */
	public void insertChar(char ch, int offset) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChars(java.lang.CharSequence, int)
	 */
	public void insertChars(CharSequence chars, int offset) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChar(int)
	 */
	public char removeChar(int offset) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChars(int, int)
	 */
	public CharSequence removeChars(int offset, int length) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChar(char, int)
	 */
	public char setChar(char ch, int offset) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChars(java.lang.CharSequence, int, int)
	 */
	public CharSequence setChars(CharSequence chars, int offset, int length) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#mutableSubSequence(int, int)
	 */
	public MutableCharSequence mutableSubSequence(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}
}
