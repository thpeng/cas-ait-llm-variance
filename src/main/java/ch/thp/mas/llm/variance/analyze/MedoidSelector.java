package ch.thp.mas.llm.variance.analyze;

import org.springframework.stereotype.Component;

@Component
public class MedoidSelector {

    public Medoid select(double[][] distances) {
        int bestIndex = -1;
        double bestTotal = Double.POSITIVE_INFINITY;
        for (int i = 0; i < distances.length; i++) {
            double total = 0.0;
            for (double distance : distances[i]) {
                total += distance;
            }
            if (total < bestTotal) {
                bestTotal = total;
                bestIndex = i;
            }
        }
        return new Medoid(bestIndex, bestTotal);
    }
}
