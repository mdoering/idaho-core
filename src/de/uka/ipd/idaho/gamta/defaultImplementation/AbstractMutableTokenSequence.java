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

import de.uka.ipd.idaho.gamta.MutableCharSequence;
import de.uka.ipd.idaho.gamta.MutableTokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.TokenSequenceListener;
import de.uka.ipd.idaho.gamta.Tokenizer;

/**
 * Abstract implementation of a mutable token sequence overlay for some mutable
 * char sequence, encapsulating the latter and implementing the handling and
 * notification of TokenSequenceListener objects. This implementation also
 * listens to changes to the underaying char sequence and casts them into
 * notifications on changes to the token sequence if necessary. The setValueAt()
 * method implements smart insertion, i.e. inserts multiple tokens instead of
 * one if the value inserted tokenized to other than one token.
 * 
 * @author sautter
 */
public abstract class AbstractMutableTokenSequence implements MutableTokenSequence {
	
	protected final MutableCharSequence charData;
	protected final Tokenizer tokenizer; 
	
	private ArrayList listeners = new ArrayList();
	
	/** Constructor
	 * @param	charData	
	 */
	protected AbstractMutableTokenSequence(MutableCharSequence charData, Tokenizer tokenizer) {
		this.charData = charData;
		this.tokenizer = tokenizer;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#addTokenSequenceListener(de.gamta.TokenSequenceListener)
	 */
	public void addTokenSequenceListener(TokenSequenceListener tsl) {
		this.listeners.add(tsl);
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableTokenSequence#removeTokenSequenceListener(de.gamta.TokenSequenceListener)
	 */
	public void removeTokenSequenceListener(TokenSequenceListener tsl) {
		this.listeners.remove(tsl);
	}
	
	
	/**	notify this listener that a portion of this MutableTokenSequence has been changed
	 * @param	index		the start position of the change
	 * @param	inserted	the sequence of tokens that was inserted
	 * @param	removed		the sequence of tokens that was removed
	 * @param	cause		the CharSequenceEvent that incured the change to the token overlay
	 */
	protected void notifyTokenSequenceChanged(int index, TokenSequence inserted, TokenSequence removed, CharSequenceEvent cause) {
		this.notifyTokenSequenceChanged(new TokenSequenceEvent(this, index, inserted, removed, cause));
	}
	
	/**	notify this listener that a portion of this MutableTokenSequence has been changed
	 * @param	tse		the TokenSequenceEvent holding the details of the cange
	 */
	protected void notifyTokenSequenceChanged(TokenSequenceEvent tse) {
		for (int l = 0; l < this.listeners.size(); l++)
			((TokenSequenceListener) this.listeners.get(l)).tokenSequenceChanged(tse);
	}
}
