package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;


import com.google.protobuf.Empty;
import com.google.protobuf.<%=idProtoWrappedType%>;
import <%=packageName%>.grpc.AuthenticationInterceptor;
<%_ if (pagination !== 'no') { _%>
import com.mycompany.myapp.grpc.PageRequest;
import com.mycompany.myapp.grpc.ProtobufUtil;
<%_ } _%><% if (dto !== 'mapstruct') { %>
import <%=packageName%>.domain.<%=instanceType%>;<% } %>
import <%=packageName%>.service.<%=entityClass%>Service;<% if (dto === 'mapstruct') { %>
import <%=packageName%>.service.dto.<%=instanceType%>;<% } %>
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;

/**
 * gRPC service providing CRUD methods for entity <%=entityClass%>.
 */
@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class <%=entityClass%>GrpcService extends <%=entityClass%>ServiceGrpc.<%=entityClass%>ServiceImplBase{

    private final <%=entityClass%>Service <%=entityInstance%>Service;

    public <%=entityClass%>GrpcService(<%=entityClass%>Service <%=entityInstance%>Service) {
        this.<%=entityInstance%>Service = <%=entityInstance%>Service;
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

    public void update<%=entityClass%>(<%=entityClass%>Proto request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=instanceType%> <%=instanceName%> = <%=entityClass%>ProtoMapper.<%=entityInstance%>ProtoTo<%=instanceType%>(request);
        <%=instanceName%> = <%=entityInstance%>Service.save(<%=instanceName%>);
        <%=entityClass%>Proto result = <%=entityClass%>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>);
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    public void getAll<%=entityClass%>s(<% if (pagination !== 'no') { %>PageRequest<% } else { %>Empty<% } %> request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=entityInstance%>Service.findAll(<% if (pagination !== 'no') { %>ProtobufUtil.pageRequestProtoToPageRequest(request)<% } %>)
            .forEach(<%=entityInstance%> -> responseObserver.onNext(<%=entityClass%>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=entityInstance%>)));
        responseObserver.onCompleted();
    }

    public void get<%=entityClass%>(<%=idProtoWrappedType%> request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=instanceType%> <%=instanceName%> = <%=entityInstance%>Service.findOne(request.getValue());
        if( <%=instanceName%> != null) {
            responseObserver.onNext(<%=entityClass%>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>));
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
        responseObserver.onCompleted();
    }

    public void delete<%=entityClass%>(<%=idProtoWrappedType%> request, StreamObserver<Empty> responseObserver) {
        <%=entityInstance%>Service.delete(request.getValue());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
