package com.pbuczek.pf.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pbuczek.pf.TestUserDetails;
import com.pbuczek.pf.interfaces.JpaEntity;
import com.pbuczek.pf.user.*;
import lombok.SneakyThrows;
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

import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("SameParameterValue")
@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class _BaseIT implements TestUserDetails {

    final Set<Integer> createdUserIds = new HashSet<>();
    static final ObjectMapper mapper = new ObjectMapper();
    static ObjectWriter ow;

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
        Integer potentialId = userRepo.getIdByUsername(TEST_USERNAME_ADMIN_1);
        if (potentialId != null) {
            userRepo.deleteUser(potentialId);
        }
        User admin = new User(TEST_USERNAME_ADMIN_1, TEST_EMAIL_ADMIN_1, TEST_PASSWORD);
        admin.setType(UserType.ADMIN);
        admin.setEnabled(true);
        userRepo.save(admin);
        createdUserIds.add(admin.getId());
    }

    int createUser(String username, String email) {
        return getObjectFromResponse(createUser(username, email, HttpStatus.OK), User.class).getId();
    }

    void createUser(String username, String email, HttpStatus expectedStatus, String expectedErrorMessage) {
        assertThat(createUser(username, email, expectedStatus).getErrorMessage()).isEqualTo(expectedErrorMessage);
    }

    @SneakyThrows
    MockHttpServletResponse createUser(String username, String email, HttpStatus expectedStatus) {
        MockHttpServletResponse response = sendAdminPostRequest(expectedStatus, "/user",
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
                                        String username, String url, String content) {
        return this.mockMvc.perform(MockMvcRequestBuilders.request(requestMethod, url)
                        .header("Authorization", getBasicAuthenticationHeader(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().is(expectedStatus.value())).andReturn().getResponse();
    }

    MockHttpServletResponse sendAdminPatchRequest(HttpStatus expectedStatus, String url, String content) {
        return sendRequest(HttpMethod.PATCH, expectedStatus, TEST_USERNAME_ADMIN_1, url, content);
    }

    MockHttpServletResponse sendAdminPostRequest(HttpStatus expectedStatus, String url, String content) {
        return sendRequest(HttpMethod.POST, expectedStatus, TEST_USERNAME_ADMIN_1, url, content);
    }

    MockHttpServletResponse sendAdminGetRequest(HttpStatus expectedStatus, String url, String content) {
        return sendRequest(HttpMethod.GET, expectedStatus, TEST_USERNAME_ADMIN_1, url, content);
    }

    MockHttpServletResponse sendAdminDeleteRequest(HttpStatus expectedStatus, String url, String content) {
        return sendRequest(HttpMethod.DELETE, expectedStatus, TEST_USERNAME_ADMIN_1, url, content);
    }


    void enableUserAccount(int userId) {
        sendRequest(HttpMethod.PATCH, HttpStatus.OK, TEST_USERNAME_ADMIN_1, "/user/enable/" + userId, "");
    }

    void changeUserPaymentPlan(Integer userId, PaymentPlan plan) {
        sendRequest(HttpMethod.PATCH, HttpStatus.OK, TEST_USERNAME_ADMIN_1, "/user/paymentplan/" + userId + "/" + plan, "");
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

    <T extends JpaEntity> T getObjectFromResponse(
            MockHttpServletResponse response, Class<T> returnedClass, Set<Integer> set) {
        T object = getObjectFromResponse(response, returnedClass);
        assertThat(object.getId()).isNotNull();
        if (set != null) {
            set.add(object.getId());
        }

        return object;
    }

    @SneakyThrows
    <T> T getObjectFromResponse(MockHttpServletResponse response, Class<T> returnedClass) {
        T object = mapper.readValue(response.getContentAsString(), returnedClass);
        assertThat(object).isNotNull();

        return object;
    }
}
