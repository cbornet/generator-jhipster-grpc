package <%=packageName%>.grpc;

import <%=packageName%>.service.AuditEventService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.Int64Value;
import io.grpc.Status;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;

public class AuditGrpcService extends RxAuditServiceGrpc.AuditServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(AuditGrpcService.class);

    private final AuditEventService auditEventService;

    public AuditGrpcService(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @Override
    public Flowable<AuditEvent> getAuditEvents(Single<AuditRequest> request) {
        return request
            .map( auditRequest -> {
                if (auditRequest.hasFromDate() || auditRequest.hasToDate()) {
                    Instant fromDate = auditRequest.hasFromDate() ?
                        ProtobufMappers
                            .dateProtoToLocalDate(auditRequest.getFromDate())
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                        : null;
                    Instant toDate = auditRequest.hasToDate() ?
                        ProtobufMappers
                            .dateProtoToLocalDate(auditRequest.getToDate())
                            .atStartOfDay(ZoneId.systemDefault())
                            .plusDays(1)
                            .toInstant()
                        : null;
                    return auditEventService.findByDates(fromDate, toDate, ProtobufMappers.pageRequestProtoToPageRequest(auditRequest.getPaginationParams()));
                } else {
                    return auditEventService.findAll(ProtobufMappers.pageRequestProtoToPageRequest(auditRequest.getPaginationParams()));
                }
            })
            .flatMapPublisher(Flowable::fromIterable)
            .map(auditEvent ->  {
                try {
                    return ProtobufMappers.auditEventToAuditEventProto(auditEvent);
                } catch (JsonProcessingException e) {
                    log.error("Couldn't parse audit event", e);
                    throw Status.INTERNAL.withCause(e).asRuntimeException();
                }
            });
    }

    @Override
    public Single<AuditEvent> getAuditEvent(Single<Int64Value> request) {
        return request
            .map(Int64Value::getValue)
            .map(id -> auditEventService.find(id).orElseThrow(Status.NOT_FOUND::asException))
            .map(auditEvent -> {
                try {
                    return ProtobufMappers.auditEventToAuditEventProto(auditEvent);
                } catch (JsonProcessingException e) {
                    throw Status.INTERNAL.withCause(e).asRuntimeException();
                }
            });
    }

}
