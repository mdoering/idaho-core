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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;
import de.uka.ipd.idaho.gamta.util.gPath.GPath;
import de.uka.ipd.idaho.gamta.util.gPath.GPathEditorPanel;
import de.uka.ipd.idaho.gamta.util.gPath.GPathParser;
import de.uka.ipd.idaho.gamta.util.swing.AnnotationDisplayDialog;
import de.uka.ipd.idaho.gamta.util.swing.DialogFactory;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.accessories.RegExEditorPanel;

/**
 * A single parameter in a ParameterConfigPanel or an AnnotationTypeConfigPanel.
 * 
 * @author sautter
 */
public abstract class AnalyzerParameter {
	
	/**
	 * Object that can store and persist parameters.
	 * 
	 * @author sautter
	 */
	public static interface Store {
		/**
		 * Store a parameter. Setting a parameter to null should remove it.
		 * @param name the name of the parameter
		 * @param value the value of the parameter
		 * @return the value this parameter was previously set to, or null, if there
		 *         is no such value
		 */
		public abstract String storeParameter(String name, String value);
		
		/**
		 * write parameters to a persistent storage location
		 */
		public abstract void persistParameters();
	}
	
	/**
	 * Object that provides parameters.
	 * 
	 * @author sautter
	 */
	public static interface Source {
		
		/**
		 * Retrieve a parameter.
		 * @param name the name of the parameter
		 * @return the value of the parameter, or null, if the parameter with the
		 *         specified name is not set
		 */
		public abstract String getParameter(String name);
		
		/**
		 * Retrieve a parameter.
		 * @param name the name of the parameter
		 * @param def the default value to be returned if the parameter is not set
		 * @return the value of the parameter, or def, if the parameter with the
		 *         specified name is not set
		 */
		public abstract String getParameter(String name, String def);
	}
	
	/**
	 * Bundle of source and store, to allow for easier anonymous implementations.
	 * 
	 * @author sautter
	 */
	public static interface Provider extends Source, Store {}
	
	/**
	 * Default storage object for parameters.
	 * 
	 * @author sautter
	 */
	public static class DefaultStore implements Provider {
		private AnalyzerDataProvider dataProvider;
		private String parameterDataName;
		
		/**
		 * Constructor
		 * @param dataProvider the data provider to use for storing parameters
		 * @param parameterDataName the name of the data object to store parameters to
		 */
		public DefaultStore(AnalyzerDataProvider dataProvider, String parameterDataName) {
			this.dataProvider = dataProvider;
			this.parameterDataName = parameterDataName;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerParameter.Source#getParameter(java.lang.String, java.lang.String)
		 */
		public String getParameter(String name, String def) {
			if (this.parameters == null)
				return def;
			return this.parameters.getProperty(name, def);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerParameter.Source#getParameter(java.lang.String)
		 */
		public String getParameter(String name) {
			return this.getParameter(name, null);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerParameter.Store#storeParameter(java.lang.String, java.lang.String)
		 */
		public String storeParameter(String name, String value) {
			if (value == null) {
				this.parameterNames.removeAll(name);
				return ((String) this.parameters.remove(name));
			}
			String oldValue = this.parameters.getProperty(name);
			if ((oldValue != null) && oldValue.equals(value))
				return value;
			this.parameterNames.addElementIgnoreDuplicates(name);
			this.parameters.setProperty(name, value);
			return oldValue;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerParameter.Store#persistParameters()
		 */
		public void persistParameters() {
			StringVector lines = new StringVector();
			this.parameterNames.sortLexicographically(false, false);
			
			//	produce lines
			for (int p = 0; p < this.parameterNames.size(); p++) {
				String name = this.parameterNames.get(p);
				String value = this.parameters.getProperty(name, "");
				
				//	build line, encode value
				try {
					lines.addElement(name + PARAMETER_VALUE_SEPARATOR + URLEncoder.encode(value, PARAMETER_ENCODING));
				} catch (Exception e) {}
			}
			
			//	store lines
			try {
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(this.dataProvider.getOutputStream(this.parameterDataName), "UTF-8"));
				lines.storeContent(bw);
				bw.flush();
				bw.close();
			} catch (IOException e) {}
		}
		
		//	constants and data structures for parameter handling
		private static final String PARAMETER_VALUE_SEPARATOR = "|";
		private static final String PARAMETER_ENCODING = "UTF-8";
		
		private Properties parameters = new Properties();
		private StringVector parameterNames = new StringVector();
	}
	
	/** the generic string parameter type */
	public static final String STRING_TYPE = "string";
	
	/** the string parameter type for longer strings, can include HTML markup */
	public static final String TEXT_TYPE = "text";
	
	/** the number parameter type */
	public static final String NUMBER_TYPE = "number";
	
	/** the number parameter type */
	public static final String INTEGER_TYPE = "integer";
	
	/** the boolean parameter type */
	public static final String BOOLEAN_TYPE = "boolean";
	
	/** the URL parameter type */
	public static final String URL_TYPE = "url";
	
	/** the regular expression pattern parameter type */
	public static final String REGULAR_EXPRESSION_TYPE = "regEx";
	
	/** the GPath parameter type */
	public static final String GPATH_TYPE = "gPath";
	
	/** the color parameter type */
	public static final String COLOR_TYPE = "color";
	
	final String type;
	final String name;
	private String originalValue;
	private String value;
	private String error = null;
	private String tooltip;
	
	JLabel nameDisplay = new JLabel("", JLabel.LEFT);
	ParameterPanel valueEditor;
	
	private AnalyzerParameter(String name, String value, String tooltip, String type) {
		this.name = name;
		this.originalValue = value;
		this.value = value;
		this.tooltip = tooltip;
		this.type = type;
		
		this.nameDisplay.setText(" " + this.name);
		this.nameDisplay.setOpaque(true);
		this.nameDisplay.setBackground(Color.WHITE);
		this.nameDisplay.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
		
		this.updateValue(this.value);
	}
	
	//	TODO figure out preferred size
	private boolean layoutValueDisplay = true;
	ParameterPanel getValueDisplay(boolean useSpacer, boolean center) {
		ParameterPanel valueDisplay = doGetValueDisplay();
		if (this.layoutValueDisplay) {
			valueDisplay.setBackground(Color.WHITE);
			valueDisplay.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));
			this.layoutValueDisplay = false;
		}
		valueDisplay.setToolTipText((this.error == null) ? this.tooltip : this.error);
		valueDisplay.setError(this.error);
		valueDisplay.setUseSpacer(useSpacer);
		valueDisplay.setCenter(center);
		return valueDisplay;
	}
	
	abstract ParameterPanel doGetValueDisplay();
	
	abstract void focus();
	
	String getValue() {
		return this.value;
	}
	
	final void updateValue(String value) {
		this.value = value;
		this.error = validateValue(this.value, this.type);
		this.nameDisplay.setBackground((this.error == null) ? Color.WHITE : Color.ORANGE);
		this.nameDisplay.setToolTipText((this.error == null) ? this.tooltip : this.error);
		System.out.println(this.name + " updated to '" + this.value + "', now " + (this.isDirty() ? "dirty" : "not dirty"));
		ParameterPanel valueDisplay = doGetValueDisplay();
		if (valueDisplay != null) {
			valueDisplay.setError(this.error);
			valueDisplay.setToolTipText((this.error == null) ? this.tooltip : this.error);
		}
	}
	
	boolean isDirty() {
		return ((this.originalValue == null) ? (this.value != null) : !this.originalValue.equals(this.value));
	}
	
	void setClean() {
		this.originalValue = this.value;
	}
	
	static AnalyzerParameter getParameter(String name, String value, String tooltip, String[] permittedValues, String type) {
		
		//	boolean parameter
		if (BOOLEAN_TYPE.equals(type))
			return new BooleanParameter(name, checkInitialValue(value, type, permittedValues), tooltip);
		
		//	color parameter
		if (COLOR_TYPE.equals(type))
			return new ColorParameter(name, checkInitialValue(value, type, permittedValues), tooltip);
		
		//	long string parameter
		if (TEXT_TYPE.equals(type))
			return new TextParameter(name, checkInitialValue(value, type, permittedValues), tooltip);
		
		//	regular expression pattern or GPath parameter
		if (REGULAR_EXPRESSION_TYPE.equals(type) || GPATH_TYPE.equals(type))
			return new PatternParameter(name, checkInitialValue(value, type, permittedValues), tooltip, type);
		
		//	no value selection specified, use plain string panel
		if (permittedValues == null)
			return new StringParameter(name, checkInitialValue(value, type, permittedValues), tooltip, type);
		
		//	use value selection
		else return new SelectorParameter(name, checkInitialValue(value, type, permittedValues), tooltip, permittedValues, type);
	}
	
	private static String checkInitialValue(String value, String type, String[] permittedValues) {
		if (permittedValues != null) {
			boolean valueOk = false;
			String firstNonNull = null;
			for (int v = 0; v < permittedValues.length; v++) {
				if (permittedValues[v] == null)
					valueOk = (valueOk || (value != null));
				else if (permittedValues[v].equals(value)) {
					valueOk = true;
					if (firstNonNull == null)
						firstNonNull = permittedValues[v];
				}
			}
			if (!valueOk && (firstNonNull != null))
				return firstNonNull;
		}
		if ((value != null) && (validateValue(value, type) == null))
			return value;
		if (BOOLEAN_TYPE.equals(type))
			return (("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)) ? "true" : "false");
		if (COLOR_TYPE.equals(type))
			return ColorParameter.getRGB(Color.GRAY);
		if (NUMBER_TYPE.equals(type) || INTEGER_TYPE.equals(type))
			return "0";
		return ((value == null) ? "" : value);
	}
	
	private static abstract class ParameterPanel extends JPanel {
		JComponent content;
		ParameterPanel(JComponent content) {
			super(new BorderLayout(), true);
			this.content = content;
		}
		abstract void setError(String error);
		void setUseSpacer(boolean useSpacer) {
			this.add(this.content, (useSpacer ? BorderLayout.NORTH : BorderLayout.CENTER));
		}
		void setCenter(boolean center) {}
	}
	
	private static class StringParameter extends AnalyzerParameter {
		private JTextField valueInput = new JTextField("");
		private StringParameter(String name, String value, String tooltip, String type) {
			super(name, value, tooltip, type);
			
			this.valueInput.setText(this.getValue());
			this.valueInput.setBorder(BorderFactory.createLoweredBevelBorder());
			this.valueInput.addKeyListener(new KeyAdapter() {
				private int valueInputPressedKey = -1;
				public void keyPressed(KeyEvent ke) {
					this.valueInputPressedKey = ke.getKeyCode();
				}
				public void keyReleased(KeyEvent ke) {
					this.valueInputPressedKey = -1;
				}
				public void keyTyped(KeyEvent ke) {
					if (this.valueInputPressedKey == KeyEvent.VK_ESCAPE) {
						revertValue();
						updateValue();
					}
				}
			});
			this.valueInput.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent fe) {
					updateValue();
				}
			});
			this.valueInput.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					updateValue();
				}
			});
			
			JPanel valueEditor = new JPanel(new BorderLayout(), true);
			valueEditor.add(this.valueInput, BorderLayout.CENTER);
			
			this.valueEditor = new ParameterPanel(valueEditor) {
				void setError(String error) {
					valueInput.setBorder((error == null) ? BorderFactory.createLoweredBevelBorder() : BorderFactory.createLineBorder(Color.ORANGE, 2));
				}
			};
		}
		private void revertValue() {
			this.valueInput.setText(this.getValue());
		}
		private void updateValue() {
			this.updateValue(this.valueInput.getText());
		}
		ParameterPanel doGetValueDisplay() {
			return this.valueEditor;
		}
		void focus() {
			this.valueInput.requestFocusInWindow();
		}
	}
	
	private static class PatternParameter extends AnalyzerParameter {
		private JLabel valueDisplay = new JLabel("");
		private JButton valueEdit = new JButton(" Edit ");
		private JButton valueTest = new JButton(" Test ");
		private PatternParameter(String name, String value, String tooltip, String type) {
			super(name, value, tooltip, type);
			
			this.valueDisplay.setText(this.getValue());
			this.valueDisplay.setBorder(BorderFactory.createLoweredBevelBorder());
			this.valueDisplay.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1)
						editValue();
				}
			});
			
			this.valueEdit.setBorder(BorderFactory.createRaisedBevelBorder());
			this.valueEdit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editValue();
				}
			});
			
			this.valueTest.setBorder(BorderFactory.createRaisedBevelBorder());
			this.valueTest.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					testValue(PatternParameter.this.getValue(), DialogFactory.getTopWindow());
				}
			});
			
			JPanel buttonPanel = new JPanel(new BorderLayout(), true);
			buttonPanel.add(this.valueEdit, BorderLayout.CENTER);
			buttonPanel.add(this.valueTest, BorderLayout.EAST);
			
			JPanel valueEditor = new JPanel(new BorderLayout(), true);
			valueEditor.add(this.valueDisplay, BorderLayout.CENTER);
			valueEditor.add(buttonPanel, BorderLayout.EAST);
			
			this.valueEditor = new ParameterPanel(valueEditor) {
				void setError(String error) {
					valueDisplay.setBorder((error == null) ? BorderFactory.createLoweredBevelBorder() : BorderFactory.createLineBorder(Color.ORANGE, 2));
				}
			};
		}
		
		ParameterPanel doGetValueDisplay() {
			return this.valueEditor;
		}
		
		void focus() {
			this.valueEdit.requestFocusInWindow();
		}
		
		private void editValue() {
			final JDialog ed;
			Window top = DialogFactory.getTopWindow();
			if (top instanceof JDialog)
				ed = new JDialog(((JDialog) top), ("Edit '" + this.name + "'"), true);
			else if (top instanceof JFrame)
				ed = new JDialog(((JFrame) top), ("Edit '" + this.name + "'"), true);
			else ed = new JDialog(((JFrame) null), ("Edit '" + this.name + "'"), true);
			ed.getContentPane().setLayout(new BorderLayout());
			ed.setSize(600, 600);
			
			//	create test button
			JButton[] buttons = new JButton[1];
			buttons[0] = new JButton("");
			buttons[0].setBorder(BorderFactory.createRaisedBevelBorder());
			buttons[0].setPreferredSize(new Dimension(115, 21));
			
			//	create editor panel and add it to dialog
			final RegExEditorPanel regExEditor;
			final GPathEditorPanel gPathEditor;
			if (REGULAR_EXPRESSION_TYPE.equals(this.type)) {
				regExEditor = new RegExEditorPanel(buttons);
				regExEditor.setContent(this.getValue());
				gPathEditor = null;
				buttons[0].setText("Test RegEx");
				buttons[0].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						testValue(regExEditor.getContent(), ed);
					}
				});
				ed.getContentPane().add(regExEditor, BorderLayout.CENTER);
			}
			else if (GPATH_TYPE.equals(this.type)) {
				regExEditor = null;
				gPathEditor = new GPathEditorPanel(buttons);
				gPathEditor.setContent(this.getValue());
				buttons[0].setText("Test GPath");
				buttons[0].addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						testValue(gPathEditor.getContent(), ed);
					}
				});
				ed.getContentPane().add(gPathEditor, BorderLayout.CENTER);
			}
			else return;
			
			//	create and add main buttons
			JButton okButton = new JButton("OK");
			okButton.setBorder(BorderFactory.createRaisedBevelBorder());
			okButton.setPreferredSize(new Dimension(100, 21));
			okButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if ((regExEditor != null) && regExEditor.isDirty()) {
						updateValue(regExEditor.getContent());
						valueDisplay.setText(getValue());
					}
					else if ((gPathEditor != null) && gPathEditor.isDirty()) {
						updateValue(gPathEditor.getContent());
						valueDisplay.setText(getValue());
					}
					ed.dispose();
				}
			});
			
			JButton cancelButton = new JButton("Cancel");
			cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
			cancelButton.setPreferredSize(new Dimension(100, 21));
			cancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					ed.dispose();
				}
			});
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			buttonPanel.add(okButton);
			buttonPanel.add(cancelButton);
			ed.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			
			ed.setLocationRelativeTo(this.valueEditor);
			ed.setVisible(true);
		}
		
		private void testValue(String expression, Window top) {
			QueriableAnnotation testDoc = Gamta.getTestDocument();
			if (testDoc == null)
				return;
			
			Annotation[] matches;
			String label;
			try {
				if (GPATH_TYPE.equals(this.type)) {
					label = "GPath";
					GPath gPath = GPathParser.parsePath(expression);
					matches = gPath.evaluate(testDoc, null);
				}
				else if (REGULAR_EXPRESSION_TYPE.equals(this.type)) {
					label = "Regular Expression";
					matches = Gamta.extractAllMatches(testDoc, expression, 0);
				}
				else return;
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(DialogFactory.getTopWindow(), ("The " + (GPATH_TYPE.equals(this.type) ? "GPath expression" : "regular expression") + " could not be tested because it contains syntax errors:\n" + e.getMessage()), ("Error Testing " + (GPATH_TYPE.equals(this.type) ? "GPath" : "Regular") + " Expression"), JOptionPane.ERROR_MESSAGE);
				e.printStackTrace(System.out);
				return;
			}
			
			AnnotationDisplayDialog add;
			if (top instanceof JDialog)
				add = new AnnotationDisplayDialog(((JDialog) top), ("Matches of " + label + " '" + name + "'"), matches, true);
			else if (top instanceof JFrame)
				add = new AnnotationDisplayDialog(((JFrame) top), ("Matches of " + label + " '" + name + "'"), matches, true);
			else add = new AnnotationDisplayDialog(((JFrame) null), ("Matches of " + label + " '" + name + "'"), matches, true);
			add.setLocationRelativeTo(this.valueEditor);
			add.setVisible(true);
		}
	}
	
	private static class TextParameter extends AnalyzerParameter {
		private JTextArea valueInput = new JTextArea("");
		private TextParameter(String name, String value, String tooltip) {
			super(name, value, tooltip, TEXT_TYPE);
			
			this.valueInput.setLineWrap(true);
			this.valueInput.setWrapStyleWord(true);
			this.valueInput.setText(this.getValue());
			this.valueInput.setBorder(BorderFactory.createLoweredBevelBorder());
			this.valueInput.addKeyListener(new KeyAdapter() {
				private int valueInputPressedKey = -1;
				public void keyPressed(KeyEvent ke) {
					this.valueInputPressedKey = ke.getKeyCode();
				}
				public void keyReleased(KeyEvent ke) {
					this.valueInputPressedKey = -1;
				}
				public void keyTyped(KeyEvent ke) {
					if (this.valueInputPressedKey == KeyEvent.VK_ESCAPE) {
						revertValue();
						updateValue();
					}
				}
			});
			this.valueInput.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent fe) {
					updateValue();
				}
			});
			
			this.valueEditor = new ParameterPanel(this.valueInput) {
				void setError(String error) {} // nothing to validate about text ...
				void setUseSpacer(boolean useSpacer) {
					super.setUseSpacer(false);
				}
			};
		}
		private void revertValue() {
			this.valueInput.setText(this.getValue());
		}
		private void updateValue() {
			this.updateValue(this.valueInput.getText());
		}
		ParameterPanel doGetValueDisplay() {
			return this.valueEditor;
		}
		void focus() {
			this.valueInput.requestFocusInWindow();
		}
	}
	
	private static class SelectorParameter extends AnalyzerParameter {
		private JComboBox valueSelector;
		private int valueSelectorPressedKey = -1;
		private SelectorParameter(String name, String value, String tooltip, String[] permittedValues, String type) {
			super(name, value, tooltip, type);
			
			this.valueSelector = new JComboBox();
			this.valueSelector.setEditable(false);
			for (int v = 0; v < permittedValues.length; v++) {
				if (permittedValues[v] == null)
					this.valueSelector.setEditable(true);
				else this.valueSelector.addItem(permittedValues[v]);
			}
			this.valueSelector.setSelectedItem(this.getValue());
			this.valueSelector.setBorder(BorderFactory.createLoweredBevelBorder());
			this.valueSelector.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					updateValue();
				}
			});
			this.valueSelector.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					if (!valueSelector.isVisible())
						return;
					else if ((valueSelectorPressedKey != -1) && !valueSelector.isPopupVisible()) {
						if (valueSelectorPressedKey == KeyEvent.VK_ESCAPE) {
							revertValue();
							updateValue();
						}
						else updateValue();
						if (valueSelectorPressedKey == KeyEvent.VK_ENTER)
							updateValue();
					}
				}
			});
			this.valueSelector.addPopupMenuListener(new PopupMenuListener() {
				public void popupMenuCanceled(PopupMenuEvent pme) {}
				public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {
					valueSelectorPressedKey = -1;
				}
			});
			((JTextComponent) this.valueSelector.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent ke) {
					valueSelectorPressedKey = ke.getKeyCode();
				}
				public void keyReleased(KeyEvent ke) {
					valueSelectorPressedKey = -1;
				}
				public void keyTyped(KeyEvent ke) {
					if (valueSelectorPressedKey == KeyEvent.VK_ESCAPE) {
						revertValue();
						updateValue();
					}
				}
			});
			
			this.valueEditor = new ParameterPanel(this.valueSelector) {
				void setError(String error) {
					valueSelector.setBorder((error == null) ? BorderFactory.createLoweredBevelBorder() : BorderFactory.createLineBorder(Color.ORANGE, 2));
				}
			};
		}
		private void revertValue() {
			this.valueSelector.setSelectedItem(this.getValue());
		}
		private void updateValue() {
			this.updateValue(this.valueSelector.getSelectedItem().toString());
		}
		ParameterPanel doGetValueDisplay() {
			return this.valueEditor;
		}
		void focus() {
			this.valueSelector.requestFocusInWindow();
		}
	}
	
	private static class BooleanParameter extends AnalyzerParameter {
		private JCheckBox valueBox;
		private BooleanParameter(String name, String value, String tooltip) {
			super(name, value, tooltip, BOOLEAN_TYPE);
			
			this.valueBox = new JCheckBox();
			this.valueBox.setSelected("true".equals(value) || "yes".equals(value));
			this.valueBox.setBorder(BorderFactory.createLoweredBevelBorder());
			this.valueBox.setBackground(Color.WHITE);
			this.valueBox.setHorizontalAlignment(JCheckBox.CENTER);
			this.valueBox.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					updateValue();
				}
			});
			
			this.valueEditor = new ParameterPanel(this.valueBox) {
				void setError(String error) {} // nothing to validate about a boolean ...
				void setCenter(boolean center) {
					valueBox.setHorizontalAlignment(center ? JCheckBox.CENTER : JCheckBox.LEFT);
				}
			};
		}
		private void updateValue() {
			this.updateValue(this.valueBox.isSelected() ? "true" : "false");
		}
		ParameterPanel doGetValueDisplay() {
			return this.valueEditor;
		}
		void focus() {
			this.valueBox.requestFocusInWindow();
		}
	}
	
	private static class ColorParameter extends AnalyzerParameter {
		private JButton valueChooser = new JButton(" click to change color ");
		private Color value;
		private ColorParameter(String name, String value, String tooltip) {
			super(name, value, tooltip, COLOR_TYPE);
			this.value = getColor(value);
			
			this.valueChooser.setBorder(BorderFactory.createLoweredBevelBorder());
			this.valueChooser.setBackground(getColor(this.getValue()));
			this.valueChooser.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					Color value = JColorChooser.showDialog(valueChooser, "", ColorParameter.this.value);
					if (value == null)
						return;
					ColorParameter.this.value = value;
					updateValue();
				}
			});
			
			if (this.value == null) {
				this.value = Color.GRAY;
				this.updateValue();
			}
			
			this.valueEditor = new ParameterPanel(this.valueChooser) {
				void setError(String error) {} // nothing to validate about a color ...
			};
		}
		private void updateValue() {
			this.valueChooser.setBackground(this.value);
			float[] hsb = Color.RGBtoHSB(this.value.getRed(), this.value.getGreen(), this.value.getBlue(), null);
			this.valueChooser.setForeground((hsb[2] < 0.5) ? Color.WHITE : Color.BLACK);
			this.updateValue(getRGB(this.value));
		}
		ParameterPanel doGetValueDisplay() {
			return this.valueEditor;
		}
		void focus() {
			this.valueChooser.requestFocusInWindow();
		}
		static String getRGB(Color color) {
			return ("" +
					getHex(color.getRed()) + 
					getHex(color.getGreen()) +
					getHex(color.getBlue()) +
					"");
		}
		private static final String getHex(int i) {
			int high = (i >>> 4) & 15;
			int low = i & 15;
			String hex = "";
			if (high < 10) hex += ("" + high);
			else hex += ("" + ((char) ('A' + (high - 10))));
			if (low < 10) hex += ("" + low);
			else hex += ("" +  ((char) ('A' + (low - 10))));
			return hex;
		}
		private static Color getColor(String rgb) {
			if (rgb.length() == 3)
				return readHexRGB(rgb.substring(0, 1), rgb.substring(1, 2), rgb.substring(2, 3));
			else if (rgb.length() == 6)
				return readHexRGB(rgb.substring(0, 2), rgb.substring(2, 4), rgb.substring(4, 6));
			else return null;
		}
		private static final Color readHexRGB(String red, String green, String blue) {
			return new Color(
					translateString(red),
					translateString(green),
					translateString(blue)
				);
		}
		private static final int translateString(String s) {
			if (s.length() == 0)
				return 0;
			
			int v = 0;
			v += translateChar(s.charAt(0));
			v <<= 4;
			v += translateChar(s.charAt((s.length() > 1) ? 1 : 0));
			return v;
		}
		private static final int translateChar(char c) {
			if (('0' <= c) && (c <= '9')) return (((int) c) - '0');
			else if (('a' <= c) && (c <= 'f')) return (((int) c) - 'a' + 10);
			else if (('A' <= c) && (c <= 'F')) return (((int) c) - 'A' + 10);
			else return 0;
		}
	}
	
	private static String validateValue(String value, String type) {
		
		//	no test for plain strings
		if (STRING_TYPE.equals(type) || TEXT_TYPE.equals(type))
			return null;
		
		//	no test for booleans either
		if (BOOLEAN_TYPE.equals(type))
			return null;
		
		//	nor for colors
		if (COLOR_TYPE.equals(type))
			return null;
		
		//	validate number
		if (NUMBER_TYPE.equals(type)) try {
			Double.parseDouble(value);
			return null;
		}
		catch (NumberFormatException nfe) {
			return ("'" + value + "' is not a valid number.");
		}
		
		//	validate integer
		if (INTEGER_TYPE.equals(type)) try {
			Integer.parseInt(value);
			return null;
		}
		catch (NumberFormatException nfe) {
			return ("'" + value + "' is not a valid integer.");
		}
		
		//	validate GPath
		if (GPATH_TYPE.equals(type))
			return GPathParser.validatePath(value);
		
		//	validate regex
		if (REGULAR_EXPRESSION_TYPE.equals(type)) try {
			Pattern.compile(value);
			return null;
		}
		catch (PatternSyntaxException pse) {
			return pse.getMessage();
		}
		
		//	validate URL
		if (URL_TYPE.equals(type)) try {
			if ((value != null) && ((value.indexOf(":/") == -1) || (value.indexOf(":/") > 5)) && ((value.indexOf("//") == -1) || (value.indexOf("//") > 6))) {
				System.out.println("Extending URL '" + value + "'");
				value = "http://" + value;
			}
			new URL(value);
			return null;
		}
		catch (MalformedURLException mue) {
			return mue.getMessage();
		}
		
		//	calm down compiler
		return null;
	}
}
