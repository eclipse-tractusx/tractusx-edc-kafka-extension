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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class KafkaOAuthServiceImplTest {

    private static final String TOKEN_URL = "https://token.url";
    private static final String REVOKE_URL = "https://revoke.url";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String TEST_TOKEN = "test-token";
    private static final String IO_ERROR_MESSAGE = "IO error";
    public static final String ACCESS_TOKEN_KEY = "access_token";

    private EdcHttpClient mockHttpClient;
    private ObjectMapper mockObjectMapper;
    private KafkaOAuthServiceImpl oauthService;
    private Response mockResponse;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(EdcHttpClient.class);
        mockObjectMapper = mock(ObjectMapper.class);
        mockResponse = mock(Response.class);
        oauthService = new KafkaOAuthServiceImpl(mockHttpClient, mockObjectMapper);
    }

    @Nested
    class RevokeTokenTests {
        private OAuthCredentials createCredentialsWithRevocationUrl() {
            return new OAuthCredentials(TOKEN_URL, Optional.of(REVOKE_URL), CLIENT_ID, CLIENT_SECRET);
        }

        private OAuthCredentials createCredentialsWithoutRevocationUrl() {
            return new OAuthCredentials(TOKEN_URL, Optional.empty(), CLIENT_ID, CLIENT_SECRET);
        }

        @Test
        void shouldExecuteSuccessfully_whenResponseIsSuccessful() throws IOException {
            // Arrange
            when(mockResponse.isSuccessful()).thenReturn(true);
            when(mockHttpClient.execute(any(Request.class))).thenReturn(mockResponse);

            // Act
            oauthService.revokeToken(createCredentialsWithRevocationUrl(), TEST_TOKEN);

            // Assert
            verify(mockHttpClient, times(1)).execute(any(Request.class));
        }

        @Test
        void shouldThrowException_whenResponseIsNotSuccessful() throws IOException {
            // Arrange
            when(mockResponse.isSuccessful()).thenReturn(false);
            when(mockResponse.code()).thenReturn(403);
            when(mockHttpClient.execute(any(Request.class))).thenReturn(mockResponse);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> oauthService.revokeToken(createCredentialsWithRevocationUrl(), TEST_TOKEN));
            assertEquals("Revoke endpoint returned HTTP 403", exception.getMessage());
            verify(mockHttpClient, times(1)).execute(any(Request.class));
        }

        @Test
        void shouldThrowException_whenIOExceptionOccurs() throws IOException {
            // Arrange
            when(mockHttpClient.execute(any(Request.class))).thenThrow(new IOException(IO_ERROR_MESSAGE));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> oauthService.revokeToken(createCredentialsWithRevocationUrl(), TEST_TOKEN));
            assertEquals("Failed to revoke OAuth2 token", exception.getMessage());
            verify(mockHttpClient, times(1)).execute(any(Request.class));
        }

        @Test
        void shouldDoNothing_whenRevocationUrlIsEmpty() throws IOException {
            // Act
            oauthService.revokeToken(createCredentialsWithoutRevocationUrl(), TEST_TOKEN);

            // Assert
            verify(mockHttpClient, never()).execute(any(Request.class));
        }
    }

    @Nested
    class GetAccessTokenTests {
        private OAuthCredentials createCredentials() {
            return new OAuthCredentials(TOKEN_URL, Optional.empty(), CLIENT_ID, CLIENT_SECRET);
        }

        @Test
        void shouldReturnAccessToken_whenResponseIsSuccessful() throws IOException {
            // Arrange
            String mockResponseBody = "{\"access_token\": \"test-token\"}";
            ResponseBody mockResponseBodyObj = mock(ResponseBody.class);
            JsonNode mockJsonNode = mock(JsonNode.class);
            JsonNode mockTokenNode = mock(JsonNode.class);

            when(mockResponse.isSuccessful()).thenReturn(true);
            when(mockResponse.body()).thenReturn(mockResponseBodyObj);
            when(mockResponseBodyObj.string()).thenReturn(mockResponseBody);
            when(mockJsonNode.get(ACCESS_TOKEN_KEY)).thenReturn(mockTokenNode);
            when(mockTokenNode.asText()).thenReturn(TEST_TOKEN);
            when(mockHttpClient.execute(any(Request.class))).thenReturn(mockResponse);
            when(mockObjectMapper.readTree(mockResponseBody)).thenReturn(mockJsonNode);

            // Act
            String accessToken = oauthService.getAccessToken(createCredentials());

            // Assert
            assertEquals(TEST_TOKEN, accessToken);
            verify(mockHttpClient, times(1)).execute(any(Request.class));
        }

        @Test
        void shouldThrowException_whenResponseIsNotSuccessful() throws IOException {
            // Arrange
            when(mockResponse.isSuccessful()).thenReturn(false);
            when(mockResponse.code()).thenReturn(401);
            when(mockHttpClient.execute(any(Request.class))).thenReturn(mockResponse);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> oauthService.getAccessToken(createCredentials()));
            assertEquals("OAuth2 token endpoint returned HTTP 401", exception.getMessage());
            verify(mockHttpClient, times(1)).execute(any(Request.class));
        }

        @Test
        void shouldThrowException_whenIOExceptionOccurs() throws IOException {
            // Arrange
            when(mockHttpClient.execute(any(Request.class))).thenThrow(new IOException(IO_ERROR_MESSAGE));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> oauthService.getAccessToken(createCredentials()));
            assertEquals("Failed to fetch OAuth2 token", exception.getMessage());
            verify(mockHttpClient, times(1)).execute(any(Request.class));
        }
    }
}