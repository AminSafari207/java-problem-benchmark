package org.example.jpb.core.shared.model;

import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.shared.constants.ModelLimits;
import org.example.jpb.util.ModelChecks;

public class TestCase {

	@Getter
	private final String id;

	@Getter
	private final String displayName;

	private final Arguments arguments;

	@Getter
	private final Object expected;

	@Builder
	private TestCase(
		String id,
		String displayName,
		Arguments arguments,
		Object expected
	) {
		this.id = ModelChecks.requireNormalizedNonBlankLengthBetween(
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
		this.arguments = ModelChecks.requireNonNull(arguments, "arguments");
		this.expected = ModelChecks.requireNonNull(expected, "expected");
	}

	public static TestCase of(
		String id,
		String displayName,
		Arguments arguments,
		Object expected
	) {
		return new TestCase(id, displayName, arguments, expected);
	}

	public static TestCase of(String id, Arguments arguments, Object expected) {
		return new TestCase(id, null, arguments, expected);
	}

	public Object[] getDeepClonedArguments() {
		return arguments.deepValues();
	}
}
