package <%=packageName%>.grpc;

import <%=packageName%>.<%=mainClass%>;

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
import org.springframework.boot.actuate.endpoint.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = JwtApp.class)
public class ConfigurationPropertiesReportServiceTest {

    @Autowired
    private ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint;

    private Server mockServer;

    private ConfigurationPropertiesReportServiceGrpc.ConfigurationPropertiesReportServiceBlockingStub stub;

    @Before
    public void setUp() throws IOException {
        ConfigurationPropertiesReportService service = new ConfigurationPropertiesReportService(configurationPropertiesReportEndpoint);
        String uniqueServerName = "Mock server for " + ConfigurationPropertiesReportService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = ConfigurationPropertiesReportServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        mockServer.shutdownNow();
    }

    @Test
    public void getConfigurationProperties() throws IOException {
        ConfigurationPropertiesReport report = stub.getConfigurationProperties(Empty.newBuilder().build());
        String configurationPropertiesReportEndpointStr = report.getConfigurationPropertiesMap().get("configurationPropertiesReportEndpoint").getProperties();
        ObjectMapper mapper = new ObjectMapper();
        ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint = mapper.readValue(configurationPropertiesReportEndpointStr, ConfigurationPropertiesReportEndpoint.class);
        assertThat(configurationPropertiesReportEndpoint.getId()).isEqualTo(this.configurationPropertiesReportEndpoint.getId());
    }

}
