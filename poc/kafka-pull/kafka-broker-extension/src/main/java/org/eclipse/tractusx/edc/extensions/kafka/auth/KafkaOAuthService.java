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

/**
 * Interface for services that handle OAuth2 access token operations for Kafka authentication.
 * Defines methods to fetch and revoke OAuth2 access tokens using the Client Credentials flow.
 */
public interface KafkaOAuthService {

    /**
     * Always performs a client_credentials flow and returns a fresh token.
     *
     * @param creds The OAuth credentials to use for token acquisition
     * @return The acquired access token as a string
     */
    String getAccessToken(OAuthCredentials creds);

    /**
     * Revokes the given token.
     *
     * @param creds The OAuth credentials used for token revocation
     * @param token The token to revoke
     */
    void revokeToken(OAuthCredentials creds, String token);
}