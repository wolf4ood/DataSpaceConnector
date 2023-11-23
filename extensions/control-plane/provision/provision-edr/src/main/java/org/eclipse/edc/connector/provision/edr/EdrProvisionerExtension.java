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

package org.eclipse.edc.connector.provision.edr;

import org.eclipse.edc.connector.transfer.spi.edr.EdrGeneratorManager;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;

/**
 * The HTTP Provisioner extension delegates to HTTP endpoints to perform provision operations.
 */
@Extension(value = EdrProvisionerExtension.NAME)
public class EdrProvisionerExtension implements ServiceExtension {

    public static final String NAME = "HTTP Provisioning";
    @Inject
    protected ProvisionManager provisionManager;
    @Inject
    protected PolicyEngine policyEngine;

    @Inject
    private ResourceManifestGenerator manifestGenerator;

    @Inject
    private TypeManager typeManager;

    @Inject
    private DataAddressValidatorRegistry dataAddressValidatorRegistry;

    @Inject
    private EdrGeneratorManager edrGeneratorManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        manifestGenerator.registerGenerator(new EdrProvisionerResourceDefinitionGenerator());
        var edrProvisioner = new EdrProvisioner(edrGeneratorManager);
        provisionManager.register(edrProvisioner);
    }


}
