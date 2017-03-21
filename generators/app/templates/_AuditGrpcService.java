package <%=packageName%>.grpc;

import <%=packageName%>.service.AuditEventService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Int64Value;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Optional;

public class AuditGrpcService extends AuditServiceGrpc.AuditServiceImplBase {

    private final AuditEventService auditEventService;

    public AuditGrpcService(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    public void getAuditEvents(AuditRequest request, StreamObserver<AuditEvent> responseObserver) {
        Page<org.springframework.boot.actuate.audit.AuditEvent> auditEvents;
        if (request.hasFromDate() || request.hasToDate()) {
            LocalDateTime fromDate = request.hasFromDate() ? ProtobufUtil.dateProtoToLocalDate(request.getFromDate()).atTime(0, 0) : null;
            LocalDateTime toDate = request.hasToDate() ? ProtobufUtil.dateProtoToLocalDate(request.getToDate()).atTime(23, 59) : null;
            auditEvents= auditEventService.findByDates(fromDate, toDate, ProtobufUtil.pageRequestProtoToPageRequest(request.getPaginationParams()));
        } else {
            auditEvents = auditEventService.findAll(ProtobufUtil.pageRequestProtoToPageRequest(request.getPaginationParams()));
        }
        try {
            for(org.springframework.boot.actuate.audit.AuditEvent event : auditEvents) {
                responseObserver.onNext(auditEventToAuditEventProto(event));
            }
            responseObserver.onCompleted();
        } catch (JsonProcessingException e) {
            responseObserver.onError(e);
        }
    }

    public void getAuditEvent(Int64Value id, StreamObserver<AuditEvent> responseObserver) {
        Optional<org.springframework.boot.actuate.audit.AuditEvent> auditEvent = auditEventService.find(id.getValue());
        if (auditEvent.isPresent()) {
            try {
                responseObserver.onNext(auditEventToAuditEventProto(auditEvent.get()));
                responseObserver.onCompleted();
            } catch (JsonProcessingException e) {
                responseObserver.onError(e);
            }
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
    }

    public static AuditEvent auditEventToAuditEventProto(org.springframework.boot.actuate.audit.AuditEvent event) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return AuditEvent.newBuilder()
            .setTimestamp(ProtobufUtil.instantToTimestamp(event.getTimestamp().toInstant()))
            .setPrincipal(event.getPrincipal())
            .setType(event.getType())
            .setData(mapper.writeValueAsString(event.getData()))
            .build();
    }

}
