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

package uk.org.kano.appian.filesystem;

import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import org.junit.Test;
import uk.org.kano.appian.TestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class FileSystemPathGetPropertiesTest extends TestBase {
    private FileSystemGetProperties fileSystemGetProperties = new FileSystemGetProperties();

    @Test
    public void getPropertiesShouldReturnSuccess() {
        IntegrationResponse response = fileSystemGetProperties.execute(null, connectedSystemConfiguration, null);
        assertThat(response.isSuccess(), equalTo(true));
    }
}
