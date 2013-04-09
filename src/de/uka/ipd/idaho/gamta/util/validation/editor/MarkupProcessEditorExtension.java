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

import java.util.ArrayList;

/**
 * Factory for extension panels to process part editors
 * 
 * @author sautter
 */
public abstract class MarkupProcessEditorExtension {
	
	private ArrayList instances = new ArrayList();
	
	/**
	 * Retrieve an extension for the editor of a specific part of a specific
	 * markup process. This method first fetches an extension panel for the
	 * argument markup process part editor from the produceExtension() method.
	 * Then, if the latter is not, null, it stores the extension panel for later
	 * access, and finally returns it.
	 * @param ppe the editor for the markup process part
	 * @return an extension panel for the argument editor, or null, if there is
	 *         none
	 */
	public MarkupProcessPartEditorExtension getExtension(MarkupProcessPartEditor ppe) {
		MarkupProcessPartEditorExtension ppee = this.produceExtension(ppe);
		if (ppee != null)
			this.instances.add(ppee);
		return ppee;
	}
	
	/**
	 * Retrieve the extension panels issued by the process editor extension. The
	 * elements of the array are in the order they were retrieved from the
	 * getExtension() method.
	 * @return an array holding the extension panels issued by the process
	 *         editor extension
	 */
	public MarkupProcessPartEditorExtension[] getInstances() {
		return ((MarkupProcessPartEditorExtension[]) this.instances.toArray(new MarkupProcessPartEditorExtension[this.instances.size()]));
	}
	
	/**
	 * Produce an extension panel for the editor of a specific part of a
	 * specific markup process. The getExtension() method redirects here.
	 * @param ppe the editor for the markup process part
	 * @return an extension panel for the argument editor, or null, if this
	 *         class does not have an extension for the argument editor
	 */
	protected abstract MarkupProcessPartEditorExtension produceExtension(MarkupProcessPartEditor ppe);
}
