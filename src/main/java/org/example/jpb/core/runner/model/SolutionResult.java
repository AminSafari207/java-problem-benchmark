package org.example.jpb.core.runner.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.shared.constants.ModelLimits;
import org.example.jpb.util.ModelChecks;

@Getter
public class SolutionResult {

	private final String solutionId;
	private final String solutionDisplayName;
	private final List<CaseSetResult> caseSetResults;

	@Builder
	private SolutionResult(
		String solutionId,
		String solutionDisplayName,
		List<CaseSetResult> caseSetResults
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
		this.caseSetResults = ModelChecks.requireNonEmptyCopy(caseSetResults, "caseSetResults");
	}

	public long passedCount() {
		return caseSetResults.stream().mapToLong(CaseSetResult::passedCount).sum();
	}

	public long totalCount() {
		return caseSetResults.stream().mapToLong(CaseSetResult::totalCount).sum();
	}

	public boolean isPassed() {
		return caseSetResults.stream().allMatch(CaseSetResult::isPassed);
	}
}
