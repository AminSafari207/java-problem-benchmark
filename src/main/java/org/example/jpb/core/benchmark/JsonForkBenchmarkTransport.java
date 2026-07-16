package org.example.jpb.core.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.example.jpb.core.benchmark.model.ForkBenchmarkRequest;
import org.example.jpb.core.benchmark.model.ForkBenchmarkResult;
import org.example.jpb.core.benchmark.model.ForkBenchmarkTransport;

public final class JsonForkBenchmarkTransport implements ForkBenchmarkTransport {

	private final ObjectMapper objectMapper;

	public JsonForkBenchmarkTransport() {
		this(new ObjectMapper().registerModule(new ParameterNamesModule()));
	}

	JsonForkBenchmarkTransport(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void writeRequest(OutputStream outputStream, ForkBenchmarkRequest request) throws IOException {
		objectMapper.writeValue(outputStream, request);
	}

	@Override
	public ForkBenchmarkRequest readRequest(InputStream inputStream) throws IOException {
		return objectMapper.readValue(inputStream, ForkBenchmarkRequest.class);
	}

	@Override
	public void writeResult(OutputStream outputStream, ForkBenchmarkResult result) throws IOException {
		objectMapper.writeValue(outputStream, result);
	}

	@Override
	public ForkBenchmarkResult readResult(InputStream inputStream) throws IOException {
		return objectMapper.readValue(inputStream, ForkBenchmarkResult.class);
	}
}
