package <%=packageName%>.grpc;

import ch.qos.logback.classic.LoggerContext;
import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;

@GRpcService
public class LogsService extends LogsServiceGrpc.LogsServiceImplBase {

    @Override
    public void getLoggers(Empty request, StreamObserver<Logger> responseObserver) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLoggerList().forEach(logger -> responseObserver.onNext(
                Logger.newBuilder()
                    .setName(logger.getName())
                    .setLevel(logger.getLevel() == null ? Level.UNDEFINED : Level.valueOf(logger.getLevel().levelStr))
                    .build()
        ));
        responseObserver.onCompleted();
    }

    @Override
    public void changeLevel(Logger logger, StreamObserver<Empty> responseObserver) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        if (Level.UNDEFINED == logger.getLevel()) {
            context.getLogger(logger.getName()).setLevel(null);
        } else {
            context.getLogger(logger.getName()).setLevel(ch.qos.logback.classic.Level.valueOf(logger.getLevel().toString()));
        }
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

}
