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
package org.eclipse.tractusx.edc.extensions.kafka;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;
import org.eclipse.tractusx.edc.extensions.kafka.auth.KafkaOAuthServiceImpl;

import java.util.Map;

/**
 * Kafka Broker flow extension
 */
@Extension(value = KafkaBrokerExtension.NAME)
public class KafkaBrokerExtension implements ServiceExtension {

    public static final String NAME = "Kafka stream extension";
    private static final String DEFAULT_DATAPLANE_SELECTOR_STRATEGY = "random";
    @Setting(value = "Defines strategy for Data Plane instance selection in case Data Plane is not embedded in current runtime", defaultValue = DEFAULT_DATAPLANE_SELECTOR_STRATEGY)
    private static final String DPF_SELECTOR_STRATEGY = "edc.dataplane.client.selector.strategy";

    @Inject
    private DataFlowManager dataFlowManager;

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    @Inject
    private EdcHttpClient httpClient;

    @Inject
    private DataPlaneSelectorService selectorService;

    @Inject
    private DataPlaneClientFactory clientFactory;

    @Inject(required = false)
    private DataFlowPropertiesProvider propertiesProvider;

    @Inject
    private TransferTypeParser transferTypeParser;

    @Inject
    private Monitor monitor;

    @Inject(required = false)
    private ControlApiUrl callbackUrl;


    @Override
    public void initialize(final ServiceExtensionContext context) {
        var selectionStrategy = context.getSetting(DPF_SELECTOR_STRATEGY, DEFAULT_DATAPLANE_SELECTOR_STRATEGY);
        var kafkaOAuthService = new KafkaOAuthServiceImpl(httpClient, typeManager.getMapper());
        var controller = new KafkaBrokerDataFlowController(vault, kafkaOAuthService, transferTypeParser, getPropertiesProvider(),
                selectorService, clientFactory, selectionStrategy, monitor, callbackUrl);
        dataFlowManager.register(controller);
    }

    private DataFlowPropertiesProvider getPropertiesProvider() {
        return propertiesProvider == null ? (tp, p) -> StatusResult.success(Map.of()) : propertiesProvider;
    }
}