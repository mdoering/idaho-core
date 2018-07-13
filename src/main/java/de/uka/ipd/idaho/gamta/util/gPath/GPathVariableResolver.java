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
package de.uka.ipd.idaho.gamta.util.gPath;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import de.uka.ipd.idaho.gamta.util.gPath.types.GPathObject;

/**
 * resolver for variables in gamta path expressions
 * 
 * @author sautter
 */
public class GPathVariableResolver {
	
	private static final GPathVariableResolver DUMMY = new GPathVariableResolver();
	
	/**	@return	the dummy resolver to use in (the rather frequent) circumstances that no variables are used
	 */
	static GPathVariableResolver getDummyResolver() {
		DUMMY.variables.clear();
		return DUMMY;
	}
	
	private HashMap variables = new HashMap();
	private GPathVariableResolver defaults = null;
	
	/** Constructor
	 */
	public GPathVariableResolver() {}
	
	/** Constructor
	 * @param	defaults	the resolver to use as a fallback if a variable is not set in this resolver
	 */
	public GPathVariableResolver(GPathVariableResolver defaults) {
		this.defaults = defaults;
	}
	
	/**	set a variable
	 * @param	name	the name of the variable
	 * @param	value	the value of the variable
	 * @return the old value of the specified variable
	 */
	public GPathObject setVariable(String name, GPathObject value) {
		return ((GPathObject) this.variables.put(name, value));
	}
	
	/**	remove a variable
	 * @param	name	the name of the variable
	 * @return the value of the removed variable
	 */
	public GPathObject removeVariable(String name) {
		return ((GPathObject) this.variables.remove(name));
	}
	
	/**	retrieve the value of a variable
	 * @param	name	the name of the variable
	 * @return the value of the variable
	 */
	public GPathObject getVariable(String name) {
		GPathObject value = ((GPathObject) this.variables.get(name));
		return ((value == null) ? ((this.defaults == null) ? null : this.defaults.getVariable(name)) : value);
	}
	
	/**	retrieve the value of a variable
	 * @param	name	the name of the variable
	 * @param	def		the default value to return if the variable is not set
	 * @return the value of the variable
	 */
	public GPathObject getVariable(String name, GPathObject def) {
		GPathObject value = ((GPathObject) this.variables.get(name));
		return ((value == null) ? ((this.defaults == null) ? def : this.defaults.getVariable(name, def)) : value);
	}
	
	/**	retrieve the names of all variables in this resolver (this will not include the variables defined in the fallback resolver)
	 * @return an array holding the names of all variables in this resolver
	 */
	public String[] getVariableNames() {
		ArrayList vNames = new ArrayList(this.variables.keySet());
		Collections.sort(vNames, String.CASE_INSENSITIVE_ORDER);
		return ((String[]) vNames.toArray(new String[vNames.size()]));
	}
	
	/**	test if a variable is set
	 * @param	name	the name of the variable
	 * @return true if the variable is set, i.e., has a value assigned to it
	 */
	public boolean isVariableSet(String name) {
		return (this.variables.containsKey(name) || ((this.defaults != null) && this.defaults.isVariableSet(name)));
	}
	
	/**	clear the variable set
	 */
	public void clear() {
		this.variables.clear();
	}
}
