package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;

/** Strict versioned JSON implementation of the report repository. */
public final class JsonReportRepository implements ReportRepository {
    static final int MAX_STORE_BYTES = 16_777_216;

    private final Path reportStorePath;

    private final ReportStoreFiles storeFiles;

    private final int maximumStoreBytes;

    private final ReportStoreJsonCodec codec;

    /**
     * Creates a repository for one caller-supplied report-store path.
     *
     * @param path report-store location
     */
    public JsonReportRepository(Path path) {
        this(path, new NioReportStoreFiles(), MAX_STORE_BYTES);
    }

    JsonReportRepository(Path path, ReportStoreFiles files, int maximumBytes) {
        this.reportStorePath = Objects.requireNonNull(path, "reportStorePath")
                .toAbsolutePath()
                .normalize();
        this.storeFiles = Objects.requireNonNull(files, "storeFiles");
        if (maximumBytes <= 0 || maximumBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("maximumStoreBytes must support a maximum-plus-one probe");
        }
        this.maximumStoreBytes = maximumBytes;
        this.codec = new ReportStoreJsonCodec();
    }

    @Override
    public synchronized List<ItemReport> loadAll() throws ReportStoreException {
        return List.copyOf(readCurrent());
    }

    @Override
    public synchronized void insert(ItemReport report) throws ReportStoreException {
        Objects.requireNonNull(report, "report");
        List<ItemReport> reports = new ArrayList<>(readCurrent());
        if (reports.stream().anyMatch(existing -> existing.reportId().equals(report.reportId()))) {
            throw new ReportStoreException(ReportStoreException.Reason.DUPLICATE_REPORT_ID);
        }
        reports.add(report);
        writeCandidate(reports);
    }

    @Override
    public synchronized void replace(UUID targetId, ItemReport replacement) throws ReportStoreException {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(replacement, "replacement");
        List<ItemReport> reports = new ArrayList<>(readCurrent());
        int targetIndex = findReportIndex(reports, targetId);
        if (targetIndex < 0) {
            throw new ReportStoreException(ReportStoreException.Reason.REPLACEMENT_TARGET_NOT_FOUND);
        }
        ItemReport existing = reports.get(targetIndex);
        if (!replacement.reportId().equals(targetId)
                || !replacement.reporterId().equals(existing.reporterId())
                || !replacement.createdAt().equals(existing.createdAt())) {
            throw new ReportStoreException(ReportStoreException.Reason.IMMUTABLE_FIELD_MISMATCH);
        }
        reports.set(targetIndex, replacement);
        writeCandidate(reports);
    }

    private static int findReportIndex(List<ItemReport> reports, UUID targetId) {
        for (int index = 0; index < reports.size(); index++) {
            if (reports.get(index).reportId().equals(targetId)) {
                return index;
            }
        }
        return -1;
    }

    private List<ItemReport> readCurrent() throws ReportStoreException {
        try {
            Optional<byte[]> stored = storeFiles.readBounded(reportStorePath, maximumStoreBytes);
            if (stored.isEmpty()) {
                return List.of();
            }
            return codec.decode(stored.get());
        } catch (ReportStoreJsonCodec.InvalidStoreException failure) {
            throw new ReportStoreException(ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE);
        } catch (StoreFileFailure failure) {
            ReportStoreException.Reason reason = failure.kind() == StoreFileFailure.Kind.OVER_LIMIT
                    ? ReportStoreException.Reason.CORRUPT_OR_UNSUPPORTED_STORE
                    : ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE;
            throw new ReportStoreException(reason);
        }
    }

    private void writeCandidate(List<ItemReport> reports) throws ReportStoreException {
        byte[] document;
        try {
            document = codec.encode(reports, maximumStoreBytes);
        } catch (ReportStoreJsonCodec.InvalidReportStateException failure) {
            throw new ReportStoreException(ReportStoreException.Reason.UNENCODABLE_OR_OVER_LIMIT_RESULT);
        }
        try {
            storeFiles.replaceAtomically(reportStorePath, document);
        } catch (StoreFileFailure failure) {
            throw new ReportStoreException(ReportStoreException.Reason.STORAGE_IO_OR_SAFE_REPLACEMENT_FAILURE);
        }
    }

}
