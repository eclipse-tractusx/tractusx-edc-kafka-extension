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
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class KafkaOAuthServiceImplTest {

    private static final String TOKEN_URL = "https://token.url";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String TEST_TOKEN = "test-token";

    private Oauth2Client mockOauth2Client;
    private KafkaOAuthServiceImpl oauthService;

    @BeforeEach
    void setUp() {
        mockOauth2Client = mock(Oauth2Client.class);
        oauthService = new KafkaOAuthServiceImpl(mockOauth2Client);
    }

    @Nested
    class GetAccessTokenTests {
        private static OAuthCredentials oauthCredentials() {
            return new OAuthCredentials(TOKEN_URL, CLIENT_ID, CLIENT_SECRET);
        }

        @Test
        void shouldReturnAccessToken_whenResponseIsSuccessful() {
            // Arrange
            when(mockOauth2Client.requestToken(any())).thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(TEST_TOKEN).build()));

            // Act
            TokenRepresentation accessToken = oauthService.getAccessToken(oauthCredentials());

            // Assert
            assertEquals(TEST_TOKEN, accessToken.getToken());
            verify(mockOauth2Client, times(1)).requestToken(any());
        }

        @Test
        void shouldThrowException_whenResponseIsNotSuccessful() {
            // Arrange
            when(mockOauth2Client.requestToken(any())).thenReturn(Result.failure("Reason"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> oauthService.getAccessToken(oauthCredentials()));
            assertEquals("Failed to obtain OAuth2 token: Reason", exception.getMessage());
            verify(mockOauth2Client, times(1)).requestToken(any());
        }
    }
}