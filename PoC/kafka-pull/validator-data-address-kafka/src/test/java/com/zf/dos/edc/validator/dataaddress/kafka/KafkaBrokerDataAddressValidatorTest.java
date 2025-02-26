package com.zf.dos.edc.validator.dataaddress.kafka;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationFailure;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.BOOTSTRAP_SERVERS;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.MECHANISM;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.PROTOCOL;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.SECRET_KEY;
import static com.zf.dos.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.TOPIC;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class KafkaBrokerDataAddressValidatorTest {

    private final KafkaBrokerDataAddressValidator validator = new KafkaBrokerDataAddressValidator();

    @Test
    void shouldPass_whenDataAddressIsValid() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("Kafka")
                .property(TOPIC, "topic.name")
                .property(BOOTSTRAP_SERVERS, "any:98123")
                .property(MECHANISM, "SCRAM-SHA-256")
                .property(PROTOCOL, "SASL_PLAINTEXT")
                .property(SECRET_KEY, "secretKey")
                .build();

        var result = validator.validate(dataAddress);

        assertThat(result).isSucceeded();
    }

    @Test
    void shouldFail_whenRequiredFieldsAreMissing() {
        var dataAddress = DataAddress.Builder.newInstance()
                .type("Kafka")
                .build();

        var result = validator.validate(dataAddress);

        assertThat(result).isFailed().extracting(ValidationFailure::getViolations)
                .satisfies(violations -> assertThat(violations).extracting(Violation::path)
                        .containsExactlyInAnyOrder(TOPIC, BOOTSTRAP_SERVERS, MECHANISM, PROTOCOL, SECRET_KEY));
    }
}