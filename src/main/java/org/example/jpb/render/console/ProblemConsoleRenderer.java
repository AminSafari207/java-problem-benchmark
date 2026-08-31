package org.example.jpb.render.console;

import java.util.Arrays;
import org.example.jpb.core.benchmark.model.BenchmarkStats;
import org.example.jpb.core.benchmark.model.ProblemBenchmarkResult;
import org.example.jpb.core.benchmark.model.SolutionBenchmarkResult;
import org.example.jpb.core.runner.model.CaseSetResult;
import org.example.jpb.core.runner.model.ProblemResult;
import org.example.jpb.core.runner.model.SolutionResult;
import org.example.jpb.core.runner.model.TestCaseResult;
import org.example.jpb.render.constants.ConsoleLayout;
import org.example.jpb.render.model.ConsoleRenderOptions;
import org.example.jpb.util.Console;

public class ProblemConsoleRenderer {

	public void render(ProblemResult problemResult, ConsoleRenderOptions config) {
		renderProblemResult(problemResult, config);
	}

	public void render(ProblemBenchmarkResult benchmarkResult, ConsoleRenderOptions config) {
		renderBenchmarkResult(benchmarkResult, config);
	}

	public void render(
		ProblemResult problemResult,
		ProblemBenchmarkResult benchmarkResult,
		ConsoleRenderOptions config
	) {
		renderProblemResult(problemResult, config);
		renderBenchmarkResult(benchmarkResult, config);
	}

	//#################################
	//## Problem Correctness ##########
	//#################################

	public void renderProblemResult(ProblemResult result) {
		renderProblemResult(result, ConsoleRenderOptions.defaults());
	}

	public void renderProblemResult(ProblemResult result, ConsoleRenderOptions options) {
		renderProblemHeader(result);

		Console.section("Correctness", ConsoleLayout.DEFAULT_WIDTH, 0);

		for (SolutionResult solution : result.getSolutionResults()) {
			renderSolution(solution, options);
		}
	}

	private void renderProblemHeader(ProblemResult result) {
		Console.line();
		Console.boxTitle(
			"Problem: " + Console.blue(result.getProblemDisplayName()),
			ConsoleLayout.DEFAULT_WIDTH
		);
		Console.line();
	}

	private void renderSolution(SolutionResult solution, ConsoleRenderOptions options) {
		renderSolutionSummary(solution, options);

		for (CaseSetResult caseSetResult : solution.getCaseSetResults()) {
			if (!shouldRenderCaseSet(caseSetResult, options)) {
				continue;
			}

			renderCaseSet(caseSetResult, options);
		}

		Console.line();
	}

	private void renderSolutionSummary(SolutionResult solution, ConsoleRenderOptions options) {
		printCorrectnessRow(
			0,
			statusLabel(solution.isPassed()),
			"Solution: ",
			solution.getSolutionDisplayName(),
			options.isShowIds() ? solution.getSolutionId() : null,
			Console.gray(solution.passedCount() + "/" + solution.totalCount() + " passed")
		);
	}

	private boolean shouldRenderCaseSet(CaseSetResult caseSetResult, ConsoleRenderOptions options) {
		return !caseSetResult.isPassed() || options.isShowPassedCaseSets();
	}

	private void renderCaseSet(CaseSetResult caseSetResult, ConsoleRenderOptions options) {
		renderCaseSetSummary(caseSetResult, options);

		int renderedFailures = 0;
		int hiddenFailures = 0;

		for (TestCaseResult testCaseResult : caseSetResult.getTestCaseResults()) {
			if (!shouldRenderTestCase(testCaseResult, options)) {
				continue;
			}

			if (!testCaseResult.isPassed() && renderedFailures >= options.getMaxFailureDetails()) {
				hiddenFailures++;
				continue;
			}

			renderTestCase(testCaseResult, options);

			if (!testCaseResult.isPassed()) {
				renderedFailures++;
			}
		}

		if (hiddenFailures > 0) {
			Console.print(
				Console.indent(ConsoleLayout.INDENT_TEST_CASE) +
				Console.gray("... " + hiddenFailures + " more failure(s) hidden")
			);
		}
	}

	private void renderCaseSetSummary(CaseSetResult caseSetResult, ConsoleRenderOptions options) {
		printCorrectnessRow(
			ConsoleLayout.INDENT_CASE_SET,
			statusLabel(caseSetResult.isPassed()),
			"Case set: ",
			caseSetResult.getCaseSetDisplayName(),
			options.isShowIds() ? caseSetResult.getCaseSetId() : null,
			Console.gray(caseSetResult.passedCount() + "/" + caseSetResult.totalCount() + " passed")
		);
	}

	private boolean shouldRenderTestCase(TestCaseResult testCaseResult, ConsoleRenderOptions options) {
		return !testCaseResult.isPassed() || options.isShowPassedTestCases();
	}

	private void renderTestCase(TestCaseResult testCaseResult, ConsoleRenderOptions options) {
		printCorrectnessRow(
			ConsoleLayout.INDENT_TEST_CASE,
			statusLabel(testCaseResult.isPassed()),
			"Test: ",
			testCaseResult.getTestCaseDisplayName(),
			options.isShowIds() ? testCaseResult.getTestCaseId() : null,
			null
		);

		if (testCaseResult.isPassed()) {
			return;
		}

		renderFailedTestCaseDetails(testCaseResult);
	}

	private void renderFailedTestCaseDetails(TestCaseResult testCaseResult) {
		keyValue(ConsoleLayout.INDENT_DETAIL, "Expected", testCaseResult.getExpected());
		keyValue(ConsoleLayout.INDENT_DETAIL, "Actual", testCaseResult.getActual());
		Console.line();
	}

	//#################################
	//## Benchmark ####################
	//#################################

	public void renderBenchmarkResult(ProblemBenchmarkResult result) {
		renderBenchmarkResult(result, ConsoleRenderOptions.defaults());
	}

	public void renderBenchmarkResult(ProblemBenchmarkResult result, ConsoleRenderOptions options) {
		if (!options.isShowBenchmark()) {
			return;
		}

		renderBenchmarkHeader(result);

		Console.section("Benchmark", ConsoleLayout.BENCHMARK_WIDTH, 0);

		if (result.isSkipped()) {
			renderSkippedBenchmark(result);
			return;
		}

		renderBenchmarkTable(result);
	}

	private void renderBenchmarkHeader(ProblemBenchmarkResult result) {
		Console.line();
		Console.boxTitle(
			"Benchmark For Problem: " + Console.blue(result.getProblemDisplayName()),
			ConsoleLayout.BENCHMARK_WIDTH
		);
		Console.line();
	}

	private void renderSkippedBenchmark(ProblemBenchmarkResult result) {
		Console.print(Console.yellow("Skipped: ") + result.getSkipReason());
		Console.line();
	}

	private void renderBenchmarkTable(ProblemBenchmarkResult result) {
		renderBenchmarkTableHeader();

		result
			.getSolutionBenchmarkResults()
			.stream()
			.sorted(this::compareBenchmarkResults)
			.forEach(this::renderBenchmarkRow);

		Console.line();
	}

	private void renderBenchmarkTableHeader() {
		Console.print(
			Console.padRightTruncate("Status", ConsoleLayout.BENCHMARK_STATUS_WIDTH) +
			Console.padRightTruncate("Solution", ConsoleLayout.BENCHMARK_SOLUTION_WIDTH) +
			Console.padRightTruncate("Samples", ConsoleLayout.BENCHMARK_SAMPLES_WIDTH) +
			Console.padRightTruncate("Avg", ConsoleLayout.BENCHMARK_TIME_WIDTH) +
			Console.padRightTruncate("Min", ConsoleLayout.BENCHMARK_TIME_WIDTH) +
			Console.padRightTruncate("P50", ConsoleLayout.BENCHMARK_TIME_WIDTH) +
			Console.padRightTruncate("P95", ConsoleLayout.BENCHMARK_TIME_WIDTH) +
			Console.padRightTruncate("P99", ConsoleLayout.BENCHMARK_TIME_WIDTH) +
			Console.padRightTruncate("Max", ConsoleLayout.BENCHMARK_TIME_WIDTH)
		);

		Console.divider('─', ConsoleLayout.BENCHMARK_WIDTH);
	}

	private void renderBenchmarkRow(SolutionBenchmarkResult solution) {
		if (solution.isFailed()) {
			renderFailedBenchmarkRow(solution);
			return;
		}

		renderSuccessfulBenchmarkRow(solution);
	}

	private void renderFailedBenchmarkRow(SolutionBenchmarkResult solution) {
		Console.print(
			Console.padRightTruncate(Console.red("FAILED"), ConsoleLayout.BENCHMARK_STATUS_WIDTH) +
			Console.padColumnTruncate(
				solution.getSolutionDisplayName(),
				ConsoleLayout.BENCHMARK_SOLUTION_WIDTH
			) +
			Console.gray(Console.truncate(solution.getErrorMessage(), remainingBenchmarkErrorWidth()))
		);
	}

	private void renderSuccessfulBenchmarkRow(SolutionBenchmarkResult solution) {
		BenchmarkStats stats = solution.getStats();

		Console.print(
			Console.padRightTruncate(Console.green("SUCCESS"), ConsoleLayout.BENCHMARK_STATUS_WIDTH) +
			Console.padColumnTruncate(
				solution.getSolutionDisplayName(),
				ConsoleLayout.BENCHMARK_SOLUTION_WIDTH
			) +
			Console.padRightTruncate(
				String.valueOf(stats.getSampleCount()),
				ConsoleLayout.BENCHMARK_SAMPLES_WIDTH
			) +
			Console.padRightTruncate(
				formatNanos(Math.round(stats.getAverageNanos())),
				ConsoleLayout.BENCHMARK_TIME_WIDTH
			) +
			Console.padRightTruncate(formatNanos(stats.getMinNanos()), ConsoleLayout.BENCHMARK_TIME_WIDTH) +
			Console.padRightTruncate(
				formatNanos(stats.getMedianNanos()),
				ConsoleLayout.BENCHMARK_TIME_WIDTH
			) +
			Console.padRightTruncate(formatNanos(stats.getP95Nanos()), ConsoleLayout.BENCHMARK_TIME_WIDTH) +
			Console.padRightTruncate(formatNanos(stats.getP99Nanos()), ConsoleLayout.BENCHMARK_TIME_WIDTH) +
			Console.padRightTruncate(formatNanos(stats.getMaxNanos()), ConsoleLayout.BENCHMARK_TIME_WIDTH)
		);
	}

	private int remainingBenchmarkErrorWidth() {
		return (
			ConsoleLayout.BENCHMARK_WIDTH -
			ConsoleLayout.BENCHMARK_STATUS_WIDTH -
			ConsoleLayout.BENCHMARK_SOLUTION_WIDTH
		);
	}

	//#################################
	//## Helpers ######################
	//#################################

	private void printCorrectnessRow(
		int indent,
		String status,
		String labelPrefix,
		String displayName,
		String id,
		String score
	) {
		String nameColumn = quotedLabeledValue(
			labelPrefix,
			displayName,
			ConsoleLayout.CORRECTNESS_NAME_WIDTH,
			2
		);
		String idColumn = id == null
			? ""
			: quotedLabeledValue("id: ", id, ConsoleLayout.CORRECTNESS_ID_WIDTH, 2);
		String scoreColumn = score == null ? "" : score;

		String row =
			Console.padColumnTruncate(status, ConsoleLayout.CORRECTNESS_STATUS_WIDTH) +
			Console.padColumnTruncate(nameColumn, ConsoleLayout.CORRECTNESS_NAME_WIDTH) +
			Console.padColumnTruncate(idColumn, ConsoleLayout.CORRECTNESS_ID_WIDTH) +
			scoreColumn;

		Console.print(Console.indent(indent) + row);
	}

	private void keyValue(int indent, String key, Object value) {
		Console.print(Console.indent(indent) + String.format("%-9s -> %s", key, formatValue(value)));
	}

	private String formatValue(Object value) {
		if (value == null) return "null";

		if (value instanceof int[] v) return Arrays.toString(v);
		if (value instanceof long[] v) return Arrays.toString(v);
		if (value instanceof byte[] v) return Arrays.toString(v);
		if (value instanceof short[] v) return Arrays.toString(v);
		if (value instanceof char[] v) return Arrays.toString(v);
		if (value instanceof double[] v) return Arrays.toString(v);
		if (value instanceof float[] v) return Arrays.toString(v);
		if (value instanceof boolean[] v) return Arrays.toString(v);
		if (value instanceof Object[] v) return Arrays.deepToString(v);

		return value.toString();
	}

	private String formatDecimal(double value) {
		if (value >= 100) {
			return String.format("%.0f", value);
		}

		if (value >= 10) {
			return String.format("%.1f", value);
		}

		return String.format("%.3f", value);
	}

	private String formatNanos(long nanos) {
		if (nanos < 1_000) {
			return nanos + " ns";
		}

		if (nanos < 1_000_000) {
			return formatDecimal(nanos / 1_000.0) + " us";
		}

		if (nanos < 1_000_000_000) {
			return formatDecimal(nanos / 1_000_000.0) + " ms";
		}

		return formatDecimal(nanos / 1_000_000_000.0) + " s";
	}

	private String statusLabel(boolean passed) {
		return passed ? Console.green("[PASS]") : Console.red("[FAIL]");
	}

	private String quoted(String value) {
		return "\"" + (value == null ? "" : value) + "\"";
	}

	private String quotedLabeledValue(String label, String value, int columnWidth, int rightPadding) {
		String safeLabel = label == null ? "" : label;
		String safeValue = value == null ? "" : value;

		int contentWidth = columnWidth - rightPadding;
		if (contentWidth <= 0) {
			return "";
		}

		int availableValueWidth = contentWidth - safeLabel.length() - 2;
		if (availableValueWidth <= 0) {
			return Console.truncate(safeLabel, contentWidth);
		}

		String truncatedValue = Console.truncate(safeValue, availableValueWidth);
		return safeLabel + "\"" + truncatedValue + "\"";
	}

	private int compareBenchmarkResults(SolutionBenchmarkResult first, SolutionBenchmarkResult second) {
		if (first.isFailed() && second.isFailed()) {
			return first.getSolutionDisplayName().compareToIgnoreCase(second.getSolutionDisplayName());
		}

		if (first.isFailed()) {
			return 1;
		}

		if (second.isFailed()) {
			return -1;
		}

		return Double.compare(first.getStats().getAverageNanos(), second.getStats().getAverageNanos());
	}
}
