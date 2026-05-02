package ch.thp.mas.llm.variance.plan;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class PlanBatchResolver {

    private static final String ALL = "ALL";

    private final PlanLoader planLoader;
    private final PlanResolver planResolver;

    public PlanBatchResolver(PlanLoader planLoader, PlanResolver planResolver) {
        this.planLoader = planLoader;
        this.planResolver = planResolver;
    }

    public List<ResolvedPlan> resolve(ApplicationArguments appArgs) {
        boolean hasPlan = appArgs.containsOption("plan");
        boolean hasPlans = appArgs.containsOption("plans");
        if (hasPlan && hasPlans) {
            throw new PlanException("Use either --plan or --plans, not both.");
        }
        if (!hasPlan && !hasPlans) {
            throw new PlanException("Missing plan selection. Use --plan=<name> or --plans=<name1,name2|ALL>.");
        }

        List<LoadedPlan> loadedPlans = hasPlan
                ? List.of(planLoader.load(optionValue(appArgs, "plan")))
                : loadPlans(optionValue(appArgs, "plans"));

        return loadedPlans.stream()
                .map(plan -> planResolver.resolve(plan, appArgs))
                .toList();
    }

    private List<LoadedPlan> loadPlans(String value) {
        if (value == null || value.isBlank()) {
            throw new PlanException("--plans must not be blank.");
        }
        if (ALL.equalsIgnoreCase(value.trim())) {
            return planLoader.loadAll();
        }

        List<String> names = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .sorted(PlanLoader.naturalPlanNameComparator())
                .toList();
        if (names.isEmpty()) {
            throw new PlanException("--plans must contain at least one plan name or ALL.");
        }

        return names.stream()
                .map(planLoader::load)
                .toList();
    }

    private static String optionValue(ApplicationArguments appArgs, String name) {
        List<String> values = appArgs.getOptionValues(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }
}
