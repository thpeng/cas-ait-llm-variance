package ch.thp.mas.llm.variance.analyze;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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

    @Test
    void lowercasesLocaleInsensitively() {
        TextTokenizer tokenizer = new TextTokenizer();
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            // In Turkish locale, "I".toLowerCase() yields "ı" (dotless i),
            // not "i". Locale.ROOT in the implementation must prevent this.
            assertThat(tokenizer.tokenize("ISTANBUL"))
                    .containsExactly("istanbul");
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    void preservesGermanSharpS() {
        assertThat(new TextTokenizer().tokenize("Straße Großbuchstaben"))
                .containsExactly("straße", "großbuchstaben");
    }

    @Test
    void discardsSymbolsAndEmoji() {
        assertThat(new TextTokenizer().tokenize("Preis: 25€ ✓ 👍"))
                .containsExactly("preis", "25");
    }

    @Test
    void throwsOnNullInput() {
        assertThatThrownBy(() -> new TextTokenizer().tokenize(null))
                .isInstanceOf(NullPointerException.class);
    }
}
