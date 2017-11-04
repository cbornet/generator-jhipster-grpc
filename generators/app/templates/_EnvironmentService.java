package <%= packageName %>.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.reactivex.Single;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class EnvironmentService extends RxEnvironmentServiceGrpc.EnvironmentServiceImplBase {

    private final EnvironmentEndpoint endpoint;

    public EnvironmentService(EnvironmentEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Single<Environment> getEnv(Single<Empty> request) {
        return request.map( empty -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return Environment.newBuilder()
                    .setValue(mapper.writeValueAsString(endpoint.invoke()))
                    .build();
            } catch (JsonProcessingException e) {
                throw Status.INTERNAL.withCause(e).asRuntimeException();
            }
        });
    }
}
