package <%= packageName %>.grpc;

import com.google.protobuf.Empty;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class MetricService extends RxMetricServiceGrpc.MetricServiceImplBase {

    private final List<PublicMetrics> publicMetrics;

    /**
     * Create a new {@link MetricService} instance.
     * @param publicMetrics the metrics to expose. The collection will be sorted using the
     * {@link AnnotationAwareOrderComparator}.
     */
    public MetricService(Collection<PublicMetrics> publicMetrics) {
        Assert.notNull(publicMetrics, "PublicMetrics must not be null");
        this.publicMetrics = new ArrayList<>(publicMetrics);
        AnnotationAwareOrderComparator.sort(this.publicMetrics);
    }

    @Override
    public Flowable<Metric> getMetrics(Single<Empty> request) {
        return request
            .map(empty -> publicMetrics)
            .flatMapPublisher(Flowable::fromIterable)
            .map(PublicMetrics::metrics)
            .flatMap(Flowable::fromIterable)
            .map(metric -> {
                Metric.Builder builder = Metric.newBuilder()
                    .setName(metric.getName());
                if (metric.getTimestamp() != null) {
                    builder.setTimestamp(ProtobufMappers.dateToTimestamp(metric.getTimestamp()));
                }
                if (metric.getValue() instanceof Long || metric.getValue() instanceof Integer) {
                    builder.setLongValue(metric.getValue().longValue());
                } else if (metric.getValue() instanceof Float || metric.getValue() instanceof Double) {
                    builder.setDoubleValue((metric.getValue()).doubleValue());
                } else {
                    builder.setStringValue(metric.getValue().toString());
                }
                return builder.build();
            });
    }

}
