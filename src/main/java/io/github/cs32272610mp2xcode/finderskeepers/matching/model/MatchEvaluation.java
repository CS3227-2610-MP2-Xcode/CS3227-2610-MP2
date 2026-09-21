package io.github.cs32272610mp2xcode.finderskeepers.matching.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Immutable matching-rule evidence for one eligible LOST-to-FOUND pair. */
public final class MatchEvaluation {
    /** The four fixed matching criteria in presentation order. */
    public enum Criterion {
        /** Exact canonical category equality. */
        CATEGORY(40, true),
        /** Exact shared normalized item-name token. */
        KEYWORDS(20, false),
        /** Exact normalized location equality. */
        LOCATION(30, false),
        /** Directional inclusive seven-day occurrence window. */
        DATE(10, true);

        private final int points;

        private final boolean mandatoryGate;

        Criterion(int criterionPoints, boolean gate) {
            points = criterionPoints;
            mandatoryGate = gate;
        }

        /** Returns the fixed points awarded when this criterion passes.
         * @return fixed positive points
         */
        public int points() {
            return points;
        }

        /** Returns whether this criterion is also a mandatory gate.
         * @return true for a mandatory qualification gate
         */
        public boolean mandatoryGate() {
            return mandatoryGate;
        }
    }

    /**
     * One fixed criterion outcome.
     *
     * @param criterion fixed criterion identity
     * @param passed whether the criterion passed
     */
    public record CriterionResult(Criterion criterion, boolean passed) {
        /** Validates the fixed criterion identity. */
        public CriterionResult {
            Objects.requireNonNull(criterion, "criterion");
        }

        /** Returns awarded points, or zero when the criterion failed.
         * @return awarded rule points
         */
        public int points() {
            return passed ? criterion.points() : 0;
        }
    }

    private final PossibleMatchPair pair;

    private final UUID lostId;

    private final UUID foundId;

    private final List<CriterionResult> components;

    private final List<String> sharedTokens;

    private final long dayGap;

    MatchEvaluation(PossibleMatchPair matchPair, UUID lostReportId, UUID foundReportId,
            boolean categoryPassed, Set<String> commonTokens, boolean locationPassed,
            long occurrenceDayGap) {
        pair = Objects.requireNonNull(matchPair, "pair");
        lostId = Objects.requireNonNull(lostReportId, "lost report ID");
        foundId = Objects.requireNonNull(foundReportId, "found report ID");
        sharedTokens = commonTokens.stream().sorted().toList();
        dayGap = occurrenceDayGap;
        components = List.of(
                new CriterionResult(Criterion.CATEGORY, categoryPassed),
                new CriterionResult(Criterion.KEYWORDS, !sharedTokens.isEmpty()),
                new CriterionResult(Criterion.LOCATION, locationPassed),
                new CriterionResult(Criterion.DATE, dayGap >= 0 && dayGap <= 7));
    }

    /** Returns the unordered pair identity.
     * @return canonical pair
     */
    public PossibleMatchPair pair() {
        return pair;
    }

    /** Returns the LOST report identifier.
     * @return LOST report identifier
     */
    public UUID lostId() {
        return lostId;
    }

    /** Returns the FOUND report identifier.
     * @return FOUND report identifier
     */
    public UUID foundId() {
        return foundId;
    }

    /** Returns all four outcomes in their fixed order.
     * @return immutable component outcomes
     */
    public List<CriterionResult> components() {
        return components;
    }

    /** Returns shared normalized keywords in deterministic lexical order.
     * @return immutable shared-token list
     */
    public List<String> sharedTokens() {
        return sharedTokens;
    }

    /** Returns FOUND date minus LOST date in calendar days.
     * @return signed calendar-day gap
     */
    public long dayGap() {
        return dayGap;
    }

    /** Returns the exact sum of the four rule components.
     * @return aggregate rule points
     */
    public int totalPoints() {
        return components.stream().mapToInt(CriterionResult::points).sum();
    }

    /** Returns whether mandatory gates and the inclusive 70-point threshold pass.
     * @return true when the pair qualifies as a suggestion
     */
    public boolean qualifies() {
        return component(Criterion.CATEGORY).passed()
                && component(Criterion.DATE).passed()
                && totalPoints() >= 70;
    }

    /** Returns brief positive labels suitable for privacy-safe rows.
     * @return immutable positive-reason labels
     */
    public List<String> positiveReasonLabels() {
        return components.stream()
                .filter(CriterionResult::passed)
                .map(result -> switch (result.criterion()) {
                    case CATEGORY -> "Same canonical category";
                    case KEYWORDS -> "Shared item-name keyword(s)";
                    case LOCATION -> "Same normalized location";
                    case DATE -> "Dates within the allowed directional window";
                })
                .toList();
    }

    /** Returns all four detailed component outcomes in fixed order.
     * @return immutable detailed reasons
     */
    public List<String> componentReasons() {
        return components.stream().map(this::componentReason).toList();
    }

    private CriterionResult component(Criterion criterion) {
        return components.get(criterion.ordinal());
    }

    private String componentReason(CriterionResult result) {
        return switch (result.criterion()) {
            case CATEGORY -> result.passed()
                    ? "Same canonical category (+40 rule points)."
                    : "Categories differ (0 rule points; mandatory gate failed).";
            case KEYWORDS -> result.passed()
                    ? "Shared normalized item-name keyword(s): "
                            + String.join(", ", sharedTokens) + " (+20 rule points)."
                    : "No shared normalized item-name keyword (0 rule points).";
            case LOCATION -> result.passed()
                    ? "Same normalized location (+30 rule points)."
                    : "Normalized locations differ (0 rule points).";
            case DATE -> result.passed()
                    ? "FOUND date is " + dayGap
                            + " day(s) after LOST date (+10 rule points)."
                    : "Dates are outside the directional 0-to-7-day window "
                            + "(0 rule points; mandatory gate failed).";
        };
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MatchEvaluation that)) {
            return false;
        }
        return dayGap == that.dayGap
                && pair.equals(that.pair)
                && lostId.equals(that.lostId)
                && foundId.equals(that.foundId)
                && components.equals(that.components)
                && sharedTokens.equals(that.sharedTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pair, lostId, foundId, components, sharedTokens, dayGap);
    }

    @Override
    public String toString() {
        return "MatchEvaluation[redacted]";
    }
}
