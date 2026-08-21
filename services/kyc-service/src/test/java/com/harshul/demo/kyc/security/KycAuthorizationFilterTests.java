package com.harshul.demo.kyc.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class KycAuthorizationFilterTests {

    private final KycAuthorizationFilter filter = new KycAuthorizationFilter();

    @Test
    void tellerCanCreateAndSubmitKyc() throws Exception {
        MockHttpServletResponse response = filter("POST", "/api/v1/kyc/sessions/session-1/submit",
                "CUSTOMER_READ,CUSTOMER_UPDATE");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void tellerCannotApproveKyc() throws Exception {
        MockHttpServletResponse response = filter("POST", "/api/v1/kyc/sessions/session-1/approve",
                "CUSTOMER_UPDATE");
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("KYC_VERIFY");
    }

    @Test
    void checkerCanReviewAndApproveKyc() throws Exception {
        assertThat(filter("GET", "/api/v1/kyc/sessions/session-1/frames", "KYC_VERIFY").getStatus())
                .isEqualTo(200);
        assertThat(filter("POST", "/api/v1/kyc/sessions/session-1/approve", "KYC_VERIFY").getStatus())
                .isEqualTo(200);
    }

    private MockHttpServletResponse filter(String method, String path, String permissions) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("X-Permissions", permissions);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
