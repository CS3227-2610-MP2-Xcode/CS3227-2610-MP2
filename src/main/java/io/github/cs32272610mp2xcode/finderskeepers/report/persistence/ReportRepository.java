package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import java.util.List;

import io.github.cs32272610mp2xcode.finderskeepers.report.model.ItemReport;

/** Persistence boundary for canonical item reports. */
public interface ReportRepository {
    /**
     * Loads every report in stable insertion order.
     *
     * @return an unmodifiable snapshot of all reports
     * @throws ReportStoreException when storage cannot be read safely
     */
    List<ItemReport> loadAll() throws ReportStoreException;
}
