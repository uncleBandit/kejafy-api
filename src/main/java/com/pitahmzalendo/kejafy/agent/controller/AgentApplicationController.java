package com.pitahmzalendo.kejafy.agent.controller;

import com.pitahmzalendo.kejafy.agent.dto.request.AgentApplicationRequest;
import com.pitahmzalendo.kejafy.agent.dto.response.AgentApplicationResponse;
import com.pitahmzalendo.kejafy.agent.service.AgentApplicationService;
import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * How a CUSTOMER becomes an AGENT: submit ID/KYC documents here, then wait
 * for admin review (see AdminAgentApplicationController). Approval flips
 * the user's role to AGENT server-side — there's no client-settable
 * "become an agent" toggle anywhere.
 */
@RestController
@RequestMapping("/api/v1/agent-applications")
@Tag(name = "Agent Applications", description = "KYC verification to become an agent")
@SecurityRequirement(name = "sessionCookie")
public class AgentApplicationController {

    private final AgentApplicationService agentApplicationService;

    public AgentApplicationController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    @PostMapping
    @Operation(summary = "Apply to become an agent (submit KYC documents)")
    @ApiResponse(responseCode = "201", description = "Application submitted, pending admin review")
    @ApiResponse(responseCode = "400", description = "Already an agent, or already has a pending application")
    public ResponseEntity<AgentApplicationResponse> apply(
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody AgentApplicationRequest request
    ) {
        var application = agentApplicationService.apply(principal.getDomainUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AgentApplicationResponse.from(application));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the caller's most recent agent application and its status")
    @ApiResponse(responseCode = "200", description = "The caller's latest application")
    @ApiResponse(responseCode = "404", description = "No application on file")
    public ResponseEntity<AgentApplicationResponse> getMine(
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        var application = agentApplicationService.getMyLatestApplication(principal.getDomainUser());
        return ResponseEntity.ok(AgentApplicationResponse.from(application));
    }
}
