package <%=packageName%>.grpc;

import <%=packageName%>.security.AuthoritiesConstants;<% if (authenticationType === 'jwt') { %>
import <%=packageName%>.security.jwt.TokenProvider;<% } %>
import io.grpc.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;<% if (authenticationType === 'oauth2') { %>
import org.springframework.security.oauth2.provider.token.TokenStore;<% } %>
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticationInterceptor implements ServerInterceptor {

    private final <% if (authenticationType === 'jwt') { %>TokenProvider<% } else { %>TokenStore<% } %> tokenProvider;

    public AuthenticationInterceptor(<% if (authenticationType === 'jwt') { %>TokenProvider<% } else { %>TokenStore<% } %> tokenProvider) {
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
                <%_ if (authenticationType === 'jwt') { _%>
                if (this.tokenProvider.validateToken(token)) {
                    Authentication authentication = this.tokenProvider.getAuthentication(token);
                <%_ } else { _%>
                Authentication authentication = this.tokenProvider.readAuthentication(token);
                if (authentication != null) {
                <%_ } _%>
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
