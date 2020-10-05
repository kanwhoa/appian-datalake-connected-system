package uk.org.kano.appian.filesystem;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.log4j.Logger;
import sun.rmi.runtime.Log;
import uk.org.kano.appian.BasicResponseHandler;
import uk.org.kano.appian.HttpUtils;
import uk.org.kano.appian.LogUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@TemplateId(name="FileSystemGetProperties")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.READ)
public class GetProperties extends SimpleIntegrationTemplate {
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    protected SimpleConfiguration getConfiguration(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, PropertyPath updatedProperty, ExecutionContext executionContext) {
        return integrationConfiguration;
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
        try {
            resourceUri = uriBuilder.addParameter("resource", "filesystem").build();
        } catch (URISyntaxException ignored) {} // Should never happen

        // Do the request
        HttpHead request = new HttpHead(resourceUri);
        IntegrationResponse executeResponse = null;
        startTime = System.currentTimeMillis();

        try {
            BasicResponseHandler brh = new BasicResponseHandler();
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
        Map<String, Object> requestDiagnostic = LogUtil.getDiagnosticMap("request", request.toString(), "operation", this.getClass().getSimpleName());
        IntegrationDesignerDiagnostic integrationDesignerDiagnostic = IntegrationDesignerDiagnostic.builder()
                .addRequestDiagnostic(requestDiagnostic)
                .addExecutionTimeDiagnostic(endTime - startTime)
                .build();

        LogUtil.mergeDiagnostic(executeResponse.getIntegrationDesignerDiagnostic(), integrationDesignerDiagnostic);
        return executeResponse;
    }
}
