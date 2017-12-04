package <%= packageName %>.grpc;

import com.google.protobuf.Empty;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class LoggersService extends ReactorLoggersServiceGrpc.LoggersServiceImplBase {

    private final LoggingSystem loggingSystem;

    public LoggersService(LoggingSystem loggingSystem) {
        Assert.notNull(loggingSystem, "LoggingSystem must not be null");
        this.loggingSystem = loggingSystem;
    }

    @Override
    public Flux<Logger> getLoggers(Mono<Empty> request) {
        return request
            .flatMapIterable(e ->  loggingSystem.getLoggerConfigurations())
            .map(loggerConfiguration ->
                Logger.newBuilder()
                    .setName(loggerConfiguration.getName())
                    .setLevel(loggerConfiguration.getEffectiveLevel() == null ? Level.UNDEFINED : Level.valueOf(loggerConfiguration.getEffectiveLevel().name()))
                    .build()
            );
    }

    @Override
    public Mono<Empty> changeLevel(Mono<Logger> request) {
        return request
            .doOnSuccess(logger -> this.loggingSystem.setLogLevel(
                logger.getName(),
                Level.UNDEFINED == logger.getLevel() ? null : LogLevel.valueOf(logger.getLevel().toString()))
            )
            .map(l -> Empty.newBuilder().build());
    }

}
