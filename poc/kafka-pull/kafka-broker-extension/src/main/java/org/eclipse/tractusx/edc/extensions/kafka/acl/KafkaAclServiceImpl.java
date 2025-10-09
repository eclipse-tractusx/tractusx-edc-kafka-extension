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

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.DeleteAclsResult;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of {@link KafkaAclService} that uses Kafka AdminClient to manage ACLs.
 * This service creates and revokes ACLs for OAuth subjects to control access to Kafka topics.
 */
public class KafkaAclServiceImpl implements KafkaAclService {

    private final Properties kafkaProperties;
    private final Monitor monitor;
    private final AdminClientFactory adminClientFactory;
    private final Map<String, AclTrackingInfo> transferProcessAcls = new ConcurrentHashMap<>();

    public KafkaAclServiceImpl(Properties kafkaProperties, Monitor monitor) {
        this(kafkaProperties, monitor, new DefaultAdminClientFactory());
    }

    public KafkaAclServiceImpl(Properties kafkaProperties, Monitor monitor, AdminClientFactory adminClientFactory) {
        this.kafkaProperties = kafkaProperties;
        this.monitor = monitor;
        this.adminClientFactory = adminClientFactory;
    }

    private static @NotNull String createUserPrincipal(String oauthSubject) {
        return "User:" + oauthSubject;
    }

    @Override
    public Result<Void> createAclsForSubject(String oauthSubject, String topicName, String transferProcessId) {
        monitor.debug("Creating ACLs for OAuth subject: %s, topic: %s, transferProcessId: %s"
                .formatted(oauthSubject, topicName, transferProcessId));

        try (Admin adminClient = adminClientFactory.createAdmin(kafkaProperties)) {
            Collection<AclBinding> aclBindings = buildAclBindings(oauthSubject, topicName);
            CreateAclsResult result = adminClient.createAcls(aclBindings);
            result.all().get(); // Wait for completion

            // Track the ACLs for later revocation
            transferProcessAcls.put(transferProcessId, new AclTrackingInfo(oauthSubject, topicName, aclBindings));

            monitor.debug("Successfully created ACLs for OAuth subject: %s, topic: %s, transferProcessId: %s"
                    .formatted(oauthSubject, topicName, transferProcessId));
            return Result.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "Interrupted while creating ACLs for subject: %s".formatted(oauthSubject);
            monitor.severe(message, e);
            return createFailureResult(message, e);
        } catch (ExecutionException e) {
            String message = "Failed to create ACLs for OAuth subject: %s".formatted(oauthSubject);
            monitor.severe(message, e);
            return createFailureResult(message, e);
        }
    }

    @Override
    public Result<Void> revokeAclsForTransferProcess(String transferProcessId) {
        monitor.debug("Revoking ACLs for transferProcessId: %s".formatted(transferProcessId));

        AclTrackingInfo aclInfo = transferProcessAcls.remove(transferProcessId);
        if (aclInfo == null) {
            monitor.debug("No ACLs found for transferProcessId: %s".formatted(transferProcessId));
            return Result.success(); // Nothing to revoke
        }

        try (Admin adminClient = adminClientFactory.createAdmin(kafkaProperties)) {
            Collection<AclBindingFilter> aclFilters = convertToAclBindingFilters(aclInfo.aclBindings());
            DeleteAclsResult result = adminClient.deleteAcls(aclFilters);
            result.all().get();

            monitor.debug("Successfully revoked ACLs for transferProcessId: %s".formatted(transferProcessId));
            return Result.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "Interrupted while revoking ACLs for transferProcessId: %s".formatted(transferProcessId);
            monitor.severe(message, e);
            return createFailureResult(message, e);
        } catch (ExecutionException e) {
            String message = "Failed to revoke ACLs for transferProcessId: %s".formatted(transferProcessId);
            monitor.severe(message, e);
            return createFailureResult(message, e);
        }
    }

    @Override
    public Result<Void> revokeAclsForSubject(String oauthSubject, String topicName) {
        monitor.debug("Revoking ACLs for OAuth subject: %s, topic: %s".formatted(oauthSubject, topicName));

        try (Admin adminClient = adminClientFactory.createAdmin(kafkaProperties)) {
            Collection<AclBinding> aclBindings = buildAclBindings(oauthSubject, topicName);
            Collection<AclBindingFilter> aclFilters = convertToAclBindingFilters(aclBindings);

            DeleteAclsResult result = adminClient.deleteAcls(aclFilters);
            result.all().get(); // Wait for completion

            monitor.debug("Successfully revoked ACLs for OAuth subject: %s, topic: %s".formatted(oauthSubject, topicName));
            return Result.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String message = "Interrupted while revoking ACLs for subject: %s".formatted(oauthSubject);
            monitor.severe(message, e);
            return createFailureResult(message, e);
        } catch (ExecutionException e) {
            String message = "Failed to revoke ACLs for OAuth subject: %s".formatted(oauthSubject);
            monitor.severe(message, e);
            return createFailureResult(message, e);
        }
    }

    private static @NotNull Result<Void> createFailureResult(String message, Exception e) {
        return Result.failure("%s: %s".formatted(message, e.getMessage()));
    }

    /**
     * Builds the ACL bindings required for a consumer to read from a topic.
     * Creates ACLs for:
     * 1. READ operation on the topic
     * 2. DESCRIBE operation on the topic
     * 3. READ operation on consumer group (with prefix matching)
     */
    private Collection<AclBinding> buildAclBindings(String oauthSubject, String topicName) {
        Collection<AclBinding> aclBindings = new ArrayList<>();

        // Topic READ permission
        ResourcePattern topicResource = new ResourcePattern(ResourceType.TOPIC, topicName, PatternType.LITERAL);
        AccessControlEntry readAce = new AccessControlEntry(createUserPrincipal(oauthSubject), "*", AclOperation.READ, AclPermissionType.ALLOW);
        aclBindings.add(new AclBinding(topicResource, readAce));

        // Topic DESCRIBE permission
        AccessControlEntry describeAce = new AccessControlEntry(createUserPrincipal(oauthSubject), "*", AclOperation.DESCRIBE, AclPermissionType.ALLOW);
        aclBindings.add(new AclBinding(topicResource, describeAce));

        // Consumer Group READ permission (using prefix pattern for flexibility)
        ResourcePattern groupResource = new ResourcePattern(ResourceType.GROUP, oauthSubject, PatternType.PREFIXED);
        AccessControlEntry groupReadAce = new AccessControlEntry(createUserPrincipal(oauthSubject), "*", AclOperation.READ, AclPermissionType.ALLOW);
        aclBindings.add(new AclBinding(groupResource, groupReadAce));

        return aclBindings;
    }

    /**
     * Converts AclBinding objects to AclBindingFilter objects for deletion.
     * AclBindingFilter is used by the AdminClient.deleteAcls() method.
     */
    private Collection<AclBindingFilter> convertToAclBindingFilters(Collection<AclBinding> aclBindings) {
        Collection<AclBindingFilter> aclFilters = new ArrayList<>();

        for (AclBinding aclBinding : aclBindings) {
            ResourcePatternFilter resourceFilter = new ResourcePatternFilter(
                    aclBinding.pattern().resourceType(),
                    aclBinding.pattern().name(),
                    aclBinding.pattern().patternType()
            );

            AccessControlEntryFilter entryFilter = new AccessControlEntryFilter(
                    aclBinding.entry().principal(),
                    aclBinding.entry().host(),
                    aclBinding.entry().operation(),
                    aclBinding.entry().permissionType()
            );

            AclBindingFilter filter = new AclBindingFilter(resourceFilter, entryFilter);
            aclFilters.add(filter);
        }

        return aclFilters;
    }

    /**
     * Information to track ACLs for a transfer process
     */
    private record AclTrackingInfo(String oauthSubject, String topicName, Collection<AclBinding> aclBindings) {
    }
}