package ch.thp.mas.llm.variance.analyze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SummaryStatistics {

    public MetricSummary summarize(List<Double> values) {
        if (values.isEmpty()) {
            return new MetricSummary(0, null, null, null, null, null);
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        double sum = 0.0;
        for (double value : sorted) {
            sum += value;
        }
        return new MetricSummary(
                sorted.size(),
                sorted.getFirst(),
                median(sorted),
                nearestRank(sorted, 0.9),
                sorted.getLast(),
                sum / sorted.size()
        );
    }

    private Double median(List<Double> sorted) {
        int size = sorted.size();
        int middle = size / 2;
        if (size % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private Double nearestRank(List<Double> sorted, double percentile) {
        int rank = (int) Math.ceil(percentile * sorted.size());
        return sorted.get(Math.max(1, rank) - 1);
    }
}
