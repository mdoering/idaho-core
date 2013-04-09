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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.validation.Validator.AbstractValidationResult;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A validatable markup process capable of measuring the progress in the process
 * marking up a document in XML. A process consists of one or more levels, which
 * build on each other and describe and validate the markup process from start
 * to end. The validators of a process work level by level, and a level is only
 * considered in validation if the previous one is complete, i.e., its
 * validation did not find any errors.
 * 
 * @author sautter
 */
public class Process extends ProcessPart {
	
	public static final String PROCESS = "process";
	
	private Level[] levels;
	
	public Process(String name, String label, String description, String errorDescription, Level[] levels) {
		super(null, name, label, description, errorDescription);
		
		this.levels = levels;
		for (int l = 0; l < this.levels.length; l++)
			this.levels[l].parent = this;
	}
	
	public Process(TreeNode data) {
		super(null, data);
		
		TreeNode[] levelNodes = data.getChildNodes(Level.LEVEL);
		ArrayList levelList = new ArrayList();
		for (int l = 0; l < levelNodes.length; l++)
			levelList.add(new Level(this, levelNodes[l]));
		
		this.levels = ((Level[]) levelList.toArray(new Level[levelList.size()]));
	}
	
	/**	@return	the Levels of this Process
	 */
	public Level[] getLevels() {
		return this.levels;
	}
	
	/**
	 * @return the names of the levels in this process
	 */
	public String[] getLevelNames() {
		String[] levelNames = new String[this.levels.length];
		for (int l = 0; l < this.levels.length; l++)
			levelNames[l] = this.levels[l].getName();
		return levelNames;
	}

	/**
	 * @return the names of the tasks in this process, prefixed with the names
	 *         of the levels they belong to
	 */
	public String[] getTaskNames() {
		StringVector taskNames = new StringVector();
		for (int l = 0; l < this.levels.length; l++) {
			String[] levelTaskNames = this.levels[l].getTaskNames();
			for (int t = 0; t < levelTaskNames.length; t++)
				taskNames.addElement(this.levels[l].getName() + "." + levelTaskNames[t]);
		}
		return taskNames.toStringArray();
	}

	/**
	 * @return the names of the steps in this process, prefixed with the names
	 *         of the levels and tasks they belong to
	 */
	public String[] getStepNames() {
		StringVector stepNames = new StringVector();
		for (int l = 0; l < this.levels.length; l++) {
			String[] levelStepNames = this.levels[l].getStepNames();
			for (int s = 0; s < levelStepNames.length; s++)
				stepNames.addElement(this.levels[l].getName() + "." + levelStepNames[s]);
		}
		return stepNames.toStringArray();
	}
	
	/**
	 * @return a validator capable of testing if a document complies with the
	 *         criterions specified by this process. The validator is guarantied
	 *         to produce Process.ProcessValidationResults result objects
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getValidator()
	 */
	public Validator getValidator() {
		Validator[] levelValidators = new Validator[this.levels.length];
		for (int l = 0; l < this.levels.length; l++)
			levelValidators[l] = this.levels[l].getValidator();
		return new ProcessValidator(levelValidators, this.getName(), this.getLabel(), this.getDescription(), this.getErrorDescription());
	}
	
	private class ProcessValidator extends Validator {
		
		private Validator[] levels;
		
		public ProcessValidator(Validator[] levels, String name, String label, String description, String errorDescription) {
			super(name, label, description, errorDescription);
			
			this.levels = levels;
			for (int l = 0; l < this.levels.length; l++)
				this.levels[l].parent = this;
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#validate(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public ValidationResult validate(QueriableAnnotation data) {
			String firstError = null;
			ValidationResult[] levelResults = new ValidationResult[this.levels.length];
			for (int l = 0; l < this.levels.length; l++) {
				if (firstError == null) {
					ValidationResult svr = this.levels[l].validate(data);
					levelResults[l] = svr;
					firstError = svr.getErrorDescription();
				}
				else levelResults[l] = new SkippedPartResult(this.levels[l]);
			}
			return new ProcessValidationResult(this, firstError, levelResults);
		}
		
		/*
		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#getDetailDescription()
		 */
		public String[] getDetailDescription() {
			StringVector descriptionCollector = new StringVector();
			descriptionCollector.addElement(this.getDescription());
			for (int l = 0; l < this.levels.length; l++) {
				String[] levelDescription = this.levels[l].getDetailDescription();
				for (int d = 0; d < levelDescription.length; d++)
					descriptionCollector.addElement(ValidationResult.DETAIL_INDENT + levelDescription[d]);
			}
			return descriptionCollector.toStringArray();
		}
//		
//		/*
//		 * @see de.uka.ipd.idaho.gamta.util.validation.Validator#writeXml(java.io.Writer)
//		 */
//		public void writeXml(Writer writer) throws IOException {
//			BufferedWriter bw = new BufferedWriter(writer);
//			
//			bw.write("<" + PROCESS + " " + NAME_ATTRIBUTE + "=\"" + this.getName() + "\" " + LABEL_ATTRIBUTE + "=\"" + this.getLabel() + "\">");
//			bw.newLine();
//			bw.write("<" + DESCRIPTION_ATTRIBUTE + ">" + this.getDescription() + "</" + DESCRIPTION_ATTRIBUTE + ">");
//			bw.newLine();
//			bw.write("<" + ERROR_DESCRIPTION_ATTRIBUTE + ">" + this.getErrorDescription() + "</" + ERROR_DESCRIPTION_ATTRIBUTE + ">");
//			bw.newLine();
//			for (int l = 0; l < this.levels.length; l++)
//				this.levels[l].writeXml(bw);
//			bw.write("</" + PROCESS + ">");
//			bw.newLine();
//			
//			bw.flush();
//		}
	}
	
	/**
	 * The validation result for a process as a whole
	 * 
	 * @author sautter
	 */
	public static class ProcessValidationResult extends AbstractValidationResult {
		private String error;
		private ValidationResult[] levelResults;
		private int failureCount = 0;
		ProcessValidationResult(ProcessValidator validator, String error, ValidationResult[] levelResults) {
			super(validator);
			this.error = error;
			this.levelResults = levelResults;
			for (int l = 0; l < this.levelResults.length; l++)
				if (!this.levelResults[l].isPassed())
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
			for (int l = 0; l < this.levelResults.length; l++) {
				String[] levelDescription = this.levelResults[l].getDetailDescriptions();
				for (int d = 0; d < levelDescription.length; d++)
					descriptionCollector.addElement(DETAIL_INDENT + levelDescription[d]);
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
			for (int l = 0; l < this.levelResults.length; l++) {
				String[] levelError = this.levelResults[l].getDetailErrorDescriptions();
				for (int e = 0; e < levelError.length; e++)
					errorCollector.addElement(DETAIL_INDENT + levelError[e]);
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
			return this.levelResults.length;
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
			return this.levelResults;
		}
		/* 
		 * @see de.uka.ipd.idaho.gamta.util.validation.ValidationResult#writeXml(java.io.Writer)
		 */
		public void writeXml(Writer out) throws IOException {
			BufferedWriter bw = ((out instanceof BufferedWriter) ? ((BufferedWriter) out) : new BufferedWriter(out));
			bw.write("<" + PROCESS + 
					((this.error == null) ? "" : (" " + ERROR_ATTRIBUTE + "=\"" + this.error + "\"")) +
					">");
			bw.newLine();
			for (int l = 0; l < this.levelResults.length; l++)
				this.levelResults[l].writeXml(bw);
			bw.write("</" + PROCESS + ">");
			bw.newLine();
			bw.flush();
		}
	}
	
	/**
	 * Restore a ValidationResult from its XML representation
	 * @param data the tree node at the root of the tree representing the
	 *            validation result to load
	 * @return a ProcessValidationResult object containing the data from the
	 *         specified tree
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#loadValidationResult(de.uka.ipd.idaho.htmlXmlUtil.TreeNode)
	 */
	public ValidationResult loadValidationResult(TreeNode data) {
		HashMap levelsByName = new HashMap();
		for (int l = 0; l < this.levels.length; l++)
			levelsByName.put(this.levels[l].getName(), this.levels[l]);
		
		TreeNode[] levelData = data.getChildNodes(Level.LEVEL);
		ArrayList levelResults = new ArrayList();
		for (int l = 0; l < levelData.length; l++) {
			Level level = ((Level) levelsByName.get(levelData[l].getAttribute(NAME_ATTRIBUTE)));
			if (level != null)
				levelResults.add(level.loadValidationResult(levelData[l]));
		}
		
		return new ProcessValidationResult(
				((ProcessValidator) this.getValidator()),
				data.getAttribute(ValidationResult.ERROR_ATTRIBUTE),
				((ValidationResult[]) levelResults.toArray(new ValidationResult[levelResults.size()]))
				);
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#getDetailDescription()
	 */
	public String[] getDetailDescription() {
		StringVector descriptionCollector = new StringVector();
		descriptionCollector.addElement(this.getDescription());
		for (int l = 0; l < this.levels.length; l++) {
			String[] levelDescription = this.levels[l].getDetailDescription();
			for (int d = 0; d < levelDescription.length; d++)
				descriptionCollector.addElement(ValidationResult.DETAIL_INDENT + levelDescription[d]);
		}
		return descriptionCollector.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.validation.ProcessPart#writeXml(java.io.Writer)
	 */
	public void writeXml(Writer writer) throws IOException {
		BufferedWriter bw = new BufferedWriter(writer);
		
		bw.write("<" + PROCESS + " " + NAME_ATTRIBUTE + "=\"" + this.xmlEscape(this.getName()) + "\" " + LABEL_ATTRIBUTE + "=\"" + this.xmlEscape(this.getLabel()) + "\">");
		bw.newLine();
		bw.write("<" + DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getDescription()) + "</" + DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		bw.write("<" + ERROR_DESCRIPTION_ATTRIBUTE + ">" + this.xmlEscape(this.getErrorDescription()) + "</" + ERROR_DESCRIPTION_ATTRIBUTE + ">");
		bw.newLine();
		for (int l = 0; l < this.levels.length; l++)
			this.levels[l].writeXml(bw);
		bw.write("</" + PROCESS + ">");
		bw.newLine();
		
		bw.flush();
	}
	
	/**
	 * Load a markup process definition from a file.
	 * @param source the file to read from
	 * @return a markup process constructed from the data in the specified file
	 * @throws IOException
	 */
	public static Process loadMarkupProcess(File source) throws IOException {
		FileInputStream fis = new FileInputStream(source);
		Process process = loadMarkupProcess(fis);
		fis.close();
		return process;
	}
	
	/**
	 * Load a markup process definition from an InputStream.
	 * @param source the InputStream to read from
	 * @return a markup process constructed from the data provided by the
	 *         specified InputStream
	 * @throws IOException
	 */
	public static Process loadMarkupProcess(InputStream source) throws IOException {
		return loadMarkupProcess(new InputStreamReader(source));
	}
	
	/**
	 * Load a markup process definition from a Reader.
	 * @param source the Reader to read from
	 * @return a markup process constructed from the data provided by the specified Reader
	 * @throws IOException
	 */
	public static Process loadMarkupProcess(Reader source) throws IOException {
		TreeNode pRoot = parser.parse(source);
		if (TreeNode.ROOT_NODE_TYPE.equals(pRoot.getNodeType()))
			pRoot = pRoot.getChildNode(Process.PROCESS, 0);
		return new Process(pRoot);
	}
	private static Parser parser = new Parser();
}
