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
