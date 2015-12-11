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
 *     * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
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
package de.uka.ipd.idaho.easyIO.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import de.uka.ipd.idaho.easyIO.streams.PeekReader;

/**
 * SQL Dump Readers parse through dumps of MySQL and PostgresSQL databases they
 * ingest from a reader. Content handling works much in the style of XML SAX
 * parsers, i.e., reading through the stream and issuing event based
 * notifications on content found.<br>
 * The main intention of this class is to facilitate exploring SQL dumps that
 * are too large for working with in a text editor, ad for extracting specific
 * data from such dumps.
 * 
 * @author sautter
 */
public abstract class SqlDumpReader {
	
	/** Constructor
	 */
	protected SqlDumpReader() {}
	
	/**
	 * Handle a line of an SQL dump that is not an INSERT INTO statement. Such
	 * lines may be comments, CREATE TABLE statements or parts thereof, or
	 * statements that add constraints. While important for exploring an SQL
	 * dump, they can be largely ignored on content extraction.
	 * @param line the line to handle
	 */
	public abstract void handleNonInsertLine(String line);
	
	/**
	 * Handle a single record to be inserted into a table. The argument record
	 * is raw, i.e., still contains escape characters. To parse a record into
	 * individual fields, use the <code>parseRecord()</code> method.
	 * @param tableName the name of the table the record is inserted into
	 * @param record the record to handle
	 */
	public abstract void handleInsertRecord(String tableName, String record);
	
	/**
	 * Handle the end of a single insert statement.
	 * @param tableName the table the insertion is directed at
	 */
	public abstract void handleInsertStatementEnd(String tableName);
	
	/**
	 * Read an SQL dump. If the argument SQL dump reader instance is null, a
	 * default instance will be used. This default instance outputs any non-
	 * insertion lines, as well as the first 3 records of the 10 first INSERT
	 * INTO statements per table. This is mostly helpful for getting a first
	 * idea of the contents of the SQL dump.
	 * @param r the reader to read from
	 * @param sdr the SQL dump reader instance to receive events
	 * @throws IOException
	 */
	public static void readSqlDump(Reader r, SqlDumpReader sdr) throws IOException {
		
		//	make sure we have an event receiver
		if (sdr == null)
			sdr = new ExplorerSqlDumpReader();
		
		//	parse through SQL dump
		PeekReader pr = new PeekReader(r, 256);
		String insertTableName = null;
		while (pr.peek() != -1) {
			
			//	handle line breaks
			if (pr.peek() == '\r')
				pr.read();
			else if (pr.peek() == '\n')
				pr.read();
			
			//	handle INSERT INTO statements
			else if (pr.startsWith("INSERT", true)) {
				String ih = cropInsertHeader(pr);
				if (insertTableName == null)
					insertTableName = extractTableName(ih);
				String rd = null;
				
				//	read data portion of statement to next line break
				while ((pr.peek() != '\n') && (pr.peek() != '\r')) {
					pr.skipSpace();
					
					//	skip over record separator
					if (pr.peek() == ',') {
						pr.read();
						pr.skipSpace();
					}
					
					//	read record
					if (pr.peek() == '(') {
						rd = cropRecordData(pr);
						sdr.handleInsertRecord(insertTableName, rd);
					}
					
					//	read line ending semicolon
					else if (pr.peek() == ';') {
						pr.read();
						sdr.handleInsertStatementEnd(insertTableName);
						break;
					}
					else break;
				}
				
				//	read to end of line
				while ((pr.peek() != '\n') && (pr.peek() != '\r'))
					System.out.print((char) pr.read());
				while ((pr.peek() == '\n') || (pr.peek() == '\r'))
					pr.read();
			}
			
			//	handle other statement
			else {
				insertTableName = null;
				StringBuffer nil = new StringBuffer();
				while ((pr.peek() != '\n') && (pr.peek() != '\r'))
					nil.append((char) pr.read());
				sdr.handleNonInsertLine(nil.toString());
				while ((pr.peek() == '\n') || (pr.peek() == '\r'))
					pr.read();
			}
		}
	}
	
	/**
	 * Parse a single insert record into individual column values.
	 * @param record the record to parse
	 * @return an array holding the individual fields
	 * @throws IOException
	 */
	public static String[] parseRecordData(String record) throws IOException {
		ArrayList recordParts = new ArrayList();
		PeekReader pr = new PeekReader(new StringReader(record), 256);
		pr.read(); // consume opening '('
		while (pr.peek() != ')') {
			
			//	skip over field separator
			if (pr.peek() == ',')
				pr.read();
			
			//	read string value
			else if (pr.peek() == '\'') {
				String str = cropString(pr, false);
				if (str.startsWith("'"))
					str = str.substring("'".length());
				if (str.endsWith("'"))
					str = str.substring(0, (str.length() - "'".length()));
				recordParts.add(str);
			}
			
			//	read other value
			else recordParts.add(cropNonQuoted(pr));
		}
		pr.read(); // consume closing ')'
		return ((String[]) recordParts.toArray(new String[recordParts.size()]));
	}
	
	private static class ExplorerSqlDumpReader extends SqlDumpReader {
		int insertCount = 0;
		int recordCount = 0;
		int insertRecordCount = 0;
		public void handleNonInsertLine(String line) {
			if (this.insertCount != 0)
				System.out.println(" [" + this.insertCount + " insert lines with " + this.insertRecordCount + " records]");
			this.recordCount = 0;
			this.insertCount = 0;
			this.insertRecordCount = 0;
			System.out.println(line);
		}
		public void handleInsertRecord(String tableName, String record) {
			if ((this.insertCount < 10) && (this.recordCount < 3))
				System.out.println(record + " ==> " + tableName);
			this.recordCount++;
			this.insertRecordCount++;
		}
		public void handleInsertStatementEnd(String tableName) {
			if (this.insertCount < 10)
				System.out.println(" [" + this.recordCount + " records]");
			this.insertCount++;
			this.recordCount = 0;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File colPath = new File("E:/Install/CoL");
		File colSqlDump = new File(colPath, "col2015ac.sql");
		readSqlDump(new InputStreamReader(new FileInputStream(colSqlDump), "UTF-8"), null);
	}
	
	private static String cropInsertHeader(PeekReader pr) throws IOException {
		StringBuffer ih = new StringBuffer();
		while (pr.peek() != '(')
			ih.append((char) pr.read());
		return ih.toString();
	}
	
	private static String extractTableName(String ih) {
		ih = ih.substring("INSERT INTO ".length()).trim();
		ih = ih.substring("`".length());
		ih = ih.substring(0, ih.indexOf('`'));
		return ih;
	}
	
	private static String cropRecordData(PeekReader pr) throws IOException {
		StringBuffer rd = new StringBuffer();
		while (pr.peek() != ')') {
			if (pr.peek() == '\'')
				rd.append(cropString(pr, true));
			else rd.append((char) pr.read());
		}
		rd.append((char) pr.read());
		return rd.toString();
	}
	
	private static String cropString(PeekReader pr, boolean keepEscaped) throws IOException {
		StringBuffer str = new StringBuffer();
		str.append((char) pr.read());
		boolean escaped = false;
		while (pr.peek() != -1) {
			if (escaped) {
				str.append((char) pr.read());
				escaped = false;
			}
			else if (pr.peek() == '\\') {
				if (keepEscaped)
					str.append((char) pr.read());
				else pr.read();
				escaped = true;
			}
			else if (pr.peek() == '\'') {
				str.append((char) pr.read());
				break;
			}
			else str.append((char) pr.read());
		}
		return str.toString();
	}
	
	private static String cropNonQuoted(PeekReader pr) throws IOException {
		StringBuffer nq = new StringBuffer();
		while (pr.peek() != -1) {
			if (pr.peek() == ',')
				break;
			else if (pr.peek() == ')')
				break;
			else nq.append((char) pr.read());
		}
		return nq.toString();
	}
}