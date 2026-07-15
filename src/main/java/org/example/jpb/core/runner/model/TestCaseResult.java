package org.example.jpb.core.runner.model;

import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.shared.constants.ModelLimits;
import org.example.jpb.util.ModelChecks;

@Getter
public class TestCaseResult {

	private final String testCaseId;
	private final String testCaseDisplayName;
	private final Object expected;
	private final Object actual;
	private final boolean passed;

	@Builder
	private TestCaseResult(
		String testCaseId,
		String testCaseDisplayName,
		Object expected,
		Object actual,
		boolean passed
	) {
		this.testCaseId =
			ModelChecks.requireNormalizedNonBlankLengthBetween(
				testCaseId,
				ModelLimits.ID_MIN_LENGTH,
				ModelLimits.ID_MAX_LENGTH,
				"testCaseId"
			);
		this.testCaseDisplayName =
			ModelChecks.requireNormalizedNonBlankLengthBetweenOrDefault(
				testCaseDisplayName,
				this.testCaseId,
				ModelLimits.DISPLAY_NAME_MIN_LENGTH,
				ModelLimits.DISPLAY_NAME_MAX_LENGTH,
				"testCaseDisplayName"
			);
		this.expected = ModelChecks.requireNonNull(expected, "expected");
		this.actual = ModelChecks.requireNonNull(actual, "actual");
		this.passed = passed;
	}
}
