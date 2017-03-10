package <%=packageName%>.grpc;

import ch.qos.logback.classic.Level;
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
                    .setLevel(logger.getLevel() == null ? "" : logger.getLevel().toString())
                    .build()
        ));
        responseObserver.onCompleted();
    }

    @Override
    public void changeLevel(Logger logger, StreamObserver<Empty> responseObserver) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(logger.getName()).setLevel(Level.valueOf(logger.getLevel()));
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

}
