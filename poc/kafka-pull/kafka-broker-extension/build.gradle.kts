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

dependencies {
   implementation(libs.edc.spi.transfer)
   implementation(libs.edc.spi.validator)
   implementation(libs.kafka.clients)
   implementation(libs.edc.lib.util)
   implementation(project(":data-address-kafka"))
   implementation(project(":validator-data-address-kafka"))
   implementation(libs.edc.spi.http)
   implementation(libs.edc.transfer.data.plane.signaling)

   testImplementation(libs.junit.jupiter)
   testImplementation(libs.assertj)
   testImplementation(libs.edc.junit)
   testImplementation(libs.mockito.core)
}

tasks.test {
   useJUnitPlatform()
}