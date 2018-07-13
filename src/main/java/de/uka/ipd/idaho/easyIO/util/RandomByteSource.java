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
package de.uka.ipd.idaho.easyIO.util;


/**
 * Generator for sequences of random bits. Whatever the requested output format,
 * this class generates random bit sequences in blocks of 16 bits internally,
 * using an invocation of Math.random() and a subsequent multiplication times
 * 65536.
 * 
 * @author sautter
 */
public class RandomByteSource {
	
	/**
	 * @return a new 128 bit SQL style GUID in it's hex representation as a
	 *         String
	 */
	public static String getGUID() {
		return getID(128);
	}

	/**
	 * @return a new ID of bitSize (rounded up to the next multiple of 16) bits
	 *         in it's hex representation (with a leading '0x') as a String
	 */
	public static String getID(int bitSize) {
		StringBuffer id = new StringBuffer();
		char[] chars = charsToHex(getRandomBits(bitSize));
		for (int i = 0; i < chars.length; i++) {
			id.append(chars[i]);
		}
		return ("0x" + id.toString());
	}

	/**
	 * @return bitSize (rounded up to the next multiple of 16) bits pack in
	 *         blocks of 8 in a byte array
	 */
	public static byte[] getRandomBytes(int bitSize) {
		char[] chars = getRandomBits(bitSize);
		byte[] ret = new byte[chars.length * 2];
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			ret[(2*i)+1] = ((byte) (c & 255));
			c >>= 8;
			ret[2*i] = ((byte) (c & 255));
		}
		return ret;
	}
	
	/**
	 * @return bitSize (rounded up to the next multiple of 16) bits pack in
	 *         blocks of 16 in a char array
	 */
	public static char[] getRandomChars(int bitSize) {
		return getRandomBits(bitSize);
	}

	/**
	 * @return bitSize (rounded up to the next multiple of 32) bits pack in
	 *         blocks of 32 in an int array
	 */
	public static int[] getRandomInts(int bitSize) {
		int size = bitSize;
		while ((size & 31) > 0) size++;	//	rise size to the next multiple of 32
		char[] chars = getRandomBits(size);
		int[] ret = new int[chars.length / 2];
		for (int i = 0; i < chars.length; i+=2) {
			int t = ((int) chars[i+1]);
			t <<= 16;
			t |= chars[i];
			ret[i/2] = t;
		}
		return ret;
	}
	
	/**
	 * @return bitSize (rounded up to the next multiple of 64) bits pack in
	 *         blocks of 64 in a long array
	 */
	public static long[] getRandomLongs(int bitSize) {
		int size = bitSize;
		while ((size & 63) > 0)
			size++; // rise size to the next multiple of 64
		char[] chars = getRandomBits(size);
		long[] ret = new long[chars.length / 4];
		for (int i = 0; i < chars.length; i += 4) {
			long l = ((int) chars[i + 3]);
			for (int j = 0; j < 3; j++) {
				l <<= 16;
				l |= chars[i + (2 - j)];
			}
			ret[i / 4] = l;
		}
		return ret;
	}

	/**
	 * @return the hex representation of the bytes in the specified array, each
	 *         char representing 4 bits
	 */
	public static char[] getHexCode(byte[] bytes) {
		int size = bytes.length;
		int stuff = 0;
		if ((size & 1) > 0) stuff = -1;
		char[] temp = new char[(size + 1) / 2];
		for (int i = stuff; i < size; i+=2) {
			char c = ((i == -1) ? ((char) 0) : ((char) bytes[i]));
			c <<= 8;
			c |= ((char) bytes[i+1]);
			temp[(i-stuff) / 2] = c;
		}
		return charsToHex(temp);
	}
	
	/**
	 * @return the hex representation of the chars in the specified array, each
	 *         char representing 4 bits
	 */
	public static char[] getHexCode(char[] chars) {
		return charsToHex(chars);
	}

	/**
	 * @return the hex representation of the ints in the specified array, each
	 *         char representing 4 bits
	 */
	public static char[] getHexCode(int[] ints) {
		int size = ints.length;
		char[] temp = new char[size * 2];
		for (int i = 0; i < size; i++) {
			int t = ints[i];
			temp[(2 * i) + 1] = ((char) (t & 65535));
			t >>= 16;
			temp[2 * i] = ((char) (t & 65535));
		}
		return charsToHex(temp);
	}

	/**
	 * @return the hex representation of the longs in the specified array, each
	 *         char representing 4 bits
	 */
	public static char[] getHexCode(long[] longs) {
		int size = longs.length;
		char[] temp = new char[size * 4];
		for (int i = 0; i < size; i++) {
			long l = longs[i];
			for (int j = 0; j < 4; j++) {
				temp[(2*i)+(3-j)] = ((char) (l & 65535));
				l >>= 16;
			}
		}
		return charsToHex(temp);
	}
	
	//	convert each 16 bit block (char) in bits into it's four-char hex representation
	private static char[] charsToHex(char[] bits) {
		int size = bits.length;
		char[] ret = new char[size * 4];
		for (int i = 0; i < size; i++) {
			char c = bits[i];
			for (int j = 0; j < 4; j++) {
				byte b = ((byte) (c & 15));
				ret[((4*i) + (3-j))] = ((b < 10) ? ((char) (b + '0')) : ((char) (b + '7'))); //	'7' = 'A' - 10
				c >>= 4;
			}
		}
		return ret;
	}
	
	//	generate bitSize (a multiple of 16) random bits, packed in a char array
	private static char[] getRandomBits(int bitSize) {
		int size = bitSize;
		while ((size & 15) > 0) size++;	//	rise size to the next multiple of 16
		size >>= 4;
		char[] ret = new char[size];
		for (int i = 0; i < size; i++) {
			double r = Math.random();
			ret[i] = (char) (r * 65536);
		}
		return ret;
	}
}
