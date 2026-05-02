package ch.thp.mas.llm.variance.run;

import ch.thp.mas.llm.variance.client.TokenUsage;
import java.time.OffsetDateTime;

public record RunLogEntry(
        int index,
        OffsetDateTime startedAt,
        OffsetDateTime endedAt,
        String response,
        TokenUsage tokenUsage
) {
}
