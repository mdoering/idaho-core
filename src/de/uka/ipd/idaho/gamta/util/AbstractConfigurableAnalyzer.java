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


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerParameter;


/**
 * More heavyweight default implementation of the Analyzer interface. Provides
 * native support for storing lists and parameters. Sub classes using this super
 * class should overwrite initAnalyzer() insetad of setDataPath(), or at least
 * inlude the respective super call as the first line of their overwriting code.
 * They also should overwrite exitAnalyzer() instead of exit(), or at least
 * inlude the respective super call as the last line of their overwriting code.
 * Both is in favor of the built-in parameter handling.
 */
public abstract class AbstractConfigurableAnalyzer implements Analyzer, AnalyzerParameter.Provider {
	
	//	constants and data structures for parameter handling
	private static final String PARAMETER_VALUE_SEPARATOR = "|";
	private static final String PARAMETER_ENCODING = "UTF-8";
	
	private Properties parameters;
	private StringVector parameterNames;
	
	private boolean parametersModified = false;
	
	//	load the parameters
	private void loadParameters() {
		this.parameterNames = new StringVector();
		this.parameters = new Properties();
		
		//	load lines
		StringVector rawLines;
		try {
			rawLines = this.loadList(this.getClass().getName() + ".cnfg");
		}
		catch (IOException ioe) {
			return;
		}
		
		//	parse lines
		for (int l = 0; l < rawLines.size(); l++) {
			String rawLine = rawLines.get(l);
			String name;
			String value;
			int splitIndex = rawLine.indexOf(PARAMETER_VALUE_SEPARATOR);
			
			//	split line and decode value
			try {
				name = rawLine.substring(0, splitIndex);
				value = URLDecoder.decode(rawLine.substring(splitIndex + 1), PARAMETER_ENCODING);
			}
			catch (Exception e) {
				continue;
			}
				
			//	store parameter if valid
			this.parameterNames.addElementIgnoreDuplicates(name);
			this.parameters.setProperty(name, value);
		}
	}
	
	//	store the parameters
	private void storeParameters() {
		if (!this.parametersModified)
			return;
		
		StringVector lines = new StringVector();
		this.parameterNames.sortLexicographically(false, false);
		
		//	produce lines
		for (int p = 0; p < this.parameterNames.size(); p++) {
			String name = this.parameterNames.get(p);
			String value = this.parameters.getProperty(name, "");
			
			//	build line, encode value
			try {
				lines.addElement(name + PARAMETER_VALUE_SEPARATOR + URLEncoder.encode(value, PARAMETER_ENCODING));
			} catch (Exception e) {}
		}
		
		//	store lines
		try {
			this.storeList(lines, (this.getClass().getName() + ".cnfg"));
			this.parametersModified = false;
		} catch (IOException e) {}
	}
	
	/** the Analyzer's data provider */
	protected AnalyzerDataProvider dataProvider;
	
	/** Constructor
	 */
	public AbstractConfigurableAnalyzer() {}
	
	/**
	 * Load a list of data.
	 * @param listName the name of the list to load
	 * @return a StringVector containing the data loaded
	 * @throws IOException if the dataPath is not set, or if any IOException
	 *             occurrs while loading the list
	 */
	protected StringVector loadList(String listName) throws IOException {
		if (this.dataProvider == null)
			throw new IOException("Data provider missing.");
		InputStream is = this.dataProvider.getInputStream(listName);
		StringVector list = StringVector.loadList(is);
		is.close();
		return list;
	}
	
	/**
	 * Store a list of data (if any data was previously stored under the
	 * specified file name, this data will be overwritten).
	 * @param list the list to store
	 * @param listName the name to store the list by
	 * @throws IOException if the dataPath is not set, or if any IOException
	 *             occurrs while storing the list
	 */
	protected void storeList(StringVector list, String listName) throws IOException {
		OutputStream os = this.dataProvider.getOutputStream(listName);
		list.storeContent(os);
		os.flush();
		os.close();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerParameter.Store#persistParameters()
	 */
	public void persistParameters() {
		this.storeParameters();
	}
	
	/**
	 * Load a parameter (will be available after the data path is set).
	 * @param name the name of the parameter
	 * @return the value of the parameter, or null, if the parameter with the
	 *         specified name is not set
	 */
	public String getParameter(String name) {
		return this.getParameter(name, null);
	}
	
	/**
	 * Load a parameter (will be available after the data path is set).
	 * @param name the name of the parameter
	 * @param def the default value to be returned if the parameter is not set
	 * @return the value of the parameter, or def, if the parameter with the
	 *         specified name is not set
	 */
	public String getParameter(String name, String def) {
		if (this.parameters == null)
			return def;
		return this.parameters.getProperty(name, def);
	}
	
	/**
	 * Store a parameter.
	 * @param name the name of the parameter
	 * @param value the value of the parameter
	 * @return the value this parameter was previously set to, or null, if there
	 *         is no such value
	 */
	public String storeParameter(String name, String value) {
		if (value == null)
			return this.removeParameter(name);
		if (this.parameters == null)
			return value;
		String oldValue = this.parameters.getProperty(name);
		if ((oldValue != null) && oldValue.equals(value))
			return value;
		this.parameterNames.addElementIgnoreDuplicates(name);
		this.parameters.setProperty(name, value);
		this.parametersModified = true;
		return oldValue;
	}
	
//	/**
//	 * Load a parameter (will be available after the data path is set).
//	 * @param name the name of the parameter
//	 * @return the value of the parameter, or null, if the parameter with the
//	 *         specified name is not set
//	 */
//	protected String getParameter(String name) {
//		return this.getParameter(name, null);
//	}
//	
//	/**
//	 * Load a parameter (will be available after the data path is set).
//	 * @param name the name of the parameter
//	 * @param def the default value to be returned if the parameter is not set
//	 * @return the value of the parameter, or def, if the parameter with the
//	 *         specified name is not set
//	 */
//	protected String getParameter(String name, String def) {
//		if (this.parameters == null)
//			return def;
//		return this.parameters.getProperty(name, def);
//	}
//	
//	/**
//	 * Store a parameter.
//	 * @param name the name of the parameter
//	 * @param value the value of the parameter
//	 * @return the value this parameter was previously set to, or null, if there
//	 *         is no such value
//	 */
//	protected String storeParameter(String name, String value) {
//		if (value == null)
//			return this.removeParameter(name);
//		if (this.parameters == null)
//			return value;
//		String oldValue = this.parameters.getProperty(name);
//		if ((oldValue != null) && oldValue.equals(value))
//			return value;
//		this.parameterNames.addElementIgnoreDuplicates(name);
//		this.parameters.setProperty(name, value);
//		this.parametersModified = true;
//		return oldValue;
//	}
//	
	/**
	 * Remove a parameter.
	 * @param name the name of the parameter
	 * @return the value this parameter was set to, or null, if there is no such
	 *         value
	 */
	protected String removeParameter(String name) {
		if (this.parameters == null)
			return null;
		if (!this.parameters.containsKey(name))
			return null;
		this.parameterNames.removeAll(name);
		this.parametersModified = true;
		return ((String) this.parameters.remove(name));
	}
	
	/**
	 * Check if a parameter is set.
	 * @param name the name of the parameter
	 * @return true if and only if this Analyzer has a parameter with the
	 *         specified name
	 */
	protected boolean hasParameter(String name) {
		if (this.parameterNames == null)
			return false;
		return this.parameterNames.contains(name);
	}
	
	/**
	 * @return the names of all parameters available
	 */
	protected String[] getParameterNames() {
		if (this.parameterNames == null) return new String[0];
		this.parameterNames.sortLexicographically(false, false);
		return this.parameterNames.toStringArray();
	}
	
	/**
	 * Clear this Analyzer's parameters.
	 */
	protected void clearParameters() {
		if ((this.parameters == null) || this.parameters.isEmpty())
			return;
		this.parameterNames.clear();
		this.parameters.clear();
		this.parametersModified = true;
	}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#setDataProvider(de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider)
	 */
	public void setDataProvider(AnalyzerDataProvider dataProvider) {
		this.dataProvider = dataProvider;
		this.loadParameters();
		this.initAnalyzer();
	}

	/**
	 * Initialize the Analyzer. This method is called after the data provider is
	 * set, it does nothing by default. Sub classes are welcome to overwrite it
	 * as needed, instead of overwriting setDataProvider() directly.
	 */
	public void initAnalyzer() {}

//	/**
//	 * This default implementation does nothing. Implementing subclasses are
//	 * welcome to override it as needed.
//	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#configureProcessor()
//	 */
//	public void configureProcessor() {}
//	
	/**
	 * This default implementation retrieves configuration panels from the
	 * getConfigPanels() method. If the latter returns a non-empty array, this
	 * implementation opens a dialog containing the panels.
	 * @see de.uka.ipd.idaho.gamta.util.Analyzer#configureProcessor()
	 */
	public void configureProcessor() {
		
		//	build dialog
		final JDialog dialog = DialogFactory.produceDialog(("Configure " + this.getConfigTitle()), true);
		dialog.getContentPane().setLayout(new BorderLayout());
		
		//	get config panels
		final AnalyzerConfigPanel[] acps = this.getConfigPanels(dialog);
		if ((acps == null) || (acps.length == 0))
			return;
		
		//	initialize content
		JComponent dialogContent;
		if (acps.length == 1)
			dialogContent = acps[0];
		else {
			dialogContent = new JTabbedPane();
			for (int p = 0; p < acps.length; p++)
				((JTabbedPane) dialogContent).addTab(acps[p].getTitle(), null, acps[p], acps[p].getToolTip());
		}
		dialog.getContentPane().add(dialogContent, BorderLayout.CENTER);
		
		//	add buttons
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton commitButton = new JButton("OK");
		commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
		commitButton.setPreferredSize(new Dimension(100, 21));
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				boolean close = true;
				for (int p = 0; p < acps.length; p++) {
					if (acps[p].isDirty()) try {
						acps[p].commitChanges();
					}
					catch (Exception e) {
						System.out.println("Error committing changes in '" + acps[p].getTitle() + "': " + e.getMessage());
						e.printStackTrace(System.out);
						int choice = JOptionPane.showConfirmDialog(dialog, ("The changes in " + acps[p].getTitle() + " could not be committed:\n" + e.getMessage() + "\nClick Yes to close the configuration dialog anyways, No to keep it open."), "Error Storing Changes", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
						if (choice != JOptionPane.YES_OPTION)
							close = false;
					}
				}
				if (close)
					dialog.dispose();
			}
		});
		buttonPanel.add(commitButton);
		JButton cancelButton = new JButton("Cancel");
		cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
		cancelButton.setPreferredSize(new Dimension(100, 21));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				dialog.dispose();
			}
		});
		buttonPanel.add(cancelButton);
		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		//	show dialog
		dialog.setSize(800, 600);
		dialog.setLocationRelativeTo(dialog.getOwner());
		this.customizeConfigDialog(dialog);
		dialog.setVisible(true);
	}
	
	/**
	 * Retrieve a set of configuration panels for the Analyzer. If this method
	 * returns null, the configureProcessor() method returns immediately;
	 * otherwise, it opens a dialog containing the panels. This default
	 * implementation does return null. Sub classes are welcome to overwrite it
	 * as needed. The argument dialog is handed over not for customization (this
	 * is what the customizeConfigDialog() is for), but in case sub classes have
	 * config panels that need to know it, e.g. as the parent of subordinate
	 * dialogs.
	 * @param dialog the configuration dialog
	 * @return an array holding the configuration panels for the Analyzer
	 */
	protected AnalyzerConfigPanel[] getConfigPanels(JDialog dialog) {
		return null;
	}
	
	/**
	 * Retrieve a title string for the configuration dialog of the Analyzer.
	 * This default implementation returns the unqualified class name of the
	 * analyzer, with a space inserted before all inner upper case letters. Sub
	 * classes are welcome to overwrite it as needed.
	 * @return a title string for the configuration dialog of the Analyzer
	 */
	protected String getConfigTitle() {
		String className = this.getClass().getName();
		className = className.substring(className.lastIndexOf('.') + 1);
		StringBuffer configTitle = new StringBuffer();
		for (int c = 0; c < className.length(); c++) {
			char ch = className.charAt(c);
			if ((c > 0) && Character.isUpperCase(ch))
				configTitle.append(' ');
			configTitle.append(ch);
		}
		return configTitle.toString();
	}
	
	/**
	 * Customize a configuration dialog before it is made visible, e.g. to
	 * modify its size or location. This default implementation does nothing;
	 * sub classes are welcome to overwrite it as needed.
	 * @param dialog the configuration dialog
	 */
	protected void customizeConfigDialog(JDialog dialog) {}
	
	/*
	 * @see de.uka.ipd.idaho.gamta.util.AbstractAnalyzer#exit()
	 */
	public void exit() {
		this.exitAnalyzer();
		this.storeParameters();
	}
	
	/**
	 * Shut down the Analyzer. This method is called by exit(), it does nothing
	 * by default. Sub classes are welcome to overwrite it as needed, instead of
	 * overwriting exit() directly.
	 */
	public void exitAnalyzer() {}
}
