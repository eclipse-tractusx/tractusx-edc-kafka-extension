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
package org.eclipse.tractusx.edc.dataaddress.kafka.spi;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Defines the schema of a DataAddress representing a Kafka endpoint.
 */
public interface KafkaBrokerDataAddressSchema {

    String KAFKA_BOOTSTRAP_SERVERS_PROPERTY = "bootstrap.servers";
    String KAFKA_SECURITY_PROTOCOL_PROPERTY = "security.protocol";
    String KAFKA_SASL_MECHANISM_PROPERTY = "sasl.mechanism";

    String KAFKA_PROPERTIES_PREFIX = EDC_NAMESPACE + "kafka.";

    /**
     * The transfer type.
     */
    String KAFKA_TYPE = "KafkaBroker";

    /**
     * The Kafka topic that will be allowed to poll for the consumer.
     */
    String TOPIC = EDC_NAMESPACE + "topic";

    /**
     * The kafka.bootstrap.servers property.
     */
    String BOOTSTRAP_SERVERS = KAFKA_PROPERTIES_PREFIX + KAFKA_BOOTSTRAP_SERVERS_PROPERTY;

    /**
     * The kafka.poll.duration property which specifies the duration of the consumer polling.
     * <p>
     * The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.
     * This parameter is optional. The default value is 1 second.
     *
     * @see java.time.Duration#parse(CharSequence) for ISO-8601 duration format
     */
    String POLL_DURATION = KAFKA_PROPERTIES_PREFIX + "poll.duration";

    /**
     * The kafka.group.prefix that will be allowed to use for the consumer.
     */
    String GROUP_PREFIX = KAFKA_PROPERTIES_PREFIX + "group.prefix";

    /**
     * The security.protocol property.
     */
    String PROTOCOL = KAFKA_PROPERTIES_PREFIX + KAFKA_SECURITY_PROTOCOL_PROPERTY;

    /**
     * The sasl.mechanism property.
     */
    String MECHANISM = KAFKA_PROPERTIES_PREFIX + KAFKA_SASL_MECHANISM_PROPERTY;

    /**
     * The authorization token.
     */
    String TOKEN = EDC_NAMESPACE + "authorization";

    /**
     * The OAuth token URL for retrieving access tokens.
     */
    String OAUTH_TOKEN_URL = EDC_NAMESPACE + "tokenUrl";

    /**
     * The OAuth client ID.
     */
    String OAUTH_CLIENT_ID = EDC_NAMESPACE + "clientId";

    /**
     * The OAuth client secret key.
     */
    String OAUTH_CLIENT_SECRET_KEY = EDC_NAMESPACE + "clientSecretKey";
}