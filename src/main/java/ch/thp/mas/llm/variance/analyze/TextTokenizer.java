package ch.thp.mas.llm.variance.analyze;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unicode-aware whitespace and punctuation tokenizer used by all surface-level
 * similarity metrics in this package.
 *
 * <p>The tokenizer extracts maximal runs of Unicode letters ({@code \p{L}}) and
 * digits ({@code \p{N}}) from the input, discarding all other characters
 * (whitespace, punctuation, symbols, control characters). Input is lowercased
 * with {@link Locale#ROOT} prior to matching, making tokenization
 * locale-insensitive and reproducible across runtimes.
 *
 * <p>Properties:
 * <ul>
 *   <li>Handles non-ASCII letters correctly: German umlauts (ä, ö, ü, ß),
 *       French accents, Italian diacritics, and similar are preserved.</li>
 *   <li>Hyphenated words and contractions are split (e.g. "U-Bahn" &rarr;
 *       {@code ["u", "bahn"]}, "don't" &rarr; {@code ["don", "t"]},
 *       "l'avion" &rarr; {@code ["l", "avion"]}).</li>
 *   <li>Numbers are preserved but split on non-digit separators
 *       (e.g. "Version 2.5" &rarr; {@code ["version", "2", "5"]}).</li>
 *   <li>No stemming, lemmatization, or stopword removal is applied.</li>
 * </ul>
 *
 * <p>This is conceptually equivalent to NLTK's
 * {@code RegexpTokenizer(r'\w+')} after lowercasing, but Unicode-aware where
 * Google's {@code rouge_score} default ({@code [a-z0-9]+}) is ASCII-only.
 * Cross-verification of ROUGE-L or BLEU scores against {@code rouge_score} or
 * NLTK should account for this difference on non-ASCII inputs.
 *
 * <p>The same instance is shared by {@link BleuMetric} and {@link RougeLMetric}
 * to ensure both metrics operate on identical token sequences.
 */
@Component
public class TextTokenizer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");

    /**
     * Tokenizes the given text into lowercased Unicode letter/digit runs.
     *
     * @param text the input string; must not be {@code null}
     * @return an ordered list of tokens; empty if the input contains no
     * letter or digit characters
     * @throws NullPointerException if {@code text} is {@code null}
     */
    public List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }
}
