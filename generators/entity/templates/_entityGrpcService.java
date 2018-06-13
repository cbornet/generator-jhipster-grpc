package <%= packageName %>.grpc.entity.<%=entityUnderscoredName%>;

import <%= packageName %>.grpc.AuthenticationInterceptor;
<%_ if (pagination !== 'no') { _%>
import <%= packageName %>.grpc.PageRequest<% if (jpaMetamodelFiltering) { %>AndFilters<% } %>;
<%_ } _%>
<%_ if (pagination !== 'no' || databaseType === 'cassandra') { _%>
import <%= packageName %>.grpc.ProtobufMappers;
<%_ } _%>
<%_ if (jpaMetamodelFiltering) { _%>
    <%_ if (pagination === 'no') { _%>
import <%= packageName %>.grpc.QueryFilter;
    <%_ } _%>
import <%= packageName %>.service.<%= entityClass %>QueryService;
<%_ } _%>
import <%= packageName %>.service.<%= entityClass %>Service;
<%_ if (jpaMetamodelFiltering) { _%>
import <%= packageName %>.service.dto.<%= entityClass %>Criteria;
<%_ } _%>

import com.google.protobuf.Empty;
import com.google.protobuf.<%=idProtoWrappedType%>;
<%_ if (searchEngine == 'elasticsearch' && this.databaseType !== 'sql') { _%>
import com.google.protobuf.StringValue;
<%_ } _%>
import io.grpc.Status;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<%_ if (jpaMetamodelFiltering) { _%>
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
    <%_ if (pagination !== 'no') { _%>
import org.springframework.data.util.Pair;
    <%_ } _%>
import org.springframework.format.support.FormattingConversionService;
import org.springframework.validation.DataBinder;
<%_ } _%>
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

<%_ if (jpaMetamodelFiltering && pagination !== 'no') { _%>
import java.util.List;
<%_ } _%>
import java.util.Optional;
<%_ if (databaseType === 'cassandra') { _%>
import java.util.UUID;
<%_ } _%>
<%_ if (jpaMetamodelFiltering && pagination !== 'no') { _%>
import java.util.stream.Collectors;
<%_ } _%>

/**
 * gRPC service providing CRUD methods for entity <%= entityClass %>.
 */
@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class <%= entityClass %>GrpcService extends Reactor<%= entityClass %>ServiceGrpc.<%= entityClass %>ServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(<%= entityClass %>GrpcService.class);

    private final <%= entityClass %>Service <%= entityInstance %>Service;

    <%_ if (jpaMetamodelFiltering) { _%>
    private final <%= entityClass %>QueryService <%= entityInstance %>QueryService;

    private final FormattingConversionService conversionService;

    <%_ } _%>
    private final <%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper;

    public <%= entityClass %>GrpcService(<%= entityClass %>Service <%= entityInstance %>Service, <% if (jpaMetamodelFiltering) { %><%= entityClass %>QueryService <%= entityInstance %>QueryService, FormattingConversionService conversionService, <% } %><%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper) {
        this.<%= entityInstance %>Service = <%= entityInstance %>Service;
        <%_ if (jpaMetamodelFiltering) { _%>
        this.<%= entityInstance %>QueryService = <%= entityInstance %>QueryService;
        this.conversionService = conversionService;
        <%_ } _%>
        this.<%= entityInstance %>ProtoMapper = <%= entityInstance %>ProtoMapper;
    }

    @Override
    public Mono<<%= entityClass %>Proto> create<%= entityClass %>(Mono<<%= entityClass %>Proto> request) {
        return request
            .doOnSuccess(<%= entityInstance %>Proto -> log.debug("REST request to save <%= entityClass %> : {}", <%= entityInstance %>Proto))
            .filter(<%= entityInstance %>Proto -> <%= entityInstance %>Proto.getIdOneofCase() != <%= entityClass %>Proto.IdOneofCase.ID)
            .switchIfEmpty(Mono.error(Status.ALREADY_EXISTS.asRuntimeException()))
            .map(<%= entityInstance %>ProtoMapper::<%= entityInstance %>ProtoTo<%= instanceType %>)
            <%_ if (databaseType === 'cassandra') { _%>
            .doOnSuccess(<%= entityInstance %> -> <%= entityInstance %>.setId(UUID.randomUUID()))
            <%_ } _%>
            .map(<%= entityInstance %>Service::save)
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Mono<<%= entityClass %>Proto> update<%= entityClass %>(Mono<<%= entityClass %>Proto> request) {
        return request
            .doOnSuccess(<%= instanceName %> -> log.debug("REST request to update <%= entityClass %> : {}", <%= instanceName %>))
            .filter(<%= entityInstance %>Proto -> <%= entityInstance %>Proto.getIdOneofCase() == <%= entityClass %>Proto.IdOneofCase.ID)
            .switchIfEmpty(Mono.error(Status.INVALID_ARGUMENT.asRuntimeException()))
            .map(<%= entityInstance %>ProtoMapper::<%= entityInstance %>ProtoTo<%= instanceType %>)
            .map(<%= entityInstance %>Service::save)
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Flux<<%= entityClass %>Proto> getAll<%= entityClassPlural %>(<% if (pagination !== 'no') { %>Mono<PageRequest<% if (jpaMetamodelFiltering) { %>AndFilters<% } } else { if (jpaMetamodelFiltering) {  %>Flux<QueryFilter<% } else { %>Mono<Empty<% }} %>> request) {
        return request
            <%_ if (pagination !== 'no') { _%>
                <%_ if (jpaMetamodelFiltering) { _%>
            .map(pageRequestAndFilters -> {
                List<PropertyValue> pvs = pageRequestAndFilters.getQueryFiltersList().stream()
                    .map(filter -> new PropertyValue(filter.getProperty(), filter.getValue().split(",")))
                    .collect(Collectors.toList());
                <%= entityClass %>Criteria criteria = new <%= entityClass %>Criteria();
                DataBinder binder = new DataBinder(criteria);
                binder.setConversionService(conversionService);
                binder.bind(new MutablePropertyValues(pvs));
                return Pair.of(criteria, ProtobufMappers.pageRequestProtoToPageRequest(pageRequestAndFilters.getPageRequest()));
            })
            .doOnSuccess(pair -> log.debug("REST request to get a page of <%= entityClassPlural %> by criteria {}", pair.getFirst()))
            .flatMapIterable(pair -> <%= entityInstance %>QueryService.findByCriteria(pair.getFirst(), pair.getSecond()))
                <%_ } else { _%>   
            .doOnSuccess(p -> log.debug("REST request to get a page of <%= entityClassPlural %>"))
            .map(ProtobufMappers::pageRequestProtoToPageRequest)
            .flatMapIterable(<%= entityInstance %>Service::findAll)
                <%_ } _%>
            <%_ } else { _%>
                <%_ if (jpaMetamodelFiltering) { _%>
            .map(filter -> new PropertyValue(filter.getProperty(), filter.getValue().split(",")))
            .collect(MutablePropertyValues::new, MutablePropertyValues::addPropertyValue)
            .map(mutablePropertyValues -> {
                <%= entityClass %>Criteria criteria = new <%= entityClass %>Criteria();
                DataBinder binder = new DataBinder(criteria);
                binder.setConversionService(conversionService);
                binder.bind(mutablePropertyValues);
                return criteria;
            })
            .doOnSuccess(criteria -> log.debug("REST request to get <%= entityClassPlural %> by criteria: {}", criteria))
            .flatMapIterable(<%= entityInstance %>QueryService::findByCriteria)
                <%_ } else { _%>
            .doOnSuccess(e-> log.debug("REST request to get all <%= entityClassPlural %>"))
            .flatMapIterable(e -> <%= entityInstance %>Service.findAll())
                <%_ } _%>
            <%_ } _%>
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Mono<<%= entityClass %>Proto> get<%= entityClass %>(Mono<<%= idProtoWrappedType %>> request) {
        return request
            .map(<%= idProtoWrappedType %>::getValue)
            <%_ if (databaseType === 'cassandra') { _%>
            .map(ProtobufMappers::stringToUuid)
            <%_ } _%>
            .doOnSuccess(id -> log.debug("REST request to get <%= entityClass %> : {}", id))
            .map(id -> <%= entityInstance %>Service.findOne(id).orElseThrow(Status.NOT_FOUND::asRuntimeException))
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Mono<Empty> delete<%= entityClass %>(Mono<<%= idProtoWrappedType %>> request) {
        return request
            .map(<%= idProtoWrappedType %>::getValue)
            <%_ if (databaseType === 'cassandra') { _%>
            .map(ProtobufMappers::stringToUuid)
            <%_ } _%>
            .doOnSuccess(id -> log.debug("REST request to delete <%= entityClass %> : {}", id))
            .doOnSuccess(<%= entityInstance %>Service::delete)
            .map(id -> Empty.getDefaultInstance());
    }
    <%_ if (searchEngine == 'elasticsearch') { _%>

    @Override
    public Flux<<%= entityClass %>Proto> search<%= entityClassPlural %>(Mono<<%= entitySearchType %>> request) {
        return request
            <%_ if (pagination !== 'no') { _%>
            .map(<%= entitySearchType %>::getQuery)
            .map(StringValue::getValue)
            .doOnSuccess(query -> log.debug("REST request to search for a page of <%= entityClassPlural %> for query {}", query))
            .flatMapMany(query -> request
                .map(<%= entitySearchType %>::getPageRequest)
                .map(ProtobufMappers::pageRequestProtoToPageRequest)
                .flatMapIterable(pageRequest -> <%= entityInstance %>Service.search(query, pageRequest))
                )
            <%_ } else { _%>
            .map(StringValue::getValue)
            .doOnSuccess(query -> log.debug("REST request to search <%= entityClassPlural %> for query {}", query))
            .flatMapIterable(<%= entityInstance %>Service::search)
            <%_ } _%>
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }
    <%_ } _%>

}
