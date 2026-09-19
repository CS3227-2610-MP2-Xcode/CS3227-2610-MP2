package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemReport;

/** Strict versioned JSON implementation of the report repository. */
public final class JsonReportRepository implements ReportRepository {
    private final Path reportStorePath;

    /**
     * Creates a repository for one caller-supplied report-store path.
     *
     * @param path report-store location
     */
    public JsonReportRepository(Path path) {
        this.reportStorePath = Objects.requireNonNull(path, "reportStorePath")
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public synchronized List<ItemReport> loadAll() throws ReportStoreException {
        if (Files.notExists(reportStorePath, NOFOLLOW_LINKS)) {
            return List.of();
        }
        throw new ReportStoreException(ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE);
    }
}
