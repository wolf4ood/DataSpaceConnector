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

package org.eclipse.edc.connector.provision.edr;

import org.eclipse.edc.connector.transfer.spi.provision.ProviderResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class EdrProvisionerResourceDefinitionGenerator implements ProviderResourceDefinitionGenerator {

    @Override
    public @Nullable ResourceDefinition generate(DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
        return EdrResourceDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetAddress(assetAddress)
                .dataRequest(dataRequest)
                .build();
    }

    @Override
    public boolean canGenerate(DataRequest dataRequest, DataAddress assetAddress, Policy policy) {
        // TODO use transfer type
        return dataRequest.getDataDestination().getType().equals("HttpProxy");
    }
}
