package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextTokenizerTest {

    @Test
    void tokenizesUnicodeWordsAndNumbers() {
        assertThat(new TextTokenizer().tokenize("Grüezi, Zürich 2026!"))
                .containsExactly("grüezi", "zürich", "2026");
    }
}
