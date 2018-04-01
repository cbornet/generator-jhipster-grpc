<%_ 
if (databaseType === 'sql') { 
    let idProtoWrappedType = 'Int64Value';
} else {
    let idProtoWrappedType = 'StringValue';
}
_%>
package <%= packageName %>.grpc;

import <%= packageName %>.service.AuditEventService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.<%= idProtoWrappedType %>;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;

public class AuditGrpcService extends ReactorAuditServiceGrpc.AuditServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(AuditGrpcService.class);

    private final AuditEventService auditEventService;

    public AuditGrpcService(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @Override
    public Flux<AuditEvent> getAuditEvents(Mono<AuditRequest> request) {
        return request
            .flatMapIterable( auditRequest -> {
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
    public Mono<AuditEvent> getAuditEvent(Mono<<%= idProtoWrappedType %>> request) {
        return request
            .map(<%= idProtoWrappedType %>::getValue)
            .map(id -> auditEventService.find(id).orElseThrow(Status.NOT_FOUND::asRuntimeException))
            .map(auditEvent -> {
                try {
                    return ProtobufMappers.auditEventToAuditEventProto(auditEvent);
                } catch (JsonProcessingException e) {
                    throw Status.INTERNAL.withCause(e).asRuntimeException();
                }
            });
    }

}
