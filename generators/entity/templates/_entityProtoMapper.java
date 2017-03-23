package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;

<% if (dto !== 'mapstruct') { %>
import <%=packageName%>.domain.<%=instanceType%>;<% } %>
<%_ for (idx in fields) {
    if(fields[idx].fieldIsEnum) { _%>
import <%=packageName%>.domain.enumeration.<%=fields[idx].fieldType%>;
<%_ }}_%>
import <%=packageName%>.grpc.ProtobufUtil;<% if (dto === 'mapstruct') { %>
import <%=packageName%>.service.dto.<%=instanceType%>;<% } %>

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;<% if (databaseType === 'cassandra') { %>

import java.util.UUID;<% } %>

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class <%=entityClass%>ProtoMapper extends ProtobufUtil {

    public <%=instanceType%> <%=entityInstance%>ProtoTo<%=instanceType%>(<%=entityClass%>Proto <%=entityInstance%>Proto) {
        if ( <%=entityInstance%>Proto == null ) {
            return null;
        }
        <%=instanceType%> <%=instanceName%> = new <%=instanceType%>();

        if(<%=entityInstance%>Proto.getIdOneofCase() == <%=entityClass%>Proto.IdOneofCase.ID) {
            <%=instanceName%>.setId(<% if (databaseType === 'cassandra') { %>UUID.fromString(<% } %><%=entityInstance%>Proto.getId()<% if (databaseType === 'cassandra') { %>)<% } %>);
        }
<%_ for (idx in fields) {
    var nullable = false;
    var fieldValidate = fields[idx].fieldValidate;
    var fieldValidateRules = fields[idx].fieldValidateRules;
    var fieldDomainType = fields[idx].fieldDomainType;
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
        <%=instanceName%>.set<%= fieldInJavaBeanMethod %>(<% if (isProtobufCustomType) { %>ProtobufUtil.<% } %><% if(fieldDomainType === 'ZonedDateTime') { %>timestampToZonedDateTime(<% } %><% if(fieldDomainType === 'UUID') { %>UUID.fromString(<% } %><% if(fieldDomainType === 'LocalDate') { %>dateProtoToLocalDate(<% } %><% if(fieldDomainType === 'BigDecimal') { %>decimalProtoToBigDecimal(<% } %><% if(fieldIsEnum) { %><%=fieldDomainType%>.valueOf(<% } %><%=entityInstance%>Proto.get<%= fieldInJavaBeanMethod %>()<% if(fieldIsEnum) { %>.toString()<% } %><% if(fieldDomainType === 'byte[]') { %>.toByteArray()<% } %><% if(fieldDomainType === 'ByteBuffer') { %>.asReadOnlyByteBuffer()<% } %>)<% if(fieldIsEnum || isProtobufCustomType || fieldDomainType === 'UUID') { %>)<% } %>;
    <%_ if (nullable || isProtobufCustomType) { _%>
        }
    <%_ } _%>
<%_ } _%>
        return <%=instanceName%>;
    }

<%_ for (idx in fields) {
    if(fields[idx].fieldIsEnum) {
        var fieldType = fields[idx].fieldType;
        var fieldName = fieldType _%>
    <%=fieldType%>Proto convert<%=fieldType%>To<%=fieldType%>Proto(<%=fieldType%> enumValue) {
        return <%=fieldType%>Proto.valueOf(enumValue.toString());
    }

<%_ }} _%>
    <%=entityClass%>Proto.Builder create<%=entityClass%>Proto () {
        return <%=entityClass%>Proto.newBuilder();
    }

    abstract <%=entityClass%>Proto.Builder <%=instanceName%>To<%=entityClass%>ProtoBuilder(<%=instanceType%> <%=instanceName%>);

    public <%=entityClass%>Proto <%=instanceName%>To<%=entityClass%>Proto(<%=instanceType%> <%=instanceName%>) {
        if (<%=instanceName%> == null) {
            return null;
        }
        return <%=instanceName%>To<%=entityClass%>ProtoBuilder(<%=instanceName%>).build();
    }

}
