package ch.thp.cas.llm.variance.plan;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PlanProperties.class)
public class PlanConfig {

    @Bean
    public Plan plan(PlanProperties properties) {
        return properties;
    }
}
