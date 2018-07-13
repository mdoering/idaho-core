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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationListener;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationTableModel.AnnotationFilter;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A GUI widget displaying Annotations in a tabular fashion, optionally with a
 * check box for selecting individual Annotations. The actual JTable used for
 * displaying the content annotations resides in BorderLayout.CENTER position of
 * this panel. Users are free to add additional panels to the
 * BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.WEST and
 * BorderLayout.SOUTH positions. Note, however, that a panel with selector
 * buttons resides in BorderLayout.SOUTH position if the selectable parameter of
 * the constructor is true.
 * 
 * @author sautter
 */
public class AnnotationTable extends JPanel {
	
	/**
	 * Interface for a single option to (conditionally) appear in the
	 * AnnotationTable's context menu.
	 * 
	 * @author sautter
	 */
	public static interface ContextMenuOption {
		
		/**
		 * Retrieve a JMenuItem to make this option accessible for the current
		 * selection of annotations in the table. For this method, all argument
		 * Annotations are read-only, so any attempt of a modification will
		 * result in an exception being thrown. ActionListeners registered to
		 * the JMenuItems returned by this method shoud perform some action by
		 * injecting an AnnotationAction to the target AnnotationTable's
		 * performAction() method. This callback mechanism is necessary for the
		 * AnnotationTable to have the control over the actions being performed
		 * (preparation -> actual action -> post-processing).<br>
		 * Note that this method may also return a JMenu containing several
		 * JMenuItems, which will appear as a sub menu of the context menu
		 * displayed. This allows for one ContextMenuOption to providing several
		 * actual options at once, e.g. for different annotation types present
		 * in the table content.
		 * @param data the content of the editor, thus the annotation to work
		 *            on.
		 * @param selection the Annotation in the table row that was clicked on
		 * @param selected an array holding the Annotations currently selected
		 *            in the annotation table
		 * @param table the AnnotationTable showing the menu, thus the
		 *            component to call back for performing the actual action
		 *            via an AnnotationAction object.
		 * @return a JMenuItem making this option accessible for the current
		 *         selection or annotations, or null, if this option does not
		 *         want to be accessible for the current selection
		 */
		public abstract JMenuItem getMenuItem(QueriableAnnotation data, Annotation selection, Annotation[] selected, AnnotationTable table);
	}
	
	/**
	 * Interface for a single keyboard shortcut to be available in the
	 * AnnotationTable. Note that all shortcuts of this category must be
	 * invoked using 'Ctrl' or 'Alt', plus optionally 'Shift', plus a custom key
	 * character. The key characters are case insensitive and have to be from
	 * the range a-z/A-Z or 0-9.<br>
	 * Consequently, either the controlDown() or the altDown() method has to
	 * return true. There is one exception to this rule, however: If the
	 * textEditable property of the host AnnotationTable is false, using
	 * shortcuts that invoked plainly by means of their key character is
	 * permissible, since basic text input is disabled. Having both
	 * controlDown() and altDown() return true is prohibited, since 'Ctrl-Alt'
	 * is used to call operation system native functions on some platforms. In
	 * addition, 'Ctrl-Shift' in combination with a number key (0-9) may also be
	 * mapped to operation system functions on some platforms and thus may not
	 * be able to invoke a shortcut on the AnnotationTable.<br>
	 * If shiftDown() returns false, the invocation of this shortcut is
	 * insensitive to the 'Shift' key, and the argument boolean of the
	 * performAction() method can be true or false. Consequently, the actual
	 * action can react dynamically to the state of the 'Shift' key. If the
	 * shiftDown() method returns true, in turn, the shortcut can be invoked
	 * only with the 'Shift' key pressed, and the argument boolean of the
	 * performAction() method is always true. If two shortcut actions are
	 * registered to the same key, with the same combination of 'Ctrl' and
	 * 'Alt', and one requires 'Shift' while the other does not, the former is
	 * invoked because its key combination is more specific.<br>
	 * Note that it is not a good idea to have all three of controlDown(),
	 * altDown() and shiftDown() return true, since then a user has to press all
	 * three keys, plus the actual key character.<br>
	 * 
	 * @author sautter
	 */
	public static abstract class Shortcut {
		
		private static final String CONTROL_KEY_PREFIX = "Ctrl";
		private static final String ALT_KEY_PREFIX = "Alt";
		private static final String SHIFT_KEY_PREFIX = "Shift";
		
		private final char keyChar;
		
		/**
		 * Constructor
		 * @param keyChar the key character identifying this shortcut (case
		 *            insensitive), must be from the range a-z/A-Z or 0-9. Other
		 *            characters will throw an IllegalArgumentException.
		 */
		protected Shortcut(char keyChar) {
			char kc = Character.toUpperCase(keyChar);
			if (('A' <= kc) && (kc <= 'Z'))
				this.keyChar = keyChar;
			else if (('0' <= kc) && (kc <= '9'))
				this.keyChar = keyChar;
			else throw new IllegalArgumentException("The key char of a shortcut must be be from the range a-z/A-Z or 0-9");
		}

		/**
		 * Indicates if invoking this shortcut requires the 'Ctrl' key
		 * @return true if the 'Ctrl' key is part of the key combination for
		 *         this shortcut
		 */
		protected abstract boolean controlDown();
		
		/**
		 * Indicates if invoking this shortcut requires the 'Alt' key
		 * @return true if the 'Alt' key is part of the key combination for
		 *         this shortcut
		 */
		protected abstract boolean altDown();
		
		/**
		 * Indicates if invoking this shortcut requires the 'Shift' key
		 * @return true if the 'Shift' key is part of the key combination for
		 *         this shortcut
		 */
		protected abstract boolean shiftDown();
		
		private final String getShortcutIdentifier() {
			
			//	produce 'Ctrl'/'Alt' part of key
			String ctrl = (this.controlDown() ? CONTROL_KEY_PREFIX : "");
			String alt = (this.altDown() ? ALT_KEY_PREFIX : "");
			
			//	catch shortcuts using both of 'Ctrl' and 'Alt', and ones using neither if text is editable
			int ctrlAltLen = (ctrl.length() + alt.length());
			if (ctrlAltLen > Math.max(ctrl.length(), alt.length())) return null;
			
			//	produce 'Shift' part of key
			String shift = (this.shiftDown() ? SHIFT_KEY_PREFIX : "");
			
			//	return key
			return (ctrl + alt + shift + this.keyChar);
		}
		
		/**
		 * Retrieve an AnnotationAction to execute on behalf of this shortcut.
		 * For this method, all argument Annotations are read-only, so any
		 * attempt of a modification will result in an exception being thrown.
		 * @param shiftDown was the 'Shift' key pressed with the key combination
		 *            invoking this shortcut? (if the shiftDown() method returns
		 *            true, this argument is always true. Otherwise, it can be
		 *            true or false, allowing the shortcut to react to the
		 *            'Shift' key dynamically)
		 * @param data the content of the editor, thus the annotation to work
		 *            on.
		 * @param selection the Annotation focused in the table (always null here)
		 * @param selected an array holding the Annotations currently selected
		 *            in the annotation table
		 * @return the annotation action to perform on behalf of the shortcut,
		 *         or null, if the shortcut is not applicable in the context of
		 *         the current selection
		 */
		public abstract AnnotationAction getAction(boolean shiftDown, QueriableAnnotation data, Annotation selection, Annotation[] selected);
	}
	
	/**
	 * The callback object to be injected in the AnnotationTable's
	 * performAction method for performing the actual action that results from a
	 * click in the context menu or a shortcut invoked by a combination of keys.
	 * 
	 * @author sautter
	 */
	public static abstract class AnnotationAction {
		
		private AnnotationTable target = null;
		
		/**
		 * Obtain an annotation last clicked in the annotation table (if this
		 * action is not in the process of being executed in an invocation of
		 * AnnotationTable.performAction(), this method returns null). If the
		 * AnnotationTable is constructed with an array of MutableAnnotations,
		 * this method returns a MutableAnnotation.
		 * @return an Annotation last clicked in the annotation table (if this
		 *         AnnotationAction is invoked on behalf of a Shortcut, this
		 *         method always returns null)
		 */
		protected final Annotation getClickedAnnotation() {
			if (this.target == null) return null;
			else return this.target.clicked;
		}
		
		/**
		 * Obtain the annotations currently selected in the annotation table (if
		 * this action is not in the process of being executed in an invocation
		 * of AnnotationTable.performAction(), this method returns null). If the
		 * AnnotationTable is constructed with an array of MutableAnnotations,
		 * this method returns an array of MutableAnnotations.
		 * @return an array holding the Annotations currently selected in the
		 *         annotation table
		 */
		protected final Annotation[] getSelectedAnnotations() {
			if (this.target == null) return null;
			else return this.target.selected;
		}
		
		/**
		 * Perform the actual action
		 * @param data the QueriableAnnotation to work on
		 */
		public abstract void performAction(QueriableAnnotation data);
	}
	
//	private Annotation[] annotations;
//	private boolean[] selectors;
	private AnnotationTableModel annotationTableModel;
	private JTable annotationTable = new JTable();
	
	private JPopupMenu contextMenu = new JPopupMenu();
	
	private ArrayList contextMenuOptions = new ArrayList();
	private ArrayList shortcuts = new ArrayList();
	private HashMap shortcutMap = new HashMap();
	
	private Annotation clicked = null;
	private Annotation[] selected;
	
	/**
	 * Constructor producing a table for plainly displaying the Annotations in
	 * the specified array.
	 * @param annotations the array holding the Annotations to display in the
	 *            surrounding table
	 */
	public AnnotationTable(Annotation[] annotations) {
		this(new AnnotationTableModel(annotations));
	}
	
	/**
	 * Constructor producing a table for displaying the Annotations in the
	 * specified array and selecting them using checkboxes.
	 * @param annotations the array holding the Annotations to display in the
	 *            surrounding table
	 * @param selectColumnName the column name for a leading column of
	 *            checkboxes for selecting individual Annotations
	 */
	public AnnotationTable(Annotation[] annotations, String selectColumnName) {
		this(new AnnotationTableModel(annotations, selectColumnName));
	}
	
	/**
	 * Constructor producing a table for displaying Annotations retrieved from a
	 * context QueriableAnnotation using an AnnotationFilter
	 * @param context the QueriableAnnotation from which to retrieve the
	 *            Annotations to display
	 * @param filter the filter for retrieving the Annotations to display from
	 *            the context QueriableAnnotation
	 */
	public AnnotationTable(QueriableAnnotation context, AnnotationFilter filter) {
		this(new AnnotationTableModel(context, filter));
	}
	
	/**
	 * Constructor producing a table for displaying Annotations retrieved from a
	 * context QueriableAnnotation using an AnnotationFilter and selecting them
	 * using checkboxes.
	 * @param context the QueriableAnnotation from which to retrieve the
	 *            Annotations to display
	 * @param filter the filter for retrieving the Annotations to display from
	 *            the context QueriableAnnotation
	 * @param selectColumnName the column name for a leading column of
	 *            checkboxes for selecting individual Annotations
	 */
	public AnnotationTable(QueriableAnnotation context, AnnotationFilter filter, String selectColumnName) {
		this(new AnnotationTableModel(context, filter, selectColumnName));
	}
	
	/**
	 * Constructor producing a table from a custom AnnotationTableModel
	 * @param model the model to use
	 */
	public AnnotationTable(AnnotationTableModel model) {
		super(new BorderLayout(), true);
		
		this.setBorder(BorderFactory.createEtchedBorder());
		
		this.annotationTableModel = model;
		
		this.annotationTable.setDefaultRenderer(Object.class, new TooltipAwareTableRenderer(4));
		this.annotationTable.setModel(this.annotationTableModel);
		
		for (int c = 0; c < this.annotationTableModel.getColumnCount(); c++) {
			int width = this.annotationTableModel.getColumnWidth(c);
			if (width > 0)
				this.annotationTable.getColumnModel().getColumn(c).setMaxWidth(width);
		}
		
		JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
		this.add(annotationTableBox, BorderLayout.CENTER);
		
		JPanel extensionPanel = this.annotationTableModel.getExtensionPanel();
		if (extensionPanel != null)
			this.add(extensionPanel, BorderLayout.SOUTH);
		
		this.annotationTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				
				//	get current status
				storeStatusForClick(me);
				
				//	if right click, show menu
				if (me.getButton() != MouseEvent.BUTTON1)
					showPopupMenu(me);
			}
		});
		
		this.annotationTable.addKeyListener(new KeyListener() {
			private int lastKeyCode = 0;
			public void keyPressed(KeyEvent ke) {
				this.lastKeyCode = ke.getKeyCode();
			}
			public void keyReleased(KeyEvent ke) {}
			public void keyTyped(KeyEvent ke) {
				handleKeyStroke(ke, this.lastKeyCode);
				this.lastKeyCode = 0;
			}
		});
	}
	
	private void storeStatusForClick(MouseEvent me) {
		
		//	get annotation clicked in the table
		int clickRow = this.annotationTable.rowAtPoint(me.getPoint());
		this.clicked = ((clickRow == -1) ? null : this.annotationTableModel.getAnnotation(clickRow));
		
		//	get annotations selected in the table
		int[] selRows = this.annotationTable.getSelectedRows();
		if (this.annotationTableModel.usesMutableAnnotations()) {
			this.selected = new MutableAnnotation[selRows.length];
			for (int r = 0; r < selRows.length; r++)
				this.selected[r] = ((MutableAnnotation) this.annotationTableModel.getAnnotation(selRows[r]));
		}
		else {
			this.selected = new Annotation[selRows.length];
			for (int r = 0; r < selRows.length; r++)
				this.selected[r] = this.annotationTableModel.getAnnotation(selRows[r]);
		}
	}
	
	private void showPopupMenu(MouseEvent me) {
		
		//	get menu items for available actions
		JMenuItem[] menuItems = this.getContextMenuItems(this.clicked, this.selected);
		
		//	no actions to display
		if (menuItems.length == 0) return;
		
		//	fill and show context menu
		else {
			this.contextMenu.removeAll();
			for (int i = 0; i < menuItems.length; i++)
				this.contextMenu.add(menuItems[i]);
			this.contextMenu.show(this.annotationTable, me.getX(), me.getY());
		}
	}
	
	private void storeStatusForKeyStroke() {
		
		//	no annotation clicked for keystroke
		this.clicked = null;
		
		//	get annotations selected in the table
		int[] selRows = this.annotationTable.getSelectedRows();
		if (this.annotationTableModel.usesMutableAnnotations()) {
			this.selected = new MutableAnnotation[selRows.length];
			for (int r = 0; r < selRows.length; r++)
				this.selected[r] = ((MutableAnnotation) this.annotationTableModel.getAnnotation(selRows[r]));
		}
		else {
			this.selected = new Annotation[selRows.length];
			for (int r = 0; r < selRows.length; r++)
				this.selected[r] = this.annotationTableModel.getAnnotation(selRows[r]);
		}
	}
	
	private void handleKeyStroke(KeyEvent ke, int lastKeyCode) {
		System.out.println("Key typed in AnnotationTable");
		System.out.println(" - last key code is '" + lastKeyCode + "', as char '" + ((char) lastKeyCode) + "'");
		System.out.println(" - char is '" + ke.getKeyChar() + "'");
		System.out.println(" - as int " + ((int) ke.getKeyChar()));
		System.out.println(" - key code is " + ke.getKeyCode());
		System.out.println(" - Ctrl is " + ke.isControlDown());
		System.out.println(" - Alt is " + ke.isAltDown());
		System.out.println(" - Shift is " + ke.isShiftDown());
		
		//	produce 'Ctrl'/'Alt' part of key
		String ctrl = (ke.isControlDown() ? Shortcut.CONTROL_KEY_PREFIX : "");
		String alt = (ke.isAltDown() ? Shortcut.ALT_KEY_PREFIX : "");
		
		//	catch key combinations using both of 'Ctrl' alt 'Alt', and ones using neither if text is editable
		int ctrlAltLen = (ctrl.length() + alt.length());
		if ((ctrlAltLen > Math.max(ctrl.length(), alt.length()))) return;
		
		//	produce 'Shift' part of key
		String shift = (ke.isShiftDown() ? Shortcut.SHIFT_KEY_PREFIX : "");
		
		//	assemble key
		String key = (ctrl + alt + shift + ((char) lastKeyCode));
		System.out.println(" - Shortcut key is " + key);
		
		//	get shortcut
		Shortcut shortcut = ((Shortcut) this.shortcutMap.get(key));
		
		//	try 'Shift' insensitice key
		if ((shortcut == null) && ke.isShiftDown()) {
			key = (ctrl + alt + ((char) lastKeyCode));
			System.out.println(" - 'Shift' insensitive shortcut key is " + key);
			shortcut = ((Shortcut) this.shortcutMap.get(key));
		}
		
		//	no shortcut mapped to key combination
		if (shortcut == null) return;
		
		//	found shortcut, store status
		this.storeStatusForKeyStroke();
		
		//	get action
		AnnotationAction action = shortcut.getAction(ke.isShiftDown(), this.annotationTableModel.getContext(), null, this.selected);
		
		//	execute action
		if (action != null) this.performAction(action);
		
		//	consume key event
		ke.consume();
	}
	
//	private void selectAll() {
//		for (int s = 0; s < this.selectors.length; s++) this.selectors[s] = true;
//		this.annotationTable.repaint();
//		this.validate();
//	}
//	
//	private void selectNone() {
//		for (int s = 0; s < this.selectors.length; s++) this.selectors[s] = false;
//		this.annotationTable.repaint();
//		this.validate();
//	}
	
	/**
	 * retrieve the Annotations currently selected by means of the respective check box
	 * @return an array holding the selected Annotations
	 */
	public Annotation[] getSelectedAnnotations() {
		ArrayList collector = new ArrayList();
		for (int r = 0; r < this.annotationTableModel.getRowCount(); r++)
			if (this.annotationTableModel.isAnnotationSelected(r))
				collector.add(this.annotationTableModel.getAnnotation(r));
		return ((Annotation[]) collector.toArray(new Annotation[collector.size()]));
	}
	
//	private class AnnotationTableModel implements TableModel {
//		private Annotation[] annotations;
//		
//		private AnnotationTableModel(Annotation[] annotations) {
//			this.annotations = annotations;
//		}
//		
//		/** @see javax.swing.table.TableModel#getColumnClass(int)
//		 */
//		public Class getColumnClass(int columnIndex) {
//			return String.class;
//		}
//		
//		/** @see javax.swing.table.TableModel#getColumnCount()
//		 */
//		public int getColumnCount() {
//			return 4;
//		}
//		
//		/** @see javax.swing.table.TableModel#getColumnName(int)
//		 */
//		public String getColumnName(int columnIndex) {
//			if (columnIndex == 0) return "Type";
//			if (columnIndex == 1) return "Start";
//			if (columnIndex == 2) return "Size";
//			if (columnIndex == 3) return "Value";
//			return null;
//		}
//		
//		/** @see javax.swing.table.TableModel#getRowCount()
//		 */
//		public int getRowCount() {
//			return this.annotations.length;
//		}
//		
//		/** @see javax.swing.table.TableModel#getValueAt(int, int)
//		 */
//		public Object getValueAt(int rowIndex, int columnIndex) {
//			Annotation a = this.annotations[rowIndex];
//			if (columnIndex == 0) return a.getType();
//			if (columnIndex == 1) return "" + a.getStartIndex();
//			if (columnIndex == 2) return "" + a.size();
//			if (columnIndex == 3) return a.getValue();
//			return null;
//		}
//		
//		/** @see javax.swing.table.TableModel#isCellEditable(int, int)
//		 */
//		public boolean isCellEditable(int rowIndex, int columnIndex) {
//			return false;
//		}
//		
//		/** @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
//		 */
//		public void addTableModelListener(TableModelListener l) {}
//		
//		/** @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
//		 */
//		public void removeTableModelListener(TableModelListener l) {}
//		
//		/** @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
//		 */
//		public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
//	}
//
//	private class SelectableAnnotationTableModel implements TableModel {
//		private boolean[] selectors;
//		private Annotation[] annotations;
//		
//		private SelectableAnnotationTableModel(boolean[] selectors, Annotation[] annotations) {
//			this.selectors = selectors;
//			this.annotations = annotations;
//		}
//		
//		/** @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
//		 */
//		public void addTableModelListener(TableModelListener l) {}
//		
//		/** @see javax.swing.table.TableModel#getColumnClass(int)
//		 */
//		public Class getColumnClass(int columnIndex) {
//			if (columnIndex == 0) return Boolean.class;
//			else return String.class;
//		}
//		
//		/** @see javax.swing.table.TableModel#getColumnCount()
//		 */
//		public int getColumnCount() {
//			return 5;
//		}
//		
//		/** @see javax.swing.table.TableModel#getColumnName(int)
//		 */
//		public String getColumnName(int columnIndex) {
//			if (columnIndex == 0) return "Select";
//			if (columnIndex == 1) return "Type";
//			if (columnIndex == 2) return "Start";
//			if (columnIndex == 3) return "Size";
//			if (columnIndex == 4) return "Value";
//			return null;
//		}
//		
//		/** @see javax.swing.table.TableModel#getRowCount()
//		 */
//		public int getRowCount() {
//			return this.annotations.length;
//		}
//		
//		/** @see javax.swing.table.TableModel#getValueAt(int, int)
//		 */
//		public Object getValueAt(int rowIndex, int columnIndex) {
//			if (columnIndex == 0) return new Boolean(this.selectors[rowIndex]);
//			Annotation a = this.annotations[rowIndex];
//			if (columnIndex == 1) return a.getType();
//			if (columnIndex == 2) return "" + a.getStartIndex();
//			if (columnIndex == 3) return "" + a.size();
//			if (columnIndex == 4) return a.getValue();
//			return null;
//		}
//		
//		/** @see javax.swing.table.TableModel#isCellEditable(int, int)
//		 */
//		public boolean isCellEditable(int rowIndex, int columnIndex) {
//			return (columnIndex == 0);
//		}
//		
//		/** @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
//		 */
//		public void removeTableModelListener(TableModelListener l) {}
//		
//		/** @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
//		 */
//		public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
//			if (columnIndex == 0) this.selectors[rowIndex] = ((Boolean) newValue).booleanValue();
//		}
//	}

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
	
	/**
	 * set the selection model of the annotation table
	 * @param selectionModel
	 */
	public void setSelectionModel(int selectionModel) {
		this.annotationTable.setSelectionMode(selectionModel);
	}
	
	/**
	 * add a ContextMenuOption to be available in the context menu of the
	 * AnnotationTable
	 * @param option the ContextMenuOption to add
	 */
	public void addContextMenuOption(ContextMenuOption option) {
		this.contextMenuOptions.add(option);
	}
	
	/**
	 * remove a ContextMenuOption from the context menu of the AnnotationTable
	 * @param option the ContextMenuOption to remove
	 */
	public void removeContextMenuOption(ContextMenuOption option) {
		this.contextMenuOptions.remove(option);
	}
	
	/**
	 * retrieve all ContextMenuOptions currently registered to this
	 * AnnotationTable
	 * @return an array holding all ContextMenuOptions currently registered to
	 *         this AnnotationTable
	 */
	public ContextMenuOption[] getContextMenuOptions() {
		return ((ContextMenuOption[]) this.contextMenuOptions.toArray(new ContextMenuOption[this.contextMenuOptions.size()]));
	}
	
	/**
	 * remove all ContextMenuOptions from this AnnotationTable
	 */
	public void clearContextMenuOptions() {
		this.contextMenuOptions.clear();
	}
	
	/*
	 * retrieve the JMenuItems for displaying a context menu for the current
	 * text selection
	 * @param data the content of the editor, thus the mutable annotation to
	 *            work on. If the textEditable property of the surrounding
	 *            AnnotationTable is set to false, any attempt of modifying the
	 *            text will throw an IllegalStateException
	 * @param selection a StandaloneAnnotation marking the text currently
	 *            selected in the display (null if no selection)
	 * @param selected an array holding the Annotations overlapping with the
	 *            current text selection in the display, or Annotations
	 *            enclosing the caret position if no text is selected
	 * @return an array holding the JMenuItem making all the registered
	 *         ContextMenuOptions accessible that are applicable for the current
	 *         text selection
	 */
	private JMenuItem[] getContextMenuItems(Annotation clicked, Annotation[] selected) {
		ArrayList menuItems = new ArrayList();
		for (int a = 0; a < this.contextMenuOptions.size(); a++) {
			JMenuItem mi = ((ContextMenuOption) this.contextMenuOptions.get(a)).getMenuItem(this.annotationTableModel.getContext(), clicked, selected, this);
			if (mi != null) menuItems.add(mi);
		}
		return ((JMenuItem[]) menuItems.toArray(new JMenuItem[menuItems.size()]));
	}
	
	/**
	 * add a Shortcut to be available in the AnnotationTable
	 * @param shortcut the Shortcut to add
	 */
	public void addShortcut(Shortcut shortcut) {
		this.shortcuts.add(shortcut);
		String sid = shortcut.getShortcutIdentifier();
		if (sid != null) this.shortcutMap.put(sid, shortcut);
	}
	
	/**
	 * remove a Shortcut from the context menu of the AnnotationTable
	 * @param shortcut the Shortcut to remove
	 */
	public void removeShortcut(Shortcut shortcut) {
		this.shortcuts.remove(shortcut);
		this.shortcutMap.remove(shortcut.getShortcutIdentifier());
	}
	
	/**
	 * retrieve all Shortcuts currently registered to this AnnotationTable
	 * @return an array holding all Shortcuts currently registered to this
	 *         AnnotationTable
	 */
	public Shortcut[] getShortcuts() {
		return ((Shortcut[]) this.shortcuts.toArray(new Shortcut[this.shortcuts.size()]));
	}
	
	/**
	 * remove all Shortcuts from this AnnotationTable
	 */
	public void clearShortcuts() {
		this.shortcuts.clear();
		this.shortcutMap.clear();
	}
	
	/**
	 * perform an action (this method is for callbacks from AnnotationActions)
	 * @param action the action to perform
	 */
	public void performAction(AnnotationAction action) {
		
		AnnotationChangeRecorder acr = null;
		
		QueriableAnnotation context = this.annotationTableModel.getContext();
		if ((context != null) && (context instanceof MutableAnnotation)) {
			acr = new AnnotationChangeRecorder();
			((MutableAnnotation) context).addAnnotationListener(acr);
		}
		
		try {
			action.target = this;
			action.performAction(context);
		}
		catch (Exception e) {
			System.out.println("Exception performing action: " + e.getClass() + " (" + e.getMessage() + ")");
			e.printStackTrace(System.out);
		}
		catch (Error e) {
			System.out.println("Error performing action: " + e.getClass() + " (" + e.getMessage() + ")");
			e.printStackTrace(System.out);
		}
		finally {
			action.target = null;
		}
		
		if (acr != null) {
			((MutableAnnotation) context).removeAnnotationListener(acr);
			if (acr.annotationsModified)
				this.annotationTableModel.refreshAnnotations();
		}
	}
	
	private class AnnotationChangeRecorder implements AnnotationListener {
		private boolean annotationsModified = false;
		
		public void annotationAdded(QueriableAnnotation doc, Annotation annotation) {
			this.annotationsModified = true;
		}
		
		public void annotationRemoved(QueriableAnnotation doc, Annotation annotation) {
			this.annotationsModified = true;
		}
		
		public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType) {
			this.annotationsModified = true;
		}
		
		public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue) {
			this.annotationsModified = true;
		}
	}
}
