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

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class MessageHeader {

    private String        senderBpn;
    private String        relatedMessageId;
    private OffsetDateTime expectedResponseBy;
    private String        context;
    private String        messageId;
    private String        receiverBpn;
    private OffsetDateTime sentDateTime;
    private String        version;
}
