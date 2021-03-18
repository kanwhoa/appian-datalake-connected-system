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

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.ProxyConfigurationData;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * A set of HTTP utilities to do authentication and such
 */
public class HttpUtils {
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    // The signing request interceptor
    private static SigningHttpRequestInterceptor signingHttpRequestInterceptor = new SigningHttpRequestInterceptor();


    /**
     * Create an HTTP Client that has a set of pre-configured authentication helpers. This does not
     * honour the proxy excludes, firstly due to support, but also, the ADLS Gen 2 host should be external.
     * @return
     */
    public static CloseableHttpClient getHttpClient(ExecutionContext executionContext) {
        HttpHost proxyHost = null;
        BasicCredentialsProvider proxyCredentials = null;
        ProxyConfigurationData proxyConfigurationData = null;

        if (null != executionContext) proxyConfigurationData = executionContext.getProxyConfigurationData();

        if (null != proxyConfigurationData && proxyConfigurationData.isEnabled()) {
            String ph = proxyConfigurationData.getHost();
            int pp = proxyConfigurationData.getPort();
            if (null != ph) ph = ph.trim();

            if (null != ph && !ph.isEmpty() && pp > 0) {
                logger.debug("Proxy settings detected, adding configuration to {}:{}", ph, pp);
                proxyHost = new HttpHost(proxyConfigurationData.getHost(), proxyConfigurationData.getPort());

                String puser = proxyConfigurationData.getUsername();
                String ppass = proxyConfigurationData.getPassword();
                if (null != puser) puser = puser.trim();
                if (null != ppass) ppass = ppass.trim();

                // Only add if there's a valid username and password.
                if (null != puser && !puser.isEmpty() && null != ppass && !ppass.isEmpty()) {
                    logger.debug("Proxy credentials detected, adding configuration");
                    proxyCredentials = new BasicCredentialsProvider();
                    proxyCredentials.setCredentials(new AuthScope(proxyHost), new UsernamePasswordCredentials(proxyConfigurationData.getUsername(), proxyConfigurationData.getPassword().toCharArray()));
                }
            }
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
            URI location = builder
                    .setScheme("https")
                    .setHost(configuration.getValue(AzureDatalakeConnectedSystemTemplate.CS_ADLS_G2_ACCOUNT_NAME) + AzureDatalakeConnectedSystemTemplate.CS_ADLS_G2_DOMAINNAME)
                    .setPath(configuration.getValue(AzureDatalakeConnectedSystemTemplate.CS_ADLS_G2_FILESYSTEM))
                    .build();
            logger.debug("Using datalake at URL {}", location.toString());
            return location;
        } catch (IOException | URISyntaxException e) {
            logger.error("Unable to build URL", e);
            return null;
        }
    }
}
