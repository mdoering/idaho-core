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
 * A char sequences that allows insering, removing, and replacing individual characters, or character ranges
 * 
 * @author sautter
 */
public interface MutableCharSequence extends CharSequence {
	
	/**
	 * an object encapsulating the essential information on changes to a MutableCharSequence
	 * 
	 * @author sautter
	 */
	public static class CharSequenceEvent {
		
		/** the MutableCharSequence that was changed
		 */
		public final MutableCharSequence charSequence;
		
		/** the start offset of the change
		 */
		public final int offset;
		
		/**	the sequence of chars that was inserted (will be empty on pure removals, never null)
		 */
		public final java.lang.CharSequence inserted;
		
		/**	the sequence of chars that was removed (will be empty on pure insertions, never null)
		 */
		public final java.lang.CharSequence removed;

		/** Constructor
		 * @param	charSequence	the MutableCharSequence that was changed
		 * @param	offset			the start offset of the change
		 * @param	inserted		the sequence of chars that was inserted (will be empty on pure removals, never null)
		 * @param	removed			the sequence of chars that was removed (will be empty on pure insertions, never null)
		 */
		public CharSequenceEvent(MutableCharSequence charSequence, int offset, java.lang.CharSequence inserted, java.lang.CharSequence removed) {
			this.charSequence = charSequence;
			this.offset = offset;
			this.inserted = ((inserted == null) ? "" : inserted);
			this.removed = ((removed == null) ? "" : removed);
		}
	}
	
	/**	add a char to the end of this char sequence
	 * @param	ch	the char to add
	 */
	public abstract void addChar(char ch);
	
	/**	add a sequence of chars to the end of this char sequence
	 * @param	chars	the chars to add
	 */
	public abstract void addChars(java.lang.CharSequence chars);
	
	/**	insert a char in this char sequence
	 * @param	ch		the char to insert
	 * @param	offset	the offset to insert the char at
	 */
	public abstract void insertChar(char ch, int offset);
	
	/**	insert a sequence of chars in this char sequence
	 * @param	chars	the chars to insert
	 * @param	offset	the offset to insert the chars at
	 */
	public abstract void insertChars(java.lang.CharSequence chars, int offset);
	
	/**	remove a char from this char sequence
	 * @param	offset	the position of the char to remove
	 * @return the char that was just removed
	 */
	public abstract char removeChar(int offset);
	
	/**	remove a sequence of chars from this char sequence
	 * @param	offset	the starting position of the chars to remove
	 * @param	length	the number of chars to remove
	 * @return the sequernce of chars that was just removed
	 */
	public abstract java.lang.CharSequence removeChars(int offset, int length);
	
	/**	replace a char with another char
	 * @param	ch		the char to set
	 * @param	offset	the position of the char to replace
	 * @return the char that was just replaced
	 */
	public abstract char setChar(char ch, int offset);
	
	/**	replace a sequence of chars char with another sequence of chars
	 * @param	chars	the chars to set
	 * @param	offset	the position of the first char to replace
	 * @param	length	the number of chars to replace
	 * @return the sequence of chars that was just replaced
	 */
	public abstract java.lang.CharSequence setChars(java.lang.CharSequence chars, int offset, int length);
	
	/** obtain a sub sequence of this mutable char sequence
	 * @param	start	the position to start at
	 * @param	end		the ending position
	 * @return a sub sequence of this mutable char sequence
	 */
	public abstract MutableCharSequence mutableSubSequence(int start, int end);
	
	/**	add a listener to be notified of changes to this char sequence
	 * @param	csl		the CharSequenceListener to add
	 */
	public abstract void addCharSequenceListener(CharSequenceListener csl);
	
	/**	remove a listener so it is no longer notified of changes to this char sequence
	 * @param	csl		the CharSequenceListener to remove
	 */
	public abstract void removeCharSequenceListener(CharSequenceListener csl);
}
