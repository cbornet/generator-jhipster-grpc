package <%=packageName%>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%=packageName%>.AbstractCassandraTest;
<%_ } _%>
import <%=packageName%>.<%=mainClass%>;
import <%=packageName%>.domain.User;
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.security.AuthoritiesConstants;
import <%=packageName%>.service.MailService;
import <%=packageName%>.service.UserService;

<%_ if (databaseType === 'cassandra') { _%>
import com.google.protobuf.Empty;
<%_ } _%>
import com.google.protobuf.StringValue;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
<%_ if (databaseType === 'sql') { _%>
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
<%_ } _%>
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
<%_ if (databaseType === 'cassandra') { _%>
import java.util.UUID;
<%_ } _%>

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * Test class for the UserGrpcService gRPC endpoint.
 *
 * @see UserGrpcService
 */
@RunWith(SpringRunner.class)
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

    @Autowired
    private MailService mailService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserProtoMapper userProtoMapper;

    private Server mockServer;

    private UserServiceGrpc.UserServiceBlockingStub stub;

    private User user;

    @Before
    public void setUp() throws IOException {
        UserGrpcService userGrpcService = new UserGrpcService(userRepository, mailService, userService, userProtoMapper);
        String uniqueServerName = "Mock server for " + UserGrpcService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(userGrpcService).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = UserServiceGrpc.newBlockingStub(channelBuilder.build());
    }

    @After
    public void tearDown() {
        mockServer.shutdownNow();
    }

        /**
     * Create a User.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which has a required relationship to the User entity.
     */
    public static User createEntity(<% if (databaseType === 'sql') { %>EntityManager em<% } %>) {
        User user = new User();
        <%_ if (databaseType === 'cassandra') { _%>
        user.setId(UUID.randomUUID().toString());
        <%_ } _%>
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        <%_ if (databaseType !== 'cassandra') { _%>
        user.setImageUrl(DEFAULT_IMAGEURL);
        <%_ } _%>
        user.setLangKey(DEFAULT_LANGKEY);
        return user;
    }

    @Before
    public void initTest() {
        <%_ if (databaseType !== 'sql') { _%>
        userRepository.deleteAll();
        <%_ } _%>
        user = createEntity(<% if (databaseType === 'sql') { %>null<% } %>);
    }

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
            <%_ } else if (databaseType === 'mongodb') { _%>
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

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void getAllUsers() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);

        // Get all the users
        List<UserProto> users = new ArrayList<>();
        stub.getAllUsers(<% if (databaseType == 'sql' || databaseType == 'mongodb') { %>PageRequest<% } else { %>Empty<% } %>.getDefaultInstance()).forEachRemaining(users::add);
        assertThat(users).extracting("login").contains(DEFAULT_LOGIN);
        assertThat(users).extracting("firstName").contains(DEFAULT_FIRSTNAME);
        assertThat(users).extracting("lastName").contains(DEFAULT_LASTNAME);
        assertThat(users).extracting("email").contains(DEFAULT_EMAIL);
        <%_ if (databaseType !== 'cassandra') { _%>
        assertThat(users).extracting("imageUrl").contains(DEFAULT_IMAGEURL);
        <%_ } _%>
        assertThat(users).extracting("langKey").contains(DEFAULT_LANGKEY);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void getUser() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);

        UserProto userProto = stub.getUser(StringValue.newBuilder().setValue(user.getLogin()).build());

        assertThat(userProto.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userProto.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(userProto.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(userProto.getEmail()).isEqualTo(DEFAULT_EMAIL);
        <%_ if (databaseType !== 'cassandra') { _%>
        assertThat(userProto.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        <%_ } _%>
        assertThat(userProto.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
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

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void updateUser() throws Exception {
        // Initialize the database
        userRepository.save<% if (databaseType === 'sql') { %>AndFlush<% } %>(user);
        int databaseSizeBeforeUpdate = userRepository.findAll().size();

        // Update the user
        User updatedUser = userRepository.findOne(user.getId());

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
            <%_ if (databaseType !== 'cassandra') { _%>
            .setCreatedBy(updatedUser.getCreatedBy())
            .setCreatedDate(ProtobufMappers.zonedDateTimeToTimestamp(updatedUser.getCreatedDate()))
            .setLastModifiedBy(updatedUser.getLastModifiedBy())
            .setLastModifiedDate(ProtobufMappers.zonedDateTimeToTimestamp(updatedUser.getLastModifiedDate()))
            <%_ } _%>
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
        User updatedUser = userRepository.findOne(user.getId());

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
            <%_ if (databaseType !== 'cassandra') { _%>
            .setCreatedBy(updatedUser.getCreatedBy())
            .setCreatedDate(ProtobufMappers.zonedDateTimeToTimestamp(updatedUser.getCreatedDate()))
            .setLastModifiedBy(updatedUser.getLastModifiedBy())
            .setLastModifiedDate(ProtobufMappers.zonedDateTimeToTimestamp(updatedUser.getLastModifiedDate()))
            <%_ } _%>
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
        User updatedUser = userRepository.findOne(user.getId());

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
            <%_ if (databaseType !== 'cassandra') { _%>
            .setCreatedBy(updatedUser.getCreatedBy())
            .setCreatedDate(ProtobufMappers.zonedDateTimeToTimestamp(updatedUser.getCreatedDate()))
            .setLastModifiedBy(updatedUser.getLastModifiedBy())
            .setLastModifiedDate(ProtobufMappers.zonedDateTimeToTimestamp(updatedUser.getLastModifiedDate()))
            <%_ } _%>
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
        User updatedUser = userRepository.findOne(user.getId());

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
            <%_ if (databaseType !== 'cassandra') { _%>
            .setCreatedBy(updatedUser.getCreatedBy())
            .setCreatedDate(ProtobufMappers.zonedDateTimeToTimestamp(updatedUser.getCreatedDate()))
            .setLastModifiedBy(updatedUser.getLastModifiedBy())
            .setLastModifiedDate(ProtobufMappers.zonedDateTimeToTimestamp(updatedUser.getLastModifiedDate()))
            <%_ } _%>
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
    }

}
