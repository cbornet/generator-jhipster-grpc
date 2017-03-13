package <%=packageName%>.grpc;


import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

@GRpcService
public class HealthService extends HealthServiceGrpc.HealthServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(HealthService.class);

    private final Map<String, HealthIndicator> healthIndicators;

    public HealthService(Map<String, HealthIndicator> healthIndicators) {
        Assert.notNull(healthIndicators, "HealthIndicators must not be null");
        this.healthIndicators = healthIndicators;
    }

    public void getHealth(Empty request, StreamObserver<Health> responseObserver) {
        log.info(this.healthIndicators.toString());
        healthIndicators.forEach((key, indicator) -> {
            final Map<String, String> details = new HashMap<>();
            indicator.health().getDetails().forEach( (detailKey, detailValue) ->
                details.put(detailKey, detailValue.toString())
            );
            responseObserver.onNext(
                    Health.newBuilder()
                        .setStatus(Status.valueOf(indicator.health().getStatus().toString()))
                        .putAllDetails(details)
                        .build()
            );
        });
        responseObserver.onCompleted();
    }
}
