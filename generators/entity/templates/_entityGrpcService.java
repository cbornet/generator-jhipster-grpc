package <%= packageName %>.grpc.entity.<%=entityUnderscoredName%>;

import <%= packageName %>.grpc.AuthenticationInterceptor;
<%_ if (pagination !== 'no') { _%>
import <%= packageName %>.grpc.PageRequest;
import <%= packageName %>.grpc.ProtobufMappers;
<%_ } _%>
import <%= packageName %>.service.<%= entityClass %>Service;

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

import java.util.Optional;

/**
 * gRPC service providing CRUD methods for entity <%= entityClass %>.
 */
@GRpcService(interceptors = {AuthenticationInterceptor.class})
public class <%= entityClass %>GrpcService extends Rx<%= entityClass %>ServiceGrpc.<%= entityClass %>ServiceImplBase {

    private final Logger log = LoggerFactory.getLogger(<%= entityClass %>GrpcService.class);

    private final <%= entityClass %>Service <%= entityInstance %>Service;

    private final <%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper;

    public <%= entityClass %>GrpcService(<%= entityClass %>Service <%= entityInstance %>Service, <%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper) {
        this.<%= entityInstance %>Service = <%= entityInstance %>Service;
        this.<%= entityInstance %>ProtoMapper = <%= entityInstance %>ProtoMapper;
    }

    @Override
    public Single<<%= entityClass %>Proto> create<%= entityClass %>(Single<<%= entityClass %>Proto> request) {
        return update<%= entityClass %>(request
            .doOnSuccess(<%= entityInstance %>Proto -> log.debug("REST request to save Foo : {}", <%= entityInstance %>Proto))
            .filter(<%= entityInstance %>Proto -> <%= entityInstance %>Proto.getIdOneofCase() != <%= entityClass %>Proto.IdOneofCase.ID)
            .switchIfEmpty(Single.error(Status.ALREADY_EXISTS.asException()))
        );
    }

    @Override
    public Single<<%= entityClass %>Proto> update<%= entityClass %>(Single<<%= entityClass %>Proto> request) {
        return request
            .map(<%= entityInstance %>ProtoMapper::<%= entityInstance %>ProtoTo<%= instanceType %>)
            .doOnSuccess(<%= instanceName %> -> log.debug("REST request to update Foo : {}", <%= instanceName %>))
            .map(<%= entityInstance %>Service::save)
            .map(<%= entityInstance %>ProtoMapper::<%= instanceName %>To<%= entityClass %>Proto);
    }

    @Override
    public Flowable<<%= entityClass %>Proto> getAll<%= entityClassPlural %>(Single<<% if (pagination !== 'no') { %>PageRequest<% } else { %>Empty<% } %>> request) {
        return request
            <%_ if (pagination !== 'no') { _%>
            .doOnSuccess(e-> log.debug("REST request to get a page of <%= entityClassPlural %>"))
            .map(ProtobufMappers::pageRequestProtoToPageRequest)
            .map(<%= entityInstance %>Service::findAll)
            <%_ } else { _%>
            .doOnSuccess(e-> log.debug("REST request to get all <%= entityClassPlural %>"))
            .map(e -> <%= entityInstance %>Service.findAll())
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
