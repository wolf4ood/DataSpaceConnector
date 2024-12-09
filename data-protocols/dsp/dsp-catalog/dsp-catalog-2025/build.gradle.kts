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


plugins {
    `java-library`
}

dependencies {
    api(project(":data-protocols:dsp:dsp-catalog:dsp-catalog-2025:dsp-catalog-http-api-2025"))
    api(project(":data-protocols:dsp:dsp-catalog:dsp-catalog-2025:dsp-catalog-transform-2025"))
}
