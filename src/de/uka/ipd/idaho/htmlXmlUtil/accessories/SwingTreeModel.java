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
package de.uka.ipd.idaho.htmlXmlUtil.accessories;


import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;

import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;

/**
 * Implementation of the TreeModel for a Java Swing JTree. This model out of the
 * box allows displaying and navigating a tree of TreeNode objects in a Swing
 * GUI. It does not support TreeModelListeners, though.
 * 
 * @author sautter
 */
public class SwingTreeModel implements javax.swing.tree.TreeModel {
	
	TreeNode root;
	
	public SwingTreeModel(TreeNode root) {
		this.root = root;
	}
	
	/** @see javax.swing.tree.TreeModel#getRoot()
	 */
	public Object getRoot() {
		return this.root;
	}
	
	/** @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
	 */
	public int getChildCount(Object node) {
		return ((node instanceof TreeNode) ? ((TreeNode) node).getChildNodeCount() : 0);
	}
	
	/** @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
	 */
	public boolean isLeaf(Object node) {
		return ((node instanceof TreeNode) ? (((TreeNode) node).getChildNodeCount() == 0) : true);
	}
	
	/** @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
	 */
	public void addTreeModelListener(TreeModelListener tml) {
	}
	
	/** @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
	 */
	public void removeTreeModelListener(TreeModelListener tml) {}
	
	/** @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
	 */
	public Object getChild(Object parent, int index) {
		if (parent instanceof TreeNode) {
			TreeNode pNode = (TreeNode) parent;
			return ((pNode.getChildNodeCount() > index) ? pNode.getChildNode(index) : null);
		}
		else return null;
	}
	
	/** @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
	 */
	public int getIndexOfChild(Object parent, Object child) {
		if ((parent != null) && (child != null) && (parent instanceof TreeNode) && (child instanceof TreeNode)) {
			for (int i = 0; i < ((TreeNode) parent).getChildNodeCount(); i++)
				if (((TreeNode) parent).getChildNode(i) == ((TreeNode) child)) return i;
		}
		return -1;
	}
	
	/** @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
	 */
	public void valueForPathChanged(TreePath path, Object newValue) {}
}
