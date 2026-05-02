package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.Manufacturer;
import java.time.OffsetDateTime;
import java.util.List;

public record RunLog(
        String planName,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        Manufacturer manufacturer,
        String model,
        String modelVersion,
        int iterations,
        RunConfigLog config,
        String prompt,
        List<RunLogEntry> repetitions
) {
}
