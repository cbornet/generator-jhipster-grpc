package <%= packageName %>.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.grpc.Status;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.stream.Collectors;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class EnvironmentService extends ReactorEnvironmentServiceGrpc.EnvironmentServiceImplBase {

    private final EnvironmentEndpoint endpoint;

    private final ObjectMapper mapper;

    public EnvironmentService(EnvironmentEndpoint endpoint, ObjectMapper mapper) {
        this.endpoint = endpoint;
        this.mapper = mapper;
    }

    @Override
    public Mono<Environment> getEnv(Mono<Empty> request) {
        return request.map( empty -> mapEnvironment(endpoint.environment(null)));
    }


    private Environment mapEnvironment(EnvironmentEndpoint.EnvironmentDescriptor environment) {
        return Environment.newBuilder()
            .addAllActiveProfiles(environment.getActiveProfiles())
            .addAllPropertySources(environment.getPropertySources().stream()
                .map(this::mapPropertySource)
                .collect(Collectors.toList())
            )
            .build();
    }


    private PropertySource mapPropertySource(EnvironmentEndpoint.PropertySourceDescriptor propertySource) {
        PropertySource.Builder builder = PropertySource.newBuilder()
            .setName(propertySource.getName());
        propertySource.getProperties().forEach((k, v) -> builder.putProperties(k, mapPropertyValue(v)));
        return builder.build();
    }

    private PropertyValue mapPropertyValue(EnvironmentEndpoint.PropertyValueDescriptor propertyValue) {
        try {
            return PropertyValue.newBuilder()
                .setOrigin(Optional.ofNullable(propertyValue.getOrigin()).orElse(""))
                .setValue(mapper.writeValueAsString(propertyValue.getValue())).build();
        } catch (JsonProcessingException e) {
            throw Status.INTERNAL.withCause(e).asRuntimeException();
        }
    }
}
