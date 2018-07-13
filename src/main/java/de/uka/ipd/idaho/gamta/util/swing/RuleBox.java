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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.constants.LiteratureConstants;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathExpression;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.gPath.GPathVariableResolver;
import de.uka.ipd.idaho.gamta.util.gPath.exceptions.GPathException;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathNumber;
import de.uka.ipd.idaho.gamta.util.gPath.types.GPathString;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.IoTools;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * A rule box is a rule based classifier component that uses a series of GPath
 * expressions for classifying annotations into a (customizable) set of
 * categories, including a (customizable) default category, which is assigned if
 * none of the rules matches.<br>
 * A rule consists of a matching side (the left hand side, a GPath expression)
 * and a category side (the right hand side, one of the categories). It has the
 * form: &quot;<code>matching side ==> category side</code>&quot;, or more
 * specificylly &quot;<code>GPath-Expression ==> Category</code>&quot;. When
 * classifying a series of annotations instead of a single one, the rule box
 * supports three running variables, two on the matching side of the rule, and
 * one of the category side:
 * <ul>
 * <li><b>$position</b>: the position of the current annotation in the series
 * of annotations to classify. This variable can be used in the GPath expression
 * of the matching side of a rule.</li>
 * <li><b>$rule</b>: the number of the rule that classified the previous
 * annotation. This variable can be used in the GPath expression of the matching
 * side of a rule.</li>
 * <li><b>$category</b>: the category assigned to the previous annotation. This
 * variable can be used on the category side of a rule.</li>
 * </ul>
 * Using the editRules() method, the rule box allows for editing the rules in a
 * graphical interface. The editor opened by this method allows for creating,
 * editing, and testing GPath expressions, and for specifying the category they
 * imply.<br>
 * 
 * @author sautter
 */
public class RuleBox implements LiteratureConstants {
	
	/**
	 * representation of a single classification rule
	 * 
	 * @author sautter
	 */
	public static class Rule {
		public final String expressionString;
		public final GPathExpression predicate;
		public final String category;
		int position = 0;
		
		/**
		 * Constructor
		 * @param predicate the GPath expression to test for the rule to match
		 * @param category the category to assign in case of a match
		 */
		public Rule(String predicate, String category) {
			this(predicate, category, 0);
		}
		
		Rule(String predicate, String category, int position) {
			this.expressionString = predicate;
			this.predicate = GPathParser.parseExpression(predicate);
			this.category = category;
			this.position = position;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return (this.position + ". " + this.expressionString + " ==> " + this.category);
		}
		
		/**
		 * Convert the rule to a single line of text for easy storage. In
		 * contrast to the toString() method, this method is not intended for
		 * display, but for data persistence. It therefore omits the leading
		 * order number.
		 * @return a text representation of the rule for easy storage
		 */
		public String toDataString() {
			return (this.expressionString + " ==> " + this.category);
		}
		
		/**
		 * Parse the string representation of a Rule and create a Rule object
		 * from it. The argument string must have the form 'GPath-Expression ==>
		 * Catergory-String'. If the GPath expression is not valid, or the
		 * specified String does not include the implication arrow '==>', this
		 * method will throw an IllegalAgrumentException.
		 * @param ruleString the string representation of the rule
		 * @return a Rule object created from the specified string
		 *         representation
		 */
		public static Rule getRule(String ruleString) {
			//	find split
			int split = ruleString.indexOf("==>");
			if (split != -1) {
				
				//	generate rule
				String predicate = ruleString.substring(0, split).trim();
				String category = ruleString.substring(split + 3).trim();
				return new Rule(predicate, category);
			}
			else throw new IllegalArgumentException("'" + ruleString + "' is not a valid rule, '==>' missing.");
		}
	}
	
	private static final String previousRuleNumber = "$rule";
	private static final String previousCategory = "$category";
	private static final String previousCategoryLabel = "<Previous Category>";
	private static final String positionInSequence = "$position";
	
	private ArrayList rules = new ArrayList();
	
	private TreeSet categories = new TreeSet();
	private String defaultCategory = null;
	
	/**
	 * Constructor
	 * @param categories the categories to be available
	 * @param defaultCategory the initial default category
	 */
	public RuleBox(String[] categories, String defaultCategory) {
		for (int c = 0; c < categories.length; c++)
			this.addCategory(categories[c]);
		this.setDefaultCategory(defaultCategory);
	}
	
	/**
	 * Retrieve the rules currently present in the rule box
	 * @return an array holding the rules currently present in the rule box
	 */
	public Rule[] getRules() {
		return ((Rule[]) this.rules.toArray(new Rule[this.rules.size()]));
	}
	
	/**
	 * Parse the string representation of a Rule, create a Rule object from it,
	 * and add it to the rule box. The argument string must have the form
	 * 'GPath-Expression ==> Catergory-String'. If the GPath expression is not
	 * valid, or the specified String does not include the implication arrow
	 * '==>', this method will throw an IllegalAgrumentException.
	 * @param ruleString the string representation of the rule
	 * @see de.uka.ipd.idaho.gamta.util.swing.RuleBox.Rule#getRule(String)
	 */
	public void addRule(String ruleString) {
		Rule rule = Rule.getRule(ruleString);
		this.rules.add(rule);
		rule.position = this.rules.size();
	}
	
	/**
	 * Add a rule to the rule box
	 * @param rule the rule to add
	 */
	public void addRule(Rule rule) {
		this.rules.add(rule);
		rule.position = this.rules.size();
	}
	
	/**
	 * Remove a rule from ths rule box
	 * @param rule the Rule to remove
	 */
	public void removeRule(Rule rule) {
		this.rules.remove(rule);
		for (int r = 0; r < this.rules.size(); r++)
			((Rule) this.rules.get(r)).position = (r+1);
	}
	
	/**
	 * Retrieve the categories currently available in this rule box.
	 * @return an array holding the categories currently available in this rule
	 *         box
	 */
	public String[] getCategories() {
		return ((String[]) this.categories.toArray(new String[this.categories.size()]));
	}
	
	/**
	 * Add a category to the rule box
	 * @param category the category to add
	 */
	public void addCategory(String category) {
		if (category != null)
			this.categories.add(category);
	}
	
	/**
	 * Remove a category from the rule box. If the category removed is the
	 * current default category, this method has no effect.
	 * @param category the category to remove
	 */
	public void removeCategory(String category) {
		if ((category != null) && !category.equals(this.defaultCategory))
			this.categories.remove(category);
	}
	
	/**
	 * @return the default category
	 */
	public String getDefaultCategory() {
		return this.defaultCategory;
	}
	
	/**
	 * Set the default category. If the argument category is not yet known to
	 * the rule box, it is added.
	 * @param defaultCategory the new default category
	 */
	public void setDefaultCategory(String defaultCategory) {
		this.defaultCategory = defaultCategory;
		this.addCategory(defaultCategory);
	}
	
	/**
	 * Classify a series of subsequent annotations according to the rules
	 * residing in the RuleBox.
	 * @param annotations an array holding the annotations to classify
	 * @return an array holding the categories assigned to the specified
	 *         annotations. This array has the same length as the argument array
	 *         of annotations.
	 */
	public String[] classifyAnnotations(QueriableAnnotation[] annotations) {
		return this.classifyAnnotations(annotations, null);
	}
	
	/**
	 * Classify a series of subsequent annotations according to the rules
	 * residing in the RuleBox, taking into account prevoiusly set categories.
	 * The respective argument may be null. If not, and the entry for a specific
	 * annotation is not null, this category will be kept, thus the
	 * corresponding annotation will not be classifed by the rules. The matching
	 * rule index for the next annotation will be set to 0, the previous
	 * category to the respective string in the array.
	 * @param annotations an array holding the annotations to classify
	 * @param existingCategories an array holding existing categories of the
	 *            annotations to classify.
	 * @return an array holding the categories assigned to the specified
	 *         annotations. This array has the same length as the argument array
	 *         of annotations.
	 */
	public String[] classifyAnnotations(QueriableAnnotation[] annotations, String[] existingCategories) {
		
		/*	open dialog only if 
		 *  there are annotations 
		 *    and 
		 *  (
		 *    there are parts of the treatment not covered by divs 
		 *      or
		 *    there is only one treatment - possibly a correction on a single treatment that is already covered
		 *  )
		 */
		
		GPathVariableResolver variables = new GPathVariableResolver();
		variables.setVariable(previousRuleNumber, new GPathString("" + (this.rules.size() + 2)));
		variables.setVariable(previousCategory, new GPathString(this.defaultCategory));
		
		String[] categories = new String[annotations.length];
		for (int a = 0; a < annotations.length; a++) {
			
			//	get existing category
			String category = (((existingCategories != null) && (a < existingCategories.length)) ? existingCategories[a] : null);
			
			//	determine category
			if (category == null)
				categories[a] = this.classifyAnnotation(annotations[a], a, variables);
			
			//	determine category from annotation content
			else {
				categories[a] = category;
				variables.setVariable(previousRuleNumber, new GPathString("0"));
				variables.setVariable(previousCategory, new GPathString(category));
			}
		}
		
		//	return result
		return categories;
	}
	
	private String classifyAnnotation(QueriableAnnotation annotation, int position, GPathVariableResolver variables) {
		variables.setVariable(positionInSequence, new GPathNumber(position + 1));
		
		//	apply rules
		for (int r = 0; r < this.rules.size(); r++) {
			Rule rule = ((Rule) this.rules.get(r));
			try {
				if (GPath.evaluateExpression(rule.predicate, annotation, variables).asBoolean().value) {
					variables.setVariable(previousRuleNumber, new GPathString("" + rule.position));
					String category = rule.category;
					if (previousCategory.equals(category))
						category = variables.getVariable(previousCategory).asString().value;
					variables.setVariable(previousCategory, new GPathString(category));
					return category;
				}
			} catch (GPathException gpe) {}
		}
		
		//	no rule applied, set variables & return default
		variables.setVariable(previousRuleNumber, new GPathString("" + (this.rules.size() + 1)));
		variables.setVariable(previousCategory, new GPathString("" + this.defaultCategory));
		return this.defaultCategory;
	}
	
	/**
	 * Open a dialog for editing the rules of the rule box. This method is
	 * intended for situations where a simple dialog is required for editing
	 * rules of a rule box.
	 * @return true if the rules or other parts of the configuration were
	 *         modified, false otherwise
	 */
	public boolean editRules() {
		final JDialog rbcd = DialogFactory.produceDialog("Configure Rule Based Classifyer", true);
		final RuleBoxConfigPanel rbcp = new RuleBoxConfigPanel(this.rules, this.getCategories(), this.defaultCategory, this);
		final boolean[] edited = new boolean[1];
		
		//	initialize main buttons
		JButton commitButton = new JButton("OK");
		commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
		commitButton.setPreferredSize(new Dimension(100, 21));
		commitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				edited[0] = rbcp.commitChanges();
				rbcd.dispose();
			}
		});
		JButton abortButton = new JButton("Cancel");
		abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
		abortButton.setPreferredSize(new Dimension(100, 21));
		abortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				edited[0] = false;
				rbcd.dispose();
			}
		});
		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.add(commitButton);
		buttonPanel.add(abortButton);
		
		//	prepare dialog
		rbcd.getContentPane().setLayout(new BorderLayout());
		rbcd.getContentPane().add(rbcp, BorderLayout.CENTER);
		rbcd.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		rbcd.setSize(800, 600);
		rbcd.setLocationRelativeTo(null);
		
		//	show dialog
		rbcd.setVisible(true);
		
		//	return result
		return edited[0];
	}
	
	/**
	 * Obtain an editor panel for modifying the rules of the rule box. This
	 * method is intended for situations where the rule editor needs to be
	 * embedded in a larger piece of GUI. You have to invoke the commitChanges()
	 * method to make the modification take effect.
	 * @return true if the rules or other parts of the configuration were
	 *         modified, false otherwise
	 */
	public JPanel getRuleEditor() {
		return new RuleBoxConfigPanel(this.rules, this.getCategories(), this.defaultCategory, this);
	}
	
	private static final String UNMATCHED = "unmatched";
	
	/**
	 * Configuration panel for the rules contained in a rule box. If this editor
	 * is displayed in a dialog, you have to invoke the commitChanges() method
	 * to make the changes take effect.
	 * 
	 * @author sautter
	 */
	public static class RuleBoxConfigPanel extends JPanel {
		
		private class RuleListModel extends AbstractListModel {
			public Object getElementAt(int index) {
				return rules.get(index);
			}
			public int getSize() {
				return rules.size();
			}
			public void fireContentsChanged() {
				super.fireContentsChanged(this, 0, this.getSize());
			}
		}
		
		private RuleBox parent;
		
		private String[] categories;
		private JComboBox defaultCategorySelector;
		private String defaultCategory;
		
		private Vector rules = new Vector();
		private JList ruleList;
		private RuleListModel ruleListModel = new RuleListModel();
		
		private boolean dirty = false;
		
		private QueriableAnnotation[] testAnnotations = null;
		private String testAnnotationType = null;
		
		RuleBoxConfigPanel(ArrayList rules, String[] categories, String defaultCategory, /*JDialog dialog,*/ RuleBox parent)  {
			super(new BorderLayout(), true);
			
			//	remember parent
			this.parent = parent;
			
			//	remember data
			this.rules.addAll(rules);
			this.categories = categories;
			this.defaultCategory = defaultCategory;
			
			//	initialize rule list
			this.ruleList = new JList(this.ruleListModel);
			JScrollPane partListBox = new JScrollPane(this.ruleList);
			this.add(partListBox, BorderLayout.CENTER);
			this.ruleList.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						if (editRule())
							refreshRuleList();
					}
				}
			});
			
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
			
			JPanel editButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			JButton button;
			button = new JButton("Create Rule");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (createRule())
						refreshRuleList();
				}
			});
			editButtonPanel.add(button);
			button = new JButton("Clone Rule");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (cloneRule())
						refreshRuleList();
				}
			});
			editButtonPanel.add(button);
			button = new JButton("Edit Rule");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (editRule())
						refreshRuleList();
				}
			});
			editButtonPanel.add(button);
			button = new JButton("Remove Rule");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if (removeRule())
						refreshRuleList();
				}
			});
			editButtonPanel.add(button);
			
			//	initialize selector for default div type
			editButtonPanel.add(new JLabel("   Default Category"));
			this.defaultCategorySelector = new JComboBox(this.categories);
			this.defaultCategorySelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.defaultCategorySelector.setSelectedItem(this.defaultCategory);
			editButtonPanel.add(this.defaultCategorySelector);
			
			//	initialize test button
			button = new JButton("Test Rules");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(80, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					testRules();
				}
			});
			editButtonPanel.add(button);
			
			JScrollPane ruleListBox = new JScrollPane(this.ruleList);
			ruleListBox.getVerticalScrollBar().setUnitIncrement(25);
			
			this.add(reorderButtonPanel, BorderLayout.WEST);
			this.add(editButtonPanel, BorderLayout.NORTH);
			this.add(ruleListBox, BorderLayout.CENTER);
		}
		
		boolean isDirty() {
			return (this.dirty || !this.defaultCategory.equals(this.defaultCategorySelector.getSelectedItem()));
		}
		
		Rule[] getRules() {
			Rule[] rules = ((Rule[]) this.rules.toArray(new Rule[this.rules.size()]));
			for (int r = 0; r < rules.length; r++)
				rules[r].position = (r+1);
			return rules;
		}
		
		String getDefaultCategory() {
			return this.defaultCategorySelector.getSelectedItem().toString();
		}
		
		void moveUp() {
			int index = this.ruleList.getSelectedIndex();
			if (index > 0) {
				this.rules.insertElementAt(this.rules.remove(index - 1), index);
				this.refreshRuleList();
				this.ruleList.setSelectedIndex(index - 1);
				this.dirty = true;
			}
		}
		
		void moveDown() {
			int index = this.ruleList.getSelectedIndex();
			if ((index != -1) && ((index + 1) != this.rules.size())) {
				this.rules.insertElementAt(this.rules.remove(index), (index + 1));
				this.refreshRuleList();
				this.ruleList.setSelectedIndex(index + 1);
				this.dirty = true;
			}
		}
		
		boolean createRule() {
			return this.createRule(null);
		}
		
		boolean cloneRule() {
			int index = this.ruleList.getSelectedIndex();
			if (index == -1) return this.createRule();
			else return this.createRule((Rule) this.rules.get(index));
		}
		
		boolean createRule(Rule model) {
			Rule rule = this.editRule(model, ((model == null) ? "Create Rule" : "Clone Rule"), "Create");
			if (rule != null) {
				this.rules.add(rule);
				this.ruleListModel.fireContentsChanged();
				this.dirty = true;
				return true;
			}
			else return false;
		}
		
		boolean editRule() {
			int index = this.ruleList.getSelectedIndex();
			if (index != -1) {
				Rule rule = this.editRule(((Rule) this.rules.get(index)), "Edit Rule", "Edit");
				if (rule != null) {
					rule.position = (index + 1);
					this.rules.setElementAt(rule, index);
					this.ruleListModel.fireContentsChanged();
					this.dirty = true;
					return true;
				}
			}
			return false;
		}
		
		private Rule editRule(Rule rule, String title, String commitButtonText) {
			final JDialog red = DialogFactory.produceDialog("Rule Application Result", true);
			final Rule[] resultRule = new Rule[1];
			
			//	initialize editor
			final RuleEditorPanel editor = new RuleEditorPanel();
			if (rule != null)
				editor.setContent(rule);
			
			//	initialize main buttons
			JButton commitButton = new JButton(commitButtonText);
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					resultRule[0] = (editor.isDirty() ? editor.getContent() : null);
					red.dispose();
				}
			});
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					resultRule[0] = null;
					red.dispose();
				}
			});
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	put the whole stuff together
			red.getContentPane().setLayout(new BorderLayout());
			red.getContentPane().add(editor, BorderLayout.CENTER);
			red.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
			
			//	layout dialog
			red.setResizable(true);
			red.setSize(new Dimension(600, 400));
			red.setLocationRelativeTo(RuleBoxConfigPanel.this);
			
			//	show dialog
			red.setVisible(true);
			
			//	return result
			return resultRule[0];
		}
		
		boolean removeRule() {
			int index = this.ruleList.getSelectedIndex();
			if (index != -1) {
				this.rules.remove(index);
				this.dirty = true;
				return true;
			}
			return false;
		}
		
		void refreshRuleList() {
			for (int r = 0; r < this.rules.size(); r++)
				((Rule) this.rules.get(r)).position = (r+1);
			this.ruleListModel.fireContentsChanged();
			this.ruleList.validate();
		}
		
		String validateRule(String expression) {
			String error = GPathParser.validatePath(expression);
			if (error == null) {
				try {
					GPathParser.parseExpression(expression);
				}
				catch (GPathException gpe) {
					error = gpe.getMessage();
				}
			}
			return error;
		}
		
		void testRules() {
			this.getTestDocument();
			if (this.testAnnotations == null) return;
			
			Rule[] rules = this.getRules();
			HashSet[] ruleResultIDs = new HashSet[rules.length];
			
			//	validate & compile rules
			for (int r = 0; r < rules.length; r++) {
				ruleResultIDs[r] = new HashSet();
				
				String expression = rules[r].expressionString;
				expression = GPath.normalizePath(expression);
				String error = this.validateRule(expression);
				
				//	report error
				if (error != null) {
					JOptionPane.showMessageDialog(this, ("The expression of rule " + (r+1) + " is not valid:\n" + error), "Rule Validation", JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				//	compile expression
				else try {
					rules[r] = new Rule(expression, rules[r].category, rules[r].position);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(this, ("Error compiling expression of rule " + (r+1) + ":\n" + e.getMessage()), "Rule Compilation Error", JOptionPane.ERROR_MESSAGE);
					System.out.println(e.getClass().getName() + ": " + e.getMessage());
					e.printStackTrace(System.out);
					return;
				}
			}
			
			int noRuleAppliedPos = (rules.length + 2);
			int[] rulePositions = new int[this.testAnnotations.length];
			Arrays.fill(rulePositions, noRuleAppliedPos);
			String[] assignedTypes = new String[this.testAnnotations.length];
			Arrays.fill(assignedTypes, this.defaultCategory);
			
			GPathVariableResolver variables = new GPathVariableResolver();
			variables.setVariable(previousRuleNumber, new GPathString("" + (rules.length + 2)));
			variables.setVariable(previousCategory, new GPathString(this.defaultCategory));
			
			String[] categories = new String[this.testAnnotations.length];
			Arrays.fill(categories, null);
			int position = 0;
			for (int a = 0; a < this.testAnnotations.length; a++) {
				
				variables.setVariable(positionInSequence, new GPathNumber(position++ + 1));
				
				String pid = this.testAnnotations[a].getAnnotationID();
				int matchRulePosition = -1;
				
				//	apply rules
				for (int r = 0; r < rules.length; r++) {
					try {
						if (GPath.DEFAULT_ENGINE.evaluateExpression(rules[r].predicate, this.testAnnotations[a], variables).asBoolean().value) {
							ruleResultIDs[r].add(pid);
							if (matchRulePosition == -1) {
								matchRulePosition = r;
								if (previousCategory.equals(rules[matchRulePosition].category))
									categories[a] = variables.getVariable(previousCategory).asString().value;
								else categories[a] = rules[matchRulePosition].category;
							}
						}
					}
					catch (GPathException gpe) {
						JOptionPane.showMessageDialog(this, gpe.getMessage(), "GPath Error", JOptionPane.ERROR_MESSAGE);
					}
				}
				
				//	no rule applied, set variables & return default
				if (matchRulePosition == -1) {
					variables.setVariable(previousRuleNumber, new GPathString("" + (this.rules.size() + 1)));
					variables.setVariable(previousCategory, new GPathString(this.defaultCategory));
					categories[a] = this.defaultCategory;
				}
				else {
					variables.setVariable(previousRuleNumber, new GPathString("" + rules[matchRulePosition].position));
					variables.setVariable(previousCategory, new GPathString(categories[a]));
				}
			}
			
			//	build dialog
			final JDialog rrd = DialogFactory.produceDialog("Rule Application Result", true);
			RuleTestResultPanel panel = new RuleTestResultPanel();
			JScrollPane panelBox = new JScrollPane(panel);
			panelBox.getVerticalScrollBar().setUnitIncrement(50);
			panelBox.getVerticalScrollBar().setBlockIncrement(100);
			panelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			rrd.getContentPane().setLayout(new BorderLayout());
			rrd.getContentPane().add(panelBox, BorderLayout.CENTER);
			
			//	create result display boxes
			for (int a = 0; a < testAnnotations.length; a++) {
				Annotation annotation = testAnnotations[a];
				String pid = annotation.getAnnotationID();
				
				//	collect rules that matched current annotation
				ArrayList matchRules = new ArrayList();
				for (int r = 0; r < rules.length; r++)
					if (ruleResultIDs[r].contains(pid))
						matchRules.add(rules[r]);
				
				//	prepare annotation for display
				panel.addRuleTestResultLine(annotation, true, rules, ((Rule[]) matchRules.toArray(new Rule[matchRules.size()])), categories[a]);
			}
			
			//	add OK button
			JButton continueButton = new JButton("Close");
			continueButton.setBorder(BorderFactory.createRaisedBevelBorder());
			continueButton.setPreferredSize(new Dimension(100, 21));
			continueButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					rrd.dispose();
				}
			});
			rrd.getContentPane().add(continueButton, BorderLayout.SOUTH);
			
			//	ensure dialog is closed with button
			rrd.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
			
			//	get feedback
			rrd.setSize(500, (Math.min(650, (panel.getPreferredSize().height + 75))));
			rrd.setLocationRelativeTo(null);
			rrd.setVisible(true);
		}
		
		void getTestDocument() {
			if (this.testAnnotations == null) {
				QueriableAnnotation testDocument = Gamta.getTestDocument();
				if (testDocument != null) {
					System.out.println("Got test document");
					
					//	get type
					String[] types = testDocument.getAnnotationTypes();
					Object typeObject = JOptionPane.showInputDialog(this, "Please select the type of the annotations to classify.", "Select Target Type", JOptionPane.QUESTION_MESSAGE, null, types, this.testAnnotationType);
					if (typeObject == null) return;
					else this.testAnnotationType = typeObject.toString();
					
					//	get annotations
					this.testAnnotations = testDocument.getAnnotations(this.testAnnotationType);
					System.out.println(" - got " + this.testAnnotations.length + " test document annotations of type " + this.testAnnotationType);
				}
			}
		}
		
//		private class RuleEditorDialog extends JDialog {
//			
//			private Rule rule;
//			private RuleEditorPanel editor;
//			
//			RuleEditorDialog(Rule rule, String title, String commitButtonText) {
//				super(RuleBoxConfigPanel.this.dialog, title, true);
//				
//				//	initialize editor
//				this.editor = new RuleEditorPanel(this);
//				if (rule != null)
//					this.editor.setContent(rule);
//				
//				//	initialize main buttons
//				JButton commitButton = new JButton(commitButtonText);
//				commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				commitButton.setPreferredSize(new Dimension(100, 21));
//				commitButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						RuleEditorDialog.this.rule = RuleEditorDialog.this.editor.getContent();
//						RuleEditorDialog.this.dispose();
//					}
//				});
//				JButton abortButton = new JButton("Cancel");
//				abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
//				abortButton.setPreferredSize(new Dimension(100, 21));
//				abortButton.addActionListener(new ActionListener() {
//					public void actionPerformed(ActionEvent e) {
//						RuleEditorDialog.this.rule = null;
//						RuleEditorDialog.this.dispose();
//					}
//				});
//				JPanel mainButtonPanel = new JPanel(new FlowLayout());
//				mainButtonPanel.add(commitButton);
//				mainButtonPanel.add(abortButton);
//				
//				//	put the whole stuff together
//				this.getContentPane().setLayout(new BorderLayout());
//				this.getContentPane().add(this.editor, BorderLayout.CENTER);
//				this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
//				
//				this.setResizable(true);
//				this.setSize(new Dimension(600, 400));
//				this.setLocationRelativeTo(RuleBoxConfigPanel.this);
//			}
//			
//			private boolean isCommitted() {
//				return (this.rule != null);
//			}
//			
//			private Rule getRule() {
//				return this.rule;
//			}
//			
//			private boolean isDirty() {
//				return this.editor.isDirty();
//			}
//		}
		
		private class RuleEditorPanel extends JPanel implements DocumentListener {
			
			private static final int MAX_SCROLLBAR_WAIT = 200;
			
			private JTextArea editor = new JTextArea();
			private JScrollPane editorBox;
			
			private String content = "";
			private boolean editorDirty = false;
			
			private JComboBox categorySelector;
			private String category = RuleBoxConfigPanel.this.defaultCategory;
			
//			private JDialog parentDialog;
			
			RuleEditorPanel(/*JDialog parentDialog*/) {
				super(new BorderLayout(), true);
//				this.parentDialog = parentDialog;
				
				//	initialize editor
				this.editor.setEditable(true);
				this.editor.getDocument().addDocumentListener(this);
				
				//	wrap editor in scroll pane
				this.editorBox = new JScrollPane(this.editor);
				
				//	initialize div type selector
				this.categorySelector = new JComboBox(categories);
				this.categorySelector.addItem(previousCategoryLabel);
				this.categorySelector.setBorder(BorderFactory.createLoweredBevelBorder());
				this.categorySelector.setSelectedItem(this.category);
				
				//	initialize buttons
				JButton refreshButton = new JButton("Refresh");
				refreshButton.setBorder(BorderFactory.createRaisedBevelBorder());
				refreshButton.setPreferredSize(new Dimension(115, 21));
				refreshButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						refreshPredicate();
					}
				});
				
				JButton validateButton = new JButton("Validate");
				validateButton.setBorder(BorderFactory.createRaisedBevelBorder());
				validateButton.setPreferredSize(new Dimension(115, 21));
				validateButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						validateRule();
					}
				});
				
				JButton testButton = new JButton("Test");
				testButton.setBorder(BorderFactory.createRaisedBevelBorder());
				testButton.setPreferredSize(new Dimension(115, 21));
				testButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						testRule();
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
				gbc.gridx ++;
				buttonPanel.add(new JLabel("Div Type", JLabel.RIGHT), gbc.clone());
				gbc.gridx ++;
				buttonPanel.add(this.categorySelector, gbc.clone());
				
				//	put the whole stuff together
				this.add(this.editorBox, BorderLayout.CENTER);
				this.add(buttonPanel, BorderLayout.SOUTH);
			}
			
			Rule getContent() {
				if (this.isDirty()) this.content = GPath.normalizePath(this.editor.getText());
				String category = this.categorySelector.getSelectedItem().toString();
				if (previousCategoryLabel.equals(category)) category = previousCategory;
				return new Rule(this.content, category);
			}
			
			void setContent(Rule rule) {
				this.content = GPath.normalizePath(rule.expressionString);
				this.refreshDisplay();
				this.editorDirty = false;
				this.categorySelector.setSelectedItem(previousCategory.equals(rule.category) ? previousCategoryLabel : rule.category);
			}
			
			boolean isDirty() {
				return (this.editorDirty || !this.category.equals(this.categorySelector.getSelectedItem().toString()));
			}
			
			void refreshPredicate() {
				String gPath = this.editor.getText();
				if ((gPath != null) && (gPath.length() != 0)) {
					
					final JScrollBar scroller = this.editorBox.getVerticalScrollBar();
					final int scrollPosition = scroller.getValue();
					
					String normalizedGPath = GPath.normalizePath(gPath);
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
			
			void validateRule() {
				boolean selected = true;
				String expression = this.editor.getSelectedText();
				if ((expression == null) || (expression.length() == 0)) {
					expression = this.editor.getText();
					selected = false;
				}
				expression = GPath.normalizePath(expression);
				String error = RuleBoxConfigPanel.this.validateRule(expression);
				
				if (error == null)
					JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is valid."), "Rule Validation", JOptionPane.INFORMATION_MESSAGE);
				else JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "Rule Validation", JOptionPane.ERROR_MESSAGE);
			}
			
			void testRule() {
				boolean selected = true;
				String expression = this.editor.getSelectedText();
				if ((expression == null) || (expression.length() == 0)) {
					expression = this.editor.getText();
					selected = false;
				}
				expression = GPath.normalizePath(expression);
				String error = RuleBoxConfigPanel.this.validateRule(expression);
				if (error != null) {
					JOptionPane.showMessageDialog(this, ("The " + (selected ? "selected expression part" : "expression") + " is not valid:\n" + error), "Rule Validation", JOptionPane.ERROR_MESSAGE);
					return;
				}
				else {
					getTestDocument();
					if (testAnnotations != null) {
						
						Rule rule = this.getContent();
						
						//	compile expression
						try {
							rule = new Rule(expression, rule.category, rule.position);
						}
						catch (Exception e) {
							JOptionPane.showMessageDialog(this, ("Error compiling expression of rule:\n" + e.getMessage()), "Rule Compilation Error", JOptionPane.ERROR_MESSAGE);
							System.out.println(e.getClass().getName() + ": " + e.getMessage());
							e.printStackTrace(System.out);
							return;
						}
						
						//	apply rule
						GPathVariableResolver variables = new GPathVariableResolver();
						variables.setVariable(previousRuleNumber, new GPathString("" + (rules.size() + 2)));
						variables.setVariable(previousCategory, new GPathString(defaultCategory));
						
						String[] categories = new String[testAnnotations.length];
						Arrays.fill(categories, null);
						int position = 0;
						if (previousCategory.equals(rule.category))
							rule = new Rule(rule.expressionString, defaultCategory, rule.position);
						
						try {
							for (int a = 0; a < testAnnotations.length; a++) {
								variables.setVariable(positionInSequence, new GPathNumber(position++ + 1));
								
								if (GPath.DEFAULT_ENGINE.evaluateExpression(rule.predicate, testAnnotations[a], variables).asBoolean().value) {
									categories[a] = rule.category;
									
									variables.setVariable(previousRuleNumber, new GPathString("" + rule.position));
									variables.setVariable(previousCategory, new GPathString(categories[a]));
								}
								
								//	no rule applied, set variables & return default
								else {
									variables.setVariable(previousRuleNumber, new GPathString("" + (rules.size() + 2)));
									variables.setVariable(previousCategory, new GPathString(defaultCategory));
								}
							}
						}
						catch (GPathException gpe) {
							JOptionPane.showMessageDialog(this, ("Error evaluating expression of rule:\n" + gpe.getMessage()), "Rule Evaluation Error", JOptionPane.ERROR_MESSAGE);
							System.out.println(gpe.getClass().getName() + ": " + gpe.getMessage());
							gpe.printStackTrace(System.out);
							return;
						}
						
						//	set up rule arrays
						Rule[] rules = {rule};
						Rule[] matchRules = {rule};
						Rule[] noMatchRules = {};
						
						//	build dialog
						final JDialog mrd = DialogFactory.produceDialog(("Match Result for " + rule.toString()), true);
						RuleTestResultPanel panel = new RuleTestResultPanel();
						JScrollPane panelBox = new JScrollPane(panel);
						panelBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						mrd.getContentPane().setLayout(new BorderLayout());
						mrd.getContentPane().add(panelBox, BorderLayout.CENTER);
						
						//	create result display boxes
						for (int a = 0; a < testAnnotations.length; a++) {
							Annotation annotation = testAnnotations[a];
							
							//	prepare annotation for display
							panel.addRuleTestResultLine(annotation, false, rules, ((categories[a] == null) ? matchRules : noMatchRules), ((categories[a] == null) ? UNMATCHED : rule.category));
						}
						
						//	add OK button
						JButton continueButton = new JButton("Close");
						continueButton.setBorder(BorderFactory.createRaisedBevelBorder());
						continueButton.setPreferredSize(new Dimension(100, 21));
						continueButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent ae) {
								mrd.dispose();
							}
						});
						mrd.getContentPane().add(continueButton, BorderLayout.SOUTH);
						
						//	ensure dialog is closed with button
						mrd.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
						
						//	get feedback
						mrd.setSize(500, (Math.min(650, (panel.getPreferredSize().height + 75))));
						mrd.setLocationRelativeTo(null);
						mrd.setVisible(true);
					}
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
				this.editorDirty = true;
			}
			
			/** @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
			 */
			public void removeUpdate(DocumentEvent e) {
				this.editorDirty = true;
			}
		}
		
		private class RuleTestResultPanel extends JPanel implements Scrollable {
			private GridBagConstraints gbc = new GridBagConstraints();
			RuleTestResultPanel() {
				super(new GridBagLayout(), true);
				gbc.insets.top = 2;
				gbc.insets.bottom = 2;
				gbc.insets.left = 5;
				gbc.insets.right = 5;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				gbc.weightx = 1;
				gbc.weighty = 0;
				gbc.gridwidth = 1;
				gbc.gridheight = 1;
				gbc.gridx = 0;
				gbc.gridy = 0;
			}
			
			public Dimension getPreferredScrollableViewportSize() {
				return this.getPreferredSize();
			}
			public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
				return 1;
			}
			public boolean getScrollableTracksViewportHeight() {
				return false;
			}
			public boolean getScrollableTracksViewportWidth() {
				return true;
			}
			public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
				return 20;
			}
			
			void addRuleTestResultLine(Annotation annotation, boolean isMultiRuleTest, Rule[] rules, Rule[] matchRules, String category) {
				if (this.gbc.gridy >= 512) return; // catches 512 element limit of GridBagLayout
				this.add(new RuleTestResultLine(annotation, isMultiRuleTest, rules, matchRules, category), this.gbc.clone());
				this.gbc.gridy++;
			}
			
			private class RuleTestResultLine extends JPanel {
				RuleTestResultLine(Annotation annotation, boolean isMultiRuleTest, Rule[] rules, Rule[] matchRules, String category) {
					super(new BorderLayout(), true);
					this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(3,5,3,5)));
					
					//	display classfication result
					JLabel categoryDisplay = new JLabel(category);
					
					//	prepare match overview for multi rule tests only
					if (isMultiRuleTest) {
						
						StringVector ruleMatchLines = new StringVector();
						int mri = 0;
						
						for (int r = 0; r < rules.length; r++) {
							String ruleString = IoTools.prepareForHtml(rules[r].toString(), new Properties() {
								public String getProperty(String key, String defaultValue) {
									if ("<>\"&".indexOf(key) == -1) return key;
									else return super.getProperty(key, defaultValue);
								}
								public String getProperty(String key) {
									if ("<>\"&".indexOf(key) == -1) return key;
									else return super.getProperty(key);
								}
								public synchronized boolean containsKey(Object key) {
									return ("<>\"&".indexOf(key.toString()) == -1);
								}
							});
							if ((mri < matchRules.length) && rules[r].toString().equals(matchRules[mri].toString())) {
								if (mri == 0) ruleMatchLines.addElement(" - <B>" + ruleString + "</B>");
								else ruleMatchLines.addElement(" - <B><I>" + ruleString + "</I></B>");
								mri++;
							}
							else ruleMatchLines.addElement(" - " + ruleString);
						}
						
						if (mri == 0) ruleMatchLines.addElement(" - <B>==>" + RuleBoxConfigPanel.this.defaultCategory + "</B>");
						else ruleMatchLines.addElement(" - ==>" + RuleBoxConfigPanel.this.defaultCategory);
						
						final String ruleMatchMessage = ("<HTML>Rule match result for this annotation (first match bold, other matches in bold italics):<BR>" + ruleMatchLines.concatStrings("<BR>") + "</HTML>");
						
						categoryDisplay.addMouseListener(new MouseAdapter() {
							public void mouseClicked(MouseEvent e) {
								JOptionPane.showMessageDialog(RuleTestResultLine.this, ruleMatchMessage, "Rule Match Overview", JOptionPane.INFORMATION_MESSAGE);
							}
						});
					}
					
//					String pageNumber = annotation.getAttribute(PAGE_NUMBER_ATTRIBUTE, "").toString().trim();
					Object pageNumberObject = annotation.getAttribute(PAGE_NUMBER_ATTRIBUTE);
					String pageNumber = ((pageNumberObject == null) ? "-1" : pageNumberObject.toString().trim());
					
					JLabel display = new JLabel("<HTML>" + annotation.getValue() + "</HTML>");
					display.setOpaque(true);
					display.setBackground(Color.WHITE);
					display.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEmptyBorder(3,5,3,5)));
					
					JPanel categoryPanel = new JPanel(new BorderLayout());
					categoryPanel.add(categoryDisplay, BorderLayout.NORTH);
					if (pageNumber.length() == 0)
						categoryPanel.add(new JLabel("<Unknown Page>"), BorderLayout.SOUTH);
					else categoryPanel.add(new JLabel("Page " + pageNumber), BorderLayout.SOUTH);
					
					categoryPanel.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
					this.add(categoryPanel, BorderLayout.WEST);
					
					display.setBackground(getCategoryColor(category, RuleBoxConfigPanel.this.getDefaultCategory()));
					this.add(display, BorderLayout.CENTER);
				}
			}
		}
		
		
		/**
		 * Commit all changes made in the editor panel to the parent rule box
		 * @return true if the parent rule box was modified as an effect of the
		 *         commit
		 */
		public boolean commitChanges() {
			if (this.isDirty()) {
				this.parent.defaultCategory = this.getDefaultCategory();
				Rule[] rules = this.getRules();
				this.parent.rules.clear();
				for (int r = 0; r < rules.length; r++)
					this.parent.rules.add(rules[r]);
				return true;
			}
			else return false;
		}
	}
	
	private static HashMap categoryColors = new HashMap();
	private static Color getCategoryColor(String category, String defaultCategory) {
		if (UNMATCHED.equals(category))
			return Color.LIGHT_GRAY;
		
		else if ((defaultCategory != null) && defaultCategory.equals(category))
			return Color.WHITE;
		
		Color categoryColor = ((Color) categoryColors.get(category));
		if (categoryColor == null) {
			categoryColor = new Color(Color.HSBtoRGB(((float) Math.random()), 0.5f, 1.0f));
			categoryColors.put(category, categoryColor);
		}
		return categoryColor;
	}
}
