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
package org.eclipse.tractusx.edc.extensions.kafka.acl;

import org.eclipse.edc.spi.result.Result;

/**
 * Interface for services that manage Kafka ACLs (Access Control Lists).
 * Provides functionality to create and revoke ACLs for OAuth subjects to control
 * access to Kafka topics and resources.
 */
public interface KafkaAclService {

    /**
     * Creates ACLs for the specified OAuth subject to allow access to the given topic.
     * This typically includes READ permissions for consumers and appropriate group access.
     *
     * @param oauthSubject The OAuth subject (principal) for which to create ACLs
     * @param topicName The Kafka topic name to grant access to
     * @param transferProcessId The transfer process ID for tracking purposes
     * @return Result indicating success or failure with error details
     */
    Result<Void> createAclsForSubject(String oauthSubject, String topicName, String transferProcessId);

    /**
     * Revokes all ACLs associated with the specified transfer process.
     * This removes access permissions that were previously granted for the OAuth subject.
     *
     * @param transferProcessId The transfer process ID whose ACLs should be revoked
     * @return Result indicating success or failure with error details
     */
    Result<Void> revokeAclsForTransferProcess(String transferProcessId);

    /**
     * Revokes ACLs for the specified OAuth subject on the given topic.
     * This is an alternative approach when subject and topic are known directly.
     *
     * @param oauthSubject The OAuth subject whose ACLs should be revoked
     * @param topicName The Kafka topic name to revoke access from
     * @return Result indicating success or failure with error details
     */
    Result<Void> revokeAclsForSubject(String oauthSubject, String topicName);
}