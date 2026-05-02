package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.LlmClient;
import ch.thp.mas.llm.variance.client.LlmClientFactory;
import ch.thp.mas.llm.variance.client.LlmRequestConfig;
import ch.thp.mas.llm.variance.client.LlmResponse;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlanRunner {

    private final LlmClientFactory clientFactory;
    private final RunClock runClock;
    private final RunLogWriter runLogWriter;

    public PlanRunner(LlmClientFactory clientFactory, RunClock runClock, RunLogWriter runLogWriter) {
        this.clientFactory = clientFactory;
        this.runClock = runClock;
        this.runLogWriter = runLogWriter;
    }

    public RunLog run(ResolvedPlan plan) throws Exception {
        System.out.println("=== Running plan: " + plan.name() + " ===");

        OffsetDateTime runStartedAt = runClock.now();
        LlmClient client = clientFactory.create(plan.manufacturer());
        LlmRequestConfig config = new LlmRequestConfig(
                plan.model(),
                plan.temperature(),
                plan.topP(),
                plan.topK(),
                plan.seed()
        );

        List<RunLogEntry> repetitions = new ArrayList<>();
        for (int i = 0; i < plan.iterations(); i++) {
            OffsetDateTime startedAt = runClock.now();
            LlmResponse response = client.call(plan.prompt(), config);
            OffsetDateTime endedAt = runClock.now();
            repetitions.add(new RunLogEntry(i + 1, startedAt, endedAt, response.text(), response.tokenUsage()));
            System.out.println(response.text() + ", ");
        }

        RunLog runLog = new RunLog(
                plan.name(),
                runStartedAt,
                runClock.now(),
                plan.manufacturer(),
                plan.model(),
                plan.modelVersion(),
                plan.iterations(),
                new RunConfigLog(plan.temperature(), plan.topP(), plan.topK(), plan.seed()),
                plan.prompt(),
                List.copyOf(repetitions)
        );
        runLogWriter.write(runLog);
        return runLog;
    }
}
