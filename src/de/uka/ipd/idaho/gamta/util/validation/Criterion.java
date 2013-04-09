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


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.validation.Validator.AbstractValidationResult;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;

/**
 * A markup criterion is the atomic validation unit for a markup process,
 * verifying an individual aspect of the markup. Actual validation is done using
 * a GPath expression that selects faulty markup from documents. If no faulty
 * markup elements are found, the criterion is passed. This approach bears quite
 * some similarity to the SchemaTron approach to XML validation.
 * 
 * @author sautter
 */
public class Criterion extends ProcessPart {
	
	public static final String CRITERION = "criterion";
	
	private static final String TEST_EXPRESSION = "testExpression";
	
	private String expression;
	
	public Criterion(ProcessPart parent, String name, String label, String description, String errorDescription, String gPath) {
		super(parent, name, label, description, errorDescription);
		this.expression = gPath;
	}
	
	public Criterion(ProcessPart parent, TreeNode data) {
		super(parent, data);
		TreeNode cNode = data.getChildNode(TEST_EXPRESSION, 0);
		cNode = cNode.getChildNode(TreeNode.DATA_NODE_TYPE, 0);
		this.expression = cNode.getNodeValue();
	}
	
	/**	@return	the GPath expression used for validating this Criterion
	 */
	public String getExpression() {
		return this.expression;
	}
	
	/**
	 * @return a validator capable of testing if a document complies with this
	 *         criterion. The validator is guarantied to produce
	 *         Criterion.CriterionValidationResults result objects
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getValidator()
	 */
	public Validator getValidator() {
		return new CriterionValidator(((Step) this.parent).getContextPath(), this.expression, this.getName(), this.getLabel(), this.getDescription(), this.getErrorDescription());
	}
	
	static class CriterionValidator extends Validator {
		
		private GPath contextPath;
		private GPathExpression predicate;
		
		CriterionValidator(String context, String predicate, String name, String label, String description, String errorDescription) {
			super(name, label, description, errorDescription);
			this.contextPath = new GPath(context);
			this.predicate = GPathParser.parseExpression(predicate);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#validate(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public ValidationResult validate(QueriableAnnotation data) {
			try {
				return this.validate(this.contextPath.evaluate(data, null));
			}
			catch (GPathException gpe) {
				return new CriterionValidationResult(this, 0, 0);
			}
		}
		
		ValidationResult validate(QueriableAnnotation[] annotations) {
			try {
				ArrayList failedList = new ArrayList();
				for (int a = 0; a < annotations.length; a++) {
					if (GPath.evaluateExpression(this.predicate, annotations[a], null).asBoolean().value)
						failedList.add(annotations[a]);
				}
				return new CriterionValidationResult(this, annotations.length, ((Annotation[]) failedList.toArray(new Annotation[failedList.size()])));
			}
			catch (GPathException gpe) {
				return new CriterionValidationResult(this, 0, 0);
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#getDetailDescription()
		 */
		public String[] getDetailDescription() {
			String[] description = {this.description};
			return description;
		}
	}
	
	/**
	 * The validation result for a criterion
	 * 
	 * @author sautter
	 */
	public static class CriterionValidationResult extends AbstractValidationResult {
		private String error;
		private int instanceCount;
		private Annotation[] failed;
		private int failureCount;
		CriterionValidationResult(CriterionValidator validator, int instanceCount, Annotation[] failed) {
			super(validator);
			this.error = ((failed.length == 0) ? null : this.validator.getErrorDescription());
			
			this.instanceCount = instanceCount;
			this.failed = failed;
			this.failureCount = failed.length;
		}
		CriterionValidationResult(CriterionValidator validator, int instanceCount, int failureCount) {
			super(validator);
			this.error = ((failureCount == 0) ? null : this.validator.getErrorDescription());
			
			this.instanceCount = instanceCount;
			this.failed = new Annotation[0];
			this.failureCount = failureCount;
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getDescription()
		 */
		public String getDescription() {
			return (this.description + " - " + ((this.error == null) ? PASSED : FAILED));
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getErrorDescription()
		 */
		public String getErrorDescription() {
			return this.error;
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getDetailDescriptions()
		 */
		public String[] getDetailDescriptions() {
			String[] description = {(this.description + " - " + ((this.error == null) ? PASSED : FAILED))};
			return description;
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getDetailErrorDescriptions()
		 */
		public String[] getDetailErrorDescriptions() {
			if (this.error == null) return new String[0];
			String[] failed = {this.error};
			return failed;
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getPartialResults()
		 */
		public ValidationResult[] getPartialResults() {
			return new ValidationResult[0];
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#isPassed()
		 */
		public boolean isPassed() {
			return (this.error == null);
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getInstanceCount()
		 */
		public int getInstanceCount() {
			return this.instanceCount;
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getFailureCount()
		 */
		public int getFailureCount() {
			return this.failureCount;
		}
		/**
		 * @return an array holding the annotations that did not pass the check
		 *         by the backing Criterion
		 */
		Annotation[] getFailed() {
			return this.failed;
		}
		
		/* 
		 * @see de.uka.ipd.idaho.gamta.util.validation.ValidationResult#writeXml(java.io.Writer)
		 */
		public void writeXml(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			bw.write("<" + CRITERION + 
					" " + NAME_ATTRIBUTE + "=\"" + this.validator.getName() + "\"" +
					" " + INSTANCES_ATTRIBUTE + "=\"" + this.getInstanceCount() + "\"" +
					" " + FAILED_ATTRIBUTE + "=\"" + this.getFailureCount() + "\"" +
					"/>");
			bw.newLine();
			bw.flush();
		}
	}
	
	/**
	 * Restore a ValidationResult from its XML representation
	 * @param data the tree node at the root of the tree representing the
	 *            validation result to load
	 * @return a CriterionValidationResult object containing the data from the
	 *         specified tree
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#loadValidationResult(de.uka.ipd.idaho.htmlXmlUtil.TreeNode)
	 */
	public ValidationResult loadValidationResult(TreeNode data) {
		return new CriterionValidationResult(
				((CriterionValidator) this.getValidator()),
				Integer.parseInt(data.getAttribute(ValidationResult.INSTANCES_ATTRIBUTE, "0")),
				Integer.parseInt(data.getAttribute(ValidationResult.FAILED_ATTRIBUTE, "0"))
				);
	}

	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getDetailDescription()
	 */
	public String[] getDetailDescription() {
		String[] desc = {this.getDescription()};
		return desc;
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#writeXml(java.io.Writer)
	 */
	public void writeXml(Writer writer) throws IOException {
		BufferedWriter bw = new BufferedWriter(writer);
		
		bw.write("<" + CRITERION + " " + NAME_ATTRIBUTE + "=\"" + this.xmlEscape(this.getName()) + "\" " + LABEL_ATTRIBUTE + "=\"" + this.xmlEscape(this.getLabel()) + "\">");
		bw.newLine();
		bw.write("<" + DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getDescription()) + "</" + DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		bw.write("<" + ERROR_DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getErrorDescription()) + "</" + ERROR_DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		bw.write("<" + TEST_EXPRESSION + ">" + this.xmlEscape(this.expression) + "</" + TEST_EXPRESSION + ">");
		bw.newLine();
		bw.write("</" + CRITERION + ">");
		bw.newLine();
		
		bw.flush();
	}
}
