package ch.thp.mas.llm.variance;

import static org.mockito.Mockito.verify;

import ch.thp.mas.llm.variance.analyze.AnalyzeCommand;
import ch.thp.mas.llm.variance.plan.ResolvedPlan;
import ch.thp.mas.llm.variance.run.PlanRunner;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(args = {"--plan=0001-rundreise-schweiz", "--iterations=1"})
class LlmVarianceApplicationTest {

    @MockitoBean
    private PlanRunner planRunner;

    @MockitoBean
    private AnalyzeCommand analyzeCommand;

    @Test
    void wiresCommandLinePlanToRunner() throws Exception {
        verify(planRunner).run(ArgumentMatchers.argThat((ResolvedPlan plan) ->
                plan.name().equals("0001-rundreise-schweiz") && plan.iterations() == 1
        ));
    }
}
