package com.zf.dos.edc.validator.dataaddress.kafka;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.MECHANISM;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.PROTOCOL;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.SECRET_KEY;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;

public class KafkaBrokerDataAddressValidator implements Validator<DataAddress> {
    public KafkaBrokerDataAddressValidator() {
    }

    public ValidationResult validate(DataAddress input) {
        List<Violation> violations = Stream.of(TOPIC, BOOTSTRAP_SERVERS, MECHANISM, PROTOCOL, SECRET_KEY).map((String it) -> {
            String value = input.getStringProperty(it);
            return value != null && !value.isBlank() ? null : Violation.violation("'%s' is a mandatory attribute".formatted(it), it, value);
        }).filter(Objects::nonNull).toList();
        return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
    }
}
