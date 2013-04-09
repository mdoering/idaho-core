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
package de.uka.ipd.idaho.gamta.util.analyzerConfiguration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * @author sautter
 */
abstract class NameListConfigPanel extends AnalyzerConfigPanel {
	NameAttributeSet[] attributes;
	NameListPanel nameList;
	final String fileExtension;
	
	NameListConfigPanel(ConfigPanelDataProvider dataProvider, String fileExtension, NameAttributeSet[] attributes, String title, String toolTip, boolean editable) {
		super(dataProvider, title, toolTip);
		this.fileExtension = fileExtension;
		this.attributes = ((attributes == null) ? new NameAttributeSet[0] : attributes);
		this.nameList = new NameListPanel();
		
		this.add(this.nameList, BorderLayout.EAST);
		
		if (editable) {
			JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton button;
			button = new JButton("Create");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (createData())
						nameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Clone");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (cloneData())
						nameList.refresh();
				}
			});
			editButtons.add(button);
			button = new JButton("Delete");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (deleteData())
						nameList.refresh();
				}
			});
			editButtons.add(button);
			
			JButton[] ebs = getEditButtons();
			for (int b = 0; b < ebs.length; b++) {
				ebs[b].setBorder(BorderFactory.createRaisedBevelBorder());
				ebs[b].setPreferredSize(new Dimension(100, 21));
				editButtons.add(ebs[b]);
			}
			
			this.add(editButtons, BorderLayout.NORTH);
		}
	}
	
	JButton[] getEditButtons() {
		return null;
	}
	
	abstract void nameSelected(String name);
	
	abstract boolean createData();
	
	abstract boolean cloneData();
	
	private boolean deleteData() {
		String name = this.nameList.getSelectedName();
		if (!this.dataProvider.isDataAvailable(name))
			return false;
		else if (!this.dataProvider.isDataDeletable(name)) {
			JOptionPane.showMessageDialog(this, ("'" + name + "' is built-in and cannot be deleted."), "Cannot Delete", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else return (this.dataProvider.deleteData(name));
	}
	
	class NameListPanel extends JPanel {
		private StringVector names;
		
		private JTable nameList; 
		private JScrollPane nameListBox;
		
		NameListPanel() {
			super(new BorderLayout(), true);
			
			//	 initialize name list
			this.nameList = new JTable(new NameListModel());
			this.nameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.nameList.addMouseListener(new MouseAdapter() {
				int selectedRow = -1;
				public void mouseClicked(MouseEvent me) {
					int row = nameList.rowAtPoint(me.getPoint());
					if (row != selectedRow) {
						selectedRow = row;
						nameSelected(getSelectedName());
					}
				}
			});
			this.nameListBox = new JScrollPane(this.nameList);
			this.nameListBox.getVerticalScrollBar().setUnitIncrement(50);
			this.nameListBox.getVerticalScrollBar().setBlockIncrement(100);
			this.add(this.nameListBox, BorderLayout.CENTER);
			
			this.refresh();
		}
		
		String[] getNames() {
			return this.names.toStringArray();
		}
		
		void setSelectedName(String name) {
			int index = this.names.indexOf(name);
			this.nameList.setRowSelectionInterval(index, index);
		}
		
		String getSelectedName() {
			int index = this.nameList.getSelectedRow();
			return (((index < 0) || (this.names.size() <= index)) ? null : this.names.get(index));
		}
		
		void refresh() {
			this.names = this.readNameList();
			this.nameList.setModel(new NameListModel());
			for (int a = 0; a < attributes.length; a++)
				this.nameList.getColumnModel().getColumn(a).setMaxWidth(60);
			this.nameList.validate();
		}
		
		private String nameAt(int index) {
			return ((this.names == null) ? "" : this.names.get(index));
		}
		
		private int getNameCount() {
			return ((this.names == null) ? 0 : this.names.size());
		}
		
		private StringVector readNameList() {
			StringVector nameList = new StringVector();
			String[] names = dataProvider.getDataNames();
			for (int n = 0; n < names.length; n++) {
				if (names[n].endsWith(fileExtension))
					nameList.addElementIgnoreDuplicates(names[n]);
			}
			nameList.sortLexicographically(false, false);
			return nameList;
		}
		
		private class NameListModel implements TableModel {
			public int getColumnCount() {
				return attributes.length + 1;
			}
			public int getRowCount() {
				return getNameCount();
			}
			public Class getColumnClass(int columnIndex) {
				return ((columnIndex < attributes.length) ? Boolean.class : String.class);
			}
			public String getColumnName(int columnIndex) {
				if (columnIndex < attributes.length)
					return attributes[columnIndex].getLabel();
				else if (columnIndex == attributes.length)
					return "Name";
				else return null;
			}
			public Object getValueAt(int rowIndex, int columnIndex) {
				String name = nameAt(rowIndex);
				if (columnIndex < attributes.length)
					return new Boolean(attributes[columnIndex].contains(name));
				else if (columnIndex == attributes.length)
					return name;
				else return null;
			}
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return (columnIndex < attributes.length);
			}
			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
				if ((columnIndex < attributes.length) && (newValue instanceof Boolean)) {
					if (((Boolean) newValue).booleanValue())
						attributes[columnIndex].add(nameAt(rowIndex));
					else attributes[columnIndex].remove(nameAt(rowIndex));
				}
			}
			public void addTableModelListener(TableModelListener l) {}
			public void removeTableModelListener(TableModelListener l) {}
		}
	}
}
