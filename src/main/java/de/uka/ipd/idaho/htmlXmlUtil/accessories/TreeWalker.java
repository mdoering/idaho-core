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


import java.io.IOException;

import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Visitor for traversing a given tree of TreeNode objects. This is useful if
 * more than one traversal is in progress at the same time.
 * 
 * @author sautter
 */
public class TreeWalker {
	
	protected Grammar grammar;

	/**
	 * Constructor
	 */
	public TreeWalker() {
		this(null);
	}

	/**
	 * Constructor
	 * @param grammar the Grammar to use for tag production
	 */
	public TreeWalker(Grammar grammar) {
		this.grammar = ((grammar == null) ? new StandardGrammar() : grammar);
	}

	/**
	 * walk through the tree under a root node
	 * @param root the node the subtree of which to walk
	 * @return the result of the walk as a String
	 */
	public final String walkTree(TreeNode root) {
		return this.walkTree(root, 0);
	}

	/**
	 * Walk through the tree under a root node.
	 * @param node the node the subtree of which to walk
	 * @param treeDepth the actual depth in the tree
	 * @return the result of the walk as a String
	 */
	public final String walkTree(TreeNode node, int treeDepth) {
		String indent = "";
		for (int i = 0; i < treeDepth; i++)
			indent = indent + this.grammar.getStandardIndent();
		TreeNode[] children = node.getChildNodes();

		if (children.length == 0) return (indent + this.produceSingularTag(node));

		StringBuffer assembler = new StringBuffer(indent + this.produceStartTag(node) + "\n");
		for (int c = 0; c < children.length; c++)
			assembler.append(this.getContentForNode(children[c], (treeDepth + 1)));
		assembler.append(indent + this.produceEndTag(node) + "\n");
		return assembler.toString();
	}

	/**
	 * Produce the content for a node. Note: This method recurses to walkTree()
	 * by default, so that the TreeWalker recursively traverses the tree in
	 * depth firth order. Subclasses may override this method as needed, e.g. to
	 * transform nodes or entire subtrees.
	 * @param node the node the content of which to produce
	 * @param treeDepth the actual depth in the tree
	 * @return the content for the specified node
	 */
	public String getContentForNode(TreeNode node, int treeDepth) {
		if (TreeNode.DATA_NODE_TYPE.equals(node.getNodeType()) || TreeNode.COMMENT_NODE_TYPE.equals(node.getNodeType())) {
			String indent = "";
			for (int i = 0; i < treeDepth; i++)
				indent = indent + this.grammar.getStandardIndent();
			return (indent + node.getNodeValue() + "\n");
		}
		return this.walkTree(node, treeDepth);
	}

	/**
	 * Walk through the tree under a root node and write the output gathered to
	 * a TokenReceiver.
	 * @param root the node the subtree of which to walk
	 * @param target the TokenReceiver to write to
	 */
	public final void walkTree(TreeNode root, TokenReceiver target) throws IOException {
		this.walkTree(root, 0, target);
	}

	/**
	 * Walk through the tree under a root node and write the output gathered to
	 * a TokenReceiver.
	 * @param node the node the subtree of which to walk
	 * @param treeDepth the actual depth in the tree
	 * @param target the TokenReceiver to write to
	 */
	public final void walkTree(TreeNode node, int treeDepth, TokenReceiver target) throws IOException {
		String indent = "";
		for (int i = 0; i < treeDepth; i++)
			indent = indent + this.grammar.getStandardIndent();
		TreeNode[] children = node.getChildNodes();

		if (children.length == 0) target.storeToken((indent + this.produceSingularTag(node)), treeDepth);

		else {
			target.storeToken((indent + this.produceStartTag(node)), treeDepth);
			for (int c = 0; c < children.length; c++)
				this.writeContentForNode(children[c], (treeDepth + 1), target);
			target.storeToken((indent + this.produceEndTag(node)), treeDepth);
		}
	}

	/**
	 * Produce the content for a node and write it to a TokenReceiver. Note:
	 * This method recurses to walkTree() by default, so that the TreeWalker
	 * recursively traverses the tree in depth firth order. Subclasses may
	 * override this method as needed, e.g. to transform nodes or entire
	 * subtrees.
	 * @param node the node the content of which to produce
	 * @param treeDepth the actual depth in the tree
	 * @param target the TokenReceiver to write to
	 */
	public void writeContentForNode(TreeNode node, int treeDepth, TokenReceiver target) throws IOException {
		if (TreeNode.DATA_NODE_TYPE.equals(node.getNodeType()) || TreeNode.COMMENT_NODE_TYPE.equals(node.getNodeType())) {
			String indent = "";
			for (int i = 0; i < treeDepth; i++)
				indent = indent + this.grammar.getStandardIndent();
			target.storeToken((indent + node.getNodeValue()), treeDepth);
		} else this.walkTree(node, treeDepth, target);
	}

	/**
	 * Produce a start tag for a node.
	 * @param node the node to produce a start tag for
	 * @return a start tag for the specified node
	 */
	public String produceSingularTag(TreeNode node) {
		String attributeString = this.produceAttributesForTag(node);
		if (attributeString.length() > 0) attributeString = this.grammar.getTagAttributeSeparator() + attributeString;
		if (grammar.isStrictXML()) return (grammar.getTagStart() + this.translateTagType(node.getNodeType()) + attributeString + this.grammar.getEndTagMarker() + "" + this.grammar.getTagEnd() + "\n");
		else if (grammar.isSingularTagType(node.getNodeType())) return this.produceStartTag(node);
		else return (this.produceStartTag(node) + this.produceEndTag(node));
	}

	/**
	 * Produce a start tag for a node.
	 * @param node the node to produce a start tag for
	 * @return a start tag for the specified node
	 */
	public String produceStartTag(TreeNode node) {
		String attributeString = this.produceAttributesForTag(node);
		if (attributeString.length() > 0) attributeString = this.grammar.getTagAttributeSeparator() + attributeString;
		return (grammar.getTagStart() + this.translateTagType(node.getNodeType()) + attributeString + this.grammar.getTagEnd());
	}

	/**
	 * Produce an end tag for a node.
	 * @param node the node to produce an end tag for
	 * @return an end tag for the specified node
	 */
	public String produceEndTag(TreeNode node) {
		return (grammar.getTagStart() + "" + this.grammar.getEndTagMarker() + this.translateTagType(node.getNodeType()) + this.grammar.getTagEnd());
	}

	/**
	 * Produce a String containing the attribute / value pairs of a node. This
	 * default implementation escapes attribute values according to XML syntax
	 * rules. Sub classes overwriting this method are responsible for output
	 * wellformedness.
	 * @param node the node whose attributes to encode
	 * @return a String containing the attribute / value pairs of the specified
	 *         node
	 */
	public String produceAttributesForTag(TreeNode node) {
		String[] attributes = node.getAttributeNames();
		StringBuffer assembler = new StringBuffer();
		for (int i = 0; i < attributes.length; i++) {
			if (i != 0) assembler.append(this.grammar.getTagAttributeSeparator());
//			assembler.append(
//						attributes[i] + 
//						this.grammar.getTagAttributeValueSeparator() +
//						this.grammar.getTagAttributeValueQuoter() +
//						node.getAttribute(attributes[i], attributes[i]) +
//						this.grammar.getTagAttributeValueQuoter()
//					);
			assembler.append(
					attributes[i] + 
					this.grammar.getTagAttributeValueSeparator() +
					this.grammar.getTagAttributeValueQuoter() +
					this.grammar.escape(node.getAttribute(attributes[i], attributes[i])) +
					this.grammar.getTagAttributeValueQuoter()
				);
		} 
		return assembler.toString();
	}

	/**
	 * Translate a tag. Note: This method returns the specified tag by default
	 * Subclasses may override this method as needed in order to translate tags.
	 * @param tagType the tag to be translated
	 * @return the translation of the specified tag
	 */
	public String translateTagType(String tagType) {
		return tagType;
	}
}
