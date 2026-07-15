package org.example.jpb.core.shared.model;

public final class Arguments {

	private static final Arguments NONE = new Arguments(new Object[0]);

	private final Object[] values;

	private Arguments(Object[] values) {
		this.values = values.clone();
	}

	public static Arguments none() {
		return NONE;
	}

	public static Arguments of(Object first, Object... rest) {
		int len = 1 + rest.length;
		Object[] arr = new Object[len];
		arr[0] = first;

		System.arraycopy(rest, 0, arr, 1, rest.length);

		return new Arguments(arr);
	}

	public static Arguments single(Object value) {
		return new Arguments(new Object[] { value });
	}

	public Object[] values() {
		return values.clone();
	}

	public Object[] deepValues() {
		Object[] copy = new Object[values.length];

		for (int i = 0; i < values.length; i++) {
			copy[i] = deepCopyValue(values[i]);
		}

		return copy;
	}

	public int size() {
		return values.length;
	}

	public Object get(int index) {
		return values[index];
	}

	private Object deepCopyValue(Object value) {
		if (value == null) {
			return null;
		}

		Class<?> type = value.getClass();

		if (!type.isArray()) {
			return value;
		}

		if (value instanceof int[] arr) return arr.clone();
		if (value instanceof long[] arr) return arr.clone();
		if (value instanceof double[] arr) return arr.clone();
		if (value instanceof float[] arr) return arr.clone();
		if (value instanceof boolean[] arr) return arr.clone();
		if (value instanceof byte[] arr) return arr.clone();
		if (value instanceof short[] arr) return arr.clone();
		if (value instanceof char[] arr) return arr.clone();

		Object[] arr = (Object[]) value;
		Object[] copy = new Object[arr.length];

		for (int i = 0; i < arr.length; i++) {
			copy[i] = deepCopyValue(arr[i]);
		}

		return copy;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Arguments(");

		for (int i = 0; i < values.length; i++) {
			sb.append(values[i]);

			if (i + 1 < values.length) {
				sb.append(", ");
			}
		}

		sb.append(")");

		return sb.toString();
	}
}
