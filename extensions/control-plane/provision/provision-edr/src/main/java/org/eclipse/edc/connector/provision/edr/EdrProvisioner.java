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

import org.eclipse.edc.connector.transfer.spi.edr.EdrGeneratorManager;
import org.eclipse.edc.connector.transfer.spi.edr.EdrResponse;
import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EdrProvisioner implements Provisioner<EdrResourceDefinition, EdrProvisionedContentResource> {

    private final EdrGeneratorManager edrGeneratorManager;

    public EdrProvisioner(EdrGeneratorManager edrGeneratorManager) {
        this.edrGeneratorManager = edrGeneratorManager;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof EdrResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof EdrProvisionedContentResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(EdrResourceDefinition resourceDefinition, Policy policy) {
        return edrGeneratorManager.generate(resourceDefinition.getDataRequest(), policy)
                .thenApply(result -> result.map(edr -> toResponse(resourceDefinition, edr)));
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(EdrProvisionedContentResource provisionedResource, Policy policy) {
        return null;
    }

    private ProvisionResponse toResponse(EdrResourceDefinition resourceDefinition, EdrResponse edr) {

        var provisioned = EdrProvisionedContentResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceDefinitionId(resourceDefinition.getId())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .dataAddress(edr.dataAddress())
                .resourceName(UUID.randomUUID().toString())
                .hasToken(false)
                .build();

        return ProvisionResponse.Builder.newInstance()
                .resource(provisioned)
                .secretToken(edr.secretToken())
                .build();
    }

}
