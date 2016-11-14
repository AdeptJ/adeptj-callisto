/** 
###############################################################################
#                                                                             # 
#    Copyright 2016, AdeptJ (http://adeptj.com)                               #
#                                                                             #
#    Licensed under the Apache License, Version 2.0 (the "License");          #
#    you may not use this file except in compliance with the License.         #
#    You may obtain a copy of the License at                                  #
#                                                                             #
#        http://www.apache.org/licenses/LICENSE-2.0                           #
#                                                                             #
#    Unless required by applicable law or agreed to in writing, software      #
#    distributed under the License is distributed on an "AS IS" BASIS,        #
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. #
#    See the License for the specific language governing permissions and      #
#    limitations under the License.                                           #
#                                                                             #
###############################################################################
*/
package com.adeptj.runtime.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.adeptj.runtime.admin.render.ContextObjects;
import com.adeptj.runtime.admin.render.RenderContext;
import com.adeptj.runtime.admin.render.RenderEngine;

/**
 * OSGi AdminLoginServlet serves the login page.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@WebServlet(name = "AdminLoginServlet", urlPatterns = { "/admin/login" })
public class AdminLoginServlet extends HttpServlet {

	private static final long serialVersionUID = -3339904764769823449L;

	/**
	 * Render login page.
	 */
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RenderContext.Builder builder = new RenderContext.Builder();
		builder.view("auth/login").contextObjects(new ContextObjects()).request(req).response(resp).locale(req.getLocale());
		RenderEngine.INSTANCE.render(builder.build());
	}

	/**
	 * Control comes here when login to "/auth/j_security_check" fails due to invalid credentials.
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		RenderContext.Builder builder = new RenderContext.Builder();
		ContextObjects contextObjects = new ContextObjects();
		contextObjects.put("validation", "Invalid credentials!!");
		contextObjects.put("j_username", req.getParameter("j_username"));
		builder.view("auth/login").contextObjects(contextObjects).request(req).response(resp).locale(req.getLocale());
		// Render login page again with validation message.
		RenderEngine.INSTANCE.render(builder.build());
	}
}