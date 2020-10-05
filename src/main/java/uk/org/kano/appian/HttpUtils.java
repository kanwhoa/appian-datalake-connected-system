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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
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
}