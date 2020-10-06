/*
 * Copyright 2020 Tim Hurman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
        credentials.forEach((key, value) -> connectedSystemConfiguration.setValue(key.toString(), value));
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
    protected static void setValues(SimpleConfiguration configuration, Map<String, ?> values) {
        values.forEach(configuration::setValue);
    }
}
