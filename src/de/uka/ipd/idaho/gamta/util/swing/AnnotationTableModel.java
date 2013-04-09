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


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;

/**
 * TableModel implementation for AnnotationTable
 * 
 * @author sautter
 */
public class AnnotationTableModel extends AbstractTableModel {
	
	/**
	 * An AnnotationFilter extracts the Annotations to display from an arbitrary
	 * QueriableAnnotation, eg using their type or a GPath expression.
	 * 
	 * @author sautter
	 */
	public static interface AnnotationFilter {
		
		/**
		 * Obtain the Annotations to display from a QueriableAnnotation
		 * @param data the QueriableAnnotation to work on
		 * @return an array holding the Annotations matching the filter
		 */
		public abstract Annotation[] getMatches(QueriableAnnotation data);
	}
	
	private QueriableAnnotation context;
	private AnnotationFilter filter;
	
	private Annotation[] annotations;
	private boolean[] selectors;
	private String selectColumnName;
	
	private boolean useMutableAnnotations = false;
	
	/**
	 * Constructor producing a table model for plainly displaying the
	 * Annotations in the specified array.
	 * @param annotations the array holding the Annotations to display in the
	 *            surrounding table
	 */
	public AnnotationTableModel(Annotation[] annotations) {
		this(annotations, null);
	}
	
	/**
	 * Constructor producing a table model for displaying the Annotations in the
	 * specified array and selecting them using checkboxes.
	 * @param annotations the array holding the Annotations to display in the
	 *            surrounding table
	 * @param selectColumnName the column name for a leading column of
	 *            checkboxes for selecting individual Annotations
	 */
	public AnnotationTableModel(Annotation[] annotations, String selectColumnName) {
		this.annotations = annotations;
		
		if (selectColumnName != null) {
			this.selectColumnName = selectColumnName;
			this.selectors = new boolean[this.annotations.length];
			for (int s = 0; s < this.selectors.length; s++)
				this.selectors[s] = false;
		}
		
		if (MutableAnnotation.class.isAssignableFrom(this.annotations.getClass().getComponentType()))
			this.useMutableAnnotations = true;
		else {
			this.useMutableAnnotations = true;
			for (int a = 0; a < this.annotations.length; a++)
				this.useMutableAnnotations = (this.useMutableAnnotations && (this.annotations [a] instanceof MutableAnnotation));
		}
	}
	
	/**
	 * Constructor producing a table model for displaying Annotations retrieved
	 * from a context QueriableAnnotation using an AnnotationFilter
	 * @param context the QueriableAnnotation from which to retrieve the
	 *            Annotations to display
	 * @param filter the filter for retrieving the Annotations to display from
	 *            the context QueriableAnnotation
	 */
	public AnnotationTableModel(QueriableAnnotation context, AnnotationFilter filter) {
		this(context, filter, null);
	}
	
	/**
	 * Constructor producing a table model for displaying Annotations retrieved
	 * from a context QueriableAnnotation using an AnnotationFilter and
	 * selecting them using checkboxes.
	 * @param context the QueriableAnnotation from which to retrieve the
	 *            Annotations to display
	 * @param filter the filter for retrieving the Annotations to display from
	 *            the context QueriableAnnotation
	 * @param selectColumnName the column name for a leading column of
	 *            checkboxes for selecting individual Annotations
	 */
	public AnnotationTableModel(QueriableAnnotation context, AnnotationFilter filter, String selectColumnName) {
		this.context = context;
		this.filter = filter;
		
		this.useMutableAnnotations = (this.context instanceof MutableAnnotation);
		
		Annotation[] matches = this.filter.getMatches(this.context);
		if (this.useMutableAnnotations && !MutableAnnotation.class.isAssignableFrom(matches.getClass().getComponentType())) {
			HashSet matchIdSet = new HashSet();
			for (int m = 0; m < matches.length; m++)
				matchIdSet.add(matches[m].getAnnotationID());
			
			LinkedList mutableMatchList = new LinkedList();
			MutableAnnotation[] mutableAnnotations = ((MutableAnnotation) this.context).getMutableAnnotations();
			for (int m = 0; m < mutableAnnotations.length; m++)
				if (matchIdSet.contains(mutableAnnotations[m].getAnnotationID()))
					mutableMatchList.add(mutableAnnotations[m]);
			
			this.annotations = ((Annotation[]) mutableMatchList.toArray(new MutableAnnotation[mutableMatchList.size()]));
		}
		else this.annotations = matches;
		
		if (selectColumnName != null) {
			this.selectColumnName = selectColumnName;
			this.selectors = new boolean[this.annotations.length];
			for (int s = 0; s < this.selectors.length; s++)
				this.selectors[s] = false;
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return ((this.selectors == null) ? 4 : 5);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	public Class getColumnClass(int columnIndex) {
		return (((this.selectors == null) || (columnIndex != 0)) ? String.class : Boolean.class);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	public String getColumnName(int columnIndex) {
		if (this.selectors != null) {
			if (columnIndex == 0) return this.selectColumnName;
			else columnIndex--;
		}
		if (columnIndex == 0) return "Type";
		else if (columnIndex == 1) return "Start";
		else if (columnIndex == 2) return "Size";
		else if (columnIndex == 3) return "Value";
		else return null;
	}
	
	/**
	 * Retrieve the inital width for the colunmIndex-th column (values <= 0 mean
	 * no setting, which will result in the column having its width assigned
	 * dynamically)
	 * @param columnIndex the index of the column
	 * @return the width of the column at the specified index
	 */
	public int getColumnWidth(int columnIndex) {
		if (this.selectors != null) {
			if (columnIndex == 0) return 60;
			else columnIndex--;
		}
		if (columnIndex == 0) return 120;
		else if (columnIndex == 1) return 60;
		else if (columnIndex == 2) return 60;
		else return 0;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
		return this.annotations.length;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (this.selectors != null) {
			if (columnIndex == 0) return new Boolean(this.selectors[rowIndex]);
			else columnIndex--;
		}
		Annotation annotation = this.annotations[rowIndex];
		if (columnIndex == 0) return annotation.getType();
		else if (columnIndex == 1) return "" + annotation.getStartIndex();
		else if (columnIndex == 2) return "" + annotation.size();
		else if (columnIndex == 3) return annotation.getValue();
		else return null;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
	 */
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return ((this.selectors!= null) && (columnIndex == 0));
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
	 */
	public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
		if (columnIndex == 0) this.selectors[rowIndex] = ((Boolean) newValue).booleanValue();
	}
	
	/**
	 * Retrieve the QueriableAnnotation the Annotations in this table model
	 * refer to. For the constructors that specify an array of Annotations, this
	 * method returns null. If the context is a MutableAnnotation, the return
	 * value of this method is as well.
	 * @return the QueriableAnnotation the Annotations in this table model refer
	 *         to
	 */
	public QueriableAnnotation getContext() {
		return this.context;
	}
	
	/**
	 * Retrieve the Annotation in the rowIndex-th row. If this table model is
	 * constructed from MutableAnnotations, this method will return a
	 * MutableAnnotation.
	 * @param rowIndex the row to retrieve the Annotation for
	 * @return the Annotation in row rowIndex
	 */
	public Annotation getAnnotation(int rowIndex) {
		return this.annotations[rowIndex];
	}
	
	/**
	 * @return true if the Annotations returned by the getContext() and
	 *         getAnnotation(int) methods are guarantied to be
	 *         MutableAnnotations.
	 */
	boolean usesMutableAnnotations() {
		return this.useMutableAnnotations;
	}
	
	/**
	 * Check if the Annotation in a given row is selected (the checkbox is
	 * checked). If the table model does not support selecting Annotations, this
	 * method always return false.
	 * @param rowIndex the index of the row to check
	 * @return true if the table model supports selection, and the checkbox in
	 *         the specified row is checked
	 */
	public boolean isAnnotationSelected(int rowIndex) {
		return ((this.selectors != null) && this.selectors[rowIndex]);
	}
	
	/**
	 * Obtain a panel with extra inputs (buttons, checkboxes, etc) to be
	 * displayed below the actual annotation table
	 * @return a panel with extra inputs
	 */
	public JPanel getExtensionPanel() {
		if (this.selectors == null) return null;
		else {
			JPanel buttonPanel = new JPanel();
			JButton selectAllButton = new JButton("Select All");
			selectAllButton.setBorder(BorderFactory.createRaisedBevelBorder());
			selectAllButton.setPreferredSize(new Dimension(100, 21));
			selectAllButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for (int s = 0; s < selectors.length; s++)
						selectors[s] = true;
					fireTableRowsUpdated(0, selectors.length);
				}
			});
			buttonPanel.add(selectAllButton);
			JButton selectNoneButton = new JButton("Select None");
			selectNoneButton.setBorder(BorderFactory.createRaisedBevelBorder());
			selectNoneButton.setPreferredSize(new Dimension(100, 21));
			selectNoneButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					for (int s = 0; s < selectors.length; s++)
						selectors[s] = false;
					fireTableRowsUpdated(0, selectors.length);
				}
			});
			buttonPanel.add(selectNoneButton);
			return buttonPanel;
		}
	}
	
	/**
	 * This method is called by the surrounding AnnotationTable if an
	 * AnnotationAction was performed, so filter-based models can refresh to
	 * adjust to circumstances where one or more annotations are new to pass or
	 * fail the filter.
	 */
	void refreshAnnotations() {
		if (this.context != null) {
			HashSet selected = new HashSet();
			
			if (this.selectors != null)
				for (int s = 0; s < this.selectors.length; s++)
					if (this.selectors[s])
						selected.add(this.annotations[s].getAnnotationID());
			
			Annotation[] matches = this.filter.getMatches(this.context);
			if (this.useMutableAnnotations && !MutableAnnotation.class.isAssignableFrom(matches.getClass().getComponentType())) {
				HashSet matchIdSet = new HashSet();
				for (int m = 0; m < matches.length; m++)
					matchIdSet.add(matches[m].getAnnotationID());
				
				LinkedList mutableMatchList = new LinkedList();
				MutableAnnotation[] mutableAnnotations = ((MutableAnnotation) this.context).getMutableAnnotations();
				for (int m = 0; m < mutableAnnotations.length; m++)
					if (matchIdSet.contains(mutableAnnotations[m].getAnnotationID()))
						mutableMatchList.add(mutableAnnotations[m]);
				
				this.annotations = ((Annotation[]) mutableMatchList.toArray(new MutableAnnotation[mutableMatchList.size()]));
			}
			else this.annotations = matches;
			
			if (this.selectors != null) {
				this.selectors = new boolean[this.annotations.length];
				for (int s = 0; s < this.selectors.length; s++)
					this.selectors[s] = selected.contains(this.annotations[s].getAnnotationID());
			}
			
			this.fireTableDataChanged();
		}
	}
}
