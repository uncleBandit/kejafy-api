package com.pitahmzalendo.kejafy.agent.controller;


import com.pitahmzalendo.kejafy.agent.dto.request.AgentApplicationReviewRequest;
import com.pitahmzalendo.kejafy.agent.dto.response.AgentApplicationResponse;
import com.pitahmzalendo.kejafy.agent.service.AgentApplicationService;
import com.pitahmzalendo.kejafy.config.security.KejafyOidcUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/agent-applications")
@Tag(name = "Admin: Agent Applications", description = "Review queue for agent KYC applications")
@SecurityRequirement(name = "sessionCookie")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAgentApplicationController {

    private final AgentApplicationService agentApplicationService;

    public AdminAgentApplicationController(AgentApplicationService agentApplicationService) {
        this.agentApplicationService = agentApplicationService;
    }

    @GetMapping("/pending")
    @Operation(summary = "List pending agent applications, oldest first")
    public ResponseEntity<Page<AgentApplicationResponse>> getPending(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AgentApplicationResponse> page = agentApplicationService
                .getPendingApplications(pageable)
                .map(AgentApplicationResponse::from);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an application — promotes the applicant to AGENT")
    public ResponseEntity<AgentApplicationResponse> approve(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal
    ) {
        var application = agentApplicationService.approve(id, principal.getDomainUser());
        return ResponseEntity.ok(AgentApplicationResponse.from(application));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject an application, optionally with a reason shown to the applicant")
    public ResponseEntity<AgentApplicationResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal KejafyOidcUser principal,
            @Valid @RequestBody(required = false) AgentApplicationReviewRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        var application = agentApplicationService.reject(id, principal.getDomainUser(), reason);
        return ResponseEntity.ok(AgentApplicationResponse.from(application));
    }
}
