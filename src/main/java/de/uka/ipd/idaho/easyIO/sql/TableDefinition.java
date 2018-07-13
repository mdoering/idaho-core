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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Vector;

import de.uka.ipd.idaho.easyIO.SqlQueryResult;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.TreeTools;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * A generic definition of a table in a relational database. This class only
 * provides those data types that are named exactly the same in a variety of
 * different RDBMS, omitting the data types that are likely to cause trouble on
 * all but one RDBMS.
 * 
 * @author Sautter
 */
public class TableDefinition {
//	TODO consider reactivationg all the data types, with mappings to the respective names in individual DBMS
//	
//	//	bit datatype
//	public static final String BIT_DATATYPE = "BIT";
//	
//	//	int datatypes
//	public static final String TINYINT_DATATYPE = "TINYINT"; // TODO this one should be possible to use
//	public static final String SMALLINT_DATATYPE = "SMALLINT"; // TODO this one should be possible to use
//	public static final String INT_DATATYPE = "INT";
//	public static final String BIGINT_DATATYPE = "BIGINT";
//	
//	//	fix point datatypes
//	public static final String DECIMAL_DATATYPE = "DECIMAL";
//	public static final String NUMERIC_DATATYPE = "NUMERIC";
//	
//	//	floating point datatypes
//	public static final String REAL_DATATYPE = "REAL";
//	public static final String FLOAT_DATATYPE = "FLOAT";
//	
//	//	date & time datatypes
//	public static final String SMALLDATETIME_DATATYPE = "SMALLDATETIME";
//	public static final String DATETIME_DATATYPE = "DATETIME";
//	public static final String TIMESTAMP_DATATYPE = "TIMESTAMP";
//	
//	//	money datatypes
//	public static final String SMALLMONEY_DATATYPE = "SMALLMONEY";
//	public static final String MONEY_DATATYPE = "MONEY";
//	
//	//	character datatypes
//	public static final String CHAR_DATATYPE = "CHAR";
//	public static final String VARCHAR_DATATYPE = "VARCHAR";
//	public static final String TEXT_DATATYPE = "TEXT"; // TODO this one should be possible to use
//	
//	//	unicode character datatypes
//	public static final String NCHAR_DATATYPE = "NCHAR";
//	public static final String NTEXT_DATATYPE = "NTEXT";
//	public static final String NVARCHAR_DATATYPE = "NVARCHAR";
//	
//	//	binary datatypes
//	public static final String BINARY_DATATYPE = "BINARY";
//	public static final String VARBINARY_DATATYPE = "VARBINARY";
//	public static final String IMAGE_DATATYPE = "IMAGE";
//	
//	//	datatype classes
//	public static final String ALL_DATATYPES = (";" + BIGINT_DATATYPE + ";" + BINARY_DATATYPE + ";" + BIT_DATATYPE + ";" + CHAR_DATATYPE + ";" + DATETIME_DATATYPE + ";" + DECIMAL_DATATYPE + ";" + FLOAT_DATATYPE + ";" + IMAGE_DATATYPE + ";" + INT_DATATYPE + ";" + MONEY_DATATYPE + ";" + NCHAR_DATATYPE + ";" + NTEXT_DATATYPE + ";" + NUMERIC_DATATYPE + ";" + NVARCHAR_DATATYPE + ";" + REAL_DATATYPE + ";" + SMALLDATETIME_DATATYPE + ";" + SMALLINT_DATATYPE + ";" + SMALLMONEY_DATATYPE + ";" + TEXT_DATATYPE + ";" + TIMESTAMP_DATATYPE + ";" + TINYINT_DATATYPE + ";" + VARBINARY_DATATYPE + ";" + VARCHAR_DATATYPE + ";");
//	public static final String NO_LENGTH_DATATYPES = (";" + BIGINT_DATATYPE + ";" + BIT_DATATYPE + ";" + DATETIME_DATATYPE + ";" + FLOAT_DATATYPE + ";" + IMAGE_DATATYPE + ";" + INT_DATATYPE + ";" + MONEY_DATATYPE + ";" + NTEXT_DATATYPE + ";" + REAL_DATATYPE + ";" + SMALLDATETIME_DATATYPE + ";" + SMALLINT_DATATYPE + ";" + SMALLMONEY_DATATYPE + ";" + TEXT_DATATYPE + ";" + TIMESTAMP_DATATYPE + ";" + TINYINT_DATATYPE + ";");
//	public static final String ESCAPED_DATATYPES = (";" + CHAR_DATATYPE + ";" + DATETIME_DATATYPE + ";" + NCHAR_DATATYPE + ";" + NTEXT_DATATYPE + ";" + NVARCHAR_DATATYPE + ";" + SMALLDATETIME_DATATYPE + ";" + TEXT_DATATYPE + ";" + TIMESTAMP_DATATYPE + ";" + VARCHAR_DATATYPE + ";");
//	public static final String DATE_DATATYPES = (";" + DATETIME_DATATYPE + ";" + SMALLDATETIME_DATATYPE + ";" + TIMESTAMP_DATATYPE + ";");
	
	//	int datatypes
	public static final String INT_DATATYPE = "INT";
	public static final String BIGINT_DATATYPE = "BIGINT";
	
	//	floating point datatypes
	public static final String REAL_DATATYPE = "REAL";
	
	//	character datatypes
	public static final String CHAR_DATATYPE = "CHAR";
	public static final String VARCHAR_DATATYPE = "VARCHAR";
	
	//	datatype classes
	public static final String ALL_DATATYPES = (";" + BIGINT_DATATYPE + ";" + CHAR_DATATYPE + ";" + ";" + INT_DATATYPE + ";" + ";" + ";" + REAL_DATATYPE + ";" + VARCHAR_DATATYPE + ";");
	public static final String NO_LENGTH_DATATYPES = (";" + BIGINT_DATATYPE + ";" + INT_DATATYPE + ";" + REAL_DATATYPE + ";");
	public static final String ESCAPED_DATATYPES = (";" + CHAR_DATATYPE + ";" + VARCHAR_DATATYPE + ";");
	
	//	tag and attribute names for XML representation
	public static final String TABLE_TAG = "table";
	public static final String COLUMN_TAG = "column";
	public static final String NAME_ATTRIBUTE = "name";
	public static final String DATATYPE_ATTRIBUTE = "datatype";
	public static final String LENGTH_ATTRIBUTE = "length";
	
	//	SQL syntax properties
	public static final String SYNTAX_CREATE_TABLE_PART_VAR_LENGTH = "CREATE_TABLE_VAR_LENGTH";
	public static final String SYNTAX_CREATE_TABLE_PART_FIX_LENGTH = "CREATE_TABLE_FIX_LENGTH";
	public static final String SYNTAX_ADD_COLUMN_VAR_LENGTH = "ADD_COLUMN_VAR_LENGTH";
	public static final String SYNTAX_ADD_COLUMN_FIX_LENGTH = "ADD_COLUMN_FIX_LENGTH";
	public static final String SYNTAX_WIDEN_COLUMN = "WIDEN_COLUMN";
	
	public static final String SYNTAX_CREATE_INDEX = "CREATE_INDEX";
	
	public static final String SYNTAX_CREATE_PRIMARY_KEY = "CREATE_PK";
	public static final String SYNTAX_CREATE_FOREIGN_KEY = "CREATE_FK";
	
	//	variables for SQL syntax properties
	public static final String SYNTAX_COLUMN_NAME_VARIABLE = "@colName";
	public static final String SYNTAX_DATA_TYPE_VARIABLE = "@dataType";
	public static final String SYNTAX_COLUMN_LENGTH_VARIABLE = "@length";
	public static final String SYNTAX_DEFAULT_VALUE_VARIABLE = "@default";
	
	public static final String SYNTAX_INDEXED_TABLE_VARIABLE = "@table";
	public static final String SYNTAX_INDEX_NAME_VARIABLE = "@indexName";
	public static final String SYNTAX_INDEXED_COLUMNS_VARIABLE = "@columns";
	
	public static final String SYNTAX_KEYED_TABLE_VARIABLE = "@table";
	public static final String SYNTAX_KEY_CONSTRAINT_NAME_VARIABLE = "@keyConstraintName";
	public static final String SYNTAX_KEYED_COLUMN_VARIABLE = "@column";
	public static final String SYNTAX_REFERENCED_TABLE_VARIABLE = "@refTable";
	public static final String SYNTAX_REFERENCED_COLUMN_VARIABLE = "@refColumn";
	
	//	parser for instantiation from XML String
	protected static Parser PARSER = new Parser(new StandardGrammar() {
		
		/** @see de.htmlXmlUtil.grammars.Grammar#correctErrors()
		 */
		public boolean correctErrors() {
			return false;
		}
		
		/** @see de.htmlXmlUtil.grammars.Grammar#getParentTags(java.lang.String)
		 */
		public HashSet getParentTags(String tag) {
			HashSet parents = new HashSet();
			if ((tag != null) && tag.equalsIgnoreCase(TableDefinition.COLUMN_TAG)) parents.add(TableDefinition.TABLE_TAG);
			return parents;
		}
		
		/** @see de.htmlXmlUtil.grammars.Grammar#canBeChildOf(java.lang.String, java.lang.String)
		 */
		public boolean canBeChildOf(String child, String parent) {
			return ((parent != null) && parent.equalsIgnoreCase(TableDefinition.TABLE_TAG) && (child != null) && child.equalsIgnoreCase(TableDefinition.COLUMN_TAG));
		}
	});
	
	/**
	 * check if a String denotes a valid data type
	 * @param dataType the String to check
	 * @return true if and only if the specified String is the name of a data
	 *         type
	 */
	public static boolean isDataType(String dataType) {
		return ((dataType != null) && (ALL_DATATYPES.indexOf(";" + dataType.toUpperCase() + ";") != -1));
	}

	/**
	 * check if a String denotes a data type that has a fix length
	 * @param dataType the String to check
	 * @return true if and only if the specified String is the name of a data
	 *         type with a fix length
	 */
	public static boolean isFixLengthType(String dataType) {
		return ((dataType != null) && (NO_LENGTH_DATATYPES.indexOf(";" + dataType.toUpperCase() + ";") != -1));
	}

	/**
	 * check if a String denotes a data type that needs to be escaped in queries
	 * @param dataType the String to check
	 * @return true if and only if the specified String is the name of a data
	 *         type that needs to be escaped in queries
	 */
	public static boolean isEscapedType(String dataType) {
		return ((dataType != null) && (ESCAPED_DATATYPES.indexOf(";" + dataType.toUpperCase() + ";") != -1));
	}

	protected String tableName;

	protected Vector columns = new Vector();

	protected boolean isValidationDef = false;

	/**
	 * Void Constuctor for subclasses
	 */
	protected TableDefinition() {
	}

	/**
	 * Constructor
	 * @param data the SqlQueryResult that's meta data is used to build the
	 *            Definition
	 * @param tableName the name of the SQL table that's structure is to be
	 *            extracted from data Note: This constructor is intended to get
	 *            the definition of an existing SQL table in order to check if
	 *            the table is to be extended to comply with a new definition
	 */
	public TableDefinition(SqlQueryResult data, String tableName) {
		this.tableName = tableName;
		this.isValidationDef = true;
		for (int i = 0; i < data.getColumnCount(); i++) {
			String colName = data.getColumnName(i);
			String colType = data.getColumnType(i);
			int length = data.getColumnLength(i);
			this.addColumn(new TableColumnDefinition(colName, colType, length, true));
		}
	}
	
	/**
	 * Constructor
	 * @param data the String containing all values for this TableDefinition and
	 *            it's IndexColumnDefinitions if data starts with '!', it is
	 *            assumed to be the String representation of a TableDefinition
	 *            (!'tableName''columnDefinitions'*) if data starts with '&lt;',
	 *            it is assumed to be the XML representation of a
	 *            TableDefinition otherwise, data must not contain any String
	 *            representations of TableColumnDefinitions (starting with '#')
	 *            and is assumed to simply be the table name
	 */
	public TableDefinition(String data) {

		// String representation
		if (data.startsWith("!")) {
			String[] parsed = data.split("\\#");
			this.tableName = parsed[0].substring(1);
			for (int c = 1; c < parsed.length; c++)
				this.addColumn(new TableColumnDefinition("#" + parsed[c]));
		}
		
		//	XML representation
		else if (data.startsWith("<" + TABLE_TAG)) {
			try {
				TreeNode root = PARSER.parse(data);
				TreeNode tableNode = root.getChildNode(TABLE_TAG, 0);
				
				//	read table name
				if (!tableNode.hasAttribute(NAME_ATTRIBUTE)) throw new IllegalArgumentException(data + " is not a valid argument to this constructor, table name is missing.");
				this.tableName = tableNode.getAttribute(NAME_ATTRIBUTE);
				
				//	initialize columns
				TreeNode[] columnNodes = TreeTools.getAllNodesOfType(tableNode, COLUMN_TAG);
				for (int c = 0; c < columnNodes.length; c++) {
					
					//	column name missing
					if (!columnNodes[c].hasAttribute(NAME_ATTRIBUTE))
						throw new IllegalArgumentException(data + " is not a valid argument to this constructor, name of column " + c + " is missing.");
						
					//	datatype missing
					else if (!columnNodes[c].hasAttribute(DATATYPE_ATTRIBUTE))
						throw new IllegalArgumentException(data + " is not a valid argument to this constructor, datatype of column " + c + " is missing.");
						
					//	name and datatype OK
					else {
						String columnName = columnNodes[c].getAttribute(NAME_ATTRIBUTE);
						String dataType = columnNodes[c].getAttribute(DATATYPE_ATTRIBUTE);
						
						//	invalid datatype
						if (!isDataType(dataType))
							throw new IllegalArgumentException(data + " is not a valid argument to this constructor, datatype of column " + c + " is invalid.");
						
						//	read length if necessary
						int length = 0;
						if (!isFixLengthType(dataType)) {
							
							if (!columnNodes[c].hasAttribute(LENGTH_ATTRIBUTE))
								throw new IllegalArgumentException(data + " is not a valid argument to this constructor, length of column " + c + " is missing.");
							
							try {
								length = Integer.parseInt(columnNodes[c].getAttribute(LENGTH_ATTRIBUTE));
							}
							catch (NumberFormatException nfe) {
								throw new IllegalArgumentException(data + " is not a valid argument to this constructor, length of column " + c + " is invalid.");
							}
						}
						
						//	add column
						this.addColumn(columnName, dataType, length);
					}
				}
			}
			catch (IOException e) {
				throw new IllegalArgumentException(data + " is not a valid argument to this constructor");
			}
		}
		
		//	just the table name
		else if (data.indexOf("#") == -1)
			this.tableName = data;
		
		else throw new IllegalArgumentException(data + " is not a valid argument to this constructor");
	}
	
	/**
	 * add a column to the table
	 * @param columnName the column name in the SQL table
	 * @param dataType the data type of the column in the SQL table
	 * @param length the length of the column in the SQL table (important only
	 *            for custom length data types like char or varchar)
	 */
	public void addColumn(String columnName, String dataType, int length) {
		this.addColumn(new TableColumnDefinition(columnName, dataType, length));
	}

	/**
	 * add a column to the table
	 * @param column the definition of the column to be added
	 */
	public void addColumn(TableColumnDefinition column) {
		if (column != null) this.columns.addElement(column);
	}

	/**
	 * @return the TableColumnDefinitions of this TableDefinition
	 */
	public TableColumnDefinition[] getColumns() {
		return ((TableColumnDefinition[]) this.columns.toArray(new TableColumnDefinition[this.columns.size()]));
	}

	/**
	 * @return the table name
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * get a column definition by it's SQL column name or it's annotation
	 * attribute name
	 * @param name the desired column definition's column name
	 * @return the desired TableColumnDefinition, or null if there is none with
	 *         the specified name
	 */
	public TableColumnDefinition getColumn(String name) {
		for (int c = 0; c < this.columns.size(); c++) {
			TableColumnDefinition tcd = ((TableColumnDefinition) this.columns.get(c));
			if (tcd.getColumnName().equalsIgnoreCase(name))
				return tcd;
		}
		return null;
	}
	
	/**	@return	a dummy SELECT query to validate the table and columns
	 */
	public String getColumnValidationQuery() {
		String columnString = "";
		for (int c = 0; c < this.columns.size(); c++) {
			TableColumnDefinition tcd = ((TableColumnDefinition) this.columns.get(c));
			columnString += (((c == 0) ? "" : ", ") + tcd.getColumnName());
		}
//		return ((columnString.indexOf(", ") == -1) ? null : ("SELECT " + columnString + " FROM " + this.tableName + " WHERE 1=0;"));
		return ((columnString.length() == 0) ? null : ("SELECT " + columnString + " FROM " + this.tableName + " WHERE 1=0;"));
	}
	
	/**
	 * @return a dummy SELECT query to validate the table and columns
	 */
	public String getValidationQuery() {
		return ("SELECT * FROM " + this.tableName + " WHERE 1=0;");
	}

	/**
	 * Produce a CREATE TABLE query to create the SQT table. The argument
	 * IoProvider supplies product specific database properties.
	 * @param syntax a Properties object holding templates that mask product
	 *            specific database features
	 * @return a CREATE TABLE query to create the SQT table
	 */
	public String getCreationQuery(Properties syntax) {
		StringBuffer columnString = new StringBuffer("CREATE TABLE " + this.tableName + " (");
		for (int c = 0; c < this.columns.size(); c++) {
			if (c != 0)
				columnString.append(", ");
			TableColumnDefinition tcd = ((TableColumnDefinition) this.columns.get(c));
			columnString.append(tcd.getCreationQuery(syntax));
		}
		columnString.append(");");
		return columnString.toString();
	}
	
	/**
	 * Produce ALTER TABLE queries to update an existing SQL table to this
	 * TableDefinition. Note: The queries won't drop or shorten columns, only
	 * add or widen them. The argument IoProvider supplies product specific
	 * database properties.
	 * @param oldDef the old TableDefinition according to the existing SQL table
	 * @param syntax a Properties object holding templates that mask product
	 *            specific database features
	 * @return a set of ALTER TABLE queries to update the SQL table
	 */
	public String[] getUpdateQueries(TableDefinition oldDef, Properties syntax) {
		ArrayList queries = new ArrayList();
		if (oldDef == null)
			queries.add(this.getCreationQuery(syntax));
		else if (this.tableName.equalsIgnoreCase(oldDef.tableName) && !this.equals(oldDef)) {
			for (int c = 0; c < this.columns.size(); c++) {
				TableColumnDefinition tcd = ((TableColumnDefinition) this.columns.get(c));
				TableColumnDefinition oldTcd = oldDef.getColumn(tcd.getColumnName());
				if (!tcd.equals(oldTcd)) {
					String query = tcd.getUpdateQuery(oldTcd, syntax);
					if (query != null)
						queries.add("ALTER TABLE " + this.tableName + " " + query + ";");
				}
			}
		}
		return ((String[]) queries.toArray(new String[queries.size()]));
	}
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return ((o != null) && (o instanceof TableDefinition) && this.equals((TableDefinition) o));
	}

	/**
	 * check if this TableDefinition is equal to the argument one, abstract from
	 * Annotation-related data
	 * @param td the TableDefinition to ckeck for identity
	 * @return true if and only if the SQL structure of both this and the
	 *         argument TableDefinition are equal, false otherwise
	 */
	public boolean equals(TableDefinition td) {
		return ((td != null) && this.tableName.equalsIgnoreCase(td.tableName) && this.columnsEqual(td));
	}
	
	//	compare the columns
	private boolean columnsEqual(TableDefinition td) {
		if (this.isValidationDef) {
			boolean equal = (this.columns.size() >= td.columns.size());
			Vector otherCols = (Vector) td.columns.clone();
			for (int i = 0; equal && (i < otherCols.size()); i++) {
				TableColumnDefinition otherCol = (TableColumnDefinition) otherCols.get(i);
				TableColumnDefinition ownCol = this.getColumn(otherCol.getColumnName());
				equal = (equal && (ownCol != null) && ownCol.equals(otherCol));
			}
			return equal;
		}
		else {
			boolean equal = (this.columns.size() == td.columns.size());
			if (equal) {
				Vector ownCols = (Vector) this.columns.clone();
				Vector otherCols = (Vector) td.columns.clone();
				Collections.sort(ownCols);
				Collections.sort(otherCols);
				for (int i = 0; equal && (i < ownCols.size()); i++) {
					TableColumnDefinition ownCol = (TableColumnDefinition) ownCols.get(i);
					TableColumnDefinition otherCol = (TableColumnDefinition) otherCols.get(i);
					equal = equal && otherCol.equals(ownCol);
				}
			}
			return equal;
		}
	}
	
	/**
	 * @return a String representation of this TableDefinition, as accepted by
	 *         the singular String constructor The String is formated like this:
	 *         !'tableName''columnDefinitions'*
	 */
	public String toString() {
		StringBuffer assembler = new StringBuffer("!" + this.tableName);
		for (int i = 0; i < this.columns.size(); i++)
			assembler.append((TableColumnDefinition) this.columns.get(i)).toString();
		return assembler.toString();
	}

	/**
	 * @return an XML representation of this TableDefinition
	 */
	public String toXML() {
		//	build XML representation of empty table
		if (this.columns.size() == 0)
			return ("<" + TABLE_TAG + 
					" " + NAME_ATTRIBUTE + "=\"" + this.tableName + "\"" +
					"/>");
		
		//	build XML representation of table with columns
		StringBuffer assembler = new StringBuffer("<" + TABLE_TAG + 
													" " + NAME_ATTRIBUTE + "=\"" + this.tableName + "\"" +
													">\n");
		for (int c = 0; c < this.columns.size(); c++)
			assembler.append("  " + ((TableColumnDefinition) this.columns.get(c)).toXML() + "\n");
		assembler.append("</" + TABLE_TAG + ">\n");
		return assembler.toString();
	}
	
	/**
	 * Load the product specific syntax resource for a given database system,
	 * identified by the class name of the JDBC driver. If the syntax resource
	 * is not found, this method returns an empty Properties object, but never
	 * null.
	 * @param jdbcDriverClassName the class name of the JDBC driver
	 * @return the product specific syntax resource
	 */
	public static synchronized Properties loadJdbcSyntax(String jdbcDriverClassName) {
		if (jdbcSyntaxFileMappings == null) try {
			jdbcSyntaxFileMappings = loadProperties("JdbcDriversToSyntaxResources.txt", false);
			jdbcSyntaxDefault = loadProperties("default.syntax.txt", false);
		}
		catch (IOException ioe) {
			System.out.println("TableDefinition: could not load JDBC syntax file mappings: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		String jdbcSyntaxFileName = jdbcSyntaxFileMappings.getProperty(jdbcDriverClassName);
		try {
			return loadProperties(jdbcSyntaxFileName, true);
		}
		catch (IOException ioe) {
			System.out.println("TableDefinition: could not load JDBC syntax file: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return new Properties(jdbcSyntaxDefault);
		}
	}
	private static Properties loadProperties(String name, boolean setDefaults) throws IOException {
		String tdcrn = TableDefinition.class.getName().replaceAll("\\.", "/");
		Properties props = new Properties(setDefaults ? jdbcSyntaxDefault : null);
		InputStream propIn = TableDefinition.class.getClassLoader().getResourceAsStream(tdcrn.substring(0, tdcrn.lastIndexOf('/')) + "/" + name);
		if (propIn == null)
			return props;
		BufferedReader propBr = new BufferedReader(new InputStreamReader(propIn));
		String prop;
		while ((prop = propBr.readLine()) != null) {
			prop = prop.trim();
			if ((prop.length() == 0) || prop.startsWith("//"))
				continue;
			String[] propParts = prop.split("\\s*\\=\\s*", 2);
			if (propParts.length == 2)
				props.setProperty(propParts[0], propParts[1]);
		}
		propBr.close();
		return props;
	}
	private static Properties jdbcSyntaxFileMappings = null;
	private static Properties jdbcSyntaxDefault = null;
	
	/**
	 * Replace a variable in a template with its value. The value must not
	 * include the variable.
	 * @param template the template
	 * @param variable the variable to replace
	 * @param value the value of the variable
	 * @return the template with the variable replaced
	 */
	public static String replaceVariable(String template, String variable, String value) {
		if (!variable.startsWith("@"))
			variable = ("@" + variable);
		int offset;
		while ((offset = template.indexOf(variable)) != -1)
			template = template.substring(0, offset) + value + template.substring(offset + variable.length());
		return template;
	}
}
