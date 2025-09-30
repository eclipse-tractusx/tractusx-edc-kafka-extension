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

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class KafkaConsumerApp {
    public static final String FORECAST_ASSET_ID = System.getenv().getOrDefault("FORECAST_ASSET_ID", "kafka-forecast-asset");
    public static final String TRACKING_ASSET_ID = System.getenv().getOrDefault("TRACKING_ASSET_ID", "kafka-tracking-asset");
    static final String ASSET_ID = System.getenv().getOrDefault("ASSET_ID", "kafka-stream-asset");
    static final String PROVIDER_ID = System.getenv().getOrDefault("PROVIDER_ID", "BPNL00000003AZQP");
    static final String PROVIDER_PROTOCOL_URL = System.getenv().getOrDefault("PROVIDER_PROTOCOL_URL", "http://control-plane-alice:8084/api/v1/dsp");
    static final String EDC_MANAGEMENT_URL = System.getenv().getOrDefault("EDC_MANAGEMENT_URL", "http://localhost:8081/management");
    static final String EDC_API_KEY = System.getenv().getOrDefault("EDC_API_KEY", "password");

    private final DataTransferClient dataTransferClient;
    private final KafkaTopicConsumptionService consumptionService;

    public KafkaConsumerApp(DataTransferClient dataTransferClient, KafkaTopicConsumptionService consumptionService) {
        this.dataTransferClient = dataTransferClient;
        this.consumptionService = consumptionService;
    }

    public KafkaConsumerApp() {
        this(new DataTransferClient(), new KafkaTopicConsumptionService(new KafkaConsumerFactory(), new DefaultMessageHandler()));
    }

    public static void main(final String[] args) {
        new KafkaConsumerApp().run();
    }

    public void run() {
        try {
            final List<EDRData> edrDataList = List.of(
                    dataTransferClient.executeDataTransferWorkflow(FORECAST_ASSET_ID),
                    dataTransferClient.executeDataTransferWorkflow(TRACKING_ASSET_ID));

            log.info("Starting Kafka topic consumption with {} EDR data entries", edrDataList.size());
            consumptionService.startConsumption(edrDataList);
        } catch (final Exception e) {
            log.error("Fatal error in KafkaConsumerApp", e);
            throw new KafkaConsumerException("Application failed to start", e);
        }
    }
}