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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JwtApp.class)
public class LogsServiceTest {

    @Autowired
    private LogsService logsService;

    private Server mockServer;
    private LogsServiceGrpc.LogsServiceBlockingStub stub;

    @Before
    public void setUp() throws IOException {
        String uniqueServerName = "Mock server for " + LogsService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(logsService).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = LogsServiceGrpc.newBlockingStub(channelBuilder.build());
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
