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

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.iam.oauth2.client.Oauth2ClientImpl;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class KafkaOAuthServiceComponentTest {

    private KafkaOAuthServiceImpl oauthService;
    private EdcHttpClient mockHttpClient;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(EdcHttpClient.class);
        Oauth2Client oauth2Client = new Oauth2ClientImpl(mockHttpClient, new JacksonTypeManager());
        oauthService = new KafkaOAuthServiceImpl(oauth2Client);
    }

    @Test
    void shouldSuccessfullyRetrieveAccessToken() throws IOException {
        // Arrange
        String tokenUrl = "https://example.com/token";
        OAuthCredentials credentials = new OAuthCredentials(
                tokenUrl,
                "test-client",
                "test-secret"
        );

        // Mock successful HTTP response
        Response mockResponse = mock(Response.class);
        ResponseBody mockResponseBody = mock(ResponseBody.class);

        when(mockResponse.isSuccessful()).thenReturn(true);
        when(mockResponse.code()).thenReturn(200);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn("{\"access_token\":\"test-access-token\",\"token_type\":\"Bearer\"}");

        // Mock the httpClient.execute() call
        when(mockHttpClient.execute(any(Request.class), any(List.class), any(Function.class)))
                .thenAnswer((Answer<Result<TokenRepresentation>>) invocation -> {
                    Request request = invocation.getArgument(0);
                    Function<Response, Result<TokenRepresentation>> responseHandler = invocation.getArgument(2);

                    // Verify the request is POST to /token with correct content type
                    assertThat(request.method()).isEqualTo("POST");
                    assertThat(request.url().encodedPath()).isEqualTo("/token");
                    assertThat(request.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded");

                    return responseHandler.apply(mockResponse);
                });

        // Act
        TokenRepresentation accessToken = oauthService.getAccessToken(credentials);

        // Assert
        assertThat(accessToken.getToken()).isEqualTo("test-access-token");
        verify(mockHttpClient, times(1)).execute(any(Request.class), any(List.class), any(Function.class));
    }

    @Test
    void shouldHandleTokenRetrievalFailure() throws IOException {
        // Arrange
        String tokenUrl = "https://example.com/token";
        OAuthCredentials credentials = new OAuthCredentials(
                tokenUrl,
                "test-client",
                "test-secret"
        );

        // Mock failed HTTP response
        Response mockResponse = mock(Response.class);
        ResponseBody mockResponseBody = mock(ResponseBody.class);

        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(401);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        when(mockResponseBody.string()).thenReturn("{\"error\": \"invalid_client\",\"error_description\": \"Invalid client or Invalid client credentials\"}");

        // Mock the httpClient.execute() call to return failure directly
        when(mockHttpClient.execute(any(Request.class), any(List.class), any(Function.class)))
                .thenAnswer((Answer<Result<TokenRepresentation>>) invocation -> {
                    Request request = invocation.getArgument(0);

                    // Verify the request is POST to /token with correct content type
                    assertThat(request.method()).isEqualTo("POST");
                    assertThat(request.url().encodedPath()).isEqualTo("/token");
                    assertThat(request.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded");

                    return Result.failure("Invalid client or Invalid client credentials");
                });

        // Act & Assert
        assertThatThrownBy(() -> oauthService.getAccessToken(credentials))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("Failed to obtain OAuth2 token:");
        verify(mockHttpClient, times(1)).execute(any(Request.class), any(List.class), any(Function.class));
    }
}