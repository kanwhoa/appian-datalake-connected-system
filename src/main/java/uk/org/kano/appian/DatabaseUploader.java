package uk.org.kano.appian;

import com.appian.connectedsystems.simplified.sdk.SimpleIntegrationTemplate;
import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.ExecutionContext;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.TemplateId;
import com.appian.connectedsystems.templateframework.sdk.configuration.PropertyPath;
import com.appian.connectedsystems.templateframework.sdk.configuration.SystemType;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateRequestPolicy;
import com.appian.connectedsystems.templateframework.sdk.metadata.IntegrationTemplateType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.log4j.Logger;
import uk.org.kano.appian.path.Create;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Get the properties of a path
 */
@TemplateId(name="DatabaseUploader")
@IntegrationTemplateType(IntegrationTemplateRequestPolicy.WRITE)
public class DatabaseUploader extends SimpleIntegrationTemplate {
    private Logger logger = Logger.getLogger(this.getClass());
    private static final String DEFAULT_JNDI_RESOURCE = "jdbc/Appian";
    private static final ContentType CSV_CONTENT_TYPE = ContentType.parse("text/csv;charset=utf8");


    @Override
    protected SimpleConfiguration getConfiguration(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, PropertyPath updatedProperty, ExecutionContext executionContext) {
        return integrationConfiguration.setProperties(
                textProperty(Constants.SC_ATTR_PATH)
                        .label("Path")
                        .description("The root path to store tables in.")
                        .isRequired(true)
                        .isExpressionable(true)
                        .build(),
                listTypeProperty(Constants.SC_ATTR_TABLES)
                        .label("Tables")
                        .description("The list of tables to upload.")
                        .isRequired(true)
                        .isExpressionable(true)
                        .itemType(SystemType.STRING)
                        .build(),
                textProperty(Constants.SC_ATTR_JNDI_RESOURCE)
                        .label("JNDI resource")
                        .description("The location of the JDNI resource (default jdbc/Appian).")
                        .isRequired(false)
                        .isExpressionable(true)
                        .isImportCustomizable(true)
                        .build()
        );
    }

    @Override
    protected IntegrationResponse execute(SimpleConfiguration integrationConfiguration, SimpleConfiguration connectedSystemConfiguration, ExecutionContext executionContext) {
        long startTime, endTime;
        DataSource dataSource;
        Map<String, Object> configurationValues;
        SimpleConfiguration subIntegrationConfiguration;
        IntegrationResponse subIntegrationResponse;
        IntegrationResponse executeResponse = null;
        Create pathCreate = new Create();
        CloseableHttpClient client = HttpUtils.getHttpClient(executionContext);

        URI resourceUri = HttpUtils.getBaseUri(connectedSystemConfiguration);
        if (null == resourceUri) {
            return LogUtil.createError("Invalid base URI", "The base URI is invalid");
        }

        // Get the data source context
        try {
            Context initCtx = new InitialContext();
            String jndiResource = integrationConfiguration.getValue(Constants.SC_ATTR_JNDI_RESOURCE);
            if (null == jndiResource) jndiResource = DEFAULT_JNDI_RESOURCE;

            dataSource = (DataSource) initCtx.lookup(jndiResource);
        } catch (NamingException e) {
            IntegrationResponse error =  LogUtil.createError("Unable to get JNDI resource", e.getMessage());
            logger.error(error.getError().getTitle(), e);
            return error;
        }

        // Get the basic properties
        String basePath = integrationConfiguration.getValue(Constants.SC_ATTR_PATH);
        if (null == basePath) {
            return LogUtil.createError("Invalid base path", "Base path was not specified");
        }

        // Get the connection
        Connection conn;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            IntegrationResponse error =  LogUtil.createError("Unable to get database connection", e.getMessage());
            logger.error(error.getError().getTitle(), e);
            return error;
        }

        // Create the basepath
        subIntegrationConfiguration = AppianUtils.getIntegrationConfiguration(pathCreate, connectedSystemConfiguration, executionContext);
        configurationValues = new HashMap<>();
        configurationValues.put(Constants.SC_ATTR_PATH, basePath);
        configurationValues.put(Constants.SC_ATTR_FILE, false);
        configurationValues.put(Constants.SC_ATTR_OVERWRITE, true);
        configurationValues.forEach(subIntegrationConfiguration::setValue);
        subIntegrationResponse = pathCreate.execute(subIntegrationConfiguration.toConfiguration(), connectedSystemConfiguration.toConfiguration(), executionContext);
        if (!subIntegrationResponse.isSuccess()) {
            try { conn.close(); } catch (SQLException ignored) {}
            return subIntegrationResponse;
        }

        startTime = System.currentTimeMillis();
        // Loop over the tables to make sure they exist and start the upload
        List<String> tables = integrationConfiguration.getValue(Constants.SC_ATTR_TABLES);
        for (String table: tables) {
            // Check the table exists
            try {
                DatabaseMetaData databaseMetaData = conn.getMetaData();
                ResultSet resultSet = databaseMetaData.getTables(null, null, table, null);

                int tableCount = 0;
                while (resultSet.next()) {
                    tableCount++;
                }
                resultSet.close();

                // FIXME
                //if (0 == tableCount) throw  new SQLException("Table does not exist");
                //if (tableCount > 1) throw  new SQLException("Name matched multiple tables");
            } catch (SQLException e) {
                try { conn.close(); } catch (SQLException ignored) {}
                IntegrationResponse error =  LogUtil.createError("Unable to get table information for "+table, e.getMessage());
                logger.error(error.getError().getTitle(), e);
                return error;
            }

            String tablePath = String.join("/", basePath, table).replaceAll("//", "/")+".csv";
            // Create the file
            subIntegrationConfiguration = AppianUtils.getIntegrationConfiguration(pathCreate, connectedSystemConfiguration, executionContext);
            configurationValues = new HashMap<>();
            configurationValues.put(Constants.SC_ATTR_PATH, tablePath);
            configurationValues.put(Constants.SC_ATTR_FILE, true);
            configurationValues.put(Constants.SC_ATTR_OVERWRITE, true);
            configurationValues.put(Constants.SC_ATTR_MIME_TYPE, CSV_CONTENT_TYPE.toString());
            configurationValues.forEach(subIntegrationConfiguration::setValue);
            subIntegrationResponse = pathCreate.execute(subIntegrationConfiguration.toConfiguration(), connectedSystemConfiguration.toConfiguration(), executionContext);
            if (!subIntegrationResponse.isSuccess()) {
                try { conn.close(); } catch (SQLException ignored) {}
                return subIntegrationResponse;
            }

            // The pipe
            final PipedInputStream pipeIn;
            final PipedOutputStream pipeOut;
            final int bufSize = 1048576; // 1MB buffer/post size.
            try {
                pipeIn = new PipedInputStream(bufSize);
                pipeOut = new PipedOutputStream(pipeIn);
            } catch (IOException e) {
                try { conn.close(); } catch (SQLException ignored) {}
                IntegrationResponse error =  LogUtil.createError("Unable to create pipes", e.getMessage());
                logger.error(error.getError().getTitle(), e);
                return error;
            }

            // Runnable 1 to export the data and write to a CSV printer
            Callable<IntegrationResponse> exporterTask = () -> {
                IntegrationResponse integrationResponse;

                // Export and upload
                try (Statement statement = conn.createStatement();
                     ResultSet resultSet = statement.executeQuery("select * from " + table);
                     CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(pipeOut), CSVFormat.RFC4180.withQuoteMode(QuoteMode.ALL_NON_NULL).withHeader(resultSet))
                ) {
                    printer.printRecords(resultSet);
                    pipeOut.flush();
                    return null;
                } catch (IOException | SQLException e) {
                    integrationResponse = LogUtil.createError("Database exporter thread threw an exception", e.getMessage());
                    logger.error(integrationResponse.getError().getTitle(), e);
                    return integrationResponse;
                }
            };

            // Runnable 2 to upload the data. This is very ugly because ADLS does not support a streaming API. Therefore. we need to set
            // each call to be one that is specific in length.
            Callable<IntegrationResponse> uploaderTask = () -> {
                byte[] buf = new byte[bufSize];
                int bufLen;
                long offset = 0;
                IntegrationResponse integrationResponse;
                URI uploadUri = resourceUri;

                try {
                    for(offset = 0; (bufLen = pipeIn.read(buf, 0, bufSize)) > 0; offset += bufLen) {
                        // Update the URL for the data package
                        try {
                            URIBuilder uriBuilder = new URIBuilder(resourceUri);
                            uploadUri = uriBuilder
                                    .setPath(uriBuilder.getPath() + tablePath)
                                    .addParameter("action", "append")
                                    .addParameter("position", Long.toString(offset))
                                    .build();
                        } catch (URISyntaxException e) {
                            try { conn.close(); } catch (SQLException ignored) {}
                            return LogUtil.createError("Invalid URI", e.getMessage());
                        }

                        HttpPatch request = new HttpPatch(uploadUri);
                        ByteArrayEntity entity = new ByteArrayEntity(buf, 0, bufLen, CSV_CONTENT_TYPE, false);
                        request.setEntity(entity);
                        BasicResponseHandler brh = new BasicResponseHandler();
                        integrationResponse = client.execute(request, brh);
                        if (!integrationResponse.isSuccess()) return integrationResponse;
                    }

                    Map<String, Object> responseData = LogUtil.getIntegrationDataMap("length", offset);
                    integrationResponse = IntegrationResponse.forSuccess(responseData).build();
                } catch (IOException e) {
                    integrationResponse = LogUtil.createError("Unable to execute request to " + uploadUri.toString(), e.getMessage());
                    logger.error(integrationResponse.getError().getDetail());
                    return integrationResponse;
                } finally {
                    try { pipeIn.close(); } catch (IOException ignored) { }
                }

                return integrationResponse;
            };

            // Run each thread and check the responses.
            ExecutorService executorService = Executors.newFixedThreadPool(2);
            CompletionService<IntegrationResponse> completionService = new ExecutorCompletionService<>(executorService);
            completionService.submit(exporterTask);
            completionService.submit(uploaderTask);

            // Wait for any of the threads to complete
            int remaining = 2;
            IntegrationResponse uploaderResponse = null;
            while (remaining > 0) {
                Future<IntegrationResponse> taskFuture;
                IntegrationResponse taskResult;
                try {
                    taskFuture = completionService.take();
                    if (null == taskFuture) continue;
                    taskResult = taskFuture.get();
                } catch (InterruptedException | ExecutionException ignored) {
                    continue;
                }

                remaining--;
                // Check for an error
                if (null == taskResult || taskResult.isSuccess()) { // Usual case
                    uploaderResponse = taskResult;
                    continue;
                }
                if (null == uploaderResponse) { // Only take the first result, which is likely the one that caused the error.
                    uploaderResponse = taskResult;
                }

                // If we've already shutdown, just loop
                if (executorService.isShutdown()) continue;
                // Catch an error
                logger.error("Exporter task threw an error");
                // Close the pipes so that that processes terminate
                try { pipeIn.close(); } catch (IOException ignored) { }
                try { pipeOut.close(); } catch (IOException ignored) { }
                // Shutdown the executor
                executorService.shutdownNow();
                // Loop through and wait for the others
            }

            // Check the response data
            if (null != uploaderResponse && !uploaderResponse.isSuccess()) {
                try { conn.close(); } catch (SQLException ignored) {}
                return uploaderResponse;
            }
            if (null == uploaderResponse) {
                try { conn.close(); } catch (SQLException ignored) {}
                uploaderResponse =  LogUtil.createError("Uploader task failed", "No upload response specified");
                logger.error(uploaderResponse.getError().getDetail());
                return uploaderResponse;
            }
            Long flushLength = (Long)uploaderResponse.getResult().get("length");
            if (null == flushLength) {
                try { conn.close(); } catch (SQLException ignored) {}
                uploaderResponse =  LogUtil.createError("Uploader task failed", "No upload data length specified");
                logger.error(uploaderResponse.getError().getDetail());
                return uploaderResponse;
            }

            // Finally flush the data
            URI flushUri;
            try {
                URIBuilder uriBuilder = new URIBuilder(resourceUri);
                flushUri = uriBuilder
                        .setPath(uriBuilder.getPath() + tablePath)
                        .addParameter("action", "flush")
                        .addParameter("close", "true")
                        .addParameter("position", flushLength.toString())
                        .build();
            } catch (URISyntaxException e) {
                try { conn.close(); } catch (SQLException ignored) {}
                return LogUtil.createError("Invalid URI", e.getMessage());
            }

            HttpPatch request = new HttpPatch(flushUri);
            request.setEntity(new StringEntity("", CSV_CONTENT_TYPE, false));
            try {
                BasicResponseHandler brh = new BasicResponseHandler();
                executeResponse = client.execute(request, brh);
            } catch (IOException e) {
                try { conn.close(); } catch (SQLException ignored) {}
                executeResponse = LogUtil.createError("Unable to execute request to " + resourceUri.toString(), e.getMessage());
                logger.error(executeResponse.getError().getDetail());
                return executeResponse;
            }
        }
        endTime = System.currentTimeMillis();

        // Cleanup
        try { conn.close(); } catch (SQLException ignored) {}
        try { client.close(); } catch (IOException ignored) {}

        // Create the response
        Map<String, Object> requestDiagnostic = LogUtil.getIntegrationDataMap("operation", this.getClass().getSimpleName());
        IntegrationDesignerDiagnostic integrationDesignerDiagnostic = IntegrationDesignerDiagnostic.builder()
                .addRequestDiagnostic(requestDiagnostic)
                .addExecutionTimeDiagnostic(endTime - startTime)
                .build();
        Map<String, Object> responseData = LogUtil.getIntegrationDataMap("tables", tables);
        return IntegrationResponse.forSuccess(responseData).withDiagnostic(integrationDesignerDiagnostic).build();
    }
}
