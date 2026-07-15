package org.example.jpb.core.benchmark.model;

import lombok.Builder;
import lombok.Getter;
import org.example.jpb.util.ModelChecks;

@Getter
public final class BenchmarkConfig {

	private static final int DEFAULT_WARMUP_ITERATIONS = 1_000;
	private static final int DEFAULT_MEASUREMENT_ITERATIONS = 10_000;

	private final int warmupIterations;
	private final int measurementIterations;

	@Builder
	private BenchmarkConfig(Integer warmupIterations, Integer measurementIterations) {
		this.warmupIterations =
			ModelChecks.requireNonNegative(
				ModelChecks.defaultIfNull(warmupIterations, DEFAULT_WARMUP_ITERATIONS),
				"warmupIterations"
			);
		this.measurementIterations =
			ModelChecks.requirePositive(
				ModelChecks.defaultIfNull(measurementIterations, DEFAULT_MEASUREMENT_ITERATIONS),
				"measurementIterations"
			);
	}
}
