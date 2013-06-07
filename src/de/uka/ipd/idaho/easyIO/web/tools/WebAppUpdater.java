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
package de.uka.ipd.idaho.easyIO.web.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Extractor tool for zipped exploded web application archives (basically,
 * zipped web applications), retaining files that were modified locally on
 * updates, and files configured to be retained in the <code>update.cnfg</code>
 * config file.
 * 
 * @author sautter
 */
public class WebAppUpdater {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//	get base folder
		File basePath = (new File(".").getAbsoluteFile());
		if (".".equals(basePath.getName()))
			basePath = basePath.getParentFile();
		
		//	check arguments
		if ((args.length == 0) || (args[0] == null) || (args[0].trim().length() == 0) || args[0].trim().toLowerCase().endsWith(".zip")) {
			if ("webapps".equalsIgnoreCase(basePath.getName())) {
				System.out.println("Please specify the name of the web application to update.");
				return;
			}
			
			//	derive web app name from folder name
			String[] nArgs = new String[args.length + 1];
			nArgs[0] = basePath.getName();
			System.arraycopy(args, 0, nArgs, 1, args.length);
			args = nArgs;
			basePath = basePath.getParentFile();
		}
		
		//	gather webapp base data
		String webAppName = args[0];
		boolean webAppExists = true;
		File webAppPath = (basePath.getName().equals(webAppName) ? basePath : new File(basePath, webAppName));
		
		//	make sure base path is parent of webapp folder
		if (webAppPath == basePath)
			basePath = basePath.getParentFile();
		
		//	we're actually installing
		if (!webAppPath.exists()) {
			webAppExists = false;
			webAppPath.mkdir();
		}
		
		//	find archive to extract (check update case first)
		String zipName = ((args.length == 1) ? (webAppName + ".zip") : args[1]);
		File zipFile = new File(webAppPath, zipName);
		if (!zipFile.exists())
			zipFile = new File(basePath, zipName);
		if (!zipFile.exists()) {
			System.out.println(zipName + " not found.");
			return;
		}
		
		//	load file name filter
		ArrayList ignoreFileNames = new ArrayList();
		if (webAppExists) try {
			File ifnFile = new File(webAppPath, "update.cnfg");
			BufferedReader ifnBr = new BufferedReader(new InputStreamReader(new FileInputStream(ifnFile)));
			for (String ifn; (ifn = ifnBr.readLine()) != null;) {
				ifn = ifn.trim();
				if ((ifn.length() == 0) || ifn.startsWith("//"))
					continue;
				
				//	simple string
				if (ifn.indexOf('*') == -1) {
					ignoreFileNames.add(ifn);
					continue;
				}
				
				//	pattern
				StringBuffer ifnRegEx = new StringBuffer();
				for (int c = 0; c < ifn.length(); c++) {
					char ch = ifn.charAt(c);
					if (Character.isLetterOrDigit(ch))
						ifnRegEx.append(ch);
					else if (ch == '*')
						ifnRegEx.append(".*");
					else {
						ifnRegEx.append('\\');
						ifnRegEx.append(ch);
					}
				}
				ignoreFileNames.add(Pattern.compile(ifnRegEx.toString(), Pattern.CASE_INSENSITIVE));
			}
		} catch (IOException ioe) {}
		
		//	unzip web app
		try {
			ZipInputStream webAppZip = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)));
			for (ZipEntry ze; (ze = webAppZip.getNextEntry()) != null;) {
				
				//	test for folders
				if (ze.isDirectory())
					continue;
				
				//	check ignore patterns
				for (int i = 0; i < ignoreFileNames.size(); i++) {
					Object io = ignoreFileNames.get(i);
					if (((io instanceof Pattern) && ((Pattern) io).matcher(ze.getName()).matches()) || ((io instanceof String) && ((String) io).equalsIgnoreCase(ze.getName()))) {
						System.out.println(" - ignoring " + ze.getName());
						ze = null;
						break;
					}
				}
				if (ze == null)
					continue;
				
				//	get timestamp and unpack file
				long zipLastModified = ze.getTime();
				updateFile(webAppPath, ze.getName(), webAppZip, zipLastModified);
				
				//	close current entry
				webAppZip.closeEntry();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
		
		//	we're done
		if (webAppExists)
			return;
		
		//	put custom scripts in newly installed webapp
		try {
			File jarFile = new File(basePath, "Updater.jar");
			InputStream jarIn = new BufferedInputStream(new FileInputStream(jarFile));
			updateFile(webAppPath, "Updater.jar", jarIn, jarFile.lastModified());
			jarIn.close();
			File cnfgFile = new File(webAppPath, "update.cnfg");
			if (!cnfgFile.exists())
				cnfgFile.createNewFile();
			File wScriptFile = new File(webAppPath, "update.bat");
			if (!wScriptFile.exists()) {
				cnfgFile.createNewFile();
				BufferedWriter wScriptBw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(wScriptFile)));
				wScriptBw.write("@echo off");
				wScriptBw.newLine();
				wScriptBw.write("java -jar Updater.jar " + webAppName + " %1 %2");
				wScriptBw.newLine();
				wScriptBw.flush();
				wScriptBw.close();
			}
			File lScriptFile = new File(webAppPath, "update");
			if (!lScriptFile.exists()) {
				cnfgFile.createNewFile();
				BufferedWriter lScriptBw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lScriptFile)));
				lScriptBw.write("#! /bin/bash");
				lScriptBw.newLine();
				lScriptBw.write("java -jar Updater.jar " + webAppName + "$1 $2");
				lScriptBw.newLine();
				lScriptBw.flush();
				lScriptBw.close();
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	private static void updateFile(File webAppPath, String fileName, InputStream zip, long zipLastModified) throws IOException {
		
		//	create target file
		File targetFile = new File(webAppPath, fileName);
		boolean targetFileExists = targetFile.exists();
		
		//	check if more recent version of file available in file system
		if (targetFile.exists() && (zipLastModified < (targetFile.lastModified() + 1000))) {
			System.out.println(" - retaining " + fileName);
			return;
		}
		
		//	make sure folders exist
		targetFile.getParentFile().mkdirs();
		
		//	create target file
		targetFile.createNewFile();
		
		//	report status
		System.out.println(" - " + (targetFileExists ? "updating" : "installing") + " " + fileName);
		
		//	copy file
		OutputStream target = new BufferedOutputStream(new FileOutputStream(targetFile));
		int count;
		byte[] data = new byte[1024];
		while ((count = zip.read(data, 0, 1024)) != -1)
			target.write(data, 0, count);
		
		//	close streams
		target.flush();
		target.close();
		
		//	set timestamp of copied file
		try {
			targetFile.setLastModified(zipLastModified);
		}
		catch (RuntimeException re) {
			re.printStackTrace(System.out);
		}
	}
}