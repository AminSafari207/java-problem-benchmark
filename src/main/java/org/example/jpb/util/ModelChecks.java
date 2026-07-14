package org.example.jpb.util;

import java.util.*;
import java.util.function.Function;

public final class ModelChecks {

	private ModelChecks() {
		throw new AssertionError("No instances");
	}

	public static String requireNormalizedNonBlank(String value, String fieldName) {
		requireNonNull(value, fieldName);

		if (value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}

		if (!value.equals(value.strip())) {
			throw new IllegalArgumentException(fieldName + " must not start or end with whitespace");
		}

		return value;
	}

	public static String defaultIfBlank(String value, String defaultValue) {
		return value == null || value.isBlank() ? defaultValue : value;
	}

	public static int defaultIfNull(Integer value, int defaultValue) {
		return value != null ? value : defaultValue;
	}

	public static <T> T requireNonNull(T value, String fieldName) {
		return Objects.requireNonNull(value, fieldName + " must not be null");
	}

	public static String requireLengthBetween(String value, int minLength, int maxLength, String fieldName) {
		requireNonNull(value, fieldName);

		int length = value.length();

		if (length < minLength || length > maxLength) {
			throw new IllegalArgumentException(
				fieldName + " length must be between " + minLength + " and " + maxLength
			);
		}

		return value;
	}

	public static String requireNormalizedNonBlankLengthBetween(
		String value,
		int minLength,
		int maxLength,
		String fieldName
	) {
		String normalized = requireNormalizedNonBlank(value, fieldName);
		return requireLengthBetween(normalized, minLength, maxLength, fieldName);
	}

	public static String requireNormalizedNonBlankLengthBetweenOrDefault(
		String value,
		String defaultValue,
		int minLength,
		int maxLength,
		String fieldName
	) {
		String resolved = defaultIfBlank(value, defaultValue);
		return requireNormalizedNonBlankLengthBetween(resolved, minLength, maxLength, fieldName);
	}

	public static int requireNonNegative(int value, String fieldName) {
		if (value < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}

		return value;
	}

	public static long requireNonNegative(long value, String fieldName) {
		if (value < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}

		return value;
	}

	public static double requireNonNegative(double value, String fieldName) {
		if (value < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}

		return value;
	}

	public static int requirePositive(int value, String fieldName) {
		if (value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}

		return value;
	}

	public static void requireZero(long value, String fieldName) {
		if (value != 0L) {
			throw new IllegalArgumentException(fieldName + " must be 0 when sampleCount is 0");
		}
	}

	public static void requireZero(double value, String fieldName) {
		if (Double.compare(value, 0.0d) != 0) {
			throw new IllegalArgumentException(fieldName + " must be 0 when sampleCount is 0");
		}
	}

	public static <T> List<T> requireNonEmptyCopy(Collection<T> values, String fieldName) {
		requireNonNull(values, fieldName);

		if (values.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be empty");
		}

		return new ArrayList<>(values);
	}

	public static <T> List<T> copyOfNullable(Collection<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}

	public static <T> List<T> requireCopy(Collection<T> values, String fieldName) {
		requireNonNull(values, fieldName);
		return List.copyOf(values);
	}

	public static void requireLessThanOrEqual(long min, long max, String minFieldName, String maxFieldName) {
		if (min > max) {
			throw new IllegalArgumentException(
				minFieldName + " must be less than or equal to " + maxFieldName
			);
		}
	}

	public static void requireLessThanOrEqual(
		double min,
		double max,
		String minFieldName,
		String maxFieldName
	) {
		if (min > max) {
			throw new IllegalArgumentException(
				minFieldName + " must be less than or equal to " + maxFieldName
			);
		}
	}

	public static <T> void requireUniqueIds(
		Collection<T> items,
		Function<T, String> idGetter,
		String fieldName
	) {
		if (items == null || items.isEmpty()) return;

		Set<String> seenIds = new HashSet<>(items.size());

		for (T item : items) {
			String id = idGetter.apply(item);

			if (!seenIds.add(id)) {
				throw new IllegalArgumentException("Duplicate " + fieldName + " id: '" + id + "'");
			}
		}
	}
}
