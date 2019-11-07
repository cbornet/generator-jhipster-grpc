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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.context.properties.ConfigurationPropertiesReportEndpoint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
@SpringBootTest(classes = {SecurityBeanOverrideConfiguration.class, <%= mainClass %>.class})
<%_ } else { _%>
@SpringBootTest(classes = <%= mainClass %>.class)
<%_ } _%>
public class ConfigurationPropertiesReportServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    @Autowired
    private ConfigurationPropertiesReportEndpoint configurationPropertiesReportEndpoint;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper mapper;

    @Value("${spring.mail.host}")
    private String mailHost;

    private Server mockServer;

    private ConfigurationPropertiesReportServiceGrpc.ConfigurationPropertiesReportServiceBlockingStub stub;

    @BeforeEach
    public void setUp() throws IOException {
        ConfigurationPropertiesReportService service = new ConfigurationPropertiesReportService(configurationPropertiesReportEndpoint, mapper);
        String uniqueServerName = "Mock server for " + ConfigurationPropertiesReportService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = ConfigurationPropertiesReportServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @AfterEach
    public void tearDown() {
        mockServer.shutdownNow();
    }

    @Test
    public void getConfigurationProperties() {
        ConfigurationPropertiesBean beans = stub.getConfigurationProperties(Empty.newBuilder().build())
            .getContextsOrThrow(applicationContext.getId())
            .getBeansOrThrow("spring.mail-org.springframework.boot.autoconfigure.mail.MailProperties");
        assertThat(beans.getPrefix()).isEqualTo("spring.mail");
        assertThat(beans.getPropertiesMap()).containsKey("host");
        assertThat(beans.getPropertiesMap().get("host")).isEqualTo(String.format("\"%s\"", mailHost));
    }

}
