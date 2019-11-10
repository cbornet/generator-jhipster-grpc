package <%= packageName %>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%= packageName %>.AbstractCassandraTest;
<%_ } _%>
import <%= packageName %>.<%=mainClass%>;
<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
import <%= packageName %>.config.SecurityBeanOverrideConfiguration;
<%_ } _%>

import com.google.protobuf.Empty;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
<%_ if (messageBroker === 'kafka') { _%>
import org.springframework.kafka.test.context.EmbeddedKafka;
<%_ } _%>

import java.io.IOException;

<%_ if (messageBroker === 'kafka') { _%>
@EmbeddedKafka
<%_ } _%>
<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
@SpringBootTest(classes = {SecurityBeanOverrideConfiguration.class, <%= mainClass %>.class})
<%_ } else { _%>
@SpringBootTest(classes = <%= mainClass %>.class)
<%_ } _%>
public class LoggersServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    @Autowired
    private LoggingSystem loggingSystem;

    private Server mockServer;

    private LoggersServiceGrpc.LoggersServiceBlockingStub stub;

    @BeforeEach
    public void setUp() throws IOException {
        LoggersService service = new LoggersService(loggingSystem);
        String uniqueServerName = "Mock server for " + LoggersService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = LoggersServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @AfterEach
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
