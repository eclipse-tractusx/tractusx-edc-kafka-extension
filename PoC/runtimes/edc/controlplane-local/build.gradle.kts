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
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.tx.edc.controlplane.base) {
        // DID / DCP extensions
        exclude(group = "org.eclipse.edc", module = "identity-did-core")
        exclude(group = "org.eclipse.edc", module = "identity-did-web")
        exclude(group = "org.eclipse.edc", module = "identity-trust-core")
        exclude(group = "org.eclipse.edc", module = "identity-trust-transform")
        exclude(group = "org.eclipse.edc", module = "identity-trust-issuers-configuration")
        exclude(group = "org.eclipse.tractusx.edc", module = "tx-dcp")
        exclude(group = "org.eclipse.tractusx.edc", module = "tx-dcp-sts-dim")

        // BDRS
        exclude(group = "org.eclipse.tractusx.edc", module = "bdrs-client")

        // Azure
        exclude(group = "org.eclipse.tractusx.edc", module = "azblob-provisioner")

        // Federated Catalog
        exclude(group = "org.eclipse.edc", module = "federated-catalog-core")
        exclude(group = "org.eclipse.edc", module = "federated-catalog-api")
        exclude(group = "org.eclipse.tractusx.edc", module = "federated-catalog")
    }

    implementation(libs.edc.iam.mock)
    implementation(project(":kafka-broker-extension"))
    implementation(project(":local-services"))
    implementation(project(":seed-vault"))
}

application {
    mainClass.set("org.eclipse.edc.boot.system.runtime.BaseRuntime")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    dependsOn("distTar", "distZip")
    mergeServiceFiles()
    archiveFileName.set("controlplane-local.jar")
}

description = "controlplane-local"
