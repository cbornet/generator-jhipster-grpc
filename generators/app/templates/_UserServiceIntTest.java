package <%=packageName%>.service;
<% if (databaseType == 'cassandra') { %>
import <%=packageName%>.AbstractCassandraTest;<% } %>
import <%=packageName%>.<%= mainClass %>;<% if ((databaseType == 'sql' || databaseType == 'mongodb') && authenticationType == 'session') { %>
import <%=packageName%>.domain.PersistentToken;<% } %>
import <%=packageName%>.domain.User;<% if ((databaseType == 'sql' || databaseType == 'mongodb') && authenticationType == 'session') { %>
import <%=packageName%>.repository.PersistentTokenRepository;<% } %>
import <%=packageName%>.config.Constants;
import <%=packageName%>.repository.UserRepository;
import <%=packageName%>.service.dto.UserDTO;
<%_ if (authenticationType !== 'oauth2') { _%>
import java.time.Instant;
import java.time.temporal.ChronoUnit;
<%_ } _%>
<%_ if (authenticationType !== 'oauth2' && (databaseType === 'sql' || databaseType === 'mongodb')) { _%>
import <%=packageName%>.service.util.RandomUtil;
<%_ } _%>
<%_ if ((databaseType == 'sql' || databaseType == 'mongodb') && authenticationType == 'session') { _%>
import java.time.LocalDate;
<%_ } _%>
<%_ if (authenticationType !== 'oauth2') { _%>
import org.apache.commons.lang3.RandomStringUtils;
<%_ } _%>
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;<% if (databaseType == 'sql') { %>
import org.springframework.transaction.annotation.Transactional;<% } %>
import org.springframework.test.context.junit4.SpringRunner;
<%_ if (databaseType === 'sql' || databaseType === 'mongodb') { _%>
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
<%_ } _%>

<%_ if (authenticationType !== 'oauth2') { _%>
import java.util.List;
<%_ } _%>
<%_ if (authenticationType !== 'oauth2' && (databaseType === 'sql' || databaseType === 'mongodb')) { _%>
import java.util.Optional;
<%_ } _%><%_ if (databaseType === 'cassandra') { _%>
import java.util.UUID;
<%_ } _%>

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for the UserResource REST controller.
 *
 * @see UserService
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = <%= mainClass %>.class)<% if (databaseType == 'sql') { %>
@Transactional<% } %>
public class UserServiceIntTest <% if (databaseType == 'cassandra') { %>extends AbstractCassandraTest <% } %>{<% if ((databaseType == 'sql' || databaseType == 'mongodb') && authenticationType == 'session') { %>

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;<% } %>

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    private User user;

    /**
     * Create a User.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which has a required relationship to the User entity.
     */
    public static User createEntity() {
        User user = new User();
        <%_ if (databaseType === 'cassandra') { _%>
        user.setId(UUID.randomUUID().toString());
        <%_ } _%>
        user.setLogin("johndoe");
        <%_ if (authenticationType !== 'oauth2') { _%>
        user.setPassword(RandomStringUtils.random(60));
        <%_ } _%>
        user.setActivated(true);
        user.setEmail("johndoe@localhost");
        user.setFirstName("john");
        user.setLastName("doe");
        <%_ if (databaseType !== 'cassandra') { _%>
        user.setImageUrl("http://placehold.it/50x50");
        <%_ } _%>
        user.setLangKey("en");
        return user;
    }

    @Before
    public void init() {<% if ((databaseType == 'sql' || databaseType == 'mongodb') && authenticationType == 'session') { %>
        persistentTokenRepository.deleteAll();<% } %>
        <%_ if (databaseType !== 'sql') { _%>
        userRepository.deleteAll();
        <%_ } _%>
        user = createEntity();
    }<% if ((databaseType == 'sql' || databaseType == 'mongodb') && authenticationType == 'session') { %>

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void testRemoveOldPersistentTokens() {
        userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);
        int existingCount = persistentTokenRepository.findByUser(user).size();
        generateUserToken(user, "1111-1111", LocalDate.now());
        LocalDate now = LocalDate.now();
        generateUserToken(user, "2222-2222", now.minus(32, ChronoUnit.DAYS));
        assertThat(persistentTokenRepository.findByUser(user)).hasSize(existingCount + 2);
        userService.removeOldPersistentTokens();
        assertThat(persistentTokenRepository.findByUser(user)).hasSize(existingCount + 1);
    }<% } %><% if (authenticationType !== 'oauth2' && (databaseType === 'sql' || databaseType === 'mongodb')) { %>

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void assertThatUserMustExistToResetPassword() {
        userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);
        Optional<User> maybeUser = userService.requestPasswordReset("invalid.login@localhost");
        assertThat(maybeUser.isPresent()).isFalse();

        maybeUser = userService.requestPasswordReset(user.getEmail());
        assertThat(maybeUser.isPresent()).isTrue();

        assertThat(maybeUser.get().getEmail()).isEqualTo(user.getEmail());
        assertThat(maybeUser.get().getResetDate()).isNotNull();
        assertThat(maybeUser.get().getResetKey()).isNotNull();
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void assertThatOnlyActivatedUserCanRequestPasswordReset() {
        user.setActivated(false);
        userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);
        Optional<User> maybeUser = userService.requestPasswordReset(user.getLogin());
        assertThat(maybeUser.isPresent()).isFalse();
        userRepository.delete(user);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void assertThatResetKeyMustNotBeOlderThan24Hours() {
        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);

        userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);

        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());

        assertThat(maybeUser.isPresent()).isFalse();

        userRepository.delete(user);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void assertThatResetKeyMustBeValid() {
        Instant daysAgo = Instant.now().minus(25, ChronoUnit.HOURS);
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey("1234");
        userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);
        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());
        assertThat(maybeUser.isPresent()).isFalse();
        userRepository.delete(user);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void assertThatUserCanResetPassword() {
        String oldPassword = user.getPassword();
        Instant daysAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        String resetKey = RandomUtil.generateResetKey();
        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);
        userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);
        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());
        assertThat(maybeUser.isPresent()).isTrue();
        assertThat(maybeUser.get().getResetDate()).isNull();
        assertThat(maybeUser.get().getResetKey()).isNull();
        assertThat(maybeUser.get().getPassword()).isNotEqualTo(oldPassword);

        userRepository.delete(user);
    }

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void testFindNotActivatedUsersByCreationDateBefore() {
        Instant now = Instant.now();
        user.setActivated(false);
        User dbUser = userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);
        dbUser.setCreatedDate(now.minus(4, ChronoUnit.DAYS));
        userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);
        List<User> users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS));
        assertThat(users).isNotEmpty();
        userService.removeNotActivatedUsers();
        users = userRepository.findAllByActivatedIsFalseAndCreatedDateBefore(now.minus(3, ChronoUnit.DAYS));
        assertThat(users).isEmpty();
    }<% } %><% if ((databaseType == 'sql' || databaseType == 'mongodb') && authenticationType == 'session') { %>

    private void generateUserToken(User user, String tokenSeries, LocalDate localDate) {
        PersistentToken token = new PersistentToken();
        token.setSeries(tokenSeries);
        token.setUser(user);
        token.setTokenValue(tokenSeries + "-data");
        token.setTokenDate(localDate);
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test agent");
        persistentTokenRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(token);
    }<% } %>

    @Test
    <%_ if (databaseType === 'sql') { _%>
    @Transactional
    <%_ } _%>
    public void assertThatAnonymousUserIsNotGet() {
        user.setLogin(Constants.ANONYMOUS_USER);
        if (!userRepository.findOneByLogin(Constants.ANONYMOUS_USER).isPresent()) {
            userRepository.save<% if (databaseType == 'sql') { %>AndFlush<% } %>(user);
        }<% if (databaseType == 'sql' || databaseType == 'mongodb') { %>
        final PageRequest pageable = new PageRequest(0, (int) userRepository.count());
        final Page<UserDTO> allManagedUsers = userService.getAllManagedUsers(pageable);
        assertThat(allManagedUsers.getContent().stream()<% } %><% if (databaseType == 'cassandra') { %>
        final List<UserDTO> allManagedUsers = userService.getAllManagedUsers();
        assertThat(allManagedUsers.stream()<% } %>
            .noneMatch(user -> Constants.ANONYMOUS_USER.equals(user.getLogin())))
            .isTrue();
    }
}
