package org.eclipse.tractusx.edc.extensions.kafka;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;

@ExtendWith(DependencyInjectionExtension.class)
class KafkaBrokerExtensionTest {

    private final DataFlowManager dataFlowManager = mock();

    private final Vault vault = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(DataFlowManager.class, dataFlowManager);
        context.registerService(Vault.class, vault);
    }

    @Test
    void initialize_RegistersKafkaDataFlowController(KafkaBrokerExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(dataFlowManager, times(1))
                .register(anyInt(), any(KafkaBrokerDataFlowController.class));
    }
}
