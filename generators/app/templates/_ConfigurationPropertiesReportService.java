package <%=packageName%>.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.reactivex.Single;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.ConfigurationPropertiesReportEndpoint;

import java.util.Map;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class ConfigurationPropertiesReportService extends RxConfigurationPropertiesReportServiceGrpc.ConfigurationPropertiesReportServiceImplBase {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(ConfigurationPropertiesReportService.class);

    private final ConfigurationPropertiesReportEndpoint endpoint;

    public ConfigurationPropertiesReportService(ConfigurationPropertiesReportEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Single<ConfigurationPropertiesReport> getConfigurationProperties(Single<Empty> request) {
        return request.map(e -> mapToConfigurationPropertiesReport(endpoint.invoke()));
    }

    private ConfigurationPropertiesReport mapToConfigurationPropertiesReport(Map<String, Object> map) {
        ConfigurationPropertiesReport.Builder builder = ConfigurationPropertiesReport.newBuilder();
        ObjectMapper mapper = new ObjectMapper();
        map.forEach((k, v) -> {
            Map<String, Object> mapValue = (Map<String, Object>) v;
            if ("parent".equals(k)) {
                builder.setParent(mapToConfigurationPropertiesReport(mapValue));
            } else {
                String properties = "";
                try {
                    properties = mapper.writeValueAsString(mapValue.get("properties"));
                } catch (JsonProcessingException e) {
                    log.error("Couldn't encode properties as JSON", e);
                }
                builder.putConfigurationProperties(k,
                    ConfigurationProperty.newBuilder()
                        .setPrefix(mapValue.get("prefix").toString())
                        .setProperties(properties)
                        .build()
                );
            }
        });
        return builder.build();
    }

}
