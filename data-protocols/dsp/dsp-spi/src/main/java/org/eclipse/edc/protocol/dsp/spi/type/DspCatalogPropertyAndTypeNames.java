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

package org.eclipse.edc.protocol.dsp.spi.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

/**
 * Dataspace protocol types and attributes for catalog request.
 */
public interface DspCatalogPropertyAndTypeNames {

    String DSPACE_SIMPLE_TYPE_CATALOG_REQUEST_MESSAGE = "CatalogRequestMessage";
    String DSPACE_TYPE_CATALOG_REQUEST_MESSAGE = DSPACE_SCHEMA + DSPACE_SIMPLE_TYPE_CATALOG_REQUEST_MESSAGE;

    String DSPACE_TYPE_CATALOG_ERROR = DSPACE_SCHEMA + "CatalogError";
    String DSPACE_PROPERTY_FILTER = DSPACE_SCHEMA + "filter";

}
