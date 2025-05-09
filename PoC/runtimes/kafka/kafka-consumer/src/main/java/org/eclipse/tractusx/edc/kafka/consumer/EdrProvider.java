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

package org.eclipse.tractusx.edc.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Slf4j
public class EdrProvider {
    private static final String MANAGEMENT_URL = "http://control-plane-bob:9191/management/v3";
    private static final String API_KEY = "password";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String EDR_REQUEST_PATH = "/edrs/request";
    private static final String EDR_DATA_PATH = "/edrs/%s/dataaddress?auto_refresh=true";
    private static final String ASSET_ID = "kafka-stream-asset";
    public static final String X_API_KEY = "X-API-KEY";
    public static final String CONTENT_TYPE = "Content-Type";

    private final HttpClient client;
    private final ObjectMapper objectMapper;

    /**
     * Creates a default EdrProvider with standard configuration
     */
    public EdrProvider() {
        this.client = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Retrieves the EDR data from the management API
     *
     * @return The EDR data or null if not available
     */
    public EDRData getEdr() {
        String transferProcessId = getTransferProcessId();
        String dataAddressUrl = MANAGEMENT_URL + String.format(EDR_DATA_PATH, transferProcessId);

        HttpRequest request = createGetRequest(dataAddressUrl);
        HttpResponse<String> response = sendRequest(request, "Failed to get EDR data");

        if (response.statusCode() == 200) {
            log.info("Successfully received EDR data response");
            String responseBody = response.body();
            try {
                EDRData edrData = objectMapper.readValue(responseBody, EDRData.class);
                log.info("Received EDRData: {}", edrData);
                return edrData;
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse EDR data response", e);
            }
        } else {
            log.error("GET request failed. Status code: {}", response.statusCode());
            log.error("EDR is not accessible");
            return null;
        }
    }

    private String getTransferProcessId() {
        final String edrRequestUrl = MANAGEMENT_URL + EDR_REQUEST_PATH;

        QuerySpec querySpec = createQuerySpec();
        String body = serializeQuerySpec(querySpec);

        HttpRequest request = createPostRequest(edrRequestUrl, body);
        HttpResponse<String> response = sendRequest(request, "Failed to get transfer process ID");

        if (response.statusCode() == 200) {
            log.info("Successfully received transfer process response");
            String responseBody = response.body();
            try {
                EndpointDataReferenceEntry[] edrEntries = objectMapper.readValue(responseBody, EndpointDataReferenceEntry[].class);
                if (edrEntries.length > 0) {
                    EndpointDataReferenceEntry edrEntry = edrEntries[0];
                    log.info("Received EDREntry: {}", edrEntry);
                    return edrEntry.getTransferProcessId();
                } else {
                    log.warn("No EDR entries found");
                    throw new RuntimeException("No transfer process ID found - no EDR entries returned");
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse EDR entry response", e);
            }
        } else {
            log.error("POST request failed. Status code: {}, Body: {}", response.statusCode(), body);
            throw new RuntimeException("EDR is not accessible - failed to get transfer process ID");
        }
    }

    private QuerySpec createQuerySpec() {
        return QuerySpec.builder()
            .context(Map.of("@vocab", "https://w3id.org/edc/v0.0.1/ns/"))
            .type("QuerySpec")
            .offset(0)
            .limit(1)
            .sortField("createdAt")
            .sortOrder("DESC")
            .filterExpression(
                List.of(new QuerySpec.FilterExpression("assetId", "=", ASSET_ID))
            )
            .build();
    }

    private String serializeQuerySpec(final QuerySpec querySpec) {
        try {
            return objectMapper.writeValueAsString(querySpec);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialize QuerySpec to JSON", e);
        }
    }

    private HttpRequest createGetRequest(final String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(X_API_KEY, API_KEY)
                .header(CONTENT_TYPE, CONTENT_TYPE_JSON)
                .GET()
                .build();
    }

    private HttpRequest createPostRequest(final String url, final String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(X_API_KEY, API_KEY)
                .header(CONTENT_TYPE, CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpResponse<String> sendRequest(final HttpRequest request, final String errorMessage) {
        try {
            log.info("Sending request to {}", request.uri());
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(errorMessage, e);
        }
    }
}
