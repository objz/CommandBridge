package dev.objz.commandbridge.velocity.util;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * MiniMessage helper + small builder.
 *
 * Simplified API:
 * - MM.cmd(command) -> clickable command (hover: "run <command>", click:
 * suggest command)
 * - MessageBuilder.cmdLine(command, description) -> renders: [command] |
 * [description]
 *
 * No backwards-compatibility helpers included.
 */
public final class MM {

	private static final MiniMessage MINI = MiniMessage.miniMessage();

	private MM() {
	}

	public static Component parse(String mm) {
		return MINI.deserialize(mm);
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	// ---------- styles ----------
	public static Component header(String text) {
		// Subtle gradient for header
		return parse("<gradient:#9AA7FF:#55CCFF><bold>" + safe(text) + "</bold></gradient>");
	}

	public static Component accent(String text) {
		return parse("<#55CCFF>" + safe(text) + "</#55CCFF>");
	}

	public static Component desc(String text) {
		return parse("<#6FC0FF>" + safe(text) + "</#6FC0FF>");
	}

	public static Component ok(String text) {
		return parse("<green>" + safe(text) + "</green>");
	}

	public static Component warn(String text) {
		return parse("<yellow>" + safe(text) + "</yellow>");
	}

	public static Component error(String text) {
		return parse("<red>" + safe(text) + "</red>");
	}

	public static Component muted(String text) {
		return parse("<gray>" + safe(text) + "</gray>");
	}

	public static Component sep() {
		return parse("<gray> | </gray>");
	}

	public static Component bullet(String content) {
		return parse("<gray>• </gray>" + content);
	}

	public static Component kv(String key, String value) {
		return parse("<gray>" + safe(key) + "</gray><gray>:</gray> <white>" + safe(value) + "</white>");
	}

	/**
	 * Create a clickable command label.
	 * - click: suggest the provided command (so the user can edit before sending)
	 * - hover: shows "run <command>"
	 * - label uses a small gradient to look nicer
	 */
	public static Component cmd(String command) {
		Component base = parse("<gradient:#FFFFFF:#6FC0FF><bold>" + safe(command) + "</bold></gradient>");
		String hover = "run " + safe(command);
		return base.hoverEvent(HoverEvent.showText(parse("<gray>" + hover + "</gray>")))
				.clickEvent(ClickEvent.suggestCommand(safe(command)));
	}

	// ---------- builder ----------
	public static MessageBuilder msg() {
		return new MessageBuilder();
	}

	public static final class MessageBuilder {
		private final List<Component> lines = new ArrayList<>();

		public MessageBuilder header(String text) {
			lines.add(MM.header(text));
			// subtle separator (not an extra blank line)
			lines.add(MM.sep());
			return this;
		}

		public MessageBuilder line(Component c) {
			lines.add(c);
			return this;
		}

		public MessageBuilder line(String mm) {
			lines.add(MM.parse(mm));
			return this;
		}

		public MessageBuilder kv(String key, String val) {
			lines.add(MM.kv(key, val));
			return this;
		}

		/**
		 * Simple cmdLine:
		 * - command: clickable command (MM.cmd(command))
		 * - description: textual description displayed after a separator
		 *
		 * Renders: [command clickable] [ | ] [description]
		 */
		public MessageBuilder cmdLine(String command, String description) {
			lines.add(MM.cmd(command).append(MM.sep()).append(MM.desc(description)));
			return this;
		}

		public MessageBuilder item(String mmContent) {
			lines.add(MM.bullet(mmContent));
			return this;
		}

		public MessageBuilder space() {
			lines.add(Component.empty());
			return this;
		}

		/**
		 * Send the accumulated components to the target.
		 * No extra blank lines are injected automatically.
		 */
		public void send(CommandSource to) {
			for (Component c : lines)
				to.sendMessage(c);
		}
	}
}
