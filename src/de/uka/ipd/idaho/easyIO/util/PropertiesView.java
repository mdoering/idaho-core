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
package de.uka.ipd.idaho.easyIO.util;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This class addds hierarchy functionality to java.util.Properties. This is
 * done by prepending the prefix specified in the constructor to every property
 * name before accessing the wrapped base Properties object. This reflects only
 * in the getProperty(), setProperty(), and getPropertyNames() methods, however.
 * The methods inherited from java.util.Hashtable are not wrapped.
 * 
 * @author sautter
 */
public class PropertiesView extends Properties {
	
	private Properties wrappedProperties;

	private String prefix;

	/** Constructor
	 * @param	properties	the Properties to be wrapped
	 * @param	prefix		the prefix to be inserted before the key for calls of the getProperty(String) and setProperty(String, String) methods 
	 */
	public PropertiesView(Properties properties, String prefix) {
		this.wrappedProperties = properties;
		this.prefix = prefix;
	}

	/** @see java.util.Properties#getProperty(java.lang.String)
	 */
	public String getProperty(String key) {
		return this.wrappedProperties.getProperty(this.prefix + key);
	}

	/** @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	public String getProperty(String key, String defaultValue) {
		return this.wrappedProperties.getProperty(this.prefix + key, defaultValue);
	}

	/** @see java.util.Properties#propertyNames()
	 */
	public Enumeration propertyNames() {
		Enumeration e = this.wrappedProperties.propertyNames();
		ArrayList pluginNames = new ArrayList();
		while (e.hasMoreElements()) {
			Object o = e.nextElement();
			if ((o != null) && (o instanceof String)) {
				String s = ((String) o);
				if (s.startsWith(this.prefix) && (s.length() > this.prefix.length())) pluginNames.add(s.substring(this.prefix.length()));
			}
		}
		return new KeyEnumeration(pluginNames);
	}

	private class KeyEnumeration implements Enumeration {

		private int counter = 0;

		private ArrayList content;

		/**	Constructor
		 * @param	content		the ArrayList to be wrapped
		 */
		KeyEnumeration(ArrayList content) {
			this.content = content;
		}

		/**	@see java.util.Enumeration#hasMoreElements()
		 */
		public boolean hasMoreElements() {
			return (this.counter < this.content.size());
		}

		/**	@see java.util.Enumeration#nextElement()
		 */
		public Object nextElement() {
			if (this.counter < this.content.size()) {
				Object o = this.content.get(this.counter);
				this.counter++;
				return o;
			} else {
				return null;
			}
		}
	}

	/** @see java.util.Properties#setProperty(java.lang.String, java.lang.String)
	 */
	public Object setProperty(String key, String value) {
		return this.wrappedProperties.setProperty((this.prefix + key), value);
	}
}
