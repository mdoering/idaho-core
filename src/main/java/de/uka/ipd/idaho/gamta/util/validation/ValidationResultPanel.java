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
package de.uka.ipd.idaho.gamta.util.validation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.gamta.util.validation.Step.StepValidationResult;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A validation result panel is a GUI widget that validates a given
 * MutableAnnotation using a given Validator and displays the validation result
 * for editing. The base panel shows the individual parts of the markup process
 * backing the specified Validator, highlighting process parts for which the
 * document failed the validation.<BR>
 * A double click on such a highlighted row opens a sub dialog (called
 * 'criterion dialog') showing the individual annotations that were subject to
 * the failed validation criterion. A criterion dialog can either display only
 * the annotations that actually failed the criterion, or show all annotations
 * that are subject to the criterion, highlighting the ones that actually.<BR>
 * The criterion dialog's context menu by default offers modifying annotation's
 * attributes, removing and deleting annotations, and marking failing
 * annotations to be ignored by the dialog's criterion. Sub classes can add
 * further options to the context menu by overwriting the getContextMenuItems()
 * method to provide respective menu items. In addition, sub classes may provide
 * a specific action for double clicks in the criterion dialog, by overwriting
 * the reactOnDoubleClick() method.
 * 
 * @author sautter
 */
public class ValidationResultPanel extends JPanel {
	/* TODO
Try re-validating only affected instances after changes in manual correction dialog
- should work if context path has no predicates
- should work if single context annotation is changed (not removed or split, though)
==> in particular, works on "Accept As Is" in context menu, and on "Accept As Is" in dialog if there's no editing

Idea: cache IDs of context annotations NOT affected by editing, re-validate only those that ARE affected
==> works even on splits
==> also works if context path has predicates only at its end (facilitates re-checking annotations of context type whose ID is not cached)
==> adjust handling of annotation trays accordingly

Implementation:
- remember type of context annotations (should remain the same - if not, do things the current (slow) way)
- inspect context path (if predicates anywhere but in the end, do things the current (slow) way)
- if context path simple enough (child/descendant steps only), parse step types and execute locally
  - plainly in for loops ==> pretty fast
- remember IDs of context annotations
- on update, re-get context annotations and inspect the ones that changed
  - specify IDs of changed annotations as arguments to re-validate method
  - forget about these IDs
  - re-get context annotations
  - filter out the ones with un-cached IDs
  - submit those annotations to
    - (optionally) context path predicates
    - criterion predicates
  - update annotation trays accordingly
	 */
	
	private MutableAnnotation data;
	private Validator validator;
	
	private ValidationResult validationResult;
	
	private PartialResultTray[] prts;
	private PartialResultTray editablePrt = null;
	
	private JLabel defaultMainLabel;
	private JLabel mainLabel;
	private JLabel defaultDetailLabel;
	private JLabel detailLabel;
	
	private JTable resultTable = new JTable();
	private JScrollPane resultListBox;
	
	/**
	 * Constructor
	 * @param data the document whose validation results to display
	 * @param validator the validator to use
	 */
	public ValidationResultPanel(MutableAnnotation data, Validator validator) {
		super(new BorderLayout(), true);
		this.data = data;
		this.validator = validator;
		
		//	explain what to do
		this.defaultMainLabel = new JLabel("Double click on the bold lines to open failing Annotations for editing.", JLabel.LEFT);
		this.defaultMainLabel.setBorder(BorderFactory.createLineBorder(defaultMainLabel.getBackground(), 4));
		
		//	explain basic detail options
		this.defaultDetailLabel = new ValidationResultLabel("Double click on the bold lines to open failing Annotations for editing.", JLabel.LEFT) {
			public void validationResultChanged(ValidationResult vr) {
				if (vr.isPassed())
					this.setText("<HTML><B>Document Status:</B> '" + vr.getValidator().getDescription() + "' complete." +
							"<BR>Use the <I>Close</I> button to return to the main window and proceed." +
							"</HTML>");
				
				else {
					String error = vr.getValidator().getErrorDescription();
					ValidationResult[] pVrs = vr.getPartialResults();
					StringVector pErrors = new StringVector();
					for (int p = 0; p < pVrs.length; p++) {
						if (!pVrs[p].isPassed())
							pErrors.addElement(pVrs[p].getValidator().getErrorDescription());
					}
					this.setText("<HTML>" +
						"<B>Document Status:</B> Some annotations fail the test for '" + vr.getValidator().getLabel() + "'." +
						"<BR>In particular, " + error.substring(0, 1).toLowerCase() + error.substring(1) + ((pVrs.length > 1) ? ":" : ".") + 
						((pVrs.length > 1) ? ("<BR>&nbsp;&nbsp;- " + pErrors.concatStrings("<BR>&nbsp;&nbsp;- ")) : "") + 
						"</HTML>");
				}
				this.validate();
			}
		};
		this.defaultDetailLabel.setBorder(BorderFactory.createLineBorder(defaultMainLabel.getBackground(), 4));
		
		//	style table
		this.resultTable.setShowHorizontalLines(true);
		this.resultTable.setShowVerticalLines(true);
		this.resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.resultTable.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				if ((me.getButton() == MouseEvent.BUTTON1) && (me.getClickCount() > 1)) {
					int rowIndex = resultTable.getSelectedRow();
					if ((rowIndex != -1) && (prts[rowIndex].isEditable()))
						showAnnotations(prts[rowIndex]);
				}
			}
		});
		
		//	display table in scroll pane
		this.resultListBox = new JScrollPane(this.resultTable);
		this.resultListBox.setViewportBorder(BorderFactory.createLoweredBevelBorder());
		
		//	display data
		this.validateDocument();
	}
	
	/**
	 * Retrieve the validator used in this validation result panel, in
	 * particular the one specified to the constructor.
	 * @return the validator used in this validation result panel
	 */
	public Validator getValidator() {
		return this.validator;
	}
	
	/**
	 * Retrieve the validation result that reflects the current state of the
	 * document this validation result panel refers to. The returned result is
	 * the one created by the last invokation of validateDocument(). The
	 * constructor invokes this method, so the validation result is never null.
	 * However, if the content document has been modified externally since the
	 * last invokation of validateDocument(), the returned validation result may
	 * not reflect these changes.
	 * @return the current validation result for the content document
	 */
	public ValidationResult getValidationResult() {
		return this.validationResult;
	}
	
	/**
	 * Retrieve the step validation result of the first step that failed. If no
	 * step fails at all in the markup process definition backing the validator,
	 * this method returns null.
	 * @return the step validation result of the first step that failed
	 */
	public StepValidationResult getCurrentStep() {
		return ((this.editablePrt == null) ? null : ((StepValidationResult) this.editablePrt.vr));
	}
	
	/**
	 * Specify the label widget to display above the validation result overview.
	 * Specifying null results in the default label to be displayed. If the
	 * argument JLabel is a ValidationResultLabel, it will be notified whenever
	 * the validation result changes in the main panel, allowing it to adjust
	 * accordingly.
	 * @param mainLabel the label widget to display above the validation result
	 *            overview
	 */
	public void setMainLabel(JLabel mainLabel) {
		this.mainLabel = mainLabel;
		if (!(this.validationResult instanceof StepValidationResult))
			this.validateDocument();
	}
	
	/**
	 * Specify the label widget to display above the detail validation result
	 * list. Specifying null results in no label to be displayed. If the
	 * argument JLabel is a ValidationResultLabel, it will be notified whenever
	 * the validation result changes in the validation result detail dialog,
	 * allowing it to adjust accordingly. The argument validation result to the
	 * validationResultChanged() method will usually be a StepValidationResult.
	 * @param detailLabel the label widget to display above the detail validation
	 *            result list
	 */
	public void setDetailLabel(JLabel detailLabel) {
		this.detailLabel = detailLabel;
		if (this.validationResult instanceof StepValidationResult)
			this.validateDocument();
	}
	
	/**
	 * A JLabel exposing a notification method for changes in the validation
	 * result, allowing to adjust its text whenever the validation result
	 * changes, i.e., for every invokation of validateDocument().
	 * 
	 * @author sautter
	 */
	public static class ValidationResultLabel extends JLabel {
		public ValidationResultLabel() {
			super();
		}
		public ValidationResultLabel(Icon image) {
			super(image);
		}
		public ValidationResultLabel(Icon image, int horizontalAlignment) {
			super(image, horizontalAlignment);
		}
		public ValidationResultLabel(String text) {
			super(text);
		}
		public ValidationResultLabel(String text, int horizontalAlignment) {
			super(text, horizontalAlignment);
		}
		public ValidationResultLabel(String text, Icon icon, int horizontalAlignment) {
			super(text, icon, horizontalAlignment);
		}

		/**
		 * Receive notification that the validation result has changed. This
		 * default implementation does nothing, sub classes are welcome to
		 * overwrite it as needed.
		 * @param vr the new validation result
		 */
		public void validationResultChanged(ValidationResult vr) {}
	}
	
	/**
	 * Open a detail validation result dialog showing which annotations were
	 * tested for the current step, and which failed the test. If the backing
	 * validator does not report any errors, this method does nothing. If a
	 * validation result detail dialog is alredy showing, this method also does
	 * nothing. In addition, if the content of this panel already is a detail
	 * display (which it is if the backing validator belongs to a markup process
	 * step), this method does nothing.
	 */
	public void showDetailResult() {
		
		//	we are in detail mode already
		if (this.validationResult instanceof StepValidationResult)
			return;
		
		//	something to display?
		if (this.editablePrt != null)
			this.showAnnotations(this.editablePrt);
	}
	
	/**
	 * Validate the contained document again, and update the display.
	 */
	public void validateDocument() {
		
		//	do validation
		this.validationResult = this.validator.validate(data);
		this.editablePrt = null;
		this.removeAll();
		
//		//	we have a single criterion validator, display details directly
//		if (vr instanceof Criterion.CriterionValidationResult) {
//			Window topWindow = DialogFactory.getTopWindow();
//			CriterionResultPanel crp;
//			if (topWindow instanceof JDialog)
//				crp = new CriterionResultPanel(((JDialog) topWindow), this.data, new PartialResultTray(null, vr, 0));
//			else if (topWindow instanceof JFrame)
//				crp = new CriterionResultPanel(DialogFactory.produceDialog(((JFrame) topWindow).getTitle(), true), this.data, new PartialResultTray(null, vr, 0));
//			else crp = new CriterionResultPanel(DialogFactory.produceDialog((vr.isPassed() ? vr.getDescription() : vr.getErrorDescription()), true), this.data, new PartialResultTray(null, vr, 0));
//			this.add(crp, BorderLayout.CENTER);
//		}
		//	we have a single step validator, display details directly
		if (this.validationResult instanceof StepValidationResult) {
			
			this.prts = new PartialResultTray[1];
			this.prts[0] = new PartialResultTray(this.validationResult, 0);
			if (this.prts[0].isEditable())
				this.editablePrt = this.prts[0];
			
			Window topWindow = DialogFactory.getTopWindow();
			StepResultPanel srp;
			if (topWindow instanceof JDialog)
				srp = new StepResultPanel(((JDialog) topWindow), this.data, this.prts[0]);
			else if (topWindow instanceof JFrame)
				srp = new StepResultPanel(DialogFactory.produceDialog(((JFrame) topWindow).getTitle(), true), this.data, this.prts[0]);
			else srp = new StepResultPanel(DialogFactory.produceDialog((this.validationResult.isPassed() ? this.validationResult.getDescription() : this.validationResult.getErrorDescription()), true), this.data, this.prts[0]);
			this.add(srp, BorderLayout.CENTER);
		}
		
		//	more complex result, display overview
		else {
			
			//	add label
			this.add(((this.mainLabel == null) ? this.defaultMainLabel : this.mainLabel), BorderLayout.NORTH);
			
			//	line up partial results in depth first order
			ArrayList prtList = this.lineUpPartialResults(this.validationResult, 0);
			this.prts = ((PartialResultTray[]) prtList.toArray(new PartialResultTray[prtList.size()]));
			for (int t = 0; t < this.prts.length; t++)
				if (this.prts[t].isEditable()) {
					this.editablePrt = this.prts[t];
					t = this.prts.length;
				}
			
			//	display result
			this.resultTable.setModel(new TableModel() {
				public int getColumnCount() {
					return 4;
				}
				public int getRowCount() {
					return prts.length;
				}
				public String getColumnName(int columnIndex) {
					if (columnIndex == 0) return "Check";
					else if (columnIndex == 1) return "Checked";
					else if (columnIndex == 2) return "OK";
					else if (columnIndex == 2) return "Percent";
					else return null;
				}
				public Class getColumnClass(int columnIndex) {
					return String.class;
				}
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
				}
				public Object getValueAt(int rowIndex, int columnIndex) {
					if (columnIndex == 0) {
						String indent = "";
						for (int i = 0; i < prts[rowIndex].depth; i++)
							indent += "&nbsp;&nbsp;";
						if (prts[rowIndex].isEditable())
							return ("<HTML>" + indent + "<B>" + prts[rowIndex].vr.getValidator().getErrorDescription() + "</B></HTML>");
						else if (!prts[rowIndex].vr.isPassed())
							return ("<HTML>" + indent + prts[rowIndex].vr.getValidator().getErrorDescription() + "</HTML>");
						else return ("<HTML>" + indent + prts[rowIndex].vr.getDescription() + "</HTML>");
					}
					else if (columnIndex == 1) return ("" + prts[rowIndex].vr.getInstanceCount());
					else if (columnIndex == 2) return ("" + prts[rowIndex].vr.getPassCount());
					else if (columnIndex == 3) return ("" + prts[rowIndex].vr.getSuccessPercent() + "%");
					else return null;
				}
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {}
				
				public void addTableModelListener(TableModelListener l) {}
				public void removeTableModelListener(TableModelListener l) {}
			});
			this.resultTable.getColumnModel().getColumn(1).setMaxWidth(60);
			this.resultTable.getColumnModel().getColumn(2).setMaxWidth(60);
			this.resultTable.getColumnModel().getColumn(3).setMaxWidth(60);
			
			this.add(this.resultListBox, BorderLayout.CENTER);
		}
		
		if ((this.mainLabel != null) && (this.mainLabel instanceof ValidationResultLabel))
			((ValidationResultLabel) this.mainLabel).validationResultChanged(this.validationResult);
		
		this.validate();
		this.repaint();
	}
	
	private boolean showDetailLabel = true;
	private JDialog validationResultDetailDialog = null;
	private Dimension vrddSize = new Dimension(500, 650);
	private Point vrddLocation = null;
	
	private synchronized void showAnnotations(PartialResultTray prt) {
		if (this.validationResultDetailDialog != null)
			return;
		
		this.validationResultDetailDialog = DialogFactory.produceDialog(prt.vr.getValidator().getErrorDescription(), true);
		this.validationResultDetailDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		
		JButton closeButton = new JButton("Close");
		closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		closeButton.setPreferredSize(new Dimension(100, 21));
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				vrddSize = validationResultDetailDialog.getSize();
				vrddLocation = validationResultDetailDialog.getLocation();
				validationResultDetailDialog.dispose();
			}
		});
		
		//	create editor
		StepResultPanel srp = new StepResultPanel(this.validationResultDetailDialog, this.data, prt);
		
		//	put the whole stuff together
		this.validationResultDetailDialog.getContentPane().setLayout(new BorderLayout());
		this.validationResultDetailDialog.getContentPane().add(srp, BorderLayout.CENTER);
		this.validationResultDetailDialog.getContentPane().add(closeButton, BorderLayout.SOUTH);
		
		this.validationResultDetailDialog.setResizable(true);
		this.validationResultDetailDialog.setSize(this.vrddSize);
		if (this.vrddLocation == null)
			this.validationResultDetailDialog.setLocationRelativeTo(this.validationResultDetailDialog.getOwner());
		else this.validationResultDetailDialog.setLocation(this.vrddLocation);
		
		//	show dialog
		this.validationResultDetailDialog.setVisible(true);
		
		//	refresh table if modified
		if (srp.isModified())
			this.validateDocument();
		
		//	make way for new dialog
		this.validationResultDetailDialog = null;
	}
	
	private ArrayList lineUpPartialResults(ValidationResult vr, int depth) {
		ArrayList prtList = new ArrayList();
		
		PartialResultTray prt = new PartialResultTray(vr, depth);
		prtList.add(prt);
		
		if (vr.isPassed())
			return prtList;
		
		else if (vr instanceof StepValidationResult)
			return prtList;
		
		ValidationResult[] childVrs = vr.getPartialResults();
		for (int c = 0; c < childVrs.length; c++)
			prtList.addAll(this.lineUpPartialResults(childVrs[c], (depth+1)));
		
		return prtList;
	}
	
	private static class PartialResultTray {
		final ValidationResult vr;
		final int depth;
		PartialResultTray(ValidationResult vr, int depth) {
			this.vr = vr;
			this.depth = depth;
		}
		boolean isEditable() {
			return ((this.vr instanceof StepValidationResult) && !this.vr.isPassed());
		}
	}
	
	/**
	 * Container for a single annotation in a criterion dialog. The contained
	 * annotation is one that is subject to the dialog's validation criterion.
	 * In addition, the container provides several basic operations that can be
	 * applied to the contained annotation, but only in case it's one that
	 * actually failed the criterion.
	 * 
	 * @author sautter
	 */
	public static class AnnotationTray {
		
		/**
		 * the annotation, relative to the surrounding validation result panel's
		 * argument annotation
		 */
		public final MutableAnnotation annotation;
		
		Annotation failed;
		boolean delete = false;
		boolean remove = false;
		boolean ignore = false;
		
		AnnotationTray(MutableAnnotation annotation) {
			this.annotation = annotation;
		}
		
		/**
		 * Check if the annotation in this tray is one that failed the backing
		 * criterion.
		 * @return true if the contained annotation failed the backing
		 *         criterion, false otherwise
		 */
		public boolean isFailed() {
			return (this.failed != null);
		}
		
		/**
		 * Mark the contained annotation for being deleted from the backing
		 * document. This method has an effect only if isFailed() returns true.
		 */
		public void delete() {
			this.delete = true;
		}
		
		/**
		 * Mark the contained annotation for being removed from the backing
		 * document. This method has an effect only if isFailed() returns true.
		 */
		public void remove() {
			this.remove = true;
		}
		
		/**
		 * Mark the contained annotation for being ignored by the backing
		 * validation criterion from now on. This method has an effect only if
		 * isFailed() returns true.
		 */
		public void ignore() {
			this.ignore = true;
		}
	}
	
	private class StepResultPanel extends JPanel {
		
		private JDialog dialog;
		private String originalTitle;
		private boolean isModified = false;
		
		private JPanel displayModePanel = new JPanel(new GridLayout(1, 0));
		private JPanel topPanel = new JPanel(new BorderLayout());
		private JTable annotationTable;
		
		private JRadioButton showMatches = new JRadioButton("Show Failing Annotations Only", false);
		private JRadioButton highlightMatches = new JRadioButton("Highlight Failing Annotations", true);
		private JCheckBox detailLabelSwitch = new JCheckBox("Show Detail Explanation", showDetailLabel);
		
		private MutableAnnotation data;
		private AnnotationTray[] annotationTrays;
		private AnnotationTray[] allAnnotationTrays;
		private HashMap annotationTraysByID = new HashMap();
		
		private Validator validator;
		private StepValidationResult vr;
		private String sortField = Annotation.START_INDEX_ATTRIBUTE;
		
		StepResultPanel(JDialog dialog, MutableAnnotation data, PartialResultTray prt) {
			super(new BorderLayout(), true);
			this.dialog = dialog;
			this.originalTitle = this.dialog.getTitle();
			
			this.data = data;
			
			this.validator = prt.vr.getValidator();
			this.vr = ((StepValidationResult) prt.vr);
			this.setBorder(BorderFactory.createEtchedBorder());
			
			this.showMatches.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (showMatches.isSelected()) {
						refreshDisplay(false);
					}
				}
			});
			this.highlightMatches.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					if (highlightMatches.isSelected()) {
						refreshDisplay(false);
					}
				}
			});
			
			ButtonGroup displayModeButtonGroup = new ButtonGroup();
			displayModeButtonGroup.add(this.showMatches);
			displayModeButtonGroup.add(this.highlightMatches);
			
			this.displayModePanel.add(this.showMatches);
			this.displayModePanel.add(this.highlightMatches);
			if (detailLabel != null) {
				this.detailLabelSwitch.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						showDetailLabel = detailLabelSwitch.isSelected();
						layoutTopPanel();
					}
				});
				this.displayModePanel.add(this.detailLabelSwitch);
			}
			
			
			//	initialize display
			this.annotationTable = new JTable();
//			this.annotationTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			this.annotationTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			this.annotationTable.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					
					int clickRowIndex = annotationTable.rowAtPoint(me.getPoint());
//					int rowIndex = annotationTable.getSelectedRow();
//					if ((clickRowIndex != -1) && ((clickRowIndex < rowIndex) || (clickRowIndex >= (rowIndex + annotationTable.getSelectedRowCount())))) {
//						ListSelectionModel lsm = annotationTable.getSelectionModel();
//						if (lsm != null) lsm.setSelectionInterval(clickRowIndex, clickRowIndex);
//					}
					
					if (me.getButton() != MouseEvent.BUTTON1)
						showContextMenu(me);
					
					else if ((me.getClickCount() > 1) && (annotationTable.getSelectedRowCount() == 1)) {
						AnnotationTray tray = annotationTrays[clickRowIndex];
						if (reactOnDoubleClick(tray)) {
							if (tray.isFailed()) {
//								System.out.println("  handling failed tray " + tray.failed.getValue());
//								if (tray.delete)
//									CriterionResultPanel.this.data.removeTokens(tray.annotation);
//								else if (tray.remove)
//									CriterionResultPanel.this.data.removeAnnotation(tray.annotation);
//								else if (tray.ignore)
//									vr.ignore(tray.failed);
//								System.out.println("  handling failed tray " + tray.failed.getValue());
								if (tray.delete)
									StepResultPanel.this.data.removeTokens(tray.annotation);
								else if (tray.remove)
									StepResultPanel.this.data.removeAnnotation(tray.annotation);
								else if (tray.ignore)
									vr.ignore(tray.failed);
							}
							refreshDisplay(true);
						}
					}
				}
			});
			
			final JTableHeader header = this.annotationTable.getTableHeader();
			header.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					int column = header.columnAtPoint(me.getPoint());
					if (column == 1)
						sortList(Annotation.START_INDEX_ATTRIBUTE);
					else if (column == 2)
						sortList(Annotation.SIZE_ATTRIBUTE);
					else if (column == 3)
						sortList(Annotation.ANNOTATION_VALUE_ATTRIBUTE);
				}
			});
			
			this.refreshDisplay(true);
			
			this.layoutTopPanel();
			this.add(this.topPanel, BorderLayout.NORTH);
			
//			
//			if (detailLabel == null)
//				this.add(displayModePanel, BorderLayout.NORTH);
//			else {
//				JPanel topPanel = new JPanel(new BorderLayout());
//				topPanel.add(detailLabel, BorderLayout.CENTER);
//				topPanel.add(displayModePanel, BorderLayout.SOUTH);
//				this.add(topPanel, BorderLayout.NORTH);
//			}
			
			JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
			this.add(annotationTableBox, BorderLayout.CENTER);
			
			isModified = false; // reset, so initial table refresh (building) does not count as modification
		}
		
		private void layoutTopPanel() {
			this.topPanel.removeAll();
			if ((detailLabel == null) || !showDetailLabel) {
				this.topPanel.add(defaultDetailLabel, BorderLayout.CENTER);
				this.topPanel.add(this.displayModePanel, BorderLayout.SOUTH);
			}
			else {
				this.topPanel.add(detailLabel, BorderLayout.CENTER);
				this.topPanel.add(this.displayModePanel, BorderLayout.SOUTH);
			}
			this.validate();
			this.repaint();
		}
		
		boolean isModified() {
			return this.isModified;
		}
		
		private void refreshDisplay(boolean reValidate) {
			
			//	do we need to apply the validator and rebuild the set of annotations?
			if (reValidate) {
				
				//	apply local validator
				this.vr = ((StepValidationResult) this.validator.validate(data));
				if ((defaultDetailLabel != null) && (defaultDetailLabel instanceof ValidationResultLabel))
					((ValidationResultLabel) defaultDetailLabel).validationResultChanged(this.vr);
				if ((detailLabel != null) && (detailLabel instanceof ValidationResultLabel))
					((ValidationResultLabel) detailLabel).validationResultChanged(this.vr);
				isModified = true;
				
				//	get instances and errors
				Annotation[] instances = this.vr.getInstances();
				Set instanceIDs = new HashSet();
				for (int i = 0; i < instances.length; i++)
					instanceIDs.add(instances[i].getAnnotationID());
				
				Annotation[] failed = this.vr.getFailed();
				Map failedByID = new HashMap();
				for (int f = 0; f < failed.length; f++)
					failedByID.put(failed[f].getAnnotationID(), failed[f]);
				
				MutableAnnotation[] annotations = data.getMutableAnnotations();
				ArrayList annotationList = new ArrayList();
				for (int a = 0; a < annotations.length; a++)
					if (instanceIDs.remove(annotations[a].getAnnotationID()))
						annotationList.add(annotations[a]);
				if (instanceIDs.remove(data.getAnnotationID()))
					annotationList.add(0, data); // can happen with a document root
				annotations = ((MutableAnnotation[]) annotationList.toArray(new MutableAnnotation[annotationList.size()]));
				
				this.allAnnotationTrays = new AnnotationTray[annotations.length];
				
				//	line up annotations
				for (int a = 0; a < annotations.length; a++) {
					if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
						this.allAnnotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
					
					else {
						this.allAnnotationTrays[a] = new AnnotationTray(annotations[a]);
						this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.allAnnotationTrays[a]);
					}
					this.allAnnotationTrays[a].failed = ((Annotation) failedByID.get(annotations[a].getAnnotationID()));
				}
				this.dialog.setTitle(this.originalTitle + " - " + annotations.length + " Annotations checked, " + failedByID.size() + " error(s)");
			}
			
			if (this.showMatches.isSelected()) {
				ArrayList failedAnnotationTrays = new ArrayList();
				for (int a = 0; a < this.allAnnotationTrays.length; a++) {
					if (this.allAnnotationTrays[a].isFailed())
						failedAnnotationTrays.add(this.allAnnotationTrays[a]);
				}
				this.annotationTrays = ((AnnotationTray[]) failedAnnotationTrays.toArray(new AnnotationTray[failedAnnotationTrays.size()]));
			}
			else {
				this.annotationTrays = new AnnotationTray[this.allAnnotationTrays.length];
				System.arraycopy(this.allAnnotationTrays, 0, this.annotationTrays, 0, this.annotationTrays.length);
//				this.annotationTrays = this.allAnnotationTrays;
			}
			
			//	sort
			this.sortList(this.sortField);
			
			//	update table
			this.annotationTable.setModel(new CriterionResultTableModel(this.annotationTrays, this.highlightMatches.isSelected()));
			this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(120);
			this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(50);
			this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(50);
			this.annotationTable.repaint();
			
			//	make changes visible
			this.validate();
		}
		
		private void sortList(final String sortField) {
			this.sortField = sortField;
			Arrays.sort(this.annotationTrays, new Comparator() {
				public int compare(Object o1, Object o2) {
					Annotation a1 = ((AnnotationTray) o1).annotation;
					Annotation a2 = ((AnnotationTray) o2).annotation;
					int c;
					
					//	handle value comparison specially (avoid building value string)
					if (Annotation.ANNOTATION_VALUE_ATTRIBUTE.equals(sortField)) {
						
						//	same start index and size ==> same tokens ==> same value
						if ((a1.getStartIndex() == a2.getStartIndex()) && (a1.size() == a2.size()))
							return 0;
						
						//	compare token by token
						int minSize = Math.min(a1.size(), a2.size());
						for (int t = 0; t < minSize; t++) {
							c = a1.valueAt(t).compareToIgnoreCase(a2.valueAt(t));
							if (c != 0)
								return c;
						}
						
						//	one annotation is prefix of the other ==> compare size
						return (a1.size() - a2.size());
					}
					
					//	other comparison
					else {
						String s1 = a1.getAttribute(sortField, "").toString();
						String s2 = a2.getAttribute(sortField, "").toString();
						
						//	try number comparison
						try {
							c = (Integer.parseInt(s1) - Integer.parseInt(s2));
							if (c != 0)
								return c;
						}
						
						//	do string comparison
						catch (NumberFormatException nfe) {
							c = s1.compareTo(s2);
							if (c != 0)
								return c;
						}
						
						return 0;
					}
				}
			});
			this.annotationTable.validate();
		}
		
		private class CriterionResultTableModel implements TableModel {
			private AnnotationTray[] annotations;
			private boolean highlightMatches = true;
			CriterionResultTableModel(AnnotationTray[] annotations, boolean highlightMatches) {
				this.annotations = annotations;
				this.highlightMatches = highlightMatches;
			}
			
			/** @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void addTableModelListener(TableModelListener l) {}
			
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
				Annotation annotation = this.annotations[rowIndex].annotation;
				if (this.highlightMatches && this.annotations[rowIndex].isFailed()) {
					String value = null;
					if (columnIndex == 0) value = annotation.getType();
					if (columnIndex == 1) value = "" + annotation.getStartIndex();
					if (columnIndex == 2) value = "" + annotation.size();
					if (columnIndex == 3) value = annotation.getValue();
					return ((value == null) ? null : ("<HTML><B>" + value + "</B></HTML>"));
				}
				else {
					if (columnIndex == 0) return annotation.getType();
					if (columnIndex == 1) return "" + annotation.getStartIndex();
					if (columnIndex == 2) return "" + annotation.size();
					if (columnIndex == 3) return annotation.getValue();
					return null;
				}
			}
			
			/** @see javax.swing.table.TableModel#isCellEditable(int, int)
			 */
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}
			
			/** @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
			 */
			public void removeTableModelListener(TableModelListener l) {}
			
			/** @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
			 */
			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
		}
		
		private void showContextMenu(MouseEvent me) {
			final int[] selectedRows = this.annotationTable.getSelectedRows();
			if (selectedRows.length == 0)
				return;
			
			Arrays.sort(selectedRows);
			
			final AnnotationTray[] trays = new AnnotationTray[selectedRows.length];
			for (int r = 0; r < selectedRows.length; r++)
				trays[r] = this.annotationTrays[selectedRows[r]];
			Arrays.sort(trays, new Comparator() {
				Comparator ano = AnnotationUtils.getComparator(Gamta.getAnnotationNestingOrder());
				public int compare(Object o1, Object o2) {
					AnnotationTray at1 = ((AnnotationTray) o1);
					AnnotationTray at2 = ((AnnotationTray) o2);
					return this.ano.compare(at1.annotation, at2.annotation);
				}
			});
			final AnnotationTray tray = trays[0];
			
			
			JPopupMenu menu = new JPopupMenu();
			JMenuItem mi = null;
			
			JMenuItem[] cMis = getContextMenuItems(trays);
			if (cMis != null) {
				for (int m = 0; m < cMis.length; m++) {
					mi = cMis[m];
					if (mi == null) {
						if (m != 0)
							menu.addSeparator();
					}
					else {
						ActionListener[] als = mi.getActionListeners();
						for (int l = 0; l < als.length; l++) {
//							System.out.println("ValidationResultPanel: got custom action listener " + als[l].getClass().getName());
							mi.removeActionListener(als[l]);
							mi.addActionListener(new CmiActionListener(als[l], trays));
						}
						menu.add(mi);
					}
				}
			}
			
			if (trays.length > 1) {
				if (mi != null)
					menu.addSeparator();
				
				mi = new JMenuItem("Modify Attribute");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						modifyAnnotationAttribute(annotationTable.getSelectedRows());
					}
				});
				menu.add(mi);
				mi = new JMenuItem("Remove Attribute");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						removeAnnotationAttribute(annotationTable.getSelectedRows());
					}
				});
				menu.add(mi);
			}
			if (mi != null)
				menu.addSeparator();
			
			
			boolean sameType = true;
			final String type = tray.annotation.getType();
			for (int r = 1; r < trays.length; r++)
				sameType = (sameType && trays[r].annotation.getType().equals(type));
			
			if (sameType) {
				mi = new JMenuItem("Rename");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String[] types = data.getAnnotationTypes();
						Arrays.sort(types, new Comparator() {
							public int compare(Object o1, Object o2) {
								String s1 = ((String) o1);
								String s2 = ((String) o2);
								int colon1 = s1.indexOf(':');
								int colon2 = s2.indexOf(':');
								if ((colon1 == -1) != (colon2 == -1)) // one of the types has a namespace prefix, the other has none
									return (colon1 - colon2); // keep generic types (without namespace prefix) before qualified ones
								return String.CASE_INSENSITIVE_ORDER.compare(s1, s2); // do normal comparison otherwise
							}
						});
						
						RenameAnnotationDialog rad = new RenameAnnotationDialog(type, types);
						rad.setVisible(true);
						if (rad.targetType == null) return;
						
						String newType = rad.targetType.trim();
						if ((newType.length() != 0) && !newType.equals(type)) {
							for (int r = 0; r < trays.length; r++)
								trays[r].annotation.changeTypeTo(newType);
							refreshDisplay(true);
						}
					}
				});
				menu.add(mi);
				menu.addSeparator();
			}
			
			mi = new JMenuItem("Accept As Is");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					boolean modified = false;
					for (int r = 0; r < trays.length; r++)
						if (vr.ignore(trays[r].failed))
							modified = true;
					if (modified) refreshDisplay(true);
				}
			});
			menu.add(mi);
			
			mi = new JMenuItem("Remove");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					for (int r = 0; r < trays.length; r++)
						data.removeAnnotation(trays[r].annotation);
					refreshDisplay(true);
				}
			});
			menu.add(mi);
			
			if (trays.length == 1) {
				mi = new JMenuItem("Remove All");
				mi.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						String value = tray.annotation.getValue();
						int annotationCount = 0;
						for (int a = 0; a < annotationTrays.length; a++)
							if (annotationTrays[a].annotation.getValue().equals(value)) {
								annotationCount ++;
								data.removeAnnotation(annotationTrays[a].annotation);
							}
						if (annotationCount > 0) refreshDisplay(true);
					}
				});
				menu.add(mi);
			}
			
			mi = new JMenuItem("Delete");
			mi.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					for (int r = trays.length; r > 0; r--)
						data.removeTokens(trays[r - 1].annotation);
					refreshDisplay(true);
				}
			});
			menu.add(mi);
			
			menu.show(this.annotationTable, me.getX(), me.getY());
		}
		
		private class CmiActionListener implements ActionListener {
			private ActionListener al;
			private AnnotationTray[] trays;
			CmiActionListener(ActionListener al, AnnotationTray[] trays) {
				this.al = al;
				this.trays = trays;
			}
			public void actionPerformed(ActionEvent ae) {
//				System.out.println("CmiActionListener: performing action ...");
				this.al.actionPerformed(ae);
//				System.out.println("  wrapped action performed, handling trays");
				for (int t = this.trays.length; t > 0; t--) {
					AnnotationTray tray = this.trays[t-1];
					if (tray.isFailed()) {
//						System.out.println("  handling failed tray " + tray.failed.getValue());
						if (tray.delete)
							data.removeTokens(tray.annotation);
						else if (tray.remove)
							data.removeAnnotation(tray.annotation);
						else if (tray.ignore)
							vr.ignore(tray.failed);
					}
				}
//				System.out.println("  trays done");
				refreshDisplay(true);
//				System.out.println("  display refreshed");
			}
		}
		
		private class RenameAnnotationDialog extends JDialog {
			
			private String targetType;
			private JComboBox targetTypeSelector;
			
			private boolean keyPressed = false;
			
			private RenameAnnotationDialog(String sourceType, String[] existingTypes) {
				super(dialog, "Rename Annotation", true);
				
				JLabel sourceTypeLabel = new JLabel(" " + sourceType);
				sourceTypeLabel.setBorder(BorderFactory.createLoweredBevelBorder());
				
				this.targetTypeSelector = new JComboBox(existingTypes);
				this.targetTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.targetTypeSelector.setEditable(true);
				this.targetTypeSelector.setSelectedItem(sourceType);
				this.targetTypeSelector.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (keyPressed && isVisible() && !targetTypeSelector.isPopupVisible()) commit();
					}
				});
				((JTextComponent) this.targetTypeSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						keyPressed = true;
					}
					public void keyReleased(KeyEvent ke) {
						keyPressed = false;
					}
				});
				
				JPanel selectorPanel = new JPanel(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.weighty = 0;
				gbc.gridwidth = 1;
				gbc.gridheight = 1;
				gbc.insets.top = 5;
				gbc.insets.left = 5;
				gbc.insets.right = 5;
				gbc.insets.bottom = 5;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				
				gbc.gridy = 0;
				gbc.gridx = 0;
				gbc.weightx = 0;
				selectorPanel.add(new JLabel("Current Annotation Type"), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				selectorPanel.add(sourceTypeLabel, gbc.clone());
				
				gbc.gridy = 1;
				gbc.gridx = 0;
				gbc.weightx = 0;
				selectorPanel.add(new JLabel("Rename Annotation To"), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				selectorPanel.add(this.targetTypeSelector, gbc.clone());
				
				//	initialize main buttons
				JButton commitButton = new JButton("Rename");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						commit();
					}
				});
				
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						RenameAnnotationDialog.this.dispose();
					}
				});
				
				JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				mainButtonPanel.add(commitButton);
				mainButtonPanel.add(abortButton);
				
				//	put the whole stuff together
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(selectorPanel, BorderLayout.CENTER);
				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
				
				this.setResizable(true);
				this.setSize(new Dimension(400, 120));
				this.setLocationRelativeTo(dialog);
			}
			
			private void commit() {
				Object item = targetTypeSelector.getSelectedItem();
				this.targetType = ((item == null) ? "" : item.toString());
				this.dispose();
			}
		}
		
		private void modifyAnnotationAttribute(int[] rows) {
			Annotation[] annotations = new Annotation[rows.length];
			for (int r = 0; r < rows.length; r++)
				annotations[r] = this.annotationTrays[rows[r]].annotation;
			if (annotations.length == 0) return;
			
			ModifyAttributeDialog mad = new ModifyAttributeDialog(annotations);
			mad.setVisible(true);
			
			if (mad.isModified()) refreshDisplay(true);
		}
		
		/**
		 * dialog for entering the parameters of an attribute modification operation
		 * 
		 * @author sautter
		 */
		private class ModifyAttributeDialog extends JDialog {
			
			private static final String allValues = "<All Values>";
			
			private Annotation[] annotations;
			
			private JComboBox attributeSelector;
			private JComboBox oldValueSelector;
			private JComboBox newValueSelector;
			
			private JRadioButton addButton = new JRadioButton("Add where not set", true);
			private JRadioButton setButton = new JRadioButton("Set everywhere");
			private JRadioButton changeButton = new JRadioButton("Change value above");
			
			private boolean isModified = false;
			private boolean keyPressed = false;
			
			ModifyAttributeDialog(Annotation[] annotations) {
				super(dialog, "Modify Annotation Attributes", true);
				this.annotations = annotations;
				
				//	initialize selector
				this.attributeSelector = new JComboBox();
				this.attributeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.attributeSelector.setEditable(true);
				this.attributeSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						attributeChanged();
					}
				});
				
				this.oldValueSelector = new JComboBox();
				this.oldValueSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.oldValueSelector.setEditable(false);
				this.oldValueSelector.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						oldValueChanged();
					}
				});
				
				this.newValueSelector = new JComboBox();
				this.newValueSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.newValueSelector.setEditable(true);
				this.newValueSelector.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						if (keyPressed && isVisible() && !newValueSelector.isPopupVisible()) modifyAttribute();
					}
				});
				((JTextComponent) this.newValueSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
					public void keyPressed(KeyEvent ke) {
						keyPressed = true;
					}
					public void keyReleased(KeyEvent ke) {
						keyPressed = false;
					}
				});
				
				
				StringVector attributeNameCollector = new StringVector();
				for (int a = 0; a < annotations.length; a++)
					attributeNameCollector.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
				attributeNameCollector.sortLexicographically(false, false);
				
				this.attributeSelector.removeAllItems();
				for (int i = 0; i < attributeNameCollector.size(); i++)
					this.attributeSelector.addItem(attributeNameCollector.get(i));
				
				JPanel selectorPanel = new JPanel(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.weighty = 1;
				gbc.gridwidth = 1;
				gbc.gridheight = 1;
				gbc.insets.top = 5;
				gbc.insets.left = 5;
				gbc.insets.right = 5;
				gbc.insets.bottom = 5;
				gbc.fill = GridBagConstraints.BOTH;
				
				gbc.gridy = 0;
				gbc.gridx = 0;
				gbc.weightx = 0;
				selectorPanel.add(new JLabel("Attribute To Modify"), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				selectorPanel.add(this.attributeSelector, gbc.clone());
				
				gbc.gridy = 1;
				gbc.gridx = 0;
				gbc.weightx = 0;
				selectorPanel.add(new JLabel("Old Attribute Value"), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				selectorPanel.add(this.oldValueSelector, gbc.clone());
				
				gbc.gridy = 2;
				gbc.gridx = 0;
				gbc.weightx = 0;
				selectorPanel.add(new JLabel("New Attribute Value"), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				selectorPanel.add(this.newValueSelector, gbc.clone());
				
				ButtonGroup bg = new ButtonGroup();
				bg.add(this.addButton);
				bg.add(this.setButton);
				bg.add(this.changeButton);
				JPanel modePanel = new JPanel(new GridLayout(1,3));
				modePanel.add(this.addButton);
				modePanel.add(this.setButton);
				modePanel.add(this.changeButton);
				gbc.gridy = 3;
				gbc.gridx = 0;
				gbc.weightx = 1;
				gbc.gridwidth = 2;
				selectorPanel.add(modePanel, gbc.clone());
				
				//	initialize main buttons
				JButton commitButton = new JButton("Modify");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						modifyAttribute();
					}
				});
				
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						ModifyAttributeDialog.this.dispose();
					}
				});
				
				JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				mainButtonPanel.add(commitButton);
				mainButtonPanel.add(abortButton);
				
				//	put the whole stuff together
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(selectorPanel, BorderLayout.CENTER);
				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
				
				this.setResizable(true);
				this.setSize(new Dimension(400, 180));
				this.setLocationRelativeTo(dialog);
			}
			
			private void attributeChanged() {
				Object attributeItem = this.attributeSelector.getSelectedItem();
				if (attributeItem != null) {
					String attribute = attributeItem.toString();
					StringVector attributeValues = new StringVector();
					
					for (int a = 0; a < this.annotations.length; a++) {
						if (this.annotations[a].hasAttribute(attribute))
							attributeValues.addElementIgnoreDuplicates(this.annotations[a].getAttribute(attribute).toString());
					}
					
					attributeValues.sortLexicographically(false, false);
					this.oldValueSelector.removeAllItems();
					this.oldValueSelector.addItem(allValues);
					for (int i = 0; i < attributeValues.size(); i++)
						this.oldValueSelector.addItem(attributeValues.get(i));
					
					attributeValues.sortLexicographically(false, false);
					this.newValueSelector.removeAllItems();
					for (int i = 0; i < attributeValues.size(); i++) {
						this.newValueSelector.addItem(attributeValues.get(i));
					}
				}
			}
			
			private void oldValueChanged() {
				Object item = this.oldValueSelector.getSelectedItem();
				if (item != null) this.newValueSelector.setSelectedItem(item);
			}
			
			boolean isModified() {
				return this.isModified;
			}
			
			private void modifyAttribute() {
				String attribute = this.attributeSelector.getSelectedItem().toString();
				Object oldValueObject = this.oldValueSelector.getSelectedItem();
				String oldValue = (allValues.equals(oldValueObject) ? null : oldValueObject.toString());
				Object newValueObject = this.newValueSelector.getSelectedItem();
				String newValue = ((newValueObject == null) ? "" : newValueObject.toString());
				
				if (this.addButton.isSelected()) {
					for (int a = 0; a < this.annotations.length; a++)
						if (!this.annotations[a].hasAttribute(attribute)) {
							this.annotations[a].setAttribute(attribute, newValue);
							this.isModified = true;
						}
				}
				
				else if (this.setButton.isSelected()) {
					for (int a = 0; a < this.annotations.length; a++) {
						this.annotations[a].setAttribute(attribute, newValue);
						this.isModified = true;
					}
				}
				
				else if (this.changeButton.isSelected()) {
					for (int a = 0; a < this.annotations.length; a++)
						if (oldValue.equals(this.annotations[a].getAttribute(attribute, ""))) {
							this.annotations[a].setAttribute(attribute, newValue);
							this.isModified = true;
						}
				}
				
				this.dispose();
			}
		}
		
		private void removeAnnotationAttribute(int[] rows) {
			Annotation[] annotations = new Annotation[rows.length];
			for (int r = 0; r < rows.length; r++)
				annotations[r] = this.annotationTrays[rows[r]].annotation;
			if (annotations.length == 0) return;
			
			RemoveAttributeDialog rad = new RemoveAttributeDialog(annotations);
			rad.setVisible(true);
			
			if (rad.isModified()) refreshDisplay(true);
		}
		
		/**
		 * dialog for entering the parameters of an attribute removal operation
		 * 
		 * @author sautter
		 */
		private class RemoveAttributeDialog extends JDialog {
			
			private static final String ALL_ATTRIBUTES_ATTRIBUTE = "<All Attributes>";
			
			private Annotation[] annotations;
			
			private JComboBox attributeSelector;
			
			private boolean isModified = false;
			
			RemoveAttributeDialog(Annotation[] annotations) {
				super(dialog, "Remove Annotation Attributes", true);
				this.annotations = annotations;
				
				this.attributeSelector = new JComboBox();
				this.attributeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.attributeSelector.setEditable(false);
				
				StringVector attributeNameCollector = new StringVector();
				for (int a = 0; a < this.annotations.length; a++)
					attributeNameCollector.addContentIgnoreDuplicates(this.annotations[a].getAttributeNames());
				attributeNameCollector.sortLexicographically(false, false);
				this.attributeSelector.removeAllItems();
				this.attributeSelector.addItem(ALL_ATTRIBUTES_ATTRIBUTE);
				for (int i = 0; i < attributeNameCollector.size(); i++)
					this.attributeSelector.addItem(attributeNameCollector.get(i));
				
				JPanel selectorPanel = new JPanel(new GridBagLayout());
				GridBagConstraints gbc = new GridBagConstraints();
				gbc.weighty = 1;
				gbc.gridwidth = 1;
				gbc.gridheight = 1;
				gbc.insets.top = 5;
				gbc.insets.left = 5;
				gbc.insets.right = 5;
				gbc.insets.bottom = 5;
				gbc.fill = GridBagConstraints.BOTH;
				
				gbc.gridy = 0;
				gbc.gridx = 0;
				gbc.weightx = 0;
				selectorPanel.add(new JLabel("Attribute Name"), gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				selectorPanel.add(this.attributeSelector, gbc.clone());
				
				//	initialize main buttons
				JButton commitButton = new JButton("Remove");
				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
				commitButton.setPreferredSize(new Dimension(100, 21));
				commitButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						removeAttribute();
					}
				});
				
				JButton abortButton = new JButton("Cancel");
				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				abortButton.setPreferredSize(new Dimension(100, 21));
				abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						RemoveAttributeDialog.this.dispose();
					}
				});
				
				JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				mainButtonPanel.add(commitButton);
				mainButtonPanel.add(abortButton);
				
				//	put the whole stuff together
				this.getContentPane().setLayout(new BorderLayout());
				this.getContentPane().add(selectorPanel, BorderLayout.CENTER);
				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
				
				this.setResizable(true);
				this.setSize(new Dimension(400, 100));
				this.setLocationRelativeTo(dialog);
			}
			
			boolean isModified() {
				return this.isModified;
			}
			
			private void removeAttribute() {
				Object attributeObject = this.attributeSelector.getSelectedItem();
				String attribute = (((attributeObject == null) || ALL_ATTRIBUTES_ATTRIBUTE.equals(attributeObject)) ? null : attributeObject.toString());
				for (int a = 0; a < this.annotations.length; a++) {
					if (attribute == null) {
						this.isModified = (this.isModified || (this.annotations[a].getAttributeNames().length != 0));
						this.annotations[a].clearAttributes();
					}
					else if (this.annotations[a].hasAttribute(attribute)) {
						this.annotations[a].removeAttribute(attribute);
						this.isModified = true;
					}
				}
				this.dispose();
			}
		}
	}
//	private class CriterionResultPanel extends JPanel {
//		
//		private JDialog dialog;
//		private String originalTitle;
//		private boolean isModified = false;
//		
//		private JTable annotationTable;
//		
//		private JRadioButton showMatches = new JRadioButton("Show Failing Annotations Only", false);
//		private JRadioButton highlightMatches = new JRadioButton("Highlight Failing Annotations", true);
//		
//		private MutableAnnotation data;
//		private AnnotationTray[] annotationTrays;
//		private AnnotationTray[] allAnnotationTrays;
//		private HashMap annotationTraysByID = new HashMap();
//		
//		private Validator validator;
//		private Criterion.CriterionValidationResult vr;
//		
//		CriterionResultPanel(JDialog dialog, MutableAnnotation data, PartialResultTray prt) {
//			super(new BorderLayout(), true);
//			this.dialog = dialog;
//			this.originalTitle = this.dialog.getTitle();
//			
//			this.data = data;
//			
//			this.validator = prt.vr.getValidator();
//			this.vr = ((Criterion.CriterionValidationResult) prt.vr);
//			this.setBorder(BorderFactory.createEtchedBorder());
//			
//			this.showMatches.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					if (showMatches.isSelected()) {
//						refreshDisplay(false);
//					}
//				}
//			});
//			this.highlightMatches.addItemListener(new ItemListener() {
//				public void itemStateChanged(ItemEvent ie) {
//					if (highlightMatches.isSelected()) {
//						refreshDisplay(false);
//					}
//				}
//			});
//			
//			ButtonGroup displayModeButtonGroup = new ButtonGroup();
//			displayModeButtonGroup.add(this.showMatches);
//			displayModeButtonGroup.add(this.highlightMatches);
//			
//			JPanel displayModePanel = new JPanel(new GridLayout(1, 2));
//			displayModePanel.add(this.showMatches);
//			displayModePanel.add(this.highlightMatches);
//			
//			//	initialize display
//			this.annotationTable = new JTable();
//			this.annotationTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
//			this.annotationTable.addMouseListener(new MouseAdapter() {
//				public void mouseClicked(MouseEvent me) {
//					
//					int clickRowIndex = annotationTable.rowAtPoint(me.getPoint());
//					int rowIndex = annotationTable.getSelectedRow();
//					if ((clickRowIndex != -1) && ((clickRowIndex < rowIndex) || (clickRowIndex >= (rowIndex + annotationTable.getSelectedRowCount())))) {
//						ListSelectionModel lsm = annotationTable.getSelectionModel();
//						if (lsm != null) lsm.setSelectionInterval(clickRowIndex, clickRowIndex);
//					}
//					
//					if (me.getButton() != MouseEvent.BUTTON1)
//						showContextMenu(me);
//					
//					else if ((me.getClickCount() > 1) && (annotationTable.getSelectedRowCount() == 1)) {
//						AnnotationTray tray = annotationTrays[clickRowIndex];
//						if (reactOnDoubleClick(tray)) {
//							if (tray.isFailed()) {
//								System.out.println("  handling failed tray " + tray.failed.getValue());
//								if (tray.delete)
//									CriterionResultPanel.this.data.removeTokens(tray.annotation);
//								else if (tray.remove)
//									CriterionResultPanel.this.data.removeAnnotation(tray.annotation);
//								else if (tray.ignore)
//									vr.ignore(tray.failed);
//							}
//							refreshDisplay(true);
//						}
//					}
//				}
//			});
//			this.refreshDisplay(true);
//			JScrollPane annotationTableBox = new JScrollPane(this.annotationTable);
//			
//			this.add(displayModePanel, BorderLayout.NORTH);
//			this.add(annotationTableBox, BorderLayout.CENTER);
//			
//			isModified = false; // reset, so initial table refresh (building) does not count as modification
//		}
//		
//		boolean isModified() {
//			return this.isModified;
//		}
//		
//		private void refreshDisplay(boolean reValidate) {
//			
//			//	do we need to apply the validator and rebuild the set of annotations?
//			if (reValidate) {
//				
//				//	apply local validator
//				this.vr = ((Criterion.CriterionValidationResult) this.validator.validate(data));
//				isModified = true;
//				
//				//	get instances and errors
//				Annotation[] instances = this.vr.getInstances();
//				Set instanceIDs = new HashSet();
//				for (int i = 0; i < instances.length; i++)
//					instanceIDs.add(instances[i].getAnnotationID());
//				
//				Annotation[] failed = this.vr.getFailed();
//				Map failedByID = new HashMap();
//				for (int f = 0; f < failed.length; f++)
//					failedByID.put(failed[f].getAnnotationID(), failed[f]);
//				
//				MutableAnnotation[] annotations = data.getMutableAnnotations();
//				ArrayList annotationList = new ArrayList();
//				for (int a = 0; a < annotations.length; a++)
//					if (instanceIDs.remove(annotations[a].getAnnotationID()))
//						annotationList.add(annotations[a]);
//				if (instanceIDs.remove(data.getAnnotationID()))
//					annotationList.add(0, data); // can happen with a document root
//				annotations = ((MutableAnnotation[]) annotationList.toArray(new MutableAnnotation[annotationList.size()]));
//				
//				this.allAnnotationTrays = new AnnotationTray[annotations.length];
//				
//				//	line up annotations
//				for (int a = 0; a < annotations.length; a++) {
//					if (this.annotationTraysByID.containsKey(annotations[a].getAnnotationID()))
//						this.allAnnotationTrays[a] = ((AnnotationTray) this.annotationTraysByID.get(annotations[a].getAnnotationID()));
//					
//					else {
//						this.allAnnotationTrays[a] = new AnnotationTray(annotations[a]);
//						this.annotationTraysByID.put(annotations[a].getAnnotationID(), this.allAnnotationTrays[a]);
//					}
//					this.allAnnotationTrays[a].failed = ((Annotation) failedByID.get(annotations[a].getAnnotationID()));
//				}
//				this.dialog.setTitle(this.originalTitle + " - " + annotations.length + " Annotations checked, " + failedByID.size() + " error(s)");
//			}
//			
//			if (this.showMatches.isSelected()) {
//				ArrayList failedAnnotationTrays = new ArrayList();
//				for (int a = 0; a < this.allAnnotationTrays.length; a++) {
//					if (this.allAnnotationTrays[a].isFailed())
//						failedAnnotationTrays.add(this.allAnnotationTrays[a]);
//				}
//				this.annotationTrays = ((AnnotationTray[]) failedAnnotationTrays.toArray(new AnnotationTray[failedAnnotationTrays.size()]));
//			}
//			else this.annotationTrays = this.allAnnotationTrays;
//			
//			//	update table
//			this.annotationTable.setModel(new CriterionResultTableModel(this.annotationTrays, this.highlightMatches.isSelected()));
//			this.annotationTable.getColumnModel().getColumn(0).setMaxWidth(120);
//			this.annotationTable.getColumnModel().getColumn(1).setMaxWidth(50);
//			this.annotationTable.getColumnModel().getColumn(2).setMaxWidth(50);
//			this.annotationTable.repaint();
//			
//			//	make changes visible
//			this.validate();
//		}
//		
//		private class CriterionResultTableModel implements TableModel {
//			private AnnotationTray[] annotations;
//			private boolean highlightMatches = true;
//			CriterionResultTableModel(AnnotationTray[] annotations, boolean highlightMatches) {
//				this.annotations = annotations;
//				this.highlightMatches = highlightMatches;
//			}
//			
//			/** @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
//			 */
//			public void addTableModelListener(TableModelListener l) {}
//			
//			/** @see javax.swing.table.TableModel#getColumnClass(int)
//			 */
//			public Class getColumnClass(int columnIndex) {
//				return String.class;
//			}
//			
//			/** @see javax.swing.table.TableModel#getColumnCount()
//			 */
//			public int getColumnCount() {
//				return 4;
//			}
//			
//			/** @see javax.swing.table.TableModel#getColumnName(int)
//			 */
//			public String getColumnName(int columnIndex) {
//				if (columnIndex == 0) return "Type";
//				if (columnIndex == 1) return "Start";
//				if (columnIndex == 2) return "Size";
//				if (columnIndex == 3) return "Value";
//				return null;
//			}
//			
//			/** @see javax.swing.table.TableModel#getRowCount()
//			 */
//			public int getRowCount() {
//				return this.annotations.length;
//			}
//			
//			/** @see javax.swing.table.TableModel#getValueAt(int, int)
//			 */
//			public Object getValueAt(int rowIndex, int columnIndex) {
//				Annotation annotation = this.annotations[rowIndex].annotation;
//				if (this.highlightMatches && this.annotations[rowIndex].isFailed()) {
//					String value = null;
//					if (columnIndex == 0) value = annotation.getType();
//					if (columnIndex == 1) value = "" + annotation.getStartIndex();
//					if (columnIndex == 2) value = "" + annotation.size();
//					if (columnIndex == 3) value = annotation.getValue();
//					return ((value == null) ? null : ("<HTML><B>" + value + "</B></HTML>"));
//				}
//				else {
//					if (columnIndex == 0) return annotation.getType();
//					if (columnIndex == 1) return "" + annotation.getStartIndex();
//					if (columnIndex == 2) return "" + annotation.size();
//					if (columnIndex == 3) return annotation.getValue();
//					return null;
//				}
//			}
//			
//			/** @see javax.swing.table.TableModel#isCellEditable(int, int)
//			 */
//			public boolean isCellEditable(int rowIndex, int columnIndex) {
//				return false;
//			}
//			
//			/** @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
//			 */
//			public void removeTableModelListener(TableModelListener l) {}
//			
//			/** @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
//			 */
//			public void setValueAt(Object newValue, int rowIndex, int columnIndex) {}
//		}
//		
//		private void showContextMenu(MouseEvent me) {
//			final int firstRow = this.annotationTable.getSelectedRow();
//			if (firstRow == -1) return;
//			
//			final int rowCount = this.annotationTable.getSelectedRowCount();
//			final AnnotationTray tray = this.annotationTrays[firstRow];
//			
//			AnnotationTray[] trays = new AnnotationTray[rowCount];
//			System.arraycopy(annotationTrays, firstRow, trays, 0, rowCount);
//			
//			JPopupMenu menu = new JPopupMenu();
//			JMenuItem mi = null;
//			
//			JMenuItem[] cMis = getContextMenuItems(trays);
//			if (cMis != null) {
//				for (int m = 0; m < cMis.length; m++) {
//					mi = cMis[m];
//					if (mi == null) {
//						if (m != 0)
//							menu.addSeparator();
//					}
//					else {
//						ActionListener[] als = mi.getActionListeners();
//						for (int l = 0; l < als.length; l++) {
////							System.out.println("ValidationResultPanel: got custom action listener " + als[l].getClass().getName());
//							mi.removeActionListener(als[l]);
//							mi.addActionListener(new CmiActionListener(als[l], trays));
//						}
//						menu.add(mi);
//					}
//				}
//			}
//			
//			if (rowCount > 1) {
//				if (mi != null)
//					menu.addSeparator();
//				
//				mi = new JMenuItem("Modify Attribute");
//				mi.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						modifyAnnotationAttribute(annotationTable.getSelectedRows());
//					}
//				});
//				menu.add(mi);
//				mi = new JMenuItem("Remove Attribute");
//				mi.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						removeAnnotationAttribute(annotationTable.getSelectedRows());
//					}
//				});
//				menu.add(mi);
//			}
//			if (mi != null)
//				menu.addSeparator();
//			
//			
//			boolean sameType = true;
//			final String type = tray.annotation.getType();
//			for (int r = 1; r < rowCount; r++)
//				sameType = (sameType && this.annotationTrays[firstRow + r].annotation.getType().equals(type));
//			
//			if (sameType) {
//				mi = new JMenuItem("Rename");
//				mi.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						String[] types = data.getAnnotationTypes();
//						Arrays.sort(types, new Comparator() {
//							public int compare(Object o1, Object o2) {
//								String s1 = ((String) o1);
//								String s2 = ((String) o2);
//								int colon1 = s1.indexOf(':');
//								int colon2 = s2.indexOf(':');
//								if ((colon1 == -1) != (colon2 == -1)) // one of the types has a namespace prefix, the other has none
//									return (colon1 - colon2); // keep generic types (without namespace prefix) before qualified ones
//								return String.CASE_INSENSITIVE_ORDER.compare(s1, s2); // do normal comparison otherwise
//							}
//						});
//						
//						RenameAnnotationDialog rad = new RenameAnnotationDialog(type, types);
//						rad.setVisible(true);
//						if (rad.targetType == null) return;
//						
//						String newType = rad.targetType.trim();
//						if ((newType.length() != 0) && !newType.equals(type)) {
//							for (int r = 0; r < rowCount; r++)
//								annotationTrays[firstRow + r].annotation.changeTypeTo(newType);
//							refreshDisplay(true);
//						}
//					}
//				});
//				menu.add(mi);
//				menu.addSeparator();
//			}
//			
////			mi = new JMenuItem("Ignore Criterion");
//			mi = new JMenuItem("Accept As Is");
//			mi.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					boolean modified = false;
//					for (int r = 0; r < rowCount; r++)
//						if (vr.ignore(annotationTrays[firstRow + r].failed))
//							modified = true;
//					if (modified) refreshDisplay(true);
//				}
//			});
//			menu.add(mi);
//			
//			mi = new JMenuItem("Remove");
//			mi.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					for (int r = 0; r < rowCount; r++)
//						data.removeAnnotation(annotationTrays[firstRow + r].annotation);
//					refreshDisplay(true);
//				}
//			});
//			menu.add(mi);
//			
//			if (rowCount == 1) {
//				mi = new JMenuItem("Remove All");
//				mi.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						String value = tray.annotation.getValue();
//						int annotationCount = 0;
//						for (int a = 0; a < annotationTrays.length; a++)
//							if (annotationTrays[a].annotation.getValue().equals(value)) {
//								annotationCount ++;
//								data.removeAnnotation(annotationTrays[a].annotation);
//							}
//						if (annotationCount > 0) refreshDisplay(true);
//					}
//				});
//				menu.add(mi);
//			}
//			
//			mi = new JMenuItem("Delete");
//			mi.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent ae) {
//					for (int r = rowCount; r > 0; r--)
//						data.removeTokens(annotationTrays[firstRow + r - 1].annotation);
//					refreshDisplay(true);
//				}
//			});
//			menu.add(mi);
//			
//			menu.show(this.annotationTable, me.getX(), me.getY());
//		}
//		
//		private class CmiActionListener implements ActionListener {
//			private ActionListener al;
//			private AnnotationTray[] trays;
//			CmiActionListener(ActionListener al, AnnotationTray[] trays) {
//				this.al = al;
//				this.trays = trays;
//			}
//			public void actionPerformed(ActionEvent ae) {
////				System.out.println("CmiActionListener: performing action ...");
//				this.al.actionPerformed(ae);
////				System.out.println("  wrapped action performed, handling trays");
//				for (int t = this.trays.length; t > 0; t--) {
//					AnnotationTray tray = this.trays[t-1];
//					if (tray.isFailed()) {
////						System.out.println("  handling failed tray " + tray.failed.getValue());
//						if (tray.delete)
//							data.removeTokens(tray.annotation);
//						else if (tray.remove)
//							data.removeAnnotation(tray.annotation);
//						else if (tray.ignore)
//							vr.ignore(tray.failed);
//					}
//				}
////				System.out.println("  trays done");
//				refreshDisplay(true);
////				System.out.println("  display refreshed");
//			}
//		}
//		
//		private class RenameAnnotationDialog extends JDialog {
//			
//			private String targetType;
//			private JComboBox targetTypeSelector;
//			
//			private boolean keyPressed = false;
//			
//			private RenameAnnotationDialog(String sourceType, String[] existingTypes) {
//				super(dialog, "Rename Annotation", true);
//				
//				JLabel sourceTypeLabel = new JLabel(" " + sourceType);
//				sourceTypeLabel.setBorder(BorderFactory.createLoweredBevelBorder());
//				
//				this.targetTypeSelector = new JComboBox(existingTypes);
//				this.targetTypeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.targetTypeSelector.setEditable(true);
//				this.targetTypeSelector.setSelectedItem(sourceType);
//				this.targetTypeSelector.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						if (keyPressed && isVisible() && !targetTypeSelector.isPopupVisible()) commit();
//					}
//				});
//				((JTextComponent) this.targetTypeSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
//					public void keyPressed(KeyEvent ke) {
//						keyPressed = true;
//					}
//					public void keyReleased(KeyEvent ke) {
//						keyPressed = false;
//					}
//				});
//				
//				JPanel selectorPanel = new JPanel(new GridBagLayout());
//				GridBagConstraints gbc = new GridBagConstraints();
//				gbc.weighty = 0;
//				gbc.gridwidth = 1;
//				gbc.gridheight = 1;
//				gbc.insets.top = 5;
//				gbc.insets.left = 5;
//				gbc.insets.right = 5;
//				gbc.insets.bottom = 5;
//				gbc.fill = GridBagConstraints.HORIZONTAL;
//				
//				gbc.gridy = 0;
//				gbc.gridx = 0;
//				gbc.weightx = 0;
//				selectorPanel.add(new JLabel("Current Annotation Type"), gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 1;
//				selectorPanel.add(sourceTypeLabel, gbc.clone());
//				
//				gbc.gridy = 1;
//				gbc.gridx = 0;
//				gbc.weightx = 0;
//				selectorPanel.add(new JLabel("Rename Annotation To"), gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 1;
//				selectorPanel.add(this.targetTypeSelector, gbc.clone());
//				
//				//	initialize main buttons
//				JButton commitButton = new JButton("Rename");
//				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				commitButton.setPreferredSize(new Dimension(100, 21));
//				commitButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						commit();
//					}
//				});
//				
//				JButton abortButton = new JButton("Cancel");
//				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				abortButton.setPreferredSize(new Dimension(100, 21));
//				abortButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						RenameAnnotationDialog.this.dispose();
//					}
//				});
//				
//				JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//				mainButtonPanel.add(commitButton);
//				mainButtonPanel.add(abortButton);
//				
//				//	put the whole stuff together
//				this.getContentPane().setLayout(new BorderLayout());
//				this.getContentPane().add(selectorPanel, BorderLayout.CENTER);
//				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
//				
//				this.setResizable(true);
//				this.setSize(new Dimension(400, 120));
//				this.setLocationRelativeTo(dialog);
//			}
//			
//			private void commit() {
//				Object item = targetTypeSelector.getSelectedItem();
//				this.targetType = ((item == null) ? "" : item.toString());
//				this.dispose();
//			}
//		}
//		
//		private void modifyAnnotationAttribute(int[] rows) {
//			Annotation[] annotations = new Annotation[rows.length];
//			for (int r = 0; r < rows.length; r++)
//				annotations[r] = this.annotationTrays[rows[r]].annotation;
//			if (annotations.length == 0) return;
//			
//			ModifyAttributeDialog mad = new ModifyAttributeDialog(annotations);
//			mad.setVisible(true);
//			
//			if (mad.isModified()) refreshDisplay(true);
//		}
//		
//		/**
//		 * dialog for entering the parameters of an attribute modification operation
//		 * 
//		 * @author sautter
//		 */
//		private class ModifyAttributeDialog extends JDialog {
//			
//			private static final String allValues = "<All Values>";
//			
//			private Annotation[] annotations;
//			
//			private JComboBox attributeSelector;
//			private JComboBox oldValueSelector;
//			private JComboBox newValueSelector;
//			
//			private JRadioButton addButton = new JRadioButton("Add where not set", true);
//			private JRadioButton setButton = new JRadioButton("Set everywhere");
//			private JRadioButton changeButton = new JRadioButton("Change value above");
//			
//			private boolean isModified = false;
//			private boolean keyPressed = false;
//			
//			ModifyAttributeDialog(Annotation[] annotations) {
//				super(dialog, "Modify Annotation Attributes", true);
//				this.annotations = annotations;
//				
//				//	initialize selector
//				this.attributeSelector = new JComboBox();
//				this.attributeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.attributeSelector.setEditable(true);
//				this.attributeSelector.addItemListener(new ItemListener() {
//					public void itemStateChanged(ItemEvent e) {
//						attributeChanged();
//					}
//				});
//				
//				this.oldValueSelector = new JComboBox();
//				this.oldValueSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.oldValueSelector.setEditable(false);
//				this.oldValueSelector.addItemListener(new ItemListener() {
//					public void itemStateChanged(ItemEvent e) {
//						oldValueChanged();
//					}
//				});
//				
//				this.newValueSelector = new JComboBox();
//				this.newValueSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.newValueSelector.setEditable(true);
//				this.newValueSelector.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent ae) {
//						if (keyPressed && isVisible() && !newValueSelector.isPopupVisible()) modifyAttribute();
//					}
//				});
//				((JTextComponent) this.newValueSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
//					public void keyPressed(KeyEvent ke) {
//						keyPressed = true;
//					}
//					public void keyReleased(KeyEvent ke) {
//						keyPressed = false;
//					}
//				});
//				
//				
//				StringVector attributeNameCollector = new StringVector();
//				for (int a = 0; a < annotations.length; a++)
//					attributeNameCollector.addContentIgnoreDuplicates(annotations[a].getAttributeNames());
//				attributeNameCollector.sortLexicographically(false, false);
//				
//				this.attributeSelector.removeAllItems();
//				for (int i = 0; i < attributeNameCollector.size(); i++)
//					this.attributeSelector.addItem(attributeNameCollector.get(i));
//				
//				JPanel selectorPanel = new JPanel(new GridBagLayout());
//				GridBagConstraints gbc = new GridBagConstraints();
//				gbc.weighty = 1;
//				gbc.gridwidth = 1;
//				gbc.gridheight = 1;
//				gbc.insets.top = 5;
//				gbc.insets.left = 5;
//				gbc.insets.right = 5;
//				gbc.insets.bottom = 5;
//				gbc.fill = GridBagConstraints.BOTH;
//				
//				gbc.gridy = 0;
//				gbc.gridx = 0;
//				gbc.weightx = 0;
//				selectorPanel.add(new JLabel("Attribute To Modify"), gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 1;
//				selectorPanel.add(this.attributeSelector, gbc.clone());
//				
//				gbc.gridy = 1;
//				gbc.gridx = 0;
//				gbc.weightx = 0;
//				selectorPanel.add(new JLabel("Old Attribute Value"), gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 1;
//				selectorPanel.add(this.oldValueSelector, gbc.clone());
//				
//				gbc.gridy = 2;
//				gbc.gridx = 0;
//				gbc.weightx = 0;
//				selectorPanel.add(new JLabel("New Attribute Value"), gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 1;
//				selectorPanel.add(this.newValueSelector, gbc.clone());
//				
//				ButtonGroup bg = new ButtonGroup();
//				bg.add(this.addButton);
//				bg.add(this.setButton);
//				bg.add(this.changeButton);
//				JPanel modePanel = new JPanel(new GridLayout(1,3));
//				modePanel.add(this.addButton);
//				modePanel.add(this.setButton);
//				modePanel.add(this.changeButton);
//				gbc.gridy = 3;
//				gbc.gridx = 0;
//				gbc.weightx = 1;
//				gbc.gridwidth = 2;
//				selectorPanel.add(modePanel, gbc.clone());
//				
//				//	initialize main buttons
//				JButton commitButton = new JButton("Modify");
//				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				commitButton.setPreferredSize(new Dimension(100, 21));
//				commitButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						modifyAttribute();
//					}
//				});
//				
//				JButton abortButton = new JButton("Cancel");
//				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				abortButton.setPreferredSize(new Dimension(100, 21));
//				abortButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						ModifyAttributeDialog.this.dispose();
//					}
//				});
//				
//				JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//				mainButtonPanel.add(commitButton);
//				mainButtonPanel.add(abortButton);
//				
//				//	put the whole stuff together
//				this.getContentPane().setLayout(new BorderLayout());
//				this.getContentPane().add(selectorPanel, BorderLayout.CENTER);
//				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
//				
//				this.setResizable(true);
//				this.setSize(new Dimension(400, 180));
//				this.setLocationRelativeTo(dialog);
//			}
//			
//			private void attributeChanged() {
//				Object attributeItem = this.attributeSelector.getSelectedItem();
//				if (attributeItem != null) {
//					String attribute = attributeItem.toString();
//					StringVector attributeValues = new StringVector();
//					
//					for (int a = 0; a < this.annotations.length; a++) {
//						if (this.annotations[a].hasAttribute(attribute))
//							attributeValues.addElementIgnoreDuplicates(this.annotations[a].getAttribute(attribute).toString());
//					}
//					
//					attributeValues.sortLexicographically(false, false);
//					this.oldValueSelector.removeAllItems();
//					this.oldValueSelector.addItem(allValues);
//					for (int i = 0; i < attributeValues.size(); i++)
//						this.oldValueSelector.addItem(attributeValues.get(i));
//					
//					attributeValues.sortLexicographically(false, false);
//					this.newValueSelector.removeAllItems();
//					for (int i = 0; i < attributeValues.size(); i++) {
//						this.newValueSelector.addItem(attributeValues.get(i));
//					}
//				}
//			}
//			
//			private void oldValueChanged() {
//				Object item = this.oldValueSelector.getSelectedItem();
//				if (item != null) this.newValueSelector.setSelectedItem(item);
//			}
//			
//			boolean isModified() {
//				return this.isModified;
//			}
//			
//			private void modifyAttribute() {
//				String attribute = this.attributeSelector.getSelectedItem().toString();
//				Object oldValueObject = this.oldValueSelector.getSelectedItem();
//				String oldValue = (allValues.equals(oldValueObject) ? null : oldValueObject.toString());
//				Object newValueObject = this.newValueSelector.getSelectedItem();
//				String newValue = ((newValueObject == null) ? "" : newValueObject.toString());
//				
//				if (this.addButton.isSelected()) {
//					for (int a = 0; a < this.annotations.length; a++)
//						if (!this.annotations[a].hasAttribute(attribute)) {
//							this.annotations[a].setAttribute(attribute, newValue);
//							this.isModified = true;
//						}
//				}
//				
//				else if (this.setButton.isSelected()) {
//					for (int a = 0; a < this.annotations.length; a++) {
//						this.annotations[a].setAttribute(attribute, newValue);
//						this.isModified = true;
//					}
//				}
//				
//				else if (this.changeButton.isSelected()) {
//					for (int a = 0; a < this.annotations.length; a++)
//						if (oldValue.equals(this.annotations[a].getAttribute(attribute, ""))) {
//							this.annotations[a].setAttribute(attribute, newValue);
//							this.isModified = true;
//						}
//				}
//				
//				this.dispose();
//			}
//		}
//		
//		private void removeAnnotationAttribute(int[] rows) {
//			Annotation[] annotations = new Annotation[rows.length];
//			for (int r = 0; r < rows.length; r++)
//				annotations[r] = this.annotationTrays[rows[r]].annotation;
//			if (annotations.length == 0) return;
//			
//			RemoveAttributeDialog rad = new RemoveAttributeDialog(annotations);
//			rad.setVisible(true);
//			
//			if (rad.isModified()) refreshDisplay(true);
//		}
//		
//		/**
//		 * dialog for entering the parameters of an attribute removal operation
//		 * 
//		 * @author sautter
//		 */
//		private class RemoveAttributeDialog extends JDialog {
//			
//			private static final String ALL_ATTRIBUTES_ATTRIBUTE = "<All Attributes>";
//			
//			private Annotation[] annotations;
//			
//			private JComboBox attributeSelector;
//			
//			private boolean isModified = false;
//			
//			RemoveAttributeDialog(Annotation[] annotations) {
//				super(dialog, "Remove Annotation Attributes", true);
//				this.annotations = annotations;
//				
//				this.attributeSelector = new JComboBox();
//				this.attributeSelector.setBorder(BorderFactory.createLoweredBevelBorder());
//				this.attributeSelector.setEditable(false);
//				
//				StringVector attributeNameCollector = new StringVector();
//				for (int a = 0; a < this.annotations.length; a++)
//					attributeNameCollector.addContentIgnoreDuplicates(this.annotations[a].getAttributeNames());
//				attributeNameCollector.sortLexicographically(false, false);
//				this.attributeSelector.removeAllItems();
//				this.attributeSelector.addItem(ALL_ATTRIBUTES_ATTRIBUTE);
//				for (int i = 0; i < attributeNameCollector.size(); i++)
//					this.attributeSelector.addItem(attributeNameCollector.get(i));
//				
//				JPanel selectorPanel = new JPanel(new GridBagLayout());
//				GridBagConstraints gbc = new GridBagConstraints();
//				gbc.weighty = 1;
//				gbc.gridwidth = 1;
//				gbc.gridheight = 1;
//				gbc.insets.top = 5;
//				gbc.insets.left = 5;
//				gbc.insets.right = 5;
//				gbc.insets.bottom = 5;
//				gbc.fill = GridBagConstraints.BOTH;
//				
//				gbc.gridy = 0;
//				gbc.gridx = 0;
//				gbc.weightx = 0;
//				selectorPanel.add(new JLabel("Attribute Name"), gbc.clone());
//				gbc.gridx = 1;
//				gbc.weightx = 1;
//				selectorPanel.add(this.attributeSelector, gbc.clone());
//				
//				//	initialize main buttons
//				JButton commitButton = new JButton("Remove");
//				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				commitButton.setPreferredSize(new Dimension(100, 21));
//				commitButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						removeAttribute();
//					}
//				});
//				
//				JButton abortButton = new JButton("Cancel");
//				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				abortButton.setPreferredSize(new Dimension(100, 21));
//				abortButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						RemoveAttributeDialog.this.dispose();
//					}
//				});
//				
//				JPanel mainButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//				mainButtonPanel.add(commitButton);
//				mainButtonPanel.add(abortButton);
//				
//				//	put the whole stuff together
//				this.getContentPane().setLayout(new BorderLayout());
//				this.getContentPane().add(selectorPanel, BorderLayout.CENTER);
//				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
//				
//				this.setResizable(true);
//				this.setSize(new Dimension(400, 100));
//				this.setLocationRelativeTo(dialog);
//			}
//			
//			boolean isModified() {
//				return this.isModified;
//			}
//			
//			private void removeAttribute() {
//				Object attributeObject = this.attributeSelector.getSelectedItem();
//				String attribute = (((attributeObject == null) || ALL_ATTRIBUTES_ATTRIBUTE.equals(attributeObject)) ? null : attributeObject.toString());
//				for (int a = 0; a < this.annotations.length; a++) {
//					if (attribute == null) {
//						this.isModified = (this.isModified || (this.annotations[a].getAttributeNames().length != 0));
//						this.annotations[a].clearAttributes();
//					}
//					else if (this.annotations[a].hasAttribute(attribute)) {
//						this.annotations[a].removeAttribute(attribute);
//						this.isModified = true;
//					}
//				}
//				this.dispose();
//			}
//		}
//	}
	
	/**
	 * Provide menu items for custom context menu functions in a step dialog's
	 * context menu. The argument array has at least one element. The annotation
	 * trays in the argument array are sorted in their content annotations'
	 * document order, but the content annotations do not necessarily cover a
	 * continuous part of the document. To add a separator bar to the context
	 * menu, set the respective field in the returned array to null. This
	 * default implementation returns null by default, so no special functions
	 * are offered. Sub classes are welcome to overwrite it as needed.
	 * @param selectedTrays an array holding the annotation trays currently
	 *            selected in the validation result detail dialog.
	 * @return an array holding menu items for custom context menu functions
	 */
	protected JMenuItem[] getContextMenuItems(AnnotationTray[] selectedTrays) {
		return null;
	}
	
	/**
	 * Perform a special double click action on an annotation tray. If this
	 * method modifies the annotation tray or its content in any way, it has to
	 * return true so the criterion dialog can refresh its display. This default
	 * implementation simply returns false, sub classes are welcome to overwrite
	 * it as needed.
	 * @param clickedTray the annotation tray that was double clicked
	 * @return true if the argument annotation tray or its content were modified
	 *         in any way, false otherwise
	 */
	protected boolean reactOnDoubleClick(AnnotationTray clickedTray) {
		return false;
	}
}
