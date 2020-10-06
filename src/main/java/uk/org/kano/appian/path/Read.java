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

package uk.org.kano.appian.path;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.log4j.Logger;
import uk.org.kano.appian.BasicResponseHandler;
import uk.org.kano.appian.Constants;
import uk.org.kano.appian.HttpUtils;
import uk.org.kano.appian.LogUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Get the properties of a path
 */
@TemplateId(name="PathRead")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class Read extends SimpleIntegrationTemplate {
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    protected SimpleConfiguration getConfiguration(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, PropertyPath updatedProperty, ExecutionContext executionContext) {
        return integrationConfiguration.setProperties(
                textProperty(Constants.SC_ATTR_PATH)
                        .label("Path")
                        .description("The path to list the contents of.")
                        .isRequired(true)
                        .isExpressionable(true)
                        .build(),
                booleanProperty(Constants.SC_ATTR_BASE64_BODY)
                        .label("Base64 body")
                        .description("Return the body as a base64 encoded value.")
                        .isRequired(false)
                        .isExpressionable(true)
                        .build()
        );
    }

    @Override
    protected IntegrationResponse execute(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {
        long startTime, endTime;
        CloseableHttpClient client = HttpUtils.getHttpClient(executionContext);

        URI resourceUri = HttpUtils.getBaseUri(connectedSystemConfiguration);
        if (null == resourceUri) {
            return LogUtil.createError("Invalid base URI", "The base URI is invalid");
        }

        URIBuilder uriBuilder = new URIBuilder(resourceUri);
        String path = integrationConfiguration.getValue(Constants.SC_ATTR_PATH);
        if (null == path || (path.startsWith("/") && path.length() < 2) || (!path.startsWith("/") && path.length() < 1)) {
            return LogUtil.createError("Invalid path", "Invalid path specified");
        }
        if(!path.startsWith("/")) path = "/" + path;

        // Create the URI
        try {
            resourceUri = uriBuilder
                    .setPath(uriBuilder.getPath() + path)
                    .build();
        } catch (URISyntaxException e) {
            return LogUtil.createError("Invalid URI", e.getMessage());
        }

        // Do the request
        HttpGet request = new HttpGet(resourceUri);
        IntegrationResponse executeResponse = null;
        startTime = System.currentTimeMillis();

        try {
            BasicResponseHandler brh = new BasicResponseHandler();
            brh.setEncodeBodyAsBase64(integrationConfiguration.<Boolean>getValue(Constants.SC_ATTR_BASE64_BODY));
            executeResponse = client.execute(request, brh);
        } catch (IOException e) {
            executeResponse = LogUtil.createError("Unable to execute request to " + resourceUri.toString(), e.getMessage());
            logger.error(executeResponse.getError().getDetail());
        } finally {
            try {
                client.close();
            } catch (IOException ignored) {}
        }
        endTime = System.currentTimeMillis();

        // Record some diagnostics
        Map<String, Object> requestDiagnostic = LogUtil.getIntegrationDataMap("request", request.toString(), "operation", this.getClass().getSimpleName());
        IntegrationDesignerDiagnostic integrationDesignerDiagnostic = IntegrationDesignerDiagnostic.builder()
                .addRequestDiagnostic(requestDiagnostic)
                .addExecutionTimeDiagnostic(endTime - startTime)
                .build();

        LogUtil.mergeDiagnostic(executeResponse.getIntegrationDesignerDiagnostic(), integrationDesignerDiagnostic);
        return executeResponse;
    }
}
