package <%= packageName %>.grpc.entity.<%=entityUnderscoredName%>;
<% if (databaseType === 'cassandra') { %>
import <%= packageName %>.AbstractCassandraTest;<% } %>
import <%= packageName %>.<%= mainClass %>;
<% if (authenticationType == 'uaa') { %>
import <%= packageName %>.config.SecurityBeanOverrideConfiguration;
<% } %>
import <%= packageName %>.domain.<%= entityClass %>;
<%_ for (r of relationships) { // import entities in required relationships
        if (r.relationshipValidate != null && r.relationshipValidate === true || jpaMetamodelFiltering) { _%>
import <%= packageName %>.domain.<%= r.otherEntityNameCapitalized %>;
<%_ } } _%>
<%_ for (r of relationships) { // import entities in required relationships
        if (r.relationshipValidate != null && r.relationshipValidate === true || jpaMetamodelFiltering) { _%>
import <%= r.otherEntityTest %>;
<%_ } } _%>
<%_ if (pagination !== 'no') { _%>
import <%= packageName %>.grpc.*;
<%_ } else if (jpaMetamodelFiltering) { _%>
import <%= packageName %>.grpc.QueryFilter;
<%_ } _%>
import <%= packageName %>.repository.<%= entityClass %>Repository;
<%_ if (searchEngine == 'elasticsearch') { _%>
import <%= packageName %>.repository.search.<%= entityClass %>SearchRepository;
<%_ } _%>
import <%= packageName %>.service.<%= entityClass %>Service;
<%_ if (jpaMetamodelFiltering) { _%>
import <%= packageName %>.service.<%= entityClass %>QueryService;
<%_ } _%>
<%_ if (dto == 'mapstruct') { _%>
import <%= packageName %>.service.dto.<%= entityClass %>DTO;
import <%= packageName %>.service.mapper.<%= entityClass %>Mapper;
<%_ } _%>
import <%= packageName %>.web.rest.TestUtil;

<%_ if (!jpaMetamodelFiltering && pagination === 'no') { _%>
import com.google.protobuf.Empty;
<%_ } _%>
import com.google.protobuf.<%=idProtoWrappedType%>;
<%_ if (searchEngine == 'elasticsearch' && this.databaseType !== 'sql') { _%>
import com.google.protobuf.StringValue;
<%_ } _%>
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
<%_ if (searchEngine === 'elasticsearch' && pagination !== 'no') { _%>
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
<%_ } _%>
<%_ if (jpaMetamodelFiltering) { _%>
import org.springframework.format.support.FormattingConversionService;
<%_ } _%>
import org.springframework.test.context.junit4.SpringRunner;
<%_ if (databaseType === 'sql') { _%>
import org.springframework.transaction.support.TransactionTemplate;
<%_ } _%>
<%_ if (pagination === 'no' && jpaMetamodelFiltering) { _%>
import reactor.core.publisher.Flux;
<%_ } else { _%>
import reactor.core.publisher.Mono;
<%_ } _%>

<%_ if (databaseType === 'sql') { _%>
import javax.persistence.EntityManager;
<%_ } _%>
<%_ if (jpaMetamodelFiltering) { _%>
import java.time.Duration;
<%_ } _%>
<%_ if (fieldsContainLocalDate == true) { _%>
import java.time.LocalDate;
<%_ } _%>
<%_ if (fieldsContainZonedDateTime == true || fieldsContainInstant == true) { _%>
import java.time.Instant;
<%_ } _%>
<%_ if (fieldsContainZonedDateTime == true) { _%>
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
<%_ } _%>
<%_ if (fieldsContainLocalDate == true || fieldsContainZonedDateTime == true) { _%>
import java.time.ZoneId;
<%_ } _%>
<%_ if (fieldsContainBigDecimal == true) { _%>
import java.math.BigDecimal;
<%_ } _%>
<%_ if (fieldsContainBlob == true && databaseType === 'cassandra') { _%>
import java.nio.ByteBuffer;
<%_ } _%>
import java.util.*;
<%_ if (searchEngine == 'elasticsearch') { _%>
import java.util.stream.StreamSupport;
<%_ } _%>

<%_ if (searchEngine === 'elasticsearch') { _%>
import static org.mockito.Mockito.*;
<%_ } _%>
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

<%_ for (idx in fields) { if (fields[idx].fieldIsEnum == true) { _%>import <%= packageName %>.domain.enumeration.<%= fields[idx].fieldType %>;
<%_ } } _%>
/**
 * Test class for the <%= entityClass %>GrpcService gRPC service.
 *
 * @see <%= entityClass %>GrpcService
 */
@RunWith(SpringRunner.class)
<%_ if (authenticationType === 'uaa' && applicationType !== 'uaa') { _%>
@SpringBootTest(classes = {<%= mainClass %>.class, SecurityBeanOverrideConfiguration.class})
<%_ } else { _%>
@SpringBootTest(classes = <%= mainClass %>.class)
<%_ } _%>
public class <%= entityClass %>GrpcServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{
<%_
    let oldSource = '';
    try {
        oldSource = this.fs.readFileSync(this.SERVER_TEST_SRC_DIR + packageFolder + '/web/rest/' + entityClass + 'ResourceIntTest.java', 'utf8');
    } catch (e) {}
_%>
    <%_ for (idx in fields) {
    const defaultValueName = 'DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase();
    const updatedValueName = 'UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase();

    let defaultValue = 1;
    let updatedValue = 2;

    if (fields[idx].fieldValidate == true) {
        if (fields[idx].fieldValidateRules.indexOf('max') != -1) {
            defaultValue = fields[idx].fieldValidateRulesMax;
            updatedValue = parseInt(fields[idx].fieldValidateRulesMax) - 1;
        }
        if (fields[idx].fieldValidateRules.indexOf('min') != -1) {
            defaultValue = fields[idx].fieldValidateRulesMin;
            updatedValue = parseInt(fields[idx].fieldValidateRulesMin) + 1;
        }
        if (fields[idx].fieldValidateRules.indexOf('minbytes') != -1) {
            defaultValue = fields[idx].fieldValidateRulesMinbytes;
            updatedValue = fields[idx].fieldValidateRulesMinbytes;
        }
        if (fields[idx].fieldValidateRules.indexOf('maxbytes') != -1) {
            updatedValue = fields[idx].fieldValidateRulesMaxbytes;
        }
    }

    const fieldType = fields[idx].fieldType;
    const fieldTypeBlobContent = fields[idx].fieldTypeBlobContent;
    const isEnum = fields[idx].fieldIsEnum;
    let enumValue1;
    let enumValue2;
    if (isEnum) {
        const values = fields[idx].fieldValues.replace(/\s/g, '').split(',');
        enumValue1 = values[0];
        if (values.length > 1) {
            enumValue2 = values[1];
        } else {
            enumValue2 = enumValue1;
        }
    }

    if (fieldType == 'String' || fieldTypeBlobContent == 'text') {
        // Generate Strings, using the min and max string length if they are configured
        let sampleTextString = "";
        let updatedTextString = "";
        let sampleTextLength = 10;
        if (fields[idx].fieldValidateRulesMinlength > sampleTextLength) {
            sampleTextLength = fields[idx].fieldValidateRulesMinlength;
        }
        if (fields[idx].fieldValidateRulesMaxlength < sampleTextLength) {
            sampleTextLength = fields[idx].fieldValidateRulesMaxlength;
        }
        for (let i = 0; i < sampleTextLength; i++) {
            sampleTextString += "A";
            updatedTextString += "B";
        }
        if (fields[idx].fieldValidateRulesPattern !== undefined) {
            if (oldSource !== '') {
                // Check for old values
                const sampleTextStringSearchResult = new RegExp('private static final String ' + defaultValueName + ' = "(.*)";', 'm').exec(oldSource);
                if (sampleTextStringSearchResult != null) {
                    sampleTextString = sampleTextStringSearchResult[1];
                }
                const updatedTextStringSearchResult = new RegExp('private static final String ' + updatedValueName + ' = "(.*)";', 'm').exec(oldSource);
                if (updatedTextStringSearchResult != null) {
                    updatedTextString = updatedTextStringSearchResult[1];
                }
            }
            // Generate Strings, using pattern
            try {
                const patternRegExp = new RegExp(fields[idx].fieldValidateRulesPattern);
                const randExp = new this.randexp(fields[idx].fieldValidateRulesPattern);
                // set infinite repetitionals max range
                randExp.max = 1;
                if (!patternRegExp.test(sampleTextString.replace(/\\"/g, '"').replace(/\\\\/g, '\\'))) {
                    sampleTextString = randExp.gen().replace(/\\/g, '\\\\').replace(/"/g, '\\"');
                }
                if (!patternRegExp.test(updatedTextString.replace(/\\"/g, '"').replace(/\\\\/g, '\\'))) {
                    updatedTextString = randExp.gen().replace(/\\/g, '\\\\').replace(/"/g, '\\"');
                }
            } catch (error) {
                log(this.chalkRed('Error generating test value for entity "' + entityClass +
                    '" field "' + fields[idx].fieldName + '" with pattern "' + fields[idx].fieldValidateRulesPattern +
                    '", generating default values for this field. Detailed error message: "' + error.message + '".'));
            }
            if (sampleTextString === updatedTextString) {
                updatedTextString = updatedTextString + "B";
                log(this.chalkRed('Randomly generated first and second test values for entity "' + entityClass +
                    '" field "' + fields[idx].fieldName + '" with pattern "' + fields[idx].fieldValidateRulesPattern +
                    '" in file "' + entityClass + 'ResourceIntTest" where equal, added symbol "B" to second value.'));
            }
        }_%>

    private static final String <%= defaultValueName %> = "<%-sampleTextString %>";
    private static final String <%= updatedValueName %> = "<%-updatedTextString %>";
    <%_ } else if (fieldType == 'Integer') { _%>

    private static final Integer <%= defaultValueName %> = <%= defaultValue %>;
    private static final Integer <%= updatedValueName %> = <%= updatedValue %>;
    <%_ } else if (fieldType == 'Long') { _%>

    private static final Long <%= defaultValueName %> = <%= defaultValue %>L;
    private static final Long <%= updatedValueName %> = <%= updatedValue %>L;
    <%_ } else if (fieldType == 'Float') { _%>

    private static final <%= fieldType %> <%= defaultValueName %> = <%= defaultValue %>F;
    private static final <%= fieldType %> <%= updatedValueName %> = <%= updatedValue %>F;
    <%_ } else if (fieldType == 'Double') { _%>

    private static final <%= fieldType %> <%= defaultValueName %> = <%= defaultValue %>D;
    private static final <%= fieldType %> <%= updatedValueName %> = <%= updatedValue %>D;
    <%_ } else if (fieldType == 'BigDecimal') { _%>

    private static final BigDecimal <%= defaultValueName %> = new BigDecimal(<%= defaultValue %>);
    private static final BigDecimal <%= updatedValueName %> = new BigDecimal(<%= updatedValue %>);
    <%_ } else if (fieldType == 'UUID') { _%>

    private static final UUID <%= defaultValueName %> = UUID.randomUUID();
    private static final UUID <%= updatedValueName %> = UUID.randomUUID();
    <%_ } else if (fieldType == 'LocalDate') { _%>

    private static final LocalDate <%= defaultValueName %> = LocalDate.ofEpochDay(0L);
    private static final LocalDate <%= updatedValueName %> = LocalDate.now(ZoneId.systemDefault());
    <%_ } else if (fieldType == 'Instant') { _%>

    private static final Instant <%= defaultValueName %> = Instant.ofEpochMilli(0L);
    private static final Instant <%= updatedValueName %> = Instant.now();
    <%_ } else if (fieldType == 'ZonedDateTime') { _%>

    private static final ZonedDateTime <%= defaultValueName %> = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime <%= updatedValueName %> = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    <%_ } else if (fieldType == 'Boolean') { _%>

    private static final Boolean <%= defaultValueName %> = false;
    private static final Boolean <%= updatedValueName %> = true;
    <%_ } else if ((fieldType == 'byte[]' || fieldType === 'ByteBuffer') && fieldTypeBlobContent != 'text') { _%>

    <%_ if (databaseType !== 'cassandra') { _%>
    private static final byte[] <%= defaultValueName %> = TestUtil.createByteArray(<%= defaultValue %>, "0");
    private static final byte[] <%= updatedValueName %> = TestUtil.createByteArray(<%= updatedValue %>, "1");
    <%_ } else { _%>
    private static final ByteBuffer <%= defaultValueName %> = ByteBuffer.wrap(TestUtil.createByteArray(<%= defaultValue %>, "0"));
    private static final ByteBuffer <%= updatedValueName %> = ByteBuffer.wrap(TestUtil.createByteArray(<%= updatedValue %>, "1"));
    <%_ } _%>
    private static final String <%= defaultValueName %>_CONTENT_TYPE = "image/jpg";
    private static final String <%= updatedValueName %>_CONTENT_TYPE = "image/png";
    <%_ } else if (isEnum) { _%>

    private static final <%= fieldType %> <%= defaultValueName %> = <%= fieldType %>.<%= enumValue1 %>;
    private static final <%= fieldType %> <%= updatedValueName %> = <%= fieldType %>.<%= enumValue2 %>;
    <%_ } } _%>

    @Autowired
    private <%= entityClass %>Repository <%= entityInstance %>Repository;<% if (dto == 'mapstruct') { %>

    @Autowired
    private <%= entityClass %>Mapper <%= entityInstance %>Mapper;<% } %>

    @Autowired
    private <%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper;

    @Autowired
    private <%= entityClass %>Service <%= entityInstance %>Service;

    <%_ if (jpaMetamodelFiltering) { _%>
    @Autowired
    private <%= entityClass %>QueryService <%= entityInstance %>QueryService;

    @Autowired
    private FormattingConversionService conversionService;

    <%_ } _%>
    <%_ if (searchEngine == 'elasticsearch') { _%>
    @Autowired
    private <%= entityClass %>SearchRepository mock<%= entityClass %>SearchRepository;

    <%_ } _%>
    <%_ if (databaseType === 'sql') { _%>
    @Autowired
    private EntityManager em;

    @Autowired
    private TransactionTemplate transactionTemplate;

    <%_ } _%>
    private Server mockServer;

    private <%= entityClass %>ServiceGrpc.<%= entityClass %>ServiceBlockingStub stub;

    private Reactor<%= entityClass %>ServiceGrpc.Reactor<%= entityClass %>ServiceStub rxstub;

    private <%= entityClass %> <%= entityInstance %>;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        <%= entityClass %>GrpcService <%= entityInstance %>GrpcService = new <%= entityClass %>GrpcService(<%= entityInstance %>Service, <% if (jpaMetamodelFiltering) { %><%= entityInstance %>QueryService, conversionService, <% } %><%= entityInstance %>ProtoMapper);
        String uniqueServerName = "Mock server for " + <%= entityClass %>GrpcService.class;
        mockServer = InProcessServerBuilder.forName(uniqueServerName).addService(<%= entityInstance %>GrpcService).build().start();
        stub = <%= entityClass %>ServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName(uniqueServerName).directExecutor().build());
        rxstub = Reactor<%= entityClass %>ServiceGrpc.newReactorStub(InProcessChannelBuilder.forName(uniqueServerName).build());
    }

    @After
    public void tearDown() {
        <%= entityInstance %>Repository.deleteAll();
        mockServer.shutdownNow();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static <%= entityClass %> createEntity(<% if (databaseType === 'sql') { %>EntityManager em<% } %>) {
        <%_ if (fluentMethods) { _%>
        <%= entityClass %> <%= entityInstance %> = new <%= entityClass %>()<% for (idx in fields) { %>
            .<%= fields[idx].fieldName %>(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>)<% if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { %>
            .<%= fields[idx].fieldName %>ContentType(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE)<% } %><% } %>;
        <%_ } else { _%>
        <%= entityClass %> <%= entityInstance %> = new <%= entityClass %>();
            <%_ for (idx in fields) { _%>
        <%= entityInstance %>.set<%= fields[idx].fieldInJavaBeanMethod %>(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase() %>);
                <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        <%= entityInstance %>.set<%= fields[idx].fieldInJavaBeanMethod %>ContentType(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase() %>_CONTENT_TYPE);
                <%_ } _%>
            <%_ } _%>
        <%_ } _%>
        <%_ for (idx in relationships) {
            const relationshipValidate = relationships[idx].relationshipValidate;
            const otherEntityNameCapitalized = relationships[idx].otherEntityNameCapitalized;
            const relationshipFieldName = relationships[idx].relationshipFieldName;
            const relationshipType = relationships[idx].relationshipType;
            const relationshipNameCapitalizedPlural = relationships[idx].relationshipNameCapitalizedPlural;
            const relationshipNameCapitalized = relationships[idx].relationshipNameCapitalized;
            if (relationshipValidate != null && relationshipValidate === true) { _%>
        // Add required entity
            <%= otherEntityNameCapitalized %> <%= relationshipFieldName %> = <%= otherEntityNameCapitalized %>GrpcServiceIntTest.createEntity(em);
            em.persist(<%= relationshipFieldName %>);
            em.flush();
            <%_ if (relationshipType == 'many-to-many') { _%>
        <%= entityInstance %>.get<%= relationshipNameCapitalizedPlural %>().add(<%= relationshipFieldName %>);
            <%_ } else { _%>
        <%= entityInstance %>.set<%= relationshipNameCapitalized %>(<%= relationshipFieldName %>);
            <%_ } _%>
        <%_ } } _%>
        return <%= entityInstance %>;
    }

    @Before
    public void initTest() {
        <%_ for (field of fields.filter(f => f.fieldType === 'ByteBuffer')) { _%>
        <%='DEFAULT_' + field.fieldNameUnderscored.toUpperCase()%>.rewind();
        <%_ } _%>
        <%= entityInstance %>Repository.deleteAll();
        <%_ if (searchEngine == 'elasticsearch') { _%>
        <%= entityInstance %>SearchRepository.deleteAll();
        <%_ } _%>
        <%_ if (databaseType === 'sql') { _%>
        <%= entityInstance %> = transactionTemplate.execute(s -> createEntity(em));
        <%_ } else { _%>
        <%= entityInstance %> = createEntity();
        <%_ } _%>
    }

    @Test
    public void create<%= entityClass %>() throws Exception {
        int databaseSizeBeforeCreate = <%= entityInstance %>Repository.findAll().size();

        // Create the <%= entityClass %>
        <%_ if (dto == 'mapstruct') { _%>
        <%= entityClass %>DTO <%= entityInstance %>DTO = <%= entityInstance %>Mapper.toDto(<%= entityInstance %>);
        <%_ } _%>
        <%= entityClass %>Proto <%= entityInstance %>Proto = <%= entityInstance %>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>);

        stub.create<%= entityClass %>(<%= entityInstance %>Proto);

        // Validate the <%= entityClass %> in the database
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeCreate + 1);
        <%= entityClass %> test<%= entityClass %> = <%= entityInstance %>List.get(<%= entityInstance %>List.size() - 1);
        <%_ for (idx in fields) { _%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(test<%= entityClass %>.get<%= fields[idx].fieldInJavaBeanMethod %>ContentType()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
            <%_ } _%>
        assertThat(test<%= entityClass %>.<% if (fields[idx].fieldType == 'Boolean') { %>is<% } else { %>get<% } %><%= fields[idx].fieldInJavaBeanMethod %>()).isEqual<% if (fields[idx].fieldType === 'BigDecimal') { %>ByComparing<% } %>To(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if (fields[idx].fieldType == 'ByteBuffer') { %>.rewind()<% } %>);
        <%_ } _%>
        <%_ if (searchEngine == 'elasticsearch') { _%>

        // Validate the <%= entityClass %> in Elasticsearch
        verify(mock<%= entityClass %>SearchRepository, times(1)).save(test<%= entityClass %>);
        <%_ } _%>
    }

    @Test
    public void create<%= entityClass %>WithExistingId() throws Exception {
        int databaseSizeBeforeCreate = <%= entityInstance %>Repository.findAll().size();

        // Create the <%= entityClass %> with an existing ID
        <%= entityInstance %>.setId(<% if (databaseType === 'sql') { %>1L<% } else if (databaseType === 'mongodb') { %>"existing_id"<% } else if (databaseType === 'cassandra') { %>UUID.randomUUID()<% } %>);
        <%_ if (dto == 'mapstruct') { _%>
        <%= entityClass %>DTO <%= entityInstance %>DTO = <%= entityInstance %>Mapper.toDto(<%= entityInstance %>);
        <%_ } _%>
        <%= entityClass %>Proto <%= entityInstance %>Proto = <%= entityInstance %>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>);

        try {
            stub.create<%= entityClass %>(<%= entityInstance %>Proto);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
        }

        // Validate the Alice in the database
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeCreate);
        <%_ if (searchEngine == 'elasticsearch') { _%>

        // Validate the <%= entityClass %> in Elasticsearch
        verify(mock<%= entityClass %>SearchRepository, times(0)).save(test<%= entityClass %>);
        <%_ } _%>
    }

    @Test
    public void getAll<%= entityClassPlural %>() throws Exception {
        // Initialize the database
        <%= entityInstance %>Repository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);

        // Get all the <%= entityInstancePlural %>
        <%_ if (pagination !== 'no') { _%>
            <%_ if (jpaMetamodelFiltering) { _%>
        PageRequestAndFilters query = PageRequestAndFilters.newBuilder()
            .setPageRequest( PageRequest.newBuilder()
                .addOrders(Order.newBuilder()
                    .setProperty("id")
                    .setDirection(Direction.DESC)))
            .build();
            <%_ } else { _%>
        PageRequest query = PageRequest.newBuilder()
            .addOrders(Order.newBuilder().setProperty("id").setDirection(Direction.DESC))
            .build();
            <%_ } _%>
        <%_ } _%>

        <%= entityClass %> found<%= entityClass %> = rxstub.getAll<%= entityClassPlural %>(<% if (pagination !== 'no') { %>Mono.just(query)<% } else { %><% if (jpaMetamodelFiltering) { %>Flux.empty()<% } else { %>Mono.just(Empty.getDefaultInstance())<% }} %>)
            .filter(<%= entityInstance %>Proto -> <%= entityInstance %>.getId()<% if (databaseType === 'cassandra') { %>.toString()<% } %>.equals(<%= entityInstance %>Proto.getId()))
            .map(<%= entityInstance %>ProtoMapper::<%= entityInstance %>ProtoTo<%= instanceType %>)
            <%_ if (dto == 'mapstruct') { _%>
            .map(<%= entityInstance %>Mapper::toEntity)
            <%_ } _%>
            .blockFirst();

        <%_ for (idx in fields) { _%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(found<%= entityClass %>.get<%= fields[idx].fieldInJavaBeanMethod %>ContentType()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
            <%_ } _%>
        assertThat(found<%= entityClass %>.<% if (fields[idx].fieldType == 'Boolean') { %>is<% } else { %>get<% } %><%= fields[idx].fieldInJavaBeanMethod %>()).isEqual<% if (fields[idx].fieldType === 'BigDecimal') { %>ByComparing<% } %>To(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if (fields[idx].fieldType == 'ByteBuffer') { %>.rewind()<% } %>);
        <%_ } _%>
    }
<%_ if (jpaMetamodelFiltering) {
        fields.forEach((searchBy) => {
            // we can't filter by all the fields.
            if (isFilterableType(searchBy.fieldType)) {
                 _%>

    @Test
    public void getAll<%= entityClassPlural %>By<%= searchBy.fieldInJavaBeanMethod %>IsEqualToSomething() throws Exception {
        // Initialize the database
        <%= entityInstance %>Repository.saveAndFlush(<%= entityInstance %>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> equals to <%='DEFAULT_' + searchBy.fieldNameUnderscored.toUpperCase()%>
        default<%= entityClass %>ShouldBeFound("<%= searchBy.fieldName %>.equals", <%='DEFAULT_' + searchBy.fieldNameUnderscored.toUpperCase()%>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> equals to <%='UPDATED_' + searchBy.fieldNameUnderscored.toUpperCase()%>
        default<%= entityClass %>ShouldNotBeFound("<%= searchBy.fieldName %>.equals", <%='UPDATED_' + searchBy.fieldNameUnderscored.toUpperCase()%>);
    }

    @Test
    public void getAll<%= entityClassPlural %>By<%= searchBy.fieldInJavaBeanMethod %>IsInShouldWork() throws Exception {
        // Initialize the database
        <%= entityInstance %>Repository.saveAndFlush(<%= entityInstance %>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> in <%='DEFAULT_' + searchBy.fieldNameUnderscored.toUpperCase()%> or <%='UPDATED_' + searchBy.fieldNameUnderscored.toUpperCase()%>
        default<%= entityClass %>ShouldBeFound("<%= searchBy.fieldName %>.in", <%='DEFAULT_' + searchBy.fieldNameUnderscored.toUpperCase()%> + "," + <%='UPDATED_' + searchBy.fieldNameUnderscored.toUpperCase()%>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> equals to <%='UPDATED_' + searchBy.fieldNameUnderscored.toUpperCase()%>
        default<%= entityClass %>ShouldNotBeFound("<%= searchBy.fieldName %>.in", <%='UPDATED_' + searchBy.fieldNameUnderscored.toUpperCase()%>);
    }

    @Test
    public void getAll<%= entityClassPlural %>By<%= searchBy.fieldInJavaBeanMethod %>IsNullOrNotNull() throws Exception {
        // Initialize the database
        <%= entityInstance %>Repository.saveAndFlush(<%= entityInstance %>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> is not null
        default<%= entityClass %>ShouldBeFound("<%= searchBy.fieldName %>.specified", "true");

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> is null
        default<%= entityClass %>ShouldNotBeFound("<%= searchBy.fieldName %>.specified", "false");
    }
<%_
            }
            // the range criterias
            if (['Byte', 'Short', 'Integer', 'Long', 'LocalDate', 'ZonedDateTime'].includes(searchBy.fieldType)) { 
              var defaultValue = 'DEFAULT_' + searchBy.fieldNameUnderscored.toUpperCase();
              var biggerValue = 'UPDATED_' + searchBy.fieldNameUnderscored.toUpperCase();
              if (searchBy.fieldValidate === true && searchBy.fieldValidateRules.includes('max')) {
                  // if maximum is specified the updated variable is smaller than the default one!
                  biggerValue = '(' + defaultValue + ' + 1)';
              }
            _%>

    @Test
    public void getAll<%= entityClassPlural %>By<%= searchBy.fieldInJavaBeanMethod %>IsGreaterThanOrEqualToSomething() throws Exception {
        // Initialize the database
        <%= entityInstance %>Repository.saveAndFlush(<%= entityInstance %>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> greater than or equals to <%= defaultValue %>
        default<%= entityClass %>ShouldBeFound("<%= searchBy.fieldName %>.greaterOrEqualThan", <%= defaultValue %>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> greater than or equals to <%= biggerValue %>
        default<%= entityClass %>ShouldNotBeFound("<%= searchBy.fieldName %>.greaterOrEqualThan", <%= biggerValue %>);
    }

    @Test
    public void getAll<%= entityClassPlural %>By<%= searchBy.fieldInJavaBeanMethod %>IsLessThanSomething() throws Exception {
        // Initialize the database
        <%= entityInstance %>Repository.saveAndFlush(<%= entityInstance %>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> less than or equals to <%= defaultValue %>
        default<%= entityClass %>ShouldNotBeFound("<%= searchBy.fieldName %>.lessThan", <%= defaultValue %>);

        // Get all the <%= entityInstance %>List where <%= searchBy.fieldName %> less than or equals to <%= biggerValue %>
        default<%= entityClass %>ShouldBeFound("<%= searchBy.fieldName %>.lessThan", <%= biggerValue %>);
    }

<%_         } _%>
<%_     }); _%>
<%_ relationships.forEach((relationship) => { _%>

    @Test
    public void getAll<%= entityClassPlural %>By<%= relationship.relationshipNameCapitalized %>IsEqualToSomething() throws Exception {
        // Initialize the database
        <%= relationship.otherEntityNameCapitalized %> <%= relationship.relationshipFieldName %> = transactionTemplate.execute(status -> {
            <%= relationship.otherEntityNameCapitalized %> <%= relationship.relationshipFieldName %>1 = <%= relationship.otherEntityNameCapitalized %>GrpcServiceIntTest.createEntity(em);
            em.persist(<%= relationship.relationshipFieldName %>1);
            em.flush();
    <%_ if (relationship.relationshipType === 'many-to-many' || relationship.relationshipType === 'one-to-many') { _%>
            <%= entityInstance %>.add<%= relationship.relationshipNameCapitalized %>(<%= relationship.relationshipFieldName %>1);
    <%_ } else { _%>
            <%= entityInstance %>.set<%= relationship.relationshipNameCapitalized %>(<%= relationship.relationshipFieldName %>1);
        <%_ if (relationship.ownerSide === false) { _%>
            <%= relationship.relationshipFieldName %>1.set<%= relationship.otherEntityRelationshipNameCapitalized %>(<%= entityInstance %>);
        <%_ } _%>
    <%_ } _%>
            <%= entityInstance %>Repository.saveAndFlush(<%= entityInstance %>);
            return <%= relationship.relationshipFieldName %>1;
        });
        <% if (authenticationType === 'oauth2' && relationship.relationshipFieldName === 'user') { _%>String<%_ } else { _%>Long<% } %> <%= relationship.relationshipFieldName %>Id = <%= relationship.relationshipFieldName %>.getId();

        <%_ if (relationship.ownerSide === false || relationship.relationshipType === 'one-to-many') { _%>
        try {
            // Get all the <%= entityInstance %>List where <%= relationship.relationshipFieldName %> equals to <%= relationship.relationshipFieldName %>Id
            default<%= entityClass %>ShouldBeFound("<%= relationship.relationshipFieldName %>Id.equals", <%= relationship.relationshipFieldName %>Id);

            // Get all the <%= entityInstance %>List where <%= relationship.relationshipFieldName %> equals to <%= relationship.relationshipFieldName %>Id + 1
            default<%= entityClass %>ShouldNotBeFound("<%= relationship.relationshipFieldName %>Id.equals", <%= relationship.relationshipFieldName %>Id + 1);
        } finally {
            transactionTemplate.execute(s -> {
                em.remove(em.find(<%= relationship.otherEntityNameCapitalized %>.class, <%= relationship.relationshipFieldName %>Id));
                return null;
            });
        }
        <%_ } else { _%>
        // Get all the <%= entityInstance %>List where <%= relationship.relationshipFieldName %> equals to <%= relationship.relationshipFieldName %>Id
        default<%= entityClass %>ShouldBeFound("<%= relationship.relationshipFieldName %>Id.equals", <%= relationship.relationshipFieldName %>Id);

        // Get all the <%= entityInstance %>List where <%= relationship.relationshipFieldName %> equals to <%= relationship.relationshipFieldName %>Id + 1
        default<%= entityClass %>ShouldNotBeFound("<%= relationship.relationshipFieldName %>Id.equals", <%= relationship.relationshipFieldName %>Id + 1);
        <%_ } _%>
    }

<%_ }); _%>
    /**
     * Executes the search, and checks that the default entity is returned
     */
    private void default<%= entityClass %>ShouldBeFound(String property, Object value) throws Exception {
        <%_ if (pagination !== 'no') { _%>
        PageRequestAndFilters query = PageRequestAndFilters.newBuilder()
            .setPageRequest( PageRequest.newBuilder()
                .addOrders(Order.newBuilder()
                    .setProperty("id")
                    .setDirection(Direction.DESC)))
            .addQueryFilters(QueryFilter.newBuilder()
                .setProperty(property)
                .setValue(value.toString()))
            .build();
        <%_ } else { _%>
        QueryFilter query = QueryFilter.newBuilder()
                .setProperty(property)
                .setValue(value.toString())
                .build();
        <%_ } _%>

        <%= entityClass %> found<%= entityClass %> = rxstub.getAll<%= entityClassPlural %>(<% if (pagination !== 'no') { %>Mono<% } else { %>Flux<% } %>.just(query))
            .filter(<%= entityInstance %>Proto -> <%= entityInstance %>.getId()<% if (databaseType === 'cassandra') { %>.toString()<% } %>.equals(<%= entityInstance %>Proto.getId()))
            .map(<%= entityInstance %>ProtoMapper::<%= entityInstance %>ProtoTo<%= instanceType %>)
            <%_ if (dto == 'mapstruct') { _%>
            .map(<%= entityInstance %>Mapper::toEntity)
            <%_ } _%>
            .blockFirst();

        <%_ for (idx in fields) { _%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(found<%= entityClass %>.get<%= fields[idx].fieldInJavaBeanMethod %>ContentType()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
            <%_ } _%>
        assertThat(found<%= entityClass %>.<% if (fields[idx].fieldType == 'Boolean') { %>is<% } else { %>get<% } %><%= fields[idx].fieldInJavaBeanMethod %>()).isEqual<% if (fields[idx].fieldType === 'BigDecimal') { %>ByComparing<% } %>To(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if (fields[idx].fieldType == 'ByteBuffer') { %>.rewind()<% } %>);
        <%_ } _%>
    }

    /**
     * Executes the search, and checks that the default entity is not returned
     */
    private void default<%= entityClass %>ShouldNotBeFound(String property, Object value) throws Exception {
                <%_ if (pagination !== 'no') { _%>
        PageRequestAndFilters query = PageRequestAndFilters.newBuilder()
            .setPageRequest( PageRequest.newBuilder()
                .addOrders(Order.newBuilder()
                    .setProperty("id")
                    .setDirection(Direction.DESC)))
            .addQueryFilters(QueryFilter.newBuilder()
                .setProperty(property)
                .setValue(value.toString()))
            .build();
        <%_ } else { _%>
        QueryFilter query = QueryFilter.newBuilder()
                .setProperty(property)
                .setValue(value.toString())
                .build();
        <%_ } _%>

        assertThat(rxstub.getAll<%= entityClassPlural %>(<% if (pagination !== 'no') { %>Mono<% } else { %>Flux<% } %>.just(query)).count().block(Duration.ofSeconds(5))).isZero();
    }
<%_  } _%>

    @Test
    public void get<%= entityClass %>() throws Exception {
        // Initialize the database
        <%= entityInstance %>Repository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);

        // Get the <%= entityInstance %>
        <%= entityClass %>Proto <%= entityInstance %>Proto = stub.get<%= entityClass %>(<%=idProtoWrappedType%>.newBuilder().setValue(<%= entityInstance %>.getId()<% if (databaseType === 'cassandra') { %>.toString()<% } %>).build());
        <% if (dto == 'mapstruct') { %><%= entityClass %>DTO <%= entityInstance %>DTO<% } else { %><%= entityClass %> found<%= entityClass %><% } %> = <%= entityInstance %>ProtoMapper.<%= entityInstance %>ProtoTo<%= instanceType %>(<%= entityInstance %>Proto);
        <%_ if (dto == 'mapstruct') { _%>
        <%= entityClass %> found<%= entityClass %> = <%= entityInstance %>Mapper.toEntity(<%= entityInstance %>DTO);
        <%_ } _%>

        <%_ for (idx in fields) { _%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(found<%= entityClass %>.get<%= fields[idx].fieldInJavaBeanMethod %>ContentType()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
            <%_ } _%>
        assertThat(found<%= entityClass %>.<% if (fields[idx].fieldType == 'Boolean') { %>is<% } else { %>get<% } %><%= fields[idx].fieldInJavaBeanMethod %>()).isEqual<% if (fields[idx].fieldType === 'BigDecimal') { %>ByComparing<% } %>To(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if (fields[idx].fieldType == 'ByteBuffer') { %>.rewind()<% } %>);
        <%_ } _%>
    }

    @Test
    public void getNonExisting<%= entityClass %>() throws Exception {
        try {
            // Get the <%= entityInstance %>
            stub.get<%= entityClass %>(<%=idProtoWrappedType%>.newBuilder().setValue(<% if (databaseType === 'sql') { %>Long.MAX_VALUE<% } else if (databaseType === 'mongodb') { %>String.valueOf(Long.MAX_VALUE)<% } else if (databaseType === 'cassandra') { %>UUID.randomUUID().toString()<% } %>).build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }

    @Test
    public void update<%= entityClass %>() throws Exception {
        // Initialize the database
<%_ if (dto !== 'mapstruct') { _%>
        <%= entityInstance %>Service.save(<%= entityInstance %>);
        // As the test used the service layer, reset the Elasticsearch mock repository
        reset(mock<%= entityClass %>SearchRepository);
<%_ } else { _%>
        <%= entityInstance %>Repository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);
<%_ } _%>

        int databaseSizeBeforeUpdate = <%= entityInstance %>Repository.findAll().size();

        // Update the <%= entityInstance %>
        <%_ if (databaseType === 'sql') { _%>
        <%= entityClass %>Proto <%= entityInstance %>Proto = transactionTemplate.execute(s-> {
            <%= entityClass %> updated<%= entityClass %> = <%= entityInstance %>Repository.findById(<%= entityInstance %>.getId()).orElseThrow(RuntimeException::new);
            <%_ if (fluentMethods && fields.length > 0) { _%>
            updated<%= entityClass %><% for (idx in fields) { %>
                .<%= fields[idx].fieldName %>(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>)<% if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { %>
                .<%= fields[idx].fieldName %>ContentType(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE)<% } %><% } %>;
            <%_ } else { _%>
                <%_ for (idx in fields) { _%>
            updated<%= entityClass %>.set<%= fields[idx].fieldInJavaBeanMethod %>(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
                    <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType == 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
            updated<%= entityClass %>.set<%= fields[idx].fieldInJavaBeanMethod %>ContentType(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
                    <%_ } _%>
                <%_ } _%>
            <%_ } _%>
            <%_ if (dto == 'mapstruct') { _%>
            <%= entityClass %>DTO updated<%= entityClass %>DTO = <%= entityInstance %>Mapper.toDto(updated<%= entityClass %>);
            <%_ } _%>
            em.detach(updated<%= entityClass %>);
            return <%= entityInstance %>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(updated<%= instanceType %>);
        });
        <%_ } else { _%>
        <%= entityClass %> updated<%= entityClass %> = <%= entityInstance %>Repository.findById(<%= entityInstance %>.getId()).orElseThrow(RuntimeException::new);
            <%_ if (fluentMethods && fields.length > 0) { _%>
        updated<%= entityClass %><% for (idx in fields) { %>
            .<%= fields[idx].fieldName %>(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>)<% if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { %>
            .<%= fields[idx].fieldName %>ContentType(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE)<% } %><% } %>;
            <%_ } else { _%>
                <%_ for (idx in fields) { _%>
        updated<%= entityClass %>.set<%= fields[idx].fieldInJavaBeanMethod %>(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
                    <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType == 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        updated<%= entityClass %>.set<%= fields[idx].fieldInJavaBeanMethod %>ContentType(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
                    <%_ } _%>
                <%_ } _%>
            <%_ } _%>
            <%_ if (dto == 'mapstruct') { _%>
        <%= entityClass %>DTO updated<%= entityClass %>DTO = <%= entityInstance %>Mapper.toDto(updated<%= entityClass %>);
            <%_ } _%>
        <%= entityClass %>Proto <%= entityInstance %>Proto = <%= entityInstance %>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(updated<%= instanceType %>);
        <%_ } _%>

        stub.update<%= entityClass %>(<%= entityInstance %>Proto);

        // Validate the <%= entityClass %> in the database
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeUpdate);
        <%= entityClass %> test<%= entityClass %> = <%= entityInstance %>List.get(<%= entityInstance %>List.size() - 1);
        <%_ for (idx in fields) { _%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(test<%= entityClass %>.get<%= fields[idx].fieldInJavaBeanMethod %>ContentType()).isEqualTo(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
            <%_ } _%>
        assertThat(test<%= entityClass %>.<% if (fields[idx].fieldType == 'Boolean') { %>is<% } else { %>get<% } %><%= fields[idx].fieldInJavaBeanMethod %>()).isEqual<% if (fields[idx].fieldType === 'BigDecimal') { %>ByComparing<% } %>To(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if (fields[idx].fieldType == 'ByteBuffer') { %>.rewind()<% } %>);
        <%_ } _%>
        <%_ if (searchEngine == 'elasticsearch') { _%>

        // Validate the <%= entityClass %> in Elasticsearch
        verify(mock<%= entityClass %>SearchRepository, times(1)).save(test<%= entityClass %>);
        <%_ } _%>
    }

    @Test
    public void updateNonExisting<%= entityClass %>() throws Exception {
        int databaseSizeBeforeUpdate = <%= entityInstance %>Repository.findAll().size();

        // Create the <%= entityClass %><% if (dto == 'mapstruct') { %>
        <%= entityClass %>DTO <%= entityInstance %>DTO = <%= entityInstance %>Mapper.toDto(<%= entityInstance %>);<% } %>
        <%= entityClass %>Proto <%= entityInstance %>Proto = <%= entityInstance %>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        stub.update<%= entityClass %>(<%= entityInstance %>Proto);

        // Validate the <%= entityClass %> in the database
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeUpdate + 1);
        <%_ if (searchEngine == 'elasticsearch') { _%>

        // Validate the <%= entityClass %> in Elasticsearch
        verify(mock<%= entityClass %>SearchRepository, times(0)).save(test<%= entityClass %>);
        <%_ } _%>
    }

    @Test
    public void delete<%= entityClass %>() throws Exception {
        // Initialize the database
<%_ if (dto != 'mapstruct') { _%>
        <%= entityInstance %>Service.save(<%= entityInstance %>);
<%_ } else { _%>
        <%= entityInstance %>Repository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);
<%_ } _%>

        int databaseSizeBeforeDelete = <%= entityInstance %>Repository.findAll().size();

        // Get the <%= entityInstance %>
        stub.delete<%= entityClass %>(<%=idProtoWrappedType%>.newBuilder().setValue(<%= entityInstance %>.getId()<% if (databaseType === 'cassandra') { %>.toString()<% } %>).build());

        // Validate the database is empty
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeDelete - 1);

        <%_ if (searchEngine === 'elasticsearch') { _%>

        // Validate the <%= entityClass %> in Elasticsearch
        verify(mock<%= entityClass %>SearchRepository, times(1)).deleteById(<%= entityInstance %>.getId());
        <%_ } _%>
    }
    <%_ if (searchEngine == 'elasticsearch') { _%>

    @Test
    public void search<%= entityClassPlural %>() throws Exception {
        // Initialize the database
        <%= entityClass %> saved<%= entityClass %> = <%= entityInstance %>Repository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);

        // Search the <%= entityInstancePlural %>
        <%_ if (pagination !== 'no') { _%>
        <%= entityClass %>SearchPageRequest query = <%= entityClass %>SearchPageRequest.newBuilder()
            .setPageRequest(PageRequest.newBuilder()
                .addOrders(Order.newBuilder()
                    .setProperty("id")
                    .setDirection(Direction.DESC)
                )
            )
            .setQuery((StringValue.newBuilder().setValue("id:" + saved<%= entityClass %>.getId()).build()))
            .build();
        <%_ } else { _%>
        StringValue query = StringValue.newBuilder().setValue("id:" + saved<%= entityClass %>.getId()).build();
        <%_ } _%>

        <%_ if (pagination !== 'no') { _%>
        when(mock<%= entityClass %>SearchRepository.search(queryStringQuery("id:" + <%= entityInstance %>.getId()), PageRequest.of(0, 20)))
            .thenReturn(new PageImpl<>(Collections.singletonList(<%= entityInstance %>), PageRequest.of(0, 1), 1));
        <%_ } else { _%>
        when(mock<%= entityClass %>SearchRepository.search(queryStringQuery("id:" + <%= entityInstance %>.getId())))
            .thenReturn(Collections.singletonList(<%= entityInstance %>));
        <%_ } _%>

        Optional<<%= entityClass %>> maybe<%= entityClass %> = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(stub.search<%= entityClassPlural %>(query), Spliterator.ORDERED),
            false)
            .filter(<%= entityInstance %>Proto -> saved<%= entityClass %>.getId().equals(<%= entityInstance %>Proto.getId()))
            .map(<%= entityInstance %>ProtoMapper::<%= entityInstance %>ProtoTo<%= instanceType %>)
            <%_ if (dto == 'mapstruct') { _%>
            .map(<%= entityInstance %>Mapper::toEntity)
            <%_ } _%>
            .findAny();

        assertThat(maybe<%= entityClass %>).isPresent();
        <%= entityClass %> found<%= entityClass %> = maybe<%= entityClass %>.orElse(null);
        <%_ for (idx in fields) { _%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(found<%= entityClass %>.get<%= fields[idx].fieldInJavaBeanMethod %>ContentType()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
            <%_ } _%>
        assertThat(found<%= entityClass %>.<% if (fields[idx].fieldType == 'Boolean') { %>is<% } else { %>get<% } %><%= fields[idx].fieldInJavaBeanMethod %>()).isEqual<% if (fields[idx].fieldType === 'BigDecimal') { %>ByComparing<% } %>To(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if (fields[idx].fieldType == 'ByteBuffer') { %>.rewind()<% } %>);
        <%_ } _%>
    }
    <%_ } _%>

}
