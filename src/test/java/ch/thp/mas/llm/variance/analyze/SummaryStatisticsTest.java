package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryStatisticsTest {

    private final SummaryStatistics statistics = new SummaryStatistics();

    @Test
    void summarizesEmptyListWithNullNumericValues() {
        MetricSummary summary = statistics.summarize(List.of());

        assertThat(summary.count()).isEqualTo(0);
        assertThat(summary.min()).isNull();
        assertThat(summary.median()).isNull();
        assertThat(summary.p90()).isNull();
        assertThat(summary.max()).isNull();
        assertThat(summary.mean()).isNull();
    }

    @Test
    void summarizesSingleValue() {
        MetricSummary summary = statistics.summarize(List.of(3.0));

        assertThat(summary.count()).isEqualTo(1);
        assertThat(summary.min()).isEqualTo(3.0);
        assertThat(summary.median()).isEqualTo(3.0);
        assertThat(summary.p90()).isEqualTo(3.0);
        assertThat(summary.max()).isEqualTo(3.0);
        assertThat(summary.mean()).isEqualTo(3.0);
    }

    @Test
    void computesMedianForOddAndEvenCounts() {
        assertThat(statistics.summarize(List.of(5.0, 1.0, 3.0)).median()).isEqualTo(3.0);
        assertThat(statistics.summarize(List.of(4.0, 1.0, 2.0, 3.0)).median()).isEqualTo(2.5);
    }

    @Test
    void computesNearestRankP90() {
        assertThat(statistics.summarize(List.of(1.0, 2.0, 3.0, 4.0, 5.0)).p90()).isEqualTo(5.0);
        assertThat(statistics.summarize(List.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)).p90())
                .isEqualTo(9.0);
    }
}
