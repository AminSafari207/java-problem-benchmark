package org.example.jpb.problems;

import java.util.List;
import org.example.jpb.annotation.CaseSet;
import org.example.jpb.annotation.Contract;
import org.example.jpb.annotation.Problem;
import org.example.jpb.annotation.Solution;
import org.example.jpb.core.shared.model.Arguments;
import org.example.jpb.core.shared.model.TestCase;

@Problem(id = "dummy-problem", displayName = "Dummy Problem")
@Contract(accepts = { Integer.class }, expects = Integer.class)
public class DummyProblem {

	@CaseSet(id = "case-set-01")
	public List<TestCase> cases2 = List.of(
		TestCase.of("three", Arguments.single(1), 1)
	);

	@Solution(id = "return-one")
	public Integer solve(Integer input) {
		return 1;
	}
}
