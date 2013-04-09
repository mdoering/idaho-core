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
package de.uka.ipd.idaho.easyIO.sql;


import java.util.Properties;

/**
 * A generic definition of a single coulmn of a table in a relational database.
 * This class is intended for use with instances of TableDefinition.
 * 
 * @author sautter
 */
public class TableColumnDefinition implements Comparable {
	
	protected String columnName;
	protected String dataType;
	protected int length;
	
	protected boolean isValidationDef = false;
	
	/**	Constructor
	 * @param data	the String containing all values for this TableColumnDefinition
	 * The String is has to be formated like this: #'columnName';'dataType';'length'
	 */
	public TableColumnDefinition(String data) {
		if (data.startsWith("#")) {
//			StringVector parser = new StringVector();
//			parser.parseAndAddElements(data.substring(1), ";");
//			this.columnName = parser.get(0);
//			this.dataType = parser.get(1);
//			this.length = Integer.parseInt(parser.get(2));
			String[] parsed = data.substring(1).split("\\s*\\;\\s*");
			if (parsed.length != 3)
				throw new IllegalArgumentException(data + " is not a valid argument to this constructor");
			this.columnName = parsed[0];
			this.dataType = parsed[1];
			this.length = (TableDefinition.isFixLengthType(this.dataType) ? 0 : Integer.parseInt(parsed[2]));
		}
		else throw new IllegalArgumentException(data + " is not a valid argument to this constructor");
	}

	/**
	 * fully custom Constructor
	 * @param columnName the column name in the SQL table
	 * @param dataType the data type of the column in the SQL table
	 * @param length the length of the column in the SQL table (important only
	 *            for custom length data types like char or varchar)
	 */
	public TableColumnDefinition(String columnName, String dataType, int length) {
		this.columnName = columnName;
		this.dataType = dataType;
		this.length = (TableDefinition.isFixLengthType(this.dataType) ? 0 : length);
	}

	/**
	 * fully custom Constructor
	 * @param columnName the column name in the SQL table
	 * @param dataType the data type of the column in the SQL table
	 * @param length the length of the column in the SQL table (important only
	 *            for custom length data types like char or varchar)
	 * @param isValidationDef setting this parameter to true indicates that this
	 *            TableColumnDefinition is used on table validation, causing it
	 *            to return true on equals even if the length is more than that
	 *            of the compared TableColumnDefinition
	 */
	TableColumnDefinition(String columnName, String dataType, int length, boolean isValidationDef) {
		this(columnName, dataType, length);
		this.isValidationDef = isValidationDef;
	}
	
	/**
	 * @return the column name in the SQL table
	 */
	public String getColumnName() {
		return this.columnName;
	}

	/**
	 * @return the column's SQL data type
	 */
	public String getDataType() {
		return this.dataType;
	}

	/**
	 * @return the length of the column in the SQL table, or 0 if the data type
	 *         has a predefined or arbitrary length
	 */
	public int getColumnLength() {
		return this.length;
	}
//
//	/**
//	 * @return a part of a CREATE TABLE query to create the column the SQT table
//	 */
//	public String getCreationQuery() {
//		StringBuffer assembler = new StringBuffer(this.columnName + " " + this.dataType + ((this.length > 0) ? ("(" + this.length + ")") : "") + " DEFAULT ");
//		
//		if (TableDefinition.isEscapedType(this.dataType))
//			assembler.append("''");
//		else assembler.append("0");
//		
//		return assembler.toString();
//	}
//	
//	/**
//	 * create an ALTER TABLE query to update the column in the SQT table
//	 * @param oldDef the old TableColumnDefinition according to the existing SQL
//	 *            table
//	 * @return an ALTER TABLE query to update the column in the SQT table Note:
//	 *         The query shorten columns, only add or extend them
//	 */
//	public String getUpdateQuery(TableColumnDefinition oldDef) {
//		if ((oldDef == null) || !this.columnName.equalsIgnoreCase(oldDef.columnName))
//			return "ADD " + this.getCreationQuery();
//		else if (this.dataType.equalsIgnoreCase(oldDef.dataType) && (this.length > oldDef.length))
//			return "ALTER COLUMN " + this.columnName + " " + this.dataType + ((this.length > 0) ? ("(" + this.length + ")") : "");
//		else return null;
//	}

	/**
	 * Create a part for a CREATE TABLE query that creates the column.
	 * @param syntax a Properties object holding templates that mask product
	 *            specific database features
	 * @return a part of a CREATE TABLE query to create the column
	 */
	public String getCreationQuery(Properties syntax) {
		return this.replaceVariables(syntax.getProperty((this.length > 0) ? TableDefinition.SYNTAX_CREATE_TABLE_PART_VAR_LENGTH : TableDefinition.SYNTAX_CREATE_TABLE_PART_FIX_LENGTH));
	}
	
	/**
	 * create an ALTER TABLE query to update the column in the SQT table
	 * @param oldDef the old TableColumnDefinition according to the existing SQL
	 *            table
	 * @param syntax a Properties object holding templates that mask product
	 *            specific database features
	 * @return an ALTER TABLE query to update the column in the SQT table Note:
	 *         The query shorten columns, only add or extend them
	 */
	public String getUpdateQuery(TableColumnDefinition oldDef, Properties syntax) {
		if ((oldDef == null) || !this.columnName.equalsIgnoreCase(oldDef.columnName))
			return this.replaceVariables(syntax.getProperty((this.length > 0) ? TableDefinition.SYNTAX_ADD_COLUMN_VAR_LENGTH : TableDefinition.SYNTAX_ADD_COLUMN_FIX_LENGTH));
		if (this.dataType.equalsIgnoreCase(oldDef.dataType) && (this.length > oldDef.length))
			return this.replaceVariables(syntax.getProperty(TableDefinition.SYNTAX_WIDEN_COLUMN));
		else return null;
	}
	
	private String replaceVariables(String template) {
		template = TableDefinition.replaceVariable(template, TableDefinition.SYNTAX_COLUMN_NAME_VARIABLE, this.columnName);
		template = TableDefinition.replaceVariable(template, TableDefinition.SYNTAX_DATA_TYPE_VARIABLE, this.dataType);
		template = TableDefinition.replaceVariable(template, TableDefinition.SYNTAX_DEFAULT_VALUE_VARIABLE, (TableDefinition.isEscapedType(this.dataType) ? "''" : "0"));
		if (this.length > 0)
			template = TableDefinition.replaceVariable(template, TableDefinition.SYNTAX_COLUMN_LENGTH_VARIABLE, ("" + this.length));
		return template;
	}
	
	/**
	 * equals-method needed for Collection.contains, etc
	 */
	public boolean equals(Object o) {
		return ((o instanceof TableColumnDefinition) && this.equals((TableColumnDefinition) o));
	}

	/**
	 * compareTo-method needed for Collections.sort, etc
	 */
	public int compareTo(Object o) {
		TableColumnDefinition tcd = ((TableColumnDefinition) o);
		int result = this.columnName.compareTo(tcd.columnName);
		result = ((result == 0) ? this.dataType.compareTo(tcd.dataType) : result);
		result = (((result == 0) && TableDefinition.isFixLengthType(this.dataType)) ? (this.length - tcd.length) : result);
		return result;
	}
	
	/**	check if this TableColumnDefinition is equal to the argument one
	 * @param 	tcd	the TableColumnDefinition to ckeck for identity
	 * @return	true if and only if the SQL structure of both this and the argument TableColumnDefinition are equal, false otherwise
	 */
	public boolean equals(TableColumnDefinition tcd) {
		return (
				(tcd != null)
				&&
				this.columnName.equalsIgnoreCase(tcd.columnName)
				&&
				this.dataType.equalsIgnoreCase(tcd.dataType)
				&&
				(
					(this.length == tcd.length)
					||
					TableDefinition.isFixLengthType(this.dataType)
					||
					(this.isValidationDef && (this.length > tcd.length))
				)
			);
	}

	/**	@return	a String representation of this TableColumnDefinition, as accepted by the singular String constructor
	 * The String is formated like this: #'columnName';'dataType';'length'
	 */
	public String toString() {
		return "#" + this.columnName + ";" + this.dataType + ";" + this.length;
	}

	/**	@return	an XML representation of this TableColumnDefinition
	 */
	public String toXML() {
		return ("<" + TableDefinition.COLUMN_TAG + 
				" " + TableDefinition.NAME_ATTRIBUTE + "=\"" + this.columnName + "\"" + 
				" " + TableDefinition.DATATYPE_ATTRIBUTE + "=\"" + this.dataType + "\"" +
				" " + TableDefinition.LENGTH_ATTRIBUTE + "=\"" + this.length + "\"" + "" +
				"/>");
	}
}
