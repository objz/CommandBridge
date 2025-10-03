package dev.objz.commandbridge.proto.feedback;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight accumulator for "process N items" style ops (register, execute,
 * etc).
 * Call success()/failure(msg) per item; call warn(msg) for soft issues.
 */
public final class FeedbackCollector {
	private int requested;
	private int succeeded;
	private int failed;
	private final List<String> warnings = new ArrayList<>();
	private final List<String> errors = new ArrayList<>();

	/** Count an item as successful (increments requested + succeeded). */
	public FeedbackCollector success() {
		requested++;
		succeeded++;
		return this;
	}

	/** Count an item as failed with message (increments requested + failed). */
	public FeedbackCollector failure(String message) {
		requested++;
		failed++;
		if (message != null && !message.isBlank())
			errors.add(message);
		return this;
	}

	/** Add a non-fatal warning (does NOT change requested/succeeded/failed). */
	public FeedbackCollector warn(String message) {
		if (message != null && !message.isBlank())
			warnings.add(message);
		return this;
	}

	/** Merge another collector into this one. */
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
