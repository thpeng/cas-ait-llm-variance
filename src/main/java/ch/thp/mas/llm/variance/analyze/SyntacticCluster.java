package ch.thp.mas.llm.variance.analyze;

public record SyntacticCluster(
        int clusterId,
        int pairCount,
        MetricSummary rougeLDistance,
        MetricSummary bleuDistance
) {
}
