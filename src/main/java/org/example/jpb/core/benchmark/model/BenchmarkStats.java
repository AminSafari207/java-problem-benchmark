package org.example.jpb.core.benchmark.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import org.example.jpb.util.ModelChecks;

@Getter
public class BenchmarkStats {

	private final List<Long> samplesNanos;
	private final int sampleCount;
	private final long totalNanos;
	private final long minNanos;
	private final long maxNanos;
	private final long medianNanos;
	private final long p95Nanos;
	private final long p99Nanos;

	private BenchmarkStats(List<Long> samplesNanos) {
		this.samplesNanos = ModelChecks.requireNonEmptyCopy(samplesNanos, "samplesNanos");

		long calculatedTotalNanos = 0L;

		for (int i = 0; i < this.samplesNanos.size(); i++) {
			Long sampleNanos = this.samplesNanos.get(i);

			ModelChecks.requireNonNull(sampleNanos, "samplesNanos element at index '" + i + "'");
			ModelChecks.requireNonNegative(sampleNanos, "samplesNanos element at index '" + i + "'");

			calculatedTotalNanos = Math.addExact(calculatedTotalNanos, sampleNanos);
		}

		List<Long> sortedSamplesNanos = new ArrayList<>(this.samplesNanos);

		Collections.sort(sortedSamplesNanos);

		this.sampleCount = this.samplesNanos.size();
		this.totalNanos = calculatedTotalNanos;
		this.minNanos = sortedSamplesNanos.getFirst();
		this.maxNanos = sortedSamplesNanos.getLast();
		this.medianNanos = percentile(sortedSamplesNanos, 0.50d);
		this.p95Nanos = percentile(sortedSamplesNanos, 0.95d);
		this.p99Nanos = percentile(sortedSamplesNanos, 0.99d);
	}

	public static BenchmarkStats of(List<Long> samplesNanos) {
		return new BenchmarkStats(samplesNanos);
	}

	public double getAverageNanos() {
		return sampleCount == 0 ? 0.0d : (double) totalNanos / sampleCount;
	}

	private long percentile(List<Long> sortedSamplesNanos, double percentile) {
		int rank = (int) Math.ceil(percentile * sortedSamplesNanos.size());
		int index = Math.max(0, rank - 1);

		return sortedSamplesNanos.get(index);
	}
}
