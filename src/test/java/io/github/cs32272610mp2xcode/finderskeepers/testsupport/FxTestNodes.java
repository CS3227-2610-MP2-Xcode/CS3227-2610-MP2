package io.github.cs32272610mp2xcode.finderskeepers.testsupport;

import java.util.Objects;
import java.util.stream.Stream;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/** Traverses currently constructed controls, including unskinned container content. */
public final class FxTestNodes {
    private FxTestNodes() {
    }

    /** Returns a depth-first stream of a node and its currently attached content. */
    public static Stream<Node> descendants(Node root) {
        Objects.requireNonNull(root, "root");
        Stream<Node> children;
        if (root instanceof TabPane tabs) {
            children = tabs.getTabs().stream().map(Tab::getContent).filter(Objects::nonNull);
        } else if (root instanceof ScrollPane scroll) {
            children = Stream.ofNullable(scroll.getContent());
        } else if (root instanceof SplitPane split) {
            children = split.getItems().stream();
        } else if (root instanceof Parent parent) {
            children = parent.getChildrenUnmodifiable().stream();
        } else {
            children = Stream.empty();
        }
        return Stream.concat(Stream.of(root), children.flatMap(FxTestNodes::descendants));
    }

    /** Finds a rendered action by its user-visible label. */
    public static Button button(Node root, String text) {
        return descendants(root).filter(Button.class::isInstance).map(Button.class::cast)
                .filter(candidate -> candidate.getText().equals(text))
                .findFirst().orElseThrow();
    }

    /** Joins currently constructed label text for user-visible feedback assertions. */
    public static String labels(Node root) {
        return descendants(root).filter(Label.class::isInstance).map(Label.class::cast)
                .map(Label::getText).reduce("", (left, right) -> left + " " + right);
    }
}
