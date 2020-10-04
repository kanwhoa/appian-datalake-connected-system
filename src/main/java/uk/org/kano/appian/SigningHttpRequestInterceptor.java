package uk.org.kano.appian;

import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * An HTTP request interceptor for adding the request signing.
 */
public class SigningHttpRequestInterceptor implements HttpRequestInterceptor {
    private Mac sha256_HMAC = null;

    public void setKey(String key) throws IOException {
        try {
            sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(Base64.getDecoder().decode(key), "HmacSHA256");
            sha256_HMAC.init(secret_key);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IOException("Unable to create signature algorithm", e);
        }
    }

    @Override
    public void process(HttpRequest httpRequest, EntityDetails entityDetails, HttpContext httpContext) throws IOException {
        String contentLength = "";
        if (null != entityDetails && entityDetails.getContentLength() > 0) contentLength = Long.toString(entityDetails.getContentLength());

        // Add the time and date of the request
        httpRequest.addHeader("x-ms-date", DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now().withZoneSameInstant(ZoneId.of("GMT"))));

        // Get the URI, we'll use it multiple times
        URI location = null;
        try {
            location = httpRequest.getUri();
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI", e);
        }

        // Parse the canonical location
        String uriPath = location.getPath();
        if (null == uriPath) throw new IOException("Filesystem not specified");
        String[] uriPathSegments = uriPath.split("/");
        //if (uriPathSegments.length < 2) throw new IOException("Filesystem not specified");

        // Get the accountName
        String accountName = location.getHost().replaceAll("\\..*$", "");

        // Create the options string
        StringBuilder optionsBuilder = new StringBuilder();
        if (null != location.getQuery()) {
            Arrays.stream(location.getQuery().split("&")).forEach(option -> {
                String[] parts = option.split("=", 2);
                optionsBuilder.append('\n');
                optionsBuilder.append(parts[0]);
                optionsBuilder.append(':');
                optionsBuilder.append(2 == parts.length ? parts[1] : "");
            });
        }

        String contentType = "", contentEncoding = "";
        if (null != entityDetails) {
            contentType = entityDetails.getContentType();
            if (null == contentType) contentType = "";
            contentEncoding = entityDetails.getContentEncoding();
            if (null == contentEncoding) contentEncoding = "";
        }

        String headerBlock = httpRequest.getMethod() + "\n" +
                contentEncoding + "\n" +
                getHeaderValueOrEmpty(httpRequest, "Content-Language") + "\n" +
                contentLength + "\n" +
                getHeaderValueOrEmpty(httpRequest, "Content-MD5") + "\n" +
                contentType + "\n" +
                getHeaderValueOrEmpty(httpRequest, "Date") + "\n" +
                getHeaderValueOrEmpty(httpRequest, "If-Modified-Since") + "\n" +
                getHeaderValueOrEmpty(httpRequest, "If-Match") + "\n" +
                getHeaderValueOrEmpty(httpRequest, "If-None-Match") + "\n" +
                getHeaderValueOrEmpty(httpRequest, "If-Unmodified-Since") + "\n" +
                getHeaderValueOrEmpty(httpRequest, "Range") + "\n" +
                StreamSupport.stream(Spliterators.spliteratorUnknownSize(httpRequest.headerIterator(), Spliterator.ORDERED), false)
                    .filter(header -> header.getName().toLowerCase().startsWith("x-ms-"))
                    .sorted(Comparator.comparing(o -> o.getName().toLowerCase()))
                    .map(header -> header.getName().toLowerCase() + ":" + (null == header.getValue() ? "" : header.getValue()))
                    .collect(Collectors.joining("\n")) + "\n" +
                "/" + accountName +  location.getPath() + optionsBuilder.toString();

        //sha256_HMAC.reset();
        byte[] mac = sha256_HMAC.doFinal(headerBlock.getBytes(StandardCharsets.UTF_8));
        httpRequest.addHeader("Authorization", "SharedKey " + accountName + ":" + Base64.getEncoder().encodeToString(mac));
    }

    /**
     * Get a header
     * @param name
     * @return the first header value, or an empty String
     */
    private String getHeaderValueOrEmpty(HttpRequest httpRequest, String name) {
        Header header = null;
        try {
            header = httpRequest.getHeader(name);
            return (null == header || null == header.getValue()) ? "" : header.getValue();
        } catch (ProtocolException e) {
            return "";
        }
    }

    /**
     * Create a set of default headers for use
     * @return
     */
    public Collection<? extends Header> getAuthenticationHeaders() {
        ArrayList<Header> headers = new ArrayList<>();
        headers.add(new BasicHeader("x-ms-version", "2019-02-02"));
        return headers;
    }
}
