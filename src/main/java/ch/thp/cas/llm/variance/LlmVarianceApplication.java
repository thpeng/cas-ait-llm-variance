package ch.thp.cas.llm.variance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LlmVarianceApplication implements CommandLineRunner {

    private static final String OPENAI_URL = "https://api.openai.com/v1/responses";
    // Modell-Snapshot fixieren (keine "latest"-Aliasse). Bei Bedarf austauschen.
    private static final String MODEL = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-5-mini-2025-08-07");
    public static void main(String[] args) {
        SpringApplication.run(LlmVarianceApplication.class, args);
    }
    private static List<String> hauptstadt= new ArrayList<>();

    @Override
    public void run(String... args) throws Exception {

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Bitte OPENAI_API_KEY als Environment-Variable setzen.");
            System.exit(1);
        }

        // Deterministische Baseline-Frage
        String prompt = "Frage: Was war die Hauptstadt der Schweiz im Jahre 798?\n"
                + "Antworte nur mit dem Städtenamen.";

        String requestBody = """
            {
              "model": "%s",
              "input": %s
                                                  }
            """.formatted(MODEL, toJsonString(prompt));

        for (int i = 0; i< 30; i++) {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                System.err.println("HTTP " + resp.statusCode() + ": " + resp.body());
                System.exit(2);
            }

            // Robust: erst "output_text", sonst aus "output[*].content[*].text"
            String answer = extractText(resp.body());
            hauptstadt.add(answer);
        }
        hauptstadt.stream().forEach(h -> System.out.println(h + ", "));
        // Erwartet: "Bern"
    }

    // JSON-escape fuer den Prompt
    private static String toJsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String extractText(String json) throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(json);

        // 1) Einige Responses enthalten ein Convenience-Feld "output_text"
        if (root.hasNonNull("output_text")) {
            return root.get("output_text").asText().trim();
        }

        // 2) Generisch: output[*] -> message.content[*].text (type=output_text)
        if (root.has("output")) {
            for (JsonNode item : root.get("output")) {
                if ("message".equals(item.path("type").asText())) {
                    for (JsonNode part : item.path("content")) {
                        String t = part.path("type").asText();
                        if ("output_text".equals(t) || "text".equals(t)) {
                            String txt = part.path("text").asText();
                            if (txt != null && !txt.isBlank()) return txt.trim();
                        }
                    }
                }
            }
        }

        // 3) Fallback: gesamtes JSON (zur Fehlersuche)
        return json;
    }
}