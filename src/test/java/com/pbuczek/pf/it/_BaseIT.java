package com.pbuczek.pf.it;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pbuczek.pf.apikey.ApiKey;
import com.pbuczek.pf.user.*;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("SameParameterValue")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class _BaseIT {

    final Set<Integer> createdUserIds = new HashSet<>();
    static final ObjectMapper mapper = new ObjectMapper();
    static ObjectWriter ow;
    static final Logger logger = Logger.getLogger("_BaseIT");

    String TEST_USERNAME_ADMIN_1 = "";
    String TEST_EMAIL_ADMIN_1 = "";
    String TEST_USERNAME_STANDARD_1 = "";
    String TEST_EMAIL_STANDARD_1 = "";
    String TEST_USERNAME_STANDARD_2 = "";
    String TEST_EMAIL_STANDARD_2 = "";
    String TEST_PASSWORD = "aB@1" + RandomStringUtils.random(50);

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepo;

    @BeforeAll
    static void initialize() {
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        mapper.registerModule(new JavaTimeModule());
        ow = mapper.writer().withDefaultPrettyPrinter();
    }

    @BeforeEach
    void setUp() {
        TEST_USERNAME_ADMIN_1 = generateRandomUsername();
        TEST_USERNAME_STANDARD_1 = generateRandomUsername();
        TEST_USERNAME_STANDARD_2 = generateRandomUsername();
        TEST_EMAIL_ADMIN_1 = TEST_USERNAME_ADMIN_1 + "@test.com";
        TEST_EMAIL_STANDARD_1 = TEST_USERNAME_STANDARD_1 + "@test.com";
        TEST_EMAIL_STANDARD_2 = TEST_USERNAME_STANDARD_2 + "@test.com";

        User admin = new User(TEST_USERNAME_ADMIN_1, TEST_EMAIL_ADMIN_1, TEST_PASSWORD);
        admin.setType(UserType.ADMIN);
        admin.setEnabled(true);
        userRepo.save(admin);
        createdUserIds.add(admin.getId());
    }

    private String generateRandomUsername() {
        String username = "";
        for (int i = 0; i < 100; i++) {
            username = "_Test_" + LocalDate.now() + RandomStringUtils.random(24, true, true);
            if (userRepo.findByUsername(username).isEmpty()) {
                break;
            }
            logger.log(new LogRecord(Level.WARNING, "username taken, retrying"));
        }
        return username;
    }

    int createUser(String username, String email) {
        return getObjectFromResponse(createUser(username, email, HttpStatus.OK), User.class).getId();
    }

    void createUser(String username, String email, HttpStatus expectedStatus, String expectedErrorMessage) {
        assertThat(createUser(username, email, expectedStatus).getErrorMessage()).isEqualTo(expectedErrorMessage);
    }

    @SneakyThrows
    MockHttpServletResponse createUser(String username, String email, HttpStatus expectedStatus) {
        MockHttpServletResponse response =
                sendRequest(HttpMethod.POST, expectedStatus, TEST_USERNAME_ADMIN_1, "/user",
                        ow.writeValueAsString(new UserDto(username, email, TEST_PASSWORD)));
        try {
            User user = mapper.readValue(response.getContentAsString(), User.class);
            if (user.getId() != null) {
                createdUserIds.add(user.getId());
            }
        } catch (Exception ignored) {
        }

        return response;
    }

    @SneakyThrows
    MockHttpServletResponse sendRequest(HttpMethod requestMethod, HttpStatus expectedStatus,
                                        String authUsername, String url, String content) {
        return this.mockMvc.perform(MockMvcRequestBuilders.request(requestMethod, url)
                        .header("Authorization", getBasicAuthenticationHeader(authUsername))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().is(expectedStatus.value())).andReturn().getResponse();
    }


    void enableUserAccount(int userId) {
        sendRequest(HttpMethod.PATCH, HttpStatus.OK, TEST_USERNAME_ADMIN_1, "/user/enable/" + userId, "");
    }

    void changeUserPaymentPlan(Integer userId, PaymentPlan plan) {
        sendRequest(HttpMethod.PATCH, HttpStatus.OK, TEST_USERNAME_ADMIN_1, "/user/paymentplan/" + userId + "/" + plan, "");
    }

    @SneakyThrows
    int deleteUser(Integer userId) {
        return Integer.parseInt(
                sendRequest(HttpMethod.DELETE, HttpStatus.OK, TEST_USERNAME_ADMIN_1, "/user/" + userId, "")
                        .getContentAsString());
    }

    String getBasicAuthenticationHeader(String username) {
        String valueToEncode = username + ":" + TEST_PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    <T, U extends JpaRepository<T, Integer>> T getObjectFromJpaRepo(Integer objectId, U repo) {
        Optional<T> optionalObject = repo.findById(objectId);
        assertThat(optionalObject).isPresent();
        return optionalObject.get();
    }

    @SneakyThrows
    <T> T getObjectFromResponse(MockHttpServletResponse response, Class<T> returnedClass) {
        T object = mapper.readValue(response.getContentAsString(), returnedClass);
        assertThat(object).isNotNull();
        return object;
    }

    @SneakyThrows
    <T> List<T> getListOfObjectsFromResponse(MockHttpServletResponse response, TypeReference<List<T>> typeReference) {
        return mapper.readValue(response.getContentAsString(), typeReference);
    }
}
