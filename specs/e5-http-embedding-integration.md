# E5 HTTP Embedding Integration

## Goal

Integrate the WSL-hosted Python API for `intfloat/multilingual-e5-large` into the Java analysis pipeline.

The Python service lives in:

```text
src/main/python/server.py
```

It is expected to run inside WSL and expose an HTTP API on port `8000`.

The Java analyzer should use this service for semantic embeddings instead of the current deterministic `local-hashing-v1` development fallback.

## Python API

Current endpoints:

```text
POST /load
POST /embed
POST /unload
GET  /status
```

### `POST /load`

Loads the model into GPU memory.

Response examples:

```json
{
  "status": "loaded",
  "model": "intfloat/multilingual-e5-large",
  "device": "cuda"
}
```

```json
{
  "status": "already_loaded"
}
```

### `POST /embed`

Embeds a list of texts. The model must already be loaded.

Request:

```json
{
  "texts": [
    "passage: Antwort 1",
    "passage: Antwort 2"
  ]
}
```

Response:

```json
{
  "dim": 1024,
  "count": 2,
  "embeddings": [
    [0.01, 0.02],
    [0.03, 0.04]
  ]
}
```

If the model is not loaded, the server returns HTTP `409`.

### `POST /unload`

Unloads the model and clears GPU memory.

Response examples:

```json
{
  "status": "unloaded"
}
```

```json
{
  "status": "already_unloaded"
}
```

### `GET /status`

Returns load and CUDA/GPU status. This is useful for diagnostics and preflight checks.

## Java Integration

Add a production embedding implementation:

```text
ch.thp.mas.llm.variance.analyze.E5HttpEmbeddingService
```

It should implement the existing interface:

```java
public interface EmbeddingService {
    List<EmbeddingResult> embed(List<String> texts, AnalysisConfig config);
}
```

Responsibilities:

- Call `POST /load` before embedding.
- Call `POST /embed` with all response texts.
- Call `POST /unload` after embedding.
- Always attempt `POST /unload` in a `finally` block after a successful or failed load attempt.
- Convert returned vectors into `EmbeddingResult`.
- Preserve the current `EmbeddingService` seam so tests can still use fake embeddings.

The lifecycle should be:

```text
analyze run
  -> load model
  -> embed all responses
  -> unload model
  -> continue cosine distance, medoid, DBSCAN, syntactic analysis
```

## Text Prefixing

The Python API currently embeds exactly the text it receives. Therefore Java should apply the E5 prefix before calling `/embed`.

Use:

```text
passage: <response>
```

This should come from `AnalysisConfig.embeddingPrefix()`.

Implementation detail:

```java
String textForEmbedding = config.embeddingPrefix() + " " + response;
```

If the prefix is blank, no leading extra whitespace should be added.

## Configuration

Add configuration for the embedding HTTP service.

Recommended environment variables:

```text
LLM_VARIANCE_EMBEDDING_BASE_URL=http://localhost:8000
LLM_VARIANCE_EMBEDDING_PROVIDER=e5-http
```

Default:

```text
http://localhost:8000
```

The provider selection should allow:

- `e5-http`: use the WSL Python API
- `local-hashing`: use the deterministic fallback for tests/dev only

The default for real analysis should become `e5-http`. If the HTTP service is not reachable, analysis should fail clearly rather than silently falling back to local hashing.

## HTTP Client

Use Spring's existing stack if available. Since this project currently depends on `spring-boot-starter`, use one of:

- `RestClient` if available through Spring Framework 6
- `WebClient` if `spring-boot-starter-webflux` is added
- Java `HttpClient` from the JDK to avoid extra dependencies

Recommendation:

Use Java 21 `java.net.http.HttpClient` for this integration. It is sufficient for three simple JSON endpoints and avoids adding a web client dependency.

Jackson can serialize/deserialize request and response records.

Suggested records:

```java
record EmbedRequest(List<String> texts) {}
record EmbedResponse(int dim, int count, List<List<Double>> embeddings) {}
record LoadResponse(String status, String model, String device) {}
record UnloadResponse(String status) {}
```

## Error Handling

Fail analysis with `AnalysisException` when:

- `/load` returns non-2xx
- `/embed` returns non-2xx
- `/embed` returns `count` different from requested text count
- `/embed` returns empty embeddings
- Any returned vector has a different dimension than `dim`
- `/unload` fails after a successful load
- The HTTP service is unreachable

If `/load` fails, do not call `/embed`. Calling `/unload` after failed load is optional; the first implementation may skip it.

If `/embed` fails after `/load` succeeds, call `/unload` in `finally` and then fail analysis.

If `/unload` fails after successful embedding, fail the analysis and do not write an analysis file. This keeps GPU cleanup issues visible.

## Analysis Output

Update `AnalysisConfig.defaults()` for real analysis:

```text
embeddingModel: intfloat/multilingual-e5-large
embeddingPrefix: passage:
maxEmbeddingTokens: 514
```

The analysis JSON should make the embedding provider visible. Add one of:

```json
{
  "embeddingProvider": "e5-http",
  "embeddingBaseUrl": "http://localhost:8000"
}
```

or include those fields in the existing config block.

Do not store raw embeddings in the analysis output. They are large and unnecessary for review. Store only derived distances and cluster summaries.

## Token Limit And Truncation

The Python server currently does not expose tokenization or truncation information.

Options:

1. Let the Python service handle model-side truncation and return no truncation metadata.
2. Extend the Python API later to return truncation flags.

First implementation:

- Keep `truncated=false` for all `EmbeddingResult` values from `E5HttpEmbeddingService`.
- Document in analysis limitations that truncation detection is unavailable for the HTTP embedding service.

Later enhancement:

Return:

```json
{
  "truncated": [false, true]
}
```

from `/embed`.

## Spring Wiring

Create a small factory/configuration:

```text
EmbeddingServiceConfig
```

Behavior:

- If `LLM_VARIANCE_EMBEDDING_PROVIDER=e5-http`, create `E5HttpEmbeddingService`.
- If `LLM_VARIANCE_EMBEDDING_PROVIDER=local-hashing`, create `LocalHashingEmbeddingService`.
- If unset, default to `e5-http`.

To avoid two `EmbeddingService` beans, remove `@Component` from `LocalHashingEmbeddingService` and instantiate it explicitly in the config.

## Testing Plan

### Unit Tests

Test request/response validation without starting Python:

- Prefix is applied correctly.
- Blank prefix does not add leading whitespace.
- `/embed` count mismatch fails.
- Empty embedding list fails.
- Vector dimension mismatch fails.
- Non-2xx `/load` fails.
- Non-2xx `/embed` fails.
- Non-2xx `/unload` fails after load.

### Lifecycle Tests

Use a fake HTTP server, for example JDK `HttpServer`.

Verify:

- Normal path calls `/load`, `/embed`, `/unload` in that order.
- If `/embed` fails, `/unload` is still called.
- If `/load` fails, `/embed` is not called.
- Returned vectors are normalized by the Python model and accepted as-is.

### Analyzer Integration Test

Use the fake HTTP server with deterministic embeddings:

- Run analyzer on a small run log.
- Verify semantic clustering uses returned vectors.
- Verify analysis JSON config reports `intfloat/multilingual-e5-large` and provider `e5-http`.

### Manual Smoke Test

Start the Python server in WSL:

```bash
cd /mnt/c/develop/workspace/mas-llm-variance
uvicorn src.main.python.server:app --host 0.0.0.0 --port 8000
```

From Windows/Java side:

```powershell
$env:LLM_VARIANCE_EMBEDDING_PROVIDER="e5-http"
$env:LLM_VARIANCE_EMBEDDING_BASE_URL="http://localhost:8000"
./gradlew bootRun --args="--analyze=<run-log-file-name>"
```

Check:

- `/load` loads the model.
- `/embed` returns vectors.
- `/unload` frees GPU memory.
- Analysis file is written under `src/main/resources/analysis/`.

## Open Questions

1. Should Java start the WSL Python server automatically, or should the user start it manually? manuall or with wsl startup hooks
2. Should `/unload` happen after each analyzed run, or only once after `--analyze=ALL` completes? after each run
3. Should the Python API expose truncation flags and token counts for embedding input? no, currently not needed
4. Should the Java side call `/status` before `/load` for better diagnostics? call status first. 
