package io.github.cs32272610mp2xcode.finderskeepers.workspace;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import javafx.scene.Node;

/**
 * Neutral composition value for one authenticated workspace feature.
 *
 * @param content feature content node
 * @param onEnter authoritative entry callback
 * @param hasUnsavedText transient-text query
 * @param clearSessionState transient-state clearing callback
 */
public record WorkspaceFeature(Node content, Runnable onEnter,
        BooleanSupplier hasUnsavedText, Runnable clearSessionState) {
    /** Validates the feature callbacks and content node. */
    public WorkspaceFeature {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(onEnter, "onEnter");
        Objects.requireNonNull(hasUnsavedText, "hasUnsavedText");
        Objects.requireNonNull(clearSessionState, "clearSessionState");
    }
}
