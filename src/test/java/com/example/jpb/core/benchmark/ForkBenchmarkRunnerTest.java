package com.example.jpb.core.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import org.example.jpb.core.benchmark.enums.BenchmarkStatus;
import org.example.jpb.core.benchmark.model.BenchmarkConfig;
import org.example.jpb.core.benchmark.model.ProblemBenchmarkResult;
import org.example.jpb.core.benchmark.model.SolutionBenchmarkResult;
import org.example.jpb.core.execution.ProblemExecutor;
import org.example.jpb.core.execution.model.ProblemExecutionResult;
import org.example.jpb.problems.DummyProblem;
import org.junit.jupiter.api.Test;

class ForkBenchmarkRunnerTest {

	@Test
	void shouldReturnForkResultWhenOutputExceedsPipeBuffer() {
		int measurementIterations = 20_000;

		BenchmarkConfig benchmarkConfig = BenchmarkConfig.builder()
			.warmupIterations(1)
			.measurementIterations(measurementIterations)
			.forkCount(1)
			.build();

		ProblemExecutionResult executionResult = new ProblemExecutor().execute(
			DummyProblem.class,
			benchmarkConfig
		);
		ProblemBenchmarkResult benchmarkResult =
			executionResult.getProblemBenchmarkResult();

		assertEquals(1, benchmarkResult.getSolutionBenchmarkResults().size());

		SolutionBenchmarkResult solutionResult = benchmarkResult
			.getSolutionBenchmarkResults()
			.getFirst();

		assertEquals(BenchmarkStatus.SUCCESS, solutionResult.getStatus());
		assertNotNull(solutionResult.getStats());
		assertEquals(
			measurementIterations,
			solutionResult.getStats().getSampleCount()
		);
	}
}
