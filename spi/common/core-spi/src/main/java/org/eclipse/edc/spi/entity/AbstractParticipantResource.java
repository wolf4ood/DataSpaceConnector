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

import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.DEFAULT_DATASPACE_CONTEXT;

public abstract class AbstractParticipantResource extends Entity implements ParticipantResource {

    protected String participantContextId;
    protected String dataspaceContext = DEFAULT_DATASPACE_CONTEXT;


    @Override
    public String getParticipantContextId() {
        return participantContextId;
    }

    @Override
    public String getDataspaceContext() {
        return dataspaceContext;
    }


    protected abstract static class Builder<T extends AbstractParticipantResource, B extends AbstractParticipantResource.Builder<T, B>> extends Entity.Builder<T, B> {

        protected Builder(T entity) {
            super(entity);
        }

        public B participantContextId(String participantContextId) {
            entity.participantContextId = participantContextId;
            return self();
        }

        public B dataspaceContext(String dataspaceContext) {
            entity.dataspaceContext = dataspaceContext;
            return self();
        }

        @Override
        protected T build() {
            super.build();
            Objects.requireNonNull(entity.dataspaceContext);
            return entity;
        }
    }
}
