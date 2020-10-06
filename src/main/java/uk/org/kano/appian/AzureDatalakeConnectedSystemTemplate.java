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

import com.appian.connectedsystems.simplified.sdk.SimpleConnectedSystemTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.simplified.sdk.connectiontesting.SimpleTestableConnectedSystemTemplate;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.ConfigurationDescriptor;
import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;
import uk.org.kano.appian.filesystem.GetProperties;

import java.io.IOException;
import java.net.URI;

@TemplateId(name="AzureDatalakeConnectedSystemTemplate")
public class AzureDatalakeConnectedSystemTemplate extends SimpleTestableConnectedSystemTemplate {
    static String CS_ADLS_G2_ACCOUNT_NAME = "accountName";
    static String CS_ADLS_G2_ACCOUNT_KEY = "accountKey";
    static String CS_ADLS_G2_FILESYSTEM = "fileSystem";
    static String CS_ADLS_G2_DOMAINNAME = ".dfs.core.windows.net";

    /**
     * Get the configuration
     * @param configuration
     * @param executionContext
     * @return
     */
    @Override
    protected SimpleConfiguration getConfiguration(SimpleConfiguration configuration, ExecutionContext executionContext) {
        return configuration.setProperties(
                textProperty(CS_ADLS_G2_ACCOUNT_NAME)
                        .label("ADLS Gen 2 Account Name")
                        .description("The Account Name of the ADLS resource eg: https://accountname.blob.core.windows.net/")
                        .isRequired(true)
                        .isImportCustomizable(true)
                        .build(),
                encryptedTextProperty(CS_ADLS_G2_ACCOUNT_KEY)
                        .label("ADLS Gen 2 Account Key")
                        .description("The Account Key of the ADLS resource (base 64 encoded value)")
                        .isRequired(true)
                        .isImportCustomizable(true)
                        .build(),
                textProperty(CS_ADLS_G2_FILESYSTEM)
                        .label("Password")
                        .description("The File System of the ADLS resource eg: https://accountname.blob.core.windows.net/filesystem")
                        .isRequired(true)
                        .isImportCustomizable(true)
                        .build()
        );
    }

    /**
     * Test a connection to the connected system by logging in and getting the descriptor resources
     * @param configuration
     * @param executionContext
     * @return
     */
    @Override
    protected TestConnectionResult testConnection(SimpleConfiguration configuration, ExecutionContext executionContext) {
        GetProperties getProperties = new GetProperties();
        IntegrationResponse response = getProperties.execute(configuration.toConfiguration(), configuration.toConfiguration(), executionContext);
        if (response.isSuccess()) {
            return TestConnectionResult.success();
        } else {
            return TestConnectionResult.error(response.getError().getDetail());
        }
    }
}
