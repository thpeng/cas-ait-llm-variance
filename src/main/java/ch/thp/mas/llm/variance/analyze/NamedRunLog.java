package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.run.RunLog;

public record NamedRunLog(String filename, RunLog runLog) {
}
