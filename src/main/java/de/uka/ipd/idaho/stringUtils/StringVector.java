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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Vector;

/**
 * A sorted multi-set specialized to Strings. As opposed to Java collections,
 * even with generics, this class can behave case sensitive or case insensitive
 * on demand. StringVector supports all the methods provided by
 * java.util.Vector, plus
 * <ul>
 * <li>set operations (the union(), intersect() and without() methods, and the
 * isSubsetOf() and contains() methods) </li>
 * <li>O(1) lookup (the contains() and containsIgnoreCase() methods, which are
 * backed by HashMaps) </li>
 * <li>built-in sorting (the sortLexicographically() and sortByLength()
 * methods) </li>
 * <li>tokenization (the parseAndAddElements() methods) </li>
 * <li>basic IR functions like RSV computation (the getRSV() and
 * scalarMultiply() methods)</li>
 * </ul>
 */
public class StringVector implements Dictionary {
	
	private Vector vector = new Vector();
	
	private StringIndex content = new StringIndex();
	private StringIndex contentIgnoreCase = new StringIndex(false);
	
	private boolean caseSensitivityDefault = true;
	
	/**	Constructor
	 */
	public StringVector() {
		this(true);
	}
	
	/**	Constructor
	 * 	@param	csd		the default for operations that can be case sensitive or insensitive
	 */
	public StringVector(boolean csd) {
		this.caseSensitivityDefault = csd;
	}
	
	//	method according to java.util.Vector (String instead of Object as type of parameters and return values)
	
	/** @see java.util.Vector#add(int, java.lang.Object)
	 */
	public void add(int index, String s) {
		this.vector.add(index, s);
		this.content.add(s);
		this.contentIgnoreCase.add(s);
		this.notifyModified();
	}
	
	/** @see java.util.Vector#addElement(java.lang.Object)
	 */
	public void addElement(String s) {
		this.vector.addElement(s);
		this.content.add(s);
		this.contentIgnoreCase.add(s);
		this.notifyModified();
	}
	
	/** @see java.util.Vector#clear()
	 */
	public void clear() {
		this.vector.clear();
		this.content.clear();
		this.contentIgnoreCase.clear();
		this.notifyModified();
	}
	
	/** @see java.util.Vector#firstElement()
	 */
	public String firstElement() {
		return (String) this.vector.firstElement();
	}
	
	/** @see java.util.Vector#get(int)
	 */
	public String get(int index) {
		return (String) this.vector.get(index);
	}
	
	/** @see java.util.Vector#insertElementAt(java.lang.Object, int)
	 */
	public void insertElementAt(String s, int index) {
		this.vector.insertElementAt(s, index);
		this.content.add(s);
		this.contentIgnoreCase.add(s);
		this.notifyModified();
	}
	
	/** @see java.util.Vector#lastElement()
	 */
	public String lastElement() {
		return (String) this.vector.lastElement();
	}
	
	/** @see java.util.Vector#remove(int)
	 */
	public String remove(int index) {
		String s = (String) this.vector.remove(index);
		this.content.remove(s);
		this.contentIgnoreCase.remove(s);
		this.notifyModified();
		return s;
	}
	
	/** @see java.util.Vector#remove(java.lang.Object)
	 */
	public void remove(String s) {
		this.vector.remove(s);
		this.content.remove(s);
		this.contentIgnoreCase.remove(s);
		this.notifyModified();
	}
	
	/** remove all occurences of a String
	 * @param	s	the String to be removed
	 */
	public void removeAll(String s) {
		while (this.contains(s)) this.remove(s);
	}
	
	/** @see java.util.Vector#removeElementAt(int)
	 */
	public void removeElementAt(int index) {
		String s = (String) this.vector.remove(index);
		this.content.remove(s);
		this.contentIgnoreCase.remove(s);
		this.notifyModified();
	}
	
	/** @see java.util.Vector#set(int, java.lang.Object)
	 */
	public String set(int index, String s) {
		String ret = (String) this.vector.set(index, s);
		this.content.remove(ret);
		this.content.add(s);
		this.contentIgnoreCase.remove(ret);
		this.contentIgnoreCase.add(s);
		this.notifyModified();
		return ret;
	}
	
	/** @see java.util.Vector#setElementAt(java.lang.Object, int)
	 */
	public void setElementAt(String s, int index) {
		String temp = this.get(index);
		this.content.remove(temp);
		this.content.add(s);
		this.contentIgnoreCase.remove(temp);
		this.contentIgnoreCase.add(s);
		this.vector.setElementAt(s, index);
		this.notifyModified();
	}
	
	/** @see java.util.Vector#size()
	 */
	public int size() {
		return this.vector.size();
	}
	
	/** @see java.util.Vector#isEmpty()
	 */
	public boolean isEmpty() {
		return this.vector.isEmpty();
	}
	
	/** @see java.util.Vector#indexOf(java.lang.Object)
	 */
	public int indexOf(String s) {
		return this.vector.indexOf(s);
	}
	
	/** @see java.util.Vector#indexOf(java.lang.Object, int)
	 */
	public int indexOf(String s, int index) {
		return this.vector.indexOf(s, index);
	}
	
	/** @see java.util.Vector#indexOf(java.lang.Object)
	 */
	public int lastIndexOf(String s) {
		return this.vector.lastIndexOf(s);
	}
	
	/** @see java.util.Vector#indexOf(java.lang.Object, int)
	 */
	public int lastIndexOf(String s, int index) {
		return this.vector.lastIndexOf(s, index);
	}
	
	//	additional methods
	
	/**	get the number of distinct elements contained in this Stringvector
	 * @return the number of distinct Strings contained in this StringVector (the size after duplicate elimination)
	 */
	public int getDistinctElementCount() {
		return this.getDistinctElementCount(this.caseSensitivityDefault);
	}
	
	/**	get the number of distinct elements contained in this Stringvector
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 * @return the number of distinct Strings contained in this StringVector (the size after duplicate elimination)
	 */
	public int getDistinctElementCount(boolean caseSensitive) {
		return (caseSensitive ? this.content.distinctSize() : this.contentIgnoreCase.distinctSize());
	}
	
	/**	check if this StringVector is case sensitive by default
	 * @return the default for operations on this StringVector that can be case sensitive or insensitive
	 */
	public boolean isDefaultCaseSensitive() {
		return this.caseSensitivityDefault;
	}
	
	/**	check the number of times a given String is contained in this StringVector
	 * @param	s	the String to check for
	 * @return the number of times String s is contained in this StringVector
	 */
	public int getElementCount(String s) {
		return this.getElementCount(s, this.caseSensitivityDefault);
	}
	
	/**	check the number of times a given String is contained in this StringVector
	 * @param	s	the String to check for
	 * @return the number of times String s is contained in this StringVector
	 */
	public int getElementCount(String s, boolean caseSensitive) {
		return (caseSensitive ? this.content.getCount(s) : this.contentIgnoreCase.getCount(s));
	}
	
	/**	check if this StringVector contains duplicates
	 * @return		true if and only if this StringVector contains no duplicate elements
	 */
	public boolean isDuplicateFree() {
		return this.isDuplicateFree(this.caseSensitivityDefault);
	}
	
	/**	check if this StringVector contains duplicates
	 * @param		caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 * @return		true if and only if this StringVector contains no duplicate elements
	 */
	public boolean isDuplicateFree(boolean caseSensitive) {
		return (caseSensitive ? (this.content.size() == this.size()) : (this.contentIgnoreCase.size() == this.size()));
	}
	
	/**	remove duplicate elements from this StringVector so that it contains each element exactly once
	 */
	public void removeDuplicateElements() {
		this.removeDuplicateElements(this.caseSensitivityDefault);
	}
	
	/**	remove duplicate elements from this StringVector so that it contains each element exactly once
	 * 	@param		caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 */
	public void removeDuplicateElements(boolean caseSensitive) {
		int actualIndex = 0;
		while (actualIndex < this.vector.size()) {
			if (caseSensitive) {
				if (this.content.getCount(this.get(actualIndex)) > 1) {
					this.removeElementAt(actualIndex);
				} else {
					actualIndex++;
				}
			} else {
				if (this.contentIgnoreCase.getCount(this.get(actualIndex)) > 1) {
					this.removeElementAt(actualIndex);
				} else {
					actualIndex++;
				}
			}
		}
	}
	
	/** sort the elements of this StringVector lexicographically
	 */
	public synchronized void sortLexicographically() {
		this.sortLexicographically(false, this.caseSensitivityDefault);
	}
	
	/** sort the elements of this StringVector lexicographically
	 * 	@param		descending		determines weather the elements shall be sorted in ascending or in descending order
	 */
	public synchronized void sortLexicographically(boolean descending) {
		this.sortLexicographically(descending, this.caseSensitivityDefault);
	}
	
	/** sort the elements of this StringVector lexicographically
	 * 	@param		descending		determines weather the elements shall be sorted in ascending or in descending order
	 * 	@param		caseSensitive	use String.compareTo(String s) or String.compareToIgnoreCase(String s) for String comparison
	 */
	public synchronized void sortLexicographically(boolean descending, boolean caseSensitive) {
		Collections.sort(this.vector, new StringComparator(descending, caseSensitive));
		this.notifyModified();
	}
	private class StringComparator implements Comparator {
		final boolean descending;
		final boolean caseSensitive;
		public StringComparator(boolean descending, boolean caseSensitive) {
			this.descending = descending;
			this.caseSensitive = caseSensitive;
		}
		public int compare(Object o1, Object o2) {
			if (o1 == null) return ((o2 == null) ? 0 : (this.descending ? -1 : 1));
			if (o2 == null) return ((o1 == null) ? 0 : (this.descending ? 1 : -1));
			if (!(o1 instanceof String)) return ((!(o2 instanceof String)) ? 0 : (this.descending ? -1 : 1));
			if (!(o2 instanceof String)) return ((!(o1 instanceof String)) ? 0 : (this.descending ? 1 : -1));
			int c;
			if (this.caseSensitive) c = o1.toString().compareTo(o2.toString());
			else c = o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
			return (this.descending ? -c : c);
		}
	}
	
	/**	sort the elements of this StringVector by their length in descending order
	 */
	public synchronized void sortByLength() {
		this.sortByLength(true);
	}
	
	/**	sort the elements of this StringVector by their length
	 * 	@param		descending		determines weather the elements shall be sorted in ascending or in descending order
	 */
	public synchronized void sortByLength(boolean descending) {
		Collections.sort(this.vector, new StringLengthComparator(descending));
		this.notifyModified();
	}
	private class StringLengthComparator implements Comparator {
		final boolean descending;
		public StringLengthComparator(boolean descending) {
			this.descending = descending;
		}
		public int compare(Object o1, Object o2) {
			if (o1 == null) return ((o2 == null) ? 0 : (this.descending ? -1 : 1));
			if (o2 == null) return ((o1 == null) ? 0 : (this.descending ? 1 : -1));
			if (!(o1 instanceof String)) return ((!(o2 instanceof String)) ? 0 : (this.descending ? -1 : 1));
			if (!(o2 instanceof String)) return ((!(o1 instanceof String)) ? 0 : (this.descending ? 1 : -1));
			int c = (o1.toString().length() - o2.toString().length());
			return (this.descending ? -c : c);
		}
	}
	
	/**	add an element to this StringVector if it's not already contained in it
	 * @param	s				the String to be added
	 */
	public void addElementIgnoreDuplicates(String s) {
		this.addElementIgnoreDuplicates(s, this.caseSensitivityDefault);
	}
	
	/**	add an element to this StringVector if it's not already contained in it
	 * @param	s				the String to be added
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 */
	public void addElementIgnoreDuplicates(String s, boolean caseSensitive) {
		if (caseSensitive ? !this.contains(s) : !this.containsIgnoreCase(s)) this.addElement(s);
	}
	
	/**	check if this StringVector contains a given String
	 * @param	s	the String to check for
	 * @return true if and only if this StringVector contains String s at least once (case sensitive comparison)
	 */
	public boolean contains(String s) {
		return this.content.contains(s);
	}
	
	/**	check if this StringVector contains a given String
	 * @param	s	the String to check for
	 * @return true if and only if this StringVector contains String s at least once (case insensitive comparison)
	 */
	public boolean containsIgnoreCase(String s) {
		return this.contentIgnoreCase.contains(s);
	}
	
	/** @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String, boolean)
	 */
	public boolean lookup(String string, boolean caseSensitive) {
		return (caseSensitive ? this.contains(string) : this.containsIgnoreCase(string));
	}
	
	/** @see de.uka.ipd.idaho.stringUtils.Dictionary#lookup(java.lang.String)
	 */
	public boolean lookup(String string) {
		return this.lookup(string, this.caseSensitivityDefault);
	}
	
	/**	check if this StringVector contains all Strings contained in a given StringVector
	 * @param	sv	the StringVector that's contant to check for
	 * @return true if and only if this StringVector contains every single element of StringVector s at least once (case sensitive comparison)
	 */
	public boolean contains(StringVector sv) {
		for (int i = 0; i < sv.size(); i++) if (!this.contains(sv.get(i))) return false;
		return true;
	}
	
	/**	check if this StringVector contains all Strings contained in a given StringVector
	 * @param	sv	the StringVector that's contant to check for
	 * @return true if and only if this StringVector contains every single element of StringVector s at least once (case insensitive comparison)
	 */
	public boolean containsIgnoreCase(StringVector sv) {
		for (int i = 0; i < sv.size(); i++) if (!this.containsIgnoreCase(sv.get(i))) return false;
		return true;
	}
	
	/**	add all elements of StringVector s to this StringVector
	 * @param	sv	the StringVector that's contant is to be added
	 */
	public void addContent(StringVector sv) {
		for (int i = 0; i < sv.size(); i++) this.addElement(sv.get(i));
	}
	
	/**	add all elements of StringVector s to this StringVector, but ignore elements already contained
	 * @param	sv	the StringVector containing the Strings to be added
	 */
	public void addContentIgnoreDuplicates(StringVector sv) {
		this.addContentIgnoreDuplicates(sv, this.caseSensitivityDefault);
	}
	
	/**	add all elements of StringVector s to this StringVector, but ignore elements already contained
	 * @param	sv				the StringVector containing the Strings to be added
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 */
	public void addContentIgnoreDuplicates(StringVector sv, boolean caseSensitive) {
		for (int i = 0; i < sv.size(); i++) this.addElementIgnoreDuplicates(sv.get(i), caseSensitive);
	}
	
	/**	add all elements of String array s to this StringVector
	 * @param	s	the array containing the Strings to be added
	 */
	public void addContent(String[] s) {
		if (s != null) for (int i = 0; i < s.length; i++) this.addElement(s[i]);
	}
	
	/**	add all elements of String array s to this StringVector, but ignore elements already contained
	 * @param	s	the array containing the Strings to be added
	 */
	public void addContentIgnoreDuplicates(String[] s) {
		this.addContentIgnoreDuplicates(s, this.caseSensitivityDefault);
	}
	
	/**	add all elements of String array s to this StringVector, but ignore elements already contained
	 * @param	s				the array containing the Strings to be added
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 */
	public void addContentIgnoreDuplicates(String[] s, boolean caseSensitive) {
		if (s != null) for (int i = 0; i < s.length; i++) this.addElementIgnoreDuplicates(s[i], caseSensitive);
	}
	
	/**	add all elements of StringVector s to this StringVector, clear s afterward
	 */
	public void takeoverContent(StringVector sv) {
		this.addContent(sv);
		sv.clear();
	}
	
	/**	parse elements using separator for the cutting points and add the snippets to this StringVector
	 */
	public void parseAndAddElements(String elements, String separator) {
		if (elements != null) {
			if ((separator == null) || (separator.length() == 0))
				this.addElement(elements);
			
			else {
				int splitLength = separator.length();
				int split = elements.indexOf(separator);
				int lastSplit = 0;
				while (split != -1) {
					this.addElement(elements.substring(lastSplit, split));
					lastSplit = split + splitLength;
					split = elements.indexOf(separator, lastSplit);
				}
				if (lastSplit <= elements.length()) this.addElement(elements.substring(lastSplit));
			}
		}
	}
	
	/**	check if the content of this StringVector is a subset of another StringVector's content
	 * @param	sv				the StringVector to union this StringVector with
	 * @return true if and only if all elements of this StringVector is contained in StringVector s at least once
	 */
	public boolean isSubsetOf(StringVector sv) {
		return this.isSubsetOf(sv, this.caseSensitivityDefault);
	}
	
	/**	check if the content of this StringVector is a subset of another StringVector's content
	 * @param	sv				the StringVector to union this StringVector with
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 * @return true if and only if all elements of this StringVector is contained in StringVector s at least once
	 */
	public boolean isSubsetOf(StringVector sv, boolean caseSensitive) {
		return ((sv != null) && (caseSensitive ? sv.contains(this) : sv.containsIgnoreCase(this)));
	}
	
	/**	remove all elements with index less than i from this StringVector
	 * @param	i	the index to stop at
	 * 	if i exceeds this.size(), the whole StringVector is cleared
	 * 	if i is 0 or less than 0, no element is removed at all
	 */
	public void clearTo(int i) {
		int actualIndex = i;
		while (this.size() > 0 && actualIndex > 0) {
			this.removeElementAt(0);
			actualIndex--;
		}
	}

	/**	remove all elements with index equal or grater than i from this StringVector
	 * @param	i	the index to start from
	 * 	if i exceeds this.size(), no element is removed at all
	 * 	if i is 0 or less than 0, the whole StringVector is cleared
	 */
	public void clearFrom(int i) {
		while ((i > -1) && (this.size() > i) && (this.size() > 0)) {
			this.removeElementAt(i);
		}
	}
	
	/**	concatenate the elements of this StringVector
	 * @param	s	the String to be inserted between the several elements
	 * @return	the elements of this StringVector concatenated to one String, with s inserted between the elements
	 */
	public String concatStrings(String s) {
		return this.concatStrings(s, 0);
	}
	
	/**	concatenate the elements of this StringVector
	 * @param	s		the String to be inserted between the several elements
	 * @param	index	the index of the element to start with
	 * @return	the elements of this StringVector concatenated to one String, with s inserted between the elements
	 */
	public String concatStrings(String s, int index) {
		return this.concatStrings(s, index, this.size());
	}
	
	/**	concatenate the elements of this StringVector
	 * @param	s			the String to be inserted between the several elements
	 * @param	fromIndex	the index of the element to start with
	 * @param	toIndex		the index of the element to stop before
	 * @return	the elements of this StringVector concatenated to one String, with s inserted between the elements
	 */
	public String concatStrings(String s, int fromIndex, int toIndex) {
		StringBuffer assembler = new StringBuffer("");
		int start = ((fromIndex < 0) ? 0 : ((fromIndex > this.size()) ? this.size() : fromIndex));
		int end = ((toIndex < 0) ? 0 : ((toIndex > this.size()) ? this.size() : toIndex));
		
		for (int i = start; i < end; i++) {
			if (i != start) assembler.append(s);
			assembler.append(this.get(i));
		}
		return assembler.toString();
	}
	
	/**	@return the elements of this StringVector in a String array
	 */
	public String[] toStringArray() {
		String[] ret = new String[this.size()];
		for (int i = 0; i < this.size(); i++) {
			ret[i] = this.get(i);
		}
		return ret;
	}
	
	/** @return the distinct elements of this StringVector in a Set
	 */
	public Set toSet() {
		HashSet set = new HashSet();
		for (int e = 0; e < this.size(); e++) set.add(this.get(e));
		return set;
	}
	
	/** @return the elements of this StringVector in a List
	 */
	public List toList() {
		ArrayList list = new ArrayList();
		for (int e = 0; e < this.size(); e++) list.add(this.get(e));
		return list;
	}
	
	/** @see de.uka.ipd.idaho.stringUtils.Dictionary#getEntryIterator()
	 */
	public StringIterator getEntryIterator() {
		return new StringVectorIterator();
	}
	
	private class StringVectorIterator implements StringIterator, StringVectorListener {
		private boolean dataModified = false;
		private int index = 0;
		StringVectorIterator() {
			addStringVectorListener(this);
		}
		public void stringVectorModified() {
			this.dataModified = true;
		}
		public boolean hasMoreStrings() {
			if (this.dataModified) throw new ConcurrentModificationException();
			return this.index < size();
		}
		public String nextString() {
			if (this.dataModified) throw new ConcurrentModificationException();
			String o = get(index);
			this.index ++;
			return o;
		}
		public boolean hasNext() {
			return this.hasMoreStrings();
		}
		public Object next() {
			return this.nextString();
		}
		public void remove() {
			if (this.dataModified) throw new ConcurrentModificationException();
			this.index--;
			removeElementAt(this.index);
			this.dataModified = false;
		}
		protected void finalize() throws Throwable {
			removeStringVectorListener(this);
			super.finalize();
		}
	}
	
	/** @return a wrapper to this StringVector, behaving like a Set
	 * Note: the wrapper will ignore non-String arguments to any method, its Iterators do not support modification
	 */
	public Set asSet() {
		return new StringSet(this);
	}
	
	private static class StringSet implements Set {
		private StringVector data;
		StringSet(StringVector data) {
			this.data = data;
		}
		public boolean add(Object o) {
			if ((o == null) || !(o instanceof String)) return false;
			if (this.data.isDefaultCaseSensitive()) {
				if (this.data.contains(o.toString())) return false;
				this.data.addElementIgnoreDuplicates(o.toString());
				return true;
			} else {
				if (this.data.containsIgnoreCase(o.toString())) return false;
				this.data.addElementIgnoreDuplicates(o.toString(), false);
				return true;
			}
		}
		public boolean addAll(Collection c) {
			if (c == null) return false;
			boolean change = false;
			Iterator i = c.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				change = (this.add(o) || change);
			}
			return change;
		}
		public void clear() {
			this.data.clear();
		}
		public boolean contains(Object o) {
			if ((o == null) || !(o instanceof String)) return false;
			if (this.data.isDefaultCaseSensitive()) return this.data.contains(o.toString());
			else return this.data.containsIgnoreCase(o.toString());
		}
		public boolean containsAll(Collection c) {
			if (c == null) return true;
			Iterator i = c.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				if (!this.contains(o)) return false;
			}
			return true;
		}
		public boolean isEmpty() {
			return (this.data.size() == 0);
		}
		public boolean remove(Object o) {
			if ((o == null) || !(o instanceof String)) return false;
			if (this.data.isDefaultCaseSensitive()) {
				boolean change = this.data.contains(o.toString());
				this.data.removeAll(o.toString());
				return change;
			} else {
				boolean change = this.data.containsIgnoreCase(o.toString());
				this.data.removeAll(o.toString());
				return change;
			}
		}
		public boolean removeAll(Collection c) {
			if (c == null) return false;
			boolean change = false;
			Iterator i = c.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				change = (this.remove(o) || change);
			}
			return change;
		}
		public boolean retainAll(Collection c) {
			if (c == null) return false;
			boolean change = false;
			StringVector toRemove = new StringVector();
			for (int s = 0; s < this.data.size(); s++) {
				if (!c.contains(this.data.get(s))) {
					toRemove.addElementIgnoreDuplicates(this.data.get(s));
					change = true;
				}
			}
			if (!toRemove.isEmpty())
				for (int r = 0; r < toRemove.size(); r++)
					this.data.removeAll(toRemove.get(r));
			return change;
		}
		public int size() {
			return this.data.getDistinctElementCount();
		}
		public Object[] toArray() {
			return this.data.union(this.data).toStringArray();
		}
		public Object[] toArray(Object[] o) {
			if ((o == null) || (o.length == 0)) return o;
			Object[] oHelp = this.toArray();
			for (int i = 0; (i < o.length) && (i < oHelp.length); i++) o[i] = oHelp[i];
			return o;
		}
		
		public Iterator iterator() {
			return new StringSetIterator();
		}
		private class StringSetIterator implements Iterator, StringVectorListener {
			private StringVector setData;
			private boolean dataModified = false;
			private int index = 0;
			StringSetIterator() {
				data.addStringVectorListener(this);
				this.setData = data.union(data);
			}
			public void stringVectorModified() {
				this.dataModified = true;
			}
			public boolean hasNext() {
				if (this.dataModified) throw new ConcurrentModificationException();
				return this.index < this.setData.size();
			}
			public Object next() {
				if (this.dataModified) throw new ConcurrentModificationException();
				Object o = this.setData.get(index);
				this.index ++;
				return o;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
			protected void finalize() throws Throwable {
				data.removeStringVectorListener(this);
				super.finalize();
			}
		}
	}
	
	/** @return a wrapper to this StringVector, behaving like a List
	 * Note: the wrapper will ignore non-String arguments to any method, its Iterators do not support modification
	 */
	public List asList() {
		return new StringList(this);
	}
	
	private static class StringList implements List {
		private StringVector data;
		StringList(StringVector data) {
			this.data = data;
		}
		public void add(int index, Object o) {
			if ((o == null) || !(o instanceof String)) return;
			this.data.insertElementAt(o.toString(), index);
		}
		public boolean add(Object o) {
			if ((o == null) || !(o instanceof String)) return false;
			this.data.addElement(o.toString());
			return true;
		}
		public boolean addAll(Collection c) {
			if (c == null) return false;
			boolean change = false;
			Iterator i = c.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				change = (this.add(o) || change);
			}
			return change;
		}
		public boolean addAll(int index, Collection c) {
			if (c == null) return false;
			Iterator i = c.iterator();
			int offset = 0;
			while (i.hasNext()) {
				Object o = i.next();
				if ((o != null) && (o instanceof String)) {
					this.data.insertElementAt(o.toString(), (index + offset));
					offset++;
				}
			}
			return (offset != 0);
		}
		public void clear() {
			this.data.clear();
		}
		public boolean contains(Object o) {
			if ((o == null) || !(o instanceof String)) return false;
			if (this.data.isDefaultCaseSensitive()) return this.data.contains(o.toString());
			else return this.data.containsIgnoreCase(o.toString());
		}
		public boolean containsAll(Collection c) {
			if (c == null) return true;
			Iterator i = c.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				if (!this.contains(o)) return false;
			}
			return true;
		}
		public Object get(int index) {
			return this.data.get(index);
		}
		public int indexOf(Object o) {
			if ((o == null) || !(o instanceof String)) return -1;
			return this.data.indexOf(o.toString());
		}
		public boolean isEmpty() {
			return (this.data.size() != 0);
		}
		public int lastIndexOf(Object o) {
			if ((o == null) || !(o instanceof String)) return -1;
			return this.data.lastIndexOf(o.toString());
		}
		public Object remove(int index) {
			return this.data.remove(index);
		}
		public boolean remove(Object o) {
			if ((o == null) || !(o instanceof String)) return false;
			boolean change;
			if (this.data.isDefaultCaseSensitive()) change = this.data.contains(o.toString());
			else change = this.data.containsIgnoreCase(o.toString());
			this.data.remove(o.toString());
			return change;
		}
		public boolean removeAll(Collection c) {
			if (c == null) return false;
			boolean change = false;
			Iterator i = c.iterator();
			while (i.hasNext()) {
				Object o = i.next();
				change = (this.remove(o) || change);
			}
			return change;
		}
		public boolean retainAll(Collection c) {
			if (c == null) return false;
			boolean change = false;
			StringVector toRemove = new StringVector();
			for (int s = 0; s < this.data.size(); s++) {
				if (!c.contains(this.data.get(s))) {
					toRemove.addElementIgnoreDuplicates(this.data.get(s));
					change = true;
				}
			}
			if (!toRemove.isEmpty())
				for (int r = 0; r < toRemove.size(); r++)
					this.data.removeAll(toRemove.get(r));
			return change;
		}
		public Object set(int index, Object o) {
			if ((o == null) || !(o instanceof String)) return o;
			Object old = this.data.get(index);
			this.data.setElementAt(o.toString(), index);
			return old;
		}
		public int size() {
			return this.data.size();
		}
		public List subList(int fromIndex, int toIndex) {
			ArrayList list = new ArrayList(toIndex - fromIndex);
			for (int i = fromIndex; i < toIndex; i++) list.add(this.data.get(i));
			return list;
		}
		public Object[] toArray() {
			return this.data.toStringArray();
		}
		public Object[] toArray(Object[] o) {
			if (o == null) return o;
			int end = ((o.length < this.data.size()) ? o.length : this.data.size());
			for (int i = 0; i < end; i++) o[i] = this.data.get(i);
			return o;
		}
		
		public Iterator iterator() {
			return new StringListIterator();
		}
		public ListIterator listIterator() {
			return new StringListIterator();
		}
		public ListIterator listIterator(int index) {
			return new StringListIterator(index);
		}
		private class StringListIterator implements ListIterator, StringVectorListener {
			private boolean dataModified = false;
			private int index = 0;
			StringListIterator() {
				this(0);
			}
			StringListIterator(int index) {
				data.addStringVectorListener(this);
				this.index = index;
			}
			public void stringVectorModified() {
				this.dataModified = true;
			}
			public boolean hasNext() {
				if (this.dataModified) throw new ConcurrentModificationException();
				return this.index < data.size();
			}
			public Object next() {
				if (this.dataModified) throw new ConcurrentModificationException();
				String s = data.get(index);
				index ++;
				return s;
			}
			public void remove() {
				throw new UnsupportedOperationException();
			}
			public void add(Object o) {
				throw new UnsupportedOperationException();
			}
			public boolean hasPrevious() {
				return (this.index != 0);
			}
			public int nextIndex() {
				return this.index;
			}
			public Object previous() {
				if (this.dataModified) throw new ConcurrentModificationException();
				this.index --;
				return data.get(index);
			}
			public int previousIndex() {
				return (this.index - 1);
			}
			public void set(Object o) {
				throw new UnsupportedOperationException();
			}
			protected void finalize() throws Throwable {
				data.removeStringVectorListener(this);
				super.finalize();
			}
		}
	}
	
	private static interface StringVectorListener {
		abstract void stringVectorModified();
	}
	
	//	methods for invalidating iterators on changes
	private Vector listeners = null;
	void addStringVectorListener(StringVectorListener svl) {
		if (svl == null) return;
		if (this.listeners == null) this.listeners = new Vector(2);
		this.listeners.add(svl);
	}
	void removeStringVectorListener(StringVectorListener svl) {
		if (this.listeners != null) this.listeners.remove(svl);
	}
	private void notifyModified() {
		if (this.listeners != null) for (int l = 0; l < this.listeners.size(); l++)
			((StringVectorListener) this.listeners.get(l)).stringVectorModified();
	}
	
	/** @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (!(obj instanceof StringVector)) return super.equals(obj);
		StringVector sv = ((StringVector) obj);
		if ((this.size() != sv.size()) || !this.contentEquals(sv)) return false;
		if (this.isDuplicateFree() && sv.isDuplicateFree()) return true;
		StringVector distinctElements = this.union(sv);
		for (int e = 0; e < distinctElements.size(); e++) {
			String element = distinctElements.get(e);
			if (this.getElementCount(element) != sv.getElementCount(element)) return false;
		}
		return true;
	}
	
	/**	compare the content of this StringVector to another one
	 * @param	sv				the StringVector to union this StringVector with
	 * @return true if and only if this StringVector contains the same distinct elements as the argument StringVector
	 */
	public boolean contentEquals(StringVector sv) {
		return this.contentEquals(sv, this.caseSensitivityDefault);
	}
	
	/**	compare the content of this StringVector to another one
	 * @param	sv				the StringVector to union this StringVector with
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 * @return true if and only if this StringVector contains the same distinct elements as the argument StringVector
	 */
	public boolean contentEquals(StringVector sv, boolean caseSensitive) {
		return ((sv != null) && (caseSensitive ? this.contains(sv) : this.containsIgnoreCase(sv)) && (caseSensitive ? sv.contains(this) : sv.containsIgnoreCase(this)));
	}
	
	/**	get the elements of this StringVector and those of StringVector sv together and duplicate eliminated in a new StringVector
	 * @param	sv				the StringVector to union this StringVector with
	 */
	public StringVector union(StringVector sv) {
		return this.union(sv, this.caseSensitivityDefault);
	}
	
	/**	get the elements of this StringVector and those of StringVector sv together and duplicate eliminated in a new StringVector
	 * @param	sv				the StringVector to union this StringVector with
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 */
	public StringVector union(StringVector sv, boolean caseSensitive) {
		StringVector ret = new StringVector();
		ret.addContentIgnoreDuplicates(this, caseSensitive);
		ret.addContentIgnoreDuplicates(sv, caseSensitive);
		return ret;
	}
	
	/**	get the elements of this StringVector that are not contained in StringVector sv, duplicate eliminated in a new StringVector
	 * @param	sv	the StringVector containing the elements to be skiped
	 */
	public StringVector without(StringVector sv) {
		return this.without(sv, this.caseSensitivityDefault);
	}
	
	/**	get the elements of this StringVector that are not contained in StringVector sv, duplicate eliminated in a new StringVector
	 * @param	sv				the StringVector containing the elements to be skiped
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 */
	public StringVector without(StringVector sv, boolean caseSensitive) {
		StringVector ret = new StringVector();
		for (int i = 0; i < this.size(); i++) {
			if (caseSensitive) {
				if (!sv.contains(this.get(i))) ret.addElementIgnoreDuplicates(this.get(i), caseSensitive);
			} else {
				if (!sv.containsIgnoreCase(this.get(i))) ret.addElementIgnoreDuplicates(this.get(i), caseSensitive);
			}
		}
		return ret;
	}
	
	/**	get the elements of this StringVector that are also contained in StringVector sv, duplicate eliminated in a new StringVector
	 * @param	sv	the StringVector to intersect this StringVector with
	 */
	public StringVector intersect(StringVector sv) {
		return this.intersect(sv, this.caseSensitivityDefault);
	}
	
	/**	get the elements of this StringVector that are also contained in StringVector sv, duplicate eliminated in a new StringVector
	 * @param	sv				the StringVector to intersect this StringVector with
	 * @param	caseSensitive	use String.equals(String s) or String.equalsIgnoreCase(String s) for String comparison
	 */
	public StringVector intersect(StringVector sv, boolean caseSensitive) {
		StringVector ret = new StringVector();
		for (int i = 0; i < this.size(); i++) {
			if (caseSensitive) {
				if (sv.contains(this.get(i))) ret.addElementIgnoreDuplicates(this.get(i), caseSensitive);
			} else {
				if (sv.containsIgnoreCase(this.get(i))) ret.addElementIgnoreDuplicates(this.get(i), caseSensitive);
			}
		}
		return ret;
	}
	
	//	methods for information retrieval
	
	/**	the mode mofifier for the RSV computation which induces the scalar product to be used: qT x d / ||d||
	 */
	public static final int SCALAR = 0;
	
	/**	the mode mofifier for the RSV computation which induces the cosine measure to be used (the default): qT x d / (||d|| * ||q||)
	 */
	public static final int COSINE = 1;
	
	/**	the mode mofifier for the RSV computation which induces the dice coefficient to be used: 2 (qT x d) / (dT x d / qT x q)
	 */
	public static final int DICE = 2;
	
	/**	the mode mofifier for the RSV computation which induces the jaccard coefficient to be used: (qT x d) / (dT x d + qT x q - qT x d)
	 */
	public static final int JACCARD = 3;
	
	/**	compute the RSV of this StringVector with regard to the argument StringVector using cosine measure and default case sensitivity
	 * @param	sv		the StringVector to vector multiply this StringVector with
	 * @return a float value representing the RSV of this StringVector with regard to the argument StringVector
	 */
	public float getRSV(StringVector sv) {
		return this.getRSV(sv, COSINE, this.caseSensitivityDefault);
	}
	
	/**	compute the RSV of this StringVector with regard to the argument StringVector using cosine measure
	 * @param	sv				the StringVector to vector multiply this StringVector with
	 * @param	caseSensitive	match the feature Strings case sensitive or insensitive
	 * @return a float value representing the RSV of this StringVector with regard to the argument StringVector
	 */
	public float getRSV(StringVector sv, boolean caseSensitive) {
		return this.getRSV(sv, COSINE, caseSensitive);
	}
	
	/**	compute the RSV of this StringVector with regard to the argument StringVector using default case sensitivity
	 * @param	sv		the StringVector to vector multiply this StringVector with
	 * @param	mode	the mode the two vectors are combined
	 * @return a float value representing the RSV of this StringVector with regard to the argument StringVector
	 */
	public float getRSV(StringVector sv, int mode) {
		return this.getRSV(sv, mode, this.caseSensitivityDefault);
	}
	
	/**	compute the RSV of this StringVector with regard to the argument StringVector
	 * @param	sv				the StringVector to vector multiply this StringVector with
	 * @param	caseSensitive	match the feature Strings case sensitive or insensitive
	 * @param	mode			the mode the two vectors are combined
	 * @return a float value representing the RSV of this StringVector with regard to the argument StringVector
	 */
	public float getRSV(StringVector sv, int mode, boolean caseSensitive) {
		int m = mode % 4;
		float rsv = scalarMultiply(this, sv, caseSensitive);
		float divisor = 1;
		if (m == COSINE) {
			divisor = ((float) (Math.sqrt(scalarMultiply(this, this, caseSensitive)) * Math.sqrt(scalarMultiply(sv, sv, caseSensitive))));
		} else if (m == DICE) {
			divisor = ((scalarMultiply(this, this, caseSensitive) * scalarMultiply(sv, sv, caseSensitive)) / 2);
		} else if (m == JACCARD) {
			divisor = (scalarMultiply(this, this, caseSensitive) + scalarMultiply(sv, sv, caseSensitive) - rsv);
		} else if (m == SCALAR) {
			divisor = this.size();
		}
		return ((divisor > 0) ? (rsv / divisor) : 0);
	}
	
	/**	compute the scalar product of this StringVector and the argument StringVector, considering each distinct element as a dimension
	 * @param	sv				the StringVector to vector multiply this StringVector with
	 * @return the scalar product of this StringVector and the argument StringVector
	 */
	public float scalarMultiply(StringVector sv) {
		return this.scalarMultiply(sv, this.caseSensitivityDefault);
	}
	
	/**	compute the scalar product of this StringVector and the argument StringVector, considering each distinct element as a dimension
	 * @param	sv				the StringVector to vector multiply this StringVector with
	 * @param	caseSensitive	match the feature Strings case sensitive or insensitive
	 * @return the scalar product of this StringVector and the argument StringVector
	 */
	public float scalarMultiply(StringVector sv, boolean caseSensitive) {
		if (sv == null) return 0;
		return scalarMultiply(this, sv, caseSensitive);
	}
	
	//	helper method for scalar product computation
	private static int scalarMultiply(StringVector sv1, StringVector sv2, boolean caseSensitive) {
		StringVector dimensions = sv1.union(sv2, caseSensitive);
		int sum = 0;
		for (int i = 0; i < dimensions.size(); i++)
			sum += (sv1.getElementCount(dimensions.get(i), caseSensitive) * sv2.getElementCount(dimensions.get(i), caseSensitive));
		return sum;
	}
	
	/**	read a list of Strings from a file, each line will be an individual element of the resulting StringVector
	 * @param	fileName	the path and name of the file to read the list from
	 * @return a StringVector containing the individual lines of the specified file as its elements 
	 * @throws IOException
	 */
	public static StringVector loadList(String fileName) throws IOException {
		return loadList(fileName, LINE_SEPARATOR);
	}
	
	/**	read a list of Strings from a file, each line will be an individual element of the resulting StringVector
	 * @param	file		the file to read the list from
	 * @return a StringVector containing the individual lines of the specified file as its elements 
	 * @throws IOException
	 */
	public static StringVector loadList(File file) throws IOException {
		return loadList(file, LINE_SEPARATOR);
	}
	
	/**	read a list of Strings from an arbitrary InputStream, each line will be an individual element of the resulting StringVector
	 * @param	input	the InputStream to read the list from
	 * @return a StringVector containing the individual lines from the specified InputStream as its elements 
	 * @throws IOException
	 */
	public static StringVector loadList(InputStream input) throws IOException {
		return loadList(input, LINE_SEPARATOR);
	}
	
	/**	read a list of Strings from an arbitrary Reader, each line will be an individual element of the resulting StringVector
	 * @param	reader	the Reader to read the list from
	 * @return a StringVector containing the individual lines from the specified Reader as its elements 
	 * @throws IOException
	 */
	public static StringVector loadList(Reader reader) throws IOException {
		return loadList(reader, LINE_SEPARATOR);
	}
	
	/**	read a list of Strings from a file, each line will be an individual element of the resulting StringVector
	 * @param	fileName		the path and name of the file to read the list from
	 * @param	lineSeparator	the String that are element internal line breaks substituted with
	 * @return a StringVector containing the individual lines of the specified file as its elements 
	 * @throws IOException
	 */
	public static StringVector loadList(String fileName, String lineSeparator) throws IOException {
		FileReader fr = new FileReader(fileName);
		StringVector sv = loadList(fr, lineSeparator);
		fr.close();
		return sv;
	}
	
	/**	read a list of Strings from a file, each line will be an individual element of the resulting StringVector
	 * @param	file			the file to read the list from
	 * @param	lineSeparator	the String that are element internal line breaks substituted with
	 * @return a StringVector containing the individual lines of the specified file as its elements 
	 * @throws IOException
	 */
	public static StringVector loadList(File file, String lineSeparator) throws IOException {
		FileReader fr = new FileReader(file);
		StringVector sv = loadList(fr, lineSeparator);
		fr.close();
		return sv;
	}
	
	/**	read a list of Strings from an InputStream, each line will be an individual element of the resulting StringVector
	 * @param	input			the InputStream to read the list from
	 * @param	lineSeparator	the String that are element internal line breaks substituted with
	 * @return a StringVector containing the individual lines of the specified InputStream as its elements 
	 * @throws IOException
	 */
	public static StringVector loadList(InputStream input, String lineSeparator) throws IOException {
		return loadList(new InputStreamReader(input), lineSeparator);
	}
	
	/**	read a list of Strings from an arbitrary Reader, each line will be an individual element of the resulting StringVector
	 * @param	reader			the Reader to read the list from
	 * @param	lineSeparator	the String that are element internal line breaks substituted with
	 * @return a StringVector containing the individual lines from the specified Reader as its elements 
	 * @throws IOException
	 */
	public static StringVector loadList(Reader reader, String lineSeparator) throws IOException {
		StringVector sv = new StringVector();
		if (reader == null) return sv;
		BufferedReader buf = ((reader instanceof BufferedReader) ? ((BufferedReader) reader) : new BufferedReader(reader));
		StringVector elementLines = new StringVector();
		String line = buf.readLine();
		while (line != null) {
			elementLines.parseAndAddElements(line, lineSeparator);
			sv.addElement(elementLines.concatStrings("\n"));
			elementLines.clear();
			line = buf.readLine();
		}
		return sv;
	}
	
	/**	the default substitute for element-internal line breaks when storing the StringVector as a list
	 */
	public static final String LINE_SEPARATOR = "<-BR->";
	
	/**	write the content of this StringVector to a file, one element per line
	 * @param	fileName	the path and name of the file to write to 
	 * @throws IOException
	 * Attention:	If an element of this StringVector contains line breaks, it will be written to more than one line.
	 * 				This might result in this element becoming more than one element in a new StringVector produced by the loadList() method.
	 */
	public void storeContent(String fileName) throws IOException {
		this.storeContent(fileName, LINE_SEPARATOR);
	}
	
	/**	write the content of this StringVector to a file, one element per line
	 * @param	file	the path and name of the file to write to 
	 * @throws IOException
	 * Attention:	If an element of this StringVector contains line breaks, it will be written to more than one line.
	 * 				This might result in this element becoming more than one element in a new StringVector produced by the loadList() method.
	 */
	public void storeContent(File file) throws IOException {
		this.storeContent(file, LINE_SEPARATOR);
	}
	
	/**	write the content of this StringVector to an arbitrary output stream, one element per line
	 * @param	out		the output stream to write to 
	 * @throws IOException
	 * Attention:	If an element of this StringVector contains line breaks, it will be written to more than one line.
	 * 				This might result in this element becoming more than one element in a new StringVector produced by the loadList() method.
	 */
	public void storeContent(OutputStream out) throws IOException {
		this.storeContent(out, LINE_SEPARATOR);
	}
	
	/**	write the content of this StringVector to an arbitrary writer, one element per line
	 * @param	writer	the Writer to write to 
	 * @throws IOException
	 * Attention:	If an element of this StringVector contains line breaks, it will be written to more than one line.
	 * 				This might result in this element becoming more than one element in a new StringVector produced by the loadList() method.
	 */
	public void storeContent(Writer writer) throws IOException {
		this.storeContent(writer, LINE_SEPARATOR);
	}
	
	/**	write the content of this StringVector to a file, one element per line
	 * @param	fileName		the path and name of the file to write to 
	 * @param	lineSeparator	the String to use as a substitute for element internal line breaks
	 * @throws IOException
	 * Attention:	If an element of this StringVector contains line breaks, the latter will be substituted with the specified line separator
	 */
	public void storeContent(String fileName, String lineSeparator) throws IOException {
		this.storeContent(new File(fileName), lineSeparator);
	}
	
	/**	write the content of this StringVector to a file, one element per line
	 * @param	file			the path and name of the file to write to 
	 * @param	lineSeparator	the String to use as a substitute for element internal line breaks
	 * @throws IOException
	 * Attention:	If an element of this StringVector contains line breaks, the latter will be substituted with the specified line separator
	 */
	public void storeContent(File file, String lineSeparator) throws IOException {
		FileWriter fw = new FileWriter(file);
		storeContent(fw);
		fw.close();
	}
	
	/**	write the content of this StringVector to an output stream, one element per line
	 * @param	out				the output stream to write to 
	 * @param	lineSeparator	the String to use as a substitute for element internal line breaks
	 * @throws IOException
	 * Attention:	If an element of this StringVector contains line breaks, the latter will be substituted with the specified line separator
	 */
	public void storeContent(OutputStream out, String lineSeparator) throws IOException {
		this.storeContent(new OutputStreamWriter(out), lineSeparator);
	}
	
	/**	write the content of this StringVector to an arbitrary writer, one element per line
	 * @param	writer			the Writer to write to 
	 * @param	lineSeparator	the String to use as a substitute for element internal line breaks
	 * @throws IOException
	 * Attention:	If an element of this StringVector contains line breaks, the latter will be substituted with the specified line separator
	 */
	public void storeContent(Writer writer, String lineSeparator) throws IOException {
		BufferedWriter buf = ((writer instanceof BufferedWriter) ? ((BufferedWriter) writer) : new BufferedWriter(writer));
		StringVector elementLines = new StringVector();
		String separator = (((lineSeparator == null) || (lineSeparator.indexOf('\n') != -1) || (lineSeparator.indexOf('\r') != -1)) ? LINE_SEPARATOR : lineSeparator);
		for (int e = 0; e < this.size(); e++) {
			String element = this.get(e);
			elementLines.parseAndAddElements(element, "\n");
			buf.write(elementLines.concatStrings(separator));
			buf.newLine();
			elementLines.clear();
		}
		buf.flush();
	}
}
