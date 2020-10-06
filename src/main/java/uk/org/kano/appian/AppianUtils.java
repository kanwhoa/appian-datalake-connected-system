package uk.org.kano.appian;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.configuration.ConfigurationDescriptor;

/**
 * Utilities for working with Appian
 */
public class AppianUtils {

    /**
     * Create an empty configuration object
     * @return The empty configuration.
     */
    public static SimpleConfiguration getIntegrationConfiguration(SimpleIntegrationTemplate integrationTemplate, SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {
        ConfigurationDescriptor desc = ConfigurationDescriptor.builder().build();
        desc = integrationTemplate.getConfigurationDescriptor(desc, connectedSystemConfiguration.toConfiguration(), null, executionContext);
        return SimpleConfiguration.from(desc, null, executionContext);
    }
}
