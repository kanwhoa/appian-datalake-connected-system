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
    public static Map<String, Object> getDiagnosticMap(String... kv) {
        HashMap<String, Object> rv = new HashMap<>();

        if (kv.length % 2 != 0) return rv;
        for (int i=0; i<kv.length; i+=2) {
            rv.put(kv[i], kv[i+1]);
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
