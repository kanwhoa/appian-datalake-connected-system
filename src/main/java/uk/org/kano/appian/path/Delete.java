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
import org.apache.hc.client5.http.classic.methods.HttpDelete;
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
@TemplateId(name="PathDelete")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.WRITE)
public class Delete extends SimpleIntegrationTemplate {
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    protected SimpleConfiguration getConfiguration(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, PropertyPath updatedProperty, ExecutionContext executionContext) {
        return integrationConfiguration.setProperties(
                textProperty(Constants.SC_ATTR_PATH)
                        .label("Path to delete")
                        .description("The path to be delete.")
                        .isRequired(true)
                        .isExpressionable(true)
                        .build(),
                booleanProperty(Constants.SC_ATTR_RECURSIVE)
                        .label("Delete recursively")
                        .description("Delete all paths/files under the path. Default false.")
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
        boolean recursive = Boolean.parseBoolean(integrationConfiguration.getValue(Constants.SC_ATTR_RECURSIVE));
        String path = integrationConfiguration.getValue(Constants.SC_ATTR_PATH);
        if (null == path || (path.startsWith("/") && path.length() < 2) || (!path.startsWith("/") && path.length() < 1)) {
            return LogUtil.createError("Invalid path", "Invalid path specified");
        }
        if(!path.startsWith("/")) path = "/" + path;

        // Create the URI
        try {
            resourceUri = uriBuilder
                    .setPath(uriBuilder.getPath() + path)
                    .addParameter("recursive", Boolean.toString(recursive))
                    .build();
        } catch (URISyntaxException e) {
            return LogUtil.createError("Invalid URI", e.getMessage());
        }

        // Do the request
        HttpDelete request = new HttpDelete(resourceUri);
        IntegrationResponse executeResponse = null;
        startTime = System.currentTimeMillis();

        try {
            executeResponse = client.execute(request, HttpUtils.getBasicResponseHandler(false));
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
