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
package de.uka.ipd.idaho.stringUtils.csvHandler;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Vector;

import de.uka.ipd.idaho.stringUtils.StringUtils;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A representation of CSV data that allows to execute some basic relational
 * algebra (natural join, projection, selection, ordering, and union). This
 * class basically wraps CSV and relational algebra specific functionality
 * around a java.util.Vector object.
 * 
 * @author sautter
 */
public class StringRelation {
	
	//	TODO enable reading Excel 'CSV' files, which are semicolon separated
	
	/* TODO
create readXYZ() methods in StringRelation that allow specifying the sepatator
- defaults to comma ...
- ... but allows specifying a semicolon as well, which is good for reading Microsoft "CSV"
==> helps with tab delimited formats as well
- think of more defaults, e.g. for value delimiter

	 */
	
	private Vector data = new Vector();
	
	/**	Constructor
	 */
	public StringRelation() {}
	
	/*
	 * methods according to java.util.Vector (String instead of Object as type
	 * of parameters and return values)
	 */
	
	/** @see java.util.Vector#add(int, java.lang.Object)
	 */
	public void add(int index, StringTupel s) {
		this.data.add(index, s);
	}
	
	/** @see java.util.Vector#addElement(java.lang.Object)
	 */
	public void addElement(StringTupel s) {
		this.data.addElement(s);
	}
	
	/** @see java.util.Vector#clear()
	 */
	public void clear() {
		this.data.clear();
	}
	
	/** @see java.util.Vector#firstElement()
	 */
	public StringTupel firstElement() {
		return (StringTupel) this.data.firstElement();
	}
	
	/** @see java.util.Vector#get(int)
	 */
	public StringTupel get(int index) {
		return (StringTupel) this.data.get(index);
	}
	
	/** @see java.util.Vector#insertElementAt(java.lang.Object, int)
	 */
	public void insertElementAt(StringTupel s, int index) {
		this.data.insertElementAt(s, index);
	}
	
	/** @see java.util.Vector#lastElement()
	 */
	public StringTupel lastElement() {
		return (StringTupel) this.data.lastElement();
	}
	
	/** @see java.util.Vector#remove(int)
	 */
	public StringTupel remove(int index) {
		return (StringTupel) this.data.remove(index);
	}
	
	/** @see java.util.Vector#remove(java.lang.Object)
	 */
	public void remove(StringTupel s) {
		this.data.remove(s);
	}
	
	/** @see java.util.Vector#removeElementAt(int)
	 */
	public void removeElementAt(int index) {
		this.data.remove(index);
	}
	
	/** @see java.util.Vector#set(int, java.lang.Object)
	 */
	public StringTupel set(int index, StringTupel s) {
		return (StringTupel) this.data.set(index, s);
	}
	
	/** @see java.util.Vector#setElementAt(java.lang.Object, int)
	 */
	public void setElementAt(StringTupel s, int index) {
		this.data.setElementAt(s, index);
	}
	
	/** @see java.util.Vector#isEmpty()
	 */
	public boolean isEmpty() {
		return this.data.isEmpty();
	}
	
	/** @see java.util.Vector#size()
	 */
	public int size() {
		return this.data.size();
	}
	
	/**	@return all the keys assigned to any value in any StringTupel in this StringRelation
	 */
	public StringVector getKeys() {
		StringVector sv = new StringVector();
		for (int t = 0; t < this.size(); t++)
			this.get(t).getKeys(sv);
		return sv;
	}
	
	/**	retrieve all values of a given key
	 * @param	key		the key to retrieve the values for
	 * @return all the values assigned to the specified key in any StringTupel in this StringRelation
	 */
	public StringVector getValues(String key) {
		StringVector sv = new StringVector();
		for (int t = 0; t < this.size(); t++)
			sv.addElement(this.get(t).getValue(key));
		return sv;
	}
	
	/**	get the CSV representation of all tupels contained in this StringRelation
	 * @return a StringVector contioning the CSV representation of all tupels contained in this StringRelation
	 */
	public StringVector getCsvStrings() {
		return this.getCsvStrings('"', false);
	}
	
	/**	get the CSV representation of all tupels contained in this StringRelation
	 * @param	includeKeys		if set to true, the keys will be written to the first line (default is false)
	 * @return a StringVector contioning the CSV representation of all tupels contained in this StringRelation
	 */
	public StringVector getCsvStrings(boolean includeKeys) {
		return this.getCsvStrings('"', includeKeys);
	}
	
	/**	get the CSV representation of all tupels contained in this StringRelation
	 * @param	valueDelimiter	the character to use as the value delimiter (default is '"') (will be escaped with itself if occuring in value)
	 * @return a StringVector contioning the CSV representation of all tupels contained in this StringRelation
	 */
	public StringVector getCsvStrings(char valueDelimiter) {
		return this.getCsvStrings(valueDelimiter, false);
	}
	
	/**	get the CSV representation of all tupels contained in this StringRelation
	 * @param	valueDelimiter	the character to use as the value delimiter (default is '"') (will be escaped with itself if occuring in value)
	 * @param	includeKeys		if set to true, the keys will be written to the first line (default is false)
	 * @return a StringVector contioning the CSV representation of all tupels contained in this StringRelation
	 */
	public StringVector getCsvStrings(char valueDelimiter, boolean includeKeys) {
		StringVector result = new StringVector();
		StringVector keys = this.getKeys();
		
		//	add keys if required
		if (includeKeys) {
			String delimiter = ("" + valueDelimiter);
			StringVector keyValues = new StringVector();
			for (int k = 0; k < keys.size(); k++)
				keyValues.addElement(delimiter + StringUtils.replaceAll(keys.get(k), delimiter, (delimiter + delimiter)) + delimiter);
			result.addElement(keyValues.concatStrings(","));
		}
		
		//	add tupels
		for (int t = 0; t < this.size(); t++)
			result.addElement(this.get(t).toCsvString(valueDelimiter, keys));
		
		return result;
	}
	
	/**	sort the StringTupels, using the values of a key for comparison 
	 * @param	key		the key the values of which to use for comparison
	 */
	public void orderBy(final String key) {
		if (key == null)
			return;
		Collections.sort(this.data, new StringTupelComparator() {
			public int compare(StringTupel st1, StringTupel st2) {
				if (st1 == st2) return 0;
				if (st1 == null) return 1;
				if (st2 == null) return -1;
				String s1 = st1.getValue(key);
				String s2 = st2.getValue(key);
				if (s1 == s2) return 0;
				if (s1 == null) return 1;
				if (s2 == null) return -1;
				return s1.compareToIgnoreCase(s2);
			}
		});
	}
	
	/**	sort the StringTupels, using the values of a set of keys for comparison 
	 * @param	keys	the keys the values of which to use for comparison (will be applied in reverse order so that keys[0] has the highest priority)
	 */
	public void orderBy(StringVector keys) {
		if (keys != null)
			this.orderBy(keys.toStringArray());
	}
	
	/**	sort the StringTupels, using the values of a set of keys for comparison 
	 * @param	keys	the keys the values of which to use for comparison (will be applied in reverse order so that keys[0] has the highest priority)
	 */
	public void orderBy(String[] keys) {
		if (keys != null)
			for (int k = keys.length - 1; k != -1; k--)
				this.orderBy(keys[k]);
	}
	
	/**	join this SringTupelVector with another one
	 * @param	toJoin	the StringTupelvector to join this one with
	 * @return the result of the join
	 */
	public StringRelation join(StringRelation toJoin) {
		return this.join(toJoin, false);
	}
	
	/**	join this SringTupelVector with another one
	 * @param	toJoin	the StringTupelvector to join this one with
	 * @param	outer	include the StringTupels that have not found a join partner
	 * @return the result of the join
	 */
	public StringRelation join(StringRelation toJoin, boolean outer) {
		return this.join(toJoin, outer, outer);
	}
	
	/**	join this SringTupelVector with another one
	 * @param	toJoin		the StringTupelvector to join this one with
	 * @param	leftOuter	include the StringTupels from this StringRelation that have not found a join partner
	 * @param	rightOuter	include the StringTupels from the specified StringRelation that have not found a join partner
	 * @return the result of the join
	 */
	public StringRelation join(StringRelation toJoin, boolean leftOuter, boolean rightOuter) {
		//	get keys for natural join
		StringVector ownKeys = this.getKeys();
		StringVector otherKeys = toJoin.getKeys();
		return this.join(toJoin, leftOuter, rightOuter, ownKeys.intersect(otherKeys));
	}
	
	/**	join this SringTupelVector with another one
	 * @param	toJoin		the StringTupelvector to join this one with
	 * @param	leftOuter	include the StringTupels from this StringRelation that have not found a join partner
	 * @param	rightOuter	include the StringTupels from the specified StringRelation that have not found a join partner
	 * @param	keys		the keys to use for the join
	 * @return the result of the join
	 */
	public StringRelation join(StringRelation toJoin, boolean leftOuter, boolean rightOuter, StringVector keys) {
		if ((toJoin == null) || (toJoin == this)) return this;
		if (toJoin.size() == 0) return (leftOuter ? this : new StringRelation());
		if (this.size() == 0) return (rightOuter ? toJoin : new StringRelation());
		
		//	generate keys
		StringTupel tupel;
		Integer hash;
		ArrayList ownKeys = new ArrayList(this.size());
		IdentityHashMap ownData = new IdentityHashMap(this.size());
		for (int t = 0; t < this.size(); t++) {
			tupel = this.get(t);
			hash = new Integer(tupel.toCsvString(keys).hashCode());
			ownKeys.add(hash);
			ownData.put(hash, tupel);
		}
		ArrayList otherKeys = new ArrayList(toJoin.size());
		IdentityHashMap otherData = new IdentityHashMap(toJoin.size());
		for (int t = 0; t < toJoin.size(); t++) {
			tupel = toJoin.get(t);
			hash = new Integer(tupel.toCsvString(keys).hashCode());
			otherKeys.add(hash);
			otherData.put(hash, tupel);
		}
		tupel = null;
		hash = null;
		
		//	sort keys
		Collections.sort(ownKeys);
		Collections.sort(otherKeys);
		
		//	perform join
		StringRelation result = new StringRelation();
		int ownIndex = 0;
		int otherIndex = 0;
		while ((ownIndex < ownKeys.size()) && (otherIndex < otherKeys.size())) {
			Integer ownKey = ((Integer) ownKeys.get(ownIndex));
			Integer otherKey = ((Integer) otherKeys.get(otherIndex));
			
			int c = ownKey.compareTo(otherKey);
			
			if (c < 0) {
				ownIndex++;
				if (leftOuter) result.addElement((StringTupel) ownData.get(ownKey));
			}
			
			else if (c == 0) {
				ownIndex++;
				result.addElement(((StringTupel) ownData.get(ownKey)).join((StringTupel) otherData.get(otherKey)));
				
				int i = 1;
				while ((c == 0) && ((otherIndex + i) < toJoin.size())) {
					otherKey = ((Integer) otherKeys.get(otherIndex + i));
					c = ownKey.compareTo(otherKey);
					if (c == 0) result.addElement(((StringTupel) ownData.get(ownKey)).join((StringTupel) otherData.get(otherKey)));
					i++;
				}
			}
			
			else {
				otherIndex++;
				if (rightOuter) result.addElement((StringTupel) otherData.get(otherKey));
			}
		}
		
		//	add rest of tupels
		while (leftOuter && (ownIndex < ownKeys.size())) {
			result.addElement((StringTupel) ownData.get(ownKeys.get(ownIndex)));
			ownIndex++;
		}
		while (rightOuter && (otherIndex < otherKeys.size())) {
			result.addElement((StringTupel) otherData.get(otherKeys.get(otherIndex)));
			otherIndex++;
		}
		
		//	clean up
		ownKeys.clear();
		ownData.clear();
		otherKeys.clear();
		otherData.clear();
		System.gc();
		
		return result;
	}
	
	/**	union a StringRelation with this one
	 * @param	stv		the StringRelation to union this one with
	 * @return a new StringRelation containing the StringTupels both from this and the argument StringRelation
	 */
	public StringRelation union(StringRelation stv) {
		if ((stv == null) || (stv == this)) return this;
		if (stv.size() == 0) return this;
		if (this.size() == 0) return stv;
		
		StringRelation result = new StringRelation();
		for (int t = 0; t < this.size(); t++)
			result.addElement(this.get(t));
		for (int t = 0; t < stv.size(); t++)
			result.addElement(stv.get(t));
		result.removeDuplicates();
		
		return result;
	}
	
	/**	select all StringTupels from this StringTupelvector that match the given filtering tupel (i.e. have all the keys contained in the filter mapped to the same values as the filter)
	 * @param	filter	the StringTupel to use as a filter
	 * @return all StringTupels from this StringRelation that match the specified filtering tupel
	 */
	public StringRelation filter(StringTupel filter) {
		StringRelation result = new StringRelation();
		for (int t = 0; t < this.size(); t++) {
			StringTupel tupel = this.get(t);
			if (tupel.matches(filter))
				result.addElement(tupel);
		}
		return result;
	}
	
	/**	select all StringTupels from this StringTupelvector that match the given filter
	 * @param	filter	the StringTupelFilter to use for selection
	 * @return all StringTupels from this StringRelation that match the specified filter
	 */
	public StringRelation filter(StringTupelFilter filter) {
		if (filter == null) return this;
		StringRelation result = new StringRelation();
		for (int t = 0; t < this.size(); t++) {
			StringTupel tupel = this.get(t);
			if (filter.matches(tupel))
				result.addElement(tupel);
		}
		return result;
	}
	
	/**	project this StringRelation to a given set of keys
	 * @param	keys	the keys to retain
	 * @return a new StringRelation whose tupels contain only the values of the specified keys
	 */
	public StringRelation project(StringVector keys) {
		StringVector remainingKeys = keys.intersect(this.getKeys());
		StringRelation result = new StringRelation();
		for (int t = 0; t < this.size(); t++)
			result.addElement(this.get(t).project(remainingKeys));
		return result;
	}
	
	/**	rename a key in all tupels contained in this StringRelation
	 * @param	key		the key to replace
	 * @param	newKey	the String to replace the key with
	 */
	public void renameKey(String key, String newKey) {
		if ((key == null) || (newKey == null))
			return;
		for (int t = 0; t < this.size(); t++)
			this.get(t).renameKey(key, newKey);
	}
	
	/**	remove the values assigned to a given key in all tupels contained in this StringRelation
	 * @param	key		the key of the desired value
	 */
	public void removeValue(String key) {
		for (int t = 0; t < this.size(); t++)
			this.get(t).removeValue(key);
	}
	
	/**	remove all duplicates from this StringRelation
	 */
	public void removeDuplicates() {
		if (this.size() < 2)
			return;
		
		//	generate & sort keys
		StringVector keys = this.getKeys();
		StringTupel tupel;
		Integer hash;
		ArrayList tupelKeys = new ArrayList(this.size());
		IdentityHashMap tupelPositions = new IdentityHashMap(this.size());
		for (int t = 0; t < this.size(); t++) {
			tupel = this.get(t);
			hash = new Integer(tupel.toCsvString(keys).hashCode());
			tupelKeys.add(hash);
			tupelPositions.put(hash, new Integer(t));
		}
		Collections.sort(tupelKeys);
		
		//	collect duplicate indices
		ArrayList removeIndices = new ArrayList();
		Integer first = ((Integer) tupelKeys.get(0));
		Integer second;
		int index = 1;
		while (index < tupelKeys.size()) {
			second = ((Integer) tupelKeys.get(index));
			if (first.equals(second)) {
				tupelKeys.remove(index);
				removeIndices.add(tupelPositions.remove(second));
			}
			else {
				first = second;
				index ++;
			}
		}
		
		//	remove duplicates
		Collections.sort(removeIndices);
		Collections.reverse(removeIndices);
		for (int r = 0; r < removeIndices.size(); r++)
			this.remove(((Integer) removeIndices.get(r)).intValue());
		
		//	tidy up
		tupelKeys.clear();
		tupelPositions.clear();
		removeIndices.clear();
		System.gc();
	}
	
	/**	invert a StringRelation so that the columns become the lines, and vice versa 
	 * @param	data	the StringRelation to invert
	 * @return a StringRelation representing the inverse of the specified one, column 0 containing the original keys, the original tupels in columns 1 through n
	 */
	public static StringRelation invert(StringRelation data) {
		return invert(data, data.getKeys());
	}
	
	/**	invert a StringRelation so that the columns become the lines, and vice versa 
	 * @param	data	the StringRelation to invert
	 * @param	keys	the keys to use for the inversion
	 * @return a StringRelation representing the inverse of the specified one, column 0 containing the original keys, the original tupels in columns 1 through n
	 */
	public static StringRelation invert(StringRelation data, StringVector keys) {
		StringRelation inverse = new StringRelation();
		
		for (int k = 0; k < keys.size(); k++)
			inverse.addElement(new StringTupel());
		
		int keyLength = ((int) Math.floor(Math.log10(data.size() - 1)) + 1);
		for (int t = 0; t < data.size(); t++) {
			StringTupel dataTupel = data.get(t);
			for (int k = 0; k < keys.size(); k++) {
				if (t == 0) {
					String key = "0";
					while (key.length() < keyLength) key = ("0" + key);
					inverse.get(k).setValue(key, keys.get(k));
				}
				
				String key = ("" + (t + 1));
				while (key.length() < keyLength) key = ("0" + key);
				inverse.get(k).setValue(key, dataTupel.getValue(keys.get(k), ""));
			}
		}
		return inverse;
	}
	
	private static final StringTupelFilter TRUE_FILTER = new StringTupelFilter() {
		public boolean matches(StringTupel st) {
			return true;
		}
	};
	
	/**
	 * The default separator character, the comma
	 */
	public static final char DEFAULT_SEPARATOR = ',';
	
	/**
	 * The default value delimiter character, the double quote
	 */
	public static final char DEFAULT_VALUE_DELIMITER = '"';
	
	private static final boolean DEFAULT_FIRST_LINE_IS_KEYS = true;
	
	/**
	 * Specifying this constant as the value separator to any of the
	 * readCsvData() methods will cause the implementation to try and deduce the
	 * actual separator character from the first 8192 bytes of the input
	 * provided, choosing between comma, semicolon (used in MS Excel's 'CSV'
	 * format), and tab. This is for situations where CSV data from third paries
	 * has to be loaded and the actual separator character is not known at
	 * implementation time.
	 */
	public static final char GUESS_SEPARATOR = '\u0000';
	
	//	data structures for separator guessing
	private static final char[] SEPARATORS = {',', ';', '\t'};
	private static final String[] SEPARATOR_IN_REGEX = {"\\,", "\\;", "\\t"};

	/**
	 * Read a CSV file and add its content to a StringRelation. This method
	 * assumes comma separated and double quote delimited values in platform
	 * default encoding, with the keys being given as the first line of the
	 * file.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding, with the keys being given as the first line of the file.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding, with the keys being given as the first line of the file.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding, with the keys being given as the first line of the file.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, StringVector keys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, boolean firstLineIsKeys, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter, StringVector keys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * comma separated values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param	file				the file to read
	 * @param	valueDelimiter		the value delimiter character
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter) throws IOException {
		return addCsvData(data, file, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * values in platform default encoding, with the keys being given as the
	 * first line of the file.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * values in platform default encoding, with the keys being given as the
	 * first line of the file.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * values in platform default encoding, with the keys being given as the
	 * first line of the file.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter, StringVector keys) throws IOException {
		return addCsvData(data, file, separator, valueDelimiter, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return addCsvData(data, file, separator, valueDelimiter, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, separator, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, file, separator, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, final StringTupel filter, StringVector projectionKeys) throws IOException {
		StringTupelFilter stf = new StringTupelFilter() {
			public boolean matches(StringTupel st) {
				return st.matches(filter);
			}
		};
		return addCsvData(data, file, separator, valueDelimiter, firstLineIsKeys, keys, stf, projectionKeys);
	}
	
	/**
	 * Read a CSV file and add its content to StringRelation. This method assumes
	 * values in platform default encoding.
	 * @param data the string relation to add the file content to 
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, File file, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		Reader input = new FileReader(file);
		int added = addCsvData(data, input, separator, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
		input.close();
		return added;
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values, with
	 * the keys being given as the first line.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values, with
	 * the keys being given as the first line.
	 * @param data the string relation to add the data to 
	 * @param data the string relation to add the file content to 
	 * @param input the reader to read from
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values, with
	 * the keys being given as the first line.
	 * @param data the string relation to add the data to 
	 * @param data the string relation to add the file content to 
	 * @param input the reader to read from
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values.
	 * @param data the string relation to add the data to 
	 * @param data the string relation to add the file content to 
	 * @param input the reader to read from
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, StringVector keys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values.
	 * @param data the string relation to add the data to 
	 * @param data the string relation to add the file content to 
	 * @param input the reader to read from
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values.
	 * @param input the reader to read from
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, boolean firstLineIsKeys, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated and double quote delimited values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values, with the keys being given as the
	 * first line.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values, with the keys being given as the
	 * first line.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter, StringVector keys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes comma separated values.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes the keys to be given as the first line.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter) throws IOException {
		return addCsvData(data, input, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes the keys to be given as the first line.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation. This
	 * method assumes the keys to be given as the first line.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter, StringVector keys) throws IOException {
		return addCsvData(data, input, separator, valueDelimiter, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return addCsvData(data, input, separator, valueDelimiter, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, separator, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return addCsvData(data, input, separator, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and add it to StringRelation.
	 * @param data the string relation to add the data to 
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, final StringTupel filter, StringVector projectionKeys) throws IOException {
		StringTupelFilter stf = new StringTupelFilter() {
			public boolean matches(StringTupel st) {
				return st.matches(filter);
			}
		};
		return addCsvData(data, input, separator, valueDelimiter, firstLineIsKeys, keys, stf, projectionKeys);
	}
	
	/**
	 * Read CSV data from a reader and add it to StringRelation.
	 * @param data the string relation to add the data to 
	 * @param input the Reader to read from
	 * @param separator the separator character, usually ,
	 * @param valueDelimiter the value delimiter character, usually &quot;
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return the number of tupels added
	 * @throws IOException
	 */
	public static int addCsvData(StringRelation data, Reader input, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		int oldDataSize = data.size();
		
		if (separator == GUESS_SEPARATOR) {
			input = ((input instanceof BufferedReader) ? input : new BufferedReader(input));
			separator = guessSeparator(((BufferedReader) input), valueDelimiter, keys, projectionKeys);
		}
		
		StringVector effectiveKeys = keys;
		boolean gotKeys;
		
		if (firstLineIsKeys) {
			gotKeys = false;
			effectiveKeys = null;
		}
		else {
			gotKeys = true;
			if (effectiveKeys == null)
				effectiveKeys = new StringVector();
		}
		
		int next = input.read();
		char lastChar = '\u0000';
		char currentChar;
		char nextChar = ((char) next);
		
		boolean quoted = false;
		boolean escaped = false;
		
		StringVector tupelValues = new StringVector();
		StringBuffer valueAssembler = new StringBuffer();
		
		while (next != -1) {
			currentChar = nextChar;
			next = input.read();
			nextChar = ((next == -1) ? '\u0000' : ((char) next));
			
			//	escaped character
			if (escaped) {
				escaped = false;
				valueAssembler.append(currentChar);
			}
			
			//	start or end of quoted value
			else if (currentChar == valueDelimiter) {
				if (quoted) {
					if (nextChar == valueDelimiter)
						escaped = true;
					else if ((nextChar == separator) || (nextChar == '\n') || (nextChar == '\r'))
						quoted = false;
				}
				else quoted = true;
			}
			
			//	in quoted value
			else if (quoted)
				valueAssembler.append(currentChar);
			
			//	end of value
			else if ((currentChar == separator)) {
				tupelValues.addElement((lastChar == valueDelimiter) ? valueAssembler.toString() : valueAssembler.toString().trim());
				valueAssembler = new StringBuffer();
			}
			
			//	end of tupel
			else if ((currentChar == '\n') || (currentChar == '\r') || (next == -1)) {
				if (valueAssembler.length() != 0) {
					tupelValues.addElement((lastChar == valueDelimiter) ? valueAssembler.toString() : valueAssembler.toString().trim());
					valueAssembler = new StringBuffer();
				}
				if (tupelValues.size() != 0) {
					if (!gotKeys) {
						if (effectiveKeys == null) {
							effectiveKeys = tupelValues;
							tupelValues = new StringVector();
						}
						gotKeys = true;
					}
					else {
						StringTupel st = new StringTupel();
						for (int i = 0; (i < tupelValues.size()) || (i < effectiveKeys.size()); i++)
							st.setValue(((i < effectiveKeys.size()) ? effectiveKeys.get(i) : ("" + i)), ((i < tupelValues.size()) ? tupelValues.get(i) : ""));
						if ((filter == null) || filter.matches(st))
							data.addElement((projectionKeys == null) ? st : st.project(projectionKeys));
					}
					tupelValues.clear();
				}
			}
			
			//	other char
			else valueAssembler.append(currentChar);
			
			//	remember char
			lastChar = currentChar;
		}
		
		//	check if data left in buffers
		if (valueAssembler.length() != 0)
			tupelValues.addElement(valueAssembler.toString());
		if (tupelValues.size() != 0) {
			StringTupel st = new StringTupel();
			for (int i = 0; (i < tupelValues.size()) || (i < effectiveKeys.size()); i++)
				st.setValue(((i < effectiveKeys.size()) ? effectiveKeys.get(i) : ("" + i)), ((i < tupelValues.size()) ? tupelValues.get(i) : ""));
			if ((filter == null) || filter.matches(st))
				data.addElement((projectionKeys == null) ? st : st.project(projectionKeys));
		}
		
		return (data.size() - oldDataSize);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding, with the keys being given as the first line of the file.
	 * @param file the file to read
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding, with the keys being given as the first line of the file.
	 * @param file the file to read
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding, with the keys being given as the first line of the file.
	 * @param file the file to read
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding, with the keys being given as the first line of the file.
	 * @param file the file to read
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, StringVector keys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param file the file to read
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param file the file to read
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param file the file to read
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param file the file to read
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, boolean firstLineIsKeys, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated and double quote delimited values in platform default
	 * encoding.
	 * @param file the file to read
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter, StringVector keys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * comma separated values in platform default encoding.
	 * @param	file				the file to read
	 * @param	valueDelimiter		the value delimiter character
	 * @return a StringRelation containing the StringTupels read from the specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter) throws IOException {
		return readCsvData(file, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * values in platform default encoding, with the keys being given as the
	 * first line of the file.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * values in platform default encoding, with the keys being given as the
	 * first line of the file.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * values in platform default encoding, with the keys being given as the
	 * first line of the file.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter, StringVector keys) throws IOException {
		return readCsvData(file, separator, valueDelimiter, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return readCsvData(file, separator, valueDelimiter, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, separator, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(file, separator, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, final StringTupel filter, StringVector projectionKeys) throws IOException {
		StringTupelFilter stf = new StringTupelFilter() {
			public boolean matches(StringTupel st) {
				return st.matches(filter);
			}
		};
		return readCsvData(file, separator, valueDelimiter, firstLineIsKeys, keys, stf, projectionKeys);
	}
	
	/**
	 * Read a CSV file and create a StringRelation from it. This method assumes
	 * values in platform default encoding.
	 * @param file the file to read
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(File file, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		Reader input = new FileReader(file);
		StringRelation fileContent = readCsvData(input, separator, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
		input.close();
		return fileContent;
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values, with
	 * the keys being given as the first line.
	 * @param input the reader to read from
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values, with
	 * the keys being given as the first line.
	 * @param input the reader to read from
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values, with
	 * the keys being given as the first line.
	 * @param input the reader to read from
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values.
	 * @param input the reader to read from
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the file's first line)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, StringVector keys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values.
	 * @param input the reader to read from
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values.
	 * @param input the reader to read from
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values.
	 * @param input the reader to read from
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values.
	 * @param input the reader to read from
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, boolean firstLineIsKeys, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated and double quote delimited values.
	 * @param input the reader to read from
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values, with the keys being given as the
	 * first line.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values, with the keys being given as the
	 * first line.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter, StringVector keys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes comma separated values.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, DEFAULT_SEPARATOR, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes the keys to be given as the first line.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter) throws IOException {
		return readCsvData(input, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes the keys to be given as the first line.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it. This
	 * method assumes the keys to be given as the first line.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, separator, valueDelimiter, DEFAULT_FIRST_LINE_IS_KEYS, null, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter, StringVector keys) throws IOException {
		return readCsvData(input, separator, valueDelimiter, (keys == null), keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys) throws IOException {
		return readCsvData(input, separator, valueDelimiter, firstLineIsKeys, keys, TRUE_FILTER, null);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter, StringVector keys, StringTupel filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, separator, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		return readCsvData(input, separator, valueDelimiter, (keys == null), keys, filter, projectionKeys);
	}
	
	/**
	 * Read a CSV data from a reader and create a StringRelation from it.
	 * @param input the reader to read from
	 * @param valueDelimiter the value delimiter character
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, final StringTupel filter, StringVector projectionKeys) throws IOException {
		StringTupelFilter stf = new StringTupelFilter() {
			public boolean matches(StringTupel st) {
				return st.matches(filter);
			}
		};
		return readCsvData(input, separator, valueDelimiter, firstLineIsKeys, keys, stf, projectionKeys);
	}
	
	/**
	 * Read CSV data from a reader and create a StringRelation from it.
	 * @param input the Reader to read from
	 * @param separator the separator character, usually ,
	 * @param valueDelimiter the value delimiter character, usually &quot;
	 * @param firstLineIsKeys if set to true, the first line of the file is
	 *            treated as keys instead of a tupel
	 * @param keys the keys to use for the values (if set to null, the keys are
	 *            extracted from the first line of input if firstLineIsKeys is
	 *            true, if the latter is false, the column numbers are used as
	 *            keys)
	 * @param filter a StringTupel serving as a filter for the tupels read (if
	 *            it is not null, only tupels matching the filter are added to
	 *            the resulting StringRelation; matching is done by the
	 *            StringTupel.matches(StringTupel filter) method)
	 * @param projectionKeys a StringVector containing keys to restrict the
	 *            resulting StringRelation to (if it is not null, each
	 *            StringTupel is projected to the specified keys via its
	 *            project(StringVector keys) method)
	 * @return a StringRelation containing the StringTupels read from the
	 *         specified file, eventually filtered and/or projected according to
	 *         the specified filter or projection key list
	 * @throws IOException
	 */
	public static StringRelation readCsvData(Reader input, char separator, char valueDelimiter, boolean firstLineIsKeys, StringVector keys, StringTupelFilter filter, StringVector projectionKeys) throws IOException {
		StringRelation data = new StringRelation();
		addCsvData(data, input, separator, valueDelimiter, firstLineIsKeys, keys, filter, projectionKeys);
		return data;
//		
//		if (separator == GUESS_SEPARATOR) {
//			input = ((input instanceof BufferedReader) ? input : new BufferedReader(input));
//			separator = guessSeparator(((BufferedReader) input), valueDelimiter, keys, projectionKeys);
//		}
//		
//		StringRelation fileContent = new StringRelation();
//		
//		StringVector effectiveKeys = keys;
//		boolean gotKeys;
//		
//		if (firstLineIsKeys) {
//			gotKeys = false;
//			effectiveKeys = null;
//		}
//		else {
//			gotKeys = true;
//			if (effectiveKeys == null)
//				effectiveKeys = new StringVector();
//		}
//		
//		int next = input.read();
//		char lastChar = '\u0000';
//		char currentChar;
//		char nextChar = ((char) next);
//		
//		boolean quoted = false;
//		boolean escaped = false;
//		
//		StringVector tupelValues = new StringVector();
//		StringBuffer valueAssembler = new StringBuffer();
//		
//		while (next != -1) {
//			currentChar = nextChar;
//			next = input.read();
//			nextChar = ((next == -1) ? '\u0000' : ((char) next));
//			
//			//	escaped character
//			if (escaped) {
//				escaped = false;
//				valueAssembler.append(currentChar);
//			}
//			
//			//	start or end of quoted value
//			else if (currentChar == valueDelimiter) {
//				if (quoted) {
//					if (nextChar == valueDelimiter)
//						escaped = true;
//					else if ((nextChar == separator) || (nextChar == '\n') || (nextChar == '\r'))
//						quoted = false;
//				}
//				else quoted = true;
//			}
//			
//			//	in quoted value
//			else if (quoted)
//				valueAssembler.append(currentChar);
//			
//			//	end of value
//			else if ((currentChar == separator)) {
//				tupelValues.addElement((lastChar == valueDelimiter) ? valueAssembler.toString() : valueAssembler.toString().trim());
//				valueAssembler = new StringBuffer();
//			}
//			
//			//	end of tupel
//			else if ((currentChar == '\n') || (currentChar == '\r') || (next == -1)) {
//				if (valueAssembler.length() != 0) {
//					tupelValues.addElement((lastChar == valueDelimiter) ? valueAssembler.toString() : valueAssembler.toString().trim());
//					valueAssembler = new StringBuffer();
//				}
//				if (tupelValues.size() != 0) {
//					if (!gotKeys) {
//						if (effectiveKeys == null) {
//							effectiveKeys = tupelValues;
//							tupelValues = new StringVector();
//						}
//						gotKeys = true;
//					}
//					else {
//						StringTupel st = new StringTupel();
//						for (int i = 0; (i < tupelValues.size()) || (i < effectiveKeys.size()); i++)
//							st.setValue(((i < effectiveKeys.size()) ? effectiveKeys.get(i) : ("" + i)), ((i < tupelValues.size()) ? tupelValues.get(i) : ""));
//						if ((filter == null) || filter.matches(st))
//							fileContent.addElement((projectionKeys == null) ? st : st.project(projectionKeys));
//					}
//					tupelValues.clear();
//				}
//			}
//			
//			//	other char
//			else valueAssembler.append(currentChar);
//			
//			//	remember char
//			lastChar = currentChar;
//		}
//		
//		//	check if data left in buffers
//		if (valueAssembler.length() != 0)
//			tupelValues.addElement(valueAssembler.toString());
//		if (tupelValues.size() != 0) {
//			StringTupel st = new StringTupel();
//			for (int i = 0; (i < tupelValues.size()) || (i < effectiveKeys.size()); i++)
//				st.setValue(((i < effectiveKeys.size()) ? effectiveKeys.get(i) : ("" + i)), ((i < tupelValues.size()) ? tupelValues.get(i) : ""));
//			if ((filter == null) || filter.matches(st))
//				fileContent.addElement((projectionKeys == null) ? st : st.project(projectionKeys));
//		}
//		
//		return fileContent;
	}
	
	private static char guessSeparator(BufferedReader input, char valueDelimiter, StringVector keys, StringVector projectionKeys) throws IOException {
		
		//	get first 8K of input for analysis
		input.mark(8192);
		char[] chars = new char[8192];
		int read = input.read(chars);
		String str = new String(chars, 0, read);
		input.reset();
		
		//	count sequences of candidate separators (occur for unquoted empty values)
		int[] twoSepCount = new int[SEPARATORS.length];
		for (int s = 0; s < SEPARATORS.length; s++)
			twoSepCount[s] = countOccurrences(str, (SEPARATORS[s] + "" + SEPARATORS[s]), false);
		
		//	count candidate separators between regular value characters
		int[] valSepCount = new int[SEPARATORS.length];
		for (int s = 0; s < SEPARATORS.length; s++)
			valSepCount[s] = countOccurrences(str, ("[a-zA-Z0-9\\-\\_\\" + valueDelimiter + "]" + SEPARATOR_IN_REGEX[s] + "[a-zA-Z0-9\\-\\_\\" + valueDelimiter + "]"), true);
		
		//	count candidate separators
		int[] sepCount = new int[SEPARATORS.length];
		for (int s = 0; s < SEPARATORS.length; s++)
			sepCount[s] = countOccurrences(str, ("" + SEPARATORS[s]), false);
		
		//	evaluate statistics
		char sep = GUESS_SEPARATOR;
		int sepScore = 1; // TODO figure out if this threshold makes sense, or if it should be higher
		
		//	try 'strong' evidence first
		for (int s = 0; s < SEPARATORS.length; s++)
			if ((twoSepCount[s] + valSepCount[s]) > sepScore) {
				sep = SEPARATORS[s];
				sepScore = (twoSepCount[s] + valSepCount[s]);
				System.out.println("Strong separator guess: '" + sep + "'");
			}
		if (sep != GUESS_SEPARATOR)
			return sep;
		
		//	resort to 'weak' evidence
		for (int s = 0; s < SEPARATORS.length; s++)
			if (sepCount[s] > sepScore) {
				sep = SEPARATORS[s];
				sepScore = sepCount[s];
				System.out.println("Weak separator guess: '" + sep + "'");
			}
		if (sep != GUESS_SEPARATOR)
			return sep;
		
		//	could not determine separator, use default TODO think of throwing an IOException instead
		return DEFAULT_SEPARATOR;
	}
	
	private static int countOccurrences(String str, String suf, boolean isRegEx) {
		if (suf.length() == 0)
			return 0;
		else if (isRegEx) {
			String[] parts = str.split(suf, -1);
			return parts.length;
		}
		else {
			int c = 0;
			int s = -1;
			while ((s = str.indexOf(suf, (s+1))) != -1)
				c++;
			return c;
		}
	}
	
//	public static void main(String[] args) throws Exception {
//		Reader r = new FileReader("E:/Testdaten/EcologyTestbed/crops.test.csv");
////		Reader r = new FileReader("E:/Testdaten/EcologyTestbed/crops.csv");
//		char sep = guessSeparator(new BufferedReader(r), '"', null, null);
//		System.out.println("Guessed separator: '" + sep + "'");
//	}
	
	/**
	 * Write a StringRelation to a Writer, delimiting values with double quotes
	 * and separating them with commas. Keys are written to the first line.
	 * @param output the Writer to write the data to
	 * @param data the StringRelation to be stored
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data) throws IOException {
		return writeCsvData(output, data, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, DEFAULT_FIRST_LINE_IS_KEYS);
	}
	
	/**
	 * Write a StringRelation to a Writer, delimiting values with double quotes
	 * and separating them with commas.
	 * @param	output		the Writer to write the data to
	 * @param	data		the StringRelation to be stored
	 * @param	writeKeys	if set to true, the keys are written as the first line (default is true)
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data, boolean writeKeys) throws IOException {
		return writeCsvData(output, data, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, writeKeys);
	}
	
	/**
	 * Write a StringRelation to a Writer, delimiting values with double quotes
	 * and separating them with commas. Keys are written as the first line of
	 * output.
	 * @param output the Writer to write the data to
	 * @param data the StringRelation to be stored
	 * @param keys the keys to use for writing
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data, StringVector keys) throws IOException {
		return writeCsvData(output, data, DEFAULT_SEPARATOR, DEFAULT_VALUE_DELIMITER, keys);
	}
	
	/**
	 * Write a StringRelation to a Writer, separating values them with commas.
	 * Keys are written as the first line of output.
	 * @param output the Writer to write the data to
	 * @param data the StringRelation to be stored
	 * @param valueDelimiter the value delimiter character
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data, char valueDelimiter) throws IOException {
		return writeCsvData(output, data, DEFAULT_SEPARATOR, valueDelimiter, true);
	}
	
	/**
	 * Write a StringRelation to a Writer, separating values with commas.
	 * @param output the Writer to write the data to
	 * @param data the StringRelation to be stored
	 * @param valueDelimiter the value delimiter character
	 * @param writeKeys if set to true, the keys are written as the first line
	 *            of output
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data, char valueDelimiter, boolean writeKeys) throws IOException {
		return writeCsvData(output, data, DEFAULT_SEPARATOR, valueDelimiter, (writeKeys ? data.getKeys() : null));
	}
	
	/**
	 * Write a StringRelation to a Writer, separating values with commas. Keys
	 * are written as the first line of output.
	 * @param output the Writer to write the data to
	 * @param data the StringRelation to be stored
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for writing
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data, char valueDelimiter, StringVector keys) throws IOException {
		return writeCsvData(output, data, DEFAULT_SEPARATOR, valueDelimiter, keys);
	}
	
	/**
	 * Write a StringRelation to a Writer, separating values them with a custom
	 * character. Keys are written as the first line of output.
	 * @param output the Writer to write the data to
	 * @param data the StringRelation to be stored
	 * @param separator the value separator character
	 * @param valueDelimiter the value delimiter character
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data, char separator, char valueDelimiter) throws IOException {
		return writeCsvData(output, data, separator, valueDelimiter, true);
	}

	/**
	 * Write a StringRelation to a Writer, separating values with a custom
	 * character.
	 * @param output the Writer to write the data to
	 * @param data the StringRelation to be stored
	 * @param separator the value separator character
	 * @param valueDelimiter the value delimiter character
	 * @param writeKeys if set to true, the keys are written as the first line
	 *            of output
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data, char separator, char valueDelimiter, boolean writeKeys) throws IOException {
		return writeCsvData(output, data, separator, valueDelimiter, (writeKeys ? data.getKeys() : null));
	}
	
	/**
	 * Write a StringRelation to a Writer, separating values with a custom
	 * character. Keys are written as the first line of output.
	 * @param output the Writer to write the data to
	 * @param data the StringRelation to be stored
	 * @param separator the value separator character
	 * @param valueDelimiter the value delimiter character
	 * @param keys the keys to use for writing
	 * @return true if and only if the data is written successfully
	 * @throws IOException
	 */
	public static boolean writeCsvData(Writer output, StringRelation data, char separator, char valueDelimiter, StringVector keys) throws IOException {
		StringVector writingKeys = ((keys == null) ? data.getKeys() : keys);
		
		BufferedWriter buf = ((output instanceof BufferedWriter) ? ((BufferedWriter) output) : new BufferedWriter(output));
		
		//	write keys if required
		if (keys != null) {
			String keyString = ("" + valueDelimiter + writingKeys.concatStrings("" + valueDelimiter + "" + separator + "" + valueDelimiter) + valueDelimiter);
			buf.write(keyString);
			buf.newLine();
		}
		
		//	write tupels
		for (int t = 0; t < data.size(); t++) {
			StringTupel st = data.get(t);
			String tupelString = st.toCsvString(separator, valueDelimiter, writingKeys);
			int offset = 0;
			int length = 0;
			char c;
			boolean lastWasNewLine = false;
			for (int i = 0; i < tupelString.length(); i++) {
				c = tupelString.charAt(i);
				if ((c == '\n') || (c == '\r')) {
					if (lastWasNewLine) offset++;
					else {
						buf.write(tupelString, offset, length);
						offset += length;
						length = 0;
						buf.newLine();
						offset++;
						lastWasNewLine = true;
					}
				}
				else {
					length ++;
					lastWasNewLine = false;
				}
			}
			if (length != 0) {
				buf.write(tupelString, offset, length);
				buf.newLine();
			}
		}
		if (buf != output)
			buf.flush();
		return true;
	}
}
