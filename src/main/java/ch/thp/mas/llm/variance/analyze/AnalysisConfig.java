package ch.thp.mas.llm.variance.analyze;

public record AnalysisConfig(
        String embeddingModel,
        String embeddingPrefix,
        int maxEmbeddingTokens,
        String distance,
        DbscanConfig dbscan,
        BleuConfig bleu,
        RougeConfig rouge,
        String percentile
) {

    public static AnalysisConfig defaults() {
        return new AnalysisConfig(
                "local-hashing-v1",
                "passage:",
                514,
                "cosine",
                new DbscanConfig(0.15, 2),
                new BleuConfig(4, "add-one", 0.1),
                new RougeConfig("ROUGE-L", "f1"),
                "nearest-rank"
        );
    }
}
