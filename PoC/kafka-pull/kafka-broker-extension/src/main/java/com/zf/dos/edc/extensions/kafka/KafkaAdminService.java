package com.zf.dos.edc.extensions.kafka;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;
import org.apache.kafka.common.security.auth.KafkaPrincipal;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.apache.kafka.common.acl.AclOperation.READ;
import static org.apache.kafka.common.acl.AclOperation.WRITE;
import static org.apache.kafka.common.acl.AclPermissionType.ALLOW;
import static org.apache.kafka.common.acl.AclPermissionType.ANY;
import static org.apache.kafka.common.resource.PatternType.LITERAL;
import static org.apache.kafka.common.resource.PatternType.PREFIXED;
import static org.apache.kafka.common.resource.ResourceType.GROUP;
import static org.apache.kafka.common.resource.ResourceType.TOPIC;
import static org.apache.kafka.common.security.auth.KafkaPrincipal.USER_TYPE;

/**
 * Handles creating kafka sasl credentials and access token, granting and revoking access for subscribing.
 */
public class KafkaAdminService implements AutoCloseable  {
    private final Admin adminClient;
    private final ScramMechanism scramMechanism;
    static final int TIMEOUT = 1;
    static final TimeUnit TIMEOUT_UNIT = TimeUnit.MINUTES;

    KafkaAdminService(Admin adminClient, ScramMechanism scramMechanism) {
        this.adminClient = adminClient;
        this.scramMechanism = scramMechanism;
    }

    /**
     * Create a token with duration and returns the tokenId and tokenKey.
     *
     * @param duration The duration of token.
     * @param username Owner of the toke.
     * @return A pair of create tokenId and tokenKey.
     * @throws ExecutionException   Rethrow from KafkaFuture.
     * @throws InterruptedException Rethrow from KafkaFuture.
     * @throws TimeoutException     Rethrow from KafkaFuture.
     */
    public Map.Entry<String, String> createToken(Duration duration, String username) throws ExecutionException, InterruptedException, TimeoutException {
        var tokenOption = new CreateDelegationTokenOptions().owner(new KafkaPrincipal(USER_TYPE, username));
        Optional.ofNullable(duration).ifPresent(value -> tokenOption.maxlifeTimeMs(value.toMillis()));

        var tokenResult = adminClient.createDelegationToken(tokenOption);
        var delegationToken = tokenResult.delegationToken().get(TIMEOUT, TIMEOUT_UNIT);

        var tokenId = delegationToken.tokenInfo().tokenId();
        var tokenKey = delegationToken.hmacAsBase64String();
        return Map.entry(tokenId, tokenKey);
    }

    /**
     * Create sasl credentials and grant access for the user for defined topic and group prefix and returns the password.
     *
     * @param username    The user identifier.
     * @param topic       The kafka topic for subscribe.
     * @param groupPrefix The groupPrefix that allowed to subscribe.
     * @return A password of created sasl credentials.
     * @throws ExecutionException   Rethrow from KafkaFuture.
     * @throws InterruptedException Rethrow from KafkaFuture.
     * @throws TimeoutException     Rethrow from KafkaFuture.
     */
    public String createCredentialsAndGrantAccess(String username, String topic, String groupPrefix) throws ExecutionException, InterruptedException, TimeoutException {
        var password = createConsumerCredentials(username);
        grantReadAccess(username, topic, groupPrefix);

        return password;
    }

    /**
     * Delete sasl credentials and revoke access for the user for defined topic.
     *
     * @param username The user identifier.
     * @param topic    The kafka topic.
     * @throws ExecutionException   Rethrow from KafkaFuture.
     * @throws InterruptedException Rethrow from KafkaFuture.
     * @throws TimeoutException     Rethrow from KafkaFuture.
     */
    public void deleteCredentialsAndRevokeAccess(String username, String topic) throws ExecutionException, InterruptedException, TimeoutException {
        if (isConsumerCredential(username)) {
            revokeReadAccess(username, topic);
            deleteConsumerCredentials(username);
        }
    }

    @Override
    public void close() {
        adminClient.close();
    }

    private String createConsumerCredentials(String username) throws ExecutionException, InterruptedException, TimeoutException {
        var password = generateSecurePassword();
        addScramCredential(username, password);

        return password;
    }

    private void deleteConsumerCredentials(String username) throws ExecutionException, InterruptedException, TimeoutException {
        var deletion = new UserScramCredentialDeletion(username, scramMechanism);
        var result = adminClient.alterUserScramCredentials(Collections.singletonList(deletion));

        result.all().get(TIMEOUT, TIMEOUT_UNIT);
    }

    private void grantReadAccess(String username, String topic, String groupPrefix) throws ExecutionException, InterruptedException, TimeoutException {
        var topicAclBinding = createTopicAclBinding(username, topic, READ);
        var groupAclBinding = createGroupAclBinding(username, groupPrefix);
        var readOffsetsAcl = createInternalTopicAclBinding(username, READ);
        var writeOffsetsAcl = createInternalTopicAclBinding(username, WRITE);

        var result = adminClient.createAcls(List.of(topicAclBinding, groupAclBinding, readOffsetsAcl, writeOffsetsAcl));

        result.all().get(TIMEOUT, TIMEOUT_UNIT);
    }

    private boolean isConsumerCredential(String username) throws InterruptedException, ExecutionException, TimeoutException {
        var result = adminClient.describeUserScramCredentials(List.of(username));
        var userCredentials = result.all().get(TIMEOUT, TIMEOUT_UNIT);
        return !userCredentials.get(username).credentialInfos().isEmpty();
    }

    private void revokeReadAccess(String username, String topic) throws ExecutionException, InterruptedException, TimeoutException {
        var topicFilter = createTopicAclBindingFilter(username, topic);
        var result = adminClient.deleteAcls(List.of(topicFilter));

        result.all().get(TIMEOUT, TIMEOUT_UNIT);
    }

    private void addScramCredential(String username, String password) throws ExecutionException, InterruptedException, TimeoutException {
        var scramCredential = new ScramCredentialInfo(scramMechanism, 4096);
        var upsertion = new UserScramCredentialUpsertion(username, scramCredential, password);
        var result = adminClient.alterUserScramCredentials(Collections.singletonList(upsertion));

        result.all().get(TIMEOUT, TIMEOUT_UNIT);
    }

    private AclBinding createTopicAclBinding(String username, String topic, AclOperation operation) {
        var resourcePattern = new ResourcePattern(TOPIC, topic, LITERAL);
        var entry = new AccessControlEntry("User:" + username, "*", operation, ALLOW);
        return new AclBinding(resourcePattern, entry);
    }

    private AclBindingFilter createTopicAclBindingFilter(String username, String topic) {
        var resourcePatternFilter = new ResourcePatternFilter(TOPIC, topic, LITERAL);
        var entryFilter = new AccessControlEntryFilter("User:" + username, "*", READ, ANY);
        return new AclBindingFilter(resourcePatternFilter, entryFilter);
    }

    private AclBinding createGroupAclBinding(String username, String groupPrefix) {
        var resourcePattern = new ResourcePattern(GROUP, groupPrefix, PREFIXED);
        var entry = new AccessControlEntry("User:" + username, "*", READ, ALLOW);
        return new AclBinding(resourcePattern, entry);
    }

    private AclBinding createInternalTopicAclBinding(String username, AclOperation operation) {
        var internalTopic = "__consumer_offsets";
        return createTopicAclBinding(username, internalTopic, operation);
    }

    private String generateSecurePassword() {
        return UUID.randomUUID().toString();
    }
}
