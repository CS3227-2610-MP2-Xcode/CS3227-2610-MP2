package io.github.cs32272610mp2xcode.finderskeepers.workspace;

/** Session lifecycle implemented by authenticated role workspaces. */
public interface SessionView {
    /**
     * Returns whether logout would discard unsaved text.
     *
     * @return true when the user must be warned before logout
     */
    boolean hasUnsavedText();

    /** Clears transient per-login state without changing durable storage. */
    void clearSessionState();
}
