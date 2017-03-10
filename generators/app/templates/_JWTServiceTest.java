package <%=packageName%>.grpc;

import <%=packageName%>.<%=mainClass%>;
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

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = <%=mainClass%>.class)
public class JWTServiceTest {

    @Autowired
    private JWTService serviceImpl;

    private Server mockServer;
    private JWTServiceGrpc.JWTServiceBlockingStub stub;

    @Before
    public void setUp() throws Exception {
        String uniqueServerName = "Mock server for " + JWTServiceGrpc.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(serviceImpl).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = JWTServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() throws Exception {
        mockServer.shutdownNow();
    }

    @Test
    public void testAuthenticate() {
        JWTToken token = stub.authenticate(Login.newBuilder().setUsername("user").setPassword("user").build());
        assertThat(token.getIdToken()).isNotEmpty();
    }

}
