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

package org.eclipse.tractusx.edc.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

public class EdrProvider {

    public static final String MANAGEMENT_URL = "http://control-plane-bob:9191/management/v2";
    private final HttpClient client;

    public EdrProvider() {
        client = HttpClient.newHttpClient();
    }

    public EDRData getEdr() {
        String transferProcessId = getTransferProcessId();

        String dataAddressUrl = MANAGEMENT_URL + "/edrs/" + transferProcessId + "/dataaddress";
        EDRData edrData = null;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(dataAddressUrl))
                .header("X-API-KEY", "password")
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            System.out.println("Sent request to " + dataAddressUrl);
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() == 200) {
            System.out.println("Response in status code 200");
            String responseBody = response.body();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                edrData = objectMapper.readValue(responseBody, EDRData.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Received EDRData:");
            System.out.println(edrData);
        } else {
            System.out.printf("GET request failed. Status code: %d%n", response.statusCode());
            System.out.println("EDR is not accessible");
        }

        return edrData;
    }

    private String getTransferProcessId() {
        final String edr_request_url = MANAGEMENT_URL + "/edrs/request";

        QuerySpec querySpec = new QuerySpec();
        querySpec.setContext(Map.of("@vocab", "https://w3id.org/edc/v0.0.1/ns/"));
        querySpec.setType("QuerySpec");
        querySpec.setOffset(0);
        querySpec.setLimit(1);
        querySpec.setFilterExpression(List.of(new QuerySpec.FilterExpression("assetId", "=", "kafka-stream-asset")));

        String body;
        try {
            body = new ObjectMapper().writeValueAsString(querySpec);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not parse QuerySpec to String", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(edr_request_url))
                .header("X-API-KEY", "password")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response;
        try {
            System.out.println("Sent request to " + edr_request_url);
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        EDREntry edrEntry = null;
        if (response.statusCode() == 200) {
            System.out.println("Response in status code 200");
            String responseBody = response.body();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            try {
                EDREntry[] edrEntries = objectMapper.readValue(responseBody, EDREntry[].class);
                if (edrEntries.length > 0) {
                    edrEntry = edrEntries[0];
                } else {
                    System.out.println("No EDR entries found");
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Received EDREntry:");
            System.out.println(edrEntry);
        } else {
            System.out.printf("POST request failed. Status code: %d, Body: %s%n", response.statusCode(), body);
            System.out.println("EDR is not accessible");
        }
        String transferProcessId;
        if (edrEntry != null) {
            transferProcessId = edrEntry.getTransferProcessId();
        } else {
            throw new RuntimeException("no transfer process id found");
        }
        return transferProcessId;
    }
}
