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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.util.AnalyzerDataProvider;


/**
 * Widget for editing individual key-value pairs belonging to the configuration
 * of an Analyzer. Values will be displayed depending on their specified type.
 * 
 * @author sautter
 */
public class ParameterConfigPanel extends AnalyzerConfigPanel {
	
	private AnalyzerParameter.Store parameterStore;
	
	private ArrayList parameters = new ArrayList();
	private int selectedParameter = -1;
	
	private JLabel nameLabel = new JLabel("Name", JLabel.CENTER);
	private JLabel valueLabel = new JLabel("Value", JLabel.CENTER);
	private JPanel linePanelSpacer = new JPanel();
	private JPanel linePanel = new JPanel(new GridBagLayout());
	
	/** Constructor
	 * @param dataProvider the data provider to read and write data from and to
	 * @param parameterDataName the name of the data object to store parameters to
	 * @param title the title of this panel to display in a tabbed pane
	 * @param toolTip the tooltip of this panel to display in a tabbed pane
	 */
	public ParameterConfigPanel(AnalyzerDataProvider dataProvider, String parameterDataName, String title, String toolTip) {
		this(new AnalyzerParameter.DefaultStore(dataProvider, parameterDataName), title, toolTip);
	}
	
	/** Constructor
	 * @param parameterStore the store object to persist parameters in
	 * @param title the title of this panel to display in a tabbed pane
	 * @param toolTip the tooltip of this panel to display in a tabbed pane
	 */
	public ParameterConfigPanel(AnalyzerParameter.Store parameterStore, String title, String toolTip) {
		super(null, title, toolTip);
		this.parameterStore = parameterStore;
		
		this.nameLabel.setBorder(BorderFactory.createRaisedBevelBorder());
		this.nameLabel.setPreferredSize(new Dimension(160, 21));
		this.nameLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				selectParameter(-1);
				linePanel.requestFocusInWindow();
			}
		});
		
		this.valueLabel.setBorder(BorderFactory.createRaisedBevelBorder());
		this.valueLabel.setPreferredSize(new Dimension(160, 21));
		this.valueLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				selectParameter(-1);
				linePanel.requestFocusInWindow();
			}
		});
		
		this.linePanelSpacer.setBackground(Color.WHITE);
		this.linePanelSpacer.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				selectParameter(-1);
				linePanel.requestFocusInWindow();
			}
		});
		this.linePanel.setBorder(BorderFactory.createLineBorder(this.getBackground(), 3));
		this.linePanel.setFocusable(true);
//		this.linePanel.addFocusListener(new FocusAdapter() {
//			public void focusGained(FocusEvent fe) {
//				System.out.println("focusGained");
//			}
//			public void focusLost(FocusEvent fe) {
//				System.out.println("focusLost");
//			}
//		});
		
		final String upKey = "GO_UP";
		this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), upKey);
		this.linePanel.getActionMap().put(upKey, new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				System.out.println(upKey);
				if (selectedParameter > 0)
					selectParameter(selectedParameter - 1);
				else if (selectedParameter == -1)
					selectParameter(parameters.size() - 1);
			}
		});
		
		final String downKey = "GO_DOWN";
		this.linePanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), downKey);
		this.linePanel.getActionMap().put(downKey, new AbstractAction() {
			public void actionPerformed(ActionEvent ae) {
				System.out.println(downKey);
				if (selectedParameter == -1)
					selectParameter(0);
				else if ((selectedParameter + 1) < parameters.size())
					selectParameter(selectedParameter + 1);
			}
		});
		
		JScrollPane linePanelBox = new JScrollPane(this.linePanel);
		linePanelBox.getVerticalScrollBar().setUnitIncrement(50);
		linePanelBox.getVerticalScrollBar().setBlockIncrement(100);
		this.add(linePanelBox, BorderLayout.CENTER);
	}
	
	/**
	 * Modify the color of the parameter grid (default is white).
	 * @param gridColor the new grid color
	 */
	public void setGridColor(Color gridColor) {
		this.gridColor = gridColor;
		this.layoutParameters();
	}
	private Color gridColor = Color.WHITE;
	
	private void layoutParameters() {
		this.linePanel.removeAll();
		
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
		
		gbc.gridx = 0;
		gbc.weightx = 0;
		this.linePanel.add(this.nameLabel, gbc.clone());
		gbc.gridx = 1;
		gbc.weightx = 1;
		this.linePanel.add(this.valueLabel, gbc.clone());
		gbc.gridy++;
		
		for (int l = 0; l < this.parameters.size(); l++) {
			AnalyzerParameter ap = ((AnalyzerParameter) this.parameters.get(l));
			gbc.gridx = 0;
			gbc.weightx = 0;
			ap.nameDisplay.setBorder(BorderFactory.createLineBorder(this.gridColor, 1));
			this.linePanel.add(ap.nameDisplay, gbc.clone());
			gbc.gridx = 1;
			gbc.weightx = 1;
			JComponent valueDisplay = ap.getValueDisplay(false, false);
			valueDisplay.setBorder(BorderFactory.createLineBorder(this.gridColor, 1));
			this.linePanel.add(valueDisplay, gbc.clone());
			gbc.gridy++;
		}
		
		gbc.gridx = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.gridwidth = 2;
		this.linePanel.add(this.linePanelSpacer, gbc.clone());
		
		this.validate();
		this.repaint();
	}
	
	private void selectParameter(int index) {
		if (index == -1)
			this.linePanel.requestFocusInWindow();
		else ((AnalyzerParameter) this.parameters.get(index)).focus();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel#isDirty()
	 */
	public boolean isDirty() {
		boolean dirty = false;
		for (int l = 0; l < this.parameters.size(); l++)
			dirty = (dirty || ((AnalyzerParameter) this.parameters.get(l)).isDirty());
		return dirty;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.analyzerConfiguration.AnalyzerConfigPanel#commitChanges()
	 */
	public boolean commitChanges() throws IOException {
		if (!this.isDirty())
			return false;
		for (int l = 0; l < this.parameters.size(); l++) {
			AnalyzerParameter ap = ((AnalyzerParameter) this.parameters.get(l));
			this.parameterStore.storeParameter(ap.name, ap.getValue());
		}
		this.parameterStore.persistParameters();
		for (int l = 0; l < this.parameters.size(); l++)
			((AnalyzerParameter) this.parameters.get(l)).setClean();
		return true;
	}
	
	/**
	 * Add a parameter to the panel. The name must not be empty and must not
	 * contain whitespace, line breaks, or non ASCII characters. If the
	 * permitted are not null, set one of the array elements to null to indicate
	 * the parameter value is freely editable; otherwise, the value will be
	 * restricted to the elements of the array. The type must be one of the
	 * constants defined in the class AnalyzerParameter.
	 * @param name the name of the parameter
	 * @param value the initial value of the parameter
	 * @param tooltip a tooltip text describing the parameter (may be null)
	 * @param permittedValues an array holding the values the parameter can take
	 * @param type the type of the parameter
	 */
	public void addParameter(String name, String value, String tooltip, String[] permittedValues, String type) {
		if ((name == null) || !AnnotationUtils.isValidAnnotationType(name))
			throw new IllegalArgumentException("Parameter names must not be empty and must not contain whitespace, line breaks, or non ASCII characters.");
		AnalyzerParameter ap = AnalyzerParameter.getParameter(name, value, tooltip, permittedValues, type);
		if (ap == null)
			throw new IllegalArgumentException("'" + type + "' is not a valid parameter type.");
		this.parameters.add(ap);
		this.layoutParameters();
		this.selectParameter(this.parameters.size() - 1);
	}
}
