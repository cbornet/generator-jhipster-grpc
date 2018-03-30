package <%= packageName %>.grpc;

<%_ if (authenticationType !== 'oauth2') { _%>
import <%= packageName %>.domain.User;
<%_ } _%>
<%_ if (authenticationType === 'session') { _%>
import <%= packageName %>.repository.PersistentTokenRepository;
<%_ } _%>
<%_ if (authenticationType !== 'oauth2') { _%>
import <%= packageName %>.repository.UserRepository;
import <%= packageName %>.security.SecurityUtils;
<%_ } _%>
<%_ if (authenticationType !== 'oauth2') { _%>
import <%= packageName %>.service.MailService;
<%_ } _%>
import <%= packageName %>.service.UserService;
<%_ if (authenticationType !== 'oauth2') { _%>
import <%= packageName %>.service.dto.UserDTO;
import <%= packageName %>.web.rest.vm.ManagedUserVM;
<%_ } _%>

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Status;
<%_ if (authenticationType !== 'oauth2') { _%>
import org.apache.commons.lang3.StringUtils;
<%_ } _%>
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<%_ if (authenticationType !== 'oauth2') { _%>
import org.springframework.data.util.Pair;
<%_ } _%>
import org.springframework.security.core.Authentication;
<%_ if (authenticationType === 'oauth2') { _%>
import org.springframework.security.core.context.SecurityContext;
<%_ } _%>
import org.springframework.security.core.context.SecurityContextHolder;
<%_ if (authenticationType === 'oauth2') { _%>
import org.springframework.security.oauth2.provider.OAuth2Authentication;
<%_ } else if (databaseType === 'sql') { _%>
import org.springframework.transaction.TransactionSystemException;
<%_ } _%>
<%_ if (authenticationType === 'session') { _%>
import reactor.core.publisher.Flux;
<%_ } _%>
import reactor.core.publisher.Mono;

<%_ if (authenticationType === 'session') { _%>
import java.util.ArrayList;
<%_ } _%>
<%_ if (authenticationType !== 'oauth2') { _%>
import javax.validation.ConstraintViolationException;
<%_ } _%>

@GRpcService
public class AccountService extends ReactorAccountServiceGrpc.AccountServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(AccountService.class);

    <%_ if (authenticationType !== 'oauth2') { _%>
    private final UserRepository userRepository;

    private final MailService mailService;

    <%_ } _%>
    private final UserService userService;

    <%_ if (authenticationType === 'session') { _%>
    private final PersistentTokenRepository persistentTokenRepository;

    <%_ } _%>
    private final UserProtoMapper userProtoMapper;

    public AccountService(<% if (authenticationType !== 'oauth2') { %>UserRepository userRepository, MailService mailService, <% } %>UserService userService, <% if (authenticationType === 'session') { %>PersistentTokenRepository persistentTokenRepository, <% } %>UserProtoMapper userProtoMapper) {
        <%_ if (authenticationType !== 'oauth2') { _%>
        this.userRepository = userRepository;
        this.mailService = mailService;
        <%_ } _%>
        this.userService = userService;
        <%_ if (authenticationType === 'session') { _%>
        this.persistentTokenRepository = persistentTokenRepository;
        <%_ } _%>
        this.userProtoMapper = userProtoMapper;
    }

    @Override
    public Mono<StringValue> isAuthenticated(Mono<Empty> request) {
        return request.map(e -> {
            log.debug("gRPC request to check if the current user is authenticated");
            Authentication principal = SecurityContextHolder.getContext().getAuthentication();
            StringValue.Builder builder = StringValue.newBuilder();
            if (principal != null) {
                builder.setValue(principal.getName());
            }
            return builder.build();
        });
    }

    @Override
    public Mono<UserProto> getAccount(Mono<Empty> request) {
        return request
<%_ if (authenticationType === 'oauth2') { _%>
            .map(e -> SecurityContextHolder.getContext())
            .filter(context -> context.getAuthentication() != null)
            .switchIfEmpty(Mono.error(Status.INTERNAL.withDescription("Authentication could not be found").asRuntimeException()))
            .map(SecurityContext::getAuthentication)
            .map(authentication -> {
                // For some reason this doesn't work with filter...
                if (authentication instanceof OAuth2Authentication) {
                    return authentication;
                }
                throw Status.INTERNAL.withDescription("User must be authenticated with OAuth2").asRuntimeException();
            })
            .map(it -> userService.getUserFromAuthentication((OAuth2Authentication) it))
            .map(userProtoMapper::userDTOToUserProto);
    }
<%_ } else { _%>
            .map(e -> userService.getUserWithAuthorities().orElseThrow(Status.INTERNAL::asRuntimeException))
            .map(UserDTO::new)
            .map(userProtoMapper::userDTOToUserProto);
    }

    @Override
    public Mono<Empty> registerAccount(Mono<UserProto> request) {
        return request
            .doOnSuccess(userProto -> log.debug("gRPC request to register account {}", userProto.getLogin()))
            .filter(userProto -> checkPasswordLength(userProto.getPassword()))
            .switchIfEmpty(Mono.error(Status.INVALID_ARGUMENT.withDescription("Incorrect password").asRuntimeException()))
            .filter(userProto -> !userRepository.findOneByLogin(userProto.getLogin().toLowerCase()).isPresent())
            .switchIfEmpty(Mono.error(Status.ALREADY_EXISTS.withDescription("Login already in use").asRuntimeException()))
            .filter(userProto -> !userRepository.findOneByEmailIgnoreCase(userProto.getEmail()).isPresent())
            .switchIfEmpty(Mono.error(Status.ALREADY_EXISTS.withDescription("Email already in use").asRuntimeException()))
            .map(userProto -> Pair.of(userProtoMapper.userProtoToUserDTO(userProto), userProto.getPassword()))
            .map(pair -> {
                try {
                    return userService.registerUser(pair.getFirst(), pair.getSecond());
                <%_ if (databaseType === 'sql') { _%>
                } catch (TransactionSystemException e) {
                    if (e.getOriginalException().getCause() instanceof ConstraintViolationException) {
                        log.info("Invalid user", e);
                        throw Status.INVALID_ARGUMENT.withDescription("Invalid user").asRuntimeException();
                    } else {
                        throw e;
                    }
                <%_ } _%>
                } catch (ConstraintViolationException e) {
                    log.error("Invalid user", e);
                    throw Status.INVALID_ARGUMENT.withDescription("Invalid user").asRuntimeException();
                }
            })
            .doOnSuccess(mailService::sendCreationEmail)
            .map(u -> Empty.newBuilder().build());
    }

    @Override
    public Mono<UserProto> activateAccount(Mono<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .map(key -> userService.activateRegistration(key)
                .orElseThrow(Status.INTERNAL.withDescription("No user was found for this activation key")::asRuntimeException)
            )
            .map(userProtoMapper::userToUserProto);
    }

    @Override
    public Mono<Empty> saveAccount(Mono<UserProto> request) {
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(Status.INTERNAL::asRuntimeException);
        return request
            .filter(user -> !userRepository.findOneByEmailIgnoreCase(user.getEmail())
                .map(User::getLogin)
                .map(login -> !login.equalsIgnoreCase(currentLogin))
                .isPresent()
            )
            .switchIfEmpty(Mono.error(Status.ALREADY_EXISTS.withDescription("Email already in use").asRuntimeException()))
            .filter(user -> userRepository.findOneByLogin(currentLogin).isPresent())
            .switchIfEmpty(Mono.error(Status.INTERNAL.asRuntimeException()))
            .doOnSuccess(user -> {
                try {
                    userService.updateUser(
                        user.getFirstName().isEmpty() ? null : user.getFirstName(),
                        user.getLastName().isEmpty() ? null : user.getLastName(),
                        user.getEmail().isEmpty() ? null : user.getEmail(),
                        user.getLangKey().isEmpty() ? null : user.getLangKey()<% if (databaseType === 'mongodb' || databaseType === 'sql') { %>,
                        user.getImageUrl().isEmpty() ? null : user.getImageUrl()<% } %>
                    );
                <%_ if (databaseType === 'sql') { _%>
                } catch (TransactionSystemException e) {
                    if (e.getOriginalException().getCause() instanceof ConstraintViolationException) {
                        log.info("Invalid user", e);
                        throw Status.INVALID_ARGUMENT.withDescription("Invalid user").asRuntimeException();
                    } else {
                        throw e;
                    }
                <%_ } _%>
                } catch (ConstraintViolationException e) {
                    log.error("Invalid user", e);
                    throw Status.INVALID_ARGUMENT.withDescription("Invalid user").asRuntimeException();
                }
            })
            .map(u -> Empty.newBuilder().build());
    }

    @Override
    public Mono<Empty> changePassword(Mono<PasswordChange> request) {
        return request
            .filter(passwordChange -> AccountService.checkPasswordLength(passwordChange.getNewPassword()))
            .switchIfEmpty(Mono.error(Status.INVALID_ARGUMENT.withDescription("Incorrect password").asRuntimeException()))
            .doOnSuccess(passwordChange -> userService.changePassword(passwordChange.getCurrentPassword(), passwordChange.getNewPassword()))
            .map(p -> Empty.newBuilder().build());
    }

    <%_ if (authenticationType === 'session') { _%>
    @Override
    public Flux<PersistentToken> getCurrentSessions(Mono<Empty> request) {
        return request
            .map(e-> SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .orElseThrow(Status.INTERNAL::asRuntimeException)
            )
            .flatMapIterable(persistentTokenRepository::findByUser)
            .map(ProtobufMappers::persistentTokenToPersistentTokenProto);
    }

    @Override
    public Mono<Empty> invalidateSession(Mono<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .doOnSuccess(series -> SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .map(persistentTokenRepository::findByUser)
                .orElse(new ArrayList<>())
                .stream()
                .filter(persistentToken -> StringUtils.equals(persistentToken.getSeries(), series))
                .forEach(persistentTokenRepository::delete)
            )
            .map(s -> Empty.newBuilder().build());
    }

    <%_ } _%>
    @Override
    public Mono<Empty> requestPasswordReset(Mono<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .map(mail -> userService.requestPasswordReset(mail)
                .orElseThrow(Status.INVALID_ARGUMENT.withDescription("e-mail address not registered")::asRuntimeException)
            )
            .doOnSuccess(mailService::sendPasswordResetMail)
            .map(u -> Empty.newBuilder().build());
    }

    @Override
    public Mono<Empty> finishPasswordReset(Mono<KeyAndPassword> request) {
        return request
            .filter(keyAndPassword -> checkPasswordLength(keyAndPassword.getNewPassword()))
            .switchIfEmpty(Mono.error(Status.INVALID_ARGUMENT.withDescription("Incorrect password").asRuntimeException()))
            .map(keyAndPassword -> userService
                .completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())
                .orElseThrow(Status.INTERNAL::asRuntimeException)
            )
            .doOnSuccess(mailService::sendPasswordResetMail)
            .map(user -> Empty.newBuilder().build());
    }

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
            password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
            password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }
<%_ } _%>

}
