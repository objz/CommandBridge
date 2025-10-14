package dev.objz.commandbridge.scripting.bind;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderExtractor {

	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]*)}");

	private PlaceholderExtractor() {
	}

	public static List<String> extract(String command) {
		if (command == null || command.isBlank()) {
			return List.of();
		}

		List<String> result = new ArrayList<>();
		Matcher matcher = PLACEHOLDER.matcher(command);

		while (matcher.find()) {
			String name = matcher.group(1);
			if (name != null && !name.isBlank()) {
				result.add(name.trim());
			}
		}

		return List.copyOf(result);
	}

	public static List<String> extractAll(List<String> commands) {
		if (commands == null || commands.isEmpty()) {
			return List.of();
		}

		List<String> result = new ArrayList<>();

		for (String command : commands) {
			if (command == null || command.isBlank()) {
				continue;
			}

			Matcher matcher = PLACEHOLDER.matcher(command);
			while (matcher.find()) {
				String name = matcher.group(1);
				if (name != null && !name.isBlank()) {
					result.add(name.trim());
				}
			}
		}

		return List.copyOf(result);
	}
}
