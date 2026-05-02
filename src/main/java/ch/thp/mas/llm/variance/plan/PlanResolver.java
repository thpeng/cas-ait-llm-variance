package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.Manufacturer;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class PlanResolver {

    public ResolvedPlan resolve(LoadedPlan loadedPlan, ApplicationArguments appArgs) {
        Plan plan = loadedPlan.plan();
        Manufacturer manufacturer = optionValue(appArgs, "manufacturer") != null
                ? parseManufacturer(optionValue(appArgs, "manufacturer"))
                : plan.getManufacturer();

        String model = optionValue(appArgs, "model") != null
                ? optionValue(appArgs, "model")
                : plan.getModel();
        if (model == null || model.isBlank()) {
            model = manufacturer.defaultModel();
        }

        String prompt = optionValue(appArgs, "prompt") != null
                ? optionValue(appArgs, "prompt")
                : plan.getPrompt();
        if (prompt == null || prompt.isBlank()) {
            throw new PlanException("Missing prompt in plan: " + loadedPlan.filename());
        }

        int iterations = optionValue(appArgs, "iterations") != null
                ? parseInteger(optionValue(appArgs, "iterations"), "iterations")
                : plan.getIterations();
        if (iterations < 1) {
            throw new PlanException("iterations must be at least 1 in plan: " + loadedPlan.filename());
        }

        Double temperature = optionValue(appArgs, "temperature") != null
                ? parseDouble(optionValue(appArgs, "temperature"), "temperature")
                : plan.getTemperature();
        Double topP = optionValue(appArgs, "topP") != null
                ? parseDouble(optionValue(appArgs, "topP"), "topP")
                : plan.getTopP();
        Integer topK = optionValue(appArgs, "topK") != null
                ? parseInteger(optionValue(appArgs, "topK"), "topK")
                : plan.getTopK();

        return new ResolvedPlan(
                loadedPlan.name(),
                manufacturer,
                model,
                prompt,
                iterations,
                temperature,
                topP,
                topK
        );
    }

    private static Manufacturer parseManufacturer(String value) {
        try {
            return Manufacturer.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PlanException("Unknown manufacturer: " + value, e);
        }
    }

    private static Double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new PlanException("Invalid numeric value for " + name + ": " + value, e);
        }
    }

    private static Integer parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new PlanException("Invalid integer value for " + name + ": " + value, e);
        }
    }

    private static String optionValue(ApplicationArguments appArgs, String name) {
        if (!appArgs.containsOption(name)) {
            return null;
        }
        List<String> values = appArgs.getOptionValues(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }
}
