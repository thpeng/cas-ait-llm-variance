# YAML Plan CLI Increment

## Goal

Enhance the current configuration approach so each run, or `Versuchsreihe`, is described as one YAML file below `src/main/resources/plans/`.

The application can run one plan, an explicit list of plans, or all bundled plans. When multiple plans are selected, they are executed sequentially in natural order based on the four-digit filename prefix.

## Implemented User Experience

Plan files live on the classpath:

```text
src/main/resources/plans/
  0001-rundreise-schweiz.yml
  0002-hauptstadt-798.yml
```

Filename convention:

```text
<four-digit-number>-<descriptive-name>.yml
```

Examples:

```bash
./gradlew bootRun --args="--plan=0001-rundreise-schweiz"
./gradlew bootRun --args="--plans=0001-rundreise-schweiz,0002-hauptstadt-798"
./gradlew bootRun --args="--plans=ALL"
```

The `.yml` or `.yaml` suffix is optional for selected plans.

## Behavior

- `--plan=<name>` selects one plan.
- `--plans=<name1,name2,...>` selects an explicit list.
- `--plans=ALL` discovers every YAML plan below `classpath:plans/`.
- `--plan` and `--plans` are mutually exclusive.
- Explicit lists and `ALL` are sorted by natural plan order, so `0002-...` runs before `0010-...`.
- Each file contains exactly one `Versuchsreihe`.
- Plans are treated as immutable, except for the existing command-line field overrides that are still supported by the implementation.
- Concrete run values were moved out of `application.yml`; it now only contains application metadata.

## Implementation

Implemented components:

- `PlanLoader`: loads one classpath YAML resource and discovers all plan resources for `ALL`.
- `YamlPlan`: YAML-backed implementation of the existing `Plan` contract.
- `LoadedPlan`: keeps the loaded `Plan` together with its resource name.
- `PlanBatchResolver`: parses `--plan`, `--plans`, and `ALL`.
- `PlanResolver`: resolves a loaded plan plus command-line overrides into a `ResolvedPlan`.
- `ResolvedPlan`: immutable runtime plan used by execution.
- `PlanRunner`: executes one resolved plan and returns the collected answers.
- `LlmClientFactory`: lets `PlanRunner` create real clients in production and fake clients in tests.

Validation covers:

- Missing `--plan` or `--plans`
- Supplying both `--plan` and `--plans`
- Path traversal in plan names
- Missing plan files
- Invalid filename convention
- Unknown manufacturer
- Missing prompt
- `iterations` less than `1`
- Invalid numeric values

## Testing

Added tests for:

- Loading classpath plans by name with and without suffix
- Rejecting unknown plans and path traversal
- Discovering plans in natural order
- Reporting invalid plan filenames during discovery
- Resolving single, multiple, and `ALL` selections
- Sorting explicit plan lists naturally
- Rejecting conflicting or missing selection arguments
- Applying command-line overrides
- Falling back to `manufacturer.defaultModel()` when no model is set
- Rejecting invalid resolved plans
- Running a plan with a fake `LlmClient`
- Wiring the Spring command-line path into `PlanRunner` without real LLM calls

Verification command used:

```bash
gradle --no-daemon test
```

In this workspace the checked-in Gradle wrapper is missing `gradle-wrapper.jar`, so tests were run with the cached Gradle 8.14.3 distribution and `JAVA_HOME=C:\develop\jdk-21.0.1`.

## Remaining Follow-Up

- Decide whether `--plan=ALL` should also be accepted. The current implementation accepts `ALL` only through `--plans=ALL`.
- Decide how the next increment should persist structured results instead of only printing to stdout.
