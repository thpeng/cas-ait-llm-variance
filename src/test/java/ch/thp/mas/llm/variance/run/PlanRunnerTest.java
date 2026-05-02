package ch.thp.mas.llm.variance.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.client.LlmClient;
import ch.thp.mas.llm.variance.client.LlmRequestConfig;
import ch.thp.mas.llm.variance.client.LlmResponse;
import ch.thp.mas.llm.variance.client.Manufacturer;
import ch.thp.mas.llm.variance.client.TokenUsage;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.Test;

class PlanRunnerTest {

    @Test
    void callsClientForEveryIterationAndWritesRunLog() throws Exception {
        RecordingClient client = new RecordingClient();
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        PlanRunner runner = new PlanRunner(manufacturer -> client, new FixedRunClock(), writer);
        ResolvedPlan plan = new ResolvedPlan(
                "0001-test",
                Manufacturer.OPENAI,
                "gpt-test",
                "hello",
                3,
                0.1,
                0.9,
                4,
                123L,
                null
        );

        RunLog runLog = runner.run(plan);

        assertThat(writer.logs).containsExactly(runLog);
        assertThat(runLog.planName()).isEqualTo("0001-test");
        assertThat(runLog.modelVersion()).isNull();
        assertThat(runLog.config().seed()).isEqualTo(123L);
        assertThat(runLog.repetitions()).hasSize(3);
        assertThat(runLog.repetitions()).extracting(RunLogEntry::response)
                .containsExactly("answer-1", "answer-2", "answer-3");
        assertThat(runLog.repetitions().getFirst().tokenUsage())
                .isEqualTo(new TokenUsage(10L, 5L, 15L));
        assertThat(client.prompts).containsExactly("hello", "hello", "hello");
        assertThat(client.configs).allSatisfy(config -> {
            assertThat(config.model()).isEqualTo("gpt-test");
            assertThat(config.temperature()).isEqualTo(0.1);
            assertThat(config.topP()).isEqualTo(0.9);
            assertThat(config.topK()).isEqualTo(4);
            assertThat(config.seed()).isEqualTo(123L);
        });
    }

    @Test
    void doesNotWriteLogWhenClientCallFails() {
        RecordingRunLogWriter writer = new RecordingRunLogWriter();
        PlanRunner runner = new PlanRunner(manufacturer -> new FailingClient(), new FixedRunClock(), writer);
        ResolvedPlan plan = new ResolvedPlan(
                "0001-test",
                Manufacturer.OPENAI,
                "gpt-test",
                "hello",
                1,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> runner.run(plan))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
        assertThat(writer.logs).isEmpty();
    }

    private static class RecordingClient implements LlmClient {

        private final List<String> prompts = new ArrayList<>();
        private final List<LlmRequestConfig> configs = new ArrayList<>();

        @Override
        public LlmResponse call(String prompt, LlmRequestConfig config) {
            prompts.add(prompt);
            configs.add(config);
            return new LlmResponse("answer-" + prompts.size(), new TokenUsage(10L, 5L, 15L));
        }
    }

    private static class FailingClient implements LlmClient {

        @Override
        public LlmResponse call(String prompt, LlmRequestConfig config) {
            throw new IllegalStateException("boom");
        }
    }

    private static class FixedRunClock implements RunClock {

        private final Queue<OffsetDateTime> timestamps = new ArrayDeque<>(List.of(
                OffsetDateTime.parse("2026-05-02T10:00:00+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:01+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:02+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:03+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:04+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:05+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:06+02:00"),
                OffsetDateTime.parse("2026-05-02T10:00:07+02:00")
        ));

        @Override
        public OffsetDateTime now() {
            return timestamps.remove();
        }
    }

    private static class RecordingRunLogWriter extends RunLogWriter {

        private final List<RunLog> logs = new ArrayList<>();

        RecordingRunLogWriter() {
            super(new RunFileNameFactory());
        }

        @Override
        public java.nio.file.Path write(RunLog runLog) {
            logs.add(runLog);
            return java.nio.file.Path.of("ignored");
        }
    }
}
