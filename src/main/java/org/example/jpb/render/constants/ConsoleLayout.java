package org.example.jpb.render.constants;

public final class ConsoleLayout {

	public static final int DEFAULT_WIDTH = 88;
	public static final int BENCHMARK_WIDTH = 120;

	public static final int CORRECTNESS_STATUS_WIDTH = 8;
	public static final int CORRECTNESS_NAME_WIDTH = 30;
	public static final int CORRECTNESS_ID_WIDTH = 30;

	public static final int INDENT_CASE_SET = 2;
	public static final int INDENT_TEST_CASE = 4;
	public static final int INDENT_DETAIL = 6;

	public static final int BENCHMARK_STATUS_WIDTH = 12;
	public static final int BENCHMARK_SOLUTION_WIDTH = 24;
	public static final int BENCHMARK_SAMPLES_WIDTH = 12;
	public static final int BENCHMARK_TIME_WIDTH = 12;

	private ConsoleLayout() {
		throw new AssertionError("No instances");
	}
}
