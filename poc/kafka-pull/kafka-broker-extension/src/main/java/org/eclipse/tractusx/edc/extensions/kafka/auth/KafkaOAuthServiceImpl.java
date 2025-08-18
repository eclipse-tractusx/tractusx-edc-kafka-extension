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
package org.eclipse.tractusx.edc.extensions.kafka.auth;

import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.iam.oauth2.spi.client.SharedSecretOauth2CredentialsRequest;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;

/**
 * Stateless service to fetch and revoke OAuth2 access tokens using the Client Credentials flow.
 * No token is cached—each getAccessToken() call always retrieves a new token.
 */
public class KafkaOAuthServiceImpl implements KafkaOAuthService {
    static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
    private final Oauth2Client oauth2Client;

    public KafkaOAuthServiceImpl(final Oauth2Client oauth2Client) {
        this.oauth2Client = oauth2Client;
    }

    /**
     * Always performs a client_credentials flow and returns a fresh token.
     */
    public TokenRepresentation getAccessToken(final OAuthCredentials creds) {
        var request = SharedSecretOauth2CredentialsRequest.Builder.newInstance()
                .clientId(creds.clientId())
                .clientSecret(creds.clientSecret())
                .url(creds.tokenUrl())
                .grantType(CLIENT_CREDENTIALS_GRANT_TYPE)
                .build();
        Result<TokenRepresentation> result = oauth2Client.requestToken(request);
        if (result.failed()) {
            throw new RuntimeException("Failed to obtain OAuth2 token: " + result.getFailureDetail());
        }
        return result.getContent();
    }

}