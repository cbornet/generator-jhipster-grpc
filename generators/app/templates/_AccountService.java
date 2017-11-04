package <%= packageName %>.grpc;

<%_ if (authenticationType !== 'oauth2' || applicationType !== 'monolith') { _%>
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
<%_ if (authenticationType !== 'oauth2' || applicationType === 'monolith') { _%>
import <%= packageName %>.service.UserService;
<%_ } _%>
<%_ if (authenticationType !== 'oauth2') { _%>
import <%= packageName %>.service.dto.UserDTO;
import <%= packageName %>.web.rest.vm.ManagedUserVM;
<%_ } _%>

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Status;
<%_ if (authenticationType === 'session') { _%>
import io.reactivex.Flowable;
<%_ } _%>
import io.reactivex.Single;
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
<%_ if (authenticationType === 'oauth2' && applicationType !== 'monolith') { _%>
import org.springframework.security.core.GrantedAuthority;
<%_ } _%>
<%_ if (authenticationType === 'oauth2') { _%>
import org.springframework.security.core.context.SecurityContext;
<%_ } _%>
import org.springframework.security.core.context.SecurityContextHolder;
<%_ if (authenticationType === 'oauth2') { _%>
import org.springframework.security.oauth2.provider.OAuth2Authentication;
    <%_ if (applicationType !== 'monolith') { _%>

import java.util.Map;
import java.util.stream.Collectors;
    <%_ } _%>
<%_ } else { _%>
    <%_ if (databaseType === 'sql') { _%>
import org.springframework.transaction.TransactionSystemException;
    <%_ } _%>

import javax.validation.ConstraintViolationException;
    <%_ if (authenticationType === 'session') { _%>
import java.util.ArrayList;
    <%_ } _%>
<%_ } _%>

@GRpcService
public class AccountService extends RxAccountServiceGrpc.AccountServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(AccountService.class);

    <%_ if (authenticationType !== 'oauth2') { _%>
    private final UserRepository userRepository;

    private final MailService mailService;

    <%_ } _%>
    <%_ if (authenticationType !== 'oauth2' || applicationType === 'monolith') { _%>
    private final UserService userService;

    <%_ } _%>
    <%_ if (authenticationType === 'session') { _%>
    private final PersistentTokenRepository persistentTokenRepository;

    <%_ } _%>
    private final UserProtoMapper userProtoMapper;

    public AccountService(<% if (authenticationType !== 'oauth2') { %>UserRepository userRepository, MailService mailService, <% } if (authenticationType !== 'oauth2' || applicationType === 'monolith') { %>UserService userService, <% } if (authenticationType === 'session') { %>PersistentTokenRepository persistentTokenRepository, <% } %>UserProtoMapper userProtoMapper) {
        <%_ if (authenticationType !== 'oauth2') { _%>
        this.userRepository = userRepository;
        this.mailService = mailService;
        <%_ } _%>
        <%_ if (authenticationType !== 'oauth2' || applicationType === 'monolith') { _%>
        this.userService = userService;
        <%_ } _%>
        <%_ if (authenticationType === 'session') { _%>
        this.persistentTokenRepository = persistentTokenRepository;
        <%_ } _%>
        this.userProtoMapper = userProtoMapper;
    }

    @Override
    public Single<StringValue> isAuthenticated(Single<Empty> request) {
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
    public Single<UserProto> getAccount(Single<Empty> request) {
        return request
<%_ if (authenticationType === 'oauth2') { _%>
            .map(e -> SecurityContextHolder.getContext())
            .filter(context -> context.getAuthentication() != null)
            .switchIfEmpty(Single.error(Status.INTERNAL.withDescription("Authentication could not be found").asException()))
            .map(SecurityContext::getAuthentication)
    <%_ if (applicationType === 'monolith') { _%>
            .map(authentication -> {
                // For some reason this doesn't work with filter...
                if (authentication instanceof OAuth2Authentication) {
                    return authentication;
                }
                throw Status.INTERNAL.withDescription("User must be authenticated with OAuth2").asException();
            })
            .map(it -> userService.getUserFromAuthentication((OAuth2Authentication) it))
            .map(userProtoMapper::userDTOToUserProto);
    }
    <%_ } else { _%>
            .filter(it -> it instanceof OAuth2Authentication)
            .map(it -> ((OAuth2Authentication) it).getUserAuthentication())
            .map(authentication -> {
                Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
                Boolean activated = false;
                if (details.get("email_verified") != null) {
                    activated = (Boolean) details.get("email_verified");
                }
                return new User(
                    authentication.getName(),
                    (String) details.get("given_name"),
                    (String) details.get("family_name"),
                    (String) details.get("email"),
                    (String) details.get("langKey"),
                    (String) details.get("imageUrl"),
                    activated,
                    authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
                );
            })
            .map(userProtoMapper::userToUserProto);
    }
    <%_ } _%>
<%_ } else { _%>
            .map(e -> userService.getUserWithAuthorities().orElseThrow(Status.INTERNAL::asException))
            .map(UserDTO::new)
            .map(userProtoMapper::userDTOToUserProto);
    }

    @Override
    public Single<Empty> registerAccount(Single<UserProto> request) {
        return request
            .doOnSuccess(userProto -> log.debug("gRPC request to register account {}", userProto.getLogin()))
            .filter(userProto -> checkPasswordLength(userProto.getPassword()))
            .switchIfEmpty(Single.error(Status.INVALID_ARGUMENT.withDescription("Incorrect password").asException()))
            .filter(userProto -> !userRepository.findOneByLogin(userProto.getLogin().toLowerCase()).isPresent())
            .switchIfEmpty(Single.error(Status.ALREADY_EXISTS.withDescription("Login already in use").asException()))
            .filter(userProto -> !userRepository.findOneByEmailIgnoreCase(userProto.getEmail()).isPresent())
            .switchIfEmpty(Single.error(Status.ALREADY_EXISTS.withDescription("Email already in use").asException()))
            .map(userProto -> Pair.of(userProtoMapper.userProtoToUserDTO(userProto), userProto.getPassword()))
            .map(pair -> {
                try {
                    return userService.registerUser(pair.getFirst(), pair.getSecond());
                <%_ if (databaseType === 'sql') { _%>
                } catch (TransactionSystemException e) {
                    if (e.getOriginalException().getCause() instanceof ConstraintViolationException) {
                        log.info("Invalid user", e);
                        throw Status.INVALID_ARGUMENT.withDescription("Invalid user").asException();
                    } else {
                        throw e;
                    }
                <%_ } _%>
                } catch (ConstraintViolationException e) {
                    log.error("Invalid user", e);
                    throw Status.INVALID_ARGUMENT.withDescription("Invalid user").asException();
                }
            })
            .doOnSuccess(mailService::sendCreationEmail)
            .map(u -> Empty.newBuilder().build());
    }

    @Override
    public Single<UserProto> activateAccount(Single<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .map(key -> userService.activateRegistration(key).orElseThrow(Status.INTERNAL::asException))
            .map(userProtoMapper::userToUserProto);
    }

    @Override
    public Single<Empty> saveAccount(Single<UserProto> request) {
        String currentLogin = SecurityUtils.getCurrentUserLogin().orElseThrow(Status.INTERNAL::asRuntimeException);
        return request
            .filter(user -> !userRepository.findOneByEmailIgnoreCase(user.getEmail())
                .map(User::getLogin)
                .map(login -> !login.equalsIgnoreCase(currentLogin))
                .isPresent()
            )
            .switchIfEmpty(Single.error(Status.ALREADY_EXISTS.withDescription("Email already in use").asException()))
            .map(user -> userRepository.findOneByLogin(currentLogin).orElseThrow(Status.INTERNAL::asException))
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
    public Single<Empty> changePassword(Single<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .filter(AccountService::checkPasswordLength)
            .switchIfEmpty(Single.error(Status.INVALID_ARGUMENT.withDescription("Incorrect password").asException()))
            .doOnSuccess(userService::changePassword)
            .map(p -> Empty.newBuilder().build());
    }

    <%_ if (authenticationType === 'session') { _%>
    @Override
    public Flowable<PersistentToken> getCurrentSessions(Single<Empty> request) {
        return request
            .map(e-> SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .orElseThrow(Status.INTERNAL::asException)
            )
            .map(persistentTokenRepository::findByUser)
            .flatMapPublisher(Flowable::fromIterable)
            .map(ProtobufMappers::persistentTokenToPersistentTokenProto);
    }

    @Override
    public Single<Empty> invalidateSession(Single<StringValue> request) {
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
    public Single<Empty> requestPasswordReset(Single<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .map(mail -> userService.requestPasswordReset(mail)
                .orElseThrow(Status.INVALID_ARGUMENT.withDescription("e-mail address not registered")::asException)
            )
            .doOnSuccess(mailService::sendPasswordResetMail)
            .map(u -> Empty.newBuilder().build());
    }

    @Override
    public Single<Empty> finishPasswordReset(Single<KeyAndPassword> request) {
        return request
            .filter(keyAndPassword -> checkPasswordLength(keyAndPassword.getNewPassword()))
            .switchIfEmpty(Single.error(Status.INVALID_ARGUMENT.withDescription("Incorrect password").asException()))
            .map(keyAndPassword -> userService
                .completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey())
                .orElseThrow(Status.INTERNAL::asException)
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
