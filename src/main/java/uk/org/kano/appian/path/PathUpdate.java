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
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.kano.appian.BasicResponseHandler;
import uk.org.kano.appian.Constants;
import uk.org.kano.appian.HttpUtils;
import uk.org.kano.appian.LogUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Update the contents of a file
 */
@TemplateId(name="PathUpdate")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.WRITE)
public class PathUpdate extends SimpleIntegrationTemplate {
    private static final Logger logger = LoggerFactory.getLogger(PathUpdate.class);

    @Override
    protected SimpleConfiguration getConfiguration(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, PropertyPath updatedProperty, ExecutionContext executionContext) {
        return integrationConfiguration.setProperties(
                textProperty(Constants.SC_ATTR_PATH)
                        .label("Path to update")
                        .description("The path to be created.")
                        .isRequired(true)
                        .isExpressionable(true)
                        .build(),
                textProperty(Constants.SC_ATTR_CONTENT)
                        .label("File content")
                        .description("The content to put in the file.")
                        .isRequired(true)
                        .isExpressionable(true)
                        .build(),
                booleanProperty(Constants.SC_ATTR_OVERWRITE)
                        .label("Overwrite")
                        .description("Overwrite the file (true), or append (false - default).")
                        .isRequired(false)
                        .isExpressionable(true)
                        .build()
        );
    }

    @Override
    protected IntegrationResponse execute(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {
        IntegrationResponse executeResponse = null;
        long startTime, endTime;
        CloseableHttpClient client = HttpUtils.getHttpClient(executionContext);

        URI resourceUri = HttpUtils.getBaseUri(connectedSystemConfiguration);
        if (null == resourceUri) {
            return LogUtil.createError("Invalid base URI", "The base URI is invalid");
        }

        String path = integrationConfiguration.getValue(Constants.SC_ATTR_PATH);
        if (null == path || (path.startsWith("/") && path.length() < 2) || (!path.startsWith("/") && path.length() < 1)) {
            return LogUtil.createError("Invalid path", "Invalid path specified");
        }
        if(!path.startsWith("/")) path = "/" + path;

        // Get the file properties
        PathGetProperties pathGetProperties = new PathGetProperties();
        IntegrationResponse propertiesResponse = pathGetProperties.execute(integrationConfiguration, connectedSystemConfiguration, executionContext);
        if (!propertiesResponse.isSuccess()) {
            return propertiesResponse;
        }

        // If appending data, then get the length of the file and set the file to the end of the stream.
        boolean overwrite = integrationConfiguration.<Boolean>getValue(Constants.SC_ATTR_OVERWRITE);
        long position = 0;
        if (!overwrite) {
            position = Long.parseLong(propertiesResponse.getResult().get("length").toString());
        }

        // Copy the mime type so that it doesn't get erased.
        ContentType contentType = ContentType.parse(propertiesResponse.getResult().get("type").toString());
        if (null == contentType) contentType = ContentType.APPLICATION_OCTET_STREAM;
        if (null == contentType.getCharset()) contentType = contentType.withCharset(StandardCharsets.UTF_8);

        // Create the body
        String content = integrationConfiguration.getValue(Constants.SC_ATTR_CONTENT);
        if (null == content) content = "";
        HttpEntity entity = new ByteArrayEntity(content.getBytes(contentType.getCharset()), contentType, false);
        long flushLength = position + entity.getContentLength();

        // Create the URI
        URI uploadUri, flushUri;
        try {
            URIBuilder uriBuilder = new URIBuilder(resourceUri);
            uploadUri = uriBuilder
                    .setPath(uriBuilder.getPath() + path)
                    .addParameter("action", "append")
                    .addParameter("position", Long.toString(position))
                    .build();

            uriBuilder = new URIBuilder(resourceUri);
            flushUri = uriBuilder
                    .setPath(uriBuilder.getPath() + path)
                    .addParameter("action", "flush")
                    .addParameter("close", "true")
                    .addParameter("position", Long.toString(flushLength))
                    .build();
        } catch (URISyntaxException e) {
            return LogUtil.createError("Invalid URI", e.getMessage());
        }

        // Do the request
        HttpPatch request = new HttpPatch(uploadUri);
        request.setEntity(entity);
        BasicResponseHandler brh = new BasicResponseHandler();
        startTime = System.currentTimeMillis();

        try {
            executeResponse = client.execute(request, brh);
        } catch (IOException e) {
            executeResponse = LogUtil.createError("Unable to execute request to " + uploadUri.toString(), e.getMessage());
            logger.error(executeResponse.getError().getDetail());
        }
        // Fail if the upload was not a success
        if (!executeResponse.isSuccess()) {
            try {
                client.close();
            } catch (IOException ignored) {}

            endTime = System.currentTimeMillis();
            // Record some diagnostics
            Map<String, Object> requestDiagnostic = LogUtil.getIntegrationDataMap("request", request.toString(), "operation", this.getClass().getSimpleName());
            IntegrationDesignerDiagnostic integrationDesignerDiagnostic = IntegrationDesignerDiagnostic.builder()
                    .addRequestDiagnostic(requestDiagnostic)
                    .addExecutionTimeDiagnostic(endTime - startTime)
                    .build();

            LogUtil.mergeDiagnostic(executeResponse.getIntegrationDesignerDiagnostic(), integrationDesignerDiagnostic);return executeResponse;
        }

        // Finalise and flush the data
        request = new HttpPatch(flushUri);
        request.setEntity(new StringEntity("", contentType, false));
        try {
            executeResponse = client.execute(request, brh);
        } catch (IOException e) {
            executeResponse = LogUtil.createError("Unable to execute request to " + flushUri.toString(), e.getMessage());
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
