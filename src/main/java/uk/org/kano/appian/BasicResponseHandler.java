package uk.org.kano.appian;

import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * The basic response handler
 */
public class BasicResponseHandler implements HttpClientResponseHandler<IntegrationResponse> {
    private static Logger logger = Logger.getLogger(BasicResponseHandler.class);
    private boolean missingResourceIsError = true;
    private boolean encodeBodyAsBase64 = false;

    /**
     * Handle a missing resource as an error.
     * @param missingResourceIsError True to map HTTP 403 to an error, false otherwise. Default true.
     */
    public void setHandleMissingResourceAsError(boolean missingResourceIsError) {
        this.missingResourceIsError = missingResourceIsError;
    }

    /**
     * When getting a body response, encode it as base64. Default false.
     * @param encodeBodyAsBase64 Return any body as a base64 encoded value.
     */
    public void setEncodeBodyAsBase64(boolean encodeBodyAsBase64) {
        this.encodeBodyAsBase64 = encodeBodyAsBase64;
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
            responseMap.put("type", null);

            // Extract the response body.
            HttpEntity entity = classicHttpResponse.getEntity();
            if (null != entity) {
                try (InputStream entityInputStream = entity.getContent()) {
                    String body = null;
                    ContentType contentType = ContentType.parse(entity.getContentType());
                    if (null != contentType) responseMap.put("type", contentType.toString());

                    if (null == contentType) {
                        // Happens on a delete
                    } else if (ContentType.APPLICATION_JSON.isSameMimeType(contentType) || ContentType.APPLICATION_OCTET_STREAM.isSameMimeType(contentType) || contentType.getMimeType().startsWith("text/")) { // Read the body
                        // If application/octet-stream, force read to a string.
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int bufLen, bufSize=1024;
                        byte[] buf = new byte[1024];
                        while ((bufLen = entityInputStream.read(buf, 0, bufSize)) > 0) {
                            baos.write(buf, 0, bufLen);
                        }

                        if (encodeBodyAsBase64 || ContentType.APPLICATION_OCTET_STREAM.isSameMimeType(contentType)) {
                            body = Base64.getEncoder().encodeToString(baos.toByteArray());
                        } else {
                            Charset charset = contentType.getCharset();
                            if (null == charset) charset = StandardCharsets.UTF_8;
                            body = new String(baos.toByteArray(), charset);
                        }
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
                Header contentType = classicHttpResponse.getFirstHeader("Content-Type");
                if (null != contentType) {
                    responseMap.put("type", ContentType.parse(contentType.getValue()).toString());
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
                        LogUtil.getIntegrationDataMap("responseCode", Integer.toString(classicHttpResponse.getCode()), "reasonMessage", classicHttpResponse.getReasonPhrase())
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
