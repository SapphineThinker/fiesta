package de.otto.edison.env;

import de.otto.edison.env.ParamStorePropertySourcePostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
@EnableConfigurationProperties(ParamStoreProperties.class)
@ConditionalOnProperty(name = "edison.env.paramstore.enabled", havingValue = "true")
public class ParamStoreConfiguration {

    @Bean
    public ParamStorePropertySourcePostProcessor paramStorePropertySourcePostProcessor(final SsmClient ssmClient) {
        return new ParamStorePropertySourcePostProcessor(ssmClient);
    }
}
