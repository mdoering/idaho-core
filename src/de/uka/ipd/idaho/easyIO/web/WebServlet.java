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
package de.uka.ipd.idaho.easyIO.web;

import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import de.uka.ipd.idaho.easyIO.settings.Settings;

/**
 * Abstract class providing the basic IO facilities for service servlets. Each
 * servlet can have its data stored in a separate folder below the surrounding
 * web-app's WEB-INF folder, its so-called data path. The default data path is the
 * web-app's WEB-INF folder itself, but a specific data path can be specified as
 * the <b>dataPath</b> parameter in the web.xml.<br>
 * For sub class specific settings and parameters, each servlet in addition has
 * an instance specific configuration file, loaded from its data path. By
 * default, this file is named <b>config.cnfg</b>, but an alternative name can
 * be specified in an the <b>configFile</b> parameter in the web.xml.<br>
 * 
 * 
 * @author sautter
 */
public abstract class WebServlet extends HttpServlet implements WebConstants {
	
	/** the surrounding web-app's context path, i.e., its root folder */
	protected File rootFolder;
	
	/** the surrounding web-app's WEB-INF path */
	protected File webInfFolder;
	
	/**
	 * the servlet's data folder, nested inside the surrounding web-app's
	 * WEB-INF folder to protect files not explicitly exposed from read access;
	 * files intended to be accessible through HTTP have to be registered with
	 * the ResourceServlet class and will be served by the local instance of
	 * this class
	 */
	protected File dataFolder;
	
	/**
	 * the servlet's logical data path as a string, relative to the root path,
	 * as specified in the web.xml; this string is either empty, or it starts
	 * with a '/', in accordance to the usual return values of the
	 * getContextPath() and getServletPath() methods of HttpServletRequest)
	 */
	protected String dataPath;
	
	/**
	 * a settings object containing the settings from the servlet's
	 * configuration file; this file is expected to be located in the servlet's
	 * data path, and is named config.cnfg by default; sub classes should use
	 * the getSetting(), setSetting(), and removeSetting() methods instead of
	 * accessing this object directly to facilitate automated storage of
	 * modified settings; if this object is manipulated directly, sub classes
	 * should use the setConfigDirty() method to indicate so.
	 */
	protected Settings config;
	private boolean configDirty = false;
	
	/**
	 * This implementation loads the config file and determined the root and
	 * data paths. To prevent overwriting, it is final. Sub classes should
	 * overwrite the init(Settings) method insted, which exists exactly for this
	 * purpose.
	 * @see javax.servlet.GenericServlet#init()
	 */
	public final void init() throws ServletException {
		
		//	link up to webapp host
		WebAppHost host = WebAppHost.getInstance(this.getServletContext());
		host.registerServlet(this);
		
		//	get local environment
		this.rootFolder = host.getRootFolder();
		this.webInfFolder = host.getWebInfFolder();
		
		this.dataPath = this.getInitParameter("dataPath");
		if (dataPath == null) {
			this.dataPath = "";
			this.dataFolder = this.webInfFolder;
		}
		else {
			this.dataPath = this.dataPath.trim();
			while (this.dataPath.startsWith("/"))
				this.dataPath = this.dataPath.substring(1);
			this.dataPath = this.dataPath.trim();
			if (this.dataPath.length() != 0)
				this.dataPath = ("/" + this.dataPath);
			this.dataFolder = new File(this.webInfFolder, this.dataPath);
		}
		
		//	load instance specific config file
		String configFile = this.getInitParameter("configFile");
		if (configFile == null)
			configFile = "config.cnfg";
		this.config = Settings.loadSettings(new File(this.dataFolder, configFile));
		
		//	initialize sub class
		this.doInit();
	}
	
	/**
	 * Do implementation specific initialization. This method does nothing by
	 * default, sub classes are welcome to overwrite it as needed.
	 * @throws ServletException
	 */
	protected void doInit() throws ServletException {}
	
	/**
	 * Retrieve a setting from the servlet configuration. If the setting is not
	 * present in the particular servlet's configuration file, this method
	 * retrieves the setting from the webapp-wide configuration available from
	 * the webapp host. This is to facilitate setting default values for an
	 * entire webapp in one place and to overwrite them with servlet specific
	 * values where required.
	 * @param key the name of the setting
	 * @return the setting with the specified name
	 */
	protected String getSetting(String key) {
		String value = this.config.getSetting(key);
		return ((value == null) ? WebAppHost.getInstance(this.getServletContext()).getSetting(key) : value);
	}
	
	/**
	 * Retrieve a setting from the node configuration. If the setting is not
	 * present in the particular servlet's configuration file, this method
	 * retrieves the setting from the webapp-wide configuration available from
	 * the webapp host. This is to facilitate setting default values for an
	 * entire webapp in one place and to overwrite them with servlet specific
	 * values where required.
	 * @param key the name of the setting
	 * @param def the default value to use if the setting does not exist
	 * @return the setting with the specified name, or the specified default
	 *         value if the setting does not exist
	 */
	protected String getSetting(String key, String def) {
		String value = this.config.getSetting(key);
		return ((value == null) ? WebAppHost.getInstance(this.getServletContext()).getSetting(key, def) : value);
	}
	
	/**
	 * Store a setting. The change will be persisted automatically on shutdown.
	 * @param key the name of the setting
	 * @param value the value of the setting
	 * @return the previous value of the setting
	 */
	protected String setSetting(String key, String value) {
		String oldValue = this.config.setSetting(key, value);
		this.configDirty = (this.configDirty || !value.equals(oldValue));
		return oldValue;
	}
	
	/**
	 * Remove a setting. The change will be persisted automatically on shutdown.
	 * @param key the name of the setting
	 * @return the value of the removed setting
	 */
	protected String removeSetting(String key) {
		String value = this.config.removeSetting(key);
		this.configDirty = (this.configDirty || (value != null));
		return value;
	}
	
	/**
	 * Indicate that the config object has been manipulated and needs to be
	 * stored on exit.
	 */
	protected void setConfigDirty() {
		this.configDirty = true;
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#destroy()
	 */
	public final void destroy() {
		this.exit();
		if (this.configDirty) {
			String configFile = this.getInitParameter("configFile");
			if (configFile == null)
				configFile = "config.cnfg";
			try {
				Settings.storeSettingsAsText(new File(this.dataFolder, configFile), this.config);
				this.configDirty = false;
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Do sub class specific shutdown operations. This default implementation
	 * does nothing, sub classes are welcome to overwrite it as needed.
	 */
	protected void exit() {}
	
	/**
	 * Find a file with a given name somewhere in the data accessible to the
	 * host. The file is first sought in the servlet's data path, then in the
	 * surrounding web-app's WEB-INF folder, and finally in the surrounding
	 * web-app's root folder. The file name can also contain relative paths.
	 * @param fileName the name of the file to find
	 * @return a file object pointing to the sought file, or null, if the file
	 *         does not exist in any of the folders
	 */
	public File findFile(String fileName) {
		File file = new File(this.dataFolder, fileName);
		if (!file.exists())
			file = new File(this.webInfFolder, fileName);
		if (!file.exists())
			file = new File(this.rootFolder, fileName);
		return (file.exists() ? file : null);
	}
}
