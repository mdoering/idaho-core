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


/**
 * A d-queue specialized for Strings. All the access methods are synchronized,
 * so it is feasible to let a thread wait on one of the reading methods until a
 * String is added to the queue through one of the write methods.
 */
public class StringQueue {
	
	public static final int DEFAULT_INITIAL_CAPACITY	= 16;
	public static final int MAXIMUM_CAPACITY			= (Integer.MAX_VALUE / 2) + 1;
	public static final float DEFAULT_LOAD_RATE			= 0.75f;
	
	private String[] content;
	private float loadFactor;
	private int size = 0;
	private int capacity;
	private int threshold;
	
	private int firstPointer;	//	index of the first element
	private int lastPointer;	//	index after the last element
	private boolean optimizeFiFo;
	
	/**	Constructor
	 */
	public StringQueue() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_RATE, false);
	}
	
	/**	Constructor
	 * @param	optimizeFiFo	optimize the Queue for FiFo (add only at one end and remove from the other) or for access from both ends
	 */
	public StringQueue(boolean optimizeFiFo) {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_RATE, optimizeFiFo);
	}
	
	/**	Constructor
	 * @param 	initialCapacity	the initial size of the content array
	 */
	public StringQueue(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_RATE, false);
	}
	
	/**	Constructor
	 * @param 	initialCapacity	the initial size of the content array
	 * @param	optimizeFiFo	optimize the Queue for FiFo (add only at one end and remove from the other) or for access from both ends
	 */
	public StringQueue(int initialCapacity, boolean optimizeFiFo) {
		this(initialCapacity, DEFAULT_LOAD_RATE, optimizeFiFo);
	}
	
	/**	Constructor
	 * @param 	initialCapacity	the initial size of the content array
	 * @param 	loadFactor		the part of the content array that may be filled before it's size is duplicated
	 * @param	optimizeFiFo	optimize the Queue for FiFo (add only at one end and remove from the other) or for access from both ends
	 */
	public StringQueue(int initialCapacity, float loadFactor, boolean optimizeFiFo) {
		
		//	check arguments
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}
		if ((loadFactor <= 0) || (loadFactor >= 1) || Float.isNaN(loadFactor))	{
			throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
		}
		
		int iC = ((initialCapacity > MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : initialCapacity);
		
		// Find a power of 2 >= initialCapacity
		this.capacity = DEFAULT_INITIAL_CAPACITY;
		while (this.capacity < iC) {
			this.capacity *= 2;
		}
		
		this.loadFactor = loadFactor;
		this.threshold = ((int)(this.capacity * this.loadFactor));
		if (this.threshold == this.capacity) this.threshold--;
		this.content = new String[this.capacity];
		int base = ((this.optimizeFiFo) ? 0 : (this.capacity / 2));
		this.firstPointer = base;
		this.lastPointer = base;
	}
	
	/**	add a String to the start of this Queue
	 * @param	s	the String to be added
	 */
	public synchronized void addFirst(String s) {
		
		//	add String
		this.firstPointer--;
		if (this.firstPointer == -1) this.firstPointer = (this.capacity - 1);
		this.content[this.firstPointer] = s;
		this.size++;
		if (this.size == this.threshold) this.doubleSize();
		
		//	notify any Thrad waiting on a get or remove method
		this.notify();
	}
	
	/**	add a String to the end of this Queue
	 * @param	s	the String to be added 
	 */
	public synchronized void addLast(String s) {
		
		//	add String
		this.content[this.lastPointer] = s;
		this.lastPointer++;
		if (this.lastPointer == this.capacity) this.lastPointer = 0;
		this.size++;
		if (this.size == this.threshold) this.doubleSize();
		
		//	notify any Thrad waiting on a get or remove method
		this.notify();
	}
	
	/**	get the first String in the Queue (not removing it)
	 * @return	the first String in this Queue, if there is any, null otherwise
	 * This method will block until there is a String to be returned, or the Queue is cleared.
	 */
	public synchronized String getFirst() {
		return this.getFirst(0);
	}
	
	/**	get the first String in the Queue (not removing it)
	 * @param	timeout		the number of seconds to wait if there is no String in the queue
	 * @return	the first String in this Queue, if there is any, null otherwise
	 * This method will block until the timeout is over, or there is a String to be returned, or the Queue is cleared.
	 */
	public synchronized String getFirst(int timeout) {
		
		//	wait for String to be enqueued
		if (this.isEmpty()) {
			try {
				this.wait(timeout * 1000);
			} catch (InterruptedException ie) {
			}
		}
		
		//	return first String in Queue, if the is any
		return ((this.size == 0) ? null : this.content[this.firstPointer]);
	}
	
	/**	get the last String in the Queue (not removing it)
	 * @return	the last String in this Queue, if there is any, null otherwise
	 * This method will block until there is a String to be returned, or the Queue is cleared.
	 */
	public synchronized String getLast() {
		return this.getLast(0);
	}
	
	/**	get the last String in the Queue (not removing it)
	 * @param	timeout		the number of seconds to wait if there is no String in the queue
	 * @return	the last String in this Queue, if there is any, null otherwise
	 * This method will block until the timeout is over, or there is a String to be returned, or the Queue is cleared.
	 */
	public synchronized String getLast(int timeout) {
		
		//	wait for String to be enqueued
		if (this.isEmpty()) {
			try {
				this.wait(timeout * 1000);
			} catch (InterruptedException ie) {
			}
		}
		
		//	return last String in Queue, if the is any
		return ((this.size == 0) ? null : ((this.lastPointer == 0) ? this.content[this.capacity - 1] : this.content[this.lastPointer - 1]));
	}
	
	/**	remove the first String from this Queue, if there is any
	 * @return	the first String in this Queue, if there is any, null otherwise
	 * This method will block until there is a String to be returned, or the Queue is cleared.
	 */
	public synchronized String removeFirst() {
		return this.removeFirst(0);
	}
	
	/**	remove the first String from this Queue, if there is any
	 * @param	timeout		the number of seconds to wait if there is no String in the queue
	 * @return	the first String in this Queue, if there is any, null otherwise
	 * This method will block until the timeout is over, or there is a String to be returned, or the Queue is cleared.
	 */
	public synchronized String removeFirst(int timeout) {
		
		//	wait for String to be enqueued
		if (this.isEmpty()) {
			try {
				this.wait(timeout * 1000);
			} catch (InterruptedException ie) {
			}
		}
		
		//	return first String in Queue, if the is any
		if (this.size > 0) {
			String s = this.content[this.firstPointer];
			this.content[this.firstPointer] = null;
			this.firstPointer++;
			if (this.firstPointer == this.capacity) this.firstPointer = 0;
			this.size--;
			
			//	reset indices if possible
			if (this.size == 0) {
				int base = ((this.optimizeFiFo) ? 0 : (this.capacity / 2));
				this.firstPointer = base;
				this.lastPointer = base;
			}
			
			return s;
		} else {
			return null;
		}
	}
	
	/**	remove the last String from this Queue, if there is any	
	 * @return	the last String in this Queue, if there is any, null otherwise
	 * This method will block until there is a String to be returned, or the Queue is cleared.
	 */
	public synchronized String removeLast() {
		return this.removeLast(0);
	}
	
	/**	remove the last String from this Queue, if there is any	
	 * @param	timeout		the number of seconds to wait if there is no String in the queue
	 * @return	the last String in this Queue, if there is any, null otherwise
	 * This method will block until the timeout is over, or there is a String to be returned, or the Queue is cleared.
	 */
	public synchronized String removeLast(int timeout) {
		
		//	wait for String to be enqueued
		if (this.isEmpty()) {
			try {
				this.wait(timeout * 1000);
			} catch (InterruptedException ie) {
			}
		}
		
		//	return last String in Queue, if the is any
		if (this.size > 0) {
			this.lastPointer--;
			if (this.lastPointer == -1) this.lastPointer = (this.capacity - 1);
			String s = this.content[this.lastPointer];
			this.content[this.lastPointer] = null;
			this.size--;
			
			//	reset indices if possible
			if (this.size == 0) {
				int base = ((this.optimizeFiFo) ? 0 : (this.capacity / 2));
				this.firstPointer = base;
				this.lastPointer = base;
			}
			
			return s;
		} else {
			return null;
		}
	}
	
	/**	get the number of Strings in the Queue
	 * @return	the number of Strings contained in this Queue
	 */
	public synchronized int size() {
		return this.size;
	}
	
	/**	check if the Queue is empty, i.e. if the next call to one of the get or remove methods will block
	 * @return	true if and only if this Queue contains no Strings
	 */
	public synchronized boolean isEmpty() {
		return (this.size == 0);
	}
	
	/**	@return	the index of the specified String in this Queue, or -1, if this Queue contains no String str such that s.equals(str) returns true
	 */
	public synchronized int indexOf(String s) {
		int index = 0;
		while (index < this.size) {
			if (this.get(index).equals(s)) return index;
			index++;
		}
		return -1;
	}
	
	/**	check is this Queue contains a given String
	 * @param	s the String to check for
	 * @return	true is and only if this Queue contains an String str such that s.equals(str) returns true
	 */
	public synchronized boolean contains(String s) {
		return (this.indexOf(s) > -1);
	}
	
	/**	remove all Strings from this Queue
	 * Calling this method will unblock any Thread blocked on one of the get or remove methods 
	 */
	public synchronized void clear() {
		
		//	clear content array
		for (int i = 0; i < this.content.length; i++) this.content[i] = null;
		this.size = 0;
		
		//	reset indices
		int base = ((this.optimizeFiFo) ? 0 : (this.capacity / 2));
		this.firstPointer = base;
		this.lastPointer = base;
		
		//	netify all Threads waiting on a get or remove method
		this.notifyAll();
	}
	
	/**	get the entire content of this Queue
	 * @return	the Strings contained in this Queue, packed in an array
	 */
	public synchronized String[] toArray() {
		String[] ret = new String[this.size];
		this.copyContentToArray(ret, 0, false);
		return ret;
	}
	
	/**	@return	a String representation of this Queue and it's content
	 */
	public String toString() {
		String ret = "";
		for (int i = 0; i < this.capacity; i++) {
			if (i == this.firstPointer) ret = ret + "-fp>";
			if (i == this.lastPointer) ret = ret + "-lp>";
			ret = ret + "[" + this.content[i] + "]";
			if (i == this.firstPointer) ret = ret + "<fp-";
			if (i == this.lastPointer) ret = ret + "<lp-";
		}
		return ret;
	}
	
	//	quick access to an element by relative index based on firstPointer
	private String get(int index) {
		if ((index > -1) && (index < this.size)) {
			int i = this.firstPointer + index;
			if (i >= this.capacity) i -= this.capacity;
			return this.content[i];
		} else {
			return null;
		}
	}
	
	//	double the size of this Queue's content array
	private void doubleSize() {
		
		//	double the content array's size ...
		int newCapacity = (this.capacity * 2);
		String[] newContent = new String[newCapacity];
		int newFirstPointer = ((this.optimizeFiFo) ? 0 : ((newCapacity - this.size) / 2));
		
		//	... move content Strings ...
		this.copyContentToArray(newContent, newFirstPointer, true);
		this.content = newContent;
		
		//	... and readjust indices
		this.capacity = newCapacity;
		this.threshold = ((int)(this.capacity * this.loadFactor));
		if (this.threshold == this.capacity) this.threshold--;
		this.firstPointer = newFirstPointer;
		this.lastPointer = (newFirstPointer + this.size);
	}
	
	//	move or copy the content of this Queue's content array into the argument String array
	private void copyContentToArray(String[] target, int targetStartIndex, boolean move) {
		int index = this.firstPointer;
		int targetIndex = targetStartIndex;
		while (index != this.lastPointer) {
			target[targetIndex] = this.content[index];
			if (move) this.content[index] = null;
			targetIndex++;
			index++;
			if (index == this.capacity) index = 0;
		}
	}
}
