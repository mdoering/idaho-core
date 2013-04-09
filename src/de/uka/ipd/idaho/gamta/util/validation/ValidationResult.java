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


/**
 * The result of a validation, providing detailed information of the validation
 * process, which parts of the validation succeeded, and which failed.
 * 
 * @author sautter
 */
public interface ValidationResult {
	
	public static final String FAILED_ATTRIBUTE = "failed";
	public static final String INSTANCES_ATTRIBUTE = "instances";
	public static final String ERROR_ATTRIBUTE = "error";
	
	public static final String FAILED = "Failed";
	public static final String PASSED = "OK";
	
	public static final String INCOMPLETE = "Incomplete";
	public static final String COMPLETE = "Complete";
	
	public static final String SKIPPED = "Skipped";
	
	public static final String DETAIL_INDENT = "  ";
	
	/**
	 * @return true or false, depending on wheteher the part of the validation
	 *         this result originates from was successful or not.
	 */
	public abstract boolean isPassed();
	
	/**
	 * @return the number of annotations checked by the backing validator
	 *         for producing this result (in results for Criterions), or the
	 *         number of subordinate checks performed for creating this
	 *         result (in all other results)
	 */
	public abstract int getInstanceCount();
	
	/**
	 * @return the number of checked instances that did not pass the test
	 *         (in results for Criterions), or the number of subordinate
	 *         checks that failed (in all other results)
	 */
	public abstract int getFailureCount();
	
	/**
	 * @return the number of checked instances that passed the test (in results
	 *         for Criterions), or the number of subordinate checks that
	 *         succeeded (in all other results)
	 */
	public abstract int getPassCount();
	
	/**
	 * @return the fraction of failed instances to checked instances
	 */
	public abstract float getFailureRate();
	
	/**
	 * @return the fraction of failed instances to checked instances, rounded to
	 *         a percentage
	 */
	public abstract int getFailurePercent();
	
	/**
	 * @return one minus the fraction of failed instances to checked instances
	 */
	public abstract float getSuccessRate();
	
	/**
	 * @return 100 minus the fraction of failed instances to checked instances,
	 *         rounded to a percentage
	 */
	public abstract int getSuccessPercent();
	
	/**
	 * @return the validator that created the ValidationResult
	 */
	public abstract Validator getValidator();
	
	/**
	 * @return a textual description of the test result
	 */
	public abstract String getDescription();
	
	/**
	 * @return a textual description of the reason for that the test failed, or
	 *         null if the test was passed
	 */
	public abstract String getErrorDescription();
	
	/**
	 * @return detailed textual descriptions of the test result, including both
	 *         passed and failed tests
	 */
	public abstract String[] getDetailDescriptions();
	
	/**
	 * @return textual descriptions of the tests that failed
	 */
	public abstract String[] getDetailErrorDescriptions();
	
	/**
	 * @return the partial results of the validation result
	 */
	public abstract ValidationResult[] getPartialResults();
	
	/**
	 * Write the validation result (without the actual annotations) to a writer so it can later be restored through the 
	 * @param out the writer to write to
	 * @throws IOException
	 */
	public abstract void writeXml(Writer out) throws IOException;
}
