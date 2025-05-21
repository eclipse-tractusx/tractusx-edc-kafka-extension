/*
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

package org.eclipse.tractusx.edc.vault;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Extension(value = "In-memory vault seeding extension")
public class SeedInMemoryVaultExtension implements ServiceExtension {

  @Setting
  static final String VAULT_LOCATION = "edc.vault";

  @Inject
  private Vault vault;

  @Provider
  public Vault seedInMemoryVaultFromFs(ServiceExtensionContext context) {
    var vaultLocation = context.getSetting(VAULT_LOCATION, null);
    if (vaultLocation == null) {
      context.getMonitor().warning("No vault file configured. Skip seeding.");
      return vault;
    }

    var vaultPath = Paths.get(vaultLocation);
    if (!Files.exists(vaultPath)) {
      throw new EdcException("Vault file does not exist: " + vaultLocation);
    }

    loadSecretFile(vaultPath);

    return vault;
  }

  private void loadSecretFile(Path vaultPath) {
    try (var stream = Files.newInputStream(vaultPath)) {
      var properties = new Properties();
      properties.load(stream);
      properties.stringPropertyNames().forEach(key -> vault.storeSecret(key, properties.getProperty(key)));
    } catch (IOException e) {
      throw new EdcException(e);
    }
  }
}
