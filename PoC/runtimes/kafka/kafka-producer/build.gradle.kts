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

plugins {
    application
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.kafka.clients)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    // SLF4J API
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")
    // Logback Classic provides a concrete implementation for SLF4J
    runtimeOnly("ch.qos.logback:logback-classic:1.5.13")

    testCompileOnly(libs.lombok)
    testAnnotationProcessor(libs.lombok)
}

application {
    mainClass.set("org.eclipse.tractusx.edc.kafka.producer.KafkaProducerApp")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    dependsOn("distTar", "distZip")
    mergeServiceFiles()
    archiveFileName.set("kafka-producer.jar")
}

description = "kafka-producer"