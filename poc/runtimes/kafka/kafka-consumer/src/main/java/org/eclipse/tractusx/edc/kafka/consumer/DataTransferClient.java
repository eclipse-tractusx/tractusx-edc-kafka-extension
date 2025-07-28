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

package org.eclipse.tractusx.edc.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
public class DataTransferClient {

    // API Endpoints
    private static final String CATALOG_DATASET_REQUEST_PATH = "/v3/catalog/dataset/request";
    private static final String CONTRACT_NEGOTIATIONS_PATH = "/v3/contractnegotiations";
    private static final String TRANSFER_PROCESSES_PATH = "/v3/transferprocesses";
    private static final String EDR_DATA_PATH_TEMPLATE = "/v3/edrs/%s/dataaddress";

    // Protocol Constants
    private static final String TRANSFER_TYPE = "Kafka-PULL";
    private static final String KAFKA_BROKER_TYPE = "KafkaBroker";
    private static final String DATASPACE_PROTOCOL = "dataspace-protocol-http";

    // HTTP Headers
    private static final String API_KEY_HEADER = "X-API-KEY";
    private static final String CONTENT_TYPE_JSON = "application/json";

    // Process States
    private static final String STATE_FINALIZED = "FINALIZED";
    private static final String STATE_STARTED = "STARTED";

    // Polling Configuration
    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final int POLL_INTERVAL_SECONDS = 1;

    private final String consumerManagementUrl;
    private final String providerId;
    private final String providerProtocolUrl;
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new instance of {@code DataTransferClient}.
     *
     * @param consumerManagementUrl The URL of the Consumer Management API.
     * @param providerId            The ID of the provider with which interactions will occur.
     * @param providerProtocolUrl   The URL of the provider's protocol endpoint.
     */
    public DataTransferClient(final String consumerManagementUrl, final String providerId, final String providerProtocolUrl) {
        this.consumerManagementUrl = consumerManagementUrl;
        this.providerId = providerId;
        this.providerProtocolUrl = providerProtocolUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Default constructor for the {@code DataTransferClient} class.
     * <p>
     * Initializes an instance of {@code DataTransferClient} using default values for
     * the consumer management URL, provider ID, and provider protocol URL.
     */
    public DataTransferClient() {
        this(KafkaConsumerApp.EDC_MANAGEMENT_URL, KafkaConsumerApp.PROVIDER_ID, KafkaConsumerApp.PROVIDER_PROTOCOL_URL);
    }

    /**
     * Execute the complete data transfer workflow:
     * 1. Request dataset
     * 2. Initiate contract negotiation
     * 3. Wait for negotiation to finalize
     * 4. Initiate transfer process
     * 5. Wait for transfer process to start
     * 6. Request the EDR
     *
     * @param assetId The ID of the asset to request
     * @return The EDRData if successful
     * @throws IOException          If there's an error in communication
     * @throws InterruptedException If the polling is interrupted
     */
    public EDRData executeDataTransferWorkflow(String assetId)
            throws IOException, InterruptedException {
        // Step 1: Request dataset
        DatasetResponse datasetResponse = requestDataset(assetId);
        log.debug("Dataset response received: {}", datasetResponse);

        // Step 2: Initiate contract negotiation using the offer ID from the dataset response
        String offerId = datasetResponse.getOfferPolicyId();
        ContractNegotiationResponse negotiationResponse = initiateContractNegotiation(assetId, offerId);
        log.debug("Contract negotiation initiated: {}", negotiationResponse);

        // Step 3: Poll for contract negotiation to reach FINALIZED state
        ContractNegotiationStatusResponse finalizedContract = pollUntilConditionMet(
                negotiationId -> {
                    try {
                        return getContractNegotiationStatus(negotiationId);
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                statusResponse -> STATE_FINALIZED.equals(statusResponse.getState()),
                negotiationResponse.getNegotiationId(),
                "Contract negotiation did not reach FINALIZED state"
        );
        log.debug("Contract negotiation finalized: {}", finalizedContract);

        // Step 4: Initiate transfer process using the contract agreement ID
        String contractAgreementId = finalizedContract.getContractAgreementId();
        TransferProcessResponse transferResponse = initiateTransferProcess(assetId, contractAgreementId);
        log.debug("Transfer process initiated: {}", transferResponse);

        // Step 5: Poll for transfer process to reach STARTED state
        TransferProcessResponse startedTransfer = pollUntilConditionMet(
                transferId -> {
                    try {
                        return getTransferProcessStatus(transferId);
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                },
                statusResponse -> STATE_STARTED.equals(statusResponse.getState()),
                transferResponse.getTransferId(),
                "Transfer process did not reach STARTED state"
        );
        log.debug("Transfer process started: {}", startedTransfer);

        // Step 6: Request the EDR with the transferProcessId
        return getEndpointDataReference(startedTransfer.getTransferId());
    }

    private DatasetResponse requestDataset(final String assetId)
            throws IOException, InterruptedException {
        String url = consumerManagementUrl + CATALOG_DATASET_REQUEST_PATH;

        ObjectNode requestBody = createBaseRequestNode("DatasetRequest");
        requestBody.put("@id", assetId);
        requestBody.put("protocol", DATASPACE_PROTOCOL);
        requestBody.put("counterPartyAddress", providerProtocolUrl);
        requestBody.put("counterPartyId", providerId);

        JsonNode responseJson = executeHttpRequest(url, requestBody, "Dataset request failed");
        String offerId = responseJson.path("odrl:hasPolicy").path("@id").asText();

        return DatasetResponse.builder()
                .assetId(responseJson.path("@id").asText())
                .offerPolicyId(offerId)
                .build();
    }

    private ContractNegotiationResponse initiateContractNegotiation(String assetId, String offerId)
            throws IOException, InterruptedException {
        String url = consumerManagementUrl + CONTRACT_NEGOTIATIONS_PATH;

        ObjectNode requestBody = createBaseRequestNode("ContractRequest");
        ObjectNode context = (ObjectNode) requestBody.get("@context");
        context.put("odrl", "http://www.w3.org/ns/odrl/2/");

        requestBody.put("counterPartyAddress", providerProtocolUrl);
        requestBody.put("protocol", DATASPACE_PROTOCOL);

        ObjectNode policy = objectMapper.createObjectNode();
        policy.put("@context", "http://www.w3.org/ns/odrl.jsonld");
        policy.put("@id", offerId);
        policy.put("@type", "Offer");
        policy.put("assigner", providerId);
        policy.put("target", assetId);
        requestBody.set("policy", policy);

        JsonNode responseJson = executeHttpRequest(url, requestBody, "Contract negotiation initiation failed");

        return ContractNegotiationResponse.builder()
                .negotiationId(responseJson.path("@id").asText())
                .build();
    }

    private ContractNegotiationStatusResponse getContractNegotiationStatus(String negotiationId)
            throws IOException, InterruptedException {
        String url = consumerManagementUrl + CONTRACT_NEGOTIATIONS_PATH + "/" + negotiationId;

        JsonNode responseJson = executeHttpGetRequest(url, "Contract negotiation status request failed");

        return ContractNegotiationStatusResponse.builder()
                .negotiationId(responseJson.path("@id").asText())
                .state(responseJson.path("state").asText())
                .contractAgreementId(responseJson.path("contractAgreementId").asText())
                .build();
    }

    private TransferProcessResponse initiateTransferProcess(String assetId, String contractAgreementId)
            throws IOException, InterruptedException {
        String url = consumerManagementUrl + TRANSFER_PROCESSES_PATH;

        ObjectNode requestBody = createBaseRequestNode("TransferRequest");
        requestBody.put("protocol", DATASPACE_PROTOCOL);
        requestBody.put("assetId", assetId);
        requestBody.put("contractId", contractAgreementId);
        requestBody.put("connectorId", providerId);
        requestBody.put("transferType", TRANSFER_TYPE);
        requestBody.put("counterPartyAddress", providerProtocolUrl);

        ObjectNode dataDestination = objectMapper.createObjectNode();
        dataDestination.put("type", KAFKA_BROKER_TYPE);
        requestBody.set("dataDestination", dataDestination);

        JsonNode responseJson = executeHttpRequest(url, requestBody, "Transfer process initiation failed");

        return TransferProcessResponse.builder()
                .transferId(responseJson.path("@id").asText())
                .build();
    }

    private TransferProcessResponse getTransferProcessStatus(String transferId)
            throws IOException, InterruptedException {
        String url = consumerManagementUrl + TRANSFER_PROCESSES_PATH + "/" + transferId;

        JsonNode responseJson = executeHttpGetRequest(url, "Transfer process status request failed");

        return TransferProcessResponse.builder()
                .transferId(responseJson.path("@id").asText())
                .state(responseJson.path("state").asText())
                .assetId(responseJson.path("assetId").asText())
                .contractId(responseJson.path("contractId").asText())
                .transferType(responseJson.path("transferType").asText())
                .dataDestination(Map.of("type", responseJson.path("dataDestination")
                        .path("type").asText()))
                .build();
    }

    private EDRData getEndpointDataReference(String transferId) throws IOException, InterruptedException {
        String url = consumerManagementUrl + String.format(EDR_DATA_PATH_TEMPLATE, transferId);
        JsonNode responseJson = executeHttpGetRequest(url, "Failed to get EDR data");
        try {
            String responseBody = responseJson.toString();
            log.debug(responseBody);
            EDRData edrData = objectMapper.readValue(responseBody, EDRData.class);
            log.debug("Received EDRData: {}", edrData);
            return edrData;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse EDR data response", e);
        }
    }

    private <T> T pollUntilConditionMet(
            Function<String, T> statusFunction,
            Predicate<T> condition,
            String id,
            String errorMessage)
            throws IOException, InterruptedException {

        T statusResponse = null;
        boolean isConditionMet = false;
        int attempts = 0;

        while (!isConditionMet && attempts < MAX_POLL_ATTEMPTS) {
            statusResponse = statusFunction.apply(id);

            if (condition.test(statusResponse)) {
                isConditionMet = true;
            } else {
                attempts++;
                TimeUnit.SECONDS.sleep(POLL_INTERVAL_SECONDS);
            }
        }

        if (!isConditionMet) {
            throw new IOException(errorMessage + " after " + MAX_POLL_ATTEMPTS + " attempts");
        }

        return statusResponse;
    }

    private ObjectNode createBaseRequestNode(String type) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ObjectNode context = objectMapper.createObjectNode();
        context.put("@vocab", "https://w3id.org/edc/v0.0.1/ns/");
        requestBody.set("@context", context);
        requestBody.put("@type", type);
        return requestBody;
    }

    private JsonNode executeHttpRequest(String url, ObjectNode requestBody, String errorMessage)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .header(API_KEY_HEADER, KafkaConsumerApp.EDC_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(errorMessage + " with status code: "
                    + response.statusCode() + " and body: " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    private JsonNode executeHttpGetRequest(String url, String errorMessage)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .header(API_KEY_HEADER, KafkaConsumerApp.EDC_API_KEY)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException(errorMessage + " with status code: "
                    + response.statusCode() + " and body: " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    @Data
    @Builder
    public static class DatasetResponse {
        private String assetId;
        private String offerPolicyId;
    }

    @Data
    @Builder
    public static class ContractNegotiationResponse {
        private String negotiationId;
    }

    @Data
    @Builder
    public static class ContractNegotiationStatusResponse {
        private String negotiationId;
        private String state;
        private String contractAgreementId;
    }

    @Data
    @Builder
    public static class TransferProcessResponse {
        private String transferId;
        private String state;
        private String assetId;
        private String contractId;
        private String transferType;
        private Map<String, String> dataDestination;
    }
}