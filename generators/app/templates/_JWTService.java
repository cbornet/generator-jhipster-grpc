package <%=packageName%>.grpc;

import <%=packageName%>.security.jwt.TokenProvider;

import io.grpc.Status;
import io.reactivex.Single;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

@GRpcService
public class JWTService extends RxJWTServiceGrpc.JWTServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(JWTService.class);

    private final TokenProvider tokenProvider;

    private final AuthenticationManager authenticationManager;

    public JWTService(TokenProvider tokenProvider, AuthenticationManager authenticationManager) {
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Single<JWTToken> authenticate(Single<Login> request) {
        return request
            .map( login -> {
                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword());
                try {
                    Authentication authentication = this.authenticationManager.authenticate(authenticationToken);
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
