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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import de.uka.ipd.idaho.easyIO.util.JsonParser;
import de.uka.ipd.idaho.easyIO.util.RandomByteSource;
import de.uka.ipd.idaho.easyIO.web.WebAppHost.AuthenticationProvider;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;

/**
 * Servlets of this class register authentication providers with the central
 * authentication facilities of a webapp, namely AuthenticationServlet. They
 * further receive the callbacks from OAuth providers.
 * 
 * @author sautter
 */	
public class OAuthConnectorServlet extends WebServlet {
	
	//	implemented as http://developers.google.com/accounts/docs/OAuth2Login
	
	//	useful for tests: http://developers.google.com/oauthplayground/
	
	private String authProviderName = "OAuthService";
	private String authProviderLabel = "Open Authentication Service";
	
	private String oAuthProviderLoginUrl;
	private String oAuthProviderClientId;
	private String oAuthProviderClientSecret;
	private String oAuthProviderResponseType;
	private String oAuthProviderScope;
	private String oAuthCallbackUrl;
	private String oAuthProviderAuthorizationCallbackUrl;
	private String oAuthProviderUserNameCallbackUrl;
	private String oAuthProviderUserNameResponseParameter;
	private String oAuthLoginWindowParams = "width=600,height=500,top=100,left=100,resizable=yes,scrollbar=yes,scrollbars=yes";
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.WebServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		
		//	get name and label
		this.authProviderName = this.getSetting("authProviderName", this.authProviderName);
		this.authProviderLabel = this.getSetting("authProviderLabel", this.authProviderLabel);
		
		//	read OAuth provider specific variables
		this.oAuthProviderLoginUrl = this.getSetting("oAuthProviderLoginUrl");
		if (this.oAuthProviderLoginUrl == null)
			throw new ServletException("OAuth provider login form URL missing.");
		this.oAuthProviderClientId = this.getSetting("oAuthProviderClientId");
		if (this.oAuthProviderClientId == null)
			throw new ServletException("OAuth provider client ID missing.");
		this.oAuthProviderClientSecret = this.getSetting("oAuthProviderClientSecret");
		if (this.oAuthProviderClientSecret == null)
			throw new ServletException("OAuth provider client secret missing.");
		this.oAuthProviderResponseType = this.getSetting("oAuthProviderResponseType");
		if (this.oAuthProviderResponseType == null)
			throw new ServletException("OAuth provider response type missing.");
		this.oAuthProviderScope = this.getSetting("oAuthProviderScope");
		if (this.oAuthProviderScope == null)
			throw new ServletException("OAuth provider pemission scope missing.");
		this.oAuthCallbackUrl = this.getSetting("oAuthCallbackUrl");
		if (this.oAuthCallbackUrl == null)
			throw new ServletException("Callback URL for OAuth provider missing.");
		this.oAuthProviderAuthorizationCallbackUrl = this.getSetting("oAuthProviderAuthorizationCallbackUrl");
		if (this.oAuthProviderAuthorizationCallbackUrl == null)
			throw new ServletException("OAuth provider authorization callback URL missing.");
		this.oAuthProviderUserNameCallbackUrl = this.getSetting("oAuthProviderUserNameCallbackUrl");
		if (this.oAuthProviderUserNameCallbackUrl == null)
			throw new ServletException("OAuth provider user name callback URL missing.");
		this.oAuthProviderUserNameResponseParameter = this.getSetting("oAuthProviderUserNameResponseParameter");
		if (this.oAuthProviderUserNameResponseParameter == null)
			throw new ServletException("Name of user name parameter in OAuth provider user name response missing.");
		this.oAuthLoginWindowParams = this.getSetting("oAuthLoginWindowParams", "width=600,height=500,top=100,left=100,resizable=yes,scrollbar=yes,scrollbars=yes");
		
		//	register authentication provider
		this.webAppHost.addAuthenticationProvider(new OaAuthProvider());
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		//	get state & code
		String state = request.getParameter("state");
		String code = request.getParameter("code");
		if ((state == null) || (code == null))
			return;
		
		//	try and get authorization from OAuth provider
		String authorization = null;
		try {
			URL atOAuthCallbackUrl = new URL(this.oAuthProviderAuthorizationCallbackUrl);
			HttpURLConnection atCon = ((HttpURLConnection) atOAuthCallbackUrl.openConnection());
			atCon.setDoOutput(true);
			atCon.setDoInput(true);
			atCon.setRequestMethod("POST");
			BufferedWriter atBw = new BufferedWriter(new OutputStreamWriter(atCon.getOutputStream(), "UTF-8"));
			atBw.write("code=" + URLEncoder.encode(code, "UTF-8"));
			atBw.write("&client_id=" + URLEncoder.encode(this.oAuthProviderClientId, "UTF-8"));
			atBw.write("&client_secret=" + URLEncoder.encode(this.oAuthProviderClientSecret, "UTF-8"));
			atBw.write("&redirect_uri=" + URLEncoder.encode(this.oAuthCallbackUrl, "UTF-8"));
			atBw.write("&grant_type=" + URLEncoder.encode("authorization_code", "UTF-8"));
			atBw.flush();
			Map atOAuthCallbackResponse = JsonParser.parseJson(new BufferedReader(new InputStreamReader(atCon.getInputStream(), "UTF-8")));
			Object accessToken = atOAuthCallbackResponse.get("access_token");
			Object idToken = atOAuthCallbackResponse.get("id_token");
			Object expiresIn = atOAuthCallbackResponse.get("expires_in");
			Object tokenType = atOAuthCallbackResponse.get("token_type");
			if ((accessToken == null) || (idToken == null) || (tokenType == null))
				disapprovedLoginTokens.put(state, new Long(System.currentTimeMillis()));
			else authorization = (tokenType.toString() + " " + accessToken.toString());
		}
		catch (IOException ioe) {
			errorLoginTokens.put(state, new Long(System.currentTimeMillis()));
			errorLoginTokensToErrors.setProperty(state, ("Could not get authorization from OAuth rovider: " + ioe.getMessage()));
		}
		if (authorization == null)
			return;
		
		//	get user name
		try {
			URL unOAuthCallbackUrl = new URL(this.oAuthProviderUserNameCallbackUrl);
			HttpURLConnection unCon = ((HttpURLConnection) unOAuthCallbackUrl.openConnection());
			unCon.setDoOutput(true);
			unCon.setDoInput(true);
			unCon.setRequestMethod("GET");
			unCon.setRequestProperty("Authorization", authorization);
			Map unOAuthCallbackResponse = JsonParser.parseJson(new BufferedReader(new InputStreamReader(unCon.getInputStream(), "UTF-8")));
			Object userName = unOAuthCallbackResponse.get(this.oAuthProviderUserNameResponseParameter);
			if (userName == null)
				disapprovedLoginTokens.put(state, new Long(System.currentTimeMillis()));
			else {
				approvedLoginTokens.put(state, new Long(System.currentTimeMillis()));
				approvedLoginTokensToUserNames.setProperty(state, userName.toString());
			}
		}
		catch (IOException ioe) {
			errorLoginTokens.put(state, new Long(System.currentTimeMillis()));
			errorLoginTokensToErrors.setProperty(state, ("Could not get user name from OAuth rovider: " + ioe.getMessage()));
		}
	}
	
	private LinkedHashMap approvedLoginTokens = new LinkedHashMap(16, 0.9f, false) {
		protected boolean removeEldestEntry(Entry eldest) {
			return (((Long) eldest.getValue()).longValue() < (System.currentTimeMillis() - (1000 * 60 * 60)));
		}
	};
	private LinkedHashMap disapprovedLoginTokens = new LinkedHashMap(16, 0.9f, false) {
		protected boolean removeEldestEntry(Entry eldest) {
			return (((Long) eldest.getValue()).longValue() < (System.currentTimeMillis() - (1000 * 60 * 60)));
		}
	};
	private LinkedHashMap errorLoginTokens = new LinkedHashMap(16, 0.9f, false) {
		protected boolean removeEldestEntry(Entry eldest) {
			return (((Long) eldest.getValue()).longValue() < (System.currentTimeMillis() - (1000 * 60 * 60)));
		}
	};
	private Properties approvedLoginTokensToUserNames = new Properties();
	private Properties errorLoginTokensToErrors = new Properties();
	
	private class OaAuthProvider extends AuthenticationProvider {
		public String getName() {
			return authProviderName;
		}
		public String getLabel() {
			return authProviderLabel;
		}
		public String authenticate(HttpServletRequest request) {
			
			//	get login token
			String loginToken = request.getParameter(this.getName() + "_loginToken");
			if (loginToken == null)
				return null;
			
			//	check session login token
			HttpSession session = request.getSession(false);
			if ((session == null) || !loginToken.equals(session.getAttribute(this.getName() + "_loginToken")))
				return null;
			
			//	user name is only set login token is authenticated
			return approvedLoginTokensToUserNames.getProperty(loginToken);
		}
		public boolean providesLoginFields() {
			return true;
		}
		public void sessionLoggingOut(HttpSession session) {
			session.removeAttribute(this.getName() + "_loginToken");
		}
		public void writeLoginFields(HtmlPageBuilder pageBuilder) throws IOException {
			String loginToken = ((String) pageBuilder.request.getSession(true).getAttribute(this.getName() + "_loginToken"));
			if (loginToken == null) {
				loginToken = RandomByteSource.getGUID();
				pageBuilder.request.getSession(true).setAttribute((this.getName() + "_loginToken"), loginToken);
			}
			pageBuilder.writeLine("<input type=\"hidden\" name=\"" + this.getName() + "_loginToken\" value=\"" + loginToken + "\">");
			pageBuilder.writeLine("<iframe id=\"" + this.getName() + "_checkLoginFrame\" src=\"#\" style=\"width: 100%; height: 100%; border: 0px;\"></iframe>");
			pageBuilder.writeLine("<script type=\"\">");
			pageBuilder.writeLine("var " + this.getName() + "_loginToken = '" + loginToken + "';");
			pageBuilder.writeLine("var " + this.getName() + "_loginWindow = null;");
			pageBuilder.writeLine("var " + this.getName() + "_loginConfirmWaitCount = 0;");
			pageBuilder.writeLine("var " + this.getName() + "_loginConfirmCheckCount = 0;");
			pageBuilder.writeLine("var " + this.getName() + "_loginConfirmed = false;");
			pageBuilder.writeLine("function " + this.getName() + "_checkLoginConfirmed() {");
			pageBuilder.writeLine("  var clf = getById('" + this.getName() + "_checkLoginFrame');");
			//	wait for data to load completely
			pageBuilder.writeLine("  if (!clf.contentWindow.loginStatus) {");
			pageBuilder.writeLine("    " + this.getName() + "_loginConfirmWaitCount++;");
			pageBuilder.writeLine("    if (" + this.getName() + "_loginConfirmWaitCount > 20) {");
			pageBuilder.writeLine("      alert('Login failed, server not reachable.');");
			pageBuilder.writeLine("      if (" + this.getName() + "_loginWindow != null)");
			pageBuilder.writeLine("        " + this.getName() + "_loginWindow.close();");
			pageBuilder.writeLine("    }");
			pageBuilder.writeLine("    else window.setTimeout('" + this.getName() + "_checkLoginConfirmed()', 250);");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  }");
			//	backend waiting for OAuth provider callback
			pageBuilder.writeLine("  if (clf.contentWindow.loginStatus == 'WAITING') {");
			pageBuilder.writeLine("    " + this.getName() + "_loginConfirmCheckCount++;");
			pageBuilder.writeLine("    " + this.getName() + "_loginConfirmWaitCount = 0;");
			pageBuilder.writeLine("    if (" + this.getName() + "_loginConfirmCheckCount > 2400) {");
			pageBuilder.writeLine("      alert('Login failed, " + this.getLabel() + " took too long to check credentials.');");
			pageBuilder.writeLine("      if (" + this.getName() + "_loginWindow != null)");
			pageBuilder.writeLine("        " + this.getName() + "_loginWindow.close();");
			pageBuilder.writeLine("    }");
			pageBuilder.writeLine("    else {");
			pageBuilder.writeLine("      clf.src = '" + pageBuilder.request.getContextPath() + pageBuilder.request.getServletPath() + "/webAppAuthProvider/" + this.getName() + "/confirmLogin?loginToken=" + loginToken + "';");
			pageBuilder.writeLine("      window.setTimeout('" + this.getName() + "_checkLoginConfirmed()', 250);");
			pageBuilder.writeLine("    }");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  }");
			//	login confirmed
			pageBuilder.writeLine("  if (clf.contentWindow.loginStatus == 'OK') {");
			pageBuilder.writeLine("    " + this.getName() + "_loginConfirmed = true;");
			pageBuilder.writeLine("    if (" + this.getName() + "_loginWindow != null)");
			pageBuilder.writeLine("      " + this.getName() + "_loginWindow.close();");
			pageBuilder.writeLine("    getById('webAppHost_loginForm').submit();");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  }");
			//	login declined by user
			pageBuilder.writeLine("  if (clf.contentWindow.loginStatus == 'DECLINED') {");
			pageBuilder.writeLine("    if (" + this.getName() + "_loginWindow != null)");
			pageBuilder.writeLine("      " + this.getName() + "_loginWindow.close();");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  }");
			//	some other error
			pageBuilder.writeLine("  if (clf.contentWindow.loginStatus == 'ERROR') {");
			pageBuilder.writeLine("    alert('Login failed:\\n' + clf.contentWindow.loginError);");
			pageBuilder.writeLine("    if (" + this.getName() + "_loginWindow != null)");
			pageBuilder.writeLine("      " + this.getName() + "_loginWindow.close();");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("}");
			pageBuilder.writeLine("</script>");
		}
		
		public void writeOnLoggingInFunctionBody(HtmlPageBuilder pageBuilder) throws IOException {
			//	open OAuth provider login window
			String oAuthProviderLoginWindowUrl = oAuthProviderLoginUrl +
					"?client_id=" + URLEncoder.encode(oAuthProviderClientId, "UTF-8") +
					"&response_type=" + URLEncoder.encode(oAuthProviderResponseType, "UTF-8") +
					"&scope=" + URLEncoder.encode(oAuthProviderScope, "UTF-8") +
					"&redirect_uri=" + URLEncoder.encode(oAuthCallbackUrl, "UTF-8") +
					"&state=" +
					"";
			pageBuilder.writeLine("  " + this.getName() + "_loginWindow = window.open(('" + oAuthProviderLoginWindowUrl + "' + " + this.getName() + "_loginToken), 'OAuth Login', '" + oAuthLoginWindowParams + "', true);");
			
			//	start polling backend for received callback
			pageBuilder.writeLine("  var clf = getById('" + this.getName() + "_checkLoginFrame');");
			pageBuilder.writeLine("  clf.src = ('" + pageBuilder.request.getContextPath() + pageBuilder.request.getServletPath() + "/webAppAuthProvider/" + this.getName() + "/confirmLogin?loginToken=' + " + this.getName() + "_loginToken);");
			pageBuilder.writeLine("  window.setTimeout('" + this.getName() + "_checkLoginConfirmed()', 250);");
			
			//	prevent login form submission for now
			pageBuilder.writeLine("  return false;");
		}
		public void writeOnLoginFunctionBody(HtmlPageBuilder pageBuilder) throws IOException {
			pageBuilder.writeLine("  return " + this.getName() + "_loginConfirmed;");
		}
		public boolean handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			
			//	check if request belongs here
			String pathInfo = request.getPathInfo();
			if ((pathInfo == null) || !pathInfo.startsWith("/webAppAuthProvider/" + this.getName() + "/confirmLogin"))
				return super.handleRequest(request, response);
			
			//	login confirmation request
			if (pathInfo.endsWith("/confirmLogin")) {
				
				//	get login token
				String loginToken = request.getParameter("loginToken");
				if (loginToken == null)
					return super.handleRequest(request, response);
				
				//	check login status
				String loginStatus = null;
				
				//	check if user approved login
				if (loginStatus == null) {
					if (approvedLoginTokens.containsKey(loginToken))
						loginStatus = "OK";
				}
				
				//	check if user declined login
				if (loginStatus == null) {
					if (disapprovedLoginTokens.containsKey(loginToken))
						loginStatus = "DECLINED";
				}
				
				//	check if error occurred
				if (loginStatus == null) {
					if (errorLoginTokens.containsKey(loginToken))
						loginStatus = "ERROR";
				}
				
				//	check if callback still pending
				if (loginStatus == null)
					loginStatus = "WAITING";
				
				//	send status of login token
				response.setContentType("text/html");
				response.setCharacterEncoding("UTF-8");
				Writer out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
				BufferedWriter bw = new BufferedWriter(out);
				bw.write("<html><body>");bw.newLine();
				bw.write("<script type=\"text/javascript\">");bw.newLine();
				bw.write("var loginStatus = '" + loginStatus + "';");bw.newLine();
				bw.write("</script>");bw.newLine();
				bw.write("</body></html>");bw.newLine();
				bw.flush();
				return true;
			}
			
			//	let super class handle request
			return super.handleRequest(request, response);
		}
	}
}