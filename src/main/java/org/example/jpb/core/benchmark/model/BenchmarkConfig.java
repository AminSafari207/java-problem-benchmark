package org.example.jpb.core.benchmark.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.example.jpb.util.ModelChecks;

@Value
public final class BenchmarkConfig {

	private static final int DEFAULT_WARMUP_ITERATIONS = 1_000;
	private static final int DEFAULT_MEASUREMENT_ITERATIONS = 10_000;

	private static final int DEFAULT_FORK_COUNT = 5;
	private static final int MIN_FORK_COUNT = 0;
	private static final int MAX_FORK_COUNT = 10;

	private static final long DEFAULT_FORK_TIMEOUT_MILLIS = 60_000L;
	private static final long MIN_FORK_TIMEOUT_MILLIS = 1_000L;
	private static final long MAX_FORK_TIMEOUT_MILLIS = 600_000L;

	private final int warmupIterations;
	private final int measurementIterations;
	private final int forkCount;
	private final long forkTimeoutMillis;

	@Builder
	private BenchmarkConfig(
		Integer warmupIterations,
		Integer measurementIterations,
		Integer forkCount,
		Long forkTimeoutMillis
	) {
		this.warmupIterations = ModelChecks.requireNonNegative(
			ModelChecks.defaultIfNull(warmupIterations, DEFAULT_WARMUP_ITERATIONS),
			"warmupIterations"
		);
		this.measurementIterations = ModelChecks.requirePositive(
			ModelChecks.defaultIfNull(
				measurementIterations,
				DEFAULT_MEASUREMENT_ITERATIONS
			),
			"measurementIterations"
		);
		this.forkCount = ModelChecks.requireInRange(
			ModelChecks.defaultIfNull(forkCount, DEFAULT_FORK_COUNT),
			MIN_FORK_COUNT,
			MAX_FORK_COUNT,
			"forkCount"
		);
		this.forkTimeoutMillis = ModelChecks.requireInRange(
			ModelChecks.defaultIfNull(forkTimeoutMillis, DEFAULT_FORK_TIMEOUT_MILLIS),
			MIN_FORK_TIMEOUT_MILLIS,
			MAX_FORK_TIMEOUT_MILLIS,
			"forkTimeoutMillis"
		);
	}
}
