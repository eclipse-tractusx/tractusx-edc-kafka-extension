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
package org.eclipse.tractusx.edc.kafka.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the Endpoint Data Reference (EDR) data needed for Kafka consumer configuration.
 * Contains connection parameters, authentication details, and topic information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EDRData {
    private static final String KAFKA_SECURITY_PROTOCOL = "kafka.security.protocol";
    private static final String KAFKA_SASL_MECHANISM = "kafka.sasl.mechanism";
    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
    private static final String KAFKA_GROUP_PREFIX = "kafka.group.prefix";
    private static final String KAFKA_POLL_DURATION = "kafka.poll.duration";
    private String id;
    private String contractId;
    private String endpoint;
    private String topic;
    private String authKey;
    private String authCode;
    private String token;

    @JsonProperty(KAFKA_POLL_DURATION)
    private String kafkaPollDuration;
    @JsonProperty(KAFKA_GROUP_PREFIX)
    private String kafkaGroupPrefix;
    @JsonProperty(KAFKA_SECURITY_PROTOCOL)
    private String kafkaSecurityProtocol;
    @JsonProperty(KAFKA_SASL_MECHANISM)
    private String kafkaSaslMechanism;
    @JsonProperty(KAFKA_BOOTSTRAP_SERVERS)
    private String kafkaBootstrapServers;
}