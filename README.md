# LLM Variance

`mas-llm-variance` is a small Java/Spring Boot research tool for running controlled LLM experiment series and analyzing how much the generated answers vary.

The project is built around one core question:

> If the same prompt is executed repeatedly under controlled model parameters, how stable are the answers?

## What It Does

The application supports three main workflows:

1. Define experiment plans as YAML files.
2. Execute one or more plans and write run logs.
3. Analyze completed run logs for semantic and syntactic variance.

Plans live in:

```text
src/main/resources/plans/
```

Run logs are written to:

```text
src/main/resources/runs/
```

Analysis results are written to:

```text
src/main/resources/analysis/
```

Run and analysis artifacts are intended to be committed manually to GitHub. The larger research workflow assumes GitHub rulesets, signed commits, linear history, deletion protection, and manual Git tags for experiment executions.

## Project Structure

```text
src/main/java/ch/thp/mas/llm/variance/
  LlmVarianceApplication.java
  client/
  plan/
  run/
  analyze/
```

### `client`

Provider abstraction for LLM calls.

Implemented providers:

- OpenAI
- Anthropic
- LM Studio through an Anthropic-compatible local endpoint

The `LlmClient` abstraction returns an `LlmResponse`, including:

- Full generated text
- Token usage where the provider exposes it

### `plan`

Plan loading and resolution.

Plans are YAML files named with a four-digit prefix:

```text
0001-rundreise-schweiz.yml
0002-hauptstadt-798.yml
```

The prefix gives plans a natural execution order. The CLI supports:

```bash
./gradlew bootRun --args="--plan=0001-rundreise-schweiz"
./gradlew bootRun --args="--plans=0001-rundreise-schweiz,0002-hauptstadt-798"
./gradlew bootRun --args="--plans=ALL"
```

A plan defines values such as:

- Manufacturer
- Model
- Prompt
- Iterations
- Temperature
- Top-p
- Top-k
- Seed

### `run`

Execution and run logging.

For each resolved plan, the runner:

1. Creates the provider client.
2. Executes the prompt for the configured number of iterations.
3. Captures start/end timestamps for every repetition.
4. Captures the full answer and token usage.
5. Writes one JSON run log if and only if the full plan succeeds.

Run log files are timestamped:

```text
yyyyMMdd-HHmmss-SSS-<plan-name>.json
```

The run logger is intentionally all-or-nothing. If one repetition fails, no partial log is written.

### `analyze`

Analysis of completed run logs.

The analyzer reads JSON run logs and produces JSON analysis files. It is deliberately separated from execution so experiments can be run first and analyzed later.

CLI:

```bash
./gradlew bootRun --args="--analyze=<run-log-file-name>"
./gradlew bootRun --args="--analyze=ALL"
```

Run mode and analyze mode are mutually exclusive.

## Main Analysis Algorithm

The analysis has two layers: semantic analysis and syntactic analysis.

### 1. Semantic Analysis

The semantic analysis follows this pipeline:

1. Read all responses from a run log.
2. Transform each response into an embedding.
3. Compute pairwise cosine distances between embeddings.
4. Select the medoid: the response with the lowest total distance to all other responses.
5. Cluster responses with DBSCAN.
6. Report cluster structure, outliers, medoid, and distance summaries.

The medoid is the typical answer in the run. It is not a correctness reference.

DBSCAN is used because the number of answer clusters is not known in advance. This is important for LLM evaluation: a stable plan should produce few semantic clusters, while a highly variable plan may produce many clusters or outliers.

Current note: the executable implementation uses a deterministic local hashing embedding service for development and testability. This is not a real semantic model. Before thesis-grade experiment evaluation, this must be replaced by a local multilingual embedding model such as `multilingual-e5-large`.

### 2. Syntactic Analysis

After semantic clustering, syntactic variance is measured inside each semantic cluster.

For every pair of responses in a cluster:

- Compute ROUGE-L F1 similarity.
- Compute sentence-level BLEU with Chen & Cherry method-1 smoothing.
- Convert similarities to distances with `1.0 - score`.

Then aggregate per cluster:

- Median ROUGE-L distance
- p90 ROUGE-L distance
- Median BLEU distance
- p90 BLEU distance

This pairwise approach avoids the need for a reference answer. It supports relative variance comparison inside one run, but it does not measure factual correctness or human preference.

## Configuration And Environment

Provider environment variables:

```text
OPENAI_API_KEY
ANTHROPIC_API_KEY
LMSTUDIO_BASE_URL
```

`LMSTUDIO_BASE_URL` is optional and defaults to a local endpoint if not set.

## Testing

Run the test suite with:

```bash
./gradlew test
```


## Specs

Design and increment specs live in:

```text
specs/
```
