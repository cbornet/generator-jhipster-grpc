package <%=packageName%>.grpc;


import com.google.protobuf.Empty;
import com.google.protobuf.Int64Value;
import <%=packageName%>.repository.FooRepository;
import <%=packageName%>.service.FooService;
import <%=packageName%>.service.dto.FooDTO;
import <%=packageName%>.service.mapper.FooMapper;
import com.mycompany.myapp.service.mapper.FooProtoMapper;
import com.mycompany.myapp.web.grpc.entities.foo.FooProto;
import com.mycompany.myapp.web.grpc.entities.foo.FooServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.transaction.annotation.Transactional;

@GRpcService
public class FooGrpcService extends FooServiceGrpc.FooServiceImplBase{

    private final FooService fooService;
    private final FooProtoMapper fooProtoMapper;
    private final FooRepository fooRepository;
    private final FooMapper fooMapper;

    public FooGrpcService(FooService fooService, FooProtoMapper fooProtoMapper, FooRepository fooRepository, FooMapper fooMapper) {
        this.fooService = fooService;
        this.fooProtoMapper = fooProtoMapper;
        this.fooRepository = fooRepository;
        this.fooMapper = fooMapper;
    }

    @Override
    public void createFoo(FooProto request, StreamObserver<FooProto> responseObserver) {
        if( request.getIdOneofCase() == FooProto.IdOneofCase.ID) {
            responseObserver.onError(Status.ALREADY_EXISTS.asException());
            responseObserver.onCompleted();
        } else {
            updateFoo(request, responseObserver);
        }

    }

    public void updateFoo(FooProto request, StreamObserver<FooProto> responseObserver) {
        FooDTO fooDTO = fooProtoMapper.fooProtoToFooDTO(request);
        fooDTO = fooService.save(fooDTO);
        FooProto result = fooProtoMapper.fooDTOToFooProto(fooDTO);
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }

    /**
     */
    @Transactional(readOnly = true)
    public void getAllFoos(Empty request, StreamObserver<FooProto> responseObserver) {
        fooRepository.findAllAndStream()
            .map(fooMapper::fooToFooDTO)
            .forEach(foo -> responseObserver.onNext(fooProtoMapper.fooDTOToFooProto(foo)));
        responseObserver.onCompleted();
    }

    /**
     */
    public void getFoo(Int64Value request, StreamObserver<FooProto> responseObserver) {
        FooDTO fooDTO = fooService.findOne(request.getValue());
        if( fooDTO != null) {
            responseObserver.onNext(fooProtoMapper.fooDTOToFooProto(fooDTO));
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
        responseObserver.onCompleted();
    }

    /**
     */
    public void deleteFoo(Int64Value request, StreamObserver<Empty> responseObserver) {
        fooService.delete(request.getValue());
        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }
}
