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
 * Interface for a lookup dictionary
 * 
 * @author sautter
 */
public interface Dictionary {
	
	/**	check if a String is contained in this Dictionary, using the Dictionaries default case sensitivity.
	 * @param	string	the String to look up
	 * @return true if and only if the specified String is contained in the Dictionary
	 */
	public abstract boolean lookup(String string);
	
	/**	check if a String is contained in this Dictionary
	 * @param	string			the String to look up
	 * @param	caseSensitive	do lookup case sensitive?
	 * @return true if and only if the specified String is contained in the Dictionary
	 */
	public abstract boolean lookup(String string, boolean caseSensitive);
	
	/**	@return	true if and only if lookups in this Dictionary are case sensitive by default
	 */
	public abstract boolean isDefaultCaseSensitive();
	
	/**	@return	true if and only if this Dictionary contains no entries
	 */
	public abstract boolean isEmpty();
	
	/**	@return the number of entries in this Dictionary
	 */
	public abstract int size();
	
	/**	@return	an Iterator for iterating over the entries of this Dictionary
	 */
	public abstract StringIterator getEntryIterator(); 
}
