package org.example.jpb.core.benchmark.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.benchmark.enums.ForkBenchmarkStatus;
import org.example.jpb.util.ModelChecks;

@Getter
public class ForkBenchmarkResult {

	private final int forkIndex;
	private final ForkBenchmarkStatus status;
	private final BenchmarkStats stats;
	private final long gcCountDelta;
	private final long gcTimeMillisDelta;
	private final String errorMessage;

	@Builder
	private ForkBenchmarkResult(
		int forkIndex,
		ForkBenchmarkStatus status,
		BenchmarkStats stats,
		long gcCountDelta,
		long gcTimeMillisDelta,
		String errorMessage
	) {
		this.forkIndex = ModelChecks.requireNonNegative(forkIndex, "forkIndex");
		this.status = ModelChecks.requireNonNull(status, "status");
		this.stats = stats;
		this.gcCountDelta = ModelChecks.requireNonNegative(gcCountDelta, "gcCountDelta");
		this.gcTimeMillisDelta = ModelChecks.requireNonNegative(gcTimeMillisDelta, "gcTimeMillisDelta");
		this.errorMessage = errorMessage;

		validateConsistency();
	}

	private void validateConsistency() {
		if (status == ForkBenchmarkStatus.SUCCESS) {
			ModelChecks.requireNonNull(stats, "stats");
		}

		if (status == ForkBenchmarkStatus.FAILED) {
			ModelChecks.requireNonBlank(errorMessage, "errorMessage");
		}
	}

	@JsonIgnore
	public boolean isSuccess() {
		return status == ForkBenchmarkStatus.SUCCESS;
	}

	@JsonIgnore
	public boolean isFailed() {
		return status == ForkBenchmarkStatus.FAILED;
	}
}
