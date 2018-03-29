package <%= packageName %>.grpc;

import com.google.protobuf.Empty;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@GRpcService
public class HealthService extends ReactorHealthServiceGrpc.HealthServiceImplBase {

    private final Map<String, org.springframework.boot.actuate.health.HealthIndicator> healthIndicators;

    private final org.springframework.boot.actuate.health.HealthIndicator healthIndicator;

    public HealthService(HealthAggregator healthAggregator, Map<String, org.springframework.boot.actuate.health.HealthIndicator> healthIndicators) {
        Assert.notNull(healthAggregator, "HealthAggregator must not be null");
        Assert.notNull(healthIndicators, "HealthIndicators must not be null");
        CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(healthAggregator);
        healthIndicators.forEach((key, value) -> healthIndicator.addHealthIndicator(getKey(key), value));
        this.healthIndicators = healthIndicators;
        this.healthIndicator = healthIndicator;
    }

    @Override
    public Mono<Health> getHealth(Mono<Empty> request) {
        Map<String, HealthIndicator> healthIndicatorProtos = new HashMap<>();
        this.healthIndicators.forEach((key, indicator) -> healthIndicatorProtos.put(key, healthIndicatorToHealthIndicatorProto(indicator)));

        return request.map( e ->
            Health.newBuilder()
                .setStatus(Status.valueOf(this.healthIndicator.health().getStatus().toString()))
                .putAllHealthIndicators(healthIndicatorProtos)
                .build()
        );
    }

    public HealthIndicator healthIndicatorToHealthIndicatorProto(org.springframework.boot.actuate.health.HealthIndicator healthIndicator) {
        final Map<String, String> details = new HashMap<>();
        healthIndicator.health().getDetails().forEach( (detailKey, detailValue) ->
            details.put(detailKey, detailValue.toString())
        );
        return HealthIndicator.newBuilder()
            .setStatus(Status.valueOf(healthIndicator.health().getStatus().toString()))
            .putAllDetails(details)
            .build();
    }

    private String getKey(String name) {
        int index = name.toLowerCase().indexOf("healthindicator");
        if (index > 0) {
            return name.substring(0, index);
        }
        return name;
    }

}
