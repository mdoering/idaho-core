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
import de.uka.ipd.idaho.gamta.util.validation.ProcessPart;
import de.uka.ipd.idaho.gamta.util.validation.Step;
import de.uka.ipd.idaho.gamta.util.validation.Task;

/**
 * Editor panel for a level in a markup process
 * 
 * @author sautter
 */
public class MarkupLevelEditor extends MarkupProcessPartEditor {

	private JTabbedPane tasks = new JTabbedPane();
	
	/**
	 * @param level
	 */
	public MarkupLevelEditor(MarkupProcessEditorPanel host, MarkupProcessEditor parent, Level level) {
		super(host, parent, level);
		
		this.tasks.setTabPlacement(JTabbedPane.LEFT);
		Task[] tasks = level.getTasks();
		for (int t = 0; t < tasks.length; t++) {
			final MarkupTaskEditor te = new MarkupTaskEditor(host, this, tasks[t]);
			final String originalName = tasks[t].getName();
			te.attributes.nameField.addFocusListener(new FocusAdapter() {
				private String name = originalName;
				public void focusLost(FocusEvent fe) {
					String newName = te.attributes.nameField.getText();
					if (!newName.equals(this.name)) {
						int index = MarkupLevelEditor.this.tasks.indexOfComponent(te);
						if (index != -1) {
							MarkupLevelEditor.this.tasks.setTitleAt(index, "<HTML><P align='center'><B>Task:</B><BR>" + newName + "</P></HTML>");
							this.name = newName;
						}
					}
				}
			});
			this.tasks.addTab("<HTML><P align='center'><B>Task:</B><BR>" + originalName + "</HTML>", te);
		}
		
		this.add(this.tasks, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Add Task");
		addButton.setBorder(BorderFactory.createRaisedBevelBorder());
		addButton.setPreferredSize(new Dimension(100, 21));
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addTask();
			}
		});
		this.buttons.add(addButton);
		
		JButton importButton = new JButton("Import Task");
		importButton.setBorder(BorderFactory.createRaisedBevelBorder());
		importButton.setPreferredSize(new Dimension(100, 21));
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				importTask();
			}
		});
		this.buttons.add(importButton);
		
		JButton moveButton = new JButton("Move Task");
		moveButton.setBorder(BorderFactory.createRaisedBevelBorder());
		moveButton.setPreferredSize(new Dimension(100, 21));
		moveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveTask();
			}
		});
		this.buttons.add(moveButton);
		
		JButton removeButton = new JButton("Remove Task");
		removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		removeButton.setPreferredSize(new Dimension(100, 21));
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeTask();
			}
		});
		this.buttons.add(removeButton);
		
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
		
		JButton testButton = new JButton("Test Level");
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
		int index = this.tasks.getSelectedIndex();
		if (index > 0) {
			String title = this.tasks.getTitleAt(index);
			String toolTip = this.tasks.getToolTipTextAt(index);
			Component level = this.tasks.getComponentAt(index);
			this.tasks.remove(index);
			this.tasks.insertTab(title, null, level, toolTip, (index - 1));
			this.tasks.setSelectedIndex(index - 1);
			this.setDirty();
		}
	}
	
	private void moveDown() {
		int index = this.tasks.getSelectedIndex();
		if ((index != -1) && ((index + 1) < this.tasks.getTabCount())) {
			String title = this.tasks.getTitleAt(index);
			String toolTip = this.tasks.getToolTipTextAt(index);
			Component level = this.tasks.getComponentAt(index);
			this.tasks.remove(index);
			this.tasks.insertTab(title, null, level, toolTip, (index + 1));
			this.tasks.setSelectedIndex(index + 1);
			this.setDirty();
		}
	}
	
	private void addTask() {
		this.addTask(new Task(null, "", "", "", "", new Step[0]));
	}
	
	private void importTask() {
		Task task = this.host.selectTask(true);
		if (task != null)
			this.addTask(task);
	}
	
	private void addTask(Task task) {
		final MarkupTaskEditor te = new MarkupTaskEditor(host, this, task);
		final String originalName = "";
		te.attributes.nameField.addFocusListener(new FocusAdapter() {
			private String name = originalName;
			public void focusLost(FocusEvent fe) {
				String newName = te.attributes.nameField.getText();
				if (!newName.equals(this.name)) {
					int index = MarkupLevelEditor.this.tasks.indexOfComponent(te);
					if (index != -1) {
						MarkupLevelEditor.this.tasks.setTitleAt(index, "<HTML><P align='center'><B>Task:</B><BR>" + newName + "</P></HTML>");
						this.name = newName;
					}
				}
			}
		});
		int index = this.tasks.getSelectedIndex();
		if (index == -1) this.tasks.addTab("<HTML><P align='center'><B>Task:</B><BR>" + originalName + "</HTML>", null, te, "");
		else {
			this.tasks.insertTab("<HTML><P align='center'><B>Task:</B><BR>" + originalName + "</HTML>", null, te, "", index);
			this.tasks.setSelectedIndex(index);
		}
		this.setDirty();
	}
	
	private void moveTask() {
		int index = this.tasks.getSelectedIndex();
		if (index != -1) {
			Level target = this.host.selectLevel(false);
			if (target != null) {
				MarkupLevelEditor targetEditor = ((MarkupLevelEditor) this.host.getProcessPartEditor(target));
				if (targetEditor != null) {
					targetEditor.addTask(((MarkupTaskEditor) this.tasks.getComponentAt(index)).getTask());
					this.tasks.remove(index);
					this.setDirty();
				}
			}
		}
	}
	
	private void removeTask() {
		int index = this.tasks.getSelectedIndex();
		if (index != -1) {
			this.tasks.remove(index);
			this.setDirty();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditor#getProcessPartFullName()
	 */
	public String getProcessPartFullName() {
		return this.getProcessPartName(); // exclude process name, it's the same throughout
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditor#getProcessPart()
	 */
	public ProcessPart getProcessPart() {
		return this.getLevel();
	}
	
	public Level getLevel() {
		ArrayList taskList = new ArrayList();
		for (int i = 0; i < this.tasks.getTabCount(); i++) {
			Component co = this.tasks.getComponentAt(i);
			if (co instanceof MarkupTaskEditor) {
				MarkupTaskEditor te = ((MarkupTaskEditor) co);
				taskList.add(te.getTask());
			}
		}
		
		return new Level(
				null,
				this.attributes.nameField.getText(),
				this.attributes.labelField.getText(),
				this.attributes.descriptionField.getText(),
				this.attributes.errorDescriptionField.getText(),
				((Task[]) taskList.toArray(new Task[taskList.size()]))
			);
	}
	
	/**
	 * @return the task editor currently selected, or null, if there is none
	 */
	public MarkupTaskEditor getSelectedTask() {
		Component c = this.tasks.getSelectedComponent();
		return (((c != null) && (c instanceof MarkupTaskEditor)) ? ((MarkupTaskEditor) c) : null);
	}
	
	MarkupTaskEditor getTaskEditor(String name) {
		for (int t = 0; t < this.tasks.getTabCount(); t++) {
			MarkupTaskEditor te = ((MarkupTaskEditor) this.tasks.getComponentAt(t));
			if (name.equals(te.getProcessPartName()) || name.startsWith(te.getProcessPartName() + "."))
				return te;
		}
		return null;
	}
}
