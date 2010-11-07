/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.cas.server.web;

import org.jasig.cas.server.CasProtocolVersion;
import org.jasig.cas.server.CentralAuthenticationService;
import org.jasig.cas.server.authentication.Credential;
import org.jasig.cas.server.authentication.DefaultUrlCredentialImpl;
import org.jasig.cas.server.login.CasTokenServiceAccessRequestImpl;
import org.jasig.cas.server.login.DefaultLoginRequestImpl;
import org.jasig.cas.server.login.LoginRequest;
import org.jasig.cas.server.login.LoginResponse;
import org.jasig.cas.server.session.Access;
import org.jasig.cas.server.session.AccessResponseRequest;
import org.jasig.cas.server.session.DefaultAccessResponseRequestImpl;
import org.jasig.cas.server.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the request to validate the various different CAS protocols.
 *
 * @author Scott Battaglia
 * @version $Revision$ $Date$
 * @since 4.0.0
 */
@Controller
public final class ValidationController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    private final CentralAuthenticationService centralAuthenticationService;

    @Inject
    public ValidationController(final CentralAuthenticationService centralAuthenticationService) {
        this.centralAuthenticationService = centralAuthenticationService;
    }

    @RequestMapping(method=RequestMethod.GET, value="/validate")
    public final void validateCas10Request(@RequestParam(value="renew",required=false, defaultValue = "false") final boolean renew, @RequestParam(value="service",required=true) final String service, @RequestParam(value="ticket",required=true) final String ticket, final HttpServletRequest request, final Writer writer) throws IOException {
            validateRequest(renew, service, ticket, CasProtocolVersion.CAS1, request, writer);
    }

    @RequestMapping(method=RequestMethod.GET, value="/serviceValidate")
    public final void validateCas20Request(@RequestParam(value="renew",required=false,defaultValue = "false") final boolean renew, @RequestParam(value="service",required=true) final String service, @RequestParam(value="ticket",required=true) final String ticket, @RequestParam(value="pgtUrl",required=false) final String pgtUrl, final HttpServletRequest request, final Writer writer) {
        validateRequest(renew, service, ticket, CasProtocolVersion.CAS2, request, writer);
    }

    @RequestMapping(method= RequestMethod.GET, value="/proxyValidate")
    public final void validateCasProxyTicketRequest(@RequestParam(value="renew",required=false,defaultValue = "false") final boolean renew, @RequestParam(value="service",required=true) final String service, @RequestParam(value="ticket",required=true) final String ticket, @RequestParam(value="pgtUrl",required=false) final String pgtUrl, final HttpServletRequest request, final Writer writer) {
        validateRequest(renew, service, ticket, CasProtocolVersion.CAS2_WITH_PROXYING, request, writer);
    }

    protected final void validateRequest(final boolean renew, final String service, final String ticket, final CasProtocolVersion casVersion, final HttpServletRequest request, final Writer writer) {
        if (!StringUtils.hasText(ticket) || !StringUtils.hasText(service)) {
            logger.debug("Invalid request");
            writeErrorResponse("INVALID_REQUEST", "service and ticket are required parameters.", casVersion, writer);
            return;
        }

        try {
            final CasTokenServiceAccessRequestImpl casTokenServiceAccessRequest = new CasTokenServiceAccessRequestImpl(casVersion, ticket, service, request.getRemoteAddr(), renew, false);
            final Access access = null;
            // TODO re-enable this.centralAuthenticationService.validate(casTokenServiceAccessRequest);
            final Credential proxyCredential = createProxyCredential(request);

            if (access != null) {
	            logger.debug("Successfully validated {}", ticket);
                final Session proxySession;

                if (proxyCredential != null) {
                    final LoginRequest loginRequest = new DefaultLoginRequestImpl(null, request.getRemoteAddr(), false, false, access);
                    loginRequest.getCredentials().add(proxyCredential);
                    final LoginResponse loginResponse = this.centralAuthenticationService.login(loginRequest);
                    proxySession = loginResponse.getSession();
                } else {
                    proxySession = null;
                }
// TODO revisit this
                final AccessResponseRequest accessResponseRequest = new DefaultAccessResponseRequestImpl(writer, proxySession != null ? proxySession.getId() : null, proxyCredential);
                access.generateResponse(accessResponseRequest);
            } else {
	            logger.debug("Invalid ticket {}", ticket);
                writeErrorResponse("INVALID_TICKET", "Ticket '" + ticket + "' not recognized.", casVersion, writer);
            }
        } catch (final Exception e) {
            logger.error("Ticket validation error", e);
            writeErrorResponse("INTERNAL_ERROR", e.getMessage(), casVersion, writer);
        }
    }

    protected Credential createProxyCredential(final HttpServletRequest request) {
        final String pgtUrl = request.getParameter("pgtUrl");

        return pgtUrl != null ? new DefaultUrlCredentialImpl(pgtUrl) : null;
    }

    protected final void writeErrorResponse(final String errorCode, final String errorMessage, final CasProtocolVersion casVersion, final Writer writer) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("errorCode", errorCode);
        parameters.put("message", errorMessage);
//        FreemarkerUtils.writeToFreeMarkerTemplate(casVersion.asString() + "errorResponseTemplate.ftl", parameters, writer);
    }

}
