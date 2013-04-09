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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
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

/**
 * Editor panel for an individual validation criterieon in a markup process
 * 
 * @author sautter
 */
public class MarkupCriterionEditor extends MarkupProcessPartEditor {
	
	private ExpressionEditorPanel editor;
	
	/**
	 * @param criterion
	 */
	public MarkupCriterionEditor(MarkupProcessEditorPanel host, MarkupStepEditor parent, Criterion criterion) {
		super(host, parent, criterion);
		
		this.editor = new ExpressionEditorPanel();
		this.editor.setContent(criterion.getExpression());
		this.add(this.editor, BorderLayout.CENTER);
	}
	
	/** @see de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditor#getProcessPart()
	 */
	public ProcessPart getProcessPart() {
		return this.getCriterion();
	}
	
	public Criterion getCriterion() {
		return new Criterion(
				null,
				this.attributes.nameField.getText(),
				this.attributes.labelField.getText(),
				this.attributes.descriptionField.getText(),
				this.attributes.errorDescriptionField.getText(),
				this.editor.getContent()
			);
	}
	
	private class ExpressionEditorPanel extends JPanel implements DocumentListener {
		
		private static final int MAX_SCROLLBAR_WAIT = 200;
		
		private JTextArea editor;
		private JScrollPane editorBox;
		
		private String content = "";
		private boolean expressionDirty = false;
		
		ExpressionEditorPanel() {
			super(new BorderLayout(), true);
			
			//	initialize editor
			this.editor = new JTextArea();
			this.editor.setEditable(true);
			
			//	wrap editor in scroll pane
			this.editorBox = new JScrollPane(this.editor);
			
			//	initialize buttons
			JButton refreshButton = new JButton("Refresh");
			refreshButton.setBorder(BorderFactory.createRaisedBevelBorder());
			refreshButton.setPreferredSize(new Dimension(115, 21));
			refreshButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					refreshExpression();
				}
			});
			
			JButton validateButton = new JButton("Validate");
			validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
			validateButton.setPreferredSize(new Dimension(115, 21));
			validateButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					validateExpression();
				}
			});
			
			JButton testButton = new JButton("Test");
			testButton.setBorder(BorderFactory.createRaisedBevelBorder());
			testButton.setPreferredSize(new Dimension(115, 21));
			testButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testExpression();
				}
			});
			
			JPanel buttonPanel = new JPanel(new GridBagLayout(), true);
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 3;
			gbc.insets.bottom = 3;
			gbc.insets.left = 3;
			gbc.insets.right = 3;
			gbc.weighty = 0;
			gbc.weightx = 1;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.gridy = 0;
			gbc.gridx = 0;
			buttonPanel.add(refreshButton, gbc.clone());
			gbc.gridx ++;
			buttonPanel.add(validateButton, gbc.clone());
			gbc.gridx ++;
			buttonPanel.add(testButton, gbc.clone());
			
			//	put the whole stuff together
			this.add(this.editorBox, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
			this.refreshDisplay();
		}
		
		String getContent() {
			if (this.isDirty()) this.content = GPath.normalizePath(this.editor.getText());
			return this.content;
		}
		
		void setContent(String expression) {
			this.content = GPath.normalizePath(expression);
			this.refreshDisplay();
			this.expressionDirty = false;
		}
		
		boolean isDirty() {
			return this.expressionDirty;
		}
		
		private void refreshExpression() {
			String expression = this.editor.getText();
			if ((expression != null) && (expression.length() != 0)) {
				
				final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
				final int scrollPosition = scroller.getValue();
				
				String normalizedGPath = GPath.normalizePath(expression);
				this.editor.getDocument().removeDocumentListener(this);
				this.editor.setText(GPath.explodePath(normalizedGPath, "  "));
				this.editor.getDocument().addDocumentListener(this);
				
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						int scrollbarWaitCounter = 0;
						while (scroller.getValueIsAdjusting() && (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT)) try {
							Thread.sleep(10);
							scrollbarWaitCounter ++;
						} catch (Exception e) {}
						
						if (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT) {
							scroller.setValueIsAdjusting(true);
							scroller.setValue(scrollPosition);
							scroller.setValueIsAdjusting(false);
						}
						validate();
					}
				});
			}
		}
		
		private void validateExpression() {
			boolean selected = true;
			String expression = this.editor.getSelectedText();
			if ((expression == null) || (expression.length() == 0)) {
				expression = this.editor.getText();
				selected = false;
			}
			String error = GPathParser.validatePath(expression);
			if (error == null) JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is valid."), "GPath Validation", JOptionPane.INFORMATION_MESSAGE);
			else JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
		}
		
		private void testExpression() {
			boolean selected = true;
			String expression = this.editor.getSelectedText();
			if ((expression == null) || (expression.length() == 0)) {
				expression = this.editor.getText();
				selected = false;
			}
			
			String error = GPathParser.validatePath(expression);
			if (error != null) JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "GPath Validation", JOptionPane.ERROR_MESSAGE);
			else try {
				
				expression = (((MarkupStepEditor) parent).getContextPath() + "[" + expression + "]");
				
				QueriableAnnotation startDoc = host.getStartDocument();
				if (startDoc != null) {
					Annotation[] startAnnotations = GPath.evaluatePath(startDoc, expression, null);
					
					QueriableAnnotation goldDoc = host.getGoldDocument();
					Annotation[] goldAnnotations = ((goldDoc == null) ? null : GPath.evaluatePath(goldDoc, expression, null));
					
					CriterionTestDialog ctd = new CriterionTestDialog(startAnnotations, goldAnnotations);
					ctd.setLocationRelativeTo(this);
					ctd.setVisible(true);
				}
			}
			catch (GPathException gpe) {
				JOptionPane.showMessageDialog(this, gpe.getMessage(), "GPath Expression Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		private void refreshDisplay() {
			final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
			final int scrollPosition = scroller.getValue();
			
			this.editor.getDocument().removeDocumentListener(this);
			this.editor.setText(GPath.explodePath(this.content, "  "));
			this.editor.getDocument().addDocumentListener(this);
			
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					int scrollbarWaitCounter = 0;
					while (scroller.getValueIsAdjusting() && (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT)) try {
						Thread.sleep(10);
						scrollbarWaitCounter ++;
					} catch (Exception e) {}
					
					if (scrollbarWaitCounter < MAX_SCROLLBAR_WAIT) {
						scroller.setValueIsAdjusting(true);
						scroller.setValue(scrollPosition);
						scroller.setValueIsAdjusting(false);
					}
					validate();
				}
			});
		}
		
		/** @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
		 */
		public void changedUpdate(DocumentEvent e) {
			//	attribute changes are not of interest for now
		}
		
		/** @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
		 */
		public void insertUpdate(DocumentEvent e) {
			this.expressionDirty = true;
			setDirty();
		}
		
		/** @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
		 */
		public void removeUpdate(DocumentEvent e) {
			this.expressionDirty = true;
			setDirty();
		}
		
		private class CriterionTestDialog extends JPanel {
			private JDialog dialog;
			CriterionTestDialog(Annotation[] startAnnotations, Annotation[] goldAnnotations) {
				super(new BorderLayout(), true);
				this.dialog = DialogFactory.produceDialog("Results Of Criterion", true);
				
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
	}
}
