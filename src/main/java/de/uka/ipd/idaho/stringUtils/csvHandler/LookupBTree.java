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


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;

import sun.security.action.GetPropertyAction;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A simple file-based BTree implementation that allows storing and looking up
 * StringTupels by an alphanumeric key.
 * 
 * @author sautter
 */
public class LookupBTree {
	
	private static final int DEFAULT_MAX_CACHE_SIZE = 128;
	private static final String INNER_NODE_DATA_FILE = "data";
	private static final int DEFAULT_MAX_NODE_TUPELS = 250;
	
	private HashMap cache = new HashMap();
	private StringVector cacheKeys = new StringVector();
	private int cacheSize = DEFAULT_MAX_CACHE_SIZE;
	
	private File basePath;
	
	private String keyAttributeName;
	private char valueDelimiter = '"';
	private int maxNodeTupels = DEFAULT_MAX_NODE_TUPELS;
	
	private BTreeNode root;
	
	private boolean writeOnly = false;
	private StringVector writeKeys = new StringVector();
	private int maxCacheTupels; 
	private String lineSeparator = ((String) AccessController.doPrivileged(new GetPropertyAction("line.separator")));
	
	/** Constructor
	 * @param	basePath			the root folder of the file-based BTree
	 * @param	keyAttributeName	the name of the attribute to compare keys with on lookup
	 */
	public LookupBTree(File basePath, String keyAttributeName) {
		this(basePath, keyAttributeName, DEFAULT_MAX_NODE_TUPELS, DEFAULT_MAX_CACHE_SIZE);
	}
	
	/** Constructor
	 * @param	basePath			the root folder of the file-based BTree
	 * @param	keyAttributeName	the name of the attribute to compare keys with on lookup
	 * @param	maxNodeTupels		the maximum number of tupels a node in the backing BTree can hold before being split 
	 * @param	cacheSize			the maximum number of nodes in the cache
	 */
	public LookupBTree(File basePath, String keyAttributeName, int maxNodeTupels, int cacheSize) {
		if (!basePath.isDirectory())
			throw new IllegalArgumentException("The base path must be a folder.");
		this.basePath = basePath;
		if (!this.basePath.exists() && !this.basePath.mkdirs())
			throw new IllegalArgumentException("The base path does not exist and could not be created.");
		
		if (keyAttributeName.trim().length() == 0)
			throw new IllegalArgumentException("The key attribute name must not be empty");
		this.keyAttributeName = keyAttributeName;
		
		this.root = new BTreeNode(this.basePath, 0);
		this.maxNodeTupels = maxNodeTupels;
		this.cacheSize = cacheSize;
	}
	
	/** Constructor building a LookupBTree in write only mode (faster writing, but no lookup possible)
	 * @param	basePath			the root folder of the file-based BTree
	 * @param	keyAttributeName	the name of the attribute to compare keys with on lookup
	 * @param	maxNodeTupels		the maximum number of tupels a node in the backing BTree can hold before being split 
	 * @param	cacheSize			the maximum number of nodes in the cache
	 * @param	writeKeys			the keys for writing the data to files
	 * @param	maxCacheTupels		the maximum number of tupels a node can accumulate before writing them to disc
	 */
	public LookupBTree(File basePath, String keyAttributeName, int maxNodeTupels, int cacheSize, StringVector writeKeys, int maxCacheTupels) {
		this(basePath, keyAttributeName, maxNodeTupels, cacheSize);
		this.writeOnly = true;
		this.writeKeys = writeKeys;
		this.maxCacheTupels = maxCacheTupels;
	}
	
	/**	retrieve the values for a key
	 * @param	key		the alphanumeric key String to look up
	 * @return a StringRelation containing all the StringTuleps identified by the specified key
	 */
	public StringRelation lookup(String key) {
		if (this.writeOnly)
			throw new RuntimeException("Lookup is not possible in write only mode.");
		
		//	normalize key and get data relation
		String nKey = normalizeKey(key);
		BTreeNode dataNode = this.getNodeForKey(nKey, false);
		StringRelation result = new StringRelation();
		
		//	key found
		if (dataNode != null) {
			StringRelation data = dataNode.getData();
			for (int d = 0; d < data.size(); d++) {
				StringTupel st = data.get(d);
				String keyAttribute = st.getValue(this.keyAttributeName, "");
				if (keyAttribute.equals(key)) result.addElement(st);
			}
		}
		
		//	return result, with or without tupels
		return result;
	}
	
	/**	retrieve the values for a key, using normalized strings for matching the key attributes
	 * @param	key		the alphanumeric key String to look up
	 * @return a StringRelation containing all the StringTuleps identified by the specified key
	 */
	public StringRelation fuzzyLookup(String key) {
		if (this.writeOnly)
			throw new RuntimeException("Lookup is not possible in write only mode.");
		
		//	normalize key and get data relation
		String nKey = normalizeKey(key);
		BTreeNode dataNode = this.getNodeForKey(nKey, false);
		StringRelation result = new StringRelation();
		
		//	key found
		if (dataNode != null) {
			StringRelation data = dataNode.getData();
			for (int d = 0; d < data.size(); d++) {
				StringTupel st = data.get(d);
				String keyAttribute = normalizeKey(st.getValue(this.keyAttributeName, ""));
				if (keyAttribute.equalsIgnoreCase(nKey)) result.addElement(st);
			}
		}
		
		//	return result, with or without tupels
		return result;
	}
	
	/**	store a StringTupel of data
	 * @param	data	the StringTupel to be stored
	 */
	public void store(StringTupel data) {
		
		//	get and check key
		String key = data.getValue(this.keyAttributeName, "");
		if (key.length() == 0) return;
		
		//	get data relation and add tupel
		String nKey = normalizeKey(key);
		BTreeNode dataNode = this.getNodeForKey(nKey, true);
		if (dataNode != null) dataNode.storeData(data);
	}
	
	/**	get the node holding the data for the specified key
	 * @param	key		the key to retrieve the data for
	 * @param	create	create the requested node if it does not yet exist?
	 * @return the node holding the data for the specified key, or null, if there is no such node and create is false
	 */
	private BTreeNode getNodeForKey(String key, boolean create) {
		
		//	do cache lookup
		for (int k = (key.length() - 1); k >=0; k--) {
			String cacheKey = key.substring(0, k);
			if (this.cacheKeys.contains(cacheKey)) {
				this.cacheKeys.removeAll(cacheKey);
				this.cacheKeys.addElement(cacheKey);
				BTreeNode cacheNode = ((BTreeNode) this.cache.get(cacheKey));
				BTreeNode dataNode = cacheNode.getNode(key, create);
				if (cacheNode != dataNode) {
					cacheNode.clear();
					this.cache.remove(cacheKey);
					this.cacheKeys.removeAll(cacheKey);
					cacheKey = key.substring(0, dataNode.treeDepth);
					this.cacheKeys.addElement(cacheKey);
					this.cache.put(cacheKey, dataNode);
					this.cacheMisses++;
				} else this.cacheHits++;
				return dataNode;
			}
		}
		
		BTreeNode dataNode = this.root.getNode(key, create);
		this.cacheMisses++;
		
		//	store node in cache
		if (dataNode != null) {
			String cacheKey = key.substring(0, dataNode.treeDepth);
			this.cache.put(cacheKey, dataNode);
			this.cacheKeys.removeAll(cacheKey);
			this.cacheKeys.addElement(cacheKey);
			while (this.cache.size() > this.cacheSize) {
				String removeKey = this.cacheKeys.remove(0);
				BTreeNode removeNode = ((BTreeNode) this.cache.remove(removeKey));
				removeNode.clear();
			}
		}
		
		//	return result node
		return dataNode;
	}
	
	/**	flush the cache, writing all data to disc
	 */
	public void flushCache() {
		while (this.cache.size() > 0) {
			String removeKey = this.cacheKeys.remove(0);
			BTreeNode removeNode = ((BTreeNode) this.cache.remove(removeKey));
			removeNode.clear();
		}
		this.resetCacheStatistics();
	}
	
	/**	class for representing nodes in the BTree
	 */
	private class BTreeNode {
		
		//private FileWriter dataWriter = null;
		private File dataFile;
		private int treeDepth;
		
		private HashMap childNodes = new HashMap();
		
		private StringRelation data = null;
		private boolean dirty = false;
		
		//	in write only mode, counter for tupels in file
		private int fileDataCount = 0;
		
		/**	Constructor
		 * @param	dataFile			the file represented by this node
		 * @param	treeDepth			the depth of this node in the BTree
		 */
		private BTreeNode(File dataFile, int treeDepth) {
			this.dataFile = dataFile;
			this.treeDepth = treeDepth;
		}
		
		/** retrieve the BTreeNode holding the data for a given key	
		 * @param	key		the key String to find the node for
		 * @param	create	create the node if not found?
		 * @return the BTreeNode holding the data for a given key, or null, if there is no such node and create is false
		 */
		private BTreeNode getNode(String key, boolean create) {
			//	this node is required
			if (key.length() == this.treeDepth) return this;
			
			//	this node holds the data, even if the key is longer
			if (this.dataFile.isFile()) return this;
			
			//	produce local key and return child node
			String localKey = key.substring(this.treeDepth, (this.treeDepth + 1));
			BTreeNode child = this.getChildNode(localKey, create);
			return ((child == null) ? null : child.getNode(key, create));
		}
		
		/**	retrieve a child key
		 * @param	localKey	the key identifying the child node
		 * @param	create		create the node if not found?
		 * @return the child node identified by the specified key, or null, if there is no such node and create is false
		 */
		private BTreeNode getChildNode(String localKey, boolean create) {
			BTreeNode child = ((BTreeNode) this.childNodes.get(localKey));
			
			//	child node exists
			if (child != null) return child;
			
			//	create file and node representing it
			File childFile = new File(this.dataFile, localKey);
			if (childFile.exists()) {
				child = new BTreeNode(childFile, (this.treeDepth + 1));
				this.childNodes.put(localKey, child);
				return child;
			} else if (create) {
				try {
					if (childFile.createNewFile()) {
						child = new BTreeNode(childFile, (this.treeDepth + 1));
						this.childNodes.put(localKey, child);
						return child;
					} else return null;
				} catch (IOException ioe) {
					System.out.println("BTreeNode: " + ioe.getClass() + ": " + ioe.getMessage() + " while creating child node for " + dataFile.getAbsolutePath());
					return null;
				}
			} else return null;
		}
		
		/**	@return	a StringRelation holding the data represented by thei BTreeNode
		 */
		private StringRelation getData() {
			if (writeOnly)
				throw new RuntimeException("Lookup is not possible in write only mode.");
			
			//	make sure data is loaded
			this.loadData();
			
			//	return data
			return this.data;
		}
		
		/**	store a StringTupel of data
		 * @param	data	the StringTupel to be stored
		 * @return true if and only if the specified StringTuple was store successfully 
		 */
		private boolean storeData(StringTupel data) {
			
			//	in write only mode, avoid loading data
			if (writeOnly && (this.data == null)) this.data = new StringRelation();
			
			//	make sure data is loaded
			this.loadData();
			
			//	data could not be loaded
			if (this.data == null) return false;
			
			//	store tuple
			this.data.addElement(data);
			this.dirty = true;
			
			//	split node if necessary
			if (this.data.size() > maxNodeTupels) this.split();
			else if (writeOnly && ((this.data.size() + this.fileDataCount) > maxNodeTupels)) this.split();
			else if (writeOnly && (this.data.size() > maxCacheTupels)) this.writeData();
			
			//	report success
			return true;
		}
		
		/**	split the node
		 */
		private void split() {
			
			//	split only if data is in file, no need to split directory
			//	TODO: consider splitting "data" file of inner nodes
			if (this.dataFile.isFile()) {
				
//				//	close file writer if open
//				this.closeWriter();
				
				//	in write only mode, load existing data and add cached tupels
				if (writeOnly && (this.data.size() != 0)) {
					StringRelation cacheData = this.data;
					this.data = null;
					this.loadData();
					for (int d = 0; d < cacheData.size(); d++)
						this.data.addElement(cacheData.get(d));
					this.dirty = true;
					
				//	make sure data is in memory
				} else this.loadData();
				
				//	create child node keys and re-distribute data
				StringVector childNodeKeys = new StringVector();
				HashMap childNodeData = new HashMap();
				for (int d = 0; d < this.data.size(); d++) {
					
					//	get data tupel
					StringTupel st = data.get(d);
					
					//	create local key
					String key = normalizeKey(st.getValue(keyAttributeName, ""));
					String localChildKey = ((key.length() == this.treeDepth) ? INNER_NODE_DATA_FILE : key.substring(this.treeDepth, (this.treeDepth + 1)));
					childNodeKeys.addElementIgnoreDuplicates(localChildKey);
					
					//	re-assign data tupel
					StringRelation childData;
					if (childNodeData.containsKey(localChildKey))
						childData = ((StringRelation) childNodeData.get(localChildKey));
					else {
						childData = new StringRelation();
						childNodeData.put(localChildKey, childData);
					}
					childData.addElement(st);
				}
				
				//	transform data file to folder
				String fileName = this.dataFile.getAbsolutePath();
				if (this.dataFile.length() == 0) this.dataFile.delete();
				else {
					File oldFile = new File(fileName + ".old");
					if (oldFile.exists()) oldFile.delete();
					this.dataFile.renameTo(oldFile);
				}
				this.dataFile = new File(fileName);
				this.dataFile.mkdir();
				
				//	clear local data
				this.data.clear();
				this.fileDataCount = 0;
				
				//	create child nodes
				for (int c = 0; c < childNodeKeys.size(); c++) {
					
					//	produce child node
					String localChildKey = childNodeKeys.get(c);
					
					//	local data of now inner node
					if (INNER_NODE_DATA_FILE.equals(localChildKey)) {
						
						//	transfer data
						StringRelation childData = ((StringRelation) childNodeData.get(localChildKey));
						for (int d = 0; d < childData.size(); d++)
							this.data.addElement(childData.get(d));
						
						//	force write data
						this.writeData();
						
					//	child node data
					} else {
						
						BTreeNode child = this.getChildNode(localChildKey, true);
						if (child != null) {
							
							//	transfer data
							StringRelation childData = ((StringRelation) childNodeData.get(localChildKey));
							for (int d = 0; d < childData.size(); d++)
								child.storeData(childData.get(d));
							
							//	force write data
							child.writeData();
							
							//	tidy up
							childData.clear();
						}
					}
				}
				
				//	tidy up a little more
				System.gc();
			}
		}
		
		/**	make sure the data file is loaded
		 */
		private void loadData() {
			if (this.data == null) try {
				FileReader fr = (this.dataFile.isFile() ? new FileReader(this.dataFile) : new FileReader(new File(this.dataFile, INNER_NODE_DATA_FILE)));
				this.data = StringRelation.readCsvData(fr, valueDelimiter, true, null);
				fr.close();
				this.dirty = false;
				this.fileDataCount = this.data.size();
			} catch (IOException ioe) {
				System.out.println("BTreeNode: " + ioe.getClass() + ": " + ioe.getMessage() + " while loading data from " + dataFile.getAbsolutePath());
			}
		}
		
		/**	write the data to the file system
		 */
		private void writeData() {
			
			//	avoid unnecessary writes
			if (!this.dirty || (this.data == null) || (this.data.size() == 0)) return;
			
			//	create storage file
			File file = (this.dataFile.isFile() ? this.dataFile : new File(this.dataFile, INNER_NODE_DATA_FILE));
			
			//	write only mode, append tupels to existing file
			if (writeOnly) {
				try {
					
					file.createNewFile();
//					if (this.dataWriter == null) this.dataWriter = new FileWriter(file, true);
					FileWriter dataWriter = new FileWriter(file, true);
					
					//	first write, need to write keys
					if (this.fileDataCount == 0)
//						this.dataWriter.write("" + valueDelimiter + writeKeys.concatStrings("" + valueDelimiter + "," + valueDelimiter) + valueDelimiter + lineSeparator);
						dataWriter.write("" + valueDelimiter + writeKeys.concatStrings("" + valueDelimiter + "," + valueDelimiter) + valueDelimiter + lineSeparator);
					
					//	write new data
					for (int d = 0; d < this.data.size(); d++)
//						this.dataWriter.write(this.data.get(d).toCsvString(valueDelimiter, writeKeys) + lineSeparator);
						dataWriter.write(this.data.get(d).toCsvString(valueDelimiter, writeKeys) + lineSeparator);
					
					//	remember storing data, and clean up
					this.fileDataCount+= this.data.size();
					this.data.clear();
					this.dirty = false;
					
					//	close writer
//					this.dataWriter.flush();
					dataWriter.flush();
					dataWriter.close();
					
				} catch (IOException ioe) {
					System.out.println("BTreeNode: " + ioe.getClass() + ": " + ioe.getMessage() + " while appending data to " + file.getAbsolutePath());
				}
				
			//	regular mode
			} else {
				
				//	clean up old file if existent
				if (file.exists() && (file.length() != 0)) {
					String fileName = file.getAbsolutePath();
					File oldFile = new File(fileName + ".old");
					if (oldFile.exists()) oldFile.delete();
					file.renameTo(oldFile);
					file = new File(fileName);
				}

				try {
					//	store data to file
					file.createNewFile();
					FileWriter writer = new FileWriter(file, true);
					StringRelation.writeCsvData(writer, this.data, valueDelimiter);
					writer.flush();
					writer.close();
					
					//	remember file and local data structure are synchonous now
					this.dirty = false;
				} catch (IOException ioe) {
					System.out.println("BTreeNode: " + ioe.getClass() + ": " + ioe.getMessage() + " while writing data to " + file.getAbsolutePath());
				}
			}
		}
		
		/**	clean up data (called when node is removed from cache)
		 */
		private void clear() {
			
			//	close writer if necessary
//			this.closeWriter();
			
			//	check if something to clear
			if (this.data != null) {
				
				//	make sure data is persistent
				this.writeData();
				
				//	clear data structures
				this.data.clear();
				this.data = null;
				System.gc();
			}
		}
		
//		/**	close the file writer
//		 */
//		private void closeWriter() {
//			if (this.dataWriter != null) try {
//				this.dataWriter.flush();
//				this.dataWriter.close();
//				this.dataWriter = null;
//			} catch (IOException ioe) {}
//		}
//		
		/**	compute the size the sub tree under this node
		 * @param	leavesOnly	count leaf nodes only?
		 * @return	the number of nodes in the sub tree under this node
		 */
		private int size(boolean leavesOnly) {
			int size = ((leavesOnly && this.dataFile.isDirectory()) ? 0 : 1);
			ArrayList childList = new ArrayList(this.childNodes.values());
			for (int c = 0; c < childList.size(); c++)
				size += ((BTreeNode) childList.get(c)).size(leavesOnly);
			return size;
		}
	}
	
	//	THE STATISTICS FUNCTIONS
	
	/**	compute the tree size
	 * @return	the number of nodes in the BTree
	 */
	public int size() {
		return this.root.size(false);
	}
	
	/**	compute the tree size
	 * @param	leavesOnly	count leaf nodes only?
	 * @return	the number of nodes in the BTree
	 */
	public int size(boolean leavesOnly) {
		return this.root.size(leavesOnly);
	}
	
	/**	@return	the cache size
	 */
	public int getChacheSize() {
		return this.cacheSize;
	}
	
	/**	@return	the cache fill level in percent
	 */
	public int getChacheLevel() {
		return ((this.cacheKeys.size() * 100) / this.cacheSize);
	}
	
	private int cacheHits = 0;
	private int cacheMisses = 0;
	
	/**	@return	the cache hit ratio in percent
	 */
	public int getCacheSuccess() {
		int sum = (this.cacheHits + this.cacheMisses);
		return ((sum == 0) ? 0 : ((this.cacheHits * 100) / sum));
	}
	
	/**	@return	the number of cache hits since the last reset
	 */
	public int getCacheHits() {
		return this.cacheHits;
	}
	
	/**	@return	the number of cache misses since the last reset
	 */
	public int getCacheMisses() {
		return this.cacheMisses;
	}
	
	/**	reset the statistics on cache hits and misses
	 */
	public void resetCacheStatistics() {
		this.cacheHits = 0;
		this.cacheMisses = 0;
	}
	
	//	THE STATISTICS FUNCTIONS
	
	static final String KEY_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789_";
	
	/**	normalize a key String (convert to lower case, replace all non-alphanumeric characters by underscores)
	 * @param	key		the string to normalize
	 * @return the normalized form of the specified key String
	 */
	private static String normalizeKey(String key) {
		String lKey = key.toLowerCase();
		if (lKey.matches("[a-z0-9\\_]")) return lKey;
		StringBuffer assembler = new StringBuffer();
		for (int c = 0; c < lKey.length(); c++) {
			char ch = lKey.charAt(c);
			if (KEY_CHARACTERS.indexOf(ch) == -1) assembler.append("_");
			else assembler.append(ch);
		}
		return assembler.toString();
	}
}
