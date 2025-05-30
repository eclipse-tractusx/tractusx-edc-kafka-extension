/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 * Copyright (c) 2025 Cofinity-X GmbH
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

package org.eclipse.tractusx.edc.local;

import org.eclipse.tractusx.edc.local.services.DummyBdrsClient;
import org.eclipse.tractusx.edc.local.services.DummySecureTokenService;
import org.eclipse.tractusx.edc.local.services.DummyTokenValidationService;
import org.eclipse.tractusx.edc.local.services.VaultPublicKeyResolver;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.iam.identitytrust.spi.SecureTokenService;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.token.spi.TokenValidationService;
import org.eclipse.tractusx.edc.spi.identity.mapper.BdrsClient;

@Extension(value = "Vault public key resolver extension")
public class LocalServicesExtension implements ServiceExtension {

  @Inject
  private Monitor monitor;

  @Inject
  private Vault vault;

  @Provider
  public DidPublicKeyResolver didPublicKeyResolver() {
    var pemParser = new PemParser(monitor);
    return new VaultPublicKeyResolver(vault, pemParser);
  }

  @Provider
  public SecureTokenService secureTokenService() {
    return new DummySecureTokenService();
  }

  @Provider
  public TokenValidationService tokenValidationService() {
    return new DummyTokenValidationService();
  }

  @Provider
  public BdrsClient bdrsClient() {
    return new DummyBdrsClient();
  }
}
