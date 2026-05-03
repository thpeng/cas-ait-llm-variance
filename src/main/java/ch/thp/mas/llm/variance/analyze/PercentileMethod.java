package ch.thp.mas.llm.variance.analyze;

/**
 * Method used to compute percentile aggregates of pairwise distances.
 */
public enum PercentileMethod {
    /**
     * Nearest-rank method (NIST): for the p-th percentile of {@code n} values,
     * pick the value at rank {@code ceil(p · n / 100)} in the sorted sequence.
     * No interpolation between values.
     */
    NEAREST_RANK,

    /**
     * Linear interpolation between adjacent ranks (matches NumPy's default
     * {@code linear} method).
     */
    LINEAR_INTERPOLATION
}