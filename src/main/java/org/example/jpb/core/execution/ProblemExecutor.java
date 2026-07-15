package org.example.jpb.core.execution;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.example.jpb.core.benchmark.BenchmarkRunner;
import org.example.jpb.core.benchmark.model.BenchmarkConfig;
import org.example.jpb.core.benchmark.model.ProblemBenchmarkResult;
import org.example.jpb.core.execution.model.ProblemExecutionResult;
import org.example.jpb.core.preparation.model.PreparedProblem;
import org.example.jpb.core.preparation.model.PreparedSolution;
import org.example.jpb.core.preparation.ProblemPreparator;
import org.example.jpb.core.runner.ProblemRunner;
import org.example.jpb.core.runner.model.ProblemResult;
import org.example.jpb.core.runner.model.SolutionResult;

public class ProblemExecutor {

	private final ProblemPreparator problemPreparator;
	private final ProblemRunner problemRunner;
	private final BenchmarkRunner benchmarkRunner;

	public ProblemExecutor() {
		this.problemPreparator = new ProblemPreparator();
		this.problemRunner = new ProblemRunner();
		this.benchmarkRunner = new BenchmarkRunner();
	}

	public ProblemExecutionResult execute(Class<?> problemClass, BenchmarkConfig benchmarkConfig) {
		PreparedProblem preparedProblem = problemPreparator.prepare(problemClass);
		ProblemResult problemResult = problemRunner.run(preparedProblem);

		List<PreparedSolution> passedSolutions = findPassedSolutions(preparedProblem, problemResult);

		ProblemBenchmarkResult benchmarkResult = passedSolutions.isEmpty()
			? ProblemBenchmarkResult.skipped(
				preparedProblem.getId(),
				preparedProblem.getDisplayName(),
				"No solutions passed correctness; benchmark skipped."
			)
			: benchmarkRunner.run(preparedProblem.withNewSolutions(passedSolutions), benchmarkConfig);

		return ProblemExecutionResult
			.builder()
			.problemResult(problemResult)
			.problemBenchmarkResult(benchmarkResult)
			.build();
	}

	private List<PreparedSolution> findPassedSolutions(
		PreparedProblem preparedProblem,
		ProblemResult problemResult
	) {
		Set<String> passedSolutionIds = problemResult
			.getSolutionResults()
			.stream()
			.filter(SolutionResult::isPassed)
			.map(SolutionResult::getSolutionId)
			.collect(Collectors.toSet());

		return preparedProblem
			.getSolutions()
			.stream()
			.filter(solution -> passedSolutionIds.contains(solution.getId()))
			.toList();
	}
}
