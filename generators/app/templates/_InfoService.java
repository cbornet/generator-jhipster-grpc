package <%= packageName %>.grpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.grpc.Status;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.actuate.info.InfoEndpoint;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@GRpcService
public class InfoService extends ReactorInfoServiceGrpc.InfoServiceImplBase {

    private final InfoEndpoint infoEndpoint;

    private final ObjectMapper mapper;

    public InfoService(InfoEndpoint infoEndpoint, ObjectMapper mapper) {
        this.infoEndpoint = infoEndpoint;
        this.mapper = mapper;
    }

    @Override
    public Mono<Info> getInfo(Mono<Empty> request) {
        Map<String, String> details = new HashMap<>();
        infoEndpoint.info().forEach((k,v) -> {
            try {
                details.put(k, mapper.writeValueAsString(v));
            } catch (JsonProcessingException e) {
                throw Status.INTERNAL.withCause(e).asRuntimeException();
            }
        });
        return request.map(e -> Info.newBuilder().putAllDetails(details).build());
    }
}
