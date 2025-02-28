package org.eclipse.tractusx.edc.extensions.kafka;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;

import static org.eclipse.tractusx.edc.extensions.kafka.KafkaBrokerDataFlowController.SECRET_NOT_DEFINED;
import static org.eclipse.tractusx.edc.extensions.kafka.KafkaBrokerDataFlowController.START_FAILED;
import static org.eclipse.tractusx.edc.extensions.kafka.KafkaBrokerDataFlowController.SUSPEND_FAILED;
import static org.eclipse.tractusx.edc.extensions.kafka.KafkaBrokerDataFlowController.TERMINATE_FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.GROUP_PREFIX;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.KAFKA_TYPE;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.MECHANISM;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.POLL_DURATION;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.PROTOCOL;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.SECRET_KEY;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_CODE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.AUTH_KEY;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.CONTRACT_ID;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.EDR_SIMPLE_TYPE;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ENDPOINT;
import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.ID;
import static org.mockito.Mockito.*;

class KafkaBrokerDataFlowControllerTest {
    private KafkaBrokerDataFlowController controller;

    private DataAddress contentDataAddress;

    private TransferProcess transferProcess;

    private Policy policy;

    private Vault vault;

    @BeforeEach
    void setUp() throws Exception {
        contentDataAddress = DataAddress.Builder.newInstance()
                .type(KAFKA_TYPE)
                .property(TOPIC, "test-topic")
                .property(BOOTSTRAP_SERVERS, "localhost:9092")
                .property(SECRET_KEY, "secretKey")
                .property(PROTOCOL, "protocol")
                .property(MECHANISM, "mechanism")
                .property(POLL_DURATION, "PT5M")
                .build();

        transferProcess = TransferProcess.Builder.newInstance()
                .contentDataAddress(contentDataAddress)
                .transferType("Kafka-PULL")
                .contractId("contract")
                .correlationId("correlation")
                .id("transferProcessId").build();
        policy = Policy.Builder.newInstance().assignee("test-group").build();
        vault = mock();
        KafkaAdminServiceProvider kafkaAdminServiceProvider = mock();
        KafkaAdminService kafkaAdminService = mock();
        when(kafkaAdminService.createCredentialsAndGrantAccess(anyString(), anyString(), anyString())).thenReturn("password");
        when(kafkaAdminService.createToken(any(), anyString())).thenReturn(new AbstractMap.SimpleEntry<>("tokenId", "password"));
        when(kafkaAdminServiceProvider.provide(any(), any())).thenReturn(kafkaAdminService);
        controller = new KafkaBrokerDataFlowController(vault, kafkaAdminServiceProvider);
    }

    @Test
    void canHandle_ShouldReturnTrue_WhenTypeAndTransferTypeMatch() {
        boolean result = controller.canHandle(transferProcess);
        assertThat(result).isTrue();
    }

    @Test
    void canHandle_ShouldReturnFalse_WhenTypeDoesNotMatch() {
        contentDataAddress.setType("Non-Kafka");
        transferProcess.setContentDataAddress(contentDataAddress);
        boolean result = controller.canHandle(transferProcess);
        assertThat(result).isFalse();
    }

    @Test
    void canHandle_ShouldReturnFalse_WhenTransferTypeDoesNotMatch() {
        transferProcess = TransferProcess.Builder.newInstance().contentDataAddress(contentDataAddress).transferType("Not-Kafka-PULL").build();
        boolean result = controller.canHandle(transferProcess);
        assertThat(result).isFalse();
    }

    @Test
    void start_ShouldReturnSuccess_WhenValidInput() {
        when(vault.resolveSecret(any())).thenReturn("secret-key");
        StatusResult<DataFlowResponse> result = controller.start(transferProcess, policy);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNotNull();

        AbstractResultAssert.assertThat(result).isSucceeded().extracting(DataFlowResponse::getDataAddress).satisfies(kafkaDataAddress -> {
            assertThat(kafkaDataAddress.getType()).isEqualTo(EDR_SIMPLE_TYPE);
            assertThat(kafkaDataAddress.getStringProperty(ID)).isEqualTo("transferProcessId");
            assertThat(kafkaDataAddress.getStringProperty(ENDPOINT)).isEqualTo("localhost:9092");
            assertThat(kafkaDataAddress.getStringProperty(AUTH_KEY)).isEqualTo("tokenId");
            assertThat(kafkaDataAddress.getStringProperty(AUTH_CODE)).isEqualTo("password");
            assertThat(kafkaDataAddress.getStringProperty(CONTRACT_ID)).isEqualTo("contract");
            assertThat(kafkaDataAddress.getStringProperty(TOPIC)).isEqualTo("test-topic");
            assertThat(kafkaDataAddress.getStringProperty(GROUP_PREFIX)).isEqualTo("test-group");
        });
    }

    @Test
    void start_ShouldReturnFailure_WhenMissedSecret() {
        StatusResult<?> result = controller.start(transferProcess, policy);
        assertThat(result.fatalError()).isTrue();
        assertThat(START_FAILED + SECRET_NOT_DEFINED.formatted("secretKey")).isEqualTo(result.getFailureDetail());
    }

    @Test
    void suspend_ShouldReturnSuccess_WhenOperationSucceeds() {
        when(vault.resolveSecret(any())).thenReturn("secret-key");
        StatusResult<?> result = controller.suspend(transferProcess);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void suspend_ShouldReturnFailure_WhenMissedSecret() {
        StatusResult<?> result = controller.suspend(transferProcess);
        assertThat(result.fatalError()).isTrue();
        assertThat(SUSPEND_FAILED + SECRET_NOT_DEFINED.formatted("secretKey")).isEqualTo(result.getFailureDetail());
    }

    @Test
    void terminate_ShouldReturnSuccess_WhenOperationSucceeds() {
        when(vault.resolveSecret(any())).thenReturn("secret-key");
        StatusResult<?> result = controller.terminate(transferProcess);
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void terminate_ShouldReturnFailure_WhenMissedSecret() {
        StatusResult<?> result = controller.terminate(transferProcess);
        assertThat(result.fatalError()).isTrue();
        assertThat(TERMINATE_FAILED + SECRET_NOT_DEFINED.formatted("secretKey")).isEqualTo(result.getFailureDetail());
    }
}

