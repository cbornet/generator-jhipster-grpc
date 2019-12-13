package <%= packageName %>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%= packageName %>.AbstractCassandraTest;
<%_ } _%>
import <%= packageName %>.<%=mainClass%>;
import <%= packageName %>.domain.User;
import <%= packageName %>.repository.UserRepository;
import <%= packageName %>.security.jwt.TokenProvider;

import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

@SpringBootTest(classes = <%=mainClass%>.class)
public class JWTServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    private static final String USER_LOGIN = "user-jwt";
    private static final String USER_PASSWORD = "user-jwt-password";

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    private Server mockServer;

    private JWTServiceGrpc.JWTServiceBlockingStub stub;

    @BeforeEach
    public void setUp() throws IOException {
        JWTService service = new JWTService(tokenProvider, authenticationManagerBuilder);
        String uniqueServerName = "Mock server for " + JWTService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = JWTServiceGrpc.newBlockingStub(channelBuilder.build());

        userRepository.deleteAll();
        User user = new User();
        user.setLogin(USER_LOGIN);
        user.setPassword(encoder.encode(USER_PASSWORD));
        user.setActivated(true);
        userRepository.save(user);
    }

    @AfterEach
    public void tearDown() {
        mockServer.shutdownNow();
    }

    @Test
    public void testAuthenticate() {
        JWTToken token = stub.authenticate(Login.newBuilder().setUsername(USER_LOGIN).setPassword(USER_PASSWORD).build());
        assertThat(token.getIdToken()).isNotEmpty();
    }

    @Test
    public void tesAuthenticationRefused() {
        try {
            stub.authenticate(Login.newBuilder().setUsername(USER_LOGIN).setPassword("wrong-password").build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
        }
    }

}
