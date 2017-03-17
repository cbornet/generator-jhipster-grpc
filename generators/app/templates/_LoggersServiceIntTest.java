package <%=packageName%>.grpc;

import <%=packageName%>.<%=mainClass%>;

import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JwtApp.class)
public class LoggersServiceIntTest {

    @Autowired
    private LoggingSystem loggingSystem;

    private Server mockServer;

    private LoggersServiceGrpc.LoggersServiceBlockingStub stub;

    @Before
    public void setUp() throws IOException {
        LoggersService service = new LoggersService(loggingSystem);
        String uniqueServerName = "Mock server for " + LoggersService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = LoggersServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        mockServer.shutdownNow();
    }

    @Test
    public void getAllLogs() {
        stub.getLoggers(Empty.newBuilder().build());
    }

    @Test
    public void changeLevel() {
        stub.changeLevel(Logger.newBuilder().setName("ROOT").setLevel(Level.INFO).build());
    }

}
