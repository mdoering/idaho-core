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

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.JTabbedPane;

/**
 * Widget for bundling other configuration panels. This panel is dirty if any of
 * the contained panels is dirty. Committing the changes in this panel commits
 * the changes in all contained panels. The data provider of this panel is null
 * because all editing is happening in the contained panels.
 * 
 * @author sautter
 */
public class TabbedConfigPanel extends AnalyzerConfigPanel {
	
	private AnalyzerConfigPanel[] panels;
	
	/** Constructior
	 * @param panels the panels to bundle
	 * @param title the title of this panel to display in a tabbed pane
	 * @param toolTip the tooltip of this panel to display in a tabbed pane
	 */
	public TabbedConfigPanel(AnalyzerConfigPanel[] panels, String title, String toolTip) {
		super(null, title, toolTip);
		this.panels = panels;
		if (this.panels.length == 1)
			this.add(this.panels[0], BorderLayout.CENTER);
		else {
			JTabbedPane tabs = new JTabbedPane();
			for (int p = 0; p < this.panels.length; p++)
				tabs.addTab(this.panels[p].getTitle(), null, this.panels[p], this.panels[p].getToolTip());
			this.add(tabs, BorderLayout.CENTER);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel#isDirty()
	 */
	public boolean isDirty() {
		boolean dirty = false;
		for (int p = 0; p < this.panels.length; p++)
			dirty = (dirty || this.panels[p].isDirty());
		return dirty;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel#commitChanges()
	 */
	public boolean commitChanges() throws IOException {
		boolean committed = true;
		for (int p = 0; p < this.panels.length; p++)
			committed = (committed | this.panels[p].commitChanges());
		return committed;
	}
}
