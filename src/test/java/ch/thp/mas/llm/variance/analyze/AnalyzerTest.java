package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.client.Manufacturer;
import ch.thp.mas.llm.variance.run.RunConfigLog;
import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyzerTest {

    @Test
    void analyzesRunWithFakeEmbeddings() {
        Analyzer analyzer = new Analyzer(
                (texts, config) -> List.of(
                        new EmbeddingResult(new double[]{1, 0}, false),
                        new EmbeddingResult(new double[]{0.99, 0.01}, false),
                        new EmbeddingResult(new double[]{0, 1}, false)
                ),
                new CosineDistance(),
                new MedoidSelector(),
                new DbscanClusterer(),
                new RougeLMetric(new TextTokenizer()),
                new BleuMetric(new TextTokenizer()),
                new SummaryStatistics(),
                new FixedClock()
        );

        AnalysisResult result = analyzer.analyze(new NamedRunLog("run.json", runLog()));

        assertThat(result.sourceRun()).isEqualTo("run.json");
        assertThat(result.semantic().responseCount()).isEqualTo(3);
        assertThat(result.semantic().clusters()).hasSize(1);
        assertThat(result.semantic().clusters().getFirst().repetitionIndices()).containsExactly(1, 2);
        assertThat(result.semantic().outliers()).containsExactly(3);
        assertThat(result.syntactic().clusters().getFirst().pairCount()).isEqualTo(1);
    }

    private static RunLog runLog() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-02T10:00:00+02:00");
        return new RunLog(
                "0001-test",
                now,
                now,
                Manufacturer.OPENAI,
                "gpt-test",
                null,
                3,
                new RunConfigLog(0.0, null, null, null),
                "prompt",
                List.of(
                        new RunLogEntry(1, now, now, "Die Hauptstadt ist Bern.", null),
                        new RunLogEntry(2, now, now, "Bern ist die Hauptstadt.", null),
                        new RunLogEntry(3, now, now, "Eine Rundreise durch die Schweiz.", null)
                )
        );
    }

    private static class FixedClock extends SystemRunClock {

        @Override
        public OffsetDateTime now() {
            return OffsetDateTime.parse("2026-05-02T11:00:00+02:00");
        }
    }
}
