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
package de.uka.ipd.idaho.gamta.util.swing;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A dialog widget for displaying a list of annotations. Depending on the
 * constructor arguments, the annotation list shows the specified annotations,
 * plus a checkbox next to each annotation if the showOnly constructor argument
 * is false or not specified.
 * 
 * @author sautter
 */
public class AnnotationDisplayDialog extends JDialog {
	
	private Annotation[] annotations;
	private AnnotationDisplayPanel annotationDisplay;
	
	private boolean isCommitted = false;
	
	/**	Constructor
	 * @param	host			the frame this dialog is modal to
	 * @param	title			the title for the dialog
	 * @param	annotations		the Annotations to display
	 */
	public AnnotationDisplayDialog(Frame host, String title, Annotation[] annotations) {
		this(host, title, annotations, false);
	}
	
	/**	Constructor
	 * @param	host			the frame this dialog is modal to
	 * @param	title			the title for the dialog
	 * @param	annotations		the Annotations to display
	 * @param	showOnly		if set to true, the dialog can only be cancelled
	 */
	public AnnotationDisplayDialog(Frame host, String title, Annotation[] annotations, boolean showOnly) {
		super(host, title, true);
		this.annotations = annotations;
		this.init(showOnly, host);
	}
	
	/**	Constructor
	 * @param	host			the dialog this dialog is modal to
	 * @param	title			the title for the dialog
	 * @param	annotations		the Annotations to display
	 */
	public AnnotationDisplayDialog(Dialog host, String title, Annotation[] annotations) {
		this(host, title, annotations, false);
	}
	
	/**	Constructor
	 * @param	host			the dialog this dialog is modal to
	 * @param	title			the title for the dialog
	 * @param	annotations		the Annotations to display
	 * @param	showOnly		if set to true, the dialog can only be cancelled
	 */
	public AnnotationDisplayDialog(Dialog host, String title, Annotation[] annotations, boolean showOnly) {
		super(host, title, true);
		this.annotations = annotations;
		this.init(showOnly, host);
	}
	
	private void init(boolean showOnly, Component host) {
		
		JPanel mainButtonPanel = new JPanel();
		mainButtonPanel.setLayout(new FlowLayout());
		
		//	initialize main buttons
		if (!showOnly) {
			JButton commitButton = new JButton("Annotate");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					isCommitted = true;
					dispose();
				}
			});
			mainButtonPanel.add(commitButton);
		}
		
		JButton abortButton = new JButton(showOnly ? "OK" : "Cancel");
		abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
		abortButton.setPreferredSize(new Dimension(100, 21));
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		mainButtonPanel.add(abortButton);
		
		//	put the whole stuff together
		this.getContentPane().setLayout(new BorderLayout());
		
		this.annotationDisplay = new AnnotationDisplayPanel(this.annotations, !showOnly);
		this.getContentPane().add(this.annotationDisplay, BorderLayout.CENTER);
		this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
		
		this.setResizable(true);
		//	TODO: compute size dependent on annotations.length
		this.setSize(new Dimension(300, 500));
		this.setLocationRelativeTo(host);
	}
	
	/**	@return true if and only if the dialog was committed
	 */
	public boolean isCommitted() {
		return this.isCommitted;
	}
	
	/**	@return	the selected Annotation
	 */
	public Annotation[] getSelectedAnnotations() {
		return this.annotationDisplay.getSelectedAnnotations();
	}
	private class AnnotationDisplayPanel extends JPanel {
		private Annotation[] annotations;
		private boolean[] selectors;
		private JTable annotationTable;
		
		private AnnotationDisplayPanel(Annotation[] annotations, boolean selectable) {
			super(new BorderLayout(), true);
			
			this.setBorder(BorderFactory.createEtchedBorder());
			
			this.annotations = annotations;
			this.selectors = new boolean[annotations.length];
			for (int s = 0; s < this.selectors.length; s++) this.selectors[s] = true;
			
			this.annotationTable = new JTable();
			
			if (selectable) {
				this.annotationTable.setDefaultRenderer(Object.class, new TooltipAwareTableRenderer(4));
				this.annotationTable.setModel(new SelectableAnnotationTableModel(this.selectors, annotations));
				this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(60);
				this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(120);
				this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(60);
				this.annotationTable.getColumnModel().getColumn(3).setMaxWidth(60);
				
				JPanel buttonPanel = new JPanel();
				JButton selectAllButton = new JButton("Select All");
				selectAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
				selectAllButton.setPreferredSize(new Dimension(100, 21));
				selectAllButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						selectAll();
					}
				});
				buttonPanel.add(selectAllButton);
				JButton selectNoneButton = new JButton("Select None");
				selectNoneButton.setBorder(BorderFactory.createRaisedBevelBorder());
				selectNoneButton.setPreferredSize(new Dimension(100, 21));
				selectNoneButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						selectNone();
					}
				});
				buttonPanel.add(selectNoneButton);
				this.add(buttonPanel, BorderLayout.SOUTH);
				
			} else {
				this.annotationTable.setDefaultRenderer(Object.class, new TooltipAwareTableRenderer(3));
				this.annotationTable.setModel(new AnnotationTableModel(annotations));
				this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(120);
				this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(60);
				this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(60);
			}
			
			JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
			this.add(annotationTableBox, BorderLayout.CENTER);
		}
		
		private void selectAll() {
			for (int s = 0; s < this.selectors.length; s++) this.selectors[s] = true;
			this.annotationTable.repaint();
			this.validate();
		}
		
		private void selectNone() {
			for (int s = 0; s < this.selectors.length; s++) this.selectors[s] = false;
			this.annotationTable.repaint();
			this.validate();
		}
		
		private Annotation[] getSelectedAnnotations() {
			ArrayList collector = new ArrayList();
			for (int a = 0; a < this.annotations.length; a++)
				if (this.selectors[a]) collector.add(this.annotations[a]);
			return ((Annotation[]) collector.toArray(new Annotation[collector.size()]));
		}
		
		private class AnnotationTableModel implements TableModel {
			private Annotation[] annotations;
			
			private AnnotationTableModel(Annotation[] annotations) {
				this.annotations = annotations;
			}
			
			/** @see javax.swing.table.TableModel#getColumnClass(int)
			 */
			public Class getColumnClass(int columnIndex) {
				return String.class;
			}
			
			/** @see javax.swing.table.TableModel#getColumnCount()
			 */
			public int getColumnCount() {
				return 4;
			}
			
			/** @see javax.swing.table.TableModel#getColumnName(int)
			 */
			public String getColumnName(int columnIndex) {
				if (columnIndex == 0) return "Type";
				if (columnIndex == 1) return "Start";
				if (columnIndex == 2) return "Size";
				if (columnIndex == 3) return "Value";
				return null;
			}
			
			/** @see javax.swing.table.TableModel#getRowCount()
			 */
			public int getRowCount() {
				return this.annotations.length;
			}
			
			/** @see javax.swing.table.TableModel#getValueAt(int, int)
			 */
			public Object getValueAt(int rowIndex, int columnIndex) {
				Annotation a = this.annotations[rowIndex];
				if (columnIndex == 0) return a.getType();
				if (columnIndex == 1) return "" + a.getStartIndex();
				if (columnIndex == 2) return "" + a.size();
				if (columnIndex == 3) return a.getValue();
				return null;
			}
			
			/** @see javax.swing.table.TableModel#isCellEditable(int, int)
			 */
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			
			/** @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void addTableModelListener(TableModelListener l) {}
			
			/** @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void removeTableModelListener(TableModelListener l) {}
			
			/** @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
			 */
			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
		}

		private class SelectableAnnotationTableModel implements TableModel {
			private boolean[] selectors;
			private Annotation[] annotations;
			
			private SelectableAnnotationTableModel(boolean[] selectors, Annotation[] annotations) {
				this.selectors = selectors;
				this.annotations = annotations;
			}
			
			/** @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void addTableModelListener(TableModelListener l) {}
			
			/** @see javax.swing.table.TableModel#getColumnClass(int)
			 */
			public Class getColumnClass(int columnIndex) {
				if (columnIndex == 0) return Boolean.class;
				else return String.class;
			}
			
			/** @see javax.swing.table.TableModel#getColumnCount()
			 */
			public int getColumnCount() {
				return 5;
			}
			
			/** @see javax.swing.table.TableModel#getColumnName(int)
			 */
			public String getColumnName(int columnIndex) {
				if (columnIndex == 0) return "Select";
				if (columnIndex == 1) return "Type";
				if (columnIndex == 2) return "Start";
				if (columnIndex == 3) return "Size";
				if (columnIndex == 4) return "Value";
				return null;
			}
			
			/** @see javax.swing.table.TableModel#getRowCount()
			 */
			public int getRowCount() {
				return this.annotations.length;
			}
			
			/** @see javax.swing.table.TableModel#getValueAt(int, int)
			 */
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (columnIndex == 0) return new Boolean(this.selectors[rowIndex]);
				Annotation a = this.annotations[rowIndex];
				if (columnIndex == 1) return a.getType();
				if (columnIndex == 2) return "" + a.getStartIndex();
				if (columnIndex == 3) return "" + a.size();
				if (columnIndex == 4) return a.getValue();
				return null;
			}
			
			/** @see javax.swing.table.TableModel#isCellEditable(int, int)
			 */
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return (columnIndex == 0);
			}
			
			/** @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void removeTableModelListener(TableModelListener l) {}
			
			/** @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
			 */
			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
				if (columnIndex == 0) this.selectors[rowIndex] = ((Boolean) newValue).booleanValue();
			}
		}

		private class TooltipAwareTableRenderer extends DefaultTableCellRenderer {
			private HashSet tooltipColumns = new HashSet();
			private TooltipAwareTableRenderer(int tooltipColumn) {
				this.tooltipColumns.add("" + tooltipColumn);
			}
			private TooltipAwareTableRenderer(int[] tooltipColumns) {
				for (int c = 0; c < tooltipColumns.length; c++)
					this.tooltipColumns.add("" + tooltipColumns[c]);
			}
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
				JComponent component = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				if (this.tooltipColumns.contains("" + row))
					component.setToolTipText(this.produceTooltipText(Gamta.INNER_PUNCTUATION_TOKENIZER.tokenize(value.toString())));
				return component;
			}
			private String produceTooltipText(TokenSequence tokens) {
				if (tokens.length() < 100) return tokens.toString();
				
				StringVector lines = new StringVector();
				int startToken = 0;
				int lineLength = 0;
				Token lastToken = null;
				
				for (int t = 0; t < tokens.size(); t++) {
					Token token = tokens.tokenAt(t);
					lineLength += token.length();
					if (lineLength > 100) {
						lines.addElement(tokens.getSubsequence(startToken, (t - startToken + 1)).toString());
						startToken = (t + 1);
						lineLength = 0;
					} else if (Gamta.insertSpace(lastToken, token)) lineLength++;
				}
				if (startToken < tokens.size())
					lines.addElement(tokens.getSubsequence(startToken, (tokens.size() - startToken)).toString());
				
				return ("<HTML>" + lines.concatStrings("<BR>") + "</HTML>");
			}
		}
	}
}
