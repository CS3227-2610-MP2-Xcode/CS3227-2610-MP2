package io.github.cs32272610mp2xcode.finderskeepers.report.application;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;
import io.github.cs32272610mp2xcode.finderskeepers.report.ReportCreationRequest;

/** Creates a valid report before passing it to the configured storage boundary. */
public final class ReportSubmissionService implements ReportSubmitter {
    private final Clock clock;

    private final Supplier<UUID> reportIdSupplier;

    private final Consumer<ItemReport> reportSaver;

    /**
     * Creates the report-submission use case.
     *
     * @param submissionClock clock used for domain validation and creation time
     * @param idSupplier source of new report identifiers
     * @param saver operation that persists an already valid report
     */
    public ReportSubmissionService(Clock submissionClock,
            Supplier<UUID> idSupplier, Consumer<ItemReport> saver) {
        clock = Objects.requireNonNull(submissionClock, "submissionClock");
        reportIdSupplier = Objects.requireNonNull(idSupplier, "idSupplier");
        reportSaver = Objects.requireNonNull(saver, "saver");
    }

    /**
     * Creates and saves a report, validating before persistence is invoked.
     *
     * @param request authenticated report-creation input
     * @return saved immutable report
     * @throws ReportSubmissionException if the storage operation fails
     */
    @Override
    public ItemReport submit(ReportCreationRequest request) {
        ItemReport report = ItemReport.create(
                reportIdSupplier.get(), request, clock);
        reportSaver.accept(report);
        return report;
    }
}
