/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - add toBuilder method
 *
 */

package org.eclipse.edc.connector.provision.edr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Objects;

/**
 * A resource to be provisioned by an external HTTP endpoint.
 */
@JsonTypeName("dataspaceconnector:edrproviderresourcedefinition")
@JsonDeserialize(builder = EdrResourceDefinition.Builder.class)
public class EdrResourceDefinition extends ResourceDefinition {

    private DataAddress assetAddress;

    private DataRequest dataRequest;

    private EdrResourceDefinition() {
        super();
    }

    public DataAddress getAssetAddress() {
        return assetAddress;
    }


    public DataRequest getDataRequest() {
        return dataRequest;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder());
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder
            extends ResourceDefinition.Builder<EdrResourceDefinition, Builder> {

        protected Builder() {
            super(new EdrResourceDefinition());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetAddress(DataAddress assetAddress) {
            resourceDefinition.assetAddress = assetAddress;
            return this;
        }

        public Builder dataRequest(DataRequest dataRequest) {
            resourceDefinition.dataRequest = dataRequest;
            return this;
        }

        @Override
        protected void verify() {
            Objects.requireNonNull(resourceDefinition.assetAddress, "Data Address should not be null");
            Objects.requireNonNull(resourceDefinition.dataRequest, "Data Request should not be null");
        }
    }
}
