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

import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.ProxyConfigurationData;
import com.appian.connectedsystems.templateframework.sdk.connectiontesting.TestConnectionResult;
import org.junit.Test;
import uk.org.kano.appian.filesystem.FileSystemGetProperties;

import java.util.Locale;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AzureDatalakeConnectedSystemSystemTemplateTest extends TestBase {
    private AzureDatalakeConnectedSystemTemplate template = new AzureDatalakeConnectedSystemTemplate();

    @Test
    public void connectionTestReturnSuccess() {
        TestConnectionResult response = template.testConnection(connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
    }

    @Test
    public void whenBlankProxy_thenSuccess() {
        ExecutionContext ctx = new ExecutionContext() {
            @Override
            public Locale getDesignerLocale() {
                return null;
            }

            @Override
            public Locale getExecutionLocale() {
                return null;
            }

            @Override
            public boolean isDiagnosticsEnabled() {
                return false;
            }

            @Override
            public boolean hasAccessToConnectedSystem() {
                return false;
            }

            @Override
            public ProxyConfigurationData getProxyConfigurationData() {
                return new ProxyConfigurationData() {
                    @Override
                    public boolean isEnabled() {
                        return true;
                    }

                    @Override
                    public String getHost() {
                        return "";
                    }

                    @Override
                    public int getPort() {
                        return 0;
                    }

                    @Override
                    public boolean isAuthRequired() {
                        return false;
                    }

                    @Override
                    public String getUsername() {
                        return null;
                    }

                    @Override
                    public String getPassword() {
                        return null;
                    }

                    @Override
                    public String[] getExcludedHosts() {
                        return new String[0];
                    }

                    @Override
                    public boolean isExcludedHost(String url) {
                        return false;
                    }
                };
            }

            @Override
            public Optional<String> getAccessToken() {
                return Optional.empty();
            }

            @Override
            public int getAttemptNumber() {
                return 0;
            }
        };

        TestConnectionResult response = template.testConnection(connectedSystemConfiguration, ctx);
        assertThat(response.isSuccess(), equalTo(true));
    }
}
