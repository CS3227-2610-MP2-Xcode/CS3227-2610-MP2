package io.github.cs32272610mp2xcode.finderskeepers.report.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonReportRepositoryTest {
    @TempDir
    private Path temporaryDirectory;

    @Test
    void missingStoreLoadsEmptyWithoutCreatingFilesystemEntries() throws ReportStoreException {
        Path missingParent = temporaryDirectory.resolve("missing-parent");
        Path store = missingParent.resolve("reports.json");
        ReportRepository repository = new JsonReportRepository(store);

        assertTrue(repository.loadAll().isEmpty(), "A missing store must load as empty");
        assertFalse(Files.exists(missingParent), "Loading must not create the parent directory");
        assertFalse(Files.exists(store), "Loading must not create the report store");
    }
}
