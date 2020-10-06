package uk.org.kano.appian.filesystem;

import com.appian.connectedsystems.simplified.sdk.configuration.SimpleConfiguration;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import org.junit.Test;
import uk.org.kano.appian.Constants;
import uk.org.kano.appian.TestBase;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ListTest extends TestBase {
    private List list = new List();

    @Test
    public void listShouldReturnSuccess() {
        SimpleConfiguration integrationConfiguration;
        Map<String, Object> values;
        IntegrationResponse response;

        integrationConfiguration = getIntegrationConfiguration(list);
        values = new HashMap<>();
        setValues(integrationConfiguration, values);

        response = list.execute(integrationConfiguration, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(response.getResult().get("body"), notNullValue());
    }
}
