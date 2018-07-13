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
package de.uka.ipd.idaho.gamta.util.analyzerConfiguration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;

/**
 * Set holding boolean attributes for lists and regular expression patterns.
 * 
 * @author sautter
 */
public class NameAttributeSet {
	private String dataName;
	private String label;
	private AnalyzerDataProvider dataProvider;
	private Set content = new TreeSet();
	private boolean dirty = false;
	
	/** Constructor
	 * @param dataName the name of the data object to load the set from and store it in
	 * @param label the display label for the set
	 * @param dataProvider the data provider to use for loading and storing the set
	 */
	public NameAttributeSet(String dataName, String label, AnalyzerDataProvider dataProvider) {
		this.dataName = dataName;
		this.label = label;
		this.dataProvider = dataProvider;
		if (this.dataProvider.isDataAvailable(this.dataName)) try {
			BufferedReader br = new BufferedReader(new InputStreamReader(this.dataProvider.getInputStream(this.dataName), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null)
				this.content.add(line);
			br.close();
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.out);
		}
	}
	
	/**
	 * @return the display label for the set
	 */
	public String getLabel() {
		return this.label;
	}
	
	/**
	 * Look up a data name.
	 * @param name the name to look up
	 * @return true if the set contains the name, false otherwise
	 */
	public boolean contains(String name) {
		return this.content.contains(name);
	}
	
	/**
	 * Add a data name to the set.
	 * @param name the name to add
	 */
	public void add(String name) {
		this.dirty = (this.content.add(name) || this.dirty);
	}
	
	/**
	 * Remove a data name from the set.
	 * @param name the name to remove
	 */
	public void remove(String name) {
		this.dirty = (this.content.remove(name) || this.dirty);
	}

	/**
	 * @return true if the content of this set has been modified since the last
	 *         invokation of the commitChanges() method, false otherwise
	 */
	public boolean isDirty() {
		return this.dirty;
	}
	
	/**
	 * Write the changes made to the content of this set to persistant storage.
	 * @return true if any changes were committed, false otherwise
	 * @throws IOException
	 */
	public boolean commitChanges() throws IOException {
		if (!this.isDirty())
			return false;
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream(this.dataName), "UTF-8"));
		for (Iterator cit = this.content.iterator(); cit.hasNext();) {
			bw.write((String) cit.next());
			bw.newLine();
		}
		bw.flush();
		bw.close();
		this.dirty = false;
		return true;
	}
}
