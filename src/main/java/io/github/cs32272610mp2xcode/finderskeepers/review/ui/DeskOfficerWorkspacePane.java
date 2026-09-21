package io.github.cs32272610mp2xcode.finderskeepers.review.ui;

import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.matching.application.OfficerMatchingService;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.DeterministicMatcher;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.ui.OfficerMatchingPane;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.review.application.DeskOfficerReviewService;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Cohesive authenticated Desk Officer workspace containing review and matching. */
public final class DeskOfficerWorkspacePane extends TabPane {
    /**
     * Creates fresh per-login review and matching workflows over shared repositories.
     *
     * @param reports shared canonical report repository
     * @param relationships shared possible-match relationship repository
     */
    public DeskOfficerWorkspacePane(ReportRepository reports,
            PossibleMatchRepository relationships) {
        Objects.requireNonNull(reports, "reports");
        Objects.requireNonNull(relationships, "relationships");
        Tab review = new Tab("Report review",
                new DeskOfficerReviewPane(new DeskOfficerReviewService(reports)));
        Tab matching = new Tab("Possible matches",
                new OfficerMatchingPane(new OfficerMatchingService(
                        reports, relationships, new DeterministicMatcher())));
        review.setClosable(false);
        matching.setClosable(false);
        getTabs().setAll(review, matching);
    }
}
