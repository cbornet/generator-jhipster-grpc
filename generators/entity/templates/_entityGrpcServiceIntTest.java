package <%=packageName%>.grpc.entity.<%=entityUnderscoredName%>;
<% if (databaseType == 'cassandra') { %>
import <%=packageName%>.AbstractCassandraTest;<% } %>
import <%=packageName%>.<%= mainClass %>;
<% if (authenticationType == 'uaa') { %>
import <%=packageName%>.config.SecurityBeanOverrideConfiguration;
<% } %>
import <%=packageName%>.domain.<%= entityClass %>;<% if (pagination !== 'no') { %>
import <%=packageName%>.grpc.PageRequest;<% } else { %>
import com.google.protobuf.Empty;<% } %>
<%_ for (r of relationships) { // import entities in required relationships
        if (r.relationshipValidate != null && r.relationshipValidate === true) { _%>
import <%=packageName%>.domain.<%= r.otherEntityNameCapitalized %>;
<%_ } } _%>
<%_ for (r of relationships) { // import entities in required relationships
        if (r.relationshipValidate != null && r.relationshipValidate === true) { _%>
import <%=r.otherEntityTest%>;
<%_ } } _%>
import <%=packageName%>.repository.<%= entityClass %>Repository;<% if (searchEngine == 'elasticsearch') { %>
import <%=packageName%>.repository.search.<%= entityClass %>SearchRepository;<% } %>
import <%=packageName%>.service.<%= entityClass %>Service;<% if (dto == 'mapstruct') { %>
import <%=packageName%>.service.dto.<%= entityClass %>DTO;
import <%=packageName%>.service.mapper.<%= entityClass %>Mapper;<% } %>
import <%=packageName%>.web.rest.TestUtil;

import com.google.protobuf.<%=idProtoWrappedType%>;
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
import org.springframework.test.context.junit4.SpringRunner;<% if (databaseType == 'sql') { %>
import org.springframework.transaction.annotation.Transactional;<% } %>
<% if (databaseType == 'sql') { %>
import javax.persistence.EntityManager;<% } %><% if (fieldsContainLocalDate == true) { %>
import java.time.LocalDate;<% } %><% if (fieldsContainZonedDateTime == true) { %>
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;<% } %><% if (fieldsContainLocalDate == true || fieldsContainZonedDateTime == true) { %>
import java.time.ZoneId;<% } %><% if (fieldsContainBigDecimal == true) { %>
import java.math.BigDecimal;<% } %><% if (fieldsContainBlob == true && databaseType === 'cassandra') { %>
import java.nio.ByteBuffer;<% } %>
import java.util.Iterator;
import java.util.List;<% if (databaseType == 'cassandra') { %>
import java.util.UUID;<% } %>
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

<%_ for (idx in fields) { if (fields[idx].fieldIsEnum == true) { _%>import <%=packageName%>.domain.enumeration.<%= fields[idx].fieldType %>;
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
public class <%= entityClass %>GrpcServiceIntTest <% if (databaseType == 'cassandra') { %>extends AbstractCassandraTest <% } %>{
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

    private static final String <%=defaultValueName %> = "<%-sampleTextString %>";
    private static final String <%=updatedValueName %> = "<%-updatedTextString %>";
    <%_ } else if (fieldType == 'Integer') { _%>

    private static final Integer <%=defaultValueName %> = <%= defaultValue %>;
    private static final Integer <%=updatedValueName %> = <%= updatedValue %>;
    <%_ } else if (fieldType == 'Long') { _%>

    private static final Long <%=defaultValueName %> = <%= defaultValue %>L;
    private static final Long <%=updatedValueName %> = <%= updatedValue %>L;
    <%_ } else if (fieldType == 'Float') { _%>

    private static final <%=fieldType %> <%=defaultValueName %> = <%= defaultValue %>F;
    private static final <%=fieldType %> <%=updatedValueName %> = <%= updatedValue %>F;
    <%_ } else if (fieldType == 'Double') { _%>

    private static final <%=fieldType %> <%=defaultValueName %> = <%= defaultValue %>D;
    private static final <%=fieldType %> <%=updatedValueName %> = <%= updatedValue %>D;
    <%_ } else if (fieldType == 'BigDecimal') { _%>

    private static final BigDecimal <%=defaultValueName %> = new BigDecimal(<%= defaultValue %>);
    private static final BigDecimal <%=updatedValueName %> = new BigDecimal(<%= updatedValue %>);
    <%_ } else if (fieldType == 'UUID') { _%>

    private static final UUID <%=defaultValueName %> = UUID.randomUUID();
    private static final UUID <%=updatedValueName %> = UUID.randomUUID();
    <%_ } else if (fieldType == 'LocalDate') { _%>

    private static final LocalDate <%=defaultValueName %> = LocalDate.ofEpochDay(0L);
    private static final LocalDate <%=updatedValueName %> = LocalDate.now(ZoneId.systemDefault());
    <%_ } else if (fieldType == 'ZonedDateTime') { _%>

    private static final ZonedDateTime <%=defaultValueName %> = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
    private static final ZonedDateTime <%=updatedValueName %> = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    <%_ } else if (fieldType == 'Boolean') { _%>

    private static final Boolean <%=defaultValueName %> = false;
    private static final Boolean <%=updatedValueName %> = true;
    <%_ } else if ((fieldType == 'byte[]' || fieldType === 'ByteBuffer') && fieldTypeBlobContent != 'text') { _%>

    <%_ if (databaseType !== 'cassandra') { _%>
    private static final byte[] <%=defaultValueName %> = TestUtil.createByteArray(<%= defaultValue %>, "0");
    private static final byte[] <%=updatedValueName %> = TestUtil.createByteArray(<%= updatedValue %>, "1");
    <%_ } else { _%>
    private static final ByteBuffer <%=defaultValueName %> = ByteBuffer.wrap(TestUtil.createByteArray(<%= defaultValue %>, "0"));
    private static final ByteBuffer <%=updatedValueName %> = ByteBuffer.wrap(TestUtil.createByteArray(<%= updatedValue %>, "1"));
    <%_ } _%>
    private static final String <%=defaultValueName %>_CONTENT_TYPE = "image/jpg";
    private static final String <%=updatedValueName %>_CONTENT_TYPE = "image/png";
    <%_ } else if (isEnum) { _%>

    private static final <%=fieldType %> <%=defaultValueName %> = <%=fieldType %>.<%=enumValue1 %>;
    private static final <%=fieldType %> <%=updatedValueName %> = <%=fieldType %>.<%=enumValue2 %>;
    <%_ } } _%>

    @Autowired
    private <%= entityClass %>Repository <%= entityInstance %>Repository;<% if (dto == 'mapstruct') { %>

    @Autowired
    private <%= entityClass %>Mapper <%= entityInstance %>Mapper;<% } %>

    @Autowired
    private <%= entityClass %>ProtoMapper <%= entityInstance %>ProtoMapper;

    @Autowired
    private <%= entityClass %>Service <%= entityInstance %>Service;<% if (searchEngine == 'elasticsearch') { %>

    @Autowired
    private <%= entityClass %>SearchRepository <%= entityInstance %>SearchRepository;<% } %>
<%_ if (databaseType == 'sql') { _%>

    @Autowired
    private EntityManager em;
<%_ } _%>

    private Server mockServer;

    private <%= entityClass %>ServiceGrpc.<%= entityClass %>ServiceBlockingStub stub;

    private <%= entityClass %> <%= entityInstance %>;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        <%= entityClass %>GrpcService <%= entityInstance %>GrpcService = new <%= entityClass %>GrpcService(<%= entityInstance %>Service, <%= entityInstance %>ProtoMapper);
        String uniqueServerName = "Mock server for " + <%= entityClass %>GrpcService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(<%= entityInstance %>GrpcService).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = <%= entityClass %>ServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        mockServer.shutdownNow();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static <%= entityClass %> createEntity(<% if (databaseType == 'sql') { %>EntityManager em<% } %>) {
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
        <%_ if (databaseType == 'mongodb' || databaseType == 'cassandra') { _%>
        <%= entityInstance %>Repository.deleteAll();
        <%_ } if (searchEngine == 'elasticsearch') { _%>
        <%= entityInstance %>SearchRepository.deleteAll();
        <%_ } _%>
        <%= entityInstance %> = createEntity(<% if (databaseType == 'sql') { %>em<% } %>);
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void create<%= entityClass %>() throws Exception {
        int databaseSizeBeforeCreate = <%= entityInstance %>Repository.findAll().size();

        // Create the <%= entityClass %>
        <%_ if (dto == 'mapstruct') { _%>
        <%= entityClass %>DTO <%= entityInstance %>DTO = <%= entityInstance %>Mapper.<%= entityInstance %>To<%= entityClass %>DTO(<%= entityInstance %>);
        <%_ } _%>
        <%= entityClass %>Proto <%= entityInstance %>Proto = <%= entityInstance %>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>);

        stub.create<%= entityClass %>(<%= entityInstance %>Proto);

        // Validate the <%= entityClass %> in the database
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeCreate + 1);
        <%= entityClass %> test<%= entityClass %> = <%= entityInstance %>List.get(<%= entityInstance %>List.size() - 1);
        <%_ for (idx in fields) { if (fields[idx].fieldType == 'ZonedDateTime') { _%>
        assertThat(test<%= entityClass %>.get<%=fields[idx].fieldInJavaBeanMethod%>()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
        <%_ } else if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(test<%= entityClass %>.get<%=fields[idx].fieldInJavaBeanMethod%>()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if (fields[idx].fieldType === 'ByteBuffer') { %>.rewind()<% } %>);
        assertThat(test<%= entityClass %>.get<%=fields[idx].fieldInJavaBeanMethod%>ContentType()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
        <%_ } else if (fields[idx].fieldType.toLowerCase() == 'boolean') { _%>
        assertThat(test<%= entityClass %>.is<%=fields[idx].fieldInJavaBeanMethod%>()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
        <%_ } else { _%>
        assertThat(test<%= entityClass %>.get<%=fields[idx].fieldInJavaBeanMethod%>()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
        <%_ }} if (searchEngine == 'elasticsearch') { _%>

        // Validate the <%= entityClass %> in Elasticsearch
        <%= entityClass %> <%= entityInstance %>Es = <%= entityInstance %>SearchRepository.findOne(test<%= entityClass %>.getId());
        assertThat(<%= entityInstance %>Es).isEqualToComparingFieldByField(test<%= entityClass %>);
        <%_ } _%>
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void create<%= entityClass %>WithExistingId() throws Exception {
        int databaseSizeBeforeCreate = <%= entityInstance %>Repository.findAll().size();

        // Create the <%= entityClass %> with an existing ID
        <%= entityInstance %>.setId(<% if (databaseType == 'sql') { %>1L<% } else if (databaseType == 'mongodb') { %>"existing_id"<% } else if (databaseType == 'cassandra') { %>UUID.randomUUID()<% } %>);
        <%_ if (dto == 'mapstruct') { _%>
        <%= entityClass %>DTO <%= entityInstance %>DTO = <%= entityInstance %>Mapper.<%= entityInstance %>To<%= entityClass %>DTO(<%= entityInstance %>);
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
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void getAll<%= entityClassPlural %>() throws Exception {
        // Initialize the database
        <%= entityClass %> saved<%= entityClass %> = <%= entityInstance %>Repository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);

        // Get all the <%= entityInstancePlural %>
        <%= entityClass %> found<%= entityClass %> = null;
        Iterator<<%= entityClass %>Proto> it = stub.getAll<%= entityClassPlural %>(<% if (pagination !== 'no') { %>PageRequest<% } else { %>Empty<% } %>.getDefaultInstance());
        while(it.hasNext()) {
            <%= entityClass %>Proto <%= entityInstance %>Proto = it.next();
            if (saved<%= entityClass %>.getId()<% if (databaseType == 'cassandra') { %>.toString()<% } %>.equals(<%= entityInstance %>Proto.getId())) {
                <% if (dto == 'mapstruct') { %><%= entityClass %>DTO <%= entityInstance %>DTO<% } else { %>found<%= entityClass %><% } %> = <%= entityInstance %>ProtoMapper.<%= entityInstance %>ProtoTo<%= instanceType %>(<%= entityInstance %>Proto);
                <%_ if (dto == 'mapstruct') { _%>
                found<%= entityClass %> = <%= entityInstance %>Mapper.<%= entityInstance %>DTOTo<%= entityClass %>(<%= entityInstance %>DTO);
                <%_ } _%>
                break;
            }
        }

        assertThat(found<%= entityClass %>).isNotNull();
        <%_ for (idx in fields) { _%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(found<%= entityClass %>.get<%= fields[idx].fieldInJavaBeanMethod %>ContentType()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
            <%_ } _%>
        assertThat(found<%= entityClass %>.<% if (fields[idx].fieldType == 'Boolean') { %>is<% } else { %>get<% } %><%= fields[idx].fieldInJavaBeanMethod %>()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
        <%_ } _%>
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void get<%= entityClass %>() throws Exception {
        // Initialize the database
        <%= entityInstance %>Repository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);

        // Get the <%= entityInstance %>
        <%= entityClass %>Proto <%= entityInstance %>Proto = stub.get<%= entityClass %>(<%=idProtoWrappedType%>.newBuilder().setValue(<%= entityInstance %>.getId()<% if (databaseType == 'cassandra') { %>.toString()<% } %>).build());
        <% if (dto == 'mapstruct') { %><%= entityClass %>DTO <%= entityInstance %>DTO<% } else { %><%= entityClass %> found<%= entityClass %><% } %> = <%= entityInstance %>ProtoMapper.<%= entityInstance %>ProtoTo<%= instanceType %>(<%= entityInstance %>Proto);
        <%_ if (dto == 'mapstruct') { _%>
        <%= entityClass %> found<%= entityClass %> = <%= entityInstance %>Mapper.<%= entityInstance %>DTOTo<%= entityClass %>(<%= entityInstance %>DTO);
        <%_ } _%>

        <%_ for (idx in fields) { _%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(found<%= entityClass %>.get<%= fields[idx].fieldInJavaBeanMethod %>ContentType()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
            <%_ } _%>
        assertThat(found<%= entityClass %>.<% if (fields[idx].fieldType == 'Boolean') { %>is<% } else { %>get<% } %><%= fields[idx].fieldInJavaBeanMethod %>()).isEqualTo(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
        <%_ } _%>
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void getNonExisting<%= entityClass %>() throws Exception {
        try {
            // Get the <%= entityInstance %>
            stub.get<%= entityClass %>(<%=idProtoWrappedType%>.newBuilder().setValue(<% if (databaseType == 'sql') { %>Long.MAX_VALUE<% } else if (databaseType == 'mongodb') { %>String.valueOf(Long.MAX_VALUE)<% } else if (databaseType == 'cassandra') { %>UUID.randomUUID().toString()<% } %>).build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void update<%= entityClass %>() throws Exception {
        // Initialize the database
<%_ if (dto != 'mapstruct') { _%>
        <%= entityInstance %>Service.save(<%= entityInstance %>);
<%_ } else { _%>
        <%= entityInstance %>Repository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);<% if (searchEngine == 'elasticsearch') { %>
        <%= entityInstance %>SearchRepository.save(<%= entityInstance %>);<%_ } _%>
<%_ } _%>

        int databaseSizeBeforeUpdate = <%= entityInstance %>Repository.findAll().size();

        // Update the <%= entityInstance %>
        <%= entityClass %> updated<%= entityClass %> = <%= entityInstance %>Repository.findOne(<%= entityInstance %>.getId());
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
        <%= entityClass %>DTO updated<%= entityClass %>DTO = <%= entityInstance %>Mapper.<%= entityInstance %>To<%= entityClass %>DTO(updated<%= entityClass %>);
        <%_ } _%>
        <%= entityClass %>Proto <%= entityInstance %>Proto = <%= entityInstance %>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(updated<%=instanceType%>);

        stub.update<%= entityClass %>(<%= entityInstance %>Proto);

        // Validate the <%= entityClass %> in the database
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeUpdate);
        <%= entityClass %> test<%= entityClass %> = <%= entityInstance %>List.get(<%= entityInstance %>List.size() - 1);
        <%_ for (idx in fields) { if (fields[idx].fieldType == 'ZonedDateTime') { _%>
        assertThat(test<%= entityClass %>.get<%=fields[idx].fieldInJavaBeanMethod%>()).isEqualTo(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
        <%_ } else if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
        assertThat(test<%= entityClass %>.get<%=fields[idx].fieldInJavaBeanMethod%>()).isEqualTo(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if (fields[idx].fieldType === 'ByteBuffer') { %>.rewind()<% } %>);
        assertThat(test<%= entityClass %>.get<%=fields[idx].fieldInJavaBeanMethod%>ContentType()).isEqualTo(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE);
        <%_ } else if (fields[idx].fieldType.toLowerCase() == 'boolean') { _%>
        assertThat(test<%= entityClass %>.is<%=fields[idx].fieldInJavaBeanMethod%>()).isEqualTo(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
        <%_ } else { _%>
        assertThat(test<%= entityClass %>.get<%=fields[idx].fieldInJavaBeanMethod%>()).isEqualTo(<%='UPDATED_' + fields[idx].fieldNameUnderscored.toUpperCase()%>);
        <%_ } } if (searchEngine == 'elasticsearch') { _%>

        // Validate the <%= entityClass %> in Elasticsearch
        <%= entityClass %> <%= entityInstance %>Es = <%= entityInstance %>SearchRepository.findOne(test<%= entityClass %>.getId());
        assertThat(<%= entityInstance %>Es).isEqualToComparingFieldByField(test<%= entityClass %>);
        <%_ } _%>
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void updateNonExisting<%= entityClass %>() throws Exception {
        int databaseSizeBeforeUpdate = <%= entityInstance %>Repository.findAll().size();

        // Create the <%= entityClass %><% if (dto == 'mapstruct') { %>
        <%= entityClass %>DTO <%= entityInstance %>DTO = <%= entityInstance %>Mapper.<%= entityInstance %>To<%= entityClass %>DTO(<%= entityInstance %>);<% } %>
        <%= entityClass %>Proto <%= entityInstance %>Proto = <%= entityInstance %>ProtoMapper.<%=instanceName%>To<%=entityClass%>Proto(<%=instanceName%>);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        stub.update<%= entityClass %>(<%= entityInstance %>Proto);

        // Validate the <%= entityClass %> in the database
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void delete<%= entityClass %>() throws Exception {
        // Initialize the database
<%_ if (dto != 'mapstruct') { _%>
        <%= entityInstance %>Service.save(<%= entityInstance %>);
<%_ } else { _%>
        <%= entityInstance %>Repository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);<% if (searchEngine == 'elasticsearch') { %>
        <%= entityInstance %>SearchRepository.save(<%= entityInstance %>);<%_ } _%>
<%_ } _%>

        int databaseSizeBeforeDelete = <%= entityInstance %>Repository.findAll().size();

        // Get the <%= entityInstance %>
        stub.delete<%= entityClass %>(<%=idProtoWrappedType%>.newBuilder().setValue(<%= entityInstance %>.getId()<% if (databaseType == 'cassandra') { %>.toString()<% } %>).build());
        <%_ if (searchEngine == 'elasticsearch') { _%>

        // Validate Elasticsearch is empty
        boolean <%= entityInstance %>ExistsInEs = <%= entityInstance %>SearchRepository.exists(<%= entityInstance %>.getId());
        assertThat(<%= entityInstance %>ExistsInEs).isFalse();
        <%_ } _%>

        // Validate the database is empty
        List<<%= entityClass %>> <%= entityInstance %>List = <%= entityInstance %>Repository.findAll();
        assertThat(<%= entityInstance %>List).hasSize(databaseSizeBeforeDelete - 1);
    }<% if (searchEngine == 'elasticsearch') { %>

    /*@Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void search<%= entityClass %>() throws Exception {
        // Initialize the database
<%_ if (dto != 'mapstruct') { _%>
        <%= entityInstance %>Service.save(<%= entityInstance %>);
<%_ } else { _%>
        <%= entityInstance %>Repository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(<%= entityInstance %>);
        <%= entityInstance %>SearchRepository.save(<%= entityInstance %>);
<%_ } _%>

        // Search the <%= entityInstance %>
        rest<%= entityClass %>MockMvc.perform(get("/api/_search/?query=id:" + <%= entityInstance %>.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))<% if (databaseType == 'sql') { %>
            .andExpect(jsonPath("$.[*].id").value(hasItem(<%= entityInstance %>.getId().intValue())))<% } %><% if (databaseType == 'mongodb') { %>
            .andExpect(jsonPath("$.[*].id").value(hasItem(<%= entityInstance %>.getId())))<% } %><% if (databaseType == 'cassandra') { %>
            .andExpect(jsonPath("$.[*].id").value(hasItem(<%= entityInstance %>.getId().toString())))<% } %><% for (idx in fields) {%>
            <%_ if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { _%>
            .andExpect(jsonPath("$.[*].<%=fields[idx].fieldName%>ContentType").value(hasItem(<%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%>_CONTENT_TYPE)))
            <%_ } _%>
            .andExpect(jsonPath("$.[*].<%=fields[idx].fieldName%>").value(hasItem(<% if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { %>Base64Utils.encodeToString(<% } else if (fields[idx].fieldType == 'ZonedDateTime') { %>sameInstant(<% } %><%='DEFAULT_' + fields[idx].fieldNameUnderscored.toUpperCase()%><% if ((fields[idx].fieldType == 'byte[]' || fields[idx].fieldType === 'ByteBuffer') && fields[idx].fieldTypeBlobContent != 'text') { %><% if (databaseType === 'cassandra') { %>.array()<% } %>)<% } else if (fields[idx].fieldType == 'Integer') { %><% } else if (fields[idx].fieldType == 'Long') { %>.intValue()<% } else if (fields[idx].fieldType == 'Float' || fields[idx].fieldType == 'Double') { %>.doubleValue()<% } else if (fields[idx].fieldType == 'BigDecimal') { %>.intValue()<% } else if (fields[idx].fieldType == 'Boolean') { %>.booleanValue()<% } else if (fields[idx].fieldType == 'ZonedDateTime') { %>)<% } else { %>.toString()<% } %>)))<% } %>;
    }*/<% } %>

}
