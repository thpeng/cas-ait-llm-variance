package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.client.Manufacturer;

public record AnalysisRunInfo(
        String planName,
        Manufacturer manufacturer,
        String model,
        String modelVersion,
        int iterations,
        Double temperature,
        Double topP,
        Integer topK,
        Long seed
) {
}
