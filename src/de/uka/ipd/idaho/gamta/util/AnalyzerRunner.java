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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableCharSequence;

/**
 * Wrapper for building command line tools from Analyzers. All that sub classes
 * have to do is implement the main method, which has to instantiate the
 * Analyzer and then hand the args[] and the Analyzer to the process() method
 */
public class AnalyzerRunner {
	
	protected static final String HELP_PARAMETER = "?";
	protected static final String OUTPUT_FOLDER_PARAMETER = "OUT";
	protected static final String DATA_PATH_PARAMETER = "DATA";
	
	/**	process the args[] of the main method using an Analyzer
	 * @param	args		the arguments to process
	 * @param	analyzer	the Analyzer to use
	 */
	protected static void process(String[] args, Analyzer analyzer) {
		
		if ((args.length == 0) || HELP_PARAMETER.equals(args[0])) {
			System.out.println("Batch mode runner for GAMTA Analyzers");
			System.out.println("Parameters:");
			System.out.println("- ? --> Displays these explanations");
			System.out.println("- OUT=<OutputFolder> --> specify the folder to store documents to, if missing, documents will be saved to " + (new File("./finished/").toString()));
			System.out.println("- DATA=<AnalyzerDataPath> --> specify the folder where the Analyzer can find its data, if missing it uses the local path (./)");
			System.out.println("- <All other parameters> --> specify the files to process, folders will be searched for files");
			return;
		}
		
		String outputFolderParameter = null;
		String dataPathParameter = "./";
		ArrayList filesToProcess = new ArrayList();
		
		for (int a = 0; a < args.length; a++) {
			String parameter = args[a];
			if (parameter.startsWith(OUTPUT_FOLDER_PARAMETER + "=")) outputFolderParameter = (((parameter.indexOf('=') + 1) == parameter.length()) ? "." : parameter.substring(parameter.indexOf('=') + 1).trim());
			else if (parameter.startsWith(DATA_PATH_PARAMETER + "=")) dataPathParameter = (((parameter.indexOf('=') + 1) == parameter.length()) ? "." : parameter.substring(parameter.indexOf('=') + 1).trim());
			else filesToProcess.add(new File(parameter));
		}
		
		if (analyzer == null) {
			System.out.println("No Analyzer to run, exiting.");
			return;
		}
		
		if (!dataPathParameter.endsWith("/")) dataPathParameter = (dataPathParameter + "/");
		
//		analyzer.setAnalyzerName("Analyzer");
		analyzer.setDataProvider(new AnalyzerDataProviderFileBased(new File((dataPathParameter == null) ? "./" : dataPathParameter)));
		
		File outputFolder = new File((outputFolderParameter == null) ? ("./finished/") : (outputFolderParameter + (outputFolderParameter.endsWith("/") ? "" : "/")));
		if (!outputFolder.exists()) outputFolder.mkdirs();
		
		Properties analyzerParameters = new Properties();
		analyzerParameters.setProperty(Analyzer.INTERACTIVE_PARAMETER, Analyzer.INTERACTIVE_PARAMETER);
		
		while (!filesToProcess.isEmpty()) {
			File file = ((File) filesToProcess.remove(0));
			if (file.isDirectory()) {
				File[] subFiles = file.listFiles();
				for (int f = 0; f < subFiles.length; f++)
					if (!file.equals(subFiles[f])) filesToProcess.add(subFiles[f]);
			}
			else {
				try {
					System.out.println("processing " + file.toString() + " ...");
					String fileName = file.getName();
					MutableAnnotation doc;
					if (fileName.endsWith(".txt")) {
						FileReader fr = new FileReader(file);
						MutableCharSequence mcs = Gamta.newCharSequence();
						char c;
						while ((c = ((char) fr.read())) != -1)
							mcs.addChar(c);
						doc = Gamta.newDocument(Gamta.newTokenSequence(mcs, Gamta.INNER_PUNCTUATION_TOKENIZER));
						fr.close();
					} else doc = SgmlDocumentReader.readDocument(file);
					analyzer.process(doc, analyzerParameters);
					File saveFile = new File(outputFolder, fileName);
					AnnotationUtils.writeXML(doc, new FileWriter(saveFile));
					System.out.println("File " + file.toString() + " done, saved as " + saveFile.toString() + ".");
				}
				catch (FileNotFoundException fnfe) {
					System.out.println(file.toString() + " not found.");
				}
				catch (IOException ioe) {
					System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while processing " + file.toString() + " ...");
				}
				catch (Exception e) {
					System.out.println(e.getClass().getName() + " (" + e.getMessage() + ") while processing " + file.toString() + " ...");
				}
			}
		}
		
		analyzer.exit();
	}
}
