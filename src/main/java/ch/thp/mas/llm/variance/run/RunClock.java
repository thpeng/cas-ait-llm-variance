package ch.thp.mas.llm.variance.run;

import java.time.OffsetDateTime;

public interface RunClock {

    OffsetDateTime now();
}
