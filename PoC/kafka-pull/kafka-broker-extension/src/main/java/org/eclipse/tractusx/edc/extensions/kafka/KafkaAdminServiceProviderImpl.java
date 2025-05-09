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

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ScramMechanism;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.MECHANISM;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.PROTOCOL;

public class KafkaAdminServiceProviderImpl implements KafkaAdminServiceProvider{

    public KafkaAdminServiceProviderImpl() {
    }

    @Override
    public KafkaAdminService provide(final DataAddress contentDataAddress, final String secret) {
        var mechanism = contentDataAddress.getStringProperty(MECHANISM);
        var adminProps = new Properties();
        adminProps.put(BOOTSTRAP_SERVERS_CONFIG, contentDataAddress.getStringProperty(BOOTSTRAP_SERVERS));
        adminProps.put(SECURITY_PROTOCOL_CONFIG, contentDataAddress.getStringProperty(PROTOCOL));
        adminProps.put(SASL_MECHANISM, mechanism);
        adminProps.put(SASL_JAAS_CONFIG, secret);

        return new KafkaAdminService(Admin.create(adminProps), ScramMechanism.fromMechanismName(mechanism));
    }
}
