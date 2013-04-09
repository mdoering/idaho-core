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
package de.uka.ipd.idaho.htmlXmlUtil;


import java.io.IOException;
import java.util.Vector;

import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;


/**
 * A TreeNode represents a single node in the tree retresentation of HTML/XML
 * data. It is highly similar to a DOM element and fully compatible to the DOM
 * specification, despite some method naming differences.
 * 
 * @author sautter
 */
public class TreeNode {
	
	/**
	 * Type for synthetic root nodes automatically added to input data having no
	 * root node of its own, i.e. for input which is a sequence of HTML/XML
	 * documents rather than a single document
	 */
	public static final String ROOT_NODE_TYPE = "PcRoot";
	
//	public static final String ELEMENT_NODE_TYPE = "PcElement";
	
	/**
	 * Type for nodes holding character data
	 */
	public static final String DATA_NODE_TYPE = "PcData";
	
	/**
	 * Type for nodes holding comments
	 */
	public static final String COMMENT_NODE_TYPE = "PcComment";
	
	/**
	 * Type for nodes holding embedded DTD data
	 */
	public static final String DTD_NODE_TYPE = "PcDTD";
	
	/**
	 * Type for nodes holding embedded processing instructions
	 */
	public static final String PROCESSING_INSTRUCTION_NODE_TYPE = "PcInstruction";
	
	/**
	 * Type for atttribute nodes. This type is only used in DOM compatibility
	 * mode, otherwise a TreeNode returns its attributes as plain strings.
	 */
	public static final String ATTRIBUTE_NODE_TYPE = "PcAttribute";
	
	private final String nodeType;
	private final String nodeValue;
	private TreeNode parentNode;
	private boolean marked = false;
	
	private int documentOrderPosition = -1;
	private boolean isAttribute = false;

	private TreeNodeAttributeSet attributes;
	private Vector childNodes = new Vector();

	/**	Fully custom constructor
	 * @param	parent		the new node's parent node
	 * @param	type		the new node's type
	 * @param	value		the new node's value, will be empty except if type is DATA_NODE_TYPE, COMMENT_NODE_TYPE or ATTRIBUTE_NODE_TYPE
	 * @param	attributes	the new node's attribute / value pairs, stored in a java.util.Properties
	 */
	public TreeNode(TreeNode parent, String type, String value, TreeNodeAttributeSet attributes) {
		this.nodeType = type;
		this.nodeValue = ((type.equalsIgnoreCase(DATA_NODE_TYPE) || type.equalsIgnoreCase(COMMENT_NODE_TYPE) || type.equalsIgnoreCase(ATTRIBUTE_NODE_TYPE) || type.equalsIgnoreCase(PROCESSING_INSTRUCTION_NODE_TYPE) || type.equalsIgnoreCase(DTD_NODE_TYPE)) ? value : "");
		this.parentNode = parent;
		this.attributes = attributes;
	}
	
	/**	Constructor leaving the new node's attributes empty
	 * @param	parent	the new node's parent node
	 * @param	type	the new node's type
	 * @param	value	the new node's value, will be empty except if type is DATA_NODE_TYPE, COMMENT_NODE_TYPE or ATTRIBUTE_NODE_TYPE
	 */
	public TreeNode(TreeNode parent, String type, String value) {
		this(parent, type, value, TreeNodeAttributeSet.getTagAttributes(null, null));
	}
	
	/**	Constructor leaving the new node's value and attributes empty
	 * @param	parent	the new node's parent node
	 * @param	type	the new node's type
	 */
	public TreeNode(TreeNode parent, String type) {
		this(parent, type, "", TreeNodeAttributeSet.getTagAttributes(null, null));
	}
	
	/**	add the specified node to this node's child nodes
	 */
	public void addChildNode(TreeNode newChildNode) {
		if (newChildNode != null) this.childNodes.addElement(newChildNode);
	}
	
	/**	remove the specified node from this node's children
	 */
	public void removeChildNode(TreeNode childNode) {
		int index = 0;
		while (index < this.childNodes.size()) {
			if (this.childNodes.get(index) == childNode) {
				this.childNodes.removeElementAt(index);
				index = this.childNodes.size();
			}
			else index++;
		}
	}
	
	/**	replace one of this node's child nodes by another node
	 * @param	oldChild	the child node to be replaced
	 * @param	newChild	the node to replace the child node with
	 * 	Note: 	If the node to be replaced is NULL or is not a child of this node, 
	 * 			the new node is added to this nodes chil nodes without removing another node
	 */
	public void replaceChildNode(TreeNode oldChild, TreeNode newChild) {
		boolean replaced = (newChild != null);
		for (int i = 0; (i < this.childNodes.size()) && !replaced && (oldChild != null); i++) {
			if (this.childNodes.get(i) == oldChild) {
				this.childNodes.setElementAt(newChild, i);
				replaced = true;
			}
		}
		if (!replaced) this.addChildNode(newChild);
	}
	
	/**	remove all child nodes from this node
	 */
	public void removeChildNodes() {
		this.childNodes.clear();
	}
	
	/**	@return	true if and only if this node has at least one child node of the specified type
	 */
	public boolean hasChildNodeOfType(String type) {
		for (int i = 0; i < this.childNodes.size(); i++)
			if (((TreeNode) this.childNodes.get(i)).getNodeType().equalsIgnoreCase(type)) return true;
		
		return false;
	}
	
	/**	@return	the number of this node's child nodes
	 */
	public int getChildNodeCount() {
		return this.childNodes.size();
	}
	
	/**	@return	the number of this node's child nodes that have the specified type
	 */
	public int getChildNodeCount(String type) {
		int counter = 0;
		for (int i = 0; i < this.childNodes.size(); i++)
			if (((TreeNode) this.childNodes.get(i)).getNodeType().equalsIgnoreCase(type)) counter++;
		
		return counter;
	}
	
	/**	@return	the index of the argument node within this node, or -1 if the specified node is not a child node of this node
	 */
	public int getChildNodeIndex(TreeNode node) {
		for (int i = 0; i < this.childNodes.size(); i++)
			if (node == this.childNodes.get(i)) return i;
		return -1;
	}

	/**	@return	the type specific index of the argument node within this node, or -1 if the specified node is not a child node of this node
	 */
	public int getTypeSpecificChildNodeIndex(TreeNode node) {
		int counter = 0;
		for (int i = 0; i < this.childNodes.size(); i++) {
			if (node == this.childNodes.get(i)) return counter;
			if (node.getNodeType().equalsIgnoreCase(((TreeNode) this.childNodes.get(i)).getNodeType())) counter++;
		}
		return -1;
	}

	/**	@return	all of this node's child nodes
	 */
	public TreeNode[] getChildNodes() {
		return ((TreeNode[]) this.childNodes.toArray(new TreeNode[this.childNodes.size()]));
	}
	
	/**	@return	all of this node's child nodes that have the specified type
	 */
	public TreeNode[] getChildNodes(String type) {
		if (type == null) return this.getChildNodes();
		Vector children = new Vector();
		for (int i = 0; i < this.childNodes.size(); i++) if (((TreeNode) this.childNodes.get(i)).getNodeType().equalsIgnoreCase(type)) children.addElement(this.childNodes.get(i));
		return ((TreeNode[]) children.toArray(new TreeNode[children.size()]));
	}
	
	/**	get one of this node's child nodes, specified by it's over all index
	 * @param	index	the child node's type specifc index
	 * @return	the index-th of this node's child nodes
	 */
	public TreeNode getChildNode(int index) {
		if (index < this.childNodes.size())
			return ((TreeNode) this.childNodes.get(index));
		else return null;
	}
	
	/**	get one of this node's child nodes, specified by it's type specific index
	 * @param	type	the type of the child node
	 * @param	index	the child node's type specifc index
	 * @return	the index-th of this node's child nodes, counting only nodes of the specified type
	 */
	public TreeNode getChildNode(String type, int index) {
		int counter = -1;
		for (int i = 0; i < this.childNodes.size(); i++) {
			if (((TreeNode) this.childNodes.get(i)).getNodeType().equalsIgnoreCase(type)) counter++;
			if (counter == index) return ((TreeNode) this.childNodes.get(i));
		}
		return null;
	}
	
	/**	@return	this node's parent node
	 */
	public TreeNode getParent() {
		return this.parentNode;
	}

	/**	sets this node's parent node to the specified node	
	 */
	public void setParent(TreeNode parent) {
		this.parentNode = parent;
	}
	
	/**	@return	this node's type (if the node is an attribute node, this method returns the attribute's name)
	 */
	public String getNodeType() {
		return (this.isAttribute ? this.nodeValue : this.nodeType);
	}
	
	/**	@return	this node's value
	 */
	public String getNodeValue() {
		return (this.isAttribute ? this.parentNode.getAttribute(this.nodeValue) : this.nodeValue);
	}
	
	/**	@return	true if and only if this TreeNode has the specified attribute
	 */
	public boolean hasAttribute(String attribute) {
		return this.attributes.containsAttribute(attribute);
	}
	
	/**	read the value of an attribute
	 * @param	attribute	the name of the attribute
	 * @return	the value of the specified attribute, or null, if the TreeNode has no attribute with the specified name
	 */
	public String getAttribute(String attribute) {
		return this.attributes.getAttribute(attribute);
	}
	
	/**	read the value of an attribute
	 * @param	attribute	the name of the attribute
	 * @return	the value of the specified attribute as a TreeNode, or null, if the TreeNode has no attribute with the specified name
	 * 	Note:	This method unnecessarily produces an additional TreeNode, it is provided only for DOM compatibility
	 */
	public TreeNode getAttributeNode(String attribute) {
		TreeNode attributeNode = new TreeNode(this, ATTRIBUTE_NODE_TYPE, attribute);
		attributeNode.isAttribute = true;
		attributeNode.documentOrderPosition = this.documentOrderPosition;
		return attributeNode;
	}
	
	/**	@return	true if and only if this TreeNode is a DOM compatibility attribute node
	 */
	public boolean isAttributeNode() {
		return this.isAttribute;
	}
	
	/**	read the value of an attribute
	 * @param	attribute	the name of the attribute
	 * @param	def			the value to return if the attribute is not set
	 * @return	the value of the specified attribute, or def, if the TreeNode has no attribute with the specified name
	 */
	public String getAttribute(String attribute, String def) {
		return this.attributes.getAttribute(attribute, def);
	}
	
	/**	add a attribute & value pair to this node
	 * @param 	attribute	the attribute
	 * @param 	value		the attribute's value
	 * 	Note: if the specified attribute is already set for this node, it's value is changed to the specified value 
	 */
	public String setAttribute(String attribute, String value) {
		return this.attributes.setAttribute(attribute, value);
	}
	
	/**	remove the specified attribute from this node
	 */
	public String removeAttribute(String attribute) {
		return this.attributes.removeAttribute(attribute);
	}
	
	/**	clear this node's attributes
	 */
	public void clearAttributes() {
		this.attributes.clear();
	}
	
	/**	convert this node and it's subtree to code, using a standard Grammar 
	 * @return	a code representation of this tree as a String
	 */
	public String treeToCode() {
		return this.treeToCode(new StandardGrammar());
	}
	/**	convert this node and it's subtree to code, using a standard Grammar
	 * @param 	indent		the (whitespace) sequence to be inserted at the beginning of a line once per tree level for indention
	 * @return	a code representation of this tree as a String
	 */
	public String treeToCode(String indent) {
		return this.treeToCode(indent, 0, (new StandardGrammar()));
	}
	/**	convert this node and it's subtree to code, using the specified Grammar's standard indention
	 * @param	grammar		the Grammar according to which the code is to be generated 
	 * @return	a code representation of this tree as a String
	 */
	public String treeToCode(Grammar grammar) {
		Grammar g = ((grammar != null) ? grammar : new StandardGrammar());
		return this.treeToCode(g.getStandardIndent(), 0, g);
	}
	/**	convert this node and it's subtree to code
	 * @param 	indent		the (whitespace) sequence to be inserted at the beginning of a line once per tree level for indention
	 * @param	grammar		the Grammar according to which the code is to be generated 
	 * @return	a code representation of this tree as a String
	 */
	public String treeToCode(String indent, Grammar grammar) {
		return this.treeToCode(indent, 0, ((grammar != null) ? grammar : new StandardGrammar()));
	}
	
	//	main conversion method for recursive code generation out of the tree
	private String treeToCode(String ind, int level, Grammar gram) {
		
		Grammar grammar = ((gram != null) ? gram : new StandardGrammar());
		
		String indent = "";
		for (int i = 0; i < level; i++)
			indent = indent + ind;
		
//		//	data or comment node
//		if (this.nodeType.equalsIgnoreCase(DATA_NODE_TYPE) || this.nodeType.equalsIgnoreCase(COMMENT_NODE_TYPE) || this.nodeType.equalsIgnoreCase(DTD_NODE_TYPE) || this.nodeType.equalsIgnoreCase(PROCESSING_INSTRUCTION_NODE_TYPE))
//			return this.nodeValue + "\n";
//		
		//	comment, DTD, etc. node
		if (this.nodeType.equalsIgnoreCase(COMMENT_NODE_TYPE) || this.nodeType.equalsIgnoreCase(DTD_NODE_TYPE) || this.nodeType.equalsIgnoreCase(PROCESSING_INSTRUCTION_NODE_TYPE))
			return this.nodeValue + "\n";
		
		//	data node
		else if (this.nodeType.equalsIgnoreCase(DATA_NODE_TYPE))
//			return grammar.escape(this.nodeValue) + "\n";
			return ((this.nodeValue.trim().length() == 0) ? "" : (grammar.escape(this.nodeValue) + "\n"));
		
		//	singular data node as only child 
		else if ((this.childNodes.size() == 1) && ((TreeNode) this.childNodes.get(0)).getNodeType().equalsIgnoreCase(DATA_NODE_TYPE))
			return (indent + this.getStartTag(grammar) + grammar.escape(((TreeNode) this.childNodes.get(0)).getNodeValue()) + this.getEndTag(grammar) + "\n");
		
		//	no child nodes
		else if (this.childNodes.size() == 0)
			return (indent + this.getSingularTag(grammar) + "\n");
		
		//	otherwise
		else {
			StringBuffer returnValue = new StringBuffer();
			
			for (int i = 0; i < this.childNodes.size(); i++)
				if (this.childNodes.get(i) != null) returnValue = returnValue.append(((TreeNode) this.childNodes.get(i)).treeToCode(ind, (this.nodeType.equals(ROOT_NODE_TYPE) ? level : (level + 1)), grammar));
			
			//	root node
			if (this.nodeType.equals(ROOT_NODE_TYPE))
				return returnValue.toString();
			
			//	non-root node
			else return (indent + this.getStartTag(grammar) + "\n" + returnValue.toString() + indent + this.getEndTag(grammar) + "\n");
		}
	}
	
	/**	convert this node and it's subtree to code, using a standard Grammar
	 * @param	receiver	the TokenReceiver to store the code in, one token per line
	 */
	public void treeToCode(TokenReceiver receiver) throws IOException {
		this.treeToCode(receiver, new StandardGrammar());
	}
	/**	convert this node and it's subtree to code, using a standard Grammar
	 * @param	receiver	the TokenReceiver to store the code in, one token per line
	 * @param 	indent		the (whitespace) sequence to be inserted at the beginning of a line once per tree level for indention
	 */
	public void treeToCode(TokenReceiver receiver, String indent) throws IOException {
		this.treeToCode(receiver, indent, 0, (new StandardGrammar()));
	}
	/**	convert this node and it's subtree to code, using the specified Grammar's standard indention
	 * @param	receiver	the TokenReceiver to store the code in, one token per line
	 * @param	grammar		the Grammar according to which the code is to be generated 
	 */
	public void treeToCode(TokenReceiver receiver, Grammar grammar) throws IOException {
		Grammar g = ((grammar != null) ? grammar : new StandardGrammar());
		this.treeToCode(receiver, g.getStandardIndent(), 0, g);
	}
	/**	convert this node and it's subtree to code
	 * @param	receiver	the TokenReceiver to store the code in, one token per line
	 * @param 	indent		the (whitespace) sequence to be inserted at the beginning of a line once per tree level for indention
	 * @param	grammar		the Grammar according to which the code is to be generated 
	 */
	public void treeToCode(TokenReceiver receiver, String indent, Grammar grammar) throws IOException {
		this.treeToCode(receiver, indent, 0, ((grammar != null) ? grammar : new StandardGrammar()));
	}
	
	//	main conversion method for recursive code generation out of the tree
	private void treeToCode(TokenReceiver receiver, String ind, int level, Grammar gram) throws IOException {
		
		Grammar grammar = ((gram != null) ? gram : new StandardGrammar());
		
		String indent = "";
		for (int i = 0; i < level; i++) {
			indent = indent + ind;
		}
		
//		//	data or comment node
//		if (this.nodeType.equalsIgnoreCase(DATA_NODE_TYPE) || this.nodeType.equalsIgnoreCase(COMMENT_NODE_TYPE) || this.nodeType.equalsIgnoreCase(DTD_NODE_TYPE) || this.nodeType.equalsIgnoreCase(PROCESSING_INSTRUCTION_NODE_TYPE))
//			receiver.storeToken(this.nodeValue, level);
//			
		//	comment, DTD, etc node
		if (this.nodeType.equalsIgnoreCase(COMMENT_NODE_TYPE) || this.nodeType.equalsIgnoreCase(DTD_NODE_TYPE) || this.nodeType.equalsIgnoreCase(PROCESSING_INSTRUCTION_NODE_TYPE))
			receiver.storeToken(this.nodeValue, level);
			
		//	datanode
		else if (this.nodeType.equalsIgnoreCase(DATA_NODE_TYPE))
			receiver.storeToken(grammar.escape(this.nodeValue), level);
			
		//	singular data node as only child 
		else if ((this.childNodes.size() == 1) && ((TreeNode) this.childNodes.get(0)).getNodeType().equalsIgnoreCase(DATA_NODE_TYPE))
			receiver.storeToken((indent + this.getStartTag(grammar) + grammar.escape(((TreeNode) this.childNodes.get(0)).getNodeValue()) + this.getEndTag(grammar)), level);
			
		//	no child nodes
		else if (this.childNodes.size() == 0)
			receiver.storeToken((indent + this.getSingularTag(grammar)), level);
			
		//	otherwise
		else {
			
			//	write start tag
			if (!this.nodeType.equals(ROOT_NODE_TYPE)) receiver.storeToken((indent + this.getStartTag(grammar)), level);
			
			//	iterate through children
			for (int i = 0; i < this.childNodes.size(); i++) {
				if (this.childNodes.get(i) != null)
					((TreeNode) this.childNodes.get(i)).treeToCode(receiver, ind, (this.nodeType.equals(ROOT_NODE_TYPE) ? level : (level + 1)), grammar);
			}
			
			//	write end tag
			if (!this.nodeType.equals(ROOT_NODE_TYPE)) receiver.storeToken((indent + this.getEndTag(grammar)), level);
		}
	}
	
	/**	@return	a start tag for this node according to the specified Grammar, usually <'type + attributes'>
	 */
	protected String getStartTag(Grammar grammar) {
//		String attributeString = this.getAttributesForTag(grammar.getTagAttributeSeparator(), grammar.getTagAttributeValueSeparator(), grammar.getTagAttributeValueQuoter());
		String attributeString = this.getAttributesForTag(grammar);
		if (attributeString.length() > 0) attributeString = grammar.getTagAttributeSeparator() + attributeString;
		return (grammar.getTagStart() + this.nodeType + attributeString + grammar.getTagEnd());
	}
	
	/**	@return	a singular tag for this node according to the specified Grammar, usually 
	 * 		- <'type + attributes'/> in XML
	 * 		- <'type + attributes'> (e.g. <'BR'>) in HTML
	 * 		- null, if XML style singular tags are not desired by the specified Grammar
	 */
	protected String getSingularTag(Grammar grammar) {
//		String attributeString = this.getAttributesForTag(grammar.getTagAttributeSeparator(), grammar.getTagAttributeValueSeparator(), grammar.getTagAttributeValueQuoter());
		if (grammar.isStrictXML()) {
			String attributeString = this.getAttributesForTag(grammar);
			return (grammar.getTagStart() + this.nodeType + ((attributeString.length() == 0) ? "" : ("" + grammar.getTagAttributeSeparator())) + attributeString + grammar.getEndTagMarker() + "" + grammar.getTagEnd());
		}
		else if (grammar.isSingularTagType(this.nodeType))
			return this.getStartTag(grammar);
		
		else return null;
//			return (this.getStartTag(grammar) + this.getEndTag(grammar));
	}
	
	/**	@return	an end tag for this node according to the specified Grammar, usually </'type'>
	 */
	protected String getEndTag(Grammar grammar) {
		return (grammar.getTagStart() + "" + grammar.getEndTagMarker() + this.nodeType + grammar.getTagEnd());
	}
	
	/**	@return	the names of this node's attributes in an array
	 */
	public String[] getAttributeNames() {
		return this.attributes.getAttributeNames();
	}
	
	/**	@return	this node's attribute / value pairs in an array
	 */
	public String[] getAttributeValuePairs(Grammar grammar) {
		return this.attributes.getAttributeValuePairs(grammar);
	}
	
	/**	@return	this node's attribute / value pairs in an array
	 */
	public String[] getAttributeValuePairs(char attributeValueSeparator, char quoter) {
		return this.attributes.getAttributeValuePairs(attributeValueSeparator, quoter);
	}
	
	/**	@return	this node's attribute / value pairs as a list
	 */
	public String getAttributesForTag(Grammar grammar) {
		return this.attributes.getAttributeValueString(grammar);
	}
	
	/**	@return	this node's attribute / value pairs as a list
	 */
	public String getAttributesForTag(char attributeSeparator, char attributeValueSeparator, char quoter) {
		return this.attributes.getAttributeValueString(attributeSeparator, attributeValueSeparator, quoter);
	}
	
	/**	@return	the number of nodes in this node's subtree
	 */
	public int countNodesInSubtree() {
		int numberOfNodes = 1;
		for (int i = 0; i < this.childNodes.size(); i++) {
			numberOfNodes += ((TreeNode) this.childNodes.get(i)).countNodesInSubtree();
		}
		return numberOfNodes;
	}
	
	/**	@return	the number of nodes in this node's subtree that have the specified type
	 */
	public int countNodesInSubtree(String type) {
		int numberOfNodes = 0;
		if (this.nodeType.equalsIgnoreCase(type)) numberOfNodes++;
		for (int i = 0; i < this.childNodes.size(); i++)
			numberOfNodes += ((TreeNode) this.childNodes.get(i)).countNodesInSubtree(type);
		return numberOfNodes;
	}
	
	/**	@return	the number of leaf nodes in this node's subtree 
	 */
	public int countLeafNodesInSubtree() {
		if (this.childNodes.size() == 0) {
			return 1;
		} else {
			int numberOfLeafs = 0;
			for (int i = 0; i < this.childNodes.size(); i++) {
				numberOfLeafs += ((TreeNode) this.childNodes.get(i)).countLeafNodesInSubtree();
			}
			return numberOfLeafs;
		}
	}
	
	/**	@return	the tree depth of this node, i.e. the number of steps up to the root
	 */
	public int getTreeDepth() {
		int depth = -1;
		TreeNode tn = this;
		while ((tn != null) && !tn.getNodeType().equals(TreeNode.ROOT_NODE_TYPE)) {
			tn = tn.getParent();
			depth ++;
		}
		return depth;
	}

	/**	@return	the mark status of this node
	 */
	public boolean isMarked() {
		return this.marked;
	}
	
	/**	mark this node
	 */
	public void markNode() {
		this.marked = true;
	}
	
	/**	unmark this node
	 */
	public void unmarkNode() {
		this.marked = false;
	}
	
	/**	mark this node and all nodes in its subtree
	 */
	public void markSubtree() {
		this.marked = true;
		for (int i = 0; i < this.childNodes.size(); i++) {
			((TreeNode) this.childNodes.get(i)).markSubtree();
		}
	}
	
	/**	unmark this node and all nodes in its subtree
	 */
	public void unmarkSubtree() {
		this.marked = false;
		for (int i = 0; i < this.childNodes.size(); i++) {
			((TreeNode) this.childNodes.get(i)).unmarkSubtree();
		}
	}
	
	/**	@return	this TreeNode's position in the document order of the tree it belongs to, or -1 if the document order position has not been set 
	 */
	public int getDocumentOrderPosition() {
		return this.documentOrderPosition;
	}
	
	/**	make this TreeNode know its position in the document order of the tree it belongs to
	 * @param	newPosition		the number to set the position to
	 * @return the number the TreeNode's position was previously set to
	 */
	public int setDocumentOrderPosition(int newPosition) {
		int temp = this.documentOrderPosition;
		this.documentOrderPosition = newPosition;
		return temp;
	}
	
	/**	compute the document order position for this TreeNode and all TreeNodes in its subtree
	 * @param	firstPosition	the position number to start at (will be assigned to this TreeNode)
	 * @return the last position number that was assigned
	 */
	public int computeDocumentOrderPosition(int firstPosition) {
		this.documentOrderPosition = firstPosition;
		int pos = firstPosition;
		for (int c = 0; c < this.childNodes.size(); c++) {
			TreeNode child = this.getChildNode(c);
			pos++;
			pos = child.computeDocumentOrderPosition(pos);
		}
		return pos;
	}
	
	/**	clear all references in the subtree of this TreeNode for faster garbage collection
	 */
	public void deleteSubtree() {
		if (this.childNodes != null) {
			for (int i = 0; i < this.childNodes.size(); i++)
				this.getChildNode(i).deleteSubtree();
		}
		this.parentNode = null;
		if (this.attributes != null) this.attributes.clear();
		this.attributes = null;
		if (this.childNodes != null) this.childNodes.clear();
		this.childNodes = null;
	}
	
	/**	@see	java.lang.Object#toString()
	 */
	public String toString() {
		if (this.nodeType.equalsIgnoreCase(DATA_NODE_TYPE))
			return this.nodeValue;
		else return this.nodeType + ": " + this.getAttributesForTag(' ', '=', '"');
	}
	
	/**	@see	java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return (((o != null) && (o instanceof TreeNode)) ? this.equals((TreeNode) o) : super.equals(o));
	}
	
	/**	@see	java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(TreeNode node) {
		if (node == null) return false;
		return (node.toString().equals(this.toString()) && (this.documentOrderPosition == node.documentOrderPosition));
	}
}
