package org.example.jpb.core.benchmark.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.example.jpb.core.shared.constants.ModelLimits;
import org.example.jpb.util.ModelChecks;

@Value
public class ForkBenchmarkRequest {

	private final int forkIndex;
	private final String problemId;
	private final String problemClassName;
	private final String solutionId;
	private final BenchmarkConfig config;

	@Builder
	@JsonCreator
	public ForkBenchmarkRequest(
		@JsonProperty("forkIndex") int forkIndex,
		@JsonProperty("problemId") String problemId,
		@JsonProperty("problemClassName") String problemClassName,
		@JsonProperty("solutionId") String solutionId,
		@JsonProperty("config") BenchmarkConfig config
	) {
		this.forkIndex = ModelChecks.requireNonNegative(forkIndex, "forkIndex");
		this.problemId =
			ModelChecks.requireNormalizedNonBlankLengthBetween(
				problemId,
				ModelLimits.ID_MIN_LENGTH,
				ModelLimits.ID_MAX_LENGTH,
				"problemId"
			);
		this.problemClassName = ModelChecks.requireNormalizedNonBlank(problemClassName, "problemClassName");
		this.solutionId =
			ModelChecks.requireNormalizedNonBlankLengthBetween(
				solutionId,
				ModelLimits.ID_MIN_LENGTH,
				ModelLimits.ID_MAX_LENGTH,
				"solutionId"
			);
		this.config = ModelChecks.requireNonNull(config, "config");
	}
}
