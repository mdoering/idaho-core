/**
 * 
 */
package de.uka.ipd.idaho.gamta.util;

import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed;

/**
 * This class is a simple wrapper that allows a char sequence to act as a token
 * sequence. The tokens form a simple overlay over the wrapped char sequence. If
 * the char sequence is modified after being passed to the constructor, this
 * does not reflect in the tokenization and may result in undesired behavior.<br>
 * The <code>toString()</code> method loops through to the wrapped char
 * sequence. The same applies to the tokens returned by any of the respective
 * methods.
 * 
 * @author sautter
 */
public class CharTokenSequence implements TokenSequence {
	private CharSequence charData;
	private Tokenizer tokenizer;
	private ArrayList tokens = null;
	
	private class CtsToken extends AbstractAttributed implements Token {
		int startOffset;
		int endOffset;
		CtsToken(int startOffset, int endOffset) {
			this.startOffset = startOffset;
			this.endOffset = endOffset;
		}
		public int length() {
			return (this.endOffset - this.startOffset);
		}
		public char charAt(int index) {
			return charData.charAt(this.startOffset + index);
		}
		public CharSequence subSequence(int start, int end) {
			return charData.subSequence((this.startOffset + start), (this.startOffset + end));
		}
		public int getStartOffset() {
			return this.startOffset;
		}
		public int getEndOffset() {
			return this.endOffset;
		}
		public String getValue() {
			return charData.subSequence(this.startOffset, this.endOffset).toString();
		}
		public String toString() {
			return this.getValue();
		}
		public Tokenizer getTokenizer() {
			return tokenizer;
		}
	}
	
	/**
	 * Constructor
	 * @param data the char sequence to wrap
	 * @param tokenizer the tokenizer to use for generating the token overlay
	 */
	public CharTokenSequence(CharSequence data, Tokenizer tokenizer) {
		this.charData = data;
		this.tokenizer = tokenizer;
	}
	
	private void ensureTokens() {
		if (this.tokens != null)
			return;
		this.tokens = new ArrayList();
		for (Tokenizer.TokenIterator ti = this.tokenizer.getTokenIterator(this.charData); ti.hasMoreTokens();) {
			Tokenizer.CharSequenceToken cst = ti.getNextToken();
			this.tokens.add(new CtsToken(cst.startOffset, cst.endOffset));
		}
//		System.out.println("Token overlay created");
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return this.charData.length();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		return this.charData.charAt(index);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return this.charData.subSequence(start, end);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#tokenAt(int)
	 */
	public Token tokenAt(int index) {
		this.ensureTokens();
		return ((Token) this.tokens.get(index));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#firstToken()
	 */
	public Token firstToken() {
		this.ensureTokens();
		return ((Token) this.tokens.get(0));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#lastToken()
	 */
	public Token lastToken() {
		this.ensureTokens();
		return ((Token) this.tokens.get(this.tokens.size() - 1));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#valueAt(int)
	 */
	public String valueAt(int index) {
		this.ensureTokens();
		return ((CtsToken) this.tokens.get(index)).getValue();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#firstValue()
	 */
	public String firstValue() {
		this.ensureTokens();
		return ((CtsToken) this.tokens.get(0)).getValue();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#lastValue()
	 */
	public String lastValue() {
		this.ensureTokens();
		return ((CtsToken) this.tokens.get(this.tokens.size()-1)).getValue();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#getLeadingWhitespace()
	 */
	public String getLeadingWhitespace() {
		this.ensureTokens();
		return this.charData.subSequence(0, ((CtsToken) this.tokens.get(0)).startOffset).toString();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#getWhitespaceAfter(int)
	 */
	public String getWhitespaceAfter(int index) {
		this.ensureTokens();
		int nextTokenStart = (((index + 1) == this.tokens.size()) ? this.charData.length() : ((CtsToken) this.tokens.get(index + 1)).startOffset);
		return this.charData.subSequence(((CtsToken) this.tokens.get(index)).endOffset, nextTokenStart).toString();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#size()
	 */
	public int size() {
		this.ensureTokens();
		return this.tokens.size();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#getTokenizer()
	 */
	public Tokenizer getTokenizer() {
		return this.tokenizer;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#getSubsequence(int, int)
	 */
	public TokenSequence getSubsequence(int start, int size) {
		this.ensureTokens();
		return new CharTokenSequence(this.charData.subSequence(((CtsToken) this.tokens.get(start)).startOffset, ((CtsToken) this.tokens.get(start + size - 1)).endOffset), this.tokenizer);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.charData.toString();
	}
//	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		TokenSequence ts = new CharTokenSequence("this is a test", Gamta.INNER_PUNCTUATION_TOKENIZER);
//		System.out.println(ts.length());
//		System.out.println(ts.charAt(2));
//		System.out.println(ts.valueAt(2));
//		System.out.println(ts.getSubsequence(1, 2));
//	}
}