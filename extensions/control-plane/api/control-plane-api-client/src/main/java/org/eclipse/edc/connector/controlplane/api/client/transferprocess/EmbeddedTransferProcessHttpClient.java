/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.api.client.transferprocess;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.CompleteProvisionCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.connector.dataplane.spi.DataFlow;
import org.eclipse.edc.connector.dataplane.spi.port.TransferProcessApiClient;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.function.Function;

import static org.eclipse.edc.spi.response.ResponseStatus.ERROR_RETRY;


public class EmbeddedTransferProcessHttpClient implements TransferProcessApiClient {

    private final TransferProcessService transferProcessService;

    public EmbeddedTransferProcessHttpClient(TransferProcessService transferProcessService) {
        this.transferProcessService = transferProcessService;
    }

    @Override
    public StatusResult<Void> completed(DataFlow dataFlow) {
        return transferProcessService.complete(dataFlow.getId())
                .flatMap(toStatusResult());
    }

    @Override
    public StatusResult<Void> failed(DataFlow dataFlow, String reason) {
        return transferProcessService.terminate(new TerminateTransferCommand(dataFlow.getId(), reason))
                .flatMap(toStatusResult());
    }

    @Override
    public StatusResult<Void> provisioned(DataFlow dataFlow) {
        return transferProcessService.completeProvision(new CompleteProvisionCommand(dataFlow.getId(), dataFlow.provisionedDataAddress()))
                .flatMap(toStatusResult());
    }

    private Function<ServiceResult<Void>, StatusResult<Void>> toStatusResult() {
        return it -> {
            if (it.succeeded()) {
                return StatusResult.success();
            } else {
                return StatusResult.failure(ERROR_RETRY, it.getFailureDetail());
            }
        };
    }
}
