package io.github.cs32272610mp2xcode.finderskeepers.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReportEnumParsingTest {
    @Test
    void parsesEveryExactStoredName() {
        for (ReportType type : ReportType.values()) {
            assertEquals(type, ReportType.fromStoredName(type.storedName()));
        }
        for (ItemCategory category : ItemCategory.values()) {
            assertEquals(category, ItemCategory.fromStoredName(category.storedName()));
        }
        for (ReportStatus status : ReportStatus.values()) {
            assertEquals(status, ReportStatus.fromStoredName(status.storedName()));
        }
    }

    @Test
    void rejectsUnknownOrDifferentlyCasedReportTypes() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportType.fromStoredName("lost"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportType.fromStoredName("STOLEN"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportType.fromStoredName(" "));
        assertThrows(IllegalArgumentException.class,
                () -> ReportType.fromStoredName(null));
    }

    @Test
    void rejectsUnknownOrDifferentlyCasedCategories() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemCategory.fromStoredName("Stationery"));
        assertThrows(IllegalArgumentException.class,
                () -> ItemCategory.fromStoredName("VEHICLE"));
        assertThrows(IllegalArgumentException.class,
                () -> ItemCategory.fromStoredName(""));
        assertThrows(IllegalArgumentException.class,
                () -> ItemCategory.fromStoredName(null));
    }

    @Test
    void rejectsUnknownOrDifferentlyCasedStatuses() {
        assertThrows(IllegalArgumentException.class,
                () -> ReportStatus.fromStoredName("submitted"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportStatus.fromStoredName("CLOSED"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportStatus.fromStoredName("\t"));
        assertThrows(IllegalArgumentException.class,
                () -> ReportStatus.fromStoredName(null));
    }
}
