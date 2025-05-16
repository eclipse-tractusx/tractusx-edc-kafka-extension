package org.eclipse.tractusx.edc.kafka.producer;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TrackingMessage {

    private Request request;
    private Header header;

    @Data
    public static class Request {

        private String identifierNumber;
        private String catenaXId;
        private List<StepIdentifier> stepIdentifierList;
        private String customerId;
        private String billOfProcessId;
        private String identifierType;
        private String version;
        private String processReferenceType;
    }

    @Data
    public static class Header {

        private String        senderBpn;
        private String        relatedMessageId;
        private OffsetDateTime expectedResponseBy;
        private String        context;
        private String        messageId;
        private String        receiverBpn;
        private OffsetDateTime sentDateTime;
        private String        version;
    }

    @Data
    public static class StepIdentifier {

        private String processStepId;
        private List<ProcessParameter> processParameterList;
        private String capabilityId;
        private String billOfMaterialElementId;
        private String partInstanceLevel;
        private String partInstanceId;
        private String billOfMaterialId;
    }

    @Data
    public static class ProcessParameter {

        @JsonProperty("processParameterSemanticId")
        private String semanticId;

        @JsonProperty("processParameterName")
        private String name;
    }
}
