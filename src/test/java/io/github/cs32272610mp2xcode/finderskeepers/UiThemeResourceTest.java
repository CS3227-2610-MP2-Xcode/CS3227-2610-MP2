package io.github.cs32272610mp2xcode.finderskeepers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class UiThemeResourceTest {
    @Test
    void sharedThemeIsPackagedWithShellNavigationAndActionStyles() throws IOException {
        try (InputStream resource = FindersKeepersApp.class.getResourceAsStream("app.css")) {
            assertNotNull(resource);
            String css = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
            assertAll(
                    () -> assertTrue(css.contains(".login-shell")),
                    () -> assertTrue(css.contains(".login-mascot")),
                    () -> assertTrue(css.contains(".top-bar")),
                    () -> assertTrue(css.contains(".workspace-tabs")),
                    () -> assertTrue(css.contains(".student-workspace")),
                    () -> assertTrue(css.contains(".primary-button")),
                    () -> assertTrue(css.contains(".danger-button")));
        }
    }

    @Test
    void studentMascotIsPackagedWithTheApplication() {
        assertNotNull(FindersKeepersApp.class.getResource(
                "report/ui/assets/student-bear.png"));
    }
}
