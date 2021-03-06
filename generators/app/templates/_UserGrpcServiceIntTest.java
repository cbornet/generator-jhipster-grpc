package <%= packageName %>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%= packageName %>.AbstractCassandraTest;
<%_ } _%>
import <%= packageName %>.<%=mainClass%>;
import <%= packageName %>.domain.User;
import <%= packageName %>.repository.UserRepository;
<%_ if (searchEngine === 'elasticsearch') { _%>
import <%= packageName %>.repository.search.UserSearchRepository;
<%_ } _%>
import <%= packageName %>.security.AuthoritiesConstants;
<%_ if (authenticationType !== 'oauth2') { _%>
import <%= packageName %>.service.MailService;
<%_ } _%>
import <%= packageName %>.service.UserService;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;

import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
<%_ if (searchEngine === 'elasticsearch') { _%>
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
<%_ } _%>
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
<%_ if (cacheManagerIsAvailable === true) { _%>
import org.springframework.cache.CacheManager;
<%_ } _%>
<%_ if (cacheProvider === 'memcached' ) { _%>
import org.springframework.cache.support.NoOpCacheManager;
<%_ } _%>
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
<%_ if (databaseType === 'sql') { _%>
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
<%_ } _%>
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

<%_ if (databaseType === 'couchbase') { _%>
import static <%= packageName %>.web.rest.TestUtil.mockAuthentication;
<%_ } _%>
<%_ if (searchEngine === 'elasticsearch') { _%>
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
<%_ } _%>
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
<%_ if (searchEngine === 'elasticsearch') { _%>
import static org.mockito.Mockito.when;
<%_ } _%>

/**
 * Test class for the UserGrpcService gRPC endpoint.
 *
 * @see UserGrpcService
 */
@SpringBootTest(classes = <%=mainClass%>.class)
public class UserGrpcServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String UPDATED_LOGIN = "jhipster";

    private static final String DEFAULT_PASSWORD = "passjohndoe";
    private static final String UPDATED_PASSWORD = "passjhipster";

    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String UPDATED_EMAIL = "jhipster@localhost";

    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String UPDATED_FIRSTNAME = "jhipsterFirstName";

    private static final String DEFAULT_LASTNAME = "doe";
    private static final String UPDATED_LASTNAME = "jhipsterLastName";

    <%_ if (databaseType !== 'cassandra') { _%>
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String UPDATED_IMAGEURL = "http://placehold.it/40x40";

    <%_ } _%>
    private static final String DEFAULT_LANGKEY = "en";
    private static final String UPDATED_LANGKEY = "fr";

    @Autowired
    private UserRepository userRepository;

    <%_ if (authenticationType !== 'oauth2') { _%>
    @Autowired
    private MailService mailService;

    <%_ } _%>
    @Autowired
    private UserService userService;

    @Autowired
    private UserProtoMapper userProtoMapper;

    <%_ if (searchEngine === 'elasticsearch') { _%>
    /**
     * This repository is mocked in the <%=packageName%>.repository.search test package.
     *
     * @see <%= packageName %>.repository.search.UserSearchRepositoryMockConfiguration
     */
    @Autowired
    private UserSearchRepository mockUserSearchRepository;

    <%_ } _%>
    <%_ if (cacheManagerIsAvailable === true) { _%>
    @Autowired
    private CacheManager cacheManager;

    <%_ } _%>
    private Server mockServer;

    private UserServiceGrpc.UserServiceBlockingStub stub;

    private User user;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        <%_ if (cacheManagerIsAvailable === true) { _%>
        cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).clear();
        cacheManager.getCache(UserRepository.USERS_BY_EMAIL_CACHE).clear();
        <%_ } _%>
        UserGrpcService userGrpcService = new UserGrpcService(<% if (authenticationType !== 'oauth2') { %>userRepository, mailService, <% } %>userService, userProtoMapper<% if (searchEngine === 'elasticsearch') { %>, mockUserSearchRepository<% } %>);
        String uniqueServerName = "Mock server for " + UserGrpcService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(userGrpcService).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = UserServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockServer.shutdownNow();
        mockServer.awaitTermination(10, TimeUnit.SECONDS);
    }

    /**
     * Create a User.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which has a required relationship to the User entity.
     */
    public static User createEntity(<% if (databaseType === 'sql') { %>EntityManager em<% } %>) {
        User user = new User();
        <%_ if (databaseType === 'cassandra' || authenticationType === 'oauth2') { _%>
        user.setId(UUID.randomUUID().toString());
        <%_ } _%>
        user.setLogin(DEFAULT_LOGIN + RandomStringUtils.randomAlphanumeric(10));
        <%_ if (authenticationType !== 'oauth2') { _%>
        user.setPassword(RandomStringUtils.random(60));
        <%_ } _%>
        user.setActivated(true);
        user.setEmail(RandomStringUtils.randomAlphanumeric(10) + DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        <%_ if (databaseType !== 'cassandra') { _%>
        user.setImageUrl(DEFAULT_IMAGEURL);
        <%_ } _%>
        user.setLangKey(DEFAULT_LANGKEY);
        return user;
    }

    @BeforeEach
    public void initTest() {
        <%_ if (databaseType === 'couchbase') { _%>
        mockAuthentication();
        <%_ } _%>
        <%_ if (databaseType !== 'sql') { _%>
        userRepository.deleteAll();
        <%_ } _%>
        user = createEntity(<% if (databaseType === 'sql') { %>null<% } %>);
        user.setLogin(DEFAULT_LOGIN);
        user.setEmail(DEFAULT_EMAIL);
    }
    <%_ if (authenticationType !== 'oauth2') { _%>

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void createUser() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        // Create the User
        UserProto userProto = UserProto.newBuilder()
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            <%_ if (databaseType !== 'cassandra') { _%>
            .setImageUrl(DEFAULT_IMAGEURL)
            <%_ } _%>
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        stub.createUser(userProto);

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate + 1);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(testUser.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(DEFAULT_EMAIL);
         <%_ if (databaseType !== 'cassandra') { _%>
        assertThat(testUser.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        <%_ } _%>
        assertThat(testUser.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void createUserWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        UserProto userProto = UserProto.newBuilder()
            <%_ if (databaseType === 'cassandra') { _%>
            .setId(UUID.randomUUID().toString())
            <%_ } else if (databaseType === 'mongodb' || databaseType === 'couchbase') { _%>
            .setId("1L")
            <%_ } else { _%>
            .setId(1L)
            <%_ } _%>
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            <%_ if (databaseType !== 'cassandra') { _%>
            .setImageUrl(DEFAULT_IMAGEURL)
            <%_ } _%>
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        try {
            stub.createUser(userProto);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            // Validate the User in the database
            List<User> userList = userRepository.findAll();
            assertThat(userList).hasSize(databaseSizeBeforeCreate);
        }

    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void createUserWithExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        UserProto userProto = UserProto.newBuilder()
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail("anothermail@localhost")
            .setActivated(true)
            <%_ if (databaseType !== 'cassandra') { _%>
            .setImageUrl(DEFAULT_IMAGEURL)
            <%_ } _%>
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        try {
            // Create the User
            stub.createUser(userProto);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
            // Validate the User in the database
            List<User> userList = userRepository.findAll();
            assertThat(userList).hasSize(databaseSizeBeforeCreate);
        }

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void createUserWithExistingEmail() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        UserProto userProto = UserProto.newBuilder()
            .setLogin("anotherlogin")
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            <%_ if (databaseType !== 'cassandra') { _%>
            .setImageUrl(DEFAULT_IMAGEURL)
            <%_ } _%>
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        try {
            // Create the User
            stub.createUser(userProto);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
            // Validate the User in the database
            List<User> userList = userRepository.findAll();
            assertThat(userList).hasSize(databaseSizeBeforeCreate);
        }

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeCreate);
    }
    <%_ } _%>

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void getAllUsers() throws Exception {
        // Initialize the database
        User savedUser = userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);

        // Get all the users
        <%_ if (['sql', 'mongodb', 'couchbase'].includes(databaseType)) { _%>
        PageRequest pageRequest = PageRequest.newBuilder()
            .addOrders(Order.newBuilder().setProperty("id").setDirection(Direction.DESC))
            .build();
        <%_ } _%>
        Optional<UserProto> maybeUser = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(stub.getAllUsers(<% if (['sql', 'mongodb', 'couchbase'].includes(databaseType)) { %>pageRequest<% } else { %>Empty.getDefaultInstance()<% } %>), Spliterator.ORDERED),
            false)
            .filter(userProto -> savedUser.getId().equals(userProto.getId()))
            .findAny();

        assertThat(maybeUser).isPresent();
        UserProto foundUser = maybeUser.orElse(null);
        assertThat(foundUser.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(foundUser.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(foundUser.getEmail()).isEqualTo(DEFAULT_EMAIL);
        <%_ if (databaseType !== 'cassandra') { _%>
        assertThat(foundUser.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        <%_ } _%>
        assertThat(foundUser.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void getUser() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);

        <%_ if (cacheManagerIsAvailable === true) { _%>
        assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin())).isNull();

        <%_ } _%>
        UserProto userProto = stub.getUser(StringValue.newBuilder().setValue(user.getLogin()).build());

        assertThat(userProto.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userProto.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(userProto.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(userProto.getEmail()).isEqualTo(DEFAULT_EMAIL);
        <%_ if (databaseType !== 'cassandra') { _%>
        assertThat(userProto.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        <%_ } _%>
        assertThat(userProto.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        <%_ if (cacheProvider === 'memcached') { _%>
        if (!(cacheManager instanceof NoOpCacheManager)) {
            assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin())).isNotNull();
        }
        <%_ } else if (cacheManagerIsAvailable === true) { _%>
        assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin())).isNotNull();
        <%_ } _%>
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void getNonExistingUser() throws Exception {
        try {
            stub.getUser(StringValue.newBuilder().setValue("unknown").build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }
    <%_ if (authenticationType !== 'oauth2') { _%>

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void updateUser() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);
        int databaseSizeBeforeUpdate = userRepository.findAll().size();

        // Update the user
        User updatedUser = userRepository.findById(user.getId()).orElseThrow(RuntimeException::new);

        UserProto userProto = UserProto.newBuilder()
            .setId(updatedUser.getId())
            .setLogin(updatedUser.getLogin())
            .setPassword(UPDATED_PASSWORD)
            .setFirstName(UPDATED_FIRSTNAME)
            .setLastName(UPDATED_LASTNAME)
            .setEmail(UPDATED_EMAIL)
            .setActivated(updatedUser.getActivated())
            <%_ if (databaseType !== 'cassandra') { _%>
            .setImageUrl(UPDATED_IMAGEURL)
            <%_ } _%>
            .setLangKey(UPDATED_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        stub.updateUser(userProto);

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeUpdate);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
        <%_ if (databaseType !== 'cassandra') { _%>
        assertThat(testUser.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
        <%_ } _%>
        assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void updateUserLogin() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);
        int databaseSizeBeforeUpdate = userRepository.findAll().size();

        // Update the user
        User updatedUser = userRepository.findById(user.getId()).orElseThrow(RuntimeException::new);

        UserProto userProto = UserProto.newBuilder()
            .setId(updatedUser.getId())
            .setLogin(UPDATED_LOGIN)
            .setPassword(UPDATED_PASSWORD)
            .setFirstName(UPDATED_FIRSTNAME)
            .setLastName(UPDATED_LASTNAME)
            .setEmail(UPDATED_EMAIL)
            .setActivated(updatedUser.getActivated())
            <%_ if (databaseType !== 'cassandra') { _%>
            .setImageUrl(UPDATED_IMAGEURL)
            <%_ } _%>
            .setLangKey(UPDATED_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        stub.updateUser(userProto);

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeUpdate);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getLogin()).isEqualTo(UPDATED_LOGIN);
        assertThat(testUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
        <%_ if (databaseType !== 'cassandra') { _%>
        assertThat(testUser.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
        <%_ } _%>
        assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void updateUserExistingEmail() throws Exception {
        // Initialize the database with 2 users
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);

        User anotherUser = new User();
        <%_ if (databaseType === 'cassandra') { _%>
        anotherUser.setId(UUID.randomUUID().toString());
        <%_ } _%>
        anotherUser.setLogin("jhipster");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("jhipster@localhost");
        anotherUser.setFirstName("java");
        anotherUser.setLastName("hipster");
        <%_ if (databaseType !== 'cassandra') { _%>
        anotherUser.setImageUrl("");
        <%_ } _%>
        anotherUser.setLangKey("en");
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(anotherUser);

        // Update the user
        User updatedUser = userRepository.findById(user.getId()).orElseThrow(RuntimeException::new);

        UserProto userProto = UserProto.newBuilder()
            .setId(updatedUser.getId())
            .setLogin(updatedUser.getLogin())
            .setPassword(updatedUser.getPassword())
            .setFirstName(updatedUser.getFirstName())
            .setLastName(updatedUser.getLastName())
            .setEmail("jhipster@localhost")  // this email should already be used by anotherUser
            .setActivated(updatedUser.getActivated())
            <%_ if (databaseType !== 'cassandra') { _%>
            .setImageUrl(updatedUser.getImageUrl())
            <%_ } _%>
            .setLangKey(updatedUser.getLangKey())
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        try {
            // Create the User
            stub.updateUser(userProto);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
        }
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void updateUserExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);

        User anotherUser = new User();
        <%_ if (databaseType === 'cassandra') { _%>
        anotherUser.setId(UUID.randomUUID().toString());
        <%_ } _%>
        anotherUser.setLogin("jhipster");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("jhipster@localhost");
        anotherUser.setFirstName("java");
        anotherUser.setLastName("hipster");
        <%_ if (databaseType !== 'cassandra') { _%>
        anotherUser.setImageUrl("");
        <%_ } _%>
        anotherUser.setLangKey("en");
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(anotherUser);

        // Update the user
        User updatedUser = userRepository.findById(user.getId()).orElseThrow(RuntimeException::new);

        UserProto userProto = UserProto.newBuilder()
            .setId(updatedUser.getId())
            .setLogin("jhipster") // this login should already be used by anotherUser
            .setPassword(updatedUser.getPassword())
            .setFirstName(updatedUser.getFirstName())
            .setLastName(updatedUser.getLastName())
            .setEmail(updatedUser.getEmail())
            .setActivated(updatedUser.getActivated())
            <%_ if (databaseType !== 'cassandra') { _%>
            .setImageUrl(updatedUser.getImageUrl())
            <%_ } _%>
            .setLangKey(updatedUser.getLangKey())
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        try {
            // Create the User
            stub.updateUser(userProto);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
        }
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void deleteUser() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);
        int databaseSizeBeforeDelete = userRepository.findAll().size();

        // Delete the user
        stub.deleteUser(StringValue.newBuilder().setValue(user.getLogin()).build());

        // Validate the database is empty
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeDelete - 1);
        <%_ if (cacheManagerIsAvailable === true) { _%>

        assertThat(cacheManager.getCache(UserRepository.USERS_BY_LOGIN_CACHE).get(user.getLogin())).isNull();
        <%_ } _%>
    }
    <%_ } _%>
    <%_ if ((databaseType === 'sql' || databaseType === 'mongodb' || databaseType === 'couchbase')
        && (!(authenticationType === 'oauth2' && applicationType === 'microservice'))) { _%>

    @Test
    public void getAllAuthorities() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            DEFAULT_EMAIL,
            DEFAULT_PASSWORD,
            Collections.singletonList(new SimpleGrantedAuthority(AuthoritiesConstants.ADMIN))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        List<String> roles = new ArrayList<>();
        stub.getAllAuthorities(Empty.getDefaultInstance()).forEachRemaining(role -> roles.add(role.getValue()));
        assertThat(roles).contains(AuthoritiesConstants.ADMIN, AuthoritiesConstants.USER);
    }

    @Test
    public void getAllAuthoritiesRejected() throws Exception {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            DEFAULT_EMAIL,
            DEFAULT_PASSWORD,
            Collections.singletonList(new SimpleGrantedAuthority(AuthoritiesConstants.USER))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            List<String> roles = new ArrayList<>();
            stub.getAllAuthorities(Empty.getDefaultInstance()).forEachRemaining(role -> roles.add(role.getValue()));
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e){
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.PERMISSION_DENIED);
        }
    }
    <%_ } _%>
    <%_ if (searchEngine === 'elasticsearch') { _%>

    @Test
    @Transactional
    public void searchUsers() throws Exception {

        // Initialize the database
        User savedUser = userRepository.saveAndFlush(user);

        when(mockUserSearchRepository.search(
            queryStringQuery("id:" + user.getId()),
            org.springframework.data.domain.PageRequest.of(0, 20, Sort.Direction.DESC, "id"))
        ).thenReturn(new PageImpl<>(
            Collections.singletonList(user),
            org.springframework.data.domain.PageRequest.of(0, 1), 1));

        // Search the users
        UserSearchPageRequest query = UserSearchPageRequest.newBuilder()
            .setPageRequest(PageRequest.newBuilder()
                .addOrders(Order.newBuilder()
                    .setProperty("id")
                    .setDirection(Direction.DESC)
                )
            )
            .setQuery((StringValue.newBuilder().setValue("id:" + savedUser.getId()).build()))
            .build();

        Optional<UserProto> maybeUser = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(stub.searchUsers(query), Spliterator.ORDERED),
                false)
            .filter(userProto -> savedUser.getId().equals(userProto.getId()))
            .findAny();

        assertThat(maybeUser).isPresent();
        UserProto foundUser = maybeUser.orElse(null);
        assertThat(foundUser.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(foundUser.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(foundUser.getEmail()).isEqualTo(DEFAULT_EMAIL);
        <%_ if (databaseType !== 'cassandra') { _%>
        assertThat(foundUser.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        <%_ } _%>
        assertThat(foundUser.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
    }
    <%_ } _%>

}
