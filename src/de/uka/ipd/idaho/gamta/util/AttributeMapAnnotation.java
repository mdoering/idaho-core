/**
 * 
 */
package de.uka.ipd.idaho.gamta.util;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import de.uka.ipd.idaho.easyIO.util.RandomByteSource;
import de.uka.ipd.idaho.gamta.AnnotationUtils;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.TokenSequence;
import de.uka.ipd.idaho.gamta.Tokenizer;
import de.uka.ipd.idaho.gamta.defaultImplementation.AbstractAttributed;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * This class is a simple wrapper that allows key / value mappings to act as a
 * queriable annotation, optionally including an annotation value. If the value
 * token sequence handed to any of the constructors is null, it will simply be
 * 'DUMMY' using the default tokenizer. When any of the String / Tokenizer
 * constructors is used, the contained token sequence is generated only when
 * first accessed. If the value String is null, it defaults to 'DUMMY', if the
 * tokenizer is null, the default tokenizer is used.
 * 
 * @author sautter
 */
public class AttributeMapAnnotation extends AbstractAttributed implements QueriableAnnotation {
	private static QueriableAnnotation[] qaDummy = new QueriableAnnotation[0];
	private static String[] sDummy = new String[0];
	
	private String type;
	private String id;
	
	private CharSequence charData = "DUMMY";
	private Tokenizer tokenizer = Gamta.INNER_PUNCTUATION_TOKENIZER;
	private TokenSequence tokens;
	
	/** Constructor
	 * @param type the annotation type
	 * @param attributes the map holding the attributes for the annotation
	 * @param charData the value for the annotation
	 * @param tokenizer the tokenizer to tokenize the char data with for token based operations
	 */
	public AttributeMapAnnotation(String type, Map attributes, CharSequence charData, Tokenizer tokenizer) {
		this.type = type;
		if (charData != null)
			this.charData = charData;
		if (tokenizer != null)
			this.tokenizer = tokenizer;
		for (Iterator kit = attributes.keySet().iterator(); kit.hasNext();) {
			Object key = kit.next();
			if (key instanceof String) {
				Object value = attributes.get(key);
				if (value != null)
					this.setAttribute(((String) key), value);
			}
		}
	}
	
	/** Constructor
	 * @param type the annotation type
	 * @param attributes the hashtable holding the attributes for the annotation
	 * @param charData the value for the annotation
	 * @param tokenizer the tokenizer to tokenize the char data with for token based operations
	 */
	public AttributeMapAnnotation(String type, Hashtable attributes, CharSequence charData, Tokenizer tokenizer) {
		this.type = type;
		if (charData != null)
			this.charData = charData;
		if (tokenizer != null)
			this.tokenizer = tokenizer;
		for (Iterator kit = attributes.keySet().iterator(); kit.hasNext();) {
			Object key = kit.next();
			if (key instanceof String) {
				Object value = attributes.get(key);
				if (value != null)
					this.setAttribute(((String) key), value);
			}
		}
	}
	
	/** Constructor
	 * @param type the annotation type
	 * @param attributes the string tupel holding the attributes for the annotation
	 * @param charData the value for the annotation
	 * @param tokenizer the tokenizer to tokenize the char data with for token based operations
	 */
	public AttributeMapAnnotation(String type, StringTupel attributes, CharSequence charData, Tokenizer tokenizer) {
		this(type, attributes, attributes.getKeys().toStringArray(), charData, tokenizer);
	}

	/** Constructor
	 * @param type the annotation type
	 * @param attributes the string tupel holding the attributes for the annotation
	 * @param keys the keys to read the attributes
	 * @param charData the value for the annotation
	 * @param tokenizer the tokenizer to tokenize the char data with for token based operations
	 */
	public AttributeMapAnnotation(String type, StringTupel attributes, String[] keys, CharSequence charData, Tokenizer tokenizer) {
		this.type = type;
		if (charData != null)
			this.charData = charData;
		if (tokenizer != null)
			this.tokenizer = tokenizer;
		for (int k = 0; k < keys.length; k++) {
			String value = attributes.getValue(keys[k]);
			if (value != null)
				this.setAttribute(keys[k], value);
		}
	}

	/** Constructor
	 * @param type the annotation type
	 * @param attributes the map holding the attributes for the annotation
	 * @param tokens the token sequence wrapped in the annotation
	 */
	public AttributeMapAnnotation(String type, Map attributes, TokenSequence tokens) {
		this.type = type;
		if (tokens != null) {
			this.tokens = tokens;
			this.tokenizer = this.tokens.getTokenizer();
		}
		for (Iterator kit = attributes.keySet().iterator(); kit.hasNext();) {
			Object key = kit.next();
			if (key instanceof String) {
				Object value = attributes.get(key);
				if (value != null)
					this.setAttribute(((String) key), value);
			}
		}
	}
	
	/** Constructor
	 * @param type the annotation type
	 * @param attributes the hashtable holding the attributes for the annotation
	 * @param tokens the token sequence wrapped in the annotation
	 */
	public AttributeMapAnnotation(String type, Hashtable attributes, TokenSequence tokens) {
		this.type = type;
		if (tokens != null) {
			this.tokens = tokens;
			this.tokenizer = this.tokens.getTokenizer();
		}
		for (Iterator kit = attributes.keySet().iterator(); kit.hasNext();) {
			Object key = kit.next();
			if (key instanceof String) {
				Object value = attributes.get(key);
				if (value != null)
					this.setAttribute(((String) key), value);
			}
		}
	}
	
	/** Constructor
	 * @param type the annotation type
	 * @param attributes the string tupel holding the attributes for the annotation
	 * @param tokens the token sequence wrapped in the annotation
	 */
	public AttributeMapAnnotation(String type, StringTupel attributes, TokenSequence tokens) {
		this(type, attributes, attributes.getKeys().toStringArray(), tokens);
	}
	
	/** Constructor
	 * @param type the annotation type
	 * @param attributes the string tupel holding the attributes for the annotation
	 * @param keys the keys to read the attributes
	 * @param tokens the token sequence wrapped in the annotation
	 */
	public AttributeMapAnnotation(String type, StringTupel attributes, String[] keys, TokenSequence tokens) {
		this.type = type;
		if (tokens != null) {
			this.tokens = tokens;
			this.tokenizer = this.tokens.getTokenizer();
		}
		for (int k = 0; k < keys.length; k++) {
			String value = attributes.getValue(keys[k]);
			if (value != null)
				this.setAttribute(keys[k], value);
		}
	}

	private void ensureTokens() {
		if (this.tokens != null)
			return;
		this.tokens = new CharTokenSequence(this.charData, this.tokenizer);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#getStartIndex()
	 */
	public int getStartIndex() {
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#getEndIndex()
	 */
	public int getEndIndex() {
		this.ensureTokens();
		return this.tokens.size();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#getType()
	 */
	public String getType() {
		return this.type;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#changeTypeTo(java.lang.String)
	 */
	public String changeTypeTo(String newType) {
		if (newType == null)
			return this.type;
		String oldType = this.type;
		this.type = newType;
		return oldType;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#getAnnotationID()
	 */
	public String getAnnotationID() {
		if (this.id == null) {
			int[] idParts = {
				((this.tokens == null) ? this.charData : this.tokens).toString().hashCode(),
				AnnotationUtils.produceStartTag(this).hashCode(),
			};
			this.id = new String(RandomByteSource.getHexCode(idParts));
		}
		return this.id;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#getValue()
	 */
	public String getValue() {
		return ((this.tokens == null) ? this.charData : this.tokens).toString();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#toXML()
	 */
	public String toXML() {
		return (AnnotationUtils.produceStartTag(this) + AnnotationUtils.escapeForXml(this.getValue()) + AnnotationUtils.produceEndTag(this));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String)
	 */
	public String getDocumentProperty(String propertyName) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#getDocumentProperty(java.lang.String, java.lang.String)
	 */
	public String getDocumentProperty(String propertyName, String defaultValue) {
		return defaultValue;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.Annotation#getDocumentPropertyNames()
	 */
	public String[] getDocumentPropertyNames() {
		return sDummy;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#tokenAt(int)
	 */
	public Token tokenAt(int index) {
		this.ensureTokens();
		return this.tokens.tokenAt(index);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#firstToken()
	 */
	public Token firstToken() {
		this.ensureTokens();
		return this.tokens.firstToken();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#lastToken()
	 */
	public Token lastToken() {
		this.ensureTokens();
		return this.tokens.lastToken();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#valueAt(int)
	 */
	public String valueAt(int index) {
		this.ensureTokens();
		return this.tokens.valueAt(index);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#firstValue()
	 */
	public String firstValue() {
		this.ensureTokens();
		return this.tokens.firstValue();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#lastValue()
	 */
	public String lastValue() {
		this.ensureTokens();
		return this.tokens.lastValue();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#getLeadingWhitespace()
	 */
	public String getLeadingWhitespace() {
		return "";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.TokenSequence#getWhitespaceAfter(int)
	 */
	public String getWhitespaceAfter(int index) {
		this.ensureTokens();
		return this.tokens.getWhitespaceAfter(index);
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
		return this.tokens.getSubsequence(start, size);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#length()
	 */
	public int length() {
		return ((this.tokens == null) ? this.charData : this.tokens).length();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#charAt(int)
	 */
	public char charAt(int index) {
		return ((this.tokens == null) ? this.charData : this.tokens).charAt(index);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	public CharSequence subSequence(int start, int end) {
		return ((this.tokens == null) ? this.charData : this.tokens).subSequence(start, end);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if (o == this)
			return 0;
		if (o instanceof AttributeMapAnnotation)
			return this.toString().compareTo(((AttributeMapAnnotation) o).toString());
		return -1;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.CharSpan#getStartOffset()
	 */
	public int getStartOffset() {
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.CharSpan#getEndOffset()
	 */
	public int getEndOffset() {
		return this.length();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAbsoluteStartIndex()
	 */
	public int getAbsoluteStartIndex() {
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAbsoluteStartOffset()
	 */
	public int getAbsoluteStartOffset() {
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAnnotations()
	 */
	public QueriableAnnotation[] getAnnotations() {
		return qaDummy;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAnnotations(java.lang.String)
	 */
	public QueriableAnnotation[] getAnnotations(String type) {
		return qaDummy;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAnnotationTypes()
	 */
	public String[] getAnnotationTypes() {
		return sDummy;
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.gamta.QueriableAnnotation#getAnnotationNestingOrder()
	 */
	public String getAnnotationNestingOrder() {
		return this.type;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.getValue();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}