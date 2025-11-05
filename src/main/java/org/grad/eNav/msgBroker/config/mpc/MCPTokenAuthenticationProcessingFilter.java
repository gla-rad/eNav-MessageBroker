/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grad.eNav.msgBroker.config.mpc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.grad.eNav.msgBroker.exceptions.MCPHeaderAuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;

/**
 * The MCP Token Authentication Processing Filter
 * <p/>
 * When X.509 SSL requests have been authenticated by the API Gateway,
 * they can subsequently be forwarded onto the internal microservices.
 * However, the client certificate is not easily (or sometimes not at all)
 * accessible, so the gateway placed that information into the forwarded
 * request headers. This filter extracts that and passes it on to the
 * authentication manager for further processing.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class MCPTokenAuthenticationProcessingFilter extends AbstractAuthenticationProcessingFilter {

    /**
     * Initialise the MCP Authentication Processing filter with a matcher for
     * when it should be activated and an authentication manager to allow for
     * the auntication to proceed.
     *
     * @param requiresAuthenticationRequestMatcher the filter activation matcher
     * @param authenticationManager the filter authentication manager
     */
    public MCPTokenAuthenticationProcessingFilter(RequestMatcher requiresAuthenticationRequestMatcher,
                                                  AuthenticationManager authenticationManager) {
        super(requiresAuthenticationRequestMatcher);
        //Set authentication manager
        setAuthenticationManager(authenticationManager);
    }

    /**
     * This method implements the actual authentication operation.
     * <p/>
     * It will basically read the MRN and X509 certificate information from the request
     * headers and will attempt to validate the certificate. If it seems valid it will
     * accept the authentication and provide the certificates as the credentials for the
     * authorisation processing.
     *
     * @param request the incoming request
     * @param response the resulting response
     * @return the authentication result
     * @throws AuthenticationException if the authentication failed
     * @throws IOException for any IO exceptions that took place
     * @throws ServletException if the servlet fails to process the request
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException, IOException, ServletException {
        // Sanity check
        if(request.getHeader(SecomRequestHeaders.MRN_HEADER) == null
                || request.getHeader(SecomRequestHeaders.CERT_HEADER) == null) {
            return getAuthenticationManager().authenticate(null);
        }

        //Declare the final variables
        final String mrn;
        final X509Certificate certificate;

        // Extract the information from the request headers
        try {
            mrn = request.getHeader(SecomRequestHeaders.MRN_HEADER);
            certificate = getCertFromPem(request.getHeader(SecomRequestHeaders.CERT_HEADER));
        } catch (Exception ex) {
            throw new MCPHeaderAuthenticationException(ex.getMessage(), ex);
        }

        // Create a token object ot pass to Authentication Provider
        PreAuthenticatedAuthenticationToken token = new PreAuthenticatedAuthenticationToken(mrn, certificate, new HashSet<>());
        return getAuthenticationManager().authenticate(token);
    }

    /**
     * Upon successful authorisation, this function will pass down the incoming
     * request to the rest of the security filters for processing. The authentication
     * result however will be set in the security holder context.
     *
     * @param request the incoming request
     * @param response the resulting response
     * @param chain the filter chain
     * @param authResult the authentication result
     * @throws IOException for any IO exceptions that took place
     * @throws ServletException if the servlet fails to process the request
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {
        // Save user principle in security context
        SecurityContextHolder.getContext().setAuthentication(authResult);
        chain.doFilter(request, response);
    }

    /**
     * For SECOM receivers, the conversion is the opposite. When consuming the
     * transferred payload, the public key is converted back to its original
     * format.
     * <ul>
     *     <li>Add header -----BEGIN CERTIFICATE-----</li>
     *     <li>Add OS specific line feed character</li>
     *     <li>Split the public key string from the payload into an array of max 64 characters per row</li>
     *     <li>For each element in the array
     *          <ul>
     *             <li>add element as new row</li>
     *             <li>add OS specific line feed character</li>
     *         </ul>
     *     </li>
     *     <li>Add footer -----END CERTIFICATE-----</li>
     *     <li>Add OS specific line feed character</li>
     * </ul>
     * <p/>
     * This implementation, as in the official SECOM guideline, returns the
     * reconstructed X509Certificate object.
     *
     * @param certMinified The minified certificate to be converted
     * @return the original X509Certificate object
     * @throws CertificateException When a certificate cannot be restored correctly
     */
    public static X509Certificate getCertFromPem(String certMinified) throws CertificateException {
        // Do the string conversion and reconstruct the X509Certificate object
        final ByteArrayInputStream ins = new ByteArrayInputStream(getCertStringFromPem(certMinified).getBytes(StandardCharsets.UTF_8));
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(ins);
    }

    /**
     * For SECOM receivers, the conversion is the opposite. When consuming the
     * transferred payload, the X509 certificate is converted back to its
     * original format.
     * <ul>
     *     <li>Add header -----BEGIN CERTIFICATE-----</li>
     *     <li>Add OS specific line feed character</li>
     *     <li>Split the public key string from the payload into an array of max 64 characters per row</li>
     *     <li>For each element in the array
     *          <ul>
     *             <li>add element as new row</li>
     *             <li>add OS specific line feed character</li>
     *         </ul>
     *     </li>
     *     <li>Add footer -----END CERTIFICATE-----</li>
     *     <li>Add OS specific line feed character</li>
     * </ul>
     * <p/>
     * This implementation returns a simple string, augmented with the PEM
     * headers and line separators.
     *
     * @param certMinified  The minified certificate to be converted
     * @return the original X509 certificate PEM string representation
     */
    public static String getCertStringFromPem(String certMinified) {
        final StringBuilder sb = new StringBuilder();
        // 1. Add header -----BEGIN CERTIFICATE-----
        sb.append("-----BEGIN CERTIFICATE-----");
        // 2. Add OS specific line feed character
        sb.append(System.lineSeparator());
        // 3. Split the public key string from the payload into an array of max 64 characters per row
        Arrays.stream(certMinified.split("(?<=\\G.{64})"))
                // 4. For each element in the array
                .forEach(row -> {
                    // 4a. add element as new row
                    sb.append(row);
                    // 4b. add OS specific line feed character
                    sb.append(System.lineSeparator());
                });
        // 5. Add footer -----END CERTIFICATE-----
        sb.append("-----END CERTIFICATE-----");
        // 6. Add OS specific line feed character
        sb.append(System.lineSeparator());

        // And finally return the reconstructed string
        return sb.toString();
    }
}
