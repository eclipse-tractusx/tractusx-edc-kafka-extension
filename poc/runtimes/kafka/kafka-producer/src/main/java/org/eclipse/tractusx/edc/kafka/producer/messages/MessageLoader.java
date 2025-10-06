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

import java.io.IOException;
import java.util.List;

/**
 * Interface for loading message data from various sources.
 */
public interface MessageLoader {
    
    /**
     * Loads forecast messages from a data source.
     * 
     * @return List of forecast messages
     * @throws IOException if loading fails
     */
    List<ForecastMessage> loadForecastMessages() throws IOException;
    
    /**
     * Loads tracking messages from a data source.
     * 
     * @return List of tracking messages  
     * @throws IOException if loading fails
     */
    List<TrackingMessage> loadTrackingMessages() throws IOException;
}