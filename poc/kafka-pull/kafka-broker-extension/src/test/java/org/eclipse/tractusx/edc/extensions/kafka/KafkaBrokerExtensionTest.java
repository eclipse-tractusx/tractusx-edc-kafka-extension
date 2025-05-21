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
package org.eclipse.tractusx.edc.extensions.kafka;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;

@ExtendWith(DependencyInjectionExtension.class)
class KafkaBrokerExtensionTest {

    private final DataFlowManager dataFlowManager = mock();

    private final Vault vault = mock();

    @BeforeEach
    void setUp(final ServiceExtensionContext context) {
        context.registerService(DataFlowManager.class, dataFlowManager);
        context.registerService(Vault.class, vault);
    }

    @Test
    void initialize_RegistersKafkaDataFlowController(final KafkaBrokerExtension extension, final ServiceExtensionContext context) {
        extension.initialize(context);

        verify(dataFlowManager, times(1))
                .register(any(KafkaBrokerDataFlowController.class));
    }
}
