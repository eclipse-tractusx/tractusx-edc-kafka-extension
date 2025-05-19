/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.eclipse.tractusx.edc.kafka.consumer;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.apache.kafka.common.security.oauthbearer.internals.secured.BasicOAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.internals.secured.ConfigurationUtils;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_SCOPE_CLAIM_NAME;
import static org.eclipse.tractusx.edc.kafka.consumer.KafkaConsumerApp.ASSET_ID;

/**
 * Callback handler that retrieves an OAuth bearer token from EDRData and converts it to
 * a format suitable for Kafka SASL/OAUTHBEARER authentication.
 * <p>
 * This handler processes OAuthBearerTokenCallback instances by:
 * 1. Retrieving the access token from an EDR provider
 * 2. Parsing the JWT token to extract required claims
 * 3. Creating a BasicOAuthBearerToken that Kafka can use for authentication
 */
@Slf4j
public class EdrTokenCallbackHandler implements AuthenticateCallbackHandler {


    private boolean isInitialized = false;
    private String scopeClaimName;
    private final EdrDataProvider edrDataProvider;

    /**
     * Constructor with default EdrDataProvider implementation
     */
    public EdrTokenCallbackHandler() {
        this.edrDataProvider = new DefaultEdrDataProvider();
    }

    /**
     * Constructor with custom EdrDataProvider for testing
     *
     * @param edrDataProvider custom provider implementation
     */
    public EdrTokenCallbackHandler(final EdrDataProvider edrDataProvider) {
        this.edrDataProvider = edrDataProvider;
    }

    @Override
    public void configure(final Map<String, ?> configs, final String mechanism, final List<AppConfigurationEntry> jaasConfigEntries) {
        ConfigurationUtils cu = new ConfigurationUtils(configs, mechanism);
        scopeClaimName = cu.get(SASL_OAUTHBEARER_SCOPE_CLAIM_NAME);
        isInitialized = true;
    }

    /**
     * Handles an array of {@link Callback} objects, processing each callback as needed.
     * If the callback is of type {@link OAuthBearerTokenCallback}, it manages the token retrieval
     * and sets the token on the callback. Unsupported callback types will result in an exception.
     *
     * @param callbacks an array of {@link Callback} objects to be processed
     * @throws UnsupportedCallbackException if one or more callbacks are of an unsupported type
     * @throws IOException if an I/O error occurs during token retrieval
     */
    @Override
    public void handle(final Callback[] callbacks) throws UnsupportedCallbackException, IOException {
        checkInitialized();

        for (Callback callback : callbacks) {
            if (callback instanceof OAuthBearerTokenCallback tokenCallback) {
                try {
                    handleTokenCallback(tokenCallback);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while handling OAuthBearerTokenCallback", e);
                    Thread.currentThread().interrupt();
                }
            } else {
                throw new UnsupportedCallbackException(callback, "Unsupported callback type");
            }
        }
    }

    private void handleTokenCallback(final OAuthBearerTokenCallback callback) throws IOException, InterruptedException {
        log.debug("Handling OAuthBearerTokenCallback");
        String accessToken = fetchAccessTokenFromEDC();
        OAuthBearerToken oAuthBearerToken = toOAuthBearerToken(accessToken);
        callback.token(oAuthBearerToken);
        log.debug("OAuth bearer token successfully provided to callback");
    }

    private OAuthBearerToken toOAuthBearerToken(final String accessToken) {
        log.debug("Converting JWT access token to OAuthBearerToken");
        try {
            DecodedJWT jwt = JWT.decode(accessToken);
            long issuedAt = jwt.getIssuedAtAsInstant().toEpochMilli();
            long expiresAt = jwt.getExpiresAtAsInstant().toEpochMilli();
            String subject = jwt.getSubject();
            String scopeClaim = jwt.getClaim(scopeClaimName).asString();
            Set<String> scopes = Set.of(scopeClaim);

            return new BasicOAuthBearerToken(accessToken, scopes, expiresAt, subject, issuedAt);
        } catch (JWTDecodeException e) {
            throw new IllegalStateException("Failed to decode JWT token: " + e.getMessage(), e);
        }
    }

    private String fetchAccessTokenFromEDC() throws IOException, InterruptedException {
        log.debug("Fetching access token from EDC");
        EDRData edrData = edrDataProvider.getEdrData();

        if (edrData == null) {
            throw new IOException("No EDR data found");
        }

        String token = edrData.getToken();
        if (token == null || token.isEmpty()) {
            throw new IOException("EDR data contains no token");
        }

        log.debug("Successfully retrieved access token from EDC");
        return token;
    }

    @Override
    public void close() {
        // Nothing to clean up
        log.debug("EdrTokenCallbackHandler closed");
    }

    private void checkInitialized() {
        if (!isInitialized) {
            throw new IllegalStateException(
                    String.format("To use %s, first call the configure method", getClass().getSimpleName())
            );
        }
    }

    /**
     * Interface for providing EDR data, allows for dependency injection and testing
     */
    public interface EdrDataProvider {
        EDRData getEdrData() throws IOException, InterruptedException;
    }

    /**
     * Default implementation of EdrDataProvider that uses EdrProvider
     */
    @RequiredArgsConstructor
    private static class DefaultEdrDataProvider implements EdrDataProvider {

        @Override
        public EDRData getEdrData() throws IOException, InterruptedException {
            return new DataTransferClient().executeDataTransferWorkflow(ASSET_ID);
        }
    }
}