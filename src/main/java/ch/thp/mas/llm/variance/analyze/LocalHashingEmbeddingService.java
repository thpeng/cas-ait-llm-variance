package ch.thp.mas.llm.variance.analyze;

import java.util.List;

public class LocalHashingEmbeddingService implements EmbeddingService {

    private static final int DIMENSIONS = 384;

    private final TextTokenizer tokenizer;

    public LocalHashingEmbeddingService(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    @Override
    public List<EmbeddingResult> embed(List<String> texts, AnalysisConfig config) {
        return texts.stream()
                .map(text -> embedOne(textForEmbedding(text, config), config.maxEmbeddingTokens()))
                .toList();
    }

    private String textForEmbedding(String text, AnalysisConfig config) {
        return config.embeddingPrefix().isBlank()
                ? text
                : config.embeddingPrefix() + " " + text;
    }

    private EmbeddingResult embedOne(String text, int maxTokens) {
        List<String> tokens = tokenizer.tokenize(text);
        boolean truncated = tokens.size() > maxTokens;
        List<String> usedTokens = truncated ? tokens.subList(0, maxTokens) : tokens;
        double[] vector = new double[DIMENSIONS];
        for (String token : usedTokens) {
            int hash = token.hashCode();
            int index = Math.floorMod(hash, DIMENSIONS);
            vector[index] += (hash & 1) == 0 ? 1.0 : -1.0;
        }
        normalize(vector);
        return new EmbeddingResult(vector, truncated);
    }

    private void normalize(double[] vector) {
        double sum = 0.0;
        for (double value : vector) {
            sum += value * value;
        }
        double norm = Math.sqrt(sum);
        if (norm == 0.0) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
