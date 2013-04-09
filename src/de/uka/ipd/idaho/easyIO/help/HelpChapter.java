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
package de.uka.ipd.idaho.easyIO.help;


import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.tree.TreeNode;

/**
 * This class represents an individual chapter (usually an HTML page) of a help.
 * It implements TreeNode in order to make the help navigatable in a JTree. The
 * title and content of the help chapter represented by an instance of this
 * class have to be specified as constructor arguments. For larger help pages,
 * it is recommended to use DynamicHelpChapter instead.
 * 
 * @author sautter
 */
public class HelpChapter implements TreeNode {
	
	protected HelpChapter parent = null;
	private Vector subChapters = new Vector();
	
	protected String title = "Help";
	protected String text = "Help is underway";
	
	/**	Constructor
	 * @param 	title	the title for this HelpChapter
	 * @param 	text	the text of this HelpChapter
	 */
	public HelpChapter(String title, String text) {
		this.title = title;
		this.text = text;
	}
	
	/**
	 * Overwrites the toString() method in class Object This implementation
	 * returns the same String as the getTitle() method so that the title is
	 * displayed in the JTree
	 */
	public String toString() {
		return this.getTitle();
	}

	/**
	 * @return the title of this help chapter
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * @return a Reader the text of this help chapter can be read from
	 */
	public Reader getTextReader() {
		return new StringReader(this.text);
	}

	/**
	 * add a sub chapter to this help chapter (will be accessible as a child
	 * node of this help chapter's node in the navigation)
	 * @param subChapter the new sub chapter
	 */
	public void addSubChapter(HelpChapter subChapter) {
		if ((subChapter != null) && (subChapter != this)) {
			this.subChapters.addElement(subChapter);
			subChapter.parent = this;
		}
	}
	
	/** @see javax.swing.tree.TreeNode#getChildAt(int)
	 */
	public TreeNode getChildAt(int index) {
		if ((index < 0) || (index >= this.subChapters.size())) return null;
		return ((TreeNode) this.subChapters.get(index));
	}
	
	/** @see javax.swing.tree.TreeNode#getChildCount()
	 */
	public int getChildCount() {
		return this.subChapters.size();
	}
	
	/** @see javax.swing.tree.TreeNode#getParent()
	 */
	public TreeNode getParent() {
		return this.parent;
	}
	
	/** @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
	 */
	public int getIndex(TreeNode node) {
		return this.subChapters.indexOf(node);
	}
	
	/** @see javax.swing.tree.TreeNode#getAllowsChildren()
	 */
	public boolean getAllowsChildren() {
		return true;
	}
	
	/** @see javax.swing.tree.TreeNode#isLeaf()
	 */
	public boolean isLeaf() {
		return this.subChapters.isEmpty();
	}
	
	/** @see javax.swing.tree.TreeNode#children()
	 */
	public Enumeration children() {
		return Collections.enumeration(this.subChapters);
	}
}
