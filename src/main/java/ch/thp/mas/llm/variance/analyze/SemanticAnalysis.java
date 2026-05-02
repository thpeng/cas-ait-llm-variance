package ch.thp.mas.llm.variance.analyze;

import java.util.List;

public record SemanticAnalysis(
        int responseCount,
        int truncatedResponses,
        MedoidAnalysis medoid,
        MetricSummary pairwiseCosineDistance,
        List<SemanticCluster> clusters,
        List<Integer> outliers
) {
}
