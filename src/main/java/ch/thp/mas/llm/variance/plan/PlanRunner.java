package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.LlmClient;
import ch.thp.mas.llm.variance.client.LlmClientFactory;
import ch.thp.mas.llm.variance.client.LlmRequestConfig;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlanRunner {

    private final LlmClientFactory clientFactory;

    public PlanRunner(LlmClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<String> run(ResolvedPlan plan) throws Exception {
        System.out.println("=== Running plan: " + plan.name() + " ===");

        LlmClient client = clientFactory.create(plan.manufacturer());
        LlmRequestConfig config = new LlmRequestConfig(
                plan.model(),
                plan.temperature(),
                plan.topP(),
                plan.topK()
        );

        List<String> results = new ArrayList<>();
        for (int i = 0; i < plan.iterations(); i++) {
            String answer = client.call(plan.prompt(), config);
            results.add(answer);
            System.out.println(answer + ", ");
        }
        return results;
    }
}
