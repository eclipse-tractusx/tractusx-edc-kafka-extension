/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.tractusx.edc.kafka.producer.messages;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Implementation of MessageLoader that loads messages from classpath resources.
 */
@AllArgsConstructor
public class ResourceMessageLoader implements MessageLoader {

    public static final String DEFAULT_FORECAST_RESOURCE = "production-forecast.json";
    public static final String DEFAULT_TRACKING_RESOURCE = "production-tracking.json";
    private final ObjectMapper objectMapper;
    private final String forecastResourcePath;
    private final String trackingResourcePath;
    
    public ResourceMessageLoader(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_FORECAST_RESOURCE, DEFAULT_TRACKING_RESOURCE);
    }
    
    @Override
    public List<ForecastMessage> loadForecastMessages() throws IOException {
        try (final InputStream in = getClass().getClassLoader()
                .getResourceAsStream(forecastResourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Forecast resource not found on classpath: " + forecastResourcePath);
            }
            return objectMapper.readValue(in, new TypeReference<>() {});
        }
    }
    
    @Override
    public List<TrackingMessage> loadTrackingMessages() throws IOException {
        try (final InputStream in = getClass().getClassLoader()
                .getResourceAsStream(trackingResourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Tracking resource not found on classpath: " + trackingResourcePath);
            }
            return objectMapper.readValue(in, new TypeReference<>() {});
        }
    }
}