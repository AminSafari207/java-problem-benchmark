package org.example.jpb.util;

import java.util.regex.Pattern;

public final class Console {

	private static final String RESET = "\u001B[0m";
	private static final String RED = "\u001B[31m";
	private static final String GREEN = "\u001B[32m";
	private static final String YELLOW = "\u001B[33m";
	private static final String BLUE = "\u001B[34m";
	private static final String GRAY = "\u001B[90m";

	private static final String TOP_LEFT = "┌";
	private static final String TOP_RIGHT = "┐";
	private static final String BOTTOM_LEFT = "└";
	private static final String BOTTOM_RIGHT = "┘";
	private static final String H = "─";
	private static final String V = "│";

	private static final Pattern ANSI_PATTERN = Pattern.compile(
		"\u001B\\[[0-9;]*m"
	);

	private Console() {
		System.out.println("Console must not be instantiated");
	}

	public static void print(String message) {
		System.out.println(message);
	}

	public static void line() {
		System.out.println();
	}

	public static void divider(char c, int width) {
		print(String.valueOf(c).repeat(width));
	}

	public static String indent(int level) {
		return "  ".repeat(Math.max(0, level));
	}

	public static void boxTitle(String title, int width) {
		print(TOP_LEFT + H.repeat(width - 2) + TOP_RIGHT);

		String content = " " + title;
		print(V + padRight(content, width - 2) + V);

		print(BOTTOM_LEFT + H.repeat(width - 2) + BOTTOM_RIGHT);
	}

	public static String padRight(String text, int width) {
		int visualLen = visualLength(text);
		if (visualLen >= width) return text;
		return text + " ".repeat(width - visualLen);
	}

	public static String truncate(String text, int width) {
		if (text == null) return "";
		if (width <= 0) return "";

		if (visualLength(text) <= width) {
			return text;
		}

		if (width == 1) {
			return "…";
		}

		String plain = ANSI_PATTERN.matcher(text).replaceAll("");
		if (plain.length() <= width) {
			return plain;
		}

		return plain.substring(0, width - 1).trim() + "…";
	}

	public static String padRightTruncate(String text, int width) {
		return padRight(truncate(text, width), width);
	}

	public static String padColumnTruncate(
		String text,
		int width,
		int trailingPadding
	) {
		if (width <= 0) {
			throw new IllegalArgumentException("width must be positive");
		}

		if (trailingPadding < 0) {
			throw new IllegalArgumentException("trailingPadding must be >= 0");
		}

		if (trailingPadding > width) {
			throw new IllegalArgumentException("trailingPadding must be <= width");
		}

		String value = text == null ? "" : text;
		int contentWidth = width - trailingPadding;

		if (contentWidth <= 0) {
			return " ".repeat(width);
		}

		return (
			Console.padRightTruncate(value, contentWidth) +
			" ".repeat(trailingPadding)
		);
	}

	public static String padColumnTruncate(String text, int width) {
		return padColumnTruncate(text, width, 2);
	}

	public static void section(String title, int width, int indent) {
		String prefix = indent(indent);

		print(prefix + title);

		int lineWidth = width - prefix.length();
		print(prefix + "─".repeat(lineWidth));
	}

	public static String red(String text) {
		return RED + text + RESET;
	}

	public static String green(String text) {
		return GREEN + text + RESET;
	}

	public static String yellow(String text) {
		return YELLOW + text + RESET;
	}

	public static String blue(String text) {
		return BLUE + text + RESET;
	}

	public static String gray(String text) {
		return GRAY + text + RESET;
	}

	//

	public static int visualLength(String text) {
		if (text == null) return 0;
		return ANSI_PATTERN.matcher(text).replaceAll("").length();
	}
}
