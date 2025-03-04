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
package org.eclipse.tractusx.edc.dataaddress.kafka.spi;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Defines the schema of a DataAddress representing a Kafka endpoint.
 */
public interface KafkaBrokerDataAddressSchema {

    /**
     * The transfer type.
     */
    String KAFKA_TYPE = "KafkaBroker";

    /**
     * The Kafka topic that will be allowed to poll for the consumer.
     */
    String TOPIC = EDC_NAMESPACE + "topic";

    /**
     * The bootstrap.servers property
     */
    String BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";

    /**
     * The sasl.mechanism property
     */
    String MECHANISM = "kafka.sasl.mechanism";

    /**
     * The security.protocol property
     */
    String PROTOCOL = "kafka.security.protocol";

    /**
     * The duration of the consumer polling.
     * <p>
     * The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.
     * This parameter is optional. Default value is 1s.
     *
     * @see java.time.Duration#parse(CharSequence) for ISO-8601 duration format
     */
    String POLL_DURATION = EDC_NAMESPACE + "pollDuration";

    /**
     * The secret token/credentials
     */
    String SECRET_KEY = EDC_NAMESPACE + "secretKey";

    /**
     * The groupPrefix that will be allowed to use for the consumer
     */
    String GROUP_PREFIX = EDC_NAMESPACE + "groupPrefix";
}
