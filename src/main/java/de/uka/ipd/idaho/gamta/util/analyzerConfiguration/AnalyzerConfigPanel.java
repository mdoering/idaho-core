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
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;

/**
 * Widget for editing parts of the configuration of an Analyzer, holding the
 * infrastructure required by the concrete sub classes of this class.
 * 
 * @author sautter
 */
public abstract class AnalyzerConfigPanel extends JPanel {
	
	/**
	 * Specialized data provider for analyzer configuration panels, allowing
	 * more fine grained control over creation, editing, and, foremost, deletion
	 * of configuration files. This allows analyzers with built-in parameters to
	 * prevent them from being deleted.
	 * 
	 * @author sautter
	 */
	public static interface ConfigPanelDataProvider extends AnalyzerDataProvider {
		
		/**
		 * Test if a data object is deletable.
		 * @param dataName the name of the data object to test
		 * @return true if the data object with the specified name can be
		 *         deleted
		 */
		public abstract boolean isDataDeletable(String dataName);
	}
	
	/**
	 * Default implementation of configuration panel data provider, behaving
	 * like the analyzer data provider it wraps. Analyzers using the
	 * configuration panel API can simply extend this class to overwrite the
	 * methods they need to modify.
	 * 
	 * @author sautter
	 */
	public static class DefaultConfigPanelDataProvider implements ConfigPanelDataProvider {
		private AnalyzerDataProvider dataProvider;
		
		/**
		 * Constructor
		 * @param dataProvider the analyzer data provider to wrap
		 */
		public DefaultConfigPanelDataProvider(AnalyzerDataProvider dataProvider) {
			this.dataProvider = dataProvider;
		}
		public boolean isDataAvailable(String dataName) {
			return this.dataProvider.isDataAvailable(dataName);
		}
		public InputStream getInputStream(String dataName) throws IOException {
			return this.dataProvider.getInputStream(dataName);
		}
		public URL getURL(String dataName) throws IOException {
			return this.dataProvider.getURL(dataName);
		}
		public boolean isDataEditable() {
			return this.dataProvider.isDataEditable();
		}
		public boolean isDataEditable(String dataName) {
			return this.dataProvider.isDataEditable(dataName);
		}
		public OutputStream getOutputStream(String dataName) throws IOException {
			return this.dataProvider.getOutputStream(dataName);
		}
		public boolean deleteData(String dataName) {
			return (this.isDataDeletable(dataName) && this.dataProvider.deleteData(dataName));
		}
		public String[] getDataNames() {
			return this.dataProvider.getDataNames();
		}
		public String getAbsolutePath() {
			return this.dataProvider.getAbsolutePath();
		}
		public boolean equals(AnalyzerDataProvider adp) {
			return this.dataProvider.equals(adp);
		}
		public boolean isDataDeletable(String dataName) {
			return this.isDataEditable(dataName);
		}
	}
	
	private String fontName = "Verdana";
	private int fontSize = 12;
	private Color fontColor = Color.BLACK;
	
	/**
	 * @return the current font name
	 */
	public String getFontName() {
		return this.fontName;
	}
	
	/**
	 * Modify the font to use.
	 * @param fontName the name of the font to use from now on
	 */
	public void setFontName(String fontName) {
		this.setFont(fontName, -1, null);
	}
	
	/**
	 * @return the current font size
	 */
	public int getFontSize() {
		return this.fontSize;
	}
	
	/**
	 * @param fontSize the font size to use from now on
	 */
	public void setFontSize(int fontSize) {
		this.setFont(null, fontSize, null);
	}
	
	/**
	 * @return the current font color
	 */
	public Color getFontColor() {
		return this.fontColor;
	}
	
	/**
	 * @param fontColor the font color to use from now on
	 */
	public void setFontColor(Color fontColor) {
		this.setFont(null, -1, fontColor);
	}

	/**
	 * Modify the font used in the editing parts of the configuration panel.
	 * Specifying a non-positive font size or a null font name or color results
	 * in the respective property not being changed. Sub classes wanting to
	 * receive notification of font updates have to overwrite the updateFont()
	 * method.
	 * @param fontName the name of the font to use
	 * @param fontSize the font size to use
	 * @param fontColor the font color to use
	 */
	public void setFont(String fontName, int fontSize, Color fontColor) {
		boolean update = false;
		if (fontName == null)
			fontName = this.fontName;
		else {
			update = (update || !this.fontName.equals(fontName));
			this.fontName = fontName;
		}
		if (fontSize < 1)
			fontSize = this.fontSize;
		else {
			update = (update || (fontSize != this.fontSize));
			this.fontSize = fontSize;
		}
		if (fontColor == null)
			fontColor = this.fontColor;
		else {
			update = (update || !this.fontColor.equals(fontColor));
			this.fontColor = fontColor;
		}
		if (update) this.updateFont(new Font(this.fontName, Font.PLAIN, this.fontSize));
	}

	/**
	 * Receive a new font. This method exists in order to avoid problems with
	 * the JComponent.setFont() method that is invoked the constructors of super
	 * classes, i.e. before class specific construction is done. This default
	 * implementation does nothing, sub classes are welcome to overwrite it as
	 * needed.
	 * @param font
	 */
	protected void updateFont(Font font) {}
	
	/**
	 * the data provider to read and write data from and to
	 */
	protected ConfigPanelDataProvider dataProvider;
	
	private String title;
	private String toolTip;
	
	/** Constructor
	 * @param dataProvider the data provider to read and write data from and to
	 * @param title the title of this panel to display in a tabbed pane
	 * @param toolTip the tooltip of this panel to display in a tabbed pane
	 */
	public AnalyzerConfigPanel(AnalyzerDataProvider dataProvider, String title, String toolTip) {
		this(new DefaultConfigPanelDataProvider(dataProvider), title, toolTip);
	}
	
	/** Constructor
	 * @param dataProvider the data provider to read and write data from and to
	 * @param title the title of this panel to display in a tabbed pane
	 * @param toolTip the tooltip of this panel to display in a tabbed pane
	 */
	public AnalyzerConfigPanel(ConfigPanelDataProvider dataProvider, String title, String toolTip) {
		super(new BorderLayout(), true);
		this.dataProvider = dataProvider;
		this.title = title;
		this.toolTip = toolTip;
	}
	
	/**
	 * @return the title of this panel to display in a tabbed pane
	 */
	public String getTitle() {
		return this.title;
	}
	
	/**
	 * @return the tooltip of this panel to display in a tabbed pane
	 */
	public String getToolTip() {
		return this.toolTip;
	}
	
	/**
	 * @return true if the content of this panel has been modified since the
	 *         last invokation of the commitChanges() method, false otherwise
	 */
	public abstract boolean isDirty();
	
	/**
	 * Write the changes made to the content of this panel to persistant
	 * storage.
	 * @return true if any changes were committed, false otherwise
	 * @throws IOException
	 */
	public abstract boolean commitChanges() throws IOException;
}
