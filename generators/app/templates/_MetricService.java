package <%= packageName %>.grpc;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Status;
import org.lognet.springboot.grpc.GRpcService;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;


@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class MetricService extends ReactorMetricServiceGrpc.MetricServiceImplBase {

    private final ObjectMapper mapper;

    private final MetricRegistry registry;

    public MetricService(MetricRegistry registry) {
        this.registry = registry;
        this.mapper = (new ObjectMapper()).registerModule(
            new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false, MetricFilter.ALL)
        );
    }

    @Override
    public Mono<StringValue> getMetrics(Mono<Empty> request) {
        return request
            .map(empty -> {
                try {
                    return StringValue.newBuilder().setValue(mapper.writeValueAsString(registry)).build();
                } catch (JsonProcessingException e) {
                    throw Status.INTERNAL.withCause(e).asRuntimeException();
                }
            });

    }

}
