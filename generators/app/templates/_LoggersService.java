package <%= packageName %>.grpc;

import com.google.protobuf.Empty;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class LoggersService extends RxLoggersServiceGrpc.LoggersServiceImplBase {

    private final LoggingSystem loggingSystem;

    public LoggersService(LoggingSystem loggingSystem) {
        Assert.notNull(loggingSystem, "LoggingSystem must not be null");
        this.loggingSystem = loggingSystem;
    }

    @Override
    public Flowable<Logger> getLoggers(Single<Empty> request) {
        return request
            .map(e ->  loggingSystem.getLoggerConfigurations())
            .flatMapPublisher(Flowable::fromIterable)
            .map(loggerConfiguration ->
                Logger.newBuilder()
                    .setName(loggerConfiguration.getName())
                    .setLevel(loggerConfiguration.getEffectiveLevel() == null ? Level.UNDEFINED : Level.valueOf(loggerConfiguration.getEffectiveLevel().name()))
                    .build()
            );
    }

    @Override
    public Single<Empty> changeLevel(Single<Logger> request) {
        return request
            .doOnSuccess(logger -> this.loggingSystem.setLogLevel(
                logger.getName(),
                Level.UNDEFINED == logger.getLevel() ? null : LogLevel.valueOf(logger.getLevel().toString()))
            )
            .map(l -> Empty.newBuilder().build());
    }

}
