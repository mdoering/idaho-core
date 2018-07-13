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


import java.util.regex.Pattern;

import de.uka.ipd.idaho.gamta.MutableCharSequence;

/**
 * Simple implementation of a mutable char sequence, using a plain StringBuffer for internal representation. Fast on random access, but very slow on modifications.
 * 
 * @author sautter
 */
public class StringBufferCharSequence extends AbstractMutableCharSequence {
	
	private StringBuffer data;
	
	/** Constructor
	 */
	public StringBufferCharSequence() {
		this("");
	}
	
	/** Constructor
	 * @param	chars	the initial content of this StringBufferCharSequence 
	 */
	public StringBufferCharSequence(CharSequence chars) {
		this.data = new StringBuffer(chars.toString());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChar(char)
	 */
	public void addChar(char ch) {
		this.insertChar(ch, this.data.length());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChars(java.lang.CharSequence)
	 */
	public void addChars(CharSequence chars) {
		this.insertChars(chars, this.data.length());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChar(char, int)
	 */
	public void insertChar(char ch, int offset) {
		this.data.insert(offset, ch);
		this.notifyCharSequenceChanged(offset, ("" + ch), "");
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChars(java.lang.CharSequence, int)
	 */
	public void insertChars(CharSequence chars, int offset) {
		this.data.insert(offset, chars.toString());
		this.notifyCharSequenceChanged(offset, chars, "");
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChar(int)
	 */
	public char removeChar(int offset) {
		char ch = this.data.charAt(offset);
		this.data.deleteCharAt(offset);
		this.notifyCharSequenceChanged(offset, "", ("" + ch));
		return ch;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChars(int, int)
	 */
	public CharSequence removeChars(int offset, int length) {
		CharSequence ch = this.data.subSequence(offset, (offset + length));
		this.data.delete(offset, (offset + length));
		this.notifyCharSequenceChanged(offset, "", ch);
		return ch;
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChar(char, int)
	 */
	public char setChar(char ch, int offset) {
		char oldCh = this.data.charAt(offset);
		this.data.setCharAt(offset, ch);
		this.notifyCharSequenceChanged(offset, ("" + ch), ("" + oldCh));
		return oldCh;
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChars(java.lang.CharSequence, int, int)
	 */
	public CharSequence setChars(CharSequence chars, int offset, int length) {
		CharSequence oldChars = this.data.subSequence(offset, (offset + length));
		this.data.replace(offset, (offset + length), chars.toString());
		this.notifyCharSequenceChanged(offset, chars, oldChars);
		return oldChars;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#equals(de.gamta.CharSequence)
	 */
	public boolean equals(CharSequence cs) {
		return ((cs != null) && this.data.toString().equals(cs.toString()));
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#equalsIgnoreCase(java.lang.CharSequence)
	 */
	public boolean equalsIgnoreCase(CharSequence cs) {
		return ((cs != null) && this.data.toString().equalsIgnoreCase(cs.toString()));
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#compareTo(de.gamta.CharSequence)
	 */
	public int compareTo(CharSequence cs) {
		return this.data.toString().compareTo(cs.toString());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return this.data.toString().hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.data.toString();
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#offsetOf(de.gamta.CharSequence)
	 */
	public int offsetOf(CharSequence cs) {
		return this.data.indexOf(cs.toString());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#offsetOf(de.gamta.CharSequence, int)
	 */
	public int offsetOf(CharSequence cs, int from) {
		return this.data.indexOf(cs.toString(), from);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#lastOffsetOf(de.gamta.CharSequence)
	 */
	public int lastOffsetOf(CharSequence cs) {
		return this.data.lastIndexOf(cs.toString());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#lastOffsetOf(de.gamta.CharSequence, int)
	 */
	public int lastOffsetOf(CharSequence cs, int to) {
		return this.data.lastIndexOf(cs.toString(), to);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#startsWith(de.gamta.CharSequence)
	 */
	public boolean startsWith(CharSequence cs) {
		return this.data.toString().startsWith(cs.toString());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#endsWith(de.gamta.CharSequence)
	 */
	public boolean endsWith(CharSequence cs) {
		return this.data.toString().endsWith(cs.toString());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#contains(java.lang.CharSequence)
	 */
	public boolean contains(CharSequence cs) {
		return (this.offsetOf(cs) != -1);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.CharSequence#matches(java.lang.String)
	 */
	public boolean matches(String regex) {
		return Pattern.compile(regex).matcher(this.data).matches();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return this.data.length();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int offset) {
		return this.data.charAt(offset);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return this.data.subSequence(start, end);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public MutableCharSequence mutableSubSequence(int start, int end) {
		return new StringBufferCharSequence(this.data.subSequence(start, end));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object obj) {
		return this.compareTo(obj.toString());
	}
}
