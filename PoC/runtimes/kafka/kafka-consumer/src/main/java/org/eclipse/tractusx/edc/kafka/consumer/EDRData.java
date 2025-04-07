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

package org.eclipse.tractusx.edc.kafka.consumer;

public class EDRData {
    private String endpoint;
    private String authCode;
    private String contractId;
    private String topic;
    private String id;
    private String authKey;
    private String groupPrefix;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getGroupPrefix() {
        return groupPrefix;
    }

    public void setGroupPrefix(String groupPrefix) {
        this.groupPrefix = groupPrefix;
    }

    @Override
    public String toString() {
        return "EDRData{" +
                "endpoint='" + endpoint + '\'' +
                ", authCode='" + authCode + '\'' +
                ", contractId='" + contractId + '\'' +
                ", topic='" + topic + '\'' +
                ", id='" + id + '\'' +
                ", authKey='" + authKey + '\'' +
                ", groupPrefix='" + groupPrefix + '\'' +
                '}';
    }
}
