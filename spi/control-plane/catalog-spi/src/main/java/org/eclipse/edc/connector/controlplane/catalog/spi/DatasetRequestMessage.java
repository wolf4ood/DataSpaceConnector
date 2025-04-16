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
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.edc.connector.controlplane.catalog.spi;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.DEFAULT_DATASPACE_CONTEXT;

/**
 * A request for a participant's {@link Dataset}.
 */
public class DatasetRequestMessage implements RemoteMessage {

    private final Policy policy;
    protected String dataspaceContext = DEFAULT_DATASPACE_CONTEXT;
    protected String participantContextId;
    private String datasetId;
    private String protocol;
    private String counterPartyAddress;
    private String counterPartyId;

    private DatasetRequestMessage() {
        // at this time, this is just a placeholder.
        policy = Policy.Builder.newInstance().build();
    }

    public String getDatasetId() {
        return datasetId;
    }

    @NotNull
    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }

    public void setParticipantContextId(String participantContextId) {
        this.participantContextId = participantContextId;
    }

    public String getDataspaceContext() {
        return dataspaceContext;
    }

    @NotNull
    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @NotNull
    @Override
    public String getCounterPartyId() {
        return counterPartyId;
    }

    /**
     * Returns the {@link Policy} associated with the Dataset Request. Currently, this is an empty policy and serves as
     * placeholder.
     *
     * @return the stub {@link Policy}.
     */
    public Policy getPolicy() {
        return policy;
    }

    public static class Builder {
        private final DatasetRequestMessage message;

        private Builder() {
            message = new DatasetRequestMessage();
        }

        @JsonCreator
        public static DatasetRequestMessage.Builder newInstance() {
            return new DatasetRequestMessage.Builder();
        }

        public Builder datasetId(String datasetId) {
            this.message.datasetId = datasetId;
            return this;
        }

        public DatasetRequestMessage.Builder protocol(String protocol) {
            this.message.protocol = protocol;
            return this;
        }

        public DatasetRequestMessage.Builder dataspaceContext(String dataspaceContext) {
            this.message.dataspaceContext = dataspaceContext;
            return this;
        }

        public DatasetRequestMessage.Builder participantContextId(String participantContextId) {
            this.message.participantContextId = participantContextId;
            return this;
        }

        public DatasetRequestMessage.Builder counterPartyAddress(String callbackAddress) {
            this.message.counterPartyAddress = callbackAddress;
            return this;
        }

        public DatasetRequestMessage.Builder counterPartyId(String counterPartyId) {
            this.message.counterPartyId = counterPartyId;
            return this;
        }

        public DatasetRequestMessage build() {
            Objects.requireNonNull(message.protocol, "protocol");

            return message;
        }

    }
}
