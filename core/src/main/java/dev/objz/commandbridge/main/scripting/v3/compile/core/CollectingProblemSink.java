package dev.objz.commandbridge.main.scripting.v3.compile.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollectingProblemSink implements ProblemSink {
	private final List<Problem> problems = new ArrayList<>();

	@Override
	public void add(Problem problem) {
		problems.add(problem);
	}

	@Override
	public List<Problem> snapshot() {
		return Collections.unmodifiableList(problems);
	}

	public List<Problem> problems() {
		return snapshot();
	}
}
