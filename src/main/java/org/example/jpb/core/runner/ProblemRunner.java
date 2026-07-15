package org.example.jpb.core.runner;

import java.util.ArrayList;
import java.util.List;

import org.example.jpb.core.preparation.model.PreparedCaseSet;
import org.example.jpb.core.preparation.model.PreparedProblem;
import org.example.jpb.core.preparation.model.PreparedSolution;
import org.example.jpb.core.preparation.ProblemPreparator;
import org.example.jpb.core.runner.model.CaseSetResult;
import org.example.jpb.core.runner.model.ProblemResult;
import org.example.jpb.core.runner.model.SolutionResult;
import org.example.jpb.core.runner.model.TestCaseResult;
import org.example.jpb.core.shared.model.TestCase;
import org.example.jpb.util.ReflectionExecutor;
import org.example.jpb.util.ResultComparator;

public class ProblemRunner {

	private final ProblemPreparator preparator;

	public ProblemRunner() {
		this(new ProblemPreparator());
	}

	public ProblemRunner(ProblemPreparator preparator) {
		this.preparator = preparator;
	}

	public ProblemResult run(Class<?> problemClass) {
		PreparedProblem preparedProblem = preparator.prepare(problemClass);
		return run(preparedProblem);
	}

	public ProblemResult run(PreparedProblem preparedProblem) {
		List<SolutionResult> solutionResults = new ArrayList<>();

		for (PreparedSolution preparedSolution : preparedProblem.getSolutions()) {
			SolutionResult result = runSolution(
				preparedProblem.getProblemInstance(),
				preparedSolution,
				preparedProblem.getCaseSets()
			);

			solutionResults.add(result);
		}

		return ProblemResult
			.builder()
			.problemId(preparedProblem.getId())
			.problemDisplayName(preparedProblem.getDisplayName())
			.solutionResults(solutionResults)
			.build();
	}

	private SolutionResult runSolution(
		Object instance,
		PreparedSolution preparedSolution,
		List<PreparedCaseSet> caseSets
	) {
		List<CaseSetResult> caseSetResults = new ArrayList<>();

		for (PreparedCaseSet caseSet : caseSets) {
			List<TestCaseResult> testCaseResults = new ArrayList<>();

			for (TestCase testCase : caseSet.getTestCases()) {
				Object actual;

				try {
					actual =
						ReflectionExecutor.invoke(
							instance,
							preparedSolution.getSolutionMethod(),
							testCase.getDeepClonedArguments()
						);
				} catch (RuntimeException e) {
					throw new RuntimeException(
						"Execution failed: @Solution method '" +
						preparedSolution.getDisplayName() +
						"' failed for test case '" +
						testCase.getDisplayName() +
						"'",
						e
					);
				}

				boolean passed = ResultComparator.areEqual(testCase.getExpected(), actual);

				testCaseResults.add(
					TestCaseResult
						.builder()
						.testCaseId(testCase.getId())
						.testCaseDisplayName(testCase.getDisplayName())
						.expected(testCase.getExpected())
						.actual(actual)
						.passed(passed)
						.build()
				);
			}

			caseSetResults.add(
				CaseSetResult
					.builder()
					.caseSetId(caseSet.getId())
					.caseSetDisplayName(caseSet.getDisplayName())
					.testCaseResults(testCaseResults)
					.build()
			);
		}

		return SolutionResult
			.builder()
			.solutionId(preparedSolution.getId())
			.solutionDisplayName(preparedSolution.getDisplayName())
			.caseSetResults(caseSetResults)
			.build();
	}
}
