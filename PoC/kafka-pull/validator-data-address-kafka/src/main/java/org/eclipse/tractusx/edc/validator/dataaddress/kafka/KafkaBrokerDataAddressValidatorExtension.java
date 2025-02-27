package org.eclipse.tractusx.edc.validator.dataaddress.kafka;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.KAFKA_TYPE;

@Extension(value = KafkaBrokerDataAddressValidatorExtension.NAME)
public class KafkaBrokerDataAddressValidatorExtension implements ServiceExtension {
    public static final String NAME = "DataAddress KafkaBroker Validator";
    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;

    public KafkaBrokerDataAddressValidatorExtension() {
    }

    public void initialize(ServiceExtensionContext context) {
        var validator = new KafkaBrokerDataAddressValidator();
        this.dataAddressValidatorRegistry.registerSourceValidator(KAFKA_TYPE, validator);
        this.dataAddressValidatorRegistry.registerDestinationValidator(KAFKA_TYPE, validator);
    }
}
