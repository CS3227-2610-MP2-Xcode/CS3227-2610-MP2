package io.github.cs32272610mp2xcode.finderskeepers.review.ui;

import java.util.Objects;

import io.github.cs32272610mp2xcode.finderskeepers.matching.application.OfficerMatchingService;
import io.github.cs32272610mp2xcode.finderskeepers.matching.model.DeterministicMatcher;
import io.github.cs32272610mp2xcode.finderskeepers.matching.persistence.PossibleMatchRepository;
import io.github.cs32272610mp2xcode.finderskeepers.matching.ui.OfficerMatchingPane;
import io.github.cs32272610mp2xcode.finderskeepers.report.persistence.ReportRepository;
import io.github.cs32272610mp2xcode.finderskeepers.review.application.DeskOfficerReviewService;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.SessionView;
import io.github.cs32272610mp2xcode.finderskeepers.workspace.WorkspaceFeature;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Cohesive authenticated Desk Officer workspace containing review and matching. */
public final class DeskOfficerWorkspacePane extends TabPane implements SessionView {
    private final WorkspaceFeature claimsFeature;

    /**
     * Creates fresh per-login review and matching workflows over shared repositories.
     *
     * @param reports shared canonical report repository
     * @param relationships shared possible-match relationship repository
     */
    public DeskOfficerWorkspacePane(ReportRepository reports,
            PossibleMatchRepository relationships) {
        this(reports, relationships, null);
    }

    /**
     * Creates fresh per-login review, matching, and Claims workflows.
     *
     * @param reports shared canonical report repository
     * @param relationships shared possible-match relationship repository
     * @param claims neutral authenticated Claims feature
     */
    public DeskOfficerWorkspacePane(ReportRepository reports,
            PossibleMatchRepository relationships, WorkspaceFeature claims) {
        Objects.requireNonNull(reports, "reports");
        Objects.requireNonNull(relationships, "relationships");
        claimsFeature = claims;
        Tab review = new Tab("Report review",
                new DeskOfficerReviewPane(new DeskOfficerReviewService(reports)));
        Tab matching = new Tab("Possible matches",
                new OfficerMatchingPane(new OfficerMatchingService(
                        reports, relationships, new DeterministicMatcher())));
        review.setClosable(false);
        matching.setClosable(false);
        getTabs().setAll(review, matching);
        if (claimsFeature != null) {
            Tab claimsTab = new Tab("Claims", claimsFeature.content());
            claimsTab.setClosable(false);
            getTabs().add(claimsTab);
            getSelectionModel().selectedItemProperty().addListener(
                    (observable, previous, selected) -> {
                        if (selected == claimsTab) {
                            claimsFeature.onEnter().run();
                        }
                    });
        }
    }

    @Override
    public boolean hasUnsavedText() {
        return claimsFeature != null && claimsFeature.hasUnsavedText().getAsBoolean();
    }

    @Override
    public void clearSessionState() {
        if (claimsFeature != null) {
            claimsFeature.clearSessionState().run();
        }
    }
}
