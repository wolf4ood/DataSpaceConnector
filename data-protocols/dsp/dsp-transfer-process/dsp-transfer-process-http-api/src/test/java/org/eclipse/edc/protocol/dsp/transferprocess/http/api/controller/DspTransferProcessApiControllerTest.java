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

package org.eclipse.edc.protocol.dsp.transferprocess.http.api.controller;

import org.eclipse.edc.jsonld.spi.JsonLdNamespace;
import org.eclipse.edc.junit.annotations.ApiTest;
import org.junit.jupiter.api.Nested;

import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_08;
import static org.eclipse.edc.protocol.dsp.spi.type.DspConstants.DSP_NAMESPACE_V_2024_1;
import static org.eclipse.edc.protocol.dsp.spi.version.DspVersions.V_2024_1_PATH;
import static org.eclipse.edc.protocol.dsp.transferprocess.http.api.TransferProcessApiPaths.BASE_PATH;

@ApiTest
class DspTransferProcessApiControllerTest {

    @ApiTest
    @Nested
    class DspTransferProcessApiControllerV08Test extends DspTransferProcessApiControllerBaseTest {

        @Override
        protected String basePath() {
            return BASE_PATH;
        }

        @Override
        protected JsonLdNamespace namespace() {
            return DSP_NAMESPACE_V_08;
        }

        @Override
        protected Object controller() {
            return new DspTransferProcessApiController(protocolService, dspRequestHandler, participantContextSupplier);
        }
    }

    @ApiTest
    @Nested
    class DspTransferProcessControllerV20241Test extends DspTransferProcessApiControllerBaseTest {

        @Override
        protected String basePath() {
            return V_2024_1_PATH + BASE_PATH;
        }

        @Override
        protected JsonLdNamespace namespace() {
            return DSP_NAMESPACE_V_2024_1;
        }

        @Override
        protected Object controller() {
            return new DspTransferProcessApiController20241(protocolService, dspRequestHandler, participantContextSupplier);
        }
    }
}
