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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import de.uka.ipd.idaho.gamta.util.ControllingProgressMonitor;

/**
 * JPanel implementation of a progress monitor, to easily integrate monitoring
 * in a user interface. If a panel is constructed to display control buttons,
 * the monitored process must not run in the Swing event dispatch thread.
 * 
 * @author sautter
 */
public class ProgressMonitorPanel extends JPanel implements ControllingProgressMonitor {
	
	private JLabel step = new JLabel("", JLabel.LEFT);
	
	private JLabel info = new JLabel("", JLabel.CENTER);
	private ArrayList infoLines = new ArrayList();
	private int infoLineLimit = 3;
	
	private int baseProgress = 0;
	private int maxProgress = 100;
	private JProgressBar progress = new JProgressBar();
	
	private boolean pause = false;
	private Object pauseLock = null;
	private JButton pauseResumeButton = null;
	
	private boolean abort = false;
	private JButton abortButton = null;
	
	private Dimension buttonSize = new Dimension(80, 21);
	
	private String abortExceptionMessage = "Aborted at user's request.";
	
	/**
	 * Constructor
	 */
	public ProgressMonitorPanel() {
		this(false, false);
	}
	
	/**
	 * Constructor
	 * @param supportPauseResume add button for pausing/resuming the monitored
	 *            process?
	 * @param supportAbort add button for aborting the monitored process?
	 */
	public ProgressMonitorPanel(boolean supportPauseResume, boolean supportAbort) {
		super(new BorderLayout(), true);
		
		this.progress.setStringPainted(true);
		
		if (supportPauseResume || supportAbort) {
			JPanel infoPanel = new JPanel(new BorderLayout(), true);
			infoPanel.add(this.step, BorderLayout.NORTH);
			infoPanel.add(this.info, BorderLayout.CENTER);
			infoPanel.add(this.progress, BorderLayout.SOUTH);
			
			this.pauseLock = new Object();
			
			if (supportPauseResume) {
				this.pauseResumeButton = new JButton("Pause");
				this.pauseResumeButton.setPreferredSize(this.buttonSize);
				this.pauseResumeButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.pauseResumeButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						pauseResume();
					}
				});
			}
			
			if (supportAbort) {
				this.abortButton = new JButton("Abort");
				this.abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
				this.abortButton.setPreferredSize(this.buttonSize);
				this.abortButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						abort();
					}
				});
			}
			
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER), true);
			if (this.pauseResumeButton != null)
				buttonPanel.add(this.pauseResumeButton);
			if (this.abortButton != null)
				buttonPanel.add(this.abortButton);
			
			this.add(infoPanel, BorderLayout.CENTER);
			this.add(buttonPanel, BorderLayout.SOUTH);
		}
		else {
			this.add(this.step, BorderLayout.NORTH);
			this.add(this.info, BorderLayout.CENTER);
			this.add(this.progress, BorderLayout.SOUTH);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#supportsPauseResume()
	 */
	public boolean supportsPauseResume() {
		return (this.pauseResumeButton != null);
	}
	void pauseResume() {
		synchronized(this.pauseLock) {
			if (this.pause) {
				this.pause = false;
				this.pauseResumeButton.setText("Pause");
				this.pauseLock.notify();
			}
			else {
				this.pause = true;
				this.pauseResumeButton.setText("Pausing ...");
				this.pauseResumeButton.setEnabled(false);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ControllingProgressMonitor#setPauseResumeEnabled(boolean)
	 */
	public void setPauseResumeEnabled(boolean pre) {
		if (this.pauseResumeButton != null)
			this.pauseResumeButton.setEnabled(pre);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#supportsAbort()
	 */
	public boolean supportsAbort() {
		return (this.abortButton != null);
	}
	void abort() {
		synchronized(pauseLock) {
			this.abort = true;
			this.abortButton.setText("Aborting ...");
			this.abortButton.setEnabled(false);
			if (this.pauseResumeButton != null)
				this.pauseResumeButton.setEnabled(false);
			this.pauseLock.notify();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ControllingProgressMonitor#setAbortEnabled(boolean)
	 */
	public void setAbortEnabled(boolean ae) {
		if (this.abortButton != null)
			this.abortButton.setEnabled(ae);
	}
	
	/**
	 * @see javax.swing.JLabel#getHorizontalAlignment()
	 * @return the constant indicating the horizontal alignment of the step
	 *         label
	 */
	public int getStepLabelAlignment() {
		return this.step.getHorizontalAlignment();
	}
	
	/**
	 * Set the horizontal alignment (text orientation) of the step label, i.e.,
	 * the area in which the panel displays strings handed to the setStep()
	 * method.
	 * @see javax.swing.JLabel#setHorizontalAlignment(int)
	 * @param alignment the constant indicating the alignment of the step label
	 */
	public void setStepLabelAlignment(int alignment) {
		this.step.setHorizontalAlignment(alignment);
	}
	
	/**
	 * @see javax.swing.JLabel#getHorizontalAlignment()
	 * @return the constant indicating the horizontal alignment of the info
	 *         label
	 */
	public int getInfoLabelAlignment() {
		return this.info.getHorizontalAlignment();
	}
	
	/**
	 * Set the horizontal alignment (text orientation) of the info label, i.e.,
	 * the area in which the panel displays strings handed to the setInfo()
	 * method.
	 * @see javax.swing.JLabel#setHorizontalAlignment(int)
	 * @param alignment the constant indicating the alignment of the info label
	 */
	public void setInfoLabelAlignment(int alignment) {
		this.info.setHorizontalAlignment(alignment);
	}
	
	/**
	 * @return the maximum number of info messages to display
	 */
	public int getInfoLineLimit() {
		return this.infoLineLimit;
	}
	
	/**
	 * Set the maximum number of info messages to display. If this number is
	 * greater than 1, the panel displays the last <code>infoLineLimit</code>
	 * strings handed to the setInfo() method, latest one at the bottom. Calling
	 * this method with an argument &lt;1 has no effect.
	 * @param infoLineLimit the maximum number of info messages to display
	 */
	public void setInfoLineLimit(int infoLineLimit) {
		if (infoLineLimit < 1)
			return;
		this.infoLineLimit = infoLineLimit;
	}
	
	/**
	 * @return the size of the pause/resume and abort buttons
	 */
	public Dimension getButtonSize() {
		return new Dimension(this.buttonSize.width, this.buttonSize.height);
	}
	
	/**
	 * Set the size for the pause/resume and abort buttons. This method is best
	 * called before the progress monitor panel is made visible. If this panel
	 * has been constructed not to display either of the pause/resume and abort
	 * buttons, this method has no effect.
	 * @param buttonSize the size for the pause/resume and abort buttons
	 */
	public void setButtonSize(Dimension buttonSize) {
		if (buttonSize == null)
			return;
		if (this.buttonSize.equals(buttonSize))
			return;
		this.buttonSize = buttonSize;
		if (this.pauseResumeButton != null)
			this.pauseResumeButton.setPreferredSize(this.buttonSize);
		if (this.abortButton != null)
			this.abortButton.setPreferredSize(this.buttonSize);
		this.revalidate();
		this.repaint();
	}
	
	/**
	 * @return the message text of the exception thrown when the abort button is
	 *         clicked
	 */
	public String getAbortExceptionMessage() {
		return this.abortExceptionMessage;
	}
	
	/**
	 * Set the message text of the runtime exception to throw in the monitored
	 * process when the abort button is clicked. Invoking this method with a
	 * null argument has no effect. If this panel has been constructed not to
	 * display an abort button, this method has no effect.
	 * @param exceptionText the message text of the exception to throw on abort
	 * @see de.uka.ipd.idaho.gamta.util.ControllingProgressMonitor#setAbortExceptionMessage(java.lang.String)
	 */
	public void setAbortExceptionMessage(String aem) {
		if (aem != null)
			this.abortExceptionMessage = aem;
	}
	
	private final void checkPauseAbort() {
		if (this.pauseLock == null)
			return;
		
		if (this.pause) // do unsynchronized check first (saves acquiring monitor in normal operation)
			synchronized (this.pauseLock) {
				if (this.pause) {
					this.pauseResumeButton.setText("Resume");
					this.pauseResumeButton.setEnabled(true);
					try {
						this.pauseLock.wait();
					} catch (InterruptedException ie) {}
				}
			}
		
		if (this.abort)
			throw new RuntimeException(this.abortExceptionMessage);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setStep(java.lang.String)
	 */
	public void setStep(String step) {
		System.out.println(step);
		try {
			this.step.setText("<HTML><B>" + step + "</B></HTML>");
//			this.step.validate();
		}
		catch (Exception e) {} // we have to do this due to some weird runtime behavior in Swing
		catch (Error e) {} // we have to do this due to some weird runtime behavior in Swing
		this.checkPauseAbort();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setInfo(java.lang.String)
	 */
	public void setInfo(String info) {
		System.out.println(info);
		this.infoLines.add(info);
		while (this.infoLines.size() > this.infoLineLimit)
			this.infoLines.remove(0);
		
		StringBuffer infoText = new StringBuffer("<HTML>" + this.infoLines.get(0));
		for (int l = 1; l < this.infoLines.size(); l++)
			infoText.append("<BR>" + ((String) this.infoLines.get(l)));
		infoText.append("</HTML>");
		try {
			this.info.setText(infoText.toString());
//			this.info.validate();
		}
		catch (Exception e) {} // we have to do this due to some weird runtime behavior in Swing
		catch (Error e) {} // we have to do this due to some weird runtime behavior in Swing
		this.checkPauseAbort();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setBaseProgress(int)
	 */
	public void setBaseProgress(int baseProgress) {
		this.baseProgress = baseProgress;
		this.checkPauseAbort();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setMaxProgress(int)
	 */
	public void setMaxProgress(int maxProgress) {
		this.maxProgress = maxProgress;
		this.checkPauseAbort();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.util.ProgressMonitor#setProgress(int)
	 */
	public void setProgress(int progress) {
		this.progress.setValue(this.baseProgress + (((this.maxProgress - this.baseProgress) * progress) / 100));
		this.checkPauseAbort();
	}
}
