# Analyze Algorithm Test Spec

## Goal

Strengthen the test suite for the analysis package so the semantic and syntactic evaluation is trustworthy enough to support the MAS work.

The current tests verify that the pipeline runs and that each algorithm has a minimal sanity check. That is not enough. The analysis output will be used to argue about variance, stability, clusters, outliers, and relative distances. The tests therefore need to validate algorithmic correctness, edge cases, numerical stability, and regression behavior.

## Scope

This spec covers tests for:

- Tokenization
- Cosine distance
- Pairwise distance matrix construction
- Medoid selection
- DBSCAN clustering
- Summary statistics
- BLEU with smoothing
- ROUGE-L
- Syntactic pairwise aggregation per cluster
- Full analyzer behavior with deterministic embeddings
- Golden analysis fixtures

This spec does not require the real multilingual-e5-large model in the default test suite. Model-backed tests should be optional/profile-gated because model artifacts are large and environment-dependent.

## Test Layers

Use four layers:

1. Exact unit tests for deterministic pure functions.
2. Invariant tests for mathematical properties.
3. Synthetic scenario tests with known clusters and medoids.
4. Golden fixture tests that lock complete analysis JSON output for representative runs.

The first three layers should run in every `gradle test`. The model-backed smoke test should be disabled by default or gated behind a profile/system property.

## Test Fixtures

Create dedicated fixtures under:

```text
src/test/resources/analyze/
```

Suggested structure:

```text
src/test/resources/analyze/
  runs/
    single-word-capital.json
    two-clear-clusters.json
    outlier-run.json
    syntactic-paraphrases.json
    empty-responses.json
  expected/
    single-word-capital-analysis.json
    two-clear-clusters-analysis.json
    outlier-run-analysis.json
    syntactic-paraphrases-analysis.json
```

Golden files should be stable because tests use a fake deterministic `EmbeddingService` and fixed clock.

## Numeric Tolerances

Use explicit tolerances for floating point assertions:

```text
defaultTolerance = 1e-9
metricTolerance = 1e-6
```

Golden JSON tests should either:

- compare exact values after rounding to a fixed precision, or
- deserialize JSON and compare numeric fields with tolerances.

Do not compare raw pretty-printed JSON strings unless field order and rounding are intentionally controlled.

## Tokenizer Tests

Test `TextTokenizer` extensively because BLEU and ROUGE-L depend on it.

Cases:

- German umlauts: `Grüezi Zürich` -> `grüezi`, `zürich`
- French accents: `déjà été` -> `déjà`, `été`
- Italian apostrophe/punctuation handling: `l'Italia è bella` -> `l`, `italia`, `è`, `bella`
- English punctuation: `Hello, world!` -> `hello`, `world`
- Numbers: `2026 Version 4.5` -> `2026`, `version`, `4`, `5`
- Whitespace normalization: tabs, newlines, repeated spaces
- Empty string -> empty token list
- Punctuation-only string -> empty token list

Also test determinism:

- Same input produces identical token sequence across repeated calls.

## Cosine Distance Tests

Exact cases:

- Identical unit vectors -> `0.0`
- Orthogonal vectors -> `1.0`
- Opposite vectors -> `2.0`
- Non-normalized identical direction vectors -> `0.0`
- One zero vector -> `1.0`

Matrix cases:

- Pairwise matrix is square.
- Diagonal is `0.0`.
- Matrix is symmetric.
- Known vectors produce expected distances.

Invariant tests:

- `distance(a, b) == distance(b, a)`
- `distance(a, a) == 0.0` for non-zero vectors
- Distance range is `[0.0, 2.0]` for normal finite vectors

## Medoid Tests

Use explicit distance matrices where the medoid is obvious.

Cases:

- Single response -> medoid index `0`, total distance `0.0`
- Three responses where middle point minimizes total distance
- Tie case: define and test deterministic tie behavior. Recommendation: lowest index wins.
- Cluster medoid inside a subset should not be confused with global medoid.

Example:

```text
distances:
  0: [0.0, 0.1, 0.9]
  1: [0.1, 0.0, 0.8]
  2: [0.9, 0.8, 0.0]

expected medoid: 1
```

## DBSCAN Tests

DBSCAN is central to the semantic interpretation. Add a much broader suite.

Cases:

- One dense cluster, no outliers
- Two dense clusters, no outliers
- One dense cluster plus one outlier
- Border point joins a cluster
- All points are noise
- Duplicate points form a cluster
- `minPts = 1` puts every point into a cluster
- `epsilon = 0.0` clusters only identical points
- Deterministic cluster labels for the same input

Test using distance matrices, not embeddings, so expected behavior is precise.

Important assertions:

- Noise label is `-1`
- Cluster labels are deterministic and start at `0`
- All cluster members are reachable by DBSCAN density rules
- Input order sensitivity is controlled or documented

## Summary Statistics Tests

Cases:

- Empty list -> count `0`, numeric fields `null`
- Single value -> all summaries equal that value
- Odd count median
- Even count median
- p90 nearest-rank behavior
- Negative values if utility is generic
- Unsorted input produces same output as sorted input

Explicit p90 examples:

```text
values = [1, 2, 3, 4, 5]
p90 nearest-rank = 5

values = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
p90 nearest-rank = 9
```

## ROUGE-L Tests

ROUGE-L must be tested against hand-computable examples.

Cases:

- Identical text -> F1 `1.0`
- No overlap -> F1 `0.0`
- Partial ordered overlap
- Same tokens different order
- Candidate shorter than reference
- Candidate longer than reference
- Empty candidate
- Empty reference
- Multilingual token examples

Hand-computable example:

```text
candidate: "a b c"
reference: "a x c"
LCS length: 2
precision: 2/3
recall: 2/3
F1: 2/3
distance: 1/3
```

Add tests that assert the analyzer converts ROUGE-L similarity to distance with:

```text
distance = 1.0 - score
```

## BLEU Tests

BLEU with smoothing must be tested carefully because short generated answers are common.

Cases:

- Identical short text returns positive score and is close to high similarity
- No overlap still returns non-zero smoothed score
- Candidate shorter than reference applies brevity penalty
- Candidate longer than reference has brevity penalty `1.0`
- Repeated n-grams use clipped counts
- Empty candidate returns `0.0`
- Empty reference with non-empty candidate behaves deterministically

Hand-computable examples should be documented in test names or comments.

Also test smoothing explicitly:

```text
precision_n = (matches_n + 1.0) / (candidate_ngrams_n + 1.0)
```

This prevents accidental replacement with a different smoothing method.

## Syntactic Aggregation Tests

Use fixed semantic clusters and response texts.

Cases:

- Cluster with one response -> pair count `0`, summaries null
- Cluster with two responses -> pair count `1`, median and p90 equal the single distance
- Cluster with three responses -> pair count `3`
- ROUGE-L and BLEU distances are separately aggregated
- Outliers are excluded from cluster syntactic aggregation

Test that pair count follows:

```text
n * (n - 1) / 2
```

## Full Analyzer Scenario Tests

Use fake embeddings so semantic behavior is deterministic.

### Scenario 1: Single-Word Stable Answers

Responses:

```text
Bern
Bern
Bern
```

Expected:

- One cluster
- No outliers
- Medoid is first response by tie rule
- Pairwise cosine distance summary is all zeros
- ROUGE-L distance median `0.0`
- BLEU distance very low, exact value according to smoothing

### Scenario 2: Two Clear Semantic Clusters

Responses:

```text
Bern ist die Hauptstadt.
Die Hauptstadt ist Bern.
Zürich ist eine grosse Stadt.
Eine Rundreise durch die Schweiz ...
Die Reise startet in Genf ...
```

Fake embeddings assign:

- First three responses near cluster A
- Last two responses near cluster B

Expected:

- Two clusters
- No outliers
- Correct repetition indices per cluster
- Correct medoid per cluster
- Syntactic summaries per cluster only compare within the cluster

### Scenario 3: Outlier

Responses:

```text
Bern ist die Hauptstadt.
Die Hauptstadt ist Bern.
Pizza Rezept mit Tomaten.
```

Expected:

- One cluster with responses 1 and 2
- Response 3 listed as outlier
- Outlier not included in syntactic cluster aggregation

### Scenario 4: Truncated Embedding Input

Use a fake embedding service that marks one response as truncated.

Expected:

- `truncatedResponses` count is correct
- Limitation/warning is present if truncation reporting is implemented

## Golden JSON Tests

Add golden tests for full `AnalysisResult` output.

Approach:

1. Load a run fixture from `src/test/resources/analyze/runs`.
2. Use fake embeddings and fixed clock.
3. Run analyzer.
4. Compare selected full JSON tree against expected fixture.

Fields to compare:

- Source run
- Config
- Run metadata
- Semantic response count
- Medoid repetition index and total distance
- Pairwise summary
- Cluster list
- Outlier list
- Syntactic cluster summaries
- Limitations

Do not include nondeterministic fields unless fixed by the test clock.

## Regression Tests For Known Risks

Add tests for these specific risks:

- Analysis changes when response order changes: document expected behavior for tie cases.
- DBSCAN labels change unexpectedly due to traversal order.
- Empty or null token usage in run logs does not affect analysis.
- Blank responses do not crash tokenizer, BLEU, ROUGE-L, or embedding path.
- Analysis rejects run logs with zero repetitions.
- `--analyze=ALL` processes files in deterministic sorted order.
- Analysis writer never overwrites an existing file.

## Optional Model-Backed Tests

Add a disabled test class:

```text
MultilingualE5EmbeddingServiceSmokeTest
```

Enable with:

```bash
./gradlew test -Pe5Smoke=true
```

or a system property:

```bash
./gradlew test -De5Smoke=true
```

The test should:

- Read model path from an environment variable, for example `LLM_VARIANCE_E5_MODEL_DIR`.
- Fail clearly if the property is enabled but the model path is missing.
- Embed two German paraphrases and one unrelated sentence.
- Assert paraphrases have smaller cosine distance than unrelated texts.

This test should not run in the default suite.

## Acceptance Criteria

The analysis test increment is complete when:

- Every pure algorithm has exact and edge-case tests.
- DBSCAN behavior is covered with at least five synthetic matrices.
- BLEU and ROUGE-L have hand-computable reference tests.
- Full analyzer has at least three scenario tests with fake embeddings.
- At least two golden fixture tests exist.
- Default `gradle test` remains independent of large model files.
- The spec documents all numeric tolerances and tie-breaking behavior.

## Implementation Order

1. Add fixture directories and builders for run logs and fake embeddings.
2. Expand pure function tests.
3. Add DBSCAN scenario tests.
4. Add syntactic metric reference tests.
5. Add analyzer scenario tests.
6. Add golden JSON tests.
7. Add optional disabled multilingual-e5 smoke test.
