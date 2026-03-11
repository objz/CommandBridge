package dev.objz.commandbridge.scripting.migration;

import java.util.List;
import java.util.Map;

/**
 * Represents a single version-to-version migration step loaded from a
 * resource file (e.g. {@code migrations/2-3.yml}).
 * <p>
 * Rules are grouped by section {@code "scripts"}, {@code "configs"}.
 *
 * @param from     the source version
 * @param to       the target version
 * @param sections map of section name to its list of migration rules
 */
public record MigrationStep(int from, int to, Map<String, List<MigrationRule>> sections) {

    public MigrationStep {
        sections = Map.copyOf(sections);
    }

    public List<MigrationRule> rules(String section) {
        return sections.getOrDefault(section, List.of());
    }
}
