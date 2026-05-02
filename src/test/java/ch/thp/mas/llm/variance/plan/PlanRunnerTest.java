package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;

import ch.thp.mas.llm.variance.client.LlmClient;
import ch.thp.mas.llm.variance.client.LlmRequestConfig;
import ch.thp.mas.llm.variance.client.Manufacturer;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanRunnerTest {

    @Test
    void callsClientForEveryIteration() throws Exception {
        RecordingClient client = new RecordingClient();
        PlanRunner runner = new PlanRunner(manufacturer -> client);
        ResolvedPlan plan = new ResolvedPlan(
                "0001-test",
                Manufacturer.OPENAI,
                "gpt-test",
                "hello",
                3,
                0.1,
                0.9,
                4
        );

        List<String> results = runner.run(plan);

        assertThat(results).containsExactly("answer-1", "answer-2", "answer-3");
        assertThat(client.prompts).containsExactly("hello", "hello", "hello");
        assertThat(client.configs).allSatisfy(config -> {
            assertThat(config.model()).isEqualTo("gpt-test");
            assertThat(config.temperature()).isEqualTo(0.1);
            assertThat(config.topP()).isEqualTo(0.9);
            assertThat(config.topK()).isEqualTo(4);
        });
    }

    private static class RecordingClient implements LlmClient {

        private final List<String> prompts = new ArrayList<>();
        private final List<LlmRequestConfig> configs = new ArrayList<>();

        @Override
        public String call(String prompt, LlmRequestConfig config) {
            prompts.add(prompt);
            configs.add(config);
            return "answer-" + prompts.size();
        }
    }
}
