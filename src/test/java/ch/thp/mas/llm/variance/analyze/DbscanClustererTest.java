package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DbscanClustererTest {

    @Test
    void clustersByDistanceMatrixAndMarksOutliers() {
        double[][] distances = {
                {0.0, 0.1, 0.9},
                {0.1, 0.0, 0.9},
                {0.9, 0.9, 0.0}
        };

        int[] labels = new DbscanClusterer().cluster(distances, new DbscanConfig(0.2, 2));

        assertThat(labels).containsExactly(0, 0, -1);
    }
}
