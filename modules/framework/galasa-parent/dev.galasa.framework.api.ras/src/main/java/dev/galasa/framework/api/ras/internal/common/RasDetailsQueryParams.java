/*
 * Copyright contributors to the Galasa project
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package dev.galasa.framework.api.ras.internal.common;

import dev.galasa.framework.api.common.SupportedQueryParameterNames;

public class RasDetailsQueryParams {
    
    public static final String DETAILS_METHOD_QUERY_PARAM_VALUE = "methods";

    public static final SupportedQueryParameterNames SUPPORTED_DETAIL_QUERY_PARAMS = new SupportedQueryParameterNames(DETAILS_METHOD_QUERY_PARAM_VALUE);

    public RasDetailsQueryParams() {
        
    }

    public boolean isMethodDetailsExcluded(String detailParam) {

        boolean isMethodDetailsExcluded = true;
        if(detailParam != null && detailParam.equals(DETAILS_METHOD_QUERY_PARAM_VALUE)) {
            isMethodDetailsExcluded = false;
        }

        return isMethodDetailsExcluded;

    }

    public boolean isParamSupported(String queryParam) {
        return SUPPORTED_DETAIL_QUERY_PARAMS.isSupported(queryParam);
    }

}
