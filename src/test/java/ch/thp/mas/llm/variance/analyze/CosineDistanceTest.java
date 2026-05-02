package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CosineDistanceTest {

    @Test
    void computesCosineDistance() {
        CosineDistance distance = new CosineDistance();

        assertThat(distance.distance(new double[]{1, 0}, new double[]{1, 0})).isEqualTo(0.0);
        assertThat(distance.distance(new double[]{1, 0}, new double[]{0, 1})).isEqualTo(1.0);
    }
}
