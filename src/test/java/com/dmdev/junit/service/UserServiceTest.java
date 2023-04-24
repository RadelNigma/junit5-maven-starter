package com.dmdev.junit.service;

import com.dmdev.junit.dto.User;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.*;

@Tag("fast")
@Tag("user")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class UserServiceTest {

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETR = User.of(2, "Petr", "111");
    private UserService userService;

    @BeforeAll
    void init() {
        System.out.println("BeforeAll: " + this);
    }

    @BeforeEach
    void prepare() {
        System.out.println("BeforeEach: " + this);
        userService = new UserService();
    }

    @Test
    @Order(1)
    @DisplayName("users will be empty if no users added")
    void usersEmptyIfNoUserAdded() {
        System.out.println("Test 1: " + this);
        var users = userService.getAll();

        MatcherAssert.assertThat(users,  empty());
        assertTrue(users.isEmpty(), "User list should be empty!");
    }

    @Test
    @Order(2)
    void usersSizeIfUserAdded() {
        System.out.println("Test 2: " + this);
        userService.add(IVAN);
        userService.add(PETR);

        var users = userService.getAll();

        assertThat(users).hasSize(2);
//        assertEquals(2, users.size());
    }

    @Test
    void usersConvertedToMapById() {
        userService.add(IVAN,PETR);

       Map<Integer,User > users =  userService.getAllConvertedById();


        MatcherAssert.assertThat(users, IsMapContaining.hasKey(IVAN.getId()));

       assertAll(
               () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
               () -> assertThat(users).containsValues(IVAN,PETR)
       );
    }

    @AfterEach
    void deleteDateFromDatabase() {
        System.out.println("AfterEach: " + this);
    }

    @AfterAll
    void closeConnectionPool() {
        System.out.println("AfterAll: " + this);
    }

    @Nested
    @DisplayName("test user login functionality")
    @Tag("login")
    class LoginTest {
        @Test
        void loginSuccessIfUserExist() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUserName(),IVAN.getPassword());

            assertThat(maybeUser).isPresent();
            maybeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));
//        assertTrue(maybeUser.isPresent());
//        maybeUser.ifPresent(user -> assertEquals(IVAN, user));
        }

        @Test
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> {
                        var exception = assertThrows(IllegalArgumentException.class, () -> userService.login(null, "dummy"),
                                "login should throw exception on null username");
                        assertThat(exception.getMessage()).isEqualTo("username or password is null");
                    },
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null),"login should throw exception on null username")
            );
        }

        @Test
        void loginFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUserName(), "dummy");

            assertTrue(maybeUser.isEmpty());
        }

        @Test
        void loginFailIfUserDoesNotExist() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login("dummy", IVAN.getPassword());

            assertTrue(maybeUser.isEmpty());
        }
    }
}
