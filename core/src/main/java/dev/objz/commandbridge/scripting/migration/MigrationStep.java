package dev.objz.commandbridge.scripting.migration;

import java.util.List;

/**
 * Represents a single version-to-version migration step loaded from a
 * resource file (e.g. {@code migrations/2-3.yml}).
 *
 * @param from  the source version
 * @param to    the target version
 * @param rules the list of rules to apply
 */
public record MigrationStep(int from, int to, List<MigrationRule> rules) {

    public MigrationStep {
        rules = List.copyOf(rules);
    }
}
