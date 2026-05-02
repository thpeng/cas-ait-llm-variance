package ch.thp.mas.llm.variance.analyze;

import java.util.List;

public interface EmbeddingService {

    List<EmbeddingResult> embed(List<String> texts, AnalysisConfig config);
}
