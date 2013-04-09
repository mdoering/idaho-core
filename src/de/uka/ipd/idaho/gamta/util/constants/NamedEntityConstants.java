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
package de.uka.ipd.idaho.gamta.util.constants;


/**
 * This interface provides constants for annotation types and attribute names
 * useful when dealing with named entity extraction or markup.
 * 
 * @author sautter
 */
public interface NamedEntityConstants {
	
	/** the annotation type for marking a date in the text of a document*/
	public static final String DATE_TYPE = "date";
	
	/** the annotation type for marking a time in the text of a document*/
	public static final String TIME_TYPE = "time";
	
	/** the annotation type for marking a generic number in the text of a document*/
	public static final String NUMBER_TYPE = "number";
	
	/** the annotation type for marking a quantity in the text of a document*/
	public static final String QUANTITY_TYPE = "quantity";
	
	/** the annotation type for marking a monetary amount in the text of a document*/
	public static final String MONETARY_AMOUNT_TYPE = "monetaryAmount";
	
	/** the attribute holding the numerical value of a number, quantity, or monetary amount in a respective annotation*/
	public static final String VALUE_ATTRIBUTE = "value";
	
	/** the attribute holding the unit of a quantity or monetary amount in a respective annotation (e.g. 'mm', 'feet', 'sec', or 'USD')*/
	public static final String UNIT_ATTRIBUTE = "unit";
	
	/** the attribute holding the factor of metrically normalized quantity value in a respective annotation, always out of [1,10)*/
	public static final String METRIC_VALUE_ATTRIBUTE = "metricValue";
	
	/** the attribute holding the 10-based exponent of metrically normalized quantity value in a respective annotation*/
	public static final String METRIC_MAGNITUDE_ATTRIBUTE = "metricMagnitude";
	
	/** the attribute holding the metric unit of a quantity in a respective annotation (e.g. 'm', 'sec', or 'USD')*/
	public static final String METRIC_UNIT_ATTRIBUTE = "metricUnit";
	
	
	/** the annotation type for marking up a generic proper name in the text of a document (e.g. for further classification)*/
	public static final String PROPER_NAME_TYPE = "properName";
	
	/** the annotation type for marking up a generic acronym in the text of a document*/
	public static final String ACRONYM_TYPE = "acronym";
	
	/** the annotation type for marking up a person's name in the text of a document*/
	public static final String PERSON_TYPE = "person";
	
	/** the annotation type for marking up a location name in the text of a document*/
	public static final String LOCATION_TYPE = "location";
	
	/** the annotation type for marking up an organization's name in the text of a document*/
	public static final String ORGANIZATION_TYPE = "organization";
	
	/** the annotation type for marking up an official position in the text of a document*/
	public static final String OFFICIAL_POSITION_TYPE = "officialPosition";
	
	/** the attribute holding a normalized form of an arbitray proper name in a respective annotation*/
	public static final String NAME_ATTRIBUTE = "name";
}
