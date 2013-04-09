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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.gamta.util.validation.Criterion;
import de.uka.ipd.idaho.gamta.util.validation.ProcessPart;
import de.uka.ipd.idaho.gamta.util.validation.Step;

/**
 * Editor panle for a step in a markup process
 * 
 * @author sautter
 */
public class MarkupStepEditor extends MarkupProcessPartEditor {
	
	private JTextField contextEditor = new JTextField();
	private JTabbedPane criterions = new JTabbedPane();
	
	/**
	 * @param step
	 */
	public MarkupStepEditor(MarkupProcessEditorPanel host, MarkupTaskEditor parent, Step step) {
		super(host, parent, step);
		
		this.contextEditor.setText(step.getContextPath());
		this.contextEditor.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent de) {}
			public void insertUpdate(DocumentEvent de) {
				setDirty();
			}
			public void removeUpdate(DocumentEvent de) {
				setDirty();
			}
		});
		
		this.criterions.setTabPlacement(JTabbedPane.LEFT);
		Criterion[] criterions = step.getCriterions();
		for (int c = 0; c < criterions.length; c++) {
			final MarkupCriterionEditor ce = new MarkupCriterionEditor(host, this, criterions[c]);
			final String originalName = criterions[c].getName();
			ce.attributes.nameField.addFocusListener(new FocusAdapter() {
				private String name = originalName;
				public void focusLost(FocusEvent fe) {
					String newName = ce.attributes.nameField.getText();
					if (!newName.equals(this.name)) {
						int index = MarkupStepEditor.this.criterions.indexOfComponent(ce);
						if (index != -1) {
							MarkupStepEditor.this.criterions.setTitleAt(index, "<HTML><P align='center'><B>Criterion:</B><BR>" + newName + "</P></HTML>");
							this.name = newName;
						}
					}
				}
			});
			this.criterions.addTab("<HTML><P align='center'><B>Criterion:</B><BR>" + originalName + "</HTML>", ce);
		}
		
		JPanel contextPanel = new JPanel(new BorderLayout());
		contextPanel.add(new JLabel("<HTML><B>Context Path:&nbsp;</B></HTML>"), BorderLayout.WEST);
		contextPanel.add(this.contextEditor, BorderLayout.CENTER);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(contextPanel, BorderLayout.NORTH);
		mainPanel.add(this.criterions, BorderLayout.CENTER);
		
		this.add(mainPanel, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Add Criterion");
		addButton.setBorder(BorderFactory.createRaisedBevelBorder());
		addButton.setPreferredSize(new Dimension(100, 21));
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addCriterion();
			}
		});
		this.buttons.add(addButton);
		
		JButton importButton = new JButton("Import Criterion");
		importButton.setBorder(BorderFactory.createRaisedBevelBorder());
		importButton.setPreferredSize(new Dimension(100, 21));
		importButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				importCriterion();
			}
		});
		this.buttons.add(importButton);
		
		JButton moveButton = new JButton("Move Criterion");
		moveButton.setBorder(BorderFactory.createRaisedBevelBorder());
		moveButton.setPreferredSize(new Dimension(100, 21));
		moveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				moveCriterion();
			}
		});
		this.buttons.add(moveButton);
		
		JButton removeButton = new JButton("Remove Criterion");
		removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		removeButton.setPreferredSize(new Dimension(100, 21));
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeCriterion();
			}
		});
		this.buttons.add(removeButton);
		
		JButton testButton = new JButton("Test Step");
		testButton.setBorder(BorderFactory.createRaisedBevelBorder());
		testButton.setPreferredSize(new Dimension(100, 21));
		testButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				testProcessPart();
			}
		});
		this.buttons.add(testButton);
		
		JButton validateContextButton = new JButton("Validate Context");
		validateContextButton.setBorder(BorderFactory.createRaisedBevelBorder());
		validateContextButton.setPreferredSize(new Dimension(100, 21));
		validateContextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				validateContext();
			}
		});
		this.buttons.add(validateContextButton);
		
		JButton testContextButton = new JButton("Test Context");
		testContextButton.setBorder(BorderFactory.createRaisedBevelBorder());
		testContextButton.setPreferredSize(new Dimension(100, 21));
		testContextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				testContext();
			}
		});
		this.buttons.add(testContextButton);
	}
	
	private void validateContext() {
		boolean selected = true;
		String gPath = this.contextEditor.getSelectedText();
		if ((gPath == null) || (gPath.length() == 0)) {
			gPath = this.contextEditor.getText();
			selected = false;
		}
		String error = GPathParser.validatePath(gPath);
		if (error == null) JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected path part" : "path") + " is valid."), "GPath Validation", JOptionPane.INFORMATION_MESSAGE);
		else JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected path part" : "path") + " is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
	}
	
	private void testContext() {
		boolean selected = true;
		String gPath = this.contextEditor.getSelectedText();
		if ((gPath == null) || (gPath.length() == 0)) {
			gPath = this.contextEditor.getText();
			selected = false;
		}
		
		String error = GPathParser.validatePath(gPath);
		if (error != null) JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected path part" : "path") + " is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
		else try {
			QueriableAnnotation startDoc = host.getStartDocument();
			if (startDoc != null) {
				Annotation[] startAnnotations = GPath.evaluatePath(startDoc, gPath, null);
				
				QueriableAnnotation goldDoc = host.getGoldDocument();
				Annotation[] goldAnnotations = ((goldDoc == null) ? null : GPath.evaluatePath(goldDoc, gPath, null));
				
				StepTestDialog ctd = new StepTestDialog(startAnnotations, goldAnnotations);
				ctd.setLocationRelativeTo(this);
				ctd.setVisible(true);
			}
		}
		catch (GPathException gpe) {
			JOptionPane.showMessageDialog(this, gpe, "GPath Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private class StepTestDialog extends JPanel {
		private JDialog dialog;
		StepTestDialog(Annotation[] startAnnotations, Annotation[] goldAnnotations) {
			super(new BorderLayout(), true);
			this.dialog = DialogFactory.produceDialog("Results Of Step", true);
			
			JButton okButton = new JButton("OK");
			okButton.setBorder(BorderFactory.createRaisedBevelBorder());
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dialog.dispose();
				}
			});
			this.add(okButton, BorderLayout.SOUTH);
			
			AnnotationDisplayPanel startDisplay = new AnnotationDisplayPanel(startAnnotations);
			if (goldAnnotations == null) this.add(startDisplay, BorderLayout.CENTER);
			else {
				AnnotationDisplayPanel goldDisplay = new AnnotationDisplayPanel(goldAnnotations);
				JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, startDisplay, goldDisplay);
				split.setDividerLocation(.5);
				split.setResizeWeight(.5);
				split.setEnabled(true);
				this.add(split, BorderLayout.CENTER);
			}
			
			this.dialog.getContentPane().setLayout(new BorderLayout());
			this.dialog.getContentPane().add(this, BorderLayout.CENTER);
			this.dialog.setSize(((goldAnnotations == null) ? 400 : 700), 500);
			this.dialog.setResizable(true);
		}
		
		void setLocationRelativeTo(Component comp) {
			this.dialog.setLocationRelativeTo(comp);
		}
		
		public void setVisible(boolean visible) {
			this.dialog.setVisible(visible);
		}
		
		private class AnnotationDisplayPanel extends JPanel {
			private JTable annotationTable;
			
			AnnotationDisplayPanel(Annotation[] annotations) {
				super(new BorderLayout(), true);
				
				this.setBorder(BorderFactory.createEtchedBorder());
				
				this.annotationTable = new JTable(new AnnotationTableModel(annotations));
				this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(120);
				this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(60);
				this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(60);
				
				JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
				this.add(annotationTableBox, BorderLayout.CENTER);
			}
			
			private class AnnotationTableModel implements TableModel {
				private Annotation[] annotations;
				
				AnnotationTableModel(Annotation[] annotations) {
					this.annotations = annotations;
				}
				
				public int getRowCount() {
					return this.annotations.length;
				}
				
				public int getColumnCount() {
					return 4;
				}
				
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				
				public String getColumnName(int columnIndex) {
					if (columnIndex == 0) return "Type";
					if (columnIndex == 1) return "Start";
					if (columnIndex == 2) return "Size";
					if (columnIndex == 3) return "Value";
					return null;
				}
				
				public Object getValueAt(int rowIndex, int columnIndex) {
					Annotation a = this.annotations[rowIndex];
					if (columnIndex == 0) return a.getType();
					if (columnIndex == 1) return "" + a.getStartIndex();
					if (columnIndex == 2) return "" + a.size();
					if (columnIndex == 3) return a.getValue();
					return null;
				}
				
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				
				public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
				
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			}
		}
	}
	
	private void addCriterion() {
		this.addCriterion(new Criterion(null, "", "", "", "", ""));
	}
	
	private void importCriterion() {
		Criterion criterion = this.host.selectCriterion(true);
		if (criterion != null)
			this.addCriterion(criterion);
	}
	
	private void addCriterion(Criterion criterion) {
		final MarkupCriterionEditor ce = new MarkupCriterionEditor(this.host, this, criterion);
		final String originalName = "";
		ce.attributes.nameField.addFocusListener(new FocusAdapter() {
			private String name = originalName;
			public void focusLost(FocusEvent fe) {
				String newName = ce.attributes.nameField.getText();
				if (!newName.equals(this.name)) {
					int index = MarkupStepEditor.this.criterions.indexOfComponent(ce);
					if (index != -1) {
						MarkupStepEditor.this.criterions.setTitleAt(index, "<HTML><P align='center'><B>Criterion:</B><BR>" + newName + "</P></HTML>");
						this.name = newName;
					}
				}
			}
		});
		int index = this.criterions.getSelectedIndex();
		if (index == -1) this.criterions.addTab("<HTML><P align='center'><B>Criterion:</B><BR>" + originalName + "</HTML>", ce);
		else {
			this.criterions.insertTab("<HTML><P align='center'><B>Criterion:</B><BR>" + "" + "</HTML>", null, ce, "", index);
			this.criterions.setSelectedIndex(index);
		}
		this.setDirty();
	}
	
	private void moveCriterion() {
		int index = this.criterions.getSelectedIndex();
		if (index != -1) {
			Step target = this.host.selectStep(false);
			if (target != null) {
				MarkupStepEditor targetEditor = ((MarkupStepEditor) this.host.getProcessPartEditor(target));
				if (targetEditor != null) {
					targetEditor.addCriterion(((MarkupCriterionEditor) this.criterions.getComponentAt(index)).getCriterion());
					this.criterions.remove(index);
					this.setDirty();
				}
			}
		}
	}
	
	private void removeCriterion() {
		int index = this.criterions.getSelectedIndex();
		if (index != -1) {
			this.criterions.remove(index);
			this.setDirty();
		}
	}
	
	String getContextPath() {
		return this.contextEditor.getText();
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditor#getProcessPart()
	 */
	public ProcessPart getProcessPart() {
		return this.getStep();
	}
	
	public Step getStep() {
		ArrayList criterionList = new ArrayList();
		for (int i = 0; i < this.criterions.getTabCount(); i++) {
			MarkupCriterionEditor ce = ((MarkupCriterionEditor) this.criterions.getComponentAt(i));
			criterionList.add(ce.getCriterion());
		}
		return new Step(
				null,
				this.attributes.nameField.getText(),
				this.attributes.labelField.getText(),
				this.attributes.descriptionField.getText(),
				this.attributes.errorDescriptionField.getText(),
				this.contextEditor.getText(),
				((Criterion[]) criterionList.toArray(new Criterion[criterionList.size()]))
			);
	}
	
	/**
	 * @return the step editor currently selected, or null, if there is none
	 */
	public MarkupCriterionEditor getSelectedCriterion() {
		Component c = this.criterions.getSelectedComponent();
		return (((c != null) && (c instanceof MarkupCriterionEditor)) ? ((MarkupCriterionEditor) c) : null);
	}
	
	MarkupCriterionEditor getCriterionEditor(String name) {
		for (int c = 0; c < this.criterions.getTabCount(); c++) {
			MarkupCriterionEditor ce = ((MarkupCriterionEditor) this.criterions.getComponentAt(c));
			if (name.equals(ce.getProcessPartName()))
				return ce;
		}
		return null;
	}
}
