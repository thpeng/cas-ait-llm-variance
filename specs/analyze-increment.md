# Analyze Increment

## Goal

Add an `analyze` capability that evaluates completed run logs. The analysis combines:

- Semantic variance analysis with multilingual-e5-large embeddings, cosine distance, medoid selection, and DBSCAN clustering.
- Syntactic variance analysis with pairwise ROUGE-L and BLEU with smoothing inside each semantic cluster.

The output is an analysis artifact per run that describes response stability, cluster structure, outliers, and within-cluster syntactic variance. The analysis is relative within a run; it does not judge correctness or factual quality.

## Input

The analyzer reads completed run logs from:

```text
src/main/resources/runs/
```

Each log already contains:

- Plan name
- Manufacturer
- Model
- Model version
- Iterations
- Temperature, top-p, top-k, seed
- Prompt
- Repetition responses
- Start/end timestamps
- Token usage

The analyzer uses the `response` text from each repetition as the primary analysis input.

## Output

Write one analysis file per run to:

```text
src/main/resources/analysis/
```

Filename:

```text
<run-log-file-name-without-extension>-analysis-yyyyMMdd-HHmmss-SSS.json
```

Example:

```text
20260502-104530-123-0001-rundreise-schweiz-analysis-20260502-111500-000.json
```

The analysis file should include:

- Source run log filename
- Analysis timestamp
- Analysis configuration and algorithm parameters
- Semantic medoid
- Pairwise cosine distance summary
- DBSCAN clusters and outliers
- Per-cluster syntactic BLEU and ROUGE-L summaries
- Limitations and warnings, for example truncated embedding input

## Command-Line Experience

Add an analyzer entry path without mixing it into run execution.

Recommended commands:

```bash
./gradlew bootRun --args="--analyze=<run-log-file-name>"
./gradlew bootRun --args="--analyze=ALL"
```

Behavior:

- `--analyze=<file>` analyzes one run log.
- `--analyze=ALL` analyzes every run log under `src/main/resources/runs/`.
- Running and analyzing are mutually exclusive. The application should reject combinations like `--plan=... --analyze=...`.
- Analysis should not call any remote LLM provider.

## Package Structure

Create a new package:

```text
ch.thp.mas.llm.variance.analyze
```

Suggested classes:

```text
AnalyzeCommand
RunLogReader
AnalysisWriter
AnalysisFileNameFactory
Analyzer
AnalysisConfig
AnalysisResult
SemanticAnalyzer
EmbeddingService
EmbeddingModelConfig
CosineDistance
MedoidSelector
DbscanClusterer
SyntacticAnalyzer
RougeLMetric
BleuMetric
MetricSummary
```

Keep package boundaries clear:

- `run` owns execution and run log creation.
- `analyze` owns reading completed logs and writing analysis files.
- `plan` remains focused on plan selection and resolution.

## Semantic Analysis

Implement the algorithm from section 3.4.1:

1. Read all responses from one completed run log.
2. Transform response texts into embeddings with local `multilingual-e5-large`.
3. Compute pairwise cosine distances from the embeddings.
4. Select the medoid: the response with the minimal summed distance to all other responses.
5. Cluster embeddings with DBSCAN.
6. Report statistical summaries, cluster structure, and outliers.

### Embedding Model

Use local multilingual-e5-large. The model choice supports German, French, Italian, and English and provides enough context for roughly 300-400 words.

Recommended implementation path:

- Store model artifacts locally outside Git, for example under `models/multilingual-e5-large/`.
- Use ONNX Runtime Java or DJL with ONNX Runtime for JVM inference.
- Use HuggingFace tokenizers for model-compatible tokenization.

Recommended dependencies to evaluate:

```gradle
implementation("ai.djl:api:<version>")
implementation("ai.djl.onnxruntime:onnxruntime-engine:0.36.0")
implementation("ai.djl.huggingface:tokenizers:0.36.0")
```

Alternative lower-level path:

```gradle
implementation("com.microsoft.onnxruntime:onnxruntime:1.24.3")
```

Recommendation:

Start with DJL plus ONNX Runtime because DJL provides a higher-level model loading and inference abstraction, while still using ONNX Runtime underneath. Fall back to direct ONNX Runtime only if DJL makes the E5 pooling/tokenizer flow awkward.

Implementation note: until the multilingual-e5-large artifacts are available below the `C:\develop` tree, the analyzer uses an `EmbeddingService` interface with a deterministic local hashing implementation. This keeps the full analysis pipeline executable and testable while preserving the replacement point for the real model-backed embedding service. The model location should later be configurable via environment variable.

### E5 Text Formatting

E5-style models expect prefixed text. Use:

```text
passage: <response text>
```

for response embeddings.

Normalize embeddings to unit length before cosine calculations. With normalized vectors, cosine distance is:

```text
1 - dot(a, b)
```

### Truncation

The embedding model has a limited token window. The analyzer should:

- Tokenize before inference.
- Truncate to the configured max tokens.
- Record whether each response was truncated.
- Report total truncated responses in the analysis output.

Default:

```text
maxEmbeddingTokens: 514
```

## DBSCAN Clustering

Use DBSCAN because no predefined cluster count is required.

Recommended dependency:

```gradle
implementation("com.github.haifengl:smile-core:5.2.3")
```

Smile provides a Java DBSCAN implementation that uses radius and `minPts`. If the Smile dependency is considered too large or the license is unsuitable, implement DBSCAN directly over the already computed cosine distance matrix.

Recommended default parameters:

```json
{
  "dbscan": {
    "epsilon": 0.15,
    "minPts": 2,
    "distance": "cosine"
  }
}
```

These parameters must be written into every analysis file. They should be configurable per analysis run because the best epsilon may vary by prompt type and model.

## Medoid

The medoid is the response with the minimal total cosine distance to all other responses in the same run.

Output:

```json
{
  "medoid": {
    "repetitionIndex": 4,
    "totalDistance": 1.234,
    "response": "..."
  }
}
```

The medoid represents the typical answer in the run. It is not a correctness reference.

## Syntactic Analysis

Implement the algorithm from section 3.4.2:

1. Reuse the semantic clusters.
2. For every cluster, compare all response pairs inside the cluster.
3. Compute ROUGE-L and BLEU with smoothing for each pair.
4. Convert similarity scores to distances:

   ```text
   distance = 1.0 - score
   ```

5. Aggregate distances per cluster:

   - Median ROUGE-L distance
   - p90 ROUGE-L distance
   - Median BLEU distance
   - p90 BLEU distance

6. Report aggregations per cluster.

Clusters with fewer than two responses should have empty pairwise metric lists and null aggregate values.

## BLEU Implementation

Recommendation: implement BLEU internally instead of adding an external metric library.

Reasoning:

- BLEU with smoothing is small and deterministic.
- The project only needs pairwise sentence-level BLEU inside clusters.
- Java metric libraries for BLEU/ROUGE are less standard than Python's NLTK, sacreBLEU, and rouge-score ecosystem.
- Internal implementation avoids adding a dependency for a small calculation surface.

Implementation details:

- Tokenize with a deterministic local tokenizer.
- Lowercase by default.
- Split on Unicode letter/number sequences and keep punctuation out of tokens.
- Compute modified n-gram precision for n = 1..4.
- Use brevity penalty.
- Apply smoothing to avoid zero values for short texts.

Recommended smoothing:

```text
precision_n = (matches_n + 1.0) / (candidate_ngrams_n + 1.0)
```

Store the smoothing method in the analysis config.

## ROUGE-L Implementation

Recommendation: implement ROUGE-L internally.

Implementation details:

- Use the same tokenizer as BLEU.
- Compute longest common subsequence length between token sequences.
- Compute precision, recall, and F1.
- Use ROUGE-L F1 as the similarity score.
- Convert to distance with `1.0 - f1`.

This implementation is straightforward, deterministic, and avoids pulling in a dependency just for LCS-based scoring.

## Tokenization For Syntactic Metrics

Create one shared tokenizer for BLEU and ROUGE-L:

```text
ch.thp.mas.llm.variance.analyze.TextTokenizer
```

Default behavior:

- Unicode-aware.
- Lowercase with `Locale.ROOT`.
- Extract words/numbers with a regex such as `[\p{L}\p{N}]+`.
- Preserve token order.

This is not language-perfect, but it is deterministic and adequate for relative within-run variance comparison across German, French, Italian, and English.

## Statistical Summaries

Add reusable summary utilities for numeric lists:

- Count
- Min
- Median
- p90
- Max
- Mean

Use these summaries for:

- Pairwise cosine distances over the whole run
- Per-cluster cosine distances
- Per-cluster BLEU distances
- Per-cluster ROUGE-L distances

Percentile method should be documented in the output config. Recommended first implementation:

```text
nearest-rank percentile
```

## Analysis JSON Shape

Example:

```json
{
  "sourceRun": "20260502-104530-123-0001-rundreise-schweiz.json",
  "analyzedAt": "2026-05-02T11:15:00.000+02:00",
  "config": {
    "embeddingModel": "multilingual-e5-large",
    "embeddingPrefix": "passage:",
    "maxEmbeddingTokens": 514,
    "distance": "cosine",
    "dbscan": {
      "epsilon": 0.15,
      "minPts": 2
    },
    "bleu": {
      "maxN": 4,
      "smoothing": "add-one"
    },
    "rouge": {
      "variant": "ROUGE-L",
      "score": "f1"
    },
    "percentile": "nearest-rank"
  },
  "run": {
    "planName": "0001-rundreise-schweiz",
    "manufacturer": "ANTHROPIC",
    "model": "claude-sonnet-4-5-20250929",
    "modelVersion": null,
    "iterations": 20,
    "temperature": 0.0,
    "topP": null,
    "topK": null,
    "seed": null
  },
  "semantic": {
    "responseCount": 20,
    "truncatedResponses": 0,
    "medoid": {
      "repetitionIndex": 4,
      "totalDistance": 1.234,
      "response": "..."
    },
    "pairwiseCosineDistance": {
      "count": 190,
      "min": 0.01,
      "median": 0.08,
      "p90": 0.22,
      "max": 0.41,
      "mean": 0.10
    },
    "clusters": [
      {
        "clusterId": 0,
        "size": 16,
        "repetitionIndices": [1, 2, 3, 4],
        "medoidRepetitionIndex": 4,
        "pairwiseCosineDistance": {}
      }
    ],
    "outliers": [7, 12]
  },
  "syntactic": {
    "clusters": [
      {
        "clusterId": 0,
        "pairCount": 120,
        "rougeLDistance": {
          "median": 0.18,
          "p90": 0.34
        },
        "bleuDistance": {
          "median": 0.42,
          "p90": 0.67
        }
      }
    ]
  },
  "limitations": [
    "Embedding model choice influences cluster structure.",
    "Metrics describe relative variance, not answer correctness."
  ]
}
```

## Implementation Plan

1. Add `analyze` package and CLI dispatch

   Update `LlmVarianceApplication` so run and analyze modes are mutually exclusive.

2. Add run log reading

   Reuse Jackson to deserialize JSON run logs into the existing `RunLog` records.

3. Add analysis config

   Start with defaults in code. Later increments can move these to YAML.

4. Add embedding service

   Implement local multilingual-e5-large embedding inference through DJL/ONNX Runtime. Keep the interface testable:

   ```java
   List<EmbeddingResult> embed(List<String> texts, AnalysisConfig config)
   ```

5. Add cosine distance and medoid utilities

   Implement and test without model dependencies.

6. Add DBSCAN clustering

   Prefer Smile DBSCAN if license and dependency size are acceptable. Otherwise implement DBSCAN over the cosine distance matrix.

   Implementation note: the first executable implementation uses an internal DBSCAN over the cosine distance matrix, avoiding a new dependency until the embedding model packaging is settled.

7. Add syntactic metrics

   Implement tokenizer, ROUGE-L, BLEU with add-one smoothing, pairwise cluster aggregation, and summary statistics.

8. Add analysis writer

   Write one JSON file per analyzed run under `src/main/resources/analysis/`.

## Testing Plan

1. Unit tests without model dependencies

   - Cosine distance
   - Medoid selection
   - DBSCAN wrapper using fixed vectors
   - Tokenizer
   - ROUGE-L with known examples
   - BLEU smoothing behavior for short texts
   - Median and p90 summaries
   - Pairwise aggregation per cluster

2. Analyzer service tests

   Use a fake `EmbeddingService` with deterministic vectors.

   Verify:

   - Medoid selection
   - Cluster assignment
   - Outlier reporting
   - Syntactic per-cluster summaries
   - Analysis JSON shape

3. File tests

   - Read run logs from `src/main/resources/runs/` or a temporary test directory.
   - Write analysis files to a temporary directory.
   - Prevent overwriting existing analysis files unless explicitly requested.

4. Optional model smoke test

   Add a disabled or profile-gated test that loads the local multilingual-e5-large model and embeds two short texts.

   Keep this out of the default test suite because model files are large and environment-dependent.

## Dependency Recommendation

Add only what is necessary:

```gradle
implementation("ai.djl:api:0.36.0")
implementation("ai.djl.onnxruntime:onnxruntime-engine:0.36.0")
implementation("ai.djl.huggingface:tokenizers:0.36.0")
implementation("com.github.haifengl:smile-core:5.2.3")
```

Do not add a BLEU/ROUGE dependency in the first implementation. Implement those metrics internally.

License decision: GPL 3.0 and Apache 2.0 dependencies are acceptable for this project. Smile can be used later if its API fits cleanly.

## Notes From Library Review

- ONNX Runtime provides Java bindings and Maven artifacts for JVM inference.
- DJL provides a higher-level Java abstraction and an ONNX Runtime engine. Its docs recommend using DJL abstractions instead of binding application code directly to ONNX Runtime internals.
- DJL provides HuggingFace tokenizer support as a separate module.
- Smile provides a Java DBSCAN implementation with `radius` and `minPts`.
- BLEU and ROUGE-L are small enough for deterministic internal implementations in this project.

## Open Questions

1. Is Smile's GPL/LGPL licensing acceptable for this repository, or should DBSCAN be implemented internally? gpl looks fine. 
2. Where should the local multilingual-e5-large model artifacts live on developer machines? below the c:develop tree. should be configurable over env 
3. Should `--analyze=ALL` skip run logs that already have an analysis file, or fail unless overwrite is explicitly requested? Answer: no overwrite; analysis filenames include a timestamp and are written with `CREATE_NEW`.
