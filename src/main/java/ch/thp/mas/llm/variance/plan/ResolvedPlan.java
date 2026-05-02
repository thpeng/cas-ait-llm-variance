package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.Manufacturer;

public record ResolvedPlan(
        String name,
        Manufacturer manufacturer,
        String model,
        String prompt,
        int iterations,
        Double temperature,
        Double topP,
        Integer topK,
        Long seed,
        String modelVersion
) implements Plan {

    @Override
    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    @Override
    public String getModel() {
        return model;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    @Override
    public Double getTemperature() {
        return temperature;
    }

    @Override
    public Double getTopP() {
        return topP;
    }

    @Override
    public Integer getTopK() {
        return topK;
    }

    @Override
    public Long getSeed() {
        return seed;
    }

    @Override
    public int getIterations() {
        return iterations;
    }
}
