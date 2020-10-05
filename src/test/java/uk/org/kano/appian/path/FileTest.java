package uk.org.kano.appian.path;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import org.junit.Before;
import org.junit.Test;
import uk.org.kano.appian.Constants;
import uk.org.kano.appian.TestBase;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class FileTest extends TestBase {
    private Create pathCreate = new Create();
    private Delete pathDelete = new Delete();
    private GetProperties pathGetProperties = new GetProperties();
    private Update pathUpdate = new Update();
    private Rename pathRename = new Rename();
    private List pathList = new List();
    private Read pathRead = new Read();

    // Files, will assume in the root directory.
    private String fileName1 = "example1.csv";
    private String fileName2 = "example2.csv";

    @Before
    public void preClean() {
        SimpleConfiguration integrationConfiguration;
        Map<String, Object> values;

        integrationConfiguration = getIntegrationConfiguration(pathDelete);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName1);
        values.put(Constants.SC_ATTR_RECURSIVE, Boolean.TRUE);
        setValues(integrationConfiguration, values);
        pathDelete.execute(integrationConfiguration, connectedSystemConfiguration, null);

        integrationConfiguration = getIntegrationConfiguration(pathDelete);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName2);
        values.put(Constants.SC_ATTR_RECURSIVE, Boolean.TRUE);
        setValues(integrationConfiguration, values);
        pathDelete.execute(integrationConfiguration, connectedSystemConfiguration, null);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void basicOperations_File_Success() {
        SimpleConfiguration integrationConfiguration;
        Map<String, Object> values;
        IntegrationResponse response;

        // Create an empty file
        integrationConfiguration = getIntegrationConfiguration(pathCreate);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName1);
        values.put(Constants.SC_ATTR_FILE, Boolean.TRUE);
        values.put(Constants.SC_ATTR_OVERWRITE, Boolean.TRUE);
        values.put(Constants.SC_ATTR_MIME_TYPE, "text/csv;charset=utf8");
        setValues(integrationConfiguration, values);

        response = pathCreate.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("201"));

        // Check the file exists
        integrationConfiguration = getIntegrationConfiguration(pathGetProperties);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName1);
        setValues(integrationConfiguration, values);

        response = pathGetProperties.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(Boolean.parseBoolean(response.getResult().get("exists").toString()), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));

        // Upload some content to the file
        String data1 = "\"Column 1\"\n\"Example data\"\n";
        int data1Len = data1.getBytes(StandardCharsets.UTF_8).length;
        integrationConfiguration = getIntegrationConfiguration(pathUpdate);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName1);
        values.put(Constants.SC_ATTR_OVERWRITE, true);
        values.put(Constants.SC_ATTR_CONTENT, data1);
        setValues(integrationConfiguration, values);

        response = pathUpdate.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(Boolean.parseBoolean(response.getResult().get("exists").toString()), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));

        // Get the file properties and check it is the right length
        integrationConfiguration = getIntegrationConfiguration(pathGetProperties);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName1);
        setValues(integrationConfiguration, values);

        response = pathGetProperties.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(Boolean.parseBoolean(response.getResult().get("exists").toString()), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));
        assertThat(Integer.parseInt(response.getResult().get("length").toString()), equalTo(data1Len));

        // Append some more data to the file
        String data2 = "\"テストデータ\"\n";
        int data2Len = data2.getBytes(StandardCharsets.UTF_8).length;
        integrationConfiguration = getIntegrationConfiguration(pathUpdate);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName1);
        values.put(Constants.SC_ATTR_OVERWRITE, false);
        values.put(Constants.SC_ATTR_CONTENT, data2);
        setValues(integrationConfiguration, values);

        response = pathUpdate.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(Boolean.parseBoolean(response.getResult().get("exists").toString()), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));

        // Get the file properties and check it is the right length
        integrationConfiguration = getIntegrationConfiguration(pathGetProperties);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName1);
        setValues(integrationConfiguration, values);

        response = pathGetProperties.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(Boolean.parseBoolean(response.getResult().get("exists").toString()), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));
        assertThat(Integer.parseInt(response.getResult().get("length").toString()), equalTo(data1Len + data2Len));

        // Rename the file
        integrationConfiguration = getIntegrationConfiguration(pathRename);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_SOURCE_PATH, fileName1);
        values.put(Constants.SC_ATTR_DESTINATION_PATH, fileName2);
        setValues(integrationConfiguration, values);

        response = pathRename.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("201"));

        // Check and make sure the original does not exist
        integrationConfiguration = getIntegrationConfiguration(pathGetProperties);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName1);
        setValues(integrationConfiguration, values);

        response = pathGetProperties.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(Boolean.parseBoolean(response.getResult().get("exists").toString()), equalTo(false));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("404"));

        // Check the new file exists
        integrationConfiguration = getIntegrationConfiguration(pathGetProperties);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName2);
        setValues(integrationConfiguration, values);

        response = pathGetProperties.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(Boolean.parseBoolean(response.getResult().get("exists").toString()), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));
        assertThat(Integer.parseInt(response.getResult().get("length").toString()), equalTo(data1Len + data2Len));

        // Do a file list and confirm the old and new files
        integrationConfiguration = getIntegrationConfiguration(pathList);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName2);
        values.put(Constants.SC_ATTR_RECURSIVE, false);
        setValues(integrationConfiguration, values);

        response = pathList.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));
        assertThat(response.getResult().get("body"), hasJsonPath("$.paths[0].name", equalTo(fileName2)));

        // Read the contents of the new file and check the length
        integrationConfiguration = getIntegrationConfiguration(pathRead);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName2);
        setValues(integrationConfiguration, values);

        response = pathRead.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));
        assertThat(response.getResult().get("body").toString().length(), equalTo(data1.length() + data2.length()));

        // Delete the new file
        integrationConfiguration = getIntegrationConfiguration(pathDelete);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName2);
        setValues(integrationConfiguration, values);

        response = pathDelete.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));

        // Check the new file does not exist
        integrationConfiguration = getIntegrationConfiguration(pathGetProperties);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_PATH, fileName2);
        setValues(integrationConfiguration, values);

        response = pathGetProperties.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(Boolean.parseBoolean(response.getResult().get("exists").toString()), equalTo(false));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("404"));
    }

    // TODO: recursive list test
    // return body as base64 test
}
