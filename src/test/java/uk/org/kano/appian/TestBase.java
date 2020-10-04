package uk.org.kano.appian;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.configuration.ConfigurationDescriptor;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

public class TestBase {
    protected static Properties credentials;
    protected static SimpleConfiguration connectedSystemConfiguration;
    private static ConfigurationDescriptor connectedSystemConfigurationDescriptor;

    @BeforeClass
    public static void loadCredentials() throws Exception {
        URL credentialsLocation = TestBase.class.getResource("/credentials.properties");
        if (null == credentialsLocation) throw new IOException("Credentials file not found");
        credentials = new Properties();

        InputStream in = credentialsLocation.openStream();
        credentials.load(in);
        in.close();

        // Get the configuration to help test classes.
        connectedSystemConfiguration = getConnectedSystemConfiguration();
        setValues(connectedSystemConfiguration, credentials);
    }

    /**
     * Get the configuration to access the datalake.
     * @return The configuration for the connected system
     */
    private static SimpleConfiguration getConnectedSystemConfiguration() {
        AzureDatalakeConnectedSystemTemplate ct = new AzureDatalakeConnectedSystemTemplate();
        ConfigurationDescriptor desc = ConfigurationDescriptor.builder().build();

        desc = ct.getConfigurationDescriptor(desc, null, null);
        connectedSystemConfigurationDescriptor = desc;
        return SimpleConfiguration.from(desc, null, null);
    }

    /**
     * Create an empty configuration object
     * @return The empty configuration.
     */
    protected SimpleConfiguration getIntegrationConfiguration(SimpleIntegrationTemplate integrationTemplate) {
        ConfigurationDescriptor desc = ConfigurationDescriptor.builder().build();
        desc = integrationTemplate.getConfigurationDescriptor(desc, connectedSystemConfigurationDescriptor, null, null);
        return SimpleConfiguration.from(desc, null, null);
    }

    /**
     * Set the properties from a map
     * @param configuration
     * @param values
     */
    protected static void setValues(SimpleConfiguration configuration, Map<?, ?> values) {
        values.forEach( (key, value) -> configuration.setValue(key.toString(), value.toString()));
    }
}
