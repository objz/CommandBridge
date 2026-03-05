package dev.objz.commandbridge.scripting.migration;

public record MigrationResult(
        Status status,
        int fromVersion,
        int toVersion,
        String yaml,
        String error,
        int rulesApplied) {

    public enum Status {
        SUCCESS,
        SKIPPED,
        ERROR
    }

    public boolean ok() {
        return status == Status.SUCCESS;
    }

    public boolean skipped() {
        return status == Status.SKIPPED;
    }

    static MigrationResult success(int from, int to, String yaml, int rulesApplied) {
        return new MigrationResult(Status.SUCCESS, from, to, yaml, null, rulesApplied);
    }

    static MigrationResult skip(int version) {
        return new MigrationResult(Status.SKIPPED, version, version, null, null, 0);
    }

    static MigrationResult error(String error) {
        return new MigrationResult(Status.ERROR, -1, -1, null, error, 0);
    }
}
