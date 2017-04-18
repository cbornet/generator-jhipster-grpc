package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;

<% if (dto !== 'mapstruct') { %>
import <%=packageName%>.domain.<%=instanceType%>;<% } %>
<%_ for (idx in fields) {
    if(fields[idx].fieldIsEnum) { _%>
import <%=packageName%>.domain.enumeration.<%=fields[idx].fieldType%>;
<%_ }}_%>
import <%=packageName%>.grpc.ProtobufUtil;<% if (dto === 'mapstruct') { %>
import <%=packageName%>.service.dto.<%=instanceType%>;<% } %>

import org.mapstruct.*;<% if (databaseType === 'cassandra') { %>

import java.util.UUID;<% } %>

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public abstract class <%=entityClass%>ProtoMapper extends ProtobufUtil {

    abstract <%=instanceType%> <%=entityInstance%>ProtoTo<%=instanceType%>(<%=entityClass%>Proto <%=entityInstance%>Proto);

    @AfterMapping
    // Set back null fields : necessary until https://github.com/google/protobuf/issues/2984 is fixed
    public void <%=entityInstance%>ProtoTo<%=instanceType%>(<%=entityClass%>Proto <%=entityInstance%>Proto, @MappingTarget <%=instanceType%> <%=instanceName%>) {
        if ( <%=entityInstance%>Proto == null ) {
            return;
        }

        if(<%=entityInstance%>Proto.getIdOneofCase() != <%=entityClass%>Proto.IdOneofCase.ID) {
            <%=instanceName%>.setId(null);
        }
<%_ for (idx in fields) {
    var nullable = false;
    var fieldValidate = fields[idx].fieldValidate;
    var fieldValidateRules = fields[idx].fieldValidateRules;
    var fieldDomainType = fields[idx].fieldDomainType;
    var fieldTypeBlobContent = fields[idx].fieldTypeBlobContent;
    var isProtobufCustomType = fields[idx].isProtobufCustomType;
    var fieldInJavaBeanMethod = fields[idx].fieldInJavaBeanMethod;
    var fieldNameUnderscored = fields[idx].fieldNameUnderscored;
    if (!isProtobufCustomType && !(fieldValidate && fieldValidateRules.indexOf('required') != -1)) {
        nullable = true;
    }_%>
    <%_ if (nullable) { _%>
        if(<%= entityInstance %>Proto.get<%= fieldInJavaBeanMethod %>OneofCase() != <%= entityClass %>Proto.<%= fieldInJavaBeanMethod %>OneofCase.<%= fieldNameUnderscored.toUpperCase() %>) {
            <%=instanceName%>.set<%= fieldInJavaBeanMethod %>(null);
        }
        <%_ if ((fieldDomainType === 'byte[]' || fieldDomainType === 'ByteBuffer') && fieldTypeBlobContent != 'text') { _%>
        if(<%= entityInstance %>Proto.get<%= fieldInJavaBeanMethod %>ContentTypeOneofCase() != <%= entityClass %>Proto.<%= fieldInJavaBeanMethod %>ContentTypeOneofCase.<%= fieldNameUnderscored.toUpperCase() %>_CONTENT_TYPE) {
            <%=instanceName%>.set<%= fieldInJavaBeanMethod %>ContentType(null);
        }
        <%_ } _%>
    <%_ } _%>
<%_ } _%>
    }

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

<%_ for (idx in fields) {
    if(fields[idx].fieldIsEnum) {
        var fieldType = fields[idx].fieldType;
        var fieldName = fieldType _%>
    abstract <%=fieldType%>Proto convert<%=fieldType%>To<%=fieldType%>Proto(<%=fieldType%> enumValue);

    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    abstract <%=fieldType%> convert<%=fieldType%>ProtoTo<%=fieldType%>(<%=fieldType%>Proto enumValue);

<%_ }} _%>
}
