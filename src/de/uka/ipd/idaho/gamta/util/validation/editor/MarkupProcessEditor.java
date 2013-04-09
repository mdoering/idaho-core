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
package de.uka.ipd.idaho.gamta.util.validation.editor;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import de.uka.ipd.idaho.gamta.util.validation.Level;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.ProcessPart;
import de.uka.ipd.idaho.gamta.util.validation.Task;

/**
 * Editor panel for a markup process as a whole
 * 
 * @author sautter
 */
public class MarkupProcessEditor extends MarkupProcessPartEditor {
	
	private JTabbedPane levels = new JTabbedPane();
	
	/**
	 * @param host the test document provider to obtain test data from
	 * @param process the process to edit
	 */
	public MarkupProcessEditor(MarkupProcessEditorPanel host, Process process) {
		super(host, null, process);
		
		this.levels.setBorder(BorderFactory.createEmptyBorder());
		this.levels.setTabPlacement(JTabbedPane.LEFT);
		Level[] levels = process.getLevels();
		for (int l = 0; l < levels.length; l++) {
			final MarkupLevelEditor le = new MarkupLevelEditor(host, this, levels[l]);
			final String originalName = levels[l].getName();
			le.attributes.nameField.addFocusListener(new FocusAdapter() {
				private String name = originalName;
				public void focusLost(FocusEvent fe) {
					String newName = le.attributes.nameField.getText();
					if (!newName.equals(this.name)) {
						int index = MarkupProcessEditor.this.levels.indexOfComponent(le);
						if (index != -1) {
							MarkupProcessEditor.this.levels.setTitleAt(index, "<HTML><P align='center'><B>Level:</B><BR>" + newName + "</P></HTML>");
							this.name = newName;
						}
					}
				}
			});
			this.levels.addTab("<HTML><P align='center'><B>Level:</B><BR>" + originalName + "</P></HTML>", le);
		}
		
		JButton upButton = new JButton("Up");
		upButton.setBorder(BorderFactory.createRaisedBevelBorder());
		upButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveUp();
			}
		});
		JButton downButton = new JButton("Down");
		downButton.setBorder(BorderFactory.createRaisedBevelBorder());
		downButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveDown();
			}
		});
		JPanel reorderButtonPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.top = 2;
		gbc.insets.bottom = 2;
		gbc.insets.left = 5;
		gbc.insets.right = 3;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridx = 0;
		gbc.gridwidth = 1;
		
		gbc.gridy = 0;
		reorderButtonPanel.add(upButton, gbc.clone());
		gbc.gridy = 1;
		reorderButtonPanel.add(downButton, gbc.clone());
		
		this.add(reorderButtonPanel, BorderLayout.WEST);
		this.add(this.levels, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Add Level");
		addButton.setBorder(BorderFactory.createRaisedBevelBorder());
		addButton.setPreferredSize(new Dimension(100, 21));
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addLevel();
			}
		});
		this.buttons.add(addButton);
		
		JButton importButton = new JButton("Import Level");
		importButton.setBorder(BorderFactory.createRaisedBevelBorder());
		importButton.setPreferredSize(new Dimension(100, 21));
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				importLevel();
			}
		});
		this.buttons.add(importButton);
		
		JButton removeButton = new JButton("Remove Level");
		removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		removeButton.setPreferredSize(new Dimension(100, 21));
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeLevel();
			}
		});
		this.buttons.add(removeButton);
		
		JButton testButton = new JButton("Test Process");
		testButton.setBorder(BorderFactory.createRaisedBevelBorder());
		testButton.setPreferredSize(new Dimension(100, 21));
		testButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				testProcessPart();
			}
		});
		this.buttons.add(testButton);
	}
	
	private void moveUp() {
		int index = this.levels.getSelectedIndex();
		if (index > 0) {
			String title = this.levels.getTitleAt(index);
			String toolTip = this.levels.getToolTipTextAt(index);
			Component level = this.levels.getComponentAt(index);
			this.levels.remove(index);
			this.levels.insertTab(title, null, level, toolTip, (index - 1));
			this.levels.setSelectedIndex(index - 1);
			this.setDirty();
		}
	}
	
	private void moveDown() {
		int index = this.levels.getSelectedIndex();
		if ((index != -1) && ((index + 1) < this.levels.getTabCount())) {
			String title = this.levels.getTitleAt(index);
			String toolTip = this.levels.getToolTipTextAt(index);
			Component level = this.levels.getComponentAt(index);
			this.levels.remove(index);
			this.levels.insertTab(title, null, level, toolTip, (index + 1));
			this.levels.setSelectedIndex(index + 1);
			this.setDirty();
		}
	}
	
	private void addLevel() {
		this.addLevel(new Level(null, "", "", "", "", new Task[0]));
	}
	
	private void importLevel() {
		Level level = this.host.selectLevel(true);
		if (level != null)
			this.addLevel(level);
	}
	
	private void addLevel(Level level) {
		final MarkupLevelEditor le = new MarkupLevelEditor(host, this, level);
		final String originalName = "";
		le.attributes.nameField.addFocusListener(new FocusAdapter() {
			private String name = originalName;
			public void focusLost(FocusEvent fe) {
				String newName = le.attributes.nameField.getText();
				if (!newName.equals(this.name)) {
					int index = MarkupProcessEditor.this.levels.indexOfComponent(le);
					if (index != -1) {
						MarkupProcessEditor.this.levels.setTitleAt(index, "<HTML><P align='center'><B>Level:</B><BR>" + newName + "</P></HTML>");
						this.name = newName;
					}
				}
			}
		});
		int index = this.levels.getSelectedIndex();
		if (index == -1) this.levels.addTab("<HTML><P align='center'><B>Level:</B><BR>" + originalName + "</P></HTML>", le);
		else {
			this.levels.insertTab("<HTML><P align='center'><B>Level:</B><BR>" + originalName + "</P></HTML>", null, le, "", index);
			this.levels.setSelectedIndex(index);
		}
		this.setDirty();
	}
	
	private void removeLevel() {
		int index = this.levels.getSelectedIndex();
		if (index != -1) {
			this.levels.remove(index);
			this.setDirty();
		}
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditor#getProcessPart()
	 */
	public ProcessPart getProcessPart() {
		return this.getProcess();
	}
	
	public Process getProcess() {
		ArrayList levelList = new ArrayList();
		for (int i = 0; i < this.levels.getTabCount(); i++) {
			MarkupLevelEditor le = ((MarkupLevelEditor) this.levels.getComponentAt(i));
			levelList.add(le.getLevel());
		}
		return new Process(
				this.attributes.nameField.getText(),
				this.attributes.labelField.getText(),
				this.attributes.descriptionField.getText(),
				this.attributes.errorDescriptionField.getText(),
				((Level[]) levelList.toArray(new Level[levelList.size()]))
			);
	}
	
	/**
	 * @return the level editor currently selected, or null, if there is none
	 */
	public MarkupLevelEditor getSelectedLevel() {
		Component c = this.levels.getSelectedComponent();
		return (((c != null) && (c instanceof MarkupLevelEditor)) ? ((MarkupLevelEditor) c) : null);
	}
	
	MarkupLevelEditor getLevelEditor(String name) {
		for (int l = 0; l < this.levels.getTabCount(); l++) {
			MarkupLevelEditor le = ((MarkupLevelEditor) this.levels.getComponentAt(l));
			if (name.equals(le.getProcessPartName()) || name.startsWith(le.getProcessPartName() + "."))
				return le;
		}
		return null;
	}
}
