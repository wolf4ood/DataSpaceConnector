/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.spi;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;

import java.net.URI;

/**
 * Represents a Dataspace Profile Context: the binding of a dataspace identity (JSON-LD namespace +
 * context document) to a DSP protocol version and an identity-extraction strategy.
 *
 * <p>The {@link #id} is used as the profile segment in DSP HTTP paths in virtual mode
 * ({@code /{participantContextId}/{profileId}/...}) and as the suffix of the protocol string
 * ({@code dataspace-protocol-http:{profileId}}).
 *
 * @param id                   the profile identifier; appears in URL paths and protocol strings.
 * @param protocolVersion      the DSP protocol version this profile binds to.
 * @param webhook              the protocol endpoint URL.
 * @param idExtractionFunction extracts a participant id from a verified ClaimToken.
 * @param namespace            the JSON-LD namespace of the dataspace.
 * @param jsonLdContextUrl     URL of the JSON-LD context document used for compaction.
 */
public record DataspaceProfileContext(String id,
                                      ProtocolVersion protocolVersion,
                                      ProtocolWebhook webhook,
                                      ParticipantIdExtractionFunction idExtractionFunction,
                                      JsonLdNamespace namespace,
                                      URI jsonLdContextUrl) {

}
