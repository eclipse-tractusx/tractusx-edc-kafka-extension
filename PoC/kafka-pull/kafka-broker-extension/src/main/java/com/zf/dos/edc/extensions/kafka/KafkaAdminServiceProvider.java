package com.zf.dos.edc.extensions.kafka;

import org.eclipse.edc.spi.types.domain.DataAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Handles creating KafkaAdminService.
 */
public interface KafkaAdminServiceProvider{
    KafkaAdminService  provide(DataAddress dataAddress, String secret) throws ExecutionException, InterruptedException, TimeoutException;
}
