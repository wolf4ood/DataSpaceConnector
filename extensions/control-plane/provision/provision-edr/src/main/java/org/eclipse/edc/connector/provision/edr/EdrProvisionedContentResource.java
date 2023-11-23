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
 *
 */

package org.eclipse.edc.connector.provision.edr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;

/**
 * A reference to a provisioned resource.
 */
@JsonDeserialize(builder = EdrProvisionedContentResource.Builder.class)
@JsonTypeName("dataspaceconnector:edrprovisionedresource")
public class EdrProvisionedContentResource extends ProvisionedContentResource {

    private EdrProvisionedContentResource() {
        super();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedContentResource.Builder<EdrProvisionedContentResource, Builder> {

        private Builder() {
            super(new EdrProvisionedContentResource());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        @Override
        public Builder transferProcessId(String transferProcessId) {
            provisionedResource.transferProcessId = transferProcessId;
            return this;
        }
        
    }
}
