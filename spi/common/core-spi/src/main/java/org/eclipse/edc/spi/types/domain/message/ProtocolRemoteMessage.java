/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.types.domain.message;

import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.DEFAULT_DATASPACE_CONTEXT;

public abstract class ProtocolRemoteMessage implements RemoteMessage {

    protected String protocol = "unknown";

    protected String dataspaceContext = DEFAULT_DATASPACE_CONTEXT;
    protected String participantContextId;

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        Objects.requireNonNull(protocol);
        this.protocol = protocol;
    }

    public String getDataspaceContext() {
        return dataspaceContext;
    }

    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }
}
