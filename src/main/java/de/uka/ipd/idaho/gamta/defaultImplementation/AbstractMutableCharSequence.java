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
package de.uka.ipd.idaho.gamta.defaultImplementation;


import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.CharSequenceListener;
import de.uka.ipd.idaho.gamta.MutableCharSequence;

/**
 * Abstract implementation of a mutable char sequence, implementing the handling
 * and notification of CharSequenceListener objects.
 * 
 * @author sautter
 */
public abstract class AbstractMutableCharSequence implements MutableCharSequence {
	
	private ArrayList listeners = new ArrayList();
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addCharSequenceListener(de.gamta.CharSequenceListener)
	 */
	public void addCharSequenceListener(CharSequenceListener csl) {
		this.listeners.add(csl);
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeCharSequenceListener(de.gamta.CharSequenceListener)
	 */
	public void removeCharSequenceListener(CharSequenceListener csl) {
		this.listeners.remove(csl);
	}
	
	/**	notify this listener that a portion of this MutableCharSequence has been changed
	 * @param	offset		the start position of the change
	 * @param	inserted	the sequence of chars that was inserted
	 * @param	removed		the sequence of chars that was removed
	 */
	protected void notifyCharSequenceChanged(int offset, java.lang.CharSequence inserted, java.lang.CharSequence removed) {
		this.notifyCharSequenceChanged(new CharSequenceEvent(this, offset, inserted, removed));
	}
	
	/**	notify this listener that a portion of this MutableCharSequence has been changed
	 * @param	cse		the CharSequenceEvent holding the details of the cange
	 */
	protected void notifyCharSequenceChanged(CharSequenceEvent cse) {
		for (int l = 0; l < this.listeners.size(); l++)
			((CharSequenceListener) this.listeners.get(l)).charSequenceChanged(cse);
	}
}
