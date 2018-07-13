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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.validation.Criterion.CriterionValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.Criterion.CriterionValidator;
import de.uka.ipd.idaho.gamta.util.validation.Validator.AbstractValidationResult;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A markup step represents a single part of a task. A step is validated using
 * one or more criterions. The validators of a step evaluate each criterion
 * independent of the others, regardless how many of the latter failed or
 * validated successfully.
 * 
 * @author sautter
 */
public class Step extends ProcessPart {
	
	public static final String STEP = "step";
	
	private static final String CONTEXT_PATH = "contextPath";
	
	private String context;
	private Criterion[] criterions;
	
	public Step(ProcessPart parent, String name, String label, String description, String errorDescription, String context, Criterion[] criterions) {
		super(parent, name, label, description, errorDescription);
		this.context = context;
		this.criterions = criterions;
		for (int c = 0; c < this.criterions.length; c++)
			this.criterions[c].parent = this;
	}
	
	public Step(ProcessPart parent, TreeNode data) {
		super(parent, data);
		
		TreeNode cNode = data.getChildNode(CONTEXT_PATH, 0);
		cNode = cNode.getChildNode(TreeNode.DATA_NODE_TYPE, 0);
		this.context = cNode.getNodeValue();
		
		TreeNode[] criterionNodes = data.getChildNodes(Criterion.CRITERION);
		ArrayList criterionList = new ArrayList();
		for (int c = 0; c < criterionNodes.length; c++)
			criterionList.add(new Criterion(this, criterionNodes[c]));
		
		this.criterions = ((Criterion[]) criterionList.toArray(new Criterion[criterionList.size()]));
	}
	
	/**
	 * @return the context path of this step
	 */
	public String getContextPath() {
		return this.context;
	}
	
	/**
	 * @return the Criterions of this Step
	 */
	public Criterion[] getCriterions() {
		return this.criterions;
	}
	
	/**
	 * @return a validator capable of testing if a document complies with the
	 *         criterions specified by this step. The validator is guarantied to
	 *         produce Step.StepValidationResults result objects
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getValidator()
	 */
	public Validator getValidator() {
		CriterionValidator[] criterionValidators = new CriterionValidator[this.criterions.length];
		for (int c = 0; c < this.criterions.length; c++)
			criterionValidators[c] = ((CriterionValidator) this.criterions[c].getValidator());
		return new StepValidator(this.context, criterionValidators, this.getName(), this.getLabel(), this.getDescription(), this.getErrorDescription());
	}
	
	private static class StepValidator extends Validator {
		
		private GPath contextPath;
		private CriterionValidator[] criterions;
		
		private String failIgnoreAttribute;
		
		StepValidator(String context, CriterionValidator[] criterions, String name, String label, String description, String errorDescription) {
			super(name, label, description, errorDescription);
			this.contextPath = new GPath(context);
			this.criterions = criterions;
			for (int c = 0; c < this.criterions.length; c++)
				this.criterions[c].parent = this;
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#validate(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public ValidationResult validate(QueriableAnnotation data) {
			if (this.failIgnoreAttribute == null)
				this.failIgnoreAttribute = ("_ignore_" + this.getFullName().hashCode());
			
			try {
				
				//	get context instances
				QueriableAnnotation[] instances = this.contextPath.evaluate(data, null);
				
				//	collect annotations failing individual criterions
				HashMap errorDetailLists = new HashMap();
				CriterionValidationResult[] criterionResults = new CriterionValidationResult[this.criterions.length];
				for (int c = 0; c < this.criterions.length; c++) {
					criterionResults[c] = ((CriterionValidationResult) this.criterions[c].validate(instances));
					Annotation[] failed = criterionResults[c].getFailed();
					for (int f = 0; f < failed.length; f++)
						if (!"true".equals(failed[f].getAttribute(this.failIgnoreAttribute))) {
							ArrayList errorDetailList = ((ArrayList) errorDetailLists.get(failed[f].getAnnotationID()));
							if (errorDetailList == null) {
								errorDetailList = new ArrayList();
								errorDetailLists.put(failed[f].getAnnotationID(), errorDetailList);
							}
							errorDetailList.add(this.criterions[c].getErrorDescription());
						}
				}
				
				//	put error messages in arrays
				HashMap errorDetails = new HashMap();
				for (Iterator fit = errorDetailLists.keySet().iterator(); fit.hasNext();) {
					Object faid = fit.next();
					ArrayList errorDetailList = ((ArrayList) errorDetailLists.get(faid));
					if (errorDetailList != null)
						errorDetails.put(faid, ((String[]) errorDetailList.toArray(new String[errorDetailList.size()])));
				}
				
				//	return validation result
				return new StepValidationResult(this, instances, errorDetails, criterionResults);
			}
			catch (GPathException gpe) {
				return new StepValidationResult(this, 0, 0);
			}
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#getDetailDescription()
		 */
		public String[] getDetailDescription() {
			StringVector descriptionCollector = new StringVector();
			descriptionCollector.addElement(this.getDescription());
			for (int c = 0; c < this.criterions.length; c++) {
				String[] criterionDescription = this.criterions[c].getDetailDescription();
				for (int d = 0; d < criterionDescription.length; d++)
					descriptionCollector.addElement(ValidationResult.DETAIL_INDENT + criterionDescription[d]);
			}
			return descriptionCollector.toStringArray();
		}
	}
	
	/**
	 * The validation result for a step
	 * 
	 * @author sautter
	 */
	public static class StepValidationResult extends AbstractValidationResult {
		private String error;
		private ValidationResult[] criterionResults;
		private Annotation[] instances;
		private int instanceCount;
		private HashMap failedErrorDetails = new HashMap();
		private int failureCount;
		private String failIgnoreAttribute;
		
		StepValidationResult(StepValidator validator, Annotation[] instances, Map failedErrorDetails, ValidationResult[] criterionResults) {
			super(validator);
			this.error = ((failedErrorDetails.size() == 0) ? null : this.validator.getErrorDescription());
			this.criterionResults = criterionResults;
			
			this.instances = instances;
			this.instanceCount = this.instances.length;
			this.failedErrorDetails.putAll(failedErrorDetails);
			this.failureCount = this.failedErrorDetails.size();
			
			this.failIgnoreAttribute = "_ignore_" + validator.getFullName().hashCode();
		}
		StepValidationResult(StepValidator validator, int instanceCount, int failureCount) {
			super(validator);
			this.error = ((failureCount == 0) ? null : this.validator.getErrorDescription());
			this.criterionResults = new ValidationResult[0];
			
			this.instances = new Annotation[0];
			this.instanceCount = instanceCount;
			this.failureCount = failureCount;
			
			this.failIgnoreAttribute = "_ignore_" + validator.getFullName().hashCode();
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getDescription()
		 */
		public String getDescription() {
			return (this.description + " - " + ((this.error == null) ? COMPLETE : INCOMPLETE));
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
			StringVector descriptionCollector = new StringVector();
			descriptionCollector.addElement(this.getDescription());
			for (int c = 0; c < this.criterionResults.length; c++) {
				String[] criterionDescription = this.criterionResults[c].getDetailDescriptions();
				for (int d = 0; d < criterionDescription.length; d++)
					descriptionCollector.addElement(DETAIL_INDENT + criterionDescription[d]);
			}
			return descriptionCollector.toStringArray();
		}
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getDetailErrorDescriptions()
		 */
		public String[] getDetailErrorDescriptions() {
			if (this.error == null) return new String[0];
			StringVector errorCollector = new StringVector();
			errorCollector.addElement(this.error);
			for (int c = 0; c < this.criterionResults.length; c++) {
				String[] criterionError = this.criterionResults[c].getDetailErrorDescriptions();
				for (int e = 0; e < criterionError.length; e++)
					errorCollector.addElement(DETAIL_INDENT + criterionError[e]);
			}
			return errorCollector.toStringArray();
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
		/* 
		 * @see de.uka.ipd.idaho.goldenGateScf.cms.process.ValidationResult#getPartialResults()
		 */
		public ValidationResult[] getPartialResults() {
			return this.criterionResults;
		}
		/**
		 * @return an array holding all the annotations checked by the backing
		 *         Criterion
		 */
		public Annotation[] getInstances() {
			return this.instances;
		}
		/**
		 * @return an array holding the annotations that did not pass the check
		 *         by the backing Criterion
		 */
		public Annotation[] getFailed() {
			ArrayList failedList = new ArrayList();
			for (int i = 0; i < this.instances.length; i++) {
				if (this.failedErrorDetails.containsKey(this.instances[i].getAnnotationID()))
					failedList.add(this.instances[i]);
			}
			return ((Annotation[]) failedList.toArray(new Annotation[failedList.size()]));
		}
		/**
		 * Ignore that an annotation failed the step backing this validation
		 * result. This method works only for annotations retrieved from this
		 * validation result via the getFailed() method.
		 * @param failed the annotation to ignore
		 * @return true if an annotation was marked for ignoring, false
		 *         otherwise
		 */
		public boolean ignore(Annotation failed) {
			if (failed == null) return false;
			
			if (this.failedErrorDetails.containsKey(failed.getAnnotationID())) {
				failed.setAttribute(this.failIgnoreAttribute, "true");
				this.failedErrorDetails.remove(failed.getAnnotationID());
				return true;
			}
			else return false;
		}
		/**
		 * Retrieve the detailed error messages for a given annotation. In
		 * particular, the returned array contains the error messages for the
		 * backing step's individual criterions. If the specified annotation
		 * passed the test for the backing step, this method returns an empty
		 * array, but never null.
		 * @param failed the annotation to retrieve the error details for
		 * @return an array holding the error details for the specified
		 *         annotation
		 */
		public String[] getErrorDetails(Annotation failed) {
			if (this.failedErrorDetails.containsKey(failed.getAnnotationID()))
				return ((String[]) this.failedErrorDetails.get(failed.getAnnotationID()));
			else return new String[0];
		}
		
		/* 
		 * @see de.uka.ipd.idaho.gamta.util.validation.ValidationResult#writeXml(java.io.Writer)
		 */
		public void writeXml(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			bw.write("<" + STEP + 
					" " + NAME_ATTRIBUTE + "=\"" + this.validator.getName() + "\"" +
					" " + INSTANCES_ATTRIBUTE + "=\"" + this.getInstanceCount() + "\"" +
					" " + FAILED_ATTRIBUTE + "=\"" + this.getFailureCount() + "\"" +
					((this.error == null) ? "" : (" " + ERROR_ATTRIBUTE + "=\"" + this.error + "\"")) +
					">");
			bw.newLine();
			for (int c = 0; c < this.criterionResults.length; c++)
				this.criterionResults[c].writeXml(bw);
			bw.write("</" + STEP + ">");
			bw.newLine();
			bw.flush();
		}
	}
	
	/**
	 * Restore a ValidationResult from its XML representation
	 * @param data the tree node at the root of the tree representing the
	 *            validation result to load
	 * @return a StepValidationResult object containing the data from the
	 *         specified tree
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#loadValidationResult(de.uka.ipd.idaho.htmlXmlUtil.TreeNode)
	 */
	public ValidationResult loadValidationResult(TreeNode data) {
		HashMap criterionsByName = new HashMap();
		for (int c = 0; c < this.criterions.length; c++)
			criterionsByName.put(this.criterions[c].getName(), this.criterions[c]);
		
		TreeNode[] criterionData = data.getChildNodes(Criterion.CRITERION);
		ArrayList criterionResults = new ArrayList();
		for (int c = 0; c < criterionData.length; c++) {
			Criterion criterion = ((Criterion) criterionsByName.get(criterionData[c].getAttribute(NAME_ATTRIBUTE)));
			if (criterion != null)
				criterionResults.add(criterion.loadValidationResult(criterionData[c]));
		}
		
		return new StepValidationResult(
				((StepValidator) this.getValidator()),
				Integer.parseInt(data.getAttribute(ValidationResult.INSTANCES_ATTRIBUTE, "0")),
				Integer.parseInt(data.getAttribute(ValidationResult.FAILED_ATTRIBUTE, "0"))
				);
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getDetailDescription()
	 */
	public String[] getDetailDescription() {
		StringVector descriptionCollector = new StringVector();
		descriptionCollector.addElement(this.getDescription());
		for (int c = 0; c < this.criterions.length; c++) {
			String[] criterionDescription = this.criterions[c].getDetailDescription();
			for (int d = 0; d < criterionDescription.length; d++)
				descriptionCollector.addElement(ValidationResult.DETAIL_INDENT + criterionDescription[d]);
		}
		return descriptionCollector.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#writeXml(java.io.Writer)
	 */
	public void writeXml(Writer writer) throws IOException {
		BufferedWriter bw = new BufferedWriter(writer);
		
		bw.write("<" + STEP + " " + NAME_ATTRIBUTE + "=\"" + this.xmlEscape(this.getName()) + "\" " + LABEL_ATTRIBUTE + "=\"" + this.xmlEscape(this.getLabel()) + "\">");
		bw.newLine();
		bw.write("<" + DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getDescription()) + "</" + DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		bw.write("<" + ERROR_DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getErrorDescription()) + "</" + ERROR_DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		bw.write("<" + CONTEXT_PATH + ">" + this.xmlEscape(this.getContextPath()) + "</" + CONTEXT_PATH + ">");
		bw.newLine();
		for (int c = 0; c < this.criterions.length; c++)
			this.criterions[c].writeXml(bw);
		bw.write("</" + STEP + ">");
		bw.newLine();
		
		bw.flush();
	}
}
