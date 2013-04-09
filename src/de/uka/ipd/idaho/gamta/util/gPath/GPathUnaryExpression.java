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


import de.uka.ipd.idaho.gamta.util.gPath.types.GPathNumber;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;

/**
 * @author sautter
 *
 * TODO document this class
 */
public class GPathUnaryExpression extends GPathExpression {
	
	//	for literals
	GPathString literal = null;
	char quoter = '"';
	
	//	for numbers
	GPathNumber number = null;
	boolean isNegative = false;
	
	//	for variable references
	String variableName = null;
	
	//	for nestings
	GPathExpression enclosedExpression = null;
	
	//	for function calls
	String functionName = null;
	GPathExpression[] functionArgs = null;
	
	//	for filter expressions
	GPathPredicate[] predicates = null;
	
	//	for path expressions
	GPath pathExpression = null;
	
	//	for union expressions
	GPathUnaryExpression[] partExpressions = null;
	
	/**	@see java.lang.Object#toString()
	 */
	public String toString() {
		if (this.literal != null) return (this.quoter + this.literal.value + this.quoter);
		if (this.number != null) return ((this.isNegative ? "-" : "") + ((((int) Math.round(this.number.value)) == this.number.value) ? ("" + ((int) Math.round(this.number.value))) : ("" + this.number.value)));
		if (this.variableName != null) return this.variableName;
		if (this.enclosedExpression != null) return ("(" + this.enclosedExpression.toString() + ")");
		if (this.functionName != null) {
			String string = this.functionName + "(";
			if ((this.functionArgs != null) && (this.functionArgs.length != 0)) {
				string += this.functionArgs[0].toString();
				for (int a = 1; a < this.functionArgs.length; a++)
					string += ", " + this.functionArgs[a].toString();
			}
			string += ")";
			if (this.predicates != null)
				for (int p = 0; p < this.predicates.length; p++)
					string += this.predicates[p];
			if (this.pathExpression != null) {
				string += "/" + this.pathExpression.toString();
			}
			return string;
		}
		if (this.pathExpression != null) return this.pathExpression.toString();
		if ((this.partExpressions != null) && (this.partExpressions.length != 0)) {
			String string = this.partExpressions[0].toString();
			for (int p = 1; p < this.partExpressions.length; p++)
				string += " | " + this.partExpressions[p];
			return string;
		}
		return "";
	}
}
