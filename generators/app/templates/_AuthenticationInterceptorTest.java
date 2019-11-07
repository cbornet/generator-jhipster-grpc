<%_
    var authClass;
    var authInstance;
    var authMethod;
    if (authenticationType === 'session') {
        authClass = 'AuthenticationManager';
        authInstance = 'authenticationManager';
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

import <%= packageName %>.security.AuthoritiesConstants;<% if (authenticationType === 'jwt') { %>
import <%= packageName %>.security.jwt.TokenProvider;<% } %>

import com.google.protobuf.Empty;
import io.grpc.*;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
<%_ if (authenticationType === 'session') { _%>
import org.mockito.ArgumentCaptor;
<%_ } _%>
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;<% if (authenticationType === 'session') { %>
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;<% } %>
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;<% if (authenticationType === 'session') { %>
import org.springframework.security.core.Authentication;<% } %>
import org.springframework.security.core.authority.SimpleGrantedAuthority;<% if (authenticationType === 'oauth2' || authenticationType === 'uaa') { %>
import org.springframework.security.oauth2.provider.OAuth2Authentication;<% } %><% if (authenticationType === 'uaa') { %>
import org.springframework.security.oauth2.provider.token.TokenStore;<% } %><% if (authenticationType === 'oauth2') { %>
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;<% } %>

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;<% if (authenticationType === 'session') { %>
import static org.mockito.Matchers.anyObject;<% } %>
import static org.mockito.Mockito.*;

/**
 * Test class for the AuthenticationInterceptor gRPC interceptor class.
 *
 * @see AuthenticationInterceptor
 */
public class AuthenticationInterceptorTest {

    @Mock
    private <%=authClass%> <%=authInstance%>;

    private Server fakeServer;

    private ManagedChannel inProcessChannel;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        <%_ if (authenticationType === 'jwt') { _%>
        doReturn(true).when(tokenProvider).validateToken(anyString());
        <%_ } _%>
        doReturn(<% if (authenticationType === 'oauth2' || authenticationType === 'uaa') { %>new OAuth2Authentication(null, <% } %>new UsernamePasswordAuthenticationToken("user", "user",
            Collections.singletonList(new SimpleGrantedAuthority(AuthoritiesConstants.USER)))<% if (authenticationType === 'oauth2' || authenticationType === 'uaa') { %>)<% } %>
        ).when(<%=authInstance%>).<%=authMethod%>(<%=authMethodArgMatcher%>());

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
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "<%=authScheme%> dXNlcjp1c2Vy");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNIMPLEMENTED);
        <%_ if (authenticationType === 'session') { _%>
        ArgumentCaptor<Authentication> argument = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(argument.capture());
        assertThat(argument.getValue().getName()).isEqualTo("user");
        assertThat(argument.getValue().getCredentials().toString()).isEqualTo("user");
        <%_ } else { _%>
        verify(<%=authInstance%>).<%=authMethod%>("dXNlcjp1c2Vy");
        <%_ } _%>
        <%_ if (authenticationType === 'jwt') { _%>
        verify(tokenProvider).validateToken("dXNlcjp1c2Vy");
        <%_ } _%>
    }

    @Test
    public void testCapitalizedAuthorizationHeader() {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "<%=authScheme%> dXNlcjp1c2Vy");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNIMPLEMENTED);
        <%_ if (authenticationType === 'session') { _%>
        ArgumentCaptor<Authentication> argument = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(argument.capture());
        assertThat(argument.getValue().getName()).isEqualTo("user");
        assertThat(argument.getValue().getCredentials().toString()).isEqualTo("user");
        <%_ } else { _%>
        verify(<%=authInstance%>).<%=authMethod%>("dXNlcjp1c2Vy");
        <%_ } _%>
        <%_ if (authenticationType === 'jwt') { _%>
        verify(tokenProvider).validateToken("dXNlcjp1c2Vy");
        <%_ } _%>
    }

    @Test
    public void testNoAuthorization() {
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = LoggersServiceGrpc.newBlockingStub(inProcessChannel);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }

    @Test
    public void testAnonymousUserDenied() {
        doReturn(<% if (authenticationType === 'oauth2' || authenticationType === 'uaa') { %>new OAuth2Authentication(null, <% } %>new UsernamePasswordAuthenticationToken("user", "user",
            Collections.singletonList(new SimpleGrantedAuthority(AuthoritiesConstants.ANONYMOUS)))<% if (authenticationType === 'oauth2' || authenticationType === 'uaa') { %>)<% } %>
        ).when(<%=authInstance%>).<%=authMethod%>(<%=authMethodArgMatcher%>());
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "<%=authScheme%> dXNlcjp1c2Vy");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.PERMISSION_DENIED);
    }

    @Test
    public void testMissingScheme() {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "dXNlcjp1c2Vy");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }

    <%_ if (authenticationType === 'session') { _%>
    @Test
    public void testWrongUser() {
        doThrow(new BadCredentialsException("unknown user")).when(authenticationManager).authenticate(anyObject());
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "<%=authScheme%> dXNlcjp1c2Vy");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }

    @Test
    public void testMalformedToken() {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Basic dXNlcjp1c2Vy!");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }

    @Test
    public void testMissingColon() {
        Metadata metadata = new Metadata();
        // Basic useruser
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Basic dXNlcnVzZXI=");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
    }
    <%_ } else { _%>
    @Test
    public void testInvalidToken() {
        <%_ if (authenticationType === 'jwt') { _%>
        doReturn(false).when(tokenProvider).validateToken(anyString());
        <%_ } else { _%>
        doReturn(null).when(<%=authInstance%>).<%=authMethod%>(anyString());
        <%_ } _%>

        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer user_token");
        LoggersServiceGrpc.LoggersServiceBlockingStub stub = MetadataUtils.attachHeaders(LoggersServiceGrpc.newBlockingStub(inProcessChannel), metadata);
        assertGetLoggersReturnsCode(stub, Status.Code.UNAUTHENTICATED);
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
