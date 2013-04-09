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

import de.uka.ipd.idaho.gamta.util.validation.Criterion;
import de.uka.ipd.idaho.gamta.util.validation.ProcessPart;
import de.uka.ipd.idaho.gamta.util.validation.Step;
import de.uka.ipd.idaho.gamta.util.validation.Task;

/**
 * Editor panel for a task in a markup process
 * 
 * @author sautter
 */
public class MarkupTaskEditor extends MarkupProcessPartEditor {
	
	private JTabbedPane steps = new JTabbedPane();
	
	/**
	 * @param task
	 */
	public MarkupTaskEditor(MarkupProcessEditorPanel host, MarkupLevelEditor parent, Task task) {
		super(host, parent, task);
		
		this.steps.setTabPlacement(JTabbedPane.LEFT);
		Step[] steps = task.getSteps();
		for (int s = 0; s < steps.length; s++) {
			final MarkupStepEditor se = new MarkupStepEditor(host, this, steps[s]);
			final String originalName = steps[s].getName();
			se.attributes.nameField.addFocusListener(new FocusAdapter() {
				private String name = originalName;
				public void focusLost(FocusEvent fe) {
					String newName = se.attributes.nameField.getText();
					if (!newName.equals(this.name)) {
						int index = MarkupTaskEditor.this.steps.indexOfComponent(se);
						if (index != -1) {
							MarkupTaskEditor.this.steps.setTitleAt(index, "<HTML><P align='center'><B>Step:</B><BR>" + newName + "</P></HTML>");
							this.name = newName;
						}
					}
				}
			});
			this.steps.addTab("<HTML><P align='center'><B>Step:</B><BR>" + originalName + "</HTML>", se);
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
		this.add(this.steps, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Add Step");
		addButton.setBorder(BorderFactory.createRaisedBevelBorder());
		addButton.setPreferredSize(new Dimension(100, 21));
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addStep();
			}
		});
		this.buttons.add(addButton);
		
		JButton importButton = new JButton("Import Step");
		importButton.setBorder(BorderFactory.createRaisedBevelBorder());
		importButton.setPreferredSize(new Dimension(100, 21));
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				importStep();
			}
		});
		this.buttons.add(importButton);
		
		JButton moveButton = new JButton("Move Step");
		moveButton.setBorder(BorderFactory.createRaisedBevelBorder());
		moveButton.setPreferredSize(new Dimension(100, 21));
		moveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveStep();
			}
		});
		this.buttons.add(moveButton);
		
		JButton removeButton = new JButton("Remove Step");
		removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		removeButton.setPreferredSize(new Dimension(100, 21));
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeStep();
			}
		});
		this.buttons.add(removeButton);
		
		JButton testButton = new JButton("Test Task");
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
		int index = this.steps.getSelectedIndex();
		if (index > 0) {
			String title = this.steps.getTitleAt(index);
			String toolTip = this.steps.getToolTipTextAt(index);
			Component level = this.steps.getComponentAt(index);
			this.steps.remove(index);
			this.steps.insertTab(title, null, level, toolTip, (index - 1));
			this.steps.setSelectedIndex(index - 1);
			this.setDirty();
		}
	}
	
	private void moveDown() {
		int index = this.steps.getSelectedIndex();
		if ((index != -1) && ((index + 1) < this.steps.getTabCount())) {
			String title = this.steps.getTitleAt(index);
			String toolTip = this.steps.getToolTipTextAt(index);
			Component level = this.steps.getComponentAt(index);
			this.steps.remove(index);
			this.steps.insertTab(title, null, level, toolTip, (index + 1));
			this.steps.setSelectedIndex(index + 1);
			this.setDirty();
		}
	}
	
	private void addStep() {
		this.addStep(new Step(null, "", "", "", "", "", new Criterion[0]));
	}
	
	private void importStep() {
		Step step = this.host.selectStep(true);
		if (step != null)
			this.addStep(step);
	}
	
	private void addStep(Step step) {
		final MarkupStepEditor se = new MarkupStepEditor(host, this, step);
		final String originalName = "";
		se.attributes.nameField.addFocusListener(new FocusAdapter() {
			private String name = originalName;
			public void focusLost(FocusEvent fe) {
				String newName = se.attributes.nameField.getText();
				if (!newName.equals(this.name)) {
					int index = MarkupTaskEditor.this.steps.indexOfComponent(se);
					if (index != -1) {
						MarkupTaskEditor.this.steps.setTitleAt(index, "<HTML><P align='center'><B>Step:</B><BR>" + newName + "</P></HTML>");
						this.name = newName;
					}
				}
			}
		});
		int index = this.steps.getSelectedIndex();
		if (index == -1) this.steps.addTab("<HTML><P align='center'><B>Step:</B><BR>" + originalName + "</HTML>", se);
		else {
			this.steps.insertTab("<HTML><P align='center'><B>Step:</B><BR>" + originalName + "</HTML>", null, se, "", index);
			this.steps.setSelectedIndex(index);
		}
		this.setDirty();
	}
	
	private void moveStep() {
		int index = this.steps.getSelectedIndex();
		if (index != -1) {
			Task target = this.host.selectTask(false);
			if (target != null) {
				MarkupTaskEditor targetEditor = ((MarkupTaskEditor) this.host.getProcessPartEditor(target));
				if (targetEditor != null) {
					targetEditor.addStep(((MarkupStepEditor) this.steps.getComponentAt(index)).getStep());
					this.steps.remove(index);
					this.setDirty();
				}
			}
		}
	}
	
	private void removeStep() {
		int index = this.steps.getSelectedIndex();
		if (index != -1) {
			this.steps.remove(index);
			this.setDirty();
		}
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditor#getProcessPart()
	 */
	public ProcessPart getProcessPart() {
		return this.getTask();
	}
	
	public Task getTask() {
		ArrayList stepList = new ArrayList();
		for (int i = 0; i < this.steps.getTabCount(); i++) {
			MarkupStepEditor se = ((MarkupStepEditor) this.steps.getComponentAt(i));
			stepList.add(se.getStep());
		}
		return new Task(
				null,
				this.attributes.nameField.getText(),
				this.attributes.labelField.getText(),
				this.attributes.descriptionField.getText(),
				this.attributes.errorDescriptionField.getText(),
				((Step[]) stepList.toArray(new Step[stepList.size()]))
			);
	}
	
	/**
	 * @return the step editor currently selected, or null, if there is none
	 */
	public MarkupStepEditor getSelectedStep() {
		Component c = this.steps.getSelectedComponent();
		return (((c != null) && (c instanceof MarkupStepEditor)) ? ((MarkupStepEditor) c) : null);
	}
	
	MarkupStepEditor getStepEditor(String name) {
		for (int s = 0; s < this.steps.getTabCount(); s++) {
			MarkupStepEditor se = ((MarkupStepEditor) this.steps.getComponentAt(s));
			if (name.equals(se.getProcessPartName()) || name.startsWith(se.getProcessPartName() + "."))
				return se;
		}
		return null;
	}
}
