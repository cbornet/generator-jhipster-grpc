package <%= packageName %>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%= packageName %>.AbstractCassandraTest;
<%_ } _%>
import <%= packageName %>.<%=mainClass%>;
<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
import <%= packageName %>.config.SecurityBeanOverrideConfiguration;
<%_ } _%>

import com.codahale.metrics.MetricRegistry;
import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
@SpringBootTest(classes = {SecurityBeanOverrideConfiguration.class, <%= mainClass %>.class})
<%_ } else { _%>
@SpringBootTest(classes = <%= mainClass %>.class)
<%_ } _%>
public class MetricServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    @Autowired
    private MetricRegistry registry;

    private Server mockServer;

    private MetricServiceGrpc.MetricServiceBlockingStub stub;

    @BeforeEach
    public void setUp() throws IOException {
        MetricService service = new MetricService(registry);
        String uniqueServerName = "Mock server for " + MetricService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = MetricServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @AfterEach
    public void tearDown() {
        mockServer.shutdownNow();
    }

    @Test
    public void testGetMetrics() {
        assertThat(stub.getMetrics(Empty.newBuilder().build()).getValue()).isNotEmpty();
    }

}
