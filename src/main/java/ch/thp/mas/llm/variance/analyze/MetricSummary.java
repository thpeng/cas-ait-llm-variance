package ch.thp.mas.llm.variance.analyze;

public record MetricSummary(
        int count,
        Double min,
        Double median,
        Double p90,
        Double max,
        Double mean
) {
}
