package org.example.jpb.core.benchmark.model;

import java.io.IOException;

public interface ForkProcessLauncher {
	ForkBenchmarkResult launch(ForkBenchmarkRequest request) throws IOException;
}
