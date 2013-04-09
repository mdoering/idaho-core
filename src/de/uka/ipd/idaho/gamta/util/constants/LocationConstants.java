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
 * This interface provides additional constants for annotation types and
 * attribute names to the ones in NamedEntityConstants, which are useful when
 * dealing in-depth with location named entities or geo-referencing.
 * 
 * @author sautter
 */
public interface LocationConstants extends NamedEntityConstants {
	
	/** the attribute holding the numerical longitude of a location in a respective annotation*/
	public static final String LONGITUDE_ATTRIBUTE = "longitude";
	
	/** the attribute holding the numerical latitude of a location in a respective annotation*/
	public static final String LATITUDE_ATTRIBUTE = "latitude";
	
	/** the attribute holding the precision (in meters) of a pair of geographical coordinates assigned to some annotation as longitude and latitude*/
	public static final String LONG_LAT_PRECISION_ATTRIBUTE = "longLatPrecision";
	
	/** the attribute holding the numerical elevation of a location in a respective annotation*/
	public static final String ELEVATION_ATTRIBUTE = "elevation";
	
	/** the attribute holding the continent or ocean a location lies in*/
	public static final String CONTINENT_OCEAN_ATTRIBUTE = "continentOcean";
	
	/** the attribute holding the country a location lies in*/
	public static final String COUNTRY_ATTRIBUTE = "country";
	
	/** the attribute holding the state or province a location lies in*/
	public static final String STATE_PROVINCE_ATTRIBUTE = "stateProvince";
	
	/** the attribute holding the county a location lies in*/
	public static final String COUNTY_ATTRIBUTE = "county";
	
	/** the attribute holding the numerical population of a location in a respective annotation*/
	public static final String POPULATION_ATTRIBUTE = "population";
	
	/** the attribute holding the type of a location in a respective annotation*/
	public static final String LOCATION_TYPE_ATTRIBUTE = "type";
	
	
	/** the annotation type for marking up a geographical coordinate in the text of a document*/
	public static final String GEO_COORDINATE_TYPE = "geoCoordinate";
	
	/** the attribute holding the precision (in meters) of a single geographical coordinate in a respective annotation*/
	public static final String PRECISION_ATTRIBUTE = "precision";
	
	/** the attribute holding the orientation ('longitude' or 'latitude') of a geographical coordinate in a respective annotation*/
	public static final String ORIENTATION_ATTRIBUTE = "orientation";
	
	/** the value of the orientation attribute indicating a longitudinal (north-south) orientation of a geographical coordinate*/
	public static final String LONGITUDE_ORIENTATION = "longitude";
	
	/** the value of the orientation attribute indicating a latitudinal (east-west) orientation of a geographical coordinate*/
	public static final String LATITUDE_ORIENTATION = "latitude";
	
	/** the attribute holding the direction ('north', 'south', 'east' or 'west') of a geographical coordinate in a respective annotation*/
	public static final String DIRECTION_ATTRIBUTE = "direction";
	
	/** the value of the direction attribute indicating a north direction of a geographical coordinate*/
	public static final String NORTH_DIRECTION = "north";
	
	/** the value of the direction attribute indicating a south direction of a geographical coordinate*/
	public static final String SOUTH_DIRECTION = "south";
	
	/** the value of the direction attribute indicating a east direction of a geographical coordinate*/
	public static final String EAST_DIRECTION = "east";
	
	/** the value of the direction attribute indicating a west direction of a geographical coordinate*/
	public static final String WEST_DIRECTION = "west";
}
