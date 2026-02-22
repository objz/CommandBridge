package dev.objz.commandbridge.net.payloads.feedback;

import java.util.ArrayList;
import java.util.List;

public final class FeedbackCollector {
    private int requested;
    private int succeeded;
    private int failed;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public FeedbackCollector success() {
        requested++;
        succeeded++;
        return this;
    }

    public FeedbackCollector failure(String message) {
        requested++;
        failed++;
        if (message != null && !message.isBlank())
            errors.add(message);
        return this;
    }

    public FeedbackCollector warn(String message) {
        if (message != null && !message.isBlank())
            warnings.add(message);
        return this;
    }

    public FeedbackCollector merge(FeedbackCollector other) {
        if (other == null)
            return this;
        this.requested += other.requested;
        this.succeeded += other.succeeded;
        this.failed += other.failed;
        this.warnings.addAll(other.warnings);
        this.errors.addAll(other.errors);
        return this;
    }

    public int requested() {
        return requested;
    }

    public int succeeded() {
        return succeeded;
    }

    public int failed() {
        return failed;
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public Feedback build() {
        return new Feedback(requested, succeeded, failed, List.copyOf(warnings), List.copyOf(errors));
    }
}
