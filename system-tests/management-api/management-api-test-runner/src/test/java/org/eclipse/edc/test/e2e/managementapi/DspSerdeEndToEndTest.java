/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.test.e2e.managementapi;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreementVerificationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractNegotiationEventMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationTerminationMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractOfferMessage;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequestMessage;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersions;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferCompletionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferSuspensionMessage;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol.TransferTerminationMessage;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.protocol.dsp.spi.DspError;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_SCOPE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspNegotiationPropertyAndTypeNames.DSPACE_TYPE_CONTRACT_NEGOTIATION;
import static org.eclipse.edc.protocol.dsp.spi.type.DspPropertyAndTypeNames.DSPACE_PROPERTY_STATE;
import static org.eclipse.edc.protocol.dsp.spi.type.DspTransferProcessPropertyAndTypeNames.DSPACE_TYPE_TRANSFER_PROCESS;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.DSP_API_CONTEXT;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.catalogRequestObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.contractAgreementObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.contractAgreementVerificationObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.contractNegotiationEventObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.contractNegotiationTerminationObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.contractOfferObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.contractRequestObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.transferCompletionObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.transferRequestObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.transferStartObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.transferSuspensionObject;
import static org.eclipse.edc.test.e2e.managementapi.DspTestFunctions.transferTerminationObject;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.MANAGEMENT_API_CONTEXT;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.policyDefinitionObject;
import static org.eclipse.edc.util.io.Ports.getFreePort;

@EndToEndTest
public class DspSerdeEndToEndTest {

    @RegisterExtension
    private static final RuntimeExtension RUNTIME = new SerdeRuntime();


    @ParameterizedTest
    @ValueSource(strings = { "REQUESTED", "STARTED", "COMPLETED", "SUSPENDED", "TERMINATED" })
    void ser_TransferProcess(String state) {
        var tp = TransferProcess.Builder.newInstance()
                .type(TransferProcess.Type.PROVIDER)
                .state(TransferProcessStates.valueOf(state).code())
                .correlationId(UUID.randomUUID().toString())
                .build();
        var expanded = serialize(tp);

        assertThat(expanded.getString(TYPE)).isEqualTo(DSPACE_TYPE_TRANSFER_PROCESS);
        assertThat(expanded.getJsonObject(DSPACE_PROPERTY_STATE).getString(ID)).isEqualTo(DSPACE_SCHEMA + state);

        var result = compact(expanded);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo("TransferProcess");
        assertThat(result.getString("providerPid")).isEqualTo(tp.getId());
        assertThat(result.getString("consumerPid")).isEqualTo(tp.getCorrelationId());
        assertThat(result.getString("state")).isEqualTo(state);
    }

    @ParameterizedTest
    @ValueSource(strings = { "REQUESTED", "OFFERED", "ACCEPTED", "AGREED", "VERIFIED", "FINALIZED", "TERMINATED" })
    void ser_ContractNegotiation(String state) {
        var cn = ContractNegotiation.Builder.newInstance()
                .type(ContractNegotiation.Type.PROVIDER)
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol")
                .state(ContractNegotiationStates.valueOf(state).code())
                .correlationId(UUID.randomUUID().toString())
                .build();

        var expanded = serialize(cn);

        assertThat(expanded.getString(TYPE)).isEqualTo(DSPACE_TYPE_CONTRACT_NEGOTIATION);
        assertThat(expanded.getJsonObject(DSPACE_PROPERTY_STATE).getString(ID)).isEqualTo(DSPACE_SCHEMA + state);

        var result = compact(expanded);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo("ContractNegotiation");
        assertThat(result.getString("providerPid")).isEqualTo(cn.getId());
        assertThat(result.getString("consumerPid")).isEqualTo(cn.getCorrelationId());
        assertThat(result.getString("state")).isEqualTo(state);
    }

    @ParameterizedTest
    @ValueSource(strings = { "CatalogError", "TransferError", "ContractNegotiationError", "VersionsError" })
    void ser_Errors(String errorType) {

        var err = DspError.Builder.newInstance()
                .type(DSPACE_SCHEMA + errorType)
                .code("code")
                .messages(List.of("message1", "message2"))
                .build();

        var compacted = compact(err.toJson());

        assertThat(compacted).isNotNull();
        assertThat(compacted.getString(TYPE)).isEqualTo(errorType);
        assertThat(compacted.getString("code")).isEqualTo("code");
        assertThat(compacted.getJsonArray("reason")).contains(Json.createValue("message1"), Json.createValue("message2"));
    }

    @Test
    void ser_ProtocolVersion() {

        var versions = new ProtocolVersions(List.of(V_2024_1));

        var compacted = serializeAndCompact(versions);

        assertThat(compacted).isNotNull();
        assertThat(compacted.getJsonArray("protocolVersions")).first().satisfies(version -> {
            var versionObject = version.asJsonObject();
            assertThat(versionObject.getString("dspace:version")).isEqualTo(V_2024_1.version());
            assertThat(versionObject.getString("path")).isEqualTo(V_2024_1.path());
        });
    }

    @Test
    void ser_Catalog() {

        var policyDefJson = policyDefinitionObject();
        var offerId = UUID.randomUUID().toString();

        var initialPolicyJson = Json.createObjectBuilder(policyDefJson.getJsonObject("policy")).add(ID, offerId).build();
        var policyDefinition = deserialize(policyDefJson, PolicyDefinition.class);

        var dataService = DataService.Builder.newInstance().id(UUID.randomUUID().toString())
                .endpointDescription("connector")
                .endpointUrl("http://myconnector")
                .build();

        var distribution = Distribution.Builder.newInstance().format("HttpData-PULL")
                .dataService(dataService)
                .build();

        var dataset = Dataset.Builder.newInstance().id(UUID.randomUUID().toString())
                .offer(offerId, policyDefinition.getPolicy())
                .distribution(distribution).build();

        var catalog = Catalog.Builder.newInstance()
                .participantId("participantId")
                .dataService(dataService)
                .dataset(dataset)
                .build();
        var result = serializeAndCompact(catalog);

        assertThat(result).isNotNull();
        assertThat(result.getString(TYPE)).isEqualTo("Catalog");

        assertThat(result.getString("participantId")).isEqualTo(catalog.getParticipantId());
        assertThat(result.getJsonArray("distribution")).isEmpty();

        assertThat(result.getJsonArray("service")).hasSize(1)
                .first().satisfies(s -> {
                    var service = s.asJsonObject();
                    assertThat(service.getString(ID)).isEqualTo(dataService.getId());
                    assertThat(service.getString(TYPE)).isEqualTo("DataService");
                    assertThat(service.getString("endpointDescription")).isEqualTo(dataService.getEndpointDescription());
                    assertThat(service.getString("endpointURL")).isEqualTo(dataService.getEndpointUrl());
                });


        var datasets = result.getJsonArray("dataset");
        assertThat(datasets).hasSize(1);

        var jsonDataset = datasets.getJsonObject(0).asJsonObject();
        assertThat(jsonDataset.getString(ID)).isEqualTo(dataset.getId());
        assertThat(jsonDataset.getString(TYPE)).isEqualTo("Dataset");

        var distributions = jsonDataset.getJsonArray("distribution");
        assertThat(distributions).hasSize(1);

        var jsonDistribution = distributions.getJsonObject(0).asJsonObject();
        assertThat(jsonDistribution.getString("format")).isEqualTo(distribution.getFormat());
        assertThat(jsonDistribution.getJsonArray("accessService")).hasSize(1)
                .first().satisfies(s -> {
                    var service = s.asJsonObject();
                    assertThat(service.getString(ID)).isEqualTo(dataService.getId());
                    assertThat(service.getString(TYPE)).isEqualTo("DataService");
                    assertThat(service.getString("endpointDescription")).isEqualTo(dataService.getEndpointDescription());
                    assertThat(service.getString("endpointURL")).isEqualTo(dataService.getEndpointUrl());
                });


        var policies = jsonDataset.getJsonArray("hasPolicy");
        assertThat(policies).hasSize(1);

        var jsonPolicy = policies.getJsonObject(0).asJsonObject();

        assertThat(jsonPolicy).isEqualTo(initialPolicyJson);

    }

    /**
     * Tests for entities that supports transformation from/to JsonObject
     */
    @ParameterizedTest(name = "{1}")
    @ArgumentsSource(JsonInputProvider.class)
    void serde(JsonObject inputObject, Class<?> klass, Function<JsonObject, JsonObject> mapper) {
        var typeTransformerRegistry = RUNTIME.getService(TypeTransformerRegistry.class);
        var jsonLd = RUNTIME.getService(JsonLd.class);
        var registry = typeTransformerRegistry.forContext(DSP_API_CONTEXT);

        // Expand the input
        var expanded = jsonLd.expand(inputObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));

        // transform the expanded into the input klass type
        var result = registry.transform(expanded, klass).orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));

        // Compact the result
        var compacted = serializeAndCompact(result);

        // checks that the compacted == inputObject
        var mapped = Optional.ofNullable(mapper).map(m -> m.apply(compacted)).orElse(compacted);
        assertThat(mapped).isEqualTo(inputObject);
    }

    private JsonObject serializeAndCompact(Object object) {
        var result = serialize(object);
        return compact(result);
    }

    private JsonObject serialize(Object object) {
        var typeTransformerRegistry = RUNTIME.getService(TypeTransformerRegistry.class);
        var registry = typeTransformerRegistry.forContext(DSP_API_CONTEXT);
        return registry.transform(object, JsonObject.class).orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));
    }

    private JsonObject compact(JsonObject input) {
        var jsonLd = RUNTIME.getService(JsonLd.class);
        return jsonLd.compact(input, DSP_SCOPE).orElseThrow(failure -> new RuntimeException(failure.getFailureDetail()));
    }

    private <T> T deserialize(JsonObject inputObject, Class<T> klass) {
        var typeTransformerRegistry = RUNTIME.getService(TypeTransformerRegistry.class);
        var registry = typeTransformerRegistry.forContext(MANAGEMENT_API_CONTEXT);
        var jsonLd = RUNTIME.getService(JsonLd.class);

        var expanded = jsonLd.expand(inputObject).orElseThrow(f -> new AssertionError(f.getFailureDetail()));


        return registry.transform(expanded, klass).orElseThrow(failure -> new RuntimeException());
    }

    private static class JsonInputProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

            Function<JsonObject, JsonObject> mapper = this::removeId;

            return Stream.of(
                    Arguments.of(catalogRequestObject(), CatalogRequestMessage.class, null),
                    Arguments.of(transferRequestObject(), TransferRequestMessage.class, mapper),
                    Arguments.of(transferStartObject(), TransferStartMessage.class, mapper),
                    Arguments.of(transferCompletionObject(), TransferCompletionMessage.class, mapper),
                    Arguments.of(transferTerminationObject(), TransferTerminationMessage.class, mapper),
                    Arguments.of(transferSuspensionObject(), TransferSuspensionMessage.class, mapper),
                    Arguments.of(contractRequestObject(), ContractRequestMessage.class, mapper),
                    Arguments.of(contractOfferObject(), ContractOfferMessage.class, mapper),
                    Arguments.of(contractAgreementObject(), ContractAgreementMessage.class, mapper),
                    Arguments.of(contractAgreementVerificationObject(), ContractAgreementVerificationMessage.class, mapper),
                    Arguments.of(contractNegotiationEventObject("ACCEPTED"), ContractNegotiationEventMessage.class, mapper),
                    Arguments.of(contractNegotiationEventObject("FINALIZED"), ContractNegotiationEventMessage.class, mapper),
                    Arguments.of(contractNegotiationTerminationObject(), ContractNegotiationTerminationMessage.class, mapper)

            );
        }

        private JsonObject removeId(JsonObject compacted) {
            var newElement = Json.createObjectBuilder(compacted);
            newElement.remove("@id");
            return newElement.build();
        }

    }

    static class SerdeRuntime extends ManagementEndToEndExtension {

        protected SerdeRuntime() {
            super(context());
        }

        private static ManagementEndToEndTestContext context() {
            var managementPort = getFreePort();
            var protocolPort = getFreePort();

            var runtime = new EmbeddedRuntime(
                    "control-plane",
                    new HashMap<>() {
                        {
                            put("web.http.path", "/");
                            put("web.http.port", String.valueOf(getFreePort()));
                            put("web.http.protocol.path", "/protocol");
                            put("web.http.protocol.port", String.valueOf(protocolPort));
                            put("web.http.control.port", String.valueOf(getFreePort()));
                            put("edc.dsp.callback.address", "http://localhost:" + protocolPort + "/protocol");
                            put("web.http.management.path", "/management");
                            put("web.http.management.port", String.valueOf(managementPort));
                            put("edc.dsp.context.enabled", "true");
                            put("edc.dsp.management.enabled", "true");
                        }
                    },
                    ":system-tests:management-api:management-api-test-runtime"
            );

            return new ManagementEndToEndTestContext(runtime, managementPort, protocolPort);
        }

    }

}
