package ch.thp.mas.llm.variance.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.thp.mas.llm.variance.client.Manufacturer;
import ch.thp.mas.llm.variance.client.TokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunLogWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesUtf8JsonRunLog() throws Exception {
        RunLogWriter writer = new RunLogWriter(tempDir.resolve("runs"), new RunFileNameFactory(), objectMapper());
        RunLog runLog = runLog();

        Path written = writer.write(runLog);

        assertThat(written).exists();
        assertThat(written.getFileName().toString()).isEqualTo("20260502-104530-123-0001-test.json");
        String json = Files.readString(written);
        assertThat(json).contains("\"planName\":\"0001-test\"");
        assertThat(json).contains("\"response\":\"Vollständige Antwort\"");
        assertThat(json).contains("\"totalTokens\":15");
    }

    @Test
    void doesNotOverwriteExistingLogFile() {
        RunLogWriter writer = new RunLogWriter(tempDir.resolve("runs"), new RunFileNameFactory(), objectMapper());
        RunLog runLog = runLog();
        writer.write(runLog);

        assertThatThrownBy(() -> writer.write(runLog))
                .isInstanceOf(RunLoggingException.class)
                .hasMessageContaining("Could not write run log");
    }

    private static RunLog runLog() {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-02T10:45:30.123+02:00");
        return new RunLog(
                "0001-test",
                startedAt,
                OffsetDateTime.parse("2026-05-02T10:45:31.123+02:00"),
                Manufacturer.OPENAI,
                "gpt-test",
                null,
                1,
                new RunConfigLog(0.1, 0.9, 4, 123L),
                "Prompt",
                List.of(new RunLogEntry(
                        1,
                        OffsetDateTime.parse("2026-05-02T10:45:30.200+02:00"),
                        OffsetDateTime.parse("2026-05-02T10:45:31.000+02:00"),
                        "Vollständige Antwort",
                        new TokenUsage(10L, 5L, 15L)
                ))
        );
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
