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
public class HealthServiceTest {

    @Autowired
    private HealthService serviceImpl;

    private Server mockServer;
    private HealthServiceGrpc.HealthServiceBlockingStub stub;

    @Before
    public void setUp() throws IOException {
        String uniqueServerName = "Mock server for " + HealthServiceGrpc.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(serviceImpl).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = HealthServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        mockServer.shutdownNow();
    }

    @Test
    public void testHealth() {
        stub.getHealth(Empty.newBuilder().build());
    }
}
