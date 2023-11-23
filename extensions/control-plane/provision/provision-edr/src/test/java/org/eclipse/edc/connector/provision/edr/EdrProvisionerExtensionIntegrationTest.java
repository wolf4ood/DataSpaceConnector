/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.connector.provision.edr;

import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.validation.ContractValidationService;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.spi.transferprocess.TransferProcessProtocolService;
import org.eclipse.edc.connector.transfer.spi.edr.EdrGeneratorManager;
import org.eclipse.edc.connector.transfer.spi.edr.EdrResponse;
import org.eclipse.edc.connector.transfer.spi.edr.EdrToken;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.retry.TransferWaitStrategy;
import org.eclipse.edc.connector.transfer.spi.store.TransferProcessStore;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRequestMessage;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferStartMessage;
import org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.entity.StatefulEntity;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.iam.VerificationContext;
import org.eclipse.edc.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.edc.spi.protocol.ProtocolWebhook;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
import org.eclipse.edc.validator.spi.DataAddressValidatorRegistry;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.edc.connector.transfer.spi.types.TransferProcessStates.STARTED;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
@ExtendWith(EdcExtension.class)
public class EdrProvisionerExtensionIntegrationTest {
    private static final String ASSET_ID = "assetId";
    private static final String CONTRACT_ID = UUID.randomUUID().toString();
    private static final String POLICY_ID = "3";
    private final int dataPort = getFreePort();
    private final ContractValidationService contractValidationService = mock();
    private final IdentityService identityService = mock();

    private final EdrGeneratorManager generatorManager = mock();
    private final DataFlowController dataFlowController = mock();

    private final RemoteMessageDispatcherRegistry remoteMessageDispatcherRegistry = mock();

    @BeforeEach
    void setup(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.management.port", String.valueOf(dataPort),
                "web.http.management.path", "/api/v1/management"
        ));

        extension.registerServiceMock(TransferWaitStrategy.class, () -> 1);
        extension.registerServiceMock(ContractValidationService.class, contractValidationService);
        extension.registerServiceMock(ProtocolWebhook.class, mock(ProtocolWebhook.class));
        extension.registerServiceMock(IdentityService.class, identityService);
        extension.registerServiceMock(EdrGeneratorManager.class, generatorManager);
        extension.registerServiceMock(RemoteMessageDispatcherRegistry.class, remoteMessageDispatcherRegistry);
        var dataAddressValidatorRegistry = mock(DataAddressValidatorRegistry.class);
        when(dataAddressValidatorRegistry.validateSource(any())).thenReturn(ValidationResult.success());
        when(dataAddressValidatorRegistry.validateDestination(any())).thenReturn(ValidationResult.success());
        extension.registerServiceMock(DataAddressValidatorRegistry.class, dataAddressValidatorRegistry);

    }

    /**
     * Tests the case where an initial request returns a retryable failure and the second request completes.
     */
    @Test
    void processProvisioningEdr(TransferProcessProtocolService protocolService,
                                ContractNegotiationStore negotiationStore,
                                AssetIndex assetIndex,
                                TransferProcessStore store, PolicyDefinitionStore policyStore, DataFlowManager manager,
                                Vault vault,
                                TypeManager typeManager) {

        manager.register(dataFlowController);
        var edr = DataAddress.Builder.newInstance().type(EndpointDataReference.EDR_SIMPLE_TYPE).build();
        var dataAddress = DataAddress.Builder.newInstance()
                .type(HttpDataAddressSchema.HTTP_DATA_TYPE)
                .property(HttpDataAddressSchema.BASE_URL, "http://test")
                .build();

        var response = DataFlowResponse.Builder.newInstance().dataAddress(edr).build();
        var correlationId = UUID.randomUUID().toString();

        var token = new EdrToken("token");

        when(contractValidationService.validateAgreement(any(), any())).thenReturn(Result.success(null));
        when(generatorManager.generate(any(), any())).thenReturn(CompletableFuture.completedFuture(StatusResult.success(new EdrResponse(dataAddress, token))));
        when(dataFlowController.canHandle(argThat(tp -> tp.getDestinationType().equals("HttpProxy")))).thenReturn(true);
        when(dataFlowController.initiateFlow(argThat(tp -> tp.getDestinationType().equals("HttpProxy")), any())).thenReturn(StatusResult.success(response));
        when(remoteMessageDispatcherRegistry.dispatch(any(), any())).thenReturn(CompletableFuture.completedFuture(StatusResult.success(null)));

        negotiationStore.save(createContractNegotiation(correlationId));
        policyStore.create(createPolicyDefinition());
        assetIndex.create(createAssetEntry());

        when(identityService.verifyJwtToken(any(), isA(VerificationContext.class))).thenReturn(Result.success(ClaimToken.Builder.newInstance().build()));

        var result = protocolService.notifyRequested(createTransferRequestMessage(), TokenRepresentation.Builder.newInstance().build());

        assertThat(result).isSucceeded();
        await().untilAsserted(() -> {
            var transferProcess = store.findById(result.getContent().getId());
            assertThat(transferProcess).isNotNull()
                    .extracting(StatefulEntity::getState).isEqualTo(STARTED.code());
        });

        var message = ArgumentCaptor.forClass(TransferStartMessage.class);

        verify(remoteMessageDispatcherRegistry).dispatch(any(), message.capture());

        assertThat(message.getValue()).extracting(TransferStartMessage::getDataAddress)
                .usingRecursiveComparison()
                .isEqualTo(edr);

        var transferProcess = store.findById(result.getContent().getId());

        assertThat(transferProcess).isNotNull();


        assertThat(transferProcess).isNotNull().extracting(TransferProcess::getContentDataAddress)
                .satisfies(dad -> {
                    assertThat(dad.getProperties()).containsAllEntriesOf(dataAddress.getProperties());
                    var sk = vault.resolveSecret(dad.getKeyName());
                    var edrToken = typeManager.readValue(sk, EdrToken.class);
                    assertThat(token).usingRecursiveComparison().isEqualTo(edrToken);
                });
    }

    private PolicyDefinition createPolicyDefinition() {
        return PolicyDefinition.Builder.newInstance().policy(Policy.Builder.newInstance().build()).id(POLICY_ID).build();
    }

    private ContractNegotiation createContractNegotiation(String correlationId) {
        var policy = Policy.Builder.newInstance().build();
        var contractAgreement = ContractAgreement.Builder.newInstance()
                .assetId(ASSET_ID)
                .id(CONTRACT_ID)
                .policy(policy)
                .consumerId("consumer")
                .providerId("provider")
                .build();

        var contractOffer = ContractOffer.Builder.newInstance()
                .id(randomUUID().toString())
                .assetId(randomUUID().toString())
                .policy(policy)
                .build();
        return ContractNegotiation.Builder.newInstance()
                .id(randomUUID().toString())
                .type(ContractNegotiation.Type.PROVIDER)
                .correlationId(correlationId)
                .counterPartyId(randomUUID().toString())
                .counterPartyAddress("test")
                .protocol("test")
                .contractAgreement(contractAgreement)
                .contractOffer(contractOffer)
                .build();
    }

    @NotNull
    private Asset createAssetEntry() {
        return Asset.Builder.newInstance()
                .id(ASSET_ID)
                .dataAddress(DataAddress.Builder.newInstance().type("HttpData").build())
                .build();
    }

    private TransferRequestMessage createTransferRequestMessage() {
        return TransferRequestMessage.Builder.newInstance()
                .processId(randomUUID().toString())
                .dataDestination(DataAddress.Builder.newInstance().type("HttpProxy").build())
                .protocol("any")
                .counterPartyAddress("http://any")
                .callbackAddress("http://any")
                .contractId(CONTRACT_ID)
                .build();
    }

}
