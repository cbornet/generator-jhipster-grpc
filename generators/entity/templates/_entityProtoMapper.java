package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;

import com.google.protobuf.ByteString;
//import org.mapstruct.Mapper;

<%_ for (idx in fields) {
    if(fields[idx].fieldIsEnum) { _%>
import <%=packageName%>.domain.enumeration.<%=fields[idx].fieldType%>;
<%_ }}_%>
import <%=packageName%>.service.dto.<%=entityClass%>DTO;
import <%=packageName%>.grpc.ProtobufUtil;

import java.util.List;

//@Mapper(componentModel = "spring")
public abstract class <%=entityClass%>ProtoMapper {
    public static <%=entityClass%>DTO <%=entityInstance%>ProtoTo<%=entityClass%>DTO(<%=entityClass%>Proto <%=entityInstance%>Proto) {
        if ( <%=entityInstance%>Proto == null ) {
            return null;
        }
        <%=entityClass%>DTO <%=entityInstance%>DTO = new <%=entityClass%>DTO();

        if(<%=entityInstance%>Proto.getIdOneofCase() == <%=entityClass%>Proto.IdOneofCase.ID) {
            <%=entityInstance%>DTO.setId( <%=entityInstance%>Proto.getId() );
        }
<%_ for (idx in fields) {
    var nullable = false;
    var fieldValidate = fields[idx].fieldValidate;
    var fieldValidateRules = fields[idx].fieldValidateRules;
    var fieldType = fields[idx].fieldType;
    var fieldIsEnum = fields[idx].fieldIsEnum;
    var isProtobufCustomType = fields[idx].isProtobufCustomType;
    var fieldInJavaBeanMethod = fields[idx].fieldInJavaBeanMethod;
    var fieldNameUnderscored = fields[idx].fieldNameUnderscored;
    if (!isProtobufCustomType && !(fieldValidate && fieldValidateRules.indexOf('required') != -1)) {
        nullable = true;
    }_%>
    <%_ if (nullable) { _%>
        if(<%= entityInstance %>Proto.get<%= fieldInJavaBeanMethod %>OneofCase() == <%= entityClass %>Proto.<%= fieldInJavaBeanMethod %>OneofCase.<%= fieldNameUnderscored.toUpperCase() %>) {
    <% } -%>
    <%_ if (isProtobufCustomType) { _%>
        if(<%= entityInstance %>Proto.has<%= fieldInJavaBeanMethod %>()) {
    <% } -%>
        <%=entityInstance%>DTO.set<%= fieldInJavaBeanMethod %>(<% if (isProtobufCustomType) { %>ProtobufUtil.<% } %><% if(fieldType === 'ZonedDateTime') { %>timestampToZonedDateTime(<% } %><% if(fieldType === 'LocalDate') { %>dateProtoToLocalDate(<% } %><% if(fieldType === 'BigDecimal') { %>decimalProtoToBigDecimal(<% } %><% if(fieldIsEnum) { %><%=fieldType%>.valueOf(<% } %><%=entityInstance%>Proto.get<%= fieldInJavaBeanMethod %>()<% if(fieldIsEnum) { %>.toString()<% } %><% if(fieldType === 'byte[]') { %>.toByteArray()<% } %>)<% if(fieldIsEnum || isProtobufCustomType) { %>)<% } %>;
    <%_ if (nullable || isProtobufCustomType) { _%>
        }
    <%_ } _%>
<%_ } _%>
        return <%=entityInstance%>DTO;
    }

    //public abstract List<FooDTO> fooProtosToFooDTOs(List<FooProto> fooProtos);

    public static <%=entityClass%>Proto <%=entityInstance%>DTOTo<%=entityClass%>Proto(<%=entityClass%>DTO <%=entityInstance%>DTO) {
        if ( <%=entityInstance%>DTO == null ) {

            return null;
        }
        <%=entityClass%>Proto.Builder <%=entityInstance%>ProtoBuilder = <%=entityClass%>Proto.newBuilder();

        if (<%=entityInstance%>DTO.getId() != null) {
            <%=entityInstance%>ProtoBuilder.setId(<%=entityInstance%>DTO.getId());
        }
<%_ for (idx in fields) {
    var nullable = false;
    var fieldValidate = fields[idx].fieldValidate;
    var fieldValidateRules = fields[idx].fieldValidateRules;
    var fieldType = fields[idx].fieldType;
    var fieldIsEnum = fields[idx].fieldIsEnum;
    var isProtobufCustomType = fields[idx].isProtobufCustomType;
    var fieldInJavaBeanMethod = fields[idx].fieldInJavaBeanMethod;
    var fieldNameUnderscored = fields[idx].fieldNameUnderscored;
    if (!isProtobufCustomType && !(fieldValidate && fieldValidateRules.indexOf('required') != -1)) {
        nullable = true;
    }_%>
        if (fooDTO.get<%= fieldInJavaBeanMethod %>() != null) {
            <%=entityInstance%>ProtoBuilder.set<%= fieldInJavaBeanMethod %>(<% if(fieldType === 'byte[]') { %>ByteString.copyFrom(<% } %><% if (isProtobufCustomType) { %>ProtobufUtil.<% } %><% if(fieldType === 'ZonedDateTime') { %>zonedDateTimeToTimestamp(<% } %><% if(fieldType === 'LocalDate') { %>localDateToDateProto(<% } %><% if(fieldType === 'BigDecimal') { %>bigDecimalToDecimalProto(<% } %><% if(fieldIsEnum) { %><%=fieldType%>Proto.valueOf(<% } %><%=entityInstance%>DTO.get<%= fieldInJavaBeanMethod %>()<% if(fieldIsEnum) { %>.toString()<% } %>)<% if(fieldIsEnum || isProtobufCustomType || fieldType === 'byte[]') { %>)<% } %>;
        }
<%_ } _%>
        return fooProtoBuilder.build();
    }

    //public abstract List<FooProto> fooDTOsToFooProtos(List<FooDTO> fooDTOs);
}
