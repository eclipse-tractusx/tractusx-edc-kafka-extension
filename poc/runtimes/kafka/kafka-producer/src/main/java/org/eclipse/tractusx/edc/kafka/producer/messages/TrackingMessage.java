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

import java.util.List;

@Data
public class TrackingMessage implements Message {

    private Request request;
    private MessageHeader header;

    @Data
    public static class Request {

        private String identifierNumber;
        private String catenaXId;
        private List<StepIdentifier> stepIdentifierList;
        private String customerId;
        private String billOfProcessId;
        private String identifierType;
        private String version;
        private String processReferenceType;
    }

    @Data
    public static class StepIdentifier {

        private String processStepId;
        private List<ProcessParameter> processParameterList;
        private String capabilityId;
        private String billOfMaterialElementId;
        private String partInstanceLevel;
        private String partInstanceId;
        private String billOfMaterialId;
    }

    @Data
    public static class ProcessParameter {

        @JsonProperty("processParameterSemanticId")
        private String semanticId;

        @JsonProperty("processParameterName")
        private String name;
    }
}
