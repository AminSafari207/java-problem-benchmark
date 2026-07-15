package org.example.jpb.core.preparation.model;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.shared.constants.ModelLimits;
import org.example.jpb.core.shared.model.TestCase;
import org.example.jpb.util.ModelChecks;

@Getter
public final class PreparedCaseSet {

	private final String id;
	private final String displayName;
	private final List<TestCase> testCases;

	@Builder
	private PreparedCaseSet(String id, String displayName, List<TestCase> testCases) {
		this.id =
			ModelChecks.requireNormalizedNonBlankLengthBetween(
				id,
				ModelLimits.ID_MIN_LENGTH,
				ModelLimits.ID_MAX_LENGTH,
				"id"
			);
		this.displayName =
			ModelChecks.requireNormalizedNonBlankLengthBetweenOrDefault(
				displayName,
				this.id,
				ModelLimits.DISPLAY_NAME_MIN_LENGTH,
				ModelLimits.DISPLAY_NAME_MAX_LENGTH,
				"displayName"
			);
		this.testCases = ModelChecks.requireNonEmptyCopy(testCases, "testCases");

		ModelChecks.requireUniqueIds(this.testCases, TestCase::getId, "testCases");
	}
}
