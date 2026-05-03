package ch.thp.mas.llm.variance.analyze;

import ch.thp.mas.llm.variance.run.RunLog;
import ch.thp.mas.llm.variance.run.RunLogEntry;
import ch.thp.mas.llm.variance.run.SystemRunClock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class Analyzer {

    private final EmbeddingService embeddingService;
    private final CosineDistance cosineDistance;
    private final MedoidSelector medoidSelector;
    private final DbscanClusterer dbscanClusterer;
    private final RougeLMetric rougeLMetric;
    private final BleuMetric bleuMetric;
    private final SummaryStatistics summaryStatistics;
    private final SystemRunClock runClock;

    public Analyzer(
            EmbeddingService embeddingService,
            CosineDistance cosineDistance,
            MedoidSelector medoidSelector,
            DbscanClusterer dbscanClusterer,
            RougeLMetric rougeLMetric,
            BleuMetric bleuMetric,
            SummaryStatistics summaryStatistics,
            SystemRunClock runClock
    ) {
        this.embeddingService = embeddingService;
        this.cosineDistance = cosineDistance;
        this.medoidSelector = medoidSelector;
        this.dbscanClusterer = dbscanClusterer;
        this.rougeLMetric = rougeLMetric;
        this.bleuMetric = bleuMetric;
        this.summaryStatistics = summaryStatistics;
        this.runClock = runClock;
    }

    public AnalysisResult analyze(NamedRunLog namedRunLog) {
        AnalysisConfig config = AnalysisConfig.defaults();
        RunLog runLog = namedRunLog.runLog();
        List<String> responses = runLog.repetitions().stream()
                .map(RunLogEntry::response)
                .toList();
        if (responses.isEmpty()) {
            throw new AnalysisException("Run log has no responses: " + namedRunLog.filename());
        }

        List<EmbeddingResult> embeddings = embeddingService.embed(responses, config);
        double[][] distances = cosineDistance.pairwise(embeddings);
        Medoid medoid = medoidSelector.select(distances);
        int[] labels = dbscanClusterer.cluster(distances, config.dbscan());
        List<SemanticCluster> semanticClusters = semanticClusters(labels, distances);

        return new AnalysisResult(
                namedRunLog.filename(),
                runClock.now(),
                config,
                runInfo(runLog),
                new SemanticAnalysis(
                        responses.size(),
                        (int) embeddings.stream().filter(EmbeddingResult::truncated).count(),
                        new MedoidAnalysis(
                                medoid.index() + 1,
                                medoid.totalDistance(),
                                responses.get(medoid.index())
                        ),
                        summaryStatistics.summarize(upperTriangle(distances)),
                        semanticClusters,
                        outliers(labels)
                ),
                new SyntacticAnalysis(syntacticClusters(semanticClusters, responses))
        );
    }

    private AnalysisRunInfo runInfo(RunLog runLog) {
        return new AnalysisRunInfo(
                runLog.planName(),
                runLog.manufacturer(),
                runLog.model(),
                runLog.modelVersion(),
                runLog.iterations(),
                runLog.config().temperature(),
                runLog.config().topP(),
                runLog.config().topK(),
                runLog.config().seed()
        );
    }

    private List<SemanticCluster> semanticClusters(int[] labels, double[][] distances) {
        Map<Integer, List<Integer>> byCluster = new LinkedHashMap<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] >= 0) {
                byCluster.computeIfAbsent(labels[i], ignored -> new ArrayList<>()).add(i);
            }
        }
        return byCluster.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> semanticCluster(entry.getKey(), entry.getValue(), distances))
                .toList();
    }

    private SemanticCluster semanticCluster(int clusterId, List<Integer> indices, double[][] distances) {
        Integer medoidIndex = indices.size() > 1
                ? clusterMedoid(indices, distances) + 1
                : indices.getFirst() + 1;
        return new SemanticCluster(
                clusterId,
                indices.size(),
                indices.stream().map(index -> index + 1).toList(),
                medoidIndex,
                summaryStatistics.summarize(pairDistances(indices, distances))
        );
    }

    private int clusterMedoid(List<Integer> indices, double[][] distances) {
        return indices.stream()
                .min(Comparator.comparingDouble(index -> indices.stream()
                        .mapToDouble(other -> distances[index][other])
                        .sum()))
                .orElseThrow();
    }

    private List<SyntacticCluster> syntacticClusters(List<SemanticCluster> semanticClusters, List<String> responses) {
        return semanticClusters.stream()
                .map(cluster -> syntacticCluster(cluster, responses))
                .toList();
    }

    private SyntacticCluster syntacticCluster(SemanticCluster cluster, List<String> responses) {
        List<Integer> indices = cluster.repetitionIndices().stream().map(index -> index - 1).toList();
        List<Double> rougeDistances = new ArrayList<>();
        List<Double> bleuDistances = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            for (int j = i + 1; j < indices.size(); j++) {
                String left = responses.get(indices.get(i));
                String right = responses.get(indices.get(j));
                rougeDistances.add(1.0 - rougeLMetric.score(left, right));
                bleuDistances.add(1.0 - bleuMetric.score(left, right, AnalysisConfig.defaults().bleu()));
            }
        }
        return new SyntacticCluster(
                cluster.clusterId(),
                rougeDistances.size(),
                summaryStatistics.summarize(rougeDistances),
                summaryStatistics.summarize(bleuDistances)
        );
    }

    private List<Integer> outliers(int[] labels) {
        List<Integer> outliers = new ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] < 0) {
                outliers.add(i + 1);
            }
        }
        return outliers;
    }

    private List<Double> upperTriangle(double[][] distances) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < distances.length; i++) {
            for (int j = i + 1; j < distances.length; j++) {
                values.add(distances[i][j]);
            }
        }
        return values;
    }

    private List<Double> pairDistances(List<Integer> indices, double[][] distances) {
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < indices.size(); i++) {
            for (int j = i + 1; j < indices.size(); j++) {
                values.add(distances[indices.get(i)][indices.get(j)]);
            }
        }
        return values;
    }
}
