package com.dmdev.junit.service;

import com.dmdev.junit.TestBase;
import com.dmdev.junit.dto.User;
import com.dmdev.junit.extension.*;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.*;

@Tag("fast")
@Tag("user")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
@ExtendWith({
        UserServiceParamResolver.class,
        PostProcessingExtension.class,
        ConditionalExtension.class,
        ThrowableExtension.class
//        GlobalExtension.class
})
public class UserServiceTest extends TestBase {

    private static final User IVAN = User.of(1, "Ivan", "123");
    private static final User PETR = User.of(2, "Petr", "111");
    private UserService userService;

    public UserServiceTest(TestInfo testInfo) {
        System.out.println();
    }

    @BeforeAll
    void init() {
        System.out.println("BeforeAll: " + this);
    }

    @BeforeEach
    void prepare(UserService userService) {
        System.out.println("BeforeEach: " + this);
        this.userService = userService;
    }

    @Test
    @Order(1)
    @DisplayName("users will be empty if no users added")
    void usersEmptyIfNoUserAdded(UserService userService) throws IOException {
        if (true) {
           throw new RuntimeException();
        }
        System.out.println("Test 1: " + this);
        var users = userService.getAll();

        MatcherAssert.assertThat(users, empty());
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
        userService.add(IVAN, PETR);

        Map<Integer, User> users = userService.getAllConvertedById();


        MatcherAssert.assertThat(users, IsMapContaining.hasKey(IVAN.getId()));

        assertAll(
                () -> assertThat(users).containsKeys(IVAN.getId(), PETR.getId()),
                () -> assertThat(users).containsValues(IVAN, PETR)
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
    @Timeout(value = 200, unit = TimeUnit.MILLISECONDS)
    class LoginTest {
//        @Test
        @RepeatedTest(value = 5, name = RepeatedTest.LONG_DISPLAY_NAME)
        void loginSuccessIfUserExist(RepetitionInfo repetitionInfo) {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUserName(), IVAN.getPassword());

            assertThat(maybeUser).isPresent();
            maybeUser.ifPresent(user -> assertThat(user).isEqualTo(IVAN));
//        assertTrue(maybeUser.isPresent());
//        maybeUser.ifPresent(user -> assertEquals(IVAN, user));
        }

        @Test
//        @org.junit.Test(expected = IllegalArgumentException.class)
        void throwExceptionIfUsernameOrPasswordIsNull() {
            assertAll(
                    () -> {
                        var exception = assertThrows(IllegalArgumentException.class, () -> userService.login(null, "dummy"),
                                "login should throw exception on null username");
                        assertThat(exception.getMessage()).isEqualTo("username or password is null");
                    },
                    () -> assertThrows(IllegalArgumentException.class, () -> userService.login("dummy", null), "login should throw exception on null username")
            );
        }

        @Test
        @Disabled("flaky, need to see")
        void loginFailIfPasswordIsNotCorrect() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login(IVAN.getUserName(), "dummy");

            assertTrue(maybeUser.isEmpty());
        }

        @Test
        void checkLoginFunctionalityPerformance() {
            System.out.println(Thread.currentThread().getName());
            var result = assertTimeoutPreemptively(Duration.ofMillis(200L), () -> {
                System.out.println(Thread.currentThread().getName());
                Thread.sleep(100L);
                return userService.login("dummy", IVAN.getPassword());
            });
        }

        @Test
        void loginFailIfUserDoesNotExist() {
            userService.add(IVAN);
            Optional<User> maybeUser = userService.login("dummy", IVAN.getPassword());

            assertTrue(maybeUser.isEmpty());
        }

        @ParameterizedTest(name = "{arguments} test")
//        @ArgumentsSource()
//        @NullSource
//        @EmptySource
//        @NullAndEmptySource
//        @ValueSource(strings = {
//                "Ivan", "Petr"
//        })
//        @EnumSource
//        @CsvFileSource(resources = "/login-test-data.csv", delimiter = ',', numLinesToSkip = 1)
//        @CsvSource({
//                "Ivan,123",
//                "Petr,111"
//        })
        @MethodSource("com.dmdev.junit.service.UserServiceTest#getArgumentsForLoginTest")
        @DisplayName("login param test")
        void loginParametrizedTest(String username, String password, Optional<User> user) {
            userService.add(IVAN, PETR);

            var maybeUser = userService.login(username, password);
            assertThat(maybeUser).isEqualTo(user);
        }
    }

    static Stream<Arguments> getArgumentsForLoginTest() {
        return Stream.of(
                Arguments.of("Ivan", "123", Optional.of(IVAN)),
                Arguments.of("Petr", "111", Optional.of(PETR)),
                Arguments.of("Petr", "dummy", Optional.empty()),
                Arguments.of("dummy", "123", Optional.empty())
        );
    }

}
