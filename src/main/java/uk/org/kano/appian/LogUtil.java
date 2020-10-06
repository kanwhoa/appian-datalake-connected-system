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

package uk.org.kano.appian;

import com.appian.connectedsystems.templateframework.sdk.IntegrationError;
import com.appian.connectedsystems.templateframework.sdk.IntegrationResponse;
import com.appian.connectedsystems.templateframework.sdk.diagnostics.IntegrationDesignerDiagnostic;

import java.util.HashMap;
import java.util.Map;

public class LogUtil {

    public static IntegrationResponse createError(String title, String detail) {
        IntegrationError error = IntegrationError.builder()
                .title(title)
                .message(detail)
                .build();
        return IntegrationResponse.forError(error).build();
    }

    /**
     * Varargs to map
     * @param kv
     * @return
     */
    public static Map<String, Object> getIntegrationDataMap(Object... kv) {
        HashMap<String, Object> rv = new HashMap<>();

        if (kv.length % 2 != 0) return rv;
        for (int i=0; i<kv.length; i+=2) {
            rv.put(kv[i].toString(), kv[i+1]);
        }
        return rv;
    }

    /**
     * Merge two diagnostic records
     * @param first
     * @param second
     * @return
     */
    public static IntegrationDesignerDiagnostic mergeDiagnostic(IntegrationDesignerDiagnostic first, IntegrationDesignerDiagnostic second) {
        second.getData().forEach( (key, value) -> first.getData().put(key, value));
        return first;
    }
}
