package com.zf.dos.edc.extensions.kafka;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.GROUP_PREFIX;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.KAFKA_TYPE;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.MECHANISM;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.POLL_DURATION;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.PROTOCOL;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.SECRET_KEY;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_CODE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.CONTRACT_ID;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ENDPOINT;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ID;

class KafkaBrokerDataFlowController implements DataFlowController {
    private final String TRANSFER_TYPE = "Kafka-PULL";
    static final String START_FAILED = "Failed to start data flow: ";
    static String SUSPEND_FAILED = "Failed to suspend data flow: ";
    static String TERMINATE_FAILED = "Failed to terminate data flow: ";
    static String SECRET_NOT_DEFINED = "secret key %s was not defined";
    private final Vault vault;
    private final KafkaAdminServiceProvider kafkaAdminServiceProvider;

    public KafkaBrokerDataFlowController(Vault vault, KafkaAdminServiceProvider kafkaAdminServiceProvider) {
        this.vault = vault;
        this.kafkaAdminServiceProvider = kafkaAdminServiceProvider;
    }

    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return KAFKA_TYPE.equals(transferProcess.getContentDataAddress().getType())
                && TRANSFER_TYPE.equals(transferProcess.getTransferType());
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(TransferProcess transferProcess, Policy policy) {
        try {
            var contentDataAddress = transferProcess.getContentDataAddress();
            var transferProcessId = transferProcess.getId();
            var topic = contentDataAddress.getStringProperty(TOPIC);
            var endpoint = contentDataAddress.getStringProperty(BOOTSTRAP_SERVERS);
            var contractId = transferProcess.getContractId();
            var mechanism = contentDataAddress.getStringProperty(MECHANISM);
            var protocol = contentDataAddress.getStringProperty(PROTOCOL);
            var duration = Optional.ofNullable(contentDataAddress.getStringProperty(POLL_DURATION)).map(Duration::parse).orElse(null);
            var groupPrefix = policy.getAssignee();
            var secret = getSecret(contentDataAddress.getStringProperty(SECRET_KEY));
            Map.Entry<String, String> token;

            try (var kafkaAdminService = kafkaAdminServiceProvider.provide(contentDataAddress, secret)) {
                var password = kafkaAdminService.createCredentialsAndGrantAccess(transferProcessId, topic, groupPrefix);
                vault.storeSecret(transferProcessId, password);
                token = kafkaAdminService.createToken(duration, transferProcessId);
            }

            var kafkaDataAddress = DataAddress.Builder.newInstance()
                    .type(EDR_SIMPLE_TYPE)
                    .property(ID, transferProcessId)
                    .property(ENDPOINT, endpoint)
                    .property(AUTH_KEY, token.getKey())
                    .property(AUTH_CODE, token.getValue())
                    .property(CONTRACT_ID, contractId)
                    .property(TOPIC, topic)
                    .property(PROTOCOL, protocol)
                    .property(MECHANISM, mechanism)
                    .property(GROUP_PREFIX, groupPrefix)
                    .build();

            return StatusResult.success(DataFlowResponse.Builder.newInstance().dataAddress(kafkaDataAddress).build());
        } catch (ExecutionException | InterruptedException | EdcException | TimeoutException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, START_FAILED + e.getMessage());
        }
    }

    @Override
    public StatusResult<Void> suspend(TransferProcess transferProcess) {
        return deleteCredentialsAndRevokeAccess(transferProcess, SUSPEND_FAILED);
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        return deleteCredentialsAndRevokeAccess(transferProcess, TERMINATE_FAILED);
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        return Set.of(TRANSFER_TYPE);
    }

    private String getSecret(String secretKey) {
        return Optional.ofNullable(vault.resolveSecret(secretKey)).orElseThrow(() -> new EdcException(SECRET_NOT_DEFINED.formatted(secretKey)));
    }

    private StatusResult<Void> deleteCredentialsAndRevokeAccess(TransferProcess transferProcess, String error) {
        try {
            var contentDataAddress = transferProcess.getContentDataAddress();
            var topic = contentDataAddress.getStringProperty(TOPIC);
            var username = transferProcess.getId();
            var secret = getSecret(contentDataAddress.getStringProperty(SECRET_KEY));

            try (var kafkaAdminService = kafkaAdminServiceProvider.provide(contentDataAddress, secret)) {
                kafkaAdminService.deleteCredentialsAndRevokeAccess(username, topic);
                vault.deleteSecret(username);
                return StatusResult.success();
            }
        } catch (ExecutionException | InterruptedException | EdcException | TimeoutException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, error + e.getMessage());
        }
    }
}