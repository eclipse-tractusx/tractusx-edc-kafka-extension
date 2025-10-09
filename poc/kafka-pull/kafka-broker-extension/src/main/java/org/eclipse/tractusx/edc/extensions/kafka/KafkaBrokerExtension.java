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
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.tractusx.edc.extensions.kafka.acl.KafkaAclServiceImpl;
import org.eclipse.tractusx.edc.extensions.kafka.auth.KafkaOAuthServiceImpl;

import java.util.Map;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.*;
import static org.apache.kafka.common.config.SslConfigs.*;
import static org.eclipse.tractusx.edc.core.utils.ConfigUtil.missingMandatoryProperty;

/**
 * Kafka Broker flow extension
 */
@Extension(value = KafkaBrokerExtension.NAME)
public class KafkaBrokerExtension implements ServiceExtension {

    public static final String NAME = "Kafka stream extension";

    // Basic Kafka connection properties
    @Setting(value = "Kafka bootstrap servers for AdminClient connection", required = true)
    public static final String KAFKA_BOOTSTRAP_SERVERS = "edc.kafka.bootstrap.servers";

    public static final String DEFAULT_SECURITY_PROTOCOL = "SASL_SSL";
    @Setting(value = "Kafka security protocol for AdminClient connection", defaultValue = DEFAULT_SECURITY_PROTOCOL)
    public static final String KAFKA_SECURITY_PROTOCOL = "edc.kafka.security.protocol";

    public static final String DEFAULT_SASL_MECHANISM = "OAUTHBEARER";
    @Setting(value = "Kafka SASL mechanism for AdminClient connection", defaultValue = DEFAULT_SASL_MECHANISM)
    public static final String KAFKA_SASL_MECHANISM = "edc.kafka.sasl.mechanism";

    // Authentication properties for AdminClient
    @Setting(value = "OAuth token endpoint URL for Kafka AdminClient authentication", required = true)
    public static final String KAFKA_ADMIN_TOKEN_URL = "edc.kafka.admin.token.url";

    @Setting(value = "OAuth client ID for Kafka AdminClient authentication", required = true)
    public static final String KAFKA_ADMIN_CLIENT_ID = "edc.kafka.admin.client.id";

    @Setting(value = "Vault key for OAuth client secret for Kafka AdminClient authentication", required = true)
    public static final String KAFKA_ADMIN_CLIENT_SECRET_KEY = "edc.kafka.admin.client.secret.key";

    // SSL Configuration properties for AdminClient
    @Setting(value = "SSL truststore location for Kafka AdminClient")
    public static final String KAFKA_SSL_TRUSTSTORE_LOCATION = "edc.kafka.ssl.truststore.location";

    @Setting(value = "SSL truststore password for Kafka AdminClient")
    public static final String KAFKA_SSL_TRUSTSTORE_PASSWORD = "edc.kafka.ssl.truststore.password";

    @Setting(value = "SSL endpoint identification algorithm for Kafka AdminClient")
    public static final String KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM = "edc.kafka.ssl.endpoint.identification.algorithm";

    @Setting(value = "The file format of the trust store file for Kafka AdminClient")
    public static final String KAFKA_SSL_TRUSTSTORE_TYPE_CONFIG = "edc.kafka.ssl.truststore.type";

    @Setting(value = "Flag to enable or disable Kafka ACL management.")
    public static final String KAFKA_ACL_ENABLED = "edc.kafka.acl.enabled";

    @Inject
    private DataFlowManager dataFlowManager;

    @Inject
    private Vault vault;

    @Inject
    private Oauth2Client oauth2Client;

    @Inject(required = false)
    private DataFlowPropertiesProvider propertiesProvider;

    @Inject
    private TransferTypeParser transferTypeParser;

    @Inject
    private Monitor monitor;

    @Override
    public void initialize(final ServiceExtensionContext context) {
        Properties kafkaProperties = createKafkaAdminProperties(context);

        boolean aclEnabled = context.getSetting(KAFKA_ACL_ENABLED, true);
        var kafkaAclService = new KafkaAclServiceImpl(kafkaProperties, monitor);

        var kafkaOAuthService = new KafkaOAuthServiceImpl(oauth2Client);
        var controller = new KafkaBrokerDataFlowController(vault, kafkaOAuthService, kafkaAclService, transferTypeParser, getPropertiesProvider(), aclEnabled);
        dataFlowManager.register(controller);
    }

    private Properties createKafkaAdminProperties(ServiceExtensionContext context) {
        Properties properties = new Properties();
        // Basic connection properties
        String bootstrapServers = context.getSetting(KAFKA_BOOTSTRAP_SERVERS, null);
        if (bootstrapServers == null) {
            missingMandatoryProperty(monitor, KAFKA_BOOTSTRAP_SERVERS);
        }
        properties.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(SECURITY_PROTOCOL_CONFIG, context.getSetting(KAFKA_SECURITY_PROTOCOL, DEFAULT_SECURITY_PROTOCOL));
        properties.put(SASL_MECHANISM, context.getSetting(KAFKA_SASL_MECHANISM, DEFAULT_SASL_MECHANISM));


        // Authentication configuration
        String tokenUrl = context.getSetting(KAFKA_ADMIN_TOKEN_URL, null);
        String clientId = context.getSetting(KAFKA_ADMIN_CLIENT_ID, null);
        String clientSecretKey = context.getSetting(KAFKA_ADMIN_CLIENT_SECRET_KEY, null);

        if (tokenUrl != null && clientId != null && clientSecretKey != null) {
            // Get client secret from vault
            String clientSecret = vault.resolveSecret(clientSecretKey);
            if (clientSecret != null) {
                // Configure SASL/OAUTHBEARER authentication
                String jaasConfig =
                        "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
                                "clientId=\"" + clientId + "\" " +
                                "clientSecret=\"" + clientSecret + "\";";
                properties.put(SASL_JAAS_CONFIG, jaasConfig);
                properties.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler");
                properties.put(SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, tokenUrl);

                monitor.info("Configured Kafka AdminClient with OAuth authentication");
            } else {
                monitor.warning("Kafka AdminClient client secret not found in vault: " + clientSecretKey + ". Using basic configuration only.");
            }
        } else {
            monitor.info("Kafka AdminClient authentication not fully configured. Using basic connection properties only.");
        }

        // SSL Configuration
        String truststoreLocation = context.getSetting(KAFKA_SSL_TRUSTSTORE_LOCATION, null);
        String truststorePassword = context.getSetting(KAFKA_SSL_TRUSTSTORE_PASSWORD, null);
        String endpointIdentificationAlgorithm = context.getSetting(KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM, null);
        String truststoreType = context.getSetting(KAFKA_SSL_TRUSTSTORE_TYPE_CONFIG, null);

        if (truststoreLocation != null) {
            properties.put(SSL_TRUSTSTORE_LOCATION_CONFIG, truststoreLocation);
        }

        if (truststorePassword != null) {
            properties.put(SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePassword);
        }

        if (endpointIdentificationAlgorithm != null) {
            properties.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, endpointIdentificationAlgorithm);
        }

        if (truststoreType != null) {
            properties.put(SSL_TRUSTSTORE_TYPE_CONFIG, truststoreType);
        }

        return properties;
    }

    private DataFlowPropertiesProvider getPropertiesProvider() {
        return propertiesProvider == null ? (tp, p) -> StatusResult.success(Map.of()) : propertiesProvider;
    }
}