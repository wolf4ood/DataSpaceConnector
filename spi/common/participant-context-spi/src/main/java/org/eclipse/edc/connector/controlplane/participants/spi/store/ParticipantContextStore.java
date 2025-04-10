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

package org.eclipse.edc.connector.controlplane.participants.spi.store;

import org.eclipse.edc.spi.entity.ParticipantContext;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;

public interface ParticipantContextStore {


    /**
     * Queries the store for ParticipantContexts based on the given query specification.
     *
     * @param querySpec The {@link QuerySpec} indicating the criteria for the query.
     * @return A {@link StoreResult} object containing a list of {@link ParticipantContext} objects that match the query. If none are found, returns an empty stream.
     */
    Collection<ParticipantContext> query(QuerySpec querySpec);

    StoreResult<Void> create(ParticipantContext participantContext);

    default ParticipantContext findById(String id) {
        return query(QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("id", "=", id))
                .build())
                .stream().findFirst()
                .orElse(null);
    }
}
