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
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationListener;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.AttributeUtils;
import de.uka.ipd.idaho.gamta.CharSequenceListener;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableCharSequence;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.StandaloneAnnotation;
import de.uka.ipd.idaho.gamta.MutableCharSequence.CharSequenceEvent;

/**
 * An editor widget for manipulating Annotations in a GUI. The actual JTextPane
 * used for displaying the content mutable annotation resides in
 * BorderLayout.CENTER position of this panel. Users are free to add additional
 * panels to the BorderLayout.NORTH, BorderLayout.EAST, BorderLayout.WEST and
 * BorderLayout.SOUTH positions.
 * 
 * @author sautter
 */
public class AnnotationEditor extends JPanel {
	
	/**
	 * Interface for a single option to (conditionally) appear in the
	 * AnnotationEditor's context menu.
	 * 
	 * @author sautter
	 */
	public static interface ContextMenuOption {
		
		/**
		 * Retrieve a JMenuItem to make this option accessible for the current
		 * text selection in the editor. For this method, all argument
		 * Annotations are read-only, so any attempt of a modification will
		 * result in an exception being thrown. ActionListeners registered to
		 * the JMenuItems returned by this method shoud perform some action by
		 * injecting an AnnotationAction to the target AnnotationEditor's
		 * performAction() method. This callback mechanism is necessary for the
		 * AnnotationEditor to have the control over the actions being performed
		 * (preparation -> actual action -> post-processing, eg for undo
		 * management).<br>
		 * Note that this method may also return a JMenu containing several
		 * JMenuItems, which will appear as a sub menu of the context menu
		 * displayed. This allows for one ContextMenuOption to providing several
		 * actual options at once, e.g. for different annotation types present
		 * in the editor content.
		 * @param data the content of the editor, thus the annotation to work
		 *            on.
		 * @param selection a StandaloneAnnotation marking the text currently
		 *            selected in the display (null if no selection)
		 * @param selected an array holding the Annotations overlapping with the
		 *            current text selection in the display, or Annotations
		 *            enclosing the caret position if no text is selected
		 * @param editor the AnnotationEditor showing the menu, thus the
		 *            component to call back for performing the actual action
		 *            via an AnnotationAction object.
		 * @return a JMenuItem making this option accessible for the current
		 *         text selection, or null, if this option does not want to be
		 *         accessible for the current selection
		 */
		public abstract JMenuItem getMenuItem(QueriableAnnotation data, Annotation selection, Annotation[] selected, AnnotationEditor editor);
	}
	
	/**
	 * Interface for a single keyboard shortcut to be available in the
	 * AnnotationEditor. Note that all shortcuts of this category must be
	 * invoked using 'Ctrl' or 'Alt', plus optionally 'Shift', plus a custom key
	 * character. The key characters are case insensitive and have to be from
	 * the range a-z/A-Z or 0-9.<br>
	 * Consequently, either the controlDown() or the altDown() method has to
	 * return true. There is one exception to this rule, however: If the
	 * textEditable property of the host AnnotationEditor is false, using
	 * shortcuts that invoked plainly by means of their key character is
	 * permissible, since basic text input is disabled. Having both
	 * controlDown() and altDown() return true is prohibited, since 'Ctrl-Alt'
	 * is used to call operation system native functions on some platforms. In
	 * addition, 'Ctrl-Shift' in combination with a number key (0-9) may also be
	 * mapped to operation system functions on some platforms and thus may not
	 * be able to invoke a shortcut on the AnnotationEditor.<br>
	 * If shiftDown() returns false, the invokation of this shortcut is
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
	 * Note that registering a shortcut to be invoked by 'Ctrl' plus a key
	 * character only can shadow shortcuts native to the text editor component,
	 * such as 'Ctrl-C' for copying the currently selected text to the system
	 * clipboard.
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
		
		private final String getShortcutIdentifier(boolean textEditable) {
			
			//	produce 'Ctrl'/'Alt' part of key
			String ctrl = (this.controlDown() ? CONTROL_KEY_PREFIX : "");
			String alt = (this.altDown() ? ALT_KEY_PREFIX : "");
			
			//	catch shortcuts using both of 'Ctrl' alt 'Alt', and ones using neither if text is editable
			int ctrlAltLen = (ctrl.length() + alt.length());
			if ((ctrlAltLen > Math.max(ctrl.length(), alt.length())) || (textEditable && (ctrlAltLen == 0))) return null;
			
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
		 * @param selection a StandaloneAnnotation marking the text currently
		 *            selected in the display (null if no selection)
		 * @param selected an array holding the Annotations overlapping with the
		 *            current text selection in the display, or Annotations
		 *            enclosing the caret position if no text is selected
		 * @return the annotation action to perform on behalf of the shortcut,
		 *         or null, if the shortcut is not applicable in the context of
		 *         the current selection
		 */
		public abstract AnnotationAction getAction(boolean shiftDown, QueriableAnnotation data, Annotation selection, Annotation[] selected);
	}
	
	/**
	 * The callback object to be injected in the AnnotationEditor's
	 * performAction method for performing the actual action that results from a
	 * click in the context menu or a shortcut invoked by a combination of keys.
	 * 
	 * @author sautter
	 */
	public static abstract class AnnotationAction {
		
		private AnnotationEditor target = null;
		
		/**
		 * Obtain an annotation marking the text currently selected in the
		 * target annotation editor (if this action is not in the process of
		 * being executed in an invocation of
		 * AnnotationEditor.performAction(), this method returns null)
		 * @return an Annotation marking the text currently selected
		 */
		protected final StandaloneAnnotation getSelection() {
			if (this.target == null) return null;
			else return this.target.selection;
		}
		
		/**
		 * Obtain the annotations currently selected in the target
		 * annotation editor (if this action is not in the process of being
		 * executed in an invocation of AnnotationEditor.performAction(),
		 * this method returns null)
		 * @return the Annotations whose highlights overlap with the current
		 *         selection, or, if there is no selection, the Annotation
		 *         whose highlight the last click was in, or null, if the
		 *         last click was not within an Annotation's highlight
		 */
		protected final MutableAnnotation[] getSelectedAnnotations() {
			if (this.target == null) return null;
			else if (this.target.selection == null) {
				MutableAnnotation[] selected = {this.target.lastClicked};
				return selected;
			}
			else return this.target.getAnnotationsOverlapping(this.target.selection);
		}
		
		/**
		 * Perform the actual action on a mutable annotation, given a piece
		 * of selected text and a set of selected annotations
		 * @param data the content of the editor, thus the mutable
		 *            annotation to work on. If the textEditable property of
		 *            the surrounding AnnotationEditor is set to false, any
		 *            attempt of modifying the text will throw an
		 *            IllegalStateException
		 */
		public abstract void performAction(MutableAnnotation data);
	}
	
	private boolean textEditable = false;
	
	private HashMap annotationColors = new HashMap();
	
	private ArrayList contextMenuOptions = new ArrayList();
	private ArrayList shortcuts = new ArrayList();
	private HashMap shortcutMap = new HashMap();
	
	private JTextPane editor = new JTextPane();
	private StyledDocument editorDocument;
	
	private JPopupMenu contextMenu = new JPopupMenu();
	
	private SimpleAttributeSet textFontStyle = new SimpleAttributeSet();
	
	private MutableAnnotation content;
	private MutableAnnotation originalContent;
	
	private TextSynchronizer synchronizer;
	private TextChangeRecorder changeRecorder;
	
	private StandaloneAnnotation selection = null;
	private MutableAnnotation lastClicked = null;
	
	/**
	 * Constructor
	 */
	public AnnotationEditor() {
		super(new BorderLayout(), true);
		
		this.editor.setEditable(false);
		this.add(new JScrollPane(this.editor), BorderLayout.CENTER);
		
		this.textFontStyle.addAttribute(StyleConstants.FontConstants.Family, "Verdana");
		this.textFontStyle.addAttribute(StyleConstants.FontConstants.Size, new Integer(12));
		this.textFontStyle.addAttribute(StyleConstants.ColorConstants.Foreground, Color.BLACK);
		this.textFontStyle.addAttribute(StyleConstants.ColorConstants.Background, Color.WHITE);
		
		this.applySpans();
		
		this.editor.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent me) {
				
				//	get current status
				storeStatusForClick(me);
				
				//	if right click, show menu
				if (me.getButton() != MouseEvent.BUTTON1)
					showPopupMenu(me, editor.viewToModel(me.getPoint()));
			}
		});
		
		this.editor.addKeyListener(new KeyListener() {
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
	
	private HashMap highlightAttributeCache = new HashMap();
	
	private AttributeSet getAnnotationHighlight(String type) {
		Color color = this.getAnnotationColor(type);
		if (color == null) return null;
		
		SimpleAttributeSet highlightAttributes = ((SimpleAttributeSet) this.highlightAttributeCache.get(type));
		if (highlightAttributes == null) {
			highlightAttributes = new SimpleAttributeSet();
			highlightAttributes.addAttribute(StyleConstants.ColorConstants.Background, color);
			this.highlightAttributeCache.put(type, highlightAttributes);
		}
		return highlightAttributes;
	}
	
	private void applySpans() {
		//	nothing to lay out
		if (this.content == null) return;
		
		//	do layout
		this.applySpans(0, this.editorDocument.getLength());
	}
	
	private void applySpans(int start, int end) {
		
		//	nothing to lay out
		if (this.content == null) return;
		
		//	reset display
		this.editorDocument.setCharacterAttributes(0, this.editorDocument.getLength(), this.textFontStyle, true);
		
		//	highlight annotations
		Annotation[] annotations = this.content.getAnnotations();
		for (int a = 0; a < annotations.length; a++) {
			AttributeSet highlight = this.getAnnotationHighlight(annotations[a].getType());
			if (highlight != null)
				this.editorDocument.setCharacterAttributes(annotations[a].getStartOffset(), annotations[a].length(), highlight, false);
		}
		
		//	make changes visible
		this.editor.validate();
		
		try { //  show caret
			this.editor.getCaret().setVisible(true);
			this.editor.getCaret().setBlinkRate(500);
		} catch (Exception e) {}
	}
	
	private void showPopupMenu(MouseEvent me, int clickPosition) {
		if (this.content == null) return;
		
		//	get annotation for selected text
		Annotation[] selected;
		
		//	no text selected, use click postion
		if (this.selection == null) {
			if (this.lastClicked == null) selected = new Annotation[0];
			else {
				selected = new Annotation[1];
				selected[0] = this.lastClicked;
			}
		}
		
		//	use selected text
		else selected = this.getAnnotationsOverlapping(this.selection);
		
		//	get menu items for available actions
		JMenuItem[] menuItems = this.getContextMenuItems(this.content, this.selection, selected);
		
		//	no actions to display
		if (menuItems.length == 0) return;
		
		//	fill and show context menu
		else {
			this.contextMenu.removeAll();
			for (int i = 0; i < menuItems.length; i++)
				this.contextMenu.add(menuItems[i]);
			this.contextMenu.show(this.editor, me.getX(), me.getY());
		}
	}
	
	private void handleKeyStroke(KeyEvent ke, int lastKeyCode) {
		if (this.content == null) return;
		
		System.out.println("Key typed in AnnotationEditor");
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
		if ((ctrlAltLen > Math.max(ctrl.length(), alt.length())) || (this.textEditable && (ctrlAltLen == 0))) return;
		
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
		
		//	get annotations selected by their highlights
		Annotation[] selected;
		
		//	no text selected, use click postion
		if (this.selection == null) {
			if (this.lastClicked == null) selected = new Annotation[0];
			else {
				selected = new Annotation[1];
				selected[0] = this.lastClicked;
			}
		}
		
		//	use selected text
		else selected = this.getAnnotationsOverlapping(this.selection);
		
		//	get action
		AnnotationAction action = shortcut.getAction(ke.isShiftDown(), this.content, this.selection, selected);
		
		//	execute action
		if (action != null) this.performAction(action);
		
		//	consume key event
		ke.consume();
	}
	
	private void storeStatusForClick(MouseEvent me) {
		
		//	nothing to store status for
		if (this.content == null) return;
		
		//	get currently selected text
		this.selection = annotateSelection();
		
		//	get annotations whose highlight is selected
		this.lastClicked = getAnnotationAt(this.editor.viewToModel(me.getPoint()));
	}
	
	private void storeStatusForKeyStroke() {
		
		//	nothing to store status for
		if (this.content == null) return;
		
		//	get currently selected text
		this.selection = annotateSelection();
		
		//	get annotations whose highlight is selected
		this.lastClicked = getAnnotationAt(this.editor.getCaretPosition());
	}
	
	private StandaloneAnnotation annotateSelection() {
		int start = this.editor.getSelectionStart();
		int end = this.editor.getSelectionEnd();
		
		if (start == end)
			return null;
		
		else if (end < start) {
			int temp = end;
			end = start;
			start = temp;
		}
		
		int left;
		int right;
		
		//	use binary search to narrow interval
		left = 0;
		right = this.content.size();
		int startTokenIndex = 0;
		while ((right - left) > 2) {
			startTokenIndex = ((left + right) / 2);
			if (this.content.tokenAt(startTokenIndex).getEndOffset() <= start) left = startTokenIndex;
			else if (this.content.tokenAt(startTokenIndex).getStartOffset() > start) right = startTokenIndex;
			else break;
		}
		
		//	scan remaining interval
		startTokenIndex = left;
		while (startTokenIndex < this.content.size()) {
			if (this.content.tokenAt(startTokenIndex).getEndOffset() <= start) startTokenIndex++;
			else break;
		}
		
		//	use binary search to narrow interval
		left = startTokenIndex;
		right = this.content.size();
		int endTokenIndex = this.content.size() - 1;
		while ((right - left) > 2) {
			endTokenIndex = ((left + right) / 2);
			if (this.content.tokenAt(endTokenIndex).getEndOffset() < end) left = endTokenIndex;
			else if (this.content.tokenAt(endTokenIndex).getStartOffset() >= end) right = endTokenIndex;
			else break;
		}
		
		//	scan remaining interval
		endTokenIndex = Math.min(right, (this.content.size() - 1));
		while (endTokenIndex > -1) {
			if (this.content.tokenAt(endTokenIndex).getStartOffset() >= end) endTokenIndex--;
			else break;
		}
		
		if (startTokenIndex > endTokenIndex) return null;
		
		return Gamta.newAnnotation(this.content, null, startTokenIndex, (endTokenIndex - startTokenIndex + 1));
	}
	
	private MutableAnnotation getAnnotationAt(int position) {
		MutableAnnotation[] annotations = this.content.getMutableAnnotations();
		LinkedList annotationList = new LinkedList();
		for (int a = 0; a < annotations.length; a++) {
			if ((annotations[a].getStartOffset() <= position) && (annotations[a].getEndOffset() > position) && this.annotationColors.containsKey(annotations[a].getType()))
				annotationList.addLast(annotations[a]);
		}
		return (annotationList.isEmpty() ? null : ((MutableAnnotation) annotationList.getLast()));
	}
	
	private MutableAnnotation[] getAnnotationsOverlapping(Annotation selection) {
		MutableAnnotation[] annotations = this.content.getMutableAnnotations();
		ArrayList annotationList = new ArrayList();
		for (int a = 0; a < annotations.length; a++) {
			if (AnnotationUtils.overlaps(annotations[a], selection) && this.annotationColors.containsKey(annotations[a].getType()))
				annotationList.add(annotations[a]);
		}
		return ((MutableAnnotation[]) annotationList.toArray(new MutableAnnotation[annotationList.size()]));
	}
	
	/**
	 * set the content of this AnnotationEditor. If this AnnotationEditor was
	 * previously used to edit a different mutable annotation, make sure to
	 * invoke commitChanges() if you want to retain changes made to the previous
	 * content.
	 * @param content the new content to be edited in the AnnotationEditor
	 */
	public void setContent(MutableAnnotation content) {
		
		if (this.content != null) {
			this.content.removeCharSequenceListener(this.synchronizer);
			this.editorDocument.removeDocumentListener(this.synchronizer);
			this.synchronizer = null;
			
			this.content.removeCharSequenceListener(this.changeRecorder);
			this.changeRecorder = null;
		}
		
		this.originalContent = content;
		this.content = Gamta.copyDocument(content);
		
		this.editorDocument = new DefaultStyledDocument();
		for (int v = 0; v < this.content.size(); v++) try {
			this.editorDocument.insertString(this.editorDocument.getLength(), this.content.valueAt(v), null);
			this.editorDocument.insertString(this.editorDocument.getLength(), this.content.getWhitespaceAfter(v), null);
		}
		catch (BadLocationException ble) {
			//	this should never happen with the offsets used above, but no way of telling the compiler ...
			System.out.println("BadLocationException writing content to display: " + ble.getMessage());
			ble.printStackTrace(System.out);
		}
		
		this.synchronizer = new TextSynchronizer();
		this.content.addCharSequenceListener(this.synchronizer);
		this.editorDocument.addDocumentListener(this.synchronizer);
		
		this.changeRecorder = new TextChangeRecorder();
		this.content.addCharSequenceListener(this.changeRecorder);
		
		this.applySpans();
		
		this.editor.setEditable(this.textEditable);
		this.editor.setDocument(this.editorDocument);
	}
	
	/**
	 * Write the changes made to the working copy of the content mutable
	 * annotation through to this very annotation. The annotation editor will
	 * write all changes made since the last invokation of either one of
	 * setContent(), commitChanges(), or reset() through to the content mutable
	 * annotation. The retionale behind using a working copy for editing is in
	 * order to facilitate resetting the content / cancelling dialogs, etc.
	 */
	public void commitChanges() {
		if (this.content == null) return;
		
		//	write through changes to text
		this.changeRecorder.writeChanges(this.originalContent);
		
		//	get annotations from both original content and working copy
		Annotation[] originalAnnotations = this.originalContent.getAnnotations();
		Annotation[] annotations = this.content.getAnnotations();
		
		//	sort out annotations that didn't change
		int oai = 0;
		for (int a = 0; a < annotations.length; a++) {
			
			//	find next original content annotation
			while ((oai < originalAnnotations.length) && (originalAnnotations[oai].getStartIndex() < annotations[a].getStartIndex()))
				oai++;
			
			//	sort out unchanged annotations
			if ((oai < originalAnnotations.length) && AnnotationUtils.equals(originalAnnotations[oai], annotations[a], true)) {
				
				//	adjust attributes if necessary
				if (!AttributeUtils.hasEqualAttributes(originalAnnotations[oai], annotations[a])) {
					originalAnnotations[oai].clearAttributes();
					originalAnnotations[oai].copyAttributes(annotations[a]);
				}
				
				//	annotations done, remove them from arrays
				originalAnnotations[oai++] = null;
				annotations[a] = null;
			}
		}
		
		//	remove old markup that was modified
		for (int oa = 0; oa < originalAnnotations.length; oa++) {
			if ((originalAnnotations[oa] != null) && !AnnotationUtils.equals(this.originalContent, originalAnnotations[oa]))
				this.originalContent.removeAnnotation(originalAnnotations[oa]);
		}
		
		//	copy new markup from working copy
		for (int a = 0; a < annotations.length; a++) {
			if ((annotations[a] != null) && !DocumentRoot.DOCUMENT_TYPE.equals(annotations[a].getType())) {
				if (AnnotationUtils.equals(this.originalContent, annotations[a])) {
					this.originalContent.clearAttributes();
					this.originalContent.copyAttributes(annotations[a]);
				}
				else this.originalContent.addAnnotation(annotations[a]);
			}
		}
	}
	
	/**
	 * Undo the changes made to the working copy of the content mutable
	 * annotation through to this very annotation. The annotation editor will
	 * undo all changes made since the last invokation of either one of
	 * setContent(), commitChanges(), or rest(). The retionale behind using a
	 * working copy for editing is in order to facilitate resetting the content /
	 * cancelling dialogs, etc.
	 */
	public void reset() {
		if (this.originalContent != null)
			this.setContent(this.originalContent);
	}
	
	private class TextSynchronizer implements CharSequenceListener, DocumentListener {
		private boolean inChange = false;
		
		public void changedUpdate(DocumentEvent de) {}
		
		public void insertUpdate(DocumentEvent de) {
			
			//	ignore document events originating from writing through changes from content
			if (this.inChange) return;
			
			//	write insertion through to content
			int offset = de.getOffset();
			try {
				String inserted = editorDocument.getText(offset, de.getLength());
				this.inChange = true;
				content.insertChars(inserted, offset);
				this.inChange = false;
			}
			catch (BadLocationException ble) {
				//	due to the selection of offset and length, this should never happen
				System.out.println("BadLocationException writing insert from display to content: " + ble.getMessage());
				ble.printStackTrace(System.out);
			}
		}
		
		public void removeUpdate(DocumentEvent de) {
			
			//	ignore document events originating from writing through changes from content
			if (this.inChange) return;
			
			//	write removal through to content
			this.inChange = true;
			content.removeChars(de.getOffset(), de.getLength());
			this.inChange = false;
		}
		
		public void charSequenceChanged(CharSequenceEvent change) {
			
			//	ignore char sequence events originating from writing through changes from editor document
			if (this.inChange) return;
			
			//	write through removal
			if (change.removed.length() != 0) try {
					this.inChange = true;
					editorDocument.remove(change.offset, change.removed.length());
					this.inChange = false;
				}
				catch (BadLocationException ble) {
					//	due to the selection of offset and length, this should never happen
					System.out.println("BadLocationException writing removal from content to display: " + ble.getMessage());
					ble.printStackTrace(System.out);
				}
				
			//	write through insertion
			if (change.inserted.length() != 0) try {
					this.inChange = true;
					editorDocument.insertString(change.offset, change.inserted.toString(), null);
					this.inChange = false;
					applySpans();
				}
				catch (BadLocationException ble) {
					//	due to the selection of offset and length, this should never happen
					System.out.println("BadLocationException writing insertion from content to display: " + ble.getMessage());
					ble.printStackTrace(System.out);
				}
		}
	}
	
	private class TextChangeRecorder implements CharSequenceListener {
		private LinkedList changes = new LinkedList();
		
		public void charSequenceChanged(CharSequenceEvent change) {
			this.changes.addLast(change);
		}
		
		private void writeChanges(MutableCharSequence target) {
			while (!this.changes.isEmpty()) {
				CharSequenceEvent change = ((CharSequenceEvent) this.changes.removeFirst());
				if ((change.inserted.length() != 0) && (change.removed.length() != 0))
					target.setChars(change.inserted, change.offset, change.removed.length());
				else if (change.inserted.length() != 0)
					target.insertChars(change.inserted, change.offset);
				else if (change.removed.length() != 0)
					target.removeChars(change.offset, change.removed.length());
			}
		}
	}
	
	/**
	 * @return the textEditable property of this AnnotationEditor
	 */
	public boolean isTextEditable() {
		return this.textEditable;
	}
	
	/**
	 * @param textEditable The textEditable to set.
	 */
	public synchronized void setTextEditable(boolean textEditable) {
		
		//	refresh shortcut mapping if property changes
		if (this.textEditable != textEditable) {
			this.shortcutMap.clear();
			Shortcut[] shortcuts = this.getShortcuts();
			for (int s = 0; s < shortcuts.length; s++) {
				String sid = shortcuts[s].getShortcutIdentifier(textEditable);
				if (sid != null) this.shortcutMap.put(sid, shortcuts[s]);
			}
		}
		
		//	set property
		this.textEditable = textEditable;
		this.editor.setEditable(this.textEditable);
	}
	
	/**
	 * set the font to use for displaying the content text
	 * @param font the font to use for displaying the content text
	 */
	public void setTextFont(Font font) {
		this.textFontStyle.addAttribute(StyleConstants.FontConstants.Family, font.getFamily());
		this.applySpans();
	}
	
	/**
	 * @return the Font currently in use for displaying the content text
	 */
	public Font getTextFont() {
		return Font.decode(this.textFontStyle.getAttribute(StyleConstants.FontConstants.Family).toString());
	}
	
	/**
	 * set the font size to use for displaying the content text
	 * @param size the font size to use for displaying the content text
	 */
	public void setFontSize(int size) {
		this.textFontStyle.addAttribute(StyleConstants.FontConstants.Size, new Integer(size));
		this.applySpans();
	}
	
	/**
	 * @return the font size currently in use for displaying the content text
	 */
	public int getFontSize() {
		Integer fontSize = ((Integer) this.textFontStyle.getAttribute(StyleConstants.FontConstants.Size));
		return ((fontSize == null) ? 12 : fontSize.intValue());
	}
	
	/**
	 * set the font color to use for displaying the content text
	 * @param color the font color to use for displaying the content text
	 */
	public void setFontColor(Color color) {
		this.textFontStyle.addAttribute(StyleConstants.ColorConstants.Foreground, color);
		this.applySpans();
	}
	
	/**
	 * @return the font color currently in use for displaying the content text
	 */
	public Color getFontColor() {
		return ((Color) this.textFontStyle.getAttribute(StyleConstants.FontConstants.Foreground));
	}
	
	/**
	 * set the color to use for the display background
	 * @param color the color to use for the display background
	 */
	public void setBackgroundColor(Color color) {
		this.textFontStyle.addAttribute(StyleConstants.ColorConstants.Background, color);
		this.applySpans();
	}
	
	/**
	 * @return the color currently in use for the display background
	 */
	public Color getBackGroundColor() {
		return ((Color) this.textFontStyle.getAttribute(StyleConstants.FontConstants.Background));
	}
	
	/**
	 * set the highlight color for the annotations of a specific type (setting
	 * the color to null will prevent annotations of the specified type from
	 * being highlighted)
	 * @param annotationType the annotation type to set the highlight color for
	 * @param color the new highlight color for the specified annotation type
	 */
	public void setAnnotationColor(String annotationType, Color color) {
		if (annotationType != null) {
			
			//	perform change
			if (color == null) this.annotationColors.remove(annotationType);
			else this.annotationColors.put(annotationType, color);
			
			//	make change visible
			if ((this.content != null) && (this.content.getAnnotations(annotationType).length != 0))
				this.applySpans();
		}
	}
	
	/**
	 * retrieve the highlight color for an annotation type
	 * @param annotationType the annotation type to get the highlight color for
	 * @return the highlight color for the specified annotation type, or null,
	 *         if no color is set for this annotation type
	 */
	public Color getAnnotationColor(String annotationType) {
		return ((Color) this.annotationColors.get(annotationType));
	}
	
	/**
	 * @return an array holding the annotation types currently highlighted, in
	 *         other words, the annotation types a highlight color is set for
	 */
	public String[] getVisibleAnnotationTypes() {
		String[] types = ((String[]) this.annotationColors.keySet().toArray(new String[this.annotationColors.size()]));
		Arrays.sort(types, String.CASE_INSENSITIVE_ORDER);
		return types;
	}
	
	/**
	 * add a ContextMenuOption to be available in the context menu of the
	 * AnnotationEditor
	 * @param option the ContextMenuOption to add
	 */
	public void addContextMenuOption(ContextMenuOption option) {
		this.contextMenuOptions.add(option);
	}
	
	/**
	 * remove a ContextMenuOption from the context menu of the AnnotationEditor
	 * @param option the ContextMenuOption to remove
	 */
	public void removeContextMenuOption(ContextMenuOption option) {
		this.contextMenuOptions.remove(option);
	}
	
	/**
	 * retrieve all ContextMenuOptions currently registered to this
	 * AnnotationEditor
	 * @return an array holding all ContextMenuOptions currently registered to
	 *         this AnnotationEditor
	 */
	public ContextMenuOption[] getContextMenuOptions() {
		return ((ContextMenuOption[]) this.contextMenuOptions.toArray(new ContextMenuOption[this.contextMenuOptions.size()]));
	}
	
	/**
	 * remove all ContextMenuOptions from this AnnotationEditor
	 */
	public void clearContextMenuOptions() {
		this.contextMenuOptions.clear();
	}
	
	/*
	 * retrieve the JMenuItems for displaying a context menu for the current
	 * text selection
	 * @param data the content of the editor, thus the mutable annotation to
	 *            work on. If the textEditable property of the surrounding
	 *            AnnotationEditor is set to false, any attempt of modifying the
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
	private JMenuItem[] getContextMenuItems(MutableAnnotation data, Annotation selection, Annotation[] selected) {
		ArrayList menuItems = new ArrayList();
		for (int a = 0; a < this.contextMenuOptions.size(); a++) {
			JMenuItem mi = ((ContextMenuOption) this.contextMenuOptions.get(a)).getMenuItem(data, selection, selected, this);
			if (mi != null) menuItems.add(mi);
		}
		return ((JMenuItem[]) menuItems.toArray(new JMenuItem[menuItems.size()]));
	}
	
	/**
	 * add a Shortcut to be available in the AnnotationEditor
	 * @param shortcut the Shortcut to add
	 */
	public void addShortcut(Shortcut shortcut) {
		this.shortcuts.add(shortcut);
		String sid = shortcut.getShortcutIdentifier(this.textEditable);
		if (sid != null) this.shortcutMap.put(sid, shortcut);
	}
	
	/**
	 * remove a Shortcut from the context menu of the AnnotationEditor
	 * @param shortcut the Shortcut to remove
	 */
	public void removeShortcut(Shortcut shortcut) {
		this.shortcuts.remove(shortcut);
		this.shortcutMap.remove(shortcut.getShortcutIdentifier(this.textEditable));
	}
	
	/**
	 * retrieve all Shortcuts currently registered to this AnnotationEditor
	 * @return an array holding all Shortcuts currently registered to this
	 *         AnnotationEditor
	 */
	public Shortcut[] getShortcuts() {
		return ((Shortcut[]) this.shortcuts.toArray(new Shortcut[this.shortcuts.size()]));
	}
	
	/**
	 * remove all Shortcuts from this AnnotationEditor
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
		if (this.content == null) return;
		
		AnnotationChangeRecorder acr = new AnnotationChangeRecorder();
		this.content.addAnnotationListener(acr);
		
		try {
			action.target = this;
			action.performAction(this.content);
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
		
		//	TODO: build atomic undo unit from changes
		
		//	adjust display to changes performed
		this.content.removeAnnotationListener(acr);
		if (acr.annotationsModified)
			this.applySpans();
	}
	
	private class AnnotationChangeRecorder implements AnnotationListener {
		private boolean annotationsModified = false;
		
		public void annotationAdded(QueriableAnnotation doc, Annotation annotation) {
			this.annotationsModified = (this.annotationsModified || annotationColors.containsKey(annotation.getType()));
		}
		
		public void annotationRemoved(QueriableAnnotation doc, Annotation annotation) {
			this.annotationsModified = (this.annotationsModified || annotationColors.containsKey(annotation.getType()));
		}
		
		public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType) {
			this.annotationsModified = (this.annotationsModified || annotationColors.containsKey(oldType) || annotationColors.containsKey(annotation.getType()));
		}
		
		public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue) {}
	}
}
