/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *	 * Redistributions of source code must retain the above copyright
 *	   notice, this list of conditions and the following disclaimer.
 *	 * Redistributions in binary form must reproduce the above copyright
 *	   notice, this list of conditions and the following disclaimer in the
 *	   documentation and/or other materials provided with the distribution.
 *	 * Neither the name of the Universität Karlsruhe (TH) / KIT nor the
 *	   names of its contributors may be used to endorse or promote products
 *	   derived from this software without specific prior written permission.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Properties;

import de.uka.ipd.idaho.htmlXmlUtil.accessories.HtmlPageBuilder;

/**
 * Simple, single-class alternative to recaptcha4j, which has errors in its
 * response verification code, presumably for failing to adapt to an API update.
 * 
 * @author sautter
 */
public class ReCAPTCHA {
//	private static final String PROPERTY_THEME = "theme";
//	private static final String PROPERTY_TABINDEX = "tabindex";
//	
	private static final String DEFAULT_SERVER_URL = "http://www.google.com/recaptcha/api";
	private static final String DEFAULT_VERIFY_URL = "http://www.google.com/recaptcha/api/verify";
	
	private final String publicKey;
	private final String privateKey;
	private final boolean includeNoscript;
	
	private final String serverUrl;
	private final String verifyUrl;
	
	/**
	 * @param publicKey
	 * @param privateKey
	 */
	public ReCAPTCHA(String publicKey, String privateKey) {
		this(publicKey, privateKey, false, DEFAULT_SERVER_URL, DEFAULT_VERIFY_URL);
	}
	
	/**
	 * @param publicKey
	 * @param privateKey
	 * @param includeNoscript
	 */
	public ReCAPTCHA(String publicKey, String privateKey, boolean includeNoscript) {
		this(publicKey, privateKey, includeNoscript, DEFAULT_SERVER_URL, DEFAULT_VERIFY_URL);
	}
	
	/**
	 * @param publicKey
	 * @param privateKey
	 * @param includeNoscript
	 * @param serverUrl
	 */
	public ReCAPTCHA(String publicKey, String privateKey, boolean includeNoscript, String serverUrl) {
		this(publicKey, privateKey, includeNoscript, serverUrl, (serverUrl + "/verify"));
	}
	
	/**
	 * @param publicKey
	 * @param privateKey
	 * @param includeNoscript
	 * @param serverUrl
	 * @param verifyUrl
	 */
	public ReCAPTCHA(String publicKey, String privateKey, boolean includeNoscript, String serverUrl, String verifyUrl) {
		this.publicKey = publicKey;
		this.privateKey = privateKey;
		this.includeNoscript = includeNoscript;
		this.serverUrl = serverUrl;
		this.verifyUrl = verifyUrl;
	}
	
	/**
	 * Creates HTML output with embedded ReCAPTCHA. The string response should be output on a HTML page (e.g. inside a JSP).
	 * 
	 * @param errorMessage An error message to display in the CAPTCHA, null if none.
	 * @param options Options for rendering, <code>tabindex</code> and <code>theme</code> are currently supported by recaptcha. You can
	 *   put any options here though, and they will be added to the RecaptchaOptions javascript array.
	 * @return
	 */
	public String createRecaptchaHtml(String errorMessage, Properties options) throws IOException {
		String errorPart = (errorMessage == null ? "" : "&amp;error=" + URLEncoder.encode(errorMessage, "UTF-8"));
		String html = this.getJSOptions(options);
		
		html += "<script type=\"text/javascript\" src=\"" + this.serverUrl + "/challenge?k=" + this.publicKey + errorPart + "\"></script>\r\n";
		
		if (this.includeNoscript) {
			String noscript = "<noscript>\r\n" + 
				"	   <iframe src=\"" + this.serverUrl + "/noscript?k=" + this.publicKey + errorPart + "\" height=\"300\" width=\"500\" frameborder=\"0\"></iframe><br/>\r\n" +
				"	   <textarea name=\"recaptcha_challenge_field\" rows=\"3\" cols=\"40\"></textarea>\r\n" + 
				"	   <input type=\"hidden\" name=\"recaptcha_response_field\" value=\"manual_challenge\"/>\r\n" + 
				"</noscript>";
			html += noscript;
		}
		
		return html;
	}
	
	private String getJSOptions(Properties properties) {
		if ((properties == null) || (properties.size() == 0))
			return "";
		
		StringBuffer jsOptions = new StringBuffer("<script type=\"text/javascript\">\r\n"); 
		
		jsOptions.append("var RecaptchaOptions = {");
		for (Enumeration pne = properties.keys(); pne.hasMoreElements(); ) {
			String property = ((String) pne.nextElement());
			jsOptions.append(property + ": '" + properties.getProperty(property) + "'");
			if (pne.hasMoreElements())
				jsOptions.append(",");
		}
		jsOptions.append("};\r\n");
		
		jsOptions.append("</script>\r\n");
		return jsOptions.toString();
	}
	
	/**
	 * Creates HTML output with embedded ReCAPTCHA. The string response should be output on a HTML page (e.g. inside a JSP).
	 * 
	 * @param errorMessage An error message to display in the CAPTCHA, null if none.
	 * @param options Options for rendering, <code>tabindex</code> and <code>theme</code> are currently supported by recaptcha. You can
	 *   put any options here though, and they will be added to the RecaptchaOptions javascript array.
	 * @param hpb the HtmlPageBuilder to write to
	 */
	public void writeRecaptchaHtml(String errorMessage, Properties options, HtmlPageBuilder hpb) throws IOException {
		String errorPart = (errorMessage == null ? "" : "&amp;error=" + URLEncoder.encode(errorMessage, "UTF-8"));
		this.writeJSOptions(options, hpb);
		
		hpb.writeLine("<script type=\"text/javascript\" src=\"" + this.serverUrl + "/challenge?k=" + this.publicKey + errorPart + "\"></script>");
		
		if (this.includeNoscript) {
			hpb.writeLine("<noscript>"); 
			hpb.writeLine("  <iframe src=\"" + this.serverUrl + "/noscript?k=" + this.publicKey + errorPart + "\" height=\"300\" width=\"500\" frameborder=\"0\"></iframe><br/>");
			hpb.writeLine("  <textarea name=\"recaptcha_challenge_field\" rows=\"3\" cols=\"40\"></textarea>");
			hpb.writeLine("  <input type=\"hidden\" name=\"recaptcha_response_field\" value=\"manual_challenge\"/>"); 
			hpb.writeLine("</noscript>");
		}
	}
	
	private void writeJSOptions(Properties properties, HtmlPageBuilder hpb) throws IOException {
		if ((properties == null) || (properties.size() == 0))
			return;
		
		hpb.writeLine("<script type=\"text/javascript\">"); 
		
		hpb.writeLine("var RecaptchaOptions = {");
		for (Enumeration pne = properties.keys(); pne.hasMoreElements(); ) {
			String property = ((String) pne.nextElement());
			hpb.write(property + ": '" + properties.getProperty(property) + "'");
			if (pne.hasMoreElements())
				hpb.write(",");
			hpb.newLine();
		}
		hpb.writeLine("};");
		
		hpb.writeLine("</script>");
	}
	
	/**
	 * Validates a ReCAPTCHA challenge and response.
	 * 
	 * @param remoteAddr The address of the user, e.g. request.getRemoteAddr()
	 * @param challenge The challenge from the ReCAPTCHA form, this is usually request.getParameter("recaptcha_challenge_field") in your code.
	 * @param userResponse The response from the ReCAPTCHA form, this is usually request.getParameter("recaptcha_response_field") in your code.
	 * @return a Response object wrapping an indicator for pass or fail together with an error message
	 */
	public Response checkAnswer(String remoteAddr, String challenge, String userResponse) throws IOException {
		String verifyParams = "" +
				"privatekey=" + URLEncoder.encode(this.privateKey, "UTF-8") + 
				"&remoteip=" + URLEncoder.encode(remoteAddr, "UTF-8") +
				"&challenge=" + URLEncoder.encode(challenge, "UTF-8") + 
				"&response=" + URLEncoder.encode(userResponse, "UTF-8");
		
		String verifyResponse;
		try {
			HttpURLConnection verifyCon = ((HttpURLConnection) new URL(this.verifyUrl).openConnection());
			verifyCon.setConnectTimeout(10000);
			verifyCon.setReadTimeout(10000);
			
			byte[] verifyBytes = verifyParams.getBytes("UTF-8");
			
			verifyCon.setDoOutput(true);
			verifyCon.setDoInput(true);
			verifyCon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			verifyCon.setRequestProperty("Content-Length", ("" + verifyBytes.length));
			verifyCon.setRequestMethod("POST");
			
			OutputStream vbOut = verifyCon.getOutputStream();
			vbOut.write(verifyBytes);
			vbOut.flush();
			
			InputStream vrIn = verifyCon.getInputStream();
			ByteArrayOutputStream vrOut = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			for (int r; (r = vrIn.read(buf, 0, buf.length)) != -1;)
				vrOut.write(buf, 0, r);
			vbOut.close();
			vrIn.close();
			
			verifyResponse = vrOut.toString("UTF-8");
			if (verifyResponse == null)
				return new Response(false, "ReCAPTCHA-Not-Reachable");
		}
		catch (IOException ioe) {
			return new Response(false, "ReCAPTCHA-Not-Reachable");
		}
		
		String[] verifyResponseParts = verifyResponse.split("\\r?\\n");
		if (verifyResponseParts.length < 1)
			return new Response(false, "No answer returned from ReCAPTCHA: " + verifyResponse);
		boolean valid = "true".equals(verifyResponseParts[0]);
		return new Response(valid, (valid ? null : ((verifyResponseParts.length < 2) ? "Missing-Error-Message" : verifyResponseParts[1])));
	}
	
	/**
	 * Response wrapper, consisting of a boolean indicating success, and
	 * possibly an error message. 
	 * 
	 * @author sautter
	 */
	public static class Response {
		private boolean valid;
		private String errorMessage;
		
		Response(boolean valid, String errorMessage) {
			this.valid = valid;
			this.errorMessage = errorMessage;
		}
		
		/**
		 * The ReCAPTCHA error message. invalid-site-public-key invalid-site-private-key invalid-request-cookie 
		 * incorrect-captcha-sol verify-params-incorrect verify-params-incorrect recaptcha-not-reachable
		 * 
		 * @return
		 */
		public String getErrorMessage() {
			return this.errorMessage;
		}
		
		/**
		 * Check if the user response is valid.
		 * @return <code>true</code> if the CAPTCHA is "passed".
		 */
		public boolean isValid() {
			return this.valid;
		}
	}
}