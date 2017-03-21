package <%=packageName%>.grpc;


import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

@GRpcService
public class HealthService extends HealthServiceGrpc.HealthServiceImplBase {

    private final Map<String, org.springframework.boot.actuate.health.HealthIndicator> healthIndicators;

    private final org.springframework.boot.actuate.health.HealthIndicator healthIndicator;

    public HealthService(HealthAggregator healthAggregator, Map<String, org.springframework.boot.actuate.health.HealthIndicator> healthIndicators) {
        Assert.notNull(healthAggregator, "HealthAggregator must not be null");
        Assert.notNull(healthIndicators, "HealthIndicators must not be null");
        CompositeHealthIndicator healthIndicator = new CompositeHealthIndicator(
            healthAggregator);
        for (Map.Entry<String, org.springframework.boot.actuate.health.HealthIndicator> entry : healthIndicators.entrySet()) {
            healthIndicator.addHealthIndicator(getKey(entry.getKey()), entry.getValue());
        }
        this.healthIndicators = healthIndicators;
        this.healthIndicator = healthIndicator;
    }

    @Override
    public void getHealth(Empty request, StreamObserver<Health> responseObserver) {
        Map<String, HealthIndicator> healthIndicatorProtos = new HashMap<>();
        this.healthIndicators.forEach((key, indicator) -> healthIndicatorProtos.put(key, healthIndicatorToHealthIndicatorProto(indicator)));

        responseObserver.onNext(Health.newBuilder()
            .setStatus(Status.valueOf(this.healthIndicator.health().getStatus().toString()))
            .putAllHealthIndicators(healthIndicatorProtos)
            .build());
        responseObserver.onCompleted();
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
