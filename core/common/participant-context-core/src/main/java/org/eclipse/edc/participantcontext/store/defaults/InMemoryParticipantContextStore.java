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

package org.eclipse.edc.participantcontext.store.defaults;

import org.eclipse.edc.connector.controlplane.participants.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.entity.ParticipantContext;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.store.ReflectionBasedQueryResolver;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryParticipantContextStore implements ParticipantContextStore {

    private final QueryResolver<ParticipantContext> participantContextQueryResolver;

    private final Map<String, ParticipantContext> participants = new ConcurrentHashMap<>();

    public InMemoryParticipantContextStore(CriterionOperatorRegistry criterionOperatorRegistry) {
        this.participantContextQueryResolver = new ReflectionBasedQueryResolver<>(ParticipantContext.class, criterionOperatorRegistry);
    }

    @Override
    public Collection<ParticipantContext> query(QuerySpec querySpec) {
        return participantContextQueryResolver.query(participants.values().stream(), querySpec)
                .collect(Collectors.toList());
    }

    @Override
    public StoreResult<Void> create(ParticipantContext participantContext) {
        participants.put(participantContext.id(), participantContext);
        return StoreResult.success();
    }
}
