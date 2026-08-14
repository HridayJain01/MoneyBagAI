package com.moneybags.identity.controller;

import com.moneybags.identity.api.ApiModels.ResolveRequest;
import com.moneybags.identity.api.ApiModels.SessionPrincipal;
import com.moneybags.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-trust surface. The gateway blocks /internal/** from the outside, so these
 * routes are only reachable inside the deployment.
 */
@RestController
@RequestMapping("/internal/v1/sessions")
@RequiredArgsConstructor
public class InternalSessionController {

    private final AuthService auth;

    /**
     * The gateway calls this on every authenticated request (behind a short-TTL cache)
     * and turns the result into downstream actor headers.
     *
     * <p>POST rather than GET so the session id is never captured in a URL or access log.
     */
    @Operation(summary = "Resolve an opaque session id to its principal")
    @PostMapping("/resolve")
    public SessionPrincipal resolve(@Valid @RequestBody ResolveRequest request) {
        return auth.resolve(request.sessionId());
    }
}
