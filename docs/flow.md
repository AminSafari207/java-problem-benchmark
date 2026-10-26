# Architectural flow

This document describes how java-problem-bench works internally.

The README covers the basics. this is for when you want to understand the pipeline or extend it.

---
## Overview

The system has five layers:

1. **Annotations** -- markers on problem classes that declare what a problem is
2. **Model** -- validated classes that hold contracts, prepared artifacts, and results
3. **Preparation** -- reflection-based collector that reads annotations and validates artifacts
4. **Execution** -- correctness runner and benchmark runner
5. **Render** -- output formatters (currently console only)

```
  @Problem class
        |
        v
  ProblemExecutor.execute()
        |
        +-- ProblemPreparator.prepare()
        |     +-- validate & instantiate
        |     +-- read @Contract
        |     +-- collect @CaseSet artifacts
        |     +-- collect @Solution methods
        |     +-- validate signatures & uniqueness
        |     |
        |     v
        |   PreparedProblem
        |
        +-- ProblemRunner.run()
        |     +-- execute & compare
        |     |
        |     v
        |   ProblemResult
        |
        +-- filter passed solutions by id
        |
        +-- BenchmarkRunner.run()  (or skip if none passed)
        |     |
        |     +-- forkCount == 0 -> run in the current JVM
        |     |
        |     +-- forkCount > 0  -> run in separate JVMs
        |           +-- ForkBenchmarkRunner
        |           +-- JavaForkProcessLauncher
        |           +-- ForkBenchmarkWorker
        |           +-- JSON request/result over stdio
        |           +-- merge samples from every fork
        |     |
        |     v
        |   ProblemBenchmarkResult
        |
        v
  ProblemExecutionResult
        |
        v
  ProblemConsoleRenderer.renderProblemResult()
  ProblemConsoleRenderer.renderBenchmarkResult()
```

`ProblemRunner` can also be used on its own if you only want correctness and already have a `PreparedProblem`.

---
## Annotations

| Annotation | Target | Purpose |
|---|---|---|
| `@Problem(id, displayName?)` | class | Marks a class as a runnable problem |
| `@Contract(accepts, expects)` | class | Declares the parameter and return types that case sets and solutions must match |
| `@CaseSet(id, displayName?)` | method or field | Provides one or more `TestCase` instances as a grouped set |
| `@Solution(id, displayName?)` | method | A candidate solution to run against all case sets |
| `@Generator` | method | Planned: generate random cases at runtime (not wired yet) |

### ID and display name rules

Every `@Problem`, `@CaseSet`, `@Solution`, and `TestCase` must have a non-blank `id`.

`displayName` is optional everywhere. when it's blank, the model falls back to `id`.

Lengths are enforced via `ModelLimits`:

- `id`: 1--64 characters
- `displayName`: 1--120 characters

IDs must be unique within their scope:

- case set ids must be unique per problem
- solution ids must be unique per problem
- test case ids must be unique within a case set

Values must be non-blank and must not contain leading or trailing whitespace.

### Contract rules

The problem class must declare exactly one `@Contract` annotation.

- `accepts` — array of parameter types, in order
- `expects` — return type

During preparation, the annotation is read from the class and converted into a `PreparedContract` for validation.

### Case set rules

A `@CaseSet` method or field may produce:

- a single `TestCase`, or
- a `List<TestCase>`

Each test case carries an `id`, optional `displayName`, an `Arguments` object, and an expected return value. the runner validates that argument count and types match the contract, and that the expected value is compatible with the declared return type.

### Solution rules

Each `@Solution` method must match the contract exactly: same parameter count, same parameter types, same return type. Primitive and wrapper types are treated as equivalent (`int` vs `Integer`).

Solutions are sorted by `id` before execution.

---
## Model

Most model classes are immutable builder classes validated through `ModelChecks`. Lombok generates the boilerplate.

### PreparedContract

Internal model built from `@Contract` during preparation:

```java
PreparedContract.of(new Class<?>[] { Integer.class, String.class }, Boolean.class);
```

Stores `Class<?>[] acceptedTypes` and `Class<?> expectedType`. Used only for validation at runtime; it does not perform any invocation itself.

Problem authors declare the contract on the class instead:

```java
@Contract(accepts = { Integer.class, String.class }, expects = Boolean.class)
```

### Arguments

Wraps the input values for a test case:

- `Arguments.none()` -- zero arguments
- `Arguments.single(value)` -- one argument
- `Arguments.of(first, rest...)` -- multiple arguments

Values are cloned on access to prevent mutation. `deepValues()` clones primitive arrays and recursively clones object arrays.

### TestCase

```java
TestCase.of("case-id", Arguments.single(1), 2);
TestCase.of("case-id", "My display name", Arguments.single(1), 2);
```

Fields:

- `id` -- required, unique within its case set
- `displayName` -- optional, falls back to `id`
- `arguments` -- non-null
- `expected` -- non-null

`getDeepClonedArguments()` returns a defensive copy of the argument values.

### Prepared artifacts

`ProblemPreparator` produces a `PreparedProblem` that holds everything needed for execution:

```
PreparedProblem
  id, displayName
  problemClass, problemInstance
  contract
  caseSets: List<PreparedCaseSet>
    id, displayName
    testCases: List<TestCase>
  solutions: List<PreparedSolution>
    id, displayName
    solutionMethod
    invoker
```

`PreparedProblem.withNewSolutions(...)` creates a copy with a different solution list. `ProblemExecutor` uses this to benchmark only the solutions that passed correctness.

### Result models

After a run completes, results are structured in four levels:

```
ProblemResult
  problemId, problemDisplayName
  solutionResults: List<SolutionResult>
    solutionId, solutionDisplayName
    caseSetResults: List<CaseSetResult>
      caseSetId, caseSetDisplayName
      testCaseResults: List<TestCaseResult>
        testCaseId, testCaseDisplayName
        expected, actual, passed
```

`SolutionResult`, `CaseSetResult`, and `TestCaseResult` expose pass/total counts and `isPassed()` for summary rendering.

Benchmark results are separate:

```
ProblemExecutionResult
  problemResult: ProblemResult
  problemBenchmarkResult: ProblemBenchmarkResult
    problemId, problemDisplayName
    solutionBenchmarkResults: List<SolutionBenchmarkResult>
      solutionId, solutionDisplayName
      status: SUCCESS | FAILED
      stats: BenchmarkStats
        sampleCount, totalNanos, minNanos, maxNanos
        medianNanos, p95Nanos, p99Nanos, samplesNanos
      errorMessage (on failure)
    skipReason (when benchmark was skipped)
```

`BenchmarkStats.getAverageNanos()` is computed from `totalNanos / sampleCount`.

For forked runs there is also an internal `ForkBenchmarkResult` per fork. it carries the fork index, status, stats, optional GC deltas, and an error message. those per-fork results are not exposed directly; `BenchmarkRunner` aggregates them into a regular `SolutionBenchmarkResult`.

### BenchmarkConfig

Controls warmup, measurement, and fork execution:

- `warmupIterations` -- default 1,000, must be >= 0
- `measurementIterations` -- default 10,000, must be > 0
- `forkCount` -- default 5, must be between 0 and 10
- `forkTimeoutMillis` -- default 60,000, must be between 1,000 and 600,000

`forkCount(0)` means in-process benchmarking. any positive value forks that many JVM processes per solution.

---
## Execution guarantees and current assumptions

The framework currently assumes problem definitions are deterministic and that
solution methods do not rely on external state.

- Case sets and solutions are executed sequentially.
- A single prepared problem instance is used during an in-process run.
- Solutions should not mutate shared problem state.
- Mutable case arguments are copied only for value types supported by
  `Arguments`.
- Benchmark timing includes the current invocation path and test-case traversal.
  It does not yet isolate algorithm time from reflection or framework overhead.
- Forked benchmarks isolate the JVM, but each fork still uses the same
  reflection-based runner and case traversal.
- Benchmark results are comparative and educational, not a replacement for JMH.

---
## Preparation pipeline

`ProblemPreparator.prepare(Class<?> problemClass)` follows this sequence:

### 1. Validate problem class

- Class must not be null
- Class must have `@Problem`

### 2. Instantiate

Creates an instance via the no-argument constructor (made accessible via reflection). The instance is used when invoking `@CaseSet` methods and `@Solution` methods.

### 3. Read contract

Reads `@Contract` from the problem class and builds a `PreparedContract` from `accepts` and `expects`.

### 4. Collect case sets

Iterates declared methods and fields annotated with `@CaseSet`:

- Methods are invoked on the problem instance
- Fields are read from the problem instance
- Return values are normalized into `PreparedCaseSet` objects with their test cases

### 5. Collect solutions

Finds all methods annotated with `@Solution`, makes them accessible, creates an invoker, and sorts by `id`.

### 6. Validate artifacts

Fails fast if:

- no case sets found
- no solutions found
- duplicate ids within case sets, solutions, or test cases
- any test case has wrong argument count or incompatible types
- any solution has a mismatched signature

### 7. Return

Builds and returns a validated `PreparedProblem`.

---
## Correctness pipeline

`ProblemRunner.run(PreparedProblem)` (or `run(Class<?>)` which prepares first):

For each solution, for each case set, for each test case:

1. Invoke the solution method with deep-cloned case arguments via `ReflectionExecutor`
2. Compare the return value to the expected value via `ResultComparator`
3. Build a `TestCaseResult`, roll up into `CaseSetResult`, then `SolutionResult`

If invocation throws, the runner wraps it in a `RuntimeException` with context about which solution and test case failed.

Assembles all `SolutionResult` objects into a `ProblemResult` and returns it. Rendering is intentionally outside the runner.

---
## Benchmark pipeline

`ProblemExecutor` orchestrates correctness and benchmarking:

1. Prepare the problem through `ProblemPreparator`.
2. Run correctness through `ProblemRunner`.
3. Select solutions that passed all configured correctness cases.
4. If no solution passes, return a skipped `ProblemBenchmarkResult`.
5. Otherwise, run `BenchmarkRunner` on passing solutions only.

`BenchmarkRunner.run(PreparedProblem, BenchmarkConfig)` currently processes one
solution at a time:

1. **Warmup**  
   Invokes the solution across every configured test case
   `warmupIterations` times. This gives the JVM an opportunity to profile and
   compile hot paths, but does not guarantee stable JIT state.

2. **Measurement**  
   Repeats the complete test-case workload `measurementIterations` times.
   Each repetition is one benchmark sample.

3. **Statistics**  
   Records each sample's elapsed nanoseconds and derives sample count, total,
   min, max, average, median, p95, and p99.

4. **Fork aggregation**  
   When `forkCount` is greater than zero, each fork runs its own warmup and
   measurement, then the parent combines the samples from all successful forks
   into one `BenchmarkStats`.

5. **Failure handling**  
   An exception during warmup or measurement marks that solution benchmark as
   `FAILED` and stores an error message. A failed or timed-out fork also fails
   the whole solution benchmark.

A measurement sample currently represents one full pass across all case sets and
test cases for one solution. Timing is wall-clock elapsed time for that complete
workload.

The current benchmark implementation is intentionally basic:

- it uses `System.nanoTime()`;
- it supports in-process and isolated JVM fork execution;
- it includes framework overhead such as reflective invocation and traversal;
- it does not yet use batch timing, adaptive warmup, or environment metadata;
- it is useful for learning and rough comparisons, not as a replacement for
  JMH.

---
## Fork benchmark pipeline

Forked benchmarking exists to keep one JVM's warmup and allocation behavior from
bleeding into the next run.

When `forkCount > 0`, `BenchmarkRunner` delegates each solution to
`ForkBenchmarkRunner`. That runner creates `forkCount` requests and runs them
sequentially.

Each request contains:

- the fork index
- problem id and problem class name
- solution id
- the full `BenchmarkConfig`
- an optional `processBuilderCommand`

`JavaForkProcessLauncher` turns a request into a process:

1. Use the request's `processBuilderCommand` if present. otherwise build a
   default command from the current `java.home`, `java.class.path`, and
   `ForkBenchmarkWorker`.
2. Start the process with stdout and stderr kept separate.
3. Drain stderr up to 64 KiB and stdout without a cap on background threads.
4. Write the JSON request to the process stdin, then close stdin.
5. Wait for the process with `forkTimeoutMillis`. on timeout, destroy it and
   return a failed result with whatever stderr was captured.
6. Check the exit code, then deserialize the result from the buffered stdout.

The stdout drain is not optional. without it the parent waits for the child to
exit while the child is blocked writing a large result into a full OS pipe,
which deadlocks both sides.

`ForkBenchmarkWorker` runs in the child JVM:

1. Read the JSON request from stdin.
2. Load the problem class with `Class.forName`.
3. Prepare the problem and find the solution by id.
4. Run `BenchmarkRunner.runSingleSolutionInProcess` with `forkCount` forced to
   zero.
5. Write a `ForkBenchmarkResult` as JSON to stdout.

Back in the parent, `BenchmarkRunner.aggregateForkResults` combines the raw
samples from every successful fork. if any fork is missing, failed, or returns
no samples, the solution's benchmark is marked failed and the fork error is
preserved.

---
## Utilities

### ModelChecks

Central validation helpers used by model builders:

- null/blank/whitespace checks
- length bounds
- unique id enforcement in collections
- non-empty list copies

### ReflectionExecutor

Centralizes reflective invocation. Unwraps `InvocationTargetException` so the original runtime exception or error propagates cleanly. It also creates the `SolutionInvoker` used by the benchmark path.

### ResultComparator

Compares expected and actual values:

- reference equality first
- null handling
- array deep equality for all primitive and object array types
- `Objects.equals` for everything else

This is correctness comparison only. timing is handled separately by `BenchmarkRunner`.

### Console

ANSI-colored output helpers, box drawing, indentation, truncation, padding, and visual-length calculation (so padding works correctly even with color codes).

---
## Renderer

`ProblemConsoleRenderer` has two entry points:

### Correctness rendering

`renderProblemResult(ProblemResult, ConsoleRenderOptions)` prints:

- a boxed problem header
- per-solution sections with pass/fail lines
- per-case-set and per-test-case detail (configurable)
- expected vs actual details on failure
- summary counts (`N / M passed`) colored green or red

### Benchmark rendering

`renderBenchmarkResult(ProblemBenchmarkResult, ConsoleRenderOptions)` prints:

- a boxed benchmark header
- skipped message if benchmark was not run
- a table with status, solution name, samples, avg/min/p50/p95/p99/max timing
- failed benchmark rows with truncated error messages
- successful rows sorted by average time (fastest first)

It does not care whether the stats came from one in-process run or ten forks.
by the time the renderer sees them, the samples have already been merged.

### ConsoleRenderOptions

| Option | Default | Purpose |
|---|---|---|
| `showIds` | `true` | Show id column in correctness output |
| `showPassedCaseSets` | `false` | Show case sets that fully passed |
| `showPassedTestCases` | `false` | Show individual passed test cases |
| `showBenchmark` | `true` | Render benchmark section at all |
| `maxFailureDetails` | unlimited | Cap how many failed test case details are shown per case set |

The renderer does not influence execution. You can skip it entirely and inspect the result models directly.

---
## Extension points

These are the natural places to add new behavior:

| Extension | Where to hook in |
|---|---|
| Random case generation | New collector in `ProblemPreparator` for `@Generator` methods |
| Problem discovery | New scanner that finds `@Problem` classes on the classpath |
| Richer benchmarking | Per-case timing, JVM warmup control, GC hints, or JMH integration in `BenchmarkRunner` |
| Custom fork command | Set `processBuilderCommand` on `ForkBenchmarkRequest` |
| New output formats | Additional renderers alongside `ProblemConsoleRenderer` |
| Selective benchmarking | Filter which case sets or test cases feed into benchmark measurement |

---
## Error handling

The runner fails early and with specific messages. Validation errors use `IllegalStateException` or `IllegalArgumentException` with the problem class name, field/method name, and what went wrong. Execution errors include the solution name and test case name.

This is intentional. When you are iterating on a problem definition, you want to know exactly which annotation or signature is wrong, not get a generic reflection stack trace.

Benchmark failures are softer: a thrown exception during warmup or measurement marks that solution as `FAILED` in the benchmark result instead of aborting the whole run.

Fork failures are treated the same way. a timeout, non-zero exit, missing result, or broken JSON becomes a failed `ForkBenchmarkResult`, and the parent keeps going so the rest of the problem's solutions can still produce output.

---
## Current limitations

- `@Generator` is defined but not collected or executed.
- Only declared methods and fields on the problem class itself are scanned;
  inherited members are ignored.
- Correctness execution is sequential; bounded parallel execution is a future
  improvement.
- Forks are run sequentially, one process at a time.
- The current benchmark is an in-process or forked comparative timer, not a
  rigorous JVM microbenchmark framework:
    - reflective invocation and framework traversal are included in timing;
    - timing is per full test-case workload, not normalized `ns/op`;
    - a static result sink keeps invocation results alive, but it is not a full
      JMH-style blackhole;
    - no adaptive warmup or environment metadata;
    - mutable input isolation is limited to what `Arguments` can defensively
      copy;
    - forked results serialize every raw sample as JSON, so very large
      measurement counts mean very large request/result payloads.
- No problem discovery exists yet; callers pass problem classes explicitly.
- Console alignment is ANSI-aware for the renderer's own styling, but does not
  fully support wide Unicode terminal characters.

These are the gaps i'd like to close next.
