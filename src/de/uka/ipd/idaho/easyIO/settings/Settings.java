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
package de.uka.ipd.idaho.easyIO.settings;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * A Settings object is quite similar to a java.util.Properties (and can act as
 * one), except for the capability of working with hierarchical data. The
 * hierarchy is expressed with prefixes. Each Settings object can ahve an
 * arbitrary number of distinct sub sets, which are distinguished within the
 * scope of the parent object by means of prefixes: Each sub set has its own
 * prefix. Within the parent object, all settings belonging to a given sub set
 * with prefix 'prefix' are identified (and put out) as
 * 'prefix.&lt;settingName&gt;', where '&lt;settingName&gt;' is the name of the
 * setting within the sub set. In addition, this class has on board the
 * functionality of reading and writing the content of Settings objects to
 * Files, Streams, and Reader/Writers, both in text format (style of Apache
 * configuration files) and in XML.
 * 
 * @author sautter
 */
public class Settings {
	
	//	TODO redo settings IO, i.e., stream it
	
	//	constants for XML style storage
	/**
	 * the name of an element representing a set of settings in the XML
	 * representation
	 */
	public static final String SETTING_GROUP_TAG = "settingsGroup";

	/**
	 * the name of an element representing single setting in the XML
	 * representation
	 */
	public static final String SETTING_TAG = "setting";

	/**
	 * the name of the attribute containing the name of a setting in the XML
	 * representation
	 */
	public static final String SETTING_NAME_ATTRIBUTE = "name";

	/**
	 * the name of the attribute containing the value of a setting in the XML
	 * representation
	 */
	public static final String SETTING_VALUE_ATTRIBUTE = "value";

	// constants for text style storage
	/**
	 * the character used for separating a prefixes from eachother and the
	 * setting name in the text representation, namely '.'
	 */
	public static final char PREFIX_PART_SEPARATOR 		= '.';
	
	/**
	 * the character marking a line as a comment in the text representation,
	 * namely '#'
	 */
	public static final String COMMENT_MARKER = "#";

	/**
	 * the character used for escaping settings values in the text
	 * representation, namely '\'
	 */
	public static final char ESCAPER = '\\';

	/**
	 * the character used for separating a setting's name from its value in the
	 * text representation, namely '='
	 */
	public static final char SETTING_VALUE_SEPARATOR = '=';

	/**
	 * the character used for quoting a setting's value in the text
	 * representation, namely '"'
	 */
	public static final char SETTING_VALUE_DELIMITER = '"';

	/**
	 * the characters to escape in a setting's value in the text representation,
	 * namely '"' and '/'
	 */
	public static final char[] TO_ESCAPE = { ESCAPER, SETTING_VALUE_DELIMITER };
	
	private static final String FILE_DATE_FORMAT = "yyyy-MM-dd-HH-mm-ss";
	
	private static Parser PARSER = new Parser(new SettingsGrammar());
	
	private Properties keyStore = new Properties();
	private Properties valueStore = new Properties();
	
	private HashMap subSets = new HashMap();
	private Settings parent = null;
	private String prefix = null;
	
	/**	Constructor
	 */
	public Settings() {}
	
	/**	Constructor
	 * @param parent
	 * @param prefix
	 */
	private Settings(Settings parent, String prefix) {
		this.parent = parent;
		this.prefix = prefix;
	}
	
	/**
	 * Store a key / value pair.
	 * @param key the key to map the specified value to
	 * @param value the value to be mapped to the specified key
	 * @return the value the specified key was mapped to before
	 */
	public String setSetting(String key, String value) {
		Settings set = this.getSubsetFor(key);
		String localKey = getLocalKey(key);
		
		String oldValue = set.valueStore.getProperty(localKey);
		set.keyStore.setProperty(localKey, key.substring(key.lastIndexOf(PREFIX_PART_SEPARATOR) + 1));
		set.valueStore.setProperty(localKey, value);
		return oldValue;
	}
	
	/**
	 * Add all key / value pairs contained in the specified Settings to this
	 * Settings object.
	 * @param settings the Settings containing the key / value pairs to be added
	 */
	public void setSettings(Settings settings) {
		if (settings != null) {
			String[] keys = settings.getKeys();
			for (int k = 0; k < keys.length; k++) this.setSetting(keys[k], settings.getSetting(keys[k]));
		}
	}
	
	/**
	 * Add all key / value pairs contained in the specified Properties to this
	 * Settings object.
	 * @param prop the Properties containing the key / value pairs to be added
	 */
	public void setProperties(Properties prop) {
		if (prop != null) {
			Enumeration en = prop.keys();
			Object key;
			while (en.hasMoreElements()) {
				key = en.nextElement();
				if (key instanceof String)
					this.setSetting(((String) key), prop.getProperty((String) key));
			}
		}
	}
	
	/**
	 * Get the value assigned to key.
	 * @param key the key of the required value
	 * @return the value assigned to the specified key
	 */
	public String getSetting(String key) {
		return this.getSetting(key, null);
	}
	
	/**
	 * Get the value assigned to key, or def if there is no value assigned to
	 * the specified key.
	 * @param key the key of the required value
	 * @param def the value to be returned if there is no value assigned to the
	 *            specified key
	 * @return the value assigned to the specified key, or the specified default
	 *         if there is no such value
	 */
	public String getSetting(String key, String def) {
		Settings set = this.getSubsetFor(key);
		return set.valueStore.getProperty(getLocalKey(key), def);
	}
	
	/**
	 * Delete a key / value pair.
	 * @param key the key of the key / value pair to be removed
	 * @return the value the specified key was mapped to
	 */
	public String removeSetting(String key) {
		Settings set = this.getSubsetFor(key);
		String localKey = getLocalKey(key);
		
		String value = set.valueStore.getProperty(localKey);
		set.keyStore.remove(localKey);
		set.valueStore.remove(localKey);
		return value;
	}
	
	private static String getLocalKey(String key) {
		return ((key == null) ? null : produceKey(key.substring(key.lastIndexOf(PREFIX_PART_SEPARATOR) + 1)));
	}
	
	private Settings getSubsetFor(String key) {
		//	no prefix
		if ((key == null) || (key.indexOf(PREFIX_PART_SEPARATOR) == -1)) return this;
		
		//	find apppropriate subset
		int split = key.indexOf(PREFIX_PART_SEPARATOR);
		String prefix = key.substring(0, split);
		return this.getSubset(prefix).getSubsetFor(key.substring(split + 1));
	}
	
	private static final String INDENT_PER_LEVEL = "  ";
	
	/**
	 * Store the content of this Settings object to some file in text format
	 * (Apache style config file). If the specified file already exists, it will
	 * be renamed and then recreated to store the settings to.
	 * @param file the file to store the settings to
	 */
	public void storeAsText(File file) throws IOException {
		storeSettingsAsText(file, this);
	}

	/**
	 * Store the content of this Settings object to some output stream in text
	 * format (Apache style config file).
	 * @param out the output stream to store the settings to
	 */
	public void storeAsText(OutputStream out) throws IOException {
		storeSettingsAsText(out, this);
	}
	
	/**
	 * Store the content of this Settings object to some writer in text format
	 * (Apache style config file).
	 * @param out the writer to store the settings to
	 */
	public void storeAsText(Writer out) throws IOException {
		storeSettingsAsText(out, this);
	}

	/**
	 * Store the content of this Settings object to some file in XML format. If
	 * the specified file already exists, it will be renamed and then recreated
	 * to store the settings to.
	 * @param file the File to store the settings to
	 */
	public void storeAsXML(File file) throws IOException {
		storeSettingsAsXML(file, this);
	}

	/**
	 * Store the content of this Settings object to some output stream in XML
	 * format.
	 * @param out the output stream to store the settings to
	 */
	public void storeAsXML(OutputStream out) throws IOException {
		storeSettingsAsXML(out, this);
	}

	/**
	 * Store the content of this Settings object to some writer in XML format.
	 * @param out the writer to store the settings to
	 */
	public void storeAsXML(Writer out) throws IOException {
		storeSettingsAsXML(out, this);
	}
	
	/**
	 * Test whether this Settings contains a given key.
	 * @param key the key String to look for
	 * @return true if and only if this Settings contains the specified key
	 */
	public boolean containsKey(String key) {
		String test = "";
		return (this.getSetting(key, test) != test);
	}

	/**
	 * Test whether this Settings contains a given value (regardless of the key
	 * it belongs to).
	 * @param value the String to look for
	 * @return true if and only if this Settings contains the specified value
	 */
	public boolean containsValue(String value) {
		if ((this.valueStore.containsValue(value))) return true;
		ArrayList list = new ArrayList(this.subSets.values());
		for (int i = 0; i < list.size(); i++)
			if (((Settings) list.get(i)).containsValue(value)) return true;
		return false;
	}

	/**
	 * Check if this Settings has some given prefix.
	 * @param prefix the prefix to check for
	 * @return true if and only if this Settings has the specified prefix
	 */
	public boolean hasPrefix(String prefix) {
		//	no prefix
		if ((prefix == null) || (prefix.length() == 0))
			return true;
		
		//	prefix of first level subset
		if (prefix.indexOf(PREFIX_PART_SEPARATOR) == -1) return
		produceKey(prefix).equals(produceKey(this.prefix));
		
		//	prefix of some deeper level subset
		if ((PREFIX_PART_SEPARATOR + produceKey(prefix)).endsWith(PREFIX_PART_SEPARATOR + produceKey(this.prefix))) 
			return ((this.parent != null) && this.parent.hasPrefix(prefix.substring(0, prefix.lastIndexOf(PREFIX_PART_SEPARATOR))));
		
		//	no match
		return false;
	}
	
	/**
	 * Get this Settings' prefix.
	 * @return this Settings' prefix
	 */
	public String getPrefix() {
		return this.prefix;
	}
	
	/**
	 * Get this Settings' full prefix in the Settings hierarchy.
	 * @return this Settings's full prefix in the Settings hierarchy
	 */
	public String getFullPrefix() {
		String parentPrefix = ((this.parent == null) ? null : this.parent.getFullPrefix());
		return ((parentPrefix == null) ? this.prefix : (parentPrefix + PREFIX_PART_SEPARATOR + this.prefix));
	}
	
	/**	change the prefix of this Settings (invoced on the root of a Settings hierarchy, this method has no effect)
	 * @param	newPrefix	the new prefix for this Settings
	 */
	public void setPrefix(String newPrefix) {
		if ((this.parent != null) && (newPrefix != null) && (newPrefix.length() != 0)) {
			this.parent.changeSubsetPrefix(this, newPrefix);
			this.prefix = newPrefix;
		}
	}
	
	/**
	 * Check if this Settings has a subset with some given prefix.
	 * @param prefix the prefix to check for
	 * @return true if and only if this Settings has a subset with the specified
	 *         prefix
	 */
	public boolean hasSubset(String prefix) {
		//	no prefix
		if ((prefix == null) || (prefix.length() == 0))
			return true;
		
		//	prefix of first level subset
		if (prefix.indexOf(PREFIX_PART_SEPARATOR) == -1)
			return this.subSets.containsKey(produceKey(prefix));
		
		//	prefix of some deeper level subset
		int split = prefix.indexOf(PREFIX_PART_SEPARATOR);
		String localPrefix = produceKey(prefix.substring(0, split));
		return (this.subSets.containsKey(localPrefix) && this.getSubset(localPrefix).hasSubset(prefix.substring(split + 1)));
	}
	
	/**
	 * Retrieve the prefixes of all this Settings' subsets.
	 * @return the prefixes of this Settings' subsets, packed in an array of
	 *         Strings
	 */
	public String[] getSubsetPrefixes() {
		ArrayList subsets = new ArrayList(this.subSets.values());
		ArrayList subsetPrefixes = new ArrayList();
		for (int s = 0; s < subsets.size(); s++) subsetPrefixes.add(((Settings) subsets.get(s)).prefix);
		Collections.sort(subsetPrefixes);
		return ((String[]) subsetPrefixes.toArray(new String[subsetPrefixes.size()]));
	}
	
	/**
	 * Retrieve all the keys contained in this Settings, without its subsets, packed
	 * in a String array.
	 * @return all the keys contained in this Settings, packed in a String array
	 */
	public String[] getLocalKeys() {
		ArrayList keys = new ArrayList(this.keyStore.values());
		Collections.sort(keys);
		return ((String[]) keys.toArray(new String[keys.size()]));
	}
	
	/**
	 * Retrieve all the keys contained in this Settings and its subsets, packed
	 * in a String array.
	 * @return all the keys contained in this Settings, packed in a String array
	 */
	public String[] getKeys() {
		return getKeys(true);
	}
	
	/**
	 * Retrieve all the keys contained in this Settings and its subsets, packed
	 * in a String array, restricted to the keys that start with the specified
	 * prefix.
	 * @return all the keys contained in this Settings, packed in a String array
	 */
	public String[] getKeys(String prefix) {
		String unifiedPrefix = ((prefix == null) ? "" : (prefix + (prefix.endsWith("" + PREFIX_PART_SEPARATOR) ? "" : ("" + PREFIX_PART_SEPARATOR))));
		String[] keys = this.getSubsetFor(unifiedPrefix).getKeys();
		String[] prefixedKeys = new String[keys.length];
		for (int k = 0; k < keys.length; k++) prefixedKeys[k] = unifiedPrefix + keys[k];
		return prefixedKeys;
	}
	
	/**
	 * Retrieve all the keys contained in this Settings and its subsets, packed
	 * in a String array. The keys are fully qualified to the root of the
	 * Settings hierarchy.
	 * @return all the keys contained in this Settings, packed in a String array
	 */
	public String[] getFullKeys() {
		String[] keys = this.getKeys(true);
		String prefix = this.getFullPrefix();
		String[] prefixedKeys = new String[keys.length];
		for (int k = 0; k < keys.length; k++)
			prefixedKeys[k] = prefix + PREFIX_PART_SEPARATOR + keys[k];
		return prefixedKeys;
	}
	
	private String[] getKeys(boolean isRoot) {
		ArrayList keys = new ArrayList();
		
		//	get own keys
		String[] localKeys = this.getLocalKeys();
		for (int k = 0; k < localKeys.length; k++)
			keys.add((isRoot ? "" : (this.prefix + PREFIX_PART_SEPARATOR)) + localKeys[k]);
		
		//	get keys from subsets
		ArrayList subsets = new ArrayList(this.subSets.values());
		for (int s = 0; s < subsets.size(); s++) {
			String[] subsetKeys = ((Settings) subsets.get(s)).getKeys(false);
			for (int k = 0; k < subsetKeys.length; k++)
				keys.add((isRoot ? "" : (this.prefix + PREFIX_PART_SEPARATOR)) + subsetKeys[k]);
		}
		
		//	return results
		Collections.sort(keys);
		return ((String[]) keys.toArray(new String[keys.size()]));
	}
	
	/**
	 * Retrieve all the values contained in this Settings, packed in a String
	 * array, restricted to the keys that start with prefix
	 * @return all the values contained in this Settings, packed in a String
	 *         array
	 */
	public String[] getValues() {
		//	get own values
		ArrayList values = new ArrayList(this.valueStore.values());
		
		//	get subset values
		ArrayList subsets = new ArrayList(this.subSets.values());
		for (int s = 0; s < subsets.size(); s++) {
			String[] subsetValues = ((Settings) subsets.get(s)).getValues();
			for (int v = 0; v < subsetValues.length; v++)
				values.add(subsetValues[v]);
		}
		
		//	return result
		return ((String[]) values.toArray(new String[values.size()]));
	}
	
	/**
	 * @return the number of key / value pairs contained in this Settings and
	 *         it's subsets
	 */
	public int size() {
		int size = this.keyStore.size();
		ArrayList list = new ArrayList(this.subSets.values());
		for (int i = 0; i < list.size(); i++) size += ((Settings) list.get(i)).size();
		return size;
	}
	
	/**
	 * @return true if and only if this Settings object contains no key / value
	 *         pair
	 */
	public boolean isEmpty() {
		return (this.size() == 0);
	}
	
	/**
	 * Remove all key / value pairs from this Settings and its subsets.
	 */
	public void clear() {
		this.subSets.clear();
		this.keyStore.clear();
		this.valueStore.clear();
	}
	
	/**
	 * Get a Settings representing a subset of this one (if it doesn't exist, it
	 * is created automatically).
	 * @param prefix the prefix for the subset (will be prepended to all keys in
	 *            the subset automatically)
	 * @return a Settings representing a subset of this one, i.e. only the key /
	 *         value pairs that's keys begin with the specified prefix
	 */
	public Settings getSubset(String prefix) {
		//	check parameter
		if ((prefix == null) || (prefix.length() == 0)) return this;
		
		//	produce first prefix part
		int split = prefix.indexOf(PREFIX_PART_SEPARATOR);
		String localPrefix = produceKey((split == -1) ? prefix : prefix.substring(0, split));
		
		//	check if subset exists
		Object o = this.subSets.get(localPrefix);
		if ((o != null) && (o instanceof Settings)) return ((split == -1) ? ((Settings) o) : ((Settings) o).getSubset(prefix.substring(split + 1)));
		
		//	create new subset
		Settings subset = new Settings(this, ((split == -1) ? prefix : prefix.substring(0, split)));
		this.subSets.put(localPrefix, subset);
		return ((split == -1) ? subset : subset.getSubset(prefix.substring(split + 1)));
	}
	
	private void changeSubsetPrefix(Settings subset, String newPrefix) {
		if ((subset != null) && (newPrefix != null) && (newPrefix.length() != 0)) {
			String key = produceKey(subset.prefix);
			this.subSets.remove(key);
			this.subSets.put(produceKey(newPrefix), subset);
		}
	}
	
	/**
	 * Remove a subset from this Settings (also clears the subset).
	 * @param subset the subset to be removed
	 */
	public void removeSubset(Settings subset) {
		if (subset != null) {
			String key = produceKey(subset.prefix);
			this.subSets.remove(key);
			subset.clear();
		}
	}
	
	/**
	 * Convert this Settings object to a java.util.Properties
	 * @return a wrapper object behaving like a Properties while using this
	 *         Settings' data
	 */
	public Properties toProperties() {
		return wrapSettings(this);
	}
	
	/**
	 * Load settings from a file. This method will automatically determin if the
	 * settings data is in text format or in XML format.
	 * @param file the file to read
	 * @return a Settings object containing the key / value pairs loaded from
	 *         the file
	 */
	public static Settings loadSettings(File file) {
		try {
			FileInputStream fis = new FileInputStream(file);
			Settings set = loadSettings(fis);
			fis.close();
			return set;
		}
		catch (Exception e) {
			return new Settings();
		}
	}
	
	/**
	 * Load settings from an input stream. This method will automatically
	 * determin if the settings data is in text format or in XML format.
	 * @param in the input stream to read from
	 * @return a Settings object containing the key / value pairs loaded from
	 *         the input stream
	 */
	public static Settings loadSettings(InputStream in) {
		try {
			return loadSettings(new InputStreamReader(in));
		}
		catch (Exception e) {
			return new Settings();
		}
	}
	
	/**
	 * Load settings from a reader. This method will automatically determin if
	 * the settings data is in text format or in XML format.
	 * @param in the reader to read from
	 * @return a Settings object containing the key / value pairs loaded from
	 *         the reader
	 */
	public static Settings loadSettings(Reader in) {
		Settings set = new Settings();
		
		try {
			//	make sure we can peek
			BufferedReader sourceReader = ((in instanceof BufferedReader) ? ((BufferedReader) in) : new BufferedReader(in));
			
			//	check for XML
			boolean isXml = true;
			sourceReader.mark(("<" + SETTING_GROUP_TAG).length());
			char[] peek = new char[("<" + SETTING_GROUP_TAG).length()];
			if (sourceReader.read(peek) < ("<" + SETTING_GROUP_TAG).length())
				isXml = false;
			else isXml = ("<" + SETTING_GROUP_TAG).equals(new String(peek));
			sourceReader.reset();
			
			//	XML style
			if (isXml) {
				TreeNode root = PARSER.parse(sourceReader);
				readSettingsFromXML(root.getChildNode(SETTING_GROUP_TAG, 0), set);
			}
			
			//	text style
			else {
				ArrayList lines = new ArrayList();
				for (String line; (line = sourceReader.readLine()) != null;)
					lines.add(line);
				readSettingsFromText(lines, set);
			}
		}
		catch (FileNotFoundException fnfe) {
			System.out.println("Unable to find specified file, please check path and filename.");
		}
		catch (IOException ioe) {
			System.out.println(ioe.getClass() + ": " + ioe.getMessage() + " while loading file.");
		}
		
		return set;
	}
	
	private static void readSettingsFromText(ArrayList lines, Settings settings) {
		ArrayList keys = new ArrayList();
		Properties pairs = new Properties();
		
		//	read settings line by line
		for (int l = 0; l < lines.size(); l++) {
			String line = ((String) lines.get(l)).trim();
			
			//	exclude comments and blank lines
			if (line.startsWith(COMMENT_MARKER) || (line.length() == 0))
				continue;
			
			//	get key
			int keyValueSplit = line.indexOf(SETTING_VALUE_SEPARATOR);
			int valueStart = line.indexOf(SETTING_VALUE_DELIMITER);
			String key = (((keyValueSplit != -1) && (keyValueSplit < valueStart)) ? line.substring(0, line.indexOf(SETTING_VALUE_SEPARATOR)).trim() : null);
			
			//	get value only if key is valid
			if (key == null)
				continue;
			
			//	get value
			StringBuffer value = new StringBuffer();
			int escape = -1;
			char ch;
			
			//	collect value
			for (int c = (valueStart + 1); c < line.length(); c++) {
				ch = line.charAt(c);
				
				//	character is escaped
				if (c == escape)
					value = value.append(ch);
					
				//	escape next character
				else if (ch == ESCAPER)
					escape = c + 1;
					
				//	end of value
				else if (ch == SETTING_VALUE_DELIMITER)
					break;
					
				//	actual character belongs to value
				else value.append(ch);
			}
			
			//	remember key and value
			keys.add(key);
			pairs.setProperty(key, value.toString().trim());
		}
		
		//	sort keys
		Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
		
		//	read top level settings and collect top level group prefixes
		TreeSet prefixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int k = 0; k < keys.size(); k++) {
			String key = ((String) keys.get(k));
			if (key.indexOf(PREFIX_PART_SEPARATOR) == -1)
				settings.setSetting(key, pairs.getProperty(key));
			else prefixes.add(key.substring(0, key.indexOf(PREFIX_PART_SEPARATOR)));
		}
		
		//	create subsets
		for (Iterator pit = prefixes.iterator(); pit.hasNext();)
			readSubsetFromText(keys, pairs, ((String) pit.next()), settings);
	}
	
	private static void readSubsetFromText(ArrayList keys, Properties pairs, String prefix, Settings settings) {
		
		//	create subset for prefix
		String subsetName = ((prefix.indexOf(PREFIX_PART_SEPARATOR) == -1) ? prefix : prefix.substring(prefix.lastIndexOf(PREFIX_PART_SEPARATOR) + 1));
		Settings subset = settings.getSubset(subsetName);
		
		//	read settings and collect group prefixes
		TreeSet prefixes = new TreeSet(String.CASE_INSENSITIVE_ORDER);
		for (int k = 0; k < keys.size(); k++) {
			String key = ((String) keys.get(k));
			if (key.startsWith(prefix + PREFIX_PART_SEPARATOR)) {
				int prefixLength = prefix.length() + 1;	//	consider separator char
				if (key.indexOf(PREFIX_PART_SEPARATOR, prefixLength) == -1)
					subset.setSetting(key.substring(prefixLength), pairs.getProperty(key));
				else prefixes.add(key.substring(0, key.indexOf(PREFIX_PART_SEPARATOR, prefixLength)));
			}
		}
		
		//	create subsets recursively
		for (Iterator pit = prefixes.iterator(); pit.hasNext();)
			readSubsetFromText(keys, pairs, ((String) pit.next()), subset);
	}
	
	private static void readSettingsFromXML(TreeNode root, Settings settings) {
		
		//	read and store settings
		TreeNode[] settingNodes = root.getChildNodes(SETTING_TAG);
		for (int n = 0; n < settingNodes.length; n++) {
			if (settingNodes[n].hasAttribute(SETTING_NAME_ATTRIBUTE) && settingNodes[n].hasAttribute(SETTING_VALUE_ATTRIBUTE))
				settings.setSetting(settingNodes[n].getAttribute(SETTING_NAME_ATTRIBUTE), settingNodes[n].getAttribute(SETTING_VALUE_ATTRIBUTE));
		}
		
		//	create subsets recursively
		TreeNode[] settingGroupNodes = root.getChildNodes(SETTING_GROUP_TAG);
		for (int n = 0; n < settingGroupNodes.length; n++) {
			if (settingGroupNodes[n].hasAttribute(SETTING_NAME_ATTRIBUTE))
				readSettingsFromXML(settingGroupNodes[n], settings.getSubset(settingGroupNodes[n].getAttribute(SETTING_NAME_ATTRIBUTE)));
		}
	}
	
	private static void makeWay(File file) throws IOException {
		if (!file.exists())
			return;
		
		File oldFile = new File(file.getPath() + "." + new SimpleDateFormat(FILE_DATE_FORMAT).format(new Date()) + ".old");
		if (!oldFile.createNewFile())
			return;
		
		OutputStream out = new BufferedOutputStream(new FileOutputStream(oldFile));
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		byte[] buffer = new byte[1024];
		int read;
		while ((read = in.read(buffer, 0, buffer.length)) != -1)
			out.write(buffer, 0, read);
		out.flush();
		out.close();
		in.close();
	}
	
	/**
	 * Store the content of a Settings object to some file in text format
	 * (Apache style config file). If the specified file already exists, it will
	 * be renamed and then recreated to store the settings to.
	 * @param file the file to store the settings to
	 * @param settings the Settings to be stored
	 */
	public static void storeSettingsAsText(File file, Settings settings) throws IOException {
		
		//	rename existing file
		makeWay(file);
		
		//	store settings
		FileOutputStream fos = new FileOutputStream(file);
		storeSettingsAsText(fos, settings);
		fos.flush();
		fos.close();
	}

	/**
	 * Store the content of a Settings object to some output stream in text
	 * format (Apache style config file).
	 * @param out the output stream to store the settings to
	 * @param settings the Settings to be stored
	 */
	public static void storeSettingsAsText(OutputStream out, Settings settings) throws IOException {
		storeSettingsAsText(new OutputStreamWriter(out), settings);
	}

	/**
	 * Store the content of a Settings object to some writer in text format
	 * (Apache style config file).
	 * @param out the Writer to store the settings to
	 * @param settings the Settings to be stored
	 */
	public static void storeSettingsAsText(Writer out, Settings settings) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		String[] keys = settings.getKeys();
		for (int k = 0; k < keys.length; k++) {
			bw.write(keys[k] + " " + SETTING_VALUE_SEPARATOR + " " + SETTING_VALUE_DELIMITER + EasyIO.escape(settings.getSetting(keys[k], ""), TO_ESCAPE, ESCAPER) + SETTING_VALUE_DELIMITER + ";");
			bw.newLine();
		}
		if (bw != out)
			bw.flush();
	}
	
	/**
	 * Store the content of a Settings object to some file in XML format. If the
	 * specified file already exists, it will be renamed and then recreated to
	 * store the settings to.
	 * @param file the File to store the settings to
	 * @param settings the Settings to be stored
	 */
	public static void storeSettingsAsXML(File file, Settings settings) throws IOException {
		
		//	rename existing file
		makeWay(file);
		
		//	store settings
		FileOutputStream fos = new FileOutputStream(file);
		storeSettingsAsXML(fos, settings);
		fos.flush();
		fos.close();
	}
	
	/**
	 * Store the content of a Settings object to some output stream in XML
	 * format.
	 * @param out the output stream to store the settings to
	 * @param settings the Settings to be stored
	 */
	public static void storeSettingsAsXML(OutputStream out, Settings settings) throws IOException {
		storeSettingsAsXML(new OutputStreamWriter(out), settings);
	}
	
	/**
	 * Store the content of a Settings object to some writer in XML format.
	 * @param out the writer to store the settings to
	 * @param settings the Settings to be stored
	 */
	public static void storeSettingsAsXML(Writer out, Settings settings) throws IOException {
		BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
		settings.writeXML(bw, "");
		if (bw != out)
			bw.flush();
	}
	
	private void writeXML(BufferedWriter bw, String indent) throws IOException {
		boolean isRoot = (indent.length() == 0);
		
		//	don't display an empty group
		if (!this.isEmpty()) {
			
			//	add start tag
			bw.write(indent + "<" + SETTING_GROUP_TAG + 
					((isRoot || (this.prefix == null)) ? "" : (" " + SETTING_NAME_ATTRIBUTE + "=\"" + this.prefix) + "\"") +
					">");
			bw.newLine();
			
			//	add own settings
			String[] keys = this.getKeys();
			for (int k = 0; k < keys.length; k++) {
				if (keys[k].indexOf(PREFIX_PART_SEPARATOR) == -1) {
					bw.write(indent + INDENT_PER_LEVEL + "<" + SETTING_TAG + "" +
							" " + SETTING_NAME_ATTRIBUTE + "=\"" + keys[k] + "\"" +
							" " + SETTING_VALUE_ATTRIBUTE + "=\"" + this.getSetting(keys[k], "") + "\"" +
							"/>");
					bw.newLine();
				}
			}
			
			//	add subsets
			String[] subsetPrefixes = this.getSubsetPrefixes();
			for (int s = 0; s < subsetPrefixes.length; s++)
				this.getSubset(subsetPrefixes[s]).writeXML(bw, (indent + INDENT_PER_LEVEL));
			
			//	add end tag
			bw.write(indent + "</" + SETTING_GROUP_TAG + ">");
			bw.newLine();
		}
	}

	/**
	 * Wrap a Settings object so that it appears to be a java.util.Properties.
	 * @param settings the Settings to be wrapped
	 * @return a Properties object wrapping the specified Settings object
	 */
	public static Properties wrapSettings(Settings settings) {
		return new SettingsView(settings);
	}
	
	private static final int HASH_THRESHOLD = 13;
	
	private static String produceKey(String rawKey) {
		if (rawKey == null) return null;
		return ((rawKey.length() < HASH_THRESHOLD) ? rawKey.toLowerCase() : ("" + rawKey.toLowerCase().hashCode()));
	}
	
	private static class SettingsView extends Properties {
		
		private Settings valueStore;
		
		/** Constructor
		 * @param	settings	the Settiongs object to be wrapped
		 */
		SettingsView(Settings settings) {
			this.valueStore = settings;
		}
		
		/** @see java.util.Properties#getProperty(java.lang.String)
		 */
		public String getProperty(String key) {
			return this.valueStore.getSetting(key);
		}
		
		/** @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
		 */
		public String getProperty(String key, String defaultValue) {
			return this.valueStore.getSetting(key, defaultValue);
		}
		
		/** @see java.util.Properties#setProperty(java.lang.String, java.lang.String)
		 */
		public Object setProperty(String key, String value) {
			return this.valueStore.setSetting(key, value);
		}
		
		/** @see java.util.Properties#propertyNames()
		 */
		public Enumeration propertyNames() {
			return new StringArrayEnumeration(this.valueStore.getKeys());
		}
		
		/** @see java.util.Map#clear()
		 */
		public void clear() {
			this.valueStore.clear();
		}
		
		/** @see java.util.Map#containsKey(java.lang.Object)
		 */
		public boolean containsKey(Object key) {
			return ((key != null) 
					&& (key instanceof String) 
					&& (this.valueStore.containsKey((String) key))
					);
		}
		
		/** @see java.util.Map#containsValue(java.lang.Object)
		 */
		public boolean containsValue(Object value) {
			return ((value != null) 
					&& (value instanceof String) 
					&& (this.valueStore.containsValue((String) value))
					);
		}
		
		/** @see java.util.Dictionary#get(java.lang.Object)
		 */
		public Object get(Object key) {
			if ((key != null) && (key instanceof String))
				return this.valueStore.getSetting((String) key);
			return super.get(key);
		}
		
		/** @see java.util.Dictionary#isEmpty()
		 */
		public boolean isEmpty() {
			return this.valueStore.isEmpty();
		}
		
		/** @see java.util.Dictionary#put(java.lang.Object, java.lang.Object)
		 */
		public Object put(Object key, Object value) {
			if ((key instanceof String) && (value instanceof String))
				return this.valueStore.setSetting(((String) key), ((String) value));
			return super.put(key, value);
		}
		
		/** @see java.util.Map#putAll(java.util.Map)
		 */
		public void putAll(Map t) {
			ArrayList list = new ArrayList(t.keySet());
			for (int i = 0; i < list.size(); i++)
				this.put(list.get(i), t.get(list.get(i)));
		}
		
		/** @see java.util.Dictionary#remove(java.lang.Object)
		 */
		public Object remove(Object key) {
			if ((key != null) && (key instanceof String))
				return this.valueStore.removeSetting((String) key);
			return super.remove(key);
		}
		
		/** @see java.util.Dictionary#size()
		 */
		public int size() {
			return this.valueStore.size();
		}
		
		/** @see java.lang.Object#toString()
		 */
		public String toString() {
			return this.valueStore.toString();
		}
		
		/** @see java.util.Dictionary#keys()
		 */
		public Enumeration keys() {
			return this.propertyNames();
		}
		
		/** @see java.util.Map#keySet()
		 */
		public Set keySet() {
			HashSet keySet = new HashSet();
			Enumeration names = this.propertyNames();
			while (names.hasMoreElements())
				keySet.add(names.nextElement());
			return keySet;
		}
		
		/** @see java.util.Map#values()
		 */
		public Collection values() {
			ArrayList list = new ArrayList(this.keySet());
			HashSet valueSet = new HashSet();
			for (int i = 0; i < list.size(); i++)
				valueSet.add(this.get(list.get(i)));
			return valueSet;
		}
		
		/** @see java.util.Dictionary#elements()
		 */
		public Enumeration elements() {
			String[] keys = this.valueStore.getKeys();
			String[] values = new String[keys.length];
			for (int i = 0; i < keys.length; i++)
				values[i] = this.valueStore.getSetting(keys[i]);
			return new StringArrayEnumeration(values);
		}
	}

	private static class StringArrayEnumeration implements Enumeration {
		
		private int counter = 0;
		private String[] content;
		
		/**	Constructor
		 * @param	content		the ArrayList to be wrapped
		 */
		StringArrayEnumeration(String[] content) {
			this.content = content;
		}
		
		/**	@see java.util.Enumeration#hasMoreElements()
		 */
		public boolean hasMoreElements() {
			return (this.counter < this.content.length);
		}
		
		/**	@see java.util.Enumeration#nextElement()
		 */
		public Object nextElement() {
			if (this.counter < this.content.length) {
				String s = this.content[counter];
				this.counter ++;
				return s;
			} else return null;
		}
	}

	private static class SettingsGrammar extends StandardGrammar {
		
		SettingsGrammar() {
			super();
		}
		
		/** @see de.htmlXmlUtil.grammars.Grammar#correctErrors()
		 */
		public boolean correctErrors() {
			return false;
		}
		
		/** @see de.htmlXmlUtil.grammars.Grammar#getParentTags(java.lang.String)
		 */
		public HashSet getParentTags(String tag) {
			HashSet parents = new HashSet();
			parents.add(Settings.SETTING_GROUP_TAG);
			return parents;
		}
		
		/** @see de.htmlXmlUtil.grammars.Grammar#canBeChildOf(java.lang.String, java.lang.String)
		 */
		public boolean canBeChildOf(String child, String parent) {
			return ((parent != null) && parent.equalsIgnoreCase(Settings.SETTING_GROUP_TAG));
		}
	}
}
