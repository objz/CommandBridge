package dev.objz.commandbridge.velocity.ui.cli;

import dev.objz.commandbridge.velocity.ui.Theme;

public final class CliLayout {
	public static final int DEFAULT_WIDTH = 70;

	private CliLayout() {
	}

	public static void appendHeader(StringBuilder sb, String title) {
		appendHeader(sb, title, DEFAULT_WIDTH);
	}

	public static void appendHeader(StringBuilder sb, String title, int width) {
		int boxWidth = Math.max(DEFAULT_WIDTH, width);
		String text = " " + title.toUpperCase() + " ";
		int totalPadding = boxWidth - text.length() - 2;
		int leftPad = totalPadding / 2;
		int rightPad = totalPadding - leftPad;

		sb.append("\n");
		sb.append(Theme.ANSI_PRIMARY).append(Theme.ANSI_BOLD);
		sb.append(BoxDrawing.boxTop(boxWidth, true));
		sb.append(Theme.ANSI_RESET).append("\n");

		sb.append(Theme.ANSI_PRIMARY).append(Theme.ANSI_BOLD);
		sb.append(BoxDrawing.DOUBLE_VERTICAL);
		sb.append(Theme.ANSI_ACCENT).append(Theme.ANSI_BOLD);
		sb.append(" ".repeat(Math.max(0, leftPad)));
		sb.append(text);
		sb.append(" ".repeat(Math.max(0, rightPad)));
		sb.append(Theme.ANSI_PRIMARY).append(Theme.ANSI_BOLD);
		sb.append(BoxDrawing.DOUBLE_VERTICAL);
		sb.append(Theme.ANSI_RESET).append("\n");

		sb.append(Theme.ANSI_PRIMARY).append(Theme.ANSI_BOLD);
		sb.append(BoxDrawing.boxBottom(boxWidth, true));
		sb.append(Theme.ANSI_RESET);
	}
}
