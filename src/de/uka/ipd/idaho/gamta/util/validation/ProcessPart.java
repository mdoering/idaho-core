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
package de.uka.ipd.idaho.gamta.util.validation;


import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Abstract super class of all process validation classes.
 * 
 * @author sautter
 */
public abstract class ProcessPart {
	
	public static final String REFERENCE_ATTRIBUTE = "ref";
	public static final String NAME_ATTRIBUTE = "name";
	public static final String LABEL_ATTRIBUTE = "label";
	public static final String DESCRIPTION_ATTRIBUTE = "description";
	public static final String ERROR_DESCRIPTION_ATTRIBUTE = "errorDescription";
	
	protected ProcessPart parent;
	protected String name;
	protected String label;
	protected String description;
	protected String errorDescription;
	
	protected ProcessPart(ProcessPart parent, String name, String label, String description, String errorDescription) {
		this.parent = parent;
		
		this.name = name;
		
		this.label = label;
		if (this.label == null) this.label = this.name;
		
		this.description = description;
		if (this.description == null) this.description = this.name;
		
		this.errorDescription = errorDescription;
		if (this.errorDescription == null) this.errorDescription = ("Validation for '" + this.name + "' failed.");
	}
	
	protected ProcessPart(ProcessPart parent, TreeNode data) {
		this.parent = parent;
		
//		this.name = this.xmlUnEscape(data.getAttribute(NAME_ATTRIBUTE));
//		this.name = AnnotationUtils.unescapeFromXml(data.getAttribute(NAME_ATTRIBUTE));
		this.name = data.getAttribute(NAME_ATTRIBUTE);
//		this.label = this.xmlUnEscape(data.getAttribute(LABEL_ATTRIBUTE, this.name));
//		this.label = AnnotationUtils.unescapeFromXml(data.getAttribute(LABEL_ATTRIBUTE, this.name));
		this.label = data.getAttribute(LABEL_ATTRIBUTE, this.name);
		
		TreeNode dNode = data.getChildNode(DESCRIPTION_ATTRIBUTE, 0);
		if (dNode != null) {
			dNode = dNode.getChildNode(TreeNode.DATA_NODE_TYPE, 0);
			if (dNode != null)
//				this.description = this.xmlUnEscape(dNode.getNodeValue());
//				this.description = AnnotationUtils.unescapeFromXml(dNode.getNodeValue());
				this.description = dNode.getNodeValue();
		}
		if (this.description == null) this.description = this.name;
		
		TreeNode eNode = data.getChildNode(ERROR_DESCRIPTION_ATTRIBUTE, 0);
		if (eNode != null) {
			eNode = eNode.getChildNode(TreeNode.DATA_NODE_TYPE, 0);
			if (eNode != null)
//				this.errorDescription = this.xmlUnEscape(eNode.getNodeValue());
//				this.errorDescription = AnnotationUtils.unescapeFromXml(eNode.getNodeValue());
				this.errorDescription = eNode.getNodeValue();
		}
		if (this.errorDescription == null) this.errorDescription = ("Validation for '" + this.name + "' failed.");
	}
	
	/**
	 * @return a validator capable of testing if a document complies with the
	 *         markup criteriona specified by this markup process part
	 */
	public abstract Validator getValidator();
	
	/**
	 * Evaluate if a document complies with the markup criterions specified by
	 * this markup process part
	 * @param data the document to validate
	 * @return a ValidationResult object containing the result of the
	 *         validation, in all details, from the markup process as a whole
	 *         down to the individual criterions.
	 */
	public final ValidationResult validate(QueriableAnnotation data) {
		return this.getValidator().validate(data);
	}
	
	/**
	 * Restore a ValidationResult from its XML representation. The individual
	 * process parts return an instance of their validation results.
	 * @param data the tree node at the root of the tree representing the
	 *            validation result to load
	 * @return a ValidationResult object containing the data from the specified
	 *         tree
	 */
	public abstract ValidationResult loadValidationResult(TreeNode data);
	
	/**
	 * @return the name identifying the markup process part
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return the name identifying the markup process part, prefixed with the
	 *         full names identifying the parent parts
	 */
	public String getFullName() {
		return ((this.parent == null) ? "" : (this.parent.getFullName() + ".")) + this.getName();
	}
	
	/**
	 * @return the label to display for the markup process part
	 */
	public String getLabel() {
		return this.label;
	}
	
	/**
	 * @return the textual description of the markup process part
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * @return the textual description of the error to report if a document does
	 *         not comply with the criteria specified by the markup process part
	 */
	public String getErrorDescription() {
		return this.errorDescription;
	}
	
	/**
	 * @return the description of the markup process part and all possible sub
	 *         parts
	 */
	public abstract String[] getDetailDescription();
	
	/**
	 * @return an XML definition of this markup process part
	 */
	public String toXml() {
		StringWriter sw = new StringWriter();
		try {
			this.writeXml(sw);
		} catch (IOException ioe) {}
		return sw.toString();
	}
	
	/**
	 * write an XML definition of this markup process part to some writer
	 * @param writer the Writer to write to
	 */
	public abstract void writeXml(Writer writer) throws IOException;
	
	/**
	 * Escape a string to be well-formed XML - in particular, escape &amp;,
	 * &lt;, &gt;, and &quot;
	 * @param string the string to escape
	 * @return the escaped string
	 */
	protected String xmlEscape(String string) {
		return escaper.escape(string);
//		StringBuffer escaped = new StringBuffer();
//		for (int c = 0; c < string.length(); c++) {
//			char ch = string.charAt(c);
//			if (ch == '<') escaped.append("&lt;");
//			else if (ch == '>') escaped.append("&gt;");
//			else if (ch == '"') escaped.append("&quot;");
//			else if (ch == '&') escaped.append("&amp;");
//			else escaped.append(ch);
//		}
//		return escaped.toString();
	}
	private static final Grammar escaper = new StandardGrammar();
//	
//	/**
//	 * un-escape a string from its XML encoding - in particular, un-escape
//	 * &amp;amp;, &amp;lt;, &amp;gt;, and &amp;quot;
//	 * @param string the string to escape
//	 * @return the escaped string
//	 */
//	protected String xmlUnEscape(String string) {
//		StringBuffer unEscaped = new StringBuffer();
//		for (int c = 0; c < string.length();) {
//			char ch = string.charAt(c);
//			if (ch == '&') {
//				if (string.startsWith("amp;", (c+1))) {
//					unEscaped.append('&');
//					c+=5;
//				}
//				else if (string.startsWith("lt;", (c+1))) {
//					unEscaped.append('<');
//					c+=4;
//				}
//				else if (string.startsWith("gt;", (c+1))) {
//					unEscaped.append('>');
//					c+=4;
//				}
//				else if (string.startsWith("quot;", (c+1))) {
//					unEscaped.append('"');
//					c+=6;
//				}
//				else {
//					unEscaped.append(ch);
//					c++;
//				}
//			}
//			else {
//				unEscaped.append(ch);
//				c++;
//			}
//		}
//		return unEscaped.toString();
//	}
}
