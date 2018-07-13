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


import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import de.uka.ipd.idaho.gamta.MutableTokenSequence;

/**
 * Synchronizer writing the changes made in an arbitrary JTextComponent through to a mutable token sequence 
 * 
 * @author sautter
 */
public class DocumentSynchronizer implements DocumentListener {
	
	/**
	 * Synchronize a mutable token sequence with a JTextComponent displaying it
	 * for editing. This method adds a DocumentListener to the the specified
	 * JTextComponent's content Document. Further, it automatically fills the
	 * specified JTextComponent with the text represented by the specified
	 * mutable token sequence.
	 * @param content the mutable token sequence to display
	 * @param display the JTextComponent displaying the token sequence
	 */
	public static DocumentSynchronizer synchronizeDocuments(MutableTokenSequence content, JTextComponent display) {
		return new DocumentSynchronizer(content, display);
	}
	
	private DocumentEvent lastEvent = null;
	
	private MutableTokenSequence content;
	private JTextComponent display;
	
	private boolean isWritingDisplay = false;
	
	/**	Constructor
	 * @param	content		the mutable token sequence to display
	 * @param	display		the JTextComponent displaying the token sequence
	 * Note:	This constuctor will add the newly constructed DocumentSynchronizer to the the specified JTextComponent's content Document as a DocumentListener
	 * Note: 	This constructor will automatically fill the specified JTextComponent with the text represented by the specified DocumentPart
	 * @deprecated replaced by the static synchronizeDocuments() method, will go private in the future
	 */
	public DocumentSynchronizer(MutableTokenSequence content, JTextComponent display) {
		this.content = content;
		this.display = display;
		this.writeContentToDisplay();
		this.display.getDocument().addDocumentListener(this);
	}
	
	/** @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
	 */
	public void changedUpdate(DocumentEvent de) {}
	
	/** @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
	 */
	public synchronized void insertUpdate(DocumentEvent de) {
		if (this.isWritingDisplay)
			return;
		if (this.lastEvent == de)
			return;
		
		this.lastEvent = de;
		
		int start = de.getOffset();
		int length = de.getLength();
		
		try {
			String inserted = de.getDocument().getText(start, length);
			this.content.insertChars(inserted, start);
		}
		catch (BadLocationException e) {
			refreshDisplayContent();
		}
	}
	
	/** @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
	 */
	public synchronized void removeUpdate(DocumentEvent de) {
		if (this.isWritingDisplay) return;
		
		if (this.lastEvent == de) return;
		this.lastEvent = de;
		
		int start = de.getOffset();
		int length = de.getLength();
		
		this.content.removeChars(start, length);
	}
	
	private void refreshDisplayContent() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				writeContentToDisplay();
			}
		});
	}
	
	/**	force the DocumentSynchronizer to write the content of its DocumentPart to its JTextComponent 
	 */
	public synchronized void writeContentToDisplay() {
		
		//	disable event handling for the time writing
		this.isWritingDisplay = true;
		this.display.setText("");
		this.display.setEnabled(false);
		
		//	produce text
		Document document = this.display.getDocument();
		
		//	feed dummy document to display so the updates do not incure repaint operations
		try {
			Document dummy = ((Document) document.getClass().newInstance());
			this.display.setDocument(dummy);
		} catch (InstantiationException e1) {} 
		catch (IllegalAccessException e1) {}
		
		try {
			document.insertString(document.getLength(), content.getLeadingWhitespace(), null);
		} catch (Exception e) {}
		for (int t = 0; t < content.size(); t++) {
			try {
				document.insertString(document.getLength(), this.content.valueAt(t), null);
			} catch (Exception e) {}
			try {
				document.insertString(document.getLength(), this.content.getWhitespaceAfter(t), null);
			} catch (Exception e) {}
		}
		
		//	feed original document back to display
		this.display.setDocument(document);
		
		//	re-enable event handling
		this.display.setEnabled(true);
		this.isWritingDisplay = false;
	}
}
