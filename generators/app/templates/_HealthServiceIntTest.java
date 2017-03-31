package <%=packageName%>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%=packageName%>.AbstractCassandraTest;
<%_ } _%>
import <%=packageName%>.<%=mainClass%>;
<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
import <%=packageName%>.config.SecurityBeanOverrideConfiguration;
<%_ } _%>

import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = <% if (authenticationType === 'uaa' && applicationType !== 'uaa') { %>{<%= mainClass %>.class, SecurityBeanOverrideConfiguration.class}<% } else { %><%=mainClass%>.class<% } %>)
public class HealthServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    @Autowired
    HealthAggregator healthAggregator;

    @Autowired
    Map<String, org.springframework.boot.actuate.health.HealthIndicator> healthIndicators;

    private Server mockServer;

    private HealthServiceGrpc.HealthServiceBlockingStub stub;

    @Before
    public void setUp() throws IOException {
        HealthService service = new HealthService(healthAggregator, healthIndicators);
        String uniqueServerName = "Mock server for " + HealthService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
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
        Health health = stub.getHealth(Empty.newBuilder().build());
        assertThat(health.getStatus()).isNotEqualTo(Status.UNKNOWN);
    }
}
