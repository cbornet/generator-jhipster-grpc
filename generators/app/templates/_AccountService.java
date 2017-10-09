package <%=packageName%>.grpc;

import <%=packageName%>.domain.User;
<%_ if (authenticationType === 'session') { _%>
import <%=packageName%>.repository.PersistentTokenRepository;
<%_ } _%>
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.security.SecurityUtils;
import <%=packageName%>.service.MailService;
import <%=packageName%>.service.UserService;
import <%=packageName%>.service.dto.UserDTO;
import <%=packageName%>.web.rest.vm.ManagedUserVM;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Status;
<%_ if (authenticationType === 'session') { _%>
import io.reactivex.Flowable;
<%_ } _%>
import io.reactivex.Single;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
<%_ if (databaseType === 'sql') { _%>
import org.springframework.transaction.TransactionSystemException;
<%_ } _%>

import javax.validation.ConstraintViolationException;
<%_ if (authenticationType === 'session') { _%>
import java.util.ArrayList;
<%_ } _%>
import java.util.Optional;

@GRpcService
public class AccountService extends RxAccountServiceGrpc.AccountServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    <%_ if (authenticationType === 'session') { _%>
    private final PersistentTokenRepository persistentTokenRepository;

    <%_ } _%>
    private final UserProtoMapper userProtoMapper;

    public AccountService(UserRepository userRepository, UserService userService, MailService mailService, <% if (authenticationType === 'session') { %>PersistentTokenRepository persistentTokenRepository, <% } %>UserProtoMapper userProtoMapper) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
        <%_ if (authenticationType === 'session') { _%>
        this.persistentTokenRepository = persistentTokenRepository;
        <%_ } _%>
        this.userProtoMapper = userProtoMapper;
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
            .map(userProto -> {
                try {
                    return userService.createUser(
                        userProto.getLogin().isEmpty() ? null : userProto.getLogin(),
                        userProto.getPassword().isEmpty() ? null : userProto.getPassword(),
                        userProto.getFirstName().isEmpty() ? null : userProto.getFirstName(),
                        userProto.getLastName().isEmpty() ? null : userProto.getLastName(),
                        userProto.getEmail().isEmpty() ? null : userProto.getEmail().toLowerCase(),
                        <%_ if (databaseType === 'mongodb' || databaseType === 'sql') { _%>
                        userProto.getImageUrl().isEmpty() ? null : userProto.getImageUrl(),
                        <%_ } _%>
                        userProto.getLangKey().isEmpty() ? null : userProto.getLangKey()
                    );
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
            .map(e -> Optional.ofNullable(userService.getUserWithAuthorities()).orElseThrow(Status.INTERNAL::asException))
            .map(UserDTO::new)
            .map(userProtoMapper::userDTOToUserProto);
    }

    @Override
    public Single<Empty> saveAccount(Single<UserProto> request) {
        return request
            .filter(user -> !userRepository.findOneByEmailIgnoreCase(user.getEmail())
                .map(User::getLogin)
                .map(login -> !login.equalsIgnoreCase(SecurityUtils.getCurrentUserLogin()))
                .isPresent()
            )
            .switchIfEmpty(Single.error(Status.ALREADY_EXISTS.withDescription("Email already in use").asException()))
            .filter(user -> userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).isPresent())
            .switchIfEmpty(Single.error(Status.INTERNAL.asException()))
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
            .map(e -> userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).orElseThrow(Status.INTERNAL::asException))
            .map(persistentTokenRepository::findByUser)
            .flatMapPublisher(Flowable::fromIterable)
            .map(ProtobufMappers::persistentTokenToPersistentTokenProto);
    }

    @Override
    public Single<Empty> invalidateSession(Single<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .filter(series -> userRepository
                .findOneByLogin(SecurityUtils.getCurrentUserLogin())
                .map(persistentTokenRepository::findByUser)
                .orElse(new ArrayList<>())
                .stream()
                .filter(persistentToken -> StringUtils.equals(persistentToken.getSeries(), series))
                .count() > 0)
            .doOnSuccess(persistentTokenRepository::delete)
            .toSingle("")
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
}
