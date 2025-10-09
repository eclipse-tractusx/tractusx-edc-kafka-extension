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

package org.eclipse.tractusx.edc.kafka.producer.config;

public class ProducerProperties {

    public String getBootstrapServers() {
        return getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    }

    public long getMessageSendIntervalMs() {
        return Long.parseLong(getOrDefault("MESSAGE_SEND_INTERVAL_MS", "2000"));
    }

    public String getSslTruststoreLocation() {
        return getOrDefault("SSL_TRUSTSTORE_LOCATION", "/opt/java/openjdk/lib/security/cacerts");
    }

    public String getSslEndpointIdentificationAlgorithm() {
        return getOrDefault("SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "");
    }

    public String getSslTruststoreType() {
        return getOrDefault("SSL_TRUSTSTORE_TYPE", "JKS");
    }

    public String getSecurityProtocol() {
        return getOrDefault("SECURITY_PROTOCOL", "SASL_PLAINTEXT");
    }

    public String getProductionForecastTopic() {
        return getOrDefault("KAFKA_PRODUCTION_FORECAST_TOPIC", "kafka-production-forecast-topic");
    }

    public String getProductionTrackingTopic() {
        return getOrDefault("KAFKA_PRODUCTION_TRACKING_TOPIC", "kafka-production-tracking-topic");
    }

    public String getStreamTopic() {
        return getOrDefault("KAFKA_STREAM_TOPIC", "kafka-stream-topic");
    }

    // Keycloak Configuration Properties
    public String getTokenUrl() {
        return getOrDefault("KEYCLOAK_TOKEN_URL", "http://localhost:8080/realms/kafka/protocol/openid-connect/token");
    }

    public String getRevokeUrl() {
        return getOrDefault("KEYCLOAK_REVOKE_URL", "http://localhost:8080/realms/kafka/protocol/openid-connect/revoke");
    }

    public String getClientId() {
        return getOrDefault("KEYCLOAK_CLIENT_ID", "default");
    }

    public String getClientSecret() {
        return getOrDefault("KEYCLOAK_CLIENT_SECRET", "mysecret");
    }

    public String getVaultClientSecretKey() {
        return getOrDefault("VAULT_CLIENT_SECRET_KEY", "secretKey");
    }

    // EDC Configuration Properties
    public String getAssetId() {
        return getOrDefault("ASSET_ID", "kafka-stream-asset");
    }

    public String getForecastAssetId() {
        return getOrDefault("FORECAST_ASSET_ID", "kafka-forecast-asset");
    }

    public String getTrackingAssetId() {
        return getOrDefault("TRACKING_ASSET_ID", "kafka-tracking-asset");
    }

    public String getApiAuthKey() {
        return getOrDefault("EDC_API_AUTH_KEY", "password");
    }

    public String getManagementUrl() {
        return getOrDefault("EDC_MANAGEMENT_URL", "http://localhost:8081/management");
    }

    private static String getOrDefault(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue);
    }
}