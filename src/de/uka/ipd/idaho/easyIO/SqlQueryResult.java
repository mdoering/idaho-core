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
package de.uka.ipd.idaho.easyIO;


import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * An SqlQueryResult wraps a java.sqj.ResultSet. It is capable of copying the
 * complete result set to a two-dimensional array of Strings, closing the
 * backing ResultSet immediately. While this may take a lot of memory for large
 * results, it facilitates relesing backing resources as early as possible.
 * Thus, copying is recommended for small result sets that are worked on for
 * longer periods of time. In addition, copying can add backward navigation
 * capabilities to result sets that do not support this tzpe of navigation. To
 * accomodate to general Java, this class maps the 1, ..., n indexing style used
 * in SQL to the 0, ..., (n-1) indexing style common in Java.
 * 
 * @author sautter
 */
public class SqlQueryResult {
	
	private static final String noLengthTypes = ";bigint;bit;datetime;float;image;int;money;ntext;real;smalldatetime;smallint;smallmoney;text;timestamp;tinyint;";
	
	//	query execution data
	private String query;
	private boolean success = true;
	
	private int columnCount = -1;
	private int rowCount = -1;
	
	//	administrative data
	private int currentRow = -1;
	private boolean copied = true;
	private ResultSet resultSet;
	
	//	resultArray meta data
	private String[] columnTitles = new String[0];
	private String[] dataTypeNames = new String[0];
	private int[] columnLengths = new int[0];
	private int[] dataTypeCodes = new int[0];
	private String[][] resultArray = new String[0][0];
	
	//	exception
	private SQLException exception = null;
	
	/**	Constructor retrieving data from an SQL resultArray set retrieved from some database via JDBC
	 * @param	query	the query res resulted from
	 * @param	resultSet		the SQL resultArray set containing the data
	 */
	public SqlQueryResult(String query, ResultSet resultSet) {
		this(query, resultSet, false);
	}
	
	/**	Constructor retrieving data from an SQL resultArray set retrieved from some database via JDBC
	 * @param	query	the query res resulted from
	 * @param	resultSet		the SQL resultArray set containing the data
	 * @param	copy	if set to true, the entire resultArray table will be copied to an array for faster access (high memory cost!)
	 */
	public SqlQueryResult(String query, ResultSet resultSet, boolean copy) {
		this.query = query;
		if (resultSet == null)
			this.success = false;
		else {
			try {
				//	read meta data
				ResultSetMetaData meta = resultSet.getMetaData();
				this.columnCount = meta.getColumnCount();
				this.columnTitles = new String[this.columnCount];
				this.dataTypeNames = new String[this.columnCount];
				this.dataTypeCodes = new int[this.columnCount];
				this.columnLengths = new int[this.columnCount];
				for (int i = 0; i < this.columnCount; i++) {
					this.columnTitles[i] = meta.getColumnName(i+1);
					this.dataTypeNames[i] = meta.getColumnTypeName(i+1);
					this.dataTypeCodes[i] = meta.getColumnType(i+1);
					this.columnLengths[i] = meta.getPrecision(i+1);
				}
				
				//	copy result data to array if desired
				if (copy) {
					int rows = 0;
					ArrayList[] columns = new ArrayList[this.columnCount];
					for (int i = 0; i < this.columnCount; i++)
						columns[i] = new ArrayList();
					
					while (resultSet.next()) {
						for (int i = 0; i < this.columnCount; i++) {
							String s = resultSet.getString(i+1);
							columns[i].add(s);
						}
						rows ++;
					}
					this.rowCount = rows;
					this.resultArray = new String[this.columnCount][this.rowCount];
					for (int i = 0; i < this.columnCount; i++)
						this.resultArray[i] = ((String[]) columns[i].toArray(new String[rows]));
//					StringVector[] columns = new StringVector[this.columnCount];
//					for (int i = 0; i < this.columnCount; i++)
//						columns[i] = new StringVector();
//					
//					while (resultSet.next()) {
//						for (int i = 0; i < this.columnCount; i++) {
//							String s = resultSet.getString(i+1);
//							columns[i].addElement(s);
//						}
//						rows ++;
//					}
//					this.rowCount = rows;
//					this.resultArray = new String[this.columnCount][this.rowCount];
//					for (int i = 0; i < this.columnCount; i++)
//						this.resultArray[i] = columns[i].toStringArray();
					
					resultSet.close();
					System.gc();
				}
				
				//	put up logistics for standard ResultSet handling otherwise
				else {
					this.resultSet = resultSet;
					this.resultSet.last();
					this.rowCount = this.resultSet.getRow();
					this.resultSet.beforeFirst();
					this.currentRow = -1;
					this.copied = false;
				}
			}
			catch (SQLException e) {
				System.out.println("SqlQueryResult: " + e.getMessage() + " while initializing");
				this.exception = e;
				this.success = false;
			}
		}
	}
	
	/** close this SqlQueryResult, releasing any resources
	 */
	public void close() {
		if (this.resultSet != null) try {
			this.resultSet.close();
			this.resultSet.getStatement().close();
		} catch (SQLException sqle) {}
		
		this.columnTitles = null;
		this.dataTypeNames = null;
		this.resultArray = null;
	}
	
	/** @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		if (this.resultSet != null) {
			this.resultSet.close();
			this.resultSet.getStatement().close();
		}
	}
	
	/**	tells weather the query was executed successfully
	 * @return	true if the query was executed successfully, false if any exception occured
	 */
	public boolean wasSuccessful() {
		return this.success;
	}
	
	/**	tells weather the query resultArray was copied to main memory
	 * @return	true if the query resultArray was copied to main memory, false if it contains a cursor
	 */
	public boolean isCopied() {
		return this.copied;
	}
	
	/** @see java.sql.ResultSet#isFirst()
	 */
	public boolean isBeforeFirst() throws SQLException {
		if (this.copied) {
			return (this.currentRow == -1);
		} else {
			return this.resultSet.isBeforeFirst();
		}
	}

	/** @see java.sql.ResultSet#isFirst()
	 */
	public boolean isFirst() throws SQLException {
		if (this.copied)
			return (this.currentRow == 0);
		else return this.resultSet.isFirst();
	}

	/** @see java.sql.ResultSet#isLast()
	 */
	public boolean isLast() throws SQLException {
		if (this.copied)
			return ((this.currentRow == (this.rowCount - 1)) && (this.rowCount != 0));
		else return this.resultSet.isLast();
	}
	
	/** @see java.sql.ResultSet#isLast()
	 */
	public boolean isAfterLast() throws SQLException {
		if (this.copied)
			return (this.currentRow == this.rowCount);
		else return this.resultSet.isAfterLast();
	}

	/** @see java.sql.ResultSet#beforeFirst()
	 */
	public boolean beforeFirst() {
		try {
			if (!this.copied)
				this.resultSet.beforeFirst();
			this.currentRow = -1;
			return true;
		}
		catch (SQLException sqle) {
			return false;
		}
	}

	/** @see java.sql.ResultSet#first()
	 */
	public boolean first() {
		try {
			if (!this.copied)
				this.resultSet.first();
			this.currentRow = 0;
			return true;
		}
		catch (SQLException sqle) {
			return false;
		}
	}
	
	/** @see java.sql.ResultSet#last()
	 */
	public boolean last() {
		try {
			if (!this.copied)
				this.resultSet.last();
			this.currentRow = (this.rowCount - 1);
			return true;
		}
		catch (SQLException sqle) {
			return false;
		}
	}
	
	/** @see java.sql.ResultSet#afterLast()
	 */
	public boolean afterLast() {
		try {
			if (!this.copied)
				this.resultSet.afterLast();
			this.currentRow = this.rowCount;
			return true;
		}
		catch (SQLException sqle) {
			return false;
		}
	}

	/** @see java.sql.ResultSet#next()
	 */
	public boolean next() {
		boolean isAfterLast = (this.currentRow == this.rowCount);
		if (!isAfterLast) this.currentRow ++;
		if (this.copied)
			return (this.currentRow < this.rowCount);
		else {
			try {
				return this.resultSet.next();
			}
			catch (SQLException sqle) {
				if (!isAfterLast)
					this.currentRow --;
				return false;
			}
		}
	}
	
	/** @see java.sql.ResultSet#previous()
	 */
	public boolean previous() {
		boolean isBeforeFirst = (this.currentRow == -1);
		if (!isBeforeFirst) this.currentRow --;
		if (this.copied)
			return (this.currentRow > -1);
		else {
			try {
				return this.resultSet.previous();
			}
			catch (SQLException sqle) {
				if (!isBeforeFirst)
					this.currentRow ++;
				return false;
			}
		}
	}
	
	/** @see java.sql.ResultSet#absolute(int)
	 */
	public boolean goToRow(int rowIndex) {
		//System.out.println("StandardIoProvider: jumping to " + rowIndex);
		if (rowIndex == this.currentRow)
			return true;
		
		else if (this.success && (rowIndex >= 0) && (rowIndex < this.rowCount)) {
			//System.out.println("StandardIoProvider: trying to jump to " + rowIndex);
			try {
				if (this.copied || this.resultSet.absolute(rowIndex + 1)) {
					//System.out.println("StandardIoProvider: arrived at " + rowIndex);
					this.currentRow = rowIndex;
					return true;
				}
				else return false;
			}
			catch (SQLException sqle) {
				return false;
			}
		}
		else return false;
	}
	
	/** @see java.sql.ResultSet#getRow()
	 */
	public int getCurrentRowIndex() {
		if (!this.copied)
			try {
				this.currentRow = this.resultSet.getRow() - 1;
			}
			catch (SQLException sqle) {}
		
		return this.currentRow;
	}
	
	/** @see java.sql.ResultSet#getString(java.lang.String)
	 */
	public String getString(String columnName) {
		return this.getString(this.findColumn(columnName));
	}
	
	/** @see java.sql.ResultSet#getString(int)
	 */
	public String getString(int columnIndex) {
		if (this.success && (columnIndex >= 0) && (columnIndex < this.columnCount) && (this.currentRow >= 0) && (this.currentRow < this.rowCount)) {
			if (this.copied)
				return this.resultArray[columnIndex][this.currentRow];
			else {
				try {
					return this.resultSet.getString(columnIndex + 1);
				}
				catch (SQLException sqle) {
					return null;
				}
			}
		}
		else return null;
	}
	
	/**	get the content of column col, row row in the resultArray
	 * @param	rowIndex		the row index
	 * @param	columnIndex		the column index
	 * @return	the content of the indexed resultArray table field, if both indices are in range
	 */
	public String getString(int rowIndex, int columnIndex) {
		int store = this.currentRow;
		if (this.goToRow(rowIndex)) {
			String string = this.getString(columnIndex);
			this.goToRow(store);
			return string;
		}
		else return null;
	}
	
	/** @see java.sql.ResultSet#findColumn(java.lang.String)
	 */
	public int findColumn(String columnName) {
		for (int i = 0; i < this.columnTitles.length; i++)
			if (this.columnTitles[i].equalsIgnoreCase(columnName)) return i;
		
		return -1;
	}
	
	/**	@return		the query that the contained data resulted from
	 */
	public String getQuery() {
		return this.query;
	}
	
	/**	@return		the number of columns in the query resultArray, or -1 if the number of columns is not available
	 */
	public int getColumnCount() {
		return this.columnCount;
	}
	
	/**	@return		the number of rows in th query resultArray, thus the number of records, or -1 if the number of rows is not available
	 */
	public int getRowCount() {
		return this.rowCount;
	}
	
	/**	@return		if the query failed for some reason, the exception that caused the failure 
	 */
	public SQLException getException() {
		return this.exception;
	}
	
	/**	get the name of of the index-th column if the query was executed successfully
	 * @param	columnIndex	the index of the columns that's name is required
	 * @return	the name of index-th column, if the index is in the range of 0 - (index-1), null otherwise
	 */
	public String getColumnName(int columnIndex) {
		if (this.success && (columnIndex >= 0) && (columnIndex < this.columnTitles.length))
			return this.columnTitles[columnIndex];
		else return null;
	}
	
	/**	get the data type name of the index-th column if the query was executed successfully
	 * @param	columnIndex	the index of the columns that's data type name is required
	 * @return	the data type name of index-th column, if the index is in the range of 0 - (index-1), null otherwise
	 */
	public String getColumnType(int columnIndex) {
		if (this.success && (columnIndex >= 0) && (columnIndex < this.dataTypeNames.length))
			return this.dataTypeNames[columnIndex];
		else return null;
	}
	
	/**	get the data type code of the index-th column if the query was executed successfully
	 * @param	columnIndex	the index of the columns that's data type code is required
	 * @return	the data type code of index-th column, if the index is in the range of 0 - (index-1), 0 otherwise
	 */
	public int getColumnTypeCode(int columnIndex) {
		if (this.success && (columnIndex >= 0) && (columnIndex < this.dataTypeCodes.length))
			return this.dataTypeCodes[columnIndex];
		else return 0;
	}
	
	/**	get the length of the index-th column if the query was executed successfully
	 * @param	columnIndex	the index of the columns that's length is required
	 * @return	the data type code of index-th column, if the index is in the range of 0 - (index-1), 0 otherwise
	 * 	Note:	For fix length data types like int or float, this method also returns 0
	 * 			If the columns may have an arbitrary length (eg text), this method also returns 0
	 */
	public int getColumnLength(int columnIndex) {
		if (this.success && (columnIndex >= 0) && (columnIndex < this.columnLengths.length)) {
			String type = this.dataTypeNames[columnIndex].toLowerCase();
			return ((noLengthTypes.indexOf(";" + type + ";") > -1) ? 0 : this.columnLengths[columnIndex]);
		}
		else return 0;
	}
	
	/**	get all entries of a resultArray column in a StringVector object
	 * @param	columnIndex	the index of the requested column
	 * @return	all rowCount of the index-th resultArray column
	 */
	public String[] getColumnData(int columnIndex) {
		if (this.success && (columnIndex >= 0) && (columnIndex < this.columnCount)) {
			ArrayList ret = new ArrayList();
			int indexStore = this.getCurrentRowIndex();
			if (this.beforeFirst()) {
				while (this.next())
					ret.add(this.getString(columnIndex));
				this.goToRow(indexStore);
			}
			return ((String[]) ret.toArray(new String[ret.size()]));
		}
		else return null;
	}
//	public StringVector getColumnData(int columnIndex) {
//		if (this.success && (columnIndex >= 0) && (columnIndex < this.columnCount)) {
//			StringVector ret = new StringVector();
//			int indexStore = this.getCurrentRowIndex();
//			if (this.beforeFirst()) {
//				while (this.next()) {
//					ret.addElement(this.getString(columnIndex));
//				}
//				this.goToRow(indexStore);
//			}
//			return ret;
//		}
//		else return null;
//	}
	
	/**	get all entries of a resultArray record in a StringVector object
	 * @param	rowIndex	the index of the requested record
	 * @return	all fields of the index-th resultArray record in a StringVector object
	 */
	public String[] getRowData(int rowIndex) {
		if (this.success && (rowIndex >= 0) && (rowIndex < this.rowCount)) {
			String[] ret = new String[this.columnCount];
			for (int c = 0; c < this.columnCount; c++)
				ret[c] = this.getString(rowIndex, c);
			return ret;
		}
		else return null;
	}
//	public StringVector getRowData(int rowIndex) {
//		if (this.success && (rowIndex >= 0) && (rowIndex < this.rowCount)) {
//			StringVector ret = new StringVector();
//			for (int i = 0; i < this.columnCount; i++) {
//				ret.addElement(this.getString(rowIndex, i));
//			}
//			return ret;
//		}
//		else return null;
//	}
}
