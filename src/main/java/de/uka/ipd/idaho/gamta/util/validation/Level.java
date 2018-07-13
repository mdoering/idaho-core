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
 * A markup level encircles one major stage from a raw document to the final,
 * fully XML annotated result. A level consists of several tasks, which mark
 * activities that can be executed in parallel. In addition, a level may have a
 * set of synchronization criterions, which can validate the consistency of the
 * result of individual tasks among each other. The validators of a level
 * validate each of the tasks, regardless of the results for the respective
 * other tasks. The level criterions are always evaluated as well.
 * 
 * @author sautter
 */
public class Level extends ProcessPart {
	
	public static final String LEVEL = "level";
	
	private Task[] tasks;
	
	public Level(ProcessPart parent, String name, String label, String description, String errorDescription, Task[] tasks) {
		super(parent, name, label, description, errorDescription);
		
		this.tasks = tasks;
		for (int t = 0; t < this.tasks.length; t++)
			this.tasks[t].parent = this;
	}
	
	public Level(ProcessPart parent, TreeNode data) {
		super(parent, data);
		
		TreeNode[] taskNodes = data.getChildNodes(Task.TASK);
		ArrayList taskList = new ArrayList();
		for (int t = 0; t < taskNodes.length; t++)
			taskList.add(new Task(this, taskNodes[t]));
		
		this.tasks = ((Task[]) taskList.toArray(new Task[taskList.size()]));
	}
	
	/**	@return	the Tasks of this Level
	 */
	public Task[] getTasks() {
		return this.tasks;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.cms.process.ProcessPart#getFullName()
	 */
	public String getFullName() {
		return this.getName();
	}
	
	/**
	 * @return the names of the tasks in this level
	 */
	public String[] getTaskNames() {
		String[] taskNames = new String[this.tasks.length];
		for (int t = 0; t < this.tasks.length; t++)
			taskNames[t] = this.tasks[t].getName();
		return taskNames;
	}

	/**
	 * @return the names of the steps in this level, prefixed with the names
	 *         of the tasks they belong to
	 */
	public String[] getStepNames() {
		StringVector stepNames = new StringVector();
		for (int t = 0; t < this.tasks.length; t++) {
			String[] taskStepNames = this.tasks[t].getStepNames();
			for (int s = 0; s < taskStepNames.length; s++)
				stepNames.addElement(this.tasks[t].getName() + "." + taskStepNames[s]);
		}
		return stepNames.toStringArray();
	}
	
	/**
	 * @return a validator capable of testing if a document complies with the
	 *         criterions specified by this level. The validator is guarantied to
	 *         produce Level.LevelValidationResults result objects
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getValidator()
	 */
	public Validator getValidator() {
		Validator[] taskValidators = new Validator[this.tasks.length];
		for (int t = 0; t < this.tasks.length; t++)
			taskValidators[t] = this.tasks[t].getValidator();
		return new LevelValidator(taskValidators, /*criterionValidators, */this.getName(), this.getLabel(), this.getDescription(), this.getErrorDescription());
	}
	
	private class LevelValidator extends Validator {
		
		private Validator[] tasks;
		
		LevelValidator(Validator[] tasks, /*Validator[] criterions, */String name, String label, String description, String errorDescription) {
			super(name, label, description, errorDescription);
			
			this.tasks = tasks;
			for (int t = 0; t < this.tasks.length; t++)
				this.tasks[t].parent = this;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#getFullName()
		 */
		public String getFullName() {
			return this.getName();
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#validate(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public ValidationResult validate(QueriableAnnotation data) {
			String firstError = null;
			ValidationResult[] taskResults = new ValidationResult[this.tasks.length];
			for (int t = 0; t < this.tasks.length; t++) {
				if (firstError == null) {
					ValidationResult tvr = this.tasks[t].validate(data);
					taskResults[t] = tvr;
					firstError = tvr.getErrorDescription();
				}
				else taskResults[t] = new SkippedPartResult(this.tasks[t]);
			}
			return new LevelValidationResult(this, firstError, taskResults/*, criterionResults*/);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#getDetailDescription()
		 */
		public String[] getDetailDescription() {
			StringVector descriptionCollector = new StringVector();
			descriptionCollector.addElement(this.getDescription());
			for (int t = 0; t < this.tasks.length; t++) {
				String[] taskDescription = this.tasks[t].getDetailDescription();
				for (int d = 0; d < taskDescription.length; d++)
					descriptionCollector.addElement(ValidationResult.DETAIL_INDENT + taskDescription[d]);
			}
			return descriptionCollector.toStringArray();
		}
	}
	
	/**
	 * The validation result for a level
	 * 
	 * @author sautter
	 */
	public static class LevelValidationResult extends AbstractValidationResult {
		private String error;
		private ValidationResult[] taskResults;
		private int failureCount = 0;
		LevelValidationResult(LevelValidator validator, String error, ValidationResult[] taskResults/*, ValidationResult[] criterionResults*/) {
			super(validator);
			this.error = error;
			this.taskResults = taskResults;
			for (int t = 0; t < this.taskResults.length; t++)
				if (!this.taskResults[t].isPassed())
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
			for (int t = 0; t < this.taskResults.length; t++) {
				String[] taskDescription = this.taskResults[t].getDetailDescriptions();
				for (int d = 0; d < taskDescription.length; d++)
					descriptionCollector.addElement(DETAIL_INDENT + taskDescription[d]);
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
			for (int t = 0; t < this.taskResults.length; t++) {
				String[] taskError = this.taskResults[t].getDetailErrorDescriptions();
				for (int e = 0; e < taskError.length; e++)
					errorCollector.addElement(DETAIL_INDENT + taskError[e]);
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
			return (this.taskResults.length/* + this.criterionResults.length*/);
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
			ValidationResult[] partialResults = new ValidationResult[this.taskResults.length/* + this.criterionResults.length*/];
			System.arraycopy(this.taskResults, 0, partialResults, 0, this.taskResults.length);
			return partialResults;
		}
		/* 
		 * @see de.uka.ipd.idaho.gamta.util.validation.ValidationResult#writeXml(java.io.Writer)
		 */
		public void writeXml(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			bw.write("<" + LEVEL + 
					" " + NAME_ATTRIBUTE + "=\"" + this.validator.getName() + "\"" +
					((this.error == null) ? "" : (" " + ERROR_ATTRIBUTE + "=\"" + this.error + "\"")) +
					">");
			bw.newLine();
			for (int t = 0; t < this.taskResults.length; t++)
				this.taskResults[t].writeXml(bw);
			bw.write("</" + LEVEL + ">");
			bw.newLine();
			bw.flush();
		}
	}
	
	/**
	 * Restore a ValidationResult from its XML representation
	 * @param data the tree node at the root of the tree representing the
	 *            validation result to load
	 * @return a LevelValidationResult object containing the data from the
	 *         specified tree
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#loadValidationResult(de.uka.ipd.idaho.htmlXmlUtil.TreeNode)
	 */
	public ValidationResult loadValidationResult(TreeNode data) {
		
		HashMap tasksByName = new HashMap();
		for (int t = 0; t < this.tasks.length; t++)
			tasksByName.put(this.tasks[t].getName(), this.tasks[t]);
		
		TreeNode[] taskData = data.getChildNodes(Task.TASK);
		ArrayList taskResults = new ArrayList();
		for (int t = 0; t < taskData.length; t++) {
			Task task = ((Task) tasksByName.get(taskData[t].getAttribute(NAME_ATTRIBUTE)));
			if (task != null)
				taskResults.add(task.loadValidationResult(taskData[t]));
		}
		
		return new LevelValidationResult(
				((LevelValidator) this.getValidator()),
				data.getAttribute(ValidationResult.ERROR_ATTRIBUTE),
				((ValidationResult[]) taskResults.toArray(new ValidationResult[taskResults.size()]))/*,
				((ValidationResult[]) criterionResults.toArray(new ValidationResult[criterionResults.size()]))*/
				);
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getDetailDescription()
	 */
	public String[] getDetailDescription() {
		StringVector descriptionCollector = new StringVector();
		descriptionCollector.addElement(this.getDescription());
		for (int t = 0; t < this.tasks.length; t++) {
			String[] taskDescription = this.tasks[t].getDetailDescription();
			for (int d = 0; d < taskDescription.length; d++)
				descriptionCollector.addElement(ValidationResult.DETAIL_INDENT + taskDescription[d]);
		}
		return descriptionCollector.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#writeXml(java.io.Writer)
	 */
	public void writeXml(Writer writer) throws IOException {
		BufferedWriter bw = new BufferedWriter(writer);
		
		bw.write("<" + LEVEL + " " + NAME_ATTRIBUTE + "=\"" + this.xmlEscape(this.getName()) + "\" " + LABEL_ATTRIBUTE + "=\"" + this.xmlEscape(this.getLabel()) + "\">");
		bw.newLine();
		bw.write("<" + DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getDescription()) + "</" + DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		bw.write("<" + ERROR_DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getErrorDescription()) + "</" + ERROR_DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		for (int t = 0; t < this.tasks.length; t++)
			this.tasks[t].writeXml(bw);
		bw.write("</" + LEVEL + ">");
		bw.newLine();
		
		bw.flush();
	}
}
