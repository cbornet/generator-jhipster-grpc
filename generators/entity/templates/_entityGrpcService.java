package <%= packageName %>.grpc.entity.<%=entityUnderscoredName%>;

import <%= packageName %>.grpc.AuthenticationInterceptor;
<%_ if (pagination !== 'no') { _%>
import <%= packageName %>.grpc.PageRequest<% if (jpaMetamodelFiltering) { %>AndFilters<% } %>;
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
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<%_ if (jpaMetamodelFiltering) { _%>
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.convert.ConversionService;
    <%_ if (pagination !== 'no') { _%>
import org.springframework.data.util.Pair;
    <%_ } _%>
import org.springframework.validation.DataBinder;
<%_ } _%>

<%_ if (jpaMetamodelFiltering && pagination !== 'no') { _%>
import java.util.List;
<%_ } _%>
import java.util.Optional;
<%_ if (jpaMetamodelFiltering && pagination !== 'no') { _%>
import java.util.stream.Collectors;
<%_ } _%>

/**
 * gRPC service providing CRUD methods for entity <%= entityClass %>.
 */
@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class <%= entityClass %>GrpcService extends Rx<%= entityClass %>ServiceGrpc.<%= entityClass %>ServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(<%= entityClass %>GrpcService.class);

    private final <%= entityClass %>Service <%= entityInstance %>Service;

    <%_ if (jpaMetamodelFiltering) { _%>
    private final <%= entityClass %>QueryService <%= entityInstance %>QueryService;

    private final ConversionService conversionService;
    
    <%_ } _%>
    private final <%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper;

    public <%= entityClass %>GrpcService(<%= entityClass %>Service <%= entityInstance %>Service, <% if (jpaMetamodelFiltering) { %><%= entityClass %>QueryService <%= entityInstance %>QueryService, ConversionService conversionService, <% } %><%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper) {
        this.<%= entityInstance %>Service = <%= entityInstance %>Service;
        <%_ if (jpaMetamodelFiltering) { _%>
        this.<%= entityInstance %>QueryService = <%= entityInstance %>QueryService;
        this.conversionService = conversionService;
        <%_ } _%>
        this.<%= entityInstance %>ProtoMapper = <%= entityInstance %>ProtoMapper;
    }

    @Override
    public Single<<%= entityClass %>Proto> create<%= entityClass %>(Single<<%= entityClass %>Proto> request) {
        return update<%= entityClass %>(request
            .doOnSuccess(<%= entityInstance %>Proto -> log.debug("REST request to save <%= entityClass %> : {}", <%= entityInstance %>Proto))
            .filter(<%= entityInstance %>Proto -> <%= entityInstance %>Proto.getIdOneofCase() != <%= entityClass %>Proto.IdOneofCase.ID)
            .switchIfEmpty(Single.error(Status.ALREADY_EXISTS.asException()))
        );
    }

    @Override
    public Single<<%= entityClass %>Proto> update<%= entityClass %>(Single<<%= entityClass %>Proto> request) {
        return request
            .map(<%= entityInstance %>ProtoMapper::<%= entityInstance %>ProtoTo<%= instanceType %>)
            .doOnSuccess(<%= instanceName %> -> log.debug("REST request to update <%= entityClass %> : {}", <%= instanceName %>))
            .map(<%= entityInstance %>Service::save)
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Flowable<<%= entityClass %>Proto> getAll<%= entityClassPlural %>(<% if (pagination !== 'no') { %>Single<PageRequest<% if (jpaMetamodelFiltering) { %>AndFilters<% } } else { if (jpaMetamodelFiltering) {  %>Flowable<QueryFilter<% } else { %>Single<Empty<% }} %>> request) {
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
            .doOnSuccess(pair -> log.debug("REST request to get a page of Bars by criteria {}", pair.getFirst()))
            .map(pair -> barQueryService.findByCriteria(pair.getFirst(), pair.getSecond()))
                <%_ } else { _%>   
            .doOnSuccess(p -> log.debug("REST request to get a page of <%= entityClassPlural %>"))
            .map(ProtobufMappers::pageRequestProtoToPageRequest)
            .map(<%= entityInstance %>Service::findAll)
                <%_ } _%>
            <%_ } else { _%>
                <%_ if (jpaMetamodelFiltering) { _%>
            .map(filter -> new PropertyValue(filter.getProperty(), filter.getValue().split(",")))
            .collectInto(new MutablePropertyValues(), MutablePropertyValues::addPropertyValue)
            .map(mutablePropertyValues -> {
                <%= entityClass %>Criteria criteria = new <%= entityClass %>Criteria();
                DataBinder binder = new DataBinder(criteria);
                binder.setConversionService(conversionService);
                binder.bind(mutablePropertyValues);
                return criteria;
            })
            .doOnSuccess(criteria -> log.debug("REST request to get <%= entityClassPlural %> by criteria: {}", criteria))
            .map(<%= entityInstance %>QueryService::findByCriteria)
                <%_ } else { _%>
            .doOnSuccess(e-> log.debug("REST request to get all <%= entityClassPlural %>"))
            .map(e -> <%= entityInstance %>Service.findAll())
                <%_ } _%>
            <%_ } _%>
            .flatMapPublisher(Flowable::fromIterable)
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Single<<%= entityClass %>Proto> get<%= entityClass %>(Single<<%= idProtoWrappedType %>> request) {
        return request
            .map(<%= idProtoWrappedType %>::getValue)
            .doOnSuccess(id -> log.debug("REST request to get <%= entityClass %> : {}", id))
            .map(id -> Optional.ofNullable(<%= entityInstance %>Service.findOne(id)).orElseThrow(Status.NOT_FOUND::asException))
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Single<Empty> delete<%= entityClass %>(Single<<%= idProtoWrappedType %>> request) {
        return request
            .map(<%= idProtoWrappedType %>::getValue)
            .doOnSuccess(id -> log.debug("REST request to delete <%= entityClass %> : {}", id))
            .doOnSuccess(<%= entityInstance %>Service::delete)
            .map(id -> Empty.getDefaultInstance());
    }
    <%_ if (searchEngine == 'elasticsearch') { _%>

    @Override
    public Flowable<<%= entityClass %>Proto> search<%= entityClassPlural %>(Single<<%= entitySearchType %>> request) {
        return request
            <%_ if (pagination !== 'no') { _%>
            .map(<%= entitySearchType %>::getQuery)
            .map(StringValue::getValue)
            .doOnSuccess(query -> log.debug("REST request to search for a page of <%= entityClassPlural %> for query {}", query))
            .flatMapPublisher(query -> request
                .map(<%= entitySearchType %>::getPageRequest)
                .map(ProtobufMappers::pageRequestProtoToPageRequest)
                .map(pageRequest -> <%= entityInstance %>Service.search(query, pageRequest))
                .flatMapPublisher(Flowable::fromIterable)
            )
            <%_ } else { _%>
            .map(StringValue::getValue)
            .doOnSuccess(query -> log.debug("REST request to search <%= entityClassPlural %> for query {}", query))
            .map(<%= entityInstance %>Service::search)
            .flatMapPublisher(Flowable::fromIterable)
            <%_ } _%>
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }
    <%_ } _%>

}
