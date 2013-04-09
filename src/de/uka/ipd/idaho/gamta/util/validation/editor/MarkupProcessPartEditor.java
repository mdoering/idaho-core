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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Writer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.gamta.util.validation.ProcessPart;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.Validator;

/**
 * Abstract super class of all editor panels for a markup process or parts of
 * it.
 * 
 * @author sautter
 */
public abstract class MarkupProcessPartEditor extends JPanel {
	private boolean dirty = false;
	
	protected MarkupProcessEditorPanel host;
	protected MarkupProcessPartEditor parent;
	
	protected BasicAttributesPanel attributes;
	protected JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
	
	protected MarkupProcessPartEditor(MarkupProcessEditorPanel host, MarkupProcessPartEditor parent, ProcessPart processPart) {
		super(new BorderLayout(), true);
		this.host = host;
		this.parent = parent;
		
		this.attributes = new BasicAttributesPanel(processPart, this);
		
		MarkupProcessEditorExtension extension = this.host.getExtension();
		MarkupProcessPartEditorExtension extensionPanel = ((extension == null) ? null : extension.getExtension(this));
		
		JPanel detailPanel = new JPanel(new BorderLayout());
		if (extensionPanel == null) {
			detailPanel.add(this.attributes, BorderLayout.CENTER);
			detailPanel.add(this.buttons, BorderLayout.SOUTH);
		}
		else {
			detailPanel.add(this.attributes, BorderLayout.NORTH);
			detailPanel.add(extensionPanel, BorderLayout.CENTER);
			detailPanel.add(this.buttons, BorderLayout.SOUTH);
		}
		this.add(detailPanel, BorderLayout.NORTH);
	}
	
	protected void testProcessPart() {
		QueriableAnnotation startDoc = this.host.getStartDocument();
		if (startDoc != null) {
			Validator validator = this.getProcessPart().getValidator();
			ValidationResult startVr = validator.validate(startDoc);
			String[] startDetails = startVr.getDetailDescriptions();
			
			QueriableAnnotation goldDoc = this.host.getGoldDocument();
			String[] goldDetails = null;
			if (goldDoc != null) {
				ValidationResult goldVr = validator.validate(goldDoc);
				goldDetails = goldVr.getDetailDescriptions();
			}
			
			ValidationResultDialog ctd = new ValidationResultDialog(startDetails, goldDetails);
			ctd.setLocationRelativeTo(this);
			ctd.setVisible(true);
		}
	}
	
	private class ValidationResultDialog extends JPanel {
		JDialog dialog;
		ValidationResultDialog(String[] startDocResult, String[] goldDocResult) {
			super(new BorderLayout(), true);
			this.dialog = DialogFactory.produceDialog("Validation Result", true);
			
			JTable startResultList = new JTable(new ValidationResultTableModel(startDocResult));
			startResultList.setShowHorizontalLines(true);
			startResultList.setShowVerticalLines(false);
			startResultList.setTableHeader(null);
			JScrollPane startResultListBox = new JScrollPane(startResultList);
			startResultListBox.setViewportBorder(BorderFactory.createLoweredBevelBorder());
			
			if (goldDocResult == null) this.add(startResultListBox, BorderLayout.CENTER);
			else {
				JTable goldResultList = new JTable(new ValidationResultTableModel(goldDocResult));
				goldResultList.setShowHorizontalLines(true);
				goldResultList.setShowVerticalLines(false);
				goldResultList.setTableHeader(null);
				JScrollPane goldResultListBox = new JScrollPane(goldResultList);
				goldResultListBox.setViewportBorder(BorderFactory.createLoweredBevelBorder());
				
				JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, startResultListBox, goldResultListBox);
				split.setDividerLocation(.5);
				split.setResizeWeight(.5);
				split.setEnabled(true);
				this.add(split, BorderLayout.CENTER);
			}
			
			JButton okButton = new JButton("OK");
			okButton.setBorder(BorderFactory.createRaisedBevelBorder());
			okButton.setPreferredSize(new Dimension(100, 21));
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dialog.dispose();
				}
			});
			this.add(okButton, BorderLayout.SOUTH);
			
			this.dialog.getContentPane().setLayout(new BorderLayout());
			this.dialog.getContentPane().add(this, BorderLayout.CENTER);
			this.dialog.setSize(((goldDocResult == null) ? 400 : 700), 500);
		}
		
		void setLocationRelativeTo(Component comp) {
			this.dialog.setLocationRelativeTo(comp);
		}
		
		public void setVisible(boolean visible) {
			this.dialog.setVisible(visible);
		}
		
		private class ValidationResultTableModel implements TableModel {
			private String[] resultDetails;
			ValidationResultTableModel(String[] resultDetails) {
				this.resultDetails = resultDetails;
			}
			public int getColumnCount() {
				return 1;
			}
			public int getRowCount() {
				return this.resultDetails.length;
			}
			public String getColumnName(int columnIndex) {
				if (columnIndex == 0) return "Document Title";
				return null;
			}
			public Class getColumnClass(int columnIndex) {
				return String.class;
			}
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (columnIndex == 0) return this.resultDetails[rowIndex];
				return null;
			}
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
			
			public void addTableModelListener(TableModelListener l) {}
			public void removeTableModelListener(TableModelListener l) {}
		}
	}
	
	/**
	 * Notify the process part editor that its content has been modified. This
	 * method propagates the notification to the parent process part editor (if
	 * any).
	 */
	protected void setDirty() {
		this.dirty = true;
		if (this.parent != null)
			this.parent.setDirty();
	}
	
	/**
	 * @return true if the content of this process part editor or one of its sub
	 *         editors was modified, false otherwise
	 */
	public boolean isDirty() {
		return this.dirty;
	}
	
	/**	write an XML definition of this ProcessPartEditor's content to a Writer 
	 * @param	writer	the Writer to write to
	 */
	public void writeContent(Writer writer) throws IOException {
		ProcessPart pp = this.getProcessPart();
		pp.writeXml(writer);
		writer.flush();
	}
	
	/**
	 * @return the fully qualified name, i.e., the name currently entered in the
	 *         name field, prefixed with the names of the parent parts of this
	 *         part
	 */
	public String getProcessPartFullName() {
		return ((this.parent == null) ? "" : (this.parent.getProcessPartFullName() + ".")) + this.getProcessPartName();
	}
	
	/**
	 * @return the name currently entered in the name field
	 */
	public String getProcessPartName() {
		return this.attributes.nameField.getText();
	}
	
	/**
	 * @return the label text currently entered in the label field
	 */
	public String getProcessPartLabel() {
		return this.attributes.labelField.getText();
	}
	
	/**
	 * @return the description text currently entered in the description field
	 */
	public String getProcessPartDescription() {
		return this.attributes.descriptionField.getText();
	}
	
	/**
	 * @return the error description text currently entered in the error description field
	 */
	public String getProcessPartErrorDescription() {
		return this.attributes.errorDescriptionField.getText();
	}
	
	/**
	 * @return a ProcessPart object holding the definition of the part of the
	 *         markup process accourding to this ProcessPartEditor's current
	 *         content
	 */
	public abstract ProcessPart getProcessPart();
	
	
	/**
	 * Input panel for the basid attributes of a markup process part, namely name,
	 * label, description, and error description
	 * 
	 * @author sautter
	 */
	protected static class BasicAttributesPanel extends JPanel {
		
		JTextField nameField = new JTextField();
		JTextField labelField = new JTextField();
		JTextField descriptionField = new JTextField();
		JTextField errorDescriptionField = new JTextField();
		
		/** Constructor
		 */
		BasicAttributesPanel(ProcessPart processPart, final MarkupProcessPartEditor parent) {
			super(new GridBagLayout(), true);
			
			this.setBorder(BorderFactory.createEtchedBorder());
			
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			this.nameField.setText(processPart.getName());
			this.nameField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent ke) {
					parent.setDirty();
				}
			});
			this.labelField.setBorder(BorderFactory.createLoweredBevelBorder());
			this.labelField.setText(processPart.getLabel());
			this.labelField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent ke) {
					parent.setDirty();
				}
			});
			this.descriptionField.setBorder(BorderFactory.createLoweredBevelBorder());
			this.descriptionField.setText(processPart.getDescription());
			this.descriptionField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent ke) {
					parent.setDirty();
				}
			});
			this.errorDescriptionField.setBorder(BorderFactory.createLoweredBevelBorder());
			this.errorDescriptionField.setText(processPart.getErrorDescription());
			this.errorDescriptionField.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent ke) {
					parent.setDirty();
				}
			});
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 2;
			gbc.insets.bottom = 2;
			gbc.insets.left = 5;
			gbc.insets.right = 5;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weighty = 0;
			gbc.gridwidth = 1;
			gbc.gridheight = 1;
			gbc.gridx = 0;
			gbc.gridy = 0;
			
			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("Name", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 1;
			this.add(this.nameField, gbc.clone());
			gbc.gridx = 2;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("Label", JLabel.LEFT), gbc.clone());
			gbc.gridx = 3;
			gbc.weightx = 1;
			gbc.gridwidth = 1;
			this.add(this.labelField, gbc.clone());
			gbc.gridy ++;

			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("Description", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 3;
			this.add(this.descriptionField, gbc.clone());
			gbc.gridy ++;

			gbc.gridx = 0;
			gbc.weightx = 0;
			gbc.gridwidth = 1;
			this.add(new JLabel("Error Description", JLabel.LEFT), gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			gbc.gridwidth = 3;
			this.add(this.errorDescriptionField, gbc.clone());
			gbc.gridy ++;
		}
	}
}
