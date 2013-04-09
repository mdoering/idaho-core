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


import java.util.Properties;

import de.uka.ipd.idaho.gamta.MutableAnnotation;

/**
 * Interface for generic components working on GAMTA documents or parts of them
 * 
 * @author sautter
 */
public interface Analyzer {
	
	/** Indicates if user interaction is allowed while processing a document */
	public static final String INTERACTIVE_PARAMETER = "INTERACTIVE";
	
	/** Indicates if network and web access is allowed while processing a document */
	public static final String ONLINE_PARAMETER = "ONLINE";
	
	/**
	 * Process a MutableAnnotation
	 * @param data the MutableAnnotation to be processed
	 * @param parameters parameters for the processing
	 */
	public abstract void process(MutableAnnotation data, Properties parameters);
	
	/**
	 * Make the Analyzer know the data provider giving it access to its data (if
	 * it needs any). Note: if the Analyzer needs initialization, it should be
	 * done when the data provider is set
	 * @param dataProvider the data provider holding the data of the Analyzer
	 * 
	 */
	public abstract void setDataProvider(AnalyzerDataProvider dataProvider);
	
	/**
	 * Configure the Analyzer (this method is provided so the Analyzer specific
	 * configuration is accessible through the GUI in a unified way)
	 */
	public abstract void configureProcessor();
	
	/**
	 * Exit the Analyzer (shut it down, store collected data, clean up memory,
	 * etc ...)
	 */
	public abstract void exit();
}
