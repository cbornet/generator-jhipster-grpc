package <%=packageName%>.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class EnvironmentService extends EnvironmentServiceGrpc.EnvironmentServiceImplBase {

    private final EnvironmentEndpoint endpoint;

    public EnvironmentService(EnvironmentEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void getEnv(Empty request, StreamObserver<Environment> responseObserver) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            responseObserver.onNext(Environment.newBuilder()
                .setValue(mapper.writeValueAsString(endpoint.invoke()))
                .build()
            );
        } catch (JsonProcessingException e) {
            throw new StatusRuntimeException(Status.INTERNAL.withCause(e));
        }
        responseObserver.onCompleted();
    }
}
