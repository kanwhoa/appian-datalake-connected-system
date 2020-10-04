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
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.log4j.Logger;
import uk.org.kano.appian.Constants;
import uk.org.kano.appian.HttpUtils;
import uk.org.kano.appian.LogUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * TODO: allow renames.
 */
@TemplateId(name="PathRename")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.WRITE)
public class Rename extends SimpleIntegrationTemplate {
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    protected SimpleConfiguration getConfiguration(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, PropertyPath updatedProperty, ExecutionContext executionContext) {
        return integrationConfiguration.setProperties(
                textProperty(Constants.SC_ATTR_SOURCE_PATH)
                        .label("Source path")
                        .description("The source path.")
                        .isRequired(true)
                        .isExpressionable(true)
                        .build(),
                textProperty(Constants.SC_ATTR_DESTINATION_PATH)
                        .label("Destination path")
                        .description("The destination path.")
                        .isRequired(true)
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

        String sourcePath = integrationConfiguration.getValue(Constants.SC_ATTR_SOURCE_PATH);
        if (null == sourcePath || (sourcePath.startsWith("/") && sourcePath.length() < 2) || (!sourcePath.startsWith("/") && sourcePath.length() < 1)) {
            return LogUtil.createError("Invalid path", "Invalid source path specified");
        }
        if(!sourcePath.startsWith("/")) sourcePath = "/" + sourcePath;

        String destinationPath = integrationConfiguration.getValue(Constants.SC_ATTR_DESTINATION_PATH);
        if (null == destinationPath || (destinationPath.startsWith("/") && destinationPath.length() < 2) || (!destinationPath.startsWith("/") && destinationPath.length() < 1)) {
            return LogUtil.createError("Invalid path", "Invalid destination path specified");
        }
        if(!destinationPath.startsWith("/")) destinationPath = "/" + destinationPath;

        // Create the URI
        try {
            URIBuilder uriBuilder = new URIBuilder(resourceUri);
            sourcePath = uriBuilder
                    .setPath(uriBuilder.getPath() + sourcePath)
                    .build().getPath();

            uriBuilder = new URIBuilder(resourceUri);
            resourceUri = uriBuilder
                    .setPath(uriBuilder.getPath() + destinationPath)
                    .build();
        } catch (URISyntaxException e) {
            return LogUtil.createError("Invalid URI", e.getMessage());
        }

        // Do the request
        HttpPut request = new HttpPut(resourceUri);
        request.addHeader("x-ms-rename-source", sourcePath);
        IntegrationResponse executeResponse = null;
        startTime = System.currentTimeMillis();

        // Create an empty entity
        HttpEntity entity = new StringEntity("", ContentType.APPLICATION_OCTET_STREAM, false);
        request.setEntity(entity);

        try {
            executeResponse = client.execute(request, HttpUtils.getBasicResponseHandler());
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
        Map<String, Object> requestDiagnostic = LogUtil.getDiagnosticMap("request", request.toString(), "operation", this.getClass().getSimpleName());
        IntegrationDesignerDiagnostic integrationDesignerDiagnostic = IntegrationDesignerDiagnostic.builder()
                .addRequestDiagnostic(requestDiagnostic)
                .addExecutionTimeDiagnostic(endTime - startTime)
                .build();

        LogUtil.mergeDiagnostic(executeResponse.getIntegrationDesignerDiagnostic(), integrationDesignerDiagnostic);
        return executeResponse;
    }
}
