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

package org.eclipse.edc.connector.dataplane.iam.service;

import org.eclipse.edc.connector.controlplane.participants.spi.ParticipantContextSupplier;
import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessControlService;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.spi.constants.CoreConstants.DEFAULT_DATASPACE_CONTEXT;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.result.Result.success;

public class DataPlaneAuthorizationServiceImpl implements DataPlaneAuthorizationService {
    public static final String PROPERTY_AGREEMENT_ID = "agreement_id";
    public static final String PROPERTY_ASSET_ID = "asset_id";
    public static final String PROPERTY_PROCESS_ID = "process_id";
    public static final String PROPERTY_FLOW_TYPE = "flow_type";
    private static final String PROPERTY_PARTICIPANT_ID = "participant_id";
    private final DataPlaneAccessTokenService accessTokenService;
    private final PublicEndpointGeneratorService endpointGenerator;
    private final DataPlaneAccessControlService accessControlService;
    private final ParticipantContextSupplier participantContextSupplier;
    private final Clock clock;

    public DataPlaneAuthorizationServiceImpl(DataPlaneAccessTokenService accessTokenService,
                                             PublicEndpointGeneratorService endpointGenerator,
                                             DataPlaneAccessControlService accessControlService,
                                             ParticipantContextSupplier participantContextSupplier,
                                             Clock clock) {
        this.accessTokenService = accessTokenService;
        this.endpointGenerator = endpointGenerator;
        this.accessControlService = accessControlService;
        this.participantContextSupplier = participantContextSupplier;
        this.clock = clock;
    }

    @Override
    public Result<DataAddress> createEndpointDataReference(DataFlowStartMessage message) {

        var additionalProperties = new HashMap<String, Object>(message.getProperties());
        additionalProperties.put(PROPERTY_AGREEMENT_ID, message.getAgreementId());
        additionalProperties.put(PROPERTY_ASSET_ID, message.getAssetId());
        additionalProperties.put(PROPERTY_PROCESS_ID, message.getProcessId());
        additionalProperties.put(PROPERTY_FLOW_TYPE, message.getFlowType().toString());
        additionalProperties.put(PROPERTY_PARTICIPANT_ID, message.getParticipantId());
        var tokenParams = createTokenParams(message);
        var sourceDataAddress = message.getSourceDataAddress();


        // create the "front-channel" data address
        var dataAddressBuilder = endpointGenerator.generateFor(message.getTransferType().destinationType(), sourceDataAddress)
                .compose(ep -> createSecureEndpoint(ep, tokenParams, additionalProperties, sourceDataAddress))
                .compose(se -> createDataAddress(se.tokenRepresentation(), se.endpoint()));

        // "decorate" the data address with response channel properties
        var responseChannelType = message.getTransferType().responseChannelType();
        if (responseChannelType != null) {
            var responseChannelTokenParams = createTokenParams(message);
            dataAddressBuilder = dataAddressBuilder.compose(builder -> endpointGenerator.generateResponseFor(responseChannelType)
                    .compose(ep -> createSecureEndpoint(ep, responseChannelTokenParams, additionalProperties, sourceDataAddress))
                    .compose(endpoint -> addResponseChannel(builder, endpoint.tokenRepresentation(), endpoint.endpoint())));
        }


        return dataAddressBuilder.map(DataAddress.Builder::build);
    }

    @Override
    public Result<DataAddress> authorize(String token, Map<String, Object> requestData) {
        return accessTokenService.resolve(token)
                .compose(atd -> accessControlService.checkAccess(atd.claimToken(), atd.dataAddress(), requestData, atd.additionalProperties())
                        .map(u -> atd.dataAddress())
                );
    }

    @Override
    public Result<Void> revokeEndpointDataReference(String transferProcessId, String reason) {
        return accessTokenService.revoke(transferProcessId, reason);
    }

    private Result<SecureEndpoint> createSecureEndpoint(Endpoint endpoint, TokenParameters tokenParameters, Map<String, Object> additionalProperties, DataAddress sourceDataAddress) {
        return accessTokenService.obtainToken(tokenParameters, sourceDataAddress, additionalProperties)
                .map(tr -> new SecureEndpoint(endpoint, tr));
    }

    private Result<DataAddress.Builder> createDataAddress(TokenRepresentation tokenRepresentation, Endpoint publicEndpoint) {
        var address = DataAddress.Builder.newInstance()
                .type(publicEndpoint.endpointType())
                .property(EDC_NAMESPACE + "endpoint", publicEndpoint.endpoint())
                .property(EDC_NAMESPACE + "endpointType", publicEndpoint.endpointType()) //this is duplicated in the type() field, but will make serialization easier
                .property(EDC_NAMESPACE + "authorization", tokenRepresentation.getToken())
                .properties(tokenRepresentation.getAdditional()); // would contain the "authType = bearer" entry

        return success(address);
    }

    private Result<DataAddress.Builder> addResponseChannel(DataAddress.Builder builder, TokenRepresentation tokenRepresentation, Endpoint returnChannelEndpoint) {
        var map = new HashMap<String, String>() {
            {
                put(EDC_NAMESPACE + "responseChannel-endpoint", returnChannelEndpoint.endpoint());
                put(EDC_NAMESPACE + "responseChannel-endpointType", returnChannelEndpoint.endpointType());
                put(EDC_NAMESPACE + "responseChannel-authorization", tokenRepresentation.getToken());
            }
        };
        tokenRepresentation.getAdditional().forEach((k, v) -> map.put(k.replace(EDC_NAMESPACE, EDC_NAMESPACE + "responseChannel-"), v.toString()));

        map.forEach(builder::property);

        return Result.success(builder);

    }

    private TokenParameters createTokenParams(DataFlowStartMessage message) {
        // TODO change signaling spec
        var ownParticipantId = participantContextSupplier.get().getIdentity(DEFAULT_DATASPACE_CONTEXT);
        return TokenParameters.Builder.newInstance()
                .claims(JwtRegisteredClaimNames.JWT_ID, UUID.randomUUID().toString())
                .claims(JwtRegisteredClaimNames.AUDIENCE, message.getParticipantId())
                .claims(JwtRegisteredClaimNames.ISSUER, ownParticipantId)
                .claims(JwtRegisteredClaimNames.SUBJECT, ownParticipantId)
                .claims(JwtRegisteredClaimNames.ISSUED_AT, clock.instant().toEpochMilli()) // todo: milli or second?
                .build();
    }

    private record SecureEndpoint(Endpoint endpoint, TokenRepresentation tokenRepresentation) {
    }
}
