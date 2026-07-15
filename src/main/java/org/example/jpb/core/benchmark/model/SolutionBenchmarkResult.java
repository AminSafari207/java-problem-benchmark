package org.example.jpb.core.benchmark.model;

import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.shared.constants.ModelLimits;
import org.example.jpb.core.benchmark.enums.BenchmarkStatus;
import org.example.jpb.util.ModelChecks;

@Getter
public class SolutionBenchmarkResult {

	private final String solutionId;
	private final String solutionDisplayName;
	private final BenchmarkStatus status;
	private final BenchmarkStats stats;
	private final String errorMessage;

	@Builder
	private SolutionBenchmarkResult(
		String solutionId,
		String solutionDisplayName,
		BenchmarkStatus status,
		BenchmarkStats stats,
		String errorMessage
	) {
		this.solutionId =
			ModelChecks.requireNormalizedNonBlankLengthBetween(
				solutionId,
				ModelLimits.ID_MIN_LENGTH,
				ModelLimits.ID_MAX_LENGTH,
				"solutionId"
			);
		this.solutionDisplayName =
			ModelChecks.requireNormalizedNonBlankLengthBetweenOrDefault(
				solutionDisplayName,
				this.solutionId,
				ModelLimits.DISPLAY_NAME_MIN_LENGTH,
				ModelLimits.DISPLAY_NAME_MAX_LENGTH,
				"solutionDisplayName"
			);
		this.status = ModelChecks.requireNonNull(status, "status");
		this.stats = stats;
		this.errorMessage = errorMessage;
	}

	public boolean isSuccess() {
		return status == BenchmarkStatus.SUCCESS;
	}

	public boolean isFailed() {
		return status == BenchmarkStatus.FAILED;
	}
}
