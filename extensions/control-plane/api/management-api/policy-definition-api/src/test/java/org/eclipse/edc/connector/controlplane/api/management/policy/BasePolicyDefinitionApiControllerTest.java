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

package org.eclipse.edc.connector.controlplane.api.management.policy;

import io.restassured.specification.RequestSpecification;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.edc.api.model.IdResponse;
import org.eclipse.edc.connector.controlplane.participants.spi.ParticipantContextSupplier;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.policydefinition.PolicyDefinitionService;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.entity.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.web.jersey.testfixtures.RestControllerTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.restassured.http.ContentType.JSON;
import static org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition.EDC_POLICY_DEFINITION_TYPE;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.CONTEXT;
import static org.eclipse.edc.jsonld.spi.JsonLdKeywords.TYPE;
import static org.eclipse.edc.spi.query.QuerySpec.EDC_QUERY_SPEC_TYPE;
import static org.eclipse.edc.validator.spi.Violation.violation;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ApiTest
public abstract class BasePolicyDefinitionApiControllerTest extends RestControllerTestBase {

    protected final TypeTransformerRegistry transformerRegistry = mock();
    protected final PolicyDefinitionService service = mock();
    protected final JsonObjectValidatorRegistry validatorRegistry = mock();

    protected final ParticipantContextSupplier participantContextSupplier = () -> new ParticipantContext("participantContextId", "participantContextId");

    @Test
    void create_shouldReturnDefinitionId() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        var policyDefinition = createPolicyDefinition().id("policyDefinitionId").createdAt(1234).build();
        var response = Json.createObjectBuilder()
                .add("id", policyDefinition.getId())
                .add("createdAt", policyDefinition.getCreatedAt())
                .build();

        when(transformerRegistry.transform(any(), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
        when(service.create(any())).thenReturn(ServiceResult.success(policyDefinition));
        when(transformerRegistry.transform(any(IdResponse.class), eq(JsonObject.class))).thenReturn(Result.success(response));

        var requestBody = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "context")
                        .add(TYPE, "Set")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("policyDefinitionId"))
                .body("createdAt", is(1234));

        verify(validatorRegistry).validate(eq(EDC_POLICY_DEFINITION_TYPE), any());
        verify(transformerRegistry).transform(isA(JsonObject.class), eq(PolicyDefinition.class));
        verify(service).create(policyDefinition);
    }

    @Test
    void create_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("failure", "failure path")));
        var requestBody = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "context")
                        .add(TYPE, "Set")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(400)
                .contentType(JSON);

        verifyNoInteractions(transformerRegistry, service);
    }

    @Test
    void create_shouldReturnBadRequest_whenTransformationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "context")
                        .add(TYPE, "Set")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(400)
                .contentType(JSON);
        verifyNoInteractions(service);
    }

    @Test
    void create_shouldReturnConflict_whenItAlreadyExists() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        var policyDefinition = createPolicyDefinition().id("policyDefinitionId").createdAt(1234).build();
        when(transformerRegistry.transform(any(), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
        when(service.create(any())).thenReturn(ServiceResult.conflict("already exists"));
        var requestBody = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "context")
                        .add(TYPE, "Set")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post()
                .then()
                .statusCode(409)
                .contentType(JSON);
    }

    @Test
    void delete_shouldCallService() {
        var policyDefinition = createPolicyDefinition().build();
        when(service.deleteById(any())).thenReturn(ServiceResult.success(policyDefinition));

        baseRequest()
                .delete("/id")
                .then()
                .statusCode(204);

        verify(service).deleteById("id");
    }

    @Test
    void delete_shouldReturnNotFound_whenNotFound() {
        when(service.deleteById(any())).thenReturn(ServiceResult.notFound("not found"));

        baseRequest()
                .delete("/id")
                .then()
                .statusCode(404);
    }

    @Test
    void update_shouldCallService() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        var policyDefinition = createPolicyDefinition().build();
        when(transformerRegistry.transform(any(), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
        when(service.update(any())).thenReturn(ServiceResult.success(policyDefinition));
        var requestBody = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "context")
                        .add(TYPE, "Set")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .put("/id")
                .then()
                .statusCode(204);
        verify(validatorRegistry).validate(eq(EDC_POLICY_DEFINITION_TYPE), any());
        verify(transformerRegistry).transform(isA(JsonObject.class), eq(PolicyDefinition.class));
        verify(service).update(policyDefinition);
    }

    @Test
    void update_shouldReturnBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("failure", "failure path")));
        var requestBody = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "context")
                        .add(TYPE, "Set")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .put("/id")
                .then()
                .statusCode(400);
        verifyNoInteractions(transformerRegistry, service);
    }

    @Test
    void update_shouldReturnBadRequest_whenTransformationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "context")
                        .add(TYPE, "Set")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .put("/id")
                .then()
                .statusCode(400);
        verifyNoInteractions(service);
    }

    @Test
    void update_shouldReturnNotFound_whenNotFound() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        var policyDefinition = createPolicyDefinition().build();
        when(transformerRegistry.transform(any(), eq(PolicyDefinition.class))).thenReturn(Result.success(policyDefinition));
        when(service.update(any())).thenReturn(ServiceResult.notFound("not found"));
        var requestBody = Json.createObjectBuilder()
                .add("policy", Json.createObjectBuilder()
                        .add(CONTEXT, "context")
                        .add(TYPE, "Set")
                        .build())
                .build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .put("/id")
                .then()
                .statusCode(404);
    }

    @Test
    void get_shouldReturnPolicyDefinition() {
        var policyDefinition = createPolicyDefinition().build();
        var expandedBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
        when(service.findById(any())).thenReturn(policyDefinition);
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedBody));

        baseRequest()
                .get("/id")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("id", is("id"))
                .body("createdAt", is(1234));
        verify(service).findById("id");
        verify(transformerRegistry).transform(policyDefinition, JsonObject.class);
    }

    @Test
    void get_shouldReturnNotFound_whenNotFound() {
        when(service.findById(any())).thenReturn(null);

        baseRequest()
                .get("/id")
                .then()
                .statusCode(404)
                .contentType(JSON);
        verifyNoInteractions(transformerRegistry);
    }

    @Test
    void get_shouldReturnNotFound_whenTransformFails() {
        when(service.findById(any())).thenReturn(createPolicyDefinition().build());
        when(transformerRegistry.transform(any(), any())).thenReturn(Result.failure("error"));

        baseRequest()
                .get("/id")
                .then()
                .statusCode(404)
                .contentType(JSON);
    }

    @Test
    void search_shouldReturnQueriedPolicyDefinitions() {
        var querySpec = QuerySpec.none();
        var policyDefinition = createPolicyDefinition().id("id").build();
        var expandedResponseBody = Json.createObjectBuilder().add("id", "id").add("createdAt", 1234).build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(policyDefinition)));
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.success(expandedResponseBody));
        var requestBody = Json.createObjectBuilder().build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(1))
                .body("[0].id", is("id"))
                .body("[0].createdAt", is(1234));

        verify(validatorRegistry).validate(eq(EDC_QUERY_SPEC_TYPE), any());
        verify(transformerRegistry).transform(isA(JsonObject.class), eq(QuerySpec.class));
        verify(service).search(querySpec);
        verify(transformerRegistry).transform(policyDefinition, JsonObject.class);
    }

    @Test
    void search_shouldBadRequest_whenValidationFails() {
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.failure(violation("failure", "failure path")));
        var requestBody = Json.createObjectBuilder().build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(400)
                .contentType(JSON);

        verifyNoInteractions(transformerRegistry, service);
    }

    @Test
    void search_shouldReturn400_whenInvalidQuery() {
        var requestBody = Json.createObjectBuilder()
                .add("offset", -1)
                .build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(JsonObject.class), eq(QuerySpec.class))).thenReturn(Result.failure("failure"));


        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(400);

        verify(transformerRegistry).transform(any(JsonObject.class), eq(QuerySpec.class));
        verifyNoInteractions(service);
        verifyNoMoreInteractions(transformerRegistry);
    }

    @Test
    void search_shouldReturnBadRequest_whenQuerySpecTransformFails() {
        var requestBody = Json.createObjectBuilder().build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.failure("error"));

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(400)
                .contentType(JSON);
        verifyNoInteractions(service);
    }

    @Test
    void search_shouldReturnBadRequest_whenServiceReturnsBadRequest() {
        var querySpec = QuerySpec.none();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
        when(service.search(any())).thenReturn(ServiceResult.badRequest("error"));
        var requestBody = Json.createObjectBuilder().build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(400)
                .contentType(JSON);
    }

    @Test
    void search_shouldFilterOutResults_whenTransformFails() {
        var querySpec = QuerySpec.none();
        var policyDefinition = createPolicyDefinition().id("id").build();
        when(validatorRegistry.validate(any(), any())).thenReturn(ValidationResult.success());
        when(transformerRegistry.transform(any(), eq(QuerySpec.class))).thenReturn(Result.success(querySpec));
        when(service.search(any())).thenReturn(ServiceResult.success(List.of(policyDefinition)));
        when(transformerRegistry.transform(any(), eq(JsonObject.class))).thenReturn(Result.failure("error"));
        var requestBody = Json.createObjectBuilder().build();

        baseRequest()
                .body(requestBody)
                .contentType(JSON)
                .post("/request")
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("size()", is(0));
    }

    protected abstract RequestSpecification baseRequest();

    @NotNull
    private PolicyDefinition.Builder createPolicyDefinition() {
        var policy = Policy.Builder.newInstance().build();

        return PolicyDefinition.Builder.newInstance()
                .id("policyDefinitionId")
                .createdAt(1234)
                .policy(policy);
    }
}
