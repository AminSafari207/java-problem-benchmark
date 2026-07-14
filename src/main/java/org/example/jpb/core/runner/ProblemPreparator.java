package org.example.jpb.core.runner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.example.jpb.annotation.CaseSet;
import org.example.jpb.annotation.Contract;
import org.example.jpb.annotation.Problem;
import org.example.jpb.annotation.Solution;
import org.example.jpb.core.model.*;
import org.example.jpb.util.ModelChecks;
import org.example.jpb.util.ReflectionExecutor;

public class ProblemPreparator {

	public PreparedProblem prepare(Class<?> problemClass) {
		validateProblemClass(problemClass);

		Problem problem = problemClass.getAnnotation(Problem.class);
		Object problemInstance = instantiate(problemClass);
		ProblemContract contract = readContract(problemClass);

		List<PreparedCaseSet> caseSets = resolveCaseSets(problemClass, problemInstance);
		List<PreparedSolution> solutions = resolveSolutions(problemClass, problemInstance);

		validateArtifacts(problemClass, contract, caseSets, solutions);

		return PreparedProblem
			.builder()
			.id(problem.id())
			.displayName(problem.displayName())
			.problemClass(problemClass)
			.problemInstance(problemInstance)
			.contract(contract)
			.caseSets(caseSets)
			.solutions(solutions)
			.build();
	}

	private Object instantiate(Class<?> problemClass) {
		try {
			var constructor = problemClass.getDeclaredConstructor();
			constructor.setAccessible(true);

			return constructor.newInstance();
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(
				"Invalid problem class: " +
				problemClass.getName() +
				" must declare an accessible no-argument constructor",
				e
			);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to instantiate problem class: " + problemClass.getName(), e);
		}
	}

	private ProblemContract readContract(Class<?> problemClass) {
		Contract contract = problemClass.getAnnotation(Contract.class);

		if (contract == null) {
			throw new IllegalStateException(
				"Problem class '" + problemClass.getName() + "' must declare a @Contract annotation"
			);
		}

		return ProblemContract.of(contract.accepts(), contract.expects());
	}

	private List<PreparedCaseSet> resolveCaseSets(Class<?> problemClass, Object problemInstance) {
		List<PreparedCaseSet> caseSets = collectCaseSets(problemClass, problemInstance);

		ModelChecks.requireUniqueIds(caseSets, PreparedCaseSet::getId, "caseSets");

		return caseSets;
	}

	private List<PreparedCaseSet> collectCaseSets(Class<?> problemClass, Object problemInstance) {
		List<PreparedCaseSet> caseSets = new ArrayList<>();

		for (Method method : problemClass.getDeclaredMethods()) {
			CaseSet caseSet = method.getAnnotation(CaseSet.class);

			if (caseSet == null) continue;

			Object value = ReflectionExecutor.invoke(problemInstance, method);
			String source = "@CaseSete method " + method.getName();

			caseSets.add(
				PreparedCaseSet
					.builder()
					.id(caseSet.id())
					.displayName(caseSet.displayName())
					.testCases(extractTestCases(value, source))
					.build()
			);
		}

		for (Field field : problemClass.getDeclaredFields()) {
			CaseSet caseSet = field.getAnnotation(CaseSet.class);

			if (caseSet == null) continue;

			try {
				field.setAccessible(true);

				Object value = field.get(problemInstance);
				String source = "@CaseSete field " + field.getName();

				caseSets.add(
					PreparedCaseSet
						.builder()
						.id(caseSet.id())
						.displayName(caseSet.displayName())
						.testCases(extractTestCases(value, source))
						.build()
				);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(
					"Failed to access @CaseSet field '" + field.getName() + "' in " + problemClass.getName(),
					e
				);
			}
		}

		return caseSets;
	}

	private List<TestCase> extractTestCases(Object value, String source) {
		if (value == null) {
			throw new IllegalStateException(source + " produced null");
		}

		if (value instanceof TestCase testCase) {
			return List.of(testCase);
		}

		if (value instanceof List<?> list) {
			List<TestCase> testCases = new ArrayList<>();

			for (int i = 0; i < list.size(); i++) {
				Object element = list.get(i);

				if (!(element instanceof TestCase testCase)) {
					throw new IllegalStateException(
						source +
						" contains non-TestCase element at index " +
						i +
						": " +
						describeValueType(element)
					);
				}

				testCases.add(testCase);
			}

			return testCases;
		}

		throw new IllegalStateException(
			source + " must produce TestCase or List<TestCase>, but got " + value.getClass().getName()
		);
	}

	private List<PreparedSolution> resolveSolutions(Class<?> problemClass, Object problemInstance) {
		List<PreparedSolution> solutions = collectSolutions(problemClass, problemInstance);

		ModelChecks.requireUniqueIds(solutions, PreparedSolution::getId, "solutionResults");

		return solutions;
	}

	private List<PreparedSolution> collectSolutions(Class<?> problemClass, Object problemInstance) {
		List<PreparedSolution> solutions = new ArrayList<>();

		for (Method method : problemClass.getDeclaredMethods()) {
			Solution solution = method.getAnnotation(Solution.class);

			if (solution == null) continue;

			method.setAccessible(true);
			solutions.add(
				PreparedSolution
					.builder()
					.id(solution.id())
					.displayName(solution.displayName())
					.solutionMethod(method)
					.invoker(ReflectionExecutor.createInvoker(problemInstance, method))
					.build()
			);
		}

		solutions.sort(Comparator.comparing(PreparedSolution::getId));

		return solutions;
	}

	//#################################
	//## Validators ###################
	//#################################

	private void validateProblemClass(Class<?> problemClass) {
		if (problemClass == null) {
			throw new IllegalArgumentException(
				"Invalid problem class: " + problemClass.getName() + " must not be null"
			);
		}

		if (!problemClass.isAnnotationPresent(Problem.class)) {
			throw new IllegalArgumentException(
				"Invalid problem class: " + problemClass.getName() + " is not annotated with @Problem"
			);
		}
	}

	private void validateArtifacts(
		Class<?> problemClass,
		ProblemContract contract,
		List<PreparedCaseSet> caseSets,
		List<PreparedSolution> solutions
	) {
		if (caseSets.isEmpty()) {
			throw new IllegalStateException(
				"Invalid problem definition: no @Case found in " + problemClass.getName()
			);
		}

		if (solutions.isEmpty()) {
			throw new IllegalStateException(
				"Invalid problem definition: no @Solution found in " + problemClass.getName()
			);
		}

		validateCaseSets(problemClass, contract, caseSets);
		validateSolutions(problemClass, contract, solutions);
	}

	private void validateCaseSets(
		Class<?> problemClass,
		ProblemContract contract,
		List<PreparedCaseSet> caseSets
	) {
		for (PreparedCaseSet caseSet : caseSets) {
			validateTestCases(problemClass, contract, caseSet.getTestCases(), caseSet.getDisplayName());
		}
	}

	private void validateTestCases(
		Class<?> problemClass,
		ProblemContract contract,
		List<TestCase> testCases,
		String caseSetDisplayName
	) {
		Class<?>[] expectedParams = contract.getAcceptedTypes();
		Class<?> expectedReturnType = contract.getExpectedType();

		for (TestCase testCase : testCases) {
			Object[] args = testCase.getDeepClonedArguments();

			if (args.length != expectedParams.length) {
				throw new IllegalStateException(
					"Test case '" +
					testCase.getDisplayName() +
					"' of case set '" +
					caseSetDisplayName +
					"' in " +
					problemClass.getName() +
					" has " +
					args.length +
					" argument(s), but contract requires " +
					expectedParams.length
				);
			}

			for (int i = 0; i < args.length; i++) {
				if (!isValueCompatible(expectedParams[i], args[i])) {
					throw new IllegalStateException(
						"Test case '" +
						testCase.getDisplayName() +
						"' of case set '" +
						caseSetDisplayName +
						"' in " +
						problemClass.getName() +
						"' has incompatible argument at index " +
						i +
						": expected " +
						expectedParams[i].getName() +
						", but got " +
						describeValueType(args[i])
					);
				}
			}

			if (!isValueCompatible(expectedReturnType, testCase.getExpected())) {
				throw new IllegalStateException(
					"Test case '" +
					testCase.getDisplayName() +
					"' of case set '" +
					caseSetDisplayName +
					"' in " +
					problemClass.getName() +
					"' has incompatible expected value: expected " +
					expectedReturnType.getName() +
					", but got " +
					describeValueType(testCase.getExpected())
				);
			}
		}
	}

	private void validateSolutions(
		Class<?> problemClass,
		ProblemContract contract,
		List<PreparedSolution> solutions
	) {
		for (PreparedSolution solution : solutions) {
			validateSolutionSignature(problemClass, contract, solution.getSolutionMethod());
		}
	}

	private void validateSolutionSignature(
		Class<?> problemClass,
		ProblemContract contract,
		Method solutionMethod
	) {
		Class<?>[] actualParams = solutionMethod.getParameterTypes();
		Class<?>[] expectedParams = contract.getAcceptedTypes();

		if (actualParams.length != expectedParams.length) {
			throw new IllegalStateException(
				"@Solution method '" +
				solutionMethod.getName() +
				"' in " +
				problemClass.getName() +
				" has " +
				actualParams.length +
				" parameter(s), but contract requires " +
				expectedParams.length
			);
		}

		for (int i = 0; i < expectedParams.length; i++) {
			if (!sameType(actualParams[i], expectedParams[i])) {
				throw new IllegalStateException(
					"@Solution method '" +
					solutionMethod.getName() +
					"' in " +
					problemClass.getName() +
					"' has incompatible parameter type at index " +
					i +
					": expected " +
					expectedParams[i].getName() +
					", got " +
					actualParams[i].getName()
				);
			}
		}

		if (!sameType(solutionMethod.getReturnType(), contract.getExpectedType())) {
			throw new IllegalStateException(
				"@Solution method '" +
				solutionMethod.getName() +
				"' in " +
				problemClass.getName() +
				"' has incompatible return type: expected " +
				contract.getExpectedType().getName() +
				", got " +
				solutionMethod.getReturnType().getName()
			);
		}
	}

	//#################################
	//## Helpers ######################
	//#################################

	private boolean isValueCompatible(Class<?> expectedType, Object value) {
		if (value == null) {
			return !expectedType.isPrimitive();
		}

		Class<?> actualType = value.getClass();
		return toWrapper(expectedType).isAssignableFrom(toWrapper(actualType));
	}

	private String describeValueType(Object value) {
		return value == null ? "null" : value.getClass().getName();
	}

	private boolean sameType(Class<?> actual, Class<?> expected) {
		if (actual.equals(expected)) {
			return true;
		}

		return toWrapper(actual).equals(toWrapper(expected));
	}

	private Class<?> toWrapper(Class<?> type) {
		if (!type.isPrimitive()) {
			return type;
		}

		if (type == int.class) return Integer.class;
		if (type == long.class) return Long.class;
		if (type == boolean.class) return Boolean.class;
		if (type == double.class) return Double.class;
		if (type == float.class) return Float.class;
		if (type == char.class) return Character.class;
		if (type == byte.class) return Byte.class;
		if (type == short.class) return Short.class;
		if (type == void.class) return Void.class;

		throw new IllegalArgumentException("Unknown primitive type: " + type.getName());
	}
}
