package ch.thp.mas.llm.variance.run;

import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

@Component
public class SystemRunClock implements RunClock {

    @Override
    public OffsetDateTime now() {
        return OffsetDateTime.now();
    }
}
