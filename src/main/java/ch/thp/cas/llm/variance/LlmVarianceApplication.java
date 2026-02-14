package ch.thp.cas.llm.variance;

import ch.thp.cas.llm.variance.client.LlmClient;
import ch.thp.cas.llm.variance.client.LlmRequestConfig;
import ch.thp.cas.llm.variance.client.Manufacturer;
import ch.thp.cas.llm.variance.plan.Plan;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LlmVarianceApplication implements CommandLineRunner {

    private final Plan plan;
    private final ApplicationArguments appArgs;

    public LlmVarianceApplication(Plan plan, ApplicationArguments appArgs) {
        this.plan = plan;
        this.appArgs = appArgs;
    }

    public static void main(String[] args) {
        SpringApplication.run(LlmVarianceApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Manufacturer manufacturer = getOptionValue("manufacturer") != null
                ? Manufacturer.valueOf(getOptionValue("manufacturer"))
                : plan.getManufacturer();

        String model = getOptionValue("model") != null
                ? getOptionValue("model")
                : plan.getModel();
        if (model == null) {
            model = manufacturer.defaultModel();
        }

        String prompt = getOptionValue("prompt") != null
                ? getOptionValue("prompt")
                : plan.getPrompt();

        Double temperature = getOptionDouble("temperature") != null
                ? getOptionDouble("temperature")
                : plan.getTemperature();

        Double topP = getOptionDouble("topP") != null
                ? getOptionDouble("topP")
                : plan.getTopP();

        Integer topK = getOptionInteger("topK") != null
                ? getOptionInteger("topK")
                : plan.getTopK();

        int iterations = getOptionValue("iterations") != null
                ? Integer.parseInt(getOptionValue("iterations"))
                : plan.getIterations();

        LlmClient client = manufacturer.createClient();
        LlmRequestConfig config = new LlmRequestConfig(model, temperature, topP, topK);

        List<String> results = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            String answer = client.call(prompt, config);
            results.add(answer);
        }
        results.forEach(r -> System.out.println(r + ", "));
    }

    private String getOptionValue(String name) {
        if (appArgs.containsOption(name)) {
            List<String> values = appArgs.getOptionValues(name);
            if (values != null && !values.isEmpty()) {
                return values.getFirst();
            }
        }
        return null;
    }

    private Double getOptionDouble(String name) {
        String val = getOptionValue(name);
        return val != null ? Double.parseDouble(val) : null;
    }

    private Integer getOptionInteger(String name) {
        String val = getOptionValue(name);
        return val != null ? Integer.parseInt(val) : null;
    }
}
