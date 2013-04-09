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
package de.uka.ipd.idaho.gamta;


import java.util.Set;
import java.util.regex.Pattern;

/**
 * Static class providing comparison functionality for Attributed objects.
 * 
 * @author sautter
 */
public class AttributeUtils {
	
	private static final String NCNameRegEx = "([a-zA-Z\\_][a-zA-Z0-9\\_\\-\\.]*+)"; // regular expression matching XML NCNames
	private static final String ANameRegEx = "(" + NCNameRegEx + "(\\:" + NCNameRegEx + ")?+)"; // regular expression matching XML QNames
	private static final Pattern ANamePattern = Pattern.compile(ANameRegEx);
	
	/**
	 * check whether an attribute name String is valid
	 * @param name the attribute name String to test
	 * @return true if and only if the specified attribute name is valid (a
	 *         QName in the sense of XML)
	 */
	public static boolean isValidAttributeName(String name) {
		return ANamePattern.matcher(name).matches();
	}
	
//	private static final String AValueRegEx = "[^\\'\\\"\\&\\<\\>\\n\\r\\t]*+";	// regular expression matching admissible XML attribute values
//	private static final String AValueRegEx = "[^\\\"\\&\\<\\n\\r\\t]*+";	// regular expression matching admissible XML attribute values
//	private static final Pattern AValuePattern = Pattern.compile(AValueRegEx);
//	
//	/**
//	 * check whether an attribute value String is valid
//	 * @param value the attribute name String to test
//	 * @return true if and only if the specified attribute value is valid (does
//	 *         not contain any of the characters ', ", &amp;, &lt;, or &gt;, and
//	 *         no whitespace but simple space characters)
//	 */
//	public static boolean isValidAttributeValue(String value) {
//		return AValuePattern.matcher(value).matches();
//	}
	
	/**
	 * attribute copy mode that sets attributes at the target object to the
	 * values from the source object, regardless if they were set with the
	 * target object before. This mode is the default.
	 */
	public static final int SET_ATTRIBUTE_COPY_MODE = 0;
	
	/**
	 * attribute copy mode that sets attributes at the target object to the
	 * values from the source object only if they were not set with the target
	 * object before. This mode does not overwrite existing values at the target
	 * object.
	 */
	public static final int ADD_ATTRIBUTE_COPY_MODE = 1;
	
	/**
	 * attribute copy mode that sets attributes at the target object to the
	 * values from the source object only if they were set with the target
	 * object before. This mode does not add new attributes to the target
	 * object, only changes values of existing ones. 
	 */
	public static final int OVERWRITE_ATTRIBUTE_COPY_MODE = 2;
	
	/**
	 * copy attributes from one Attributed object to another one.
	 * @param source the Attributed object to copy the attributes from
	 * @param target the Attributed object to transfer the attributes to
	 */
	public static void copyAttributes(Attributed source, Attributed target) {
		copyAttributes(source, target, SET_ATTRIBUTE_COPY_MODE);
	}
	
	/**
	 * copy attributes from one Attributed object to another one.
	 * @param source the Attributed object to copy the attributes from
	 * @param target the Attributed object to transfer the attributes to
	 * @param mode the copying mode, one of SET_ATTRIBUTE_COPY_MODE,
	 *            ADD_ATTRIBUTE_COPY_MODE and OVERWRITE_ATTRIBUTE_COPY_MODE
	 */
	public static void copyAttributes(Attributed source, Attributed target, int mode) {
		copyAttributes(source, target, mode, null);
	}
	
	/**
	 * copy attributes from one Attributed object to another one.
	 * @param source the Attributed object to copy the attributes from
	 * @param target the Attributed object to transfer the attributes to
	 * @param filter a Set containing the names of the Attributes to copy
	 */
	public static void copyAttributes(Attributed source, Attributed target, Set filter) {
		copyAttributes(source, target, SET_ATTRIBUTE_COPY_MODE, filter);
	}
	
	/**
	 * copy attributes from one Attributed object to another one.
	 * @param source the Attributed object to copy the attributes from
	 * @param target the Attributed object to transfer the attributes to
	 * @param mode the copying mode, one of SET_ATTRIBUTE_COPY_MODE,
	 *            ADD_ATTRIBUTE_COPY_MODE and OVERWRITE_ATTRIBUTE_COPY_MODE
	 * @param filter a Set containing the names of the Attributes to copy
	 */
	public static void copyAttributes(Attributed source, Attributed target, int mode, Set filter) {
		
		//	get attribute names of source
		String[] attributeNames = source.getAttributeNames();
		
		//	process attributes one by one
		for (int a = 0; a < attributeNames.length; a++) {
			
			//	check filter
			if ((filter == null) || filter.contains(attributeNames[a]))
				
				//	check copy mode
				if (((mode != ADD_ATTRIBUTE_COPY_MODE) && target.hasAttribute(attributeNames[a])) || ((mode != OVERWRITE_ATTRIBUTE_COPY_MODE) && !target.hasAttribute(attributeNames[a])))
					
					//	copy attribute
					target.setAttribute(attributeNames[a], source.getAttribute(attributeNames[a]));
		}
	}
	
	/**
	 * test if some Attributed object has at least the attributes of some
	 * reference object
	 * @param toTest the Attributed object to test
	 * @param reference the reference object
	 * @return true if toTest has at least all the attributes the reference
	 *         object has
	 */
	public static boolean hasAttributesOf(Attributed toTest, Attributed reference) {
		return hasAttributesOf(toTest, reference, null);
	}
	
	/**
	 * test if some Attributed object has at least the attributes of some
	 * reference object
	 * @param toTest the Attributed object to test
	 * @param reference the reference object
	 * @param filter a Set containing the names of the Attributes to test
	 * @return true if toTest has at least all the attributes the reference
	 *         object has
	 */
	public static boolean hasAttributesOf(Attributed toTest, Attributed reference, Set filter) {
		
		//	get attribute names of reference
		String[] referenceAttributeNames = reference.getAttributeNames();
		
		//	process attributes one by one
		for (int a = 0; a < referenceAttributeNames.length; a++) {
			
			//	check filter
			if ((filter == null) || filter.contains(referenceAttributeNames[a]))
				if (!toTest.hasAttribute(referenceAttributeNames[a])) return false;
		}
		
		//	all checks successful
		return true;
	}
	
	/**
	 * test if some Attributed object has at exactly the same attributes as some
	 * reference object
	 * @param toTest the Attributed object to test
	 * @param reference the reference object
	 * @return true if toTest has at exactly the same attributes as reference
	 *         object
	 */
	public static boolean hasSameAttributes(Attributed toTest, Attributed reference) {
		return hasSameAttributes(toTest, reference, null);
	}
	
	/**
	 * test if some Attributed object has at exactly the same attributes as some
	 * reference object
	 * @param toTest the Attributed object to test
	 * @param reference the reference object
	 * @param filter a Set containing the names of the Attributes to test
	 * @return true if toTest has at exactly the same attributes as reference
	 *         object
	 */
	public static boolean hasSameAttributes(Attributed toTest, Attributed reference, Set filter) {
		
		//	get attribute names of reference
		String[] referenceAttributeNames = reference.getAttributeNames();
		
		//	process reference attributes one by one
		for (int a = 0; a < referenceAttributeNames.length; a++) {
			
			//	check filter
			if ((filter == null) || filter.contains(referenceAttributeNames[a]))
				if (!toTest.hasAttribute(referenceAttributeNames[a])) return false;
		}
		
		//	get attribute names of test object
		String[] toTestAttributeNames = toTest.getAttributeNames();
		
		//	process test attributes one by one
		for (int a = 0; a < toTestAttributeNames.length; a++) {
			
			//	check filter
			if ((filter == null) || filter.contains(toTestAttributeNames[a]))
				if (!reference.hasAttribute(toTestAttributeNames[a])) return false;
		}
		
		//	all checks successful
		return true;
	}
	
	/**
	 * test if some Attributed object has at least the attributes of some
	 * reference object, with the same values
	 * @param toTest the Attributed object to test
	 * @param reference the reference object
	 * @return true if toTest has at least all the attributes the reference
	 *         object has, and the attributes have the same values
	 */
	public static boolean hasAttributeValuesOf(Attributed toTest, Attributed reference) {
		return hasAttributeValuesOf(toTest, reference, null);
	}
	
	/**
	 * test if some Attributed object has at least the attributes of some
	 * reference object, with the same values
	 * @param toTest the Attributed object to test
	 * @param reference the reference object
	 * @param filter a Set containing the names of the Attributes to test
	 * @return true if toTest has at least all the attributes the reference
	 *         object has, and the attributes have the same values
	 */
	public static boolean hasAttributeValuesOf(Attributed toTest, Attributed reference, Set filter) {
		
		//	get attribute names of reference
		String[] referenceAttributeNames = reference.getAttributeNames();
		
		//	process attributes one by one
		for (int a = 0; a < referenceAttributeNames.length; a++) {
			
			//	check filter
			if ((filter == null) || filter.contains(referenceAttributeNames[a])) {
				
				//	toTest doesn't have attribute
				if (!toTest.hasAttribute(referenceAttributeNames[a])) return false;
				
				//	attribute is null in reference, check if same is true for toTest
				if (reference.getAttribute(referenceAttributeNames[a]) == null)
					if (toTest.getAttribute(referenceAttributeNames[a]) != null) return false;
				
				//	compare attributes using equals() method
				if (!reference.getAttribute(referenceAttributeNames[a]).equals(toTest.getAttribute(referenceAttributeNames[a]))) return false;
			}
		}
		
		//	all checks successful
		return true;
	}
	
	/**
	 * test if some Attributed object has exactly the same attributes as some
	 * reference object, with the same values
	 * @param toTest the Attributed object to test
	 * @param reference the reference object
	 * @return true if toTest has exactly the same attributes as reference
	 *         object has, and the attributes have the same values
	 */
	public static boolean hasEqualAttributes(Attributed toTest, Attributed reference) {
		return hasEqualAttributes(toTest, reference, null);
	}
	
	/**
	 * test if some Attributed object has exactly the same attributes as some
	 * reference object, with the same values
	 * @param toTest the Attributed object to test
	 * @param reference the reference object
	 * @param filter a Set containing the names of the Attributes to test
	 * @return true if toTest has exactly the same attributes as reference
	 *         object has, and the attributes have the same values
	 */
	public static boolean hasEqualAttributes(Attributed toTest, Attributed reference, Set filter) {
		
		//	get attribute names of reference
		String[] referenceAttributeNames = reference.getAttributeNames();
		
		//	process attributes one by one
		for (int a = 0; a < referenceAttributeNames.length; a++) {
			
			//	check filter
			if ((filter == null) || filter.contains(referenceAttributeNames[a])) {
				
				//	get attribute values
				Object referenceAttribute = reference.getAttribute(referenceAttributeNames[a]);
				Object toTestAttribute = toTest.getAttribute(referenceAttributeNames[a]);
				
				//	both are null, we're OK
				if ((toTestAttribute == null) && (referenceAttribute == null))
					continue;
				
				//	either one is null ==> not equal
				if ((toTestAttribute == null) || (referenceAttribute == null))
					return false;
				
				//	values not equal ==> not equal
				if (!referenceAttribute.equals(toTestAttribute))
					return false;
			}
		}
		
		//	get attribute names of test object
		String[] toTestAttributeNames = toTest.getAttributeNames();
		
		//	process test attributes one by one (containment is sufficient here, since checks for equality have been done above)
		for (int a = 0; a < toTestAttributeNames.length; a++) {
			
			//	check filter
			if ((filter == null) || filter.contains(toTestAttributeNames[a]))
				if (!reference.hasAttribute(toTestAttributeNames[a])) return false;
		}
		
		//	all checks successful
		return true;
	}
}
