package org.example.jpb.core.benchmark.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ForkBenchmarkTransport {
	void writeRequest(OutputStream outputStream, ForkBenchmarkRequest request) throws IOException;

	ForkBenchmarkRequest readRequest(InputStream inputStream) throws IOException;

	void writeResult(OutputStream outputStream, ForkBenchmarkResult result) throws IOException;

	ForkBenchmarkResult readResult(InputStream inputStream) throws IOException;
}
