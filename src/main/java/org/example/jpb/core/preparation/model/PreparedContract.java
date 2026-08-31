package org.example.jpb.core.preparation.model;

import java.util.Arrays;
import lombok.Getter;
import org.example.jpb.util.ModelChecks;

@Getter
public final class PreparedContract {

	private final Class<?>[] acceptedTypes;
	private final Class<?> expectedType;

	private PreparedContract(Class<?>[] acceptedTypes, Class<?> expectedType) {
		this.acceptedTypes = ModelChecks.requireNonNull(
			acceptedTypes,
			"acceptedTypes"
		);
		this.expectedType = ModelChecks.requireNonNull(
			expectedType,
			"expectedType"
		);
	}

	public static PreparedContract of(
		Class<?>[] acceptedTypes,
		Class<?> expectedType
	) {
		return new PreparedContract(acceptedTypes, expectedType);
	}

	@Override
	public String toString() {
		return (
			"ProblemContract{" +
			"acceptedTypes=" +
			Arrays.toString(acceptedTypes) +
			", expectedType=" +
			expectedType +
			'}'
		);
	}
}
