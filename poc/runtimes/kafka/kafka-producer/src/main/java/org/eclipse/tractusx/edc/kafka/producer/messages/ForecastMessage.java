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

package org.eclipse.tractusx.edc.kafka.producer.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ForecastMessage implements Message {

    private Request request;
    private MessageHeader  header;

    @Data
    public static class Request {

        private TimeQuantity precisionOfForecast;
        private TimeQuantity offset;
        private String       orderId;
        private String       customerId;
        private TimeQuantity deviationOfSchedule;
        private boolean      productionForecastForAll;
        private String       version;
        private TimeQuantity notificationInterval;
        private String       communicationMode;
    }

    @Data
    public static class TimeQuantity {

        private int value;

        @JsonProperty("timeUnit") // keep the original JSON field-name
        private String unit;
    }
}
