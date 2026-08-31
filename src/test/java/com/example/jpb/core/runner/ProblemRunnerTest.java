package com.example.jpb.core.runner;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.example.jpb.annotation.CaseSet;
import org.example.jpb.annotation.Contract;
import org.example.jpb.annotation.Problem;
import org.example.jpb.annotation.Solution;
import org.example.jpb.core.benchmark.enums.BenchmarkStatus;
import org.example.jpb.core.benchmark.model.BenchmarkConfig;
import org.example.jpb.core.benchmark.model.ProblemBenchmarkResult;
import org.example.jpb.core.execution.ProblemExecutor;
import org.example.jpb.core.execution.model.ProblemExecutionResult;
import org.example.jpb.core.runner.model.ProblemResult;
import org.example.jpb.core.shared.model.Arguments;
import org.example.jpb.core.shared.model.TestCase;
import org.junit.jupiter.api.Test;

class ProblemRunnerTest {

	@Problem(id = "problem-dummy", displayName = "Dummy")
	@Contract(accepts = { Integer.class }, expects = Integer.class)
	static class DummyProblem {

		@CaseSet(id = "case-set-01", displayName = "Case set 01")
		public List<TestCase> cases() {
			return List.of(
				TestCase.of("one", Arguments.single(1), 2),
				TestCase.of("two", Arguments.single(2), 3)
			);
		}

		@CaseSet(id = "case-set-02")
		public List<TestCase> cases2 = List.of(
			TestCase.of("three", Arguments.single(150), 151),
			TestCase.of("four", Arguments.single(300), 301)
		);

		@Solution(id = "add-one", displayName = "Add one")
		public Integer addOne(Integer input) {
			return input + 1;
		}

		@Solution(id = "add-two-subtract-one")
		public Integer addTwoSubOne(Integer input) {
			if (input == 300) return input + 2;

			return input + 2 - 1;
		}
	}

	@Test
	void shouldRunCorrectnessChecksAndBenchmarkOnlyPassingSolutions() {
		BenchmarkConfig benchmarkConfig = BenchmarkConfig.builder()
			.warmupIterations(1)
			.measurementIterations(1)
			.forkCount(0)
			.build();

		ProblemExecutor problemExecutor = new ProblemExecutor();
		ProblemExecutionResult executionResult = problemExecutor.execute(
			DummyProblem.class,
			benchmarkConfig
		);

		ProblemResult problemResult = executionResult.getProblemResult();
		ProblemBenchmarkResult benchmarkResult =
			executionResult.getProblemBenchmarkResult();

		assertNotNull(problemResult);
		assertEquals("Dummy", problemResult.getProblemDisplayName());
		assertEquals(2, problemResult.getSolutionResults().size());

		assertNotNull(benchmarkResult);
		assertEquals(1, benchmarkResult.getSolutionBenchmarkResults().size());
		assertEquals(
			"add-one",
			benchmarkResult.getSolutionBenchmarkResults().getFirst().getSolutionId()
		);
		assertEquals(
			BenchmarkStatus.SUCCESS,
			benchmarkResult.getSolutionBenchmarkResults().getFirst().getStatus()
		);
	}
}
