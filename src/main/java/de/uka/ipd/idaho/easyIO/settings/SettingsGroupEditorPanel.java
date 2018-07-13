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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileFilter;

/**
 * An editor for the content of a Settings object, including sub sets. This
 * panel will contain a SettingsEditorPanel for local settings, i.e. the ones
 * not belonging to a sub set, and a child SettringsGroupEditorPanel for each
 * sub set. All the panels arranged in a JTabbedPane.
 * 
 * @author sautter
 */
public class SettingsGroupEditorPanel extends JPanel {
	
	private boolean isDirty = false;
	private Settings data;
	
	private SettingsGroupEditorPanel parent;
	private boolean developmentMode = false;
	
	private JPanel actionPanel = new JPanel();
	private SettingsEditorPanel ownSettings;
	private JTabbedPane tabs = new JTabbedPane();
	private JFileChooser fileChooser = new JFileChooser();
	
	/** Constructor
	 */
	public SettingsGroupEditorPanel() {
		this(null, null);
	}
	
	/** Constructor
	 * @param	data	the Settings to be edited in this SettingsGroupEditorPanel
	 */
	public SettingsGroupEditorPanel(Settings data) {
		this(data, null);
	}
	
	/** Constructor
	 * @param	data	the Settings to be edited in this SettingsGroupEditorPanel
	 * @param	parent	the parent SettingsGroupEditorPanel
	 */
	public SettingsGroupEditorPanel(Settings data, SettingsGroupEditorPanel parent) {
		super();
		this.data = ((data == null) ? new Settings() : data);
		this.parent = parent;
		this.init();
	}
	
	private void init() {
		this.setLayout(new BorderLayout());
		this.add(this.actionPanel, BorderLayout.NORTH);
		this.add(this.tabs, BorderLayout.CENTER);
		
		this.actionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		
		JButton newSubsetButton = new JButton(" New Subset ");
		newSubsetButton.setBorder(null);
		newSubsetButton.setBackground(Color.WHITE);
		newSubsetButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newSettingsGroup();
			}
		});
		this.actionPanel.add(newSubsetButton);
		
		//	add buttons for loading and saving if root panel
		if (this.parent == null) {
			JButton newSettingsButton = new JButton(" New ");
			newSettingsButton.setBorder(null);
			newSettingsButton.setBackground(Color.WHITE);
			newSettingsButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					newSettings();
				}
			});
			this.actionPanel.add(newSettingsButton);
			
			JButton loadSettingsFileButton = new JButton(" Load ");
			loadSettingsFileButton.setBorder(null);
			loadSettingsFileButton.setBackground(Color.WHITE);
			loadSettingsFileButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					loadSettings();
				}
			});
			this.actionPanel.add(loadSettingsFileButton);
			
			JButton saveSettingsFileButton = new JButton(" Save ");
			saveSettingsFileButton.setBorder(null);
			saveSettingsFileButton.setBackground(Color.WHITE);
			saveSettingsFileButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					saveSettings();
				}
			});
			this.actionPanel.add(saveSettingsFileButton);
			
			this.fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			this.fileChooser.addChoosableFileFilter(new ExtensionFileFilter(XML_FILE_FORMAT_EXTENSION, "XML style configuration files"));
			this.fileChooser.addChoosableFileFilter(new ExtensionFileFilter(TEXT_FILE_FORMAT_EXTENSION, "text style configuration files (CNFG)"));
		}
		
		//	add buttons for renaming and deleting if not root panel
		else {
			JButton renameSettingsGroupButton = new JButton(" Rename ");
			renameSettingsGroupButton.setBorder(null);
			renameSettingsGroupButton.setBackground(Color.WHITE);
			renameSettingsGroupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					renameSettingsGroup();
				}
			});
			this.actionPanel.add(renameSettingsGroupButton);
			
			JButton deleteSettingsGroupButton = new JButton(" Delete ");
			deleteSettingsGroupButton.setBorder(null);
			deleteSettingsGroupButton.setBackground(Color.WHITE);
			deleteSettingsGroupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteSettingsGroup();
				}
			});
			this.actionPanel.add(deleteSettingsGroupButton);
		}
		
		this.initTabs();
	}
	
	/**	@return	true if and only if the settings currently being edited are changable in development mode 
	 */
	boolean inDevelopmentMode() {
		return ((this.parent == null) ? this.developmentMode : this.parent.inDevelopmentMode());
	}
	
	private void initTabs() {
		this.tabs.removeAll();
		
//		this.ownSettings = new SettingsEditorPanel(this.data, this);
		this.ownSettings = new SettingsEditorPanel(this.data);
		this.tabs.add(" Local Settings ", this.ownSettings);
		
		String[] subsetPrefixes = this.data.getSubsetPrefixes();
		for (int p = 0; p < subsetPrefixes.length; p++) {
			SettingsGroupEditorPanel sgep = new SettingsGroupEditorPanel(this.data.getSubset(subsetPrefixes[p]), this);
			this.tabs.add(subsetPrefixes[p], sgep);
		}
	}
	
	private void newSettingsGroup() {
		if (this.inDevelopmentMode()) {
			String prefix = ((String) JOptionPane.showInputDialog(this, "Enter prefix for new group", "Create Settings Group", JOptionPane.PLAIN_MESSAGE, null, null, ""));
			if ((prefix != null) && (prefix.length() != 0) && !this.data.hasSubset(prefix)) {
				SettingsGroupEditorPanel sgep = new SettingsGroupEditorPanel(this.data.getSubset(prefix), this);
				this.tabs.add(prefix, sgep);
				this.revalidate();
				this.tabs.setSelectedComponent(sgep);
			}
		}
		else JOptionPane.showMessageDialog(this, "New subsets can be created in Development mode only.", "Function Not Available In Edit Mode", JOptionPane.INFORMATION_MESSAGE, null);
	}
	
	private void deleteSettingsGroup() {
		if (this.inDevelopmentMode())
			if ((this.parent != null) && (JOptionPane.showConfirmDialog(this, ("Really delete entire " + this.data.getPrefix() + " group?"), "Delete Settings Group", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)) this.parent.deleteSettingsGroup(this); 
		else JOptionPane.showMessageDialog(this, "Subsets can be deleted in Development mode only.", "Function Not Available In Edit Mode", JOptionPane.INFORMATION_MESSAGE, null);
	}
	
	private void deleteSettingsGroup(SettingsGroupEditorPanel sgep) {
		this.tabs.remove(sgep);
		this.data.removeSubset(sgep.data);
		this.revalidate();
	}
	
	private void renameSettingsGroup() {
		if (this.inDevelopmentMode()) {
			String newPrefix = ((String) JOptionPane.showInputDialog(this, "Enter new prefix", "Rename Settings Group", JOptionPane.PLAIN_MESSAGE, null, null, this.data.getPrefix()));
			if ((newPrefix != null) && (newPrefix.length() != 0) && (this.parent != null)) this.parent.renameSettingsGroup(this, newPrefix); 
		}
		else JOptionPane.showMessageDialog(this, "Subsets can be renamed in Development mode only.", "Function Not Available In Edit Mode", JOptionPane.INFORMATION_MESSAGE, null);
	}
	
	private void renameSettingsGroup(SettingsGroupEditorPanel sgep, String newPrefix) {
		if (!this.data.hasSubset(newPrefix)) {
			sgep.data.setPrefix(newPrefix);
			this.tabs.remove(sgep);
			this.tabs.add(sgep, newPrefix);
			this.revalidate();
			this.tabs.setSelectedComponent(sgep);
		}
	}
	
	/**	load a settings file to edit it
	 */
	public void loadSettings() {
		int choice = JOptionPane.NO_OPTION;
		if (this.isDirty) choice = JOptionPane.showOptionDialog(this, "Save current Settings before closing", "Save Settings", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (choice == JOptionPane.CANCEL_OPTION) return;
		else if ((choice == JOptionPane.NO_OPTION) || this.saveSettings()) {
			if (this.fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				this.data = Settings.loadSettings(this.fileChooser.getSelectedFile());
				this.initTabs();
				String[] modes = {"Edit", "Development"};
				this.developmentMode = (JOptionPane.showOptionDialog(this, "Open Settings in Edit mode or Development mode?\n- Edit mode: Change values only\n- Development mode: Full access", "Choose Mode", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, modes, "Edit") == 1);
				this.revalidate();
				this.isDirty = true;
			}
		}
	}
	
	private static final String XML_FILE_FORMAT_OPTION = "XML";
	private static final String TEXT_FILE_FORMAT_OPTION = "Text";
	private static final String CUSTOM_FILE_FORMAT_OPTION = "Other";
	private static final String[] FILE_FORMAT_OPTIONS = {XML_FILE_FORMAT_OPTION, TEXT_FILE_FORMAT_OPTION, CUSTOM_FILE_FORMAT_OPTION};
	private static final String XML_FILE_FORMAT_EXTENSION = "xml";
	private static final String TEXT_FILE_FORMAT_EXTENSION = "cnfg";
	private static final String[] FILE_FORMAT_EXTENSIONS = {XML_FILE_FORMAT_EXTENSION, TEXT_FILE_FORMAT_EXTENSION};
	
	/**	save the settings currently being edited
	 * @return true if and only if the settings were saved successfully
	 */
	public boolean saveSettings() {
		if (this.fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				File file = this.fileChooser.getSelectedFile();
				FileFilter filter = this.fileChooser.getFileFilter();
				String extension = "";
				if ((filter != null) && (filter instanceof ExtensionFileFilter))
					extension = ((ExtensionFileFilter) filter).getExtension();
				else {
					int choice = JOptionPane.showOptionDialog(this, "Save Settings as Text or as XML?", "Choose Save Mode", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, FILE_FORMAT_OPTIONS, XML_FILE_FORMAT_OPTION);
					if ((choice > -1) && (choice < FILE_FORMAT_EXTENSIONS.length)) extension = FILE_FORMAT_EXTENSIONS[choice];
				}
				if (!file.getName().toLowerCase().endsWith(extension.toLowerCase())) file = new File(file.toString() + "." + extension);
				if (XML_FILE_FORMAT_EXTENSION.equalsIgnoreCase(extension))
					Settings.storeSettingsAsXML(file, this.data);
				else Settings.storeSettingsAsText(file, this.data);
				return true;
			}
			catch (IOException ioe) {
				JOptionPane.showMessageDialog(this, (ioe.getMessage() + " while saving Settings"), "An Error Occured", JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		else return false;
	}
	
	private void newSettings() {
		int choice = JOptionPane.showOptionDialog(this, "Save Settings before closing", "Save Settings", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
		if (choice == JOptionPane.CANCEL_OPTION) return;
		if (choice == JOptionPane.YES_OPTION) this.saveSettings();
		this.data = new Settings();
		this.initTabs();
		this.revalidate();
	}
	
	private class ExtensionFileFilter extends FileFilter {
		
		private String originalFileExtension;
		private String unifiedFileExtension;
		private String description;
		
		/**	Constructor
		 */
		ExtensionFileFilter(String fileExtension, String description) {
			this.originalFileExtension = ((fileExtension == null) ? "" : fileExtension);
			this.unifiedFileExtension = ((fileExtension == null) ? "" : ((fileExtension.startsWith(".") ? "" : ".") + fileExtension.toLowerCase()));
			this.description = ((description == null) ? "" : description);
		}
		
		/** @see javax.swing.filechooser.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File file) {
			return ((file != null) && (file.isDirectory() || file.getName().toLowerCase().endsWith(this.unifiedFileExtension)));
		}
		
		/** @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription() {
			return this.description;
		}
		
		/**	@return	the extension used by this filter
		 */
		public String getExtension() {
			return this.originalFileExtension;
		}
	}
	
//	private class SettingsEditorPanel extends JPanel {
//		
//		private Settings data;
//		
//		private SettingsGroupEditorPanel parent;
//		
//		private JScrollPane settingsEditor = new JScrollPane();
//		private JPanel actionPanel = new JPanel();
//		private JButton newSettingButton = new JButton(" New Setting ");
//		private JPanel editorContent = new JPanel();
//		
//		/** Constructor
//		 * @param	data	the Settings to be edited in this SettingsEditorPanel
//		 */
//		public SettingsEditorPanel(Settings data, SettingsGroupEditorPanel parent) {
//			super(new BorderLayout(), true);
//			this.data = data;
//			this.parent = parent;
//			this.init();
//		}
//		
//		private void init() {
//			this.add(this.actionPanel, BorderLayout.NORTH);
//			this.add(this.settingsEditor, BorderLayout.CENTER);
//			
//			this.actionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
//			
//			this.newSettingButton.setBorder(null);
//			this.newSettingButton.setBackground(Color.WHITE);
//			this.newSettingButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent e) {
//					newSetting();
//				}
//			});
//			this.actionPanel.add(newSettingButton);
//			
//			this.settingsEditor.getViewport().add(this.editorContent);
//			this.editorContent.setLayout(new BoxLayout(this.editorContent, BoxLayout.Y_AXIS));
//			
//			//	add SettingsEditorLines for local settings to editorContent
//			String[] keys = this.data.getLocalKeys();
//			for (int k = 0; k < keys.length; k++) {
//				this.editorContent.add(new SettingsEditorLine(keys[k], this.data, this));
//			}
//		}
//		
//		boolean inDevelopmentMode() {
//			return this.parent.inDevelopmentMode();
//		}
//		
//		private void newSetting() {
//			if (this.parent.inDevelopmentMode()) {
//				String key = ((String) JOptionPane.showInputDialog(this.parent, "Enter new key below", "Create Setting", JOptionPane.PLAIN_MESSAGE, null, null, ""));
//				if ((key != null) && (key.length() != 0)) {
//					this.data.setSetting(key, "");
//					this.editorContent.add(new SettingsEditorLine(key, this.data, this));
//					this.editorContent.revalidate();
//				}
//			}
//			else JOptionPane.showMessageDialog(this, "Settings can be created in Development mode only.", "Function Not Available In Edit Mode", JOptionPane.INFORMATION_MESSAGE, null);
//		}
//		
//		void remove (SettingsEditorLine line) {
//			this.editorContent.remove(line);
//			this.editorContent.revalidate();
//		}
//		
//		private class SettingsEditorLine extends JPanel {
//			
//			private String key;
//			private Settings data;
//			
//			private SettingsEditorPanel parent;
//			
//			private JLabel keyDisplay = new JLabel();
//			private JButton renameButton = new JButton();
//			private JTextField valueDisplay = new JTextField();
//			private JButton deleteButton = new JButton();
//			
//			/**	Constructor
//			 * @param	key		the key of the setting to be edited within this SettingsEditorLine
//			 * @param	data	the Settings containing the key and value of the setting to be edited within this SettingsEditorLine
//			 */
//			SettingsEditorLine(String key, Settings data, SettingsEditorPanel parent) {
//				this.key = key;
//				this.data = data;
//				this.parent = parent;
//				this.init();
//			}
//			
//			private void init() {
//				this.setLayout(new FlowLayout(FlowLayout.LEFT));
//				this.setMaximumSize(new Dimension(2000, 30));
//				
//				this.keyDisplay.setText(this.key);
//				this.keyDisplay.setPreferredSize(new Dimension(200, 25));
//				this.add(this.keyDisplay);
//				
//				this.renameButton.setText(" Rename ");
//				this.renameButton.setBorder(null);
//				this.renameButton.setBackground(Color.WHITE);
//				this.renameButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						renameSetting();
//					}
//				});
//				this.add(this.renameButton);
//				
//				this.valueDisplay.setText(this.data.getSetting(this.key, ""));
//				this.valueDisplay.setPreferredSize(new Dimension(200, 25));
//				this.valueDisplay.addFocusListener(new FocusListener() {
//					public void focusGained(FocusEvent e) {}
//					
//					public void focusLost(FocusEvent e) {
//						storeValue();
//					}
//				});
//				this.add(this.valueDisplay);
//				
//				this.deleteButton.setText(" Delete ");
//				this.deleteButton.setBorder(null);
//				this.deleteButton.setBackground(Color.WHITE);
//				this.deleteButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						deleteSetting();
//					}
//				});
//				this.add(this.deleteButton);
//				
//				SwingUtilities.invokeLater(new Runnable() {
//					public void run() {
//						valueDisplay.requestFocus();
//					}
//				});
//			}
//			
//			private void renameSetting() {
//				if (this.parent.inDevelopmentMode()) {
//					String newKey = ((String) JOptionPane.showInputDialog(this.parent, "Enter new key below", ("Rename " + this.key), JOptionPane.PLAIN_MESSAGE, null, null, this.key));
//					if ((newKey != null) && (newKey.length() != 0)) {
//						String value = this.data.removeSetting(this.key);
//						this.data.setSetting(newKey, value);
//						this.key = newKey;
//						this.keyDisplay.setText(this.key);
//					}
//				}
//				else JOptionPane.showMessageDialog(this, "Settings can be renamed in Development mode only.", "Function Not Available In Edit Mode", JOptionPane.INFORMATION_MESSAGE, null);
//			}
//			
//			private void deleteSetting() {
//				if (this.parent.inDevelopmentMode()) {
//					this.data.removeSetting(this.key);
//					this.parent.remove(this);
//				}
//				else JOptionPane.showMessageDialog(this, "Settings can be deleted in Development mode only.", "Function Not Available In Edit Mode", JOptionPane.INFORMATION_MESSAGE, null);
//			}
//			
//			private void storeValue() {
//				this.data.setSetting(this.key, this.valueDisplay.getText());
//			}
//		}
//	}
}
