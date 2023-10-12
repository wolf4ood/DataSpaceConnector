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

package org.eclipse.edc.iam.identitytrust.sts.embedded;

import org.eclipse.edc.identitytrust.SecureTokenService;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.AUDIENCE;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.ISSUER;
import static org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames.SUBJECT;
import static org.eclipse.edc.spi.result.Result.failure;
import static org.eclipse.edc.spi.result.Result.success;

/**
 * Implementation of a {@link SecureTokenService}, that is capable of creating a self-signed ID token ("SI token") completely in-process.
 * To that end, it makes use of the <a href="https://connect2id.com/products/nimbus-jose-jwt">Nimbus JOSE/JWT library</a>.<br/>
 * As a recommendation, the private key it uses should not be used for anything else.
 */
public class EmbeddedSecureTokenService implements SecureTokenService {

    public static final String SCOPE_CLAIM = "scope";
    public static final String ACCESS_TOKEN_CLAIM = "access_token";
    private static final List<String> ACCESS_TOKEN_INHERITED_CLAIMS = List.of(ISSUER);
    private final TokenGenerationService tokenGenerationService;
    private final Clock clock;
    private final long validity;

    public EmbeddedSecureTokenService(TokenGenerationService tokenGenerationService, Clock clock, long validity) {
        this.tokenGenerationService = tokenGenerationService;
        this.clock = clock;
        this.validity = validity;
    }

    @Override
    public Result<TokenRepresentation> createToken(Map<String, Object> claims, @Nullable String bearerAccessScope) {
        var selfIssuedClaims = new HashMap<>(claims);
        return ofNullable(bearerAccessScope)
                .map(scope -> createAndAcceptAccessToken(claims, scope, selfIssuedClaims::put))
                .orElse(success())
                .compose(v -> tokenGenerationService.generate(new SelfIssuedTokenDecorator(selfIssuedClaims, clock, validity)));
    }

    private Result<Void> createAndAcceptAccessToken(Map<String, Object> claims, String scope, BiConsumer<String, String> consumer) {
        return createAccessToken(claims, scope)
                .compose(tokenRepresentation -> success(tokenRepresentation.getToken()))
                .onSuccess(withClaim(ACCESS_TOKEN_CLAIM, consumer))
                .mapTo();
    }

    private Result<TokenRepresentation> createAccessToken(Map<String, Object> claims, String bearerAccessScope) {
        var accessTokenClaims = new HashMap<>(accessTokenInheritedClaims(claims));
        accessTokenClaims.put(SCOPE_CLAIM, bearerAccessScope);
        return addClaim(claims, ISSUER, withClaim(AUDIENCE, accessTokenClaims::put))
                .compose(v -> addClaim(claims, AUDIENCE, withClaim(SUBJECT, accessTokenClaims::put)))
                .compose(v -> tokenGenerationService.generate(new SelfIssuedTokenDecorator(accessTokenClaims, clock, validity)));

    }

    private Result<Void> addClaim(Map<String, Object> claims, String claim, Consumer<String> consumer) {
        return ofNullable(claims.get(claim))
                .map(String.class::cast)
                .map(Result::success)
                .orElse(failure(format("Missing %s in the input claims", claim)))
                .onSuccess(consumer)
                .mapTo();
    }

    private Consumer<String> withClaim(String key, BiConsumer<String, String> consumer) {
        return (value) -> consumer.accept(key, value);
    }

    private Map<String, Object> accessTokenInheritedClaims(Map<String, Object> claims) {
        return claims.entrySet().stream()
                .filter(entry -> ACCESS_TOKEN_INHERITED_CLAIMS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
