package <%=packageName%>.grpc;

import <%=packageName%>.domain.User;
import <%=packageName%>.repository.PersistentTokenRepository;
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.security.SecurityUtils;
import <%=packageName%>.service.MailService;
import <%=packageName%>.service.UserService;
import <%=packageName%>.service.dto.UserDTO;
import <%=packageName%>.web.rest.vm.ManagedUserVM;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.StringUtils;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.validation.ConstraintViolationException;
import java.util.Optional;

@GRpcService
public class AccountService extends AccountServiceGrpc.AccountServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;

    private final UserService userService;

    private final MailService mailService;

    private final PersistentTokenRepository persistentTokenRepository;

    private final UserProtoMapper userProtoMapper;

    public AccountService(UserRepository userRepository, UserService userService, MailService mailService, PersistentTokenRepository persistentTokenRepository, UserProtoMapper userProtoMapper) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mailService = mailService;
        this.persistentTokenRepository = persistentTokenRepository;
        this.userProtoMapper = userProtoMapper;
    }

    @Override
    public void registerAccount(UserProto userProto, StreamObserver<Empty> responseObserver) {
        log.debug("gRPC request to register account {}", userProto.getLogin());
        if (userRepository.findOneByLogin(userProto.getLogin().toLowerCase()).isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Login already in use").asException());
        } else if (userRepository.findOneByEmail(userProto.getEmail()).isPresent()) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Email already in use").asException());
        } else {
            try {
                User newUser = userService.createUser(
                    userProto.getLogin().isEmpty() ? null : userProto.getLogin(),
                    userProto.getPassword().isEmpty() ? null : userProto.getPassword(),
                    userProto.getFirstName().isEmpty() ? null : userProto.getFirstName(),
                    userProto.getLastName().isEmpty() ? null : userProto.getLastName(),
                    userProto.getEmail().isEmpty() ? null : userProto.getEmail().toLowerCase(),
                    userProto.getImageUrl().isEmpty() ? null : userProto.getImageUrl(),
                    userProto.getLangKey().isEmpty() ? null : userProto.getLangKey()
                );
                mailService.sendCreationEmail(newUser);
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            } catch (ConstraintViolationException e) {
                log.error("Invalid user", e);
                responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid user").asException());
            }
        }

    }

    @Override
    public void activateAccount(StringValue key, StreamObserver<UserProto> responseObserver) {
        Optional<User> user = userService.activateRegistration(key.getValue());
        if (user.isPresent()) {
            responseObserver.onNext(userProtoMapper.userToUserProto(user.get()));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.INTERNAL.asException());
        }
    }

    @Override
    public void isAuthenticated(Empty request, StreamObserver<StringValue> responseObserver) {
        log.debug("gRPC request to check if the current user is authenticated");
        Authentication principal = SecurityContextHolder.getContext().getAuthentication();
        StringValue.Builder builder = StringValue.newBuilder();
        if (principal != null ) {
            builder.setValue(principal.getName());
        }
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAccount(Empty request, StreamObserver<UserProto> responseObserver) {
        User user = userService.getUserWithAuthorities();
        if (user != null) {
            responseObserver.onNext(userProtoMapper.userDTOToUserProto(new UserDTO(user)));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.INTERNAL.asException());
        }
    }

    @Override
    public void saveAccount(UserProto user, StreamObserver<Empty> responseObserver) {
        Optional<User> existingUser = userRepository.findOneByEmail(user.getEmail());
        if (existingUser.isPresent() && (!existingUser.get().getLogin().equalsIgnoreCase(user.getLogin()))) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription("Email already in use").asException());
        } else {
            existingUser = userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin());
            if (existingUser.isPresent()) {
                userService.updateUser(
                    user.getFirstName().isEmpty() ? null : user.getFirstName(),
                    user.getLastName().isEmpty() ? null : user.getLastName(),
                    user.getEmail().isEmpty() ? null : user.getEmail(),
                    user.getLangKey().isEmpty() ? null : user.getLangKey()
                );
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.INTERNAL.asException());
            }
        }
    }

    @Override
    public void changePassword(StringValue password, StreamObserver<Empty> responseObserver) {
        if (!checkPasswordLength(password.getValue())) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Incorrect password").asException());
        } else {
            userService.changePassword(password.getValue());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void invalidateSession(StringValue series, StreamObserver<Empty> responseObserver) {
        userRepository.findOneByLogin(SecurityUtils.getCurrentUserLogin()).ifPresent(u ->
            persistentTokenRepository.findByUser(u).stream()
                .filter(persistentToken -> StringUtils.equals(persistentToken.getSeries(), series.getValue()))
                .findAny().ifPresent(t -> persistentTokenRepository.delete(series.getValue())));
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void requestPasswordReset(StringValue mail, StreamObserver<Empty> responseObserver) {
        Optional<User> user = userService.requestPasswordReset(mail.getValue());
        if (user.isPresent()) {
            mailService.sendPasswordResetMail(user.get());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("e-mail address not registered").asException());
        }
    }

    @Override
    public void finishPasswordReset(KeyAndPassword keyAndPassword, StreamObserver<Empty> responseObserver) {
        if (!checkPasswordLength(keyAndPassword.getNewPassword())) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Incorrect password").asException());
        } else {
            Optional<User> user = userService.completePasswordReset(keyAndPassword.getNewPassword(), keyAndPassword.getKey());
            if (user.isPresent()) {
                mailService.sendPasswordResetMail(user.get());
                responseObserver.onNext(Empty.newBuilder().build());
                responseObserver.onCompleted();
            } else {
                responseObserver.onError(Status.INTERNAL.asException());
            }
        }
    }

    private static boolean checkPasswordLength(String password) {
        return !StringUtils.isEmpty(password) &&
            password.length() >= ManagedUserVM.PASSWORD_MIN_LENGTH &&
            password.length() <= ManagedUserVM.PASSWORD_MAX_LENGTH;
    }
}
