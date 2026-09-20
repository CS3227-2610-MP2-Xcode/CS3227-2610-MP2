package io.github.cs32272610mp2xcode.finderskeepers.auth.ui;

import io.github.cs32272610mp2xcode.finderskeepers.AppMetadata;
import io.github.cs32272610mp2xcode.finderskeepers.auth.application.ApplicationRoute;

/** User-facing copy for one authenticated application route. */
record RoutePresentation(String title, String description) {
    static RoutePresentation forRoute(ApplicationRoute route) {
        return switch (route) {
            case STUDENT -> new RoutePresentation(
                    AppMetadata.STUDENT_ROLE + " Home",
                    "Report a lost or found item and check your updates.");
            case DESK_OFFICER -> new RoutePresentation(
                    AppMetadata.DESK_OFFICER_ROLE + " Home",
                    "Review reports and coordinate item collection.");
            case LOGIN -> throw new IllegalArgumentException("Authenticated route required");
        };
    }
}
