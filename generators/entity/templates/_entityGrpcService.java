package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;


import com.google.protobuf.Empty;
import com.google.protobuf.<%=idProtoWrappedType%>;<% if (['jwt', 'oauth2'].includes(authenticationType)) { %>
import <%=packageName%>.grpc.AuthenticationInterceptor;<% } %>
import <%=packageName%>.repository.<%=entityClass%>Repository;
import <%=packageName%>.service.<%=entityClass%>Service;
import <%=packageName%>.service.dto.<%=entityClass%>DTO;
import <%=packageName%>.service.mapper.<%=entityClass%>Mapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.transaction.annotation.Transactional;

/**
 * gRPC service providing CRUD methods for entity <%=entityClass%>.
 */
@GRpcService<% if (['jwt', 'oauth2'].includes(authenticationType)) { %>(interceptors = {AuthenticationInterceptor.class})<% } %>
public class <%=entityClass%>GrpcService extends <%=entityClass%>ServiceGrpc.<%=entityClass%>ServiceImplBase{

    private final <%=entityClass%>Service <%=entityInstance%>Service;
    private final <%=entityClass%>Repository <%=entityInstance%>Repository;
    private final <%=entityClass%>Mapper <%=entityInstance%>Mapper;

    public <%=entityClass%>GrpcService(<%=entityClass%>Service <%=entityInstance%>Service, <%=entityClass%>Repository <%=entityInstance%>Repository, <%=entityClass%>Mapper <%=entityInstance%>Mapper) {
        this.<%=entityInstance%>Service = <%=entityInstance%>Service;
        this.<%=entityInstance%>Repository = <%=entityInstance%>Repository;
        this.<%=entityInstance%>Mapper = <%=entityInstance%>Mapper;
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
        <%=entityClass%>DTO <%=entityInstance%>DTO = <%=entityClass%>ProtoMapper.<%=entityInstance%>ProtoTo<%=entityClass%>DTO(request);
        <%=entityInstance%>DTO = <%=entityInstance%>Service.save(<%=entityInstance%>DTO);
        <%=entityClass%>Proto result = <%=entityClass%>ProtoMapper.<%=entityInstance%>DTOTo<%=entityClass%>Proto(<%=entityInstance%>DTO);
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    /*@Transactional(readOnly = true)
    public void getAll<%=entityClass%>s(Empty request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=entityInstance%>Repository.findAllAndStream()
            .map(<%=entityInstance%>Mapper::<%=entityInstance%>To<%=entityClass%>DTO)
            .forEach(<%=entityInstance%> -> responseObserver.onNext(<%=entityClass%>ProtoMapper.<%=entityInstance%>DTOTo<%=entityClass%>Proto(<%=entityInstance%>)));
        responseObserver.onCompleted();
    }*/

    public void getAll<%=entityClass%>s(Empty request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=entityInstance%>Service.findAll()
            .forEach(<%=entityInstance%> -> responseObserver.onNext(<%=entityClass%>ProtoMapper.<%=entityInstance%>DTOTo<%=entityClass%>Proto(<%=entityInstance%>)));
        responseObserver.onCompleted();
    }

    public void get<%=entityClass%>(<%=idProtoWrappedType%> request, StreamObserver<<%=entityClass%>Proto> responseObserver) {
        <%=entityClass%>DTO <%=entityInstance%>DTO = <%=entityInstance%>Service.findOne(request.getValue());
        if( <%=entityInstance%>DTO != null) {
            responseObserver.onNext(<%=entityClass%>ProtoMapper.<%=entityInstance%>DTOTo<%=entityClass%>Proto(<%=entityInstance%>DTO));
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
