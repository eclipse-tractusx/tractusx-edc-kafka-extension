package org.eclipse.tractusx.edc.extensions.kafka;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ScramMechanism;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;

public class KafkaAdminServiceProviderImpl implements KafkaAdminServiceProvider{

    public KafkaAdminServiceProviderImpl() {
    }

    @Override
    public KafkaAdminService provide(DataAddress contentDataAddress, String secret) {
        var mechanism = contentDataAddress.getStringProperty(MECHANISM);
        var adminProps = new Properties();
        adminProps.put(BOOTSTRAP_SERVERS_CONFIG, contentDataAddress.getStringProperty(BOOTSTRAP_SERVERS));
        adminProps.put(SECURITY_PROTOCOL_CONFIG, contentDataAddress.getStringProperty(PROTOCOL));
        adminProps.put(SASL_MECHANISM, mechanism);
        adminProps.put(SASL_JAAS_CONFIG, secret);

        return new KafkaAdminService(Admin.create(adminProps), ScramMechanism.fromMechanismName(mechanism));
    }
}
