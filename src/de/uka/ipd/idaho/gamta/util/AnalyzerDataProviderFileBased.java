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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * File based data provider for analyzers, working relative to some base folder.
 * 
 * @author sautter
 */
public class AnalyzerDataProviderFileBased extends AbstractAnalyzerDataProvider {
	
	private File basePath;
	private int basePathLength;
	private String[] fileList = null;
	
	/**
	 * @param basePath
	 */
	public AnalyzerDataProviderFileBased(File basePath) {
		this.basePath = basePath;
		this.basePathLength = this.basePath.getAbsolutePath().length();
	}

	/* (non-Javadoc)
	 * @see de.gamta.util.AnalyzerDataProvider#isDataAvailable(java.lang.String)
	 */
	public boolean isDataAvailable(String dataName) {
		return this.getFile(dataName).exists();
	}

	/* (non-Javadoc)
	 * @see de.gamta.util.AnalyzerDataProvider#getInputStream(java.lang.String)
	 */
	public InputStream getInputStream(String dataName) throws IOException {
		return new FileInputStream(this.getFile(dataName));
	}

	/* (non-Javadoc)
	 * @see de.gamta.util.AnalyzerDataProvider#getURL(java.lang.String)
	 */
	public URL getURL(String dataName) throws IOException {
		if (dataName.indexOf("://") == -1)
			return this.getFile(dataName).toURI().toURL();
		else return new URL(dataName);
	}

	/* (non-Javadoc)
	 * @see de.gamta.util.AnalyzerDataProvider#isDataEditable(java.lang.String)
	 */
	public boolean isDataEditable(String dataName) {
		return true;
	}

	/* (non-Javadoc)
	 * @see de.gamta.util.AnalyzerDataProvider#getOutputStream(java.lang.String)
	 */
	public OutputStream getOutputStream(String dataName) throws IOException {
		File dataFile = this.getFile(dataName);
		if (dataFile.exists()) {
			dataFile.renameTo(new File(dataFile.getPath() + "." + System.currentTimeMillis() + ".old"));
			dataFile = this.getFile(dataName);
		}
		else {
			dataFile.getParentFile().mkdirs();
			dataFile.createNewFile();
		}
		return new FileOutputStream(dataFile);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AnalyzerDataProvider#deleteData(java.lang.String)
	 */
	public boolean deleteData(String name) {
		File file = this.getFile(name);
		return (!file.exists() || file.delete());
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AnalyzerDataProvider#getDataNames()
	 */
	public String[] getDataNames() {
//		if (this.fileList == null)
			this.fileList = this.readFileList(this.basePath);
		return this.fileList;
	}
	
	//	list files recursively
	private String[] readFileList(File directory) {
		StringVector resultFiles = new StringVector();
		File[] files = directory.listFiles();
		for (int f = 0; f < files.length; f++) {
			if (files[f].isDirectory() && !files[f].equals(directory)) {
				resultFiles.addContent(this.readFileList(files[f]));
			} else {
				resultFiles.addElement(files[f].getAbsolutePath().substring(this.basePathLength + 1).replaceAll("\\\\", "/"));
			}
		}
		return resultFiles.toStringArray();
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.util.AnalyzerDataProvider#isDataEditable()
	 */
	public boolean isDataEditable() {
		return true;
	}
	
	private File getFile(String dataName) {
		return new File(this.basePath, dataName);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider#getAbsolutePath()
	 */
	public String getAbsolutePath() {
		if (this.absolutePath == null) {
			String path = this.basePath.getAbsolutePath().replaceAll("\\\\", "/");
			StringBuffer cleanPath = new StringBuffer();
			for (int c = 0; c < path.length(); c++) {
				char ch = path.charAt(c);
				if (ch == '/') {
					if (path.startsWith("./", (c+1)))
						c++; // ignore current slash and jump dot
					else cleanPath.append(ch);
				}
				else if (ch == '\\') {
					if (path.startsWith("./", (c+1)) || path.startsWith(".\\", (c+1)))
						c++; // ignore current slash and jump dot
					else cleanPath.append('/');
				}
				else cleanPath.append(ch);
			}
			this.absolutePath = cleanPath.toString();
		}
		return this.absolutePath;
	}
	private String absolutePath = null;
	
	public static void main(String[] args) {
		File f = new File("./Test/");
		System.out.println(f.getAbsolutePath());
		f = f.getAbsoluteFile();
		System.out.println(f.getAbsolutePath());
		AnalyzerDataProviderFileBased adp = new AnalyzerDataProviderFileBased(f);
		System.out.println(adp.getAbsolutePath());
	}
}
