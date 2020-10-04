package uk.org.kano.appian.filesystem;

import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import org.junit.Test;
import uk.org.kano.appian.TestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class GetPropertiesTest extends TestBase {
    private GetProperties getProperties = new GetProperties();

    @Test
    public void getPropertiesShouldReturnSuccess() {
        IntegrationResponse response = getProperties.execute(null, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
    }
}
