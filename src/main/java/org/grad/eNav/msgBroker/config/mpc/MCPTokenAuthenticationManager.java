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

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.grad.eNav.msgBroker.exceptions.MCPHeaderAuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.security.auth.x500.X500Principal;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The MCP Token Authentication Manager.
 * <p/>
 * This is the component responsible for checking the MCP certificate if
 * it is provided through the request header, using the X-SECOM-CERT
 * header. This is the technique used by the API-Gateway to propagate
 * the security information.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
@Slf4j
@Component
public class MCPTokenAuthenticationManager implements AuthenticationManager {

    /**
     * The allowed organisation MRNs.
     */
    @Value("${gla.rad.msg-broker.x509.publishers.mrn:}")
    private String allowedPublishersMrn;

    /**
     * The Component initialisation function.
     */
    @PostConstruct
    public void init() {
        log.info("initialising the MCP Token Authentication Manager");
    }

    /**
     * Implements the authentication manager operations when an MCP certificate
     * is provided is the header of the request.
     *
     * @param authentication the authentication provided
     * @return The authentication result
     */
    @Override
    public Authentication authenticate(Authentication authentication) {
        // Make sure we have what appears to be valid authentication credentials
        if(Objects.nonNull(authentication.getCredentials()) && (authentication.getCredentials() instanceof X509Certificate x509Certificate)) {
            // Retrieve the principles from the authorisation credentials
            final String mrn = (String)authentication.getPrincipal();
            final X500Principal x500Principal = ((X509Certificate)authentication.getCredentials()).getSubjectX500Principal();
            final Map<ASN1ObjectIdentifier,String> x500PrincipalMap = this.parseX509Principal(x500Principal);

            // TODO: Perform more thorough checks
            try {
                x509Certificate.checkValidity();
            } catch (GeneralSecurityException ex) {
                throw new MCPHeaderAuthenticationException(ex.getMessage(), ex);
            }

            // Put some debugging cause this is a really sticking point
            log.debug("MCP token authentication request from {} from organisation {} received",
                    x500PrincipalMap.get(BCStyle.UID),
                    x500PrincipalMap.get(BCStyle.O));

            // If the allowed organisations are restricted, apply that to the access
            if(Strings.isNotBlank(this.allowedPublishersMrn)) {
                log.debug("Allowed publishing entities are with MRN prefix {}", this.allowedPublishersMrn);
                authentication.setAuthenticated(
                        mrn.startsWith(this.allowedPublishersMrn) &&
                        x500PrincipalMap.get(BCStyle.UID).startsWith(this.allowedPublishersMrn)
                );
            }
        } else {
            authentication.setAuthenticated(false);
        }

        // If authenticated, add the admin role since this is a supported publisher
        if(authentication.isAuthenticated()) {
            authentication = new PreAuthenticatedAuthenticationToken(
                    authentication.getPrincipal(),
                    authentication.getCredentials(),
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        // Return the authentication
        log.debug("The final authentication decision was {}", authentication.isAuthenticated());
        return authentication;
    }

    /**
     * Parses the provided X.509 principal into a nicely formatted map.
     *
     * @param x500Principal the X.509 principal
     * @return the generated map
     */
    Map<ASN1ObjectIdentifier,String> parseX509Principal(X500Principal x500Principal) {
        // Initialise the map
        final Map<ASN1ObjectIdentifier, String> principalMap = new HashMap();

        // Parse using X500Name
        X500Name x500name = new X500Name(x500Principal.getName(X500Principal.RFC1779));
        principalMap.put(BCStyle.UID, IETFUtils.valueToString(x500name.getRDNs(BCStyle.UID)[0].getFirst().getValue()));
        principalMap.put(BCStyle.CN, IETFUtils.valueToString(x500name.getRDNs(BCStyle.CN)[0].getFirst().getValue()));
        principalMap.put(BCStyle.O, IETFUtils.valueToString(x500name.getRDNs(BCStyle.O)[0].getFirst().getValue()));
        principalMap.put(BCStyle.OU, IETFUtils.valueToString(x500name.getRDNs(BCStyle.OU)[0].getFirst().getValue()));
        principalMap.put(BCStyle.C, IETFUtils.valueToString(x500name.getRDNs(BCStyle.C)[0].getFirst().getValue()));

        // Return the map
        return principalMap;
    }
}
