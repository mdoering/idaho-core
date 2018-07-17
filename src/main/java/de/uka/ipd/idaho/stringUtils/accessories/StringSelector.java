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
package de.uka.ipd.idaho.stringUtils.accessories;


import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ItemListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A JPanel offering a list of Strings for selection in a labelled combo box.
 * 
 * @author sautter
 */
public class StringSelector extends JPanel {
	private JComboBox selector = new JComboBox();
	
	/**	Constructor
	 * @param	label		the label for the selector
	 * @param	strings		the stings to offer for selection
	 * @param	editable	allow manual input, or just selection of a given string?
	 */
	public StringSelector(String label, String[] strings, boolean editable) {
		super(new GridBagLayout());
		
		Arrays.sort(strings);
		this.selector = new JComboBox(strings);
		this.selector.setBorder(BorderFactory.createLoweredBevelBorder());
		this.selector.setEditable(editable);
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets.top = 2;
		gbc.insets.bottom = 2;
		gbc.insets.left = 3;
		gbc.insets.right = 3;
		gbc.weighty = 0;
		gbc.weightx = 1;
		gbc.gridheight = 1;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridy = 0;
		
		gbc.gridx = 0;
		this.add(new JLabel(label), gbc.clone());
		gbc.gridx = 1;
		this.add(this.selector, gbc.clone());	
	}
	
	/**	@return	the String currently selected in the selector
	 */
	public String getSelectedString() {
		Object o = this.selector.getSelectedItem();
		if ((o != null) && (o instanceof String)) return o.toString();
		else return null;
	}
	
	/** change the selection
	 * @param	selected	the String to set selected
	 */
	public void setSelectedString(String selected) {
		this.selector.setSelectedItem(selected);
	}
	
	/**	change selectable options
	 * @param	strings		the new options
	 */
	public void setStrings(String[] strings) {
		Arrays.sort(strings);
		this.selector.setModel(new DefaultComboBoxModel(strings));
	}
	
	/** add an item listener to the selector box 
	 * @param	il	the ItemListener to add
	 */
	public void addItemListener(ItemListener il) {
		this.selector.addItemListener(il);
	}
}
