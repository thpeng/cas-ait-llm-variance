package ch.thp.mas.llm.variance.analyze;

import java.util.Objects;

/**
 * Top-level configuration for the variance analysis pipeline.
 *
 * <p>Bundles the embedding model parameters, the distance metric, and the
 * configurations of all downstream analysis components (DBSCAN clustering,
 * BLEU and ROUGE surface metrics, and percentile aggregation).
 *
 * <p>Field semantics:
 * <ul>
 *   <li>{@code embeddingModel}, {@code embeddingPrefix},
 *       {@code maxEmbeddingTokens}: parameters for embedding generation. The
 *       {@code embeddingPrefix} convention follows the E5 family
 *       (Wang et al., 2022), which expects {@code "passage: "} or
 *       {@code "query: "} prefixes; for embedding models without this
 *       convention the prefix may be set to the empty string.
 *       {@code maxEmbeddingTokens} caps input length to the model's context
 *       window (e.g. 512 + 2 special tokens for BERT-family models).</li>
 *   <li>{@code distance}: distance metric used for the semantic analysis
 *       pipeline (cosine distance over embeddings).</li>
 *   <li>{@code dbscan}, {@code bleu}, {@code rouge}: see the corresponding
 *       config records.</li>
 *   <li>{@code percentile}: method used to compute percentile aggregates of
 *       pairwise distances (median, p90, etc.) within clusters.</li>
 * </ul>
 *
 * <p>The {@link #defaults()} factory provides a baseline configuration. See
 * its Javadoc for the open question regarding the default embedding model.
 */
public record AnalysisConfig(
        String embeddingModel,
        String embeddingPrefix,
        int maxEmbeddingTokens,
        DistanceMetric distance,
        DbscanConfig dbscan,
        BleuConfig bleu,
        RougeConfig rouge,
        PercentileMethod percentile
) {

    public AnalysisConfig {
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        Objects.requireNonNull(embeddingPrefix, "embeddingPrefix must not be null");
        if (maxEmbeddingTokens < 1) {
            throw new IllegalArgumentException("maxEmbeddingTokens must be at least 1");
        }
        Objects.requireNonNull(distance, "distance must not be null");
        Objects.requireNonNull(dbscan, "dbscan must not be null");
        Objects.requireNonNull(bleu, "bleu must not be null");
        Objects.requireNonNull(rouge, "rouge must not be null");
        Objects.requireNonNull(percentile, "percentile must not be null");
    }

    /**
     * Baseline configuration for the analysis pipeline.
     *
     * <p>TODO: replace {@code "local-hashing-v1"} with a real multilingual
     * embedding model (e.g. an E5 multilingual variant) before running thesis
     * experiments. The hashing model is a deterministic fallback for local
     * development and CI; cosine distance over hashing embeddings reflects
     * lexical/token overlap mediated by the hash function, not semantic
     * similarity. Using it for the "semantic analysis" pipeline would
     * undermine the methodological framing in Chapter 3. Once the
     * multilingual model is installed and the full chain is tested, update
     * {@code embeddingModel} and verify that {@code maxEmbeddingTokens} and
     * {@code embeddingPrefix} match the chosen model's contract.
     *
     * @return a baseline {@code AnalysisConfig} suitable for local development
     */
    public static AnalysisConfig defaults() {
        return new AnalysisConfig(
                "local-hashing-v1",
                "passage:",
                514,
                DistanceMetric.COSINE,
                new DbscanConfig(0.15, 2),
                new BleuConfig(4, 0.1),
                new RougeConfig(RougeConfig.Variant.ROUGE_L, RougeConfig.Aggregation.F1),
                PercentileMethod.NEAREST_RANK
        );
    }
}