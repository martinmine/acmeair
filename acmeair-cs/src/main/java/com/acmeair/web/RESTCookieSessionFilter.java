/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package com.acmeair.web;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acmeair.util.HTTPHelper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.acmeair.util.Util;

	public class RESTCookieSessionFilter implements Filter {
	
	static final String LOGIN_USER = "acmeair.login_user";
	private static String authServiceLocation = ((System.getenv("AUTH_SERVICE") == null) ? Util.getServiceProxy() + "/auth/acmeair-as" : System.getenv("AUTH_SERVICE"));

	//private static String authServiceLocation = System.getenv("AUTH_SERVICE");
			
	private static final String AUTHCHECK_PATH = "/rest/api/login/authcheck/";
	private static final String VALIDATE_PATH = "/rest/api/customer/validateid";
	private static final String CONFIG_PATH = "/rest/api/customer/config";
	private static final String LOADER_PATH = "/rest/api/customer/loader";
	
	public static String SESSIONID_COOKIE_NAME = "sessionid";

	private static final Logger LOGGER = Logger.getLogger(RESTCookieSessionFilter.class.getName());
		
	@Inject
	BeanManager beanManager;

	@Override
	public void destroy() {
	}
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest)req;
		HttpServletResponse response = (HttpServletResponse)resp;

		final String path = request.getContextPath() + request.getServletPath() + request.getPathInfo();

		if (path.endsWith(VALIDATE_PATH) || path.contains(CONFIG_PATH) || path.contains(LOADER_PATH)) {
			// if validating id, let the request flow
			// TODO: need to secure this somehow probably
			chain.doFilter(req, resp);
			return;
		}

		final Cookie cookies[] = request.getCookies();
		Cookie sessionCookie = null;

		if (cookies != null) {
			for (Cookie c : cookies) {
				if (c.getName().equals(SESSIONID_COOKIE_NAME)) {
					sessionCookie = c;
				}

				if (sessionCookie != null) {
					break;
				}
			}
		}
		String sessionId = "";
		if (sessionCookie!=null) // We need both cookie to work
			sessionId= sessionCookie.getValue().trim();
		// did this check as the logout currently sets the cookie value to "" instead of aging it out
		// see comment in LogingREST.java
		if (sessionId.equals("")) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final AsyncContext ac = request.startAsync();
		final String url = "http://" + authServiceLocation  + AUTHCHECK_PATH + sessionId;

		HTTPHelper.execute(new HttpGet(url)).thenAccept(r -> {
			try {
				if (r != null) {
					JSONObject jsonObject = (JSONObject) JSONValue.parse(EntityUtils.toString(r.getEntity()));

					if (jsonObject == null) {
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
						ac.complete();
						return;
					}

					String loginUser = (String) jsonObject.get("customerid");

					if (loginUser == null) {
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
						ac.complete();
					}

					ac.getRequest().setAttribute(LOGIN_USER, loginUser);
					ac.dispatch();
				} else {
					try {
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
						ac.complete();
					} catch (IOException e) {
						// ¯\_(ツ)_/¯
						LOGGER.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
			catch (Exception e) {
				LOGGER.log(Level.SEVERE, e.getMessage(), e);

				try {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
				} catch (IOException e1) {
					LOGGER.log(Level.SEVERE, e1.getMessage(), e1);
				}
				ac.complete();
			}
		});
	}

	@Override
	public void init(FilterConfig config) throws ServletException {
	}
}
