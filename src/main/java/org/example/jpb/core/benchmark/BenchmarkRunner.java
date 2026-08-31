package org.example.jpb.core.benchmark;

import java.util.ArrayList;
import java.util.List;
import org.example.jpb.core.benchmark.enums.BenchmarkStatus;
import org.example.jpb.core.benchmark.model.*;
import org.example.jpb.core.preparation.model.PreparedCaseSet;
import org.example.jpb.core.preparation.model.PreparedProblem;
import org.example.jpb.core.preparation.model.PreparedSolution;
import org.example.jpb.core.preparation.model.SolutionInvoker;
import org.example.jpb.core.shared.model.TestCase;
import org.example.jpb.util.ModelChecks;

public class BenchmarkRunner {

	private static volatile Object resultSink;

	private final ForkBenchmarkRunner forkBenchmarkRunner;

	public BenchmarkRunner() {
		this(new ForkBenchmarkRunner());
	}

	public BenchmarkRunner(ForkBenchmarkRunner forkBenchmarkRunner) {
		this.forkBenchmarkRunner = ModelChecks.requireNonNull(
			forkBenchmarkRunner,
			"forkBenchmarkRunner"
		);
	}

	public ProblemBenchmarkResult run(
		PreparedProblem preparedProblem,
		BenchmarkConfig config
	) {
		List<PreparedSolution> preparedSolutions = preparedProblem.getSolutions();

		validateInputs(preparedProblem, preparedSolutions, config);

		if (config.getForkCount() == 0) {
			return runInProcess(preparedProblem, config);
		}

		return runForked(preparedProblem, config);
	}

	private ProblemBenchmarkResult runInProcess(
		PreparedProblem preparedProblem,
		BenchmarkConfig config
	) {
		List<SolutionBenchmarkResult> solutionsBenchmarkResults = new ArrayList<>();

		for (PreparedSolution preparedSolution : preparedProblem.getSolutions()) {
			SolutionBenchmarkResult benchmarkResult = runSingleSolutionInProcess(
				preparedProblem,
				preparedSolution,
				config
			);

			solutionsBenchmarkResults.add(benchmarkResult);
		}

		return ProblemBenchmarkResult.builder()
			.problemId(preparedProblem.getId())
			.problemDisplayName(preparedProblem.getDisplayName())
			.solutionBenchmarkResults(solutionsBenchmarkResults)
			.build();
	}

	public SolutionBenchmarkResult runSingleSolutionInProcess(
		PreparedProblem preparedProblem,
		PreparedSolution preparedSolution,
		BenchmarkConfig config
	) {
		validateInputs(preparedProblem, List.of(preparedSolution), config);

		if (config.getForkCount() != 0) {
			throw new IllegalArgumentException(
				"Fork worker requires BenchmarkConfig with forkCount equal to zero"
			);
		}

		return benchmarkSolution(
			preparedProblem,
			preparedSolution,
			config.getWarmupIterations(),
			config.getMeasurementIterations()
		);
	}

	private ProblemBenchmarkResult runForked(
		PreparedProblem preparedProblem,
		BenchmarkConfig config
	) {
		List<SolutionBenchmarkResult> solutionsBenchmarkResults = new ArrayList<>();

		for (PreparedSolution preparedSolution : preparedProblem.getSolutions()) {
			List<ForkBenchmarkResult> forkResults = forkBenchmarkRunner.run(
				preparedProblem,
				preparedSolution,
				config
			);
			SolutionBenchmarkResult benchmarkResult = aggregateForkResults(
				preparedSolution,
				forkResults
			);

			solutionsBenchmarkResults.add(benchmarkResult);
		}

		return ProblemBenchmarkResult.builder()
			.problemId(preparedProblem.getId())
			.problemDisplayName(preparedProblem.getDisplayName())
			.solutionBenchmarkResults(solutionsBenchmarkResults)
			.build();
	}

	private SolutionBenchmarkResult aggregateForkResults(
		PreparedSolution preparedSolution,
		List<ForkBenchmarkResult> forkResults
	) {
		if (forkResults == null || forkResults.isEmpty()) {
			return failedForkAggregation(
				preparedSolution,
				"No fork benchmark results were returned"
			);
		}

		List<Long> allSamplesNanos = new ArrayList<>();

		for (ForkBenchmarkResult forkResult : forkResults) {
			if (forkResult == null) {
				return failedForkAggregation(
					preparedSolution,
					"A fork returned no benchmark result"
				);
			}

			if (forkResult.getStatus() != BenchmarkStatus.SUCCESS) {
				return failedForkAggregation(
					preparedSolution,
					formatForkFailure(forkResult)
				);
			}

			BenchmarkStats stats = forkResult.getStats();

			if (stats == null) {
				return failedForkAggregation(
					preparedSolution,
					"Fork " +
						forkResult.getForkIndex() +
						" succeeded without benchmark stats"
				);
			}

			List<Long> samplesNanos = stats.getSamplesNanos();

			if (samplesNanos == null || samplesNanos.isEmpty()) {
				return failedForkAggregation(
					preparedSolution,
					"Fork " +
						forkResult.getForkIndex() +
						" succeeded without measurement samples"
				);
			}

			allSamplesNanos.addAll(samplesNanos);
		}

		return SolutionBenchmarkResult.builder()
			.solutionId(preparedSolution.getId())
			.solutionDisplayName(preparedSolution.getDisplayName())
			.status(BenchmarkStatus.SUCCESS)
			.stats(BenchmarkStats.of(allSamplesNanos))
			.errorMessage(null)
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

			BenchmarkStats stats = runMeasurement(
				preparedProblem,
				preparedSolution,
				measurementIterations
			);

			return SolutionBenchmarkResult.builder()
				.solutionId(preparedSolution.getId())
				.solutionDisplayName(solutionName)
				.status(BenchmarkStatus.SUCCESS)
				.stats(stats)
				.errorMessage(null)
				.build();
		} catch (Exception e) {
			return SolutionBenchmarkResult.builder()
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
			Object[][] argumentsByCase = cloneAllArguments(preparedProblem);

			runAllCases(preparedSolution, argumentsByCase);
		}
	}

	private BenchmarkStats runMeasurement(
		PreparedProblem preparedProblem,
		PreparedSolution preparedSolution,
		int measurementIterations
	) {
		List<Long> samplesNanos = new ArrayList<>(measurementIterations);

		for (int i = 0; i < measurementIterations; i++) {
			Object[][] argumentsByCase = cloneAllArguments(preparedProblem);

			long startNanos = System.nanoTime();

			runAllCases(preparedSolution, argumentsByCase);

			samplesNanos.add(System.nanoTime() - startNanos);
		}

		return BenchmarkStats.of(samplesNanos);
	}

	private void runAllCases(
		PreparedSolution preparedSolution,
		Object[][] argumentsByCase
	) {
		SolutionInvoker invoker = preparedSolution.getInvoker();

		for (Object[] arguments : argumentsByCase) {
			resultSink = invoker.invoke(arguments);
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
			throw new IllegalArgumentException(
				"warmupIterations must not be negative"
			);
		}

		if (config.getMeasurementIterations() <= 0) {
			throw new IllegalArgumentException(
				"measurementIterations must be greater than zero"
			);
		}
	}

	//#################################
	//## Helpers ######################
	//#################################

	private Object[][] cloneAllArguments(PreparedProblem preparedProblem) {
		int testCaseCount = 0;

		for (PreparedCaseSet preparedCaseSet : preparedProblem.getCaseSets()) {
			testCaseCount = Math.addExact(
				testCaseCount,
				preparedCaseSet.getTestCases().size()
			);
		}

		Object[][] argumentsByCase = new Object[testCaseCount][];
		int index = 0;

		for (PreparedCaseSet preparedCaseSet : preparedProblem.getCaseSets()) {
			for (TestCase testCase : preparedCaseSet.getTestCases()) {
				argumentsByCase[index++] = testCase.getDeepClonedArguments();
			}
		}

		return argumentsByCase;
	}

	private SolutionBenchmarkResult failedForkAggregation(
		PreparedSolution preparedSolution,
		String errorMessage
	) {
		return SolutionBenchmarkResult.builder()
			.solutionId(preparedSolution.getId())
			.solutionDisplayName(preparedSolution.getDisplayName())
			.status(BenchmarkStatus.FAILED)
			.stats(null)
			.errorMessage(errorMessage)
			.build();
	}

	private String formatForkFailure(ForkBenchmarkResult forkResult) {
		String errorMessage = forkResult.getErrorMessage();

		if (errorMessage == null || errorMessage.isBlank()) {
			errorMessage = "No error message was provided";
		}

		return "Fork " + forkResult.getForkIndex() + " failed: " + errorMessage;
	}
}
