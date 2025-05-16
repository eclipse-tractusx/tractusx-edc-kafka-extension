package org.eclipse.tractusx.edc.kafka.producer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ForecastMessage {

    private Request request;
    private MessageHeader  header;

    @Data
    public static class Request {

        private TimeQuantity precisionOfForecast;
        private TimeQuantity offset;
        private String       orderId;
        private String       customerId;
        private TimeQuantity deviationOfSchedule;
        private boolean      productionForecastForAll;
        private String       version;
        private TimeQuantity notificationInterval;
        private String       communicationMode;
    }

    @Data
    public static class TimeQuantity {

        private int value;

        @JsonProperty("timeUnit")   // keep original JSON field-name
        private String unit;
    }
}
