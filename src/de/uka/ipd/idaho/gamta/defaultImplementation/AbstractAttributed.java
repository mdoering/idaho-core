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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import de.uka.ipd.idaho.gamta.Attributed;

/**
 * Abstract implementation of the Attributed interface, providing standard
 * implementations for all the methods in the latter interface, including lazy
 * instantiation of the HashMap used for storing the attributes. Note that this
 * class is not abstract, despite its name, because it can serve as a standalone
 * store for arbitrary attributes, with some convenience over a plain HashMap.
 * 
 * @author sautter
 */
public class AbstractAttributed implements Attributed {
	
	private HashMap attributes = null; // initialize to null, create only on demand
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#clearAttributes()
	 */
	public void clearAttributes() {
		if (this.attributes != null)
			this.attributes.clear();
		this.attributes = null;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#copyAttributes(de.uka.ipd.idaho.gamta.Attributed)
	 */
	public void copyAttributes(Attributed source) {
		if (source != null) {
			String[] attributeNames = source.getAttributeNames();
			if (attributeNames.length == 0) return; // no need for initializing map if no attributes to copy
			if (this.attributes == null) //	initialize with appropriate capacity so resizing won't occur here
				this.attributes = new HashMap(Math.max(((3 * attributeNames.length) / 2), 2)) ;
			for (int a = 0; a < attributeNames.length; a++) 
				this.attributes.put(attributeNames[a], source.getAttribute(attributeNames[a]));
		}
	}
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#getAttribute(java.lang.String, java.lang.Object)
	 */
	public Object getAttribute(String name, Object def) {
		if (this.attributes == null)
			return def;
		return (this.attributes.containsKey(name) ? this.attributes.get(name) : def);
	}
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		if (this.attributes == null)
			return null;
		return this.attributes.get(name);
	}
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#getAttributeNames()
	 */
	public String[] getAttributeNames() {
		if (this.attributes == null)
			return new String[0];
		ArrayList names = new ArrayList(this.attributes.keySet());
		Collections.sort(names);
		return ((String[]) names.toArray(new String[names.size()]));
	}
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#hasAttribute(java.lang.String)
	 */
	public boolean hasAttribute(String name) {
		return ((this.attributes != null) && this.attributes.containsKey(name));
	}
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#removeAttribute(java.lang.String)
	 */
	public Object removeAttribute(String name) {
		if (this.attributes == null)
			return null;
		Object value = this.attributes.remove(name);
		if (this.attributes.isEmpty())
			this.attributes = null;
		return value;
	}
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#setAttribute(java.lang.String)
	 */
	public void setAttribute(String name) {
		this.setAttribute(name, "true");
	}
	
	/** @see de.uka.ipd.idaho.gamta.Attributed#setAttribute(java.lang.String, java.lang.Object)
	 */
	public Object setAttribute(String name, Object value) {
		if (value == null)
			return this.removeAttribute(name);
		if (this.attributes == null)
			this.attributes = new HashMap(2);
		return this.attributes.put(name, value);
	}
}
