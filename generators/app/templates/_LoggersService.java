package <%=packageName%>.grpc;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.util.Assert;

import java.util.Collection;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class LoggersService extends LoggersServiceGrpc.LoggersServiceImplBase {

    private final LoggingSystem loggingSystem;

    public LoggersService(LoggingSystem loggingSystem) {
        Assert.notNull(loggingSystem, "LoggingSystem must not be null");
        this.loggingSystem = loggingSystem;
    }

    @Override
    public void getLoggers(Empty request, StreamObserver<Logger> responseObserver) {
        Collection<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
        if (configurations != null) {
            configurations.forEach(loggerConfiguration -> responseObserver.onNext(
                Logger.newBuilder()
                    .setName(loggerConfiguration.getName())
                    .setLevel(loggerConfiguration.getEffectiveLevel() == null ? Level.UNDEFINED : Level.valueOf(loggerConfiguration.getEffectiveLevel().name()))
                    .build()
            ));
        }
        responseObserver.onCompleted();
    }

    @Override
    public void changeLevel(Logger logger, StreamObserver<Empty> responseObserver) {
        this.loggingSystem.setLogLevel(
            logger.getName(),
            Level.UNDEFINED == logger.getLevel() ? null : LogLevel.valueOf(logger.getLevel().toString())
        );
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

}
