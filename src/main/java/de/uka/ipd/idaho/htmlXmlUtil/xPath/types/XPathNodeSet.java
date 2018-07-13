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
package de.uka.ipd.idaho.htmlXmlUtil.xPath.types;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.TreeSet;

import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.xPath.XPath;

/**
 * A node set, as defined in the XPath specification
 * 
 * @author sautter
 */
public class XPathNodeSet extends XPathObject {
	
	private static final Comparator DOC_ORDER = new Comparator() {
		public int compare(Object o1, Object o2) {
			if (o1 == o2) return 0;
			int c = (((TreeNode) o1).getDocumentOrderPosition() - ((TreeNode) o2).getDocumentOrderPosition());
			return ((c == 0) ? (o1.hashCode() - o2.hashCode()) : c);
		}
	};
	
	private static final Comparator REVERSE_DOC_ORDER = new Comparator() {
		public int compare(Object o1, Object o2) {
			return -DOC_ORDER.compare(o1, o2);
		}
	};
	
	private ArrayList nodeList = null;
	private TreeSet nodeSet;
	
	public final boolean isReverseDocOrder;
	
	public XPathNodeSet() {
		this(false);
	}
	
	public XPathNodeSet(boolean reverse) {
		this.isReverseDocOrder = reverse;
		this.nodeSet = new TreeSet(this.isReverseDocOrder ? REVERSE_DOC_ORDER : DOC_ORDER);
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathObject#asBoolean()
	 */
	public XPathBoolean asBoolean() {
		return new XPathBoolean(this.size() != 0);
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathObject#asNumber()
	 */
	public XPathNumber asNumber() {
		return this.asString().asNumber();
	}
	
	/** @see de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathObject#asString()
	 */
	public XPathString asString() {
		if (this.isEmpty())
			return new XPathString("");
		return XPath.stringValue(this.getFirst());
	}
	
	/**	filter the nodes in this NodeSet by their type
	 * @param	filterType	the type to use as the filter
	 * @return a new NodeSet containing the nodes from this NodeSet that have the specified type 
	 */
	public XPathNodeSet filterByType(String filterType) {
		if (filterType == null)
			return this;
		XPathNodeSet result = new XPathNodeSet(this.isReverseDocOrder);
		for (int n = 0; n < this.size(); n++) {
			TreeNode node = this.get(n);
			if (filterType.equals(node.getNodeType())) result.add(node);
		}
		return result;
	}
	
	public boolean add(TreeNode node) {
		if ((node != null) && this.nodeSet.add(node)) {
			this.nodeList = null;
			return true;
		}
		else return false;
	}
	
	public boolean addAll(XPathNodeSet nodeSet) {
		if ((nodeSet != null) && this.nodeSet.addAll(nodeSet.nodeSet)) {
			this.nodeList = null;
			return true;
		}
		else return false;
	}
	
	public boolean remove(TreeNode node) {
		if ((node != null) && this.nodeSet.remove(node)) {
			if (this.nodeList != null)
				this.nodeList.remove(node);
			return true;
		}
		else return false;
	}
	
	public boolean isEmpty() {
		return this.nodeSet.isEmpty();
	}
	
	public int size() {
		return this.nodeSet.size();
	}
	
	public TreeNode getFirst() {
		if (this.nodeList == null)
			this.nodeList = new ArrayList(this.nodeSet);
		return ((this.size() == 0) ? null : ((TreeNode) this.nodeList.get(0)));
	}
	
	public TreeNode getLast() {
		if (this.nodeList == null)
			this.nodeList = new ArrayList(this.nodeSet);
		return ((this.size() == 0) ? null : ((TreeNode) this.nodeList.get(this.size() - 1)));
	}
	
	/**	get the node at index
	 * @param	index	the index of the desired node by the rules of a JAVA List, i.e. 0 ... (size() - 1)
	 * @return the TreeNode at the specified index
	 */
	public TreeNode get(int index) {
		if ((index >= 0) && (index < this.size())) {
			if (this.nodeList == null)
				this.nodeList = new ArrayList(this.nodeSet);
			return ((TreeNode) this.nodeList.get(index));
		}
		else return null;
	}
	
	/**	get the node at position
	 * @param	position	the index of the desired node by the rules of XPath, i.e. 1 ... size()
	 * @return the TreeNode at the specified index
	 */
	public TreeNode nodeAt(int position) {
		return this.get(position - 1);
	}
	
	/**	get the index of a TreeNode in this NodeSet
	 * @param	node	the TreeNode to find
	 * @return the index of the specified TreeNode in this NodeSet by the rules of a JAVA List, i.e. 0 ... (size() - 1)
	 */
	public int indexOf(TreeNode node) {
		if (this.nodeList == null)
			this.nodeList = new ArrayList(this.nodeSet);
		return this.nodeList.indexOf(node);
	}
	
	/**	get the position of a TreeNode in this NodeSet
	 * @param	node	the TreeNode to find
	 * @return the position of the specified TreeNode in this NodeSet by the rules of XPath, i.e. 1 ... size()
	 */
	public int positionOf(TreeNode node) {
		return this.indexOf(node) + 1;
	}
//	
//	private static final Comparator DOC_ORDER = new Comparator() {
//		public int compare(Object o1, Object o2) {
//			if (o1 == o2) return 0;
//			if (o1 == null) return 1;
//			if (o2 == null) return -1;
//			if (!(o1 instanceof TreeNode)) return 1;
//			if (!(o2 instanceof TreeNode)) return -1;
//			return (((TreeNode) o1).getDocumentOrderPosition() - ((TreeNode) o2).getDocumentOrderPosition());
//		}
//	};
//	
//	private ArrayList nodeList = new ArrayList();
//	private HashSet nodeSet = new HashSet();
//	
//	private boolean isSorted = false;
//	public final boolean isReverseDocOrder;
//	
//	public XPathNodeSet() {
//		this(false);
//	}
//	
//	public XPathNodeSet(boolean reverse) {
//		this.isReverseDocOrder = reverse;
//	}
//	
//	/** @see de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathObject#asBoolean()
//	 */
//	public XPathBoolean asBoolean() {
//		return new XPathBoolean(this.size() != 0);
//	}
//	
//	/** @see de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathObject#asNumber()
//	 */
//	public XPathNumber asNumber() {
//		return this.asString().asNumber();
//	}
//	
//	/** @see de.uka.ipd.idaho.htmlXmlUtil.xPath.types.XPathObject#asString()
//	 */
//	public XPathString asString() {
//		if (this.isEmpty()) return new XPathString("");
//		return XPath.stringValue(this.getFirst());
//	}
//	
//	/**	filter the nodes in this NodeSet by their type
//	 * @param	filterType	the type to use as the filter
//	 * @return a new NodeSet containing the nodes from this NodeSet that have the specified type 
//	 */
//	public XPathNodeSet filterByType(String filterType) {
//		if (filterType == null) return this;
//		XPathNodeSet result = new XPathNodeSet(this.isReverseDocOrder);
//		for (int n = 0; n < this.size(); n++) {
//			TreeNode node = this.get(n);
//			if (filterType.equals(node.getNodeType())) result.add(node);
//		}
//		return result;
//	}
//	
//	public boolean add(TreeNode node) {
//		if ((node == null) || this.nodeSet.contains(node)) return false;
//		this.nodeList.add(node);
//		this.nodeSet.add(node);
//		this.isSorted = false;
//		return true;
//	}
//	
//	public boolean addAll(XPathNodeSet nodeSet) {
//		if ((nodeSet == null) || nodeSet.isEmpty()) return false;
//		boolean changed = false;
//		for (int n = 0; n < nodeSet.size(); n++)
//			changed = this.add(nodeSet.get(n)) || changed;
//		return changed;
//	}
//	
//	public boolean remove(TreeNode node) {
//		this.nodeList.remove(node);
//		return this.nodeSet.remove(node);
//	}
//	
//	public boolean isEmpty() {
//		return this.nodeSet.isEmpty();
//	}
//	
//	public int size() {
//		return this.nodeSet.size();
//	}
//	
//	public TreeNode getFirst() {
//		if (!this.isSorted) this.sort();
//		return ((this.size() == 0) ? null : ((TreeNode) this.nodeList.get(0)));
//	}
//	
//	public TreeNode getLast() {
//		if (!this.isSorted) this.sort();
//		return ((this.size() == 0) ? null : ((TreeNode) this.nodeList.get(this.size() - 1)));
//	}
//	
//	/**	get the node at index
//	 * @param	index	the index of the desired node by the rules of a JAVA List, i.e. 0 ... (size() - 1)
//	 * @return the TreeNode at the specified index
//	 */
//	public TreeNode get(int index) {
//		if (!this.isSorted) this.sort();
//		if ((index >= 0) && (index < this.size())) return ((TreeNode) this.nodeList.get(index));
//		return null;
//	}
//	
//	/**	get the node at position
//	 * @param	position	the index of the desired node by the rules of XPath, i.e. 1 ... size()
//	 * @return the TreeNode at the specified index
//	 */
//	public TreeNode nodeAt(int position) {
//		if (!this.isSorted) this.sort();
//		return this.get(position - 1);
//	}
//	
//	/**	get the index of a TreeNode in this NodeSet
//	 * @param	node	the TreeNode to find
//	 * @return the index of the specified TreeNode in this NodeSet by the rules of a JAVA List, i.e. 0 ... (size() - 1)
//	 */
//	public int indexOf(TreeNode node) {
//		if (!this.isSorted) this.sort();
//		return this.nodeList.indexOf(node);
//	}
//	
//	/**	get the position of a TreeNode in this NodeSet
//	 * @param	node	the TreeNode to find
//	 * @return the position of the specified TreeNode in this NodeSet by the rules of XPath, i.e. 1 ... size()
//	 */
//	public int positionOf(TreeNode node) {
//		if (!this.isSorted) this.sort();
//		return this.nodeList.indexOf(node) + 1;
//	}
//	
//	//	sort the NodeSet in document order
//	private void sort() {
//		Collections.sort(this.nodeList, DOC_ORDER);
//		if (this.isReverseDocOrder) Collections.reverse(this.nodeList);
//		this.isSorted = true;
//	}
}
