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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import com.acmeair.util.HTTPHelper;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.acmeair.service.AuthService;
import com.acmeair.service.ServiceLocator;
import com.acmeair.util.Util;

@Path("/login")
public class LoginREST {
	
	public static String SESSIONID_COOKIE_NAME = "sessionid";
	private AuthService authService = ServiceLocator.instance().getService(AuthService.class);
	
	private static final Logger LOGGER = Logger.getLogger(LoginREST.class.getName());
	private static String customerServiceLocation = ((System.getenv("CUSTOMER_SERVICE") == null) ? Util.getServiceProxy() + "/customer/acmeair-cs" : System.getenv("CUSTOMER_SERVICE"));
	//private static String customerServiceLocation = System.getenv("CUSTOMER_SERVICE");
	
	private static final String VALIDATE_PATH = "/rest/api/customer/validateid";
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Produces("text/plain")
	public void login(@Suspended final AsyncResponse response,
						  @FormParam("login") String login, @FormParam("password") String password)
			throws UnsupportedEncodingException {

		validateCustomer(login, password).thenAccept(validCustomer -> {
			if (!validCustomer) {
				response.resume(Response.status(Response.Status.FORBIDDEN).build());
				return;
			}

			JSONObject sessionJson = authService.createSession(login);
			String sessionId=(String) sessionJson.get("_id");


			// TODO:  Need to fix the security issues here - they are pretty gross likely

			// TODO: The mobile client app requires JSON in the response.
			// To support the mobile client app, choose one of the following designs:
			// - Change this method to return JSON, and change the web app javascript to handle a JSON response.
			//   example:  return Response.ok("{\"status\":\"logged-in\"}").cookie(sessCookie).build();
			// - Or create another method which is identical to this one, except returns JSON response.
			//   Have the web app use the original method, and the mobile client app use the new one.
			// System.out.println("Setting " + SESSIONID_COOKIE_NAME + " to " + sessionId);

			// TODO, What is correct here? for now match the response cookie from Node (which seems to work).
			response.resume(Response.ok("logged in").header("Set-Cookie", SESSIONID_COOKIE_NAME + "=" + sessionId + "; Path=/").build());
		});
	}

	@GET
	@Path("/logout")
	@Produces("text/plain")
	public Response logout(@QueryParam("login") String login, @CookieParam("sessionid") String sessionid) {
		try {
			// TODO: seems to be a bug with chrome on the sessionid. This has always existed...
			// Looks like a difference between how the node.js app and java app handle cookies.
			if (sessionid.equals("")) {
				LOGGER.severe("Session ID is empty");
				return Response.status(Response.Status.BAD_REQUEST).build();
				//System.out.println("sessionid is empty...");
			} else  {
				authService.invalidateSession(sessionid);
			}	
			// The following call will trigger query against all partitions, disable for now
			//			customerService.invalidateAllUserSessions(login);
			
			// TODO:  Want to do this with setMaxAge to zero, but to do that I need to have the same path/domain as cookie
			// created in login.  Unfortunately, until we have a elastic ip and domain name its hard to do that for "localhost".
			// doing this will set the cookie to the empty string, but the browser will still send the cookie to future requests
			// and the server will need to detect the value is invalid vs actually forcing the browser to time out the cookie and
			// not send it to begin with
			//NewCookie sessCookie = new NewCookie(SESSIONID_COOKIE_NAME, "");
			
			return Response.ok("logged out").header("Set-Cookie", SESSIONID_COOKIE_NAME + "=; Path=/").build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@GET
	@Path("/authcheck/{tokenid}")
	@Produces("application/json")
	public Response validateSession(@PathParam("tokenid") String tokenid) {
				
		JSONObject sessionJson = authService.validateSession(tokenid);
		if (sessionJson == null) {
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		String customerid = (String) sessionJson.get("customerid");
		
		if (customerid == null) {
			return Response.status(Response.Status.FORBIDDEN).build();
		} else {
			return Response.ok(sessionJson.toString()).build();
		}
		
	}
	
	private CompletableFuture<Boolean> validateCustomer(String login, String password) throws UnsupportedEncodingException {

		final CompletableFuture<Boolean> future = new CompletableFuture<>();

		if (customerServiceLocation == null || customerServiceLocation == "") {
			customerServiceLocation = "localhost/acmeair";
		}

		final String url = "http://" + customerServiceLocation + VALIDATE_PATH;

		HttpPost request = new HttpPost(url);

		List<NameValuePair> form = new ArrayList<>();
		form.add(new BasicNameValuePair("login", login));
		form.add(new BasicNameValuePair("password", password));

		request.setEntity(new UrlEncodedFormEntity(form));

		HTTPHelper.execute(request).thenAccept(output -> {
			try {
				final JSONObject jsonObject = (JSONObject) JSONValue.parse(EntityUtils.toString(output.getEntity()));
				String validCustomer = (String) jsonObject.get("validCustomer");

				future.complete(validCustomer.equals("true"));
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		});

		return future;
	}
}
