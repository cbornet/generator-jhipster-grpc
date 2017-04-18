package <%=packageName%>.grpc;

import <%=packageName%>.service.AuditEventService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.<%=idProtoWrappedType%>;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.Optional;

public class AuditGrpcService extends AuditServiceGrpc.AuditServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(AuditGrpcService.class);

    private final AuditEventService auditEventService;

    public AuditGrpcService(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @Override
    public void getAuditEvents(AuditRequest request, StreamObserver<AuditEvent> responseObserver) {
        Page<org.springframework.boot.actuate.audit.AuditEvent> auditEvents;
        if (request.hasFromDate() || request.hasToDate()) {
            LocalDateTime fromDate = request.hasFromDate() ? ProtobufMappers.dateProtoToLocalDate(request.getFromDate()).atTime(0, 0) : null;
            LocalDateTime toDate = request.hasToDate() ? ProtobufMappers.dateProtoToLocalDate(request.getToDate()).atTime(23, 59) : null;
            auditEvents= auditEventService.findByDates(fromDate, toDate, ProtobufMappers.pageRequestProtoToPageRequest(request.getPaginationParams()));
        } else {
            auditEvents = auditEventService.findAll(ProtobufMappers.pageRequestProtoToPageRequest(request.getPaginationParams()));
        }
        try {
            for(org.springframework.boot.actuate.audit.AuditEvent event : auditEvents) {
                responseObserver.onNext(ProtobufMappers.auditEventToAuditEventProto(event));
            }
            responseObserver.onCompleted();
        } catch (JsonProcessingException e) {
            log.error("Couldn't parse audit events", e);
            throw new StatusRuntimeException(Status.INTERNAL.withCause(e));
        }
    }

    @Override
    public void getAuditEvent(<%=idProtoWrappedType%> id, StreamObserver<AuditEvent> responseObserver) {
        Optional<org.springframework.boot.actuate.audit.AuditEvent> auditEvent = auditEventService.find(id.getValue());
        if (auditEvent.isPresent()) {
            try {
                responseObserver.onNext(ProtobufMappers.auditEventToAuditEventProto(auditEvent.get()));
                responseObserver.onCompleted();
            } catch (JsonProcessingException e) {
                responseObserver.onError(e);
            }
        } else {
            responseObserver.onError(Status.NOT_FOUND.asException());
        }
    }

}
