package <%= packageName %>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%= packageName %>.AbstractCassandraTest;
<%_ } _%>
import <%= packageName %>.<%=mainClass%>;
<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
import <%= packageName %>.config.SecurityBeanOverrideConfiguration;
<%_ } _%>

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
@SpringBootTest(classes = {SecurityBeanOverrideConfiguration.class, <%= mainClass %>.class})
<%_ } else { _%>
@SpringBootTest(classes = <%= mainClass %>.class)
<%_ } _%>
public class EnvironmentServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    @Autowired
    private EnvironmentEndpoint endpoint;

    @Autowired
    private ObjectMapper mapper;

    private Server mockServer;

    private EnvironmentServiceGrpc.EnvironmentServiceBlockingStub stub;

    @BeforeEach
    public void setUp() throws IOException {
        EnvironmentService service = new EnvironmentService(endpoint, mapper);
        String uniqueServerName = "Mock server for " + EnvironmentService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = EnvironmentServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @AfterEach
    public void tearDown() {
        mockServer.shutdownNow();
    }

    @Test
    public void testEnvironment() {
        Environment environment = stub.getEnv(Empty.newBuilder().build());
        Optional<Map<String, PropertyValue>> propertyValueMap = environment.getPropertySourcesList().stream()
            .filter(p -> "Inlined Test Properties".equals(p.getName()))
            .map(PropertySource::getPropertiesMap)
            .findFirst();
        assertThat(propertyValueMap).isPresent();
        assertThat(propertyValueMap.get()).hasEntrySatisfying(
            "org.springframework.boot.test.context.SpringBootTestContextBootstrapper",
            pv -> assertThat(pv.getValue()).isEqualTo("\"true\""));
    }
}
