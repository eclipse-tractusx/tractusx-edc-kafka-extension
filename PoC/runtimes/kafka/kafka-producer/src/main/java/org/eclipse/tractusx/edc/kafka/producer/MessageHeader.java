package org.eclipse.tractusx.edc.kafka.producer;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class MessageHeader {

    private String        senderBpn;
    private String        relatedMessageId;
    private OffsetDateTime expectedResponseBy;
    private String        context;
    private String        messageId;
    private String        receiverBpn;
    private OffsetDateTime sentDateTime;
    private String        version;
}
