package com.pitahmzalendo.kejafy.report.service;

import com.pitahmzalendo.kejafy.listing.entity.Listing;
import com.pitahmzalendo.kejafy.listing.entity.ListingStatus;
import com.pitahmzalendo.kejafy.listing.exception.ListingNotFoundException;
import com.pitahmzalendo.kejafy.listing.repository.ListingRepository;
import com.pitahmzalendo.kejafy.report.dto.request.CreateReportRequest;
import com.pitahmzalendo.kejafy.report.dto.request.ReviewReportRequest;
import com.pitahmzalendo.kejafy.report.entity.Report;
import com.pitahmzalendo.kejafy.report.entity.ReportStatus;
import com.pitahmzalendo.kejafy.report.entity.ReportTargetType;
import com.pitahmzalendo.kejafy.report.exception.ReportException;
import com.pitahmzalendo.kejafy.report.exception.ReportNotFoundException;
import com.pitahmzalendo.kejafy.report.repository.ReportRepository;
import com.pitahmzalendo.kejafy.user.entity.User;
import com.pitahmzalendo.kejafy.user.entity.UserRole;
import com.pitahmzalendo.kejafy.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The fraud-reporting half of the trust layer (Listing.verified is the
 * other half — an admin-confirmed positive signal; this is the
 * user-driven negative one).
 *
 * Deliberately does NOT touch ListingService or a Listing's `status` —
 * it goes straight through ListingRepository and Listing's own
 * flagForReview()/archive() methods, so this module doesn't need to
 * know ListingService's internals to auto-hide or take down a listing.
 */
@Service
@Transactional
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    // How many still-PENDING reports a single listing can collect before
    // it's automatically pulled from the public feed pending admin
    // review (see Listing.flagForReview). Deliberately low: false
    // positives cost an admin one extra "dismiss" click; a fake listing
    // left live while reports pile up costs a house-hunter real money.
    static final int AUTO_FLAG_THRESHOLD = 3;

    private final ReportRepository reportRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ReportService(ReportRepository reportRepository,
                          ListingRepository listingRepository,
                          UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    public Report reportListing(User reporter, Long listingId, CreateReportRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing not found."));

        if (listing.isOwnedBy(reporter)) {
            throw new ReportException("You can't report your own listing.");
        }
        if (reportRepository.existsByReporterAndReportedListingAndStatus(reporter, listing, ReportStatus.PENDING)) {
            throw new ReportException("You already have a pending report for this listing.");
        }

        Report report = reportRepository.save(
                Report.forListing(reporter, listing, request.reason(), request.details()));

        listing.incrementReportCount();
        long pendingCount = reportRepository.countByReportedListingAndStatus(listing, ReportStatus.PENDING);
        if (!listing.isFlaggedForReview() && listing.getStatus() == ListingStatus.PUBLISHED
                && pendingCount >= AUTO_FLAG_THRESHOLD) {
            listing.flagForReview();
            log.warn("Listing auto-flagged for review, listingId={}, pendingReports={}",
                    listing.getId(), pendingCount);
        }
        listingRepository.save(listing);

        log.info("Listing report filed, listingId={}, reporterId={}, reason={}",
                listing.getId(), reporter.getId(), request.reason());
        return report;
    }

    public Report reportAgent(User reporter, Long agentId, CreateReportRequest request) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ReportException("Agent not found."));

        if (agent.getRole() != UserRole.AGENT) {
            throw new ReportException("This account is not an agent.");
        }
        if (agent.getId().equals(reporter.getId())) {
            throw new ReportException("You can't report yourself.");
        }
        if (reportRepository.existsByReporterAndReportedAgentAndStatus(reporter, agent, ReportStatus.PENDING)) {
            throw new ReportException("You already have a pending report for this agent.");
        }

        Report report = reportRepository.save(
                Report.forAgent(reporter, agent, request.reason(), request.details()));

        log.info("Agent report filed, agentId={}, reporterId={}, reason={}",
                agent.getId(), reporter.getId(), request.reason());
        return report;
    }

    @Transactional(readOnly = true)
    public Page<Report> getMyReports(User reporter, Pageable pageable) {
        return reportRepository.findByReporterOrderByCreatedAtDesc(reporter, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Report> getPending(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtAsc(ReportStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Listing> getFlaggedListings(Pageable pageable) {
        return listingRepository.findByFlaggedForReviewTrueOrderByFlaggedAtAsc(pageable);
    }

    /**
     * The other half of the trust layer alongside fraud reports: an
     * admin-confirmed positive signal (site visit, phone verification,
     * etc), independent of whether the listing has ever been reported.
     * Lives here rather than ListingService so the whole trust-layer
     * surface (reports + verification) is in one module.
     */
    public Listing verifyListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing not found."));
        listing.verify();
        listingRepository.save(listing);
        log.info("Listing verified, listingId={}", listingId);
        return listing;
    }

    public Listing unverifyListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException("Listing not found."));
        listing.unverify();
        listingRepository.save(listing);
        log.info("Listing verification revoked, listingId={}", listingId);
        return listing;
    }

    /**
     * Resolves a report. If it targets a listing and takeDownListing is
     * set, archives that listing in the same call (via Listing.archive(),
     * not ListingService — see class javadoc). Either way, once no
     * PENDING reports remain against a flagged listing, the flag clears
     * automatically — a DISMISSED resolution that leaves the listing
     * live shouldn't require a second, separate "unflag" step.
     */
    public Report resolve(Long reportId, User admin, ReviewReportRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found."));

        if (!report.isPending()) {
            throw new ReportException("This report has already been reviewed.");
        }
        if (request.takeDownListing() && report.getTargetType() != ReportTargetType.LISTING) {
            throw new ReportException("takeDownListing only applies to a listing report.");
        }

        report.resolve(admin, request.status(), request.resolutionNotes());

        if (report.getTargetType() == ReportTargetType.LISTING) {
            Listing listing = report.getReportedListing();

            if (request.takeDownListing()) {
                listing.archive();
                listing.clearFlag();
            } else {
                long remainingPending = reportRepository.countByReportedListingAndStatus(
                        listing, ReportStatus.PENDING);
                if (remainingPending == 0 && listing.isFlaggedForReview()) {
                    listing.clearFlag();
                }
            }
            listingRepository.save(listing);
        }

        log.info("Report resolved, reportId={}, status={}, adminId={}, takeDownListing={}",
                reportId, request.status(), admin.getId(), request.takeDownListing());
        return report;
    }
}
