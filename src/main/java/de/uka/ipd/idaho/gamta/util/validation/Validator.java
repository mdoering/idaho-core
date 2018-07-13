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
import java.io.Writer;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;

/**
 * A validator performs the process of document validation, or a part of it.
 * 
 * @author sautter
 */
public abstract class Validator {
	
	/**
	 * Abstract implementation of a validation result, used in different
	 * validator implementations. In particular, this class implements all the
	 * convenience methods for success and failure rates based on the
	 * getInstanceCount() and getFailureCount() methods, which this class leaves
	 * abstract.
	 * 
	 * @author sautter
	 */
	public static abstract class AbstractValidationResult implements ValidationResult {
		protected Validator validator;
		protected String description;
		/**
		 * Constructor
		 * @param validator the validator that created the result
		 */
		protected AbstractValidationResult(Validator validator) {
			this.validator = validator;
			this.description = this.validator.getDescription();
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getValidator()
		 */
		public Validator getValidator() {
			return this.validator;
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getFailureRate()
		 */
		public float getFailureRate() {
			float ic = this.getInstanceCount();
			float fc = this.getFailureCount();
			return ((ic == 0) ? 0 : (fc / ic));
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getFailurePercent()
		 */
		public int getFailurePercent() {
			return ((int) (this.getFailureRate() * 100));
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getPassCount()
		 */
		public int getPassCount() {
			return (this.getInstanceCount() - this.getFailureCount());
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getSuccessRate()
		 */
		public float getSuccessRate() {
			return (1 - this.getFailureRate());
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getSuccessPercent()
		 */
		public int getSuccessPercent() {
			return ((int) (this.getSuccessRate() * 100));
		}
	}
	
	/**
	 * Placeholder result to represent the result of a validation part that was
	 * skipped in a hierachical part of a validation process because one of the
	 * previous validation parts failed. This is useful for the criterions of a
	 * markup step, for the steps of a markup task, and for the levels of a
	 * markup process.
	 * 
	 * @author sautter
	 */
	public static class SkippedPartResult extends AbstractValidationResult {
		/**
		 * Constructor
		 * @param validator the validator that created the result
		 */
		public SkippedPartResult(Validator validator) {
			super(validator);
		}
		/* (non-Javadoc)
		 * @see de.goldenGateScf.cms.process.ValidationResult#getDeatilErrorDescriptions()
		 */
		public String[] getDetailErrorDescriptions() {
			String[] failed = {this.description + " - " + SKIPPED};
			return failed;
		}
		/* (non-Javadoc)
		 * @see de.goldenGateScf.cms.process.ValidationResult#getDescription()
		 */
		public String getDescription() {
			return (this.description + " - " + SKIPPED);
		}
		/* (non-Javadoc)
		 * @see de.goldenGateScf.cms.process.ValidationResult#getDetailDescriptions()
		 */
		public String[] getDetailDescriptions() {
			String[] failed = {this.description + " - " + SKIPPED};
			return failed;
		}
		/* (non-Javadoc)
		 * @see de.goldenGateScf.cms.process.ValidationResult#getErrorDescription()
		 */
		public String getErrorDescription() {
			return (this.description + " - " + SKIPPED);
		}
		/* (non-Javadoc)
		 * @see de.goldenGateScf.cms.process.ValidationResult#getPartialResults()
		 */
		public ValidationResult[] getPartialResults() {
			return new ValidationResult[0];
		}
		/* (non-Javadoc)
		 * @see de.goldenGateScf.cms.process.ValidationResult#isPassed()
		 */
		public boolean isPassed() {
			return false;
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getFailureCount()
		 */
		public int getFailureCount() {
			return 0;
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getInstanceCount()
		 */
		public int getInstanceCount() {
			return 0;
		}
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateServer.cms.process.ValidationResult#writeXml(java.io.Writer)
		 */
		public void writeXml(Writer out) throws IOException {
			//	don't include skipped partial results in XML representations
		}
	}
	
	protected Validator parent;
	protected String name;
	protected String label;
	protected String description;
	protected String errorDescription;
	
	/**
	 * Constructor
	 * @param name a name identifying the Validator, in particular the name of
	 *            the backing markup process part
	 * @param label a label to use for displaying the Validator
	 * @param description a textual description of the test performed by the
	 *            Validator
	 * @param errorDescription a textual description of the error indicated by a
	 *            failure of the test performed by the Validator
	 */
	protected Validator(String name, String label, String description, String errorDescription) {
		this.name = name;
		this.label = label;
		this.description = description;
		this.errorDescription = errorDescription;
	}
	
	/** test whether a document complies with the requirements of this Validator
	 * @param	data	the document to test
	 * @return a MarkupTestResult describing the result of the test
	 */
	public abstract ValidationResult validate(QueriableAnnotation data);
	
	/**
	 * @return a name identifying the Validator, in particular the name of the
	 *         backing markup process part
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * @return the name identifying the Validator, prefixed with the full names
	 *         identifying the parent Validators, , in particular the full name
	 *         of the backing markup process part
	 */
	public String getFullName() {
		return ((this.parent == null) ? "" : (this.parent.getFullName() + ".")) + this.getName();
	}
	
	/** @return a label to use for displaying the Validator
	 */
	public String getLabel() {
		return this.label;
	}
	
	/** @return a textual description of the test performed by the Validator
	 */
	public String getDescription() {
		return this.description;
	}
	
	/** @return	textual description of the error indicated by a failure of the test performed by the Validator
	 */
	public String getErrorDescription() {
		return this.errorDescription;
	}
	
	/**	@return textual descriptions of the test performed by this Validator
	 */
	public abstract String[] getDetailDescription();
//	
//	/**	@return	an XML definition of this markup validator
//	 */
//	public String toXml() {
//		StringWriter sw = new StringWriter();
//		try {
//			this.writeXml(sw);
//		} catch (IOException ioe) {}
//		return sw.toString();
//	}
//	
//	/**	write an XML definition of this markup validator to some writer
//	 * @param	writer	the Writer to write to
//	 */
//	public abstract void writeXml(Writer writer) throws IOException;
}
