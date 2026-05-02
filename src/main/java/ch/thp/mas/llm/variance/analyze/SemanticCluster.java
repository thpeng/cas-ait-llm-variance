package ch.thp.mas.llm.variance.analyze;

import java.util.List;

public record SemanticCluster(
        int clusterId,
        int size,
        List<Integer> repetitionIndices,
        Integer medoidRepetitionIndex,
        MetricSummary pairwiseCosineDistance
) {
}
