package com.pitahmzalendo.kejafy.agent.service;

import com.pitahmzalendo.kejafy.agent.dto.request.AgentApplicationRequest;
import com.pitahmzalendo.kejafy.agent.entity.AgentApplication;
import com.pitahmzalendo.kejafy.agent.entity.AgentApplicationStatus;
import com.pitahmzalendo.kejafy.agent.exception.AgentApplicationException;
import com.pitahmzalendo.kejafy.agent.repository.AgentApplicationRepository;
import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;
import com.pitahmzalendo.kejafy.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AgentApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AgentApplicationService.class);

    private final AgentApplicationRepository agentApplicationRepository;
    private final UserRepository userRepository;

    public AgentApplicationService(AgentApplicationRepository agentApplicationRepository,
                                   UserRepository userRepository) {
        this.agentApplicationRepository = agentApplicationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Submits a KYC application for the given user. Any authenticated user
     * can apply except one who is already an AGENT/ADMIN or already has a
     * PENDING application — both are rejected rather than silently
     * accepted, so the client gets a clear reason instead of a confusing
     * duplicate.
     */
    public AgentApplication apply(User applicant, AgentApplicationRequest request) {
        if (applicant.getRole() != UserRole.CUSTOMER) {
            throw new AgentApplicationException(
                    "Only customer accounts can apply to become an agent.");
        }
        if (agentApplicationRepository.existsByApplicantAndStatus(applicant, AgentApplicationStatus.PENDING)) {
            throw new AgentApplicationException(
                    "You already have a pending agent application.");
        }

        AgentApplication application = new AgentApplication(
                applicant,
                request.idNumber(),
                request.idDocumentFrontUrl(),
                request.idDocumentBackUrl(),
                request.selfieUrl()
        );

        application = agentApplicationRepository.save(application);
        log.info("Agent application submitted, applicantId={}, applicationId={}",
                applicant.getId(), application.getId());
        return application;
    }

    @Transactional(readOnly = true)
    public AgentApplication getMyLatestApplication(User applicant) {
        return agentApplicationRepository.findFirstByApplicantOrderBySubmittedAtDesc(applicant)
                .orElseThrow(() -> new AgentApplicationException("No agent application found."));
    }

    @Transactional(readOnly = true)
    public Page<AgentApplication> getPendingApplications(Pageable pageable) {
        return agentApplicationRepository.findByStatusOrderBySubmittedAtAsc(
                AgentApplicationStatus.PENDING, pageable);
    }

    /** Approving flips the applicant's role to AGENT — this is the only place that happens. */
    public AgentApplication approve(Long applicationId, User admin) {
        AgentApplication application = requirePending(applicationId);
        application.approve(admin);

        User applicant = application.getApplicant();
        applicant.setRole(UserRole.AGENT);
        userRepository.save(applicant);

        log.info("Agent application approved, applicationId={}, applicantId={}, reviewerId={}",
                applicationId, applicant.getId(), admin.getId());
        return application;
    }

    public AgentApplication reject(Long applicationId, User admin, String reason) {
        AgentApplication application = requirePending(applicationId);
        application.reject(admin, reason);
        log.info("Agent application rejected, applicationId={}, applicantId={}, reviewerId={}",
                applicationId, application.getApplicant().getId(), admin.getId());
        return application;
    }

    private AgentApplication requirePending(Long applicationId) {
        AgentApplication application = agentApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new AgentApplicationException("Agent application not found."));
        if (!application.isPending()) {
            throw new AgentApplicationException("This application has already been reviewed.");
        }
        return application;
    }
}
