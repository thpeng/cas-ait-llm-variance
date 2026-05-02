package ch.thp.mas.llm.variance.analyze;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextTokenizerTest {

    @Test
    void tokenizesUnicodeWordsAndNumbers() {
        assertThat(new TextTokenizer().tokenize("Grüezi, Zürich 2026!"))
                .containsExactly("grüezi", "zürich", "2026");
    }

    @Test
    void handlesFrenchAccentsAndItalianApostrophes() {
        TextTokenizer tokenizer = new TextTokenizer();

        assertThat(tokenizer.tokenize("déjà été")).containsExactly("déjà", "été");
        assertThat(tokenizer.tokenize("l'Italia è bella")).containsExactly("l", "italia", "è", "bella");
    }

    @Test
    void normalizesWhitespaceAndPunctuation() {
        TextTokenizer tokenizer = new TextTokenizer();

        assertThat(tokenizer.tokenize("Hello,\n\tworld! Version 4.5"))
                .containsExactly("hello", "world", "version", "4", "5");
    }

    @Test
    void returnsEmptyListForBlankOrPunctuationOnlyInput() {
        TextTokenizer tokenizer = new TextTokenizer();

        assertThat(tokenizer.tokenize("")).isEmpty();
        assertThat(tokenizer.tokenize("?!,.;")).isEmpty();
    }

    @Test
    void isDeterministic() {
        TextTokenizer tokenizer = new TextTokenizer();

        assertThat(tokenizer.tokenize("Grüezi Zürich"))
                .isEqualTo(tokenizer.tokenize("Grüezi Zürich"));
    }
}
