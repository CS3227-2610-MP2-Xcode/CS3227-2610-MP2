package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import java.util.List;
import java.util.UUID;

import io.github.cs32272610mp2xcode.finderskeepers.report.ItemReport;

/** Persistence boundary for canonical item reports. */
public interface ReportRepository {
    /**
     * Loads every report in stable insertion order.
     *
     * @return an unmodifiable snapshot of all reports
     * @throws ReportStoreException when storage cannot be read safely
     */
    List<ItemReport> loadAll() throws ReportStoreException;

    /**
     * Inserts one report at the end of the stable report order.
     *
     * @param report canonical report to insert
     * @throws ReportStoreException when the identifier is duplicated or storage cannot be changed safely
     */
    void insert(ItemReport report) throws ReportStoreException;

    /**
     * Replaces one existing report without changing its stable position.
     *
     * @param targetId identifier of the report being replaced
     * @param replacement complete replacement report
     * @throws ReportStoreException when the target is missing, immutable state changes, or storage cannot be changed
     */
    void replace(UUID targetId, ItemReport replacement) throws ReportStoreException;
}
