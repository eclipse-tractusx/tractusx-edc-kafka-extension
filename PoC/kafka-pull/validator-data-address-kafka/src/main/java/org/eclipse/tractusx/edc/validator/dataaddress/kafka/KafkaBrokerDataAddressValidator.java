/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 * Copyright (c) 2025 Cofinity-X GmbH
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
package org.eclipse.tractusx.edc.validator.dataaddress.kafka;

import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Validator;
import org.eclipse.edc.validator.spi.Violation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.*;

public class KafkaBrokerDataAddressValidator implements Validator<DataAddress> {
    public KafkaBrokerDataAddressValidator() {
    }

    public ValidationResult validate(final DataAddress input) {
        List<Violation> violations = Stream.of(
                TOPIC,
                BOOTSTRAP_SERVERS,
                MECHANISM,
                PROTOCOL,
                OAUTH_TOKEN_URL,
                OAUTH_REVOKE_URL,
                OAUTH_CLIENT_ID,
                OAUTH_CLIENT_SECRET_KEY
        ).map((final String it) -> {
            String value = input.getStringProperty(it);
            return value != null && !value.isBlank() ? null : Violation.violation("'%s' is a mandatory attribute".formatted(it), it, value);
        }).filter(Objects::nonNull).toList();
        return violations.isEmpty() ? ValidationResult.success() : ValidationResult.failure(violations);
    }
}
