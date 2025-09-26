package dev.objz.commandbridge.main.scripting.v3.loader;

import dev.objz.commandbridge.main.logging.Log;
import dev.objz.commandbridge.main.scripting.v3.util.Levenshtein;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.*;

public final class KeyNormalizer {
	private final Map<String, String> aliases = new LinkedHashMap<>();
	private final Set<String> allowed; // dynamic, per-model

	public KeyNormalizer(Set<String> allowedPaths) {
		this.allowed = allowedPaths;
		alias("defaults.target.server.timout", "defaults.target.server.timeout");
		alias("defaults.target.server.frequenty", "defaults.target.server.frequency");
		alias("defaults.target.runas", "defaults.target.run-as");
		alias("commands[].target.runas", "commands[].target.run-as");
	}

	private void alias(String bad, String good) {
		aliases.put(bad, good);
	}

	public void normalize(ConfigurationNode root) {
		List<String> paths = new ArrayList<>();
		walk("", root, paths);
		for (String p : paths) {
			String normalized = remapIndex(p);
			if (aliases.containsKey(normalized)) {
				move(root, p, aliases.get(normalized));
				Log.warn("Script: corrected '{}' -> '{}'", p, aliases.get(normalized));
				continue;
			}
			if (!matchesAny(allowed, normalized)) {
				String guess = closest(normalized, allowed);
				if (guess != null) {
					String resolved = reindexGuess(guess, p);
					move(root, p, resolved);
					Log.warn("Script: corrected '{}' -> '{}' (levenshtein)", p, resolved);
				} else {
					Log.warn("Script: unknown key '{}' (ignored)", p);
				}
			}
		}
	}

	private static String reindexGuess(String guess, String original) {
		String[] gParts = guess.split("\\.");
		String[] oParts = original.split("\\.");
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < gParts.length; i++) {
			String g = gParts[i];
			String o = i < oParts.length ? oParts[i] : "";
			if (g.endsWith("[]")) {
				String key = g.substring(0, g.length() - 2);
				int lb = o.indexOf('[');
				if (lb >= 0 && o.endsWith("]")) {
					String idx = o.substring(lb + 1, o.length() - 1);
					if (!idx.isEmpty()) {
						g = key + "[" + idx + "]";
					} else {
						g = key;
					}
				} else {
					g = key;
				}
			}
			if (out.length() > 0)
				out.append('.');
			out.append(g);
		}
		return out.toString();
	}

	private static void walk(String base, ConfigurationNode n, List<String> out) {
		if (n.isMap()) {
			for (var e : n.childrenMap().entrySet()) {
				String k = String.valueOf(e.getKey());
				String path = base.isEmpty() ? k : base + "." + k;
				out.add(path);
				walk(path, e.getValue(), out);
			}
		} else if (n.isList()) {
			int i = 0;
			for (ConfigurationNode c : n.childrenList()) {
				String path = base + "[" + (i++) + "]";
				out.add(path);
				walk(path, c, out);
			}
		}
	}

	private static String remapIndex(String p) {
		return p.replaceAll("\\[[0-9]+\\]", "[]");
	}

	private static boolean matchesAny(Set<String> allowed, String path) {
		for (String a : allowed) {
			String ar = a.replace("[]", "\\[[0-9]+\\]");
			if (path.equals(a) || path.matches(ar))
				return true;
		}
		return false;
	}

	private static String closest(String input, Set<String> options) {
		int threshold = dynamicThreshold(input);
		String best = null;
		int bestD = Integer.MAX_VALUE;
		for (String o : options) {
			int d = Levenshtein.distance(o, input);
			if (d < bestD) {
				bestD = d;
				best = o;
			}
		}
		return bestD <= threshold ? best : null;
	}

	private static int dynamicThreshold(String input) {
		int len = (input == null) ? 0 : input.length();
		if (len <= 4)
			return 1;
		if (len <= 8)
			return 2;
		if (len <= 16)
			return 3;
		return 4; // clamp for very long keys
	}

	private static void move(ConfigurationNode root, String from, String to) {
		var src = get(root, from);
		if (src == null || src.virtual())
			return;
		var dst = get(root, to);
		if (dst == null)
			return;
		try {
			dst.set(src.raw());
			src.set(null);
		} catch (Exception ignore) {
		}
	}

	private static ConfigurationNode get(ConfigurationNode root, String path) {
		String[] parts = path.split("\\.");
		ConfigurationNode cur = root;
		for (String pr : parts) {
			if (pr.endsWith("]")) {
				int lb = pr.indexOf('[');
				String key = pr.substring(0, lb);
				String idxStr = pr.substring(lb + 1, pr.length() - 1);
				if (idxStr.isEmpty()) {
					cur = cur.node(key);
				} else {
					int idx = Integer.parseInt(idxStr);
					cur = cur.node(key).node(idx);
				}
			} else {
				cur = cur.node(pr);
			}
		}
		return cur;
	}
}
