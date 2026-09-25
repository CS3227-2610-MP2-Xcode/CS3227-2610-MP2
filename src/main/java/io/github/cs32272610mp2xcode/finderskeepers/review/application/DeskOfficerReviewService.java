package io.github.cs32272610mp2xcode.finderskeepers.review.application;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.claim.application.ApprovedClaimReportService;
import io.github.cs32272610mp2xcode.finderskeepers.claim.persistence.ClaimStoreException;
import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportStatus;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportStoreException;

/** Stateful read-only application service for one Desk Officer report-review view. */
public final class DeskOfficerReviewService {
    private static final String NO_SELECTION_MESSAGE =
            "Select a report to view details.";

    private static final String LOAD_FAILURE_MESSAGE =
            "Reports are unavailable. Please try again.";

    private final ReportRepository repository;

    private final ApprovedClaimReportService approvedClaimReports;

    private List<ItemReport> activeReports = List.of();

    private ReviewQueueFilter activeFilter = ReviewQueueFilter.ALL;

    private UUID selectedReportId;

    private boolean available;

    /**
     * Creates a review service without Claims integration for compatibility callers.
     *
     * @param reportRepository canonical report persistence boundary
     */
    public DeskOfficerReviewService(ReportRepository reportRepository) {
        this(reportRepository, null);
    }

    /**
     * Creates a review service over canonical reports and approved Claim endpoints.
     *
     * @param reportRepository canonical report persistence boundary
     * @param approvedReports approved-Claim endpoint read service
     */
    public DeskOfficerReviewService(ReportRepository reportRepository,
            ApprovedClaimReportService approvedReports) {
        repository = Objects.requireNonNull(reportRepository, "reportRepository");
        approvedClaimReports = approvedReports;
    }

    /**
     * Enters a fresh view and loads the default All queue.
     *
     * @return current immutable presentation state
     */
    public ReviewQueueState enter() {
        activeFilter = ReviewQueueFilter.ALL;
        selectedReportId = null;
        return load(false);
    }

    /**
     * Refreshes the authoritative queue while preserving its current filter.
     *
     * @return current immutable presentation state
     */
    public ReviewQueueState refresh() {
        return load(true);
    }

    /**
     * Explicitly retries a previously failed queue load.
     *
     * @return current immutable presentation state
     */
    public ReviewQueueState retry() {
        return available ? state() : load(true);
    }

    /**
     * Applies one of the exact in-memory queue filters.
     *
     * @param filter filter to activate
     * @return current immutable presentation state
     */
    public ReviewQueueState changeFilter(ReviewQueueFilter filter) {
        Objects.requireNonNull(filter, "filter");
        if (!available) {
            return state();
        }
        activeFilter = filter;
        reconcileSelection();
        return state();
    }

    /**
     * Selects one currently visible report by canonical identifier.
     *
     * @param reportId visible report identifier
     * @return current immutable presentation state
     */
    public ReviewQueueState select(UUID reportId) {
        Objects.requireNonNull(reportId, "reportId");
        if (!available) {
            return state();
        }
        selectedReportId = findById(visibleReports(), reportId)
                .map(ItemReport::reportId)
                .orElse(null);
        return state();
    }

    private ReviewQueueState load(boolean preserveSelection) {
        UUID previousSelection = preserveSelection ? selectedReportId : null;
        try {
            List<ItemReport> reports = repository.loadAll();
            Set<UUID> excluded = approvedClaimReports == null
                    ? Set.of() : approvedClaimReports.loadApprovedReportIds();
            activeReports = reports.stream()
                    .filter(DeskOfficerReviewService::hasActiveStatus)
                    .filter(report -> !excluded.contains(report.reportId()))
                    .toList();
            available = true;
            selectedReportId = previousSelection;
            reconcileSelection();
        } catch (ReportStoreException | ClaimStoreException exception) {
            activeReports = List.of();
            selectedReportId = null;
            available = false;
        }
        return state();
    }

    private void reconcileSelection() {
        if (selectedReportId != null && findById(visibleReports(), selectedReportId).isEmpty()) {
            selectedReportId = null;
        }
    }

    private ReviewQueueState state() {
        List<ItemReport> visibleReports = visibleReports();
        Optional<ItemReport> selected = selectedReportId == null
                ? Optional.empty() : findById(visibleReports, selectedReportId);
        Optional<String> queueMessage = available
                ? emptyMessage(visibleReports) : Optional.empty();
        String detailsMessage = available ? NO_SELECTION_MESSAGE : LOAD_FAILURE_MESSAGE;
        if (selected.isPresent()) {
            detailsMessage = "";
        }
        return new ReviewQueueState(available, activeFilter, visibleReports, selected,
                queueMessage, detailsMessage, !available);
    }

    private Optional<String> emptyMessage(List<ItemReport> visibleReports) {
        if (activeReports.isEmpty()) {
            return Optional.of("No active reports.");
        }
        if (visibleReports.isEmpty() && activeFilter != ReviewQueueFilter.ALL) {
            return Optional.of("No active reports match the "
                    + activeFilter.displayName() + " filter.");
        }
        return Optional.empty();
    }

    private List<ItemReport> visibleReports() {
        if (!available) {
            return List.of();
        }
        return activeReports.stream().filter(activeFilter::matches).toList();
    }

    private static Optional<ItemReport> findById(List<ItemReport> reports, UUID reportId) {
        return reports.stream()
                .filter(report -> report.reportId().equals(reportId))
                .findFirst();
    }

    private static boolean hasActiveStatus(ItemReport report) {
        return report.status() == ReportStatus.SUBMITTED
                || report.status() == ReportStatus.UNDER_REVIEW;
    }
}
