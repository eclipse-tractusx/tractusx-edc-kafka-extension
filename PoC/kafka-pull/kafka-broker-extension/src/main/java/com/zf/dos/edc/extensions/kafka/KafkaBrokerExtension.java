package com.zf.dos.edc.extensions.kafka;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

/**
 * Kafka Broker flow extension
 */
@Extension(value = KafkaBrokerExtension.NAME)
public class KafkaBrokerExtension implements ServiceExtension {

    public static final String NAME =  "Kafka stream extension";

    @Inject
    private DataFlowManager dataFlowManager;

    @Inject
    private Vault vault;

    @Override
    public void initialize(ServiceExtensionContext context) {
        dataFlowManager.register(10, new KafkaBrokerDataFlowController(vault, new KafkaAdminServiceProviderImpl()));
    }
}