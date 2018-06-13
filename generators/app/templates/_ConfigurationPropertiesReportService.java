package <%= packageName %>.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.grpc.Status;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class ConfigurationPropertiesReportService extends ReactorConfigurationPropertiesReportServiceGrpc.ConfigurationPropertiesReportServiceImplBase {

    private final ConfigurationPropertiesReportEndpoint endpoint;

    private final ObjectMapper mapper;

    public ConfigurationPropertiesReportService(ConfigurationPropertiesReportEndpoint endpoint, ObjectMapper mapper) {
        this.endpoint = endpoint;
        this.mapper = mapper;
    }

    @Override
    public Mono<ApplicationConfigurationProperties> getConfigurationProperties(Mono<Empty> request) {
        return request.map(e -> mapApplicationConfigurationProperties(endpoint.configurationProperties()));
    }

    private ApplicationConfigurationProperties mapApplicationConfigurationProperties(ConfigurationPropertiesReportEndpoint.ApplicationConfigurationProperties props) {
        return ApplicationConfigurationProperties.newBuilder()
            .putAllContexts(props.getContexts().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> mapContextConfigurationProperties(e.getValue()))
                )
            )
            .build();
    }

    private ContextConfigurationProperties mapContextConfigurationProperties(ConfigurationPropertiesReportEndpoint.ContextConfigurationProperties props) {
        ContextConfigurationProperties.Builder builder = ContextConfigurationProperties.newBuilder();
        Optional.ofNullable(props.getParentId()).ifPresent(builder::setParentId);
        props.getBeans().forEach((k, v) -> builder.putBeans(k, mapConfigurationPropertiesBean(v)));
        return builder.build();
    }

    private ConfigurationPropertiesBean mapConfigurationPropertiesBean(ConfigurationPropertiesReportEndpoint.ConfigurationPropertiesBeanDescriptor descriptor) {
        ConfigurationPropertiesBean.Builder builder = ConfigurationPropertiesBean.newBuilder();
        Optional.ofNullable(descriptor.getPrefix()).ifPresent(builder::setPrefix);
        descriptor.getProperties().forEach((k, v) -> {
            try {
                builder.putProperties(k, mapper.writeValueAsString(v));
            } catch (JsonProcessingException e) {
                throw Status.INTERNAL.withCause(e).asRuntimeException();
            }
        });
        return builder.build();
    }

}
