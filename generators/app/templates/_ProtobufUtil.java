package <%=packageName%>.grpc;

import com.google.protobuf.Timestamp;
import <%=packageName%>.grpc.Date;
import <%=packageName%>.grpc.Decimal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;


public abstract class ProtobufUtil {
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
        return Timestamp.newBuilder()
            .setSeconds(zonedDateTime.toInstant().getEpochSecond())
            .setNanos(zonedDateTime.getNano())
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
}
