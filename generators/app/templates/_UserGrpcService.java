package <%=packageName%>.grpc;

import <%=packageName%>.domain.User;
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.service.MailService;
import <%=packageName%>.service.UserService;
import <%=packageName%>.service.dto.UserDTO;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(UserGrpcService.class);

    private final UserRepository userRepository;

    private final MailService mailService;

    private final UserService userService;

    private final UserProtoMapper userProtoMapper;

    public UserGrpcService(UserRepository userRepository, MailService mailService,
                           UserService userService, UserProtoMapper userProtoMapper) {

        this.userRepository = userRepository;
        this.mailService = mailService;
        this.userService = userService;
        this.userProtoMapper = userProtoMapper;
    }

    public void createUser(UserProto userProto, StreamObserver<UserProto> responseObserver) {
        log.debug("gRPC request to save User : {}", userProto);

        if (!"".equals(userProto.getId())) {
            responseObserver.onError(new StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("A new user cannot already have an ID")));
            // Lowercase the user login before comparing with database
        } else if (userRepository.findOneByLogin(userProto.getLogin().toLowerCase()).isPresent()) {
            responseObserver.onError(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Login already in use")));
        } else if (userRepository.findOneByEmail(userProto.getEmail()).isPresent()) {
            responseObserver.onError(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Email already in use")));
        } else {
            User newUser = userService.createUser(userProtoMapper.userProtoToUserDTO(userProto));
            mailService.sendCreationEmail(newUser);
            responseObserver.onNext(userProtoMapper.userToUserProto(newUser));
            responseObserver.onCompleted();
        }
    }

    public void updateUser(UserProto userProto, StreamObserver<UserProto> responseObserver) {
        log.debug("gRPC request to update User : {}", userProto);
        Optional<User> existingUser = userRepository.findOneByEmail(userProto.getEmail());
        if (existingUser.isPresent() && (!existingUser.get().getId().equals(userProto.getId()))) {
            responseObserver.onError(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Email already in use")));
            return;
        }
        existingUser = userRepository.findOneByLogin(userProto.getLogin().toLowerCase());
        if (existingUser.isPresent() && (!existingUser.get().getId().equals(userProto.getId()))) {
            responseObserver.onError(new StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("Login already in use")));
            return;
        }
        Optional<UserDTO> updatedUser = userService.updateUser(userProtoMapper.userProtoToUserDTO(userProto));
        if (updatedUser.isPresent()) {
            responseObserver.onNext(userProtoMapper.userDTOToUserProto(updatedUser.get()));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
        }
    }

    public void getAllUsers(PageRequest request, StreamObserver<UserProto> responseObserver) {
        log.debug("gRPC request to get all users");
        userService.getAllManagedUsers(userProtoMapper.pageRequestProtoToPageRequest(request))
            .forEach(userDTO -> responseObserver.onNext(userProtoMapper.userDTOToUserProto(userDTO)));
        responseObserver.onCompleted();
    }

    public void getUser(StringValue login, StreamObserver<UserProto> responseObserver) {
        log.debug("gRPC request to get User : {}", login.getValue());
        Optional<User> user = userService.getUserWithAuthoritiesByLogin(login.getValue());
        if(user.isPresent()) {
            responseObserver.onNext(userProtoMapper.userToUserProto(user.get()));
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
        }
    }

    public void deleteUser(StringValue login, StreamObserver<Empty> responseObserver) {
        log.debug("gRPC request to delete User: {}", login.getValue());
        userService.deleteUser(login.getValue());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

}
