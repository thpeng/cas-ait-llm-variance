package ch.thp.mas.llm.variance.analyze;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class E5HttpEmbeddingService implements EmbeddingService {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public E5HttpEmbeddingService(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<EmbeddingResult> embed(List<String> texts, AnalysisConfig config) {
        boolean loaded = false;
        try {
            post("/load", "");
            loaded = true;
            EmbedResponse response = embedTexts(textsForEmbedding(texts, config));
            return toEmbeddingResults(response, texts.size());
        } finally {
            if (loaded) {
                post("/unload", "");
            }
        }
    }

    private EmbedResponse embedTexts(List<String> texts) {
        try {
            String body = objectMapper.writeValueAsString(new EmbedRequest(texts));
            HttpResponse<String> response = post("/embed", body);
            return objectMapper.readValue(response.body(), EmbedResponse.class);
        } catch (IOException e) {
            throw new AnalysisException("Could not parse embedding service response.", e);
        }
    }

    private List<String> textsForEmbedding(List<String> texts, AnalysisConfig config) {
        return texts.stream()
                .map(text -> config.embeddingPrefix().isBlank()
                        ? text
                        : config.embeddingPrefix() + " " + text)
                .toList();
    }

    private List<EmbeddingResult> toEmbeddingResults(EmbedResponse response, int expectedCount) {
        if (response.count() != expectedCount) {
            throw new AnalysisException("Embedding service returned count " + response.count()
                    + " but " + expectedCount + " embeddings were requested.");
        }
        if (response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new AnalysisException("Embedding service returned no embeddings.");
        }
        if (response.embeddings().size() != expectedCount) {
            throw new AnalysisException("Embedding service returned " + response.embeddings().size()
                    + " vectors but " + expectedCount + " embeddings were requested.");
        }
        for (List<Double> vector : response.embeddings()) {
            if (vector.size() != response.dim()) {
                throw new AnalysisException("Embedding vector dimension " + vector.size()
                        + " does not match response dim " + response.dim() + ".");
            }
        }
        return response.embeddings().stream()
                .map(vector -> new EmbeddingResult(toArray(vector), false))
                .toList();
    }

    private HttpResponse<String> post(String path, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body == null || body.isBlank() ? "{}" : body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AnalysisException("Embedding service " + path + " failed with HTTP "
                        + response.statusCode() + ": " + response.body());
            }
            return response;
        } catch (IOException e) {
            throw new AnalysisException("Could not reach embedding service at " + baseUrl + path + ".", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnalysisException("Interrupted while calling embedding service at " + baseUrl + path + ".", e);
        }
    }

    private double[] toArray(List<Double> vector) {
        double[] values = new double[vector.size()];
        for (int i = 0; i < vector.size(); i++) {
            values[i] = vector.get(i);
        }
        return values;
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private record EmbedRequest(List<String> texts) {
    }

    private record EmbedResponse(int dim, int count, List<List<Double>> embeddings) {
    }
}
