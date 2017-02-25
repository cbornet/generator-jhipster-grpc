package <%=packageName%>.grpc;

import <%=packageName%>.security.AuthoritiesConstants;
import <%=packageName%>.security.jwt.TokenProvider;
import io.grpc.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticationInterceptor implements ServerInterceptor {

    private final TokenProvider tokenProvider;

    public AuthenticationInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String authorizationValue = metadata.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));
        if(authorizationValue == null) {
            // Some implementations don't support uppercased metadata keys
            authorizationValue = metadata.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
        }
        if (StringUtils.hasText(authorizationValue) && authorizationValue.startsWith("Bearer ")) {
            String token = authorizationValue.substring(7, authorizationValue.length());
            if (StringUtils.hasText(token)) {
                if (this.tokenProvider.validateToken(token)) {
                    Authentication authentication = this.tokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    if (authentication.getAuthorities().stream()
                        .noneMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(AuthoritiesConstants.ANONYMOUS))
                        ) {
                        return serverCallHandler.startCall(serverCall, metadata);
                    } else {
                        serverCall.close(Status.PERMISSION_DENIED, metadata);
                    }
                }
            }
        }
        serverCall.close(Status.UNAUTHENTICATED, metadata);
        return null;
    }
}
