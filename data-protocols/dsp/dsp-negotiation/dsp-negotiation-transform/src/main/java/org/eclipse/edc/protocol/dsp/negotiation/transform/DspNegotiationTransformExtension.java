/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.negotiation.transform;

import jakarta.json.Json;
import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractAgreementMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractAgreementVerificationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationErrorTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationEventMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractNegotiationTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractOfferMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.from.JsonObjectFromContractRequestMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractAgreementMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractAgreementVerificationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationAckTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationEventMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractNegotiationTerminationMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractOfferMessageTransformer;
import org.eclipse.edc.protocol.dsp.negotiation.transform.to.JsonObjectToContractRequestMessageTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.util.Map;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_TRANSFORMER_CONTEXT_V_2024_1;

/**
 * Provides the transformers for negotiation message types via the {@link TypeTransformerRegistry}.
 */
@Extension(value = DspNegotiationTransformExtension.NAME)
public class DspNegotiationTransformExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol Negotiation Transform Extension";

    @Inject
    private TypeTransformerRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registerTransformers(DSP_TRANSFORMER_CONTEXT_V_08, DSP_NAMESPACE_V_08);
        registerTransformers(DSP_TRANSFORMER_CONTEXT_V_2024_1, DSP_NAMESPACE_V_2024_1);
    }

    private void registerTransformers(String version, JsonLdNamespace namespace) {
        var builderFactory = Json.createBuilderFactory(Map.of());

        var dspApiTransformerRegistry = registry.forContext(version);
        dspApiTransformerRegistry.register(new JsonObjectFromContractAgreementMessageTransformer(builderFactory, namespace));
        dspApiTransformerRegistry.register(new JsonObjectFromContractAgreementVerificationMessageTransformer(builderFactory, namespace));
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationEventMessageTransformer(builderFactory, namespace));
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationTerminationMessageTransformer(builderFactory, namespace));
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationTransformer(builderFactory, namespace));
        dspApiTransformerRegistry.register(new JsonObjectFromContractRequestMessageTransformer(builderFactory, namespace));
        dspApiTransformerRegistry.register(new JsonObjectFromContractOfferMessageTransformer(builderFactory, namespace));
        dspApiTransformerRegistry.register(new JsonObjectFromContractNegotiationErrorTransformer(builderFactory, namespace));

        dspApiTransformerRegistry.register(new JsonObjectToContractAgreementMessageTransformer(namespace));
        dspApiTransformerRegistry.register(new JsonObjectToContractAgreementVerificationMessageTransformer(namespace));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationEventMessageTransformer(namespace));
        dspApiTransformerRegistry.register(new JsonObjectToContractRequestMessageTransformer(namespace));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationTerminationMessageTransformer(namespace));
        dspApiTransformerRegistry.register(new JsonObjectToContractOfferMessageTransformer(namespace));
        dspApiTransformerRegistry.register(new JsonObjectToContractNegotiationAckTransformer(namespace));
    }
}