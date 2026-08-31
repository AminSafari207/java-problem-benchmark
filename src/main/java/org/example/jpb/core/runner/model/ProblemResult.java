package org.example.jpb.core.runner.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.shared.constants.ModelLimits;
import org.example.jpb.util.ModelChecks;

@Getter
public class ProblemResult {

	private final String problemId;
	private final String problemDisplayName;
	private final List<SolutionResult> solutionResults;

	@Builder
	private ProblemResult(
		String problemId,
		String problemDisplayName,
		List<SolutionResult> solutionResults
	) {
		this.problemId = ModelChecks.requireNormalizedNonBlankLengthBetween(
			problemId,
			ModelLimits.ID_MIN_LENGTH,
			ModelLimits.ID_MAX_LENGTH,
			"problemId"
		);
		this.problemDisplayName =
			ModelChecks.requireNormalizedNonBlankLengthBetweenOrDefault(
				problemDisplayName,
				this.problemId,
				ModelLimits.DISPLAY_NAME_MIN_LENGTH,
				ModelLimits.DISPLAY_NAME_MAX_LENGTH,
				"problemDisplayName"
			);
		this.solutionResults = ModelChecks.requireNonEmptyCopy(
			solutionResults,
			"solutionResults"
		);
	}
}
