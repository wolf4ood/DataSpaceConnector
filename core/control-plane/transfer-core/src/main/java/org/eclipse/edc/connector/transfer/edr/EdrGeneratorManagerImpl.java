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

package org.eclipse.edc.connector.transfer.edr;

import org.eclipse.edc.connector.transfer.spi.edr.EdrGenerator;
import org.eclipse.edc.connector.transfer.spi.edr.EdrGeneratorManager;
import org.eclipse.edc.connector.transfer.spi.edr.EdrResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

public class EdrGeneratorManagerImpl implements EdrGeneratorManager {

    private final List<EdrGenerator> edrGenerators;

    public EdrGeneratorManagerImpl() {
        edrGenerators = new ArrayList<>();
    }

    @Override
    public void register(EdrGenerator generator) {
        edrGenerators.add(generator);
    }

    @Override
    public CompletableFuture<StatusResult<EdrResponse>> generate(DataRequest dataRequest, Policy policy) {
        return edrGenerators.stream()
                .filter(generator -> generator.canGenerate(dataRequest, policy))
                .findFirst()
                .map(generator -> generator.generate(dataRequest, policy))
                .orElseGet(() -> CompletableFuture.completedFuture(StatusResult.failure(FATAL_ERROR, generatorNotFound(dataRequest.getId()))));
    }

    private String generatorNotFound(String id) {
        return format("Unable to process transfer %s. No Edr generator found", id);
    }
}
