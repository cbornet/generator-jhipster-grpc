package <%=packageName%>.grpc;

import com.google.protobuf.Timestamp;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
public abstract class ProtobufUtil {

    private static final int DEFAULT_MAX_PAGE_SIZE = 2000;
    private static final Pageable DEFAULT_PAGE_REQUEST = new org.springframework.data.domain.PageRequest(0, 20);

    public static LocalDate dateProtoToLocalDate(Date date) {
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }

    public static Date localDateToDateProto(LocalDate date) {
        return Date.newBuilder()
            .setYear(date.getYear())
            .setMonth(date.getMonthValue())
            .setDay(date.getDayOfMonth())
            .build();
    }

    public static ZonedDateTime timestampToZonedDateTime(Timestamp timestamp) {
        return ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()),
            ZoneId.of("UTC")
        );
    }

    public static Timestamp zonedDateTimeToTimestamp(ZonedDateTime zonedDateTime) {
        return instantToTimestamp(zonedDateTime.toInstant());
    }

    public static Timestamp instantToTimestamp(Instant instant) {
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    public static BigDecimal decimalProtoToBigDecimal(Decimal decimal) {
        return BigDecimal.valueOf(decimal.getUnscaledVal(), decimal.getScale());
    }

    public static Decimal bigDecimalToDecimalProto(BigDecimal decimal) {
        return Decimal.newBuilder()
            .setUnscaledVal(decimal.unscaledValue().longValue())
            .setScale(decimal.scale())
            .build();
    }

    public static org.springframework.data.domain.PageRequest pageRequestProtoToPageRequest(PageRequest pageRequestProto) {
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

}
