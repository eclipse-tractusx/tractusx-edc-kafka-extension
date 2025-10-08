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
package org.eclipse.tractusx.edc.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api")
@Slf4j
public class SubscriptionController {

    private final DataTransferClient dataTransferClient;
    private final KafkaTopicConsumptionService consumptionService;

    @Autowired
    public SubscriptionController(DataTransferClient dataTransferClient, KafkaTopicConsumptionService consumptionService) {
        this.dataTransferClient = dataTransferClient;
        this.consumptionService = consumptionService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @RequestParam String assetId) throws InterruptedException {

        try {
            log.info("Received subscription request for assetId: {}", sanitizeForLog(assetId));
            EDRData edrData = dataTransferClient.executeDataTransferWorkflow(assetId);
            ConsumerRecords<String, String> records = consumptionService.consumeOnce(edrData);
            log.info("Received {} records for assetId: {}", records.count(), sanitizeForLog(assetId));

            List<KafkaRecordDto> recordDtos = StreamSupport.stream(records.spliterator(), false)
                .map(this::mapToDto)
                .collect(Collectors.toList());

            SubscriptionResponse response = SubscriptionResponse.builder()
                .assetId(assetId)
                .recordCount(recordDtos.size())
                .records(recordDtos)
                .timestamp(Instant.now())
                .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException | IOException e) {
            log.error("Error processing subscription request for assetId: {}", sanitizeForLog(assetId), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SubscriptionResponse.error("Internal server error"));
        }
    }

    private KafkaRecordDto mapToDto(ConsumerRecord<String, String> record) {
        return KafkaRecordDto.builder()
            .topic(record.topic())
            .partition(record.partition())
            .offset(record.offset())
            .key(record.key())
            .value(record.value())
            .timestamp(Instant.ofEpochMilli(record.timestamp()))
            .build();
    }

    private String sanitizeForLog(String input) {
        if (input == null) {
            return null;
        }
        String sanitized = input.replaceAll("[\\x00-\\x08\\x0A-\\x1F\\x7F]", "");

        // Replace newline and carriage return with visible markers
        sanitized = sanitized.replace("\n", "\\n").replace("\r", "\\r");

        // Trim excessive length to avoid log flooding
        int maxLength = 200;
        if (sanitized.length() > maxLength) {
            sanitized = sanitized.substring(0, maxLength) + "...";
        }

        return sanitized;
    }
}