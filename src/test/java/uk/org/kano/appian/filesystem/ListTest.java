package uk.org.kano.appian.filesystem;

import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import org.junit.Test;
import uk.org.kano.appian.TestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class ListTest extends TestBase {
    private List list = new List();

    @Test
    public void listShouldReturnSuccess() {
        IntegrationResponse response = list.execute(null, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
        assertThat(response.getResult().get("body"), notNullValue());
    }
}
