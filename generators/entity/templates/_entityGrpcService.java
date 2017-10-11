package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;

import <%=packageName%>.grpc.AuthenticationInterceptor;
<%_ if (pagination !== 'no') { _%>
import <%=packageName%>.grpc.PageRequest;
import <%=packageName%>.grpc.ProtobufMappers;
<%_ } _%>
import <%=packageName%>.service.<%= entityClass %>Service;<% if (dto === 'mapstruct') { %>
import <%=packageName%>.service.dto.<%=instanceType%>;<% } %>

import com.google.protobuf.Empty;
import com.google.protobuf.<%=idProtoWrappedType%>;
import io.grpc.Status;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.lognet.springboot.grpc.GRpcService;

import java.util.Optional;

/**
 * gRPC service providing CRUD methods for entity <%= entityClass %>.
 */
@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class <%= entityClass %>GrpcService extends Rx<%= entityClass %>ServiceGrpc.<%= entityClass %>ServiceImplBase {

    private final <%= entityClass %>Service <%= entityInstance %>Service;

    private final <%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper;

    public <%= entityClass %>GrpcService(<%= entityClass %>Service <%= entityInstance %>Service, <%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper) {
        this.<%= entityInstance %>Service = <%= entityInstance %>Service;
        this.<%= entityInstance %>ProtoMapper = <%= entityInstance %>ProtoMapper;
    }

    @Override
    public Single<<%= entityClass %>Proto> create<%= entityClass %>(Single<<%= entityClass %>Proto> request) {
        return update<%= entityClass %>(request
            .filter(<%= entityInstance %>Proto -> <%= entityInstance %>Proto.getIdOneofCase() != <%= entityClass %>Proto.IdOneofCase.ID)
            .switchIfEmpty(Single.error(Status.ALREADY_EXISTS.asException()))
        );
    }

    @Override
    public Single<<%= entityClass %>Proto> update<%= entityClass %>(Single<<%= entityClass %>Proto> request) {
        return request
            .map(<%= entityInstance %>ProtoMapper::<%= entityInstance %>ProtoTo<%= instanceType %>)
            .map(<%= entityInstance %>Service::save)
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Flowable<<%= entityClass %>Proto> getAll<%= entityClassPlural %>(Single<<% if (pagination !== 'no') { %>PageRequest<% } else { %>Empty<% } %>> request) {
        return request
            <%_ if (pagination !== 'no') { _%>
            .map(ProtobufMappers::pageRequestProtoToPageRequest)
            .map(<%= entityInstance %>Service::findAll)
            <%_ } else { _%>
            .map(e -> <%= entityInstance %>Service.findAll())
            <%_ } _%>
            .flatMapPublisher(Flowable::fromIterable)
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Single<<%= entityClass %>Proto> get<%= entityClass %>(Single<<%= idProtoWrappedType %>> request) {
        return request
            .map(<%= idProtoWrappedType %>::getValue)
            .map(id -> Optional.ofNullable(<%= entityInstance %>Service.findOne(id)).orElseThrow(Status.NOT_FOUND::asException))
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Single<Empty> delete<%= entityClass %>(Single<<%= idProtoWrappedType %>> request) {
        return request
            .map(<%= idProtoWrappedType %>::getValue)
            .doOnSuccess(<%= entityInstance %>Service::delete)
            .map(id -> Empty.newBuilder().build());
    }

}
