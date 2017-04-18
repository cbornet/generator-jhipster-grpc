package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;

<% if (dto !== 'mapstruct') { %>
import <%=packageName%>.domain.<%=instanceType%>;<% } %>
<%_ for (idx in fields) {
    if(fields[idx].fieldIsEnum) { _%>
import <%=packageName%>.domain.enumeration.<%=fields[idx].fieldType%>;
<%_ }}_%>
import <%=packageName%>.grpc.ProtobufMappers;<% if (dto === 'mapstruct') { %>
import <%=packageName%>.service.dto.<%=instanceType%>;<% } %>
<%
  if (dto !== 'mapstruct') {
    var existingMapperImport = [];
    for (r of relationships) {
      if ((r.relationshipType == 'many-to-many' && r.ownerSide == true)|| r.relationshipType == 'many-to-one' ||(r.relationshipType == 'one-to-one' && r.ownerSide == true)){
        // if the entity is mapped twice, we should implement the mapping once
        if (existingMapperImport.indexOf(r.otherEntityProtoMapper) == -1 && r.otherEntityNameCapitalized !== entityClass) {
          existingMapperImport.push(r.otherEntityProtoMapper);
      %>import <%= r.otherEntityProtoMapper %>;
<% } } } } %>

import org.mapstruct.*;<% if (databaseType === 'cassandra') { %>

import java.util.UUID;<% } %>

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS, uses = {ProtobufMappers.class<%
  if (dto !== 'mapstruct') {
    var existingMappings = [];
    for (r of relationships) {
      if ((r.relationshipType == 'many-to-many' && r.ownerSide == true)|| r.relationshipType == 'many-to-one' ||(r.relationshipType == 'one-to-one' && r.ownerSide == true)){
        // if the entity is mapped twice, we should implement the mapping once
        if (existingMappings.indexOf(r.otherEntityNameCapitalized) == -1 && r.otherEntityNameCapitalized !== entityClass) {
          existingMappings.push(r.otherEntityNameCapitalized);
      %>, <%= r.otherEntityNameCapitalized %>ProtoMapper.class<% } } } } %>})
public interface <%=entityClass%>ProtoMapper {

<%_
// Proto -> entity mapping
if (dto  !== 'mapstruct') {
    for (idx in relationships) {
        const relationshipType = relationships[idx].relationshipType;
        const relationshipName = relationships[idx].relationshipName;
        const relationshipNamePlural = relationships[idx].relationshipNamePlural;
        const ownerSide = relationships[idx].ownerSide;
        if (relationshipType == 'many-to-one' || (relationshipType == 'one-to-one' && ownerSide == true)) { _%>
    @Mapping(source = "<%= relationshipName %>Id", target = "<%= relationshipName %>")<% } else if (relationshipType == 'many-to-many' && ownerSide == false) { %>
    @Mapping(target = "<%= relationshipNamePlural %>", ignore = true)<% } else if (relationshipType == 'one-to-many') { %>
    @Mapping(target = "<%= relationshipNamePlural %>", ignore = true)<% } else if (relationshipType == 'one-to-one' && ownerSide == false) { %>
    @Mapping(target = "<%= relationshipName %>", ignore = true)<% } } } %>
    <%=instanceType%> <%=entityInstance%>ProtoTo<%=instanceType%>(<%=entityClass%>Proto <%=entityInstance%>Proto);

    @AfterMapping
    // Set back null fields : necessary until https://github.com/google/protobuf/issues/2984 is fixed
    default void <%=entityInstance%>ProtoTo<%=instanceType%>(<%=entityClass%>Proto <%=entityInstance%>Proto, @MappingTarget <%=instanceType%> <%=instanceName%>) {
        if ( <%=entityInstance%>Proto == null ) {
            return;
        }

        if(<%=entityInstance%>Proto.getIdOneofCase() != <%=entityClass%>Proto.IdOneofCase.ID) {
            <%=instanceName%>.setId(null);
        }
<%_ for (f of fields) {
    let nullable = false;
    if (!f.isProtobufCustomType && !(f.fieldValidate && f.fieldValidateRules.indexOf('required') != -1)) {
        nullable = true;
    }_%>
    <%_ if (nullable) { _%>
        if(<%= entityInstance %>Proto.get<%= f.fieldInJavaBeanMethod %>OneofCase() != <%= entityClass %>Proto.<%= f.fieldInJavaBeanMethod %>OneofCase.<%= f.fieldNameUnderscored.toUpperCase() %>) {
            <%=instanceName%>.set<%= f.fieldInJavaBeanMethod %>(null);
        }
        <%_ if ((f.fieldDomainType === 'byte[]' || f.fieldDomainType === 'ByteBuffer') && f.fieldTypeBlobContent != 'text') { _%>
        if(<%= entityInstance %>Proto.get<%= f.fieldInJavaBeanMethod %>ContentTypeOneofCase() != <%= entityClass %>Proto.<%= f.fieldInJavaBeanMethod %>ContentTypeOneofCase.<%= f.fieldNameUnderscored.toUpperCase() %>_CONTENT_TYPE) {
            <%=instanceName%>.set<%= f.fieldInJavaBeanMethod %>ContentType(null);
        }
        <%_ } _%>
    <%_ } _%>
<%_ } _%>
<%_for (r of relationships) { _%>
    <%_ if ((r.relationshipType == 'many-to-one' || (r.relationshipType == 'one-to-one' && r.ownerSide == true)) && r.relationshipValidate !== true) { _%>
        if(<%= entityInstance %>Proto.get<%= r.relationshipNameCapitalized %>IdOneofCase() != <%= entityClass %>Proto.<%= r.relationshipNameCapitalized %>IdOneofCase.<%= r.relationshipNameUnderscored.toUpperCase() %>_ID) {
            <%=instanceName%>.set<%= r.relationshipNameCapitalized %><% if (dto === 'mapstruct') { %>Id<% } %>(null);
        }
    <%_ } _%>
<%_ } _%>
    }

    default <%=entityClass%>Proto.Builder create<%=entityClass%>Proto () {
        return <%=entityClass%>Proto.newBuilder();
    }

<%_
// entity -> Proto mapping
if (dto  !== 'mapstruct') {
    for (idx in relationships) {
        const relationshipType = relationships[idx].relationshipType;
        const relationshipName = relationships[idx].relationshipName;
        const ownerSide = relationships[idx].ownerSide;
        if (relationshipType == 'many-to-one' || (relationshipType == 'one-to-one' && ownerSide == true)) {
        _%>
    @Mapping(source = "<%= relationshipName %>.id", target = "<%= relationships[idx].relationshipFieldName %>Id")<% if (relationships[idx].otherEntityFieldCapitalized !='Id' && relationships[idx].otherEntityFieldCapitalized != '') { %>
    @Mapping(source = "<%= relationshipName %>.<%= relationships[idx].otherEntityField %>", target = "<%= relationships[idx].relationshipFieldName %><%= relationships[idx].otherEntityFieldCapitalized %>")<% } } } } %>
    <%=entityClass%>Proto.Builder <%=instanceName%>To<%=entityClass%>ProtoBuilder(<%=instanceType%> <%=instanceName%>);

    default <%=entityClass%>Proto <%=instanceName%>To<%=entityClass%>Proto(<%=instanceType%> <%=instanceName%>) {
        if (<%=instanceName%> == null) {
            return null;
        }
        return <%=instanceName%>To<%=entityClass%>ProtoBuilder(<%=instanceName%>).build();
    }

<%_ for (idx in fields) {
    if(fields[idx].fieldIsEnum) {
        var fieldType = fields[idx].fieldType;
        var fieldName = fieldType _%>
    <%=fieldType%>Proto convert<%=fieldType%>To<%=fieldType%>Proto(<%=fieldType%> enumValue);

    @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.NULL)
    <%=fieldType%> convert<%=fieldType%>ProtoTo<%=fieldType%>(<%=fieldType%>Proto enumValue);

<%_ }} _%>

<%_ if(databaseType === 'sql' && dto  !== 'mapstruct') { _%>
    default <%= entityClass %> <%= entityInstance %>FromId(Long id) {
        if (id == null) {
            return null;
        }
        <%= entityClass %> <%= entityInstance %> = new <%= entityClass %>();
        <%= entityInstance %>.setId(id);
        return <%= entityInstance %>;
    }
<%_ } _%>
}
