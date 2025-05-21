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

import java.util.Optional;

/**
 * Simple immutable holder for the four OAuth2 parameters.
 *
 * @param tokenUrl       The endpoint URL to fetch OAuth2 tokens using client credentials flow.
 * @param revocationUrl  The optional endpoint URL used to revoke tokens. This may be empty if token revocation is not supported.
 * @param clientId       The identifier of the client application attempting to authenticate.
 * @param clientSecret   The secret associated with the client identifier for authentication.
 */
public record OAuthCredentials(String tokenUrl, Optional<String> revocationUrl, String clientId, String clientSecret) {

}
