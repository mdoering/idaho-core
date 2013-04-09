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

/**
 * Implementation of a mutable char sequence storing the chars in blocks. The
 * actual behavior is controlled by the maximum block size and the fill factor.
 * The fill factor determines the initial fill level of the last block before
 * appending another char causes opening a new block. A high fill factor will
 * result in better memory usage, but insertions become more likely to cause a
 * block split. If the size of two neighboring blocks drops below (blockSize *
 * fillFactor), the blocks will be merged.
 * 
 * @author sautter
 */
public class BlockCharSequence extends AbstractMutableCharSequence {
	
	/**	the default maximum block size
	 */
	public static final int DEFAULT_BLOCK_SIZE = 1024;
	
	/**	the default block fill factor.  
	 */
	public static final float DEFAULT_BLOCK_FILL_FACTOR = 0.95f;
	
	private final int blockSize;
	private final int blockFillSize;
	private final float fillFactor;
	
	private int length = 0;
	
	private ArrayList blocks = new ArrayList();
	private Block lastBlock = new Block(0);
	
	private class Block {
		int offset = 0;
		StringBuffer data = new StringBuffer();
		Block(int offset) {
			this.offset = offset;
		}
	}
	
	/** Constructor using default values
	 */
	public BlockCharSequence() {
		this(DEFAULT_BLOCK_SIZE, DEFAULT_BLOCK_FILL_FACTOR);
	}
	
	/** Constructor for custom configurations
	 * @param	blockSize
	 * @param	fillFactor
	 */
	public BlockCharSequence(int blockSize, float fillFactor) {
		this.blockSize = blockSize;
		this.fillFactor = fillFactor;
		this.blockFillSize = ((int) Math.min(1, (this.blockSize * this.fillFactor)));
		this.blocks.add(this.lastBlock);
	}
	
	/*
	 * * @see de.gamta.MutableCharSequence#addChar(char)
	 */
	public void addChar(char ch) {
		if ((this.lastBlock.data.length() + 1) > this.blockFillSize) {
			this.lastBlock = new Block(this.length);
			this.blocks.add(this.lastBlock);
		}
		this.lastBlock.data.append(ch);
		this.length++;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#addChars(java.lang.CharSequence)
	 */
	public void addChars(CharSequence chars) {
		if ((this.lastBlock.data.length() + chars.length()) > this.blockFillSize) {
			int offset = 0;
			while (offset < chars.length()) {
				
			}
			this.lastBlock = new Block(this.length);
			this.blocks.add(this.lastBlock);
		} else {
			this.lastBlock.data.append(chars.toString());
			this.length += chars.length();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChar(char, int)
	 */
	public void insertChar(char ch, int offset) {
		// TODO Auto-generated method stub

	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#insertChars(java.lang.CharSequence, int)
	 */
	public void insertChars(CharSequence chars, int offset) {
		// TODO Auto-generated method stub

	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChar(int)
	 */
	public char removeChar(int offset) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#removeChars(int, int)
	 */
	public CharSequence removeChars(int offset, int length) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChar(char, int)
	 */
	public char setChar(char ch, int offset) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#setChars(java.lang.CharSequence, int, int)
	 */
	public CharSequence setChars(CharSequence chars, int offset, int length) {
		// TODO Auto-generated method stub
		return null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return this.length;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see de.gamta.MutableCharSequence#mutableSubSequence(int, int)
	 */
	public MutableCharSequence mutableSubSequence(int start, int end) {
		// TODO Auto-generated method stub
		return null;
	}
}
