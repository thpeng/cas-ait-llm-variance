# YAML Plan CLI Increment

## Goal

Enhance the current configuration approach so each run, or `Versuchsreihe`, is described as a YAML file and selected from the command line by name.

The application should be runnable with a plan name, load the matching YAML plan file, execute the configured LLM calls, and keep the existing per-field command-line override behavior where it still makes sense.

## Current State

- Runtime configuration is currently bound from `src/main/resources/application.yml` into `PlanProperties`.
- `LlmVarianceApplication` receives a single `Plan` bean and manually applies command-line overrides for `manufacturer`, `model`, `prompt`, `temperature`, `topP`, `topK`, and `iterations`.
- The selected `Manufacturer` creates the concrete `LlmClient`.
- The application loops over `iterations`, calls the client with the configured prompt, and prints all answers.
- There are no tests in the repository yet.

## Proposed User Experience

Plan files are stored as YAML resources in the application resources folder:

```text
src/main/resources/plans/
  rundreise-schweiz.yml
  hauptstadt-798.yml
```

This keeps bundled run definitions on the classpath, so they are available both during local development and from the packaged jar.

The application is started with the plan name:

```bash
./gradlew bootRun --args="--plan=rundreise-schweiz"
```

The application resolves this to:

```text
classpath:plans/rundreise-schweiz.yml
```

The `.yml` suffix should be optional at the command line, so these are equivalent:

```bash
./gradlew bootRun --args="--plan=rundreise-schweiz"
./gradlew bootRun --args="--plan=rundreise-schweiz.yml"
```

An example plan file:

```yaml
manufacturer: ANTHROPIC
model: claude-sonnet-4-5-20250929
prompt: "gib mir eine Rundreise durch die Schweiz mit 5 Zielen an"
iterations: 20
temperature: 0.0
topP:
topK:
```

## Implementation Plan

1. Introduce classpath-based plan loading

   Add a `PlanLoader` component responsible for resolving a requested plan name to a YAML resource and binding it to a `Plan` implementation.

   The loader should:

   - Look in `classpath:plans/` by default.
   - Accept plan names with or without `.yml` or `.yaml`.
   - Reject path traversal such as `../secret`.
   - Return clear errors when the plan is missing, unreadable, or invalid.
   - Be designed so an external filesystem location can be added later without changing the CLI contract.

2. Keep `Plan` as the domain contract

   Reuse the existing `Plan` interface as the runtime shape of a run.

   Replace or complement `PlanProperties` with a plain YAML-backed implementation, for example `YamlPlan`, using the same fields:

   - `manufacturer`
   - `model`
   - `prompt`
   - `temperature`
   - `topP`
   - `topK`
   - `iterations`

3. Separate default app configuration from run plans

   Keep `application.yml` for application-level settings only. No concrete run should remain in `application.yml`.

   If an external plan location becomes necessary later, add an application-level setting such as:

   ```yaml
   plans:
     location: classpath:plans/
   ```

   Move concrete run values out of `application.yml` into YAML files under `src/main/resources/plans/`.

4. Update command-line argument handling

   Reserve `--plan=<name>` for selecting the YAML file.

   Preserve useful field overrides, so a run can still be tweaked without editing the YAML file:

   ```bash
   ./gradlew bootRun --args="--plan=rundreise-schweiz --iterations=3 --temperature=0.7"
   ```

   The resolution order should be:

   1. YAML plan file value
   2. Command-line field override
   3. Code default where one already exists, such as `manufacturer.defaultModel()`

   For clarity in implementation, create a small `ResolvedPlan` or `PlanResolver` that merges the YAML plan with command-line overrides before execution starts.

5. Make execution easier to test

   Extract the loop that executes a plan into a service, for example `PlanRunner`.

   `LlmVarianceApplication` should become thin:

   - Read `--plan`
   - Load and resolve the plan
   - Pass it to `PlanRunner`

   This allows most behavior to be tested without starting the full Spring application or calling real LLM APIs.

6. Add example plan files

   Add at least one checked-in example under `src/main/resources/plans/`, based on the current `application.yml` values:

   ```text
   src/main/resources/plans/rundreise-schweiz.yml
   ```

   Optionally add a second small deterministic example that is useful for local manual testing.

7. Improve command-line failure messages

   Add clear validation and messages for:

   - Missing `--plan`
   - Unknown manufacturer
   - Missing plan file
   - Missing prompt
   - `iterations` less than `1`
   - Invalid numeric values

## Testing Plan

1. Unit-test plan file loading

   Add tests for `PlanLoader` using test resources:

   - Loads a valid `.yml` file by name without suffix.
   - Loads a valid `.yaml` file by name with suffix.
   - Rejects unknown plan names.
   - Rejects path traversal.
   - Reports invalid YAML clearly.

2. Unit-test command-line override resolution

   Add tests for the resolver that combines YAML values and command-line options:

   - Uses YAML values when no overrides are present.
   - Overrides `iterations`, `temperature`, `topP`, `topK`, `manufacturer`, `model`, and `prompt`.
   - Falls back to `manufacturer.defaultModel()` when the model is missing.
   - Rejects invalid values.

3. Unit-test plan execution without real API calls

   Add tests for `PlanRunner` with a fake `LlmClient`:

   - Calls the fake client exactly `iterations` times.
   - Passes the expected prompt and `LlmRequestConfig`.
   - Collects or prints results in the expected order.

4. Add a narrow Spring integration test

   Add one integration test that starts the application context with a test resource plan and verifies that the command-line entry path wires together.

   This should still avoid real OpenAI, Anthropic, or LM Studio calls by injecting a fake client factory or replacing the execution service in the test context.

5. Manual smoke test

   Run:

   ```bash
   ./gradlew test
   ./gradlew bootRun --args="--plan=rundreise-schweiz --iterations=1"
   ```

   The second command requires the appropriate provider environment variable when using a real remote provider:

   - `OPENAI_API_KEY` for OpenAI
   - `ANTHROPIC_API_KEY` for Anthropic
   - `LMSTUDIO_BASE_URL` optionally for LM Studio

## Open Questions

1. Should an external filesystem plan location be supported in this increment, or is `src/main/resources/plans/` enough for now? Answer: it is enough. 
2. Should a plan file contain exactly one `Versuchsreihe`, or should one YAML file eventually support multiple named runs? Answer: only one
3. Should results continue printing to stdout only, or should each run also write structured output to a results file? Answer: will be enhanced in the next spec. 
4. Should command-line overrides remain for every field, or should plans be treated as immutable once selected? Answer: immutable
