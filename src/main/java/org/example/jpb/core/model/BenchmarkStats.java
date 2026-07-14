package org.example.jpb.core.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.example.jpb.util.ModelChecks;

@Getter
public class BenchmarkStats {

	private final List<Long> samplesNanos;
	private final int sampleCount;
	private final long totalNanos;
	private final long minNanos;
	private final long maxNanos;

	private BenchmarkStats(List<Long> samplesNanos) {
		this.samplesNanos = ModelChecks.requireCopy(samplesNanos, "sampleNanos");

		for (Long sampleNano : this.samplesNanos) {
			ModelChecks.requireNonNegative(sampleNano, "samplesNanos element");
		}

		for (int i = 0; i < this.samplesNanos.size(); i++) {
			ModelChecks.requireNonNegative(samplesNanos.get(i), "samplesNanos element of index '" + i + "'");
		}

		this.sampleCount = this.samplesNanos.size();
		this.totalNanos = this.samplesNanos.stream().mapToLong(Long::longValue).sum();
		this.minNanos = this.samplesNanos.stream().mapToLong(Long::longValue).min().orElse(0L);
		this.maxNanos = this.samplesNanos.stream().mapToLong(Long::longValue).max().orElse(0L);
	}

	public static BenchmarkStats of(List<Long> samplesNanos) {
		return new BenchmarkStats(samplesNanos);
	}

	public double getAverageNanos() {
		return sampleCount == 0 ? 0.0d : (double) totalNanos / sampleCount;
	}
}
