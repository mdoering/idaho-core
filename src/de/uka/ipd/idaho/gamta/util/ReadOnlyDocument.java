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
package de.uka.ipd.idaho.gamta.util;


import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;

/**
 * This wrapper seals a QueriableAnnotation, giving full read access to its content,
 * but blocking any modifications by throwing a RuntimeException whenever a
 * modifying method is invoked
 */

public class ReadOnlyDocument extends GenericQueriableAnnotationWrapper {
	
	private static final String DEFAULT_EXCEPTION_TEXT = "This view on the document is read-only and cannot be modified.";
	private String exceptionText;
	
	/**	Constructor
	 * @param	data	the document or snnotation to seal
	 */
	public ReadOnlyDocument(QueriableAnnotation data) {
		this(data, null);
	}
	
	/**	Constructor
	 * @param	data			the document or annotation to seal
	 * @param	exceptionText	the message text for the exceptions thrown on modification attempts
	 */
	public ReadOnlyDocument(QueriableAnnotation data, String exceptionText) {
		super(data);
		this.exceptionText = ((exceptionText == null) ? DEFAULT_EXCEPTION_TEXT : exceptionText);
	}
	
	public String changeTypeTo(String newType) {
		throw new RuntimeException(this.exceptionText);
	}
	
	public void clearAttributes() {
		throw new RuntimeException(this.exceptionText);
	}

	public void copyAttributes(Attributed source) {
		throw new RuntimeException(this.exceptionText);
	}
	
	public Object removeAttribute(String name) {
		throw new RuntimeException(this.exceptionText);
	}
	
	public Object setAttribute(String name, Object value) {
		throw new RuntimeException(this.exceptionText);
	}
	
	protected QueriableAnnotation wrapAnnotation(QueriableAnnotation annotation) {
		return new ReadOnlyDocument(annotation);
	}
	
	protected Token wrapToken(Token token) {
		return new ReadOnlyToken(token);
	}
	
	private class ReadOnlyToken extends GenericTokenWrapper {

		/** Constructor
		 * @param token
		 */
		public ReadOnlyToken(Token token) {
			super(token);
		}
		
		public void clearAttributes() {
			throw new RuntimeException(exceptionText);
		}

		public void copyAttributes(Attributed source) {
			throw new RuntimeException(exceptionText);
		}
		
		public Object removeAttribute(String name) {
			throw new RuntimeException(exceptionText);
		}
		
		public Object setAttribute(String name, Object value) {
			throw new RuntimeException(exceptionText);
		}
	}
}
