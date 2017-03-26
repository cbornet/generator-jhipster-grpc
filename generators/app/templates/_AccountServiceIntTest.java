package <%=packageName%>.grpc;

<%_ if (databaseType === 'cassandra') { _%>
import <%=packageName%>.AbstractCassandraTest;
<%_ } _%>
import <%=packageName%>.<%= mainClass %>;
import <%=packageName%>.domain.Authority;
<%_ if (authenticationType == 'session') { _%>
import <%=packageName%>.domain.PersistentToken;
<%_ } _%>
import <%=packageName%>.domain.User;
import <%=packageName%>.repository.AuthorityRepository;
<%_ if (authenticationType == 'session') { _%>
import <%=packageName%>.repository.PersistentTokenRepository;
<%_ } _%>
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.security.AuthoritiesConstants;
import <%=packageName%>.service.MailService;
import <%=packageName%>.service.UserService;
import <%=packageName%>.web.rest.UserResourceIntTest;

import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = <%= mainClass %>.class)
public class AccountServiceIntTest <% if (databaseType === 'cassandra') { %>extends AbstractCassandraTest <% } %>{

    private static final String DEFAULT_LOGIN = "johndoe";

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
    private AuthorityRepository authorityRepository;

    @Autowired
    private UserService userService;

    <%_ if (authenticationType == 'session') { _%>
    @Autowired
    private PersistentTokenRepository persistentTokenRepository;

    <%_ } _%>
    @Autowired
    private UserProtoMapper userProtoMapper;

    @Autowired
    private EntityManager em;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserService mockUserService;

    @Mock
    private MailService mockMailService;

    private Server mockServer;

    private Server mockUserServer;

    private AccountServiceGrpc.AccountServiceBlockingStub stub;

    private AccountServiceGrpc.AccountServiceBlockingStub userStub;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockMailService).sendActivationEmail(anyObject());

        AccountService service =
            new AccountService(userRepository, userService, mockMailService,<% if (authenticationType == 'session') { %> persistentTokenRepository,<% } %> userProtoMapper);

        AccountService userService =
            new AccountService(userRepository, mockUserService, mockMailService,<% if (authenticationType == 'session') { %> persistentTokenRepository, <% } %>userProtoMapper);

        String uniqueServerName = "Mock server for " + AccountService.class;
        mockServer = InProcessServerBuilder
            .forName(uniqueServerName).directExecutor().addService(service).build().start();
        InProcessChannelBuilder channelBuilder =
            InProcessChannelBuilder.forName(uniqueServerName).directExecutor();
        stub = AccountServiceGrpc.newBlockingStub(channelBuilder.build());

        String uniqueUserServerName = "Mock user server for " + AccountService.class;
        mockUserServer = InProcessServerBuilder
            .forName(uniqueUserServerName).directExecutor().addService(userService).build().start();
        InProcessChannelBuilder userChannelBuilder =
            InProcessChannelBuilder.forName(uniqueUserServerName).directExecutor();
        userStub = AccountServiceGrpc.newBlockingStub(userChannelBuilder.build());
    }

    @After
    public void tearDown() {
        mockServer.shutdownNow();
        mockUserServer.shutdown();
    }

    @Test
    public void testNonAuthenticatedUser() throws Exception {
        StringValue login = userStub.isAuthenticated(Empty.newBuilder().build());
        assertThat(login.getValue()).isEmpty();
    }

    @Test
    @WithMockUser(username = "test")
    public void testAuthenticatedUser() throws Exception {
        StringValue login = userStub.isAuthenticated(Empty.newBuilder().build());
        assertThat(login.getValue()).isEqualTo("test");
    }

    @Test
    public void testGetExistingAccount() throws Exception {
        Set<Authority> authorities = new HashSet<>();
        Authority authority = new Authority();
        authority.setName(AuthoritiesConstants.ADMIN);
        authorities.add(authority);

        User user = new User();
        user.setLogin(DEFAULT_LOGIN);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setEmail(DEFAULT_EMAIL);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);
        user.setAuthorities(authorities);
        when(mockUserService.getUserWithAuthorities()).thenReturn(user);

        UserProto userProto = userStub.getAccount(Empty.newBuilder().build());
        assertThat(userProto.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userProto.getFirstName()).isEqualTo(DEFAULT_FIRSTNAME);
        assertThat(userProto.getLastName()).isEqualTo(DEFAULT_LASTNAME);
        assertThat(userProto.getEmail()).isEqualTo(DEFAULT_EMAIL);
        assertThat(userProto.getImageUrl()).isEqualTo(DEFAULT_IMAGEURL);
        assertThat(userProto.getLangKey()).isEqualTo(DEFAULT_LANGKEY);
        assertThat(userProto.getAuthoritiesList()).containsExactly(AuthoritiesConstants.ADMIN);
    }

    @Test
    public void testGetUnknownAccount() throws Exception {
        when(mockUserService.getUserWithAuthorities()).thenReturn(null);
        try {
            userStub.getAccount(Empty.newBuilder().build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        }
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterValid() throws Exception {
        UserProto validUser = UserProto.newBuilder()
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        stub.registerAccount(validUser);

        assertThat(userRepository.findOneByLogin(DEFAULT_LOGIN)).isPresent();
    }

    @Test
    public void testRegisterInvalid() throws Exception {
        UserProto invalidUser = UserProto.newBuilder()
            .setLogin("funky-log!n")
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        try {
            stub.registerAccount(invalidUser);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            em.clear();
        }
        assertThat(userRepository.findOneByEmail(DEFAULT_EMAIL)).isNotPresent();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterDuplicateLogin() throws Exception {
        // Good
        UserProto validUser = UserProto.newBuilder()
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        // Duplicate login, different email
        UserProto duplicatedUser = UserProto.newBuilder()
            .setId(validUser.getId())
            .setLogin(validUser.getLogin())
            .setPassword(validUser.getPassword())
            .setFirstName(validUser.getFirstName())
            .setLastName(validUser.getLastName())
            .setEmail("jhipster@localhost")
            .setActivated(validUser.getActivated())
            .setImageUrl(validUser.getImageUrl())
            .setLangKey(validUser.getLangKey())
            .setCreatedBy(validUser.getCreatedBy())
            .setCreatedDate(validUser.getCreatedDate())
            .setLastModifiedBy(validUser.getLastModifiedBy())
            .setLastModifiedDate(validUser.getLastModifiedDate())
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        stub.registerAccount(validUser);

        try {
            stub.registerAccount(duplicatedUser);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
            em.clear();
        }
        assertThat(userRepository.findOneByEmail("jhipster@localhost")).isNotPresent();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterDuplicateEmail() throws Exception {
        // Good
        UserProto validUser = UserProto.newBuilder()
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        // Duplicate login, different email
        UserProto duplicatedUser = UserProto.newBuilder()
            .setId(validUser.getId())
            .setLogin("anotherlogin")
            .setPassword(validUser.getPassword())
            .setFirstName(validUser.getFirstName())
            .setLastName(validUser.getLastName())
            .setEmail(validUser.getEmail())
            .setActivated(validUser.getActivated())
            .setImageUrl(validUser.getImageUrl())
            .setLangKey(validUser.getLangKey())
            .setCreatedBy(validUser.getCreatedBy())
            .setCreatedDate(validUser.getCreatedDate())
            .setLastModifiedBy(validUser.getLastModifiedBy())
            .setLastModifiedDate(validUser.getLastModifiedDate())
            .addAuthorities(AuthoritiesConstants.USER)
            .build();

        stub.registerAccount(validUser);

        try {
            stub.registerAccount(duplicatedUser);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
            em.clear();
        }
        assertThat(userRepository.findOneByLogin("anotherlogin")).isNotPresent();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRegisterAdminIsIgnored() throws Exception {
        UserProto user = UserProto.newBuilder()
            .setLogin(DEFAULT_LOGIN)
            .setPassword(DEFAULT_PASSWORD)
            .setFirstName(DEFAULT_FIRSTNAME)
            .setLastName(DEFAULT_LASTNAME)
            .setEmail(DEFAULT_EMAIL)
            .setActivated(true)
            .setImageUrl(DEFAULT_IMAGEURL)
            .setLangKey(DEFAULT_LANGKEY)
            .addAuthorities(AuthoritiesConstants.ADMIN)
            .build();

        stub.registerAccount(user);

        Optional<User> userDup = userRepository.findOneByLogin(DEFAULT_LOGIN);
        assertThat(userDup).isPresent();
        assertThat(userDup.get().getAuthorities())
            .hasSize(1)
            .containsExactly(authorityRepository.findOne(AuthoritiesConstants.USER));
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testActivateAccount() throws Exception {
        final String activationKey = "some activationKey";
        User user = UserResourceIntTest.createEntity(null);
        user.setActivated(false);
        user.setActivationKey(activationKey);

        userRepository.saveAndFlush(user);

        UserProto userProto = stub.activateAccount(StringValue.newBuilder().setValue(activationKey).build());
        assertThat(userProto.getLogin()).isEqualTo(DEFAULT_LOGIN);
        assertThat(userProto.getActivated()).isTrue();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testActivateAccountWithWrongKey() throws Exception {
        try {
            stub.activateAccount(StringValue.newBuilder().setValue("some wrong key").build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        }
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    @WithMockUser(DEFAULT_LOGIN)
    public void testSaveAccount() throws Exception {
        User user = UserResourceIntTest.createEntity(null);
        user.setAuthorities(new HashSet<>());
        userRepository.saveAndFlush(user);

        UserProto userProto = UserProto.newBuilder()
            .setLogin(DEFAULT_LOGIN)
            .setPassword(UPDATED_PASSWORD)
            .setFirstName(UPDATED_FIRSTNAME)
            .setLastName(UPDATED_LASTNAME)
            .setEmail(UPDATED_EMAIL)
            .setActivated(false)
            .setImageUrl(UPDATED_IMAGEURL)
            .setLangKey(UPDATED_LANGKEY)
            .addAuthorities(AuthoritiesConstants.ADMIN)
            .build();

        stub.saveAccount(userProto);

        User updatedUser = userRepository.findOneByLogin(DEFAULT_LOGIN).get();
        assertThat(updatedUser.getFirstName()).isEqualTo(UPDATED_FIRSTNAME);
        assertThat(updatedUser.getLastName()).isEqualTo(UPDATED_LASTNAME);
        assertThat(updatedUser.getEmail()).isEqualTo(UPDATED_EMAIL);
        assertThat(updatedUser.getLangKey()).isEqualTo(UPDATED_LANGKEY);
        assertThat(updatedUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(updatedUser.getImageUrl()).isEqualTo(user.getImageUrl());
        assertThat(updatedUser.getActivated()).isEqualTo(user.getActivated());
        assertThat(updatedUser.getAuthorities()).isEmpty();
    }

    @Test
    @WithMockUser(DEFAULT_LOGIN)
    public void testSaveInvalidEmail() throws Exception {
        User user = UserResourceIntTest.createEntity(null);
        user.setAuthorities(new HashSet<>());
        userRepository.saveAndFlush(user);

        UserProto invalidUser = UserProto.newBuilder()
            .setPassword(UPDATED_PASSWORD)
            .setFirstName(UPDATED_FIRSTNAME)
            .setLastName(UPDATED_LASTNAME)
            .setEmail("Invalid email")
            .setActivated(false)
            .setImageUrl(UPDATED_IMAGEURL)
            .setLangKey(UPDATED_LANGKEY)
            .addAuthorities(AuthoritiesConstants.ADMIN)
            .build();

        try {
            stub.saveAccount(invalidUser);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
            assertThat(userRepository.findOneByEmail("Invalid email")).isNotPresent();
        } finally {
            userRepository.delete(user);
        }
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    @WithMockUser(DEFAULT_LOGIN)
    public void testSaveAccountExistingEmail() throws Exception {
        User user = UserResourceIntTest.createEntity(null);
        userRepository.saveAndFlush(user);

        User anotherUser = UserResourceIntTest.createEntity(null);
        anotherUser.setLogin("anotherUser");
        anotherUser.setEmail("another.email@localhost.com");
        userRepository.saveAndFlush(anotherUser);

        UserProto userProto = UserProto.newBuilder()
            .setPassword(UPDATED_PASSWORD)
            .setFirstName(UPDATED_FIRSTNAME)
            .setLastName(UPDATED_LASTNAME)
            .setEmail("another.email@localhost.com")
            .setActivated(false)
            .setImageUrl(UPDATED_IMAGEURL)
            .setLangKey(UPDATED_LANGKEY)
            .addAuthorities(AuthoritiesConstants.ADMIN)
            .build();

        try {
            stub.saveAccount(userProto);
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.ALREADY_EXISTS);
        }
        User updatedUser = userRepository.findOneByLogin(DEFAULT_LOGIN).get();
        assertThat(updatedUser.getEmail()).isEqualTo(user.getEmail());
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    @WithMockUser(DEFAULT_LOGIN)
    public void testChangePassword() {
        User user = UserResourceIntTest.createEntity(null);
        userRepository.saveAndFlush(user);

        stub.changePassword(StringValue.newBuilder().setValue("new password").build());

        User updatedUser = userRepository.findOneByLogin(DEFAULT_LOGIN).get();
        assertThat(passwordEncoder.matches("new password", updatedUser.getPassword())).isTrue();
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    @WithMockUser(DEFAULT_LOGIN)
    public void testChangePasswordTooSmall() {
        User user = UserResourceIntTest.createEntity(null);
        userRepository.saveAndFlush(user);

        try {
            stub.changePassword(StringValue.newBuilder().setValue("foo").build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        }
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    @WithMockUser(DEFAULT_LOGIN)
    public void testChangePasswordTooLong() {
        User user = UserResourceIntTest.createEntity(null);
        userRepository.saveAndFlush(user);

        try {
            String longPassword = Stream.generate(() -> String.valueOf("A")).limit(101).collect(Collectors.joining());
            assertThat(longPassword.length()).isEqualTo(101);
            stub.changePassword(StringValue.newBuilder().setValue(longPassword).build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        }
    }

    <%_ if (authenticationType == 'session') { _%>
    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    @WithMockUser(DEFAULT_LOGIN)
    public void testGetCurrentSessions() {
        User user = UserResourceIntTest.createEntity(null);
        userRepository.saveAndFlush(user);

        PersistentToken token = new PersistentToken();
        token.setSeries("1111-1111");
        token.setUser(user);
        token.setTokenValue("1111-1111-data");
        token.setTokenDate(LocalDate.of(2017, 3, 23));
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test agent");
        persistentTokenRepository.saveAndFlush(token);

        <%=packageName%>.grpc.PersistentToken tokenProto = stub.getCurrentSessions(Empty.newBuilder().build()).next();
        assertThat(tokenProto.getSeries()).isEqualTo("1111-1111");
        assertThat(tokenProto.getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(tokenProto.getUserAgent()).isEqualTo("Test agent");
        assertThat(tokenProto.getTokenDate().getYear()).isEqualTo(2017);
        assertThat(tokenProto.getTokenDate().getMonth()).isEqualTo(3);
        assertThat(tokenProto.getTokenDate().getDay()).isEqualTo(23);
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    @WithMockUser(DEFAULT_LOGIN)
    public void testInvalidateSession() {
        User user = UserResourceIntTest.createEntity(null);
        userRepository.saveAndFlush(user);

        PersistentToken token = new PersistentToken();
        token.setSeries("1111-1111");
        token.setUser(user);
        token.setTokenValue("1111-1111-data");
        token.setTokenDate(LocalDate.now());
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test agent");
        persistentTokenRepository.saveAndFlush(token);

        assertThat(persistentTokenRepository.findByUser(user)).hasSize(1);
        stub.invalidateSession(StringValue.newBuilder().setValue("1111-1111").build());
        assertThat(persistentTokenRepository.findByUser(user)).isEmpty();
    }

    <%_ } _%>
    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testRequestPasswordReset() {
        User user = UserResourceIntTest.createEntity(null);
        userRepository.saveAndFlush(user);
        stub.requestPasswordReset(StringValue.newBuilder().setValue(DEFAULT_EMAIL).build());
    }

    @Test
    public void testRequestPasswordResetWrongEmail() {
        try {
            stub.requestPasswordReset(StringValue.newBuilder().setValue(DEFAULT_EMAIL).build());
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        }
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testFinishPasswordReset() {
        User user = UserResourceIntTest.createEntity(null);
        user.setResetDate(ZonedDateTime.now().plusDays(1));
        user.setResetKey("reset key");
        userRepository.saveAndFlush(user);

        stub.finishPasswordReset(KeyAndPassword.newBuilder()
            .setNewPassword("new password")
            .setKey("reset key").build()
        );

        User updatedUser = userRepository.findOneByLogin(DEFAULT_LOGIN).get();
        assertThat(passwordEncoder.matches("new password", updatedUser.getPassword())).isTrue();
    }

    @Test
    public void testFinishPasswordResetPasswordTooSmall() {
        try {
            stub.finishPasswordReset(KeyAndPassword.newBuilder()
                .setNewPassword("foo")
                .setKey("reset key").build()
            );
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
        }
    }

    @Test<% if (databaseType == 'sql') { %>
    @Transactional<% } %>
    public void testFinishPasswordResetWrongKey() {
        User user = UserResourceIntTest.createEntity(null);
        userRepository.saveAndFlush(user);
        try {
            stub.finishPasswordReset(KeyAndPassword.newBuilder()
                .setNewPassword("new password")
                .setKey("wrong reset key").build()
            );
            failBecauseExceptionWasNotThrown(StatusRuntimeException.class);
        } catch (StatusRuntimeException e) {
            assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
        }
    }

}
