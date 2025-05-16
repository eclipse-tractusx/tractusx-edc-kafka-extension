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
package org.eclipse.tractusx.edc.validator.dataaddress.kafka;

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(DependencyInjectionExtension.class)
class KafkaBrokerDataAddressValidatorExtensionTest {

    private final DataAddressValidatorRegistry dataAddressValidatorRegistry = mock();

    @BeforeEach
    void setUp(final ServiceExtensionContext context) {
        context.registerService(DataAddressValidatorRegistry.class, dataAddressValidatorRegistry);
    }

    @Test
    void initialize_shouldRegisterValidatorsWithKafkaType(final KafkaBrokerDataAddressValidatorExtension extension, final ServiceExtensionContext context) {
        // Arrange
        extension.initialize(context);

        verify(dataAddressValidatorRegistry, times(1))
                .registerSourceValidator(anyString(), any(KafkaBrokerDataAddressValidator.class));
        verify(dataAddressValidatorRegistry, times(1))
                .registerDestinationValidator(anyString(), any(KafkaBrokerDataAddressValidator.class));
    }
}