package dev.objz.commandbridge.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BarBuilder {

	public static final class Segment {
		final double weight;
		final String color;

		Segment(String color, double weight) {
			this.color = color;
			this.weight = Math.max(0, weight);
		}
	}

	private final List<Segment> segments = new ArrayList<>();
	private int width = 30;
	private String fillChar = "|";
	private String leftBracket = "<gray>[</gray>";
	private String rightBracket = "<gray>]</gray>";

	private BarBuilder(int width) {
		this.width = Math.max(3, width);

	}

	public static BarBuilder create(int width) {
		return new BarBuilder(width);
	}

	public BarBuilder fill(String fill) {
		this.fillChar = fill;
		return this;
	}

	public BarBuilder brackets(String left, String right) {
		this.leftBracket = left;
		this.rightBracket = right;
		return this;
	}

	public BarBuilder add(String color, double weight) {
		this.segments.add(new Segment(color, weight));
		return this;
	}

	public String build() {
		if (segments.isEmpty()) {
			return leftBracket + "<dark_gray>" + " ".repeat(width) + "</dark_gray>" + rightBracket;
		}

		double total = segments.stream().mapToDouble(s -> s.weight).sum();
		if (total <= 0)
			total = 1.0;

		int n = segments.size();
		int[] counts = new int[n];
		double[] remainders = new double[n];
		int used = 0;

		for (int i = 0; i < n; i++) {
			double exact = (segments.get(i).weight / total) * width;
			counts[i] = (int) Math.floor(exact);
			remainders[i] = exact - counts[i];
			used += counts[i];
		}

		int leftover = width - used;
		if (leftover > 0) {
			List<Integer> order = new ArrayList<>();
			for (int i = 0; i < n; i++)
				order.add(i);
			order.sort(Comparator.<Integer>comparingDouble(i -> remainders[i]).reversed());
			for (int i = 0; i < leftover; i++)
				counts[order.get(i % n)]++;
		}

		StringBuilder sb = new StringBuilder(leftBracket);
		for (int i = 0; i < n; i++) {
			if (counts[i] == 0)
				continue;
			sb.append("<").append(segments.get(i).color).append(">");
			sb.append(fillChar.repeat(counts[i]));
			sb.append("</").append(segments.get(i).color).append(">");
		}
		sb.append(rightBracket);
		return sb.toString();
	}
}
