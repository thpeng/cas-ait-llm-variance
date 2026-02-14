package ch.thp.cas.llm.variance.client;

public record LlmRequestConfig(
        String model,
        Double temperature,
        Double topP,
        Integer topK
) {
}
