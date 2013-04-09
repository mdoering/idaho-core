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


import java.sql.SQLException;
import java.util.Properties;

import de.uka.ipd.idaho.easyIO.sql.TableDefinition;

///**
// * An IoProvider provides a variety of IO functionality, configured to specific
// * conditions of the local environment:
// * <ul>
// * <li>File IO works relative to a specific folder if no absolute paths are
// * used. This facilitates for multiple components (who all have their own
// * IoProvider instance) to each have a data folder of their own, which they can
// * operate in without caring about conflicts with other components, e.g. file
// * naming collisions.</li>
// * <li>Web Access can be readily configured with proxy and proxy authentication
// * data, so components do not have to care about how exactly to access the web
// * in their actual environment.</li>
// * <li>Sending e-mail via SMTP just requires a subject line, a message text,
// * and a recipient's address. The IoProvider abstracts from how exactly the
// * message is sent to the recipient.</li>
// * <li>Database access via JDBC restricts to submitting queries. The IoProvider
// * takes care of how to access the database server, and which actual database to
// * use.</li>
// * </ul>
// * 
// * @author sautter
// */
/**
 * An IoProvider provides a variety of IO functionality, configured to specific
 * conditions of the local environment:
 * <ul>
 * <li>Web Access can be readily configured with proxy and proxy authentication
 * data, so components do not have to care about how exactly to access the web
 * in their actual environment.</li>
 * <li>Sending e-mail via SMTP just requires a subject line, a message text,
 * and a recipient's address. The IoProvider abstracts from how exactly the
 * message is sent to the recipient.</li>
 * <li>Database access via JDBC restricts to submitting queries. The IoProvider
 * takes care of how to access the database server, and which actual database to
 * use.</li>
 * </ul>
 * 
 * @author sautter
 */
public interface IoProvider {
//	
//	/**
//	 * Retrieve a connection to the centrally configured database.
//	 * @return a connection to the centrally configured database
//	 */
//	public abstract DbConnection getDbConnection() throws SQLException;
//	
//	/**
//	 * store the key / value pairs contained in a Setting (if a setting already
//	 * exists, the value is updated)
//	 * @param settings the Settings to be stored
//	 * @return true if and only if the Settings was stored successfully
//	 */
//	public abstract boolean storeSettings(Settings settings);
//
//	/**
//	 * store the key / value pairs contained in a Setting (if a setting already
//	 * exists, the value is updated)
//	 * @param settings the Settings to be stored
//	 * @param clear if set to true, all settings not contained in the specified
//	 *            Settings object will be deleted
//	 * @return true if and only if the Settings was stored successfully
//	 */
//	public abstract boolean storeSettings(Settings settings, boolean clear);
//
//	/**
//	 * store the key / value pairs contained in a Setting (if a setting already
//	 * exists, the value is updated)
//	 * @param ownerName the owner of the Settings (intended to store some
//	 *            Settings of a named component)
//	 * @param settings the Settings to be stored
//	 * @return true if and only if the Settings was stored successfully
//	 */
//	public abstract boolean storeSettings(String ownerName, Settings settings);
//
//	/**
//	 * store the key / value pairs contained in a Setting (if a setting already
//	 * exists, the value is updated)
//	 * @param ownerName the owner of the Settings (intended to store some
//	 *            Settings of a named component)
//	 * @param settings the Settings to be stored
//	 * @param clear if set to true, all settings not contained in the specified
//	 *            Settings object will be deleted
//	 * @return true if and only if the Settings was stored successfully
//	 */
//	public abstract boolean storeSettings(String ownerName, Settings settings, boolean clear);
//
//	/**
//	 * load global settings into a Settings object
//	 * @return a Settings object containing the desired settings
//	 */
//	public abstract Settings loadSettings();
//
//	/**
//	 * load a set of component specific settings into a Settings object
//	 * @param ownerName the name of the object owning the settings to be loaded
//	 * @return a Settings object containing the desired settings
//	 */
//	public abstract Settings loadSettings(String ownerName);

	/**
	 * Check if a table with the specified name exists in the centrally
	 * configured database.
	 * @param tableName the name of the table whose existance to check
	 * @return true if and only if a table with the specified name exists
	 */
	public abstract boolean ensureTable(String tableName);

	/**
	 * Check if a table with the specified name exists in the centrally
	 * configured database and complies with a given definition. This means that
	 * at least all required columns exist and have at least the required width.
	 * @param definition the definition of the table to validate, in its string
	 *            representation
	 * @param create if set to true, the questioned table will be created or
	 *            extended to comply with the specified definition (extensions
	 *            only)
	 * @return true if and only if a table with the specified name and
	 *         definition exists or was created or extended successfully
	 * @see de.uka.ipd.idaho.easyIO.sql.TableDefinition
	 */
	public abstract boolean ensureTable(String definition, boolean create);

//	/**
//	 * check if a table with the specified name exists and complies the
//	 * specified definition
//	 * @param definition the definition of the table to be validated
//	 * @param create if set to true, the questioned table will be created or
//	 *            adjusted to the specified definition (extensions only)
//	 * @return true if and only if a table with the specified name and
//	 *         definition exists or was created or adjusted successfully
//	 * @deprecated de.easyIO.util.TableDefinition and
//	 *             de.easyIO.util.TableColumnDefinition are bound to the SQL
//	 *             dialect by Microsoft. Use de.easyIO.sql.TableDefinition and
//	 *             de.easyIO.sql.TableColumnDefinition and the
//	 *             ensureTablePortable() method instead.
//	 */
//	public abstract boolean ensureTable(de.uka.ipd.idaho.easyIO.util.TableDefinition definition, boolean create);
	
	/**
	 * Check if a table with the specified name exists in the centrally
	 * configured database and complies with a given definition. This means that
	 * at least all required columns exist and have at least the required width.
	 * @param definition the definition of the table to validate
	 * @param create if set to true, the questioned table will be created or
	 *            extended to comply with the specified definition (extensions
	 *            only)
	 * @return true if and only if a table with the specified name and
	 *         definition exists or was created or extended successfully
	 * @see de.uka.ipd.idaho.easyIO.sql.TableDefinition
	 */
	public abstract boolean ensureTable(TableDefinition definition, boolean create);
	
	/**
	 * Declare a column of a database table the primary key, i.e., add a primary
	 * key constraint. The constraint is named
	 * '&lt;table&gt;_PK_&lt;column&gt;'. If the table or the specified column
	 * does not exist, this method will not add the primary key constraint and
	 * simply return false.
	 * @param table the name of the table
	 * @param column the name of the primary key column
	 * @return true if the primary key was created or existed before, false
	 *         otherwise
	 */
	public abstract boolean setPrimaryKey(String table, String column);
	
	/**
	 * Declare a column of a database table to be a foreign key referencing
	 * another column in another table, i.e., add a foreign key constraint. The
	 * constraint is named
	 * '&lt;table&gt;_&lt;column&gt;_FK_&lt;refTable&gt;_&lt;refColumn&gt;'. If
	 * the table or the specified column does not exist, or the referenced table
	 * or the referenced column does not exist, this method will not add the
	 * foreign key constraint and simply return false.
	 * @param table the name of the table
	 * @param column the name of the primary key column
	 * @return true if the primary key was created or existed before, false
	 *         otherwise
	 */
	public abstract boolean setForeignKey(String table, String column, String refTable, String refColumn);
	
	/**
	 * Create an index over a column of a database table. The index is named
	 * '&lt;table&gt;_index_&lt;column&gt;'. If the table or the specified
	 * column does not exist, this method will not create an index and simply
	 * return false.
	 * @param table the name of the table
	 * @param column the name of the column to index
	 * @return true if the index was created or existed before, false otherwise
	 */
	public abstract boolean indexColumn(String table, String column);
	
	/**
	 * Create an index over one or more columns of a database table. The index
	 * is named '&lt;table&gt;_index_&lt;column1&gt;_..._&lt;columnN&gt;'. If
	 * the table does not exist, the column array is empty, or one of the
	 * columns in the array does not exist, this method will not create an index
	 * and simply return false.
	 * @param table the name of the table
	 * @param columns the columns to index
	 * @return true if the index was created or existed before, false otherwise
	 */
	public abstract boolean indexColumns(String table, String[] columns);
	
//	/**
//	 * Generate a part for a CREATE TABLE query that creates a given column.
//	 * This method exists so the syntax can be varied depending on the database
//	 * system and its product specific features. The data type must be one of
//	 * the respective constants defined in the TableDefinition class. The length
//	 * is ignored for fixed-length data types.
//	 * @param colName the name of the column
//	 * @param dataType the data type
//	 * @param length the column length
//	 * @return a part for a CREATE TABLE query creating the column
//	 */
//	public abstract String getCreateTablePart(String colName, String dataType, int length);
//	
//	/**
//	 * Generate a part for an ALTER TABLE query that adds a given column. This
//	 * method exists so the syntax can be varied depending on the database
//	 * system and its product specific features. The data type must be one of
//	 * the respective constants defined in the TableDefinition class. The length
//	 * is ignored for fixed-length data types.
//	 * @param colName the name of the column
//	 * @param dataType the data type
//	 * @param length the column length
//	 * @return a part for an ALTER TABLE query adding the column
//	 */
//	public abstract String getAddColumnPart(String colName, String dataType, int length);
//	
//	/**
//	 * Generate a part for an ALTER TABLE query that widens a given column. This
//	 * method exists so the syntax can be varied depending on the database
//	 * system and its product specific features. The data type must be one of
//	 * the respective constants defined in the TableDefinition class. The length
//	 * is ignored for fixed-length data types.
//	 * @param colName the name of the column
//	 * @param dataType the data type
//	 * @param length the column length
//	 * @return a part for an ALTER TABLE query widening the column
//	 */
//	public abstract String getWidenColumnPart(String colName, String dataType, int length);
//	
//	public abstract String getCreateIndexPart(String colName, String indexType, String params);// (indexName = colName+indexType+'Index');
//	public abstract String getDropIndexPart(String colName, String indexType);// (indexName = colName+indexType+'Index');
		
	/**
	 * Execute a select query on the centrally configured database.
	 * @param query the query to execute
	 * @param copy copy the result of the query to the result object (faster in
	 *            iteration, but memory-intensive for large results)
	 * @return the result of query
	 * @throws SQLException
	 */
	public abstract SqlQueryResult executeSelectQuery(String query, boolean copy) throws SQLException;

	/**
	 * Execute a select query on the centrally configured database.
	 * @param query the query to execute
	 * @return the result of query
	 * @throws SQLException
	 */
	public abstract SqlQueryResult executeSelectQuery(String query) throws SQLException;

	/**
	 * Execute an update query on the centrally configured database.
	 * @param query the query to execute
	 * @return the return state of the query execution
	 * @throws SQLException
	 */
	public abstract int executeUpdateQuery(String query) throws SQLException;

	/**
	 * Test whether there is a centrally configured database to work with. If
	 * this method returns false, all JDBC related method calls are likely to
	 * throw exceptions.
	 * @return true if and only if this IoProvider allows database access
	 */
	public abstract boolean isJdbcAvailable();
	
	/**
	 * Retrieve meta data on the wrapped JDBC connection. If the
	 * isJdbcAvailable() method returns false, the return value of this method
	 * is undefined.
	 * @return a properties object holding the JDBC meta data as key/value pairs
	 */
	public abstract Properties getJdbcMetaData();
	
	/**
	 * Close the IoProvider, i.e., cloase all embedded connections. After
	 * calling this method, an IoProvider should not be used again.
	 */
	public abstract void close();
//	/**
//	 * read a text file
//	 * @param fileName the path and name of the file to be read, under this
//	 *            StandardIoProvider's default directory
//	 * @return the content of the file addressed by fileName
//	 * @throws FileNotFoundException
//	 * @throws IOException
//	 */
//	public abstract String readFile(String fileName) throws FileNotFoundException, IOException;
//
//	/**
//	 * read a text file
//	 * @param file the file to be read
//	 * @return the content of the specified File
//	 * @throws FileNotFoundException
//	 * @throws IOException
//	 */
//	public abstract String readFile(File file) throws FileNotFoundException, IOException;
//
//	/**
//	 * write content to the end of an existing file, create file if non-existant
//	 * @param fileName the path and name of the file to write to, under this
//	 *            StandardIoProvider's default directory
//	 * @param content the String to be written to the end of the specified file
//	 * @throws IOException
//	 */
//	public abstract void writeFile(String fileName, String content) throws IOException;
//
//	/**
//	 * write content to the end of an existing file, create file if non-existant
//	 * @param file the File to write to
//	 * @param content the String(s) to be written to the end of the specified
//	 *            file
//	 * @throws IOException
//	 */
//	public abstract void writeFile(File file, String content) throws IOException;
//
//	/**
//	 * write content to the end of an existing file, create file if non-existant
//	 * @param fileName the path and name of the file to write to, under this
//	 *            StandardIoProvider's default directory
//	 * @param content the String(s) to be written to the end of the specified
//	 *            file
//	 * @throws IOException
//	 */
//	public abstract void writeFile(String fileName, StringVector content) throws IOException;
//
//	/**
//	 * write content to the end of an existing file, create file if non-existant
//	 * @param file the File to write to
//	 * @param content the String(s) to be written to the end of the specified
//	 *            file
//	 * @throws IOException
//	 */
//	public abstract void writeFile(File file, StringVector content) throws IOException;
//
//	/**
//	 * write content to the end of an existing file
//	 * @param fileName the path and name of the file to write to, under this
//	 *            StandardIoProvider's default directory
//	 * @param content the String to be written to the end of the specified file
//	 * @throws IOException
//	 */
//	public abstract void writeToFile(String fileName, String content) throws IOException;
//
//	/**
//	 * write content to the end of an existing file
//	 * @param file the File to write to
//	 * @param content the String to be written to the end of the specified file
//	 * @throws IOException
//	 */
//	public abstract void writeToFile(File file, String content) throws IOException;
//
//	/**
//	 * obtain a File by its name. If the file does not exist, it is created. If
//	 * the specified file name denotes a relative path, the returned file is
//	 * relative to this IoProvider's data base path.
//	 * @param fileName the path and name of the desired file
//	 * @return the File denoted by the specified file name and, if the latter is
//	 *         relative, by this IoProvider's data base path
//	 * @throws IOException
//	 */
//	public abstract File getFile(String fileName) throws IOException;
//
//	/**
//	 * @return the File denoting the base directory for this IoProvider's file
//	 *         IO
//	 */
//	public abstract File getHomeDirectory();
//
//	/**
//	 * change the base directory for this IoProvider's file IO (use with care)
//	 * @param homeDirectory the new base directory for this IoProvider's file IO
//	 */
//	public abstract void setHomeDirectory(File homeDirectory);
//
//	/**
//	 * load a page form the WWW
//	 * @param url the address to read
//	 * @return the page as a String
//	 * @throws MalformedURLException
//	 * @throws IOException
//	 */
//	public abstract String getPage(String url) throws MalformedURLException, IOException;

	/**
	 * Send out an eMail message via SMTP to the centrally configured server and
	 * using the centrally configured credentials.
	 * @param subject the subject line for the eMail
	 * @param message the text of the eMail
	 * @param toAddresses an array holding the addresses to send the eMail to
	 * @throws Exception
	 */
	public abstract void smtpSend(String subject, String message, String[] toAddresses) throws Exception;

	/**
	 * Test whether or not sending eMail through this IO Provider is possible,
	 * i.e., whether there is a centrally configured mail server and account. If
	 * this method returns false, smtpSend() is likely to throw exceptions.
	 * @return true if and only if this IO Provider allows sending eMail
	 */
	public abstract boolean isMessagingAvailable();
}
