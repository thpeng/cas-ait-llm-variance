package ch.thp.mas.llm.variance.analyze;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RougeLMetric {

    private final TextTokenizer tokenizer;

    public RougeLMetric(TextTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public double score(String candidate, String reference) {
        List<String> candidateTokens = tokenizer.tokenize(candidate);
        List<String> referenceTokens = tokenizer.tokenize(reference);
        if (candidateTokens.isEmpty() || referenceTokens.isEmpty()) {
            return 0.0;
        }
        int lcs = lcs(candidateTokens, referenceTokens);
        double precision = (double) lcs / candidateTokens.size();
        double recall = (double) lcs / referenceTokens.size();
        if (precision + recall == 0.0) {
            return 0.0;
        }
        return 2 * precision * recall / (precision + recall);
    }

    private int lcs(List<String> a, List<String> b) {
        int[][] dp = new int[a.size() + 1][b.size() + 1];
        for (int i = 1; i <= a.size(); i++) {
            for (int j = 1; j <= b.size(); j++) {
                if (a.get(i - 1).equals(b.get(j - 1))) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[a.size()][b.size()];
    }
}
