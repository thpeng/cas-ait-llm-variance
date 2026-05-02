package ch.thp.mas.llm.variance.plan;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.LoaderOptions;

@Component
public class PlanLoader {

    private static final String PLAN_ROOT = "plans/";
    private static final Pattern PLAN_FILE_PATTERN = Pattern.compile("^(\\d{4})-.+\\.ya?ml$");
    private static final List<String> SUFFIXES = List.of(".yml", ".yaml");

    private final PathMatchingResourcePatternResolver resourceResolver;

    public PlanLoader() {
        this(new PathMatchingResourcePatternResolver());
    }

    PlanLoader(PathMatchingResourcePatternResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public LoadedPlan load(String name) {
        String normalizedName = normalizePlanName(name);
        for (String candidate : candidateFileNames(normalizedName)) {
            validatePlanFileName(candidate);
            ClassPathResource resource = new ClassPathResource(PLAN_ROOT + candidate);
            if (resource.exists()) {
                return read(resource, candidate);
            }
        }
        throw new PlanException("Plan not found: " + name);
    }

    public List<LoadedPlan> loadAll() {
        return discoverPlanNames().stream()
                .map(this::load)
                .toList();
    }

    public List<String> discoverPlanNames() {
        Map<String, String> planNames = new LinkedHashMap<>();
        for (String suffix : SUFFIXES) {
            for (Resource resource : resourcesForSuffix(suffix)) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                validatePlanFileName(filename);
                String planName = removeYamlSuffix(filename);
                planNames.putIfAbsent(planName, filename);
            }
        }
        return planNames.keySet().stream()
                .sorted(naturalPlanNameComparator())
                .toList();
    }

    public static Comparator<String> naturalPlanNameComparator() {
        return Comparator
                .comparingInt(PlanLoader::planNumber)
                .thenComparing(String::compareTo);
    }

    static String removeYamlSuffix(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".yaml")) {
            return filename.substring(0, filename.length() - ".yaml".length());
        }
        if (lower.endsWith(".yml")) {
            return filename.substring(0, filename.length() - ".yml".length());
        }
        return filename;
    }

    private LoadedPlan read(Resource resource, String filename) {
        try (InputStream inputStream = resource.getInputStream()) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(YamlPlan.class, loaderOptions));
            YamlPlan plan = yaml.load(inputStream);
            if (plan == null) {
                throw new PlanException("Plan file is empty: " + filename);
            }
            return new LoadedPlan(removeYamlSuffix(filename), filename, plan);
        } catch (YAMLException e) {
            throw new PlanException("Invalid YAML in plan file: " + filename, e);
        } catch (IOException e) {
            throw new PlanException("Could not read plan file: " + filename, e);
        }
    }

    private List<Resource> resourcesForSuffix(String suffix) {
        try {
            return List.of(resourceResolver.getResources("classpath*:" + PLAN_ROOT + "*" + suffix));
        } catch (IOException e) {
            throw new PlanException("Could not discover plans under classpath:" + PLAN_ROOT, e);
        }
    }

    private static List<String> candidateFileNames(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
            return List.of(name);
        }

        List<String> candidates = new ArrayList<>();
        for (String suffix : SUFFIXES) {
            candidates.add(name + suffix);
        }
        return candidates;
    }

    private static String normalizePlanName(String name) {
        if (name == null || name.isBlank()) {
            throw new PlanException("Plan name must not be blank.");
        }

        String trimmed = name.trim();
        if (trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
            throw new PlanException("Invalid plan name: " + name);
        }
        return trimmed;
    }

    private static void validatePlanFileName(String filename) {
        if (!PLAN_FILE_PATTERN.matcher(filename).matches()) {
            throw new PlanException("Plan file name must start with a four digit number: " + filename);
        }
    }

    private static int planNumber(String name) {
        String filename = candidateFileNames(normalizePlanName(name)).getFirst();
        Matcher matcher = PLAN_FILE_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            throw new PlanException("Plan file name must start with a four digit number: " + name);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
