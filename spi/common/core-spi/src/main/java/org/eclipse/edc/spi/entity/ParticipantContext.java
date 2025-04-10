/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.spi.entity;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static org.eclipse.edc.spi.constants.CoreConstants.DEFAULT_DATASPACE_CONTEXT;

public record ParticipantContext(String id, Map<String, String> identities) {

    public ParticipantContext(String id, String participantId) {
        this(id, Map.of(DEFAULT_DATASPACE_CONTEXT, participantId));
    }

    @Nullable
    public String getIdentity(String dataspaceContext) {
        return identities.get(dataspaceContext);
    }
}
