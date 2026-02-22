package dev.objz.commandbridge.scripting.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ProblemSink {

    public static final class Problem {
        public final String path;
        public final String message;

        public Problem(String path, String message) {
            this.path = path;
            this.message = message;
        }

        @Override
        public String toString() {
            return (path == null || path.isBlank()) ? message : (path + ": " + message);
        }
    }

    private final List<Problem> problems = new ArrayList<>();

    public void error(String path, String message) {
        problems.add(new Problem(path, message));
    }

    public boolean hasErrors() {
        return !problems.isEmpty();
    }

    public int count() {
        return problems.size();
    }

    public List<Problem> problems() {
        return Collections.unmodifiableList(problems);
    }

    public String toBulletedList(String header) {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isBlank()) {
            sb.append(header).append("\n");
        }
        for (Problem p : problems) {
            sb.append("  - ");
            if (p.path != null && !p.path.isBlank()) {
                sb.append(p.path).append(": ");
            }
            sb.append(p.message);
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Problem p : problems)
            sb.append(p.toString()).append("\n");
        return sb.toString().trim();
    }
}
