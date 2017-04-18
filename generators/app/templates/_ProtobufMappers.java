package <%=packageName%>.grpc;

<%_ if ((databaseType === 'sql' || databaseType === 'mongodb') && !skipUserManagement) { _%>
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
<%_ } _%>
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class ProtobufMappers {

    private static final int DEFAULT_MAX_PAGE_SIZE = 2000;
    private static final Pageable DEFAULT_PAGE_REQUEST = new org.springframework.data.domain.PageRequest(0, 20);

    public static LocalDate dateProtoToLocalDate(Date date) {
        return date == null ? null : LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public static Date localDateToDateProto(LocalDate date) {
        return date == null ? null : Date.newBuilder()
            .setYear(date.getYear())
            .setMonth(date.getMonthValue())
            .setDay(date.getDayOfMonth())
            .build();
    }

    public static ZonedDateTime timestampToZonedDateTime(Timestamp timestamp) {
        return timestamp == null ? null : ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()),
            ZoneId.of("UTC")
        );
    }

    public static Timestamp zonedDateTimeToTimestamp(ZonedDateTime zonedDateTime) {
        return zonedDateTime == null ? null : instantToTimestamp(zonedDateTime.toInstant());
    }

    public static Timestamp instantToTimestamp(Instant instant) {
        return instant == null ? null :  Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    public static Timestamp dateToTimestamp(java.util.Date date) {
        return date == null ? null : instantToTimestamp(date.toInstant());
    }

    public static BigDecimal decimalProtoToBigDecimal(Decimal decimal) {
        return decimal == null ? null : BigDecimal.valueOf(decimal.getUnscaledVal(), decimal.getScale());
    }

    public static Decimal bigDecimalToDecimalProto(BigDecimal decimal) {
        return decimal == null ? null : Decimal.newBuilder()
            .setUnscaledVal(decimal.unscaledValue().longValue())
            .setScale(decimal.scale())
            .build();
    }

    public static ByteString bytesToByteString(byte[] bytes) {
        return bytes == null ? null : ByteString.copyFrom(bytes);
    }

    public static byte[] ByteStringToBytes(ByteString byteString) {
        return byteString == null ? null : byteString.toByteArray();
    }

    public static ByteString byteBufferToByteString(ByteBuffer buffer) {
        return buffer == null ? null : ByteString.copyFrom(buffer);
    }

    public static ByteBuffer byteStringToByteBuffer(ByteString byteString) {
        return byteString == null ? null : byteString.asReadOnlyByteBuffer();
    }

    public static String uuidToString(UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    public static UUID stringToUuid(String uuid) {
        return uuid == null || uuid.isEmpty() ? null : UUID.fromString(uuid);
    }

    public static org.springframework.data.domain.PageRequest pageRequestProtoToPageRequest(PageRequest pageRequestProto) {
        if (pageRequestProto == null) {
            return null;
        }
        int page = pageRequestProto.getPage();
        int pageSize = pageRequestProto.getSize();

        // Limit lower bound
        pageSize = pageSize < 1 ? DEFAULT_PAGE_REQUEST.getPageSize() : pageSize;
        // Limit upper bound
        pageSize = pageSize > DEFAULT_MAX_PAGE_SIZE ? DEFAULT_MAX_PAGE_SIZE : pageSize;

        List<Sort.Order> orders = pageRequestProto.getOrdersList().stream()
            .map( o -> new Sort.Order(Sort.Direction.fromString(o.getDirection().toString()), o.getProperty()))
            .collect(Collectors.toList());
        Sort sort = orders.isEmpty() ? null : new Sort(orders);

        return new org.springframework.data.domain.PageRequest(page, pageSize, sort);
    }
    <%_ if (authenticationType == 'session') { _%>

    public static PersistentToken persistentTokenToPersistentTokenProto(<%=packageName%>.domain.PersistentToken token) {
        if (token == null) {
            return null;
        }
        PersistentToken.Builder builder = PersistentToken.newBuilder().setTokenDate(<% if (databaseType == 'cassandra') { %>dateToTimestamp(token.getTokenDate())<% } else { %>localDateToDateProto(token.getTokenDate())<% } %>);
        if (token.getSeries() != null) {
            builder.setSeries(token.getSeries());
        }
        if (token.getIpAddress() != null) {
            builder.setIpAddress(token.getIpAddress());
        }
        if (token.getUserAgent() != null) {
            builder.setUserAgent(token.getUserAgent());
        }
        return builder.build();
    }
    <%_ } _%>
    <%_ if ((databaseType === 'sql' || databaseType === 'mongodb') && !skipUserManagement) { _%>

    public static AuditEvent auditEventToAuditEventProto(org.springframework.boot.actuate.audit.AuditEvent event) throws JsonProcessingException {
        if (event == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        AuditEvent.Builder builder =  AuditEvent.newBuilder()
            .setTimestamp(dateToTimestamp(event.getTimestamp()))
            .setData(mapper.writeValueAsString(event.getData()));
        if (event.getPrincipal() != null) {
            builder.setPrincipal(event.getPrincipal());
        }
        if(event.getType() != null) {
            builder.setType(event.getType());
        }
        return builder.build();
    }
    <%_ } _%>

}
