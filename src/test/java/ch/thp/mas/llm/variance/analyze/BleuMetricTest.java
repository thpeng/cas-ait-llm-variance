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

    @Test
    void returnsZeroForEmptyCandidate() {
        assertThat(metric.score("", "hauptstadt bern", new BleuConfig(4, "add-one"))).isEqualTo(0.0);
    }

    @Test
    void noOverlapStillReturnsSmoothedPositiveScore() {
        double score = metric.score("foo bar", "hauptstadt bern", new BleuConfig(4, "add-one"));

        assertThat(score).isGreaterThan(0.0);
    }

    @Test
    void brevityPenaltyReducesShortCandidateScore() {
        BleuConfig config = new BleuConfig(4, "add-one");

        double shortCandidate = metric.score("bern", "bern ist hauptstadt", config);
        double equalLengthCandidate = metric.score("bern ist hauptstadt", "bern ist hauptstadt", config);

        assertThat(shortCandidate).isLessThan(equalLengthCandidate);
    }
}
