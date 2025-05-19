#
# Copyright (c) 2025 Contributors to the Eclipse Foundation
# Copyright (c) 2025 Cofinity-X GmbH
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
#

FROM eclipse-temurin:21-jre-alpine

ARG APP_USER=docker
ARG APP_UID=10100

RUN addgroup --system "$APP_USER" && \
    adduser \
        --shell /sbin/nologin \
        --disabled-password \
        --gecos "" \
        --ingroup "$APP_USER" \
        --no-create-home \
        --uid "$APP_UID" \
        "$APP_USER"

COPY build/libs/controlplane-local.jar /app/application.jar

USER "$APP_USER"

ENTRYPOINT [ \
     "java", \
     "-Dedc.fs.config=/app/configuration.properties", \
     "-Djava.security.egd=file:/dev/urandom", \
     "-jar", \
     "/app/application.jar", \
     "--log-level=DEBUG"]