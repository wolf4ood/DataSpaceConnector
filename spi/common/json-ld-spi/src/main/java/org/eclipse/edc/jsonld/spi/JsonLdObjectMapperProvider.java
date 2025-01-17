/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.jsonld.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Provides an {@link ObjectMapper} for JSON-LD processing.
 */
@FunctionalInterface
@ExtensionPoint
public interface JsonLdObjectMapperProvider {

    ObjectMapper get();

}
