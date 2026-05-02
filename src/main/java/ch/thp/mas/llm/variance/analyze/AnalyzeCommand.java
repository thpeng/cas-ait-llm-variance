package ch.thp.mas.llm.variance.analyze;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

@Component
public class AnalyzeCommand {

    private static final String ALL = "ALL";

    private final RunLogReader runLogReader;
    private final Analyzer analyzer;
    private final AnalysisWriter analysisWriter;

    public AnalyzeCommand(RunLogReader runLogReader, Analyzer analyzer, AnalysisWriter analysisWriter) {
        this.runLogReader = runLogReader;
        this.analyzer = analyzer;
        this.analysisWriter = analysisWriter;
    }

    public List<Path> run(ApplicationArguments appArgs) {
        String selection = optionValue(appArgs);
        List<NamedRunLog> runLogs = ALL.equalsIgnoreCase(selection)
                ? runLogReader.readAll()
                : List.of(runLogReader.read(selection));
        return runLogs.stream()
                .map(runLog -> analysisWriter.write(runLog.filename(), analyzer.analyze(runLog)))
                .toList();
    }

    private String optionValue(ApplicationArguments appArgs) {
        List<String> values = appArgs.getOptionValues("analyze");
        if (values == null || values.isEmpty() || values.getFirst().isBlank()) {
            throw new AnalysisException("--analyze must specify a run log filename or ALL.");
        }
        return values.getFirst();
    }
}
