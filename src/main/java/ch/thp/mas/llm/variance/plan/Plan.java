package ch.thp.mas.llm.variance.plan;

import ch.thp.mas.llm.variance.client.Manufacturer;

public interface Plan {

    Manufacturer getManufacturer();

    String getModel();

    String getPrompt();

    Double getTemperature();

    Double getTopP();

    Integer getTopK();

    int getIterations();
}
