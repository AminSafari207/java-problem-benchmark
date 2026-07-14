package org.example.jpb.core.benchmark;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.jpb.core.enums.BenchmarkStatus;
import org.example.jpb.core.model.*;
import org.example.jpb.util.ModelChecks;
import org.example.jpb.util.ReflectionExecutor;

public class BenchmarkRunner {

	public ProblemBenchmarkResult run(PreparedProblem preparedProblem, BenchmarkConfig config) {
		List<PreparedSolution> preparedSolutions = preparedProblem.getSolutions();

		validateInputs(preparedProblem, preparedSolutions, config);

		int warmupIterations = config.getWarmupIterations();
		int measurementIterations = config.getMeasurementIterations();

		List<SolutionBenchmarkResult> solutionsBenchmarkResults = new ArrayList<>();

		for (PreparedSolution preparedSolution : preparedSolutions) {
			SolutionBenchmarkResult benchmarkResult = benchmarkSolution(
				preparedProblem,
				preparedSolution,
				warmupIterations,
				measurementIterations
			);

			solutionsBenchmarkResults.add(benchmarkResult);
		}

		return ProblemBenchmarkResult
			.builder()
			.problemId(preparedProblem.getId())
			.problemDisplayName(preparedProblem.getDisplayName())
			.solutionBenchmarkResults(solutionsBenchmarkResults)
			.build();
	}

	private SolutionBenchmarkResult benchmarkSolution(
		PreparedProblem preparedProblem,
		PreparedSolution preparedSolution,
		int warmupIterations,
		int measurementIterations
	) {
		String solutionName = preparedSolution.getDisplayName();

		try {
			runWarmup(preparedProblem, preparedSolution, warmupIterations);

			BenchmarkStats stats = runMeasurement(preparedProblem, preparedSolution, measurementIterations);

			return SolutionBenchmarkResult
				.builder()
				.solutionId(preparedSolution.getId())
				.solutionDisplayName(solutionName)
				.status(BenchmarkStatus.SUCCESS)
				.stats(stats)
				.errorMessage(null)
				.build();
		} catch (Exception e) {
			return SolutionBenchmarkResult
				.builder()
				.solutionId(preparedSolution.getId())
				.solutionDisplayName(solutionName)
				.status(BenchmarkStatus.FAILED)
				.stats(null)
				.errorMessage(buildErrorMessage(e))
				.build();
		}
	}

	private void runWarmup(
		PreparedProblem preparedProblem,
		PreparedSolution preparedSolution,
		int warmupIterations
	) {
		for (int i = 0; i < warmupIterations; i++) {
			runAllCases(preparedProblem, preparedSolution);
		}
	}

	private BenchmarkStats runMeasurement(
		PreparedProblem preparedProblem,
		PreparedSolution preparedSolution,
		int measurementIterations
	) {
		List<Long> samplesNanos = new ArrayList<>(measurementIterations);

		for (int i = 0; i < measurementIterations; i++) {
			long startNanos = System.nanoTime();

			runAllCases(preparedProblem, preparedSolution);

			samplesNanos.add(System.nanoTime() - startNanos);
		}

		return BenchmarkStats.of(samplesNanos);
	}

	private void runAllCases(PreparedProblem preparedProblem, PreparedSolution preparedSolution) {
		Method method = preparedSolution.getSolutionMethod();
		Object instance = preparedProblem.getProblemInstance();

		for (PreparedCaseSet preparedCaseSet : preparedProblem.getCaseSets()) {
			for (TestCase testCase : preparedCaseSet.getTestCases()) {
				Object[] arguments = testCase.getDeepClonedArguments();

				ReflectionExecutor.invoke(instance, method, arguments);
			}
		}
	}

	private String buildErrorMessage(Exception e) {
		String message = e.getMessage();

		if (message == null || message.isBlank()) {
			return e.getClass().getSimpleName();
		}

		return e.getClass().getSimpleName() + ": " + message;
	}

	//#################################
	//## Validators ###################
	//#################################

	private void validateInputs(
		PreparedProblem preparedProblem,
		List<PreparedSolution> preparedSolutions,
		BenchmarkConfig config
	) {
		if (preparedProblem == null) {
			throw new IllegalArgumentException("preparedProblem must not be null");
		}

		if (preparedSolutions == null) {
			throw new IllegalArgumentException("preparedSolutions must not be null");
		}

		if (config == null) {
			throw new IllegalArgumentException("config must not be null");
		}

		if (config.getWarmupIterations() < 0) {
			throw new IllegalArgumentException("warmupIterations must not be negative");
		}

		if (config.getMeasurementIterations() <= 0) {
			throw new IllegalArgumentException("measurementIterations must be greater than zero");
		}
	}
}
