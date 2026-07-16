package org.example.jpb.core.benchmark;

import org.example.jpb.core.benchmark.enums.BenchmarkStatus;
import org.example.jpb.core.benchmark.enums.ForkBenchmarkStatus;
import org.example.jpb.core.benchmark.model.*;
import org.example.jpb.core.preparation.ProblemPreparator;
import org.example.jpb.core.preparation.model.PreparedProblem;
import org.example.jpb.core.preparation.model.PreparedSolution;

public final class ForkBenchmarkWorker {

	private ForkBenchmarkWorker() {
		throw new AssertionError("No instances");
	}

	public static void main(String[] args) {
		ForkBenchmarkTransport transport = new JsonForkBenchmarkTransport();

		try {
			ForkBenchmarkRequest request = transport.readRequest(System.in);
			ForkBenchmarkResult result = run(request);

			transport.writeResult(System.out, result);
		} catch (Exception exception) {
			System.err.println("Fork benchmark worker failed: " + exception.getMessage());
			System.exit(1);
		}
	}

	private static ForkBenchmarkResult run(ForkBenchmarkRequest request) {
		try {
			PreparedProblem preparedProblem = prepareProblem(request);
			PreparedSolution preparedSolution = findSolution(preparedProblem, request);

			BenchmarkConfig inProcessConfig = BenchmarkConfig
				.builder()
				.warmupIterations(request.getConfig().getWarmupIterations())
				.measurementIterations(request.getConfig().getMeasurementIterations())
				.forkCount(0)
				.forkTimeoutMillis(request.getConfig().getForkTimeoutMillis())
				.build();

			SolutionBenchmarkResult benchmarkResult = new BenchmarkRunner()
				.runSingleSolutionInProcess(preparedProblem, preparedSolution, inProcessConfig);

			if (benchmarkResult.getStatus() == BenchmarkStatus.FAILED) {
				return ForkBenchmarkResult
					.builder()
					.forkIndex(request.getForkIndex())
					.status(ForkBenchmarkStatus.FAILED)
					.errorMessage(benchmarkResult.getErrorMessage())
					.build();
			}

			return ForkBenchmarkResult
				.builder()
				.forkIndex(request.getForkIndex())
				.status(ForkBenchmarkStatus.SUCCESS)
				.stats(benchmarkResult.getStats())
				.build();
		} catch (Exception exception) {
			return ForkBenchmarkResult
				.builder()
				.forkIndex(request.getForkIndex())
				.status(ForkBenchmarkStatus.FAILED)
				.errorMessage(buildErrorMessage(exception))
				.build();
		}
	}

	private static PreparedProblem prepareProblem(ForkBenchmarkRequest request)
		throws ClassNotFoundException {
		Class<?> problemClass = Class.forName(request.getProblemClassName());

		PreparedProblem preparedProblem = new ProblemPreparator().prepare(problemClass);

		if (!preparedProblem.getId().equals(request.getProblemId())) {
			throw new IllegalStateException(
				"Problem class '" +
				request.getProblemClassName() +
				"' resolved to problem ID '" +
				preparedProblem.getId() +
				"', but request expected '" +
				request.getProblemId() +
				"'"
			);
		}

		return preparedProblem;
	}

	private static PreparedSolution findSolution(
		PreparedProblem preparedProblem,
		ForkBenchmarkRequest request
	) {
		return preparedProblem
			.getSolutions()
			.stream()
			.filter(solution -> solution.getId().equals(request.getSolutionId()))
			.findFirst()
			.orElseThrow(() ->
				new IllegalArgumentException(
					"Solution '" +
					request.getSolutionId() +
					"' was not found for problem '" +
					request.getProblemId() +
					"'"
				)
			);
	}

	//#################################
	//## Helpers ######################
	//#################################

	private static String buildErrorMessage(Exception exception) {
		String message = exception.getMessage();

		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}

		return exception.getClass().getSimpleName() + ": " + message;
	}
}
