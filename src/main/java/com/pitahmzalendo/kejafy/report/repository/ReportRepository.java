package com.pitahmzalendo.kejafy.report.repository;

import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.report.entity.Report;
import com.pitahmzalendo.kejafy.report.entity.ReportStatus;
import com.pitahmzalendo.kejafy.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    Page<Report> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    Page<Report> findByReporterOrderByCreatedAtDesc(User reporter, Pageable pageable);

    long countByReportedListingAndStatus(Listing listing, ReportStatus status);

    long countByReportedAgentAndStatus(User agent, ReportStatus status);

    // Stops the same person spamming duplicate reports on the same
    // target while one is still open — they can file again only after
    // the earlier report has been resolved one way or the other.
    boolean existsByReporterAndReportedListingAndStatus(User reporter, Listing listing, ReportStatus status);

    boolean existsByReporterAndReportedAgentAndStatus(User reporter, User agent, ReportStatus status);
}
