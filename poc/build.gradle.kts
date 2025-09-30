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

import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin

plugins {
    `java-library`
    jacoco
    `jacoco-report-aggregation`
    id ("org.sonarqube") version "6.2.0.5505"
    alias(libs.plugins.shadow)
    alias(libs.plugins.docker)
}

val javaVersion: String by project

project.subprojects.forEach {
    dependencies {
        jacocoAggregation(project(it.path))
    }

}

allprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }

        tasks.withType(JavaCompile::class.java) {
            // making sure the code does not use any APIs from a more recent version.
            // Ref: https://docs.gradle.org/current/userguide/building_java_projects.html#sec:java_cross_compilation
            options.release.set(javaVersion.toInt())
        }
        withJavadocJar()
        withSourcesJar()
    }

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

tasks.check {
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}

subprojects {
    tasks.register<DependencyReportTask>("allDependencies") {}
    afterEvaluate {
        // the "dockerize" task is added to all projects that use the `shadowJar` plugin or `spring.boot` plugin
        if (project.plugins.hasPlugin(libs.plugins.shadow.get().pluginId) || 
            project.plugins.hasPlugin(libs.plugins.spring.boot.get().pluginId)) {
            val downloadOpentelemetryAgent = tasks.create("downloadOpentelemetryAgent", Copy::class) {
                val openTelemetry = configurations.create("open-telemetry")

                dependencies {
                    openTelemetry(libs.opentelemetry.javaagent)
                }

                from(openTelemetry)
                into("build/resources/otel")
                rename { "opentelemetry-javaagent.jar" }
            }

            val copyLegalDocs = tasks.create("copyLegalDocs", Copy::class) {
                from(project.rootProject.projectDir)
                into("build/legal")
                include("SECURITY.md", "NOTICE.md", "DEPENDENCIES", "LICENSE")
            }

            val copyDockerfile = tasks.create("copyDockerfile", Copy::class) {
                from(rootProject.projectDir.toPath().resolve("resources"))
                into(project.layout.buildDirectory.dir("resources").get().dir("docker"))
                include("Dockerfile")
            }

            // Determine which jar task to use based on the plugin
            val jarTask = if (project.plugins.hasPlugin(libs.plugins.shadow.get().pluginId)) {
                tasks.named(ShadowJavaPlugin.SHADOW_JAR_TASK_NAME).get()
            } else {
                // For Spring Boot projects, use bootJar task
                tasks.named("bootJar").get()
            }

            jarTask
                .dependsOn(copyDockerfile)
                .dependsOn(copyLegalDocs)
                .dependsOn(downloadOpentelemetryAgent)

            //actually apply the plugin to the (sub-)project
            apply(plugin = libs.plugins.docker.get().pluginId)

            val dockerTask: DockerBuildImage = tasks.create("dockerize", DockerBuildImage::class) {
                dockerFile.set(File("build/resources/docker/Dockerfile"))

                val dockerContextDir = project.projectDir
                images.add("${project.name}:${project.version}")
                images.add("${project.name}:latest")

                if (System.getProperty("platform") != null) {
                    platform.set(System.getProperty("platform"))
                }

                buildArgs.put("JAR", "build/libs/${project.name}.jar")
                buildArgs.put("OTEL_JAR", "build/resources/otel/opentelemetry-javaagent.jar")
                buildArgs.put("ADDITIONAL_FILES", "build/legal/*")
                inputDir.set(file(dockerContextDir))
            }

            dockerTask.dependsOn(jarTask)
        }
    }
}