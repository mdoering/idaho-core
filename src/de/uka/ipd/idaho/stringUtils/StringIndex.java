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


import java.util.HashMap;

/**
 * A counting index for Strings
 * 
 * @author sautter
 */
public class StringIndex {
	
	private static final String NULLKEY = "null";
	
	private boolean hash = true;
	private HashMap content = new HashMap();
	private boolean caseSensitive = true;
	private int size = 0;
	
	/**	Constructor
	 */
	public StringIndex() {
		this(true);
	}
	
	/**	Constructor
	 */
	public StringIndex(boolean caseSensitive) {
//		this(caseSensitive, true);
		this(caseSensitive, !caseSensitive); // if we're case sensitive, we don't save anything by hashing, as then strings are not copied
	}
	
	/**	Constructor
	 */
	protected StringIndex(boolean caseSensitive, boolean hash) {
		this.caseSensitive = caseSensitive;
		this.hash = hash;
	}
	
	/**	@return		true if and only if this index contains the specified string
	 */
	public boolean contains(String string) {
		String s = this.getKeyString(string);
		return this.content.containsKey(s);
//		Object o = this.content.get(s);
//		return (o != null);
	}
	
	/**	@return		the number of times the specified string has been added to this index
	 */
	public int getCount(String string) {
		String s = this.getKeyString(string);
		Int i = ((Int) this.content.get(s));
		return ((i == null) ? 0 : i.intValue());
//		Object o = this.content.get(s);
//		if ((o != null) && (o instanceof Int)) {
//			return ((Int) o).intValue();
//		} else {
//			return 0;
//		}
	}
	
	/**	add a string to this index, using count 1
	 * @return	true if the specified string was added for the first time, false otherwise
	 */
	public boolean add(String string) {
//		boolean inserted;
		String s = this.getKeyString(string);
		Int i = ((Int) this.content.get(s));
		this.size++;
		if (i == null) {
			this.content.put(s, new Int(1));
			return true;
		}
		else {
			i.increment();
			return false;
		}
//		Object o = this.content.get(s);
//		if ((o != null) && (o instanceof Int)) {
//			((Int) o).increment();
//			inserted = false;
//		} else {
//			this.content.put(s, new Int(1));
//			inserted = true;
//		}
//		this.size ++;
//		return inserted;
	}
	
	/**	add a string to this index, using a custom count (same as count times adding string, but faster)
	 * @return	true if the specified string was added for the first time, false otherwise
	 */
	public boolean add(String string, int count) {
//		boolean inserted;
		String s = this.getKeyString(string);
		Int i = ((Int) this.content.get(s));
		this.size += count;
		if (i == null) {
			this.content.put(s, new Int(count));
			return true;
		}
		else {
			i.increment(count);
			return false;
		}
//		Object o = this.content.get(s);
//		if ((o != null) && (o instanceof Int)) {
//			((Int) o).increment(count);
//			inserted = false;
//		} else {
//			this.content.put(s, new Int(count));
//			inserted = true;
//		}
//		this.size += count;
//		return inserted;
	}
	
	/**	remove a string from this index once, decreasing it's count by 1
	 * @return	true if the specified string was totally removed, false otherwise
	 */
	public boolean remove(String string) {
//		boolean removed = false;
		String s = this.getKeyString(string);
		Int i = ((Int) this.content.get(s));
		if (i == null)
			return false;
		this.size--;
		if (i.intValue() > 1) {
			i.decrement();
			return false;
		}
		else {
			this.content.remove(s);
			return true;
		}
//		Object o = this.content.get(s);
//		if ((o != null) && (o instanceof Int)) {
//			int i = ((Int) o).intValue();
//			if (i <= 1) {
//				this.content.remove(s);
//				removed = true;
//			} else {
//				((Int) o).decrement();
//			}
//			this.size --;
//		}
//		return removed;
	}
	
	/**	remove a string from this index, using a custom count (same as count times removing string, but faster)
	 * @return	true if the specified string was totally removed, false otherwise
	 */
	public boolean remove(String string, int count) {
//		boolean removed = false;
		String s = this.getKeyString(string);
		Int i = ((Int) this.content.get(s));
		if (i == null)
			return false;
		if (i.intValue() > count) {
			this.size -= count;
			i.decrement(count);
			return false;
		}
		else {
			this.size -= i.intValue();
			this.content.remove(s);
			return true;
		}
//		Object o = this.content.get(s);
//		if ((o != null) && (o instanceof Int)) {
//			int i = ((Int) o).intValue();
//			int c = ((i > count) ? count : i);
//			if (i <= c) {
//				this.content.remove(s);
//				removed = true;
//			} else {
//				((Int) o).decrement(c);
//			}
//			this.size -= c;
//		}
//		return removed;
	}
	
	/**	remove a string from this index totally, setting it's count to 0
	 */
	public void removeAll(String string) {
		String s = this.getKeyString(string);
		Int i = ((Int) this.content.get(s));
		if (i != null) {
			this.size -= i.intValue();
			this.content.remove(s);
		}
//		this.size -= this.getCount(string);
//		this.content.remove(s);
	}
	
	/**	totally clear this index
	 */
	public void clear() {
		this.content.clear();
		this.size = 0;
	}
	
	/**	@return		true if this index's case sensitivity property has been initialized as true
	 */
	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}
	
	/**	@return		the number of strings that have been added to this index so far
	 */
	public int size() {
		return this.size;
	}
	
	/**	@return		the number of distinct strings that have been added to this index so far
	 */
	public int distinctSize() {
		return this.content.size();
	}
	
	//	threshold for hashing (12 is the length of the String representation of Integer.MIN_VALUE)
	private static final int HASH_THRESHOLD = 13;
	
	/**	prepare a String for storage in the hashmap
	 * @param	string	the string to be prepared
	 * @return a key String for the HashMap (strings longer than the threshold are hashed)
	 */
	private String getKeyString(String string) {
		if (this.hash) {
			if (string == null)
				return NULLKEY;
			else if (this.caseSensitive)
				return ((string.length() < HASH_THRESHOLD) ? string : ("" + string.hashCode()));
			else return ((string.length() < HASH_THRESHOLD) ? string.toLowerCase() : ("" + string.toLowerCase().hashCode()));
		}
		else {
			if (string == null)
				return null;
			else if (this.caseSensitive)
				return string;
			else return string.toLowerCase();
		}
	}
	
	private class Int {
		private int value;
		Int(int val) {
			this.value = val;
		}
		int intValue() {
			return this.value;
		}
		void increment() {
			this.value ++;
		}
		void increment(int i) {
			this.value += i;
		}
		void decrement() {
			this.value --;
		}
		void decrement(int i) {
			this.value = ((this.value > i) ? (this.value - i) : 0);
		}
	}
}
