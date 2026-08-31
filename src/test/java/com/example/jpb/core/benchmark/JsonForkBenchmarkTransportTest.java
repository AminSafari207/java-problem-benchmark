package com.example.jpb.core.benchmark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.example.jpb.annotation.CaseSet;
import org.example.jpb.annotation.Contract;
import org.example.jpb.annotation.Problem;
import org.example.jpb.annotation.Solution;
import org.example.jpb.core.benchmark.JavaForkProcessLauncher;
import org.example.jpb.core.benchmark.JsonForkBenchmarkTransport;
import org.example.jpb.core.benchmark.enums.BenchmarkStatus;
import org.example.jpb.core.benchmark.model.BenchmarkConfig;
import org.example.jpb.core.benchmark.model.ForkBenchmarkRequest;
import org.example.jpb.core.benchmark.model.ForkBenchmarkResult;
import org.example.jpb.core.benchmark.model.ForkBenchmarkTransport;
import org.example.jpb.core.shared.model.Arguments;
import org.example.jpb.core.shared.model.TestCase;
import org.junit.jupiter.api.Test;

class JsonForkBenchmarkTransportTest {

	private final ForkBenchmarkTransport transport =
		new JsonForkBenchmarkTransport();

	@Problem(id = "dummy-problem", displayName = "Dummy")
	@Contract(accepts = { Integer.class }, expects = Integer.class)
	static class DummyProblem {

		@CaseSet(id = "case-set-01")
		public List<TestCase> cases2 = List.of(
			TestCase.of("three", Arguments.single(1), 1)
		);

		@Solution(id = "return-one")
		public Integer solve(Integer input) {
			return 1;
		}
	}

	@Test
	void shouldWriteAndReadRequest() throws Exception {
		BenchmarkConfig config = BenchmarkConfig.builder()
			.warmupIterations(3)
			.measurementIterations(5)
			.forkCount(1)
			.build();

		ForkBenchmarkRequest request = ForkBenchmarkRequest.builder()
			.forkIndex(0)
			.problemId("two-sum")
			.problemClassName("org.example.jpb.problems.TwoSumProblem")
			.solutionId("hash-map")
			.config(config)
			.build();

		ByteArrayOutputStream output = new ByteArrayOutputStream();

		transport.writeRequest(output, request);

		ForkBenchmarkRequest actual = transport.readRequest(
			new ByteArrayInputStream(output.toByteArray())
		);

		assertEquals(request, actual);
	}

	@Test
	void launchReturnsWorkerResult() throws IOException {
		BenchmarkConfig config = BenchmarkConfig.builder()
			.warmupIterations(1)
			.measurementIterations(2)
			.forkCount(1)
			.forkTimeoutMillis(5_000L)
			.build();

		ForkBenchmarkRequest request = ForkBenchmarkRequest.builder()
			.forkIndex(3)
			.problemId("dummy-problem")
			.problemClassName(DummyProblem.class.getName())
			.solutionId("return-one")
			.config(config)
			.build();

		JavaForkProcessLauncher launcher = new JavaForkProcessLauncher(
			new JsonForkBenchmarkTransport()
		);

		ForkBenchmarkResult result = launcher.launch(request);

		assertEquals(BenchmarkStatus.SUCCESS, result.getStatus());
		assertEquals(3, result.getForkIndex());
		assertEquals(2, result.getStats().getSampleCount());
	}
}
