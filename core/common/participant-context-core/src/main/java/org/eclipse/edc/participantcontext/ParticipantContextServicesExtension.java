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

package org.eclipse.edc.participantcontext;

import org.eclipse.edc.connector.controlplane.participants.spi.ParticipantContextSupplier;
import org.eclipse.edc.connector.controlplane.participants.spi.store.ParticipantContextStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.entity.ParticipantContext;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.spi.system.ServiceExtensionContext.ANONYMOUS_PARTICIPANT;

@Extension(value = ParticipantContextServicesExtension.NAME)
public class ParticipantContextServicesExtension implements ServiceExtension {

    public static final String NAME = "Participant Context Default Services Extension";

    @Setting(description = "Configures the participant id this runtime is operating on behalf of", key = "edc.participant.id", defaultValue = ANONYMOUS_PARTICIPANT)
    public String participantId;

    @Inject
    private ParticipantContextStore participantContextStore;
    @Inject
    private TransactionContext transactionContext;

    @Inject
    private Monitor monitor;

    @Override
    public String name() {
        return NAME;
    }


    @Provider(isDefault = true)
    public ParticipantContextSupplier participantContextSupplier() {
        var participantContext = new ParticipantContext(participantId, participantId);
        return () -> participantContext;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (ANONYMOUS_PARTICIPANT.equals(participantId)) {
            monitor.warning("The runtime is configured as an anonymous participant. DO NOT DO THIS IN PRODUCTION.");
        }
    }

    @Override
    public void prepare() {

        transactionContext.execute(() -> {
            var participant = participantContextStore.findById(participantId);
            if (participant != null) {
                return;
            }
            var participantContext = new ParticipantContext(participantId, participantId);
            var result = this.participantContextStore.create(participantContext);
            if (result.failed()) {
                throw new IllegalStateException("Failed to create participant context: " + result.getFailureMessages());
            }
        });

    }

}
