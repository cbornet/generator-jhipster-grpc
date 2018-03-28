package <%= packageName %>.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.grpc.Status;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import reactor.core.publisher.Mono;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class ConfigurationPropertiesReportService extends ReactorConfigurationPropertiesReportServiceGrpc.ConfigurationPropertiesReportServiceImplBase {

    private final ConfigurationPropertiesReportEndpoint endpoint;

    public ConfigurationPropertiesReportService(ConfigurationPropertiesReportEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Mono<ApplicationConfigurationProperties> getConfigurationProperties(Mono<Empty> request) {
        return request.map(e -> mapApplicationConfigurationProperties(endpoint.configurationProperties()));
    }

    private ApplicationConfigurationProperties mapApplicationConfigurationProperties(ConfigurationPropertiesReportEndpoint.ApplicationConfigurationProperties props) {
        ApplicationConfigurationProperties.Builder builder = ApplicationConfigurationProperties.newBuilder();
        props.getContexts().forEach((k, v) -> builder.putContexts(k, mapContextConfigurationPropertiess(v)));
        return builder.build();
    }

    private ContextConfigurationProperties mapContextConfigurationPropertiess(ConfigurationPropertiesReportEndpoint.ContextConfigurationProperties props) {
        ContextConfigurationProperties.Builder builder = ContextConfigurationProperties.newBuilder();
        if (props.getParentId() != null) {
            builder.setParentId(props.getParentId());
        }
        props.getBeans().forEach((k, v) -> builder.putBeans(k, mapConfigurationPropertiesBeanDescriptor(v)));
        return builder.build();
    }

    private ConfigurationPropertiesBeanDescriptor mapConfigurationPropertiesBeanDescriptor(ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor descriptor) {
        ObjectMapper objectMapper = new ObjectMapper();
        ConfigurationPropertiesBeanDescriptor.Builder builder = ConfigurationPropertiesBeanDescriptor.newBuilder();
        if (descriptor.getPrefix() != null) {
            builder.setPrefix(descriptor.getPrefix());
        }
        descriptor.getProperties().forEach((k, v) -> {
            try {
                builder.putProperties(k, objectMapper.writeValueAsString(v));
            } catch (JsonProcessingException e) {
                throw Status.INTERNAL.withCause(e).asRuntimeException();
            }
        });
        return builder.build();
    }

}
