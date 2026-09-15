package io.github.cs32272610mp2xcode.finderskeepers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ProjectSetupTest {
    @Test
    void applicationMetadataContainsInitialIdentity() {
        assertEquals("Finders Keepers", AppMetadata.NAME);
        assertEquals("0.1.0", AppMetadata.VERSION);
        assertEquals("Student", AppMetadata.STUDENT_ROLE);
        assertEquals("Desk Officer", AppMetadata.DESK_OFFICER_ROLE);
        assertFalse(AppMetadata.NAME.isBlank());
    }

    @Test
    void buildRuntimeUsesJava25() {
        assertEquals(25, Runtime.version().feature());
    }
}
