package ch.thp.mas.llm.variance.analyze;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BleuMetric {

    private final TextTokenizer tokenizer;

    public BleuMetric(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public double score(String candidate, String reference, BleuConfig config) {
        List<String> candidateTokens = tokenizer.tokenize(candidate);
        List<String> referenceTokens = tokenizer.tokenize(reference);
        if (candidateTokens.isEmpty()) {
            return 0.0;
        }

        double logPrecisionSum = 0.0;
        for (int n = 1; n <= config.maxN(); n++) {
            Counts counts = modifiedPrecision(candidateTokens, referenceTokens, n);
            double precision = (counts.matches + 1.0) / (counts.total + 1.0);
            logPrecisionSum += Math.log(precision);
        }

        double brevityPenalty = candidateTokens.size() > referenceTokens.size()
                ? 1.0
                : Math.exp(1.0 - ((double) referenceTokens.size() / candidateTokens.size()));
        return brevityPenalty * Math.exp(logPrecisionSum / config.maxN());
    }

    private Counts modifiedPrecision(List<String> candidate, List<String> reference, int n) {
        Map<String, Integer> candidateCounts = ngrams(candidate, n);
        Map<String, Integer> referenceCounts = ngrams(reference, n);
        int matches = 0;
        int total = 0;
        for (Map.Entry<String, Integer> entry : candidateCounts.entrySet()) {
            int count = entry.getValue();
            total += count;
            matches += Math.min(count, referenceCounts.getOrDefault(entry.getKey(), 0));
        }
        return new Counts(matches, total);
    }

    private Map<String, Integer> ngrams(List<String> tokens, int n) {
        Map<String, Integer> counts = new HashMap<>();
        if (tokens.size() < n) {
            return counts;
        }
        for (int i = 0; i <= tokens.size() - n; i++) {
            String ngram = String.join(" ", tokens.subList(i, i + n));
            counts.merge(ngram, 1, Integer::sum);
        }
        return counts;
    }

    private record Counts(int matches, int total) {
    }
}
