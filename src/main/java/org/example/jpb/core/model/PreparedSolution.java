package org.example.jpb.core.model;

import java.lang.reflect.Method;
import lombok.Builder;
import lombok.Getter;
import org.example.jpb.core.constants.ModelLimits;
import org.example.jpb.util.ModelChecks;

@Getter
public class PreparedSolution {

	private final String id;
	private final String displayName;
	private final Method solutionMethod;
	private final SolutionInvoker invoker;

	@Builder
	public PreparedSolution(String id, String displayName, Method solutionMethod, SolutionInvoker invoker) {
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
		this.solutionMethod = ModelChecks.requireNonNull(solutionMethod, "solutionMethod");
		this.invoker = ModelChecks.requireNonNull(invoker, "invoker");
	}
}
