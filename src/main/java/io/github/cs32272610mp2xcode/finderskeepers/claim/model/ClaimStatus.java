package io.github.cs32272610mp2xcode.finderskeepers.claim.model;

/** Approved lifecycle states for an ownership claim. */
public enum ClaimStatus {
    /** Claim awaits a Desk Officer decision. */
    PENDING_REVIEW("Pending review", false),

    /** Claim was approved. */
    APPROVED("Approved", true),

    /** Claim was rejected. */
    REJECTED("Rejected", true),

    /** Claim was withdrawn by its claimant. */
    WITHDRAWN("Withdrawn", true);

    private final String displayName;

    private final boolean terminal;

    ClaimStatus(String label, boolean isTerminal) {
        displayName = label;
        terminal = isTerminal;
    }

    /**
     * Returns the stable stored enum name.
     *
     * @return stored name
     */
    public String storedName() {
        return name();
    }

    /**
     * Returns the approved user-facing status label.
     *
     * @return display label
     */
    public String displayName() {
        return displayName;
    }

    /**
     * Returns whether no later lifecycle transition is permitted.
     *
     * @return true for an approved, rejected, or withdrawn claim
     */
    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Parses an exact persisted status name.
     *
     * @param storedValue stored enum name
     * @return parsed status
     */
    public static ClaimStatus fromStoredName(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            throw new IllegalArgumentException("Claim status is required.");
        }
        try {
            return valueOf(storedValue);
        } catch (IllegalArgumentException failure) {
            throw new IllegalArgumentException("Unknown claim status.", failure);
        }
    }
}
