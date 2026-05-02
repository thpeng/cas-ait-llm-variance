# Run Logging Increment

## Goal

Refactor plan execution into a separate `run` package and add durable JSON logging for every completed plan run.

Every execution of a plan creates one log file under `src/main/resources/runs/`. If a repetition fails, the run aborts and no partial log file is written.

## MAS Requirement

Each plan execution must log:

- Hersteller
- Modell
- Modellversion
- Anzahl von Wiederholungen
- Konfigurationswerte wie Temperature, Top-p, Top-k, Seed
- Prompt

For every individual repetition, the log must additionally record:

- Zeitstempel Beginn und Ende, client-side completion of the request
- Vollständige Antwort
- Anzahl verbrauchte Tokens

The log files are manually committed to GitHub after execution. Git tags remain manual until `llm-variance` is mature enough to execute the tests automatically.

## Implemented Package Structure

Plan definition and resolution stay in:

```text
ch.thp.mas.llm.variance.plan
```

Execution and logging move to:

```text
ch.thp.mas.llm.variance.run
  PlanRunner
  RunLogWriter
  RunLog
  RunLogEntry
  RunClock
  RunFileNameFactory
```

The application entrypoint depends on `PlanBatchResolver` from `plan` and `PlanRunner` from `run`.

## Log Location

Logs are written to:

```text
src/main/resources/runs/
```

The directory is created automatically if it does not exist.

## Log File Naming

Each log file starts with date and timestamp, followed by the plan name:

```text
yyyyMMdd-HHmmss-SSS-<plan-name>.json
```

Example:

```text
20260502-104530-123-0001-rundreise-schweiz.json
```

If multiple plans are executed through `--plans` or `--plans=ALL`, each plan execution gets its own log file.

## Log Format

Use structured JSON. `modelVersion` is `null` if not explicitly provided. Token fields that cannot be provided by a client are present with `null` values.

```json
{
  "planName": "0001-rundreise-schweiz",
  "startedAt": "2026-05-02T10:45:30.123+02:00",
  "endedAt": "2026-05-02T10:46:12.456+02:00",
  "manufacturer": "ANTHROPIC",
  "model": "claude-sonnet-4-5-20250929",
  "modelVersion": null,
  "iterations": 20,
  "config": {
    "temperature": 0.0,
    "topP": null,
    "topK": null,
    "seed": null
  },
  "prompt": "gib mir eine Rundreise durch die Schweiz mit 5 Zielen an",
  "repetitions": [
    {
      "index": 1,
      "startedAt": "2026-05-02T10:45:30.130+02:00",
      "endedAt": "2026-05-02T10:45:32.931+02:00",
      "response": "Vollständige Antwort...",
      "tokenUsage": {
        "inputTokens": null,
        "outputTokens": null,
        "totalTokens": null
      }
    }
  ]
}
```

## Implementation Notes

- `PlanRunner` collects all repetitions in memory and writes the log only after the full plan succeeds.
- `RunLogWriter` writes UTF-8 JSON with `CREATE_NEW`, so existing logs are never overwritten.
- `RunClock` is injectable for deterministic tests.
- `LlmClient.call(...)` returns `LlmResponse`, containing response text and `TokenUsage`.
- `seed` is part of the plan and logged request config. Provider support may differ; if unsupported, clients leave provider behavior unchanged.
- Git tagging is intentionally not automated in this increment.

## Testing

Covered by tests for:

- Timestamped JSON filename generation and filename sanitization
- JSON log writing, UTF-8 content, and overwrite protection
- Runner logging of plan-level data, repetition timestamps, full responses, token usage, and seed
- All-or-nothing behavior when a client call fails
- Spring command-line wiring into the new `run.PlanRunner`

Verification command:

```bash
gradle --no-daemon test
```
