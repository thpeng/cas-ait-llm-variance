package ch.thp.cas.llm.variance.plan;

import ch.thp.cas.llm.variance.client.Manufacturer;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plan")
public class PlanProperties implements Plan {

    private Manufacturer manufacturer = Manufacturer.OPENAI;
    private String model;
    private String prompt = "Frage: Was war die Hauptstadt der Schweiz im Jahre 798?\nAntworte nur mit dem Städtenamen.";
    private Double temperature;
    private Double topP;
    private Integer topK;
    private int iterations = 30;

    @Override
    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    @Override
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @Override
    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    @Override
    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    @Override
    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }
}
