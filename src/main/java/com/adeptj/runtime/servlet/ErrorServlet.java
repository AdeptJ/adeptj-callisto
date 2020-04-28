/*
###############################################################################
#                                                                             # 
#    Copyright 2016, AdeptJ (http://www.adeptj.com)                           #
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

import com.adeptj.runtime.common.Environment;
import com.adeptj.runtime.common.RequestUtil;
import com.adeptj.runtime.config.Configs;
import com.adeptj.runtime.templating.TemplateData;
import com.adeptj.runtime.templating.TemplateEngine;
import com.adeptj.runtime.templating.TemplateEngineContext;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ErrorServlet that serves the error page w.r.t error coded(401, 403, 404, 500).
 * <p>
 * This servlet is invoked by the container using the servlet error page mechanism therefore should not be called directly.
 *
 * @author Rakesh.Kumar, AdeptJ
 */
@WebServlet(name = "AdeptJ ErrorServlet", urlPatterns = {"/error/401", "/error/403", "/error/404", "/error/500"})
public class ErrorServlet extends HttpServlet {

    private static final long serialVersionUID = -3339904764769823449L;

    private static final String KEY_EXCEPTION = "exception";

    private static final String KEY_STATUS_CODES = "common.status-codes";

    private static final String ERROR_TEMPLATE_FMT = "error/%s";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        int status = resp.getStatus();
        if (Configs.of().undertow().getIntList(KEY_STATUS_CODES).contains(status)) {
            String template = String.format(ERROR_TEMPLATE_FMT, status);
            TemplateEngineContext.Builder builder = TemplateEngineContext.builder(template, resp);
            if (Environment.isDev() && RequestUtil.hasException(req)) {
                builder.templateData(new TemplateData().with(KEY_EXCEPTION, RequestUtil.getException(req)));
            }
            TemplateEngine.getInstance().render(builder.build());
        }
    }
}