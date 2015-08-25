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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.gamta.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This set implementation keeps track of how often an element was added and
 * removed. Internally, it uses a <code>Map</code> mapping each set element to
 * its current count; by default a <code>HashMap</code>, but other
 * implementations of <code>Map</code> can be handed to respective
 * constructors. The <code>size()</code> of the set changes with every addition
 * and removal of elements; the number of elements is available via the
 * <code>elementCount()</code> method. The <code>Iterator</code> of the set
 * returns every element once.
 * 
 * @author sautter
 */
public class CountingSet implements Set {
	private static class Counter {
		int c = 0;
		Counter(int c) {
			this.c = c;
		}
	}
	
	private Map content;
	private int size = 0;
	
	/** Constructor
	 */
	public CountingSet() {
		this(new HashMap());
	}
	
	/** Constructor
	 * @param contentOrder the content order
	 */
	public CountingSet(Comparator contentOrder) {
		this(new TreeMap(contentOrder));
	}
	
	/** Constructor
	 * @param content the <code>Map</code> to use internally
	 */
	public CountingSet(Map content) {
		this.content = content;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#size()
	 */
	public int size() {
		return this.size;
	}
	
	/**
	 * Returns the number of <em>distinct</em> elements in the set.
	 * @return the number of distinct elements
	 */
	public int elementCount() {
		return this.content.size();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#isEmpty()
	 */
	public boolean isEmpty() {
		return this.content.isEmpty();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#contains(java.lang.Object)
	 */
	public boolean contains(Object obj) {
		return this.content.containsKey(obj);
	}
	
	/**
	 * Retrieve the smallest element in the set.
	 * @return the smallest element in the set
	 * @throws UnsupportedOperationException if the content <code>Map</code> is
	 *            not a <code>SortedMap</code>
	 * @see java.util.SortedSet#first()
	 */
	public Object first() {
		if (this.content instanceof SortedMap)
			return ((SortedMap) this.content).firstKey();
		else throw new UnsupportedOperationException("The method first() only works if the content Map is a SortedMap");
	}
	
	/**
	 * Retrieve the largest element in the set.
	 * @return the largest element in the set
	 * @throws UnsupportedOperationException if the content <code>Map</code> is
	 *            not a <code>SortedMap</code>
	 * @see java.util.SortedSet#last()
	 */
	public Object last() {
		if (this.content instanceof SortedMap)
			return ((SortedMap) this.content).lastKey();
		else throw new UnsupportedOperationException("The method last() only works if the content Map is a SortedMap");
	}
	
	/**
	 * Returns the number of times the set contains the argument element.
	 * @param obj the element whose frequency in this set to obtain
	 * @return the number of times the set contains the argument element
	 */
	public int getCount(Object obj) {
		Counter cnt = ((Counter) this.content.get(obj));
		return ((cnt == null) ? 0 : cnt.c);
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#iterator()
	 */
	public Iterator iterator() {
		return this.content.keySet().iterator();
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#toArray()
	 */
	public Object[] toArray() {
		return this.content.keySet().toArray();
	}

	/* (non-Javadoc)
	 * @see java.util.Set#toArray(T[])
	 */
	public Object[] toArray(Object[] a) {
		return this.content.keySet().toArray(a);
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean add(Object obj) {
		return this.add(obj, 1);
	}
	
	/**
	 * Adds the specified element to the set with a custom count. This method
	 * has the same effect as <code>count</code> subsequent invocations of the
	 * single-argument <code>add()</code> method with the same element.
	 * @param obj the element to add
	 * @param count the count
	 * @return true if the element was added for the first time
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean add(Object obj, int count) {
		if (count <= 0)
			return false;
		this.size += count;
		Counter cnt = ((Counter) this.content.get(obj));
		if (cnt == null) {
			cnt = new Counter(count);
			this.content.put(obj, cnt);
			return true;
		}
		else {
			cnt.c += count;
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Object obj) {
		return this.remove(obj, 1);
	}
	
	/**
	 * Removes the specified element from the set with a custom count. This
	 * method has the same effect as <code>count</code> subsequent invocations
	 * of the single-argument <code>remove()</code> method with the same
	 * element.
	 * @param obj the element to remove
	 * @param count the count
	 * @return true if the element was removed completely
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Object obj, int count) {
		if (count <= 0)
			return false;
		Counter cnt = ((Counter) this.content.get(obj));
		if (cnt == null)
			return false;
		if (cnt.c <= count) {
			this.size -= cnt.c;
			this.content.remove(obj);
			return true;
		}
		else {
			cnt.c -= count;
			this.size -= count;
			return false;
		}
	}
	
	/**
	 * Removes the specified element from the set completely. This method has
	 * the same effect as <code>remove(obj, getCount(obj))</code>.
	 * @param obj the element to remove
	 * @return the frequency the element had prior to removal
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public int removeAll(Object obj) {
		Counter cnt = ((Counter) this.content.get(obj));
		if (cnt == null)
			return 0;
		this.size -= cnt.c;
		this.content.remove(obj);
		return cnt.c;
	}

	/* (non-Javadoc)
	 * @see java.util.Set#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection c) {
		if (c instanceof CountingSet)
			return this.containsAll((CountingSet) c);
		else return this.content.keySet().containsAll(c);
	}
	
	/**
	 * Returns true if this set contains all of the elements of the specified
	 * counting set, with at least the same frequency.
	 * @param cs the counting set to be checked for containment in this set
	 * @return true if this set contains all of the elements of the specified
	 *            counting set, with at least the same frequency
	 */
	public boolean containsAll(CountingSet cs) {
		if (cs == this)
			return true;
		for (Iterator eit = cs.iterator(); eit.hasNext();) {
			Object e = eit.next();
			if (this.getCount(e) < cs.getCount(e))
				return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection c) {
		if (c instanceof CountingSet)
			return this.addAll((CountingSet) c);
		boolean changed = false;
		for (Iterator oit = c.iterator(); oit.hasNext();) {
			Object obj = oit.next();
			changed = (changed | this.add(obj));
		}
		return changed;
	}
	
	/**
	 * Adds all of the elements in the specified counting set to this one. If a
	 * counting set is added to itself via this method, the invocation has no
	 * effect, preventing accidental doubling of all counts. 
	 * @param cs the counting set whose elements to add to this one
	 * @return <code>true</code> if this set changed as a result of the call
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	public boolean addAll(CountingSet cs) {
		boolean changed = false;
		if (cs == this)
			return changed;
		for (Iterator oit = cs.iterator(); oit.hasNext();) {
			Object obj = oit.next();
			changed = (changed | this.add(obj, cs.getCount(obj)));
		}
		return changed;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection c) {
		if (c instanceof CountingSet)
			return this.retainAll((CountingSet) c);
		boolean changed = false;
		for (Iterator eit = this.iterator(); eit.hasNext();) {
			Object e = eit.next();
			if (c.contains(e))
				continue;
			Counter cnt = ((Counter) this.content.get(e));
			this.size--;
			if (cnt.c == 1)
				eit.remove();
			else cnt.c--;
			changed = true;
		}
		return changed;
	}
	
	/**
	 * Retains only the elements in this set that are contained in the
	 * specified counting set, with at most their counts in the latter.
	 * @param cs the counting set containing the elements to retain
	 * @return true if this counting set changed as a result of the call
	 */
	public boolean retainAll(CountingSet cs) {
		boolean changed = false;
		if (cs == this)
			return changed;
		for (Iterator eit = this.iterator(); eit.hasNext();) {
			Object e = eit.next();
			Counter cnt = ((Counter) this.content.get(e));
			int rc = cs.getCount(e);
			if (cnt.c <= rc)
				continue;
			this.size -= (cnt.c - rc);
			if (rc == 0)
				eit.remove();
			else cnt.c = rc;
			changed = true;
		}
		return changed;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection c) {
		if (c instanceof CountingSet)
			return this.removeAll((CountingSet) c);
		boolean changed = false;
		for (Iterator oit = c.iterator(); oit.hasNext();) {
			Object obj = oit.next();
			changed = (changed | this.remove(obj));
		}
		return changed;
	}
	
	/**
	 * Removes all of the elements in the specified counting set from this one.
	 * If a counting set is removed from itself via this method, the invocation
	 * has no effect, preventing accidental clearing of the set. 
	 * @param cs the counting set whose elements to remove from this one
	 * @return <code>true</code> if this set changed as a result of the call
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	boolean removeAll(CountingSet cs) {
		boolean changed = false;
		if (cs == this)
			return changed;
		for (Iterator oit = cs.iterator(); oit.hasNext();) {
			Object obj = oit.next();
			changed = (changed | this.remove(obj, cs.getCount(obj)));
		}
		return changed;
	}
	
	/* (non-Javadoc)
	 * @see java.util.Set#clear()
	 */
	public void clear() {
		this.content.clear();
		this.size = 0;
	}
}