package com.pitahmzalendo.kejafy.agent.repository;


import com.pitahmzalendo.kejafy.agent.entity.AgentApplication;
import com.pitahmzalendo.kejafy.agent.entity.AgentApplicationStatus;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentApplicationRepository extends JpaRepository<AgentApplication, Long> {

    Optional<AgentApplication> findFirstByApplicantOrderBySubmittedAtDesc(User applicant);

    boolean existsByApplicantAndStatus(User applicant, AgentApplicationStatus status);

    Page<AgentApplication> findByStatusOrderBySubmittedAtAsc(AgentApplicationStatus status, Pageable pageable);
}
