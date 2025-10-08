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
import org.springframework.stereotype.Component;

/**
 * Default implementation of MessageHandler that logs received Kafka messages.
 * This class can be autowired using Spring dependency injection.
 */
@Slf4j
@Component
public class DefaultMessageHandler implements MessageHandler {

    @Override
    public void handleMessage(ConsumerRecord<String, String> consumerRecord) {
        log.info("Received record(topic={} key={}, value={}) meta(partition={}, offset={})",
                consumerRecord.topic(), consumerRecord.key(), consumerRecord.value(), 
                consumerRecord.partition(), consumerRecord.offset());
    }
}