package <%=packageName%>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%=packageName%>.AbstractCassandraTest;
<%_ } _%>
import <%=packageName%>.<%=mainClass%>;
<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
import <%=packageName%>.config.SecurityBeanOverrideConfiguration;
<%_ } _%>

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
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = <% if (authenticationType === 'uaa' && applicationType !== 'uaa') { %>{<%= mainClass %>.class, SecurityBeanOverrideConfiguration.class}<% } else { %><%=mainClass%>.class<% } %>)
public class EnvironmentServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    @Autowired
    private EnvironmentEndpoint endpoint;

    private Server mockServer;

    private EnvironmentServiceGrpc.EnvironmentServiceBlockingStub stub;

    @Before
    public void setUp() throws IOException {
        EnvironmentService service = new EnvironmentService(endpoint);
        String uniqueServerName = "Mock server for " + EnvironmentService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
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
        assertThat(env).isNotEmpty();
    }
}
