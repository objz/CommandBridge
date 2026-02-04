package dev.objz.commandbridge.velocity.ui.chat;

import dev.objz.commandbridge.util.MM;
import dev.objz.commandbridge.velocity.ui.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public final class ChatTable {
	public enum Align {
		LEFT,
		RIGHT
	}

	private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
	private static final int MIN_COL_WIDTH = 12;
	private static final String SEPARATOR = "│";

	private static final class Column {
		final String header;
		final Align align;
		final int weight;
		final int minWidth;

		Column(String header, Align align, int weight, int minWidth) {
			this.header = header;
			this.align = align;
			this.weight = Math.max(1, weight);
			this.minWidth = Math.max(1, minWidth);
		}
	}

	private final List<Column> columns = new ArrayList<>();
	private final List<Component[]> rows = new ArrayList<>();
	private int width;
	private int gap = 2;

	public ChatTable width(int width) {
		this.width = Math.max(0, width);
		return this;
	}

	public ChatTable gap(int gap) {
		this.gap = Math.max(1, gap);
		return this;
	}

	public ChatTable addColumn(String header, Align align, int weight, int minWidth) {
		columns.add(new Column(header, align, weight, minWidth));
		return this;
	}

	public void addRow(String... values) {
		Component[] comps = new Component[values.length];
		for (int i = 0; i < values.length; i++) {
			comps[i] = MM.parse(values[i]);
		}
		addRow(comps);
	}

	public void addRow(Component... values) {
		if (values.length != columns.size()) {
			throw new IllegalArgumentException("Row length mismatch");
		}
		rows.add(values);
	}

	public int measureWidth() {
		int[] widths = computeWidths(0);
		return totalWidth(widths);
	}

	public List<Component> renderLines() {
		return renderLines(width);
	}

	public List<Component> renderLines(int targetWidth) {
		int[] widths = computeWidths(targetWidth);
		List<Component> out = new ArrayList<>();
		out.add(renderHeader(widths));
		for (Component[] row : rows) {
			out.add(renderRow(row, widths));
		}
		return out;
	}

	private int[] computeWidths(int targetWidth) {
		int count = columns.size();
		int[] widths = new int[count];
		int[] weights = new int[count];
		int[] maxCells = new int[count];

		for (int i = 0; i < count; i++) {
			Column col = columns.get(i);
			int headerLen = ChatLayout.pixelWidth(col.header, true);
			maxCells[i] = headerLen;
			weights[i] = col.weight;
		}
		for (Component[] row : rows) {
			for (int i = 0; i < row.length; i++) {
				int len = ChatLayout.pixelWidth(row[i]);
				if (len > maxCells[i]) {
					maxCells[i] = len;
				}
			}
		}

		int baseSum = 0;
		for (int i = 0; i < count; i++) {
			int min = Math.max(columns.get(i).minWidth, maxCells[i]);
			widths[i] = Math.max(MIN_COL_WIDTH, min);
			baseSum += widths[i];
		}

		int sepSpace = separatorSpace();
		int baseTotal = baseSum + sepSpace;
		int desired = targetWidth > 0 ? Math.max(targetWidth, baseTotal) : baseTotal;
		int available = Math.max(1, desired - sepSpace);

		if (baseSum < available) {
			int leftover = available - baseSum;
			int totalWeight = 0;
			for (int w : weights) totalWeight += w;
			int used = 0;
			for (int i = 0; i < count; i++) {
				int add = (int) Math.floor((double) leftover * weights[i] / totalWeight);
				widths[i] += add;
				used += add;
			}
			int remaining = leftover - used;
			int idx = 0;
			while (remaining > 0) {
				widths[idx % count] += 1;
				remaining--;
				idx++;
			}
		} else if (baseSum > available) {
			int overflow = baseSum - available;
			while (overflow > 0) {
				int idx = widestColumn(widths);
				if (widths[idx] <= MIN_COL_WIDTH) {
					break;
				}
				widths[idx] -= 1;
				overflow--;
			}
		}

		return widths;
	}

	private int totalWidth(int[] widths) {
		int sum = 0;
		for (int w : widths) sum += w;
		return sum + separatorSpace();
	}

	private int separatorSpace() {
		if (columns.size() <= 1) {
			return 0;
		}
		int gapPx = ChatLayout.spacesWidth(gap);
		int sepPx = ChatLayout.pixelWidth(SEPARATOR, false);
		return (columns.size() - 1) * (gapPx * 2 + sepPx);
	}

	private int widestColumn(int[] widths) {
		int idx = 0;
		for (int i = 1; i < widths.length; i++) {
			if (widths[i] > widths[idx]) {
				idx = i;
			}
		}
		return idx;
	}

	private Component renderHeader(int[] widths) {
		Component line = Component.empty();
		for (int i = 0; i < columns.size(); i++) {
			if (i > 0) {
				line = line.append(separatorComponent());
			}
			String header = columns.get(i).header;
			Component headerComp = MM.parse("<gradient:" + Theme.C_PRIMARY + ":" + Theme.C_ACCENT + "><bold>" + header + "</bold></gradient>");
			line = line.append(pad(headerComp, widths[i], columns.get(i).align));
		}
		return line;
	}

	private Component renderRow(Component[] row, int[] widths) {
		Component line = Component.empty();
		for (int i = 0; i < row.length; i++) {
			if (i > 0) {
				line = line.append(separatorComponent());
			}
			line = line.append(pad(row[i], widths[i], columns.get(i).align));
		}
		return line;
	}

	private Component separatorComponent() {
		String pad = " ".repeat(Math.max(0, gap));
		return Component.empty()
				.append(Component.text(pad))
				.append(MM.parse("<" + Theme.C_SEP + ">" + SEPARATOR + "</" + Theme.C_SEP + ">"))
				.append(Component.text(pad));
	}

	private Component pad(Component value, int width, Align align) {
		int len = ChatLayout.pixelWidth(value);
		if (len > width) {
			String plain = PLAIN.serialize(value);
			String truncated = truncateToWidth(plain, width);
			value = Component.text(truncated);
			len = ChatLayout.pixelWidth(value);
		}
		int pad = Math.max(0, width - len);
		if (align == Align.RIGHT) {
			return Component.text(ChatLayout.spacesForPixels(pad)).append(value);
		}
		return value.append(Component.text(ChatLayout.spacesForPixels(pad)));
	}

	private String truncateToWidth(String text, int maxWidth) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			String next = sb.toString() + c;
			if (ChatLayout.pixelWidth(next, false) > maxWidth) {
				break;
			}
			sb.append(c);
		}
		String base = sb.toString();
		if (ChatLayout.pixelWidth(base, false) <= maxWidth) {
			return base;
		}
		String ellipsis = "...";
		int ellipsisWidth = ChatLayout.pixelWidth(ellipsis, false);
		String trimmed = base;
		while (!trimmed.isEmpty() && ChatLayout.pixelWidth(trimmed, false) + ellipsisWidth > maxWidth) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed + ellipsis;
	}
}
