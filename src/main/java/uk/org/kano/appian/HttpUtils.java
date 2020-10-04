package uk.org.kano.appian;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.ProxyConfigurationData;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.AuthenticationStrategy;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A set of HTTP utilities to do authentication and such
 */
public class HttpUtils {
    private static Logger logger = Logger.getLogger(HttpUtils.class);

    // The signing request interceptor
    private static SigningHttpRequestInterceptor signingHttpRequestInterceptor = new SigningHttpRequestInterceptor();


    /**
     * Create an HTTP Client that has a set of pre-configured authentication helpers.
     * FIXME, this doesn't honour the proxy exclude hosts
     * @return
     */
    public static CloseableHttpClient getHttpClient(ExecutionContext executionContext) {
        HttpHost proxyHost = null;
        BasicCredentialsProvider proxyCredentials = null;

        if (null != executionContext && null != executionContext.getProxyConfigurationData()) {
            ProxyConfigurationData proxyConfigurationData = executionContext.getProxyConfigurationData();

            proxyHost = new HttpHost(proxyConfigurationData.getHost(), proxyConfigurationData.getPort());
            proxyCredentials = new BasicCredentialsProvider();
            proxyCredentials.setCredentials(
                    new AuthScope(proxyHost),
                    new UsernamePasswordCredentials(proxyConfigurationData.getUsername(), proxyConfigurationData.getPassword().toCharArray()));
        }
        return HttpClients.custom()
                .addRequestInterceptorLast(signingHttpRequestInterceptor)
                .setDefaultHeaders(signingHttpRequestInterceptor.getAuthenticationHeaders())
                .setProxy(proxyHost)
                .setDefaultCredentialsProvider(proxyCredentials)
                .build();
    }

    /**
     * Get the base URL of the datalake
     * @param configuration
     * @return
     */
    public static URI getBaseUri(SimpleConfiguration configuration) {
        URIBuilder builder = new URIBuilder();
        try {
            signingHttpRequestInterceptor.setKey(configuration.getValue(AzureDatalakeConnectedSystemTemplate.CS_ADLS_G2_ACCOUNT_KEY));
            return builder
                    .setScheme("https")
                    .setHost(configuration.getValue(AzureDatalakeConnectedSystemTemplate.CS_ADLS_G2_ACCOUNT_NAME) + AzureDatalakeConnectedSystemTemplate.CS_ADLS_G2_DOMAINNAME)
                    .setPath(configuration.getValue(AzureDatalakeConnectedSystemTemplate.CS_ADLS_G2_FILESYSTEM))
                    .build();
        } catch (IOException | URISyntaxException e) {
            logger.error("Unable to build URL", e);
            return null;
        }
    }

    /**
     * Get a handler for basic responses. Do not use for file downloads.
     * @param missingResourceIsError True if a 404 is to be treated as an error
     */
    public static BasicResponseHandler getBasicResponseHandler(boolean missingResourceIsError) {
        return new BasicResponseHandler(missingResourceIsError);
    }
    public static BasicResponseHandler getBasicResponseHandler() {
        return new BasicResponseHandler(true);
    }

}

/**
 * The basic response handler
 */
class BasicResponseHandler implements HttpClientResponseHandler<IntegrationResponse> {
    private static Logger logger = Logger.getLogger(BasicResponseHandler.class);
    private ObjectMapper mapper = new ObjectMapper();
    private boolean missingResourceIsError = true;

    private BasicResponseHandler() {}

    BasicResponseHandler(boolean missingResourceIsError) {
        this.missingResourceIsError = missingResourceIsError;
    }

    @Override
    public IntegrationResponse handleResponse(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {
        int status = classicHttpResponse.getCode();

        IntegrationResponse rv;
        String responseJson = null;
        if (status >= HttpStatus.SC_SUCCESS && status < HttpStatus.SC_REDIRECTION) {
            Map<String, Object> responseMap = new HashMap<>();

            // Parse the properties
            responseMap.put("properties", getProperties(classicHttpResponse));
            responseMap.put("requestId", getHeaderValue(classicHttpResponse.getHeader("x-ms-request-id")));
            responseMap.put("exists", true);
            responseMap.put("body", null);
            responseMap.put("length", null);

            // Extract the response body.
            HttpEntity entity = classicHttpResponse.getEntity();
            if (null != entity) {
                try (InputStream entityInputStream = entity.getContent()) {
                    String body = null;
                    String contentType = entity.getContentType();
                    if (null == contentType) { // Empty body
                        // Do nothing
                    } else if (contentType.startsWith(ContentType.APPLICATION_JSON.getMimeType())) { // Parse JSON
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int bufLen, bufSize=1024;
                        byte[] buf = new byte[1024];
                        while ((bufLen = entityInputStream.read(buf, 0, bufSize)) > 0) {
                            baos.write(buf, 0, bufLen);
                        }
                        String contentEncoding = entity.getContentEncoding();
                        if (null == contentEncoding) contentEncoding = StandardCharsets.UTF_8.name();
                        body = new String(baos.toByteArray(), contentEncoding);
                    } else {
                        logger.info("Do not know how to handle a response body of type " + entity.getContentType());
                    }

                    responseMap.put("body", body);
                    rv = IntegrationResponse.forSuccess(responseMap).build();
                } catch (IOException e) {
                    logger.error("Error while parsing the response body", e);
                    rv = LogUtil.createError("Error reading the response body", e.getMessage());
                }
            } else {
                Header contentLength = classicHttpResponse.getFirstHeader("Content-Length");
                if (null != contentLength) {
                    responseMap.put("length", Long.parseLong(null == contentLength.getValue() ? "0" : contentLength.getValue()));
                }
                rv = IntegrationResponse.forSuccess(responseMap).build();
            }
        } else if (status == HttpStatus.SC_BAD_REQUEST) {
            rv = LogUtil.createError("Bad Request", classicHttpResponse.getReasonPhrase());
        } else if (status == HttpStatus.SC_UNAUTHORIZED) {
            rv = LogUtil.createError("Unauthorised", "Credentials not provided or are incorrect");
        } else if (status == HttpStatus.SC_NOT_FOUND) {
            if (missingResourceIsError) {
                rv = LogUtil.createError("Not Found", classicHttpResponse.getReasonPhrase());
            } else {
                Map<String, Object> responseMap = new HashMap<>();
                responseMap.put("requestId", getHeaderValue(classicHttpResponse.getHeader("x-ms-request-id")));
                responseMap.put("body", null);
                responseMap.put("exists", false);
                rv = IntegrationResponse.forSuccess(responseMap).build();
            }
        } else if (status >= HttpStatus.SC_INTERNAL_SERVER_ERROR && status <= HttpStatus.SC_SERVICE_UNAVAILABLE) {
            rv = LogUtil.createError("Service Unavailable", classicHttpResponse.getReasonPhrase());
        } else {
            rv = LogUtil.createError("Unknown error", "An unknown error occurred ("+status+" - " + classicHttpResponse.getReasonPhrase() + ")");
        }

        IntegrationDesignerDiagnostic integrationDesignerDiagnostic = IntegrationDesignerDiagnostic.builder()
                .addResponseDiagnostic(
                        LogUtil.getDiagnosticMap("responseCode", Integer.toString(classicHttpResponse.getCode()), "reasonMessage", classicHttpResponse.getReasonPhrase())
                )
                .build();
        return rv.toBuilder().withDiagnostic(integrationDesignerDiagnostic).build();
    }

    /**
     * Get the Azure properties from the object
     * @param classicHttpResponse
     */
    private Map<String, Object> getProperties(ClassicHttpResponse classicHttpResponse) {
        HashMap<String, Object> rv = new HashMap<>();

        Header propsHeader = null;
        try {
            propsHeader = classicHttpResponse.getHeader("x-ms-properties");
        } catch (ProtocolException e) {
            logger.warn("Unable to extract properties header", e);
            return rv;
        }
        String value = getHeaderValue(propsHeader);

        if (null != value) {
            Arrays.stream(propsHeader.getValue().split(",")).forEach(option -> {
                String[] parts = option.split("=", 2);
                rv.put(parts[0], parts.length < 2 ? "" : parts[1]);
            });
        }
        return rv;
    }

    /**
     * Get a header value
     * @param header
     */
    private String getHeaderValue(Header header) {
        if (null == header) return null;
        String value = header.getValue();
        if (null == value) return null;
        value = value.trim();
        return "".equals(value) ? null : value;
    }
}
