package com.pitahmzalendo.kejafy.agent.dto.response;


import com.pitahmzalendo.kejafy.agent.entity.AgentApplication;
import com.pitahmzalendo.kejafy.agent.entity.AgentApplicationStatus;

import java.time.LocalDateTime;

public record AgentApplicationResponse(
        Long id,
        Long applicantId,
        String applicantName,
        String idNumber,
        String idDocumentFrontUrl,
        String idDocumentBackUrl,
        String selfieUrl,
        AgentApplicationStatus status,
        String rejectionReason,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt
) {
    public static AgentApplicationResponse from(AgentApplication app) {
        return new AgentApplicationResponse(
                app.getId(),
                app.getApplicant().getId(),
                app.getApplicant().getFirstName() + " " + app.getApplicant().getLastName(),
                app.getIdNumber(),
                app.getIdDocumentFrontUrl(),
                app.getIdDocumentBackUrl(),
                app.getSelfieUrl(),
                app.getStatus(),
                app.getRejectionReason(),
                app.getSubmittedAt(),
                app.getReviewedAt()
        );
    }
}
