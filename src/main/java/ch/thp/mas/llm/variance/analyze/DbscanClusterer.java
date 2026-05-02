package ch.thp.mas.llm.variance.analyze;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import org.springframework.stereotype.Component;

@Component
public class DbscanClusterer {

    private static final int UNVISITED = Integer.MIN_VALUE;
    private static final int NOISE = -1;

    public int[] cluster(double[][] distances, DbscanConfig config) {
        int[] labels = new int[distances.length];
        Arrays.fill(labels, UNVISITED);
        int clusterId = 0;

        for (int point = 0; point < distances.length; point++) {
            if (labels[point] != UNVISITED) {
                continue;
            }
            int[] neighbors = neighbors(distances, point, config.epsilon());
            if (neighbors.length < config.minPts()) {
                labels[point] = NOISE;
                continue;
            }
            expandCluster(distances, labels, point, neighbors, clusterId, config);
            clusterId++;
        }
        return labels;
    }

    private void expandCluster(
            double[][] distances,
            int[] labels,
            int point,
            int[] neighbors,
            int clusterId,
            DbscanConfig config
    ) {
        labels[point] = clusterId;
        Queue<Integer> queue = new ArrayDeque<>();
        for (int neighbor : neighbors) {
            queue.add(neighbor);
        }

        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (labels[current] == NOISE) {
                labels[current] = clusterId;
            }
            if (labels[current] != UNVISITED) {
                continue;
            }
            labels[current] = clusterId;
            int[] currentNeighbors = neighbors(distances, current, config.epsilon());
            if (currentNeighbors.length >= config.minPts()) {
                for (int neighbor : currentNeighbors) {
                    queue.add(neighbor);
                }
            }
        }
    }

    private int[] neighbors(double[][] distances, int point, double epsilon) {
        return java.util.stream.IntStream.range(0, distances.length)
                .filter(candidate -> distances[point][candidate] <= epsilon)
                .toArray();
    }
}
