package org.example.jpb.core.benchmark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.example.jpb.core.benchmark.enums.ForkBenchmarkStatus;
import org.example.jpb.core.benchmark.model.BenchmarkConfig;
import org.example.jpb.core.benchmark.model.ForkBenchmarkRequest;
import org.example.jpb.core.benchmark.model.ForkBenchmarkResult;
import org.example.jpb.core.preparation.model.PreparedProblem;
import org.example.jpb.core.preparation.model.PreparedSolution;

public class ForkBenchmarkRunner {

	private final JavaForkProcessLauncher processLauncher;

	public ForkBenchmarkRunner() {
		this(new JavaForkProcessLauncher(new JsonForkBenchmarkTransport()));
	}

	ForkBenchmarkRunner(JavaForkProcessLauncher processLauncher) {
		this.processLauncher = processLauncher;
	}

	public List<ForkBenchmarkResult> run(
		PreparedProblem preparedProblem,
		PreparedSolution preparedSolution,
		BenchmarkConfig config
	) {
		List<ForkBenchmarkResult> results = new ArrayList<>(config.getForkCount());

		for (int forkIndex = 0; forkIndex < config.getForkCount(); forkIndex++) {
			ForkBenchmarkRequest request = ForkBenchmarkRequest
				.builder()
				.forkIndex(forkIndex)
				.problemId(preparedProblem.getId())
				.problemClassName(preparedProblem.getProblemClass().getName())
				.solutionId(preparedSolution.getId())
				.config(config)
				.build();

			results.add(runFork(request));
		}

		return results;
	}

	private ForkBenchmarkResult runFork(ForkBenchmarkRequest request) {
		try {
			return processLauncher.launch(request);
		} catch (IOException exception) {
			return ForkBenchmarkResult
				.builder()
				.forkIndex(request.getForkIndex())
				.status(ForkBenchmarkStatus.FAILED)
				.errorMessage("Could not start or communicate with fork process: " + exception.getMessage())
				.build();
		}
	}
}
