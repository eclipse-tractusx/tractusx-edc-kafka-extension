/*
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
    implementation("org.eclipse.edc:transfer-spi:0.11.0-20250110-SNAPSHOT")
    implementation("org.eclipse.edc:validator-spi:0.11.0-20250110-SNAPSHOT")
    implementation("org.apache.kafka:kafka-clients:3.7.0")
    implementation("org.eclipse.edc:util-lib:0.11.0-20250110-SNAPSHOT")
    implementation(project(":data-address-kafka"))
    implementation(project(":validator-data-address-kafka"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.eclipse.edc:junit:0.11.0-20250110-SNAPSHOT")
    testImplementation("org.mockito:mockito-core:5.2.0")
}

tasks.test {
    useJUnitPlatform()
}