package ch.thp.mas.llm.variance.client;

public record TokenUsage(Long inputTokens, Long outputTokens, Long totalTokens) {

    public static TokenUsage of(Long inputTokens, Long outputTokens) {
        Long totalTokens = inputTokens != null && outputTokens != null
                ? inputTokens + outputTokens
                : null;
        return new TokenUsage(inputTokens, outputTokens, totalTokens);
    }
}
