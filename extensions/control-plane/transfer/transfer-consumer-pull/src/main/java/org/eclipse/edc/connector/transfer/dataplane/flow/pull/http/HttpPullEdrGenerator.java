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

package org.eclipse.edc.connector.transfer.dataplane.flow.pull.http;

import org.eclipse.edc.connector.transfer.spi.edr.EdrGenerator;
import org.eclipse.edc.connector.transfer.spi.edr.EdrResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.concurrent.CompletableFuture;

public class HttpPullEdrGenerator implements EdrGenerator {

    // TODO switch to HTTP-pull transfer type
    @Override
    public boolean canGenerate(DataRequest dataRequest, Policy policy) {
        return dataRequest.getDataDestination().getType().equals("HttpProxy");
    }

    @Override
    public CompletableFuture<StatusResult<EdrResponse>> generate(DataRequest dataRequest, Policy policy) {
        return null;
    }
}
