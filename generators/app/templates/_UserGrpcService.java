package <%= packageName %>.grpc;

<%_ if (authenticationType !== 'oauth2') { _%>
import <%= packageName %>.domain.User;
import <%= packageName %>.repository.UserRepository;
<%_ } _%>
<%_ if (searchEngine === 'elasticsearch') { _%>
import <%= packageName %>.repository.search.UserSearchRepository;
<%_ } _%>
import <%= packageName %>.security.AuthoritiesConstants;
import <%= packageName %>.security.SecurityUtils;
<%_ if (authenticationType !== 'oauth2') { _%>
import <%= packageName %>.service.MailService;
<%_ } _%>
import <%= packageName %>.service.UserService;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Status;
<%_ if (searchEngine === 'elasticsearch') { _%>
import org.elasticsearch.index.query.QueryBuilders;
<%_ } _%>
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class UserGrpcService extends ReactorUserServiceGrpc.UserServiceImplBase {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(UserGrpcService.class);

    <%_ if (authenticationType !== 'oauth2') { _%>
    private final UserRepository userRepository;

    private final MailService mailService;

    <%_ } _%>
    private final UserService userService;

    private final UserProtoMapper userProtoMapper;

    <%_ if (searchEngine === 'elasticsearch') { _%>
    private final UserSearchRepository userSearchRepository;

    <%_ } _%>
    public UserGrpcService(<% if (authenticationType !== 'oauth2') { %>UserRepository userRepository, MailService mailService, <% } %>UserService userService,
                        UserProtoMapper userProtoMapper<% if (searchEngine === 'elasticsearch') { %>, UserSearchRepository userSearchRepository<% } %>) {
        <%_ if (authenticationType !== 'oauth2') { _%>
        this.userRepository = userRepository;
        this.mailService = mailService;
        <%_ } _%>
        this.userService = userService;
        this.userProtoMapper = userProtoMapper;
        <%_ if (searchEngine === 'elasticsearch') { _%>
        this.userSearchRepository = userSearchRepository;
        <%_ } _%>
    }
    <%_ if (authenticationType !== 'oauth2') { _%>

    @Override
    public Mono<UserProto> createUser(Mono<UserProto> request) {
        return request
            .doOnSuccess(userProto -> log.debug("gRPC request to save User : {}", userProto))
            .filter(userProto -> userProto.getIdOneofCase() != UserProto.IdOneofCase.ID)
            .switchIfEmpty(Mono.error(Status.INVALID_ARGUMENT.withDescription("A new user cannot already have an ID").asRuntimeException()))
            .filter(userProto -> !userRepository.findOneByLogin(userProto.getLogin().toLowerCase()).isPresent())
            .switchIfEmpty(Mono.error(Status.ALREADY_EXISTS.withDescription("Login already in use").asRuntimeException()))
            .filter(userProto -> !userRepository.findOneByEmailIgnoreCase(userProto.getEmail()).isPresent())
            .switchIfEmpty(Mono.error(Status.ALREADY_EXISTS.withDescription("Email already in use").asRuntimeException()))
            .map(userProtoMapper::userProtoToUserDTO)
            .map(userService::createUser)
            .doOnSuccess(mailService::sendCreationEmail)
            .map(userProtoMapper::userToUserProto);
    }

    @Override
    public Mono<UserProto> updateUser(Mono<UserProto> request) {
        return request
            .doOnSuccess(userProto -> log.debug("gRPC request to update User : {}", userProto))
            .filter(userProto -> !userRepository
                .findOneByEmailIgnoreCase(userProto.getEmail())
                .map(User::getId)
                .filter(id -> !id.equals(userProto.getId()))
                .isPresent()
            )
            .switchIfEmpty(Mono.error(Status.ALREADY_EXISTS.withDescription("Email already in use").asRuntimeException()))
            .filter(userProto -> !userRepository
                .findOneByLogin(userProto.getLogin().toLowerCase())
                .map(User::getId)
                .filter(id -> !id.equals(userProto.getId()))
                .isPresent()
            )
            .switchIfEmpty(Mono.error(Status.ALREADY_EXISTS.withDescription("Login already in use").asRuntimeException()))
            .map(userProtoMapper::userProtoToUserDTO)
            .map(user -> userService.updateUser(user).orElseThrow(Status.NOT_FOUND::asRuntimeException))
            .map(userProtoMapper::userDTOToUserProto);
    }
    <%_ } _%>

    @Override
    public Flux<UserProto> getAllUsers(Mono<<% if (databaseType === 'sql' || databaseType === 'mongodb') { %>PageRequest<% } else { %>Empty<% } %>> request) {
        log.debug("gRPC request to get all users");
        return request
            <%_ if (databaseType === 'sql' || databaseType === 'mongodb') { _%>
            .map(ProtobufMappers::pageRequestProtoToPageRequest)
            .flatMapIterable(userService::getAllManagedUsers)
            <%_ } else { _%>
            .flatMapIterable(e-> userService.getAllManagedUsers())
            <%_ } _%>
            .map(userProtoMapper::userDTOToUserProto);
    }

    @Override
    public Mono<UserProto> getUser(Mono<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .doOnSuccess(login -> log.debug("gRPC request to get User : {}", login))
            .map(login -> userService.getUserWithAuthoritiesByLogin(login).orElseThrow(Status.NOT_FOUND::asRuntimeException))
            .map(userProtoMapper::userToUserProto);
    }
    <%_ if (authenticationType !== 'oauth2') { _%>

    @Override
    public Mono<Empty> deleteUser(Mono<StringValue> request) {
        return request
            .map(StringValue::getValue)
            .doOnSuccess(login -> log.debug("gRPC request to delete User: {}", login))
            .doOnSuccess(userService::deleteUser)
            .map(l -> Empty.newBuilder().build());
    }
    <%_ } _%>
    <%_ if (databaseType === 'sql' || databaseType === 'mongodb') { _%>

    @Override
    public Flux<StringValue> getAllAuthorities(Mono<Empty> request) {
        return request
            .doOnSuccess(e -> log.debug("gRPC request to gat all authorities"))
            .filter(e -> SecurityUtils.isCurrentUserInRole(AuthoritiesConstants.ADMIN))
            .switchIfEmpty(Mono.error(Status.PERMISSION_DENIED.asRuntimeException()))
            .flatMapIterable(e -> userService.getAuthorities())
            .map(authority -> StringValue.newBuilder().setValue(authority).build());
    }
    <%_ } _%>
    <%_ if (searchEngine === 'elasticsearch') { _%>

    @Override
    public Flux<UserProto> searchUsers(Mono<UserSearchPageRequest> request) {
        return request
            .map(UserSearchPageRequest::getQuery)
            .map(StringValue::getValue)
            .doOnSuccess(query -> log.debug("gRPC request to search Users for query {}", query))
            .map(QueryBuilders::queryStringQuery)
            .flatMapMany(query -> request
                .map(UserSearchPageRequest::getPageRequest)
                .map(ProtobufMappers::pageRequestProtoToPageRequest)
                .flatMapIterable(pageRequest -> userSearchRepository.search(query, pageRequest))
            )
            .map(userProtoMapper::userToUserProto);
    }
    <%_ } _%>

}
