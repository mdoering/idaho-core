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
package de.uka.ipd.idaho.gamta;


/**
 * interface for classes whose instances can have attributes
 * 
 * @author sautter
 */
public interface Attributed {

	/**	add a boolean-valued attribute to this object, setting it to <code>true</code> in some kind of way so at least <code>getAttribute()</code> returned a non-null value afterward, and <code>hasAttribute()</code> returns <code>true</code>
	 * @param	name	the name for the attribute
	 */
	public abstract void setAttribute(String name);

	/**	add an attribute to this object
	 * @param	name	the name for the attribute
	 * @param	value	the value of the attribute
	 * @return the old value of the attribute, if there was one, null otherwise
	 */
	public abstract Object setAttribute(String name, Object value);
	
	/**	add all attributes of another object to this object
	 * @param	source	the object to copy the attributes from
	 */
	public abstract void copyAttributes(Attributed source);
	
	/**	retrieve the value of an attribute
	 * @param	name	the name of the attribute
	 * @return the value of the attribute with the specified name, or null, if there is no such attribute
	 */
	public abstract Object getAttribute(String name);
	
	/**	retrieve the value of an attribute
	 * @param	name	the name of the attribute
	 * @param	def		the value to return if this object does not have an attribute with the specified name
	 * @return the value of the attribute with the specified name, or def, if there is no such attribute
	 */
	public abstract Object getAttribute(String name, Object def);
	
	/**	check if this object has a particular attribute
	 * @param	name	the name of the attribute
	 * @return true if and only if this object has an attribute with the specified name
	 */
	public abstract boolean hasAttribute(String name);
	
	/**	@return	the names of all attributes of this object, packed in an array of Strings
	 */
	public abstract String[] getAttributeNames();
	
	/**	remove an attribute from this object
	 * @param	name	the name of the attribute to be removed
	 * @return the value of the attribute with the specified name, or null, if there is no such attribute
	 */
	public abstract Object removeAttribute(String name);
	
	/**	remove all attributes from this object
	 */
	public abstract void clearAttributes();
}
