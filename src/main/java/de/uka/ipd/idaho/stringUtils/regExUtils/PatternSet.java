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
package de.uka.ipd.idaho.stringUtils.regExUtils;


import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * For arbitrary objects, a PatternSet behaves just as a normal
 * java.util.HashSet. <BR>
 * If used with Strings, however, it enables a patttern based lookup. This means
 * that a PatternSet can contain a set of patterns, and the contains(Object o)
 * method returns true if (a) o is of type java.lang.String and (b.1) the
 * content Set contains the specified String explicitly, or (b.2) at least on of
 * the contained patterns matches the specified String
 * 
 * @author sautter
 */
public class PatternSet implements Set {
	
	private StringVector patterns = new StringVector();
	
	private Set positiveSet = new HashSet();
	private Set matchCacheSet = new HashSet();
	private Set noMatchCacheSet = new HashSet();
	
	/**	Constructor for a new empty PatternSet
	 */
	public PatternSet() {
		this(new String[0]);
	}
	
	/**	Constructor
	 * @param	patterns	the initial content for the new PatternSet
	 */
	public PatternSet(StringVector patterns) {
		this(patterns.toStringArray());
	}
	
	/**	Constructor
	 * @param	patterns	the initial content for the new PatternSet
	 */
	public PatternSet(String[] patterns) {
		if (patterns != null) {
			
			//	convert regular expressions
			for (int p = 0; p < patterns.length; p++) {
				String pattern = patterns[p];
				if (RegExUtils.isSimplePattern(pattern))
					this.patterns.addElementIgnoreDuplicates(RegExUtils.getFullPattern(pattern));
				else if (RegExUtils.isValidRegEx(pattern))
					this.patterns.addElementIgnoreDuplicates(pattern);
				else this.positiveSet.add(pattern);
			}
		}
	}
	
	/** @see java.util.Set#add(java.lang.Object)
	 */
	public boolean add(Object o) {
		return ((o instanceof String) ? this.add((String) o) : this.positiveSet.contains(o));
	}
	
	/** @see java.util.Set#add(java.lang.Object)
	 */
	public boolean add(String s) {
		if (RegExUtils.isSimplePattern(s)) {
			String pattern = RegExUtils.getFullPattern(s);
			boolean change = !this.patterns.contains(pattern);
			this.patterns.addElementIgnoreDuplicates(pattern);
			this.noMatchCacheSet.clear();
			return change;
		}
		else if (RegExUtils.isValidRegEx(s)) {
			boolean change = !this.patterns.contains(s);
			this.patterns.addElementIgnoreDuplicates(s);
			this.noMatchCacheSet.clear();
			return change;
		}
		else return this.positiveSet.add(s);
	}
	
	/** @see java.util.Set#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection c) {
		boolean change = false;
		Iterator iter = c.iterator();
		while (iter.hasNext())
			change = (this.add(iter.next()) || change);
		return change;
	}
	
	/** @see java.util.Set#clear()
	 */
	public void clear() {
		this.positiveSet.clear();
		this.matchCacheSet.clear();
		this.noMatchCacheSet.clear();
		
		this.patterns.clear();
	}
	
	/** @see java.util.Set#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		
		//	check arguments
		if (this.positiveSet.contains(o)) return true;
		if (o == null) return false;
		if (!(o instanceof String)) return false;
		
		//	do cache lookup
		if (this.matchCacheSet.contains(o)) return true;
		if (this.noMatchCacheSet.contains(o)) return false;
		
		//	do list lookup
		for (int t = 0; t < this.patterns.size(); t++) {
			try {
				//	try regular expression match
				if (o.toString().matches(this.patterns.get(t))) {
					this.matchCacheSet.add(o);
					return true;
				}
			}
			catch (Exception e) {
				//	try plain comparison
				if (o.toString().equals(this.patterns.get(t))) {
					this.matchCacheSet.add(o);
					return true;
				}
			}
		}
		
		//	remember string which is not contained
		this.noMatchCacheSet.add(o);
		return false;
	}
	
	/** @see java.util.Set#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection c) {
		if (c == null) return true;
		Iterator iter = c.iterator();
		while (iter.hasNext()) 
			if (!this.contains(iter.next())) return false;
		return true;
	}
	
	/** @see java.util.Set#isEmpty()
	 */
	public boolean isEmpty() {
		return ((this.positiveSet.size() + this.patterns.size()) == 0);
	}
	
	/** @see java.util.Set#iterator()
	 */
	public Iterator iterator() {
		return this.positiveSet.iterator();
	}
	
	/** @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Object o) {
		return this.positiveSet.remove(o);
	}
	
	/** @see java.util.Set#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection c) {
		return this.positiveSet.removeAll(c);
	}
	
	/** @see java.util.Set#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection c) {
		return this.positiveSet.retainAll(c);
	}
	
	/** @see java.util.Set#size()
	 */
	public int size() {
		return (this.positiveSet.size() + this.patterns.size());
	}
	
	/** @see java.util.Set#toArray()
	 */
	public Object[] toArray() {
		return ((this.patterns == null) ? new String[0] : this.patterns.toStringArray());
	}
	
	/** @see java.util.Set#toArray(java.lang.Object[])
	 */
	public Object[] toArray(Object[] o) {
		Object[] array = this.toArray();
		System.arraycopy(array, 0, o, 0, Math.min(o.length, array.length));
		return null;
	}
}
