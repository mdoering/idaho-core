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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.tanesha.recaptcha.ReCaptcha;
import net.tanesha.recaptcha.ReCaptchaFactory;
import net.tanesha.recaptcha.ReCaptchaImpl;
import net.tanesha.recaptcha.ReCaptchaResponse;
import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.IoProvider;
import de.uka.ipd.idaho.easyIO.SqlQueryResult;
import de.uka.ipd.idaho.easyIO.sql.TableDefinition;
import de.uka.ipd.idaho.easyIO.web.WebAppHost.AuthenticationProvider;
import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;

/**
 * Servlet providing authentication via e-mail verified self-registration
 * accounts.
 * 
 * @author sautter
 */
public class AuthenticationServlet extends HtmlServlet {
	
	private static final String USER_NAME_PARAMETER = "UserName";
	private static final String PSWD_PARAMETER = "Pswd";
	private static final String PSWD_HASH_PARAMETER = "PswdHash";
	private static final String CREATE_TIME_PARAMETER = "CreateTime";
	private static final String LAST_ACCESS_PARAMETER = "LastAccess";
	
	private WebAppHost webAppHost;
	private IoProvider io;
	
	private String authProviderName = "SelfRegAuth";
	private String authProviderLabel = "Self Registration Service";
	
	private String reCaptchaPublicKey;
	private String reCaptchaPrivateKey;
	private boolean useReCaptcha = false;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.easyIO.web.HtmlServlet#doInit()
	 */
	protected void doInit() throws ServletException {
		super.doInit();
		
		//	connect to host for
		this.webAppHost = WebAppHost.getInstance(this.getServletContext());
		
		//	get name and label
		this.authProviderName = this.getSetting("authProviderName", this.authProviderName);
		this.authProviderLabel = this.getSetting("authProviderLabel", this.authProviderLabel);
		
		//	get IoProvider from host
		this.io = this.webAppHost.getIoProvider();
		if (!this.io.isJdbcAvailable())
			throw new RuntimeException("AuthenticationServlet: Cannot work without database");
		
		//	create account table
		TableDefinition td = new TableDefinition(this.authProviderName + "Data");
		td.addColumn(USER_NAME_PARAMETER, TableDefinition.VARCHAR_DATATYPE, 32);
		td.addColumn(PSWD_HASH_PARAMETER, TableDefinition.INT_DATATYPE, 0);
		td.addColumn(CREATE_TIME_PARAMETER, TableDefinition.BIGINT_DATATYPE, 0);
		td.addColumn(LAST_ACCESS_PARAMETER, TableDefinition.BIGINT_DATATYPE, 0);
		if (!this.io.ensureTable(td, true))
			throw new RuntimeException("AuthenticationServlet: Cannot work without database");
		
		//	index account table
		this.io.indexColumn((this.authProviderName + "Data"), USER_NAME_PARAMETER);
		this.io.indexColumn((this.authProviderName + "Data"), PSWD_HASH_PARAMETER);
		
		//	load reCAPTCHA keys
		this.reCaptchaPublicKey = this.getSetting("reCaptchaPublicKey", this.reCaptchaPublicKey);
		this.reCaptchaPrivateKey = this.getSetting("reCaptchaPrivateKey", this.reCaptchaPrivateKey);
		this.useReCaptcha = ((this.reCaptchaPublicKey != null) && (this.reCaptchaPrivateKey != null));
		
		//	register authentication provider
		this.webAppHost.addAuthenticationProvider(new AsAuthProvider());
	}
	
	private class AsAuthProvider extends AuthenticationProvider {
		public String getName() {
			return authProviderName;
		}
		public String getLabel() {
			return authProviderLabel;
		}
		public String authenticate(HttpServletRequest request) {
			
			//	get login parameters
			String userName = request.getParameter(this.getName() + "_" + USER_NAME_PARAMETER);
			String password = request.getParameter(this.getName() + "_" + PSWD_PARAMETER);
			System.out.println(" - username is " + userName);
			System.out.println(" - password is " + password);
			
			//	not a login request, at least not for this authentication provider
			if ((userName == null) || (password == null))
				return null;
			
			//	update last access time for user/pswd pair ==> login successful if one row updated
			String query = "UPDATE " + (authProviderName + "Data") + 
					" SET " + LAST_ACCESS_PARAMETER + " = " + System.currentTimeMillis() + 
					" WHERE " + PSWD_HASH_PARAMETER + " = " + password.hashCode() + 
					" AND " + USER_NAME_PARAMETER + " LIKE '" + EasyIO.sqlEscape(userName) + "'" +
					";";
			try {
				int updated = io.executeUpdateQuery(query);
				if (updated == 1) {
					request.getSession(true);
					return userName;
				}
				else return null;
			}
			catch (SQLException sqle) {
				System.out.println("Could not authenticate user '" + userName + "' - database not accessible.");
				System.out.println("  query was " + query);
				return null;
			}
		}
		public boolean providesLoginFields() {
			return true;
		}
		public void writeLoginFields(HtmlPageBuilder pageBuilder) throws IOException {
			
			//	provide login fields
			pageBuilder.writeLine("<table class=\"" + this.getName() + "_loginTable loginTable\">");
			pageBuilder.writeLine("<tr>");
			
			pageBuilder.write("<td class=\"" + this.getName() + "_loginTableLabelCell loginTableLabelCell\">");
			pageBuilder.write("User&nbsp;Name");
			pageBuilder.writeLine("</td>");
			pageBuilder.write("<td class=\"" + this.getName() + "_loginTableFieldCell loginTableFieldCell\">");
			pageBuilder.write("<input type=\"text\" id=\"" + this.getName() + "_" + USER_NAME_PARAMETER + "_input\" name=\"" + this.getName() + "_" + USER_NAME_PARAMETER + "\" />");
			pageBuilder.writeLine("</td>");
			
			pageBuilder.write("<td class=\"" + this.getName() + "_loginTableLabelCell loginTableLabelCell\">");
			pageBuilder.write("Password");
			pageBuilder.writeLine("</td>");
			pageBuilder.write("<td class=\"" + this.getName() + "_loginTableFieldCell loginTableFieldCell\">");
			pageBuilder.write("<input type=\"password\" id=\"" + this.getName() + "_" + PSWD_PARAMETER + "_input\" name=\"" + this.getName() + "_" + PSWD_PARAMETER + "\" />");
			pageBuilder.writeLine("</td>");
			
			pageBuilder.writeLine("</tr>");
			pageBuilder.writeLine("</table>");
			
			//	add link for registration (popup div)
			pageBuilder.writeLine("<span class=\"" + this.getName() + "_loginFunctionLink loginFunctionLink\">");
			pageBuilder.write("<a class=\"" + this.getName() + "_loginFunctionLink loginFunctionLink\" href=\"#\" onclick=\"return " + this.getName() + "_prepareCreateAccountDialog();\">");
			pageBuilder.write("No account? Click here to create one!");
			pageBuilder.writeLine("</a>");
			pageBuilder.writeLine("</span>");
			
			//	add popup div for registration
			pageBuilder.writeLine("<div id=\"" + this.getName() + "_CreateAccountScreen\" style=\"position: fixed; left: 0px; top: 0px; width: 100%; height: 100%; display: none; background-color: black; opacity: 0.3;\"></div>");
			pageBuilder.writeLine("<div id=\"" + this.getName() + "_CreateAccountDialog\" style=\"position: fixed; left: 0px; top: 0px; width: 100px; height: 100px; display: none; background-color: white; opacity: 1;\">");
			pageBuilder.writeLine("<iframe id=\"" + this.getName() + "_CreateAccountFrame\" src=\"#\" style=\"width: 100%; height: 100%; border: 0px;\"></iframe>");
			pageBuilder.writeLine("<input type=\"button\" id=\"" + this.getName() + "_createAccountButton\" value=\"Create Account\" onclick=\"" + this.getName() + "_doCreateAccount();\" />");
			pageBuilder.writeLine("<input type=\"button\" id=\"" + this.getName() + "_cancelAccountButton\" value=\"Cancel\" onclick=\"" + this.getName() + "_doCancelAccount();\" />");
			pageBuilder.writeLine("</div>");
			
			//	add JavaScript functions for registration
			pageBuilder.writeLine("<script type=\"text/javascript\">");
			pageBuilder.writeLine("function " + this.getName() + "_prepareCreateAccountDialog() {");
			pageBuilder.writeLine("  var caf = document.getElementById('" + this.getName() + "_CreateAccountFrame');");
			pageBuilder.writeLine("  if (caf != null) {");
			pageBuilder.writeLine("    caf.src = '" + pageBuilder.request.getContextPath() + pageBuilder.request.getServletPath() + "/webAppAuthProvider/" + this.getName() + "/caForm" + "';");
			pageBuilder.writeLine("    window.setTimeout('" + this.getName() + "_openCreateAccountDialog()', 100);");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("  return false;");
			pageBuilder.writeLine("}");
			
			pageBuilder.writeLine("function " + this.getName() + "_openCreateAccountDialog() {");
			pageBuilder.writeLine("  var caf = document.getElementById('" + this.getName() + "_CreateAccountFrame');");
			pageBuilder.writeLine("  if (caf == null)");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  var car = caf.contentWindow.document.getElementById('createAccountResult');");
			pageBuilder.writeLine("  if (car == null) {");
			pageBuilder.writeLine("    window.setTimeout('" + this.getName() + "_openCreateAccountDialog()', 100);");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("  var cas = document.getElementById('" + this.getName() + "_CreateAccountScreen');");
			pageBuilder.writeLine("  var cad = document.getElementById('" + this.getName() + "_CreateAccountDialog');");
			pageBuilder.writeLine("  if ((cas == null) || (cad == null))");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  cas.style.display = '';");
			pageBuilder.writeLine("  cad.style.display = '';");
			pageBuilder.writeLine("  var fWidth = " + this.getName() + "_getDocWidth(caf.contentWindow.document);");
			pageBuilder.writeLine("  var fHeight = " + this.getName() + "_getDocHeight(caf.contentWindow.document);");
			pageBuilder.writeLine("  caf.style.width = fWidth;");
			pageBuilder.writeLine("  caf.style.height = fHeight;");
			pageBuilder.writeLine("  cad.style.width = fWidth;");
			pageBuilder.writeLine("  cad.style.height = fHeight + 30;");
			pageBuilder.writeLine("  cad.style.left = (cas.offsetWidth - fWidth) / 2;");
			pageBuilder.writeLine("  cad.style.top = (cas.offsetHeight - fHeight - 30) / 2;");
			pageBuilder.writeLine("}");
			
			pageBuilder.writeLine("function " + this.getName() + "_doCreateAccount() {");
			pageBuilder.writeLine("  var cafr = document.getElementById('" + this.getName() + "_CreateAccountFrame');");
			pageBuilder.writeLine("  if (cafr == null)");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  var caf = cafr.contentWindow.document.getElementById('createAccountForm');");
			pageBuilder.writeLine("  if (caf != null) {");
			pageBuilder.writeLine("    if (!cafr.contentWindow.checkCreateAccountData())");
			pageBuilder.writeLine("      return;");
			pageBuilder.writeLine("    caf.submit();");
			pageBuilder.writeLine("    window.setTimeout('" + this.getName() + "_readCreateAccountResult()', 100);");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("  else " + this.getName() + "_doCancelAccount();");
			pageBuilder.writeLine("}");
			
			pageBuilder.writeLine("function " + this.getName() + "_readCreateAccountResult() {");
			pageBuilder.writeLine("  var cafr = document.getElementById('" + this.getName() + "_CreateAccountFrame');");
			pageBuilder.writeLine("  var car = cafr.contentWindow.document.getElementById('createAccountResult');");
			pageBuilder.writeLine("  if ((car == null) || (car.value == ''))");
			pageBuilder.writeLine("    window.setTimeout('" + this.getName() + "_readCreateAccountResult()', 100);");
			pageBuilder.writeLine("  else if (car.value == 'OK') {");
			pageBuilder.writeLine("    " + this.getName() + "_closeCreateAccountDialog();");
			pageBuilder.writeLine("    var lf = document.getElementById('webAppHost_loginForm');");
			pageBuilder.writeLine("    if (lf == null)");
			pageBuilder.writeLine("      return;");
			pageBuilder.writeLine("    var unf = cafr.contentWindow.document.getElementById('createAccountUn');");
			pageBuilder.writeLine("    var uni = document.getElementById('" + this.getName() + "_" + USER_NAME_PARAMETER + "_input');");
			pageBuilder.writeLine("    if ((unf == null) || (uni == null))");
			pageBuilder.writeLine("      return;");
			pageBuilder.writeLine("    uni.value = unf.value;");
			pageBuilder.writeLine("    var pwdf = cafr.contentWindow.document.getElementById('createAccountPwd');");
			pageBuilder.writeLine("    var pwdi = document.getElementById('" + this.getName() + "_" + PSWD_PARAMETER + "_input');");
			pageBuilder.writeLine("    if ((pwdf == null) || (pwdi == null))");
			pageBuilder.writeLine("      return;");
			pageBuilder.writeLine("    pwdi.value = pwdf.value;");
			pageBuilder.writeLine("    lf.submit();");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("  else {");
			pageBuilder.writeLine("    alert(car.value);");
			pageBuilder.writeLine("    car.value = '';");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("}");
			
			pageBuilder.writeLine("function " + this.getName() + "_doCancelAccount() {");
			pageBuilder.writeLine("  " + this.getName() + "_closeCreateAccountDialog();");
			pageBuilder.writeLine("  var caf = document.getElementById('" + this.getName() + "_CreateAccountFrame');");
			pageBuilder.writeLine("  if (caf != null)");
			pageBuilder.writeLine("    caf.src = '#';");
			pageBuilder.writeLine("}");
			
			pageBuilder.writeLine("function " + this.getName() + "_closeCreateAccountDialog() {");
			pageBuilder.writeLine("  var cas = document.getElementById('" + this.getName() + "_CreateAccountScreen');");
			pageBuilder.writeLine("  if (cas != null)");
			pageBuilder.writeLine("    cas.style.display = 'none';");
			pageBuilder.writeLine("  var cad = document.getElementById('" + this.getName() + "_CreateAccountDialog');");
			pageBuilder.writeLine("  if (cad != null)");
			pageBuilder.writeLine("    cad.style.display = 'none';");
			pageBuilder.writeLine("}");
			
			pageBuilder.writeLine("function " + this.getName() + "_getDocWidth(doc) {");
			pageBuilder.writeLine("  return Math.max(");
			pageBuilder.writeLine("    Math.max(doc.body.scrollWidth, doc.documentElement.scrollWidth),");
			pageBuilder.writeLine("    Math.max(doc.body.offsetWidth, doc.documentElement.offsetWidth),");
			pageBuilder.writeLine("    Math.max(doc.body.clientWidth, doc.documentElement.clientWidth)");
			pageBuilder.writeLine("  );");
			pageBuilder.writeLine("}");
			pageBuilder.writeLine("function " + this.getName() + "_getDocHeight(doc) {");
			pageBuilder.writeLine("  return Math.max(");
			pageBuilder.writeLine("    Math.max(doc.body.scrollHeight, doc.documentElement.scrollHeight),");
			pageBuilder.writeLine("    Math.max(doc.body.offsetHeight, doc.documentElement.offsetHeight),");
			pageBuilder.writeLine("    Math.max(doc.body.clientHeight, doc.documentElement.clientHeight)");
			pageBuilder.writeLine("  );");
			pageBuilder.writeLine("}");
			pageBuilder.writeLine("</script>");
		}
		public void writeAccountManagerHtml(HtmlPageBuilder pageBuilder) throws IOException {
			pageBuilder.writeLine("<input type=\"button\" id=\"" + this.getName() + "_ChangePasswordButton\" value=\"Change Password\" onclick=\"" + this.getName() + "_showChangePasswordFields();\" />");
			
			pageBuilder.writeLine("<div id=\"" + this.getName() + "_ChangePasswordFields\" style=\"position: absolute; display: none;\" onmouseover=\"" + this.getName() + "_changePasswordFields_mouseOver(event);\" onmouseout=\"" + this.getName() + "_changePasswordFields_mouseOut(event);\">");
			pageBuilder.writeLine("<table class=\"" + this.getName() + "_ChangePasswordFieldTable\">");
			
			pageBuilder.write("<tr>");
			pageBuilder.write("<td class=\"" + this.getName() + "_ChangePasswordFieldLabelCell\">");
			pageBuilder.write("Old&nbsp;Password");
			pageBuilder.write("</td>");
			pageBuilder.write("<td class=\"" + this.getName() + "_ChangePasswordFieldCell\">");
			pageBuilder.write("<input type=\"password\" id=\"" + this.getName() + "_oldPwd_input\" name=\"" + this.getName() + "_oldPwd" + "\" />");
			pageBuilder.write("</td>");
			pageBuilder.writeLine("</tr>");
			
			pageBuilder.write("<tr>");
			pageBuilder.write("<td class=\"" + this.getName() + "_ChangePasswordFieldLabelCell\">");
			pageBuilder.write("New&nbsp;Password");
			pageBuilder.write("</td>");
			pageBuilder.write("<td class=\"" + this.getName() + "_ChangePasswordFieldCell\">");
			pageBuilder.write("<input type=\"password\" id=\"" + this.getName() + "_newPwd_input\" name=\"" + this.getName() + "_newPwd" + "\" />");
			pageBuilder.write("</td>");
			pageBuilder.writeLine("</tr>");
			
			pageBuilder.write("<tr>");
			pageBuilder.write("<td class=\"" + this.getName() + "_ChangePasswordFieldLabelCell\">");
			pageBuilder.write("Confirm&nbsp;Password");
			pageBuilder.write("</td>");
			pageBuilder.write("<td class=\"" + this.getName() + "_ChangePasswordFieldCell\">");
			pageBuilder.write("<input type=\"password\" id=\"" + this.getName() + "_confirmPwd_input\" name=\"" + this.getName() + "_confirmPwd" + "\" />");
			pageBuilder.write("</td>");
			pageBuilder.writeLine("</tr>");
			
			pageBuilder.write("<tr>");
			pageBuilder.write("<td class=\"" + this.getName() + "_ChangePasswordButtonCell\" colspan=\"2\">");
			pageBuilder.write("<input type=\"button\" id=\"" + this.getName() + "_changePwdButton\" value=\"Change Password\" onclick=\"" + this.getName() + "_doChangePassword();\" />");
			pageBuilder.write("</td>");
			pageBuilder.writeLine("</tr>");
			
			pageBuilder.writeLine("</table>");
			pageBuilder.writeLine("</div>");
			pageBuilder.writeLine("<iframe id=\"" + this.getName() + "_ChangePasswordFrame\" style=\"width: 0px; height: 0px; border-width: 0px;\"></iframe>");
			
			pageBuilder.writeLine("<script type=\"text/javascript\">");
			
			pageBuilder.writeLine("var mouseOverCpf = false;");
			pageBuilder.writeLine("function " + this.getName() + "_changePasswordFields_mouseOver(event) {");
			pageBuilder.writeLine("  var cpf = document.getElementById('" + this.getName() + "_ChangePasswordFields');");
			pageBuilder.writeLine("  if ((cpf != null) && !webAppHost_isChild(cpf, event.relatedTarget))");
			pageBuilder.writeLine("    mouseOverCpf = true;");
			pageBuilder.writeLine("}");
			pageBuilder.writeLine("function " + this.getName() + "_changePasswordFields_mouseOut(event) {");
			pageBuilder.writeLine("  if (!mouseOverCpf)");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  var cpf = document.getElementById('" + this.getName() + "_ChangePasswordFields');");
			pageBuilder.writeLine("  if ((cpf != null) && !webAppHost_isChild(cpf, event.relatedTarget))");
			pageBuilder.writeLine("    cpf.style.display = 'none';");
			pageBuilder.writeLine("}");
			pageBuilder.writeLine("function " + this.getName() + "_showChangePasswordFields() {");
			pageBuilder.writeLine("  var cpb = document.getElementById('" + this.getName() + "_ChangePasswordButton');");
			pageBuilder.writeLine("  var cpf = document.getElementById('" + this.getName() + "_ChangePasswordFields');");
			pageBuilder.writeLine("  if ((cpb == null) || (cpf == null))");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  mouseOverCpf = false;");
			pageBuilder.writeLine("  var opi = document.getElementById('" + this.getName() + "_oldPwd_input');");
			pageBuilder.writeLine("  if (opi != null)");
			pageBuilder.writeLine("    opi.value = '';");
			pageBuilder.writeLine("  var npi = document.getElementById('" + this.getName() + "_newPwd_input');");
			pageBuilder.writeLine("  if (npi != null)");
			pageBuilder.writeLine("    npi.value = '';");
			pageBuilder.writeLine("  var cpi = document.getElementById('" + this.getName() + "_confirmPwd_input');");
			pageBuilder.writeLine("  if (cpi != null)");
			pageBuilder.writeLine("    cpi.value = '';");
			pageBuilder.writeLine("  cpf.style.display = '';");
			pageBuilder.writeLine("  var cpbMidX = cpb.offsetLeft + (cpb.offsetWidth / 2);");
			pageBuilder.writeLine("  var cpbMidY = cpb.offsetTop + (cpb.offsetHeight / 2);");
			pageBuilder.writeLine("  var cpbObj = cpb;");
			pageBuilder.writeLine("  while (cpbObj = cpbObj.offsetParent) {");
			pageBuilder.writeLine("    cpbMidX += cpbObj.offsetLeft;");
			pageBuilder.writeLine("    cpbMidY += cpbObj.offsetTop;");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("  cpf.style.left = cpbMidX - cpf.offsetWidth;");
			pageBuilder.writeLine("  cpf.style.top = cpbMidY;");
			pageBuilder.writeLine("  if (opi != null)");
			pageBuilder.writeLine("    opi.focus();");
			pageBuilder.writeLine("}");
			
			pageBuilder.writeLine("function " + this.getName() + "_doChangePassword() {");
			pageBuilder.writeLine("  var opi = document.getElementById('" + this.getName() + "_oldPwd_input');");
			pageBuilder.writeLine("  if (opi == null)");
			pageBuilder.writeLine("    return false;");
			pageBuilder.writeLine("  var npi = document.getElementById('" + this.getName() + "_newPwd_input');");
			pageBuilder.writeLine("  if (npi == null)");
			pageBuilder.writeLine("    return false;");
			pageBuilder.writeLine("  var cpi = document.getElementById('" + this.getName() + "_confirmPwd_input');");
			pageBuilder.writeLine("  if (cpi == null)");
			pageBuilder.writeLine("    return false;");
			pageBuilder.writeLine("  if (opi.value == npi.value) {");
			pageBuilder.writeLine("    mouseOverCpf = false;");
			pageBuilder.writeLine("    alert('The new password matches the old one.');");
			pageBuilder.writeLine("    npi.value = '';");
			pageBuilder.writeLine("    cpi.value = '';");
			pageBuilder.writeLine("    var cpfs = document.getElementById('" + this.getName() + "_ChangePasswordFields');");
			pageBuilder.writeLine("    if (cpfs != null)");
			pageBuilder.writeLine("      cpfs.style.display = '';");
			pageBuilder.writeLine("    npi.focus();");
			pageBuilder.writeLine("    return false;");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("  if (npi.value != cpi.value) {");
			pageBuilder.writeLine("    mouseOverCpf = false;");
			pageBuilder.writeLine("    alert('The new password does not match its confirmation.');");
			pageBuilder.writeLine("    npi.value = '';");
			pageBuilder.writeLine("    cpi.value = '';");
			pageBuilder.writeLine("    var cpfs = document.getElementById('" + this.getName() + "_ChangePasswordFields');");
			pageBuilder.writeLine("    if (cpfs != null)");
			pageBuilder.writeLine("      cpfs.style.display = '';");
			pageBuilder.writeLine("    npi.focus();");
			pageBuilder.writeLine("    return false;");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("  var cpfr = document.getElementById('" + this.getName() + "_ChangePasswordFrame');");
			pageBuilder.writeLine("  var cpfo = cpfr.contentWindow.document.getElementById('changePwdForm');");
			pageBuilder.writeLine("  if (cpfo == null) {");
			pageBuilder.writeLine("    if (cpfr.src != '" + pageBuilder.request.getContextPath() + pageBuilder.request.getServletPath() + "/webAppAuthProvider/cpForm" + "')");
			pageBuilder.writeLine("      cpfr.src = '" + pageBuilder.request.getContextPath() + pageBuilder.request.getServletPath() + "/webAppAuthProvider/cpForm" + "';");
			pageBuilder.writeLine("    window.setTimeout('" + this.getName() + "_doChangePassword()', 100);");
			pageBuilder.writeLine("    return false;");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("  else {");
			pageBuilder.writeLine("    var opf = cpfr.contentWindow.document.getElementById('oldPwd_field');");
			pageBuilder.writeLine("    if (opf == null)");
			pageBuilder.writeLine("      return false;");
			pageBuilder.writeLine("    var npf = cpfr.contentWindow.document.getElementById('newPwd_field');");
			pageBuilder.writeLine("    if (npf == null)");
			pageBuilder.writeLine("      return false;");
			pageBuilder.writeLine("    var cpf = cpfr.contentWindow.document.getElementById('confirmPwd_field');");
			pageBuilder.writeLine("    if (cpf == null)");
			pageBuilder.writeLine("      return false;");
			pageBuilder.writeLine("    opf.value = opi.value;");
			pageBuilder.writeLine("    npf.value = npi.value;");
			pageBuilder.writeLine("    cpf.value = cpi.value;");
			pageBuilder.writeLine("    cpfo.submit();");
			pageBuilder.writeLine("    window.setTimeout('" + this.getName() + "_readChangePasswordResult()', 100);");
			pageBuilder.writeLine("    var cpfs = document.getElementById('" + this.getName() + "_ChangePasswordFields');");
			pageBuilder.writeLine("    if (cpfs != null)");
			pageBuilder.writeLine("      cpfs.style.display = 'none';");
			pageBuilder.writeLine("    return false;");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("}");
			pageBuilder.writeLine("function " + this.getName() + "_readChangePasswordResult() {");
			pageBuilder.writeLine("  var cpfr = document.getElementById('" + this.getName() + "_ChangePasswordFrame');");
			pageBuilder.writeLine("  var cprf = cpfr.contentWindow.document.getElementById('changePwdResult_field');");
			pageBuilder.writeLine("  if (cprf == null)");
			pageBuilder.writeLine("    window.setTimeout('" + this.getName() + "_readChangePasswordResult()', 100);");
			pageBuilder.writeLine("  else {");
			pageBuilder.writeLine("    alert(cprf.value);");
			pageBuilder.writeLine("    var cpfs = document.getElementById('" + this.getName() + "_ChangePasswordFields');");
			pageBuilder.writeLine("    if (cpfs != null)");
			pageBuilder.writeLine("      cpfs.style.display = 'none';");
			pageBuilder.writeLine("  }");
			pageBuilder.writeLine("}");
			pageBuilder.writeLine("function " + this.getName() + "_resetChangePasswordFields() {");
			pageBuilder.writeLine("  var cpf = document.getElementById('webAppHostChangePasswordFields');");
			pageBuilder.writeLine("  if (cpf == null)");
			pageBuilder.writeLine("    return;");
			pageBuilder.writeLine("  cpf.style.display = 'none';");
			pageBuilder.writeLine("  mouseOverCpf = false;");
			pageBuilder.writeLine("  var opi = document.getElementById('" + this.getName() + "_oldPwd_input');");
			pageBuilder.writeLine("  if (opi != null)");
			pageBuilder.writeLine("    opi.value = '';");
			pageBuilder.writeLine("  var npi = document.getElementById('" + this.getName() + "_newPwd_input');");
			pageBuilder.writeLine("  if (npi != null)");
			pageBuilder.writeLine("    npi.value = '';");
			pageBuilder.writeLine("  var cpi = document.getElementById('" + this.getName() + "_confirmPwd_input');");
			pageBuilder.writeLine("  if (cpi != null)");
			pageBuilder.writeLine("    cpi.value = '';");
			pageBuilder.writeLine("}");
			 
			pageBuilder.writeLine("</script>");
		}
		public String getAccountManagerOnclickCall() {
			return (this.getName() + "_resetChangePasswordFields()");
		}
		public String getAccountManagerOnshowCall() {
			return (this.getName() + "_resetChangePasswordFields()");
		}
		public String getAccountManagerOnhideCall() {
			return (this.getName() + "_resetChangePasswordFields()");
		}
		public boolean handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
			
			//	check path info
			String pathInfo = request.getPathInfo();
			if (pathInfo == null)
				return false;
			
			//	request for password change form
			if (pathInfo.endsWith("/cpForm")) {
				response.setContentType("text/html");
				response.setCharacterEncoding("UTF-8");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
				bw.write("<html><body>");
				bw.write("<form id=\"changePwdForm\" method=\"POST\" action=\"" + request.getContextPath() + request.getServletPath() + "/webAppAuthProvider/changePwd" + "\">");
				bw.write("<input type=\"hidden\" id=\"oldPwd_field\" name=\"oldPwd\" value=\"\" />");
				bw.write("<input type=\"hidden\" id=\"newPwd_field\" name=\"newPwd\" value=\"\" />");
				bw.write("<input type=\"hidden\" id=\"confirmPwd_field\" name=\"confirmPwd\" value=\"\" />");
				bw.write("</form>");
				bw.write("</body></html>");
				bw.flush();
				return true;
			}
			
			//	request for account creation form
			if (pathInfo.endsWith("/caForm")) {
				sendCreateAccountForm(request, response, null, null);
				return true;
			}
			
			//	request actually changing password
			if (pathInfo.endsWith("/changePwd")) {
				String message = "";
				
				//	get parameters
				String userName = webAppHost.getUserName(request);
				String oldPassword = request.getParameter("oldPwd");
				String newPassword = request.getParameter("newPwd");
				String confirmPassword = request.getParameter("confirmPwd");
				if (newPassword.equals(confirmPassword)) {
					if (oldPassword.equals(newPassword))
						message = "Old and new passwords are the same, please try again.";
					else {
						String query = "UPDATE " + (authProviderName + "Data") + 
								" SET " + PSWD_HASH_PARAMETER + " = " + newPassword.hashCode() + 
								" WHERE " + PSWD_HASH_PARAMETER + " = " + oldPassword.hashCode() + 
								" AND " + USER_NAME_PARAMETER + " LIKE '" + EasyIO.sqlEscape(userName) + "'" +
								";";
						try {
							int updated = io.executeUpdateQuery(query);
							if (updated == 1)
								message = "Password changed successfully.";
							else message = "Invalid password or unknown user.";
						}
						catch (SQLException sqle) {
							System.out.println("Could not change password of user '" + userName + "' - database not accessible.");
							System.out.println("  query was " + query);
							message = "Could not change password due to technical problems.";
						}
					}
				}
				else message = "New password and confirmation do not match, please try again.";
				
				//	send result
				response.setContentType("text/html");
				response.setCharacterEncoding("UTF-8");
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
				bw.write("<html><body>");
				bw.write("<form id=\"changePwdResultForm\" action=\"#\">");
				bw.write("<input type=\"hidden\" id=\"changePwdResult_field\" name=\"changePwdResult\" value=\"" + message + "\" />");
				bw.write("</form>");
				bw.write("</body></html>");
				bw.flush();
				return true;
			}
			
			//	request actually creating account
			if (pathInfo.endsWith("/createAccount")) {
				String error = null;
				
				//	check recaptcha first, and encapsulate exceptions
				try {
					if (!checkReCaptcha(request))
						error = "Your ReCAPTCHA response is invalid.";
				}
				catch (Exception e) {
					System.out.println("Error validating ReCAPTCHA response: " + e.getMessage());
					e.printStackTrace(System.out);
					error = "Your ReCAPTCHA response cannot be validated at the moment due to technical problems.";
				}
				
				//	get parameters
				String userName = request.getParameter("userName");
				String password = request.getParameter("pswd");
				String cPassword = request.getParameter("cPswd");
				
				//	check if user name valid
				if (error == null) {
					if (userName == null)
						error = "Invalid user name.";
					else if (!userName.matches("[A-Za-z0-9\\-\\_\\.]+")) {
						error = "Invalid user name, use letters and digits only.";
						userName = null;
					}
				}
				
				//	check if user exists
				if (error == null) {
					String query = "SELECT * FROM " + (authProviderName + "Data") + 
							" WHERE " + USER_NAME_PARAMETER + " LIKE '" + EasyIO.sqlEscape(userName) + "'" +
							";";
					SqlQueryResult sqr = null;
					try {
						sqr = io.executeSelectQuery(query);
						if (sqr.next()) {
							error = "The selected user name already exists.";
							userName = null;
						}
					}
					catch (SQLException sqle) {
						System.out.println("Could not create account for user '" + userName + "' - database not accessible.");
						System.out.println("  query was " + query);
						error = "Could not create account due to technical problems.";
					}
					finally {
						if (sqr != null)
							sqr.close();
					}
				}
				
				//	check if passwords match
				if (error == null) {
					if (password == null)
						error = "Invalid password";
					else if (!password.equals(cPassword))
						error = "Password and confirmation do not match, please try again.";
				}
				
				//	create account
				if (error == null) {
					String query = "INSERT INTO " + (authProviderName + "Data") + 
						" (" + 
							USER_NAME_PARAMETER + 
							", " + 
							PSWD_HASH_PARAMETER + 
							", " + 
							CREATE_TIME_PARAMETER + 
							", " + 
							LAST_ACCESS_PARAMETER + 
						") VALUES (" +
							"'" + EasyIO.sqlEscape(userName) + "'" + 
							", " + 
							password.hashCode() + 
							", " + 
							System.currentTimeMillis() + 
							", " + 
							"-1" + 
						");";
					try {
						io.executeUpdateQuery(query);
					}
					catch (SQLException sqle) {
						System.out.println("Could not create account for user '" + userName + "' - database not accessible.");
						System.out.println("  query was " + query);
						error = "Could not create account due to technical problems.";
					}
				}
				
				if (error == null) {
					response.setContentType("text/html");
					response.setCharacterEncoding("UTF-8");
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
					bw.write("<html><body>");
					bw.write("<form id=\"createAccountForm\" method=\"POST\" action=\"#\">");
					bw.write("<input type=\"hidden\" id=\"createAccountUn\" name=\"un\" value=\"" + userName + "\" />");
					bw.write("<input type=\"hidden\" id=\"createAccountPwd\" name=\"pw\" value=\"" + password + "\" />");
					bw.write("<input type=\"hidden\" id=\"createAccountResult\" name=\"car\" value=\"OK\" />");
					bw.write("</form>");
					bw.write("</body></html>");
					bw.flush();
				}
				else sendCreateAccountForm(request, response, userName, error);
				
				return true;
			}
			
			//	this one's not for us
			return false;
		}
	}
	
	protected void exit() {
		this.io.close();
	}
	
	private boolean checkReCaptcha(HttpServletRequest request) throws IOException {
		if (!this.useReCaptcha)
			return true;
		String remoteAddr = request.getRemoteAddr();
		ReCaptchaImpl reCaptcha = new ReCaptchaImpl();
		reCaptcha.setPrivateKey(this.reCaptchaPrivateKey);
		String challenge = request.getParameter("recaptcha_challenge_field");
		String response = request.getParameter("recaptcha_response_field");
		ReCaptchaResponse reCaptchaResponse = reCaptcha.checkAnswer(remoteAddr, challenge, response);
		return reCaptchaResponse.isValid();
	}
	
	private void sendCreateAccountForm(HttpServletRequest request, HttpServletResponse response, String userName, String error) throws IOException {
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
		bw.write("<html><head>"); bw.newLine();
		
		String[] cssStylesheets = this.getCssStylesheets();
		if (cssStylesheets != null)
			for (int c = 0; c < cssStylesheets.length; c++) {
				String cssStylesheetUrl = cssStylesheets[c];
				if (cssStylesheetUrl.indexOf("://") == -1)
					cssStylesheetUrl = request.getContextPath() + this.getRelativeDataPath() + "/" + cssStylesheetUrl;
				bw.write("<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"" + cssStylesheetUrl + "\" />"); bw.newLine();
			}
		bw.write("</head><body>"); bw.newLine();
		bw.write("<form id=\"createAccountForm\" method=\"POST\" action=\"" + request.getContextPath() + request.getServletPath() + "/webAppAuthProvider/" + this.authProviderName + "/createAccount" + "\">");
		bw.write("<table class=\"" + this.authProviderName + "_CreateAccountFieldTable\">"); bw.newLine();
		
		bw.write("<tr>");
		bw.write("<td class=\"" + this.authProviderName + "_CreateAccountFieldLabelCell\">");
		bw.write("User&nbsp;Name");
		bw.write("</td>");
		bw.write("<td class=\"" + this.authProviderName + "_CreateAccountFieldCell\">");
		bw.write("<input type=\"text\" id=\"userName_field\" name=\"userName\" value=\"" + ((userName == null) ? "" : userName) + "\" tabindex=\"1\">");
		bw.write("</td>");
		if (this.useReCaptcha) {
			bw.write("<td class=\"" + this.authProviderName + "_CreateAccountReCaptchaCell\" rowspan=\"3\">");
			ReCaptcha reCaptcha = ReCaptchaFactory.newReCaptcha(this.reCaptchaPublicKey, this.reCaptchaPrivateKey, true);
			bw.write(reCaptcha.createRecaptchaHtml(null, null));
			bw.write("</td>");
		}
		bw.write("</tr>"); bw.newLine();
		
		bw.write("<tr>");
		bw.write("<td class=\"" + this.authProviderName + "_CreateAccountFieldLabelCell\">");
		bw.write("Password");
		bw.write("</td>");
		bw.write("<td class=\"" + this.authProviderName + "_CreateAccountFieldCell\">");
		bw.write("<input type=\"password\" id=\"pswd_field\" name=\"pswd\" value=\"\" tabindex=\"2\" />");
		bw.write("</td>");
		bw.write("</tr>"); bw.newLine();
		
		bw.write("<tr>");
		bw.write("<td class=\"" + this.authProviderName + "_CreateAccountFieldLabelCell\">");
		bw.write("Confirm&nbsp;Password");
		bw.write("</td>");
		bw.write("<td class=\"" + this.authProviderName + "_CreateAccountFieldCell\">");
		bw.write("<input type=\"password\" id=\"cPswd_field\" name=\"cPswd\" value=\"\" tabindex=\"3\" />");
		bw.write("</td>");
		bw.write("</tr>"); bw.newLine();
		
		bw.write("</table>"); bw.newLine();
		
		bw.write("<input type=\"hidden\" id=\"createAccountResult\" name=\"car\" value=\"" + ((error == null) ? "" : error) + "\" />");
		bw.write("</form>"); bw.newLine();
		
		bw.write("<script type=\"text/javascript\">"); bw.newLine();
		bw.write("function checkCreateAccountData() {"); bw.newLine();
		bw.write("  var unf = document.getElementById('userName_field');"); bw.newLine();
		bw.write("  var pwdf = document.getElementById('pswd_field');"); bw.newLine();
		bw.write("  var cpwdf = document.getElementById('cPswd_field');"); bw.newLine();
		bw.write("  if ((unf == null) || (pwdf == null) || (cpwdf == null))"); bw.newLine();
		bw.write("    return false;"); bw.newLine();
		bw.write("  if (unf.value == '') {"); bw.newLine();
		bw.write("    alert('Please specify a user name.');"); bw.newLine();
		bw.write("    return false;"); bw.newLine();
		bw.write("  }"); bw.newLine();
		bw.write("  if ((pwdf.value == '') || (cpwdf.value == '')) {"); bw.newLine();
		bw.write("    alert('Please specify a password and confirm it.');"); bw.newLine();
		bw.write("    return false;"); bw.newLine();
		bw.write("  }"); bw.newLine();
		bw.write("  if (pwdf.value != cpwdf.value) {"); bw.newLine();
		bw.write("    alert('The specified password does not match its confirmation.');"); bw.newLine();
		bw.write("    return false;"); bw.newLine();
		bw.write("  }"); bw.newLine();
		bw.write("  return true;"); bw.newLine();
		bw.write("}"); bw.newLine();
		bw.write("</script>"); bw.newLine();
		
		bw.write("</body></html>");
		bw.flush();
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		//	TODO use this for administation purposes
	}
	
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		//	TODO use this for administation purposes
	}
	
	/*
TODO use this function to protect passwords in the future
String.prototype.hashCode = function(){
	var hash = 0, i, char;
	if (this.length == 0) return hash;
	for (i = 0; i < this.length; i++) {
		char = this.charCodeAt(i);
		hash = ((hash<<5)-hash)+char;
		hash = hash & hash; // Convert to 32bit integer
	}
	return hash;
};
	 */
}