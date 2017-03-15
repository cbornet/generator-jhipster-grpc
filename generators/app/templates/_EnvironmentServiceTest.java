package <%=packageName%>.grpc;

import <%=packageName%>.<%=mainClass%>;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JwtApp.class)
public class EnvironmentServiceTest {

    @Autowired
    private EnvironmentService serviceImpl;

    private Server mockServer;
    private EnvironmentServiceGrpc.EnvironmentServiceBlockingStub stub;

    @Before
    public void setUp() throws IOException {
        String uniqueServerName = "Mock server for " + EnvironmentServiceGrpc.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(serviceImpl).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = EnvironmentServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        mockServer.shutdownNow();
    }

    @Test
    public void testEnvironment() throws IOException {
        Environment Environment = stub.getEnv(Empty.newBuilder().build());
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<HashMap<String,Object>>() {};
        // String value should represent a Json map
        HashMap<String,Object> env = mapper.readValue(Environment.getValue(), typeRef);
        assertThat(!env.isEmpty());
    }
}
