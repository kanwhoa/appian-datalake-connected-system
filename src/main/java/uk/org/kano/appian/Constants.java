package uk.org.kano.appian;

import org.apache.hc.core5.http.ContentType;

import java.nio.charset.StandardCharsets;

/**
 * A selectoin of constants used in the
 */
public final class Constants {
    private Constants() {}

    public static final String SC_ATTR_FILE = "file";
    public static final String SC_ATTR_OVERWRITE = "overwrite";
    public static final String SC_ATTR_CONTENT = "content";
    public static final String SC_ATTR_PATH = "path";
    public static final String SC_ATTR_RECURSIVE = "recursive";
    public static final String SC_ATTR_SOURCE_PATH = "sourcePath";
    public static final String SC_ATTR_DESTINATION_PATH = "destinationPath";
    public static final String SC_ATTR_BASE64_BODY = "base64Body";
    public static final String SC_ATTR_MIME_TYPE = "mimeType";
}
