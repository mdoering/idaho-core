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

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.validation.Validator.AbstractValidationResult;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A task represents an individual activity within a markup level. A task
 * consists of one or more steps, which are executed sequentially. The
 * validators of a task validate the steps one by one, and each step is only
 * evaluated if the previous one validated successfully.
 * 
 * @author sautter
 */
public class Task extends ProcessPart {
	
	public static final String TASK = "task";
	
	private Step[] steps;
	
	public Task(ProcessPart parent, String name, String label, String description, String errorDescription, Step[] steps) {
		super(parent, name, label, description, errorDescription);
		
		this.steps = steps;
		for (int s = 0; s < this.steps.length; s++)
			this.steps[s].parent = this;
	}
	
	public Task(ProcessPart parent, TreeNode data) {
		super(parent, data);
		
		TreeNode[] stepNodes = data.getChildNodes(Step.STEP);
		ArrayList stepList = new ArrayList();
		for (int s = 0; s < stepNodes.length; s++)
			stepList.add(new Step(this, stepNodes[s]));
		
		this.steps = ((Step[]) stepList.toArray(new Step[stepList.size()]));
	}
	
	/**	
	 * @return	the Steps of this Task
	 */
	public Step[] getSteps() {
		return this.steps;
	}
	
	/**
	 * @return the names of the steps in this task
	 */
	public String[] getStepNames() {
		String[] stepNames = new String[this.steps.length];
		for (int s = 0; s < this.steps.length; s++)
			stepNames[s] = this.steps[s].getName();
		return stepNames;
	}
	
	/**
	 * @return a validator capable of testing if a document complies with the
	 *         criterions specified by this task. The validator is guarantied to
	 *         produce Task.TaskValidationResults result objects
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getValidator()
	 */
	public Validator getValidator() {
		Validator[] stepValidators = new Validator[this.steps.length];
		for (int s = 0; s < this.steps.length; s++)
			stepValidators[s] = this.steps[s].getValidator();
		return new TaskValidator(stepValidators, this.getName(), this.getLabel(), this.getDescription(), this.getErrorDescription());
	}
	
	private class TaskValidator extends Validator {
		
		private Validator[] steps;
		
		TaskValidator(Validator[] steps, String name, String label, String description, String errorDescription) {
			super(name, label, description, errorDescription);
			
			this.steps = steps;
			for (int s = 0; s < this.steps.length; s++)
				this.steps[s].parent = this;
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#validate(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public ValidationResult validate(QueriableAnnotation data) {
			String firstError = null;
			ValidationResult[] stepResults = new ValidationResult[this.steps.length];
			for (int s = 0; s < this.steps.length; s++) {
				if (firstError == null) {
					ValidationResult svr = this.steps[s].validate(data);
					stepResults[s] = svr;
					firstError = svr.getErrorDescription();
				}
				else stepResults[s] = new SkippedPartResult(this.steps[s]);
			}
			return new TaskValidationResult(this, firstError, stepResults);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#getDetailDescription()
		 */
		public String[] getDetailDescription() {
			StringVector descriptionCollector = new StringVector();
			descriptionCollector.addElement(this.getDescription());
			for (int s = 0; s < this.steps.length; s++) {
				String[] stepDescription = this.steps[s].getDetailDescription();
				for (int d = 0; d < stepDescription.length; d++)
					descriptionCollector.addElement(ValidationResult.DETAIL_INDENT + stepDescription[d]);
			}
			return descriptionCollector.toStringArray();
		}
	}
	
	/**
	 * The validation result for a task
	 * 
	 * @author sautter
	 */
	public static class TaskValidationResult extends AbstractValidationResult {
		private String error;
		private ValidationResult[] stepResults;
		private int failureCount = 0;
		TaskValidationResult(TaskValidator validator, String error, ValidationResult[] stepResults) {
			super(validator);
			this.error = error;
			this.stepResults = stepResults;
			for (int s = 0; s < this.stepResults.length; s++)
				if (!this.stepResults[s].isPassed())
					this.failureCount++;
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
			for (int s = 0; s < this.stepResults.length; s++) {
				String[] stepDescription = this.stepResults[s].getDetailDescriptions();
				for (int d = 0; d < stepDescription.length; d++)
					descriptionCollector.addElement(DETAIL_INDENT + stepDescription[d]);
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
			for (int s = 0; s < this.stepResults.length; s++) {
				String[] stepError = this.stepResults[s].getDetailErrorDescriptions();
				for (int e = 0; e < stepError.length; e++)
					errorCollector.addElement(DETAIL_INDENT + stepError[e]);
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
			return this.stepResults.length;
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
			return this.stepResults;
		}
		/* 
		 * @see de.uka.ipd.idaho.gamta.util.validation.ValidationResult#writeXml(java.io.Writer)
		 */
		public void writeXml(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			bw.write("<" + TASK + 
					" " + NAME_ATTRIBUTE + "=\"" + this.validator.getName() + "\"" +
					((this.error == null) ? "" : (" " + ERROR_ATTRIBUTE + "=\"" + this.error + "\"")) +
					">");
			bw.newLine();
			for (int s = 0; s < this.stepResults.length; s++)
				this.stepResults[s].writeXml(bw);
			bw.write("</" + TASK + ">");
			bw.newLine();
			bw.flush();
		}
	}
	
	/**
	 * Restore a ValidationResult from its XML representation
	 * @param data the tree node at the root of the tree representing the
	 *            validation result to load
	 * @return a TaskValidationResult object containing the data from the
	 *         specified tree
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#loadValidationResult(de.uka.ipd.idaho.htmlXmlUtil.TreeNode)
	 */
	public ValidationResult loadValidationResult(TreeNode data) {
		HashMap stepsByName = new HashMap();
		for (int s = 0; s < this.steps.length; s++)
			stepsByName.put(this.steps[s].getName(), this.steps[s]);
		
		TreeNode[] stepData = data.getChildNodes(Step.STEP);
		ArrayList stepResults = new ArrayList();
		for (int s = 0; s < stepData.length; s++) {
			Step step = ((Step) stepsByName.get(stepData[s].getAttribute(NAME_ATTRIBUTE)));
			if (step != null)
				stepResults.add(step.loadValidationResult(stepData[s]));
		}
		
		return new TaskValidationResult(
				((TaskValidator) this.getValidator()),
				data.getAttribute(ValidationResult.ERROR_ATTRIBUTE),
				((ValidationResult[]) stepResults.toArray(new ValidationResult[stepResults.size()]))
				);
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getDetailDescription()
	 */
	public String[] getDetailDescription() {
		StringVector descriptionCollector = new StringVector();
		descriptionCollector.addElement(this.getDescription());
		for (int s = 0; s < this.steps.length; s++) {
			String[] stepDescription = this.steps[s].getDetailDescription();
			for (int d = 0; d < stepDescription.length; d++)
				descriptionCollector.addElement(ValidationResult.DETAIL_INDENT + stepDescription[d]);
		}
		return descriptionCollector.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#writeXml(java.io.Writer)
	 */
	public void writeXml(Writer writer) throws IOException {
		BufferedWriter bw = new BufferedWriter(writer);
		
		bw.write("<" + TASK + " " + NAME_ATTRIBUTE + "=\"" + this.xmlEscape(this.getName()) + "\" " + LABEL_ATTRIBUTE + "=\"" + this.xmlEscape(this.getLabel()) + "\">");
		bw.newLine();
		bw.write("<" + DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getDescription()) + "</" + DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		bw.write("<" + ERROR_DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getErrorDescription()) + "</" + ERROR_DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		for (int t = 0; t < this.steps.length; t++)
			this.steps[t].writeXml(bw);
		bw.write("</" + TASK + ">");
		bw.newLine();
		
		bw.flush();
	}
}
