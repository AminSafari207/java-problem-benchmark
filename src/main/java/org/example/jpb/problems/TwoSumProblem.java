package org.example.jpb.problems;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.jpb.annotation.CaseSet;
import org.example.jpb.annotation.Contract;
import org.example.jpb.annotation.Problem;
import org.example.jpb.annotation.Solution;
import org.example.jpb.core.shared.model.Arguments;
import org.example.jpb.core.shared.model.TestCase;

@Problem(id = "two-sum", displayName = "Two Sum")
@Contract(accepts = { int[].class, Integer.class }, expects = int[].class)
public class TwoSumProblem {

    private static final int NO_SOLUTION = -1;

    @CaseSet(id = "small-inputs", displayName = "Small inputs")
    List<TestCase> smallInputs() {
        return List.of(
            TestCase.of("pair-at-start", Arguments.of(new int[] { 2, 7, 11, 15 }, 9), new int[] { 0, 1 }),
            TestCase.of("pair-at-ends", Arguments.of(new int[] { 3, 2, 4 }, 7), new int[] { 0, 2 }),
            TestCase.of("duplicate-values", Arguments.of(new int[] { 3, 3 }, 6), new int[] { 0, 1 })
        );
    }

    @CaseSet(id = "edge-cases", displayName = "Edge cases")
    final List<TestCase> edgeCases = List.of(
        TestCase.of("negative-values", Arguments.of(new int[] { -3, 4, 3, 90 }, 0), new int[] { 0, 2 }),
        TestCase.of("zeros", Arguments.of(new int[] { 0, 4, 3, 0 }, 0), new int[] { 0, 3 }),
        TestCase.of(
            "no-solution",
            Arguments.of(new int[] { 1, 2, 3 }, 100),
            new int[] { NO_SOLUTION, NO_SOLUTION }
        )
    );

    @Solution(id = "brute-force", displayName = "Brute force")
    int[] bruteForce(int[] numbers, Integer target) {
        for (int left = 0; left < numbers.length - 1; left++) {
            for (int right = left + 1; right < numbers.length; right++) {
                if (numbers[left] + numbers[right] == target) {
                    return new int[] { left, right };
                }
            }
        }

        return new int[] { NO_SOLUTION, NO_SOLUTION };
    }

    @Solution(id = "hash-map", displayName = "Hash map")
    int[] hashMap(int[] numbers, Integer target) {
        Map<Integer, Integer> indexByValue = new HashMap<>();

        for (int index = 0; index < numbers.length; index++) {
            int complement = target - numbers[index];
            Integer complementIndex = indexByValue.get(complement);

            if (complementIndex != null) {
                return new int[] { complementIndex, index };
            }

            indexByValue.put(numbers[index], index);
        }

        return new int[] { NO_SOLUTION, NO_SOLUTION };
    }
}
