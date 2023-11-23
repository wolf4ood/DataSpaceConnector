/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.flow.pull;

import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static org.eclipse.edc.connector.transfer.spi.flow.FlowType.PULL;

public class ConsumerPullTransferDataFlowController implements DataFlowController {
    @Override
    public boolean canHandle(TransferProcess transferProcess) {
        return false;
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> initiateFlow(TransferProcess transferProcess, Policy policy) {
        return null;
    }

    @Override
    public StatusResult<Void> terminate(TransferProcess transferProcess) {
        return null;
    }

    @Override
    public Set<String> transferTypesFor(Asset asset) {
        return Set.of("%s-%s".formatted("Http", PULL));
    }
}
