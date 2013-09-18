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
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.IoProvider;
import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder.HtmlPageBuilderHost;

/**
 * This class provides a shared host for the servlets of a webapp. It has at
 * most one instance for each webapp, distinguished by context path. This
 * instance is created when the first servlet attempts to retrieve it. In
 * particular, this class provides the following services:<br>
 * A registry allowing servlets to to obtain references to one another, e.g. to
 * facilitate communication via method invocations insetad of local HTTP
 * loopback. Note that servlet containers may instantiate servlets only when
 * they are first accessed via HTTP, so the absence of a servlet from the
 * registry does not necessarily mean that it is absent - it might just yet to
 * be loaded.<br>
 * A centralized access point to configurations for JDBC and eMail, and
 * arbitrary read-only variables, e.g. the names of CSS stylesheets to use. When
 * first created, the instances of this class load these configurations from a
 * file named 'web.cnfg', expected to be located in the webapp's WEB-INF folder.<br>
 * An abstract centralized point of authentication. It is up to servlets to
 * register actual authentication providers. This class only provides a
 * centralized access point.
 * 
 * @author sautter
 */
public class WebAppHost {
	
	/**
	 * the name of the HTTP request parameter for specifying to use specific
	 * authentication provider
	 */
	public static final String AUTHENTICATION_PROVIDER_NAME_PARAMETER = "authenticationProviderName";
	
	/**
	 * the name of the HTTP request parameter for specifying the URL to forward
	 * to after login
	 */
	public static final String FORWARD_URL_PARAMETER = "forwardUrl";
	
	private static HashMap instances = new HashMap();
	
	/**
	 * Retrieve the host object for the web application with a given servlet
	 * context. If no host object exists so far, this method creates it.
	 * @param sc the servlet context of the webapp to obtain the host for
	 * @return the host for the webapp identified by the argument servlet
	 *         context
	 */
	public static synchronized WebAppHost getInstance(ServletContext sc) {
		String path = sc.getRealPath("./");
		WebAppHost webAppHost = ((WebAppHost) instances.get(path));
		if (webAppHost != null) return webAppHost;
		webAppHost = new WebAppHost(path);
		instances.put(webAppHost.path, webAppHost);
		return webAppHost;
	}
	
	private String path;
	private File rootFolder;
	private File webInfFolder;
	private Settings settings;
	private HashMap registry = new HashMap();
	
	private WebAppHost(String path) {
		System.out.println("WebAppHost created for path " + path);
		this.path = path;
		this.rootFolder = new File(this.path);
		this.webInfFolder = new File(this.rootFolder, "WEB-INF");
		
		this.settings = Settings.loadSettings(new File(this.webInfFolder, "web.cnfg"));
		
		this.authenticationProviderAttribute = ("authenticationProvider" + this.path.hashCode());
		this.authenticatedUserNameAttribute = ("authenticatedUserName" + this.path.hashCode());
		
		Settings amlData = this.settings.getSubset("accountManagerLinks");
		ArrayList amlList = new ArrayList(2);
		for (int l = 0; l < amlData.size(); l++) {
			Settings aml = amlData.getSubset("" + l);
			String label = aml.getSetting("label");
			String href = aml.getSetting("href");
			if ((label != null) && (href != null))
				amlList.add(new AccountManagerLink(label, href));
		}
		this.accountManagerLinks = ((AccountManagerLink[]) amlList.toArray(new AccountManagerLink[amlList.size()]));
		this.accountManagerLinkWindowParams = amlData.getSetting("windowParams", this.accountManagerLinkWindowParams);
	}
	
	/**
	 * Retrieve the root folder of the web application.
	 * @return the root folder of the web application
	 */
	public File getRootFolder() {
		return this.rootFolder;
	}
	
	/**
	 * Retrieve the WEB-INF folder of the web application.
	 * @return the WEB-INF folder of the web application
	 */
	public File getWebInfFolder() {
		return this.webInfFolder;
	}
	
	/**
	 * Obtain an IO provider object configured according to the central
	 * settings, e.g. to access a database via JDBC.
	 * @return an IO provider
	 */
	public IoProvider getIoProvider() {
		return EasyIO.getIoProvider(this.settings.getSubset("EasyIO"));
	}
	
	/**
	 * Retrieve a servlet from the registry. The argument name has to be the one
	 * assigned to the servlet in the web.xml. If there is no servlet with the
	 * specified name or has not been instantiated yet, this method returns
	 * null.
	 * @param sn the name of the servlet
	 * @return the servlet with the specified name
	 */
	public synchronized Servlet getServlet(String sn) {
		return ((Servlet) this.registry.get(sn));
	}
	
	/**
	 * Register a servlet so it becomes accessible through the getServlet()
	 * method.
	 * @param s the servlet to register
	 */
	public synchronized void registerServlet(Servlet s) {
		String sn = s.getServletConfig().getServletName();
		this.registry.put(sn, s);
		System.out.println("WebAppHost (" + this.path + "): registered servlet as '" + sn + "', class is " + s.getClass().getName());
	}
	
	/**
	 * Get the value assigned to a webapp-wide parameter. If there is no value
	 * assigned to the argument key, this method returns null.
	 * @param key the key of the required value
	 * @return the value assigned to the specified key
	 */
	public String getSetting(String key) {
		return this.settings.getSetting(key);
	}
	
	/**
	 * Get the value assigned to a webapp-wide parameter. If there is no value
	 * assigned to the argument key, this method returns the specified default
	 * value.
	 * @param key the key of the required value
	 * @param def the value to be returned if there is no value assigned to the
	 *            specified key
	 * @return the value assigned to the specified key, or the specified default
	 *         if there is no such value
	 */
	public String getSetting(String key, String def) {
		return this.settings.getSetting(key, def);
	}
	
	/**
	 * Interface to implement for objects that are able to authenticate HTTP
	 * sessions.
	 * 
	 * @author sautter
	 */
	public static abstract class AuthenticationProvider {
		
		/**
		 * Retrieve a name for the authentication provider. As the name has to
		 * be unique within the scope of a webapp, strings returned by this
		 * method should be rather distinctive. Further, strings returned by
		 * this method must be non-null, free from whitespace, and preferably
		 * strictly alphanumeric.
		 * @return the name of the authentication source.
		 */
		public abstract String getName();
		
		/**
		 * Retrieve a nice name for the authentication provider, to use for
		 * display purposes. The returned string must not be null or empty, and
		 * it should not contain line breaks.
		 * @return a nice name for the authentication source.
		 */
		public abstract String getLabel();
		
		/**
		 * Authenticate the session associated with an HTTP request. The
		 * argument request does not have an HTTP session yet, otherwise this
		 * method would not be called at all. Implementations of this method
		 * must return null to indicate that they could not authenticate the
		 * argument HTTP request. Returning a non-null result indicates that the
		 * authentication provider authenticates the HTTP session associated
		 * with the argument HTTP request. Further, implementations may create
		 * an HTTP session (call <code>request.getSession(true)</code>) only
		 * after successful authentication.
		 * @param request the request the HTTP session belongs to
		 * @return the user name belonging to the argument HTTP request
		 */
		public abstract String authenticate(HttpServletRequest request);
		
		/**
		 * Receive notification that an HTTP session is about to be invalidated.
		 * This method exists to enable authentication providers to clean up
		 * their internal data structures. The argument session is not
		 * invalidated yet, so ID and attributes are still accessible.
		 * Implementations of this method must not invalidate the session
		 * themselves. This default implementation does nothing. Sub classes are
		 * welcome to overwrite it as needed.
		 * @param session the HTTP session being invalidated
		 */
		public void sessionLoggingOut(HttpSession session) {}
		
		/**
		 * Indicates whether or not the authentication source wants to be
		 * selectable in a login form.
		 * @return true to be included in login forms, false otherwise
		 */
		public abstract boolean providesLoginFields();
		
		/**
		 * Write the login fields for the authentication source to an HTML page
		 * builder. This method is only called if the providesLoginFields()
		 * method returns true. Any names of fields generated by this method
		 * should start with the authentication provider's name in order to
		 * prevent collisions.
		 * @param pageBuilder the page builder to write to
		 * @throws IOException
		 * @see de.uka.ipd.idaho.easyIO.web.WebAppHost.AuthenticationProvider#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
		 */
		public abstract void writeLoginFields(HtmlPageBuilder pageBuilder) throws IOException;
		
		/**
		 * Write the body of the JavaScript function executed when this
		 * authentication source is selected. This default implementation
		 * leaves the function body empty. Sub classes are welcome to overwrite
		 * it as needed.
		 * @param pageBuilder the page builder to write to
		 */
		public void writeOnSelectFunctionBody(HtmlPageBuilder pageBuilder) throws IOException {}
		
		/**
		 * Write the body of the JavaScript function executed when this
		 * authentication source is de-selected. This default implementation
		 * leaves the function body empty. Sub classes are welcome to overwrite
		 * it as needed.
		 * @param pageBuilder the page builder to write to
		 */
		public void writeOnDeselectFunctionBody(HtmlPageBuilder pageBuilder) throws IOException {
			pageBuilder.writeLine("  return true;");
		}
		
		/**
		 * Write the body of the JavaScript function executed when the login
		 * button is clicked with this authentication source being selected.
		 * This default implementation writes a function body that simply
		 * returns true, approving the submission. Sub classes are welcome to
		 * overwrite it as needed.
		 * @param pageBuilder the page builder to write to
		 */
		public void writeOnLoggingInFunctionBody(HtmlPageBuilder pageBuilder) throws IOException {
			pageBuilder.writeLine("  return true;");
		}
		
		/**
		 * Write the body of the JavaScript function executed when the login
		 * form is submitted with this authentication source being selected.
		 * This default implementation writes a function body that simply
		 * returns true, approving the submission. Sub classes are welcome to
		 * overwrite it as needed.
		 * @param pageBuilder the page builder to write to
		 */
		public void writeOnLoginFunctionBody(HtmlPageBuilder pageBuilder) throws IOException {
			pageBuilder.writeLine("  return true;");
		}
		
		/**
		 * Write HTML code providing authentication source specific
		 * functionality to an HTML page builder, e.g. for allowing a user to
		 * change his password. This default implementation does nothing, sub
		 * classes are welcome to overwrite it as needed.
		 * @param pageBuilder the page builder to write to
		 * @throws IOException
		 * @see de.uka.ipd.idaho.easyIO.web.WebAppHost.AuthenticationProvider#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
		 */
		public void writeAccountManagerHtml(HtmlPageBuilder pageBuilder) throws IOException {}
		
		/**
		 * Retrieve the JavaScript function call to execute for a click directly
		 * on the account manager (not on one of its child elements). The
		 * returned function call can, for instance, manipulate HTML elements
		 * created by the writeAccountManagerHtml() method. This default
		 * implementation returns null. Sub classes are welcome to overwrite it
		 * as needed.
		 * @return the JavaScript call to execute for a click on the account
		 *         manager
		 */
		public String getAccountManagerOnclickCall() {
			return null;
		}
		
		/**
		 * Retrieve the JavaScript function call to execute when the account
		 * manager becomes visible. The returned function call can, for
		 * instance, manipulate HTML elements created by the writeAccountManagerHtml()
		 * method. This default implementation returns null. Sub classes are
		 * welcome to overwrite it as needed.
		 * @return the JavaScript call to execute when the account manager
		 *         becomes visible
		 */
		public String getAccountManagerOnshowCall() {
			return null;
		}
		
		/**
		 * Retrieve the JavaScript function call to execute when the account
		 * manager becomes invisible. The returned function call can, for
		 * instance, manipulate HTML elements created by the writeAccountManagerHtml()
		 * method. This default implementation returns null. Sub classes are
		 * welcome to overwrite it as needed.
		 * @return the JavaScript call to execute when the account manager
		 *         becomes invisible
		 */
		public String getAccountManagerOnhideCall() {
			return null;
		}
		
		/**
		 * Handle an HTTP request received by a servlet to provide
		 * implementation specific functionality, e.g. changing a password. For
		 * an HTTP request to get here, the (first step of the) path info has to
		 * be 'webAppAuthProvider'. For pre-authentication callbacks, e.g.
		 * password recovery, the second step has to be the name of the
		 * authentication provider. This default implementation simply returns
		 * false, sub classes are welcome to overwrite it as needed.
		 * @param request the HTTP request to handle
		 * @param response the HTTP response to write to
		 * @return true if the request has been handled, false otherwise
		 * @throws IOException
		 */
		public boolean handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			return false;
		}
	}
	
	private final String authenticationProviderAttribute;
	private final String authenticatedUserNameAttribute;
	
	private TreeMap authenticationProvidersByName = new TreeMap();
	
	private String accountManagerLinkWindowParams = "width=500,height=400,top=100,left=100,resizable=yes,scrollbar=yes,scrollbars=yes";
	private AccountManagerLink[] accountManagerLinks = {};
	private class AccountManagerLink {
		final String label;
		final String href;
		AccountManagerLink(String label, String href) {
			this.label = label;
			this.href = href;
		}
	}
	
	/**
	 * Add an authentication provider. Adding an authentication provider more
	 * than once does not have any effect.
	 * @param ap the authentication provider to add
	 */
	public void addAuthenticationProvider(AuthenticationProvider ap) {
		if (ap != null)
			this.authenticationProvidersByName.put(ap.getName(), ap);
	}
	
	/**
	 * Remove an authentication provider. Removing an authentication provider
	 * more than once does not have any effect.
	 * @param ap the authentication provider to remove
	 */
	public void removeAuthenticationProvider(AuthenticationProvider ap) {
		if (ap != null)
			this.authenticationProvidersByName.remove(ap.getName());
	}
	
	/**
	 * Retrieve the user name associated with an HTTP request. If the argument
	 * request is not yet authenticated, this method returns null.
	 * @param request the HTTP request to obtain the user for
	 * @return the user name associated with the argument HTTP request
	 */
	public String getUserName(HttpServletRequest request) {
		return this.getUserName(request.getSession(false));
	}
	
	/**
	 * Retrieve the user name associated with an HTTP session. If the argument
	 * request is not yet authenticated, this method returns null.
	 * @param session the HTTP session to obtain the user for
	 * @return the user name associated with the argument HTTP session
	 */
	public String getUserName(HttpSession session) {
		return ((session == null) ? null : ((String) session.getAttribute(this.authenticatedUserNameAttribute)));
	}
	
	/**
	 * Retrieve the authentication provider that authenticated an HTTP request.
	 * If the argument request is not yet authenticated, this method returns
	 * null.
	 * @param request the HTTP request to obtain the authentication provider for
	 * @return the authentication provider that authenticated the argument HTTP
	 *         request
	 */
	public AuthenticationProvider getAuthenticationProvider(HttpServletRequest request) {
		return this.getAuthenticationProvider(request.getSession(false));
	}
	
	/**
	 * Retrieve the authentication provider that authenticated an HTTP session.
	 * If the argument request is not yet authenticated, this method returns
	 * null.
	 * @param session the HTTP session to obtain the authentication provider for
	 * @return the authentication provider that authenticated the argument HTTP
	 *         session
	 */
	public AuthenticationProvider getAuthenticationProvider(HttpSession session) {
		return ((session == null) ? null : ((AuthenticationProvider) session.getAttribute(this.authenticationProviderAttribute)));
	}
	
	/**
	 * Log out the session associated with an HTTP request. The session is
	 * invalidated, and before that, its authentication provider is notified of
	 * the upcoming logout.
	 * @param session the HTTP request whose associated session to log out
	 */
	public void logout(HttpServletRequest request) {
		this.logout(request.getSession(false));
	}
	
	/**
	 * Log out an HTTP session. The session is invalidated, and before that, its
	 * authentication provider is notified of the upcoming logout.
	 * @param session the HTTP session to log out
	 */
	public void logout(HttpSession session) {
		if (session == null) return;
		AuthenticationProvider ap = this.getAuthenticationProvider(session);
		if (ap != null) ap.sessionLoggingOut(session);
		session.removeAttribute(this.authenticationProviderAttribute);
		session.removeAttribute(this.authenticatedUserNameAttribute);
		session.invalidate();
	}
	
	/**
	 * Obtain an HTML page builder to create a login page. The returned page
	 * builder includes a login form when encountering the argument marker tag,
	 * or the first marker tag if the argument is null. The form is POSTed to
	 * the same servlet path as the argument HTTP request. After attempted
	 * login, the user is forwarded to the argument URL. Specifying null as the
	 * forward URL will redirect to the same servlet path the login was
	 * submitted to.
	 * @param pageBuilderHost the HTML page builder host to use (usually, but
	 *            not necessarily the very servlet calling this method)
	 * @param request the HTTP request to create a login page builder for
	 * @param response the HTTP response to send the login page to
	 * @param markerTag the marker tag to include the login for for
	 * @param loginForwardUrl the URL to forward to after form submission
	 * @return a login page builder
	 * @throws IOException
	 */
	public HtmlPageBuilder getLoginPageBuilder(HtmlPageBuilderHost pageBuilderHost, HttpServletRequest request, HttpServletResponse response, final String markerTag, final String loginForwardUrl) throws IOException {
		final ArrayList apList = new ArrayList();
		for (Iterator apnit = this.authenticationProvidersByName.keySet().iterator(); apnit.hasNext();) {
			String apn = ((String) apnit.next());
			AuthenticationProvider ap = ((AuthenticationProvider) this.authenticationProvidersByName.get(apn));
			if ((ap != null) && ap.providesLoginFields())
				apList.add(ap);
		}
		
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		return new HtmlPageBuilder(pageBuilderHost, request, response) {
			protected void include(String type, String tag) throws IOException {
				if ((markerTag == null) || markerTag.equals(type)) this.includeLoginForm();
				else
					super.include(type, tag);
			}
			
			private void includeLoginForm() throws IOException {
				this.writeLine("<form id=\"webAppHost_loginForm\" method=\"POST\" action=\"" + this.request.getContextPath() + this.request.getServletPath() + "/webAppLogin\" onsubmit=\"return webAppHost_onLogin();\">");
				this.writeLine("<table class=\"webAppHost_loginTable loginTable\" id=\"webAppHost_loginTable\">");
				String forwardUrl = loginForwardUrl;
				if (forwardUrl == null)
					forwardUrl = this.request.getContextPath() + this.request.getServletPath();
				this.writeLine("<input type=\"hidden\" name=\"" + FORWARD_URL_PARAMETER + "\" value=\"" + forwardUrl + "\" />");
				
				this.writeLine("<tr>");
				this.writeLine("<td class=\"webAppHost_loginTableCell loginTableCell\" id=\"webAppHost_loginFieldCell\">");
				for (int p = 0; p < apList.size(); p++) {
					AuthenticationProvider ap = ((AuthenticationProvider) apList.get(p));
					this.writeLine("<div id=\"" + ap.getName() + "_fields\" style=\"display: none;\">");
					ap.writeLoginFields(this);
					this.writeLine("</div>");
				}
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				this.writeLine("<tr>");
				this.writeLine("<td class=\"webAppHost_loginTableCell loginTableCell\" id=\"webAppHost_apSelectorFieldCell\">");
				this.write("Authenticate&nbsp;via:&nbsp;");
				this.writeLine("<select name=\"" + AUTHENTICATION_PROVIDER_NAME_PARAMETER + "\" id=\"" + AUTHENTICATION_PROVIDER_NAME_PARAMETER + "_field\" onchange=\"authenticationProviderNameChanged();\">");
				for (int p = 0; p < apList.size(); p++) {
					AuthenticationProvider ap = ((AuthenticationProvider) apList.get(p));
					this.writeLine("<option value=\"" + ap.getName() + "\">" + ap.getLabel() + "</option>");
				}
				this.writeLine("</select>");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				this.writeLine("<tr>");
				this.writeLine("<td class=\"webAppHost_loginTableCell loginTableCell\" id=\"webAppHost_loginButtonCell\">");
				this.writeLine("<input type=\"submit\" value=\"Login\" class=\"webAppHost_button\" id=\"webAppHost_loginButton\" onclick=\"return webAppHost_onLoggingIn();\" />");
				this.writeLine("</td>");
				this.writeLine("</tr>");
				
				this.writeLine("</table>");
				this.writeLine("</form>");
			}
			
			protected String[] getOnloadCalls() {
				String[] olcs = {"authenticationProviderNameChanged();"};
				return olcs;
			}
			
			protected void writePageHeadExtensions() throws IOException {
				this.writeLine("<script type=\"text/javascript\">");
				this.writeLine("function getAuthenticationProviderName() {");
				this.writeLine("  var apnf = document.getElementById('" + AUTHENTICATION_PROVIDER_NAME_PARAMETER + "_field');");
				this.writeLine("  return ((apnf == null) ? '' : apnf.value);");
				this.writeLine("}");
				this.writeLine("var selectedAuthenticationProviderName = null;");
				this.writeLine("function authenticationProviderNameChanged() {");
				this.writeLine("  var apn = getAuthenticationProviderName();");
				this.writeLine("  var apnfs;");
				for (int p = 0; p < apList.size(); p++) {
					AuthenticationProvider ap = ((AuthenticationProvider) apList.get(p));
					this.writeLine("  apfs = document.getElementById('" + ap.getName() + "_fields');");
					this.writeLine("  if (apnfs != null)");
					this.writeLine("    apfs.style.display = ((apn == '" + ap.getName() + "') ? '' : 'none');");
					this.writeLine("  if (selectedAuthenticationProviderName == '" + ap.getName() + "')");
					this.writeLine("    " + ap.getName() + "_deselected();");
					this.writeLine("  if (apn == '" + ap.getName() + "') {");
					this.writeLine("    " + ap.getName() + "_selected();");
					this.writeLine("    window.webAppHost_onLoggingIn = window." + ap.getName() + "_onLoggingIn;");
					this.writeLine("    window.webAppHost_onLogin = window." + ap.getName() + "_onLogin;");
					this.writeLine("  }");
				}
				this.writeLine("  selectedAuthenticationProviderName = apn;");
				this.writeLine("}");
				this.writeLine("function webAppHost_onLoggingIn() {");
				this.writeLine("  return true;");
				this.writeLine("}");
				this.writeLine("function webAppHost_onLogin() {");
				this.writeLine("  return true;");
				this.writeLine("}");
				for (int p = 0; p < apList.size(); p++) {
					AuthenticationProvider ap = ((AuthenticationProvider) apList.get(p));
					this.writeLine("function " + ap.getName() + "_selected() {");
					ap.writeOnSelectFunctionBody(this);
					this.writeLine("}");
					this.writeLine("function " + ap.getName() + "_deselected() {");
					ap.writeOnDeselectFunctionBody(this);
					this.writeLine("}");
					this.writeLine("function " + ap.getName() + "_onLoggingIn() {");
					ap.writeOnLoggingInFunctionBody(this);
					this.writeLine("}");
					this.writeLine("function " + ap.getName() + "_onLogin() {");
					ap.writeOnLoginFunctionBody(this);
					this.writeLine("}");
				}
				this.writeLine("</script>");
			}
		};
	}
	
	/**
	 * Write user account manager code to some HTML page builder. After logout,
	 * the user is forwarded to the argument URL. Specifying null as the forward
	 * URL will redirect to the same servlet path the login was submitted to.
	 * Servlets wanting to include this kind of functionality in the HTML pages
	 * they produce have to call this method at some point from the page
	 * builders they use.
	 * @param pageBuilder the HTML page builder to write to
	 * @param logoutForwardUrl the URL to forward to after logout
	 * @throws IOException
	 */
	public void writeAccountManagerHtml(HtmlPageBuilder pageBuilder, String logoutForwardUrl) throws IOException {
		
		//	fetch required data
		String userName = this.getUserName(pageBuilder.request);
		AuthenticationProvider authProv = this.getAuthenticationProvider(pageBuilder.request);
		if ((userName == null) || (authProv == null))
			return;
		
		//	open div
		pageBuilder.writeLine("<div id=\"webAppHostAccountManager\" style=\"position: fixed; left: 0px; top: 0px; width: 100%; text-align: right; background-color: white; opacity: 0.3;\" onmouseover=\"webAppHostAccountManager_show(event);\" onmouseout=\"webAppHostAccountManager_hide(event);\" onclick=\"webAppHostAccountManager_onclick(event);\">");
		
		//	add configured links
		for (int l = 0; l < this.accountManagerLinks.length; l++)
			pageBuilder.writeLine("<a class=\"webAppHost_accountManagerLink\" href=\"#\" onclick=\"webAppHost_openAccountManagerLink('" + this.accountManagerLinks[l].label + "', '" + this.accountManagerLinks[l].href + "'); return false;\">" + this.accountManagerLinks[l].label + "</a>");
		
		//	add user name and auth provider display
		pageBuilder.writeLine("<span id=\"webAppHost_userNameDisplay\">" + userName + "</span>");
		pageBuilder.writeLine("<span id=\"webAppHost_authProviderDisplay\">(via " + authProv.getLabel() + ")</span>");
		
		//	add logout button and associated JavaScript
		String fwUrlQuery = "";
		if (logoutForwardUrl != null)
			fwUrlQuery = ("?" + FORWARD_URL_PARAMETER + "=" + URLEncoder.encode(logoutForwardUrl, "UTF-8"));
		pageBuilder.writeLine("<input type=\"button\" class=\"webAppHost_button\" id=\"webAppHost_logoutButton\" value=\"Logout\" onclick=\"webAppHost_doLogout();\" />");
		pageBuilder.writeLine("<script type=\"text/javascript\">");
		pageBuilder.writeLine("function webAppHost_doLogout() {");
		pageBuilder.writeLine("  window.location.href = '" + pageBuilder.request.getContextPath() + pageBuilder.request.getServletPath() + "/webAppLogout" + fwUrlQuery + "';");
		pageBuilder.writeLine("  return false;");
		pageBuilder.writeLine("}");
		pageBuilder.writeLine("function webAppHost_openAccountManagerLink(label, href) {");
		pageBuilder.writeLine("  window.open(href, label, '" + this.accountManagerLinkWindowParams + "');");
		pageBuilder.writeLine("}");
		pageBuilder.writeLine("</script>");
		
		//	add authentication provider specific code
		authProv.writeAccountManagerHtml(pageBuilder);
		
		//	include calls to mounting point functions
		String onShow = authProv.getAccountManagerOnshowCall();
		String onHide = authProv.getAccountManagerOnhideCall();
		String onClick = authProv.getAccountManagerOnclickCall();
		
		//	add JavaScript for showing and hiding
		pageBuilder.writeLine("<script type=\"text/javascript\">");
		pageBuilder.writeLine("function webAppHostAccountManager_show(event) {");
		pageBuilder.writeLine("  var am = document.getElementById('webAppHostAccountManager');");
		pageBuilder.writeLine("  if (am != null) {");
		pageBuilder.writeLine("    am.style.opacity = 1;");
		pageBuilder.writeLine("    if (webAppHost_isChild(am, event.relatedTarget))");
		pageBuilder.writeLine("      return;");
		pageBuilder.writeLine("  }");
		if (onShow != null)
			pageBuilder.writeLine("  " + onShow + (onShow.endsWith(";") ? "" : ";"));
		pageBuilder.writeLine("}");
		pageBuilder.writeLine("function webAppHostAccountManager_hide(event) {");
		pageBuilder.writeLine("  var am = document.getElementById('webAppHostAccountManager');");
		pageBuilder.writeLine("  if (am != null) {");
		pageBuilder.writeLine("    am.style.opacity = 0.3;");
		pageBuilder.writeLine("    if (webAppHost_isChild(am, event.relatedTarget))");
		pageBuilder.writeLine("      return;");
		pageBuilder.writeLine("  }");
		if (onHide != null)
			pageBuilder.writeLine("  " + onHide + (onHide.endsWith(";") ? "" : ";"));
		pageBuilder.writeLine("}");
		pageBuilder.writeLine("function webAppHostAccountManager_onclick(event) {");
		pageBuilder.writeLine("  var am = document.getElementById('webAppHostAccountManager');");
		pageBuilder.writeLine("  if ((am != null) && (event.target != am))");
		pageBuilder.writeLine("    return;");
		if (onClick != null)
			pageBuilder.writeLine("  " + onClick + (onClick.endsWith(";") ? "" : ";"));
		pageBuilder.writeLine("}");
		pageBuilder.writeLine("function webAppHost_isChild(element, eventTarget) {");
		pageBuilder.writeLine("  while (eventTarget != null) {");
		pageBuilder.writeLine("    if (eventTarget == element)");
		pageBuilder.writeLine("      return true;");
		pageBuilder.writeLine("    eventTarget = eventTarget.parentNode;");
		pageBuilder.writeLine("  }");
		pageBuilder.writeLine("  return false;");
		pageBuilder.writeLine("}");
		pageBuilder.writeLine("</script>");
		pageBuilder.writeLine("</div>");
	}
	
	/**
	 * Handle an HTTP request received by a servlet. This method only reads the
	 * path info of the HTTP request to check whether or not the request is
	 * directed here. Only if the latter applies, this method reads request
	 * parameters.
	 * @param request the HTTP request to handle
	 * @param response the HTTP response to write to
	 * @return true if the request has been handled, false otherwise
	 * @throws IOException
	 */
	public boolean handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//	check path info
		String pathInfo = request.getPathInfo();
		if (pathInfo == null)
			return false;
		
		//	login
		if ("/webAppLogin".equals(pathInfo)) {
			String apn = request.getParameter(AUTHENTICATION_PROVIDER_NAME_PARAMETER);
			String fwUrl = request.getParameter(FORWARD_URL_PARAMETER);
			if (fwUrl == null)
				fwUrl = request.getContextPath() + request.getServletPath();
			HttpSession session = request.getSession(false);
			if (session != null) {
				response.sendRedirect(fwUrl);
				return true;
			}
			
			if ((apn != null) && (apn.trim().length() != 0)) {
				AuthenticationProvider ap = ((AuthenticationProvider) this.authenticationProvidersByName.get(apn));
				if (ap != null) {
					String userName = ap.authenticate(request);
					if (userName != null) {
						session = request.getSession(true);
						session.setAttribute(this.authenticationProviderAttribute, ap);
						session.setAttribute(this.authenticatedUserNameAttribute, userName);
					}
				}
				response.sendRedirect(fwUrl);
				return true;
			}
			
			for (Iterator apnit = this.authenticationProvidersByName.keySet().iterator(); apnit.hasNext();) {
				apn = ((String) apnit.next());
				AuthenticationProvider ap = ((AuthenticationProvider) this.authenticationProvidersByName.get(apn));
				if (ap != null) {
					String userName = ap.authenticate(request);
					if (userName != null) {
						session = request.getSession(true);
						session.setAttribute(this.authenticationProviderAttribute, ap);
						session.setAttribute(this.authenticatedUserNameAttribute, userName);
					}
				}
			}
			response.sendRedirect(fwUrl);
			return true;
		}
		
		//	logout
		if ("/webAppLogout".equals(pathInfo)) {
			this.logout(request);
			String fwUrl = request.getParameter(FORWARD_URL_PARAMETER);
			if (fwUrl == null)
				fwUrl = request.getContextPath() + request.getServletPath();
			response.sendRedirect(fwUrl);
			return true;
		}
		
		//	request for specific authentication provider
		if ("/webAppAuthProvider".equals(pathInfo) || pathInfo.startsWith("/webAppAuthProvider/")) {
			
			//	target authentication provider identified by session
			AuthenticationProvider ap = this.getAuthenticationProvider(request);
			if (ap != null)
				return ap.handleRequest(request, response);
			
			//	target authentication provider identified by path
			if (!pathInfo.startsWith("/webAppAuthProvider/"))
				return false;
			String apn = pathInfo.substring("/webAppAuthProvider/".length());
			if (apn.indexOf('/') != -1)
				apn = apn.substring(0, apn.indexOf('/'));
			ap = ((AuthenticationProvider) this.authenticationProvidersByName.get(apn));
			if (ap != null)
				return ap.handleRequest(request, response);
		}
		
		//	this one'e not meant for us ...
		return false;
	}
}