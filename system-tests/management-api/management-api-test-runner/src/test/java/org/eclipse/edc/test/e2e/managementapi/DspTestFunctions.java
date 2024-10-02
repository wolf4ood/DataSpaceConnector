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

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import java.util.UUID;

import static jakarta.json.Json.createArrayBuilder;
import static jakarta.json.Json.createObjectBuilder;
import static java.time.Instant.ofEpochSecond;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.ID;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_CONTEXT;
import static org.eclipse.edc.protocol.dsp.spi.type.DspCatalogPropertyAndTypeNames.DSPACE_SIMPLE_TYPE_CATALOG_REQUEST_MESSAGE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_DSPACE_CONTEXT;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.embeddedQuerySpec;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.inForceDatePermission;
import static org.eclipse.edc.test.e2e.managementapi.TestFunctions.policy;

public class DspTestFunctions {

    public static final String DSP_API_CONTEXT = "dsp-api";

    public static JsonArrayBuilder createContextBuilder() {
        return createArrayBuilder()
                .add(DSPACE_CONTEXT)
                .add(EDC_DSPACE_CONTEXT);
    }

    public static JsonObject catalogRequestObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, DSPACE_SIMPLE_TYPE_CATALOG_REQUEST_MESSAGE)
                .add("filter", createArrayBuilder().add(embeddedQuerySpec()))
                .build();
    }

    public static JsonObject transferRequestObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "TransferRequestMessage")
                .add("agreementId", "agreementId")
                .add("consumerPid", "consumerPid")
                .add("callbackAddress", "callbackAddress")
                .add("format", "HttpData-PULL")
                .build();
    }

    public static JsonObject transferStartObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "TransferStartMessage")
                .add("consumerPid", "consumerPid")
                .add("providerPid", "consumerPid")
                .add("dataAddress", dataAddress())
                .build();
    }

    public static JsonObject transferCompletionObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "TransferCompletionMessage")
                .add("consumerPid", "consumerPid")
                .add("providerPid", "consumerPid")
                .build();
    }

    public static JsonObject transferTerminationObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "TransferTerminationMessage")
                .add("consumerPid", "consumerPid")
                .add("providerPid", "consumerPid")
                .add("reason", createArrayBuilder().add("reason"))
                .add("code", "code")
                .build();
    }

    public static JsonObject transferSuspensionObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "TransferSuspensionMessage")
                .add("consumerPid", "consumerPid")
                .add("providerPid", "consumerPid")
                .add("reason", createArrayBuilder().add("reason"))
                .add("code", "code")
                .build();
    }

    public static JsonObject contractRequestObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "ContractRequestMessage")
                .add("consumerPid", "consumerPid")
                .add("callbackAddress", "callbackAddress")
                .add("offer", inForceDatePolicy())
                .add("providerPid", "providerPid")
                .build();
    }

    public static JsonObject contractAgreementObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "ContractAgreementMessage")
                .add("consumerPid", "consumerPid")
                .add("agreement", inForceDateAgreement())
                .add("providerPid", "providerPid")
                .build();
    }

    public static JsonObject contractAgreementVerificationObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "ContractAgreementVerificationMessage")
                .add("consumerPid", "consumerPid")
                .add("providerPid", "providerPid")
                .build();
    }

    public static JsonObject contractNegotiationEventObject(String eventType) {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "ContractNegotiationEventMessage")
                .add("consumerPid", "consumerPid")
                .add("providerPid", "providerPid")
                .add("eventType", eventType)
                .build();
    }

    public static JsonObject contractNegotiationTerminationObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "ContractNegotiationTerminationMessage")
                .add("consumerPid", "consumerPid")
                .add("providerPid", "providerPid")
                .add("reason", createArrayBuilder().add("reason"))
                .add("code", "code")
                .build();
    }

    public static JsonObject contractOfferObject() {
        return createObjectBuilder()
                .add(CONTEXT, createContextBuilder().build())
                .add(TYPE, "ContractOfferMessage")
                .add("consumerPid", "consumerPid")
                .add("callbackAddress", "callbackAddress")
                .add("offer", inForceDatePolicy())
                .add("providerPid", "providerPid")
                .build();
    }

    public static JsonObject inForceDatePolicy() {
        return policy(inForceDatePermission("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s"), "Offer")
                .add(ID, UUID.randomUUID().toString())
                .build();
    }

    public static JsonObject inForceDateAgreement() {
        return policy(inForceDatePermission("gteq", "contractAgreement+0s", "lteq", "contractAgreement+10s"), "Agreement")
                .add(ID, UUID.randomUUID().toString())
                .add("timestamp", ofEpochSecond(System.currentTimeMillis()).toString())
                .add("assigner", "assigner")
                .add("assignee", "assignee")
                .build();
    }

    private static JsonObject dataAddress() {
        return createObjectBuilder().add(TYPE, "DataAddress")
                .add("endpointType", "https://w3id.org/idsa/v4.1/HTTP")
                .add("endpointProperties", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add(TYPE, "EndpointProperty")
                                .add("name", "authorization")
                                .add("value", "TOKEN-ABCDEFG"))
                        .add(createObjectBuilder()
                                .add(TYPE, "EndpointProperty")
                                .add("name", "authType")
                                .add("value", "bearer")))
                .build();
    }

}
