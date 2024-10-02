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

package org.eclipse.edc.protocol.dsp.http.api.configuration;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.transform.edc.from.JsonObjectFromAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.edc.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.connector.controlplane.transform.odrl.OdrlTransformersFactory;
import org.eclipse.edc.connector.controlplane.transform.odrl.from.JsonObjectFromPolicyTransformer;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.agent.ParticipantIdMapper;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.dspace.from.JsonObjectFromDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.dspace.to.JsonObjectToDataAddressDspaceTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.from.JsonObjectFromQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToCriterionTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonObjectToQuerySpecTransformer;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.web.jersey.providers.jsonld.JerseyJsonLdInterceptor;
import org.eclipse.edc.web.jersey.providers.jsonld.ObjectMapperProvider;
import org.eclipse.edc.web.spi.WebServer;
import org.eclipse.edc.web.spi.WebService;
import org.eclipse.edc.web.spi.configuration.ApiContext;
import org.eclipse.edc.web.spi.configuration.WebServiceConfigurer;
import org.eclipse.edc.web.spi.configuration.WebServiceSettings;

import java.util.Map;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.VOCAB;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_DSPACE_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_PREFIX;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

/**
 * Configure 'protocol' api context.
 */
@Extension(value = DspApiConfigurationExtension.NAME)
@Provides(ProtocolWebhook.class)
public class DspApiConfigurationExtension implements ServiceExtension {

    public static final String NAME = "Dataspace Protocol API Configuration Extension";

    @Setting(value = "Configures endpoint for reaching the Protocol API.", defaultValue = "<hostname:protocol.port/protocol.path>")
    public static final String DSP_CALLBACK_ADDRESS = "edc.dsp.callback.address";

    private static final boolean DEFAULT_DSP_API_ENABLE_CONTEXT = false;

    @Setting(value = "If set enable the usage of dsp api JSON-LD context.", defaultValue = "" + DEFAULT_DSP_API_ENABLE_CONTEXT)
    private static final String DSP_API_ENABLE_CONTEXT = "edc.dsp.context.enabled";

    @SettingContext("Protocol API context setting key")
    private static final String PROTOCOL_CONFIG_KEY = "web.http." + ApiContext.PROTOCOL;

    public static final WebServiceSettings SETTINGS = WebServiceSettings.Builder.newInstance()
            .apiConfigKey(PROTOCOL_CONFIG_KEY)
            .contextAlias(ApiContext.PROTOCOL)
            .defaultPath("/api/v1/dsp")
            .defaultPort(8282)
            .name("Protocol API")
            .build();

    @Inject
    private TypeManager typeManager;
    @Inject
    private WebService webService;
    @Inject
    private WebServer webServer;
    @Inject
    private WebServiceConfigurer configurator;
    @Inject
    private JsonLd jsonLd;
    @Inject
    private TypeTransformerRegistry transformerRegistry;
    @Inject
    private ParticipantIdMapper participantIdMapper;
    @Inject
    private Hostname hostname;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var contextConfig = context.getConfig(PROTOCOL_CONFIG_KEY);
        var apiConfiguration = configurator.configure(contextConfig, webServer, SETTINGS);
        var dspWebhookAddress = context.getSetting(DSP_CALLBACK_ADDRESS, format("http://%s:%s%s", hostname.get(), apiConfiguration.getPort(), apiConfiguration.getPath()));
        context.registerService(ProtocolWebhook.class, () -> dspWebhookAddress);

        var jsonLdMapper = typeManager.getMapper(JSON_LD);


        var isDspContextEnabled = context.getSetting(DSP_API_ENABLE_CONTEXT, DEFAULT_DSP_API_ENABLE_CONTEXT);

        if (isDspContextEnabled) {
            jsonLd.registerContext(DSPACE_CONTEXT, DSP_SCOPE);
            jsonLd.registerContext(EDC_DSPACE_CONTEXT, DSP_SCOPE);
        } else {
            // registers ns for DSP scope
            jsonLd.registerNamespace(DCAT_PREFIX, DCAT_SCHEMA, DSP_SCOPE);
            jsonLd.registerNamespace(DCT_PREFIX, DCT_SCHEMA, DSP_SCOPE);
            jsonLd.registerNamespace(ODRL_PREFIX, ODRL_SCHEMA, DSP_SCOPE);
            jsonLd.registerNamespace(DSPACE_PREFIX, DSPACE_SCHEMA, DSP_SCOPE);
            jsonLd.registerNamespace(VOCAB, EDC_NAMESPACE, DSP_SCOPE);
            jsonLd.registerNamespace(EDC_PREFIX, EDC_NAMESPACE, DSP_SCOPE);
        }

        webService.registerResource(ApiContext.PROTOCOL, new ObjectMapperProvider(jsonLdMapper));
        webService.registerResource(ApiContext.PROTOCOL, new JerseyJsonLdInterceptor(jsonLd, jsonLdMapper, DSP_SCOPE));

        registerTransformers();
    }

    private void registerTransformers() {
        var mapper = typeManager.getMapper(JSON_LD);
        mapper.registerSubtypes(AtomicConstraint.class, LiteralExpression.class);

        var jsonBuilderFactory = Json.createBuilderFactory(Map.of());

        // EDC model to JSON-LD transformers
        var dspApiTransformerRegistry = transformerRegistry.forContext("dsp-api");
        dspApiTransformerRegistry.register(new JsonObjectFromPolicyTransformer(jsonBuilderFactory, participantIdMapper));
        dspApiTransformerRegistry.register(new JsonObjectFromAssetTransformer(jsonBuilderFactory, mapper));
        dspApiTransformerRegistry.register(new JsonObjectFromDataAddressDspaceTransformer(jsonBuilderFactory, mapper));
        dspApiTransformerRegistry.register(new JsonObjectFromQuerySpecTransformer(jsonBuilderFactory));
        dspApiTransformerRegistry.register(new JsonObjectFromCriterionTransformer(jsonBuilderFactory, mapper));

        // JSON-LD to EDC model transformers
        // ODRL Transformers
        OdrlTransformersFactory.jsonObjectToOdrlTransformers(participantIdMapper).forEach(dspApiTransformerRegistry::register);

        dspApiTransformerRegistry.register(new JsonValueToGenericTypeTransformer(mapper));
        dspApiTransformerRegistry.register(new JsonObjectToAssetTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToQuerySpecTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToCriterionTransformer());
        dspApiTransformerRegistry.register(new JsonObjectToDataAddressDspaceTransformer());
    }

}
