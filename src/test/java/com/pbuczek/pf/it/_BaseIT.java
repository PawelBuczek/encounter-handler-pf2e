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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureObservability
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class _BaseIT implements TestUserDetails {

    final List<Integer> createdUserIds = new ArrayList<>();
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

    @SneakyThrows
    int createUser(String username, String email) {
        MockHttpServletResponse response = createUser(username, email, HttpStatus.OK);
        User user = mapper.readValue(response.getContentAsString(), User.class);
        if (user.getId() != null) {
            createdUserIds.add(user.getId());
        } else {
            fail("User could not be created correctly");
        }
        return user.getId();
    }

    @SneakyThrows
    MockHttpServletResponse createUser(String username, String email, HttpStatus expectedStatus) {
        return sendAdminRequest(HttpMethod.POST,
                "/user",
                ow.writeValueAsString(new UserDto(username, email, TEST_PASSWORD)))
                .andExpect(status().is(expectedStatus.value()))
                .andReturn().getResponse();
    }

    void createUser(String username, String email, HttpStatus expectedStatus, String expectedErrorMessage) {
        assertThat(createUser(username, email, expectedStatus).getErrorMessage()).isEqualTo(expectedErrorMessage);
    }

    @SneakyThrows
    ResultActions sendAdminRequest(HttpMethod requestMethod, String url, String content) {
        return this.mockMvc.perform(MockMvcRequestBuilders.request(requestMethod, url)
                        .header("Authorization", getBasicAuthenticationHeader(TEST_USERNAME_ADMIN_1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content));
    }

    void enableUserAccount(int userId) {
        sendAdminRequest(HttpMethod.PATCH, "/user/enable/" + userId, "");
    }

    void changeUserPaymentPlan(Integer userId, PaymentPlan plan) {
        sendAdminRequest(HttpMethod.PATCH, "/user/paymentplan/" + userId + "/" + plan, "");
    }

    String getBasicAuthenticationHeader(String username) {
        String valueToEncode = username + ":" + TEST_PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    @SneakyThrows
    <T extends JpaEntity> T getObjectFromResponse(
            MockHttpServletResponse response, Class<T> returnedClass, List<Integer> list) {

        T object = mapper.readValue(response.getContentAsString(), returnedClass);
        assertThat(object).isNotNull();
        assertThat(object.getId()).isNotNull();
        list.add(object.getId());

        return object;
    }
}
