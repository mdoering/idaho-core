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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.Attributed;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceUtils;
import de.uka.ipd.idaho.gamta.util.CountingSet;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * An editor widget for manipulating the attributes of an Attributed object in
 * a GUI
 * 
 * @author sautter
 */
public class AttributeEditor extends JPanel {
	
	private static final String DUMMY_ATTRIBUTE_NAME = "Attribute Name";
	private static final String DUMMY_ATTRIBUTE_VALUE = "Attribute Value";
	
	private AttributeTablePanel attributeTable = new AttributeTablePanel();
	
	private JComboBox attributeNameField = new JComboBox();
	private JComboBox attributeValueField = new JComboBox();
	
	private TreeMap contextAttributeValueSetsByName = new TreeMap();
	
	private Attributed attributed;
	private TreeMap attributes = new TreeMap();
	
	private boolean nameFieldKeyPressed = false;
	private boolean valueFieldKeyPressed = false;
	
	/**
	 * Constructor
	 * @param annotation the Annotation whose attributes to edit
	 */
	public AttributeEditor(Annotation annotation) {
		this(annotation, null);
	}
	
	/**
	 * Constructor
	 * @param annotation the Annotation whose attributes to edit
	 * @param context the context annotation, e.g. the document the annotation
	 *            refers to (will be used to provide attribute name and value
	 *            suggestions)
	 */
	public AttributeEditor(Annotation annotation, QueriableAnnotation context) {
		this(annotation, annotation.getType(), annotation.getValue(), ((context == null) ? new Annotation[0] : context.getAnnotations(annotation.getType())));
	}
	
	/**
	 * Constructor
	 * @param token the Token whose attributes to edit
	 * @param context the context token sequence, e.g. the document the token
	 *            belongs to (will be used to provide attribute name and value
	 *            suggestions)
	 */
	public AttributeEditor(Token token, TokenSequence context) {
		this(token, Token.TOKEN_ANNOTATION_TYPE, token.getValue(), getAttributedTokens(context));
	}
	private static Attributed[] getAttributedTokens(TokenSequence tokens) {
		ArrayList attributedTokens = new ArrayList();
		for (int t = 0; t < tokens.size(); t++) {
			Token token = tokens.tokenAt(t);
			if (token.getAttributeNames().length != 0)
				attributedTokens.add(token);
		}
		return ((Attributed[]) attributedTokens.toArray(new Attributed[attributedTokens.size()]));
	}
	
	/**
	 * Constructor
	 * @param attributed the attribute bearing object whose attributes to edit
	 * @param type the type of the attributed object (may be null)
	 * @param value the string value of the attributed object (may be null)
	 * @param context the context, e.g. attributed objects similar to the one
	 *            whose attributes to edit (will be used to provide attribute
	 *            name and value suggestions, may be null)
	 */
	public AttributeEditor(Attributed attributed, String type, String value, Attributed[] context) {
		super(new BorderLayout(), true);
		this.attributed = attributed;
		
		//	store attributes of annotation being edited
		String[] attributeNames = attributed.getAttributeNames();
		Arrays.sort(attributeNames, String.CASE_INSENSITIVE_ORDER);
		for (int n = 0; n < attributeNames.length; n++)
			this.attributes.put(attributeNames[n], this.attributed.getAttribute(attributeNames[n]));
		
		//	get and index attributes and values of all context objects (for offering suggestions)
		for (int c = 0; (context != null) && (c < context.length); c++) {
			attributeNames = context[c].getAttributeNames();
			for (int n = 0; n < attributeNames.length; n++) {
				Object attributeValue = context[c].getAttribute(attributeNames[n]);
				if (attributeValue != null)
					this.getValueSet(attributeNames[n]).add(attributeValue.toString());
			}
		}
		
		//	initialize attribute editor fields
		this.attributeNameField.setBorder(BorderFactory.createLoweredBevelBorder());
		this.attributeNameField.setEditable(true);
		this.resetAttributeNameField();
		
		this.attributeNameField.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent fe) {
				if (DUMMY_ATTRIBUTE_NAME.equals(attributeNameField.getSelectedItem()))
					attributeNameField.setSelectedItem("");
			}
		});
		((JTextComponent) this.attributeNameField.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				nameFieldKeyPressed = true;
			}
			public void keyReleased(KeyEvent ke) {
				nameFieldKeyPressed = false;
			}
		});
		this.attributeNameField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (nameFieldKeyPressed && isVisible() && !attributeNameField.isPopupVisible())
					attributeValueField.requestFocusInWindow();
			}
		});
		this.attributeNameField.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {
				resetAttributeValueField();
			}
		});
		
		
		this.attributeValueField.setBorder(BorderFactory.createLoweredBevelBorder());
		this.attributeValueField.setEditable(true);
		this.resetAttributeValueField();
		
		this.attributeValueField.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent fe) {
				if (DUMMY_ATTRIBUTE_VALUE.equals(attributeValueField.getSelectedItem()))
					attributeValueField.setSelectedItem("");
			}
		});
		((JTextComponent) this.attributeValueField.getEditor().getEditorComponent()).addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent ke) {
				valueFieldKeyPressed = true;
			}
			public void keyReleased(KeyEvent ke) {
				valueFieldKeyPressed = false;
			}
		});
		this.attributeValueField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (valueFieldKeyPressed && isVisible() && !attributeValueField.isPopupVisible())
					setAttribute();
			}
		});
		
		//	initialize buttons
		JButton setAttributeButton = new JButton(" Add / Set Attribute ");
		setAttributeButton.setBorder(BorderFactory.createRaisedBevelBorder());
		setAttributeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				setAttribute();
			}
		});
		
		JPanel attributeButtonPanel = new JPanel(new BorderLayout(), true);
		attributeButtonPanel.add(this.attributeNameField, BorderLayout.CENTER);
		attributeButtonPanel.add(setAttributeButton, BorderLayout.EAST);
		attributeButtonPanel.add(this.attributeValueField, BorderLayout.SOUTH);
		
		//	display annotation's data (for giving users the context)
		JLabel annotationTypeField = new JLabel((type == null) ? "generic" : type);
		annotationTypeField.setBorder(BorderFactory.createLoweredBevelBorder());
		
		JLabel annotationValueField = new JLabel((value == null) ? "" : value);
		annotationValueField.setBorder(BorderFactory.createLoweredBevelBorder());
		if (attributed instanceof TokenSequence)
			annotationValueField.setToolTipText(this.produceTooltipText((TokenSequence) attributed));
		
		JPanel annotationDataPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.insets.top = 5;
		gbc.insets.left = 5;
		gbc.insets.right = 5;
		gbc.insets.bottom = 5;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridy = 0;
		gbc.gridx = 0;
		annotationDataPanel.add(annotationTypeField, gbc.clone());
		gbc.gridx = 1;
		gbc.weightx = 1;
		gbc.gridwidth = 3;
		annotationDataPanel.add(annotationValueField, gbc.clone());
		
		//	wrap and initialize attribute table
		JScrollPane attributeTableBox = new JScrollPane(this.attributeTable);
		attributeTableBox.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		attributeTableBox.setViewportBorder(BorderFactory.createLoweredBevelBorder());
		this.attributeTable.updateAttributes();
		
		//	put the whole stuff together
		this.setLayout(new BorderLayout());
		this.add(annotationDataPanel, BorderLayout.NORTH);
		this.add(attributeTableBox, BorderLayout.CENTER);
		this.add(attributeButtonPanel, BorderLayout.SOUTH);
	}
	
	private void resetAttributeNameField() {
		this.attributeNameField.setModel(new DefaultComboBoxModel((String[]) this.contextAttributeValueSetsByName.keySet().toArray(new String[this.contextAttributeValueSetsByName.size()])));
		this.attributeNameField.setSelectedItem(DUMMY_ATTRIBUTE_NAME);
	}
	
	private void resetAttributeValueField() {
		Object nameObj = this.attributeNameField.getSelectedItem();
		if ((nameObj == null) || DUMMY_ATTRIBUTE_NAME.equals(nameObj)) {
			this.attributeValueField.setModel(new DefaultComboBoxModel(new String[0]));
			this.attributeValueField.setSelectedItem(DUMMY_ATTRIBUTE_VALUE);
		}
		else {
			AttributeValueSet values = this.getValueSet(nameObj.toString());
			this.attributeValueField.setModel(new DefaultComboBoxModel(values.toStringArray()));
			Object value = this.attributes.get(nameObj.toString());
			this.attributeValueField.setSelectedItem((value == null) ? DUMMY_ATTRIBUTE_VALUE : value);
		}
	}
	
	private AttributeValueSet getValueSet(String attributeName) {
		AttributeValueSet values = ((AttributeValueSet) this.contextAttributeValueSetsByName.get(attributeName));
		if (values == null) {
			values = new AttributeValueSet();
			this.contextAttributeValueSetsByName.put(attributeName, values);
		}
		return values;
	}
	
	private class AttributeValueSet extends CountingSet {
		AttributeValueSet() {
			super(new TreeMap());
		}
		String[] toStringArray() {
			return ((String[]) this.toArray(new String[this.elementCount()]));
		}
	}
	
	private String produceTooltipText(TokenSequence tokens) {
		if (tokens.length() < 100) return TokenSequenceUtils.concatTokens(tokens);
		
		StringVector lines = new StringVector();
		int startToken = 0;
		int lineLength = 0;
		Token lastToken = null;
		
		for (int t = 0; t < tokens.size(); t++) {
			Token token = tokens.tokenAt(t);
			lineLength += token.length();
			if (lineLength > 100) {
				lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (t - startToken + 1)));
				startToken = (t + 1);
				lineLength = 0;
			}
			else if (Gamta.insertSpace(lastToken, token))
				lineLength++;
		}
		if (startToken < tokens.size())
			lines.addElement(TokenSequenceUtils.concatTokens(tokens, startToken, (tokens.size() - startToken)));
		
		return ("<HTML>" + lines.concatStrings("<BR>") + "</HTML>");
	}
	
	private void setAttribute() {
		
		//	get name
		String name = null;
		Object nameObj = attributeNameField.getSelectedItem();
		if (nameObj != null)
			name = nameObj.toString().trim();
		if ((name == null) || (name.length() == 0) || DUMMY_ATTRIBUTE_NAME.equals(name))
			return;
		
		//	get value
		String value = null;
		Object valueObj = attributeValueField.getSelectedItem();
		if (valueObj != null)
			value = valueObj.toString();
		if ((value == null) || (value.length() == 0) || DUMMY_ATTRIBUTE_VALUE.equals(value))
			return;
		
		//	name OK?
		if (AttributeUtils.isValidAttributeName(name)) {
			
			//	get old value
			String oldValue = null;
			Object oldValueObj = this.attributes.get(name);
			if (oldValueObj != null)
				oldValue = oldValueObj.toString();
			
			//	set attribute
			this.attributes.put(name, value);
			
			//	update value lists
			this.getValueSet(name).add(value);
			if (oldValue != null)
				this.getValueSet(name).remove(oldValue);
			
			//	refresh attribute table
			this.attributeTable.updateAttributes();
			
			//	refresh input fields
			this.resetAttributeNameField();
		}
		
		//	show error message
		else JOptionPane.showMessageDialog(this, "Cannot add attribute. The specified name is invalid.", "Invalid Attribute Name", JOptionPane.ERROR_MESSAGE);
	}
	
	private void removeAttribute(String name) {
		
		//	get value
		String value = null;
		Object valueObj = this.attributes.get(name);
		if (valueObj != null)
			value = valueObj.toString();
		
		//	update data
		this.attributes.remove(name);
		
		//	update value lists
		if (value != null)
			this.getValueSet(name).removeAll(value);
		
		//	refresh attribute table
		this.attributeTable.updateAttributes();
		
		//	refresh input fields
		this.resetAttributeNameField();
	}
	
	/**
	 * Write the changes made in the editor through to the annotation being
	 * edited.
	 * @return true if there were any changes to be written, false otherwise
	 */
	public boolean writeChanges() {
		boolean modified = false;
		
		String[] oldAttributeNames = this.attributed.getAttributeNames();
		for (int a = 0; a < oldAttributeNames.length; a++) {
			Object newValue = this.attributes.get(oldAttributeNames[a]);
			if (newValue == null) {
				this.attributed.removeAttribute(oldAttributeNames[a]);
				modified = true;
			}
			else {
				Object oldValue = this.attributed.getAttribute(oldAttributeNames[a]);
				if ((oldValue == null) || !newValue.equals(oldValue)) {
					this.attributed.setAttribute(oldAttributeNames[a], newValue);
					modified = true;
				}
			}
		}
		
		for (Iterator anit = this.attributes.keySet().iterator(); anit.hasNext();) {
			String name = ((String) anit.next());
			if (!this.attributed.hasAttribute(name)) {
				Object value = this.attributes.get(name);
				this.attributed.setAttribute(name, value);
				modified = true;
			}
		}
		
		return modified;
	}
	
	private class AttributeTablePanel extends JPanel {
		private ArrayList lines = new ArrayList();
		private JPanel spacer = new JPanel();
		
		AttributeTablePanel() {
			super(new GridBagLayout(), true);
		}
		
		void updateAttributes() {
			this.lines.clear();
			for (Iterator anit = attributes.keySet().iterator(); anit.hasNext();) {
				String name = ((String) anit.next());
				Object value = attributes.get(name);
				this.lines.add(new AttributeTableLine(name, value));
			}
			this.layoutLines();
		}
		
		void layoutLines() {
			this.removeAll();
			
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.insets.top = 0;
			gbc.insets.bottom = 0;
			gbc.insets.left = 0;
			gbc.insets.right = 0;
			gbc.weighty = 0;
			gbc.gridy = 0;
			gbc.gridheight = 1;
			gbc.gridwidth = 1;
			gbc.fill = GridBagConstraints.BOTH;
			
			for (int l = 0; l < this.lines.size(); l++) {
				AttributeTableLine line = ((AttributeTableLine) this.lines.get(l));
				gbc.gridx = 0;
				gbc.weightx = 0;
				this.add(line.removeButton, gbc.clone());
				gbc.gridx = 1;
				gbc.weightx = 1;
				this.add(line.displayLabel, gbc.clone());
				gbc.gridy++;
			}
			
			gbc.gridx = 0;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.gridwidth = 2;
			this.add(this.spacer, gbc.clone());
			
			this.validate();
			this.repaint();
		}
		
		private class AttributeTableLine {
			JButton removeButton;
			JLabel displayLabel;
			AttributeTableLine(final String name, Object value) {
				this.removeButton = new JButton("<HTML>&nbsp;<B>X</B>&nbsp;</HTML>");
				this.removeButton.setToolTipText("Remove attribute '" + name + "'");
				this.removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.removeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						removeAttribute(name);
						lines.remove(AttributeTableLine.this);
						layoutLines();
					}
				});
				this.displayLabel = new JLabel("<HTML>&nbsp;<B>" + name + "</B>: " + value.toString() + "</HTML>");
				this.displayLabel.setOpaque(true);
				this.displayLabel.setBackground(Color.WHITE);
				this.displayLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
				this.displayLabel.addMouseListener(new MouseAdapter() {
					public void mouseClicked(MouseEvent me) {
						attributeNameField.setSelectedItem(name);
					}
				});
			}
		}
	}
}