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
package de.uka.ipd.idaho.easyIO.settings;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * Simple, non-hierarchical editor panel for settings.
 * 
 * @author sautter
 */
public class SettingsEditorPanel extends JPanel {

	/**
	 * Listener for observing changes to Settings objects happening inside a
	 * SettingsEditorPanel
	 * 
	 * @author sautter
	 */
	public static interface SettingsListener {
		/**
		 * Receive notification that a setting was modified.
		 * @param key the key of the setting
		 * @param oldValue the old value (null if a setting is newly added)
		 * @param newValue the new value (null if a setting is removed)
		 */
		public abstract void settingChanged(String key, String oldValue, String newValue);
	}
	
	private ArrayList sls = new ArrayList();
	
	private void notifySettingChanged(String key, String oldValue, String newValue) {
		for (int l = 0; l < this.sls.size(); l++)
			((SettingsListener) this.sls.get(l)).settingChanged(key, oldValue, newValue);
	}
	
	/**
	 * Add a settings listener to observe updates.
	 * @param sl the listener to add
	 */
	public void addSettingsListener(SettingsListener sl) {
		if (sl != null)
			this.sls.add(sl);
	}
	
	/**
	 * Remove a settings listener to quit observing updates.
	 * @param sl the listener to remove
	 */
	public void removeSettingsListener(SettingsListener sl) {
		if (sl != null)
			this.sls.remove(sl);
	}
	
	private static final String DEFAULT_KEY_REGEX = "[^\\s\\.\\=\\\"\\']++";
	private static final String DEFAULT_KEY_REGEX_EXPLANATION = "do not use dots, whitespaces, quotes, or equal signs";
	private static final String DEFAULT_SETTING_LABEL = "Setting";
	
	private Settings settings = new Settings();
	
	private String keyRegEx;
	private String keyRegExExplanation;
	
	private String settingLabel;
	private String newSettingLabel;
	
	private JTable settingsTable;
	private String[] keys = new String[0];


	/**
	 * Constructor
	 */
	public SettingsEditorPanel() {
		this(null, null, null, null);
	}
	
	/**
	 * Constructor
	 * @param setLabel the label for settings, e.g. 'Setting' or 'Parameter'
	 *            (defaults to 'Setting' is set to null)
	 */
	public SettingsEditorPanel(String setLabel) {
		this(null, null, null, setLabel);
	}
	
	/**
	 * Constructor
	 * @param keyRegEx a regular expression pattern to restrict permitted keys
	 *            (specifying null means no restrictions, except that keys must
	 *            not contain dots, whitespace characters, quotes, or equal
	 *            signs)
	 * @param keyRegExExplanation a string explaining which keys are permitted
	 *            (defaults to 'do not use dots, whitespaces, quotes, or equal
	 *            signs' if set to null)
	 */
	public SettingsEditorPanel(String keyRegEx, String keyRegExExplanation) {
		this(null, keyRegEx, keyRegExExplanation, null);
	}
	
	/**
	 * Constructor
	 * @param keyRegEx a regular expression pattern to restrict permitted keys
	 *            (specifying null means no restrictions, except that keys must
	 *            not contain dots, whitespace characters, quotes, or equal
	 *            signs)
	 * @param keyRegExExplanation a string explaining which keys are permitted
	 *            (defaults to 'do not use dots, whitespaces, quotes, or equal
	 *            signs' if set to null)
	 * @param setLabel the label for settings, e.g. 'Setting' or 'Parameter'
	 *            (defaults to 'Setting' is set to null)
	 */
	public SettingsEditorPanel(String keyRegEx, String keyRegExExplanation, String setLabel) {
		this(null, keyRegEx, keyRegExExplanation, setLabel);
	}
	
	/**
	 * Constructor
	 * @param set the Settings objects whose content to edit (specifying null
	 *            creates a new Settings object retrievable via the
	 *            getSettings() method)
	 */
	public SettingsEditorPanel(Settings set) {
		this(set, null, null, null);
	}
	
	/**
	 * Constructor
	 * @param set the Settings objects whose content to edit (specifying null
	 *            creates a new Settings object retrievable via the
	 *            getSettings() method)
	 * @param setLabel the label for settings, e.g. 'Setting' or 'Parameter'
	 *            (defaults to 'Setting' is set to null)
	 */
	public SettingsEditorPanel(Settings set, String setLabel) {
		this(set, null, null, setLabel);
	}
	
	/**
	 * Constructor
	 * @param set the Settings objects whose content to edit (specifying null
	 *            creates a new Settings object retrievable via the
	 *            getSettings() method)
	 * @param keyRegEx a regular expression pattern to restrict permitted keys
	 *            (specifying null means no restrictions, except that keys must
	 *            not contain dots, whitespace characters, quotes, or equal
	 *            signs)
	 * @param keyRegExExplanation a string explaining which keys are permitted
	 *            (defaults to 'do not use dots, whitespaces, quotes, or equal
	 *            signs' if set to null)
	 */
	public SettingsEditorPanel(Settings set, String keyRegEx, String keyRegExExplanation) {
		this(set, keyRegEx, keyRegExExplanation, null);
	}
	
	/**
	 * Constructor
	 * @param set the Settings objects whose content to edit (specifying null
	 *            creates a new Settings object retrievable via the
	 *            getSettings() method)
	 * @param keyRegEx a regular expression pattern to restrict permitted keys
	 *            (specifying null means no restrictions, except that keys must
	 *            not contain dots, whitespace characters, quotes, or equal
	 *            signs)
	 * @param keyRegExExplanation a string explaining which keys are permitted
	 *            (defaults to 'do not use dots, whitespaces, quotes, or equal
	 *            signs' if set to null)
	 * @param setLabel the label for settings, e.g. 'Setting' or 'Parameter'
	 *            (defaults to 'Setting' is set to null)
	 */
	public SettingsEditorPanel(Settings set, String keyRegEx, String keyRegExExplanation, String setLabel) {
		super(new BorderLayout(), true);
		if (set != null)
			this.settings.setSettings(set);
		this.updateKeys();
		this.keyRegEx = ((keyRegEx == null) ? DEFAULT_KEY_REGEX : keyRegEx);
		this.keyRegExExplanation = ((keyRegExExplanation == null) ? DEFAULT_KEY_REGEX_EXPLANATION : keyRegExExplanation);
		if (!this.keyRegExExplanation.matches("(\\s|.)*(\\.|\\?|\\!)"))
			this.keyRegExExplanation = (this.keyRegExExplanation + ".");
		this.settingLabel = ((setLabel == null) ? DEFAULT_SETTING_LABEL : setLabel);
		this.newSettingLabel = ("<New " + settingLabel + ">");
		
		this.settingsTable = new JTable(new TableModel() {
			private ArrayList tmls = new ArrayList();
			
			public int getRowCount() {
				return keys.length + 1;
			}
			public int getColumnCount() {
				return 2;
			}
			
			public String getColumnName(int columnIndex) {
				return ((columnIndex == 0) ? settingLabel : "Value");
			}
			public Class getColumnClass(int columnIndex) {
				return String.class;
			}
			
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (rowIndex == keys.length) {
					return ((columnIndex == 0) ? newSettingLabel : "");
				} else return ((columnIndex == 0) ? keys[rowIndex] : settings.getSetting(keys[rowIndex]));
			}
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return true;
			}
			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
				final String newString = ((columnIndex == 0) ? ((String) newValue).trim() : ((String) newValue));
				if (rowIndex == keys.length) {
					if (columnIndex != 0)
						return;
					
					if ("".equals(newString) || newSettingLabel.equals(newString))
						return;
					if ((settings.getSetting(newString) != null)) {
						JOptionPane.showMessageDialog(settingsTable, ("The " + settingLabel.toLowerCase() + " '" + newString + "' already exists."), (settingLabel + " Already Exists"), JOptionPane.ERROR_MESSAGE);
						return;
					}
					
					if (!validateParameterName(newString))
						return;
					
					settings.setSetting(newString, "");
					notifySettingChanged(newString, null, "");
					this.keysUpdated();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							for (int r = 0; r < keys.length; r++) {
								if (keys[r].equals(newString))
									settingsTable.editCellAt(r, 1);
							}
						}
					});
				}
				else if (columnIndex == 0) {
					if (keys[rowIndex].equals(newString))
						return;
					if (!"".equals(newString) && !validateParameterName(newString))
						return;
					String setValue = settings.getSetting(keys[rowIndex]);
					String otherValue = settings.getSetting(newString);
					if ((otherValue == null) && !"".equals(newString)) {
						settings.setSetting(newString, setValue);
						notifySettingChanged(newString, null, setValue);
					}
					settings.removeSetting(keys[rowIndex]);
					notifySettingChanged(keys[rowIndex], setValue, null);
					this.keysUpdated();
				}
				else {
					String oldString = settings.setSetting(keys[rowIndex], newString);
					notifySettingChanged(keys[rowIndex], oldString, newString);
				}
			}
			
			private void keysUpdated() {
				updateKeys();
				TableModelEvent tme = new TableModelEvent(this);
				for (int l = 0; l < this.tmls.size(); l++)
					((TableModelListener) this.tmls.get(l)).tableChanged(tme);
			}
			
			public void addTableModelListener(TableModelListener tml) {
				if (tml != null)
					this.tmls.add(tml);
			}
			public void removeTableModelListener(TableModelListener tml) {
				if (tml != null)
					this.tmls.remove(tml);
			}
		});
		JScrollPane settingsTableBox = new JScrollPane(settingsTable);
		this.add(settingsTableBox, BorderLayout.CENTER);
	}
	
	private boolean validateParameterName(String settingName) {
		if (settingName.matches(this.keyRegEx) && settingName.matches(DEFAULT_KEY_REGEX))
			return true;
		JOptionPane.showMessageDialog(settingsTable, ("'" + settingName + "' is not a valid " + this.settingLabel.toLowerCase() + " name;\n" + this.keyRegExExplanation), ("Invalid " + this.settingLabel + " Name"), JOptionPane.ERROR_MESSAGE);
		return false;
	}
	
	private void updateKeys() {
		this.keys = this.settings.getKeys();
		Arrays.sort(this.keys);
	}

	/**
	 * Retrieve the settings in this panel. If the target Settings object is
	 * null, a new Settings object will be created and returned.
	 * @param target a Settings object to store the current settings in
	 * @return a Settings object containing the current settings
	 */
	public Settings getSettings(Settings target) {
		if (target == null)
			target = new Settings();
		target.setSettings(this.settings);
		return target;
	}
}
