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

package uk.org.kano.appian.path;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.org.kano.appian.Constants;
import uk.org.kano.appian.TestBase;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class FileOperationsTest extends TestBase {
    private PathCreate pathCreate = new PathCreate();
    private PathDelete pathDelete = new PathDelete();
    private PathGetProperties pathGetProperties = new PathGetProperties();
    private PathUpdate pathUpdate = new PathUpdate();
    private PathRename pathRename = new PathRename();
    private PathList pathList = new PathList();
    private PathRead pathRead = new PathRead();

    // Files, will assume in the root directory.
    private String fileName1 = "example1.csv";
    private String fileName2 = "example2.csv";

    @Before
    @After
    public void clean() {
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
    public void basicOperations_Success() {
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

        // Do a file list and confirm the new file is there
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
        assertThat(response.getResult().get("body").toString(), equalTo(data1 + data2));

        // Read the contents as a base64 string
        integrationConfiguration = getIntegrationConfiguration(pathRead);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_BASE64_BODY, true);
        values.put(Constants.SC_ATTR_PATH, fileName2);
        setValues(integrationConfiguration, values);

        response = pathRead.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));
        String base64Body = response.getResult().get("body").toString();
        assertThat(new String(Base64.getDecoder().decode(base64Body), StandardCharsets.UTF_8), equalTo(data1 + data2));

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

    @Test
    @SuppressWarnings("rawtypes")
    public void recursiveList_Success() {
        SimpleConfiguration integrationConfiguration;
        Map<String, Object> values;
        IntegrationResponse response;

        integrationConfiguration = getIntegrationConfiguration(pathList);
        values = new HashMap<>();
        values.put(Constants.SC_ATTR_RECURSIVE, true);
        setValues(integrationConfiguration, values);

        response = pathList.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(((Map)response.getIntegrationDesignerDiagnostic().getData().get("response")).get("responseCode"), equalTo("200"));
        assertThat(response.getResult().get("body"), isJson(
                allOf(
                        hasJsonPath("$.paths[*].name", hasSize(greaterThan(0)))
                )
                )
        );
    }
}
