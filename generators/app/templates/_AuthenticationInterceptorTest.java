<%_
    var authClass;
    var authInstance;
    var authMethod;
    if (authenticationType === 'session') {
        authClass = 'AuthenticationManagerBuilder';
        authInstance = 'authenticationManagerBuilder';
        authMethod = 'authenticate';
        authMethodArgMatcher = 'anyObject';
        authScheme = 'Basic';
    } else if (authenticationType === 'jwt') {
        authClass = 'TokenProvider';
        authInstance = 'tokenProvider';
        authMethod = 'getAuthentication';
        authMethodArgMatcher = 'anyString';
        authScheme = 'Bearer';
    } else if (authenticationType === 'uaa') {
        authClass = 'TokenStore';
        authInstance = 'tokenStore';
        authMethod = 'readAuthentication';
        authMethodArgMatcher = 'anyString';
        authScheme = 'Bearer';
    } else if (authenticationType === 'oauth2') {
        authClass = 'ResourceServerTokenServices';
        authInstance = 'tokenServices';
        authMethod = 'loadAuthentication';
        authMethodArgMatcher = 'anyString';
        authScheme = 'Bearer';
    }

_%>

package <%= packageName %>.grpc;

<%_ if (authenticationType === 'session' && databaseType === 'cassandra') { _%>
import <%= packageName %>.AbstractCassandraTest;
<%_ } _%>
<%_ if (['uaa', 'jwt'].includes(authenticationType)) { _%>
import <%= packageName %>.security.AuthoritiesConstants;
<%_ } _%>
<%_ if (authenticationType === 'jwt') { _%>
import <%= packageName %>.security.jwt.TokenProvider;
<%_ } _%>

import com.google.protobuf.Empty;
<%_ if (authenticationType === 'jwt') { _%>
import io.github.jhipster.config.JHipsterProperties;
<%_ } _%>
import io.grpc.*;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
<%_ if (authenticationType === 'jwt') { _%>
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
<%_ } _%>
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
<%_ if (authenticationType === 'uaa') { _%>
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
<%_ } _%>
<%_ if (authenticationType === 'session') { _%>
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
<%_ } _%>
<%_ if (['uaa', 'jwt'].includes(authenticationType)) { _%>
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
<%_ } _%>
<%_ if (authenticationType === 'session') { _%>
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
<%_ } _%>
<%_ if (['uaa', 'jwt'].includes(authenticationType)) { _%>
import org.springframework.security.core.authority.SimpleGrantedAuthority;
<%_ } _%>
import org.springframework.security.core.context.SecurityContextHolder;
<%_ if (authenticationType === 'jwt') { _%>
import org.springframework.test.util.ReflectionTestUtils;
<%_ } _%>
<%_ if (authenticationType === 'oauth2' || authenticationType === 'uaa') { _%>
import org.springframework.security.oauth2.provider.OAuth2Authentication;
<%_ } _%>
<%_ if (authenticationType === 'uaa') { _%>
import org.springframework.security.oauth2.provider.token.TokenStore;
<%_ } _%>
<%_ if (authenticationType === 'oauth2') { _%>
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
<%_ } _%>

<%_ if (authenticationType === 'session') { _%>
import java.nio.charset.StandardCharsets;
import java.util.Base64;
<%_ } _%>
<%_ if (['uaa', 'jwt'].includes(authenticationType)) { _%>
import java.util.Collections;

<%_ } _%>
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
<%_ if (authenticationType === 'uaa') { _%>
import static org.mockito.Mockito.*;
<%_ } _%>

/**
 * Test class for the AuthenticationInterceptor gRPC interceptor class.
 *
 * @see AuthenticationInterceptor
 */
<%_ if (authenticationType === 'session') { _%>
@SpringBootTest
<%_ } _%>
public class AuthenticationInterceptorTest <% if (authenticationType === 'session' && databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    <%_ if (authenticationType === 'uaa') { _%>
    @Mock
    <%_ } _%>
    <%_ if (authenticationType === 'session') { _%>
    @Autowired
    <%_ } _%>
    private <%=authClass%> <%=authInstance%>;

    private Server fakeServer;

    private ManagedChannel inProcessChannel;

    @BeforeEach
    public void setUp() throws Exception {
        <%_ if (authenticationType === 'jwt') { _%>
        JHipsterProperties jHipsterProperties = new JHipsterProperties();
        tokenProvider = new TokenProvider(jHipsterProperties);
        ReflectionTestUtils.setField(tokenProvider, "key",
                Keys.hmacShaKeyFor(Decoders.BASE64
                        .decode("fd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8")));

        ReflectionTestUtils.setField(tokenProvider, "tokenValidityInMilliseconds", 60000);
        <%_ } _%>
        <%_ if (authenticationType === 'uaa') { _%>
        MockitoAnnotations.initMocks(this);
        doReturn(new OAuth2Authentication(null, new UsernamePasswordAuthenticationToken("user", "user",
            Collections.singletonList(new SimpleGrantedAuthority(AuthoritiesConstants.USER))))
        ).when(<%=authInstance%>).<%=authMethod%>(<%=authMethodArgMatcher%>());
        <%_ } _%>

        String uniqueServerName = "fake server for " + getClass();
        fakeServer = InProcessServerBuilder.forName(uniqueServerName)
            .addService(ServerInterceptors.intercept(new LoggersServiceGrpc.LoggersServiceImplBase() {}, new AuthenticationInterceptor(<%=authInstance%>)))
            .directExecutor()
            .build()
            .start();
        inProcessChannel = InProcessChannelBuilder.forName(uniqueServerName)
            .directExecutor()
            .build();
    }

    @AfterEach
    public void tearDown() {
        inProcessChannel.shutdownNow();
        fakeServer.shutdownNow();
    }

    @Test
    public void testIntercept() {
        <%_ if (authenticationType === 'jwt') { _%>
        String authToken = getJwt("user", AuthoritiesConstants.USER);
        <%_ } _%>
        <%_ if (authenticationType === 'session') { _%>
        String authToken = Base64.getEncoder().encodeToString("user:user".getBytes(StandardCharsets.UTF_8));
        <%_ } _%>
        <%_ if (authenticationType === 'uaa') { _%>
        String authToken = "test_access_token";
        <%_ } _%>
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "<%=authScheme%> " + authToken);
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNIMPLEMENTED);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user");
    }

    @Test
    public void testCapitalizedAuthorizationHeader() {
        <%_ if (authenticationType === 'jwt') { _%>
        String authToken = getJwt("user", AuthoritiesConstants.USER);
        <%_ } _%>
        <%_ if (authenticationType === 'session') { _%>
        String authToken = Base64.getEncoder().encodeToString("user:user".getBytes(StandardCharsets.UTF_8));
        <%_ } _%>
        <%_ if (authenticationType === 'uaa') { _%>
        String authToken = "test_access_token";
        <%_ } _%>
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "<%=authScheme%> " + authToken);
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNIMPLEMENTED);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user");
    }

    @Test
    public void testNoAuthorization() {
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = LoggersServiceGrpc.newBlockingStub(inProcessChannel);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }


    <%_ if (authenticationType !== 'session') { _%>
    @Test
    public void testAnonymousUserDenied() {
        <%_ if (authenticationType === 'jwt') { _%>
        String authToken = getJwt("anonymous", AuthoritiesConstants.ANONYMOUS);
        <%_ } _%>
        <%_ if (authenticationType === 'uaa') { _%>
        String authToken = "test_access_token";
        doReturn(new OAuth2Authentication(null, new UsernamePasswordAuthenticationToken("user", "user",
            Collections.singletonList(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS))))
        ).when(<%=authInstance%>).<%=authMethod%>(<%=authMethodArgMatcher%>());
        <%_ } _%>
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "<%=authScheme%> " + authToken);
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.PERMISSION_DENIED);
    }

    <%_ } _%>
    @Test
    public void testWrongScheme() {
        <%_ if (authenticationType === 'jwt') { _%>
        String authToken = getJwt("user", AuthoritiesConstants.USER);
        <%_ } _%>
        <%_ if (authenticationType === 'session') { _%>
        String authToken = Base64.getEncoder().encodeToString("user:user".getBytes(StandardCharsets.UTF_8));
        <%_ } _%>
        <%_ if (authenticationType === 'uaa') { _%>
        String authToken = "test_access_token";
        <%_ } _%>
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "<% if (['jwt', 'uaa'].includes(authenticationType)) { %>Basic<% } else { %>Bearer<% } %> " + authToken);
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }

    <%_ if (authenticationType === 'session') { _%>
    @Test
    public void testWrongUser() {
        String authToken = Base64.getEncoder().encodeToString("unknown:unknown".getBytes(StandardCharsets.UTF_8));
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "<%=authScheme%> " + authToken);
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }

    @Test
    public void testMalformedToken() {
        String authToken = "!" + Base64.getEncoder().encodeToString("user:user".getBytes(StandardCharsets.UTF_8));
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Basic " + authToken);
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }

    @Test
    public void testMissingColon() {
        String authToken = Base64.getEncoder().encodeToString("useruser".getBytes(StandardCharsets.UTF_8));
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Basic " + authToken);
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }
    <%_ } else { _%>
    @Test
    public void testInvalidToken() {
        <%_ if (authenticationType === 'uaa') { _%>
        doReturn(null).when(<%=authInstance%>).<%=authMethod%>(anyString());
        <%_ } _%>
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer user_token");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }
    <%_ } _%>

    <%_ if (authenticationType === 'jwt') { _%>
    private String getJwt(String user, String user2) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                "test-password",
                Collections.singletonList(new SimpleGrantedAuthority(user2))
        );
        return tokenProvider.createToken(authentication, false);
    }

    <%_ } _%>
    private static void assertGetLoggersReturnsCode(LoggersServiceGrpc.LoggersServiceBlockingStub stub, Status.Code code) {
        try {
            stub.getLoggers(Empty.getDefaultInstance()).forEachRemaining(l -> {});
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(code);
        }
    }

}
