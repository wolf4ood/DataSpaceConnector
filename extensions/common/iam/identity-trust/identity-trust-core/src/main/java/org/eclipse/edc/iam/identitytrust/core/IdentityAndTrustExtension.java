/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.core;

import org.eclipse.edc.iam.identitytrust.service.IdentityAndTrustService;
import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension("Identity And Trust Extension")
public class IdentityAndTrustExtension implements ServiceExtension {

    @Setting(value = "DID of this connector", required = true)
    public static final String ISSUER_DID_PROPERTY = "edc.iam.issuer.id";

    @Inject
    private SecureTokenService secureTokenService;

    @Provider
    public IdentityService createIdentityService(ServiceExtensionContext context) {
        return new IdentityAndTrustService(secureTokenService, getIssuerDid(context), context.getParticipantId());
    }

    private String getIssuerDid(ServiceExtensionContext context) {
        return context.getConfig().getString(ISSUER_DID_PROPERTY);
    }
}
