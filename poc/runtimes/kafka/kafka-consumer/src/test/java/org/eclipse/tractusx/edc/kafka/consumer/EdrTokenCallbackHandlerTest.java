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
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerTokenCallback;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.Map;

import static org.apache.kafka.common.config.SaslConfigs.SASL_OAUTHBEARER_SCOPE_CLAIM_NAME;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EdrTokenCallbackHandlerTest {

    @Test
    void shouldHandleOAuthBearerTokenCallbackSuccessfully() throws Exception {
        // Arrange
        EdrTokenCallbackHandler.EdrDataProvider mockEdrDataProvider = mock(EdrTokenCallbackHandler.EdrDataProvider.class);
        EDRData mockEdrData = mock(EDRData.class);
        when(mockEdrDataProvider.getEdrData()).thenReturn(mockEdrData);
        when(mockEdrData.getAuthorization()).thenReturn("mockAccessToken");

        OAuthBearerTokenCallback mockCallback = mock(OAuthBearerTokenCallback.class);

        EdrTokenCallbackHandler handler = new EdrTokenCallbackHandler(mockEdrDataProvider);
        handler.configure(Map.of(SASL_OAUTHBEARER_SCOPE_CLAIM_NAME, "scope1"), "mechanism", null);

        DecodedJWT mockDecodedJWT = mock(DecodedJWT.class);
        Claim mockClaim = mock(Claim.class);
        when(mockDecodedJWT.getIssuedAtAsInstant()).thenReturn(java.time.Instant.now().minusSeconds(1000));
        when(mockDecodedJWT.getExpiresAtAsInstant()).thenReturn(java.time.Instant.now().plusSeconds(1000));
        when(mockDecodedJWT.getSubject()).thenReturn("subject");
        when(mockDecodedJWT.getClaim(anyString())).thenReturn(mockClaim);
        when(mockClaim.asString()).thenReturn("scope1 scope2");
        try (var jwtMock = Mockito.mockStatic(JWT.class)) {
            jwtMock.when(() -> JWT.decode("mockAccessToken")).thenReturn(mockDecodedJWT);

            // Act
            handler.handle(new Callback[]{mockCallback});

            // Assert
            verify(mockCallback, times(1)).token(any(OAuthBearerToken.class));
        }
    }

    @Test
    void shouldThrowUnsupportedCallbackExceptionForUnsupportedCallbackType() {
        // Arrange
        EdrTokenCallbackHandler handler = new EdrTokenCallbackHandler();
        handler.configure(Map.of(), "mechanism", null);

        Callback unsupportedCallback = mock(Callback.class);

        // Act & Assert
        assertThrows(UnsupportedCallbackException.class,
                () -> handler.handle(new Callback[]{unsupportedCallback}));
    }

    @Test
    void shouldThrowIOExceptionWhenEdrDataIsNull() throws Exception {
        // Arrange
        EdrTokenCallbackHandler.EdrDataProvider mockEdrDataProvider = mock(EdrTokenCallbackHandler.EdrDataProvider.class);
        when(mockEdrDataProvider.getEdrData()).thenReturn(null);

        OAuthBearerTokenCallback mockCallback = mock(OAuthBearerTokenCallback.class);

        EdrTokenCallbackHandler handler = new EdrTokenCallbackHandler(mockEdrDataProvider);
        handler.configure(Map.of(), "mechanism", null);

        // Act & Assert
        assertThrows(IOException.class,
                () -> handler.handle(new Callback[]{mockCallback}));
    }

    @Test
    void shouldThrowIOExceptionWhenEdrDataContainsNoToken() throws Exception {
        // Arrange
        EdrTokenCallbackHandler.EdrDataProvider mockEdrDataProvider = mock(EdrTokenCallbackHandler.EdrDataProvider.class);
        EDRData mockEdrData = mock(EDRData.class);
        when(mockEdrDataProvider.getEdrData()).thenReturn(mockEdrData);
        when(mockEdrData.getAuthorization()).thenReturn(null);

        OAuthBearerTokenCallback mockCallback = mock(OAuthBearerTokenCallback.class);

        EdrTokenCallbackHandler handler = new EdrTokenCallbackHandler(mockEdrDataProvider);
        handler.configure(Map.of(), "mechanism", null);

        // Act & Assert
        assertThrows(IOException.class,
                () -> handler.handle(new Callback[]{mockCallback}));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenHandlerNotInitialized() {
        // Arrange
        EdrTokenCallbackHandler handler = new EdrTokenCallbackHandler();

        OAuthBearerTokenCallback mockCallback = mock(OAuthBearerTokenCallback.class);

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> handler.handle(new Callback[]{mockCallback}));
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenJwtTokenInvalid() throws Exception {
        // Arrange
        EdrTokenCallbackHandler.EdrDataProvider mockEdrDataProvider = mock(EdrTokenCallbackHandler.EdrDataProvider.class);
        EDRData mockEdrData = mock(EDRData.class);
        when(mockEdrDataProvider.getEdrData()).thenReturn(mockEdrData);
        when(mockEdrData.getAuthorization()).thenReturn("invalidToken");

        OAuthBearerTokenCallback mockCallback = mock(OAuthBearerTokenCallback.class);

        EdrTokenCallbackHandler handler = new EdrTokenCallbackHandler(mockEdrDataProvider);
        handler.configure(Map.of(), "mechanism", null);

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> handler.handle(new Callback[]{mockCallback}));
    }
}