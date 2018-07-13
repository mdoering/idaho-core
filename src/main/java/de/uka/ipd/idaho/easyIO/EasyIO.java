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


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.easyIO.sql.TableDefinition;

///**
// * Utility library for multiple IO purposes. In particular, this library can:
// * <ul>
// * <li>read, write, and rename files</li>
// * <li>download web pages</li>
// * <li>escape and un-escape string values with arbitrary escape and to-escape
// * characters</li>
// * <li>prepare strings for being used in SQL queries</li>
// * <li>produce IoProvider objects</li>
// * <li>send e-mails via SMTP (this is why this class depends on mail.jar to be
// * present on the class path)</li>
// * </ul>
// * 
// * @author Guido Sautter
// */
/**
 * Utility library for various IO purposes. In particular, this library can:
 * <ul>
 * <li>read and write files</li>
 * <li>escape and un-escape string values with arbitrary escape and to-escape
 * characters</li>
 * <li>prepare strings for being used in SQL queries</li>
 * <li>produce IO Provider objects</li>
 * <li>send e-mails via SMTP (this is why this class depends on mail.jar to be
 * present on the class path)</li>
 * </ul>
 * 
 * @author Guido Sautter
 */
public class EasyIO {
	
	/**
	 * Create an IO Provider configured according to the specified settings. The
	 * implementation of the IO Provider interface uses the following settings:
	 * <ul>
	 * <li><b>Web IO:</b>
	 * <ul>
	 * <li><b>WWW.UseProxy</b>: use a proxy server for accessing the web?
	 * Specify 'YES' to indicate using a proxy server.</li>
	 * <li><b>WWW.Proxy</b>: the address of the proxy server to use.</li>
	 * <li><b>WWW.ProxyPort</b>: the port to access the proxy server through.</li>
	 * <li><b>WWW.UseProxyAuth</b>: authenticate with the proxy server? Specify
	 * 'YES' to indicate so.</li>
	 * <li><b>WWW.ProxyUser</b>: the user name to use for authenticationg with
	 * the proxy server.</li>
	 * <li><b>WWW.ProxyPassword</b>: the password to use for authenticationg
	 * with the proxy server.</li>
	 * </ul>
	 * </li>
	 * <li><b>Sending e-mail via SMTP:</b>
	 * <ul>
	 * <li><b>SMTP.Server</b>: the name of the SMTP server to use.</li>
	 * <li><b>SMTP.Port</b>: the port to access the SMTP server through.</li>
	 * <li><b>SMTP.Login</b>: the login name for the SMTP server.</li>
	 * <li><b>SMTP.Password</b>: the password for the SMTP server.</li>
	 * <li><b>SMTP.FromAddress</b>: the sender address for e-mails.</li>
	 * </ul>
	 * </li>
	 * <li><b>Database access via JDBC:</b>
	 * <ul>
	 * <li><b>JDBC.DriverClassName</b>: the class name of the JDBC driver to
	 * use.</li>
	 * <li><b>JDBC.DriverClassPath</b>: the path(s) and name(s) of the jar
	 * file(s) to load the JDBC driver from.</li>
	 * <li><b>JDBC.Driver</b>: the name of the JDBC driver to use, usually
	 * something like 'jdbc:...'</li>
	 * <li><b>JDBC.Host</b>: the name of the database server (in a machine
	 * sense)</li>
	 * <li><b>JDBC.Port</b>: the port to access the database server through</li>
	 * <li><b>JDBC.AuthMode</b>: how to authenticate with the database server?
	 * Specify 'HUP' to indicate using host-user-password authentication,
	 * otherwise a connection URL will be used.</li>
	 * <li><b>JDBC.User</b>: the user name to authenticate with the database
	 * server.</li>
	 * <li><b>JDBC.Password</b>: the password to authenticate with the database
	 * server.</li>
	 * <li><b>JDBC.DefaultDB</b>: the database to use initially. Can be changed
	 * via the SQL query 'USE &lt;DatabaseName&gt;'. The datbase specified here
	 * has to exist prior to the application to start. This is to facilitate
	 * running applications with authentication data that does not allow for
	 * creating new databases.</li>
	 * <li><b>JDBC.Url</b>: the URL to use for connecting to and authenticating
	 * with the database server. This is an alternative for host-user-password
	 * authentication and specifying a default DB.</li></li>
	 * <li><b>JDBC.TerminalSemicolon</b>: do SQL queries require a terminal
	 * semicolon for the database server to connect to? This setting is to
	 * simplify running the same application on multiple different database
	 * servers, which may differ in requiring, accepting, or denying SQL queries
	 * to be terminated with a semicolon. Depending on this setting, the IO
	 * provider makes sure every SQL query end with or does not end with a
	 * semicolon, respectively. The default is to ensure an ending semicolon, as
	 * this is most common. Specify 'NO' for this parameter to enforce SQL
	 * queries to not end with a semicolon.</li>
	 * </li>
	 * </ul>
	 * @param configuration the Properties object containing the settings
	 * @return an IO Provider configured according to the specified settings
	 */
	public static IoProvider getIoProvider(Settings configuration) {
		return ((configuration == null) ? null : new StandardIoProvider(configuration));
	}
	
	private static class StandardIoProvider implements IoProvider {
		
		private Settings configuration;
		
		private boolean wwwValid = false;
		private boolean smtpValid = false;
		private boolean jdbcValid = false;
		
//		//	settings for file io
//		private String fileHomeDir;
//	
		//	settings for www io
		private boolean wwwProxyNeeded;
		private String wwwProxy;
		private String wwwProxyPort;
		private boolean wwwProxyAuthNeeded;
		private String wwwProxyUser;
		private String wwwProxyPassword;
		
		//	settings for smtp io
		private String smtpServer;
		private int smtpPort;
		private String smtpLogin;
		private String smtpPassword;
		private String smtpFromAddress;
//		private boolean smtpServerIsLocal = false;
		
		//	settings for jdbc io
		private String jdbcDriverClassName;
		private String jdbcDriverClassPath = "";
		private String jdbcDriver;
		private String jdbcHost;
		private String jdbcPort;
		private boolean jdbcUseHostUserPassword;
		private String jdbcUser;
		private String jdbcPassword;
		private String jdbcDefaultDbSetting;
		private String jdbcUrl;
		private boolean jdbcTerminalSemicolon = true;
		
		//	local objects for instant use
		private Connection jdbcCon = null;
		
		//	jdbc syntax patterns
		private Properties jdbcSyntax = null;
		
		/**
		 * Constructor
		 * @param configuration the parameters for the StandardIoProvider,
		 *            contained in a Settings
		 */
		StandardIoProvider(Settings configuration) {
			
			//	get configuration
			this.configuration = configuration;
			
			//	initialize www io
			this.wwwProxyNeeded = "YES".equalsIgnoreCase(this.configuration.getSetting("WWW.UseProxy"));
			this.wwwProxy = this.configuration.getSetting("WWW.Proxy");
			this.wwwProxyPort = this.configuration.getSetting("WWW.ProxyPort");
			this.wwwProxyAuthNeeded = "YES".equalsIgnoreCase(this.configuration.getSetting("WWW.UseProxyAuth"));
			this.wwwProxyUser = this.configuration.getSetting("WWW.ProxyUser");
			this.wwwProxyPassword = this.configuration.getSetting("WWW.ProxyPassword");
			
			//	check if www is valid
			this.wwwValid = (!this.wwwProxyNeeded ||
							  ((this.wwwProxy != null) &&
							  (this.wwwProxyPort != null) &&
								(!this.wwwProxyAuthNeeded ||
								  ((this.wwwProxyUser != null) &&
								  (this.wwwProxyPassword != null)))));
			
			//	set proxy if needed
			if (this.wwwValid && this.wwwProxyNeeded) {
				System.getProperties().put("proxySet", "true");
				System.getProperties().put("proxyHost", this.wwwProxy);
				System.getProperties().put("proxyPort", this.wwwProxyPort);
				if (this.wwwProxyAuthNeeded) {
					//	sorry, proxy auth is not supported at the moment
				}
			}
			
			//	initialize smtp io
			this.smtpServer = this.configuration.getSetting("SMTP.Server");
			this.smtpPort = Integer.parseInt(this.configuration.getSetting("SMTP.Port", "-1"));
			this.smtpLogin = this.configuration.getSetting("SMTP.Login");
			this.smtpPassword = this.configuration.getSetting("SMTP.Password");
			this.smtpFromAddress = this.configuration.getSetting("SMTP.FromAddress");
			
			//	check if smtp is valid
//			this.smtpValid = ((this.smtpServer != null) &&
//							 (this.smtpPort != -1l) &&
//							 (this.smtpLogin != null) &&
//							 (this.smtpPassword != null) &&
//							 (this.smtpFromAddress != null) &&
//							 (this.smtpServerIsLocal || this.wwwValid));
			this.smtpValid = ((this.smtpServer != null) &&
					 (this.smtpPort != -1l) &&
					 (this.smtpFromAddress != null));
			
			//	initialize jdbc io
			this.jdbcDriverClassName = this.configuration.getSetting("JDBC.DriverClassName");
			this.jdbcDriverClassPath = this.configuration.getSetting("JDBC.DriverClassPath", "");
			this.jdbcDriver = this.configuration.getSetting("JDBC.Driver");
			this.jdbcHost = this.configuration.getSetting("JDBC.Host");
			this.jdbcPort = this.configuration.getSetting("JDBC.Port");
			this.jdbcUseHostUserPassword = "HUP".equalsIgnoreCase(this.configuration.getSetting("JDBC.AuthMode"));
			this.jdbcUser = this.configuration.getSetting("JDBC.User");
			this.jdbcPassword = this.configuration.getSetting("JDBC.Password");
			this.jdbcDefaultDbSetting = this.configuration.getSetting("JDBC.DefaultDB");
			this.jdbcUrl = this.configuration.getSetting("JDBC.Url");
			this.jdbcTerminalSemicolon = "YES".equals(this.configuration.getSetting("JDBC.TerminalSemicolon", "YES"));
			this.jdbcCon = this.getJdbcConnection();
			
			//	check if jdbc is valid
			this.jdbcValid = (this.jdbcCon != null);
			
			//	load product specific database features
			if (this.jdbcValid)
				this.jdbcSyntax = TableDefinition.loadJdbcSyntax(this.jdbcDriverClassName);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.easyIO.IoProvider#close()
		 */
		public void close() {
			if (this.jdbcCon != null) try {
				this.jdbcCon.close();
				this.jdbcCon = null;
			}
			catch (SQLException sqle) {
				System.out.println("StandardIoProvider: " + sqle.getMessage() + " while closing database connection.");
			}
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#finalize()
		 */
		protected void finalize() throws Throwable {
			this.close();
		}
		
		//	create a JDBC connection according to the settings
		private Connection getJdbcConnection() {
			try {
				String url;
				
				//	get DB specific libraries
				String[] driverClassPathParts = this.jdbcDriverClassPath.split("\\s");
				ArrayList urlList = new ArrayList();
				for (int p = 0; p < driverClassPathParts.length; p++) {
					if (driverClassPathParts[p].length() != 0) try {
						System.out.println("StandardIoProvider: adding '" + driverClassPathParts[p] + "' to class path for JDBC driver");
						if (driverClassPathParts[p].indexOf("://") == -1)
							urlList.add(new File(driverClassPathParts[p]).toURL());
						else urlList.add(new URL(driverClassPathParts[p]));
					} catch (Exception e) {}
				}
				
				//	load JDBC driver
				Class driverClass;
				if (urlList.isEmpty())
					driverClass = this.getClass().getClassLoader().loadClass(this.jdbcDriverClassName);
				else {
					URL[] urls = ((URL[]) urlList.toArray(new URL[urlList.size()]));
					ClassLoader driverClassLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
					driverClass = driverClassLoader.loadClass(this.jdbcDriverClassName);
				}
				System.out.println("StandardIoProvider: got JDBC driver class '" + this.jdbcDriverClassName + "'");
				
				//	wrap and register driver instance
				DriverManager.registerDriver(new GenericDriver((Driver) driverClass.newInstance()));
				System.out.println("StandardIoProvider: JDBC driver instance wrapped");
				
				//	using user/password authentication
				if (this.jdbcUseHostUserPassword) {
					
					//	connection url given --> simply use it
					if (this.jdbcUrl != null)
						url = this.jdbcUrl;
					
					//	connection url not given --> assemble it
					else {
						url = this.jdbcDriver + ":" + this.jdbcHost;
						
						if (this.jdbcPort != null)
							url = url + ":" + this.jdbcPort;
						
						if (this.jdbcDefaultDbSetting != null)
							url = url + this.jdbcDefaultDbSetting;
					}
					
					//	get connection and return it
					Connection con = DriverManager.getConnection(url, this.jdbcUser, this.jdbcPassword);
					return con;
				}
				
				//	using url authentication
				else {
					
					//	connection url given --> simply use it
					if (this.jdbcUrl != null)
						url = this.jdbcUrl;
						
					//	connection url not given --> assemble it
					else {
						url = this.jdbcDriver + ":" + this.jdbcHost;
						if (this.jdbcPort != null)
							url = url + ":" + this.jdbcPort;
						
						if (this.jdbcDefaultDbSetting != null)
							url = url + this.jdbcDefaultDbSetting;
						
						url = url + "?" + this.jdbcUser;
						
						if (this.jdbcPassword != null)
							url = url + "&" + this.jdbcPassword;
					}
					
					//	get connection and return it
					Connection con = DriverManager.getConnection(url);
					return con;
				}
			}
			
			//	catch exception caused by invalid settings
			catch (SQLException sql) {
				System.out.println("StandardIoProvider: " + sql.getClass().getName() + " (" + sql.getMessage() + ") while creating JDBC connection.");
				sql.printStackTrace(System.out);
				return null;
			}
			
			//	catch exception caused by missing settings
			catch (NullPointerException npe) {
				System.out.println("StandardIoProvider: " + npe.getClass().getName() + " (" + npe.getMessage() + ") while creating JDBC connection.");
				npe.printStackTrace(System.out);
				return null;
			}
			
			//	catch other possible exceptions
			catch (Exception e) {
				System.out.println("StandardIoProvider: " + e.getClass().getName() + " (" + e.getMessage() + ") while creating JDBC connection.");
				e.printStackTrace(System.out);
				return null;
			}
		}
		
		/**
		 * Wrapper for JDBC drivers that are loaded at runtime (DriverManager
		 * requires drivers to be loaded through system classloader for some
		 * reason)
		 * 
		 * @author sautter
		 */
		private class GenericDriver implements Driver {
			private Driver driver;
			private GenericDriver(Driver driver) {
				this.driver = driver;
			}
			public boolean acceptsURL(String url) throws SQLException {
				return this.driver.acceptsURL(url);
			}
			public Connection connect(String url, Properties properties) throws SQLException {
				return this.driver.connect(url, properties);
			}
			public int getMajorVersion() {
				return this.driver.getMajorVersion();
			}
			public int getMinorVersion() {
				return this.driver.getMinorVersion();
			}
			public DriverPropertyInfo[] getPropertyInfo(String url, Properties properties) throws SQLException {
				return this.driver.getPropertyInfo(url, properties);
			}
			public boolean jdbcCompliant() {
				return this.driver.jdbcCompliant();
			}

			public Logger getParentLogger() throws SQLFeatureNotSupportedException {
				throw new SQLFeatureNotSupportedException("");
			}
		}
		
		/**	check if a table with the specified name exists
		 * @param	tableName	the name of the table that's existance is to be checked
		 * @return	true if and only if a table with the specified name exists
		 */
		public boolean ensureTable(String tableName) {
			if (this.jdbcValid) {
				try {
					this.executeSelectQuery("SELECT * FROM " + tableName + " WHERE 1=0");
					return true;
				}
				catch (SQLException sqle) {
					return false;
				}
			}
			else return false;
		}
		
		/**	check if a table with the specified name exists and complies the specified definition
		 * @param	definition	the definition of the table to be validated (@see de.easyIO.TableDefinition for details)
		 * @param 	create		if set to true, the questioned table will be created or adjusted to the specified definition (extensions only)
		 * @return	true if and only if a table with the specified name and definition exists or was created or adjusted successfully
		 */
		public boolean ensureTable(String definition, boolean create) {
			return this.ensureTable((new TableDefinition(definition)), create);
		}
		
		/**	check if a table with the specified name exists and complies the specified definition
		 * @param	definition	the definition of the table to be validated
		 * @param 	create		if set to true, the questioned table will be created or adjusted to the specified definition (extensions only)
		 * @return	true if and only if a table with the specified name and definition exists or was created or adjusted successfully
		 */
		public boolean ensureTable(TableDefinition definition, boolean create) {
			if (!this.jdbcValid || (definition == null)) {
				System.out.println("StandardIoProvider: JDBC invalid or TableDefinition was null.");
				return false;
			}
			
			/* TODO create syntax resource files (as in StringUtils for postscript):
			 * - one for each supported database
			 * - content:
			 *   - mapping of generic data types to database specific ones
			 * ==> all data types become available
			 */
			
			try {
				
				//	get old definition from DB
				String columnValidationQuery = definition.getColumnValidationQuery();
				if (columnValidationQuery == null) {
					System.out.println("StandardIoProvider: empty table definition.");
					return false;
				}
				System.out.println("StandardIoProvider: ensuring table columns in " + definition.getTableName() + "\n  " + columnValidationQuery);
				SqlQueryResult sqr = this.executeSelectQuery(columnValidationQuery, true);
				System.out.println("StandardIoProvider: column validation query successful.");
				TableDefinition existingDefinition = new TableDefinition(sqr, definition.getTableName());
				
				//	check for updates
				if (create) {
					try {
						
						//	get and execute update queries
						String[] updates = definition.getUpdateQueries(existingDefinition, this.jdbcSyntax);
						for (int u = 0; u < updates.length; u++) {
							System.out.println("StandardIoProvider: altering column\n  " + updates[u]);
							this.executeUpdateQuery(updates[u]);
						}
//						StringVector updates = definition.getUpdateQueries(existingDefinition, this.jdbcSyntax);
//						for (int i = 0; i < updates.size(); i++) {
//							System.out.println("StandardIoProvider: altering column\n  " + updates.get(i));
//							this.executeUpdateQuery(updates.get(i));
//						}
						return true;
					}
					catch (SQLException updateSqlEx) {
						System.out.println("StandardIoProvider: " + updateSqlEx.getMessage() + " while updating table.");
						return false;
					}
				}
				
				//	check for equality if update not allowed
				else return existingDefinition.equals(definition);
			}
			
			//	at least one column missing
			catch (SQLException columnSqlEx) {
				System.out.println("StandardIoProvider: caught " + columnSqlEx.getMessage() + " while ensuring table, some column is missing.");
				try {
					
					//	get old definition from DB
					String validationQuery = definition.getValidationQuery();
					System.out.println("StandardIoProvider: ensuring table " + definition.getTableName() + "\n  " + validationQuery);
					SqlQueryResult sqr = this.executeSelectQuery(validationQuery, true);
					System.out.println("StandardIoProvider: validation query successful.");
					TableDefinition existingDefinition = new TableDefinition(sqr, definition.getTableName());
					
					//	check for updates
					if (create) {
						String updateQuery = "";
						try {
							
							//	get and execute update queries
							String[] updates = definition.getUpdateQueries(existingDefinition, this.jdbcSyntax);
							for (int u = 0; u < updates.length; u++) {
								updateQuery = updates[u];
								System.out.println("StandardIoProvider: creating or altering column\n  " + updates[u]);
								this.executeUpdateQuery(updates[u]);
							}
//							StringVector updates = definition.getUpdateQueries(existingDefinition, this.jdbcSyntax);
//							for (int i = 0; i < updates.size(); i++) {
//								updateQuery = updates.get(i);
//								System.out.println("StandardIoProvider: creating or altering column\n  " + updates.get(i));
//								this.executeUpdateQuery(updates.get(i));
//							}
							return true;
						}
						catch (SQLException updateSqlEx) {
							System.out.println("StandardIoProvider: " + updateSqlEx.getMessage() + " while extending / updating table.\n  Query was " + updateQuery);
							return false;
						}
					}
					
					//	check for equality if update not allowed
					else return existingDefinition.equals(definition);
				}
				
				//	table doesn't exist at all
				catch (SQLException tableSqlEx) {
					System.out.println("StandardIoProvider: caught " + tableSqlEx.getMessage() + " while ensuring table, table doesn't exist.");
					String creationQuery = "";
					try {
						
						//	create table if allowed
						if (create) {
							creationQuery = definition.getCreationQuery(this.jdbcSyntax);
							System.out.println("StandardIoProvider: creating table\n  " + creationQuery);
							this.executeUpdateQuery(creationQuery);
							return true;
						}
						else return false;
					}
					
					//	exception while creating table
					catch (SQLException createSqlEx) {
						System.out.println("StandardIoProvider: " + createSqlEx.getMessage() + " while creating table.\n  Query was " + creationQuery);
						return false;
					}
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.easyIO.IoProvider#setPrimaryKey(java.lang.String, java.lang.String)
		 */
		public boolean setPrimaryKey(String table, String column) {
			
			//	check if table and column exist
			String checkQuery = ("SELECT " + column + " FROM " + table + " WHERE 1=0;");
			try {
				System.out.println("StandardIoProvider: checking table for primary key constraint creation.\n  " + checkQuery.toString());
				this.executeSelectQuery(checkQuery.toString());
			}
			
			//	exception while checking table
			catch (SQLException checkSqlEx) {
				System.out.println("StandardIoProvider: " + checkSqlEx.getMessage() + " while checking table for primary key constraint creation.\n  Query was " + checkQuery);
				return false;
			}
			
			//	create primary key constraint
			String constraintName = (table + "_PK_" + column);
			String createQuery = this.jdbcSyntax.getProperty(TableDefinition.SYNTAX_CREATE_PRIMARY_KEY);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_KEY_CONSTRAINT_NAME_VARIABLE, constraintName);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_KEYED_TABLE_VARIABLE, table);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_KEYED_COLUMN_VARIABLE, column);
			try {
				System.out.println("StandardIoProvider: creating primary key constraint\n  " + createQuery);
				this.executeUpdateQuery(createQuery);
				return true;
			}
			
			//	exception while creating index
			catch (SQLException createSqlEx) {
				
				//	catch case of index already existing, and return true on respective exceptions
				if (this.isAlreadyExistsErrorMessage(createSqlEx.getMessage(), "constraint", constraintName))
				
				//	other error
				System.out.println("StandardIoProvider: " + createSqlEx.getMessage() + " while creating primary key constraint.\n  Query was " + createQuery);
				return false;
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.easyIO.IoProvider#setForeignKey(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
		 */
		public boolean setForeignKey(String table, String column, String refTable, String refColumn) {
			
			//	check if table and column exist
			String checkQuery = ("SELECT " + column + " FROM " + table + " WHERE 1=0;");
			try {
				System.out.println("StandardIoProvider: checking table for constraint creation.\n  " + checkQuery.toString());
				this.executeSelectQuery(checkQuery.toString());
			}
			
			//	exception while checking table
			catch (SQLException checkSqlEx) {
				System.out.println("StandardIoProvider: " + checkSqlEx.getMessage() + " while checking table for primary key constraint creation.\n  Query was " + checkQuery);
				return false;
			}
			
			//	check if referenced table and column exist
			String refCheckQuery = ("SELECT " + refColumn + " FROM " + refTable + " WHERE 1=0;");
			try {
				System.out.println("StandardIoProvider: checking referenced table for foreign key constraint creation.\n  " + refCheckQuery.toString());
				this.executeSelectQuery(refCheckQuery.toString());
			}
			
			//	exception while checking table
			catch (SQLException checkSqlEx) {
				System.out.println("StandardIoProvider: " + checkSqlEx.getMessage() + " while checking referenced table for foreign key constraint creation.\n  Query was " + refCheckQuery);
				return false;
			}
			
			//	create foreign key constraint
			String constraintName = (table + "_" + column + "_FK_" + refTable + "_" + refColumn);
			String createQuery = this.jdbcSyntax.getProperty(TableDefinition.SYNTAX_CREATE_FOREIGN_KEY);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_KEY_CONSTRAINT_NAME_VARIABLE, constraintName);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_KEYED_TABLE_VARIABLE, table);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_KEYED_COLUMN_VARIABLE, column);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_REFERENCED_TABLE_VARIABLE, refTable);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_REFERENCED_COLUMN_VARIABLE, refColumn);
			try {
				System.out.println("StandardIoProvider: creating foreign key constraint\n  " + createQuery);
				this.executeUpdateQuery(createQuery);
				return true;
			}
			
			//	exception while creating index
			catch (SQLException createSqlEx) {
				
				//	catch case of index already existing, and return true on respective exceptions
				if (this.isAlreadyExistsErrorMessage(createSqlEx.getMessage(), "constraint", constraintName))
				
				//	other error
				System.out.println("StandardIoProvider: " + createSqlEx.getMessage() + " while creating foreign key constraint.\n  Query was " + createQuery);
				return false;
			}
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.easyIO.IoProvider#indexColumn(java.lang.String, java.lang.String)
		 */
		public boolean indexColumn(String table, String column) {
			String[] columns = {column};
			return this.indexColumns(table, columns);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.easyIO.IoProvider#indexColumns(java.lang.String, java.lang.String[])
		 */
		public boolean indexColumns(String table, String[] columns) {
			
			//	check arguments
			if ((table == null) || (table.trim().length() == 0) || (columns == null) || (columns.length == 0))
				return false;
			
			//	check if columns valid
			LinkedHashSet cols = new LinkedHashSet(columns.length);
			for (int c = 0; c < columns.length; c++) {
				if ((columns[c] != null) && (columns[c].trim().length() != 0))
					cols.add(columns[c].trim());
			}
			columns = ((String[]) cols.toArray(new String[cols.size()]));
			
			//	check if table and columns exist
			StringBuffer checkQuery = new StringBuffer("SELECT ");
			for (int c = 0; c < columns.length; c++) {
				if (c != 0)
					checkQuery.append(", ");
				checkQuery.append(columns[c]);
			}
			checkQuery.append(" FROM " + table + " WHERE 1=0;");
			try {
				System.out.println("StandardIoProvider: checking table for index creation.\n  " + checkQuery.toString());
				this.executeSelectQuery(checkQuery.toString());
			}
			
			//	exception while checking table
			catch (SQLException checkSqlEx) {
				System.out.println("StandardIoProvider: " + checkSqlEx.getMessage() + " while checking table for index creation.\n  Query was " + checkQuery);
				return false;
			}
			
			//	create index
			StringBuffer indexName = new StringBuffer(table + "_index");
			StringBuffer indexColumns = new StringBuffer();
			for (int c = 0; c < columns.length; c++) {
				indexName.append("_" + columns[c]);
				if (c != 0)
					indexColumns.append(", ");
				indexColumns.append(columns[c]);
			}
			String createQuery = this.jdbcSyntax.getProperty(TableDefinition.SYNTAX_CREATE_INDEX);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_INDEX_NAME_VARIABLE, indexName.toString());
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_INDEXED_TABLE_VARIABLE, table);
			createQuery = TableDefinition.replaceVariable(createQuery, TableDefinition.SYNTAX_INDEXED_COLUMNS_VARIABLE, indexColumns.toString());
			try {
				System.out.println("StandardIoProvider: creating index\n  " + createQuery);
				this.executeUpdateQuery(createQuery);
				return true;
			}
			
			//	exception while creating index
			catch (SQLException createSqlEx) {
				
				//	catch case of index already existing, and return true on respective exceptions
				if (this.isAlreadyExistsErrorMessage(createSqlEx.getMessage(), "index", indexName.toString()))
					return true;
				
				//	other error
				System.out.println("StandardIoProvider: " + createSqlEx.getMessage() + " while creating index.\n  Query was " + createQuery);
				return false;
			}
		}
		
		private boolean isAlreadyExistsErrorMessage(String cseMessage, String constructType, String constructName) {
			//	TODO figure out how to make this more generic, or base it on some pattern in syntax file
			
			if (cseMessage == null)
				return false;
			cseMessage = cseMessage.toLowerCase();
			if ((cseMessage.indexOf(constructName.toLowerCase()) == -1) && (cseMessage.indexOf(constructType) == -1))
				return false;
			
			if ((cseMessage.indexOf("already") != -1) && (cseMessage.indexOf("exists") != -1))
				return true;
			if ((cseMessage.indexOf("bereits") != -1) && (cseMessage.indexOf("vorhanden") != -1))
				return true;
			if ((cseMessage.indexOf("existiert") != -1) && (cseMessage.indexOf("bereits") != -1))
				return true;
			
			return false;
		}
		
//		public boolean ensureTable(TableDefinition definition, boolean create) {
//			if (!this.jdbcValid || (definition == null)) {
//				System.out.println("StandardIoProvider: JDBC invalid or TableDefinition was null.");
//				return false;
//			}
//			
//			try {
//				
//				//	get old definition from DB
//				String validationQuery = definition.getColumnValidationQuery();
//				System.out.println("StandardIoProvider: ensuring table columns in " + definition.getTableName() + "\n  " + validationQuery);
//				SqlQueryResult sqr = this.executeSelectQuery(validationQuery);
//				System.out.println("StandardIoProvider: column validation query successful.");
//				TableDefinition existingDefinition = new TableDefinition(sqr, definition.getTableName());
//				
//				//	check for updates
//				if (create) {
//					try {
//						
//						//	get and execute update queries
//						StringVector updates = definition.getUpdateQueries(existingDefinition);
//						for (int i = 0; i < updates.size(); i++) {
//							System.out.println("StandardIoProvider: altering column\n  " + updates.get(i));
//							this.executeUpdateQuery(updates.get(i));
//						}
//						return true;
//					}
//					catch (SQLException updateSqlEx) {
//						System.out.println("StandardIoProvider: " + updateSqlEx.getMessage() + " while updating table.");
//						return false;
//					}
//				}
//				
//				//	check for equality if update not allowed
//				else return existingDefinition.equals(definition);
//			}
//			
//			//	at least one column missing
//			catch (SQLException columnSqlEx) {
//				System.out.println("StandardIoProvider: caught " + columnSqlEx.getMessage() + " while ensuring table, some column is missing.");
//				try {
//					
//					//	get old definition from DB
//					String validationQuery = definition.getValidationQuery();
//					System.out.println("StandardIoProvider: ensuring table " + definition.getTableName() + "\n  " + validationQuery);
//					SqlQueryResult sqr = this.executeSelectQuery(validationQuery);
//					System.out.println("StandardIoProvider: validation query successful.");
//					TableDefinition existingDefinition = new TableDefinition(sqr, definition.getTableName());
//					
//					//	check for updates
//					if (create) {
//						String updateQuery = "";
//						try {
//							
//							//	get and execute update queries
//							StringVector updates = definition.getUpdateQueries(existingDefinition);
//							for (int i = 0; i < updates.size(); i++) {
//								updateQuery = updates.get(i);
//								System.out.println("StandardIoProvider: creating or altering column\n  " + updates.get(i));
//								this.executeUpdateQuery(updates.get(i));
//							}
//							return true;
//						}
//						catch (SQLException updateSqlEx) {
//							System.out.println("StandardIoProvider: " + updateSqlEx.getMessage() + " while extending / updating table.\n  Query was " + updateQuery);
//							return false;
//						}
//					}
//					
//					//	check for equality if update not allowed
//					else return existingDefinition.equals(definition);
//				}
//				
//				//	table doesn't exist at all
//				catch (SQLException tableSqlEx) {
//					System.out.println("StandardIoProvider: caught " + tableSqlEx.getMessage() + " while ensuring table, table doesn't exist.");
//					String creationQuery = "";
//					try {
//						
//						//	create table if allowed
//						if (create) {
//							creationQuery = definition.getCreationQuery();
//							System.out.println("StandardIoProvider: creating table\n  " + creationQuery);
//							this.executeUpdateQuery(creationQuery);
//							return true;
//						}
//						else return false;
//					}
//					
//					//	exception while creating table
//					catch (SQLException createSqlEx) {
//						System.out.println("StandardIoProvider: " + createSqlEx.getMessage() + " while creating table.\n  Query was " + creationQuery);
//						return false;
//					}
//				}
//			}
//		}
//		
//		/**	check if a table with the specified name exists and complies the specified definition
//		 * @param	definition	the definition of the table to be validated
//		 * @param 	create		if set to true, the questioned table will be created or adjusted to the specified definition (extensions only)
//		 * @return	true if and only if a table with the specified name and definition exists or was created or adjusted successfully
//		 * @deprecated de.easyIO.util.TableDefinition and de.easyIO.util.TableColumnDefinition are bound to the SQL dialect by Microsoft. Use de.uka.ipd.idaho.easyIO.sql.TableDefinition and de.uka.ipd.idaho.easyIO.sql.TableColumnDefinition and the ensureTablePortable() method instead. 
//		 */
//		public boolean ensureTable(de.uka.ipd.idaho.easyIO.util.TableDefinition definition, boolean create) {
//			if (this.jdbcValid && (definition != null)) {
//				try {
//					
//					//	get old definition from DB
//					String validationQuery = definition.getColumnValidationQuery();
//					System.out.println("StandardIoProvider: ensuring table columns in " + definition.getTableName() + "\n  " + validationQuery);
//					SqlQueryResult sqr = this.executeSelectQuery(validationQuery);
//					System.out.println("StandardIoProvider: column validation query successful.");
//					de.uka.ipd.idaho.easyIO.util.TableDefinition existingDefinition = new de.uka.ipd.idaho.easyIO.util.TableDefinition(sqr, definition.getTableName());
//					
//					//	check for updates
//					if (create) {
//						try {
//							
//							//	get and execute update queries
//							StringVector updates = definition.getUpdateQueries(existingDefinition);
//							for (int i = 0; i < updates.size(); i++) {
//								System.out.println("StandardIoProvider: altering column\n  " + updates.get(i));
//								this.executeUpdateQuery(updates.get(i));
//							}
//							return true;
//						}
//						catch (SQLException updateSqlEx) {
//							System.out.println("StandardIoProvider: " + updateSqlEx.getMessage() + " while updating table.");
//							return false;
//						}
//					}
//					
//					//	check for equality if update not allowed
//					else return existingDefinition.equals(definition);
//				}
//				
//				//	at least one column missing
//				catch (SQLException columnSqlEx) {
//					System.out.println("StandardIoProvider: caught " + columnSqlEx.getMessage() + " while ensuring table, some column is missing.");
//					try {
//						
//						//	get old definition from DB
//						String validationQuery = definition.getValidationQuery();
//						System.out.println("StandardIoProvider: ensuring table " + definition.getTableName() + "\n  " + validationQuery);
//						SqlQueryResult sqr = this.executeSelectQuery(validationQuery);
//						System.out.println("StandardIoProvider: validation query successful.");
//						de.uka.ipd.idaho.easyIO.util.TableDefinition existingDefinition = new de.uka.ipd.idaho.easyIO.util.TableDefinition(sqr, definition.getTableName());
//						
//						//	check for updates
//						if (create) {
//							String updateQuery = "";
//							try {
//								
//								//	get and execute update queries
//								StringVector updates = definition.getUpdateQueries(existingDefinition);
//								for (int i = 0; i < updates.size(); i++) {
//									updateQuery = updates.get(i);
//									System.out.println("StandardIoProvider: creationg or altering column\n  " + updates.get(i));
//									this.executeUpdateQuery(updates.get(i));
//								}
//								return true;
//							}
//							catch (SQLException updateSqlEx) {
//								System.out.println("StandardIoProvider: " + updateSqlEx.getMessage() + " while extending / updating table.\n  Query was " + updateQuery);
//								return false;
//							}
//						}
//						
//						//	check for equality if update not allowed
//						else return existingDefinition.equals(definition);
//					}
//					
//					//	table doesn't exist at all
//					catch (SQLException tableSqlEx) {
//						System.out.println("StandardIoProvider: caught " + tableSqlEx.getMessage() + " while ensuring table, table doesn't exist.");
//						String creationQuery = "";
//						try {
//							
//							//	create table if allowed
//							if (create) {
//								creationQuery = definition.getCreationQuery();
//								System.out.println("StandardIoProvider: creation table\n  " + creationQuery);
//								this.executeUpdateQuery(creationQuery);
//								return true;
//							}
//							else return false;
//						}
//						
//						//	exception while creating table
//						catch (SQLException createSqlEx) {
//							System.out.println("StandardIoProvider: " + createSqlEx.getMessage() + " while creating table.\n  Query was " + creationQuery);
//							return false;
//						}
//					}
//				}
//			}
//			else {
//				System.out.println("StandardIoProvider: JDBC invalid or TableDefinition was null.");
//				return false;
//			}
//		}
	//	
		/**	execute a select query
		 * @param	query	the query to be executed	
		 * @param	copy	copy the result of the query to the result object
		 * 					(faster in iteration, but memory-intensive for large results)
		 * @return	the result of query
		 * @throws SQLException
		 */
		public SqlQueryResult executeSelectQuery(String query, boolean copy) throws SQLException {
			if (this.jdbcValid && (query != null) && query.toLowerCase().startsWith("select")) {
				Statement st = null;
				SqlQueryResult sqr = null;
				try {
					st = (copy ? this.jdbcCon.createStatement() : this.jdbcCon.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY));
					sqr = new SqlQueryResult(query, st.executeQuery(this.prepareQuery(query)), copy);
					return sqr;
				}
				finally {
					if ((copy || (sqr == null)) && (st != null))
						st.close();
				}
			}
			else return null;
		}
		
		/**	execute a select query over the default JDBC connection
		 * @param	query	the query to be executed
		 * @return	the result of query
		 * @throws SQLException
		 */
		public SqlQueryResult executeSelectQuery(String query) throws SQLException {
			return this.executeSelectQuery(query, false);
		}
		
		/**	execute an update query over the default JDBC connection
		 * @param	query	the query to be executed	
		 * @return	the return state of the query execution
		 * @throws SQLException
		 */
		public int executeUpdateQuery(String query) throws SQLException {
			if (this.jdbcValid && (query != null)) {
				Statement st = null;
				try {
					st = this.jdbcCon.createStatement();
					return st.executeUpdate(this.prepareQuery(query));
				}
				finally {
					if (st != null)
						st.close();
				}
			}
			else return 0;
		}
		
		private final String prepareQuery(String query) {
			if (query.endsWith(";"))
				return (this.jdbcTerminalSemicolon ? query : query.substring(0, (query.length() - 1)));
			else return (this.jdbcTerminalSemicolon ? (query + ";") : query);
		}
		
		/** @see de.uka.ipd.idaho.easyIO.IoProvider#isJdbcAvailable()
		 */
		public boolean isJdbcAvailable() {
			return this.jdbcValid;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.easyIO.IoProvider#getJdbcMetaData()
		 */
		public Properties getJdbcMetaData() {
			if (!this.isJdbcAvailable())
				return null;
			Properties jmd = new Properties();
			try {
				DatabaseMetaData dmd = this.jdbcCon.getMetaData();
				jmd.setProperty("Database Server", (dmd.getDatabaseProductName() + " " + dmd.getDatabaseProductVersion()));
				jmd.setProperty("JDBC Driver", (dmd.getDriverName() + " " + dmd.getDriverVersion()));
			}
			catch (SQLException sqle) {
				jmd.setProperty("Metadata Error", sqle.getMessage());
			}
			return jmd;
		}
		
		//		/** read a text file
//		 * @param	fileName	the path and name of the file to be read, under this StandardIoProvider's default directory
//		 * @return	the content of the file addressed by fileName
//		 * @throws FileNotFoundException
//		 * @throws IOException
//		 */
//		public String readFile(String fileName) throws FileNotFoundException, IOException {
//			return EasyIO.readFile((new File(fileName)).isAbsolute() ? fileName : (this.fileHomeDir + fileName));
//		}
//		/** read a text file
//		 * @param	file	the file to be read
//		 * @return	the content of the specified File
//		 * @throws FileNotFoundException
//		 * @throws IOException
//		 */
//		public String readFile(File file) throws FileNotFoundException, IOException {
//			return EasyIO.readFile(file);
//		}
	//	
//		/**	write content to the end of an existing file, create file if non-existant
//		 * @param	fileName	the path and name of the file to write to, under this StandardIoProvider's default directory
//		 * @param	content		the String to be written to the end of the specified file
//		 * @throws IOException
//		 */
//		public void writeFile(String fileName, String content) throws IOException {
//			EasyIO.writeFile(((new File(fileName)).isAbsolute() ? fileName : (this.fileHomeDir + fileName)), content);
//		}
//		/**	write content to the end of an existing file, create file if non-existant
//		 * @param	file		the File to write to
//		 * @param	content		the String(s) to be written to the end of the specified file
//		 * @throws IOException
//		 */
//		public void writeFile(File file, String content) throws IOException {
//			EasyIO.writeFile(file, content);
//		}
//		/**	write content to the end of an existing file, create file if non-existant
//		 * @param	fileName	the path and name of the file to write to, under this StandardIoProvider's default directory
//		 * @param	content		the String(s) to be written to the end of the specified file
//		 * @throws IOException
//		 */
//		public void writeFile(String fileName, StringVector content) throws IOException {
//			EasyIO.writeFile(((new File(fileName)).isAbsolute() ? fileName : (this.fileHomeDir + fileName)), content);
//		}
//		/**	write content to the end of an existing file, create file if non-existant
//		 * @param	file		the File to write to
//		 * @param	content		the String(s) to be written to the end of the specified file
//		 * @throws IOException
//		 */
//		public void writeFile(File file, StringVector content) throws IOException {
//			EasyIO.writeFile(file, content);
//		}
	//	
//		/**	write content to the end of an existing file
//		 * @param	fileName	the path and name of the file to write to, under this StandardIoProvider's default directory
//		 * @param	content		the String to be written to the end of the specified file
//		 * @throws IOException
//		 */
//		public void writeToFile(String fileName, String content) throws IOException {
//			EasyIO.writeToFile(((new File(fileName)).isAbsolute() ? fileName : (this.fileHomeDir + fileName)), content);
//		}
//		/**	write content to the end of an existing file
//		 * @param	file		the File to write to
//		 * @param	content		the String to be written to the end of the specified file
//		 * @throws IOException
//		 */
//		public void writeToFile(File file, String content) throws IOException {
//			EasyIO.writeToFile(file, content);
//		}
	//	
//		/**	obtain a File by its name. If the file does not exist, it is created. If the specified file name denotes a relative path, the returned file is relative to this IoProvider's data base path.    
//		 * @param	fileName	the path and name of the desired file
//		 * @return	the File denoted by the specified file name and, if the latter is relative, by this IoProvider's data base path 
//		 * @throws IOException
//		 */
//		public File getFile(String fileName) throws IOException {
//			File file = new File((new File(fileName)).isAbsolute() ? fileName : (this.fileHomeDir + fileName));
//			if (!file.exists()) {
//				if (file.isDirectory()) file.mkdirs();
//				else {
//					File parentFile = file.getParentFile();
//					if ((parentFile != null) && !parentFile.exists()) parentFile.mkdirs();
//					file.createNewFile();
//				}
//			}
//			return file;
//		}
	//	
//		/**	@return	the File denoting the base directory for this IoProvider's file IO
//		 */
//		public File getHomeDirectory() {
//			return new File(this.fileHomeDir);
//		}
	//	
//		/**	change the base directory for this IoProvider's file IO (use with care)
//		 * @param	homeDirectory	the new base directory for this IoProvider's file IO
//		 */
//		public void setHomeDirectory(File homeDirectory) {
//			if (homeDirectory != null) {
//				if (homeDirectory.isDirectory()) {
//					if (!homeDirectory.exists()) homeDirectory.mkdirs();
//					this.fileHomeDir = homeDirectory.getAbsolutePath();
//				}
//				else this.setHomeDirectory(homeDirectory.getParentFile());
//			}
//		}
	//	
//		/**	load a page form the WWW
//		 * @param	url		the address to read
//		 * @return	the page as a String
//		 * @throws MalformedURLException
//		 * @throws IOException
//		 */
//		public String getPage(String url) throws MalformedURLException, IOException {
//			if (this.wwwValid)
//				return EasyIO.getPage(url);
//			else return null;
//		}
	//	
		/**	send msg as an eMail with subject sbj to toAddress
		 */
		public void smtpSend(String subject, String message, String[] toAddresses) throws Exception {
			if (this.smtpValid)
				EasyIO.smtpSend(subject, message, toAddresses, this.smtpFromAddress, this.smtpServer, this.smtpPort, this.smtpLogin, this.smtpPassword);
		}

		/**	@return	true if and only if this IoProvider allows to send mails
		 */
		public boolean isMessagingAvailable() {
			return this.smtpValid;
		}
	}
//	
//	/**
//	 * Creates an IO Provider configured according to the settings specified in
//	 * the specified file.
//	 * @param settingsFile the path and file containing the settings
//	 * @return an IO interface configured according to the settings specified in
//	 *         the specified file
//	 */
//	public static IoProvider getIoProvider(String settingsFile) {
//		Settings config = Settings.loadSettings(settingsFile);
//		return new StandardIoProvider(config);
//	}
//	
//	/**	readFile reads the content of the specified file and returns it as a String
//	* @param	fileName	the path and name of the file to read
//	* @return	the content of the specified file as a String
//	* @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
//	* @throws	IOException, if any IOException occures
//	*/
//	public static String readFile(String fileName) throws FileNotFoundException, IOException {
//		return readFile(new File(fileName));
//	}
//	
	/**
	 * Read the content of a file and return it as a String.
	 * @param file the file to read
	 * @return the content of the specified file as a String
	 * @throws IOException, if any IOException occures
	 */
	public static String readFile(File file) throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuffer assembler = new StringBuffer();
		String line;
		while ((line = br.readLine()) != null) {
			assembler.append(line);
			assembler.append("\n");
		}
		br.close();
		return assembler.toString();
	}
//	
//	/**	writeFile writes the String content to the end of the specified file
//	 * @param	fileName	the path and name of the file to write to
//	 * @param	content		the String to be written to the end of the specified file
//	 * @return	true, if the writing process was successful, false otherwise
//	 * @throws	IOException, if any IOEXception occures
//	 * 	If the specified file doesn't exist in the specified path, it will be created
//	 */
//	public static boolean writeFile(String fileName, String content) throws IOException {
//		return writeFile(new File(fileName), content);
//	}
//	
	/**
	 * Append a string to the end of a file. If the specified file doesn't exist
	 * in the specified path, it will be created.
	 * @param file the file to write to
	 * @param content the String to write to the end of the specified file
	 * @return true if the writing process was successful, false otherwise
	 * @throws IOException, if any IOEXception occures
	 */
	public static boolean writeFile(File file, String content) throws IOException {
		try {
			BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
			buf.write(content);
			buf.newLine();
			buf.flush();
			buf.close();
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}
//	
//	/**	writeFile writes the Strings contained in the StringVector content to the end of the specified file
//	 * @param	fileName	the path and name of the file to write to
//	 * @param	content		the StringVector containing the Strings to be written to the end of the specified file
//	 * @return	true, if the writing process was successful, false otherwise
//	 * @throws	IOException, if any IOEXception occures
//	 * 	If the specified file doesn't exist in the specified path, it will be created
//	 */
//	public static boolean writeFile(String fileName, StringVector content) throws IOException {
//		return writeFile(new File(fileName), content);
//	}
//	
//	/**	writeFile writes the Strings contained in the StringVector content to the end of the specified file
//	 * @param	file		the file to write to
//	 * @param	content		the StringVector containing the Strings to be written to the end of the specified file
//	 * @return	true, if the writing process was successful, false otherwise
//	 * @throws	IOException, if any IOEXception occures
//	 * 	If the specified file doesn't exist in the specified path, it will be created
//	 */
//	public static boolean writeFile(File file, StringVector content) throws IOException {
//		StringVector lines;
//		BufferedWriter buf;
//		
//		try {
//			buf = new BufferedWriter(new FileWriter(file, true));
//			for (int c = 0; c < content.size(); c++) {
//				lines = parseWriteString(content.get(c));
//				for (int l = 0; l < lines.size(); l++) {
//					buf.write(lines.get(l), 0, lines.get(l).length());
//					buf.newLine();
//				}
//			}
//			buf.flush();
//			buf.close();
//			return true;
//		}
//		catch (IOException e) {
//			return false;
//		}
//	}
//	
//	/**	writeToFile writes the String content to the end of the specified file
//	 * @param	fileName	the path and name of the file to write to
//	 * @param	content		the String to be written to the end of the specified file
//	 * @return	true, if the writing process was successful, false otherwise
//	 * @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
//	 * @throws	IOException, if any IOEXception occures
//	 * 	If the specified file doesn't exist in the specified path, it won't be created
//	 */
//	public static boolean writeToFile(String fileName, String content) throws IOException {
//		File f = new File(fileName);
//		return writeToFile(f, content);
//	}
//	
//	/**	writeToFile writes the String content to the end of the specified file
//	 * @param	file		the file to write to
//	 * @param	content		the String to be written to the end of the specified file
//	 * @return	true, if the writing process was successful, false otherwise
//	 * @throws	FileNotFoundException, if the specified file dosn't exist in the specified path
//	 * @throws	IOException, if any IOEXception occures
//	 * 	If the specified file doesn't exist in the specified path, it won't be created
//	 */
//	public static boolean writeToFile(File file, String content) throws IOException {
//		StringVector lines;
//		BufferedWriter buf;
//		
//		if (file.exists()) {
//			lines = parseWriteString(content);
//			try {
//				buf = new BufferedWriter(new FileWriter(file, true));
//				for (int i = 0; i < lines.size(); i++) {
//					buf.write(lines.get(i), 0, lines.get(i).length());
//					buf.newLine();
//				}
//				buf.flush();
//				buf.close();
//				return true;
//			}
//			catch (IOException e) {
//				return false;
//			}
//		}
//		else return false;
//	}
//	
//	/**	parseWriteString parses the String s to individual lines at \n and \r characters
//	 * @param	s	the String to be parsed
//	 * @return	a StringVector containing the parts of s
//	 */
//	private static StringVector parseWriteString(String s) {
//		StringVector lines = new StringVector();
//		String workString = s;
//		while ((workString.indexOf("\n") > - 1) || (workString.indexOf("\r") > - 1)) {
//			if ((workString.indexOf("\n") > - 1) && ((workString.indexOf("\n") < workString.indexOf("\r")) || (workString.indexOf("\r") == -1))) {
//				lines.addElement(workString.substring(0, workString.indexOf("\n")));
//				if (workString.indexOf("\n") < workString.length())
//					workString = workString.substring(workString.indexOf("\n")+1);
//				else workString = "";
//			}
//			else if ((workString.indexOf("\r") > - 1) && ((workString.indexOf("\r") < workString.indexOf("\n")) || (workString.indexOf("\n") == -1))) {
//				lines.addElement(workString.substring(0, workString.indexOf("\r")));
//				if (workString.indexOf("\r") < workString.length())
//					workString = workString.substring(workString.indexOf("\r")+1);
//				else workString = "";
//			}
//		}
//		if (workString.length() > 0)
//			lines.addElement(workString);
//		
//		return lines;
//	}
//	
//	/**	rename a file (if the File denoted by the new name exists, it is deleted)
//	 * @param	file		the File to rename (if it does not exist, nothing changes)
//	 * @param	newFile		the File denoting the new name (if it exists, it is deleted before the other file is renamed)
//	 * @return a File object referencing the renamed file
//	 */
//	public static File renameFile(File file, File newFile) {
//		if ((file == null) || !file.exists() || (newFile == null)) return file;
//		if (newFile.exists()) newFile.delete();
//		file.renameTo(newFile);
//		return newFile;
//	}
//
//	/**	getPage reads the internet page pointed to by url and returns it as a String
//	* @param	url			the address of the internet page to get
//	* @return	the internet page as a String
//	* @throws	MalformedURLException, if the specified url is malformed
//	* @throws	IOException, if any IOException occures
//	*/
//	public static String getPage(String url) throws MalformedURLException, IOException {
//		String protocol = "http";
//		String host;
//		int port = 0;
//		String file = "";
//		URL address;
//		
//		//	parse url
//		//	- read protocol if given
//		if (url.indexOf("://") > -1) {
//			protocol = url.substring(0, url.indexOf("://"));
//			
//			//		- parse file if given
//			if (url.indexOf("/", (url.indexOf("://") + 3)) > -1) {
//				host = url.substring((url.indexOf("://") + 3), url.indexOf("/", (url.indexOf("://") + 3)));
//				file = url.substring(url.indexOf("/", (url.indexOf("://") + 3)));
//			}
//			
//			//		- otherwise
//			else host = url.substring(url.indexOf("://") + 3);
//		}
//		
//		//	- otherwise
//		else {
//			
//			//		- parse file if given
//			if (url.indexOf("/") > -1) {
//				host = url.substring(0, url.indexOf("/"));
//				file = url.substring(url.indexOf("/"));
//			}
//			
//			//		- otherwise
//			else host = url;
//		}
//		
//		//	- read port number if given
//		if (host.indexOf(":") > -1) {
//			port = java.lang.Integer.parseInt(host.substring(host.indexOf(":") + 1));
//			host =host.substring(0, host.indexOf(":"));
//		}
//		
//		//	create full address
//		if (port == 0)
//			address = new URL(protocol, host, file);
//		else address = new URL(protocol, host, port, file);
//		
//		//	open connection
//		URLConnection connection = address.openConnection();
//		connection.setDoInput(true);
//		connection.setDoOutput(true);
//		connection.setUseCaches(false);
//		connection.setRequestProperty("User-Agent", "EasyIO");
//		connection.connect();
//		BufferedReader urlReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//		
//		StringBuffer pageAssembler = new StringBuffer();
//		String pagePiece;
//		
//		//	read page
//		while ((pagePiece = urlReader.readLine()) != null) {
//			pagePiece = pagePiece.trim();
//			pageAssembler.append(pagePiece + "\n");
//		}
//		urlReader.close();
//		return pageAssembler.toString();
//	}
//	
	/**
	 * Send the String msg as an eMail with the subject sbj to the
	 * address toAddress.
	 * @param subject the subject of the mail to be sent
	 * @param message the text of the mail to be sent
	 * @param toAddresses an array holding the addresses to send the mail to
	 * @param fromAddress the address to be specified as the sender of the mail
	 * @param server the smtp server to send the mail
	 * @param port the port to send the mail through
	 * @param login the login to the smtp server
	 * @param password the password to the smtp server
	 * @throws MessagingException, if any MessagingException occures
	 */
	public static void smtpSend(String subject, String message, String toAddresses[], String fromAddress, String server, int port, final String login, final String password) throws MessagingException {
		if (toAddresses.length == 0)
			return;
		
        Properties props = System.getProperties();
        if ((login != null) && (password != null))
        	props.setProperty("mail.smtp.auth", "true");
		
		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(login, password);
			}
		};
		Session session = Session.getInstance(props, auth);
		
		MimeMessage msg = new MimeMessage(session);
		
		msg.setFrom(new InternetAddress(fromAddress));
		Address to[] = new Address[toAddresses.length];
		for (int t = 0; t < to.length; t++)
			to[t] = new InternetAddress(toAddresses[t]);
		msg.setRecipients(Message.RecipientType.TO, to);
		
		msg.setSubject(subject);
		
		MimeBodyPart body = new MimeBodyPart();
		body.setText(message);
		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(body);
		msg.setContent(multipart);
		
		Transport transport = session.getTransport("smtp");
		transport.connect(server, port, login, password);
		transport.sendMessage(msg, to);
		transport.close();
	}
	
	/**
	 * Insert escaper Characters into data, one before every appearance of
	 * Character toEscape.
	 * @param data the String to be provides with escape Characters
	 * @return the escaped string
	 */
	public static String sqlEscape(String data) {
		return escape(data, '\'', '\'');
	}

	/**
	 * Prepare a String to be used in a LIKE clause in an SQL query (replace
	 * spaces and other problematic characters by wildcards).
	 * @param string the String to be prepared
	 * @return the specified String prepared to be used in a LIKE clause in an
	 *         SQL query
	 */
	public static String prepareForLIKE(String string) {
		string = string.replaceAll("\\s+", "%");
		string = string.replaceAll("\\'", "%");
		return string;
//		String sqlString = string;
//		StringVector escaper = new StringVector();
//		
//		escaper.parseAndAddElements(sqlString, " ");
//		sqlString = escaper.concatStrings("%");
//		
//		escaper.clear();
//		
//		escaper.parseAndAddElements(sqlString, "'");
//		sqlString = escaper.concatStrings("%");
//		
//		return sqlString;
	}

	/**
	 * Insert escaper Characters into data, one before every appearance of
	 * Character toEscape.
	 * @param data the String to be provides with escape Characters
	 * @param toEscape the Character to be escaped
	 * @param escaper the Character to be used for escaping
	 * @return the escaped string
	 */
	public static String escape(String data, char toEscape, char escaper) {
		if (data == null)
			return null;
		StringBuffer assembler = new StringBuffer();
		char currentChar;
		for (int i = 0; i < data.length(); i++) {
			currentChar = data.charAt(i);
			if (currentChar == toEscape)
				assembler.append(escaper);
			assembler.append(currentChar);
		}
		return assembler.toString();
	}

	/**
	 * Insert escaper Characters into data, one before every appearance of each
	 * Character contained in toEscape.
	 * @param data the String to be provides with escape Characters
	 * @param toEscape the array containing the Characters to be escaped
	 * @param escaper the Character to be used for escaping
	 * @return the escaped string
	 */
	public static String escape(String data, char toEscape[], char escaper) {
		if (data == null)
			return null;
		StringBuffer assembler = new StringBuffer();
		String escapeChars = new String(toEscape);
		char currentChar;
		for (int i = 0; i < data.length(); i++) {
			currentChar = data.charAt(i);
			if (escapeChars.indexOf(currentChar) != -1)
				assembler.append(escaper);
			assembler.append(currentChar);
		}
		return assembler.toString();
	}
}
