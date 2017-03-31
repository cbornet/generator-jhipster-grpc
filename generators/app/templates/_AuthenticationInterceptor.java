package <%=packageName%>.grpc;

import <%=packageName%>.security.AuthoritiesConstants;<% if (authenticationType === 'jwt') { %>
import <%=packageName%>.security.jwt.TokenProvider;<% } %>
import io.grpc.*;
import io.grpc.Status;<% if (authenticationType === 'session') { %>
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;<% } %>
import org.springframework.security.core.Authentication;<% if (authenticationType === 'session') { %>
import org.springframework.security.core.AuthenticationException;<% } %>
import org.springframework.security.core.context.SecurityContextHolder;<% if (authenticationType === 'oauth2' || authenticationType === 'uaa') { %>
import org.springframework.security.oauth2.provider.token.TokenStore;<% } %>
import org.springframework.stereotype.Component;<% if (authenticationType === 'session') { %>
import org.springframework.util.Base64Utils;<% } %>
import org.springframework.util.StringUtils;<% if (authenticationType === 'session') { %>

import java.nio.charset.Charset;<% } %>

@Component
public class AuthenticationInterceptor implements ServerInterceptor {

    <%_ if (authenticationType === 'session') { _%>
    private final Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    private final AuthenticationManager authenticationManager;

    public AuthenticationInterceptor(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    <%_ } else if (authenticationType === 'jwt') { _%>
    private final TokenProvider tokenProvider;

    public AuthenticationInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    <%_ } else if (authenticationType === 'oauth2' || authenticationType === 'uaa') { _%>
    private final TokenStore tokenStore;

    public AuthenticationInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }
    <%_ } _%>

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall, Metadata metadata, ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String authorizationValue = metadata.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));
        if(authorizationValue == null) {
            // Some implementations don't support uppercased metadata keys
            authorizationValue = metadata.get(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER));
        }
        if (StringUtils.hasText(authorizationValue) && authorizationValue.startsWith("<% if (authenticationType === 'session') { %>Basic<% } else { %>Bearer<% } %> ")) {
            String token = authorizationValue.substring(<% if (authenticationType === 'session') { %>6<% } else { %>7<% } %>, authorizationValue.length());
            if (StringUtils.hasText(token)) {
                <%_ if (authenticationType === 'session') { _%>
                try {
                    String[] credentials = decodeToken(token);
                    Authentication authentication =
                        new UsernamePasswordAuthenticationToken(credentials[0], credentials[1]);
                    authenticationManager.authenticate(authentication);
                <%_ } else if (authenticationType === 'jwt') { _%>
                if (this.tokenProvider.validateToken(token)) {
                    Authentication authentication = this.tokenProvider.getAuthentication(token);
                <%_ } else if (authenticationType === 'oauth2' || authenticationType === 'uaa') { _%>
                Authentication authentication = this.tokenStore.readAuthentication(token);
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
                }<% if (authenticationType === 'session') { %> catch (AuthenticationException e) {
                    log.error("Cannot authenticate", e);
                    serverCall.close(Status.UNAUTHENTICATED, metadata);
                }<% } %>
            }
        }
        serverCall.close(Status.UNAUTHENTICATED, metadata);
        return null;
    }
    <%_ if (authenticationType === 'session') { _%>
    private String[] decodeToken(String base64Token) {
        byte[] decoded;
        try {
            decoded = Base64Utils.decode(base64Token.getBytes(Charset.forName("UTF-8")));
        }
        catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }
        String token = new String(decoded, Charset.forName("UTF-8"));
        int delim = token.indexOf(":");
        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[] { token.substring(0, delim), token.substring(delim + 1) };
    }
    <%_ } _%>
}
