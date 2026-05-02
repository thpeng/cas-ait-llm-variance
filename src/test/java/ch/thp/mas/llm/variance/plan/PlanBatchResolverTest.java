package ch.thp.mas.llm.variance.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class PlanBatchResolverTest {

    private final PlanBatchResolver resolver = new PlanBatchResolver(new PlanLoader(), new PlanResolver());

    @Test
    void resolvesSinglePlan() {
        List<ResolvedPlan> plans = resolver.resolve(args("--plan=0001-rundreise-schweiz"));

        assertThat(plans).extracting(ResolvedPlan::name).containsExactly("0001-rundreise-schweiz");
    }

    @Test
    void resolvesMultiplePlansInNaturalOrder() {
        List<ResolvedPlan> plans = resolver.resolve(args("--plans=0002-hauptstadt-798,0001-rundreise-schweiz"));

        assertThat(plans).extracting(ResolvedPlan::name)
                .containsExactly("0001-rundreise-schweiz", "0002-hauptstadt-798");
    }

    @Test
    void resolvesAllPlans() {
        List<ResolvedPlan> plans = resolver.resolve(args("--plans=ALL"));

        assertThat(plans).extracting(ResolvedPlan::name)
                .contains("0001-rundreise-schweiz", "0002-hauptstadt-798");
    }

    @Test
    void rejectsPlanAndPlansTogether() {
        assertThatThrownBy(() -> resolver.resolve(args("--plan=0001-rundreise-schweiz", "--plans=ALL")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("either --plan or --plans");
    }

    @Test
    void rejectsMissingPlanSelection() {
        assertThatThrownBy(() -> resolver.resolve(args("--iterations=1")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("Missing plan selection");
    }

    @Test
    void rejectsEmptyPlanList() {
        assertThatThrownBy(() -> resolver.resolve(args("--plans=,")))
                .isInstanceOf(PlanException.class)
                .hasMessageContaining("at least one plan");
    }

    private static DefaultApplicationArguments args(String... args) {
        return new DefaultApplicationArguments(args);
    }
}
