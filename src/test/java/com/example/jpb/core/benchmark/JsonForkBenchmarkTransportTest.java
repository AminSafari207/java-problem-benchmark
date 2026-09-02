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

	@Test
	void shouldSerializeAndDeserializeRequest() throws Exception {
		BenchmarkConfig config = BenchmarkConfig.builder()
			.warmupIterations(3)
			.measurementIterations(5)
			.forkCount(1)
			.build();

		ForkBenchmarkRequest request = ForkBenchmarkRequest.builder()
			.forkIndex(0)
			.problemId("two-sum")
			.problemClassName("DummyProblem")
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
	void shouldSerializeAndDeserializeResult() throws Exception {
		String ERROR_MESSAGE = "ERROR_MESSAGE";

		ForkBenchmarkResult result = ForkBenchmarkResult.builder()
			.forkIndex(0)
			.status(BenchmarkStatus.FAILED)
			.errorMessage(ERROR_MESSAGE)
			.build();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		transport.writeResult(output, result);

		ForkBenchmarkResult actual = transport.readResult(
			new ByteArrayInputStream(output.toByteArray())
		);

		assertEquals(result, actual);
	}
}
