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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.validation.Criterion;
import de.uka.ipd.idaho.gamta.util.validation.Level;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.ProcessPart;
import de.uka.ipd.idaho.gamta.util.validation.Step;
import de.uka.ipd.idaho.gamta.util.validation.Task;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessEditorPanel.TestExtension.TestPartExtension;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;

/**
 * Editor for markup process definitions.
 * 
 * @author sautter
 */
public class MarkupProcessEditorPanel extends JPanel implements MarkupProcessEditorHost {
	
	private MarkupProcessEditorHost host;
	private MarkupProcessEditorExtension extension;
	
	private MarkupProcessEditor processEditor = null;
	
	/**
	 * Constructor
	 * @param host the host object to obtain test documents from
	 * @param extension an extension for the markup process editor, which can
	 *            add additional fields to the editors for the individual parts
	 *            of a markup process
	 */
	public MarkupProcessEditorPanel(MarkupProcessEditorHost host, MarkupProcessEditorExtension extension) {
		super(new BorderLayout(), true);
		this.host = host;
		this.extension = extension;
	}
	
	/**
	 * Retrieve the extension installed in the markup process editor. This is
	 * exactly the extension specified as a constructor argument, if any was
	 * specified.
	 * @return the extension of the markup process editor.
	 */
	public MarkupProcessEditorExtension getExtension() {
		return this.extension;
	}
	
	/**
	 * @return true if the markup process currently opened in the editor was
	 *         modified, false otherwise
	 */
	public boolean isDirty() {
		return ((this.processEditor == null) ? false : this.processEditor.isDirty());
	}
	
	/**
	 * Retrieve the current state of the markup process currently open for
	 * editing.
	 * @return the current markup process
	 */
	public Process getProcess() {
		return ((this.processEditor == null) ? null : this.processEditor.getProcess());
	}
	
	/**
	 * Set the markup process to edit. If there was a markup process opened for
	 * editing proir to the invocation of this method, the changes to that
	 * process are lost. Therefore, they should be retrieved via the
	 * getProcess() method beforehand.
	 * @param process the new markup process to edit
	 */
	public void setProcess(Process process) {
		if (this.processEditor != null)
			this.remove(this.processEditor);
		this.processEditor = new MarkupProcessEditor(this, process);
		this.add(this.processEditor, BorderLayout.CENTER);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.cms.process.flexEditor.MarkupProcessEditorHost#getStartDocument()
	 */
	public QueriableAnnotation getStartDocument() {
		return this.host.getStartDocument();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.cms.process.flexEditor.MarkupProcessEditorHost#getGoldDocument()
	 */
	public QueriableAnnotation getGoldDocument() {
		return this.host.getGoldDocument();
	}
	
	Level selectLevel(boolean forImport) {
		if (this.processEditor == null)
			return null;
		
		TreeMap levelsByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		Process process = this.processEditor.getProcess();
		Level[] levels = process.getLevels();
		
		Process[] externalProcesses = (forImport ? this.getImportablePartSources() : null);
		if ((externalProcesses == null) || (externalProcesses.length == 0)) {
			for (int l = 0; l < levels.length; l++)
				levelsByName.put(levels[l].getFullName(), levels[l]);
		}
		else {
			for (int l = 0; l < levels.length; l++)
				levelsByName.put(("<CurrentProcess>." + levels[l].getFullName()), levels[l]);
			for (int p = 0; p < externalProcesses.length; p++) {
				levels = externalProcesses[p].getLevels();
				for (int l = 0; l < levels.length; l++)
					levelsByName.put((externalProcesses[p].getName() + "." + levels[l].getFullName()), levels[l]);
			}
		}
		
		String selectedLevelName = this.selectString(((String[]) levelsByName.keySet().toArray(new String[levelsByName.size()])), (forImport ? "Select Level to Import" : "Select Level to Move Task to"));
		return ((selectedLevelName == null) ? null : ((Level) levelsByName.get(selectedLevelName)));
	}
	
	Task selectTask(boolean forImport) {
		if (this.processEditor == null)
			return null;
		
		TreeMap tasksByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		Process process = this.processEditor.getProcess();
		Level[] levels = process.getLevels();
		Task[] tasks;
		
		Process[] externalProcesses = (forImport ? this.getImportablePartSources() : null);
		if ((externalProcesses == null) || (externalProcesses.length == 0)) {
			for (int l = 0; l < levels.length; l++) {
				tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++)
					tasksByName.put(tasks[t].getFullName(), tasks[t]);
			}
		}
		else {
			for (int l = 0; l < levels.length; l++) {
				tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++)
					tasksByName.put(("<CurrentProcess>." + tasks[t].getFullName()), tasks[t]);
			}
			for (int p = 0; p < externalProcesses.length; p++) {
				levels = externalProcesses[p].getLevels();
				for (int l = 0; l < levels.length; l++) {
					tasks = levels[l].getTasks();
					for (int t = 0; t < tasks.length; t++)
						tasksByName.put((externalProcesses[p].getName() + "." + tasks[t].getFullName()), tasks[t]);
				}
			}
		}
		
		String selectedTaskName = this.selectString(((String[]) tasksByName.keySet().toArray(new String[tasksByName.size()])), (forImport ? "Select Task to Import" : "Select Task to Move Step to"));
		return ((selectedTaskName == null) ? null : ((Task) tasksByName.get(selectedTaskName)));
	}
	
	Step selectStep(boolean forImport) {
		if (this.processEditor == null)
			return null;
		
		TreeMap stepsByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		Process process = this.processEditor.getProcess();
		Level[] levels = process.getLevels();
		Task[] tasks;
		Step[] steps;
		
		Process[] externalProcesses = (forImport ? this.getImportablePartSources() : null);
		if ((externalProcesses == null) || (externalProcesses.length == 0)) {
			for (int l = 0; l < levels.length; l++) {
				tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++) {
					steps = tasks[t].getSteps();
					for (int s = 0; s < steps.length; s++)
						stepsByName.put(steps[s].getFullName(), steps[s]);
				}
			}
		}
		else {
			for (int l = 0; l < levels.length; l++) {
				tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++) {
					steps = tasks[t].getSteps();
					for (int s = 0; s < steps.length; s++)
						stepsByName.put(("<CurrentProcess>." + steps[s].getFullName()), steps[s]);
				}
			}
			for (int p = 0; p < externalProcesses.length; p++) {
				levels = externalProcesses[p].getLevels();
				for (int l = 0; l < levels.length; l++) {
					tasks = levels[l].getTasks();
					for (int t = 0; t < tasks.length; t++) {
						steps = tasks[t].getSteps();
						for (int s = 0; s < steps.length; s++)
							stepsByName.put((externalProcesses[p].getName() + "." + steps[s].getFullName()), steps[s]);
					}
				}
			}
		}
		
		String selectedStepName = this.selectString(((String[]) stepsByName.keySet().toArray(new String[stepsByName.size()])), (forImport ? "Select Step to Import" : "Select Step to Move Criterion to"));
		return ((selectedStepName == null) ? null : ((Step) stepsByName.get(selectedStepName)));
	}
	
	Criterion selectCriterion(boolean forImport) {
		if (this.processEditor == null)
			return null;
		
		TreeMap criterionsByName = new TreeMap(String.CASE_INSENSITIVE_ORDER);
		
		Process process = this.processEditor.getProcess();
		Level[] levels = process.getLevels();
		Task[] tasks;
		Step[] steps;
		Criterion[] criterions;
		
		Process[] externalProcesses = (forImport ? this.getImportablePartSources() : null);
		if ((externalProcesses == null) || (externalProcesses.length == 0)) {
			for (int l = 0; l < levels.length; l++) {
				tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++) {
					steps = tasks[t].getSteps();
					for (int s = 0; s < steps.length; s++) {
						criterions = steps[s].getCriterions();
						for (int c = 0; c < criterions.length; c++)
							criterionsByName.put(criterions[c].getFullName(), criterions[c]);
					}
				}
			}
		}
		else {
			for (int l = 0; l < levels.length; l++) {
				tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++) {
					steps = tasks[t].getSteps();
					for (int s = 0; s < steps.length; s++) {
						criterions = steps[s].getCriterions();
						for (int c = 0; c < criterions.length; c++)
							criterionsByName.put(("<CurrentProcess>." + criterions[c].getFullName()), criterions[c]);
					}
				}
			}
			for (int p = 0; p < externalProcesses.length; p++) {
				levels = externalProcesses[p].getLevels();
				for (int l = 0; l < levels.length; l++) {
					tasks = levels[l].getTasks();
					for (int t = 0; t < tasks.length; t++) {
						steps = tasks[t].getSteps();
						for (int s = 0; s < steps.length; s++) {
							criterions = steps[s].getCriterions();
							for (int c = 0; c < criterions.length; c++)
								criterionsByName.put((externalProcesses[p].getName() + "." + criterions[c].getFullName()), criterions[c]);
						}
					}
				}
			}
		}
		
		String selectedCriterionName = this.selectString(((String[]) criterionsByName.keySet().toArray(new String[criterionsByName.size()])), (forImport ? "Select Criterion to Import" : "HOW THE HECK DID YOU DO THIS ???"));
		return ((selectedCriterionName == null) ? null : ((Criterion) criterionsByName.get(selectedCriterionName)));
	}
	
	MarkupProcessPartEditor getProcessPartEditor(ProcessPart part) {
		if (this.processEditor == null)
			return null;
		
		String name;
		MarkupProcessPartEditor partEditor;
		
		name = part.getFullName();
		partEditor = this.processEditor.getLevelEditor(name);
		if (partEditor == null)
			return null;
		else if (name.equals(partEditor.getProcessPartName()))
			return partEditor;
		
		name = name.substring(partEditor.getProcessPartName().length() + 1);
		partEditor = ((MarkupLevelEditor) partEditor).getTaskEditor(name);
		if (partEditor == null)
			return null;
		else if (name.equals(partEditor.getProcessPartName()))
			return partEditor;
		
		name = name.substring(partEditor.getProcessPartName().length() + 1);
		partEditor = ((MarkupTaskEditor) partEditor).getStepEditor(name);
		if (partEditor == null)
			return null;
		else if (name.equals(partEditor.getProcessPartName()))
			return partEditor;
		
		name = name.substring(partEditor.getProcessPartName().length() + 1);
		partEditor = ((MarkupStepEditor) partEditor).getCriterionEditor(name);
		if (partEditor == null)
			return null;
		else if (name.equals(partEditor.getProcessPartName()))
			return partEditor;
		else return null;
	}
	
	/**
	 * Provide additional markup processes (beside the one currently edited in
	 * this component) to import levels, tasks, steps, and criterions from. This
	 * default implementation returns an empty array. Sub classes are welcome to
	 * overwrite it as needed.
	 * @return an array holding additional markup processes to import process
	 *         parts from
	 */
	protected Process[] getImportablePartSources() {
		return new Process[0];
	}
	
	private String selectString(String[] options, String label) {
		Window owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		
		StringSelector selector;
		if (owner instanceof Dialog)
			selector = new StringSelector(((Dialog) owner), options, label);
		else if (owner instanceof Frame)
			selector = new StringSelector(((Frame) owner), options, label);
		else selector = new StringSelector(((Frame) null), options, label);
		
		selector.setVisible(true);
		return (selector.isCommitted() ? selector.getSelectedString() : null);
	}
	
	private class StringSelector extends JDialog {
		private JList selector;
		private String selected = null;
		StringSelector(Dialog owner, String[] strings, String title) {
			super(owner, title, true);
			this.init(owner, strings);
		}
		StringSelector(Frame owner, String[] strings, String title) {
			super(owner, title, true);
			this.init(owner, strings);
		}
		private void init(Window owner, String[] strings) {	
			Arrays.sort(strings, String.CASE_INSENSITIVE_ORDER);
			this.selector = new JList(strings);
			this.selector.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			
			this.selector.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						int index = selector.getSelectedIndex();
						if (index != -1)
							commit();
					}
				}
			});
			
			//	initialize main buttons
			JButton commitButton = new JButton("Select");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					commit();
				}
			});
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					selected = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(new JScrollPane(this.selector), BorderLayout.CENTER);
			this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(300, Math.min(Math.max(200, (60 + (strings.length * 25))), 500)));
			this.setLocationRelativeTo(owner);
		}
		private void commit() {
			this.selected = ((String) this.selector.getSelectedValue());
			this.dispose();
		}
		boolean isCommitted() {
			return (this.selected != null);
		}
		String getSelectedString() {
			return this.selected;
		}
	}
	
	public static void main(String[] args) throws Exception {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
//		Process mp = new Process("<enter name>", "<enter nice name>", "<enter description>", "Document not completely marked up.", new Level[0]);
		
		TreeNode pRoot = new Parser().parse(new FileInputStream("E:/GoldenGATEv3/Plugins/MarkupWizardData/TaxonX-Wizard.markupWizard.process.xml"));
		if (TreeNode.ROOT_NODE_TYPE.equals(pRoot.getNodeType()))
			pRoot = pRoot.getChildNode(Process.PROCESS, 0);
		Process mp = new Process(pRoot);
		
		final TestExtension te = new TestExtension();
		final MarkupProcessEditorPanel pe = new MarkupProcessEditorPanel(new MarkupProcessEditorHost() {
			public QueriableAnnotation getGoldDocument() {
				return null;
			}
			public QueriableAnnotation getStartDocument() {
				return null;
			}
		}, te);
		pe.setProcess(mp);
		JFrame frame = new JFrame();
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				try {
					pe.getProcess().writeXml(new PrintWriter(System.out));
					TestPartExtension[] tpes = te.getConcreteInstances();
					for (int t = 0; t < tpes.length; t++) {
						String s = tpes[t].toString();
						if (s != null)
							System.out.println(s);
					}
				}
				catch (IOException ioe) {
					ioe.printStackTrace();
				}
				System.exit(0);
			}
		});
		frame.getContentPane().setLayout(new BorderLayout());
		frame.add(pe, BorderLayout.CENTER);
		frame.setSize(1000, 700);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	static class TestExtension extends MarkupProcessEditorExtension {
		protected MarkupProcessPartEditorExtension produceExtension(MarkupProcessPartEditor ppe) {
			if (ppe instanceof MarkupProcessEditor) return null;
			return new TestPartExtension(ppe);
		}
		
		static class TestPartExtension extends MarkupProcessPartEditorExtension {
			static final String noDp = "<No DocumentProcessor>";
			JComboBox testSelector;
			TestPartExtension(MarkupProcessPartEditor host) {
				super(host);
				this.add(new JLabel("Extension :-)"), BorderLayout.WEST);
				String[] testOptions = {
					noDp,
					"SomeAnalyzer.analyzer@AnalyzerManager",
					"AnotherAnalyzer.analyzer@AnalyzerManager",
					"SomePipeline.pipeline@PipelineManager",
				};
				this.testSelector = new JComboBox(testOptions);
				this.testSelector.setEditable(false);
				this.add(this.testSelector, BorderLayout.CENTER);
			}
			
			public String toString() {
				String selectedDp = this.testSelector.getSelectedItem().toString();
				if (noDp.equals(selectedDp)) return null;
				else return (this.host.getProcessPartFullName() + "->" + selectedDp);
			}
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGateServer.cms.process.flexEditor.MarkupProcessEditorExtension#getInstances()
		 */
		public TestPartExtension[] getConcreteInstances() {
			MarkupProcessPartEditorExtension[] instances = super.getInstances();
			TestPartExtension[] concreteInstances = new TestPartExtension[instances.length];
			for (int i = 0; i < instances.length; i++)
				concreteInstances[i] = ((TestPartExtension) instances[i]);
			return concreteInstances;
		}
	}
}
