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

rootProject.name = "Poc"
include(":controlplane-local")
include(":dataplane-local")
include(":seed-vault")
include(":local-services")
include(":kafka-consumer")
include(":kafka-producer")
include(":kafka-broker-extension")
include(":data-address-kafka")
include(":validator-data-address-kafka")

project(":controlplane-local").projectDir = file("runtimes/edc/controlplane-local")
project(":dataplane-local").projectDir = file("runtimes/edc/dataplane-local")
project(":seed-vault").projectDir = file("runtimes/edc/seed-in-memory-vault")
project(":local-services").projectDir = file("runtimes/edc/local-services")
project(":kafka-consumer").projectDir = file("runtimes/kafka/kafka-consumer")
project(":kafka-producer").projectDir = file("runtimes/kafka/kafka-producer")
project(":kafka-broker-extension").projectDir = file("kafka-pull/kafka-broker-extension")
project(":data-address-kafka").projectDir = file("kafka-pull/data-address-kafka")
project(":validator-data-address-kafka").projectDir = file("kafka-pull/validator-data-address-kafka")

