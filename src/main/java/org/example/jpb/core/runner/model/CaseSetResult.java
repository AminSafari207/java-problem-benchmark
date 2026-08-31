package org.example.jpb.core.runner.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.shared.constants.ModelLimits;
import org.example.jpb.util.ModelChecks;

@Getter
public class CaseSetResult {

	private final String caseSetId;
	private final String caseSetDisplayName;
	private final List<TestCaseResult> testCaseResults;

	@Builder
	public CaseSetResult(
		String caseSetId,
		String caseSetDisplayName,
		List<TestCaseResult> testCaseResults
	) {
		this.caseSetId = ModelChecks.requireNormalizedNonBlankLengthBetween(
			caseSetId,
			ModelLimits.ID_MIN_LENGTH,
			ModelLimits.ID_MAX_LENGTH,
			"caseSetId"
		);
		this.caseSetDisplayName =
			ModelChecks.requireNormalizedNonBlankLengthBetweenOrDefault(
				caseSetDisplayName,
				this.caseSetId,
				ModelLimits.DISPLAY_NAME_MIN_LENGTH,
				ModelLimits.DISPLAY_NAME_MAX_LENGTH,
				"caseSetDisplayName"
			);
		this.testCaseResults = ModelChecks.requireNonEmptyCopy(
			testCaseResults,
			"testCaseResults"
		);
	}

	public long passedCount() {
		return testCaseResults.stream().filter(TestCaseResult::isPassed).count();
	}

	public long totalCount() {
		return testCaseResults.size();
	}

	public boolean isPassed() {
		return passedCount() == totalCount();
	}
}
