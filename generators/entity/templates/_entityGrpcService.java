package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;

<% if (dto !== 'mapstruct') { %>
import <%=packageName%>.domain.<%=instanceType%>;<% } %>
import <%=packageName%>.grpc.AuthenticationInterceptor;
<%_ if (pagination !== 'no') { _%>
import <%=packageName%>.grpc.PageRequest;
import <%=packageName%>.grpc.ProtobufMappers;
<%_ } _%>
import <%=packageName%>.service.<%=entityClass%>Service;<% if (dto === 'mapstruct') { %>
import <%=packageName%>.service.dto.<%=instanceType%>;<% } %>

import com.google.protobuf.Empty;
import com.google.protobuf.<%=idProtoWrappedType%>;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;

/**
 * gRPC service providing CRUD methods for entity <%=entityClass%>.
 */
@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class <%=entityClass%>GrpcService extends <%=entityClass%>ServiceGrpc.<%=entityClass%>ServiceImplBase{

    private final <%=entityClass%>Service <%=entityInstance%>Service;

    private final <%=entityClass%>ProtoMapper <%=entityInstance%>ProtoMapper;

    public <%=entityClass%>GrpcService(<%=entityClass%>Service <%=entityInstance%>Service, <%=entityClass%>ProtoMapper <%=entityInstance%>ProtoMapper) {
        this.<%=entityInstance%>Service = <%=entityInstance%>Service;
        this.<%=entityInstance%>ProtoMapper = <%=entityInstance%>ProtoMapper;
    }

    @Override
    public void create<%=entityClass%>(<%=entityClass%>Proto request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        if( request.getIdOneofCase() == <%=entityClass%>Proto.IdOneofCase.ID) {
            responseObserver.onError(Status.ALREADY_EXISTS.asException());
            responseObserver.onCompleted();
        } else {
            update<%=entityClass%>(request, responseObserver);
        }

    }

    @Override
    public void update<%=entityClass%>(<%=entityClass%>Proto request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=instanceType%> <%=instanceName%> = <%=entityInstance%>ProtoMapper.<%=entityInstance%>ProtoTo<%=instanceType%>(request);
        <%=instanceName%> = <%=entityInstance%>Service.save(<%=instanceName%>);
        <%=entityClass%>Proto result = <%=entityInstance%>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>);
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    @Override
    public void getAll<%=entityClassPlural%>(<% if (pagination !== 'no') { %>PageRequest<% } else { %>Empty<% } %> request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=entityInstance%>Service.findAll(<% if (pagination !== 'no') { %>ProtobufMappers.pageRequestProtoToPageRequest(request)<% } %>)
            .forEach(<%=entityInstance%> -> responseObserver.onNext(<%=entityInstance%>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=entityInstance%>)));
        responseObserver.onCompleted();
    }

    @Override
    public void get<%=entityClass%>(<%=idProtoWrappedType%> request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=instanceType%> <%=instanceName%> = <%=entityInstance%>Service.findOne(request.getValue());
        if( <%=instanceName%> != null) {
            responseObserver.onNext(<%=entityInstance%>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>));
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void delete<%=entityClass%>(<%=idProtoWrappedType%> request, StreamObserver<Empty> responseObserver) {
        <%=entityInstance%>Service.delete(request.getValue());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
