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
package de.uka.ipd.idaho.gamta.util.gScript.exceptions;


/**
 * Exception thrown by GScript if any exceptional condition is encountered
 * during parsing or execution.
 * 
 * @author sautter
 */
public class GScriptException extends Exception {
	
	private String[] resultsSoFar = new String[0];
	
	/**	@return the results that were collected before this excetion was thrown
	 */
	public String[] resultsSoFar() {
		return this.resultsSoFar;
	}
	
	/**
	 */
	public GScriptException() {
		super();
	}
	
	/**
	 * @param cause
	 */
	public GScriptException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * @param message
	 */
	public GScriptException(String message) {
		super(message);
	}
	
	/**
	 * @param message
	 * @param resultsSoFar
	 */
	public GScriptException(String message, String[] resultsSoFar) {
		super(message);
		this.resultsSoFar = resultsSoFar;
	}
	
	/**
	 * @param message
	 * @param resultsSoFar
	 * @param cause
	 */
	public GScriptException(String message, String[] resultsSoFar, Throwable cause) {
		super(message, cause);
		this.resultsSoFar = resultsSoFar;
	}
}
