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

import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

public interface ParticipantResource {

    static QuerySpec.Builder queryByParticipantContextId(String participantContextId) {
        return QuerySpec.Builder.newInstance().filter(filterByParticipantContextId(participantContextId));
    }

    static Criterion filterByParticipantContextId(String participantContextId) {
        return new Criterion("participantContextId", "=", participantContextId);
    }
    
    String getParticipantContextId();

    String getDataspaceContext();

}
