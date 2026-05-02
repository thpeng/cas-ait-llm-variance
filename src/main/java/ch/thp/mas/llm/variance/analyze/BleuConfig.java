package ch.thp.mas.llm.variance.analyze;

public record BleuConfig(int maxN, String smoothing, double smoothingEpsilon) {
    public BleuConfig {
        if (smoothingEpsilon <= 0 || smoothingEpsilon >= 1) {
            throw new IllegalArgumentException("epsilon must be in (0, 1)");
        }
    }
}
