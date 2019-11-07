package <%= packageName %>.grpc;

import <%= packageName %>.security.jwt.TokenProvider;

import io.grpc.Status;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import reactor.core.publisher.Mono;

@GRpcService
public class JWTService extends ReactorJWTServiceGrpc.JWTServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(JWTService.class);

    private final TokenProvider tokenProvider;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    public JWTService(TokenProvider tokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder) {
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    @Override
    public Mono<JWTToken> authenticate(Mono<Login> request) {
        return request
            .map( login -> {
                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());
                try {
                    Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    return tokenProvider.createToken(authentication, login.getRememberMe());
                } catch (AuthenticationException ae) {
                    log.trace("Authentication exception", ae);
                    throw Status.UNAUTHENTICATED.asRuntimeException();
                }
            })
            .map(jwt -> JWTToken.newBuilder().setIdToken(jwt).build());

    }
}
