package ch.thp.cas.llm.variance.plan;

import ch.thp.cas.llm.variance.client.Manufacturer;

public interface Plan {

    Manufacturer getManufacturer();

    String getModel();

    String getPrompt();

    Double getTemperature();

    Double getTopP();

    Integer getTopK();

    int getIterations();
}
