package org.example.jpb;

import org.example.jpb.core.benchmark.model.BenchmarkConfig;
import org.example.jpb.core.benchmark.model.ProblemBenchmarkResult;
import org.example.jpb.core.execution.ProblemExecutor;
import org.example.jpb.core.execution.model.ProblemExecutionResult;
import org.example.jpb.core.runner.model.ProblemResult;
import org.example.jpb.problems.TwoSumProblem;
import org.example.jpb.render.console.ProblemConsoleRenderer;
import org.example.jpb.render.model.ConsoleRenderOptions;

public class App {

	public static void main(String[] args) {
		BenchmarkConfig benchmarkConfig = BenchmarkConfig.builder()
			.warmupIterations(10_000)
			.measurementIterations(100_000)
			.forkCount(0)
			.build();

		ConsoleRenderOptions consoleRenderOptions = ConsoleRenderOptions.builder()
			.showIds(true)
			.showPassedCaseSets(true)
			.showPassedTestCases(true)
			.showBenchmark(true)
			.build();

		ProblemExecutor problemExecutor = new ProblemExecutor();
		ProblemExecutionResult problemExecutionResult = problemExecutor.execute(
			TwoSumProblem.class,
			benchmarkConfig
		);

		ProblemResult problemResult = problemExecutionResult.getProblemResult();
		ProblemBenchmarkResult problemBenchmarkResult =
			problemExecutionResult.getProblemBenchmarkResult();

		ProblemConsoleRenderer renderer = new ProblemConsoleRenderer();

		renderer.render(
			problemResult,
			problemBenchmarkResult,
			consoleRenderOptions
		);
	}
}
