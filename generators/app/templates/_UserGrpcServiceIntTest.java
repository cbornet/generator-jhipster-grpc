package <%=packageName%>.grpc;

import <%=packageName%>.JwtApp;
import <%=packageName%>.domain.User;
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.service.MailService;
import <%=packageName%>.service.UserService;
import <%=packageName%>.web.rest.UserResource;

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
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * Test class for the UserGrpcService gRPC endpoint.
 *
 * @see UserGrpcService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = JwtApp.class)
public class UserGrpcServiceIntTest {

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

    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String UPDATED_IMAGEURL = "http://placehold.it/40x40";

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
    public static User createEntity() {
        User user = new User();
        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.random(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        return user;
    }

    @Before
    public void initTest() {
        userRepository.deleteAll();
        user = createEntity();
    }

    @Test
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
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities("ROLE_USER")
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
        assertThat(testUser.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(testUser.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
    }

    @Test
    public void createUserWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        UserProto userProto = UserProto.newBuilder()
            .setId("1L")
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities("ROLE_USER")
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
    public void createUserWithExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        UserProto userProto = UserProto.newBuilder()
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail("anothermail@localhost")
            .setActivated(true)
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities("ROLE_USER")
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
    public void createUserWithExistingEmail() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeCreate = userRepository.findAll().size();

        UserProto userProto = UserProto.newBuilder()
            .setLogin("anotherlogin")
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities("ROLE_USER")
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
    public void getAllUsers() throws Exception {
        // Initialize the database
        userRepository.save(user);

        // Get all the users
        List<UserProto> users = new ArrayList<>();
        stub.getAllUsers(PageRequest.getDefaultInstance()).forEachRemaining(users::add);

        assertThat(users).extracting("login").contains(DEFAULT_LOGIN);
        assertThat(users).extracting("firstName").contains(DEFAULT_FIRSTNAME);
        assertThat(users).extracting("lastName").contains(DEFAULT_LASTNAME);
        assertThat(users).extracting("email").contains(DEFAULT_EMAIL);
        assertThat(users).extracting("imageUrl").contains(DEFAULT_IMAGEURL);
        assertThat(users).extracting("langKey").contains(DEFAULT_LANGKEY);
    }

    @Test
    public void getUser() throws Exception {
        // Initialize the database
        userRepository.save(user);

        UserProto userProto = stub.getUser(StringValue.newBuilder().setValue(user.getLogin()).build());

        assertThat(userProto.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userProto.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(userProto.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(userProto.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(userProto.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(userProto.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
    }

    @Test
    public void getNonExistingUser() throws Exception {
        try {
            stub.getUser(StringValue.newBuilder().setValue("unknown").build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
        }
    }

    @Test
    public void updateUser() throws Exception {
        // Initialize the database
        userRepository.save(user);
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
            .setImageUrl(UPDATED_IMAGEURL)
            .setLangKey(UPDATED_LANGKEY)
            .setCreatedBy(updatedUser.getCreatedBy())
            .setCreatedDate(ProtobufUtil.zonedDateTimeToTimestamp(updatedUser.getCreatedDate()))
            .setLastModifiedBy(updatedUser.getLastModifiedBy())
            .setLastModifiedDate(ProtobufUtil.zonedDateTimeToTimestamp(updatedUser.getLastModifiedDate()))
            .addAuthorities("ROLE_USER")
            .build();

        stub.updateUser(userProto);

        // Validate the User in the database
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeUpdate);
        User testUser = userList.get(userList.size() - 1);
        assertThat(testUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(testUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(testUser.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(testUser.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
        assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
    }

    @Test
    public void updateUserLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);
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
            .setImageUrl(UPDATED_IMAGEURL)
            .setLangKey(UPDATED_LANGKEY)
            .setCreatedBy(updatedUser.getCreatedBy())
            .setCreatedDate(ProtobufUtil.zonedDateTimeToTimestamp(updatedUser.getCreatedDate()))
            .setLastModifiedBy(updatedUser.getLastModifiedBy())
            .setLastModifiedDate(ProtobufUtil.zonedDateTimeToTimestamp(updatedUser.getLastModifiedDate()))
            .addAuthorities("ROLE_USER")
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
        assertThat(testUser.getImageUrl()).isEqualTo(UPDATED_IMAGEURL);
        assertThat(testUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
    }

    @Test
    public void updateUserExistingEmail() throws Exception {
        // Initialize the database with 2 users
        userRepository.save(user);

        User anotherUser = new User();
        anotherUser.setLogin("jhipster");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("jhipster@localhost");
        anotherUser.setFirstName("java");
        anotherUser.setLastName("hipster");
        anotherUser.setImageUrl("");
        anotherUser.setLangKey("en");
        userRepository.save(anotherUser);

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
            .setImageUrl(updatedUser.getImageUrl())
            .setLangKey(updatedUser.getLangKey())
            .setCreatedBy(updatedUser.getCreatedBy())
            .setCreatedDate(ProtobufUtil.zonedDateTimeToTimestamp(updatedUser.getCreatedDate()))
            .setLastModifiedBy(updatedUser.getLastModifiedBy())
            .setLastModifiedDate(ProtobufUtil.zonedDateTimeToTimestamp(updatedUser.getLastModifiedDate()))
            .addAuthorities("ROLE_USER")
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
    public void updateUserExistingLogin() throws Exception {
        // Initialize the database
        userRepository.save(user);

        User anotherUser = new User();
        anotherUser.setLogin("jhipster");
        anotherUser.setPassword(RandomStringUtils.random(60));
        anotherUser.setActivated(true);
        anotherUser.setEmail("jhipster@localhost");
        anotherUser.setFirstName("java");
        anotherUser.setLastName("hipster");
        anotherUser.setImageUrl("");
        anotherUser.setLangKey("en");
        userRepository.save(anotherUser);

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
            .setImageUrl(updatedUser.getImageUrl())
            .setLangKey(updatedUser.getLangKey())
            .setCreatedBy(updatedUser.getCreatedBy())
            .setCreatedDate(ProtobufUtil.zonedDateTimeToTimestamp(updatedUser.getCreatedDate()))
            .setLastModifiedBy(updatedUser.getLastModifiedBy())
            .setLastModifiedDate(ProtobufUtil.zonedDateTimeToTimestamp(updatedUser.getLastModifiedDate()))
            .addAuthorities("ROLE_USER")
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
    public void deleteUser() throws Exception {
        // Initialize the database
        userRepository.save(user);
        int databaseSizeBeforeDelete = userRepository.findAll().size();

        // Delete the user
        stub.deleteUser(StringValue.newBuilder().setValue(user.getLogin()).build());

        // Validate the database is empty
        List<User> userList = userRepository.findAll();
        assertThat(userList).hasSize(databaseSizeBeforeDelete - 1);
    }

}
