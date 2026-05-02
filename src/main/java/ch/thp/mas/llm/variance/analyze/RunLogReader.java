package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.run.RunLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RunLogReader {

    private static final Path DEFAULT_RUNS_DIRECTORY = Path.of("src", "main", "resources", "runs");

    private final Path runsDirectory;
    private final ObjectMapper objectMapper;

    public RunLogReader() {
        this(DEFAULT_RUNS_DIRECTORY, defaultObjectMapper());
    }

    RunLogReader(Path runsDirectory, ObjectMapper objectMapper) {
        this.runsDirectory = runsDirectory;
        this.objectMapper = objectMapper;
    }

    public NamedRunLog read(String filename) {
        String safeFilename = safeFilename(filename);
        Path path = runsDirectory.resolve(safeFilename);
        if (!Files.exists(path)) {
            throw new AnalysisException("Run log not found: " + filename);
        }
        try {
            return new NamedRunLog(safeFilename, objectMapper.readValue(path.toFile(), RunLog.class));
        } catch (IOException e) {
            throw new AnalysisException("Could not read run log: " + filename, e);
        }
    }

    public List<NamedRunLog> readAll() {
        try {
            if (!Files.exists(runsDirectory)) {
                throw new AnalysisException("Runs directory does not exist: " + runsDirectory);
            }
            List<Path> files = Files.list(runsDirectory)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
            if (files.isEmpty()) {
                throw new AnalysisException("No run logs found in: " + runsDirectory);
            }
            return files.stream()
                    .map(path -> read(path.getFileName().toString()))
                    .toList();
        } catch (IOException e) {
            throw new AnalysisException("Could not list run logs in: " + runsDirectory, e);
        }
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank() || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new AnalysisException("Invalid run log filename: " + filename);
        }
        return filename.endsWith(".json") ? filename : filename + ".json";
    }

    private static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
