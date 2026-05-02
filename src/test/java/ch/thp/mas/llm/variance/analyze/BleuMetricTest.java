package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BleuMetricTest {

    private final BleuMetric metric = new BleuMetric(new TextTokenizer());

    @Test
    void returnsPositiveScoreForShortTextBecauseOfSmoothing() {
        double score = metric.score("hauptstadt bern", "hauptstadt bern", new BleuConfig(4, "add-one"));

        assertThat(score).isGreaterThan(0.0);
    }
}
